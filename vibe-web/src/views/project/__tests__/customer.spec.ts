/**
 * customer 视图单元测试（Task E2.1）
 *
 * 覆盖 CRUD + 联系人子表 + 关联项目抽屉：
 *   - 渲染（标题、表格、搜索表单、按钮）
 *   - onMounted 加载列表
 *   - 搜索 / 重置
 *   - 新增弹窗：openCreate / handleSubmit
 *   - 编辑：openEdit（含 getCustomerDetail）
 *   - 删除：handleDelete -> Modal.confirm
 *   - 联系人子表：addContact / removeContact / syncPrimaryContact
 *   - 关联项目抽屉：openProjectDrawer / loadCustomerProjects
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import Customer from '../customer.vue'

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

/* ============ Mock @/api/project ============ */
const apiMocks = vi.hoisted(() => ({
  pageCustomers: vi.fn(),
  getCustomerDetail: vi.fn(),
  createCustomer: vi.fn(),
  updateCustomer: vi.fn(),
  deleteCustomer: vi.fn(),
  pageProjects: vi.fn()
}))

vi.mock('@/api/project', () => ({
  pageCustomers: apiMocks.pageCustomers,
  getCustomerDetail: apiMocks.getCustomerDetail,
  createCustomer: apiMocks.createCustomer,
  updateCustomer: apiMocks.updateCustomer,
  deleteCustomer: apiMocks.deleteCustomer,
  pageProjects: apiMocks.pageProjects
}))

describe('customer view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.pageCustomers.mockResolvedValue({
      records: [
        {
          id: 1,
          customerCode: 'CUST-001',
          customerName: 'XX科技',
          contactName: '李四',
          contactPhone: '13800000000',
          contactEmail: 'lisi@xx.com',
          region: '华北',
          industry: '金融',
          address: '北京市',
          createTime: '2026-01-01 10:00:00'
        }
      ],
      total: 1
    })
    apiMocks.getCustomerDetail.mockResolvedValue({
      id: 5,
      customerCode: 'CUST-005',
      customerName: '编辑客户',
      contactName: '王五',
      contactPhone: '13900000000',
      contactEmail: 'ww@xx.com',
      region: '华东',
      industry: '制造',
      address: '上海市',
      remark: '备注'
    })
    apiMocks.createCustomer.mockResolvedValue(2)
    apiMocks.updateCustomer.mockResolvedValue(undefined)
    apiMocks.deleteCustomer.mockResolvedValue(undefined)
    apiMocks.pageProjects.mockResolvedValue({
      records: [
        {
          id: 101,
          projectCode: 'P-001',
          projectName: '项目A',
          projectType: 'IMPLEMENT',
          productLine: '网络',
          status: 'EXECUTE',
          pmName: '经理A',
          progressPct: 60,
          plannedStart: '2026-01-01',
          plannedEnd: '2026-06-30'
        }
      ],
      total: 1
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Customer, {})
  }

  it('渲染标题与表格容器', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.search-card').exists()).toBe(true)
    expect(wrapper.find('.table-card').exists()).toBe(true)
    expect(wrapper.text()).toContain('刷新')
    expect(wrapper.text()).toContain('新增客户')
  })

  it('onMounted 调用 pageCustomers 加载列表', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(apiMocks.pageCustomers).toHaveBeenCalledTimes(1)
    const args = apiMocks.pageCustomers.mock.calls[0][0]
    expect(args.page).toBe(1)
    expect(args.size).toBe(10)
  })

  it('加载后渲染表格数据行', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('XX科技')
    expect(wrapper.text()).toContain('CUST-001')
  })

  it('handleSearch 重置页码为 1 并重新加载', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.pagination.current = 5
    vm.query.customerName = 'kw'
    await vm.handleSearch()
    await flushPromises()
    expect(vm.pagination.current).toBe(1)
    const last = apiMocks.pageCustomers.mock.calls.at(-1)[0]
    expect(last.customerName).toBe('kw')
    expect(last.page).toBe(1)
  })

  it('handleReset 清空 query 并重新加载', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.query.customerName = 'dirty'
    vm.query.customerCode = 'dirty'
    vm.query.region = 'dirty'
    vm.query.industry = 'dirty'
    await vm.handleReset()
    await flushPromises()
    expect(vm.query.customerName).toBe('')
    expect(vm.query.customerCode).toBe('')
    expect(vm.query.region).toBe('')
    expect(vm.query.industry).toBe('')
  })

  it('openCreate 重置 formData 并打开弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    await nextTick()
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(false)
    expect(vm.formData.customerCode).toBe('')
    expect(vm.formData.customerName).toBe('')
    expect(vm.contacts.length).toBe(1)
    expect(vm.contacts[0].isPrimary).toBe(true)
  })

  it('openEdit 调用 getCustomerDetail 并加载到 formData', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openEdit({ id: 5, customerName: 'XX' })
    await flushPromises()
    expect(apiMocks.getCustomerDetail).toHaveBeenCalledWith(5)
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(true)
    expect(vm.formData.id).toBe(5)
    expect(vm.formData.customerCode).toBe('CUST-005')
    expect(vm.formData.customerName).toBe('编辑客户')
    expect(vm.formData.contactName).toBe('王五')
  })

  it('openEdit getCustomerDetail 失败时回退到 row 数据', async () => {
    apiMocks.getCustomerDetail.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openEdit({
      id: 7,
      customerCode: 'CUST-007',
      customerName: '降级客户',
      contactName: '主联系人'
    })
    await flushPromises()
    expect(vm.formData.id).toBe(7)
    expect(vm.formData.customerName).toBe('降级客户')
    expect(vm.formData.contactName).toBe('主联系人')
  })

  it('addContact 增加非主联系人', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    const before = vm.contacts.length
    vm.addContact()
    expect(vm.contacts.length).toBe(before + 1)
    expect(vm.contacts.at(-1).isPrimary).toBe(false)
  })

  it('removeContact 不可删除主联系人', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    // 主联系人索引 0
    vm.removeContact(0)
    expect(mocks.messageWarning).toHaveBeenCalledWith('主联系人不可删除，请直接编辑')
    expect(vm.contacts.length).toBe(1)
  })

  it('removeContact 删除非主联系人', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.addContact()
    expect(vm.contacts.length).toBe(2)
    vm.removeContact(1)
    expect(vm.contacts.length).toBe(1)
  })

  it('syncPrimaryContact 同步主联系人到 formData', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.contacts[0].name = '主'
    vm.contacts[0].phone = '138'
    vm.contacts[0].email = 'main@xx.com'
    vm.syncPrimaryContact()
    expect(vm.formData.contactName).toBe('主')
    expect(vm.formData.contactPhone).toBe('138')
    expect(vm.formData.contactEmail).toBe('main@xx.com')
  })

  it('handleSubmit 缺少编码与名称时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formData.customerCode = ' '
    vm.formData.customerName = ''
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写客户编码和名称')
  })

  it('handleSubmit 新增时调用 createCustomer', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.customerCode = 'CUST-NEW'
    vm.formData.customerName = '新客户'
    vm.contacts[0].name = '主联系人'
    vm.contacts[0].phone = '13900000000'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createCustomer).toHaveBeenCalled()
    // 主联系人应同步到 formData
    const callArg = apiMocks.createCustomer.mock.calls[0][0]
    expect(callArg.contactName).toBe('主联系人')
    expect(callArg.contactPhone).toBe('13900000000')
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
    expect(vm.formVisible).toBe(false)
  })

  it('handleSubmit 编辑时调用 updateCustomer', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openEdit({ id: 5 })
    await flushPromises()
    vm.formData.customerName = '更新名'
    vm.formData.contactName = '王五'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.updateCustomer).toHaveBeenCalledWith(5, expect.objectContaining({
      customerName: '更新名'
    }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('更新成功')
  })

  it('handleDelete 触发 Modal.confirm', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const before = mocks.modalConfirmCallbacks.length
    vm.handleDelete({ id: 11, customerName: 'X' })
    expect(mocks.modalConfirmCallbacks.length).toBeGreaterThan(before)
  })

  it('handleDelete onOk 调用 deleteCustomer', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 13, customerName: 'X' })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteCustomer).toHaveBeenCalledWith(13)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('openProjectDrawer 调用 loadCustomerProjects 并打开抽屉', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openProjectDrawer({ id: 9, customerName: '客户X' })
    await flushPromises()
    expect(apiMocks.pageProjects).toHaveBeenCalled()
    const args = apiMocks.pageProjects.mock.calls[0][0]
    expect(args.customerId).toBe(9)
    expect(vm.projectDrawerVisible).toBe(true)
    expect(vm.projectList.length).toBeGreaterThan(0)
  })

  it('loadCustomerProjects 异常时不抛错且清空列表', async () => {
    apiMocks.pageProjects.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await expect(vm.loadCustomerProjects(99)).resolves.not.toThrow()
    await flushPromises()
    expect(vm.projectList).toEqual([])
    expect(vm.projectPagination.total).toBe(0)
  })

  it('handleProjectTableChange 更新分页并重新加载', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openProjectDrawer({ id: 9, customerName: '客户X' })
    await flushPromises()
    const before = apiMocks.pageProjects.mock.calls.length
    vm.handleProjectTableChange({ current: 2, pageSize: 20 })
    await flushPromises()
    expect(vm.projectPagination.current).toBe(2)
    expect(vm.projectPagination.pageSize).toBe(20)
    expect(apiMocks.pageProjects.mock.calls.length).toBeGreaterThan(before)
  })

  it('formatPlannedRange 格式化项目周期', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.formatPlannedRange({ plannedStart: '2026-01-01', plannedEnd: '2026-06-30' }))
      .toBe('2026-01-01 ~ 2026-06-30')
    expect(vm.formatPlannedRange({})).toBe('— ~ —')
  })

  it('loadData 异常时不抛错', async () => {
    apiMocks.pageCustomers.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.table-card').exists()).toBe(true)
  })
})
