<script setup lang="ts">
/**
 * 我的任务
 * 当前用户的任务列表，按状态分类筛选
 *
 * 数据来源：/api/v1/dashboard 聚合接口（DashboardVO：role/realName/director/pm/engineer/agent）
 * 字段访问按 dashboard.role 选择对应子块：
 *   - SUPER_ADMIN / DIRECTOR → 暂无个人任务（提示文案）
 *   - PM                    → pm.pendingDispatchTasks（待派单任务）
 *   - ENGINEER              → engineer.todayTasks + engineer.overdueTasks
 *   - AGENT_ADMIN/AGENT_ENGINEER → agent.pendingTasks + agent.submittedTasks
 *
 * Bug 修复（spec Task 21.1）：
 *   旧代码错误地从 res.myTasks 取数（DashboardVO 不存在该字段，永远返回空）。
 *   改为按 role 路径取数，并将 TaskItem / OutsourceTaskItem 归一化为 MyTask。
 */
import { ref, onMounted, computed } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getDashboard, type DashboardVO, type TaskItem, type OutsourceTaskItem } from '@/api/report'
import { useUserStore } from '@/stores/user'
import { TaskStatus, TaskStatusTone, TaskStatusLabel } from '@/types/enum'

interface MyTask {
  id: number
  name: string
  dueDate?: string
  status: string
  projectName?: string
}

const userStore = useUserStore()

const loading = ref(false)
const dashboard = ref<DashboardVO | null>(null)
const dataSource = ref<MyTask[]>([])
const statusFilter = ref<string | undefined>(undefined)

/** 将 TaskItem 归一化为 MyTask */
function fromTaskItem(t: TaskItem): MyTask {
  return {
    id: t.taskId,
    name: t.taskName,
    dueDate: t.plannedEnd,
    status: t.status,
    projectName: t.projectName
  }
}

/** 将 OutsourceTaskItem 归一化为 MyTask */
function fromOutsourceItem(t: OutsourceTaskItem): MyTask {
  return {
    id: t.outsourceTaskId,
    name: t.taskName,
    dueDate: t.deadline,
    status: t.status,
    projectName: t.projectName
  }
}

/** 当前用户角色（dashboard.role 优先，回退到 userStore.roles[0]） */
const currentRole = computed<string>(() => {
  return dashboard.value?.role || userStore.roles?.[0] || ''
})

/** 是否为无个人任务的视图（总监/管理员/客户等） */
const isEmptyRoleView = computed(() => {
  const role = currentRole.value
  return role === 'SUPER_ADMIN' || role === 'DIRECTOR' || role === 'FINANCE' || role === 'CUSTOMER'
})

async function loadData() {
  loading.value = true
  try {
    const data = await getDashboard()
    dashboard.value = data
    const role = data?.role || userStore.roles?.[0] || ''

    // 按 role 选择对应子块的任务列表
    let list: MyTask[] = []
    if (role === 'PM') {
      const pending = data?.pm?.pendingDispatchTasks ?? []
      list = pending.map(fromTaskItem)
    } else if (role === 'ENGINEER') {
      const today = data?.engineer?.todayTasks ?? []
      const overdue = data?.engineer?.overdueTasks ?? []
      // 合并今日任务 + 超期任务，去重（按 id）
      const merged: MyTask[] = []
      const seen = new Set<number>()
      for (const t of [...today, ...overdue]) {
        const mt = fromTaskItem(t)
        if (!seen.has(mt.id)) {
          seen.add(mt.id)
          merged.push(mt)
        }
      }
      list = merged
    } else if (role === 'AGENT_ADMIN' || role === 'AGENT_ENGINEER') {
      const pending = data?.agent?.pendingTasks ?? []
      const submitted = data?.agent?.submittedTasks ?? []
      // 合并待接单 + 待审核任务，去重
      const merged: MyTask[] = []
      const seen = new Set<number>()
      for (const t of [...pending, ...submitted]) {
        const mt = fromOutsourceItem(t)
        if (!seen.has(mt.id)) {
          seen.add(mt.id)
          merged.push(mt)
        }
      }
      list = merged
    }
    // SUPER_ADMIN / DIRECTOR / FINANCE / CUSTOMER 等无个人任务：保持空列表 + 提示

    dataSource.value = list
  } catch (e) {
    console.error('[dashboard.my-tasks] load failed:', e)
    dataSource.value = []
  } finally {
    loading.value = false
  }
}

const filtered = computed(() => {
  if (!statusFilter.value) return dataSource.value
  return dataSource.value.filter((t) => t.status === statusFilter.value)
})

const columns = [
  { title: '任务名称', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '所属项目', dataIndex: 'projectName', key: 'projectName', width: 200, ellipsis: true },
  { title: '截止日期', dataIndex: 'dueDate', key: 'dueDate', width: 130 },
  { title: '状态', key: 'status', width: 110 },
  { title: '操作', key: 'action', width: 100 }
]

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="我的任务" description="我负责或参与的任务列表">
    <template #extra>
      <a-button @click="loadData" :loading="loading"><template #icon><ReloadOutlined /></template>刷新</a-button>
    </template>

    <!-- 总监/管理员等无个人任务的提示 -->
    <div v-if="isEmptyRoleView" class="vibe-card" style="padding: 24px;">
      <EmptyState
        description="当前角色暂无个人任务，请前往工作台查看全局数据"
        action-text="返回工作台"
        @action="$router.push('/dashboard')"
      />
    </div>

    <template v-else>
      <div class="vibe-card filter-card">
        <a-radio-group v-model:value="statusFilter" button-style="solid">
          <a-radio-button :value="undefined">全部</a-radio-button>
          <a-radio-button v-for="s in [TaskStatus.TODO, TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS, TaskStatus.SUBMITTED, TaskStatus.CONFIRMED]" :key="s" :value="s">{{ TaskStatusLabel[s] }}</a-radio-button>
        </a-radio-group>
      </div>

      <div class="vibe-card table-card">
        <a-table :columns="columns" :data-source="filtered" :loading="loading" row-key="id" :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <StatusTag :tone="TaskStatusTone[record.status as TaskStatus] || 'default'">{{ TaskStatusLabel[record.status as TaskStatus] || record.status }}</StatusTag>
            </template>
            <template v-else-if="column.key === 'action'">
              <a>查看</a>
            </template>
          </template>
          <template #emptyText><EmptyState description="暂无任务" /></template>
        </a-table>
      </div>
    </template>
  </PageContainer>
</template>

<style lang="less" scoped>
.filter-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
</style>
