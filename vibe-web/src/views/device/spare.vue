<script setup lang="ts">
/**
 * 备件管理
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { pageSpareParts, createSparePart, updateSparePart, deleteSparePart, sparePartAction } from '@/api/device'
import type { SparePart, SparePartQueryParams } from '@/types/device'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<SparePart[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const query = reactive<SparePartQueryParams>({ keyword: '', category: undefined, warehouseId: undefined, status: undefined })

async function loadData() {
  loading.value = true
  try {
    const res = (await pageSpareParts({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<SparePart>
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

// 弹窗
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
}

function openEdit(row: SparePart) {
  isEdit.value = true
  Object.assign(formData, row)
  formVisible.value = true
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
    // ignore
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
      } catch (e) { /* ignore */ }
    }
  })
}

// 领用 / 归还 / 返修 弹窗
const actionVisible = ref(false)
const actionLoading = ref(false)
const actionType = ref<'OUT' | 'RETURN' | 'REPAIR'>('OUT')
const actionRow = ref<SparePart | null>(null)
const actionForm = reactive({ quantity: 1, remark: '' })

function openAction(row: SparePart, type: 'OUT' | 'RETURN' | 'REPAIR') {
  actionRow.value = row
  actionType.value = type
  actionForm.quantity = 1
  actionForm.remark = ''
  actionVisible.value = true
}

async function handleAction() {
  if (!actionRow.value) return
  actionLoading.value = true
  try {
    await sparePartAction(actionRow.value.id, actionType.value, actionForm.quantity, actionForm.remark)
    message.success('操作成功')
    actionVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    actionLoading.value = false
  }
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

const columns = [
  { title: '备件编码', dataIndex: 'partCode', key: 'partCode', width: 140 },
  { title: '备件名称', dataIndex: 'partName', key: 'partName', ellipsis: true },
  { title: '类别', dataIndex: 'category', key: 'category', width: 110 },
  { title: '所属型号', dataIndex: 'modelName', key: 'modelName', width: 140 },
  { title: '库存', key: 'stock', width: 130 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 230, fixed: 'right' }
]

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="备件管理" description="备件库存、领用、归还、返修跟踪">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新备件</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="关键字">
          <a-input v-model:value="query.partName" placeholder="备件编码/名称" allow-clear style="width: 200px" @pressEnter="handleSearch" />
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
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="vibe-card table-card">
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1200 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'category'">
            {{ categoryOptions.find(c => c.value === record.category)?.label || record.category }}
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
            <a-space size="small">
              <a @click="openAction(record, 'OUT')">领用</a>
              <a-divider type="vertical" />
              <a @click="openAction(record, 'RETURN')">归还</a>
              <a-divider type="vertical" />
              <a @click="openEdit(record)"><EditOutlined /></a>
              <a-divider type="vertical" />
              <a class="danger-link" @click="handleDelete(record)"><DeleteOutlined /></a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无备件" action-text="新备件" @action="openCreate" /></template>
      </a-table>
    </div>

    <a-modal v-model:open="formVisible" :title="isEdit ? '编辑备件' : '新设备件'" width="560px" :confirm-loading="formLoading" @ok="handleSubmit">
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
            <a-form-item label="所属型号 ID">
              <a-input-number v-model:value="formData.modelId" style="width: 100%" />
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

    <a-modal v-model:open="actionVisible" :title="{ OUT: '备件领用', RETURN: '备件归还', REPAIR: '返修登记' }[actionType]" :confirm-loading="actionLoading" @ok="handleAction">
      <a-form layout="vertical">
        <a-form-item label="数量" required>
          <a-input-number v-model:value="actionForm.quantity" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="actionForm.remark" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
.danger-link { color: @status-exception; }
.warning { color: @status-exception; font-weight: 600; }
</style>
