<script setup lang="ts">
/**
 * 左侧菜单（设计文档 3.2）
 * width: 220px（可折叠至 80px）
 * 按权限动态渲染菜单
 */
import { computed, ref, watch } from 'vue'
import { LayoutSider, Menu } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { menuConfig, type MenuItem } from '@/layouts/menuConfig'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

/** 按权限过滤菜单 */
function filterMenu(items: MenuItem[]): MenuItem[] {
  return items
    .filter((item) => {
      if (item.hidden) return false
      if (!item.permission) return true
      return userStore.isAdmin || userStore.hasPermission(item.permission)
    })
    .map((item) => ({
      ...item,
      children: item.children ? filterMenu(item.children) : undefined
    }))
    .filter((item) => !item.children || item.children.length > 0)
}

const filteredMenu = computed(() => filterMenu(menuConfig))

/** 选中菜单项 */
const selectedKeys = computed<string[]>(() => {
  // 优先匹配完整路径
  return [route.path]
})

/** 展开的父级菜单 key */
const openKeys = ref<string[]>([])

// 默认展开当前路径所属的顶层菜单
watch(
  () => route.path,
  (path) => {
    const top = menuConfig.find((m) => path.startsWith(m.path))
    if (top && !openKeys.value.includes(top.key)) {
      openKeys.value = [top.key]
    }
  },
  { immediate: true }
)

function handleClick({ key }: { key: string | number }) {
  const item = findItem(filteredMenu.value, String(key))
  if (item) {
    router.push(item.path)
  }
}

function findItem(items: MenuItem[], key: string): MenuItem | undefined {
  for (const item of items) {
    if (item.key === key) return item
    if (item.children) {
      const found = findItem(item.children, key)
      if (found) return found
    }
  }
  return undefined
}
</script>

<template>
  <LayoutSider
    v-model:collapsed="appStore.siderCollapsed"
    class="app-sider"
    :width="220"
    :collapsed-width="80"
    :trigger="null"
    collapsible
  >
    <Menu
      mode="inline"
      :selected-keys="selectedKeys"
      v-model:open-keys="openKeys"
      @click="handleClick"
    >
      <template v-for="item in filteredMenu" :key="item.key">
        <a-sub-menu v-if="item.children && item.children.length" :key="item.key">
          <template #title>
            <span class="menu-item-content">
              <component :is="item.icon" v-if="item.icon" />
              <span class="menu-label">{{ item.label }}</span>
            </span>
          </template>
          <a-menu-item v-for="child in item.children" :key="child.key">
            <span class="menu-item-content">
              <component :is="child.icon" v-if="child.icon" />
              <span class="menu-label">{{ child.label }}</span>
            </span>
          </a-menu-item>
        </a-sub-menu>
        <a-menu-item v-else :key="item.key">
          <span class="menu-item-content">
            <component :is="item.icon" v-if="item.icon" />
            <span class="menu-label">{{ item.label }}</span>
          </span>
        </a-menu-item>
      </template>
    </Menu>
  </LayoutSider>
</template>

<style lang="less" scoped>
.app-sider {
  background: @bg-container;
  border-right: 1px solid #f0f0f0;
  overflow: auto;
  height: calc(100vh - @header-height);
  position: sticky;
  top: @header-height;
}
.menu-item-content {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.menu-label {
  white-space: nowrap;
}
:deep(.ant-menu) {
  border-inline-end: none !important;
}
:deep(.ant-layout-sider-children) {
  padding: 8px 0;
}
</style>
