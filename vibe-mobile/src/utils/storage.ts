/**
 * 本地存储工具
 * 区分移动端与 PC 端的存储 key，便于同域多端共存
 */

const CLIENT_TYPE = import.meta.env.VITE_CLIENT_TYPE || 'MOBILE'

/** 移动端专用 key 前缀 */
const KEY_PREFIX = `vibe_${CLIENT_TYPE.toLowerCase()}_`

/** 完整 key 拼接 */
function fullKey(key: string): string {
  return `${KEY_PREFIX}${key}`
}

class StorageHelper {
  /** 设置 localStorage */
  set(key: string, value: unknown): void {
    try {
      localStorage.setItem(fullKey(key), JSON.stringify(value))
    } catch (e) {
      console.warn('[storage] set failed:', key, e)
    }
  }

  /** 读取 localStorage */
  get<T = unknown>(key: string, defaultValue?: T): T | undefined {
    try {
      const raw = localStorage.getItem(fullKey(key))
      if (raw === null || raw === undefined) return defaultValue
      return JSON.parse(raw) as T
    } catch (e) {
      console.warn('[storage] get failed:', key, e)
      return defaultValue
    }
  }

  /** 删除 localStorage */
  remove(key: string): void {
    localStorage.removeItem(fullKey(key))
  }

  /** 清理所有移动端存储（按前缀） */
  clearAll(): void {
    const keys: string[] = []
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i)
      if (k && k.startsWith(KEY_PREFIX)) keys.push(k)
    }
    keys.forEach((k) => localStorage.removeItem(k))
  }
}

/** 预定义的存储 key */
export const StorageKeys = {
  /** Token */
  TOKEN: 'token',
  /** 刷新 Token */
  REFRESH_TOKEN: 'refresh_token',
  /** 用户信息 */
  USER_INFO: 'user_info',
  /** 最近一次定位 */
  LAST_LOCATION: 'last_location',
  /** 上传队列快照 */
  UPLOAD_QUEUE_SNAPSHOT: 'upload_queue_snapshot'
} as const

export const storage = new StorageHelper()
