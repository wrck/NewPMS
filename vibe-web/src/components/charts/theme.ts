/**
 * ECharts 统一主题配置（spec 阶段三 Task 17 - SubTask 17.4）
 *
 * 设计要点：
 * - 品牌色 #1677FF 为基色，扩展 10 色调色盘覆盖多系列
 * - 状态色与 src/styles/variables.less 中的 @status-* 保持一致
 * - 统一 tooltip / legend / grid / 坐标轴 样式
 * - 通过 echarts.registerTheme('vibe', vibeTheme) 注册后，由 vue-echarts 的 theme 属性引用
 */
import type { EChartsOption } from 'echarts'

/** 品牌主色 */
export const BRAND_PRIMARY = '#1677FF'

/**
 * Vibe 调色盘（10 色）
 * 顺序：品牌蓝 → 成功绿 → 警告黄 → 异常红 → 代理商紫 → 青色 → 粉色 → 橙色 → 黄绿 → 靛蓝
 * 与 src/styles/variables.less 的状态色语义对齐，便于图表与状态 Tag 视觉统一
 */
export const vibeColors = [
  '#1677FF',
  '#52C41A',
  '#FAAD14',
  '#FF4D4F',
  '#722ED1',
  '#13C2C2',
  '#EB2F96',
  '#FA8C16',
  '#A0D911',
  '#2F54EB'
]

/** 状态色映射（用于业务图表按状态上色时直接取色） */
export const vibeStatusColors = {
  success: '#52C41A',
  processing: '#1677FF',
  pending: '#FAAD14',
  exception: '#FF4D4F',
  archived: '#8C8C8C',
  agent: '#722ED1',
  hold: '#FADB14'
} as const

/** 统一字体族 */
const VIBE_FONT_FAMILY =
  '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif'

/**
 * Vibe ECharts 主题对象
 * 注册：registerTheme('vibe', vibeTheme)
 * 引用：<BaseChart theme="vibe" />
 */
export const vibeTheme = {
  color: vibeColors,
  backgroundColor: 'transparent',
  textStyle: {
    fontFamily: VIBE_FONT_FAMILY,
    color: '#1F1F1F'
  },
  title: {
    textStyle: {
      color: '#1F1F1F',
      fontWeight: 600,
      fontSize: 16,
      fontFamily: VIBE_FONT_FAMILY
    },
    subtextStyle: {
      color: '#8C8C8C',
      fontFamily: VIBE_FONT_FAMILY
    }
  },
  legend: {
    textStyle: {
      color: '#595959',
      fontSize: 12,
      fontFamily: VIBE_FONT_FAMILY
    },
    pageTextStyle: {
      color: '#8C8C8C'
    }
  },
  tooltip: {
    backgroundColor: 'rgba(255, 255, 255, 0.96)',
    borderColor: '#F0F0F0',
    borderWidth: 1,
    padding: [8, 12],
    textStyle: {
      color: '#1F1F1F',
      fontSize: 12,
      fontFamily: VIBE_FONT_FAMILY
    },
    extraCssText: 'box-shadow: 0 3px 6px rgba(0,0,0,0.06), 0 6px 16px rgba(0,0,0,0.08); border-radius: 8px;'
  },
  grid: {
    top: 40,
    right: 24,
    bottom: 40,
    left: 48,
    containLabel: true
  },
  categoryAxis: {
    axisLine: { lineStyle: { color: '#D9D9D9' } },
    axisTick: { lineStyle: { color: '#D9D9D9' } },
    axisLabel: { color: '#8C8C8C', fontSize: 12 },
    splitLine: { show: false, lineStyle: { color: '#F0F0F0' } }
  },
  valueAxis: {
    axisLine: { show: false, lineStyle: { color: '#D9D9D9' } },
    axisTick: { show: false, lineStyle: { color: '#D9D9D9' } },
    axisLabel: { color: '#8C8C8C', fontSize: 12 },
    splitLine: { show: true, lineStyle: { color: '#F0F0F0', type: 'dashed' } }
  },
  line: {
    lineStyle: { width: 2 },
    symbolSize: 6,
    symbol: 'circle'
  },
  bar: {
    itemStyle: { borderRadius: [4, 4, 0, 0] }
  },
  pie: {
    itemStyle: { borderColor: '#fff', borderWidth: 2 }
  },
  radar: {
    axisName: { color: '#8C8C8C', fontSize: 12 },
    splitLine: { lineStyle: { color: '#F0F0F0' } },
    splitArea: { areaStyle: { color: ['rgba(0,0,0,0)', 'rgba(0,0,0,0.02)'] } },
    axisLine: { lineStyle: { color: '#F0F0F0' } }
  },
  funnel: {
    itemStyle: { borderColor: '#fff', borderWidth: 1 }
  },
  gauge: {
    axisLine: { lineStyle: { color: [[1, '#E8E8E8']] } },
    axisTick: { lineStyle: { color: '#D9D9D9' } },
    splitLine: { lineStyle: { color: '#D9D9D9' } },
    axisLabel: { color: '#8C8C8C' },
    detail: { color: '#1F1F1F' }
  }
}

/** 默认 grid 配置（供子组件复用） */
export const defaultGrid: EChartsOption['grid'] = {
  top: 40,
  right: 24,
  bottom: 40,
  left: 48,
  containLabel: true
}

/** 默认 tooltip 配置（供子组件复用，触发类型由各图表决定） */
export const defaultTooltip = {
  backgroundColor: 'rgba(255, 255, 255, 0.96)',
  borderColor: '#F0F0F0',
  borderWidth: 1,
  padding: [8, 12],
  textStyle: {
    color: '#1F1F1F',
    fontSize: 12
  },
  extraCssText:
    'box-shadow: 0 3px 6px rgba(0,0,0,0.06), 0 6px 16px rgba(0,0,0,0.08); border-radius: 8px;'
}

/** 默认 legend 配置（供子组件复用） */
export const defaultLegend = {
  bottom: 0,
  textStyle: { color: '#595959', fontSize: 12 },
  icon: 'circle',
  itemWidth: 8,
  itemHeight: 8,
  itemGap: 16
}
