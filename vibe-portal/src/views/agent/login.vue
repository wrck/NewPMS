<template>
  <div class="agent-login">
    <div class="agent-login__brand">
      <div class="agent-login__logo">
        <van-icon name="friends-o" size="40" color="#722ed1" />
      </div>
      <h2>代理商工作台</h2>
      <p>请使用代理商账号登录</p>
    </div>

    <van-form @submit="onSubmit" class="agent-login__form">
      <van-cell-group inset>
        <van-field
          v-model="form.username"
          name="username"
          label="账号"
          placeholder="请输入账号"
          clearable
          :rules="[{ required: true, message: '请输入账号' }]"
        />
        <van-field
          v-model="form.password"
          name="password"
          label="密码"
          placeholder="请输入密码"
          :type="showPwd ? 'text' : 'password'"
          :rules="[{ required: true, message: '请输入密码' }]"
        >
          <template #right-icon>
            <van-icon
              :name="showPwd ? 'eye-o' : 'closed-eye'"
              size="18"
              color="#8c8c8c"
              @click="showPwd = !showPwd"
            />
          </template>
        </van-field>
      </van-cell-group>

      <div class="agent-login__tips">
        代理商账号登录后仅可见本公司任务，客户/合同/成本等敏感信息将被屏蔽
      </div>

      <div class="agent-login__submit">
        <van-button block type="primary" native-type="submit" :loading="loading">
          登录
        </van-button>
      </div>
    </van-form>

    <div class="agent-login__footer">
      <a @click="goCustomer">客户入口</a>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useAgentStore } from '@/stores/agent'

const route = useRoute()
const router = useRouter()
const agentStore = useAgentStore()

const form = reactive({
  username: '',
  password: ''
})
const showPwd = ref(false)
const loading = ref(false)

async function onSubmit() {
  loading.value = true
  try {
    await agentStore.login({ username: form.username, password: form.password })
    showToast('登录成功')
    const redirect = (route.query.redirect as string) || '/agent/dashboard'
    router.replace(redirect)
  } catch (e) {
    // toast 已在拦截器中处理
  } finally {
    loading.value = false
  }
}

function goCustomer() {
  router.push('/customer/login')
}
</script>

<style lang="scss" scoped>
.agent-login {
  min-height: 100%;
  padding: 0 0 calc(24px + var(--safe-bottom));
  background: linear-gradient(180deg, #f3eaff 0%, #f5f7fa 240px);

  &__brand {
    text-align: center;
    padding: 48px 24px 24px;

    h2 {
      margin-top: 12px;
      font-size: 20px;
      color: var(--color-text-primary);
    }

    p {
      margin-top: 8px;
      font-size: 13px;
      color: var(--color-text-secondary);
    }
  }

  &__logo {
    width: 72px;
    height: 72px;
    border-radius: 16px;
    background: #fff;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 12px rgba(114, 46, 209, 0.18);
  }

  &__form {
    margin-top: 16px;
  }

  &__tips {
    padding: 12px 24px 0;
    font-size: 12px;
    color: var(--color-text-secondary);
    line-height: 1.6;
  }

  &__submit {
    padding: 16px 24px 0;

    :deep(.van-button--primary) {
      background: var(--agent-primary);
      border-color: var(--agent-primary);
    }
  }

  &__footer {
    margin-top: 24px;
    text-align: center;
    font-size: 13px;

    a {
      color: var(--color-text-secondary);
      text-decoration: underline;
    }
  }
}
</style>
