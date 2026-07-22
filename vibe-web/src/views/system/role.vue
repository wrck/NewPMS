<script setup lang="ts">
/**
 * 角色权限
 * 角色 CRUD、权限分配、数据范围
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined,
  SafetyOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  listRoles,
  createRole,
  updateRole,
  deleteRole,
  getRolePermissions,
  assignRolePermissions
} from '@/api/system'
import type { SysRole, SysRoleDTO } from '@/api/system'
import type { RoleCode } from '@/types/user'

const loading = ref(false)
const dataSource = ref<SysRole[]>([])
const query = reactive({ roleName: '', status: undefined as 1 | 0 | undefined })

async function loadData() {
  loading.value = true
  try {
    const all = (await listRoles()) as unknown as SysRole[]
    let list = all || []
    if (query.roleName) {
      const kw = query.roleName.toLowerCase()
      list = list.filter((r) => r.roleName?.toLowerCase().includes(kw))
    }
    if (query.status !== undefined) {
      list = list.filter((r) => r.status === query.status)
    }
    dataSource.value = list
  } catch (e) {
    console.error('[system.role] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

const dataScopeLabel: Record<string, string> = {
  ALL: '全部数据',
  DEPT: '本部门',
  SELF: '仅本人',
  CUSTOM: '自定义'
}
const dataScopeTone: Record<string, any> = {
  ALL: 'error', DEPT: 'warning', SELF: 'default', CUSTOM: 'processing'
}

const roleLabel: Record<RoleCode, string> = {
  SUPER_ADMIN: '超级管理员',
  DIRECTOR: '总监',
  PM: '项目经理',
  ENGINEER: '工程师',
  AGENT_ADMIN: '代理商管理员',
  AGENT_ENGINEER: '代理商工程师',
  FINANCE: '财务',
  CUSTOMER: '客户'
}

const columns = [
  { title: '角色编码', dataIndex: 'roleCode', key: 'roleCode', width: 160 },
  { title: '角色名称', dataIndex: 'roleName', key: 'roleName', width: 140 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '数据范围', key: 'dataScope', width: 120 },
  { title: '用户数', dataIndex: 'userCount', key: 'userCount', width: 90 },
  { title: '状态', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
]

// 角色弹窗
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<SysRoleDTO>({
  roleCode: undefined,
  roleName: '',
  description: '',
  dataScope: 'SELF',
  status: 1,
  permissionCodes: [],
  customOrgIds: []
})

// 角色表单校验规则（异常处理三层闭环 SubTask 8.4 补充）
const roleFormRules = {
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { max: 64, message: '角色名称长度不能超过 64', trigger: 'blur' }
  ],
  description: [
    { max: 255, message: '描述长度不能超过 255', trigger: 'blur' }
  ]
}
const roleFormRef = ref()

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    roleCode: undefined,
    roleName: '',
    description: '',
    dataScope: 'SELF',
    status: 1,
    permissionCodes: [],
    customOrgIds: []
  })
  formVisible.value = true
}

function openEdit(row: SysRole) {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    roleCode: row.roleCode,
    roleName: row.roleName,
    description: row.description,
    dataScope: row.dataScope,
    status: row.status,
    permissionCodes: row.permissions ? [...row.permissions] : [],
    customOrgIds: []
  })
  formVisible.value = true
}

async function handleSubmit() {
  // 异常处理三层闭环：先校验表单，再调用后端
  try {
    await roleFormRef.value?.validate()
  } catch {
    return
  }
  if (!formData.roleName) {
    message.warning('请填写角色名称')
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateRole(formData.id, formData)
      message.success('更新成功')
    } else {
      await createRole(formData)
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

function handleDelete(row: SysRole) {
  if (row.roleCode === 'SUPER_ADMIN') {
    message.warning('超级管理员角色不可删除')
    return
  }
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除角色「${row.roleName}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteRole(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) { /* ignore */ }
    }
  })
}

// 权限分配
const permVisible = ref(false)
const permLoading = ref(false)
const permRow = ref<SysRole | null>(null)
const permTree = ref<any[]>([])
const checkedKeys = ref<string[]>([])
const expandedKeys = ref<string[]>([])

async function openPermissions(row: SysRole) {
  permRow.value = row
  permVisible.value = true
  permLoading.value = true
  try {
    const menuIds = (await getRolePermissions(row.id)) as unknown as string[]
    checkedKeys.value = menuIds || []
    permTree.value = []
    expandedKeys.value = []
  } catch (e) {
    console.error('[system.role] load permissions failed:', e)
  } finally {
    permLoading.value = false
  }
}

async function handleAssignPermissions() {
  if (!permRow.value) return
  permLoading.value = true
  try {
    await assignRolePermissions(permRow.value.id, checkedKeys.value)
    message.success('权限已更新')
    permVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    permLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="角色权限" description="角色管理、权限分配、数据范围">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新增角色</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="角色名称">
          <a-input v-model:value="query.roleName" placeholder="角色名称" allow-clear style="width: 180px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 120px">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="vibe-card table-card">
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 1300 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'dataScope'">
            <StatusTag :tone="dataScopeTone[record.dataScope]">{{ dataScopeLabel[record.dataScope] || record.dataScope }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'userCount'">
            {{ record.userCount ?? '-' }}
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="record.status === 1 ? 'success' : 'archived'">{{ record.status === 1 ? '启用' : '禁用' }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a @click="openPermissions(record)"><SafetyOutlined /> 权限</a>
              <a @click="openEdit(record)"><EditOutlined /> 编辑</a>
              <a class="danger-link" @click="handleDelete(record)"><DeleteOutlined /></a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无角色" action-text="新增角色" @action="openCreate" /></template>
      </a-table>
    </div>

    <!-- 新增/编辑角色 -->
    <a-modal v-model:open="formVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="560px" :confirm-loading="formLoading" @ok="handleSubmit">
      <a-form ref="roleFormRef" layout="vertical" :model="formData" :rules="roleFormRules">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="角色编码">
              <a-select v-model:value="formData.roleCode" :disabled="isEdit" placeholder="选择角色编码" allow-clear>
                <a-select-option v-for="(v, k) in roleLabel" :key="k" :value="k">{{ v }}（{{ k }}）</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="角色名称" name="roleName" required>
              <a-input v-model:value="formData.roleName" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="数据范围">
              <a-select v-model:value="formData.dataScope">
                <a-select-option v-for="(v, k) in dataScopeLabel" :key="k" :value="k">{{ v }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态">
              <a-select v-model:value="formData.status">
                <a-select-option :value="1">启用</a-select-option>
                <a-select-option :value="0">禁用</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="描述" name="description">
              <a-textarea v-model:value="formData.description" :rows="2" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 权限分配 -->
    <a-modal v-model:open="permVisible" :title="`权限分配 - ${permRow?.roleName || ''}`" width="600px" :confirm-loading="permLoading" @ok="handleAssignPermissions">
      <a-spin :spinning="permLoading">
        <EmptyState v-if="!permTree.length" description="暂无权限数据" size="compact" />
        <a-tree
          v-else
          v-model:checked-keys="checkedKeys"
          v-model:expanded-keys="expandedKeys"
          :tree-data="permTree"
          checkable
          :field-names="{ title: 'title', key: 'key', children: 'children' }"
          :height="400"
          :check-strictly="false"
        />
      </a-spin>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
.danger-link { color: @status-exception; }
</style>
