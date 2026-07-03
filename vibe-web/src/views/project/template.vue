<script setup lang="ts">
/**
 * 项目模板管理
 * 提供模板列表 + 新建/编辑弹窗（含阶段、任务定义）
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageTemplates,
  createTemplate,
  updateTemplate,
  deleteTemplate,
  getTemplateDetail
} from '@/api/project'
import type { ProjectTemplate } from '@/types/project'

const loading = ref(false)
const dataSource = ref<ProjectTemplate[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`
})
const query = reactive({ keyword: '' })

async function loadData() {
  loading.value = true
  try {
    const res: any = await pageTemplates({ keyword: query.keyword })
    dataSource.value = res?.records || []
    pagination.total = res?.total || 0
  } catch (e) {
    console.error('[template] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

// ============ 新建/编辑弹窗 ============
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<Partial<ProjectTemplate>>({
  templateName: '',
  projectType: 'NEW',
  productLine: 'ROUTER',
  description: '',
  phases: [],
  tasks: [],
  status: 'ENABLED'
})

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    templateName: '',
    projectType: 'NEW',
    productLine: 'ROUTER',
    description: '',
    phases: [{ phaseCode: 'SURVEY', phaseName: '勘察', sortOrder: 1 }],
    tasks: [],
    status: 'ENABLED'
  })
  formVisible.value = true
}

async function openEdit(row: ProjectTemplate) {
  isEdit.value = true
  try {
    const detail = await getTemplateDetail(row.id)
    Object.assign(formData, detail)
  } catch (e) {
    Object.assign(formData, row)
  }
  formVisible.value = true
}

async function handleSubmit() {
  if (!formData.templateName?.trim()) {
    message.warning('请输入模板名称')
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateTemplate(formData.id, formData)
      message.success('模板已更新')
    } else {
      await createTemplate(formData)
      message.success('模板已创建')
    }
    formVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    formLoading.value = false
  }
}

function handleDelete(row: ProjectTemplate) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除模板「${row.templateName}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteTemplate(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        // ignore
      }
    }
  })
}

function addPhase() {
  if (!formData.phases) formData.phases = []
  formData.phases.push({ phaseCode: 'INSTALL', phaseName: '', sortOrder: formData.phases.length + 1 })
}

function removePhase(idx: number) {
  formData.phases?.splice(idx, 1)
}

function addTask() {
  if (!formData.tasks) formData.tasks = []
  formData.tasks.push({ taskName: '', taskType: 'INSTALL' })
}

function removeTask(idx: number) {
  formData.tasks?.splice(idx, 1)
}

const columns = [
  { title: '模板名称', dataIndex: 'templateName', key: 'templateName', ellipsis: true },
  { title: '项目类型', dataIndex: 'projectType', key: 'projectType', width: 100 },
  { title: '产品线', dataIndex: 'productLine', key: 'productLine', width: 100 },
  { title: '阶段数', key: 'phaseCount', width: 80 },
  { title: '任务数', key: 'taskCount', width: 80 },
  { title: '状态', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' }
]

const phaseCodeOptions = [
  { value: 'SURVEY', label: '勘察' },
  { value: 'DESIGN', label: '设计' },
  { value: 'DELIVER', label: '到货' },
  { value: 'INSTALL', label: '安装' },
  { value: 'DEBUG', label: '调试' },
  { value: 'ACCEPT', label: '验收' }
]
const taskTypeOptions = [
  { value: 'SURVEY', label: '勘察' },
  { value: 'INSTALL', label: '安装' },
  { value: 'DEBUG', label: '调试' },
  { value: 'CUTOVER', label: '割接' },
  { value: 'ACCEPT', label: '验收' },
  { value: 'OTHER', label: '其他' }
]

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="项目模板" description="项目模板库：复用阶段、任务、里程碑定义">
    <template #extra>
      <a-button @click="loadData">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
      <a-button type="primary" @click="openCreate">
        <template #icon><PlusOutlined /></template>
        新建模板
      </a-button>
    </template>

    <div class="vibe-card search-card">
      <a-input
        v-model:value="query.keyword"
        placeholder="模板名称"
        allow-clear
        style="width: 260px"
        @pressEnter="handleSearch"
      />
      <a-button type="primary" style="margin-left: 8px" @click="handleSearch">查询</a-button>
    </div>

    <div class="vibe-card table-card">
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'phaseCount'">{{ record.phases?.length || 0 }}</template>
          <template v-else-if="column.key === 'taskCount'">{{ record.tasks?.length || 0 }}</template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="record.status === 'ENABLED' ? 'success' : 'archived'">
              {{ record.status === 'ENABLED' ? '启用' : '停用' }}
            </StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a @click="openEdit(record)"><EditOutlined /> 编辑</a>
              <a-divider type="vertical" />
              <a class="danger-link" @click="handleDelete(record)"><DeleteOutlined /> 删除</a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无模板" action-text="新建模板" @action="openCreate" /></template>
      </a-table>
    </div>

    <a-modal
      v-model:open="formVisible"
      :title="isEdit ? '编辑模板' : '新建模板'"
      width="780px"
      :confirm-loading="formLoading"
      :mask-closable="false"
      @ok="handleSubmit"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="模板名称" required>
              <a-input v-model:value="formData.templateName" placeholder="如：标准网络交付模板" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
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
          <a-col :span="6">
            <a-form-item label="产品线">
              <a-select
                v-model:value="formData.productLine"
                :options="[
                  { value: 'ROUTER', label: '路由' },
                  { value: 'SWITCH', label: '交换' },
                  { value: 'WIRELESS', label: '无线' },
                  { value: 'SECURITY', label: '安全' },
                  { value: 'DC', label: '数据中心' },
                  { value: 'OTHER', label: '其他' }
                ]"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述">
          <a-textarea v-model:value="formData.description" :rows="2" />
        </a-form-item>

        <div class="sub-section">
          <div class="sub-title">
            <span>阶段定义</span>
            <a-button size="small" type="link" @click="addPhase">+ 添加阶段</a-button>
          </div>
          <div v-for="(phase, idx) in formData.phases" :key="idx" class="sub-row">
            <a-select v-model:value="phase.phaseCode" :options="phaseCodeOptions" style="width: 120px" placeholder="阶段编码" />
            <a-input v-model:value="phase.phaseName" placeholder="阶段名称" style="flex: 1" />
            <a-input-number v-model:value="phase.sortOrder" placeholder="排序" style="width: 90px" />
            <a-button type="link" danger @click="removePhase(idx)">删除</a-button>
          </div>
        </div>

        <div class="sub-section">
          <div class="sub-title">
            <span>任务定义</span>
            <a-button size="small" type="link" @click="addTask">+ 添加任务</a-button>
          </div>
          <div v-for="(task, idx) in formData.tasks" :key="idx" class="sub-row">
            <a-input v-model:value="task.taskName" placeholder="任务名称" style="flex: 1" />
            <a-select v-model:value="task.taskType" :options="taskTypeOptions" style="width: 120px" placeholder="类型" />
            <a-input-number v-model:value="task.plannedDays" placeholder="计划天数" style="width: 110px" />
            <a-button type="link" danger @click="removeTask(idx)">删除</a-button>
          </div>
        </div>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card {
  padding: 16px 20px;
  margin-bottom: 16px;
}
.table-card {
  padding: 0;
}
.danger-link {
  color: @status-exception;
}
.sub-section {
  margin-top: 12px;
  padding: 12px;
  background: @bg-stripe;
  border-radius: 6px;
}
.sub-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-weight: 600;
}
.sub-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
</style>
