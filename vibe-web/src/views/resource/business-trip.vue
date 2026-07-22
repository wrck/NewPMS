<script setup lang="ts">
/**
 * 差旅管理
 * 差旅申请 CRUD + 行程信息（出发地/目的地/交通/住宿）+ 审批状态
 *
 * 注意：后端 BusinessTripController 尚未实现，接口已就绪（resource.ts），
 * 待后端补全后即可联调。
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, CheckOutlined, CloseOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageBusinessTrips,
  createBusinessTrip,
  updateBusinessTrip,
  deleteBusinessTrip,
  approveBusinessTrip,
  pageEngineers
} from '@/api/resource'
import { pageProjects, pageTasks } from '@/api/project'
import type {
  BusinessTrip,
  BusinessTripDTO,
  BusinessTripQueryParams,
  BusinessTripStatus,
  TransportMode
} from '@/types/resource'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<BusinessTrip[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50', '100']
})
const query = reactive<BusinessTripQueryParams>({
  engineerId: undefined,
  projectId: undefined,
  status: undefined,
  startDate: undefined,
  endDate: undefined
})

async function loadData() {
  loading.value = true
  try {
    const res = (await pageBusinessTrips({
      ...query,
      page: pagination.current,
      size: pagination.pageSize
    })) as unknown as PageResult<BusinessTrip>
    dataSource.value = res?.records || []
    pagination.total = res?.total || 0
  } catch (e) {
    console.error('[resource.business-trip] load failed:', e)
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
  query.projectId = undefined
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

const statusMap: Record<BusinessTripStatus, { tone: any; label: string }> = {
  PENDING: { tone: 'warning', label: '待审批' },
  APPROVED: { tone: 'success', label: '已批准' },
  REJECTED: { tone: 'error', label: '已拒绝' },
  COMPLETED: { tone: 'archived', label: '已完成' }
}

const transportOptions: Array<{ value: TransportMode; label: string }> = [
  { value: 'PLANE', label: '飞机' },
  { value: 'TRAIN', label: '火车' },
  { value: 'CAR', label: '汽车' },
  { value: 'OTHER', label: '其他' }

]

/* ============ 新增/编辑弹窗 ============ */
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<BusinessTripDTO>({
  engineerId: undefined,
  projectId: undefined,
  taskId: undefined,
  origin: '',
  destination: '',
  startDate: '',
  endDate: '',
  transportMode: 'TRAIN',
  accommodation: '',
  estimatedCost: undefined,
  actualCost: undefined,
  reason: '',
  remark: ''
})

// 差旅表单校验规则（异常处理三层闭环 SubTask 8.4 补充）
const tripFormRules = {
  engineerId: [
    { required: true, message: '请选择工程师', trigger: 'change' }
  ],
  origin: [
    { required: true, message: '请输入出发地', trigger: 'blur' },
    { max: 128, message: '出发地长度不能超过 128', trigger: 'blur' }
  ],
  destination: [
    { required: true, message: '请输入目的地', trigger: 'blur' },
    { max: 128, message: '目的地长度不能超过 128', trigger: 'blur' }
  ],
  startDate: [
    { required: true, message: '请选择开始日期', trigger: 'change' }
  ],
  endDate: [
    { required: true, message: '请选择结束日期', trigger: 'change' }
  ],
  reason: [
    { required: true, message: '请填写出差事由', trigger: 'blur' },
    { max: 512, message: '出差事由长度不能超过 512', trigger: 'blur' }
  ],
  accommodation: [
    { max: 255, message: '住宿信息长度不能超过 255', trigger: 'blur' }
  ],
  remark: [
    { max: 255, message: '备注长度不能超过 255', trigger: 'blur' }
  ]
}
const tripFormRef = ref()

// ============ 下拉选项（实体引用字段） ============
const engineerOptions = ref<Array<{ value: string | number; label: string }>>([])
const projectOptions = ref<Array<{ value: string | number; label: string }>>([])
const taskOptions = ref<Array<{ value: string | number; label: string }>>([])

async function loadEngineerOptions() {
  try {
    const res = (await pageEngineers({ page: 1, size: 200 } as any)) as any
    engineerOptions.value = (res?.records || []).map((e: any) => ({ value: e.id, label: e.name }))
  } catch (e) {
    console.warn('[resource.business-trip] load engineers failed:', e)
  }
}

async function loadProjectOptions() {
  try {
    const res = (await pageProjects({ page: 1, size: 200 } as any)) as any
    projectOptions.value = (res?.records || []).map((p: any) => ({ value: p.id, label: p.projectName }))
  } catch (e) {
    console.warn('[resource.business-trip] load projects failed:', e)
  }
}

async function loadTaskOptions(projectId?: string | number) {
  if (!projectId) {
    taskOptions.value = []
    return
  }
  try {
    const res = (await pageTasks({ projectId: projectId as any, page: 1, size: 200 } as any)) as any
    const list = res?.records || []
    taskOptions.value = list.map((t: any) => ({ value: t.id, label: t.taskName }))
  } catch (e) {
    console.warn('[resource.business-trip] load tasks failed:', e)
    taskOptions.value = []
  }
}

function handleProjectChange(value?: string | number) {
  formData.taskId = undefined
  loadTaskOptions(value)
}

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    engineerId: undefined,
    projectId: undefined,
    taskId: undefined,
    origin: '',
    destination: '',
    startDate: '',
    endDate: '',
    transportMode: 'TRAIN',
    accommodation: '',
    estimatedCost: undefined,
    actualCost: undefined,
    reason: '',
    remark: ''
  })
  taskOptions.value = []
  formVisible.value = true
}

function openEdit(row: BusinessTrip) {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    engineerId: row.engineerId,
    projectId: row.projectId,
    taskId: row.taskId,
    origin: row.origin || '',
    destination: row.destination || '',
    startDate: row.startDate || '',
    endDate: row.endDate || '',
    transportMode: row.transportMode || 'TRAIN',
    accommodation: row.accommodation || '',
    estimatedCost: row.estimatedCost,
    actualCost: row.actualCost,
    reason: row.reason || '',
    remark: row.remark || ''
  })
  loadTaskOptions(row.projectId)
  formVisible.value = true
}

async function handleSubmit() {
  // 异常处理三层闭环：先校验表单，再调用后端
  try {
    await tripFormRef.value?.validate()
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
      await updateBusinessTrip(formData.id, formData)
      message.success('更新成功')
    } else {
      await createBusinessTrip(formData)
      message.success('申请已提交')
    }
    formVisible.value = false
    loadData()
  } catch (e) {
    console.error('[resource.business-trip] submit failed:', e)
  } finally {
    formLoading.value = false
  }
}

function handleDelete(row: BusinessTrip) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除差旅记录「${row.engineerName || '#' + row.engineerId} - ${row.destination || ''}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteBusinessTrip(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        console.error('[resource.business-trip] delete failed:', e)
      }
    }
  })
}

function handleApprove(row: BusinessTrip, approved: boolean) {
  const label = approved ? '批准' : '拒绝'
  Modal.confirm({
    title: `确认${label}`,
    content: `确定${label}该差旅申请吗？`,
    async onOk() {
      try {
        await approveBusinessTrip(row.id, approved)
        message.success(`${label}成功`)
        loadData()
      } catch (e) {
        console.error('[resource.business-trip] approve failed:', e)
      }
    }
  })
}

const columns = [
  { title: '工程师', dataIndex: 'engineerName', key: 'engineerName', width: 110 },
  { title: '出发地', dataIndex: 'origin', key: 'origin', width: 120, ellipsis: true },
  { title: '目的地', dataIndex: 'destination', key: 'destination', width: 120, ellipsis: true },
  { title: '出差时间', key: 'dateRange', width: 200 },
  { title: '交通', dataIndex: 'transportMode', key: 'transportMode', width: 90 },
  { title: '住宿', dataIndex: 'accommodation', key: 'accommodation', width: 140, ellipsis: true },
  { title: '预估费用', dataIndex: 'estimatedCost', key: 'estimatedCost', width: 110 },
  { title: '关联项目', dataIndex: 'projectName', key: 'projectName', width: 140, ellipsis: true },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' as const }
]

function transportLabel(code?: string): string {
  return transportOptions.find((t) => t.value === code)?.label || code || '—'
}

onMounted(() => {
  loadData()
  loadEngineerOptions()
  loadProjectOptions()
})
</script>

<template>
  <PageContainer title="差旅管理" description="差旅申请、行程安排与审批记录">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>申请差旅</a-button>
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
        <a-form-item label="项目">
          <a-select
            v-model:value="query.projectId"
            placeholder="选择项目"
            allow-clear
            show-search
            style="width: 180px"
            :options="projectOptions"
            :filter-option="(input: string, option: any) => option.label.includes(input)"
          />
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
        :scroll="{ x: 1450 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'dateRange'">
            <span class="tnum">{{ record.startDate || '—' }} ~ {{ record.endDate || '—' }}</span>
          </template>
          <template v-else-if="column.key === 'transportMode'">
            <a-tag>{{ transportLabel(record.transportMode) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'estimatedCost'">
            <span class="tnum">{{ record.estimatedCost != null ? `¥${record.estimatedCost}` : '—' }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="statusMap[record.status as BusinessTripStatus]?.tone">
              {{ statusMap[record.status as BusinessTripStatus]?.label || record.status }}
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
          <EmptyState description="暂无差旅记录" action-text="申请差旅" @action="openCreate" />
        </template>
      </a-table>
    </div>

    <a-modal
      v-model:open="formVisible"
      :title="isEdit ? '编辑差旅申请' : '差旅申请'"
      width="780px"
      :confirm-loading="formLoading"
      :mask-closable="false"
      @ok="handleSubmit"
    >
      <a-form ref="tripFormRef" layout="vertical" :model="formData" :rules="tripFormRules">
        <a-row :gutter="16">
          <a-col :span="8">
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
          <a-col :span="8">
            <a-form-item label="关联项目">
              <a-select
                v-model:value="formData.projectId"
                show-search
                placeholder="选择项目"
                style="width: 100%"
                :options="projectOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
                @change="handleProjectChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="关联任务">
              <a-select
                v-model:value="formData.taskId"
                show-search
                placeholder="选择任务"
                style="width: 100%"
                :options="taskOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="出发地" name="origin" required>
              <a-input v-model:value="formData.origin" placeholder="如 北京" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="目的地" name="destination" required>
              <a-input v-model:value="formData.destination" placeholder="如 上海" />
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
          <a-col :span="8">
            <a-form-item label="交通方式">
              <a-select v-model:value="formData.transportMode" :options="transportOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="预估费用">
              <a-input-number v-model:value="formData.estimatedCost" :min="0" :precision="2" style="width: 100%" placeholder="元" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="实际费用">
              <a-input-number v-model:value="formData.actualCost" :min="0" :precision="2" style="width: 100%" placeholder="元" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="住宿信息" name="accommodation">
              <a-input v-model:value="formData.accommodation" placeholder="如 XX 酒店 / 公司宿舍" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="出差事由" name="reason" required>
              <a-textarea v-model:value="formData.reason" :rows="2" placeholder="出差事由/任务说明" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="formData.remark" :rows="2" />
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
