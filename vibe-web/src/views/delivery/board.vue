<script setup lang="ts">
/**
 * 交付看板
 * 设计文档 2.5 / 3.3.4：按工单状态分组展示，全局跟踪交付进度
 * - 卡片支持点击跳转现场作业
 * - 卡片支持快捷操作：进行中→完成、已提交→确认
 * - 超期工单高亮标识
 * - 列底部汇总统计
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message as antdMessage, Modal } from 'ant-design-vue'
import { ReloadOutlined, CheckOutlined, FieldTimeOutlined, ProfileOutlined, ScheduleOutlined, SyncOutlined, CheckCircleOutlined, WarningOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import HelpHint from '@/components/HelpHint.vue'
import StatisticCard from '@/components/StatisticCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageWorkOrders,
  completeWorkOrder,
  confirmWorkOrder
} from '@/api/delivery'
import type { WorkOrder, WorkOrderQueryParams, WorkOrderConfirmDTO } from '@/types/delivery'
import { TaskStatus, TaskStatusTone, TaskStatusLabel, Priority, PriorityLabel } from '@/types/enum'

const router = useRouter()

const loading = ref(false)
const allData = ref<WorkOrder[]>([])
const query = reactive<Pick<WorkOrderQueryParams, 'projectId' | 'executeMode'>>({
  projectId: undefined,
  executeMode: undefined
})

// 看板列定义（按状态机顺序）
const columns = [
  { status: TaskStatus.PENDING, label: TaskStatusLabel[TaskStatus.PENDING], tone: TaskStatusTone[TaskStatus.PENDING] },
  { status: TaskStatus.ASSIGNED, label: TaskStatusLabel[TaskStatus.ASSIGNED], tone: TaskStatusTone[TaskStatus.ASSIGNED] },
  { status: TaskStatus.IN_PROGRESS, label: TaskStatusLabel[TaskStatus.IN_PROGRESS], tone: TaskStatusTone[TaskStatus.IN_PROGRESS] },
  { status: TaskStatus.COMPLETED, label: TaskStatusLabel[TaskStatus.COMPLETED], tone: TaskStatusTone[TaskStatus.COMPLETED] },
  { status: TaskStatus.CONFIRMED, label: TaskStatusLabel[TaskStatus.CONFIRMED], tone: TaskStatusTone[TaskStatus.CONFIRMED] }
]

const executeModeLabel: Record<string, string> = { SELF: '自营', AGENT: '代理' }
const priorityTone: Record<Priority, any> = {
  LOW: 'default', MEDIUM: 'processing', HIGH: 'warning', URGENT: 'error'
}

/** 当前操作的工单 + 弹窗 */
const actionLoading = ref(false)
const confirmVisible = ref(false)
const confirmForm = reactive<WorkOrderConfirmDTO>({ approved: true, remark: '', rating: 5 })
const currentOrder = ref<WorkOrder | null>(null)

// 工单确认表单校验规则（异常处理三层闭环 SubTask 8.4 补充）
const confirmFormRules = {
  approved: [
    { required: true, message: '请选择确认结果', trigger: 'change' }
  ],
  remark: [
    { max: 200, message: '处理说明长度不能超过 200', trigger: 'blur' }
  ],
  rating: [
    { type: 'number', min: 0, max: 5, message: '评分需在 0-5 之间', trigger: 'change' }
  ]
}
const confirmFormRef = ref()

async function loadData() {
  loading.value = true
  try {
    const res = (await pageWorkOrders({
      ...query,
      page: 1,
      size: 200
    } as WorkOrderQueryParams)) as unknown as { records: WorkOrder[] }
    allData.value = res.records || []
  } catch (e: any) {
    console.error('[delivery.board] load failed:', e)
    antdMessage.error(e?.message || '工单数据加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

/** 工单是否超期（计划结束 < 今天且未完成） */
function isOverdue(item: WorkOrder): boolean {
  if (!item.plannedEnd) return false
  if ([TaskStatus.CONFIRMED, TaskStatus.COMPLETED].includes(item.status as TaskStatus)) return false
  return new Date(item.plannedEnd).getTime() < Date.now()
}

// 按状态分组
const grouped = computed(() => {
  const map: Record<string, WorkOrder[]> = {}
  columns.forEach((c) => (map[c.status] = []))
  allData.value.forEach((item) => {
    if (map[item.status as TaskStatus]) {
      map[item.status as TaskStatus].push(item)
    }
  })
  return map
})

// 统计卡片
const stats = computed(() => {
  const total = allData.value.length
  const done = allData.value.filter((d) => d.status === TaskStatus.CONFIRMED).length
  const ongoing = allData.value.filter((d) =>
    [TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS].includes(d.status as TaskStatus)
  ).length
  const pending = allData.value.filter((d) => d.status === TaskStatus.PENDING).length
  const overdue = allData.value.filter(isOverdue).length
  return { total, done, ongoing, pending, overdue }
})

/** 跳转现场作业 */
function gotoField(workOrderId?: number) {
  router.push({ path: '/delivery/field', query: workOrderId ? { id: workOrderId } : {} })
}

/** 卡片点击：跳转现场作业 */
function onCardClick(item: WorkOrder) {
  gotoField(item.id)
}

/** 快捷完成（IN_PROGRESS → SUBMITTED） */
async function handleComplete(item: WorkOrder, e: Event) {
  e.stopPropagation()
  Modal.confirm({
    title: '确认完成工单',
    content: `工单「${item.workOrderName}」将标记为已完成，等待 PM 确认。`,
    okText: '确认完成',
    cancelText: '取消',
    onOk: async () => {
      try {
        await completeWorkOrder(item.id)
        antdMessage.success('工单已标记完成，等待 PM 确认')
        await loadData()
      } catch (err: any) {
        antdMessage.error('完成失败：' + (err?.message || '未知错误'))
      }
    }
  })
}

/** 打开确认弹窗（SUBMITTED → CONFIRMED/REJECTED） */
function openConfirm(item: WorkOrder, e: Event) {
  e.stopPropagation()
  currentOrder.value = item
  confirmForm.approved = true
  confirmForm.remark = ''
  confirmForm.rating = 5
  confirmVisible.value = true
}

/** 提交确认 */
async function handleConfirmSubmit() {
  if (!currentOrder.value) return
  // 异常处理三层闭环：先校验表单，再调用后端
  try {
    await confirmFormRef.value?.validate()
  } catch {
    return
  }
  // 驳回时必填驳回原因
  if (confirmForm.approved === false && !confirmForm.remark?.trim()) {
    antdMessage.warning('驳回时请填写驳回原因')
    return
  }
  actionLoading.value = true
  try {
    await confirmWorkOrder(currentOrder.value.id, {
      approved: confirmForm.approved,
      remark: confirmForm.remark,
      rating: confirmForm.rating
    })
    antdMessage.success(confirmForm.approved ? '工单已确认通过' : '工单已驳回')
    confirmVisible.value = false
    await loadData()
  } catch (err: any) {
    antdMessage.error('确认失败：' + (err?.message || '未知错误'))
  } finally {
    actionLoading.value = false
  }
}

/** 列统计：占比百分比 */
function colPercent(status: TaskStatus): number {
  if (!stats.value.total) return 0
  return Math.round(((grouped.value[status]?.length || 0) / stats.value.total) * 100)
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="交付看板" description="按工单状态分组展示，全局跟踪交付进度">
    <template #title-suffix>
      <HelpHint
        title="交付看板"
        content="按工单状态分组跟踪交付进度：\n1. 五列状态：待派发 → 已指派 → 进行中 → 已完成 → 已确认；\n2. 进行中工单卡片可点击「标记完成」转交 PM 确认；\n3. 已完成工单可点击「确认工单」打开确认弹窗，通过后状态变为已确认；\n4. 超期工单（计划结束 < 今天且未完成）将以红色边框高亮；\n5. 点击「现场作业」进入现场作业页进行施工步骤跟踪。"
      />
    </template>
    <template #extra>
      <a-button @click="gotoField()" type="primary" ghost>
        <template #icon><FieldTimeOutlined /></template>
        现场作业
      </a-button>
      <a-button @click="loadData" :loading="loading">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
    </template>

    <!-- 统计卡片 -->
    <a-row :gutter="16" class="stat-row">
      <a-col :xs="12" :md="4">
        <StatisticCard title="工单总数" :value="stats.total" unit="单" :icon="ProfileOutlined" :loading="loading" />
      </a-col>
      <a-col :xs="12" :md="5">
        <StatisticCard title="待派发" :value="stats.pending" unit="单" :icon="ScheduleOutlined" :loading="loading" />
      </a-col>
      <a-col :xs="12" :md="5">
        <StatisticCard title="进行中" :value="stats.ongoing" unit="单" :icon="SyncOutlined" accent="#1677FF" :loading="loading" />
      </a-col>
      <a-col :xs="12" :md="5">
        <StatisticCard title="已完成" :value="stats.done" unit="单" :icon="CheckCircleOutlined" accent="#52C41A" :loading="loading" />
      </a-col>
      <a-col :xs="12" :md="5">
        <StatisticCard
          title="超期工单"
          :value="stats.overdue"
          unit="单"
          :icon="WarningOutlined"
          accent="#FF4D4F"
          :loading="loading"
        />
      </a-col>
    </a-row>

    <!-- 过滤 -->
    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="项目ID">
          <a-input-number v-model:value="query.projectId" placeholder="项目ID" style="width: 140px" />
        </a-form-item>
        <a-form-item label="执行方式">
          <a-select v-model:value="query.executeMode" placeholder="全部" allow-clear style="width: 130px">
            <a-select-option v-for="(v, k) in executeModeLabel" :key="k" :value="k">{{ v }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">筛选</a-button>
        </a-form-item>
      </a-form>
    </div>

    <!-- 看板 -->
    <a-spin :spinning="loading">
      <div class="kanban" v-if="stats.total">
        <div v-for="col in columns" :key="col.status" class="kanban-col">
          <div class="kanban-col-header">
            <StatusTag :tone="col.tone">{{ col.label }}</StatusTag>
            <span class="count tnum">
              {{ grouped[col.status]?.length || 0 }}
              <span class="count-pct">({{ colPercent(col.status) }}%)</span>
            </span>
          </div>
          <div class="kanban-body">
            <EmptyState v-if="!grouped[col.status]?.length" description="无" size="compact" />
            <div
              v-for="item in grouped[col.status]"
              :key="item.id"
              class="kanban-card"
              :class="{ overdue: isOverdue(item) }"
              @click="onCardClick(item)"
            >
              <div class="card-title">
                <span class="card-no">{{ item.workOrderNo }}</span>
                <StatusTag :tone="priorityTone[item.priority as Priority]">{{ PriorityLabel[item.priority as Priority] }}</StatusTag>
              </div>
              <div class="card-name" :title="item.workOrderName">{{ item.workOrderName }}</div>
              <div class="card-meta">
                <span class="text-auxiliary" :title="item.projectName">{{ item.projectName || '-' }}</span>
              </div>
              <div class="card-meta">
                <span class="text-auxiliary">{{ item.engineerName || item.agentEngineerName || '未指派' }}</span>
                <a-tag :color="item.executeMode === 'AGENT' ? 'purple' : 'blue'" style="margin: 0">
                  {{ executeModeLabel[item.executeMode] }}
                </a-tag>
              </div>
              <div class="card-footer">
                <span class="text-auxiliary" :title="'计划开始：' + (item.plannedStart || '-')">
                  {{ item.plannedStart || '-' }}
                </span>
                <span v-if="item.stepProgress" class="text-auxiliary">
                  步骤 {{ item.stepProgress.completed }}/{{ item.stepProgress.total }}
                </span>
                <span v-if="isOverdue(item)" class="overdue-tag">超期</span>
              </div>
              <!-- 快捷操作 -->
              <div v-if="item.status === TaskStatus.IN_PROGRESS || item.status === TaskStatus.COMPLETED" class="card-actions">
                <a-button
                  v-if="item.status === TaskStatus.IN_PROGRESS"
                  type="primary"
                  size="small"
                  block
                  @click="handleComplete(item, $event)"
                >
                  <CheckOutlined /> 标记完成
                </a-button>
                <a-button
                  v-else-if="item.status === TaskStatus.COMPLETED"
                  type="primary"
                  ghost
                  size="small"
                  block
                  @click="openConfirm(item, $event)"
                >
                  <CheckOutlined /> 确认工单
                </a-button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="vibe-card empty-wrap">
        <EmptyState description="暂无工单数据" action-text="去现场作业" @action="gotoField()" />
      </div>
    </a-spin>

    <!-- 确认工单弹窗 -->
    <a-modal
      v-model:open="confirmVisible"
      title="确认工单"
      :confirm-loading="actionLoading"
      @ok="handleConfirmSubmit"
      @cancel="confirmVisible = false"
    >
      <a-alert
        v-if="currentOrder"
        :message="`工单：${currentOrder.workOrderNo} - ${currentOrder.workOrderName}`"
        type="info"
        show-icon
        style="margin-bottom: 16px"
      />
      <a-form ref="confirmFormRef" layout="vertical" :model="confirmForm" :rules="confirmFormRules">
        <a-form-item label="确认结果" name="approved" required>
          <a-radio-group v-model:value="confirmForm.approved">
            <a-radio :value="true">确认通过</a-radio>
            <a-radio :value="false">驳回返工</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="服务质量评分" name="rating" v-if="confirmForm.approved">
          <a-rate v-model:value="confirmForm.rating" allow-half />
        </a-form-item>
        <a-form-item label="处理说明" name="remark">
          <a-textarea
            v-model:value="confirmForm.remark"
            :placeholder="confirmForm.approved ? '可填写确认意见' : '请填写驳回原因'"
            :rows="3"
            :maxlength="200"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.stat-row {
  margin-bottom: 16px;
}
.text-auxiliary {
  color: @text-tertiary;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.search-card {
  padding: 16px 20px;
  margin-bottom: 16px;
}

.kanban {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 8px;
}
.kanban-col {
  flex: 0 0 280px;
  background: @bg-page;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 360px);
  min-height: 360px;
}
.kanban-col-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid @border-color-split;
  .count {
    color: @text-tertiary;
    font-size: 14px;
  }
  .count-pct {
    color: @text-disabled;
    font-size: 12px;
    margin-left: 2px;
  }
}
.kanban-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}
.kanban-card {
  background: #fff;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid @border-color-split;
  transition: box-shadow 0.2s, border-color 0.2s;
  cursor: pointer;
  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    border-color: @brand-primary;
  }
  &.overdue {
    border-left: 3px solid @status-exception;
    background: rgba(255, 77, 79, 0.02);
  }
  .card-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 6px;
    .card-no {
      font-size: 12px;
      color: @text-tertiary;
    }
  }
  .card-name {
    font-weight: 500;
    margin-bottom: 6px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .card-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 12px;
    margin-bottom: 4px;
  }
  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 12px;
    margin-top: 6px;
    padding-top: 6px;
    border-top: 1px dashed @border-color-split;
  }
  .overdue-tag {
    color: #fff;
    background: @status-exception;
    padding: 0 6px;
    height: 18px;
    line-height: 18px;
    border-radius: 9px;
    font-size: 11px;
  }
  .card-actions {
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px dashed @border-color-split;
  }
}
.empty-wrap {
  padding: 40px 0;
}

/* 响应式：小屏卡片宽度 */
@media (max-width: 768px) {
  .kanban-col {
    flex: 0 0 240px;
  }
}
</style>
