<script setup lang="ts">
/**
 * GaugeChart 仪表盘组件（spec 阶段三 Task 17 - SubTask 17.3）
 *
 * Props:
 *   - value: number         当前值
 *   - title: string         标题（可选）
 *   - min: number           最小值，默认 0
 *   - max: number           最大值，默认 100
 *   - target: number        目标值（可选，在仪表下方展示目标刻度文字）
 */
import { computed } from 'vue'
import type { EChartsOption } from 'echarts'
import BaseChart from './BaseChart.vue'
import { defaultTooltip } from './theme'

interface Props {
  value: number
  title?: string
  min?: number
  max?: number
  target?: number
}

const props = withDefaults(defineProps<Props>(), {
  min: 0,
  max: 100
})

/** 根据比值返回状态色（与 vibeStatusColors 对齐） */
function ratioColor(ratio: number): string {
  if (ratio >= 0.9) return '#52C41A'
  if (ratio >= 0.6) return '#1677FF'
  if (ratio >= 0.3) return '#FAAD14'
  return '#FF4D4F'
}

const option = computed<EChartsOption | null>(() => {
  const ratio = props.max > props.min ? (props.value - props.min) / (props.max - props.min) : 0
  const color = ratioColor(ratio)
  const hasTarget = props.target !== undefined && props.target !== null
  // 目标值通过 detail 下方文字展示，避免 markLine 在仪表盘上的类型与渲染问题
  const targetFormatter = hasTarget ? `\n目标 ${props.target}` : ''
  return {
    tooltip: { formatter: '{b}: {c}', ...defaultTooltip },
    series: [
      {
        name: props.title || '指标',
        type: 'gauge',
        min: props.min,
        max: props.max,
        radius: '90%',
        progress: {
          show: true,
          width: 14,
          roundCap: true,
          itemStyle: { color }
        },
        axisLine: {
          roundCap: true,
          lineStyle: { width: 14, color: [[1, '#F0F0F0']] }
        },
        axisTick: { show: false },
        splitLine: { distance: -20, length: 8, lineStyle: { color: '#D9D9D9' } },
        axisLabel: { distance: -28, color: '#8C8C8C', fontSize: 11 },
        pointer: {
          icon: 'path://M2,-2 L2,2 L0,-60 Z',
          length: '60%',
          width: 6,
          offsetCenter: [0, 0],
          itemStyle: { color }
        },
        anchor: { show: true, size: 12, itemStyle: { color, borderColor: '#fff', borderWidth: 2 } },
        title: {
          show: !!props.title,
          offsetCenter: [0, '78%'],
          color: '#8C8C8C',
          fontSize: 13
        },
        detail: {
          valueAnimation: true,
          formatter: '{value}%' + targetFormatter,
          offsetCenter: [0, '24%'],
          color: '#1F1F1F',
          fontSize: 22,
          fontWeight: 'bold',
          fontFamily: 'DIN Alternate, -apple-system, sans-serif',
          rich: hasTarget
            ? {
                b: { color: '#8C8C8C', fontSize: 12, fontWeight: 'normal', lineHeight: 18 }
              }
            : undefined
        },
        data: [{ value: props.value, name: props.title || '' }]
      }
    ]
  }
})
</script>

<template>
  <BaseChart :option="option" />
</template>
