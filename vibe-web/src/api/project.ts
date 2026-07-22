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
  ProjectTemplate,
  ProjectTemplatePhase,
  ProjectTemplateTask,
  Customer,
  CustomerDTO,
  CustomerQueryParams
} from '@/types/project'

const BASE = '/projects'

/* ============ 项目 CRUD ============ */

/** 分页查询项目 */
export function pageProjects(params: ProjectQueryParams) {
  return http.get<PageResult<Project>>(BASE, params as Record<string, unknown>)
}

/** 项目详情聚合 */
export function getProjectDetail(id: string | number) {
  return http.get<ProjectDetail>(`${BASE}/${id}`)
}

/** 立项（手动创建 / 选择模板生成阶段与任务） */
export function createProject(dto: ProjectSaveDTO) {
  return http.post<number>(BASE, dto)
}

/** 编辑项目 */
export function updateProject(id: string | number, dto: ProjectSaveDTO) {
  return http.put<void>(`${BASE}/${id}`, dto)
}

/** 删除项目（仅 INIT/PLAN 状态可删除） */
export function deleteProject(id: string | number) {
  return http.delete<void>(`${BASE}/${id}`)
}

/** 项目状态流转 */
export function transitionProjectStatus(id: string | number, dto: ProjectStatusDTO) {
  return http.put<void>(`${BASE}/${id}/status`, dto)
}

/** 看板分组查询 */
export function getProjectKanban(params?: ProjectQueryParams) {
  return http.get<ProjectKanbanGroup[]>(`${BASE}/kanban`, params as Record<string, unknown>)
}

/** 甘特图数据 */
export function getProjectGantt(id: string | number) {
  return http.get<ProjectGantt>(`${BASE}/${id}/gantt`)
}

/** 结项检查（返回不满足原因，null 表示通过） */
export function checkProjectClose(id: string | number) {
  return http.get<string>(`${BASE}/${id}/close-check`)
}

/** 归档（CLOSE → ARCHIVED） */
export function archiveProject(id: string | number, dto: { reviewSummary?: string; lessonsLearned?: string }) {
  return http.put<void>(`${BASE}/${id}/archive`, dto)
}

/* ============ 阶段 ============ */

export function listPhases(projectId: string | number) {
  return http.get<ProjectPhase[]>(`${BASE}/${projectId}/phases`)
}

export function savePhase(projectId: string | number, dto: Partial<ProjectPhase>) {
  if (dto.id) {
    return http.put<void>(`${BASE}/${projectId}/phases/${dto.id}`, dto)
  }
  return http.post<number>(`${BASE}/${projectId}/phases`, dto)
}

export function deletePhase(projectId: string | number, phaseId: string | number) {
  return http.delete<void>(`${BASE}/${projectId}/phases/${phaseId}`)
}

/* ============ 任务 ============ */

/** 分页查询任务（跨项目，含数据权限） */
export function pageTasks(params: ProjectTaskQueryParams) {
  return http.get<PageResult<ProjectTask>>(`${BASE}/tasks`, params as Record<string, unknown>)
}

/** 查询项目下的全部任务 */
export function listTasksByProject(projectId: string | number) {
  return http.get<ProjectTask[]>(`${BASE}/${projectId}/tasks`)
}

/** 任务详情 */
export function getTaskDetail(taskId: string | number) {
  return http.get<ProjectTask>(`${BASE}/tasks/${taskId}`)
}

/** 新增任务 */
export function createTask(projectId: string | number, dto: ProjectTaskDTO) {
  return http.post<number>(`${BASE}/${projectId}/tasks`, dto)
}

/** 编辑任务 */
export function updateTask(taskId: string | number, dto: ProjectTaskDTO) {
  return http.put<void>(`${BASE}/tasks/${taskId}`, dto)
}

/** 删除任务 */
export function deleteTask(taskId: string | number) {
  return http.delete<void>(`${BASE}/tasks/${taskId}`)
}

/** 任务派发 */
export function dispatchTask(taskId: string | number, dto: TaskDispatchDTO) {
  return http.put<void>(`${BASE}/tasks/${taskId}/dispatch`, dto)
}

/** 批量派单 */
export function batchDispatchTasks(dto: { taskIds: Array<string | number>; dispatch: TaskDispatchDTO }) {
  return http.post<number>(`${BASE}/tasks/batch-dispatch`, dto)
}

/** 任务转派 */
export function transferTask(taskId: string | number, dto: TaskTransferDTO) {
  return http.put<void>(`${BASE}/tasks/${taskId}/transfer`, dto)
}

/** 任务退回 */
export function returnTask(taskId: string | number, dto: TaskReturnDTO) {
  return http.put<void>(`${BASE}/tasks/${taskId}/return`, dto)
}

/** 进度更新 */
export function updateTaskProgress(taskId: string | number, dto: TaskProgressDTO) {
  return http.put<void>(`${BASE}/tasks/${taskId}/progress`, dto)
}

/** 甘特图拖拽排期 */
export function rescheduleTask(taskId: string | number, newStart: string, newEnd: string) {
  return http.put<void>(`${BASE}/tasks/${taskId}/reschedule`, undefined, {
    params: { newStart, newEnd }
  })
}

/** 超期未完成任务列表 */
export function listOverdueTasks() {
  return http.get<ProjectTask[]>(`${BASE}/tasks/overdue`)
}

/* ============ 里程碑 ============ */

export function listMilestones(projectId: string | number) {
  return http.get<Milestone[]>(`${BASE}/${projectId}/milestones`)
}

export function saveMilestone(projectId: string | number, dto: Partial<Milestone>) {
  if (dto.id) {
    return http.put<void>(`${BASE}/${projectId}/milestones/${dto.id}`, dto)
  }
  return http.post<number>(`${BASE}/${projectId}/milestones`, dto)
}

export function deleteMilestone(projectId: string | number, id: string | number) {
  return http.delete<void>(`${BASE}/${projectId}/milestones/${id}`)
}

/* ============ 风险 ============ */

export function listRisks(projectId: string | number) {
  return http.get<ProjectRisk[]>(`${BASE}/${projectId}/risks`)
}

export function saveRisk(projectId: string | number, dto: Partial<ProjectRisk>) {
  if (dto.id) {
    return http.put<void>(`${BASE}/${projectId}/risks/${dto.id}`, dto)
  }
  return http.post<number>(`${BASE}/${projectId}/risks`, dto)
}

export function deleteRisk(projectId: string | number, id: string | number) {
  return http.delete<void>(`${BASE}/${projectId}/risks/${id}`)
}

/* ============ 问题 ============ */

export function listIssues(projectId: string | number) {
  return http.get<ProjectIssue[]>(`${BASE}/${projectId}/issues`)
}

export function saveIssue(projectId: string | number, dto: Partial<ProjectIssue>) {
  if (dto.id) {
    return http.put<void>(`${BASE}/${projectId}/issues/${dto.id}`, dto)
  }
  return http.post<number>(`${BASE}/${projectId}/issues`, dto)
}

export function deleteIssue(projectId: string | number, id: string | number) {
  return http.delete<void>(`${BASE}/${projectId}/issues/${id}`)
}

/* ============ 变更 ============ */

export function listChanges(projectId: string | number) {
  return http.get<ProjectChange[]>(`${BASE}/${projectId}/changes`)
}

export function createChange(projectId: string | number, dto: Partial<ProjectChange>) {
  return http.post<number>(`${BASE}/${projectId}/changes`, dto)
}

export function approveChange(projectId: string | number, id: string | number, approved: boolean, remark?: string) {
  return http.put<void>(`${BASE}/${projectId}/changes/${id}/approve`, {
    approveResult: approved ? 'APPROVED' : 'REJECTED',
    opinion: remark
  })
}

/* ============ 成员 ============ */

export function listMembers(projectId: string | number) {
  return http.get<ProjectMember[]>(`${BASE}/${projectId}/members`)
}

export function addMember(projectId: string | number, dto: { userId: number | string; role: string }) {
  return http.post<number>(`${BASE}/${projectId}/members`, undefined, {
    params: { userId: dto.userId, role: dto.role }
  })
}

export function removeMember(projectId: string | number, id: string | number) {
  return http.delete<void>(`${BASE}/${projectId}/members/${id}`)
}

/* ============ 沟通记录 ============ */

export function listComments(projectId: string | number) {
  return http.get<ProjectComment[]>(`${BASE}/${projectId}/comments`)
}

export function addComment(projectId: string | number, content: string, attachments?: Array<{ name: string; url: string }>) {
  return http.post<number>(`${BASE}/${projectId}/comments`, { content, attachments })
}

/* ============ 项目模板 ============ */

const TEMPLATE_BASE = '/project-templates'

export function pageTemplates(params?: { keyword?: string; productLine?: string; projectType?: string }) {
  return http.get<PageResult<ProjectTemplate>>(TEMPLATE_BASE, params as Record<string, unknown>)
}

export function getTemplateDetail(id: string | number) {
  return http.get<ProjectTemplate>(`${TEMPLATE_BASE}/${id}`)
}

export function createTemplate(dto: Partial<ProjectTemplate>) {
  return http.post<number>(TEMPLATE_BASE, dto)
}

export function updateTemplate(id: string | number, dto: Partial<ProjectTemplate>) {
  return http.put<void>(`${TEMPLATE_BASE}/${id}`, dto)
}

export function deleteTemplate(id: string | number) {
  return http.delete<void>(`${TEMPLATE_BASE}/${id}`)
}

/* ============ 模板阶段 ============ */

/** 新增模板阶段 */
export function addTemplatePhase(templateId: string | number, dto: Partial<ProjectTemplatePhase>) {
  return http.post<number>(`${TEMPLATE_BASE}/${templateId}/phases`, dto)
}

/** 编辑模板阶段 */
export function updateTemplatePhase(id: string | number, dto: Partial<ProjectTemplatePhase>) {
  return http.put<void>(`${TEMPLATE_BASE}/phases/${id}`, dto)
}

/** 删除模板阶段 */
export function deleteTemplatePhase(id: string | number) {
  return http.delete<void>(`${TEMPLATE_BASE}/phases/${id}`)
}

/* ============ 模板任务 ============ */

/** 新增模板任务 */
export function addTemplateTask(templateId: string | number, dto: Partial<ProjectTemplateTask>) {
  return http.post<number>(`${TEMPLATE_BASE}/${templateId}/tasks`, dto)
}

/** 编辑模板任务 */
export function updateTemplateTask(id: string | number, dto: Partial<ProjectTemplateTask>) {
  return http.put<void>(`${TEMPLATE_BASE}/tasks/${id}`, dto)
}

/** 删除模板任务 */
export function deleteTemplateTask(id: string | number) {
  return http.delete<void>(`${TEMPLATE_BASE}/tasks/${id}`)
}

/* ============ 客户档案 ============ */

const CUSTOMER_BASE = '/customers'

/** 分页查询客户 */
export function pageCustomers(params: CustomerQueryParams) {
  return http.get<PageResult<Customer>>(CUSTOMER_BASE, params as Record<string, unknown>)
}

/** 客户详情 */
export function getCustomerDetail(id: string | number) {
  return http.get<Customer>(`${CUSTOMER_BASE}/${id}`)
}

/** 新增客户 */
export function createCustomer(dto: CustomerDTO) {
  return http.post<number>(CUSTOMER_BASE, dto)
}

/** 编辑客户 */
export function updateCustomer(id: string | number, dto: CustomerDTO) {
  return http.put<void>(`${CUSTOMER_BASE}/${id}`, dto)
}

/** 删除客户 */
export function deleteCustomer(id: string | number) {
  return http.delete<void>(`${CUSTOMER_BASE}/${id}`)
}
