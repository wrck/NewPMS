<script setup lang="ts">
/**
 * FunnelChart 漏斗图组件（spec 阶段三 Task 17 - SubTask 17.3）
 *
 * Props:
 *   - data: [{ name, value }]           数据项
 *   - sort: 'descending' | 'ascending' | 'none'  排序方式，默认 descending
 */
import { computed } from 'vue'
import type { EChartsOption } from 'echarts'
import BaseChart from './BaseChart.vue'
import { defaultTooltip } from './theme'

interface FunnelItem {
  name: string
  value: number
}

interface Props {
  data: FunnelItem[]
  sort?: 'descending' | 'ascending' | 'none'
}

const props = withDefaults(defineProps<Props>(), {
  sort: 'descending'
})

const option = computed<EChartsOption | null>(() => {
  if (!props.data || props.data.length === 0) return null
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c}', ...defaultTooltip },
    series: [
      {
        type: 'funnel',
        left: '10%',
        top: 40,
        bottom: 40,
        width: '80%',
        min: 0,
        max: Math.max(...props.data.map((d) => d.value), 0),
        minSize: '0%',
        maxSize: '100%',
        sort: props.sort,
        gap: 2,
        label: {
          show: true,
          position: 'inside',
          color: '#fff',
          fontSize: 12
        },
        labelLine: { length: 10, lineStyle: { width: 1 } },
        itemStyle: { borderColor: '#fff', borderWidth: 1 },
        emphasis: {
          label: { fontSize: 14, fontWeight: 'bold' }
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
