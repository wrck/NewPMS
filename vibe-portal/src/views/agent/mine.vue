<template>
  <div class="agent-mine">
    <!-- 公司信息卡片 -->
    <section class="profile-card">
      <div class="avatar">
        <van-icon name="friends-o" size="32" color="#722ed1" />
      </div>
      <div class="profile-info">
        <div class="contact-name">
          <span>{{ agentStore.contactName || '代理商' }}</span>
          <van-loading v-if="loading" size="14" color="#fff" class="refresh-loading" />
        </div>
        <div class="company-name">{{ agentStore.companyName || '-' }}</div>
        <div class="username">账号：{{ agentStore.username || '-' }}</div>
        <div v-if="agentStore.agentInfo?.phone" class="phone">
          手机：{{ agentStore.agentInfo.phone }}
        </div>
      </div>
      <div class="refresh-btn" @click="refreshUserInfo">
        <van-icon name="replay" size="18" color="#fff" />
      </div>
    </section>

    <!-- 功能入口 -->
    <van-cell-group inset class="menu-group">
      <van-cell title="结算查看" icon="balance-o" is-link @click="goSettlement" />
      <van-cell title="任务列表" icon="orders-o" is-link @click="goTasks" />
      <van-cell title="工作台" icon="wap-nav" is-link @click="goDashboard" />
    </van-cell-group>

    <van-cell-group inset class="menu-group">
      <van-cell title="关于" icon="info-o" is-link @click="showAbout" />
      <van-cell title="清除缓存" icon="delete-o" is-link @click="onClearCache" />
    </van-cell-group>

    <div class="logout">
      <van-button block type="danger" plain @click="onLogout">退出登录</van-button>
    </div>

    <div class="version">版本 v0.1.0</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast, showSuccessToast } from 'vant'
import { useAgentStore } from '@/stores/agent'
import { storage } from '@/utils/storage'

const router = useRouter()
const agentStore = useAgentStore()

const loading = ref(false)

/** 拉取最新用户信息 */
async function refreshUserInfo() {
  if (loading.value) return
  loading.value = true
  try {
    await agentStore.fetchUserInfo()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 进入页面静默刷新一次用户信息（失败忽略，沿用本地缓存）
  refreshUserInfo()
})

function goSettlement() {
  router.push('/agent/settlement')
}

function goTasks() {
  router.push('/agent/tasks')
}

function goDashboard() {
  router.push('/agent/dashboard')
}

function showAbout() {
  showToast('实施交付 - 代理商入口 H5 v0.1.0')
}

function onClearCache() {
  showConfirmDialog({ title: '提示', message: '确定清除本地缓存吗？' })
    .then(() => {
      // 仅清理非登录态缓存，保留 Token
      storage.remove('last_location')
      showSuccessToast('已清除缓存')
    })
    .catch(() => {})
}

function onLogout() {
  showConfirmDialog({ title: '提示', message: '确定退出登录吗？' })
    .then(async () => {
      await agentStore.logout()
      router.replace('/agent/login')
    })
    .catch(() => {})
}
</script>

<style lang="scss" scoped>
.agent-mine {
  min-height: 100%;
  padding: 12px;
  padding-bottom: calc(24px + var(--safe-bottom));
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: linear-gradient(135deg, var(--agent-primary) 0%, #9254de 100%);
  border-radius: 12px;
  padding: 16px;
  color: #fff;

  .avatar {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.2);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .profile-info {
    flex: 1;
    min-width: 0;

    .contact-name {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 17px;
      font-weight: 600;

      .refresh-loading {
        display: inline-flex;
      }
    }
    .company-name {
      margin-top: 4px;
      font-size: 13px;
      opacity: 0.9;
    }
    .username {
      margin-top: 2px;
      font-size: 12px;
      opacity: 0.8;
    }
    .phone {
      margin-top: 2px;
      font-size: 12px;
      opacity: 0.8;
    }
  }

  .refresh-btn {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.2);
    flex-shrink: 0;
    cursor: pointer;

    &:active {
      background: rgba(255, 255, 255, 0.35);
    }
  }
}

.menu-group {
  margin-top: 12px;
}

.logout {
  margin-top: 24px;
  padding: 0 12px;
}

.version {
  margin-top: 16px;
  text-align: center;
  font-size: 12px;
  color: var(--color-text-secondary);
}
</style>
