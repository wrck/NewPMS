<script setup lang="ts">
/**
 * 管理驾驶舱（spec 阶段三 Task 24 - SubTask 24.1）
 *
 * 全面 ECharts 化重写：
 *   - 4 张 KPI 卡片（含环比）
 *   - 项目阶段分布 PieChart
 *   - 近 12 月趋势 StackedChart
 *   - 风险项目列表
 *   - 设备状态 GaugeChart
 *   - 区域分布 MapChart
 */
import { ref, computed, onMounted } from 'vue'
import { ReloadOutlined, ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import { PieChart, StackedChart, GaugeChart, MapChart } from '@/components/charts'
import { getCockpit, getProjectReport } from '@/api/report'
import type { CockpitData, RiskWarning } from '@/api/report'

/* ============ 状态 ============ */
const loading = ref(false)
const data = ref<CockpitData | null>(null)
/** 区域分布数据（来自项目报表 byRegion） */
const regionData = ref<Array<{ name: string; value: number }>>([])

/* ============ KPI 卡片数据 ============ */
interface KpiCard {
  key: string
  title: string
  value: number
  unit: string
  /** 环比百分比，正数上升 / 负数下降 / 0 持平 */
  trend: number
}

const kpis = computed<KpiCard[]>(() => {
  const k = data.value?.kpi
  if (!k) return []
  // 在建项目环比：本月新增 - 本月结项 占在建基数比例
  const ongoingTrend =
    k.ongoingProjects > 0
      ? ((k.monthNewProjects - k.monthClosedProjects) / k.ongoingProjects) * 100
      : 0
  return [
    { key: 'ongoing', title: '在建项目', value: k.ongoingProjects, unit: '个', trend: ongoingTrend },
    { key: 'risk', title: '风险项目', value: k.riskProjects, unit: '个', trend: 0 },
    { key: 'device', title: '设备到货率', value: k.deviceArrivalRate ?? 0, unit: '%', trend: 0 },
    { key: 'acceptance', title: '验收完成率', value: k.acceptanceCompletionRate ?? 0, unit: '%', trend: 0 }
  ]
})

/* ============ 项目阶段分布 PieChart ============ */
const phaseChartData = computed(() =>
  (data.value?.phaseDistribution || []).map((p) => ({
    name: p.phaseName || p.phase,
    // Long 经 JacksonConfig 序列化为字符串，需转 number 供 ECharts 使用
    value: Number(p.count) || 0
  }))
)

/* ============ 近 12 月趋势 StackedChart ============ */
const trendXAxis = computed(() => (data.value?.projectTrend || []).map((t) => t.month))

const trendSeries = computed(() => {
  const trend = data.value?.projectTrend || []
  if (trend.length === 0) return []
  return [
    { name: '新增', data: trend.map((t) => Number(t.newCount) || 0) },
    { name: '结项', data: trend.map((t) => Number(t.closedCount) || 0) },
    { name: '在建', data: trend.map((t) => Number(t.ongoingCount) || 0) }
  ]
})

/* ============ 设备状态 GaugeChart ============ */
const deviceGaugeValue = computed(() => data.value?.kpi.deviceArrivalRate ?? 0)

/* ============ 风险项目表格 ============ */
const riskLevelTone: Record<string, 'default' | 'warning' | 'error'> = {
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

const riskColumns = [
  { title: '项目名称', dataIndex: 'projectName', key: 'projectName', ellipsis: true },
  { title: '风险类型', key: 'riskType', width: 110 },
  { title: '等级', key: 'level', width: 90 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '检测时间', dataIndex: 'detectedAt', key: 'detectedAt', width: 170 }
]

const riskList = computed<RiskWarning[]>(() => data.value?.riskWarnings || [])

/* ============ 数据加载 ============ */
async function loadData() {
  loading.value = true
  try {
    const [cockpitRes, projectRes] = await Promise.all([
      getCockpit(),
      getProjectReport({}).catch(() => null)
    ])
    data.value = cockpitRes as unknown as CockpitData
    // 项目报表 byRegion → MapChart 数据
    const byRegion = (projectRes as unknown as { byRegion?: Array<{ region: string; count: number }> } | null)?.byRegion
    regionData.value = (byRegion || []).map((r) => ({ name: r.region, value: Number(r.count) || 0 }))
  } catch (e) {
    console.error('[report.cockpit] load failed:', e)
    message.error('加载驾驶舱数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="管理驾驶舱" description="全局运营指标、风险预警与图表化总览">
    <template #extra>
      <a-button @click="loadData" :loading="loading">
        <template #icon><ReloadOutlined /></template>刷新
      </a-button>
    </template>

    <a-spin :spinning="loading">
      <!-- 4 张 KPI 卡片（含环比） -->
      <a-row :gutter="16" class="kpi-row">
        <a-col :xs="12" :sm="12" :md="6" v-for="kpi in kpis" :key="kpi.key">
          <a-card class="kpi-card" :bordered="true">
            <a-statistic :title="kpi.title" :value="kpi.value" :suffix="kpi.unit" />
            <div class="kpi-trend">
              <span :class="kpi.trend > 0 ? 'up' : kpi.trend < 0 ? 'down' : 'flat'">
                <ArrowUpOutlined v-if="kpi.trend > 0" />
                <ArrowDownOutlined v-else-if="kpi.trend < 0" />
                <span v-else>—</span>
                <template v-if="kpi.trend !== 0">{{ Math.abs(kpi.trend).toFixed(1) }}%</template>
              </span>
              <span class="trend-label">较上月</span>
            </div>
          </a-card>
        </a-col>
      </a-row>

      <!-- 项目阶段分布 + 近 12 月趋势 -->
      <a-row :gutter="16" class="chart-row">
        <a-col :xs="24" :md="10">
          <a-card title="项目阶段分布" :bordered="true">
            <PieChart :data="phaseChartData" :loading="loading" :height="320" />
          </a-card>
        </a-col>
        <a-col :xs="24" :md="14">
          <a-card title="近 12 月项目趋势" :bordered="true">
            <StackedChart
              :data="trendSeries"
              :x-axis="trendXAxis"
              :loading="loading"
              :height="320"
            />
          </a-card>
        </a-col>
      </a-row>

      <!-- 设备状态仪表盘 + 区域分布 MapChart -->
      <a-row :gutter="16" class="chart-row">
        <a-col :xs="24" :md="10">
          <a-card title="设备到货率" :bordered="true">
            <GaugeChart :value="deviceGaugeValue" :min="0" :max="100" :height="320" />
          </a-card>
        </a-col>
        <a-col :xs="24" :md="14">
          <a-card title="项目区域分布" :bordered="true">
            <MapChart :data="regionData" :loading="loading" :height="320" />
          </a-card>
        </a-col>
      </a-row>

      <!-- 风险项目列表 -->
      <a-card title="风险项目列表" :bordered="true" class="table-card">
        <a-table
          :columns="riskColumns"
          :data-source="riskList"
          row-key="id"
          size="small"
          :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'riskType'">
              <a-tag>{{ riskTypeLabel[record.riskType] || record.riskType }}</a-tag>
            </template>
            <template v-else-if="column.key === 'level'">
              <StatusTag :tone="riskLevelTone[record.level] || 'default'">
                {{ riskLevelLabel[record.level] || record.level }}
              </StatusTag>
            </template>
          </template>
          <template #emptyText><EmptyState description="暂无风险项目" /></template>
        </a-table>
      </a-card>
    </a-spin>
  </PageContainer>
</template>

<style lang="less" scoped>
.kpi-row {
  margin-bottom: 16px;
  .ant-col {
    margin-bottom: 12px;
  }
}
.chart-row {
  margin-bottom: 16px;
  .ant-col {
    margin-bottom: 12px;
  }
}
.table-card {
  margin-bottom: 16px;
}
.kpi-card {
  text-align: center;
  :deep(.ant-statistic) {
    text-align: center;
  }
  :deep(.ant-statistic-title) {
    font-size: 14px;
    color: @text-tertiary;
  }
  :deep(.ant-statistic-content-value) {
    font-size: 28px;
    font-weight: 600;
    color: @text-primary;
  }
}
.kpi-trend {
  margin-top: 8px;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  .up {
    color: #52c41a;
  }
  .down {
    color: #ff4d4f;
  }
  .flat {
    color: #8c8c8c;
  }
  .trend-label {
    color: @text-tertiary;
  }
}
</style>
