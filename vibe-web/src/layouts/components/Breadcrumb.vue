<script setup lang="ts">
/**
 * 面包屑导航（基于 appStore.breadcrumbs）
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HomeOutlined } from '@ant-design/icons-vue'
import { useAppStore } from '@/stores'

const appStore = useAppStore()
const route = useRoute()
const router = useRouter()

const items = computed(() => {
  const list = appStore.breadcrumbs.map((b) => ({
    title: b.title,
    path: b.path
  }))
  // 首部插入「首页」
  return [{ title: '首页', path: '/dashboard' }, ...list]
})

function go(path?: string) {
  if (!path) return
  // 不重复跳转当前页
  if (path === route.path) return
  router.push(path)
}
</script>

<template>
  <a-breadcrumb separator=">">
    <a-breadcrumb-item
      v-for="(item, idx) in items"
      :key="idx"
      class="breadcrumb-item"
      @click="go(item.path)"
    >
      <template #default>
        <span class="crumb-link" :class="{ active: idx === items.length - 1 }">
          <HomeOutlined v-if="idx === 0" />
          <span class="crumb-text">{{ item.title }}</span>
        </span>
      </template>
    </a-breadcrumb-item>
  </a-breadcrumb>
</template>

<style scoped lang="less">
.breadcrumb-item {
  cursor: pointer;

  .crumb-link {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: @text-auxiliary;
    transition: color 0.2s;

    &:hover {
      color: @brand-primary;
    }

    &.active {
      color: @text-primary;
      font-weight: 500;
      cursor: default;
    }
  }
}
</style>
