<script setup lang="ts">
/**
 * 现场签到页（设计文档 3.4.2）
 * 流程：GPS 定位 → 校验范围 → 拍照（带水印）→ 提交签到
 * 后端：POST /work-orders/{id}/checkin (multipart/form-data)
 *      字段：location{longitude,latitude,address,timeText} + remark + photo(MultipartFile)
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showFailToast, showLoadingToast, closeToast } from 'vant'
import { fetchTaskDetail, checkIn } from '@/api'
import {
  WorkOrderStatusMap,
  type GpsLocation as ApiGpsLocation,
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
const loading = ref(false)
const submitting = ref(false)
const photos = ref<CapturedPhoto[]>([])

const gpsRef = ref<InstanceType<typeof GpsLocation> | null>(null)

const uploaderName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || ''
)

const inRange = computed(() => gpsRef.value?.inRange ?? false)

async function loadTask() {
  loading.value = true
  try {
    const taskId = route.params.taskId as string
    task.value = await fetchTaskDetail(taskId)
  } catch (e) {
    task.value = null
  } finally {
    loading.value = false
  }
}

function onPhotoChange(list: CapturedPhoto[]) {
  photos.value = list
}

/** 提交签到：multipart 上传 location + photo */
async function onCheckIn() {
  if (!task.value || !gpsRef.value) return
  const loc = await gpsRef.value.locate()
  if (!loc) {
    showFailToast('定位失败，请重试')
    return
  }
  if (photos.value.length === 0) {
    showFailToast('请至少拍摄 1 张现场照片')
    return
  }
  submitting.value = true
  showLoadingToast({ message: '签到中...', forbidClick: true, duration: 0 })
  try {
    const location: ApiGpsLocation = {
      longitude: loc.longitude,
      latitude: loc.latitude,
      address: loc.address,
      timeText: dayjs().format('YYYY-MM-DD HH:mm:ss')
    }
    // 取第一张照片作为签到照片（已在 PhotoCapture 中完成压缩+水印）
    const photo = photos.value[0]
    await checkIn(task.value.id, {
      location,
      remark: `现场签到（${photos.value.length} 张照片）`,
      photo: photo.blob
    })
    closeToast()
    showSuccessToast('签到成功')
    setTimeout(() => {
      router.replace(`/field/steps/${task.value!.id}`)
    }, 1000)
  } catch (e) {
    closeToast()
    // request 拦截器已 toast 错误
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadTask()
})
</script>

<template>
  <div class="checkin-page page">
    <van-loading v-if="loading" type="spinner" class="loading" />

    <template v-else-if="task">
      <!-- 任务摘要 -->
      <div class="card summary">
        <div class="summary__title">{{ task.projectName || '工单' }}</div>
        <div v-if="task.taskName" class="summary__site">{{ task.taskName }}</div>
        <div v-if="task.checkinLocation?.address" class="summary__addr">📍 {{ task.checkinLocation.address }}</div>
        <div class="summary__status">
          <span
            class="status-tag"
            :style="{ color: WorkOrderStatusMap[task.status]?.color, borderColor: WorkOrderStatusMap[task.status]?.color }"
          >
            {{ WorkOrderStatusMap[task.status]?.label || task.status }}
          </span>
        </div>
      </div>

      <!-- 步骤指示 -->
      <van-steps :active="0" active-color="#1677ff">
        <van-step>签到</van-step>
        <van-step>施工</van-step>
        <van-step>确认</van-step>
        <van-step>签退</van-step>
      </van-steps>

      <!-- GPS 定位 -->
      <div class="section-title">📍 现场定位</div>
      <GpsLocation
        ref="gpsRef"
        :target-longitude="task.checkinLocation?.longitude"
        :target-latitude="task.checkinLocation?.latitude"
        :allow-radius="500"
      />

      <!-- 拍照 -->
      <div class="section-title">📷 现场拍照</div>
      <div class="card">
        <PhotoCapture
          :max-count="3"
          :watermark="true"
          :uploader="uploaderName"
          @change="onPhotoChange"
        />
        <p class="tip">需拍摄现场环境照片（至少 1 张），将自动添加时间/GPS/上传人水印</p>
      </div>

      <!-- 底部操作 -->
      <div class="bottom-action">
        <van-button
          type="primary"
          block
          round
          :loading="submitting"
          class="touchable"
          @click="onCheckIn"
        >
          确认签到
        </van-button>
      </div>
    </template>

    <van-empty v-else description="任务不存在" />
  </div>
</template>

<style scoped lang="scss">
.checkin-page {
  padding: 12px 12px 100px;
}

.section-title {
  margin: 16px 4px 8px;
  font-size: 15px;
  font-weight: 600;
}

.summary {
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

  &__addr {
    font-size: 13px;
    color: var(--color-text-regular);
    margin-top: 8px;
  }

  &__status {
    margin-top: 10px;
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
  margin-top: 10px;
  font-size: 12px;
  color: var(--color-text-placeholder);
  line-height: 1.6;
}

.loading {
  text-align: center;
  padding: 60px 0;
}
</style>
