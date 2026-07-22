<script setup lang="ts">
/**
 * 项目列表页（设计文档 3.3.2）
 * - 顶部：搜索表单（关键字、状态、优先级、产品线、执行模式）+ 操作按钮（新建项目、看板视图）
 * - 中部：表格（项目编号/名称/客户/PM/状态/进度/计划周期/优先级/操作）
 * - 底部：分页
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  AppstoreOutlined,
  TableOutlined,
  ReloadOutlined,
  ExportOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import HelpHint from '@/components/HelpHint.vue'
import StatusTag from '@/components/StatusTag.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageProjects,
  createProject,
  updateProject,
  deleteProject,
  exportProjects,
  pageTemplates,
  pageCustomers
} from '@/api/project'
import { pageUsers } from '@/api/system'
import type { Project, ProjectQueryParams, ProjectSaveDTO } from '@/types/project'
import { ProjectStatus, ProjectStatusTone, ProjectStatusLabel, Priority, PriorityLabel } from '@/types/enum'
import type { PageResult } from '@/types/api'

const router = useRouter()

// ============ 列表数据 ============
const loading = ref(false)
const dataSource = ref<Project[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const query = reactive<ProjectQueryParams>({
  keyword: '',
  status: undefined,
  priority: undefined,
  productLine: undefined,
  executeMode: undefined,
  page: 1,
  size: 10
})

const viewMode = ref<'table' | 'kanban'>('table')

// 状态/优先级/产品线/执行模式选项
const statusOptions = Object.values(ProjectStatus).map((s) => ({
  value: s,
  label: ProjectStatusLabel[s]
}))
const priorityOptions = Object.values(Priority).map((p) => ({ value: p, label: PriorityLabel[p] }))
const productLineOptions = [
  { value: 'ROUTER', label: '路由' },
  { value: 'SWITCH', label: '交换' },
  { value: 'WIRELESS', label: '无线' },
  { value: 'SECURITY', label: '安全' },
  { value: 'DC', label: '数据中心' },
  { value: 'OTHER', label: '其他' }
]
const executeModeOptions = [
  { value: 'SELF', label: '自施' },
  { value: 'AGENT', label: '代施' },
  { value: 'MIXED', label: '混合' }
]

async function loadData() {
  loading.value = true
  try {
    query.page = pagination.current
    query.size = pagination.pageSize
    const res = (await pageProjects(query)) as unknown as PageResult<Project>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[project.list] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  query.keyword = ''
  query.status = undefined
  query.priority = undefined
  query.productLine = undefined
  query.executeMode = undefined
  pagination.current = 1
  loadData()
}

const exportLoading = ref(false)

async function handleExport() {
  exportLoading.value = true
  try {
    await exportProjects(query)
    message.success('导出成功')
  } catch (e) {
    // 错误提示已在拦截器处理
  } finally {
    exportLoading.value = false
  }
}

function handleTableChange(p: any) {
  pagination.current = p.current
  pagination.pageSize = p.pageSize
  loadData()
}

function goDetail(id: string | number) {
  // 字符串透传，避免雪花 Long 在 JS number 下丢精度
  router.push(`/project/detail/${id}`)
}

// ============ 新建/编辑弹窗 ============
const formVisible = ref(false)
const formLoading = ref(false)
const formRef = ref()
const isEdit = ref(false)
const formData = reactive<ProjectSaveDTO>({
  projectName: '',
  customerId: 0,
  projectType: 'NEW',
  productLine: 'ROUTER',
  executeMode: 'SELF',
  priority: 'MEDIUM' as Priority,
  pmId: 0,
  region: '',
  contractNo: '',
  plannedStart: '',
  plannedEnd: '',
  description: '',
  templateId: undefined
})

const templateOptions = ref<Array<{ value: string | number; label: string }>>([])

async function loadTemplates() {
  try {
    const res = await pageTemplates({ keyword: '' })
    const list = (res as any)?.records || []
    templateOptions.value = list.map((t: any) => ({ value: t.id, label: t.templateName }))
  } catch (e) {
    console.warn('[template] load failed:', e)
  }
}

// 客户下拉选项（实体引用字段：customerId）
const customerOptions = ref<Array<{ value: string | number; label: string }>>([])
async function loadCustomers() {
  try {
    const res = await pageCustomers({ page: 1, size: 200 })
    const list = (res as any)?.records || []
    customerOptions.value = list.map((c: any) => ({ value: c.id, label: c.customerName }))
  } catch (e) {
    console.warn('[customer] load failed:', e)
  }
}

// 项目经理下拉选项（实体引用字段：pmId，按 PM 角色筛 sys_user）
const pmOptions = ref<Array<{ value: string | number; label: string }>>([])
async function loadPms() {
  try {
    const res = await pageUsers({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    pmOptions.value = list.map((u: any) => ({ value: u.id, label: u.realName || u.userName }))
  } catch (e) {
    console.warn('[pm] load failed:', e)
  }
}

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    projectName: '',
    customerId: undefined,
    projectType: 'NEW',
    productLine: 'ROUTER',
    executeMode: 'SELF',
    priority: 'MEDIUM',
    pmId: undefined,
    region: '',
    contractNo: '',
    plannedStart: '',
    plannedEnd: '',
    description: '',
    templateId: undefined
  } as ProjectSaveDTO)
  formVisible.value = true
  loadTemplates()
  loadCustomers()
  loadPms()
}

function openEdit(row: Project) {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    projectName: row.projectName,
    customerId: row.customerId,
    projectType: row.projectType,
    productLine: row.productLine,
    executeMode: row.executeMode,
    priority: row.priority,
    pmId: row.pmId,
    region: row.region,
    contractNo: row.contractNo,
    plannedStart: row.plannedStart,
    plannedEnd: row.plannedEnd,
    description: row.description,
    templateId: undefined
  } as ProjectSaveDTO)
  formVisible.value = true
  loadCustomers()
  loadPms()
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateProject(formData.id, formData)
      message.success('项目已更新')
    } else {
      const id = await createProject(formData)
      message.success(`项目已创建，编号：${id}`)
    }
    formVisible.value = false
    loadData()
  } catch (e) {
    // 错误提示已在拦截器处理
  } finally {
    formLoading.value = false
  }
}

function handleDelete(row: Project) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除项目「${row.projectName}」吗？仅 INIT/PLAN 状态可删除。`,
    okText: '确定',
    cancelText: '取消',
    okType: 'danger',
    async onOk() {
      try {
        await deleteProject(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        // ignore
      }
    }
  })
}

// ============ 表格列 ============
const columns = [
  { title: '项目编号', dataIndex: 'projectCode', key: 'projectCode', width: 140, fixed: 'left' },
  { title: '项目名称', dataIndex: 'projectName', key: 'projectName', ellipsis: true },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 140, ellipsis: true },
  { title: 'PM', dataIndex: 'pmName', key: 'pmName', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 110 },
  { title: '进度', dataIndex: 'progressPct', key: 'progressPct', width: 160 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 80 },
  { title: '计划周期', key: 'plannedRange', width: 200 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' }
]

function rangeText(row: Project) {
  const s = row.plannedStart || '-'
  const e = row.plannedEnd || '-'
  return `${s} ~ ${e}`
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="项目列表" description="管理所有交付项目的全生命周期">
    <template #title-suffix>
      <HelpHint
        title="项目列表"
        content="管理项目全生命周期：\n1. 点击「新建项目」创建项目（必填：项目名、客户、PM）；\n2. 可选项目模板自动生成阶段与任务；\n3. 支持按状态/优先级/产品线/执行模式筛选；\n4. 点击项目名进入详情页规划阶段与任务。"
      />
    </template>
    <template #extra>
      <a-button @click="loadData">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
      <a-radio-group v-model:value="viewMode" button-style="solid" size="small">
        <a-radio-button value="table"><TableOutlined /></a-radio-button>
        <a-radio-button value="kanban"><AppstoreOutlined /></a-radio-button>
      </a-radio-group>
      <a-button :loading="exportLoading" @click="handleExport">
        <template #icon><ExportOutlined /></template>
        导出
      </a-button>
      <a-button type="primary" @click="openCreate">
        <template #icon><PlusOutlined /></template>
        新建项目
      </a-button>
    </template>

    <!-- 搜索表单 -->
    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="关键字">
          <a-input
            v-model:value="query.keyword"
            placeholder="项目编号 / 名称 / 合同号"
            allow-clear
            style="width: 220px"
            @pressEnter="handleSearch"
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-select
            v-model:value="query.status"
            placeholder="全部状态"
            allow-clear
            style="width: 140px"
            :options="statusOptions"
          />
        </a-form-item>
        <a-form-item label="优先级">
          <a-select
            v-model:value="query.priority"
            placeholder="全部"
            allow-clear
            style="width: 110px"
            :options="priorityOptions"
          />
        </a-form-item>
        <a-form-item label="产品线">
          <a-select
            v-model:value="query.productLine"
            placeholder="全部"
            allow-clear
            style="width: 130px"
            :options="productLineOptions"
          />
        </a-form-item>
        <a-form-item label="执行模式">
          <a-select
            v-model:value="query.executeMode"
            placeholder="全部"
            allow-clear
            style="width: 110px"
            :options="executeModeOptions"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </div>

    <!-- 表格视图 -->
    <div v-if="viewMode === 'table'" class="vibe-card table-card">
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1280 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'projectName'">
            <a class="project-link" @click="goDetail(record.id)">{{ record.projectName }}</a>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="ProjectStatusTone[record.status as ProjectStatus]">
              {{ ProjectStatusLabel[record.status as ProjectStatus] }}
            </StatusTag>
          </template>
          <template v-else-if="column.key === 'progressPct'">
            <ProgressBar :percent="record.progressPct || 0" />
          </template>
          <template v-else-if="column.key === 'priority'">
            <a-tag :color="record.priority === 'URGENT' ? 'red' : record.priority === 'HIGH' ? 'orange' : 'default'">
              {{ PriorityLabel[record.priority as Priority] }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'plannedRange'">
            <span class="text-auxiliary">{{ rangeText(record) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a @click="goDetail(record.id)">详情</a>
              <a-divider type="vertical" />
              <a @click="openEdit(record)">编辑</a>
              <a-divider type="vertical" />
              <a class="danger-link" @click="handleDelete(record)">删除</a>
            </a-space>
          </template>
        </template>

        <template #emptyText>
          <EmptyState description="暂无项目数据" action-text="新建项目" @action="openCreate" />
        </template>
      </a-table>
    </div>

    <!-- 看板视图（占位，后续接入 kanban 接口） -->
    <div v-else class="vibe-card kanban-card">
      <EmptyState description="看板视图开发中" />
    </div>

    <!-- 新建/编辑弹窗 -->
    <a-modal
      v-model:open="formVisible"
      :title="isEdit ? '编辑项目' : '新建项目'"
      width="680px"
      :confirm-loading="formLoading"
      :mask-closable="false"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleSubmit"
    >
      <a-form
        ref="formRef"
        :model="formData"
        layout="vertical"
        :rules="{
          projectName: [{ required: true, message: '请输入项目名称' }],
          customerId: [{ required: true, message: '请选择客户' }],
          pmId: [{ required: true, message: '请选择项目经理' }]
        }"
      >
        <a-row :gutter="16">
          <a-col :span="14">
            <a-form-item label="项目名称" name="projectName">
              <a-input v-model:value="formData.projectName" placeholder="请输入项目名称" />
            </a-form-item>
          </a-col>
          <a-col :span="10">
            <a-form-item label="客户" name="customerId">
              <a-select
                v-model:value="formData.customerId"
                show-search
                :options="customerOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
                placeholder="选择客户"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="项目类型">
              <a-select
                v-model:value="formData.projectType"
                :options="[
                  { value: 'NEW', label: '新建' },
                  { value: 'EXPAND', label: '扩容' },
                  { value: 'REFORM', label: '改造' },
                  { value: 'REPLACE', label: '替换' },
                  { value: 'SECURITY', label: '安全' }
                ]"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="产品线">
              <a-select v-model:value="formData.productLine" :options="productLineOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="执行模式">
              <a-select v-model:value="formData.executeMode" :options="executeModeOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="优先级">
              <a-select v-model:value="formData.priority" :options="priorityOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="项目经理" name="pmId">
              <a-select
                v-model:value="formData.pmId"
                show-search
                :options="pmOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
                placeholder="选择项目经理"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="区域">
              <a-input v-model:value="formData.region" placeholder="如：华北" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="合同编号">
              <a-input v-model:value="formData.contractNo" placeholder="合同编号" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="计划开始">
              <a-date-picker v-model:value="formData.plannedStart" style="width: 100%" value-format="YYYY-MM-DD" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="计划结束">
              <a-date-picker v-model:value="formData.plannedEnd" style="width: 100%" value-format="YYYY-MM-DD" />
            </a-form-item>
          </a-col>
          <a-col v-if="!isEdit" :span="24">
            <a-form-item label="项目模板">
              <a-select
                v-model:value="formData.templateId"
                placeholder="不使用模板"
                allow-clear
                :options="templateOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="项目描述">
              <a-textarea v-model:value="formData.description" :rows="3" placeholder="项目描述" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card {
  padding: 16px 20px 0;
  margin-bottom: 16px;
}
.table-card {
  padding: 0;
}
.project-link {
  font-weight: 500;
}
.danger-link {
  color: @status-exception;
}
.kanban-card {
  padding: 24px;
}
</style>
