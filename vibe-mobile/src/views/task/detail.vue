<script setup lang="ts">
/**
 * 任务详情页
 * GET /work-orders/{id} 返回工单详情（含 steps/photos/issues）
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showImagePreview } from 'vant'
import { fetchTaskDetail, checkIn } from '@/api'
import {
  WorkOrderStatusMap,
  WorkOrderStatusEnum,
  type WorkOrderPhotoVO,
  type WorkOrderVO
} from '@/types/api'
import GpsLocation from '@/components/GpsLocation/index.vue'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

const task = ref<WorkOrderVO | null>(null)
const loading = ref(false)
const gpsComponent = ref<InstanceType<typeof GpsLocation> | null>(null)

const photos = computed<WorkOrderPhotoVO[]>(() => task.value?.photos || [])
const steps = computed(() => task.value?.steps || [])
const issues = computed(() => task.value?.issues || [])

function fmt(d?: string): string {
  if (!d) return '-'
  return dayjs(d).format('YYYY-MM-DD HH:mm')
}

async function loadDetail() {
  loading.value = true
  try {
    const id = route.params.id as string
    task.value = await fetchTaskDetail(id)
  } finally {
    loading.value = false
  }
}

function goWork() {
  if (!task.value) return
  if (task.value.status === WorkOrderStatusEnum.IN_PROGRESS) {
    router.push(`/field/steps/${task.value.id}`)
  } else {
    router.push(`/field/checkin/${task.value.id}`)
  }
}

async function onCheckIn() {
  if (!task.value || !gpsComponent.value) return
  const loc = await gpsComponent.value.locate()
  if (!loc) return
  // 签到需要照片，跳转到签到页操作
  router.push(`/field/checkin/${task.value.id}`)
}

function previewPhoto(url: string, idx: number) {
  const urls = photos.value.map((p) => p.photoUrl).filter(Boolean) as string[]
  showImagePreview({ images: urls, startPosition: idx })
}

function goSteps() {
  if (!task.value) return
  router.push(`/field/steps/${task.value.id}`)
}

function goIssue() {
  if (!task.value) return
  router.push(`/field/issue/${task.value.id}`)
}

function goComplete() {
  if (!task.value) return
  router.push(`/field/complete/${task.value.id}`)
}

onMounted(() => {
  loadDetail()
})
</script>

<template>
  <div class="task-detail page">
    <van-loading v-if="loading" type="spinner" class="loading" />

    <template v-else-if="task">
      <!-- 任务概要 -->
      <div class="card summary-card">
        <div class="summary-card__title">{{ task.projectName || '工单' }}</div>
        <div v-if="task.taskName" class="summary-card__site">{{ task.taskName }}</div>
        <div class="summary-card__status">
          <span
            class="status-tag"
            :style="{ color: WorkOrderStatusMap[task.status]?.color, borderColor: WorkOrderStatusMap[task.status]?.color }"
          >
            {{ WorkOrderStatusMap[task.status]?.label || task.status }}
          </span>
        </div>
      </div>

      <!-- 任务信息 -->
      <van-cell-group inset title="任务信息">
        <van-cell title="工单ID" :value="String(task.id)" />
        <van-cell v-if="task.taskId" title="任务ID" :value="String(task.taskId)" />
        <van-cell v-if="task.engineerName" title="工程师" :value="task.engineerName" />
        <van-cell v-if="task.checkinLocation?.address" title="签到地址" :value="task.checkinLocation.address" />
        <van-cell title="签到时间" :value="fmt(task.checkinTime)" />
        <van-cell title="签退时间" :value="fmt(task.checkoutTime)" />
        <van-cell v-if="task.totalDuration" title="总工时" :value="`${task.totalDuration} 小时`" />
        <van-cell v-if="task.totalStepCount" title="进度" :value="`${task.completedStepCount || 0}/${task.totalStepCount} 步`" />
        <van-cell v-if="task.createTime" title="创建时间" :value="fmt(task.createTime)" />
        <van-cell v-if="task.remark" title="备注" :value="task.remark" />
      </van-cell-group>

      <!-- GPS 定位 -->
      <div class="section-title">现场定位</div>
      <GpsLocation
        ref="gpsComponent"
        :target-longitude="task.checkinLocation?.longitude"
        :target-latitude="task.checkinLocation?.latitude"
        :allow-radius="500"
      />

      <!-- 施工步骤 -->
      <div v-if="steps.length" class="section-title">施工步骤（{{ steps.length }}）</div>
      <div v-if="steps.length" class="card step-list">
        <div
          v-for="step in steps"
          :key="step.id"
          class="step-row"
          :class="{ done: step.status === 'COMPLETED', skipped: step.status === 'SKIPPED' }"
        >
          <van-icon
            :name="step.status === 'COMPLETED' ? 'success' : step.status === 'SKIPPED' ? 'warning-o' : 'circle'"
            :color="step.status === 'COMPLETED' ? '#52c41a' : step.status === 'SKIPPED' ? '#faad14' : '#bfbfbf'"
          />
          <span class="step-name">{{ step.stepNo }}. {{ step.stepName }}</span>
          <span v-if="step.completedTime" class="step-time">{{ fmt(step.completedTime) }}</span>
        </div>
      </div>

      <!-- 现场照片 -->
      <div v-if="photos.length" class="section-title">现场照片（{{ photos.length }}）</div>
      <div v-if="photos.length" class="card photo-grid">
        <div
          v-for="(photo, idx) in photos"
          :key="photo.id"
          class="photo-item"
          @click="previewPhoto(photo.photoUrl, idx)"
        >
          <img :src="photo.thumbnailUrl || photo.photoUrl" alt="照片" />
        </div>
      </div>

      <!-- 异常问题 -->
      <div v-if="issues.length" class="section-title">异常问题（{{ issues.length }}）</div>
      <div v-if="issues.length" class="card issue-list">
        <div v-for="issue in issues" :key="issue.id" class="issue-row">
          <div class="issue-row__head">
            <van-tag
              :type="issue.severity === 'BLOCKING' ? 'danger' : issue.severity === 'MAJOR' ? 'warning' : 'primary'"
              size="medium"
            >
              {{ issue.severity }}
            </van-tag>
            <span class="issue-status">{{ issue.status }}</span>
          </div>
          <div class="issue-row__desc">{{ issue.description }}</div>
          <div v-if="issue.createTime" class="issue-row__time">{{ fmt(issue.createTime) }}</div>
        </div>
      </div>

      <!-- 底部操作 -->
      <div class="bottom-action">
        <van-button
          v-if="task.status === WorkOrderStatusEnum.PENDING_CHECKIN"
          type="primary"
          block
          round
          class="touchable"
          @click="onCheckIn"
        >
          去签到
        </van-button>
        <template v-else-if="task.status === WorkOrderStatusEnum.IN_PROGRESS">
          <van-button type="warning" plain class="touchable" @click="goIssue">
            上报异常
          </van-button>
          <van-button type="primary" class="touchable" @click="goSteps">
            继续施工
          </van-button>
        </template>
        <van-button plain block round class="touchable" @click="router.back()">
          返回
        </van-button>
      </div>
    </template>

    <van-empty v-else description="任务不存在" />
  </div>
</template>

<style scoped lang="scss">
.task-detail {
  padding: 12px 12px 100px;

  .section-title {
    margin: 16px 4px 8px;
    font-size: 14px;
    font-weight: 600;
    color: var(--color-text-regular);
  }
}

.summary-card {
  text-align: center;
  padding: 20px;

  &__title {
    font-size: 17px;
    font-weight: 700;
  }

  &__site {
    font-size: 13px;
    color: var(--color-text-secondary);
    margin-top: 4px;
  }

  &__status {
    margin-top: 10px;
  }

  .status-tag {
    padding: 4px 12px;
    border-radius: 12px;
    border: 1px solid;
    font-size: 12px;
    font-weight: 600;
  }
}

.step-list {
  .step-row {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 0;
    border-bottom: 1px solid var(--border-color-light);

    &:last-child {
      border-bottom: none;
    }

    .step-name {
      flex: 1;
      font-size: 14px;
    }

    .step-time {
      font-size: 12px;
      color: var(--color-text-placeholder);
    }

    &.done .step-name {
      color: var(--color-text-secondary);
    }
    &.skipped .step-name {
      color: var(--color-text-placeholder);
      text-decoration: line-through;
    }
  }
}

.photo-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;

  .photo-item {
    aspect-ratio: 1;
    border-radius: var(--radius-sm);
    overflow: hidden;
    background: var(--bg-disabled);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
}

.issue-list {
  .issue-row {
    padding: 10px 0;
    border-bottom: 1px solid var(--border-color-light);

    &:last-child {
      border-bottom: none;
    }

    &__head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 6px;
    }

    &__desc {
      font-size: 13px;
      color: var(--color-text-regular);
      line-height: 1.6;
    }

    &__time {
      font-size: 11px;
      color: var(--color-text-placeholder);
      margin-top: 4px;
      text-align: right;
    }
  }
}

.loading {
  text-align: center;
  padding: 60px 0;
}
</style>
