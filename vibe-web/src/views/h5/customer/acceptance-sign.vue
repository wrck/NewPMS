<template>
  <H5Layout title="验收签核" :show-back="true">
    <div v-if="loading" class="loading-area">
      <a-spin tip="加载验收任务中..." />
    </div>

    <div v-else-if="!task" class="empty-area">
      <a-empty description="任务不存在或链接已失效" />
    </div>

    <div v-else class="acceptance-detail">
      <!-- 任务基本信息 -->
      <div class="info-card">
        <h2 class="task-name">{{ task.name }}</h2>
        <div class="info-row">
          <span class="label">当前状态</span>
          <span class="status-tag" :class="`status-${task.status.toLowerCase()}`">
            {{ formatTaskStatus(task.status) }}
          </span>
        </div>
        <div class="info-row" v-if="task.applyTime">
          <span class="label">申请时间</span>
          <span class="value">{{ formatDateTime(task.applyTime) }}</span>
        </div>
        <div class="info-row" v-if="task.internalAuditTime">
          <span class="label">内部审核</span>
          <span class="value">{{ formatDateTime(task.internalAuditTime) }}</span>
        </div>
      </div>

      <!-- 已签核信息 -->
      <div v-if="task.customerSignResult" class="signed-card">
        <div class="signed-icon">
          {{ getSignResultIcon(task.customerSignResult) }}
        </div>
        <div class="signed-info">
          <div class="signed-title">{{ formatSignResult(task.customerSignResult) }}</div>
          <div class="signed-time" v-if="task.customerSignTime">
            签核时间：{{ formatDateTime(task.customerSignTime) }}
          </div>
          <div class="signed-remark" v-if="task.customerSignRemark">
            意见：{{ task.customerSignRemark }}
          </div>
        </div>
      </div>

      <!-- 测试记录 -->
      <div class="section-title">
        <span>─── 测试记录（{{ task.testRecords?.length || 0 }}项） ───</span>
      </div>

      <div v-if="task.testRecords?.length" class="test-list">
        <div
          v-for="(record, idx) in task.testRecords"
          :key="idx"
          class="test-item"
        >
          <div class="test-header">
            <div class="test-name">{{ record.testName }}</div>
            <div class="test-result" :class="`result-${record.testResult.toLowerCase()}`">
              {{ formatTestResult(record.testResult) }}
            </div>
          </div>
          <div class="test-meta">
            <span class="test-type">{{ formatTestType(record.testType) }}</span>
            <span v-if="record.testValue">值：{{ record.testValue }}</span>
            <span v-if="record.testTime">{{ formatDateTime(record.testTime) }}</span>
          </div>
          <div class="test-remark" v-if="record.remark">{{ record.remark }}</div>
        </div>
      </div>

      <div v-else class="empty-area">
        <a-empty description="暂无测试记录" />
      </div>

      <!-- 签核操作 -->
      <div v-if="canSign" class="sign-area">
        <div class="section-title">
          <span>─── 提交签核 ───</span>
        </div>

        <div class="form-card">
          <a-form layout="vertical">
            <a-form-item label="签核结果" required>
              <a-radio-group v-model:value="form.result" button-style="solid">
                <a-radio-button value="PASS">✅ 通过</a-radio-button>
                <a-radio-button value="CONDITIONAL_PASS">⚠️ 有条件通过</a-radio-button>
                <a-radio-button value="REJECT">❌ 不通过</a-radio-button>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="签核意见">
              <a-textarea
                v-model:value="form.remark"
                placeholder="请输入签核意见（条件通过/不通过时建议填写）"
                :rows="3"
                :maxlength="500"
                show-count
              />
            </a-form-item>
            <a-button
              type="primary"
              size="large"
              block
              :loading="submitting"
              @click="onSubmit"
            >
              提交签核
            </a-button>
          </a-form>
        </div>
      </div>

      <!-- 需要登录提示 -->
      <div v-else-if="needLogin" class="login-tip">
        <div class="tip-icon">🔐</div>
        <div class="tip-text">提交签核需要先登录客户账号</div>
        <a-button type="primary" @click="goLogin">前往登录</a-button>
      </div>
    </div>
  </H5Layout>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import H5Layout from '@/layouts/H5Layout.vue'
import { getAcceptanceTaskByToken, submitAcceptanceSign } from '@/api/customerPortal'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const submitting = ref(false)
const needLogin = ref(false)
const task = ref<any>(null)

const form = reactive({
  result: 'PASS' as 'PASS' | 'CONDITIONAL_PASS' | 'REJECT',
  remark: ''
})

const token = computed(() => route.params.token as string)

const canSign = computed(() => {
  if (!task.value) return false
  return task.value.status === 'CUSTOMER_SIGNING' && !task.value.customerSignResult
})

onMounted(async () => {
  if (!token.value) {
    message.error('token 缺失')
    router.back()
    return
  }
  loading.value = true
  try {
    task.value = await getAcceptanceTaskByToken(token.value)
  } catch (e: any) {
    console.error('[H5 acceptance sign]', e)
    if (e?.code === 401 || e?.code === 40301) {
      needLogin.value = true
    } else {
      message.error(e?.message || '加载失败')
    }
  } finally {
    loading.value = false
  }
})

async function onSubmit() {
  if (!form.result) {
    message.warning('请选择签核结果')
    return
  }
  submitting.value = true
  try {
    await submitAcceptanceSign({
      token: token.value,
      result: form.result,
      remark: form.remark || undefined
    })
    message.success('签核已提交')
    task.value = await getAcceptanceTaskByToken(token.value)
  } catch (e: any) {
    console.error('[H5 submit acceptance sign]', e)
    if (e?.code === 401 || e?.code === 40301) {
      needLogin.value = true
      message.warning('请先登录客户账号')
    } else {
      message.error(e?.message || '提交失败')
    }
  } finally {
    submitting.value = false
  }
}

function goLogin() {
  router.push({
    path: '/h5/customer/login',
    query: { redirect: route.fullPath }
  })
}

function formatTaskStatus(status: string): string {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    APPLIED: '已申请',
    INTERNAL_AUDITED: '内部审核通过',
    CUSTOMER_SIGNING: '待客户签核',
    COMPLETED: '已完成',
    REJECTED: '已驳回'
  }
  return map[status] || status
}

function formatTestType(type: string): string {
  const map: Record<string, string> = {
    FUNCTION: '功能测试',
    PERFORMANCE: '性能测试',
    REDUNDANCY: '冗余测试',
    OTHER: '其他'
  }
  return map[type] || type
}

function formatTestResult(result: string): string {
  const map: Record<string, string> = {
    PENDING: '待测',
    PASS: '通过',
    FAIL: '不通过',
    NA: '不适用'
  }
  return map[result] || result
}

function formatSignResult(result: string): string {
  const map: Record<string, string> = {
    PASS: '✅ 您已通过',
    CONDITIONAL_PASS: '⚠️ 您有条件通过',
    REJECT: '❌ 您已驳回'
  }
  return map[result] || result
}

function getSignResultIcon(result: string): string {
  if (result === 'PASS') return '✅'
  if (result === 'CONDITIONAL_PASS') return '⚠️'
  if (result === 'REJECT') return '❌'
  return '📄'
}

function formatDateTime(dt?: string): string {
  if (!dt) return '-'
  return dt.replace('T', ' ').substring(0, 16)
}
</script>

<style scoped>
.loading-area,
.empty-area {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px 0;
  flex-direction: column;
  gap: 12px;
}

.info-card,
.form-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.task-name {
  font-size: 17px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 12px;
  line-height: 1.4;
}

.info-row {
  display: flex;
  padding: 6px 0;
  font-size: 13px;
}

.info-row .label {
  width: 80px;
  color: #888;
  flex-shrink: 0;
}

.info-row .value {
  flex: 1;
  color: #1a1a1a;
  word-break: break-all;
}

.status-tag {
  display: inline-block;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #f0f0f0;
  color: #666;
}

.status-customer_signing {
  background: #fff7e6;
  color: #fa8c16;
}

.status-completed {
  background: #f6ffed;
  color: #52c41a;
}

.status-rejected {
  background: #fff1f0;
  color: #ff4d4f;
}

.signed-card {
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid #b7eb8f;
}

.signed-icon {
  font-size: 32px;
}

.signed-info {
  flex: 1;
}

.signed-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.signed-time,
.signed-remark {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.section-title {
  text-align: center;
  font-size: 13px;
  color: #999;
  margin: 16px 0 12px;
}

.test-list {
  background: #fff;
  border-radius: 12px;
  padding: 4px 16px;
  margin-bottom: 12px;
}

.test-item {
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.test-item:last-child {
  border-bottom: none;
}

.test-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.test-name {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
}

.test-result {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #f0f0f0;
  color: #666;
}

.test-result.result-pass {
  background: #f6ffed;
  color: #52c41a;
}

.test-result.result-fail {
  background: #fff1f0;
  color: #ff4d4f;
}

.test-result.result-pending {
  background: #fff7e6;
  color: #fa8c16;
}

.test-meta {
  font-size: 11px;
  color: #999;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.test-remark {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
  padding: 6px 8px;
  background: #f9f9f9;
  border-radius: 6px;
}

.sign-area {
  margin-top: 16px;
}

.login-tip {
  background: #fff;
  border-radius: 12px;
  padding: 32px 16px;
  text-align: center;
  margin-top: 16px;
}

.tip-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.tip-text {
  font-size: 14px;
  color: #666;
  margin-bottom: 16px;
}
</style>
