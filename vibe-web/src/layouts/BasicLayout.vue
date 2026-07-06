<script setup lang="ts">
/**
 * 全局布局（设计文档 3.2）
 * Header(56px) + Sider(220px可折叠至80px) + Content
 *
 * 接入：
 *   - Tutorial 新手教程组件（首次登录自动触发 + appStore.tutorialTrigger 监听）
 *   - FeedbackButton 反馈悬浮按钮（所有登录用户可见）
 */
import { ref, watch, onMounted } from 'vue'
import { Layout, LayoutHeader, LayoutSider, LayoutContent } from 'ant-design-vue'
import { ConfigProvider, theme as antdTheme } from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import { themeConfig } from '@/styles/theme'
import AppHeader from './components/AppHeader.vue'
import AppSider from './components/AppSider.vue'
import MessageDrawer from './components/MessageDrawer.vue'
import Tutorial from '@/components/Onboarding/Tutorial.vue'
import FeedbackButton from '@/components/Feedback/FeedbackButton.vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

dayjs.locale('zh-cn')

const appStore = useAppStore()
const userStore = useUserStore()

const tutorialRef = ref<InstanceType<typeof Tutorial> | null>(null)
const showFeedbackButton = ref(false)

/** 监听 tutorialTrigger 计数器变化，触发教程打开 */
watch(
  () => appStore.tutorialTrigger,
  () => {
    if (tutorialRef.value) {
      tutorialRef.value.start()
    }
  }
)

/** 检测首次登录：未完成 onboarding 且已登录则自动触发教程 */
onMounted(() => {
  showFeedbackButton.value = userStore.isLogin
  const onboardingDone = localStorage.getItem('onboarding_done')
  if (userStore.isLogin && !onboardingDone && tutorialRef.value) {
    // 延迟 800ms 等待布局渲染完成后再打开教程
    setTimeout(() => {
      tutorialRef.value?.start()
    }, 800)
  }
})

/** 监听登录状态变化 */
watch(
  () => userStore.isLogin,
  (val) => {
    showFeedbackButton.value = val
  }
)
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
      <Tutorial ref="tutorialRef" />
      <FeedbackButton :visible="showFeedbackButton" />
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
