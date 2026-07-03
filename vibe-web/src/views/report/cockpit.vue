<script setup lang="ts">
/**
 * 管理驾驶舱
 * 全局核心指标、项目阶段分布、月度趋势、待办、风险预警、动态
 */
import { ref, onMounted } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatisticCard from '@/components/StatisticCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getCockpit } from '@/api/report'
import type { CockpitData, TodoItem, RiskWarning, Activity } from '@/api/report'
import { Priority, PriorityLabel } from '@/types/enum'

const loading = ref(false)
const data = ref<CockpitData | null>(null)

async function loadData() {
  loading.value = true
  try {
    data.value = (await getCockpit()) as unknown as CockpitData
  } catch (e) {
    console.error('[report.cockpit] load failed:', e)
  } finally {
    loading.value = false
  }
}

const riskLevelTone: Record<string, any> = {
  LOW: 'default',
  MEDIUM: 'warning',
  HIGH: 'error'
}
const riskLevelLabel: Record<string, string> = { LOW: '低', MEDIUM: '中', HIGH: '高' }
const riskTypeLabel: Record<string, string> = {
  PROGRESS: '进度',
  DEVICE: '设备',
  RESOURCE: '资源',
  AGENT: '代理商',
  OTHER: '其他'
}

const todoTypeLabel: Record<string, string> = {
  TASK: '任务',
  APPROVAL: '审批',
  ISSUE: '问题',
  WORKLOAD: '工作量'
}
const priorityTone: Record<Priority, any> = {
  LOW: 'default', MEDIUM: 'processing', HIGH: 'warning', URGENT: 'error'
}

const activityTypeColor: Record<string, string> = {
  CREATE: 'blue',
  UPDATE: 'gold',
  DELETE: 'red',
  APPROVE: 'green',
  COMPLETE: 'green',
  OTHER: 'gray'
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="管理驾驶舱" description="全局运营指标、风险预警与动态总览">
    <template #extra>
      <a-button @click="loadData" :loading="loading"><template #icon><ReloadOutlined /></template>刷新</a-button>
    </template>

    <a-spin :spinning="loading">
      <!-- 核心指标 -->
      <a-row :gutter="16" class="stat-row">
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="在建项目" :value="data?.kpi.ongoingProjects ?? 0" unit="个" icon="ProjectOutlined" />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="风险项目" :value="data?.kpi.riskProjects ?? 0" unit="个" icon="WarningOutlined" accent="#ff4d4f" />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="超期项目" :value="data?.kpi.overdueProjects ?? 0" unit="个" icon="ClockCircleOutlined" accent="#ff7a45" />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="本月新增" :value="data?.kpi.monthNewProjects ?? 0" unit="个" icon="RiseOutlined" />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="本月结项" :value="data?.kpi.monthClosedProjects ?? 0" unit="个" icon="CheckCircleOutlined" />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="工程师利用率" :value="data?.kpi.engineerUtilization ?? 0" unit="%" icon="TeamOutlined" />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="设备到货率" :value="data?.kpi.deviceArrivalRate ?? 0" unit="%" icon="HddOutlined" />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="验收完成率" :value="data?.kpi.acceptanceCompletionRate ?? 0" unit="%" icon="CheckSquareOutlined" />
        </a-col>
      </a-row>

      <!-- 项目阶段分布 + 月度趋势 -->
      <a-row :gutter="16">
        <a-col :xs="24" :md="10">
          <div class="vibe-card block-card">
            <div class="card-head"><h3 class="card-title">项目阶段分布</h3></div>
            <div class="card-body">
              <EmptyState v-if="!data?.phaseDistribution?.length" description="暂无数据" size="compact" />
              <div v-else class="phase-list">
                <div v-for="p in data.phaseDistribution" :key="p.phase" class="phase-item">
                  <span class="phase-name">{{ p.phaseName }}</span>
                  <span class="phase-count tnum">{{ p.count }}</span>
                </div>
              </div>
            </div>
          </div>
        </a-col>
        <a-col :xs="24" :md="14">
          <div class="vibe-card block-card">
            <div class="card-head"><h3 class="card-title">月度项目趋势</h3></div>
            <div class="card-body">
              <EmptyState v-if="!data?.projectTrend?.length" description="暂无数据" size="compact" />
              <a-table v-else :data-source="data.projectTrend" row-key="month" size="small" :pagination="false">
                <a-table-column title="月份" data-index="month" :width="100" />
                <a-table-column title="新增" data-index="newCount" :width="80" />
                <a-table-column title="结项" data-index="closedCount" :width="80" />
                <a-table-column title="在建" data-index="ongoingCount" :width="80" />
                <a-table-column title="趋势" key="trend">
                  <template #default="{ record }">
                    <ProgressBar :percent="record.newCount ? Math.round((record.closedCount / record.newCount) * 100) : 0" :show-label="false" />
                  </template>
                </a-table-column>
              </a-table>
            </div>
          </div>
        </a-col>
      </a-row>

      <!-- 待办 + 风险预警 -->
      <a-row :gutter="16">
        <a-col :xs="24" :md="12">
          <div class="vibe-card block-card">
            <div class="card-head"><h3 class="card-title">待办事项</h3></div>
            <div class="card-body">
              <EmptyState v-if="!data?.todoList?.length" description="暂无待办" size="compact" />
              <a-list v-else :data-source="data.todoList" item-layout="horizontal" size="small">
                <template #renderItem="{ item }">
                  <a-list-item>
                    <a-list-item-meta>
                      <template #title>
                        <a-tag :color="item.priority ? priorityTone[item.priority as Priority] : 'default'">{{ todoTypeLabel[item.type] || item.type }}</a-tag>
                        <span>{{ item.title }}</span>
                      </template>
                      <template #description>{{ item.description }}<span v-if="item.dueDate" class="text-auxiliary"> · 截止 {{ item.dueDate }}</span></template>
                    </a-list-item-meta>
                  </a-list-item>
                </template>
              </a-list>
            </div>
          </div>
        </a-col>
        <a-col :xs="24" :md="12">
          <div class="vibe-card block-card">
            <div class="card-head"><h3 class="card-title">风险预警</h3></div>
            <div class="card-body">
              <EmptyState v-if="!data?.riskWarnings?.length" description="暂无风险" size="compact" />
              <a-list v-else :data-source="data.riskWarnings" item-layout="horizontal" size="small">
                <template #renderItem="{ item }">
                  <a-list-item>
                    <a-list-item-meta>
                      <template #title>
                        <StatusTag :tone="riskLevelTone[item.level]">{{ riskTypeLabel[item.riskType] || item.riskType }} · {{ riskLevelLabel[item.level] }}</StatusTag>
                        <span>{{ item.projectName }}</span>
                      </template>
                      <template #description>{{ item.description }}<span class="text-auxiliary"> · {{ item.detectedAt }}</span></template>
                    </a-list-item-meta>
                  </a-list-item>
                </template>
              </a-list>
            </div>
          </div>
        </a-col>
      </a-row>

      <!-- 最近动态 -->
      <div class="vibe-card block-card">
        <div class="card-head"><h3 class="card-title">最近动态</h3></div>
        <div class="card-body">
          <EmptyState v-if="!data?.recentActivities?.length" description="暂无动态" size="compact" />
          <a-timeline v-else>
            <a-timeline-item v-for="act in data.recentActivities" :key="act.id" :color="activityTypeColor[act.type] || 'gray'">
              <p class="timeline-title">{{ act.title }} <span class="text-auxiliary">{{ act.operatedAt }}</span></p>
              <p v-if="act.description" class="timeline-desc text-auxiliary">{{ act.description }}</p>
              <p v-if="act.operatorName" class="timeline-desc text-auxiliary">操作人：{{ act.operatorName }}</p>
            </a-timeline-item>
          </a-timeline>
        </div>
      </div>
    </a-spin>
  </PageContainer>
</template>

<style lang="less" scoped>
.stat-row { margin-bottom: 16px; .ant-col { margin-bottom: 12px; } }
.block-card { height: 100%; }
.card-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 20px; border-bottom: 1px solid @border-color-split;
}
.card-title { margin: 0; font-size: 15px; font-weight: 600; color: @text-primary; }
.card-body { padding: 16px 20px; }
.text-auxiliary { color: @text-auxiliary; font-size: 12px; }
.phase-list { display: flex; flex-direction: column; gap: 12px; }
.phase-item {
  display: flex; justify-content: space-between; align-items: center;
  .phase-name { color: @text-secondary; }
  .phase-count { font-size: 18px; font-weight: 600; }
}
.timeline-title { margin: 0; font-weight: 500; }
.timeline-desc { margin: 4px 0 0; font-size: 12px; }
</style>
