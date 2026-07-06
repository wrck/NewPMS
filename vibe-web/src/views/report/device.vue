<script setup lang="ts">
/**
 * 设备报表（spec 阶段三 Task 24 - SubTask 24.3）
 *
 * 全面 ECharts 化重写：
 *   - 设备状态分布 PieChart
 *   - 型号分布 BarChart（按产品线聚合）
 *   - 异常趋势 LineChart
 *   - 明细表格（BOM 完成率 + 库存状态）
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import { ImportExport } from '@/components/ImportExport'
import { PieChart, BarChart, LineChart } from '@/components/charts'
import { getDeviceReport } from '@/api/report'
import { DeviceStatus, DeviceStatusLabel } from '@/types/enum'

/* ============ 类型 ============ */
interface DeviceReportData {
  summary: { total: number; online: number; offline: number; abnormal: number }
  statusDistribution: Array<{ status: string; count: number }>
  productLineDistribution: Array<{ productLine: string; count: number }>
  bomCompletion: Array<{ projectId: number; projectName: string; totalQty: number; completedQty: number; rate: number }>
  inventoryStatus: Array<{ warehouseName: string; totalQty: number; warningQty: number }>
  /** 异常趋势（可选，后端未实现时为 undefined） */
  abnormalTrend?: Array<{ month: string; abnormalCount: number }>
}

/* ============ 状态 ============ */
const loading = ref(false)
const data = ref<DeviceReportData | null>(null)
const query = reactive({
  startDate: '',
  endDate: '',
  productLine: ''
})

/* ============ 数据加载 ============ */
async function loadData() {
  loading.value = true
  try {
    data.value = (await getDeviceReport(
      query as Record<string, unknown>
    )) as unknown as DeviceReportData
  } catch (e) {
    console.error('[report.device] load failed:', e)
    message.error('加载设备报表数据失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

function handleReset() {
  Object.assign(query, { startDate: '', endDate: '', productLine: '' })
  loadData()
}

/* ============ 设备状态分布 PieChart ============ */
const statusPieData = computed(() =>
  (data.value?.statusDistribution || []).map((s) => ({
    name: DeviceStatusLabel[s.status as DeviceStatus] || s.status,
    value: s.count
  }))
)

/* ============ 型号分布 BarChart（按产品线） ============ */
const productLineBarData = computed(() => {
  const list = data.value?.productLineDistribution || []
  if (list.length === 0) return { xAxis: [] as string[], series: [] as Array<{ name: string; data: number[] }> }
  return {
    xAxis: list.map((p) => p.productLine || '未分类'),
    series: [{ name: '设备数', data: list.map((p) => p.count) }]
  }
})

/* ============ 异常趋势 LineChart ============ */
const abnormalTrendData = computed(() => {
  const trend = data.value?.abnormalTrend || []
  if (trend.length === 0) return { xAxis: [] as string[], series: [] as Array<{ name: string; data: number[] }> }
  return {
    xAxis: trend.map((t) => t.month),
    series: [{ name: '异常设备数', data: trend.map((t) => t.abnormalCount) }]
  }
})

/* ============ 明细表格 ============ */
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

const bomData = computed(() => data.value?.bomCompletion || [])
const inventoryData = computed(() => data.value?.inventoryStatus || [])

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="设备报表" description="设备状态分布、型号分布、异常趋势与 BOM 完成率">
    <template #extra>
      <a-button @click="loadData" :loading="loading">
        <template #icon><ReloadOutlined /></template>刷新
      </a-button>
    </template>

    <!-- 筛选 + ImportExport -->
    <a-card class="filter-card" :bordered="true">
      <a-form layout="inline" :model="query">
        <a-form-item label="产品线">
          <a-input v-model:value="query.productLine" placeholder="如：路由/交换" allow-clear style="width: 160px" />
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
          export-api="/api/v1/report/device/export"
          :export-params="query as Record<string, unknown>"
          :show-import="false"
          :show-template="false"
        />
      </div>
    </a-card>

    <a-spin :spinning="loading">
      <!-- 状态分布 + 型号分布 -->
      <a-row :gutter="16" class="chart-row">
        <a-col :xs="24" :md="12">
          <a-card title="设备状态分布" :bordered="true">
            <PieChart :data="statusPieData" :height="320" />
          </a-card>
        </a-col>
        <a-col :xs="24" :md="12">
          <a-card title="型号分布" :bordered="true">
            <BarChart :data="productLineBarData.series" :x-axis="productLineBarData.xAxis" :height="320" />
          </a-card>
        </a-col>
      </a-row>

      <!-- 异常趋势 LineChart -->
      <a-card title="异常趋势" :bordered="true" class="chart-row">
        <LineChart
          :data="abnormalTrendData.series"
          :x-axis="abnormalTrendData.xAxis"
          smooth
          area
          :height="320"
        />
      </a-card>

      <!-- BOM 完成率明细 -->
      <a-card title="各项目 BOM 完成率" :bordered="true" class="table-card">
        <a-table
          :columns="bomColumns"
          :data-source="bomData"
          row-key="projectId"
          size="small"
          :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'rate'">
              <ProgressBar :percent="record.rate" />
              <span style="margin-left: 8px; color: #595959">{{ record.rate }}%</span>
            </template>
          </template>
          <template #emptyText><EmptyState description="暂无 BOM 数据" /></template>
        </a-table>
      </a-card>

      <!-- 库存状态 -->
      <a-card title="库存状态" :bordered="true">
        <a-table
          :columns="inventoryColumns"
          :data-source="inventoryData"
          row-key="warehouseName"
          size="small"
          :pagination="false"
        >
          <template #emptyText><EmptyState description="暂无库存数据" /></template>
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
