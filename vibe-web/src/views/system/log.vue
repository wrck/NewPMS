<script setup lang="ts">
/**
 * 操作日志
 * 系统操作日志查询与详情查看
 */
import { ref, reactive, onMounted } from 'vue'
import { ReloadOutlined, EyeOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageLogs,
  getLogDetail
} from '@/api/system'
import type { SysLog, SysLogQueryParams } from '@/api/system'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<SysLog[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const query = reactive<SysLogQueryParams>({ title: '', module: '', type: undefined, operatorId: undefined, status: undefined, startBegin: '', startEnd: '' })

async function loadData() {
  loading.value = true
  try {
    const res = (await pageLogs({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<SysLog>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[system.log] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

const typeLabel: Record<string, string> = {
  INSERT: '新增',
  UPDATE: '修改',
  DELETE: '删除',
  QUERY: '查询',
  EXPORT: '导出',
  OTHER: '其他'
}
const typeTone: Record<string, any> = {
  INSERT: 'success',
  UPDATE: 'processing',
  DELETE: 'error',
  QUERY: 'default',
  EXPORT: 'warning',
  OTHER: 'default'
}

const columns = [
  { title: '模块', dataIndex: 'module', key: 'module', width: 120 },
  { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '操作类型', key: 'type', width: 100 },
  { title: '请求方式', dataIndex: 'requestMethod', key: 'requestMethod', width: 100 },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: 110 },
  { title: 'IP', dataIndex: 'operatorIp', key: 'operatorIp', width: 130 },
  { title: '耗时(ms)', dataIndex: 'costMs', key: 'costMs', width: 100 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作时间', dataIndex: 'operatedAt', key: 'operatedAt', width: 170 },
  { title: '操作', key: 'action', width: 90, fixed: 'right' }
]

// 详情弹窗
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<SysLog | null>(null)

async function openDetail(row: SysLog) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = (await getLogDetail(row.id)) as unknown as SysLog
  } catch (e) {
    detail.value = row
  } finally {
    detailLoading.value = false
  }
}

function costColor(ms?: number) {
  if (ms == null) return ''
  if (ms > 3000) return 'text-error'
  if (ms > 1000) return 'text-warning'
  return 'text-success'
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="操作日志" description="系统操作日志查询与审计">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="标题">
          <a-input v-model:value="query.title" placeholder="标题关键字" allow-clear style="width: 180px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="模块">
          <a-input v-model:value="query.module" placeholder="模块名" allow-clear style="width: 140px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="操作类型">
          <a-select v-model:value="query.type" placeholder="全部" allow-clear style="width: 120px">
            <a-select-option v-for="(v, k) in typeLabel" :key="k" :value="k">{{ v }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 110px">
            <a-select-option :value="1">成功</a-select-option>
            <a-select-option :value="0">失败</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="开始起">
          <a-date-picker v-model:value="query.startBegin" value-format="YYYY-MM-DD" style="width: 150px" />
        </a-form-item>
        <a-form-item label="结束止">
          <a-date-picker v-model:value="query.startEnd" value-format="YYYY-MM-DD" style="width: 150px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="vibe-card table-card">
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1400 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <StatusTag :tone="typeTone[record.type]">{{ typeLabel[record.type] || record.type }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'costMs'">
            <span class="tnum" :class="costColor(record.costMs)">{{ record.costMs ?? '-' }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="record.status === 1 ? 'success' : 'error'">{{ record.status === 1 ? '成功' : '失败' }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a @click="openDetail(record)"><EyeOutlined /> 详情</a>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无日志" /></template>
      </a-table>
    </div>

    <!-- 详情 -->
    <a-modal v-model:open="detailVisible" title="日志详情" :footer="null" width="780px">
      <a-spin :spinning="detailLoading">
        <a-descriptions v-if="detail" :column="2" size="small" bordered>
          <a-descriptions-item label="模块">{{ detail.module }}</a-descriptions-item>
          <a-descriptions-item label="标题">{{ detail.title }}</a-descriptions-item>
          <a-descriptions-item label="操作类型">
            <StatusTag :tone="typeTone[detail.type]">{{ typeLabel[detail.type] || detail.type }}</StatusTag>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <StatusTag :tone="detail.status === 1 ? 'success' : 'error'">{{ detail.status === 1 ? '成功' : '失败' }}</StatusTag>
          </a-descriptions-item>
          <a-descriptions-item label="请求方式">{{ detail.requestMethod || '-' }}</a-descriptions-item>
          <a-descriptions-item label="请求 URL">{{ detail.requestUrl || '-' }}</a-descriptions-item>
          <a-descriptions-item label="操作人">{{ detail.operatorName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="操作 IP">{{ detail.operatorIp || '-' }}</a-descriptions-item>
          <a-descriptions-item label="耗时">{{ detail.costMs ?? '-' }} ms</a-descriptions-item>
          <a-descriptions-item label="操作时间">{{ detail.operatedAt }}</a-descriptions-item>
          <a-descriptions-item label="方法" :span="2">{{ detail.method }}</a-descriptions-item>
          <a-descriptions-item label="请求参数" :span="2">
            <pre class="code-block">{{ detail.requestParams || '-' }}</pre>
          </a-descriptions-item>
          <a-descriptions-item label="响应结果" :span="2">
            <pre class="code-block">{{ detail.responseResult || '-' }}</pre>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.errorMsg" label="错误信息" :span="2">
            <pre class="code-block text-error">{{ detail.errorMsg }}</pre>
          </a-descriptions-item>
        </a-descriptions>
      </a-spin>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
.code-block {
  margin: 0; max-height: 200px; overflow: auto;
  background: @bg-page; padding: 8px; border-radius: 4px;
  font-size: 12px; white-space: pre-wrap; word-break: break-all;
}
.text-error { color: @status-exception; }
.text-warning { color: @status-warning; }
.text-success { color: @status-success; }
</style>
