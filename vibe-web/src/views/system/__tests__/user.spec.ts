/**
 * system/user 视图单元测试（Task 2.6）
 *
 * 覆盖：
 *   - 渲染（标题、表格、搜索）
 *   - onMounted 加载 pageUsers + listRoles
 *   - 搜索
 *   - openCreate / openEdit
 *   - handleSubmit 新建/编辑（含校验）
 *   - handleDelete / handleChangeStatus -> Modal.confirm
 *   - openAssignRoles / handleAssignRoles
 *   - openResetPwd / handleResetPwd（校验密码长度）
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import UserView from '../user.vue'

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
  pageUsers: vi.fn(),
  createUser: vi.fn(),
  updateUser: vi.fn(),
  deleteUser: vi.fn(),
  changeUserStatus: vi.fn(),
  resetUserPassword: vi.fn(),
  assignUserRoles: vi.fn(),
  listRoles: vi.fn()
}))

vi.mock('@/api/system', () => apiMocks)

describe('system user view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.pageUsers.mockResolvedValue({
      records: [
        {
          id: 1,
          userName: 'admin',
          realName: '管理员',
          email: 'admin@x.com',
          phone: '13800000000',
          orgName: '总部',
          roles: ['SUPER_ADMIN'],
          status: 1,
          lastLoginAt: '2026-07-01'
        }
      ],
      total: 1
    })
    apiMocks.createUser.mockResolvedValue(2)
    apiMocks.updateUser.mockResolvedValue(undefined)
    apiMocks.deleteUser.mockResolvedValue(undefined)
    apiMocks.changeUserStatus.mockResolvedValue(undefined)
    apiMocks.resetUserPassword.mockResolvedValue(undefined)
    apiMocks.assignUserRoles.mockResolvedValue(undefined)
    apiMocks.listRoles.mockResolvedValue([
      { id: 1, roleCode: 'SUPER_ADMIN', roleName: '超级管理员', status: 1 },
      { id: 2, roleCode: 'PM', roleName: '项目经理', status: 1 }
    ])
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(UserView, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('用户管理')
  })

  it('onMounted 调用 pageUsers 和 listRoles', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.pageUsers).toHaveBeenCalledTimes(1)
    expect(apiMocks.listRoles).toHaveBeenCalledTimes(1)
  })

  it('handleSearch 重置页码为 1', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.pagination.current = 5
    vm.query.userName = 'kw'
    await vm.handleSearch()
    await flushPromises()
    expect(vm.pagination.current).toBe(1)
  })

  it('openCreate 重置 formData 并打开弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(false)
    expect(vm.formData.userName).toBe('')
  })

  it('openEdit 加载行数据到 formData', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openEdit({ id: 5, userName: 'u1', realName: 'r1', email: 'e', phone: 'p', orgId: 1, status: 1, roles: ['PM'] })
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(true)
    expect(vm.formData.id).toBe(5)
    expect(vm.formData.userName).toBe('u1')
    expect(vm.formData.roleCodes).toEqual(['PM'])
  })

  it('handleSubmit 缺用户名/姓名时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.userName = ''
    vm.formData.realName = ''
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写用户名和姓名')
  })

  it('handleSubmit 新建缺密码时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.userName = 'u1'
    vm.formData.realName = 'r1'
    vm.formData.password = ''
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请设置初始密码')
  })

  it('handleSubmit 新建调用 createUser', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.userName = 'new'
    vm.formData.realName = '新用户'
    vm.formData.password = '123456'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createUser).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
  })

  it('handleSubmit 编辑调用 updateUser', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openEdit({ id: 7, userName: 'u', realName: 'r', email: '', phone: '', orgId: undefined, status: 1, roles: [] })
    vm.formData.realName = '新名'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.updateUser).toHaveBeenCalledWith(7, expect.objectContaining({ realName: '新名' }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('更新成功')
  })

  it('handleDelete onOk 调用 deleteUser', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 13, realName: 'X' })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteUser).toHaveBeenCalledWith(13)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('handleChangeStatus onOk 调用 changeUserStatus', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleChangeStatus({ id: 5, realName: 'X', status: 1 })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.changeUserStatus).toHaveBeenCalledWith(5, 0)
  })

  it('openAssignRoles 打开角色分配弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openAssignRoles({ id: 5, roles: ['PM'] })
    expect(vm.roleVisible).toBe(true)
    expect(vm.roleRow.id).toBe(5)
    expect(vm.selectedRoles).toEqual(['PM'])
  })

  it('handleAssignRoles 调用 assignUserRoles', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openAssignRoles({ id: 5, roles: [] })
    vm.selectedRoles = ['PM', 'ENGINEER']
    await vm.handleAssignRoles()
    await flushPromises()
    expect(apiMocks.assignUserRoles).toHaveBeenCalledWith(5, ['PM', 'ENGINEER'])
    expect(mocks.messageSuccess).toHaveBeenCalledWith('角色已分配')
  })

  it('openResetPwd 打开重置密码弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openResetPwd({ id: 5 })
    expect(vm.pwdVisible).toBe(true)
    expect(vm.newPassword).toBe('')
  })

  it('handleResetPwd 缺密码时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openResetPwd({ id: 5 })
    vm.newPassword = ''
    await vm.handleResetPwd()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请输入新密码')
  })

  it('handleResetPwd 密码少于 6 位时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openResetPwd({ id: 5 })
    vm.newPassword = '123'
    await vm.handleResetPwd()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('密码至少 6 位')
  })

  it('handleResetPwd 调用 resetUserPassword', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openResetPwd({ id: 7 })
    vm.newPassword = '123456'
    await vm.handleResetPwd()
    await flushPromises()
    expect(apiMocks.resetUserPassword).toHaveBeenCalledWith(7, '123456')
    expect(mocks.messageSuccess).toHaveBeenCalledWith('密码已重置')
  })

  it('pageUsers 异常时不抛错', async () => {
    apiMocks.pageUsers.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.dataSource).toEqual([])
  })
})
