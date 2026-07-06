# ImportExport 导入导出按钮组件

> spec 阶段三 Task 20：抽取通用导入导出按钮组件，一键集成 Excel 导出、Excel 导入（含结果弹窗）、模板下载三大功能，业务页面无需重复编写 fetch + Blob 下载逻辑。

## 目录

- [基础用法](#基础用法)
- [Props](#props)
- [Emits](#emits)
- [Expose](#expose)
- [后端接口约定](#后端接口约定)
- [导入结果结构](#导入结果结构)
- [进阶用法](#进阶用法)
- [注意事项](#注意事项)

## 基础用法

```vue
<template>
  <ImportExport
    export-api="/api/v1/system/users/export"
    import-api="/api/v1/system/users/import"
    template-url="/api/v1/system/users/template"
    @export-success="onExportSuccess"
    @import-success="onImportSuccess"
    @refresh="loadList"
  />
</template>

<script setup lang="ts">
import { ImportExport } from '@/components/ImportExport'
import type { ImportResult } from '@/components/ImportExport'

function onExportSuccess() {
  console.log('导出完成')
}
function onImportSuccess(result: ImportResult) {
  console.log('导入结果:', result)
}
function loadList() {
  // 重新拉取列表数据
}
</script>
```

组件会自动渲染三个按钮：

```
[导出] [导入] [下载模板]
```

## Props

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `exportApi` | `string` | - | 导出接口 URL（POST，返回 Blob） |
| `importApi` | `string` | - | 导入接口 URL（POST，multipart/form-data） |
| `templateUrl` | `string` | - | 模板下载 URL（GET，返回 Blob） |
| `exportText` | `string` | `'导出'` | 导出按钮文案 |
| `importText` | `string` | `'导入'` | 导入按钮文案 |
| `templateText` | `string` | `'下载模板'` | 模板下载按钮文案 |
| `showExport` | `boolean` | `true` | 是否显示导出按钮 |
| `showImport` | `boolean` | `true` | 是否显示导入按钮 |
| `showTemplate` | `boolean` | `true` | 是否显示模板下载按钮 |
| `size` | `'small' \| 'middle' \| 'large'` | `'middle'` | 按钮尺寸（透传 a-button） |
| `exportParams` | `Record<string, unknown>` | `{}` | 导出参数（POST JSON body） |
| `importData` | `Record<string, unknown>` | `{}` | 导入附加数据（FormData 字段） |
| `exportFileName` | `string` | `导出_<ts>.xlsx` | 自定义导出文件名（不传则从 Content-Disposition 解析） |
| `exportDisabled` | `boolean` | `false` | 禁用导出按钮 |
| `full` | `boolean` | `false` | 完整模式（同时显示三个按钮，等价于三个 show\* 都为 true） |
| `maxSizeMb` | `number` | `10` | 上传文件大小上限（MB） |
| `autoRefresh` | `boolean` | `false` | 导入成功后自动 emit `refresh` 事件 |

## Emits

| 事件 | 参数 | 说明 |
| --- | --- | --- |
| `export-success` | - | 导出成功（文件已下载） |
| `export-error` | `(error: unknown)` | 导出失败 |
| `import-success` | `(result: ImportResult)` | 导入完成（无论成败，返回结果） |
| `import-error` | `(error: unknown)` | 导入失败（网络/后端异常） |
| `template-downloaded` | - | 模板下载成功 |
| `template-error` | `(error: unknown)` | 模板下载失败 |
| `refresh` | - | 导入成功后请求父组件刷新数据（`autoRefresh=true` 时触发） |

## Expose

通过 `ref` 可调用以下方法：

```ts
interface ImportExportExpose {
  export(): Promise<void>          // 主动触发导出
  downloadTemplate(): Promise<void> // 主动下载模板
  closeResult(): void               // 关闭导入结果弹窗
}
```

使用示例：

```vue
<template>
  <ImportExport ref="ieRef" export-api="..." />
  <a-button @click="ieRef?.export()">手动导出</a-button>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ImportExport from '@/components/ImportExport/index.vue'

const ieRef = ref<InstanceType<typeof ImportExport>>()
</script>
```

## 后端接口约定

### 导出接口（`exportApi`）

- 方法：`POST`
- 请求头：`Content-Type: application/json`、`Authorization: Bearer <token>`
- 请求体：`exportParams` 序列化后的 JSON
- 响应：`Blob`（Excel 二进制），响应头推荐带 `Content-Disposition: attachment; filename*=UTF-8''<URL编码文件名>.xlsx`
- 失败响应：返回 `Result` JSON（`{ code: <非200>, message }`），组件会读取 message 并报错

### 导入接口（`importApi`）

- 方法：`POST`
- 请求头：`Authorization: Bearer <token>`（不要手动设 Content-Type，由 FormData 自动设置 boundary）
- 请求体：`multipart/form-data`
  - `file`：Excel 文件
  - 其余字段来自 `importData`（值会通过 `String(...)` 序列化）
- 响应：`Result<ImportResultPayload>`
  ```json
  {
    "code": 200,
    "message": "ok",
    "data": {
      "successCount": 95,
      "failCount": 5,
      "totalCount": 100,
      "duration": 1234,
      "errors": [
        { "row": 3, "field": "phone", "message": "手机号格式错误", "value": "abc" }
      ]
    }
  }
  ```

### 模板下载接口（`templateUrl`）

- 方法：`GET`
- 请求头：`Authorization: Bearer <token>`
- 响应：`Blob`（Excel 二进制），响应头推荐带 `Content-Disposition`

## 导入结果结构

```ts
interface ImportResult {
  success: boolean         // 整体是否成功（failCount===0）
  successCount: number
  failCount: number
  totalCount: number
  errors?: ImportError[]
  duration?: number        // ms
}

interface ImportError {
  row: number              // 行号（1-based）
  field?: string           // 字段名（可选）
  message: string          // 错误描述
  value?: unknown          // 错误值（可选）
}
```

弹窗会展示：
1. 成功/失败/总行数
2. 耗时（如有）
3. 错误详情列表（最多展示 100 条，分页每页 10 条）

## 进阶用法

### 仅导出（无导入）

```vue
<ImportExport
  export-api="/api/v1/users/export"
  :show-import="false"
  :show-template="false"
  :export-params="{ status: 1, keyword: 'foo' }"
  export-file-name="活跃用户.xlsx"
/>
```

### 带附加参数的导入

```vue
<ImportExport
  import-api="/api/v1/users/import"
  :import-data="{ tenantId: 'T001', overwrite: 'true' }"
  :auto-refresh="true"
  @refresh="loadList"
/>
```

### 嵌入 CrudTable 工具栏

```vue
<CrudTable ...>
  <template #toolbar>
    <ImportExport
      export-api="/api/v1/users/export"
      import-api="/api/v1/users/import"
      template-url="/api/v1/users/template"
      :auto-refresh="true"
      @refresh="crudRef?.refresh()"
    />
  </template>
</CrudTable>
```

### 自定义按钮文案

```vue
<ImportExport
  export-text="导出用户"
  import-text="批量导入"
  template-text="下载导入模板"
  ...
/>
```

## 注意事项

1. **Token 自动携带**：组件从 `useUserStore().token` 读取，已包含 Pinia 自动解包。无需手动透传。
2. **跨域**：如后端接口跨域，需后端在响应头中暴露 `Content-Disposition`：
   ```
   Access-Control-Expose-Headers: Content-Disposition
   ```
   否则文件名解析会回退到 `exportFileName` 或默认值。
3. **文件类型限制**：默认仅允许 `.xlsx` / `.xls`，通过 `accept=".xlsx,.xls"` 与运行时双重校验。
4. **错误处理**：组件内部已统一 `try/catch`，错误信息通过 `message.error` 弹出，业务可通过 `export-error` / `import-error` / `template-error` 事件进一步处理。
5. **导入结果弹窗**：组件自带一个 `a-modal`，无论后端返回成功还是部分失败都会弹出，展示详细统计。如需自定义展示，可监听 `import-success` 事件并自行渲染。
6. **上传方式**：组件采用「自定义上传」策略（`before-upload` 返回 `false` 阻止 antd 默认上传），由组件内部 `fetch` 直接调用后端，避免 antd `action` 的额外请求。
7. **响应式 `exportParams`**：每次点击导出时都会读取最新的 `props.exportParams`，因此父组件可以动态修改查询条件。
