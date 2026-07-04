<template>
  <H5Layout title="割接方案审批" :show-back="true">
    <div v-if="loading" class="loading-area">
      <a-spin tip="加载方案中..." />
    </div>

    <div v-else-if="!plan" class="empty-area">
      <a-empty description="方案不存在或链接已失效" />
    </div>

    <div v-else class="cutover-detail">
      <!-- 方案基本信息 -->
      <div class="info-card">
        <h2 class="plan-name">{{ plan.planName }}</h2>
        <div class="info-row">
          <span class="label">割接日期</span>
          <span class="value">{{ plan.cutoverDate || '-' }}</span>
        </div>
        <div class="info-row">
          <span class="label">计划时间</span>
          <span class="value">
            {{ formatDateTime(plan.startTime) }} ~ {{ formatDateTime(plan.endTime) }}
          </span>
        </div>
        <div class="info-row" v-if="plan.impactScope">
          <span class="label">影响范围</span>
          <span class="value">{{ plan.impactScope }}</span>
        </div>
        <div class="info-row" v-if="plan.emergencyContact">
          <span class="label">紧急联系人</span>
          <span class="value">{{ plan.emergencyContact }}</span>
        </div>
        <div class="info-row">
          <span class="label">当前状态</span>
          <span class="status-tag" :class="`status-${plan.status.toLowerCase()}`">
            {{ formatPlanStatus(plan.status) }}
          </span>
        </div>
      </div>

      <!-- 已签核信息（如果已签核） -->
      <div v-if="plan.customerSignResult" class="signed-card">
        <div class="signed-icon">
          {{ plan.customerSignResult === 'APPROVED' ? '✅' : '❌' }}
        </div>
        <div class="signed-info">
          <div class="signed-title">
            {{ plan.customerSignResult === 'APPROVED' ? '您已同意' : '您已驳回' }}
          </div>
          <div class="signed-time" v-if="plan.customerSignTime">
            签核时间：{{ formatDateTime(plan.customerSignTime) }}
          </div>
          <div class="signed-remark" v-if="plan.customerSignRemark">
            意见：{{ plan.customerSignRemark }}
          </div>
        </div>
      </div>

      <!-- 步骤列表 -->
      <div class="section-title">
        <span>─── 割接步骤（{{ plan.completedStepCount || 0 }}/{{ plan.stepCount || plan.steps?.length || 0 }}） ───</span>
      </div>

      <div class="step-list" v-if="plan.steps?.length">
        <div
          v-for="(step, idx) in plan.steps"
          :key="idx"
          class="step-item"
        >
          <div class="step-num">{{ idx + 1 }}</div>
          <div class="step-content">
            <div class="step-header">
              <span class="step-name">{{ step.stepName }}</span>
              <span class="step-status" :class="`status-${step.status.toLowerCase()}`">
                {{ formatStepStatus(step.status) }}
              </span>
            </div>
            <div class="step-desc" v-if="step.description">{{ step.description }}</div>
            <div class="step-meta">
              <span v-if="step.ownerName">负责人：{{ step.ownerName }}</span>
              <span v-if="step.estimatedDuration">预计：{{ step.estimatedDuration }}分钟</span>
              <span v-if="step.actualDuration">实际：{{ step.actualDuration }}分钟</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 审批操作（仅在未签核且状态为 PENDING_CUSTOMER_APPROVAL 时显示） -->
      <div
        v-if="canApprove"
        class="approval-area"
      >
        <div class="section-title">
          <span>─── 提交审批 ───</span>
        </div>

        <div class="form-card">
          <a-form layout="vertical">
            <a-form-item label="审批结果" required>
              <a-radio-group v-model:value="form.result" button-style="solid">
                <a-radio-button value="APPROVED">✅ 同意</a-radio-button>
                <a-radio-button value="REJECTED">❌ 驳回</a-radio-button>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="审批意见">
              <a-textarea
                v-model:value="form.remark"
                placeholder="请输入审批意见（可选）"
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
              提交审批
            </a-button>
          </a-form>
        </div>
      </div>

      <!-- 需要登录提示 -->
      <div v-else-if="needLogin" class="login-tip">
        <div class="tip-icon">🔐</div>
        <div class="tip-text">提交审批需要先登录客户账号</div>
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
import { getCutoverPlanByToken, submitCutoverApproval } from '@/api/customerPortal'
import type { CustomerCutoverPlan } from '@/types/customerPortal'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const submitting = ref(false)
const needLogin = ref(false)
const plan = ref<CustomerCutoverPlan | null>(null)

const form = reactive({
  result: 'APPROVED' as 'APPROVED' | 'REJECTED',
  remark: ''
})

const token = computed(() => route.params.token as string)

/** 是否允许审批：状态为 PENDING_CUSTOMER_APPROVAL 且尚未签核 */
const canApprove = computed(() => {
  if (!plan.value) return false
  return plan.value.status === 'PENDING_CUSTOMER_APPROVAL' && !plan.value.customerSignResult
})

onMounted(async () => {
  if (!token.value) {
    message.error('token 缺失')
    router.back()
    return
  }
  loading.value = true
  try {
    plan.value = await getCutoverPlanByToken(token.value)
  } catch (e: any) {
    console.error('[H5 cutover approval]', e)
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
    message.warning('请选择审批结果')
    return
  }
  submitting.value = true
  try {
    await submitCutoverApproval({
      token: token.value,
      result: form.result,
      remark: form.remark || undefined
    })
    message.success(form.result === 'APPROVED' ? '已同意' : '已驳回')
    // 刷新方案状态
    plan.value = await getCutoverPlanByToken(token.value)
  } catch (e: any) {
    console.error('[H5 submit cutover approval]', e)
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

function formatPlanStatus(status: string): string {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    INTERNAL_APPROVAL: '内部审批中',
    APPROVED: '已通过',
    PENDING_CUSTOMER_APPROVAL: '待客户审批',
    CUSTOMER_APPROVED: '客户已同意',
    CUSTOMER_REJECTED: '客户已驳回',
    IN_PROGRESS: '执行中',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

function formatStepStatus(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待执行',
    IN_PROGRESS: '执行中',
    COMPLETED: '已完成',
    SKIPPED: '已跳过'
  }
  return map[status] || status
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

.plan-name {
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

.status-pending_customer_approval {
  background: #fff7e6;
  color: #fa8c16;
}

.status-customer_approved,
.status-completed {
  background: #f6ffed;
  color: #52c41a;
}

.status-customer_rejected,
.status-cancelled {
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

.signed-card:has(.signed-icon:contains('❌')) {
  background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%);
  border-color: #ffa39e;
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

.step-list {
  background: #fff;
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 12px;
}

.step-item {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}

.step-item:last-child {
  border-bottom: none;
}

.step-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #1677ff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.step-content {
  flex: 1;
  min-width: 0;
}

.step-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.step-name {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
}

.step-status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #f0f0f0;
  color: #666;
}

.step-status.status-in_progress {
  background: #e6f4ff;
  color: #1677ff;
}

.step-status.status-completed {
  background: #f6ffed;
  color: #52c41a;
}

.step-desc {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.step-meta {
  font-size: 11px;
  color: #999;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.approval-area {
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
