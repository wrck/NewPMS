# OrgTree 组织树组件

> spec 阶段三 Task 16：抽取通用组织树组件，封装组织树加载（`/api/v1/orgs/tree`）、搜索过滤、展开/折叠、节点选择，并提供 `OrgTreeSelect` 表单选择器变体。

## 目录

- [OrgTree 组织树](#orgtree-组织树)
- [OrgTreeSelect 表单选择器](#orgtreeselect-表单选择器)
- [useOrgTree Composable](#useorgtree-composable)
- [工具函数](#工具函数)
- [类型定义](#类型定义)
- [嵌入 FormModal](#嵌入-formmodal)
- [注意事项](#注意事项)

## OrgTree 组织树

完整的组织树组件：内置搜索框、按 `orgType` 过滤、单选/多选/复选、`v-model` 双向绑定、节点名称关键字高亮。

```vue
<template>
  <OrgTree
    v-model="selectedOrgId"
    :default-expanded-keys="[1, 2]"
    :searchable="true"
    :show-line="true"
    @change="onChange"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { OrgTree } from '@/components/OrgTree'
import type { OrgTreeNode } from '@/components/OrgTree'

const selectedOrgId = ref<number>()

function onChange(value: number | number[] | undefined, nodes: OrgTreeNode[]) {
  console.log('selected:', value, nodes)
}
</script>
```

### 多选（复选框）模式

```vue
<template>
  <OrgTree
    v-model="selectedOrgIds"
    :multiple="true"
    :checkable="true"
    :selectable="false"
    @change="onChange"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { OrgTree } from '@/components/OrgTree'

const selectedOrgIds = ref<number[]>([])
function onChange(value: number | number[] | undefined) {
  console.log('checked:', value)
}
</script>
```

### 按 orgType 过滤

```vue
<template>
  <OrgTree org-type="department" :searchable="true" />
</template>
```

### Props

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `modelValue` | `number \| number[]` | - | v-model 绑定值；单选为 `number`，多选为 `number[]` |
| `orgType` | `'all' \| 'company' \| 'department' \| 'team'` | `'all'` | 按组织类型过滤 |
| `single` | `boolean` | `true` | 单选模式（与 `multiple` 互斥） |
| `multiple` | `boolean` | `false` | 多选模式 |
| `checkable` | `boolean` | `false` | 是否显示复选框 |
| `selectable` | `boolean` | `true` | 是否可点击节点选中 |
| `defaultExpandedKeys` | `number[]` | `[]` | 默认展开的节点 key |
| `searchable` | `boolean` | `true` | 是否显示搜索框 |
| `showLine` | `boolean` | `false` | 是否显示连接线 |
| `lazyLoad` | `boolean` | `false` | 是否懒加载子节点（API 已返回完整树时为预留字段） |
| `disabled` | `boolean` | `false` | 是否禁用 |
| `status` | `1 \| 0` | - | 按状态过滤（传给 `listOrgTree`） |
| `defaultExpandAll` | `boolean` | `false` | 默认展开全部节点 |

### Emits

| 事件 | 参数 | 说明 |
| --- | --- | --- |
| `update:modelValue` | `(value: number \| number[] \| undefined)` | v-model 同步 |
| `change` | `(value, nodes: OrgTreeNode[])` | 值变化时触发，附带选中节点对象 |
| `select` | `(nodes, info)` | 节点选中事件（透传 a-tree select） |
| `check` | `(nodes, info)` | 节点勾选事件（透传 a-tree check） |
| `expand` | `(expandedKeys, info)` | 展开事件 |

### Expose

通过 `ref` 可调用以下方法：

```ts
import type { OrgTreeExpose } from '@/components/OrgTree'

const treeRef = ref<OrgTreeExpose>()

// 强制重新拉取组织树（跳过缓存）
await treeRef.value?.refresh()

// 获取当前树数据
const data = treeRef.value?.getTreeData()

// 根据 id 查找节点
const node = treeRef.value?.findNode(123)

// 获取从根到节点的路径
const path = treeRef.value?.getPath(123)

// 展开/折叠全部
treeRef.value?.expandAll()
treeRef.value?.collapseAll()
```

## OrgTreeSelect 表单选择器

基于 `a-tree-select` 的表单内嵌变体，与 OrgTree 共享 `useOrgTree` 缓存。适合放在表单中作为「所属组织」字段。

```vue
<template>
  <a-form-item label="所属组织" name="orgId">
    <OrgTreeSelect v-model="form.orgId" placeholder="请选择所属组织" />
  </a-form-item>

  <a-form-item label="可见组织" name="visibleOrgIds">
    <OrgTreeSelect v-model="form.visibleOrgIds" multiple placeholder="请选择可见组织" />
  </a-form-item>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { OrgTreeSelect } from '@/components/OrgTree'

const form = reactive<{ orgId?: number; visibleOrgIds?: number[] }>({})
</script>
```

### Props

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `modelValue` | `number \| number[]` | - | v-model 绑定值 |
| `orgType` | `'all' \| 'company' \| 'department' \| 'team'` | `'all'` | 按组织类型过滤 |
| `multiple` | `boolean` | `false` | 是否多选 |
| `searchable` | `boolean` | `true` | 是否可搜索 |
| `defaultExpandAll` | `boolean` | `true` | 默认展开全部 |
| `disabled` | `boolean` | `false` | 是否禁用 |
| `placeholder` | `string` | `'请选择组织'` | 占位提示 |
| `status` | `1 \| 0` | - | 按状态过滤 |

### Emits

| 事件 | 参数 | 说明 |
| --- | --- | --- |
| `update:modelValue` | `(value)` | v-model 同步 |
| `change` | `(value, nodes)` | 值变化时触发 |
| `dropdownVisibleChange` | `(visible: boolean)` | 下拉显示/隐藏 |
| `search` | `(value: string)` | 搜索值变化 |

## useOrgTree Composable

直接使用 composable，自行渲染树或扩展业务逻辑：

```ts
import { useOrgTree } from '@/components/OrgTree'

const { treeData, loading, error, fetchTree, findNode, getPath } = useOrgTree()

// 首次加载（命中缓存直接返回）
await fetchTree()

// 强制刷新（跳过缓存）
await fetchTree({ force: true })

// 仅加载启用状态的组织
await fetchTree({ status: 1 })

// 查找节点
const node = findNode(123)

// 获取从根到节点的路径
const path = getPath(123) // [root, ..., target]
```

### 缓存策略

- 模块级单例缓存，所有 `useOrgTree` 实例共享
- TTL 5 分钟，超时后下次调用自动刷新
- 通过 `clearOrgTreeCache()` 可手动清空缓存

## 工具函数

`OrgTree/index.ts` 同时导出以下工具函数，可独立使用：

| 函数 | 签名 | 说明 |
| --- | --- | --- |
| `filterTree` | `(nodes, keyword) => OrgTreeNode[]` | 关键字过滤树（保留匹配项与祖先链） |
| `filterByOrgType` | `(nodes, orgType) => OrgTreeNode[]` | 按 orgType 过滤树 |
| `findNodeInTree` | `(nodes, id) => OrgTreeNode \| undefined` | 递归查找节点 |
| `findPathInTree` | `(nodes, id) => OrgTreeNode[]` | 递归查找路径 |
| `collectAllKeys` | `(nodes) => number[]` | 收集所有节点 key |
| `clearOrgTreeCache` | `() => void` | 清空组织树缓存 |

## 类型定义

```ts
import type {
  OrgTreeNode,
  OrgTreeProps,
  OrgTreeSelectProps,
  OrgTreeExpose,
  OrgType,
  OrgTypeFilter
} from '@/components/OrgTree'
```

## 嵌入 FormModal

`OrgTreeSelect` 与 `FormModal` 的 `treeSelect` 字段类型兼容。两种使用方式：

### 方式一：通过 `asyncOptions` 加载组织树

```ts
import type { FormField } from '@/components/FormModal'

const fields: FormField[] = [
  {
    field: 'orgId',
    label: '所属组织',
    type: 'treeSelect',
    required: true,
    asyncOptions: async () => {
      const { useOrgTree } = await import('@/components/OrgTree')
      const { fetchTree, findNode } = useOrgTree()
      const tree = await fetchTree()
      // 转换为 treeSelect 选项格式
      const transform = (nodes: any[]): any[] =>
        nodes.map((n) => ({
          label: n.orgName,
          value: n.id,
          children: n.children?.length ? transform(n.children) : undefined
        }))
      return transform(tree)
    }
  }
]
```

### 方式二：通过 `form-extra` slot 嵌入 OrgTreeSelect

```vue
<FormModal v-model:visible="visible" v-model:data="formData" :fields="fields">
  <template #form-extra="{ data }">
    <a-form-item label="所属组织" name="orgId">
      <OrgTreeSelect v-model="data.orgId" />
    </a-form-item>
  </template>
</FormModal>
```

## 注意事项

1. **缓存共享**：`OrgTree` 与 `OrgTreeSelect` 共享同一份模块级缓存，多个组件实例不会重复请求 API。
2. **status 变化自动刷新**：当 `status` prop 变化时，组件会自动重新拉取（强制跳过缓存）。
3. **搜索高亮**：搜索时匹配关键字会以 `<mark>` 高亮显示，仅匹配 `orgName`，不匹配 `orgCode`。
4. **过滤规则**：搜索/类型过滤时，匹配节点保留完整子树；不匹配但子节点匹配的父节点仅挂载匹配子分支。
5. **v-model 类型**：单选模式返回 `number`，多选模式返回 `number[]`；清空时返回 `undefined`。
6. **lazyLoad 预留**：`listOrgTree` API 返回完整树，`lazyLoad` prop 为预留字段，当前为 no-op。
