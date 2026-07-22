/**
 * 代理商管理模块类型定义
 * 对应后端：/api/v1/agent-companies、/api/v1/outsource-tasks、
 *           /api/v1/outsource-tasks/{taskId}/deliverables、
 *           /api/v1/outsource-tasks/{taskId}/workload、
 *           /api/v1/agent-companies/{companyId}/scores、
 *           /api/v1/agent-companies/{companyId}/engineers
 */
import type { PageParams } from './api'
import type { OutsourceStatus } from './enum'

/** 代理商合作状态 */
export type AgentStatus = 'ACTIVE' | 'SUSPENDED' | 'TERMINATED'

/** 代理商公司 */
export interface AgentCompany {
  id: number
  companyName: string
  companyCode: string
  qualification?: string
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  address?: string
  serviceRegions?: string[]
  productLines?: string[]
  status: AgentStatus
  overallScore?: number
  cooperationStart?: string
  cooperationEnd?: string
  projectCount?: number
  totalAmount?: number
  remark?: string
  createdAt?: string
}

/** 代理商工程师 */
export interface AgentEngineer {
  id: number
  agentCompanyId: number
  agentCompanyName?: string
  name: string
  phone: string
  email?: string
  skills?: Array<{ name: string; level: 'JUNIOR' | 'INTERMEDIATE' | 'SENIOR' | 'EXPERT' }>
  certifications?: Array<{ name: string; validUntil?: string }>
  status: 'ACTIVE' | 'DISABLED'
  qualityScore?: number
  taskCount?: number
  joinedAt?: string
}

/** 转包任务 */
export interface OutsourceTask {
  id: number
  taskCode?: string
  projectId: number
  projectName?: string
  taskId?: number
  taskName?: string
  agentCompanyId: number
  agentCompanyName?: string
  agentEngineerId?: number
  agentEngineerName?: string
  taskScope: string
  deadline?: string
  status: OutsourceStatus
  submitCount?: number
  confirmedBy?: number
  confirmedByName?: string
  confirmedTime?: string
  rejectReason?: string
  attachments?: Array<{ name: string; url: string }>
  createdAt?: string
  updatedAt?: string
}

/** 转包任务交付物 */
export interface OutsourceDeliverable {
  id: number
  taskId: number
  deliverableType: 'PHOTO' | 'TEST_RECORD' | 'SIGN_OFF' | 'CONFIG' | 'OTHER'
  name: string
  url: string
  thumbnailUrl?: string
  uploadedBy?: number
  uploadedByName?: string
  uploadedAt?: string
  remark?: string
}

/** 工作量确认 */
export interface OutsourceWorkload {
  id: number
  taskId: number
  projectId: number
  projectName?: string
  agentCompanyId: number
  agentCompanyName?: string
  manDays: number
  siteCount?: number
  deviceCount?: number
  travelDays?: number
  otherCost?: number
  totalAmount?: number
  status: 'SUBMITTED' | 'CONFIRMED' | 'REJECTED'
  submittedBy?: number
  confirmedBy?: number
  confirmByName?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 代理商评分 */
export interface AgentScore {
  id: number
  agentCompanyId: number
  agentCompanyName?: string
  taskId?: number
  taskName?: string
  scorerId?: number
  scorerName?: string
  timeliness: number
  quality: number
  communication: number
  issueRate: number
  overallScore: number
  comment?: string
  scoredAt?: string
}

/** 代理商公司查询参数 */
export interface AgentCompanyQueryParams extends PageParams {
  companyName?: string
  status?: AgentStatus
  region?: string
  productLine?: string
}

/** 转包任务查询参数 */
export interface OutsourceTaskQueryParams extends PageParams {
  projectId?: number
  agentCompanyId?: number
  status?: OutsourceStatus
  startBegin?: string
  startEnd?: string
}

/** 工作量查询参数 */
export interface WorkloadQueryParams extends PageParams {
  agentCompanyId?: number
  projectId?: number
  status?: OutsourceWorkload['status']
  beginTime?: string
  endTime?: string
}

/** 代理商公司 DTO */
export interface AgentCompanyDTO {
  id?: number
  companyName: string
  companyCode: string
  qualification?: string
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  address?: string
  serviceRegions?: string[]
  productLines?: string[]
  status?: AgentStatus
  cooperationStart?: string
  cooperationEnd?: string
  remark?: string
}

/** 代理商工程师 DTO */
export interface AgentEngineerDTO {
  id?: number
  agentCompanyId: number
  name: string
  phone: string
  email?: string
  skills?: Array<{ name: string; level: 'JUNIOR' | 'INTERMEDIATE' | 'SENIOR' | 'EXPERT' }>
  certifications?: Array<{ name: string; validUntil?: string }>
  status?: 'ACTIVE' | 'DISABLED'
}

/** 转包任务创建 DTO */
export interface OutsourceTaskDTO {
  projectId: number
  taskId?: number
  agentCompanyId: number
  taskScope: string
  deadline?: string
  attachments?: Array<{ name: string; url: string }>
}

/** 工作量确认 DTO */
export interface WorkloadConfirmDTO {
  manDays: number
  siteCount?: number
  deviceCount?: number
  remark?: string
}

/** 评分 DTO */
export interface AgentScoreDTO {
  taskId?: number
  timeliness: number
  quality: number
  communication: number
  issueRate: number
  comment?: string
}
