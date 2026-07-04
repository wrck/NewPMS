<script setup lang="ts">
/**
 * 财务报表
 * 收入/成本/利润汇总、按客户/区域/产品线分布、代理商结算汇总
 */
import { ref, reactive, onMounted } from 'vue'
import { ReloadOutlined, ExportOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatisticCard from '@/components/StatisticCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getFinanceReport } from '@/api/report'

interface FinanceReportData {
  summary: { totalRevenue: number; totalCost: number; totalProfit: number; profitMargin: number }
  byCustomer: Array<{ customerId: number; customerName: string; revenue: number; cost: number; profit: number }>
  byRegion: Array<{ region: string; revenue: number; cost: number; profit: number }>
  byProductLine: Array<{ productLine: string; revenue: number; cost: number; profit: number }>
  agentSettlement: Array<{ agentCompanyId: number; agentCompanyName: string; totalAmount: number; paidAmount: number; pendingAmount: number }>
}

const loading = ref(false)
const data = ref<FinanceReportData | null>(null)
const query = reactive({
  startDate: '',
  endDate: '',
  customerId: undefined as number | undefined
})

async function loadData() {
  loading.value = true
  try {
    data.value = (await getFinanceReport(query as Record<string, unknown>)) as unknown as FinanceReportData
  } catch (e) {
    console.error('[report.finance] load failed:', e)
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

function fmtMoney(v: number | undefined): string {
  if (v == null) return '0.00'
  return v.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

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

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="财务报表" description="收入/成本/利润汇总、按客户/区域/产品线分布、代理商结算">
    <template #extra>
      <a-button @click="loadData" :loading="loading"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button @click="handleExport"><template #icon><ExportOutlined /></template>导出</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="客户ID">
          <a-input-number v-model:value="query.customerId" placeholder="客户ID" style="width: 140px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <a-spin :spinning="loading">
      <a-row :gutter="16" class="stat-row">
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="总收入" :value="fmtMoney(data?.summary.totalRevenue)" unit="元" icon="MoneyCollectOutlined" />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="总成本" :value="fmtMoney(data?.summary.totalCost)" unit="元" icon="MinusCircleOutlined" accent="#faad14" />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="总利润" :value="fmtMoney(data?.summary.totalProfit)" unit="元" icon="RiseOutlined" accent="#52c41a" />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="利润率" :value="data?.summary.profitMargin ?? 0" unit="%" icon="PercentageOutlined" />
        </a-col>
      </a-row>

      <div class="vibe-card block-card">
        <div class="card-head"><h3 class="card-title">客户利润分析</h3></div>
        <a-table :columns="customerColumns" :data-source="data?.byCustomer || []" row-key="customerId" size="small" :scroll="{ x: 600 }" :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'revenue'"><span style="color: #1677ff">¥{{ fmtMoney(record.revenue) }}</span></template>
            <template v-else-if="column.key === 'cost'"><span style="color: #faad14">¥{{ fmtMoney(record.cost) }}</span></template>
            <template v-else-if="column.key === 'profit'"><span :style="{ color: record.profit >= 0 ? '#52c41a' : '#ff4d4f' }">¥{{ fmtMoney(record.profit) }}</span></template>
          </template>
          <template #emptyText><EmptyState description="暂无客户利润数据" /></template>
        </a-table>
      </div>

      <a-row :gutter="16">
        <a-col :xs="24" :md="12">
          <div class="vibe-card block-card">
            <div class="card-head"><h3 class="card-title">区域利润</h3></div>
            <a-table :columns="regionColumns" :data-source="data?.byRegion || []" row-key="region" size="small" :scroll="{ x: 600 }" :pagination="{ pageSize: 8 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'revenue'"><span style="color: #1677ff">¥{{ fmtMoney(record.revenue) }}</span></template>
                <template v-else-if="column.key === 'cost'"><span style="color: #faad14">¥{{ fmtMoney(record.cost) }}</span></template>
                <template v-else-if="column.key === 'profit'"><span :style="{ color: record.profit >= 0 ? '#52c41a' : '#ff4d4f' }">¥{{ fmtMoney(record.profit) }}</span></template>
              </template>
              <template #emptyText><EmptyState description="暂无数据" /></template>
            </a-table>
          </div>
        </a-col>
        <a-col :xs="24" :md="12">
          <div class="vibe-card block-card">
            <div class="card-head"><h3 class="card-title">产品线利润</h3></div>
            <a-table :columns="productLineColumns" :data-source="data?.byProductLine || []" row-key="productLine" size="small" :scroll="{ x: 600 }" :pagination="{ pageSize: 8 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'revenue'"><span style="color: #1677ff">¥{{ fmtMoney(record.revenue) }}</span></template>
                <template v-else-if="column.key === 'cost'"><span style="color: #faad14">¥{{ fmtMoney(record.cost) }}</span></template>
                <template v-else-if="column.key === 'profit'"><span :style="{ color: record.profit >= 0 ? '#52c41a' : '#ff4d4f' }">¥{{ fmtMoney(record.profit) }}</span></template>
              </template>
              <template #emptyText><EmptyState description="暂无数据" /></template>
            </a-table>
          </div>
        </a-col>
      </a-row>

      <div class="vibe-card table-card">
        <div class="card-head"><h3 class="card-title">代理商结算汇总</h3></div>
        <a-table :columns="agentColumns" :data-source="data?.agentSettlement || []" row-key="agentCompanyId" size="small" :scroll="{ x: 600 }" :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'totalAmount'">¥{{ fmtMoney(record.totalAmount) }}</template>
            <template v-else-if="column.key === 'paidAmount'"><span style="color: #52c41a">¥{{ fmtMoney(record.paidAmount) }}</span></template>
            <template v-else-if="column.key === 'pendingAmount'"><span style="color: #ff4d4f">¥{{ fmtMoney(record.pendingAmount) }}</span></template>
          </template>
          <template #emptyText><EmptyState description="暂无代理商结算数据" /></template>
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
