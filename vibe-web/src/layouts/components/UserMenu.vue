<script setup lang="ts">
/**
 * 用户菜单（设计文档 3.2）
 * 消息铃铛 + 用户头像/名称下拉菜单
 */
import { computed } from 'vue'
import { Dropdown, Avatar, Badge, Tooltip } from 'ant-design-vue'
import {
  BellOutlined,
  UserOutlined,
  SettingOutlined,
  LogoutOutlined,
  KeyOutlined,
  DownOutlined
} from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { resetRouter } from '@/router'
import { message, Modal } from 'ant-design-vue'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const username = computed(() => userStore.username || '用户')
const avatarText = computed(() => username.value.charAt(0).toUpperCase())

/** 下拉菜单项 */
const menuItems = [
  { key: 'profile', label: '个人中心', icon: UserOutlined },
  { key: 'password', label: '修改密码', icon: KeyOutlined },
  { key: 'setting', label: '个人设置', icon: SettingOutlined },
  { type: 'divider' as const },
  { key: 'logout', label: '退出登录', icon: LogoutOutlined, danger: true }
]

async function handleMenu({ key }: { key: string | number }) {
  switch (String(key)) {
    case 'profile':
      message.info('个人中心建设中')
      break
    case 'password':
      message.info('修改密码建设中')
      break
    case 'setting':
      message.info('个人设置建设中')
      break
    case 'logout':
      handleLogout()
      break
  }
}

function handleLogout() {
  Modal.confirm({
    title: '确认退出登录？',
    content: '退出后需要重新登录',
    okText: '退出',
    cancelText: '取消',
    async onOk() {
      await userStore.logout()
      resetRouter()
      message.success('已退出登录')
      router.push('/login')
    }
  })
}

function openMessage() {
  appStore.toggleMessageDrawer(true)
}
</script>

<template>
  <div class="user-menu">
    <Tooltip title="消息中心">
      <Badge :count="appStore.unreadCount" :offset="[-2, 4]" :overflow-count="99">
        <span class="header-action" @click="openMessage">
          <BellOutlined />
        </span>
      </Badge>
    </Tooltip>

    <Dropdown :trigger="['click']" placement="bottomRight">
      <div class="user-info">
        <Avatar :size="30" class="user-avatar">{{ avatarText }}</Avatar>
        <span class="user-name">{{ username }}</span>
        <DownOutlined class="user-arrow" />
      </div>
      <template #overlay>
        <a-menu @click="handleMenu">
          <template v-for="(item, idx) in menuItems" :key="idx">
            <a-menu-divider v-if="item.type === 'divider'" />
            <a-menu-item v-else :key="item.key">
              <component :is="item.icon" />
              <span style="margin-left: 8px">{{ item.label }}</span>
            </a-menu-item>
          </template>
        </a-menu>
      </template>
    </Dropdown>
  </div>
</template>

<style lang="less" scoped>
.user-menu {
  display: flex;
  align-items: center;
  gap: 8px;
}
.header-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  font-size: 18px;
  color: @text-secondary;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    background: @bg-sub;
    color: @brand-primary;
  }
}
.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 8px;
  height: 36px;
  border-radius: 18px;
  cursor: pointer;
  transition: background 0.2s;
  &:hover {
    background: @bg-sub;
  }
}
.user-avatar {
  background: @brand-primary;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}
.user-name {
  font-size: 14px;
  color: @text-primary;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.user-arrow {
  font-size: 10px;
  color: @text-tertiary;
}
</style>
