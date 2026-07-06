<script setup lang="ts">
/**
 * AMap 高德地图组件（spec 阶段三 Task 19）
 *
 * Props:
 *   - center: [lng, lat]                  地图中心点，默认北京
 *   - zoom: number                        缩放级别，默认 11
 *   - markers: AMapMarker[]               标记点列表
 *   - cluster: boolean                    是否聚合，默认 true
 *   - height: string                      容器高度，默认 '500px'
 *   - showToolBar / showScale: boolean   工具条与比例尺
 *   - mapStyle: string                    地图样式 URL
 *
 * Events:
 *   - map-ready(map)                      地图实例就绪
 *   - marker-click(marker, markerInstance) 标记点击
 *   - map-click(lng, lat)                地图空白点击
 *   - marker-dragend(marker, lng, lat)    标记拖拽结束
 *
 * Expose:
 *   - setCenter(lng, lat, zoom?)          设置中心点
 *   - setZoom(zoom)                       设置缩放
 *   - getMapInstance()                    获取地图实例
 *   - renderMarkers()                     重新渲染标记
 *
 * 说明：
 *   - @amap/amap-jsapi-loader 未提供完整类型，AMap 实例为 any；
 *     组件内不暴露内部实现细节，业务通过事件与 expose 方法交互。
 *   - 首次加载需配置 VITE_AMAP_KEY / VITE_AMAP_SECURITY_CODE（见 .env）。
 *   - 未配置 Key 时 loader 会回退到 'demo_key'，仅用于本地开发联调。
 */
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { loadAMap } from './loader'
import type { AMapProps, AMapMarker, AMapInstance, MarkerInstance } from './types'

const props = withDefaults(defineProps<AMapProps>(), {
  center: () => [116.397428, 39.90923], // 北京
  zoom: 11,
  markers: () => [],
  cluster: true,
  height: '500px',
  showToolBar: true,
  showScale: true,
  mapStyle: 'amap://styles/normal'
})

const emit = defineEmits<{
  'map-ready': [map: AMapInstance]
  'marker-click': [marker: AMapMarker, markerInstance: MarkerInstance]
  'map-click': [lng: number, lat: number]
  'marker-dragend': [marker: AMapMarker, lng: number, lat: number]
}>()

const mapContainerRef = ref<HTMLElement>()
const loading = ref(true)
const error = ref<string>('')

/** 地图实例（any 类型，高德官方未提供完整 TS 类型） */
let mapInstance: AMapInstance | null = null
/** 已渲染的标记实例列表 */
let markersLayer: any[] = []
/** 聚合实例 */
let clusterInstance: any = null

/** 初始化地图 */
const initMap = async () => {
  if (!mapContainerRef.value) return
  loading.value = true
  error.value = ''
  try {
    const AMap = await loadAMap()
    // 用局部常量 inst 承载地图实例，避免 await 后 mapInstance 类型宽化为 null
    const inst: AMapInstance = new AMap.Map(mapContainerRef.value, {
      zoom: props.zoom,
      center: props.center,
      mapStyle: props.mapStyle,
      viewMode: '2D'
    })
    mapInstance = inst
    if (props.showToolBar) {
      inst.addControl(new AMap.ToolBar())
    }
    if (props.showScale) {
      inst.addControl(new AMap.Scale())
    }
    inst.on('click', (e: any) => {
      emit('map-click', e.lnglat.getLng(), e.lnglat.getLat())
    })
    renderMarkers()
    emit('map-ready', inst)
  } catch (e: any) {
    error.value = e?.message || '地图加载失败'
    console.error('[AMap] 加载失败:', e)
  } finally {
    loading.value = false
  }
}

/** 渲染标记点（支持聚合） */
const renderMarkers = async () => {
  // 用局部常量避免 await 后 mapInstance 类型宽化
  const inst = mapInstance
  if (!inst) return
  const AMap = await loadAMap()

  // 清理旧标记
  markersLayer.forEach((m) => inst.remove(m))
  markersLayer = []
  if (clusterInstance) {
    clusterInstance.setMarkers([])
    clusterInstance = null
  }

  if (!props.markers || props.markers.length === 0) return

  const markerInstances = props.markers.map((m) => {
    const marker = new AMap.Marker({
      position: [m.lng, m.lat],
      title: m.title || '',
      content: m.content || undefined,
      icon: m.icon || undefined,
      draggable: m.draggable || false,
      offset: m.offset ? new AMap.Pixel(m.offset[0], m.offset[1]) : new AMap.Pixel(-13, -32)
    })
    marker.on('click', () => emit('marker-click', m, marker))
    if (m.draggable) {
      marker.on('dragend', (e: any) => {
        const lng = e.lnglat.getLng()
        const lat = e.lnglat.getLat()
        emit('marker-dragend', m, lng, lat)
      })
    }
    if (m.infoWindow) {
      const infoWindow = new AMap.InfoWindow({
        content: m.infoWindow.content,
        offset: new AMap.Pixel(0, -32)
      })
      marker.on('click', () => infoWindow.open(inst, [m.lng, m.lat]))
    }
    return marker
  })
  markersLayer = markerInstances

  if (props.cluster && markerInstances.length > 1) {
    clusterInstance = new AMap.MarkerCluster(inst, markerInstances, {
      gridSize: 60,
      maxZoom: 16,
      renderMarker: (ctx: any) => {
        if (ctx.marker) {
          const data = ctx.marker.getData()
          ctx.marker.setContent(
            `<div class="amap-cluster-marker">${data?.[0]?.title || ''}</div>`
          )
        }
      }
    })
  } else {
    inst.add(markerInstances)
  }
}

/** 设置中心点 */
const setCenter = (lng: number, lat: number, zoom?: number) => {
  if (mapInstance) {
    mapInstance.setCenter([lng, lat])
    if (zoom) mapInstance.setZoom(zoom)
  }
}

/** 设置缩放级别 */
const setZoom = (zoom: number) => {
  if (mapInstance) mapInstance.setZoom(zoom)
}

/** 获取地图实例 */
const getMapInstance = () => mapInstance

watch(() => props.markers, () => renderMarkers(), { deep: true })
watch(
  () => props.center,
  (newCenter) => {
    if (mapInstance && newCenter) setCenter(newCenter[0], newCenter[1])
  },
  { deep: true }
)
watch(() => props.zoom, (newZoom) => setZoom(newZoom))

onMounted(() => initMap())
onBeforeUnmount(() => {
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
})

defineExpose({ setCenter, setZoom, getMapInstance, renderMarkers })
</script>

<template>
  <div class="amap-container">
    <div
      ref="mapContainerRef"
      class="amap-canvas"
      :style="{ height: height, width: '100%' }"
    ></div>
    <div v-if="loading" class="amap-loading">
      <a-spin tip="地图加载中..." />
    </div>
    <div v-if="error" class="amap-error">
      <a-empty :description="error" />
    </div>
  </div>
</template>

<style scoped>
.amap-container {
  position: relative;
  width: 100%;
}
.amap-canvas {
  width: 100%;
}
.amap-loading,
.amap-error {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10;
}
:deep(.amap-cluster-marker) {
  padding: 4px 8px;
  border-radius: 4px;
  background: #1677ff;
  color: #fff;
  font-size: 12px;
}
</style>
