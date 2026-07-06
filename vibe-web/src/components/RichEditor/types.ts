/**
 * RichEditor 富文本编辑器组件类型定义
 * 对应 spec：阶段三 前端通用组件抽取 - Task 15
 *
 * 基于 wangEditor 5 封装，支持：
 *   - v-model 双向绑定（HTML / 纯文本两种输出）
 *   - 自定义工具栏（excludeKeys / mode）
 *   - 只读模式
 *   - 图片上传走 presign + PUT MinIO（可对接 Task 14 的 FileUpload 组件）
 */
import type { IDomEditor } from '@wangeditor/editor'

/** 编辑器模式：default 完整工具栏 / simple 简洁工具栏 */
export type EditorMode = 'default' | 'simple'

/** 输出格式：html 完整富文本 / text 纯文本 */
export type OutputFormat = 'html' | 'text'

/** RichEditor 组件 Props */
export interface RichEditorProps {
  /** v-model 绑定值（HTML 字符串或纯文本，取决于 outputFormat） */
  modelValue: string
  /** 编辑器总高度（px），工具栏占 50px，编辑区占剩余高度 */
  height?: number
  /** 编辑器模式：default 完整 / simple 简洁 */
  mode?: EditorMode
  /** 是否只读（只读时隐藏工具栏） */
  readonly?: boolean
  /** 输出格式：html（默认）/ text */
  outputFormat?: OutputFormat
  /** 占位提示文字 */
  placeholder?: string
  /** 工具栏排除的按钮 key 列表（如 ['fullScreen', 'group-video']） */
  excludeKeys?: string[]
  /** 图片上传目标 bucket（默认 'documents'） */
  uploadBucket?: string
  /** 单张图片大小上限（MB），默认 10 */
  maxImageSize?: number
}

/** RichEditor 组件 Emits */
export interface RichEditorEmits {
  /** v-model 更新 */
  (e: 'update:modelValue', value: string): void
  /** 内容变化事件 */
  (e: 'change', value: string): void
  /** 编辑器创建完成，传出 editor 实例 */
  (e: 'created', editor: IDomEditor): void
  /** 图片上传成功 */
  (e: 'upload-success', payload: { url: string; alt: string; file: File }): void
  /** 图片上传失败 */
  (e: 'upload-error', payload: { file: File; error: Error }): void
}

/** RichEditor 通过 ref 暴露给父组件的方法 */
export interface RichEditorExpose {
  /** 获取 wangEditor 实例 */
  getEditor: () => IDomEditor | null
  /** 获取当前 HTML */
  getHtml: () => string
  /** 获取当前纯文本 */
  getText: () => string
  /** 清空编辑器 */
  clear: () => void
  /**聚焦编辑器 */
  focus: () => void
  /** 失焦 */
  blur: () => void
  /** 手动插入图片（外部调用，如从 FileUpload 拖入） */
  insertImage: (url: string, alt?: string, href?: string) => void
}

/** presign 接口响应数据结构 */
export interface PresignResponse {
  /** 预签名 PUT 上传地址 */
  uploadUrl: string
  /** 上传成功后的访问地址 */
  accessUrl: string
  /** 对象 key（MinIO 存储路径） */
  objectKey?: string
}

/** 重新导出编辑器类型，方便业务方按需引用 */
export type { IDomEditor } from '@wangeditor/editor'
