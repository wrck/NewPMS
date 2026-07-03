import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { LoginResult, UserInfo } from '@/types/api'
import { storage, StorageKeys } from '@/utils/storage'
import { request } from '@/utils/request'

/**
 * 用户状态 store
 * 移动端 Token 有效期 7 天（由后端签发，前端持久化）
 */
export const useUserStore = defineStore(
  'user',
  () => {
    const token = ref<string>('')
    const refreshToken = ref<string>('')
    /** Token 过期时间戳（ms），由后端 expiresIn 计算 */
    const expireAt = ref<number>(0)
    const userInfo = ref<UserInfo | null>(null)

    const isLoggedIn = computed(() => !!token.value)
    const roles = computed(() => userInfo.value?.roles ?? [])
    const nickname = computed(() => userInfo.value?.nickname ?? '')
    const avatar = computed(() => userInfo.value?.avatar ?? '')
    const username = computed(() => userInfo.value?.username ?? '')

    /** 是否为工程师角色 */
    const isEngineer = computed(
      () => roles.value.includes('ENGINEER') || roles.value.includes('AGENT_ENGINEER')
    )

    /**
     * 登录（账号密码）
     * 后端根据 X-Client-Type=MOBILE 签发 7 天 Token
     * 对齐后端 LoginDTO：字段 account（非 username）
     */
    async function login(payload: { username: string; password: string }): Promise<void> {
      const data = await request.post<LoginResult>('/auth/login', {
        account: payload.username,
        password: payload.password,
        clientType: 'MOBILE'
      })
      setLogin(data)
    }

    /**
     * 登录成功后设置登录态
     * 后端 LoginVO 不直接返回完整 userInfo，前端从 userId/userName/realName 派生
     * @param data 登录响应（LoginVO）
     */
    function setLogin(data: LoginResult): void {
      token.value = data.token
      refreshToken.value = ''
      // 后端 LoginVO 不再单独返回 userInfo，前端根据基础字段构造
      const derived: UserInfo = {
        id: data.userId,
        username: data.userName,
        nickname: data.realName || data.userName,
        roles: [],
        permissions: []
      }
      userInfo.value = derived
      // expiresIn 单位秒，转毫秒时间戳（移动端默认 7 天）
      expireAt.value = Date.now() + (data.expiresIn || 7 * 24 * 3600) * 1000
      // 同步写入 localStorage（供 request 拦截器在 store 初始化前读取）
      storage.set(StorageKeys.TOKEN, data.token)
      storage.set(StorageKeys.REFRESH_TOKEN, '')
      storage.set(StorageKeys.USER_INFO, derived)
    }

    /**
     * 更新 Token（续签）
     */
    function updateToken(newToken: string, newExpireAt?: number): void {
      token.value = newToken
      if (newExpireAt) expireAt.value = newExpireAt
      storage.set(StorageKeys.TOKEN, newToken)
    }

    /** 退出登录：清空状态与本地存储 */
    function logout(): void {
      token.value = ''
      refreshToken.value = ''
      userInfo.value = null
      expireAt.value = 0
      storage.remove(StorageKeys.TOKEN)
      storage.remove(StorageKeys.REFRESH_TOKEN)
      storage.remove(StorageKeys.USER_INFO)
    }

    /** 是否即将过期（剩余 < 2h） */
    function isExpiringSoon(): boolean {
      if (!expireAt.value) return false
      return expireAt.value - Date.now() < 2 * 3600 * 1000
    }

    return {
      token,
      refreshToken,
      expireAt,
      userInfo,
      isLoggedIn,
      roles,
      nickname,
      avatar,
      username,
      isEngineer,
      login,
      setLogin,
      updateToken,
      logout,
      isExpiringSoon
    }
  },
  {
    // 持久化到 localStorage（key 带移动端前缀，避免与 PC 端冲突）
    persist: {
      key: 'vibe_mobile_user',
      storage: localStorage,
      paths: ['token', 'refreshToken', 'expireAt', 'userInfo']
    }
  }
)
