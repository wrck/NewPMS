/**
 * agent/settlement 视图单元测试（Task 2.2）
 *
 * 覆盖：
 *   - 渲染（标题、表格、搜索）
 *   - onMounted 加载 listWorkloads
 *   - 搜索 / 重置
 *   - openConfirm 加载 getTaskWorkload 后回填
 *   - handleConfirm 校验 + confirmWorkload 调用
 *   - openApprove / handleApprove 调用 approveWorkload
 *   - formatMoney 工具函数
 *   - statusMap 包含 8 状态
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Settlement from '../settlement.vue'

/* ============ Mock ant-design-vue message ============ */
const mocks = vi.hoisted(() => ({
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
  messageWarning: vi.fn()
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
    }
  }
})

/* ============ Mock @/api/agent ============ */
const apiMocks = vi.hoisted(() => ({
  listWorkloads: vi.fn(),
  getTaskWorkload: vi.fn(),
  confirmWorkload: vi.fn(),
  approveWorkload: vi.fn()
}))

vi.mock('@/api/agent', () => apiMocks)

describe('agent settlement view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    apiMocks.listWorkloads.mockResolvedValue({
      records: [
        {
          taskId: 1,
          projectName: '项目A',
          agentCompanyName: '代理商A',
          manDays: 5,
          siteCount: 2,
          deviceCount: 10,
          travelDays: 1,
          otherCost: 200,
          totalAmount: 5000,
          status: 'DRAFT',
          confirmByName: '张三'
        }
      ],
      total: 1
    })
    apiMocks.getTaskWorkload.mockResolvedValue({
      manDays: 5,
      siteCount: 2,
      deviceCount: 10,
      travelDays: 1,
      otherCost: 200,
      totalAmount: 5000,
      remark: '已有备注'
    })
    apiMocks.confirmWorkload.mockResolvedValue(undefined)
    apiMocks.approveWorkload.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Settlement, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('结算管理')
    expect(wrapper.text()).toContain('项目A')
    expect(wrapper.text()).toContain('代理商A')
  })

  it('onMounted 调用 listWorkloads', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.listWorkloads).toHaveBeenCalledTimes(1)
  })

  it('handleSearch 重置页码为 1', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.pagination.current = 5
    vm.query.agentCompanyId = 100
    await vm.handleSearch()
    await flushPromises()
    expect(vm.pagination.current).toBe(1)
  })

  it('openConfirm 调用 getTaskWorkload 并回填 confirmForm', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openConfirm({ taskId: 1, projectName: 'X' })
    await flushPromises()
    expect(apiMocks.getTaskWorkload).toHaveBeenCalledWith(1)
    expect(vm.confirmVisible).toBe(true)
    expect(vm.confirmForm.manDays).toBe(5)
    expect(vm.confirmForm.totalAmount).toBe(5000)
  })

  it('handleConfirm 缺人天或金额时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openConfirm({ taskId: 1 })
    await flushPromises()
    vm.confirmForm.manDays = 0
    vm.confirmForm.totalAmount = 0
    await vm.handleConfirm()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写人天和结算金额')
  })

  it('handleConfirm 调用 confirmWorkload', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openConfirm({ taskId: 1 })
    await flushPromises()
    vm.confirmForm.manDays = 5
    vm.confirmForm.totalAmount = 5000
    await vm.handleConfirm()
    await flushPromises()
    expect(apiMocks.confirmWorkload).toHaveBeenCalledWith(1, expect.objectContaining({ manDays: 5 }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('工作量已确认')
    expect(vm.confirmVisible).toBe(false)
  })

  it('openApprove 打开审批弹窗并设置 approved 标记', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openApprove({ taskId: 1 }, false)
    expect(vm.approveVisible).toBe(true)
    expect(vm.approveForm.approved).toBe(false)
  })

  it('handleApprove 驳回时缺原因 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openApprove({ taskId: 1 }, false)
    vm.approveForm.remark = ''
    await vm.handleApprove()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写驳回原因')
  })

  it('handleApprove 通过调用 approveWorkload(true)', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openApprove({ taskId: 1 }, true)
    vm.approveForm.remark = 'ok'
    await vm.handleApprove()
    await flushPromises()
    expect(apiMocks.approveWorkload).toHaveBeenCalledWith(1, true, 'ok')
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已审批通过')
  })

  it('handleApprove 驳回调用 approveWorkload(false, remark)', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openApprove({ taskId: 1 }, false)
    vm.approveForm.remark = '不合理'
    await vm.handleApprove()
    await flushPromises()
    expect(apiMocks.approveWorkload).toHaveBeenCalledWith(1, false, '不合理')
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已驳回')
  })

  it('formatMoney 格式化金额', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.formatMoney(1234.5)).toBe('¥1,234.50')
    expect(vm.formatMoney(undefined)).toBe('-')
    expect(vm.formatMoney(null)).toBe('-')
  })

  it('statusMap 包含 8 种状态', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(Object.keys(vm.statusMap).length).toBe(8)
    expect(vm.statusMap.DRAFT.label).toBe('草稿')
    expect(vm.statusMap.CLOSED.label).toBe('已关闭')
    expect(vm.statusMap.REJECTED.label).toBe('已驳回')
    expect(vm.statusMap.FINANCE_APPROVED.label).toBe('财务已审批')
  })

  it('listWorkloads 异常时不抛错', async () => {
    apiMocks.listWorkloads.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.dataSource).toEqual([])
  })
})
