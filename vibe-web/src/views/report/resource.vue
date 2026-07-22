<script setup lang="ts">
/**
 * 资源报表（spec 阶段三 Task 24 - SubTask 24.4）
 *
 * 全面 ECharts 化重写：
 *   - 工时统计 StackedChart（按工程师堆叠：正常工时 + 加班工时）
 *   - 工程师负荷 RadarChart（任务数/工时/利用率/按时率）
 *   - 出差分布 MapChart
 *   - 明细表格（工程师负荷 + 项目投入）
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import { ImportExport } from '@/components/ImportExport'
import { StackedChart, RadarChart, MapChart } from '@/components/charts'
import { getResourceReport } from '@/api/report'
import { pageEngineers } from '@/api/resource'
import { listOrgTree } from '@/api/system'

/* ============ 类型 ============ */
interface EngineerRow {
  engineerId: number
  engineerName: string
  taskCount: number
  hours: number
  overtimeHours: number
  utilization: number
  onTimeRate: number
  /** 出差次数（可选，后端未实现时为 undefined） */
  travelCount?: number
  /** 工程师所属区域（可选，用于 MapChart） */
  region?: string
}

interface ResourceReportData {
  summary: { totalEngineers: number; avgUtilization: number; totalHours: number; overtimeHours: number }
  byEngineer: EngineerRow[]
  byProject: Array<{ projectId: number; projectName: string; hours: number; engineerCount: number }>
  /** 出差分布（可选，后端未实现时为 undefined） */
  travelDistribution?: Array<{ region: string; count: number }>
}

/* ============ 状态 ============ */
const loading = ref(false)
const data = ref<ResourceReportData | null>(null)
const query = reactive({
  startDate: '',
  endDate: '',
  engineerId: undefined as number | undefined,
  orgId: undefined as number | undefined
})

/* ============ 数据加载 ============ */
async function loadData() {
  loading.value = true
  try {
    data.value = (await getResourceReport(
      query as Record<string, unknown>
    )) as unknown as ResourceReportData
  } catch (e) {
    console.error('[report.resource] load failed:', e)
    message.error('加载资源报表数据失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

function handleReset() {
  Object.assign(query, { startDate: '', endDate: '', engineerId: undefined, orgId: undefined })
  loadData()
}

/* ============ 工时统计 StackedChart ============ */
const hoursStacked = computed(() => {
  const list = data.value?.byEngineer || []
  if (list.length === 0) return { xAxis: [] as string[], series: [] as Array<{ name: string; data: number[] }> }
  return {
    xAxis: list.map((e) => e.engineerName),
    series: [
      { name: '正常工时', data: list.map((e) => Math.max(0, e.hours - e.overtimeHours)) },
      { name: '加班工时', data: list.map((e) => e.overtimeHours) }
    ]
  }
})

/* ============ 工程师负荷 RadarChart ============ */
const radarData = computed(() => {
  const list = data.value?.byEngineer || []
  if (list.length === 0) return { indicators: [] as Array<{ name: string; max: number }>, series: [] as Array<{ name: string; value: number[] }> }

  // 计算各维度最大值（利用率/按时率最大为 100）
  const maxTask = Math.max(...list.map((e) => e.taskCount), 1)
  const maxHours = Math.max(...list.map((e) => Number(e.hours) || 0), 1)
  const indicators = [
    { name: '任务数', max: maxTask },
    { name: '工时', max: maxHours },
    { name: '利用率', max: 100 },
    { name: '按时率', max: 100 }
  ]
  return {
    indicators,
    series: list.map((e) => ({
      name: e.engineerName,
      // Long/BigDecimal 经 JacksonConfig 序列化为字符串，需转 number 供 ECharts 使用
      value: [Number(e.taskCount) || 0, Number(e.hours) || 0, Number(e.utilization) || 0, Number(e.onTimeRate) || 0]
    }))
  }
})

/* ============ 出差分布 MapChart ============ */
const travelMapData = computed(() => {
  const list = data.value?.travelDistribution || []
  return list.map((t) => ({ name: t.region, value: t.count }))
})

/* ============ 明细表格 ============ */
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

const engineerData = computed(() => data.value?.byEngineer || [])
const projectData = computed(() => data.value?.byProject || [])

// 工程师下拉选项（实体引用字段：engineerId）
const engineerOptions = ref<Array<{ value: string | number; label: string }>>([])
async function loadEngineers() {
  try {
    const res = await pageEngineers({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    engineerOptions.value = list.map((e: any) => ({ value: e.id, label: e.name || e.engineerNo }))
  } catch (e) {
    console.warn('[engineer] load failed:', e)
  }
}

// 组织树下拉选项（实体引用字段：orgId）
const orgTreeData = ref<any[]>([])
async function loadOrgTree() {
  try {
    orgTreeData.value = (await listOrgTree()) || []
  } catch (e) {
    console.warn('[org] load failed:', e)
  }
}

onMounted(() => {
  loadData()
  loadEngineers()
  loadOrgTree()
})
</script>

<template>
  <PageContainer title="资源报表" description="工程师负荷、工时统计、出差分布与项目投入分析">
    <template #extra>
      <a-button @click="loadData" :loading="loading">
        <template #icon><ReloadOutlined /></template>刷新
      </a-button>
    </template>

    <!-- 筛选 + ImportExport -->
    <a-card class="filter-card" :bordered="true">
      <a-form layout="inline" :model="query">
        <a-form-item label="工程师">
          <a-select
            v-model:value="query.engineerId"
            show-search
            allow-clear
            :options="engineerOptions"
            :filter-option="(input: string, option: any) => option.label.includes(input)"
            placeholder="全部工程师"
            style="width: 160px"
          />
        </a-form-item>
        <a-form-item label="组织">
          <a-tree-select
            v-model:value="query.orgId"
            allow-clear
            :tree-data="orgTreeData"
            :field-names="{ label: 'orgName', value: 'id', children: 'children' }"
            placeholder="全部组织"
            style="width: 160px"
          />
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
          export-api="/api/v1/report/resource/export"
          :export-params="query as Record<string, unknown>"
          :show-import="false"
          :show-template="false"
        />
      </div>
    </a-card>

    <a-spin :spinning="loading">
      <!-- 工时统计 StackedChart + 工程师负荷 RadarChart -->
      <a-row :gutter="16" class="chart-row">
        <a-col :xs="24" :md="12">
          <a-card title="工时统计（按工程师）" :bordered="true">
            <StackedChart :data="hoursStacked.series" :x-axis="hoursStacked.xAxis" :height="320" />
          </a-card>
        </a-col>
        <a-col :xs="24" :md="12">
          <a-card title="工程师负荷雷达" :bordered="true">
            <RadarChart :data="radarData.series" :indicators="radarData.indicators" :height="320" />
          </a-card>
        </a-col>
      </a-row>

      <!-- 出差分布 MapChart -->
      <a-card title="出差分布" :bordered="true" class="chart-row">
        <MapChart :data="travelMapData" :height="360" />
      </a-card>

      <!-- 工程师负荷明细 -->
      <a-card title="工程师负荷明细" :bordered="true" class="table-card">
        <a-table
          :columns="engineerColumns"
          :data-source="engineerData"
          row-key="engineerId"
          size="small"
          :scroll="{ x: 800 }"
          :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }"
        >
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
      </a-card>

      <!-- 项目维度投入 -->
      <a-card title="项目维度投入" :bordered="true">
        <a-table
          :columns="projectColumns"
          :data-source="projectData"
          row-key="projectId"
          size="small"
          :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }"
        >
          <template #emptyText><EmptyState description="暂无项目投入数据" /></template>
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
