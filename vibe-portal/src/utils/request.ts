/**
 * Axios 请求实例 - 外部入口 H5
 * - 同时承载客户（临时 2h Token）与代理商（JWT）两套认证
 * - 通过请求级配置 authScope: 'customer' | 'agent' 选择对应 Token
 * - 客户 Token 本地过期前置判断（避免无效请求）
 * - 401 自动跳转对应入口的登录页
 */
import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig
} from 'axios'
import { showFailToast, showLoadingToast, closeToast } from 'vant'
import { storage, StorageKeys } from './storage'
import type { Result } from '@/types/api'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
const CLIENT_TYPE = import.meta.env.VITE_CLIENT_TYPE || 'PORTAL'

/** 鉴权作用域：客户 / 代理商 */
type AuthScope = 'customer' | 'agent'

/** 请求扩展配置（注意：使用 authScope 而非 auth，避免与 axios 内置 auth 冲突） */
interface PortalRequestConfig {
  /** 鉴权作用域，决定使用哪一套 Token；不传则不带 Token */
  authScope?: AuthScope
  /** 静默模式：不弹 toast */
  silent?: boolean
  /** 显示 loading 文案 */
  loadingText?: string
}

/** 读取指定作用域的 Token */
function getTokenByScope(scope: AuthScope): string | undefined {
  if (scope === 'customer') {
    const token = storage.get<string>(StorageKeys.CUSTOMER_TOKEN)
    if (!token) return undefined
    // 客户 Token 本地过期前置判断
    const expiresAt = storage.get<number>(StorageKeys.CUSTOMER_EXPIRES_AT)
    if (expiresAt && Date.now() >= expiresAt) {
      return undefined
    }
    return token
  }
  return storage.get<string>(StorageKeys.AGENT_TOKEN) || undefined
}

const instance: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8',
    'X-Client-Type': CLIENT_TYPE
  }
})

/** 请求拦截器 */
instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const cfg = (config as InternalAxiosRequestConfig & PortalRequestConfig) || {}
    if (cfg.authScope && config.headers) {
      const token = getTokenByScope(cfg.authScope)
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
      // 透传租户类型给后端
      config.headers['X-Tenant-Type'] = cfg.authScope === 'customer' ? 'CUSTOMER' : 'AGENT'
    }
    if (config.headers) {
      config.headers['X-Client-Type'] = CLIENT_TYPE
    }

    if (cfg.loadingText) {
      showLoadingToast({
        message: cfg.loadingText,
        forbidClick: true,
        duration: 0
      })
    }
    return config
  },
  (error) => {
    closeToast()
    return Promise.reject(error)
  }
)

/** 响应拦截器 */
instance.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    const cfg = (response.config as AxiosResponse['config'] & PortalRequestConfig) || {}
    if (cfg.loadingText) closeToast()

    const res = response.data
    // 文件流直接返回
    if (response.config.responseType === 'blob') {
      return response as unknown as AxiosResponse
    }

    // 业务成功
    if (res.code === 0 || res.success === true) {
      return res as unknown as AxiosResponse
    }

    if (!cfg.silent) {
      showFailToast(res.message || '请求失败')
    }
    return Promise.reject(new Error(res.message || 'Error'))
  },
  (error) => {
    closeToast()
    const cfg = (error?.config || {}) as PortalRequestConfig
    const response = error?.response

    if (response) {
      switch (response.status) {
        case 401: {
          const scope: AuthScope = cfg.authScope || 'agent'
          if (scope === 'customer') {
            storage.remove(StorageKeys.CUSTOMER_TOKEN)
            storage.remove(StorageKeys.CUSTOMER_EXPIRES_AT)
            storage.remove(StorageKeys.CUSTOMER_INFO)
            storage.remove(StorageKeys.CUSTOMER_PHONE)
            if (!location.pathname.startsWith('/customer/login')) {
              const redirect = encodeURIComponent(location.pathname + location.search)
              location.href = `/customer/login?redirect=${redirect}`
            }
          } else {
            storage.remove(StorageKeys.AGENT_TOKEN)
            storage.remove(StorageKeys.AGENT_REFRESH_TOKEN)
            storage.remove(StorageKeys.AGENT_INFO)
            if (!location.pathname.startsWith('/agent/login')) {
              const redirect = encodeURIComponent(location.pathname + location.search)
              location.href = `/agent/login?redirect=${redirect}`
            }
          }
          if (!cfg.silent) showFailToast('登录已过期，请重新登录')
          break
        }
        case 403:
          if (!cfg.silent) showFailToast('没有操作权限')
          break
        case 404:
          if (!cfg.silent) showFailToast('请求的资源不存在')
          break
        case 500:
          if (!cfg.silent) showFailToast('服务器开小差，请稍后再试')
          break
        default: {
          const msg = response.data?.message || `请求失败(${response.status})`
          if (!cfg.silent) showFailToast(msg)
        }
      }
    } else if (error.code === 'ECONNABORTED') {
      if (!cfg.silent) showFailToast('请求超时，请检查网络后重试')
    } else if (!navigator.onLine) {
      if (!cfg.silent) showFailToast('当前网络不可用，请检查网络连接')
    } else {
      if (!cfg.silent) showFailToast('网络异常，请稍后再试')
    }
    return Promise.reject(error)
  }
)

/** 通用请求方法封装 */
export interface RequestOptions extends Omit<AxiosRequestConfig, 'auth'>, PortalRequestConfig {}

function request<T = unknown>(config: RequestOptions): Promise<T> {
  return instance.request<unknown, Result<T>>(config).then((res) => res.data as T)
}

export const http = {
  get<T = unknown>(url: string, params?: Record<string, unknown>, options?: RequestOptions) {
    return request<T>({ ...options, url, method: 'GET', params })
  },
  post<T = unknown>(url: string, data?: unknown, options?: RequestOptions) {
    return request<T>({ ...options, url, method: 'POST', data })
  },
  put<T = unknown>(url: string, data?: unknown, options?: RequestOptions) {
    return request<T>({ ...options, url, method: 'PUT', data })
  },
  delete<T = unknown>(url: string, params?: Record<string, unknown>, options?: RequestOptions) {
    return request<T>({ ...options, url, method: 'DELETE', params })
  },
  upload<T = unknown>(
    url: string,
    formData: FormData,
    options?: RequestOptions & { onProgress?: (percent: number) => void }
  ) {
    return request<T>({
      ...options,
      url,
      method: 'POST',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (e.total && options?.onProgress) {
          options.onProgress(Math.round((e.loaded / e.total) * 100))
        }
      }
    })
  }
}

export default instance
