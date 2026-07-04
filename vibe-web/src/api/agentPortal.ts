/**
 * 代理商门户 H5 API 封装
 * 对应后端：AgentPortalController，路径前缀 /api/v1/agent
 *
 * 说明：转包任务的接单/拒绝/指派/提交交付物等操作仍由 agent.ts 中的
 * /outsource-tasks 接口提供，本文件仅提供 H5 工作台聚合查询与消息通知。
 */
import { http } from '@/utils/request'
import type { AgentWorkbench, AgentMessage } from '@/types/agentPortal'

const BASE = '/agent'

/* ============ 工作台 ============ */

/** 代理商工作台（统计卡片 + 三类任务 top 5） */
export function getAgentWorkbench() {
  return http.get<AgentWorkbench>(`${BASE}/workbench`)
}

/* ============ 消息通知 ============ */

/** 我的消息列表 */
export function getAgentMessages() {
  return http.get<AgentMessage[]>(`${BASE}/messages`)
}

/** 未读消息数 */
export function getAgentUnreadCount() {
  return http.get<number>(`${BASE}/messages/unread-count`)
}

/** 标记消息已读 */
export function markAgentMessageRead(messageId: number) {
  return http.post<void>(`${BASE}/messages/${messageId}/read`)
}

/** 全部标记已读 */
export function markAllAgentMessagesRead() {
  return http.post<void>(`${BASE}/messages/read-all`)
}
