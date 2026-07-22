<script setup lang="ts">
/**
 * 项目预算管理
 * 设计文档 2.8.1：预算编制（人工/差旅/代理商/其他）+ 审批 + 调整 + 预算 vs 实际对比
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageFinanceBudgets,
  createFinanceBudget,
  updateFinanceBudget,
  deleteFinanceBudget,
  submitFinanceBudget,
  approveFinanceBudget,
  getFinanceBudgetDetail
} from '@/api/finance'
import { pageProjects } from '@/api/project'
import type { FinanceBudget, FinanceBudgetQuery, FinanceBudgetDTO, BudgetApprovalStatus } from '@/types/finance'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<FinanceBudget[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`
})
const query = reactive<FinanceBudgetQuery>({ projectId: undefined, year: undefined, approvalStatus: undefined })

const statusMap: Record<BudgetApprovalStatus, { label: string; color: string }> = {
  DRAFT: { label: '草稿', color: 'default' },
  PENDING: { label: '待审批', color: 'processing' },
  APPROVED: { label: '已通过', color: 'success' },
  REJECTED: { label: '已驳回', color: 'error' }
}

async function loadData() {
  loading.value = true
  try {
    const res = (await pageFinanceBudgets({
      ...query,
      page: pagination.current,
      size: pagination.pageSize
    })) as unknown as PageResult<FinanceBudget>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[finance.budget] load failed:', e)
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
  query.year = undefined
  query.approvalStatus = undefined
  handleSearch()
}

const columns = [
  { title: '项目', dataIndex: 'projectName', key: 'projectName', width: 140, ellipsis: true },
  { title: '年度', dataIndex: 'year', key: 'year', width: 100 },
  { title: '人工', dataIndex: 'laborAmount', key: 'laborAmount', width: 120 },
  { title: '差旅', dataIndex: 'travelAmount', key: 'travelAmount', width: 120 },
  { title: '代理商', dataIndex: 'agentAmount', key: 'agentAmount', width: 120 },
  { title: '总额', dataIndex: 'totalAmount', key: 'totalAmount', width: 140 },
  { title: '审批状态', dataIndex: 'approvalStatus', key: 'approvalStatus', width: 120 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' as const }
]

/* ============ 新增/编辑 ============ */
const modalVisible = ref(false)
const modalTitle = ref('')
const formRef = ref()
const form = reactive<FinanceBudgetDTO>({
  projectId: undefined,
  year: new Date().getFullYear(),
  laborAmount: 0,
  travelAmount: 0,
  agentAmount: 0,
  otherAmount: 0,
  remark: ''
})
const rules = {
  projectId: [{ required: true, message: '请选择项目' }],
  year: [{ required: true, message: '请输入年度' }]
}

/* ============ 实体引用下拉选项 ============ */
const projectOptions = ref<Array<{ value: string | number; label: string }>>([])

async function loadProjectOptions() {
  try {
    const res = await pageProjects({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    projectOptions.value = list.map((p: any) => ({ value: p.id, label: p.projectName }))
  } catch (e) {
    console.warn('[finance.budget] load projects failed:', e)
  }
}

function openCreate() {
  modalTitle.value = '新建预算'
  Object.assign(form, {
    id: undefined,
    projectId: undefined,
    year: new Date().getFullYear(),
    laborAmount: 0,
    travelAmount: 0,
    agentAmount: 0,
    otherAmount: 0,
    remark: ''
  })
  modalVisible.value = true
}

function openEdit(record: FinanceBudget) {
  modalTitle.value = '编辑预算'
  // BigDecimal 经 JacksonConfig 序列化为字符串，绑定 a-input-number 前转 number
  const num = (v: unknown) => (v != null ? Number(v) : 0)
  Object.assign(form, {
    id: record.id,
    projectId: record.projectId,
    year: record.year,
    laborAmount: num(record.laborAmount),
    travelAmount: num(record.travelAmount),
    agentAmount: num(record.agentAmount),
    otherAmount: num(record.otherAmount),
    remark: record.remark
  })
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
      await updateFinanceBudget(form.id, form)
      message.success('更新成功')
    } else {
      await createFinanceBudget(form)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch (e) {
    console.error('[finance.budget] save failed:', e)
  }
}

function handleDelete(record: FinanceBudget) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除项目「${record.projectName || record.projectId}」${record.year} 年预算吗？`,
    okText: '删除',
    okType: 'danger',
    async onOk() {
      try {
        await deleteFinanceBudget(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        console.error('[finance.budget] delete failed:', e)
      }
    }
  })
}

function handleSubmitApproval(record: FinanceBudget) {
  Modal.confirm({
    title: '提交审批',
    content: `确定提交项目「${record.projectName || record.projectId}」${record.year} 年预算审批吗？`,
    okText: '提交',
    async onOk() {
      try {
        await submitFinanceBudget(record.id)
        message.success('已提交审批')
        loadData()
      } catch (e) {
        console.error('[finance.budget] submit failed:', e)
      }
    }
  })
}

function handleApprove(record: FinanceBudget, passed: boolean) {
  Modal.confirm({
    title: passed ? '审批通过' : '审批驳回',
    content: `确定${passed ? '通过' : '驳回'}项目「${record.projectName || record.projectId}」的预算吗？`,
    okText: '确定',
    async onOk() {
      try {
        await approveFinanceBudget(record.id, passed)
        message.success('审批完成')
        loadData()
      } catch (e) {
        console.error('[finance.budget] approve failed:', e)
      }
    }
  })
}

/* ============ 详情对比 ============ */
const detailVisible = ref(false)
const detailRecord = ref<FinanceBudget | null>(null)

async function viewDetail(record: FinanceBudget) {
  try {
    detailRecord.value = (await getFinanceBudgetDetail(record.id)) as unknown as FinanceBudget
    detailVisible.value = true
  } catch (e) {
    console.error('[finance.budget] detail failed:', e)
  }
}

onMounted(() => {
  loadData()
  // 预加载搜索表单 / 弹窗下拉选项
  loadProjectOptions()
})
</script>

<template>
  <PageContainer title="项目预算" description="预算编制（人工/差旅/代理商/其他）→ 审批 → 预算 vs 实际对比">
    <template #extra>
      <a-button @click="handleReset">
        <template #icon><ReloadOutlined /></template>
        重置
      </a-button>
      <a-button type="primary" @click="openCreate">
        <template #icon><PlusOutlined /></template>
        新建预算
      </a-button>
    </template>

    <a-form layout="inline" style="margin-bottom: 16px" @submit.prevent="handleSearch">
      <a-form-item label="项目">
        <a-select
          v-model:value="query.projectId"
          show-search
          allow-clear
          placeholder="选择项目"
          style="width: 200px"
          :options="projectOptions"
          :filter-option="(input: string, option: any) => option.label.includes(input)"
        />
      </a-form-item>
      <a-form-item label="年度">
        <a-input-number v-model:value="query.year" placeholder="年度" :min="2020" :max="2099" style="width: 120px" />
      </a-form-item>
      <a-form-item label="审批状态">
        <a-select v-model:value="query.approvalStatus" placeholder="全部" allow-clear style="width: 140px">
          <a-select-option v-for="(v, k) in statusMap" :key="k" :value="k">{{ v.label }}</a-select-option>
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
      :row-key="(record: FinanceBudget) => record.id"
      @change="(p: any) => { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }"
    >
      <template #emptyText>
        <EmptyState description="暂无预算数据" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'projectName'">
          {{ record.projectName || record.projectId }}
        </template>
        <template v-else-if="column.key === 'approvalStatus'">
          <a-tag :color="statusMap[record.approvalStatus as BudgetApprovalStatus]?.color">
            {{ statusMap[record.approvalStatus as BudgetApprovalStatus]?.label }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="viewDetail(record)">
            <EyeOutlined /> 详情
          </a-button>
          <a-button v-if="record.approvalStatus === 'DRAFT'" type="link" size="small" @click="openEdit(record)">
            <EditOutlined /> 编辑
          </a-button>
          <a-button v-if="record.approvalStatus === 'DRAFT'" type="link" size="small" @click="handleSubmitApproval(record)">
            提交审批
          </a-button>
          <a-button v-if="record.approvalStatus === 'PENDING'" type="link" size="small" @click="handleApprove(record, true)">
            通过
          </a-button>
          <a-button v-if="record.approvalStatus === 'PENDING'" type="link" size="small" danger @click="handleApprove(record, false)">
            驳回
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
            <a-form-item label="预算年度" name="year">
              <a-input-number v-model:value="form.year" :min="2020" :max="2099" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="人工预算">
              <a-input-number v-model:value="form.laborAmount" :min="0" :step="1000" :precision="2" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="差旅预算">
              <a-input-number v-model:value="form.travelAmount" :min="0" :step="1000" :precision="2" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="代理商预算">
              <a-input-number v-model:value="form.agentAmount" :min="0" :step="1000" :precision="2" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="其他预算">
              <a-input-number v-model:value="form.otherAmount" :min="0" :step="1000" :precision="2" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注">
          <a-textarea v-model:value="form.remark" :rows="2" placeholder="备注" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情对比弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="预算 vs 实际对比"
      width="720px"
      :footer="null"
    >
      <a-table
        v-if="detailRecord"
        :data-source="[
          { item: '人工', budget: detailRecord.laborAmount, actual: detailRecord.actualLabor },
          { item: '差旅', budget: detailRecord.travelAmount, actual: detailRecord.actualTravel },
          { item: '代理商', budget: detailRecord.agentAmount, actual: detailRecord.actualAgent },
          { item: '其他', budget: detailRecord.otherAmount, actual: detailRecord.actualOther },
          { item: '合计', budget: detailRecord.totalAmount, actual: detailRecord.actualTotal }
        ]"
        :pagination="false"
        :row-key="(r: any) => r.item"
        size="small"
      >
        <a-table-column title="项" data-index="item" />
        <a-table-column title="预算" data-index="budget" />
        <a-table-column title="实际" data-index="actual" />
      </a-table>
    </a-modal>
  </PageContainer>
</template>
