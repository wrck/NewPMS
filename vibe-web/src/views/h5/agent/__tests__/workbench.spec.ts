/**
 * h5/agent/workbench 视图单元测试（Task 2.8）
 *
 * 覆盖：
 *   - 渲染（标题、统计卡片、待接单/进行中/待审核任务）
 *   - onMounted 调用 getAgentWorkbench + getAgentUnreadCount
 *   - summary / pendingTasks / inProgressTasks / submittedTasks computed
 *   - onAccept -> Modal.confirm + respondOutsourceTask(true) + refreshWorkbench
 *   - onReject -> Modal.confirm + respondOutsourceTask(false, reason)
 *   - formatDateTime 工具
 *   - 401/40301 异常跳转登录
 *   - 其他异常 message.error
 *   - goMessages / goTaskDetail / goSubmitDeliverable 路由跳转
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import WorkbenchView from '../workbench.vue'

/* ============ Mock vue-router ============ */
const routerMocks = vi.hoisted(() => ({
  push: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerMocks.push })
}))

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

/* ============ Mock @/api/agentPortal ============ */
const portalMocks = vi.hoisted(() => ({
  getAgentWorkbench: vi.fn(),
  getAgentUnreadCount: vi.fn()
}))

vi.mock('@/api/agentPortal', () => portalMocks)

/* ============ Mock @/api/agent（respondOutsourceTask） ============ */
const agentMocks = vi.hoisted(() => ({
  respondOutsourceTask: vi.fn()
}))

vi.mock('@/api/agent', () => agentMocks)

/* ============ Stub H5Layout ============ */
const stubs = {
  H5Layout: {
    template: '<div class="stub-h5-layout"><slot /><slot name="right" /></div>',
    props: ['title']
  }
}

describe('h5 agent workbench view', () => {
  const mockWorkbench = {
    summary: {
      pendingCount: 3,
      inProgressCount: 5,
      submittedCount: 2,
      overdueCount: 1
    },
    pendingTasks: [
      {
        id: 101,
        taskName: '待接单任务A',
        projectName: '项目A',
        deadline: '2026-07-15'
      }
    ],
    inProgressTasks: [
      {
        id: 201,
        taskName: '进行中任务B',
        projectName: '项目B',
        agentEngineerName: '工程师张三',
        deadline: '2026-07-20'
      }
    ],
    submittedTasks: [
      {
        id: 301,
        taskName: '待审核任务C',
        projectName: '项目C',
        updateTime: '2026-07-01T10:30:00',
        submitCount: 2
      }
    ]
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    portalMocks.getAgentWorkbench.mockResolvedValue(mockWorkbench)
    portalMocks.getAgentUnreadCount.mockResolvedValue(5)
    agentMocks.respondOutsourceTask.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(WorkbenchView, { global: { stubs } })
  }

  it('渲染标题与统计卡片', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('待接单')
    expect(wrapper.text()).toContain('进行中')
    expect(wrapper.text()).toContain('待审核')
    expect(wrapper.text()).toContain('已超期')
  })

  it('onMounted 调用 getAgentWorkbench 与 getAgentUnreadCount', async () => {
    mountView()
    await flushPromises()
    expect(portalMocks.getAgentWorkbench).toHaveBeenCalledTimes(1)
    expect(portalMocks.getAgentUnreadCount).toHaveBeenCalledTimes(1)
  })

  it('summary computed 返回 summary 数据', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.summary.pendingCount).toBe(3)
    expect(vm.summary.inProgressCount).toBe(5)
    expect(vm.summary.submittedCount).toBe(2)
    expect(vm.summary.overdueCount).toBe(1)
  })

  it('pendingTasks / inProgressTasks / submittedTasks computed', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.pendingTasks.length).toBe(1)
    expect(vm.pendingTasks[0].taskName).toBe('待接单任务A')
    expect(vm.inProgressTasks.length).toBe(1)
    expect(vm.inProgressTasks[0].agentEngineerName).toBe('工程师张三')
    expect(vm.submittedTasks.length).toBe(1)
    expect(vm.submittedTasks[0].submitCount).toBe(2)
  })

  it('unreadCount 已加载', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.unreadCount).toBe(5)
  })

  it('onAccept 触发 Modal.confirm onOk 调用 respondOutsourceTask(true)', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.onAccept({ id: 101, taskName: '待接单任务A', projectName: '项目A' })
    expect(mocks.modalConfirmCallbacks.length).toBe(1)
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(agentMocks.respondOutsourceTask).toHaveBeenCalledWith(101, true)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('接单成功')
    // refreshWorkbench 调用 getAgentWorkbench
    expect(portalMocks.getAgentWorkbench).toHaveBeenCalled()
  })

  it('onReject 触发 Modal.confirm onOk 调用 respondOutsourceTask(false, reason)', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.onReject({ id: 101, taskName: '待接单任务A', projectName: '项目A' })
    expect(mocks.modalConfirmCallbacks.length).toBe(1)
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(agentMocks.respondOutsourceTask).toHaveBeenCalledWith(101, false, undefined)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已拒绝')
  })

  it('onAccept 异常时 message.error', async () => {
    agentMocks.respondOutsourceTask.mockRejectedValueOnce(new Error('接单失败'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.onAccept({ id: 101, taskName: '任务A', projectName: '项目A' })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(mocks.messageError).toHaveBeenCalled()
  })

  it('getAgentWorkbench 401 异常时跳转登录', async () => {
    portalMocks.getAgentWorkbench.mockRejectedValueOnce({ code: 401, message: 'unauthorized' })
    const wrapper = mountView()
    await flushPromises()
    expect(routerMocks.push).toHaveBeenCalledWith(expect.objectContaining({
      path: '/h5/agent/login',
      query: { redirect: '/h5/agent/workbench' }
    }))
  })

  it('getAgentWorkbench 40301 异常时跳转登录', async () => {
    portalMocks.getAgentWorkbench.mockRejectedValueOnce({ code: 40301, message: 'token expired' })
    const wrapper = mountView()
    await flushPromises()
    expect(routerMocks.push).toHaveBeenCalledWith(expect.objectContaining({
      path: '/h5/agent/login'
    }))
  })

  it('getAgentWorkbench 其他异常 message.error', async () => {
    portalMocks.getAgentWorkbench.mockRejectedValueOnce(new Error('加载失败'))
    const wrapper = mountView()
    await flushPromises()
    expect(mocks.messageError).toHaveBeenCalledWith('加载失败')
  })

  it('formatDateTime 格式化时间', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.formatDateTime('2026-07-01T10:30:00')).toBe('2026-07-01 10:30')
    expect(vm.formatDateTime(undefined)).toBe('')
    expect(vm.formatDateTime('')).toBe('')
  })

  it('goMessages 跳转消息页', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.goMessages()
    expect(routerMocks.push).toHaveBeenCalledWith('/h5/agent/messages')
  })

  it('goTaskDetail 跳转任务详情', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.goTaskDetail(101)
    expect(routerMocks.push).toHaveBeenCalledWith('/agent/outsource?id=101')
  })

  it('goSubmitDeliverable 跳转交付物提交页', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.goSubmitDeliverable({ id: 201 } as any)
    expect(routerMocks.push).toHaveBeenCalledWith('/h5/agent/deliverable-submit?taskId=201')
  })

  it('getAgentUnreadCount 异常时默认 0（不抛错）', async () => {
    portalMocks.getAgentUnreadCount.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.unreadCount).toBe(0)
  })
})
