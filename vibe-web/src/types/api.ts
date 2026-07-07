/**
 * 后端统一响应体类型（按系统设计文档 1.4）
 */
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
  timestamp?: number
  errors?: Array<{ field: string; message: string }>
}

export interface PageResult<T = unknown> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
}

/** 分页查询参数 */
export interface PageParams {
  page?: number
  size?: number
  keyword?: string
  sortField?: string
  sortOrder?: 'asc' | 'desc'
}

/** 趋势方向（统计卡片 StatisticCard 使用） */
export type TrendDirection = 'up' | 'down' | 'flat'

/** 业务错误码（按设计文档 1.4 错误码规范） */
export const ErrorCode = {
  SUCCESS: 200,
  // 400xx 参数校验错误
  PARAM_ERROR: 40000,
  // 401xx 认证错误
  UNAUTHORIZED: 40100,
  TOKEN_EXPIRED: 40101,
  TOKEN_INVALID: 40102,
  // 403xx 权限不足
  FORBIDDEN: 40300,
  // 404xx 资源不存在
  NOT_FOUND: 40400,
  // 409xx 业务冲突
  CONFLICT: 40900,
  // 500xx 系统内部错误
  SERVER_ERROR: 50000,
  // 502xx 外部服务调用失败
  EXTERNAL_ERROR: 50200
} as const
