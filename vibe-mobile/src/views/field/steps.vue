<script setup lang="ts">
/**
 * 施工步骤跟踪页（设计文档 3.4.2）
 * 使用 StepTracker 组件逐步跟踪施工进度
 * 流程：加载步骤 → 逐步完成（拍照+备注）→ 拍照上传 → 全部完成跳转完成确认
 *
 * 后端：
 *   - GET /work-orders/{workOrderId}/steps  步骤列表
 *   - POST /work-orders/{workOrderId}/steps/{stepId}/complete  完成步骤
 *   - POST /work-orders/{workOrderId}/photos  上传步骤照片（multipart）
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showSuccessToast, showLoadingToast, closeToast } from 'vant'
import {
  fetchTaskDetail,
  fetchWorkSteps,
  completeStep,
  completeWorkOrder,
  uploadWorkOrderPhoto
} from '@/api'
import {
  WorkOrderStatusMap,
  type WorkOrderStepVO,
  type WorkOrderVO
} from '@/types/api'
import StepTracker from '@/components/StepTracker/index.vue'
import UploadQueue from '@/components/UploadQueue/index.vue'
import type { CapturedPhoto } from '@/components/PhotoCapture/index.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const task = ref<WorkOrderVO | null>(null)
const steps = ref<WorkOrderStepVO[]>([])
const loading = ref(false)
const submittingStepId = ref<string | number>('')

const trackerRef = ref<InstanceType<typeof StepTracker> | null>(null)

const uploaderName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || ''
)

async function loadData() {
  loading.value = true
  try {
    const taskId = route.params.taskId as string
    const [t, s] = await Promise.allSettled([fetchTaskDetail(taskId), fetchWorkSteps(taskId)])
    if (t.status === 'fulfilled') task.value = t.value
    else task.value = null
    if (s.status === 'fulfilled') {
      steps.value = (s.value || []).map((step) => ({
        ...step,
        // StepTracker 兼容字段：默认需要拍照
        needPhoto: step.needPhoto ?? true,
        minPhotoCount: step.minPhotoCount ?? 1
      }))
    } else {
      steps.value = []
    }
  } finally {
    loading.value = false
  }
}

/**
 * StepTracker emit 'complete' 事件回调
 * 调用后端 completeStep 接口 + 上传照片
 */
async function onStepComplete(step: WorkOrderStepVO, photos: CapturedPhoto[]) {
  if (!task.value) return
  submittingStepId.value = step.id
  showLoadingToast({ message: '提交中...', forbidClick: true, duration: 0 })
  try {
    // 1. 调用后端标记步骤完成
    await completeStep(task.value.id, step.id, {
      remark: step.remark
    })

    // 2. 上传该步骤的照片（如果有）
    if (photos.length > 0) {
      for (const photo of photos) {
        try {
          await uploadWorkOrderPhoto(task.value.id, {
            file: photo.blob,
            stepId: step.id,
            longitude: photo.longitude,
            latitude: photo.latitude,
            address: photo.address,
            takenTime: photo.capturedAt
          })
        } catch (e) {
          // 单张失败不阻断（已缓存到上传队列）
          console.warn('[steps] upload photo failed:', e)
        }
      }
    }

    closeToast()
    showSuccessToast(`步骤「${step.stepName}」已完成`)
    // 刷新步骤列表
    await refreshSteps()
  } catch (e) {
    closeToast()
    // request 拦截器已 toast 错误
  } finally {
    submittingStepId.value = ''
  }

  if (trackerRef.value?.allDone) {
    showSuccessToast('所有步骤已完成')
  }
}

async function refreshSteps() {
  if (!task.value) return
  try {
    const list = await fetchWorkSteps(task.value.id)
    steps.value = (list || []).map((step) => ({
      ...step,
      needPhoto: step.needPhoto ?? true,
      minPhotoCount: step.minPhotoCount ?? 1
    }))
  } catch (e) {
    /* ignore */
  }
}

function onAllComplete() {
  showSuccessToast('所有步骤已完成')
}

/** 跳转异常上报 */
function goIssue() {
  if (!task.value) return
  router.push(`/field/issue/${task.value.id}`)
}

/** 跳转完成确认 */
async function goComplete() {
  if (!task.value) return
  if (!trackerRef.value?.allDone) {
    showToast('请先完成所有施工步骤')
    return
  }
  // 调用后端工程师标记完成（可失败，弱网允许继续）
  try {
    await completeWorkOrder(task.value.id, '工程师现场完成')
  } catch (e) {
    // 弱网忽略
  }
  router.push(`/field/complete/${task.value.id}`)
}

const allDone = computed(() => trackerRef.value?.allDone ?? false)

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="steps-page page">
    <van-loading v-if="loading" type="spinner" class="loading" />

    <template v-else-if="task">
      <!-- 任务摘要 -->
      <div class="card summary">
        <div class="summary__title">{{ task.projectName || '工单' }}{{ task.taskName ? ` - ${task.taskName}` : '' }}</div>
        <div class="summary__meta">
          <span
            class="status-tag"
            :style="{ color: WorkOrderStatusMap[task.status]?.color, borderColor: WorkOrderStatusMap[task.status]?.color }"
          >
            {{ WorkOrderStatusMap[task.status]?.label || task.status }}
          </span>
        </div>
      </div>

      <!-- 步骤指示 -->
      <van-steps :active="1" active-color="#1677ff">
        <van-step>签到</van-step>
        <van-step>施工</van-step>
        <van-step>确认</van-step>
        <van-step>签退</van-step>
      </van-steps>

      <!-- 施工步骤跟踪 -->
      <div class="section-title">📋 施工步骤</div>
      <StepTracker
        ref="trackerRef"
        :steps="steps"
        :uploader="uploaderName"
        @complete="onStepComplete"
        @all-complete="onAllComplete"
      />

      <!-- 上传队列（弱网续传） -->
      <div class="section-title">📤 上传队列</div>
      <div class="card">
        <UploadQueue
          :biz-id="String(task.id)"
          biz-type="work_order"
          :uploader="uploaderName"
        />
        <p class="tip">弱网环境下照片将自动缓存，网络恢复后自动续传</p>
      </div>

      <!-- 底部操作 -->
      <div class="bottom-action">
        <van-button type="warning" plain class="touchable" @click="goIssue">
          ⚠️ 上报异常
        </van-button>
        <van-button
          type="primary"
          class="touchable"
          :disabled="!allDone || submittingStepId !== ''"
          @click="goComplete"
        >
          {{ allDone ? '下一步：完成确认' : '请完成所有步骤' }}
        </van-button>
      </div>
    </template>

    <van-empty v-else description="任务不存在" />
  </div>
</template>

<style scoped lang="scss">
.steps-page {
  padding: 12px 12px 100px;
}

.section-title {
  margin: 16px 4px 8px;
  font-size: 15px;
  font-weight: 600;
}

.summary {
  &__title {
    font-size: 16px;
    font-weight: 700;
  }

  &__meta {
    margin-top: 8px;
  }

  .status-tag {
    display: inline-block;
    padding: 4px 12px;
    border-radius: 12px;
    border: 1px solid;
    font-size: 12px;
    font-weight: 600;
  }
}

.tip {
  font-size: 12px;
  color: var(--color-text-placeholder);
  margin-top: 8px;
  line-height: 1.6;
}

.loading {
  text-align: center;
  padding: 60px 0;
}
</style>
