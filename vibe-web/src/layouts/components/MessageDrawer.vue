<script setup lang="ts">
/**
 * 站内信抽屉（占位）
 * 后续接入消息中心接口：列表、未读计数、已读标记
 */
import { computed, ref } from 'vue'
import { Drawer, List, Tag, Button, Badge } from 'ant-design-vue'
import EmptyState from '@/components/EmptyState.vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()

const visible = computed({
  get: () => appStore.messageDrawerVisible,
  set: (v) => appStore.toggleMessageDrawer(v)
})

// 占位消息列表（后续接入接口）
interface MessageItem {
  id: number
  title: string
  content: string
  time: string
  read: boolean
  type: string
}

const messages = ref<MessageItem[]>([])

function markAllRead() {
  messages.value.forEach((m) => (m.read = true))
  appStore.setUnreadCount(0)
}
</script>

<template>
  <Drawer
    v-model:open="visible"
    title="消息中心"
    placement="right"
    :width="380"
    :body-style="{ padding: 0 }"
  >
    <template #extra>
      <Button type="link" size="small" :disabled="!messages.length" @click="markAllRead">
        全部已读
      </Button>
    </template>
    <div class="msg-drawer">
      <EmptyState v-if="!messages.length" description="暂无消息" size="compact" />
      <List v-else :data-source="messages" :split="true">
        <template #renderItem="{ item }">
          <List.Item class="msg-item" :class="{ unread: !item.read }">
            <List.Item.Meta>
              <template #title>
                <div class="msg-title">
                  <Badge :dot="!item.read" :offset="[6, 0]">
                    <span>{{ item.title }}</span>
                  </Badge>
                  <Tag color="blue" :bordered="false">{{ item.type }}</Tag>
                </div>
              </template>
              <template #description>
                <div class="msg-content">{{ item.content }}</div>
                <div class="msg-time">{{ item.time }}</div>
              </template>
            </List.Item.Meta>
          </List.Item>
        </template>
      </List>
    </div>
  </Drawer>
</template>

<style lang="less" scoped>
.msg-drawer {
  height: 100%;
  overflow: auto;
}
.msg-item {
  padding: 12px 16px;
  cursor: pointer;
  &:hover {
    background: @bg-selected;
  }
  &.unread {
    background: #fafcff;
  }
}
.msg-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.msg-content {
  color: @text-secondary;
  font-size: 13px;
  margin-top: 4px;
}
.msg-time {
  color: @text-tertiary;
  font-size: 12px;
  margin-top: 4px;
}
</style>
