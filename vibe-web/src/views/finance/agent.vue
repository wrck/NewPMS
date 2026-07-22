<script setup lang="ts">
/**
 * 代理商结算
 * 设计文档 2.8.3：工作量确认单 → 费用计算 → 对账 → 审批流 → 付款跟踪
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageFinanceSettlements,
  createFinanceSettlement,
  updateFinanceSettlement,
  deleteFinanceSettlement,
  pmConfirmSettlement,
  agentConfirmSettlement,
  directorApproveSettlement,
  financeApproveSettlement,
  updateSettlementPaymentStatus
} from '@/api/finance'
import { pageProjects } from '@/api/project'
import { pageAgentCompanies, pageOutsourceTasks } from '@/api/agent'
import type { FinanceWorkloadConfirmation, FinanceWorkloadQuery, FinanceWorkloadDTO, SettlementApprovalStatus, PaymentStatus } from '@/types/finance'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<FinanceWorkloadConfirmation[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`
})
const query = reactive<FinanceWorkloadQuery>({ projectId: undefined, agentCompanyId: undefined, period: '', approvalStatus: undefined, paymentStatus: undefined })

const approvalStatusMap: Record<SettlementApprovalStatus, { label: string; color: string }> = {
  DRAFT: { label: '草稿', color: 'default' },
  PM_CONFIRMED: { label: 'PM已确认', color: 'processing' },
  AGENT_CONFIRMED: { label: '代理商已确认', color: 'blue' },
  PENDING: { label: '待审批', color: 'warning' },
  DIRECTOR_APPROVED: { label: '总监已审批', color: 'cyan' },
  FINANCE_APPROVED: { label: '财务已审批', color: 'success' },
  REJECTED: { label: '已驳回', color: 'error' }
}

const paymentStatusMap: Record<PaymentStatus, { label: string; color: string }> = {
  UNPAID: { label: '未付款', color: 'default' },
  PAYING: { label: '付款中', color: 'processing' },
  PAID: { label: '已付款', color: 'success' }
}

async function loadData() {
  loading.value = true
  try {
    const res = (await pageFinanceSettlements({
      ...query,
      page: pagination.current,
      size: pagination.pageSize
    })) as unknown as PageResult<FinanceWorkloadConfirmation>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[finance.settlement] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  query.projectId = undefined
  query.agentCompanyId = undefined
  query.period = ''
  query.approvalStatus = undefined
  query.paymentStatus = undefined
  handleSearch()
}

const columns = [
  { title: '项目', dataIndex: 'projectName', key: 'projectName', width: 140, ellipsis: true },
  { title: '代理商', dataIndex: 'agentCompanyName', key: 'agentCompanyName', width: 140, ellipsis: true },
  { title: '对账周期', dataIndex: 'period', key: 'period', width: 120 },
  { title: '工作量(人天)', dataIndex: 'workloadDays', key: 'workloadDays', width: 120 },
  { title: '结算总额', dataIndex: 'totalAmount', key: 'totalAmount', width: 140 },
  { title: '审批状态', dataIndex: 'approvalStatus', key: 'approvalStatus', width: 140 },
  { title: '付款状态', dataIndex: 'paymentStatus', key: 'paymentStatus', width: 120 },
  { title: '操作', key: 'action', width: 280, fixed: 'right' as const }
]

/* ============ 新增/编辑 ============ */
const modalVisible = ref(false)
const modalTitle = ref('')
const formRef = ref()
const form = reactive<FinanceWorkloadDTO>({
  projectId: undefined,
  outsourceTaskId: undefined,
  agentCompanyId: undefined,
  period: '',
  workloadDays: 0,
  unitPrice: 0,
  travelAmount: 0,
  otherAmount: 0,
  remark: ''
})
const rules = {
  projectId: [{ required: true, message: '请选择项目' }],
  agentCompanyId: [{ required: true, message: '请选择代理商' }],
  period: [{ required: true, message: '请输入对账周期' }],
  workloadDays: [{ required: true, message: '请输入工作量' }],
  unitPrice: [{ required: true, message: '请输入人天单价' }]
}

/* ============ 实体引用下拉选项 ============ */
const projectOptions = ref<Array<{ value: string | number; label: string }>>([])
const agentCompanyOptions = ref<Array<{ value: string | number; label: string }>>([])
const outsourceTaskOptions = ref<Array<{ value: string | number; label: string }>>([])

async function loadProjectOptions() {
  try {
    const res = await pageProjects({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    projectOptions.value = list.map((p: any) => ({ value: p.id, label: p.projectName }))
  } catch (e) {
    console.warn('[finance.settlement] load projects failed:', e)
  }
}

async function loadAgentCompanyOptions() {
  try {
    const res = await pageAgentCompanies({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    agentCompanyOptions.value = list.map((c: any) => ({ value: c.id, label: c.companyName }))
  } catch (e) {
    console.warn('[finance.settlement] load agent companies failed:', e)
  }
}

async function loadOutsourceTaskOptions(agentCompanyId?: string | number) {
  if (!agentCompanyId) {
    outsourceTaskOptions.value = []
    return
  }
  try {
    const res = await pageOutsourceTasks({ page: 1, size: 200, agentCompanyId } as any)
    const list = (res as any)?.records || []
    outsourceTaskOptions.value = list.map((t: any) => ({ value: t.id, label: t.taskName }))
  } catch (e) {
    console.warn('[finance.settlement] load outsource tasks failed:', e)
  }
}

// 代理商变更时清空转包任务并重新加载
function handleAgentCompanyChange(value: string | number) {
  form.outsourceTaskId = undefined
  loadOutsourceTaskOptions(value)
}

function openCreate() {
  modalTitle.value = '新建结算单'
  const now = new Date()
  Object.assign(form, {
    id: undefined,
    projectId: undefined,
    outsourceTaskId: undefined,
    agentCompanyId: undefined,
    period: `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`,
    workloadDays: 0,
    unitPrice: 0,
    travelAmount: 0,
    otherAmount: 0,
    remark: ''
  })
  outsourceTaskOptions.value = []
  modalVisible.value = true
}

function openEdit(record: FinanceWorkloadConfirmation) {
  modalTitle.value = '编辑结算单'
  // BigDecimal 经 JacksonConfig 序列化为字符串，绑定 a-input-number 前转 number
  const num = (v: unknown) => (v != null ? Number(v) : 0)
  Object.assign(form, {
    id: record.id,
    projectId: record.projectId,
    outsourceTaskId: record.outsourceTaskId,
    agentCompanyId: record.agentCompanyId,
    period: record.period,
    workloadDays: num(record.workloadDays),
    unitPrice: num(record.unitPrice),
    travelAmount: num(record.travelAmount),
    otherAmount: num(record.otherAmount),
    remark: record.remark
  })
  // 编辑时按当前代理商加载转包任务选项
  loadOutsourceTaskOptions(record.agentCompanyId)
  modalVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  try {
    if (form.id) {
      await updateFinanceSettlement(form.id, form)
      message.success('更新成功')
    } else {
      await createFinanceSettlement(form)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch (e) {
    console.error('[finance.settlement] save failed:', e)
  }
}

function handleDelete(record: FinanceWorkloadConfirmation) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除该结算单吗？`,
    okText: '删除',
    okType: 'danger',
    async onOk() {
      try {
        await deleteFinanceSettlement(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        console.error('[finance.settlement] delete failed:', e)
      }
    }
  })
}

async function handleAction(record: FinanceWorkloadConfirmation, action: string) {
  try {
    if (action === 'pm') await pmConfirmSettlement(record.id)
    else if (action === 'agent') await agentConfirmSettlement(record.id)
    else if (action === 'director-pass') await directorApproveSettlement(record.id, true)
    else if (action === 'director-reject') await directorApproveSettlement(record.id, false)
    else if (action === 'finance-pass') await financeApproveSettlement(record.id, true)
    else if (action === 'finance-reject') await financeApproveSettlement(record.id, false)
    else if (action === 'paying') await updateSettlementPaymentStatus(record.id, 'PAYING')
    else if (action === 'paid') await updateSettlementPaymentStatus(record.id, 'PAID')
    message.success('操作成功')
    loadData()
  } catch (e) {
    console.error('[finance.settlement] action failed:', e)
  }
}

onMounted(() => {
  loadData()
  // 预加载搜索表单 / 弹窗下拉选项
  loadProjectOptions()
  loadAgentCompanyOptions()
})
</script>

<template>
  <PageContainer title="代理商结算" description="工作量确认单 → 费用计算 → 对账 → 审批流 → 付款跟踪">
    <template #extra>
      <a-button @click="handleReset">
        <template #icon><ReloadOutlined /></template>
        重置
      </a-button>
      <a-button type="primary" @click="openCreate">
        <template #icon><PlusOutlined /></template>
        新建结算单
      </a-button>
    </template>

    <a-form layout="inline" style="margin-bottom: 16px" @submit.prevent="handleSearch">
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
      <a-form-item label="对账周期">
        <a-input v-model:value="query.period" placeholder="YYYY-MM" allow-clear style="width: 120px" />
      </a-form-item>
      <a-form-item label="审批状态">
        <a-select v-model:value="query.approvalStatus" placeholder="全部" allow-clear style="width: 160px">
          <a-select-option v-for="(v, k) in approvalStatusMap" :key="k" :value="k">{{ v.label }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">查询</a-button>
      </a-form-item>
    </a-form>

    <a-table
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      :row-key="(record: FinanceWorkloadConfirmation) => record.id"
      @change="(p: any) => { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }"
    >
      <template #emptyText>
        <EmptyState description="暂无结算单" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'projectName'">
          {{ record.projectName || record.projectId }}
        </template>
        <template v-else-if="column.key === 'agentCompanyName'">
          {{ record.agentCompanyName || record.agentCompanyId }}
        </template>
        <template v-else-if="column.key === 'approvalStatus'">
          <a-tag :color="approvalStatusMap[record.approvalStatus as SettlementApprovalStatus]?.color">
            {{ approvalStatusMap[record.approvalStatus as SettlementApprovalStatus]?.label }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'paymentStatus'">
          <a-tag :color="paymentStatusMap[record.paymentStatus as PaymentStatus]?.color">
            {{ paymentStatusMap[record.paymentStatus as PaymentStatus]?.label }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button v-if="record.approvalStatus === 'DRAFT'" type="link" size="small" @click="openEdit(record)">
            <EditOutlined />
          </a-button>
          <a-button v-if="record.approvalStatus === 'DRAFT'" type="link" size="small" @click="handleAction(record, 'pm')">
            PM确认
          </a-button>
          <a-button v-if="record.approvalStatus === 'PM_CONFIRMED'" type="link" size="small" @click="handleAction(record, 'agent')">
            代理商确认
          </a-button>
          <a-button v-if="record.approvalStatus === 'PENDING'" type="link" size="small" @click="handleAction(record, 'director-pass')">
            总监通过
          </a-button>
          <a-button v-if="record.approvalStatus === 'DIRECTOR_APPROVED'" type="link" size="small" @click="handleAction(record, 'finance-pass')">
            财务通过
          </a-button>
          <a-button v-if="record.approvalStatus === 'FINANCE_APPROVED' && record.paymentStatus === 'UNPAID'" type="link" size="small" @click="handleAction(record, 'paying')">
            标记付款中
          </a-button>
          <a-button v-if="record.paymentStatus === 'PAYING'" type="link" size="small" @click="handleAction(record, 'paid')">
            标记已付款
          </a-button>
          <a-button v-if="record.approvalStatus === 'DRAFT'" type="link" size="small" danger @click="handleDelete(record)">
            <DeleteOutlined />
          </a-button>
        </template>
      </template>
    </a-table>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="640px"
      :ok-text="form.id ? '保存' : '创建'"
      cancel-text="取消"
      @ok="handleSubmit"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="项目" name="projectId">
              <a-select
                v-model:value="form.projectId"
                show-search
                placeholder="选择项目"
                style="width: 100%"
                :options="projectOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="代理商" name="agentCompanyId">
              <a-select
                v-model:value="form.agentCompanyId"
                show-search
                placeholder="选择代理商"
                style="width: 100%"
                :options="agentCompanyOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
                @change="handleAgentCompanyChange"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="对账周期" name="period">
              <a-input v-model:value="form.period" placeholder="YYYY-MM 或 PROJECT" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="转包任务">
              <a-select
                v-model:value="form.outsourceTaskId"
                show-search
                allow-clear
                placeholder="先选择代理商"
                style="width: 100%"
                :options="outsourceTaskOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="工作量(人天)" name="workloadDays">
              <a-input-number v-model:value="form.workloadDays" :min="0" :step="0.5" :precision="2" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="人天单价" name="unitPrice">
              <a-input-number v-model:value="form.unitPrice" :min="0" :step="100" :precision="2" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="差旅费用">
              <a-input-number v-model:value="form.travelAmount" :min="0" :step="100" :precision="2" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="其他费用">
              <a-input-number v-model:value="form.otherAmount" :min="0" :step="100" :precision="2" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注">
          <a-textarea v-model:value="form.remark" :rows="2" placeholder="备注" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>
