/**
 * 全局路由守卫
 * - 未登录跳 /login
 * - Token 校验（已登录但 userInfo 缺失时拉取）
 * - 角色权限校验
 * - 页面标题设置 / 面包屑更新
 */
import type { Router } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore, useAppStore } from '@/stores'

NProgress.configure({ showSpinner: false, easing: 'ease', speed: 300 })

// 白名单：无需登录可访问
const WHITE_LIST = ['/login', '/404']

export function setupRouterGuard(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    NProgress.start()

    // 设置页面标题
    const appTitle = import.meta.env.VITE_APP_TITLE || 'Vibe 交付管理平台'
    document.title = to.meta?.title ? `${to.meta.title} - ${appTitle}` : appTitle

    const userStore = useUserStore()
    const appStore = useAppStore()

    // 更新面包屑（基于 matched 路由）
    const breadcrumbs = to.matched
      .filter((r) => r.meta?.title)
      .map((r) => ({ title: r.meta!.title as string, path: r.path }))
    appStore.setBreadcrumbs(breadcrumbs)

    // 白名单放行
    if (WHITE_LIST.includes(to.path)) {
      // 已登录用户访问登录页 → 重定向到首页
      if (to.path === '/login' && userStore.isLogin) {
        next({ path: '/' })
        NProgress.done()
        return
      }
      next()
      NProgress.done()
      return
    }

    // requireAuth=false 的路由直接放行
    if (to.meta?.requireAuth === false) {
      next()
      NProgress.done()
      return
    }

    // 未登录 → 跳登录页（携带 redirect）
    if (!userStore.isLogin) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      NProgress.done()
      return
    }

    // 已登录但未拉取 userInfo → 拉取
    if (!userStore.userInfo) {
      try {
        await userStore.fetchUserInfo()
      } catch (e) {
        // 拉取失败（Token 失效等）→ 清空本地态并跳登录
        console.error('[guard] fetchUserInfo failed:', e)
        userStore.reset()
        next({ path: '/login', query: { redirect: to.fullPath } })
        NProgress.done()
        return
      }
    }

    // 角色权限校验
    if (to.meta?.roles && to.meta.roles.length > 0) {
      if (!userStore.hasAnyRole(to.meta.roles)) {
        // 无权限 → 跳 404（避免暴露路由存在性）
        next({ path: '/404' })
        NProgress.done()
        return
      }
    }

    next()
    NProgress.done()
  })

  router.afterEach(() => {
    NProgress.done()
  })

  router.onError((error) => {
    console.error('[router] error:', error)
    NProgress.done()
  })
}
