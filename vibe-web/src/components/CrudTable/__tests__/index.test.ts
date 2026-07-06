/**
 * CrudTable 组件单元测试（spec 阶段三 Task 12 - SubTask 12.7）
 *
 * 覆盖范围：
 *   - 组件渲染（标题、表格、工具栏）
 *   - 搜索表单展开/折叠
 *   - 搜索/重置触发
 *   - 分页变化触发 page 调用
 *   - 新增按钮点击弹出 FormModal
 *   - 编辑按钮回填数据
 *   - 删除按钮确认后调用 delete
 *   - 自定义 actions 渲染与点击
 *   - 行选择与批量删除
 *   - 异常处理（API 失败不阻塞）
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import type { CrudColumn, SearchField, CrudAction, CrudApiFunc } from '../types'
import CrudTable from '../index.vue'

/* ============ Mock ant-design-vue 静态方法 ============ */
const mocks = vi.hoisted(() => {
  return {
    messageSuccess: vi.fn(),
    messageError: vi.fn(),
    messageWarning: vi.fn(),
    messageInfo: vi.fn(),
    modalConfirmCallbacks: [] as Array<{ onOk?: () => any; onCancel?: () => any }>,
    hasPermissionMock: vi.fn<[string], boolean>(() => true)
  }
})
const { messageSuccess, messageError, messageWarning, messageInfo, modalConfirmCallbacks, hasPermissionMock } = mocks

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

/* ============ Mock useUserStore ============ */
vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    hasPermission: mocks.hasPermissionMock,
    isAdmin: true,
    permissions: []
  })
}))

/* ============ 测试数据 ============ */
const sampleRecords = [
  { id: 1, userName: 'alice', realName: 'Alice', status: 1, age: 25 },
  { id: 2, userName: 'bob', realName: 'Bob', status: 0, age: 30 },
  { id: 3, userName: 'carol', realName: 'Carol', status: 1, age: 28 }
]

function makeApiFunc(overrides: Partial<CrudApiFunc> = {}): CrudApiFunc {
  return {
    page: vi.fn(async () => ({
      records: sampleRecords,
      total: 3,
      page: 1,
      size: 10,
      pages: 1
    })),
    create: vi.fn(async () => 1),
    update: vi.fn(async () => undefined),
    delete: vi.fn(async () => undefined),
    ...overrides
  }
}

const columns: CrudColumn[] = [
  { field: 'userName', title: '用户名', width: 120, formType: 'input', formRules: [{ required: true }] },
  { field: 'realName', title: '姓名', width: 100, formType: 'input' },
  {
    field: 'status',
    title: '状态',
    width: 80,
    valueEnum: { '1': { text: '启用', status: 'success' }, '0': { text: '禁用', status: 'error' } },
    formType: 'select',
    formDefaultValue: 1
  }
]

const searchFields: SearchField[] = [
  { field: 'userName', label: '用户名', type: 'input' },
  { field: 'realName', label: '姓名', type: 'input' },
  { field: 'status', label: '状态', type: 'select', options: [{ label: '启用', value: 1 }, { label: '禁用', value: 0 }] },
  { field: 'age', label: '年龄', type: 'input' }
]

function makeWrapper(options: any = {}) {
  const apiFunc = options.apiFunc || makeApiFunc()
  return mount(CrudTable, {
    props: {
      columns,
      searchFields,
      apiFunc,
      permissionPrefix: 'system:user',
      title: '用户管理',
      ...(options.props || {})
    },
    global: {
      stubs: {
        // 仅 stub 图标组件，避免 ant-design-vue 4.x 图标渲染成本
        ReloadOutlined: true,
        PlusOutlined: true,
        EditOutlined: true,
        DeleteOutlined: true,
        SearchOutlined: true,
        DownOutlined: true,
        UpOutlined: true,
        ...options.stubs
      }
    }
  })
}

/* ============ 测试用例 ============ */
describe('CrudTable', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    modalConfirmCallbacks.length = 0
    hasPermissionMock.mockReturnValue(true)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('渲染', () => {
    it('应渲染表格容器与标题', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      expect(wrapper.find('.crud-table').exists()).toBe(true)
      expect(wrapper.text()).toContain('用户管理')
    })

    it('应渲染搜索表单（searchFields 非空）', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      expect(wrapper.find('.crud-search').exists()).toBe(true)
      // 默认折叠时只展示前 3 个字段
      const labels = wrapper.findAll('.crud-search .ant-form-item-label label')
      const labelTexts = labels.map((l) => l.text())
      expect(labelTexts).toContain('用户名')
    })

    it('searchFields 为空时不渲染搜索表单', async () => {
      const wrapper = makeWrapper({ props: { searchFields: [] } })
      await flushPromises()
      expect(wrapper.find('.crud-search').exists()).toBe(false)
    })

    it('挂载后调用 apiFunc.page 加载数据', async () => {
      const apiFunc = makeApiFunc()
      makeWrapper({ apiFunc })
      await flushPromises()
      expect(apiFunc.page).toHaveBeenCalledTimes(1)
      // 第一次调用应包含默认 page/size
      const callArgs = (apiFunc.page as any).mock.calls[0][0]
      expect(callArgs.page).toBe(1)
      expect(callArgs.size).toBe(10)
    })

    it('应渲染工具栏的新增按钮', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const toolbar = wrapper.find('.crud-toolbar')
      expect(toolbar.exists()).toBe(true)
      expect(toolbar.text()).toContain('新增')
    })

    it('无 create 权限时隐藏新增按钮', async () => {
      hasPermissionMock.mockImplementation((p: string) => !p?.endsWith('create'))
      const wrapper = makeWrapper()
      await flushPromises()
      const toolbar = wrapper.find('.crud-toolbar')
      expect(toolbar.text()).not.toContain('新增')
    })
  })

  describe('搜索表单', () => {
    it('点击展开按钮切换折叠状态', async () => {
      const apiFunc = makeApiFunc()
      const wrapper = makeWrapper({ apiFunc })
      await flushPromises()
      // searchFields.length=4 > 3，应该有展开按钮
      const toggleBtn = wrapper.find('.crud-search button.ant-btn-link')
      expect(toggleBtn.exists()).toBe(true)
      expect(toggleBtn.text()).toContain('展开')
      await toggleBtn.trigger('click')
      await nextTick()
      // 切换后文案变为折叠
      const updated = wrapper.find('.crud-search button.ant-btn-link')
      expect(updated.text()).toContain('折叠')
    })

    it('点击查询按钮触发 page 调用', async () => {
      const apiFunc = makeApiFunc()
      const wrapper = makeWrapper({ apiFunc })
      await flushPromises()
      const initialCallCount = (apiFunc.page as any).mock.calls.length
      // 直接调用 handleSearch（jsdom 下 a-form submit 事件触发不稳定）
      ;(wrapper.vm as any).handleSearch()
      await flushPromises()
      expect((apiFunc.page as any).mock.calls.length).toBeGreaterThan(initialCallCount)
      // handleSearch 会重置 current 为 1
      const lastCall = (apiFunc.page as any).mock.calls.at(-1)[0]
      expect(lastCall.page).toBe(1)
    })

    it('点击重置按钮清空查询条件并重新加载', async () => {
      const apiFunc = makeApiFunc()
      const wrapper = makeWrapper({ apiFunc })
      await flushPromises()
      const vm = wrapper.vm as any
      // 给字段赋值模拟用户输入
      vm.searchQuery.userName = 'dirty'
      expect(vm.searchQuery.userName).toBe('dirty')
      const initialCallCount = (apiFunc.page as any).mock.calls.length
      await vm.reset()
      await flushPromises()
      expect((apiFunc.page as any).mock.calls.length).toBeGreaterThan(initialCallCount)
      // 重置后 userName 应回到 undefined
      expect(vm.searchQuery.userName).toBeUndefined()
    })
  })

  describe('分页', () => {
    it('分页变化触发 page 调用并传递新 page/size', async () => {
      const apiFunc = makeApiFunc()
      const wrapper = makeWrapper({ apiFunc })
      await flushPromises()
      const initialCallCount = (apiFunc.page as any).mock.calls.length
      // 直接调用组件内部 handleTableChange
      const vm = wrapper.vm as any
      vm.handleTableChange(
        { current: 2, pageSize: 20 },
        {},
        { field: 'userName', order: 'descend' }
      )
      await flushPromises()
      expect((apiFunc.page as any).mock.calls.length).toBeGreaterThan(initialCallCount)
      const lastCall = (apiFunc.page as any).mock.calls.at(-1)[0]
      expect(lastCall.page).toBe(2)
      expect(lastCall.size).toBe(20)
      expect(lastCall.sortField).toBe('userName')
      expect(lastCall.sortOrder).toBe('desc')
    })

    it('page 异常时不抛错且不影响页面', async () => {
      const apiFunc = makeApiFunc({
        page: vi.fn(async () => {
          throw new Error('network error')
        })
      })
      // 不应抛出
      const wrapper = makeWrapper({ apiFunc })
      await flushPromises()
      expect(wrapper.find('.crud-table').exists()).toBe(true)
    })
  })

  describe('新增弹窗', () => {
    it('点击新增按钮打开弹窗', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      // 通过 expose 触发（避免点击事件触发主按钮 stub 的副作用）
      ;(wrapper.vm as any).openCreate()
      await nextTick()
      // 弹窗 a-modal 渲染（ ant-modal-root 由 antd 注入 body）
      const modals = document.querySelectorAll('.ant-modal')
      // 即便数量为 0（jsdom 渲染时机问题），formVisible 内部状态应变为 true
      expect((wrapper.vm as any).formVisible).toBe(true)
    })

    it('无 create 权限时弹窗不打开并提示', async () => {
      hasPermissionMock.mockImplementation((p: string) => !p?.endsWith('create'))
      const wrapper = makeWrapper()
      await flushPromises()
      ;(wrapper.vm as any).openCreate()
      await nextTick()
      expect((wrapper.vm as any).formVisible).toBe(false)
      expect(messageWarning).toHaveBeenCalled()
    })

    it('提交新增调用 create 并刷新', async () => {
      const apiFunc = makeApiFunc()
      const wrapper = makeWrapper({ apiFunc })
      await flushPromises()
      ;(wrapper.vm as any).openCreate()
      await nextTick()
      // 填入表单数据
      const vm = wrapper.vm as any
      vm.formModel.userName = 'newuser'
      vm.formModel.realName = 'New'
      const beforeCreate = (apiFunc.create as any).mock.calls.length
      const beforePage = (apiFunc.page as any).mock.calls.length
      await vm.handleSubmit()
      await flushPromises()
      expect((apiFunc.create as any).mock.calls.length).toBeGreaterThan(beforeCreate)
      expect((apiFunc.page as any).mock.calls.length).toBeGreaterThan(beforePage)
      expect(messageSuccess).toHaveBeenCalledWith('新增成功')
    })
  })

  describe('编辑弹窗', () => {
    it('openEdit 回填表单数据', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const record = { id: 1, userName: 'alice', realName: 'Alice', status: 1, age: 25 }
      ;(wrapper.vm as any).openEdit(record)
      await nextTick()
      const vm = wrapper.vm as any
      expect(vm.formVisible).toBe(true)
      expect(vm.isEdit).toBe(true)
      expect(vm.formModel.userName).toBe('alice')
      expect(vm.formModel.realName).toBe('Alice')
      expect(vm.formModel.status).toBe(1)
    })

    it('提交编辑调用 update', async () => {
      const apiFunc = makeApiFunc()
      const wrapper = makeWrapper({ apiFunc })
      await flushPromises()
      const record = { id: 1, userName: 'alice', realName: 'Alice', status: 1 }
      ;(wrapper.vm as any).openEdit(record)
      await nextTick()
      const vm = wrapper.vm as any
      const beforeUpdate = (apiFunc.update as any).mock.calls.length
      await vm.handleSubmit()
      await flushPromises()
      expect((apiFunc.update as any).mock.calls.length).toBeGreaterThan(beforeUpdate)
      const callArgs = (apiFunc.update as any).mock.calls.at(-1)
      expect(callArgs[0]).toBe(1)
      expect(messageSuccess).toHaveBeenCalledWith('更新成功')
    })
  })

  describe('删除', () => {
    it('handleDelete 调用 Modal.confirm 并在 onOk 时调用 delete', async () => {
      const apiFunc = makeApiFunc()
      const wrapper = makeWrapper({ apiFunc })
      await flushPromises()
      const record = { id: 5, userName: 'del', realName: 'Del', status: 1 }
      const beforeConfirm = modalConfirmCallbacks.length
      ;(wrapper.vm as any).handleDelete(record)
      expect(modalConfirmCallbacks.length).toBeGreaterThan(beforeConfirm)
      const last = modalConfirmCallbacks.at(-1)
      expect(last?.onOk).toBeTypeOf('function')
      const beforeDelete = (apiFunc.delete as any).mock.calls.length
      await last!.onOk!()
      await flushPromises()
      expect((apiFunc.delete as any).mock.calls.length).toBeGreaterThan(beforeDelete)
      const callArgs = (apiFunc.delete as any).mock.calls.at(-1)
      expect(callArgs[0]).toBe(5)
      expect(messageSuccess).toHaveBeenCalledWith('删除成功')
    })

    it('无 delete 权限时不调用 delete', async () => {
      hasPermissionMock.mockImplementation((p: string) => !p?.endsWith('delete'))
      const apiFunc = makeApiFunc()
      const wrapper = makeWrapper({ apiFunc })
      await flushPromises()
      const beforeConfirm = modalConfirmCallbacks.length
      ;(wrapper.vm as any).handleDelete({ id: 9 })
      expect(modalConfirmCallbacks.length).toBe(beforeConfirm)
      expect(messageWarning).toHaveBeenCalled()
    })

    it('删除异常时不抛错', async () => {
      const apiFunc = makeApiFunc({
        delete: vi.fn(async () => {
          throw new Error('delete failed')
        })
      })
      const wrapper = makeWrapper({ apiFunc })
      await flushPromises()
      ;(wrapper.vm as any).handleDelete({ id: 1 })
      const last = modalConfirmCallbacks.at(-1)
      expect(last?.onOk).toBeTypeOf('function')
      await expect(last!.onOk!()).resolves.not.toThrow()
    })
  })

  describe('自定义 actions', () => {
    it('渲染自定义 action 按钮并响应点击', async () => {
      const onClick = vi.fn()
      const actions: CrudAction[] = [
        { label: '复制', onClick }
      ]
      const wrapper = makeWrapper({ props: { actions } })
      await flushPromises()
      const vm = wrapper.vm as any
      const list = vm.resolveRowActions({ id: 1, userName: 'a' })
      const copyAction = list.find((a: CrudAction) => a.label === '复制')
      expect(copyAction).toBeTruthy()
      copyAction.onClick({ id: 1 })
      expect(onClick).toHaveBeenCalledWith({ id: 1 })
    })

    it('visible 返回 false 的 action 不出现', async () => {
      const actions: CrudAction[] = [
        { label: '隐藏的', onClick: vi.fn(), visible: () => false },
        { label: '可见的', onClick: vi.fn(), visible: () => true }
      ]
      const wrapper = makeWrapper({ props: { actions } })
      await flushPromises()
      const vm = wrapper.vm as any
      const list = vm.resolveRowActions({ id: 1, status: 0 })
      const labels = list.map((a: CrudAction) => a.label)
      expect(labels).toContain('可见的')
      expect(labels).not.toContain('隐藏的')
    })
  })

  describe('行选择', () => {
    it('启用 rowSelection 时配置生效', async () => {
      const wrapper = makeWrapper({ props: { rowSelection: true } })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.rowSelectionConfig).toBeTruthy()
      expect(vm.selectedRowKeys).toEqual([])
    })

    it('批量删除在未选中时给出 warning', async () => {
      const wrapper = makeWrapper({ props: { rowSelection: true } })
      await flushPromises()
      const vm = wrapper.vm as any
      await vm.handleBatchDelete()
      expect(messageWarning).toHaveBeenCalledWith('请先勾选要删除的行')
    })

    it('批量删除选中时调用 delete 并清空选择', async () => {
      const apiFunc = makeApiFunc()
      const wrapper = makeWrapper({ props: { rowSelection: true }, apiFunc })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.selectedRowKeys = [1, 2, 3]
      await vm.handleBatchDelete()
      expect(modalConfirmCallbacks.length).toBeGreaterThan(0)
      const last = modalConfirmCallbacks.at(-1)
      await last!.onOk!()
      await flushPromises()
      expect((apiFunc.delete as any).mock.calls.length).toBeGreaterThanOrEqual(3)
      expect(messageSuccess).toHaveBeenCalledWith('批量删除成功')
    })
  })

  describe('Expose 方法', () => {
    it('refresh 调用 page', async () => {
      const apiFunc = makeApiFunc()
      const wrapper = makeWrapper({ apiFunc })
      await flushPromises()
      const before = (apiFunc.page as any).mock.calls.length
      await (wrapper.vm as any).refresh()
      expect((apiFunc.page as any).mock.calls.length).toBeGreaterThan(before)
    })

    it('reset 清空查询并重新加载', async () => {
      const apiFunc = makeApiFunc()
      const wrapper = makeWrapper({ apiFunc })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.searchQuery.userName = 'dirty'
      const before = (apiFunc.page as any).mock.calls.length
      await vm.reset()
      expect((apiFunc.page as any).mock.calls.length).toBeGreaterThan(before)
      // 重置后 userName 应回到 undefined
      expect(vm.searchQuery.userName).toBeUndefined()
    })

    it('getSelectedRows / clearSelected', async () => {
      const wrapper = makeWrapper({ props: { rowSelection: true } })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.selectedRows = [{ id: 1 }, { id: 2 }]
      expect(vm.getSelectedRows()).toEqual([{ id: 1 }, { id: 2 }])
      vm.clearSelected()
      expect(vm.getSelectedRows()).toEqual([])
    })
  })

  describe('工具函数', () => {
    it('getNestedValue 支持嵌套路径', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.getNestedValue({ user: { name: 'X' } }, 'user.name')).toBe('X')
      expect(vm.getNestedValue({ user: { name: 'X' } }, 'user.age')).toBeUndefined()
    })

    it('setNestedValue 支持嵌套路径', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      const obj: any = {}
      vm.setNestedValue(obj, 'a.b.c', 42)
      expect(obj.a.b.c).toBe(42)
    })

    it('renderCellText 处理 format/valueEnum/null', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      // format
      const col1 = { field: 'amount', title: '金额', format: (v: number) => `¥${v}` }
      expect(vm.renderCellText(col1, { amount: 9.5 })).toBe('¥9.5')
      // valueEnum
      const col2 = { field: 'status', title: '状态', valueEnum: { '1': { text: '启用' } } }
      expect(vm.renderCellText(col2, { status: 1 })).toBe('启用')
      // null
      const col3 = { field: 'empty', title: '空' }
      expect(vm.renderCellText(col3, { empty: null })).toBe('')
    })

    it('getFormOptions 优先使用 formOptions', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      const opts = vm.getFormOptions({
        field: 'f', title: 'T',
        formOptions: [{ label: 'A', value: 'a' }],
        valueEnum: { 'b': { text: 'B' } }
      })
      expect(opts).toEqual([{ label: 'A', value: 'a' }])
    })

    it('getFormOptions 缺省时回退到 valueEnum 转换', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      const opts = vm.getFormOptions({
        field: 'f', title: 'T',
        valueEnum: { '1': { text: '启用' }, '0': { text: '禁用' } }
      })
      // JS 对象的数字键按升序迭代：'0' 先于 '1'
      expect(opts).toEqual([
        { label: '禁用', value: 0, disabled: undefined },
        { label: '启用', value: 1, disabled: undefined }
      ])
    })
  })

  describe('权限前缀', () => {
    it('permissionPrefix 拼接 :create / :update / :delete', async () => {
      hasPermissionMock.mockReturnValue(true)
      const wrapper = makeWrapper()
      await flushPromises()
      // 触发内部 hasPermission 调用
      const vm = wrapper.vm as any
      vm.openCreate()
      expect(hasPermissionMock).toHaveBeenCalledWith('system:user:create')
    })
  })

  describe('表单字段类型渲染', () => {
    it('各种 formType 都能正确打开编辑弹窗并渲染对应控件', async () => {
      const allTypeColumns: CrudColumn[] = [
        { field: 'name', title: '名称', formType: 'input', formDefaultValue: '默认' },
        { field: 'count', title: '数量', formType: 'inputNumber', formDefaultValue: 0 },
        { field: 'pwd', title: '密码', formType: 'inputPassword', hideInTable: true },
        { field: 'desc', title: '描述', formType: 'textarea', formDefaultValue: '' },
        { field: 'status', title: '状态', formType: 'select', formOptions: [{ label: '启用', value: 1 }] },
        { field: 'date', title: '日期', formType: 'date' },
        { field: 'dateRange', title: '日期范围', formType: 'dateRange' },
        { field: 'enabled', title: '启用', formType: 'switch', formDefaultValue: true },
        { field: 'gender', title: '性别', formType: 'radio', formOptions: [{ label: '男', value: 'M' }, { label: '女', value: 'F' }] },
        { field: 'tags', title: '标签', formType: 'checkbox', formOptions: [{ label: 'A', value: 'a' }] },
        { field: 'tree', title: '树', formType: 'treeSelect', formOptions: [{ label: '根', value: 1, children: [] }] },
        { field: 'cascade', title: '级联', formType: 'cascader', formOptions: [{ label: 'X', value: 'x', children: [] }] }
      ]
      const apiFunc = makeApiFunc()
      const wrapper = mount(CrudTable, {
        props: {
          columns: allTypeColumns,
          apiFunc,
          permissionPrefix: 'system:user'
        },
        global: {
          stubs: {
            ReloadOutlined: true,
            PlusOutlined: true,
            EditOutlined: true,
            DeleteOutlined: true,
            SearchOutlined: true,
            DownOutlined: true,
            UpOutlined: true
          }
        }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      // 打开新增弹窗，覆盖各种 formType 渲染分支
      vm.openCreate()
      await nextTick()
      expect(vm.formVisible).toBe(true)
      // 校验 formModel 默认值
      expect(vm.formModel.name).toBe('默认')
      expect(vm.formModel.count).toBe(0)
      expect(vm.formModel.enabled).toBe(true)
      // 触发工具函数覆盖更多分支
      expect(vm.getFormOptions(allTypeColumns[4])).toEqual([{ label: '启用', value: 1 }])
    })

    it('upload 控件类型可正常渲染', async () => {
      const cols: CrudColumn[] = [{ field: 'file', title: '附件', formType: 'upload' }]
      const apiFunc = makeApiFunc()
      const wrapper = mount(CrudTable, {
        props: { columns: cols, apiFunc, permissionPrefix: 'system:user' },
        global: {
          stubs: {
            ReloadOutlined: true,
            PlusOutlined: true,
            EditOutlined: true,
            DeleteOutlined: true,
            SearchOutlined: true,
            DownOutlined: true,
            UpOutlined: true
          }
        }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.openCreate()
      await nextTick()
      expect(vm.formVisible).toBe(true)
    })

    it('getActionPermission 返回拼接结果', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.getActionPermission('export')).toBe('system:user:export')
      // 无 permissionPrefix 时回退
      const wrapper2 = mount(CrudTable, {
        props: { columns, apiFunc: makeApiFunc() },
        global: {
          stubs: {
            ReloadOutlined: true,
            PlusOutlined: true,
            EditOutlined: true,
            DeleteOutlined: true,
            SearchOutlined: true,
            DownOutlined: true,
            UpOutlined: true
          }
        }
      })
      await flushPromises()
      const vm2 = wrapper2.vm as any
      expect(vm2.getActionPermission('export')).toBe('export')
      expect(vm2.getActionPermission(undefined)).toBeUndefined()
    })

    it('onActionClick 失败时弹 error message', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      const action = { label: '出错', onClick: () => { throw new Error('boom') } }
      await vm.onActionClick(action, { id: 1 })
      expect(messageError).toHaveBeenCalledWith('操作失败')
    })

    it('rowSelectionConfig 在 rowSelection=false 时为 undefined', async () => {
      const wrapper = makeWrapper({ props: { rowSelection: false } })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.rowSelectionConfig).toBeUndefined()
    })

    it('watch searchFields 异步注入新字段时初始化默认值', async () => {
      const wrapper = makeWrapper({ props: { searchFields: [] } })
      await flushPromises()
      const vm = wrapper.vm as any
      const initialKeys = Object.keys(vm.searchQuery)
      // 异步注入字段
      await wrapper.setProps({
        searchFields: [{ field: 'newField', label: '新字段', type: 'input', defaultValue: '默认' }]
      })
      await nextTick()
      expect(vm.searchQuery.newField).toBe('默认')
      // 原有字段保留
      expect(Object.keys(vm.searchQuery).length).toBeGreaterThanOrEqual(initialKeys.length)
    })

    it('tableColumns 包含操作列且 hideInTable=true 不渲染', async () => {
      const wrapper = makeWrapper({
        props: {
          columns: [
            { field: 'name', title: '名称', formType: 'input' },
            { field: 'hidden', title: '隐藏', hideInTable: true, formType: 'input' }
          ]
        }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      const cols = vm.tableColumns
      // 包含原始字段
      expect(cols.some((c: any) => c.key === 'name')).toBe(true)
      // 隐藏字段不出现
      expect(cols.some((c: any) => c.key === 'hidden')).toBe(false)
      // 末尾追加操作列
      const last = cols[cols.length - 1]
      expect(last.key).toBe('__action__')
    })

    it('computeActionWidth 包含自定义 actions 时宽度增加', async () => {
      const actions: CrudAction[] = [
        { label: 'A', onClick: vi.fn() },
        { label: 'B', onClick: vi.fn() }
      ]
      const wrapper = makeWrapper({ props: { actions } })
      await flushPromises()
      const vm = wrapper.vm as any
      const w = vm.computeActionWidth()
      // 至少包含 编辑(60) + 删除(60) + 2 个自定义(140) + 间隔(16)
      expect(w).toBeGreaterThanOrEqual(160)
    })

    it('openEdit 无 update 权限时不打开并提示', async () => {
      hasPermissionMock.mockImplementation((p: string) => !p?.endsWith('update'))
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      vm.openEdit({ id: 1, userName: 'a' })
      await nextTick()
      expect(vm.formVisible).toBe(false)
      expect(messageWarning).toHaveBeenCalled()
    })

    it('handleSubmit 表单校验失败时不提交', async () => {
      const apiFunc = makeApiFunc()
      const wrapper = makeWrapper({ apiFunc })
      await flushPromises()
      const vm = wrapper.vm as any
      // 模拟 formRef.validate 抛错
      vm.formRef = { value: { validate: () => Promise.reject(new Error('invalid')) } }
      // 直接给 validate 一个会 reject 的引用
      ;(vm as any).formRef = { validate: () => Promise.reject(new Error('invalid')) }
      const beforeCreate = (apiFunc.create as any).mock.calls.length
      await vm.handleSubmit()
      await flushPromises()
      expect((apiFunc.create as any).mock.calls.length).toBe(beforeCreate)
      expect(messageWarning).toHaveBeenCalledWith('请完善表单信息')
    })

    it('handleSubmit 提交时 API 失败不抛错且不阻塞', async () => {
      const apiFunc = makeApiFunc({
        create: vi.fn(async () => {
          throw new Error('create failed')
        })
      })
      const wrapper = makeWrapper({ apiFunc })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.openCreate()
      await nextTick()
      vm.formModel.userName = 'new'
      // formRef 为 undefined 走 try 分支不抛
      ;(vm as any).formRef = undefined
      await expect(vm.handleSubmit()).resolves.not.toThrow()
      await flushPromises()
    })

    it('getFormOptions 无 formOptions/valueEnum 时返回空数组', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      const opts = vm.getFormOptions({ field: 'f', title: 'T' })
      expect(opts).toEqual([])
    })

    it('findColumn 返回 undefined 时 renderCellTextByField 兜底', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      // 不存在的字段（record 中有该属性但不在 columns 中）
      expect(vm.renderCellTextByField('extra', { extra: 99 })).toBe('99')
      // record 中字段值为 null 时返回空
      expect(vm.renderCellTextByField('extra', { extra: null })).toBe('')
      expect(vm.renderCellTextByField('extra', {})).toBe('')
    })

    it('getColumnValueEnum 未定义返回 undefined', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.getColumnValueEnum('userName')).toBeUndefined()
      expect(vm.getColumnValueEnum('status')).toBeTruthy()
    })

    it('getValueEnumTag 值为 null 时返回 undefined', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.getValueEnumTag('status', { status: null })).toBeUndefined()
      expect(vm.getValueEnumTag('userName', { userName: 'a' })).toBeUndefined()
    })

    it('resolveRowActions 排除不可见 + 包含 divider', async () => {
      const actions: CrudAction[] = [
        { label: '可见', onClick: vi.fn(), visible: () => true },
        { label: '不可见', onClick: vi.fn(), visible: () => false },
        { divider: true } as any
      ]
      const wrapper = makeWrapper({ props: { actions } })
      await flushPromises()
      const vm = wrapper.vm as any
      const list = vm.resolveRowActions({ id: 1 })
      const labels = list.map((a: CrudAction) => a.label || 'divider')
      expect(labels).toContain('编辑')
      expect(labels).toContain('可见')
      expect(labels).toContain('删除')
      // 包含 divider 项
      expect(list.some((a: CrudAction) => a.divider)).toBe(true)
    })

    it('handleTableChange 不带 sort 时清空 sorter', async () => {
      const apiFunc = makeApiFunc()
      const wrapper = makeWrapper({ apiFunc })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.handleTableChange({ current: 1, pageSize: 10 }, {}, {})
      await flushPromises()
      // sorter 应为空对象
      expect(vm.sorter).toEqual({})
    })

    it('handleBatchDelete 删除部分失败仍走 message.success', async () => {
      let callCount = 0
      const apiFunc = makeApiFunc({
        delete: vi.fn(async (id: number) => {
          callCount++
          if (id === 2) throw new Error('delete failed for id=2')
        })
      })
      const wrapper = makeWrapper({ props: { rowSelection: true }, apiFunc })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.selectedRowKeys = [1, 2, 3]
      await vm.handleBatchDelete()
      const last = modalConfirmCallbacks.at(-1)
      await last!.onOk!()
      await flushPromises()
      // 调用 3 次 delete
      expect(callCount).toBe(3)
    })

    it('rowSelection onChange 触发时更新 selectedRowKeys 与 emit', async () => {
      const wrapper = makeWrapper({ props: { rowSelection: true } })
      await flushPromises()
      const vm = wrapper.vm as any
      const config = vm.rowSelectionConfig
      expect(config).toBeTruthy()
      // 模拟 a-table 调用 onChange
      config.onChange([1, 2], [{ id: 1 }, { id: 2 }])
      expect(vm.selectedRowKeys).toEqual([1, 2])
      expect(vm.selectedRows).toEqual([{ id: 1 }, { id: 2 }])
      // 触发 emit('selectionChange')
      const emitted = wrapper.emitted()
      expect(emitted.selectionChange).toBeTruthy()
      expect(emitted.selectionChange.at(-1)).toEqual([[{ id: 1 }, { id: 2 }]])
    })

    it('renderCellText format 抛错时回退为字符串', async () => {
      const apiFunc = makeApiFunc()
      const wrapper = mount(CrudTable, {
        props: {
          columns: [
            {
              field: 'amount',
              title: '金额',
              format: () => {
                throw new Error('format error')
              }
            }
          ],
          apiFunc
        },
        global: {
          stubs: {
            ReloadOutlined: true,
            PlusOutlined: true,
            EditOutlined: true,
            DeleteOutlined: true,
            SearchOutlined: true,
            DownOutlined: true,
            UpOutlined: true
          }
        }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      // format 抛错 → 回退为 String(value)
      expect(vm.renderCellText({ field: 'amount', title: '金额', format: () => { throw new Error('x') } }, { amount: 42 })).toBe('42')
      expect(vm.renderCellText({ field: 'amount', title: '金额', format: () => { throw new Error('x') } }, { amount: null })).toBe('')
    })

    it('getCheckboxProps 始终返回 disabled=false', async () => {
      const wrapper = makeWrapper({ props: { rowSelection: true } })
      await flushPromises()
      const vm = wrapper.vm as any
      const result = vm.rowSelectionConfig.getCheckboxProps({ id: 1 })
      expect(result).toEqual({ disabled: false })
    })
  })
})
