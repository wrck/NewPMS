<template>
  <div class="customer-login">
    <div class="customer-login__brand">
      <div class="customer-login__logo">
        <van-icon name="checked" size="40" color="#1677ff" />
      </div>
      <h2>项目交付进度查询</h2>
      <p>请使用您预留的手机号或账号登录</p>
    </div>

    <van-tabs v-model:active="mode" shrink line-width="40" class="customer-login__tabs">
      <van-tab title="验证码登录" name="sms" />
      <van-tab title="账号密码登录" name="pwd" />
    </van-tabs>

    <van-form @submit="onSubmit" class="customer-login__form">
      <van-cell-group inset>
        <!-- 验证码模式 -->
        <template v-if="mode === 'sms'">
          <van-field
            v-model="phone"
            name="phone"
            label="手机号"
            placeholder="请输入手机号"
            type="tel"
            maxlength="11"
            clearable
            :rules="[
              { required: true, message: '请输入手机号' },
              { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
            ]"
          />
          <van-field
            v-model="smsCode"
            name="smsCode"
            label="验证码"
            placeholder="请输入短信验证码"
            maxlength="6"
            type="number"
            :rules="[{ required: true, message: '请输入验证码' }]"
          >
            <template #button>
              <van-button
                size="small"
                type="primary"
                plain
                :disabled="counting > 0 || !phoneOk"
                :loading="sending"
                @click.prevent="onSendCode"
              >
                {{ counting > 0 ? `${counting}s` : '获取验证码' }}
              </van-button>
            </template>
          </van-field>
        </template>

        <!-- 账号密码模式 -->
        <template v-else>
          <van-field
            v-model="username"
            name="username"
            label="账号"
            placeholder="请输入账号"
            clearable
            :rules="[{ required: true, message: '请输入账号' }]"
          />
          <van-field
            v-model="password"
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
        </template>
      </van-cell-group>

      <div class="customer-login__tips">
        <template v-if="mode === 'sms'">验证码有效期为 5 分钟，登录后 Token 有效期 2 小时</template>
        <template v-else>客户账号登录后 Token 有效期 2 小时</template>
      </div>

      <div class="customer-login__submit">
        <van-button block type="primary" native-type="submit" :loading="loading">
          登录
        </van-button>
      </div>
    </van-form>

    <div class="customer-login__footer">
      <a @click="goAgent">代理商入口</a>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useCustomerStore } from '@/stores/customer'

const route = useRoute()
const router = useRouter()
const customerStore = useCustomerStore()

const mode = ref<'sms' | 'pwd'>('sms')
const phone = ref('')
const smsCode = ref('')
const username = ref('')
const password = ref('')
const showPwd = ref(false)
const loading = ref(false)
const sending = ref(false)
const counting = ref(0)

let timer: ReturnType<typeof setInterval> | null = null

const phoneOk = computed(() => /^1[3-9]\d{9}$/.test(phone.value))

async function onSendCode() {
  if (!phoneOk.value) {
    showToast('请输入正确的手机号')
    return
  }
  sending.value = true
  try {
    await customerStore.sendSmsCode(phone.value)
    showToast('验证码已发送')
    counting.value = 60
    timer = setInterval(() => {
      counting.value--
      if (counting.value <= 0 && timer) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch (e) {
    // toast 已在拦截器中处理
  } finally {
    sending.value = false
  }
}

async function onSubmit() {
  loading.value = true
  try {
    if (mode.value === 'sms') {
      await customerStore.login({ phone: phone.value, smsCode: smsCode.value })
    } else {
      await customerStore.login({ username: username.value, password: password.value })
    }
    showToast('登录成功')
    const redirect = (route.query.redirect as string) || '/customer/progress'
    router.replace(redirect)
  } catch (e) {
    // toast 已在拦截器中处理
  } finally {
    loading.value = false
  }
}

function goAgent() {
  router.push('/agent/login')
}

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss" scoped>
.customer-login {
  min-height: 100%;
  padding: 0 0 calc(24px + var(--safe-bottom));
  background: linear-gradient(180deg, #e8f1ff 0%, #f5f7fa 240px);

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
    box-shadow: 0 4px 12px rgba(22, 119, 255, 0.18);
  }

  &__tabs {
    margin-top: 8px;
    :deep(.van-tabs__nav) {
      background: transparent;
    }
  }

  &__form {
    margin-top: 12px;
  }

  &__tips {
    padding: 12px 24px 0;
    font-size: 12px;
    color: var(--color-text-secondary);
  }

  &__submit {
    padding: 16px 24px 0;
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
