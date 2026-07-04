<template>
  <H5Layout title="代理商工作台">
    <template #right>
      <Badge :count="unreadCount" :offset="[-4, 4]" @click="goMessages">
        <BellOutlined style="font-size: 18px; color: #fff" />
      </Badge>
    </template>

    <div v-if="loading" class="loading-area">
      <a-spin />
    </div>

    <div v-else-if="!workbench" class="empty-area">
      <a-empty description="加载失败" />
    </div>

    <div v-else class="workbench">
      <!-- 统计卡片 -->
      <div class="summary-cards">
        <div class="summary-card card-pending">
          <div class="summary-num">{{ summary.pendingCount ?? 0 }}</div>
          <div class="summary-label">待接单</div>
        </div>
        <div class="summary-card card-in-progress">
          <div class="summary-num">{{ summary.inProgressCount ?? 0 }}</div>
          <div class="summary-label">进行中</div>
        </div>
        <div class="summary-card card-submitted">
          <div class="summary-num">{{ summary.submittedCount ?? 0 }}</div>
          <div class="summary-label">待审核</div>
        </div>
        <div class="summary-card card-overdue">
          <div class="summary-num">{{ summary.overdueCount ?? 0 }}</div>
          <div class="summary-label">已超期</div>
        </div>
      </div>

      <!-- 待接单任务 -->
      <div class="task-section" v-if="pendingTasks.length">
        <div class="section-header">
          <span class="section-title">待接单（{{ pendingTasks.length }}）</span>
        </div>
        <div
          v-for="task in pendingTasks"
          :key="task.id"
          class="task-card"
          @click="goTaskDetail(task.id)"
        >
          <div class="task-header">
            <span class="task-name">{{ task.taskName || task.projectName || '-' }}</span>
            <span class="task-status status-pending">待接单</span>
          </div>
          <div class="task-meta">
            <span v-if="task.projectName">项目：{{ task.projectName }}</span>
            <span v-if="task.deadline">截止：{{ task.deadline }}</span>
          </div>
          <div class="task-action-row">
            <a-button type="primary" size="small" @click.stop="onAccept(task)">接单</a-button>
            <a-button size="small" @click.stop="onReject(task)">拒绝</a-button>
            <a-button type="link" size="small">查看详情</a-button>
          </div>
        </div>
      </div>

      <!-- 进行中任务 -->
      <div class="task-section" v-if="inProgressTasks.length">
        <div class="section-header">
          <span class="section-title">进行中（{{ inProgressTasks.length }}）</span>
        </div>
        <div
          v-for="task in inProgressTasks"
          :key="task.id"
          class="task-card"
          @click="goTaskDetail(task.id)"
        >
          <div class="task-header">
            <span class="task-name">{{ task.taskName || task.projectName || '-' }}</span>
            <span class="task-status status-in-progress">进行中</span>
          </div>
          <div class="task-meta">
            <span v-if="task.agentEngineerName">执行人：{{ task.agentEngineerName }}</span>
            <span v-if="task.deadline">截止：{{ task.deadline }}</span>
          </div>
          <div class="task-action-row">
            <a-button type="primary" size="small" @click.stop="goSubmitDeliverable(task)">
              提交交付物
            </a-button>
            <a-button type="link" size="small">查看详情</a-button>
          </div>
        </div>
      </div>

      <!-- 待审核任务 -->
      <div class="task-section" v-if="submittedTasks.length">
        <div class="section-header">
          <span class="section-title">待审核（{{ submittedTasks.length }}）</span>
        </div>
        <div
          v-for="task in submittedTasks"
          :key="task.id"
          class="task-card"
          @click="goTaskDetail(task.id)"
        >
          <div class="task-header">
            <span class="task-name">{{ task.taskName || task.projectName || '-' }}</span>
            <span class="task-status status-submitted">PM审核中</span>
          </div>
          <div class="task-meta">
            <span v-if="task.updateTime">提交：{{ formatDateTime(task.updateTime) }}</span>
            <span v-if="task.submitCount">第{{ task.submitCount }}次提交</span>
          </div>
        </div>
      </div>

      <div
        v-if="!pendingTasks.length && !inProgressTasks.length && !submittedTasks.length"
        class="empty-area"
      >
        <a-empty description="暂无任务" />
      </div>
    </div>
  </H5Layout>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { BellOutlined } from '@ant-design/icons-vue'
import { Badge } from 'ant-design-vue'
import H5Layout from '@/layouts/H5Layout.vue'
import { getAgentWorkbench, getAgentUnreadCount } from '@/api/agentPortal'
import { respondOutsourceTask } from '@/api/agent'
import type { AgentWorkbench, AgentOutsourceTaskItem } from '@/types/agentPortal'

const router = useRouter()

const loading = ref(true)
const workbench = ref<AgentWorkbench | null>(null)
const unreadCount = ref(0)

const summary = computed(() => workbench.value?.summary || {})
const pendingTasks = computed(() => workbench.value?.pendingTasks || [])
const inProgressTasks = computed(() => workbench.value?.inProgressTasks || [])
const submittedTasks = computed(() => workbench.value?.submittedTasks || [])

onMounted(async () => {
  loading.value = true
  try {
    const [wb, unread] = await Promise.all([
      getAgentWorkbench(),
      getAgentUnreadCount().catch(() => 0)
    ])
    workbench.value = wb
    unreadCount.value = (unread as number) || 0
  } catch (e: any) {
    console.error('[H5 agent workbench]', e)
    if (e?.code === 401 || e?.code === 40301) {
      router.push({
        path: '/h5/agent/login',
        query: { redirect: '/h5/agent/workbench' }
      })
    } else {
      message.error(e?.message || '加载失败')
    }
  } finally {
    loading.value = false
  }
})

function goMessages() {
  router.push('/h5/agent/messages')
}

function goTaskDetail(taskId: number) {
  // 跳转到任务详情（这里简单复用 PC 端路由，若需要可独立实现）
  router.push(`/agent/outsource?id=${taskId}`)
}

function goSubmitDeliverable(task: AgentOutsourceTaskItem) {
  router.push(`/h5/agent/deliverable-submit?taskId=${task.id}`)
}

async function onAccept(task: AgentOutsourceTaskItem) {
  Modal.confirm({
    title: '确认接单',
    content: `确定接受任务「${task.taskName || task.projectName}」吗？`,
    okText: '确定接单',
    cancelText: '取消',
    onOk: async () => {
      try {
        await respondOutsourceTask(task.id, true)
        message.success('接单成功')
        await refreshWorkbench()
      } catch (e: any) {
        message.error(e?.message || '接单失败')
      }
    }
  })
}

function onReject(task: AgentOutsourceTaskItem) {
  let reason = ''
  Modal.confirm({
    title: '拒绝任务',
    content: () =>
      h(
        'div',
        { style: 'padding-top:8px' },
        [
          h('div', { style: 'margin-bottom:8px' }, `确定拒绝任务「${task.taskName || task.projectName}」吗？`),
          h('textarea', {
            placeholder: '请输入拒绝原因（建议填写）',
            style: 'width:100%;min-height:60px;padding:6px;border:1px solid #d9d9d9;border-radius:4px;',
            onInput: (e: any) => {
              reason = e.target.value
            }
          })
        ]
      ),
    okText: '确定拒绝',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await respondOutsourceTask(task.id, false, reason || undefined)
        message.success('已拒绝')
        await refreshWorkbench()
      } catch (e: any) {
        message.error(e?.message || '操作失败')
      }
    }
  })
}

async function refreshWorkbench() {
  try {
    workbench.value = await getAgentWorkbench()
  } catch (e) {
    console.error('[refresh workbench]', e)
  }
}

function formatDateTime(dt?: string): string {
  if (!dt) return ''
  return dt.replace('T', ' ').substring(0, 16)
}

// 引入 h 函数用于 Modal 自定义内容
import { h } from 'vue'
</script>

<style scoped>
.loading-area,
.empty-area {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 0;
}

.summary-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 16px;
}

.summary-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  text-align: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  border-left: 3px solid #1677ff;
}

.card-pending {
  border-left-color: #fa8c16;
}

.card-in-progress {
  border-left-color: #1677ff;
}

.card-submitted {
  border-left-color: #722ed1;
}

.card-overdue {
  border-left-color: #ff4d4f;
}

.summary-num {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1;
}

.card-pending .summary-num {
  color: #fa8c16;
}

.card-in-progress .summary-num {
  color: #1677ff;
}

.card-submitted .summary-num {
  color: #722ed1;
}

.card-overdue .summary-num {
  color: #ff4d4f;
}

.summary-label {
  font-size: 12px;
  color: #888;
  margin-top: 4px;
}

.task-section {
  margin-bottom: 16px;
}

.section-header {
  padding: 8px 4px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.task-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: transform 0.15s;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.task-card:active {
  transform: scale(0.98);
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.task-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #f0f0f0;
  color: #666;
  margin-left: 8px;
}

.status-pending {
  background: #fff7e6;
  color: #fa8c16;
}

.status-in-progress {
  background: #e6f4ff;
  color: #1677ff;
}

.status-submitted {
  background: #f9f0ff;
  color: #722ed1;
}

.task-meta {
  font-size: 12px;
  color: #888;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.task-action-row {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f5f5f5;
}
</style>
