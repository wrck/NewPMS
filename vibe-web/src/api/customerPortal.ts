/**
 * 客户门户 H5 API 封装
 * 对应后端：CustomerPortalController，路径前缀 /api/v1/customer
 *
 * 注意：
 *  - 进度查看 / 文档下载 / 待办 / 消息 需要 CUSTOMER 登录态
 *  - GET /cutover/{token} 和 GET /acceptance/{token} 通过 token 访问，无需登录态
 *  - POST /cutover/approval 和 POST /acceptance/sign 提交审批/签核结果，需要 CUSTOMER 登录态
 */
import { http } from '@/utils/request'
import type {
  CustomerProject,
  ProjectProgress,
  CustomerDocument,
  CustomerCutoverPlan,
  CustomerAcceptanceTask,
  CustomerTodo,
  CustomerMessage,
  CustomerCutoverApprovalDTO,
  CustomerAcceptanceSignDTO
} from '@/types/customerPortal'

const BASE = '/customer'

/* ============ 3.1 进度查看 ============ */

/** 我的项目列表 */
export function getMyProjects() {
  return http.get<CustomerProject[]>(`${BASE}/projects`)
}

/** 项目进度详情（含阶段时间线） */
export function getProjectProgress(projectId: number) {
  return http.get<ProjectProgress>(`${BASE}/projects/${projectId}/progress`)
}

/** 项目可下载文档（含 MinIO 预签名 URL） */
export function getProjectDocuments(projectId: number) {
  return http.get<CustomerDocument[]>(`${BASE}/projects/${projectId}/documents`)
}

/* ============ 3.2 割接审批 ============ */

/** 通过 token 查看割接方案详情（无需登录态） */
export function getCutoverPlanByToken(token: string) {
  return http.get<CustomerCutoverPlan>(`${BASE}/cutover/${token}`)
}

/** 提交割接审批结果（需要 CUSTOMER 登录态） */
export function submitCutoverApproval(dto: CustomerCutoverApprovalDTO) {
  return http.post<void>(`${BASE}/cutover/approval`, dto)
}

/* ============ 3.3 验收签核 ============ */

/** 通过 token 查看验收任务详情（无需登录态） */
export function getAcceptanceTaskByToken(token: string) {
  return http.get<CustomerAcceptanceTask>(`${BASE}/acceptance/${token}`)
}

/** 提交验收签核结果（需要 CUSTOMER 登录态） */
export function submitAcceptanceSign(dto: CustomerAcceptanceSignDTO) {
  return http.post<void>(`${BASE}/acceptance/sign`, dto)
}

/* ============ 3.4 待办列表 ============ */

/** 我的待办（待审批割接 + 待签核验收） */
export function getMyTodos() {
  return http.get<CustomerTodo[]>(`${BASE}/todos`)
}

/* ============ 3.5 消息通知 ============ */

/** 我的消息列表 */
export function getMyMessages() {
  return http.get<CustomerMessage[]>(`${BASE}/messages`)
}

/** 未读消息数 */
export function getUnreadMessageCount() {
  return http.get<number>(`${BASE}/messages/unread-count`)
}

/** 标记消息已读 */
export function markMessageRead(messageId: number) {
  return http.post<void>(`${BASE}/messages/${messageId}/read`)
}

/** 全部标记已读 */
export function markAllMessagesRead() {
  return http.post<void>(`${BASE}/messages/read-all`)
}
