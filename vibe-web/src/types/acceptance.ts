/**
 * 验收管理模块类型定义
 * 对应后端：/api/v1/acceptance/{standards|tasks|issues|docs}
 * 设计文档 2.7
 */
import type { PageParams } from './api'

/* ============ 枚举类型 ============ */

/** 验收任务状态 */
export type AcceptanceTaskStatus =
  | 'DRAFT'           // 草稿
  | 'APPLIED'         // 已申请
  | 'INTERNAL_AUDITED' // 内部审核通过
  | 'CUSTOMER_SIGNING' // 客户签核中
  | 'COMPLETED'       // 已完成
  | 'REJECTED'        // 已驳回

/** 遗留问题状态 */
export type AcceptanceIssueStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED'

/** 严重等级 */
export type IssueSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

/** 测试类型 */
export type TestType = 'FUNCTION' | 'PERFORMANCE' | 'REDUNDANCY' | 'OTHER'

/** 测试结果 */
export type TestResult = 'PENDING' | 'PASS' | 'FAIL' | 'NA'

/** 签核结果 */
export type SignResult = 'PASS' | 'CONDITIONAL_PASS' | 'REJECT'

/** 竣工文档类型 */
export type AcceptanceDocType =
  | 'TOPOLOGY'            // As-Built 网络拓扑图
  | 'DEVICE_LIST'         // 设备清单
  | 'CONFIG_BACKUP'       // 配置备份文件
  | 'TEST_REPORT'         // 测试报告
  | 'MAINTENANCE_MANUAL'  // 维护操作手册
  | 'OTHER'

/* ============ 实体类型 ============ */

/** 验收检查项 */
export interface AcceptanceStandardItem {
  id?: number
  standardId?: number
  name: string
  requirement?: string
  testMethod?: string
  weight?: number
  sortOrder?: number
}

/** 验收标准 */
export interface AcceptanceStandard {
  id: number
  name: string
  projectType?: string
  standardVersion?: string
  description?: string
  status?: number
  items?: AcceptanceStandardItem[]
  createTime?: string
  updateTime?: string
}

/** 验收任务 */
export interface AcceptanceTask {
  id: number
  projectId: number
  /** 关联项目名称（后端 JOIN project 表填充） */
  projectName?: string
  standardId?: number
  /** 关联验收标准名称（后端 JOIN acceptance_standard 表填充） */
  standardName?: string
  name: string
  applyUserId?: number
  applyTime?: string
  internalAuditUserId?: number
  internalAuditTime?: string
  internalAuditResult?: 'PASS' | 'REJECT'
  customerSignLink?: string
  customerSignUser?: string
  customerSignTime?: string
  customerSignResult?: SignResult
  score?: number
  status: AcceptanceTaskStatus
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 验收测试记录 */
export interface AcceptanceTestRecord {
  id: number
  taskId: number
  itemId?: number
  testType: TestType
  testName: string
  testResult: TestResult
  testValue?: string
  evidenceUrl?: string
  testerId?: number
  testTime?: string
  remark?: string
  createTime?: string
}

/** 遗留问题 */
export interface AcceptanceIssue {
  id: number
  taskId: number
  projectId: number
  /** 关联项目名称（后端 JOIN project 表填充） */
  projectName?: string
  name: string
  description?: string
  severity: IssueSeverity
  assigneeId?: number
  dueDate?: string
  resolvedTime?: string
  status: AcceptanceIssueStatus
  closeUserId?: number
  closeTime?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 竣工文档 */
export interface AcceptanceDoc {
  id: number
  taskId: number
  projectId: number
  /** 关联项目名称（后端 JOIN project 表填充） */
  projectName?: string
  docType: AcceptanceDocType
  name: string
  fileUrl: string
  fileSize?: number
  docVersion?: string
  uploaderId?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/* ============ 查询参数 ============ */

/** 验收标准查询 */
export interface AcceptanceStandardQuery extends PageParams {
  name?: string
  projectType?: string
  status?: number
}

/** 验收任务查询 */
export interface AcceptanceTaskQuery extends PageParams {
  projectId?: number
  name?: string
  status?: AcceptanceTaskStatus
  applyUserId?: number
}

/** 遗留问题查询 */
export interface AcceptanceIssueQuery extends PageParams {
  projectId?: number
  taskId?: number
  status?: AcceptanceIssueStatus
  severity?: IssueSeverity
  assigneeId?: number
}

/** 竣工文档查询 */
export interface AcceptanceDocQuery extends PageParams {
  projectId?: number
  taskId?: number
  docType?: AcceptanceDocType
}

/* ============ DTO 类型 ============ */

/** 验收标准创建/更新 */
export interface AcceptanceStandardDTO {
  id?: number
  name: string
  projectType?: string
  standardVersion?: string
  description?: string
  status?: number
  items?: AcceptanceStandardItem[]
}

/** 验收任务创建 */
export interface AcceptanceTaskDTO {
  projectId: number
  standardId?: number
  name: string
  remark?: string
}

/** 验收任务状态流转 */
export interface AcceptanceTaskAction {
  taskId: number
  result?: 'PASS' | 'REJECT' | 'CONDITIONAL_PASS'
  customerSignUser?: string
  remark?: string
}

/** 遗留问题创建/更新 */
export interface AcceptanceIssueDTO {
  id?: number
  taskId: number
  projectId: number
  name: string
  description?: string
  severity?: IssueSeverity
  assigneeId?: number
  dueDate?: string
  remark?: string
}

/** 竣工文档创建/更新 */
export interface AcceptanceDocDTO {
  id?: number
  taskId: number
  projectId: number
  docType: AcceptanceDocType
  name: string
  fileUrl: string
  fileSize?: number
  docVersion?: string
  remark?: string
}
