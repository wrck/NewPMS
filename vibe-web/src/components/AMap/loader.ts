/**
 * AMapLoader 单例封装（spec 阶段三 Task 19）
 *
 * 说明：
 *   - @amap/amap-jsapi-loader 默认导出 .load()，返回 Promise<any>（高德官方未提供完整类型）。
 *   - 这里做单例缓存：首次调用注入 _AMapSecurityConfig（安全密钥），后续复用同一 Promise。
 *   - destroyAMap 仅清空缓存 Promise，不主动调用 loader.reset()，
 *     以避免破坏页面中仍存在的地图实例；如确需重置可手动 import 调用。
 *
 * 用法：
 *   import { loadAMap } from '@/components/AMap/loader'
 *   const AMap = await loadAMap()
 *   const map = new AMap.Map(container, { zoom: 11, center: [116.39, 39.9] })
 */
import AMapLoader from '@amap/amap-jsapi-loader'

/** 缓存的加载 Promise，避免重复注入 script */
let amapPromise: Promise<any> | null = null

export interface AMapLoaderOptions {
  /** 高德 Web 端 Key，缺省取 VITE_AMAP_KEY */
  key?: string
  /** JSAPI 版本，缺省取 VITE_AMAP_VERSION 或 2.0 */
  version?: string
  /** 需要预加载的插件列表 */
  plugins?: string[]
}

/**
 * 加载高德 JSAPI（单例）。
 * 首次调用会向 window 注入 _AMapSecurityConfig 并触发脚本注入。
 */
export function loadAMap(options: AMapLoaderOptions = {}): Promise<any> {
  if (amapPromise) return amapPromise

  // 安全密钥（控制台创建 Key 后获取，用于 JSAPI 鉴权）
  const securityCode = import.meta.env.VITE_AMAP_SECURITY_CODE
  if (securityCode) {
    ;(window as any)._AMapSecurityConfig = {
      securityJsCode: securityCode
    }
  }

  amapPromise = AMapLoader.load({
    key: options.key || import.meta.env.VITE_AMAP_KEY || 'demo_key',
    version: options.version || import.meta.env.VITE_AMAP_VERSION || '2.0',
    plugins: options.plugins || [
      'AMap.Scale',
      'AMap.ToolBar',
      'AMap.MarkerCluster',
      'AMap.Geocoder',
      'AMap.Geolocation',
      'AMap.AutoComplete',
      'AMap.PlaceSearch'
    ]
  })

  return amapPromise
}

/** 清空缓存的加载 Promise（不会卸载已注入的 window.AMap） */
export function destroyAMap() {
  amapPromise = null
}
