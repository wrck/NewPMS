<template>
  <div class="h5-agent-login">
    <div class="logo-area">
      <div class="logo-icon">🚚</div>
      <h1 class="title">代理商门户</h1>
      <p class="subtitle">接单 · 交付 · 提交 · 协作</p>
    </div>

    <div class="form-card">
      <a-form layout="vertical">
        <a-form-item label="账号" required>
          <a-input
            v-model:value="form.username"
            placeholder="请输入代理商账号"
            size="large"
            @press-enter="onLogin"
          >
            <template #prefix><UserOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item label="密码" required>
          <a-input-password
            v-model:value="form.password"
            placeholder="请输入密码"
            size="large"
            @press-enter="onLogin"
          >
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>
        <a-button
          type="primary"
          size="large"
          block
          :loading="loading"
          @click="onLogin"
        >
          登 录
        </a-button>
      </a-form>

      <div class="tips">
        <p>💡 测试账号：testagent / admin123（代理商管理员）</p>
        <p>💡 登录后可查看分配给本公司的任务并提交交付物</p>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { login as loginApi } from '@/api/auth'
import { useUserStore } from '@/stores'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

async function onLogin() {
  if (!form.username || !form.password) {
    message.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const res = await loginApi({
      username: form.username,
      password: form.password,
      clientId: 'H5_AGENT'
    })
    if (res?.token) {
      localStorage.setItem('vibe_token', res.token)
      if (res.refreshToken) {
        localStorage.setItem('vibe_refresh_token', res.refreshToken)
      }
      await userStore.fetchUserInfo()
      message.success('登录成功')
      const redirect = route.query.redirect
      if (typeof redirect === 'string' && redirect) {
        router.push(redirect)
      } else {
        router.push('/h5/agent/workbench')
      }
    } else {
      message.error('登录失败，请重试')
    }
  } catch (e: any) {
    console.error('[H5 agent login]', e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.h5-agent-login {
  min-height: 100vh;
  background: linear-gradient(180deg, #fa8c16 0%, #d4380d 50%, #f5f7fa 50%);
  padding: 24px 16px;
}

.logo-area {
  text-align: center;
  color: #fff;
  padding: 32px 0 24px;
}

.logo-icon {
  font-size: 48px;
  margin-bottom: 8px;
}

.title {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 8px;
}

.subtitle {
  font-size: 13px;
  opacity: 0.85;
  margin: 0;
}

.form-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px 20px 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.tips {
  margin-top: 16px;
  padding: 12px;
  background: #fff7e6;
  border-radius: 8px;
  font-size: 12px;
  color: #666;
  line-height: 1.6;
}

.tips p {
  margin: 0 0 4px;
}
</style>
