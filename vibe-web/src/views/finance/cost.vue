<script setup lang="ts">
/**
 * 成本归集
 * 设计文档 2.8.2：人工成本/差旅费用/代理商费用/其他费用 → 项目维度
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageFinanceCosts,
  createFinanceCost,
  updateFinanceCost,
  deleteFinanceCost
} from '@/api/finance'
import { pageProjects } from '@/api/project'
import { pageOutsourceTasks } from '@/api/agent'
import type { FinanceCost, FinanceCostQuery, FinanceCostDTO, FinanceCostType } from '@/types/finance'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<FinanceCost[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`
})
const query = reactive<FinanceCostQuery>({ projectId: undefined, costType: undefined, startDate: '', endDate: '' })

const costTypeMap: Record<FinanceCostType, { label: string; color: string }> = {
  LABOR: { label: '人工成本', color: 'blue' },
  TRAVEL: { label: '差旅费用', color: 'cyan' },
  AGENT: { label: '代理商费用', color: 'orange' },
  OTHER: { label: '其他费用', color: 'default' }
}

async function loadData() {
  loading.value = true
  try {
    const res = (await pageFinanceCosts({
      ...query,
      page: pagination.current,
      size: pagination.pageSize
    })) as unknown as PageResult<FinanceCost>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[finance.cost] load failed:', e)
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
  query.costType = undefined
  query.startDate = ''
  query.endDate = ''
  handleSearch()
}

const columns = [
  { title: '项目', dataIndex: 'projectName', key: 'projectName', width: 140, ellipsis: true },
  { title: '成本类型', dataIndex: 'costType', key: 'costType', width: 140 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 140 },
  { title: '发生日期', dataIndex: 'costDate', key: 'costDate', width: 120 },
  { title: '业务来源', dataIndex: 'refType', key: 'refType', width: 140 },
  { title: '说明', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const }
]

/* ============ 新增/编辑 ============ */
const modalVisible = ref(false)
const modalTitle = ref('')
const formRef = ref()
const form = reactive<FinanceCostDTO>({
  projectId: undefined,
  costType: 'LABOR' as FinanceCostType,
  amount: 0,
  costDate: '',
  refType: 'MANUAL',
  refId: undefined,
  description: ''
})
const rules = {
  projectId: [{ required: true, message: '请选择项目' }],
  costType: [{ required: true, message: '请选择成本类型' }],
  amount: [{ required: true, message: '请输入金额' }],
  costDate: [{ required: true, message: '请选择发生日期' }]
}

/* ============ 实体引用下拉选项 ============ */
const projectOptions = ref<Array<{ value: string | number; label: string }>>([])
const outsourceTaskOptions = ref<Array<{ value: string | number; label: string }>>([])

async function loadProjectOptions() {
  try {
    const res = await pageProjects({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    projectOptions.value = list.map((p: any) => ({ value: p.id, label: p.projectName }))
  } catch (e) {
    console.warn('[finance.cost] load projects failed:', e)
  }
}

// 按当前选中的项目加载转包任务，供 refId 在 refType=OUTSOURCE_TASK 时使用
async function loadOutsourceTaskOptions(projectId?: string | number) {
  if (!projectId) {
    outsourceTaskOptions.value = []
    return
  }
  try {
    const res = await pageOutsourceTasks({ page: 1, size: 200, projectId } as any)
    const list = (res as any)?.records || []
    outsourceTaskOptions.value = list.map((t: any) => ({ value: t.id, label: t.taskName }))
  } catch (e) {
    console.warn('[finance.cost] load outsource tasks failed:', e)
  }
}

// 项目变更时清空 refId，并按新项目重载转包任务选项
function handleProjectChange() {
  form.refId = undefined
  if (form.refType === 'OUTSOURCE_TASK') {
    loadOutsourceTaskOptions(form.projectId)
  }
}

// 业务来源类型变更：清空 refId，并按需重载转包任务选项
function handleRefTypeChange() {
  form.refId = undefined
  if (form.refType === 'OUTSOURCE_TASK') {
    loadOutsourceTaskOptions(form.projectId)
  } else {
    outsourceTaskOptions.value = []
  }
}

function openCreate() {
  modalTitle.value = '新建成本'
  Object.assign(form, {
    id: undefined,
    projectId: undefined,
    costType: 'LABOR',
    amount: 0,
    costDate: new Date().toISOString().substring(0, 10),
    refType: 'MANUAL',
    refId: undefined,
    description: ''
  })
  outsourceTaskOptions.value = []
  modalVisible.value = true
}

function openEdit(record: FinanceCost) {
  modalTitle.value = '编辑成本'
  // BigDecimal 经 JacksonConfig 序列化为字符串，绑定 a-input-number 前转 number
  Object.assign(form, {
    id: record.id,
    projectId: record.projectId,
    costType: record.costType,
    amount: record.amount != null ? Number(record.amount) : 0,
    costDate: record.costDate,
    refType: record.refType,
    refId: record.refId,
    description: record.description
  })
  // 编辑时若来源为转包任务，按当前项目预加载转包任务选项
  if (record.refType === 'OUTSOURCE_TASK') {
    loadOutsourceTaskOptions(record.projectId)
  } else {
    outsourceTaskOptions.value = []
  }
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
      await updateFinanceCost(form.id, form)
      message.success('更新成功')
    } else {
      await createFinanceCost(form)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch (e) {
    console.error('[finance.cost] save failed:', e)
  }
}

function handleDelete(record: FinanceCost) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除该成本记录吗？`,
    okText: '删除',
    okType: 'danger',
    async onOk() {
      try {
        await deleteFinanceCost(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        console.error('[finance.cost] delete failed:', e)
      }
    }
  })
}

onMounted(() => {
  loadData()
  // 预加载搜索表单 / 弹窗下拉选项
  loadProjectOptions()
})
</script>

<template>
  <PageContainer title="成本归集" description="人工/差旅/代理商/其他费用 → 项目维度">
    <template #extra>
      <a-button @click="handleReset">
        <template #icon><ReloadOutlined /></template>
        重置
      </a-button>
      <a-button type="primary" @click="openCreate">
        <template #icon><PlusOutlined /></template>
        新建成本
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
      <a-form-item label="成本类型">
        <a-select v-model:value="query.costType" placeholder="全部" allow-clear style="width: 140px">
          <a-select-option v-for="(v, k) in costTypeMap" :key="k" :value="k">{{ v.label }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="开始日期">
        <a-date-picker v-model:value="query.startDate" value-format="YYYY-MM-DD" style="width: 140px" />
      </a-form-item>
      <a-form-item label="结束日期">
        <a-date-picker v-model:value="query.endDate" value-format="YYYY-MM-DD" style="width: 140px" />
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
      :row-key="(record: FinanceCost) => record.id"
      @change="(p: any) => { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }"
    >
      <template #emptyText>
        <EmptyState description="暂无成本数据" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'projectName'">
          {{ record.projectName || record.projectId }}
        </template>
        <template v-else-if="column.key === 'costType'">
          <a-tag :color="costTypeMap[record.costType as FinanceCostType]?.color">
            {{ costTypeMap[record.costType as FinanceCostType]?.label }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="openEdit(record)">
            <EditOutlined /> 编辑
          </a-button>
          <a-button type="link" size="small" danger @click="handleDelete(record)">
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
                @change="handleProjectChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="成本类型" name="costType">
              <a-select v-model:value="form.costType">
                <a-select-option v-for="(v, k) in costTypeMap" :key="k" :value="k">{{ v.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="金额" name="amount">
              <a-input-number v-model:value="form.amount" :min="0" :step="100" :precision="2" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="发生日期" name="costDate">
              <a-date-picker v-model:value="form.costDate" value-format="YYYY-MM-DD" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="业务来源">
              <a-select v-model:value="form.refType" @change="handleRefTypeChange">
                <a-select-option value="MANUAL">手动录入</a-select-option>
                <a-select-option value="TIMESHEET">工时单</a-select-option>
                <a-select-option value="BUSINESS_TRIP">差旅单</a-select-option>
                <a-select-option value="OUTSOURCE_TASK">转包任务</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <!-- 业务来源为转包任务时联动加载，其它来源保留手填 -->
            <a-form-item v-if="form.refType === 'OUTSOURCE_TASK'" label="转包任务">
              <a-select
                v-model:value="form.refId"
                show-search
                allow-clear
                placeholder="选择转包任务"
                style="width: 100%"
                :options="outsourceTaskOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
              />
            </a-form-item>
            <a-form-item v-else label="关联单据">
              <a-input-number v-model:value="form.refId" placeholder="可选" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="费用说明">
          <a-textarea v-model:value="form.description" :rows="2" placeholder="费用说明" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>
