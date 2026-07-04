/**
 * 财务核算模块类型定义
 * 对应后端：/api/v1/finance/{budgets|costs|settlements|profits}
 * 设计文档 2.8
 */
import type { PageParams } from './api'

/* ============ 枚举类型 ============ */

/** 预算审批状态 */
export type BudgetApprovalStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED'

/** 成本类型 */
export type FinanceCostType = 'LABOR' | 'TRAVEL' | 'AGENT' | 'OTHER'

/** 结算审批状态 */
export type SettlementApprovalStatus =
  | 'DRAFT'
  | 'PM_CONFIRMED'
  | 'AGENT_CONFIRMED'
  | 'PENDING'
  | 'DIRECTOR_APPROVED'
  | 'FINANCE_APPROVED'
  | 'REJECTED'

/** 付款状态 */
export type PaymentStatus = 'UNPAID' | 'PAYING' | 'PAID'

/** 关联业务类型 */
export type CostRefType = 'TIMESHEET' | 'BUSINESS_TRIP' | 'OUTSOURCE_TASK' | 'MANUAL'

/* ============ 实体类型 ============ */

/** 项目预算 */
export interface FinanceBudget {
  id: number
  projectId: number
  year: number
  laborAmount?: number
  travelAmount?: number
  agentAmount?: number
  otherAmount?: number
  totalAmount?: number
  approvalStatus: BudgetApprovalStatus
  approverId?: number
  approveTime?: string
  remark?: string
  createTime?: string
  updateTime?: string
  // 实际成本对比（运行时计算）
  actualLabor?: number
  actualTravel?: number
  actualAgent?: number
  actualOther?: number
  actualTotal?: number
}

/** 成本归集 */
export interface FinanceCost {
  id: number
  projectId: number
  costType: FinanceCostType
  amount: number
  costDate: string
  refType?: CostRefType
  refId?: number
  description?: string
  createTime?: string
  updateTime?: string
}

/** 代理商结算 */
export interface FinanceWorkloadConfirmation {
  id: number
  projectId: number
  outsourceTaskId?: number
  agentCompanyId: number
  period: string
  workloadDays: number
  unitPrice: number
  travelAmount?: number
  otherAmount?: number
  totalAmount?: number
  pmConfirmUserId?: number
  pmConfirmTime?: string
  agentConfirmUserId?: number
  agentConfirmTime?: string
  approvalStatus: SettlementApprovalStatus
  paymentStatus: PaymentStatus
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 利润分析 */
export interface FinanceProfit {
  projectId: number
  projectName?: string
  revenue?: number
  selfCost?: number
  agentCost?: number
  totalCost?: number
  profit?: number
  profitMargin?: number
  selfCostRatio?: number
  agentCostRatio?: number
}

/* ============ 查询参数 ============ */

export interface FinanceBudgetQuery extends PageParams {
  projectId?: number
  year?: number
  approvalStatus?: BudgetApprovalStatus
}

export interface FinanceCostQuery extends PageParams {
  projectId?: number
  costType?: FinanceCostType
  startDate?: string
  endDate?: string
}

export interface FinanceWorkloadQuery extends PageParams {
  projectId?: number
  agentCompanyId?: number
  period?: string
  approvalStatus?: SettlementApprovalStatus
  paymentStatus?: PaymentStatus
}

/* ============ DTO 类型 ============ */

export interface FinanceBudgetDTO {
  id?: number
  projectId: number
  year: number
  laborAmount?: number
  travelAmount?: number
  agentAmount?: number
  otherAmount?: number
  remark?: string
}

export interface FinanceCostDTO {
  id?: number
  projectId: number
  costType: FinanceCostType
  amount: number
  costDate: string
  refType?: CostRefType
  refId?: number
  description?: string
}

export interface FinanceWorkloadDTO {
  id?: number
  projectId: number
  outsourceTaskId?: number
  agentCompanyId: number
  period: string
  workloadDays: number
  unitPrice: number
  travelAmount?: number
  otherAmount?: number
  remark?: string
}
