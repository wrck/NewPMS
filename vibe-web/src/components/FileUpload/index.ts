/**
 * FileUpload 组件统一导出
 *
 * 用法：
 *   import FileUpload from '@/components/FileUpload'
 * 或：
 *   import { FileUpload } from '@/components/FileUpload'
 *
 * 类型也可单独导出供父组件使用：
 *   import type { FileItem, FileUploadProps } from '@/components/FileUpload'
 */
import FileUpload from './index.vue'

export { default as FileUpload } from './index.vue'
export type {
  FileItem,
  FileItemStatus,
  UploadListType,
  PresignResponse,
  MultipartInitResponse,
  CompleteMultipartPayload,
  WatermarkOptions,
  FileUploadProps,
  FileUploadEmits,
  FileUploadExpose
} from './types'

export default FileUpload
