/**
 * acceptance/standard 视图单元测试（Task 2.4）
 *
 * 覆盖：
 *   - 渲染（标题、表格、搜索）
 *   - onMounted 加载 pageAcceptanceStandards
 *   - 搜索 / 重置
 *   - openCreate / openEdit
 *   - handleSubmit 新建/编辑
 *   - handleDelete -> Modal.confirm
 *   - 检查项子表：openItemDrawer 调用 getAcceptanceStandardDetail
 *   - addItemRow / removeItemRow / moveItemRow
 *   - saveItems 校验空名称 + 调用 updateAcceptanceStandard
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Standard from '../standard.vue'

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
  pageAcceptanceStandards: vi.fn(),
  createAcceptanceStandard: vi.fn(),
  updateAcceptanceStandard: vi.fn(),
  deleteAcceptanceStandard: vi.fn(),
  getAcceptanceStandardDetail: vi.fn()
}))

vi.mock('@/api/acceptance', () => apiMocks)

describe('acceptance standard view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.pageAcceptanceStandards.mockResolvedValue({
      records: [
        {
          id: 1,
          name: '标准验收模板',
          projectType: 'NETWORK',
          standardVersion: '1.0.0',
          status: 1,
          createTime: '2026-07-01',
          items: []
        }
      ],
      total: 1
    })
    apiMocks.createAcceptanceStandard.mockResolvedValue(2)
    apiMocks.updateAcceptanceStandard.mockResolvedValue(undefined)
    apiMocks.deleteAcceptanceStandard.mockResolvedValue(undefined)
    apiMocks.getAcceptanceStandardDetail.mockResolvedValue({
      id: 1,
      name: '标准验收模板',
      projectType: 'NETWORK',
      standardVersion: '1.0.0',
      description: '',
      status: 1,
      items: [
        { id: 10, name: '设备外观', requirement: '无损伤', testMethod: '目视', weight: 1, sortOrder: 0 },
        { id: 11, name: '功能测试', requirement: '通过', testMethod: '测试', weight: 2, sortOrder: 1 }
      ]
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Standard, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('验收标准')
  })

  it('onMounted 调用 pageAcceptanceStandards', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.pageAcceptanceStandards).toHaveBeenCalledTimes(1)
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
    vm.query.name = 'dirty'
    vm.query.projectType = 'NETWORK'
    await vm.handleReset()
    await flushPromises()
    expect(vm.query.name).toBe('')
    expect(vm.query.projectType).toBe('')
  })

  it('openCreate 重置 form 并打开弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    expect(vm.modalVisible).toBe(true)
    expect(vm.form.name).toBe('')
    expect(vm.form.items).toEqual([])
  })

  it('openEdit 加载记录到 form', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openEdit({ id: 5, name: '标准', projectType: 'NETWORK', standardVersion: '2.0', description: 'd', status: 1, items: [{ name: 'X' }] })
    expect(vm.modalVisible).toBe(true)
    expect(vm.form.id).toBe(5)
    expect(vm.form.name).toBe('标准')
    expect(vm.form.items.length).toBe(1)
  })

  it('handleSubmit 新建调用 createAcceptanceStandard', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formRef = { validate: () => Promise.resolve() }
    vm.openCreate()
    vm.form.name = '新标准'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createAcceptanceStandard).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
  })

  it('handleSubmit 编辑调用 updateAcceptanceStandard', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formRef = { validate: () => Promise.resolve() }
    vm.openEdit({ id: 7, name: 'old', projectType: '', standardVersion: '1.0', description: '', status: 1, items: [] })
    vm.form.name = 'new'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.updateAcceptanceStandard).toHaveBeenCalledWith(7, expect.objectContaining({ name: 'new' }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('更新成功')
  })

  it('handleDelete onOk 调用 deleteAcceptanceStandard', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 13, name: 'X' })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteAcceptanceStandard).toHaveBeenCalledWith(13)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('openItemDrawer 调用 getAcceptanceStandardDetail', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openItemDrawer({ id: 1, name: 'X', items: [] })
    await flushPromises()
    expect(apiMocks.getAcceptanceStandardDetail).toHaveBeenCalledWith(1)
    expect(vm.itemDrawerVisible).toBe(true)
    expect(vm.itemRows.length).toBe(2)
  })

  it('addItemRow 添加新行', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.itemRows = []
    vm.addItemRow()
    expect(vm.itemRows.length).toBe(1)
    expect(vm.itemRows[0].name).toBe('')
    expect(vm.itemRows[0].weight).toBe(1)
  })

  it('removeItemRow 删除行并重排序', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.itemRows = [
      { name: 'A', sortOrder: 0 },
      { name: 'B', sortOrder: 1 },
      { name: 'C', sortOrder: 2 }
    ]
    vm.removeItemRow(1)
    expect(vm.itemRows.length).toBe(2)
    expect(vm.itemRows[0].name).toBe('A')
    expect(vm.itemRows[1].name).toBe('C')
    expect(vm.itemRows[0].sortOrder).toBe(0)
    expect(vm.itemRows[1].sortOrder).toBe(1)
  })

  it('moveItemRow 上移/下移', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.itemRows = [
      { name: 'A', sortOrder: 0 },
      { name: 'B', sortOrder: 1 },
      { name: 'C', sortOrder: 2 }
    ]
    vm.moveItemRow(1, 'up')
    expect(vm.itemRows[0].name).toBe('B')
    vm.moveItemRow(0, 'down')
    expect(vm.itemRows[0].name).toBe('A')
  })

  it('saveItems 空名称时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.currentStandard = { id: 1, name: 'X', projectType: '', standardVersion: '1.0', description: '', status: 1 }
    vm.itemRows = [{ name: '', requirement: '', testMethod: '', weight: 1, sortOrder: 0 }]
    await vm.saveItems()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('存在检查项名称为空，请补充')
    expect(apiMocks.updateAcceptanceStandard).not.toHaveBeenCalled()
  })

  it('saveItems 调用 updateAcceptanceStandard', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.currentStandard = { id: 1, name: 'X', projectType: '', standardVersion: '1.0', description: '', status: 1 }
    vm.itemRows = [{ name: '检查项', requirement: 'r', testMethod: 't', weight: 1, sortOrder: 0 }]
    await vm.saveItems()
    await flushPromises()
    expect(apiMocks.updateAcceptanceStandard).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('检查项已保存')
  })

  it('pageAcceptanceStandards 异常时不抛错', async () => {
    apiMocks.pageAcceptanceStandards.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.dataSource).toEqual([])
  })
})
