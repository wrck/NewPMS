/**
 * 设备资产管理模块 API 封装
 * 对应后端：/api/v1/devices/{models|instances|boms|inventory|spare-parts|warehouses|dashboard}
 *           /api/v1/devices/inventory/ledger、/api/v1/devices/inventory/warnings
 */
import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  DeviceModel,
  DeviceInstance,
  DeviceBom,
  Warehouse,
  InventoryLedger,
  InventoryTransaction,
  SparePart,
  DeviceDashboard,
  DeviceModelQueryParams,
  DeviceInstanceQueryParams,
  DeviceBomQueryParams,
  InventoryQueryParams,
  SparePartQueryParams,
  DeviceInstanceDTO,
  DeviceModelDTO,
  InventoryTransactionDTO
} from '@/types/device'

const BASE = '/devices'

/* ============ 设备型号库 ============ */

export function pageDeviceModels(params: DeviceModelQueryParams) {
  return http.get<PageResult<DeviceModel>>(`${BASE}/models`, params as Record<string, unknown>)
}

export function getDeviceModelDetail(id: number) {
  return http.get<DeviceModel>(`${BASE}/models/${id}`)
}

export function createDeviceModel(dto: DeviceModelDTO) {
  return http.post<number>(`${BASE}/models`, dto)
}

export function updateDeviceModel(id: number, dto: DeviceModelDTO) {
  return http.put<void>(`${BASE}/models/${id}`, dto)
}

export function deleteDeviceModel(id: number) {
  return http.delete<void>(`${BASE}/models/${id}`)
}

/* ============ 设备实例（台账） ============ */

export function pageDeviceInstances(params: DeviceInstanceQueryParams) {
  return http.get<PageResult<DeviceInstance>>(`${BASE}/instances`, params as Record<string, unknown>)
}

export function getDeviceInstanceDetail(id: number) {
  return http.get<DeviceInstance>(`${BASE}/instances/${id}`)
}

export function createDeviceInstance(dto: DeviceInstanceDTO) {
  return http.post<number>(`${BASE}/instances`, dto)
}

export function updateDeviceInstance(id: number, dto: DeviceInstanceDTO) {
  return http.put<void>(`${BASE}/instances/${id}`, dto)
}

export function deleteDeviceInstance(id: number) {
  return http.delete<void>(`${BASE}/instances/${id}`)
}

/** 设备状态流转 */
export function transitionDeviceStatus(id: number, targetStatus: string, remark?: string) {
  return http.put<void>(`${BASE}/instances/${id}/status`, { targetStatus, remark })
}

/** Excel 批量导入设备 */
export function importDeviceInstances(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<{ successCount: number; failCount: number; errors?: string[] }>(
    `${BASE}/instances/import`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  )
}

/** 下载导入模板 */
export function downloadImportTemplate() {
  return http.get<Blob>(`${BASE}/instances/import-template`, undefined, {
    responseType: 'blob'
  })
}

/* ============ 设备 BOM ============ */

export function pageDeviceBoms(params: DeviceBomQueryParams) {
  return http.get<PageResult<DeviceBom>>(`${BASE}/boms`, params as Record<string, unknown>)
}

export function createDeviceBom(dto: Partial<DeviceBom>) {
  return http.post<number>(`${BASE}/boms`, dto)
}

export function updateDeviceBom(id: number, dto: Partial<DeviceBom>) {
  return http.put<void>(`${BASE}/boms/${id}`, dto)
}

export function deleteDeviceBom(id: number) {
  return http.delete<void>(`${BASE}/boms/${id}`)
}

/** BOM 确认 */
export function confirmDeviceBom(id: number) {
  return http.put<void>(`${BASE}/boms/${id}/confirm`)
}

/* ============ 仓库 ============ */

export function listWarehouses(params?: { region?: string; status?: string }) {
  return http.get<Warehouse[]>(`${BASE}/warehouses`, params as Record<string, unknown>)
}

export function createWarehouse(dto: Partial<Warehouse>) {
  return http.post<number>(`${BASE}/warehouses`, dto)
}

export function updateWarehouse(id: number, dto: Partial<Warehouse>) {
  return http.put<void>(`${BASE}/warehouses/${id}`, dto)
}

export function deleteWarehouse(id: number) {
  return http.delete<void>(`${BASE}/warehouses/${id}`)
}

/* ============ 库存台账 ============ */

export function pageInventory(params: InventoryQueryParams) {
  return http.get<PageResult<InventoryLedger>>(`${BASE}/inventory`, params as Record<string, unknown>)
}

export function pageInventoryLedger(params: InventoryQueryParams) {
  return http.get<PageResult<InventoryLedger>>(`${BASE}/inventory/ledger`, params as Record<string, unknown>)
}

/** 出入库流水 */
export function pageInventoryTransactions(params: InventoryQueryParams & { type?: string }) {
  return http.get<PageResult<InventoryTransaction>>(`${BASE}/inventory/transactions`, params as Record<string, unknown>)
}

/** 新增出入库记录 */
export function createInventoryTransaction(dto: InventoryTransactionDTO) {
  return http.post<number>(`${BASE}/inventory/transactions`, dto)
}

/** 库存预警列表 */
export function listInventoryWarnings() {
  return http.get<InventoryLedger[]>(`${BASE}/inventory/warnings`)
}

/* ============ 备件管理 ============ */

export function pageSpareParts(params: SparePartQueryParams) {
  return http.get<PageResult<SparePart>>(`${BASE}/spare-parts`, params as Record<string, unknown>)
}

export function createSparePart(dto: Partial<SparePart>) {
  return http.post<number>(`${BASE}/spare-parts`, dto)
}

export function updateSparePart(id: number, dto: Partial<SparePart>) {
  return http.put<void>(`${BASE}/spare-parts/${id}`, dto)
}

export function deleteSparePart(id: number) {
  return http.delete<void>(`${BASE}/spare-parts/${id}`)
}

/** 备件领用/归还 */
export function sparePartAction(id: number, action: 'OUT' | 'RETURN' | 'REPAIR', quantity: number, remark?: string) {
  return http.post<void>(`${BASE}/spare-parts/${id}/${action}`, { quantity, remark })
}

/* ============ 设备看板 ============ */

export function getDeviceDashboard() {
  return http.get<DeviceDashboard>(`${BASE}/dashboard`)
}
