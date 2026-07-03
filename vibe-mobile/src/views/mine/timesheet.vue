<script setup lang="ts">
/**
 * 工时填报页 + 历史工时列表
 *
 * 后端：
 *   - POST /timesheets                       工时填报（TimesheetDTO）
 *   - GET /timesheets                        分页查询历史（TimesheetQuery → PageResult<TimesheetVO>）
 *   - DELETE /timesheets/{id}                删除工时
 *   - GET /timesheets/summary                工时汇总（TimesheetStatsVO）
 */
import { ref, reactive, onMounted } from 'vue'
import {
  showSuccessToast,
  showFailToast,
  showLoadingToast,
  closeToast,
  showConfirmDialog
} from 'vant'
import {
  fetchTimesheets,
  submitTimesheet,
  deleteTimesheet,
  fetchTimesheetSummary,
  type TimesheetQuery
} from '@/api'
import type { TimesheetStatsVO, TimesheetVO } from '@/types/api'
import dayjs from 'dayjs'

defineOptions({ name: 'MineTimesheet' })

/* ============ 工时填报表单（对齐后端 TimesheetDTO） ============ */
const form = reactive({
  workDate: dayjs().format('YYYY-MM-DD'),
  hours: 8,
  overtimeHours: 0,
  travelDays: 0,
  taskId: '',
  description: ''
})

const datePicker = ref(false)
const hourPicker = ref(false)
const overtimePicker = ref(false)
const travelPicker = ref(false)

const dateColumns = generateDateColumns()
const hourColumns = Array.from({ length: 24 }, (_, i) => ({
  text: `${i + 1} 小时`,
  value: i + 1
}))
const overtimeColumns = Array.from({ length: 13 }, (_, i) => ({
  text: `${i} 小时`,
  value: i
}))
const travelColumns = Array.from({ length: 31 }, (_, i) => ({
  text: `${i} 天`,
  value: i
}))

function generateDateColumns() {
  const today = dayjs()
  const list: { text: string; value: string }[] = []
  for (let i = 0; i < 30; i++) {
    const d = today.subtract(i, 'day')
    list.push({
      text: d.format('YYYY-MM-DD') + (i === 0 ? '（今天）' : ''),
      value: d.format('YYYY-MM-DD')
    })
  }
  return list
}

function onDateConfirm({ selectedValues }: { selectedValues: string[] }) {
  form.workDate = selectedValues[0]
  datePicker.value = false
}

function onHourConfirm({ selectedOptions }: { selectedOptions: any[] }) {
  form.hours = selectedOptions[0]?.value || 8
  hourPicker.value = false
}

function onOvertimeConfirm({ selectedOptions }: { selectedOptions: any[] }) {
  form.overtimeHours = selectedOptions[0]?.value || 0
  overtimePicker.value = false
}

function onTravelConfirm({ selectedOptions }: { selectedOptions: any[] }) {
  form.travelDays = selectedOptions[0]?.value || 0
  travelPicker.value = false
}

const submitting = ref(false)

async function onSubmit() {
  if (!form.hours || form.hours <= 0) {
    showFailToast('请填写工时')
    return
  }
  submitting.value = true
  showLoadingToast({ message: '提交中...', forbidClick: true, duration: 0 })
  try {
    await submitTimesheet({
      workDate: form.workDate,
      hours: form.hours,
      overtimeHours: form.overtimeHours,
      travelDays: form.travelDays,
      taskId: form.taskId || undefined,
      description: form.description
    })
    closeToast()
    showSuccessToast('工时已提交')
    form.description = ''
    // 刷新历史列表与汇总
    await Promise.all([loadHistory(), loadSummary()])
  } catch (e) {
    closeToast()
    // request 拦截器已 toast 错误
  } finally {
    submitting.value = false
  }
}

/* ============ 历史工时列表（分页） ============ */
const history = ref<TimesheetVO[]>([])
const listLoading = ref(false)
const listFinished = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 10

const query = reactive<TimesheetQuery>({
  page: 1,
  size: pageSize,
  status: '',
  workDateStart: '',
  workDateEnd: ''
})

async function loadHistory(reset = false) {
  if (reset) {
    page.value = 1
    history.value = []
    listFinished.value = false
  }
  if (listFinished.value) return
  listLoading.value = true
  try {
    const res = await fetchTimesheets({
      page: page.value,
      size: pageSize,
      status: query.status || undefined,
      workDateStart: query.workDateStart || undefined,
      workDateEnd: query.workDateEnd || undefined
    })
    const records = res?.records || []
    history.value.push(...records)
    total.value = res?.total || 0
    if (history.value.length >= total.value || records.length === 0) {
      listFinished.value = true
    } else {
      page.value += 1
    }
  } catch (e) {
    listFinished.value = true
  } finally {
    listLoading.value = false
  }
}

function onLoad() {
  loadHistory()
}

async function onDelete(item: TimesheetVO) {
  try {
    await showConfirmDialog({
      title: '删除工时',
      message: `确认删除 ${item.workDate} 的工时记录？`
    })
    await deleteTimesheet(item.id)
    showSuccessToast('已删除')
    await loadHistory(true)
    await loadSummary()
  } catch (e) {
    // 取消或失败
  }
}

/* ============ 工时汇总 ============ */
const summary = ref<TimesheetStatsVO | null>(null)

async function loadSummary() {
  try {
    const today = dayjs()
    const start = today.subtract(29, 'day')
    summary.value = await fetchTimesheetSummary({
      startDate: start.format('YYYY-MM-DD'),
      endDate: today.format('YYYY-MM-DD')
    })
  } catch (e) {
    summary.value = null
  }
}

function statusText(status?: string): string {
  switch (status) {
    case 'SUBMITTED':
      return '已提交'
    case 'APPROVED':
      return '已批准'
    case 'REJECTED':
      return '已驳回'
    default:
      return status || '-'
  }
}

function statusColor(status?: string): string {
  switch (status) {
    case 'APPROVED':
      return '#52c41a'
    case 'REJECTED':
      return '#ff4d4f'
    case 'SUBMITTED':
      return '#faad14'
    default:
      return '#8c8c8c'
  }
}

onMounted(() => {
  loadSummary()
})
</script>

<template>
  <div class="timesheet-page page">
    <!-- 工时汇总卡片 -->
    <div class="card summary-card">
      <div class="summary-title">近 30 天工时汇总</div>
      <div class="summary-grid">
        <div class="summary-item">
          <div class="value">{{ summary?.totalHours ?? 0 }}</div>
          <div class="label">总工时(h)</div>
        </div>
        <div class="summary-item">
          <div class="value">{{ summary?.totalOvertimeHours ?? 0 }}</div>
          <div class="label">加班(h)</div>
        </div>
        <div class="summary-item">
          <div class="value">{{ summary?.totalTravelDays ?? 0 }}</div>
          <div class="label">出差(天)</div>
        </div>
        <div class="summary-item">
          <div class="value">{{ summary?.totalManDays ?? 0 }}</div>
          <div class="label">人天</div>
        </div>
      </div>
    </div>

    <!-- 工时填报 -->
    <van-cell-group inset title="工时填报">
      <van-cell title="日期" :value="form.workDate" is-link @click="datePicker = true" />
      <van-cell
        title="工作时长"
        :value="`${form.hours} 小时`"
        is-link
        @click="hourPicker = true"
      />
      <van-cell
        title="加班时长"
        :value="`${form.overtimeHours} 小时`"
        is-link
        @click="overtimePicker = true"
      />
      <van-cell
        title="出差天数"
        :value="`${form.travelDays} 天`"
        is-link
        @click="travelPicker = true"
      />
      <van-field
        v-model="form.taskId"
        label="关联任务ID"
        placeholder="选填，关联工单/任务"
      />
      <van-field
        v-model="form.description"
        type="textarea"
        label="工作内容"
        placeholder="请填写工作内容说明"
        rows="2"
        autosize
        maxlength="500"
        show-word-limit
      />
    </van-cell-group>

    <div class="bottom-action">
      <van-button
        type="primary"
        block
        round
        :loading="submitting"
        class="touchable"
        @click="onSubmit"
      >
        提交工时
      </van-button>
    </div>

    <!-- 历史工时列表 -->
    <div class="section-title">历史工时（共 {{ total }} 条）</div>
    <van-list
      v-model:loading="listLoading"
      :finished="listFinished"
      finished-text="没有更多了"
      @load="onLoad"
    >
      <div v-for="item in history" :key="item.id" class="card history-item">
        <div class="history-item__head">
          <span class="date">{{ item.workDate }}</span>
          <span class="status" :style="{ color: statusColor(item.status) }">
            {{ statusText(item.status) }}
          </span>
        </div>
        <div class="history-item__body">
          <div class="row">
            <span class="label">工时：</span>
            <span class="value">{{ item.hours }}h</span>
          </div>
          <div v-if="Number(item.overtimeHours) > 0" class="row">
            <span class="label">加班：</span>
            <span class="value">{{ item.overtimeHours }}h</span>
          </div>
          <div v-if="item.travelDays" class="row">
            <span class="label">出差：</span>
            <span class="value">{{ item.travelDays }}天</span>
          </div>
          <div v-if="item.projectName" class="row">
            <span class="label">项目：</span>
            <span class="value">{{ item.projectName }}</span>
          </div>
          <div v-if="item.taskName" class="row">
            <span class="label">任务：</span>
            <span class="value">{{ item.taskName }}</span>
          </div>
          <div v-if="item.description" class="desc">{{ item.description }}</div>
        </div>
        <div class="history-item__foot">
          <span v-if="item.approverName" class="approver">
            审批人：{{ item.approverName }}
          </span>
          <van-button
            v-if="item.status === 'SUBMITTED' || item.status === 'REJECTED'"
            size="mini"
            type="danger"
            plain
            class="touchable"
            @click="onDelete(item)"
          >
            删除
          </van-button>
        </div>
      </div>
    </van-list>
    <van-empty v-if="!listLoading && history.length === 0" description="暂无工时记录" />

    <!-- 日期选择 -->
    <van-popup v-model:show="datePicker" position="bottom" round>
      <van-picker
        :columns="dateColumns"
        @confirm="onDateConfirm"
        @cancel="datePicker = false"
      />
    </van-popup>

    <!-- 工时选择 -->
    <van-popup v-model:show="hourPicker" position="bottom" round>
      <van-picker
        :columns="hourColumns"
        @confirm="onHourConfirm"
        @cancel="hourPicker = false"
      />
    </van-popup>

    <!-- 加班选择 -->
    <van-popup v-model:show="overtimePicker" position="bottom" round>
      <van-picker
        :columns="overtimeColumns"
        @confirm="onOvertimeConfirm"
        @cancel="overtimePicker = false"
      />
    </van-popup>

    <!-- 出差选择 -->
    <van-popup v-model:show="travelPicker" position="bottom" round>
      <van-picker
        :columns="travelColumns"
        @confirm="onTravelConfirm"
        @cancel="travelPicker = false"
      />
    </van-popup>
  </div>
</template>

<style scoped lang="scss">
.timesheet-page {
  padding: 12px 0 100px;
}

.section-title {
  margin: 16px 16px 8px;
  font-size: 15px;
  font-weight: 600;
}

.summary-card {
  margin: 0 12px;
}

.summary-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  text-align: center;
}

.summary-item {
  .value {
    font-size: 18px;
    font-weight: 700;
    color: var(--brand-primary);
  }
  .label {
    font-size: 12px;
    color: var(--color-text-secondary);
    margin-top: 4px;
  }
}

.history-item {
  margin: 8px 12px;

  &__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 8px;
    border-bottom: 1px solid var(--border-color-light);

    .date {
      font-size: 15px;
      font-weight: 600;
    }
    .status {
      font-size: 12px;
      font-weight: 600;
    }
  }

  &__body {
    padding: 8px 0;

    .row {
      display: flex;
      font-size: 13px;
      padding: 2px 0;

      .label {
        color: var(--color-text-secondary);
        flex-shrink: 0;
      }
      .value {
        flex: 1;
        color: var(--color-text-regular);
      }
    }

    .desc {
      font-size: 12px;
      color: var(--color-text-secondary);
      line-height: 1.6;
      margin-top: 6px;
      padding: 6px 8px;
      background: var(--bg-disabled);
      border-radius: var(--radius-sm);
    }
  }

  &__foot {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 8px;
    border-top: 1px solid var(--border-color-light);

    .approver {
      font-size: 12px;
      color: var(--color-text-placeholder);
    }
  }
}
</style>
