/**
 * Charts 图表组件聚合导出（spec 阶段三 Task 17）
 *
 * 用法：
 *   import { PieChart, LineChart } from '@/components/charts'
 *   // 或聚合导出
 *   import { PieChart } from '@/components'
 *
 * 说明：
 *   - BaseChart 为基础包装组件，业务一般直接使用具体图表子组件
 *   - 8 个图表子组件均基于 BaseChart 封装，自动套用 vibe 主题
 *   - 主题与调色盘从 theme.ts 统一导出，便于业务页面按状态色取色
 */
export { default as BaseChart } from './BaseChart.vue'
export { default as PieChart } from './PieChart.vue'
export { default as LineChart } from './LineChart.vue'
export { default as BarChart } from './BarChart.vue'
export { default as StackedChart } from './StackedChart.vue'
export { default as FunnelChart } from './FunnelChart.vue'
export { default as MapChart } from './MapChart.vue'
export { default as RadarChart } from './RadarChart.vue'
export { default as GaugeChart } from './GaugeChart.vue'

export {
  vibeColors,
  vibeStatusColors,
  vibeTheme,
  BRAND_PRIMARY,
  defaultGrid,
  defaultTooltip,
  defaultLegend
} from './theme'
