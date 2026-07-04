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

// H5 路径前缀白名单（按前缀匹配，需手动检查 requireAuth）
const H5_PUBLIC_PREFIXES = [
  '/h5/customer/login',
  '/h5/customer/cutover/', // token 访问，无需登录
  '/h5/customer/acceptance/', // token 访问，无需登录
  '/h5/agent/login'
]

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

    // H5 公开路径前缀匹配（token 访问等）
    if (H5_PUBLIC_PREFIXES.some((prefix) => to.path.startsWith(prefix))) {
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

    // 未登录 → 跳登录页（携带 redirect，根据路径判断跳 PC 或 H5 登录页）
    if (!userStore.isLogin) {
      // H5 路径跳 H5 登录页，PC 路径跳 PC 登录页
      if (to.path.startsWith('/h5/customer')) {
        next({ path: '/h5/customer/login', query: { redirect: to.fullPath } })
      } else if (to.path.startsWith('/h5/agent')) {
        next({ path: '/h5/agent/login', query: { redirect: to.fullPath } })
      } else {
        next({ path: '/login', query: { redirect: to.fullPath } })
      }
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
