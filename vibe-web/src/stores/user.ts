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
    // actions
    hasRole,
    hasAnyRole,
    setToken,
    setUserInfo,
    login,
    logout,
    fetchUserInfo,
    reset
  }
})
