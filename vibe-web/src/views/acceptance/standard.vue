<script setup lang="ts">
/**
 * 验收标准管理
 * 设计文档 2.7.1：验收标准模板库 + 检查项定义 + 项目级验收标准定制
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageAcceptanceStandards,
  createAcceptanceStandard,
  updateAcceptanceStandard,
  deleteAcceptanceStandard
} from '@/api/acceptance'
import type { AcceptanceStandard, AcceptanceStandardQuery, AcceptanceStandardDTO } from '@/types/acceptance'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<AcceptanceStandard[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`
})
const query = reactive<AcceptanceStandardQuery>({ name: '', projectType: '', status: undefined })

async function loadData() {
  loading.value = true
  try {
    const res = (await pageAcceptanceStandards({
      ...query,
      page: pagination.current,
      size: pagination.pageSize
    })) as unknown as PageResult<AcceptanceStandard>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[acceptance.standard] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  query.name = ''
  query.projectType = ''
  query.status = undefined
  handleSearch()
}

const columns = [
  { title: '标准名称', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '适用项目类型', dataIndex: 'projectType', key: 'projectType', width: 140 },
  { title: '版本', dataIndex: 'standardVersion', key: 'standardVersion', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const }
]

/* ============ 新增/编辑弹窗 ============ */
const modalVisible = ref(false)
const modalTitle = ref('')
const formRef = ref()
const form = reactive<AcceptanceStandardDTO>({
  name: '',
  projectType: '',
  standardVersion: '1.0.0',
  description: '',
  status: 1,
  items: []
})

const rules = {
  name: [{ required: true, message: '请输入标准名称' }]
}

function openCreate() {
  modalTitle.value = '新建验收标准'
  Object.assign(form, {
    id: undefined,
    name: '',
    projectType: '',
    standardVersion: '1.0.0',
    description: '',
    status: 1,
    items: []
  })
  modalVisible.value = true
}

function openEdit(record: AcceptanceStandard) {
  modalTitle.value = '编辑验收标准'
  Object.assign(form, {
    id: record.id,
    name: record.name,
    projectType: record.projectType,
    standardVersion: record.standardVersion,
    description: record.description,
    status: record.status,
    items: record.items || []
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
      await updateAcceptanceStandard(form.id, form)
      message.success('更新成功')
    } else {
      await createAcceptanceStandard(form)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch (e) {
    console.error('[acceptance.standard] save failed:', e)
  }
}

function handleDelete(record: AcceptanceStandard) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除验收标准「${record.name}」吗？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deleteAcceptanceStandard(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        console.error('[acceptance.standard] delete failed:', e)
      }
    }
  })
}

/* ============ 检查项编辑 ============ */
function addItem() {
  if (!form.items) form.items = []
  form.items.push({ name: '', requirement: '', testMethod: '', weight: 1, sortOrder: form.items.length })
}

function removeItem(index: number) {
  form.items?.splice(index, 1)
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="验收标准" description="验收标准模板库与检查项定义">
    <template #extra>
      <a-button @click="handleReset">
        <template #icon><ReloadOutlined /></template>
        重置
      </a-button>
      <a-button type="primary" @click="openCreate">
        <template #icon><PlusOutlined /></template>
        新建标准
      </a-button>
    </template>

    <!-- 搜索栏 -->
    <a-form layout="inline" style="margin-bottom: 16px" @submit.prevent="handleSearch">
      <a-form-item label="名称">
        <a-input v-model:value="query.name" placeholder="标准名称" allow-clear @pressEnter="handleSearch" />
      </a-form-item>
      <a-form-item label="项目类型">
        <a-input v-model:value="query.projectType" placeholder="项目类型" allow-clear @pressEnter="handleSearch" />
      </a-form-item>
      <a-form-item label="状态">
        <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 120px">
          <a-select-option :value="1">启用</a-select-option>
          <a-select-option :value="0">禁用</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">查询</a-button>
      </a-form-item>
    </a-form>

    <!-- 列表 -->
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      :row-key="(record: AcceptanceStandard) => record.id"
      @change="(p: any) => { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }"
    >
      <template #emptyText>
        <EmptyState description="暂无验收标准" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'default'">
            {{ record.status === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="openEdit(record)">
            <EditOutlined /> 编辑
          </a-button>
          <a-button type="link" size="small" danger @click="handleDelete(record)">
            <DeleteOutlined /> 删除
          </a-button>
        </template>
      </template>
    </a-table>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="720px"
      :ok-text="form.id ? '保存' : '创建'"
      cancel-text="取消"
      @ok="handleSubmit"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="标准名称" name="name">
              <a-input v-model:value="form.name" placeholder="请输入标准名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="适用项目类型">
              <a-input v-model:value="form.projectType" placeholder="如：ROUTER / SWITCH" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="版本">
              <a-input v-model:value="form.standardVersion" placeholder="1.0.0" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态">
              <a-select v-model:value="form.status">
                <a-select-option :value="1">启用</a-select-option>
                <a-select-option :value="0">禁用</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="标准说明">
          <a-textarea v-model:value="form.description" :rows="2" placeholder="标准说明" />
        </a-form-item>

        <!-- 检查项列表 -->
        <div style="margin-top: 8px">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px">
            <strong>检查项</strong>
            <a-button type="dashed" size="small" @click="addItem">
              <PlusOutlined /> 添加检查项
            </a-button>
          </div>
          <div v-for="(item, idx) in form.items" :key="idx" style="display: flex; gap: 8px; margin-bottom: 8px">
            <a-input v-model:value="item.name" placeholder="检查项名称" style="flex: 2" />
            <a-input v-model:value="item.requirement" placeholder="检查要求" style="flex: 3" />
            <a-input-number v-model:value="item.weight" :min="0" :step="0.1" placeholder="权重" style="width: 100px" />
            <a-button type="link" danger size="small" @click="removeItem(idx)">
              <DeleteOutlined />
            </a-button>
          </div>
          <a-empty v-if="!form.items || form.items.length === 0" description="暂无检查项" />
        </div>
      </a-form>
    </a-modal>
  </PageContainer>
</template>
