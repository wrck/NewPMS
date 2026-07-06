/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string
  readonly VITE_API_BASE_URL: string
  readonly VITE_API_TARGET: string
  readonly VITE_PORT: string
  readonly VITE_TOKEN_KEY: string
  // 高德地图 JSAPI 配置（spec 阶段三 Task 19）
  readonly VITE_AMAP_KEY: string
  readonly VITE_AMAP_SECURITY_CODE: string
  readonly VITE_AMAP_VERSION: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
