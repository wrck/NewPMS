/**
 * 认证相关接口封装
 */
import { http } from '@/utils/request'
import type { LoginParams, LoginResult, UserInfo } from '@/types/user'

/** 账号密码登录 */
export function login(params: LoginParams) {
  return http.post<LoginResult>('/auth/login', {
    username: params.username,
    password: params.password,
    clientId: params.clientId || 'PC',
    captcha: params.captcha,
    captchaKey: params.captchaKey
  })
}

/** 退出登录 */
export function logout() {
  return http.post<void>('/auth/logout')
}

/** 刷新 Token */
export function refresh(refreshToken: string) {
  return http.post<LoginResult>('/auth/refresh', { refreshToken })
}

/** 获取当前登录用户信息 */
export function getUserInfo() {
  return http.get<UserInfo>('/auth/me')
}

/** 修改密码 */
export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return http.post<void>('/auth/change-password', data)
}
