/**
 * 验收管理模块 API 封装
 * 对应后端：/api/v1/acceptance/{standards|tasks|issues|docs}
 * 设计文档 2.7：验收标准/测试记录/验收流程/遗留问题/竣工文档
 */
import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  AcceptanceStandard,
  AcceptanceTask,
  AcceptanceIssue,
  AcceptanceDoc,
  AcceptanceTestRecord,
  AcceptanceStandardQuery,
  AcceptanceTaskQuery,
  AcceptanceIssueQuery,
  AcceptanceDocQuery,
  AcceptanceStandardDTO,
  AcceptanceTaskDTO,
  AcceptanceTaskAction,
  AcceptanceIssueDTO,
  AcceptanceDocDTO
} from '@/types/acceptance'

const BASE = '/acceptance'

/* ============ 验收标准管理 ============ */

/** 分页查询验收标准 */
export function pageAcceptanceStandards(params: AcceptanceStandardQuery) {
  return http.get<PageResult<AcceptanceStandard>>(`${BASE}/standards`, params as Record<string, unknown>)
}

/** 查询全部启用的验收标准（下拉选择） */
export function listEnabledStandards() {
  return http.get<AcceptanceStandard[]>(`${BASE}/standards/enabled`)
}

/** 验收标准详情（含检查项） */
export function getAcceptanceStandardDetail(id: number) {
  return http.get<AcceptanceStandard>(`${BASE}/standards/${id}`)
}

/** 创建验收标准 */
export function createAcceptanceStandard(dto: AcceptanceStandardDTO) {
  return http.post<number>(`${BASE}/standards`, dto)
}

/** 更新验收标准 */
export function updateAcceptanceStandard(id: number, dto: AcceptanceStandardDTO) {
  return http.put<void>(`${BASE}/standards/${id}`, dto)
}

/** 删除验收标准 */
export function deleteAcceptanceStandard(id: number) {
  return http.delete<void>(`${BASE}/standards/${id}`)
}

/* ============ 验收任务（验收流程） ============ */

/** 分页查询验收任务 */
export function pageAcceptanceTasks(params: AcceptanceTaskQuery) {
  return http.get<PageResult<AcceptanceTask>>(`${BASE}/tasks`, params as Record<string, unknown>)
}

/** 验收任务详情 */
export function getAcceptanceTaskDetail(id: number) {
  return http.get<AcceptanceTask>(`${BASE}/tasks/${id}`)
}

/** 创建验收任务 */
export function createAcceptanceTask(dto: AcceptanceTaskDTO) {
  return http.post<number>(`${BASE}/tasks`, dto)
}

/** 更新验收任务 */
export function updateAcceptanceTask(id: number, dto: AcceptanceTaskDTO) {
  return http.put<void>(`${BASE}/tasks/${id}`, dto)
}

/** 删除验收任务 */
export function deleteAcceptanceTask(id: number) {
  return http.delete<void>(`${BASE}/tasks/${id}`)
}

/** PM 提交验收申请 */
export function applyAcceptanceTask(action: AcceptanceTaskAction) {
  return http.post<void>(`${BASE}/tasks/apply`, action)
}

/** 内部技术审核 */
export function internalAuditAcceptanceTask(action: AcceptanceTaskAction) {
  return http.post<void>(`${BASE}/tasks/internal-audit`, action)
}

/** 发起客户签核 */
export function startCustomerSign(taskId: number) {
  return http.post<void>(`${BASE}/tasks/${taskId}/start-customer-sign`)
}

/** 客户签核结果 */
export function customerSignAcceptanceTask(action: AcceptanceTaskAction) {
  return http.post<void>(`${BASE}/tasks/customer-sign`, action)
}

/** 查询验收任务的测试记录 */
export function listAcceptanceTestRecords(taskId: number) {
  return http.get<AcceptanceTestRecord[]>(`${BASE}/tasks/${taskId}/test-records`)
}

/* ============ 遗留问题跟踪 ============ */

/** 分页查询遗留问题 */
export function pageAcceptanceIssues(params: AcceptanceIssueQuery) {
  return http.get<PageResult<AcceptanceIssue>>(`${BASE}/issues`, params as Record<string, unknown>)
}

/** 遗留问题详情 */
export function getAcceptanceIssueDetail(id: number) {
  return http.get<AcceptanceIssue>(`${BASE}/issues/${id}`)
}

/** 创建遗留问题 */
export function createAcceptanceIssue(dto: AcceptanceIssueDTO) {
  return http.post<number>(`${BASE}/issues`, dto)
}

/** 更新遗留问题 */
export function updateAcceptanceIssue(id: number, dto: AcceptanceIssueDTO) {
  return http.put<void>(`${BASE}/issues/${id}`, dto)
}

/** 删除遗留问题 */
export function deleteAcceptanceIssue(id: number) {
  return http.delete<void>(`${BASE}/issues/${id}`)
}

/** 指派整改责任人 */
export function assignAcceptanceIssue(id: number, assigneeId: number) {
  return http.post<void>(`${BASE}/issues/${id}/assign?assigneeId=${assigneeId}`)
}

/** 标记整改完成 */
export function resolveAcceptanceIssue(id: number) {
  return http.post<void>(`${BASE}/issues/${id}/resolve`)
}

/** 闭环确认 */
export function closeAcceptanceIssue(id: number) {
  return http.post<void>(`${BASE}/issues/${id}/close`)
}

/* ============ 竣工文档 ============ */

/** 分页查询竣工文档 */
export function pageAcceptanceDocs(params: AcceptanceDocQuery) {
  return http.get<PageResult<AcceptanceDoc>>(`${BASE}/docs`, params as Record<string, unknown>)
}

/** 竣工文档详情 */
export function getAcceptanceDocDetail(id: number) {
  return http.get<AcceptanceDoc>(`${BASE}/docs/${id}`)
}

/** 上传/创建竣工文档 */
export function createAcceptanceDoc(dto: AcceptanceDocDTO) {
  return http.post<number>(`${BASE}/docs`, dto)
}

/** 更新竣工文档 */
export function updateAcceptanceDoc(id: number, dto: AcceptanceDocDTO) {
  return http.put<void>(`${BASE}/docs/${id}`, dto)
}

/** 删除竣工文档 */
export function deleteAcceptanceDoc(id: number) {
  return http.delete<void>(`${BASE}/docs/${id}`)
}
