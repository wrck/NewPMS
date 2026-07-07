/**
 * finance/budget 视图单元测试（Task 2.3）
 *
 * 覆盖：
 *   - 渲染（标题、表格、搜索）
 *   - onMounted 加载 pageFinanceBudgets
 *   - 搜索 / 重置
 *   - openCreate / openEdit
 *   - handleSubmit 新建/编辑（含 form.validate）
 *   - handleDelete -> Modal.confirm
 *   - handleSubmitApproval -> Modal.confirm
 *   - handleApprove -> Modal.confirm
 *   - viewDetail 调用 getFinanceBudgetDetail
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Budget from '../budget.vue'

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
  pageFinanceBudgets: vi.fn(),
  createFinanceBudget: vi.fn(),
  updateFinanceBudget: vi.fn(),
  deleteFinanceBudget: vi.fn(),
  submitFinanceBudget: vi.fn(),
  approveFinanceBudget: vi.fn(),
  getFinanceBudgetDetail: vi.fn()
}))

vi.mock('@/api/finance', () => apiMocks)

describe('finance budget view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.pageFinanceBudgets.mockResolvedValue({
      records: [
        {
          id: 1,
          projectId: 100,
          year: 2026,
          laborAmount: 100000,
          travelAmount: 20000,
          agentAmount: 30000,
          otherAmount: 5000,
          totalAmount: 155000,
          approvalStatus: 'DRAFT'
        }
      ],
      total: 1
    })
    apiMocks.createFinanceBudget.mockResolvedValue(2)
    apiMocks.updateFinanceBudget.mockResolvedValue(undefined)
    apiMocks.deleteFinanceBudget.mockResolvedValue(undefined)
    apiMocks.submitFinanceBudget.mockResolvedValue(undefined)
    apiMocks.approveFinanceBudget.mockResolvedValue(undefined)
    apiMocks.getFinanceBudgetDetail.mockResolvedValue({
      id: 1,
      projectId: 100,
      year: 2026,
      laborAmount: 100000,
      travelAmount: 20000,
      agentAmount: 30000,
      otherAmount: 5000,
      totalAmount: 155000,
      approvalStatus: 'DRAFT',
      actualLaborAmount: 90000,
      actualTravelAmount: 18000,
      actualAgentAmount: 28000,
      actualOtherAmount: 4000,
      actualTotalAmount: 140000
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Budget, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('预算')
  })

  it('onMounted 调用 pageFinanceBudgets', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.pageFinanceBudgets).toHaveBeenCalledTimes(1)
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
    vm.query.year = 2025
    await vm.handleReset()
    await flushPromises()
    expect(vm.query.projectId).toBeUndefined()
    expect(vm.query.year).toBeUndefined()
  })

  it('openCreate 重置 form 并打开弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    expect(vm.modalVisible).toBe(true)
    expect(vm.form.projectId).toBeUndefined()
    expect(vm.form.laborAmount).toBe(0)
  })

  it('openEdit 加载记录到 form', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openEdit({ id: 5, projectId: 100, year: 2026, laborAmount: 1000, travelAmount: 200, agentAmount: 300, otherAmount: 50, remark: 'r' })
    expect(vm.modalVisible).toBe(true)
    expect(vm.form.id).toBe(5)
    expect(vm.form.projectId).toBe(100)
    expect(vm.form.laborAmount).toBe(1000)
  })

  it('handleSubmit 调用 createFinanceBudget 新建', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    // mock formRef.validate
    vm.formRef = { validate: () => Promise.resolve() }
    vm.openCreate()
    vm.form.projectId = 200
    vm.form.year = 2026
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createFinanceBudget).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
  })

  it('handleSubmit 调用 updateFinanceBudget 编辑', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formRef = { validate: () => Promise.resolve() }
    vm.openEdit({ id: 7, projectId: 100, year: 2026, laborAmount: 1000, travelAmount: 0, agentAmount: 0, otherAmount: 0, remark: '' })
    vm.form.laborAmount = 2000
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.updateFinanceBudget).toHaveBeenCalledWith(7, expect.objectContaining({ laborAmount: 2000 }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('更新成功')
  })

  it('handleDelete 触发 Modal.confirm onOk 调用 deleteFinanceBudget', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 13, projectId: 100, year: 2026 })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteFinanceBudget).toHaveBeenCalledWith(13)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('handleSubmitApproval onOk 调用 submitFinanceBudget', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleSubmitApproval({ id: 5, projectId: 100, year: 2026 })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.submitFinanceBudget).toHaveBeenCalledWith(5)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已提交审批')
  })

  it('handleApprove onOk 调用 approveFinanceBudget', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleApprove({ id: 5, projectId: 100, year: 2026 }, true)
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.approveFinanceBudget).toHaveBeenCalledWith(5, true)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('审批完成')
  })

  it('viewDetail 调用 getFinanceBudgetDetail', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.viewDetail({ id: 1, projectId: 100, year: 2026 })
    await flushPromises()
    expect(apiMocks.getFinanceBudgetDetail).toHaveBeenCalledWith(1)
    expect(vm.detailVisible).toBe(true)
  })

  it('pageFinanceBudgets 异常时不抛错', async () => {
    apiMocks.pageFinanceBudgets.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.dataSource).toEqual([])
  })
})
