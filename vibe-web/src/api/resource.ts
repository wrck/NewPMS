/**
 * 资源调度模块 API 封装
 * 对应后端：/api/v1/engineers、/api/v1/schedules、/api/v1/dispatches、/api/v1/timesheets
 *           /api/v1/business-trips、/api/v1/engineer-leaves（后端 Controller 待补全，前端方法签名已就绪）
 */
import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  Engineer,
  Schedule,
  DispatchTask,
  DispatchCandidate,
  Timesheet,
  EngineerQueryParams,
  ScheduleQueryParams,
  DispatchQueryParams,
  TimesheetQueryParams,
  EngineerDTO,
  TimesheetDTO,
  DispatchDTO,
  BatchDispatchDTO,
  BusinessTrip,
  BusinessTripDTO,
  BusinessTripQueryParams,
  EngineerLeave,
  EngineerLeaveDTO,
  EngineerLeaveQueryParams
} from '@/types/resource'

/* ============ 工程师资源池 ============ */

const ENGINEER_BASE = '/engineers'

export function pageEngineers(params: EngineerQueryParams) {
  return http.get<PageResult<Engineer>>(ENGINEER_BASE, params as Record<string, unknown>)
}

export function getEngineerDetail(id: number) {
  return http.get<Engineer>(`${ENGINEER_BASE}/${id}`)
}

export function createEngineer(dto: EngineerDTO) {
  return http.post<number>(ENGINEER_BASE, dto)
}

export function updateEngineer(id: number, dto: EngineerDTO) {
  return http.put<void>(`${ENGINEER_BASE}/${id}`, dto)
}

export function deleteEngineer(id: number) {
  return http.delete<void>(`${ENGINEER_BASE}/${id}`)
}

/** 工程师负荷（近期任务数 + 利用率） */
export function getEngineerWorkload(id: number) {
  return http.get<{ ongoingTaskCount: number; utilization: number; upcomingSchedules: Schedule[] }>(
    `${ENGINEER_BASE}/${id}/workload`
  )
}

/* ============ 排期管理 ============ */

const SCHEDULE_BASE = '/schedules'

export function listSchedules(params: ScheduleQueryParams) {
  return http.get<Schedule[]>(SCHEDULE_BASE, params as unknown as Record<string, unknown>)
}

export function createSchedule(dto: Partial<Schedule>) {
  return http.post<number>(SCHEDULE_BASE, dto)
}

export function updateSchedule(id: number, dto: Partial<Schedule>) {
  return http.put<void>(`${SCHEDULE_BASE}/${id}`, dto)
}

export function deleteSchedule(id: number) {
  return http.delete<void>(`${SCHEDULE_BASE}/${id}`)
}

/* ============ 任务派发 ============ */

const DISPATCH_BASE = '/dispatches'

/** 待派发任务列表 */
export function pageDispatchTasks(params: DispatchQueryParams) {
  return http.get<PageResult<DispatchTask>>(`${DISPATCH_BASE}/tasks`, params as Record<string, unknown>)
}

/** 智能推荐派发候选 */
export function recommendCandidates(taskId: number) {
  return http.get<DispatchCandidate[]>(`${DISPATCH_BASE}/tasks/${taskId}/recommend`)
}

/** 派发任务（指向项目任务派发接口的便捷入口） */
export function dispatchTask(taskId: number, dto: DispatchDTO) {
  return http.put<void>(`${DISPATCH_BASE}/tasks/${taskId}`, dto)
}

/** 批量派发 */
export function batchDispatchTasks(dto: BatchDispatchDTO) {
  return http.post<number>(`${DISPATCH_BASE}/tasks/batch`, dto)
}

/* ============ 工时管理 ============ */

const TIMESHEET_BASE = '/timesheets'

export function pageTimesheets(params: TimesheetQueryParams) {
  return http.get<PageResult<Timesheet>>(TIMESHEET_BASE, params as Record<string, unknown>)
}

export function getTimesheetDetail(id: number) {
  return http.get<Timesheet>(`${TIMESHEET_BASE}/${id}`)
}

export function createTimesheet(dto: TimesheetDTO) {
  return http.post<number>(TIMESHEET_BASE, dto)
}

export function updateTimesheet(id: number, dto: TimesheetDTO) {
  return http.put<void>(`${TIMESHEET_BASE}/${id}`, dto)
}

export function deleteTimesheet(id: number) {
  return http.delete<void>(`${TIMESHEET_BASE}/${id}`)
}

/** 提交工时（DRAFT → SUBMITTED） */
export function submitTimesheet(id: number) {
  return http.put<void>(`${TIMESHEET_BASE}/${id}/submit`)
}

/** 审批工时（SUBMITTED → APPROVED/REJECTED） */
export function approveTimesheet(id: number, approved: boolean, remark?: string) {
  return http.put<void>(`${TIMESHEET_BASE}/${id}/approve`, { approved, remark })
}

/** 工时统计 */
export function getTimesheetStats(params: { startDate?: string; endDate?: string; engineerId?: number; projectId?: number }) {
  return http.get<{ totalHours: number; overtimeHours: number; travelDays: number; byEngineer: Array<{ engineerId: number; engineerName: string; hours: number }> }>(
    `${TIMESHEET_BASE}/stats`,
    params as Record<string, unknown>
  )
}

/* ============ 差旅管理 ============ */
// TODO: 后端 BusinessTripController 尚未实现，以下接口签名已就绪，待后端补全 Controller 后即可联调。
const TRIP_BASE = '/business-trips'

/** 分页查询差旅 */
export function pageBusinessTrips(params: BusinessTripQueryParams) {
  return http.get<PageResult<BusinessTrip>>(TRIP_BASE, params as Record<string, unknown>)
}

/** 差旅详情 */
export function getBusinessTripDetail(id: number) {
  return http.get<BusinessTrip>(`${TRIP_BASE}/${id}`)
}

/** 新增差旅 */
export function createBusinessTrip(dto: BusinessTripDTO) {
  return http.post<number>(TRIP_BASE, dto)
}

/** 编辑差旅 */
export function updateBusinessTrip(id: number, dto: BusinessTripDTO) {
  return http.put<void>(`${TRIP_BASE}/${id}`, dto)
}

/** 删除差旅 */
export function deleteBusinessTrip(id: number) {
  return http.delete<void>(`${TRIP_BASE}/${id}`)
}

/** 差旅审批（PENDING → APPROVED/REJECTED） */
export function approveBusinessTrip(id: number, approved: boolean, remark?: string) {
  return http.put<void>(`${TRIP_BASE}/${id}/approve`, { approved, remark })
}

/* ============ 工程师请假 ============ */
// TODO: 后端 EngineerLeaveController 尚未实现，以下接口签名已就绪，待后端补全 Controller 后即可联调。
const LEAVE_BASE = '/engineer-leaves'

/** 分页查询请假单 */
export function pageEngineerLeaves(params: EngineerLeaveQueryParams) {
  return http.get<PageResult<EngineerLeave>>(LEAVE_BASE, params as Record<string, unknown>)
}

/** 请假单详情 */
export function getEngineerLeaveDetail(id: number) {
  return http.get<EngineerLeave>(`${LEAVE_BASE}/${id}`)
}

/** 新增请假单 */
export function createEngineerLeave(dto: EngineerLeaveDTO) {
  return http.post<number>(LEAVE_BASE, dto)
}

/** 编辑请假单 */
export function updateEngineerLeave(id: number, dto: EngineerLeaveDTO) {
  return http.put<void>(`${LEAVE_BASE}/${id}`, dto)
}

/** 删除请假单 */
export function deleteEngineerLeave(id: number) {
  return http.delete<void>(`${LEAVE_BASE}/${id}`)
}

/** 请假审批（PENDING → APPROVED/REJECTED） */
export function approveEngineerLeave(id: number, approved: boolean, remark?: string) {
  return http.put<void>(`${LEAVE_BASE}/${id}/approve`, { approved, remark })
}
