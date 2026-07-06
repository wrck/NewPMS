<script setup lang="ts">
/**
 * BarChart 柱状图组件（spec 阶段三 Task 17 - SubTask 17.3）
 *
 * Props:
 *   - data: [{ name, data: number[] }]  系列
 *   - xAxis: string[]                   X 轴类目
 *   - horizontal: boolean               是否横向条形图
 *   - stack: boolean                    是否堆叠
 */
import { computed } from 'vue'
import type { EChartsOption } from 'echarts'
import BaseChart from './BaseChart.vue'
import { defaultGrid, defaultLegend, defaultTooltip } from './theme'

interface BarSeries {
  name: string
  data: number[]
}

interface Props {
  data: BarSeries[]
  xAxis: string[]
  horizontal?: boolean
  stack?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  horizontal: false,
  stack: false
})

const option = computed<EChartsOption | null>(() => {
  if (!props.data || props.data.length === 0 || !props.xAxis || props.xAxis.length === 0) {
    return null
  }
  const categoryAxis = {
    type: 'category' as const,
    data: props.xAxis
  }
  const valueAxis = { type: 'value' as const }
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, ...defaultTooltip },
    legend: { ...defaultLegend },
    grid: { ...defaultGrid },
    xAxis: props.horizontal ? valueAxis : categoryAxis,
    yAxis: props.horizontal ? categoryAxis : valueAxis,
    series: props.data.map((s) => ({
      name: s.name,
      type: 'bar',
      stack: props.stack ? 'total' : undefined,
      emphasis: { focus: 'series' },
      itemStyle: { borderRadius: props.horizontal ? [0, 4, 4, 0] : [4, 4, 0, 0] },
      data: s.data
    }))
  }
})
</script>

<template>
  <BaseChart :option="option" />
</template>
