/**
 * delivery/cutover 视图单元测试（Task 2.5）
 *
 * 覆盖：
 *   - 渲染（标题、表格、搜索）
 *   - onMounted 加载 pageCutoverPlans
 *   - 搜索
 *   - openCreate / addStep / handleSubmit
 *   - openDetail 调用 getCutoverPlanDetail
 *   - handleDelete -> Modal.confirm
 *   - openInternalApproval / handleInternalApproval
 *   - openStepExec / handleStepExec（4 种 mode）
 *   - openComplete / handleComplete
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Cutover from '../cutover.vue'

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

/* ============ Mock @/api/cutover ============ */
const apiMocks = vi.hoisted(() => ({
  pageCutoverPlans: vi.fn(),
  getCutoverPlanDetail: vi.fn(),
  createCutoverPlan: vi.fn(),
  updateCutoverPlan: vi.fn(),
  deleteCutoverPlan: vi.fn(),
  submitInternalApproval: vi.fn(),
  internalApprovePlan: vi.fn(),
  internalRejectPlan: vi.fn(),
  startCustomerApproval: vi.fn(),
  startCutoverExecution: vi.fn(),
  executeCutoverStep: vi.fn(),
  rollbackCutoverStep: vi.fn(),
  exceptionCutoverStep: vi.fn(),
  completeCutoverPlan: vi.fn(),
  abortCutoverPlan: vi.fn(),
  CutoverPlanStatusLabel: { DRAFT: '草稿', PENDING_INTERNAL_APPROVAL: '待内部审批', INTERNAL_APPROVED: '内部通过', INTERNAL_REJECTED: '内部驳回', PENDING_CUSTOMER_APPROVAL: '待客户审批', CUSTOMER_APPROVED: '客户通过', CUSTOMER_REJECTED: '客户驳回', EXECUTING: '执行中', COMPLETED: '已完成', ABORTED: '已中止' },
  CutoverStepStatusLabel: { PENDING: '待执行', EXECUTING: '执行中', COMPLETED: '已完成', ROLLED_BACK: '已回退', ABORTED: '已中止' }
}))

vi.mock('@/api/cutover', () => apiMocks)

describe('delivery cutover view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.pageCutoverPlans.mockResolvedValue({
      records: [
        {
          id: 1,
          planName: '割接方案A',
          projectName: '项目A',
          cutoverDate: '2026-08-01',
          startTime: '2026-08-01 22:00',
          endTime: '2026-08-02 02:00',
          stepCount: 3,
          completedStepCount: 1,
          status: 'DRAFT'
        }
      ],
      total: 1
    })
    apiMocks.getCutoverPlanDetail.mockResolvedValue({
      id: 1,
      planName: '割接方案A',
      projectId: 100,
      cutoverDate: '2026-08-01',
      startTime: '2026-08-01 22:00',
      endTime: '2026-08-02 02:00',
      impactScope: '核心业务',
      emergencyContact: '张三',
      remark: '',
      steps: [
        { id: 10, sortOrder: 0, stepName: '步骤1', estimatedDuration: 30, ownerName: '工程师', status: 'COMPLETED' },
        { id: 11, sortOrder: 1, stepName: '步骤2', estimatedDuration: 60, ownerName: '工程师', status: 'PENDING' }
      ],
      logs: []
    })
    apiMocks.createCutoverPlan.mockResolvedValue(2)
    apiMocks.updateCutoverPlan.mockResolvedValue(undefined)
    apiMocks.deleteCutoverPlan.mockResolvedValue(undefined)
    apiMocks.internalApprovePlan.mockResolvedValue(undefined)
    apiMocks.internalRejectPlan.mockResolvedValue(undefined)
    apiMocks.executeCutoverStep.mockResolvedValue(undefined)
    apiMocks.rollbackCutoverStep.mockResolvedValue(undefined)
    apiMocks.exceptionCutoverStep.mockResolvedValue(undefined)
    apiMocks.completeCutoverPlan.mockResolvedValue(undefined)
    apiMocks.abortCutoverPlan.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Cutover, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('割接')
  })

  it('onMounted 调用 pageCutoverPlans', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.pageCutoverPlans).toHaveBeenCalledTimes(1)
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

  it('openCreate 重置 formData 并打开弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(false)
    expect(vm.formData.planName).toBe('')
    expect(vm.formData.steps).toEqual([])
  })

  it('addStep 缺名称时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.stepInput.stepName = ''
    vm.addStep()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写步骤名称')
  })

  it('addStep 添加步骤到 formData.steps', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.stepInput.stepName = '新步骤'
    vm.stepInput.estimatedDuration = 30
    vm.addStep()
    expect(vm.formData.steps.length).toBe(1)
    expect(vm.formData.steps[0].stepName).toBe('新步骤')
    expect(vm.stepInput.stepName).toBe('')
  })

  it('handleSubmit 缺必填项时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.planName = ''
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写方案名称、项目、割接日期和起止时间')
  })

  it('handleSubmit 缺步骤时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.planName = '方案'
    vm.formData.projectId = 100
    vm.formData.cutoverDate = '2026-08-01'
    vm.formData.startTime = '2026-08-01 22:00'
    vm.formData.endTime = '2026-08-02 02:00'
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请至少添加一个割接步骤')
  })

  it('handleSubmit 开始时间晚于结束时间时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.planName = '方案'
    vm.formData.projectId = 100
    vm.formData.cutoverDate = '2026-08-01'
    vm.formData.startTime = '2026-08-02 06:00'
    vm.formData.endTime = '2026-08-02 02:00'
    vm.formData.steps = [{ stepName: 'X' }]
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('开始时间必须早于结束时间')
  })

  it('handleSubmit 新建调用 createCutoverPlan', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.planName = '方案'
    vm.formData.projectId = 100
    vm.formData.cutoverDate = '2026-08-01'
    vm.formData.startTime = '2026-08-01 22:00'
    vm.formData.endTime = '2026-08-02 02:00'
    vm.formData.steps = [{ stepName: 'X' }]
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createCutoverPlan).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
  })

  it('handleDelete onOk 调用 deleteCutoverPlan', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 13, planName: 'X' })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteCutoverPlan).toHaveBeenCalledWith(13)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('openDetail 调用 getCutoverPlanDetail', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openDetail({ id: 1, planName: 'X' })
    await flushPromises()
    expect(apiMocks.getCutoverPlanDetail).toHaveBeenCalledWith(1)
    expect(vm.drawerVisible).toBe(true)
    expect(vm.currentDetail.id).toBe(1)
  })

  it('openInternalApproval 打开内部审批弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openInternalApproval({ id: 5, planName: 'X' })
    expect(vm.internalApprovalVisible).toBe(true)
    expect(vm.internalApprovalForm.planId).toBe(5)
    expect(vm.internalApprovalForm.result).toBe('APPROVED')
  })

  it('handleInternalApproval 通过调用 internalApprovePlan', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openInternalApproval({ id: 5, planName: 'X' })
    vm.internalApprovalForm.result = 'APPROVED'
    vm.internalApprovalForm.remark = 'ok'
    await vm.handleInternalApproval()
    await flushPromises()
    expect(apiMocks.internalApprovePlan).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已通过内部审批')
  })

  it('handleInternalApproval 驳回调用 internalRejectPlan', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openInternalApproval({ id: 5, planName: 'X' })
    vm.internalApprovalForm.result = 'REJECTED'
    vm.internalApprovalForm.remark = '不合理'
    await vm.handleInternalApproval()
    await flushPromises()
    expect(apiMocks.internalRejectPlan).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已驳回内部审批')
  })

  it('handleStepExec start 调用 executeCutoverStep', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.currentDetail = { id: 1 }
    vm.openStepExec({ id: 10 }, 'start')
    expect(vm.stepExecMode).toBe('start')
    await vm.handleStepExec()
    await flushPromises()
    expect(apiMocks.executeCutoverStep).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('步骤已开始执行')
  })

  it('handleStepExec rollback 调用 rollbackCutoverStep', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.currentDetail = { id: 1 }
    vm.openStepExec({ id: 11 }, 'rollback')
    await vm.handleStepExec()
    await flushPromises()
    expect(apiMocks.rollbackCutoverStep).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('步骤已回退')
  })

  it('handleStepExec exception 调用 exceptionCutoverStep', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.currentDetail = { id: 1 }
    vm.openStepExec({ id: 12 }, 'exception')
    await vm.handleStepExec()
    await flushPromises()
    expect(apiMocks.exceptionCutoverStep).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('步骤已标记异常')
  })

  it('openComplete 打开完成总结弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.currentDetail = { id: 1 }
    vm.openComplete()
    expect(vm.completeVisible).toBe(true)
    expect(vm.completeForm.planId).toBe(1)
  })

  it('handleComplete 缺总结时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.currentDetail = { id: 1 }
    vm.openComplete()
    vm.completeForm.summary = ''
    await vm.handleComplete()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写执行总结')
  })

  it('handleComplete 调用 completeCutoverPlan', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.currentDetail = { id: 1 }
    vm.openComplete()
    vm.completeForm.summary = '执行完成'
    vm.completeForm.problemImprovement = '改进'
    await vm.handleComplete()
    await flushPromises()
    expect(apiMocks.completeCutoverPlan).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('割接已完成')
  })

  it('pageCutoverPlans 异常时不抛错', async () => {
    apiMocks.pageCutoverPlans.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.dataSource).toEqual([])
  })
})
