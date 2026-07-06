/**
 * SchemaImporter 组件单元测试（Task E1.2）
 *
 * 覆盖范围：
 *   - 弹窗显隐
 *   - 模板选择下拉加载（pageTemplates 调用）
 *   - JSON 校验：空 / 非法 JSON / type 不匹配 / 正确
 *   - 上传文件读取内容
 *   - 模板选择触发 schemaJson 自动填充
 *   - 确认导入 emit('import', payload)
 *   - 取消关闭弹窗 emit('update:visible', false)
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import SchemaImporter from '../SchemaImporter.vue'

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

/* ============ Mock @/api/lowcode pageTemplates ============ */
const pageTemplatesMock = vi.hoisted(() => vi.fn())
vi.mock('@/api/lowcode', () => ({
  pageTemplates: pageTemplatesMock
}))

describe('SchemaImporter', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    pageTemplatesMock.mockResolvedValue({
      records: [
        {
          id: 1,
          templateCode: 'tpl_customer',
          templateName: '客户模板',
          templateType: 'FORM',
          schemaJson: JSON.stringify({ type: 'object', properties: {} }),
          usageCount: 0,
          status: 1
        }
      ],
      total: 1
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    // 清理 ant-design-vue Modal teleport 到 document.body 的残留内容
    document.body.innerHTML = ''
  })

  function mountImporter(visible = true, templateType: 'FORM' | 'LIST' | 'TAB' | 'RELATION' = 'FORM') {
    return mount(SchemaImporter, {
      props: {
        visible,
        templateType,
        title: '导入 Schema'
      }
    })
  }

  it('visible 时加载模板列表', async () => {
    const wrapper = mountImporter(true, 'FORM')
    await flushPromises()
    expect(pageTemplatesMock).toHaveBeenCalledWith({
      page: 1,
      size: 100,
      templateType: 'FORM',
      status: 1
    })
    // Modal 内容会被 teleport 到 document.body，需检查 body 文本
    expect(document.body.textContent).toContain('从模板选择')
  })

  it('visible 为 false 时不加载模板', async () => {
    const wrapper = mountImporter(false, 'FORM')
    await flushPromises()
    expect(pageTemplatesMock).not.toHaveBeenCalled()
  })

  it('显示 expectedType 提示（FORM -> object）', async () => {
    mountImporter(true, 'FORM')
    await flushPromises()
    expect(document.body.textContent).toContain('期望 schema type="object"')
  })

  it('显示 expectedType 提示（LIST -> list）', async () => {
    mountImporter(true, 'LIST')
    await flushPromises()
    expect(document.body.textContent).toContain('期望 schema type="list"')
  })

  it('validate 空内容返回 false', async () => {
    const wrapper = mountImporter(true, 'FORM')
    await flushPromises()
    const vm = wrapper.vm as any
    vm.schemaJson = ''
    expect(vm.validate()).toBe(false)
    expect(vm.validationErrors).toContain('JSON 内容不能为空')
  })

  it('validate 非法 JSON 返回 false 并写入错误', async () => {
    const wrapper = mountImporter(true, 'FORM')
    await flushPromises()
    const vm = wrapper.vm as any
    vm.schemaJson = 'not-a-json'
    expect(vm.validate()).toBe(false)
    expect(vm.validationErrors.some((e: string) => e.includes('JSON 解析失败'))).toBe(true)
  })

  it('validate JSON 非对象返回 false', async () => {
    const wrapper = mountImporter(true, 'FORM')
    await flushPromises()
    const vm = wrapper.vm as any
    vm.schemaJson = '"a string"'
    expect(vm.validate()).toBe(false)
    expect(vm.validationErrors).toContain('JSON 必须是对象')
  })

  it('validate FORM 期望 type=object，type 不匹配返回 false', async () => {
    const wrapper = mountImporter(true, 'FORM')
    await flushPromises()
    const vm = wrapper.vm as any
    vm.schemaJson = JSON.stringify({ type: 'list' })
    expect(vm.validate()).toBe(false)
    expect(vm.validationErrors.some((e: string) => e.includes('期望 type="object"'))).toBe(true)
  })

  it('validate LIST 期望 type=list，匹配返回 true', async () => {
    const wrapper = mountImporter(true, 'LIST')
    await flushPromises()
    const vm = wrapper.vm as any
    vm.schemaJson = JSON.stringify({ type: 'list', columns: [] })
    expect(vm.validate()).toBe(true)
  })

  it('validate TAB 期望 type=tab', async () => {
    const wrapper = mountImporter(true, 'TAB')
    await flushPromises()
    const vm = wrapper.vm as any
    // type 不匹配时应返回 false
    vm.schemaJson = JSON.stringify({ type: 'object' })
    expect(vm.validate()).toBe(false)
    expect(vm.validationErrors.some((e: string) => e.includes('期望 type="tab"'))).toBe(true)
    // type 匹配时返回 true
    vm.schemaJson = JSON.stringify({ type: 'tab', tabs: [] })
    expect(vm.validate()).toBe(true)
  })

  it('validate RELATION 期望 type=relation', async () => {
    const wrapper = mountImporter(true, 'RELATION')
    await flushPromises()
    const vm = wrapper.vm as any
    // type 不匹配时应返回 false
    vm.schemaJson = JSON.stringify({ type: 'object' })
    expect(vm.validate()).toBe(false)
    expect(vm.validationErrors.some((e: string) => e.includes('期望 type="relation"'))).toBe(true)
  })

  it('handleOk 校验失败时显示 warning 且不 emit', async () => {
    const wrapper = mountImporter(true, 'FORM')
    await flushPromises()
    const vm = wrapper.vm as any
    vm.schemaJson = 'bad-json'
    vm.handleOk()
    expect(messageMocks.warning).toHaveBeenCalledWith('请检查 JSON 格式')
    // 不触发 import
    expect(wrapper.emitted('import')).toBeFalsy()
  })

  it('handleOk 校验通过时 emit("import", payload) 与 update:visible', async () => {
    const wrapper = mountImporter(true, 'FORM')
    await flushPromises()
    const vm = wrapper.vm as any
    const validJson = JSON.stringify({ type: 'object', properties: {} })
    vm.schemaJson = validJson
    vm.selectedTemplateId = 1
    vm.handleOk()
    await flushPromises()
    const emitted = wrapper.emitted()
    expect(emitted['import']).toBeTruthy()
    expect(emitted['import'][0][0]).toEqual({
      schemaJson: validJson,
      templateId: 1
    })
    expect(emitted['update:visible']).toBeTruthy()
    expect(emitted['update:visible'][0][0]).toBe(false)
    // 重置
    expect(vm.schemaJson).toBe('')
    expect(vm.selectedTemplateId).toBeUndefined()
  })

  it('handleCancel 触发 update:visible=false 并重置状态', async () => {
    const wrapper = mountImporter(true, 'FORM')
    await flushPromises()
    const vm = wrapper.vm as any
    vm.schemaJson = 'temp'
    vm.selectedTemplateId = 5
    vm.handleCancel()
    await flushPromises()
    const emitted = wrapper.emitted('update:visible')
    expect(emitted).toBeTruthy()
    expect(emitted[0][0]).toBe(false)
    expect(vm.schemaJson).toBe('')
    expect(vm.selectedTemplateId).toBeUndefined()
  })

  it('选择模板时填充 schemaJson 并触发校验', async () => {
    const wrapper = mountImporter(true, 'FORM')
    await flushPromises()
    const vm = wrapper.vm as any
    // templates 已加载，第一条 id=1，schemaJson 为 valid FormSchema
    vm.handleTemplateChange(1)
    await flushPromises()
    expect(vm.selectedTemplateId).toBe(1)
    expect(vm.schemaJson).toContain('"type":"object"')
    // validate() 应已调用并写入无错误
    expect(vm.validationErrors.length).toBe(0)
  })

  it('handleTemplateChange 接受非 number 时不操作', async () => {
    const wrapper = mountImporter(true, 'FORM')
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleTemplateChange('1') // 字符串
    expect(vm.selectedTemplateId).toBeUndefined()
    vm.handleTemplateChange(null)
    expect(vm.selectedTemplateId).toBeUndefined()
  })

  it('expectedTypeMessage 对 FORM 返回 "object"', async () => {
    const wrapper = mountImporter(true, 'FORM')
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.expectedTypeMessage).toBe('期望 schema type="object"')
  })

  it('expectedTypeMessage 对 LIST 返回 "list"', async () => {
    const wrapper = mountImporter(true, 'LIST')
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.expectedTypeMessage).toBe('期望 schema type="list"')
  })

  it('expectedTypeMessage 在 templateType 未传时为空字符串', async () => {
    const wrapper = mount(SchemaImporter, {
      props: { visible: true, title: 'X' }
    })
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.expectedTypeMessage).toBe('')
  })

  it('loadTemplates 异常时不抛错且 templates 为空数组', async () => {
    pageTemplatesMock.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountImporter(true, 'FORM')
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.templates).toEqual([])
    expect(vm.templatesLoading).toBe(false)
  })

  it('selectOptions 返回 {value, label} 形式', async () => {
    const wrapper = mountImporter(true, 'FORM')
    await flushPromises()
    const vm = wrapper.vm as any
    const opts = vm.selectOptions
    expect(opts.length).toBe(1)
    expect(opts[0]).toEqual({
      value: 1,
      label: '客户模板（tpl_customer）'
    })
  })

  it('filterOption 大小写不敏感匹配 label', async () => {
    const wrapper = mountImporter(true, 'FORM')
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.filterOption('CUSTOMER', { label: '客户模板（tpl_customer）' })).toBe(true)
    expect(vm.filterOption('xyz', { label: '客户模板' })).toBe(false)
  })
})
