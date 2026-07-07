/**
 * agent/outsource 视图单元测试（Task 2.2）
 *
 * 覆盖：
 *   - 渲染（标题、表格、搜索）
 *   - onMounted 加载 pageOutsourceTasks
 *   - 搜索 / 重置
 *   - openCreate / handleSubmit 新建转包任务
 *   - openRespond / handleRespond 接单/拒绝
 *   - openAssign / handleAssign 指派工程师
 *   - openDeliverables 调用 listDeliverables
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Outsource from '../outsource.vue'

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
  pageOutsourceTasks: vi.fn(),
  createOutsourceTask: vi.fn(),
  respondOutsourceTask: vi.fn(),
  assignAgentEngineer: vi.fn(),
  listAgentEngineers: vi.fn(),
  listDeliverables: vi.fn()
}))

vi.mock('@/api/agent', () => apiMocks)

describe('agent outsource view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    apiMocks.pageOutsourceTasks.mockResolvedValue({
      records: [
        {
          id: 1,
          taskCode: 'OT-001',
          projectName: '项目A',
          taskName: '转包任务',
          agentCompanyId: 5,
          agentCompanyName: '代理商A',
          agentEngineerId: 10,
          agentEngineerName: '工程师',
          deadline: '2026-08-01',
          submitCount: 0,
          status: 'PENDING'
        }
      ],
      total: 1
    })
    apiMocks.createOutsourceTask.mockResolvedValue(2)
    apiMocks.respondOutsourceTask.mockResolvedValue(undefined)
    apiMocks.assignAgentEngineer.mockResolvedValue(undefined)
    apiMocks.listAgentEngineers.mockResolvedValue([
      { id: 10, name: '工程师A', phone: '13900000000' }
    ])
    apiMocks.listDeliverables.mockResolvedValue([
      { id: 100, deliverableType: 'PHOTO', name: '现场照片.jpg', uploadedByName: '工程师', uploadedAt: '2026-07-01', url: '/files/1' }
    ])
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Outsource, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('转包任务')
    expect(wrapper.text()).toContain('OT-001')
    expect(wrapper.text()).toContain('转包任务')
  })

  it('onMounted 调用 pageOutsourceTasks', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.pageOutsourceTasks).toHaveBeenCalledTimes(1)
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
    expect(vm.formData.projectId).toBe(0)
    expect(vm.formData.taskScope).toBe('')
  })

  it('handleSubmit 缺必填项时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.projectId = 0
    vm.formData.agentCompanyId = 0
    vm.formData.taskScope = ''
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请完整填写项目、代理商和任务范围')
  })

  it('handleSubmit 调用 createOutsourceTask', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.projectId = 10
    vm.formData.agentCompanyId = 5
    vm.formData.taskScope = '安装 10 台设备'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createOutsourceTask).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
    expect(vm.formVisible).toBe(false)
  })

  it('openRespond 打开接单弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openRespond({ id: 1, taskName: 'X' }, true)
    expect(vm.respondVisible).toBe(true)
    expect(vm.respondForm.accepted).toBe(true)
  })

  it('handleRespond 接单调用 respondOutsourceTask(true)', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openRespond({ id: 5, taskName: 'X' }, true)
    vm.respondForm.remark = '接受'
    await vm.handleRespond()
    await flushPromises()
    expect(apiMocks.respondOutsourceTask).toHaveBeenCalledWith(5, true, '接受')
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已接单')
  })

  it('handleRespond 拒绝调用 respondOutsourceTask(false)', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openRespond({ id: 6, taskName: 'X' }, false)
    vm.respondForm.remark = '能力不足'
    await vm.handleRespond()
    await flushPromises()
    expect(apiMocks.respondOutsourceTask).toHaveBeenCalledWith(6, false, '能力不足')
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已拒绝')
  })

  it('openAssign 调用 listAgentEngineers', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openAssign({ id: 1, agentCompanyId: 5, agentEngineerId: 10 })
    await flushPromises()
    expect(apiMocks.listAgentEngineers).toHaveBeenCalledWith(5)
    expect(vm.assignVisible).toBe(true)
  })

  it('handleAssign 缺工程师时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.assignRow = { id: 1 }
    vm.assignEngineerId = undefined
    await vm.handleAssign()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请选择工程师')
  })

  it('handleAssign 调用 assignAgentEngineer', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.assignRow = { id: 7 }
    vm.assignEngineerId = 10
    await vm.handleAssign()
    await flushPromises()
    expect(apiMocks.assignAgentEngineer).toHaveBeenCalledWith(7, 10)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已指派工程师')
  })

  it('openDeliverables 调用 listDeliverables', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openDeliverables({ id: 9, taskName: 'X' })
    await flushPromises()
    expect(apiMocks.listDeliverables).toHaveBeenCalledWith(9)
    expect(vm.deliverableList.length).toBe(1)
  })

  it('pageOutsourceTasks 异常时不抛错', async () => {
    apiMocks.pageOutsourceTasks.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.dataSource).toEqual([])
  })
})
