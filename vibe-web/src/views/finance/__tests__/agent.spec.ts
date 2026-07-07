/**
 * finance/agent 视图单元测试（Task 2.3）
 *
 * 覆盖：
 *   - 渲染（标题、表格、搜索）
 *   - onMounted 加载 pageFinanceSettlements
 *   - 搜索 / 重置
 *   - openCreate / openEdit
 *   - handleSubmit 新建/编辑
 *   - handleDelete -> Modal.confirm
 *   - handleAction 多状态流转（pm/agent/director-pass/finance-pass/paying/paid）
 *   - approvalStatusMap / paymentStatusMap 映射
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Agent from '../agent.vue'

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
  pageFinanceSettlements: vi.fn(),
  createFinanceSettlement: vi.fn(),
  updateFinanceSettlement: vi.fn(),
  deleteFinanceSettlement: vi.fn(),
  pmConfirmSettlement: vi.fn(),
  agentConfirmSettlement: vi.fn(),
  directorApproveSettlement: vi.fn(),
  financeApproveSettlement: vi.fn(),
  updateSettlementPaymentStatus: vi.fn()
}))

vi.mock('@/api/finance', () => apiMocks)

describe('finance agent settlement view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.pageFinanceSettlements.mockResolvedValue({
      records: [
        {
          id: 1,
          projectId: 100,
          agentCompanyId: 5,
          period: '2026-07',
          workloadDays: 10,
          totalAmount: 50000,
          approvalStatus: 'DRAFT',
          paymentStatus: 'UNPAID'
        }
      ],
      total: 1
    })
    apiMocks.createFinanceSettlement.mockResolvedValue(2)
    apiMocks.updateFinanceSettlement.mockResolvedValue(undefined)
    apiMocks.deleteFinanceSettlement.mockResolvedValue(undefined)
    apiMocks.pmConfirmSettlement.mockResolvedValue(undefined)
    apiMocks.agentConfirmSettlement.mockResolvedValue(undefined)
    apiMocks.directorApproveSettlement.mockResolvedValue(undefined)
    apiMocks.financeApproveSettlement.mockResolvedValue(undefined)
    apiMocks.updateSettlementPaymentStatus.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Agent, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('代理商结算')
  })

  it('onMounted 调用 pageFinanceSettlements', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.pageFinanceSettlements).toHaveBeenCalledTimes(1)
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
    vm.query.period = '2026-01'
    vm.query.approvalStatus = 'DRAFT'
    await vm.handleReset()
    await flushPromises()
    expect(vm.query.projectId).toBeUndefined()
    expect(vm.query.period).toBe('')
    expect(vm.query.approvalStatus).toBeUndefined()
  })

  it('openCreate 重置 form 并打开弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    expect(vm.modalVisible).toBe(true)
    expect(vm.form.projectId).toBeUndefined()
    expect(vm.form.workloadDays).toBe(0)
  })

  it('openEdit 加载记录到 form', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openEdit({ id: 5, projectId: 100, agentCompanyId: 5, period: '2026-07', workloadDays: 10, unitPrice: 5000, travelAmount: 1000, otherAmount: 200, remark: 'r' })
    expect(vm.modalVisible).toBe(true)
    expect(vm.form.id).toBe(5)
    expect(vm.form.workloadDays).toBe(10)
  })

  it('handleSubmit 新建调用 createFinanceSettlement', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formRef = { validate: () => Promise.resolve() }
    vm.openCreate()
    vm.form.projectId = 200
    vm.form.agentCompanyId = 5
    vm.form.period = '2026-07'
    vm.form.workloadDays = 5
    vm.form.unitPrice = 1000
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createFinanceSettlement).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
  })

  it('handleSubmit 编辑调用 updateFinanceSettlement', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formRef = { validate: () => Promise.resolve() }
    vm.openEdit({ id: 7, projectId: 100, agentCompanyId: 5, period: '2026-07', workloadDays: 10, unitPrice: 5000, travelAmount: 0, otherAmount: 0, remark: '' })
    vm.form.workloadDays = 20
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.updateFinanceSettlement).toHaveBeenCalledWith(7, expect.objectContaining({ workloadDays: 20 }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('更新成功')
  })

  it('handleDelete onOk 调用 deleteFinanceSettlement', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 13 })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteFinanceSettlement).toHaveBeenCalledWith(13)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('handleAction pm 调用 pmConfirmSettlement', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.handleAction({ id: 5 }, 'pm')
    await flushPromises()
    expect(apiMocks.pmConfirmSettlement).toHaveBeenCalledWith(5)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('操作成功')
  })

  it('handleAction director-pass 调用 directorApproveSettlement(true)', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.handleAction({ id: 5 }, 'director-pass')
    await flushPromises()
    expect(apiMocks.directorApproveSettlement).toHaveBeenCalledWith(5, true)
  })

  it('handleAction finance-pass 调用 financeApproveSettlement(true)', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.handleAction({ id: 5 }, 'finance-pass')
    await flushPromises()
    expect(apiMocks.financeApproveSettlement).toHaveBeenCalledWith(5, true)
  })

  it('handleAction paying 调用 updateSettlementPaymentStatus', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.handleAction({ id: 5 }, 'paying')
    await flushPromises()
    expect(apiMocks.updateSettlementPaymentStatus).toHaveBeenCalledWith(5, 'PAYING')
  })

  it('handleAction paid 调用 updateSettlementPaymentStatus PAID', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.handleAction({ id: 5 }, 'paid')
    await flushPromises()
    expect(apiMocks.updateSettlementPaymentStatus).toHaveBeenCalledWith(5, 'PAID')
  })

  it('approvalStatusMap 包含 7 种状态', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(Object.keys(vm.approvalStatusMap).length).toBe(7)
    expect(vm.approvalStatusMap.DRAFT.label).toBe('草稿')
    expect(vm.approvalStatusMap.FINANCE_APPROVED.label).toBe('财务已审批')
  })

  it('paymentStatusMap 包含 3 种状态', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(Object.keys(vm.paymentStatusMap).length).toBe(3)
    expect(vm.paymentStatusMap.UNPAID.label).toBe('未付款')
    expect(vm.paymentStatusMap.PAID.label).toBe('已付款')
  })

  it('pageFinanceSettlements 异常时不抛错', async () => {
    apiMocks.pageFinanceSettlements.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.dataSource).toEqual([])
  })
})
