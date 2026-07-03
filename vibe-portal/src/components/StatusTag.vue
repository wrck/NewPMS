<template>
  <span class="status-tag" :style="tagStyle">{{ label }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'default'

interface Props {
  /** 显示文字 */
  label: string
  /** 状态类型 */
  type?: TagType
  /** 自定义颜色（覆盖 type） */
  color?: string
  /** 是否为描边样式 */
  plain?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  type: 'default',
  plain: false
})

const colorMap: Record<TagType, string> = {
  primary: '#1677ff',
  success: '#52c41a',
  warning: '#faad14',
  danger: '#ff4d4f',
  default: '#8c8c8c'
}

const tagStyle = computed(() => {
  const color = props.color || colorMap[props.type]
  if (props.plain) {
    return {
      color,
      background: `${color}1a`,
      border: `1px solid ${color}40`
    }
  }
  return {
    color: '#fff',
    background: color,
    border: `1px solid ${color}`
  }
})
</script>

<style lang="scss" scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.4;
  white-space: nowrap;
  font-weight: 500;
}
</style>
