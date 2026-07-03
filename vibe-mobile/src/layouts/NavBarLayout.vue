<script setup lang="ts">
/**
 * 子页面布局：顶部返回栏（NavBar）
 * 适用于任务详情/签到/施工步骤/异常上报/工时填报/消息/设置 等
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const title = computed(() => (route.meta.title as string) || '')

function onBack(): void {
  // 优先历史返回，无历史则回首页
  if (window.history.state && window.history.state.back) {
    router.back()
  } else {
    router.replace('/home')
  }
}
</script>

<template>
  <div class="nav-bar-layout">
    <van-nav-bar
      :title="title"
      left-arrow
      fixed
      placeholder
      safe-area-inset-top
      @click-left="onBack"
    >
      <template #left>
        <van-icon name="arrow-left" size="18" />
        <span class="back-text">返回</span>
      </template>
    </van-nav-bar>
    <div class="nav-content">
      <router-view />
    </div>
  </div>
</template>

<style scoped lang="scss">
.nav-bar-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.nav-content {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.back-text {
  margin-left: 2px;
  font-size: 15px;
}

:deep(.van-nav-bar) {
  background: var(--brand-primary);
}
:deep(.van-nav-bar__title) {
  color: #fff;
  font-size: 16px;
}
:deep(.van-nav-bar .van-icon),
:deep(.van-nav-bar__text) {
  color: #fff !important;
}
</style>
