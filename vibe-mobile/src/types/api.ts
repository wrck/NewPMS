/**
 * 通用 API 类型定义
 * 字段对齐后端 VO/DTO（com.vibe.*）
 */

/** 统一响应体（后端 Result<T>，成功 code = 200） */
export interface Result<T = unknown> {
  /** 业务状态码：200 成功，其它为错误码 */
  code: number
  message: string
  data: T
  timestamp?: number
}

/** 分页查询基础参数（对齐后端 com.vibe.common.base.PageQuery） */
export interface PageQuery {
  /** 页码，从 1 开始 */
  page?: number
  /** 每页大小 */
  size?: number
  /** 排序字段（可多，逗号分隔） */
  sortField?: string
  /** 排序方向：ASC / DESC */
  sortOrder?: 'ASC' | 'DESC'
}

/** 分页结果（对齐后端 com.vibe.common.result.PageResult） */
export interface PageResult<T = unknown> {
  /** 数据列表 */
  records: T[]
  /** 总记录数 */
  total: number
  /** 当前页码（从 1 开始） */
  page: number
  /** 每页大小 */
  size: number
  /** 总页数 */
  pages: number
}

/** 错误码常量（对齐后端 ResultCode） */
export const ErrorCode = {
  /** 成功 */
  SUCCESS: 200,
  /** HTTP 401（未授权，由 axios 拦截器判定） */
  HTTP_UNAUTHORIZED: 401,
  /** HTTP 403 */
  HTTP_FORBIDDEN: 403,
  /** 未登录或登录已失效 */
  UNAUTHORIZED: 40100,
  /** Token 无效 */
  TOKEN_INVALID: 40101,
  /** Token 已过期 */
  TOKEN_EXPIRED: 40102,
  /** Token 已被加入黑名单 */
  TOKEN_BLACKLISTED: 40103,
  /** 权限不足 */
  FORBIDDEN: 40300,
  /** 资源不存在 */
  NOT_FOUND: 40400,
  /** 业务冲突 */
  BUSINESS_CONFLICT: 40900,
  /** 系统内部错误 */
  INTERNAL_ERROR: 50000
} as const

/** 判定是否为认证类错误码（40100-40199） */
export function isAuthErrorCode(code: number): boolean {
  return code >= 40100 && code <= 40199
}

/** 判定是否为权限类错误码（40300-40399） */
export function isForbiddenErrorCode(code: number): boolean {
  return code >= 40300 && code <= 40399
}

/** GPS 定位信息（对齐后端 com.vibe.delivery.bo.GpsLocation） */
export interface GpsLocation {
  /** 经度 */
  longitude?: number
  /** 纬度 */
  latitude?: number
  /** 详细地址 */
  address?: string
  /** 与客户现场距离（米） */
  distanceMeters?: number
  /** 拍照/签到时间（yyyy-MM-dd HH:mm:ss） */
  timeText?: string
}

/** 登录响应（对齐后端 com.vibe.auth.vo.LoginVO） */
export interface LoginResult {
  /** JWT Token */
  token: string
  /** Token 类型，固定 Bearer */
  tokenType?: string
  /** 有效期（秒） */
  expiresIn: number
  /** 用户ID */
  userId: number | string
  /** 用户名 */
  userName: string
  /** 真实姓名 */
  realName?: string
  /** 租户类型 */
  tenantType?: string
}

/** 用户信息（前端从 LoginVO 派生） */
export interface UserInfo {
  id: number | string
  username: string
  nickname: string
  avatar?: string
  phone?: string
  email?: string
  /** 角色编码列表 */
  roles: string[]
  /** 权限编码列表 */
  permissions: string[]
  /** 所属组织ID */
  orgId?: number | string
  /** 工程师ID（关联资源调度） */
  engineerId?: number | string
}

/**
 * 工单状态枚举（后端 WorkOrderStatusEnum）
 * PENDING_CHECKIN 待签到 / IN_PROGRESS 进行中 / COMPLETED 工程师已完成 / CONFIRMED PM 已确认
 */
export enum WorkOrderStatusEnum {
  PENDING_CHECKIN = 'PENDING_CHECKIN',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CONFIRMED = 'CONFIRMED',
  CANCELLED = 'CANCELLED'
}

/** 工单状态中文映射 */
export const WorkOrderStatusMap: Record<string, { label: string; color: string }> = {
  [WorkOrderStatusEnum.PENDING_CHECKIN]: { label: '待签到', color: '#1677ff' },
  [WorkOrderStatusEnum.IN_PROGRESS]: { label: '进行中', color: '#faad14' },
  [WorkOrderStatusEnum.COMPLETED]: { label: '已完成', color: '#52c41a' },
  [WorkOrderStatusEnum.CONFIRMED]: { label: '已确认', color: '#52c41a' },
  [WorkOrderStatusEnum.CANCELLED]: { label: '已取消', color: '#8c8c8c' }
}

/** 兼容旧引用：任务状态枚举（与工单状态一致） */
export const TaskStatusEnum = WorkOrderStatusEnum
export const TaskStatusMap = WorkOrderStatusMap

/**
 * 工单视图对象（对齐后端 com.vibe.delivery.vo.WorkOrderVO）
 * 列表/详情共用，列表场景 steps/photos/issues 为 null。
 */
export interface WorkOrderVO {
  /** 工单ID */
  id: number | string
  /** 项目任务ID */
  taskId?: number | string
  /** 任务名称 */
  taskName?: string
  /** 项目ID */
  projectId?: number | string
  /** 项目名称 */
  projectName?: string
  /** 工程师ID */
  engineerId?: number | string
  /** 工程师姓名 */
  engineerName?: string
  /** 签到时间 */
  checkinTime?: string
  /** 签退时间 */
  checkoutTime?: string
  /** 签到 GPS 坐标与地址 */
  checkinLocation?: GpsLocation
  /** 签退 GPS 坐标与地址 */
  checkoutLocation?: GpsLocation
  /** 签到照片预签名 URL */
  checkinPhotoUrl?: string
  /** 工单状态 */
  status: string
  /** 总工时（小时） */
  totalDuration?: number | string
  /** 照片数量 */
  photoCount?: number
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime?: string
  /** 步骤完成数 */
  completedStepCount?: number
  /** 步骤总数 */
  totalStepCount?: number
  /** 施工步骤列表（详情场景） */
  steps?: WorkOrderStepVO[]
  /** 施工照片列表（详情场景） */
  photos?: WorkOrderPhotoVO[]
  /** 异常问题列表（详情场景） */
  issues?: WorkOrderIssueVO[]
}

/** 兼容旧引用：TaskItem = WorkOrderVO */
export type TaskItem = WorkOrderVO

/** 施工步骤状态：WAITING 待完成 / COMPLETED 已完成 / SKIPPED 已跳过 */
export type WorkOrderStepStatus = 'WAITING' | 'COMPLETED' | 'SKIPPED'

/**
 * 工单施工步骤 VO（对齐后端 com.vibe.delivery.vo.WorkOrderStepVO）
 */
export interface WorkOrderStepVO {
  /** 步骤ID */
  id: number | string
  /** 工单ID */
  workOrderId: number | string
  /** 步骤序号 */
  stepNo: number
  /** 步骤名称 */
  stepName: string
  /** 状态 WAITING/COMPLETED/SKIPPED */
  status: WorkOrderStepStatus
  /** 完成时间 */
  completedTime?: string
  /** 耗时（秒） */
  duration?: number
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime?: string

  /* ============ 前端组件兼容字段（StepTracker 组件使用） ============ */
  /** 步骤描述（前端展示用，后端暂未返回） */
  description?: string
  /** 是否需要拍照（前端展示用） */
  needPhoto?: boolean
  /** 最少照片数（前端展示用） */
  minPhotoCount?: number
  /** 已拍照数量 */
  photoCount?: number
  /** 完成时间（旧版兼容字段，等价于 completedTime） */
  completedAt?: string
}

/** 兼容旧引用：WorkStep = WorkOrderStepVO */
export type WorkStep = WorkOrderStepVO

/**
 * 工单施工照片 VO（对齐后端 com.vibe.delivery.vo.WorkOrderPhotoVO）
 */
export interface WorkOrderPhotoVO {
  /** 照片ID */
  id: number | string
  /** 工单ID */
  workOrderId: number | string
  /** 关联步骤ID */
  stepId?: number | string
  /** 照片预签名 URL */
  photoUrl: string
  /** 缩略图预签名 URL */
  thumbnailUrl?: string
  /** GPS 信息 */
  gps?: GpsLocation
  /** 拍摄时间 */
  takenTime?: string
  /** 上传人ID */
  uploadedBy?: number | string
  /** 上传人姓名 */
  uploadedByName?: string
  /** 创建时间 */
  createTime?: string
}

/** 异常问题状态：OPEN / PROCESSING / RESOLVED / CLOSED */
export type WorkOrderIssueStatus = 'OPEN' | 'PROCESSING' | 'RESOLVED' | 'CLOSED'

/** 严重程度：MINOR 轻微 / MAJOR 严重 / BLOCKING 阻断 */
export type IssueSeverity = 'MINOR' | 'MAJOR' | 'BLOCKING'

/**
 * 工单异常问题 VO（对齐后端 com.vibe.delivery.vo.WorkOrderIssueVO）
 */
export interface WorkOrderIssueVO {
  /** 问题ID */
  id: number | string
  /** 工单ID */
  workOrderId: number | string
  /** 问题类型（DEVICE_FAULT/CONFIG_ERROR/SITE_ISSUE/OTHER） */
  issueType?: string
  /** 严重程度 MINOR/MAJOR/BLOCKING */
  severity: string
  /** 问题描述 */
  description: string
  /** 问题照片预签名 URL 列表 */
  photoUrls?: string[]
  /** 状态 */
  status: WorkOrderIssueStatus | string
  /** 解决时间 */
  resolvedTime?: string
  /** 备注 */
  remark?: string
  /** 创建人ID */
  createBy?: number | string
  /** 创建人姓名 */
  createByName?: string
  /** 创建时间 */
  createTime?: string
}

/** 异常问题上报 DTO（对齐后端 WorkOrderIssueReportDTO） */
export interface WorkOrderIssueReportDTO {
  /** 问题类型 */
  issueType?: string
  /** 严重程度 MINOR/MAJOR/BLOCKING */
  severity: IssueSeverity | string
  /** 问题描述 */
  description: string
  /** 问题照片地址列表（已上传到 MinIO 的 objectName） */
  photos?: string[]
  /** 备注 */
  remark?: string
}

/** 工时状态：SUBMITTED 已提交 / APPROVED 已批准 / REJECTED 已驳回 */
export type TimesheetStatus = 'SUBMITTED' | 'APPROVED' | 'REJECTED'

/**
 * 工时 VO（对齐后端 com.vibe.resource.vo.TimesheetVO）
 */
export interface TimesheetVO {
  /** 工时ID */
  id: number | string
  /** 工程师ID */
  engineerId?: number | string
  /** 工程师姓名 */
  engineerName?: string
  /** 项目ID */
  projectId?: number | string
  /** 项目名称 */
  projectName?: string
  /** 任务ID */
  taskId?: number | string
  /** 任务名称 */
  taskName?: string
  /** 工作日期 */
  workDate?: string
  /** 工作时长（小时） */
  hours?: number | string
  /** 加班时长（小时） */
  overtimeHours?: number | string
  /** 出差天数 */
  travelDays?: number
  /** 工作内容说明 */
  description?: string
  /** 状态 SUBMITTED/APPROVED/REJECTED */
  status?: TimesheetStatus | string
  /** 审批人ID */
  approverId?: number | string
  /** 审批人姓名 */
  approverName?: string
  /** 审批时间 */
  approveTime?: string
  /** 创建时间 */
  createTime?: string
}

/** 工时填报 DTO（对齐后端 com.vibe.resource.dto.TimesheetDTO） */
export interface TimesheetDTO {
  /** 工时ID（编辑时必填） */
  id?: number | string
  /** 工程师ID（PM 代填时必填） */
  engineerId?: number | string
  /** 项目ID */
  projectId?: number | string
  /** 任务ID */
  taskId?: number | string
  /** 工作日期 */
  workDate: string
  /** 工作时长（小时） */
  hours: number | string
  /** 加班时长（小时） */
  overtimeHours?: number | string
  /** 出差天数 */
  travelDays?: number
  /** 工作内容说明 */
  description?: string
}

/**
 * 工时统计 VO（对齐后端 com.vibe.resource.vo.TimesheetStatsVO）
 */
export interface TimesheetStatsVO {
  /** 工程师ID */
  engineerId?: number | string
  /** 工程师姓名 */
  engineerName?: string
  /** 项目ID */
  projectId?: number | string
  /** 项目名称 */
  projectName?: string
  /** 统计键（按月度维度时为 yyyy-MM） */
  statKey?: string
  /** 总工时（小时） */
  totalHours?: number | string
  /** 总加班时长（小时） */
  totalOvertimeHours?: number | string
  /** 总出差天数 */
  totalTravelDays?: number
  /** 总人天（按 8 小时/天折算） */
  totalManDays?: number | string
  /** 记录数 */
  recordCount?: number
}

/**
 * 站内信 VO（对齐后端 com.vibe.system.vo.SysNoticeVO）
 */
export interface SysNoticeVO {
  /** 通知ID */
  id: number | string
  /** 通知标题 */
  noticeTitle: string
  /** 通知类型 1-通知 2-消息 */
  noticeType?: number
  /** 通知内容 */
  noticeContent?: string
  /** 接收人ID */
  recipientId?: number | string
  /** 已读状态 0-未读 1-已读 */
  readStatus: number
  /** 发送时间 */
  sendTime?: string
}

/** 上传文件元信息（前端 IndexedDB 缓存用） */
export interface UploadFileMeta {
  /** 本地唯一ID */
  localId: string
  /** 业务关联ID（工单ID/步骤ID等） */
  bizId: string
  /** 业务类型 */
  bizType: string
  /** 文件名 */
  name: string
  /** 文件大小 */
  size: number
  /** MIME 类型 */
  type: string
  /** base64 缩略图 */
  thumbnail?: string
  /** GPS 经度 */
  longitude?: number
  /** GPS 纬度 */
  latitude?: number
  /** 拍照时间 */
  capturedAt: string
  /** 上传人 */
  uploader: string
  /** 上传状态 */
  status: 'PENDING' | 'UPLOADING' | 'SUCCESS' | 'FAILED'
  /** 远端文件URL（成功后回填） */
  remoteUrl?: string
  /** 已上传字节 */
  loaded?: number
  /** 重试次数 */
  retryCount?: number
  /** 创建时间 */
  createdAt: number
}
