/**
 * AMap 组件类型定义（spec 阶段三 Task 19）
 *
 * 说明：
 *   - 高德官方未提供完整 TS 类型，AMapInstance / MarkerInstance 采用
 *     "[key: string]: any" 最小定义以避免 vue-tsc 报错；
 *     业务如需更强类型可自行扩展或引入 @amap/amap-jsapi-types。
 */

/** 标记点数据 */
export interface AMapMarker {
  /** 业务标识 */
  id?: string | number
  /** 经度 */
  lng: number
  /** 纬度 */
  lat: number
  /** 鼠标悬浮提示 */
  title?: string
  /** HTML 字符串作为标记内容（自定义 DOM 标记） */
  content?: string
  /** 图标 URL 或图标配置对象 */
  icon?: string | object
  /** 是否可拖拽 */
  draggable?: boolean
  /** 标记偏移 [x, y] */
  offset?: [number, number]
  /** 点击标记时弹出的信息窗口 */
  infoWindow?: {
    content: string
  }
  /** 自定义业务数据，会在事件回调中原样透传 */
  data?: any
}

/** AMap 组件 Props */
export interface AMapProps {
  /** 地图中心点 [lng, lat] */
  center?: [number, number]
  /** 缩放级别 */
  zoom?: number
  /** 标记点列表 */
  markers?: AMapMarker[]
  /** 是否启用聚合（MarkerCluster），默认 true */
  cluster?: boolean
  /** 容器高度，默认 '500px' */
  height?: string
  /** 是否显示工具条，默认 true */
  showToolBar?: boolean
  /** 是否显示比例尺，默认 true */
  showScale?: boolean
  /** 地图样式 URL，默认 'amap://styles/normal' */
  mapStyle?: string
}

/** 高德地图实例类型（最小定义，避免类型报错） */
export interface AMapInstance {
  [key: string]: any
}

/** 标记实例类型（最小定义，避免类型报错） */
export interface MarkerInstance {
  [key: string]: any
}
