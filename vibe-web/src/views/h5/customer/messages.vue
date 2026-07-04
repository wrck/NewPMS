<template>
  <H5Layout title="消息中心" :show-back="true">
    <template #right>
      <span v-if="messages.length > 0" class="read-all-btn" @click="onMarkAllRead">
        全部已读
      </span>
    </template>

    <div v-if="loading" class="loading-area">
      <a-spin />
    </div>

    <div v-else-if="messages.length === 0" class="empty-area">
      <a-empty description="暂无消息" />
    </div>

    <div v-else class="message-list">
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="message-card"
        :class="{ unread: msg.isRead === 0 }"
        @click="onClickMessage(msg)"
      >
        <div class="msg-dot" v-if="msg.isRead === 0"></div>
        <div class="msg-icon" :class="`type-${msg.messageType.toLowerCase()}`">
          {{ getTypeIcon(msg.messageType) }}
        </div>
        <div class="msg-info">
          <div class="msg-title">{{ msg.title }}</div>
          <div class="msg-content" v-if="msg.content">{{ msg.content }}</div>
          <div class="msg-meta">
            <span class="msg-type">{{ getTypeName(msg.messageType) }}</span>
            <span class="msg-time">{{ formatDateTime(msg.createTime) }}</span>
          </div>
        </div>
      </div>
    </div>
  </H5Layout>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import H5Layout from '@/layouts/H5Layout.vue'
import { getMyMessages, markMessageRead, markAllMessagesRead } from '@/api/customerPortal'
import type { CustomerMessage } from '@/types/customerPortal'

const router = useRouter()

const loading = ref(true)
const messages = ref<CustomerMessage[]>([])

onMounted(async () => {
  loading.value = true
  try {
    messages.value = (await getMyMessages()) || []
  } catch (e: any) {
    console.error('[H5 customer messages]', e)
    if (e?.code === 401 || e?.code === 40301) {
      router.push({
        path: '/h5/customer/login',
        query: { redirect: '/h5/customer/messages' }
      })
    } else {
      message.error(e?.message || '加载失败')
    }
  } finally {
    loading.value = false
  }
})

async function onClickMessage(msg: CustomerMessage) {
  // 标记为已读
  if (msg.isRead === 0) {
    try {
      await markMessageRead(msg.id)
      msg.isRead = 1
    } catch (e) {
      console.error('[H5 markMessageRead]', e)
    }
  }
  // 如果有项目关联，跳转到项目进度
  if (msg.projectId) {
    router.push(`/h5/customer/projects/${msg.projectId}/progress`)
  }
}

async function onMarkAllRead() {
  try {
    await markAllMessagesRead()
    messages.value.forEach((m) => (m.isRead = 1))
    message.success('已全部标记为已读')
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  }
}

function getTypeIcon(type: string): string {
  const map: Record<string, string> = {
    PROJECT_PROGRESS: '📊',
    CUTOVER_NOTICE: '📋',
    ACCEPTANCE_NOTICE: '✍️',
    DOCUMENT_UPLOAD: '📄'
  }
  return map[type] || '📨'
}

function getTypeName(type: string): string {
  const map: Record<string, string> = {
    PROJECT_PROGRESS: '进度更新',
    CUTOVER_NOTICE: '割接通知',
    ACCEPTANCE_NOTICE: '验收通知',
    DOCUMENT_UPLOAD: '文档上传'
  }
  return map[type] || type
}

function formatDateTime(dt: string): string {
  if (!dt) return ''
  return dt.replace('T', ' ').substring(0, 16)
}
</script>

<style scoped>
.loading-area,
.empty-area {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px 0;
}

.read-all-btn {
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  cursor: pointer;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.message-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  display: flex;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.15s;
  position: relative;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.message-card:active {
  transform: scale(0.98);
}

.message-card.unread {
  background: #f0f5ff;
}

.msg-dot {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff4d4f;
}

.msg-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  background: #f0f5ff;
  flex-shrink: 0;
}

.msg-info {
  flex: 1;
  min-width: 0;
}

.msg-title {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.msg-content {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.msg-meta {
  font-size: 11px;
  color: #999;
  display: flex;
  gap: 8px;
}

.msg-type {
  color: #1677ff;
}
</style>
