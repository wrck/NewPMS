/**
 * 项目管理模块 API 封装
 * 对应后端：/api/v1/projects、/api/v1/projects/{id}/phases|tasks|milestones|risks|issues|changes|members|comments
 *           /api/v1/project-templates
 */
import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  Project,
  ProjectDetail,
  ProjectQueryParams,
  ProjectSaveDTO,
  ProjectStatusDTO,
  ProjectKanbanGroup,
  ProjectGantt,
  ProjectPhase,
  ProjectTask,
  ProjectTaskQueryParams,
  ProjectTaskDTO,
  TaskDispatchDTO,
  TaskTransferDTO,
  TaskReturnDTO,
  TaskProgressDTO,
  Milestone,
  ProjectRisk,
  ProjectIssue,
  ProjectChange,
  ProjectMember,
  ProjectComment,
  ProjectTemplate
} from '@/types/project'

const BASE = '/projects'

/* ============ 项目 CRUD ============ */

/** 分页查询项目 */
export function pageProjects(params: ProjectQueryParams) {
  return http.get<PageResult<Project>>(BASE, params as Record<string, unknown>)
}

/** 项目详情聚合 */
export function getProjectDetail(id: number) {
  return http.get<ProjectDetail>(`${BASE}/${id}`)
}

/** 立项（手动创建 / 选择模板生成阶段与任务） */
export function createProject(dto: ProjectSaveDTO) {
  return http.post<number>(BASE, dto)
}

/** 编辑项目 */
export function updateProject(id: number, dto: ProjectSaveDTO) {
  return http.put<void>(`${BASE}/${id}`, dto)
}

/** 删除项目（仅 INIT/PLAN 状态可删除） */
export function deleteProject(id: number) {
  return http.delete<void>(`${BASE}/${id}`)
}

/** 项目状态流转 */
export function transitionProjectStatus(id: number, dto: ProjectStatusDTO) {
  return http.put<void>(`${BASE}/${id}/status`, dto)
}

/** 看板分组查询 */
export function getProjectKanban(params?: ProjectQueryParams) {
  return http.get<ProjectKanbanGroup[]>(`${BASE}/kanban`, params as Record<string, unknown>)
}

/** 甘特图数据 */
export function getProjectGantt(id: number) {
  return http.get<ProjectGantt>(`${BASE}/${id}/gantt`)
}

/** 结项检查（返回不满足原因，null 表示通过） */
export function checkProjectClose(id: number) {
  return http.get<string>(`${BASE}/${id}/close-check`)
}

/** 归档（CLOSE → ARCHIVED） */
export function archiveProject(id: number, dto: { reviewRecord?: string }) {
  return http.put<void>(`${BASE}/${id}/archive`, dto)
}

/* ============ 阶段 ============ */

export function listPhases(projectId: number) {
  return http.get<ProjectPhase[]>(`${BASE}/${projectId}/phases`)
}

export function savePhase(projectId: number, dto: Partial<ProjectPhase>) {
  if (dto.id) {
    return http.put<void>(`${BASE}/${projectId}/phases/${dto.id}`, dto)
  }
  return http.post<number>(`${BASE}/${projectId}/phases`, dto)
}

export function deletePhase(projectId: number, phaseId: number) {
  return http.delete<void>(`${BASE}/${projectId}/phases/${phaseId}`)
}

/* ============ 任务 ============ */

/** 分页查询任务（跨项目，含数据权限） */
export function pageTasks(params: ProjectTaskQueryParams) {
  return http.get<PageResult<ProjectTask>>(`${BASE}/tasks`, params as Record<string, unknown>)
}

/** 查询项目下的全部任务 */
export function listTasksByProject(projectId: number) {
  return http.get<ProjectTask[]>(`${BASE}/${projectId}/tasks`)
}

/** 任务详情 */
export function getTaskDetail(taskId: number) {
  return http.get<ProjectTask>(`${BASE}/tasks/${taskId}`)
}

/** 新增任务 */
export function createTask(projectId: number, dto: ProjectTaskDTO) {
  return http.post<number>(`${BASE}/${projectId}/tasks`, dto)
}

/** 编辑任务 */
export function updateTask(taskId: number, dto: ProjectTaskDTO) {
  return http.put<void>(`${BASE}/tasks/${taskId}`, dto)
}

/** 删除任务 */
export function deleteTask(taskId: number) {
  return http.delete<void>(`${BASE}/tasks/${taskId}`)
}

/** 任务派发 */
export function dispatchTask(taskId: number, dto: TaskDispatchDTO) {
  return http.put<void>(`${BASE}/tasks/${taskId}/dispatch`, dto)
}

/** 批量派单 */
export function batchDispatchTasks(dto: { taskIds: number[] } & TaskDispatchDTO) {
  return http.post<number>(`${BASE}/tasks/batch-dispatch`, dto)
}

/** 任务转派 */
export function transferTask(taskId: number, dto: TaskTransferDTO) {
  return http.put<void>(`${BASE}/tasks/${taskId}/transfer`, dto)
}

/** 任务退回 */
export function returnTask(taskId: number, dto: TaskReturnDTO) {
  return http.put<void>(`${BASE}/tasks/${taskId}/return`, dto)
}

/** 进度更新 */
export function updateTaskProgress(taskId: number, dto: TaskProgressDTO) {
  return http.put<void>(`${BASE}/tasks/${taskId}/progress`, dto)
}

/** 甘特图拖拽排期 */
export function rescheduleTask(taskId: number, newStart: string, newEnd: string) {
  return http.put<void>(`${BASE}/tasks/${taskId}/reschedule`, undefined, {
    params: { newStart, newEnd }
  })
}

/** 超期未完成任务列表 */
export function listOverdueTasks() {
  return http.get<ProjectTask[]>(`${BASE}/tasks/overdue`)
}

/* ============ 里程碑 ============ */

export function listMilestones(projectId: number) {
  return http.get<Milestone[]>(`${BASE}/${projectId}/milestones`)
}

export function saveMilestone(projectId: number, dto: Partial<Milestone>) {
  if (dto.id) {
    return http.put<void>(`${BASE}/${projectId}/milestones/${dto.id}`, dto)
  }
  return http.post<number>(`${BASE}/${projectId}/milestones`, dto)
}

export function deleteMilestone(projectId: number, id: number) {
  return http.delete<void>(`${BASE}/${projectId}/milestones/${id}`)
}

/* ============ 风险 ============ */

export function listRisks(projectId: number) {
  return http.get<ProjectRisk[]>(`${BASE}/${projectId}/risks`)
}

export function saveRisk(projectId: number, dto: Partial<ProjectRisk>) {
  if (dto.id) {
    return http.put<void>(`${BASE}/${projectId}/risks/${dto.id}`, dto)
  }
  return http.post<number>(`${BASE}/${projectId}/risks`, dto)
}

export function deleteRisk(projectId: number, id: number) {
  return http.delete<void>(`${BASE}/${projectId}/risks/${id}`)
}

/* ============ 问题 ============ */

export function listIssues(projectId: number) {
  return http.get<ProjectIssue[]>(`${BASE}/${projectId}/issues`)
}

export function saveIssue(projectId: number, dto: Partial<ProjectIssue>) {
  if (dto.id) {
    return http.put<void>(`${BASE}/${projectId}/issues/${dto.id}`, dto)
  }
  return http.post<number>(`${BASE}/${projectId}/issues`, dto)
}

export function deleteIssue(projectId: number, id: number) {
  return http.delete<void>(`${BASE}/${projectId}/issues/${id}`)
}

/* ============ 变更 ============ */

export function listChanges(projectId: number) {
  return http.get<ProjectChange[]>(`${BASE}/${projectId}/changes`)
}

export function createChange(projectId: number, dto: Partial<ProjectChange>) {
  return http.post<number>(`${BASE}/${projectId}/changes`, dto)
}

export function approveChange(projectId: number, id: number, approved: boolean, remark?: string) {
  return http.put<void>(`${BASE}/${projectId}/changes/${id}/approve`, { approved, remark })
}

/* ============ 成员 ============ */

export function listMembers(projectId: number) {
  return http.get<ProjectMember[]>(`${BASE}/${projectId}/members`)
}

export function addMember(projectId: number, dto: Partial<ProjectMember>) {
  return http.post<number>(`${BASE}/${projectId}/members`, dto)
}

export function removeMember(projectId: number, id: number) {
  return http.delete<void>(`${BASE}/${projectId}/members/${id}`)
}

/* ============ 沟通记录 ============ */

export function listComments(projectId: number) {
  return http.get<ProjectComment[]>(`${BASE}/${projectId}/comments`)
}

export function addComment(projectId: number, content: string, attachments?: Array<{ name: string; url: string }>) {
  return http.post<number>(`${BASE}/${projectId}/comments`, { content, attachments })
}

/* ============ 项目模板 ============ */

const TEMPLATE_BASE = '/project-templates'

export function pageTemplates(params?: { keyword?: string; productLine?: string; projectType?: string }) {
  return http.get<PageResult<ProjectTemplate>>(TEMPLATE_BASE, params as Record<string, unknown>)
}

export function getTemplateDetail(id: number) {
  return http.get<ProjectTemplate>(`${TEMPLATE_BASE}/${id}`)
}

export function createTemplate(dto: Partial<ProjectTemplate>) {
  return http.post<number>(TEMPLATE_BASE, dto)
}

export function updateTemplate(id: number, dto: Partial<ProjectTemplate>) {
  return http.put<void>(`${TEMPLATE_BASE}/${id}`, dto)
}

export function deleteTemplate(id: number) {
  return http.delete<void>(`${TEMPLATE_BASE}/${id}`)
}
