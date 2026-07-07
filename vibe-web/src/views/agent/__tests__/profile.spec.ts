/**
 * agent/profile 视图单元测试（Task 2.2）
 *
 * 覆盖：
 *   - 渲染（标题、表格、搜索表单）
 *   - onMounted 加载 pageAgentCompanies
 *   - 搜索 / 重置
 *   - 新增公司：openCreate / handleSubmit
 *   - 编辑公司：openEdit
 *   - 删除公司：handleDelete -> Modal.confirm
 *   - 变更合作状态：handleChangeStatus
 *   - 工程师抽屉：openDrawer 调用 listAgentEngineers + listAgentScores
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import Profile from '../profile.vue'

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

/* ============ Mock @/api/agent ============ */
const apiMocks = vi.hoisted(() => ({
  pageAgentCompanies: vi.fn(),
  createAgentCompany: vi.fn(),
  updateAgentCompany: vi.fn(),
  deleteAgentCompany: vi.fn(),
  changeAgentCompanyStatus: vi.fn(),
  listAgentEngineers: vi.fn(),
  createAgentEngineer: vi.fn(),
  updateAgentEngineer: vi.fn(),
  deleteAgentEngineer: vi.fn(),
  changeAgentEngineerStatus: vi.fn(),
  listAgentScores: vi.fn()
}))

vi.mock('@/api/agent', () => apiMocks)

describe('agent profile view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.pageAgentCompanies.mockResolvedValue({
      records: [
        {
          id: 1,
          companyCode: 'AC-001',
          companyName: '北京代理商',
          contactName: '李四',
          contactPhone: '13800000000',
          serviceRegions: ['华北'],
          productLines: ['网络'],
          status: 'ACTIVE',
          projectCount: 3,
          overallScore: 4.5,
          createTime: '2026-01-01'
        }
      ],
      total: 1
    })
    apiMocks.createAgentCompany.mockResolvedValue(2)
    apiMocks.updateAgentCompany.mockResolvedValue(undefined)
    apiMocks.deleteAgentCompany.mockResolvedValue(undefined)
    apiMocks.changeAgentCompanyStatus.mockResolvedValue(undefined)
    apiMocks.listAgentEngineers.mockResolvedValue([
      { id: 10, name: '工程师A', phone: '13900000000', status: 'ACTIVE', skills: [], taskCount: 5, qualityScore: 4.6 }
    ])
    apiMocks.listAgentScores.mockResolvedValue([])
    apiMocks.createAgentEngineer.mockResolvedValue(10)
    apiMocks.updateAgentEngineer.mockResolvedValue(undefined)
    apiMocks.deleteAgentEngineer.mockResolvedValue(undefined)
    apiMocks.changeAgentEngineerStatus.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Profile, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('北京代理商')
    expect(wrapper.text()).toContain('AC-001')
  })

  it('onMounted 调用 pageAgentCompanies', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.pageAgentCompanies).toHaveBeenCalledTimes(1)
  })

  it('handleSearch 重置页码为 1', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.pagination.current = 5
    vm.query.companyName = 'kw'
    await vm.handleSearch()
    await flushPromises()
    expect(vm.pagination.current).toBe(1)
  })

  it('openCreate 重置 formData 并打开弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    await nextTick()
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(false)
    expect(vm.formData.companyName).toBe('')
  })

  it('openEdit 加载行数据到 formData', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openEdit({ id: 5, companyCode: 'AC-002', companyName: '上海代理商', status: 'SUSPENDED', serviceRegions: ['华东'], productLines: [] })
    await nextTick()
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(true)
    expect(vm.formData.id).toBe(5)
    expect(vm.formData.companyName).toBe('上海代理商')
  })

  it('handleSubmit 缺名称/编码时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formData.companyName = ''
    vm.formData.companyCode = ''
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写公司名称和编码')
  })

  it('handleSubmit 新增时调用 createAgentCompany', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.companyCode = 'AC-NEW'
    vm.formData.companyName = '新代理商'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createAgentCompany).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
  })

  it('handleSubmit 编辑时调用 updateAgentCompany', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openEdit({ id: 7, companyCode: 'AC-7', companyName: '旧名', status: 'ACTIVE' })
    vm.formData.companyName = '新名'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.updateAgentCompany).toHaveBeenCalledWith(7, expect.objectContaining({ companyName: '新名' }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('更新成功')
  })

  it('handleDelete 触发 Modal.confirm', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const before = mocks.modalConfirmCallbacks.length
    vm.handleDelete({ id: 11, companyName: 'X' })
    expect(mocks.modalConfirmCallbacks.length).toBeGreaterThan(before)
  })

  it('handleDelete onOk 调用 deleteAgentCompany', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 13, companyName: 'X' })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteAgentCompany).toHaveBeenCalledWith(13)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('handleChangeStatus onOk 调用 changeAgentCompanyStatus', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleChangeStatus({ id: 1, companyName: 'X' }, 'SUSPENDED')
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.changeAgentCompanyStatus).toHaveBeenCalledWith(1, 'SUSPENDED')
    expect(mocks.messageSuccess).toHaveBeenCalledWith('状态已变更')
  })

  it('openDrawer 调用 listAgentEngineers + listAgentScores', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openDrawer({ id: 1, companyName: 'X' })
    await flushPromises()
    expect(apiMocks.listAgentEngineers).toHaveBeenCalledWith(1)
    expect(apiMocks.listAgentScores).toHaveBeenCalledWith(1)
    expect(vm.drawerVisible).toBe(true)
  })

  it('addRegion 添加区域到 serviceRegions', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.regionInput = '华南'
    vm.addRegion()
    expect(vm.formData.serviceRegions).toContain('华南')
  })

  it('pageAgentCompanies 异常时不抛错', async () => {
    apiMocks.pageAgentCompanies.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    // 异常被组件 catch，页面正常渲染（不抛错）
    expect(wrapper.find('.table-card').exists()).toBe(true)
  })
})
