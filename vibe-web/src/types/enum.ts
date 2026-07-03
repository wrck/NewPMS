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
  [ProjectStatus.INIT]: '已立项',
  [ProjectStatus.PLAN]: '计划中',
  [ProjectStatus.EXECUTE]: '执行中',
  [ProjectStatus.ACCEPT]: '验收中',
  [ProjectStatus.CLOSE]: '已结项',
  [ProjectStatus.ARCHIVED]: '已归档',
  [ProjectStatus.ON_HOLD]: '挂起',
  [ProjectStatus.CANCELLED]: '已取消'
}

/** 设备状态机：IN_FACTORY→...→ONLINE 及异常分支 */
export enum DeviceStatus {
  IN_FACTORY = 'IN_FACTORY', // 在库
  SHIPPED = 'SHIPPED', // 发运中
  ARRIVED = 'ARRIVED', // 已到货
  INSTALLING = 'INSTALLING', // 安装中
  ONLINE = 'ONLINE', // 在网
  OFFLINE = 'OFFLINE', // 离线
  ABNORMAL = 'ABNORMAL', // 异常
  SCRAPPED = 'SCRAPPED' // 报废
}

export const DeviceStatusTone: Record<DeviceStatus, StatusTone> = {
  [DeviceStatus.IN_FACTORY]: 'default',
  [DeviceStatus.SHIPPED]: 'processing',
  [DeviceStatus.ARRIVED]: 'processing',
  [DeviceStatus.INSTALLING]: 'processing',
  [DeviceStatus.ONLINE]: 'success',
  [DeviceStatus.OFFLINE]: 'archived',
  [DeviceStatus.ABNORMAL]: 'error',
  [DeviceStatus.SCRAPPED]: 'archived'
}

export const DeviceStatusLabel: Record<DeviceStatus, string> = {
  [DeviceStatus.IN_FACTORY]: '在库',
  [DeviceStatus.SHIPPED]: '发运中',
  [DeviceStatus.ARRIVED]: '已到货',
  [DeviceStatus.INSTALLING]: '安装中',
  [DeviceStatus.ONLINE]: '在网',
  [DeviceStatus.OFFLINE]: '离线',
  [DeviceStatus.ABNORMAL]: '异常',
  [DeviceStatus.SCRAPPED]: '报废'
}

/** 任务/工单状态 */
export enum TaskStatus {
  TODO = 'TODO', // 未开始
  ASSIGNED = 'ASSIGNED', // 已派发
  IN_PROGRESS = 'IN_PROGRESS', // 进行中
  SUBMITTED = 'SUBMITTED', // 已提交
  CONFIRMED = 'CONFIRMED', // 已确认
  REJECTED = 'REJECTED', // 已驳回
  OVERDUE = 'OVERDUE', // 超期
  CANCELLED = 'CANCELLED' // 已取消
}

export const TaskStatusTone: Record<TaskStatus, StatusTone> = {
  [TaskStatus.TODO]: 'default',
  [TaskStatus.ASSIGNED]: 'processing',
  [TaskStatus.IN_PROGRESS]: 'processing',
  [TaskStatus.SUBMITTED]: 'warning',
  [TaskStatus.CONFIRMED]: 'success',
  [TaskStatus.REJECTED]: 'error',
  [TaskStatus.OVERDUE]: 'error',
  [TaskStatus.CANCELLED]: 'archived'
}

export const TaskStatusLabel: Record<TaskStatus, string> = {
  [TaskStatus.TODO]: '未开始',
  [TaskStatus.ASSIGNED]: '已派发',
  [TaskStatus.IN_PROGRESS]: '进行中',
  [TaskStatus.SUBMITTED]: '已提交',
  [TaskStatus.CONFIRMED]: '已确认',
  [TaskStatus.REJECTED]: '已驳回',
  [TaskStatus.OVERDUE]: '超期',
  [TaskStatus.CANCELLED]: '已取消'
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
  [OutsourceStatus.IN_PROGRESS]: '进行中',
  [OutsourceStatus.SUBMITTED]: '已提交',
  [OutsourceStatus.CONFIRMED]: '已确认',
  [OutsourceStatus.REJECTED]: '已拒绝',
  [OutsourceStatus.RETURNED]: '已退回',
  [OutsourceStatus.OVERDUE]: '超期'
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
