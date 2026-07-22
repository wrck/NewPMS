<script setup lang="ts">
/**
 * 系统配置
 * 参数配置 CRUD
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
  pageConfigs,
  createConfig,
  updateConfig,
  deleteConfig
} from '@/api/system'
import type { SysConfig } from '@/api/system'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<SysConfig[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const query = reactive({ keyword: '' })

async function loadData() {
  loading.value = true
  try {
    const res = (await pageConfigs({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<SysConfig>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[system.config] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

const columns = [
  { title: '参数键名', dataIndex: 'configKey', key: 'configKey', width: 220 },
  { title: '参数名称', dataIndex: 'configName', key: 'configName', width: 180 },
  { title: '参数值', dataIndex: 'configValue', key: 'configValue', ellipsis: true },
  { title: '类型', dataIndex: 'configType', key: 'configType', width: 100 },
  { title: '状态', key: 'status', width: 90 },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 170 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<Partial<SysConfig>>({
  configKey: '',
  configValue: '',
  configName: '',
  configType: '',
  description: '',
  status: 1
})

function openCreate() {
  isEdit.value = false
  Object.assign(formData, { id: undefined, configKey: '', configValue: '', configName: '', configType: '', description: '', status: 1 })
  formVisible.value = true
}

function openEdit(row: SysConfig) {
  isEdit.value = true
  Object.assign(formData, { ...row })
  formVisible.value = true
}

async function handleSubmit() {
  if (!formData.configKey || !formData.configName || formData.configValue == null) {
    message.warning('请填写参数键名、名称和值')
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateConfig(formData.id, formData)
      message.success('更新成功')
    } else {
      await createConfig(formData)
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

function handleDelete(row: SysConfig) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除参数「${row.configName}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteConfig(row.id)
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
  <PageContainer title="系统配置" description="系统参数配置管理">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新增参数</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="关键字">
          <a-input v-model:value="query.keyword" placeholder="参数键名/名称" allow-clear style="width: 240px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="vibe-card table-card">
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1200 }">
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
        <template #emptyText><EmptyState description="暂无配置" action-text="新增参数" @action="openCreate" /></template>
      </a-table>
    </div>

    <a-modal v-model:open="formVisible" :title="isEdit ? '编辑参数' : '新增参数'" width="560px" :confirm-loading="formLoading" @ok="handleSubmit">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="参数键名" required>
              <a-input v-model:value="formData.configKey" :disabled="isEdit" placeholder="如 sys.title" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="参数名称" required>
              <a-input v-model:value="formData.configName" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="参数值" required>
              <a-textarea v-model:value="formData.configValue" :rows="2" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="类型">
              <a-input v-model:value="formData.configType" placeholder="如 string/number/json" />
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
            <a-form-item label="描述">
              <a-textarea v-model:value="formData.description" :rows="2" />
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
