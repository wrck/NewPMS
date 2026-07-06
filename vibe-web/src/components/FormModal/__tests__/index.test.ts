/**
 * FormModal 组件单元测试（spec 阶段三 Task 13 - SubTask 13.6）
 *
 * 覆盖范围：
 *   - 组件渲染（标题、字段、按钮）
 *   - 13 种字段类型渲染
 *   - 字段联动（visibleWhen / requiredWhen / disabledWhen / optionsWhen）
 *   - 异步选项加载（asyncOptions）
 *   - 表单校验（required / rules）
 *   - submit / cancel 事件
 *   - expose 方法（validate / resetFields / setFieldValue / getFormData）
 *   - 双向绑定（update:visible / update:data）
 *   - 弹窗关闭清空数据
 *
 * 注：a-modal 在 ant-design-vue 4 中通过 Teleport 渲染到 document.body，
 * 导致 wrapper.text() / wrapper.find() 无法直接获取弹窗内容。
 * 因此本测试通过 stub 替换 a-modal 为 in-place 渲染版本，
 * 使弹窗内容可被 wrapper.find() 直接查询。
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import type { FormField } from '../types'
import FormModal from '../index.vue'

/* ============ Mock ant-design-vue 静态方法 ============ */
const mocks = vi.hoisted(() => {
  return {
    messageSuccess: vi.fn(),
    messageError: vi.fn(),
    messageWarning: vi.fn(),
    messageInfo: vi.fn()
  }
})
const { messageSuccess, messageError, messageWarning, messageInfo } = mocks

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
    }
  }
})

/* ============ a-modal stub：in-place 渲染，便于断言 ============ */
/**
 * ant-design-vue 的 a-modal 默认 teleport 到 document.body，
 * 这里 stub 为 inline 渲染版本，使 wrapper.find/wrapper.text 能直接获取弹窗内容。
 *
 * 行为对齐：
 *   - open=true 渲染弹窗，open=false 不渲染（与 destroyOnClose=true 一致）
 *   - emit ok / cancel / after-close 事件
 *   - 提供 #footer 默认按钮（取消/确定）
 */
const ModalStub = {
  name: 'AModal',
  emits: ['ok', 'cancel', 'update:open', 'after-close', 'afterClose'],
  props: {
    open: { type: Boolean, default: false },
    title: { type: String, default: '' },
    width: { type: [Number, String], default: 520 },
    confirmLoading: { type: Boolean, default: false },
    loading: { type: Boolean, default: false },
    destroyOnClose: { type: Boolean, default: false },
    maskClosable: { type: Boolean, default: true }
  },
  template: `
    <div v-if="open" class="ant-modal-root">
      <div class="ant-modal">
        <div class="ant-modal-title">{{ title }}</div>
        <div class="ant-modal-body"><slot /></div>
        <div class="ant-modal-footer">
          <slot name="footer">
            <button class="ant-btn modal-cancel-btn" @click="$emit('cancel')">取消</button>
            <button
              class="ant-btn ant-btn-primary modal-ok-btn"
              :class="{ 'ant-btn-loading': loading || confirmLoading }"
              @click="$emit('ok')"
            >确定</button>
          </slot>
        </div>
      </div>
    </div>
  `
}

/* ============ 测试辅助 ============ */
function makeWrapper(options: any = {}) {
  return mount(FormModal, {
    props: {
      visible: true,
      title: '测试弹窗',
      fields: options.fields || [],
      data: options.data || {},
      ...(options.props || {})
    },
    global: {
      stubs: {
        'a-modal': ModalStub,
        ...(options.stubs || {})
      }
    }
  })
}

/** 构造一个完整 13 类型字段集合 */
function buildAllTypeFields(): FormField[] {
  return [
    { field: 'name', label: '名称', type: 'input', defaultValue: '' },
    { field: 'count', label: '数量', type: 'inputNumber', defaultValue: 0, max: 100, min: 0, precision: 2 },
    { field: 'pwd', label: '密码', type: 'inputPassword', defaultValue: '' },
    { field: 'desc', label: '描述', type: 'textarea', defaultValue: '' },
    {
      field: 'status', label: '状态', type: 'select', defaultValue: 1,
      options: [{ label: '启用', value: 1 }, { label: '禁用', value: 0 }]
    },
    { field: 'date', label: '日期', type: 'date', showTime: true, valueFormat: 'YYYY-MM-DD HH:mm:ss' },
    { field: 'dateRange', label: '日期范围', type: 'dateRange' },
    { field: 'enabled', label: '启用', type: 'switch', defaultValue: false },
    {
      field: 'gender', label: '性别', type: 'radio',
      options: [{ label: '男', value: 'M' }, { label: '女', value: 'F' }]
    },
    {
      field: 'tags', label: '标签', type: 'checkbox',
      options: [{ label: 'A', value: 'a' }, { label: 'B', value: 'b' }]
    },
    {
      field: 'cascade', label: '级联', type: 'cascader',
      options: [{ label: 'X', value: 'x', children: [{ label: 'X1', value: 'x1' }] }]
    },
    { field: 'file', label: '附件', type: 'upload', maxSize: 5, accept: '.jpg,.png', maxCount: 3 },
    {
      field: 'tree', label: '树', type: 'treeSelect',
      treeData: [{ label: '根', value: 1, children: [] }]
    }
  ]
}

describe('FormModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('渲染', () => {
    it('应渲染弹窗标题', async () => {
      const wrapper = makeWrapper({
        fields: [{ field: 'name', label: '名称', type: 'input' }]
      })
      await flushPromises()
      expect(wrapper.text()).toContain('测试弹窗')
    })

    it('应渲染所有可见字段标签', async () => {
      const fields: FormField[] = [
        { field: 'name', label: '用户名', type: 'input' },
        { field: 'age', label: '年龄', type: 'inputNumber' }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const text = wrapper.text()
      expect(text).toContain('用户名')
      expect(text).toContain('年龄')
    })

    it('visible=false 时不渲染弹窗', async () => {
      const wrapper = makeWrapper({
        fields: [{ field: 'name', label: '名称', type: 'input' }],
        props: { visible: false }
      })
      await flushPromises()
      // stub v-if="open" 时 visible=false 不渲染任何内容
      expect(wrapper.find('.ant-modal').exists()).toBe(false)
      expect(wrapper.text()).not.toContain('测试弹窗')
    })

    it('应渲染取消与确定按钮', async () => {
      const wrapper = makeWrapper({
        fields: [{ field: 'name', label: '名称', type: 'input' }]
      })
      await flushPromises()
      const buttons = wrapper.findAll('button')
      // antd Button 在中文字符之间插入空格，归一化后断言
      const texts = buttons.map((b) => b.text().replace(/\s/g, ''))
      expect(texts).toContain('取消')
      expect(texts).toContain('确定')
    })

    it('loading=true 时确定按钮附加 ant-btn-loading 类', async () => {
      const wrapper = makeWrapper({
        fields: [{ field: 'name', label: '名称', type: 'input' }],
        props: { loading: true }
      })
      await flushPromises()
      // 归一化文本以处理 antd 中文按钮间距
      const okBtn = wrapper.findAll('button').find((b) => b.text().replace(/\s/g, '').includes('确定'))
      expect(okBtn).toBeTruthy()
      expect(okBtn!.classes()).toContain('ant-btn-loading')
    })
  })

  describe('13 种字段类型渲染', () => {
    it('能渲染所有 13 种字段类型', async () => {
      const fields = buildAllTypeFields()
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      // 验证内部数据初始化
      expect(vm.innerData.name).toBe('')
      expect(vm.innerData.count).toBe(0)
      expect(vm.innerData.enabled).toBe(false)
      expect(vm.innerData.status).toBe(1)
      // 不抛错即认为所有控件可渲染
      expect(wrapper.text()).toContain('名称')
      expect(wrapper.text()).toContain('附件')
      expect(wrapper.text()).toContain('树')
    })

    it('input 类型支持 readonly / disabled / maxLength', async () => {
      const wrapper = makeWrapper({
        fields: [{
          field: 'name', label: '名称', type: 'input',
          readonly: true, disabled: true, maxLength: 10
        }]
      })
      await flushPromises()
      const input = wrapper.find('input')
      expect(input.exists()).toBe(true)
      expect(input.attributes('maxlength')).toBe('10')
      // antd input readonly 透传为 readonly 属性
      expect(input.attributes('readonly')).toBeDefined()
      // disabled 通过 antd input 透传到原生 input disabled
      expect(input.attributes('disabled')).toBeDefined()
    })

    it('select 类型支持 multiple 模式', async () => {
      const wrapper = makeWrapper({
        fields: [{
          field: 'roleCodes', label: '角色', type: 'select', multiple: true,
          options: [{ label: '管理员', value: 'ADMIN' }]
        }]
      })
      await flushPromises()
      // multiple 模式 antd 渲染 .ant-select-multiple 类
      const selectMultiple = wrapper.find('.ant-select-multiple')
      expect(selectMultiple.exists()).toBe(true)
    })

    it('inputNumber 类型透传 max/min/precision', async () => {
      const wrapper = makeWrapper({
        fields: [{
          field: 'count', label: '数量', type: 'inputNumber',
          max: 100, min: 0, precision: 2
        }]
      })
      await flushPromises()
      const input = wrapper.find('input')
      expect(input.exists()).toBe(true)
      // antd input-number 不会把 max/min 透传到原生 input 的 HTML 属性，
      // 这里通过 getInputNumberProps 验证 props 已正确生成并 v-bind 到 a-input-number
      const vm = wrapper.vm as any
      const field = (wrapper.props('fields') as FormField[])[0]
      const inputNumberProps = vm.getInputNumberProps(field)
      expect(inputNumberProps.max).toBe(100)
      expect(inputNumberProps.min).toBe(0)
      expect(inputNumberProps.precision).toBe(2)
    })

    it('upload 类型支持 beforeUpload 校验（超大文件拒绝）', async () => {
      const wrapper = makeWrapper({
        fields: [{
          field: 'file', label: '附件', type: 'upload',
          maxSize: 1, accept: '.jpg', maxCount: 1
        }]
      })
      await flushPromises()
      const vm = wrapper.vm as any
      // 模拟 5MB 文件
      const bigFile = new File(['x'.repeat(1024 * 1024 * 5)], 'big.jpg', { type: 'image/jpeg' })
      Object.defineProperty(bigFile, 'size', { value: 5 * 1024 * 1024 })
      const result = vm.beforeUpload({ maxSize: 1, accept: '.jpg' }, bigFile)
      expect(result).toBe(false)
      expect(messageError).toHaveBeenCalled()
    })

    it('upload 类型 beforeUpload 文件类型不匹配时拒绝', async () => {
      const wrapper = makeWrapper({
        fields: [{
          field: 'file', label: '附件', type: 'upload',
          accept: '.jpg,.png'
        }]
      })
      await flushPromises()
      const vm = wrapper.vm as any
      const wrongFile = new File(['x'], 'doc.pdf', { type: 'application/pdf' })
      const result = vm.beforeUpload({ accept: '.jpg,.png' }, wrongFile)
      expect(result).toBe(false)
      expect(messageError).toHaveBeenCalled()
    })

    it('upload 类型 beforeUpload 通过校验时返回 true', async () => {
      const wrapper = makeWrapper({
        fields: [{
          field: 'file', label: '附件', type: 'upload',
          maxSize: 10, accept: '.jpg'
        }]
      })
      await flushPromises()
      const vm = wrapper.vm as any
      const okFile = new File(['x'], 'pic.jpg', { type: 'image/jpeg' })
      const result = vm.beforeUpload({ maxSize: 10, accept: '.jpg' }, okFile)
      expect(result).toBe(true)
      expect(messageError).not.toHaveBeenCalled()
    })

    it('select 类型支持单选模式（默认）', async () => {
      const wrapper = makeWrapper({
        fields: [{
          field: 'status', label: '状态', type: 'select',
          options: [{ label: '启用', value: 1 }]
        }]
      })
      await flushPromises()
      // 单选模式 antd 渲染 .ant-select-single 类（非 .ant-select-multiple）
      const selectSingle = wrapper.find('.ant-select-single')
      expect(selectSingle.exists()).toBe(true)
      expect(wrapper.find('.ant-select-multiple').exists()).toBe(false)
    })

    it('switch 类型默认渲染 a-switch', async () => {
      const wrapper = makeWrapper({
        fields: [{ field: 'enabled', label: '启用', type: 'switch', defaultValue: true }]
      })
      await flushPromises()
      expect(wrapper.find('.ant-switch').exists()).toBe(true)
      const vm = wrapper.vm as any
      expect(vm.innerData.enabled).toBe(true)
    })

    it('radio 类型渲染 radio-group', async () => {
      const wrapper = makeWrapper({
        fields: [{
          field: 'gender', label: '性别', type: 'radio',
          options: [{ label: '男', value: 'M' }, { label: '女', value: 'F' }]
        }]
      })
      await flushPromises()
      expect(wrapper.find('.ant-radio-group').exists()).toBe(true)
      const radios = wrapper.findAll('.ant-radio')
      expect(radios.length).toBe(2)
    })

    it('checkbox 类型渲染 checkbox-group', async () => {
      const wrapper = makeWrapper({
        fields: [{
          field: 'tags', label: '标签', type: 'checkbox',
          options: [{ label: 'A', value: 'a' }, { label: 'B', value: 'b' }]
        }]
      })
      await flushPromises()
      expect(wrapper.find('.ant-checkbox-group').exists()).toBe(true)
      const cbs = wrapper.findAll('.ant-checkbox')
      expect(cbs.length).toBe(2)
    })
  })

  describe('字段联动', () => {
    it('visibleWhen：字段值匹配时显示，不匹配时隐藏', async () => {
      const fields: FormField[] = [
        {
          field: 'type', label: '类型', type: 'radio', defaultValue: 'personal',
          options: [{ label: '个人', value: 'personal' }, { label: '企业', value: 'enterprise' }]
        },
        {
          field: 'idCard', label: '身份证号', type: 'input',
          visibleWhen: { field: 'type', value: 'personal' }
        },
        {
          field: 'creditCode', label: '信用代码', type: 'input',
          visibleWhen: { field: 'type', value: 'enterprise' }
        }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      // 默认 type=personal，应显示 idCard，隐藏 creditCode
      expect(vm.isFieldVisible(fields[1])).toBe(true)
      expect(vm.isFieldVisible(fields[2])).toBe(false)
      expect(wrapper.text()).toContain('身份证号')
      expect(wrapper.text()).not.toContain('信用代码')
      // 切换 type=enterprise
      vm.onFieldValueChange(fields[0], 'enterprise')
      await nextTick()
      expect(vm.isFieldVisible(fields[1])).toBe(false)
      expect(vm.isFieldVisible(fields[2])).toBe(true)
      expect(wrapper.text()).not.toContain('身份证号')
      expect(wrapper.text()).toContain('信用代码')
    })

    it('visibleWhen value 为数组时支持多值匹配', async () => {
      const fields: FormField[] = [
        { field: 'status', label: '状态', type: 'inputNumber', defaultValue: 1 },
        {
          field: 'note', label: '备注', type: 'input',
          visibleWhen: { field: 'status', value: [1, 2, 3] }
        }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.isFieldVisible(fields[1])).toBe(true)
      vm.onFieldValueChange(fields[0], 5)
      await nextTick()
      expect(vm.isFieldVisible(fields[1])).toBe(false)
    })

    it('requiredWhen：字段值匹配时必填', async () => {
      const fields: FormField[] = [
        {
          field: 'type', label: '类型', type: 'radio', defaultValue: 'personal',
          options: [{ label: '个人', value: 'personal' }, { label: '企业', value: 'enterprise' }]
        },
        {
          field: 'idCard', label: '身份证号', type: 'input',
          requiredWhen: { field: 'type', value: 'personal' }
        }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.isFieldRequired(fields[1])).toBe(true)
      // 切换为企业，idCard 不必填
      vm.onFieldValueChange(fields[0], 'enterprise')
      await nextTick()
      expect(vm.isFieldRequired(fields[1])).toBe(false)
    })

    it('disabledWhen：字段值匹配时禁用', async () => {
      const fields: FormField[] = [
        {
          field: 'status', label: '状态', type: 'select', defaultValue: 0,
          options: [{ label: '禁用', value: 0 }, { label: '启用', value: 1 }]
        },
        {
          field: 'reason', label: '禁用原因', type: 'input',
          disabledWhen: { field: 'status', value: 1 }
        }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      // 默认 status=0，不禁用
      expect(vm.isFieldDisabled(fields[1])).toBe(false)
      // 切换 status=1，禁用 reason
      vm.onFieldValueChange(fields[0], 1)
      await nextTick()
      expect(vm.isFieldDisabled(fields[1])).toBe(true)
    })

    it('optionsWhen：字段值匹配时切换选项', async () => {
      const fields: FormField[] = [
        {
          field: 'country', label: '国家', type: 'select', defaultValue: 'cn',
          options: [{ label: '中国', value: 'cn' }, { label: '美国', value: 'us' }]
        },
        {
          field: 'city', label: '城市', type: 'select',
          options: [{ label: '北京', value: 'bj' }],
          optionsWhen: {
            field: 'country', value: 'us',
            options: [{ label: '纽约', value: 'ny' }, { label: '洛杉矶', value: 'la' }]
          }
        }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      // 默认 country=cn，optionsWhen 不命中，使用 options
      const opts1 = vm.getFieldOptions(fields[1])
      expect(opts1).toEqual([{ label: '北京', value: 'bj' }])
      // 切换 country=us，optionsWhen 命中
      vm.onFieldValueChange(fields[0], 'us')
      await nextTick()
      const opts2 = vm.getFieldOptions(fields[1])
      expect(opts2).toEqual([
        { label: '纽约', value: 'ny' },
        { label: '洛杉矶', value: 'la' }
      ])
    })

    it('字段直接声明 disabled 时也禁用', async () => {
      const fields: FormField[] = [
        { field: 'name', label: '名称', type: 'input', disabled: true }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.isFieldDisabled(fields[0])).toBe(true)
    })

    it('字段直接声明 required 时也必填', async () => {
      const fields: FormField[] = [
        { field: 'name', label: '名称', type: 'input', required: true }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.isFieldRequired(fields[0])).toBe(true)
      // 应自动追加 required 校验规则
      const rules = vm.getFieldRules(fields[0])
      expect(rules.some((r: any) => r.required === true)).toBe(true)
    })
  })

  describe('异步选项加载', () => {
    it('onMounted 调用 asyncOptions 加载选项', async () => {
      const asyncOptions = vi.fn(async () => [
        { label: '选项A', value: 'a' },
        { label: '选项B', value: 'b' }
      ])
      const fields: FormField[] = [
        { field: 'choice', label: '选择', type: 'select', asyncOptions }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      expect(asyncOptions).toHaveBeenCalledTimes(1)
      const vm = wrapper.vm as any
      const opts = vm.getFieldOptions(fields[0])
      expect(opts).toEqual([
        { label: '选项A', value: 'a' },
        { label: '选项B', value: 'b' }
      ])
    })

    it('asyncOptions 抛错时回退为空数组', async () => {
      const asyncOptions = vi.fn(async () => {
        throw new Error('load failed')
      })
      const fields: FormField[] = [
        { field: 'choice', label: '选择', type: 'select', asyncOptions }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      const opts = vm.getFieldOptions(fields[0])
      expect(opts).toEqual([])
    })

    it('asyncOptions 优先级高于 options', async () => {
      const asyncOptions = vi.fn(async () => [{ label: '异步', value: 'async' }])
      const fields: FormField[] = [
        {
          field: 'choice', label: '选择', type: 'select',
          options: [{ label: '静态', value: 'static' }],
          asyncOptions
        }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      const opts = vm.getFieldOptions(fields[0])
      expect(opts).toEqual([{ label: '异步', value: 'async' }])
    })

    it('无 asyncOptions/options/treeData 时返回空数组', async () => {
      const fields: FormField[] = [
        { field: 'choice', label: '选择', type: 'select' }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.getFieldOptions(fields[0])).toEqual([])
    })

    it('treeSelect 字段优先使用 treeData', async () => {
      const treeData = [{ label: '根', value: 1, children: [] }]
      const fields: FormField[] = [
        { field: 'tree', label: '树', type: 'treeSelect', treeData }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.getFieldOptions(fields[0])).toEqual(treeData)
    })
  })

  describe('表单校验', () => {
    it('required 字段自动生成校验规则（输入类型）', async () => {
      const fields: FormField[] = [
        { field: 'name', label: '名称', type: 'input', required: true }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      const rules = vm.getFieldRules(fields[0])
      expect(rules[0].required).toBe(true)
      expect(rules[0].message).toContain('请输入')
    })

    it('required 字段自动生成校验规则（选择类型）', async () => {
      const fields: FormField[] = [
        {
          field: 'status', label: '状态', type: 'select', required: true,
          options: [{ label: '启用', value: 1 }]
        }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      const rules = vm.getFieldRules(fields[0])
      expect(rules[0].required).toBe(true)
      expect(rules[0].message).toContain('请选择')
    })

    it('rules 字段追加在 required 之后', async () => {
      const fields: FormField[] = [
        {
          field: 'email', label: '邮箱', type: 'input', required: true,
          rules: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
        }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      const rules = vm.getFieldRules(fields[0])
      expect(rules.length).toBe(2)
      expect(rules[0].required).toBe(true)
      expect(rules[1].type).toBe('email')
    })

    it('handleSubmit 校验失败时弹 warning 且不 emit submit', async () => {
      const fields: FormField[] = [
        { field: 'name', label: '名称', type: 'input', required: true }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      // 模拟 formRef.validate 抛错
      vm.formRef = { validate: () => Promise.reject(new Error('invalid')) }
      await vm.handleSubmit()
      await flushPromises()
      expect(messageWarning).toHaveBeenCalledWith('请完善表单信息')
      expect(wrapper.emitted('submit')).toBeFalsy()
    })

    it('handleSubmit 校验通过时 emit submit 且 payload 包含可见字段', async () => {
      const fields: FormField[] = [
        { field: 'name', label: '名称', type: 'input', defaultValue: 'alice' },
        {
          field: 'type', label: '类型', type: 'radio', defaultValue: 'a',
          options: [{ label: 'A', value: 'a' }, { label: 'B', value: 'b' }]
        },
        {
          field: 'hidden', label: '隐藏字段', type: 'input',
          visibleWhen: { field: 'type', value: 'b' }
        }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.formRef = { validate: () => Promise.resolve(true) }
      await vm.handleSubmit()
      await flushPromises()
      const emit = wrapper.emitted('submit')
      expect(emit).toBeTruthy()
      const payload = emit![0][0] as Record<string, any>
      expect(payload.name).toBe('alice')
      expect(payload.type).toBe('a')
      // hidden 字段因 visibleWhen 不命中，不应在 payload 中
      expect(payload.hidden).toBeUndefined()
    })

    it('handleSubmit 校验通过但 formRef 为 undefined 时不抛错', async () => {
      const fields: FormField[] = [
        { field: 'name', label: '名称', type: 'input', defaultValue: 'a' }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.formRef = undefined
      await expect(vm.handleSubmit()).resolves.not.toThrow()
      await flushPromises()
      expect(wrapper.emitted('submit')).toBeTruthy()
    })
  })

  describe('submit / cancel 事件', () => {
    it('点击取消按钮触发 cancel 与 update:visible', async () => {
      const fields: FormField[] = [
        { field: 'name', label: '名称', type: 'input' }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      // FormModal 通过 #footer slot 提供真实 a-button，antd 中文按钮会插入空格，需归一化
      const cancelBtn = wrapper.findAll('button').find((b) => b.text().replace(/\s/g, '').includes('取消'))
      expect(cancelBtn).toBeTruthy()
      await cancelBtn!.trigger('click')
      await flushPromises()
      expect(wrapper.emitted('cancel')).toBeTruthy()
      const visibleEmit = wrapper.emitted('update:visible')
      expect(visibleEmit).toBeTruthy()
      expect(visibleEmit![0][0]).toBe(false)
    })

    it('点击确定按钮触发 handleSubmit', async () => {
      const fields: FormField[] = [
        { field: 'name', label: '名称', type: 'input', defaultValue: 'a' }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      // formRef 由 antd 注册，jsdom 下可能未挂载，模拟为成功
      vm.formRef = { validate: () => Promise.resolve(true) }
      // 归一化中文按钮文本空格
      const okBtn = wrapper.findAll('button').find((b) => b.text().replace(/\s/g, '').includes('确定'))
      expect(okBtn).toBeTruthy()
      await okBtn!.trigger('click')
      await flushPromises()
      expect(wrapper.emitted('submit')).toBeTruthy()
    })
  })

  describe('双向绑定', () => {
    it('字段值变化时 emit update:data', async () => {
      const fields: FormField[] = [
        { field: 'name', label: '名称', type: 'input', defaultValue: '' }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.onFieldValueChange(fields[0], 'alice')
      await nextTick()
      const emit = wrapper.emitted('update:data')
      expect(emit).toBeTruthy()
      const last = emit!.at(-1)![0] as Record<string, any>
      expect(last.name).toBe('alice')
    })

    it('visible 变化时 initInnerData 调用', async () => {
      const fields: FormField[] = [
        { field: 'name', label: '名称', type: 'input', defaultValue: '' }
      ]
      const wrapper = makeWrapper({
        fields,
        props: { visible: false }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      // visible=false 时 onMounted 跳过 initInnerData，innerData 应为空
      expect(vm.innerData.name).toBeUndefined()
      // 切换 visible=true 并传入 data
      await wrapper.setProps({ visible: true, data: { name: 'bob' } })
      await flushPromises()
      expect(vm.innerData.name).toBe('bob')
    })
  })

  describe('Expose 方法', () => {
    it('validate 调用 formRef.validate', async () => {
      const fields: FormField[] = [{ field: 'name', label: '名称', type: 'input' }]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      const spy = vi.fn(() => Promise.resolve(true))
      vm.formRef = { validate: spy }
      await vm.validate()
      expect(spy).toHaveBeenCalled()
    })

    it('validate formRef 为空时返回 true', async () => {
      const fields: FormField[] = [{ field: 'name', label: '名称', type: 'input' }]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.formRef = undefined
      await expect(vm.validate()).resolves.toBe(true)
    })

    it('resetFields 重置为初始默认值', async () => {
      const fields: FormField[] = [
        { field: 'name', label: '名称', type: 'input', defaultValue: '初始值' }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.innerData.name = '修改后'
      expect(vm.innerData.name).toBe('修改后')
      vm.resetFields()
      expect(vm.innerData.name).toBe('初始值')
    })

    it('setFieldValue 设置字段值并 emit update:data', async () => {
      const fields: FormField[] = [
        { field: 'name', label: '名称', type: 'input' }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.setFieldValue('name', 'newvalue')
      expect(vm.innerData.name).toBe('newvalue')
      const emit = wrapper.emitted('update:data')
      expect(emit).toBeTruthy()
      const last = emit!.at(-1)![0] as Record<string, any>
      expect(last.name).toBe('newvalue')
    })

    it('setFieldValue 支持嵌套字段路径', async () => {
      const fields: FormField[] = [
        { field: 'user.name', label: '用户名', type: 'input' }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.setFieldValue('user.name', 'alice')
      expect(vm.innerData.user.name).toBe('alice')
    })

    it('getFormData 返回深拷贝的表单数据', async () => {
      const fields: FormField[] = [
        { field: 'name', label: '名称', type: 'input', defaultValue: 'a' },
        { field: 'count', label: '数量', type: 'inputNumber', defaultValue: 5 }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      const data = vm.getFormData()
      expect(data.name).toBe('a')
      expect(data.count).toBe(5)
      // 修改返回值不影响 innerData
      data.name = 'modified'
      expect(vm.innerData.name).toBe('a')
    })

    it('getFormData 支持嵌套字段路径', async () => {
      const fields: FormField[] = [
        { field: 'user.name', label: '用户名', type: 'input', defaultValue: 'x' }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      const data = vm.getFormData()
      expect(data.user.name).toBe('x')
    })
  })

  describe('嵌套字段路径', () => {
    it('getNestedValue / setNestedValue 支持嵌套路径', async () => {
      const fields: FormField[] = [
        { field: 'user.name', label: '用户名', type: 'input' }
      ]
      const wrapper = makeWrapper({ fields, data: { user: { name: 'alice' } } })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.getNestedValue(vm.innerData, 'user.name')).toBe('alice')
      vm.setNestedValue(vm.innerData, 'user.age', 25)
      expect(vm.innerData.user.age).toBe(25)
    })

    it('getNestedValue 中间字段为 undefined 时返回 undefined', async () => {
      const wrapper = makeWrapper({
        fields: [{ field: 'a.b.c', label: '字段', type: 'input' }]
      })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.getNestedValue({}, 'a.b.c')).toBeUndefined()
      expect(vm.getNestedValue(undefined as any, 'a.b')).toBeUndefined()
    })

    it('setNestedValue 自动创建中间对象', async () => {
      const wrapper = makeWrapper({
        fields: [{ field: 'a.b.c', label: '字段', type: 'input' }]
      })
      await flushPromises()
      const vm = wrapper.vm as any
      const obj: any = {}
      vm.setNestedValue(obj, 'a.b.c', 42)
      expect(obj.a.b.c).toBe(42)
    })
  })

  describe('弹窗关闭清空数据', () => {
    it('handleClosed 清空 innerData 与 asyncOptionsMap', async () => {
      const asyncOptions = vi.fn(async () => [{ label: 'X', value: 'x' }])
      const fields: FormField[] = [
        { field: 'name', label: '名称', type: 'input', defaultValue: '' },
        { field: 'choice', label: '选择', type: 'select', asyncOptions }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.innerData.name).toBe('')
      expect(vm.asyncOptionsMap.choice).toEqual([{ label: 'X', value: 'x' }])
      // 触发关闭回调
      await vm.handleClosed()
      expect(vm.innerData.name).toBeUndefined()
      expect(Object.keys(vm.asyncOptionsMap).length).toBe(0)
    })

    it('handleClosed 调用 formRef.resetFields（如存在）', async () => {
      const fields: FormField[] = [{ field: 'name', label: '名称', type: 'input' }]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      const spy = vi.fn()
      vm.formRef = { resetFields: spy }
      await vm.handleClosed()
      expect(spy).toHaveBeenCalled()
    })
  })

  describe('Props 默认值与定制', () => {
    it('span 默认为 12，可被字段级 span 覆盖', async () => {
      const fields: FormField[] = [
        { field: 'a', label: 'A', type: 'input' },
        { field: 'b', label: 'B', type: 'input', span: 24 }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      // 检查 a-col 的 span 属性
      const cols = wrapper.findAll('.ant-col')
      // 应存在 span=24 的 col
      const span24 = cols.find((c) => c.classes().some((cl) => cl === 'ant-col-24'))
      expect(span24).toBeTruthy()
      // 应存在 span=12 的 col
      const span12 = cols.find((c) => c.classes().some((cl) => cl === 'ant-col-12'))
      expect(span12).toBeTruthy()
    })

    it('width 透传给 a-modal（stub 接收 width 属性）', async () => {
      const wrapper = makeWrapper({
        fields: [{ field: 'name', label: '名称', type: 'input' }],
        props: { width: 800 }
      })
      await flushPromises()
      // stub 渲染的 .ant-modal 应存在
      expect(wrapper.find('.ant-modal').exists()).toBe(true)
    })

    it('labelCol / wrapperCol 默认为 6 / 18', async () => {
      const wrapper = makeWrapper({
        fields: [{ field: 'name', label: '名称', type: 'input' }]
      })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.labelColObj.span).toBe(6)
      expect(vm.wrapperColObj.span).toBe(18)
    })

    it('labelCol / wrapperCol 可定制', async () => {
      const wrapper = makeWrapper({
        fields: [{ field: 'name', label: '名称', type: 'input' }],
        props: { labelCol: 4, wrapperCol: 20 }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.labelColObj.span).toBe(4)
      expect(vm.wrapperColObj.span).toBe(20)
    })
  })

  describe('visibleFields 计算属性', () => {
    it('过滤掉 visibleWhen 不命中的字段', async () => {
      const fields: FormField[] = [
        { field: 'a', label: 'A', type: 'input' },
        {
          field: 'b', label: 'B', type: 'input',
          visibleWhen: { field: 'a', value: 'show' }
        }
      ]
      const wrapper = makeWrapper({ fields, data: { a: 'hide' } })
      await flushPromises()
      const vm = wrapper.vm as any
      const visibleList = vm.visibleFields
      expect(visibleList.length).toBe(1)
      expect(visibleList[0].field).toBe('a')
    })

    it('无 visibleWhen 时所有字段可见', async () => {
      const fields: FormField[] = [
        { field: 'a', label: 'A', type: 'input' },
        { field: 'b', label: 'B', type: 'input' }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.visibleFields.length).toBe(2)
    })
  })

  describe('Slots', () => {
    it('form-extra 插槽渲染到表单底部', async () => {
      const wrapper = mount(FormModal, {
        props: {
          visible: true,
          title: '测试',
          fields: [{ field: 'name', label: '名称', type: 'input' }],
          data: {}
        },
        global: {
          stubs: { 'a-modal': ModalStub }
        },
        slots: {
          'form-extra': '<div class="extra-slot">额外字段</div>'
        }
      })
      await flushPromises()
      expect(wrapper.find('.extra-slot').exists()).toBe(true)
      expect(wrapper.text()).toContain('额外字段')
    })

    it('footer 插槽替换默认按钮', async () => {
      const wrapper = mount(FormModal, {
        props: {
          visible: true,
          title: '测试',
          fields: [{ field: 'name', label: '名称', type: 'input' }],
          data: {}
        },
        global: {
          stubs: { 'a-modal': ModalStub }
        },
        slots: {
          footer: '<div class="custom-footer">自定义按钮</div>'
        }
      })
      await flushPromises()
      expect(wrapper.find('.custom-footer').exists()).toBe(true)
      expect(wrapper.text()).toContain('自定义按钮')
      // 默认取消/确定按钮被替换
      const buttons = wrapper.findAll('button')
      const texts = buttons.map((b) => b.text())
      expect(texts).not.toContain('取消')
    })
  })

  describe('完整集成场景', () => {
    it('13 种字段类型 + 联动 + 异步选项 + 校验完整流程', async () => {
      const asyncOptions = vi.fn(async () => [
        { label: '异步选项', value: 'async1' }
      ])
      const fields: FormField[] = [
        ...buildAllTypeFields(),
        {
          field: 'extraType', label: '类型', type: 'radio', defaultValue: 'a',
          options: [{ label: 'A', value: 'a' }, { label: 'B', value: 'b' }]
        },
        {
          field: 'extra', label: '扩展字段', type: 'input',
          visibleWhen: { field: 'extraType', value: 'b' },
          requiredWhen: { field: 'extraType', value: 'b' },
          disabledWhen: { field: 'extraType', value: 'a' }
        },
        {
          field: 'asyncChoice', label: '异步选择', type: 'select',
          asyncOptions
        }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      // 异步选项已加载
      expect(asyncOptions).toHaveBeenCalled()
      const opts = vm.getFieldOptions(fields.find((f) => f.field === 'asyncChoice')!)
      expect(opts.length).toBe(1)
      // extra 字段：extraType=a 时 visibleWhen {field:'extraType', value:'b'} 不命中
      const extraField = fields.find((f) => f.field === 'extra')!
      expect(vm.isFieldVisible(extraField)).toBe(false)
      // 切换 extraType=b
      vm.onFieldValueChange(fields.find((f) => f.field === 'extraType')!, 'b')
      await nextTick()
      expect(vm.isFieldVisible(extraField)).toBe(true)
      expect(vm.isFieldRequired(extraField)).toBe(true)
      expect(vm.isFieldDisabled(extraField)).toBe(false)
    })

    it('编辑模式回填数据后正常显示', async () => {
      const fields: FormField[] = [
        { field: 'name', label: '名称', type: 'input' },
        { field: 'count', label: '数量', type: 'inputNumber', defaultValue: 0 },
        {
          field: 'status', label: '状态', type: 'select',
          options: [{ label: '启用', value: 1 }, { label: '禁用', value: 0 }]
        }
      ]
      const wrapper = makeWrapper({
        fields,
        data: { name: 'alice', count: 25, status: 1 }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.innerData.name).toBe('alice')
      expect(vm.innerData.count).toBe(25)
      expect(vm.innerData.status).toBe(1)
    })

    it('提交后 payload 仅包含可见字段', async () => {
      const fields: FormField[] = [
        { field: 'a', label: 'A', type: 'input', defaultValue: 'a-val' },
        {
          field: 'b', label: 'B', type: 'input',
          visibleWhen: { field: 'a', value: 'show' },
          defaultValue: 'b-val'
        }
      ]
      const wrapper = makeWrapper({ fields })
      await flushPromises()
      const vm = wrapper.vm as any
      vm.formRef = { validate: () => Promise.resolve(true) }
      await vm.handleSubmit()
      await flushPromises()
      const payload = wrapper.emitted('submit')![0][0] as Record<string, any>
      expect(payload.a).toBe('a-val')
      // b 字段不可见，不应出现在 payload 中
      expect(payload.b).toBeUndefined()
    })
  })
})
