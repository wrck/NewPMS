/**
 * delivery/field 视图单元测试（Task 2.5）
 *
 * 覆盖：
 *   - 渲染（标题、表格、搜索）
 *   - onMounted 加载 pageWorkOrders
 *   - 搜索
 *   - openCreate / addStep / handleSubmit
 *   - openDetail 调用 getWorkOrderDetail + listWorkOrderSteps/Photos/Issues
 *   - openConfirm / handleConfirm 调用 confirmWorkOrder
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Field from '../field.vue'

/* ============ Mock ant-design-vue message + Modal ============ */
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

/* ============ Mock @/api/delivery ============ */
const apiMocks = vi.hoisted(() => ({
  pageWorkOrders: vi.fn(),
  createWorkOrder: vi.fn(),
  getWorkOrderDetail: vi.fn(),
  listWorkOrderSteps: vi.fn(),
  listWorkOrderPhotos: vi.fn(),
  listWorkOrderIssues: vi.fn(),
  confirmWorkOrder: vi.fn()
}))

vi.mock('@/api/delivery', () => apiMocks)

describe('delivery field view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    apiMocks.pageWorkOrders.mockResolvedValue({
      records: [
        {
          id: 1,
          workOrderNo: 'WO-001',
          workOrderName: '设备安装',
          projectName: '项目A',
          executeMode: 'SELF',
          engineerName: '张三',
          priority: 'HIGH',
          stepProgress: 50,
          status: 'IN_PROGRESS',
          plannedStart: '2026-08-01'
        }
      ],
      total: 1
    })
    apiMocks.createWorkOrder.mockResolvedValue(2)
    apiMocks.getWorkOrderDetail.mockResolvedValue({
      id: 1,
      workOrderName: '设备安装',
      status: 'IN_PROGRESS'
    })
    apiMocks.listWorkOrderSteps.mockResolvedValue([
      { id: 10, stepOrder: 1, stepName: '签到', status: 'COMPLETED', estimatedMinutes: 30 }
    ])
    apiMocks.listWorkOrderPhotos.mockResolvedValue([
      { id: 20, url: '/photo/1.jpg', remark: '现场照片' }
    ])
    apiMocks.listWorkOrderIssues.mockResolvedValue([
      { id: 30, description: '设备故障', severity: 'HIGH', status: 'OPEN' }
    ])
    apiMocks.confirmWorkOrder.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Field, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('现场作业')
  })

  it('onMounted 调用 pageWorkOrders', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.pageWorkOrders).toHaveBeenCalledTimes(1)
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
    expect(vm.formData.workOrderName).toBe('')
    expect(vm.formData.executeMode).toBe('SELF')
    expect(vm.formData.priority).toBe('MEDIUM')
  })

  it('addStep 添加步骤到 standardSteps', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.stepInput.stepName = '步骤1'
    vm.stepInput.description = '描述'
    vm.stepInput.estimatedMinutes = 30
    vm.addStep()
    expect(vm.formData.standardSteps.length).toBe(1)
    expect(vm.formData.standardSteps[0].stepName).toBe('步骤1')
    expect(vm.stepInput.stepName).toBe('')
  })

  it('addStep 缺名称时不添加', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.stepInput.stepName = ''
    vm.addStep()
    expect(vm.formData.standardSteps.length).toBe(0)
  })

  it('handleSubmit 缺名称/项目时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.workOrderName = ''
    vm.formData.projectId = 0
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写工单名称和项目')
  })

  it('handleSubmit 调用 createWorkOrder', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.workOrderName = '新工单'
    vm.formData.projectId = 100
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createWorkOrder).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
    expect(vm.formVisible).toBe(false)
  })

  it('openDetail 调用 getWorkOrderDetail + steps/photos/issues', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openDetail({ id: 1, workOrderName: 'X' })
    await flushPromises()
    expect(apiMocks.getWorkOrderDetail).toHaveBeenCalledWith(1)
    expect(apiMocks.listWorkOrderSteps).toHaveBeenCalledWith(1)
    expect(apiMocks.listWorkOrderPhotos).toHaveBeenCalledWith(1)
    expect(apiMocks.listWorkOrderIssues).toHaveBeenCalledWith(1)
    expect(vm.drawerVisible).toBe(true)
    expect(vm.stepList.length).toBe(1)
    expect(vm.photoList.length).toBe(1)
    expect(vm.issueList.length).toBe(1)
  })

  it('openConfirm 打开确认弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openConfirm({ id: 5, workOrderName: 'X' })
    expect(vm.confirmVisible).toBe(true)
    expect(vm.confirmRow.id).toBe(5)
    expect(vm.confirmForm.approved).toBe(true)
  })

  it('handleConfirm 驳回缺原因时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openConfirm({ id: 5 })
    vm.confirmForm.approved = false
    vm.confirmForm.remark = ''
    await vm.handleConfirm()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写驳回原因')
  })

  it('handleConfirm 通过调用 confirmWorkOrder', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openConfirm({ id: 7 })
    vm.confirmForm.approved = true
    vm.confirmForm.remark = 'ok'
    vm.confirmForm.rating = 4
    await vm.handleConfirm()
    await flushPromises()
    expect(apiMocks.confirmWorkOrder).toHaveBeenCalledWith(7, expect.objectContaining({ approved: true, rating: 4 }))
    // 组件实际使用 '已确认完成' 而非 '工单已确认通过'
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已确认完成')
  })

  it('pageWorkOrders 异常时不抛错', async () => {
    apiMocks.pageWorkOrders.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.dataSource).toEqual([])
  })
})
