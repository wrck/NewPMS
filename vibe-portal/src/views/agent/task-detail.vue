<template>
  <div class="task-detail">
    <van-skeleton v-if="loading && !detail" title :row="8" />

    <template v-if="detail">
      <!-- 任务基本信息 -->
      <section class="card">
        <div class="flex-between">
          <span class="task-no">{{ detail.taskCode }}</span>
          <StatusTag :label="statusInfo.label" :type="statusInfo.type" plain />
        </div>
        <h2 class="task-site">{{ detail.siteName }}</h2>
        <p class="task-project">{{ detail.projectName }}</p>

        <ul class="info-grid">
          <li v-if="detail.pmName">
            <span class="label">PM</span>
            <span>{{ detail.pmName }}</span>
          </li>
          <li v-if="detail.executorName">
            <span class="label">执行人</span>
            <span>{{ detail.executorName }}</span>
          </li>
          <li v-if="detail.deadline">
            <span class="label">截止时间</span>
            <span>{{ formatDate(detail.deadline) }}</span>
          </li>
          <li v-if="detail.address">
            <span class="label">地址</span>
            <span class="ellipsis-2">{{ detail.address }}</span>
          </li>
          <li v-if="detail.contactName">
            <span class="label">联系人</span>
            <span>{{ detail.contactName }} {{ detail.contactPhone || '' }}</span>
          </li>
          <li v-if="detail.planStartTime">
            <span class="label">计划开始</span>
            <span>{{ formatDate(detail.planStartTime) }}</span>
          </li>
          <li v-if="detail.planEndTime">
            <span class="label">计划结束</span>
            <span>{{ formatDate(detail.planEndTime) }}</span>
          </li>
        </ul>

        <div v-if="detail.percent != null" class="progress-block">
          <div class="flex-between">
            <span class="label">完成进度</span>
            <span class="percent">{{ detail.percent }}%</span>
          </div>
          <ProgressBar :percent="detail.percent" variant="agent" :show-label="false" />
        </div>
      </section>

      <!-- 任务要求 -->
      <section v-if="detail.description || detail.requirement" class="card">
        <div class="section-title">任务要求</div>
        <p class="section-text">{{ detail.description || detail.requirement }}</p>
      </section>

      <!-- 交付物要求 -->
      <section v-if="detail.deliverableRequirements?.length" class="card">
        <div class="section-title">交付物要求</div>
        <ul class="req-list">
          <li v-for="r in detail.deliverableRequirements" :key="r.id">
            <van-icon :name="r.required ? 'certificate' : 'circle'" :color="r.required ? '#fa8c16' : '#8c8c8c'" />
            <span>{{ r.name }}</span>
            <van-tag v-if="r.required" type="danger">必传</van-tag>
            <span v-if="r.minCount" class="min-count">至少{{ r.minCount }}项</span>
          </li>
        </ul>
      </section>

      <!-- 已提交交付物 -->
      <section v-if="deliverables.length" class="card">
        <div class="section-title">已提交交付物</div>
        <div v-for="d in deliverables" :key="d.id" class="deliverable-block">
          <div class="deliverable-block__head">
            <span class="deliverable-time">{{ d.submitTime ? formatDateTime(d.submitTime) : '未提交' }}</span>
            <StatusTag
              v-if="d.status"
              :label="deliverableStatusText(d.status)"
              :type="deliverableStatusType(d.status)"
              plain
            />
          </div>
          <!-- 施工照片 -->
          <div v-if="d.photos?.length" class="sub-section">
            <div class="sub-title">施工照片（{{ d.photos.length }}）</div>
            <div class="thumb-grid">
              <img
                v-for="(url, i) in d.photos"
                :key="i"
                :src="url"
                class="thumb"
                @click="onPreviewPhotos(d.photos || [], i)"
              />
            </div>
          </div>
          <!-- 测试记录 -->
          <div v-if="d.testRecords?.length" class="sub-section">
            <div class="sub-title">测试记录</div>
            <ul class="file-list">
              <li v-for="(url, i) in d.testRecords" :key="i" @click="onOpenFile(url)">
                <van-icon name="description" />
                <span class="ellipsis">{{ fileName(url) }}</span>
                <van-icon name="eye-o" color="#1677ff" />
              </li>
            </ul>
          </div>
          <!-- 签收单 -->
          <div v-if="d.receipts?.length" class="sub-section">
            <div class="sub-title">签收单</div>
            <ul class="file-list">
              <li v-for="(url, i) in d.receipts" :key="i" @click="onOpenFile(url)">
                <van-icon name="description" />
                <span class="ellipsis">{{ fileName(url) }}</span>
                <van-icon name="eye-o" color="#1677ff" />
              </li>
            </ul>
          </div>
          <p v-if="d.remark" class="deliverable-remark">备注：{{ d.remark }}</p>
        </div>
      </section>
    </template>

    <!-- 底部操作 -->
    <div v-if="detail" class="bottom-action">
      <template v-if="detail.status === 'PENDING'">
        <van-button block type="default" plain @click="onReject">拒绝</van-button>
        <van-button block type="primary" @click="onAccept">接单</van-button>
      </template>
      <van-button
        v-else-if="detail.status === 'ACCEPTED'"
        block
        type="primary"
        @click="onAssign"
      >
        指派工程师
      </van-button>
      <van-button
        v-else-if="['IN_PROGRESS', 'REJECTED'].includes(detail.status)"
        block
        type="primary"
        @click="goSubmit"
      >
        提交交付物
      </van-button>
    </div>

    <!-- 指派工程师弹窗 -->
    <van-dialog
      v-model:show="showAssignDialog"
      title="指派工程师"
      show-cancel-button
      :before-close="onAssignConfirm"
    >
      <div class="assign-form">
        <van-field
          v-model="assignForm.engineerName"
          placeholder="请输入工程师姓名"
          label="工程师"
          label-width="60"
          clearable
        />
        <van-field
          v-model="assignForm.engineerId"
          placeholder="选填，工程师ID"
          label="ID"
          label-width="60"
          clearable
        />
      </div>
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog, showImagePreview } from 'vant'
import dayjs from 'dayjs'
import ProgressBar from '@/components/ProgressBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  getOutsourceTaskDetail,
  getDeliverables,
  acceptTask,
  rejectTask,
  assignEngineer
} from '@/api/agent'
import {
  OutsourceTaskStatusMap,
  type OutsourceTaskDetailVO,
  type OutsourceDeliverableVO
} from '@/types/api'

const route = useRoute()
const router = useRouter()

const detail = ref<OutsourceTaskDetailVO | undefined>()
const deliverables = ref<OutsourceDeliverableVO[]>([])
const loading = ref(false)
const showAssignDialog = ref(false)
const assignForm = ref({ engineerName: '', engineerId: '' })

const statusInfo = computed(() =>
  detail.value
    ? OutsourceTaskStatusMap[detail.value.status]
    : { label: '', type: 'default' as const }
)

const taskId = computed(() => route.params.id as string)

async function fetchDetail() {
  loading.value = true
  try {
    detail.value = await getOutsourceTaskDetail(taskId.value)
    // 同步拉取交付物列表
    fetchDeliverables()
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function fetchDeliverables() {
  try {
    deliverables.value = (await getDeliverables(taskId.value)) || []
  } catch (e) {
    deliverables.value = []
  }
}

function formatDate(d?: string): string {
  return d ? dayjs(d).format('YYYY-MM-DD') : '-'
}

function formatDateTime(d?: string): string {
  return d ? dayjs(d).format('YYYY-MM-DD HH:mm') : '-'
}

function fileName(url: string): string {
  return decodeURIComponent(url.split('/').pop() || '文件')
}

function onPreviewPhotos(urls: string[], start: number) {
  showImagePreview({ images: urls, startPosition: start })
}

function onOpenFile(url: string) {
  window.open(url, '_blank')
}

function deliverableStatusText(s: NonNullable<OutsourceDeliverableVO['status']>): string {
  return { DRAFT: '草稿', SUBMITTED: '已提交', CONFIRMED: '已确认', REJECTED: '已驳回' }[s]
}

function deliverableStatusType(s: NonNullable<OutsourceDeliverableVO['status']>): 'default' | 'warning' | 'success' | 'danger' {
  return ({ DRAFT: 'default', SUBMITTED: 'warning', CONFIRMED: 'success', REJECTED: 'danger' } as const)[s]
}

async function onAccept() {
  showConfirmDialog({ title: '确认接单', message: '确定接受该任务吗？' })
    .then(async () => {
      try {
        await acceptTask(taskId.value)
        showToast('接单成功')
        fetchDetail()
      } catch (e) {
        // 拦截器已提示
      }
    })
    .catch(() => {})
}

async function onReject() {
  showConfirmDialog({ title: '确认拒绝', message: '确定拒绝该任务吗？' })
    .then(async () => {
      try {
        await rejectTask(taskId.value)
        showToast('已拒绝')
        router.back()
      } catch (e) {
        // 拦截器已提示
      }
    })
    .catch(() => {})
}

function onAssign() {
  assignForm.value = { engineerName: '', engineerId: '' }
  showAssignDialog.value = true
}

async function onAssignConfirm(action: string): Promise<boolean> {
  if (action !== 'confirm') return true
  if (!assignForm.value.engineerName.trim()) {
    showToast('请输入工程师姓名')
    return false
  }
  try {
    await assignEngineer(taskId.value, {
      engineerName: assignForm.value.engineerName.trim(),
      engineerId: assignForm.value.engineerId.trim() || undefined
    })
    showToast('指派成功')
    fetchDetail()
    return true
  } catch (e) {
    return false
  }
}

function goSubmit() {
  router.push(`/agent/tasks/${taskId.value}/submit`)
}

onMounted(fetchDetail)
</script>

<style lang="scss" scoped>
.task-detail {
  padding: 12px;
  min-height: 100%;
  padding-bottom: calc(72px + var(--safe-bottom));

  .section-title {
    font-size: 15px;
    font-weight: 600;
    margin-bottom: 10px;
  }

  .section-text {
    font-size: 13px;
    color: var(--color-text-regular);
    line-height: 1.6;
  }
}

.task-no {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.task-site {
  margin-top: 6px;
  font-size: 18px;
  font-weight: 600;
}

.task-project {
  margin-top: 4px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.info-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px 8px;

  li {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;

    .label {
      font-size: 12px;
      color: var(--color-text-secondary);
    }
    span:last-child {
      font-size: 13px;
      color: var(--color-text-primary);
    }
  }
}

.progress-block {
  margin-top: 12px;

  .label {
    font-size: 13px;
    color: var(--color-text-secondary);
  }
  .percent {
    font-size: 14px;
    font-weight: 600;
    color: var(--agent-primary);
  }
}

.req-list {
  li {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 0;
    font-size: 13px;
    color: var(--color-text-regular);

    .min-count {
      margin-left: auto;
      font-size: 12px;
      color: var(--color-text-secondary);
    }
  }
}

.deliverable-block {
  padding: 12px 0;
  border-top: 1px solid var(--border-color-light);

  &:first-child {
    border-top: none;
    padding-top: 0;
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;

    .deliverable-time {
      font-size: 12px;
      color: var(--color-text-secondary);
    }
  }
}

.sub-section {
  margin-top: 8px;

  .sub-title {
    font-size: 12px;
    color: var(--color-text-secondary);
    margin-bottom: 6px;
  }
}

.thumb-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;

  .thumb {
    width: 100%;
    aspect-ratio: 1 / 1;
    object-fit: cover;
    border-radius: 6px;
    background: #f5f5f5;
  }
}

.file-list {
  li {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 0;
    border-bottom: 1px solid var(--border-color-light);
    font-size: 13px;

    span {
      flex: 1;
      min-width: 0;
    }
  }
}

.deliverable-remark {
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.assign-form {
  padding: 12px 16px 4px;
}
</style>
