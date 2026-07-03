/**
 * 代理商管理模块 API 封装
 * 对应后端：/api/v1/agent-companies、/api/v1/outsource-tasks、
 *           /api/v1/outsource-tasks/{taskId}/deliverables、
 *           /api/v1/outsource-tasks/{taskId}/workload、
 *           /api/v1/agent-companies/{companyId}/scores、
 *           /api/v1/agent-companies/{companyId}/engineers
 */
import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  AgentCompany,
  AgentEngineer,
  OutsourceTask,
  OutsourceDeliverable,
  OutsourceWorkload,
  AgentScore,
  AgentCompanyQueryParams,
  OutsourceTaskQueryParams,
  WorkloadQueryParams,
  AgentCompanyDTO,
  AgentEngineerDTO,
  OutsourceTaskDTO,
  WorkloadConfirmDTO,
  AgentScoreDTO
} from '@/types/agent'

const COMPANY_BASE = '/agent-companies'
const OUTSOURCE_BASE = '/outsource-tasks'

/* ============ 代理商公司 ============ */

export function pageAgentCompanies(params: AgentCompanyQueryParams) {
  return http.get<PageResult<AgentCompany>>(COMPANY_BASE, params as Record<string, unknown>)
}

export function getAgentCompanyDetail(id: number) {
  return http.get<AgentCompany>(`${COMPANY_BASE}/${id}`)
}

export function createAgentCompany(dto: AgentCompanyDTO) {
  return http.post<number>(COMPANY_BASE, dto)
}

export function updateAgentCompany(id: number, dto: AgentCompanyDTO) {
  return http.put<void>(`${COMPANY_BASE}/${id}`, dto)
}

export function deleteAgentCompany(id: number) {
  return http.delete<void>(`${COMPANY_BASE}/${id}`)
}

/** 变更合作状态（ACTIVE/SUSPENDED/TERMINATED） */
export function changeAgentCompanyStatus(id: number, status: 'ACTIVE' | 'SUSPENDED' | 'TERMINATED', remark?: string) {
  return http.put<void>(`${COMPANY_BASE}/${id}/status`, { status, remark })
}

/* ============ 代理商工程师 ============ */

export function listAgentEngineers(companyId: number) {
  return http.get<AgentEngineer[]>(`${COMPANY_BASE}/${companyId}/engineers`)
}

export function createAgentEngineer(companyId: number, dto: AgentEngineerDTO) {
  return http.post<number>(`${COMPANY_BASE}/${companyId}/engineers`, dto)
}

export function updateAgentEngineer(companyId: number, id: number, dto: AgentEngineerDTO) {
  return http.put<void>(`${COMPANY_BASE}/${companyId}/engineers/${id}`, dto)
}

export function deleteAgentEngineer(companyId: number, id: number) {
  return http.delete<void>(`${COMPANY_BASE}/${companyId}/engineers/${id}`)
}

/* ============ 转包任务 ============ */

export function pageOutsourceTasks(params: OutsourceTaskQueryParams) {
  return http.get<PageResult<OutsourceTask>>(OUTSOURCE_BASE, params as Record<string, unknown>)
}

export function getOutsourceTaskDetail(id: number) {
  return http.get<OutsourceTask>(`${OUTSOURCE_BASE}/${id}`)
}

export function createOutsourceTask(dto: OutsourceTaskDTO) {
  return http.post<number>(OUTSOURCE_BASE, dto)
}

/** 代理商接单/拒绝 */
export function respondOutsourceTask(id: number, accepted: boolean, remark?: string) {
  return http.put<void>(`${OUTSOURCE_BASE}/${id}/respond`, { accepted, remark })
}

/** 代理商指派工程师 */
export function assignAgentEngineer(id: number, agentEngineerId: number) {
  return http.put<void>(`${OUTSOURCE_BASE}/${id}/assign`, { agentEngineerId })
}

/** PM 退回（含退回原因） */
export function rejectOutsourceTask(id: number, reason: string) {
  return http.put<void>(`${OUTSOURCE_BASE}/${id}/reject`, { reason })
}

/** PM 确认通过 */
export function confirmOutsourceTask(id: number, remark?: string) {
  return http.put<void>(`${OUTSOURCE_BASE}/${id}/confirm`, { remark })
}

/* ============ 转包任务交付物 ============ */

export function listDeliverables(taskId: number) {
  return http.get<OutsourceDeliverable[]>(`${OUTSOURCE_BASE}/${taskId}/deliverables`)
}

export function uploadDeliverable(taskId: number, dto: Partial<OutsourceDeliverable>) {
  return http.post<number>(`${OUTSOURCE_BASE}/${taskId}/deliverables`, dto)
}

export function deleteDeliverable(taskId: number, id: number) {
  return http.delete<void>(`${OUTSOURCE_BASE}/${taskId}/deliverables/${id}`)
}

/* ============ 工作量确认与结算 ============ */

export function listWorkloads(params: WorkloadQueryParams) {
  return http.get<PageResult<OutsourceWorkload>>(`${OUTSOURCE_BASE}/workloads`, params as Record<string, unknown>)
}

export function getTaskWorkload(taskId: number) {
  return http.get<OutsourceWorkload>(`${OUTSOURCE_BASE}/${taskId}/workload`)
}

export function submitWorkload(taskId: number, dto: WorkloadConfirmDTO) {
  return http.post<void>(`${OUTSOURCE_BASE}/${taskId}/workload`, dto)
}

/** PM 确认工作量 */
export function confirmWorkload(taskId: number, dto: Partial<WorkloadConfirmDTO>) {
  return http.put<void>(`${OUTSOURCE_BASE}/${taskId}/workload/confirm`, dto)
}

/** 工作量审批（PM → 总监 → 财务） */
export function approveWorkload(taskId: number, approved: boolean, remark?: string) {
  return http.put<void>(`${OUTSOURCE_BASE}/${taskId}/workload/approve`, { approved, remark })
}

/* ============ 代理商评分 ============ */

export function listAgentScores(companyId: number) {
  return http.get<AgentScore[]>(`${COMPANY_BASE}/${companyId}/scores`)
}

export function createAgentScore(companyId: number, dto: AgentScoreDTO) {
  return http.post<number>(`${COMPANY_BASE}/${companyId}/scores`, dto)
}
