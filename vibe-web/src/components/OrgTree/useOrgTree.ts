/**
 * useOrgTree 组织树 composable
 * 对应 spec：阶段三 前端通用组件抽取 - Task 16 - SubTask 16.2 / 16.4
 *
 * 提供能力：
 *   - fetchTree：调用 listOrgTree 加载组织树，内置 5 分钟 LRU 缓存
 *   - findNode：递归查找指定 id 的节点
 *   - getPath：返回从根到指定节点的路径数组
 *   - filterTree：根据关键字过滤树（保留匹配项与祖先链）
 *   - filterByOrgType：根据 orgType 过滤树
 *   - collectKeys：递归收集所有节点 key
 *
 * 缓存策略：
 *   - 模块级单例缓存，多个组件实例共享
 *   - TTL 5 分钟；超时后下次调用自动刷新
 *   - 提供 clearCache 用于强制失效
 */
import { ref, type Ref } from 'vue'
import { listOrgTree } from '@/api/system'
import type { SysOrg } from '@/api/system'
import type { OrgTreeNode, OrgType, OrgTypeFilter } from './types'

/** 缓存有效期 5 分钟 */
const CACHE_TTL = 5 * 60 * 1000

interface CacheEntry {
  data: OrgTreeNode[]
  timestamp: number
}

/** 模块级缓存（所有 useOrgTree 实例共享） */
let cache: CacheEntry | null = null

/** 判断缓存是否有效 */
function isCacheValid(): boolean {
  return !!cache && Date.now() - cache.timestamp < CACHE_TTL
}

/** 写入缓存 */
function setCache(data: OrgTreeNode[]): void {
  cache = { data, timestamp: Date.now() }
}

/** 清空缓存（强制下次重新拉取） */
export function clearOrgTreeCache(): void {
  cache = null
}

/**
 * 递归查找节点
 * @param nodes 树节点数组
 * @param id 目标节点 id
 */
export function findNodeInTree(nodes: OrgTreeNode[], id: number): OrgTreeNode | undefined {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children && node.children.length) {
      const found = findNodeInTree(node.children, id)
      if (found) return found
    }
  }
  return undefined
}

/**
 * 递归查找从根到指定节点的路径
 * @param nodes 树节点数组
 * @param id 目标节点 id
 * @returns 路径数组（从根到目标节点）；未找到返回空数组
 */
export function findPathInTree(nodes: OrgTreeNode[], id: number): OrgTreeNode[] {
  for (const node of nodes) {
    if (node.id === id) return [node]
    if (node.children && node.children.length) {
      const subPath = findPathInTree(node.children, id)
      if (subPath.length > 0) return [node, ...subPath]
    }
  }
  return []
}

/**
 * 关键字过滤树
 *
 * 过滤规则：
 *   - 节点 orgName 包含关键字 → 保留该节点（及其全部原子树）
 *   - 子节点有匹配 → 保留父节点（仅保留匹配的子分支）
 *   - 都不匹配 → 移除
 *
 * @param nodes 原始树节点数组
 * @param keyword 搜索关键字（大小写不敏感）
 */
export function filterTree(nodes: OrgTreeNode[], keyword: string): OrgTreeNode[] {
  if (!keyword) return nodes
  const lower = keyword.toLowerCase()
  const result: OrgTreeNode[] = []
  for (const node of nodes) {
    const matched = (node.orgName || '').toLowerCase().includes(lower)
    const filteredChildren = node.children ? filterTree(node.children, keyword) : []
    if (matched) {
      // 节点自身匹配：保留其完整原子树
      result.push({ ...node })
    } else if (filteredChildren.length > 0) {
      // 节点不匹配但有匹配的子节点：保留并仅挂载匹配子分支
      result.push({ ...node, children: filteredChildren })
    }
  }
  return result
}

/**
 * 按组织类型过滤树
 *
 * 过滤规则：
 *   - orgType === 'all' → 返回原树
 *   - 节点 orgType 匹配 → 保留（及其完整子树）
 *   - 子节点有匹配 → 保留父节点（仅挂载匹配的子分支）
 */
export function filterByOrgType(nodes: OrgTreeNode[], orgType: OrgTypeFilter): OrgTreeNode[] {
  if (orgType === 'all') return nodes
  const result: OrgTreeNode[] = []
  for (const node of nodes) {
    const matched = node.orgType === orgType
    const filteredChildren = node.children ? filterByOrgType(node.children, orgType) : []
    if (matched) {
      result.push({ ...node })
    } else if (filteredChildren.length > 0) {
      result.push({ ...node, children: filteredChildren })
    }
  }
  return result
}

/**
 * 递归收集所有节点 key（用于展开全部）
 */
export function collectAllKeys(nodes: OrgTreeNode[]): number[] {
  const keys: number[] = []
  const walk = (list: OrgTreeNode[]): void => {
    for (const n of list) {
      keys.push(n.id)
      if (n.children && n.children.length) walk(n.children)
    }
  }
  walk(nodes)
  return keys
}

/** useOrgTree 拉取选项 */
export interface UseOrgTreeFetchOptions {
  /** 按状态过滤（1=启用 / 0=禁用） */
  status?: 1 | 0
  /** 是否强制跳过缓存 */
  force?: boolean
}

/** useOrgTree 返回值 */
export interface UseOrgTreeReturn {
  /** 树数据 */
  treeData: Ref<OrgTreeNode[]>
  /** 加载状态 */
  loading: Ref<boolean>
  /** 错误对象 */
  error: Ref<Error | null>
  /** 拉取组织树（命中缓存时直接返回） */
  fetchTree: (options?: UseOrgTreeFetchOptions) => Promise<OrgTreeNode[]>
  /** 根据 id 查找节点（基于当前 treeData） */
  findNode: (id: number) => OrgTreeNode | undefined
  /** 获取从根到指定节点的路径 */
  getPath: (id: number) => OrgTreeNode[]
  /** 清空缓存 */
  clearCache: () => void
}

/**
 * 组织树 composable
 *
 * 使用示例：
 * ```ts
 * const { treeData, loading, fetchTree, findNode, getPath } = useOrgTree()
 * onMounted(() => fetchTree())
 * ```
 */
export function useOrgTree(): UseOrgTreeReturn {
  const treeData = ref<OrgTreeNode[]>([]) as Ref<OrgTreeNode[]>
  const loading = ref(false)
  const error = ref<Error | null>(null)

  async function fetchTree(options: UseOrgTreeFetchOptions = {}): Promise<OrgTreeNode[]> {
    const { status, force = false } = options
    // 命中缓存直接返回（不区分 status 参数，按首次拉取为准）
    if (!force && isCacheValid() && cache) {
      treeData.value = cache.data
      return cache.data
    }
    loading.value = true
    error.value = null
    try {
      const params = status !== undefined ? { status } : undefined
      const res = (await listOrgTree(params as Record<string, unknown> | undefined)) as unknown as SysOrg[]
      const data = (res || []) as OrgTreeNode[]
      treeData.value = data
      setCache(data)
      return data
    } catch (e) {
      const err = e instanceof Error ? e : new Error(String(e))
      error.value = err
      console.error('[useOrgTree] fetchTree failed:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  function findNode(id: number): OrgTreeNode | undefined {
    return findNodeInTree(treeData.value, id)
  }

  function getPath(id: number): OrgTreeNode[] {
    return findPathInTree(treeData.value, id)
  }

  function clearCache(): void {
    clearOrgTreeCache()
  }

  return {
    treeData,
    loading,
    error,
    fetchTree,
    findNode,
    getPath,
    clearCache
  }
}

/** 重新导出类型以便外部使用 */
export type { OrgTreeNode, OrgType, OrgTypeFilter }
