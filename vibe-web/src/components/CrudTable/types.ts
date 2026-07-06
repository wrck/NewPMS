/**
 * CrudTable 通用 CRUD 表格组件类型定义
 * 对应 spec：阶段三 前端通用组件抽取 - Task 12
 */
import type { VNode } from 'vue'
import type { PageResult, PageParams } from '@/types/api'

/** 表单字段控件类型 */
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

/** 表格列对齐方式 */
export type ColumnAlign = 'left' | 'center' | 'right'

/** 枚举值项 */
export interface ValueEnumItem {
  /** 显示文本 */
  text: string
  /** 状态色（用于 StatusTag / a-tag color），可选 */
  status?: string
  /** 是否禁用该选项（在表单 select 中） */
  disabled?: boolean
}

/** 通用列定义，扩展 antd TableColumn 并附加 CRUD 元信息 */
export interface CrudColumn {
  /** 字段名（对应 record 上的属性，支持 a.b 嵌套） */
  field: string
  /** 列标题 */
  title: string
  /** 列宽 */
  width?: number | string
  /** 对齐方式 */
  align?: ColumnAlign
  /** 是否可排序（前端排序或请求后端 sortField） */
  sortable?: boolean
  /** 是否可搜索（保留字段，CrudTable 实际渲染依赖 searchFields） */
  searchable?: boolean
  /** 单元格文本格式化 */
  format?: (value: any, record: any) => string
  /** 枚举映射：自动渲染 Tag/StatusTag */
  valueEnum?: Record<string, ValueEnumItem>
  /** 表单控件类型，未声明则不在表单中出现 */
  formType?: FormFieldType
  /** 表单校验规则 */
  formRules?: any[]
  /** 表单控件选项（select/radio/checkbox/cascader/treeSelect 使用） */
  formOptions?: any[]
  /** 表单栅格跨度（a-col span，默认 12） */
  formSpan?: number
  /** 表单默认值 */
  formDefaultValue?: any
  /** 是否在表单中隐藏 */
  hideInForm?: boolean
  /** 是否在表格中隐藏 */
  hideInTable?: boolean
  /** 表单中只读（编辑时禁用） */
  readonly?: boolean
  /** 列是否固定（'left' / 'right'） */
  fixed?: 'left' | 'right'
  /** 是否省略号显示 */
  ellipsis?: boolean
}

/** 搜索字段控件类型 */
export type SearchFieldType =
  | 'input'
  | 'select'
  | 'date'
  | 'dateRange'
  | 'cascader'
  | 'treeSelect'
  | 'switch'

/** 搜索字段定义 */
export interface SearchField {
  /** 字段名（对应 query 上的属性，支持 a.b 嵌套） */
  field: string
  /** 标签 */
  label: string
  /** 控件类型 */
  type: SearchFieldType
  /** 选项（select/cascader/treeSelect 使用） */
  options?: any[]
  /** 默认值 */
  defaultValue?: any
  /** 占位提示 */
  placeholder?: string
  /** 控件宽度（px） */
  width?: number | string
  /** 是否允许清空 */
  allowClear?: boolean
}

/** 自定义行级操作按钮 */
export interface CrudAction {
  /** 按钮文本 */
  label: string
  /** 图标（ant-design icon 组件或 VNode） */
  icon?: any
  /** 点击回调 */
  onClick: (record: any) => void | Promise<void>
  /** 是否可见（按行判断） */
  visible?: (record: any) => boolean
  /** 权限标识（与 permissionPrefix 拼接校验） */
  permission?: string
  /** 是否危险操作（红色） */
  danger?: boolean
  /** 是否显示为 divider 分隔符 */
  divider?: boolean
}

/** 主键绑定配置 */
export interface ModelBinding {
  /** 主键字段名（默认 'id'） */
  idField?: string
  /** 主键标签（用于删除提示，默认 'ID'） */
  idLabel?: string
}

/** CRUD API 函数集合 */
export interface CrudApiFunc<RecordType = any, DTOType = any, QueryType = any> {
  /** 分页查询 */
  page: (params: QueryType & PageParams) => Promise<PageResult<RecordType>>
  /** 新增 */
  create: (data: DTOType) => Promise<unknown>
  /** 编辑（更新） */
  update: (id: any, data: DTOType) => Promise<unknown>
  /** 删除 */
  delete: (id: any) => Promise<unknown>
}

/** 暴露给外部的 Ref 实例方法 */
export interface CrudTableExpose {
  /** 主动刷新当前页 */
  refresh: () => Promise<void>
  /** 重置搜索条件并刷新 */
  reset: () => Promise<void>
  /** 打开新增弹窗 */
  openCreate: () => void
  /** 打开编辑弹窗 */
  openEdit: (record: any) => void
  /** 获取当前选中行 */
  getSelectedRows: () => any[]
  /** 清空选中 */
  clearSelected: () => void
}

/** 导出 VNode 类型用于 icon slot */
export type { VNode }
