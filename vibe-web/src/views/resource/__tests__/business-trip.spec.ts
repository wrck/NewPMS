/**
 * business-trip 视图单元测试（Task E2.1）
 *
 * 覆盖 CRUD + 审批：
 *   - 渲染（标题、表格、搜索表单、按钮）
 *   - onMounted 加载列表
 *   - 搜索 / 重置
 *   - 新增弹窗：openCreate / handleSubmit
 *   - 编辑：openEdit
 *   - 删除：handleDelete -> Modal.confirm
 *   - 审批：handleApprove
 *   - 日期校验
 *   - transportLabel 工具函数
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import BusinessTrip from '../business-trip.vue'

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
  pageBusinessTrips: vi.fn(),
  createBusinessTrip: vi.fn(),
  updateBusinessTrip: vi.fn(),
  deleteBusinessTrip: vi.fn(),
  approveBusinessTrip: vi.fn()
}))

vi.mock('@/api/resource', () => ({
  pageBusinessTrips: apiMocks.pageBusinessTrips,
  createBusinessTrip: apiMocks.createBusinessTrip,
  updateBusinessTrip: apiMocks.updateBusinessTrip,
  deleteBusinessTrip: apiMocks.deleteBusinessTrip,
  approveBusinessTrip: apiMocks.approveBusinessTrip
}))

describe('business-trip view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.pageBusinessTrips.mockResolvedValue({
      records: [
        {
          id: 1,
          engineerId: 10,
          engineerName: '张三',
          origin: '北京',
          destination: '上海',
          startDate: '2026-01-01',
          endDate: '2026-01-03',
          transportMode: 'TRAIN',
          accommodation: '酒店',
          estimatedCost: 1000,
          projectId: 5,
          projectName: '项目A',
          status: 'PENDING'
        }
      ],
      total: 1
    })
    apiMocks.createBusinessTrip.mockResolvedValue(2)
    apiMocks.updateBusinessTrip.mockResolvedValue(undefined)
    apiMocks.deleteBusinessTrip.mockResolvedValue(undefined)
    apiMocks.approveBusinessTrip.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(BusinessTrip, {})
  }

  it('渲染标题与表格容器', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.search-card').exists()).toBe(true)
    expect(wrapper.find('.table-card').exists()).toBe(true)
    expect(wrapper.text()).toContain('刷新')
    expect(wrapper.text()).toContain('申请差旅')
  })

  it('onMounted 调用 pageBusinessTrips 加载列表', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(apiMocks.pageBusinessTrips).toHaveBeenCalledTimes(1)
    const args = apiMocks.pageBusinessTrips.mock.calls[0][0]
    expect(args.page).toBe(1)
    expect(args.size).toBe(10)
  })

  it('加载后渲染表格数据行', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('张三')
    expect(wrapper.text()).toContain('上海')
    expect(wrapper.text()).toContain('北京')
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
    const last = apiMocks.pageBusinessTrips.mock.calls.at(-1)[0]
    expect(last.engineerId).toBe(9)
    expect(last.page).toBe(1)
  })

  it('handleReset 清空 query 并重新加载', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.query.engineerId = 9
    vm.query.status = 'PENDING'
    await vm.handleReset()
    await flushPromises()
    expect(vm.query.engineerId).toBeUndefined()
    expect(vm.query.status).toBeUndefined()
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
    expect(vm.formData.origin).toBe('')
    expect(vm.formData.transportMode).toBe('TRAIN')
  })

  it('openEdit 加载行数据到 formData', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const row = {
      id: 5,
      engineerId: 12,
      origin: '广州',
      destination: '深圳',
      startDate: '2026-02-01',
      endDate: '2026-02-03',
      transportMode: 'CAR',
      reason: '出差'
    }
    vm.openEdit(row)
    await nextTick()
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(true)
    expect(vm.formData.id).toBe(5)
    expect(vm.formData.engineerId).toBe(12)
    expect(vm.formData.origin).toBe('广州')
    expect(vm.formData.destination).toBe('深圳')
    expect(vm.formData.transportMode).toBe('CAR')
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

  it('handleSubmit 新增时调用 createBusinessTrip', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.engineerId = 9
    vm.formData.startDate = '2026-02-01'
    vm.formData.endDate = '2026-02-03'
    vm.formData.origin = '北京'
    vm.formData.destination = '上海'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createBusinessTrip).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('申请已提交')
    expect(vm.formVisible).toBe(false)
  })

  it('handleSubmit 编辑时调用 updateBusinessTrip', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openEdit({
      id: 7,
      engineerId: 12,
      origin: '旧',
      destination: '旧',
      startDate: '2026-02-01',
      endDate: '2026-02-03',
      transportMode: 'TRAIN'
    })
    vm.formData.destination = '新目的地'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.updateBusinessTrip).toHaveBeenCalledWith(7, expect.objectContaining({
      engineerId: 12,
      destination: '新目的地'
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

  it('handleDelete onOk 调用 deleteBusinessTrip', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 13, engineerName: 'X' })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteBusinessTrip).toHaveBeenCalledWith(13)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('handleApprove 触发 Modal.confirm', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const before = mocks.modalConfirmCallbacks.length
    vm.handleApprove({ id: 21 }, true)
    expect(mocks.modalConfirmCallbacks.length).toBeGreaterThan(before)
  })

  it('handleApprove onOk 调用 approveBusinessTrip', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleApprove({ id: 25 }, true)
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.approveBusinessTrip).toHaveBeenCalledWith(25, true)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('批准成功')
  })

  it('handleApprove 拒绝时调用 approveBusinessTrip(false)', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleApprove({ id: 26 }, false)
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.approveBusinessTrip).toHaveBeenCalledWith(26, false)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('拒绝成功')
  })

  it('transportLabel 返回交通方式中文', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.transportLabel('PLANE')).toBe('飞机')
    expect(vm.transportLabel('TRAIN')).toBe('火车')
    expect(vm.transportLabel('CAR')).toBe('汽车')
    expect(vm.transportLabel('OTHER')).toBe('其他')
    expect(vm.transportLabel(undefined)).toBe('—')
    expect(vm.transportLabel('UNKNOWN')).toBe('UNKNOWN')
  })

  it('loadData 异常时不抛错', async () => {
    apiMocks.pageBusinessTrips.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.table-card').exists()).toBe(true)
  })
})
