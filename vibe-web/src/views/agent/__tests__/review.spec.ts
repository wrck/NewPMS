/**
 * agent/review 视图单元测试（Task 2.2）
 *
 * 覆盖：
 *   - 渲染（标题、表格、搜索）
 *   - onMounted 加载 pageOutsourceTasks（默认 status=SUBMITTED）
 *   - 搜索
 *   - openReview 打开审核弹窗
 *   - handleReview 通过调用 confirmOutsourceTask
 *   - handleReview 退回调用 rejectOutsourceTask
 *   - openDeliverables 调用 listDeliverables
 *   - 退回缺原因时 warning
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Review from '../review.vue'

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
  confirmOutsourceTask: vi.fn(),
  rejectOutsourceTask: vi.fn(),
  listDeliverables: vi.fn()
}))

vi.mock('@/api/agent', () => apiMocks)

describe('agent review view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    apiMocks.pageOutsourceTasks.mockResolvedValue({
      records: [
        {
          id: 1,
          taskCode: 'OT-001',
          projectName: '项目A',
          taskName: '审核任务',
          agentCompanyName: '代理商A',
          agentEngineerName: '工程师',
          deadline: '2026-08-01',
          submitCount: 1,
          status: 'SUBMITTED'
        }
      ],
      total: 1
    })
    apiMocks.confirmOutsourceTask.mockResolvedValue(undefined)
    apiMocks.rejectOutsourceTask.mockResolvedValue(undefined)
    apiMocks.listDeliverables.mockResolvedValue([
      { id: 100, deliverableType: 'PHOTO', name: '现场照片.jpg', uploadedByName: '工程师', uploadedAt: '2026-07-01', remark: '', url: '/files/1' }
    ])
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Review, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('交付审核')
    expect(wrapper.text()).toContain('审核任务')
    expect(wrapper.text()).toContain('OT-001')
  })

  it('onMounted 调用 pageOutsourceTasks 默认 status=SUBMITTED', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.pageOutsourceTasks).toHaveBeenCalledTimes(1)
    const args = apiMocks.pageOutsourceTasks.mock.calls[0][0]
    expect(args.status).toBe('SUBMITTED')
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

  it('openReview 打开弹窗并设置 approved 标记', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openReview({ id: 1, taskName: 'X' }, true)
    expect(vm.reviewVisible).toBe(true)
    expect(vm.reviewForm.approved).toBe(true)
    vm.openReview({ id: 1, taskName: 'X' }, false)
    expect(vm.reviewForm.approved).toBe(false)
  })

  it('handleReview 通过调用 confirmOutsourceTask', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openReview({ id: 5, taskName: 'X' }, true)
    vm.reviewForm.remark = '通过'
    await vm.handleReview()
    await flushPromises()
    expect(apiMocks.confirmOutsourceTask).toHaveBeenCalledWith(5, '通过')
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已确认通过')
    expect(vm.reviewVisible).toBe(false)
  })

  it('handleReview 退回缺原因时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openReview({ id: 5, taskName: 'X' }, false)
    vm.reviewForm.remark = ''
    await vm.handleReview()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写退回原因')
    expect(apiMocks.rejectOutsourceTask).not.toHaveBeenCalled()
  })

  it('handleReview 退回调用 rejectOutsourceTask', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openReview({ id: 7, taskName: 'X' }, false)
    vm.reviewForm.remark = '不合格'
    await vm.handleReview()
    await flushPromises()
    expect(apiMocks.rejectOutsourceTask).toHaveBeenCalledWith(7, '不合格')
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已退回')
  })

  it('openDeliverables 调用 listDeliverables', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openDeliverables({ id: 9, taskName: 'X' })
    await flushPromises()
    expect(apiMocks.listDeliverables).toHaveBeenCalledWith(9)
    expect(vm.deliverableVisible).toBe(true)
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
