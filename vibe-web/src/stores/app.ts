/**
 * 应用 Store（Pinia）
 * 侧边栏折叠状态 / 面包屑 / 消息未读数等全局 UI 状态
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
  /** 消息抽屉是否打开 */
  const noticeDrawerVisible = ref<boolean>(false)

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

  function openNoticeDrawer() {
    noticeDrawerVisible.value = true
  }

  function closeNoticeDrawer() {
    noticeDrawerVisible.value = false
  }

  return {
    // state
    sidebarCollapsed,
    breadcrumbs,
    unreadNoticeCount,
    noticeDrawerVisible,
    // actions
    toggleSidebar,
    setSidebar,
    setBreadcrumbs,
    setUnreadNoticeCount,
    openNoticeDrawer,
    closeNoticeDrawer
  }
})
