/**
 * 报表分析模块 API 封装
 * 对应后端：/api/v1/report/{dashboard|cockpit|...}
 */
import { http } from '@/utils/request'

const BASE = '/report'

/** 管理驾驶舱核心指标 */
export interface CockpitKpi {
  ongoingProjects: number
  riskProjects: number
  overdueProjects: number
  monthNewProjects: number
  monthClosedProjects: number
  engineerUtilization: number
  deviceArrivalRate: number
  acceptanceCompletionRate: number
}

/** 项目阶段分布 */
export interface PhaseDistribution {
  phase: string
  phaseName: string
  count: number
}

/** 月度项目趋势 */
export interface ProjectTrend {
  month: string
  newCount: number
  closedCount: number
  ongoingCount: number
}

/** 待办事项 */
export interface TodoItem {
  id: number
  type: 'TASK' | 'APPROVAL' | 'ISSUE' | 'WORKLOAD'
  title: string
  description?: string
  dueDate?: string
  priority?: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
  link?: string
}

/** 风险预警项 */
export interface RiskWarning {
  id: number
  projectId: number
  projectName: string
  riskType: 'PROGRESS' | 'DEVICE' | 'RESOURCE' | 'AGENT' | 'OTHER'
  description: string
  level: 'LOW' | 'MEDIUM' | 'HIGH'
  detectedAt: string
}

/** 操作动态时间线 */
export interface Activity {
  id: number
  type: string
  title: string
  description?: string
  operatorId?: number
  operatorName?: string
  operatedAt: string
}

/** 管理驾驶舱聚合数据 */
export interface CockpitData {
  kpi: CockpitKpi
  phaseDistribution: PhaseDistribution[]
  projectTrend: ProjectTrend[]
  todoList: TodoItem[]
  riskWarnings: RiskWarning[]
  recentActivities: Activity[]
}

/** 获取管理驾驶舱数据 */
export function getCockpit() {
  return http.get<CockpitData>(`${BASE}/cockpit`)
}

/** 仪表盘数据（首页） */
export function getDashboard() {
  return http.get<{
    myTasks: Array<{ id: number; name: string; dueDate?: string; status: string; projectName?: string }>
    myProjects: Array<{ id: number; projectName: string; status: string; progressPct: number; role: string }>
    notices: Array<{ id: number; title: string; type: string; createdAt: string; read: boolean }>
    stats: {
      ongoingProjects: number
      riskProjects: number
      monthDueProjects: number
      pendingDispatchTasks: number
    }
  }>(`${BASE}/dashboard`)
}

/** 项目报表 */
export function getProjectReport(params: {
  startDate?: string
  endDate?: string
  status?: string
  pmId?: number
  productLine?: string
  region?: string
}) {
  return http.get<{
    summary: { total: number; completed: number; ongoing: number; overdue: number; avgProgress: number }
    byStatus: Array<{ status: string; statusName: string; count: number }>
    byProductLine: Array<{ productLine: string; count: number }>
    byRegion: Array<{ region: string; count: number }>
    byPm: Array<{ pmId: number; pmName: string; total: number; completed: number; overdue: number }>
    detail: Array<{
      id: number
      projectCode: string
      projectName: string
      status: string
      progressPct: number
      pmName: string
      plannedStart: string
      plannedEnd: string
      actualEnd?: string
      overdue: boolean
    }>
  }>(`${BASE}/project`, params as Record<string, unknown>)
}

/** 设备报表 */
export function getDeviceReport(params: { startDate?: string; endDate?: string; productLine?: string }) {
  return http.get<{
    summary: { total: number; online: number; offline: number; abnormal: number }
    statusDistribution: Array<{ status: string; count: number }>
    productLineDistribution: Array<{ productLine: string; count: number }>
    bomCompletion: Array<{ projectId: number; projectName: string; totalQty: number; completedQty: number; rate: number }>
    inventoryStatus: Array<{ warehouseName: string; totalQty: number; warningQty: number }>
  }>(`${BASE}/device`, params as Record<string, unknown>)
}

/** 资源报表 */
export function getResourceReport(params: { startDate?: string; endDate?: string; engineerId?: number; orgId?: number }) {
  return http.get<{
    summary: { totalEngineers: number; avgUtilization: number; totalHours: number; overtimeHours: number }
    byEngineer: Array<{
      engineerId: number
      engineerName: string
      taskCount: number
      hours: number
      overtimeHours: number
      utilization: number
      onTimeRate: number
    }>
    byProject: Array<{ projectId: number; projectName: string; hours: number; engineerCount: number }>
  }>(`${BASE}/resource`, params as Record<string, unknown>)
}

/** 财务报表 */
export function getFinanceReport(params: { startDate?: string; endDate?: string; customerId?: number }) {
  return http.get<{
    summary: { totalRevenue: number; totalCost: number; totalProfit: number; profitMargin: number }
    byCustomer: Array<{ customerId: number; customerName: string; revenue: number; cost: number; profit: number }>
    byRegion: Array<{ region: string; revenue: number; cost: number; profit: number }>
    byProductLine: Array<{ productLine: string; revenue: number; cost: number; profit: number }>
    agentSettlement: Array<{
      agentCompanyId: number
      agentCompanyName: string
      totalAmount: number
      paidAmount: number
      pendingAmount: number
    }>
  }>(`${BASE}/finance`, params as Record<string, unknown>)
}
