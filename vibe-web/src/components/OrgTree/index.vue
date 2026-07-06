<script setup lang="ts">
/**
 * OrgTree 组织树组件（spec 阶段三 - Task 16）
 *
 * 一站式封装：
 *   1. 调用 /api/v1/orgs/tree 加载组织树（基于 useOrgTree composable，5 分钟 LRU 缓存）
 *   2. 内置搜索框，支持名称模糊匹配并高亮命中关键字
 *   3. 支持单选 / 多选 / 复选框模式
 *   4. 支持按 orgType 过滤（company / department / team）
 *   5. 支持 v-model 双向绑定（单选 number、多选 number[]）
 *   6. 支持展开/折叠、默认展开全部、默认展开指定 keys
 *   7. 暴露 refresh / expandAll / collapseAll / findNode / getPath 方法
 *
 * 使用示例见 ./README.md
 */
import { ref, computed, watch, onMounted } from 'vue'
import {
  useOrgTree,
  filterTree,
  filterByOrgType,
  collectAllKeys
} from './useOrgTree'
import {
  TREE_FIELD_NAMES,
  type OrgTreeNode,
  type OrgTreeProps,
  type OrgTreeExpose,
  type HighlightSegment
} from './types'

/* ============ Props ============ */
interface Props extends OrgTreeProps {}

const props = withDefaults(defineProps<Props>(), {
  orgType: 'all',
  single: true,
  multiple: false,
  checkable: false,
  selectable: true,
  searchable: true,
  showLine: false,
  lazyLoad: false,
  disabled: false,
  defaultExpandAll: false,
  defaultExpandedKeys: () => []
})

/* ============ Emits ============ */
interface Emits {
  /** v-model 更新 */
  (e: 'update:modelValue', value: number | number[] | undefined): void
  /** 值变化时触发（含选中节点数组） */
  (e: 'change', value: number | number[] | undefined, nodes: OrgTreeNode[]): void
  /** 节点选中事件（透传 a-tree select） */
  (e: 'select', nodes: OrgTreeNode[], info: unknown): void
  /** 节点勾选事件（透传 a-tree check） */
  (e: 'check', nodes: OrgTreeNode[], info: unknown): void
  /** 展开事件 */
  (e: 'expand', expandedKeys: number[], info: unknown): void
}

const emit = defineEmits<Emits>()

/* ============ Composable ============ */
const { treeData, loading, fetchTree, findNode, getPath } = useOrgTree()

/* ============ 内部状态 ============ */
const searchValue = ref('')
const expandedKeys = ref<number[]>([...props.defaultExpandedKeys])
const checkedKeys = ref<number[]>([])
const selectedKeys = ref<number[]>([])
/** 标记是否已初始化 expandedKeys，避免覆盖父组件传入的 defaultExpandedKeys */
let expandedInitialized = false

const fieldNames = TREE_FIELD_NAMES

/* ============ 计算属性：过滤后的树数据 ============ */
const filteredTreeData = computed<OrgTreeNode[]>(() => {
  let result = treeData.value
  // 按 orgType 过滤
  if (props.orgType !== 'all') {
    result = filterByOrgType(result, props.orgType)
  }
  // 按关键字过滤
  if (searchValue.value) {
    result = filterTree(result, searchValue.value)
  }
  return result
})

/**
 * a-tree 的 treeData 期望 DataNode[]（含 key 字段），
 * 但 OrgTreeNode 使用 id（通过 fieldNames 重映射）。
 * 这里 cast 为 any[] 以绕过严格类型检查（fieldNames 已正确重映射）。
 */
const treeDataForRender = computed<any[]>(() => filteredTreeData.value as unknown as any[])

/* ============ 同步 modelValue -> 内部 keys ============ */
watch(
  () => props.modelValue,
  (val) => {
    if (val === undefined || val === null) {
      checkedKeys.value = []
      selectedKeys.value = []
      return
    }
    if (Array.isArray(val)) {
      checkedKeys.value = val
      selectedKeys.value = val
    } else {
      checkedKeys.value = []
      selectedKeys.value = [val]
    }
  },
  { immediate: true }
)

/* ============ 监听树数据变化，初始化展开 keys ============ */
watch(
  filteredTreeData,
  (nodes) => {
    if (!expandedInitialized && nodes.length > 0) {
      if (props.defaultExpandAll) {
        expandedKeys.value = collectAllKeys(nodes)
      } else if (props.defaultExpandedKeys.length > 0) {
        expandedKeys.value = [...props.defaultExpandedKeys]
      }
      expandedInitialized = true
    }
  },
  { immediate: true }
)

/* ============ 搜索时自动展开所有过滤后的节点 ============ */
watch(
  searchValue,
  (keyword) => {
    if (keyword) {
      // 搜索时展开所有可见节点，确保匹配结果可见
      expandedKeys.value = collectAllKeys(filteredTreeData.value)
    }
  }
)

/* ============ 监听 status 变化重新拉取 ============ */
watch(
  () => props.status,
  () => {
    fetchTree({ force: true, status: props.status })
  }
)

/* ============ 高亮文本分段 ============ */
function highlightSegments(text: string, keyword: string): HighlightSegment[] {
  if (!text) return [{ text: '', match: false }]
  if (!keyword) return [{ text, match: false }]
  const lower = text.toLowerCase()
  const klower = keyword.toLowerCase()
  const segments: HighlightSegment[] = []
  let i = 0
  while (i < text.length) {
    const idx = lower.indexOf(klower, i)
    if (idx === -1) {
      segments.push({ text: text.slice(i), match: false })
      break
    }
    if (idx > i) {
      segments.push({ text: text.slice(i, idx), match: false })
    }
    segments.push({ text: text.slice(idx, idx + klower.length), match: true })
    i = idx + klower.length
  }
  return segments
}

/* ============ 事件处理 ============ */
function handleCheck(checked: unknown, info: unknown): void {
  let value: number[] = []
  if (Array.isArray(checked)) {
    value = checked as number[]
  } else if (checked && typeof checked === 'object' && Array.isArray((checked as any).checked)) {
    // 兼容 checkable + checkStrictly 模式下返回 { checked, halfChecked }
    value = (checked as any).checked as number[]
  }
  checkedKeys.value = value
  const nodes = value.map((id) => findNode(id)).filter(Boolean) as OrgTreeNode[]
  emit('update:modelValue', value)
  emit('change', value, nodes)
  emit('check', nodes, info)
}

function handleSelect(selected: unknown, info: unknown): void {
  const value = (Array.isArray(selected) ? selected : []) as number[]
  selectedKeys.value = value
  let emitValue: number | number[] | undefined
  if (props.multiple) {
    emitValue = value
  } else {
    emitValue = value[0]
  }
  const nodes = value.map((id) => findNode(id)).filter(Boolean) as OrgTreeNode[]
  emit('update:modelValue', emitValue)
  emit('change', emitValue, nodes)
  emit('select', nodes, info)
}

function handleExpand(keys: unknown, info: unknown): void {
  expandedKeys.value = (Array.isArray(keys) ? keys : []) as number[]
  emit('expand', expandedKeys.value, info)
}

/* ============ Expose 方法 ============ */
async function refresh(): Promise<void> {
  await fetchTree({ force: true, status: props.status })
}

function expandAll(): void {
  expandedKeys.value = collectAllKeys(filteredTreeData.value)
}

function collapseAll(): void {
  expandedKeys.value = []
}

function getTreeData(): OrgTreeNode[] {
  return treeData.value
}

defineExpose<OrgTreeExpose>({
  refresh,
  expandAll,
  collapseAll,
  getTreeData,
  findNode,
  getPath
})

/* ============ 初始化 ============ */
onMounted(() => {
  fetchTree({ status: props.status })
})
</script>

<template>
  <div class="org-tree-container">
    <a-input-search
      v-if="searchable"
      v-model:value="searchValue"
      placeholder="搜索组织名称"
      allow-clear
      class="org-tree-search"
    />
    <a-spin :spinning="loading">
      <a-tree
        v-if="filteredTreeData.length > 0"
        :tree-data="treeDataForRender"
        :field-names="fieldNames"
        :checkable="checkable"
        :selectable="selectable"
        :multiple="multiple"
        :default-expanded-keys="defaultExpandedKeys"
        :expanded-keys="expandedKeys"
        :checked-keys="checkedKeys"
        :selected-keys="selectedKeys"
        :show-line="showLine"
        :disabled="disabled"
        :block-node="true"
        @check="handleCheck"
        @select="handleSelect"
        @expand="handleExpand"
      >
        <template #title="node">
          <span class="org-node-title">
            <template
              v-for="(seg, idx) in highlightSegments(node.orgName, searchValue)"
              :key="idx"
            >
              <mark v-if="seg.match" class="org-highlight">{{ seg.text }}</mark>
              <template v-else>{{ seg.text }}</template>
            </template>
            <a-tag
              v-if="node.orgCode"
              color="blue"
              class="org-code-tag"
            >
              {{ node.orgCode }}
            </a-tag>
          </span>
        </template>
      </a-tree>
      <a-empty
        v-else
        :description="loading ? '加载中...' : '暂无数据'"
      />
    </a-spin>
  </div>
</template>

<style lang="less" scoped>
.org-tree-container {
  width: 100%;
}
.org-tree-search {
  margin-bottom: 8px;
}
.org-node-title {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.org-code-tag {
  margin-left: 4px;
  font-size: 12px;
  line-height: 18px;
  padding: 0 4px;
}
.org-highlight {
  background-color: #fff3b0;
  color: #d4380d;
  padding: 0;
  margin: 0;
}
</style>
