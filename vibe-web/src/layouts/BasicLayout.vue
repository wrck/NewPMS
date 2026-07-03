<script setup lang="ts">
/**
 * 全局布局（设计文档 3.2）
 * Header(56px) + Sider(220px可折叠至80px) + Content
 */
import { Layout, LayoutHeader, LayoutSider, LayoutContent } from 'ant-design-vue'
import { ConfigProvider, theme as antdTheme } from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import { themeConfig } from '@/styles/theme'
import AppHeader from './components/AppHeader.vue'
import AppSider from './components/AppSider.vue'
import MessageDrawer from './components/MessageDrawer.vue'
import { useAppStore } from '@/stores/app'

dayjs.locale('zh-cn')

const appStore = useAppStore()
</script>

<template>
  <ConfigProvider
    :locale="zhCN"
    :theme="{ ...themeConfig, algorithm: appStore.themeMode === 'dark' ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm }"
  >
    <Layout class="basic-layout">
      <AppHeader />
      <Layout class="basic-body">
        <AppSider />
        <Layout class="basic-content-layout">
          <LayoutContent class="basic-content">
            <router-view v-slot="{ Component, route }">
              <transition name="fade-slide" mode="out-in">
                <keep-alive>
                  <component :is="Component" :key="route.fullPath" />
                </keep-alive>
              </transition>
            </router-view>
          </LayoutContent>
        </Layout>
      </Layout>
      <MessageDrawer />
    </Layout>
  </ConfigProvider>
</template>

<style lang="less" scoped>
.basic-layout {
  min-height: 100vh;
}
.basic-body {
  flex: 1;
  min-height: 0;
}
.basic-content-layout {
  background: @bg-page;
}
.basic-content {
  overflow: auto;
  background: @bg-page;
}

// 路由切换动画
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.2s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
