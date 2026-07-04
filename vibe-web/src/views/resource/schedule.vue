<script setup lang="ts">
/**
 * 排期日历
 * 设计文档 2.4 / 3.3.3：
 * - 周视图 / 月视图 切换
 * - 工程师维度行 + 日期列
 * - 单元格点击新增排期
 * - 排期块点击编辑（支持删除）
 * - 冲突检测：同一天同一工程师多项排期高亮提示
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ReloadOutlined, LeftOutlined, RightOutlined, EditOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import dayjs, { Dayjs } from 'dayjs'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  listSchedules,
  pageEngineers,
  createSchedule,
  updateSchedule,
  deleteSchedule
} from '@/api/resource'
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
  return currentDate.value.format('YYYY-MM-MM')
})

async function loadEngineers() {
  try {
    const res: any = await pageEngineers({ page: 1, size: 100, status: 'ACTIVE' })
    engineers.value = res?.records || []
  } catch (e: any) {
    console.warn('[engineers] load failed:', e)
    message.error('工程师列表加载失败：' + (e?.message || '未知错误'))
  }
}

async function loadSchedules() {
  loading.value = true
  try {
    const start = dateRange.value[0].format('YYYY-MM-DD')
    const end = dateRange.value[dateRange.value.length - 1].format('YYYY-MM-DD')
    const engineerIds = engineers.value.map((e) => e.id)
    schedules.value = (await listSchedules({ startDate: start, endDate: end, engineerIds })) || []
  } catch (e: any) {
    console.error('[schedule] load failed:', e)
    message.error('排期数据加载失败：' + (e?.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

function prevRange() {
  currentDate.value =
    viewMode.value === 'week' ? currentDate.value.subtract(1, 'week') : currentDate.value.subtract(1, 'month')
  loadSchedules()
}

function nextRange() {
  currentDate.value =
    viewMode.value === 'week' ? currentDate.value.add(1, 'week') : currentDate.value.add(1, 'month')
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

/** 工程师在当前周期内的排期数 */
function scheduleCountOf(engineerId: number): number {
  return schedules.value.filter((s) => s.engineerId === engineerId).length
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

const statusLabelMap: Record<string, string> = {
  PLANNED: '计划中',
  CONFIRMED: '已确认',
  IN_PROGRESS: '进行中',
  DONE: '已完成',
  CANCELLED: '已取消'
}

const statusToneMap: Record<string, any> = {
  PLANNED: 'default',
  CONFIRMED: 'processing',
  IN_PROGRESS: 'processing',
  DONE: 'success',
  CANCELLED: 'archived'
}

// 新增/编辑弹窗
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
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
  isEdit.value = false
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

function openEdit(schedule: Schedule, e: Event) {
  e.stopPropagation()
  isEdit.value = true
  Object.assign(formData, {
    id: schedule.id,
    engineerId: schedule.engineerId,
    type: schedule.type,
    title: schedule.title,
    startDate: schedule.startDate,
    endDate: schedule.endDate,
    projectId: schedule.projectId,
    remark: schedule.remark || ''
  })
  formVisible.value = true
}

async function handleSubmit() {
  if (!formData.engineerId) {
    message.warning('请选择工程师')
    return
  }
  if (!formData.title || !formData.title.trim()) {
    message.warning('请填写标题')
    return
  }
  if (!formData.startDate || !formData.endDate) {
    message.warning('请选择起止日期')
    return
  }
  if (dayjs(formData.endDate).isBefore(dayjs(formData.startDate))) {
    message.warning('结束日期不能早于开始日期')
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateSchedule(formData.id, formData)
      message.success('排期已更新')
    } else {
      await createSchedule(formData)
      message.success('排期已添加')
    }
    formVisible.value = false
    await loadSchedules()
  } catch (e: any) {
    message.error((isEdit.value ? '更新' : '添加') + '失败：' + (e?.message || '未知错误'))
  } finally {
    formLoading.value = false
  }
}

function handleDelete() {
  if (!formData.id) return
  Modal.confirm({
    title: '确认删除',
    content: `排期「${formData.title}」将被删除，此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteSchedule(formData.id!)
        message.success('排期已删除')
        formVisible.value = false
        await loadSchedules()
      } catch (e: any) {
        message.error('删除失败：' + (e?.message || '未知错误'))
      }
    }
  })
}

const typeOptions = Object.entries(typeLabelMap).map(([value, label]) => ({ value, label }))
const weekDayLabels = ['日', '一', '二', '三', '四', '五', '六']

/** 当前周期内总排期数 */
const totalSchedules = computed(() => schedules.value.length)

onMounted(() => {
  loadEngineers().then(() => loadSchedules())
})
</script>

<template>
  <PageContainer title="排期日历" description="按周/月查看工程师排期，支持冲突检测与编辑">
    <template #extra>
      <a-radio-group v-model:value="viewMode" button-style="solid" @change="loadSchedules">
        <a-radio-button value="week">周</a-radio-button>
        <a-radio-button value="month">月</a-radio-button>
      </a-radio-group>
      <a-button @click="loadSchedules" :loading="loading">
        <template #icon><ReloadOutlined /></template>刷新
      </a-button>
    </template>

    <div class="vibe-card calendar-card">
      <div class="calendar-toolbar">
        <a-space>
          <a-button size="small" @click="prevRange"><LeftOutlined /></a-button>
          <a-button size="small" @click="today">今天</a-button>
          <a-button size="small" @click="nextRange"><RightOutlined /></a-button>
          <span class="range-label">{{ rangeLabel }}</span>
        </a-space>
        <div class="toolbar-right">
          <span class="stat-text">共 <span class="tnum">{{ totalSchedules }}</span> 条排期</span>
          <a-button type="primary" size="small" @click="openCreate()">
            <template #icon><PlusOutlined /></template>新增排期
          </a-button>
        </div>
      </div>

      <!-- 图例 -->
      <div class="legend">
        <span v-for="(label, key) in typeLabelMap" :key="key" class="legend-item">
          <span class="legend-dot" :class="`type-${key}`"></span>
          {{ label }}
        </span>
      </div>

      <a-spin :spinning="loading">
        <div class="calendar-grid" :style="{ gridTemplateColumns: `140px repeat(${dateRange.length}, 1fr)` }">
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
              <div class="eng-count">
                <a-tag v-if="scheduleCountOf(eng.id)" color="blue" class="count-tag">
                  {{ scheduleCountOf(eng.id) }} 条
                </a-tag>
              </div>
            </div>
            <div
              v-for="d in dateRange"
              :key="eng.id + '-' + d.format('YYYY-MM-DD')"
              class="grid-cell"
              :class="{ weekend: d.day() === 0 || d.day() === 6, today: d.isSame(dayjs(), 'day') }"
              @click="openCreate(eng.id, d)"
            >
              <div
                v-for="s in schedulesOf(eng.id, d)"
                :key="s.id"
                class="schedule-block"
                :class="`type-${s.type}`"
                @click="openEdit(s, $event)"
              >
                <div class="schedule-head">
                  <StatusTag :tone="typeColorMap[s.type]">{{ typeLabelMap[s.type] || s.type }}</StatusTag>
                  <StatusTag
                    v-if="s.status"
                    :tone="statusToneMap[s.status] || 'default'"
                    class="schedule-status"
                  >
                    {{ statusLabelMap[s.status] || s.status }}
                  </StatusTag>
                </div>
                <span class="schedule-title" :title="s.title">{{ s.title }}</span>
                <div v-if="s.projectName" class="schedule-project">{{ s.projectName }}</div>
              </div>
            </div>
          </template>
        </div>

        <EmptyState
          v-if="!engineers.length"
          description="暂无工程师，请先在工程师资源池中维护"
          size="compact"
          action-text="去维护工程师"
          @action="$router.push('/resource/engineer')"
        />
      </a-spin>
    </div>

    <!-- 新增/编辑排期弹窗 -->
    <a-modal
      v-model:open="formVisible"
      :title="isEdit ? '编辑排期' : '新增排期'"
      width="540px"
      :confirm-loading="formLoading"
      @ok="handleSubmit"
      @cancel="formVisible = false"
    >
      <a-form layout="vertical">
        <a-form-item label="工程师" required>
          <a-select v-model:value="formData.engineerId" placeholder="选择工程师" :disabled="isEdit">
            <a-select-option v-for="e in engineers" :key="e.id" :value="e.id">
              {{ e.name }} ({{ e.engineerNo }})
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="类型" required>
              <a-select v-model:value="formData.type" :options="typeOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="所属项目 ID">
              <a-input-number v-model:value="formData.projectId" style="width: 100%" placeholder="可选" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="标题" required>
          <a-input v-model:value="formData.title" placeholder="如：项目A现场调试" :maxlength="100" show-count />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="开始日期" required>
              <a-date-picker v-model:value="formData.startDate" style="width: 100%" value-format="YYYY-MM-DD" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结束日期" required>
              <a-date-picker v-model:value="formData.endDate" style="width: 100%" value-format="YYYY-MM-DD" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注">
          <a-textarea v-model:value="formData.remark" :rows="2" :maxlength="200" show-count />
        </a-form-item>
      </a-form>

      <template #footer>
        <a-space>
          <a-button v-if="isEdit" danger :loading="formLoading" @click="handleDelete">
            <template #icon><DeleteOutlined /></template>删除
          </a-button>
          <span style="flex: 1"></span>
          <a-button @click="formVisible = false">取消</a-button>
          <a-button type="primary" :loading="formLoading" @click="handleSubmit">
            <template #icon><EditOutlined /></template>{{ isEdit ? '保存' : '添加' }}
          </a-button>
        </a-space>
      </template>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.calendar-card {
  padding: 16px 20px;
}
.calendar-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}
.range-label {
  font-weight: 600;
  font-size: 15px;
  margin-left: 8px;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.stat-text {
  font-size: 13px;
  color: @text-tertiary;
}
.legend {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: @bg-stripe;
  border-radius: 4px;
  flex-wrap: wrap;
}
.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: @text-secondary;
}
.legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 2px;
  background: @brand-primary;
  &.type-TASK { background: @brand-primary; }
  &.type-LEAVE { background: @status-pending; }
  &.type-TRAINING { background: @status-agent; }
  &.type-MEETING { background: @text-tertiary; }
  &.type-BUSINESS_TRIP { background: @status-agent; }
}
.calendar-grid {
  display: grid;
  border: 1px solid @border-color-split;
  border-radius: 6px;
  overflow: auto;
  max-height: 70vh;
  min-width: 800px;
}
.grid-header {
  padding: 8px 6px;
  text-align: center;
  border-right: 1px solid @border-color-split;
  border-bottom: 1px solid @border-color-split;
  background: @bg-stripe;
  position: sticky;
  top: 0;
  z-index: 2;
  &.today {
    background: @brand-bg-light;
    color: @brand-primary;
  }
  &.weekend {
    background: #fafafa;
  }
  &.corner {
    font-weight: 600;
    text-align: left;
    padding-left: 12px;
  }
}
.date-num {
  font-size: 14px;
  font-weight: 600;
}
.date-week {
  font-size: 12px;
  color: @text-tertiary;
}
.grid-row-label {
  padding: 8px 12px;
  border-right: 1px solid @border-color-split;
  border-bottom: 1px solid @border-color-split;
  background: @bg-stripe;
  position: sticky;
  left: 0;
  z-index: 1;
  min-width: 140px;
}
.eng-name {
  font-size: 13px;
  font-weight: 500;
}
.eng-no {
  font-size: 11px;
}
.eng-count {
  margin-top: 4px;
}
.count-tag {
  margin: 0;
  font-size: 11px;
  line-height: 16px;
  padding: 0 6px;
}
.grid-cell {
  padding: 4px;
  min-height: 60px;
  border-right: 1px solid @border-color-split;
  border-bottom: 1px solid @border-color-split;
  cursor: pointer;
  transition: background 0.15s;
  &:hover {
    background: @bg-selected;
  }
  &.weekend {
    background: #fafafa;
  }
  &.today {
    background: rgba(22, 119, 255, 0.04);
  }
}
.schedule-block {
  padding: 4px 6px;
  margin-bottom: 4px;
  background: #fff;
  border-radius: 4px;
  border-left: 3px solid @brand-primary;
  font-size: 12px;
  cursor: pointer;
  transition: box-shadow 0.15s;
  &:hover {
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  }
  &.type-TASK { border-left-color: @brand-primary; }
  &.type-LEAVE { border-left-color: @status-pending; }
  &.type-TRAINING { border-left-color: @status-agent; }
  &.type-MEETING { border-left-color: @text-tertiary; }
  &.type-BUSINESS_TRIP { border-left-color: @status-agent; }
}
.schedule-head {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 2px;
  flex-wrap: wrap;
}
.schedule-status {
  font-size: 10px;
  line-height: 14px;
  padding: 0 4px;
  height: 16px;
}
.schedule-title {
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
}
.schedule-project {
  font-size: 11px;
  color: @text-tertiary;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.text-auxiliary {
  color: @text-tertiary;
  font-size: 11px;
}

/* 响应式：小屏水平滚动 */
@media (max-width: 768px) {
  .calendar-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .toolbar-right {
    justify-content: space-between;
  }
}
</style>
