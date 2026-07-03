/**
 * 通用 API 类型定义 - 外部入口 H5
 * 字段命名对齐后端 VO（module-collaboration / module-agent / module-auth）
 */

/** 统一响应体 */
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
  timestamp?: number
  /** 是否成功（code === 0 视为成功） */
  success?: boolean
}

/** 分页结果（对齐后端 PageResult） */
export interface PageResult<T = unknown> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

/** 分页查询基础参数（对齐后端 page/size 命名） */
export interface PageQuery {
  page?: number
  size?: number
  orderBy?: string
  order?: 'asc' | 'desc'
}

/** 错误码常量 */
export const ErrorCode = {
  SUCCESS: 0,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  SERVER_ERROR: 500,
  BUSINESS_ERROR: 10001
} as const

/** 客户端类型（认证 clientId） */
export type ClientId = 'PORTAL_CUSTOMER' | 'PORTAL_AGENT'

/* ===================== 认证（module-auth） ===================== */

/** 登录参数（手机号+验证码 或 账号+密码，由 clientId 区分入口） */
export interface LoginParams {
  clientId: ClientId
  /** 模式一：手机号 + 短信验证码 */
  phone?: string
  smsCode?: string
  /** 一次性 Token（链接登录场景，可选） */
  oneTimeToken?: string
  /** 模式二：账号 + 密码 */
  username?: string
  password?: string
}

/** 登录响应 */
export interface LoginResult<U extends UserInfo = UserInfo> {
  token: string
  refreshToken?: string
  /** 有效期（秒） */
  expiresIn: number
  /** 过期时间（毫秒时间戳，可选） */
  expiresAt?: number
  /** 当前用户信息 */
  user: U
}

/** 用户信息（客户/代理商字段的并集，由各端解释） */
export interface UserInfo {
  id: number | string
  /** 账号（代理商） */
  username?: string
  /** 显示名（客户名称 / 联系人姓名） */
  name?: string
  phone?: string
  avatar?: string
  roles?: string[]
  /** 代理商公司信息 */
  companyId?: number | string
  companyName?: string
  contactName?: string
  /** 客户关联项目ID列表 */
  projectIds?: (number | string)[]
}

/** 客户信息（兼容别名） */
export type CustomerInfo = UserInfo
/** 代理商信息（兼容别名） */
export type AgentInfo = UserInfo

/* ===================== 客户侧（module-collaboration） ===================== */

/** 客户项目列表项 CustomerProjectVO */
export interface CustomerProjectVO {
  projectId: number | string
  projectCode: string
  projectName: string
  projectType?: string
  currentPhase?: string
  /** 整体进度百分比 0-100 */
  progressPct: number
  plannedStart?: string
  plannedEnd?: string
  status?: string
}

/** 项目进度详情 ProjectProgressVO */
export interface ProjectProgressVO {
  projectId: number | string
  projectName: string
  /** 整体进度百分比 0-100 */
  progressPct: number
  currentPhaseName?: string
  /** 阶段时间线 */
  phases: PhaseTimelineVO[]
}

/** 阶段时间线 PhaseTimelineVO */
export interface PhaseTimelineVO {
  id: number | string
  name: string
  status: 'DONE' | 'IN_PROGRESS' | 'PENDING' | 'SKIPPED'
  /** 阶段进度百分比 0-100 */
  percent?: number
  planStart?: string
  planEnd?: string
  /** 实际完成时间 */
  actualEnd?: string
}

/** 项目文档 CustomerDocumentVO */
export interface CustomerDocumentVO {
  id: number | string
  name: string
  /** 文件类型，如 pdf/docx */
  fileType?: string
  size?: number
  uploadTime?: string
  /** 下载 URL（预签名） */
  downloadUrl?: string
  version?: string
}

/* ===================== 代理商侧（module-agent） ===================== */

/** 转包任务状态枚举 */
export enum OutsourceTaskStatusEnum {
  /** 待接单 */
  PENDING = 'PENDING',
  /** 已接单（待指派工程师） */
  ACCEPTED = 'ACCEPTED',
  /** 进行中 */
  IN_PROGRESS = 'IN_PROGRESS',
  /** 待审核（已提交交付物） */
  SUBMITTED = 'SUBMITTED',
  /** 审核通过 */
  CONFIRMED = 'CONFIRMED',
  /** 已驳回 */
  REJECTED = 'REJECTED',
  /** 已取消 */
  CANCELLED = 'CANCELLED'
}

/** 转包任务状态中文映射 */
export const OutsourceTaskStatusMap: Record<
  OutsourceTaskStatusEnum,
  { label: string; color: string; type: 'primary' | 'success' | 'warning' | 'danger' | 'default' }
> = {
  [OutsourceTaskStatusEnum.PENDING]: { label: '待接单', color: '#1677ff', type: 'primary' },
  [OutsourceTaskStatusEnum.ACCEPTED]: { label: '已接单', color: '#13c2c2', type: 'primary' },
  [OutsourceTaskStatusEnum.IN_PROGRESS]: { label: '进行中', color: '#faad14', type: 'warning' },
  [OutsourceTaskStatusEnum.SUBMITTED]: { label: '待审核', color: '#fa8c16', type: 'warning' },
  [OutsourceTaskStatusEnum.CONFIRMED]: { label: '已通过', color: '#52c41a', type: 'success' },
  [OutsourceTaskStatusEnum.REJECTED]: { label: '已驳回', color: '#ff4d4f', type: 'danger' },
  [OutsourceTaskStatusEnum.CANCELLED]: { label: '已取消', color: '#8c8c8c', type: 'default' }
}

/** 转包任务 OutsourceTaskVO（代理商视角，敏感字段已脱敏） */
export interface OutsourceTaskVO {
  id: number | string
  /** 任务编号 */
  taskCode: string
  /** 项目名称（脱敏：隐藏 projectCode/customerName/contractAmount/costAmount） */
  projectName: string
  /** 站点/位置 */
  siteName: string
  status: OutsourceTaskStatusEnum
  /** 截止时间 */
  deadline?: string
  /** PM（项目负责人） */
  pmName?: string
  /** 执行人（本代理商工程师） */
  executorName?: string
  /** 任务要求说明 */
  requirement?: string
  /** 完成进度文本（如 12/20 个AP） */
  progressText?: string
  /** 进度百分比 0-100 */
  percent?: number
  /** 已提交交付物 */
  deliverables?: OutsourceDeliverableVO[]
}

/** 转包任务详情（在列表项基础上扩展） */
export interface OutsourceTaskDetailVO extends OutsourceTaskVO {
  /** 地址 */
  address?: string
  /** 联系人 */
  contactName?: string
  /** 联系电话 */
  contactPhone?: string
  /** 计划开始 */
  planStartTime?: string
  /** 计划结束 */
  planEndTime?: string
  /** 详细描述 */
  description?: string
  /** 交付要求清单 */
  deliverableRequirements?: DeliverableRequirementVO[]
}

/** 交付物要求 DeliverableRequirementVO */
export interface DeliverableRequirementVO {
  id: number | string
  name: string
  /** 类型：施工照片/测试记录/签收单 */
  type: DeliverableType
  required: boolean
  /** 最少数量（照片类） */
  minCount?: number
  remark?: string
}

/** 交付物类型 */
export type DeliverableType = 'PHOTO' | 'TEST_RECORD' | 'RECEIPT' | 'OTHER'

/** 已提交交付物 OutsourceDeliverableVO */
export interface OutsourceDeliverableVO {
  id: number | string
  taskId: number | string
  /** 提交类型 */
  type?: string
  /** 施工照片 URL 列表 */
  photos?: string[]
  /** 测试记录文件 URL 列表 */
  testRecords?: string[]
  /** 签收单文件 URL 列表 */
  receipts?: string[]
  /** 提交时间 */
  submitTime?: string
  /** 状态：草稿/已提交/已确认/已驳回 */
  status?: 'DRAFT' | 'SUBMITTED' | 'CONFIRMED' | 'REJECTED'
  remark?: string
}

/** 交付物提交参数 */
export interface SubmitDeliverableParams {
  /** 施工照片 URL 列表（必传，至少 N 张） */
  photos: string[]
  /** 测试记录文件 URL 列表（必传） */
  testRecords: string[]
  /** 签收单文件 URL 列表（必传） */
  receipts: string[]
  /** 备注 */
  remark?: string
}

/** 指派工程师参数 */
export interface AssignEngineerParams {
  /** 工程师ID（如有工程师列表则传，否则可仅传姓名） */
  engineerId?: number | string
  /** 工程师姓名 */
  engineerName?: string
}

/** 工作量明细项 */
export interface WorkloadItem {
  name: string
  unit?: string
  quantity?: number
  unitPrice?: number
  amount?: number
  remark?: string
}

/** 工作量查询结果 WorkloadVO */
export interface WorkloadVO {
  taskId: number | string
  /** 工作量明细 */
  items: WorkloadItem[]
  /** 合计金额 */
  totalAmount?: number
  /** 状态 */
  status?: string
  /** 提交时间 */
  submitTime?: string
  remark?: string
}

/** 提交工作量参数 */
export interface SubmitWorkloadParams {
  items: WorkloadItem[]
  remark?: string
}

/** 代理商评分 AgentScoreVO */
export interface AgentScoreVO {
  id: number | string
  taskId?: number | string
  taskCode?: string
  projectName?: string
  /** 评分（0-100 或 0-5，按后端约定） */
  score: number
  /** 评级，如 优/良/中/差 */
  level?: string
  /** 评语 */
  comment?: string
  /** 评分时间 */
  scoreTime?: string
}

/** 代理商工作台统计 */
export interface AgentDashboardStat {
  /** 待接单数 */
  pendingCount: number
  /** 进行中数 */
  inProgressCount: number
  /** 待审核数 */
  pendingReviewCount: number
}

/* ===================== 通用上传 ===================== */

/** 上传文件元信息 */
export interface UploadFileMeta {
  localId: string
  name: string
  size: number
  type: string
  /** 远端文件URL（上传成功后回填） */
  remoteUrl?: string
  /** base64 缩略图 */
  thumbnail?: string
  status: 'PENDING' | 'UPLOADING' | 'SUCCESS' | 'FAILED'
}
