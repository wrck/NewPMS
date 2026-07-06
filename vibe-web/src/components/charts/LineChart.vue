<script setup lang="ts">
/**
 * LineChart 折线图组件（spec 阶段三 Task 17 - SubTask 17.3）
 *
 * Props:
 *   - data: [{ name, data: number[] }]  系列
 *   - xAxis: string[]                   X 轴类目
 *   - smooth: boolean                   是否平滑曲线
 *   - area: boolean                     是否面积图，默认 false
 *   - stack: boolean                    是否堆叠
 */
import { computed } from 'vue'
import type { EChartsOption } from 'echarts'
import BaseChart from './BaseChart.vue'
import { defaultGrid, defaultLegend, defaultTooltip } from './theme'

interface LineSeries {
  name: string
  data: number[]
}

interface Props {
  data: LineSeries[]
  xAxis: string[]
  smooth?: boolean
  area?: boolean
  stack?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  smooth: false,
  area: false,
  stack: false
})

const option = computed<EChartsOption | null>(() => {
  if (!props.data || props.data.length === 0 || !props.xAxis || props.xAxis.length === 0) {
    return null
  }
  return {
    tooltip: { trigger: 'axis', ...defaultTooltip },
    legend: { ...defaultLegend },
    grid: { ...defaultGrid },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: props.xAxis
    },
    yAxis: { type: 'value' },
    series: props.data.map((s) => ({
      name: s.name,
      type: 'line',
      smooth: props.smooth,
      stack: props.stack ? 'total' : undefined,
      areaStyle: props.area ? { opacity: 0.15 } : undefined,
      emphasis: { focus: 'series' },
      data: s.data
    }))
  }
})
</script>

<template>
  <BaseChart :option="option" />
</template>
