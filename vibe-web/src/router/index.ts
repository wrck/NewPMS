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

export default router
