<template>
  <div class="agent-layout">
    <header class="agent-layout__header">
      <van-icon
        v-if="showBack"
        name="arrow-left"
        size="20"
        class="agent-layout__back"
        @click="onBack"
      />
      <h1 class="agent-layout__title">{{ title }}</h1>
      <span class="agent-layout__company" v-if="!showBack && companyName">{{ companyName }}</span>
    </header>
    <main class="agent-layout__main">
      <router-view v-slot="{ Component }">
        <keep-alive :include="cacheViews">
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </main>
    <van-tabbar v-model="activeTab" route :placeholder="true" :safe-area-inset-bottom="true">
      <van-tabbar-item
        v-for="t in tabs"
        :key="t.name"
        :to="t.to"
        :name="t.name"
        :icon="t.icon"
      >
        {{ t.label }}
      </van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAgentStore } from '@/stores/agent'

const route = useRoute()
const router = useRouter()
const agentStore = useAgentStore()

const activeTab = ref('dashboard')

const title = computed(() => (route.meta.title as string) || '代理商工作台')
const companyName = computed(() => agentStore.companyName)
const showBack = computed(() => !['/agent/dashboard', '/agent/tasks', '/agent/mine'].includes(route.path))
const cacheViews = computed(() => ['AgentDashboard', 'AgentTaskList'])

const tabs = [
  { name: 'dashboard', label: '工作台', icon: 'wap-nav', to: '/agent/dashboard' },
  { name: 'tasks', label: '任务', icon: 'orders-o', to: '/agent/tasks' },
  { name: 'mine', label: '我的', icon: 'manager-o', to: '/agent/mine' }
]

function onBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/agent/dashboard')
  }
}
</script>

<style lang="scss" scoped>
.agent-layout {
  display: flex;
  flex-direction: column;
  height: 100%;

  &__header {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    height: 46px;
    padding: 0 12px;
    padding-top: var(--safe-top);
    background: var(--agent-primary);
    color: #fff;
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: #fff;
  }

  &__back {
    position: absolute;
    left: 12px;
    top: 50%;
    transform: translateY(-50%);
    color: #fff;
    padding: 4px;
  }

  &__company {
    position: absolute;
    right: 12px;
    top: 50%;
    transform: translateY(-50%);
    font-size: 12px;
    color: rgba(255, 255, 255, 0.85);
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__main {
    flex: 1;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
  }
}

:deep(.van-tabbar-item--active) {
  color: var(--agent-primary);
}
</style>
