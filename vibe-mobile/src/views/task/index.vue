<script setup lang="ts">
/**
 * 任务列表页
 * - 下拉刷新 + 上拉加载
 * - 状态筛选（GET /work-orders?page=1&size=10&status=xxx）
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchTasks, type TaskQuery } from '@/api'
import {
  WorkOrderStatusMap,
  WorkOrderStatusEnum,
  type WorkOrderVO
} from '@/types/api'
import dayjs from 'dayjs'

defineOptions({ name: 'Task' })

const router = useRouter()

const list = ref<WorkOrderVO[]>([])
const loading = ref(false)
const refreshing = ref(false)
const finished = ref(false)
const error = ref(false)

const query = reactive<TaskQuery>({
  page: 1,
  size: 10,
  status: ''
})

const statusOptions = [
  { text: '全部', value: '' },
  { text: '待签到', value: WorkOrderStatusEnum.PENDING_CHECKIN },
  { text: '进行中', value: WorkOrderStatusEnum.IN_PROGRESS },
  { text: '已完成', value: WorkOrderStatusEnum.COMPLETED },
  { text: '已确认', value: WorkOrderStatusEnum.CONFIRMED }
]

const dropdownValue = ref('')

function formatDate(d?: string): string {
  if (!d) return ''
  return dayjs(d).format('MM-DD')
}

async function onLoad() {
  if (refreshing.value) return
  loading.value = true
  try {
    const res = await fetchTasks(query)
    const items = res?.records || []
    if (query.page === 1) {
      list.value = items
    } else {
      list.value.push(...items)
    }
    finished.value = list.value.length >= (res?.total || 0)
    if (!finished.value) {
      query.page = (query.page || 1) + 1
    }
    error.value = false
  } catch (err) {
    error.value = true
  } finally {
    loading.value = false
  }
}

async function onRefresh() {
  refreshing.value = true
  query.page = 1
  finished.value = false
  try {
    const res = await fetchTasks(query)
    list.value = res?.records || []
    finished.value = list.value.length >= (res?.total || 0)
    if (!finished.value) {
      query.page = 2
    }
  } catch (err) {
    list.value = []
  } finally {
    refreshing.value = false
    loading.value = false
  }
}

function onStatusChange(value: string) {
  query.status = value
  query.page = 1
  finished.value = false
  list.value = []
  onLoad()
}

function goDetail(id: string | number) {
  router.push(`/task/detail/${id}`)
}

onMounted(() => {
  // 初始加载由 van-list 触发
})
</script>

<template>
  <div class="task-page">
    <!-- 筛选栏 -->
    <van-dropdown-menu class="filter-bar">
      <van-dropdown-item v-model="dropdownValue" :options="statusOptions" @change="onStatusChange" />
    </van-dropdown-menu>

    <!-- 列表 -->
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh" class="task-page__refresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        :error="error"
        finished-text="没有更多了"
        error-text="加载失败，点击重试"
        @load="onLoad"
        @update:error="error = $event"
      >
        <div
          v-for="task in list"
          :key="task.id"
          class="task-item card"
          @click="goDetail(task.id)"
        >
          <div class="task-item__head">
            <span class="title ellipsis">{{ task.projectName || task.taskName || '工单' }}</span>
          </div>
          <div class="task-item__meta">
            <span
              class="status-tag"
              :style="{ color: WorkOrderStatusMap[task.status]?.color, borderColor: WorkOrderStatusMap[task.status]?.color }"
            >
              {{ WorkOrderStatusMap[task.status]?.label || task.status }}
            </span>
            <span v-if="task.status === 'IN_PROGRESS' && task.totalStepCount" class="progress">
              进度：{{ task.completedStepCount || 0 }}/{{ task.totalStepCount }} 步
            </span>
            <span v-else-if="task.checkinTime" class="deadline">
              签到：{{ formatDate(task.checkinTime) }}
            </span>
            <span v-else-if="task.checkoutTime" class="finished-time">
              完成于 {{ formatDate(task.checkoutTime) }}
            </span>
          </div>
          <div v-if="task.checkinLocation?.address" class="task-item__addr ellipsis">
            <van-icon name="location-o" /> {{ task.checkinLocation.address }}
          </div>
        </div>
      </van-list>

      <van-empty v-if="!loading && list.length === 0" description="暂无任务" />
    </van-pull-refresh>
  </div>
</template>

<style scoped lang="scss">
.task-page {
  min-height: 100%;

  &__refresh {
    min-height: calc(100vh - 100px);
  }
}

.filter-bar {
  position: sticky;
  top: 0;
  z-index: 10;
}

.task-item {
  margin: 8px 12px;

  &__head {
    .title {
      font-size: 15px;
      font-weight: 600;
    }
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 8px 0;
    font-size: 12px;
    color: var(--color-text-secondary);
  }

  .status-tag {
    padding: 2px 8px;
    border-radius: 10px;
    border: 1px solid;
    font-size: 11px;
    font-weight: 600;
  }

  &__addr {
    font-size: 13px;
    color: var(--color-text-secondary);
  }
}
</style>
