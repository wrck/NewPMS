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
  /**
   * 主键 ID（雪花算法 Long，后端序列化为字符串以避免 JS 精度丢失）
   * 实际运行时类型为 string，类型签名保留 string | number 以兼容历史调用方
   */
  id: string | number
  projectCode: string
  projectName: string
  customerId: string | number
  customerName?: string
  projectType: ProjectType
  productLine: ProductLine
  executeMode: ExecuteMode
  priority: Priority
  status: ProjectStatus
  currentPhase?: string
  pmId: string | number
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

/** 项目详情（聚合，对齐后端 ProjectDetailVO 扁平字段） */
export interface ProjectDetail extends Project {
  phases?: ProjectPhase[]
  milestones?: Milestone[]
  members?: ProjectMember[]
  /** 任务总数（后端扁平字段 taskTotal） */
  taskTotal?: number
  /** 已完成任务数（后端扁平字段 taskCompleted） */
  taskCompleted?: number
  /** 进行中任务数（后端扁平字段 taskInProgress） */
  taskInProgress?: number
  /** 待分配任务数（后端扁平字段 taskPending） */
  taskPending?: number
  riskCount?: number
  issueCount?: number
  /** @deprecated 兼容旧嵌套对象 taskStats */
  taskStats?: {
    total: number
    completed: number
    inProgress: number
    pending: number
    overdue: number
  }
}

/** 项目阶段（对齐后端 ProjectPhaseVO） */
export interface ProjectPhase {
  id: string | number
  projectId: string | number
  phaseCode: PhaseCode
  phaseName: string
  sortOrder: number
  status: PhaseStatus
  plannedStart?: string
  plannedEnd?: string
  actualStart?: string
  actualEnd?: string
  deliverables?: string
  progressPct?: number
  createTime?: string
}

/** 项目任务（对齐后端 ProjectTaskVO） */
export interface ProjectTask {
  id: string | number
  projectId: string | number
  projectName?: string
  phaseId?: string | number
  phaseName?: string
  parentTaskId?: string | number
  parentTaskName?: string
  taskName: string
  taskType: TaskType
  status: TaskStatus
  executeMode: 'SELF' | 'AGENT'
  assigneeId?: string | number
  assigneeName?: string
  agentCompanyId?: string | number
  agentCompanyName?: string
  agentEngineerId?: string | number
  agentEngineerName?: string
  siteInfo?: string
  deviceIds?: string
  plannedStart?: string
  plannedEnd?: string
  actualStart?: string
  actualEnd?: string
  priority: Priority
  description?: string
  attachments?: string
  version?: number
  createTime?: string
  updateTime?: string
  /** @deprecated 后端 VO 无此字段，保留仅为前端兼容 */
  progressPct?: number
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

/** 里程碑（对齐后端 ProjectMilestoneVO） */
export interface Milestone {
  id: string | number
  projectId: string | number
  milestoneName: string
  plannedDate?: string
  actualDate?: string
  deliverables?: string
  status?: 'PENDING' | 'ACHIEVED' | 'OVERDUE'
  createTime?: string
}

/** 风险登记（对齐后端 ProjectRiskVO/DTO，字段 measure 非 response） */
export interface ProjectRisk {
  id: string | number
  projectId: string | number
  riskDesc: string
  impact: 'LOW' | 'MEDIUM' | 'HIGH'
  probability: 'LOW' | 'MEDIUM' | 'HIGH'
  /** 应对措施（后端字段名 measure） */
  measure?: string
  ownerId?: string | number
  ownerName?: string
  status: 'OPEN' | 'PROCESSING' | 'CLOSED'
  dueDate?: string
  /** 是否超期 */
  overdue?: boolean
  createdAt?: string
}

/** 问题跟踪（对齐后端 ProjectIssueVO） */
export interface ProjectIssue {
  id: string | number
  projectId: string | number
  taskId?: string | number
  issueDesc: string
  impact?: string
  ownerId?: string | number
  ownerName?: string
  status: 'OPEN' | 'PROCESSING' | 'RESOLVED' | 'CLOSED'
  dueDate?: string
  /** 解决时间（后端字段名 resolvedTime） */
  resolvedTime?: string
  overdue?: boolean
  remark?: string
  createdAt?: string
}

/** 变更记录（对齐后端 ProjectChangeLogVO/DTO，字段 changeContent 非 title） */
export interface ProjectChange {
  id: string | number
  projectId: string | number
  changeType: 'SCOPE' | 'TIME' | 'RESOURCE' | 'OTHER'
  /** 变更内容（后端字段名 changeContent） */
  changeContent: string
  reason: string
  impactAnalysis?: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXECUTED'
  applicantId?: string | number
  applicantName?: string
  approverId?: string | number
  approverName?: string
  approveTime?: string
  createTime?: string
  /** @deprecated 兼容旧字段名 createdAt */
  createdAt?: string
}

/** 项目成员（对齐后端 ProjectMemberVO，无 realName 字段） */
export interface ProjectMember {
  id: string | number
  projectId: string | number
  userId: string | number
  userName: string
  /** @deprecated 后端 VO 无 realName，保留仅为前端兼容 */
  realName?: string
  role: 'PM' | 'ENGINEER' | 'TECH_LEAD' | 'QA' | 'VIEWER'
  joinTime?: string
  /** 兼容旧字段名 joinedAt */
  joinedAt?: string
}

/** 项目沟通记录（对齐后端 ProjectCommentVO） */
export interface ProjectComment {
  id: string | number
  projectId: string | number
  taskId?: string | number
  content: string
  authorId?: string | number
  authorName?: string
  parentId?: string | number
  replies?: ProjectComment[]
  createTime?: string
  /** @deprecated 兼容旧字段名 createdAt */
  createdAt?: string
  /** @deprecated 兼容旧字段名 userName */
  userName?: string
}

/** 项目模板阶段 */
export interface ProjectTemplatePhase {
  id: number
  templateId?: number
  phaseCode: PhaseCode | string
  phaseName: string
  sortOrder?: number
  /** 交付物清单（JSON 字符串） */
  deliverables?: string
}

/** 项目模板任务 */
export interface ProjectTemplateTask {
  id: number
  templateId?: number
  phaseCode: PhaseCode | string
  taskName: string
  taskType?: TaskType | string
  description?: string
  /** 默认工期（天） */
  defaultDays?: number
}

/** 项目模板（含阶段、任务详情） */
export interface ProjectTemplate {
  id: string | number
  templateName: string
  projectType?: ProjectType
  productLine?: ProductLine
  description?: string
  phases?: ProjectTemplatePhase[]
  tasks?: ProjectTemplateTask[]
  /** 模板状态：1-启用 0-禁用（对齐后端 Integer status） */
  status?: number
  createTime?: string
  /** @deprecated 兼容旧字段名 createdAt */
  createdAt?: string
}

/** 客户档案 */
export interface Customer {
  id: number
  customerName: string
  customerCode: string
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  address?: string
  region?: string
  industry?: string
  remark?: string
  createTime?: string
}

/** 客户查询参数 */
export interface CustomerQueryParams extends PageParams {
  customerName?: string
  customerCode?: string
  region?: string
  industry?: string
}

/** 客户新增/编辑 DTO */
export interface CustomerDTO {
  id?: number
  customerName: string
  customerCode: string
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  address?: string
  region?: string
  industry?: string
  remark?: string
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
  id?: string | number
  projectName: string
  customerId: string | number
  projectType: ProjectType
  productLine: ProductLine
  executeMode: ExecuteMode
  priority: Priority
  pmId: string | number
  region?: string
  contractNo?: string
  plannedStart?: string
  plannedEnd?: string
  description?: string
  remark?: string
  templateId?: string | number
}

/** 任务 DTO（对齐后端 ProjectTaskDTO） */
export interface ProjectTaskDTO {
  id?: string | number
  projectId?: string | number
  phaseId?: string | number
  parentTaskId?: string | number
  taskName: string
  taskType: TaskType
  executeMode: 'SELF' | 'AGENT'
  assigneeId?: string | number
  agentCompanyId?: string | number
  agentEngineerId?: string | number
  siteInfo?: string
  /** 关联设备ID列表（JSON 字符串，后端为 String 类型） */
  deviceIds?: string
  /** 附件列表（JSON 字符串） */
  attachments?: string
  plannedStart?: string
  plannedEnd?: string
  priority: Priority
  description?: string
}

/** 任务派发 DTO（对齐后端 TaskDispatchDTO） */
export interface TaskDispatchDTO {
  executeMode: 'SELF' | 'AGENT'
  assigneeId?: string | number
  agentCompanyId?: string | number
  agentEngineerId?: string | number
  /** 任务范围 */
  taskScope?: string
  /** 截止日期 yyyy-MM-dd（后端为 String 类型） */
  deadline?: string
}

/** 任务转派 DTO（对齐后端 TaskTransferDTO，字段名 newAssigneeId/newAgentCompanyId/newAgentEngineerId） */
export interface TaskTransferDTO {
  newAssigneeId?: string | number
  newAgentCompanyId?: string | number
  newAgentEngineerId?: string | number
  reason: string
}

/** 任务退回 DTO */
export interface TaskReturnDTO {
  reason: string
}

/** 任务进度更新 DTO（对齐后端 TaskProgressDTO，字段名 targetStatus） */
export interface TaskProgressDTO {
  targetStatus: TaskStatus
  remark?: string
}

export { ProjectStatus, TaskStatus }
