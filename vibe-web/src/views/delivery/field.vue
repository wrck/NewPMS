<script setup lang="ts">
/**
 * 现场作业
 * 工单管理：列表、创建、详情（步骤/照片/异常）、PM 确认
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  EyeOutlined,
  CheckOutlined,
  CloseOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageWorkOrders,
  createWorkOrder,
  getWorkOrderDetail,
  listWorkOrderSteps,
  listWorkOrderPhotos,
  listWorkOrderIssues,
  confirmWorkOrder
} from '@/api/delivery'
import { pageProjects, pageTasks } from '@/api/project'
import { pageEngineers } from '@/api/resource'
import { pageAgentCompanies, listAgentEngineers } from '@/api/agent'
import type {
  WorkOrder,
  WorkOrderStep,
  WorkOrderPhoto,
  WorkOrderIssue,
  WorkOrderQueryParams,
  WorkOrderCreateDTO,
  WorkOrderConfirmDTO
} from '@/types/delivery'
import { TaskStatus, TaskStatusTone, TaskStatusLabel, Priority, PriorityLabel } from '@/types/enum'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<WorkOrder[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const query = reactive<WorkOrderQueryParams>({ projectId: undefined, engineerId: undefined, agentCompanyId: undefined, status: undefined, priority: undefined, executeMode: undefined, startBegin: '', startEnd: '' })

// 实体引用字段下拉选项
const projectOptions = ref<Array<{ value: string | number; label: string }>>([])
const engineerOptions = ref<Array<{ value: string | number; label: string }>>([])
const taskOptions = ref<Array<{ value: string | number; label: string }>>([])
const agentCompanyOptions = ref<Array<{ value: string | number; label: string }>>([])
const agentEngineerOptions = ref<Array<{ value: string | number; label: string }>>([])

// 项目下拉（搜索表单 + 创建弹窗共用）
async function loadProjects() {
  try {
    const res = await pageProjects({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    projectOptions.value = list.map((p: any) => ({ value: p.id, label: p.projectName }))
  } catch (e) {
    console.warn('[project] load failed:', e)
  }
}

// 工程师下拉（搜索表单 + 创建弹窗共用）
async function loadEngineers() {
  try {
    const res = await pageEngineers({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    engineerOptions.value = list.map((e: any) => ({ value: e.id, label: e.name }))
  } catch (e) {
    console.warn('[engineer] load failed:', e)
  }
}

// 任务下拉（按项目联动加载）
async function loadTasks(projectId?: string | number) {
  if (!projectId) {
    taskOptions.value = []
    return
  }
  try {
    const res = await pageTasks({ page: 1, size: 200, projectId } as any)
    const list = (res as any)?.records || []
    taskOptions.value = list.map((t: any) => ({ value: t.id, label: t.taskName }))
  } catch (e) {
    console.warn('[task] load failed:', e)
  }
}

// 代理商公司下拉（创建弹窗用）
async function loadAgentCompanies() {
  try {
    const res = await pageAgentCompanies({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    agentCompanyOptions.value = list.map((c: any) => ({ value: c.id, label: c.companyName }))
  } catch (e) {
    console.warn('[agent-company] load failed:', e)
  }
}

// 代理工程师下拉（按代理商公司联动加载）
async function loadAgentEngineers(companyId?: string | number) {
  if (!companyId) {
    agentEngineerOptions.value = []
    return
  }
  try {
    const list = (await listAgentEngineers(companyId as any) as any) || []
    agentEngineerOptions.value = list.map((e: any) => ({ value: e.id, label: e.name }))
  } catch (e) {
    console.warn('[agent-engineer] load failed:', e)
  }
}

// 联动：项目变化 -> 清空任务并重新加载
function onProjectIdChange(value: any) {
  formData.taskId = undefined
  loadTasks(value)
}

// 联动：代理商变化 -> 清空代理工程师并重新加载
function onAgentCompanyIdChange(value: any) {
  formData.agentEngineerId = undefined
  loadAgentEngineers(value)
}

async function loadData() {
  loading.value = true
  try {
    const res = (await pageWorkOrders({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<WorkOrder>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[delivery.field] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

const executeModeLabel: Record<string, string> = {
  SELF: '自营',
  AGENT: '代理'
}

const columns = [
  { title: '工单号', dataIndex: 'workOrderNo', key: 'workOrderNo', width: 140 },
  { title: '工单名称', dataIndex: 'workOrderName', key: 'workOrderName', ellipsis: true },
  { title: '所属项目', dataIndex: 'projectName', key: 'projectName', width: 150, ellipsis: true },
  { title: '执行方式', key: 'executeMode', width: 90 },
  { title: '执行人', dataIndex: 'engineerName', key: 'engineerName', width: 110 },
  { title: '优先级', key: 'priority', width: 90 },
  { title: '步骤进度', key: 'stepProgress', width: 140 },
  { title: '状态', key: 'status', width: 100 },
  { title: '计划开始', dataIndex: 'plannedStart', key: 'plannedStart', width: 120 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' }
]

const priorityTone: Record<Priority, any> = {
  LOW: 'default',
  MEDIUM: 'processing',
  HIGH: 'warning',
  URGENT: 'error'
}

// 创建弹窗
const formVisible = ref(false)
const formLoading = ref(false)
const formData = reactive<WorkOrderCreateDTO>({
  workOrderName: '',
  projectId: undefined as unknown as number,
  taskId: undefined,
  engineerId: undefined,
  agentCompanyId: undefined,
  agentEngineerId: undefined,
  executeMode: 'SELF',
  priority: 'MEDIUM',
  siteInfo: { siteName: '', address: '', contact: '', phone: '' },
  plannedStart: '',
  plannedEnd: '',
  description: '',
  standardSteps: []
})
const stepInput = reactive({ stepName: '', description: '', estimatedMinutes: 0 })

function openCreate() {
  Object.assign(formData, {
    workOrderName: '',
    projectId: undefined,
    taskId: undefined,
    engineerId: undefined,
    agentCompanyId: undefined,
    agentEngineerId: undefined,
    executeMode: 'SELF',
    priority: 'MEDIUM',
    siteInfo: { siteName: '', address: '', contact: '', phone: '' },
    plannedStart: '',
    plannedEnd: '',
    description: '',
    standardSteps: []
  })
  stepInput.stepName = ''
  stepInput.description = ''
  stepInput.estimatedMinutes = 0
  taskOptions.value = []
  agentEngineerOptions.value = []
  loadAgentCompanies()
  formVisible.value = true
}

function addStep() {
  if (!stepInput.stepName) return
  formData.standardSteps = [...(formData.standardSteps || []), {
    stepName: stepInput.stepName,
    description: stepInput.description || undefined,
    estimatedMinutes: stepInput.estimatedMinutes || undefined
  }]
  stepInput.stepName = ''
  stepInput.description = ''
  stepInput.estimatedMinutes = 0
}

async function handleSubmit() {
  if (!formData.workOrderName || !formData.projectId) {
    message.warning('请填写工单名称和项目')
    return
  }
  formLoading.value = true
  try {
    await createWorkOrder(formData)
    message.success('创建成功')
    formVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    formLoading.value = false
  }
}

// 详情抽屉
const drawerVisible = ref(false)
const drawerLoading = ref(false)
const currentOrder = ref<WorkOrder | null>(null)
const stepList = ref<WorkOrderStep[]>([])
const photoList = ref<WorkOrderPhoto[]>([])
const issueList = ref<WorkOrderIssue[]>([])
const drawerTab = ref('steps')

async function openDetail(row: WorkOrder) {
  drawerVisible.value = true
  drawerLoading.value = true
  drawerTab.value = 'steps'
  try {
    currentOrder.value = (await getWorkOrderDetail(row.id)) as unknown as WorkOrder
    const [steps, photos, issues] = await Promise.all([
      listWorkOrderSteps(row.id) as unknown as Promise<WorkOrderStep[]>,
      listWorkOrderPhotos(row.id) as unknown as Promise<WorkOrderPhoto[]>,
      listWorkOrderIssues(row.id) as unknown as Promise<WorkOrderIssue[]>
    ])
    stepList.value = steps || []
    photoList.value = photos || []
    issueList.value = issues || []
  } catch (e) {
    console.error('[delivery.field] load detail failed:', e)
  } finally {
    drawerLoading.value = false
  }
}

const stepStatusMap: Record<string, { tone: any; label: string }> = {
  PENDING: { tone: 'default', label: '待执行' },
  IN_PROGRESS: { tone: 'processing', label: '执行中' },
  COMPLETED: { tone: 'success', label: '已完成' },
  SKIPPED: { tone: 'archived', label: '跳过' }
}

const severityTone: Record<string, any> = {
  LOW: 'default',
  MEDIUM: 'warning',
  HIGH: 'error'
}
const severityLabel: Record<string, string> = { LOW: '低', MEDIUM: '中', HIGH: '高' }
const issueStatusMap: Record<string, { tone: any; label: string }> = {
  OPEN: { tone: 'error', label: '待处理' },
  PROCESSING: { tone: 'warning', label: '处理中' },
  RESOLVED: { tone: 'success', label: '已解决' },
  CLOSED: { tone: 'archived', label: '已关闭' }
}

const stepColumns = [
  { title: '顺序', dataIndex: 'stepOrder', key: 'stepOrder', width: 60 },
  { title: '步骤名称', dataIndex: 'stepName', key: 'stepName', ellipsis: true },
  { title: '预计(min)', dataIndex: 'estimatedMinutes', key: 'estimatedMinutes', width: 100 },
  { title: '实际(min)', dataIndex: 'actualMinutes', key: 'actualMinutes', width: 100 },
  { title: '执行人', dataIndex: 'operatorName', key: 'operatorName', width: 100 },
  { title: '状态', key: 'status', width: 100 },
  { title: '完成时间', dataIndex: 'completedAt', key: 'completedAt', width: 160 }
]

const issueColumns = [
  { title: '问题描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '严重程度', key: 'severity', width: 100 },
  { title: '上报人', dataIndex: 'reporterName', key: 'reporterName', width: 100 },
  { title: '上报时间', dataIndex: 'reportedAt', key: 'reportedAt', width: 160 },
  { title: '处理人', dataIndex: 'handlerName', key: 'handlerName', width: 100 },
  { title: '状态', key: 'status', width: 100 },
  { title: '解决方案', dataIndex: 'resolution', key: 'resolution', ellipsis: true }
]

// PM 确认弹窗
const confirmVisible = ref(false)
const confirmLoading = ref(false)
const confirmRow = ref<WorkOrder | null>(null)
const confirmForm = reactive<WorkOrderConfirmDTO>({ approved: true, remark: '', rating: 5 })

function openConfirm(row: WorkOrder) {
  confirmRow.value = row
  confirmForm.approved = true
  confirmForm.remark = ''
  confirmForm.rating = 5
  confirmVisible.value = true
}

async function handleConfirm() {
  if (!confirmRow.value) return
  if (!confirmForm.approved && !confirmForm.remark) {
    message.warning('请填写驳回原因')
    return
  }
  confirmLoading.value = true
  try {
    await confirmWorkOrder(confirmRow.value.id, confirmForm)
    message.success(confirmForm.approved ? '已确认完成' : '已驳回')
    confirmVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    confirmLoading.value = false
  }
}

function stepPercent(row: WorkOrder) {
  if (!row.stepProgress || !row.stepProgress.total) return 0
  return Math.round((row.stepProgress.completed / row.stepProgress.total) * 100)
}

onMounted(() => {
  loadProjects()
  loadEngineers()
  loadData()
})
</script>

<template>
  <PageContainer title="现场作业" description="工单创建、进度跟踪、步骤/照片/异常管理、PM 确认">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>创建工单</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
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
        <a-form-item label="执行方式">
          <a-select v-model:value="query.executeMode" placeholder="全部" allow-clear style="width: 110px">
            <a-select-option v-for="(v, k) in executeModeLabel" :key="k" :value="k">{{ v }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 120px">
            <a-select-option v-for="s in Object.values(TaskStatus)" :key="s" :value="s">{{ TaskStatusLabel[s] }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="优先级">
          <a-select v-model:value="query.priority" placeholder="全部" allow-clear style="width: 110px">
            <a-select-option v-for="p in Object.values(Priority)" :key="p" :value="p">{{ PriorityLabel[p] }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="vibe-card table-card">
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1500 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'executeMode'">
            <a-tag :color="record.executeMode === 'AGENT' ? 'purple' : 'blue'">{{ executeModeLabel[record.executeMode] }}</a-tag>
          </template>
          <template v-else-if="column.key === 'priority'">
            <StatusTag :tone="priorityTone[record.priority as Priority]">{{ PriorityLabel[record.priority as Priority] }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'stepProgress'">
            <ProgressBar :percent="stepPercent(record)" />
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="TaskStatusTone[record.status as TaskStatus]">{{ TaskStatusLabel[record.status as TaskStatus] || record.status }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a @click="openDetail(record)"><EyeOutlined /> 详情</a>
              <a v-if="record.status === 'SUBMITTED'" @click="openConfirm(record)"><CheckOutlined /> 确认</a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无工单" action-text="创建工单" @action="openCreate" /></template>
      </a-table>
    </div>

    <!-- 创建工单 -->
    <a-modal v-model:open="formVisible" title="创建工单" width="780px" :confirm-loading="formLoading" @ok="handleSubmit">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="工单名称" required>
              <a-input v-model:value="formData.workOrderName" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="项目" required>
              <a-select
                v-model:value="formData.projectId"
                placeholder="选择项目"
                show-search
                :options="projectOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
                style="width: 100%"
                @change="onProjectIdChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="任务">
              <a-select
                v-model:value="formData.taskId"
                placeholder="选择任务"
                show-search
                allow-clear
                :options="taskOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="执行方式">
              <a-select v-model:value="formData.executeMode">
                <a-select-option v-for="(v, k) in executeModeLabel" :key="k" :value="k">{{ v }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="优先级">
              <a-select v-model:value="formData.priority">
                <a-select-option v-for="p in Object.values(Priority)" :key="p" :value="p">{{ PriorityLabel[p] }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12" v-if="formData.executeMode === 'SELF'">
            <a-form-item label="工程师">
              <a-select
                v-model:value="formData.engineerId"
                placeholder="选择工程师"
                show-search
                allow-clear
                :options="engineerOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12" v-if="formData.executeMode === 'AGENT'">
            <a-form-item label="代理商">
              <a-select
                v-model:value="formData.agentCompanyId"
                placeholder="选择代理商"
                show-search
                allow-clear
                :options="agentCompanyOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
                style="width: 100%"
                @change="onAgentCompanyIdChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12" v-if="formData.executeMode === 'AGENT'">
            <a-form-item label="代理工程师">
              <a-select
                v-model:value="formData.agentEngineerId"
                placeholder="选择代理工程师"
                show-search
                allow-clear
                :options="agentEngineerOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="计划开始">
              <a-date-picker v-model:value="formData.plannedStart" value-format="YYYY-MM-DD" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="计划结束">
              <a-date-picker v-model:value="formData.plannedEnd" value-format="YYYY-MM-DD" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-divider orientation="left" plain>站点信息</a-divider>
          </a-col>
          <a-col :span="12">
            <a-form-item label="站点名称">
              <a-input v-model:value="formData.siteInfo!.siteName" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系人">
              <a-input v-model:value="formData.siteInfo!.contact" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="地址">
              <a-input v-model:value="formData.siteInfo!.address" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-divider orientation="left" plain>标准步骤</a-divider>
          </a-col>
          <a-col :span="24">
            <a-input-group compact>
              <a-input v-model:value="stepInput.stepName" placeholder="步骤名称" style="width: 30%" />
              <a-input v-model:value="stepInput.description" placeholder="说明" style="width: 40%" />
              <a-input-number v-model:value="stepInput.estimatedMinutes" placeholder="预计分钟" :min="0" style="width: 20%" />
              <a-button type="primary" style="width: 10%" @click="addStep">添加</a-button>
            </a-input-group>
            <div class="step-list">
              <a-tag v-for="(s, i) in formData.standardSteps || []" :key="i" closable color="blue" @close="formData.standardSteps?.splice(i, 1)">
                {{ i + 1 }}. {{ s.stepName }}{{ s.estimatedMinutes ? `(${s.estimatedMinutes}min)` : '' }}
              </a-tag>
            </div>
          </a-col>
          <a-col :span="24">
            <a-form-item label="工单描述">
              <a-textarea v-model:value="formData.description" :rows="2" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 工单详情 -->
    <a-drawer :open="drawerVisible" :width="900" :title="currentOrder?.workOrderName || '工单详情'" @close="drawerVisible = false">
      <a-spin :spinning="drawerLoading">
        <a-descriptions v-if="currentOrder" :column="3" size="small" bordered class="desc-block">
          <a-descriptions-item label="工单号">{{ currentOrder.workOrderNo }}</a-descriptions-item>
          <a-descriptions-item label="所属项目">{{ currentOrder.projectName }}</a-descriptions-item>
          <a-descriptions-item label="执行方式">
            <a-tag :color="currentOrder.executeMode === 'AGENT' ? 'purple' : 'blue'">{{ executeModeLabel[currentOrder.executeMode] }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="执行人">{{ currentOrder.engineerName || currentOrder.agentEngineerName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="优先级">
            <StatusTag :tone="priorityTone[currentOrder.priority as Priority]">{{ PriorityLabel[currentOrder.priority as Priority] }}</StatusTag>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <StatusTag :tone="TaskStatusTone[currentOrder.status as TaskStatus]">{{ TaskStatusLabel[currentOrder.status as TaskStatus] }}</StatusTag>
          </a-descriptions-item>
          <a-descriptions-item label="签到时间">{{ currentOrder.checkinTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="签退时间">{{ currentOrder.checkoutTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="步骤进度">
            <ProgressBar :percent="stepPercent(currentOrder)" />
          </a-descriptions-item>
        </a-descriptions>

        <a-tabs v-model:activeKey="drawerTab" class="detail-tabs">
          <a-tab-pane key="steps" tab="作业步骤">
            <a-table :columns="stepColumns" :data-source="stepList" row-key="id" size="small" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <StatusTag :tone="stepStatusMap[record.status]?.tone">{{ stepStatusMap[record.status]?.label || record.status }}</StatusTag>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="photos" :tab="`现场照片 (${photoList.length})`">
            <div class="photo-wall">
              <a-empty v-if="!photoList.length" description="暂无照片" />
              <div v-for="p in photoList" :key="p.id" class="photo-item">
                <a-image :src="p.thumbnailUrl || p.url" :preview="{ src: p.url }" />
                <div class="photo-meta">
                  <span class="text-auxiliary">{{ p.stepName || '其他' }}</span>
                  <span class="text-auxiliary">{{ p.uploadedByName }}</span>
                </div>
              </div>
            </div>
          </a-tab-pane>
          <a-tab-pane key="issues" :tab="`异常问题 (${issueList.length})`">
            <a-table :columns="issueColumns" :data-source="issueList" row-key="id" size="small" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'severity'">
                  <StatusTag :tone="severityTone[record.severity]">{{ severityLabel[record.severity] }}</StatusTag>
                </template>
                <template v-else-if="column.key === 'status'">
                  <StatusTag :tone="issueStatusMap[record.status]?.tone">{{ issueStatusMap[record.status]?.label || record.status }}</StatusTag>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
        </a-tabs>
      </a-spin>
    </a-drawer>

    <!-- PM 确认 -->
    <a-modal v-model:open="confirmVisible" :title="confirmForm.approved ? '确认工单完成' : '驳回工单'" :confirm-loading="confirmLoading" @ok="handleConfirm">
      <a-form layout="vertical">
        <a-form-item label="是否通过">
          <a-radio-group v-model:value="confirmForm.approved">
            <a-radio :value="true">确认通过</a-radio>
            <a-radio :value="false">驳回</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="confirmForm.approved" label="评分">
          <a-rate v-model:value="confirmForm.rating" />
        </a-form-item>
        <a-form-item :label="confirmForm.approved ? '确认意见' : '驳回原因'" :required="!confirmForm.approved">
          <a-textarea v-model:value="confirmForm.remark" :rows="3" :placeholder="confirmForm.approved ? '可填写确认意见' : '请说明驳回原因'" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
.desc-block { margin-bottom: 16px; }
.detail-tabs { margin-top: 8px; }
.step-list { margin-top: 8px; }
.photo-wall {
  display: flex; flex-wrap: wrap; gap: 12px;
}
.photo-item {
  width: 140px;
  .photo-meta {
    display: flex; justify-content: space-between;
    font-size: 12px; margin-top: 4px;
  }
}
</style>
