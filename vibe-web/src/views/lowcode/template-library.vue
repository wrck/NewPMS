<script setup lang="ts">
/**
 * 模板库管理（spec 阶段三 - Task A4.5）
 *
 * 功能：
 *   - 分页查询模板（按 templateType / templateName / status 搜索）
 *   - 新增/编辑模板（直接编辑 schemaJson 文本，便于复用）
 *   - 复制模板
 *   - 导出 JSON / 导入 JSON
 *   - 实例化为配置（弹出选择类型 + 名称后调用对应 instantiate 接口）
 *
 * 与四个配置页（form/list/tab/relation）的区别：
 *   - 模板库是元数据；配置是基于模板实例化的具体配置。
 *   - 实例化按钮根据 templateType 调用对应配置类型的 instantiate 接口。
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
  ImportOutlined,
  ThunderboltOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import SchemaImporter from '@/components/Lowcode/SchemaImporter.vue'
import {
  pageTemplates,
  createTemplate,
  updateTemplate,
  deleteTemplate,
  copyTemplate,
  exportTemplateJson,
  importTemplate,
  instantiateFormFromTemplate,
  instantiateListFromTemplate,
  instantiateTabFromTemplate,
  instantiateRelationFromTemplate,
  downloadBlob
} from '@/api/lowcode'
import type {
  LowcodeTemplateVO,
  LowcodeTemplateDTO,
  LowcodeTemplateQueryParams,
  LowcodeTemplateType,
  LowcodeInstantiateDTO
} from '@/types/lowcode'
import type { PageResult } from '@/types/api'

/** 编辑用的 DTO 类型（在 DTO 基础上扩展可选 id，用于编辑态保留主键） */
type TemplateEditDTO = LowcodeTemplateDTO & { id?: number }

const loading = ref(false)
const dataSource = ref<LowcodeTemplateVO[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`
})
const query = reactive<LowcodeTemplateQueryParams>({
  keyword: '',
  templateType: undefined,
  status: undefined
})

const columns = [
  { title: '模板编码', dataIndex: 'templateCode', key: 'templateCode', width: 180 },
  { title: '模板名称', dataIndex: 'templateName', key: 'templateName', width: 200 },
  { title: '类型', dataIndex: 'templateType', key: 'templateType', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '使用次数', dataIndex: 'usageCount', key: 'usageCount', width: 100 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 180 },
  { title: '操作', key: 'action', width: 320, fixed: 'right' as const }
]

const templateTypeOptions = [
  { value: 'FORM', label: '表单' },
  { value: 'LIST', label: '列表' },
  { value: 'TAB', label: '标签页' },
  { value: 'RELATION', label: '关联页' }
]

const templateTypeLabel: Record<LowcodeTemplateType, string> = {
  FORM: '表单',
  LIST: '列表',
  TAB: '标签页',
  RELATION: '关联页'
}

async function loadData() {
  loading.value = true
  try {
    const res = (await pageTemplates({
      ...query,
      page: pagination.current,
      size: pagination.pageSize
    })) as unknown as PageResult<LowcodeTemplateVO>
    dataSource.value = res?.records || []
    pagination.total = res?.total || 0
  } catch (e) {
    console.error('[template-library] load failed:', e)
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
  query.templateType = undefined
  query.status = undefined
  handleSearch()
}

/* ============ 新增/编辑 ============ */
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<TemplateEditDTO>({
  templateCode: '',
  templateName: '',
  templateType: 'FORM',
  schemaJson: '',
  status: 1,
  description: ''
})

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    templateCode: '',
    templateName: '',
    templateType: 'FORM',
    schemaJson: '',
    status: 1,
    description: ''
  } as TemplateEditDTO)
  formVisible.value = true
}

function openEdit(row: LowcodeTemplateVO) {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    templateCode: row.templateCode,
    templateName: row.templateName,
    templateType: row.templateType,
    schemaJson: row.schemaJson || '',
    status: row.status,
    description: row.description
  } as TemplateEditDTO)
  formVisible.value = true
}

async function handleSubmit() {
  if (!formData.templateCode || !formData.templateName) {
    message.warning('请填写模板编码与名称')
    return
  }
  if (!formData.schemaJson) {
    message.warning('请填写 Schema JSON')
    return
  }
  formLoading.value = true
  try {
    const { id: _id, ...dto } = formData
    void _id
    if (isEdit.value && formData.id) {
      await updateTemplate(formData.id, dto)
      message.success('更新成功')
    } else {
      await createTemplate(dto)
      message.success('创建成功')
    }
    formVisible.value = false
    loadData()
  } catch (e) {
    console.error('[template-library] submit failed:', e)
  } finally {
    formLoading.value = false
  }
}

/* ============ 复制 ============ */
async function handleCopy(row: LowcodeTemplateVO) {
  try {
    await copyTemplate(row.id)
    message.success('复制成功')
    loadData()
  } catch (e) {
    console.error('[template-library] copy failed:', e)
  }
}

/* ============ 删除 ============ */
function handleDelete(row: LowcodeTemplateVO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除模板「${row.templateName}」吗？`,
    okType: 'danger',
    onOk: async () => {
      try {
        await deleteTemplate(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        console.error('[template-library] delete failed:', e)
      }
    }
  })
}

/* ============ 导出 JSON ============ */
async function handleExport(row: LowcodeTemplateVO) {
  try {
    const blob = await exportTemplateJson(row.id)
    downloadBlob(blob, `${row.templateCode}.json`)
    message.success('导出成功')
  } catch (e) {
    console.error('[template-library] export failed:', e)
  }
}

/* ============ 导入 JSON ============ */
const importerVisible = ref(false)
function openImporter() {
  importerVisible.value = true
}

async function handleImport(payload: { schemaJson: string; templateId?: number }) {
  try {
    const newTpl: LowcodeTemplateDTO = {
      templateCode: `import_${Date.now()}`,
      templateName: `导入模板_${new Date().toLocaleString()}`,
      templateType: 'FORM',
      schemaJson: payload.schemaJson,
      status: 1,
      description: '通过 JSON 导入'
    }
    await importTemplate(newTpl)
    message.success('导入成功')
    loadData()
  } catch (e) {
    console.error('[template-library] import failed:', e)
  }
}

/* ============ 实例化 ============ */
const instantiateVisible = ref(false)
const instantiateLoading = ref(false)
const instantiateTarget = ref<LowcodeTemplateVO | null>(null)
const instantiateDto = reactive<LowcodeInstantiateDTO>({
  configCode: '',
  configName: ''
})

function openInstantiate(row: LowcodeTemplateVO) {
  instantiateTarget.value = row
  instantiateDto.configCode = `${row.templateCode}_inst_${Date.now()}`
  instantiateDto.configName = `基于模板：${row.templateName}`
  instantiateVisible.value = true
}

async function handleInstantiate() {
  if (!instantiateTarget.value) return
  if (!instantiateDto.configCode || !instantiateDto.configName) {
    message.warning('请填写配置编码与名称')
    return
  }
  instantiateLoading.value = true
  try {
    const tpl = instantiateTarget.value
    if (tpl.templateType === 'FORM') {
      await instantiateFormFromTemplate(tpl.id, instantiateDto)
    } else if (tpl.templateType === 'LIST') {
      await instantiateListFromTemplate(tpl.id, instantiateDto)
    } else if (tpl.templateType === 'TAB') {
      await instantiateTabFromTemplate(tpl.id, instantiateDto)
    } else if (tpl.templateType === 'RELATION') {
      await instantiateRelationFromTemplate(tpl.id, instantiateDto)
    }
    message.success('实例化成功')
    instantiateVisible.value = false
    loadData()
  } catch (e) {
    console.error('[template-library] instantiate failed:', e)
  } finally {
    instantiateLoading.value = false
  }
}

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
  <PageContainer title="模板库" description="低代码模板管理与实例化">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button @click="openImporter"><template #icon><ImportOutlined /></template>导入 JSON</a-button>
      <a-button type="primary" @click="openCreate"><template #icon><PlusOutlined /></template>新增模板</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="关键字">
          <a-input v-model:value="query.keyword" placeholder="编码或名称" allow-clear style="width: 240px" @press-enter="handleSearch" />
        </a-form-item>
        <a-form-item label="类型">
          <a-select v-model:value="query.templateType" placeholder="全部" allow-clear style="width: 130px" :options="templateTypeOptions" />
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
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1400 }" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'templateType'">
            <a-tag color="blue">{{ templateTypeLabel[record.templateType as LowcodeTemplateType] || record.templateType }}</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="record.status === 1 ? 'success' : 'default'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small" wrap>
              <a @click="openEdit(record as LowcodeTemplateVO)"><EditOutlined /> 编辑</a>
              <a-divider type="vertical" />
              <a @click="openInstantiate(record as LowcodeTemplateVO)"><ThunderboltOutlined /> 实例化</a>
              <a-divider type="vertical" />
              <a @click="handleCopy(record as LowcodeTemplateVO)"><CopyOutlined /> 复制</a>
              <a-divider type="vertical" />
              <a @click="handleExport(record as LowcodeTemplateVO)"><DownloadOutlined /> 导出</a>
              <a-divider type="vertical" />
              <a class="danger-link" @click="handleDelete(record as LowcodeTemplateVO)"><DeleteOutlined /> 删除</a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无模板" action-text="新增模板" @action="openCreate" /></template>
      </a-table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <a-modal v-model:open="formVisible" :title="isEdit ? '编辑模板' : '新增模板'" width="780px" :confirm-loading="formLoading" @ok="handleSubmit">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="模板编码" required>
              <a-input v-model:value="formData.templateCode" placeholder="如 customer_form_tpl" :disabled="isEdit" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="模板名称" required>
              <a-input v-model:value="formData.templateName" placeholder="如 客户表单模板" />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="类型">
              <a-select v-model:value="formData.templateType" :options="templateTypeOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="状态">
              <a-select v-model:value="formData.status" :options="[{ value: 1, label: '启用' }, { value: 0, label: '禁用' }]" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述">
          <a-input v-model:value="formData.description" placeholder="可选" />
        </a-form-item>
        <a-form-item label="Schema JSON" required>
          <a-textarea
            v-model:value="formData.schemaJson"
            :rows="14"
            placeholder='{"$schema":"http://json-schema.org/draft-07/schema#","type":"object","properties":{}}'
            spellcheck="false"
            :style="{ fontFamily: 'monospace', fontSize: '12px' }"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 实例化弹窗 -->
    <a-modal v-model:open="instantiateVisible" title="实例化为配置" width="520px" :confirm-loading="instantiateLoading" @ok="handleInstantiate">
      <a-alert
        v-if="instantiateTarget"
        type="info"
        show-icon
        :message="`将从模板「${instantiateTarget.templateName}」（${templateTypeLabel[instantiateTarget.templateType]}）生成一份新配置`"
        style="margin-bottom: 12px"
      />
      <a-form layout="vertical">
        <a-form-item label="配置编码" required>
          <a-input v-model:value="instantiateDto.configCode" placeholder="如 customer_form_new" />
        </a-form-item>
        <a-form-item label="配置名称" required>
          <a-input v-model:value="instantiateDto.configName" placeholder="如 客户表单-2026Q3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 导入 JSON 弹窗 -->
    <SchemaImporter
      v-model:visible="importerVisible"
      title="导入模板 Schema"
      @import="handleImport"
    />
  </PageContainer>
</template>
