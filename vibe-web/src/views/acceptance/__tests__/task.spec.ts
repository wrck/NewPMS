/**
 * acceptance/task 视图单元测试（Task 2.4）
 *
 * 覆盖：
 *   - 渲染（标题、表格、搜索）
 *   - onMounted 加载 pageAcceptanceTasks
 *   - 搜索 / 重置
 *   - openCreate / handleSubmit
 *   - handleApply -> Modal.confirm + applyAcceptanceTask
 *   - handleDelete -> Modal.confirm + deleteAcceptanceTask
 *   - viewDetail 调用 listAcceptanceTestRecords
 *   - statusLabel/statusColor 工具函数
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Task from '../task.vue'

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
  pageAcceptanceTasks: vi.fn(),
  createAcceptanceTask: vi.fn(),
  deleteAcceptanceTask: vi.fn(),
  applyAcceptanceTask: vi.fn(),
  listAcceptanceTestRecords: vi.fn()
}))

vi.mock('@/api/acceptance', () => apiMocks)

describe('acceptance task view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.pageAcceptanceTasks.mockResolvedValue({
      records: [
        {
          id: 1,
          name: '项目A初验',
          projectId: 100,
          status: 'DRAFT',
          applyTime: '',
          customerSignResult: '',
          score: undefined
        }
      ],
      total: 1
    })
    apiMocks.createAcceptanceTask.mockResolvedValue(2)
    apiMocks.deleteAcceptanceTask.mockResolvedValue(undefined)
    apiMocks.applyAcceptanceTask.mockResolvedValue(undefined)
    apiMocks.listAcceptanceTestRecords.mockResolvedValue([
      { id: 10, itemName: '设备检查', result: 'PASS', remark: '' }
    ])
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Task, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('验收任务')
  })

  it('onMounted 调用 pageAcceptanceTasks', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.pageAcceptanceTasks).toHaveBeenCalledTimes(1)
  })

  it('handleSearch 重置页码为 1', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.pagination.current = 5
    vm.query.name = 'kw'
    await vm.handleSearch()
    await flushPromises()
    expect(vm.pagination.current).toBe(1)
  })

  it('handleReset 清空 query', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.query.projectId = 999
    vm.query.name = 'dirty'
    vm.query.status = 'DRAFT'
    await vm.handleReset()
    await flushPromises()
    expect(vm.query.projectId).toBeUndefined()
    expect(vm.query.name).toBe('')
    expect(vm.query.status).toBeUndefined()
  })

  it('openCreate 重置 form 并打开弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    expect(vm.modalVisible).toBe(true)
    expect(vm.form.name).toBe('')
  })

  it('handleSubmit 调用 createAcceptanceTask', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formRef = { validate: () => Promise.resolve() }
    vm.openCreate()
    vm.form.projectId = 200
    vm.form.name = '初验'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createAcceptanceTask).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
    expect(vm.modalVisible).toBe(false)
  })

  it('handleApply onOk 调用 applyAcceptanceTask', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleApply({ id: 5, name: 'X' })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.applyAcceptanceTask).toHaveBeenCalledWith({ taskId: 5 })
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已提交验收申请')
  })

  it('handleDelete onOk 调用 deleteAcceptanceTask', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 13, name: 'X' })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteAcceptanceTask).toHaveBeenCalledWith(13)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('viewDetail 调用 listAcceptanceTestRecords', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.viewDetail({ id: 1, name: 'X' })
    await flushPromises()
    expect(apiMocks.listAcceptanceTestRecords).toHaveBeenCalledWith(1)
    expect(vm.detailVisible).toBe(true)
    expect(vm.testRecords.length).toBe(1)
  })

  it('statusLabel 返回正确中文标签', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.statusLabel('DRAFT')).toBe('草稿')
    expect(vm.statusLabel('COMPLETED')).toBe('已完成')
    expect(vm.statusLabel('REJECTED')).toBe('已驳回')
    expect(vm.statusLabel('UNKNOWN')).toBe('UNKNOWN')
  })

  it('statusColor 返回正确颜色', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.statusColor('DRAFT')).toBe('default')
    expect(vm.statusColor('COMPLETED')).toBe('success')
    expect(vm.statusColor('REJECTED')).toBe('error')
  })

  it('pageAcceptanceTasks 异常时不抛错', async () => {
    apiMocks.pageAcceptanceTasks.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.dataSource).toEqual([])
  })
})
