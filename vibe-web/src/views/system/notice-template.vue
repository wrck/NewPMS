<script setup lang="ts">
/**
 * 通知模板
 * 模板编码 / 名称 / 标题模板 / 内容模板 / 触达渠道 / 接收人类型
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
  pageNoticeTemplates,
  createNoticeTemplate,
  updateNoticeTemplate,
  deleteNoticeTemplate
} from '@/api/system'
import type {
  SysNoticeTemplate,
  SysNoticeTemplateDTO,
  SysNoticeTemplateQueryParams
} from '@/api/system'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<SysNoticeTemplate[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const query = reactive<SysNoticeTemplateQueryParams>({ keyword: '', recipientType: '', status: undefined })

async function loadData() {
  loading.value = true
  try {
    const res = (await pageNoticeTemplates({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<SysNoticeTemplate>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[system.notice-template] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

const recipientOptions = [
  { label: '工程师', value: 'ENGINEER' },
  { label: '项目经理', value: 'PM' },
  { label: '代理商', value: 'AGENT' },
  { label: '客户', value: 'CUSTOMER' },
  { label: '管理者', value: 'MANAGER' }
]

const channelOptions = [
  { label: '站内信', value: 'SITE' },
  { label: '邮件', value: 'EMAIL' },
  { label: '短信', value: 'SMS' },
  { label: '钉钉', value: 'DINGTALK' },
  { label: '飞书', value: 'FEISHU' }
]

const columns = [
  { title: '模板编码', dataIndex: 'templateCode', key: 'templateCode', width: 160 },
  { title: '模板名称', dataIndex: 'templateName', key: 'templateName', width: 180 },
  { title: '接收人类型', dataIndex: 'recipientType', key: 'recipientType', width: 120 },
  { title: '触达渠道', dataIndex: 'channels', key: 'channels', width: 200, ellipsis: true },
  { title: '状态', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<SysNoticeTemplateDTO>({
  templateCode: '',
  templateName: '',
  titleTemplate: '',
  contentTemplate: '',
  channels: '["SITE"]',
  recipientType: '',
  status: 1
})

const selectedChannels = ref<string[]>(['SITE'])

function openCreate() {
  isEdit.value = false
  selectedChannels.value = ['SITE']
  Object.assign(formData, {
    id: undefined,
    templateCode: '',
    templateName: '',
    titleTemplate: '',
    contentTemplate: '',
    channels: '["SITE"]',
    recipientType: '',
    status: 1
  })
  formVisible.value = true
}

function openEdit(row: SysNoticeTemplate) {
  isEdit.value = true
  let channels: string[] = []
  try {
    channels = row.channels ? JSON.parse(row.channels) : []
  } catch (e) {
    channels = []
  }
  if (!Array.isArray(channels) || channels.length === 0) channels = ['SITE']
  selectedChannels.value = channels
  Object.assign(formData, {
    id: row.id,
    templateCode: row.templateCode,
    templateName: row.templateName,
    titleTemplate: row.titleTemplate || '',
    contentTemplate: row.contentTemplate,
    channels: row.channels || '["SITE"]',
    recipientType: row.recipientType || '',
    status: row.status
  })
  formVisible.value = true
}

function onChannelsChange(values: string[]) {
  formData.channels = JSON.stringify(values)
}

async function handleSubmit() {
  if (!formData.templateCode || !formData.templateName || !formData.contentTemplate) {
    message.warning('请填写模板编码、名称和内容模板')
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateNoticeTemplate(formData.id, formData)
      message.success('更新成功')
    } else {
      await createNoticeTemplate(formData)
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

function handleDelete(row: SysNoticeTemplate) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除模板「${row.templateName}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteNoticeTemplate(row.id)
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
  <PageContainer title="通知模板" description="通知标题/内容模板配置，支持多渠道触达">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新增模板</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="关键字">
          <a-input v-model:value="query.keyword" placeholder="模板编码/名称" allow-clear style="width: 200px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="接收人类型">
          <a-select v-model:value="query.recipientType" placeholder="全部" allow-clear style="width: 140px" :options="recipientOptions" />
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
        <template #emptyText><EmptyState description="暂无模板" action-text="新增模板" @action="openCreate" /></template>
      </a-table>
    </div>

    <a-modal v-model:open="formVisible" :title="isEdit ? '编辑模板' : '新增模板'" width="680px" :confirm-loading="formLoading" @ok="handleSubmit">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="模板编码" required>
              <a-input v-model:value="formData.templateCode" :disabled="isEdit" placeholder="如 PROJECT_CREATED" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="模板名称" required>
              <a-input v-model:value="formData.templateName" placeholder="如 项目创建通知" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="标题模板">
              <a-input v-model:value="formData.titleTemplate" placeholder="支持变量 ${projectName} ${operator}" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="内容模板" required>
              <a-textarea v-model:value="formData.contentTemplate" :rows="5" placeholder="支持变量占位符 ${var}" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="接收人类型">
              <a-select v-model:value="formData.recipientType" placeholder="请选择" allow-clear :options="recipientOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="10">
            <a-form-item label="触达渠道">
              <a-select v-model:value="selectedChannels" mode="multiple" placeholder="选择触达渠道" :options="channelOptions" @change="onChannelsChange" />
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
