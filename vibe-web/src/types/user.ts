/**
 * 用户相关类型定义
 */

/** 租户类型 */
export type TenantType = 'INTERNAL' | 'AGENT' | 'CUSTOMER'

/** 系统角色（与后端 RBAC 角色编码对齐） */
export type RoleCode =
  | 'SUPER_ADMIN'
  | 'DIRECTOR'
  | 'PM'
  | 'ENGINEER'
  | 'AGENT_ADMIN'
  | 'AGENT_ENGINEER'
  | 'FINANCE'
  | 'CUSTOMER'

/** 用户信息 */
export interface UserInfo {
  userId: number
  userName: string
  realName: string
  avatar?: string
  email?: string
  phone?: string
  roles: RoleCode[]
  tenantType: TenantType
  tenantId?: number | null
  orgId?: number
  orgName?: string
  /** 权限标识列表（可选，由后端 /auth/me 返回；前端做兜底校验） */
  permissions?: string[]
}

/** 登录请求参数 */
export interface LoginParams {
  username: string
  password: string
  clientId?: 'PC' | 'MOBILE'
  captcha?: string
  captchaKey?: string
}

/** 登录响应 */
export interface LoginResult {
  token: string
  refreshToken?: string
  expiresIn: number
  userInfo: UserInfo
}
