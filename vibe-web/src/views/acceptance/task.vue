<script setup lang="ts">
/**
 * 验收任务
 * 设计文档 2.7.2-2.7.3：测试记录 + 验收流程（申请→内部审核→客户签核→完成）
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, EyeOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  pageAcceptanceTasks,
  createAcceptanceTask,
  deleteAcceptanceTask,
  applyAcceptanceTask,
  listAcceptanceTestRecords
} from '@/api/acceptance'
import { pageProjects } from '@/api/project'
import type { AcceptanceTask, AcceptanceTaskQuery, AcceptanceTaskStatus } from '@/types/acceptance'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<AcceptanceTask[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`
})
const query = reactive<AcceptanceTaskQuery>({ projectId: undefined, name: '', status: undefined })

const statusOptions: { label: string; value: AcceptanceTaskStatus; color: string }[] = [
  { label: '草稿', value: 'DRAFT', color: 'default' },
  { label: '已申请', value: 'APPLIED', color: 'processing' },
  { label: '内部审核通过', value: 'INTERNAL_AUDITED', color: 'processing' },
  { label: '客户签核中', value: 'CUSTOMER_SIGNING', color: 'warning' },
  { label: '已完成', value: 'COMPLETED', color: 'success' },
  { label: '已驳回', value: 'REJECTED', color: 'error' }
]

function statusLabel(s: string) {
  return statusOptions.find((o) => o.value === s)?.label || s
}
function statusColor(s: string) {
  return statusOptions.find((o) => o.value === s)?.color || 'default'
}

/* ============ 实体引用下拉选项 ============ */
// 项目下拉选项（实体引用字段：projectId）
const projectOptions = ref<Array<{ value: string | number; label: string }>>([])
async function loadProjects() {
  try {
    const res = await pageProjects({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    projectOptions.value = list.map((p: any) => ({ value: p.id, label: p.projectName }))
  } catch (e) {
    console.warn('[project] load failed:', e)
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = (await pageAcceptanceTasks({
      ...query,
      page: pagination.current,
      size: pagination.pageSize
    })) as unknown as PageResult<AcceptanceTask>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[acceptance.task] load failed:', e)
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
  query.name = ''
  query.status = undefined
  handleSearch()
}

const columns = [
  { title: '任务名称', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '项目', dataIndex: 'projectName', key: 'projectName', width: 140 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 140 },
  { title: '申请时间', dataIndex: 'applyTime', key: 'applyTime', width: 180 },
  { title: '客户签核结果', dataIndex: 'customerSignResult', key: 'customerSignResult', width: 140 },
  { title: '得分', dataIndex: 'score', key: 'score', width: 80 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const }
]

/* ============ 新增弹窗 ============ */
const modalVisible = ref(false)
const formRef = ref()
const form = reactive({ projectId: undefined as number | undefined, standardId: undefined as number | undefined, name: '', remark: '' })
const rules = {
  projectId: [{ required: true, message: '请选择项目' }],
  name: [{ required: true, message: '请输入验收任务名称' }]
}

function openCreate() {
  Object.assign(form, { projectId: undefined, standardId: undefined, name: '', remark: '' })
  modalVisible.value = true
  loadProjects()
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  try {
    await createAcceptanceTask(form)
    message.success('创建成功')
    modalVisible.value = false
    loadData()
  } catch (e) {
    console.error('[acceptance.task] create failed:', e)
  }
}

/* ============ 操作 ============ */
function handleApply(record: AcceptanceTask) {
  Modal.confirm({
    title: '提交验收申请',
    content: `确定提交「${record.name}」的验收申请吗？提交后将进入内部审核。`,
    okText: '提交',
    async onOk() {
      try {
        await applyAcceptanceTask({ taskId: record.id })
        message.success('已提交验收申请')
        loadData()
      } catch (e) {
        console.error('[acceptance.task] apply failed:', e)
      }
    }
  })
}

function handleDelete(record: AcceptanceTask) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除验收任务「${record.name}」吗？（仅草稿状态可删除）`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deleteAcceptanceTask(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        console.error('[acceptance.task] delete failed:', e)
      }
    }
  })
}

/* ============ 详情/测试记录 ============ */
const detailVisible = ref(false)
const detailRecord = ref<AcceptanceTask | null>(null)
const testRecords = ref<any[]>([])

async function viewDetail(record: AcceptanceTask) {
  detailRecord.value = record
  detailVisible.value = true
  try {
    testRecords.value = (await listAcceptanceTestRecords(record.id)) as unknown as any[]
  } catch (e) {
    testRecords.value = []
  }
}

onMounted(() => {
  loadData()
  loadProjects()
})
</script>

<template>
  <PageContainer title="验收任务" description="验收流程：申请 → 内部审核 → 客户签核 → 完成">
    <template #extra>
      <a-button @click="handleReset">
        <template #icon><ReloadOutlined /></template>
        重置
      </a-button>
      <a-button type="primary" @click="openCreate">
        <template #icon><PlusOutlined /></template>
        新建任务
      </a-button>
    </template>

    <a-form layout="inline" style="margin-bottom: 16px" @submit.prevent="handleSearch">
      <a-form-item label="任务名称">
        <a-input v-model:value="query.name" placeholder="任务名称" allow-clear @pressEnter="handleSearch" />
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
        <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 160px">
          <a-select-option v-for="s in statusOptions" :key="s.value" :value="s.value">{{ s.label }}</a-select-option>
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
      :row-key="(record: AcceptanceTask) => record.id"
      @change="(p: any) => { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }"
    >
      <template #emptyText>
        <EmptyState description="暂无验收任务" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'projectName'">
          {{ record.projectName || record.projectId }}
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'customerSignResult'">
          <a-tag v-if="record.customerSignResult" :color="record.customerSignResult === 'REJECT' ? 'error' : 'success'">
            {{ record.customerSignResult }}
          </a-tag>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="viewDetail(record)">
            <EyeOutlined /> 详情
          </a-button>
          <a-button v-if="record.status === 'DRAFT'" type="link" size="small" @click="handleApply(record)">
            提交申请
          </a-button>
          <a-button v-if="record.status === 'DRAFT'" type="link" size="small" danger @click="handleDelete(record)">
            删除
          </a-button>
        </template>
      </template>
    </a-table>

    <!-- 新增弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      title="新建验收任务"
      :ok-text="'创建'"
      cancel-text="取消"
      @ok="handleSubmit"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
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
        <!-- TODO: acceptance 模块暂无验收标准列表 API，待后续补标准列表 API 后改为 select 动态加载 -->
        <a-form-item label="验收标准">
          <a-input-number v-model:value="form.standardId" placeholder="可选" style="width: 100%" />
        </a-form-item>
        <a-form-item label="任务名称" name="name">
          <a-input v-model:value="form.name" placeholder="请输入验收任务名称" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="form.remark" :rows="2" placeholder="备注" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="验收任务详情"
      width="800px"
      :footer="null"
    >
      <a-descriptions v-if="detailRecord" :column="2" bordered size="small">
        <a-descriptions-item label="任务名称">{{ detailRecord.name }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="statusColor(detailRecord.status)">{{ statusLabel(detailRecord.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="项目">{{ detailRecord.projectName || detailRecord.projectId }}</a-descriptions-item>
        <a-descriptions-item label="验收标准">{{ detailRecord.standardName || detailRecord.standardId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="申请时间">{{ detailRecord.applyTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="内部审核结果">{{ detailRecord.internalAuditResult || '-' }}</a-descriptions-item>
        <a-descriptions-item label="客户签核人">{{ detailRecord.customerSignUser || '-' }}</a-descriptions-item>
        <a-descriptions-item label="客户签核结果">{{ detailRecord.customerSignResult || '-' }}</a-descriptions-item>
        <a-descriptions-item label="得分">{{ detailRecord.score ?? '-' }}</a-descriptions-item>
        <a-descriptions-item label="备注">{{ detailRecord.remark || '-' }}</a-descriptions-item>
      </a-descriptions>

      <h3 style="margin-top: 16px; margin-bottom: 8px">测试记录</h3>
      <a-table
        :data-source="testRecords"
        :pagination="false"
        :row-key="(r: any) => r.id"
        size="small"
      >
        <a-table-column title="测试项" data-index="testName" />
        <a-table-column title="类型" data-index="testType" :width="100" />
        <a-table-column title="结果" data-index="testResult" :width="100" />
        <a-table-column title="测试值" data-index="testValue" />
        <a-table-column title="测试时间" data-index="testTime" :width="160" />
      </a-table>
    </a-modal>
  </PageContainer>
</template>
