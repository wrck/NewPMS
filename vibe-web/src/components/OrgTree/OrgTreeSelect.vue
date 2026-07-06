<script setup lang="ts">
/**
 * OrgTreeSelect 组织树选择器（spec 阶段三 - Task 16 - SubTask 16.4）
 *
 * 基于 a-tree-select 的表单内嵌变体，与 OrgTree 共用 useOrgTree composable：
 *   1. 调用 /api/v1/orgs/tree 加载组织树（共享 5 分钟 LRU 缓存）
 *   2. 适配 FormModal 的 treeSelect 字段类型，可独立使用或嵌入 FormModal 的 form-extra slot
 *   3. 支持单选/多选、按 orgType 过滤、关键字搜索
 *   4. v-model 双向绑定（单选 number、多选 number[]）
 *
 * 使用示例见 ./README.md
 */
import { computed, watch, onMounted } from 'vue'
import { useOrgTree, filterByOrgType } from './useOrgTree'
import {
  TREE_SELECT_FIELD_NAMES,
  type OrgTreeNode,
  type OrgTreeSelectProps
} from './types'

/* ============ Props ============ */
interface Props extends OrgTreeSelectProps {}

const props = withDefaults(defineProps<Props>(), {
  orgType: 'all',
  multiple: false,
  searchable: true,
  defaultExpandAll: true,
  disabled: false,
  placeholder: '请选择组织'
})

/* ============ Emits ============ */
interface Emits {
  /** v-model 更新 */
  (e: 'update:modelValue', value: number | number[] | undefined): void
  /** 值变化时触发（含选中节点数组） */
  (e: 'change', value: number | number[] | undefined, nodes: OrgTreeNode[]): void
  /** 下拉展开/收起 */
  (e: 'dropdownVisibleChange', visible: boolean): void
  /** 搜索值变化 */
  (e: 'search', value: string): void
}

const emit = defineEmits<Emits>()

/* ============ Composable ============ */
const { treeData, loading, fetchTree, findNode } = useOrgTree()

const fieldNames = TREE_SELECT_FIELD_NAMES

/* ============ 计算属性 ============ */
/** 当前值（透传给 a-tree-select） */
const innerValue = computed<number | number[] | undefined>(() => props.modelValue)

/** 按 orgType 过滤后的树数据 */
const filteredTreeData = computed<OrgTreeNode[]>(() => {
  if (props.orgType === 'all') return treeData.value
  return filterByOrgType(treeData.value, props.orgType)
})

/* ============ 事件处理 ============ */
function handleChange(value: unknown): void {
  let emitValue: number | number[] | undefined
  if (value === undefined || value === null) {
    emitValue = undefined
  } else if (Array.isArray(value)) {
    emitValue = value as number[]
  } else {
    emitValue = value as number
  }
  emit('update:modelValue', emitValue)
  const nodes = resolveNodes(emitValue)
  emit('change', emitValue, nodes)
}

function handleDropdownVisibleChange(visible: boolean): void {
  emit('dropdownVisibleChange', visible)
}

function handleSearch(value: string): void {
  emit('search', value)
}

/** 根据值解析为节点数组 */
function resolveNodes(value: number | number[] | undefined): OrgTreeNode[] {
  if (value === undefined || value === null) return []
  if (Array.isArray(value)) {
    return value.map((v) => findNode(v)).filter(Boolean) as OrgTreeNode[]
  }
  const node = findNode(value)
  return node ? [node] : []
}

/* ============ 监听 status 变化重新拉取 ============ */
watch(
  () => props.status,
  () => {
    fetchTree({ force: true, status: props.status })
  }
)

/* ============ 初始化 ============ */
onMounted(() => {
  fetchTree({ status: props.status })
})

/* ============ Expose ============ */
defineExpose({
  refresh: () => fetchTree({ force: true, status: props.status }),
  findNode,
  getTreeData: () => treeData.value
})
</script>

<template>
  <a-tree-select
    :value="innerValue"
    :tree-data="filteredTreeData"
    :field-names="fieldNames"
    :multiple="multiple"
    :tree-checkable="multiple"
    :show-search="searchable"
    :tree-default-expand-all="defaultExpandAll"
    :placeholder="placeholder"
    :disabled="disabled"
    :loading="loading"
    :allow-clear="true"
    tree-node-filter-prop="orgName"
    :show-checked-strategy="multiple ? 'SHOW_PARENT' : undefined"
    style="width: 100%"
    @change="handleChange"
    @dropdown-visible-change="handleDropdownVisibleChange"
    @search="handleSearch"
  />
</template>

<style lang="less" scoped>
.org-tree-select {
  width: 100%;
}
</style>
