<script setup lang="ts">
/**
 * 集成配置
 * 外部系统连接信息 CRUD + 启用/禁用 + 测试连接
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined,
  ApiOutlined,
  ThunderboltOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageIntegrationConfigs,
  createIntegrationConfig,
  updateIntegrationConfig,
  deleteIntegrationConfig,
  toggleIntegrationConfigEnabled,
  testIntegrationConnection
} from '@/api/integration'
import type {
  IntegrationConfig,
  IntegrationConfigDTO,
  IntegrationConfigQueryParams
} from '@/api/integration'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<IntegrationConfig[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const query = reactive<IntegrationConfigQueryParams>({ keyword: '', adapterType: '', enabled: undefined })

async function loadData() {
  loading.value = true
  try {
    const res = (await pageIntegrationConfigs({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<IntegrationConfig>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[integration.config] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

const adapterOptions = [
  { label: 'REST API', value: 'REST_API' },
  { label: 'Webhook', value: 'WEBHOOK' },
  { label: '数据库', value: 'DATABASE' },
  { label: '消息队列', value: 'MESSAGE_QUEUE' }
]

const authOptions = [
  { label: '无认证', value: 'NONE' },
  { label: 'Basic', value: 'BASIC' },
  { label: 'Bearer Token', value: 'BEARER' },
  { label: 'API Key', value: 'API_KEY' },
  { label: 'OAuth2', value: 'OAUTH2' }
]

const columns = [
  { title: '系统编码', dataIndex: 'systemCode', key: 'systemCode', width: 140 },
  { title: '系统名称', dataIndex: 'systemName', key: 'systemName', width: 160 },
  { title: '适配器类型', dataIndex: 'adapterType', key: 'adapterType', width: 130 },
  { title: '接入点 URL', dataIndex: 'endpointUrl', key: 'endpointUrl', ellipsis: true },
  { title: '认证方式', dataIndex: 'authType', key: 'authType', width: 110 },
  { title: '启用', key: 'enabled', width: 80 },
  { title: '最近调用', dataIndex: 'lastCallTime', key: 'lastCallTime', width: 170 },
  { title: '最近状态', key: 'lastCallStatus', width: 110 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
]

const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<IntegrationConfigDTO>({
  systemCode: '',
  systemName: '',
  adapterType: 'REST_API',
  endpointUrl: '',
  authType: 'NONE',
  authConfig: '',
  timeoutMs: 10000,
  retryCount: 0,
  enabled: 1,
  description: ''
})

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    systemCode: '',
    systemName: '',
    adapterType: 'REST_API',
    endpointUrl: '',
    authType: 'NONE',
    authConfig: '',
    timeoutMs: 10000,
    retryCount: 0,
    enabled: 1,
    description: ''
  })
  formVisible.value = true
}

function openEdit(row: IntegrationConfig) {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    systemCode: row.systemCode,
    systemName: row.systemName,
    adapterType: row.adapterType || 'REST_API',
    endpointUrl: row.endpointUrl,
    authType: row.authType || 'NONE',
    authConfig: row.authConfig || '',
    timeoutMs: row.timeoutMs ?? 10000,
    retryCount: row.retryCount ?? 0,
    enabled: row.enabled,
    description: row.description || ''
  })
  formVisible.value = true
}

async function handleSubmit() {
  if (!formData.systemCode || !formData.systemName || !formData.endpointUrl) {
    message.warning('请填写系统编码、名称和接入点 URL')
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateIntegrationConfig(formData.id, formData)
      message.success('更新成功')
    } else {
      await createIntegrationConfig(formData)
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

function handleDelete(row: IntegrationConfig) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除集成配置「${row.systemName}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteIntegrationConfig(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) { /* ignore */ }
    }
  })
}

async function handleToggleEnabled(row: IntegrationConfig) {
  const next = row.enabled === 1 ? 0 : 1
  try {
    await toggleIntegrationConfigEnabled(row.id, next)
    message.success(next === 1 ? '已启用' : '已禁用')
    loadData()
  } catch (e) { /* ignore */ }
}

async function handleTestConnection(row: IntegrationConfig) {
  try {
    const reachable = (await testIntegrationConnection(row.id)) as unknown as boolean
    if (reachable) {
      message.success(`「${row.systemName}」连接测试成功`)
    } else {
      message.warning(`「${row.systemName}」连接测试失败：endpoint 不可达`)
    }
    loadData()
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="集成配置" description="外部系统连接信息管理（ERP/NMS/IM/物流/OA 等）">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新增配置</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="关键字">
          <a-input v-model:value="query.keyword" placeholder="系统编码/名称" allow-clear style="width: 200px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="适配器类型">
          <a-select v-model:value="query.adapterType" placeholder="全部" allow-clear style="width: 150px" :options="adapterOptions" />
        </a-form-item>
        <a-form-item label="启用">
          <a-select v-model:value="query.enabled" placeholder="全部" allow-clear style="width: 110px">
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
          <template v-if="column.key === 'adapterType'">
            <StatusTag :tone="record.adapterType === 'REST_API' ? 'processing' : 'default'">
              <ApiOutlined /> {{ record.adapterType }}
            </StatusTag>
          </template>
          <template v-else-if="column.key === 'enabled'">
            <a-switch :checked="record.enabled === 1" size="small" @click="handleToggleEnabled(record)" />
          </template>
          <template v-else-if="column.key === 'lastCallStatus'">
            <StatusTag v-if="record.lastCallStatus" :tone="record.lastCallStatus === 'SUCCESS' ? 'success' : 'error'">{{ record.lastCallStatus === 'SUCCESS' ? '成功' : '失败' }}</StatusTag>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a @click="handleTestConnection(record)"><ThunderboltOutlined /> 测试</a>
              <a @click="openEdit(record)"><EditOutlined /> 编辑</a>
              <a class="danger-link" @click="handleDelete(record)"><DeleteOutlined /></a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无集成配置" action-text="新增配置" @action="openCreate" /></template>
      </a-table>
    </div>

    <a-modal v-model:open="formVisible" :title="isEdit ? '编辑集成配置' : '新增集成配置'" width="720px" :confirm-loading="formLoading" @ok="handleSubmit">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="系统编码" required>
              <a-input v-model:value="formData.systemCode" :disabled="isEdit" placeholder="如 ERP / NMS / FEISHU" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="系统名称" required>
              <a-input v-model:value="formData.systemName" placeholder="如 SAP ERP 系统" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="适配器类型">
              <a-select v-model:value="formData.adapterType" :options="adapterOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="认证方式">
              <a-select v-model:value="formData.authType" :options="authOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="接入点 URL" required>
              <a-input v-model:value="formData.endpointUrl" placeholder="如 https://api.example.com/v1" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="认证配置 (JSON)">
              <a-textarea v-model:value="formData.authConfig" :rows="3" placeholder='如 {"token":"xxx","apiKey":"xxx"}' />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="超时 (ms)">
              <a-input-number v-model:value="formData.timeoutMs" :min="1000" :step="1000" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="重试次数">
              <a-input-number v-model:value="formData.retryCount" :min="0" :max="10" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="状态">
              <a-select v-model:value="formData.enabled">
                <a-select-option :value="1">启用</a-select-option>
                <a-select-option :value="0">禁用</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="描述">
              <a-textarea v-model:value="formData.description" :rows="2" placeholder="配置说明" />
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
