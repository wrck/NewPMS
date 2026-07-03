/**
 * 客户端 API（module-collaboration CustomerPortalController）
 * 路径前缀 /v1/customer，需要 CUSTOMER 角色
 */
import { http } from '@/utils/request'
import type { CustomerProjectVO, ProjectProgressVO, CustomerDocumentVO } from '@/types/api'

const SCOPE = { authScope: 'customer' } as const

/** 我的项目列表 GET /v1/customer/projects */
export function getMyProjects() {
  return http.get<CustomerProjectVO[]>('/v1/customer/projects', undefined, {
    ...SCOPE,
    silent: true
  })
}

/** 项目进度详情 GET /v1/customer/projects/{projectId}/progress */
export function getProjectProgress(projectId: number | string) {
  return http.get<ProjectProgressVO>(
    `/v1/customer/projects/${projectId}/progress`,
    undefined,
    { ...SCOPE, silent: true }
  )
}

/** 项目文档列表 GET /v1/customer/projects/{projectId}/documents */
export function getProjectDocuments(projectId: number | string) {
  return http.get<CustomerDocumentVO[]>(
    `/v1/customer/projects/${projectId}/documents`,
    undefined,
    { ...SCOPE, silent: true }
  )
}

/**
 * 获取文档下载预签名 URL（如列表中 downloadUrl 缺失时回退使用）
 * 注：后端如未单独提供该端点，可直接使用文档自带的 downloadUrl
 */
export function getDocumentDownloadUrl(projectId: number | string, documentId: number | string) {
  return http.get<string>(
    `/v1/customer/projects/${projectId}/documents/${documentId}/download-url`,
    undefined,
    { ...SCOPE, silent: true }
  )
}
