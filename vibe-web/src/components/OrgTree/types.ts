/**
 * OrgTree 组织树组件类型定义
 * 对应 spec：阶段三 前端通用组件抽取 - Task 16
 *
 * 设计目标：
 *   - OrgTreeNode 扩展自后端 SysOrg，附加 orgType 字段以支持按类型过滤
 *   - OrgTreeProps 覆盖单选/多选、可勾选、可搜索、懒加载等场景
 *   - OrgTreeSelectProps 用于表单内嵌的 a-tree-select 变体
 */
import type { SysOrg } from '@/api/system'

/** 组织类型枚举（用于前端按类型过滤） */
export type OrgType = 'company' | 'department' | 'team'

/** 组织类型过滤选项：'all' 表示不过滤 */
export type OrgTypeFilter = 'all' | OrgType

/**
 * 组织树节点
 *
 * 扩展自后端 SysOrg：
 *   - 覆盖 children 类型为 OrgTreeNode[]（递归结构）
 *   - 附加 orgType 字段（后端可枚举：company/department/team）
 */
export interface OrgTreeNode extends Omit<SysOrg, 'children'> {
  /** 组织类型（公司 / 部门 / 小组） */
  orgType?: OrgType
  /** 子节点（递归 OrgTreeNode） */
  children?: OrgTreeNode[]
}

/**
 * OrgTree 主组件 Props
 */
export interface OrgTreeProps {
  /** v-model 绑定值：单选为 number，多选为 number[] */
  modelValue?: number | number[]
  /** 组织类型过滤，'all' 表示不过滤 */
  orgType?: OrgTypeFilter
  /** 单选模式（默认 true，与 multiple 互斥） */
  single?: boolean
  /** 多选模式 */
  multiple?: boolean
  /** 是否显示复选框（多选时通常为 true） */
  checkable?: boolean
  /** 是否可点击节点选中 */
  selectable?: boolean
  /** 默认展开的节点 key */
  defaultExpandedKeys?: number[]
  /** 是否显示搜索框 */
  searchable?: boolean
  /** 是否显示连接线 */
  showLine?: boolean
  /** 是否懒加载子节点（API 已返回完整树时为模拟懒加载） */
  lazyLoad?: boolean
  /** 是否禁用 */
  disabled?: boolean
  /** 按状态过滤（1=启用 / 0=禁用，传给 listOrgTree） */
  status?: 1 | 0
  /** 默认展开全部 */
  defaultExpandAll?: boolean
}

/**
 * OrgTreeSelect 表单选择器 Props
 */
export interface OrgTreeSelectProps {
  /** v-model 绑定值 */
  modelValue?: number | number[]
  /** 组织类型过滤 */
  orgType?: OrgTypeFilter
  /** 是否多选 */
  multiple?: boolean
  /** 是否可搜索 */
  searchable?: boolean
  /** 默认展开全部 */
  defaultExpandAll?: boolean
  /** 是否禁用 */
  disabled?: boolean
  /** 占位提示 */
  placeholder?: string
  /** 按状态过滤 */
  status?: 1 | 0
}

/**
 * OrgTree 组件 Expose 方法
 */
export interface OrgTreeExpose {
  /** 强制重新拉取组织树（跳过缓存） */
  refresh: () => Promise<void>
  /** 获取当前树数据 */
  getTreeData: () => OrgTreeNode[]
  /** 根据 id 查找节点 */
  findNode: (id: number) => OrgTreeNode | undefined
  /** 获取从根到指定节点的路径数组 */
  getPath: (id: number) => OrgTreeNode[]
  /** 展开全部节点 */
  expandAll: () => void
  /** 折叠全部节点 */
  collapseAll: () => void
}

/** 树字段名映射（a-tree field-names） */
export const TREE_FIELD_NAMES = {
  title: 'orgName',
  key: 'id',
  children: 'children'
} as const

/** tree-select 字段名映射（a-tree-select field-names） */
export const TREE_SELECT_FIELD_NAMES = {
  label: 'orgName',
  value: 'id',
  children: 'children'
} as const

/** 高亮文本片段 */
export interface HighlightSegment {
  /** 文本内容 */
  text: string
  /** 是否匹配关键字 */
  match: boolean
}
