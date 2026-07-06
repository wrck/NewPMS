import type { Component } from 'vue'
import {
  DashboardOutlined,
  ProjectOutlined,
  HddOutlined,
  TeamOutlined,
  CarOutlined,
  ApartmentOutlined,
  CheckCircleOutlined,
  MoneyCollectOutlined,
  BarChartOutlined,
  SettingOutlined,
  ApiOutlined,
  FormOutlined
} from '@ant-design/icons-vue'

/** 菜单项 */
export interface MenuItem {
  /** 唯一 key */
  key: string
  /** 显示名称 */
  label: string
  /** 路由路径 */
  path: string
  /** 图标组件 */
  icon?: Component
  /** 所需权限码（任一即可），为空表示不限制 */
  permission?: string
  /** 子菜单 */
  children?: MenuItem[]
  /** 是否在菜单中隐藏（详情页等） */
  hidden?: boolean
}

/**
 * 菜单结构（设计文档 3.2）
 * 不同角色登录后菜单不同（根据权限动态渲染）
 */
export const menuConfig: MenuItem[] = [
  {
    key: 'dashboard',
    label: '工作台',
    path: '/dashboard',
    icon: DashboardOutlined,
    children: [
      { key: 'dashboard-home', label: '我的工作台', path: '/dashboard' },
      { key: 'my-tasks', label: '我的任务', path: '/dashboard/my-tasks' },
      { key: 'my-messages', label: '我的消息', path: '/dashboard/my-messages' }
    ]
  },
  {
    key: 'project',
    label: '项目管理',
    path: '/project',
    icon: ProjectOutlined,
    permission: 'project:view',
    children: [
      { key: 'project-list', label: '项目列表', path: '/project/list' },
      { key: 'project-customer', label: '客户档案', path: '/project/customer' },
      { key: 'project-template', label: '项目模板', path: '/project/template' },
      { key: 'project-report', label: '项目报表', path: '/project/report' }
    ]
  },
  {
    key: 'device',
    label: '设备资产',
    path: '/device',
    icon: HddOutlined,
    permission: 'device:view',
    children: [
      { key: 'device-model', label: '设备型号库', path: '/device/model' },
      { key: 'device-ledger', label: '设备台账', path: '/device/ledger' },
      { key: 'device-inout', label: '出入库管理', path: '/device/inout' },
      { key: 'device-provision', label: '设备预配', path: '/device/provision' },
      { key: 'device-spare', label: '备件管理', path: '/device/spare' },
      { key: 'device-warehouse', label: '仓库管理', path: '/device/warehouse' },
      { key: 'device-board', label: '设备看板', path: '/device/board' }
    ]
  },
  {
    key: 'resource',
    label: '资源调度',
    path: '/resource',
    icon: TeamOutlined,
    permission: 'resource:view',
    children: [
      { key: 'resource-engineer', label: '工程师资源池', path: '/resource/engineer' },
      { key: 'resource-schedule', label: '排期日历', path: '/resource/schedule' },
      { key: 'resource-dispatch', label: '任务派发', path: '/resource/dispatch' },
      { key: 'resource-timesheet', label: '工时管理', path: '/resource/timesheet' },
      { key: 'resource-business-trip', label: '差旅管理', path: '/resource/business-trip' },
      { key: 'resource-leave', label: '请假管理', path: '/resource/leave' }
    ]
  },
  {
    key: 'delivery',
    label: '交付管理',
    path: '/delivery',
    icon: CarOutlined,
    permission: 'delivery:view',
    children: [
      { key: 'delivery-field', label: '现场作业', path: '/delivery/field' },
      { key: 'delivery-cutover', label: '割接管理', path: '/delivery/cutover' },
      { key: 'delivery-board', label: '交付看板', path: '/delivery/board' }
    ]
  },
  {
    key: 'agent',
    label: '代理商',
    path: '/agent',
    icon: ApartmentOutlined,
    permission: 'agent:view',
    children: [
      { key: 'agent-profile', label: '代理商档案', path: '/agent/profile' },
      { key: 'agent-outsource', label: '转包任务', path: '/agent/outsource' },
      { key: 'agent-review', label: '交付审核', path: '/agent/review' },
      { key: 'agent-settlement', label: '结算管理', path: '/agent/settlement' }
    ]
  },
  {
    key: 'acceptance',
    label: '验收管理',
    path: '/acceptance',
    icon: CheckCircleOutlined,
    permission: 'acceptance:view',
    children: [
      { key: 'acceptance-standard', label: '验收标准', path: '/acceptance/standard' },
      { key: 'acceptance-task', label: '验收任务', path: '/acceptance/task' },
      { key: 'acceptance-issue', label: '遗留问题', path: '/acceptance/issue' },
      { key: 'acceptance-doc', label: '竣工文档', path: '/acceptance/doc' }
    ]
  },
  {
    key: 'finance',
    label: '财务核算',
    path: '/finance',
    icon: MoneyCollectOutlined,
    permission: 'finance:view',
    children: [
      { key: 'finance-budget', label: '项目预算', path: '/finance/budget' },
      { key: 'finance-cost', label: '成本归集', path: '/finance/cost' },
      { key: 'finance-agent', label: '代理商结算', path: '/finance/agent' },
      { key: 'finance-profit', label: '利润分析', path: '/finance/profit' }
    ]
  },
  {
    key: 'report',
    label: '报表中心',
    path: '/report',
    icon: BarChartOutlined,
    permission: 'report:view',
    children: [
      { key: 'report-cockpit', label: '管理驾驶舱', path: '/report/cockpit' },
      { key: 'report-project', label: '项目报表', path: '/report/project' },
      { key: 'report-device', label: '设备报表', path: '/report/device' },
      { key: 'report-resource', label: '资源报表', path: '/report/resource' },
      { key: 'report-finance', label: '财务报表', path: '/report/finance' }
    ]
  },
  {
    key: 'system',
    label: '系统管理',
    path: '/system',
    icon: SettingOutlined,
    permission: 'system:view',
    children: [
      { key: 'system-user', label: '用户管理', path: '/system/user' },
      { key: 'system-role', label: '角色权限', path: '/system/role' },
      { key: 'system-org', label: '组织架构', path: '/system/org' },
      { key: 'system-position', label: '岗位管理', path: '/system/position' },
      { key: 'system-dict', label: '数据字典', path: '/system/dict' },
      { key: 'system-config', label: '系统配置', path: '/system/config' },
      { key: 'system-notice-template', label: '通知模板', path: '/system/notice-template' },
      { key: 'system-notice', label: '站内信', path: '/system/notice' },
      { key: 'system-feedback', label: '反馈管理', path: '/system/feedback' },
      { key: 'system-log', label: '操作日志', path: '/system/log' },
      { key: 'system-login-log', label: '登录日志', path: '/system/login-log' },
      { key: 'integration-config', label: '集成配置', path: '/integration/config' },
      { key: 'integration-call-log', label: '集成调用日志', path: '/integration/call-log' }
    ]
  },
  {
    key: 'lowcode',
    label: '低代码配置',
    path: '/lowcode',
    icon: FormOutlined,
    permission: 'lowcode:config:view',
    children: [
      { key: 'lowcode-form', label: '表单配置', path: '/lowcode/form' },
      { key: 'lowcode-list', label: '列表配置', path: '/lowcode/list' },
      { key: 'lowcode-tab', label: '标签页配置', path: '/lowcode/tab' },
      { key: 'lowcode-relation', label: '关联页配置', path: '/lowcode/relation' },
      { key: 'lowcode-template', label: '模板库', path: '/lowcode/template' }
    ]
  }
]
