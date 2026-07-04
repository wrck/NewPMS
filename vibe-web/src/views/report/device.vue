<script setup lang="ts">
/**
 * 设备报表
 * 设备维度统计：状态分布、产品线分布、各项目 BOM 完成率、库存状态
 */
import { ref, reactive, onMounted } from 'vue'
import { ReloadOutlined, ExportOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatisticCard from '@/components/StatisticCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getDeviceReport } from '@/api/report'
import { DeviceStatus, DeviceStatusTone, DeviceStatusLabel } from '@/types/enum'

interface DeviceReportData {
  summary: { total: number; online: number; offline: number; abnormal: number }
  statusDistribution: Array<{ status: string; count: number }>
  productLineDistribution: Array<{ productLine: string; count: number }>
  bomCompletion: Array<{ projectId: number; projectName: string; totalQty: number; completedQty: number; rate: number }>
  inventoryStatus: Array<{ warehouseName: string; totalQty: number; warningQty: number }>
}

const loading = ref(false)
const data = ref<DeviceReportData | null>(null)
const query = reactive({
  startDate: '',
  endDate: '',
  productLine: ''
})

async function loadData() {
  loading.value = true
  try {
    data.value = (await getDeviceReport(query as Record<string, unknown>)) as unknown as DeviceReportData
  } catch (e) {
    console.error('[report.device] load failed:', e)
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

function statusLabel(s: string): string {
  return (DeviceStatusLabel as Record<string, string>)[s] || s
}

function statusTone(s: string) {
  return (DeviceStatusTone as Record<string, string>)[s as DeviceStatus] || 'default'
}

const bomColumns = [
  { title: '项目名称', dataIndex: 'projectName', key: 'projectName', ellipsis: true },
  { title: 'BOM 总数', dataIndex: 'totalQty', key: 'totalQty', width: 110 },
  { title: '已到货', dataIndex: 'completedQty', key: 'completedQty', width: 110 },
  { title: '完成率', key: 'rate', width: 200 }
]

const inventoryColumns = [
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName' },
  { title: '在库设备数', dataIndex: 'totalQty', key: 'totalQty', width: 140 },
  { title: '预警数', dataIndex: 'warningQty', key: 'warningQty', width: 120 }
]

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="设备报表" description="设备状态分布、产品线分布、各项目 BOM 完成率与库存状态">
    <template #extra>
      <a-button @click="loadData" :loading="loading"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button @click="handleExport"><template #icon><ExportOutlined /></template>导出</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="产品线">
          <a-input v-model:value="query.productLine" placeholder="如：路由/交换" allow-clear style="width: 160px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <a-spin :spinning="loading">
      <a-row :gutter="16" class="stat-row">
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="设备总数" :value="data?.summary.total ?? 0" unit="台" icon="HddOutlined" />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="在网" :value="data?.summary.online ?? 0" unit="台" icon="CheckCircleOutlined" />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="在库/在途" :value="data?.summary.offline ?? 0" unit="台" icon="ClockCircleOutlined" />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <StatisticCard title="异常" :value="data?.summary.abnormal ?? 0" unit="台" icon="WarningOutlined" accent="#ff4d4f" />
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :xs="24" :md="12">
          <div class="vibe-card block-card">
            <div class="card-head"><h3 class="card-title">状态分布</h3></div>
            <div class="card-body">
              <EmptyState v-if="!data?.statusDistribution?.length" description="暂无数据" size="compact" />
              <div v-else class="dist-list">
                <div v-for="d in data.statusDistribution" :key="d.status" class="dist-item">
                  <StatusTag :tone="statusTone(d.status)">{{ statusLabel(d.status) }}</StatusTag>
                  <span class="tnum">{{ d.count }}</span>
                </div>
              </div>
            </div>
          </div>
        </a-col>
        <a-col :xs="24" :md="12">
          <div class="vibe-card block-card">
            <div class="card-head"><h3 class="card-title">产品线分布</h3></div>
            <div class="card-body">
              <EmptyState v-if="!data?.productLineDistribution?.length" description="暂无数据" size="compact" />
              <div v-else class="dist-list">
                <div v-for="d in data.productLineDistribution" :key="d.productLine" class="dist-item">
                  <span>{{ d.productLine || '未分类' }}</span>
                  <span class="tnum">{{ d.count }}</span>
                </div>
              </div>
            </div>
          </div>
        </a-col>
      </a-row>

      <div class="vibe-card block-card">
        <div class="card-head"><h3 class="card-title">各项目 BOM 完成率</h3></div>
        <a-table :columns="bomColumns" :data-source="data?.bomCompletion || []" row-key="projectId" size="small" :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'rate'">
              <ProgressBar :percent="record.rate" />
              <span style="margin-left: 8px; color: #595959">{{ record.rate }}%</span>
            </template>
          </template>
          <template #emptyText><EmptyState description="暂无 BOM 数据" /></template>
        </a-table>
      </div>

      <div class="vibe-card table-card">
        <div class="card-head"><h3 class="card-title">库存状态</h3></div>
        <a-table :columns="inventoryColumns" :data-source="data?.inventoryStatus || []" row-key="warehouseName" size="small" :pagination="false">
          <template #emptyText><EmptyState description="暂无库存数据" /></template>
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
