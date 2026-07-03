<script setup lang="ts">
/**
 * 状态标签组件（设计文档 3.1.4）
 * 按状态语义返回对应颜色 Tag
 */
import { computed } from 'vue'
import { Tag } from 'ant-design-vue'
import { statusColorMap, type StatusTone } from '@/styles/theme'

interface Props {
  /** 色调（语义） */
  tone?: StatusTone
  /** 文本（若不传则用 label） */
  text?: string
  /** 是否边框模式（false=填充浅色背景，true=仅边框） */
  bordered?: boolean
  /** 是否圆角胶囊 */
  pill?: boolean
  /** 自定义颜色（覆盖 tone） */
  color?: string
  /** 自定义背景色 */
  background?: string
}

const props = withDefaults(defineProps<Props>(), {
  bordered: false,
  pill: false
})

const style = computed(() => {
  const opt = props.tone ? statusColorMap[props.tone] : null
  const color = props.color || opt?.color || '#8C8C8C'
  const background = props.background || opt?.background || '#F5F5F5'
  const border = opt?.border || '#D9D9D9'
  if (props.bordered) {
    return {
      color,
      background: 'transparent',
      border: `1px solid ${border}`
    }
  }
  return {
    color,
    background,
    border: `1px solid transparent`
  }
})

const radius = computed(() => (props.pill ? '12px' : '4px'))
</script>

<template>
  <Tag class="status-tag" :style="{ ...style, borderRadius: radius }">
    <span v-if="tone" class="status-dot" :style="{ background: style.color }"></span>
    <slot>{{ text }}</slot>
  </Tag>
</template>

<style lang="less" scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 0;
  padding: 0 8px;
  height: 22px;
  line-height: 20px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}
.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
</style>
