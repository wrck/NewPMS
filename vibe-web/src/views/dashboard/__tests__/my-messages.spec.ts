/**
 * dashboard/my-messages 视图单元测试（Task 2.1）
 *
 * 覆盖：
 *   - 渲染（标题、列表、过滤区）
 *   - onMounted 加载 getDashboard
 *   - filter.type / onlyUnread 过滤
 *   - markRead 标记单条已读
 *   - markAllRead 标记全部已读
 *   - unreadCount 计算
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import MyMessages from '../my-messages.vue'

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

/* ============ Mock @/api/report（getDashboard 返回 notices 字段） ============ */
const reportMocks = vi.hoisted(() => ({
  getDashboard: vi.fn()
}))

vi.mock('@/api/report', () => ({
  getDashboard: reportMocks.getDashboard
}))

function makeNotices() {
  return {
    notices: [
      { id: 1, title: '任务通知', type: 'TASK', createdAt: '2026-07-01 10:00:00', read: false },
      { id: 2, title: '审批通知', type: 'APPROVAL', createdAt: '2026-07-02 11:00:00', read: false },
      { id: 3, title: '系统通知', type: 'SYSTEM', createdAt: '2026-07-03 12:00:00', read: true }
    ]
  }
}

describe('dashboard my-messages view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    reportMocks.getDashboard.mockResolvedValue(makeNotices())
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(MyMessages, {})
  }

  it('渲染标题与列表', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('我的消息')
    expect(wrapper.text()).toContain('任务通知')
  })

  it('onMounted 调用 getDashboard', async () => {
    mountView()
    await flushPromises()
    expect(reportMocks.getDashboard).toHaveBeenCalledTimes(1)
  })

  it('渲染所有通知', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('任务通知')
    expect(wrapper.text()).toContain('审批通知')
    expect(wrapper.text()).toContain('系统通知')
  })

  it('unreadCount 计算正确', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.unreadCount).toBe(2)
  })

  it('filter.type 过滤通知类型', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.filtered.length).toBe(3)
    vm.filter.type = 'TASK'
    expect(vm.filtered.length).toBe(1)
    expect(vm.filtered[0].title).toBe('任务通知')
  })

  it('filter.onlyUnread 过滤未读', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.filter.onlyUnread = true
    expect(vm.filtered.length).toBe(2)
    expect(vm.filtered.every((n: any) => !n.read)).toBe(true)
  })

  it('markRead 标记单条已读', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const notice = vm.dataSource[0]
    expect(notice.read).toBe(false)
    vm.markRead(notice)
    expect(notice.read).toBe(true)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已标记为已读')
  })

  it('markAllRead 标记全部已读', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.unreadCount).toBe(2)
    vm.markAllRead()
    expect(vm.dataSource.every((n: any) => n.read)).toBe(true)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('全部已读')
  })

  it('getDashboard 异常时 dataSource 为空', async () => {
    reportMocks.getDashboard.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.dataSource).toEqual([])
  })

  it('typeLabel 映射包含常见类型', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.typeLabel.TASK).toBe('任务')
    expect(vm.typeLabel.APPROVAL).toBe('审批')
    expect(vm.typeLabel.SYSTEM).toBe('系统')
    expect(vm.typeLabel.RISK).toBe('风险')
  })
})
