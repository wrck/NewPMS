<script setup lang="ts">
/**
 * 设置页
 * - 修改密码
 * - 清除缓存
 * - 关于
 * - 退出登录
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  showConfirmDialog,
  showSuccessToast,
  showFailToast,
  showLoadingToast,
  closeToast,
  showToast
} from 'vant'
import { useUserStore } from '@/stores/user'
import { changePassword } from '@/api'
import { storage } from '@/utils/storage'
import { clearAllCachedFiles, getCachedFileCount } from '@/utils/indexeddb'

defineOptions({ name: 'MineSettings' })

const router = useRouter()
const userStore = useUserStore()

const cacheCount = ref(0)
const version = ref('1.0.0')

/* ============ 修改密码 ============ */
const pwdDialog = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const pwdSubmitting = ref(false)

function openPwdDialog() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdDialog.value = true
}

async function onChangePassword() {
  if (!pwdForm.newPassword) {
    showFailToast('请输入新密码')
    return
  }
  if (pwdForm.newPassword.length < 6 || pwdForm.newPassword.length > 64) {
    showFailToast('密码长度需在 6-64 之间')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    showFailToast('两次输入的密码不一致')
    return
  }
  const userId = userStore.userInfo?.id
  if (!userId) {
    showFailToast('用户信息缺失，请重新登录')
    return
  }
  pwdSubmitting.value = true
  showLoadingToast({ message: '提交中...', forbidClick: true, duration: 0 })
  try {
    await changePassword(userId, { newPassword: pwdForm.newPassword })
    closeToast()
    showSuccessToast('密码修改成功，请重新登录')
    pwdDialog.value = false
    // 修改密码后强制重新登录
    setTimeout(async () => {
      await userStore.logout()
      router.replace('/login')
    }, 1500)
  } catch (e) {
    closeToast()
    // request 拦截器已 toast 错误（可能 403 无权限）
  } finally {
    pwdSubmitting.value = false
  }
}

async function loadCacheCount() {
  try {
    cacheCount.value = await getCachedFileCount()
  } catch (e) {
    cacheCount.value = 0
  }
}

/** 清除缓存（IndexedDB + localStorage 中非登录态数据） */
async function onClearCache() {
  try {
    await showConfirmDialog({
      title: '清除缓存',
      message: `将清除 ${cacheCount.value} 个待上传文件缓存，确认清除？`
    })
    await clearAllCachedFiles()
    // 仅清理上传队列快照，保留登录态
    storage.remove('upload_queue_snapshot')
    storage.remove('last_location')
    cacheCount.value = 0
    showSuccessToast('缓存已清除')
  } catch (e) {
    // 取消
  }
}

/** 检查更新 */
function onCheckUpdate() {
  showToast('当前已是最新版本')
}

/** 关于 */
function onAbout() {
  showToast(`实施交付移动端 v${version.value}`)
}

/** 退出登录 */
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

onMounted(() => {
  loadCacheCount()
})
</script>

<template>
  <div class="settings-page page">
    <!-- 账号 -->
    <van-cell-group inset title="账号">
      <van-cell title="修改密码" is-link @click="openPwdDialog">
        <template #icon>
          <van-icon name="lock" class="cell-icon" color="#1677ff" />
        </template>
      </van-cell>
      <van-cell title="退出登录" is-link @click="onLogout">
        <template #icon>
          <van-icon name="cross" class="cell-icon" color="#ff4d4f" />
        </template>
      </van-cell>
    </van-cell-group>

    <!-- 缓存与存储 -->
    <van-cell-group inset title="缓存与存储">
      <van-cell title="待上传缓存" :value="`${cacheCount} 个文件`" is-link @click="onClearCache">
        <template #icon>
          <van-icon name="records" class="cell-icon" color="#1677ff" />
        </template>
      </van-cell>
      <van-cell title="清除缓存" is-link @click="onClearCache">
        <template #icon>
          <van-icon name="delete-o" class="cell-icon" color="#ff4d4f" />
        </template>
      </van-cell>
    </van-cell-group>

    <!-- 关于 -->
    <van-cell-group inset title="关于">
      <van-cell title="当前版本" :value="`v${version}`" is-link @click="onAbout">
        <template #icon>
          <van-icon name="info-o" class="cell-icon" color="#8c8c8c" />
        </template>
      </van-cell>
      <van-cell title="检查更新" is-link @click="onCheckUpdate">
        <template #icon>
          <van-icon name="upgrade" class="cell-icon" color="#52c41a" />
        </template>
      </van-cell>
      <van-cell title="用户协议" is-link>
        <template #icon>
          <van-icon name="description" class="cell-icon" color="#722ed1" />
        </template>
      </van-cell>
      <van-cell title="隐私政策" is-link>
        <template #icon>
          <van-icon name="lock" class="cell-icon" color="#faad14" />
        </template>
      </van-cell>
    </van-cell-group>

    <div class="footer-tip">
      <p>实施交付移动端 · 工程师现场作业平台</p>
      <p>© 2025 ServiceDeliver</p>
    </div>

    <!-- 修改密码弹窗 -->
    <van-dialog
      v-model:show="pwdDialog"
      title="修改密码"
      show-cancel-button
      :confirm-button-loading="pwdSubmitting"
      @confirm="onChangePassword"
    >
      <van-cell-group inset>
        <van-field
          v-model="pwdForm.oldPassword"
          type="password"
          label="原密码"
          placeholder="请输入原密码（验证用）"
        />
        <van-field
          v-model="pwdForm.newPassword"
          type="password"
          label="新密码"
          placeholder="6-64 位新密码"
          maxlength="64"
        />
        <van-field
          v-model="pwdForm.confirmPassword"
          type="password"
          label="确认密码"
          placeholder="请再次输入新密码"
          maxlength="64"
        />
      </van-cell-group>
      <p class="pwd-tip">提示：修改成功后将自动退出，请使用新密码重新登录。</p>
    </van-dialog>
  </div>
</template>

<style scoped lang="scss">
.settings-page {
  padding: 12px 0;
}

.cell-icon {
  margin-right: 8px;
  font-size: 18px;
}

.footer-tip {
  text-align: center;
  padding: 32px 16px;
  font-size: 12px;
  color: var(--color-text-placeholder);
  line-height: 1.8;

  p {
    margin: 0;
  }
}

.pwd-tip {
  margin: 8px 16px 12px;
  font-size: 12px;
  color: var(--color-text-placeholder);
  line-height: 1.6;
}
</style>
