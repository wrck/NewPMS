/**
 * delivery/board 视图单元测试（Task 2.5）
 *
 * 覆盖：
 *   - 渲染（标题、看板列）
 *   - onMounted 加载 pageWorkOrders
 *   - 按状态分组 grouped
 *   - stats 统计（total/done/ongoing/pending/overdue）
 *   - isOverdue 判断
 *   - handleComplete -> Modal.confirm + completeWorkOrder
 *   - openConfirm 打开弹窗 / handleConfirmSubmit 调用 confirmWorkOrder
 *   - onCardClick 跳转现场作业
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Board from '../board.vue'

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

/* ============ Mock @/api/delivery ============ */
const apiMocks = vi.hoisted(() => ({
  pageWorkOrders: vi.fn(),
  completeWorkOrder: vi.fn(),
  confirmWorkOrder: vi.fn()
}))

vi.mock('@/api/delivery', () => apiMocks)

/* ============ Mock vue-router ============ */
const routerPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush })
}))

describe('delivery board view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.pageWorkOrders.mockResolvedValue({
      records: [
        { id: 1, workOrderName: '工单1', status: 'PENDING', priority: 'HIGH', plannedEnd: '2026-08-01' },
        { id: 2, workOrderName: '工单2', status: 'IN_PROGRESS', priority: 'MEDIUM', plannedEnd: '2026-08-10' },
        { id: 3, workOrderName: '工单3', status: 'CONFIRMED', priority: 'LOW', plannedEnd: '2026-07-01' },
        { id: 4, workOrderName: '工单4', status: 'IN_PROGRESS', priority: 'URGENT', plannedEnd: '2020-01-01' }
      ]
    })
    apiMocks.completeWorkOrder.mockResolvedValue(undefined)
    apiMocks.confirmWorkOrder.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Board, {})
  }

  it('渲染标题与看板列', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('待分配')
    expect(wrapper.text()).toContain('进行中')
    expect(wrapper.text()).toContain('已确认')
  })

  it('onMounted 调用 pageWorkOrders', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.pageWorkOrders).toHaveBeenCalledTimes(1)
  })

  it('渲染工单卡片', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('工单1')
    expect(wrapper.text()).toContain('工单2')
    expect(wrapper.text()).toContain('工单3')
  })

  it('grouped 按状态分组', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.grouped.PENDING.length).toBe(1)
    expect(vm.grouped.IN_PROGRESS.length).toBe(2)
    expect(vm.grouped.CONFIRMED.length).toBe(1)
    expect(vm.grouped.ASSIGNED.length).toBe(0)
  })

  it('stats 统计正确', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.stats.total).toBe(4)
    expect(vm.stats.done).toBe(1)
    expect(vm.stats.ongoing).toBe(2)
    expect(vm.stats.pending).toBe(1)
    expect(vm.stats.overdue).toBe(1) // 工单4 已超期
  })

  it('isOverdue 已完成工单返回 false', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.isOverdue({ status: 'CONFIRMED', plannedEnd: '2020-01-01' })).toBe(false)
    expect(vm.isOverdue({ status: 'COMPLETED', plannedEnd: '2020-01-01' })).toBe(false)
  })

  it('isOverdue 未完成且超期返回 true', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.isOverdue({ status: 'IN_PROGRESS', plannedEnd: '2020-01-01' })).toBe(true)
    expect(vm.isOverdue({ status: 'IN_PROGRESS', plannedEnd: '2999-12-31' })).toBe(false)
  })

  it('isOverdue 无计划结束时间返回 false', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.isOverdue({ status: 'PENDING', plannedEnd: undefined })).toBe(false)
    expect(vm.isOverdue({ status: 'PENDING', plannedEnd: '' })).toBe(false)
  })

  it('handleComplete 触发 Modal.confirm', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const before = mocks.modalConfirmCallbacks.length
    const evt = { stopPropagation: vi.fn() } as any
    vm.handleComplete({ id: 5, workOrderName: 'X' }, evt)
    expect(mocks.modalConfirmCallbacks.length).toBeGreaterThan(before)
  })

  it('handleComplete onOk 调用 completeWorkOrder', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const evt = { stopPropagation: vi.fn() } as any
    vm.handleComplete({ id: 7, workOrderName: 'X' }, evt)
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.completeWorkOrder).toHaveBeenCalledWith(7)
    expect(mocks.messageSuccess).toHaveBeenCalled()
  })

  it('openConfirm 打开确认弹窗并初始化 form', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const evt = { stopPropagation: vi.fn() } as any
    vm.openConfirm({ id: 9, workOrderName: 'X' }, evt)
    expect(vm.confirmVisible).toBe(true)
    expect(vm.currentOrder.id).toBe(9)
    expect(vm.confirmForm.approved).toBe(true)
    expect(vm.confirmForm.rating).toBe(5)
  })

  it('handleConfirmSubmit 调用 confirmWorkOrder', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.currentOrder = { id: 11 }
    vm.confirmForm.approved = true
    vm.confirmForm.remark = 'ok'
    vm.confirmForm.rating = 4
    await vm.handleConfirmSubmit()
    await flushPromises()
    expect(apiMocks.confirmWorkOrder).toHaveBeenCalledWith(11, expect.objectContaining({ approved: true, rating: 4 }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('工单已确认通过')
    expect(vm.confirmVisible).toBe(false)
  })

  it('onCardClick 跳转现场作业', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.onCardClick({ id: 13 })
    expect(routerPush).toHaveBeenCalledWith({ path: '/delivery/field', query: { id: 13 } })
  })

  it('pageWorkOrders 异常时显示错误消息', async () => {
    apiMocks.pageWorkOrders.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    expect(mocks.messageError).toHaveBeenCalled()
    const vm = wrapper.vm as any
    expect(vm.allData).toEqual([])
  })
})
