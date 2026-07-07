/**
 * h5/customer/projects 视图单元测试（Task 2.8）
 *
 * 覆盖：
 *   - 渲染（标题、项目卡片、待办入口）
 *   - onMounted 调用 getMyProjects + getUnreadMessageCount + getMyTodos
 *   - projects / unreadCount / todoCount 状态
 *   - formatStatus / formatPhase / getProgressColor 工具
 *   - 401 异常跳转登录
 *   - 其他异常 message.error
 *   - goProgress / goMessages / goTodos 路由跳转
 *   - 空项目时显示 a-empty
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ProjectsView from '../projects.vue'

/* ============ Mock vue-router ============ */
const routerMocks = vi.hoisted(() => ({
  push: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerMocks.push })
}))

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

/* ============ Mock @/api/customerPortal ============ */
const portalMocks = vi.hoisted(() => ({
  getMyProjects: vi.fn(),
  getMyTodos: vi.fn(),
  getUnreadMessageCount: vi.fn()
}))

vi.mock('@/api/customerPortal', () => portalMocks)

/* ============ Stub H5Layout ============ */
const stubs = {
  H5Layout: {
    template: '<div class="stub-h5-layout"><h1>{{ title }}</h1><slot /><slot name="right" /></div>',
    props: ['title']
  }
}

describe('h5 customer projects view', () => {
  const mockProjects = [
    {
      projectId: 1,
      projectCode: 'P001',
      projectName: '项目A',
      status: 'EXECUTE',
      projectType: '类型1',
      currentPhase: 'DELIVER',
      progressPct: 50,
      plannedEnd: '2026-08-01'
    },
    {
      projectId: 2,
      projectCode: 'P002',
      projectName: '项目B',
      status: 'CLOSE',
      projectType: '类型2',
      currentPhase: 'ACCEPT',
      progressPct: 100,
      plannedEnd: '2026-06-01'
    }
  ]

  const mockTodos = [
    { id: 1, type: 'APPROVAL', title: '待审批1' },
    { id: 2, type: 'TASK', title: '待办任务1' },
    { id: 3, type: 'TASK', title: '待办任务2' }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    portalMocks.getMyProjects.mockResolvedValue(mockProjects)
    portalMocks.getMyTodos.mockResolvedValue(mockTodos)
    portalMocks.getUnreadMessageCount.mockResolvedValue(7)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(ProjectsView, { global: { stubs } })
  }

  it('渲染标题与项目列表', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('我的项目')
    expect(wrapper.text()).toContain('项目A')
    expect(wrapper.text()).toContain('项目B')
  })

  it('onMounted 调用 getMyProjects + getUnreadMessageCount + getMyTodos', async () => {
    mountView()
    await flushPromises()
    expect(portalMocks.getMyProjects).toHaveBeenCalledTimes(1)
    expect(portalMocks.getUnreadMessageCount).toHaveBeenCalledTimes(1)
    expect(portalMocks.getMyTodos).toHaveBeenCalledTimes(1)
  })

  it('projects / unreadCount / todoCount 已加载', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.projects.length).toBe(2)
    expect(vm.unreadCount).toBe(7)
    expect(vm.todoCount).toBe(3)
  })

  it('待办入口在 todoCount > 0 时显示', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('3 项待办')
  })

  it('待办入口在 todoCount = 0 时不显示', async () => {
    portalMocks.getMyTodos.mockResolvedValueOnce([])
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).not.toContain('项待办')
  })

  it('formatStatus 映射 8 种状态', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.formatStatus('INIT')).toBe('初始化')
    expect(vm.formatStatus('PLAN')).toBe('规划中')
    expect(vm.formatStatus('EXECUTE')).toBe('实施中')
    expect(vm.formatStatus('ACCEPT')).toBe('验收中')
    expect(vm.formatStatus('CLOSE')).toBe('已关闭')
    expect(vm.formatStatus('ARCHIVED')).toBe('已归档')
    expect(vm.formatStatus('ON_HOLD')).toBe('已暂停')
    expect(vm.formatStatus('CANCELLED')).toBe('已取消')
  })

  it('formatStatus 未知状态返回原值', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.formatStatus('UNKNOWN')).toBe('UNKNOWN')
  })

  it('formatPhase 映射 6 种阶段', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.formatPhase('SURVEY')).toBe('调研')
    expect(vm.formatPhase('DESIGN')).toBe('设计')
    expect(vm.formatPhase('DELIVER')).toBe('交付')
    expect(vm.formatPhase('INSTALL')).toBe('安装')
    expect(vm.formatPhase('DEBUG')).toBe('调试')
    expect(vm.formatPhase('ACCEPT')).toBe('验收')
  })

  it('formatPhase undefined 或未知值', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.formatPhase(undefined)).toBe('-')
    expect(vm.formatPhase('')).toBe('-')
    expect(vm.formatPhase('UNKNOWN')).toBe('UNKNOWN')
  })

  it('getProgressColor 按百分比返回颜色', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.getProgressColor(100)).toBe('#52c41a')
    expect(vm.getProgressColor(80)).toBe('#52c41a')
    expect(vm.getProgressColor(50)).toBe('#1677ff')
    expect(vm.getProgressColor(40)).toBe('#1677ff')
    expect(vm.getProgressColor(30)).toBe('#faad14')
    expect(vm.getProgressColor(20)).toBe('#faad14')
    expect(vm.getProgressColor(10)).toBe('#ff4d4f')
    expect(vm.getProgressColor(0)).toBe('#ff4d4f')
  })

  it('getMyProjects 401 异常时跳转登录', async () => {
    portalMocks.getMyProjects.mockRejectedValueOnce({ code: 401, message: 'unauthorized' })
    const wrapper = mountView()
    await flushPromises()
    expect(routerMocks.push).toHaveBeenCalledWith('/h5/customer/login')
  })

  it('getMyProjects 40301 异常时跳转登录', async () => {
    portalMocks.getMyProjects.mockRejectedValueOnce({ code: 40301, message: 'token expired' })
    const wrapper = mountView()
    await flushPromises()
    expect(routerMocks.push).toHaveBeenCalledWith('/h5/customer/login')
  })

  it('getMyProjects 其他异常 message.error', async () => {
    portalMocks.getMyProjects.mockRejectedValueOnce(new Error('加载失败'))
    const wrapper = mountView()
    await flushPromises()
    expect(mocks.messageError).toHaveBeenCalledWith('加载失败')
  })

  it('getUnreadMessageCount 异常时默认 0', async () => {
    portalMocks.getUnreadMessageCount.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.unreadCount).toBe(0)
  })

  it('getMyTodos 异常时默认空数组', async () => {
    portalMocks.getMyTodos.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.todoCount).toBe(0)
  })

  it('goProgress 跳转项目进度详情', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.goProgress(1)
    expect(routerMocks.push).toHaveBeenCalledWith('/h5/customer/projects/1/progress')
  })

  it('goMessages 跳转消息页', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.goMessages()
    expect(routerMocks.push).toHaveBeenCalledWith('/h5/customer/messages')
  })

  it('goTodos 跳转待办页', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.goTodos()
    expect(routerMocks.push).toHaveBeenCalledWith('/h5/customer/todos')
  })

  it('项目列表为空时显示 a-empty', async () => {
    portalMocks.getMyProjects.mockResolvedValueOnce([])
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('暂无项目')
  })
})
