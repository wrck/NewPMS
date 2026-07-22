<script setup lang="ts">
/**
 * 出入库管理（流水 + 新增出入库记录）
 */
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { pageInventoryTransactions, createInventoryTransaction, listWarehouses, pageDeviceModels } from '@/api/device'
import { pageProjects } from '@/api/project'
import type { InventoryTransaction, InventoryTransactionDTO, InventoryQueryParams, Warehouse } from '@/types/device'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<InventoryTransaction[]>([])
const warehouses = ref<Warehouse[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`
})
const query = reactive<InventoryQueryParams & { type?: string }>({ warehouseId: undefined, modelId: undefined, type: undefined })

async function loadData() {
  loading.value = true
  try {
    const res = (await pageInventoryTransactions({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<InventoryTransaction>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[device.inout] load failed:', e)
  } finally {
    loading.value = false
  }
}

async function loadWarehouses() {
  try {
    warehouses.value = (await listWarehouses()) || []
  } catch (e) {
    console.warn('[warehouse] load failed:', e)
  }
}

// 实体引用字段下拉选项（型号/项目）
const modelOptions = ref<Array<{ value: string | number; label: string }>>([])
const projectOptions = ref<Array<{ value: string | number; label: string }>>([])

async function loadModelOptions() {
  try {
    const res = await pageDeviceModels({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    modelOptions.value = list.map((m: any) => ({ value: m.id, label: m.modelName }))
  } catch (e) {
    console.warn('[device.inout] load models failed:', e)
  }
}

async function loadProjectOptions() {
  try {
    const res = await pageProjects({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    projectOptions.value = list.map((p: any) => ({ value: p.id, label: p.projectName }))
  } catch (e) {
    console.warn('[device.inout] load projects failed:', e)
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

const typeMap: Record<string, { tone: any; label: string }> = {
  IN: { tone: 'success', label: '入库' },
  OUT: { tone: 'warning', label: '出库' },
  RETURN: { tone: 'processing', label: '退库' },
  TRANSFER: { tone: 'agent', label: '调拨' }
}

const typeOptions = Object.entries(typeMap).map(([value, opt]) => ({ value, label: opt.label }))

// 新增弹窗
const formVisible = ref(false)
const formLoading = ref(false)
const formData = reactive<InventoryTransactionDTO>({
  type: 'IN',
  warehouseId: 0,
  toWarehouseId: undefined,
  modelId: undefined,
  quantity: 1,
  projectId: undefined,
  remark: ''
})

function openCreate() {
  Object.assign(formData, {
    type: 'IN',
    warehouseId: 0,
    toWarehouseId: undefined,
    modelId: undefined,
    quantity: 1,
    projectId: undefined,
    remark: ''
  })
  formVisible.value = true
  loadModelOptions()
  loadProjectOptions()
}

async function handleSubmit() {
  if (!formData.warehouseId || !formData.modelId || !formData.quantity) {
    message.warning('请完整填写仓库 / 型号 / 数量')
    return
  }
  if (formData.type === 'TRANSFER' && !formData.toWarehouseId) {
    message.warning('调拨需填写目标仓库')
    return
  }
  formLoading.value = true
  try {
    await createInventoryTransaction(formData)
    message.success('已提交')
    formVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    formLoading.value = false
  }
}

const columns = [
  { title: '流水号', dataIndex: 'transactionNo', key: 'transactionNo', width: 180 },
  { title: '类型', key: 'type', width: 90 },
  { title: '型号', dataIndex: 'modelName', key: 'modelName', width: 140 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 130 },
  { title: '目标仓库', dataIndex: 'toWarehouseName', key: 'toWarehouseName', width: 130 },
  { title: '所属项目', dataIndex: 'projectName', key: 'projectName', width: 150, ellipsis: true },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: 110 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 }
]

onMounted(() => {
  loadData()
  loadWarehouses()
  loadModelOptions()
})
</script>

<template>
  <PageContainer title="出入库管理" description="设备出入库流水登记与查询">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新增单据</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="仓库">
          <a-select v-model:value="query.warehouseId" placeholder="全部" allow-clear style="width: 160px">
            <a-select-option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.warehouseName }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="型号">
          <a-select
            v-model:value="query.modelId"
            placeholder="全部"
            allow-clear
            show-search
            style="width: 160px"
            :options="modelOptions"
            :filter-option="(input: string, option: any) => option.label.includes(input)"
          />
        </a-form-item>
        <a-form-item label="类型">
          <a-select v-model:value="query.type" placeholder="全部" allow-clear style="width: 130px" :options="typeOptions" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="vibe-card table-card">
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1400 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <StatusTag :tone="typeMap[record.type]?.tone">{{ typeMap[record.type]?.label || record.type }}</StatusTag>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无出入库流水" action-text="新增单据" @action="openCreate" /></template>
      </a-table>
    </div>

    <a-modal v-model:open="formVisible" title="新增出入库单据" width="560px" :confirm-loading="formLoading" @ok="handleSubmit">
      <a-form layout="vertical">
        <a-form-item label="类型">
          <a-radio-group v-model:value="formData.type">
            <a-radio v-for="opt in typeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="仓库" required>
              <a-select v-model:value="formData.warehouseId" placeholder="选择仓库">
                <a-select-option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.warehouseName }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col v-if="formData.type === 'TRANSFER'" :span="12">
            <a-form-item label="目标仓库" required>
              <a-select v-model:value="formData.toWarehouseId" placeholder="目标仓库">
                <a-select-option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.warehouseName }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="型号" required>
              <a-select
                v-model:value="formData.modelId"
                show-search
                placeholder="选择型号"
                style="width: 100%"
                :options="modelOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="数量" required>
              <a-input-number v-model:value="formData.quantity" :min="1" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="所属项目">
              <a-select
                v-model:value="formData.projectId"
                show-search
                allow-clear
                placeholder="选择项目"
                style="width: 100%"
                :options="projectOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
              />
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
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
</style>
