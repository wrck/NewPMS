/**
 * 集成管理模块 API 封装
 * 对应后端：/api/v1/integration/configs、/api/v1/integration/call-logs
 */
import { http } from '@/utils/request'
import type { PageResult, PageParams } from '@/types/api'

/* ============ 集成配置 ============ */

export interface IntegrationConfig {
  id: number
  systemCode: string
  systemName: string
  adapterType?: string
  endpointUrl: string
  authType?: string
  authConfig?: string
  timeoutMs?: number
  retryCount?: number
  enabled: 1 | 0
  description?: string
  lastCallTime?: string
  lastCallStatus?: 'SUCCESS' | 'FAIL'
  createTime?: string
}

export interface IntegrationConfigDTO {
  id?: number
  systemCode: string
  systemName: string
  adapterType?: string
  endpointUrl: string
  authType?: string
  authConfig?: string
  timeoutMs?: number
  retryCount?: number
  enabled?: 1 | 0
  description?: string
}

export interface IntegrationConfigQueryParams extends PageParams {
  keyword?: string
  adapterType?: string
  enabled?: 1 | 0
}

const CONFIG_BASE = '/integration/configs'

export function pageIntegrationConfigs(params: IntegrationConfigQueryParams) {
  return http.get<PageResult<IntegrationConfig>>(CONFIG_BASE, params as Record<string, unknown>)
}

export function getIntegrationConfigDetail(id: number) {
  return http.get<IntegrationConfig>(`${CONFIG_BASE}/${id}`)
}

export function getIntegrationConfigByCode(systemCode: string) {
  return http.get<IntegrationConfig>(`${CONFIG_BASE}/by-code/${systemCode}`)
}

export function listEnabledIntegrationConfigs() {
  return http.get<IntegrationConfig[]>(`${CONFIG_BASE}/enabled`)
}

export function createIntegrationConfig(dto: IntegrationConfigDTO) {
  return http.post<number>(CONFIG_BASE, dto)
}

export function updateIntegrationConfig(id: number, dto: IntegrationConfigDTO) {
  return http.put<void>(`${CONFIG_BASE}/${id}`, dto)
}

export function deleteIntegrationConfig(id: number) {
  return http.delete<void>(`${CONFIG_BASE}/${id}`)
}

export function toggleIntegrationConfigEnabled(id: number, enabled: 1 | 0) {
  return http.put<void>(`${CONFIG_BASE}/${id}/enabled`, null, { params: { enabled } })
}

export function testIntegrationConnection(id: number) {
  return http.post<boolean>(`${CONFIG_BASE}/${id}/test`)
}

/* ============ 集成调用日志 ============ */

export interface IntegrationCallLog {
  id: number
  configId?: number
  systemCode: string
  callScene: string
  requestMethod?: string
  requestUrl?: string
  requestHeaders?: string
  requestBody?: string
  responseStatus?: number
  responseBody?: string
  status: 'SUCCESS' | 'FAIL' | 'TIMEOUT'
  errorMsg?: string
  costMs?: number
  callerIp?: string
  operatedAt: string
}

export interface IntegrationCallLogQueryParams extends PageParams {
  configId?: number
  systemCode?: string
  callScene?: string
  status?: 'SUCCESS' | 'FAIL' | 'TIMEOUT'
  startBegin?: string
  startEnd?: string
}

const LOG_BASE = '/integration/call-logs'

export function pageIntegrationCallLogs(params: IntegrationCallLogQueryParams) {
  return http.get<PageResult<IntegrationCallLog>>(LOG_BASE, params as Record<string, unknown>)
}

export function getIntegrationCallLogDetail(id: number) {
  return http.get<IntegrationCallLog>(`${LOG_BASE}/${id}`)
}

export function deleteIntegrationCallLog(id: number) {
  return http.delete<void>(`${LOG_BASE}/${id}`)
}

export function clearAllIntegrationCallLogs() {
  return http.post<void>(`${LOG_BASE}/clear`)
}
