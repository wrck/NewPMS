/**
 * 认证 API（module-auth AuthController）
 * 路径前缀 /v1/auth，clientId 区分客户入口（PORTAL_CUSTOMER）与代理商入口（PORTAL_AGENT）
 */
import { http } from '@/utils/request'
import type { LoginParams, LoginResult, UserInfo } from '@/types/api'

/** 登录（统一入口，由 clientId 决定客户/代理商） */
export function login<U extends UserInfo = UserInfo>(params: LoginParams) {
  return http.post<LoginResult<U>>(
    '/v1/auth/login',
    params,
    // 登录请求本身不带 Token，按 clientId 选择作用域以便 401 跳转对应登录页
    {
      authScope: params.clientId === 'PORTAL_CUSTOMER' ? 'customer' : 'agent',
      loadingText: '登录中...'
    }
  )
}

/** 退出登录 */
export function logout(scope: 'customer' | 'agent' = 'agent') {
  return http.post<void>('/v1/auth/logout', undefined, {
    authScope: scope,
    silent: true
  })
}

/** 获取当前用户信息 */
export function getUserInfo<U extends UserInfo = UserInfo>(scope: 'customer' | 'agent' = 'agent') {
  return http.get<U>('/v1/auth/me', undefined, { authScope: scope, silent: true })
}

/**
 * 发送短信验证码（客户登录用，按 v1 约定挂载在 auth 下）
 * 注：后端如未提供该端点，可由调用方静默处理
 */
export function sendSmsCode(phone: string) {
  return http.post<void>(
    '/v1/auth/sms/send',
    { phone, clientId: 'PORTAL_CUSTOMER' },
    { authScope: 'customer', loadingText: '发送中...' }
  )
}
