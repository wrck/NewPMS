<script setup lang="ts">
/**
 * 集成调用日志
 * 外部系统调用历史与审计
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  ReloadOutlined,
  EyeOutlined,
  DeleteOutlined,
  ClearOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageIntegrationCallLogs,
  getIntegrationCallLogDetail,
  deleteIntegrationCallLog,
  clearAllIntegrationCallLogs
} from '@/api/integration'
import type {
  IntegrationCallLog,
  IntegrationCallLogQueryParams
} from '@/api/integration'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<IntegrationCallLog[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const query = reactive<IntegrationCallLogQueryParams>({
  configId: undefined,
  systemCode: '',
  callScene: '',
  status: undefined,
  startBegin: '',
  startEnd: ''
})

async function loadData() {
  loading.value = true
  try {
    const res = (await pageIntegrationCallLogs({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<IntegrationCallLog>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[integration.call-log] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

const statusTone: Record<string, any> = {
  SUCCESS: 'success',
  FAIL: 'error',
  TIMEOUT: 'warning'
}

const columns = [
  { title: '系统编码', dataIndex: 'systemCode', key: 'systemCode', width: 140 },
  { title: '调用场景', dataIndex: 'callScene', key: 'callScene', width: 130 },
  { title: '请求方式', dataIndex: 'requestMethod', key: 'requestMethod', width: 100 },
  { title: '请求 URL', dataIndex: 'requestUrl', key: 'requestUrl', ellipsis: true },
  { title: 'HTTP', dataIndex: 'responseStatus', key: 'responseStatus', width: 80 },
  { title: '状态', key: 'status', width: 100 },
  { title: '耗时(ms)', dataIndex: 'costMs', key: 'costMs', width: 100 },
  { title: '调用时间', dataIndex: 'operatedAt', key: 'operatedAt', width: 170 },
  { title: '操作', key: 'action', width: 130, fixed: 'right' }
]

const detailVisible = ref(false)
const detail = ref<IntegrationCallLog | null>(null)

async function openDetail(row: IntegrationCallLog) {
  detailVisible.value = true
  try {
    detail.value = (await getIntegrationCallLogDetail(row.id)) as unknown as IntegrationCallLog
  } catch (e) {
    detail.value = row
  }
}

function handleDelete(row: IntegrationCallLog) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除该调用日志吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteIntegrationCallLog(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) { /* ignore */ }
    }
  })
}

function handleClearAll() {
  Modal.confirm({
    title: '清空所有日志',
    content: '此操作将永久删除所有调用日志，是否继续？',
    okType: 'danger',
    async onOk() {
      try {
        await clearAllIntegrationCallLogs()
        message.success('已清空')
        loadData()
      } catch (e) { /* ignore */ }
    }
  })
}

function costColor(ms?: number) {
  if (ms == null) return ''
  if (ms > 5000) return 'text-error'
  if (ms > 2000) return 'text-warning'
  return 'text-success'
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="集成调用日志" description="外部系统调用历史记录与审计">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button danger @click="handleClearAll"><template #icon><ClearOutlined /></template>清空全部</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="系统编码">
          <a-input v-model:value="query.systemCode" placeholder="系统编码" allow-clear style="width: 160px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="调用场景">
          <a-input v-model:value="query.callScene" placeholder="调用场景" allow-clear style="width: 160px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 120px">
            <a-select-option value="SUCCESS">成功</a-select-option>
            <a-select-option value="FAIL">失败</a-select-option>
            <a-select-option value="TIMEOUT">超时</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="开始起">
          <a-date-picker v-model:value="query.startBegin" value-format="YYYY-MM-DD HH:mm:ss" show-time style="width: 200px" />
        </a-form-item>
        <a-form-item label="结束止">
          <a-date-picker v-model:value="query.startEnd" value-format="YYYY-MM-DD HH:mm:ss" show-time style="width: 200px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="vibe-card table-card">
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1500 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'responseStatus'">
            <span :class="record.responseStatus != null && (record.responseStatus >= 400 ? 'text-error' : 'text-success')">
              {{ record.responseStatus ?? '-' }}
            </span>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="statusTone[record.status]">{{ record.status }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'costMs'">
            <span class="tnum" :class="costColor(record.costMs)">{{ record.costMs ?? '-' }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a @click="openDetail(record)"><EyeOutlined /> 详情</a>
              <a class="danger-link" @click="handleDelete(record)"><DeleteOutlined /></a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无调用日志" /></template>
      </a-table>
    </div>

    <a-modal v-model:open="detailVisible" title="调用日志详情" :footer="null" width="820px">
      <a-descriptions v-if="detail" :column="2" size="small" bordered>
        <a-descriptions-item label="系统编码">{{ detail.systemCode }}</a-descriptions-item>
        <a-descriptions-item label="调用场景">{{ detail.callScene }}</a-descriptions-item>
        <a-descriptions-item label="请求方式">{{ detail.requestMethod || '-' }}</a-descriptions-item>
        <a-descriptions-item label="HTTP 响应">{{ detail.responseStatus ?? '-' }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <StatusTag :tone="statusTone[detail.status]">{{ detail.status }}</StatusTag>
        </a-descriptions-item>
        <a-descriptions-item label="耗时">{{ detail.costMs ?? '-' }} ms</a-descriptions-item>
        <a-descriptions-item label="调用方 IP">{{ detail.callerIp || '-' }}</a-descriptions-item>
        <a-descriptions-item label="调用时间">{{ detail.operatedAt }}</a-descriptions-item>
        <a-descriptions-item label="请求 URL" :span="2">{{ detail.requestUrl || '-' }}</a-descriptions-item>
        <a-descriptions-item label="请求头" :span="2">
          <pre class="code-block">{{ detail.requestHeaders || '-' }}</pre>
        </a-descriptions-item>
        <a-descriptions-item label="请求体" :span="2">
          <pre class="code-block">{{ detail.requestBody || '-' }}</pre>
        </a-descriptions-item>
        <a-descriptions-item label="响应体" :span="2">
          <pre class="code-block">{{ detail.responseBody || '-' }}</pre>
        </a-descriptions-item>
        <a-descriptions-item v-if="detail.errorMsg" label="错误信息" :span="2">
          <pre class="code-block text-error">{{ detail.errorMsg }}</pre>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
.danger-link { color: @status-exception; }
.code-block {
  margin: 0; max-height: 200px; overflow: auto;
  background: @bg-page; padding: 8px; border-radius: 4px;
  font-size: 12px; white-space: pre-wrap; word-break: break-all;
}
.text-error { color: @status-exception; }
.text-warning { color: @status-warning; }
.text-success { color: @status-success; }
</style>
