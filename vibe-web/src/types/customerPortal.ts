/**
 * 客户门户 H5 类型定义
 * 对应后端：com.vibe.collaboration.vo.Customer* / ProjectProgressVO / DocumentVO
 */

/** 客户视角的项目列表项（脱敏） */
export interface CustomerProject {
  projectId: number
  projectCode: string
  projectName: string
  projectType?: string
  currentPhase?: string
  progressPct: number
  plannedStart?: string
  plannedEnd?: string
  status: string
}

/** 阶段时间线项 */
export interface PhaseTimeline {
  phaseId?: number
  phaseCode: string
  phaseName: string
  status: string // NOT_STARTED / IN_PROGRESS / COMPLETED
  sortOrder: number
  plannedStart?: string
  plannedEnd?: string
  actualStart?: string
  actualEnd?: string
  progressPct?: number
}

/** 项目整体进度 */
export interface ProjectProgress {
  projectId: number
  projectName: string
  progressPct: number
  currentPhaseName?: string
  overallStatus: string
  phases: PhaseTimeline[]
}

/** 可下载文档 */
export interface CustomerDocument {
  docId: string
  docName: string
  docType: string // DESIGN / REPORT / CONFIG / OTHER
  uploadTime?: string
  downloadUrl?: string
}

/** 客户割接步骤 */
export interface CustomerCutoverStep {
  id: number
  sortOrder: number
  stepName: string
  description?: string
  estimatedDuration?: number
  ownerName?: string
  status: string
  actualStartTime?: string
  actualEndTime?: string
  actualDuration?: number
}

/** 客户割接方案（脱敏） */
export interface CustomerCutoverPlan {
  id: number
  projectId: number
  planName: string
  cutoverDate?: string
  startTime?: string
  endTime?: string
  impactScope?: string
  emergencyContact?: string
  status: string
  customerSignResult?: string // APPROVED / REJECTED / null
  customerSignTime?: string
  customerSignRemark?: string
  stepCount?: number
  completedStepCount?: number
  steps: CustomerCutoverStep[]
}

/** 客户验收测试记录 */
export interface CustomerTestRecord {
  id: number
  testType: string // FUNCTION / PERFORMANCE / REDUNDANCY / OTHER
  testName: string
  testResult: string // PENDING / PASS / FAIL / NA
  testValue?: string
  testTime?: string
  remark?: string
}

/** 客户验收任务（脱敏） */
export interface CustomerAcceptanceTask {
  id: number
  projectId: number
  name: string
  status: string // DRAFT / APPLIED / INTERNAL_AUDITED / CUSTOMER_SIGNING / COMPLETED / REJECTED
  applyTime?: string
  internalAuditTime?: string
  customerSignTime?: string
  customerSignResult?: string // PASS / CONDITIONAL_PASS / REJECT / null
  customerSignRemark?: string
  testRecords: CustomerTestRecord[]
}

/** 客户待办事项 */
export interface CustomerTodo {
  type: string // CUTOVER_APPROVAL / ACCEPTANCE_SIGN
  businessId: number
  projectId: number
  projectName: string
  title: string
  signToken: string
  createTime: string
}

/** 客户消息 */
export interface CustomerMessage {
  id: number
  customerId: number
  messageType: string // PROJECT_PROGRESS / CUTOVER_NOTICE / ACCEPTANCE_NOTICE / DOCUMENT_UPLOAD
  businessId?: number
  projectId?: number
  title: string
  content?: string
  isRead: number // 0-未读 1-已读
  createTime: string
}

/** 割接审批请求 */
export interface CustomerCutoverApprovalDTO {
  token: string
  result: 'APPROVED' | 'REJECTED'
  remark?: string
  signUser?: string
}

/** 验收签核请求 */
export interface CustomerAcceptanceSignDTO {
  token: string
  result: 'PASS' | 'CONDITIONAL_PASS' | 'REJECT'
  remark?: string
  signUser?: string
}
