<script setup lang="ts">
/**
 * 我的页
 */
import { useRouter } from 'vue-router'
import { showConfirmDialog } from 'vant'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'Mine' })

const router = useRouter()
const userStore = useUserStore()

const menuGroups = [
  {
    items: [
      { icon: 'clock-o', title: '工时填报', path: '/mine/timesheet', color: '#1677ff' },
      { icon: 'logistics-o', title: '出差记录', path: '/mine/timesheet', color: '#722ed1' },
      { icon: 'chart-trending-o', title: '绩效概览', path: '/mine/settings', color: '#13c2c2' }
    ]
  },
  {
    items: [
      { icon: 'chat-o', title: '消息中心', path: '/mine/messages', color: '#fa8c16', badge: 3 },
      { icon: 'setting-o', title: '设置', path: '/mine/settings', color: '#8c8c8c' }
    ]
  }
]

function onMenu(item: { path: string }) {
  router.push(item.path)
}

async function onLogout() {
  try {
    await showConfirmDialog({
      title: '提示',
      message: '确定要退出登录吗？'
    })
    await userStore.logout()
    router.replace('/login')
  } catch (e) {
    // 取消
  }
}
</script>

<template>
  <div class="mine-page page">
    <!-- 个人信息卡 -->
    <div class="profile-card">
      <div class="profile-card__avatar">
        <van-image
          round
          width="64"
          height="64"
          :src="userStore.avatar"
          fit="cover"
        >
          <template #error>
            <div class="avatar-placeholder">
              {{ (userStore.nickname || 'U').charAt(0) }}
            </div>
          </template>
        </van-image>
      </div>
      <div class="profile-card__info">
        <div class="name">{{ userStore.nickname || '未登录' }}</div>
        <div class="role">{{ userStore.roles.join('、') || '工程师' }}</div>
      </div>
    </div>

    <!-- 菜单组 -->
    <div v-for="(group, idx) in menuGroups" :key="idx" class="menu-group">
      <van-cell-group inset>
        <van-cell
          v-for="item in group.items"
          :key="item.title"
          :title="item.title"
          is-link
          @click="onMenu(item)"
        >
          <template #icon>
            <van-icon
              :name="item.icon"
              :style="{ color: item.color }"
              class="menu-icon"
            />
          </template>
          <template #value>
            <van-tag v-if="item.badge" type="danger" round>{{ item.badge }}</van-tag>
          </template>
        </van-cell>
      </van-cell-group>
    </div>

    <!-- 退出登录 -->
    <div class="logout-section">
      <van-button type="danger" plain block round class="touchable" @click="onLogout">
        退出登录
      </van-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.mine-page {
  padding: 12px 0;
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%);
  color: #fff;
  margin: 0 12px 16px;
  padding: 20px 16px;
  border-radius: var(--radius-lg);

  &__avatar {
    .avatar-placeholder {
      width: 64px;
      height: 64px;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.25);
      color: #fff;
      font-size: 28px;
      font-weight: 600;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  &__info {
    .name {
      font-size: 18px;
      font-weight: 600;
    }
    .role {
      font-size: 12px;
      opacity: 0.85;
      margin-top: 4px;
    }
  }
}

.menu-group {
  margin-bottom: 12px;
}

.menu-icon {
  margin-right: 8px;
  font-size: 18px;
}

.logout-section {
  padding: 24px 16px;
}
</style>
