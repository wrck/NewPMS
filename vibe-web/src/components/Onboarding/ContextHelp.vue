<script setup lang="ts">
/**
 * 上下文帮助气泡（Task D4.2）
 *
 * props:
 *   - title: 帮助标题
 *   - content: 帮助正文（支持换行）
 *   - selector: 可选，目标元素 selector（用于在气泡标题中显示目标名称）
 *
 * 渲染：? 图标 + Tooltip 气泡
 */
import { Tooltip } from 'ant-design-vue'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'

interface Props {
  title?: string
  content: string
  selector?: string
  /** 图标大小，默认 14 */
  size?: number
  /** 图标颜色，默认跟随主题 */
  color?: string
}

withDefaults(defineProps<Props>(), {
  title: '帮助',
  size: 14,
  color: undefined
})
</script>

<template>
  <Tooltip :overlay-style="{ maxWidth: '320px' }" placement="bottom">
    <template #title>
      <div class="context-help-content">
        <div v-if="title" class="context-help-title">{{ title }}</div>
        <div class="context-help-body">{{ content }}</div>
      </div>
    </template>
    <span class="context-help-icon" :style="{ color: color, fontSize: `${size}px` }">
      <QuestionCircleOutlined />
    </span>
  </Tooltip>
</template>

<style lang="less" scoped>
.context-help-icon {
  display: inline-flex;
  align-items: center;
  cursor: help;
  color: rgba(0, 0, 0, 0.45);
  &:hover {
    color: @brand-primary;
  }
}

.context-help-content {
  font-size: 12px;
  line-height: 1.6;
}

.context-help-title {
  font-weight: 600;
  margin-bottom: 4px;
  color: #fff;
}

.context-help-body {
  white-space: pre-wrap;
  color: rgba(255, 255, 255, 0.85);
}
</style>
