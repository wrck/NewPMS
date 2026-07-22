<script setup lang="ts">
/**
 * 结算管理
 * 代理商工作量确认：工作量列表、PM 确认/驳回
 */
import { ref, reactive, onMounted, h } from 'vue'
import { message, Modal, Input as AInput } from 'ant-design-vue'
import {
  ReloadOutlined,
  CheckOutlined,
  CloseOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  listWorkloads,
  getTaskWorkload,
  confirmWorkload,
  rejectWorkload,
  pageAgentCompanies
} from '@/api/agent'
import { pageProjects } from '@/api/project'
import type {
  OutsourceWorkload,
  WorkloadQueryParams,
  WorkloadConfirmDTO
} from '@/types/agent'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<OutsourceWorkload[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const query = reactive<WorkloadQueryParams>({ agentCompanyId: undefined, projectId: undefined, status: undefined, beginTime: '', endTime: '' })

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

// ============ 实体引用下拉选项 ============
const projectOptions = ref<Array<{ value: string | number; label: string }>>([])
const agentCompanyOptions = ref<Array<{ value: string | number; label: string }>>([])

async function loadProjectOptions() {
  try {
    const res = await pageProjects({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    projectOptions.value = list.map((p: any) => ({ value: p.id, label: p.projectName }))
  } catch (e) {
    console.warn('[agent.settlement] load projects failed:', e)
  }
}

async function loadAgentCompanyOptions() {
  try {
    const res = await pageAgentCompanies({ page: 1, size: 200 })
    const list = (res as any)?.records || []
    agentCompanyOptions.value = list.map((c: any) => ({ value: c.id, label: c.companyName }))
  } catch (e) {
    console.warn('[agent.settlement] load agent companies failed:', e)
  }
}

const statusMap: Record<string, { tone: any; label: string }> = {
  SUBMITTED: { tone: 'warning', label: '待确认' },
  CONFIRMED: { tone: 'success', label: '已确认' },
  REJECTED: { tone: 'error', label: '已驳回' }
}

const columns = [
  { title: '所属项目', dataIndex: 'projectName', key: 'projectName', ellipsis: true },
  { title: '代理商', dataIndex: 'agentCompanyName', key: 'agentCompanyName', width: 150, ellipsis: true },
  { title: '人天', key: 'manDays', width: 90 },
  { title: '站点数', dataIndex: 'siteCount', key: 'siteCount', width: 90 },
  { title: '设备数', dataIndex: 'deviceCount', key: 'deviceCount', width: 90 },
  { title: '状态', key: 'status', width: 100 },
  { title: '确认人', dataIndex: 'confirmByName', key: 'confirmByName', width: 100 },
  { title: '提交时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
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
  remark: ''
})

async function openConfirm(row: OutsourceWorkload) {
  confirmRow.value = row
  // 若已有工作量数据则回填，否则尝试从任务拉取（后端返回数组，取最新一条）
  let base: Partial<OutsourceWorkload> = row
  try {
    const arr = (await getTaskWorkload(row.taskId)) as unknown as OutsourceWorkload[]
    base = arr[0] || row
  } catch (e) {
    // 使用当前行
  }
  Object.assign(confirmForm, {
    manDays: base.manDays || 0,
    siteCount: base.siteCount,
    deviceCount: base.deviceCount,
    remark: base.remark || ''
  })
  confirmVisible.value = true
}

async function handleConfirm() {
  if (!confirmRow.value) return
  confirmLoading.value = true
  try {
    await confirmWorkload(confirmRow.value.taskId, confirmRow.value.id)
    message.success('工作量已确认')
    confirmVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    confirmLoading.value = false
  }
}

function openReject(row: OutsourceWorkload) {
  const state = reactive({ remark: '' })
  Modal.confirm({
    title: '驳回工作量',
    content: () => h('div', [
      h('p', { style: 'margin-bottom: 8px' }, '驳回后工作量将退回代理商重新提交'),
      h(AInput.TextArea, {
        value: state.remark,
        'onUpdate:value': (v: string) => { state.remark = v },
        rows: 3,
        placeholder: '请说明驳回原因'
      })
    ]),
    okType: 'danger',
    async onOk() {
      try {
        await rejectWorkload(row.taskId, row.id, state.remark)
        message.success('已驳回')
        loadData()
      } catch (e) {
        // ignore
      }
    }
  })
}

function formatMoney(v?: number | string) {
  if (v == null || v === '') return '-'
  const n = Number(v)
  if (isNaN(n)) return '-'
  return `¥${n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

onMounted(() => {
  loadData()
  loadProjectOptions()
  loadAgentCompanyOptions()
})
</script>

<template>
  <PageContainer title="结算管理" description="代理商工作量确认与结算">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="代理商">
          <a-select
            v-model:value="query.agentCompanyId"
            show-search
            allow-clear
            placeholder="选择代理商"
            style="width: 180px"
            :options="agentCompanyOptions"
            :filter-option="(input: string, option: any) => option.label.includes(input)"
          />
        </a-form-item>
        <a-form-item label="项目">
          <a-select
            v-model:value="query.projectId"
            show-search
            allow-clear
            placeholder="选择项目"
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
        <a-form-item label="开始起">
          <a-date-picker v-model:value="query.beginTime" value-format="YYYY-MM-DD" style="width: 150px" />
        </a-form-item>
        <a-form-item label="开始止">
          <a-date-picker v-model:value="query.endTime" value-format="YYYY-MM-DD" style="width: 150px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="vibe-card table-card">
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1300 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'manDays'">
            <span class="tnum">{{ record.manDays }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="statusMap[record.status]?.tone">{{ statusMap[record.status]?.label || record.status }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a v-if="record.status === 'SUBMITTED'" @click="openConfirm(record)"><CheckOutlined /> 确认</a>
              <a v-if="record.status === 'SUBMITTED'" class="danger-link" @click="openReject(record)"><CloseOutlined /> 驳回</a>
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
          <a-col :span="8">
            <a-form-item label="人天" required>
              <a-input-number v-model:value="confirmForm.manDays" :min="0" :step="0.5" style="width: 100%" />
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
          <a-col :span="24">
            <a-form-item label="备注">
              <a-textarea v-model:value="confirmForm.remark" :rows="2" />
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
</style>
