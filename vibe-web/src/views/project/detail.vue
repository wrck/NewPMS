<script setup lang="ts">
/**
 * 项目详情页（设计文档 3.3.3）
 * 标签页：基本信息 / 阶段任务 / 里程碑 / 风险 / 问题 / 变更 / 成员 / 评论
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  ExportOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import FormModal from '@/components/FormModal/index.vue'
import type { FormField } from '@/components/FormModal/types'
import {
  getProjectDetail,
  listPhases,
  listTasksByProject,
  listMilestones,
  listRisks,
  listIssues,
  listChanges,
  listMembers,
  listComments,
  addComment,
  transitionProjectStatus,
  createTask,
  exportProjects,
  checkProjectClose,
  archiveProject,
  savePhase,
  deletePhase,
  saveMilestone,
  deleteMilestone,
  saveRisk,
  deleteRisk,
  saveIssue,
  deleteIssue,
  addMember,
  removeMember
} from '@/api/project'
import { pageEngineers } from '@/api/resource'
import { pageUsers } from '@/api/system'
import type { ProjectDetail, ProjectPhase, ProjectTask, Milestone, ProjectRisk, ProjectIssue, ProjectChange, ProjectMember, ProjectComment, TaskType } from '@/types/project'
import {
  ProjectStatus,
  ProjectStatusTone,
  ProjectStatusLabel,
  TaskStatus,
  TaskStatusTone,
  TaskStatusLabel,
  Priority,
  PriorityLabel
} from '@/types/enum'

const route = useRoute()
const router = useRouter()
// 直接透传字符串 id，避免雪花 Long 经 Number() 转换丢精度
const projectId = computed(() => route.params.id as string)

const loading = ref(false)
const detail = ref<ProjectDetail | null>(null)
const activeTab = ref<'info' | 'phases' | 'tasks' | 'milestones' | 'risks' | 'issues' | 'changes' | 'members' | 'comments'>('info')

// 子资源
const phases = ref<ProjectPhase[]>([])
const tasks = ref<ProjectTask[]>([])
const milestones = ref<Milestone[]>([])
const risks = ref<ProjectRisk[]>([])
const issues = ref<ProjectIssue[]>([])
const changes = ref<ProjectChange[]>([])
const members = ref<ProjectMember[]>([])
const comments = ref<ProjectComment[]>([])

const commentText = ref('')
const commentSubmitting = ref(false)

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getProjectDetail(projectId.value)
  } catch (e) {
    console.error('[project.detail] load failed:', e)
  } finally {
    loading.value = false
  }
}

async function loadTabData(tab: typeof activeTab.value) {
  try {
    switch (tab) {
      case 'phases':
        phases.value = (await listPhases(projectId.value)) || []
        break
      case 'tasks':
        tasks.value = (await listTasksByProject(projectId.value)) || []
        break
      case 'milestones':
        milestones.value = (await listMilestones(projectId.value)) || []
        break
      case 'risks':
        risks.value = (await listRisks(projectId.value)) || []
        break
      case 'issues':
        issues.value = (await listIssues(projectId.value)) || []
        break
      case 'changes':
        changes.value = (await listChanges(projectId.value)) || []
        break
      case 'members':
        members.value = (await listMembers(projectId.value)) || []
        break
      case 'comments':
        comments.value = (await listComments(projectId.value)) || []
        break
    }
  } catch (e) {
    console.error('[project.detail] load tab data failed:', e)
  }
}

function handleTabChange(tab: string) {
  activeTab.value = tab as typeof activeTab.value
  loadTabData(activeTab.value)
}

// ============ 新增任务 FormModal ============
/** 新增任务弹窗显隐 */
const taskModalVisible = ref(false)
/** 新增任务提交 loading */
const taskSubmitting = ref(false)
/** 新增任务表单数据 */
const taskFormData = reactive<Record<string, any>>({
  taskName: '',
  taskType: 'OTHER',
  executeMode: 'SELF',
  priority: Priority.MEDIUM,
  assigneeId: undefined,
  plannedStart: '',
  plannedEnd: '',
  description: ''
})

/** 任务类型选项 */
const taskTypeOptions = [
  { label: '勘察', value: 'SURVEY' as TaskType },
  { label: '安装', value: 'INSTALL' as TaskType },
  { label: '调试', value: 'DEBUG' as TaskType },
  { label: '割接', value: 'CUTOVER' as TaskType },
  { label: '验收', value: 'ACCEPT' as TaskType },
  { label: '其他', value: 'OTHER' as TaskType }
]

/** 执行模式选项 */
const executeModeOptions = [
  { label: '自施', value: 'SELF' },
  { label: '代施', value: 'AGENT' }
]

/** 优先级选项 */
const priorityOptions = [
  { label: PriorityLabel[Priority.LOW], value: Priority.LOW },
  { label: PriorityLabel[Priority.MEDIUM], value: Priority.MEDIUM },
  { label: PriorityLabel[Priority.HIGH], value: Priority.HIGH },
  { label: PriorityLabel[Priority.URGENT], value: Priority.URGENT }
]

/** 工程师下拉选项（实体引用字段：assigneeId） */
const engineerOptions = ref<Array<{ value: string | number; label: string }>>([])
async function loadEngineerOptions() {
  try {
    const res = await pageEngineers({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    engineerOptions.value = list.map((e: any) => ({ value: e.id, label: e.name }))
  } catch (e) {
    console.warn('[engineer] load failed:', e)
  }
}

/** 新增任务表单字段定义（assigneeId 改为 select + 动态加载工程师列表） */
const taskFormFields = computed<FormField[]>(() => [
  { field: 'taskName', label: '任务名称', type: 'input', required: true, placeholder: '请输入任务名称', maxLength: 100, span: 24 },
  { field: 'taskType', label: '任务类型', type: 'select', required: true, options: taskTypeOptions, span: 12 },
  { field: 'executeMode', label: '执行模式', type: 'select', required: true, options: executeModeOptions, span: 12 },
  { field: 'priority', label: '优先级', type: 'select', required: true, options: priorityOptions, span: 12 },
  { field: 'assigneeId', label: '指派给', type: 'select', options: engineerOptions.value, placeholder: '选择执行人（可留空后派单）', span: 12 },
  { field: 'plannedStart', label: '计划开始', type: 'date', valueFormat: 'YYYY-MM-DD', span: 12 },
  { field: 'plannedEnd', label: '计划结束', type: 'date', valueFormat: 'YYYY-MM-DD', span: 12 },
  { field: 'description', label: '描述', type: 'textarea', placeholder: '任务描述 / 交付要求等', maxLength: 500, span: 24 }
])

/** 打开新增任务弹窗 */
function handleOpenTaskModal() {
  // 重置表单数据为默认值
  Object.assign(taskFormData, {
    taskName: '',
    taskType: 'OTHER',
    executeMode: 'SELF',
    priority: Priority.MEDIUM,
    assigneeId: undefined,
    plannedStart: '',
    plannedEnd: '',
    description: ''
  })
  taskModalVisible.value = true
  // 加载工程师下拉选项（assigneeId 实体引用字段）
  loadEngineerOptions()
}

/** 新增任务提交 */
async function handleTaskSubmit(payload: Record<string, any>) {
  taskSubmitting.value = true
  try {
    await createTask(projectId.value, payload)
    message.success('任务新增成功')
    taskModalVisible.value = false
    // 刷新任务列表
    tasks.value = (await listTasksByProject(projectId.value)) || []
    // 同步刷新项目详情（更新 taskStats）
    loadDetail()
  } catch (e: any) {
    console.error('[project.detail] createTask failed:', e)
    message.error(e?.message || '任务新增失败')
  } finally {
    taskSubmitting.value = false
  }
}

// ============ 状态流转 ============
function nextStatus(cur: ProjectStatus): ProjectStatus | null {
  const order: ProjectStatus[] = [ProjectStatus.INIT, ProjectStatus.PLAN, ProjectStatus.EXECUTE, ProjectStatus.ACCEPT, ProjectStatus.CLOSE]
  const idx = order.indexOf(cur)
  if (idx >= 0 && idx < order.length - 1) return order[idx + 1]
  return null
}

function handleTransition() {
  if (!detail.value) return
  const next = nextStatus(detail.value.status)
  if (!next) {
    message.info('当前状态无下一步流转')
    return
  }
  Modal.confirm({
    title: '状态流转确认',
    content: `确认将项目状态从「${ProjectStatusLabel[detail.value.status]}」流转为「${ProjectStatusLabel[next]}」？`,
    okText: '确认流转',
    cancelText: '取消',
    async onOk() {
      try {
        await transitionProjectStatus(projectId.value, {
          targetStatus: next,
          version: detail.value?.version
        })
        message.success('状态流转成功')
        loadDetail()
      } catch (e) {
        // ignore
      }
    }
  })
}

// ============ 评论 ============
async function submitComment() {
  if (!commentText.value.trim()) {
    message.warning('请输入评论内容')
    return
  }
  commentSubmitting.value = true
  try {
    await addComment(projectId.value, commentText.value)
    message.success('评论已发布')
    commentText.value = ''
    comments.value = await listComments(projectId.value)
  } catch (e) {
    // ignore
  } finally {
    commentSubmitting.value = false
  }
}

function goTask(taskId: number | string) {
  router.push(`/project/task/${taskId}`)
}

// ============ 导出 ============
const exportLoading = ref(false)

async function handleExport() {
  exportLoading.value = true
  try {
    await exportProjects()
    message.success('导出成功')
  } catch (e) {
    // 错误提示已在拦截器处理
  } finally {
    exportLoading.value = false
  }
}

// ============ 归档（CLOSE → ARCHIVED） ============
const archiveVisible = ref(false)
const archiveForm = reactive({ reviewSummary: '', lessonsLearned: '' })
const archiveLoading = ref(false)

async function handleArchiveCheck() {
  try {
    const reason = await checkProjectClose(projectId.value)
    if (reason) {
      message.warning(`结项检查未通过：${reason}`)
      return
    }
    archiveForm.reviewSummary = ''
    archiveForm.lessonsLearned = ''
    archiveVisible.value = true
  } catch (e) {
    // 错误提示已在拦截器处理
  }
}

async function handleArchiveSubmit() {
  archiveLoading.value = true
  try {
    await archiveProject(projectId.value, {
      reviewSummary: archiveForm.reviewSummary || undefined,
      lessonsLearned: archiveForm.lessonsLearned || undefined
    })
    message.success('归档成功')
    archiveVisible.value = false
    loadDetail()
  } catch (e) {
    // ignore
  } finally {
    archiveLoading.value = false
  }
}

// ============ 表格列 ============
const taskColumns = [
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName', ellipsis: true },
  { title: '所属阶段', dataIndex: 'phaseName', key: 'phaseName', width: 120 },
  { title: '执行人', dataIndex: 'assigneeName', key: 'assigneeName', width: 110 },
  { title: '执行模式', key: 'executeMode', width: 90 },
  { title: '状态', key: 'status', width: 100 },
  { title: '进度', key: 'progressPct', width: 140 },
  { title: '优先级', key: 'priority', width: 80 },
  { title: '计划周期', key: 'plannedRange', width: 200 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]

const phaseColumns = [
  { title: '阶段', dataIndex: 'phaseName', key: 'phaseName', width: 140 },
  { title: '状态', key: 'status', width: 120 },
  { title: '进度', key: 'progressPct', width: 160 },
  { title: '计划开始', dataIndex: 'plannedStart', key: 'plannedStart', width: 130 },
  { title: '计划结束', dataIndex: 'plannedEnd', key: 'plannedEnd', width: 130 },
  { title: '实际开始', dataIndex: 'actualStart', key: 'actualStart', width: 130 },
  { title: '实际结束', dataIndex: 'actualEnd', key: 'actualEnd', width: 130 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]

// ============ 阶段 CRUD ============
const phaseModalVisible = ref(false)
const phaseForm = reactive<Partial<ProjectPhase>>({})
const phaseLoading = ref(false)
const phaseIsEdit = ref(false)

const phaseCodeOptions = [
  { label: '勘察', value: 'SURVEY' },
  { label: '设计', value: 'DESIGN' },
  { label: '交付', value: 'DELIVER' },
  { label: '安装', value: 'INSTALL' },
  { label: '调试', value: 'DEBUG' },
  { label: '验收', value: 'ACCEPT' }
]

const phaseStatusOptions = [
  { label: '未开始', value: 'NOT_STARTED' },
  { label: '进行中', value: 'IN_PROGRESS' },
  { label: '已完成', value: 'COMPLETED' }
]

function openPhaseCreate() {
  phaseIsEdit.value = false
  Object.assign(phaseForm, {
    phaseCode: undefined,
    phaseName: '',
    sortOrder: 0,
    status: 'NOT_STARTED',
    plannedStart: undefined,
    plannedEnd: undefined,
    actualStart: undefined,
    actualEnd: undefined,
    deliverables: '' as any
  })
  phaseModalVisible.value = true
}

function openPhaseEdit(record: ProjectPhase) {
  phaseIsEdit.value = true
  Object.assign(phaseForm, record, { deliverables: (record as any).deliverables ?? '' })
  phaseModalVisible.value = true
}

async function handlePhaseSubmit() {
  if (!phaseForm.phaseCode || !phaseForm.phaseName) {
    message.warning('请填写阶段编码和名称')
    return
  }
  phaseLoading.value = true
  try {
    await savePhase(projectId.value, phaseForm)
    message.success(phaseIsEdit.value ? '阶段已更新' : '阶段已创建')
    phaseModalVisible.value = false
    phases.value = await listPhases(projectId.value)
  } catch (e) {
    // ignore
  } finally {
    phaseLoading.value = false
  }
}

function handlePhaseDelete(record: ProjectPhase) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除阶段「${record.phaseName}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deletePhase(projectId.value, record.id)
        message.success('删除成功')
        phases.value = await listPhases(projectId.value)
      } catch (e) {
        // ignore
      }
    }
  })
}

// ============ 里程碑 CRUD ============
const milestoneModalVisible = ref(false)
const milestoneForm = reactive<Partial<Milestone>>({})
const milestoneLoading = ref(false)
const milestoneIsEdit = ref(false)

const milestoneStatusOptions = [
  { label: '待达成', value: 'PENDING' },
  { label: '已达成', value: 'ACHIEVED' },
  { label: '已超期', value: 'OVERDUE' }
]

function openMilestoneCreate() {
  milestoneIsEdit.value = false
  Object.assign(milestoneForm, {
    milestoneName: '',
    plannedDate: undefined,
    actualDate: undefined,
    deliverables: '',
    status: 'PENDING'
  })
  milestoneModalVisible.value = true
}

function openMilestoneEdit(record: Milestone) {
  milestoneIsEdit.value = true
  Object.assign(milestoneForm, record)
  milestoneModalVisible.value = true
}

async function handleMilestoneSubmit() {
  if (!milestoneForm.milestoneName) {
    message.warning('请填写里程碑名称')
    return
  }
  milestoneLoading.value = true
  try {
    await saveMilestone(projectId.value, milestoneForm)
    message.success(milestoneIsEdit.value ? '里程碑已更新' : '里程碑已创建')
    milestoneModalVisible.value = false
    milestones.value = await listMilestones(projectId.value)
  } catch (e) {
    // ignore
  } finally {
    milestoneLoading.value = false
  }
}

function handleMilestoneDelete(record: Milestone) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除里程碑「${record.milestoneName}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteMilestone(projectId.value, record.id)
        message.success('删除成功')
        milestones.value = await listMilestones(projectId.value)
      } catch (e) {
        // ignore
      }
    }
  })
}

// ============ 风险 CRUD ============
// 注：后端字段为 measure，前端类型 ProjectRisk 沿用 response，这里以交叉类型补齐 measure
type RiskForm = Partial<ProjectRisk> & { measure?: string }
const riskModalVisible = ref(false)
const riskForm = reactive<RiskForm>({})
const riskLoading = ref(false)
const riskIsEdit = ref(false)

const riskImpactOptions = [
  { label: '低', value: 'LOW' },
  { label: '中', value: 'MEDIUM' },
  { label: '高', value: 'HIGH' }
]

const riskStatusOptions = [
  { label: '待处理', value: 'OPEN' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已关闭', value: 'CLOSED' }
]

function openRiskCreate() {
  riskIsEdit.value = false
  Object.assign(riskForm, {
    riskDesc: '',
    impact: 'MEDIUM',
    probability: 'MEDIUM',
    measure: '' as any,
    ownerId: undefined,
    status: 'OPEN',
    dueDate: undefined
  })
  riskModalVisible.value = true
  loadEngineerOptions()
}

function openRiskEdit(record: ProjectRisk) {
  riskIsEdit.value = true
  Object.assign(riskForm, record, { measure: (record as any).measure ?? (record as any).response ?? '' })
  riskModalVisible.value = true
  loadEngineerOptions()
}

async function handleRiskSubmit() {
  if (!riskForm.riskDesc) {
    message.warning('请填写风险描述')
    return
  }
  riskLoading.value = true
  try {
    await saveRisk(projectId.value, riskForm)
    message.success(riskIsEdit.value ? '风险已更新' : '风险已创建')
    riskModalVisible.value = false
    risks.value = await listRisks(projectId.value)
  } catch (e) {
    // ignore
  } finally {
    riskLoading.value = false
  }
}

function handleRiskDelete(record: ProjectRisk) {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该风险记录吗？',
    okType: 'danger',
    async onOk() {
      try {
        await deleteRisk(projectId.value, record.id)
        message.success('删除成功')
        risks.value = await listRisks(projectId.value)
      } catch (e) {
        // ignore
      }
    }
  })
}

// ============ 问题 CRUD ============
const issueModalVisible = ref(false)
const issueForm = reactive<Partial<ProjectIssue>>({})
const issueLoading = ref(false)
const issueIsEdit = ref(false)

const issueStatusOptions = [
  { label: '待处理', value: 'OPEN' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已解决', value: 'RESOLVED' },
  { label: '已关闭', value: 'CLOSED' }
]

function openIssueCreate() {
  issueIsEdit.value = false
  Object.assign(issueForm, {
    issueDesc: '',
    impact: '',
    ownerId: undefined,
    status: 'OPEN',
    dueDate: undefined
  })
  issueModalVisible.value = true
  loadEngineerOptions()
}

function openIssueEdit(record: ProjectIssue) {
  issueIsEdit.value = true
  Object.assign(issueForm, record)
  issueModalVisible.value = true
  loadEngineerOptions()
}

async function handleIssueSubmit() {
  if (!issueForm.issueDesc) {
    message.warning('请填写问题描述')
    return
  }
  issueLoading.value = true
  try {
    await saveIssue(projectId.value, issueForm)
    message.success(issueIsEdit.value ? '问题已更新' : '问题已创建')
    issueModalVisible.value = false
    issues.value = await listIssues(projectId.value)
  } catch (e) {
    // ignore
  } finally {
    issueLoading.value = false
  }
}

function handleIssueDelete(record: ProjectIssue) {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该问题记录吗？',
    okType: 'danger',
    async onOk() {
      try {
        await deleteIssue(projectId.value, record.id)
        message.success('删除成功')
        issues.value = await listIssues(projectId.value)
      } catch (e) {
        // ignore
      }
    }
  })
}

// ============ 成员 CRUD ============
const memberModalVisible = ref(false)
const memberForm = reactive<{ userId: string | number | undefined; role: string }>({
  userId: undefined,
  role: 'ENGINEER'
})
const memberLoading = ref(false)

const userOptions = ref<Array<{ value: string | number; label: string }>>([])

async function loadUserOptions() {
  try {
    const res = await pageUsers({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    userOptions.value = list.map((u: any) => ({ value: u.id, label: u.realName || u.userName }))
  } catch (e) {
    console.warn('[user] load failed:', e)
  }
}

const memberRoleOptions = [
  { label: '项目经理', value: 'PM' },
  { label: '工程师', value: 'ENGINEER' },
  { label: '技术负责人', value: 'TECH_LEAD' },
  { label: '测试', value: 'QA' },
  { label: '观察者', value: 'VIEWER' }
]

function openMemberAdd() {
  memberForm.userId = undefined
  memberForm.role = 'ENGINEER'
  memberModalVisible.value = true
  loadUserOptions()
}

async function handleMemberSubmit() {
  if (!memberForm.userId) {
    message.warning('请选择用户')
    return
  }
  memberLoading.value = true
  try {
    await addMember(projectId.value, { userId: memberForm.userId, role: memberForm.role })
    message.success('成员已添加')
    memberModalVisible.value = false
    members.value = await listMembers(projectId.value)
  } catch (e) {
    // ignore
  } finally {
    memberLoading.value = false
  }
}

function handleMemberRemove(record: ProjectMember) {
  Modal.confirm({
    title: '确认移除',
    content: `确定要移除成员「${record.realName || record.userName}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await removeMember(projectId.value, record.id)
        message.success('移除成功')
        members.value = await listMembers(projectId.value)
      } catch (e) {
        // ignore
      }
    }
  })
}

const phaseStatusMap: Record<string, { tone: any; label: string }> = {
  NOT_STARTED: { tone: 'default', label: '未开始' },
  IN_PROGRESS: { tone: 'processing', label: '进行中' },
  COMPLETED: { tone: 'success', label: '已完成' }
}

onMounted(() => {
  loadDetail()
  loadTabData('phases')
})
</script>

<template>
  <PageContainer :show-back="true" @back="router.back()">
    <template #header>
      <div class="detail-header">
        <h2 class="vibe-page-title">
          {{ detail?.projectName || '项目详情' }}
          <span class="project-code">{{ detail?.projectCode }}</span>
        </h2>
        <p class="detail-meta" v-if="detail">
          PM：{{ detail.pmName || '-' }} · 客户：{{ detail.customerName || detail.customerId }} ·
          区域：{{ detail.region || '-' }} · 优先级：{{ PriorityLabel[detail.priority as Priority] }}
        </p>
      </div>
    </template>
    <template #extra>
      <a-button @click="loadDetail">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
      <a-button :loading="exportLoading" @click="handleExport">
        <template #icon><ExportOutlined /></template>
        导出
      </a-button>
      <a-button v-if="detail && detail.status === ProjectStatus.CLOSE" type="primary" @click="handleArchiveCheck">
        归档
      </a-button>
      <a-button v-if="detail && nextStatus(detail.status)" type="primary" @click="handleTransition">
        流转至「{{ ProjectStatusLabel[nextStatus(detail.status) as ProjectStatus] }}」
      </a-button>
    </template>

    <a-spin :spinning="loading">
      <!-- 概览卡片 -->
      <a-row v-if="detail" :gutter="16" class="overview-row">
        <a-col :xs="12" :md="6">
          <div class="vibe-card overview-card">
            <div class="overview-label">当前状态</div>
            <div class="overview-value">
              <StatusTag :tone="ProjectStatusTone[detail.status as ProjectStatus]">
                {{ ProjectStatusLabel[detail.status as ProjectStatus] }}
              </StatusTag>
            </div>
          </div>
        </a-col>
        <a-col :xs="12" :md="6">
          <div class="vibe-card overview-card">
            <div class="overview-label">总进度</div>
            <div class="overview-value">
              <ProgressBar :percent="detail.progressPct || 0" size="large" />
            </div>
          </div>
        </a-col>
        <a-col :xs="12" :md="6">
          <div class="vibe-card overview-card">
            <div class="overview-label">任务统计</div>
            <div class="overview-value text-statistic">
              {{ detail.taskStats?.completed || 0 }} / {{ detail.taskStats?.total || 0 }}
              <span class="overview-sub">已完成</span>
            </div>
          </div>
        </a-col>
        <a-col :xs="12" :md="6">
          <div class="vibe-card overview-card">
            <div class="overview-label">风险 / 问题</div>
            <div class="overview-value text-statistic">
              <span class="text-exception">{{ detail.riskCount || 0 }}</span> /
              <span class="text-exception">{{ detail.issueCount || 0 }}</span>
            </div>
          </div>
        </a-col>
      </a-row>

      <!-- 标签页 -->
      <div class="vibe-card tab-card">
        <a-tabs :active-key="activeTab" @change="handleTabChange">
          <!-- 基本信息 -->
          <a-tab-pane key="info" tab="基本信息">
            <a-descriptions v-if="detail" :column="3" bordered size="small">
              <a-descriptions-item label="项目编号">{{ detail.projectCode }}</a-descriptions-item>
              <a-descriptions-item label="项目名称">{{ detail.projectName }}</a-descriptions-item>
              <a-descriptions-item label="客户">{{ detail.customerName || detail.customerId }}</a-descriptions-item>
              <a-descriptions-item label="项目类型">{{ detail.projectType }}</a-descriptions-item>
              <a-descriptions-item label="产品线">{{ detail.productLine }}</a-descriptions-item>
              <a-descriptions-item label="执行模式">{{ detail.executeMode }}</a-descriptions-item>
              <a-descriptions-item label="优先级">{{ PriorityLabel[detail.priority as Priority] }}</a-descriptions-item>
              <a-descriptions-item label="项目经理">{{ detail.pmName || detail.pmId }}</a-descriptions-item>
              <a-descriptions-item label="区域">{{ detail.region || '-' }}</a-descriptions-item>
              <a-descriptions-item label="合同编号">{{ detail.contractNo || '-' }}</a-descriptions-item>
              <a-descriptions-item label="计划开始">{{ detail.plannedStart || '-' }}</a-descriptions-item>
              <a-descriptions-item label="计划结束">{{ detail.plannedEnd || '-' }}</a-descriptions-item>
              <a-descriptions-item label="实际开始">{{ detail.actualStart || '-' }}</a-descriptions-item>
              <a-descriptions-item label="实际结束">{{ detail.actualEnd || '-' }}</a-descriptions-item>
              <a-descriptions-item label="当前阶段">{{ detail.currentPhase || '-' }}</a-descriptions-item>
              <a-descriptions-item label="项目描述" :span="3">{{ detail.description || '-' }}</a-descriptions-item>
              <a-descriptions-item label="备注" :span="3">{{ detail.remark || '-' }}</a-descriptions-item>
            </a-descriptions>
          </a-tab-pane>

          <!-- 阶段 -->
          <a-tab-pane key="phases" tab="阶段">
            <div class="tab-toolbar">
              <a-button type="primary" size="small" @click="openPhaseCreate">
                <template #icon><PlusOutlined /></template>
                新增阶段
              </a-button>
            </div>
            <a-table
              :columns="phaseColumns"
              :data-source="phases"
              row-key="id"
              :pagination="false"
              size="small"
              :scroll="{ x: 1100 }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <StatusTag :tone="phaseStatusMap[record.status]?.tone">
                    {{ phaseStatusMap[record.status]?.label || record.status }}
                  </StatusTag>
                </template>
                <template v-else-if="column.key === 'progressPct'">
                  <ProgressBar :percent="record.progressPct || 0" />
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-button type="link" size="small" @click="openPhaseEdit(record)">编辑</a-button>
                  <a-divider type="vertical" />
                  <a-button type="link" size="small" danger @click="handlePhaseDelete(record)">删除</a-button>
                </template>
              </template>
              <template #emptyText><EmptyState description="暂无阶段数据" size="compact" /></template>
            </a-table>
          </a-tab-pane>

          <!-- 任务 -->
          <a-tab-pane key="tasks" tab="任务">
            <div class="tab-toolbar">
              <a-button type="primary" size="small" @click="handleOpenTaskModal">
                <template #icon><PlusOutlined /></template>
                新增任务
              </a-button>
            </div>
            <a-table
              :columns="taskColumns"
              :data-source="tasks"
              row-key="id"
              :pagination="{ pageSize: 10 }"
              size="small"
              :scroll="{ x: 1100 }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'executeMode'">
                  <a-tag :color="record.executeMode === 'AGENT' ? 'purple' : 'blue'">
                    {{ record.executeMode === 'AGENT' ? '代施' : '自施' }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'status'">
                  <StatusTag :tone="TaskStatusTone[record.status as TaskStatus]">
                    {{ TaskStatusLabel[record.status as TaskStatus] }}
                  </StatusTag>
                </template>
                <template v-else-if="column.key === 'progressPct'">
                  <ProgressBar :percent="record.progressPct || 0" />
                </template>
                <template v-else-if="column.key === 'priority'">
                  {{ PriorityLabel[record.priority as Priority] }}
                </template>
                <template v-else-if="column.key === 'plannedRange'">
                  <span class="text-auxiliary">{{ record.plannedStart || '-' }} ~ {{ record.plannedEnd || '-' }}</span>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a @click="goTask(record.id)">详情</a>
                </template>
              </template>
              <template #emptyText><EmptyState description="暂无任务" size="compact" /></template>
            </a-table>
          </a-tab-pane>

          <!-- 里程碑 -->
          <a-tab-pane key="milestones" tab="里程碑">
            <div class="tab-toolbar">
              <a-button type="primary" size="small" @click="openMilestoneCreate">
                <template #icon><PlusOutlined /></template>
                新增里程碑
              </a-button>
            </div>
            <a-timeline>
              <a-timeline-item
                v-for="m in milestones"
                :key="m.id"
                :color="m.status === 'ACHIEVED' ? 'green' : m.status === 'OVERDUE' ? 'red' : 'blue'"
              >
                <div class="milestone-item">
                  <span class="milestone-name">{{ m.milestoneName }}</span>
                  <span class="milestone-date">计划：{{ m.plannedDate }}</span>
                  <span v-if="m.actualDate" class="milestone-date">实际：{{ m.actualDate }}</span>
                  <StatusTag
                    :tone="m.status === 'ACHIEVED' ? 'success' : m.status === 'OVERDUE' ? 'error' : 'default'"
                    :text="m.status === 'ACHIEVED' ? '已达成' : m.status === 'OVERDUE' ? '已超期' : '待达成'"
                  />
                  <span class="milestone-actions">
                    <a-button type="link" size="small" @click="openMilestoneEdit(m)">编辑</a-button>
                    <a-divider type="vertical" />
                    <a-button type="link" size="small" danger @click="handleMilestoneDelete(m)">删除</a-button>
                  </span>
                </div>
                <div v-if="m.deliverables" class="milestone-deliverable">交付物：{{ m.deliverables }}</div>
              </a-timeline-item>
            </a-timeline>
            <EmptyState v-if="!milestones.length" description="暂无里程碑" size="compact" />
          </a-tab-pane>

          <!-- 风险 -->
          <a-tab-pane key="risks" tab="风险">
            <div class="tab-toolbar">
              <a-button type="primary" size="small" @click="openRiskCreate">
                <template #icon><PlusOutlined /></template>
                新增风险
              </a-button>
            </div>
            <a-table
              :data-source="risks"
              row-key="id"
              :pagination="false"
              size="small"
              :scroll="{ x: 1100 }"
              :columns="[
                { title: '风险描述', dataIndex: 'riskDesc', key: 'riskDesc', ellipsis: true },
                { title: '影响', dataIndex: 'impact', key: 'impact', width: 80 },
                { title: '概率', dataIndex: 'probability', key: 'probability', width: 80 },
                { title: '应对措施', dataIndex: 'measure', key: 'measure', ellipsis: true },
                { title: '责任人', dataIndex: 'ownerName', key: 'ownerName', width: 100 },
                { title: '状态', key: 'status', width: 100 },
                { title: '截止', dataIndex: 'dueDate', key: 'dueDate', width: 120 },
                { title: '操作', key: 'action', width: 120, fixed: 'right' }
              ]"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <StatusTag
                    :tone="record.status === 'CLOSED' ? 'success' : record.status === 'PROCESSING' ? 'processing' : 'warning'"
                    :text="record.status === 'CLOSED' ? '已关闭' : record.status === 'PROCESSING' ? '处理中' : '待处理'"
                  />
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-button type="link" size="small" @click="openRiskEdit(record)">编辑</a-button>
                  <a-divider type="vertical" />
                  <a-button type="link" size="small" danger @click="handleRiskDelete(record)">删除</a-button>
                </template>
              </template>
              <template #emptyText><EmptyState description="暂无风险登记" size="compact" /></template>
            </a-table>
          </a-tab-pane>

          <!-- 问题 -->
          <a-tab-pane key="issues" tab="问题">
            <div class="tab-toolbar">
              <a-button type="primary" size="small" @click="openIssueCreate">
                <template #icon><PlusOutlined /></template>
                新增问题
              </a-button>
            </div>
            <a-table
              :data-source="issues"
              row-key="id"
              :pagination="false"
              size="small"
              :scroll="{ x: 1100 }"
              :columns="[
                { title: '问题描述', dataIndex: 'issueDesc', key: 'issueDesc', ellipsis: true },
                { title: '影响', dataIndex: 'impact', key: 'impact', ellipsis: true },
                { title: '责任人', dataIndex: 'ownerName', key: 'ownerName', width: 100 },
                { title: '状态', key: 'status', width: 100 },
                { title: '截止', dataIndex: 'dueDate', key: 'dueDate', width: 120 },
                { title: '解决时间', dataIndex: 'resolvedTime', key: 'resolvedTime', width: 150 },
                { title: '操作', key: 'action', width: 120, fixed: 'right' }
              ]"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <StatusTag
                    :tone="record.status === 'CLOSED' ? 'success' : record.status === 'RESOLVED' ? 'success' : record.status === 'PROCESSING' ? 'processing' : 'warning'"
                    :text="({ OPEN: '待处理', PROCESSING: '处理中', RESOLVED: '已解决', CLOSED: '已关闭' } as Record<string, string>)[record.status]"
                  />
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-button type="link" size="small" @click="openIssueEdit(record)">编辑</a-button>
                  <a-divider type="vertical" />
                  <a-button type="link" size="small" danger @click="handleIssueDelete(record)">删除</a-button>
                </template>
              </template>
              <template #emptyText><EmptyState description="暂无问题记录" size="compact" /></template>
            </a-table>
          </a-tab-pane>

          <!-- 变更 -->
          <a-tab-pane key="changes" tab="变更">
            <a-table
              :data-source="changes"
              row-key="id"
              :pagination="false"
              size="small"
              :columns="[
                { title: '变更内容', dataIndex: 'changeContent', key: 'changeContent', ellipsis: true },
                { title: '类型', dataIndex: 'changeType', key: 'changeType', width: 100 },
                { title: '原因', dataIndex: 'reason', key: 'reason', ellipsis: true },
                { title: '申请人', dataIndex: 'applicantName', key: 'applicantName', width: 100 },
                { title: '审批人', dataIndex: 'approverName', key: 'approverName', width: 100 },
                { title: '状态', key: 'status', width: 100 },
                { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 }
              ]"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <StatusTag
                    :tone="record.status === 'APPROVED' ? 'success' : record.status === 'REJECTED' ? 'error' : record.status === 'EXECUTED' ? 'archived' : 'warning'"
                    :text="({ PENDING: '待审批', APPROVED: '已通过', REJECTED: '已拒绝', EXECUTED: '已执行' } as Record<string, string>)[record.status]"
                  />
                </template>
              </template>
              <template #emptyText><EmptyState description="暂无变更记录" size="compact" /></template>
            </a-table>
          </a-tab-pane>

          <!-- 成员 -->
          <a-tab-pane key="members" tab="成员">
            <div class="tab-toolbar">
              <a-button type="primary" size="small" @click="openMemberAdd">
                <template #icon><PlusOutlined /></template>
                添加成员
              </a-button>
            </div>
            <a-table
              :data-source="members"
              row-key="id"
              :pagination="false"
              size="small"
              :scroll="{ x: 900 }"
              :columns="[
                { title: '用户名', dataIndex: 'userName', key: 'userName' },
                { title: '姓名', dataIndex: 'realName', key: 'realName' },
                { title: '角色', dataIndex: 'role', key: 'role' },
                { title: '加入时间', dataIndex: 'joinTime', key: 'joinTime' },
                { title: '操作', key: 'action', width: 100, fixed: 'right' }
              ]"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'role'">
                  <a-tag>{{ record.role }}</a-tag>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-button type="link" size="small" danger @click="handleMemberRemove(record)">移除</a-button>
                </template>
              </template>
              <template #emptyText><EmptyState description="暂无成员" size="compact" /></template>
            </a-table>
          </a-tab-pane>

          <!-- 评论 -->
          <a-tab-pane key="comments" tab="评论">
            <div class="comment-input">
              <a-textarea
                v-model:value="commentText"
                :rows="3"
                placeholder="发表评论..."
                :maxlength="500"
                show-count
              />
              <div class="comment-actions">
                <a-button type="primary" :loading="commentSubmitting" @click="submitComment">发布</a-button>
              </div>
            </div>
            <a-list :data-source="comments" item-layout="horizontal">
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta :description="item.content">
                    <template #title>
                      {{ item.userName }}
                      <span class="comment-time">{{ item.createdAt }}</span>
                    </template>
                    <template #avatar>
                      <a-avatar>{{ item.userName?.charAt(0)?.toUpperCase() }}</a-avatar>
                    </template>
                  </a-list-item-meta>
                </a-list-item>
              </template>
              <template #emptyText><EmptyState description="暂无评论，快来发表第一条吧" size="compact" /></template>
            </a-list>
          </a-tab-pane>
        </a-tabs>
      </div>

      <!-- 新增任务弹窗 -->
      <FormModal
        v-model:visible="taskModalVisible"
        v-model:data="taskFormData"
        title="新增任务"
        :fields="taskFormFields"
        :loading="taskSubmitting"
        :width="720"
        :span="12"
        @submit="handleTaskSubmit"
      />

      <!-- 阶段新增/编辑弹窗 -->
      <a-modal
        v-model:open="phaseModalVisible"
        :title="phaseIsEdit ? '编辑阶段' : '新增阶段'"
        :confirm-loading="phaseLoading"
        @ok="handlePhaseSubmit"
      >
        <a-form layout="vertical">
          <a-form-item label="阶段编码" required>
            <a-select v-model:value="phaseForm.phaseCode" :options="phaseCodeOptions" placeholder="选择阶段" />
          </a-form-item>
          <a-form-item label="阶段名称" required>
            <a-input v-model:value="phaseForm.phaseName" placeholder="如：勘察设计" />
          </a-form-item>
          <a-form-item label="排序">
            <a-input-number v-model:value="phaseForm.sortOrder" :min="0" style="width: 100%" />
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="phaseForm.status" :options="phaseStatusOptions" />
          </a-form-item>
          <a-form-item label="计划开始">
            <a-date-picker v-model:value="phaseForm.plannedStart" value-format="YYYY-MM-DD" style="width: 100%" />
          </a-form-item>
          <a-form-item label="计划结束">
            <a-date-picker v-model:value="phaseForm.plannedEnd" value-format="YYYY-MM-DD" style="width: 100%" />
          </a-form-item>
          <a-form-item label="实际开始">
            <a-date-picker v-model:value="phaseForm.actualStart" value-format="YYYY-MM-DD" style="width: 100%" />
          </a-form-item>
          <a-form-item label="实际结束">
            <a-date-picker v-model:value="phaseForm.actualEnd" value-format="YYYY-MM-DD" style="width: 100%" />
          </a-form-item>
          <a-form-item label="交付物">
            <a-textarea v-model:value="phaseForm.deliverables" :rows="2" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 里程碑新增/编辑弹窗 -->
      <a-modal
        v-model:open="milestoneModalVisible"
        :title="milestoneIsEdit ? '编辑里程碑' : '新增里程碑'"
        :confirm-loading="milestoneLoading"
        @ok="handleMilestoneSubmit"
      >
        <a-form layout="vertical">
          <a-form-item label="里程碑名称" required>
            <a-input v-model:value="milestoneForm.milestoneName" placeholder="请输入里程碑名称" />
          </a-form-item>
          <a-form-item label="计划日期">
            <a-date-picker v-model:value="milestoneForm.plannedDate" value-format="YYYY-MM-DD" style="width: 100%" />
          </a-form-item>
          <a-form-item label="实际日期">
            <a-date-picker v-model:value="milestoneForm.actualDate" value-format="YYYY-MM-DD" style="width: 100%" />
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="milestoneForm.status" :options="milestoneStatusOptions" />
          </a-form-item>
          <a-form-item label="交付物">
            <a-textarea v-model:value="milestoneForm.deliverables" :rows="2" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 风险新增/编辑弹窗 -->
      <a-modal
        v-model:open="riskModalVisible"
        :title="riskIsEdit ? '编辑风险' : '新增风险'"
        :confirm-loading="riskLoading"
        @ok="handleRiskSubmit"
      >
        <a-form layout="vertical">
          <a-form-item label="风险描述" required>
            <a-textarea v-model:value="riskForm.riskDesc" :rows="2" placeholder="请输入风险描述" />
          </a-form-item>
          <a-form-item label="影响">
            <a-select v-model:value="riskForm.impact" :options="riskImpactOptions" />
          </a-form-item>
          <a-form-item label="概率">
            <a-select v-model:value="riskForm.probability" :options="riskImpactOptions" />
          </a-form-item>
          <a-form-item label="应对措施">
            <a-textarea v-model:value="riskForm.measure" :rows="2" placeholder="应对/缓解措施" />
          </a-form-item>
          <a-form-item label="责任人">
            <a-select
              v-model:value="riskForm.ownerId"
              :options="engineerOptions"
              placeholder="选择责任人"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="riskForm.status" :options="riskStatusOptions" />
          </a-form-item>
          <a-form-item label="截止日期">
            <a-date-picker v-model:value="riskForm.dueDate" value-format="YYYY-MM-DD" style="width: 100%" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 问题新增/编辑弹窗 -->
      <a-modal
        v-model:open="issueModalVisible"
        :title="issueIsEdit ? '编辑问题' : '新增问题'"
        :confirm-loading="issueLoading"
        @ok="handleIssueSubmit"
      >
        <a-form layout="vertical">
          <a-form-item label="问题描述" required>
            <a-textarea v-model:value="issueForm.issueDesc" :rows="2" placeholder="请输入问题描述" />
          </a-form-item>
          <a-form-item label="影响">
            <a-input v-model:value="issueForm.impact" placeholder="问题影响" />
          </a-form-item>
          <a-form-item label="责任人">
            <a-select
              v-model:value="issueForm.ownerId"
              :options="engineerOptions"
              placeholder="选择责任人"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="issueForm.status" :options="issueStatusOptions" />
          </a-form-item>
          <a-form-item label="截止日期">
            <a-date-picker v-model:value="issueForm.dueDate" value-format="YYYY-MM-DD" style="width: 100%" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 成员添加弹窗 -->
      <a-modal
        v-model:open="memberModalVisible"
        title="添加成员"
        :confirm-loading="memberLoading"
        @ok="handleMemberSubmit"
      >
        <a-form layout="vertical">
          <a-form-item label="用户" required>
            <a-select
              v-model:value="memberForm.userId"
              :options="userOptions"
              placeholder="选择用户"
              show-search
              option-filter-prop="label"
            />
          </a-form-item>
          <a-form-item label="角色" required>
            <a-select v-model:value="memberForm.role" :options="memberRoleOptions" />
          </a-form-item>
        </a-form>
      </a-modal>
    </a-spin>

    <!-- 归档弹窗 -->
    <a-modal
      v-model:open="archiveVisible"
      title="项目归档"
      :confirm-loading="archiveLoading"
      @ok="handleArchiveSubmit"
    >
      <a-form layout="vertical">
        <a-form-item label="复盘记录">
          <a-textarea v-model:value="archiveForm.reviewSummary" :rows="4" placeholder="项目复盘总结" />
        </a-form-item>
        <a-form-item label="经验沉淀">
          <a-textarea v-model:value="archiveForm.lessonsLearned" :rows="4" placeholder="经验教训沉淀" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.detail-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.project-code {
  margin-left: 12px;
  font-size: 13px;
  color: @text-tertiary;
  font-weight: 400;
}
.detail-meta {
  margin: 0;
  font-size: 13px;
  color: @text-tertiary;
}
.overview-row {
  margin-bottom: 16px;
}
.overview-card {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
}
.overview-label {
  font-size: 13px;
  color: @text-tertiary;
}
.overview-value {
  font-size: 16px;
}
.overview-sub {
  margin-left: 4px;
  font-size: 12px;
  color: @text-tertiary;
}
.tab-card {
  padding: 0 20px 16px;
}
.tab-toolbar {
  padding: 12px 0;
  display: flex;
  justify-content: flex-end;
}
.milestone-item {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.milestone-name {
  font-weight: 500;
}
.milestone-date {
  font-size: 12px;
  color: @text-tertiary;
}
.milestone-deliverable {
  margin-top: 4px;
  font-size: 12px;
  color: @text-secondary;
}
.milestone-actions {
  display: inline-flex;
  align-items: center;
  margin-left: auto;
}
.comment-input {
  margin: 12px 0;
}
.comment-actions {
  margin-top: 8px;
  text-align: right;
}
.comment-time {
  margin-left: 12px;
  font-size: 12px;
  color: @text-tertiary;
  font-weight: 400;
}
.text-exception {
  color: @status-exception;
  font-weight: 600;
}
</style>
