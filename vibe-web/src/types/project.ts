/**
 * 项目管理模块类型定义
 * 对应后端：/api/v1/projects、/api/v1/projects/{id}/phases|tasks|milestones|risks|issues|changes|members|comments
 *           /api/v1/project-templates
 */
import type { PageParams } from './api'
import type { Priority } from './enum'
import { ProjectStatus, TaskStatus } from './enum'

/** 项目执行模式 */
export type ExecuteMode = 'SELF' | 'AGENT' | 'MIXED'

/** 项目类型 */
export type ProjectType = 'NEW' | 'EXPAND' | 'REFORM' | 'REPLACE' | 'SECURITY'

/** 产品线 */
export type ProductLine = 'ROUTER' | 'SWITCH' | 'WIRELESS' | 'SECURITY' | 'DC' | 'OTHER'

/** 阶段编码 */
export type PhaseCode = 'SURVEY' | 'DESIGN' | 'DELIVER' | 'INSTALL' | 'DEBUG' | 'ACCEPT'

/** 阶段状态 */
export type PhaseStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED'

/** 任务类型 */
export type TaskType = 'SURVEY' | 'INSTALL' | 'DEBUG' | 'CUTOVER' | 'ACCEPT' | 'OTHER'

/** 项目查询参数 */
export interface ProjectQueryParams extends PageParams {
  status?: ProjectStatus
  priority?: Priority
  executeMode?: ExecuteMode
  productLine?: ProductLine
  pmId?: number
  region?: string
  customerId?: number
  startBegin?: string
  startEnd?: string
}

/** 项目列表项 */
export interface Project {
  id: number
  projectCode: string
  projectName: string
  customerId: number
  customerName?: string
  projectType: ProjectType
  productLine: ProductLine
  executeMode: ExecuteMode
  priority: Priority
  status: ProjectStatus
  currentPhase?: string
  pmId: number
  pmName?: string
  region?: string
  contractNo?: string
  plannedStart?: string
  plannedEnd?: string
  actualStart?: string
  actualEnd?: string
  progressPct: number
  description?: string
  remark?: string
  createdAt?: string
  updatedAt?: string
  version?: number
}

/** 项目详情（聚合） */
export interface ProjectDetail extends Project {
  phases?: ProjectPhase[]
  milestones?: Milestone[]
  taskStats?: {
    total: number
    completed: number
    inProgress: number
    pending: number
    overdue: number
  }
  riskCount?: number
  issueCount?: number
  members?: ProjectMember[]
}

/** 项目阶段 */
export interface ProjectPhase {
  id: number
  projectId: number
  phaseCode: PhaseCode
  phaseName: string
  sortOrder: number
  status: PhaseStatus
  plannedStart?: string
  plannedEnd?: string
  actualStart?: string
  actualEnd?: string
  deliverables?: Array<{ name: string; required: boolean; submitted: boolean }>
  progressPct?: number
}

/** 项目任务 */
export interface ProjectTask {
  id: number
  projectId: number
  projectName?: string
  phaseId?: number
  phaseName?: string
  parentTaskId?: number
  parentTaskName?: string
  taskName: string
  taskType: TaskType
  status: TaskStatus
  executeMode: 'SELF' | 'AGENT'
  assigneeId?: number
  assigneeName?: string
  agentCompanyId?: number
  agentCompanyName?: string
  agentEngineerId?: number
  agentEngineerName?: string
  siteInfo?: { siteName?: string; address?: string; contact?: string; phone?: string }
  deviceIds?: number[]
  plannedStart?: string
  plannedEnd?: string
  actualStart?: string
  actualEnd?: string
  priority: Priority
  description?: string
  attachments?: Array<{ name: string; url: string }>
  progressPct?: number
  createdAt?: string
  updatedAt?: string
}

/** 项目任务查询参数 */
export interface ProjectTaskQueryParams extends PageParams {
  projectId?: number
  phaseId?: number
  status?: TaskStatus
  executeMode?: 'SELF' | 'AGENT'
  assigneeId?: number
  agentCompanyId?: number
  priority?: Priority
  overdue?: boolean
}

/** 里程碑 */
export interface Milestone {
  id: number
  projectId: number
  milestoneName: string
  plannedDate: string
  actualDate?: string
  deliverables?: string
  status: 'PENDING' | 'ACHIEVED' | 'OVERDUE'
  remark?: string
}

/** 风险登记 */
export interface ProjectRisk {
  id: number
  projectId: number
  riskDesc: string
  impact: 'LOW' | 'MEDIUM' | 'HIGH'
  probability: 'LOW' | 'MEDIUM' | 'HIGH'
  response?: string
  ownerId?: number
  ownerName?: string
  status: 'OPEN' | 'PROCESSING' | 'CLOSED'
  dueDate?: string
  createdAt?: string
}

/** 问题跟踪 */
export interface ProjectIssue {
  id: number
  projectId: number
  issueDesc: string
  impact?: string
  ownerId?: number
  ownerName?: string
  status: 'OPEN' | 'PROCESSING' | 'RESOLVED' | 'CLOSED'
  dueDate?: string
  resolvedAt?: string
  remark?: string
  createdAt?: string
}

/** 变更记录 */
export interface ProjectChange {
  id: number
  projectId: number
  changeType: 'SCOPE' | 'TIME' | 'RESOURCE' | 'COST' | 'OTHER'
  title: string
  reason: string
  beforeSnapshot?: string
  afterSnapshot?: string
  impactAnalysis?: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXECUTED'
  applicantId?: number
  applicantName?: string
  approverId?: number
  approverName?: string
  approvedAt?: string
  createdAt?: string
}

/** 项目成员 */
export interface ProjectMember {
  id: number
  projectId: number
  userId: number
  userName: string
  realName?: string
  role: 'PM' | 'ENGINEER' | 'TECH_LEAD' | 'QA' | 'VIEWER'
  joinedAt?: string
}

/** 项目沟通记录 */
export interface ProjectComment {
  id: number
  projectId: number
  userId: number
  userName: string
  content: string
  attachments?: Array<{ name: string; url: string }>
  createdAt: string
}

/** 项目模板 */
export interface ProjectTemplate {
  id: number
  templateName: string
  projectType?: ProjectType
  productLine?: ProductLine
  description?: string
  phases?: Array<{ phaseCode: PhaseCode; phaseName: string; sortOrder: number }>
  tasks?: Array<{ taskName: string; taskType: TaskType; phaseCode?: PhaseCode; plannedDays?: number }>
  status?: 'ENABLED' | 'DISABLED'
  createdAt?: string
}

/** 看板分组项 */
export interface ProjectKanbanGroup {
  status: ProjectStatus
  statusName: string
  count: number
  projects: Project[]
}

/** 甘特图数据 */
export interface ProjectGantt {
  project: { id: number; projectName: string; plannedStart: string; plannedEnd: string }
  phases: Array<{
    id: number
    name: string
    start: string
    end: string
    status: PhaseStatus
    progress: number
  }>
  tasks: Array<{
    id: number
    name: string
    phaseId?: number
    start: string
    end: string
    status: TaskStatus
    progress: number
    assigneeName?: string
    dependencies?: number[]
  }>
  milestones: Array<{ id: number; name: string; date: string; achieved: boolean }>
}

/** 状态流转 DTO */
export interface ProjectStatusDTO {
  id?: number
  targetStatus: ProjectStatus
  remark?: string
  version?: number
}

/** 项目创建/编辑 DTO */
export interface ProjectSaveDTO {
  id?: number
  projectName: string
  customerId: number
  projectType: ProjectType
  productLine: ProductLine
  executeMode: ExecuteMode
  priority: Priority
  pmId: number
  region?: string
  contractNo?: string
  plannedStart?: string
  plannedEnd?: string
  description?: string
  remark?: string
  templateId?: number
}

/** 任务 DTO */
export interface ProjectTaskDTO {
  id?: number
  projectId?: number
  phaseId?: number
  parentTaskId?: number
  taskName: string
  taskType: TaskType
  executeMode: 'SELF' | 'AGENT'
  assigneeId?: number
  agentCompanyId?: number
  agentEngineerId?: number
  siteInfo?: ProjectTask['siteInfo']
  deviceIds?: number[]
  plannedStart?: string
  plannedEnd?: string
  priority: Priority
  description?: string
}

/** 任务派发 DTO */
export interface TaskDispatchDTO {
  executeMode: 'SELF' | 'AGENT'
  assigneeId?: number
  agentCompanyId?: number
  agentEngineerId?: number
  remark?: string
}

/** 任务转派 DTO */
export interface TaskTransferDTO {
  toAssigneeId?: number
  toAgentEngineerId?: number
  reason: string
}

/** 任务退回 DTO */
export interface TaskReturnDTO {
  reason: string
}

/** 任务进度更新 DTO */
export interface TaskProgressDTO {
  status: TaskStatus
  progressPct: number
  remark?: string
  version?: number
}

export { ProjectStatus, TaskStatus }
