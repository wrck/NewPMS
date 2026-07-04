<script setup lang="ts">
/**
 * 利润分析
 * 设计文档 2.8.4：项目级利润 / 毛利率 / 自施 vs 代施对比 / 多维度分析
 */
import { ref, onMounted } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import { listProjectProfit } from '@/api/finance'
import type { FinanceProfit } from '@/types/finance'

const loading = ref(false)
const dataSource = ref<FinanceProfit[]>([])

const columns = [
  { title: '项目', dataIndex: 'projectName', key: 'projectName', ellipsis: true },
  { title: '收入', dataIndex: 'revenue', key: 'revenue', width: 140 },
  { title: '自有成本', dataIndex: 'selfCost', key: 'selfCost', width: 140 },
  { title: '代理商成本', dataIndex: 'agentCost', key: 'agentCost', width: 140 },
  { title: '总成本', dataIndex: 'totalCost', key: 'totalCost', width: 140 },
  { title: '毛利润', dataIndex: 'profit', key: 'profit', width: 140 },
  { title: '毛利率(%)', dataIndex: 'profitMargin', key: 'profitMargin', width: 120 },
  { title: '自施占比(%)', dataIndex: 'selfCostRatio', key: 'selfCostRatio', width: 120 },
  { title: '代施占比(%)', dataIndex: 'agentCostRatio', key: 'agentCostRatio', width: 120 }
]

async function loadData() {
  loading.value = true
  try {
    dataSource.value = (await listProjectProfit()) as unknown as FinanceProfit[]
  } catch (e) {
    console.error('[finance.profit] load failed:', e)
  } finally {
    loading.value = false
  }
}

function profitColor(profit: number | undefined): string {
  if (profit === undefined || profit === null) return 'default'
  return profit >= 0 ? 'success' : 'error'
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="利润分析" description="项目级利润 / 毛利率 / 自施 vs 代施对比 / 多维度分析">
    <template #extra>
      <a-button @click="loadData">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
    </template>

    <a-table
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="false"
      :row-key="(record: FinanceProfit) => record.projectId"
    >
      <template #emptyText>
        <EmptyState description="暂无利润数据" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'profit'">
          <a-tag :color="profitColor(record.profit)">
            {{ record.profit ?? 0 }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'profitMargin'">
          <span :style="{ color: (record.profitMargin ?? 0) >= 0 ? '#52c41a' : '#ff4d4f' }">
            {{ record.profitMargin ?? 0 }}%
          </span>
        </template>
      </template>
    </a-table>

    <a-alert
      type="info"
      show-icon
      style="margin-top: 16px"
      message="说明"
      description="利润 = 收入 - 总成本；毛利率 = 利润 / 收入 × 100%。收入字段暂返回 0，待项目表补充合同金额后完善。"
    />
  </PageContainer>
</template>
