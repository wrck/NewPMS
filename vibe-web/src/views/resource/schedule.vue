<script setup lang="ts">
/**
 * 排期日历
 * - 周视图 / 月视图 切换
 * - 工程师维度行 + 日期列
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, LeftOutlined, RightOutlined } from '@ant-design/icons-vue'
import dayjs, { Dayjs } from 'dayjs'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import { listSchedules, pageEngineers, createSchedule } from '@/api/resource'
import type { Schedule, Engineer } from '@/types/resource'

const loading = ref(false)
const viewMode = ref<'week' | 'month'>('week')
const currentDate = ref<Dayjs>(dayjs())
const engineers = ref<Engineer[]>([])
const schedules = ref<Schedule[]>([])

const dateRange = computed(() => {
  if (viewMode.value === 'week') {
    const start = currentDate.value.startOf('week')
    return Array.from({ length: 7 }, (_, i) => start.add(i, 'day'))
  }
  // 月视图：当月所有天
  const start = currentDate.value.startOf('month')
  const end = currentDate.value.endOf('month')
  const days: Dayjs[] = []
  let d = start
  while (d.isBefore(end) || d.isSame(end, 'day')) {
    days.push(d)
    d = d.add(1, 'day')
  }
  return days
})

const rangeLabel = computed(() => {
  if (viewMode.value === 'week') {
    const start = currentDate.value.startOf('week')
    const end = start.add(6, 'day')
    return `${start.format('YYYY-MM-DD')} ~ ${end.format('YYYY-MM-DD')}`
  }
  return currentDate.value.format('YYYY-MM-DD')
})

async function loadEngineers() {
  try {
    const res: any = await pageEngineers({ page: 1, size: 100, status: 'ACTIVE' })
    engineers.value = res?.records || []
  } catch (e) {
    console.warn('[engineers] load failed:', e)
  }
}

async function loadSchedules() {
  loading.value = true
  try {
    const start = dateRange.value[0].format('YYYY-MM-DD')
    const end = dateRange.value[dateRange.value.length - 1].format('YYYY-MM-DD')
    const engineerIds = engineers.value.map((e) => e.id)
    schedules.value = (await listSchedules({ startDate: start, endDate: end, engineerIds })) || []
  } catch (e) {
    console.error('[schedule] load failed:', e)
  } finally {
    loading.value = false
  }
}

function prevRange() {
  currentDate.value = viewMode.value === 'week' ? currentDate.value.subtract(1, 'week') : currentDate.value.subtract(1, 'month')
  loadSchedules()
}

function nextRange() {
  currentDate.value = viewMode.value === 'week' ? currentDate.value.add(1, 'week') : currentDate.value.add(1, 'month')
  loadSchedules()
}

function today() {
  currentDate.value = dayjs()
  loadSchedules()
}

function schedulesOf(engineerId: number, date: Dayjs): Schedule[] {
  const dateStr = date.format('YYYY-MM-DD')
  return schedules.value.filter((s) => {
    if (s.engineerId !== engineerId) return false
    const start = dayjs(s.startDate)
    const end = dayjs(s.endDate)
    return dateStr >= start.format('YYYY-MM-DD') && dateStr <= end.format('YYYY-MM-DD')
  })
}

const typeColorMap: Record<string, any> = {
  TASK: 'processing',
  LEAVE: 'warning',
  TRAINING: 'agent',
  MEETING: 'default',
  BUSINESS_TRIP: 'agent'
}

const typeLabelMap: Record<string, string> = {
  TASK: '任务',
  LEAVE: '请假',
  TRAINING: '培训',
  MEETING: '会议',
  BUSINESS_TRIP: '出差'
}

// 新增排期弹窗
const formVisible = ref(false)
const formLoading = ref(false)
const formData = reactive<Partial<Schedule>>({
  engineerId: undefined,
  type: 'TASK',
  title: '',
  startDate: '',
  endDate: '',
  projectId: undefined,
  remark: ''
})

function openCreate(engineerId?: number, date?: Dayjs) {
  const dateStr = date ? date.format('YYYY-MM-DD') : dayjs().format('YYYY-MM-DD')
  Object.assign(formData, {
    id: undefined,
    engineerId,
    type: 'TASK',
    title: '',
    startDate: dateStr,
    endDate: dateStr,
    projectId: undefined,
    remark: ''
  })
  formVisible.value = true
}

async function handleSubmit() {
  if (!formData.engineerId || !formData.title) {
    message.warning('请填写工程师和标题')
    return
  }
  formLoading.value = true
  try {
    await createSchedule(formData)
    message.success('排期已添加')
    formVisible.value = false
    loadSchedules()
  } catch (e) {
    // ignore
  } finally {
    formLoading.value = false
  }
}

const typeOptions = Object.entries(typeLabelMap).map(([value, label]) => ({ value, label }))

const weekDayLabels = ['日', '一', '二', '三', '四', '五', '六']

onMounted(() => {
  loadEngineers().then(() => loadSchedules())
})
</script>

<template>
  <PageContainer title="排期日历" description="按周/月查看工程师排期，支持冲突检测">
    <template #extra>
      <a-radio-group v-model:value="viewMode" button-style="solid" @change="loadSchedules">
        <a-radio-button value="week">周</a-radio-button>
        <a-radio-button value="month">月</a-radio-button>
      </a-radio-group>
      <a-button @click="loadSchedules"><template #icon><ReloadOutlined /></template>刷新</a-button>
    </template>

    <div class="vibe-card calendar-card">
      <div class="calendar-toolbar">
        <a-space>
          <a-button size="small" @click="prevRange"><LeftOutlined /></a-button>
          <a-button size="small" @click="today">今天</a-button>
          <a-button size="small" @click="nextRange"><RightOutlined /></a-button>
        </a-space>
        <div class="range-label">{{ rangeLabel }}</div>
        <a-button type="primary" size="small" @click="openCreate()">+ 新增排期</a-button>
      </div>

      <a-spin :spinning="loading">
        <div class="calendar-grid" :style="{ gridTemplateColumns: `120px repeat(${dateRange.length}, 1fr)` }">
          <div class="grid-header corner">工程师</div>
          <div
            v-for="d in dateRange"
            :key="d.format('YYYY-MM-DD')"
            class="grid-header"
            :class="{ today: d.isSame(dayjs(), 'day'), weekend: d.day() === 0 || d.day() === 6 }"
          >
            <div class="date-num">{{ d.format('DD') }}</div>
            <div class="date-week">周{{ weekDayLabels[d.day()] }}</div>
          </div>

          <template v-for="eng in engineers" :key="eng.id">
            <div class="grid-row-label">
              <div class="eng-name">{{ eng.name }}</div>
              <div class="eng-no text-auxiliary">{{ eng.engineerNo }}</div>
            </div>
            <div
              v-for="d in dateRange"
              :key="eng.id + '-' + d.format('YYYY-MM-DD')"
              class="grid-cell"
              :class="{ weekend: d.day() === 0 || d.day() === 6 }"
              @click="openCreate(eng.id, d)"
            >
              <div
                v-for="s in schedulesOf(eng.id, d)"
                :key="s.id"
                class="schedule-block"
                :class="`type-${s.type}`"
              >
                <StatusTag :tone="typeColorMap[s.type]">{{ typeLabelMap[s.type] || s.type }}</StatusTag>
                <span class="schedule-title">{{ s.title }}</span>
              </div>
            </div>
          </template>
        </div>

        <EmptyState v-if="!engineers.length" description="暂无工程师，请先在工程师资源池中维护" size="compact" />
      </a-spin>
    </div>

    <a-modal v-model:open="formVisible" title="新增排期" width="520px" :confirm-loading="formLoading" @ok="handleSubmit">
      <a-form layout="vertical">
        <a-form-item label="工程师">
          <a-select v-model:value="formData.engineerId" placeholder="选择工程师">
            <a-select-option v-for="e in engineers" :key="e.id" :value="e.id">{{ e.name }} ({{ e.engineerNo }})</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="类型">
          <a-select v-model:value="formData.type" :options="typeOptions" />
        </a-form-item>
        <a-form-item label="标题">
          <a-input v-model:value="formData.title" placeholder="如：项目A现场调试" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="开始日期">
              <a-date-picker v-model:value="formData.startDate" style="width: 100%" value-format="YYYY-MM-DD" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结束日期">
              <a-date-picker v-model:value="formData.endDate" style="width: 100%" value-format="YYYY-MM-DD" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="所属项目 ID">
          <a-input-number v-model:value="formData.projectId" style="width: 100%" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="formData.remark" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.calendar-card { padding: 16px 20px; }
.calendar-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.range-label { font-weight: 600; font-size: 16px; }
.calendar-grid {
  display: grid;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  overflow: auto;
  max-height: 70vh;
}
.grid-header {
  padding: 8px 6px;
  text-align: center;
  border-right: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  background: @bg-stripe;
  &.today { background: @brand-bg-light; color: @brand-primary; }
  &.weekend { background: #fafafa; }
  &.corner { font-weight: 600; }
}
.date-num { font-size: 14px; font-weight: 600; }
.date-week { font-size: 12px; color: @text-tertiary; }
.grid-row-label {
  padding: 8px 12px;
  border-right: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  background: @bg-stripe;
}
.eng-name { font-size: 13px; font-weight: 500; }
.eng-no { font-size: 11px; }
.grid-cell {
  padding: 4px;
  min-height: 60px;
  border-right: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  &:hover { background: @bg-selected; }
  &.weekend { background: #fafafa; }
}
.schedule-block {
  padding: 4px 6px;
  margin-bottom: 4px;
  background: #fff;
  border-radius: 4px;
  border-left: 3px solid @brand-primary;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  &.type-TASK { border-left-color: @brand-primary; }
  &.type-LEAVE { border-left-color: @status-pending; }
  &.type-TRAINING { border-left-color: @status-agent; }
  &.type-MEETING { border-left-color: @text-tertiary; }
  &.type-BUSINESS_TRIP { border-left-color: @status-agent; }
}
.schedule-title {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
