/**
 * Axios 实例封装
 * - baseURL 从 env 读取
 * - 请求拦截器：携带 Bearer Token
 * - 响应拦截器：统一处理 Result 响应体、401 跳登录、Token 续签响应头处理、错误 message 提示
 *
 * 错误处理策略（Task C1）：
 * - 400xx 参数校验错误：提取 errors[] 并通过 window.__lastFieldErrors 暴露给表单组件高亮字段
 * - 403xx 权限错误：提示「权限不足」
 * - 409xx 业务冲突：提示后端返回的 message
 * - 网络错误 / 超时：Modal.confirm 弹出友好提示与「重试」按钮，点击后重发原请求
 */
import axios, { type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { message, Modal } from 'ant-design-vue'
import type { Result } from '@/types/api'
import { ErrorCode } from '@/types/api'

const TOKEN_KEY = import.meta.env.VITE_TOKEN_KEY || 'vibe_token'
// Token 续签响应头名称（与后端约定）
const REFRESH_TOKEN_HEADER = 'X-Refresh-Token'

// ============ 字段错误全局暴露（供表单组件读取并高亮）============
/**
 * 最近一次后端返回的字段级错误列表。
 *
 * <p>表单组件可在 catch 到请求拒绝后，读取此数组并设置对应字段的 errorStatus，
 * 实现 400xx 错误的字段级高亮。每次新请求发起时会自动清空。</p>
 */
export interface FieldError {
  field: string
  message: string
}

;(globalThis as unknown as { __lastFieldErrors?: FieldError[] }).__lastFieldErrors = []

/** 读取最近一次字段错误（表单组件使用） */
export function getLastFieldErrors(): FieldError[] {
  return (globalThis as unknown as { __lastFieldErrors?: FieldError[] }).__lastFieldErrors ?? []
}

/** 清空字段错误（表单组件在用户开始修正字段时调用） */
export function clearLastFieldErrors(): void {
  ;(globalThis as unknown as { __lastFieldErrors?: FieldError[] }).__lastFieldErrors = []
}

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
    // 每次发起新请求时清空上一次的字段错误，避免陈旧高亮
    clearLastFieldErrors()
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
      // 403xx 权限错误：优先展示后端 message，否则固定提示
      message.error(respData?.message || '权限不足，无法访问该资源')
    } else if (status === 404) {
      message.error('请求的资源不存在')
    } else if (status === 409) {
      // 409xx 业务冲突：直接展示后端 message
      message.error(respData?.message || '业务冲突，请刷新后重试')
    } else if (status && status >= 500) {
      message.error('服务器开小差了，请稍后重试')
    } else if (error.code === 'ECONNABORTED') {
      // 请求超时：弹出 Modal 提供重试
      showRetryModal('请求超时', '网络请求超时，是否重试？', error.config)
    } else if (!error.response) {
      // 网络异常（无法连接到服务器）：弹出 Modal 提供重试
      showRetryModal('网络异常', '网络连接异常，请检查网络后重试。', error.config)
    } else if (respData?.message) {
      message.error(respData.message)
    } else {
      message.error(error.message || '请求失败')
    }

    return Promise.reject(error)
  }
)

/**
 * 网络错误重试 Modal：友好提示并提供「重试」按钮重发原请求。
 *
 * <p>使用 Modal.confirm 弹出，避免在网络异常时反复弹出多个 Modal：
 * 通过 isRetryModalShowing 标志位控制同时只显示一个重试 Modal。</p>
 *
 * @param title   Modal 标题
 * @param content Modal 内容
 * @param config  原始请求配置（axios AxiosRequestConfig），用于重发请求
 */
let isRetryModalShowing = false
function showRetryModal(title: string, content: string, config?: AxiosRequestConfig) {
  if (isRetryModalShowing) {
    // 已有重试 Modal 显示中，不再重复弹出
    return
  }
  isRetryModalShowing = true
  Modal.confirm({
    title,
    content,
    okText: '重试',
    cancelText: '取消',
    onOk: async () => {
      try {
        if (config) {
          await service.request(config)
        }
      } catch (e) {
        // 重试失败仍走拦截器逻辑（可能再次弹出 Modal），不在此处理
        console.warn('[request] retry failed:', e)
      } finally {
        isRetryModalShowing = false
      }
    },
    onCancel: () => {
      isRetryModalShowing = false
    }
  })
}

/**
 * 处理业务错误（统一错误提示）
 *
 * <p>分类策略：</p>
 * <ul>
 *   <li>400xx 参数校验错误：提取 errors[] 写入 window.__lastFieldErrors，
 *       并以「字段: 消息」形式拼接提示</li>
 *   <li>403xx 权限错误：固定提示「权限不足」</li>
 *   <li>409xx 业务冲突：直接展示后端 message</li>
 *   <li>其它：展示后端 message 或默认提示</li>
 * </ul>
 */
function handleBusinessError(res: Result) {
  const code = res.code

  // 400xx 参数校验错误：提取 errors[] 高亮字段
  if (code >= ErrorCode.PARAM_ERROR && code < ErrorCode.UNAUTHORIZED) {
    const errors = res.errors ?? []
    ;(globalThis as unknown as { __lastFieldErrors?: FieldError[] }).__lastFieldErrors = errors
    if (errors.length > 0) {
      const detail = errors.map((e) => `${e.field}: ${e.message}`).join('；')
      message.error(`${res.message || '参数校验失败'}（${detail}）`)
    } else {
      message.error(res.message || '参数校验失败')
    }
    return
  }

  // 403xx 权限错误
  if (code >= ErrorCode.FORBIDDEN && code < ErrorCode.NOT_FOUND) {
    message.error(res.message || '权限不足')
    return
  }

  // 409xx 业务冲突
  if (code >= ErrorCode.CONFLICT && code < ErrorCode.SERVER_ERROR) {
    message.error(res.message || '业务冲突，请刷新后重试')
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
