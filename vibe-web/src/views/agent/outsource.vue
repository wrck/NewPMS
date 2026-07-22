<script setup lang="ts">
/**
 * 转包任务
 * 代理商转包任务全流程：创建、接单/拒绝、指派工程师、查看交付物
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  CheckOutlined,
  CloseOutlined,
  UserOutlined,
  EyeOutlined,
  PaperClipOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageOutsourceTasks,
  createOutsourceTask,
  respondOutsourceTask,
  assignAgentEngineer,
  listAgentEngineers,
  listDeliverables,
  pageAgentCompanies
} from '@/api/agent'
import { pageProjects, pageTasks } from '@/api/project'
import type {
  OutsourceTask,
  OutsourceTaskDTO,
  OutsourceTaskQueryParams,
  OutsourceDeliverable,
  AgentEngineer
} from '@/types/agent'
import { OutsourceStatus, OutsourceStatusTone, OutsourceStatusLabel } from '@/types/enum'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<OutsourceTask[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const query = reactive<OutsourceTaskQueryParams>({ projectId: undefined, agentCompanyId: undefined, status: undefined, startBegin: '', startEnd: '' })

async function loadData() {
  loading.value = true
  try {
    const res = (await pageOutsourceTasks({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<OutsourceTask>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[agent.outsource] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

const columns = [
  { title: '任务编码', dataIndex: 'taskCode', key: 'taskCode', width: 140 },
  { title: '所属项目', dataIndex: 'projectName', key: 'projectName', ellipsis: true },
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName', width: 160, ellipsis: true },
  { title: '代理商', dataIndex: 'agentCompanyName', key: 'agentCompanyName', width: 150, ellipsis: true },
  { title: '执行工程师', dataIndex: 'agentEngineerName', key: 'agentEngineerName', width: 110 },
  { title: '截止日期', dataIndex: 'deadline', key: 'deadline', width: 120 },
  { title: '提交次数', dataIndex: 'submitCount', key: 'submitCount', width: 90 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
]

// ============ 实体引用下拉选项 ============
const projectOptions = ref<Array<{ value: string | number; label: string }>>([])
const agentCompanyOptions = ref<Array<{ value: string | number; label: string }>>([])
const taskOptions = ref<Array<{ value: string | number; label: string }>>([])

async function loadProjectOptions() {
  try {
    const res = await pageProjects({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    projectOptions.value = list.map((p: any) => ({ value: p.id, label: p.projectName }))
  } catch (e) {
    console.warn('[agent.outsource] load projects failed:', e)
  }
}

async function loadAgentCompanyOptions() {
  try {
    const res = await pageAgentCompanies({ page: 1, size: 200 })
    const list = (res as any)?.records || []
    agentCompanyOptions.value = list.map((c: any) => ({ value: c.id, label: c.companyName }))
  } catch (e) {
    console.warn('[agent.outsource] load agent companies failed:', e)
  }
}

async function loadTaskOptions(projectId?: string | number) {
  if (!projectId) {
    taskOptions.value = []
    return
  }
  try {
    const res = await pageTasks({ projectId, page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    taskOptions.value = list.map((t: any) => ({ value: t.id, label: t.taskName }))
  } catch (e) {
    console.warn('[agent.outsource] load tasks failed:', e)
  }
}

// 项目变更联动：清空任务并重新加载任务选项
function handleProjectChange(value?: string | number) {
  formData.taskId = undefined
  taskOptions.value = []
  loadTaskOptions(value)
}

// 创建弹窗
const formVisible = ref(false)
const formLoading = ref(false)
const formData = reactive<OutsourceTaskDTO>({
  projectId: 0,
  taskId: undefined,
  agentCompanyId: 0,
  taskScope: '',
  deadline: '',
  attachments: []
})

function openCreate() {
  Object.assign(formData, {
    projectId: 0,
    taskId: undefined,
    agentCompanyId: 0,
    taskScope: '',
    deadline: '',
    attachments: []
  })
  taskOptions.value = []
  formVisible.value = true
  loadProjectOptions()
  loadAgentCompanyOptions()
}

async function handleSubmit() {
  if (!formData.projectId || !formData.agentCompanyId || !formData.taskScope) {
    message.warning('请完整填写项目、代理商和任务范围')
    return
  }
  formLoading.value = true
  try {
    await createOutsourceTask(formData)
    message.success('创建成功')
    formVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    formLoading.value = false
  }
}

// 接单/拒绝
const respondVisible = ref(false)
const respondLoading = ref(false)
const respondRow = ref<OutsourceTask | null>(null)
const respondForm = reactive({ accepted: true, remark: '' })

function openRespond(row: OutsourceTask, accepted: boolean) {
  respondRow.value = row
  respondForm.accepted = accepted
  respondForm.remark = ''
  respondVisible.value = true
}

async function handleRespond() {
  if (!respondRow.value) return
  respondLoading.value = true
  try {
    await respondOutsourceTask(respondRow.value.id, respondForm.accepted, respondForm.remark)
    message.success(respondForm.accepted ? '已接单' : '已拒绝')
    respondVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    respondLoading.value = false
  }
}

// 指派工程师
const assignVisible = ref(false)
const assignLoading = ref(false)
const assignRow = ref<OutsourceTask | null>(null)
const engineerList = ref<AgentEngineer[]>([])
const engineerLoading = ref(false)
const assignEngineerId = ref<number | undefined>(undefined)

async function openAssign(row: OutsourceTask) {
  assignRow.value = row
  assignEngineerId.value = row.agentEngineerId
  assignVisible.value = true
  engineerLoading.value = true
  try {
    engineerList.value = (await listAgentEngineers(row.agentCompanyId)) as unknown as AgentEngineer[]
  } catch (e) {
    console.error('[agent.outsource] load engineers failed:', e)
  } finally {
    engineerLoading.value = false
  }
}

async function handleAssign() {
  if (!assignRow.value || !assignEngineerId.value) {
    message.warning('请选择工程师')
    return
  }
  assignLoading.value = true
  try {
    await assignAgentEngineer(assignRow.value.id, assignEngineerId.value)
    message.success('已指派工程师')
    assignVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    assignLoading.value = false
  }
}

// 交付物查看
const deliverableVisible = ref(false)
const deliverableList = ref<OutsourceDeliverable[]>([])
const deliverableLoading = ref(false)
const deliverableRow = ref<OutsourceTask | null>(null)

async function openDeliverables(row: OutsourceTask) {
  deliverableRow.value = row
  deliverableVisible.value = true
  deliverableLoading.value = true
  try {
    deliverableList.value = (await listDeliverables(row.id)) as unknown as OutsourceDeliverable[]
  } catch (e) {
    console.error('[agent.outsource] load deliverables failed:', e)
  } finally {
    deliverableLoading.value = false
  }
}

const deliverableTypeLabel: Record<string, string> = {
  PHOTO: '现场照片',
  TEST_RECORD: '测试记录',
  SIGN_OFF: '签收单',
  CONFIG: '配置文件',
  OTHER: '其他'
}

const deliverableColumns = [
  { title: '类型', key: 'deliverableType', width: 110 },
  { title: '名称', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '上传人', dataIndex: 'uploadedByName', key: 'uploadedByName', width: 110 },
  { title: '上传时间', dataIndex: 'uploadedAt', key: 'uploadedAt', width: 160 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 90 }
]

onMounted(() => {
  loadData()
  loadProjectOptions()
  loadAgentCompanyOptions()
})
</script>

<template>
  <PageContainer title="转包任务" description="转包任务创建、接单、指派、交付物管理">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新建转包任务</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
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
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 130px">
            <a-select-option v-for="s in Object.values(OutsourceStatus)" :key="s" :value="s">{{ OutsourceStatusLabel[s] }}</a-select-option>
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
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1400 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <StatusTag :tone="OutsourceStatusTone[record.status as OutsourceStatus]">{{ OutsourceStatusLabel[record.status as OutsourceStatus] || record.status }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'submitCount'">
            <span class="tnum">{{ record.submitCount || 0 }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a v-if="record.status === 'PENDING'" @click="openRespond(record, true)"><CheckOutlined /> 接单</a>
              <a v-if="record.status === 'PENDING'" class="danger-link" @click="openRespond(record, false)"><CloseOutlined /> 拒绝</a>
              <a v-if="['ACCEPTED', 'IN_PROGRESS'].includes(record.status)" @click="openAssign(record)"><UserOutlined /> 指派</a>
              <a @click="openDeliverables(record)"><PaperClipOutlined /> 交付物</a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无转包任务" action-text="新建转包任务" @action="openCreate" /></template>
      </a-table>
    </div>

    <!-- 新建转包任务 -->
    <a-modal v-model:open="formVisible" title="新建转包任务" width="600px" :confirm-loading="formLoading" @ok="handleSubmit">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="项目" required>
              <a-select
                v-model:value="formData.projectId"
                show-search
                allow-clear
                placeholder="选择项目"
                style="width: 100%"
                :options="projectOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
                @change="handleProjectChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="任务">
              <a-select
                v-model:value="formData.taskId"
                show-search
                allow-clear
                placeholder="请先选择项目"
                style="width: 100%"
                :options="taskOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
                :disabled="!formData.projectId"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="代理商公司" required>
              <a-select
                v-model:value="formData.agentCompanyId"
                show-search
                allow-clear
                placeholder="选择代理商公司"
                style="width: 100%"
                :options="agentCompanyOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="截止日期">
              <a-date-picker v-model:value="formData.deadline" value-format="YYYY-MM-DD" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="任务范围" required>
              <a-textarea v-model:value="formData.taskScope" :rows="3" placeholder="说明转包任务的工作范围、交付要求等" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 接单/拒绝 -->
    <a-modal v-model:open="respondVisible" :title="respondForm.accepted ? '接单确认' : '拒绝任务'" :confirm-loading="respondLoading" @ok="handleRespond">
      <a-alert v-if="!respondForm.accepted" message="拒绝后任务将退回给 PM 重新分配" type="warning" show-icon style="margin-bottom: 12px" />
      <a-form layout="vertical">
        <a-form-item :label="respondForm.accepted ? '接单备注' : '拒绝原因'">
          <a-textarea v-model:value="respondForm.remark" :rows="3" :placeholder="respondForm.accepted ? '可填写接单说明' : '请说明拒绝原因'" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 指派工程师 -->
    <a-modal v-model:open="assignVisible" title="指派工程师" :confirm-loading="assignLoading" @ok="handleAssign">
      <a-spin :spinning="engineerLoading">
        <a-form layout="vertical">
          <a-form-item label="选择工程师" required>
            <a-select v-model:value="assignEngineerId" placeholder="请选择" style="width: 100%">
              <a-select-option v-for="e in engineerList" :key="e.id" :value="e.id">{{ e.name }}（{{ e.phone }}）</a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>

    <!-- 交付物 -->
    <a-modal v-model:open="deliverableVisible" :title="`交付物 - ${deliverableRow?.taskName || ''}`" :footer="null" width="780px">
      <a-table :columns="deliverableColumns" :data-source="deliverableList" :loading="deliverableLoading" row-key="id" size="small" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'deliverableType'">
            <a-tag>{{ deliverableTypeLabel[record.deliverableType] || record.deliverableType }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a :href="record.url" target="_blank"><EyeOutlined /> 查看</a>
          </template>
        </template>
      </a-table>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
.danger-link { color: @status-exception; }
</style>
