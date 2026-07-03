<script setup lang="ts">
/**
 * 数据统计卡片（设计文档 3.1.4）
 * 标题 + 大数字 + 趋势
 * ┌──────────────────┐
 * │  在建项目          │  14px 辅助文字
 * │  47               │  28px 数据数字 DIN 字体
 * │  ↑ 8 较上月       │  12px 趋势标识 绿↑ / 红↓
 * └──────────────────┘
 */
import { computed } from 'vue'
import { ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons-vue'
import type { TrendDirection } from '@/types/api'

interface Props {
  /** 标题 */
  title: string
  /** 数值 */
  value: number | string
  /** 数值后缀（单位） */
  unit?: string
  /** 趋势方向 */
  trend?: TrendDirection
  /** 趋势数值 */
  trendValue?: number | string
  /** 趋势说明 */
  trendLabel?: string
  /** 图标 */
  icon?: string
  /** 强调色 */
  accent?: string
  /** 是否加载中 */
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  trend: 'flat'
})

const trendColor = computed(() => {
  if (props.trend === 'up') return '#52C41A'
  if (props.trend === 'down') return '#FF4D4F'
  return '#8C8C8C'
})
</script>

<template>
  <div class="statistic-card vibe-card">
    <div class="statistic-head">
      <span class="statistic-title">{{ title }}</span>
      <span v-if="icon" class="statistic-icon">
        <component :is="icon" />
      </span>
    </div>
    <div class="statistic-value tnum">
      <span v-if="loading" class="skeleton">--</span>
      <template v-else>
        {{ value }}
        <span v-if="unit" class="statistic-unit">{{ unit }}</span>
      </template>
    </div>
    <div v-if="trend !== 'flat' || trendValue !== undefined" class="statistic-trend">
      <span class="trend-arrow" :style="{ color: trendColor }">
        <ArrowUpOutlined v-if="trend === 'up'" />
        <ArrowDownOutlined v-else-if="trend === 'down'" />
        <span v-else>—</span>
      </span>
      <span v-if="trendValue !== undefined" class="trend-value" :style="{ color: trendColor }">
        {{ trendValue }}
      </span>
      <span v-if="trendLabel" class="trend-label">{{ trendLabel }}</span>
    </div>
  </div>
</template>

<style lang="less" scoped>
.statistic-card {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
}
.statistic-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.statistic-title {
  font-size: 14px;
  color: @text-tertiary;
}
.statistic-icon {
  color: @brand-primary;
  font-size: 18px;
}
.statistic-value {
  font-size: 28px;
  font-weight: 600;
  color: @text-primary;
  line-height: 1.2;
  font-family: 'DIN Alternate', -apple-system, sans-serif;
}
.statistic-unit {
  font-size: 14px;
  font-weight: 400;
  color: @text-tertiary;
  margin-left: 4px;
}
.statistic-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}
.trend-label {
  color: @text-tertiary;
}
.skeleton {
  color: @text-disabled;
}
</style>
