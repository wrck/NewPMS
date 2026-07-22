<script setup lang="ts">
/**
 * 设备台账（设备实例管理）+ Excel 导入
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal, Upload } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, UploadOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import HelpHint from '@/components/HelpHint.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageDeviceInstances,
  createDeviceInstance,
  updateDeviceInstance,
  deleteDeviceInstance,
  importDeviceInstances,
  downloadImportTemplate,
  pageDeviceModels,
  listWarehouses
} from '@/api/device'
import { pageProjects } from '@/api/project'
import type { DeviceInstance, DeviceInstanceDTO, DeviceInstanceQueryParams } from '@/types/device'
import { DeviceStatus, DeviceStatusTone, DeviceStatusLabel } from '@/types/enum'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<DeviceInstance[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`
})
const query = reactive<DeviceInstanceQueryParams>({ keyword: '', status: undefined, modelId: undefined, projectId: undefined })

async function loadData() {
  loading.value = true
  try {
    const res = (await pageDeviceInstances({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<DeviceInstance>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[device.ledger] load failed:', e)
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
  query.status = undefined
  query.modelId = undefined
  query.projectId = undefined
  handleSearch()
}

// 弹窗
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<DeviceInstanceDTO>({
  serialNumber: '',
  macAddress: '',
  modelId: undefined,
  firmwareVersion: '',
  projectId: undefined,
  warehouseId: undefined,
  location: '',
  status: DeviceStatus.IN_FACTORY,
  remark: ''
})

// 设备表单校验规则（异常处理三层闭环 SubTask 8.4 补充）
const deviceFormRules = {
  serialNumber: [
    { required: true, message: '请输入设备序列号 SN', trigger: 'blur' },
    { max: 64, message: 'SN 长度不能超过 64', trigger: 'blur' }
  ],
  macAddress: [
    { pattern: /^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$|^$/, message: 'MAC 地址格式不正确（如 00:1A:2B:3C:4D:5E）', trigger: 'blur' }
  ],
  modelId: [
    { required: true, message: '请选择设备型号', trigger: 'change' }
  ]
}
const deviceFormRef = ref()

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    serialNumber: '',
    macAddress: '',
    modelId: undefined,
    firmwareVersion: '',
    projectId: undefined,
    warehouseId: undefined,
    location: '',
    status: DeviceStatus.IN_FACTORY,
    remark: ''
  })
  formVisible.value = true
  loadModelOptions()
  loadProjectOptions()
  loadWarehouseOptions()
}

function openEdit(row: DeviceInstance) {
  isEdit.value = true
  Object.assign(formData, row)
  formVisible.value = true
  loadModelOptions()
  loadProjectOptions()
  loadWarehouseOptions()
}

async function handleSubmit() {
  // 异常处理三层闭环：先校验表单，再调用后端
  try {
    await deviceFormRef.value?.validate()
  } catch {
    return
  }
  if (!formData.serialNumber || !formData.modelId) {
    message.warning('请填写序列号和型号')
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await updateDeviceInstance(formData.id, formData)
      message.success('更新成功')
    } else {
      await createDeviceInstance(formData)
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

function handleDelete(row: DeviceInstance) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除设备「${row.serialNumber}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteDeviceInstance(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) { /* ignore */ }
    }
  })
}

// 导入
const importVisible = ref(false)
const importLoading = ref(false)
const importFile = ref<File | null>(null)

function openImport() {
  importFile.value = null
  importVisible.value = true
}

function handleFileChange(file: File) {
  importFile.value = file
}

async function handleImport() {
  if (!importFile.value) {
    message.warning('请先选择文件')
    return
  }
  importLoading.value = true
  try {
    const res: any = await importDeviceInstances(importFile.value)
    message.success(`导入完成：成功 ${res?.successCount || 0} 条，失败 ${res?.failCount || 0} 条`)
    importVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    importLoading.value = false
  }
}

async function handleDownloadTemplate() {
  try {
    const blob: any = await downloadImportTemplate()
    const url = window.URL.createObjectURL(new Blob([blob as Blob]))
    const a = document.createElement('a')
    a.href = url
    a.download = '设备导入模板.xlsx'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    message.error('下载模板失败')
  }
}

const statusOptions = Object.values(DeviceStatus).map((s) => ({ value: s, label: DeviceStatusLabel[s] }))

// 实体引用字段下拉选项（型号/项目/仓库）
const modelOptions = ref<Array<{ value: string | number; label: string }>>([])
const projectOptions = ref<Array<{ value: string | number; label: string }>>([])
const warehouseOptions = ref<Array<{ value: string | number; label: string }>>([])

async function loadModelOptions() {
  try {
    const res = await pageDeviceModels({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    modelOptions.value = list.map((m: any) => ({ value: m.id, label: m.modelName }))
  } catch (e) {
    console.warn('[device.ledger] load models failed:', e)
  }
}

async function loadProjectOptions() {
  try {
    const res = await pageProjects({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    projectOptions.value = list.map((p: any) => ({ value: p.id, label: p.projectName }))
  } catch (e) {
    console.warn('[device.ledger] load projects failed:', e)
  }
}

async function loadWarehouseOptions() {
  try {
    const list = (await listWarehouses()) || []
    warehouseOptions.value = list.map((w: any) => ({ value: w.id, label: w.warehouseName }))
  } catch (e) {
    console.warn('[device.ledger] load warehouses failed:', e)
  }
}

const columns = [
  { title: 'SN', dataIndex: 'serialNumber', key: 'serialNumber', width: 160, fixed: 'left' },
  { title: 'MAC', dataIndex: 'macAddress', key: 'macAddress', width: 150 },
  { title: '型号', dataIndex: 'modelName', key: 'modelName', width: 140, ellipsis: true },
  { title: '产品线', dataIndex: 'productLine', key: 'productLine', width: 100 },
  { title: '固件版本', dataIndex: 'firmwareVersion', key: 'firmwareVersion', width: 120 },
  { title: '所属项目', dataIndex: 'projectName', key: 'projectName', width: 150, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 130, fixed: 'right' }
]

onMounted(() => {
  loadData()
  loadModelOptions()
  loadProjectOptions()
})
</script>

<template>
  <PageContainer title="设备台账" description="设备实例全量管理，支持 Excel 批量导入">
    <template #title-suffix>
      <HelpHint
        title="设备台账"
        content="设备实例全量管理：\n1. 「新增设备」单条录入（必填：SN、型号 ID）；\n2. 「批量导入」通过 Excel 模板一次性导入多台设备，可先「下载导入模板」填写；\n3. 支持 SN / 状态 / 型号 / 项目多维筛选；\n4. 设备状态遵循状态机：在厂 → 出库 → 在途 → 安装 → 在网 → 故障 → 退运。"
      />
    </template>
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button @click="handleDownloadTemplate"><template #icon><DownloadOutlined /></template>下载导入模板</a-button>
      <a-button @click="openImport"><template #icon><UploadOutlined /></template>批量导入</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新增设备</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="SN/关键字">
          <a-input v-model:value="query.serialNumber" placeholder="序列号" allow-clear style="width: 200px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 130px" :options="statusOptions" />
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
        <a-form-item label="项目">
          <a-select
            v-model:value="query.projectId"
            placeholder="全部"
            allow-clear
            show-search
            style="width: 160px"
            :options="projectOptions"
            :filter-option="(input: string, option: any) => option.label.includes(input)"
          />
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
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1300 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <StatusTag :tone="DeviceStatusTone[record.status as DeviceStatus]">
              {{ DeviceStatusLabel[record.status as DeviceStatus] }}
            </StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a @click="openEdit(record)"><EditOutlined /></a>
              <a-divider type="vertical" />
              <a class="danger-link" @click="handleDelete(record)"><DeleteOutlined /></a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无设备" action-text="新增设备" @action="openCreate" /></template>
      </a-table>
    </div>

    <a-modal v-model:open="formVisible" :title="isEdit ? '编辑设备' : '新增设备'" width="600px" :confirm-loading="formLoading" @ok="handleSubmit">
      <a-form ref="deviceFormRef" layout="vertical" :model="formData" :rules="deviceFormRules">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="序列号 SN" name="serialNumber" required>
              <a-input v-model:value="formData.serialNumber" :disabled="isEdit" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="MAC 地址" name="macAddress">
              <a-input v-model:value="formData.macAddress" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="型号" name="modelId" required>
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
            <a-form-item label="固件版本">
              <a-input v-model:value="formData.firmwareVersion" />
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
          <a-col :span="12">
            <a-form-item label="仓库">
              <a-select
                v-model:value="formData.warehouseId"
                show-search
                allow-clear
                placeholder="选择仓库"
                style="width: 100%"
                :options="warehouseOptions"
                :filter-option="(input: string, option: any) => option.label.includes(input)"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态">
              <a-select v-model:value="formData.status" :options="statusOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="位置">
              <a-input v-model:value="formData.location" />
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

    <a-modal v-model:open="importVisible" title="批量导入设备" :confirm-loading="importLoading" @ok="handleImport">
      <a-alert message="请使用 Excel 模板，按列填写设备信息后上传" type="info" show-icon style="margin-bottom: 16px" />
      <a-upload-dragger
        :before-upload="(file: File) => { handleFileChange(file); return false }"
        :max-count="1"
        accept=".xlsx,.xls"
      >
        <p class="ant-upload-drag-icon"><UploadOutlined /></p>
        <p class="ant-upload-text">点击或拖拽 Excel 文件到此处</p>
        <p class="ant-upload-hint">支持 .xlsx / .xls 格式</p>
      </a-upload-dragger>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
.danger-link { color: @status-exception; }
</style>
