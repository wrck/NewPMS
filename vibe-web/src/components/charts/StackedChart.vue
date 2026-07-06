<script setup lang="ts">
/**
 * StackedChart 堆叠图组件（spec 阶段三 Task 17 - SubTask 17.3）
 *
 * Props:
 *   - data: [{ name, data: number[] }]  系列
 *   - xAxis: string[]                   X 轴类目
 *   - type: 'bar' | 'line'               堆叠类型，默认 'bar'
 */
import { computed } from 'vue'
import type { EChartsOption } from 'echarts'
import BaseChart from './BaseChart.vue'
import { defaultGrid, defaultLegend, defaultTooltip } from './theme'

interface StackedSeries {
  name: string
  data: number[]
}

interface Props {
  data: StackedSeries[]
  xAxis: string[]
  type?: 'bar' | 'line'
}

const props = withDefaults(defineProps<Props>(), {
  type: 'bar'
})

const option = computed<EChartsOption | null>(() => {
  if (!props.data || props.data.length === 0 || !props.xAxis || props.xAxis.length === 0) {
    return null
  }
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, ...defaultTooltip },
    legend: { ...defaultLegend },
    grid: { ...defaultGrid },
    xAxis: { type: 'category', boundaryGap: false, data: props.xAxis },
    yAxis: { type: 'value' },
    series: props.data.map((s) => ({
      name: s.name,
      type: props.type,
      stack: 'total',
      areaStyle: props.type === 'line' ? { opacity: 0.15 } : undefined,
      emphasis: { focus: 'series' },
      ...(props.type === 'bar'
        ? { itemStyle: { borderRadius: [0, 0, 0, 0] } }
        : { smooth: false }),
      data: s.data
    }))
  }
})
</script>

<template>
  <BaseChart :option="option" />
</template>
