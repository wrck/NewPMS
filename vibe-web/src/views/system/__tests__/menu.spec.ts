/**
 * system/menu 视图单元测试（Task 2.7）
 *
 * 覆盖：
 *   - 渲染（标题、表格、按钮）
 *   - onMounted 加载 listMenuTree
 *   - openCreate / openEdit（openEdit 调用 getMenu）
 *   - handleSubmit 校验（menuName、MENU 需 path、BUTTON 需 perms）
 *   - handleDelete（有子菜单时 warning、Modal.confirm + deleteMenu）
 *   - handleReorder / findSiblingArray（仅同级排序）
 *   - 关联角色抽屉（openRoleDrawer 并行加载、handleSaveRoles）
 *   - menuTypeLabel 三种类型映射
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import MenuView from '../menu.vue'

/* ============ Mock ant-design-vue message + Modal ============ */
const mocks = vi.hoisted(() => ({
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
  messageWarning: vi.fn(),
  messageInfo: vi.fn(),
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
      info: mocks.messageInfo,
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
  listMenuTree: vi.fn(),
  getMenu: vi.fn(),
  createMenu: vi.fn(),
  updateMenu: vi.fn(),
  deleteMenu: vi.fn(),
  listRolesByMenu: vi.fn(),
  assignRolesToMenu: vi.fn(),
  listRoles: vi.fn()
}))

vi.mock('@/api/system', () => apiMocks)

describe('system menu view', () => {
  const mockTree = [
    {
      id: 1,
      parentId: 0,
      menuName: '系统管理',
      menuType: 'DIRECTORY',
      path: '/system',
      component: '',
      perms: '',
      icon: 'SettingOutlined',
      sortOrder: 0,
      visible: 1,
      children: [
        {
          id: 2,
          parentId: 1,
          menuName: '用户管理',
          menuType: 'MENU',
          path: '/system/user',
          component: 'system/user',
          perms: 'system:user:list',
          icon: '',
          sortOrder: 0,
          visible: 1,
          children: [
            {
              id: 4,
              parentId: 2,
              menuName: '新增用户',
              menuType: 'BUTTON',
              path: '',
              component: '',
              perms: 'system:user:add',
              icon: '',
              sortOrder: 0,
              visible: 1
            }
          ]
        },
        {
          id: 3,
          parentId: 1,
          menuName: '角色管理',
          menuType: 'MENU',
          path: '/system/role',
          component: 'system/role',
          perms: 'system:role:list',
          icon: '',
          sortOrder: 1,
          visible: 1,
          children: []
        }
      ]
    }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.listMenuTree.mockResolvedValue(mockTree)
    apiMocks.getMenu.mockResolvedValue(mockTree[0])
    apiMocks.createMenu.mockResolvedValue(10)
    apiMocks.updateMenu.mockResolvedValue(undefined)
    apiMocks.deleteMenu.mockResolvedValue(undefined)
    apiMocks.listRoles.mockResolvedValue([
      { id: 1, roleCode: 'SUPER_ADMIN', roleName: '超级管理员', status: 1 },
      { id: 2, roleCode: 'PM', roleName: '项目经理', status: 1 }
    ])
    apiMocks.listRolesByMenu.mockResolvedValue([{ id: 2 }])
    apiMocks.assignRolesToMenu.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(MenuView, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('菜单管理')
    expect(wrapper.text()).toContain('新增根菜单')
  })

  it('onMounted 调用 listMenuTree 并默认展开第一层', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(apiMocks.listMenuTree).toHaveBeenCalledTimes(1)
    const vm = wrapper.vm as any
    expect(vm.menuTree.length).toBe(1)
    expect(vm.expandedRowKeys).toEqual([1])
  })

  it('menuTypeLabel 包含三种类型', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.menuTypeLabel.DIRECTORY).toBe('目录')
    expect(vm.menuTypeLabel.MENU).toBe('菜单')
    expect(vm.menuTypeLabel.BUTTON).toBe('按钮')
  })

  it('openCreate 不传 parent 时默认 DIRECTORY', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(false)
    expect(vm.formData.menuType).toBe('DIRECTORY')
    expect(vm.formData.parentId).toBe(0)
  })

  it('openCreate 传 BUTTON 类型 parent 时 menuType 为 BUTTON', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate({ id: 3, menuType: 'BUTTON' })
    expect(vm.formData.menuType).toBe('BUTTON')
  })

  it('openCreate 传 MENU 类型 parent 时 menuType 为 MENU', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate({ id: 2, menuType: 'MENU' })
    expect(vm.formData.menuType).toBe('MENU')
  })

  it('openEdit 调用 getMenu 加载详情', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openEdit({ id: 1 })
    await flushPromises()
    expect(apiMocks.getMenu).toHaveBeenCalledWith(1)
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(true)
    expect(vm.formData.menuName).toBe('系统管理')
  })

  it('openEdit getMenu 异常时 message.error', async () => {
    apiMocks.getMenu.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openEdit({ id: 999 })
    await flushPromises()
    expect(mocks.messageError).toHaveBeenCalledWith('加载菜单详情失败')
    expect(vm.formVisible).toBe(false)
  })

  it('handleSubmit 缺 menuName 时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.menuName = ''
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写菜单名称')
  })

  it('handleSubmit MENU 类型缺 path 时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.menuName = '用户管理'
    vm.formData.menuType = 'MENU'
    vm.formData.path = ''
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('菜单类型需填写路由地址')
  })

  it('handleSubmit BUTTON 类型缺 perms 时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.menuName = '新增'
    vm.formData.menuType = 'BUTTON'
    vm.formData.perms = ''
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('按钮类型需填写权限标识')
  })

  it('handleSubmit 新建调用 createMenu', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.menuName = '测试菜单'
    vm.formData.menuType = 'MENU'
    vm.formData.path = '/test'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createMenu).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
    expect(vm.formVisible).toBe(false)
  })

  it('handleSubmit 编辑调用 updateMenu', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openEdit({ id: 1 })
    await flushPromises()
    vm.formData.menuName = '系统管理2'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.updateMenu).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('更新成功')
  })

  it('handleDelete 有子菜单时 warning 不删除', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 1, menuName: '系统管理', children: [{ id: 2 }] })
    expect(mocks.messageWarning).toHaveBeenCalledWith('存在子菜单，无法删除')
    expect(mocks.modalConfirmCallbacks.length).toBe(0)
  })

  it('handleDelete 无子菜单触发 Modal.confirm onOk 调用 deleteMenu', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 3, menuName: '新增用户', children: [] })
    expect(mocks.modalConfirmCallbacks.length).toBe(1)
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteMenu).toHaveBeenCalledWith(3)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('handleReorder 跨级排序时 message.info', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    // 拖动根节点到非同级节点
    await vm.handleReorder(1, 2)
    expect(mocks.messageInfo).toHaveBeenCalledWith('暂仅支持同级菜单内排序')
  })

  it('handleReorder 同级排序调用 updateMenu', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    // 用户管理(id=2) 拖到 新增用户(id=3) 之前，同属 id=1 子级
    await vm.handleReorder(2, 3)
    await flushPromises()
    expect(apiMocks.updateMenu).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('排序已更新')
  })

  it('openRoleDrawer 并行加载 listRoles 与 listRolesByMenu', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openRoleDrawer({ id: 2, menuName: '用户管理' })
    await flushPromises()
    expect(apiMocks.listRoles).toHaveBeenCalled()
    expect(apiMocks.listRolesByMenu).toHaveBeenCalledWith(2)
    expect(vm.roleDrawerVisible).toBe(true)
    expect(vm.allRoles.length).toBe(2)
    expect(vm.selectedRoleIds).toEqual([2])
  })

  it('handleSaveRoles 调用 assignRolesToMenu', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openRoleDrawer({ id: 2, menuName: '用户管理' })
    await flushPromises()
    vm.selectedRoleIds = [1, 2]
    await vm.handleSaveRoles()
    await flushPromises()
    expect(apiMocks.assignRolesToMenu).toHaveBeenCalledWith(2, [1, 2])
    expect(mocks.messageSuccess).toHaveBeenCalledWith('角色关联已更新')
    expect(vm.roleDrawerVisible).toBe(false)
  })

  it('listMenuTree 异常时不抛错', async () => {
    apiMocks.listMenuTree.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.menuTree).toEqual([])
    expect(vm.loading).toBe(false)
  })

  it('handleExpandAll / handleCollapseAll 操作 expandedRowKeys', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleExpandAll()
    expect(vm.expandedRowKeys).toContain(1)
    expect(vm.expandedRowKeys).toContain(2)
    vm.handleCollapseAll()
    expect(vm.expandedRowKeys).toEqual([])
  })
})
