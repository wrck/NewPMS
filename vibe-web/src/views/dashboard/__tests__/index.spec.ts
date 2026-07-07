/**
 * dashboard/index 视图单元测试（Task 2.1）
 *
 * 覆盖：
 *   - 渲染（标题、刷新按钮、待办事项卡片）
 *   - 角色差异化展示（director/pm/engineer/agent 4 个 block 的 currentBlock 计算）
 *   - dashboard API 加载（getDashboard onMounted 调用、数据填充）
 *   - 通知加载与标记已读（pageMyNotices、handleMarkRead、handleMarkAllRead）
 *   - greeting 计算（按小时）
 *   - 异常兜底（API 失败时显示错误消息）
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Dashboard from '../index.vue'

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

/* ============ Mock @/api/report ============ */
const reportMocks = vi.hoisted(() => ({
  getDashboard: vi.fn()
}))

vi.mock('@/api/report', () => ({
  getDashboard: reportMocks.getDashboard,
  // 类型导出通过 vi.importActual 已处理，运行时只关心函数
}))

/* ============ Mock @/api/system（pageMyNotices / markNoticeRead / markAllNoticesRead） ============ */
const systemMocks = vi.hoisted(() => ({
  pageMyNotices: vi.fn(),
  markNoticeRead: vi.fn(),
  markAllNoticesRead: vi.fn()
}))

vi.mock('@/api/system', () => ({
  pageMyNotices: systemMocks.pageMyNotices,
  markNoticeRead: systemMocks.markNoticeRead,
  markAllNoticesRead: systemMocks.markAllNoticesRead
}))

/* ============ Mock @/stores/user ============ */
const userStoreMock = vi.hoisted(() => ({
  roles: ['PM'] as string[],
  realName: '张三',
  username: 'zhangsan'
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => userStoreMock
}))

/* ============ Mock vue-router ============ */
const routerPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush })
}))

function makeDashboardData(role: string) {
  return {
    role,
    realName: '张三',
    director: role === 'SUPER_ADMIN' || role === 'DIRECTOR' ? {
      stats: {
        activeProjectCount: 5,
        onlineDeviceCount: 10,
        activeEngineerCount: 8,
        activeAgentCount: 3
      },
      riskProjects: [{ projectId: 1, projectName: '风险项目A', status: 'EXECUTE', riskTypeName: '进度风险', description: '延期', progressPct: 50 }],
      pendingApprovalCount: 2,
      pendingChangeCount: 1,
      pendingWorkloadCount: 3,
      projectTrend: [],
      projectStatusDist: []
    } : undefined,
    pm: role === 'PM' ? {
      myProjectCount: 4,
      activeProjectCount: 2,
      pendingDispatchCount: 1,
      pendingReviewCount: 3,
      myProjects: [{ projectId: 10, projectName: 'PM项目', status: 'EXECUTE', pmName: '张三', progressPct: 70 }],
      pendingDispatchTasks: [{ taskId: 100, taskName: '待派单', status: 'PENDING', projectName: '项目X', plannedEnd: '2026-08-01' }],
      pendingReviewDeliverables: [{ deliverableId: 200, fileName: '交付物.zip', taskName: '任务', projectName: '项目Y', deadline: '2026-08-05' }]
    } : undefined,
    engineer: role === 'ENGINEER' ? {
      todayTaskCount: 3,
      pendingTaskCount: 2,
      overdueTaskCount: 1,
      monthWorkHours: 168,
      todayTasks: [{ taskId: 300, taskName: '今日任务', status: 'IN_PROGRESS', projectName: '项目E', plannedEnd: '2026-07-10' }],
      overdueTasks: [{ taskId: 301, taskName: '超期任务', status: 'IN_PROGRESS', projectName: '项目E', plannedEnd: '2026-06-01' }]
    } : undefined,
    agent: role === 'AGENT_ADMIN' || role === 'AGENT_ENGINEER' ? {
      totalCount: 6,
      pendingCount: 2,
      inProgressCount: 3,
      overdueCount: 1,
      pendingTasks: [{ outsourceTaskId: 400, taskName: '待接单', status: 'PENDING', projectName: '项目A', deadline: '2026-08-01' }],
      submittedTasks: [{ outsourceTaskId: 401, taskName: '待审核', status: 'SUBMITTED', agentCompanyName: '代理商A', deadline: '2026-08-05' }]
    } : undefined
  }
}

describe('dashboard index view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    userStoreMock.roles = ['PM']
    reportMocks.getDashboard.mockResolvedValue(makeDashboardData('PM'))
    systemMocks.pageMyNotices.mockResolvedValue({
      records: [
        { id: 1, title: '通知1', noticeType: 1, createTime: '2026-07-01 10:00:00', readStatus: 0 },
        { id: 2, title: '通知2', noticeType: 2, createTime: '2026-07-02 11:00:00', readStatus: 1 }
      ],
      total: 2
    })
    systemMocks.markNoticeRead.mockResolvedValue(undefined)
    systemMocks.markAllNoticesRead.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Dashboard, {})
  }

  it('渲染标题与刷新按钮', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('刷新')
    // greeting 应包含"好"
    expect(wrapper.text()).toMatch(/好/)
  })

  it('onMounted 调用 getDashboard 与 pageMyNotices', async () => {
    mountView()
    await flushPromises()
    expect(reportMocks.getDashboard).toHaveBeenCalledTimes(1)
    expect(systemMocks.pageMyNotices).toHaveBeenCalled()
  })

  it('PM 角色：渲染我负责的项目块', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('我负责的项目')
    expect(wrapper.text()).toContain('PM项目')
    expect(wrapper.text()).toContain('待派单任务')
  })

  it('ENGINEER 角色：渲染今日任务与超期任务块', async () => {
    userStoreMock.roles = ['ENGINEER']
    reportMocks.getDashboard.mockResolvedValue(makeDashboardData('ENGINEER'))
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('我的近期任务')
    expect(wrapper.text()).toContain('今日任务')
    expect(wrapper.text()).toContain('超期任务')
  })

  it('DIRECTOR 角色：渲染风险项目与待办审批', async () => {
    userStoreMock.roles = ['DIRECTOR']
    reportMocks.getDashboard.mockResolvedValue(makeDashboardData('DIRECTOR'))
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('风险项目')
    expect(wrapper.text()).toContain('待办审批')
    expect(wrapper.text()).toContain('风险项目A')
  })

  it('AGENT_ADMIN 角色：渲染待接单任务与待审核任务', async () => {
    userStoreMock.roles = ['AGENT_ADMIN']
    reportMocks.getDashboard.mockResolvedValue(makeDashboardData('AGENT_ADMIN'))
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('待接单任务')
    expect(wrapper.text()).toContain('待审核任务')
  })

  it('handleMarkRead 调用 markNoticeRead 并标记已读', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const notice = { id: 99, title: 'X', type: 'TASK', createdAt: '', read: false }
    await vm.handleMarkRead(notice)
    await flushPromises()
    expect(systemMocks.markNoticeRead).toHaveBeenCalledWith(99)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已标记为已读')
    expect(notice.read).toBe(true)
  })

  it('handleMarkRead 已读时不再调用 API', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.handleMarkRead({ id: 1, title: 'X', type: 'TASK', createdAt: '', read: true })
    expect(systemMocks.markNoticeRead).not.toHaveBeenCalled()
  })

  it('handleMarkAllRead 调用 markAllNoticesRead', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.handleMarkAllRead()
    await flushPromises()
    expect(systemMocks.markAllNoticesRead).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('全部已读')
  })

  it('getDashboard 异常时显示 error 消息', async () => {
    reportMocks.getDashboard.mockRejectedValueOnce(new Error('network error'))
    const wrapper = mountView()
    await flushPromises()
    expect(mocks.messageError).toHaveBeenCalled()
  })

  it('greeting 按小时返回不同文案', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const g = vm.greeting
    expect(['凌晨好', '上午好', '中午好', '下午好', '晚上好']).toContain(g)
  })

  it('currentBlock 计算正确（PM → pm）', async () => {
    userStoreMock.roles = ['PM']
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.currentBlock).toBe('pm')
  })

  it('currentBlock 计算正确（SUPER_ADMIN → director）', async () => {
    userStoreMock.roles = ['SUPER_ADMIN']
    reportMocks.getDashboard.mockResolvedValue(makeDashboardData('SUPER_ADMIN'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.currentBlock).toBe('director')
  })
})
