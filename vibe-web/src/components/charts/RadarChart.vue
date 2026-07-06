<script setup lang="ts">
/**
 * RadarChart 雷达图组件（spec 阶段三 Task 17 - SubTask 17.3）
 *
 * Props:
 *   - data: [{ name, value: number[] }]  系列
 *   - indicators: [{ name, max }]         雷达维度指示器
 */
import { computed } from 'vue'
import type { EChartsOption } from 'echarts'
import BaseChart from './BaseChart.vue'
import { defaultLegend, defaultTooltip } from './theme'

interface RadarSeries {
  name: string
  value: number[]
}

interface Indicator {
  name: string
  max: number
}

interface Props {
  data: RadarSeries[]
  indicators: Indicator[]
}

const props = defineProps<Props>()

const option = computed<EChartsOption | null>(() => {
  if (!props.data || props.data.length === 0 || !props.indicators || props.indicators.length === 0) {
    return null
  }
  return {
    tooltip: { ...defaultTooltip },
    legend: { ...defaultLegend },
    radar: {
      indicator: props.indicators,
      radius: '65%',
      splitNumber: 4,
      axisName: { color: '#8C8C8C', fontSize: 12 },
      splitLine: { lineStyle: { color: '#F0F0F0' } },
      splitArea: { areaStyle: { color: ['rgba(0,0,0,0)', 'rgba(0,0,0,0.02)'] } },
      axisLine: { lineStyle: { color: '#F0F0F0' } }
    },
    series: [
      {
        type: 'radar',
        symbolSize: 5,
        lineStyle: { width: 2 },
        areaStyle: { opacity: 0.15 },
        emphasis: { focus: 'series' },
        data: props.data.map((d) => ({
          name: d.name,
          value: d.value
        }))
      }
    ]
  }
})
</script>

<template>
  <BaseChart :option="option" />
</template>
