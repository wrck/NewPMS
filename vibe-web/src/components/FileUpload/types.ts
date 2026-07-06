/**
 * FileUpload 通用文件上传组件类型定义
 * 对应 spec：阶段三 前端通用组件抽取 - Task 14
 *
 * 设计目标：与 CrudTable / FormModal 解耦，独立可复用，支持：
 *   - MinIO 预签名直传（小文件）/ 分片上传（大文件）
 *   - 图片压缩、缩略图、水印（时间 + GPS + 上传人）
 *   - 文件列表、预览、删除、下载
 *   - 弱网环境本地缓存 + 断点续传
 */

/** 文件项状态 */
export type FileItemStatus = 'uploading' | 'done' | 'error' | 'removed'

/** 上传列表展示样式（与 antd a-upload listType 对齐） */
export type UploadListType = 'picture-card' | 'picture' | 'text'

/**
 * 上传文件项（与 antd UploadFile 部分对齐，便于直接喂给 a-upload 的 file-list）
 */
export interface FileItem {
  /** 唯一 id（前端生成） */
  uid: string
  /** 文件名 */
  name: string
  /** 文件大小（字节） */
  size?: number
  /** MIME 类型 */
  type?: string
  /** 上传后访问 URL（MinIO accessUrl） */
  url?: string
  /** 缩略图 URL（用于 picture-card / picture 模式预览） */
  thumbUrl?: string
  /** 状态 */
  status: FileItemStatus
  /** 上传进度（0-100） */
  percent?: number
  /** 原始 File 对象（上传前后保留，便于压缩/分片/水印处理） */
  rawFile?: File
  /** 文件 hash（用于断点续传，大文件分片上传场景） */
  fileHash?: string
  /** 后端响应（presign / multipart complete 响应） */
  response?: any
  /** 错误信息（status=error 时） */
  error?: string
}

/**
 * 后端 /api/v1/files/presign 响应
 */
export interface PresignResponse {
  /** MinIO 预签名上传 URL（PUT 方法） */
  uploadUrl: string
  /** 上传成功后的访问 URL（持久化到业务表） */
  accessUrl: string
  /** 预签名 URL 过期时间（秒） */
  expires: number
  /** 自定义请求头（可选，例如要求 Content-Type） */
  headers?: Record<string, string>
  /** 后端为该文件分配的 objectName（可选，便于服务端管理） */
  objectName?: string
}

/**
 * 分片上传初始化响应
 */
export interface MultipartInitResponse {
  /** MinIO Multipart Upload uploadId */
  uploadId: string
  /** 每个 part 的预签名上传 URL（顺序即 partNumber） */
  uploadUrls: string[]
  /** accessUrl（合并后的访问地址） */
  accessUrl?: string
  /** objectName（MinIO 对象路径） */
  objectName?: string
}

/**
 * 完成分片上传请求体
 */
export interface CompleteMultipartPayload {
  uploadId: string
  accessUrl: string
  objectName?: string
  /** 每个 part 的 ETag，顺序为 partNumber 升序 */
  etags: string[]
}

/**
 * 水印配置（仅图片水印）
 */
export interface WatermarkOptions {
  /** 时间字符串（如 YYYY-MM-DD HH:mm:ss） */
  time: string
  /** GPS 位置（可选，如 "深圳市南山区"） */
  gps?: string
  /** 上传人姓名 */
  uploader: string
}

/**
 * FileUpload 组件 Props
 */
export interface FileUploadProps {
  /** v-model 绑定值：单文件时为 string，多文件时为 string[] */
  modelValue: string | string[]
  /** 接受的文件类型（MIME 或后缀，如 'image/*' / '.jpg,.png'） */
  accept?: string
  /** 单文件大小上限（MB），默认 10 */
  maxSize?: number
  /** 最大文件数（多文件模式生效），默认 9 */
  maxCount?: number
  /** 是否多选，默认 false */
  multiple?: boolean
  /** 是否对图片加水印（时间 + GPS + 上传人），默认 false */
  watermark?: boolean
  /** 是否禁用，默认 false */
  disabled?: boolean
  /** 列表展示样式，默认 'picture-card' */
  listType?: UploadListType
  /** MinIO Bucket 内目录（如 'project/101'），透传给后端 */
  dir?: string
  /** 大文件分片阈值（MB，>= 该值启用分片上传），默认 10 */
  multipartThreshold?: number
  /** 分片大小（MB），默认 5 */
  chunkSize?: number
  /** 分片上传并发数，默认 3 */
  concurrency?: number
  /** 是否启用断点续传（依赖 IndexedDB 本地缓存），默认 true */
  resume?: boolean
  /** 自定义 GPS 位置（如调用 navigator.geolocation 失败时的回退值） */
  gps?: string
}

/**
 * FileUpload 组件 Emits
 */
export interface FileUploadEmits {
  /** v-model 更新 */
  (e: 'update:modelValue', value: string | string[]): void
  /** 文件列表变化（含状态、进度） */
  (e: 'change', fileList: FileItem[]): void
  /** 单个文件上传成功 */
  (e: 'success', file: FileItem): void
  /** 单个文件上传失败 */
  (e: 'error', file: FileItem, error: Error): void
  /** 所有文件上传完成 */
  (e: 'finish', fileList: FileItem[]): void
  /** 文件被移除 */
  (e: 'remove', file: FileItem): void
  /** 预览文件 */
  (e: 'preview', file: FileItem): void
}

/**
 * 暴露给外部的实例方法
 */
export interface FileUploadExpose {
  /** 获取当前文件列表 */
  getFileList: () => FileItem[]
  /** 手动触发所有 uploading 状态的文件重试 */
  retryAll: () => Promise<void>
  /** 清空文件列表 */
  clear: () => void
  /** 取消所有上传中的任务 */
  cancelAll: () => void
}
