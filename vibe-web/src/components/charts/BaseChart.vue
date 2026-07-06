<script setup lang="ts">
/**
 * BaseChart 基础图表组件（spec 阶段三 Task 17 - SubTask 17.2 / 17.5）
 *
 * 职责：
 *   1. 封装 vue-echarts 6 + echarts 5，按需注册所需图表/组件模块（一次性 side-effect）
 *   2. 注册 vibe 自定义主题（品牌色 #1677FF + 状态色映射）
 *   3. 监听 option 变化自动 setOption（vue-echarts 内部实现）
 *   4. 容器尺寸变化自动 resize（vue-echarts autoresize，等价于监听窗口 resize adapt）
 *   5. 组件卸载自动 dispose 释放资源（vue-echarts 内部 onUnmounted 实现）
 *   6. loading 状态显示 a-spin
 *   7. 空数据（option 为空）显示 EmptyState 组件
 *   8. expose: resize() / getInstance()
 *
 * 使用示例见 ./README.md
 */
import { computed, ref } from 'vue'
import { Spin } from 'ant-design-vue'
import VChart from 'vue-echarts'
import type { EChartsOption } from 'echarts'
import { use, registerTheme } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import {
  PieChart,
  LineChart,
  BarChart,
  FunnelChart,
  MapChart as EMapChart,
  RadarChart as ERadarChart,
  GaugeChart
} from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DataZoomComponent,
  GeoComponent,
  VisualMapComponent,
  RadarComponent,
  GraphicComponent,
  MarkLineComponent,
  DatasetComponent
} from 'echarts/components'
import { vibeTheme } from './theme'
import EmptyState from '@/components/EmptyState.vue'

/* ============ ECharts 模块按需注册（模块级 side-effect，仅执行一次） ============ */
use([
  CanvasRenderer,
  // 图表类型
  PieChart,
  LineChart,
  BarChart,
  FunnelChart,
  EMapChart,
  ERadarChart,
  GaugeChart,
  // 组件
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DataZoomComponent,
  GeoComponent,
  VisualMapComponent,
  RadarComponent,
  GraphicComponent,
  MarkLineComponent,
  DatasetComponent
])

/* ============ 注册 vibe 主题（仅注册一次，通过主题名引用） ============ */
const VIBE_THEME_NAME = 'vibe'
registerTheme(VIBE_THEME_NAME, vibeTheme)

/* ============ Props ============ */
interface BaseChartProps {
  /** ECharts 配置 */
  option: EChartsOption | null | undefined
  /** 是否 loading */
  loading?: boolean
  /** 图表高度，默认 320px */
  height?: number | string
  /** 主题，默认 vibe（自定义品牌主题） */
  theme?: string
}

const props = withDefaults(defineProps<BaseChartProps>(), {
  loading: false,
  height: 320,
  theme: VIBE_THEME_NAME
})

/* ============ vue-echarts 实例引用 ============ */
const chartRef = ref<InstanceType<typeof VChart> | null>(null)

/* ============ 计算属性 ============ */
const chartHeight = computed(() =>
  typeof props.height === 'number' ? `${props.height}px` : props.height
)

const isEmpty = computed(() => !props.option || Object.keys(props.option).length === 0)

/** 传给 vue-echarts 的 option（null/undefined 统一为 undefined，兼容 ECBasicOption） */
const chartOption = computed(() => props.option ?? undefined)

/* ============ Expose ============ */
/** 手动触发 resize */
function resize(): void {
  chartRef.value?.resize()
}

/** 获取 vue-echarts 组件实例（可进一步通过 .chart 获取 ECharts 原生实例） */
function getInstance(): InstanceType<typeof VChart> | null {
  return chartRef.value
}

defineExpose({ resize, getInstance })
</script>

<template>
  <div class="base-chart" :style="{ height: chartHeight }">
    <div v-if="loading" class="base-chart-loading">
      <Spin tip="加载中..." />
    </div>
    <div v-else-if="isEmpty" class="base-chart-empty">
      <EmptyState description="暂无数据" />
    </div>
    <VChart
      v-else
      ref="chartRef"
      class="base-chart-canvas"
      :option="chartOption"
      :theme="theme"
      autoresize
      :style="{ height: chartHeight }"
    />
  </div>
</template>

<style lang="less" scoped>
.base-chart {
  position: relative;
  width: 100%;
  overflow: hidden;
}
.base-chart-loading,
.base-chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.base-chart-canvas {
  width: 100%;
  height: 100%;
}
</style>
