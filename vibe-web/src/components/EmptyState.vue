<script setup lang="ts">
/**
 * 空状态组件（设计文档 3.1.4）
 * 使用 Ant Design Empty + 简要说明 + 操作引导按钮
 */
import { Empty, Button } from 'ant-design-vue'

interface Props {
  /** 说明文字 */
  description?: string
  /** 操作按钮文字 */
  actionText?: string
  /** 操作按钮类型 */
  actionType?: 'primary' | 'default' | 'dashed'
  /** 图标类型 */
  image?: 'default' | 'simple'
  /** 垂直间距 */
  size?: 'default' | 'compact'
}

const props = withDefaults(defineProps<Props>(), {
  actionType: 'primary',
  image: 'default',
  size: 'default'
})

const emit = defineEmits<{
  (e: 'action'): void
}>()
</script>

<template>
  <div class="empty-state" :class="{ compact: size === 'compact' }">
    <Empty :image="image === 'simple' ? Empty.PRESENTED_IMAGE_SIMPLE : undefined">
      <template #description>
        <span class="empty-desc">{{ description || '暂无数据' }}</span>
      </template>
      <Button v-if="actionText" :type="actionType" @click="emit('action')">
        {{ actionText }}
      </Button>
    </Empty>
  </div>
</template>

<style lang="less" scoped>
.empty-state {
  padding: 40px 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  &.compact {
    padding: 16px;
  }
}
.empty-desc {
  color: @text-tertiary;
  font-size: 14px;
}
:deep(.ant-empty) {
  display: flex;
  flex-direction: column;
  align-items: center;
}
</style>
