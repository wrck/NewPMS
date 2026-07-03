<script setup lang="ts">
/**
 * 项目报表
 * 项目维度统计：汇总指标、状态/产品线/区域分布、PM 业绩、明细列表
 */
import { ref, reactive, onMounted } from 'vue'
import { ReloadOutlined, ExportOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatisticCard from '@/components/StatisticCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getProjectReport } from '@/api/report'
import { ProjectStatus, ProjectStatusTone, ProjectStatusLabel } from '@/types/enum'

interface ProjectReportData {
  summary: { total: number; completed: number; ongoing: number; overdue: number; avgProgress: number }
  byStatus: Array<{ status: string; statusName: string; count: number }>
  byProductLine: Array<{ productLine: string; count: number }>
  byRegion: Array<{ region: string; count: number }>
  byPm: Array<{ pmId: number; pmName: string; total: number; completed: number; overdue: number }>
  detail: Array<{
    id: number
    projectCode: string
    projectName: string
    status: string
    progressPct: number
    pmName: string
    plannedStart: string
    plannedEnd: string
    actualEnd?: string
    overdue: boolean
  }>
}

const loading = ref(false)
const data = ref<ProjectReportData | null>(null)
const query = reactive({
  startDate: '',
  endDate: '',
  status: undefined as string | undefined,
  pmId: undefined as number | undefined,
  productLine: '',
  region: ''
})

async function loadData() {
  loading.value = true
  try {
    data.value = (await getProjectReport(query as Record<string, unknown>)) as unknown as ProjectReportData
  } catch (e) {
    console.error('[report.project] load failed:', e)
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

const detailColumns = [
  { title: '项目编码', dataIndex: 'projectCode', key: 'projectCode', width: 140 },
  { title: '项目名称', dataIndex: 'projectName', key: 'projectName', ellipsis: true },
  { title: '状态', key: 'status', width: 100 },
  { title: '进度', key: 'progressPct', width: 160 },
  { title: '项目经理', dataIndex: 'pmName', key: 'pmName', width: 110 },
  { title: '计划开始', dataIndex: 'plannedStart', key: 'plannedStart', width: 120 },
  { title: '计划结束', dataIndex: 'plannedEnd', key: 'plannedEnd', width: 120 },
  { title: '实际结束', dataIndex: 'actualEnd', key: 'actualEnd', width: 120 },
  { title: '超期', key: 'overdue', width: 80, fixed: 'right' }
]

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="项目报表" description="项目维度统计分析：状态/产品线/区域分布与 PM 业绩">
    <template #extra>
      <a-button @click="loadData" :loading="loading"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button @click="handleExport"><template #icon><ExportOutlined /></template>导出</a-button>
    </template>

    <!-- 筛选 -->
    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="开始起">
          <a-date-picker v-model:value="query.startDate" value-format="YYYY-MM-DD" style="width: 150px" />
        </a-form-item>
        <a-form-item label="结束止">
          <a-date-picker v-model:value="query.endDate" value-format="YYYY-MM-DD" style="width: 150px" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 130px">
            <a-select-option v-for="s in Object.values(ProjectStatus)" :key="s" :value="s">{{ ProjectStatusLabel[s] }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="PM ID">
          <a-input-number v-model:value="query.pmId" placeholder="PM ID" style="width: 120px" />
        </a-form-item>
        <a-form-item label="产品线">
          <a-input v-model:value="query.productLine" placeholder="产品线" allow-clear style="width: 130px" />
        </a-form-item>
        <a-form-item label="区域">
          <a-input v-model:value="query.region" placeholder="区域" allow-clear style="width: 130px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <a-spin :spinning="loading">
      <!-- 汇总指标 -->
      <a-row :gutter="16" class="stat-row">
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="项目总数" :value="data?.summary.total ?? 0" unit="个" icon="ProjectOutlined" />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="已完成" :value="data?.summary.completed ?? 0" unit="个" icon="CheckCircleOutlined" />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="进行中" :value="data?.summary.ongoing ?? 0" unit="个" icon="SyncOutlined" />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="超期" :value="data?.summary.overdue ?? 0" unit="个" icon="ClockCircleOutlined" accent="#ff4d4f" />
        </a-col>
      </a-row>

      <!-- 分布 -->
      <a-row :gutter="16">
        <a-col :xs="24" :md="8">
          <div class="vibe-card block-card">
            <div class="card-head"><h3 class="card-title">状态分布</h3></div>
            <div class="card-body">
              <EmptyState v-if="!data?.byStatus?.length" description="暂无数据" size="compact" />
              <div v-else class="dist-list">
                <div v-for="d in data.byStatus" :key="d.status" class="dist-item">
                  <StatusTag :tone="ProjectStatusTone[d.status as ProjectStatus]">{{ d.statusName }}</StatusTag>
                  <span class="tnum">{{ d.count }}</span>
                </div>
              </div>
            </div>
          </div>
        </a-col>
        <a-col :xs="24" :md="8">
          <div class="vibe-card block-card">
            <div class="card-head"><h3 class="card-title">产品线分布</h3></div>
            <div class="card-body">
              <EmptyState v-if="!data?.byProductLine?.length" description="暂无数据" size="compact" />
              <div v-else class="dist-list">
                <div v-for="d in data.byProductLine" :key="d.productLine" class="dist-item">
                  <span>{{ d.productLine || '未分类' }}</span>
                  <span class="tnum">{{ d.count }}</span>
                </div>
              </div>
            </div>
          </div>
        </a-col>
        <a-col :xs="24" :md="8">
          <div class="vibe-card block-card">
            <div class="card-head"><h3 class="card-title">区域分布</h3></div>
            <div class="card-body">
              <EmptyState v-if="!data?.byRegion?.length" description="暂无数据" size="compact" />
              <div v-else class="dist-list">
                <div v-for="d in data.byRegion" :key="d.region" class="dist-item">
                  <span>{{ d.region || '未分类' }}</span>
                  <span class="tnum">{{ d.count }}</span>
                </div>
              </div>
            </div>
          </div>
        </a-col>
      </a-row>

      <!-- PM 业绩 -->
      <div class="vibe-card block-card">
        <div class="card-head"><h3 class="card-title">PM 业绩</h3></div>
        <a-table :data-source="data?.byPm || []" row-key="pmId" size="small" :pagination="false">
          <a-table-column title="项目经理" data-index="pmName" />
          <a-table-column title="负责项目" data-index="total" :width="100" />
          <a-table-column title="已完成" data-index="completed" :width="100" />
          <a-table-column title="超期" data-index="overdue" :width="100" />
          <a-table-column title="完成率" key="rate">
            <template #default="{ record }">
              <ProgressBar :percent="record.total ? Math.round((record.completed / record.total) * 100) : 0" />
            </template>
          </a-table-column>
        </a-table>
      </div>

      <!-- 明细 -->
      <div class="vibe-card table-card">
        <div class="card-head"><h3 class="card-title">项目明细</h3></div>
        <a-table :columns="detailColumns" :data-source="data?.detail || []" row-key="id" :scroll="{ x: 1200 }" :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <StatusTag :tone="ProjectStatusTone[record.status as ProjectStatus]">{{ ProjectStatusLabel[record.status as ProjectStatus] || record.status }}</StatusTag>
            </template>
            <template v-else-if="column.key === 'progressPct'">
              <ProgressBar :percent="record.progressPct" />
            </template>
            <template v-else-if="column.key === 'overdue'">
              <StatusTag :tone="record.overdue ? 'error' : 'success'">{{ record.overdue ? '超期' : '正常' }}</StatusTag>
            </template>
          </template>
          <template #emptyText><EmptyState description="暂无项目数据" /></template>
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
.card-body { padding: 16px 20px; }
.dist-list { display: flex; flex-direction: column; gap: 10px; }
.dist-item {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 14px;
  .tnum { font-weight: 600; }
}
</style>
