<template>
  <H5Layout title="项目进度" :show-back="true">
    <template #right>
      <FileTextOutlined
        style="font-size: 18px; color: #fff"
        @click="goDocuments"
      />
    </template>

    <div v-if="loading" class="loading-area">
      <a-spin />
    </div>

    <div v-else-if="!progress" class="empty-area">
      <a-empty description="未找到项目" />
    </div>

    <div v-else class="progress-detail">
      <!-- 项目概览卡片 -->
      <div class="overview-card">
        <h2 class="project-name">{{ progress.projectName }}</h2>
        <div class="overall-progress">
          <div class="progress-circle">
            <a-progress
              type="circle"
              :percent="progress.progressPct"
              :size="80"
              :stroke-color="getProgressColor(progress.progressPct)"
            />
          </div>
          <div class="progress-info">
            <div class="progress-num">{{ progress.progressPct }}%</div>
            <div class="progress-label">整体进度</div>
            <div class="phase-name" v-if="progress.currentPhaseName">
              当前阶段：{{ progress.currentPhaseName }}
            </div>
            <div class="status-tag" :class="`status-${progress.overallStatus.toLowerCase()}`">
              {{ formatStatus(progress.overallStatus) }}
            </div>
          </div>
        </div>
      </div>

      <!-- 各阶段进展 -->
      <div class="section-title">
        <span>─── 各阶段进展 ───</span>
      </div>

      <div class="phase-timeline" v-if="progress.phases?.length">
        <div
          v-for="(phase, idx) in progress.phases"
          :key="idx"
          class="phase-item"
        >
          <div class="phase-marker">
            <div class="marker-icon" :class="`marker-${phase.status.toLowerCase()}`">
              {{ getPhaseIcon(phase.status) }}
            </div>
            <div class="marker-line" v-if="idx < progress.phases.length - 1"></div>
          </div>
          <div class="phase-content">
            <div class="phase-header">
              <span class="phase-name">{{ phase.phaseName }}</span>
              <span class="phase-status" :class="`status-${phase.status.toLowerCase()}`">
                {{ formatPhaseStatus(phase.status) }}
              </span>
            </div>
            <div class="phase-meta" v-if="phase.progressPct !== undefined">
              <a-progress
                :percent="phase.progressPct"
                :show-info="true"
                size="small"
                :stroke-color="getProgressColor(phase.progressPct)"
              />
            </div>
            <div class="phase-meta">
              <span v-if="phase.actualEnd">完成：{{ phase.actualEnd }}</span>
              <span v-else-if="phase.actualStart">进行中（{{ phase.actualStart }} 起）</span>
              <span v-else-if="phase.plannedStart">计划：{{ phase.plannedStart }} ~ {{ phase.plannedEnd }}</span>
              <span v-else>待定</span>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="empty-area">
        <a-empty description="暂无阶段数据" />
      </div>
    </div>
  </H5Layout>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { FileTextOutlined } from '@ant-design/icons-vue'
import H5Layout from '@/layouts/H5Layout.vue'
import { getProjectProgress } from '@/api/customerPortal'
import type { ProjectProgress } from '@/types/customerPortal'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const progress = ref<ProjectProgress | null>(null)

onMounted(async () => {
  // 直接透传字符串 id，避免雪花 Long 经 Number() 转换丢精度
  const projectId = route.params.projectId as string
  if (!projectId) {
    message.error('项目ID缺失')
    router.back()
    return
  }
  loading.value = true
  try {
    progress.value = await getProjectProgress(projectId)
  } catch (e: any) {
    console.error('[H5 customer progress]', e)
    if (e?.code === 401 || e?.code === 40301) {
      router.push({
        path: '/h5/customer/login',
        query: { redirect: route.fullPath }
      })
    } else {
      message.error(e?.message || '加载失败')
    }
  } finally {
    loading.value = false
  }
})

function goDocuments() {
  const projectId = route.params.projectId
  router.push(`/h5/customer/projects/${projectId}/documents`)
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

function formatPhaseStatus(status: string): string {
  const map: Record<string, string> = {
    NOT_STARTED: '待开始',
    IN_PROGRESS: '进行中',
    COMPLETED: '已完成'
  }
  return map[status] || status
}

function getPhaseIcon(status: string): string {
  if (status === 'COMPLETED') return '✅'
  if (status === 'IN_PROGRESS') return '🔄'
  return '⚪'
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

.overview-card {
  background: linear-gradient(135deg, #ffffff 0%, #f0f5ff 100%);
  border-radius: 14px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(22, 119, 255, 0.08);
}

.project-name {
  font-size: 17px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 16px;
  line-height: 1.4;
}

.overall-progress {
  display: flex;
  align-items: center;
  gap: 20px;
}

.progress-info {
  flex: 1;
}

.progress-num {
  font-size: 24px;
  font-weight: 700;
  color: #1677ff;
  line-height: 1;
}

.progress-label {
  font-size: 12px;
  color: #888;
  margin-top: 4px;
}

.phase-name {
  font-size: 13px;
  color: #333;
  margin: 8px 0;
}

.status-tag {
  display: inline-block;
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

.section-title {
  text-align: center;
  font-size: 13px;
  color: #999;
  margin: 20px 0 12px;
}

.phase-timeline {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}

.phase-item {
  display: flex;
  gap: 12px;
  padding-bottom: 16px;
}

.phase-item:last-child {
  padding-bottom: 0;
}

.phase-marker {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 28px;
}

.marker-icon {
  font-size: 18px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
  background: #fff;
}

.marker-line {
  flex: 1;
  width: 2px;
  background: #e8e8e8;
  margin-top: 4px;
  min-height: 30px;
}

.phase-content {
  flex: 1;
  padding-top: 4px;
}

.phase-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.phase-name {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
}

.phase-status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #f0f0f0;
  color: #666;
}

.phase-status.status-in_progress {
  background: #e6f4ff;
  color: #1677ff;
}

.phase-status.status-completed {
  background: #f6ffed;
  color: #52c41a;
}

.phase-meta {
  font-size: 12px;
  color: #888;
  margin-top: 4px;
}
</style>
