<script setup lang="ts">
/**
 * PieChart 饼图组件（spec 阶段三 Task 17 - SubTask 17.3）
 *
 * Props:
 *   - data: [{ name, value }]          数据项
 *   - title: string                     标题（可选）
 *   - legendVisible: boolean           是否显示图例，默认 true
 *   - ring: boolean                    是否环形图，默认 true
 */
import { computed } from 'vue'
import type { EChartsOption } from 'echarts'
import BaseChart from './BaseChart.vue'
import { defaultLegend, defaultTooltip } from './theme'

interface PieItem {
  name: string
  value: number
}

interface Props {
  data: PieItem[]
  title?: string
  legendVisible?: boolean
  ring?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  legendVisible: true,
  ring: true
})

const option = computed<EChartsOption | null>(() => {
  if (!props.data || props.data.length === 0) return null
  return {
    title: props.title
      ? { text: props.title, left: 'center', top: 0 }
      : undefined,
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)',
      ...defaultTooltip
    },
    legend: props.legendVisible
      ? {
          ...defaultLegend,
          type: 'scroll'
        }
      : undefined,
    series: [
      {
        name: props.title || '占比',
        type: 'pie',
        radius: props.ring ? ['40%', '70%'] : '70%',
        center: ['50%', '52%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}: {d}%',
          color: '#595959',
          fontSize: 12
        },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold' },
          itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.2)' }
        },
        data: props.data
      }
    ]
  }
})
</script>

<template>
  <BaseChart :option="option" />
</template>
