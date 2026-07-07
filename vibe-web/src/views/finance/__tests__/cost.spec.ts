/**
 * finance/cost 视图单元测试（Task 2.3）
 *
 * 覆盖：
 *   - 渲染（标题、表格、搜索）
 *   - onMounted 加载 pageFinanceCosts
 *   - 搜索 / 重置
 *   - openCreate / openEdit
 *   - handleSubmit 新建/编辑
 *   - handleDelete -> Modal.confirm
 *   - costTypeMap 包含 4 类型
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Cost from '../cost.vue'

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

/* ============ Mock @/api/finance ============ */
const apiMocks = vi.hoisted(() => ({
  pageFinanceCosts: vi.fn(),
  createFinanceCost: vi.fn(),
  updateFinanceCost: vi.fn(),
  deleteFinanceCost: vi.fn()
}))

vi.mock('@/api/finance', () => apiMocks)

describe('finance cost view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.pageFinanceCosts.mockResolvedValue({
      records: [
        {
          id: 1,
          projectId: 100,
          costType: 'LABOR',
          amount: 5000,
          costDate: '2026-07-01',
          refType: 'MANUAL',
          description: '人工成本'
        }
      ],
      total: 1
    })
    apiMocks.createFinanceCost.mockResolvedValue(2)
    apiMocks.updateFinanceCost.mockResolvedValue(undefined)
    apiMocks.deleteFinanceCost.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Cost, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('成本归集')
  })

  it('onMounted 调用 pageFinanceCosts', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.pageFinanceCosts).toHaveBeenCalledTimes(1)
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
    vm.query.costType = 'LABOR'
    vm.query.startDate = '2026-01-01'
    vm.query.endDate = '2026-12-31'
    await vm.handleReset()
    await flushPromises()
    expect(vm.query.projectId).toBeUndefined()
    expect(vm.query.costType).toBeUndefined()
    expect(vm.query.startDate).toBe('')
  })

  it('openCreate 重置 form 并打开弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    expect(vm.modalVisible).toBe(true)
    expect(vm.form.projectId).toBeUndefined()
    expect(vm.form.costType).toBe('LABOR')
  })

  it('openEdit 加载记录到 form', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openEdit({ id: 5, projectId: 100, costType: 'TRAVEL', amount: 1000, costDate: '2026-07-01', refType: 'MANUAL', description: 'r' })
    expect(vm.modalVisible).toBe(true)
    expect(vm.form.id).toBe(5)
    expect(vm.form.costType).toBe('TRAVEL')
    expect(vm.form.amount).toBe(1000)
  })

  it('handleSubmit 新建调用 createFinanceCost', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formRef = { validate: () => Promise.resolve() }
    vm.openCreate()
    vm.form.projectId = 200
    vm.form.costType = 'LABOR'
    vm.form.amount = 5000
    vm.form.costDate = '2026-07-01'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createFinanceCost).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
  })

  it('handleSubmit 编辑调用 updateFinanceCost', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formRef = { validate: () => Promise.resolve() }
    vm.openEdit({ id: 7, projectId: 100, costType: 'LABOR', amount: 1000, costDate: '2026-07-01', refType: 'MANUAL', description: '' })
    vm.form.amount = 2000
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.updateFinanceCost).toHaveBeenCalledWith(7, expect.objectContaining({ amount: 2000 }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('更新成功')
  })

  it('handleDelete onOk 调用 deleteFinanceCost', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 13 })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteFinanceCost).toHaveBeenCalledWith(13)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('costTypeMap 包含 4 种类型', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(Object.keys(vm.costTypeMap).length).toBe(4)
    expect(vm.costTypeMap.LABOR.label).toBe('人工成本')
    expect(vm.costTypeMap.TRAVEL.label).toBe('差旅费用')
    expect(vm.costTypeMap.AGENT.label).toBe('代理商费用')
    expect(vm.costTypeMap.OTHER.label).toBe('其他费用')
  })

  it('pageFinanceCosts 异常时不抛错', async () => {
    apiMocks.pageFinanceCosts.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.dataSource).toEqual([])
  })
})
