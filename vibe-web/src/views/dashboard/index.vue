<script setup lang="ts">
/**
 * 工作台首页（占位）
 * 后续按角色差异化展示：待办事项、核心指标卡片、我负责的项目、近期任务、最近动态
 */
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import PageContainer from '@/components/PageContainer.vue'
import StatisticCard from '@/components/StatisticCard.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const userStore = useUserStore()

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const today = computed(() => {
  const d = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${week}`
})

// 占位统计卡片
const stats = [
  { title: '在建项目', value: 0, unit: '个', trend: 'flat' as const, icon: 'ProjectOutlined' },
  { title: '风险项目', value: 0, unit: '个', trend: 'flat' as const, icon: 'WarningOutlined' },
  { title: '本月到期', value: 0, unit: '个', trend: 'flat' as const, icon: 'ClockCircleOutlined' },
  { title: '待派任务', value: 0, unit: '个', trend: 'flat' as const, icon: 'ScheduleOutlined' }
]
</script>

<template>
  <PageContainer>
    <template #header>
      <div class="dashboard-header">
        <div>
          <h2 class="vibe-page-title">{{ greeting }}，{{ userStore.username || '管理员' }}</h2>
          <p class="dashboard-date">{{ today }}，祝你工作顺利！</p>
        </div>
      </div>
    </template>

    <div class="dashboard-body">
      <!-- 核心指标卡片 -->
      <a-row :gutter="16" class="stat-row">
        <a-col v-for="s in stats" :key="s.title" :xs="12" :sm="12" :md="6">
          <StatisticCard
            :title="s.title"
            :value="s.value"
            :unit="s.unit"
            :trend="s.trend"
            :icon="s.icon"
          />
        </a-col>
      </a-row>

      <!-- 待办事项 -->
      <div class="vibe-card todo-card">
        <div class="card-head">
          <h3 class="card-title">待办事项</h3>
          <a class="card-link">查看全部待办 →</a>
        </div>
        <EmptyState description="暂无待办事项" size="compact" />
      </div>

      <!-- 我负责的项目 + 近期任务 -->
      <a-row :gutter="16">
        <a-col :xs="24" :md="14">
          <div class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">我负责的项目</h3>
              <a class="card-link">查看全部 →</a>
            </div>
            <EmptyState description="暂无负责的项目" size="compact" action-text="创建项目" />
          </div>
        </a-col>
        <a-col :xs="24" :md="10">
          <div class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">我的近期任务</h3>
              <a class="card-link">更多 →</a>
            </div>
            <EmptyState description="暂无近期任务" size="compact" />
          </div>
        </a-col>
      </a-row>

      <!-- 最近动态 + 本月项目进度 -->
      <a-row :gutter="16">
        <a-col :xs="24" :md="14">
          <div class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">最近动态</h3>
            </div>
            <EmptyState description="暂无动态" size="compact" />
          </div>
        </a-col>
        <a-col :xs="24" :md="10">
          <div class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">本月项目进度</h3>
            </div>
            <div class="month-progress">
              <div class="progress-item">
                <span class="progress-label">本月新增</span>
                <ProgressBar :percent="0" :show-label="false" size="normal" />
                <span class="progress-num tnum">0</span>
              </div>
              <div class="progress-item">
                <span class="progress-label">本月结项</span>
                <ProgressBar :percent="0" :show-label="false" size="normal" />
                <span class="progress-num tnum">0</span>
              </div>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>
  </PageContainer>
</template>

<style lang="less" scoped>
.dashboard-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.dashboard-date {
  margin: 0;
  font-size: 13px;
  color: @text-tertiary;
}
.dashboard-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.stat-row {
  margin-bottom: 0;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}
.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: @text-primary;
}
.card-link {
  font-size: 13px;
}
.todo-card,
.block-card {
  height: 100%;
}
.month-progress {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.progress-item {
  display: flex;
  align-items: center;
  gap: 12px;
  .progress-label {
    width: 80px;
    font-size: 13px;
    color: @text-secondary;
  }
  .progress-num {
    width: 40px;
    text-align: right;
    font-weight: 600;
    color: @text-primary;
  }
}
</style>
