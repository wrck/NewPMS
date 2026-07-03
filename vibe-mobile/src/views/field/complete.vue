<script setup lang="ts">
/**
 * 完成确认页 + 签退（设计文档 3.4.2）
 * 流程：展示完成摘要 → 工程师标记完成 → 签退（GPS+拍照 multipart 上传）→ 返回首页
 *
 * 后端：
 *   - POST /work-orders/{id}/complete?remark=xxx  工程师标记完成
 *   - POST /work-orders/{id}/checkout  (multipart/form-data)
 *     字段：location{longitude,latitude,address,timeText} + remark + photo(MultipartFile)
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  showSuccessToast,
  showFailToast,
  showLoadingToast,
  closeToast,
  showConfirmDialog
} from 'vant'
import {
  fetchTaskDetail,
  fetchWorkSteps,
  completeWorkOrder,
  checkOut
} from '@/api'
import {
  WorkOrderStatusMap,
  type GpsLocation as ApiGpsLocation,
  type WorkOrderStepVO,
  type WorkOrderVO
} from '@/types/api'
import GpsLocation from '@/components/GpsLocation/index.vue'
import PhotoCapture, { type CapturedPhoto } from '@/components/PhotoCapture/index.vue'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const task = ref<WorkOrderVO | null>(null)
const steps = ref<WorkOrderStepVO[]>([])
const loading = ref(false)
const submitting = ref(false)

/** 阶段：CONFIRM(确认) → SIGN_OUT(签退) → DONE(完成) */
type Stage = 'CONFIRM' | 'SIGN_OUT' | 'DONE'
const stage = ref<Stage>('CONFIRM')

const gpsRef = ref<InstanceType<typeof GpsLocation> | null>(null)
const photos = ref<CapturedPhoto[]>([])

const uploaderName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || ''
)

/** 步骤完成判定：兼容后端 COMPLETED 与旧版 DONE */
function isStepDone(step: WorkOrderStepVO): boolean {
  const status = step.status as string
  return status === 'COMPLETED' || status === 'DONE'
}

const completedCount = computed(() => steps.value.filter((s) => isStepDone(s)).length)
const totalCount = computed(() => steps.value.length)

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
        // StepTracker 兼容字段
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

function onPhotoChange(list: CapturedPhoto[]) {
  photos.value = list
}

/** 确认完成 → 调用 completeWorkOrder → 进入签退 */
async function onConfirm() {
  if (!task.value) return
  try {
    await showConfirmDialog({
      title: '完成确认',
      message: `确认所有 ${totalCount.value} 个步骤已完成？`
    })
    submitting.value = true
    showLoadingToast({ message: '提交中...', forbidClick: true, duration: 0 })
    // 调用后端工程师标记完成（弱网允许继续）
    try {
      await completeWorkOrder(task.value.id, '工程师现场完成')
    } catch (e) {
      // 弱网忽略，进入签退环节
    }
    closeToast()
    stage.value = 'SIGN_OUT'
  } catch (e) {
    // 取消
  } finally {
    submitting.value = false
  }
}

/** 签退：multipart 上传 location + photo */
async function onSignOut() {
  if (!task.value || !gpsRef.value) return
  const loc = await gpsRef.value.locate()
  if (!loc) {
    showFailToast('定位失败，请重试')
    return
  }
  if (photos.value.length === 0) {
    showFailToast('请至少拍摄 1 张签退照片')
    return
  }
  submitting.value = true
  showLoadingToast({ message: '签退中...', forbidClick: true, duration: 0 })
  try {
    const location: ApiGpsLocation = {
      longitude: loc.longitude,
      latitude: loc.latitude,
      address: loc.address,
      timeText: dayjs().format('YYYY-MM-DD HH:mm:ss')
    }
    // 取第一张照片作为签退照片（已在 PhotoCapture 中完成压缩+水印）
    const photo = photos.value[0]
    await checkOut(task.value.id, {
      location,
      remark: `现场签退（${photos.value.length} 张照片）`,
      photo: photo.blob
    })
    closeToast()
    showSuccessToast('签退成功，任务已完成')
    stage.value = 'DONE'
    setTimeout(() => router.replace('/home'), 1500)
  } catch (e) {
    closeToast()
    // request 拦截器已 toast 错误；弱网允许完成
    if (!navigator.onLine) {
      showSuccessToast('已离线签退，任务已完成')
      stage.value = 'DONE'
      setTimeout(() => router.replace('/home'), 1500)
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="complete-page page">
    <van-loading v-if="loading" type="spinner" class="loading" />

    <template v-else-if="task">
      <!-- 步骤指示 -->
      <van-steps :active="stage === 'CONFIRM' ? 2 : 3" active-color="#1677ff">
        <van-step>签到</van-step>
        <van-step>施工</van-step>
        <van-step>确认</van-step>
        <van-step>签退</van-step>
      </van-steps>

      <!-- 阶段：完成确认 -->
      <template v-if="stage === 'CONFIRM'">
        <div class="card confirm-card">
          <van-icon name="checked" size="56" color="#52c41a" />
          <div class="confirm-card__title">任务完成确认</div>
          <div class="confirm-card__subtitle">
            {{ task.projectName || '工单' }}{{ task.taskName ? ` - ${task.taskName}` : '' }}
          </div>
          <div class="confirm-card__info">
            <div class="info-row">
              <span class="label">已完成步骤</span>
              <span class="value success">{{ completedCount }}/{{ totalCount }}</span>
            </div>
            <div v-if="task.totalStepCount != null" class="info-row">
              <span class="label">后端统计</span>
              <span class="value">
                {{ task.completedStepCount ?? 0 }}/{{ task.totalStepCount }}
              </span>
            </div>
            <div class="info-row">
              <span class="label">任务状态</span>
              <span
                class="value"
                :style="{ color: WorkOrderStatusMap[task.status]?.color }"
              >
                {{ WorkOrderStatusMap[task.status]?.label || task.status }}
              </span>
            </div>
          </div>
        </div>

        <!-- 步骤清单 -->
        <div class="section-title">步骤清单</div>
        <div class="card">
          <div
            v-for="step in steps"
            :key="step.id"
            class="step-row"
            :class="{ done: isStepDone(step) }"
          >
            <van-icon
              :name="isStepDone(step) ? 'success' : 'circle'"
              :color="isStepDone(step) ? '#52c41a' : '#bfbfbf'"
            />
            <span class="step-name">{{ step.stepNo }}. {{ step.stepName }}</span>
            <span v-if="step.completedTime" class="step-time">{{ step.completedTime }}</span>
          </div>
          <van-empty v-if="steps.length === 0" description="暂无步骤数据" />
        </div>

        <div class="bottom-action">
          <van-button plain class="touchable" @click="router.back()">返回修改</van-button>
          <van-button
            type="primary"
            class="touchable"
            :loading="submitting"
            @click="onConfirm"
          >
            确认完成
          </van-button>
        </div>
      </template>

      <!-- 阶段：签退 -->
      <template v-else-if="stage === 'SIGN_OUT'">
        <div class="card confirm-card">
          <van-icon name="sign" size="56" color="#1677ff" />
          <div class="confirm-card__title">签退</div>
          <div class="confirm-card__subtitle">拍摄现场照片完成签退</div>
        </div>

        <!-- GPS 定位 -->
        <div class="section-title">签退定位</div>
        <GpsLocation
          ref="gpsRef"
          :target-longitude="task.checkinLocation?.longitude"
          :target-latitude="task.checkinLocation?.latitude"
          :allow-radius="500"
        />

        <!-- 签退拍照 -->
        <div class="section-title">签退照片</div>
        <div class="card">
          <PhotoCapture
            :max-count="3"
            :watermark="true"
            :uploader="uploaderName"
            @change="onPhotoChange"
          />
          <p class="tip">需拍摄现场完成情况照片（至少 1 张）</p>
        </div>

        <div class="bottom-action">
          <van-button
            type="primary"
            block
            round
            :loading="submitting"
            class="touchable"
            @click="onSignOut"
          >
            确认签退
          </van-button>
        </div>
      </template>

      <!-- 阶段：已完成 -->
      <template v-else>
        <van-empty description="任务已完成，即将返回首页">
          <van-button type="primary" class="touchable" @click="router.replace('/home')">
            返回首页
          </van-button>
        </van-empty>
      </template>
    </template>

    <van-empty v-else description="任务不存在" />
  </div>
</template>

<style scoped lang="scss">
.complete-page {
  padding: 12px 12px 100px;
}

.section-title {
  margin: 16px 4px 8px;
  font-size: 15px;
  font-weight: 600;
}

.confirm-card {
  text-align: center;
  padding: 28px 20px;

  &__title {
    font-size: 18px;
    font-weight: 700;
    margin-top: 12px;
  }

  &__subtitle {
    font-size: 13px;
    color: var(--color-text-secondary);
    margin-top: 6px;
  }

  &__info {
    margin-top: 20px;
    text-align: left;
  }
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-color-light);

  &:last-child {
    border-bottom: none;
  }

  .label {
    font-size: 13px;
    color: var(--color-text-secondary);
  }

  .value {
    font-size: 14px;
    font-weight: 600;

    &.success {
      color: var(--color-success);
    }
  }
}

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
