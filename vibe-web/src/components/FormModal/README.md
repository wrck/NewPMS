# FormModal 通用表单弹窗组件

> spec 阶段三 Task 13：抽取通用表单弹窗组件，基于字段配置自动渲染表单，支持 13 种字段类型、字段联动、异步选项加载、表单校验，可一处声明多处复用。

## 目录

- [基础用法](#基础用法)
- [Props](#props)
- [Emits](#emits)
- [Expose](#expose)
- [支持的字段类型](#支持的字段类型)
- [字段联动](#字段联动)
- [异步选项加载](#异步选项加载)
- [表单校验](#表单校验)
- [Slots](#slots)
- [注意事项](#注意事项)

## 基础用法

```vue
<template>
  <a-button type="primary" @click="visible = true">新增</a-button>
  <FormModal
    v-model:visible="visible"
    v-model:data="formData"
    title="新增用户"
    :fields="fields"
    :loading="submitLoading"
    @submit="handleSubmit"
    @cancel="handleCancel"
  />
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import FormModal from '@/components/FormModal/index.vue'
import type { FormField } from '@/components/FormModal/types'

const visible = ref(false)
const submitLoading = ref(false)
const formData = reactive<Record<string, any>>({
  userName: '',
  realName: '',
  status: 1,
  roleCodes: []
})

const fields: FormField[] = [
  {
    field: 'userName',
    label: '用户名',
    type: 'input',
    required: true,
    placeholder: '请输入登录用户名'
  },
  {
    field: 'realName',
    label: '姓名',
    type: 'input',
    required: true
  },
  {
    field: 'phone',
    label: '手机',
    type: 'input',
    rules: [{ pattern: /^1\d{10}$/, message: '请输入正确的手机号' }]
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    defaultValue: 1,
    options: [
      { label: '启用', value: 1 },
      { label: '禁用', value: 0 }
    ]
  },
  {
    field: 'roleCodes',
    label: '角色',
    type: 'select',
    multiple: true,
    span: 24,
    options: [
      { label: '管理员', value: 'ADMIN' },
      { label: '工程师', value: 'ENGINEER' }
    ]
  }
]

async function handleSubmit(payload: Record<string, any>) {
  submitLoading.value = true
  try {
    await createUser(payload)
    message.success('新增成功')
    visible.value = false
  } finally {
    submitLoading.value = false
  }
}

function handleCancel() {
  // 取消时父组件可执行清理逻辑
}
</script>
```

## Props

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `visible` | `boolean` | 必填 | 是否显示，支持 `v-model:visible` |
| `title` | `string` | 必填 | 弹窗标题 |
| `fields` | `FormField[]` | 必填 | 字段定义，详见 [FormField](#formfield-字段说明) |
| `data` | `Record<string, any>` | 必填 | 表单数据，支持 `v-model:data` |
| `loading` | `boolean` | `false` | 提交按钮 loading 状态 |
| `width` | `number \| string` | `600` | 弹窗宽度 |
| `span` | `number` | `12` | 表单栅格跨度（24 栅格中的占比，12 表示 2 列） |
| `labelCol` | `number` | `6` | label 标签 span |
| `wrapperCol` | `number` | `18` | wrapper span |

## Emits

| 事件 | 参数 | 说明 |
| --- | --- | --- |
| `update:visible` | `(visible: boolean)` | 关闭弹窗 |
| `update:data` | `(data: Record<string, any>)` | 表单数据变化 |
| `submit` | `(data: Record<string, any>)` | 点击确定且通过校验后触发 |
| `cancel` | - | 点击取消按钮触发 |

## Expose

通过 `ref` 可调用以下方法：

```ts
import type { FormModalExpose } from '@/components/FormModal/types'

const formRef = ref<FormModalExpose>()

// 手动触发校验
await formRef.value?.validate()

// 重置表单到初始状态
formRef.value?.resetFields()

// 设置字段值
formRef.value?.setFieldValue('userName', 'alice')

// 获取表单数据（深拷贝）
const data = formRef.value?.getFormData()
```

## 支持的字段类型

| 类型 | 控件 | 说明 |
| --- | --- | --- |
| `input` | `a-input` | 文本输入 |
| `inputNumber` | `a-input-number` | 数字输入，支持 `max/min/precision` |
| `inputPassword` | `a-input-password` | 密码输入 |
| `textarea` | `a-textarea` | 多行文本，默认 3 行 |
| `select` | `a-select` | 下拉选择，支持 `multiple/options` |
| `date` | `a-date-picker` | 日期选择，支持 `showTime/valueFormat` |
| `dateRange` | `a-range-picker` | 日期范围 |
| `switch` | `a-switch` | 开关 |
| `radio` | `a-radio-group` | 单选组，需 `options` |
| `checkbox` | `a-checkbox-group` | 复选组，需 `options` |
| `cascader` | `a-cascader` | 级联选择，支持 `multiple` |
| `treeSelect` | `a-tree-select` | 树选择，支持 `multiple/treeData` |
| `upload` | `a-upload-dragger` | 拖拽上传，支持 `accept/maxSize/maxCount` |

## FormField 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `field` | `string` | 字段名，支持 `a.b` 嵌套 |
| `label` | `string` | 字段标签 |
| `type` | `FormFieldType` | 控件类型（13 种） |
| `options` | `FormFieldOption[]` | 选项列表 |
| `placeholder` | `string` | 占位提示 |
| `defaultValue` | `any` | 默认值 |
| `required` | `boolean` | 是否必填 |
| `rules` | `any[]` | 自定义校验规则（追加在 required 之后） |
| `span` | `number` | 字段栅格跨度，覆盖组件级 `span` |
| `readonly` | `boolean` | 只读（input 系列） |
| `disabled` | `boolean` | 禁用 |
| `showTime` | `boolean` | 日期是否显示时间 |
| `valueFormat` | `string` | 日期值格式 |
| `multiple` | `boolean` | 是否多选（select/cascader/treeSelect） |
| `treeData` | `any[]` | treeSelect 树数据（与 options 二选一） |
| `max` / `min` / `precision` | `number` | inputNumber 数字控制 |
| `maxLength` | `number` | input/textarea 最大长度 |
| `accept` | `string` | upload 文件类型，如 `.jpg,.png` |
| `maxSize` | `number` | upload 单文件大小限制（MB） |
| `maxCount` | `number` | upload 最大文件数 |
| `visibleWhen` | `{ field, value }` | 字段联动：当指定字段等于某值时显示 |
| `requiredWhen` | `{ field, value }` | 字段联动：当指定字段等于某值时必填 |
| `disabledWhen` | `{ field, value }` | 字段联动：当指定字段等于某值时禁用 |
| `optionsWhen` | `{ field, value, options }` | 字段联动：当指定字段等于某值时切换选项 |
| `asyncOptions` | `() => Promise<Array<{label, value}>>` | 异步加载选项 |

## 字段联动

通过 4 种联动规则，可覆盖常见联动场景：

```ts
const fields: FormField[] = [
  {
    field: 'type',
    label: '类型',
    type: 'radio',
    defaultValue: 'personal',
    options: [
      { label: '个人', value: 'personal' },
      { label: '企业', value: 'enterprise' }
    ]
  },
  // 个人时显示身份证号
  {
    field: 'idCard',
    label: '身份证号',
    type: 'input',
    visibleWhen: { field: 'type', value: 'personal' },
    requiredWhen: { field: 'type', value: 'personal' }
  },
  // 企业时显示统一社会信用代码
  {
    field: 'creditCode',
    label: '信用代码',
    type: 'input',
    visibleWhen: { field: 'type', value: 'enterprise' },
    requiredWhen: { field: 'type', value: 'enterprise' }
  },
  // 类型变化时切换城市选项
  {
    field: 'city',
    label: '城市',
    type: 'select',
    optionsWhen: {
      field: 'type',
      value: 'personal',
      options: [
        { label: '北京', value: 'bj' },
        { label: '上海', value: 'sh' }
      ]
    }
    // 缺省时使用 options
  }
]
```

`value` 字段支持数组，命中其中任意一个即视为满足：

```ts
visibleWhen: { field: 'status', value: [1, 2, 3] }
```

## 异步选项加载

为字段声明 `asyncOptions`，组件在 `onMounted` 时自动调用，加载结果回填到该字段的选项中：

```ts
const fields: FormField[] = [
  {
    field: 'orgId',
    label: '组织',
    type: 'treeSelect',
    asyncOptions: async () => {
      const tree = await getOrgTree()
      return tree // 返回 [{label, value, children}]
    }
  }
]
```

异步选项优先级高于 `options` 与 `optionsWhen`。

## 表单校验

校验规则自动生成：

1. 若 `required: true` 或 `requiredWhen` 命中，自动追加 `{ required: true, message: '请输入/请选择X' }`
2. `rules` 字段中的规则追加在 required 之后
3. 点击「确定」按钮调用 `a-form.validate()`，校验通过后触发 `submit` 事件；失败则 `message.warning('请完善表单信息')`

```ts
const fields: FormField[] = [
  {
    field: 'email',
    label: '邮箱',
    type: 'input',
    rules: [
      { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
      { max: 50, message: '邮箱不能超过 50 个字符' }
    ]
  }
]
```

## Slots

| 插槽 | 作用域参数 | 说明 |
| --- | --- | --- |
| `form-extra` | `{ data }` | 表单底部追加自定义字段 |
| `footer` | - | 替换底部按钮区（默认为 取消 / 确定） |

```vue
<FormModal ...>
  <template #form-extra="{ data }">
    <a-form-item label="备注">
      <a-input v-model:value="data.remark" />
    </a-form-item>
  </template>
</FormModal>
```

## 注意事项

1. **v-model:data 行为**：组件内部维护一份 reactive 副本，每次字段值变化时通过 `update:data` 同步到父组件。父组件修改 `data` 不会覆盖用户输入（仅同步缺失字段）。
2. **关闭即清空**：弹窗关闭后（`after-close`）会清空内部数据与校验状态，下次打开会重新初始化。
3. **嵌套字段**：`field: 'user.name'` 自动按路径读写，无需额外配置。
4. **upload 字段**：使用 `a-upload-dragger`，通过 `v-model:file-list` 双向绑定。`beforeUpload` 内置 `maxSize/accept` 校验，校验失败返回 `false` 阻止上传。
5. **联动值匹配**：`visibleWhen/requiredWhen/disabledWhen/optionsWhen` 的 `value` 字段支持数组，命中其中任意一个即视为满足。
6. **校验时机**：`required` 自动生成的规则 trigger 为 `['blur', 'change']`，可覆盖大多数场景。
7. **与 CrudTable 配合**：FormModal 是 CrudTable 内部表单的独立抽取版本，两者可独立使用；CrudTable 的 `columns[].formType` 与 FormModal 的 `fields[].type` 字段类型完全一致。
