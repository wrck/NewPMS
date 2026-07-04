<script setup lang="ts">
/**
 * 设备看板
 * 设计文档 2.3 / 3.3.2：
 * - 总数 + 状态分布 + 产品线分布 + 异常设备列表 + 仓库库存统计
 * - 异常设备支持状态流转处理（恢复在网 / 标记报废）
 * - 仓库库存预警可下钻到出入库流水
 * - 各分布项可点击跳转到对应台账筛选
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message as antdMessage } from 'ant-design-vue'
import {
  ReloadOutlined,
  WarningOutlined,
  AppstoreOutlined,
  DatabaseOutlined,
  SwapOutlined,
  ToolOutlined,
  HddOutlined,
  CheckCircleOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatisticCard from '@/components/StatisticCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getDeviceDashboard, transitionDeviceStatus, listInventoryWarnings } from '@/api/device'
import type { DeviceDashboard, DeviceInstance, InventoryLedger } from '@/types/device'
import { DeviceStatus, DeviceStatusTone, DeviceStatusLabel } from '@/types/enum'

const router = useRouter()

const loading = ref(false)
const dashboard = ref<DeviceDashboard | null>(null)
/** 库存预警明细弹窗 */
const warningVisible = ref(false)
const warningLoading = ref(false)
const warningList = ref<InventoryLedger[]>([])
/** 状态流转弹窗 */
const transitionVisible = ref(false)
const transitionLoading = ref(false)
const currentDevice = ref<DeviceInstance | null>(null)
const transitionForm = reactive<{ targetStatus: DeviceStatus; remark: string }>({
  targetStatus: DeviceStatus.ONLINE,
  remark: ''
})

async function loadData() {
  loading.value = true
  try {
    dashboard.value = await getDeviceDashboard()
  } catch (e: any) {
    console.error('[device.board] load failed:', e)
    antdMessage.error(e?.message || '设备看板数据加载失败')
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

const totalAbnormal = computed(
  () => dashboard.value?.statusDistribution?.find((d) => d.status === DeviceStatus.ABNORMAL)?.count || 0
)
const totalWarning = computed(() =>
  (dashboard.value?.warehouseStockStats || []).reduce((sum, w) => sum + (w.warningQty || 0), 0)
)

/** 跳转设备台账（按状态筛选） */
function gotoLedger(status?: DeviceStatus) {
  router.push({ path: '/device/ledger', query: status ? { status } : {} })
}

/** 跳转设备型号库 */
function gotoModel() {
  router.push('/device/model')
}

/** 跳转出入库管理 */
function gotoInout(warehouseId?: number) {
  router.push({ path: '/device/inout', query: warehouseId ? { warehouseId } : {} })
}

/** 跳转备件管理 */
function gotoSpare() {
  router.push('/device/spare')
}

/** 查看库存预警明细 */
async function viewWarnings() {
  warningVisible.value = true
  warningLoading.value = true
  try {
    warningList.value = (await listInventoryWarnings()) || []
  } catch (e: any) {
    antdMessage.error('预警明细加载失败：' + (e?.message || '未知错误'))
    warningList.value = []
  } finally {
    warningLoading.value = false
  }
}

/** 打开状态流转弹窗 */
function openTransition(row: DeviceInstance) {
  currentDevice.value = row
  transitionForm.targetStatus = DeviceStatus.ONLINE
  transitionForm.remark = ''
  transitionVisible.value = true
}

/** 提交状态流转 */
async function handleTransition() {
  if (!currentDevice.value) return
  transitionLoading.value = true
  try {
    await transitionDeviceStatus(currentDevice.value.id, transitionForm.targetStatus, transitionForm.remark)
    antdMessage.success(`设备已${DeviceStatusLabel[transitionForm.targetStatus]}`)
    transitionVisible.value = false
    await loadData()
  } catch (e: any) {
    antdMessage.error('状态流转失败：' + (e?.message || '未知错误'))
  } finally {
    transitionLoading.value = false
  }
}

/** 异常设备表格列 */
const abnormalColumns = [
  { title: 'SN', dataIndex: 'serialNumber', key: 'serialNumber', ellipsis: true },
  { title: '型号', dataIndex: 'modelName', key: 'modelName', ellipsis: true },
  { title: '状态', key: 'status', width: 100 },
  { title: '位置', dataIndex: 'location', key: 'location', ellipsis: true },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const }
]

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="设备看板" description="设备状态分布、异常设备监控、库存预警">
    <template #extra>
      <a-space>
        <a-button @click="gotoModel"><template #icon><AppstoreOutlined /></template>设备型号</a-button>
        <a-button @click="gotoLedger()"><template #icon><DatabaseOutlined /></template>设备台账</a-button>
        <a-button @click="gotoInout()"><template #icon><SwapOutlined /></template>出入库</a-button>
        <a-button @click="gotoSpare"><template #icon><ToolOutlined /></template>备件管理</a-button>
        <a-button @click="loadData" :loading="loading"><template #icon><ReloadOutlined /></template>刷新</a-button>
      </a-space>
    </template>

    <a-spin :spinning="loading">
      <!-- 核心指标卡片 -->
      <a-row :gutter="16" class="stat-row">
        <a-col :xs="12" :md="6">
          <StatisticCard title="设备总数" :value="dashboard?.totalDevices || 0" unit="台" :icon="HddOutlined" :loading="loading" />
        </a-col>
        <a-col :xs="12" :md="6">
          <div class="clickable" @click="gotoLedger(DeviceStatus.ONLINE)">
            <StatisticCard
              title="在网设备"
              :value="dashboard?.statusDistribution?.find((d) => d.status === DeviceStatus.ONLINE)?.count || 0"
              unit="台"
              :icon="CheckCircleOutlined"
              accent="#52C41A"
              :loading="loading"
            />
          </div>
        </a-col>
        <a-col :xs="12" :md="6">
          <div class="clickable" @click="gotoLedger(DeviceStatus.ABNORMAL)">
            <StatisticCard
              title="异常设备"
              :value="totalAbnormal"
              unit="台"
              :icon="WarningOutlined"
              accent="#FF4D4F"
              :loading="loading"
            />
          </div>
        </a-col>
        <a-col :xs="12" :md="6">
          <div class="clickable" @click="gotoLedger(DeviceStatus.IN_FACTORY)">
            <StatisticCard
              title="在库设备"
              :value="dashboard?.statusDistribution?.find((d) => d.status === DeviceStatus.IN_FACTORY)?.count || 0"
              unit="台"
              :icon="DatabaseOutlined"
              :loading="loading"
            />
          </div>
        </a-col>
      </a-row>

      <!-- 状态分布 + 产品线分布 -->
      <a-row :gutter="16">
        <a-col :xs="24" :md="12">
          <div class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">状态分布</h3>
              <a-button size="small" type="link" @click="gotoLedger()">查看台账</a-button>
            </div>
            <div class="dist-list">
              <div
                v-for="d in dashboard?.statusDistribution"
                :key="d.status"
                class="dist-item clickable"
                @click="gotoLedger(d.status as DeviceStatus)"
              >
                <StatusTag :tone="DeviceStatusTone[d.status as DeviceStatus]">
                  {{ DeviceStatusLabel[d.status as DeviceStatus] }}
                </StatusTag>
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

      <!-- 异常设备列表 + 仓库库存统计 -->
      <a-row :gutter="16">
        <a-col :xs="24" :md="14">
          <div class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">
                <WarningOutlined :style="{ color: '#FF4D4F' }" />
                异常设备列表
                <a-tag v-if="totalAbnormal" color="red" class="count-tag">{{ totalAbnormal }}</a-tag>
              </h3>
            </div>
            <a-table
              :data-source="dashboard?.abnormalDevices || []"
              row-key="id"
              :pagination="{ pageSize: 5, showTotal: (t: number) => `共 ${t} 条` }"
              size="small"
              :columns="abnormalColumns"
              :scroll="{ x: 600 }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <StatusTag :tone="DeviceStatusTone[record.status as DeviceStatus]">
                    {{ DeviceStatusLabel[record.status as DeviceStatus] }}
                  </StatusTag>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-button type="link" size="small" @click="openTransition(record)">状态流转</a-button>
                </template>
              </template>
              <template #emptyText><EmptyState description="暂无异常设备" size="compact" /></template>
            </a-table>
          </div>
        </a-col>

        <a-col :xs="24" :md="10">
          <div class="vibe-card block-card">
            <div class="card-head">
              <h3 class="card-title">
                仓库库存统计
                <a-tag v-if="totalWarning" color="orange" class="count-tag">{{ totalWarning }} 预警</a-tag>
              </h3>
              <a-button v-if="totalWarning" size="small" type="link" @click="viewWarnings">查看预警</a-button>
            </div>
            <div class="warehouse-list">
              <div
                v-for="w in dashboard?.warehouseStockStats"
                :key="w.warehouseId"
                class="warehouse-item clickable"
                :class="{ warning: w.warningQty > 0 }"
                @click="gotoInout(w.warehouseId)"
              >
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

    <!-- 状态流转弹窗 -->
    <a-modal
      v-model:open="transitionVisible"
      title="设备状态流转"
      :confirm-loading="transitionLoading"
      @ok="handleTransition"
      @cancel="transitionVisible = false"
    >
      <a-alert
        v-if="currentDevice"
        message="当前设备"
        :description="`SN: ${currentDevice.serialNumber} · ${currentDevice.modelName || '-'} · ${currentDevice.location || '位置未填'}`"
        type="info"
        show-icon
        style="margin-bottom: 16px"
      />
      <a-form layout="vertical">
        <a-form-item label="目标状态" required>
          <a-select v-model:value="transitionForm.targetStatus" placeholder="请选择目标状态">
            <a-select-option :value="DeviceStatus.ONLINE">在网（已恢复）</a-select-option>
            <a-select-option :value="DeviceStatus.OFFLINE">离线</a-select-option>
            <a-select-option :value="DeviceStatus.SCRAPPED">报废</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="处理说明">
          <a-textarea
            v-model:value="transitionForm.remark"
            placeholder="请填写异常原因或处理说明"
            :rows="3"
            :maxlength="200"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 库存预警明细弹窗 -->
    <a-modal
      v-model:open="warningVisible"
      title="库存预警明细"
      width="800px"
      :footer="null"
      @cancel="warningVisible = false"
    >
      <a-spin :spinning="warningLoading">
        <a-table
          :data-source="warningList"
          row-key="id"
          size="small"
          :pagination="{ pageSize: 8, showTotal: (t: number) => `共 ${t} 条` }"
          :columns="[
            { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', ellipsis: true },
            { title: '型号', dataIndex: 'modelName', key: 'modelName', ellipsis: true },
            { title: '当前库存', key: 'stockQty', width: 100 },
            { title: '安全库存', dataIndex: 'safetyStockQty', key: 'safetyStockQty', width: 100 },
            { title: '缺口', key: 'gap', width: 80 }
          ]"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'stockQty'">
              <span class="tnum stock-warn">{{ record.stockQty }}</span>
            </template>
            <template v-else-if="column.key === 'gap'">
              <a-tag color="red">{{ Math.max(0, record.safetyStockQty - record.stockQty) }}</a-tag>
            </template>
          </template>
          <template #emptyText><EmptyState description="暂无预警" size="compact" /></template>
        </a-table>
      </a-spin>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.stat-row {
  margin-bottom: 16px;
}
.block-card {
  height: 100%;
}
.clickable {
  cursor: pointer;
  transition: transform 0.15s;
  &:hover {
    transform: translateY(-1px);
  }
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
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
.count-tag {
  margin-left: 4px;
  font-size: 12px;
  line-height: 18px;
  padding: 0 6px;
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
  transition: background 0.2s;
  &:hover {
    background: @bg-selected;
  }
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
  transition: background 0.2s;
  &:hover {
    background: @bg-selected;
  }
  &.warning {
    border-left: 3px solid @status-exception;
  }
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
.stock-warn {
  color: @status-exception;
  font-weight: 600;
}

/* 响应式：小屏 1 列分布 */
@media (max-width: 576px) {
  .dist-list {
    grid-template-columns: 1fr;
  }
}
</style>
