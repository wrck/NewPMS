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
  approveBusinessTrip
} from '@/api/resource'
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
  engineerId: 0,
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

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    engineerId: 0,
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
  formVisible.value = true
}

async function handleSubmit() {
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
        <a-form-item label="工程师 ID">
          <a-input-number v-model:value="query.engineerId" placeholder="工程师 ID" style="width: 150px" />
        </a-form-item>
        <a-form-item label="项目 ID">
          <a-input-number v-model:value="query.projectId" placeholder="项目 ID" style="width: 150px" />
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
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="工程师 ID" required>
              <a-input-number v-model:value="formData.engineerId" style="width: 100%" placeholder="工程师 ID" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="关联项目 ID">
              <a-input-number v-model:value="formData.projectId" style="width: 100%" placeholder="项目 ID" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="关联任务 ID">
              <a-input-number v-model:value="formData.taskId" style="width: 100%" placeholder="任务 ID" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="出发地" required>
              <a-input v-model:value="formData.origin" placeholder="如 北京" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="目的地" required>
              <a-input v-model:value="formData.destination" placeholder="如 上海" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="开始日期" required>
              <a-date-picker v-model:value="formData.startDate" value-format="YYYY-MM-DD" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结束日期" required>
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
            <a-form-item label="住宿信息">
              <a-input v-model:value="formData.accommodation" placeholder="如 XX 酒店 / 公司宿舍" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="出差事由" required>
              <a-textarea v-model:value="formData.reason" :rows="2" placeholder="出差事由/任务说明" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注">
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
