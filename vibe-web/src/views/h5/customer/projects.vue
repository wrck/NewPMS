<template>
  <H5Layout title="我的项目">
    <template #right>
      <Badge :count="unreadCount" :offset="[-4, 4]" @click="goMessages">
        <BellOutlined style="font-size: 18px; color: #fff" />
      </Badge>
    </template>

    <div v-if="loading" class="loading-area">
      <a-spin />
    </div>

    <div v-else-if="projects.length === 0" class="empty-area">
      <a-empty description="暂无项目" />
    </div>

    <div v-else class="project-list">
      <div
        v-for="p in projects"
        :key="p.projectId"
        class="project-card"
        @click="goProgress(p.projectId)"
      >
        <div class="card-header">
          <span class="project-code">{{ p.projectCode }}</span>
          <span class="status-tag" :class="`status-${p.status.toLowerCase()}`">
            {{ formatStatus(p.status) }}
          </span>
        </div>
        <h3 class="project-name">{{ p.projectName }}</h3>
        <div class="project-meta">
          <span>类型：{{ p.projectType || '-' }}</span>
          <span>当前阶段：{{ formatPhase(p.currentPhase) }}</span>
        </div>
        <div class="progress-area">
          <div class="progress-label">
            <span>整体进度</span>
            <span class="progress-value">{{ p.progressPct }}%</span>
          </div>
          <a-progress
            :percent="p.progressPct"
            :show-info="false"
            size="small"
            :stroke-color="getProgressColor(p.progressPct)"
          />
        </div>
        <div class="project-meta" v-if="p.plannedEnd">
          <span>预计完成：{{ p.plannedEnd }}</span>
        </div>
      </div>

      <div class="todo-entry" @click="goTodos" v-if="todoCount > 0">
        <div class="todo-icon">📋</div>
        <div class="todo-text">
          <div class="todo-title">{{ todoCount }} 项待办</div>
          <div class="todo-desc">点击查看需审批/签核的事项</div>
        </div>
        <RightOutlined class="todo-arrow" />
      </div>
    </div>
  </H5Layout>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { BellOutlined, RightOutlined } from '@ant-design/icons-vue'
import { Badge } from 'ant-design-vue'
import H5Layout from '@/layouts/H5Layout.vue'
import { getMyProjects, getMyTodos, getUnreadMessageCount } from '@/api/customerPortal'
import type { CustomerProject } from '@/types/customerPortal'

const router = useRouter()

const loading = ref(true)
const projects = ref<CustomerProject[]>([])
const unreadCount = ref(0)
const todoCount = ref(0)

onMounted(async () => {
  loading.value = true
  try {
    const [pros, unread, todos] = await Promise.all([
      getMyProjects(),
      getUnreadMessageCount().catch(() => 0),
      getMyTodos().catch(() => [])
    ])
    projects.value = pros || []
    unreadCount.value = (unread as number) || 0
    todoCount.value = (todos as any[])?.length || 0
  } catch (e: any) {
    console.error('[H5 customer projects]', e)
    if (e?.code === 401 || e?.code === 40301) {
      router.push('/h5/customer/login')
    } else {
      message.error(e?.message || '加载失败')
    }
  } finally {
    loading.value = false
  }
})

function goProgress(projectId: number) {
  router.push(`/h5/customer/projects/${projectId}/progress`)
}

function goMessages() {
  router.push('/h5/customer/messages')
}

function goTodos() {
  router.push('/h5/customer/todos')
}

function formatStatus(status: string): string {
  const map: Record<string, string> = {
    INIT: '初始化',
    PLAN: '规划中',
    EXECUTE: '实施中',
    ACCEPT: '验收中',
    CLOSE: '已关闭',
    ARCHIVED: '已归档',
    ON_HOLD: '已暂停',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

function formatPhase(phase?: string): string {
  if (!phase) return '-'
  const map: Record<string, string> = {
    SURVEY: '调研',
    DESIGN: '设计',
    DELIVER: '交付',
    INSTALL: '安装',
    DEBUG: '调试',
    ACCEPT: '验收'
  }
  return map[phase] || phase
}

function getProgressColor(pct: number): string {
  if (pct >= 80) return '#52c41a'
  if (pct >= 40) return '#1677ff'
  if (pct >= 20) return '#faad14'
  return '#ff4d4f'
}
</script>

<style scoped>
.loading-area,
.empty-area {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px 0;
}

.project-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.project-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: transform 0.15s;
}

.project-card:active {
  transform: scale(0.98);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.project-code {
  font-size: 12px;
  color: #1677ff;
  font-weight: 500;
}

.status-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #f0f0f0;
  color: #666;
}

.status-execute {
  background: #e6f4ff;
  color: #1677ff;
}

.status-accept {
  background: #fff7e6;
  color: #fa8c16;
}

.status-close,
.status-archived {
  background: #f6ffed;
  color: #52c41a;
}

.status-on_hold,
.status-cancelled {
  background: #fff1f0;
  color: #ff4d4f;
}

.project-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 8px;
  line-height: 1.4;
}

.project-meta {
  font-size: 12px;
  color: #888;
  margin-bottom: 4px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.progress-area {
  margin-top: 10px;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.progress-value {
  color: #1677ff;
  font-weight: 600;
}

.todo-entry {
  background: linear-gradient(135deg, #fff7e6 0%, #fff1f0 100%);
  border-radius: 12px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
  cursor: pointer;
  border: 1px solid #ffd591;
}

.todo-entry:active {
  transform: scale(0.98);
}

.todo-icon {
  font-size: 24px;
}

.todo-text {
  flex: 1;
}

.todo-title {
  font-size: 14px;
  font-weight: 600;
  color: #d4380d;
}

.todo-desc {
  font-size: 12px;
  color: #888;
  margin-top: 2px;
}

.todo-arrow {
  color: #d4380d;
  font-size: 14px;
}
</style>
