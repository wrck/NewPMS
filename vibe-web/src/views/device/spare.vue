<script setup lang="ts">
/**
 * 备件管理（增强版）
 * - 备件台账 CRUD + 库存展示
 * - 领用/归还/返修/入库 操作弹窗（对齐后端 POST /spare-parts/actions）
 * - 流水查询抽屉（GET /spare-parts/logs）
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined,
  HistoryOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageSpareParts,
  createSparePart,
  updateSparePart,
  deleteSparePart,
  sparePartAction,
  listSparePartLogs,
  pageDeviceModels
} from '@/api/device'
import { pageProjects } from '@/api/project'
import type { SparePart, SparePartQueryParams, SparePartLog, SparePartActionDTO } from '@/types/device'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<SparePart[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50', '100']
})
const query = reactive<SparePartQueryParams>({ keyword: '', category: undefined, warehouseId: undefined, status: undefined })

async function loadData() {
  loading.value = true
  try {
    const res = (await pageSpareParts({
      ...query,
      page: pagination.current,
      size: pagination.pageSize
    })) as unknown as PageResult<SparePart>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[device.spare] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  query.keyword = ''
  query.category = undefined
  query.warehouseId = undefined
  query.status = undefined
  handleSearch()
}

function handleTableChange(p: any) {
  pagination.current = p.current || 1
  pagination.pageSize = p.pageSize || 10
  loadData()
}

/* ============ 备件 CRUD 弹窗 ============ */
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<Partial<SparePart>>({
  partCode: '',
  partName: '',
  modelId: undefined,
  category: 'ROUTER',
  unit: '个',
  stockQty: 0,
  safetyStockQty: 0,
  warehouseId: undefined,
  remark: ''
})

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    partCode: '',
    partName: '',
    modelId: undefined,
    category: 'ROUTER',
    unit: '个',
    stockQty: 0,
    safetyStockQty: 0,
    warehouseId: undefined,
    remark: ''
  })
  formVisible.value = true
  loadModelOptions()
}

function openEdit(row: SparePart) {
  isEdit.value = true
  Object.assign(formData, row)
  formVisible.value = true
  loadModelOptions()
}

async function handleSubmit() {
  if (!formData.partCode || !formData.partName) {
    message.warning('请填写备件编码和名称')
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateSparePart(formData.id, formData)
      message.success('更新成功')
    } else {
      await createSparePart(formData)
      message.success('创建成功')
    }
    formVisible.value = false
    loadData()
  } catch (e) {
    console.error('[device.spare] submit failed:', e)
  } finally {
    formLoading.value = false
  }
}

function handleDelete(row: SparePart) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除备件「${row.partName}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteSparePart(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        console.error('[device.spare] delete failed:', e)
      }
    }
  })
}

/* ============ 领用/归还/返修/入库 操作弹窗 ============ */
type ActionType = 'IN' | 'OUT' | 'RETURN' | 'REPAIR'

const actionVisible = ref(false)
const actionLoading = ref(false)
const actionType = ref<ActionType>('OUT')
const actionRow = ref<SparePart | null>(null)
const actionForm = reactive<{ quantity: number; projectId?: number; remark: string }>({
  quantity: 1,
  projectId: undefined,
  remark: ''
})

const actionTypeLabel: Record<ActionType, string> = {
  IN: '入库',
  OUT: '领用',
  RETURN: '归还',
  REPAIR: '返修'
}

function openAction(row: SparePart, type: ActionType) {
  actionRow.value = row
  actionType.value = type
  actionForm.quantity = 1
  actionForm.projectId = undefined
  actionForm.remark = ''
  actionVisible.value = true
  loadProjectOptions()
}

async function handleAction() {
  if (!actionRow.value) return
  if (!actionForm.quantity || actionForm.quantity < 1) {
    message.warning('请填写有效数量')
    return
  }
  // 领用/返修时校验库存
  if (actionType.value === 'OUT' && actionForm.quantity > actionRow.value.stockQty) {
    message.warning(`库存不足，当前库存 ${actionRow.value.stockQty}`)
    return
  }
  actionLoading.value = true
  try {
    const dto: SparePartActionDTO = {
      sparePartId: actionRow.value.id,
      actionType: actionType.value,
      quantity: actionForm.quantity,
      projectId: actionForm.projectId,
      remark: actionForm.remark
    }
    await sparePartAction(dto)
    message.success(`${actionTypeLabel[actionType.value]}成功`)
    actionVisible.value = false
    loadData()
  } catch (e) {
    console.error('[device.spare] action failed:', e)
  } finally {
    actionLoading.value = false
  }
}

/* ============ 流水查询抽屉 ============ */
const logVisible = ref(false)
const logLoading = ref(false)
const logRow = ref<SparePart | null>(null)
const logDataSource = ref<SparePartLog[]>([])

async function openLogs(row: SparePart) {
  logRow.value = row
  logVisible.value = true
  logLoading.value = true
  try {
    const res = (await listSparePartLogs({ sparePartId: row.id })) as unknown as SparePartLog[]
    logDataSource.value = res || []
  } catch (e) {
    console.error('[device.spare] load logs failed:', e)
    logDataSource.value = []
  } finally {
    logLoading.value = false
  }
}

const actionTypeTone: Record<string, string> = {
  IN: 'success',
  OUT: 'warning',
  RETURN: 'processing',
  REPAIR: 'error'
}

const statusMap: Record<string, { tone: any; label: string }> = {
  IN_STOCK: { tone: 'success', label: '在库' },
  OUT: { tone: 'warning', label: '已领用' },
  REPAIR: { tone: 'error', label: '返修中' },
  SCRAPPED: { tone: 'archived', label: '已报废' }
}

const categoryOptions = [
  { value: 'ROUTER', label: '路由器' },
  { value: 'SWITCH', label: '交换机' },
  { value: 'AP', label: 'AP' },
  { value: 'FIREWALL', label: '防火墙' },
  { value: 'WLC', label: '无线控制器' },
  { value: 'LB', label: '负载均衡' },
  { value: 'OTHER', label: '其他' }
]

// 实体引用字段下拉选项（型号/项目）
const modelOptions = ref<Array<{ value: string | number; label: string }>>([])
const projectOptions = ref<Array<{ value: string | number; label: string }>>([])

async function loadModelOptions() {
  try {
    const res = await pageDeviceModels({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    modelOptions.value = list.map((m: any) => ({ value: m.id, label: m.modelName }))
  } catch (e) {
    console.warn('[device.spare] load models failed:', e)
  }
}

async function loadProjectOptions() {
  try {
    const res = await pageProjects({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    projectOptions.value = list.map((p: any) => ({ value: p.id, label: p.projectName }))
  } catch (e) {
    console.warn('[device.spare] load projects failed:', e)
  }
}

const columns = [
  { title: '备件编码', dataIndex: 'partCode', key: 'partCode', width: 140 },
  { title: '备件名称', dataIndex: 'partName', key: 'partName', ellipsis: true },
  { title: '类别', dataIndex: 'category', key: 'category', width: 110 },
  { title: '所属型号', dataIndex: 'modelName', key: 'modelName', width: 140 },
  { title: '库存', key: 'stock', width: 130 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 320, fixed: 'right' as const }
]

const logColumns = [
  { title: '操作类型', dataIndex: 'actionType', key: 'actionType', width: 100 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '关联项目', dataIndex: 'projectName', key: 'projectName', width: 160, ellipsis: true },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作时间', dataIndex: 'createTime', key: 'createTime', width: 170 }
]

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="备件管理" description="备件台账与领用/归还/返修/入库操作及流水跟踪">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新备件</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="关键字">
          <a-input v-model:value="query.keyword" placeholder="备件编码/名称" allow-clear style="width: 200px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="类别">
          <a-select v-model:value="query.category" placeholder="全部" allow-clear style="width: 140px" :options="categoryOptions" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 130px">
            <a-select-option v-for="(v, k) in statusMap" :key="k" :value="k">{{ v.label }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </div>

    <div class="vibe-card table-card">
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1400 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'category'">
            {{ categoryOptions.find((c) => c.value === record.category)?.label || record.category }}
          </template>
          <template v-else-if="column.key === 'stock'">
            <span :class="{ warning: record.stockQty < record.safetyStockQty }">
              {{ record.stockQty }} / {{ record.safetyStockQty }}
            </span>
            <span class="text-auxiliary" style="margin-left: 4px">/{{ record.unit || '个' }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="statusMap[record.status]?.tone">{{ statusMap[record.status]?.label || record.status }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small" wrap>
              <a @click="openAction(record, 'OUT')">领用</a>
              <a-divider type="vertical" />
              <a @click="openAction(record, 'RETURN')">归还</a>
              <a-divider type="vertical" />
              <a @click="openAction(record, 'REPAIR')">返修</a>
              <a-divider type="vertical" />
              <a @click="openAction(record, 'IN')">入库</a>
              <a-divider type="vertical" />
              <a @click="openLogs(record)"><HistoryOutlined /> 流水</a>
              <a-divider type="vertical" />
              <a @click="openEdit(record)"><EditOutlined /></a>
              <a-divider type="vertical" />
              <a class="danger-link" @click="handleDelete(record)"><DeleteOutlined /></a>
            </a-space>
          </template>
        </template>
        <template #emptyText>
          <EmptyState description="暂无备件" action-text="新备件" @action="openCreate" />
        </template>
      </a-table>
    </div>

    <!-- 备件 CRUD 弹窗 -->
    <a-modal
      v-model:open="formVisible"
      :title="isEdit ? '编辑备件' : '新设备件'"
      width="560px"
      :confirm-loading="formLoading"
      :mask-closable="false"
      @ok="handleSubmit"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="备件编码" required>
              <a-input v-model:value="formData.partCode" :disabled="isEdit" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="备件名称" required>
              <a-input v-model:value="formData.partName" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="类别">
              <a-select v-model:value="formData.category" :options="categoryOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="所属型号">
              <a-select
                v-model:value="formData.modelId"
                show-search
                allow-clear
                placeholder="选择型号"
                style="width: 100%"
                :options="modelOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="单位">
              <a-input v-model:value="formData.unit" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="库存数">
              <a-input-number v-model:value="formData.stockQty" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="安全库存">
              <a-input-number v-model:value="formData.safetyStockQty" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注">
              <a-textarea v-model:value="formData.remark" :rows="2" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 领用/归还/返修/入库 操作弹窗 -->
    <a-modal
      v-model:open="actionVisible"
      :title="`${actionTypeLabel[actionType]}备件`"
      :confirm-loading="actionLoading"
      :mask-closable="false"
      width="480px"
      @ok="handleAction"
    >
      <a-alert
        v-if="actionRow"
        :message="`备件：${actionRow.partName}（${actionRow.partCode}）  当前库存：${actionRow.stockQty}`"
        type="info"
        show-icon
        style="margin-bottom: 16px"
      />
      <a-form layout="vertical">
        <a-form-item label="数量" required>
          <a-input-number v-model:value="actionForm.quantity" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item label="关联项目">
          <a-select
            v-model:value="actionForm.projectId"
            show-search
            allow-clear
            placeholder="选择项目"
            style="width: 100%"
            :options="projectOptions"
            :filter-option="(input: string, option: any) => option.label.includes(input)"
          />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="actionForm.remark" :rows="2" placeholder="领用事由/归还说明等" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 流水查询抽屉 -->
    <a-drawer
      :open="logVisible"
      :width="720"
      :title="`备件流水 - ${logRow?.partName || ''}`"
      @close="logVisible = false"
    >
      <a-table
        :columns="logColumns"
        :data-source="logDataSource"
        :loading="logLoading"
        row-key="id"
        size="small"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'actionType'">
            <a-tag :color="actionTypeTone[record.actionType]">{{ actionTypeLabel[record.actionType as ActionType] || record.actionType }}</a-tag>
          </template>
          <template v-else-if="column.key === 'quantity'">
            <span class="tnum">{{ record.quantity }}</span>
          </template>
          <template v-else-if="column.key === 'projectName'">
            {{ record.projectName || (record.projectId ? `项目#${record.projectId}` : '—') }}
          </template>
        </template>
        <template #emptyText>
          <EmptyState description="暂无操作记录" size="compact" />
        </template>
      </a-table>
    </a-drawer>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
.danger-link { color: @status-exception; }
.warning { color: @status-exception; font-weight: 600; }
.text-auxiliary { color: @text-tertiary; }
.tnum { font-variant-numeric: tabular-nums; }
</style>
