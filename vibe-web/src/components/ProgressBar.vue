<script setup lang="ts">
/**
 * 进度条组件（设计文档 3.1.4）
 * - 百分比数字显示在进度条右侧
 * - 0-30% 红色 / 30-70% 蓝色 / 70-100% 绿色
 * - 进度条高度：8px（普通）/ 20px（突出展示）
 */
import { computed } from 'vue'

interface Props {
  /** 进度值 0-100 */
  percent: number
  /** 高度模式 */
  size?: 'normal' | 'large'
  /** 是否显示百分比文字 */
  showLabel?: boolean
  /** 自定义颜色（覆盖分段色） */
  color?: string
  /** 轨道颜色 */
  trackColor?: string
  /** 是否带圆角 */
  rounded?: boolean
  /** 自定义文字（默认显示百分比） */
  label?: string
}

const props = withDefaults(defineProps<Props>(), {
  size: 'normal',
  showLabel: true,
  rounded: true
})

const clamped = computed(() => Math.max(0, Math.min(100, props.percent)))

const barColor = computed(() => {
  if (props.color) return props.color
  if (clamped.value < 30) return '#FF4D4F'
  if (clamped.value < 70) return '#1677FF'
  return '#52C41A'
})

const height = computed(() => (props.size === 'large' ? '20px' : '8px'))
const radius = computed(() => (props.rounded ? (props.size === 'large' ? '10px' : '4px') : '0'))
</script>

<template>
  <div class="progress-bar">
    <div class="progress-track" :style="{ height, borderRadius: radius, background: trackColor || '#F5F5F5' }">
      <div
        class="progress-fill"
        :style="{ width: clamped + '%', background: barColor, borderRadius: radius }"
      ></div>
    </div>
    <span v-if="showLabel" class="progress-label tnum" :style="{ color: barColor }">
      {{ label ?? clamped + '%' }}
    </span>
  </div>
</template>

<style lang="less" scoped>
.progress-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.progress-track {
  flex: 1;
  overflow: hidden;
  transition: width 0.3s ease;
}
.progress-fill {
  height: 100%;
  transition: width 0.3s ease, background 0.3s ease;
}
.progress-label {
  font-size: 12px;
  font-weight: 600;
  min-width: 36px;
  text-align: right;
  white-space: nowrap;
}
</style>
