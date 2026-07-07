/**
 * acceptance/issue 视图单元测试（Task 2.4）
 *
 * 覆盖：
 *   - 渲染（标题、表格、搜索）
 *   - onMounted 加载 pageAcceptanceIssues
 *   - 搜索 / 重置
 *   - openCreate / openEdit
 *   - handleSubmit 新建/编辑
 *   - handleDelete / handleResolve / handleClose -> Modal.confirm
 *   - statusMap / severityMap 映射
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Issue from '../issue.vue'

/* ============ Mock ant-design-vue message + Modal ============ */
const mocks = vi.hoisted(() => ({
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
  messageWarning: vi.fn(),
  modalConfirmCallbacks: [] as Array<{ onOk?: () => any; onCancel?: () => any }>
}))

vi.mock('ant-design-vue', async () => {
  const actual = await vi.importActual<typeof import('ant-design-vue')>('ant-design-vue')
  return {
    ...actual,
    message: {
      success: mocks.messageSuccess,
      error: mocks.messageError,
      warning: mocks.messageWarning,
      info: vi.fn(),
      loading: vi.fn()
    },
    Modal: {
      confirm: (opts: any) => {
        mocks.modalConfirmCallbacks.push({ onOk: opts?.onOk, onCancel: opts?.onCancel })
        return { destroy: vi.fn(), update: vi.fn() }
      }
    }
  }
})

/* ============ Mock @/api/acceptance ============ */
const apiMocks = vi.hoisted(() => ({
  pageAcceptanceIssues: vi.fn(),
  createAcceptanceIssue: vi.fn(),
  updateAcceptanceIssue: vi.fn(),
  deleteAcceptanceIssue: vi.fn(),
  resolveAcceptanceIssue: vi.fn(),
  closeAcceptanceIssue: vi.fn()
}))

vi.mock('@/api/acceptance', () => apiMocks)

describe('acceptance issue view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.pageAcceptanceIssues.mockResolvedValue({
      records: [
        {
          id: 1,
          name: '设备故障',
          projectId: 100,
          severity: 'HIGH',
          status: 'OPEN',
          dueDate: '2026-08-01'
        }
      ],
      total: 1
    })
    apiMocks.createAcceptanceIssue.mockResolvedValue(2)
    apiMocks.updateAcceptanceIssue.mockResolvedValue(undefined)
    apiMocks.deleteAcceptanceIssue.mockResolvedValue(undefined)
    apiMocks.resolveAcceptanceIssue.mockResolvedValue(undefined)
    apiMocks.closeAcceptanceIssue.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Issue, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('遗留问题')
  })

  it('onMounted 调用 pageAcceptanceIssues', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.pageAcceptanceIssues).toHaveBeenCalledTimes(1)
  })

  it('handleSearch 重置页码为 1', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.pagination.current = 5
    vm.query.projectId = 100
    await vm.handleSearch()
    await flushPromises()
    expect(vm.pagination.current).toBe(1)
  })

  it('handleReset 清空 query', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.query.projectId = 999
    vm.query.status = 'OPEN'
    vm.query.severity = 'HIGH'
    await vm.handleReset()
    await flushPromises()
    expect(vm.query.projectId).toBeUndefined()
    expect(vm.query.status).toBeUndefined()
    expect(vm.query.severity).toBeUndefined()
  })

  it('openCreate 重置 form 并打开弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    expect(vm.modalVisible).toBe(true)
    expect(vm.form.name).toBe('')
    expect(vm.form.severity).toBe('MEDIUM')
  })

  it('openEdit 加载记录到 form', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openEdit({ id: 5, taskId: 1, projectId: 100, name: '问题', description: 'd', severity: 'HIGH', assigneeId: 10, dueDate: '2026-08-01', remark: '' })
    expect(vm.modalVisible).toBe(true)
    expect(vm.form.id).toBe(5)
    expect(vm.form.severity).toBe('HIGH')
  })

  it('handleSubmit 新建调用 createAcceptanceIssue', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formRef = { validate: () => Promise.resolve() }
    vm.openCreate()
    vm.form.taskId = 1
    vm.form.projectId = 100
    vm.form.name = '新问题'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createAcceptanceIssue).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
  })

  it('handleSubmit 编辑调用 updateAcceptanceIssue', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formRef = { validate: () => Promise.resolve() }
    vm.openEdit({ id: 7, taskId: 1, projectId: 100, name: 'old', description: '', severity: 'LOW', assigneeId: undefined, dueDate: '', remark: '' })
    vm.form.name = 'new'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.updateAcceptanceIssue).toHaveBeenCalledWith(7, expect.objectContaining({ name: 'new' }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('更新成功')
  })

  it('handleDelete onOk 调用 deleteAcceptanceIssue', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 13, name: 'X' })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteAcceptanceIssue).toHaveBeenCalledWith(13)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('handleResolve onOk 调用 resolveAcceptanceIssue', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleResolve({ id: 5, name: 'X' })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.resolveAcceptanceIssue).toHaveBeenCalledWith(5)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已标记整改完成')
  })

  it('handleClose onOk 调用 closeAcceptanceIssue', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleClose({ id: 7, name: 'X' })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.closeAcceptanceIssue).toHaveBeenCalledWith(7)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已闭环确认')
  })

  it('statusMap 包含 4 种状态', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(Object.keys(vm.statusMap).length).toBe(4)
    expect(vm.statusMap.OPEN.label).toBe('待处理')
    expect(vm.statusMap.CLOSED.label).toBe('已闭环')
  })

  it('severityMap 包含 4 种严重等级', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(Object.keys(vm.severityMap).length).toBe(4)
    expect(vm.severityMap.LOW.label).toBe('低')
    expect(vm.severityMap.CRITICAL.label).toBe('严重')
  })

  it('pageAcceptanceIssues 异常时不抛错', async () => {
    apiMocks.pageAcceptanceIssues.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.dataSource).toEqual([])
  })
})
