/**
 * AMap 组件聚合导出（spec 阶段三 Task 19）
 *
 * 用法：
 *   import { AMap } from '@/components/AMap'
 *   // 或聚合导出
 *   import { AMap } from '@/components'
 *
 *   <AMap
 *     :center="[116.397428, 39.90923]"
 *     :zoom="11"
 *     :markers="markers"
 *     cluster
 *     height="500px"
 *     @map-ready="onReady"
 *     @marker-click="onMarkerClick"
 *   />
 */
import AMap from './index.vue'

export { default as AMap } from './index.vue'
export { loadAMap, destroyAMap } from './loader'
export type { AMapProps, AMapMarker, AMapInstance, MarkerInstance } from './types'

export default AMap
