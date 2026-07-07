<script setup lang="ts">
/**
 * 上下文帮助提示组件（Task 10.3）
 *
 * 渲染：? 图标 + Tooltip 悬浮说明
 *
 * 用法：
 *   <HelpHint content="此处说明文字" />
 *   <HelpHint title="标题" content="说明" :size="14" />
 *
 * 设计原则：
 *   - 不引入新依赖，复用 ant-design-vue 的 Tooltip
 *   - 仅在页面标题处使用，不影响其他逻辑
 *   - 暗色背景的 Tooltip 让标题与正文层次分明
 */
import { Tooltip } from 'ant-design-vue'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'

interface Props {
  /** 帮助标题（可选，默认不显示） */
  title?: string
  /** 帮助正文（支持换行 \n） */
  content: string
  /** 图标大小（像素），默认 14 */
  size?: number
  /** 图标颜色（不传则跟随主题弱色） */
  color?: string
  /** Tooltip 弹出位置，默认 bottom */
  placement?: 'top' | 'bottom' | 'left' | 'right' | 'topLeft' | 'topRight' | 'bottomLeft' | 'bottomRight'
}

withDefaults(defineProps<Props>(), {
  title: '',
  size: 14,
  color: undefined,
  placement: 'bottom'
})
</script>

<template>
  <Tooltip :overlay-style="{ maxWidth: '320px' }" :placement="placement">
    <template #title>
      <div class="help-hint-content">
        <div v-if="title" class="help-hint-title">{{ title }}</div>
        <div class="help-hint-body">{{ content }}</div>
      </div>
    </template>
    <span class="help-hint-icon" :style="{ color: color, fontSize: `${size}px` }">
      <QuestionCircleOutlined />
    </span>
  </Tooltip>
</template>

<style lang="less" scoped>
.help-hint-icon {
  display: inline-flex;
  align-items: center;
  cursor: help;
  color: rgba(0, 0, 0, 0.45);
  vertical-align: middle;
  margin-left: 6px;
  transition: color 0.2s;
  &:hover {
    color: @brand-primary;
  }
}

.help-hint-content {
  font-size: 12px;
  line-height: 1.6;
}

.help-hint-title {
  font-weight: 600;
  margin-bottom: 4px;
  color: #fff;
}

.help-hint-body {
  white-space: pre-wrap;
  color: rgba(255, 255, 255, 0.85);
}
</style>
