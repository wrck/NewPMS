# CrudTable 通用 CRUD 表格组件

> spec 阶段三 Task 12：抽取通用 CRUD 表格组件，封装搜索表单 + 分页 + 新增/编辑/删除弹窗 + 行选择 + 自定义 actions，可一处声明多处复用，业务页面代码行数减少 60% 以上。

## 目录

- [基础用法](#基础用法)
- [Props](#props)
- [Emits](#emits)
- [Expose](#expose)
- [列定义示例](#列定义示例)
- [搜索字段示例](#搜索字段示例)
- [自定义操作按钮](#自定义操作按钮)
- [行选择与批量删除](#行选择与批量删除)
- [权限控制](#权限控制)
- [Slots](#slots)
- [注意事项](#注意事项)

## 基础用法

```vue
<template>
  <CrudTable
    :columns="columns"
    :search-fields="searchFields"
    :api-func="{ page: pageUsers, create: createUser, update: updateUser, delete: deleteUser }"
    permission-prefix="system:user"
    title="用户管理"
    :scroll-x="1500"
    @saved="onSaved"
  />
</template>

<script setup lang="ts">
import CrudTable from '@/components/CrudTable/index.vue'
import type { CrudColumn, SearchField } from '@/components/CrudTable/types'
import { pageUsers, createUser, updateUser, deleteUser } from '@/api/system'

const columns: CrudColumn[] = [
  { field: 'userName', title: '用户名', width: 130, formType: 'input', formRules: [{ required: true, message: '请输入用户名' }], readonly: true },
  { field: 'realName', title: '姓名', width: 110, formType: 'input', formRules: [{ required: true, message: '请输入姓名' }] },
  { field: 'phone', title: '手机', width: 130, formType: 'input' },
  { field: 'status', title: '状态', width: 90, formType: 'select', valueEnum: { 1: { text: '启用', status: 'success' }, 0: { text: '禁用', status: 'error' } }, formDefaultValue: 1 }
]

const searchFields: SearchField[] = [
  { field: 'userName', label: '用户名', type: 'input', placeholder: '请输入用户名' },
  { field: 'status', label: '状态', type: 'select', options: [{ label: '启用', value: 1 }, { label: '禁用', value: 0 }] }
]

function onSaved(type: 'create' | 'update') {
  console.log('saved:', type)
}
</script>
```

## Props

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `columns` | `CrudColumn[]` | 必填 | 列定义，详见 [CrudColumn](#列定义示例) |
| `searchFields` | `SearchField[]` | `[]` | 搜索字段定义；为空时不渲染搜索表单 |
| `apiFunc` | `CrudApiFunc` | 必填 | `{ page, create, update, delete }` API 集合 |
| `modelBinding` | `{ idField?, idLabel? }` | `{ idField: 'id' }` | 主键字段名及提示标签 |
| `rowSelection` | `boolean` | `false` | 是否启用行选择 |
| `actions` | `CrudAction[]` | `[]` | 自定义行级操作按钮 |
| `permissionPrefix` | `string` | - | 权限前缀（如 `system:user`），自动与 `create/update/delete` 拼接 |
| `title` | `string` | `''` | 表格标题 |
| `description` | `string` | `''` | 表格描述 |
| `pageSize` | `number` | `10` | 每页条数 |
| `scrollX` | `number \| string` | - | 表格横向滚动宽度 |
| `formWidth` | `number \| string` | `640` | 新增/编辑弹窗宽度 |
| `createText` | `string` | `'新增'` | 新增按钮文案 |
| `searchCollapsed` | `boolean` | `true` | 搜索表单默认折叠状态（搜索字段 > 3 个时生效） |
| `rowKey` | `string` | 与 `idField` 一致 | 表格 rowKey |

## Emits

| 事件 | 参数 | 说明 |
| --- | --- | --- |
| `selectionChange` | `(rows: any[])` | 行选择变化 |
| `saved` | `(type: 'create' \| 'update', record: any)` | 新增/编辑成功后触发 |
| `deleted` | `(id: any)` | 删除成功后触发 |
| `search` | `(query: Record<string, any>)` | 搜索触发 |

## Expose

通过 `ref` 可调用以下方法：

```ts
interface CrudTableExpose {
  refresh(): Promise<void>          // 主动刷新当前页
  reset(): Promise<void>            // 重置搜索条件并刷新
  openCreate(): void                // 打开新增弹窗
  openEdit(record: any): void       // 打开编辑弹窗
  getSelectedRows(): any[]          // 获取当前选中行
  clearSelected(): void             // 清空选中
}
```

使用示例：

```vue
<template>
  <CrudTable ref="crudRef" ... />
  <a-button @click="crudRef?.refresh()">刷新</a-button>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type CrudTable from '@/components/CrudTable/index.vue'

const crudRef = ref<InstanceType<typeof CrudTable>>()
</script>
```

## 列定义示例

```ts
import type { CrudColumn } from '@/components/CrudTable/types'

const columns: CrudColumn[] = [
  // 文本列 + 表单 input
  {
    field: 'userName',
    title: '用户名',
    width: 130,
    align: 'left',
    sortable: true,
    formType: 'input',
    formRules: [{ required: true, message: '请输入用户名' }],
    readonly: true,           // 编辑时禁用
    formSpan: 12
  },
  // 枚举列：自动渲染 Tag
  {
    field: 'status',
    title: '状态',
    width: 90,
    align: 'center',
    valueEnum: {
      '1': { text: '启用', status: 'success' },
      '0': { text: '禁用', status: 'error' }
    },
    formType: 'select',
    formDefaultValue: 1
  },
  // 格式化列
  {
    field: 'amount',
    title: '金额',
    width: 120,
    format: (val) => `¥${val.toFixed(2)}`
  },
  // 只在表单中出现（不在表格中显示）
  {
    field: 'password',
    title: '密码',
    hideInTable: true,
    formType: 'inputPassword',
    formRules: [{ required: true, min: 6, message: '密码至少 6 位' }]
  },
  // 只在表格中显示（不在表单中出现）
  {
    field: 'lastLoginAt',
    title: '最后登录',
    width: 160,
    hideInForm: true
  }
]
```

### `CrudColumn` 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `field` | `string` | 字段名，支持 `a.b` 嵌套 |
| `title` | `string` | 列标题 |
| `width` | `number \| string` | 列宽 |
| `align` | `'left' \| 'center' \| 'right'` | 对齐 |
| `sortable` | `boolean` | 是否可排序 |
| `format` | `(value, record) => string` | 文本格式化 |
| `valueEnum` | `Record<string, { text, status?, disabled? }>` | 枚举映射，自动渲染 Tag |
| `formType` | `FormFieldType` | 表单控件类型，未声明则不在表单中 |
| `formRules` | `any[]` | 表单校验规则（同 antd Form rules） |
| `formOptions` | `any[]` | select/radio/checkbox 选项 |
| `formSpan` | `number` | 表单栅格 span，默认 12（共 24 栅格） |
| `formDefaultValue` | `any` | 表单默认值 |
| `hideInForm` | `boolean` | 是否在表单中隐藏 |
| `hideInTable` | `boolean` | 是否在表格中隐藏 |
| `readonly` | `boolean` | 编辑时只读 |
| `fixed` | `'left' \| 'right'` | 列固定 |
| `ellipsis` | `boolean` | 是否省略号显示，默认 `true` |

支持的 `formType`：
- `input` / `inputNumber` / `inputPassword` / `textarea`
- `select` / `radio` / `checkbox` / `cascader` / `treeSelect`
- `date` / `dateRange` / `switch` / `upload`

## 搜索字段示例

```ts
import type { SearchField } from '@/components/CrudTable/types'

const searchFields: SearchField[] = [
  { field: 'userName', label: '用户名', type: 'input', placeholder: '请输入' },
  {
    field: 'status', label: '状态', type: 'select',
    options: [{ label: '启用', value: 1 }, { label: '禁用', value: 0 }],
    defaultValue: 1
  },
  { field: 'createdAt', label: '创建时间', type: 'dateRange', width: 240 },
  { field: 'orgId', label: '组织', type: 'treeSelect', options: orgTreeData }
]
```

字段超过 3 个时，第 4 个起会被折叠，用户可点击「展开/折叠」切换。

## 自定义操作按钮

通过 `actions` 注入行级按钮，可选内置「编辑」「删除」之外的功能：

```ts
import type { CrudAction } from '@/components/CrudTable/types'
import { KeyOutlined } from '@ant-design/icons-vue'

const actions: CrudAction[] = [
  {
    label: '重置密码',
    icon: KeyOutlined,
    onClick: (record) => {
      Modal.confirm({ /* ... */ })
    },
    visible: (record) => record.status === 1
  },
  { divider: true } as CrudAction,           // 分隔符
  {
    label: '查看日志',
    onClick: (record) => router.push(`/logs?userId=${record.id}`)
  }
]
```

每行最终操作列顺序：`编辑 → 自定义按钮... → 删除`，按钮支持 `danger`、`divider` 字段。

## 行选择与批量删除

```vue
<template>
  <CrudTable
    :row-selection="true"
    @selection-change="onSelectionChange"
    ...
  />
</template>
```

启用后：
- 表格首列出现复选框
- 选中行数 > 0 时，工具栏出现「批量删除（N）」按钮
- 通过 `ref.getSelectedRows()` / `ref.clearSelected()` 程序化访问
- 内置「批量删除」会调用 `apiFunc.delete(id)` 逐个删除（容错处理）

## 权限控制

传入 `permissionPrefix` 后：
- 「新增」按钮：校验 `${prefix}:create`
- 「编辑」按钮：校验 `${prefix}:update`
- 「删除」按钮：校验 `${prefix}:delete`
- 自定义按钮：通过 `CrudAction.permission` 字段（如 `'export'`）拼接为 `${prefix}:export` 校验

未传入 `permissionPrefix` 时，所有按钮默认可见。权限校验委托给 `useUserStore().hasPermission`，超管直接放行。

## Slots

| 插槽 | 作用域参数 | 说明 |
| --- | --- | --- |
| `toolbar` | - | 工具栏右侧扩展按钮 |
| `form-extra` | `{ model, isEdit }` | 弹窗表单底部追加自定义字段 |
| `empty` | - | 表格空数据状态自定义 |

```vue
<CrudTable ...>
  <template #toolbar>
    <a-button @click="exportExcel">导出</a-button>
  </template>
  <template #form-extra="{ model, isEdit }">
    <a-form-item label="备注">
      <a-input v-model:value="model.remark" />
    </a-form-item>
  </template>
</CrudTable>
```

## 注意事项

1. **`apiFunc.page` 返回值** 必须符合 `PageResult` 结构：`{ records, total, page, size, pages }`。`records` 为空时不会报错，仅渲染空状态。
2. **`apiFunc.delete`** 既支持单删除也支持批量删除（批量时通过 `Promise.all` 并发调用，单个失败不影响其他）。
3. **错误处理**：组件内部已统一 `try/catch`，错误信息由 `@/utils/request` 拦截器统一弹出 `message.error`，业务无需重复处理。
4. **嵌套字段**：`field: 'user.name'` 会自动按路径读写，无需额外配置。
5. **表单校验**：`formRules` 直接透传给 `a-form-item` 的 `rules` 属性，校验失败会 `message.warning('请完善表单信息')` 并阻止提交。
6. **响应式重渲染**：父组件异步加载 `options` 时，通过 `watch(searchFields, deep:true)` 自动同步默认值。
7. **暴露给父组件的方法** 通过 `defineExpose` 暴露，可用 `ref` 调用。
