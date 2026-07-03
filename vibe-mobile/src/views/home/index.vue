<script setup lang="ts">
/**
 * 首页（工程师工作台）
 * - 今日任务卡片列表（GET /work-orders/me）
 * - 本周工时统计（GET /timesheets/summary）
 * - 工时入口
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { fetchMyWorkOrders, fetchWeekTimesheet, fetchUnreadNoticeCount } from '@/api'
import { WorkOrderStatusMap, WorkOrderStatusEnum, type UserInfo, type WorkOrderVO } from '@/types/api'

defineOptions({ name: 'Home' })

const router = useRouter()
const userStore = useUserStore()

const tasks = ref<WorkOrderVO[]>([])
const weekHours = ref(0)
const monthTravelDays = ref(0)
const loading = ref(false)
const unreadCount = ref(0)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const userInfo = computed<UserInfo | undefined>(() => userStore.userInfo ?? undefined)
const today = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})

async function loadData() {
  loading.value = true
  try {
    const [taskList, ts, unread] = await Promise.allSettled([
      fetchMyWorkOrders(),
      fetchWeekTimesheet(),
      fetchUnreadNoticeCount()
    ])
    if (taskList.status === 'fulfilled') tasks.value = taskList.value || []
    if (ts.status === 'fulfilled') {
      weekHours.value = ts.value?.weekHours || 0
      monthTravelDays.value = ts.value?.monthTravelDays || 0
    }
    if (unread.status === 'fulfilled') unreadCount.value = Number(unread.value || 0)
  } finally {
    loading.value = false
  }
}

function goTaskDetail(id: string | number) {
  router.push(`/task/detail/${id}`)
}

function goWork(task: WorkOrderVO) {
  // 待签到 → 签到页；进行中 → 施工步骤页
  if (task.status === WorkOrderStatusEnum.IN_PROGRESS) {
    router.push(`/field/steps/${task.id}`)
  } else {
    router.push(`/field/checkin/${task.id}`)
  }
}

function goTimesheet() {
  router.push('/mine/timesheet')
}

function goMessage() {
  router.push('/mine/messages')
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="home-page page">
    <!-- 顶部欢迎区 -->
    <div class="home-page__header">
      <div class="greeting">
        <div class="greeting-text">{{ greeting }}，{{ userInfo?.nickname || '工程师' }}</div>
        <div class="date">{{ today }}</div>
      </div>
      <div class="bell touchable" @click="goMessage">
        <van-icon name="bell" size="22" />
        <van-badge v-if="unreadCount > 0" :content="unreadCount" class="bell-badge" />
      </div>
    </div>

    <!-- 今日任务 -->
    <section class="section">
      <div class="section__title">
        <span>今日任务</span>
        <van-button plain hairline size="mini" class="touchable" @click="router.push('/task')">
          全部
        </van-button>
      </div>

      <van-loading v-if="loading" type="spinner" class="loading" />

      <van-empty v-else-if="tasks.length === 0" description="今日暂无任务" />

      <div v-else class="task-list">
        <div v-for="task in tasks" :key="task.id" class="task-card card" @click="goTaskDetail(task.id)">
          <div class="task-card__head">
            <van-icon name="location-o" class="loc-icon" />
            <span class="task-card__title ellipsis">{{ task.projectName || task.taskName || '工单' }}</span>
            <span
              class="task-card__status"
              :style="{ color: WorkOrderStatusMap[task.status]?.color }"
            >
              {{ WorkOrderStatusMap[task.status]?.label || task.status }}
            </span>
          </div>
          <div v-if="task.taskName" class="task-card__addr ellipsis">{{ task.taskName }}</div>
          <div v-if="task.checkinLocation?.address" class="task-card__addr ellipsis">
            📍 {{ task.checkinLocation.address }}
          </div>
          <div v-if="task.checkinTime" class="task-card__contact">
            签到时间：{{ task.checkinTime }}
          </div>
          <div v-if="task.totalStepCount" class="task-card__progress">
            已完成 {{ task.completedStepCount || 0 }}/{{ task.totalStepCount }} 步
            <van-progress
              :percentage="Math.round(((task.completedStepCount || 0) / task.totalStepCount) * 100)"
              :show-pivot="false"
              stroke-width="3"
            />
          </div>
          <div class="task-card__actions">
            <van-button
              v-if="task.status === WorkOrderStatusEnum.PENDING_CHECKIN"
              type="primary"
              size="small"
              class="touchable"
              @click.stop="goWork(task)"
            >
              开始签到
            </van-button>
            <van-button
              v-else-if="task.status === WorkOrderStatusEnum.IN_PROGRESS"
              type="primary"
              size="small"
              class="touchable"
              @click.stop="goWork(task)"
            >
              继续
            </van-button>
            <van-button plain size="small" class="touchable" @click.stop="goTaskDetail(task.id)">
              查看详情
            </van-button>
          </div>
        </div>
      </div>
    </section>

    <!-- 工时统计 -->
    <section class="section">
      <div class="stat-card card">
        <div class="stat-card__row">
          <div class="stat-card__item">
            <div class="value">{{ weekHours }}<span class="unit">h</span></div>
            <div class="label">本周工时</div>
          </div>
          <div class="stat-card__divider"></div>
          <div class="stat-card__item">
            <div class="value">{{ monthTravelDays }}<span class="unit">天</span></div>
            <div class="label">近期出差</div>
          </div>
        </div>
        <van-button type="primary" plain block class="touchable" @click="goTimesheet">
          填报工时 →
        </van-button>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.home-page {
  padding: 12px;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    .greeting {
      .greeting-text {
        font-size: 18px;
        font-weight: 600;
      }
      .date {
        font-size: 12px;
        color: var(--color-text-secondary);
        margin-top: 2px;
      }
    }

    .bell {
      position: relative;
      padding: 8px;
      color: var(--color-text-regular);
    }
  }
}

.section {
  margin-bottom: 16px;

  &__title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
    font-size: 15px;
    font-weight: 600;
  }
}

.task-card {
  &__head {
    display: flex;
    align-items: center;
    margin-bottom: 6px;
  }

  .loc-icon {
    color: var(--brand-primary);
    margin-right: 4px;
  }

  &__title {
    flex: 1;
    font-size: 15px;
    font-weight: 600;
    margin-right: 8px;
  }

  &__status {
    font-size: 12px;
    font-weight: 600;
  }

  &__addr {
    font-size: 13px;
    color: var(--color-text-secondary);
    margin-bottom: 4px;
  }

  &__contact {
    font-size: 12px;
    color: var(--color-text-secondary);
    margin-bottom: 6px;
  }

  &__progress {
    font-size: 12px;
    color: var(--color-text-regular);
    margin-bottom: 8px;
  }

  &__actions {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
  }
}

.stat-card {
  &__row {
    display: flex;
    align-items: center;
    margin-bottom: 12px;
  }

  &__item {
    flex: 1;
    text-align: center;

    .value {
      font-size: 26px;
      font-weight: 700;
      color: var(--brand-primary);

      .unit {
        font-size: 14px;
        font-weight: 400;
        margin-left: 2px;
      }
    }

    .label {
      font-size: 12px;
      color: var(--color-text-secondary);
      margin-top: 4px;
    }
  }

  &__divider {
    width: 1px;
    height: 36px;
    background: var(--border-color);
  }
}

.loading {
  text-align: center;
  padding: 30px 0;
}
</style>
