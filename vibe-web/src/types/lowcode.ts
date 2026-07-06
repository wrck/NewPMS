/**
 * 低代码模块类型定义
 *
 * 对齐后端 `module-lowcode`：
 *   - FormConfigController -> /api/v1/lowcode/forms
 *   - ListConfigController -> /api/v1/lowcode/lists
 *   - TabConfigController  -> /api/v1/lowcode/tabs
 *   - RelationConfigController -> /api/v1/lowcode/relations
 *   - TemplateController  -> /api/v1/lowcode/templates
 *
 * Schema 设计原则：
 *   - schemaJson 字段在传输层为字符串（后端 LONGTEXT），前端在加载后解析为强类型对象。
 *   - FormSchema / ListSchema / TabSchema / RelationSchema 为前端约定的结构化 Schema。
 *     后端只校验其是否为合法 JSON Schema Draft 7（语法层面），具体字段语义由前端解析。
 */

/* ============ 通用枚举 ============ */

/** 模板类型（与后端 LowcodeConstant 对齐） */
export type LowcodeTemplateType = 'FORM' | 'LIST' | 'TAB' | 'RELATION'

/** 配置状态：1-启用 / 0-禁用 */
export type LowcodeStatus = 1 | 0

/** 字段组件类型（设计器字段库） */
export type FieldType =
  | 'input'
  | 'textarea'
  | 'number'
  | 'select'
  | 'date'
  | 'switch'
  | 'cascader'
  | 'richText'
  | 'file'
  | 'relSelect'

/** 数据类型（JSON Schema type 字段） */
export type DataType = 'string' | 'number' | 'integer' | 'boolean' | 'object' | 'array' | 'null'

/** 列对齐方式 */
export type ColumnAlign = 'left' | 'center' | 'right'

/** 操作按钮类型 */
export type ActionType = 'create' | 'edit' | 'delete' | 'view' | 'custom'

/* ============ 字段配置 ============ */

/** 字段校验规则（Ant Design Vue Form 规则） */
export interface FieldRule {
  required?: boolean
  message?: string
  trigger?: 'blur' | 'change' | ('blur' | 'change')[]
  type?: 'string' | 'number' | 'boolean' | 'email' | 'url' | 'date'
  min?: number
  max?: number
  pattern?: string
  /** 自定义校验器名称（前端约定） */
  validator?: string
}

/** 下拉/级联选项 */
export interface FieldOption {
  label: string
  value: string | number
  disabled?: boolean
  /** 级联子节点 */
  children?: FieldOption[]
}

/**
 * 表单字段配置（设计器画布的最小单元）
 *
 * 对应 FormSchema.properties.<fieldName> 的结构。
 */
export interface FieldConfig {
  /** 字段名（对应数据键） */
  field: string
  /** 显示标签 */
  label: string
  /** 组件类型 */
  type: FieldType
  /** 数据类型（JSON Schema type） */
  dataType?: DataType
  /** 是否必填 */
  required?: boolean
  /** 默认值（基础类型） */
  defaultValue?: unknown
  /** 占位提示 */
  placeholder?: string
  /** 表单宽度（栅格 1-24，默认 12 = 2 列） */
  width?: number
  /** 校验规则 */
  rules?: FieldRule[]
  /** 选项（select / cascader / relSelect 用） */
  options?: FieldOption[]
  /** 关联实体（relSelect 用，如 customer） */
  relBizType?: string
  /** 关联显示字段 */
  relLabelField?: string
  /** 关联值字段 */
  relValueField?: string
  /** 是否在表单中只读 */
  readonly?: boolean
  /** 是否在列表中显示（仅 ListSchema 中有效） */
  hideInTable?: boolean
  /** 是否在表单中隐藏 */
  hideInForm?: boolean
  /** 描述/帮助文本 */
  description?: string
  /** 字段所属分组 */
  group?: string
  /** 排序（在画布中的顺序，从 0 起） */
  order?: number
}

/* ============ Form Schema ============ */

/**
 * 表单 Schema
 *
 * 用法：表单配置 (lowcode_form_config.schema_json) 经 JSON.parse 后得到此对象。
 * 设计器产出的 JSON 会通过 JSON.stringify 写回 schemaJson。
 */
export interface FormSchema {
  /** JSON Schema $schema 标识（兼容后端 Draft 7 校验） */
  $schema?: string
  /** Schema 类型固定为 object */
  type: 'object'
  /** 表单标题 */
  title?: string
  /** 表单描述 */
  description?: string
  /** 表单布局：水平 horizontal / 垂直 vertical / 内联 inline */
  layout?: 'horizontal' | 'vertical' | 'inline'
  /** 标签列宽（栅格，默认 6） */
  labelCol?: number
  /** 控件列宽（栅格，默认 18） */
  wrapperCol?: number
  /** 字段定义（key 为字段名） */
  properties: Record<string, FieldConfig>
  /** 表单提交接口（运行时绑定） */
  apiUrl?: string
  /** 提交方法 */
  apiMethod?: 'GET' | 'POST' | 'PUT' | 'DELETE'
}

/* ============ List Schema ============ */

/** 列表列定义 */
export interface ListColumn {
  /** 字段名（对应数据键） */
  field: string
  /** 列标题 */
  title: string
  /** 列宽 */
  width?: number | string
  /** 对齐方式 */
  align?: ColumnAlign
  /** 是否省略号显示 */
  ellipsis?: boolean
  /** 是否固定列 */
  fixed?: 'left' | 'right'
  /** 是否支持排序 */
  sortable?: boolean
  /** 值映射（如 status -> 启用/禁用） */
  valueEnum?: Record<string, { text: string; status?: string }>
  /** 自定义渲染标识（前端约定） */
  render?: 'text' | 'tag' | 'date' | 'datetime' | 'image' | 'link'
  /** 日期格式（render 为 date/datetime 时生效） */
  format?: string
}

/** 列表搜索字段 */
export interface ListSearchField {
  /** 字段名 */
  field: string
  /** 标签 */
  label: string
  /** 组件类型 */
  type: FieldType
  /** 占位提示 */
  placeholder?: string
  /** 选项 */
  options?: FieldOption[]
  /** 默认值 */
  defaultValue?: unknown
  /** 宽度 */
  width?: number
}

/** 列表操作按钮 */
export interface ListAction {
  /** 按钮类型 */
  type: ActionType
  /** 按钮文本 */
  label: string
  /** 图标（@ant-design/icons-vue 组件名） */
  icon?: string
  /** 权限标识 */
  permission?: string
  /** 自定义回调名（前端约定） */
  callback?: string
  /** 是否危险操作 */
  danger?: boolean
}

/**
 * 列表 Schema
 *
 * 用于 lowcode_list_config.schema_json。
 */
export interface ListSchema {
  $schema?: string
  /** Schema 类型标识 */
  type: 'list'
  /** 列表标题 */
  title?: string
  /** 列表描述 */
  description?: string
  /** 列定义 */
  columns: ListColumn[]
  /** 搜索字段 */
  searchFields?: ListSearchField[]
  /** 操作按钮（顶部工具栏 + 行操作） */
  actions?: ListAction[]
  /** 数据源接口（运行时调用，相对 /api/v1） */
  apiUrl?: string
  /** 主键字段（默认 id） */
  rowKey?: string
  /** 默认每页条数 */
  pageSize?: number
  /** 表格滚动宽度 */
  scrollX?: number
  /** 表单字段（用于新增/编辑弹窗） */
  formFields?: FieldConfig[]
}

/* ============ Tab Schema ============ */

/** 标签页项 */
export interface TabItem {
  /** 唯一 key */
  key: string
  /** 显示名称 */
  label: string
  /** 内嵌类型：list / form / relation / 自定义组件名 */
  contentType: 'list' | 'form' | 'relation' | 'custom'
  /** 引用的配置编码（bizType -> configCode） */
  bizType?: string
  /** 关联配置 ID（可选） */
  configId?: number
  /** 是否禁用 */
  disabled?: boolean
  /** 是否强制渲染（不销毁） */
  forceRender?: boolean
  /** 排序 */
  order?: number
}

/**
 * 标签页 Schema
 *
 * 用于 lowcode_tab_config.schema_json。
 */
export interface TabSchema {
  $schema?: string
  type: 'tab'
  title?: string
  description?: string
  /** 标签页位置 */
  tabPosition?: 'top' | 'right' | 'bottom' | 'left'
  /** 标签页类型：line | card */
  type2?: 'line' | 'card'
  /** 标签项 */
  tabs: TabItem[]
}

/* ============ Relation Schema ============ */

/**
 * 关联页 Schema（主从关系）
 *
 * 用于 lowcode_relation_config.schema_json。
 * 典型场景：客户档案 - 联系人子表；项目 - 任务子表。
 */
export interface RelationSchema {
  $schema?: string
  type: 'relation'
  title?: string
  description?: string
  /** 主表配置 */
  master: {
    /** 业务实体编码 */
    bizType: string
    /** 显示名称 */
    label?: string
    /** 数据源接口 */
    apiUrl?: string
    /** 主键字段 */
    rowKey?: string
    /** 主表显示字段 */
    displayField?: string
    /** 主表列定义 */
    columns?: ListColumn[]
    /** 主表搜索字段 */
    searchFields?: ListSearchField[]
  }
  /** 从表配置（可多个） */
  details: Array<{
    /** 业务实体编码 */
    bizType: string
    /** 显示名称 */
    label?: string
    /** 数据源接口 */
    apiUrl?: string
    /** 主键字段 */
    rowKey?: string
    /** 外键字段（关联主表） */
    foreignKey: string
    /** 列定义 */
    columns?: ListColumn[]
    /** 表单字段 */
    formFields?: FieldConfig[]
    /** 是否默认展开 */
    defaultExpand?: boolean
  }>
}

/* ============ VO / DTO（与后端对齐） ============ */

/** 通用配置 VO 基础字段（form/list/tab/relation 共享） */
export interface LowcodeConfigVOBase {
  id: number
  configCode: string
  configName: string
  /** JSON Schema 字符串（未解析） */
  schemaJson: string
  /** 关联模板 ID */
  templateId?: number | null
  /** 版本号 */
  version: number
  status: LowcodeStatus
  description?: string
  creatorId?: number
  createTime?: string
  updateTime?: string
}

/** 表单配置 VO */
export interface LowcodeFormConfigVO extends LowcodeConfigVOBase {}

/** 列表配置 VO */
export interface LowcodeListConfigVO extends LowcodeConfigVOBase {}

/** 标签页配置 VO */
export interface LowcodeTabConfigVO extends LowcodeConfigVOBase {}

/** 关联页配置 VO */
export interface LowcodeRelationConfigVO extends LowcodeConfigVOBase {}

/** 模板 VO */
export interface LowcodeTemplateVO {
  id: number
  templateCode: string
  templateName: string
  /** 模板类型 FORM/LIST/TAB/RELATION */
  templateType: LowcodeTemplateType
  schemaJson: string
  description?: string
  /** 被使用次数 */
  usageCount: number
  status: LowcodeStatus
  creatorId?: number
  createTime?: string
  updateTime?: string
}

/* ============ DTO ============ */

/** 通用配置 DTO 基础字段 */
export interface LowcodeConfigDTOBase {
  configCode: string
  configName: string
  schemaJson: string
  templateId?: number | null
  status?: LowcodeStatus
  description?: string
}

export interface LowcodeFormConfigDTO extends LowcodeConfigDTOBase {}
export interface LowcodeListConfigDTO extends LowcodeConfigDTOBase {}
export interface LowcodeTabConfigDTO extends LowcodeConfigDTOBase {}
export interface LowcodeRelationConfigDTO extends LowcodeConfigDTOBase {}

export interface LowcodeTemplateDTO {
  templateCode: string
  templateName: string
  templateType: LowcodeTemplateType
  schemaJson: string
  description?: string
  status?: LowcodeStatus
}

/** 实例化 DTO */
export interface LowcodeInstantiateDTO {
  configName: string
  configCode?: string
}

/* ============ 查询参数 ============ */

import type { PageParams } from '@/types/api'

/** 配置查询参数 */
export interface LowcodeConfigQueryParams extends PageParams {
  keyword?: string
  status?: LowcodeStatus
  templateId?: number
}

/** 模板查询参数 */
export interface LowcodeTemplateQueryParams extends PageParams {
  keyword?: string
  templateType?: LowcodeTemplateType
  status?: LowcodeStatus
}

/* ============ Schema 解析工具 ============ */

/**
 * 解析 schemaJson 字符串为 FormSchema 对象。
 * 解析失败返回 null。
 */
export function parseFormSchema(schemaJson: string): FormSchema | null {
  try {
    const parsed = JSON.parse(schemaJson) as FormSchema
    if (parsed && parsed.type === 'object' && parsed.properties) {
      return parsed
    }
    return null
  } catch {
    return null
  }
}

/** 解析 ListSchema */
export function parseListSchema(schemaJson: string): ListSchema | null {
  try {
    const parsed = JSON.parse(schemaJson) as ListSchema
    if (parsed && parsed.type === 'list' && Array.isArray(parsed.columns)) {
      return parsed
    }
    return null
  } catch {
    return null
  }
}

/** 解析 TabSchema */
export function parseTabSchema(schemaJson: string): TabSchema | null {
  try {
    const parsed = JSON.parse(schemaJson) as TabSchema
    if (parsed && parsed.type === 'tab' && Array.isArray(parsed.tabs)) {
      return parsed
    }
    return null
  } catch {
    return null
  }
}

/** 解析 RelationSchema */
export function parseRelationSchema(schemaJson: string): RelationSchema | null {
  try {
    const parsed = JSON.parse(schemaJson) as RelationSchema
    if (parsed && parsed.type === 'relation' && parsed.master && Array.isArray(parsed.details)) {
      return parsed
    }
    return null
  } catch {
    return null
  }
}

/** 将 FormSchema 序列化为字符串（用于提交到后端） */
export function stringifyFormSchema(schema: FormSchema): string {
  return JSON.stringify(schema, null, 2)
}

/** 将 ListSchema 序列化为字符串 */
export function stringifyListSchema(schema: ListSchema): string {
  return JSON.stringify(schema, null, 2)
}

/** 将 TabSchema 序列化为字符串 */
export function stringifyTabSchema(schema: TabSchema): string {
  return JSON.stringify(schema, null, 2)
}

/** 将 RelationSchema 序列化为字符串 */
export function stringifyRelationSchema(schema: RelationSchema): string {
  return JSON.stringify(schema, null, 2)
}
