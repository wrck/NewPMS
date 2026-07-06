<script setup lang="ts">
/**
 * 代理商档案
 * 代理商公司管理：档案、合作状态、工程师子档案、评分
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined,
  TeamOutlined,
  CheckCircleOutlined,
  StopOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageAgentCompanies,
  createAgentCompany,
  updateAgentCompany,
  deleteAgentCompany,
  changeAgentCompanyStatus,
  listAgentEngineers,
  createAgentEngineer,
  updateAgentEngineer,
  deleteAgentEngineer,
  changeAgentEngineerStatus,
  listAgentScores
} from '@/api/agent'
import type {
  AgentCompany,
  AgentEngineer,
  AgentScore,
  AgentCompanyDTO,
  AgentEngineerDTO,
  AgentCompanyQueryParams,
  AgentStatus
} from '@/types/agent'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<AgentCompany[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const query = reactive<AgentCompanyQueryParams>({ companyName: '', status: undefined, region: '', productLine: '' })

async function loadData() {
  loading.value = true
  try {
    const res = (await pageAgentCompanies({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<AgentCompany>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[agent.profile] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

const statusMap: Record<AgentStatus, { tone: any; label: string }> = {
  ACTIVE: { tone: 'success', label: '合作中' },
  SUSPENDED: { tone: 'warning', label: '暂停' },
  TERMINATED: { tone: 'archived', label: '终止' }
}

const columns = [
  { title: '公司编码', dataIndex: 'companyCode', key: 'companyCode', width: 120 },
  { title: '公司名称', dataIndex: 'companyName', key: 'companyName', ellipsis: true },
  { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 100 },
  { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 130 },
  { title: '服务区域', key: 'serviceRegions', width: 150, ellipsis: true },
  { title: '产品线', key: 'productLines', width: 150, ellipsis: true },
  { title: '综合评分', key: 'overallScore', width: 110 },
  { title: '项目数', dataIndex: 'projectCount', key: 'projectCount', width: 90 },
  { title: '合作状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
]

// 公司弹窗
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<AgentCompanyDTO>({
  companyName: '',
  companyCode: '',
  qualification: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  address: '',
  serviceRegions: [],
  productLines: [],
  status: 'ACTIVE',
  cooperationStart: '',
  cooperationEnd: '',
  remark: ''
})
const regionInput = ref('')
const productLineInput = ref('')

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    companyName: '',
    companyCode: '',
    qualification: '',
    contactName: '',
    contactPhone: '',
    contactEmail: '',
    address: '',
    serviceRegions: [],
    productLines: [],
    status: 'ACTIVE',
    cooperationStart: '',
    cooperationEnd: '',
    remark: ''
  })
  regionInput.value = ''
  productLineInput.value = ''
  formVisible.value = true
}

function openEdit(row: AgentCompany) {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    companyName: row.companyName,
    companyCode: row.companyCode,
    qualification: row.qualification,
    contactName: row.contactName,
    contactPhone: row.contactPhone,
    contactEmail: row.contactEmail,
    address: row.address,
    serviceRegions: row.serviceRegions ? [...row.serviceRegions] : [],
    productLines: row.productLines ? [...row.productLines] : [],
    status: row.status,
    cooperationStart: row.cooperationStart,
    cooperationEnd: row.cooperationEnd,
    remark: row.remark
  })
  regionInput.value = ''
  productLineInput.value = ''
  formVisible.value = true
}

function addRegion() {
  const v = regionInput.value.trim()
  if (v && !formData.serviceRegions?.includes(v)) {
    formData.serviceRegions = [...(formData.serviceRegions || []), v]
  }
  regionInput.value = ''
}

function addProductLine() {
  const v = productLineInput.value.trim()
  if (v && !formData.productLines?.includes(v)) {
    formData.productLines = [...(formData.productLines || []), v]
  }
  productLineInput.value = ''
}

async function handleSubmit() {
  if (!formData.companyName || !formData.companyCode) {
    message.warning('请填写公司名称和编码')
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateAgentCompany(formData.id, formData)
      message.success('更新成功')
    } else {
      await createAgentCompany(formData)
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

function handleDelete(row: AgentCompany) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除代理商「${row.companyName}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteAgentCompany(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) { /* ignore */ }
    }
  })
}

function handleChangeStatus(row: AgentCompany, status: AgentStatus) {
  const label = statusMap[status].label
  Modal.confirm({
    title: '变更合作状态',
    content: `确定将「${row.companyName}」状态变更为「${label}」吗？`,
    async onOk() {
      try {
        await changeAgentCompanyStatus(row.id, status)
        message.success('状态已变更')
        loadData()
      } catch (e) { /* ignore */ }
    }
  })
}

// 工程师/评分抽屉
const drawerVisible = ref(false)
const drawerLoading = ref(false)
const currentCompany = ref<AgentCompany | null>(null)
const engineerList = ref<AgentEngineer[]>([])
const scoreList = ref<AgentScore[]>([])
const drawerTab = ref('engineers')

async function openDrawer(row: AgentCompany) {
  currentCompany.value = row
  drawerVisible.value = true
  drawerTab.value = 'engineers'
  await loadEngineers(row.id)
  loadScores(row.id)
}

async function loadEngineers(companyId: number) {
  drawerLoading.value = true
  try {
    engineerList.value = (await listAgentEngineers(companyId)) as unknown as AgentEngineer[]
  } catch (e) {
    console.error('[agent.profile] load engineers failed:', e)
  } finally {
    drawerLoading.value = false
  }
}

async function loadScores(companyId: number) {
  try {
    scoreList.value = (await listAgentScores(companyId)) as unknown as AgentScore[]
  } catch (e) {
    console.error('[agent.profile] load scores failed:', e)
  }
}

const engineerStatusMap: Record<string, { tone: any; label: string }> = {
  ACTIVE: { tone: 'success', label: '可用' },
  DISABLED: { tone: 'archived', label: '停用' }
}
const skillLevelLabel: Record<string, string> = {
  JUNIOR: '初级',
  INTERMEDIATE: '中级',
  SENIOR: '高级',
  EXPERT: '专家'
}

const engineerColumns = [
  { title: '姓名', dataIndex: 'name', key: 'name', width: 100 },
  { title: '电话', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 180, ellipsis: true },
  { title: '技能', key: 'skills' },
  { title: '任务数', dataIndex: 'taskCount', key: 'taskCount', width: 80 },
  { title: '质量评分', dataIndex: 'qualityScore', key: 'qualityScore', width: 100 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 200 }
]

// 工程师弹窗
const engVisible = ref(false)
const engLoading = ref(false)
const engIsEdit = ref(false)
const engForm = reactive<AgentEngineerDTO>({
  agentCompanyId: 0,
  name: '',
  phone: '',
  email: '',
  skills: [],
  certifications: [],
  status: 'ACTIVE'
})
const skillInput = reactive({ name: '', level: 'JUNIOR' as 'JUNIOR' | 'INTERMEDIATE' | 'SENIOR' | 'EXPERT' })

function openEngCreate() {
  if (!currentCompany.value) return
  engIsEdit.value = false
  Object.assign(engForm, {
    id: undefined,
    agentCompanyId: currentCompany.value.id,
    name: '',
    phone: '',
    email: '',
    skills: [],
    certifications: [],
    status: 'ACTIVE'
  })
  skillInput.name = ''
  skillInput.level = 'JUNIOR'
  engVisible.value = true
}

function openEngEdit(row: AgentEngineer) {
  engIsEdit.value = true
  Object.assign(engForm, {
    id: row.id,
    agentCompanyId: row.agentCompanyId,
    name: row.name,
    phone: row.phone,
    email: row.email,
    skills: row.skills ? [...row.skills] : [],
    certifications: row.certifications ? [...row.certifications] : [],
    status: row.status
  })
  skillInput.name = ''
  skillInput.level = 'JUNIOR'
  engVisible.value = true
}

function addSkill() {
  const n = skillInput.name.trim()
  if (n && !engForm.skills?.find((s) => s.name === n)) {
    engForm.skills = [...(engForm.skills || []), { name: n, level: skillInput.level }]
  }
  skillInput.name = ''
}

async function handleEngSubmit() {
  if (!engForm.name || !engForm.phone) {
    message.warning('请填写姓名和电话')
    return
  }
  engLoading.value = true
  try {
    if (engIsEdit.value && engForm.id && currentCompany.value) {
      await updateAgentEngineer(currentCompany.value.id, engForm.id, engForm)
      message.success('更新成功')
    } else if (currentCompany.value) {
      await createAgentEngineer(currentCompany.value.id, engForm)
      message.success('添加成功')
    }
    engVisible.value = false
    loadEngineers(currentCompany.value!.id)
  } catch (e) {
    // ignore
  } finally {
    engLoading.value = false
  }
}

function handleEngDelete(row: AgentEngineer) {
  if (!currentCompany.value) return
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除工程师「${row.name}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteAgentEngineer(currentCompany.value!.id, row.id)
        message.success('删除成功')
        loadEngineers(currentCompany.value!.id)
      } catch (e) { /* ignore */ }
    }
  })
}

/** 启用/停用代理商工程师 */
function handleEngStatus(row: AgentEngineer, status: 'ACTIVE' | 'DISABLED') {
  if (!currentCompany.value) return
  const label = status === 'ACTIVE' ? '启用' : '停用'
  Modal.confirm({
    title: `确认${label}`,
    content: `确定${label}工程师「${row.name}」吗？${status === 'DISABLED' ? '停用后该工程师将不再被分配新任务。' : ''}`,
    async onOk() {
      try {
        await changeAgentEngineerStatus(currentCompany.value!.id, row.id, status)
        message.success(`${label}成功`)
        loadEngineers(currentCompany.value!.id)
      } catch (e) {
        console.error('[agent.profile] change engineer status failed:', e)
      }
    }
  })
}

const scoreColumns = [
  { title: '任务', dataIndex: 'taskName', key: 'taskName', ellipsis: true },
  { title: '及时性', dataIndex: 'timeliness', key: 'timeliness', width: 90 },
  { title: '质量', dataIndex: 'quality', key: 'quality', width: 80 },
  { title: '沟通', dataIndex: 'communication', key: 'communication', width: 80 },
  { title: '问题率', dataIndex: 'issueRate', key: 'issueRate', width: 90 },
  { title: '综合分', dataIndex: 'overallScore', key: 'overallScore', width: 90 },
  { title: '评分人', dataIndex: 'scorerName', key: 'scorerName', width: 100 },
  { title: '时间', dataIndex: 'scoredAt', key: 'scoredAt', width: 160 },
  { title: '评语', dataIndex: 'comment', key: 'comment', ellipsis: true }
]

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="代理商档案" description="代理商公司档案、合作状态、工程师与评分管理">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新增代理商</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="公司名称">
          <a-input v-model:value="query.companyName" placeholder="公司名称" allow-clear style="width: 200px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="服务区域">
          <a-input v-model:value="query.region" placeholder="如华北" allow-clear style="width: 140px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="产品线">
          <a-input v-model:value="query.productLine" placeholder="产品线" allow-clear style="width: 140px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 120px">
            <a-select-option v-for="(v, k) in statusMap" :key="k" :value="k">{{ v.label }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="vibe-card table-card">
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1400 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'serviceRegions'">
            <a-tag v-for="(r, i) in record.serviceRegions || []" :key="i" color="blue">{{ r }}</a-tag>
          </template>
          <template v-else-if="column.key === 'productLines'">
            <a-tag v-for="(p, i) in record.productLines || []" :key="i">{{ p }}</a-tag>
          </template>
          <template v-else-if="column.key === 'overallScore'">
            <span class="tnum" :class="{ 'text-warning': (record.overallScore || 0) < 80 }">{{ record.overallScore?.toFixed(1) || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="statusMap[record.status as AgentStatus]?.tone">{{ statusMap[record.status as AgentStatus]?.label || record.status }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a @click="openDrawer(record)"><TeamOutlined /> 详情</a>
              <a-divider type="vertical" />
              <a @click="openEdit(record)"><EditOutlined /></a>
              <a-dropdown>
                <a @click.prevent>更多</a>
                <template #overlay>
                  <a-menu>
                    <a-menu-item v-if="record.status !== 'ACTIVE'" @click="handleChangeStatus(record, 'ACTIVE')">设为合作中</a-menu-item>
                    <a-menu-item v-if="record.status !== 'SUSPENDED'" @click="handleChangeStatus(record, 'SUSPENDED')">暂停合作</a-menu-item>
                    <a-menu-item v-if="record.status !== 'TERMINATED'" @click="handleChangeStatus(record, 'TERMINATED')">终止合作</a-menu-item>
                    <a-menu-divider />
                    <a-menu-item danger @click="handleDelete(record)"><DeleteOutlined /> 删除</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无代理商" action-text="新增代理商" @action="openCreate" /></template>
      </a-table>
    </div>

    <!-- 新增/编辑公司 -->
    <a-modal v-model:open="formVisible" :title="isEdit ? '编辑代理商' : '新增代理商'" width="720px" :confirm-loading="formLoading" @ok="handleSubmit">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="公司编码" required>
              <a-input v-model:value="formData.companyCode" :disabled="isEdit" placeholder="如 AGT-001" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="公司名称" required>
              <a-input v-model:value="formData.companyName" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="资质等级">
              <a-input v-model:value="formData.qualification" placeholder="如 高新技术企业" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="合作状态">
              <a-select v-model:value="formData.status">
                <a-select-option v-for="(v, k) in statusMap" :key="k" :value="k">{{ v.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="联系人">
              <a-input v-model:value="formData.contactName" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="联系电话">
              <a-input v-model:value="formData.contactPhone" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="联系邮箱">
              <a-input v-model:value="formData.contactEmail" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="地址">
              <a-input v-model:value="formData.address" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="服务区域">
              <a-input-group compact>
                <a-input v-model:value="regionInput" placeholder="输入后回车添加" style="width: calc(100% - 64px)" @pressEnter="addRegion" />
                <a-button type="primary" @click="addRegion">添加</a-button>
              </a-input-group>
              <div class="tag-list">
                <a-tag v-for="(r, i) in formData.serviceRegions || []" :key="i" closable color="blue" @close="formData.serviceRegions?.splice(i, 1)">{{ r }}</a-tag>
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="产品线">
              <a-input-group compact>
                <a-input v-model:value="productLineInput" placeholder="输入后回车添加" style="width: calc(100% - 64px)" @pressEnter="addProductLine" />
                <a-button type="primary" @click="addProductLine">添加</a-button>
              </a-input-group>
              <div class="tag-list">
                <a-tag v-for="(p, i) in formData.productLines || []" :key="i" closable @close="formData.productLines?.splice(i, 1)">{{ p }}</a-tag>
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="合作开始">
              <a-date-picker v-model:value="formData.cooperationStart" value-format="YYYY-MM-DD" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="合作结束">
              <a-date-picker v-model:value="formData.cooperationEnd" value-format="YYYY-MM-DD" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注">
              <a-textarea v-model:value="formData.remark" :rows="2" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 工程师/评分抽屉 -->
    <a-drawer :open="drawerVisible" :width="860" :title="currentCompany?.companyName" @close="drawerVisible = false">
      <a-tabs v-model:activeKey="drawerTab">
        <a-tab-pane key="engineers" tab="工程师档案">
          <div class="drawer-toolbar">
            <a-button type="primary" size="small" @click="openEngCreate"><template #icon><PlusOutlined /></template>添加工程师</a-button>
          </div>
          <a-table :columns="engineerColumns" :data-source="engineerList" :loading="drawerLoading" row-key="id" size="small" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'skills'">
                <a-tag v-for="(s, i) in (record.skills || []).slice(0, 3)" :key="i" color="blue">{{ s.name }}·{{ skillLevelLabel[s.level] }}</a-tag>
                <span v-if="(record.skills?.length || 0) > 3" class="text-auxiliary">+{{ record.skills.length - 3 }}</span>
              </template>
              <template v-else-if="column.key === 'status'">
                <StatusTag :tone="engineerStatusMap[record.status]?.tone">{{ engineerStatusMap[record.status]?.label || record.status }}</StatusTag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space size="small">
                  <a @click="openEngEdit(record)"><EditOutlined /> 编辑</a>
                  <a v-if="record.status !== 'ACTIVE'" class="success-link" @click="handleEngStatus(record, 'ACTIVE')"><CheckCircleOutlined /> 启用</a>
                  <a v-else class="warning-link" @click="handleEngStatus(record, 'DISABLED')"><StopOutlined /> 停用</a>
                  <a class="danger-link" @click="handleEngDelete(record)"><DeleteOutlined /></a>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="scores" tab="评分记录">
          <a-table :columns="scoreColumns" :data-source="scoreList" row-key="id" size="small" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="['timeliness', 'quality', 'communication', 'issueRate', 'overallScore'].includes(column.key)">
                <span class="tnum">{{ record[column.key] }}</span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-drawer>

    <!-- 新增/编辑工程师 -->
    <a-modal v-model:open="engVisible" :title="engIsEdit ? '编辑工程师' : '添加工程师'" width="560px" :confirm-loading="engLoading" @ok="handleEngSubmit">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="姓名" required>
              <a-input v-model:value="engForm.name" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="电话" required>
              <a-input v-model:value="engForm.phone" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="邮箱">
              <a-input v-model:value="engForm.email" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态">
              <a-select v-model:value="engForm.status">
                <a-select-option v-for="(v, k) in engineerStatusMap" :key="k" :value="k">{{ v.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="技能">
              <a-input-group compact>
                <a-input v-model:value="skillInput.name" placeholder="技能名称" style="width: calc(100% - 120px)" @pressEnter="addSkill" />
                <a-select v-model:value="skillInput.level" style="width: 120px">
                  <a-select-option v-for="(v, k) in skillLevelLabel" :key="k" :value="k">{{ v }}</a-select-option>
                </a-select>
              </a-input-group>
              <a-button type="primary" size="small" style="margin-top: 8px" @click="addSkill">添加技能</a-button>
              <div class="tag-list">
                <a-tag v-for="(s, i) in engForm.skills || []" :key="i" closable color="blue" @close="engForm.skills?.splice(i, 1)">{{ s.name }}·{{ skillLevelLabel[s.level] }}</a-tag>
              </div>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
.danger-link { color: @status-exception; }
.success-link { color: @status-success; }
.warning-link { color: @status-warning; }
.tag-list { margin-top: 8px; }
.drawer-toolbar { margin-bottom: 12px; }
.text-warning { color: @status-warning; }
</style>
