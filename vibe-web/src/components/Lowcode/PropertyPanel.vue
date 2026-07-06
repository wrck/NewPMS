<script setup lang="ts">
/**
 * PropertyPanel 低代码字段属性配置面板（spec 阶段三 - Task A2.3）
 *
 * 编辑选中字段：label / required / defaultValue / rules / width / placeholder
 * 以及 type 特定属性：options（select/cascader）、relBizType（relSelect）等。
 */
import { computed, watch, reactive } from 'vue'
import { Form, Input, InputNumber, Switch, Select, Button, Textarea, Divider, Empty } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import type { FieldConfig, FieldOption, FieldType, FieldRule } from '@/types/lowcode'

interface Props {
  /** 当前选中字段（v-model） */
  field: FieldConfig | null
  /** 是否禁用 */
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false
})

const emit = defineEmits<{
  (e: 'update:field', field: FieldConfig): void
  (e: 'delete'): void
}>()

/** 内部副本，避免直接修改 props.field */
const inner = reactive<FieldConfig & { rules: FieldRule[]; options: FieldOption[] }>({
  field: '',
  label: '',
  type: 'input',
  required: false,
  defaultValue: undefined,
  placeholder: '',
  width: 12,
  readonly: false,
  description: '',
  relBizType: '',
  relLabelField: '',
  relValueField: '',
  rules: [],
  options: []
})

/** 是否选项类（select / cascader） */
const isOptionsType = computed(() => ['select', 'cascader'].includes(inner.type))
/** 是否关联选择 */
const isRelType = computed(() => inner.type === 'relSelect')
/** 是否开关（默认值类型为布尔） */
const isSwitch = computed(() => inner.type === 'switch')
/** 是否数字（默认值类型为数字） */
const isNumber = computed(() => inner.type === 'number')

/** 同步 props.field -> inner */
function syncFromProps() {
  if (!props.field) {
    return
  }
  inner.field = props.field.field || ''
  inner.label = props.field.label || ''
  inner.type = props.field.type
  inner.dataType = props.field.dataType
  inner.required = !!props.field.required
  inner.defaultValue = props.field.defaultValue
  inner.placeholder = props.field.placeholder || ''
  inner.width = props.field.width ?? 12
  inner.readonly = !!props.field.readonly
  inner.description = props.field.description || ''
  inner.relBizType = props.field.relBizType || ''
  inner.relLabelField = props.field.relLabelField || ''
  inner.relValueField = props.field.relValueField || ''
  inner.rules = Array.isArray(props.field.rules) ? [...props.field.rules] : []
  inner.options = Array.isArray(props.field.options) ? props.field.options.map((o) => ({ ...o })) : []
  inner.hideInForm = props.field.hideInForm
  inner.hideInTable = props.field.hideInTable
}

watch(
  () => props.field,
  () => syncFromProps(),
  { immediate: true, deep: true }
)

/** 同步 inner -> 父组件 */
function emitUpdate() {
  const payload: FieldConfig = {
    field: inner.field,
    label: inner.label,
    type: inner.type as FieldType,
    required: inner.required,
    defaultValue: inner.defaultValue,
    placeholder: inner.placeholder,
    width: inner.width,
    readonly: inner.readonly,
    description: inner.description,
    rules: inner.rules.length ? [...inner.rules] : undefined,
    options: isOptionsType.value && inner.options.length ? inner.options.map((o) => ({ ...o })) : undefined,
    relBizType: isRelType.value ? inner.relBizType : undefined,
    relLabelField: isRelType.value ? inner.relLabelField : undefined,
    relValueField: isRelType.value ? inner.relValueField : undefined,
    hideInForm: inner.hideInForm,
    hideInTable: inner.hideInTable,
    dataType: inner.dataType
  }
  emit('update:field', payload)
}

function handleField() {
  emitUpdate()
}

function handleDelete() {
  emit('delete')
}

/** 新增选项 */
function addOption() {
  const idx = inner.options.length + 1
  inner.options.push({ label: `选项${idx}`, value: idx })
  emitUpdate()
}

/** 删除选项 */
function removeOption(idx: number) {
  inner.options.splice(idx, 1)
  emitUpdate()
}

/** 新增校验规则 */
function addRule() {
  inner.rules.push({ required: false, message: '', trigger: 'blur' })
  emitUpdate()
}

/** 删除校验规则 */
function removeRule(idx: number) {
  inner.rules.splice(idx, 1)
  emitUpdate()
}
</script>

<template>
  <div class="property-panel">
    <div class="panel-header">
      <span class="title">属性配置</span>
      <a-button v-if="field" danger size="small" type="link" @click="handleDelete">
        <template #icon><DeleteOutlined /></template>
        删除字段
      </a-button>
    </div>

    <div class="panel-body">
      <Empty v-if="!field" description="请选择画布中的字段" />

      <Form v-else layout="vertical" :model="inner" :disabled="disabled">
        <Divider orientation="left" plain>基础</Divider>

        <Form.Item label="字段标识 (field)" required>
          <Input
            v-model:value="inner.field"
            placeholder="数据键名，如 customerName"
            allow-clear
            @change="handleField"
          />
        </Form.Item>

        <Form.Item label="显示标签 (label)" required>
          <Input
            v-model:value="inner.label"
            placeholder="如 客户名称"
            allow-clear
            @change="handleField"
          />
        </Form.Item>

        <Form.Item label="组件类型">
          <Select
            v-model:value="inner.type"
            :options="[
              { value: 'input', label: '单行输入' },
              { value: 'textarea', label: '多行输入' },
              { value: 'number', label: '数字' },
              { value: 'select', label: '下拉选择' },
              { value: 'date', label: '日期' },
              { value: 'switch', label: '开关' },
              { value: 'cascader', label: '级联选择' },
              { value: 'richText', label: '富文本' },
              { value: 'file', label: '文件上传' },
              { value: 'relSelect', label: '关联选择' }
            ]"
            @change="handleField"
          />
        </Form.Item>

        <Form.Item label="占位提示">
          <Input
            v-model:value="inner.placeholder"
            placeholder="占位文本"
            allow-clear
            @change="handleField"
          />
        </Form.Item>

        <Form.Item label="描述/帮助">
          <Textarea
            v-model:value="inner.description"
            :rows="2"
            placeholder="字段说明"
            @change="handleField"
          />
        </Form.Item>

        <Divider orientation="left" plain>校验</Divider>

        <Form.Item label="是否必填">
          <Switch v-model:checked="inner.required" @change="handleField" />
        </Form.Item>

        <Form.Item label="只读">
          <Switch v-model:checked="inner.readonly" @change="handleField" />
        </Form.Item>

        <Form.Item label="栅格宽度 (1-24)">
          <InputNumber
            v-model:value="inner.width"
            :min="1"
            :max="24"
            style="width: 100%"
            @change="handleField"
          />
        </Form.Item>

        <Form.Item label="默认值">
          <Input
            v-if="!isSwitch && !isNumber"
            :value="inner.defaultValue as string | undefined"
            placeholder="默认值"
            @update:value="(v) => (inner.defaultValue = v)"
            @change="handleField"
          />
          <InputNumber
            v-else-if="isNumber"
            :value="inner.defaultValue as number | undefined"
            style="width: 100%"
            @update:value="(v) => (inner.defaultValue = v)"
            @change="handleField"
          />
          <Switch
            v-else-if="isSwitch"
            :checked="!!inner.defaultValue"
            @update:checked="(v) => (inner.defaultValue = v)"
            @change="handleField"
          />
        </Form.Item>

        <!-- 选项类字段：选项编辑 -->
        <template v-if="isOptionsType">
          <Divider orientation="left" plain>选项</Divider>
          <div class="options-list">
            <div v-for="(opt, idx) in inner.options" :key="idx" class="option-row">
              <Input
                v-model:value="opt.label"
                placeholder="显示文本"
                style="flex: 1"
                @change="handleField"
              />
              <Input
                v-model:value="opt.value"
                placeholder="值"
                style="width: 100px; margin-left: 8px"
                @change="handleField"
              />
              <Button type="link" danger size="small" @click="removeOption(idx)">
                <template #icon><DeleteOutlined /></template>
              </Button>
            </div>
          </div>
          <Button type="dashed" block @click="addOption">
            <template #icon><PlusOutlined /></template>
            新增选项
          </Button>
        </template>

        <!-- 关联选择：实体配置 -->
        <template v-if="isRelType">
          <Divider orientation="left" plain>关联</Divider>
          <Form.Item label="关联实体编码 (bizType)">
            <Input
              v-model:value="inner.relBizType"
              placeholder="如 customer"
              @change="handleField"
            />
          </Form.Item>
          <Form.Item label="关联值字段">
            <Input
              v-model:value="inner.relValueField"
              placeholder="如 id"
              @change="handleField"
            />
          </Form.Item>
          <Form.Item label="关联显示字段">
            <Input
              v-model:value="inner.relLabelField"
              placeholder="如 customerName"
              @change="handleField"
            />
          </Form.Item>
        </template>

        <!-- 校验规则 -->
        <Divider orientation="left" plain>校验规则</Divider>
        <div class="rules-list">
          <div v-for="(rule, idx) in inner.rules" :key="idx" class="rule-row">
            <Select
              v-model:value="rule.trigger"
              :options="[
                { value: 'blur', label: 'blur' },
                { value: 'change', label: 'change' }
              ]"
              style="width: 100px"
              @change="handleField"
            />
            <Input
              v-model:value="rule.message"
              placeholder="错误提示"
              style="flex: 1; margin-left: 8px"
              @change="handleField"
            />
            <Button type="link" danger size="small" @click="removeRule(idx)">
              <template #icon><DeleteOutlined /></template>
            </Button>
          </div>
        </div>
        <Button type="dashed" block @click="addRule">
          <template #icon><PlusOutlined /></template>
          新增规则
        </Button>

        <Divider orientation="left" plain>可见性</Divider>
        <Form.Item label="表单中隐藏">
          <Switch v-model:checked="inner.hideInForm" @change="handleField" />
        </Form.Item>
        <Form.Item label="列表中隐藏">
          <Switch v-model:checked="inner.hideInTable" @change="handleField" />
        </Form.Item>
      </Form>
    </div>
  </div>
</template>

<style lang="less" scoped>
.property-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: @bg-container;
  border-left: 1px solid @border-split;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid @border-split;
}
.panel-header .title {
  font-weight: 600;
  color: @text-primary;
}
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}
.option-row,
.rule-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}
.options-list,
.rules-list {
  margin-bottom: 8px;
}
</style>
