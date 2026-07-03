<script setup lang="ts">
/**
 * 我的消息
 * 系统通知、待办提醒、审批消息
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { ReloadOutlined, CheckOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getDashboard } from '@/api/report'

interface Notice {
  id: number
  title: string
  type: string
  createdAt: string
  read: boolean
}

const loading = ref(false)
const dataSource = ref<Notice[]>([])
const filter = reactive({ type: '' as string, onlyUnread: false })

async function loadData() {
  loading.value = true
  try {
    const res = (await getDashboard()) as unknown as { notices: Notice[] }
    dataSource.value = res.notices || []
  } catch (e) {
    console.error('[dashboard.my-messages] load failed:', e)
  } finally {
    loading.value = false
  }
}

const typeLabel: Record<string, string> = {
  TASK: '任务',
  APPROVAL: '审批',
  SYSTEM: '系统',
  RISK: '风险',
  OTHER: '其他'
}
const typeTone: Record<string, any> = {
  TASK: 'processing',
  APPROVAL: 'warning',
  SYSTEM: 'default',
  RISK: 'error',
  OTHER: 'default'
}

const filtered = computed(() => {
  return dataSource.value.filter((n) => {
    if (filter.type && n.type !== filter.type) return false
    if (filter.onlyUnread && n.read) return false
    return true
  })
})

const unreadCount = computed(() => dataSource.value.filter((n) => !n.read).length)

function markRead(row: Notice) {
  row.read = true
  message.success('已标记为已读')
}

function markAllRead() {
  dataSource.value.forEach((n) => (n.read = true))
  message.success('全部已读')
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="我的消息" description="系统通知与待办提醒">
    <template #extra>
      <a-button @click="markAllRead" :disabled="!unreadCount"><template #icon><CheckOutlined /></template>全部已读</a-button>
      <a-button @click="loadData" :loading="loading"><template #icon><ReloadOutlined /></template>刷新</a-button>
    </template>

    <div class="vibe-card filter-card">
      <a-space>
        <a-radio-group v-model:value="filter.type" button-style="solid">
          <a-radio-button value="">全部</a-radio-button>
          <a-radio-button v-for="(v, k) in typeLabel" :key="k" :value="k">{{ v }}</a-radio-button>
        </a-radio-group>
        <a-checkbox v-model:checked="filter.onlyUnread">仅看未读</a-checkbox>
        <span class="text-auxiliary">未读 <span class="tnum">{{ unreadCount }}</span> 条</span>
      </a-space>
    </div>

    <div class="vibe-card list-card">
      <EmptyState v-if="!filtered.length" description="暂无消息" />
      <a-list v-else :data-source="filtered" item-layout="horizontal">
        <template #renderItem="{ item }">
          <a-list-item :class="{ 'unread-item': !item.read }">
            <a-list-item-meta>
              <template #title>
                <StatusTag :tone="typeTone[item.type] || 'default'">{{ typeLabel[item.type] || item.type }}</StatusTag>
                <span :class="{ 'unread-title': !item.read }">{{ item.title }}</span>
              </template>
              <template #description>
                <span class="text-auxiliary">{{ item.createdAt }}</span>
              </template>
            </a-list-item-meta>
            <template #actions>
              <a v-if="!item.read" @click="markRead(item)">标为已读</a>
            </template>
          </a-list-item>
        </template>
      </a-list>
    </div>
  </PageContainer>
</template>

<style lang="less" scoped>
.filter-card { padding: 16px 20px; margin-bottom: 16px; }
.list-card { padding: 8px 20px; }
.text-auxiliary { color: @text-auxiliary; font-size: 13px; }
.unread-item {
  background: @bg-selected;
  .unread-title { font-weight: 600; }
}
</style>
