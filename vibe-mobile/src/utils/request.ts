import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig
} from 'axios'
import { showToast } from 'vant'
import type { Result } from '@/types/api'
import { ErrorCode, isAuthErrorCode, isForbiddenErrorCode } from '@/types/api'
import { storage, StorageKeys } from './storage'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
const CLIENT_TYPE = import.meta.env.VITE_CLIENT_TYPE || 'MOBILE'

/** 请求时携带的客户端标识头（后端据此签发 7d 移动端 Token） */
const HEADER_CLIENT_TYPE = 'X-Client-Type'
/** 后端续签返回的新 Token 响应头 */
const HEADER_NEW_TOKEN = 'x-new-token'

const service: AxiosInstance = axios.create({
  baseURL,
  timeout: 20000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    [HEADER_CLIENT_TYPE]: CLIENT_TYPE
  }
})

/** 请求拦截器：注入 Token */
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 优先从 localStorage 读取（store 可能尚未初始化），保证拦截器稳定
    const token = storage.get<string>(StorageKeys.TOKEN)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

/** 是否正在跳转登录页（避免 401 时多次重复跳转） */
let isRedirecting = false

/** 401 → 清空登录态并跳登录页 */
function handleUnauthorized(): void {
  storage.remove(StorageKeys.TOKEN)
  storage.remove(StorageKeys.REFRESH_TOKEN)
  storage.remove(StorageKeys.USER_INFO)
  if (!isRedirecting) {
    isRedirecting = true
    showToast({ type: 'fail', message: '登录已过期，请重新登录' })
    // 动态导入避免循环依赖
    import('@/router').then(({ default: router }) => {
      const redirect = router.currentRoute.value.fullPath
      router.replace({ name: 'Login', query: { redirect } }).finally(() => {
        isRedirecting = false
      })
    })
  }
}

/** 解包统一响应体 Result<T>.data */
function unpack<T>(res: AxiosResponse<Result<T>>): T {
  return (res.data as Result<T>).data
}

/** 响应拦截器：统一处理 Result<T>、Token 续签、401 跳登录 */
service.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    // 移动端 Token 续签：后端在距过期 < 2h 时通过响应头返回新 Token
    const newToken = response.headers[HEADER_NEW_TOKEN] as string | undefined
    if (newToken) {
      storage.set(StorageKeys.TOKEN, newToken)
      // 同步更新 store（若已初始化）
      import('@/stores/user')
        .then(({ useUserStore }) => {
          const userStore = useUserStore()
          userStore.updateToken(newToken)
        })
        .catch(() => {
          /* store 尚未初始化时忽略 */
        })
    }

    const res = response.data
    // 非 JSON 响应（如文件流）直接返回
    if (!res || typeof res !== 'object' || typeof res.code === 'undefined') {
      return response as unknown as AxiosResponse
    }

    if (res.code === ErrorCode.SUCCESS) {
      return res as unknown as AxiosResponse
    }

    // 业务错误：统一 Toast 提示
    // 后端认证类错误码 40100-40199 → 触发跳登录
    if (isAuthErrorCode(res.code)) {
      handleUnauthorized()
      return Promise.reject(new Error(res.message || '未授权'))
    }
    // 后端权限类错误码 40300-40399 → 提示无权限
    if (isForbiddenErrorCode(res.code)) {
      showToast({ type: 'fail', message: res.message || '没有访问权限' })
      return Promise.reject(new Error(res.message || '没有访问权限'))
    }

    showToast({ type: 'fail', message: res.message || '请求失败' })
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    const status = error?.response?.status
    const bizCode = error?.response?.data?.code
    if (status === 401 || (typeof bizCode === 'number' && isAuthErrorCode(bizCode))) {
      handleUnauthorized()
    } else if (status === 403 || (typeof bizCode === 'number' && isForbiddenErrorCode(bizCode))) {
      showToast({ type: 'fail', message: error?.response?.data?.message || '没有访问权限' })
    } else if (error?.code === 'ECONNABORTED' || error?.message?.includes('timeout')) {
      showToast({ type: 'fail', message: '网络请求超时，请检查网络' })
    } else if (!navigator.onLine) {
      showToast({ type: 'fail', message: '当前网络不可用，已缓存到本地' })
    } else {
      showToast({ type: 'fail', message: error?.response?.data?.message || error?.message || '网络异常' })
    }
    return Promise.reject(error)
  }
)

/**
 * 业务请求封装：提供 get / post / upload / postForm 便捷方法，自动解包 Result<T>.data
 * 用法：
 *   request.get<T>('/url', params)
 *   request.post<T>('/url', data)
 *   request.upload<T>('/url', file, onProgress, extra)
 *   request.postForm<T>('/url', formData)  // 用于 multipart 含嵌套字段（如签到/签退）
 */
export const request = {
  get<T = unknown>(url: string, params?: Record<string, any>): Promise<T> {
    return service.get(url, { params }).then(unpack) as Promise<T>
  },
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, data, config).then(unpack) as Promise<T>
  },
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return service.put(url, data, config).then(unpack) as Promise<T>
  },
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.delete(url, config).then(unpack) as Promise<T>
  },
  /**
   * 文件上传（multipart/form-data，带进度）
   * @param url 上传地址
   * @param file 文件 Blob/File
   * @param onProgress 进度回调 (loaded, total)
   * @param extra 附加表单字段（扁平键值对）
   */
  upload<T = unknown>(
    url: string,
    file: File | Blob,
    onProgress?: (loaded: number, total: number) => void,
    extra?: Record<string, any>
  ): Promise<T> {
    const form = new FormData()
    form.append('file', file)
    if (extra) {
      Object.keys(extra).forEach((k) => {
        if (extra[k] != null && extra[k] !== '') form.append(k, String(extra[k]))
      })
    }
    return service
      .post(url, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (e) => {
          if (onProgress && e.total) onProgress(e.loaded, e.total)
        }
      })
      .then(unpack) as Promise<T>
  },
  /**
   * 提交 FormData（multipart/form-data），用于含文件 + 嵌套对象的表单
   * 如工单签到：file + location.longitude/latitude/address + remark
   * @param url 提交地址
   * @param form FormData 实例（调用方自行构造）
   * @param onProgress 上传进度回调
   */
  postForm<T = unknown>(
    url: string,
    form: FormData,
    onProgress?: (loaded: number, total: number) => void
  ): Promise<T> {
    return service
      .post(url, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (e) => {
          if (onProgress && e.total) onProgress(e.loaded, e.total)
        }
      })
      .then(unpack) as Promise<T>
  }
}

/**
 * 将对象展开为 FormData，支持嵌套对象（自动转为 a.b.c 格式）和数组（a[0]/a[1]）。
 * 用于后端 Spring multipart form binding 嵌套 DTO（如 WorkOrderCheckinDTO.location）。
 * @param form 目标 FormData（可选，不传则新建）
 * @param data 数据对象
 * @param prefix 字段前缀（嵌套时使用）
 */
export function buildFormData(
  form: FormData = new FormData(),
  data: Record<string, any>,
  prefix = ''
): FormData {
  Object.keys(data || {}).forEach((key) => {
    const value = data[key]
    const field = prefix ? `${prefix}.${key}` : key
    if (value === null || value === undefined || value === '') return
    if (value instanceof File || value instanceof Blob) {
      form.append(field, value)
    } else if (Array.isArray(value)) {
      value.forEach((v, i) => {
        if (v !== null && v !== undefined && v !== '') {
          form.append(`${field}[${i}]`, v as any)
        }
      })
    } else if (typeof value === 'object') {
      buildFormData(form, value, field)
    } else {
      form.append(field, String(value))
    }
  })
  return form
}

export default service
