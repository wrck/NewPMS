<script setup lang="ts">
/**
 * 工时管理
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, CheckOutlined, CloseOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageTimesheets,
  createTimesheet,
  updateTimesheet,
  deleteTimesheet,
  submitTimesheet,
  approveTimesheet
} from '@/api/resource'
import type { Timesheet, TimesheetDTO, TimesheetQueryParams } from '@/types/resource'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<Timesheet[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const query = reactive<TimesheetQueryParams>({ engineerId: undefined, projectId: undefined, status: undefined, startDate: '', endDate: '' })

async function loadData() {
  loading.value = true
  try {
    const res = (await pageTimesheets({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<Timesheet>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[resource.timesheet] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

// 弹窗
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<TimesheetDTO>({
  engineerId: 0,
  projectId: 0,
  taskId: undefined,
  workDate: '',
  hours: 8,
  overtimeHours: 0,
  workType: 'NORMAL',
  description: ''
})

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    engineerId: 0,
    projectId: 0,
    taskId: undefined,
    workDate: '',
    hours: 8,
    overtimeHours: 0,
    workType: 'NORMAL',
    description: ''
  })
  formVisible.value = true
}

function openEdit(row: Timesheet) {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    engineerId: row.engineerId,
    projectId: row.projectId,
    taskId: row.taskId,
    workDate: row.workDate,
    hours: row.hours,
    overtimeHours: row.overtimeHours,
    workType: row.workType,
    description: row.description
  })
  formVisible.value = true
}

async function handleSubmit() {
  if (!formData.engineerId || !formData.projectId || !formData.workDate) {
    message.warning('请完整填写工程师/项目/日期')
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateTimesheet(formData.id, formData)
      message.success('更新成功')
    } else {
      await createTimesheet(formData)
      message.success('填报成功')
    }
    formVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    formLoading.value = false
  }
}

function handleDelete(row: Timesheet) {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该工时记录吗？',
    okType: 'danger',
    async onOk() {
      try {
        await deleteTimesheet(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) { /* ignore */ }
    }
  })
}

async function handleSubmit0(row: Timesheet) {
  try {
    await submitTimesheet(row.id)
    message.success('已提交')
    loadData()
  } catch (e) { /* ignore */ }
}

// 审批弹窗
const approveVisible = ref(false)
const approveLoading = ref(false)
const approveRow = ref<Timesheet | null>(null)
const approveForm = reactive({ approved: true, remark: '' })

function openApprove(row: Timesheet, approved: boolean) {
  approveRow.value = row
  approveForm.approved = approved
  approveForm.remark = ''
  approveVisible.value = true
}

async function handleApprove() {
  if (!approveRow.value) return
  approveLoading.value = true
  try {
    await approveTimesheet(approveRow.value.id, approveForm.approved, approveForm.remark)
    message.success(approveForm.approved ? '已审批通过' : '已驳回')
    approveVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    approveLoading.value = false
  }
}

const statusMap: Record<string, { tone: any; label: string }> = {
  DRAFT: { tone: 'default', label: '草稿' },
  SUBMITTED: { tone: 'warning', label: '待审批' },
  APPROVED: { tone: 'success', label: '已通过' },
  REJECTED: { tone: 'error', label: '已驳回' }
}

const workTypeMap: Record<string, string> = {
  NORMAL: '正常',
  OVERTIME: '加班',
  BUSINESS_TRIP: '出差',
  WEEKEND: '周末'
}

const columns = [
  { title: '工程师', dataIndex: 'engineerName', key: 'engineerName', width: 110 },
  { title: '所属项目', dataIndex: 'projectName', key: 'projectName', ellipsis: true },
  { title: '任务', dataIndex: 'taskName', key: 'taskName', width: 150, ellipsis: true },
  { title: '日期', dataIndex: 'workDate', key: 'workDate', width: 120 },
  { title: '工时(h)', key: 'hours', width: 100 },
  { title: '类型', key: 'workType', width: 90 },
  { title: '状态', key: 'status', width: 100 },
  { title: '审批人', dataIndex: 'approverName', key: 'approverName', width: 100 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="工时管理" description="工程师工时填报、提交、审批">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>填报工时</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="工程师ID">
          <a-input-number v-model:value="query.engineerId" placeholder="工程师ID" style="width: 130px" />
        </a-form-item>
        <a-form-item label="项目ID">
          <a-input-number v-model:value="query.projectId" placeholder="项目ID" style="width: 130px" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 130px">
            <a-select-option v-for="(v, k) in statusMap" :key="k" :value="k">{{ v.label }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="开始">
          <a-date-picker v-model:value="query.startDate" value-format="YYYY-MM-DD" style="width: 150px" />
        </a-form-item>
        <a-form-item label="结束">
          <a-date-picker v-model:value="query.endDate" value-format="YYYY-MM-DD" style="width: 150px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="vibe-card table-card">
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1300 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'hours'">
            <span class="tnum">{{ record.hours }}</span>
            <span v-if="record.overtimeHours" class="text-auxiliary"> (含 {{ record.overtimeHours }} 加班)</span>
          </template>
          <template v-else-if="column.key === 'workType'">{{ workTypeMap[record.workType] || record.workType }}</template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="statusMap[record.status]?.tone">{{ statusMap[record.status]?.label || record.status }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a v-if="record.status === 'DRAFT'" @click="handleSubmit0(record)">提交</a>
              <a v-if="record.status === 'SUBMITTED'" @click="openApprove(record, true)"><CheckOutlined /> 通过</a>
              <a v-if="record.status === 'SUBMITTED'" class="danger-link" @click="openApprove(record, false)"><CloseOutlined /> 驳回</a>
              <a v-if="record.status === 'DRAFT'" @click="openEdit(record)"><EditOutlined /></a>
              <a v-if="record.status === 'DRAFT'" class="danger-link" @click="handleDelete(record)"><DeleteOutlined /></a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无工时记录" action-text="填报工时" @action="openCreate" /></template>
      </a-table>
    </div>

    <a-modal v-model:open="formVisible" :title="isEdit ? '编辑工时' : '填报工时'" width="560px" :confirm-loading="formLoading" @ok="handleSubmit">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="工程师 ID" required>
              <a-input-number v-model:value="formData.engineerId" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="项目 ID" required>
              <a-input-number v-model:value="formData.projectId" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="任务 ID">
              <a-input-number v-model:value="formData.taskId" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="工作日期" required>
              <a-date-picker v-model:value="formData.workDate" style="width: 100%" value-format="YYYY-MM-DD" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="工时(h)" required>
              <a-input-number v-model:value="formData.hours" :min="0" :max="24" :step="0.5" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="加班工时(h)">
              <a-input-number v-model:value="formData.overtimeHours" :min="0" :max="16" :step="0.5" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="工作类型">
              <a-select v-model:value="formData.workType">
                <a-select-option v-for="(v, k) in workTypeMap" :key="k" :value="k">{{ v }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="工作内容">
              <a-textarea v-model:value="formData.description" :rows="2" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="approveVisible" :title="approveForm.approved ? '审批通过' : '驳回工时'" :confirm-loading="approveLoading" @ok="handleApprove">
      <a-form layout="vertical">
        <a-form-item label="备注">
          <a-textarea v-model:value="approveForm.remark" :rows="3" :placeholder="approveForm.approved ? '审批意见' : '请说明驳回原因'" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
.danger-link { color: @status-exception; }
</style>
