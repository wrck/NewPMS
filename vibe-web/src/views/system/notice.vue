<script setup lang="ts">
/**
 * 站内信管理
 * - 上半部分：发送站内信（管理员可对指定用户发送）
 * - 下半部分：当前用户的站内信列表（已读 / 未读 / 删除）
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  DeleteOutlined,
  CheckOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageMyNotices,
  sendNotice,
  markNoticeRead,
  markAllNoticesRead,
  deleteNotice,
  pageUsers
} from '@/api/system'
import type { SysNotice, SysNoticeDTO, SysNoticeQueryParams, SysUser } from '@/api/system'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<SysNotice[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const query = reactive<SysNoticeQueryParams>({ noticeType: undefined, readStatus: undefined, keyword: '' })

async function loadData() {
  loading.value = true
  try {
    const res = (await pageMyNotices({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<SysNotice>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[system.notice] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

const noticeTypeLabel: Record<number, string> = { 1: '通知', 2: '消息' }

const columns = [
  { title: '标题', dataIndex: 'noticeTitle', key: 'noticeTitle', ellipsis: true },
  { title: '类型', key: 'noticeType', width: 100 },
  { title: '已读', key: 'readStatus', width: 90 },
  { title: '发送时间', dataIndex: 'sendTime', key: 'sendTime', width: 170 },
  { title: '操作', key: 'action', width: 170, fixed: 'right' }
]

/* ============ 发送站内信 ============ */
const sendVisible = ref(false)
const sendLoading = ref(false)
const sendForm = reactive<SysNoticeDTO>({
  noticeTitle: '',
  noticeType: 1,
  noticeContent: '',
  recipientId: 0
})

// 用户选项（按用户名搜索）
const userOptions = ref<{ label: string; value: number }[]>([])
const userSearchLoading = ref(false)

async function searchUsers(keyword: string) {
  if (!keyword || keyword.length < 1) return
  userSearchLoading.value = true
  try {
    const res = (await pageUsers({ userName: keyword, page: 1, size: 20 })) as unknown as PageResult<SysUser>
    userOptions.value = (res.records || []).map(u => ({
      label: `${u.userName}（${u.realName || '-'}）`,
      value: u.id
    }))
  } catch (e) {
    console.error('[system.notice] search users failed:', e)
  } finally {
    userSearchLoading.value = false
  }
}

function openSend() {
  Object.assign(sendForm, { noticeTitle: '', noticeType: 1, noticeContent: '', recipientId: 0 })
  userOptions.value = []
  sendVisible.value = true
}

async function handleSend() {
  if (!sendForm.noticeTitle || !sendForm.recipientId) {
    message.warning('请填写通知标题并选择接收人')
    return
  }
  sendLoading.value = true
  try {
    await sendNotice(sendForm)
    message.success('发送成功')
    sendVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    sendLoading.value = false
  }
}

async function handleMarkRead(row: SysNotice) {
  try {
    await markNoticeRead(row.id)
    message.success('已标记为已读')
    loadData()
  } catch (e) { /* ignore */ }
}

async function handleMarkAllRead() {
  try {
    await markAllNoticesRead()
    message.success('全部已读')
    loadData()
  } catch (e) { /* ignore */ }
}

function handleDelete(row: SysNotice) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除站内信「${row.noticeTitle}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteNotice(row.id)
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
  <PageContainer title="站内信" description="发送站内信与个人消息管理">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button @click="handleMarkAllRead"><template #icon><CheckOutlined /></template>全部已读</a-button>
      <a-button type="primary" @click="openSend"><template #icon><PlusOutlined /></template>发送站内信</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="标题">
          <a-input v-model:value="query.keyword" placeholder="标题关键字" allow-clear style="width: 200px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="类型">
          <a-select v-model:value="query.noticeType" placeholder="全部" allow-clear style="width: 110px">
            <a-select-option :value="1">通知</a-select-option>
            <a-select-option :value="2">消息</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="已读">
          <a-select v-model:value="query.readStatus" placeholder="全部" allow-clear style="width: 110px">
            <a-select-option :value="0">未读</a-select-option>
            <a-select-option :value="1">已读</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="vibe-card table-card">
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1100 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'noticeType'">
            <StatusTag :tone="record.noticeType === 1 ? 'processing' : 'default'">{{ noticeTypeLabel[record.noticeType] || '-' }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'readStatus'">
            <StatusTag :tone="record.readStatus === 1 ? 'success' : 'warning'">{{ record.readStatus === 1 ? '已读' : '未读' }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a v-if="record.readStatus === 0" @click="handleMarkRead(record)"><CheckOutlined /> 标已读</a>
              <a class="danger-link" @click="handleDelete(record)"><DeleteOutlined /></a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无站内信" /></template>
      </a-table>
    </div>

    <!-- 发送站内信 -->
    <a-modal v-model:open="sendVisible" title="发送站内信" width="600px" :confirm-loading="sendLoading" @ok="handleSend">
      <a-form layout="vertical">
        <a-form-item label="接收人" required>
          <a-select
            v-model:value="sendForm.recipientId"
            show-search
            placeholder="输入用户名搜索"
            :options="userOptions"
            :filter="false"
            :loading="userSearchLoading"
            :default-active-first-option="false"
            style="width: 100%"
            @search="searchUsers"
          />
        </a-form-item>
        <a-form-item label="通知类型">
          <a-radio-group v-model:value="sendForm.noticeType">
            <a-radio :value="1">通知</a-radio>
            <a-radio :value="2">消息</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="标题" required>
          <a-input v-model:value="sendForm.noticeTitle" placeholder="通知标题" />
        </a-form-item>
        <a-form-item label="内容">
          <a-textarea v-model:value="sendForm.noticeContent" :rows="4" placeholder="通知内容" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
.danger-link { color: @status-exception; }
</style>
