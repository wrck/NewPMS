/**
 * FieldPalette 组件单元测试（Task E1.2）
 *
 * 覆盖范围：
 *   - 渲染 10 类字段（input/textarea/number/select/date/switch/cascader/relSelect/richText/file）
 *   - 字段分组：基础字段 / 选择类 / 富内容
 *   - 拖拽开始：dataTransfer 写入 application/json 与 text/plain
 *   - expose groups 数据
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import FieldPalette from '../FieldPalette.vue'

describe('FieldPalette', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountPalette() {
    return mount(FieldPalette)
  }

  it('渲染标题与提示', () => {
    const wrapper = mountPalette()
    expect(wrapper.find('.palette-header').exists()).toBe(true)
    expect(wrapper.find('.palette-header .title').text()).toBe('字段库')
    expect(wrapper.find('.palette-header .hint').text()).toBe('拖拽到画布')
  })

  it('渲染三个分组（基础字段 / 选择类 / 富内容）', () => {
    const wrapper = mountPalette()
    const groups = wrapper.findAll('.palette-group')
    expect(groups.length).toBe(3)
    const titles = groups.map((g) => g.find('.group-title').text())
    expect(titles).toEqual(['基础字段', '选择类', '富内容'])
  })

  it('基础字段分组包含 4 项（input/textarea/number/switch）', () => {
    const wrapper = mountPalette()
    const groups = wrapper.findAll('.palette-group')
    const baseGroup = groups[0]
    const items = baseGroup.findAll('.palette-item')
    expect(items.length).toBe(4)
    const labels = items.map((i) => i.find('.item-label').text())
    expect(labels).toEqual(['单行输入', '多行输入', '数字', '开关'])
  })

  it('选择类分组包含 4 项（select/cascader/date/relSelect）', () => {
    const wrapper = mountPalette()
    const groups = wrapper.findAll('.palette-group')
    const selectGroup = groups[1]
    const items = selectGroup.findAll('.palette-item')
    expect(items.length).toBe(4)
    const labels = items.map((i) => i.find('.item-label').text())
    expect(labels).toEqual(['下拉选择', '级联选择', '日期', '关联选择'])
  })

  it('富内容分组包含 2 项（richText/file），合计 10 类字段', () => {
    const wrapper = mountPalette()
    const groups = wrapper.findAll('.palette-group')
    const richGroup = groups[2]
    const items = richGroup.findAll('.palette-item')
    expect(items.length).toBe(2)
    const labels = items.map((i) => i.find('.item-label').text())
    expect(labels).toEqual(['富文本', '文件上传'])
    // 合计 10 类
    const all = wrapper.findAll('.palette-item')
    expect(all.length).toBe(10)
  })

  it('所有字段项 draggable=true', () => {
    const wrapper = mountPalette()
    const items = wrapper.findAll('.palette-item')
    items.forEach((item) => {
      expect(item.attributes('draggable')).toBe('true')
    })
  })

  it('dragstart 时写入 dataTransfer 的 application/json 与 text/plain', () => {
    const wrapper = mountPalette()
    const firstItem = wrapper.findAll('.palette-item')[0]
    const dataTransfer = {
      effectAllowed: '',
      setData: vi.fn(),
      getData: vi.fn()
    }
    const event = new Event('dragstart', { bubbles: true }) as DragEvent
    Object.defineProperty(event, 'dataTransfer', { value: dataTransfer })
    firstItem.element.dispatchEvent(event)
    // setData 至少被调用，写入 application/json + text/plain
    expect(dataTransfer.setData).toHaveBeenCalled()
    const calls = dataTransfer.setData.mock.calls
    const appJsonCall = calls.find((c) => c[0] === 'application/json')
    const textPlainCall = calls.find((c) => c[0] === 'text/plain')
    expect(appJsonCall).toBeTruthy()
    expect(textPlainCall).toBeTruthy()
    // application/json 中应包含 type / label / dataType / prefix / source
    const payload = JSON.parse(appJsonCall[1])
    expect(payload.type).toBe('input')
    expect(payload.label).toBe('单行输入')
    expect(payload.dataType).toBe('string')
    expect(payload.prefix).toBe('input')
    expect(payload.source).toBe('palette')
  })

  it('expose groups 数据', () => {
    const wrapper = mountPalette()
    const vm = wrapper.vm as any
    expect(Array.isArray(vm.groups)).toBe(true)
    expect(vm.groups.length).toBe(3)
    const allTypes = vm.groups.flatMap((g: any) => g.items.map((i: any) => i.type))
    expect(allTypes).toEqual([
      'input',
      'textarea',
      'number',
      'switch',
      'select',
      'cascader',
      'date',
      'relSelect',
      'richText',
      'file'
    ])
  })

  it('IE 兼容降级：setData 抛错时使用 text 类型', () => {
    const wrapper = mountPalette()
    const firstItem = wrapper.findAll('.palette-item')[0]
    const dataTransfer = {
      effectAllowed: '',
      // 第一次抛错（application/json），第二次成功
      setData: vi.fn((type: string) => {
        if (type === 'application/json') {
          throw new Error('not supported')
        }
      }),
      getData: vi.fn()
    }
    const event = new Event('dragstart', { bubbles: true }) as DragEvent
    Object.defineProperty(event, 'dataTransfer', { value: dataTransfer })
    firstItem.element.dispatchEvent(event)
    const calls = dataTransfer.setData.mock.calls
    // 至少有一次失败 + 一次 text 降级
    const textCall = calls.find((c) => c[0] === 'text')
    expect(textCall).toBeTruthy()
    expect(textCall[1]).toBe('input')
  })
})
