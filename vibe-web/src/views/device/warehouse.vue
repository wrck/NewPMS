<script setup lang="ts">
/**
 * 仓库档案管理
 * 基于 CrudTable + FormModal 实现 CRUD + 库存预警阈值（safetyStock JSON）配置
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, EnvironmentOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageWarehouses,
  createWarehouse,
  updateWarehouse,
  deleteWarehouse
} from '@/api/device'
import type { Warehouse, WarehouseDTO, WarehouseQueryParams } from '@/types/device'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<Warehouse[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50', '100']
})
const query = reactive<WarehouseQueryParams>({ keyword: '', region: '' })

async function loadData() {
  loading.value = true
  try {
    const res = (await pageWarehouses({
      ...query,
      page: pagination.current,
      size: pagination.pageSize
    })) as unknown as PageResult<Warehouse>
    dataSource.value = res?.records || []
    pagination.total = res?.total || 0
  } catch (e) {
    console.error('[device.warehouse] load failed:', e)
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
  query.region = ''
  handleSearch()
}

function handleTableChange(p: any) {
  pagination.current = p.current || 1
  pagination.pageSize = p.pageSize || 10
  loadData()
}

/* ============ 新增/编辑弹窗 ============ */
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<WarehouseDTO>({
  warehouseCode: '',
  warehouseName: '',
  address: '',
  region: '',
  managerId: undefined,
  safetyStock: ''
})

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    warehouseCode: '',
    warehouseName: '',
    address: '',
    region: '',
    managerId: undefined,
    safetyStock: ''
  })
  formVisible.value = true
}

function openEdit(row: Warehouse) {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    warehouseCode: row.warehouseCode,
    warehouseName: row.warehouseName,
    address: row.address || '',
    region: row.region || '',
    managerId: row.managerId,
    safetyStock: row.safetyStock || ''
  })
  formVisible.value = true
}

async function handleSubmit() {
  if (!formData.warehouseCode || !formData.warehouseName) {
    message.warning('请填写仓库编码和名称')
    return
  }
  // 校验 safetyStock JSON 格式（如果填写）
  if (formData.safetyStock && formData.safetyStock.trim()) {
    try {
      JSON.parse(formData.safetyStock)
    } catch {
      message.error('安全库存配置需为合法 JSON，例如 {"1001":5,"1002":3}')
      return
    }
  }
  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateWarehouse(formData.id, formData)
      message.success('更新成功')
    } else {
      await createWarehouse(formData)
      message.success('创建成功')
    }
    formVisible.value = false
    loadData()
  } catch (e) {
    // 错误已由 request 拦截器统一提示
    console.error('[device.warehouse] submit failed:', e)
  } finally {
    formLoading.value = false
  }
}

function handleDelete(row: Warehouse) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除仓库「${row.warehouseName}」吗？删除后该仓库下的库存台账将无法关联。`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteWarehouse(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        console.error('[device.warehouse] delete failed:', e)
      }
    }
  })
}

const columns = [
  { title: '仓库编码', dataIndex: 'warehouseCode', key: 'warehouseCode', width: 140, fixed: 'left' as const },
  { title: '仓库名称', dataIndex: 'warehouseName', key: 'warehouseName', width: 160, ellipsis: true },
  { title: '区域', dataIndex: 'region', key: 'region', width: 110 },
  { title: '地址', dataIndex: 'address', key: 'address', ellipsis: true },
  { title: '管理员', dataIndex: 'managerName', key: 'managerName', width: 110 },
  { title: '安全库存配置', key: 'safetyStock', width: 200, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 130, fixed: 'right' as const }
]

/** 解析 safetyStock JSON 为可读文本 */
function formatSafetyStock(json?: string): string {
  if (!json) return '—'
  try {
    const obj = JSON.parse(json) as Record<string, number>
    const entries = Object.entries(obj)
    if (!entries.length) return '—'
    return entries.map(([k, v]) => `型号${k}: ${v}`).join('；')
  } catch {
    return json
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="仓库管理" description="设备资产仓库档案与库存预警阈值配置">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新增仓库</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="关键字">
          <a-input
            v-model:value="query.keyword"
            placeholder="仓库名称/编码"
            allow-clear
            style="width: 220px"
            @pressEnter="handleSearch"
          />
        </a-form-item>
        <a-form-item label="区域">
          <a-input
            v-model:value="query.region"
            placeholder="如华北"
            allow-clear
            style="width: 150px"
            @pressEnter="handleSearch"
          >
            <template #prefix><EnvironmentOutlined /></template>
          </a-input>
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
        :scroll="{ x: 1280 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'safetyStock'">
            <a-tooltip :title="formatSafetyStock(record.safetyStock)">
              <span>{{ formatSafetyStock(record.safetyStock) }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'region'">
            <a-tag v-if="record.region" color="blue">{{ record.region }}</a-tag>
            <span v-else class="text-auxiliary">—</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a @click="openEdit(record)"><EditOutlined /> 编辑</a>
              <a-divider type="vertical" />
              <a class="danger-link" @click="handleDelete(record)"><DeleteOutlined /></a>
            </a-space>
          </template>
        </template>
        <template #emptyText>
          <EmptyState description="暂无仓库档案" action-text="新增仓库" @action="openCreate" />
        </template>
      </a-table>
    </div>

    <a-modal
      v-model:open="formVisible"
      :title="isEdit ? '编辑仓库' : '新增仓库'"
      width="640px"
      :confirm-loading="formLoading"
      :mask-closable="false"
      @ok="handleSubmit"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="仓库编码" required>
              <a-input v-model:value="formData.warehouseCode" :disabled="isEdit" placeholder="如 WH-001" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="仓库名称" required>
              <a-input v-model:value="formData.warehouseName" placeholder="如 北京中心仓" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="区域">
              <a-input v-model:value="formData.region" placeholder="如 华北" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="仓库管理员 ID">
              <a-input-number v-model:value="formData.managerId" placeholder="管理员用户 ID" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="仓库地址">
              <a-input v-model:value="formData.address" placeholder="详细地址" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="安全库存配置（JSON）">
              <a-textarea
                v-model:value="formData.safetyStock"
                :rows="3"
                placeholder='按型号 ID 配置预警阈值，如 {"1001":5,"1002":3}'
              />
              <div class="form-hint">JSON 格式：键为设备型号 ID，值为最低库存阈值；库存低于该值时触发预警。</div>
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
.danger-link { color: @status-exception; }
.text-auxiliary { color: @text-tertiary; }
.form-hint {
  margin-top: 4px;
  font-size: 12px;
  color: @text-tertiary;
}
</style>
