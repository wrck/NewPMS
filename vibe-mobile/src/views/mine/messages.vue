<script setup lang="ts">
/**
 * 消息中心
 *
 * 后端：
 *   - GET /notices                分页查询当前用户站内信（PageResult<SysNoticeVO>）
 *   - GET /notices/unread-count   未读计数
 *   - PUT /notices/{id}/read      标记单条已读
 *   - PUT /notices/read-all       全部标记已读
 *   - DELETE /notices/{id}        删除站内信
 */
import { ref, computed, onMounted } from 'vue'
import {
  showSuccessToast,
  showLoadingToast,
  closeToast,
  showConfirmDialog
} from 'vant'
import {
  fetchNotices,
  fetchUnreadNoticeCount,
  markNoticeRead,
  markAllNoticesRead,
  deleteNotice,
  type NoticeQuery
} from '@/api'
import type { SysNoticeVO } from '@/types/api'

defineOptions({ name: 'MineMessages' })

const list = ref<SysNoticeVO[]>([])
const loading = ref(false)
const finished = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 20

const activeTab = ref(0)
/** 0-全部 1-未读 2-已读 */
const readStatusFilter = computed<number | undefined>(() => {
  switch (activeTab.value) {
    case 1:
      return 0
    case 2:
      return 1
    default:
      return undefined
  }
})

const unreadCount = ref(0)

async function loadUnreadCount() {
  try {
    unreadCount.value = Number(await fetchUnreadNoticeCount()) || 0
  } catch (e) {
    unreadCount.value = 0
  }
}

async function loadList(reset = false) {
  if (reset) {
    page.value = 1
    list.value = []
    finished.value = false
  }
  if (finished.value) return
  loading.value = true
  try {
    const query: NoticeQuery = {
      page: page.value,
      size: pageSize
    }
    if (readStatusFilter.value !== undefined) {
      query.readStatus = readStatusFilter.value
    }
    const res = await fetchNotices(query)
    const records = res?.records || []
    list.value.push(...records)
    total.value = res?.total || 0
    if (list.value.length >= total.value || records.length === 0) {
      finished.value = true
    } else {
      page.value += 1
    }
  } catch (e) {
    finished.value = true
  } finally {
    loading.value = false
  }
}

function onLoad() {
  loadList()
}

/** 切换 Tab */
function onTabChange() {
  loadList(true)
}

/** 点击消息：标记已读 */
async function onMessage(msg: SysNoticeVO) {
  if (msg.readStatus === 0) {
    try {
      await markNoticeRead(msg.id)
      msg.readStatus = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (e) {
      // request 拦截器已 toast 错误
    }
  }
}

/** 全部已读 */
async function onMarkAllRead() {
  if (unreadCount.value === 0) {
    showSuccessToast('没有未读消息')
    return
  }
  showLoadingToast({ message: '处理中...', forbidClick: true, duration: 0 })
  try {
    await markAllNoticesRead()
    list.value.forEach((m) => (m.readStatus = 1))
    unreadCount.value = 0
    closeToast()
    showSuccessToast('已全部标记为已读')
  } catch (e) {
    closeToast()
  }
}

/** 删除消息 */
async function onDelete(msg: SysNoticeVO, index: number) {
  try {
    await showConfirmDialog({
      title: '删除消息',
      message: `确认删除「${msg.noticeTitle}」？`
    })
    await deleteNotice(msg.id)
    list.value.splice(index, 1)
    if (msg.readStatus === 0) {
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
    showSuccessToast('已删除')
  } catch (e) {
    // 取消或失败
  }
}

function noticeTypeText(type?: number): string {
  return type === 2 ? '消息' : '通知'
}

function noticeTypeColor(type?: number): string {
  return type === 2 ? '#722ed1' : '#1677ff'
}

onMounted(() => {
  loadUnreadCount()
})
</script>

<template>
  <div class="message-page page">
    <van-tabs v-model:active="activeTab" sticky @change="onTabChange">
      <van-tab title="全部" />
      <van-tab :title="`未读${unreadCount > 0 ? `(${unreadCount})` : ''}`" />
      <van-tab title="已读" />
    </van-tabs>

    <!-- 顶部操作栏 -->
    <div v-if="unreadCount > 0" class="top-bar">
      <span class="hint">{{ unreadCount }} 条未读消息</span>
      <van-button size="small" type="primary" plain class="touchable" @click="onMarkAllRead">
        全部已读
      </van-button>
    </div>

    <van-list
      v-model:loading="loading"
      :finished="finished"
      finished-text="没有更多了"
      @load="onLoad"
    >
      <div
        v-for="(msg, index) in list"
        :key="msg.id"
        class="msg-item card"
        :class="{ 'msg-item--unread': msg.readStatus === 0 }"
        @click="onMessage(msg)"
      >
        <div class="msg-item__head">
          <van-icon
            :name="msg.readStatus === 1 ? 'envelope-o' : 'envelope'"
            :color="msg.readStatus === 1 ? '#8c8c8c' : '#1677ff'"
            size="18"
          />
          <span class="title">{{ msg.noticeTitle }}</span>
          <van-tag v-if="msg.noticeType" plain :color="noticeTypeColor(msg.noticeType)">
            {{ noticeTypeText(msg.noticeType) }}
          </van-tag>
          <van-tag v-if="msg.readStatus === 0" type="danger">新</van-tag>
        </div>
        <div v-if="msg.noticeContent" class="msg-item__content">{{ msg.noticeContent }}</div>
        <div class="msg-item__foot">
          <span class="time">{{ msg.sendTime || '' }}</span>
          <van-button
            size="mini"
            type="danger"
            plain
            class="touchable"
            @click.stop="onDelete(msg, index)"
          >
            删除
          </van-button>
        </div>
      </div>
    </van-list>

    <van-empty v-if="!loading && list.length === 0" description="暂无消息" />
  </div>
</template>

<style scoped lang="scss">
.message-page {
  padding-bottom: 20px;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: rgba(22, 119, 255, 0.06);

  .hint {
    font-size: 13px;
    color: var(--brand-primary);
  }
}

.msg-item {
  margin: 8px 12px;
  transition: background 0.2s;

  &--unread {
    border-left: 3px solid var(--brand-primary);
  }

  &__head {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 6px;

    .title {
      flex: 1;
      font-size: 15px;
      font-weight: 600;
    }
  }

  &__content {
    font-size: 13px;
    color: var(--color-text-regular);
    line-height: 1.6;
    margin-bottom: 6px;
  }

  &__foot {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .time {
      font-size: 11px;
      color: var(--color-text-placeholder);
    }
  }
}
</style>
