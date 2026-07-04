/**
 * Vue Router 实例
 */
import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './routes'
import { setupRouterGuard } from './guard'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL || '/'),
  routes,
  scrollBehavior: () => ({ left: 0, top: 0 })
})

// 安装全局前置守卫
setupRouterGuard(router)

/**
 * 重置路由到初始状态（清除动态添加的路由）
 * 用于退出登录时清理动态路由
 */
export function resetRouter() {
  const allRoutes = router.getRoutes()
  // 静态路由名称集合，这些路由不应被移除
  const staticRouteNames = new Set(
    routes.flatMap((r) => collectRouteNames(r))
  )
  for (const r of allRoutes) {
    if (r.name && !staticRouteNames.has(String(r.name))) {
      router.removeRoute(r.name)
    }
  }
}

/** 递归收集路由名称 */
function collectRouteNames(route: any): string[] {
  const names: string[] = []
  if (route.name) names.push(String(route.name))
  if (route.children && Array.isArray(route.children)) {
    for (const child of route.children) {
      names.push(...collectRouteNames(child))
    }
  }
  return names
}

export default router
