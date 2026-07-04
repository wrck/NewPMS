<template>
  <div class="h5-customer-login">
    <div class="logo-area">
      <div class="logo-icon">📱</div>
      <h1 class="title">客户协作门户</h1>
      <p class="subtitle">项目进度 · 割接审批 · 验收签核</p>
    </div>

    <div class="form-card">
      <a-tabs v-model:active-key="loginMode" centered>
        <a-tab-pane key="password" tab="账号密码" />
        <a-tab-pane key="sms" tab="手机验证码" disabled />
      </a-tabs>

      <a-form layout="vertical" v-if="loginMode === 'password'">
        <a-form-item label="账号" required>
          <a-input
            v-model:value="form.username"
            placeholder="请输入客户账号"
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
        <p>💡 测试账号：testcustomer / admin123</p>
        <p>💡 您也可以通过短信/邮件中的链接直接访问审批页面</p>
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

const loginMode = ref<'password' | 'sms'>('password')
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
      clientId: 'H5_CUSTOMER'
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
        router.push('/h5/customer/projects')
      }
    } else {
      message.error('登录失败，请重试')
    }
  } catch (e: any) {
    console.error('[H5 customer login]', e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.h5-customer-login {
  min-height: 100vh;
  background: linear-gradient(180deg, #1677ff 0%, #0958d9 50%, #f5f7fa 50%);
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
  background: #f0f5ff;
  border-radius: 8px;
  font-size: 12px;
  color: #666;
  line-height: 1.6;
}

.tips p {
  margin: 0 0 4px;
}
</style>
