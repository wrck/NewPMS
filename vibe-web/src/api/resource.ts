/**
 * 资源调度模块 API 封装
 * 对应后端：/api/v1/engineers、/api/v1/schedules、/api/v1/dispatches、/api/v1/timesheets
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
  BatchDispatchDTO
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
