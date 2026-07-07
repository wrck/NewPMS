/**
 * dashboard/my-tasks 视图单元测试（Task 2.1）
 *
 * 覆盖：
 *   - 渲染（标题、表格容器）
 *   - onMounted 加载 getDashboard
 *   - PM 角色取 pm.pendingDispatchTasks
 *   - ENGINEER 角色合并 todayTasks + overdueTasks（去重）
 *   - AGENT 角色 合并 pendingTasks + submittedTasks
 *   - 总监/管理员 显示 EmptyState 提示
 *   - statusFilter 过滤
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import MyTasks from '../my-tasks.vue'

/* ============ Mock @/api/report ============ */
const reportMocks = vi.hoisted(() => ({
  getDashboard: vi.fn()
}))

vi.mock('@/api/report', () => ({
  getDashboard: reportMocks.getDashboard
}))

/* ============ Mock @/stores/user ============ */
const userStoreMock = vi.hoisted(() => ({
  roles: ['PM'] as string[]
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => userStoreMock
}))

function makeData(role: string) {
  return {
    role,
    realName: '张三',
    pm: role === 'PM' ? {
      pendingDispatchTasks: [
        { taskId: 1, taskName: 'PM任务1', status: 'PENDING', projectName: '项目A', plannedEnd: '2026-08-01' },
        { taskId: 2, taskName: 'PM任务2', status: 'ASSIGNED', projectName: '项目B', plannedEnd: '2026-08-02' }
      ]
    } : undefined,
    engineer: role === 'ENGINEER' ? {
      todayTasks: [
        { taskId: 10, taskName: '今日任务', status: 'IN_PROGRESS', projectName: '项目E', plannedEnd: '2026-07-10' }
      ],
      overdueTasks: [
        { taskId: 11, taskName: '超期任务', status: 'IN_PROGRESS', projectName: '项目E', plannedEnd: '2026-06-01' }
      ]
    } : undefined,
    agent: role === 'AGENT_ADMIN' ? {
      pendingTasks: [
        { outsourceTaskId: 20, taskName: '待接单', status: 'PENDING', projectName: '项目X', deadline: '2026-08-01' }
      ],
      submittedTasks: [
        { outsourceTaskId: 21, taskName: '待审核', status: 'SUBMITTED', projectName: '项目Y', deadline: '2026-08-05' }
      ]
    } : undefined
  }
}

describe('dashboard my-tasks view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    userStoreMock.roles = ['PM']
    reportMocks.getDashboard.mockResolvedValue(makeData('PM'))
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(MyTasks, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('我的任务')
  })

  it('onMounted 调用 getDashboard', async () => {
    mountView()
    await flushPromises()
    expect(reportMocks.getDashboard).toHaveBeenCalledTimes(1)
  })

  it('PM 角色取 pm.pendingDispatchTasks', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('PM任务1')
    expect(wrapper.text()).toContain('PM任务2')
  })

  it('ENGINEER 角色合并今日 + 超期任务', async () => {
    userStoreMock.roles = ['ENGINEER']
    reportMocks.getDashboard.mockResolvedValue(makeData('ENGINEER'))
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('今日任务')
    expect(wrapper.text()).toContain('超期任务')
  })

  it('AGENT_ADMIN 角色合并 pending + submitted', async () => {
    userStoreMock.roles = ['AGENT_ADMIN']
    reportMocks.getDashboard.mockResolvedValue(makeData('AGENT_ADMIN'))
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('待接单')
    expect(wrapper.text()).toContain('待审核')
  })

  it('DIRECTOR 角色显示 EmptyState 提示', async () => {
    userStoreMock.roles = ['DIRECTOR']
    reportMocks.getDashboard.mockResolvedValue({ role: 'DIRECTOR', realName: '总监' })
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('当前角色暂无个人任务')
  })

  it('statusFilter 过滤任务列表', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.filtered.length).toBe(2)
    // vm.statusFilter 是 ref 的解包值，直接赋值即可
    vm.statusFilter = 'PENDING'
    expect(vm.filtered.length).toBe(1)
    expect(vm.filtered[0].name).toBe('PM任务1')
  })

  it('getDashboard 异常时 dataSource 为空', async () => {
    reportMocks.getDashboard.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.dataSource).toEqual([])
  })

  it('isEmptyRoleView 对 SUPER_ADMIN 返回 true', async () => {
    userStoreMock.roles = ['SUPER_ADMIN']
    reportMocks.getDashboard.mockResolvedValue({ role: 'SUPER_ADMIN', realName: '管理员' })
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.isEmptyRoleView).toBe(true)
  })
})
