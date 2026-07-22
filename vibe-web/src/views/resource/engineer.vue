<script setup lang="ts">
/**
 * 工程师资源池
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import { pageEngineers, createEngineer, updateEngineer, deleteEngineer } from '@/api/resource'
import { listOrgTree } from '@/api/system'
import type { SysOrg } from '@/api/system'
import type { Engineer, EngineerDTO, EngineerQueryParams, SkillLevel } from '@/types/resource'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<Engineer[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const query = reactive<EngineerQueryParams>({ keyword: '', region: '', status: undefined })

// 组织树（所属组织下拉用）
const orgTree = ref<SysOrg[]>([])

async function loadOrgTree() {
  try {
    const tree = (await listOrgTree()) as unknown as SysOrg[]
    orgTree.value = tree || []
  } catch (e) {
    console.error('[resource.engineer] load org tree failed:', e)
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = (await pageEngineers({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<Engineer>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[resource.engineer] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

// 弹窗
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<EngineerDTO>({
  engineerNo: '',
  name: '',
  phone: '',
  email: '',
  orgId: undefined,
  region: '',
  status: 'ACTIVE',
  skills: [],
  certifications: []
})

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    engineerNo: '',
    name: '',
    phone: '',
    email: '',
    orgId: undefined,
    region: '',
    status: 'ACTIVE',
    skills: [],
    certifications: []
  })
  formVisible.value = true
}

function openEdit(row: Engineer) {
  isEdit.value = true
  Object.assign(formData, row)
  formVisible.value = true
}

async function handleSubmit() {
  if (!formData.engineerNo || !formData.name) {
    message.warning('请填写工号和姓名')
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateEngineer(formData.id, formData)
      message.success('更新成功')
    } else {
      await createEngineer(formData)
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

function handleDelete(row: Engineer) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除工程师「${row.name}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteEngineer(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) { /* ignore */ }
    }
  })
}

const statusMap: Record<string, { tone: any; label: string }> = {
  ACTIVE: { tone: 'success', label: '在职' },
  ON_LEAVE: { tone: 'warning', label: '休假' },
  RESIGNED: { tone: 'archived', label: '离职' }
}

const skillLevelLabel: Record<SkillLevel, string> = {
  JUNIOR: '初级',
  INTERMEDIATE: '中级',
  SENIOR: '高级',
  EXPERT: '专家'
}

const columns = [
  { title: '工号', dataIndex: 'engineerNo', key: 'engineerNo', width: 110 },
  { title: '姓名', dataIndex: 'name', key: 'name', width: 100 },
  { title: '区域', dataIndex: 'region', key: 'region', width: 100 },
  { title: '所属组织', dataIndex: 'orgName', key: 'orgName', width: 140, ellipsis: true },
  { title: '技能', key: 'skills', ellipsis: true },
  { title: '利用率', key: 'utilization', width: 140 },
  { title: '进行中任务', dataIndex: 'ongoingTaskCount', key: 'ongoingTaskCount', width: 100 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

onMounted(() => {
  loadData()
  loadOrgTree()
})
</script>

<template>
  <PageContainer title="工程师资源池" description="工程师档案、技能、负荷管理">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新增工程师</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="关键字">
          <a-input v-model:value="query.name" placeholder="姓名/工号" allow-clear style="width: 200px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="区域">
          <a-input v-model:value="query.region" placeholder="如华北" allow-clear style="width: 130px" @pressEnter="handleSearch" />
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
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1100 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'skills'">
            <a-tag v-for="(s, i) in (record.skills || []).slice(0, 3)" :key="i" color="blue">{{ s.name }}·{{ skillLevelLabel[s.level as SkillLevel] }}</a-tag>
            <span v-if="(record.skills?.length || 0) > 3" class="text-auxiliary">+{{ record.skills.length - 3 }}</span>
          </template>
          <template v-else-if="column.key === 'utilization'">
            <ProgressBar :percent="record.utilization || 0" />
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="statusMap[record.status]?.tone">{{ statusMap[record.status]?.label || record.status }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a @click="openEdit(record)"><EditOutlined /> 编辑</a>
              <a-divider type="vertical" />
              <a class="danger-link" @click="handleDelete(record)"><DeleteOutlined /></a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无工程师" action-text="新增工程师" @action="openCreate" /></template>
      </a-table>
    </div>

    <a-modal v-model:open="formVisible" :title="isEdit ? '编辑工程师' : '新增工程师'" width="600px" :confirm-loading="formLoading" @ok="handleSubmit">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="工号" required>
              <a-input v-model:value="formData.engineerNo" :disabled="isEdit" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="姓名" required>
              <a-input v-model:value="formData.name" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="手机号">
              <a-input v-model:value="formData.phone" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="邮箱">
              <a-input v-model:value="formData.email" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="区域">
              <a-input v-model:value="formData.region" placeholder="如华北" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="所属组织">
              <a-tree-select
                v-model:value="formData.orgId"
                style="width: 100%"
                :tree-data="orgTree"
                :field-names="{ label: 'orgName', value: 'id', children: 'children' }"
                tree-default-expand-all
                allow-clear
                show-search
                tree-node-filter-prop="orgName"
                placeholder="请选择组织"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态">
              <a-select v-model:value="formData.status">
                <a-select-option v-for="(v, k) in statusMap" :key="k" :value="k">{{ v.label }}</a-select-option>
              </a-select>
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
</style>
