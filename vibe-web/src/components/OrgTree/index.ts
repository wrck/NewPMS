/**
 * OrgTree 组织树组件入口
 * 对应 spec：阶段三 前端通用组件抽取 - Task 16
 *
 * 导出：
 *   - OrgTree（默认导出，全功能组织树）
 *   - OrgTreeSelect（表单内嵌的 a-tree-select 变体）
 *   - useOrgTree（composable，含 LRU 缓存）
 *   - 类型 OrgTreeNode / OrgTreeProps / OrgTreeSelectProps / OrgTreeExpose
 *   - 工具函数 filterTree / filterByOrgType / findNodeInTree / findPathInTree / collectAllKeys / clearOrgTreeCache
 */
import OrgTree from './index.vue'

export { default as OrgTree } from './index.vue'
export { default as OrgTreeSelect } from './OrgTreeSelect.vue'
export {
  useOrgTree,
  filterTree,
  filterByOrgType,
  findNodeInTree,
  findPathInTree,
  collectAllKeys,
  clearOrgTreeCache
} from './useOrgTree'
export type { UseOrgTreeReturn, UseOrgTreeFetchOptions } from './useOrgTree'
export type {
  OrgTreeNode,
  OrgType,
  OrgTypeFilter,
  OrgTreeProps,
  OrgTreeSelectProps,
  OrgTreeExpose,
  HighlightSegment
} from './types'
export { TREE_FIELD_NAMES, TREE_SELECT_FIELD_NAMES } from './types'

export default OrgTree
