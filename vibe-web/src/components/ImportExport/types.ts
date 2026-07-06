/**
 * ImportExport 导入导出按钮组件类型定义
 * 对应 spec：阶段三 前端通用组件抽取 - Task 20
 */

/** 单行导入错误信息 */
export interface ImportError {
  /** 行号（从 1 开始） */
  row: number
  /** 字段名（可选，用于定位具体字段） */
  field?: string
  /** 错误描述 */
  message: string
  /** 错误值（可选，用于展示原始值） */
  value?: unknown
}

/** 导入结果 */
export interface ImportResult {
  /** 整体是否成功（failCount===0 视为成功） */
  success: boolean
  /** 成功行数 */
  successCount: number
  /** 失败行数 */
  failCount: number
  /** 总行数 */
  totalCount: number
  /** 错误详情列表 */
  errors?: ImportError[]
  /** 耗时（ms，可选） */
  duration?: number
}

/** ImportExport 组件 Props */
export interface ImportExportProps {
  /** 导出接口 URL（POST 请求，返回 Blob 二进制流） */
  exportApi?: string
  /** 导入接口 URL（POST 请求，multipart/form-data） */
  importApi?: string
  /** 模板下载 URL（GET 请求，返回 Blob 二进制流） */
  templateUrl?: string
  /** 导出按钮文案 */
  exportText?: string
  /** 导入按钮文案 */
  importText?: string
  /** 模板下载按钮文案 */
  templateText?: string
  /** 是否显示导出按钮 */
  showExport?: boolean
  /** 是否显示导入按钮 */
  showImport?: boolean
  /** 是否显示模板下载按钮 */
  showTemplate?: boolean
  /** 按钮尺寸（透传 a-button size） */
  size?: 'small' | 'middle' | 'large'
  /** 导出参数（POST body，会以 JSON 形式发送） */
  exportParams?: Record<string, unknown>
  /** 导入附加数据（FormData 中追加的字段） */
  importData?: Record<string, unknown>
  /** 导出文件名（默认自动生成：导出_<timestamp>.xlsx） */
  exportFileName?: string
  /** 禁用导出按钮 */
  exportDisabled?: boolean
  /** 是否完整模式（同时显示导入/导出/模板按钮） */
  full?: boolean
  /** 上传文件大小限制（MB，默认 10） */
  maxSizeMb?: number
  /** 是否在导入成功后自动刷新（透传给父组件通过 emit 处理） */
  autoRefresh?: boolean
}

/** 组件 expose 出来的方法 */
export interface ImportExportExpose {
  /** 主动触发导出 */
  export: () => Promise<void>
  /** 主动触发模板下载 */
  downloadTemplate: () => Promise<void>
  /** 关闭导入结果弹窗 */
  closeResult: () => void
}
