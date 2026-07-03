/**
 * 本地存储工具 - 外部入口 H5
 * 客户 Token 与代理商 Token 使用独立的 key，互不干扰
 */

const CLIENT_TYPE = import.meta.env.VITE_CLIENT_TYPE || 'PORTAL'

/** 外部入口专用 key 前缀 */
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

  /** 清理所有外部入口存储（按前缀） */
  clearAll(): void {
    const keys: string[] = []
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i)
      if (k && k.startsWith(KEY_PREFIX)) keys.push(k)
    }
    keys.forEach((k) => localStorage.removeItem(k))
  }
}

/**
 * 预定义的存储 key
 * 客户侧（CUSTOMER_*）与代理商侧（AGENT_*）独立存储
 */
export const StorageKeys = {
  // 客户侧
  CUSTOMER_TOKEN: 'customer_token',
  CUSTOMER_EXPIRES_AT: 'customer_expires_at',
  CUSTOMER_INFO: 'customer_info',
  CUSTOMER_PHONE: 'customer_phone',

  // 代理商侧
  AGENT_TOKEN: 'agent_token',
  AGENT_REFRESH_TOKEN: 'agent_refresh_token',
  AGENT_INFO: 'agent_info'
} as const

export const storage = new StorageHelper()
