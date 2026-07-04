/**
 * 代理商门户 H5 类型定义
 * 对应后端：com.vibe.agent.vo.AgentWorkbenchVO / AgentMessageVO / OutsourceTaskVO
 */

/** 代理商工作台统计卡片 */
export interface AgentWorkbenchSummary {
  pendingCount?: number
  inProgressCount?: number
  submittedCount?: number
  overdueCount?: number
  unreadMessageCount?: number
}

/** 代理商工作台聚合 VO */
export interface AgentWorkbench {
  summary?: AgentWorkbenchSummary
  pendingTasks?: AgentOutsourceTaskItem[]
  inProgressTasks?: AgentOutsourceTaskItem[]
  submittedTasks?: AgentOutsourceTaskItem[]
}

/**
 * 代理商视角的转包任务（脱敏后字段：不含 projectCode/customerName/contractAmount/costAmount）
 */
export interface AgentOutsourceTaskItem {
  id: number
  projectId?: number
  taskId?: number
  agentCompanyId?: number
  agentEngineerId?: number
  taskScope?: string
  deadline?: string
  status: string // PENDING/ACCEPTED/REJECTED/IN_PROGRESS/SUBMITTED/CONFIRMED/RETURNED/OVERDUE
  submitCount?: number
  confirmedBy?: number
  confirmedTime?: string
  rejectReason?: string
  version?: number
  createBy?: number
  createTime?: string
  updateBy?: number
  updateTime?: string
  /** JOIN 字段 */
  projectName?: string
  taskName?: string
  agentCompanyName?: string
  agentEngineerName?: string
  /** 以下敏感字段对代理商不可见（脱敏后为 null） */
  projectCode?: string
  customerName?: string
  contractAmount?: number
  costAmount?: number
}

/** 代理商消息 */
export interface AgentMessage {
  id: number
  agentCompanyId?: number
  messageType: string // TASK_ASSIGNED / TASK_EXPIRING / TASK_OVERDUE / DELIVERABLE_RETURNED / DELIVERABLE_CONFIRMED / WORKLOAD_CONFIRMED
  businessId?: number
  projectId?: number
  title: string
  content?: string
  isRead: number // 0-未读 1-已读
  createTime: string
}
