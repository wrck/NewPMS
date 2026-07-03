<script setup lang="ts">
/**
 * 登录页（账号密码）
 */
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showFailToast } from 'vant'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  username: '',
  password: ''
})

const loading = ref(false)

async function onSubmit() {
  if (!form.username) {
    showFailToast('请输入用户名')
    return
  }
  if (!form.password) {
    showFailToast('请输入密码')
    return
  }
  loading.value = true
  try {
    await userStore.login({ ...form })
    showSuccessToast('登录成功')
    const redirect = (route.query.redirect as string) || '/home'
    router.replace(decodeURIComponent(redirect))
  } catch (err) {
    // request 拦截器已 toast 错误
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-page__header">
      <div class="logo">实施交付</div>
      <p class="subtitle">工程师现场作业平台</p>
    </div>

    <div class="login-page__form">
      <van-cell-group inset>
        <van-field
          v-model="form.username"
          label="账号"
          placeholder="请输入用户名"
          clearable
          left-icon="manager-o"
        />
        <van-field
          v-model="form.password"
          type="password"
          label="密码"
          placeholder="请输入密码"
          clearable
          left-icon="lock"
          @keyup.enter="onSubmit"
        />
      </van-cell-group>

      <div class="login-page__action">
        <van-button
          type="primary"
          block
          round
          :loading="loading"
          class="login-btn touchable"
          @click="onSubmit"
        >
          登录
        </van-button>
      </div>

      <div class="login-page__tips">
        <span>移动端登录 Token 有效期 7 天</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  min-height: 100%;
  background: linear-gradient(180deg, #e8f1ff 0%, #f5f7fa 35%);
  padding: 0 16px;
  display: flex;
  flex-direction: column;

  &__header {
    text-align: center;
    padding: 80px 0 40px;

    .logo {
      font-size: 28px;
      font-weight: 700;
      color: var(--brand-primary);
      letter-spacing: 2px;
    }

    .subtitle {
      margin-top: 8px;
      font-size: 14px;
      color: var(--color-text-secondary);
    }
  }

  &__form {
    background: #fff;
    border-radius: var(--radius-lg);
    padding: 20px 0;
    box-shadow: 0 4px 20px rgba(22, 119, 255, 0.08);
  }

  &__action {
    padding: 20px 16px 8px;
  }

  &__tips {
    text-align: center;
    font-size: 12px;
    color: var(--color-text-placeholder);
    padding-bottom: 8px;
  }
}
</style>
