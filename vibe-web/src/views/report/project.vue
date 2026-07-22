<script setup lang="ts">
/**
 * 项目报表（spec 阶段三 Task 24 - SubTask 24.2）
 *
 * 全面 ECharts 化重写：
 *   - 项目状态分布 PieChart
 *   - 月度新增/完成 LineChart（从明细数据按月聚合）
 *   - PM 业绩 BarChart
 *   - 明细表格（分页 + 排序）
 *   - ImportExport 导出
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import { ImportExport } from '@/components/ImportExport'
import { PieChart, LineChart, BarChart } from '@/components/charts'
import { getProjectReport } from '@/api/report'
import { pageUsers } from '@/api/system'
import { ProjectStatus, ProjectStatusTone, ProjectStatusLabel } from '@/types/enum'

/* ============ 类型 ============ */
interface ProjectDetail {
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
}

interface ProjectReportData {
  summary: { total: number; completed: number; ongoing: number; overdue: number; avgProgress: number }
  byStatus: Array<{ status: string; statusName: string; count: number }>
  byProductLine: Array<{ productLine: string; count: number }>
  byRegion: Array<{ region: string; count: number }>
  byPm: Array<{ pmId: number; pmName: string; total: number; completed: number; overdue: number }>
  detail: ProjectDetail[]
}

/* ============ 状态 ============ */
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

/* ============ 数据加载 ============ */
async function loadData() {
  loading.value = true
  try {
    data.value = (await getProjectReport(
      query as Record<string, unknown>
    )) as unknown as ProjectReportData
  } catch (e) {
    console.error('[report.project] load failed:', e)
    message.error('加载项目报表数据失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

function handleReset() {
  Object.assign(query, {
    startDate: '',
    endDate: '',
    status: undefined,
    pmId: undefined,
    productLine: '',
    region: ''
  })
  loadData()
}

/* ============ 项目状态分布 PieChart ============ */
const statusPieData = computed(() =>
  (data.value?.byStatus || []).map((s) => ({
    name: s.statusName || ProjectStatusLabel[s.status as ProjectStatus] || s.status,
    value: s.count
  }))
)

/* ============ 月度新增/完成 LineChart（从明细按月聚合） ============ */
const monthlyTrend = computed(() => {
  const details = data.value?.detail || []
  if (details.length === 0) return { xAxis: [] as string[], series: [] as Array<{ name: string; data: number[] }> }

  // 收集所有月份
  const monthSet = new Set<string>()
  const newByMonth = new Map<string, number>()
  const completedByMonth = new Map<string, number>()

  details.forEach((d) => {
    // 新增按计划开始月份
    if (d.plannedStart) {
      const m = d.plannedStart.slice(0, 7) // YYYY-MM
      monthSet.add(m)
      newByMonth.set(m, (newByMonth.get(m) || 0) + 1)
    }
    // 完成按实际结束月份
    if (d.actualEnd) {
      const m = d.actualEnd.slice(0, 7)
      monthSet.add(m)
      completedByMonth.set(m, (completedByMonth.get(m) || 0) + 1)
    }
  })

  const xAxis = Array.from(monthSet).sort()
  return {
    xAxis,
    series: [
      { name: '新增', data: xAxis.map((m) => newByMonth.get(m) || 0) },
      { name: '完成', data: xAxis.map((m) => completedByMonth.get(m) || 0) }
    ]
  }
})

/* ============ PM 业绩 BarChart ============ */
const pmBarData = computed(() => {
  const byPm = data.value?.byPm || []
  if (byPm.length === 0) return { xAxis: [] as string[], series: [] as Array<{ name: string; data: number[] }> }
  const xAxis = byPm.map((p) => p.pmName)
  return {
    xAxis,
    series: [
      { name: '负责项目', data: byPm.map((p) => p.total) },
      { name: '已完成', data: byPm.map((p) => p.completed) },
      { name: '超期', data: byPm.map((p) => p.overdue) }
    ]
  }
})

/* ============ 明细表格 ============ */
const detailColumns = [
  { title: '项目编码', dataIndex: 'projectCode', key: 'projectCode', width: 140 },
  { title: '项目名称', dataIndex: 'projectName', key: 'projectName', ellipsis: true },
  { title: '状态', key: 'status', width: 100 },
  { title: '进度', key: 'progressPct', width: 160 },
  { title: '项目经理', dataIndex: 'pmName', key: 'pmName', width: 110 },
  { title: '计划开始', dataIndex: 'plannedStart', key: 'plannedStart', width: 120 },
  { title: '计划结束', dataIndex: 'plannedEnd', key: 'plannedEnd', width: 120 },
  { title: '实际结束', dataIndex: 'actualEnd', key: 'actualEnd', width: 120 },
  { title: '超期', key: 'overdue', width: 80, fixed: 'right' as const }
]

const detailData = computed(() => data.value?.detail || [])

// 项目经理下拉选项（实体引用字段：pmId）
const pmOptions = ref<Array<{ value: string | number; label: string }>>([])
async function loadPms() {
  try {
    const res = await pageUsers({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    pmOptions.value = list.map((u: any) => ({ value: u.id, label: u.realName || u.userName }))
  } catch (e) {
    console.warn('[pm] load failed:', e)
  }
}

onMounted(() => {
  loadData()
  loadPms()
})
</script>

<template>
  <PageContainer title="项目报表" description="项目维度统计分析：状态分布 / 月度趋势 / PM 业绩">
    <template #extra>
      <a-button @click="loadData" :loading="loading">
        <template #icon><ReloadOutlined /></template>刷新
      </a-button>
    </template>

    <!-- 筛选 + ImportExport -->
    <a-card class="filter-card" :bordered="true">
      <a-form layout="inline" :model="query">
        <a-form-item label="开始起">
          <a-date-picker v-model:value="query.startDate" value-format="YYYY-MM-DD" style="width: 150px" />
        </a-form-item>
        <a-form-item label="结束止">
          <a-date-picker v-model:value="query.endDate" value-format="YYYY-MM-DD" style="width: 150px" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 130px">
            <a-select-option v-for="s in Object.values(ProjectStatus)" :key="s" :value="s">
              {{ ProjectStatusLabel[s] }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="项目经理">
          <a-select
            v-model:value="query.pmId"
            show-search
            allow-clear
            :options="pmOptions"
            :filter-option="(input: string, option: any) => option.label.includes(input)"
            placeholder="全部 PM"
            style="width: 150px"
          />
        </a-form-item>
        <a-form-item label="产品线">
          <a-input v-model:value="query.productLine" placeholder="产品线" allow-clear style="width: 130px" />
        </a-form-item>
        <a-form-item label="区域">
          <a-input v-model:value="query.region" placeholder="区域" allow-clear style="width: 130px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleSearch">查询</a-button>
          <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
        </a-form-item>
      </a-form>
      <div class="filter-actions">
        <ImportExport
          export-api="/api/v1/report/project/export"
          :export-params="query as Record<string, unknown>"
          :show-import="false"
          :show-template="false"
        />
      </div>
    </a-card>

    <a-spin :spinning="loading">
      <!-- 状态分布 + 月度趋势 -->
      <a-row :gutter="16" class="chart-row">
        <a-col :xs="24" :md="12">
          <a-card title="项目状态分布" :bordered="true">
            <PieChart :data="statusPieData" :height="320" />
          </a-card>
        </a-col>
        <a-col :xs="24" :md="12">
          <a-card title="月度新增 / 完成趋势" :bordered="true">
            <LineChart
              :data="monthlyTrend.series"
              :x-axis="monthlyTrend.xAxis"
              smooth
              :height="320"
            />
          </a-card>
        </a-col>
      </a-row>

      <!-- PM 业绩排名 -->
      <a-card title="PM 业绩排名" :bordered="true" class="chart-row">
        <BarChart :data="pmBarData.series" :x-axis="pmBarData.xAxis" :height="320" />
      </a-card>

      <!-- 明细表格 -->
      <a-card title="项目明细" :bordered="true">
        <a-table
          :columns="detailColumns"
          :data-source="detailData"
          row-key="id"
          size="small"
          :scroll="{ x: 1200 }"
          :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <StatusTag :tone="ProjectStatusTone[record.status as ProjectStatus]">
                {{ ProjectStatusLabel[record.status as ProjectStatus] || record.status }}
              </StatusTag>
            </template>
            <template v-else-if="column.key === 'progressPct'">
              <ProgressBar :percent="record.progressPct" />
            </template>
            <template v-else-if="column.key === 'overdue'">
              <StatusTag :tone="record.overdue ? 'error' : 'success'">
                {{ record.overdue ? '超期' : '正常' }}
              </StatusTag>
            </template>
          </template>
          <template #emptyText><EmptyState description="暂无项目数据" /></template>
        </a-table>
      </a-card>
    </a-spin>
  </PageContainer>
</template>

<style lang="less" scoped>
.filter-card {
  margin-bottom: 16px;
  .filter-actions {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px dashed @border-color-split;
  }
}
.chart-row {
  margin-bottom: 16px;
  .ant-col {
    margin-bottom: 12px;
  }
}
</style>
