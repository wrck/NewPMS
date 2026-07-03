/**
 * 代理商 API（module-agent）
 * 路径前缀 /v1，包含转包任务、交付物、工作量、评分
 */
import { http } from '@/utils/request'
import {
  type OutsourceTaskVO,
  type OutsourceTaskDetailVO,
  type SubmitDeliverableParams,
  type OutsourceDeliverableVO,
  type AssignEngineerParams,
  type WorkloadVO,
  type SubmitWorkloadParams,
  type AgentScoreVO,
  type PageResult,
  type OutsourceTaskStatusEnum
} from '@/types/api'

const SCOPE = { authScope: 'agent' } as const

/** 任务列表查询参数 */
export interface OutsourceTaskQuery {
  page?: number
  size?: number
  status?: OutsourceTaskStatusEnum | string
  keyword?: string
}

/**
 * 转包任务分页 GET /v1/outsource-tasks?page=1&size=10
 */
export function getOutsourceTasks(params: OutsourceTaskQuery = {}) {
  return http.get<PageResult<OutsourceTaskVO>>(
    '/v1/outsource-tasks',
    {
      page: params.page ?? 1,
      size: params.size ?? 10,
      status: params.status || undefined,
      keyword: params.keyword || undefined
    },
    { ...SCOPE, silent: true }
  )
}

/** 任务详情 GET /v1/outsource-tasks/{id} */
export function getOutsourceTaskDetail(id: number | string) {
  return http.get<OutsourceTaskDetailVO>(`/v1/outsource-tasks/${id}`, undefined, {
    ...SCOPE,
    silent: true
  })
}

/** 接单 POST /v1/outsource-tasks/{id}/accept */
export function acceptTask(id: number | string) {
  return http.post<void>(`/v1/outsource-tasks/${id}/accept`, undefined, {
    ...SCOPE,
    loadingText: '处理中...'
  })
}

/** 拒绝 POST /v1/outsource-tasks/{id}/reject */
export function rejectTask(id: number | string) {
  return http.post<void>(`/v1/outsource-tasks/${id}/reject`, undefined, {
    ...SCOPE,
    loadingText: '处理中...'
  })
}

/** 指派工程师 POST /v1/outsource-tasks/{id}/assign */
export function assignEngineer(id: number | string, params: AssignEngineerParams) {
  return http.post<void>(`/v1/outsource-tasks/${id}/assign`, params, {
    ...SCOPE,
    loadingText: '指派中...'
  })
}

/** 提交交付物 POST /v1/outsource-tasks/{taskId}/deliverables */
export function submitDeliverable(taskId: number | string, params: SubmitDeliverableParams) {
  return http.post<void>(`/v1/outsource-tasks/${taskId}/deliverables`, params, {
    ...SCOPE,
    loadingText: '提交中...'
  })
}

/** 交付物列表 GET /v1/outsource-tasks/{taskId}/deliverables */
export function getDeliverables(taskId: number | string) {
  return http.get<OutsourceDeliverableVO[]>(
    `/v1/outsource-tasks/${taskId}/deliverables`,
    undefined,
    { ...SCOPE, silent: true }
  )
}

/** 工作量查询 GET /v1/outsource-tasks/{taskId}/workload */
export function getWorkload(taskId: number | string) {
  return http.get<WorkloadVO>(`/v1/outsource-tasks/${taskId}/workload`, undefined, {
    ...SCOPE,
    silent: true
  })
}

/** 提交工作量 POST /v1/outsource-tasks/{taskId}/workload */
export function submitWorkload(taskId: number | string, params: SubmitWorkloadParams) {
  return http.post<void>(`/v1/outsource-tasks/${taskId}/workload`, params, {
    ...SCOPE,
    loadingText: '提交中...'
  })
}

/** 评分查询 GET /v1/agent-companies/{companyId}/scores */
export function getScores(companyId: number | string) {
  return http.get<AgentScoreVO[]>(`/v1/agent-companies/${companyId}/scores`, undefined, {
    ...SCOPE,
    silent: true
  })
}

/**
 * 通用文件上传（multipart/form-data）
 * 注：后端未在端点清单中明确，按 v1 约定挂载在 /v1/files/upload
 */
export function uploadFile(file: File, onProgress?: (percent: number) => void) {
  const formData = new FormData()
  formData.append('file', file, file.name)
  return http.upload<string>('/v1/files/upload', formData, {
    ...SCOPE,
    silent: true,
    onProgress
  })
}
