<template>
  <div class="progress-bar" :class="[`progress-bar--${variant}`]">
    <div class="progress-bar__track">
      <div
        class="progress-bar__fill"
        :style="{ width: `${clampedPercent}%`, background: fillColor }"
        role="progressbar"
        :aria-valuenow="clampedPercent"
        aria-valuemin="0"
        aria-valuemax="100"
      />
    </div>
    <div v-if="showLabel" class="progress-bar__label">
      <slot name="label">{{ clampedPercent }}%</slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  /** 进度百分比 0-100 */
  percent: number
  /** 是否显示百分比文字 */
  showLabel?: boolean
  /** 主题色：默认/客户/代理商 */
  variant?: 'default' | 'customer' | 'agent'
  /** 自定义填充色 */
  color?: string
}

const props = withDefaults(defineProps<Props>(), {
  showLabel: true,
  variant: 'default',
  color: ''
})

const clampedPercent = computed(() => {
  const n = Number(props.percent) || 0
  return Math.min(100, Math.max(0, n))
})

const fillColor = computed(() => {
  if (props.color) return props.color
  switch (props.variant) {
    case 'customer':
      return 'var(--customer-primary)'
    case 'agent':
      return 'var(--agent-primary)'
    default:
      return 'var(--brand-primary)'
  }
})
</script>

<style lang="scss" scoped>
.progress-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;

  &__track {
    flex: 1;
    height: 8px;
    background: #f0f0f0;
    border-radius: 999px;
    overflow: hidden;
  }

  &__fill {
    height: 100%;
    border-radius: 999px;
    transition: width 0.3s ease;
    min-width: 0;
  }

  &__label {
    font-size: 12px;
    color: var(--color-text-regular);
    min-width: 36px;
    text-align: right;
    font-variant-numeric: tabular-nums;
  }
}
</style>
