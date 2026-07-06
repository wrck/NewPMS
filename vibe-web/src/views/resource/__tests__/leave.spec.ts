/**
 * leave 视图单元测试（Task E2.1）
 *
 * 覆盖 CRUD + 审批 + 排期同步：
 *   - 渲染（标题、表格、搜索表单、按钮）
 *   - onMounted 加载列表
 *   - 搜索 / 重置
 *   - 新增弹窗：openCreate / handleSubmit
 *   - 编辑：openEdit
 *   - 删除：handleDelete -> Modal.confirm
 *   - 审批：handleApprove（含排期同步）
 *   - 日期校验
 *   - calcDays / leaveTypeLabel 工具函数
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import Leave from '../leave.vue'

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

/* ============ Mock @/api/resource ============ */
const apiMocks = vi.hoisted(() => ({
  pageEngineerLeaves: vi.fn(),
  createEngineerLeave: vi.fn(),
  updateEngineerLeave: vi.fn(),
  deleteEngineerLeave: vi.fn(),
  approveEngineerLeave: vi.fn(),
  createSchedule: vi.fn()
}))

vi.mock('@/api/resource', () => ({
  pageEngineerLeaves: apiMocks.pageEngineerLeaves,
  createEngineerLeave: apiMocks.createEngineerLeave,
  updateEngineerLeave: apiMocks.updateEngineerLeave,
  deleteEngineerLeave: apiMocks.deleteEngineerLeave,
  approveEngineerLeave: apiMocks.approveEngineerLeave,
  createSchedule: apiMocks.createSchedule
}))

describe('leave view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.pageEngineerLeaves.mockResolvedValue({
      records: [
        {
          id: 1,
          engineerId: 10,
          engineerName: '张三',
          startDate: '2026-01-01',
          endDate: '2026-01-03',
          leaveType: 'ANNUAL',
          reason: '年假',
          status: 'PENDING'
        }
      ],
      total: 1
    })
    apiMocks.createEngineerLeave.mockResolvedValue(2)
    apiMocks.updateEngineerLeave.mockResolvedValue(undefined)
    apiMocks.deleteEngineerLeave.mockResolvedValue(undefined)
    apiMocks.approveEngineerLeave.mockResolvedValue(undefined)
    apiMocks.createSchedule.mockResolvedValue(1)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Leave, {})
  }

  it('渲染标题与表格容器', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.search-card').exists()).toBe(true)
    expect(wrapper.find('.table-card').exists()).toBe(true)
    expect(wrapper.text()).toContain('刷新')
    expect(wrapper.text()).toContain('请假申请')
  })

  it('onMounted 调用 pageEngineerLeaves 加载列表', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(apiMocks.pageEngineerLeaves).toHaveBeenCalledTimes(1)
    const args = apiMocks.pageEngineerLeaves.mock.calls[0][0]
    expect(args.page).toBe(1)
    expect(args.size).toBe(10)
  })

  it('加载后渲染表格数据行', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('张三')
    expect(wrapper.text()).toContain('年假')
  })

  it('handleSearch 重置页码为 1 并重新加载', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.pagination.current = 5
    vm.query.engineerId = 9
    await vm.handleSearch()
    await flushPromises()
    expect(vm.pagination.current).toBe(1)
    const last = apiMocks.pageEngineerLeaves.mock.calls.at(-1)[0]
    expect(last.engineerId).toBe(9)
    expect(last.page).toBe(1)
  })

  it('handleReset 清空 query 并重新加载', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.query.engineerId = 9
    vm.query.status = 'PENDING'
    vm.query.leaveType = 'SICK'
    await vm.handleReset()
    await flushPromises()
    expect(vm.query.engineerId).toBeUndefined()
    expect(vm.query.status).toBeUndefined()
    expect(vm.query.leaveType).toBeUndefined()
  })

  it('openCreate 重置 formData 并打开弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    await nextTick()
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(false)
    expect(vm.formData.engineerId).toBe(0)
    expect(vm.formData.startDate).toBe('')
    expect(vm.formData.leaveType).toBe('ANNUAL')
  })

  it('openEdit 加载行数据到 formData', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const row = {
      id: 5,
      engineerId: 12,
      startDate: '2026-02-01',
      endDate: '2026-02-03',
      leaveType: 'SICK',
      reason: '病假'
    }
    vm.openEdit(row)
    await nextTick()
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(true)
    expect(vm.formData.id).toBe(5)
    expect(vm.formData.engineerId).toBe(12)
    expect(vm.formData.leaveType).toBe('SICK')
    expect(vm.formData.reason).toBe('病假')
  })

  it('handleSubmit 缺少工程师时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formData.engineerId = 0
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请选择工程师')
  })

  it('handleSubmit 缺少日期时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formData.engineerId = 1
    vm.formData.startDate = ''
    vm.formData.endDate = ''
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请选择起止日期')
  })

  it('handleSubmit 结束日期早于开始日期时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formData.engineerId = 1
    vm.formData.startDate = '2026-02-10'
    vm.formData.endDate = '2026-02-05'
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('结束日期不能早于开始日期')
  })

  it('handleSubmit 新增时调用 createEngineerLeave', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.engineerId = 9
    vm.formData.startDate = '2026-02-01'
    vm.formData.endDate = '2026-02-03'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createEngineerLeave).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('请假单已提交')
    expect(vm.formVisible).toBe(false)
  })

  it('handleSubmit 编辑时调用 updateEngineerLeave', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openEdit({
      id: 7,
      engineerId: 12,
      startDate: '2026-02-01',
      endDate: '2026-02-03',
      leaveType: 'ANNUAL',
      reason: '旧原因'
    })
    vm.formData.reason = '新原因'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.updateEngineerLeave).toHaveBeenCalledWith(7, expect.objectContaining({
      engineerId: 12,
      reason: '新原因'
    }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('更新成功')
  })

  it('handleDelete 触发 Modal.confirm', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const before = mocks.modalConfirmCallbacks.length
    vm.handleDelete({ id: 11, engineerName: 'X' })
    expect(mocks.modalConfirmCallbacks.length).toBeGreaterThan(before)
  })

  it('handleDelete onOk 调用 deleteEngineerLeave', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 13, engineerName: 'X' })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteEngineerLeave).toHaveBeenCalledWith(13)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('handleApprove 批准时同步排期并调用 approveEngineerLeave', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const row = {
      id: 21,
      engineerId: 8,
      startDate: '2026-02-01',
      endDate: '2026-02-03',
      leaveType: 'ANNUAL',
      reason: '请假'
    }
    vm.handleApprove(row, true)
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.approveEngineerLeave).toHaveBeenCalledWith(21, true)
    expect(apiMocks.createSchedule).toHaveBeenCalledWith(expect.objectContaining({
      engineerId: 8,
      type: 'LEAVE',
      startDate: '2026-02-01',
      endDate: '2026-02-03',
      status: 'CONFIRMED'
    }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('批准成功')
  })

  it('handleApprove 拒绝时不调用 createSchedule', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleApprove({ id: 22, engineerId: 8 }, false)
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.approveEngineerLeave).toHaveBeenCalledWith(22, false)
    expect(apiMocks.createSchedule).not.toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('拒绝成功')
  })

  it('handleApprove 排期同步失败时 warning 但审批仍成功', async () => {
    apiMocks.createSchedule.mockRejectedValueOnce(new Error('schedule failed'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleApprove({
      id: 23,
      engineerId: 8,
      startDate: '2026-02-01',
      endDate: '2026-02-03',
      leaveType: 'ANNUAL'
    }, true)
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('审批成功，但排期同步失败，请手动维护排期')
    expect(mocks.messageSuccess).toHaveBeenCalledWith('批准成功')
  })

  it('calcDays 计算天数差', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.calcDays('2026-01-01', '2026-01-01')).toBe(1)
    expect(vm.calcDays('2026-01-01', '2026-01-03')).toBe(3)
    expect(vm.calcDays('', '')).toBe(0)
    expect(vm.calcDays('2026-01-03', '2026-01-01')).toBe(0)
  })

  it('leaveTypeLabel 返回类型中文', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.leaveTypeLabel('ANNUAL')).toBe('年假')
    expect(vm.leaveTypeLabel('SICK')).toBe('病假')
    expect(vm.leaveTypeLabel('PERSONAL')).toBe('事假')
    expect(vm.leaveTypeLabel('OTHER')).toBe('其他')
    expect(vm.leaveTypeLabel(undefined)).toBe('—')
  })

  it('loadData 异常时不抛错', async () => {
    apiMocks.pageEngineerLeaves.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.table-card').exists()).toBe(true)
  })
})
