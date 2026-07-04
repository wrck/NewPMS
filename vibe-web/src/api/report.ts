/**
 * 报表分析模块 API 封装
 * 对应后端：
 *   - 管理驾驶舱：/api/v1/cockpit（独立模块，非 /report 子路径）
 *   - 工作台首页：/api/v1/dashboard（独立模块）
 *   - 业务报表：/api/v1/report/{project|device|resource|finance}（待实现）
 */
import { http } from '@/utils/request'

/** 业务报表基础路径（project/device/resource/finance 报表） */
const BASE = '/report'
/** 管理驾驶舱基础路径（独立模块） */
const COCKPIT_BASE = '/cockpit'
/** 工作台首页基础路径（独立模块） */
const DASHBOARD_BASE = '/dashboard'

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

/* ============ Dashboard 类型（与后端 DashboardVO 对齐） ============ */

/** 驾驶舱核心指标（CockpitStatVO） */
export interface CockpitStat {
  projectCount: number
  activeProjectCount: number
  lastMonthProjectCount: number
  projectGrowthRate: number
  deviceCount: number
  onlineDeviceCount: number
  lastMonthDeviceCount: number
  deviceGrowthRate: number
  engineerCount: number
  activeEngineerCount: number
  lastMonthEngineerCount: number
  engineerGrowthRate: number
  agentCompanyCount: number
  activeAgentCount: number
  lastMonthAgentCount: number
  agentGrowthRate: number
}

/** 图表数据项（ChartDataVO） */
export interface ChartDataItem {
  name?: string
  value?: number
  month?: string
  newCount?: number
  completedCount?: number
}

/** 项目列表项（ProjectItemVO） */
export interface ProjectItem {
  projectId: number
  projectCode?: string
  projectName: string
  status: string
  currentPhase?: string
  projectType?: string
  priority?: string
  progressPct?: number
  region?: string
  plannedStart?: string
  plannedEnd?: string
  pmName?: string
  customerName?: string
}

/** 任务列表项（TaskItemVO） */
export interface TaskItem {
  taskId: number
  taskName: string
  taskType?: string
  status: string
  projectId?: number
  projectName?: string
  projectCode?: string
  executeMode?: string
  priority?: string
  plannedStart?: string
  plannedEnd?: string
  overdue?: number
}

/** 风险项目（RiskProjectVO） */
export interface RiskProject {
  projectId: number
  projectCode?: string
  projectName: string
  riskType: string
  riskTypeName?: string
  description?: string
  progressPct?: number
  pmName?: string
  plannedEnd?: string
  status: string
}

/** 交付物列表项（DeliverableItemVO） */
export interface DeliverableItem {
  deliverableId: number
  outsourceTaskId?: number
  projectTaskId?: number
  deliverableType?: string
  fileName?: string
  fileUrl?: string
  remark?: string
  submitTime?: string
  projectId?: number
  projectName?: string
  taskName?: string
  agentCompanyId?: number
  agentCompanyName?: string
  outsourceStatus?: string
  deadline?: string
}

/** 转包任务列表项（OutsourceTaskItemVO） */
export interface OutsourceTaskItem {
  outsourceTaskId: number
  projectTaskId?: number
  taskName: string
  projectId?: number
  projectCode?: string
  projectName?: string
  agentCompanyId?: number
  agentCompanyName?: string
  agentEngineerId?: number
  agentEngineerName?: string
  status: string
  taskScope?: string
  deadline?: string
  submitCount?: number
  createTime?: string
  overdue?: number
}

/** 总监首页数据（DirectorDashboardVO） */
export interface DirectorDashboard {
  stats: CockpitStat
  pendingApprovalCount: number
  pendingChangeCount: number
  pendingWorkloadCount: number
  projectStatusDist: ChartDataItem[]
  projectTrend: ChartDataItem[]
  riskProjects: RiskProject[]
}

/** PM 首页数据（PmDashboardVO） */
export interface PmDashboard {
  myProjectCount: number
  activeProjectCount: number
  pendingDispatchCount: number
  pendingReviewCount: number
  riskProjectCount: number
  myProjects: ProjectItem[]
  pendingDispatchTasks: TaskItem[]
  pendingReviewDeliverables: DeliverableItem[]
}

/** 工程师首页数据（EngineerDashboardVO） */
export interface EngineerDashboard {
  todayTaskCount: number
  pendingTaskCount: number
  overdueTaskCount: number
  todayWorkHours: number
  weekWorkHours: number
  monthWorkHours: number
  todayTasks: TaskItem[]
  overdueTasks: TaskItem[]
}

/** 代理商首页数据（AgentDashboardVO） */
export interface AgentDashboard {
  totalCount: number
  pendingCount: number
  inProgressCount: number
  submittedCount: number
  overdueCount: number
  pendingTasks: OutsourceTaskItem[]
  inProgressTasks: OutsourceTaskItem[]
  submittedTasks: OutsourceTaskItem[]
}

/** 工作台首页统一 VO（DashboardVO） */
export interface DashboardVO {
  role: string
  realName: string
  director?: DirectorDashboard
  pm?: PmDashboard
  engineer?: EngineerDashboard
  agent?: AgentDashboard
}

/** 获取管理驾驶舱数据 */
export function getCockpit() {
  return http.get<CockpitData>(COCKPIT_BASE)
}

/** 仪表盘数据（首页）- 按当前用户角色差异化返回 */
export function getDashboard() {
  return http.get<DashboardVO>(DASHBOARD_BASE)
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
