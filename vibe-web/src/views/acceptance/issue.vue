<script setup lang="ts">
/**
 * 遗留问题跟踪
 * 设计文档 2.7.4：遗留项登记 → 责任人指派 → 整改 → 闭环确认
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageAcceptanceIssues,
  createAcceptanceIssue,
  updateAcceptanceIssue,
  deleteAcceptanceIssue,
  resolveAcceptanceIssue,
  closeAcceptanceIssue
} from '@/api/acceptance'
import { pageProjects, pageTasks } from '@/api/project'
import { pageEngineers } from '@/api/resource'
import type { AcceptanceIssue, AcceptanceIssueQuery, AcceptanceIssueDTO } from '@/types/acceptance'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<AcceptanceIssue[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`
})
const query = reactive<AcceptanceIssueQuery>({ projectId: undefined, taskId: undefined, status: undefined, severity: undefined })

const statusMap: Record<string, { label: string; color: string }> = {
  OPEN: { label: '待处理', color: 'error' },
  IN_PROGRESS: { label: '整改中', color: 'processing' },
  RESOLVED: { label: '已整改', color: 'warning' },
  CLOSED: { label: '已闭环', color: 'success' }
}
const severityMap: Record<string, { label: string; color: string }> = {
  LOW: { label: '低', color: 'default' },
  MEDIUM: { label: '中', color: 'blue' },
  HIGH: { label: '高', color: 'orange' },
  CRITICAL: { label: '严重', color: 'red' }
}

async function loadData() {
  loading.value = true
  try {
    const res = (await pageAcceptanceIssues({
      ...query,
      page: pagination.current,
      size: pagination.pageSize
    })) as unknown as PageResult<AcceptanceIssue>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[acceptance.issue] load failed:', e)
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
  query.taskId = undefined
  query.status = undefined
  query.severity = undefined
  loadQueryTasks()
  handleSearch()
}

/* ============ 实体引用下拉选项 ============ */
const projectOptions = ref<Array<{ value: string | number; label: string }>>([])
const taskOptions = ref<Array<{ value: string | number; label: string }>>([])
const formTaskOptions = ref<Array<{ value: string | number; label: string }>>([])
const engineerOptions = ref<Array<{ value: string | number; label: string }>>([])

async function loadProjects() {
  try {
    const res = await pageProjects({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    projectOptions.value = list.map((p: any) => ({ value: p.id, label: p.projectName }))
  } catch (e) {
    console.warn('[project] load failed:', e)
  }
}

async function loadQueryTasks(projectId?: string | number) {
  try {
    const res = await pageTasks({ page: 1, size: 200, projectId } as any)
    const list = (res as any)?.records || []
    taskOptions.value = list.map((t: any) => ({ value: t.id, label: t.taskName }))
  } catch (e) {
    console.warn('[task] load failed:', e)
  }
}

async function loadFormTasks(projectId?: string | number) {
  try {
    const res = await pageTasks({ page: 1, size: 200, projectId } as any)
    const list = (res as any)?.records || []
    formTaskOptions.value = list.map((t: any) => ({ value: t.id, label: t.taskName }))
  } catch (e) {
    console.warn('[task] load failed:', e)
  }
}

async function loadEngineers() {
  try {
    const res = await pageEngineers({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    engineerOptions.value = list.map((en: any) => ({ value: en.id, label: en.name || en.engineerNo }))
  } catch (err) {
    console.warn('[engineer] load failed:', err)
  }
}

// 联动：项目变化时清空任务并重新加载
function handleQueryProjectChange(value: any) {
  query.taskId = undefined
  loadQueryTasks(value)
}
function handleFormProjectChange(value: any) {
  form.taskId = undefined
  loadFormTasks(value)
}

const columns = [
  { title: '问题名称', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '项目', dataIndex: 'projectName', key: 'projectName', width: 140 },
  { title: '严重等级', dataIndex: 'severity', key: 'severity', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '整改截止', dataIndex: 'dueDate', key: 'dueDate', width: 120 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' as const }
]

/* ============ 新增/编辑 ============ */
const modalVisible = ref(false)
const modalTitle = ref('')
const formRef = ref()
const form = reactive<AcceptanceIssueDTO>({
  taskId: undefined,
  projectId: undefined,
  name: '',
  description: '',
  severity: 'MEDIUM',
  assigneeId: undefined,
  dueDate: '',
  remark: ''
})
const rules = {
  taskId: [{ required: true, message: '请选择验收任务' }],
  projectId: [{ required: true, message: '请选择项目' }],
  name: [{ required: true, message: '请输入问题名称' }]
}

function openCreate() {
  modalTitle.value = '新建遗留问题'
  Object.assign(form, {
    id: undefined,
    taskId: undefined,
    projectId: undefined,
    name: '',
    description: '',
    severity: 'MEDIUM',
    assigneeId: undefined,
    dueDate: '',
    remark: ''
  })
  formTaskOptions.value = []
  modalVisible.value = true
  loadProjects()
  loadEngineers()
}

function openEdit(record: AcceptanceIssue) {
  modalTitle.value = '编辑遗留问题'
  Object.assign(form, {
    id: record.id,
    taskId: record.taskId,
    projectId: record.projectId,
    name: record.name,
    description: record.description,
    severity: record.severity,
    assigneeId: record.assigneeId,
    dueDate: record.dueDate,
    remark: record.remark
  })
  modalVisible.value = true
  loadProjects()
  loadEngineers()
  loadFormTasks(record.projectId)
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  try {
    if (form.id) {
      await updateAcceptanceIssue(form.id, form)
      message.success('更新成功')
    } else {
      await createAcceptanceIssue(form)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch (e) {
    console.error('[acceptance.issue] save failed:', e)
  }
}

function handleDelete(record: AcceptanceIssue) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除遗留问题「${record.name}」吗？`,
    okText: '删除',
    okType: 'danger',
    async onOk() {
      try {
        await deleteAcceptanceIssue(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        console.error('[acceptance.issue] delete failed:', e)
      }
    }
  })
}

function handleResolve(record: AcceptanceIssue) {
  Modal.confirm({
    title: '标记整改完成',
    content: `确定将「${record.name}」标记为整改完成吗？`,
    okText: '确认',
    async onOk() {
      try {
        await resolveAcceptanceIssue(record.id)
        message.success('已标记整改完成')
        loadData()
      } catch (e) {
        console.error('[acceptance.issue] resolve failed:', e)
      }
    }
  })
}

function handleClose(record: AcceptanceIssue) {
  Modal.confirm({
    title: '闭环确认',
    content: `确定对「${record.name}」进行闭环确认吗？`,
    okText: '确认闭环',
    async onOk() {
      try {
        await closeAcceptanceIssue(record.id)
        message.success('已闭环确认')
        loadData()
      } catch (e) {
        console.error('[acceptance.issue] close failed:', e)
      }
    }
  })
}

onMounted(() => {
  loadData()
  loadProjects()
  loadEngineers()
  loadQueryTasks()
})
</script>

<template>
  <PageContainer title="遗留问题" description="遗留项登记 → 责任人指派 → 整改 → 闭环确认">
    <template #extra>
      <a-button @click="handleReset">
        <template #icon><ReloadOutlined /></template>
        重置
      </a-button>
      <a-button type="primary" @click="openCreate">
        <template #icon><PlusOutlined /></template>
        新建问题
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
          @change="handleQueryProjectChange"
        />
      </a-form-item>
      <a-form-item label="任务">
        <a-select
          v-model:value="query.taskId"
          show-search
          allow-clear
          placeholder="选择任务"
          style="width: 180px"
          :options="taskOptions"
          :filter-option="(input: string, option: any) => option.label.includes(input)"
        />
      </a-form-item>
      <a-form-item label="状态">
        <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 140px">
          <a-select-option v-for="(v, k) in statusMap" :key="k" :value="k">{{ v.label }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="严重等级">
        <a-select v-model:value="query.severity" placeholder="全部" allow-clear style="width: 120px">
          <a-select-option v-for="(v, k) in severityMap" :key="k" :value="k">{{ v.label }}</a-select-option>
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
      :row-key="(record: AcceptanceIssue) => record.id"
      @change="(p: any) => { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }"
    >
      <template #emptyText>
        <EmptyState description="暂无遗留问题" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'projectName'">
          {{ record.projectName || record.projectId }}
        </template>
        <template v-else-if="column.key === 'severity'">
          <a-tag :color="severityMap[record.severity]?.color || 'default'">
            {{ severityMap[record.severity]?.label || record.severity }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusMap[record.status]?.color || 'default'">
            {{ statusMap[record.status]?.label || record.status }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="openEdit(record)">
            <EditOutlined /> 编辑
          </a-button>
          <a-button v-if="record.status === 'IN_PROGRESS' || record.status === 'OPEN'" type="link" size="small" @click="handleResolve(record)">
            整改完成
          </a-button>
          <a-button v-if="record.status === 'RESOLVED'" type="link" size="small" @click="handleClose(record)">
            闭环
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
            <a-form-item label="验收任务" name="taskId">
              <a-select
                v-model:value="form.taskId"
                show-search
                placeholder="选择任务"
                style="width: 100%"
                :options="formTaskOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="项目" name="projectId">
              <a-select
                v-model:value="form.projectId"
                show-search
                placeholder="选择项目"
                style="width: 100%"
                :options="projectOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
                @change="handleFormProjectChange"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="问题名称" name="name">
          <a-input v-model:value="form.name" placeholder="请输入问题名称" />
        </a-form-item>
        <a-form-item label="问题描述">
          <a-textarea v-model:value="form.description" :rows="2" placeholder="问题描述" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="严重等级">
              <a-select v-model:value="form.severity">
                <a-select-option v-for="(v, k) in severityMap" :key="k" :value="k">{{ v.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="整改截止日期">
              <a-date-picker v-model:value="form.dueDate" value-format="YYYY-MM-DD" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="整改责任人">
          <a-select
            v-model:value="form.assigneeId"
            show-search
            allow-clear
            placeholder="选择责任人"
            style="width: 100%"
            :options="engineerOptions"
            :filter-option="(input: string, option: any) => option.label.includes(input)"
          />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="form.remark" :rows="2" placeholder="备注" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>
