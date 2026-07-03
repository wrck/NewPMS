<script setup lang="ts">
/**
 * 底部 Tab 栏布局（设计文档 3.4.1）
 * 4 个 Tab：首页 / 任务 / 现场 / 我的
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

interface TabItem {
  key: string
  name: string
  label: string
  icon: string
  iconActive: string
}

const tabs: TabItem[] = [
  { key: 'home', name: 'HomeIndex', label: '首页', icon: 'wap-home-o', iconActive: 'wap-home' },
  { key: 'task', name: 'TaskIndex', label: '任务', icon: 'orders-o', iconActive: 'orders' },
  { key: 'field', name: 'FieldIndex', label: '现场', icon: 'photo-o', iconActive: 'photo' },
  { key: 'mine', name: 'MineIndex', label: '我的', icon: 'user-o', iconActive: 'user' }
]

const activeKey = computed(() => (route.meta.tabKey as string) || 'home')

function onTabChange(item: TabItem): void {
  if (item.name === route.name) return
  router.push({ name: item.name })
}
</script>

<template>
  <div class="tab-bar-layout">
    <div class="tab-content">
      <router-view />
    </div>
    <van-tabbar route safe-area-inset-bottom>
      <van-tabbar-item
        v-for="tab in tabs"
        :key="tab.key"
        :icon="activeKey === tab.key ? tab.iconActive : tab.icon"
        @click="onTabChange(tab)"
      >
        {{ tab.label }}
      </van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped lang="scss">
.tab-bar-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.tab-content {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}
</style>
