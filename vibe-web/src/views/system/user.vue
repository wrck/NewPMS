<script setup lang="ts">
/**
 * 用户管理
 * 用户 CRUD、角色分配、状态变更、密码重置
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined,
  KeyOutlined,
  SafetyOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageUsers,
  createUser,
  updateUser,
  deleteUser,
  changeUserStatus,
  resetUserPassword,
  assignUserRoles,
  listRoles
} from '@/api/system'
import type { SysUser, SysUserDTO, SysUserQueryParams, SysRole } from '@/api/system'
import type { RoleCode } from '@/types/user'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<SysUser[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const query = reactive<SysUserQueryParams>({ userName: '', realName: '', phone: '', orgId: undefined, status: undefined, roleCode: undefined })

async function loadData() {
  loading.value = true
  try {
    const res = (await pageUsers({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<SysUser>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[system.user] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
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
const roleColor: Record<RoleCode, string> = {
  SUPER_ADMIN: 'magenta',
  DIRECTOR: 'red',
  PM: 'orange',
  ENGINEER: 'blue',
  AGENT_ADMIN: 'purple',
  AGENT_ENGINEER: 'purple',
  FINANCE: 'gold',
  CUSTOMER: 'cyan'
}

const columns = [
  { title: '用户名', dataIndex: 'userName', key: 'userName', width: 130 },
  { title: '姓名', dataIndex: 'realName', key: 'realName', width: 110 },
  { title: '手机', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 180, ellipsis: true },
  { title: '组织', dataIndex: 'orgName', key: 'orgName', width: 130, ellipsis: true },
  { title: '角色', key: 'roles', width: 200 },
  { title: '状态', key: 'status', width: 90 },
  { title: '最后登录', dataIndex: 'lastLoginAt', key: 'lastLoginAt', width: 160 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' }
]

// 用户弹窗
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<SysUserDTO>({
  userName: '',
  realName: '',
  email: '',
  phone: '',
  orgId: undefined,
  status: 1,
  roleCodes: [],
  password: ''
})
const roleOptions = ref<SysRole[]>([])

async function loadRoles() {
  try {
    roleOptions.value = (await listRoles()) as unknown as SysRole[]
  } catch (e) {
    console.error('[system.user] load roles failed:', e)
  }
}

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    userName: '',
    realName: '',
    email: '',
    phone: '',
    orgId: undefined,
    status: 1,
    roleCodes: [],
    password: ''
  })
  formVisible.value = true
}

function openEdit(row: SysUser) {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    userName: row.userName,
    realName: row.realName,
    email: row.email,
    phone: row.phone,
    orgId: row.orgId,
    status: row.status,
    roleCodes: [...(row.roles || [])],
    password: ''
  })
  formVisible.value = true
}

async function handleSubmit() {
  if (!formData.userName || !formData.realName) {
    message.warning('请填写用户名和姓名')
    return
  }
  if (!isEdit.value && !formData.password) {
    message.warning('请设置初始密码')
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateUser(formData.id, formData)
      message.success('更新成功')
    } else {
      await createUser(formData)
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

function handleDelete(row: SysUser) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除用户「${row.realName}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteUser(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) { /* ignore */ }
    }
  })
}

function handleChangeStatus(row: SysUser) {
  const next = row.status === 1 ? 0 : 1
  Modal.confirm({
    title: '变更状态',
    content: `确定将用户「${row.realName}」${next === 1 ? '启用' : '禁用'}吗？`,
    async onOk() {
      try {
        await changeUserStatus(row.id, next)
        message.success('状态已变更')
        loadData()
      } catch (e) { /* ignore */ }
    }
  })
}

// 角色分配
const roleVisible = ref(false)
const roleLoading = ref(false)
const roleRow = ref<SysUser | null>(null)
const selectedRoles = ref<RoleCode[]>([])

function openAssignRoles(row: SysUser) {
  roleRow.value = row
  selectedRoles.value = [...(row.roles || [])]
  roleVisible.value = true
}

async function handleAssignRoles() {
  if (!roleRow.value) return
  roleLoading.value = true
  try {
    await assignUserRoles(roleRow.value.id, selectedRoles.value)
    message.success('角色已分配')
    roleVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    roleLoading.value = false
  }
}

// 重置密码
const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdRow = ref<SysUser | null>(null)
const newPassword = ref('')

function openResetPwd(row: SysUser) {
  pwdRow.value = row
  newPassword.value = ''
  pwdVisible.value = true
}

async function handleResetPwd() {
  if (!pwdRow.value || !newPassword.value) {
    message.warning('请输入新密码')
    return
  }
  if (newPassword.value.length < 6) {
    message.warning('密码至少 6 位')
    return
  }
  pwdLoading.value = true
  try {
    await resetUserPassword(pwdRow.value.id, newPassword.value)
    message.success('密码已重置')
    pwdVisible.value = false
  } catch (e) {
    // ignore
  } finally {
    pwdLoading.value = false
  }
}

onMounted(() => {
  loadData()
  loadRoles()
})
</script>

<template>
  <PageContainer title="用户管理" description="系统用户、角色分配、状态与密码管理">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新增用户</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="用户名">
          <a-input v-model:value="query.userName" placeholder="用户名" allow-clear style="width: 150px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="姓名">
          <a-input v-model:value="query.realName" placeholder="姓名" allow-clear style="width: 150px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="手机">
          <a-input v-model:value="query.phone" placeholder="手机号" allow-clear style="width: 150px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="角色">
          <a-select v-model:value="query.roleCode" placeholder="全部" allow-clear style="width: 150px">
            <a-select-option v-for="r in roleOptions" :key="r.roleCode" :value="r.roleCode">{{ r.roleName }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 110px">
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
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1500 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'roles'">
            <a-tag v-for="r in record.roles || []" :key="r" :color="roleColor[r as RoleCode]">{{ roleLabel[r as RoleCode] || r }}</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="record.status === 1 ? 'success' : 'archived'">{{ record.status === 1 ? '启用' : '禁用' }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a @click="openEdit(record)"><EditOutlined /> 编辑</a>
              <a @click="openAssignRoles(record)"><SafetyOutlined /> 角色</a>
              <a @click="openResetPwd(record)"><KeyOutlined /> 密码</a>
              <a-divider type="vertical" />
              <a @click="handleChangeStatus(record)">{{ record.status === 1 ? '禁用' : '启用' }}</a>
              <a class="danger-link" @click="handleDelete(record)"><DeleteOutlined /></a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无用户" action-text="新增用户" @action="openCreate" /></template>
      </a-table>
    </div>

    <!-- 新增/编辑用户 -->
    <a-modal v-model:open="formVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="640px" :confirm-loading="formLoading" @ok="handleSubmit">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="用户名" required>
              <a-input v-model:value="formData.userName" :disabled="isEdit" placeholder="登录用户名" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="姓名" required>
              <a-input v-model:value="formData.realName" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="手机">
              <a-input v-model:value="formData.phone" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="邮箱">
              <a-input v-model:value="formData.email" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="组织 ID">
              <a-input-number v-model:value="formData.orgId" style="width: 100%" />
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
            <a-form-item label="角色">
              <a-select v-model:value="formData.roleCodes" mode="multiple" placeholder="选择角色">
                <a-select-option v-for="r in roleOptions" :key="r.roleCode" :value="r.roleCode">{{ r.roleName }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="24" v-if="!isEdit">
            <a-form-item label="初始密码" required>
              <a-input-password v-model:value="formData.password" placeholder="至少 6 位" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 角色分配 -->
    <a-modal v-model:open="roleVisible" title="分配角色" :confirm-loading="roleLoading" @ok="handleAssignRoles">
      <p class="text-auxiliary">为「{{ roleRow?.realName }}」分配角色：</p>
      <a-checkbox-group v-model:value="selectedRoles" style="display: flex; flex-direction: column; gap: 8px">
        <a-checkbox v-for="r in roleOptions" :key="r.roleCode" :value="r.roleCode" :disabled="r.status === 0">
          <a-tag :color="roleColor[r.roleCode as RoleCode]">{{ r.roleName }}</a-tag>
          <span class="text-auxiliary">{{ r.description }}</span>
        </a-checkbox>
      </a-checkbox-group>
    </a-modal>

    <!-- 重置密码 -->
    <a-modal v-model:open="pwdVisible" title="重置密码" :confirm-loading="pwdLoading" @ok="handleResetPwd">
      <a-alert :message="`将为用户「${pwdRow?.realName}」重置密码`" type="info" show-icon style="margin-bottom: 12px" />
      <a-form layout="vertical">
        <a-form-item label="新密码" required>
          <a-input-password v-model:value="newPassword" placeholder="至少 6 位" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
.danger-link { color: @status-exception; }
.text-auxiliary { color: @text-auxiliary; }
</style>
