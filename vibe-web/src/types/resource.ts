/**
 * 资源调度模块类型定义
 * 对应后端：/api/v1/engineers、/api/v1/schedules、/api/v1/dispatches、/api/v1/timesheets
 */
import type { PageParams } from './api'

/** 工程师状态 */
export type EngineerStatus = 'ACTIVE' | 'ON_LEAVE' | 'RESIGNED'

/** 技能等级 */
export type SkillLevel = 'JUNIOR' | 'INTERMEDIATE' | 'SENIOR' | 'EXPERT'

/** 工程师 */
export interface Engineer {
  id: number
  engineerNo: string
  name: string
  phone?: string
  email?: string
  orgId?: number
  orgName?: string
  region?: string
  status: EngineerStatus
  skills?: Array<{ name: string; level: SkillLevel }>
  certifications?: Array<{ name: string; acquiredAt?: string; validUntil?: string }>
  joinedAt?: string
  utilization?: number
  ongoingTaskCount?: number
  avatar?: string
}

/** 排期项 */
export interface Schedule {
  id: number
  engineerId: number
  engineerName?: string
  type: 'TASK' | 'LEAVE' | 'TRAINING' | 'MEETING' | 'BUSINESS_TRIP'
  title: string
  startDate: string
  endDate: string
  projectId?: number
  projectName?: string
  taskId?: number
  taskName?: string
  status?: 'PLANNED' | 'CONFIRMED' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED'
  remark?: string
}

/** 派发任务（待派发任务项） */
export interface DispatchTask {
  id: number
  taskName: string
  projectId: number
  projectName?: string
  phaseName?: string
  taskType: string
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
  executeMode: 'SELF' | 'AGENT'
  plannedStart?: string
  plannedEnd?: string
  region?: string
  requiredSkills?: string[]
  siteInfo?: { siteName?: string; address?: string }
  status: string
  assigneeId?: number
  assigneeName?: string
}

/** 派发推荐候选 */
export interface DispatchCandidate {
  engineerId: number
  engineerName: string
  region?: string
  utilization: number
  ongoingTaskCount: number
  matchScore: number
  matchReasons: string[]
  skills: Array<{ name: string; level: SkillLevel }>
  nextAvailableDate?: string
}

/** 工时记录 */
export interface Timesheet {
  id: number
  engineerId: number
  engineerName?: string
  projectId: number
  projectName?: string
  taskId?: number
  taskName?: string
  workDate: string
  hours: number
  overtimeHours?: number
  workType: 'NORMAL' | 'OVERTIME' | 'BUSINESS_TRIP' | 'WEEKEND'
  description?: string
  status: 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED'
  approverId?: number
  approverName?: string
  approvedAt?: string
  rejectReason?: string
  createdAt?: string
}

/** 工程师查询参数 */
export interface EngineerQueryParams extends PageParams {
  name?: string
  region?: string
  status?: EngineerStatus
  skillName?: string
  skillLevel?: SkillLevel
  orgId?: number
}

/** 排期查询参数 */
export interface ScheduleQueryParams {
  engineerId?: number
  engineerIds?: number[]
  startDate: string
  endDate: string
  projectId?: number
  type?: Schedule['type']
}

/** 派发任务查询参数 */
export interface DispatchQueryParams extends PageParams {
  projectId?: number
  status?: string
  executeMode?: 'SELF' | 'AGENT'
  priority?: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
  region?: string
  unassignedOnly?: boolean
}

/** 工时查询参数 */
export interface TimesheetQueryParams extends PageParams {
  engineerId?: number
  projectId?: number
  status?: Timesheet['status']
  startDate?: string
  endDate?: string
  workType?: Timesheet['workType']
}

/** 工程师 DTO */
export interface EngineerDTO {
  id?: number
  engineerNo: string
  name: string
  phone?: string
  email?: string
  orgId?: number
  region?: string
  status?: EngineerStatus
  skills?: Array<{ name: string; level: SkillLevel }>
  certifications?: Array<{ name: string; acquiredAt?: string; validUntil?: string }>
}

/** 工时填报 DTO */
export interface TimesheetDTO {
  id?: number
  engineerId: number
  projectId: number
  taskId?: number
  workDate: string
  hours: number
  overtimeHours?: number
  workType?: Timesheet['workType']
  description?: string
}

/** 派发 DTO */
export interface DispatchDTO {
  executeMode: 'SELF' | 'AGENT'
  assigneeId?: number
  agentCompanyId?: number
  agentEngineerId?: number
  remark?: string
}

/** 批量派发 DTO */
export interface BatchDispatchDTO {
  taskIds: number[]
  executeMode: 'SELF' | 'AGENT'
  assigneeId?: number
  agentCompanyId?: number
  agentEngineerId?: number
  remark?: string
}

/* ============ 差旅管理 ============ */

/** 差旅状态 */
export type BusinessTripStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED'

/** 交通方式 */
export type TransportMode = 'PLANE' | 'TRAIN' | 'CAR' | 'OTHER'

/** 差旅记录 */
export interface BusinessTrip {
  id: number
  engineerId: number
  engineerName?: string
  projectId?: number
  projectName?: string
  taskId?: number
  origin?: string
  destination?: string
  startDate?: string
  endDate?: string
  transportMode?: TransportMode
  accommodation?: string
  estimatedCost?: number
  actualCost?: number
  reason?: string
  status: BusinessTripStatus
  approverId?: number
  approverName?: string
  approveTime?: string
  remark?: string
  createTime?: string
}

/** 差旅查询参数 */
export interface BusinessTripQueryParams extends PageParams {
  engineerId?: number
  projectId?: number
  status?: BusinessTripStatus
  startDate?: string
  endDate?: string
}

/** 差旅新增/编辑 DTO */
export interface BusinessTripDTO {
  id?: number
  engineerId: number
  projectId?: number
  taskId?: number
  origin?: string
  destination?: string
  startDate?: string
  endDate?: string
  transportMode?: TransportMode
  accommodation?: string
  estimatedCost?: number
  actualCost?: number
  reason?: string
  remark?: string
}

/* ============ 工程师请假 ============ */

/** 请假状态 */
export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

/** 请假类型 */
export type LeaveType = 'ANNUAL' | 'SICK' | 'PERSONAL' | 'OTHER'

/** 请假记录 */
export interface EngineerLeave {
  id: number
  engineerId: number
  engineerName?: string
  startDate: string
  endDate: string
  leaveType: LeaveType
  reason?: string
  status: LeaveStatus
  createTime?: string
}

/** 请假查询参数 */
export interface EngineerLeaveQueryParams extends PageParams {
  engineerId?: number
  leaveType?: LeaveType
  status?: LeaveStatus
  startDate?: string
  endDate?: string
}

/** 请假新增/编辑 DTO */
export interface EngineerLeaveDTO {
  id?: number
  engineerId: number
  startDate: string
  endDate: string
  leaveType: LeaveType
  reason?: string
}
