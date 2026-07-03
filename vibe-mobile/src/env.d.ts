/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string
  readonly VITE_API_TARGET: string
  readonly VITE_API_BASE_URL: string
  readonly VITE_CLIENT_TYPE: string
  readonly VITE_AMAP_KEY: string
  readonly VITE_AMAP_SECURITY_CODE: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

// 高德地图 JS API 全局类型
declare global {
  interface Window {
    AMap: any
    _AMapSecurityConfig?: {
      securityJsCode: string
    }
  }
  // 高德地图 JS API 全局命名空间（供 typeof AMap 引用）
  const AMap: any
}

export {}
