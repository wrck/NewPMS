<script setup lang="ts">
/**
 * 项目模板管理
 * 提供模板列表 + 新建/编辑弹窗（含阶段、任务定义）
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined,
  ApartmentOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageTemplates,
  createTemplate,
  updateTemplate,
  deleteTemplate,
  getTemplateDetail,
  addTemplatePhase,
  updateTemplatePhase,
  deleteTemplatePhase,
  addTemplateTask,
  updateTemplateTask,
  deleteTemplateTask
} from '@/api/project'
import type { ProjectTemplate, ProjectTemplatePhase, ProjectTemplateTask } from '@/types/project'

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
  formData.phases.push({ id: 0, phaseCode: 'INSTALL', phaseName: '', sortOrder: formData.phases.length + 1 } as ProjectTemplatePhase)
}

function removePhase(idx: number) {
  formData.phases?.splice(idx, 1)
}

function addTask() {
  if (!formData.tasks) formData.tasks = []
  formData.tasks.push({ id: 0, phaseCode: 'INSTALL', taskName: '', taskType: 'INSTALL', defaultDays: 1 } as ProjectTemplateTask)
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
  { title: '操作', key: 'action', width: 240, fixed: 'right' }
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

/* ============ 阶段/任务树形子管理抽屉 ============ */
const ptDrawerVisible = ref(false)
const ptLoading = ref(false)
const currentTemplate = ref<ProjectTemplate | null>(null)
const phaseList = ref<ProjectTemplatePhase[]>([])
const taskList = ref<ProjectTemplateTask[]>([])

/** 树节点数据：阶段为父节点，任务按 phaseCode 挂到对应阶段下 */
interface TreeNode {
  key: string
  title: string
  nodeType: 'phase' | 'task'
  raw: ProjectTemplatePhase | ProjectTemplateTask
  children?: TreeNode[]
}
const treeData = ref<TreeNode[]>([])

function buildTree() {
  const nodes: TreeNode[] = phaseList.value.map((p) => {
    const childTasks = taskList.value.filter((t) => t.phaseCode === p.phaseCode)
    return {
      key: `phase-${p.id}`,
      title: `${p.phaseName}（${p.phaseCode}）`,
      nodeType: 'phase' as const,
      raw: p,
      children: childTasks.map((t) => ({
        key: `task-${t.id}`,
        title: `${t.taskName}${t.defaultDays ? ` · ${t.defaultDays}天` : ''}`,
        nodeType: 'task' as const,
        raw: t
      }))
    }
  })
  treeData.value = nodes
}

async function openPhaseTaskDrawer(row: ProjectTemplate) {
  currentTemplate.value = row
  ptDrawerVisible.value = true
  await loadPhaseTaskData(row.id)
}

async function loadPhaseTaskData(templateId: number) {
  ptLoading.value = true
  try {
    const detail = (await getTemplateDetail(templateId)) as unknown as ProjectTemplate
    phaseList.value = detail.phases || []
    taskList.value = detail.tasks || []
    buildTree()
  } catch (e) {
    console.error('[template] load phases/tasks failed:', e)
    phaseList.value = []
    taskList.value = []
    treeData.value = []
  } finally {
    ptLoading.value = false
  }
}

// 阶段弹窗
const phaseModalVisible = ref(false)
const phaseModalTitle = ref('')
const phaseFormLoading = ref(false)
const phaseIsEdit = ref(false)
const phaseForm = reactive<Partial<ProjectTemplatePhase>>({
  phaseCode: '',
  phaseName: '',
  sortOrder: 1,
  deliverables: ''
})

function openPhaseCreate() {
  if (!currentTemplate.value) return
  phaseIsEdit.value = false
  phaseModalTitle.value = '新增阶段'
  Object.assign(phaseForm, {
    id: undefined,
    templateId: currentTemplate.value.id,
    phaseCode: '',
    phaseName: '',
    sortOrder: phaseList.value.length + 1,
    deliverables: ''
  })
  phaseModalVisible.value = true
}

function openPhaseEdit(node: TreeNode) {
  const p = node.raw as ProjectTemplatePhase
  phaseIsEdit.value = true
  phaseModalTitle.value = '编辑阶段'
  Object.assign(phaseForm, {
    id: p.id,
    templateId: p.templateId,
    phaseCode: p.phaseCode,
    phaseName: p.phaseName,
    sortOrder: p.sortOrder,
    deliverables: p.deliverables
  })
  phaseModalVisible.value = true
}

async function handlePhaseSubmit() {
  if (!phaseForm.phaseCode?.trim() || !phaseForm.phaseName?.trim()) {
    message.warning('请填写阶段编码和名称')
    return
  }
  if (!currentTemplate.value) return
  phaseFormLoading.value = true
  try {
    if (phaseIsEdit.value && phaseForm.id) {
      await updateTemplatePhase(phaseForm.id, phaseForm)
      message.success('阶段已更新')
    } else {
      await addTemplatePhase(currentTemplate.value.id, phaseForm)
      message.success('阶段已添加')
    }
    phaseModalVisible.value = false
    await loadPhaseTaskData(currentTemplate.value.id)
  } catch (e) {
    console.error('[template] phase submit failed:', e)
  } finally {
    phaseFormLoading.value = false
  }
}

function handlePhaseDelete(node: TreeNode) {
  const p = node.raw as ProjectTemplatePhase
  Modal.confirm({
    title: '确认删除阶段',
    content: `确定删除阶段「${p.phaseName}」吗？该阶段下的任务不会自动删除，请先处理任务。`,
    okType: 'danger',
    async onOk() {
      if (!p.id || !currentTemplate.value) return
      try {
        await deleteTemplatePhase(p.id)
        message.success('删除成功')
        await loadPhaseTaskData(currentTemplate.value.id)
      } catch (e) {
        console.error('[template] phase delete failed:', e)
      }
    }
  })
}

// 任务弹窗
const taskModalVisible = ref(false)
const taskModalTitle = ref('')
const taskFormLoading = ref(false)
const taskIsEdit = ref(false)
const taskForm = reactive<Partial<ProjectTemplateTask>>({
  phaseCode: '',
  taskName: '',
  taskType: 'INSTALL',
  description: '',
  defaultDays: 1
})

function openTaskCreate(presetPhaseCode?: string) {
  if (!currentTemplate.value) return
  taskIsEdit.value = false
  taskModalTitle.value = '新增任务'
  Object.assign(taskForm, {
    id: undefined,
    templateId: currentTemplate.value.id,
    phaseCode: presetPhaseCode || phaseList.value[0]?.phaseCode || '',
    taskName: '',
    taskType: 'INSTALL',
    description: '',
    defaultDays: 1
  })
  taskModalVisible.value = true
}

function openTaskEdit(node: TreeNode) {
  const t = node.raw as ProjectTemplateTask
  taskIsEdit.value = true
  taskModalTitle.value = '编辑任务'
  Object.assign(taskForm, {
    id: t.id,
    templateId: t.templateId,
    phaseCode: t.phaseCode,
    taskName: t.taskName,
    taskType: t.taskType || 'OTHER',
    description: t.description,
    defaultDays: t.defaultDays
  })
  taskModalVisible.value = true
}

async function handleTaskSubmit() {
  if (!taskForm.taskName?.trim()) {
    message.warning('请填写任务名称')
    return
  }
  if (!taskForm.phaseCode) {
    message.warning('请选择所属阶段')
    return
  }
  if (!currentTemplate.value) return
  taskFormLoading.value = true
  try {
    if (taskIsEdit.value && taskForm.id) {
      await updateTemplateTask(taskForm.id, taskForm)
      message.success('任务已更新')
    } else {
      await addTemplateTask(currentTemplate.value.id, taskForm)
      message.success('任务已添加')
    }
    taskModalVisible.value = false
    await loadPhaseTaskData(currentTemplate.value.id)
  } catch (e) {
    console.error('[template] task submit failed:', e)
  } finally {
    taskFormLoading.value = false
  }
}

function handleTaskDelete(node: TreeNode) {
  const t = node.raw as ProjectTemplateTask
  Modal.confirm({
    title: '确认删除任务',
    content: `确定删除任务「${t.taskName}」吗？`,
    okType: 'danger',
    async onOk() {
      if (!t.id || !currentTemplate.value) return
      try {
        await deleteTemplateTask(t.id)
        message.success('删除成功')
        await loadPhaseTaskData(currentTemplate.value.id)
      } catch (e) {
        console.error('[template] task delete failed:', e)
      }
    }
  })
}

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
              <a @click="openPhaseTaskDrawer(record)"><ApartmentOutlined /> 阶段/任务</a>
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
            <a-input-number v-model:value="task.defaultDays" placeholder="默认工期" style="width: 110px" />
            <a-button type="link" danger @click="removeTask(idx)">删除</a-button>
          </div>
        </div>
      </a-form>
    </a-modal>

    <!-- 阶段/任务树形管理抽屉 -->
    <a-drawer
      :open="ptDrawerVisible"
      :width="640"
      :title="`阶段/任务管理 - ${currentTemplate?.templateName || ''}`"
      @close="ptDrawerVisible = false"
    >
      <a-spin :spinning="ptLoading">
        <div class="drawer-toolbar">
          <a-space>
            <a-button type="primary" size="small" @click="openPhaseCreate"><PlusOutlined /> 添加阶段</a-button>
            <a-button size="small" @click="openTaskCreate()"><PlusOutlined /> 添加任务</a-button>
          </a-space>
        </div>
        <a-empty v-if="!treeData.length" description="暂无阶段与任务" />
        <a-tree
          v-else
          :tree-data="treeData"
          :default-expand-all="true"
          :show-line="true"
        >
          <template #title="node">
            <span class="tree-node">
              <span class="tree-node-label">{{ node.title }}</span>
              <span class="tree-node-actions" @click.stop>
                <template v-if="node.nodeType === 'phase'">
                  <a class="tree-link" @click="openTaskCreate(node.raw.phaseCode)">+ 任务</a>
                  <a class="tree-link" @click="openPhaseEdit(node)">编辑</a>
                  <a class="tree-link danger-link" @click="handlePhaseDelete(node)">删除</a>
                </template>
                <template v-else>
                  <a class="tree-link" @click="openTaskEdit(node)">编辑</a>
                  <a class="tree-link danger-link" @click="handleTaskDelete(node)">删除</a>
                </template>
              </span>
            </span>
          </template>
        </a-tree>
      </a-spin>
    </a-drawer>

    <!-- 阶段弹窗 -->
    <a-modal
      v-model:open="phaseModalVisible"
      :title="phaseModalTitle"
      width="520px"
      :confirm-loading="phaseFormLoading"
      @ok="handlePhaseSubmit"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="阶段编码" required>
              <a-select v-model:value="phaseForm.phaseCode" :options="phaseCodeOptions" placeholder="选择阶段编码" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="阶段名称" required>
              <a-input v-model:value="phaseForm.phaseName" placeholder="如 现场勘察" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="排序">
              <a-input-number v-model:value="phaseForm.sortOrder" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="交付物清单（JSON）">
              <a-textarea
                v-model:value="phaseForm.deliverables"
                :rows="2"
                placeholder='如 ["拓扑图","设备清单"]'
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 任务弹窗 -->
    <a-modal
      v-model:open="taskModalVisible"
      :title="taskModalTitle"
      width="520px"
      :confirm-loading="taskFormLoading"
      @ok="handleTaskSubmit"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="所属阶段" required>
              <a-select v-model:value="taskForm.phaseCode" placeholder="选择阶段">
                <a-select-option v-for="p in phaseList" :key="p.phaseCode" :value="p.phaseCode">
                  {{ p.phaseName }}（{{ p.phaseCode }}）
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="任务名称" required>
              <a-input v-model:value="taskForm.taskName" placeholder="如 设备安装" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="任务类型">
              <a-select v-model:value="taskForm.taskType" :options="taskTypeOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="默认工期（天）">
              <a-input-number v-model:value="taskForm.defaultDays" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="任务描述">
              <a-textarea v-model:value="taskForm.description" :rows="2" />
            </a-form-item>
          </a-col>
        </a-row>
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
.drawer-toolbar {
  margin-bottom: 12px;
}
.tree-node {
  display: inline-flex;
  align-items: center;
  width: 100%;
}
.tree-node-label {
  flex: 1;
}
.tree-node-actions {
  display: inline-flex;
  gap: 8px;
  margin-left: 12px;
}
.tree-link {
  font-size: 12px;
}
</style>
