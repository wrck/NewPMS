/**
 * 路由配置 - 外部入口 H5
 * - 客户入口 /customer/* 与代理商入口 /agent/* 路由分组独立
 * - 各自独立登录页与守卫
 * - 根路径自动重定向到客户入口
 */
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useCustomerStore } from '@/stores/customer'
import { useAgentStore } from '@/stores/agent'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/customer/login'
  },

  /* ===================== 客户入口 ===================== */
  {
    path: '/customer/login',
    name: 'CustomerLogin',
    component: () => import('@/views/customer/login.vue'),
    meta: { title: '客户登录', requiresAuth: false, portal: 'customer' }
  },
  {
    path: '/customer',
    component: () => import('@/layouts/CustomerLayout.vue'),
    meta: { requiresAuth: true, portal: 'customer' },
    redirect: '/customer/progress',
    children: [
      {
        path: 'progress',
        name: 'CustomerProjectProgress',
        component: () => import('@/views/customer/project-progress.vue'),
        meta: { title: '项目进度', requiresAuth: true, portal: 'customer' }
      },
      {
        path: 'documents',
        name: 'CustomerDocuments',
        component: () => import('@/views/customer/documents.vue'),
        meta: { title: '项目文档', requiresAuth: true, portal: 'customer' }
      }
    ]
  },

  /* ===================== 代理商入口 ===================== */
  {
    path: '/agent/login',
    name: 'AgentLogin',
    component: () => import('@/views/agent/login.vue'),
    meta: { title: '代理商登录', requiresAuth: false, portal: 'agent' }
  },
  {
    path: '/agent',
    component: () => import('@/layouts/AgentLayout.vue'),
    meta: { requiresAuth: true, portal: 'agent' },
    redirect: '/agent/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'AgentDashboard',
        component: () => import('@/views/agent/dashboard.vue'),
        meta: { title: '工作台', requiresAuth: true, portal: 'agent' }
      },
      {
        path: 'tasks',
        name: 'AgentTaskList',
        component: () => import('@/views/agent/task-list.vue'),
        meta: { title: '任务列表', requiresAuth: true, portal: 'agent' }
      },
      {
        path: 'mine',
        name: 'AgentMine',
        component: () => import('@/views/agent/mine.vue'),
        meta: { title: '我的', requiresAuth: true, portal: 'agent' }
      },
      {
        path: 'tasks/:id',
        name: 'AgentTaskDetail',
        component: () => import('@/views/agent/task-detail.vue'),
        meta: { title: '任务详情', requiresAuth: true, portal: 'agent' }
      },
      {
        path: 'tasks/:id/submit',
        name: 'AgentSubmitDeliverable',
        component: () => import('@/views/agent/submit-deliverable.vue'),
        meta: { title: '提交交付物', requiresAuth: true, portal: 'agent' }
      },
      {
        path: 'settlement',
        name: 'AgentSettlement',
        component: () => import('@/views/agent/settlement.vue'),
        meta: { title: '结算查看', requiresAuth: true, portal: 'agent' }
      }
    ]
  },

  {
    path: '/:pathMatch(.*)*',
    redirect: '/customer/login'
  }
]

const router = createRouter({
  history: createWebHistory('/portal/'),
  routes,
  scrollBehavior(to, from, savedPosition) {
    return savedPosition || { top: 0 }
  }
})

/** 全局前置守卫 */
router.beforeEach((to, _from, next) => {
  // 设置标题
  document.title = (to.meta.title as string) || import.meta.env.VITE_APP_TITLE || '实施交付'

  const portal = to.meta.portal as 'customer' | 'agent' | undefined

  // 不需要鉴权的页面（登录页）
  if (to.meta.requiresAuth === false) {
    // 已登录访问对应入口登录页，重定向到该入口首页
    if (portal === 'customer') {
      const customerStore = useCustomerStore()
      if (to.name === 'CustomerLogin' && customerStore.isLogin) {
        next({ path: '/customer/progress' })
        return
      }
    } else if (portal === 'agent') {
      const agentStore = useAgentStore()
      if (to.name === 'AgentLogin' && agentStore.isLogin) {
        next({ path: '/agent/dashboard' })
        return
      }
    }
    next()
    return
  }

  // 需要鉴权的页面，按入口校验
  if (portal === 'customer') {
    const customerStore = useCustomerStore()
    if (!customerStore.isLogin) {
      const redirect = encodeURIComponent(to.fullPath)
      next({ path: '/customer/login', query: { redirect } })
      return
    }
  } else if (portal === 'agent') {
    const agentStore = useAgentStore()
    if (!agentStore.isLogin) {
      const redirect = encodeURIComponent(to.fullPath)
      next({ path: '/agent/login', query: { redirect } })
      return
    }
  }

  next()
})

export default router
