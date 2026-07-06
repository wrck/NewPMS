/**
 * 应用 Store（Pinia）
 * 侧边栏折叠状态 / 面包屑 / 消息未读数 / 教程触发等全局 UI 状态
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface BreadcrumbItem {
  title: string
  path?: string
}

export const useAppStore = defineStore('app', () => {
  // ============ state ============
  /** 侧边栏是否折叠 */
  const sidebarCollapsed = ref<boolean>(false)
  /** 面包屑 */
  const breadcrumbs = ref<BreadcrumbItem[]>([])
  /** 消息未读数 */
  const unreadNoticeCount = ref<number>(0)
  /** 消息未读数（兼容别名，部分组件使用 unreadCount） */
  const unreadCount = ref<number>(0)
  /** 消息抽屉是否打开 */
  const noticeDrawerVisible = ref<boolean>(false)
  /** 消息抽屉是否打开（兼容别名，部分组件使用 messageDrawerVisible） */
  const messageDrawerVisible = ref<boolean>(false)
  /**
   * 教程触发计数器：每次 triggerTutorial() 自增，
   * BasicLayout 监听该值变化以重新打开 Tutorial 组件
   */
  const tutorialTrigger = ref<number>(0)
  /** 主题模式：light / dark（默认 light，后续可接入用户偏好持久化） */
  const themeMode = ref<'light' | 'dark'>('light')

  // ============ actions ============
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setSidebar(collapsed: boolean) {
    sidebarCollapsed.value = collapsed
  }

  function setBreadcrumbs(list: BreadcrumbItem[]) {
    breadcrumbs.value = list
  }

  function setUnreadNoticeCount(count: number) {
    unreadNoticeCount.value = Math.max(0, count)
  }

  /** 设置未读消息数（兼容别名，部分组件使用 setUnreadCount） */
  function setUnreadCount(count: number) {
    unreadCount.value = Math.max(0, count)
  }

  function openNoticeDrawer() {
    noticeDrawerVisible.value = true
  }

  function closeNoticeDrawer() {
    noticeDrawerVisible.value = false
  }

  /** 切换消息抽屉（兼容，部分组件使用 toggleMessageDrawer） */
  function toggleMessageDrawer(visible?: boolean) {
    messageDrawerVisible.value = visible !== undefined ? visible : !messageDrawerVisible.value
  }

  /** 触发新手教程重新打开 */
  function triggerTutorial() {
    tutorialTrigger.value++
  }

  /** 切换主题模式 */
  function setThemeMode(mode: 'light' | 'dark') {
    themeMode.value = mode
  }

  return {
    // state
    sidebarCollapsed,
    /** 兼容别名（旧组件使用 siderCollapsed） */
    siderCollapsed: sidebarCollapsed,
    breadcrumbs,
    unreadNoticeCount,
    unreadCount,
    noticeDrawerVisible,
    messageDrawerVisible,
    tutorialTrigger,
    themeMode,
    // actions
    toggleSidebar,
    /** 兼容别名（旧组件使用 toggleSider） */
    toggleSider: toggleSidebar,
    setSidebar,
    setBreadcrumbs,
    setUnreadNoticeCount,
    setUnreadCount,
    openNoticeDrawer,
    closeNoticeDrawer,
    toggleMessageDrawer,
    triggerTutorial,
    setThemeMode
  }
})
