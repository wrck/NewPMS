<script setup lang="ts">
/**
 * 组织架构
 * 树形组织架构维护（增删改查）
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
  listOrgTree,
  createOrg,
  updateOrg,
  deleteOrg
} from '@/api/system'
import type { SysOrg } from '@/api/system'

const loading = ref(false)
const treeData = ref<SysOrg[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = (await listOrgTree()) as unknown as SysOrg[]
    treeData.value = res || []
  } catch (e) {
    console.error('[system.org] load failed:', e)
  } finally {
    loading.value = false
  }
}

const columns = [
  { title: '组织名称', dataIndex: 'orgName', key: 'orgName' },
  { title: '组织编码', dataIndex: 'orgCode', key: 'orgCode', width: 180 },
  { title: '负责人', dataIndex: 'leaderName', key: 'leaderName', width: 120 },
  { title: '联系电话', dataIndex: 'phone', key: 'phone', width: 140 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]

/* ============ 表单弹窗 ============ */
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<Partial<SysOrg>>({
  orgCode: '',
  orgName: '',
  parentId: undefined,
  sort: 0,
  leaderId: undefined,
  leaderName: '',
  phone: '',
  status: 1
})

// 父节点选项（扁平化树）
const parentOptions = ref<{ label: string; value: number }[]>([])

function flattenOrg(list: SysOrg[], prefix = ''): { label: string; value: number }[] {
  const out: { label: string; value: number }[] = []
  list.forEach(n => {
    out.push({ label: prefix + n.orgName, value: n.id })
    if (n.children && n.children.length) {
      out.push(...flattenOrg(n.children, prefix + ' / '))
    }
  })
  return out
}

function openCreate(parent?: SysOrg) {
  isEdit.value = false
  parentOptions.value = flattenOrg(treeData.value)
  Object.assign(formData, {
    id: undefined,
    orgCode: '',
    orgName: '',
    parentId: parent?.id,
    sort: 0,
    leaderId: undefined,
    leaderName: '',
    phone: '',
    status: 1
  })
  formVisible.value = true
}

function openEdit(row: SysOrg) {
  isEdit.value = true
  // 编辑时排除自身及其子树作为可选父节点
  const all = flattenOrg(treeData.value)
  parentOptions.value = all
  Object.assign(formData, {
    id: row.id,
    orgCode: row.orgCode,
    orgName: row.orgName,
    parentId: row.parentId,
    sort: row.sort,
    leaderId: row.leaderId,
    leaderName: row.leaderName,
    phone: row.phone,
    status: row.status
  })
  formVisible.value = true
}

async function handleSubmit() {
  if (!formData.orgCode || !formData.orgName) {
    message.warning('请填写组织编码和名称')
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateOrg(formData.id, formData)
      message.success('更新成功')
    } else {
      await createOrg(formData)
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

function handleDelete(row: SysOrg) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除组织「${row.orgName}」吗？子组织将一并删除。`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteOrg(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) { /* ignore */ }
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="组织架构" description="树形组织架构维护（公司 / 部门 / 小组）">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate()"><template #icon><PlusOutlined /></template>新增组织</a-button>
    </template>

    <div class="vibe-card table-card">
      <a-table
        :columns="columns"
        :data-source="treeData"
        :loading="loading"
        :pagination="false"
        row-key="id"
        :scroll="{ x: 1100 }"
        :default-expand-all-rows="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'orgName'">
            <ApartmentOutlined /> <span>{{ record.orgName }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="record.status === 1 ? 'success' : 'archived'">{{ record.status === 1 ? '启用' : '禁用' }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a @click="openCreate(record)"><PlusOutlined /> 子级</a>
              <a @click="openEdit(record)"><EditOutlined /> 编辑</a>
              <a class="danger-link" @click="handleDelete(record)"><DeleteOutlined /></a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无组织数据" action-text="新增组织" @action="openCreate()" /></template>
      </a-table>
    </div>

    <a-modal v-model:open="formVisible" :title="isEdit ? '编辑组织' : '新增组织'" width="600px" :confirm-loading="formLoading" @ok="handleSubmit">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="组织编码" required>
              <a-input v-model:value="formData.orgCode" :disabled="isEdit" placeholder="如 TECH_DEPT" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="组织名称" required>
              <a-input v-model:value="formData.orgName" placeholder="如 技术部" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="上级组织">
              <a-select v-model:value="formData.parentId" placeholder="顶级组织" allow-clear :options="parentOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="排序">
              <a-input-number v-model:value="formData.sort" :min="0" style="width: 100%" />
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
          <a-col :span="12">
            <a-form-item label="负责人">
              <a-input v-model:value="formData.leaderName" placeholder="负责人姓名" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系电话">
              <a-input v-model:value="formData.phone" placeholder="联系电话" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.table-card { padding: 0; }
.danger-link { color: @status-exception; }
</style>
