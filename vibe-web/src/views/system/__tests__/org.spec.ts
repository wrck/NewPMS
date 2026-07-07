/**
 * system/org 视图单元测试（Task 2.7）
 *
 * 覆盖：
 *   - 渲染（标题、表格、新增按钮）
 *   - onMounted 加载 listOrgTree
 *   - openCreate / openEdit（含 parentOptions 扁平化）
 *   - handleSubmit 校验（orgCode/orgName 必填）+ 新建/编辑
 *   - handleDelete -> Modal.confirm + deleteOrg
 *   - flattenOrg 工具方法
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import OrgView from '../org.vue'

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

/* ============ Mock @/api/system ============ */
const apiMocks = vi.hoisted(() => ({
  listOrgTree: vi.fn(),
  createOrg: vi.fn(),
  updateOrg: vi.fn(),
  deleteOrg: vi.fn()
}))

vi.mock('@/api/system', () => apiMocks)

describe('system org view', () => {
  const mockTree = [
    {
      id: 1,
      orgCode: 'TECH',
      orgName: '技术部',
      parentId: undefined,
      sort: 0,
      leaderId: 1,
      leaderName: '张三',
      phone: '13800000000',
      status: 1,
      children: [
        {
          id: 2,
          orgCode: 'TECH_FE',
          orgName: '前端组',
          parentId: 1,
          sort: 0,
          leaderId: 2,
          leaderName: '李四',
          phone: '13800000001',
          status: 1,
          children: []
        }
      ]
    }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.listOrgTree.mockResolvedValue(mockTree)
    apiMocks.createOrg.mockResolvedValue(3)
    apiMocks.updateOrg.mockResolvedValue(undefined)
    apiMocks.deleteOrg.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(OrgView, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('组织架构')
    expect(wrapper.text()).toContain('新增组织')
  })

  it('onMounted 调用 listOrgTree', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(apiMocks.listOrgTree).toHaveBeenCalledTimes(1)
    const vm = wrapper.vm as any
    expect(vm.treeData.length).toBe(1)
  })

  it('openCreate 不传 parent 重置 formData', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(false)
    expect(vm.formData.orgCode).toBe('')
    expect(vm.formData.orgName).toBe('')
    expect(vm.formData.status).toBe(1)
    expect(vm.parentOptions.length).toBeGreaterThan(0)
  })

  it('openCreate 传 parent 设置 parentId', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate({ id: 1, orgName: '技术部' })
    expect(vm.formData.parentId).toBe(1)
  })

  it('openEdit 加载记录到 formData', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openEdit({
      id: 2,
      orgCode: 'TECH_FE',
      orgName: '前端组',
      parentId: 1,
      sort: 0,
      leaderId: 2,
      leaderName: '李四',
      phone: '138',
      status: 1
    })
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(true)
    expect(vm.formData.id).toBe(2)
    expect(vm.formData.orgCode).toBe('TECH_FE')
  })

  it('handleSubmit 缺 orgCode 时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.orgName = '测试'
    vm.formData.orgCode = ''
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写组织编码和名称')
  })

  it('handleSubmit 缺 orgName 时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.orgCode = 'CODE'
    vm.formData.orgName = ''
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写组织编码和名称')
  })

  it('handleSubmit 新建调用 createOrg', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.orgCode = 'NEW'
    vm.formData.orgName = '新组织'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createOrg).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
    expect(vm.formVisible).toBe(false)
  })

  it('handleSubmit 编辑调用 updateOrg', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openEdit({
      id: 2,
      orgCode: 'TECH_FE',
      orgName: '前端组',
      parentId: 1,
      sort: 0,
      status: 1
    })
    vm.formData.orgName = '前端组2'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.updateOrg).toHaveBeenCalledWith(2, expect.objectContaining({ orgName: '前端组2' }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('更新成功')
  })

  it('handleDelete 触发 Modal.confirm onOk 调用 deleteOrg', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 2, orgName: '前端组' })
    expect(mocks.modalConfirmCallbacks.length).toBe(1)
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteOrg).toHaveBeenCalledWith(2)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('flattenOrg 递归扁平化树', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const flat = vm.flattenOrg(vm.treeData)
    expect(flat.length).toBe(2)
    expect(flat[0].value).toBe(1)
    expect(flat[0].label).toBe('技术部')
    expect(flat[1].label).toContain('前端组')
  })

  it('listOrgTree 异常时不抛错', async () => {
    apiMocks.listOrgTree.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.treeData).toEqual([])
    expect(vm.loading).toBe(false)
  })

  it('handleSubmit createOrg 异常时不重置 formVisible', async () => {
    apiMocks.createOrg.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.orgCode = 'NEW'
    vm.formData.orgName = '新组织'
    await vm.handleSubmit()
    await flushPromises()
    expect(vm.formVisible).toBe(true)
  })
})
