<script setup lang="ts">
/**
 * 结算管理
 * 代理商工作量确认与结算：工作量列表、PM 确认、审批（PM → 总监 → 财务）
 */
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined,
  CheckOutlined,
  CloseOutlined,
  EditOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  listWorkloads,
  getTaskWorkload,
  confirmWorkload,
  approveWorkload
} from '@/api/agent'
import type {
  OutsourceWorkload,
  WorkloadQueryParams,
  WorkloadConfirmDTO
} from '@/types/agent'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<OutsourceWorkload[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const query = reactive<WorkloadQueryParams>({ agentCompanyId: undefined, projectId: undefined, status: undefined, startBegin: '', startEnd: '' })

async function loadData() {
  loading.value = true
  try {
    const res = (await listWorkloads({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<OutsourceWorkload>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[agent.settlement] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

const statusMap: Record<string, { tone: any; label: string }> = {
  DRAFT: { tone: 'default', label: '草稿' },
  PM_CONFIRMED: { tone: 'processing', label: 'PM 已确认' },
  AGENT_CONFIRMED: { tone: 'processing', label: '代理商已确认' },
  PENDING: { tone: 'warning', label: '待审批' },
  DIRECTOR_APPROVED: { tone: 'processing', label: '总监已审批' },
  FINANCE_APPROVED: { tone: 'success', label: '财务已审批' },
  REJECTED: { tone: 'error', label: '已驳回' },
  CLOSED: { tone: 'archived', label: '已关闭' }
}

const columns = [
  { title: '所属项目', dataIndex: 'projectName', key: 'projectName', ellipsis: true },
  { title: '代理商', dataIndex: 'agentCompanyName', key: 'agentCompanyName', width: 150, ellipsis: true },
  { title: '人天', key: 'manDays', width: 90 },
  { title: '站点数', dataIndex: 'siteCount', key: 'siteCount', width: 90 },
  { title: '设备数', dataIndex: 'deviceCount', key: 'deviceCount', width: 90 },
  { title: '出差天数', dataIndex: 'travelDays', key: 'travelDays', width: 100 },
  { title: '其他费用', key: 'otherCost', width: 110 },
  { title: '结算金额', key: 'totalAmount', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '确认人', dataIndex: 'confirmByName', key: 'confirmByName', width: 100 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]

// 确认工作量弹窗（PM 录入/调整工作量）
const confirmVisible = ref(false)
const confirmLoading = ref(false)
const confirmRow = ref<OutsourceWorkload | null>(null)
const confirmForm = reactive<WorkloadConfirmDTO>({
  manDays: 0,
  siteCount: undefined,
  deviceCount: undefined,
  travelDays: undefined,
  otherCost: undefined,
  totalAmount: 0,
  remark: ''
})

async function openConfirm(row: OutsourceWorkload) {
  confirmRow.value = row
  // 若已有工作量数据则回填，否则尝试从任务拉取
  let base: Partial<OutsourceWorkload> = row
  try {
    base = (await getTaskWorkload(row.taskId)) as unknown as OutsourceWorkload
  } catch (e) {
    // 使用当前行
  }
  Object.assign(confirmForm, {
    manDays: base.manDays || 0,
    siteCount: base.siteCount,
    deviceCount: base.deviceCount,
    travelDays: base.travelDays,
    otherCost: base.otherCost,
    totalAmount: base.totalAmount || 0,
    remark: base.remark || ''
  })
  confirmVisible.value = true
}

async function handleConfirm() {
  if (!confirmRow.value) return
  if (!confirmForm.manDays || confirmForm.totalAmount <= 0) {
    message.warning('请填写人天和结算金额')
    return
  }
  confirmLoading.value = true
  try {
    await confirmWorkload(confirmRow.value.taskId, confirmForm)
    message.success('工作量已确认')
    confirmVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    confirmLoading.value = false
  }
}

// 审批弹窗（审批通过/驳回）
const approveVisible = ref(false)
const approveLoading = ref(false)
const approveRow = ref<OutsourceWorkload | null>(null)
const approveForm = reactive({ approved: true, remark: '' })

function openApprove(row: OutsourceWorkload, approved: boolean) {
  approveRow.value = row
  approveForm.approved = approved
  approveForm.remark = ''
  approveVisible.value = true
}

async function handleApprove() {
  if (!approveRow.value) return
  if (!approveForm.approved && !approveForm.remark) {
    message.warning('请填写驳回原因')
    return
  }
  approveLoading.value = true
  try {
    await approveWorkload(approveRow.value.taskId, approveForm.approved, approveForm.remark)
    message.success(approveForm.approved ? '已审批通过' : '已驳回')
    approveVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    approveLoading.value = false
  }
}

function formatMoney(v?: number) {
  if (v == null) return '-'
  return `¥${v.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="结算管理" description="代理商工作量确认、审批与结算">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="代理商ID">
          <a-input-number v-model:value="query.agentCompanyId" placeholder="代理商ID" style="width: 130px" />
        </a-form-item>
        <a-form-item label="项目ID">
          <a-input-number v-model:value="query.projectId" placeholder="项目ID" style="width: 130px" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 130px">
            <a-select-option v-for="(v, k) in statusMap" :key="k" :value="k">{{ v.label }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="开始起">
          <a-date-picker v-model:value="query.startBegin" value-format="YYYY-MM-DD" style="width: 150px" />
        </a-form-item>
        <a-form-item label="开始止">
          <a-date-picker v-model:value="query.startEnd" value-format="YYYY-MM-DD" style="width: 150px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="vibe-card table-card">
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1500 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'manDays'">
            <span class="tnum">{{ record.manDays }}</span>
          </template>
          <template v-else-if="column.key === 'otherCost'">
            <span class="tnum">{{ formatMoney(record.otherCost) }}</span>
          </template>
          <template v-else-if="column.key === 'totalAmount'">
            <span class="tnum text-strong">{{ formatMoney(record.totalAmount) }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="statusMap[record.status]?.tone">{{ statusMap[record.status]?.label || record.status }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a v-if="record.status === 'DRAFT'" @click="openConfirm(record)"><EditOutlined /> 确认</a>
              <a v-if="record.status === 'PENDING'" @click="openApprove(record, true)"><CheckOutlined /> 通过</a>
              <a v-if="record.status === 'PENDING'" class="danger-link" @click="openApprove(record, false)"><CloseOutlined /> 驳回</a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无结算记录" /></template>
      </a-table>
    </div>

    <!-- 确认工作量 -->
    <a-modal v-model:open="confirmVisible" title="确认工作量" width="600px" :confirm-loading="confirmLoading" @ok="handleConfirm">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="人天" required>
              <a-input-number v-model:value="confirmForm.manDays" :min="0" :step="0.5" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结算金额" required>
              <a-input-number v-model:value="confirmForm.totalAmount" :min="0" :step="100" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="站点数">
              <a-input-number v-model:value="confirmForm.siteCount" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="设备数">
              <a-input-number v-model:value="confirmForm.deviceCount" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="出差天数">
              <a-input-number v-model:value="confirmForm.travelDays" :min="0" :step="0.5" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="其他费用">
              <a-input-number v-model:value="confirmForm.otherCost" :min="0" :step="100" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注">
              <a-textarea v-model:value="confirmForm.remark" :rows="2" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 审批弹窗 -->
    <a-modal v-model:open="approveVisible" :title="approveForm.approved ? '审批通过' : '驳回结算'" :confirm-loading="approveLoading" @ok="handleApprove">
      <a-alert v-if="!approveForm.approved" message="驳回后工作量将退回 PM 重新确认" type="warning" show-icon style="margin-bottom: 12px" />
      <a-form layout="vertical">
        <a-form-item :label="approveForm.approved ? '审批意见' : '驳回原因'" :required="!approveForm.approved">
          <a-textarea v-model:value="approveForm.remark" :rows="3" :placeholder="approveForm.approved ? '可填写审批意见' : '请说明驳回原因'" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
.danger-link { color: @status-exception; }
.text-strong { font-weight: 600; color: @text-primary; }
</style>
