/**
 * 交付管理模块 API 封装
 * 对应后端：/api/v1/work-orders、/api/v1/work-orders/{id}/steps|photos|issues
 */
import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  WorkOrder,
  WorkOrderStep,
  WorkOrderPhoto,
  WorkOrderIssue,
  WorkOrderQueryParams,
  WorkOrderCreateDTO,
  WorkOrderCheckinDTO,
  WorkOrderCheckoutDTO,
  WorkOrderConfirmDTO,
  WorkOrderStepDTO,
  WorkOrderIssueDTO
} from '@/types/delivery'

const BASE = '/work-orders'

/* ============ 工单 CRUD ============ */

export function pageWorkOrders(params: WorkOrderQueryParams) {
  return http.get<PageResult<WorkOrder>>(BASE, params as Record<string, unknown>)
}

export function getWorkOrderDetail(id: number) {
  return http.get<WorkOrder>(`${BASE}/${id}`)
}

export function createWorkOrder(dto: WorkOrderCreateDTO) {
  return http.post<number>(BASE, dto)
}

/** 移动端-我的工单（按状态筛选） */
export function listMyWorkOrders(status?: string) {
  return http.get<WorkOrder[]>(`${BASE}/me`, { status })
}

/* ============ 现场签到/签退/完成/确认 ============ */

export function checkinWorkOrder(id: number, dto: WorkOrderCheckinDTO, photo?: File) {
  const formData = new FormData()
  Object.entries(dto).forEach(([k, v]) => formData.append(k, String(v)))
  if (photo) formData.append('photo', photo)
  return http.post<WorkOrder>(`${BASE}/${id}/checkin`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function checkoutWorkOrder(id: number, dto: WorkOrderCheckoutDTO, photo?: File) {
  const formData = new FormData()
  Object.entries(dto).forEach(([k, v]) => formData.append(k, String(v)))
  if (photo) formData.append('photo', photo)
  return http.post<WorkOrder>(`${BASE}/${id}/checkout`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 工程师标记完成 */
export function completeWorkOrder(id: number, remark?: string) {
  return http.post<WorkOrder>(`${BASE}/${id}/complete`, undefined, {
    params: { remark }
  })
}

/** PM 确认完成 */
export function confirmWorkOrder(id: number, dto: WorkOrderConfirmDTO) {
  return http.post<WorkOrder>(`${BASE}/${id}/confirm`, dto)
}

/* ============ 工单步骤 ============ */

export function listWorkOrderSteps(workOrderId: number) {
  return http.get<WorkOrderStep[]>(`${BASE}/${workOrderId}/steps`)
}

export function updateWorkOrderStep(workOrderId: number, stepId: number, dto: WorkOrderStepDTO) {
  return http.put<void>(`${BASE}/${workOrderId}/steps/${stepId}`, dto)
}

/* ============ 工单照片 ============ */

export function listWorkOrderPhotos(workOrderId: number) {
  return http.get<WorkOrderPhoto[]>(`${BASE}/${workOrderId}/photos`)
}

export function uploadWorkOrderPhoto(workOrderId: number, file: File, stepId?: number, remark?: string) {
  const formData = new FormData()
  formData.append('file', file)
  if (stepId) formData.append('stepId', String(stepId))
  if (remark) formData.append('remark', remark)
  return http.post<number>(`${BASE}/${workOrderId}/photos`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteWorkOrderPhoto(workOrderId: number, id: number) {
  return http.delete<void>(`${BASE}/${workOrderId}/photos/${id}`)
}

/* ============ 工单异常问题 ============ */

export function listWorkOrderIssues(workOrderId: number) {
  return http.get<WorkOrderIssue[]>(`${BASE}/${workOrderId}/issues`)
}

export function reportWorkOrderIssue(workOrderId: number, dto: WorkOrderIssueDTO) {
  return http.post<number>(`${BASE}/${workOrderId}/issues`, dto)
}

export function resolveWorkOrderIssue(workOrderId: number, id: number, resolution: string) {
  return http.put<void>(`${BASE}/${workOrderId}/issues/${id}/resolve`, { resolution })
}
