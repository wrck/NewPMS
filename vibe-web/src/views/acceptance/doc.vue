<script setup lang="ts">
/**
 * 竣工文档
 * 设计文档 2.7.5：As-Built 拓扑/设备清单/配置备份/测试报告/维护手册
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageAcceptanceDocs,
  createAcceptanceDoc,
  updateAcceptanceDoc,
  deleteAcceptanceDoc
} from '@/api/acceptance'
import type { AcceptanceDoc, AcceptanceDocQuery, AcceptanceDocDTO, AcceptanceDocType } from '@/types/acceptance'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<AcceptanceDoc[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`
})
const query = reactive<AcceptanceDocQuery>({ projectId: undefined, taskId: undefined, docType: undefined })

const docTypeMap: Record<string, { label: string; color: string }> = {
  TOPOLOGY: { label: '网络拓扑图', color: 'blue' },
  DEVICE_LIST: { label: '设备清单', color: 'cyan' },
  CONFIG_BACKUP: { label: '配置备份', color: 'green' },
  TEST_REPORT: { label: '测试报告', color: 'orange' },
  MAINTENANCE_MANUAL: { label: '维护手册', color: 'purple' },
  OTHER: { label: '其他', color: 'default' }
}

async function loadData() {
  loading.value = true
  try {
    const res = (await pageAcceptanceDocs({
      ...query,
      page: pagination.current,
      size: pagination.pageSize
    })) as unknown as PageResult<AcceptanceDoc>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[acceptance.doc] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  query.projectId = undefined
  query.taskId = undefined
  query.docType = undefined
  handleSearch()
}

const columns = [
  { title: '文档名称', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '文档类型', dataIndex: 'docType', key: 'docType', width: 140 },
  { title: '项目ID', dataIndex: 'projectId', key: 'projectId', width: 100 },
  { title: '版本', dataIndex: 'docVersion', key: 'docVersion', width: 100 },
  { title: '文件大小', dataIndex: 'fileSize', key: 'fileSize', width: 120 },
  { title: '上传时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const }
]

function formatFileSize(bytes: number | undefined) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

/* ============ 新增/编辑 ============ */
const modalVisible = ref(false)
const modalTitle = ref('')
const formRef = ref()
const form = reactive<AcceptanceDocDTO>({
  taskId: undefined,
  projectId: undefined,
  docType: 'TOPOLOGY' as AcceptanceDocType,
  name: '',
  fileUrl: '',
  fileSize: undefined,
  docVersion: '1.0.0',
  remark: ''
})
const rules = {
  taskId: [{ required: true, message: '请输入验收任务ID' }],
  projectId: [{ required: true, message: '请输入项目ID' }],
  docType: [{ required: true, message: '请选择文档类型' }],
  name: [{ required: true, message: '请输入文档名称' }],
  fileUrl: [{ required: true, message: '请输入文档URL' }]
}

function openCreate() {
  modalTitle.value = '上传竣工文档'
  Object.assign(form, {
    id: undefined,
    taskId: undefined,
    projectId: undefined,
    docType: 'TOPOLOGY',
    name: '',
    fileUrl: '',
    fileSize: undefined,
    docVersion: '1.0.0',
    remark: ''
  })
  modalVisible.value = true
}

function openEdit(record: AcceptanceDoc) {
  modalTitle.value = '编辑竣工文档'
  Object.assign(form, {
    id: record.id,
    taskId: record.taskId,
    projectId: record.projectId,
    docType: record.docType,
    name: record.name,
    fileUrl: record.fileUrl,
    fileSize: record.fileSize,
    docVersion: record.docVersion,
    remark: record.remark
  })
  modalVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  try {
    if (form.id) {
      await updateAcceptanceDoc(form.id, form)
      message.success('更新成功')
    } else {
      await createAcceptanceDoc(form)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch (e) {
    console.error('[acceptance.doc] save failed:', e)
  }
}

function handleDelete(record: AcceptanceDoc) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除文档「${record.name}」吗？`,
    okText: '删除',
    okType: 'danger',
    async onOk() {
      try {
        await deleteAcceptanceDoc(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        console.error('[acceptance.doc] delete failed:', e)
      }
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="竣工文档" description="As-Built 拓扑图 / 设备清单 / 配置备份 / 测试报告 / 维护手册">
    <template #extra>
      <a-button @click="handleReset">
        <template #icon><ReloadOutlined /></template>
        重置
      </a-button>
      <a-button type="primary" @click="openCreate">
        <template #icon><PlusOutlined /></template>
        上传文档
      </a-button>
    </template>

    <a-form layout="inline" style="margin-bottom: 16px" @submit.prevent="handleSearch">
      <a-form-item label="项目ID">
        <a-input-number v-model:value="query.projectId" placeholder="项目ID" :min="1" style="width: 140px" />
      </a-form-item>
      <a-form-item label="任务ID">
        <a-input-number v-model:value="query.taskId" placeholder="任务ID" :min="1" style="width: 140px" />
      </a-form-item>
      <a-form-item label="文档类型">
        <a-select v-model:value="query.docType" placeholder="全部" allow-clear style="width: 160px">
          <a-select-option v-for="(v, k) in docTypeMap" :key="k" :value="k">{{ v.label }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">查询</a-button>
      </a-form-item>
    </a-form>

    <a-table
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      :row-key="(record: AcceptanceDoc) => record.id"
      @change="(p: any) => { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }"
    >
      <template #emptyText>
        <EmptyState description="暂无竣工文档" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'docType'">
          <a-tag :color="docTypeMap[record.docType]?.color || 'default'">
            {{ docTypeMap[record.docType]?.label || record.docType }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'fileSize'">
          {{ formatFileSize(record.fileSize) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" :href="record.fileUrl" target="_blank">
            <DownloadOutlined /> 下载
          </a-button>
          <a-button type="link" size="small" @click="openEdit(record)">
            <EditOutlined /> 编辑
          </a-button>
          <a-button type="link" size="small" danger @click="handleDelete(record)">
            <DeleteOutlined />
          </a-button>
        </template>
      </template>
    </a-table>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="640px"
      :ok-text="form.id ? '保存' : '创建'"
      cancel-text="取消"
      @ok="handleSubmit"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="验收任务ID" name="taskId">
              <a-input-number v-model:value="form.taskId" placeholder="任务ID" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="项目ID" name="projectId">
              <a-input-number v-model:value="form.projectId" placeholder="项目ID" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="文档类型" name="docType">
              <a-select v-model:value="form.docType">
                <a-select-option v-for="(v, k) in docTypeMap" :key="k" :value="k">{{ v.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="文档版本">
              <a-input v-model:value="form.docVersion" placeholder="1.0.0" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="文档名称" name="name">
          <a-input v-model:value="form.name" placeholder="请输入文档名称" />
        </a-form-item>
        <a-form-item label="文档URL" name="fileUrl">
          <a-input v-model:value="form.fileUrl" placeholder="MinIO objectName 或完整URL" />
        </a-form-item>
        <a-form-item label="文件大小（字节）">
          <a-input-number v-model:value="form.fileSize" placeholder="自动填充" style="width: 100%" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="form.remark" :rows="2" placeholder="备注" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>
