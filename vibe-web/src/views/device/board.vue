<script setup lang="ts">
/**
 * 设备看板
 * - 总数 + 状态分布 + 产品线分布 + 异常列表 + 仓库统计
 */
import { ref, onMounted, computed } from 'vue'
import { ReloadOutlined, WarningOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatisticCard from '@/components/StatisticCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getDeviceDashboard } from '@/api/device'
import type { DeviceDashboard } from '@/types/device'
import { DeviceStatus, DeviceStatusTone, DeviceStatusLabel } from '@/types/enum'

const loading = ref(false)
const dashboard = ref<DeviceDashboard | null>(null)

async function loadData() {
  loading.value = true
  try {
    dashboard.value = await getDeviceDashboard()
  } catch (e) {
    console.error('[device.board] load failed:', e)
  } finally {
    loading.value = false
  }
}

const productLineLabel: Record<string, string> = {
  ROUTER: '路由',
  SWITCH: '交换',
  WIRELESS: '无线',
  SECURITY: '安全',
  DC: '数据中心',
  OTHER: '其他'
}

const totalAbnormal = computed(() => dashboard.value?.statusDistribution?.find((d) => d.status === DeviceStatus.ABNORMAL)?.count || 0)

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="设备看板" description="设备状态分布、异常设备监控、库存预警">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
    </template>

    <a-spin :spinning="loading">
      <a-row :gutter="16" class="stat-row">
        <a-col :xs="12" :md="6">
          <StatisticCard title="设备总数" :value="dashboard?.totalDevices || 0" unit="台" icon="HddOutlined" />
        </a-col>
        <a-col :xs="12" :md="6">
          <StatisticCard title="在网设备" :value="dashboard?.statusDistribution?.find(d => d.status === DeviceStatus.ONLINE)?.count || 0" unit="台" icon="CheckCircleOutlined" accent="#52C41A" />
        </a-col>
        <a-col :xs="12" :md="6">
          <StatisticCard title="异常设备" :value="totalAbnormal" unit="台" icon="WarningOutlined" accent="#FF4D4F" />
        </a-col>
        <a-col :xs="12" :md="6">
          <StatisticCard title="在库设备" :value="dashboard?.statusDistribution?.find(d => d.status === DeviceStatus.IN_FACTORY)?.count || 0" unit="台" icon="DatabaseOutlined" />
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :xs="24" :md="12">
          <div class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">状态分布</h3>
            </div>
            <div class="dist-list">
              <div v-for="d in dashboard?.statusDistribution" :key="d.status" class="dist-item">
                <StatusTag :tone="DeviceStatusTone[d.status as DeviceStatus]">{{ DeviceStatusLabel[d.status as DeviceStatus] }}</StatusTag>
                <span class="dist-count tnum">{{ d.count }}</span>
              </div>
              <EmptyState v-if="!dashboard?.statusDistribution?.length" description="暂无数据" size="compact" />
            </div>
          </div>
        </a-col>

        <a-col :xs="24" :md="12">
          <div class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">产品线分布</h3>
            </div>
            <div class="dist-list">
              <div v-for="d in dashboard?.productLineDistribution" :key="d.productLine" class="dist-item">
                <span class="dist-label">{{ productLineLabel[d.productLine] || d.productLine }}</span>
                <span class="dist-count tnum">{{ d.count }}</span>
              </div>
              <EmptyState v-if="!dashboard?.productLineDistribution?.length" description="暂无数据" size="compact" />
            </div>
          </div>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :xs="24" :md="14">
          <div class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">
                <WarningOutlined :style="{ color: '#FF4D4F' }" />
                异常设备列表
              </h3>
            </div>
            <a-table
              :data-source="dashboard?.abnormalDevices || []"
              row-key="id"
              :pagination="{ pageSize: 5 }"
              size="small"
              :columns="[
                { title: 'SN', dataIndex: 'serialNumber', key: 'serialNumber' },
                { title: '型号', dataIndex: 'modelName', key: 'modelName' },
                { title: '状态', key: 'status', width: 100 },
                { title: '位置', dataIndex: 'location', key: 'location' }
              ]"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <StatusTag :tone="DeviceStatusTone[record.status as DeviceStatus]">{{ DeviceStatusLabel[record.status as DeviceStatus] }}</StatusTag>
                </template>
              </template>
              <template #emptyText><EmptyState description="暂无异常设备" size="compact" /></template>
            </a-table>
          </div>
        </a-col>

        <a-col :xs="24" :md="10">
          <div class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">仓库库存统计</h3>
            </div>
            <div class="warehouse-list">
              <div v-for="w in dashboard?.warehouseStockStats" :key="w.warehouseId" class="warehouse-item">
                <div class="warehouse-head">
                  <span class="warehouse-name">{{ w.warehouseName }}</span>
                  <span v-if="w.warningQty > 0" class="warning-tag">{{ w.warningQty }} 预警</span>
                </div>
                <div class="warehouse-total tnum">总数 {{ w.totalQty }}</div>
              </div>
              <EmptyState v-if="!dashboard?.warehouseStockStats?.length" description="暂无数据" size="compact" />
            </div>
          </div>
        </a-col>
      </a-row>
    </a-spin>
  </PageContainer>
</template>

<style lang="less" scoped>
.stat-row { margin-bottom: 16px; }
.block-card { height: 100%; }
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}
.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.dist-list {
  padding: 16px 20px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.dist-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: @bg-stripe;
  border-radius: 4px;
}
.dist-label {
  font-size: 14px;
  color: @text-secondary;
}
.dist-count {
  font-size: 16px;
  font-weight: 600;
  color: @text-primary;
}
.warehouse-list {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.warehouse-item {
  padding: 12px;
  background: @bg-stripe;
  border-radius: 6px;
}
.warehouse-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.warehouse-name {
  font-weight: 500;
}
.warning-tag {
  padding: 0 8px;
  height: 20px;
  line-height: 20px;
  font-size: 12px;
  background: @status-bg-exception;
  color: @status-exception;
  border-radius: 10px;
}
.warehouse-total {
  font-size: 13px;
  color: @text-secondary;
}
</style>
