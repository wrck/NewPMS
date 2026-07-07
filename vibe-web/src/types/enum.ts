/**
 * 业务状态枚举（设计文档 3.1.2 / 3.1.4 状态语义）
 * 颜色映射见 src/styles/theme.ts -> statusColorMap
 */
import type { StatusTone } from '@/styles/theme'

/** 通用状态语义色调映射 */
export const StatusToneMap = {
  // 通用未开始/待处理
  PENDING: 'default' as StatusTone,
  // 进行中/执行中
  PROCESSING: 'processing' as StatusTone,
  // 待审核/待确认
  REVIEWING: 'warning' as StatusTone,
  // 已完成/已通过
  DONE: 'success' as StatusTone,
  // 超期/异常/驳回
  ABNORMAL: 'error' as StatusTone,
  // 暂停/挂起
  PAUSED: 'pause' as StatusTone,
  // 已归档/已取消
  ARCHIVED: 'archived' as StatusTone,
  // 代理商代施
  AGENT: 'agent' as StatusTone
}

/** 项目状态机：INIT→PLAN→EXECUTE→ACCEPT→CLOSE→ARCHIVED + ON_HOLD/CANCELLED */
export enum ProjectStatus {
  INIT = 'INIT', // 已立项
  PLAN = 'PLAN', // 计划中
  EXECUTE = 'EXECUTE', // 执行中
  ACCEPT = 'ACCEPT', // 验收中
  CLOSE = 'CLOSE', // 已结项
  ARCHIVED = 'ARCHIVED', // 已归档
  ON_HOLD = 'ON_HOLD', // 挂起
  CANCELLED = 'CANCELLED' // 已取消
}

export const ProjectStatusTone: Record<ProjectStatus, StatusTone> = {
  [ProjectStatus.INIT]: 'default',
  [ProjectStatus.PLAN]: 'default',
  [ProjectStatus.EXECUTE]: 'processing',
  [ProjectStatus.ACCEPT]: 'warning',
  [ProjectStatus.CLOSE]: 'success',
  [ProjectStatus.ARCHIVED]: 'archived',
  [ProjectStatus.ON_HOLD]: 'pause',
  [ProjectStatus.CANCELLED]: 'archived'
}

export const ProjectStatusLabel: Record<ProjectStatus, string> = {
  [ProjectStatus.INIT]: '立项',
  [ProjectStatus.PLAN]: '规划中',
  [ProjectStatus.EXECUTE]: '执行中',
  [ProjectStatus.ACCEPT]: '验收中',
  [ProjectStatus.CLOSE]: '已结项',
  [ProjectStatus.ARCHIVED]: '已归档',
  [ProjectStatus.ON_HOLD]: '暂停',
  [ProjectStatus.CANCELLED]: '已取消'
}

/** 设备状态机：IN_FACTORY→SHIPPED→RECEIVED→PRE_CONFIG→INSTALLED→DEBUGGED→ONLINE 及异常分支 */
export enum DeviceStatus {
  IN_FACTORY = 'IN_FACTORY', // 在库
  SHIPPED = 'SHIPPED', // 已发运
  RECEIVED = 'RECEIVED', // 已到货
  PRE_CONFIG = 'PRE_CONFIG', // 已预配
  INSTALLED = 'INSTALLED', // 已安装
  DEBUGGED = 'DEBUGGED', // 已调试
  ONLINE = 'ONLINE', // 在网运行
  DAMAGED = 'DAMAGED', // 损坏
  LOST = 'LOST', // 遗失
  RETURNED = 'RETURNED', // 已退货
  REPAIR = 'REPAIR', // 返修中
  REPLACED = 'REPLACED', // 已替换
  EOL = 'EOL' // 退网/报废
}

export const DeviceStatusTone: Record<DeviceStatus, StatusTone> = {
  [DeviceStatus.IN_FACTORY]: 'default',
  [DeviceStatus.SHIPPED]: 'processing',
  [DeviceStatus.RECEIVED]: 'processing',
  [DeviceStatus.PRE_CONFIG]: 'processing',
  [DeviceStatus.INSTALLED]: 'processing',
  [DeviceStatus.DEBUGGED]: 'processing',
  [DeviceStatus.ONLINE]: 'success',
  [DeviceStatus.DAMAGED]: 'error',
  [DeviceStatus.LOST]: 'error',
  [DeviceStatus.RETURNED]: 'archived',
  [DeviceStatus.REPAIR]: 'error',
  [DeviceStatus.REPLACED]: 'archived',
  [DeviceStatus.EOL]: 'archived'
}

export const DeviceStatusLabel: Record<DeviceStatus, string> = {
  [DeviceStatus.IN_FACTORY]: '在库',
  [DeviceStatus.SHIPPED]: '已发运',
  [DeviceStatus.RECEIVED]: '已到货',
  [DeviceStatus.PRE_CONFIG]: '已预配',
  [DeviceStatus.INSTALLED]: '已安装',
  [DeviceStatus.DEBUGGED]: '已调试',
  [DeviceStatus.ONLINE]: '在网运行',
  [DeviceStatus.DAMAGED]: '损坏',
  [DeviceStatus.LOST]: '遗失',
  [DeviceStatus.RETURNED]: '已退货',
  [DeviceStatus.REPAIR]: '返修中',
  [DeviceStatus.REPLACED]: '已替换',
  [DeviceStatus.EOL]: '退网/报废'
}

/** 项目任务状态机：PENDING→ASSIGNED→IN_PROGRESS→COMPLETED→CONFIRMED */
export enum TaskStatus {
  PENDING = 'PENDING', // 待分配
  ASSIGNED = 'ASSIGNED', // 已分配
  IN_PROGRESS = 'IN_PROGRESS', // 进行中
  COMPLETED = 'COMPLETED', // 已完成
  CONFIRMED = 'CONFIRMED' // 已确认
}

export const TaskStatusTone: Record<TaskStatus, StatusTone> = {
  [TaskStatus.PENDING]: 'default',
  [TaskStatus.ASSIGNED]: 'processing',
  [TaskStatus.IN_PROGRESS]: 'processing',
  [TaskStatus.COMPLETED]: 'warning',
  [TaskStatus.CONFIRMED]: 'success'
}

export const TaskStatusLabel: Record<TaskStatus, string> = {
  [TaskStatus.PENDING]: '待分配',
  [TaskStatus.ASSIGNED]: '已分配',
  [TaskStatus.IN_PROGRESS]: '进行中',
  [TaskStatus.COMPLETED]: '已完成',
  [TaskStatus.CONFIRMED]: '已确认'
}

/** 工单状态机：CREATED→CHECKED_IN→IN_PROGRESS→COMPLETED→CONFIRMED */
export enum WorkOrderStatus {
  CREATED = 'CREATED', // 已创建
  CHECKED_IN = 'CHECKED_IN', // 已签到
  IN_PROGRESS = 'IN_PROGRESS', // 进行中
  COMPLETED = 'COMPLETED', // 已完成待确认
  CONFIRMED = 'CONFIRMED' // 已确认
}

export const WorkOrderStatusTone: Record<WorkOrderStatus, StatusTone> = {
  [WorkOrderStatus.CREATED]: 'default',
  [WorkOrderStatus.CHECKED_IN]: 'processing',
  [WorkOrderStatus.IN_PROGRESS]: 'processing',
  [WorkOrderStatus.COMPLETED]: 'warning',
  [WorkOrderStatus.CONFIRMED]: 'success'
}

export const WorkOrderStatusLabel: Record<WorkOrderStatus, string> = {
  [WorkOrderStatus.CREATED]: '已创建',
  [WorkOrderStatus.CHECKED_IN]: '已签到',
  [WorkOrderStatus.IN_PROGRESS]: '进行中',
  [WorkOrderStatus.COMPLETED]: '已完成待确认',
  [WorkOrderStatus.CONFIRMED]: '已确认'
}

/** 验收任务状态机：DRAFT→APPLIED→INTERNAL_AUDITED→CUSTOMER_SIGNING→COMPLETED/REJECTED */
export enum AcceptanceTaskStatus {
  DRAFT = 'DRAFT', // 草稿
  APPLIED = 'APPLIED', // 已申请
  INTERNAL_AUDITED = 'INTERNAL_AUDITED', // 内部审核通过
  CUSTOMER_SIGNING = 'CUSTOMER_SIGNING', // 客户签核中
  COMPLETED = 'COMPLETED', // 已完成
  REJECTED = 'REJECTED' // 已驳回
}

export const AcceptanceTaskStatusTone: Record<AcceptanceTaskStatus, StatusTone> = {
  [AcceptanceTaskStatus.DRAFT]: 'default',
  [AcceptanceTaskStatus.APPLIED]: 'processing',
  [AcceptanceTaskStatus.INTERNAL_AUDITED]: 'processing',
  [AcceptanceTaskStatus.CUSTOMER_SIGNING]: 'warning',
  [AcceptanceTaskStatus.COMPLETED]: 'success',
  [AcceptanceTaskStatus.REJECTED]: 'error'
}

export const AcceptanceTaskStatusLabel: Record<AcceptanceTaskStatus, string> = {
  [AcceptanceTaskStatus.DRAFT]: '草稿',
  [AcceptanceTaskStatus.APPLIED]: '已申请',
  [AcceptanceTaskStatus.INTERNAL_AUDITED]: '内部审核通过',
  [AcceptanceTaskStatus.CUSTOMER_SIGNING]: '客户签核中',
  [AcceptanceTaskStatus.COMPLETED]: '已完成',
  [AcceptanceTaskStatus.REJECTED]: '已驳回'
}

/** 工作量确认审批状态机：DRAFT→PM_CONFIRMED→AGENT_CONFIRMED→PENDING→DIRECTOR_APPROVED→FINANCE_APPROVED→CLOSED / REJECTED */
export enum WorkloadConfirmStatus {
  DRAFT = 'DRAFT', // 草稿
  PM_CONFIRMED = 'PM_CONFIRMED', // PM 已确认
  AGENT_CONFIRMED = 'AGENT_CONFIRMED', // 代理商已确认
  PENDING = 'PENDING', // 待审批
  DIRECTOR_APPROVED = 'DIRECTOR_APPROVED', // 总监已审批
  FINANCE_APPROVED = 'FINANCE_APPROVED', // 财务已审批
  REJECTED = 'REJECTED', // 已驳回
  CLOSED = 'CLOSED' // 已关闭（终态）
}

export const WorkloadConfirmStatusTone: Record<WorkloadConfirmStatus, StatusTone> = {
  [WorkloadConfirmStatus.DRAFT]: 'default',
  [WorkloadConfirmStatus.PM_CONFIRMED]: 'processing',
  [WorkloadConfirmStatus.AGENT_CONFIRMED]: 'processing',
  [WorkloadConfirmStatus.PENDING]: 'warning',
  [WorkloadConfirmStatus.DIRECTOR_APPROVED]: 'processing',
  [WorkloadConfirmStatus.FINANCE_APPROVED]: 'success',
  [WorkloadConfirmStatus.REJECTED]: 'error',
  [WorkloadConfirmStatus.CLOSED]: 'archived'
}

export const WorkloadConfirmStatusLabel: Record<WorkloadConfirmStatus, string> = {
  [WorkloadConfirmStatus.DRAFT]: '草稿',
  [WorkloadConfirmStatus.PM_CONFIRMED]: 'PM 已确认',
  [WorkloadConfirmStatus.AGENT_CONFIRMED]: '代理商已确认',
  [WorkloadConfirmStatus.PENDING]: '待审批',
  [WorkloadConfirmStatus.DIRECTOR_APPROVED]: '总监已审批',
  [WorkloadConfirmStatus.FINANCE_APPROVED]: '财务已审批',
  [WorkloadConfirmStatus.REJECTED]: '已驳回',
  [WorkloadConfirmStatus.CLOSED]: '已关闭'
}

/** 付款状态机：UNPAID→PAYING→PAID */
export enum PaymentStatus {
  UNPAID = 'UNPAID', // 未付款
  PAYING = 'PAYING', // 付款中
  PAID = 'PAID' // 已付款
}

export const PaymentStatusTone: Record<PaymentStatus, StatusTone> = {
  [PaymentStatus.UNPAID]: 'default',
  [PaymentStatus.PAYING]: 'processing',
  [PaymentStatus.PAID]: 'success'
}

export const PaymentStatusLabel: Record<PaymentStatus, string> = {
  [PaymentStatus.UNPAID]: '未付款',
  [PaymentStatus.PAYING]: '付款中',
  [PaymentStatus.PAID]: '已付款'
}

/** 转包任务状态机 */
export enum OutsourceStatus {
  PENDING = 'PENDING', // 待接单
  ACCEPTED = 'ACCEPTED', // 已接单
  IN_PROGRESS = 'IN_PROGRESS', // 进行中
  SUBMITTED = 'SUBMITTED', // 已提交
  CONFIRMED = 'CONFIRMED', // 已确认
  REJECTED = 'REJECTED', // 已拒绝
  RETURNED = 'RETURNED', // 已退回
  OVERDUE = 'OVERDUE' // 超期
}

export const OutsourceStatusTone: Record<OutsourceStatus, StatusTone> = {
  [OutsourceStatus.PENDING]: 'warning',
  [OutsourceStatus.ACCEPTED]: 'processing',
  [OutsourceStatus.IN_PROGRESS]: 'agent',
  [OutsourceStatus.SUBMITTED]: 'warning',
  [OutsourceStatus.CONFIRMED]: 'success',
  [OutsourceStatus.REJECTED]: 'error',
  [OutsourceStatus.RETURNED]: 'error',
  [OutsourceStatus.OVERDUE]: 'error'
}

export const OutsourceStatusLabel: Record<OutsourceStatus, string> = {
  [OutsourceStatus.PENDING]: '待接单',
  [OutsourceStatus.ACCEPTED]: '已接单',
  [OutsourceStatus.IN_PROGRESS]: '执行中',
  [OutsourceStatus.SUBMITTED]: '待审核',
  [OutsourceStatus.CONFIRMED]: '已确认',
  [OutsourceStatus.REJECTED]: '已拒绝',
  [OutsourceStatus.RETURNED]: '已退回',
  [OutsourceStatus.OVERDUE]: '已超期'
}

/** 优先级 */
export enum Priority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  URGENT = 'URGENT'
}

export const PriorityLabel: Record<Priority, string> = {
  [Priority.LOW]: '低',
  [Priority.MEDIUM]: '中',
  [Priority.HIGH]: '高',
  [Priority.URGENT]: '紧急'
}

/** 启用/禁用 */
export enum EnableStatus {
  ENABLED = 1,
  DISABLED = 0
}
