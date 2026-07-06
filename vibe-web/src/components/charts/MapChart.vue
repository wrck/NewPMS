<script setup lang="ts">
/**
 * MapChart 地图组件（spec 阶段三 Task 17 - SubTask 17.3）
 *
 * Props:
 *   - data: [{ name, value }]            区域数据（name 须与 GeoJSON 的 name 对齐）
 *   - mapType: 'china' | 'world'         地图类型，默认 'china'
 *   - roam: boolean                      是否开启缩放/平移
 *
 * 说明：
 *   ECharts 5 不再内置地图数据（map 目录已移除），需自行注册 GeoJSON。
 *   本组件在挂载时按 mapType 从公共 CDN（阿里 DataV）懒加载 GeoJSON 并通过
 *   echarts.registerMap 注册（注册结果全局缓存，仅加载一次）。
 *   加载失败时降级为 EmptyState 提示，便于在离线环境定位问题。
 */
import { computed, onMounted, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import { registerMap } from 'echarts/core'
import BaseChart from './BaseChart.vue'
import EmptyState from '@/components/EmptyState.vue'
import { defaultTooltip } from './theme'

interface MapItem {
  name: string
  value: number
}

interface Props {
  data: MapItem[]
  mapType?: 'china' | 'world'
  roam?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  mapType: 'china',
  roam: false
})

/** 各 mapType 对应的 GeoJSON CDN 地址 */
const GEO_JSON_URLS: Record<'china' | 'world', string> = {
  china: 'https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json',
  world: 'https://geo.datav.aliyun.com/areas_v3/bound/world.json'
}

/** 已注册的 mapType 集合（避免重复注册） */
const registeredMaps = new Set<string>()
/** 进行中的注册请求（去重并发） */
const pendingRequests = new Map<string, Promise<void>>()

/** 加载状态：loading / error / ready */
const loadStatus = ref<'loading' | 'error' | 'ready'>('loading')

/** 懒加载并注册 GeoJSON */
async function ensureMapRegistered(type: 'china' | 'world'): Promise<void> {
  if (registeredMaps.has(type)) return
  if (pendingRequests.has(type)) return pendingRequests.get(type)

  const promise = (async () => {
    const url = GEO_JSON_URLS[type]
    const res = await fetch(url)
    if (!res.ok) throw new Error(`加载地图数据失败：HTTP ${res.status}`)
    const json = await res.json()
    registerMap(type, json)
    registeredMaps.add(type)
  })()
  pendingRequests.set(type, promise)
  try {
    await promise
  } finally {
    pendingRequests.delete(type)
  }
}

onMounted(async () => {
  loadStatus.value = 'loading'
  try {
    await ensureMapRegistered(props.mapType)
    loadStatus.value = 'ready'
  } catch (e) {
    console.error('[MapChart] 地图数据加载失败：', e)
    loadStatus.value = 'error'
  }
})

const option = computed<EChartsOption | null>(() => {
  if (loadStatus.value !== 'ready') return null
  if (!props.data || props.data.length === 0) return null
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c}', ...defaultTooltip },
    visualMap: {
      type: 'continuous',
      min: 0,
      max: Math.max(...props.data.map((d) => d.value), 1),
      left: 16,
      bottom: 16,
      text: ['高', '低'],
      inRange: { color: ['#E6F4FF', '#4096FF', '#1677FF', '#0958D9'] },
      calculable: true,
      textStyle: { color: '#8C8C8C', fontSize: 12 }
    },
    series: [
      {
        name: '区域分布',
        type: 'map',
        map: props.mapType,
        roam: props.roam,
        label: { show: true, color: '#595959', fontSize: 11 },
        emphasis: {
          label: { show: true, color: '#1F1F1F', fontWeight: 'bold' },
          itemStyle: { areaColor: '#4096FF', borderColor: '#fff' }
        },
        data: props.data
      }
    ]
  }
})
</script>

<template>
  <div v-if="loadStatus === 'error'" class="map-chart-error">
    <EmptyState description="地图数据加载失败，请检查网络后重试" />
  </div>
  <BaseChart v-else :option="option" :loading="loadStatus === 'loading'" />
</template>

<style lang="less" scoped>
.map-chart-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
