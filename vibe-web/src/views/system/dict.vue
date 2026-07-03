<script setup lang="ts">
/**
 * 数据字典
 * 字典类型 + 字典数据双表管理
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined,
  FileTextOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageDictTypes,
  createDictType,
  updateDictType,
  deleteDictType,
  pageDictData,
  createDictData,
  updateDictData,
  deleteDictData
} from '@/api/system'
import type { SysDictType, SysDictData } from '@/api/system'
import type { PageResult } from '@/types/api'

const typeLoading = ref(false)
const typeData = ref<SysDictType[]>([])
const typePagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })

async function loadTypes() {
  typeLoading.value = true
  try {
    const res = (await pageDictTypes({ page: typePagination.current, size: typePagination.pageSize })) as unknown as PageResult<SysDictType>
    typeData.value = res.records || []
    typePagination.total = res.total || 0
  } catch (e) {
    console.error('[system.dict] load types failed:', e)
  } finally {
    typeLoading.value = false
  }
}

const typeColumns = [
  { title: '字典编码', dataIndex: 'dictCode', key: 'dictCode', width: 180 },
  { title: '字典名称', dataIndex: 'dictName', key: 'dictName', width: 180 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '状态', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
]

// 字典类型弹窗
const typeVisible = ref(false)
const typeLoading2 = ref(false)
const typeIsEdit = ref(false)
const typeForm = reactive<Partial<SysDictType>>({
  dictCode: '',
  dictName: '',
  description: '',
  status: 1
})

function openTypeCreate() {
  typeIsEdit.value = false
  Object.assign(typeForm, { id: undefined, dictCode: '', dictName: '', description: '', status: 1 })
  typeVisible.value = true
}

function openTypeEdit(row: SysDictType) {
  typeIsEdit.value = true
  Object.assign(typeForm, { ...row })
  typeVisible.value = true
}

async function handleTypeSubmit() {
  if (!typeForm.dictCode || !typeForm.dictName) {
    message.warning('请填写字典编码和名称')
    return
  }
  typeLoading2.value = true
  try {
    if (typeIsEdit.value && typeForm.id) {
      await updateDictType(typeForm.id, typeForm)
      message.success('更新成功')
    } else {
      await createDictType(typeForm)
      message.success('创建成功')
    }
    typeVisible.value = false
    loadTypes()
  } catch (e) {
    // ignore
  } finally {
    typeLoading2.value = false
  }
}

function handleTypeDelete(row: SysDictType) {
  Modal.confirm({
    title: '确认删除',
    content: `删除字典「${row.dictName}」将同时删除其下所有数据项，确定继续吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteDictType(row.id)
        message.success('删除成功')
        if (currentType.value?.id === row.id) {
          currentType.value = null
          dataData.value = []
        }
        loadTypes()
      } catch (e) { /* ignore */ }
    }
  })
}

// 字典数据
const currentType = ref<SysDictType | null>(null)
const dataLoading = ref(false)
const dataData = ref<SysDictData[]>([])
const dataPagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })

async function openData(row: SysDictType) {
  currentType.value = row
  dataPagination.current = 1
  await loadData()
}

async function loadData() {
  if (!currentType.value) return
  dataLoading.value = true
  try {
    const res = (await pageDictData({ dictTypeId: currentType.value.id, page: dataPagination.current, size: dataPagination.pageSize })) as unknown as PageResult<SysDictData>
    dataData.value = res.records || []
    dataPagination.total = res.total || 0
  } catch (e) {
    console.error('[system.dict] load data failed:', e)
  } finally {
    dataLoading.value = false
  }
}

const dataColumns = [
  { title: '标签', dataIndex: 'dictLabel', key: 'dictLabel', width: 150 },
  { title: '值', dataIndex: 'dictValue', key: 'dictValue', width: 150 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '默认', key: 'isDefault', width: 80 },
  { title: '状态', key: 'status', width: 90 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

// 字典数据弹窗
const dataVisible = ref(false)
const dataLoading2 = ref(false)
const dataIsEdit = ref(false)
const dataForm = reactive<Partial<SysDictData>>({
  dictLabel: '',
  dictValue: '',
  sort: 0,
  isDefault: 0,
  status: 1,
  remark: ''
})

function openDataCreate() {
  if (!currentType.value) return
  dataIsEdit.value = false
  Object.assign(dataForm, { id: undefined, dictLabel: '', dictValue: '', sort: 0, isDefault: 0, status: 1, remark: '' })
  dataVisible.value = true
}

function openDataEdit(row: SysDictData) {
  dataIsEdit.value = true
  Object.assign(dataForm, { ...row })
  dataVisible.value = true
}

async function handleDataSubmit() {
  if (!dataForm.dictLabel || dataForm.dictValue == null) {
    message.warning('请填写标签和值')
    return
  }
  dataLoading2.value = true
  try {
    if (dataIsEdit.value && dataForm.id) {
      await updateDictData(dataForm.id, dataForm)
      message.success('更新成功')
    } else {
      await createDictData({ ...dataForm, dictTypeId: currentType.value!.id })
      message.success('创建成功')
    }
    dataVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    dataLoading2.value = false
  }
}

function handleDataDelete(row: SysDictData) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除字典项「${row.dictLabel}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteDictData(row.id)
        message.success('删除成功')
        loadData()
      } catch (e) { /* ignore */ }
    }
  })
}

onMounted(() => {
  loadTypes()
})
</script>

<template>
  <PageContainer title="数据字典" description="字典类型与字典项管理">
    <template #extra>
      <a-button @click="loadTypes"><template #icon><ReloadOutlined /></template>刷新</a-button>
    </template>

    <a-row :gutter="16">
      <!-- 字典类型 -->
      <a-col :xs="24" :md="12">
        <div class="vibe-card table-card">
          <div class="card-head">
            <h3 class="card-title">字典类型</h3>
            <a-button type="primary" size="small" @click="openTypeCreate"><template #icon><PlusOutlined /></template>新增</a-button>
          </div>
          <a-table :columns="typeColumns" :data-source="typeData" :loading="typeLoading" :pagination="typePagination" row-key="id" size="small" :scroll="{ x: 900 }" @change="(p: any) => { typePagination.current = p.current; loadTypes() }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <StatusTag :tone="record.status === 1 ? 'success' : 'archived'">{{ record.status === 1 ? '启用' : '禁用' }}</StatusTag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space size="small">
                  <a @click="openData(record)"><FileTextOutlined /> 数据</a>
                  <a @click="openTypeEdit(record)"><EditOutlined /></a>
                  <a class="danger-link" @click="handleTypeDelete(record)"><DeleteOutlined /></a>
                </a-space>
              </template>
            </template>
            <template #emptyText><EmptyState description="暂无字典类型" /></template>
          </a-table>
        </div>
      </a-col>

      <!-- 字典数据 -->
      <a-col :xs="24" :md="12">
        <div class="vibe-card table-card">
          <div class="card-head">
            <h3 class="card-title">
              字典数据
              <span v-if="currentType" class="text-auxiliary"> - {{ currentType.dictName }}</span>
            </h3>
            <a-button type="primary" size="small" :disabled="!currentType" @click="openDataCreate"><template #icon><PlusOutlined /></template>新增</a-button>
          </div>
          <EmptyState v-if="!currentType" description="请选择左侧字典类型查看数据项" />
          <a-table v-else :columns="dataColumns" :data-source="dataData" :loading="dataLoading" :pagination="dataPagination" row-key="id" size="small" :scroll="{ x: 800 }" @change="(p: any) => { dataPagination.current = p.current; loadData() }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'isDefault'">
                <a-tag v-if="record.isDefault === 1" color="green">默认</a-tag>
                <span v-else>-</span>
              </template>
              <template v-else-if="column.key === 'status'">
                <StatusTag :tone="record.status === 1 ? 'success' : 'archived'">{{ record.status === 1 ? '启用' : '禁用' }}</StatusTag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space size="small">
                  <a @click="openDataEdit(record)"><EditOutlined /> 编辑</a>
                  <a class="danger-link" @click="handleDataDelete(record)"><DeleteOutlined /></a>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>
      </a-col>
    </a-row>

    <!-- 字典类型弹窗 -->
    <a-modal v-model:open="typeVisible" :title="typeIsEdit ? '编辑字典类型' : '新增字典类型'" :confirm-loading="typeLoading2" @ok="handleTypeSubmit">
      <a-form layout="vertical">
        <a-form-item label="字典编码" required>
          <a-input v-model:value="typeForm.dictCode" :disabled="typeIsEdit" placeholder="如 project_status" />
        </a-form-item>
        <a-form-item label="字典名称" required>
          <a-input v-model:value="typeForm.dictName" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="typeForm.status">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="typeForm.description" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 字典数据弹窗 -->
    <a-modal v-model:open="dataVisible" :title="dataIsEdit ? '编辑字典项' : '新增字典项'" :confirm-loading="dataLoading2" @ok="handleDataSubmit">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="字典标签" required>
              <a-input v-model:value="dataForm.dictLabel" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="字典值" required>
              <a-input v-model:value="dataForm.dictValue" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="排序">
              <a-input-number v-model:value="dataForm.sort" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态">
              <a-select v-model:value="dataForm.status">
                <a-select-option :value="1">启用</a-select-option>
                <a-select-option :value="0">禁用</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="是否默认">
              <a-switch :checked="dataForm.isDefault === 1" @change="(v: boolean) => dataForm.isDefault = v ? 1 : 0" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注">
              <a-textarea v-model:value="dataForm.remark" :rows="2" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.table-card { padding-bottom: 0; }
.card-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 20px; border-bottom: 1px solid @border-color-split;
}
.card-title { margin: 0; font-size: 15px; font-weight: 600; color: @text-primary; }
.danger-link { color: @status-exception; }
.text-auxiliary { color: @text-auxiliary; font-weight: 400; font-size: 13px; }
</style>
