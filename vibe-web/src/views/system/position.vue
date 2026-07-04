<script setup lang="ts">
/**
 * 岗位管理
 * 岗位 CRUD（所属组织 + 岗位编码 + 名称 + 排序 + 状态）
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pagePositions,
  createPosition,
  updatePosition,
  deletePosition,
  listOrgTree
} from '@/api/system'
import type { SysPosition, SysPositionDTO, SysPositionQueryParams, SysOrg } from '@/api/system'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<SysPosition[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const query = reactive<SysPositionQueryParams>({ keyword: '', orgId: undefined })

// 组织树用于 select 选项
const orgTree = ref<SysOrg[]>([])
const orgOptions = ref<{ label: string; value: number }[]>([])

function flattenOrg(list: SysOrg[]) {
  const out: { label: string; value: number }[] = []
  const walk = (nodes: SysOrg[], prefix: string) => {
    nodes.forEach(n => {
      out.push({ label: prefix + n.orgName, value: n.id })
      if (n.children && n.children.length) walk(n.children, prefix + ' / ')
    })
  }
  walk(list, '')
  return out
}

async function loadOrgTree() {
  try {
    const tree = (await listOrgTree()) as unknown as SysOrg[]
    orgTree.value = tree || []
    orgOptions.value = flattenOrg(tree || [])
  } catch (e) {
    console.error('[system.position] load org tree failed:', e)
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = (await pagePositions({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<SysPosition>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[system.position] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

const columns = [
  { title: '岗位编码', dataIndex: 'positionCode', key: 'positionCode', width: 160 },
  { title: '岗位名称', dataIndex: 'positionName', key: 'positionName', width: 180 },
  { title: '所属组织', dataIndex: 'orgName', key: 'orgName', width: 180 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '状态', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<SysPositionDTO>({
  positionName: '',
  positionCode: '',
  orgId: undefined,
  sortOrder: 0,
  status: 1
})

function openCreate() {
  isEdit.value = false
  Object.assign(formData, { id: undefined, positionName: '', positionCode: '', orgId: undefined, sortOrder: 0, status: 1 })
  formVisible.value = true
}

function openEdit(row: SysPosition) {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    positionName: row.positionName,
    positionCode: row.positionCode,
    orgId: row.orgId,
    sortOrder: row.sortOrder ?? 0,
    status: row.status
  })
  formVisible.value = true
}

async function handleSubmit() {
  if (!formData.positionName || !formData.positionCode) {
    message.warning('请填写岗位编码和名称')
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updatePosition(formData.id, formData)
      message.success('更新成功')
    } else {
      await createPosition(formData)
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

function handleDelete(row: SysPosition) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除岗位「${row.positionName}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deletePosition(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) { /* ignore */ }
    }
  })
}

onMounted(() => {
  loadOrgTree()
  loadData()
})
</script>

<template>
  <PageContainer title="岗位管理" description="岗位编码 / 名称 / 所属组织维护">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新增岗位</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="关键字">
          <a-input v-model:value="query.keyword" placeholder="岗位编码/名称" allow-clear style="width: 200px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="所属组织">
          <a-select v-model:value="query.orgId" placeholder="全部" allow-clear style="width: 200px" :options="orgOptions" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="vibe-card table-card">
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1100 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <StatusTag :tone="record.status === 1 ? 'success' : 'archived'">{{ record.status === 1 ? '启用' : '禁用' }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a @click="openEdit(record)"><EditOutlined /> 编辑</a>
              <a class="danger-link" @click="handleDelete(record)"><DeleteOutlined /></a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无岗位" action-text="新增岗位" @action="openCreate" /></template>
      </a-table>
    </div>

    <a-modal v-model:open="formVisible" :title="isEdit ? '编辑岗位' : '新增岗位'" width="560px" :confirm-loading="formLoading" @ok="handleSubmit">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="岗位编码" required>
              <a-input v-model:value="formData.positionCode" :disabled="isEdit" placeholder="如 FIELD_ENGINEER" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="岗位名称" required>
              <a-input v-model:value="formData.positionName" placeholder="如 现场工程师" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="所属组织">
              <a-select v-model:value="formData.orgId" placeholder="请选择组织" allow-clear :options="orgOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="排序">
              <a-input-number v-model:value="formData.sortOrder" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="状态">
              <a-select v-model:value="formData.status">
                <a-select-option :value="1">启用</a-select-option>
                <a-select-option :value="0">禁用</a-select-option>
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
