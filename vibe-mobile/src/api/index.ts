/**
 * 业务 API 模块：工单 / 施工步骤 / 施工照片 / 异常问题 / 工时 / 站内信
 * 端点对齐后端 module-delivery / module-resource / module-system（baseURL = /api/v1）
 */
import { request, buildFormData } from '@/utils/request'
import type {
  GpsLocation,
  PageQuery,
  PageResult,
  TimesheetDTO,
  TimesheetStatsVO,
  TimesheetVO,
  SysNoticeVO,
  WorkOrderIssueReportDTO,
  WorkOrderIssueVO,
  WorkOrderPhotoVO,
  WorkOrderStepVO,
  WorkOrderVO
} from '@/types/api'

/* ============ 工单 ============ */

/** 工单分页查询参数（对齐后端 WorkOrderQueryDTO） */
export interface WorkOrderQuery extends PageQuery {
  /** 任务名称/项目名称（模糊） */
  keyword?: string
  /** 工程师ID */
  engineerId?: number | string
  /** 项目任务ID */
  taskId?: number | string
  /** 项目ID */
  projectId?: number | string
  /** 工单状态 */
  status?: string
  /** 签到开始日期 yyyy-MM-dd */
  checkinStart?: string
  /** 签到结束日期 yyyy-MM-dd */
  checkinEnd?: string
}

/** 兼容旧引用 */
export type TaskQuery = WorkOrderQuery

/** 分页查询工单列表 GET /work-orders */
export function fetchWorkOrders(params: WorkOrderQuery) {
  return request.get<PageResult<WorkOrderVO>>('/work-orders', params as any)
}

/** 兼容旧引用：fetchTasks = fetchWorkOrders */
export const fetchTasks = fetchWorkOrders

/** 工单详情 GET /work-orders/{id} */
export function fetchWorkOrderDetail(id: string | number) {
  return request.get<WorkOrderVO>(`/work-orders/${id}`)
}

/** 兼容旧引用 */
export const fetchTaskDetail = fetchWorkOrderDetail

/** 我的工单（今日任务） GET /work-orders/me?status=xxx */
export function fetchMyWorkOrders(status?: string) {
  return request.get<WorkOrderVO[]>('/work-orders/me', status ? { status } : undefined)
}

/** 兼容旧引用：fetchTodayTasks = fetchMyWorkOrders */
export const fetchTodayTasks = fetchMyWorkOrders

/**
 * 现场签到 POST /work-orders/{id}/checkin (multipart/form-data)
 * 后端接收：location(GpsLocation) + remark + photo(MultipartFile)
 */
export function checkIn(
  workOrderId: string | number,
  payload: {
    location: GpsLocation
    remark?: string
    photo?: File | Blob | null
  }
) {
  const form = buildFormData(new FormData(), {
    location: payload.location,
    remark: payload.remark || ''
  })
  if (payload.photo) {
    form.append('photo', payload.photo)
  }
  return request.postForm<WorkOrderVO>(`/work-orders/${workOrderId}/checkin`, form)
}

/**
 * 现场签退 POST /work-orders/{id}/checkout (multipart/form-data)
 * 后端接收：location(GpsLocation) + remark + photo(MultipartFile)
 */
export function checkOut(
  workOrderId: string | number,
  payload: {
    location: GpsLocation
    remark?: string
    photo?: File | Blob | null
  }
) {
  const form = buildFormData(new FormData(), {
    location: payload.location,
    remark: payload.remark || ''
  })
  if (payload.photo) {
    form.append('photo', payload.photo)
  }
  return request.postForm<WorkOrderVO>(`/work-orders/${workOrderId}/checkout`, form)
}

/** 工程师标记完成 POST /work-orders/{id}/complete?remark=xxx */
export function completeWorkOrder(workOrderId: string | number, remark?: string) {
  return request.post<WorkOrderVO>(
    `/work-orders/${workOrderId}/complete`,
    undefined,
    remark ? { params: { remark } } : undefined
  )
}

/** PM 确认完成 POST /work-orders/{id}/confirm */
export function confirmWorkOrder(
  workOrderId: string | number,
  payload?: { remark?: string; approved?: boolean }
) {
  return request.post<WorkOrderVO>(`/work-orders/${workOrderId}/confirm`, payload || {})
}

/* ============ 施工步骤 ============ */

/** 查询工单施工步骤列表 GET /work-orders/{workOrderId}/steps */
export function fetchWorkSteps(workOrderId: string | number) {
  return request.get<WorkOrderStepVO[]>(`/work-orders/${workOrderId}/steps`)
}

/** 兼容旧引用 */
export const fetchWorkOrderSteps = fetchWorkSteps

/**
 * 标记步骤完成 POST /work-orders/{workOrderId}/steps/{stepId}/complete
 * body: { remark?, skipped? }
 */
export function completeStep(
  workOrderId: string | number,
  stepId: string | number,
  payload?: { remark?: string; skipped?: boolean }
) {
  return request.post<WorkOrderStepVO>(
    `/work-orders/${workOrderId}/steps/${stepId}/complete`,
    payload || {}
  )
}

/* ============ 施工照片 ============ */

/**
 * 上传单张施工照片 POST /work-orders/{workOrderId}/photos (multipart/form-data)
 * 后端接收：file + stepId? + longitude?/latitude?/address? + takenTime?
 */
export function uploadWorkOrderPhoto(
  workOrderId: string | number,
  payload: {
    file: File | Blob
    stepId?: number | string
    longitude?: number
    latitude?: number
    address?: string
    takenTime?: string
  },
  onProgress?: (loaded: number, total: number) => void
) {
  const form = new FormData()
  form.append('file', payload.file)
  if (payload.stepId != null) form.append('stepId', String(payload.stepId))
  if (payload.longitude != null) form.append('longitude', String(payload.longitude))
  if (payload.latitude != null) form.append('latitude', String(payload.latitude))
  if (payload.address) form.append('address', payload.address)
  if (payload.takenTime) form.append('takenTime', payload.takenTime)
  return request.postForm<WorkOrderPhotoVO>(
    `/work-orders/${workOrderId}/photos`,
    form,
    onProgress
  )
}

/**
 * 批量上传施工照片 POST /work-orders/{workOrderId}/photos/batch (multipart/form-data)
 * 后端接收：files(MultipartFile[]) + 共享的 stepId/longitude/latitude/address/takenTime
 */
export function uploadWorkOrderPhotosBatch(
  workOrderId: string | number,
  payload: {
    files: (File | Blob)[]
    stepId?: number | string
    longitude?: number
    latitude?: number
    address?: string
    takenTime?: string
  },
  onProgress?: (loaded: number, total: number) => void
) {
  const form = new FormData()
  payload.files.forEach((f) => form.append('files', f))
  if (payload.stepId != null) form.append('stepId', String(payload.stepId))
  if (payload.longitude != null) form.append('longitude', String(payload.longitude))
  if (payload.latitude != null) form.append('latitude', String(payload.latitude))
  if (payload.address) form.append('address', payload.address)
  if (payload.takenTime) form.append('takenTime', payload.takenTime)
  return request.postForm<WorkOrderPhotoVO[]>(
    `/work-orders/${workOrderId}/photos/batch`,
    form,
    onProgress
  )
}

/** 查询工单照片列表 GET /work-orders/{workOrderId}/photos */
export function fetchWorkOrderPhotos(workOrderId: string | number) {
  return request.get<WorkOrderPhotoVO[]>(`/work-orders/${workOrderId}/photos`)
}

/** 按步骤查询照片列表 GET /work-orders/{workOrderId}/photos/step/{stepId} */
export function fetchWorkOrderPhotosByStep(
  workOrderId: string | number,
  stepId: string | number
) {
  return request.get<WorkOrderPhotoVO[]>(`/work-orders/${workOrderId}/photos/step/${stepId}`)
}

/** 删除照片 DELETE /work-orders/{workOrderId}/photos/{photoId} */
export function deleteWorkOrderPhoto(workOrderId: string | number, photoId: string | number) {
  return request.delete<void>(`/work-orders/${workOrderId}/photos/${photoId}`)
}

/* ============ 异常问题 ============ */

/** 上报异常 POST /work-orders/{workOrderId}/issues */
export function reportIssue(
  workOrderId: string | number,
  payload: WorkOrderIssueReportDTO
) {
  return request.post<number>(`/work-orders/${workOrderId}/issues`, payload)
}

/** 异常列表 GET /work-orders/{workOrderId}/issues */
export function fetchWorkOrderIssues(workOrderId: string | number) {
  return request.get<WorkOrderIssueVO[]>(`/work-orders/${workOrderId}/issues`)
}

/** 异常详情 GET /work-orders/{workOrderId}/issues/{issueId} */
export function fetchWorkOrderIssueDetail(
  workOrderId: string | number,
  issueId: string | number
) {
  return request.get<WorkOrderIssueVO>(`/work-orders/${workOrderId}/issues/${issueId}`)
}

/* ============ 工时 ============ */

/** 工时分页查询参数（对齐后端 TimesheetQueryDTO） */
export interface TimesheetQuery extends PageQuery {
  engineerId?: number | string
  projectId?: number | string
  taskId?: number | string
  status?: string
  workDateStart?: string
  workDateEnd?: string
}

/** 分页查询工时 GET /timesheets */
export function fetchTimesheets(params: TimesheetQuery) {
  return request.get<PageResult<TimesheetVO>>('/timesheets', params as any)
}

/** 工时详情 GET /timesheets/{id} */
export function fetchTimesheetDetail(id: string | number) {
  return request.get<TimesheetVO>(`/timesheets/${id}`)
}

/** 工时填报 POST /timesheets */
export function submitTimesheet(payload: TimesheetDTO) {
  return request.post<number>('/timesheets', payload)
}

/** 编辑工时 PUT /timesheets/{id} */
export function updateTimesheet(id: string | number, payload: TimesheetDTO) {
  return request.put<void>(`/timesheets/${id}`, payload)
}

/** 删除工时 DELETE /timesheets/{id} */
export function deleteTimesheet(id: string | number) {
  return request.delete<void>(`/timesheets/${id}`)
}

/**
 * 工时汇总 GET /timesheets/summary?engineerId=&startDate=&endDate=
 * 后端返回 TimesheetStatsVO（包含 totalHours/totalOvertimeHours/totalTravelDays 等）
 */
export function fetchTimesheetSummary(params?: {
  engineerId?: number | string
  startDate?: string
  endDate?: string
}) {
  return request.get<TimesheetStatsVO>('/timesheets/summary', params as any)
}

/** 兼容旧引用：fetchWeekTimesheet → 调用 summary 接口（默认本周范围） */
export async function fetchWeekTimesheet(): Promise<{
  weekHours: number
  monthTravelDays: number
}> {
  const today = new Date()
  const start = new Date(today)
  start.setDate(today.getDate() - 6)
  const fmt = (d: Date) =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  try {
    const stats = await fetchTimesheetSummary({
      startDate: fmt(start),
      endDate: fmt(today)
    })
    return {
      weekHours: Number(stats?.totalHours || 0),
      monthTravelDays: Number(stats?.totalTravelDays || 0)
    }
  } catch {
    return { weekHours: 0, monthTravelDays: 0 }
  }
}

/* ============ 站内信 ============ */

/** 站内信分页查询参数（对齐后端 SysNoticeQueryDTO） */
export interface NoticeQuery extends PageQuery {
  noticeType?: number
  readStatus?: number
  keyword?: string
}

/** 分页查询当前用户站内信 GET /notices */
export function fetchNotices(params: NoticeQuery) {
  return request.get<PageResult<SysNoticeVO>>('/notices', params as any)
}

/** 未读计数 GET /notices/unread-count */
export function fetchUnreadNoticeCount() {
  return request.get<number>('/notices/unread-count')
}

/** 标记单条已读 PUT /notices/{id}/read */
export function markNoticeRead(id: string | number) {
  return request.put<void>(`/notices/${id}/read`)
}

/** 全部标记已读 PUT /notices/read-all */
export function markAllNoticesRead() {
  return request.put<void>('/notices/read-all')
}

/** 删除站内信 DELETE /notices/{id} */
export function deleteNotice(id: string | number) {
  return request.delete<void>(`/notices/${id}`)
}

/* ============ 账号 ============ */

/**
 * 修改密码 PUT /users/{userId}/password
 * 后端 SysUserPasswordDTO：仅 newPassword（明文，BCrypt 加密存储）
 * 注意：该端点需 system:user:add 权限或 SUPER_ADMIN 角色，普通工程师调用会返回 403。
 */
export function changePassword(
  userId: number | string,
  payload: { newPassword: string }
) {
  return request.put<void>(`/users/${userId}/password`, payload)
}

/* ============ 兼容旧引用：通用文件上传（保留用于上传队列等场景） ============ */

/**
 * 通用文件上传（保留旧接口签名，给 UploadQueue 等组件使用）
 * 注意：后端照片上传请优先使用 uploadWorkOrderPhoto 走工单照片端点。
 */
export function uploadFile(
  file: File | Blob,
  onProgress?: (loaded: number, total: number) => void,
  extra?: Record<string, any>
) {
  return request.upload<{ url: string; name: string; size: number }>(
    '/file/upload',
    file,
    onProgress,
    extra
  )
}
