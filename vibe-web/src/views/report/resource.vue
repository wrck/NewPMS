<script setup lang="ts">
/**
 * 资源报表
 * 资源维度统计：工程师负荷、工时统计、项目维度投入
 */
import { ref, reactive, onMounted } from 'vue'
import { ReloadOutlined, ExportOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatisticCard from '@/components/StatisticCard.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getResourceReport } from '@/api/report'

interface ResourceReportData {
  summary: { totalEngineers: number; avgUtilization: number; totalHours: number; overtimeHours: number }
  byEngineer: Array<{
    engineerId: number
    engineerName: string
    taskCount: number
    hours: number
    overtimeHours: number
    utilization: number
    onTimeRate: number
  }>
  byProject: Array<{ projectId: number; projectName: string; hours: number; engineerCount: number }>
}

const loading = ref(false)
const data = ref<ResourceReportData | null>(null)
const query = reactive({
  startDate: '',
  endDate: '',
  engineerId: undefined as number | undefined,
  orgId: undefined as number | undefined
})

async function loadData() {
  loading.value = true
  try {
    data.value = (await getResourceReport(query as Record<string, unknown>)) as unknown as ResourceReportData
  } catch (e) {
    console.error('[report.resource] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

function handleExport() {
  message.info('导出功能开发中')
}

const engineerColumns = [
  { title: '工程师', dataIndex: 'engineerName', key: 'engineerName', width: 120 },
  { title: '任务数', dataIndex: 'taskCount', key: 'taskCount', width: 90 },
  { title: '工时(h)', dataIndex: 'hours', key: 'hours', width: 100 },
  { title: '加班(h)', dataIndex: 'overtimeHours', key: 'overtimeHours', width: 100 },
  { title: '利用率', key: 'utilization', width: 200 },
  { title: '按时率', key: 'onTimeRate', width: 100 }
]

const projectColumns = [
  { title: '项目名称', dataIndex: 'projectName', key: 'projectName', ellipsis: true },
  { title: '投入工时(h)', dataIndex: 'hours', key: 'hours', width: 140 },
  { title: '工程师数', dataIndex: 'engineerCount', key: 'engineerCount', width: 120 }
]

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="资源报表" description="工程师负荷、工时统计、项目维度投入分析">
    <template #extra>
      <a-button @click="loadData" :loading="loading"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button @click="handleExport"><template #icon><ExportOutlined /></template>导出</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="工程师ID">
          <a-input-number v-model:value="query.engineerId" placeholder="工程师ID" style="width: 140px" />
        </a-form-item>
        <a-form-item label="组织ID">
          <a-input-number v-model:value="query.orgId" placeholder="组织ID" style="width: 140px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <a-spin :spinning="loading">
      <a-row :gutter="16" class="stat-row">
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="工程师总数" :value="data?.summary.totalEngineers ?? 0" unit="人" icon="TeamOutlined" />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="平均利用率" :value="data?.summary.avgUtilization ?? 0" unit="%" icon="RiseOutlined" />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="总工时" :value="data?.summary.totalHours ?? 0" unit="h" icon="ClockCircleOutlined" />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="加班工时" :value="data?.summary.overtimeHours ?? 0" unit="h" icon="WarningOutlined" accent="#faad14" />
        </a-col>
      </a-row>

      <div class="vibe-card block-card">
        <div class="card-head"><h3 class="card-title">工程师负荷</h3></div>
        <a-table :columns="engineerColumns" :data-source="data?.byEngineer || []" row-key="engineerId" size="small" :scroll="{ x: 800 }" :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'utilization'">
              <ProgressBar :percent="record.utilization" />
              <span style="margin-left: 8px; color: #595959">{{ record.utilization }}%</span>
            </template>
            <template v-else-if="column.key === 'onTimeRate'">
              <span :style="{ color: record.onTimeRate >= 80 ? '#52c41a' : record.onTimeRate >= 60 ? '#faad14' : '#ff4d4f' }">
                {{ record.onTimeRate }}%
              </span>
            </template>
          </template>
          <template #emptyText><EmptyState description="暂无工程师数据" /></template>
        </a-table>
      </div>

      <div class="vibe-card table-card">
        <div class="card-head"><h3 class="card-title">项目维度投入</h3></div>
        <a-table :columns="projectColumns" :data-source="data?.byProject || []" row-key="projectId" size="small" :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }">
          <template #emptyText><EmptyState description="暂无项目投入数据" /></template>
        </a-table>
      </div>
    </a-spin>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.stat-row { margin-bottom: 16px; .ant-col { margin-bottom: 12px; } }
.block-card { margin-bottom: 16px; }
.table-card { padding-bottom: 0; }
.card-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 20px; border-bottom: 1px solid @border-color-split;
}
.card-title { margin: 0; font-size: 15px; font-weight: 600; color: @text-primary; }
</style>
