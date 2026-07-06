/**
 * FormModal 通用表单弹窗组件类型定义
 * 对应 spec：阶段三 前端通用组件抽取 - Task 13
 *
 * 设计目标：与 CrudTable 解耦，独立可复用，支持：
 *   - 13 种字段类型（input/inputNumber/inputPassword/textarea/select/date/dateRange/
 *     switch/radio/checkbox/cascader/upload/treeSelect）
 *   - 字段联动（visibleWhen / requiredWhen / disabledWhen / optionsWhen）
 *   - 异步选项加载（asyncOptions）
 *   - 表单校验（required + rules 自动生成）
 *   - v-model:visible / v-model:data 双向绑定
 */

/** 字段控件类型（共 13 种） */
export type FormFieldType =
  | 'input'
  | 'inputNumber'
  | 'inputPassword'
  | 'textarea'
  | 'select'
  | 'date'
  | 'dateRange'
  | 'switch'
  | 'radio'
  | 'checkbox'
  | 'cascader'
  | 'upload'
  | 'treeSelect'

/** 字段联动条件：当指定字段等于某值时触发（值可为单个或数组） */
export interface FormFieldCondition {
  /** 触发字段名 */
  field: string
  /** 触发值（单值或数组，命中其中任意一个即视为满足） */
  value: any | any[]
}

/** 当指定字段等于某值时切换选项 */
export interface FormFieldOptionsWhen extends FormFieldCondition {
  /** 命中条件时使用的选项 */
  options: Array<{ label: string; value: any; disabled?: boolean }>
}

/** 字段选项 */
export interface FormFieldOption {
  label: string
  value: any
  disabled?: boolean
  /** treeSelect / cascader 子节点 */
  children?: FormFieldOption[]
  /** treeSelect 是否可选（叶子节点） */
  selectable?: boolean
}

/**
 * 表单字段定义
 */
export interface FormField {
  /** 字段名（对应 data 上的属性，支持 a.b 嵌套） */
  field: string
  /** 字段标签 */
  label: string
  /** 控件类型 */
  type: FormFieldType
  /** 选项（select/radio/checkbox/cascader/treeSelect 使用） */
  options?: FormFieldOption[]
  /** 占位提示 */
  placeholder?: string
  /** 默认值 */
  defaultValue?: any
  /** 是否必填（true 时自动追加 required 校验规则） */
  required?: boolean
  /** 自定义校验规则（追加在 required 之后） */
  rules?: any[]
  /** 字段栅格跨度（a-col span，覆盖组件级 span 默认值） */
  span?: number
  /** 是否只读（绑定 :readonly） */
  readonly?: boolean
  /** 是否禁用（绑定 :disabled） */
  disabled?: boolean
  /** 日期是否显示时间（date / dateRange 使用） */
  showTime?: boolean
  /** 日期值格式（date / dateRange 使用，如 'YYYY-MM-DD HH:mm:ss'） */
  valueFormat?: string
  /** 是否多选（select / cascader / treeSelect 使用） */
  multiple?: boolean
  /** treeSelect 树数据（与 options 二选一，优先 treeData） */
  treeData?: any[]
  /** inputNumber 最大值 */
  max?: number
  /** inputNumber 最小值 */
  min?: number
  /** inputNumber 精度 */
  precision?: number
  /** input / textarea 最大长度 */
  maxLength?: number
  /** upload 文件类型限制（如 '.jpg,.png'） */
  accept?: string
  /** upload 单文件大小限制（MB） */
  maxSize?: number
  /** upload 最大文件数 */
  maxCount?: number

  /* ============ 字段联动规则 ============ */
  /** 当指定字段等于某值时显示该字段 */
  visibleWhen?: FormFieldCondition
  /** 当指定字段等于某值时该字段必填 */
  requiredWhen?: FormFieldCondition
  /** 当指定字段等于某值时该字段禁用 */
  disabledWhen?: FormFieldCondition
  /** 当指定字段等于某值时切换选项 */
  optionsWhen?: FormFieldOptionsWhen

  /* ============ 异步选项加载 ============ */
  /** 异步加载选项（onMounted 时调用，结果回填到 options） */
  asyncOptions?: () => Promise<Array<{ label: string; value: any }>>
}

/**
 * FormModal 暴露给外部的实例方法
 */
export interface FormModalExpose {
  /** 手动触发整表校验（成功 resolve(true)，失败 reject(error)） */
  validate: () => Promise<any>
  /** 重置表单到初始状态（清空校验状态） */
  resetFields: () => void
  /** 设置指定字段的值 */
  setFieldValue: (field: string, value: any) => void
  /** 获取当前表单数据（深拷贝） */
  getFormData: () => Record<string, any>
}
