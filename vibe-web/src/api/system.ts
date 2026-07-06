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

/* ============ 菜单管理 ============ */

/** 菜单类型：DIRECTORY-目录 / MENU-菜单 / BUTTON-按钮 */
export type MenuType = 'DIRECTORY' | 'MENU' | 'BUTTON'

export interface SysMenu {
  id: number
  parentId: number
  menuName: string
  menuType: MenuType
  path?: string
  component?: string
  perms?: string
  icon?: string
  sortOrder?: number
  visible?: 1 | 0
  createTime?: string
  children?: SysMenu[]
}

export interface SysMenuDTO {
  id?: number
  parentId?: number
  menuName: string
  menuType: MenuType
  path?: string
  component?: string
  perms?: string
  icon?: string
  sortOrder?: number
  visible?: 1 | 0
}

export interface MenuRoleSimple {
  id: number
  roleName: string
  roleCode: string
  dataScope?: string
}

const MENU_BASE = '/menus'

/** 查询菜单树（全部） */
export function listMenuTree() {
  return http.get<SysMenu[]>(`${MENU_BASE}/tree`)
}

/** 查询菜单扁平列表 */
export function listAllMenus() {
  return http.get<SysMenu[]>(MENU_BASE)
}

/** 菜单详情 */
export function getMenu(id: number) {
  return http.get<SysMenu>(`${MENU_BASE}/${id}`)
}

/** 新增菜单 */
export function createMenu(dto: SysMenuDTO) {
  return http.post<number>(MENU_BASE, dto)
}

/** 编辑菜单 */
export function updateMenu(id: number, dto: SysMenuDTO) {
  return http.put<void>(`${MENU_BASE}/${id}`, dto)
}

/** 删除菜单 */
export function deleteMenu(id: number) {
  return http.delete<void>(`${MENU_BASE}/${id}`)
}

/** 查询菜单关联的角色列表 */
export function listRolesByMenu(menuId: number) {
  return http.get<MenuRoleSimple[]>(`${MENU_BASE}/${menuId}/roles`)
}

/** 给菜单分配角色（全量覆盖） */
export function assignRolesToMenu(menuId: number, roleIds: number[]) {
  return http.put<void>(`${MENU_BASE}/${menuId}/roles`, { roleIds })
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
export interface SysLoginLog {
  id: number
  username: string
  loginTime: string
  loginIp?: string
  loginLocation?: string
  browser?: string
  os?: string
  status: 1 | 0
  msg?: string
}

export interface SysLoginLogQueryParams extends PageParams {
  userName?: string
  status?: 1 | 0
  startBegin?: string
  startEnd?: string
}

export function pageLoginLogs(params: SysLoginLogQueryParams) {
  return http.get<PageResult<SysLoginLog>>(`${LOG_BASE}/login`, params as Record<string, unknown>)
}

/* ============ 岗位管理 ============ */

export interface SysPosition {
  id: number
  orgId?: number
  orgName?: string
  positionName: string
  positionCode: string
  sortOrder?: number
  status: 1 | 0
  createTime?: string
}

export interface SysPositionDTO {
  id?: number
  orgId?: number
  positionName: string
  positionCode: string
  sortOrder?: number
  status?: 1 | 0
}

export interface SysPositionQueryParams extends PageParams {
  keyword?: string
  orgId?: number
}

const POSITION_BASE = '/positions'

export function pagePositions(params: SysPositionQueryParams) {
  return http.get<PageResult<SysPosition>>(POSITION_BASE, params as Record<string, unknown>)
}

export function getPositionDetail(id: number) {
  return http.get<SysPosition>(`${POSITION_BASE}/${id}`)
}

export function createPosition(dto: SysPositionDTO) {
  return http.post<number>(POSITION_BASE, dto)
}

export function updatePosition(id: number, dto: SysPositionDTO) {
  return http.put<void>(`${POSITION_BASE}/${id}`, dto)
}

export function deletePosition(id: number) {
  return http.delete<void>(`${POSITION_BASE}/${id}`)
}

/* ============ 站内信 ============ */

export interface SysNotice {
  id: number
  noticeTitle: string
  noticeType: 1 | 2
  noticeContent?: string
  recipientId?: number
  readStatus: 0 | 1
  sendTime?: string
}

export interface SysNoticeDTO {
  noticeTitle: string
  noticeType?: 1 | 2
  noticeContent?: string
  recipientId: number
}

export interface SysNoticeQueryParams extends PageParams {
  noticeType?: 1 | 2
  readStatus?: 0 | 1
  keyword?: string
}

const NOTICE_BASE = '/notices'

export function pageMyNotices(params: SysNoticeQueryParams) {
  return http.get<PageResult<SysNotice>>(NOTICE_BASE, params as Record<string, unknown>)
}

export function getUnreadCount() {
  return http.get<number>(`${NOTICE_BASE}/unread-count`)
}

export function sendNotice(dto: SysNoticeDTO) {
  return http.post<number>(NOTICE_BASE, dto)
}

export function markNoticeRead(id: number) {
  return http.put<void>(`${NOTICE_BASE}/${id}/read`)
}

export function markAllNoticesRead() {
  return http.put<void>(`${NOTICE_BASE}/read-all`)
}

export function deleteNotice(id: number) {
  return http.delete<void>(`${NOTICE_BASE}/${id}`)
}

/* ============ 通知模板 ============ */

export interface SysNoticeTemplate {
  id: number
  templateCode: string
  templateName: string
  titleTemplate?: string
  contentTemplate: string
  channels?: string
  recipientType?: string
  status: 1 | 0
  createTime?: string
}

export interface SysNoticeTemplateDTO {
  id?: number
  templateCode: string
  templateName: string
  titleTemplate?: string
  contentTemplate: string
  channels?: string
  recipientType?: string
  status?: 1 | 0
}

export interface SysNoticeTemplateQueryParams extends PageParams {
  keyword?: string
  recipientType?: string
  status?: 1 | 0
}

const NOTICE_TEMPLATE_BASE = '/notice-templates'

export function pageNoticeTemplates(params: SysNoticeTemplateQueryParams) {
  return http.get<PageResult<SysNoticeTemplate>>(NOTICE_TEMPLATE_BASE, params as Record<string, unknown>)
}

export function getNoticeTemplateDetail(id: number) {
  return http.get<SysNoticeTemplate>(`${NOTICE_TEMPLATE_BASE}/${id}`)
}

export function createNoticeTemplate(dto: SysNoticeTemplateDTO) {
  return http.post<number>(NOTICE_TEMPLATE_BASE, dto)
}

export function updateNoticeTemplate(id: number, dto: SysNoticeTemplateDTO) {
  return http.put<void>(`${NOTICE_TEMPLATE_BASE}/${id}`, dto)
}

export function deleteNoticeTemplate(id: number) {
  return http.delete<void>(`${NOTICE_TEMPLATE_BASE}/${id}`)
}

export function getNoticeTemplateByCode(code: string) {
  return http.get<SysNoticeTemplate>(`${NOTICE_TEMPLATE_BASE}/by-code/${code}`)
}
