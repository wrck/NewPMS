/**
 * 设备资产管理模块类型定义
 * 对应后端：/api/v1/devices/{models|instances|boms|inventory|spare-parts|warehouses|dashboard}
 */
import type { PageParams } from './api'
import type { DeviceStatus } from './enum'

/** 设备类别 */
export type DeviceCategory = 'ROUTER' | 'SWITCH' | 'AP' | 'FIREWALL' | 'WLC' | 'LB' | 'OTHER'

/** 产品线（与项目一致） */
export type ProductLine = 'ROUTER' | 'SWITCH' | 'WIRELESS' | 'SECURITY' | 'DC' | 'OTHER'

/** 设备型号 */
export interface DeviceModel {
  id: number
  modelCode: string
  modelName: string
  productLine: ProductLine
  vendor?: string
  category: DeviceCategory
  specifications?: Record<string, string>
  configTemplate?: string
  manualUrl?: string
  imageUrl?: string
  description?: string
  status?: 'ENABLED' | 'DISABLED'
  createdAt?: string
}

/** 设备实例 */
export interface DeviceInstance {
  id: number
  serialNumber: string
  macAddress?: string
  modelId: number
  modelName?: string
  productLine?: ProductLine
  category?: DeviceCategory
  firmwareVersion?: string
  projectId?: number
  projectName?: string
  warehouseId?: number
  warehouseName?: string
  location?: string
  status: DeviceStatus
  installedAt?: string
  onlineAt?: string
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/** 设备 BOM（项目设备清单） */
export interface DeviceBom {
  id: number
  projectId: number
  projectName?: string
  modelId: number
  modelName?: string
  productLine?: ProductLine
  category?: DeviceCategory
  quantity: number
  arrivedQty: number
  installedQty: number
  acceptedQty: number
  unit?: string
  remark?: string
  status?: 'PENDING' | 'CONFIRMED' | 'CHANGED'
}

/** 仓库 */
export interface Warehouse {
  id: number
  warehouseCode: string
  warehouseName: string
  address?: string
  managerId?: number
  managerName?: string
  phone?: string
  region?: string
  /** 安全库存配置（JSON 字符串，按型号键值对，如 {"1001":5,"1002":3}） */
  safetyStock?: string
  status?: 'ENABLED' | 'DISABLED'
  createTime?: string
}

/** 仓库查询参数 */
export interface WarehouseQueryParams extends PageParams {
  keyword?: string
  region?: string
}

/** 仓库新增/编辑 DTO */
export interface WarehouseDTO {
  id?: number
  warehouseName: string
  warehouseCode: string
  address?: string
  region?: string
  managerId?: number
  safetyStock?: string
}

/** 库存台账 */
export interface InventoryLedger {
  id: number
  warehouseId: number
  warehouseName?: string
  modelId: number
  modelName?: string
  productLine?: ProductLine
  category?: DeviceCategory
  stockQty: number
  safetyStockQty: number
  lockedQty?: number
  unit?: string
  warning?: boolean
}

/** 出入库流水 */
export interface InventoryTransaction {
  id: number
  transactionNo: string
  type: 'IN' | 'OUT' | 'RETURN' | 'TRANSFER'
  warehouseId: number
  warehouseName?: string
  toWarehouseId?: number
  toWarehouseName?: string
  modelId: number
  modelName?: string
  quantity: number
  projectId?: number
  projectName?: string
  operatorId?: number
  operatorName?: string
  remark?: string
  createdAt: string
}

/** 备件 */
export interface SparePart {
  id: number
  partCode: string
  partName: string
  modelId?: number
  modelName?: string
  category?: DeviceCategory
  unit?: string
  stockQty: number
  safetyStockQty: number
  warehouseId?: number
  warehouseName?: string
  status?: 'IN_STOCK' | 'OUT' | 'REPAIR' | 'SCRAPPED'
  remark?: string
}

/** 备件流水（领用/归还/返修/入库） */
export interface SparePartLog {
  id: number
  sparePartId: number
  partName?: string
  partCode?: string
  /** 操作类型 IN/OUT/RETURN/REPAIR */
  actionType: 'IN' | 'OUT' | 'RETURN' | 'REPAIR'
  quantity: number
  projectId?: number
  projectName?: string
  operatorId?: number
  operatorName?: string
  remark?: string
  createTime?: string
}

/** 备件操作 DTO（与后端 SparePartActionDTO 对齐） */
export interface SparePartActionDTO {
  sparePartId: number
  actionType: 'IN' | 'OUT' | 'RETURN' | 'REPAIR'
  quantity: number
  projectId?: number
  remark?: string
}

/** 设备看板统计 */
export interface DeviceDashboard {
  totalDevices: number
  statusDistribution: Array<{ status: DeviceStatus; count: number }>
  productLineDistribution: Array<{ productLine: ProductLine; count: number }>
  abnormalDevices: DeviceInstance[]
  bomCompletionRate: Array<{ projectId: number; projectName: string; rate: number }>
  warehouseStockStats: Array<{ warehouseId: number; warehouseName: string; totalQty: number; warningQty: number }>
}

/** 设备型号查询参数 */
export interface DeviceModelQueryParams extends PageParams {
  productLine?: ProductLine
  category?: DeviceCategory
  vendor?: string
  status?: 'ENABLED' | 'DISABLED'
}

/** 设备实例查询参数 */
export interface DeviceInstanceQueryParams extends PageParams {
  modelId?: number
  status?: DeviceStatus
  projectId?: number
  warehouseId?: number
  productLine?: ProductLine
  category?: DeviceCategory
  serialNumber?: string
}

/** BOM 查询参数 */
export interface DeviceBomQueryParams extends PageParams {
  projectId?: number
  modelId?: number
  status?: 'PENDING' | 'CONFIRMED' | 'CHANGED'
}

/** 库存查询参数 */
export interface InventoryQueryParams extends PageParams {
  warehouseId?: number
  modelId?: number
  productLine?: ProductLine
  warningOnly?: boolean
}

/** 备件查询参数 */
export interface SparePartQueryParams extends PageParams {
  partName?: string
  category?: DeviceCategory
  warehouseId?: number
  status?: SparePart['status']
}

/** 设备实例创建/编辑 DTO */
export interface DeviceInstanceDTO {
  id?: number
  serialNumber: string
  macAddress?: string
  modelId: number
  firmwareVersion?: string
  projectId?: number
  warehouseId?: number
  location?: string
  status?: DeviceStatus
  remark?: string
}

/** 设备型号创建/编辑 DTO */
export interface DeviceModelDTO {
  id?: number
  modelCode: string
  modelName: string
  productLine: ProductLine
  vendor?: string
  category: DeviceCategory
  specifications?: Record<string, string>
  configTemplate?: string
  manualUrl?: string
  imageUrl?: string
  description?: string
}

/** 出入库 DTO */
export interface InventoryTransactionDTO {
  type: 'IN' | 'OUT' | 'RETURN' | 'TRANSFER'
  warehouseId: number
  toWarehouseId?: number
  modelId: number
  quantity: number
  projectId?: number
  remark?: string
}
