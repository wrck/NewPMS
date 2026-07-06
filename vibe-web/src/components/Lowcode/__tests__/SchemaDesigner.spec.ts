/**
 * SchemaDesigner 组件单元测试（Task E1.2）
 *
 * 覆盖范围：
 *   - 三栏布局渲染（设计器壳 + 工具栏）
 *   - 模式切换：form / list / tab / relation
 *   - 字段拖拽：onCanvasDrop 接受 application/json payload
 *   - 字段操作：选中 / 删除 / 移动 / 复制
 *   - 属性面板 emit('update:field') 触发 syncToJson
 *   - JSON 预览 / 导入 / 导出
 *   - v-model 同步：emit('update:modelValue')
 *   - Tab 模式：新增/删除/移动 tab
 *   - Relation 模式：初始化 / 新增从表
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import SchemaDesigner from '../SchemaDesigner.vue'
import type { FormSchema, ListSchema, TabSchema } from '@/types/lowcode'

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

/* ============ Mock SchemaImporter 避免加载模板 ============ */
vi.mock('../SchemaImporter.vue', () => ({
  default: {
    name: 'SchemaImporterStub',
    props: ['visible', 'templateType', 'title'],
    emits: ['update:visible', 'import'],
    template: '<div class="importer-stub" v-if="visible">importer</div>'
  }
}))

describe('SchemaDesigner', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountDesigner(mode: 'form' | 'list' | 'tab' | 'relation' = 'form', modelValue = '') {
    return mount(SchemaDesigner, {
      props: {
        mode,
        modelValue
      }
    })
  }

  describe('基础渲染', () => {
    it('渲染工具栏与三栏布局', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      expect(wrapper.find('.schema-designer').exists()).toBe(true)
      expect(wrapper.find('.designer-toolbar').exists()).toBe(true)
      expect(wrapper.find('.designer-body').exists()).toBe(true)
    })

    it('显示模式标签', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      expect(wrapper.find('.mode-label').text()).toBe('表单设计器')
    })

    it('list 模式显示列表设计器', async () => {
      const wrapper = mountDesigner('list')
      await flushPromises()
      expect(wrapper.find('.mode-label').text()).toBe('列表设计器')
    })

    it('tab 模式显示标签页设计器', async () => {
      const wrapper = mountDesigner('tab')
      await flushPromises()
      expect(wrapper.find('.mode-label').text()).toBe('标签页设计器')
    })

    it('relation 模式显示关联页设计器', async () => {
      const wrapper = mountDesigner('relation')
      await flushPromises()
      expect(wrapper.find('.mode-label').text()).toBe('关联页设计器')
    })

    it('工具栏渲染 导入/导出/JSON/预览 按钮', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const toolbar = wrapper.find('.designer-toolbar')
      expect(toolbar.text()).toContain('导入')
      expect(toolbar.text()).toContain('导出')
      expect(toolbar.text()).toContain('JSON')
      expect(toolbar.text()).toContain('预览')
    })

    it('form/list 模式渲染 FieldPalette', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      expect(wrapper.find('.designer-left').exists()).toBe(true)
      expect(wrapper.findComponent({ name: 'FieldPalette' }).exists()).toBe(true)
    })

    it('tab 模式不渲染 FieldPalette', async () => {
      const wrapper = mountDesigner('tab')
      await flushPromises()
      expect(wrapper.find('.designer-left').exists()).toBe(false)
    })
  })

  describe('字段拖拽', () => {
    it('空画布显示拖拽提示', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      expect(wrapper.find('.canvas-empty').exists()).toBe(true)
      expect(wrapper.find('.canvas-empty').text()).toContain('拖拽左侧字段到此处')
    })

    it('onCanvasDrop 接受 application/json payload 并新增字段', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      // 模拟 drop 事件
      const payload = {
        type: 'input',
        label: '单行输入',
        dataType: 'string',
        prefix: 'input',
        source: 'palette'
      }
      const dataTransfer = {
        getData: vi.fn((type: string) => {
          if (type === 'application/json') return JSON.stringify(payload)
          if (type === 'text/plain') return 'input'
          return ''
        })
      }
      const event = {
        preventDefault: vi.fn(),
        dataTransfer
      } as unknown as DragEvent
      vm.onCanvasDrop(event)
      await flushPromises()
      expect(vm.fields.length).toBe(1)
      expect(vm.fields[0].type).toBe('input')
      expect(vm.fields[0].label).toBe('单行输入')
      // 自动选中
      expect(vm.selectedFieldKey).toBe(vm.fields[0].field)
      // 触发 message.success
      expect(messageMocks.success).toHaveBeenCalled()
      // 触发 update:modelValue
      expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    })

    it('onCanvasDrop source 非 palette 时忽略', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      const payload = { type: 'input', label: 'X', source: 'unknown' }
      const dataTransfer = {
        getData: vi.fn(() => JSON.stringify(payload))
      }
      const event = {
        preventDefault: vi.fn(),
        dataTransfer
      } as unknown as DragEvent
      vm.onCanvasDrop(event)
      await flushPromises()
      expect(vm.fields.length).toBe(0)
    })

    it('onCanvasDrop 无 type 时忽略', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      const payload = { label: 'X', source: 'palette' }
      const dataTransfer = {
        getData: vi.fn(() => JSON.stringify(payload))
      }
      const event = {
        preventDefault: vi.fn(),
        dataTransfer
      } as unknown as DragEvent
      vm.onCanvasDrop(event)
      await flushPromises()
      expect(vm.fields.length).toBe(0)
    })

    it('onCanvasDrop JSON 解析失败时静默返回', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      const dataTransfer = {
        getData: vi.fn(() => 'not-a-json')
      }
      const event = {
        preventDefault: vi.fn(),
        dataTransfer
      } as unknown as DragEvent
      vm.onCanvasDrop(event)
      await flushPromises()
      expect(vm.fields.length).toBe(0)
    })

    it('disabled 状态下 onCanvasDrop 不操作', async () => {
      const wrapper = mount(SchemaDesigner, {
        props: { mode: 'form', modelValue: '', disabled: true }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      const payload = {
        type: 'input',
        label: 'X',
        dataType: 'string',
        prefix: 'input',
        source: 'palette'
      }
      const dataTransfer = {
        getData: vi.fn(() => JSON.stringify(payload))
      }
      const event = {
        preventDefault: vi.fn(),
        dataTransfer
      } as unknown as DragEvent
      vm.onCanvasDrop(event)
      await flushPromises()
      expect(vm.fields.length).toBe(0)
    })

    it('onCanvasDragOver 调用 preventDefault 并设置 dropEffect', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      const dataTransfer = { dropEffect: '' }
      const event = {
        preventDefault: vi.fn(),
        dataTransfer
      } as unknown as DragEvent
      vm.onCanvasDragOver(event)
      expect(event.preventDefault).toHaveBeenCalled()
      expect(dataTransfer.dropEffect).toBe('copy')
    })

    it('onCanvasDragOver 无 dataTransfer 时不抛错', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      const event = { preventDefault: vi.fn() } as unknown as DragEvent
      expect(() => vm.onCanvasDragOver(event)).not.toThrow()
    })
  })

  describe('字段操作', () => {
    it('selectField 选中字段', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.fields = [{ field: 'f1', label: 'F1', type: 'input', order: 0 }]
      vm.selectField(vm.fields[0])
      expect(vm.selectedFieldKey).toBe('f1')
      expect(vm.selectedField).toEqual(vm.fields[0])
    })

    it('selectedField 计算属性返回当前选中', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.fields = [
        { field: 'f1', label: 'F1', type: 'input', order: 0 },
        { field: 'f2', label: 'F2', type: 'input', order: 1 }
      ]
      vm.selectedFieldKey = 'f2'
      expect(vm.selectedField).toEqual(vm.fields[1])
    })

    it('selectedFieldKey 为空时返回 null', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.selectedField).toBeNull()
    })

    it('updateField 更新选中字段并触发 syncToJson', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.fields = [{ field: 'f1', label: 'F1', type: 'input', order: 0 }]
      vm.selectedFieldKey = 'f1'
      vm.updateField({ field: 'f1', label: '新标签', type: 'input', order: 0 })
      expect(vm.fields[0].label).toBe('新标签')
      // 触发 update:modelValue
      expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    })

    it('updateField 选中字段不存在时不更新', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.fields = [{ field: 'f1', label: 'F1', type: 'input', order: 0 }]
      vm.selectedFieldKey = 'notExist'
      vm.updateField({ field: 'notExist', label: 'X', type: 'input' })
      expect(vm.fields[0].label).toBe('F1')
    })

    it('deleteField 删除选中字段', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.fields = [
        { field: 'f1', label: 'F1', type: 'input', order: 0 },
        { field: 'f2', label: 'F2', type: 'input', order: 1 }
      ]
      vm.selectedFieldKey = 'f1'
      vm.deleteField()
      expect(vm.fields.length).toBe(1)
      expect(vm.fields[0].field).toBe('f2')
      // 选中状态清空
      expect(vm.selectedFieldKey).toBe('')
    })

    it('deleteField 无选中时不操作', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.fields = [{ field: 'f1', label: 'F1', type: 'input', order: 0 }]
      vm.selectedFieldKey = ''
      vm.deleteField()
      expect(vm.fields.length).toBe(1)
    })

    it('moveField 向上移动字段', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.fields = [
        { field: 'f1', label: 'F1', type: 'input', order: 0 },
        { field: 'f2', label: 'F2', type: 'input', order: 1 }
      ]
      vm.moveField(1, -1)
      expect(vm.fields[0].field).toBe('f2')
      expect(vm.fields[1].field).toBe('f1')
      // order 重新计算
      expect(vm.fields[0].order).toBe(0)
      expect(vm.fields[1].order).toBe(1)
    })

    it('moveField 在边界时不操作', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.fields = [
        { field: 'f1', label: 'F1', type: 'input', order: 0 },
        { field: 'f2', label: 'F2', type: 'input', order: 1 }
      ]
      vm.moveField(0, -1) // 已在顶部
      expect(vm.fields[0].field).toBe('f1')
      vm.moveField(1, 1) // 已在底部
      expect(vm.fields[1].field).toBe('f2')
    })

    it('duplicateField 复制字段并选中', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.fields = [{ field: 'f1', label: 'F1', type: 'input', order: 0 }]
      vm.duplicateField(0)
      expect(vm.fields.length).toBe(2)
      expect(vm.fields[0].field).toBe('f1')
      expect(vm.fields[1].field).toContain('f1_copy')
      expect(vm.selectedFieldKey).toBe(vm.fields[1].field)
    })

    it('onItemDragStart 写入 text/plain 为 idx', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      const dataTransfer = {
        effectAllowed: '',
        setData: vi.fn()
      }
      const event = { dataTransfer } as unknown as DragEvent
      vm.onItemDragStart(event, 5)
      expect(dataTransfer.effectAllowed).toBe('move')
      expect(dataTransfer.setData).toHaveBeenCalledWith('text/plain', '5')
    })

    it('onItemDrop 重新排序字段', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.fields = [
        { field: 'f1', label: 'F1', type: 'input', order: 0 },
        { field: 'f2', label: 'F2', type: 'input', order: 1 },
        { field: 'f3', label: 'F3', type: 'input', order: 2 }
      ]
      const dataTransfer = {
        getData: vi.fn(() => '0')
      }
      const event = {
        preventDefault: vi.fn(),
        dataTransfer
      } as unknown as DragEvent
      vm.onItemDrop(event, 2)
      expect(vm.fields[0].field).toBe('f2')
      expect(vm.fields[1].field).toBe('f3')
      expect(vm.fields[2].field).toBe('f1')
    })

    it('onItemDrop 相同 idx 不操作', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.fields = [
        { field: 'f1', label: 'F1', type: 'input', order: 0 },
        { field: 'f2', label: 'F2', type: 'input', order: 1 }
      ]
      const dataTransfer = {
        getData: vi.fn(() => '0')
      }
      const event = {
        preventDefault: vi.fn(),
        dataTransfer
      } as unknown as DragEvent
      vm.onItemDrop(event, 0)
      expect(vm.fields[0].field).toBe('f1')
    })

    it('onItemDrop 非法 idx 不操作', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.fields = [
        { field: 'f1', label: 'F1', type: 'input', order: 0 },
        { field: 'f2', label: 'F2', type: 'input', order: 1 }
      ]
      const dataTransfer = {
        getData: vi.fn(() => 'NaN')
      }
      const event = {
        preventDefault: vi.fn(),
        dataTransfer
      } as unknown as DragEvent
      vm.onItemDrop(event, 0)
      expect(vm.fields[0].field).toBe('f1')
    })

    it('disabled 状态下 onItemDrop 不操作', async () => {
      const wrapper = mount(SchemaDesigner, {
        props: { mode: 'form', modelValue: '', disabled: true }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.fields = [
        { field: 'f1', label: 'F1', type: 'input', order: 0 },
        { field: 'f2', label: 'F2', type: 'input', order: 1 }
      ]
      const dataTransfer = {
        getData: vi.fn(() => '0')
      }
      const event = {
        preventDefault: vi.fn(),
        dataTransfer
      } as unknown as DragEvent
      vm.onItemDrop(event, 1)
      expect(vm.fields[0].field).toBe('f1')
    })
  })

  describe('Tab 模式', () => {
    it('addTab 新增 tab 并触发 syncToJson', async () => {
      const wrapper = mountDesigner('tab')
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.tabs.length).toBe(0)
      vm.addTab()
      expect(vm.tabs.length).toBe(1)
      expect(vm.tabs[0]).toMatchObject({
        contentType: 'list',
        bizType: '',
        order: 0
      })
      expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    })

    it('removeTab 删除 tab', async () => {
      const wrapper = mountDesigner('tab')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.addTab()
      vm.addTab()
      expect(vm.tabs.length).toBe(2)
      vm.removeTab(0)
      expect(vm.tabs.length).toBe(1)
    })

    it('moveTab 移动 tab', async () => {
      const wrapper = mountDesigner('tab')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.addTab()
      vm.addTab()
      const t1 = vm.tabs[0].key
      const t2 = vm.tabs[1].key
      vm.moveTab(1, -1)
      expect(vm.tabs[0].key).toBe(t2)
      expect(vm.tabs[1].key).toBe(t1)
    })

    it('moveTab 边界不操作', async () => {
      const wrapper = mountDesigner('tab')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.addTab()
      vm.addTab()
      const t1 = vm.tabs[0].key
      vm.moveTab(0, -1)
      expect(vm.tabs[0].key).toBe(t1)
    })
  })

  describe('Relation 模式', () => {
    it('initRelation 初始化关联页结构', async () => {
      const wrapper = mountDesigner('relation')
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.relation).toBeNull()
      vm.initRelation()
      expect(vm.relation).toBeTruthy()
      expect(vm.relation.type).toBe('relation')
      expect(vm.relation.master).toBeTruthy()
      expect(vm.relation.details).toEqual([])
      // 触发 update:modelValue
      expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    })

    it('addDetail 在未初始化时先初始化', async () => {
      const wrapper = mountDesigner('relation')
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.relation).toBeNull()
      vm.addDetail()
      expect(vm.relation).toBeTruthy()
      expect(vm.relation.details.length).toBe(1)
      expect(vm.relation.details[0]).toMatchObject({
        bizType: '',
        label: '从表',
        foreignKey: 'masterId'
      })
    })

    it('addDetail 已初始化时直接追加', async () => {
      const wrapper = mountDesigner('relation')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.initRelation()
      vm.addDetail()
      vm.addDetail()
      expect(vm.relation.details.length).toBe(2)
    })

    it('removeDetail 删除从表', async () => {
      const wrapper = mountDesigner('relation')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.initRelation()
      vm.addDetail()
      vm.addDetail()
      vm.removeDetail(0)
      expect(vm.relation.details.length).toBe(1)
    })
  })

  describe('JSON 预览与导入导出', () => {
    it('toggleJson 切换 JSON 视图并填充 jsonText', async () => {
      const wrapper = mountDesigner('form', '{"type":"object"}')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.toggleJson()
      await flushPromises()
      expect(vm.showJson).toBe(true)
      expect(vm.showPreview).toBe(false)
      expect(vm.jsonText).toBe('{"type":"object"}')
    })

    it('togglePreview 切换预览视图', async () => {
      const wrapper = mountDesigner('form', '{"type":"object"}')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.togglePreview()
      await flushPromises()
      expect(vm.showPreview).toBe(true)
      expect(vm.showJson).toBe(false)
    })

    it('toggleJson 与 togglePreview 互斥', async () => {
      const wrapper = mountDesigner('form', '{"type":"object"}')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.toggleJson()
      expect(vm.showJson).toBe(true)
      vm.togglePreview()
      expect(vm.showJson).toBe(false)
      expect(vm.showPreview).toBe(true)
    })

    it('toggleImporter 打开导入弹窗', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.showImporter).toBe(false)
      vm.toggleImporter()
      expect(vm.showImporter).toBe(true)
    })

    it('handleImport emit("update:modelValue") 与 message.success', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      const newJson = '{"type":"object","properties":{}}'
      vm.handleImport({ schemaJson: newJson, templateId: 1 })
      await flushPromises()
      const emitted = wrapper.emitted('update:modelValue')
      expect(emitted).toBeTruthy()
      expect(emitted.at(-1)[0]).toBe(newJson)
      expect(messageMocks.success).toHaveBeenCalledWith('导入成功')
    })

    it('downloadJson 触发浏览器下载', async () => {
      const wrapper = mountDesigner('form', '{"type":"object"}')
      await flushPromises()
      const vm = wrapper.vm as any
      const createObjectURL = vi.fn(() => 'blob:fake-url')
      const revokeObjectURL = vi.fn()
      const origURL = window.URL
      Object.defineProperty(window, 'URL', {
        value: { ...origURL, createObjectURL, revokeObjectURL },
        configurable: true
      })
      const fakeAnchor = { click: vi.fn(), href: '', download: '' }
      vi.spyOn(document, 'createElement').mockImplementation((tag: string) => {
        if (tag === 'a') return fakeAnchor as any
        return document.createElement(tag)
      })
      vi.spyOn(document.body, 'appendChild').mockImplementation((node: Node) => node)
      vi.spyOn(document.body, 'removeChild').mockImplementation((node: Node) => node)

      vm.downloadJson()
      expect(createObjectURL).toHaveBeenCalled()
      expect(fakeAnchor.click).toHaveBeenCalled()
      expect(revokeObjectURL).toHaveBeenCalled()

      Object.defineProperty(window, 'URL', { value: origURL, configurable: true })
      vi.restoreAllMocks()
    })

    it('copyJson 调用 navigator.clipboard.writeText', async () => {
      const wrapper = mountDesigner('form', '{"type":"object"}')
      await flushPromises()
      const vm = wrapper.vm as any
      const writeText = vi.fn().mockResolvedValueOnce(undefined)
      Object.assign(navigator, { clipboard: { writeText } })
      await vm.copyJson()
      expect(writeText).toHaveBeenCalledWith('{"type":"object"}')
      expect(messageMocks.success).toHaveBeenCalledWith('已复制到剪贴板')
    })

    it('copyJson 失败时显示 error', async () => {
      const wrapper = mountDesigner('form', '{"type":"object"}')
      await flushPromises()
      const vm = wrapper.vm as any
      const writeText = vi.fn().mockRejectedValueOnce(new Error('denied'))
      Object.assign(navigator, { clipboard: { writeText } })
      await vm.copyJson()
      expect(messageMocks.error).toHaveBeenCalledWith('复制失败，请手动选择')
    })
  })

  describe('modelValue 同步', () => {
    it('空 modelValue 时清空内部状态', async () => {
      const wrapper = mountDesigner('form', '')
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.fields).toEqual([])
      expect(vm.tabs).toEqual([])
      expect(vm.relation).toBeNull()
    })

    it('传入合法 FormSchema modelValue 时解析为 fields', async () => {
      const formSchema: FormSchema = {
        type: 'object',
        title: '客户表单',
        description: '描述',
        apiUrl: '/customers',
        layout: 'horizontal',
        properties: {
          name: { field: 'name', label: '名称', type: 'input', order: 0 }
        }
      }
      const wrapper = mountDesigner('form', JSON.stringify(formSchema))
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.fields.length).toBe(1)
      expect(vm.fields[0].field).toBe('name')
      expect(vm.meta.title).toBe('客户表单')
      expect(vm.meta.description).toBe('描述')
      expect(vm.meta.apiUrl).toBe('/customers')
      expect(vm.meta.layout).toBe('horizontal')
    })

    it('传入 ListSchema modelValue 时解析为 fields 与 meta', async () => {
      const listSchema: ListSchema = {
        type: 'list',
        title: '客户列表',
        apiUrl: '/customers',
        rowKey: 'custId',
        pageSize: 20,
        scrollX: 1500,
        columns: [{ field: 'name', title: '名称' }],
        formFields: [{ field: 'name', label: '名称', type: 'input' }]
      }
      const wrapper = mountDesigner('list', JSON.stringify(listSchema))
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.fields.length).toBe(1)
      expect(vm.fields[0].field).toBe('name')
      expect(vm.meta.title).toBe('客户列表')
      expect(vm.meta.apiUrl).toBe('/customers')
      expect(vm.meta.rowKey).toBe('custId')
      expect(vm.meta.pageSize).toBe(20)
      expect(vm.meta.scrollX).toBe(1500)
    })

    it('传入 TabSchema modelValue 时解析为 tabs', async () => {
      const tabSchema: TabSchema = {
        type: 'tab',
        title: '客户 Tab',
        tabPosition: 'left',
        type2: 'card',
        tabs: [
          {
            key: 't1',
            label: '基础',
            contentType: 'list',
            bizType: 'customer',
            order: 0
          }
        ]
      }
      const wrapper = mountDesigner('tab', JSON.stringify(tabSchema))
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.tabs.length).toBe(1)
      expect(vm.tabs[0].key).toBe('t1')
      expect(vm.meta.tabPosition).toBe('left')
      expect(vm.meta.type2).toBe('card')
    })

    it('传入非法 JSON 时静默处理', async () => {
      const wrapper = mountDesigner('form', 'not-a-json')
      await flushPromises()
      const vm = wrapper.vm as any
      // parseFormSchema 返回 null，不更新内部状态
      expect(vm.fields).toEqual([])
    })

    it('modelValue 变化时重新解析', async () => {
      const wrapper = mountDesigner('form', '')
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.fields).toEqual([])
      const newSchema: FormSchema = {
        type: 'object',
        properties: {
          name: { field: 'name', label: '名称', type: 'input', order: 0 }
        }
      }
      await wrapper.setProps({ modelValue: JSON.stringify(newSchema) })
      await flushPromises()
      expect(vm.fields.length).toBe(1)
    })

    it('syncToJson form 模式生成 FormSchema JSON', async () => {
      const wrapper = mountDesigner('form')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.fields = [{ field: 'name', label: '名称', type: 'input', order: 0 }]
      vm.meta.title = '测试表单'
      vm.meta.description = '描述'
      vm.meta.apiUrl = '/x'
      vm.meta.layout = 'vertical'
      vm.syncToJson()
      const emitted = wrapper.emitted('update:modelValue')
      expect(emitted).toBeTruthy()
      const json = emitted.at(-1)[0] as string
      const parsed = JSON.parse(json)
      expect(parsed.type).toBe('object')
      expect(parsed.title).toBe('测试表单')
      expect(parsed.properties.name).toBeTruthy()
      expect(parsed.properties.name.field).toBe('name')
    })

    it('syncToJson list 模式生成 ListSchema JSON', async () => {
      const wrapper = mountDesigner('list')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.fields = [
        { field: 'name', label: '名称', type: 'input', order: 0, hideInTable: false }
      ]
      vm.meta.title = '列表'
      vm.meta.apiUrl = '/items'
      vm.meta.rowKey = 'id'
      vm.meta.pageSize = 10
      vm.meta.scrollX = 1200
      vm.syncToJson()
      const emitted = wrapper.emitted('update:modelValue')
      const json = emitted.at(-1)[0] as string
      const parsed = JSON.parse(json)
      expect(parsed.type).toBe('list')
      expect(parsed.title).toBe('列表')
      expect(parsed.apiUrl).toBe('/items')
      expect(parsed.columns[0].field).toBe('name')
      expect(parsed.formFields[0].field).toBe('name')
    })

    it('syncToJson tab 模式生成 TabSchema JSON', async () => {
      const wrapper = mountDesigner('tab')
      await flushPromises()
      const vm = wrapper.vm as any
      vm.tabs = [
        { key: 't1', label: '基础', contentType: 'list', bizType: 'customer', order: 0 }
      ]
      vm.meta.title = 'Tab 配置'
      vm.meta.tabPosition = 'top'
      vm.meta.type2 = 'line'
      vm.syncToJson()
      const emitted = wrapper.emitted('update:modelValue')
      const json = emitted.at(-1)[0] as string
      const parsed = JSON.parse(json)
      expect(parsed.type).toBe('tab')
      expect(parsed.title).toBe('Tab 配置')
      expect(parsed.tabs[0].key).toBe('t1')
    })

    it('syncToJson relation 模式仅当 relation 已初始化时生成', async () => {
      const wrapper = mountDesigner('relation')
      await flushPromises()
      const vm = wrapper.vm as any
      // 未初始化，不触发 syncToJson 输出
      vm.syncToJson()
      const beforeCalls = wrapper.emitted('update:modelValue')?.length || 0
      // 初始化后触发
      vm.initRelation()
      const afterCalls = wrapper.emitted('update:modelValue')?.length || 0
      expect(afterCalls).toBeGreaterThan(beforeCalls)
    })
  })
})
