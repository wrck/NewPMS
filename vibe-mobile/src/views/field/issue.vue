<script setup lang="ts">
/**
 * 异常上报页（设计文档 3.4.2）
 * 表单：异常类型 + 严重程度 + 问题描述 + 现场照片
 * 提交后通知 PM 和技术主管
 *
 * 后端：
 *   - POST /work-orders/{workOrderId}/issues  body: WorkOrderIssueReportDTO
 *     字段：issueType / severity(MINOR|MAJOR|BLOCKING) / description / photos[] / remark
 *   - POST /work-orders/{workOrderId}/photos  上传异常现场照片（multipart）
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showFailToast, showLoadingToast, closeToast } from 'vant'
import {
  fetchTaskDetail,
  reportIssue,
  uploadWorkOrderPhoto
} from '@/api'
import type {
  IssueSeverity,
  WorkOrderIssueReportDTO,
  WorkOrderVO
} from '@/types/api'
import PhotoCapture, { type CapturedPhoto } from '@/components/PhotoCapture/index.vue'
import UploadQueue from '@/components/UploadQueue/index.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const task = ref<WorkOrderVO | null>(null)
const loading = ref(false)
const submitting = ref(false)

/** 异常类型（对齐后端 issueType 枚举） */
const issueTypes = [
  { label: '设备故障', value: 'DEVICE_FAULT' },
  { label: '配置错误', value: 'CONFIG_ERROR' },
  { label: '现场问题', value: 'SITE_ISSUE' },
  { label: '其他', value: 'OTHER' }
]

/** 严重程度（对齐后端 IssueSeverity：MINOR/MAJOR/BLOCKING） */
const severityOptions: { label: string; value: IssueSeverity; color: string; desc: string }[] = [
  { label: '轻微', value: 'MINOR', color: '#faad14', desc: '不影响整体进度' },
  { label: '严重', value: 'MAJOR', color: '#ff4d4f', desc: '影响部分进度' },
  { label: '阻断', value: 'BLOCKING', color: '#a8071a', desc: '无法继续施工' }
]

const form = reactive<WorkOrderIssueReportDTO>({
  issueType: 'DEVICE_FAULT',
  severity: 'MINOR',
  description: '',
  photos: [],
  remark: ''
})

const photos = ref<CapturedPhoto[]>([])

const uploaderName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || ''
)

const showTypePicker = ref(false)
const issueTypeText = computed(
  () => issueTypes.find((t) => t.value === form.issueType)?.label || '设备故障'
)

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

function onTypeConfirm({ selectedOptions }: { selectedOptions: any[] }) {
  form.issueType = selectedOptions[0]?.value || 'DEVICE_FAULT'
  showTypePicker.value = false
}

function onPhotoChange(list: CapturedPhoto[]) {
  photos.value = list
}

/** 提交异常上报：先上传照片 → 再调用 reportIssue */
async function onSubmit() {
  if (!task.value) return
  if (!form.description || form.description.trim().length < 5) {
    showFailToast('请填写问题描述（至少 5 个字）')
    return
  }
  submitting.value = true
  showLoadingToast({ message: '提交中...', forbidClick: true, duration: 0 })
  try {
    // 1. 上传现场照片到工单照片端点，收集返回的 photoUrl
    const photoUrls: string[] = []
    for (const photo of photos.value) {
      try {
        const uploaded = await uploadWorkOrderPhoto(task.value.id, {
          file: photo.blob,
          longitude: photo.longitude,
          latitude: photo.latitude,
          address: photo.address,
          takenTime: photo.capturedAt
        })
        if (uploaded?.photoUrl) photoUrls.push(uploaded.photoUrl)
      } catch (e) {
        // 单张失败不阻断（已缓存到上传队列）
        console.warn('[issue] upload photo failed:', e)
      }
    }
    form.photos = photoUrls

    // 2. 调用异常上报接口
    await reportIssue(task.value.id, {
      issueType: form.issueType,
      severity: form.severity,
      description: form.description,
      photos: photoUrls,
      remark: form.remark
    })
    closeToast()
    showSuccessToast('异常已上报，已通知 PM 和技术主管')
    setTimeout(() => router.back(), 1500)
  } catch (e) {
    closeToast()
    // request 拦截器已 toast 错误；弱网允许提示离线
    if (!navigator.onLine) {
      showSuccessToast('已离线提交，网络恢复后同步')
      setTimeout(() => router.back(), 1500)
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadTask()
})
</script>

<template>
  <div class="issue-page page">
    <van-loading v-if="loading" type="spinner" class="loading" />

    <template v-else-if="task">
      <!-- 任务摘要 -->
      <div class="card summary">
        <van-icon name="warning-o" size="22" color="#ff4d4f" />
        <div class="summary__text">
          <div class="summary__title">{{ task.projectName || '工单' }}</div>
          <div v-if="task.taskName" class="summary__desc">{{ task.taskName }}</div>
          <div class="summary__desc">异常上报后将自动通知 PM 和技术主管</div>
        </div>
      </div>

      <!-- 表单 -->
      <van-cell-group inset title="异常信息">
        <van-cell title="异常类型" :value="issueTypeText" is-link @click="showTypePicker = true" />
        <van-cell title="影响程度">
          <template #value>
            <van-radio-group v-model="form.severity" direction="horizontal">
              <van-radio
                v-for="opt in severityOptions"
                :key="opt.value"
                :name="opt.value"
                :style="{ color: opt.color }"
              >
                {{ opt.label }}
              </van-radio>
            </van-radio-group>
          </template>
        </van-cell>
        <van-field
          v-model="form.description"
          type="textarea"
          label="问题描述"
          placeholder="请详细描述异常情况（至少 5 个字）"
          rows="4"
          autosize
          maxlength="500"
          show-word-limit
        />
        <van-field
          v-model="form.remark"
          type="textarea"
          label="备注"
          placeholder="选填，补充说明"
          rows="2"
          autosize
          maxlength="200"
        />
      </van-cell-group>

      <!-- 当前严重程度说明 -->
      <div class="severity-tip">
        <van-icon name="info-o" />
        <span>{{
          severityOptions.find((o) => o.value === form.severity)?.desc
        }}</span>
      </div>

      <!-- 现场照片 -->
      <div class="section-title">现场照片</div>
      <div class="card">
        <PhotoCapture
          :max-count="5"
          :watermark="true"
          :uploader="uploaderName"
          @change="onPhotoChange"
        />
        <p class="tip">拍摄异常现场照片，将自动添加时间/GPS/上传人水印</p>
      </div>

      <!-- 上传队列 -->
      <div class="section-title">上传队列</div>
      <div class="card">
        <UploadQueue
          :biz-id="`issue_${task.id}`"
          biz-type="issue"
          :uploader="uploaderName"
        />
      </div>

      <!-- 底部操作 -->
      <div class="bottom-action">
        <van-button plain class="touchable" @click="router.back()">取消</van-button>
        <van-button
          type="danger"
          class="touchable"
          :loading="submitting"
          @click="onSubmit"
        >
          提交上报
        </van-button>
      </div>
    </template>

    <van-empty v-else description="任务不存在" />

    <!-- 类型选择 -->
    <van-popup v-model:show="showTypePicker" position="bottom" round>
      <van-picker
        :columns="issueTypes"
        :columns-field-names="{ text: 'label', value: 'value' }"
        @confirm="onTypeConfirm"
        @cancel="showTypePicker = false"
      />
    </van-popup>
  </div>
</template>

<style scoped lang="scss">
.issue-page {
  padding: 12px 12px 100px;
}

.section-title {
  margin: 16px 4px 8px;
  font-size: 15px;
  font-weight: 600;
}

.summary {
  display: flex;
  align-items: center;
  gap: 10px;

  &__text {
    flex: 1;
  }

  &__title {
    font-size: 15px;
    font-weight: 600;
  }

  &__desc {
    font-size: 12px;
    color: var(--color-text-secondary);
    margin-top: 4px;
  }
}

.severity-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 8px 16px;
  padding: 8px 12px;
  background: rgba(250, 173, 20, 0.08);
  border-radius: var(--radius-sm);
  font-size: 12px;
  color: var(--color-warning);
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
