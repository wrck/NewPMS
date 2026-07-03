<script setup lang="ts">
/**
 * 任务详情页
 * 含 派发 / 转派 / 退回 / 进度更新 操作
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ReloadOutlined, ArrowLeftOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  getTaskDetail,
  dispatchTask,
  transferTask,
  returnTask,
  updateTaskProgress
} from '@/api/project'
import type { ProjectTask, TaskDispatchDTO, TaskTransferDTO, TaskProgressDTO } from '@/types/project'
import {
  TaskStatus,
  TaskStatusTone,
  TaskStatusLabel,
  Priority,
  PriorityLabel
} from '@/types/enum'

const route = useRoute()
const router = useRouter()
const taskId = computed(() => Number(route.params.taskId))

const loading = ref(false)
const detail = ref<ProjectTask | null>(null)

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getTaskDetail(taskId.value)
  } catch (e) {
    console.error('[task.detail] load failed:', e)
  } finally {
    loading.value = false
  }
}

// ============ 派发 ============
const dispatchVisible = ref(false)
const dispatchForm = reactive<TaskDispatchDTO>({
  executeMode: 'SELF',
  assigneeId: undefined,
  agentCompanyId: undefined,
  agentEngineerId: undefined,
  remark: ''
})
const dispatchLoading = ref(false)

function openDispatch() {
  Object.assign(dispatchForm, {
    executeMode: detail.value?.executeMode || 'SELF',
    assigneeId: undefined,
    agentCompanyId: undefined,
    agentEngineerId: undefined,
    remark: ''
  })
  dispatchVisible.value = true
}

async function handleDispatch() {
  if (dispatchForm.executeMode === 'SELF' && !dispatchForm.assigneeId) {
    message.warning('请输入工程师 ID')
    return
  }
  if (dispatchForm.executeMode === 'AGENT' && !dispatchForm.agentCompanyId) {
    message.warning('请输入代理商公司 ID')
    return
  }
  dispatchLoading.value = true
  try {
    await dispatchTask(taskId.value, dispatchForm)
    message.success('任务已派发')
    dispatchVisible.value = false
    loadDetail()
  } catch (e) {
    // ignore
  } finally {
    dispatchLoading.value = false
  }
}

// ============ 转派 ============
const transferVisible = ref(false)
const transferForm = reactive<TaskTransferDTO>({
  toAssigneeId: undefined,
  toAgentEngineerId: undefined,
  reason: ''
})
const transferLoading = ref(false)

function openTransfer() {
  transferForm.toAssigneeId = undefined
  transferForm.toAgentEngineerId = undefined
  transferForm.reason = ''
  transferVisible.value = true
}

async function handleTransfer() {
  if (!transferForm.toAssigneeId && !transferForm.toAgentEngineerId) {
    message.warning('请填写转派目标工程师')
    return
  }
  if (!transferForm.reason.trim()) {
    message.warning('请填写转派原因')
    return
  }
  transferLoading.value = true
  try {
    await transferTask(taskId.value, transferForm)
    message.success('转派成功')
    transferVisible.value = false
    loadDetail()
  } catch (e) {
    // ignore
  } finally {
    transferLoading.value = false
  }
}

// ============ 退回 ============
const returnVisible = ref(false)
const returnForm = reactive({ reason: '' })
const returnLoading = ref(false)

function openReturn() {
  returnForm.reason = ''
  returnVisible.value = true
}

async function handleReturn() {
  if (!returnForm.reason.trim()) {
    message.warning('请填写退回原因')
    return
  }
  returnLoading.value = true
  try {
    await returnTask(taskId.value, { reason: returnForm.reason })
    message.success('任务已退回')
    returnVisible.value = false
    loadDetail()
  } catch (e) {
    // ignore
  } finally {
    returnLoading.value = false
  }
}

// ============ 进度更新 ============
const progressVisible = ref(false)
const progressForm = reactive<TaskProgressDTO>({
  status: TaskStatus.IN_PROGRESS,
  progressPct: 0,
  remark: ''
})
const progressLoading = ref(false)

function openProgress() {
  progressForm.status = detail.value?.status || TaskStatus.IN_PROGRESS
  progressForm.progressPct = detail.value?.progressPct || 0
  progressForm.remark = ''
  progressVisible.value = true
}

async function handleProgress() {
  progressLoading.value = true
  try {
    await updateTaskProgress(taskId.value, progressForm)
    message.success('进度已更新')
    progressVisible.value = false
    loadDetail()
  } catch (e) {
    // ignore
  } finally {
    progressLoading.value = false
  }
}

const statusOptions = Object.values(TaskStatus).map((s) => ({ value: s, label: TaskStatusLabel[s] }))

onMounted(() => {
  loadDetail()
})
</script>

<template>
  <PageContainer :show-back="true" @back="router.back()">
    <template #header>
      <div>
        <h2 class="vibe-page-title">{{ detail?.taskName || '任务详情' }}</h2>
        <p class="detail-meta" v-if="detail">
          所属项目：{{ detail.projectName || detail.projectId }} ·
          阶段：{{ detail.phaseName || '-' }} ·
          类型：{{ detail.taskType }} ·
          优先级：{{ PriorityLabel[detail.priority as Priority] }}
        </p>
      </div>
    </template>
    <template #extra>
      <a-button @click="loadDetail">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
      <a-button v-if="detail?.status === TaskStatus.TODO" type="primary" @click="openDispatch">派发</a-button>
      <a-button v-if="detail && [TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS].includes(detail.status as TaskStatus)" @click="openTransfer">转派</a-button>
      <a-button v-if="detail && [TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS].includes(detail.status as TaskStatus)" @click="openReturn">退回</a-button>
      <a-button v-if="detail && [TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS].includes(detail.status as TaskStatus)" type="primary" @click="openProgress">更新进度</a-button>
    </template>

    <a-spin :spinning="loading">
      <a-row :gutter="16">
        <a-col :xs="24" :md="16">
          <div class="vibe-card info-card">
            <h3 class="card-title">任务信息</h3>
            <a-descriptions v-if="detail" :column="2" bordered size="small">
              <a-descriptions-item label="任务名称">{{ detail.taskName }}</a-descriptions-item>
              <a-descriptions-item label="任务类型">{{ detail.taskType }}</a-descriptions-item>
              <a-descriptions-item label="状态">
                <StatusTag :tone="TaskStatusTone[detail.status as TaskStatus]">
                  {{ TaskStatusLabel[detail.status as TaskStatus] }}
                </StatusTag>
              </a-descriptions-item>
              <a-descriptions-item label="优先级">{{ PriorityLabel[detail.priority as Priority] }}</a-descriptions-item>
              <a-descriptions-item label="执行模式">
                <a-tag :color="detail.executeMode === 'AGENT' ? 'purple' : 'blue'">
                  {{ detail.executeMode === 'AGENT' ? '代施' : '自施' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="执行人">
                {{ detail.assigneeName || detail.agentEngineerName || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="计划开始">{{ detail.plannedStart || '-' }}</a-descriptions-item>
              <a-descriptions-item label="计划结束">{{ detail.plannedEnd || '-' }}</a-descriptions-item>
              <a-descriptions-item label="实际开始">{{ detail.actualStart || '-' }}</a-descriptions-item>
              <a-descriptions-item label="实际结束">{{ detail.actualEnd || '-' }}</a-descriptions-item>
              <a-descriptions-item label="进度" :span="2">
                <ProgressBar :percent="detail.progressPct || 0" />
              </a-descriptions-item>
              <a-descriptions-item label="任务描述" :span="2">{{ detail.description || '-' }}</a-descriptions-item>
            </a-descriptions>
          </div>

          <div v-if="detail?.siteInfo" class="vibe-card info-card">
            <h3 class="card-title">站点信息</h3>
            <a-descriptions :column="2" bordered size="small">
              <a-descriptions-item label="站点名称">{{ detail.siteInfo.siteName || '-' }}</a-descriptions-item>
              <a-descriptions-item label="联系人">{{ detail.siteInfo.contact || '-' }}</a-descriptions-item>
              <a-descriptions-item label="联系电话">{{ detail.siteInfo.phone || '-' }}</a-descriptions-item>
              <a-descriptions-item label="地址" :span="2">{{ detail.siteInfo.address || '-' }}</a-descriptions-item>
            </a-descriptions>
          </div>
        </a-col>

        <a-col :xs="24" :md="8">
          <div class="vibe-card info-card">
            <h3 class="card-title">操作历史</h3>
            <EmptyState description="暂无操作历史" size="compact" />
          </div>
        </a-col>
      </a-row>
    </a-spin>

    <!-- 派发弹窗 -->
    <a-modal v-model:open="dispatchVisible" title="任务派发" :confirm-loading="dispatchLoading" @ok="handleDispatch">
      <a-form layout="vertical">
        <a-form-item label="执行模式">
          <a-radio-group v-model:value="dispatchForm.executeMode">
            <a-radio value="SELF">自有工程师</a-radio>
            <a-radio value="AGENT">代理商代施</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="dispatchForm.executeMode === 'SELF'" label="工程师 ID">
          <a-input-number v-model:value="dispatchForm.assigneeId" style="width: 100%" placeholder="工程师 ID" />
        </a-form-item>
        <template v-else>
          <a-form-item label="代理商公司 ID">
            <a-input-number v-model:value="dispatchForm.agentCompanyId" style="width: 100%" placeholder="代理商公司 ID" />
          </a-form-item>
          <a-form-item label="代理商工程师 ID">
            <a-input-number v-model:value="dispatchForm.agentEngineerId" style="width: 100%" placeholder="可留空，由代理商指派" />
          </a-form-item>
        </template>
        <a-form-item label="备注">
          <a-textarea v-model:value="dispatchForm.remark" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 转派弹窗 -->
    <a-modal v-model:open="transferVisible" title="任务转派" :confirm-loading="transferLoading" @ok="handleTransfer">
      <a-form layout="vertical">
        <a-form-item label="转派给自有工程师 ID">
          <a-input-number v-model:value="transferForm.toAssigneeId" style="width: 100%" placeholder="自有工程师 ID" />
        </a-form-item>
        <a-form-item label="或转派给代理商工程师 ID">
          <a-input-number v-model:value="transferForm.toAgentEngineerId" style="width: 100%" placeholder="代理商工程师 ID" />
        </a-form-item>
        <a-form-item label="转派原因" required>
          <a-textarea v-model:value="transferForm.reason" :rows="3" placeholder="请说明转派原因" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 退回弹窗 -->
    <a-modal v-model:open="returnVisible" title="任务退回" :confirm-loading="returnLoading" @ok="handleReturn">
      <a-form layout="vertical">
        <a-form-item label="退回原因" required>
          <a-textarea v-model:value="returnForm.reason" :rows="3" placeholder="请说明退回原因" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 进度更新弹窗 -->
    <a-modal v-model:open="progressVisible" title="更新进度" :confirm-loading="progressLoading" @ok="handleProgress">
      <a-form layout="vertical">
        <a-form-item label="任务状态">
          <a-select v-model:value="progressForm.status" :options="statusOptions" />
        </a-form-item>
        <a-form-item label="完成进度">
          <a-slider v-model:value="progressForm.progressPct" :min="0" :max="100" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="progressForm.remark" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.detail-meta {
  margin: 0;
  font-size: 13px;
  color: @text-tertiary;
}
.info-card {
  padding: 20px;
  margin-bottom: 16px;
}
.card-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
}
</style>
