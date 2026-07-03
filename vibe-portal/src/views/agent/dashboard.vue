<template>
  <div class="agent-dashboard">
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <!-- 统计卡片 -->
      <section class="stat-row">
        <div class="stat-card" @click="goTasks('PENDING')">
          <div class="stat-card__num">{{ stat.pendingCount }}</div>
          <div class="stat-card__label">待接单</div>
        </div>
        <div class="stat-card stat-card--warning" @click="goTasks('IN_PROGRESS')">
          <div class="stat-card__num">{{ stat.inProgressCount }}</div>
          <div class="stat-card__label">进行中</div>
        </div>
        <div class="stat-card stat-card--orange" @click="goTasks('SUBMITTED')">
          <div class="stat-card__num">{{ stat.pendingReviewCount }}</div>
          <div class="stat-card__label">待审核</div>
        </div>
      </section>

      <!-- 待接单列表 -->
      <section class="card-block">
        <div class="block-head">
          <span>待接单</span>
          <a @click="goTasks('PENDING')">查看全部 ></a>
        </div>
        <ul class="task-list">
          <li v-for="t in pendingTasks" :key="t.id" class="task-card">
            <TaskCard
              :task="t"
              @click="goDetail(t.id)"
              @accept="onAccept"
              @reject="onReject"
              @assign="onAssign"
            />
          </li>
          <li v-if="!pendingTasks.length" class="empty-tip">暂无待接单任务</li>
        </ul>
      </section>

      <!-- 进行中列表 -->
      <section class="card-block">
        <div class="block-head">
          <span>进行中</span>
          <a @click="goTasks('IN_PROGRESS')">查看全部 ></a>
        </div>
        <ul class="task-list">
          <li v-for="t in inProgressTasks" :key="t.id" class="task-card">
            <TaskCard :task="t" @click="goDetail(t.id)" @submit="onSubmitDeliverable(t.id)" />
          </li>
          <li v-if="!inProgressTasks.length" class="empty-tip">暂无进行中任务</li>
        </ul>
      </section>

      <!-- 待审核列表 -->
      <section class="card-block">
        <div class="block-head">
          <span>待审核</span>
          <a @click="goTasks('SUBMITTED')">查看全部 ></a>
        </div>
        <ul class="task-list">
          <li v-for="t in submittedTasks" :key="t.id" class="task-card">
            <TaskCard :task="t" @click="goDetail(t.id)" />
          </li>
          <li v-if="!submittedTasks.length" class="empty-tip">暂无待审核任务</li>
        </ul>
      </section>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import TaskCard from './components/TaskCard.vue'
import { getOutsourceTasks, acceptTask, rejectTask } from '@/api/agent'
import {
  OutsourceTaskStatusEnum,
  type AgentDashboardStat,
  type OutsourceTaskVO
} from '@/types/api'

const router = useRouter()
const refreshing = ref(false)
const stat = ref<AgentDashboardStat>({ pendingCount: 0, inProgressCount: 0, pendingReviewCount: 0 })
const pendingTasks = ref<OutsourceTaskVO[]>([])
const inProgressTasks = ref<OutsourceTaskVO[]>([])
const submittedTasks = ref<OutsourceTaskVO[]>([])

/** 拉取工作台数据：通过任务分页接口的 total 字段统计各状态数量 */
async function fetchDashboard() {
  try {
    const [pending, inProgress, submitted] = await Promise.all([
      getOutsourceTasks({ status: OutsourceTaskStatusEnum.PENDING, size: 10 }),
      getOutsourceTasks({ status: OutsourceTaskStatusEnum.IN_PROGRESS, size: 5 }),
      getOutsourceTasks({ status: OutsourceTaskStatusEnum.SUBMITTED, size: 5 })
    ])
    stat.value = {
      pendingCount: pending?.total ?? pending?.list?.length ?? 0,
      inProgressCount: inProgress?.total ?? inProgress?.list?.length ?? 0,
      pendingReviewCount: submitted?.total ?? submitted?.list?.length ?? 0
    }
    pendingTasks.value = pending?.list ?? []
    inProgressTasks.value = inProgress?.list ?? []
    submittedTasks.value = submitted?.list ?? []
  } catch (e) {
    // 拦截器已提示
  }
}

function onRefresh() {
  fetchDashboard().finally(() => {
    refreshing.value = false
  })
}

function goTasks(status?: string) {
  router.push({ path: '/agent/tasks', query: status ? { status } : {} })
}

function goDetail(id: number | string) {
  router.push(`/agent/tasks/${id}`)
}

function onSubmitDeliverable(id: number | string) {
  router.push(`/agent/tasks/${id}/submit`)
}

async function onAccept(task: OutsourceTaskVO) {
  showConfirmDialog({
    title: '确认接单',
    message: `确定接受任务「${task.siteName}」吗？`
  })
    .then(async () => {
      try {
        await acceptTask(task.id)
        showToast('接单成功')
        fetchDashboard()
      } catch (e) {
        // 拦截器已提示
      }
    })
    .catch(() => {})
}

async function onReject(task: OutsourceTaskVO) {
  showConfirmDialog({
    title: '确认拒绝',
    message: `确定拒绝任务「${task.siteName}」吗？`
  })
    .then(async () => {
      try {
        await rejectTask(task.id)
        showToast('已拒绝')
        fetchDashboard()
      } catch (e) {
        // 拦截器已提示
      }
    })
    .catch(() => {})
}

async function onAssign(task: OutsourceTaskVO) {
  // 跳转详情页指派（详情页提供工程师输入）
  goDetail(task.id)
}

onMounted(fetchDashboard)
</script>

<style lang="scss" scoped>
.agent-dashboard {
  padding: 12px;
  min-height: 100%;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 0;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);

  &__num {
    font-size: 24px;
    font-weight: 600;
    color: var(--agent-primary);
  }
  &__label {
    margin-top: 4px;
    font-size: 12px;
    color: var(--color-text-secondary);
  }

  &--warning .stat-card__num {
    color: var(--color-warning);
  }
  &--orange .stat-card__num {
    color: #fa8c16;
  }
}

.card-block {
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 12px;

  .block-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 15px;
    font-weight: 600;
    margin-bottom: 8px;

    a {
      font-size: 12px;
      font-weight: 400;
      color: var(--color-text-secondary);
    }
  }
}

.task-list {
  .task-card + .task-card {
    margin-top: 8px;
  }
}

.empty-tip {
  text-align: center;
  padding: 16px 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}
</style>
