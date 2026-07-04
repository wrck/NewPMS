/**
 * 财务核算模块 API 封装
 * 对应后端：/api/v1/finance/{budgets|costs|settlements|profits}
 * 设计文档 2.8：项目预算/成本归集/代理商结算/利润分析
 */
import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  FinanceBudget,
  FinanceCost,
  FinanceWorkloadConfirmation,
  FinanceProfit,
  FinanceBudgetQuery,
  FinanceCostQuery,
  FinanceWorkloadQuery,
  FinanceBudgetDTO,
  FinanceCostDTO,
  FinanceWorkloadDTO
} from '@/types/finance'

const BASE = '/finance'

/* ============ 项目预算 ============ */

/** 分页查询预算 */
export function pageFinanceBudgets(params: FinanceBudgetQuery) {
  return http.get<PageResult<FinanceBudget>>(`${BASE}/budgets`, params as Record<string, unknown>)
}

/** 预算详情 */
export function getFinanceBudgetDetail(id: number) {
  return http.get<FinanceBudget>(`${BASE}/budgets/${id}`)
}

/** 创建预算 */
export function createFinanceBudget(dto: FinanceBudgetDTO) {
  return http.post<number>(`${BASE}/budgets`, dto)
}

/** 更新预算 */
export function updateFinanceBudget(id: number, dto: FinanceBudgetDTO) {
  return http.put<void>(`${BASE}/budgets/${id}`, dto)
}

/** 删除预算 */
export function deleteFinanceBudget(id: number) {
  return http.delete<void>(`${BASE}/budgets/${id}`)
}

/** 提交预算审批 */
export function submitFinanceBudget(id: number) {
  return http.post<void>(`${BASE}/budgets/${id}/submit`)
}

/** 审批预算 */
export function approveFinanceBudget(id: number, passed: boolean, remark?: string) {
  return http.post<void>(`${BASE}/budgets/${id}/approve?passed=${passed}${remark ? `&remark=${encodeURIComponent(remark)}` : ''}`)
}

/** 查询项目年度实际成本汇总 */
export function sumActualCost(projectId: number, year?: number, costType?: string) {
  const params: Record<string, unknown> = { projectId }
  if (year) params.year = year
  if (costType) params.costType = costType
  return http.get<number>(`${BASE}/budgets/actual-cost`, params)
}

/* ============ 成本归集 ============ */

/** 分页查询成本 */
export function pageFinanceCosts(params: FinanceCostQuery) {
  return http.get<PageResult<FinanceCost>>(`${BASE}/costs`, params as Record<string, unknown>)
}

/** 成本详情 */
export function getFinanceCostDetail(id: number) {
  return http.get<FinanceCost>(`${BASE}/costs/${id}`)
}

/** 创建成本 */
export function createFinanceCost(dto: FinanceCostDTO) {
  return http.post<number>(`${BASE}/costs`, dto)
}

/** 更新成本 */
export function updateFinanceCost(id: number, dto: FinanceCostDTO) {
  return http.put<void>(`${BASE}/costs/${id}`, dto)
}

/** 删除成本 */
export function deleteFinanceCost(id: number) {
  return http.delete<void>(`${BASE}/costs/${id}`)
}

/* ============ 代理商结算 ============ */

/** 分页查询结算单 */
export function pageFinanceSettlements(params: FinanceWorkloadQuery) {
  return http.get<PageResult<FinanceWorkloadConfirmation>>(`${BASE}/settlements`, params as Record<string, unknown>)
}

/** 结算单详情 */
export function getFinanceSettlementDetail(id: number) {
  return http.get<FinanceWorkloadConfirmation>(`${BASE}/settlements/${id}`)
}

/** 创建结算单 */
export function createFinanceSettlement(dto: FinanceWorkloadDTO) {
  return http.post<number>(`${BASE}/settlements`, dto)
}

/** 更新结算单 */
export function updateFinanceSettlement(id: number, dto: FinanceWorkloadDTO) {
  return http.put<void>(`${BASE}/settlements/${id}`, dto)
}

/** 删除结算单 */
export function deleteFinanceSettlement(id: number) {
  return http.delete<void>(`${BASE}/settlements/${id}`)
}

/** PM 确认工作量 */
export function pmConfirmSettlement(id: number) {
  return http.post<void>(`${BASE}/settlements/${id}/pm-confirm`)
}

/** 代理商确认工作量 */
export function agentConfirmSettlement(id: number) {
  return http.post<void>(`${BASE}/settlements/${id}/agent-confirm`)
}

/** 总监审批 */
export function directorApproveSettlement(id: number, passed: boolean, remark?: string) {
  return http.post<void>(`${BASE}/settlements/${id}/director-approve?passed=${passed}${remark ? `&remark=${encodeURIComponent(remark)}` : ''}`)
}

/** 财务审批 */
export function financeApproveSettlement(id: number, passed: boolean, remark?: string) {
  return http.post<void>(`${BASE}/settlements/${id}/finance-approve?passed=${passed}${remark ? `&remark=${encodeURIComponent(remark)}` : ''}`)
}

/** 更新付款状态 */
export function updateSettlementPaymentStatus(id: number, paymentStatus: string) {
  return http.post<void>(`${BASE}/settlements/${id}/payment-status?paymentStatus=${paymentStatus}`)
}

/* ============ 利润分析 ============ */

/** 查询指定项目的利润分析 */
export function getProjectProfit(projectId: number) {
  return http.get<FinanceProfit>(`${BASE}/profits/projects/${projectId}`)
}

/** 查询全部项目利润分析 */
export function listProjectProfit() {
  return http.get<FinanceProfit[]>(`${BASE}/profits/projects`)
}

/** 按客户维度统计利润 */
export function listProfitByCustomer() {
  return http.get<FinanceProfit[]>(`${BASE}/profits/by-customer`)
}

/** 按区域维度统计利润 */
export function listProfitByRegion() {
  return http.get<FinanceProfit[]>(`${BASE}/profits/by-region`)
}

/** 按产品线维度统计利润 */
export function listProfitByProductLine() {
  return http.get<FinanceProfit[]>(`${BASE}/profits/by-product-line`)
}
