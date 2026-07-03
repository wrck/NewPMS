/**
 * 客户状态管理
 * - 客户 Token（2h 有效期，由后端签发，本地记录过期时间做前置判断）
 * - 手机号、客户信息缓存
 * - 对接 module-auth AuthController（clientId=PORTAL_CUSTOMER）
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { storage, StorageKeys } from '@/utils/storage'
import { login as apiLogin, logout as apiLogout, sendSmsCode as apiSendSmsCode, getUserInfo as apiGetUserInfo } from '@/api/auth'
import type { CustomerInfo, LoginResult } from '@/types/api'

/** 客户登录入参（手机号+验证码 或 账号+密码，由页面选择） */
export interface CustomerLoginInput {
  phone?: string
  smsCode?: string
  oneTimeToken?: string
  username?: string
  password?: string
}

export const useCustomerStore = defineStore('customer', () => {
  // 状态
  const token = ref<string>(storage.get<string>(StorageKeys.CUSTOMER_TOKEN) || '')
  const expiresAt = ref<number>(storage.get<number>(StorageKeys.CUSTOMER_EXPIRES_AT) || 0)
  const phone = ref<string>(storage.get<string>(StorageKeys.CUSTOMER_PHONE) || '')
  const customerInfo = ref<CustomerInfo | undefined>(storage.get<CustomerInfo>(StorageKeys.CUSTOMER_INFO))

  // 计算属性
  const isLogin = computed(() => !!token.value && Date.now() < expiresAt.value)
  const customerName = computed(() => customerInfo.value?.name || '')
  const projectIds = computed(() => customerInfo.value?.projectIds || [])

  /** 设置 Token 与过期时间 */
  function setToken(t: string, expiresAtTs: number) {
    token.value = t
    expiresAt.value = expiresAtTs
    storage.set(StorageKeys.CUSTOMER_TOKEN, t)
    storage.set(StorageKeys.CUSTOMER_EXPIRES_AT, expiresAtTs)
  }

  /** 设置手机号 */
  function setPhone(p: string) {
    phone.value = p
    storage.set(StorageKeys.CUSTOMER_PHONE, p)
  }

  /** 设置客户信息 */
  function setCustomerInfo(info: CustomerInfo) {
    customerInfo.value = info
    storage.set(StorageKeys.CUSTOMER_INFO, info)
  }

  /**
   * 发送短信验证码
   * @param p 手机号
   */
  async function sendSmsCode(p: string): Promise<void> {
    await apiSendSmsCode(p)
    setPhone(p)
  }

  /**
   * 客户登录（手机号 + 短信验证码，或 账号 + 密码）
   * @param input 登录参数
   */
  async function login(input: CustomerLoginInput): Promise<CustomerInfo> {
    const data = await apiLogin<CustomerInfo>({
      clientId: 'PORTAL_CUSTOMER',
      phone: input.phone,
      smsCode: input.smsCode,
      oneTimeToken: input.oneTimeToken,
      username: input.username,
      password: input.password
    })
    applyLoginResult(data)
    if (input.phone) setPhone(input.phone)
    return data.user
  }

  /** 应用登录响应 */
  function applyLoginResult(data: LoginResult<CustomerInfo>) {
    const expiresAtTs = data.expiresAt || (Date.now() + data.expiresIn * 1000)
    setToken(data.token, expiresAtTs)
    setCustomerInfo(data.user)
  }

  /** 拉取最新用户信息 */
  async function fetchUserInfo(): Promise<void> {
    try {
      const info = await apiGetUserInfo<CustomerInfo>('customer')
      if (info) setCustomerInfo(info)
    } catch (e) {
      // 忽略
    }
  }

  /** 退出登录 */
  async function logout(): Promise<void> {
    try {
      await apiLogout('customer')
    } catch (e) {
      // 忽略退出接口错误
    } finally {
      reset()
    }
  }

  /** 清除本地状态 */
  function reset() {
    token.value = ''
    expiresAt.value = 0
    phone.value = ''
    customerInfo.value = undefined
    storage.remove(StorageKeys.CUSTOMER_TOKEN)
    storage.remove(StorageKeys.CUSTOMER_EXPIRES_AT)
    storage.remove(StorageKeys.CUSTOMER_PHONE)
    storage.remove(StorageKeys.CUSTOMER_INFO)
  }

  return {
    token,
    expiresAt,
    phone,
    customerInfo,
    isLogin,
    customerName,
    projectIds,
    setToken,
    setPhone,
    setCustomerInfo,
    sendSmsCode,
    login,
    fetchUserInfo,
    logout,
    reset
  }
})
