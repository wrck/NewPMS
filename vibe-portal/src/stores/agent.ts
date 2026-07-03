/**
 * 代理商状态管理
 * - 代理商 JWT（clientId=PORTAL_AGENT）
 * - 代理商公司信息缓存
 * - 对接 module-auth AuthController
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { storage, StorageKeys } from '@/utils/storage'
import { login as apiLogin, logout as apiLogout, getUserInfo as apiGetUserInfo } from '@/api/auth'
import type { AgentInfo, LoginResult } from '@/types/api'

/** 代理商登录入参（账号密码） */
export interface AgentLoginInput {
  username: string
  password: string
}

export const useAgentStore = defineStore('agent', () => {
  // 状态
  const token = ref<string>(storage.get<string>(StorageKeys.AGENT_TOKEN) || '')
  const refreshToken = ref<string>(storage.get<string>(StorageKeys.AGENT_REFRESH_TOKEN) || '')
  const agentInfo = ref<AgentInfo | undefined>(storage.get<AgentInfo>(StorageKeys.AGENT_INFO))

  // 计算属性
  const isLogin = computed(() => !!token.value)
  const companyName = computed(() => agentInfo.value?.companyName || '')
  const contactName = computed(() => agentInfo.value?.contactName || agentInfo.value?.name || '')
  const companyId = computed(() => agentInfo.value?.companyId)
  const username = computed(() => agentInfo.value?.username || '')

  /** 设置 Token */
  function setToken(t: string, rt?: string) {
    token.value = t
    storage.set(StorageKeys.AGENT_TOKEN, t)
    if (rt) {
      refreshToken.value = rt
      storage.set(StorageKeys.AGENT_REFRESH_TOKEN, rt)
    }
  }

  /** 设置代理商信息 */
  function setAgentInfo(info: AgentInfo) {
    agentInfo.value = info
    storage.set(StorageKeys.AGENT_INFO, info)
  }

  /**
   * 代理商登录（账号密码，clientId=PORTAL_AGENT）
   * @param input 登录参数
   */
  async function login(input: AgentLoginInput): Promise<AgentInfo> {
    const data = await apiLogin<AgentInfo>({
      clientId: 'PORTAL_AGENT',
      username: input.username,
      password: input.password
    })
    applyLoginResult(data)
    return data.user
  }

  /** 应用登录响应 */
  function applyLoginResult(data: LoginResult<AgentInfo>) {
    setToken(data.token, data.refreshToken)
    setAgentInfo(data.user)
  }

  /** 拉取最新用户信息 */
  async function fetchUserInfo(): Promise<void> {
    try {
      const info = await apiGetUserInfo<AgentInfo>('agent')
      if (info) setAgentInfo(info)
    } catch (e) {
      // 忽略
    }
  }

  /** 退出登录 */
  async function logout(): Promise<void> {
    try {
      await apiLogout('agent')
    } catch (e) {
      // 忽略退出接口错误
    } finally {
      reset()
    }
  }

  /** 清除本地状态 */
  function reset() {
    token.value = ''
    refreshToken.value = ''
    agentInfo.value = undefined
    storage.remove(StorageKeys.AGENT_TOKEN)
    storage.remove(StorageKeys.AGENT_REFRESH_TOKEN)
    storage.remove(StorageKeys.AGENT_INFO)
  }

  return {
    token,
    refreshToken,
    agentInfo,
    isLogin,
    companyName,
    contactName,
    companyId,
    username,
    setToken,
    setAgentInfo,
    login,
    fetchUserInfo,
    logout,
    reset
  }
})
