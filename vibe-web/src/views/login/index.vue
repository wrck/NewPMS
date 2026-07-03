<script setup lang="ts">
/**
 * 登录页（账号密码表单）
 */
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Form, FormItem, Input, InputPassword, Button, Checkbox, message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import type { Rule } from 'ant-design-vue/es/form'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const remember = ref(true)

const formState = reactive({
  username: '',
  password: '',
  captcha: ''
})

const rules: Record<string, Rule[]> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

const formRef = ref()

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await userStore.login({
      username: formState.username,
      password: formState.password,
      remember: remember.value
    })
    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  } catch (e: any) {
    // 错误提示已在 request 拦截器静默处理，这里给兜底
    if (e?.message) {
      message.error(e.message)
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-bg" />
    <div class="login-container">
      <div class="login-left">
        <div class="brand">
          <img src="/favicon.svg" alt="logo" class="brand-logo" />
          <h1 class="brand-title">Vibe 交付管理平台</h1>
          <p class="brand-subtitle">专业 · 清晰 · 高效 · 可信赖</p>
        </div>
        <div class="brand-features">
          <div class="feature-item">
            <span class="feature-num">01</span>
            <div>
              <h3>项目全生命周期管理</h3>
              <p>立项、计划、执行、验收、归档全流程数字化</p>
            </div>
          </div>
          <div class="feature-item">
            <span class="feature-num">02</span>
            <div>
              <h3>资源智能调度</h3>
              <p>工程师排期、任务派发、工时统计一体化</p>
            </div>
          </div>
          <div class="feature-item">
            <span class="feature-num">03</span>
            <div>
              <h3>现场作业管控</h3>
              <p>移动端签到、施工步骤跟踪、拍照水印上传</p>
            </div>
          </div>
        </div>
      </div>

      <div class="login-right">
        <div class="login-card">
          <h2 class="login-title">账号登录</h2>
          <p class="login-desc">欢迎回来，请输入您的账号信息</p>

          <Form
            ref="formRef"
            :model="formState"
            :rules="rules"
            layout="vertical"
            size="large"
            @finish="handleSubmit"
          >
            <FormItem name="username">
              <Input
                v-model:value="formState.username"
                placeholder="请输入用户名"
                allow-clear
                @pressEnter="handleSubmit"
              >
                <template #prefix><UserOutlined /></template>
              </Input>
            </FormItem>

            <FormItem name="password">
              <InputPassword
                v-model:value="formState.password"
                placeholder="请输入密码"
                allow-clear
                @pressEnter="handleSubmit"
              >
                <template #prefix><LockOutlined /></template>
              </InputPassword>
            </FormItem>

            <FormItem name="captcha">
              <Input
                v-model:value="formState.captcha"
                placeholder="验证码（开发阶段可不填）"
                allow-clear
              >
                <template #prefix><SafetyOutlined /></template>
              </Input>
            </FormItem>

            <div class="login-options">
              <Checkbox v-model:checked="remember">记住我</Checkbox>
              <a class="forget-link">忘记密码？</a>
            </div>

            <Button
              type="primary"
              html-type="submit"
              size="large"
              block
              :loading="loading"
              class="login-btn"
            >
              登录
            </Button>
          </Form>

          <div class="login-tip">
            <span>提示：开发阶段可使用默认账号 admin / 123456</span>
          </div>
        </div>
        <p class="login-copyright">© 2026 Vibe ServiceDeliver. All rights reserved.</p>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
.login-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #e6f4ff 0%, #f5f5f5 100%);
}
.login-bg {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle at 20% 20%, rgba(22, 119, 255, 0.08) 0, transparent 40%),
    radial-gradient(circle at 80% 80%, rgba(114, 46, 209, 0.06) 0, transparent 40%);
}
.login-container {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 24px;
  gap: 80px;
}
.login-left {
  max-width: 480px;
  color: @text-primary;
}
.brand {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 48px;
}
.brand-logo {
  width: 56px;
  height: 56px;
}
.brand-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0;
  color: @brand-primary;
}
.brand-subtitle {
  font-size: 16px;
  color: @text-secondary;
  margin: 0;
}
.brand-features {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.feature-item {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  h3 {
    margin: 0 0 4px;
    font-size: 16px;
    color: @text-primary;
  }
  p {
    margin: 0;
    font-size: 13px;
    color: @text-tertiary;
  }
}
.feature-num {
  font-size: 24px;
  font-weight: 700;
  color: @brand-primary;
  font-family: 'DIN Alternate', sans-serif;
  opacity: 0.6;
}
.login-right {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.login-card {
  width: 400px;
  padding: 40px 36px 32px;
  background: #fff;
  border-radius: 12px;
  box-shadow: @shadow-modal;
}
.login-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px;
  color: @text-primary;
}
.login-desc {
  font-size: 14px;
  color: @text-tertiary;
  margin: 0 0 32px;
}
.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.forget-link {
  font-size: 13px;
}
.login-btn {
  height: 44px;
  font-size: 15px;
}
.login-tip {
  margin-top: 16px;
  padding: 8px 12px;
  background: #fffbe6;
  border-radius: 6px;
  font-size: 12px;
  color: @text-secondary;
  text-align: center;
}
.login-copyright {
  margin-top: 24px;
  font-size: 12px;
  color: @text-tertiary;
}

@media (max-width: 900px) {
  .login-left {
    display: none;
  }
}
</style>
