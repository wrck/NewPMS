<script setup lang="ts">
/**
 * 登录日志
 * 用户登录历史查询（成功/失败、IP、浏览器、操作系统）
 */
import { ref, reactive, onMounted } from 'vue'
import { ReloadOutlined, EyeOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { pageLoginLogs } from '@/api/system'
import type { SysLoginLog, SysLoginLogQueryParams } from '@/api/system'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<SysLoginLog[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const query = reactive<SysLoginLogQueryParams>({ userName: '', status: undefined, startBegin: '', startEnd: '' })

async function loadData() {
  loading.value = true
  try {
    const res = (await pageLoginLogs({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<SysLoginLog>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[system.login-log] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

const columns = [
  { title: '登录账号', dataIndex: 'username', key: 'username', width: 140 },
  { title: '登录时间', dataIndex: 'loginTime', key: 'loginTime', width: 170 },
  { title: 'IP', dataIndex: 'loginIp', key: 'loginIp', width: 140 },
  { title: '登录地点', dataIndex: 'loginLocation', key: 'loginLocation', width: 140 },
  { title: '浏览器', dataIndex: 'browser', key: 'browser', width: 140, ellipsis: true },
  { title: '操作系统', dataIndex: 'os', key: 'os', width: 160, ellipsis: true },
  { title: '状态', key: 'status', width: 90 },
  { title: '提示消息', dataIndex: 'msg', key: 'msg', ellipsis: true },
  { title: '操作', key: 'action', width: 90, fixed: 'right' }
]

// 详情弹窗
const detailVisible = ref(false)
const detail = ref<SysLoginLog | null>(null)

function openDetail(row: SysLoginLog) {
  detailVisible.value = true
  detail.value = row
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="登录日志" description="用户登录历史记录与审计">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="登录账号">
          <a-input v-model:value="query.userName" placeholder="登录账号" allow-clear style="width: 180px" @pressEnter="handleSearch" />
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
          <template v-if="column.key === 'status'">
            <StatusTag :tone="record.status === 1 ? 'success' : 'error'">{{ record.status === 1 ? '成功' : '失败' }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a @click="openDetail(record)"><EyeOutlined /> 详情</a>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无登录日志" /></template>
      </a-table>
    </div>

    <a-modal v-model:open="detailVisible" title="登录日志详情" :footer="null" width="640px">
      <a-descriptions v-if="detail" :column="2" size="small" bordered>
        <a-descriptions-item label="登录账号">{{ detail.username }}</a-descriptions-item>
        <a-descriptions-item label="登录时间">{{ detail.loginTime }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <StatusTag :tone="detail.status === 1 ? 'success' : 'error'">{{ detail.status === 1 ? '成功' : '失败' }}</StatusTag>
        </a-descriptions-item>
        <a-descriptions-item label="提示消息">{{ detail.msg || '-' }}</a-descriptions-item>
        <a-descriptions-item label="登录 IP">{{ detail.loginIp || '-' }}</a-descriptions-item>
        <a-descriptions-item label="登录地点">{{ detail.loginLocation || '-' }}</a-descriptions-item>
        <a-descriptions-item label="浏览器">{{ detail.browser || '-' }}</a-descriptions-item>
        <a-descriptions-item label="操作系统">{{ detail.os || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
</style>
