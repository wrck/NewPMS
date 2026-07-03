<script setup lang="ts">
/**
 * 现场作业入口（Tab 页）
 * 展示当前进行中/待签到的任务，提供快速入口到签到/施工/异常上报
 * 数据源：GET /work-orders/me
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchMyWorkOrders } from '@/api'
import {
  WorkOrderStatusMap,
  WorkOrderStatusEnum,
  type WorkOrderVO
} from '@/types/api'

defineOptions({ name: 'Field' })

const router = useRouter()

const tasks = ref<WorkOrderVO[]>([])
const loading = ref(false)

/** 待签到任务（PENDING_CHECKIN） */
const pendingTasks = computed(() =>
  tasks.value.filter((t) => t.status === WorkOrderStatusEnum.PENDING_CHECKIN)
)
/** 进行中任务（IN_PROGRESS） */
const inProgressTasks = computed(() =>
  tasks.value.filter((t) => t.status === WorkOrderStatusEnum.IN_PROGRESS)
)

async function loadData() {
  loading.value = true
  try {
    const list = await fetchMyWorkOrders()
    tasks.value = list || []
  } catch (e) {
    tasks.value = []
  } finally {
    loading.value = false
  }
}

function goCheckin(taskId: string | number) {
  router.push(`/field/checkin/${taskId}`)
}

function goSteps(taskId: string | number) {
  router.push(`/field/steps/${taskId}`)
}

function goIssue(taskId: string | number) {
  router.push(`/field/issue/${taskId}`)
}

function goTaskList() {
  router.push('/task')
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="field-page page">
    <!-- 顶部说明 -->
    <div class="field-page__header card">
      <van-icon name="location-o" size="22" color="#1677ff" />
      <div class="header-text">
        <div class="title">现场作业</div>
        <div class="desc">签到 → 施工步骤 → 拍照 → 完成确认 → 签退</div>
      </div>
    </div>

    <van-loading v-if="loading" type="spinner" class="loading" />

    <!-- 待签到 -->
    <section v-if="!loading && pendingTasks.length" class="section">
      <div class="section__title">
        <van-icon name="clock-o" color="#faad14" />
        <span>待签到（{{ pendingTasks.length }}）</span>
      </div>
      <div
        v-for="task in pendingTasks"
        :key="task.id"
        class="task-card card"
      >
        <div class="task-card__head">
          <span class="task-card__title ellipsis">{{ task.projectName || task.taskName || '工单' }}</span>
          <span class="task-card__status" :style="{ color: WorkOrderStatusMap[task.status]?.color }">
            {{ WorkOrderStatusMap[task.status]?.label }}
          </span>
        </div>
        <div v-if="task.checkinLocation?.address" class="task-card__addr ellipsis">
          📍 {{ task.checkinLocation.address }}
        </div>
        <div v-if="task.createTime" class="task-card__time">
          派发时间：{{ task.createTime }}
        </div>
        <div class="task-card__actions">
          <van-button
            type="primary"
            size="small"
            class="touchable"
            @click="goCheckin(task.id)"
          >
            拍照签到
          </van-button>
          <van-button plain size="small" class="touchable" @click="goTaskList">
            查看详情
          </van-button>
        </div>
      </div>
    </section>

    <!-- 进行中 -->
    <section v-if="!loading && inProgressTasks.length" class="section">
      <div class="section__title">
        <van-icon name="underway-o" color="#1677ff" />
        <span>进行中（{{ inProgressTasks.length }}）</span>
      </div>
      <div
        v-for="task in inProgressTasks"
        :key="task.id"
        class="task-card card"
      >
        <div class="task-card__head">
          <span class="task-card__title ellipsis">{{ task.projectName || task.taskName || '工单' }}</span>
          <span class="task-card__status" :style="{ color: WorkOrderStatusMap[task.status]?.color }">
            {{ WorkOrderStatusMap[task.status]?.label }}
          </span>
        </div>
        <div v-if="task.checkinLocation?.address" class="task-card__addr ellipsis">
          📍 {{ task.checkinLocation.address }}
        </div>
        <div v-if="task.checkinTime" class="task-card__time">
          签到时间：{{ task.checkinTime }}
        </div>
        <div v-if="task.totalStepCount" class="task-card__progress">
          <span>进度：{{ task.completedStepCount || 0 }}/{{ task.totalStepCount }} 步</span>
          <van-progress
            :percentage="Math.round(((task.completedStepCount || 0) / task.totalStepCount) * 100)"
            :show-pivot="false"
            stroke-width="4"
          />
        </div>
        <div class="task-card__actions">
          <van-button
            type="primary"
            size="small"
            class="touchable"
            @click="goSteps(task.id)"
          >
            继续施工
          </van-button>
          <van-button
            type="warning"
            plain
            size="small"
            class="touchable"
            @click="goIssue(task.id)"
          >
            上报异常
          </van-button>
        </div>
      </div>
    </section>

    <!-- 空状态 -->
    <van-empty
      v-if="!loading && !pendingTasks.length && !inProgressTasks.length"
      description="今日暂无现场作业任务"
    >
      <van-button type="primary" plain class="touchable" @click="goTaskList">查看全部任务</van-button>
    </van-empty>
  </div>
</template>

<style scoped lang="scss">
.field-page {
  padding: 12px;

  &__header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;

    .header-text {
      flex: 1;

      .title {
        font-size: 17px;
        font-weight: 700;
        color: var(--color-text-primary);
      }
      .desc {
        font-size: 12px;
        color: var(--color-text-secondary);
        margin-top: 2px;
      }
    }
  }
}

.section {
  margin-bottom: 16px;

  &__title {
    display: flex;
    align-items: center;
    gap: 6px;
    margin: 0 4px 10px;
    font-size: 15px;
    font-weight: 600;
    color: var(--color-text-regular);
  }
}

.task-card {
  &__head {
    display: flex;
    align-items: center;
    margin-bottom: 6px;
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

  &__time {
    font-size: 12px;
    color: var(--color-text-secondary);
    margin-bottom: 8px;
  }

  &__progress {
    font-size: 12px;
    color: var(--color-text-regular);
    margin-bottom: 8px;

    :deep(.van-progress) {
      margin-top: 4px;
    }
  }

  &__actions {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
  }
}

.loading {
  text-align: center;
  padding: 30px 0;
}
</style>
