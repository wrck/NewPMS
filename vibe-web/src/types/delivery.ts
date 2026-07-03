/**
 * 交付管理模块类型定义
 * 对应后端：/api/v1/work-orders、/api/v1/work-orders/{id}/steps|photos|issues
 */
import type { PageParams } from './api'
import type { TaskStatus } from './enum'

/** 工单状态（与任务状态共用语义） */
export type WorkOrderStatus = TaskStatus

/** 工单 */
export interface WorkOrder {
  id: number
  workOrderNo: string
  workOrderName: string
  projectId: number
  projectName?: string
  taskId?: number
  taskName?: string
  engineerId?: number
  engineerName?: string
  agentCompanyId?: number
  agentCompanyName?: string
  agentEngineerId?: number
  agentEngineerName?: string
  executeMode: 'SELF' | 'AGENT'
  status: WorkOrderStatus
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
  siteInfo?: { siteName?: string; address?: string; contact?: string; phone?: string; longitude?: number; latitude?: number }
  plannedStart?: string
  plannedEnd?: string
  actualStart?: string
  actualEnd?: string
  checkinTime?: string
  checkoutTime?: string
  checkinLocation?: { longitude: number; latitude: number; address?: string }
  checkoutLocation?: { longitude: number; latitude: number; address?: string }
  description?: string
  remark?: string
  stepProgress?: { total: number; completed: number }
  photoCount?: number
  issueCount?: number
  createdAt?: string
  updatedAt?: string
}

/** 工单步骤 */
export interface WorkOrderStep {
  id: number
  workOrderId: number
  stepOrder: number
  stepName: string
  description?: string
  estimatedMinutes?: number
  actualMinutes?: number
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'SKIPPED'
  completedAt?: string
  operatorId?: number
  operatorName?: string
  remark?: string
  photos?: Array<{ id: number; url: string; thumbnailUrl?: string }>
}

/** 工单照片 */
export interface WorkOrderPhoto {
  id: number
  workOrderId: number
  stepId?: number
  stepName?: string
  url: string
  thumbnailUrl?: string
  watermarkedUrl?: string
  takenAt?: string
  gpsLocation?: { longitude: number; latitude: number; address?: string }
  uploadedBy?: number
  uploadedByName?: string
  remark?: string
  createdAt: string
}

/** 工单异常问题 */
export interface WorkOrderIssue {
  id: number
  workOrderId: number
  workOrderNo?: string
  description: string
  impact?: string
  severity: 'LOW' | 'MEDIUM' | 'HIGH'
  status: 'OPEN' | 'PROCESSING' | 'RESOLVED' | 'CLOSED'
  reporterId?: number
  reporterName?: string
  reportedAt?: string
  handlerId?: number
  handlerName?: string
  handledAt?: string
  resolution?: string
  photos?: Array<{ url: string; thumbnailUrl?: string }>
}

/** 工单查询参数 */
export interface WorkOrderQueryParams extends PageParams {
  projectId?: number
  engineerId?: number
  agentCompanyId?: number
  status?: WorkOrderStatus
  priority?: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
  executeMode?: 'SELF' | 'AGENT'
  startBegin?: string
  startEnd?: string
}

/** 工单创建 DTO */
export interface WorkOrderCreateDTO {
  workOrderName: string
  projectId: number
  taskId?: number
  engineerId?: number
  agentCompanyId?: number
  agentEngineerId?: number
  executeMode: 'SELF' | 'AGENT'
  priority?: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
  siteInfo?: WorkOrder['siteInfo']
  plannedStart?: string
  plannedEnd?: string
  description?: string
  standardSteps?: Array<{ stepName: string; description?: string; estimatedMinutes?: number }>
}

/** 签到 DTO */
export interface WorkOrderCheckinDTO {
  longitude: number
  latitude: number
  address?: string
  remark?: string
}

/** 签退 DTO */
export interface WorkOrderCheckoutDTO {
  longitude: number
  latitude: number
  address?: string
  remark?: string
}

/** 完成确认 DTO */
export interface WorkOrderConfirmDTO {
  approved: boolean
  remark?: string
  rating?: number
}

/** 步骤更新 DTO */
export interface WorkOrderStepDTO {
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'SKIPPED'
  actualMinutes?: number
  remark?: string
}

/** 异常问题上报 DTO */
export interface WorkOrderIssueDTO {
  description: string
  impact?: string
  severity: 'LOW' | 'MEDIUM' | 'HIGH'
  photoUrls?: string[]
}
