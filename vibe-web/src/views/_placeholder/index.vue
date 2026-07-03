<script setup lang="ts">
/**
 * 通用占位页面（模块开发前填充）
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const title = computed(() => (route.meta?.title as string) || '页面')
</script>

<template>
  <PageContainer :title="title">
    <EmptyState
      description="该页面正在建设中，敬请期待"
      action-text="返回工作台"
      @action="$router.push('/dashboard')"
    />
  </PageContainer>
</template>
