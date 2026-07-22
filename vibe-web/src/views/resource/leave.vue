<script setup lang="ts">
/**
 * 工程师请假管理
 * 请假单 CRUD（工程师选择/起止时间/原因）+ 审批
 * 审批通过后，前端调用排期接口（如可用）写入 LEAVE 时间块以标记不可分配时段。
 *
 * 注意：后端 EngineerLeaveController 尚未实现，接口已就绪（resource.ts），
 * 待后端补全后即可联调。
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, CheckOutlined, CloseOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageEngineerLeaves,
  createEngineerLeave,
  updateEngineerLeave,
  deleteEngineerLeave,
  approveEngineerLeave,
  createSchedule,
  pageEngineers
} from '@/api/resource'
import type {
  EngineerLeave,
  EngineerLeaveDTO,
  EngineerLeaveQueryParams,
  LeaveStatus,
  LeaveType
} from '@/types/resource'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<EngineerLeave[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50', '100']
})
const query = reactive<EngineerLeaveQueryParams>({
  engineerId: undefined,
  leaveType: undefined,
  status: undefined,
  startDate: undefined,
  endDate: undefined
})

async function loadData() {
  loading.value = true
  try {
    const res = (await pageEngineerLeaves({
      ...query,
      page: pagination.current,
      size: pagination.pageSize
    })) as unknown as PageResult<EngineerLeave>
    dataSource.value = res?.records || []
    pagination.total = res?.total || 0
  } catch (e) {
    console.error('[resource.leave] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  query.engineerId = undefined
  query.leaveType = undefined
  query.status = undefined
  query.startDate = undefined
  query.endDate = undefined
  handleSearch()
}

function handleTableChange(p: any) {
  pagination.current = p.current || 1
  pagination.pageSize = p.pageSize || 10
  loadData()
}

const statusMap: Record<LeaveStatus, { tone: any; label: string }> = {
  PENDING: { tone: 'warning', label: '待审批' },
  APPROVED: { tone: 'success', label: '已批准' },
  REJECTED: { tone: 'error', label: '已拒绝' }
}

const leaveTypeOptions: Array<{ value: LeaveType; label: string }> = [
  { value: 'ANNUAL', label: '年假' },
  { value: 'SICK', label: '病假' },
  { value: 'PERSONAL', label: '事假' },
  { value: 'OTHER', label: '其他' }
]

function leaveTypeLabel(code?: string): string {
  return leaveTypeOptions.find((t) => t.value === code)?.label || code || '—'
}

/* ============ 新增/编辑弹窗 ============ */
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<EngineerLeaveDTO>({
  engineerId: undefined,
  startDate: '',
  endDate: '',
  leaveType: 'ANNUAL',
  reason: ''
})

// 请假表单校验规则（异常处理三层闭环 SubTask 8.4 补充）
const leaveFormRules = {
  engineerId: [
    { required: true, message: '请选择工程师', trigger: 'change' }
  ],
  startDate: [
    { required: true, message: '请选择开始日期', trigger: 'change' }
  ],
  endDate: [
    { required: true, message: '请选择结束日期', trigger: 'change' }
  ],
  reason: [
    { max: 255, message: '请假原因长度不能超过 255', trigger: 'blur' }
  ]
}
const leaveFormRef = ref()

// ============ 下拉选项（实体引用字段） ============
const engineerOptions = ref<Array<{ value: string | number; label: string }>>([])

async function loadEngineerOptions() {
  try {
    const res = (await pageEngineers({ page: 1, size: 200 } as any)) as any
    engineerOptions.value = (res?.records || []).map((e: any) => ({ value: e.id, label: e.name }))
  } catch (e) {
    console.warn('[resource.leave] load engineers failed:', e)
  }
}

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    engineerId: undefined,
    startDate: '',
    endDate: '',
    leaveType: 'ANNUAL',
    reason: ''
  })
  formVisible.value = true
}

function openEdit(row: EngineerLeave) {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    engineerId: row.engineerId,
    startDate: row.startDate,
    endDate: row.endDate,
    leaveType: row.leaveType,
    reason: row.reason || ''
  })
  formVisible.value = true
}

async function handleSubmit() {
  // 异常处理三层闭环：先校验表单，再调用后端
  try {
    await leaveFormRef.value?.validate()
  } catch {
    return
  }
  if (!formData.engineerId) {
    message.warning('请选择工程师')
    return
  }
  if (!formData.startDate || !formData.endDate) {
    message.warning('请选择起止日期')
    return
  }
  if (formData.endDate < formData.startDate) {
    message.warning('结束日期不能早于开始日期')
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateEngineerLeave(formData.id, formData)
      message.success('更新成功')
    } else {
      await createEngineerLeave(formData)
      message.success('请假单已提交')
    }
    formVisible.value = false
    loadData()
  } catch (e) {
    console.error('[resource.leave] submit failed:', e)
  } finally {
    formLoading.value = false
  }
}

function handleDelete(row: EngineerLeave) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除请假单（${row.engineerName || '工程师#' + row.engineerId} ${row.startDate}~${row.endDate}）吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteEngineerLeave(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        console.error('[resource.leave] delete failed:', e)
      }
    }
  })
}

/** 审批：通过时同步写入排期 LEAVE 时间块以标记不可分配 */
async function handleApprove(row: EngineerLeave, approved: boolean) {
  const label = approved ? '批准' : '拒绝'
  Modal.confirm({
    title: `确认${label}`,
    content: `确定${label}该请假申请吗？`,
    async onOk() {
      try {
        await approveEngineerLeave(row.id, approved)
        // 审批通过时尝试同步排期（标记不可分配时段）
        if (approved) {
          try {
            await createSchedule({
              engineerId: row.engineerId,
              type: 'LEAVE',
              title: `请假（${leaveTypeLabel(row.leaveType)}）`,
              startDate: row.startDate,
              endDate: row.endDate,
              status: 'CONFIRMED',
              remark: row.reason || '请假标记不可分配'
            })
          } catch (e) {
            // 排期同步失败仅提示，不影响审批结果
            console.warn('[resource.leave] sync schedule failed:', e)
            message.warning('审批成功，但排期同步失败，请手动维护排期')
          }
        }
        message.success(`${label}成功`)
        loadData()
      } catch (e) {
        console.error('[resource.leave] approve failed:', e)
      }
    }
  })
}

const columns = [
  { title: '工程师', dataIndex: 'engineerName', key: 'engineerName', width: 120 },
  { title: '请假类型', dataIndex: 'leaveType', key: 'leaveType', width: 100 },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate', width: 120 },
  { title: '结束日期', dataIndex: 'endDate', key: 'endDate', width: 120 },
  { title: '天数', key: 'days', width: 80 },
  { title: '请假原因', dataIndex: 'reason', key: 'reason', ellipsis: true },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 230, fixed: 'right' as const }
]

function calcDays(start?: string, end?: string): number {
  if (!start || !end) return 0
  const s = new Date(start).getTime()
  const e = new Date(end).getTime()
  if (isNaN(s) || isNaN(e) || e < s) return 0
  return Math.floor((e - s) / 86400000) + 1
}

onMounted(() => {
  loadData()
  loadEngineerOptions()
})
</script>

<template>
  <PageContainer title="工程师请假" description="请假单申请与审批，通过后自动标记不可分配时段">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>请假申请</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="工程师">
          <a-select
            v-model:value="query.engineerId"
            placeholder="选择工程师"
            allow-clear
            show-search
            style="width: 180px"
            :options="engineerOptions"
            :filter-option="(input: string, option: any) => option.label.includes(input)"
          />
        </a-form-item>
        <a-form-item label="类型">
          <a-select v-model:value="query.leaveType" placeholder="全部" allow-clear style="width: 120px" :options="leaveTypeOptions" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 130px">
            <a-select-option v-for="(v, k) in statusMap" :key="k" :value="k">{{ v.label }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </div>

    <div class="vibe-card table-card">
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1200 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'leaveType'">
            <a-tag>{{ leaveTypeLabel(record.leaveType) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'days'">
            <span class="tnum">{{ calcDays(record.startDate, record.endDate) }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="statusMap[record.status as LeaveStatus]?.tone">
              {{ statusMap[record.status as LeaveStatus]?.label || record.status }}
            </StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small" wrap>
              <a v-if="record.status === 'PENDING'" @click="handleApprove(record, true)"><CheckOutlined /> 批准</a>
              <a v-if="record.status === 'PENDING'" class="danger-link" @click="handleApprove(record, false)"><CloseOutlined /> 拒绝</a>
              <a-divider v-if="record.status === 'PENDING'" type="vertical" />
              <a @click="openEdit(record)"><EditOutlined /> 编辑</a>
              <a-divider type="vertical" />
              <a class="danger-link" @click="handleDelete(record)"><DeleteOutlined /></a>
            </a-space>
          </template>
        </template>
        <template #emptyText>
          <EmptyState description="暂无请假记录" action-text="请假申请" @action="openCreate" />
        </template>
      </a-table>
    </div>

    <a-modal
      v-model:open="formVisible"
      :title="isEdit ? '编辑请假单' : '请假申请'"
      width="560px"
      :confirm-loading="formLoading"
      :mask-closable="false"
      @ok="handleSubmit"
    >
      <a-form ref="leaveFormRef" layout="vertical" :model="formData" :rules="leaveFormRules">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="工程师" name="engineerId" required>
              <a-select
                v-model:value="formData.engineerId"
                show-search
                placeholder="选择工程师"
                style="width: 100%"
                :options="engineerOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="请假类型" required>
              <a-select v-model:value="formData.leaveType" :options="leaveTypeOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="开始日期" name="startDate" required>
              <a-date-picker v-model:value="formData.startDate" value-format="YYYY-MM-DD" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结束日期" name="endDate" required>
              <a-date-picker v-model:value="formData.endDate" value-format="YYYY-MM-DD" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="请假原因" name="reason">
              <a-textarea v-model:value="formData.reason" :rows="3" placeholder="请假原因/说明" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
.danger-link { color: @status-exception; }
.tnum { font-variant-numeric: tabular-nums; }
</style>
