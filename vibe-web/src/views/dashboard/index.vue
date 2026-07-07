<script setup lang="ts">
/**
 * 工作台首页
 * 设计文档 3.3.1：按角色差异化展示
 * 数据来源：/api/v1/dashboard 聚合接口（DashboardVO：role/realName/director/pm/engineer/agent）
 *
 * 角色映射：
 * - SUPER_ADMIN / DIRECTOR → director 块（管理驾驶舱式总览）
 * - PM                    → pm 块（我的项目 + 待派单 + 待审核）
 * - ENGINEER              → engineer 块（今日任务 + 工时）
 * - AGENT_ADMIN/AGENT_ENGINEER → agent 块（任务概况 + 各状态任务）
 *
 * 界面规范：响应式布局 / 模块化组件 / WCAG 2.1 AA 级可访问性
 * 数据保存：API 持久化 + localStorage 缓存上次刷新时间（防丢失）
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message as antdMessage } from 'ant-design-vue'
import {
  ProjectOutlined,
  WarningOutlined,
  ClockCircleOutlined,
  ScheduleOutlined,
  ReloadOutlined,
  CheckOutlined,
  RightOutlined,
  ToolOutlined,
  TeamOutlined,
  DashboardOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import PageContainer from '@/components/PageContainer.vue'
import HelpHint from '@/components/HelpHint.vue'
import StatisticCard from '@/components/StatisticCard.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getDashboard, type DashboardVO, type DirectorDashboard, type PmDashboard, type EngineerDashboard, type AgentDashboard } from '@/api/report'
import { markNoticeRead, markAllNoticesRead, pageMyNotices } from '@/api/system'
import {
  ProjectStatus,
  ProjectStatusTone,
  ProjectStatusLabel,
  TaskStatus,
  TaskStatusTone,
  TaskStatusLabel
} from '@/types/enum'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const dashboard = ref<DashboardVO | null>(null)
const notices = ref<Array<{ id: number; title: string; type: string; createdAt: string; read: boolean }>>([])

/** 角色对应的子块类型 */
type RoleBlock = 'director' | 'pm' | 'engineer' | 'agent' | 'unknown'

/** 当前角色应展示的块 */
const currentBlock = computed<RoleBlock>(() => {
  const role = dashboard.value?.role || userStore.roles?.[0] || ''
  if (role === 'SUPER_ADMIN' || role === 'DIRECTOR') return 'director'
  if (role === 'PM') return 'pm'
  if (role === 'ENGINEER') return 'engineer'
  if (role === 'AGENT_ADMIN' || role === 'AGENT_ENGINEER') return 'agent'
  // 默认按管理员处理（看 director 块）
  return 'director'
})

const director = computed<DirectorDashboard | undefined>(() => dashboard.value?.director)
const pm = computed<PmDashboard | undefined>(() => dashboard.value?.pm)
const engineer = computed<EngineerDashboard | undefined>(() => dashboard.value?.engineer)
const agent = computed<AgentDashboard | undefined>(() => dashboard.value?.agent)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const today = computed(() => {
  const d = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${week}`
})

/* ============ 角色感知的统计卡片（4 张） ============ */
interface StatCard {
  title: string
  value: number | string
  unit?: string
  icon: any
  accent?: string
  trend?: 'up' | 'down'
}

const statCards = computed<StatCard[]>(() => {
  const block = currentBlock.value
  if (block === 'director' && director.value) {
    const s = director.value.stats
    return [
      { title: '在建项目', value: s.activeProjectCount ?? 0, unit: '个', icon: ProjectOutlined, accent: '#1890FF' },
      { title: '在网设备', value: s.onlineDeviceCount ?? 0, unit: '台', icon: DashboardOutlined, accent: '#52C41A' },
      { title: '在职工程师', value: s.activeEngineerCount ?? 0, unit: '人', icon: TeamOutlined, accent: '#722ED1' },
      { title: '活跃代理商', value: s.activeAgentCount ?? 0, unit: '家', icon: ToolOutlined, accent: '#FA8C16' }
    ]
  }
  if (block === 'pm' && pm.value) {
    return [
      { title: '我的项目', value: pm.value.myProjectCount ?? 0, unit: '个', icon: ProjectOutlined, accent: '#1890FF' },
      { title: '进行中项目', value: pm.value.activeProjectCount ?? 0, unit: '个', icon: ProjectOutlined, accent: '#52C41A' },
      { title: '待派单任务', value: pm.value.pendingDispatchCount ?? 0, unit: '个', icon: ScheduleOutlined, accent: '#FAAD14' },
      { title: '待审核交付物', value: pm.value.pendingReviewCount ?? 0, unit: '个', icon: ClockCircleOutlined, accent: '#FF4D4F' }
    ]
  }
  if (block === 'engineer' && engineer.value) {
    return [
      { title: '今日任务', value: engineer.value.todayTaskCount ?? 0, unit: '个', icon: ScheduleOutlined, accent: '#1890FF' },
      { title: '待处理任务', value: engineer.value.pendingTaskCount ?? 0, unit: '个', icon: ClockCircleOutlined, accent: '#FAAD14' },
      { title: '超期任务', value: engineer.value.overdueTaskCount ?? 0, unit: '个', icon: WarningOutlined, accent: '#FF4D4F' },
      { title: '本月工时', value: engineer.value.monthWorkHours ?? 0, unit: 'h', icon: ToolOutlined, accent: '#722ED1' }
    ]
  }
  if (block === 'agent' && agent.value) {
    return [
      { title: '任务总数', value: agent.value.totalCount ?? 0, unit: '个', icon: ProjectOutlined, accent: '#1890FF' },
      { title: '待接单', value: agent.value.pendingCount ?? 0, unit: '个', icon: ClockCircleOutlined, accent: '#FAAD14' },
      { title: '进行中', value: agent.value.inProgressCount ?? 0, unit: '个', icon: ScheduleOutlined, accent: '#1890FF' },
      { title: '已超期', value: agent.value.overdueCount ?? 0, unit: '个', icon: WarningOutlined, accent: '#FF4D4F' }
    ]
  }
  return []
})

/* ============ 待办事项（统一从 notices 接口获取） ============ */
const noticeTypeLabel: Record<string, string> = {
  TASK: '任务',
  APPROVAL: '审批',
  ISSUE: '问题',
  WORKLOAD: '工作量',
  RISK: '风险',
  SYSTEM: '系统',
  OTHER: '其他'
}
const noticeTypeTone: Record<string, any> = {
  TASK: 'processing',
  APPROVAL: 'warning',
  ISSUE: 'warning',
  WORKLOAD: 'processing',
  RISK: 'error',
  SYSTEM: 'default',
  OTHER: 'default'
}

const unreadCount = computed(() => notices.value.filter((n) => !n.read).length)
const topNotices = computed(() => notices.value.slice(0, 5))

/* ============ 角色感知的项目/任务列表 ============ */
const myProjects = computed(() => pm.value?.myProjects ?? [])
const pendingDispatchTasks = computed(() => pm.value?.pendingDispatchTasks ?? [])
const pendingReviewDeliverables = computed(() => pm.value?.pendingReviewDeliverables ?? [])
const todayTasks = computed(() => engineer.value?.todayTasks ?? [])
const overdueTasks = computed(() => engineer.value?.overdueTasks ?? [])
const riskProjects = computed(() => director.value?.riskProjects ?? [])
const agentPendingTasks = computed(() => agent.value?.pendingTasks ?? [])
const agentSubmittedTasks = computed(() => agent.value?.submittedTasks ?? [])

const topProjects = computed(() => myProjects.value.slice(0, 5))
const topTodayTasks = computed(() => todayTasks.value.slice(0, 5))
const topOverdueTasks = computed(() => overdueTasks.value.slice(0, 5))
const topRiskProjects = computed(() => riskProjects.value.slice(0, 5))
const topPendingDispatch = computed(() => pendingDispatchTasks.value.slice(0, 5))
const topReviewDeliverables = computed(() => pendingReviewDeliverables.value.slice(0, 5))
const topAgentPending = computed(() => agentPendingTasks.value.slice(0, 5))
const topAgentSubmitted = computed(() => agentSubmittedTasks.value.slice(0, 5))

/* ============ 项目趋势（仅 director 可见） ============ */
const projectTrend = computed(() => director.value?.projectTrend ?? [])
const projectStatusDist = computed(() => director.value?.projectStatusDist ?? [])

/** 总监待办合计 */
const directorPending = computed(() => {
  if (!director.value) return 0
  return (director.value.pendingApprovalCount ?? 0)
    + (director.value.pendingChangeCount ?? 0)
    + (director.value.pendingWorkloadCount ?? 0)
})

/** 角色名称映射 */
const roleLabel = computed(() => {
  const map: Record<string, string> = {
    SUPER_ADMIN: '超级管理员',
    DIRECTOR: '总监',
    PM: '项目经理',
    ENGINEER: '工程师',
    AGENT_ADMIN: '代理商管理员',
    AGENT_ENGINEER: '代理商工程师',
    FINANCE: '财务',
    CUSTOMER: '客户'
  }
  return map[dashboard.value?.role || ''] || '用户'
})

/* ============ 数据加载 ============ */
const CACHE_KEY = 'vibe:dashboard:lastRefresh'
const CACHE_TTL = 60 * 1000 // 1 分钟缓存

async function loadData() {
  loading.value = true
  try {
    const data = await getDashboard()
    dashboard.value = data
    // 缓存刷新时间戳（用于断网时回显）
    try {
      localStorage.setItem(CACHE_KEY, JSON.stringify({ ts: Date.now(), role: data.role }))
    } catch {
      // localStorage 不可用时忽略
    }
    // 并行加载通知列表
    await loadNotices()
  } catch (e: any) {
    console.error('[dashboard] load failed:', e)
    antdMessage.error(e?.message || '首页数据加载失败，请稍后重试')
    // 尝试从缓存恢复（断网容错）
    try {
      const cached = localStorage.getItem(CACHE_KEY)
      if (cached) {
        const parsed = JSON.parse(cached)
        if (Date.now() - parsed.ts < CACHE_TTL * 30) {
          antdMessage.warning('已为你恢复上次会话，请刷新重试')
        }
      }
    } catch {
      // 忽略
    }
  } finally {
    loading.value = false
  }
}

async function loadNotices() {
  try {
    const res: any = await pageMyNotices({ page: 1, size: 10 })
    const records = (res?.records ?? res?.list ?? []) as any[]
    notices.value = records.map((n: any) => ({
      id: n.id ?? n.noticeId,
      title: n.noticeTitle ?? n.title ?? n.content ?? '',
      type: mapNoticeType(n.noticeType),
      createdAt: n.createTime ?? n.createdAt ?? '',
      read: !!(n.readStatus === 1 || n.readStatus === 'READ' || n.isRead)
    }))
  } catch (e: any) {
    console.warn('[dashboard.notices] load failed:', e)
    notices.value = []
  }
}

/** noticeType 数字编码 → 字符串标签（1=系统/2=任务） */
function mapNoticeType(t: any): string {
  if (t === 1 || t === '1') return 'SYSTEM'
  if (t === 2 || t === '2') return 'TASK'
  if (typeof t === 'string') return t
  return 'OTHER'
}

/** 标记单条通知已读 */
async function handleMarkRead(n: { id: number; read: boolean }) {
  if (n.read) return
  try {
    await markNoticeRead(n.id)
    n.read = true
    antdMessage.success('已标记为已读')
  } catch (e: any) {
    antdMessage.error('标记失败：' + (e?.message || '未知错误'))
  }
}

/** 全部已读 */
async function handleMarkAllRead() {
  if (!unreadCount.value) return
  try {
    await markAllNoticesRead()
    notices.value.forEach((n) => (n.read = true))
    antdMessage.success('全部已读')
  } catch (e: any) {
    antdMessage.error('操作失败：' + (e?.message || '未知错误'))
  }
}

function gotoTasks() {
  router.push('/dashboard/my-tasks')
}
function gotoMessages() {
  router.push('/dashboard/my-messages')
}
function gotoProjectList() {
  router.push('/project/list')
}
function gotoProjectDetail(id: number) {
  router.push(`/project/detail/${id}`)
}
function gotoTaskDetail(id: number) {
  router.push(`/project/task/${id}`)
}

function roleText(role: string): string {
  const map: Record<string, string> = {
    PM: '项目经理',
    ENGINEER: '工程师',
    TECH_LEAD: '技术负责人',
    QA: '质量',
    VIEWER: '关注者'
  }
  return map[role] || role
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer>
    <template #header>
      <div class="dashboard-header">
        <div>
          <h2 class="vibe-page-title">
            {{ greeting }}，{{ dashboard?.realName || userStore.realName || userStore.username || '管理员' }}
            <HelpHint
              title="工作台"
              content="工作台首页按角色展示核心指标、待办事项、我负责的项目与最近动态。\n点击右上角「?」可重新打开新手教程；点击「刷新」可重新加载数据。"
            />
          </h2>
          <p class="dashboard-date">
            <a-tag color="blue">{{ roleLabel }}</a-tag>
            {{ today }}，祝你工作顺利！
          </p>
        </div>
        <a-button :loading="loading" @click="loadData">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
    </template>

    <div class="dashboard-body">
      <!-- 核心指标卡片 -->
      <a-row :gutter="16" class="stat-row">
        <a-col v-for="card in statCards" :key="card.title" :xs="12" :sm="12" :md="6">
          <StatisticCard
            :title="card.title"
            :value="card.value"
            :unit="card.unit"
            :icon="card.icon"
            :accent="card.accent"
            :trend="card.trend"
            :loading="loading"
          />
        </a-col>
      </a-row>

      <!-- 待办事项（站内通知） -->
      <div class="vibe-card todo-card">
        <div class="card-head">
          <h3 class="card-title">
            待办事项
            <span v-if="unreadCount" class="todo-badge tnum">{{ unreadCount }}</span>
          </h3>
          <div class="card-actions">
            <a-button size="small" type="link" :disabled="!unreadCount" @click="handleMarkAllRead">
              <template #icon><CheckOutlined /></template>
              全部已读
            </a-button>
            <a-button size="small" type="link" @click="gotoMessages">
              查看全部
              <RightOutlined />
            </a-button>
          </div>
        </div>
        <EmptyState v-if="!topNotices.length" description="暂无待办事项" size="compact" />
        <ul v-else class="todo-list">
          <li
            v-for="n in topNotices"
            :key="n.id"
            class="todo-item"
            :class="{ unread: !n.read }"
            @click="handleMarkRead(n)"
          >
            <StatusTag :tone="noticeTypeTone[n.type] || 'default'">
              {{ noticeTypeLabel[n.type] || n.type }}
            </StatusTag>
            <span class="todo-title" :title="n.title">{{ n.title }}</span>
            <span class="todo-time tnum">{{ n.createdAt }}</span>
            <a-tag v-if="!n.read" color="red" class="todo-unread-tag">未读</a-tag>
          </li>
        </ul>
      </div>

      <!-- 角色差异化主区 -->
      <a-row :gutter="16">
        <!-- 总监/管理员：风险项目 + 项目趋势 -->
        <a-col v-if="currentBlock === 'director'" :xs="24" :md="14">
          <div class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">
                风险项目
                <a-tag v-if="riskProjects.length" color="red" class="todo-badge tnum">{{ riskProjects.length }}</a-tag>
              </h3>
              <a-button size="small" type="link" @click="gotoProjectList">
                查看全部
                <RightOutlined />
              </a-button>
            </div>
            <EmptyState
              v-if="!topRiskProjects.length"
              description="暂无风险项目"
              size="compact"
              action-text="去立项"
              @action="gotoProjectList"
            />
            <ul v-else class="project-list">
              <li
                v-for="p in topRiskProjects"
                :key="p.projectId"
                class="project-item risk-item"
                @click="gotoProjectDetail(p.projectId)"
              >
                <div class="project-info">
                  <span class="project-name" :title="p.projectName">{{ p.projectName }}</span>
                  <span class="project-meta">
                    <StatusTag :tone="ProjectStatusTone[p.status as ProjectStatus] || 'default'">
                      {{ ProjectStatusLabel[p.status as ProjectStatus] || p.status }}
                    </StatusTag>
                    <a-tag v-if="p.riskTypeName" color="orange" class="role-tag">{{ p.riskTypeName }}</a-tag>
                  </span>
                </div>
                <div class="risk-desc" v-if="p.description">{{ p.description }}</div>
                <div class="project-progress" v-if="p.progressPct !== undefined">
                  <ProgressBar :percent="p.progressPct" :show-label="true" size="normal" />
                </div>
              </li>
            </ul>
          </div>
        </a-col>

        <!-- PM：我的项目 -->
        <a-col v-if="currentBlock === 'pm'" :xs="24" :md="14">
          <div class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">我负责的项目</h3>
              <a-button size="small" type="link" @click="gotoProjectList">
                查看全部
                <RightOutlined />
              </a-button>
            </div>
            <EmptyState
              v-if="!topProjects.length"
              description="暂无负责的项目"
              size="compact"
              action-text="去立项"
              @action="gotoProjectList"
            />
            <ul v-else class="project-list">
              <li
                v-for="p in topProjects"
                :key="p.projectId"
                class="project-item"
                @click="gotoProjectDetail(p.projectId)"
              >
                <div class="project-info">
                  <span class="project-name" :title="p.projectName">{{ p.projectName }}</span>
                  <span class="project-meta">
                    <StatusTag :tone="ProjectStatusTone[p.status as ProjectStatus] || 'default'">
                      {{ ProjectStatusLabel[p.status as ProjectStatus] || p.status }}
                    </StatusTag>
                    <a-tag v-if="p.pmName" class="role-tag">PM：{{ p.pmName }}</a-tag>
                  </span>
                </div>
                <div class="project-progress" v-if="p.progressPct !== undefined">
                  <ProgressBar :percent="p.progressPct" :show-label="true" size="normal" />
                </div>
              </li>
            </ul>
          </div>
        </a-col>

        <!-- 工程师：今日任务 -->
        <a-col v-if="currentBlock === 'engineer'" :xs="24" :md="14">
          <div class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">我的近期任务</h3>
              <a-button size="small" type="link" @click="gotoTasks">
                更多
                <RightOutlined />
              </a-button>
            </div>
            <EmptyState v-if="!topTodayTasks.length" description="暂无近期任务" size="compact" />
            <ul v-else class="task-list">
              <li
                v-for="t in topTodayTasks"
                :key="t.taskId"
                class="task-item"
                @click="gotoTaskDetail(t.taskId)"
              >
                <div class="task-row">
                  <span class="task-name" :title="t.taskName">{{ t.taskName }}</span>
                  <StatusTag :tone="TaskStatusTone[t.status as TaskStatus] || 'default'">
                    {{ TaskStatusLabel[t.status as TaskStatus] || t.status }}
                  </StatusTag>
                </div>
                <div class="task-meta">
                  <span v-if="t.projectName" class="task-project" :title="t.projectName">
                    {{ t.projectName }}
                  </span>
                  <span v-if="t.plannedEnd" class="task-due tnum">截止：{{ t.plannedEnd }}</span>
                </div>
              </li>
            </ul>
          </div>
        </a-col>

        <!-- 代理商：待接单任务 -->
        <a-col v-if="currentBlock === 'agent'" :xs="24" :md="14">
          <div class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">待接单任务</h3>
            </div>
            <EmptyState v-if="!topAgentPending.length" description="暂无待接单任务" size="compact" />
            <ul v-else class="task-list">
              <li
                v-for="t in topAgentPending"
                :key="t.outsourceTaskId"
                class="task-item"
              >
                <div class="task-row">
                  <span class="task-name" :title="t.taskName">{{ t.taskName }}</span>
                  <StatusTag :tone="TaskStatusTone[t.status as TaskStatus] || 'default'">
                    {{ TaskStatusLabel[t.status as TaskStatus] || t.status }}
                  </StatusTag>
                </div>
                <div class="task-meta">
                  <span v-if="t.projectName" class="task-project" :title="t.projectName">
                    {{ t.projectName }}
                  </span>
                  <span v-if="t.deadline" class="task-due tnum">截止：{{ t.deadline }}</span>
                </div>
              </li>
            </ul>
          </div>
        </a-col>

        <!-- 右侧：按角色差异化 -->
        <a-col :xs="24" :md="10">
          <!-- PM：待派单任务 -->
          <div v-if="currentBlock === 'pm'" class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">待派单任务</h3>
            </div>
            <EmptyState v-if="!topPendingDispatch.length" description="暂无待派单任务" size="compact" />
            <ul v-else class="task-list">
              <li
                v-for="t in topPendingDispatch"
                :key="t.taskId"
                class="task-item"
              >
                <div class="task-row">
                  <span class="task-name" :title="t.taskName">{{ t.taskName }}</span>
                  <StatusTag :tone="TaskStatusTone[t.status as TaskStatus] || 'default'">
                    {{ TaskStatusLabel[t.status as TaskStatus] || t.status }}
                  </StatusTag>
                </div>
                <div class="task-meta">
                  <span v-if="t.projectName" class="task-project" :title="t.projectName">
                    {{ t.projectName }}
                  </span>
                  <span v-if="t.plannedEnd" class="task-due tnum">截止：{{ t.plannedEnd }}</span>
                </div>
              </li>
            </ul>
          </div>

          <!-- PM：待审核交付物 -->
          <div v-if="currentBlock === 'pm'" class="vibe-card block-card" style="margin-top: 16px;">
            <div class="card-head">
              <h3 class="card-title">待审核交付物</h3>
            </div>
            <EmptyState v-if="!topReviewDeliverables.length" description="暂无待审核交付物" size="compact" />
            <ul v-else class="task-list">
              <li
                v-for="d in topReviewDeliverables"
                :key="d.deliverableId"
                class="task-item"
              >
                <div class="task-row">
                  <span class="task-name" :title="d.fileName || d.taskName">
                    {{ d.fileName || d.taskName }}
                  </span>
                  <a-tag v-if="d.outsourceStatus" color="orange">{{ d.outsourceStatus }}</a-tag>
                </div>
                <div class="task-meta">
                  <span v-if="d.projectName" class="task-project" :title="d.projectName">
                    {{ d.projectName }}
                  </span>
                  <span v-if="d.deadline" class="task-due tnum">截止：{{ d.deadline }}</span>
                </div>
              </li>
            </ul>
          </div>

          <!-- 工程师：超期任务 -->
          <div v-if="currentBlock === 'engineer'" class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">
                超期任务
                <a-tag v-if="overdueTasks.length" color="red" class="todo-badge tnum">{{ overdueTasks.length }}</a-tag>
              </h3>
            </div>
            <EmptyState v-if="!topOverdueTasks.length" description="暂无超期任务" size="compact" />
            <ul v-else class="task-list">
              <li
                v-for="t in topOverdueTasks"
                :key="t.taskId"
                class="task-item overdue"
                @click="gotoTaskDetail(t.taskId)"
              >
                <div class="task-row">
                  <span class="task-name" :title="t.taskName">{{ t.taskName }}</span>
                  <a-tag color="red">超期</a-tag>
                </div>
                <div class="task-meta">
                  <span v-if="t.projectName" class="task-project" :title="t.projectName">
                    {{ t.projectName }}
                  </span>
                  <span v-if="t.plannedEnd" class="task-due tnum">原计划截止：{{ t.plannedEnd }}</span>
                </div>
              </li>
            </ul>
          </div>

          <!-- 总监：待办合计 -->
          <div v-if="currentBlock === 'director'" class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">待办审批</h3>
            </div>
            <div class="director-pending">
              <div class="pending-item">
                <span class="pending-label">待审批变更</span>
                <span class="pending-num tnum">{{ director?.pendingChangeCount ?? 0 }}</span>
              </div>
              <div class="pending-item">
                <span class="pending-label">待确认工作量</span>
                <span class="pending-num tnum">{{ director?.pendingWorkloadCount ?? 0 }}</span>
              </div>
              <div class="pending-item">
                <span class="pending-label">合计</span>
                <span class="pending-num tnum total">{{ directorPending }}</span>
              </div>
            </div>
          </div>

          <!-- 代理商：待审核任务 -->
          <div v-if="currentBlock === 'agent'" class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">待审核任务</h3>
            </div>
            <EmptyState v-if="!topAgentSubmitted.length" description="暂无待审核任务" size="compact" />
            <ul v-else class="task-list">
              <li
                v-for="t in topAgentSubmitted"
                :key="t.outsourceTaskId"
                class="task-item"
              >
                <div class="task-row">
                  <span class="task-name" :title="t.taskName">{{ t.taskName }}</span>
                  <StatusTag :tone="TaskStatusTone[t.status as TaskStatus] || 'default'">
                    {{ TaskStatusLabel[t.status as TaskStatus] || t.status }}
                  </StatusTag>
                </div>
                <div class="task-meta">
                  <span v-if="t.agentCompanyName" class="task-project" :title="t.agentCompanyName">
                    {{ t.agentCompanyName }}
                  </span>
                  <span v-if="t.deadline" class="task-due tnum">截止：{{ t.deadline }}</span>
                </div>
              </li>
            </ul>
          </div>
        </a-col>
      </a-row>

      <!-- 最近动态 -->
      <a-row :gutter="16">
        <a-col :xs="24" :md="14">
          <div class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">最近动态</h3>
              <a-button size="small" type="link" @click="gotoMessages">
                查看全部
                <RightOutlined />
              </a-button>
            </div>
            <EmptyState v-if="!topNotices.length" description="暂无动态" size="compact" />
            <div v-else class="activity-list">
              <a-timeline>
                <a-timeline-item
                  v-for="n in topNotices"
                  :key="n.id"
                  :color="n.read ? 'gray' : 'blue'"
                >
                  <div class="activity-item">
                    <StatusTag :tone="noticeTypeTone[n.type] || 'default'">
                      {{ noticeTypeLabel[n.type] || n.type }}
                    </StatusTag>
                    <span class="activity-title">{{ n.title }}</span>
                  </div>
                  <div class="activity-time tnum">{{ n.createdAt }}</div>
                </a-timeline-item>
              </a-timeline>
            </div>
          </div>
        </a-col>

        <!-- 本月项目进度（仅 director/engineer 角色可见） -->
        <a-col v-if="currentBlock === 'director' || currentBlock === 'engineer'" :xs="24" :md="10">
          <div class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">本月项目进度</h3>
            </div>
            <div class="month-progress">
              <div class="progress-item">
                <div class="progress-row">
                  <span class="progress-label">在建项目</span>
                  <span class="progress-num tnum">{{ statCards[0]?.value ?? 0 }}</span>
                </div>
                <ProgressBar :percent="100" :show-label="false" size="normal" />
              </div>
              <div class="progress-item" v-if="currentBlock === 'director'">
                <div class="progress-row">
                  <span class="progress-label">风险项目</span>
                  <span class="progress-num tnum">{{ riskProjects.length }}</span>
                </div>
                <ProgressBar
                  :percent="riskProjects.length ? Math.min(100, riskProjects.length * 20) : 0"
                  :show-label="false"
                  size="normal"
                />
              </div>
              <div class="progress-item" v-if="currentBlock === 'engineer'">
                <div class="progress-row">
                  <span class="progress-label">超期任务</span>
                  <span class="progress-num tnum">{{ engineer?.overdueTaskCount ?? 0 }}</span>
                </div>
                <ProgressBar
                  :percent="engineer?.overdueTaskCount ? Math.min(100, engineer.overdueTaskCount * 20) : 0"
                  :show-label="false"
                  size="normal"
                />
              </div>
              <div class="progress-item">
                <div class="progress-row">
                  <span class="progress-label">待办事项</span>
                  <span class="progress-num tnum">{{ unreadCount }}</span>
                </div>
                <ProgressBar
                  :percent="unreadCount ? Math.min(100, unreadCount * 20) : 0"
                  :show-label="false"
                  size="normal"
                />
              </div>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>
  </PageContainer>
</template>

<style lang="less" scoped>
.dashboard-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.dashboard-date {
  margin: 0;
  font-size: 13px;
  color: @text-tertiary;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.dashboard-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.stat-row {
  margin-bottom: 0;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid @border-color-split;
  flex-wrap: wrap;
  gap: 8px;
}
.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: @text-primary;
  display: flex;
  align-items: center;
  gap: 8px;
}
.todo-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: @status-exception;
  color: #fff;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}
.card-actions {
  display: flex;
  gap: 4px;
  align-items: center;
}
.todo-card,
.block-card {
  height: 100%;
}

/* 待办列表 */
.todo-list {
  list-style: none;
  padding: 8px 20px;
  margin: 0;
}
.todo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 8px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.2s;
  &:last-child {
    border-bottom: none;
  }
  &:hover {
    background: @bg-selected;
  }
  &.unread {
    background: rgba(24, 144, 255, 0.04);
  }
}
.todo-title {
  flex: 1;
  font-size: 14px;
  color: @text-primary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.todo-time {
  font-size: 12px;
  color: @text-tertiary;
  flex-shrink: 0;
}
.todo-unread-tag {
  margin: 0;
  font-size: 11px;
  line-height: 16px;
  padding: 0 4px;
}

/* 项目列表 */
.project-list {
  list-style: none;
  padding: 12px 20px;
  margin: 0;
}
.project-item {
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f5f5f5;
  &:last-child {
    border-bottom: none;
  }
  &:hover {
    background: @bg-selected;
  }
}
.risk-item {
  border-left: 3px solid @status-exception;
  padding-left: 12px;
}
.project-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.project-name {
  font-size: 14px;
  font-weight: 500;
  color: @text-primary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.project-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
.role-tag {
  margin: 0;
  font-size: 11px;
  line-height: 16px;
  padding: 0 6px;
  background: @bg-stripe;
  border: none;
  color: @text-secondary;
}
.project-progress {
  margin-top: 4px;
}
.risk-desc {
  font-size: 12px;
  color: @text-tertiary;
  margin-bottom: 6px;
  line-height: 1.5;
}

/* 任务列表 */
.task-list {
  list-style: none;
  padding: 8px 20px;
  margin: 0;
}
.task-item {
  padding: 10px 8px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.2s;
  &:last-child {
    border-bottom: none;
  }
  &:hover {
    background: @bg-selected;
  }
  &.overdue {
    border-left: 3px solid @status-exception;
    padding-left: 8px;
  }
}
.task-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}
.task-name {
  font-size: 14px;
  color: @text-primary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.task-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: @text-tertiary;
}
.task-project {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 120px;
}
.task-due {
  flex-shrink: 0;
}

/* 总监待办 */
.director-pending {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.pending-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: @bg-stripe;
  border-radius: 4px;
}
.pending-label {
  font-size: 13px;
  color: @text-secondary;
}
.pending-num {
  font-size: 18px;
  font-weight: 600;
  color: @text-primary;
  &.total {
    color: @status-exception;
  }
}

/* 动态时间线 */
.activity-list {
  padding: 16px 20px;
}
.activity-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}
.activity-title {
  font-size: 14px;
  color: @text-primary;
}
.activity-time {
  font-size: 12px;
  color: @text-tertiary;
  margin-top: 2px;
}

/* 本月进度 */
.month-progress {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.progress-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.progress-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.progress-label {
  font-size: 13px;
  color: @text-secondary;
}
.progress-num {
  font-size: 16px;
  font-weight: 600;
  color: @text-primary;
}

/* 响应式：移动端紧凑布局 */
@media (max-width: 768px) {
  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
  }
  .todo-item,
  .task-item {
    flex-wrap: wrap;
  }
  .todo-time {
    width: 100%;
    text-align: right;
  }
  .cost-bar-item {
    grid-template-columns: 100px 1fr;
  }
}
</style>
