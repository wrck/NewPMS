/**
 * Axios 实例封装
 * - baseURL 从 env 读取
 * - 请求拦截器：携带 Bearer Token
 * - 响应拦截器：统一处理 Result 响应体、401 跳登录、Token 续签响应头处理、错误 message 提示
 */
import axios, { type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import type { Result } from '@/types/api'
import { ErrorCode } from '@/types/api'

const TOKEN_KEY = import.meta.env.VITE_TOKEN_KEY || 'vibe_token'
// Token 续签响应头名称（与后端约定）
const REFRESH_TOKEN_HEADER = 'X-Refresh-Token'

// ============ 创建实例 ============
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
})

// ============ 请求拦截器 ============
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('[request] error:', error)
    return Promise.reject(error)
  }
)

// ============ 响应拦截器 ============
// 是否正在跳转登录页，避免重复跳转
let isRedirecting = false

service.interceptors.response.use(
  (response) => {
    // Token 续签：后端在距离过期 < 2h 时自动续签并通过响应头返回新 token
    const newToken = response.headers?.[REFRESH_TOKEN_HEADER.toLowerCase()]
    if (newToken) {
      localStorage.setItem(TOKEN_KEY, newToken)
    }

    const res = response.data as Result

    // 非标准响应体（如二进制流），直接返回原始 response
    if (res === null || typeof res !== 'object' || typeof res.code === 'undefined') {
      return response
    }

    // 业务成功
    if (res.code === ErrorCode.SUCCESS) {
      return res.data
    }

    // 业务失败：弹出错误提示
    handleBusinessError(res)

    // 401/Token 过期：跳转登录
    if (res.code === ErrorCode.UNAUTHORIZED || res.code === ErrorCode.TOKEN_EXPIRED || res.code === ErrorCode.TOKEN_INVALID) {
      redirectToLogin()
    }

    return Promise.reject(res)
  },
  (error) => {
    // HTTP 状态码错误处理
    const status = error?.response?.status
    const respData = error?.response?.data as Result | undefined

    if (status === 401) {
      message.error('登录已失效，请重新登录')
      redirectToLogin()
    } else if (status === 403) {
      message.error('权限不足，无法访问该资源')
    } else if (status === 404) {
      message.error('请求的资源不存在')
    } else if (status && status >= 500) {
      message.error('服务器开小差了，请稍后重试')
    } else if (error.code === 'ECONNABORTED') {
      message.error('请求超时，请检查网络后重试')
    } else if (!error.response) {
      message.error('网络异常，请检查网络连接')
    } else if (respData?.message) {
      message.error(respData.message)
    } else {
      message.error(error.message || '请求失败')
    }

    return Promise.reject(error)
  }
)

/**
 * 处理业务错误（统一错误提示）
 */
function handleBusinessError(res: Result) {
  // 参数校验错误：拼接字段级错误信息
  if (res.code >= ErrorCode.PARAM_ERROR && res.code < ErrorCode.UNAUTHORIZED && res.errors?.length) {
    const detail = res.errors.map((e) => `${e.field}: ${e.message}`).join('；')
    message.error(`${res.message}（${detail}）`)
    return
  }
  message.error(res.message || '操作失败')
}

/**
 * 重定向到登录页（清空本地登录态，避免重复跳转）
 * 根据当前路径自动判断跳 PC 登录页还是 H5 登录页
 */
function redirectToLogin() {
  if (isRedirecting) return
  isRedirecting = true
  localStorage.removeItem(TOKEN_KEY)

  // 使用 location 而非 router，避免在拦截器中引入 router 实例造成循环依赖
  const currentPath = window.location.pathname + window.location.search
  const redirect = encodeURIComponent(currentPath)

  // 根据当前路径判断跳转到哪个登录页
  let loginPath = '/login'
  if (currentPath.startsWith('/h5/customer')) {
    loginPath = '/h5/customer/login'
  } else if (currentPath.startsWith('/h5/agent')) {
    loginPath = '/h5/agent/login'
  }

  window.location.href = `${loginPath}?redirect=${redirect}`

  setTimeout(() => {
    isRedirecting = false
  }, 1000)
}

// ============ 导出便捷方法 ============
/** 泛型 request，自动剥出 data 部分 */
export function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  return service.request<unknown, T>(config)
}

export const http = {
  get<T = unknown>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig) {
    return request<T>({ url, method: 'get', params, ...config })
  },
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return request<T>({ url, method: 'post', data, ...config })
  },
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return request<T>({ url, method: 'put', data, ...config })
  },
  patch<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return request<T>({ url, method: 'patch', data, ...config })
  },
  delete<T = unknown>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig) {
    return request<T>({ url, method: 'delete', params, ...config })
  }
}

export default service
