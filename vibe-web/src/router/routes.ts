/**
 * 路由元信息类型
 */
import type { RouteRecordRaw } from 'vue-router'
import type { RoleCode } from '@/types/user'

declare module 'vue-router' {
  interface RouteMeta {
    /** 页面标题 */
    title?: string
    /** 是否需要登录 */
    requireAuth?: boolean
    /** 允许访问的角色列表，为空表示任意已登录用户 */
    roles?: RoleCode[]
    /** 是否在菜单中隐藏 */
    hideInMenu?: boolean
    /** 菜单图标（@ant-design/icons-vue 组件名） */
    icon?: string
    /** 是否缓存组件 */
    keepAlive?: boolean
    /** 外链地址 */
    link?: string
    /** 所需权限码 */
    permission?: string
  }
}

/**
 * 路由配置
 * 与 menuConfig.ts 子菜单路径一一对应；详情页（hideInMenu=true）作为隐藏子路由
 */
export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requireAuth: false, hideInMenu: true }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', requireAuth: false, hideInMenu: true }
  },
  {
    path: '/',
    name: 'BasicLayout',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/dashboard',
    meta: { requireAuth: true },
    children: [
      /* ============ 工作台 ============ */
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'DashboardOutlined', requireAuth: true, keepAlive: true }
      },
      {
        path: '/dashboard/my-tasks',
        name: 'MyTasks',
        component: () => import('@/views/dashboard/my-tasks.vue'),
        meta: { title: '我的任务', requireAuth: true, keepAlive: true, hideInMenu: true }
      },
      {
        path: '/dashboard/my-messages',
        name: 'MyMessages',
        component: () => import('@/views/dashboard/my-messages.vue'),
        meta: { title: '我的消息', requireAuth: true, keepAlive: true, hideInMenu: true }
      },

      /* ============ 项目管理 ============ */
      {
        path: '/project',
        name: 'Project',
        redirect: '/project/list',
        meta: { title: '项目管理', icon: 'ProjectOutlined', requireAuth: true, permission: 'project:view' },
        children: [
          {
            path: 'list',
            name: 'ProjectList',
            component: () => import('@/views/project/list.vue'),
            meta: { title: '项目列表', requireAuth: true, keepAlive: true, permission: 'project:view' }
          },
          {
            path: 'detail/:id',
            name: 'ProjectDetail',
            component: () => import('@/views/project/detail.vue'),
            meta: { title: '项目详情', requireAuth: true, hideInMenu: true, permission: 'project:view' }
          },
          {
            path: 'task/:taskId',
            name: 'ProjectTaskDetail',
            component: () => import('@/views/project/task-detail.vue'),
            meta: { title: '任务详情', requireAuth: true, hideInMenu: true, permission: 'project:view' }
          },
          {
            path: 'template',
            name: 'ProjectTemplate',
            component: () => import('@/views/project/template.vue'),
            meta: { title: '项目模板', requireAuth: true, keepAlive: true, permission: 'project:view' }
          },
          {
            path: 'report',
            name: 'ProjectReport',
            component: () => import('@/views/report/project.vue'),
            meta: { title: '项目报表', requireAuth: true, keepAlive: true, permission: 'report:view' }
          }
        ]
      },

      /* ============ 设备资产 ============ */
      {
        path: '/device',
        name: 'Device',
        redirect: '/device/model',
        meta: { title: '设备资产', icon: 'HddOutlined', requireAuth: true, permission: 'device:view' },
        children: [
          {
            path: 'model',
            name: 'DeviceModel',
            component: () => import('@/views/device/model.vue'),
            meta: { title: '设备型号库', requireAuth: true, keepAlive: true, permission: 'device:view' }
          },
          {
            path: 'ledger',
            name: 'DeviceLedger',
            component: () => import('@/views/device/ledger.vue'),
            meta: { title: '设备台账', requireAuth: true, keepAlive: true, permission: 'device:view' }
          },
          {
            path: 'inout',
            name: 'DeviceInout',
            component: () => import('@/views/device/inout.vue'),
            meta: { title: '出入库管理', requireAuth: true, keepAlive: true, permission: 'device:view' }
          },
          {
            path: 'spare',
            name: 'DeviceSpare',
            component: () => import('@/views/device/spare.vue'),
            meta: { title: '备件管理', requireAuth: true, keepAlive: true, permission: 'device:view' }
          },
          {
            path: 'board',
            name: 'DeviceBoard',
            component: () => import('@/views/device/board.vue'),
            meta: { title: '设备看板', requireAuth: true, keepAlive: true, permission: 'device:view' }
          }
        ]
      },

      /* ============ 资源调度 ============ */
      {
        path: '/resource',
        name: 'Resource',
        redirect: '/resource/engineer',
        meta: { title: '资源调度', icon: 'TeamOutlined', requireAuth: true, permission: 'resource:view' },
        children: [
          {
            path: 'engineer',
            name: 'ResourceEngineer',
            component: () => import('@/views/resource/engineer.vue'),
            meta: { title: '工程师资源池', requireAuth: true, keepAlive: true, permission: 'resource:view' }
          },
          {
            path: 'schedule',
            name: 'ResourceSchedule',
            component: () => import('@/views/resource/schedule.vue'),
            meta: { title: '排期日历', requireAuth: true, keepAlive: true, permission: 'resource:view' }
          },
          {
            path: 'dispatch',
            name: 'ResourceDispatch',
            component: () => import('@/views/resource/dispatch.vue'),
            meta: { title: '任务派发', requireAuth: true, keepAlive: true, permission: 'resource:view' }
          },
          {
            path: 'timesheet',
            name: 'ResourceTimesheet',
            component: () => import('@/views/resource/timesheet.vue'),
            meta: { title: '工时管理', requireAuth: true, keepAlive: true, permission: 'resource:view' }
          }
        ]
      },

      /* ============ 交付管理 ============ */
      {
        path: '/delivery',
        name: 'Delivery',
        redirect: '/delivery/field',
        meta: { title: '交付管理', icon: 'CarOutlined', requireAuth: true, permission: 'delivery:view' },
        children: [
          {
            path: 'field',
            name: 'DeliveryField',
            component: () => import('@/views/delivery/field.vue'),
            meta: { title: '现场作业', requireAuth: true, keepAlive: true, permission: 'delivery:view' }
          },
          {
            path: 'board',
            name: 'DeliveryBoard',
            component: () => import('@/views/delivery/board.vue'),
            meta: { title: '交付看板', requireAuth: true, keepAlive: true, permission: 'delivery:view' }
          }
        ]
      },

      /* ============ 代理商 ============ */
      {
        path: '/agent',
        name: 'Agent',
        redirect: '/agent/profile',
        meta: { title: '代理商', icon: 'ApartmentOutlined', requireAuth: true, permission: 'agent:view' },
        children: [
          {
            path: 'profile',
            name: 'AgentProfile',
            component: () => import('@/views/agent/profile.vue'),
            meta: { title: '代理商档案', requireAuth: true, keepAlive: true, permission: 'agent:view' }
          },
          {
            path: 'outsource',
            name: 'AgentOutsource',
            component: () => import('@/views/agent/outsource.vue'),
            meta: { title: '转包任务', requireAuth: true, keepAlive: true, permission: 'agent:view' }
          },
          {
            path: 'review',
            name: 'AgentReview',
            component: () => import('@/views/agent/review.vue'),
            meta: { title: '交付审核', requireAuth: true, keepAlive: true, permission: 'agent:view' }
          },
          {
            path: 'settlement',
            name: 'AgentSettlement',
            component: () => import('@/views/agent/settlement.vue'),
            meta: { title: '结算管理', requireAuth: true, keepAlive: true, permission: 'agent:view' }
          }
        ]
      },

      /* ============ 报表中心 ============ */
      {
        path: '/report',
        name: 'Report',
        redirect: '/report/cockpit',
        meta: {
          title: '报表中心',
          icon: 'BarChartOutlined',
          requireAuth: true,
          keepAlive: true,
          roles: ['SUPER_ADMIN', 'DIRECTOR', 'PM', 'FINANCE']
        },
        children: [
          {
            path: 'cockpit',
            name: 'ReportCockpit',
            component: () => import('@/views/report/cockpit.vue'),
            meta: {
              title: '管理驾驶舱',
              requireAuth: true,
              keepAlive: true,
              roles: ['SUPER_ADMIN', 'DIRECTOR', 'PM', 'FINANCE']
            }
          },
          {
            path: 'project',
            name: 'ReportProject',
            component: () => import('@/views/report/project.vue'),
            meta: {
              title: '项目报表',
              requireAuth: true,
              keepAlive: true,
              roles: ['SUPER_ADMIN', 'DIRECTOR', 'PM', 'FINANCE']
            }
          }
        ]
      },

      /* ============ 系统管理 ============ */
      {
        path: '/system',
        name: 'System',
        redirect: '/system/user',
        meta: { title: '系统管理', icon: 'SettingOutlined', requireAuth: true, roles: ['SUPER_ADMIN'] },
        children: [
          {
            path: 'user',
            name: 'SystemUser',
            component: () => import('@/views/system/user.vue'),
            meta: { title: '用户管理', requireAuth: true, keepAlive: true, roles: ['SUPER_ADMIN'] }
          },
          {
            path: 'role',
            name: 'SystemRole',
            component: () => import('@/views/system/role.vue'),
            meta: { title: '角色权限', requireAuth: true, keepAlive: true, roles: ['SUPER_ADMIN'] }
          },
          {
            path: 'dict',
            name: 'SystemDict',
            component: () => import('@/views/system/dict.vue'),
            meta: { title: '数据字典', requireAuth: true, keepAlive: true, roles: ['SUPER_ADMIN'] }
          },
          {
            path: 'config',
            name: 'SystemConfig',
            component: () => import('@/views/system/config.vue'),
            meta: { title: '系统配置', requireAuth: true, keepAlive: true, roles: ['SUPER_ADMIN'] }
          },
          {
            path: 'log',
            name: 'SystemLog',
            component: () => import('@/views/system/log.vue'),
            meta: { title: '操作日志', requireAuth: true, keepAlive: true, roles: ['SUPER_ADMIN'] }
          }
        ]
      }
    ]
  },
  // 兜底：未匹配到的路由跳 404
  {
    path: '/:pathMatch(.*)*',
    name: 'CatchAll',
    redirect: '/404'
  }
]
