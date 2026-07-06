<script setup lang="ts">
/**
 * 表单配置管理（spec 阶段三 - Task A4.1）
 *
 * 功能：
 *   - 分页查询表单配置列表（含 configCode/configName/status 搜索）
 *   - 新增/编辑配置：抽屉式 SchemaDesigner 设计器
 *   - 复制配置
 *   - 导出 JSON Schema（下载）
 *   - 导入 JSON Schema
 *   - 删除配置
 *
 * 视觉风格与 system/user.vue、device/ledger.vue 一致。
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined,
  CopyOutlined,
  DownloadOutlined,
  ImportOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import SchemaDesigner from '@/components/Lowcode/SchemaDesigner.vue'
import SchemaImporter from '@/components/Lowcode/SchemaImporter.vue'
import {
  pageFormConfigs,
  createFormConfig,
  updateFormConfig,
  deleteFormConfig,
  copyFormConfig,
  exportFormConfigJson,
  importFormConfig,
  downloadBlob
} from '@/api/lowcode'
import type {
  LowcodeFormConfigVO,
  LowcodeFormConfigDTO,
  LowcodeConfigQueryParams
} from '@/types/lowcode'
import type { PageResult } from '@/types/api'

/** 编辑用的 DTO 类型（在 DTO 基础上扩展可选 id，用于编辑态保留主键） */
type FormConfigEditDTO = LowcodeFormConfigDTO & { id?: number }

const loading = ref(false)
const dataSource = ref<LowcodeFormConfigVO[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`
})
const query = reactive<LowcodeConfigQueryParams>({
  keyword: '',
  status: undefined
})

const columns = [
  { title: '配置编码', dataIndex: 'configCode', key: 'configCode', width: 180 },
  { title: '配置名称', dataIndex: 'configName', key: 'configName', width: 200 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '版本', dataIndex: 'version', key: 'version', width: 80 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 180 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' as const }
]

async function loadData() {
  loading.value = true
  try {
    const res = (await pageFormConfigs({
      ...query,
      page: pagination.current,
      size: pagination.pageSize
    })) as unknown as PageResult<LowcodeFormConfigVO>
    dataSource.value = res?.records || []
    pagination.total = res?.total || 0
  } catch (e) {
    console.error('[form-config] load failed:', e)
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
  handleSearch()
}

/* ============ 设计器抽屉 ============ */
const designerVisible = ref(false)
const designerLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<FormConfigEditDTO>({
  configCode: '',
  configName: '',
  schemaJson: '',
  status: 1,
  description: ''
})

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    configCode: '',
    configName: '',
    schemaJson: '',
    status: 1,
    description: ''
  } as FormConfigEditDTO)
  designerVisible.value = true
}

function openEdit(row: LowcodeFormConfigVO) {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    configCode: row.configCode,
    configName: row.configName,
    schemaJson: row.schemaJson || '',
    status: row.status,
    description: row.description
  } as FormConfigEditDTO)
  designerVisible.value = true
}

async function handleSubmit() {
  if (!formData.configCode || !formData.configName) {
    message.warning('请填写配置编码与名称')
    return
  }
  if (!formData.schemaJson) {
    message.warning('请使用设计器设计表单 Schema')
    return
  }
  designerLoading.value = true
  try {
    const { id: _id, ...dto } = formData
    void _id
    if (isEdit.value && formData.id) {
      await updateFormConfig(formData.id, dto)
      message.success('更新成功')
    } else {
      await createFormConfig(dto)
      message.success('创建成功')
    }
    designerVisible.value = false
    loadData()
  } catch (e) {
    console.error('[form-config] submit failed:', e)
  } finally {
    designerLoading.value = false
  }
}

/* ============ 复制 ============ */
async function handleCopy(row: LowcodeFormConfigVO) {
  try {
    await copyFormConfig(row.id)
    message.success('复制成功')
    loadData()
  } catch (e) {
    console.error('[form-config] copy failed:', e)
  }
}

/* ============ 删除 ============ */
function handleDelete(row: LowcodeFormConfigVO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除配置「${row.configName}」吗？`,
    okType: 'danger',
    onOk: async () => {
      try {
        await deleteFormConfig(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        console.error('[form-config] delete failed:', e)
      }
    }
  })
}

/* ============ 导出 JSON ============ */
async function handleExport(row: LowcodeFormConfigVO) {
  try {
    const blob = await exportFormConfigJson(row.id)
    downloadBlob(blob, `${row.configCode}.json`)
    message.success('导出成功')
  } catch (e) {
    console.error('[form-config] export failed:', e)
  }
}

/* ============ 导入 JSON ============ */
const importerVisible = ref(false)
function openImporter() {
  importerVisible.value = true
}

async function handleImport(payload: { schemaJson: string; templateId?: number }) {
  try {
    const newConfig: LowcodeFormConfigDTO = {
      configCode: `import_${Date.now()}`,
      configName: `导入配置_${new Date().toLocaleString()}`,
      schemaJson: payload.schemaJson,
      status: 1,
      description: '通过 JSON 导入'
    }
    await importFormConfig(newConfig)
    message.success('导入成功')
    loadData()
  } catch (e) {
    console.error('[form-config] import failed:', e)
  }
}

/* ============ 翻页 ============ */
function handleTableChange(pag: { current?: number; pageSize?: number }) {
  if (pag) {
    pagination.current = pag.current || 1
    pagination.pageSize = pag.pageSize || pagination.pageSize
  }
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="表单配置" description="低代码表单 Schema 设计与管理">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button @click="openImporter"><template #icon><ImportOutlined /></template>导入 JSON</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新增配置</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="关键字">
          <a-input v-model:value="query.keyword" placeholder="编码或名称" allow-clear style="width: 240px" @press-enter="handleSearch" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 130px" :options="[{ value: 1, label: '启用' }, { value: 0, label: '禁用' }]" />
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
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1200 }" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <StatusTag :tone="record.status === 1 ? 'success' : 'default'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small" wrap>
              <a @click="openEdit(record as LowcodeFormConfigVO)"><EditOutlined /> 编辑</a>
              <a-divider type="vertical" />
              <a @click="handleCopy(record as LowcodeFormConfigVO)"><CopyOutlined /> 复制</a>
              <a-divider type="vertical" />
              <a @click="handleExport(record as LowcodeFormConfigVO)"><DownloadOutlined /> 导出</a>
              <a-divider type="vertical" />
              <a class="danger-link" @click="handleDelete(record as LowcodeFormConfigVO)"><DeleteOutlined /> 删除</a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无配置" action-text="新增配置" @action="openCreate" /></template>
      </a-table>
    </div>

    <!-- 设计器抽屉 -->
    <a-drawer
      v-model:open="designerVisible"
      :title="isEdit ? '编辑表单配置' : '新增表单配置'"
      placement="right"
      :width="1100"
      :destroy-on-close="false"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="配置编码" required>
              <a-input v-model:value="formData.configCode" placeholder="如 customer_form" :disabled="isEdit" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="配置名称" required>
              <a-input v-model:value="formData.configName" placeholder="如 客户表单" />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="状态">
              <a-select v-model:value="formData.status" :options="[{ value: 1, label: '启用' }, { value: 0, label: '禁用' }]" />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="描述">
              <a-input v-model:value="formData.description" placeholder="可选" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>

      <div class="designer-wrapper">
        <SchemaDesigner v-model="formData.schemaJson" mode="form" />
      </div>

      <template #footer>
        <a-space>
          <a-button @click="designerVisible = false">取消</a-button>
          <a-button type="primary" :loading="designerLoading" @click="handleSubmit">保存</a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 导入 JSON 弹窗 -->
    <SchemaImporter
      v-model:visible="importerVisible"
      template-type="FORM"
      title="导入表单 Schema"
      @import="handleImport"
    />
  </PageContainer>
</template>

<style lang="less" scoped>
.designer-wrapper {
  height: calc(100vh - 280px);
  min-height: 500px;
  border: 1px solid @border-split;
  border-radius: 6px;
  overflow: hidden;
}
</style>
