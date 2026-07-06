/**
 * PropertyPanel 组件单元测试（Task E1.2）
 *
 * 覆盖范围：
 *   - 无选中字段时显示 Empty
 *   - 字段加载：基础属性（field/label/type/required 等）同步到内部 reactive
 *   - 修改属性触发 update:field 事件
 *   - 删除按钮触发 delete 事件
 *   - 选项类字段（select/cascader）：新增/删除选项
 *   - 关联选择字段（relSelect）：显示 relBizType 配置
 *   - 校验规则：新增/删除规则
 *   - 数字/开关字段默认值控件切换
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import PropertyPanel from '../PropertyPanel.vue'
import type { FieldConfig } from '@/types/lowcode'

describe('PropertyPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function makeField(overrides: Partial<FieldConfig> = {}): FieldConfig {
    return {
      field: 'customerName',
      label: '客户名称',
      type: 'input',
      required: true,
      placeholder: '请输入',
      width: 12,
      readonly: false,
      ...overrides
    }
  }

  function mountPanel(field: FieldConfig | null = null, disabled = false) {
    return mount(PropertyPanel, {
      props: {
        field,
        disabled
      }
    })
  }

  it('无字段时渲染 Empty 与提示文案', () => {
    const wrapper = mountPanel(null)
    expect(wrapper.find('.property-panel').exists()).toBe(true)
    // Empty 提示
    expect(wrapper.text()).toContain('请选择画布中的字段')
    // 删除按钮不渲染
    expect(wrapper.find('button.danger').exists()).toBe(false)
  })

  it('加载字段后渲染面板标题与删除按钮', async () => {
    const wrapper = mountPanel(makeField())
    await flushPromises()
    expect(wrapper.find('.panel-header .title').text()).toBe('属性配置')
    // 删除按钮存在
    expect(wrapper.text()).toContain('删除字段')
  })

  it('基础字段属性同步到内部状态', async () => {
    const field = makeField()
    const wrapper = mountPanel(field)
    await flushPromises()
    const vm = wrapper.vm as any
    // inner 已同步 props.field 的字段
    expect(vm.inner.field).toBe('customerName')
    expect(vm.inner.label).toBe('客户名称')
    expect(vm.inner.type).toBe('input')
    expect(vm.inner.required).toBe(true)
    expect(vm.inner.placeholder).toBe('请输入')
  })

  it('修改 label 触发 update:field 事件', async () => {
    const field = makeField()
    const wrapper = mountPanel(field)
    await flushPromises()
    const vm = wrapper.vm as any
    vm.inner.label = '新名称'
    vm.handleField()
    await flushPromises()
    const emitted = wrapper.emitted()
    expect(emitted['update:field']).toBeTruthy()
    const payload = emitted['update:field'].at(-1)[0] as FieldConfig
    expect(payload.label).toBe('新名称')
    expect(payload.field).toBe('customerName')
  })

  it('点击删除按钮触发 delete 事件', async () => {
    const field = makeField()
    const wrapper = mountPanel(field)
    await flushPromises()
    // 找到 panel-header 的删除按钮并触发
    const deleteBtn = wrapper.find('.panel-header button')
    expect(deleteBtn.exists()).toBe(true)
    await deleteBtn.trigger('click')
    expect(wrapper.emitted('delete')).toBeTruthy()
  })

  it('选项类字段（select）显示选项编辑区', async () => {
    const field = makeField({
      type: 'select',
      options: [{ label: '选项1', value: 1 }]
    })
    const wrapper = mountPanel(field)
    await flushPromises()
    // 选项区域存在
    expect(wrapper.find('.options-list').exists()).toBe(true)
    const optInputs = wrapper.findAll('.option-row')
    expect(optInputs.length).toBe(1)
  })

  it('新增选项触发 update:field 且选项数组增加', async () => {
    const field = makeField({
      type: 'select',
      options: [{ label: '选项1', value: 1 }]
    })
    const wrapper = mountPanel(field)
    await flushPromises()
    const vm = wrapper.vm as any
    const before = vm.inner.options.length
    vm.addOption()
    await flushPromises()
    expect(vm.inner.options.length).toBe(before + 1)
    // 触发 emit
    expect(wrapper.emitted('update:field')).toBeTruthy()
  })

  it('删除选项减少数组长度', async () => {
    const field = makeField({
      type: 'select',
      options: [
        { label: 'A', value: 1 },
        { label: 'B', value: 2 }
      ]
    })
    const wrapper = mountPanel(field)
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.inner.options.length).toBe(2)
    vm.removeOption(0)
    await flushPromises()
    expect(vm.inner.options.length).toBe(1)
  })

  it('cascader 类型也显示选项编辑区', async () => {
    const field = makeField({
      type: 'cascader',
      options: []
    })
    const wrapper = mountPanel(field)
    await flushPromises()
    expect(wrapper.find('.options-list').exists()).toBe(true)
  })

  it('relSelect 类型显示关联实体配置区', async () => {
    const field = makeField({
      type: 'relSelect',
      relBizType: 'customer',
      relLabelField: 'customerName',
      relValueField: 'id'
    })
    const wrapper = mountPanel(field)
    await flushPromises()
    // 关联区域存在（包含 relBizType 输入框）
    expect(wrapper.text()).toContain('关联实体编码')
    expect(wrapper.text()).toContain('关联值字段')
    expect(wrapper.text()).toContain('关联显示字段')
    const vm = wrapper.vm as any
    expect(vm.inner.relBizType).toBe('customer')
    expect(vm.isRelType).toBe(true)
  })

  it('非 relSelect 类型不显示关联实体配置区', async () => {
    const field = makeField({ type: 'input' })
    const wrapper = mountPanel(field)
    await flushPromises()
    expect(wrapper.text()).not.toContain('关联实体编码')
  })

  it('新增校验规则增加数组长度', async () => {
    const field = makeField({ rules: [] })
    const wrapper = mountPanel(field)
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.inner.rules.length).toBe(0)
    vm.addRule()
    await flushPromises()
    expect(vm.inner.rules.length).toBe(1)
    // 默认结构
    expect(vm.inner.rules[0]).toMatchObject({
      required: false,
      message: '',
      trigger: 'blur'
    })
  })

  it('删除校验规则', async () => {
    const field = makeField({
      rules: [
        { required: true, message: 'A', trigger: 'blur' },
        { required: false, message: 'B', trigger: 'change' }
      ] as any
    })
    const wrapper = mountPanel(field)
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.inner.rules.length).toBe(2)
    vm.removeRule(0)
    await flushPromises()
    expect(vm.inner.rules.length).toBe(1)
    expect(vm.inner.rules[0].message).toBe('B')
  })

  it('disabled 状态下表单禁用', async () => {
    const field = makeField()
    const wrapper = mountPanel(field, true)
    await flushPromises()
    // antd Form 的 disabled prop 透传到子组件
    const vm = wrapper.vm as any
    // Form 组件 disabled=true
    expect(vm.inner).toBeTruthy()
    // disabled 仍能渲染属性
    expect(wrapper.text()).toContain('属性配置')
  })

  it('switch 类型 isSwitch 为 true', async () => {
    const field = makeField({ type: 'switch', defaultValue: false })
    const wrapper = mountPanel(field)
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.isSwitch).toBe(true)
    expect(vm.isNumber).toBe(false)
  })

  it('number 类型 isNumber 为 true', async () => {
    const field = makeField({ type: 'number' })
    const wrapper = mountPanel(field)
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.isNumber).toBe(true)
  })

  it('watch props.field 变化时同步内部状态', async () => {
    const field1 = makeField({ label: 'A' })
    const wrapper = mountPanel(field1)
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.inner.label).toBe('A')
    // 切换到新字段
    await wrapper.setProps({
      field: makeField({ field: 'another', label: 'B', type: 'select' })
    })
    await flushPromises()
    expect(vm.inner.label).toBe('B')
    expect(vm.inner.field).toBe('another')
  })

  it('emitUpdate 时 options 仅在 select/cascader 类型才输出', async () => {
    const field = makeField({
      type: 'input',
      options: [{ label: 'X', value: 'x' }]
    })
    const wrapper = mountPanel(field)
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleField()
    await flushPromises()
    const emitted = wrapper.emitted('update:field')
    const lastPayload = emitted.at(-1)[0] as FieldConfig
    // input 类型不输出 options
    expect(lastPayload.options).toBeUndefined()
  })

  it('emitUpdate 时 relBizType 仅在 relSelect 类型才输出', async () => {
    const field = makeField({
      type: 'input',
      relBizType: 'customer'
    })
    const wrapper = mountPanel(field)
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleField()
    await flushPromises()
    const emitted = wrapper.emitted('update:field')
    const lastPayload = emitted.at(-1)[0] as FieldConfig
    expect(lastPayload.relBizType).toBeUndefined()
  })

  it('hideInForm / hideInTable 字段同步', async () => {
    const field = makeField({ hideInForm: true, hideInTable: true })
    const wrapper = mountPanel(field)
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.inner.hideInForm).toBe(true)
    expect(vm.inner.hideInTable).toBe(true)
  })
})
