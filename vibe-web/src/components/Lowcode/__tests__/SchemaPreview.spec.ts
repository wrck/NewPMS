/**
 * SchemaPreview 组件单元测试（Task E1.2）
 *
 * 覆盖范围：
 *   - 空 schemaJson 时显示提示
 *   - 非法 JSON 时显示解析失败
 *   - FormSchema（type=object）解析成功
 *   - ListSchema（type=list）解析成功
 *   - TabSchema（type=tab）解析成功
 *   - RelationSchema（type=relation）解析成功
 *   - 未知 type 显示解析失败
 *   - readonly prop 传递给 RuntimeRenderer
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import SchemaPreview from '../SchemaPreview.vue'

/* ============ Mock RuntimeRenderer 避免完整列表渲染 ============ */
vi.mock('../RuntimeRenderer.vue', () => ({
  default: {
    name: 'RuntimeRendererStub',
    props: ['schema', 'readonly', 'bizType'],
    template: '<div class="rt-stub">{{ schema?.type || "empty" }}</div>'
  }
}))

describe('SchemaPreview', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountPreview(schemaJson: string, readonly = true, bizType?: string) {
    return mount(SchemaPreview, {
      props: {
        schemaJson,
        readonly,
        bizType
      }
    })
  }

  it('空 schemaJson 显示"请先设计 Schema 后预览"', () => {
    const wrapper = mountPreview('')
    expect(wrapper.text()).toContain('请先设计 Schema 后预览')
  })

  it('仅空白字符串时显示"请先设计"', () => {
    const wrapper = mountPreview('   ')
    expect(wrapper.text()).toContain('请先设计 Schema 后预览')
  })

  it('非法 JSON 显示"Schema 解析失败"', () => {
    const wrapper = mountPreview('not-a-json')
    expect(wrapper.text()).toContain('Schema 解析失败')
  })

  it('未知 type 显示"Schema 解析失败"', () => {
    const wrapper = mountPreview(JSON.stringify({ type: 'unknown' }))
    expect(wrapper.text()).toContain('Schema 解析失败')
  })

  it('type=object 时解析为 FormSchema 并渲染 RuntimeRenderer', async () => {
    const schema = {
      type: 'object',
      title: '客户表单',
      properties: {
        name: { type: 'input', label: '名称', field: 'name' }
      }
    }
    const wrapper = mountPreview(JSON.stringify(schema))
    await flushPromises()
    expect(wrapper.text()).toContain('实时预览')
    // 类型标签显示为 object
    expect(wrapper.find('.type-tag').text()).toBe('object')
    // RuntimeRenderer 被渲染
    expect(wrapper.find('.rt-stub').exists()).toBe(true)
    expect(wrapper.find('.rt-stub').text()).toBe('object')
  })

  it('type=list 时解析为 ListSchema 并显示类型标签', async () => {
    const schema = {
      type: 'list',
      title: '客户列表',
      columns: [{ field: 'name', title: '名称' }]
    }
    const wrapper = mountPreview(JSON.stringify(schema))
    await flushPromises()
    expect(wrapper.find('.type-tag').text()).toBe('list')
    expect(wrapper.find('.rt-stub').exists()).toBe(true)
  })

  it('type=tab 时解析为 TabSchema', async () => {
    const schema = {
      type: 'tab',
      title: '客户 Tab',
      tabs: [{ key: 't1', label: '基础', contentType: 'list', bizType: 'customer' }]
    }
    const wrapper = mountPreview(JSON.stringify(schema))
    await flushPromises()
    expect(wrapper.find('.type-tag').text()).toBe('tab')
  })

  it('type=relation 时解析为 RelationSchema', async () => {
    const schema = {
      type: 'relation',
      title: '客户关联',
      master: { bizType: 'customer', rowKey: 'id', columns: [] },
      details: [
        {
          bizType: 'contact',
          foreignKey: 'customerId',
          rowKey: 'id',
          columns: []
        }
      ]
    }
    const wrapper = mountPreview(JSON.stringify(schema))
    await flushPromises()
    expect(wrapper.find('.type-tag').text()).toBe('relation')
  })

  it('readonly prop 默认为 true', async () => {
    const schema = { type: 'object', properties: {} }
    const wrapper = mountPreview(JSON.stringify(schema))
    await flushPromises()
    // 默认 readonly=true
    expect(wrapper.props('readonly')).toBe(true)
  })

  it('bizType prop 传递到 RuntimeRenderer', async () => {
    const schema = { type: 'object', properties: {} }
    const wrapper = mountPreview(JSON.stringify(schema), false, 'customer')
    await flushPromises()
    // 使用 findComponent 获取 VueWrapper 以访问 props
    const rtStub = wrapper.findComponent({ name: 'RuntimeRendererStub' })
    expect(rtStub.exists()).toBe(true)
    // props.bizType 应为 customer
    expect(rtStub.props('bizType')).toBe('customer')
    // readonly 应为 false
    expect(rtStub.props('readonly')).toBe(false)
  })

  it('解析失败的 parsedSchemaType 为 unknown', () => {
    const wrapper = mountPreview('bad-json')
    // parsedSchema=null 时 parsedSchemaType='unknown'，但模板不展示 type-tag
    expect(wrapper.find('.type-tag').exists()).toBe(false)
  })
})
