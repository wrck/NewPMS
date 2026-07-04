<script setup lang="ts">
/**
 * 利润分析
 * 设计文档 2.8.4：项目级利润 / 毛利率 / 自施 vs 代施对比 / 多维度分析
 * - 维度切换：项目 / 客户 / 区域 / 产品线
 * - 汇总卡片：总收入、总成本、总利润、平均毛利率
 * - 年度筛选
 * - CSV 导出
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { message as antdMessage } from 'ant-design-vue'
import { ReloadOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import PageContainer from '@/components/PageContainer.vue'
import StatisticCard from '@/components/StatisticCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  listProjectProfit,
  listProfitByCustomer,
  listProfitByRegion,
  listProfitByProductLine
} from '@/api/finance'
import type { FinanceProfit } from '@/types/finance'

type Dimension = 'project' | 'customer' | 'region' | 'productLine'

const loading = ref(false)
const dataSource = ref<FinanceProfit[]>([])
const dimension = ref<Dimension>('project')
const yearFilter = ref<number | undefined>(dayjs().year())

const dimensionLabel: Record<Dimension, string> = {
  project: '项目维度',
  customer: '客户维度',
  region: '区域维度',
  productLine: '产品线维度'
}

/** 维度对应名称字段（后端在 by-customer 等接口中复用 projectName 字段存储客户/区域/产品线名） */
const nameField = computed(() => {
  switch (dimension.value) {
    case 'customer':
      return '客户'
    case 'region':
      return '区域'
    case 'productLine':
      return '产品线'
    default:
      return '项目'
  }
})

const columns = computed(() => {
  const base = [
    { title: nameField.value, dataIndex: 'projectName', key: 'projectName', ellipsis: true, width: 220 },
    { title: '收入', dataIndex: 'revenue', key: 'revenue', width: 140 },
    { title: '自有成本', dataIndex: 'selfCost', key: 'selfCost', width: 140 },
    { title: '代理商成本', dataIndex: 'agentCost', key: 'agentCost', width: 140 },
    { title: '总成本', dataIndex: 'totalCost', key: 'totalCost', width: 140 },
    { title: '毛利润', key: 'profit', width: 140 },
    { title: '毛利率', key: 'profitMargin', width: 110 },
    { title: '自施占比', key: 'selfCostRatio', width: 110 },
    { title: '代施占比', key: 'agentCostRatio', width: 110 }
  ]
  return base
})

/** 汇总统计 */
const summary = computed(() => {
  const list = dataSource.value
  const totalRevenue = list.reduce((sum, d) => sum + (d.revenue || 0), 0)
  const totalSelfCost = list.reduce((sum, d) => sum + (d.selfCost || 0), 0)
  const totalAgentCost = list.reduce((sum, d) => sum + (d.agentCost || 0), 0)
  const totalCost = list.reduce((sum, d) => sum + (d.totalCost || 0), 0)
  const totalProfit = list.reduce((sum, d) => sum + (d.profit || 0), 0)
  const avgMargin = totalRevenue ? Math.round((totalProfit / totalRevenue) * 1000) / 10 : 0
  return {
    totalRevenue,
    totalSelfCost,
    totalAgentCost,
    totalCost,
    totalProfit,
    avgMargin,
    count: list.length
  }
})

async function loadData() {
  loading.value = true
  try {
    let res: FinanceProfit[] = []
    switch (dimension.value) {
      case 'project':
        res = (await listProjectProfit()) as unknown as FinanceProfit[]
        break
      case 'customer':
        res = (await listProfitByCustomer()) as unknown as FinanceProfit[]
        break
      case 'region':
        res = (await listProfitByRegion()) as unknown as FinanceProfit[]
        break
      case 'productLine':
        res = (await listProfitByProductLine()) as unknown as FinanceProfit[]
        break
    }
    dataSource.value = res || []
  } catch (e: any) {
    console.error('[finance.profit] load failed:', e)
    antdMessage.error(e?.message || '利润数据加载失败')
    dataSource.value = []
  } finally {
    loading.value = false
  }
}

function handleDimensionChange(d: Dimension) {
  dimension.value = d
  loadData()
}

function profitColor(profit: number | undefined): string {
  if (profit === undefined || profit === null) return 'default'
  return profit >= 0 ? 'success' : 'error'
}

function formatMoney(v: number | undefined): string {
  if (v === undefined || v === null) return '-'
  return v.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatPercent(v: number | undefined): string {
  if (v === undefined || v === null) return '-'
  return `${v}%`
}

/** 导出 CSV */
function handleExport() {
  if (!dataSource.value.length) {
    antdMessage.warning('暂无数据可导出')
    return
  }
  const header = [
    nameField.value,
    '收入',
    '自有成本',
    '代理商成本',
    '总成本',
    '毛利润',
    '毛利率(%)',
    '自施占比(%)',
    '代施占比(%)'
  ]
  const rows = dataSource.value.map((d) => [
    d.projectName || '-',
    d.revenue ?? 0,
    d.selfCost ?? 0,
    d.agentCost ?? 0,
    d.totalCost ?? 0,
    d.profit ?? 0,
    d.profitMargin ?? 0,
    d.selfCostRatio ?? 0,
    d.agentCostRatio ?? 0
  ])
  const csv = [header, ...rows]
    .map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','))
    .join('\n')
  // 加 BOM 头确保 Excel 正确识别 UTF-8
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `利润分析_${dimension.value}_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  antdMessage.success('已导出 CSV')
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="利润分析" description="项目级利润 / 毛利率 / 自施 vs 代施对比 / 多维度分析">
    <template #extra>
      <a-button @click="handleExport" :disabled="!dataSource.length">
        <template #icon><DownloadOutlined /></template>
        导出 CSV
      </a-button>
      <a-button @click="loadData" :loading="loading">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
    </template>

    <!-- 维度切换 + 筛选 -->
    <div class="vibe-card filter-card">
      <a-space wrap>
        <a-radio-group :value="dimension" button-style="solid" @change="(e: any) => handleDimensionChange(e.target.value)">
          <a-radio-button v-for="(label, key) in dimensionLabel" :key="key" :value="key">{{ label }}</a-radio-button>
        </a-radio-group>
        <a-input-number
          v-model:value="yearFilter"
          :min="2020"
          :max="2030"
          placeholder="年度"
          style="width: 120px"
        />
        <span class="filter-tip">共 <span class="tnum">{{ summary.count }}</span> 条记录</span>
      </a-space>
    </div>

    <!-- 汇总卡片 -->
    <a-row :gutter="16" class="stat-row">
      <a-col :xs="12" :md="6">
        <StatisticCard
          title="总收入"
          :value="formatMoney(summary.totalRevenue)"
          icon="DollarOutlined"
          accent="#52C41A"
          :loading="loading"
        />
      </a-col>
      <a-col :xs="12" :md="6">
        <StatisticCard
          title="总成本"
          :value="formatMoney(summary.totalCost)"
          icon="AccountBookOutlined"
          accent="#FAAD14"
          :loading="loading"
        />
      </a-col>
      <a-col :xs="12" :md="6">
        <StatisticCard
          title="毛利润"
          :value="formatMoney(summary.totalProfit)"
          icon="FundOutlined"
          :accent="summary.totalProfit >= 0 ? '#52C41A' : '#FF4D4F'"
          :loading="loading"
        />
      </a-col>
      <a-col :xs="12" :md="6">
        <StatisticCard
          title="平均毛利率"
          :value="summary.avgMargin"
          unit="%"
          icon="RiseOutlined"
          :accent="summary.avgMargin >= 0 ? '#52C41A' : '#FF4D4F'"
          :loading="loading"
        />
      </a-col>
    </a-row>

    <!-- 成本结构对比条 -->
    <div class="vibe-card cost-structure-card" v-if="dataSource.length">
      <div class="card-head">
        <h3 class="card-title">成本结构对比</h3>
        <span class="text-auxiliary">自施 vs 代施成本占比</span>
      </div>
      <div class="cost-structure-body">
        <div v-for="d in dataSource.slice(0, 10)" :key="d.projectId" class="cost-bar-item">
          <div class="cost-bar-label" :title="d.projectName">{{ d.projectName || '-' }}</div>
          <div class="cost-bar-track">
            <div
              class="cost-bar-self"
              :style="{ width: `${d.selfCostRatio ?? 0}%` }"
              :title="`自施：${formatMoney(d.selfCost)} (${d.selfCostRatio ?? 0}%)`"
            ></div>
            <div
              class="cost-bar-agent"
              :style="{ width: `${d.agentCostRatio ?? 0}%` }"
              :title="`代施：${formatMoney(d.agentCost)} (${d.agentCostRatio ?? 0}%)`"
            ></div>
          </div>
          <div class="cost-bar-legend">
            <span class="legend-self">自施 {{ d.selfCostRatio ?? 0 }}%</span>
            <span class="legend-agent">代施 {{ d.agentCostRatio ?? 0 }}%</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="vibe-card table-card">
      <div class="card-head">
        <h3 class="card-title">{{ dimensionLabel[dimension] }}明细</h3>
      </div>
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="{ pageSize: 20, showTotal: (t: number) => `共 ${t} 条` }"
        :row-key="(record: FinanceProfit) => record.projectId"
        size="small"
        :scroll="{ x: 1100 }"
      >
        <template #emptyText>
          <EmptyState description="暂无利润数据" />
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'revenue'">
            <span class="tnum">{{ formatMoney(record.revenue) }}</span>
          </template>
          <template v-else-if="column.key === 'selfCost'">
            <span class="tnum">{{ formatMoney(record.selfCost) }}</span>
          </template>
          <template v-else-if="column.key === 'agentCost'">
            <span class="tnum">{{ formatMoney(record.agentCost) }}</span>
          </template>
          <template v-else-if="column.key === 'totalCost'">
            <span class="tnum">{{ formatMoney(record.totalCost) }}</span>
          </template>
          <template v-else-if="column.key === 'profit'">
            <a-tag :color="profitColor(record.profit)">
              <span class="tnum">{{ formatMoney(record.profit) }}</span>
            </a-tag>
          </template>
          <template v-else-if="column.key === 'profitMargin'">
            <span :style="{ color: (record.profitMargin ?? 0) >= 0 ? '#52c41a' : '#ff4d4f', fontWeight: 600 }">
              {{ formatPercent(record.profitMargin) }}
            </span>
          </template>
          <template v-else-if="column.key === 'selfCostRatio'">
            <span class="tnum">{{ record.selfCostRatio ?? 0 }}%</span>
          </template>
          <template v-else-if="column.key === 'agentCostRatio'">
            <span class="tnum">{{ record.agentCostRatio ?? 0 }}%</span>
          </template>
        </template>
      </a-table>
    </div>
  </PageContainer>
</template>

<style lang="less" scoped>
.filter-card {
  padding: 16px 20px;
  margin-bottom: 16px;
}
.filter-tip {
  font-size: 13px;
  color: @text-tertiary;
  margin-left: 8px;
}
.stat-row {
  margin-bottom: 16px;
}
.text-auxiliary {
  color: @text-tertiary;
  font-size: 13px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid @border-color-split;
  flex-wrap: wrap;
  gap: 8px;
}
.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.table-card {
  padding: 0;
}

/* 成本结构对比条 */
.cost-structure-card {
  margin-bottom: 16px;
}
.cost-structure-body {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.cost-bar-item {
  display: grid;
  grid-template-columns: 180px 1fr 140px;
  gap: 12px;
  align-items: center;
}
.cost-bar-label {
  font-size: 13px;
  color: @text-secondary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cost-bar-track {
  display: flex;
  height: 20px;
  background: #f5f5f5;
  border-radius: 4px;
  overflow: hidden;
}
.cost-bar-self {
  background: @brand-primary;
  transition: width 0.3s;
}
.cost-bar-agent {
  background: @status-agent;
  transition: width 0.3s;
}
.cost-bar-legend {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: @text-tertiary;
}
.legend-self::before {
  content: '';
  display: inline-block;
  width: 10px;
  height: 10px;
  background: @brand-primary;
  border-radius: 2px;
  margin-right: 4px;
  vertical-align: middle;
}
.legend-agent::before {
  content: '';
  display: inline-block;
  width: 10px;
  height: 10px;
  background: @status-agent;
  border-radius: 2px;
  margin-right: 4px;
  vertical-align: middle;
}

/* 响应式：小屏紧凑 */
@media (max-width: 768px) {
  .cost-bar-item {
    grid-template-columns: 100px 1fr;
  }
  .cost-bar-legend {
    grid-column: 1 / -1;
  }
}
</style>
