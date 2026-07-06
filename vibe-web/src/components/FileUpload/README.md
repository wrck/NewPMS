# FileUpload 通用文件上传组件

> spec 阶段三 Task 14：抽取通用文件上传组件，基于 Ant Design Vue a-upload 封装，支持 MinIO 预签名直传、图片压缩、缩略图、水印、大文件分片上传、断点续传等能力。

## 目录

- [基础用法](#基础用法)
- [Props](#props)
- [Emits](#emits)
- [Expose](#expose)
- [功能特性](#功能特性)
- [后端接口约定](#后端接口约定)
- [注意事项](#注意事项)

## 基础用法

### 单文件上传

```vue
<template>
  <FileUpload
    v-model="avatar"
    accept="image/*"
    :max-size="2"
    list-type="picture-card"
    dir="user/avatar"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import FileUpload from '@/components/FileUpload'

const avatar = ref<string>('')
</script>
```

### 多文件上传（带水印）

```vue
<template>
  <FileUpload
    v-model="photos"
    multiple
    :max-count="9"
    accept="image/*"
    :max-size="20"
    :multipart-threshold="10"
    :chunk-size="5"
    :concurrency="3"
    watermark
    gps="深圳市南山区"
    dir="project/101/photos"
    @success="onSuccess"
    @error="onError"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import FileUpload from '@/components/FileUpload'
import { message } from 'ant-design-vue'

const photos = ref<string[]>([])

function onSuccess(file: any) {
  console.log('文件上传成功:', file.name, file.url)
}

function onError(file: any, error: Error) {
  message.error(`${file.name} 上传失败：${error.message}`)
}
</script>
```

### 文件列表模式

```vue
<template>
  <FileUpload
    v-model="attachments"
    multiple
    :max-count="5"
    :max-size="50"
    accept=".pdf,.doc,.docx,.xls,.xlsx"
    list-type="text"
    dir="finance/invoices"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import FileUpload from '@/components/FileUpload'

const attachments = ref<string[]>([])
</script>
```

## Props

| 属性                | 类型                            | 默认值          | 说明                                                                       |
| ------------------- | ------------------------------- | --------------- | -------------------------------------------------------------------------- |
| `modelValue`        | `string \| string[]`           | -               | v-model 绑定值，单文件为 string（URL），多文件为 string[]                  |
| `accept`            | `string`                        | `''`            | 接受的文件类型（MIME 或后缀，如 `image/*` / `.jpg,.png`）                  |
| `maxSize`           | `number`                        | `10`            | 单文件大小上限（MB）                                                       |
| `maxCount`          | `number`                        | `9`             | 最大文件数（多文件模式生效）                                               |
| `multiple`          | `boolean`                       | `false`         | 是否多选                                                                   |
| `watermark`         | `boolean`                       | `false`         | 是否对图片加水印（时间 + GPS + 上传人）                                    |
| `disabled`          | `boolean`                       | `false`         | 是否禁用                                                                   |
| `listType`          | `'picture-card' \| 'picture' \| 'text'` | `'picture-card'` | 列表展示样式                                                               |
| `dir`               | `string`                        | `'common'`      | MinIO Bucket 内目录                                                        |
| `multipartThreshold` | `number`                       | `10`            | 大文件分片阈值（MB，≥该值启用分片上传）                                   |
| `chunkSize`         | `number`                        | `5`             | 分片大小（MB）                                                             |
| `concurrency`       | `number`                        | `3`             | 分片上传并发数                                                             |
| `resume`            | `boolean`                       | `true`          | 是否启用断点续传（依赖 IndexedDB）                                         |
| `gps`               | `string`                        | `''`            | 自定义 GPS 位置（水印用，未传则水印中省略 GPS 行）                         |

## Emits

| 事件               | 回调参数                              | 说明                              |
| ------------------ | ------------------------------------- | --------------------------------- |
| `update:modelValue` | `(value: string \| string[])`        | v-model 更新                      |
| `change`           | `(fileList: FileItem[])`             | 文件列表变化（含状态、进度）      |
| `success`          | `(file: FileItem)`                   | 单个文件上传成功                  |
| `error`            | `(file: FileItem, error: Error)`     | 单个文件上传失败                  |
| `finish`           | `(fileList: FileItem[])`             | 所有文件上传完成                  |
| `remove`           | `(file: FileItem)`                   | 文件被移除                        |
| `preview`          | `(file: FileItem)`                   | 预览文件                          |

## Expose

| 方法          | 返回值                  | 说明                                   |
| ------------- | ----------------------- | -------------------------------------- |
| `getFileList` | `FileItem[]`            | 获取当前文件列表                        |
| `retryAll`    | `Promise<void>`         | 重试所有 error 状态的文件               |
| `clear`       | `void`                  | 清空文件列表（取消上传中的任务）        |
| `cancelAll`   | `void`                  | 取消所有上传中的任务（不删除文件项）    |

## 功能特性

### 1. MinIO 预签名直传

调用后端 `POST /api/v1/files/presign` 获取 MinIO 预签名 PUT URL，前端直接 PUT 文件二进制到 MinIO，无需经服务端转发，降低服务端带宽与内存压力。

### 2. 图片压缩

使用 Canvas API 对图片压缩：

- 压缩质量：85%
- 长边上限：2048px（按比例缩放）
- 输出格式：沿用原图类型

### 3. 缩略图

对图片自动生成 200x200 缩略图，用于 `picture-card` / `picture` 模式下的列表展示。

### 4. 水印

启用 `watermark` prop 后，对图片在右下角叠加 3 行文字：

- 时间：`YYYY-MM-DD HH:mm:ss`（使用 dayjs 格式化）
- GPS：地理位置（如传入 `gps` prop）
- 上传人：取自 `userStore.realName`（兜底 `userStore.username`）

字体大小自适应图片尺寸，使用半透明黑色背景 + 白色文字增强可读性。

### 5. 大文件分片上传

- 单文件 ≥ `multipartThreshold`（默认 10MB）自动启用分片上传
- 分片大小：`chunkSize`（默认 5MB）
- 并发数：`concurrency`（默认 3）
- 流程：
  1. 调用 `POST /api/v1/files/multipart/init` 获取 `uploadId` + 每个 part 的预签名 URL
  2. 并发 PUT 各分片到 MinIO，收集 ETag
  3. 调用 `POST /api/v1/files/multipart/complete` 完成合并

### 6. 断点续传

- 启用 `resume` prop 后，使用 IndexedDB 缓存：
  - 文件 hash（SHA-256，用于唯一标识文件）
  - uploadId + accessUrl + objectName
  - 已上传的分片（partNumber -> Blob）
- 大文件上传中断后，再次选择相同文件可继续上传（复用 uploadId 与已上传分片）
- 上传完成后自动清理缓存
- 在 SSR / 无痕模式 / 浏览器禁用 IndexedDB 时自动降级为不启用断点续传

## 后端接口约定

### `POST /api/v1/files/presign`

请求体：

```json
{
  "filename": "photo.jpg",
  "contentType": "image/jpeg",
  "size": 123456,
  "dir": "project/101",
  "hash": "sha256-..."
}
```

响应：

```json
{
  "code": 200,
  "data": {
    "uploadUrl": "https://minio.example.com/bucket/project/101/xxx.jpg?X-Amz-...",
    "accessUrl": "https://minio.example.com/bucket/project/101/xxx.jpg",
    "expires": 3600,
    "headers": {},
    "objectName": "project/101/xxx.jpg"
  }
}
```

### `POST /api/v1/files/multipart/init`

请求体：

```json
{
  "filename": "video.mp4",
  "contentType": "video/mp4",
  "size": 52428800,
  "partCount": 10,
  "dir": "project/101"
}
```

响应 `data`：

```json
{
  "uploadId": "abc123",
  "uploadUrls": ["https://...part1", "https://...part2"],
  "accessUrl": "https://.../video.mp4",
  "objectName": "project/101/xxx.mp4"
}
```

### `POST /api/v1/files/multipart/complete`

请求体：

```json
{
  "uploadId": "abc123",
  "accessUrl": "https://.../video.mp4",
  "objectName": "project/101/xxx.mp4",
  "etags": ["etag1", "etag2", "..."]
}
```

### `DELETE /api/v1/files`

查询参数：`objectName`

## 注意事项

1. **后端接口未实现时的降级行为**：组件直接调用上述接口，若后端返回 404 / 500，调用方需自行捕获 `error` 事件。在 `vibe-server` 当前的 `MinioUtils` 中已实现 `getPresignedUploadUrl`，可基于此快速搭建 FileController。

2. **MinIO PUT 签名校验**：预签名 URL 与请求的 Content-Type 必须一致，组件使用 `XMLHttpRequest` 直接 PUT，并设置与 `presign.headers` 一致的请求头。

3. **CORS 配置**：MinIO Bucket 需配置 CORS 允许前端域名 PUT，详见 [MinIO CORS 文档](https://min.io/docs/minio/linux/operations/cors.html)。

4. **图片压缩性能**：大图片压缩会占用主线程，建议在 Web Worker 中执行（当前实现为同步，未来可优化）。

5. **断点续传的 ETag 处理**：当前实现为简化版本，命中断点续传时仍重新上传所有分片以保证 ETag 正确。生产环境可优化为查询后端已上传 part 列表，跳过已上传分片。

6. **v-model 双向同步**：父组件传入 URL 时，组件会自动同步为 done 状态的 fileItem；上传成功后自动同步 URL 列表回父组件。单文件模式 `modelValue` 为 string，多文件模式为 string[]。

7. **GPS 获取**：组件不主动调用 `navigator.geolocation`（涉及权限申请 UX），需调用方传入 `gps` prop；未传则水印中省略 GPS 行。
