<script setup lang="ts">
/**
 * 割接管理
 * 设计文档 2.6.2：割接方案编制/内部审批/客户审批/执行/总结全流程
 */
import { ref, reactive, computed, onMounted, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  CheckOutlined,
  CloseOutlined,
  ThunderboltOutlined,
  RollbackOutlined,
  WarningOutlined,
  StopOutlined,
  LinkOutlined,
  PlayCircleOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageCutoverPlans,
  getCutoverPlanDetail,
  createCutoverPlan,
  updateCutoverPlan,
  deleteCutoverPlan,
  submitInternalApproval,
  internalApprovePlan,
  internalRejectPlan,
  startCustomerApproval,
  startCutoverExecution,
  executeCutoverStep,
  rollbackCutoverStep,
  exceptionCutoverStep,
  completeCutoverPlan,
  abortCutoverPlan,
  CutoverPlanStatusLabel,
  CutoverStepStatusLabel
} from '@/api/cutover'
import type {
  CutoverPlan,
  CutoverPlanDetail,
  CutoverPlanStatus,
  CutoverStep,
  CutoverStepStatus,
  CutoverExecutionLog,
  CutoverPlanDTO,
  CutoverPlanQueryParams,
  CutoverStepDTO,
  CutoverApprovalDTO,
  CutoverStepExecuteDTO,
  CutoverCompleteDTO
} from '@/api/cutover'
import type { PageResult } from '@/types/api'
import { pageProjects } from '@/api/project'
import { pageUsers } from '@/api/system'

/* ============ 列表加载 ============ */
const loading = ref(false)
const dataSource = ref<CutoverPlan[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const query = reactive<CutoverPlanQueryParams>({
  projectId: undefined,
  planName: '',
  status: undefined,
  dateFrom: '',
  dateTo: '',
  applyUserId: undefined
})
const dateRange = ref<[string, string] | undefined>()

// 实体引用字段下拉选项
// 项目（搜索表单 + 编辑弹窗共用）
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

// 步骤负责人（用户）
const ownerOptions = ref<Array<{ value: string | number; label: string }>>([])
async function loadOwners() {
  try {
    const res = await pageUsers({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    ownerOptions.value = list.map((u: any) => ({ value: u.id, label: u.realName || u.userName }))
  } catch (e) {
    console.warn('[owner] load failed:', e)
  }
}

// 选中负责人时同步 ownerName（步骤列表展示用）
function onOwnerChange(value: any) {
  const opt = ownerOptions.value.find((o) => o.value === value)
  stepInput.ownerName = opt ? opt.label : ''
}

async function loadData() {
  loading.value = true
  try {
    const res = (await pageCutoverPlans({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<CutoverPlan>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[delivery.cutover] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  if (dateRange.value && dateRange.value.length === 2) {
    query.dateFrom = dateRange.value[0]
    query.dateTo = dateRange.value[1]
  } else {
    query.dateFrom = ''
    query.dateTo = ''
  }
  loadData()
}

/* ============ 状态展示映射 ============ */
const planStatusTone: Record<CutoverPlanStatus, string> = {
  DRAFT: 'default',
  PENDING_INTERNAL_APPROVAL: 'processing',
  INTERNAL_APPROVED: 'success',
  INTERNAL_REJECTED: 'error',
  PENDING_CUSTOMER_APPROVAL: 'processing',
  CUSTOMER_APPROVED: 'success',
  CUSTOMER_REJECTED: 'error',
  EXECUTING: 'processing',
  COMPLETED: 'success',
  ABORTED: 'archived'
}

const stepStatusTone: Record<CutoverStepStatus, string> = {
  PENDING: 'default',
  EXECUTING: 'processing',
  COMPLETED: 'success',
  ROLLED_BACK: 'warning',
  ABORTED: 'archived'
}

const logLevelTone: Record<string, string> = {
  INFO: 'default',
  WARN: 'warning',
  ERROR: 'error'
}
const logLevelLabel: Record<string, string> = { INFO: '信息', WARN: '警告', ERROR: '错误' }

const statusOptions = (Object.keys(CutoverPlanStatusLabel) as CutoverPlanStatus[]).map((s) => ({
  label: CutoverPlanStatusLabel[s],
  value: s
}))

const columns = [
  { title: '方案名称', dataIndex: 'planName', key: 'planName', ellipsis: true },
  { title: '所属项目', dataIndex: 'projectName', key: 'projectName', width: 150, ellipsis: true },
  { title: '割接日期', dataIndex: 'cutoverDate', key: 'cutoverDate', width: 120 },
  { title: '起止时间', key: 'timeRange', width: 220 },
  { title: '步骤进度', key: 'stepProgress', width: 130 },
  { title: '状态', key: 'status', width: 130 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' }
]

function stepPercent(row: CutoverPlan): number {
  if (!row.stepCount) return 0
  return Math.round(((row.completedStepCount || 0) / row.stepCount) * 100)
}

/* ============ 创建/编辑 表单 ============ */
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<CutoverPlanDTO>({
  projectId: undefined as unknown as number,
  planName: '',
  cutoverDate: '',
  startTime: '',
  endTime: '',
  impactScope: '',
  emergencyContact: '',
  remark: '',
  steps: []
})
const stepInput = reactive<CutoverStepDTO>({
  stepName: '',
  description: '',
  estimatedDuration: 0,
  ownerId: undefined,
  ownerName: '',
  rollbackPlan: ''
})

function resetStepInput() {
  stepInput.stepName = ''
  stepInput.description = ''
  stepInput.estimatedDuration = 0
  stepInput.ownerId = undefined
  stepInput.ownerName = ''
  stepInput.rollbackPlan = ''
}

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    projectId: undefined,
    planName: '',
    cutoverDate: '',
    startTime: '',
    endTime: '',
    impactScope: '',
    emergencyContact: '',
    remark: '',
    steps: []
  })
  resetStepInput()
  loadOwners()
  formVisible.value = true
}

function openEdit(row: CutoverPlan) {
  // 仅 DRAFT / INTERNAL_REJECTED / CUSTOMER_REJECTED 允许编辑；这里仅做列表入口兜底
  isEdit.value = true
  loadOwners()
  // 拉取详情后填充
  getCutoverPlanDetail(row.id)
    .then((detail) => {
      const d = detail as unknown as CutoverPlanDetail
      Object.assign(formData, {
        projectId: d.projectId,
        planName: d.planName,
        cutoverDate: d.cutoverDate,
        startTime: d.startTime,
        endTime: d.endTime,
        impactScope: d.impactScope || '',
        emergencyContact: d.emergencyContact || '',
        remark: d.remark || '',
        steps: (d.steps || []).map((s) => ({
          id: s.id,
          sortOrder: s.sortOrder,
          stepName: s.stepName,
          description: s.description,
          estimatedDuration: s.estimatedDuration,
          ownerId: s.ownerId,
          ownerName: s.ownerName,
          rollbackPlan: s.rollbackPlan
        }))
      })
      resetStepInput()
      formVisible.value = true
    })
    .catch(() => {
      message.error('加载方案详情失败')
    })
}

function addStep() {
  if (!stepInput.stepName) {
    message.warning('请填写步骤名称')
    return
  }
  formData.steps = [
    ...(formData.steps || []),
    {
      stepName: stepInput.stepName,
      description: stepInput.description || undefined,
      estimatedDuration: stepInput.estimatedDuration || undefined,
      ownerId: stepInput.ownerId,
      ownerName: stepInput.ownerName || undefined,
      rollbackPlan: stepInput.rollbackPlan || undefined
    }
  ]
  resetStepInput()
}

function removeStep(i: number) {
  formData.steps?.splice(i, 1)
}

async function handleSubmit() {
  if (!formData.planName || !formData.projectId || !formData.cutoverDate || !formData.startTime || !formData.endTime) {
    message.warning('请填写方案名称、项目、割接日期和起止时间')
    return
  }
  if (!formData.steps || formData.steps.length === 0) {
    message.warning('请至少添加一个割接步骤')
    return
  }
  if (formData.startTime >= formData.endTime) {
    message.warning('开始时间必须早于结束时间')
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value) {
      // 重新查询得到当前 id（openEdit 时未保存）
      const id = (currentDetail.value as CutoverPlanDetail | null)?.id
      if (!id) {
        message.error('缺少方案 ID')
        return
      }
      await updateCutoverPlan(id, formData)
      message.success('更新成功')
    } else {
      await createCutoverPlan(formData)
      message.success('创建成功')
    }
    formVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    formLoading.value = false
  }
}

function handleDelete(row: CutoverPlan) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除割接方案「${row.planName}」吗？（仅草稿状态可删除）`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteCutoverPlan(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        /* ignore */
      }
    }
  })
}

/* ============ 详情抽屉 ============ */
const drawerVisible = ref(false)
const drawerLoading = ref(false)
const currentDetail = ref<CutoverPlanDetail | null>(null)
const drawerTab = ref('steps')

async function openDetail(row: CutoverPlan) {
  drawerVisible.value = true
  drawerLoading.value = true
  drawerTab.value = 'steps'
  try {
    currentDetail.value = (await getCutoverPlanDetail(row.id)) as unknown as CutoverPlanDetail
  } catch (e) {
    console.error('[delivery.cutover] load detail failed:', e)
  } finally {
    drawerLoading.value = false
  }
}

function refreshDetail() {
  if (currentDetail.value) {
    getCutoverPlanDetail(currentDetail.value.id)
      .then((d) => {
        currentDetail.value = d as unknown as CutoverPlanDetail
      })
      .catch(() => {
        /* ignore */
      })
  }
}

const stepColumns = [
  { title: '顺序', dataIndex: 'sortOrder', key: 'sortOrder', width: 60 },
  { title: '步骤名称', dataIndex: 'stepName', key: 'stepName', ellipsis: true },
  { title: '负责人', dataIndex: 'ownerName', key: 'ownerName', width: 110 },
  { title: '预计(min)', dataIndex: 'estimatedDuration', key: 'estimatedDuration', width: 90 },
  { title: '实际(min)', dataIndex: 'actualDuration', key: 'actualDuration', width: 90 },
  { title: '状态', key: 'status', width: 100 },
  { title: '执行备注', dataIndex: 'executionRemark', key: 'executionRemark', ellipsis: true },
  { title: '操作', key: 'action', width: 200 }
]

const logColumns = [
  { title: '时间', dataIndex: 'logTime', key: 'logTime', width: 170 },
  { title: '级别', key: 'logLevel', width: 90 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 200 },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: 110 },
  { title: '内容', dataIndex: 'logContent', key: 'logContent', ellipsis: true }
]

/* ============ 内部审批 ============ */
const internalApprovalVisible = ref(false)
const internalApprovalLoading = ref(false)
const internalApprovalForm = reactive<CutoverApprovalDTO>({ planId: 0, result: 'APPROVED', remark: '' })

function openInternalApproval(row: CutoverPlan) {
  internalApprovalForm.planId = row.id
  internalApprovalForm.result = 'APPROVED'
  internalApprovalForm.remark = ''
  internalApprovalVisible.value = true
}

async function handleInternalApproval() {
  internalApprovalLoading.value = true
  try {
    if (internalApprovalForm.result === 'APPROVED') {
      await internalApprovePlan(internalApprovalForm)
      message.success('已通过内部审批')
    } else {
      await internalRejectPlan(internalApprovalForm)
      message.success('已驳回内部审批')
    }
    internalApprovalVisible.value = false
    loadData()
    refreshDetail()
  } catch (e) {
    /* ignore */
  } finally {
    internalApprovalLoading.value = false
  }
}

/* ============ 步骤执行 ============ */
const stepExecVisible = ref(false)
const stepExecLoading = ref(false)
const stepExecForm = reactive<CutoverStepExecuteDTO>({ planId: 0, stepId: 0, executionRemark: '', exceptionRemark: '', actualDuration: undefined })
const stepExecMode = ref<'start' | 'complete' | 'rollback' | 'exception'>('start')
const stepExecTitle = computed(() => {
  switch (stepExecMode.value) {
    case 'start':
      return '开始执行步骤'
    case 'complete':
      return '标记步骤完成'
    case 'rollback':
      return '回退步骤'
    case 'exception':
      return '标记步骤异常'
  }
})

function openStepExec(step: CutoverStep, mode: 'start' | 'complete' | 'rollback' | 'exception') {
  if (!currentDetail.value) return
  stepExecMode.value = mode
  stepExecForm.planId = currentDetail.value.id
  stepExecForm.stepId = step.id
  stepExecForm.executionRemark = ''
  stepExecForm.exceptionRemark = ''
  stepExecForm.actualDuration = undefined
  stepExecVisible.value = true
}

async function handleStepExec() {
  stepExecLoading.value = true
  try {
    if (stepExecMode.value === 'start') {
      await executeCutoverStep(stepExecForm)
      message.success('步骤已开始执行')
    } else if (stepExecMode.value === 'complete') {
      await executeCutoverStep(stepExecForm)
      message.success('步骤已完成')
    } else if (stepExecMode.value === 'rollback') {
      await rollbackCutoverStep(stepExecForm)
      message.success('步骤已回退')
    } else {
      await exceptionCutoverStep(stepExecForm)
      message.success('步骤已标记异常')
    }
    stepExecVisible.value = false
    refreshDetail()
  } catch (e) {
    /* ignore */
  } finally {
    stepExecLoading.value = false
  }
}

/* ============ 完成总结 ============ */
const completeVisible = ref(false)
const completeLoading = ref(false)
const completeForm = reactive<CutoverCompleteDTO>({ planId: 0, summary: '', problemImprovement: '' })

function openComplete() {
  if (!currentDetail.value) return
  completeForm.planId = currentDetail.value.id
  completeForm.summary = ''
  completeForm.problemImprovement = ''
  completeVisible.value = true
}

async function handleComplete() {
  if (!completeForm.summary) {
    message.warning('请填写执行总结')
    return
  }
  completeLoading.value = true
  try {
    await completeCutoverPlan(completeForm)
    message.success('割接已完成')
    completeVisible.value = false
    loadData()
    refreshDetail()
  } catch (e) {
    /* ignore */
  } finally {
    completeLoading.value = false
  }
}

/* ============ 中止 ============ */
function handleAbort(row: CutoverPlan) {
  let remark = ''
  Modal.confirm({
    title: '确认中止割接',
    content: () =>
      // 用 render 函数嵌入输入框（保持简单，使用 prompt 替代）
      h('div', { style: 'margin-top:8px' }, [
        h('p', { style: 'margin-bottom:8px' }, `确定要中止割接方案「${row.planName}」吗？`),
        h('textarea', {
          style: 'width:100%;min-height:60px;padding:4px 8px;',
          placeholder: '中止原因（可选）',
          onInput: (e: any) => {
            remark = e.target.value
          }
        })
      ]),
    okType: 'danger',
    async onOk() {
      try {
        await abortCutoverPlan(row.id, remark || undefined)
        message.success('已中止割接')
        loadData()
        refreshDetail()
      } catch (e) {
        /* ignore */
      }
    }
  })
}

/* ============ 客户审批链接 ============ */
const customerSignLinkVisible = ref(false)
const customerSignLink = ref('')

async function handleStartCustomerApproval(row: CutoverPlan) {
  try {
    const token = (await startCustomerApproval(row.id)) as unknown as string
    customerSignLink.value = token
    customerSignLinkVisible.value = true
    message.success('已发起客户审批')
    loadData()
    refreshDetail()
  } catch (e) {
    /* ignore */
  }
}

function copyLink() {
  navigator.clipboard?.writeText(customerSignLink.value)
  message.success('链接已复制到剪贴板')
}

/* ============ 触发执行 ============ */
async function handleStartExecution(row: CutoverPlan) {
  Modal.confirm({
    title: '开始执行割接',
    content: `确定要开始执行割接方案「${row.planName}」吗？`,
    async onOk() {
      try {
        await startCutoverExecution(row.id)
        message.success('割接已开始执行')
        loadData()
        refreshDetail()
      } catch (e) {
        /* ignore */
      }
    }
  })
}

async function handleSubmitInternalApproval(row: CutoverPlan) {
  Modal.confirm({
    title: '提交内部审批',
    content: `确定要提交割接方案「${row.planName}」到内部审批吗？`,
    async onOk() {
      try {
        await submitInternalApproval(row.id)
        message.success('已提交内部审批')
        loadData()
      } catch (e) {
        /* ignore */
      }
    }
  })
}

onMounted(() => {
  loadProjects()
  loadData()
})
</script>

<template>
  <PageContainer title="割接管理" description="割接方案编制/内部审批/客户审批/执行/总结全流程">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新建割接方案</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="方案名称">
          <a-input v-model:value="query.planName" placeholder="方案名称" allow-clear style="width: 200px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="项目">
          <a-select
            v-model:value="query.projectId"
            placeholder="选择项目"
            allow-clear
            show-search
            style="width: 200px"
            :options="projectOptions"
            :filter-option="(input: string, option: any) => option.label.includes(input)"
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 150px" :options="statusOptions" />
        </a-form-item>
        <a-form-item label="割接日期">
          <a-range-picker v-model:value="dateRange" value-format="YYYY-MM-DD" style="width: 240px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="vibe-card table-card">
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1500 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'timeRange'">
            <span class="text-auxiliary">{{ record.startTime }} ~ {{ record.endTime }}</span>
          </template>
          <template v-else-if="column.key === 'stepProgress'">
            <span>
              <a-progress :percent="stepPercent(record)" size="small" :show-info="false" style="width: 80px; display: inline-block; vertical-align: middle" />
              <span class="text-auxiliary" style="margin-left: 8px">{{ record.completedStepCount || 0 }}/{{ record.stepCount || 0 }}</span>
            </span>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="planStatusTone[record.status as CutoverPlanStatus]">{{ CutoverPlanStatusLabel[record.status as CutoverPlanStatus] || record.status }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small" wrap>
              <a @click="openDetail(record)"><EyeOutlined /> 详情</a>
              <a v-if="record.status === 'DRAFT'" @click="openEdit(record)"><EditOutlined /> 编辑</a>
              <a v-if="record.status === 'DRAFT'" class="danger-link" @click="handleDelete(record)"><DeleteOutlined /> 删除</a>
              <a v-if="record.status === 'DRAFT'" @click="handleSubmitInternalApproval(record)"><ThunderboltOutlined /> 提交审批</a>
              <a v-if="record.status === 'PENDING_INTERNAL_APPROVAL'" @click="openInternalApproval(record)"><CheckOutlined /> 审批</a>
              <a v-if="record.status === 'INTERNAL_APPROVED'" @click="handleStartCustomerApproval(record)"><LinkOutlined /> 发起客户审批</a>
              <a v-if="record.status === 'CUSTOMER_APPROVED'" @click="handleStartExecution(record)"><PlayCircleOutlined /> 开始执行</a>
              <a v-if="record.status === 'EXECUTING'" @click="openComplete(record)"><CheckOutlined /> 完成总结</a>
              <a v-if="record.status === 'EXECUTING'" class="danger-link" @click="handleAbort(record)"><StopOutlined /> 中止</a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无割接方案" action-text="新建方案" @action="openCreate" /></template>
      </a-table>
    </div>

    <!-- 新建/编辑方案 -->
    <a-modal v-model:open="formVisible" :title="isEdit ? '编辑割接方案' : '新建割接方案'" width="880px" :confirm-loading="formLoading" @ok="handleSubmit">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="方案名称" required>
              <a-input v-model:value="formData.planName" placeholder="如 核心机房网络割接" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="所属项目" required>
              <a-select
                v-model:value="formData.projectId"
                placeholder="选择项目"
                show-search
                :options="projectOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="割接日期" required>
              <a-date-picker v-model:value="formData.cutoverDate" value-format="YYYY-MM-DD" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="计划开始时间" required>
              <a-date-picker v-model:value="formData.startTime" value-format="YYYY-MM-DD HH:mm:ss" show-time format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="计划结束时间" required>
              <a-date-picker v-model:value="formData.endTime" value-format="YYYY-MM-DD HH:mm:ss" show-time format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="影响范围">
              <a-input v-model:value="formData.impactScope" placeholder="如: A区核心交换机" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="紧急联系人">
              <a-input v-model:value="formData.emergencyContact" placeholder="如: 张三 13800138000" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注">
              <a-textarea v-model:value="formData.remark" :rows="2" placeholder="方案备注" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider orientation="left" plain>割接步骤</a-divider>
        <div class="step-input-row">
          <a-input v-model:value="stepInput.stepName" placeholder="步骤名称 *" style="width: 22%" />
          <a-select
            v-model:value="stepInput.ownerId"
            placeholder="负责人"
            show-search
            allow-clear
            style="width: 26%"
            :options="ownerOptions"
            :filter-option="(input: string, option: any) => option.label.includes(input)"
            @change="onOwnerChange"
          />
          <a-input-number v-model:value="stepInput.estimatedDuration" placeholder="预计分钟" :min="0" style="width: 12%" />
          <a-input v-model:value="stepInput.rollbackPlan" placeholder="回退方案" style="width: 28%" />
          <a-button type="primary" style="width: 12%" @click="addStep">添加</a-button>
        </div>
        <a-textarea v-model:value="stepInput.description" :rows="1" placeholder="步骤描述（可选，添加前填写）" style="margin-top: 6px" />
        <div class="step-list" v-if="formData.steps && formData.steps.length">
          <a-tag v-for="(s, i) in formData.steps" :key="i" closable color="blue" @close="removeStep(i)" style="margin: 4px; padding: 4px 8px; line-height: 1.6">
            <div>
              <strong>{{ i + 1 }}. {{ s.stepName }}</strong>
              <div class="text-auxiliary" style="font-size: 12px">
                <span v-if="s.ownerName">负责人: {{ s.ownerName }}</span>
                <span v-if="s.estimatedDuration" style="margin-left: 8px">预计: {{ s.estimatedDuration }}min</span>
              </div>
              <div v-if="s.rollbackPlan" class="text-auxiliary" style="font-size: 12px">回退: {{ s.rollbackPlan }}</div>
            </div>
          </a-tag>
        </div>
        <a-empty v-else description="尚未添加步骤" :image="null" style="margin: 12px 0" />
      </a-form>
    </a-modal>

    <!-- 详情抽屉 -->
    <a-drawer :open="drawerVisible" :width="960" :title="currentDetail?.planName || '割接方案详情'" @close="drawerVisible = false">
      <a-spin :spinning="drawerLoading">
        <a-descriptions v-if="currentDetail" :column="3" size="small" bordered class="desc-block">
          <a-descriptions-item label="所属项目">{{ currentDetail.projectName || currentDetail.projectId }}</a-descriptions-item>
          <a-descriptions-item label="割接日期">{{ currentDetail.cutoverDate }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <StatusTag :tone="planStatusTone[currentDetail.status]">{{ CutoverPlanStatusLabel[currentDetail.status] }}</StatusTag>
          </a-descriptions-item>
          <a-descriptions-item label="计划起止" :span="2">{{ currentDetail.startTime }} ~ {{ currentDetail.endTime }}</a-descriptions-item>
          <a-descriptions-item label="步骤进度">
            {{ currentDetail.completedStepCount || 0 }} / {{ currentDetail.stepCount || 0 }}
          </a-descriptions-item>
          <a-descriptions-item label="影响范围" :span="3">{{ currentDetail.impactScope || '-' }}</a-descriptions-item>
          <a-descriptions-item label="紧急联系人" :span="3">{{ currentDetail.emergencyContact || '-' }}</a-descriptions-item>
          <a-descriptions-item label="申请人">{{ currentDetail.applyUserName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="申请时间">{{ currentDetail.applyTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="审批人">{{ currentDetail.approvalUserName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="审批时间">{{ currentDetail.approvalTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="审批意见" :span="2">{{ currentDetail.approvalRemark || '-' }}</a-descriptions-item>
          <a-descriptions-item label="客户签核人">{{ currentDetail.customerSignUser || '-' }}</a-descriptions-item>
          <a-descriptions-item label="客户签核时间">{{ currentDetail.customerSignTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="客户签核结果">
            <span v-if="currentDetail.customerSignResult === 'APPROVED'">通过</span>
            <span v-else-if="currentDetail.customerSignResult === 'REJECTED'">驳回</span>
            <span v-else>-</span>
          </a-descriptions-item>
          <a-descriptions-item label="客户签核意见" :span="3">{{ currentDetail.customerSignRemark || '-' }}</a-descriptions-item>
          <a-descriptions-item label="实际开始">{{ currentDetail.actualStartTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="实际结束">{{ currentDetail.actualEndTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="备注">{{ currentDetail.remark || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="currentDetail.summary" label="执行总结" :span="3">{{ currentDetail.summary }}</a-descriptions-item>
          <a-descriptions-item v-if="currentDetail.problemImprovement" label="问题与改进" :span="3">{{ currentDetail.problemImprovement }}</a-descriptions-item>
        </a-descriptions>

        <a-tabs v-model:activeKey="drawerTab" class="detail-tabs">
          <a-tab-pane key="steps" tab="割接步骤">
            <a-table :columns="stepColumns" :data-source="currentDetail?.steps || []" row-key="id" size="small" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <StatusTag :tone="stepStatusTone[record.status as CutoverStepStatus]">{{ CutoverStepStatusLabel[record.status as CutoverStepStatus] || record.status }}</StatusTag>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-space size="small" v-if="currentDetail?.status === 'EXECUTING'">
                    <a v-if="record.status === 'PENDING'" @click="openStepExec(record, 'start')"><PlayCircleOutlined /> 开始</a>
                    <a v-if="record.status === 'EXECUTING'" @click="openStepExec(record, 'complete')"><CheckOutlined /> 完成</a>
                    <a v-if="record.status === 'EXECUTING'" @click="openStepExec(record, 'rollback')"><RollbackOutlined /> 回退</a>
                    <a v-if="record.status !== 'COMPLETED' && record.status !== 'ABORTED'" class="warning-link" @click="openStepExec(record, 'exception')"><WarningOutlined /> 异常</a>
                  </a-space>
                  <span v-else class="text-auxiliary">-</span>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="logs" :tab="`操作日志 (${currentDetail?.logs?.length || 0})`">
            <a-table :columns="logColumns" :data-source="currentDetail?.logs || []" row-key="id" size="small" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'logLevel'">
                  <StatusTag :tone="logLevelTone[record.logLevel]">{{ logLevelLabel[record.logLevel] || record.logLevel }}</StatusTag>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
        </a-tabs>
      </a-spin>
    </a-drawer>

    <!-- 内部审批 -->
    <a-modal v-model:open="internalApprovalVisible" title="内部审批" :confirm-loading="internalApprovalLoading" @ok="handleInternalApproval">
      <a-form layout="vertical">
        <a-form-item label="审批结果" required>
          <a-radio-group v-model:value="internalApprovalForm.result">
            <a-radio value="APPROVED">通过</a-radio>
            <a-radio value="REJECTED">驳回</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审批意见" :required="internalApprovalForm.result === 'REJECTED'">
          <a-textarea v-model:value="internalApprovalForm.remark" :rows="3" :placeholder="internalApprovalForm.result === 'REJECTED' ? '请说明驳回原因' : '可填写审批意见'" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 步骤执行 -->
    <a-modal v-model:open="stepExecVisible" :title="stepExecTitle" :confirm-loading="stepExecLoading" @ok="handleStepExec">
      <a-form layout="vertical">
        <template v-if="stepExecMode === 'complete'">
          <a-form-item label="实际耗时（分钟）">
            <a-input-number v-model:value="stepExecForm.actualDuration" :min="0" style="width: 100%" placeholder="留空则按起止时间自动计算" />
          </a-form-item>
          <a-form-item label="执行备注">
            <a-textarea v-model:value="stepExecForm.executionRemark" :rows="3" placeholder="执行情况说明" />
          </a-form-item>
        </template>
        <template v-else-if="stepExecMode === 'exception'">
          <a-form-item label="异常说明" required>
            <a-textarea v-model:value="stepExecForm.exceptionRemark" :rows="3" placeholder="请描述异常情况" />
          </a-form-item>
        </template>
        <template v-else-if="stepExecMode === 'rollback'">
          <a-form-item label="回退备注">
            <a-textarea v-model:value="stepExecForm.executionRemark" :rows="3" placeholder="回退情况说明" />
          </a-form-item>
        </template>
        <template v-else>
          <a-form-item label="执行备注">
            <a-textarea v-model:value="stepExecForm.executionRemark" :rows="3" placeholder="开始执行的备注" />
          </a-form-item>
        </template>
      </a-form>
    </a-modal>

    <!-- 完成总结 -->
    <a-modal v-model:open="completeVisible" title="割接完成总结" :confirm-loading="completeLoading" @ok="handleComplete">
      <a-form layout="vertical">
        <a-form-item label="执行总结" required>
          <a-textarea v-model:value="completeForm.summary" :rows="4" placeholder="整体执行情况总结" />
        </a-form-item>
        <a-form-item label="问题与改进">
          <a-textarea v-model:value="completeForm.problemImprovement" :rows="3" placeholder="遗留问题及改进建议" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 客户审批链接 -->
    <a-modal v-model:open="customerSignLinkVisible" title="客户签核链接" :footer="null">
      <p class="text-auxiliary">请将以下链接发送给客户进行签核：</p>
      <a-input-group compact>
        <a-input :value="customerSignLink" read-only style="width: calc(100% - 90px)" />
        <a-button type="primary" style="width: 90px" @click="copyLink">复制</a-button>
      </a-input-group>
      <p class="text-auxiliary" style="margin-top: 8px; font-size: 12px">提示: 客户打开链接完成签核后，方案状态将自动更新为「客户审批通过/驳回」</p>
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
.desc-block {
  margin-bottom: 16px;
}
.detail-tabs {
  margin-top: 8px;
}
.step-input-row {
  display: flex;
  gap: 6px;
}
.step-list {
  margin-top: 8px;
}
.danger-link {
  color: @status-exception;
}
.warning-link {
  color: @status-warning;
}
</style>
