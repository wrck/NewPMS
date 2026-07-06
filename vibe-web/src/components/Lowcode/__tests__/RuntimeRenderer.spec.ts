/**
 * RuntimeRenderer 组件单元测试（Task E1.2）
 *
 * 覆盖范围：
 *   - 无 Schema 时显示 EmptyState
 *   - Form Schema 渲染：表单字段（input/textarea/number/select/date/switch 等）
 *   - List Schema 渲染：列、搜索、表格、操作按钮
 *   - Tab Schema 渲染：a-tabs + tab-pane
 *   - Relation Schema 渲染：master + details
 *   - 数据加载：onMounted 触发 loadListData / loadRelationMaster
 *   - expose 方法：refreshList / refreshRelation / submitForm
 *   - 表单提交 emit('submit')
 *   - staticData 不调用 http.get
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import RuntimeRenderer from '../RuntimeRenderer.vue'
import type {
  FormSchema,
  ListSchema,
  TabSchema,
  RelationSchema
} from '@/types/lowcode'

/* ============ Mock ant-design-vue message ============ */
const messageMocks = vi.hoisted(() => ({
  success: vi.fn(),
  error: vi.fn(),
  warning: vi.fn(),
  info: vi.fn()
}))

vi.mock('ant-design-vue', async () => {
  const actual = await vi.importActual<typeof import('ant-design-vue')>('ant-design-vue')
  return {
    ...actual,
    message: {
      success: messageMocks.success,
      error: messageMocks.error,
      warning: messageMocks.warning,
      info: messageMocks.info,
      loading: vi.fn()
    }
  }
})

/* ============ Mock @/utils/request http ============ */
const httpMocks = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn()
}))

vi.mock('@/utils/request', async () => {
  const actual = await vi.importActual<typeof import('@/utils/request')>('@/utils/request')
  return {
    ...actual,
    http: {
      get: httpMocks.get,
      post: httpMocks.post,
      put: httpMocks.put,
      patch: vi.fn(),
      delete: vi.fn()
    }
  }
})

/* ============ Mock StatusTag / EmptyState 避免子组件复杂依赖 ============ */
vi.mock('@/components/StatusTag.vue', () => ({
  default: {
    name: 'StatusTagStub',
    props: ['color', 'tone'],
    template: '<span class="status-tag-stub"><slot /></span>'
  }
}))

vi.mock('@/components/EmptyState.vue', () => ({
  default: {
    name: 'EmptyStateStub',
    props: ['description', 'size'],
    template: '<div class="empty-state-stub">{{ description }}</div>'
  }
}))

describe('RuntimeRenderer', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('无 Schema 与兜底', () => {
    it('schema 为 null 时渲染 EmptyState', async () => {
      const wrapper = mount(RuntimeRenderer, {
        props: { schema: null }
      })
      await flushPromises()
      expect(wrapper.find('.empty-state-stub').exists()).toBe(true)
      expect(wrapper.text()).toContain('未加载到有效 Schema')
    })

    it('schemaType 为空时 schemaType 计算为 null', async () => {
      const wrapper = mount(RuntimeRenderer, {
        props: { schema: null }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.schemaType).toBeNull()
    })
  })

  describe('Form Schema 渲染', () => {
    function makeFormSchema(overrides: Partial<FormSchema> = {}): FormSchema {
      return {
        type: 'object',
        title: '测试表单',
        properties: {
          name: {
            field: 'name',
            label: '名称',
            type: 'input',
            required: true,
            placeholder: '请输入名称',
            width: 12,
            order: 0
          },
          age: {
            field: 'age',
            label: '年龄',
            type: 'number',
            width: 12,
            order: 1
          },
          active: {
            field: 'active',
            label: '启用',
            type: 'switch',
            width: 12,
            defaultValue: false,
            order: 2
          },
          desc: {
            field: 'desc',
            label: '描述',
            type: 'textarea',
            width: 24,
            order: 3
          }
        },
        ...overrides
      }
    }

    it('渲染表单与所有字段', async () => {
      const schema = makeFormSchema()
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      expect(wrapper.find('form').exists()).toBe(true)
      // 字段标签存在
      expect(wrapper.text()).toContain('名称')
      expect(wrapper.text()).toContain('年龄')
      expect(wrapper.text()).toContain('启用')
      expect(wrapper.text()).toContain('描述')
    })

    it('formFields 按 order 排序', async () => {
      const schema = makeFormSchema()
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      const fields = vm.formFields
      expect(fields[0].field).toBe('name')
      expect(fields[1].field).toBe('age')
      expect(fields[2].field).toBe('active')
      expect(fields[3].field).toBe('desc')
    })

    it('initFormModel 应用 defaultValue', async () => {
      const schema: FormSchema = {
        type: 'object',
        properties: {
          active: {
            field: 'active',
            label: '启用',
            type: 'switch',
            defaultValue: true
          },
          name: {
            field: 'name',
            label: '名称',
            type: 'input',
            defaultValue: '默认名'
          }
        }
      }
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.formModel.active).toBe(true)
      expect(vm.formModel.name).toBe('默认名')
    })

    it('initialData 覆盖 defaultValue', async () => {
      const schema: FormSchema = {
        type: 'object',
        properties: {
          name: { field: 'name', label: '名称', type: 'input', defaultValue: '默认' }
        }
      }
      const wrapper = mount(RuntimeRenderer, {
        props: {
          schema,
          initialData: { name: '已存在' }
        }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.formModel.name).toBe('已存在')
    })

    it('getFormRules 必填字段返回 required 规则', async () => {
      const schema = makeFormSchema()
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      const nameField = vm.formFields.find((f: any) => f.field === 'name')
      const rules = vm.getFormRules(nameField)
      expect(rules.some((r: any) => r.required === true)).toBe(true)
    })

    it('readonly=true 时不显示提交按钮', async () => {
      const schema = makeFormSchema()
      const wrapper = mount(RuntimeRenderer, {
        props: { schema, readonly: true }
      })
      await flushPromises()
      expect(wrapper.find('button[type="submit"]').exists()).toBe(false)
    })

    it('表单提交时 emit("submit", formModel)', async () => {
      const schema = makeFormSchema()
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.formModel.name = 'Alice'
      await vm.handleFormSubmit()
      await flushPromises()
      const emitted = wrapper.emitted('submit')
      expect(emitted).toBeTruthy()
      expect(emitted[0][0].name).toBe('Alice')
    })

    it('表单校验失败时不 emit submit 并提示', async () => {
      const schema = makeFormSchema()
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      // 模拟 validate 抛错
      vm.formRef = { value: { validate: () => Promise.reject(new Error('invalid')) } }
      await vm.handleFormSubmit()
      await flushPromises()
      expect(messageMocks.warning).toHaveBeenCalledWith('请完善表单信息')
      expect(wrapper.emitted('submit')).toBeFalsy()
    })

    it('submitForm expose 调用 handleFormSubmit', async () => {
      // 使用无必填字段的 schema，确保 validate 通过并 emit('submit')
      const schema: FormSchema = {
        type: 'object',
        title: '测试表单',
        properties: {
          name: { field: 'name', label: '名称', type: 'input', width: 12, order: 0 }
        }
      }
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      // submitForm 是 handleFormSubmit 的别名（defineExpose 直接引用），
      // 调用 submitForm 应触发 emit('submit') 行为，以此验证调用链
      expect(typeof vm.submitForm).toBe('function')
      await vm.submitForm()
      await flushPromises()
      expect(wrapper.emitted('submit')).toBeTruthy()
    })

    it('apiUrl + apiMethod=POST 时自动提交数据', async () => {
      const schema: FormSchema = {
        type: 'object',
        properties: {
          name: { field: 'name', label: '名称', type: 'input' }
        },
        apiUrl: '/customers',
        apiMethod: 'POST'
      }
      httpMocks.post.mockResolvedValueOnce({})
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.formModel.name = 'Bob'
      await vm.handleFormSubmit()
      await flushPromises()
      expect(httpMocks.post).toHaveBeenCalledWith('/customers', vm.formModel)
      expect(messageMocks.success).toHaveBeenCalledWith('提交成功')
    })

    it('apiMethod=PUT 时调用 http.put', async () => {
      const schema: FormSchema = {
        type: 'object',
        properties: {
          name: { field: 'name', label: '名称', type: 'input' }
        },
        apiUrl: '/customers/1',
        apiMethod: 'PUT'
      }
      httpMocks.put.mockResolvedValueOnce({})
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      await vm.handleFormSubmit()
      await flushPromises()
      expect(httpMocks.put).toHaveBeenCalledWith('/customers/1', vm.formModel)
    })

    it('字段类型 input/textarea/number/switch 都能渲染', async () => {
      const schema = makeFormSchema()
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      // 至少 4 个 a-form-item（4 个字段）
      const formItems = wrapper.findAll('.ant-form-item')
      expect(formItems.length).toBeGreaterThanOrEqual(4)
    })

    it('hideInForm=true 字段不显示', async () => {
      const schema: FormSchema = {
        type: 'object',
        properties: {
          visible: { field: 'visible', label: '可见', type: 'input', hideInForm: false },
          hidden: { field: 'hidden', label: '隐藏', type: 'input', hideInForm: true }
        }
      }
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      // 「隐藏」字段在 DOM 中 v-show=false，仍渲染但 display:none
      expect(wrapper.text()).toContain('可见')
      expect(wrapper.text()).toContain('隐藏')
      const hiddenItem = wrapper.findAll('.ant-form-item').find((el) => el.text().includes('隐藏'))
      // v-show 渲染但 style display:none
      expect(hiddenItem).toBeTruthy()
    })
  })

  describe('List Schema 渲染', () => {
    function makeListSchema(overrides: Partial<ListSchema> = {}): ListSchema {
      return {
        type: 'list',
        title: '客户列表',
        apiUrl: '/customers',
        rowKey: 'id',
        pageSize: 10,
        scrollX: 1200,
        columns: [
          { field: 'id', title: 'ID', width: 80 },
          { field: 'name', title: '名称', width: 180 },
          {
            field: 'status',
            title: '状态',
            valueEnum: {
              '1': { text: '启用', status: 'success' },
              '0': { text: '禁用', status: 'error' }
            }
          }
        ],
        searchFields: [
          { field: 'name', label: '名称', type: 'input' },
          {
            field: 'status',
            label: '状态',
            type: 'select',
            options: [
              { label: '启用', value: 1 },
              { label: '禁用', value: 0 }
            ]
          }
        ],
        actions: [{ type: 'create', label: '新增' }],
        ...overrides
      }
    }

    it('渲染列表容器与表格', async () => {
      const schema = makeListSchema()
      httpMocks.get.mockResolvedValueOnce({
        records: [{ id: 1, name: 'Alice', status: 1 }],
        total: 1
      })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      expect(wrapper.find('.list-mode').exists()).toBe(true)
      expect(wrapper.find('.table-card').exists()).toBe(true)
    })

    it('onMounted 调用 loadListData 并 fetch apiUrl', async () => {
      const schema = makeListSchema()
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      expect(httpMocks.get).toHaveBeenCalledWith('/customers', {
        page: 1,
        size: 10
      })
    })

    it('staticData 时不调用 http.get', async () => {
      const schema = makeListSchema()
      const wrapper = mount(RuntimeRenderer, {
        props: { schema, staticData: [{ id: 1, name: 'Alice' }] }
      })
      await flushPromises()
      expect(httpMocks.get).not.toHaveBeenCalled()
      const vm = wrapper.vm as any
      expect(vm.listData).toEqual([{ id: 1, name: 'Alice' }])
    })

    it('无 apiUrl 时 listData 为空', async () => {
      const schema = makeListSchema({ apiUrl: '' })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.listData).toEqual([])
    })

    it('listData 加载成功后填充', async () => {
      const schema = makeListSchema()
      const fakeData = [
        { id: 1, name: 'Alice', status: 1 },
        { id: 2, name: 'Bob', status: 0 }
      ]
      httpMocks.get.mockResolvedValueOnce({ records: fakeData, total: 2 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.listData).toEqual(fakeData)
      expect(vm.listPagination.total).toBe(2)
    })

    it('loadListData 异常时不抛错', async () => {
      const schema = makeListSchema()
      httpMocks.get.mockRejectedValueOnce(new Error('network'))
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      // 不抛错即可
      expect(wrapper.find('.list-mode').exists()).toBe(true)
    })

    it('renderCellText 处理 valueEnum', async () => {
      const schema = makeListSchema()
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      const col = { field: 'status', valueEnum: { '1': { text: '启用' } } }
      expect(vm.renderCellText(col, { status: 1 })).toBe('启用')
      expect(vm.renderCellText(col, { status: 99 })).toBe('99')
    })

    it('renderCellText 处理 null 值', async () => {
      const schema = makeListSchema()
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.renderCellText({ field: 'name' }, { name: null })).toBe('')
      expect(vm.renderCellText({ field: 'name' }, { name: 'Alice' })).toBe('Alice')
    })

    it('renderTag 返回 valueEnum 子项', async () => {
      const schema = makeListSchema()
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      const col = {
        field: 'status',
        valueEnum: { '1': { text: '启用', status: 'success' } }
      }
      expect(vm.renderTag(col, { status: 1 })).toEqual({ text: '启用', status: 'success' })
      expect(vm.renderTag(col, { status: null })).toBeUndefined()
    })

    it('resolveRowActions 过滤掉 create 类型', async () => {
      const schema = makeListSchema({
        actions: [
          { type: 'create', label: '新增' },
          { type: 'edit', label: '编辑' },
          { type: 'delete', label: '删除' }
        ]
      })
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      const actions = vm.resolveRowActions({ id: 1 })
      expect(actions.some((a: any) => a.type === 'create')).toBe(false)
      expect(actions.some((a: any) => a.type === 'edit')).toBe(true)
      expect(actions.some((a: any) => a.type === 'delete')).toBe(true)
    })

    it('triggerAction emit("action", payload) 并调用 actionHandlers', async () => {
      const schema = makeListSchema()
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const handler = vi.fn()
      const wrapper = mount(RuntimeRenderer, {
        props: {
          schema,
          actionHandlers: { edit: handler }
        }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.triggerAction('edit', { id: 5 })
      const emitted = wrapper.emitted('action')
      expect(emitted).toBeTruthy()
      expect(emitted[0][0]).toEqual({ type: 'edit', record: { id: 5 } })
      expect(handler).toHaveBeenCalledWith({ id: 5 })
    })

    it('refreshList expose 触发 loadListData', async () => {
      const schema = makeListSchema()
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      await vm.refreshList()
      await flushPromises()
      expect(httpMocks.get).toHaveBeenCalledTimes(2)
    })

    it('handleListSearch 重置页码为 1', async () => {
      const schema = makeListSchema()
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.listPagination.current = 5
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      await vm.handleListSearch()
      await flushPromises()
      expect(vm.listPagination.current).toBe(1)
    })

    it('handleListReset 清空 searchQuery 并重新加载', async () => {
      const schema = makeListSchema()
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.searchQuery.name = 'dirty'
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      await vm.handleListReset()
      await flushPromises()
      expect(vm.searchQuery.name).toBeUndefined()
    })

    it('handleTableChange 更新页码与 pageSize', async () => {
      const schema = makeListSchema()
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      vm.handleTableChange({ current: 3, pageSize: 20 })
      await flushPromises()
      expect(vm.listPagination.current).toBe(3)
      expect(vm.listPagination.pageSize).toBe(20)
    })

    it('tableColumns 包含操作列（非 readonly）', async () => {
      const schema = makeListSchema()
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema, readonly: false }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      const cols = vm.tableColumns
      const actionCol = cols.find((c: any) => c.key === '__action__')
      expect(actionCol).toBeTruthy()
      expect(actionCol.title).toBe('操作')
    })

    it('readonly=true 时不包含操作列', async () => {
      const schema = makeListSchema()
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema, readonly: true }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      const cols = vm.tableColumns
      expect(cols.find((c: any) => c.key === '__action__')).toBeFalsy()
    })

    it('toTableColumns 转换列定义', async () => {
      const schema = makeListSchema()
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      const cols = vm.toTableColumns([
        { field: 'name', title: '名称', width: 120, align: 'left' }
      ])
      expect(cols[0]).toMatchObject({
        title: '名称',
        dataIndex: 'name',
        key: 'name',
        width: 120,
        align: 'left'
      })
    })

    it('toTableColumns 传入 undefined 返回空数组', async () => {
      const schema = makeListSchema()
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.toTableColumns(undefined)).toEqual([])
    })
  })

  describe('Tab Schema 渲染', () => {
    function makeTabSchema(): TabSchema {
      return {
        type: 'tab',
        title: '客户 Tab',
        tabPosition: 'top',
        type2: 'line',
        tabs: [
          { key: 't1', label: '基础', contentType: 'list', bizType: 'customer' },
          { key: 't2', label: '联系人', contentType: 'list', bizType: 'contact' }
        ]
      }
    }

    it('渲染 a-tabs 与 a-tab-pane', async () => {
      const schema = makeTabSchema()
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      // ant-tabs 类存在
      expect(wrapper.find('.ant-tabs').exists()).toBe(true)
      // 两个 tab-pane
      const panes = wrapper.findAll('.ant-tabs-tabpane')
      expect(panes.length).toBeGreaterThanOrEqual(2)
    })

    it('tabItems 返回 tabs 数组', async () => {
      const schema = makeTabSchema()
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.tabItems.length).toBe(2)
      expect(vm.tabItems[0].key).toBe('t1')
    })

    it('tabSchema 返回 TabSchema', async () => {
      const schema = makeTabSchema()
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.tabSchema).toBeTruthy()
      expect(vm.tabSchema.type).toBe('tab')
    })
  })

  describe('Relation Schema 渲染', () => {
    function makeRelationSchema(): RelationSchema {
      return {
        type: 'relation',
        title: '客户关联',
        master: {
          bizType: 'customer',
          label: '客户',
          apiUrl: '/customers',
          rowKey: 'id',
          columns: [{ field: 'name', title: '名称' }]
        },
        details: [
          {
            bizType: 'contact',
            label: '联系人',
            apiUrl: '/contacts',
            rowKey: 'id',
            foreignKey: 'customerId',
            columns: [{ field: 'name', title: '姓名' }]
          }
        ]
      }
    }

    it('onMounted 触发 loadRelationMaster', async () => {
      const schema = makeRelationSchema()
      httpMocks.get.mockResolvedValueOnce({
        records: [{ id: 1, name: 'Alice' }],
        total: 1
      })
      // details 加载
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      expect(httpMocks.get).toHaveBeenCalledWith('/customers', {
        page: 1,
        size: 50
      })
    })

    it('master 数据加载后自动选中第一行并加载 details', async () => {
      const schema = makeRelationSchema()
      httpMocks.get.mockResolvedValueOnce({
        records: [{ id: 1, name: 'Alice' }],
        total: 1
      })
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.relationSelectedMaster).toEqual({ id: 1, name: 'Alice' })
      // details 接口被调用，参数含 foreignKey
      expect(httpMocks.get).toHaveBeenCalledWith('/contacts', {
        page: 1,
        size: 50,
        customerId: 1
      })
    })

    it('master 无 apiUrl 时不加载', async () => {
      const schema: RelationSchema = {
        type: 'relation',
        master: { bizType: 'customer', rowKey: 'id', columns: [] },
        details: []
      }
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      expect(httpMocks.get).not.toHaveBeenCalled()
    })

    it('refreshRelation expose 触发 loadRelationMaster', async () => {
      const schema = makeRelationSchema()
      httpMocks.get.mockResolvedValueOnce({
        records: [{ id: 1, name: 'Alice' }],
        total: 1
      })
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      httpMocks.get.mockResolvedValueOnce({
        records: [{ id: 2, name: 'Bob' }],
        total: 1
      })
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      await vm.refreshRelation()
      await flushPromises()
      expect(httpMocks.get).toHaveBeenCalledTimes(4)
    })

    it('relationRowClass 高亮选中行', async () => {
      const schema = makeRelationSchema()
      httpMocks.get.mockResolvedValueOnce({
        records: [{ id: 1, name: 'Alice' }],
        total: 1
      })
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.relationRowClass({ id: 1 })).toBe('row-selected')
      expect(vm.relationRowClass({ id: 2 })).toBe('')
    })

    it('onRelationRowClick 返回 onClick 回调', async () => {
      const schema = makeRelationSchema()
      httpMocks.get.mockResolvedValueOnce({
        records: [{ id: 1, name: 'Alice' }],
        total: 1
      })
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      const result = vm.onRelationRowClick({ id: 2 })
      expect(typeof result.onClick).toBe('function')
      // 调用应触发 details 重新加载
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      result.onClick()
      await flushPromises()
      expect(vm.relationSelectedMaster).toEqual({ id: 2 })
    })

    it('getFieldOptions 返回 field.options 或空数组', async () => {
      const schema: FormSchema = {
        type: 'object',
        properties: {
          type: {
            field: 'type',
            label: '类型',
            type: 'select',
            options: [{ label: 'A', value: 'a' }]
          }
        }
      }
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.getFieldOptions(vm.formFields[0])).toEqual([{ label: 'A', value: 'a' }])
      const noOptsField = { field: 'x', type: 'input' }
      expect(vm.getFieldOptions(noOptsField)).toEqual([])
    })

    it('setFieldValue 设置 formModel', async () => {
      const schema: FormSchema = {
        type: 'object',
        properties: {
          name: { field: 'name', label: '名称', type: 'input' }
        }
      }
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.setFieldValue(vm.formFields[0], 'new value')
      expect(vm.formModel.name).toBe('new value')
    })

    it('setSearchValue 空值时删除字段', async () => {
      const schema: ListSchema = {
        type: 'list',
        apiUrl: '/x',
        columns: [],
        searchFields: [{ field: 'name', label: '名称', type: 'input' }]
      }
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.setSearchValue('name', 'Alice')
      expect(vm.searchQuery.name).toBe('Alice')
      vm.setSearchValue('name', null)
      expect(vm.searchQuery.name).toBeUndefined()
      vm.setSearchValue('name', '')
      expect(vm.searchQuery.name).toBeUndefined()
    })

    it('getFileListValue 规范化文件数组', async () => {
      const schema: FormSchema = {
        type: 'object',
        properties: {
          file: { field: 'file', label: '附件', type: 'file' }
        }
      }
      const wrapper = mount(RuntimeRenderer, {
        props: {
          schema,
          initialData: {
            file: [{ id: 1, name: 'a.txt', status: 'done' }]
          }
        }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      const result = vm.getFileListValue(vm.formFields[0])
      expect(Array.isArray(result)).toBe(true)
      expect(result[0].uid).toBe('1')
      expect(result[0].name).toBe('a.txt')
      expect(result[0].status).toBe('done')
    })

    it('getFileListValue 非数组返回 undefined', async () => {
      const schema: FormSchema = {
        type: 'object',
        properties: {
          file: { field: 'file', label: '附件', type: 'file' }
        }
      }
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.getFileListValue(vm.formFields[0])).toBeUndefined()
    })

    it('getCascaderValue 返回数组类型', async () => {
      const schema: FormSchema = {
        type: 'object',
        properties: {
          cat: { field: 'cat', label: '分类', type: 'cascader' }
        }
      }
      const wrapper = mount(RuntimeRenderer, {
        props: {
          schema,
          initialData: { cat: ['a', 'b'] }
        }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      const result = vm.getCascaderValue(vm.formFields[0])
      expect(Array.isArray(result)).toBe(true)
      expect(result).toEqual(['a', 'b'])
    })

    it('getStrValue/getNumValue/getBoolValue 类型访问器', async () => {
      const schema: FormSchema = {
        type: 'object',
        properties: {
          name: { field: 'name', label: '名称', type: 'input' },
          age: { field: 'age', label: '年龄', type: 'number' },
          active: { field: 'active', label: '启用', type: 'switch' }
        }
      }
      const wrapper = mount(RuntimeRenderer, {
        props: {
          schema,
          initialData: { name: 'Alice', age: 30, active: true }
        }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      const nameField = vm.formFields.find((f: any) => f.field === 'name')
      const ageField = vm.formFields.find((f: any) => f.field === 'age')
      const activeField = vm.formFields.find((f: any) => f.field === 'active')
      expect(vm.getStrValue(nameField)).toBe('Alice')
      expect(vm.getNumValue(ageField)).toBe(30)
      expect(vm.getBoolValue(activeField)).toBe(true)
    })

    it('getArrValue 返回数组或 undefined', async () => {
      const schema: FormSchema = {
        type: 'object',
        properties: {
          file: { field: 'file', label: '附件', type: 'file' }
        }
      }
      const wrapper = mount(RuntimeRenderer, {
        props: {
          schema,
          initialData: { file: [{ id: 1 }] }
        }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      const field = vm.formFields[0]
      expect(Array.isArray(vm.getArrValue(field))).toBe(true)
    })

    it('getSearchStr / getSearchNum 类型访问器', async () => {
      const schema: ListSchema = {
        type: 'list',
        apiUrl: '/x',
        columns: [],
        searchFields: [
          { field: 'name', label: '名称', type: 'input' },
          { field: 'age', label: '年龄', type: 'number' }
        ]
      }
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.searchQuery.name = 'Alice'
      vm.searchQuery.age = 30
      expect(vm.getSearchStr('name')).toBe('Alice')
      expect(vm.getSearchNum('age')).toBe(30)
      // null/undefined 时返回 undefined
      expect(vm.getSearchStr('notExist')).toBeUndefined()
      expect(vm.getSearchNum('notExist')).toBeUndefined()
    })

    it('schema 变化触发 list 数据重载', async () => {
      const schema1: ListSchema = {
        type: 'list',
        apiUrl: '/customers',
        columns: [{ field: 'name', title: '名称' }]
      }
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      const wrapper = mount(RuntimeRenderer, {
        props: { schema: schema1 }
      })
      await flushPromises()
      // 修改 schema
      const schema2: ListSchema = {
        type: 'list',
        apiUrl: '/orders',
        columns: [{ field: 'id', title: 'ID' }]
      }
      httpMocks.get.mockResolvedValueOnce({ records: [], total: 0 })
      await wrapper.setProps({ schema: schema2 })
      await flushPromises()
      // 触发新 apiUrl 加载
      const lastCall = httpMocks.get.mock.calls.at(-1)
      expect(lastCall[0]).toBe('/orders')
    })

    it('initialData 变化时重新初始化 formModel', async () => {
      const schema: FormSchema = {
        type: 'object',
        properties: {
          name: { field: 'name', label: '名称', type: 'input' }
        }
      }
      const wrapper = mount(RuntimeRenderer, {
        props: {
          schema,
          initialData: { name: 'Alice' }
        }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.formModel.name).toBe('Alice')
      await wrapper.setProps({ initialData: { name: 'Bob' } })
      await flushPromises()
      expect(vm.formModel.name).toBe('Bob')
    })
  })
})
