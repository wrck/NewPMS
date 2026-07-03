<template>
  <div class="customer-layout">
    <header class="customer-layout__header">
      <van-icon
        v-if="showBack"
        name="arrow-left"
        size="20"
        class="customer-layout__back"
        @click="onBack"
      />
      <h1 class="customer-layout__title">{{ title }}</h1>
      <span v-if="isLogin" class="customer-layout__logout" @click="onLogout">退出</span>
      <span v-else class="customer-layout__placeholder" />
    </header>
    <main class="customer-layout__main">
      <router-view v-slot="{ Component }">
        <keep-alive :include="cacheViews">
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog } from 'vant'
import { useCustomerStore } from '@/stores/customer'

const route = useRoute()
const router = useRouter()
const customerStore = useCustomerStore()

const title = computed(() => (route.meta.title as string) || '项目进度')
const isLogin = computed(() => customerStore.isLogin)
const showBack = computed(() => route.path !== '/customer/progress')
const cacheViews = computed(() => ['CustomerProjectProgress', 'CustomerDocuments'])

function onBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/customer/progress')
  }
}

function onLogout() {
  showConfirmDialog({
    title: '提示',
    message: '确定退出登录吗？'
  })
    .then(async () => {
      await customerStore.logout()
      router.replace('/customer/login')
    })
    .catch(() => {})
}
</script>

<style lang="scss" scoped>
.customer-layout {
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
    background: #fff;
    border-bottom: 1px solid var(--border-color-light);
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: var(--color-text-primary);
  }

  &__back {
    position: absolute;
    left: 12px;
    top: 50%;
    transform: translateY(-50%);
    color: var(--color-text-regular);
    padding: 4px;
  }

  &__logout,
  &__placeholder {
    position: absolute;
    right: 12px;
    top: 50%;
    transform: translateY(-50%);
    font-size: 13px;
    color: var(--customer-primary);
    padding: 4px 6px;
  }

  &__main {
    flex: 1;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
  }
}
</style>
