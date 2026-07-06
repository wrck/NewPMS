<script setup lang="ts">
/**
 * 财务报表（spec 阶段三 Task 24 - SubTask 24.5）
 *
 * 全面 ECharts 化重写：
 *   - 利润趋势 LineChart
 *   - 收入成本对比 BarChart
 *   - 客户占比 FunnelChart
 *   - 明细表格（客户利润 + 区域利润 + 产品线利润 + 代理商结算）
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import { ImportExport } from '@/components/ImportExport'
import { LineChart, BarChart, FunnelChart } from '@/components/charts'
import { getFinanceReport } from '@/api/report'

/* ============ 类型 ============ */
interface FinanceReportData {
  summary: { totalRevenue: number; totalCost: number; totalProfit: number; profitMargin: number }
  byCustomer: Array<{ customerId: number; customerName: string; revenue: number; cost: number; profit: number }>
  byRegion: Array<{ region: string; revenue: number; cost: number; profit: number }>
  byProductLine: Array<{ productLine: string; revenue: number; cost: number; profit: number }>
  agentSettlement: Array<{ agentCompanyId: number; agentCompanyName: string; totalAmount: number; paidAmount: number; pendingAmount: number }>
  /** 利润趋势（可选，后端未实现时为 undefined） */
  profitTrend?: Array<{ month: string; profit: number }>
}

/* ============ 状态 ============ */
const loading = ref(false)
const data = ref<FinanceReportData | null>(null)
const query = reactive({
  startDate: '',
  endDate: '',
  customerId: undefined as number | undefined
})

/* ============ 数据加载 ============ */
async function loadData() {
  loading.value = true
  try {
    data.value = (await getFinanceReport(
      query as Record<string, unknown>
    )) as unknown as FinanceReportData
  } catch (e) {
    console.error('[report.finance] load failed:', e)
    message.error('加载财务报表数据失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

function handleReset() {
  Object.assign(query, { startDate: '', endDate: '', customerId: undefined })
  loadData()
}

/* ============ 工具函数 ============ */
function fmtMoney(v: number | undefined): string {
  if (v == null) return '0.00'
  return v.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/* ============ 利润趋势 LineChart ============ */
const profitTrendData = computed(() => {
  const trend = data.value?.profitTrend || []
  if (trend.length === 0) return { xAxis: [] as string[], series: [] as Array<{ name: string; data: number[] }> }
  return {
    xAxis: trend.map((t) => t.month),
    series: [{ name: '利润', data: trend.map((t) => t.profit) }]
  }
})

/* ============ 收入成本对比 BarChart（按区域） ============ */
const regionBarData = computed(() => {
  const list = data.value?.byRegion || []
  if (list.length === 0) return { xAxis: [] as string[], series: [] as Array<{ name: string; data: number[] }> }
  return {
    xAxis: list.map((r) => r.region || '未分类'),
    series: [
      { name: '收入', data: list.map((r) => r.revenue) },
      { name: '成本', data: list.map((r) => r.cost) }
    ]
  }
})

/* ============ 客户占比 FunnelChart ============ */
const customerFunnelData = computed(() => {
  const list = data.value?.byCustomer || []
  // 按收入降序，取前 10 名
  return list
    .slice()
    .sort((a, b) => b.revenue - a.revenue)
    .slice(0, 10)
    .map((c) => ({ name: c.customerName || `客户${c.customerId}`, value: c.revenue }))
})

/* ============ 明细表格 ============ */
const customerColumns = [
  { title: '客户名称', dataIndex: 'customerName', key: 'customerName', ellipsis: true },
  { title: '收入', key: 'revenue', width: 140 },
  { title: '成本', key: 'cost', width: 140 },
  { title: '利润', key: 'profit', width: 140 }
]

const regionColumns = [
  { title: '区域', dataIndex: 'region', key: 'region' },
  { title: '收入', key: 'revenue', width: 140 },
  { title: '成本', key: 'cost', width: 140 },
  { title: '利润', key: 'profit', width: 140 }
]

const productLineColumns = [
  { title: '产品线', dataIndex: 'productLine', key: 'productLine' },
  { title: '收入', key: 'revenue', width: 140 },
  { title: '成本', key: 'cost', width: 140 },
  { title: '利润', key: 'profit', width: 140 }
]

const agentColumns = [
  { title: '代理商', dataIndex: 'agentCompanyName', key: 'agentCompanyName', ellipsis: true },
  { title: '结算总额', key: 'totalAmount', width: 140 },
  { title: '已付', key: 'paidAmount', width: 140 },
  { title: '待付', key: 'pendingAmount', width: 140 }
]

const customerData = computed(() => data.value?.byCustomer || [])
const regionData = computed(() => data.value?.byRegion || [])
const productLineData = computed(() => data.value?.byProductLine || [])
const agentData = computed(() => data.value?.agentSettlement || [])

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="财务报表" description="利润趋势、收入成本对比、客户占比与代理商结算">
    <template #extra>
      <a-button @click="loadData" :loading="loading">
        <template #icon><ReloadOutlined /></template>刷新
      </a-button>
    </template>

    <!-- 筛选 + ImportExport -->
    <a-card class="filter-card" :bordered="true">
      <a-form layout="inline" :model="query">
        <a-form-item label="客户ID">
          <a-input-number v-model:value="query.customerId" placeholder="客户ID" style="width: 140px" />
        </a-form-item>
        <a-form-item label="开始起">
          <a-date-picker v-model:value="query.startDate" value-format="YYYY-MM-DD" style="width: 150px" />
        </a-form-item>
        <a-form-item label="结束止">
          <a-date-picker v-model:value="query.endDate" value-format="YYYY-MM-DD" style="width: 150px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleSearch">查询</a-button>
          <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
        </a-form-item>
      </a-form>
      <div class="filter-actions">
        <ImportExport
          export-api="/api/v1/report/finance/export"
          :export-params="query as Record<string, unknown>"
          :show-import="false"
          :show-template="false"
        />
      </div>
    </a-card>

    <a-spin :spinning="loading">
      <!-- 利润趋势 + 收入成本对比 -->
      <a-row :gutter="16" class="chart-row">
        <a-col :xs="24" :md="12">
          <a-card title="利润趋势" :bordered="true">
            <LineChart
              :data="profitTrendData.series"
              :x-axis="profitTrendData.xAxis"
              smooth
              area
              :height="320"
            />
          </a-card>
        </a-col>
        <a-col :xs="24" :md="12">
          <a-card title="收入成本对比（按区域）" :bordered="true">
            <BarChart :data="regionBarData.series" :x-axis="regionBarData.xAxis" :height="320" />
          </a-card>
        </a-col>
      </a-row>

      <!-- 客户占比 FunnelChart -->
      <a-card title="客户收入占比（Top 10）" :bordered="true" class="chart-row">
        <FunnelChart :data="customerFunnelData" :height="320" />
      </a-card>

      <!-- 客户利润明细 -->
      <a-card title="客户利润分析" :bordered="true" class="table-card">
        <a-table
          :columns="customerColumns"
          :data-source="customerData"
          row-key="customerId"
          size="small"
          :scroll="{ x: 600 }"
          :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'revenue'"><span style="color: #1677ff">¥{{ fmtMoney(record.revenue) }}</span></template>
            <template v-else-if="column.key === 'cost'"><span style="color: #faad14">¥{{ fmtMoney(record.cost) }}</span></template>
            <template v-else-if="column.key === 'profit'">
              <span :style="{ color: record.profit >= 0 ? '#52c41a' : '#ff4d4f' }">¥{{ fmtMoney(record.profit) }}</span>
            </template>
          </template>
          <template #emptyText><EmptyState description="暂无客户利润数据" /></template>
        </a-table>
      </a-card>

      <!-- 区域利润 + 产品线利润 -->
      <a-row :gutter="16" class="chart-row">
        <a-col :xs="24" :md="12">
          <a-card title="区域利润" :bordered="true">
            <a-table
              :columns="regionColumns"
              :data-source="regionData"
              row-key="region"
              size="small"
              :scroll="{ x: 600 }"
              :pagination="{ pageSize: 8 }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'revenue'"><span style="color: #1677ff">¥{{ fmtMoney(record.revenue) }}</span></template>
                <template v-else-if="column.key === 'cost'"><span style="color: #faad14">¥{{ fmtMoney(record.cost) }}</span></template>
                <template v-else-if="column.key === 'profit'">
                  <span :style="{ color: record.profit >= 0 ? '#52c41a' : '#ff4d4f' }">¥{{ fmtMoney(record.profit) }}</span>
                </template>
              </template>
              <template #emptyText><EmptyState description="暂无数据" /></template>
            </a-table>
          </a-card>
        </a-col>
        <a-col :xs="24" :md="12">
          <a-card title="产品线利润" :bordered="true">
            <a-table
              :columns="productLineColumns"
              :data-source="productLineData"
              row-key="productLine"
              size="small"
              :scroll="{ x: 600 }"
              :pagination="{ pageSize: 8 }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'revenue'"><span style="color: #1677ff">¥{{ fmtMoney(record.revenue) }}</span></template>
                <template v-else-if="column.key === 'cost'"><span style="color: #faad14">¥{{ fmtMoney(record.cost) }}</span></template>
                <template v-else-if="column.key === 'profit'">
                  <span :style="{ color: record.profit >= 0 ? '#52c41a' : '#ff4d4f' }">¥{{ fmtMoney(record.profit) }}</span>
                </template>
              </template>
              <template #emptyText><EmptyState description="暂无数据" /></template>
            </a-table>
          </a-card>
        </a-col>
      </a-row>

      <!-- 代理商结算汇总 -->
      <a-card title="代理商结算汇总" :bordered="true">
        <a-table
          :columns="agentColumns"
          :data-source="agentData"
          row-key="agentCompanyId"
          size="small"
          :scroll="{ x: 600 }"
          :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'totalAmount'">¥{{ fmtMoney(record.totalAmount) }}</template>
            <template v-else-if="column.key === 'paidAmount'"><span style="color: #52c41a">¥{{ fmtMoney(record.paidAmount) }}</span></template>
            <template v-else-if="column.key === 'pendingAmount'"><span style="color: #ff4d4f">¥{{ fmtMoney(record.pendingAmount) }}</span></template>
          </template>
          <template #emptyText><EmptyState description="暂无代理商结算数据" /></template>
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
.table-card {
  margin-bottom: 16px;
}
</style>
