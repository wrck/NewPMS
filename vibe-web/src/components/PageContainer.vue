<script setup lang="ts">
/**
 * 页面容器（设计文档 3.2）
 * 页面标题区（带操作按钮）+ 内容区
 * ┌──────────────────────────────────────────┐
 * │  页面标题区 (带操作按钮)                  │
 * │  「项目管理」          [+ 创建项目] [筛选] │
 * └──────────────────────────────────────────┘
 */
import { computed } from 'vue'

interface Props {
  /** 页面标题 */
  title?: string
  /** 副标题/描述 */
  description?: string
  /** 是否使用卡片包裹内容区 */
  card?: boolean
  /** 内容区内边距 */
  padding?: number | string
  /** 是否显示返回按钮 */
  showBack?: boolean
  /** 标签页标题（与 title 二选一，标签页模式不渲染标题栏） */
  bare?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  card: false,
  padding: 24,
  showBack: false,
  bare: false
})

const emit = defineEmits<{
  (e: 'back'): void
}>()

const contentStyle = computed(() => ({
  padding: typeof props.padding === 'number' ? `${props.padding}px` : props.padding
}))
</script>

<template>
  <div class="page-container">
    <div v-if="!bare && (title || $slots.header || $slots.extra)" class="page-header vibe-card">
      <div class="page-header-left">
        <span v-if="showBack" class="back-btn" @click="emit('back')">←</span>
        <div class="page-header-title-wrap">
          <h2 v-if="title" class="vibe-page-title">{{ title }}</h2>
          <p v-if="description" class="page-description">{{ description }}</p>
          <slot name="header" />
        </div>
      </div>
      <div v-if="$slots.extra" class="page-header-extra">
        <slot name="extra" />
      </div>
    </div>
    <div class="page-body" :class="{ 'in-card': card }" :style="contentStyle">
      <slot />
    </div>
  </div>
</template>

<style lang="less" scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
}
.page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.page-header-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.page-description {
  margin: 0;
  font-size: 13px;
  color: @text-tertiary;
}
.back-btn {
  cursor: pointer;
  font-size: 18px;
  color: @text-secondary;
  &:hover {
    color: @brand-primary;
  }
}
.page-header-extra {
  display: flex;
  align-items: center;
  gap: 8px;
}
.page-body {
  flex: 1;
  min-width: 0;
  &.in-card {
    background: @bg-container;
    border-radius: @radius-card;
    box-shadow: @shadow-card;
  }
}
</style>
