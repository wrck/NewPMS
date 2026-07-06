<script setup lang="ts">
/**
 * FormModal 通用表单弹窗组件（spec 阶段三 - Task 13）
 *
 * 一站式封装：
 *   1. 基于 fields 配置自动渲染 a-form + a-row + a-col 栅格表单
 *   2. 支持 13 种字段类型（input/inputNumber/inputPassword/textarea/select/date/dateRange/
 *      switch/radio/checkbox/cascader/upload/treeSelect）
 *   3. 字段联动：visibleWhen / requiredWhen / disabledWhen / optionsWhen 实时计算
 *   4. 异步选项加载：asyncOptions 在 onMounted 时调用
 *   5. 表单校验：基于 required + rules 自动生成，submit 时调用 validate
 *   6. v-model:visible / v-model:data 双向绑定
 *   7. 取消按钮重置表单，弹窗关闭清空数据
 *
 * 使用示例见 ./README.md
 */
import { ref, reactive, computed, watch, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import type { FormField, FormFieldOption } from './types'

/* ============ Props ============ */
interface FormModalProps {
  /** 是否显示（v-model:visible） */
  visible: boolean
  /** 弹窗标题 */
  title: string
  /** 字段定义 */
  fields: FormField[]
  /** 表单数据（v-model:data） */
  data: Record<string, any>
  /** 提交 loading 状态 */
  loading?: boolean
  /** 弹窗宽度 */
  width?: number | string
  /** 表单栅格跨度（默认 12，即 24 栅格中的 12 = 2 列） */
  span?: number
  /** label 标签宽度 */
  labelCol?: number
  /** wrapper 宽度 */
  wrapperCol?: number
}

const props = withDefaults(defineProps<FormModalProps>(), {
  loading: false,
  width: 600,
  span: 12,
  labelCol: 6,
  wrapperCol: 18
})

/* ============ Emits ============ */
interface FormModalEmits {
  (e: 'update:visible', visible: boolean): void
  (e: 'update:data', data: Record<string, any>): void
  /** 提交表单（已通过校验） */
  (e: 'submit', data: Record<string, any>): void
  /** 取消 */
  (e: 'cancel'): void
}

const emit = defineEmits<FormModalEmits>()

/* ============ 内部表单数据 ============ */
/**
 * 用 reactive 维护一份内部副本，避免直接修改 props.data。
 * 通过 watch 同步 props.data -> innerData，并通过 setFieldValue 同步回 props.data。
 */
const innerData = reactive<Record<string, any>>({})
const formRef = ref()

/** 异步选项缓存：field -> options[] */
const asyncOptionsMap = reactive<Record<string, FormFieldOption[]>>({})

/* ============ 工具函数 ============ */
function getNestedValue(obj: any, path: string): any {
  if (!path) return undefined
  if (!path.includes('.')) return obj?.[path]
  return path.split('.').reduce((acc, key) => (acc == null ? undefined : acc[key]), obj)
}

function setNestedValue(obj: any, path: string, value: any): void {
  if (!path.includes('.')) {
    obj[path] = value
    return
  }
  const keys = path.split('.')
  const last = keys.pop() as string
  const target = keys.reduce((acc, key) => {
    if (acc[key] == null) acc[key] = {}
    return acc[key]
  }, obj)
  target[last] = value
}

/** 同步 innerData -> emit update:data */
function syncToParent() {
  const snapshot: Record<string, any> = {}
  for (const f of props.fields) {
    setNestedValue(snapshot, f.field, getNestedValue(innerData, f.field))
  }
  // 保留父组件传入但未在 fields 中声明的额外字段
  for (const k of Object.keys(props.data || {})) {
    if (!(k in snapshot) && innerData[k] !== undefined) {
      snapshot[k] = innerData[k]
    }
  }
  emit('update:data', snapshot)
}

/* ============ 初始化表单数据 ============ */
function initInnerData() {
  // 清空旧字段
  Object.keys(innerData).forEach((k) => delete innerData[k])
  // 用 props.data 初始化（如果是嵌套字段，按路径写入）
  for (const f of props.fields) {
    const incoming = getNestedValue(props.data, f.field)
    const val = incoming !== undefined ? incoming : f.defaultValue
    setNestedValue(innerData, f.field, val)
  }
  // 同步额外的字段（不在 fields 中但在 data 中）
  for (const k of Object.keys(props.data || {})) {
    if (getNestedValue(innerData, k) === undefined && props.data[k] !== undefined) {
      innerData[k] = props.data[k]
    }
  }
}

/* ============ 字段联动 ============ */
function matchCondition(condition: { field: string; value: any | any[] } | undefined): boolean {
  if (!condition) return false
  const cur = getNestedValue(innerData, condition.field)
  const expected = condition.value
  if (Array.isArray(expected)) {
    return expected.includes(cur)
  }
  return cur === expected
}

/** 字段是否可见（visibleWhen 命中时显示） */
function isFieldVisible(field: FormField): boolean {
  if (!field.visibleWhen) return true
  return matchCondition(field.visibleWhen)
}

/** 字段是否必填（required || requiredWhen 命中） */
function isFieldRequired(field: FormField): boolean {
  if (field.required) return true
  if (field.requiredWhen && matchCondition(field.requiredWhen)) return true
  return false
}

/** 字段是否禁用（disabled || disabledWhen 命中） */
function isFieldDisabled(field: FormField): boolean {
  if (field.disabled) return true
  if (field.disabledWhen && matchCondition(field.disabledWhen)) return true
  return false
}

/** 获取字段的选项（asyncOptions 优先 > optionsWhen 命中 > options） */
function getFieldOptions(field: FormField): FormFieldOption[] {
  // 异步选项优先
  const asyncOpts = asyncOptionsMap[field.field]
  if (asyncOpts && asyncOpts.length > 0) return asyncOpts
  // optionsWhen 命中
  if (field.optionsWhen && matchCondition(field.optionsWhen)) {
    return field.optionsWhen.options
  }
  // 默认 options / treeData
  return field.options || field.treeData || []
}

/* ============ 校验规则生成 ============ */
/** 基于 required + rules 自动生成 a-form-item rules */
function getFieldRules(field: FormField): any[] {
  const rules: any[] = []
  if (isFieldRequired(field)) {
    const isSelectLike = [
      'select',
      'date',
      'dateRange',
      'cascader',
      'treeSelect',
      'upload'
    ].includes(field.type)
    const msg = isSelectLike
      ? `请选择${field.label}`
      : `请输入${field.label}`
    rules.push({ required: true, message: msg, trigger: ['blur', 'change'] })
  }
  if (field.rules && field.rules.length) {
    rules.push(...field.rules)
  }
  return rules
}

/* ============ 字段值变化处理 ============ */
function onFieldValueChange(field: FormField, value: any) {
  setNestedValue(innerData, field.field, value)
  syncToParent()
}

/* ============ 取消 / 关闭 ============ */
function handleCancel() {
  emit('cancel')
  closeVisible()
}

function closeVisible() {
  emit('update:visible', false)
}

/** 弹窗关闭后重置表单数据与校验状态 */
async function handleClosed() {
  // 清空校验状态
  if (formRef.value) {
    formRef.value.resetFields?.()
  }
  // 清空 innerData
  Object.keys(innerData).forEach((k) => delete innerData[k])
  // 清空 asyncOptions 缓存
  Object.keys(asyncOptionsMap).forEach((k) => delete asyncOptionsMap[k])
}

/* ============ 提交 ============ */
async function handleSubmit() {
  try {
    if (formRef.value) {
      await formRef.value.validate()
    }
  } catch (e) {
    message.warning('请完善表单信息')
    return
  }
  // 通过校验，组装 payload
  const payload: Record<string, any> = {}
  for (const f of props.fields) {
    if (!isFieldVisible(f)) continue
    setNestedValue(payload, f.field, getNestedValue(innerData, f.field))
  }
  // 保留额外字段
  for (const k of Object.keys(props.data || {})) {
    if (getNestedValue(payload, k) === undefined && props.data[k] !== undefined) {
      payload[k] = props.data[k]
    }
  }
  emit('submit', payload)
}

/* ============ Expose 方法 ============ */
async function validate(): Promise<any> {
  if (formRef.value) {
    return await formRef.value.validate()
  }
  return true
}

function resetFields(): void {
  if (formRef.value) {
    formRef.value.resetFields?.()
  }
  // 重新初始化为默认值
  initInnerData()
}

function setFieldValue(field: string, value: any): void {
  setNestedValue(innerData, field, value)
  syncToParent()
}

function getFormData(): Record<string, any> {
  const result: Record<string, any> = {}
  for (const f of props.fields) {
    setNestedValue(result, f.field, getNestedValue(innerData, f.field))
  }
  return JSON.parse(JSON.stringify(result))
}

defineExpose({
  validate,
  resetFields,
  setFieldValue,
  getFormData
})

/* ============ 异步选项加载 ============ */
async function loadAsyncOptions() {
  for (const f of props.fields) {
    if (f.asyncOptions) {
      try {
        const opts = await f.asyncOptions()
        asyncOptionsMap[f.field] = opts
      } catch (e) {
        console.error(`[FormModal] asyncOptions load failed for field "${f.field}":`, e)
        asyncOptionsMap[f.field] = []
      }
    }
  }
}

/* ============ 监听 visible 初始化 ============ */
watch(
  () => props.visible,
  async (val) => {
    if (val) {
      initInnerData()
      await nextTick()
      if (formRef.value) {
        formRef.value.clearValidate?.()
      }
    }
  },
  { immediate: false }
)

/* ============ 监听 fields / data 变化 ============ */
watch(
  () => props.data,
  () => {
    // 仅同步缺失的字段，避免覆盖用户输入
    for (const f of props.fields) {
      const incoming = getNestedValue(props.data, f.field)
      if (incoming !== undefined && getNestedValue(innerData, f.field) === undefined) {
        setNestedValue(innerData, f.field, incoming)
      }
    }
  },
  { deep: true }
)

/* ============ 初始化 ============ */
onMounted(() => {
  // 仅在 visible=true 时初始化表单数据；visible=false 时由 watch 监听后初始化
  if (props.visible) {
    initInnerData()
  }
  loadAsyncOptions()
})

/* ============ 模板辅助 ============ */
const labelColObj = computed(() => ({ span: props.labelCol }))
const wrapperColObj = computed(() => ({ span: props.wrapperCol }))

/** 可见字段列表 */
const visibleFields = computed(() => props.fields.filter((f) => isFieldVisible(f)))

/** 透传给 a-input-number 的属性 */
function getInputNumberProps(field: FormField) {
  return {
    max: field.max,
    min: field.min,
    precision: field.precision
  }
}

/** upload 文件大小校验：返回 false 表示拒绝 */
function beforeUpload(field: FormField, file: File): boolean {
  if (field.maxSize) {
    const isLt = file.size / 1024 / 1024 < field.maxSize
    if (!isLt) {
      message.error(`文件大小不能超过 ${field.maxSize}MB`)
      return false
    }
  }
  if (field.accept) {
    const acceptList = field.accept.split(',').map((s) => s.trim().toLowerCase())
    const fileName = file.name.toLowerCase()
    const matched = acceptList.some((ext) => fileName.endsWith(ext))
    if (!matched) {
      message.error(`仅支持以下文件类型：${field.accept}`)
      return false
    }
  }
  return true
}
</script>

<template>
  <a-modal
    :open="visible"
    :title="title"
    :width="width"
    :confirm-loading="loading"
    :mask-closable="false"
    :destroy-on-close="true"
    @ok="handleSubmit"
    @cancel="handleCancel"
    @after-close="handleClosed"
  >
    <a-form
      ref="formRef"
      :model="innerData"
      :label-col="labelColObj"
      :wrapper-col="wrapperColObj"
    >
      <a-row :gutter="16">
        <a-col
          v-for="field in visibleFields"
          :key="field.field"
          :span="field.span || span"
        >
          <a-form-item
            :label="field.label"
            :name="field.field"
            :rules="getFieldRules(field)"
          >
            <!-- input -->
            <a-input
              v-if="field.type === 'input'"
              :value="getNestedValue(innerData, field.field)"
              :placeholder="field.placeholder || `请输入${field.label}`"
              :readonly="field.readonly"
              :disabled="isFieldDisabled(field)"
              :maxlength="field.maxLength"
              allow-clear
              @update:value="(v: any) => onFieldValueChange(field, v)"
            />
            <!-- inputNumber -->
            <a-input-number
              v-else-if="field.type === 'inputNumber'"
              :value="getNestedValue(innerData, field.field)"
              :placeholder="field.placeholder || `请输入${field.label}`"
              :disabled="isFieldDisabled(field)"
              v-bind="getInputNumberProps(field)"
              style="width: 100%"
              @update:value="(v: any) => onFieldValueChange(field, v)"
            />
            <!-- inputPassword -->
            <a-input-password
              v-else-if="field.type === 'inputPassword'"
              :value="getNestedValue(innerData, field.field)"
              :placeholder="field.placeholder || `请输入${field.label}`"
              :disabled="isFieldDisabled(field)"
              :maxlength="field.maxLength"
              @update:value="(v: any) => onFieldValueChange(field, v)"
            />
            <!-- textarea -->
            <a-textarea
              v-else-if="field.type === 'textarea'"
              :value="getNestedValue(innerData, field.field)"
              :placeholder="field.placeholder || `请输入${field.label}`"
              :disabled="isFieldDisabled(field)"
              :maxlength="field.maxLength"
              :rows="3"
              allow-clear
              @update:value="(v: any) => onFieldValueChange(field, v)"
            />
            <!-- select -->
            <a-select
              v-else-if="field.type === 'select'"
              :value="getNestedValue(innerData, field.field)"
              :placeholder="field.placeholder || `请选择${field.label}`"
              :options="getFieldOptions(field)"
              :disabled="isFieldDisabled(field)"
              :mode="field.multiple ? 'multiple' : undefined"
              allow-clear
              @update:value="(v: any) => onFieldValueChange(field, v)"
            />
            <!-- date -->
            <a-date-picker
              v-else-if="field.type === 'date'"
              :value="getNestedValue(innerData, field.field)"
              :placeholder="field.placeholder || `请选择${field.label}`"
              :disabled="isFieldDisabled(field)"
              :show-time="field.showTime"
              :value-format="field.valueFormat"
              style="width: 100%"
              @update:value="(v: any) => onFieldValueChange(field, v)"
            />
            <!-- dateRange -->
            <a-range-picker
              v-else-if="field.type === 'dateRange'"
              :value="getNestedValue(innerData, field.field)"
              :disabled="isFieldDisabled(field)"
              :show-time="field.showTime"
              :value-format="field.valueFormat"
              style="width: 100%"
              @update:value="(v: any) => onFieldValueChange(field, v)"
            />
            <!-- switch -->
            <a-switch
              v-else-if="field.type === 'switch'"
              :checked="!!getNestedValue(innerData, field.field)"
              :disabled="isFieldDisabled(field)"
              @update:checked="(v: any) => onFieldValueChange(field, v)"
            />
            <!-- radio -->
            <a-radio-group
              v-else-if="field.type === 'radio'"
              :value="getNestedValue(innerData, field.field)"
              :disabled="isFieldDisabled(field)"
              @update:value="(v: any) => onFieldValueChange(field, v)"
            >
              <a-radio
                v-for="opt in getFieldOptions(field)"
                :key="opt.value"
                :value="opt.value"
                :disabled="opt.disabled"
              >
                {{ opt.label }}
              </a-radio>
            </a-radio-group>
            <!-- checkbox -->
            <a-checkbox-group
              v-else-if="field.type === 'checkbox'"
              :value="getNestedValue(innerData, field.field)"
              :disabled="isFieldDisabled(field)"
              @update:value="(v: any) => onFieldValueChange(field, v)"
            >
              <a-checkbox
                v-for="opt in getFieldOptions(field)"
                :key="opt.value"
                :value="opt.value"
                :disabled="opt.disabled"
              >
                {{ opt.label }}
              </a-checkbox>
            </a-checkbox-group>
            <!-- cascader -->
            <a-cascader
              v-else-if="field.type === 'cascader'"
              :value="getNestedValue(innerData, field.field)"
              :options="getFieldOptions(field)"
              :placeholder="field.placeholder || `请选择${field.label}`"
              :disabled="isFieldDisabled(field)"
              :multiple="field.multiple"
              change-on-select
              @update:value="(v: any) => onFieldValueChange(field, v)"
            />
            <!-- treeSelect -->
            <a-tree-select
              v-else-if="field.type === 'treeSelect'"
              :value="getNestedValue(innerData, field.field)"
              :tree-data="getFieldOptions(field)"
              :placeholder="field.placeholder || `请选择${field.label}`"
              :disabled="isFieldDisabled(field)"
              :multiple="field.multiple"
              tree-default-expand-all
              allow-clear
              @update:value="(v: any) => onFieldValueChange(field, v)"
            />
            <!-- upload -->
            <a-upload-dragger
              v-else-if="field.type === 'upload'"
              :file-list="getNestedValue(innerData, field.field) || []"
              :accept="field.accept"
              :max-count="field.maxCount"
              :multiple="field.maxCount ? field.maxCount > 1 : true"
              :before-upload="(file: File) => beforeUpload(field, file)"
              @update:file-list="(v: any) => onFieldValueChange(field, v)"
            >
              <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
              <p v-if="field.maxSize || field.accept" class="ant-upload-hint">
                <template v-if="field.maxSize">单个文件不超过 {{ field.maxSize }}MB；</template>
                <template v-if="field.accept">支持类型：{{ field.accept }}</template>
              </p>
            </a-upload-dragger>
          </a-form-item>
        </a-col>
      </a-row>
      <!-- 自定义表单插槽 -->
      <slot name="form-extra" :data="innerData" />
    </a-form>
    <template #footer>
      <slot name="footer">
        <a-button @click="handleCancel">取消</a-button>
        <a-button type="primary" :loading="loading" @click="handleSubmit">确定</a-button>
      </slot>
    </template>
  </a-modal>
</template>

<style lang="less" scoped>
.ant-upload-hint {
  color: @text-tertiary;
  font-size: 12px;
}
</style>
