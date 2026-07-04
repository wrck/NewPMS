/**
 * 用户 Store（Pinia）
 * 管理 token / roles / userInfo，提供 login / logout / getUserInfo actions
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginParams, LoginResult, RoleCode, UserInfo } from '@/types/user'
import * as authApi from '@/api/auth'

const TOKEN_KEY = import.meta.env.VITE_TOKEN_KEY || 'vibe_token'

export const useUserStore = defineStore('user', () => {
  // ============ state ============
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const refreshToken = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)

  // ============ getters ============
  const roles = computed<RoleCode[]>(() => userInfo.value?.roles || [])
  const isLogin = computed<boolean>(() => !!token.value)
  const realName = computed<string>(() => userInfo.value?.realName || '游客')
  const avatar = computed<string>(() => userInfo.value?.avatar || '')
  const tenantType = computed(() => userInfo.value?.tenantType || 'INTERNAL')
  /** 用户名（兼容 username 别名，部分组件直接使用 userStore.username） */
  const username = computed<string>(() => userInfo.value?.userName || '')
  /** 当前用户的权限标识列表（来自后端 /auth/me 的 permissions 字段） */
  const permissions = computed<string[]>(() => userInfo.value?.permissions || [])
  /** 是否为超级管理员（直接放行所有权限校验） */
  const isAdmin = computed<boolean>(() => hasRole('SUPER_ADMIN'))

  /** 是否拥有某个角色 */
  function hasRole(role: RoleCode | RoleCode[]): boolean {
    if (!roles.value.length) return false
    if (Array.isArray(role)) return role.some((r) => roles.value.includes(r))
    return roles.value.includes(role)
  }

  /** 是否拥有任一权限角色（用于菜单/路由可见性） */
  function hasAnyRole(roleList: RoleCode[] = []): boolean {
    if (!roleList || roleList.length === 0) return true
    return hasRole(roleList)
  }

  /**
   * 是否拥有某个权限标识（用于菜单/按钮级别权限控制）
   *
   * 校验策略：
   * 1. 超级管理员直接返回 true
   * 2. 否则检查 userInfo.permissions 是否包含该权限
   * 3. 兜底：基于角色-权限映射表（前端的 MVP 策略，避免依赖后端权限接口）
   *
   * @param permission 权限标识，如 'system:view' / 'project:view'
   */
  function hasPermission(permission: string): boolean {
    if (!permission) return true
    // 超级管理员直接放行
    if (isAdmin.value) return true
    // 后端返回的权限列表命中
    if (permissions.value.includes(permission)) return true
    // 兜底：基于角色的权限映射（MVP 阶段，避免非超管用户菜单全部不可见）
    return matchPermissionByRole(permission)
  }

  /**
   * 角色 -> 权限的兜底映射。
   *
   * 后端如果未通过 /auth/me 返回 permissions 字段，则基于角色做粗粒度匹配：
   * - DIRECTOR: 全部业务模块可见
   * - PM: project / resource / delivery / acceptance / report
   * - ENGINEER: device / resource / delivery
   * - FINANCE: finance / report
   * - AGENT_ADMIN / AGENT_ENGINEER: agent
   * - CUSTOMER: report (只读)
   */
  function matchPermissionByRole(permission: string): boolean {
    const module = permission.split(':')[0]
    const roleModules: Record<string, string[]> = {
      DIRECTOR: ['project', 'device', 'resource', 'delivery', 'agent', 'acceptance', 'finance', 'report', 'system'],
      PM: ['project', 'resource', 'delivery', 'acceptance', 'report'],
      ENGINEER: ['device', 'resource', 'delivery'],
      FINANCE: ['finance', 'report'],
      AGENT_ADMIN: ['agent'],
      AGENT_ENGINEER: ['agent'],
      CUSTOMER: ['report']
    }
    for (const role of roles.value) {
      const allowed = roleModules[role] || []
      if (allowed.includes(module)) return true
    }
    return false
  }

  // ============ actions ============
  function setToken(value: string, rt?: string) {
    token.value = value
    if (rt) refreshToken.value = rt
    localStorage.setItem(TOKEN_KEY, value)
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
  }

  /** 登录 */
  async function login(params: LoginParams): Promise<LoginResult> {
    const result = await authApi.login({
      ...params,
      clientId: params.clientId || 'PC'
    })
    setToken(result.token, result.refreshToken)
    setUserInfo(result.userInfo)
    return result
  }

  /** 退出登录 */
  async function logout(): Promise<void> {
    try {
      await authApi.logout()
    } catch (e) {
      // 即使后端调用失败，本地也要清空
      console.warn('[user] logout api failed:', e)
    } finally {
      reset()
    }
  }

  /** 拉取当前用户信息 */
  async function fetchUserInfo(): Promise<UserInfo> {
    const info = await authApi.getUserInfo()
    setUserInfo(info)
    return info
  }

  /** 清空本地登录态 */
  function reset() {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  return {
    // state
    token,
    refreshToken,
    userInfo,
    // getters
    roles,
    isLogin,
    realName,
    avatar,
    tenantType,
    username,
    permissions,
    isAdmin,
    // actions
    hasRole,
    hasAnyRole,
    hasPermission,
    setToken,
    setUserInfo,
    login,
    logout,
    fetchUserInfo,
    reset
  }
})
