/**
 * 割接管理模块 API 封装
 * 对应后端：/api/v1/cutover/plans
 * 设计文档 2.6.2：割接方案编制/审批/执行/总结全流程
 */
import { http } from '@/utils/request'
import type { PageResult, PageParams } from '@/types/api'

/* ============ 类型定义 ============ */

/** 割接方案状态 */
export type CutoverPlanStatus =
  | 'DRAFT'
  | 'PENDING_INTERNAL_APPROVAL'
  | 'INTERNAL_APPROVED'
  | 'INTERNAL_REJECTED'
  | 'PENDING_CUSTOMER_APPROVAL'
  | 'CUSTOMER_APPROVED'
  | 'CUSTOMER_REJECTED'
  | 'EXECUTING'
  | 'COMPLETED'
  | 'ABORTED'

/** 割接步骤状态 */
export type CutoverStepStatus = 'PENDING' | 'EXECUTING' | 'COMPLETED' | 'ROLLED_BACK' | 'ABORTED'

/** 割接方案 VO（列表） */
export interface CutoverPlan {
  id: number
  projectId: number
  projectName?: string
  planName: string
  cutoverDate: string
  startTime: string
  endTime: string
  status: CutoverPlanStatus
  applyUserId?: number
  applyUserName?: string
  applyTime?: string
  stepCount?: number
  completedStepCount?: number
  createTime?: string
  updateTime?: string
}

/** 割接步骤 VO */
export interface CutoverStep {
  id: number
  planId: number
  sortOrder: number
  stepName: string
  description?: string
  estimatedDuration?: number
  ownerId?: number
  ownerName?: string
  rollbackPlan?: string
  status: CutoverStepStatus
  actualStartTime?: string
  actualEndTime?: string
  actualDuration?: number
  executionRemark?: string
  exceptionRemark?: string
}

/** 割接操作日志 VO */
export interface CutoverExecutionLog {
  id: number
  planId: number
  stepId?: number
  stepName?: string
  operatorId?: number
  operatorName?: string
  action: string
  logTime: string
  logContent?: string
  logLevel: 'INFO' | 'WARN' | 'ERROR'
}

/** 割接方案详情 VO */
export interface CutoverPlanDetail extends CutoverPlan {
  impactScope?: string
  emergencyContact?: string
  remark?: string
  approvalUserId?: number
  approvalUserName?: string
  approvalTime?: string
  approvalRemark?: string
  customerSignLink?: string
  customerSignUser?: string
  customerSignTime?: string
  customerSignResult?: 'APPROVED' | 'REJECTED'
  customerSignRemark?: string
  actualStartTime?: string
  actualEndTime?: string
  summary?: string
  problemImprovement?: string
  steps: CutoverStep[]
  logs: CutoverExecutionLog[]
}

/** 割接步骤 DTO（嵌入创建/更新请求中） */
export interface CutoverStepDTO {
  id?: number
  sortOrder?: number
  stepName: string
  description?: string
  estimatedDuration?: number
  ownerId?: number
  ownerName?: string
  rollbackPlan?: string
}

/** 割接方案创建/更新 DTO */
export interface CutoverPlanDTO {
  projectId: number
  planName: string
  cutoverDate: string
  startTime: string
  endTime: string
  impactScope?: string
  emergencyContact?: string
  remark?: string
  steps: CutoverStepDTO[]
}

/** 割接方案查询参数 */
export interface CutoverPlanQueryParams extends PageParams {
  projectId?: number
  planName?: string
  status?: CutoverPlanStatus
  dateFrom?: string
  dateTo?: string
  applyUserId?: number
}

/** 割接审批 DTO */
export interface CutoverApprovalDTO {
  planId: number
  result?: 'APPROVED' | 'REJECTED'
  customerSignUser?: string
  remark?: string
}

/** 割接步骤执行 DTO */
export interface CutoverStepExecuteDTO {
  planId: number
  stepId: number
  executionRemark?: string
  exceptionRemark?: string
  actualDuration?: number
}

/** 割接完成 DTO */
export interface CutoverCompleteDTO {
  planId: number
  summary?: string
  problemImprovement?: string
}

/* ============ API ============ */

const BASE = '/cutover/plans'

/** 分页查询割接方案 */
export function pageCutoverPlans(params: CutoverPlanQueryParams) {
  return http.get<PageResult<CutoverPlan>>(BASE, params as Record<string, unknown>)
}

/** 割接方案详情（含步骤/操作日志） */
export function getCutoverPlanDetail(id: number) {
  return http.get<CutoverPlanDetail>(`${BASE}/${id}`)
}

/** 创建割接方案（含步骤） */
export function createCutoverPlan(dto: CutoverPlanDTO) {
  return http.post<number>(BASE, dto)
}

/** 更新割接方案（仅草稿可改） */
export function updateCutoverPlan(id: number, dto: CutoverPlanDTO) {
  return http.put<void>(`${BASE}/${id}`, dto)
}

/** 删除割接方案（仅草稿可删） */
export function deleteCutoverPlan(id: number) {
  return http.delete<void>(`${BASE}/${id}`)
}

/* ============ 审批流程 ============ */

/** 提交内部审批 */
export function submitInternalApproval(id: number) {
  return http.post<void>(`${BASE}/${id}/submit-internal-approval`)
}

/** 内部审批通过 */
export function internalApprovePlan(dto: CutoverApprovalDTO) {
  return http.post<void>(`${BASE}/internal-approve`, dto)
}

/** 内部审批驳回 */
export function internalRejectPlan(dto: CutoverApprovalDTO) {
  return http.post<void>(`${BASE}/internal-reject`, dto)
}

/** 发起客户审批（返回客户签核链接token） */
export function startCustomerApproval(id: number) {
  return http.post<string>(`${BASE}/${id}/start-customer-approval`)
}

/* ============ 执行流程 ============ */

/** 开始执行割接 */
export function startCutoverExecution(id: number) {
  return http.post<void>(`${BASE}/${id}/start-execution`)
}

/** 执行步骤（PENDING→EXECUTING 或 EXECUTING→COMPLETED） */
export function executeCutoverStep(dto: CutoverStepExecuteDTO) {
  return http.post<void>(`${BASE}/execute-step`, dto)
}

/** 回退步骤 */
export function rollbackCutoverStep(dto: CutoverStepExecuteDTO) {
  return http.post<void>(`${BASE}/rollback-step`, dto)
}

/** 步骤异常 */
export function exceptionCutoverStep(dto: CutoverStepExecuteDTO) {
  return http.post<void>(`${BASE}/exception-step`, dto)
}

/** 完成割接 */
export function completeCutoverPlan(dto: CutoverCompleteDTO) {
  return http.post<void>(`${BASE}/complete`, dto)
}

/** 中止割接 */
export function abortCutoverPlan(id: number, remark?: string) {
  return http.post<void>(`${BASE}/${id}/abort`, null, { params: { remark } })
}

/* ============ 查询 ============ */

/** 查询割接方案操作日志 */
export function listCutoverLogs(planId: number) {
  return http.get<CutoverExecutionLog[]>(`${BASE}/${planId}/logs`)
}

/* ============ 状态展示辅助 ============ */

/** 割接方案状态标签映射 */
export const CutoverPlanStatusLabel: Record<CutoverPlanStatus, string> = {
  DRAFT: '草稿',
  PENDING_INTERNAL_APPROVAL: '待内部审批',
  INTERNAL_APPROVED: '内部审批通过',
  INTERNAL_REJECTED: '内部审批驳回',
  PENDING_CUSTOMER_APPROVAL: '待客户审批',
  CUSTOMER_APPROVED: '客户审批通过',
  CUSTOMER_REJECTED: '客户审批驳回',
  EXECUTING: '执行中',
  COMPLETED: '已完成',
  ABORTED: '已中止'
}

/** 割接步骤状态标签映射 */
export const CutoverStepStatusLabel: Record<CutoverStepStatus, string> = {
  PENDING: '待执行',
  EXECUTING: '执行中',
  COMPLETED: '已完成',
  ROLLED_BACK: '已回退',
  ABORTED: '已中止'
}
