<template>
  <div class="h5-layout">
    <header class="h5-header">
      <div class="h5-header-left" @click="onBack" v-if="showBack">
        <LeftOutlined />
      </div>
      <div class="h5-header-left" v-else>
        <slot name="left" />
      </div>
      <div class="h5-header-title">{{ title }}</div>
      <div class="h5-header-right">
        <slot name="right" />
      </div>
    </header>
    <main class="h5-main">
      <slot />
    </main>
  </div>
</template>

<script lang="ts" setup>
import { LeftOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'

defineProps<{
  title: string
  showBack?: boolean
}>()

const router = useRouter()

function onBack() {
  // 优先使用浏览器历史回退（如果有上一页），否则跳到 H5 首页
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/h5/customer/projects')
  }
}
</script>

<style scoped>
.h5-layout {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.h5-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
  color: #fff;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  min-height: 48px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.h5-header-left {
  min-width: 32px;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.h5-header-title {
  flex: 1;
  text-align: center;
  font-size: 17px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.h5-header-right {
  min-width: 32px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.h5-main {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
}
</style>
