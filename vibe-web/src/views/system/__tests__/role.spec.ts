/**
 * system/role 视图单元测试（Task 2.6）
 *
 * 覆盖：
 *   - 渲染（标题、表格、搜索）
 *   - onMounted 加载 listRoles
 *   - 搜索
 *   - openCreate / openEdit
 *   - handleSubmit 新建/编辑
 *   - handleDelete -> Modal.confirm + SUPER_ADMIN 不可删
 *   - openPermissions / handleAssignPermissions
 *   - dataScopeLabel / roleLabel 映射
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import RoleView from '../role.vue'

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
  listRoles: vi.fn(),
  createRole: vi.fn(),
  updateRole: vi.fn(),
  deleteRole: vi.fn(),
  getRolePermissions: vi.fn(),
  assignRolePermissions: vi.fn()
}))

vi.mock('@/api/system', () => apiMocks)

describe('system role view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.listRoles.mockResolvedValue([
      { id: 1, roleCode: 'SUPER_ADMIN', roleName: '超级管理员', description: 'd', dataScope: 'ALL', status: 1, userCount: 1, createdAt: '2026-07-01', permissions: ['*'] },
      { id: 2, roleCode: 'PM', roleName: '项目经理', description: 'd', dataScope: 'DEPT', status: 1, userCount: 5, createdAt: '2026-07-01', permissions: ['project:view'] }
    ])
    apiMocks.createRole.mockResolvedValue(3)
    apiMocks.updateRole.mockResolvedValue(undefined)
    apiMocks.deleteRole.mockResolvedValue(undefined)
    apiMocks.getRolePermissions.mockResolvedValue(['project:view', 'project:edit'])
    apiMocks.assignRolePermissions.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(RoleView, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('角色权限')
  })

  it('onMounted 调用 listRoles', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.listRoles).toHaveBeenCalledTimes(1)
  })

  it('handleSearch 调用 listRoles', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.query.roleName = 'PM'
    await vm.handleSearch()
    await flushPromises()
    expect(apiMocks.listRoles).toHaveBeenCalled()
  })

  it('openCreate 重置 formData 并打开弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(false)
    expect(vm.formData.roleName).toBe('')
    expect(vm.formData.dataScope).toBe('SELF')
  })

  it('openEdit 加载记录到 formData', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openEdit({ id: 5, roleCode: 'PM', roleName: '项目经理', description: 'd', dataScope: 'DEPT', status: 1, permissions: ['project:view'] })
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(true)
    expect(vm.formData.id).toBe(5)
    expect(vm.formData.roleCode).toBe('PM')
    expect(vm.formData.permissionCodes).toEqual(['project:view'])
  })

  it('handleSubmit 缺名称时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.roleName = ''
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写角色名称')
  })

  it('handleSubmit 新建调用 createRole', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.roleName = '新角色'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createRole).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
  })

  it('handleSubmit 编辑调用 updateRole', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openEdit({ id: 7, roleCode: 'X', roleName: 'old', description: '', dataScope: 'SELF', status: 1, permissions: [] })
    vm.formData.roleName = 'new'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.updateRole).toHaveBeenCalledWith(7, expect.objectContaining({ roleName: 'new' }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('更新成功')
  })

  it('handleDelete SUPER_ADMIN 时 warning 不删除', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 1, roleCode: 'SUPER_ADMIN', roleName: '超级管理员' })
    expect(mocks.messageWarning).toHaveBeenCalledWith('超级管理员角色不可删除')
    expect(mocks.modalConfirmCallbacks.length).toBe(0)
  })

  it('handleDelete 非 SUPER_ADMIN 触发 Modal.confirm onOk 调用 deleteRole', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 5, roleCode: 'PM', roleName: 'X' })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteRole).toHaveBeenCalledWith(5)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('openPermissions 调用 getRolePermissions', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openPermissions({ id: 2, roleName: 'X' })
    await flushPromises()
    expect(apiMocks.getRolePermissions).toHaveBeenCalledWith(2)
    expect(vm.permVisible).toBe(true)
    expect(vm.checkedKeys).toEqual(['project:view', 'project:edit'])
  })

  it('handleAssignPermissions 调用 assignRolePermissions', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.permRow = { id: 5 }
    vm.checkedKeys = ['project:view', 'project:edit']
    await vm.handleAssignPermissions()
    await flushPromises()
    expect(apiMocks.assignRolePermissions).toHaveBeenCalledWith(5, ['project:view', 'project:edit'])
    expect(mocks.messageSuccess).toHaveBeenCalledWith('权限已更新')
  })

  it('dataScopeLabel 包含 4 种数据范围', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(Object.keys(vm.dataScopeLabel).length).toBe(4)
    expect(vm.dataScopeLabel.ALL).toBe('全部数据')
    expect(vm.dataScopeLabel.SELF).toBe('仅本人')
  })

  it('listRoles 异常时不抛错', async () => {
    apiMocks.listRoles.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.dataSource).toEqual([])
  })
})
