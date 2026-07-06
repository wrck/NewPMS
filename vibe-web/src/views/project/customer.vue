<script setup lang="ts">
/**
 * 客户档案管理
 * 客户 CRUD + 联系人子表（表单内嵌）+ 关联项目列表（侧边抽屉）
 * 对应后端：/api/v1/customers
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined,
  EnvironmentOutlined,
  ProjectOutlined,
  ContactsOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  pageCustomers,
  getCustomerDetail,
  createCustomer,
  updateCustomer,
  deleteCustomer
} from '@/api/project'
import { pageProjects } from '@/api/project'
import type { Customer, CustomerDTO, CustomerQueryParams } from '@/types/project'
import type { Project, ProjectStatus } from '@/types/project'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<Customer[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50', '100']
})
const query = reactive<CustomerQueryParams>({ customerName: '', customerCode: '', region: '', industry: '' })

async function loadData() {
  loading.value = true
  try {
    const res = (await pageCustomers({
      ...query,
      page: pagination.current,
      size: pagination.pageSize
    })) as unknown as PageResult<Customer>
    dataSource.value = res?.records || []
    pagination.total = res?.total || 0
  } catch (e) {
    console.error('[project.customer] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  query.customerName = ''
  query.customerCode = ''
  query.region = ''
  query.industry = ''
  handleSearch()
}

function handleTableChange(p: any) {
  pagination.current = p.current || 1
  pagination.pageSize = p.pageSize || 10
  loadData()
}

/* ============ 新增/编辑弹窗 ============ */
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<CustomerDTO>({
  customerName: '',
  customerCode: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  address: '',
  region: '',
  industry: '',
  remark: ''
})

/** 联系人子表（前端本地维护）
 * TODO: 后端尚未提供独立联系人子表接口（/customers/{id}/contacts），
 * 当前仅主联系人通过 contactName/contactPhone/contactEmail 持久化，
 * 额外联系人为前端临时态，提交时仅保存主联系人字段。后端补全后可改为独立 CRUD。
 */
interface ContactRow {
  name: string
  phone: string
  email: string
  isPrimary: boolean
}
const contacts = ref<ContactRow[]>([])

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    customerName: '',
    customerCode: '',
    contactName: '',
    contactPhone: '',
    contactEmail: '',
    address: '',
    region: '',
    industry: '',
    remark: ''
  })
  contacts.value = [{ name: '', phone: '', email: '', isPrimary: true }]
  formVisible.value = true
}

async function openEdit(row: Customer) {
  isEdit.value = true
  try {
    const detail = (await getCustomerDetail(row.id)) as unknown as Customer
    Object.assign(formData, {
      id: detail.id,
      customerName: detail.customerName,
      customerCode: detail.customerCode,
      contactName: detail.contactName || '',
      contactPhone: detail.contactPhone || '',
      contactEmail: detail.contactEmail || '',
      address: detail.address || '',
      region: detail.region || '',
      industry: detail.industry || '',
      remark: detail.remark || ''
    })
  } catch (e) {
    Object.assign(formData, {
      id: row.id,
      customerName: row.customerName,
      customerCode: row.customerCode,
      contactName: row.contactName || '',
      contactPhone: row.contactPhone || '',
      contactEmail: row.contactEmail || '',
      address: row.address || '',
      region: row.region || '',
      industry: row.industry || '',
      remark: row.remark || ''
    })
  }
  // 主联系人回填为第一行
  contacts.value = [
    {
      name: formData.contactName || '',
      phone: formData.contactPhone || '',
      email: formData.contactEmail || '',
      isPrimary: true
    }
  ]
  formVisible.value = true
}

function addContact() {
  contacts.value.push({ name: '', phone: '', email: '', isPrimary: false })
}

function removeContact(idx: number) {
  if (contacts.value[idx]?.isPrimary) {
    message.warning('主联系人不可删除，请直接编辑')
    return
  }
  contacts.value.splice(idx, 1)
}

/** 提交时把主联系人同步到 contactName/contactPhone/contactEmail */
function syncPrimaryContact() {
  const primary = contacts.value.find((c) => c.isPrimary) || contacts.value[0]
  if (primary) {
    formData.contactName = primary.name
    formData.contactPhone = primary.phone
    formData.contactEmail = primary.email
  }
}

async function handleSubmit() {
  if (!formData.customerName?.trim() || !formData.customerCode?.trim()) {
    message.warning('请填写客户编码和名称')
    return
  }
  syncPrimaryContact()
  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateCustomer(formData.id, formData)
      message.success('更新成功')
    } else {
      await createCustomer(formData)
      message.success('创建成功')
    }
    formVisible.value = false
    loadData()
  } catch (e) {
    console.error('[project.customer] submit failed:', e)
  } finally {
    formLoading.value = false
  }
}

function handleDelete(row: Customer) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除客户「${row.customerName}」吗？关联项目将保留但失去客户引用。`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteCustomer(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        console.error('[project.customer] delete failed:', e)
      }
    }
  })
}

/* ============ 关联项目抽屉 ============ */
const projectDrawerVisible = ref(false)
const projectLoading = ref(false)
const currentCustomer = ref<Customer | null>(null)
const projectList = ref<Project[]>([])
const projectPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`
})

const projectStatusMap: Record<ProjectStatus, { tone: any; label: string }> = {
  INIT: { tone: 'archived', label: '立项' },
  PLAN: { tone: 'processing', label: '规划' },
  EXECUTE: { tone: 'processing', label: '执行' },
  ACCEPT: { tone: 'processing', label: '验收' },
  CLOSE: { tone: 'success', label: '结项' },
  ARCHIVED: { tone: 'archived', label: '归档' },
  ON_HOLD: { tone: 'warning', label: '挂起' },
  CANCELLED: { tone: 'error', label: '取消' }
}

const projectColumns = [
  { title: '项目编码', dataIndex: 'projectCode', key: 'projectCode', width: 140 },
  { title: '项目名称', dataIndex: 'projectName', key: 'projectName', ellipsis: true },
  { title: '类型', dataIndex: 'projectType', key: 'projectType', width: 90 },
  { title: '产品线', dataIndex: 'productLine', key: 'productLine', width: 100 },
  { title: '状态', key: 'status', width: 100 },
  { title: '项目经理', dataIndex: 'pmName', key: 'pmName', width: 110 },
  { title: '进度', key: 'progressPct', width: 120 },
  { title: '计划周期', key: 'plannedRange', width: 200 }
]

async function openProjectDrawer(row: Customer) {
  currentCustomer.value = row
  projectDrawerVisible.value = true
  projectPagination.current = 1
  await loadCustomerProjects(row.id)
}

async function loadCustomerProjects(customerId: number) {
  projectLoading.value = true
  try {
    const res = (await pageProjects({
      customerId,
      page: projectPagination.current,
      size: projectPagination.pageSize
    } as any)) as unknown as PageResult<Project>
    projectList.value = res?.records || []
    projectPagination.total = res?.total || 0
  } catch (e) {
    console.error('[project.customer] load projects failed:', e)
    projectList.value = []
    projectPagination.total = 0
  } finally {
    projectLoading.value = false
  }
}

function handleProjectTableChange(p: any) {
  projectPagination.current = p.current || 1
  projectPagination.pageSize = p.pageSize || 10
  if (currentCustomer.value) {
    loadCustomerProjects(currentCustomer.value.id)
  }
}

function formatPlannedRange(record: Project): string {
  const s = record.plannedStart || '—'
  const e = record.plannedEnd || '—'
  return `${s} ~ ${e}`
}

const columns = [
  { title: '客户编码', dataIndex: 'customerCode', key: 'customerCode', width: 140, fixed: 'left' as const },
  { title: '客户名称', dataIndex: 'customerName', key: 'customerName', width: 180, ellipsis: true },
  { title: '主联系人', dataIndex: 'contactName', key: 'contactName', width: 110 },
  { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 140 },
  { title: '区域', dataIndex: 'region', key: 'region', width: 110 },
  { title: '行业', dataIndex: 'industry', key: 'industry', width: 120 },
  { title: '地址', dataIndex: 'address', key: 'address', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const }
]

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="客户档案" description="客户档案管理：主联系人、关联项目查询">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新增客户</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="客户名称">
          <a-input
            v-model:value="query.customerName"
            placeholder="客户名称"
            allow-clear
            style="width: 200px"
            @pressEnter="handleSearch"
          />
        </a-form-item>
        <a-form-item label="客户编码">
          <a-input
            v-model:value="query.customerCode"
            placeholder="如 CUST-001"
            allow-clear
            style="width: 160px"
            @pressEnter="handleSearch"
          />
        </a-form-item>
        <a-form-item label="区域">
          <a-input
            v-model:value="query.region"
            placeholder="如 华北"
            allow-clear
            style="width: 140px"
            @pressEnter="handleSearch"
          >
            <template #prefix><EnvironmentOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item label="行业">
          <a-input
            v-model:value="query.industry"
            placeholder="如 金融"
            allow-clear
            style="width: 140px"
            @pressEnter="handleSearch"
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

    <div class="vibe-card table-card">
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
          <template v-if="column.key === 'region'">
            <a-tag v-if="record.region" color="blue">{{ record.region }}</a-tag>
            <span v-else class="text-auxiliary">—</span>
          </template>
          <template v-else-if="column.key === 'industry'">
            <a-tag v-if="record.industry">{{ record.industry }}</a-tag>
            <span v-else class="text-auxiliary">—</span>
          </template>
          <template v-else-if="column.key === 'contactName'">
            <span v-if="record.contactName">{{ record.contactName }}</span>
            <span v-else class="text-auxiliary">—</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a @click="openProjectDrawer(record)"><ProjectOutlined /> 关联项目</a>
              <a-divider type="vertical" />
              <a @click="openEdit(record)"><EditOutlined /> 编辑</a>
              <a-divider type="vertical" />
              <a class="danger-link" @click="handleDelete(record)"><DeleteOutlined /></a>
            </a-space>
          </template>
        </template>
        <template #emptyText>
          <EmptyState description="暂无客户档案" action-text="新增客户" @action="openCreate" />
        </template>
      </a-table>
    </div>

    <!-- 新增/编辑客户弹窗 -->
    <a-modal
      v-model:open="formVisible"
      :title="isEdit ? '编辑客户' : '新增客户'"
      width="760px"
      :confirm-loading="formLoading"
      :mask-closable="false"
      @ok="handleSubmit"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="客户编码" required>
              <a-input v-model:value="formData.customerCode" :disabled="isEdit" placeholder="如 CUST-001" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="客户名称" required>
              <a-input v-model:value="formData.customerName" placeholder="如 XX 科技有限公司" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="区域">
              <a-input v-model:value="formData.region" placeholder="如 华北" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="行业">
              <a-input v-model:value="formData.industry" placeholder="如 金融 / 制造 / 政企" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="地址">
              <a-input v-model:value="formData.address" placeholder="详细地址" />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 联系人子表 -->
        <div class="sub-section">
          <div class="sub-title">
            <span><ContactsOutlined /> 联系人</span>
            <a-button size="small" type="link" @click="addContact">+ 添加联系人</a-button>
          </div>
          <div class="contact-list-hint">
            主联系人信息将保存到客户档案；额外联系人需后端补全独立接口后持久化。
          </div>
          <div v-for="(c, idx) in contacts" :key="idx" class="sub-row">
            <a-tag v-if="c.isPrimary" color="green" class="primary-tag">主</a-tag>
            <a-input v-model:value="c.name" placeholder="姓名" style="width: 140px" />
            <a-input v-model:value="c.phone" placeholder="电话" style="width: 160px" />
            <a-input v-model:value="c.email" placeholder="邮箱" style="flex: 1" />
            <a-button type="link" danger size="small" @click="removeContact(idx)">
              <DeleteOutlined />
            </a-button>
          </div>
        </div>

        <a-form-item label="备注" style="margin-top: 12px">
          <a-textarea v-model:value="formData.remark" :rows="2" placeholder="备注信息" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 关联项目抽屉 -->
    <a-drawer
      :open="projectDrawerVisible"
      :width="960"
      :title="`关联项目 - ${currentCustomer?.customerName || ''}`"
      @close="projectDrawerVisible = false"
    >
      <a-table
        :columns="projectColumns"
        :data-source="projectList"
        :loading="projectLoading"
        :pagination="projectPagination"
        row-key="id"
        size="small"
        :scroll="{ x: 1100 }"
        @change="handleProjectTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <StatusTag :tone="projectStatusMap[record.status as ProjectStatus]?.tone">
              {{ projectStatusMap[record.status as ProjectStatus]?.label || record.status }}
            </StatusTag>
          </template>
          <template v-else-if="column.key === 'progressPct'">
            <a-progress :percent="record.progressPct || 0" size="small" />
          </template>
          <template v-else-if="column.key === 'plannedRange'">
            <span class="tnum">{{ formatPlannedRange(record) }}</span>
          </template>
        </template>
        <template #emptyText>
          <EmptyState description="该客户暂无关联项目" />
        </template>
      </a-table>
    </a-drawer>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
.danger-link { color: @status-exception; }
.text-auxiliary { color: @text-tertiary; }
.sub-section {
  margin-top: 8px;
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
.contact-list-hint {
  margin-bottom: 8px;
  font-size: 12px;
  color: @text-tertiary;
}
.sub-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.primary-tag {
  flex-shrink: 0;
}
</style>
