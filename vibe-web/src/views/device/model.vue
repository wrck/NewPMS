<script setup lang="ts">
/**
 * 设备型号库
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageDeviceModels,
  createDeviceModel,
  updateDeviceModel,
  deleteDeviceModel
} from '@/api/device'
import type { DeviceModel, DeviceModelDTO, DeviceModelQueryParams, DeviceCategory, ProductLine } from '@/types/device'

const loading = ref(false)
const dataSource = ref<DeviceModel[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`
})
const query = reactive<DeviceModelQueryParams>({ keyword: '', productLine: undefined, category: undefined, vendor: '' })

async function loadData() {
  loading.value = true
  try {
    const res: any = await pageDeviceModels({ ...query, page: pagination.current, size: pagination.pageSize })
    dataSource.value = res?.records || []
    pagination.total = res?.total || 0
  } catch (e) {
    console.error('[device.model] load failed:', e)
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
  query.productLine = undefined
  query.category = undefined
  query.vendor = ''
  handleSearch()
}

// 弹窗
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<DeviceModelDTO>({
  modelCode: '',
  modelName: '',
  productLine: 'ROUTER',
  vendor: '',
  category: 'ROUTER',
  description: ''
})

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    modelCode: '',
    modelName: '',
    productLine: 'ROUTER',
    vendor: '',
    category: 'ROUTER',
    configTemplate: '',
    manualUrl: '',
    description: ''
  })
  formVisible.value = true
}

function openEdit(row: DeviceModel) {
  isEdit.value = true
  Object.assign(formData, row)
  formVisible.value = true
}

async function handleSubmit() {
  if (!formData.modelCode || !formData.modelName) {
    message.warning('请填写型号编码和名称')
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateDeviceModel(formData.id, formData)
      message.success('更新成功')
    } else {
      await createDeviceModel(formData)
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

function handleDelete(row: DeviceModel) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除型号「${row.modelName}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteDeviceModel(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        // ignore
      }
    }
  })
}

const productLineOptions = [
  { value: 'ROUTER', label: '路由' },
  { value: 'SWITCH', label: '交换' },
  { value: 'WIRELESS', label: '无线' },
  { value: 'SECURITY', label: '安全' },
  { value: 'DC', label: '数据中心' },
  { value: 'OTHER', label: '其他' }
]
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
  { title: '型号编码', dataIndex: 'modelCode', key: 'modelCode', width: 140 },
  { title: '型号名称', dataIndex: 'modelName', key: 'modelName', ellipsis: true },
  { title: '产品线', dataIndex: 'productLine', key: 'productLine', width: 100 },
  { title: '类别', dataIndex: 'category', key: 'category', width: 110 },
  { title: '厂商', dataIndex: 'vendor', key: 'vendor', width: 120 },
  { title: '状态', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="设备型号库" description="设备型号基础数据维护">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新型号</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="关键字">
          <a-input v-model:value="query.keyword" placeholder="型号编码/名称" allow-clear style="width: 200px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="产品线">
          <a-select v-model:value="query.productLine" placeholder="全部" allow-clear style="width: 130px" :options="productLineOptions" />
        </a-form-item>
        <a-form-item label="类别">
          <a-select v-model:value="query.category" placeholder="全部" allow-clear style="width: 140px" :options="categoryOptions" />
        </a-form-item>
        <a-form-item label="厂商">
          <a-input v-model:value="query.vendor" placeholder="厂商" allow-clear style="width: 140px" @pressEnter="handleSearch" />
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
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1100 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'productLine'">
            {{ productLineOptions.find(p => p.value === record.productLine)?.label || record.productLine }}
          </template>
          <template v-else-if="column.key === 'category'">
            {{ categoryOptions.find(c => c.value === record.category)?.label || record.category }}
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="record.status === 'DISABLED' ? 'archived' : 'success'">
              {{ record.status === 'DISABLED' ? '停用' : '启用' }}
            </StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a @click="openEdit(record)"><EditOutlined /> 编辑</a>
              <a-divider type="vertical" />
              <a class="danger-link" @click="handleDelete(record)"><DeleteOutlined /></a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无型号数据" action-text="新型号" @action="openCreate" /></template>
      </a-table>
    </div>

    <a-modal v-model:open="formVisible" :title="isEdit ? '编辑型号' : '新型号'" width="640px" :confirm-loading="formLoading" @ok="handleSubmit">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="型号编码" required>
              <a-input v-model:value="formData.modelCode" :disabled="isEdit" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="型号名称" required>
              <a-input v-model:value="formData.modelName" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="产品线">
              <a-select v-model:value="formData.productLine" :options="productLineOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="类别">
              <a-select v-model:value="formData.category" :options="categoryOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="厂商">
              <a-input v-model:value="formData.vendor" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="安装手册链接">
              <a-input v-model:value="formData.manualUrl" placeholder="URL" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="描述">
              <a-textarea v-model:value="formData.description" :rows="2" />
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
</style>
