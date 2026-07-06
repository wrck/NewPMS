# RichEditor 富文本编辑器组件

> spec 阶段三 Task 15：基于 wangEditor 5 封装通用富文本编辑器，支持 v-model 双向绑定、自定义工具栏、只读模式与图片直传 MinIO。

## 目录

- [基础用法](#基础用法)
- [Props](#props)
- [Emits](#emits)
- [Expose](#expose)
- [输出格式](#输出格式)
- [图片上传](#图片上传)
- [只读模式](#只读模式)
- [与 FileUpload 组件协同](#与-fileupload-组件协同)
- [注意事项](#注意事项)

## 基础用法

```vue
<template>
  <a-form-item label="正文" name="content">
    <RichEditor v-model="form.content" :height="500" />
  </a-form-item>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import RichEditor from '@/components/RichEditor/index.vue'

const form = reactive({
  content: '<p>请输入公告正文...</p>'
})
</script>
```

> 由于 `vite.config.ts` 已配置 `unplugin-vue-components` 自动注册 `src/components` 下的组件，模板中也可直接使用 `<RichEditor />` 而无需 import。

## Props

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `modelValue` | `string` | 必填 | v-model 绑定值（HTML 或纯文本，取决于 outputFormat） |
| `height` | `number` | `400` | 编辑器总高度（px），工具栏占 50px，编辑区占剩余 |
| `mode` | `'default' \| 'simple'` | `'default'` | wangEditor 工具栏模式 |
| `readonly` | `boolean` | `false` | 是否只读（隐藏工具栏） |
| `outputFormat` | `'html' \| 'text'` | `'html'` | 输出格式：HTML / 纯文本 |
| `placeholder` | `string` | `'请输入内容...'` | 占位提示文字 |
| `excludeKeys` | `string[]` | `['fullScreen', 'group-video']` | 工具栏排除的按钮 key 列表 |
| `uploadBucket` | `string` | `'documents'` | 图片上传目标 bucket |
| `maxImageSize` | `number` | `10` | 单张图片大小上限（MB） |

## Emits

| 事件 | 参数 | 说明 |
| --- | --- | --- |
| `update:modelValue` | `(value: string)` | v-model 更新 |
| `change` | `(value: string)` | 内容变化（与 update:modelValue 同步触发，便于监听） |
| `created` | `(editor: IDomEditor)` | 编辑器实例创建完成 |
| `upload-success` | `({ url, alt, file })` | 图片上传成功 |
| `upload-error` | `({ file, error })` | 图片上传失败 |

## Expose

通过 `ref` 可调用以下方法：

```ts
interface RichEditorExpose {
  getEditor(): IDomEditor | null    // 获取 wangEditor 实例
  getHtml(): string                  // 获取当前 HTML
  getText(): string                   // 获取当前纯文本
  clear(): void                       // 清空编辑器
  focus(): void                       // 聚焦
  blur(): void                        // 失焦
  insertImage(url: string, alt?: string, href?: string): void  // 手动插入图片
}
```

使用示例：

```vue
<template>
  <RichEditor ref="editorRef" v-model="content" />
  <a-button @click="editorRef?.clear()">清空</a-button>
  <a-button @click="showText">查看纯文本</a-button>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { ComponentInstance } from 'vue'
import RichEditor from '@/components/RichEditor/index.vue'

const editorRef = ref<ComponentInstance<typeof RichEditor>>()
const content = ref('')

function showText() {
  console.log(editorRef.value?.getText())
}
</script>
```

## 输出格式

通过 `outputFormat` 切换 v-model 的输出：

```vue
<!-- HTML 模式（默认）：输出 <p>hello</p> -->
<RichEditor v-model="html" output-format="html" />

<!-- 纯文本模式：输出 hello -->
<RichEditor v-model="text" output-format="text" />
```

> 注意：编辑器内部始终以 HTML 维护内容，仅 v-model 输出时转换为纯文本。切换 outputFormat 不会重新解析已存在内容，建议在组件挂载时确定输出格式。

## 图片上传

### 默认上传逻辑

组件内置图片上传走后端 presign + PUT 直传 MinIO 流程：

1. 用户在工具栏点击「图片」按钮，选择本地图片
2. 前端校验文件类型（必须为 `image/*`）与大小（默认 ≤ 10MB）
3. 调用 `POST /api/v1/files/presign` 获取预签名 PUT URL 与 accessUrl
4. 前端 PUT 直传 MinIO
5. 上传成功后通过 `insertFn` 将图片插入编辑器

请求体：

```json
{
  "fileName": "demo.png",
  "contentType": "image/png",
  "fileSize": 102400,
  "bucket": "documents"
}
```

响应体（统一 `Result` 包装）：

```json
{
  "code": 200,
  "data": {
    "uploadUrl": "https://minio.example.com/documents/xxx?X-Amz-...",
    "accessUrl": "https://minio.example.com/documents/xxx",
    "objectKey": "xxx"
  }
}
```

### 自定义上传

如需自定义上传逻辑（如对接 OSS、七牛等），有两种方式：

1. 通过 `upload-success` / `upload-error` 事件监听上传结果
2. 通过 `insertImage` 方法外部插入图片 URL（绕过默认上传流程）

```ts
// 先上传到任意位置，再插入编辑器
editorRef.value?.insertImage('https://cdn.example.com/xxx.png', '描述')
```

## 只读模式

```vue
<RichEditor v-model="content" readonly :height="600" />
```

只读模式下：
- 工具栏隐藏
- 编辑区填满整个高度
- 内容不可编辑
- 边框颜色变浅

适合在详情页、预览页中展示富文本内容。

## 与 FileUpload 组件协同

Task 14 的 FileUpload 组件封装了通用文件上传逻辑，可通过两种方式与 RichEditor 协同：

### 方式 1：监听 FileUpload 上传成功，调用 insertImage

```vue
<template>
  <RichEditor ref="editorRef" v-model="content" />
  <FileUpload
    accept="image/*"
    :show-upload-list="false"
    @success="onImageUploaded"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import RichEditor from '@/components/RichEditor/index.vue'
import FileUpload from '@/components/FileUpload/index.vue'

const editorRef = ref()
const content = ref('')

function onImageUploaded({ url, name }) {
  editorRef.value?.insertImage(url, name)
}
</script>
```

### 方式 2：直接使用 RichEditor 内置图片上传

无需引入 FileUpload，工具栏自带的图片按钮已实现直传 MinIO。

## 注意事项

1. **依赖**：基于 `@wangeditor/editor@^5.1.23` 与 `@wangeditor/editor-for-vue@^5.1.12`，已写入 `package.json` dependencies。
2. **样式**：通过 `import '@wangeditor/editor/dist/css/style.css'` 全量引入，体积约 200KB（gzip 后 ~60KB）。
3. **响应式限制**：wangEditor 实例使用 `shallowRef` 持有，`readonly` / `placeholder` 等配置在编辑器初始化后变更不会动态生效，需重新挂载组件。如需动态切换 readonly，建议使用 `v-if` 重建组件。
4. **卸载清理**：组件 `onBeforeUnmount` 已自动调用 `editor.destroy()` 避免内存泄漏。
5. **token 续签**：图片上传直接走 `fetch` 而非 `@/utils/request` 拦截器，因此 token 续签响应头需由调用方处理；如使用默认上传流程，建议保证登录态在编辑期间有效。
6. **MinIO CORS**：PUT 直传 MinIO 需后端配置 CORS 允许浏览器跨域 PUT 请求。
7. **测试环境**：wangEditor 依赖真实 DOM API（如 `Selection`、`Range`），jsdom 下无法完整初始化，单元测试中通过 stub `Editor` / `Toolbar` 组件验证逻辑。
