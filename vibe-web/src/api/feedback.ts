/**
 * 反馈与工单模块 API 封装
 * 对应后端：/api/v1/feedback
 *
 * 接口列表：
 *   - POST /feedback                    提交反馈（任意已登录用户）
 *   - GET  /feedback                    管理员分页查询全部反馈（需 system:feedback:list）
 *   - GET  /feedback/mine               查询我提交的反馈
 *   - PUT  /feedback/{id}/handle        处理反馈（需 system:feedback:handle）
 */
import { http } from '@/utils/request'
import type { PageResult, PageParams } from '@/types/api'

/** 反馈类型 */
export type FeedbackType = 'BUG' | 'SUGGESTION' | 'QUESTION'

/** 反馈状态 */
export type FeedbackStatus = 'PENDING' | 'PROCESSING' | 'RESOLVED' | 'CLOSED'

/** 反馈视图对象（与后端 SysFeedbackVO 对齐） */
export interface SysFeedback {
  id: number
  type: FeedbackType
  title: string
  content?: string
  screenshotUrl?: string
  contact?: string
  submitterId?: number
  submitterName?: string
  status: FeedbackStatus
  handlerId?: number
  handlerName?: string
  handleNote?: string
  handleTime?: string
  createTime?: string
}

/** 反馈提交 DTO（与后端 SysFeedbackDTO 对齐） */
export interface SysFeedbackDTO {
  type: FeedbackType
  title: string
  content?: string
  /** 截图 URL（多个用逗号分隔） */
  screenshotUrl?: string
  contact?: string
}

/** 反馈处理 DTO（与后端 SysFeedbackHandleDTO 对齐） */
export interface SysFeedbackHandleDTO {
  /** 目标状态：PROCESSING / RESOLVED / CLOSED */
  status: 'PROCESSING' | 'RESOLVED' | 'CLOSED'
  handleNote?: string
}

/** 反馈分页查询参数 */
export interface SysFeedbackQueryParams extends PageParams {
  type?: FeedbackType
  status?: FeedbackStatus
  keyword?: string
  submitterId?: number
}

const FEEDBACK_BASE = '/feedback'

/** 提交反馈 */
export function submitFeedback(dto: SysFeedbackDTO) {
  return http.post<number>(FEEDBACK_BASE, dto)
}

/** 管理员分页查询全部反馈 */
export function pageFeedback(params: SysFeedbackQueryParams) {
  return http.get<PageResult<SysFeedback>>(FEEDBACK_BASE, params as Record<string, unknown>)
}

/** 查询我提交的反馈 */
export function pageMyFeedback(params: SysFeedbackQueryParams) {
  return http.get<PageResult<SysFeedback>>(`${FEEDBACK_BASE}/mine`, params as Record<string, unknown>)
}

/** 处理反馈（状态变更 + 备注） */
export function handleFeedback(id: number, dto: SysFeedbackHandleDTO) {
  return http.put<void>(`${FEEDBACK_BASE}/${id}/handle`, dto)
}
