/**
 * 系统管理模块 API 封装
 * 对应后端：/api/v1/users、/api/v1/roles、/api/v1/orgs、/api/v1/dicts、/api/v1/configs、/api/v1/logs
 */
import { http } from '@/utils/request'
import type { PageResult, PageParams } from '@/types/api'
import type { RoleCode } from '@/types/user'

/* ============ 用户管理 ============ */

export interface SysUser {
  id: number
  userName: string
  realName: string
  avatar?: string
  email?: string
  phone?: string
  orgId?: number
  orgName?: string
  status: 1 | 0
  roles: RoleCode[]
  roleNames?: string[]
  createdAt?: string
  lastLoginAt?: string
}

export interface SysUserDTO {
  id?: number
  userName: string
  realName: string
  email?: string
  phone?: string
  orgId?: number
  status?: 1 | 0
  roleCodes?: RoleCode[]
  password?: string
}

export interface SysUserQueryParams extends PageParams {
  userName?: string
  realName?: string
  phone?: string
  orgId?: number
  status?: 1 | 0
  roleCode?: RoleCode
}

const USER_BASE = '/users'

export function pageUsers(params: SysUserQueryParams) {
  return http.get<PageResult<SysUser>>(USER_BASE, params as Record<string, unknown>)
}

export function getUserDetail(id: number) {
  return http.get<SysUser>(`${USER_BASE}/${id}`)
}

export function createUser(dto: SysUserDTO) {
  return http.post<number>(USER_BASE, dto)
}

export function updateUser(id: number, dto: SysUserDTO) {
  return http.put<void>(`${USER_BASE}/${id}`, dto)
}

export function deleteUser(id: number) {
  return http.delete<void>(`${USER_BASE}/${id}`)
}

export function assignUserRoles(id: number, roleCodes: RoleCode[]) {
  return http.put<void>(`${USER_BASE}/${id}/roles`, { roleCodes })
}

export function getUserRoles(id: number) {
  return http.get<Array<{ id: number; roleCode: RoleCode; roleName: string }>>(`${USER_BASE}/${id}/roles`)
}

export function changeUserStatus(id: number, status: 1 | 0) {
  return http.put<void>(`${USER_BASE}/${id}/status`, { status })
}

export function resetUserPassword(id: number, newPassword: string) {
  return http.put<void>(`${USER_BASE}/${id}/password`, { newPassword })
}

/* ============ 角色权限 ============ */

export interface SysRole {
  id: number
  roleCode: RoleCode
  roleName: string
  description?: string
  dataScope: 'ALL' | 'DEPT' | 'SELF' | 'CUSTOM'
  status: 1 | 0
  permissions?: string[]
  permissionsTree?: any[]
  userCount?: number
  createdAt?: string
}

export interface SysRoleDTO {
  id?: number
  roleCode?: RoleCode
  roleName: string
  description?: string
  dataScope?: SysRole['dataScope']
  status?: 1 | 0
  permissionCodes?: string[]
  customOrgIds?: number[]
}

const ROLE_BASE = '/roles'

export function listRoles(params?: { roleName?: string; status?: 1 | 0 }) {
  return http.get<SysRole[]>(ROLE_BASE, params as Record<string, unknown>)
}

export function getRoleDetail(id: number) {
  return http.get<SysRole>(`${ROLE_BASE}/${id}`)
}

export function createRole(dto: SysRoleDTO) {
  return http.post<number>(ROLE_BASE, dto)
}

export function updateRole(id: number, dto: SysRoleDTO) {
  return http.put<void>(`${ROLE_BASE}/${id}`, dto)
}

export function deleteRole(id: number) {
  return http.delete<void>(`${ROLE_BASE}/${id}`)
}

/** 获取角色的菜单权限树 */
export function getRolePermissions(id: number) {
  return http.get<{ permissionCodes: string[]; permissionTree: any[] }>(`${ROLE_BASE}/${id}/permissions`)
}

/** 分配角色权限 */
export function assignRolePermissions(id: number, permissionCodes: string[]) {
  return http.put<void>(`${ROLE_BASE}/${id}/permissions`, { permissionCodes })
}

/* ============ 组织架构 ============ */

export interface SysOrg {
  id: number
  orgCode: string
  orgName: string
  parentId?: number
  parentName?: string
  sort: number
  leaderId?: number
  leaderName?: string
  phone?: string
  status: 1 | 0
  children?: SysOrg[]
}

const ORG_BASE = '/orgs'

export function listOrgTree(params?: { status?: 1 | 0 }) {
  return http.get<SysOrg[]>(`${ORG_BASE}/tree`, params as Record<string, unknown>)
}

export function createOrg(dto: Partial<SysOrg>) {
  return http.post<number>(ORG_BASE, dto)
}

export function updateOrg(id: number, dto: Partial<SysOrg>) {
  return http.put<void>(`${ORG_BASE}/${id}`, dto)
}

export function deleteOrg(id: number) {
  return http.delete<void>(`${ORG_BASE}/${id}`)
}

/* ============ 数据字典 ============ */

export interface SysDictType {
  id: number
  dictCode: string
  dictName: string
  description?: string
  status: 1 | 0
  createdAt?: string
}

export interface SysDictData {
  id: number
  dictTypeId: number
  dictTypeCode?: string
  dictLabel: string
  dictValue: string
  sort: number
  isDefault?: 0 | 1
  status: 1 | 0
  remark?: string
}

const DICT_TYPE_BASE = '/dicts/types'
const DICT_DATA_BASE = '/dicts/data'

export function pageDictTypes(params: PageParams) {
  return http.get<PageResult<SysDictType>>(DICT_TYPE_BASE, params as Record<string, unknown>)
}

export function createDictType(dto: Partial<SysDictType>) {
  return http.post<number>(DICT_TYPE_BASE, dto)
}

export function updateDictType(id: number, dto: Partial<SysDictType>) {
  return http.put<void>(`${DICT_TYPE_BASE}/${id}`, dto)
}

export function deleteDictType(id: number) {
  return http.delete<void>(`${DICT_TYPE_BASE}/${id}`)
}

export function pageDictData(params: PageParams & { dictTypeId?: number; dictTypeCode?: string }) {
  return http.get<PageResult<SysDictData>>(DICT_DATA_BASE, params as Record<string, unknown>)
}

export function createDictData(dto: Partial<SysDictData>) {
  return http.post<number>(DICT_DATA_BASE, dto)
}

export function updateDictData(id: number, dto: Partial<SysDictData>) {
  return http.put<void>(`${DICT_DATA_BASE}/${id}`, dto)
}

export function deleteDictData(id: number) {
  return http.delete<void>(`${DICT_DATA_BASE}/${id}`)
}

/** 按字典编码拉取选项（前端缓存使用） */
export function listDictByCode(dictCode: string) {
  return http.get<SysDictData[]>(`${DICT_DATA_BASE}/code/${dictCode}`)
}

/* ============ 系统配置 ============ */

export interface SysConfig {
  id: number
  configKey: string
  configValue: string
  configName: string
  configType?: string
  description?: string
  status: 1 | 0
  updatedAt?: string
}

const CONFIG_BASE = '/configs'

export function pageConfigs(params: PageParams & { configKey?: string; configName?: string }) {
  return http.get<PageResult<SysConfig>>(CONFIG_BASE, params as Record<string, unknown>)
}

export function createConfig(dto: Partial<SysConfig>) {
  return http.post<number>(CONFIG_BASE, dto)
}

export function updateConfig(id: number, dto: Partial<SysConfig>) {
  return http.put<void>(`${CONFIG_BASE}/${id}`, dto)
}

export function deleteConfig(id: number) {
  return http.delete<void>(`${CONFIG_BASE}/${id}`)
}

/** 按 key 拉取配置 */
export function getConfigByKey(key: string) {
  return http.get<SysConfig>(`${CONFIG_BASE}/key/${key}`)
}

/* ============ 操作日志 ============ */

export interface SysLog {
  id: number
  title: string
  module: string
  type: 'INSERT' | 'UPDATE' | 'DELETE' | 'QUERY' | 'EXPORT' | 'OTHER'
  method: string
  requestUrl?: string
  requestMethod?: string
  requestParams?: string
  responseResult?: string
  operatorId?: number
  operatorName?: string
  operatorIp?: string
  status: 1 | 0
  errorMsg?: string
  costMs?: number
  operatedAt: string
}

export interface SysLogQueryParams extends PageParams {
  title?: string
  module?: string
  type?: SysLog['type']
  operatorId?: number
  status?: 1 | 0
  startBegin?: string
  startEnd?: string
}

const LOG_BASE = '/logs'

export function pageLogs(params: SysLogQueryParams) {
  return http.get<PageResult<SysLog>>(LOG_BASE, params as Record<string, unknown>)
}

export function getLogDetail(id: number) {
  return http.get<SysLog>(`${LOG_BASE}/${id}`)
}

/** 登录日志 */
export function pageLoginLogs(params: PageParams & { userName?: string; status?: 1 | 0; startBegin?: string; startEnd?: string }) {
  return http.get<PageResult<SysLog>>(`${LOG_BASE}/login`, params as Record<string, unknown>)
}
