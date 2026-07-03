/**
 * 高德地图 JS API 2.0 加载与 GPS 定位工具
 */
import AMapLoader from '@amap/amap-jsapi-loader'
import { storage, StorageKeys } from './storage'

const AMAP_KEY = import.meta.env.VITE_AMAP_KEY
const AMAP_SECURITY_CODE = import.meta.env.VITE_AMAP_SECURITY_CODE

let amapPromise: Promise<typeof AMap> | null = null

/** 加载高德地图 SDK（单例） */
export function loadAMap(): Promise<typeof AMap> {
  if (amapPromise) return amapPromise
  if (AMAP_SECURITY_CODE) {
    window._AMapSecurityConfig = { securityJsCode: AMAP_SECURITY_CODE }
  }
  amapPromise = AMapLoader.load({
    key: AMAP_KEY,
    version: '2.0',
    plugins: ['AMap.Geolocation', 'AMap.Geocoder']
  })
  return amapPromise
}

/** 定位结果 */
export interface LocationResult {
  /** 经度 */
  longitude: number
  /** 纬度 */
  latitude: number
  /** 精度（米） */
  accuracy: number
  /** 地址描述 */
  address?: string
  /** 省份 */
  province?: string
  /** 城市 */
  city?: string
  /** 区县 */
  district?: string
  /** 定位时间戳 */
  timestamp: number
}

/**
 * 获取当前位置（浏览器 Geolocation API 优先，回退高德 SDK）
 */
export async function getCurrentLocation(): Promise<LocationResult> {
  // 优先使用高德 SDK 定位（更准确，附带逆地理编码）
  try {
    const AMap = await loadAMap()
    return await new Promise<LocationResult>((resolve, reject) => {
      const geolocation = new AMap.Geolocation({
        enableHighAccuracy: true,
        timeout: 10000,
        GeoLocationFirst: true,
        maximumAge: 0,
        convert: true,
        showButton: false,
        showMarker: false,
        showCircle: false
      })
      geolocation.getCurrentPosition((status: string, result: any) => {
        if (status === 'complete' && result.position) {
          const loc: LocationResult = {
            longitude: result.position.lng,
            latitude: result.position.lat,
            accuracy: result.accuracy || 0,
            address: result.formattedAddress,
            province: result.addressComponent?.province,
            city: result.addressComponent?.city,
            district: result.addressComponent?.district,
            timestamp: Date.now()
          }
          // 缓存最近一次定位
          storage.set(StorageKeys.LAST_LOCATION, loc)
          resolve(loc)
        } else {
          reject(new Error(result?.message || '高德定位失败'))
        }
      })
    })
  } catch (err) {
    console.warn('[gps] AMap geolocation failed, fallback to browser API', err)
    return fallbackBrowserGeolocation()
  }
}

/** 浏览器原生定位回退方案 */
function fallbackBrowserGeolocation(): Promise<LocationResult> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('设备不支持定位'))
      return
    }
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const loc: LocationResult = {
          longitude: position.coords.longitude,
          latitude: position.coords.latitude,
          accuracy: position.coords.accuracy,
          timestamp: position.timestamp
        }
        storage.set(StorageKeys.LAST_LOCATION, loc)
        resolve(loc)
      },
      (err) => {
        let msg = '定位失败'
        switch (err.code) {
          case err.PERMISSION_DENIED:
            msg = '定位权限被拒绝，请在系统设置中允许定位'
            break
          case err.POSITION_UNAVAILABLE:
            msg = '位置信息不可用'
            break
          case err.TIMEOUT:
            msg = '定位超时，请重试'
            break
        }
        reject(new Error(msg))
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
    )
  })
}

/**
 * 计算两点之间的距离（Haversine 公式，单位：米）
 */
export function getDistance(
  lon1: number,
  lat1: number,
  lon2: number,
  lat2: number
): number {
  const R = 6371000 // 地球半径（米）
  const toRad = (deg: number) => (deg * Math.PI) / 180
  const dLat = toRad(lat2 - lat1)
  const dLon = toRad(lon2 - lon1)
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) *
      Math.cos(toRad(lat2)) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return Math.round(R * c)
}

/**
 * 获取最近一次缓存的定位
 */
export function getLastLocation(): LocationResult | undefined {
  return storage.get<LocationResult>(StorageKeys.LAST_LOCATION)
}

/**
 * 在指定容器内渲染高德地图（显示当前位置标记）
 */
export async function renderMap(
  container: HTMLElement,
  longitude: number,
  latitude: number,
  options: { zoom?: number; markerLabel?: string } = {}
): Promise<any> {
  const AMap = await loadAMap()
  const map = new AMap.Map(container, {
    zoom: options.zoom ?? 16,
    center: [longitude, latitude],
    viewMode: '2D'
  })
  const marker = new AMap.Marker({
    position: [longitude, latitude],
    label: options.markerLabel
      ? { content: options.markerLabel, direction: 'top' }
      : undefined
  })
  map.add(marker)
  return map
}
