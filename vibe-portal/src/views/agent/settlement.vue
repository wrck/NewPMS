<template>
  <div class="settlement">
    <van-tabs v-model:active="activeTab" sticky line-width="40">
      <van-tab title="工作量" name="workload" />
      <van-tab title="评分" name="scores" />
    </van-tabs>

    <!-- 工作量 -->
    <div v-show="activeTab === 'workload'" class="tab-pane">
      <!-- 任务选择 -->
      <section class="card task-picker">
        <div class="task-picker__label">选择任务</div>
        <div class="task-picker__select" @click="showTaskSheet = true">
          <span class="task-picker__name ellipsis">
            {{ currentTask ? currentTask.siteName : '请选择任务' }}
          </span>
          <van-icon name="arrow-down" size="14" color="#8c8c8c" />
        </div>
      </section>

      <template v-if="currentTask">
        <!-- 工作量明细 -->
        <section class="card">
          <div class="section-title">
            工作量明细
            <span v-if="workload?.status" class="status-tag-text">
              {{ workloadStatusText }}
            </span>
          </div>

          <div v-if="!items.length" class="empty-tip">暂无工作量项，请添加</div>

          <ul class="workload-list">
            <li v-for="(item, idx) in items" :key="idx" class="workload-item">
              <van-field
                v-model="item.name"
                placeholder="项目名称"
                label="名称"
                label-width="50"
                :readonly="readonly"
              />
              <div class="workload-item__row">
                <van-field
                  v-model="item.unit"
                  placeholder="单位"
                  label="单位"
                  label-width="50"
                  :readonly="readonly"
                />
                <van-field
                  v-model.number="item.quantity"
                  type="number"
                  placeholder="数量"
                  label="数量"
                  label-width="50"
                  :readonly="readonly"
                />
              </div>
              <div class="workload-item__row">
                <van-field
                  v-model.number="item.unitPrice"
                  type="number"
                  placeholder="单价"
                  label="单价"
                  label-width="50"
                  :readonly="readonly"
                />
                <van-field
                  :model-value="itemAmount(item)"
                  placeholder="金额"
                  label="金额"
                  label-width="50"
                  readonly
                />
              </div>
              <div v-if="!readonly" class="workload-item__del" @click="onRemoveItem(idx)">
                <van-icon name="delete-o" color="#ff4d4f" /> 删除
              </div>
            </li>
          </ul>

          <div v-if="!readonly" class="add-row" @click="onAddItem">
            <van-icon name="add-o" color="#722ed1" />
            <span>添加工作量项</span>
          </div>

          <div class="total-row">
            <span>合计</span>
            <span class="total-amount">¥{{ formatAmount(totalAmount) }}</span>
          </div>

          <div v-if="workload?.submitTime" class="submit-time">
            提交时间：{{ formatDateTime(workload.submitTime) }}
          </div>
        </section>

        <div v-if="!readonly" class="submit-wrap">
          <van-button block type="primary" :loading="submitting" @click="onSubmitWorkload">
            提交工作量
          </van-button>
        </div>
      </template>

      <div v-else class="empty-tip">请先选择任务</div>
    </div>

    <!-- 评分 -->
    <div v-show="activeTab === 'scores'" class="tab-pane">
      <van-pull-refresh v-model="scoreRefreshing" @refresh="onScoreRefresh">
        <ul class="score-list">
          <li v-for="s in scores" :key="s.id" class="score-item">
            <div class="score-item__head">
              <span class="task-code ellipsis">{{ s.taskCode || '任务' }}</span>
              <span class="score-value" :class="`score-value--${scoreLevel(s.score)}`">
                {{ s.score }}分
              </span>
            </div>
            <div v-if="s.projectName" class="project-name ellipsis">{{ s.projectName }}</div>
            <div v-if="s.level" class="score-level">评级：{{ s.level }}</div>
            <p v-if="s.comment" class="score-comment">{{ s.comment }}</p>
            <div v-if="s.scoreTime" class="score-time">{{ formatDateTime(s.scoreTime) }}</div>
          </li>
        </ul>
        <van-empty v-if="!scores.length" description="暂无评分记录" />
      </van-pull-refresh>
    </div>

    <!-- 任务选择 ActionSheet -->
    <van-action-sheet
      v-model:show="showTaskSheet"
      title="选择任务"
      :actions="taskActions"
      close-on-click-action
      cancel-text="取消"
      :round="true"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { showToast, showSuccessToast } from 'vant'
import dayjs from 'dayjs'
import { useAgentStore } from '@/stores/agent'
import { getOutsourceTasks, getWorkload, submitWorkload, getScores } from '@/api/agent'
import type { OutsourceTaskVO, WorkloadVO, WorkloadItem, AgentScoreVO } from '@/types/api'

const agentStore = useAgentStore()

const activeTab = ref<'workload' | 'scores'>('workload')

/* ---------- 任务列表 ---------- */
const tasks = ref<OutsourceTaskVO[]>([])
const currentTask = ref<OutsourceTaskVO | undefined>()
const showTaskSheet = ref(false)

const taskActions = computed(() =>
  tasks.value.map((t) => ({
    name: t.siteName,
    subname: t.taskCode,
    color: currentTask.value?.id === t.id ? '#722ed1' : '#1a1a1a',
    callback: () => selectTask(t)
  }))
)

async function fetchTasks() {
  try {
    const data = await getOutsourceTasks({ page: 1, size: 100 })
    tasks.value = data?.list || []
  } catch (e) {
    // 拦截器已提示
  }
}

async function selectTask(t: OutsourceTaskVO) {
  currentTask.value = t
  showTaskSheet.value = false
  await fetchWorkload(t.id)
}

/* ---------- 工作量 ---------- */
const workload = ref<WorkloadVO | undefined>()
const items = ref<WorkloadItem[]>([])
const submitting = ref(false)

/** 是否只读：已提交/已确认时不可编辑 */
const readonly = computed(() => {
  const s = workload.value?.status
  return !!s && s !== 'DRAFT' && s !== 'REJECTED' && s !== '' && s !== undefined && s !== null
})

const workloadStatusText = computed(() => {
  const s = workload.value?.status
  if (!s) return ''
  return ({ DRAFT: '草稿', SUBMITTED: '已提交', CONFIRMED: '已确认', REJECTED: '已驳回' } as Record<string, string>)[s] || s
})

const totalAmount = computed(() =>
  items.value.reduce((sum, it) => sum + itemAmountNumber(it), 0)
)

function itemAmountNumber(item: WorkloadItem): number {
  const q = Number(item.quantity) || 0
  const p = Number(item.unitPrice) || 0
  return q * p
}

function itemAmount(item: WorkloadItem): string {
  return formatAmount(itemAmountNumber(item))
}

async function fetchWorkload(taskId: number | string) {
  try {
    const data = await getWorkload(taskId)
    workload.value = data
    items.value = (data?.items || []).map((it) => ({ ...it }))
  } catch (e) {
    workload.value = undefined
    items.value = []
  }
}

function onAddItem() {
  items.value.push({ name: '', unit: '', quantity: 0, unitPrice: 0 })
}

function onRemoveItem(idx: number) {
  items.value.splice(idx, 1)
}

async function onSubmitWorkload() {
  const validItems = items.value.filter((it) => it.name && it.name.trim())
  if (!validItems.length) {
    showToast('请至少添加一项工作量')
    return
  }
  if (!currentTask.value) return
  submitting.value = true
  try {
    await submitWorkload(currentTask.value.id, {
      items: validItems.map((it) => ({
        name: it.name.trim(),
        unit: it.unit,
        quantity: it.quantity,
        unitPrice: it.unitPrice,
        amount: itemAmountNumber(it)
      }))
    })
    showSuccessToast('提交成功')
    await fetchWorkload(currentTask.value.id)
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

/* ---------- 评分 ---------- */
const scores = ref<AgentScoreVO[]>([])
const scoreRefreshing = ref(false)

async function fetchScores() {
  const cid = agentStore.companyId
  if (!cid) {
    showToast('未获取到公司信息')
    return
  }
  try {
    scores.value = (await getScores(cid)) || []
  } catch (e) {
    // 拦截器已提示
  }
}

function onScoreRefresh() {
  fetchScores().finally(() => {
    scoreRefreshing.value = false
  })
}

function scoreLevel(score: number): string {
  if (score >= 90) return 'excellent'
  if (score >= 75) return 'good'
  if (score >= 60) return 'mid'
  return 'low'
}

/* ---------- 工具 ---------- */
function formatAmount(n: number): string {
  return (n || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDateTime(d?: string): string {
  return d ? dayjs(d).format('YYYY-MM-DD HH:mm') : '-'
}

// 切换到评分 tab 时首次加载
watch(activeTab, (val) => {
  if (val === 'scores' && !scores.value.length) fetchScores()
})

onMounted(fetchTasks)
</script>

<style lang="scss" scoped>
.settlement {
  min-height: 100%;
  background: var(--bg-page);
  padding-bottom: calc(24px + var(--safe-bottom));
}

.tab-pane {
  padding: 12px;
}

.task-picker {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;

  &__label {
    font-size: 13px;
    color: var(--color-text-secondary);
    flex-shrink: 0;
  }

  &__select {
    display: flex;
    align-items: center;
    gap: 6px;
    flex: 1;
    min-width: 0;
    justify-content: flex-end;
  }

  &__name {
    font-size: 14px;
    color: var(--color-text-primary);
    font-weight: 500;
    max-width: 220px;
  }
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;

  .status-tag-text {
    font-size: 12px;
    font-weight: 400;
    color: var(--color-text-secondary);
  }
}

.workload-list {
  .workload-item {
    padding: 10px;
    border-radius: 8px;
    background: #fafbfc;
    margin-bottom: 8px;
    border: 1px solid var(--border-color-light);

    &__row {
      display: flex;
      gap: 8px;

      .van-field {
        flex: 1;
      }
    }

    &__del {
      margin-top: 6px;
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      color: var(--color-danger);
      justify-content: flex-end;
    }
  }
}

.add-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 10px;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  color: var(--agent-primary);
  font-size: 13px;
}

.total-row {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--border-color-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;

  .total-amount {
    font-size: 16px;
    font-weight: 600;
    color: var(--color-danger);
  }
}

.submit-time {
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.submit-wrap {
  margin-top: 16px;

  :deep(.van-button--primary) {
    background: var(--agent-primary);
    border-color: var(--agent-primary);
  }
}

.empty-tip {
  text-align: center;
  padding: 16px 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* 评分 */
.score-list {
  .score-item + .score-item {
    margin-top: 8px;
  }
}

.score-item {
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);

  &__head {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .task-code {
      font-size: 13px;
      color: var(--color-text-secondary);
      flex: 1;
      min-width: 0;
    }

    .score-value {
      font-size: 16px;
      font-weight: 600;

      &--excellent {
        color: var(--color-success);
      }
      &--good {
        color: var(--brand-primary);
      }
      &--mid {
        color: var(--color-warning);
      }
      &--low {
        color: var(--color-danger);
      }
    }
  }

  .project-name {
    margin-top: 6px;
    font-size: 14px;
    font-weight: 500;
    color: var(--color-text-primary);
  }

  .score-level {
    margin-top: 4px;
    font-size: 12px;
    color: var(--color-text-regular);
  }

  .score-comment {
    margin-top: 6px;
    font-size: 13px;
    color: var(--color-text-regular);
    line-height: 1.5;
  }

  .score-time {
    margin-top: 6px;
    font-size: 12px;
    color: var(--color-text-secondary);
  }
}
</style>
