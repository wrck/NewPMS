/**
 * 低代码模块 API 封装
 * 对应后端：
 *   - FormConfigController     -> /api/v1/lowcode/forms
 *   - ListConfigController     -> /api/v1/lowcode/lists
 *   - TabConfigController      -> /api/v1/lowcode/tabs
 *   - RelationConfigController -> /api/v1/lowcode/relations
 *   - TemplateController       -> /api/v1/lowcode/templates
 *
 * 每个 Controller 均提供：分页查询 / 详情 / 创建 / 更新 / 删除 / 复制 / 导出 JSON / 导入 JSON / 模板实例化。
 */
import { http } from '@/utils/request'
import service from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  LowcodeFormConfigVO,
  LowcodeListConfigVO,
  LowcodeTabConfigVO,
  LowcodeRelationConfigVO,
  LowcodeTemplateVO,
  LowcodeFormConfigDTO,
  LowcodeListConfigDTO,
  LowcodeTabConfigDTO,
  LowcodeRelationConfigDTO,
  LowcodeTemplateDTO,
  LowcodeInstantiateDTO,
  LowcodeConfigQueryParams,
  LowcodeTemplateQueryParams
} from '@/types/lowcode'

/* ============ 表单配置 ============ */

const FORM_BASE = '/lowcode/forms'

export function pageFormConfigs(params: LowcodeConfigQueryParams) {
  return http.get<PageResult<LowcodeFormConfigVO>>(FORM_BASE, params as Record<string, unknown>)
}

export function getFormConfigDetail(id: number) {
  return http.get<LowcodeFormConfigVO>(`${FORM_BASE}/${id}`)
}

export function createFormConfig(dto: LowcodeFormConfigDTO) {
  return http.post<number>(FORM_BASE, dto)
}

export function updateFormConfig(id: number, dto: LowcodeFormConfigDTO) {
  return http.put<void>(`${FORM_BASE}/${id}`, dto)
}

export function deleteFormConfig(id: number) {
  return http.delete<void>(`${FORM_BASE}/${id}`)
}

export function copyFormConfig(id: number) {
  return http.post<number>(`${FORM_BASE}/${id}/copy`)
}

/**
 * 导出表单 JSON Schema（下载文件）。
 * 后端直接返回 JSON 字符串（非标准 Result 包装）。
 */
export function exportFormConfigJson(id: number): Promise<Blob> {
  return service.get(`${FORM_BASE}/${id}/export`, { responseType: 'blob' }).then((res) => {
    return res as unknown as Blob
  })
}

export function importFormConfig(dto: LowcodeFormConfigDTO) {
  return http.post<number>(`${FORM_BASE}/import`, dto)
}

export function instantiateFormFromTemplate(templateId: number, dto: LowcodeInstantiateDTO) {
  return http.post<number>(`${FORM_BASE}/templates/${templateId}/instantiate`, dto)
}

/* ============ 列表配置 ============ */

const LIST_BASE = '/lowcode/lists'

export function pageListConfigs(params: LowcodeConfigQueryParams) {
  return http.get<PageResult<LowcodeListConfigVO>>(LIST_BASE, params as Record<string, unknown>)
}

export function getListConfigDetail(id: number) {
  return http.get<LowcodeListConfigVO>(`${LIST_BASE}/${id}`)
}

export function createListConfig(dto: LowcodeListConfigDTO) {
  return http.post<number>(LIST_BASE, dto)
}

export function updateListConfig(id: number, dto: LowcodeListConfigDTO) {
  return http.put<void>(`${LIST_BASE}/${id}`, dto)
}

export function deleteListConfig(id: number) {
  return http.delete<void>(`${LIST_BASE}/${id}`)
}

export function copyListConfig(id: number) {
  return http.post<number>(`${LIST_BASE}/${id}/copy`)
}

export function exportListConfigJson(id: number): Promise<Blob> {
  return service.get(`${LIST_BASE}/${id}/export`, { responseType: 'blob' }).then((res) => {
    return res as unknown as Blob
  })
}

export function importListConfig(dto: LowcodeListConfigDTO) {
  return http.post<number>(`${LIST_BASE}/import`, dto)
}

export function instantiateListFromTemplate(templateId: number, dto: LowcodeInstantiateDTO) {
  return http.post<number>(`${LIST_BASE}/templates/${templateId}/instantiate`, dto)
}

/* ============ 标签页配置 ============ */

const TAB_BASE = '/lowcode/tabs'

export function pageTabConfigs(params: LowcodeConfigQueryParams) {
  return http.get<PageResult<LowcodeTabConfigVO>>(TAB_BASE, params as Record<string, unknown>)
}

export function getTabConfigDetail(id: number) {
  return http.get<LowcodeTabConfigVO>(`${TAB_BASE}/${id}`)
}

export function createTabConfig(dto: LowcodeTabConfigDTO) {
  return http.post<number>(TAB_BASE, dto)
}

export function updateTabConfig(id: number, dto: LowcodeTabConfigDTO) {
  return http.put<void>(`${TAB_BASE}/${id}`, dto)
}

export function deleteTabConfig(id: number) {
  return http.delete<void>(`${TAB_BASE}/${id}`)
}

export function copyTabConfig(id: number) {
  return http.post<number>(`${TAB_BASE}/${id}/copy`)
}

export function exportTabConfigJson(id: number): Promise<Blob> {
  return service.get(`${TAB_BASE}/${id}/export`, { responseType: 'blob' }).then((res) => {
    return res as unknown as Blob
  })
}

export function importTabConfig(dto: LowcodeTabConfigDTO) {
  return http.post<number>(`${TAB_BASE}/import`, dto)
}

export function instantiateTabFromTemplate(templateId: number, dto: LowcodeInstantiateDTO) {
  return http.post<number>(`${TAB_BASE}/templates/${templateId}/instantiate`, dto)
}

/* ============ 关联页配置 ============ */

const RELATION_BASE = '/lowcode/relations'

export function pageRelationConfigs(params: LowcodeConfigQueryParams) {
  return http.get<PageResult<LowcodeRelationConfigVO>>(RELATION_BASE, params as Record<string, unknown>)
}

export function getRelationConfigDetail(id: number) {
  return http.get<LowcodeRelationConfigVO>(`${RELATION_BASE}/${id}`)
}

export function createRelationConfig(dto: LowcodeRelationConfigDTO) {
  return http.post<number>(RELATION_BASE, dto)
}

export function updateRelationConfig(id: number, dto: LowcodeRelationConfigDTO) {
  return http.put<void>(`${RELATION_BASE}/${id}`, dto)
}

export function deleteRelationConfig(id: number) {
  return http.delete<void>(`${RELATION_BASE}/${id}`)
}

export function copyRelationConfig(id: number) {
  return http.post<number>(`${RELATION_BASE}/${id}/copy`)
}

export function exportRelationConfigJson(id: number): Promise<Blob> {
  return service.get(`${RELATION_BASE}/${id}/export`, { responseType: 'blob' }).then((res) => {
    return res as unknown as Blob
  })
}

export function importRelationConfig(dto: LowcodeRelationConfigDTO) {
  return http.post<number>(`${RELATION_BASE}/import`, dto)
}

export function instantiateRelationFromTemplate(templateId: number, dto: LowcodeInstantiateDTO) {
  return http.post<number>(`${RELATION_BASE}/templates/${templateId}/instantiate`, dto)
}

/* ============ 模板管理 ============ */

const TEMPLATE_BASE = '/lowcode/templates'

export function pageTemplates(params: LowcodeTemplateQueryParams) {
  return http.get<PageResult<LowcodeTemplateVO>>(TEMPLATE_BASE, params as Record<string, unknown>)
}

export function getTemplateDetail(id: number) {
  return http.get<LowcodeTemplateVO>(`${TEMPLATE_BASE}/${id}`)
}

export function createTemplate(dto: LowcodeTemplateDTO) {
  return http.post<number>(TEMPLATE_BASE, dto)
}

export function updateTemplate(id: number, dto: LowcodeTemplateDTO) {
  return http.put<void>(`${TEMPLATE_BASE}/${id}`, dto)
}

export function deleteTemplate(id: number) {
  return http.delete<void>(`${TEMPLATE_BASE}/${id}`)
}

export function copyTemplate(id: number) {
  return http.post<number>(`${TEMPLATE_BASE}/${id}/copy`)
}

export function exportTemplateJson(id: number): Promise<Blob> {
  return service.get(`${TEMPLATE_BASE}/${id}/export`, { responseType: 'blob' }).then((res) => {
    return res as unknown as Blob
  })
}

export function importTemplate(dto: LowcodeTemplateDTO) {
  return http.post<number>(`${TEMPLATE_BASE}/import`, dto)
}

/**
 * 校验模板可实例化（返回模板 ID，可视为模板有效）。
 *
 * 后端 TemplateController 的 POST /templates/{templateId}/instantiate 是同步实例化接口，
 * 但模板本身不依赖具体配置类型，前端调用此接口做"试实例化"。
 */
export function instantiateTemplate(templateId: number, dto: LowcodeInstantiateDTO) {
  return http.post<number>(`${TEMPLATE_BASE}/${templateId}/instantiate`, dto)
}

/* ============ 通用工具：触发浏览器下载 ============ */

/** 将 Blob 触发下载 */
export function downloadBlob(blob: Blob, filename: string): void {
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}
