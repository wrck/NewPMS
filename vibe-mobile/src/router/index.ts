import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 移动端路由
 * Tab 页使用 TabBarLayout（首页/任务/现场/我的）
 * 子页面使用 NavBarLayout（带顶部返回栏）
 * 登录页无布局
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/TabBarLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'HomeIndex',
        component: () => import('@/views/home/index.vue'),
        meta: { title: '首页', tabKey: 'home', requiresAuth: true }
      },
      {
        path: 'task',
        name: 'TaskIndex',
        component: () => import('@/views/task/index.vue'),
        meta: { title: '任务', tabKey: 'task', requiresAuth: true }
      },
      {
        path: 'field',
        name: 'FieldIndex',
        component: () => import('@/views/field/index.vue'),
        meta: { title: '现场', tabKey: 'field', requiresAuth: true }
      },
      {
        path: 'mine',
        name: 'MineIndex',
        component: () => import('@/views/mine/index.vue'),
        meta: { title: '我的', tabKey: 'mine', requiresAuth: true }
      }
    ]
  },
  {
    path: '/task',
    component: () => import('@/layouts/NavBarLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'detail/:id',
        name: 'TaskDetail',
        component: () => import('@/views/task/detail.vue'),
        meta: { title: '任务详情', requiresAuth: true }
      }
    ]
  },
  {
    path: '/field',
    component: () => import('@/layouts/NavBarLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'checkin/:taskId',
        name: 'FieldCheckin',
        component: () => import('@/views/field/checkin.vue'),
        meta: { title: '现场签到', requiresAuth: true }
      },
      {
        path: 'steps/:taskId',
        name: 'FieldSteps',
        component: () => import('@/views/field/steps.vue'),
        meta: { title: '施工步骤', requiresAuth: true }
      },
      {
        path: 'issue/:taskId',
        name: 'FieldIssue',
        component: () => import('@/views/field/issue.vue'),
        meta: { title: '异常上报', requiresAuth: true }
      },
      {
        path: 'complete/:taskId',
        name: 'FieldComplete',
        component: () => import('@/views/field/complete.vue'),
        meta: { title: '完成确认', requiresAuth: true }
      }
    ]
  },
  {
    path: '/mine',
    component: () => import('@/layouts/NavBarLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'timesheet',
        name: 'MineTimesheet',
        component: () => import('@/views/mine/timesheet.vue'),
        meta: { title: '工时填报', requiresAuth: true }
      },
      {
        path: 'messages',
        name: 'MineMessages',
        component: () => import('@/views/mine/messages.vue'),
        meta: { title: '消息中心', requiresAuth: true }
      },
      {
        path: 'settings',
        name: 'MineSettings',
        component: () => import('@/views/mine/settings.vue'),
        meta: { title: '设置', requiresAuth: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

/** 全局前置守卫：Token 校验 */
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const title = (to.meta.title as string) || ''
  if (title) document.title = `${title} - 实施交付`

  if (to.meta.requiresAuth === false) {
    // 已登录用户访问登录页 → 跳首页
    if (to.name === 'Login' && userStore.isLoggedIn) {
      next({ path: '/home' })
      return
    }
    next()
    return
  }

  if (!userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }
  next()
})

export default router
