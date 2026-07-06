<script setup lang="ts">
/**
 * 验收标准管理
 * 设计文档 2.7.1：验收标准模板库 + 检查项定义 + 项目级验收标准定制
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, CheckSquareOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageAcceptanceStandards,
  createAcceptanceStandard,
  updateAcceptanceStandard,
  deleteAcceptanceStandard,
  getAcceptanceStandardDetail
} from '@/api/acceptance'
import type { AcceptanceStandard, AcceptanceStandardQuery, AcceptanceStandardDTO, AcceptanceStandardItem } from '@/types/acceptance'
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
  { title: '操作', key: 'action', width: 260, fixed: 'right' as const }
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

/* ============ 检查项子表抽屉 ============ */
const itemDrawerVisible = ref(false)
const itemDrawerLoading = ref(false)
const itemDrawerSaving = ref(false)
const currentStandard = ref<AcceptanceStandard | null>(null)
const itemRows = ref<AcceptanceStandardItem[]>([])

const itemColumns = [
  { title: '序号', key: 'sortOrder', width: 70 },
  { title: '检查项名称', key: 'name', width: 180 },
  { title: '检查要求', key: 'requirement' },
  { title: '测试方法', key: 'testMethod', width: 160 },
  { title: '权重', key: 'weight', width: 80 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const }
]

async function openItemDrawer(record: AcceptanceStandard) {
  currentStandard.value = record
  itemDrawerVisible.value = true
  itemDrawerLoading.value = true
  try {
    const detail = (await getAcceptanceStandardDetail(record.id)) as unknown as AcceptanceStandard
    itemRows.value = (detail.items || []).map((it) => ({ ...it }))
  } catch (e) {
    console.error('[acceptance.standard] load items failed:', e)
    itemRows.value = (record.items || []).map((it) => ({ ...it }))
  } finally {
    itemDrawerLoading.value = false
  }
}

function addItemRow() {
  itemRows.value.push({
    name: '',
    requirement: '',
    testMethod: '',
    weight: 1,
    sortOrder: itemRows.value.length
  })
}

function removeItemRow(idx: number) {
  itemRows.value.splice(idx, 1)
  // 重新排序
  itemRows.value.forEach((it, i) => (it.sortOrder = i))
}

function moveItemRow(idx: number, direction: 'up' | 'down') {
  const target = direction === 'up' ? idx - 1 : idx + 1
  if (target < 0 || target >= itemRows.value.length) return
  const tmp = itemRows.value[idx]
  itemRows.value[idx] = itemRows.value[target]
  itemRows.value[target] = tmp
  itemRows.value.forEach((it, i) => (it.sortOrder = i))
}

async function saveItems() {
  if (!currentStandard.value) return
  // 校验检查项名称非空
  const emptyName = itemRows.value.find((it) => !it.name?.trim())
  if (emptyName) {
    message.warning('存在检查项名称为空，请补充')
    return
  }
  itemDrawerSaving.value = true
  try {
    const dto: AcceptanceStandardDTO = {
      id: currentStandard.value.id,
      name: currentStandard.value.name,
      projectType: currentStandard.value.projectType,
      standardVersion: currentStandard.value.standardVersion,
      description: currentStandard.value.description,
      status: currentStandard.value.status,
      items: itemRows.value.map((it, idx) => ({
        id: it.id,
        standardId: currentStandard.value!.id,
        name: it.name,
        requirement: it.requirement,
        testMethod: it.testMethod,
        weight: it.weight,
        sortOrder: idx
      }))
    }
    await updateAcceptanceStandard(currentStandard.value.id, dto)
    message.success('检查项已保存')
    itemDrawerVisible.value = false
    loadData()
  } catch (e) {
    console.error('[acceptance.standard] save items failed:', e)
  } finally {
    itemDrawerSaving.value = false
  }
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
          <a-button type="link" size="small" @click="openItemDrawer(record)">
            <CheckSquareOutlined /> 检查项
          </a-button>
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

    <!-- 检查项子表抽屉 -->
    <a-drawer
      :open="itemDrawerVisible"
      :width="960"
      :title="`检查项管理 - ${currentStandard?.name || ''}`"
      @close="itemDrawerVisible = false"
    >
      <a-spin :spinning="itemDrawerLoading">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px">
          <span class="text-auxiliary">共 {{ itemRows.length }} 项检查，编辑后点击底部保存。</span>
          <a-button type="primary" size="small" @click="addItemRow">
            <PlusOutlined /> 添加检查项
          </a-button>
        </div>
        <a-table
          :columns="itemColumns"
          :data-source="itemRows"
          row-key="sortOrder"
          size="small"
          :pagination="false"
          :scroll="{ x: 900 }"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'sortOrder'">{{ index + 1 }}</template>
            <template v-else-if="column.key === 'name'">
              <a-input v-model:value="record.name" placeholder="检查项名称" size="small" />
            </template>
            <template v-else-if="column.key === 'requirement'">
              <a-input v-model:value="record.requirement" placeholder="检查要求" size="small" />
            </template>
            <template v-else-if="column.key === 'testMethod'">
              <a-input v-model:value="record.testMethod" placeholder="测试方法" size="small" />
            </template>
            <template v-else-if="column.key === 'weight'">
              <a-input-number v-model:value="record.weight" :min="0" :step="0.1" size="small" style="width: 80px" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space size="small">
                <a-button type="link" size="small" :disabled="index === 0" @click="moveItemRow(index, 'up')">上移</a-button>
                <a-button type="link" size="small" :disabled="index === itemRows.length - 1" @click="moveItemRow(index, 'down')">下移</a-button>
                <a-button type="link" size="small" danger @click="removeItemRow(index)">
                  <DeleteOutlined />
                </a-button>
              </a-space>
            </template>
          </template>
          <template #emptyText>
            <EmptyState description="暂无检查项" action-text="添加检查项" @action="addItemRow" />
          </template>
        </a-table>
      </a-spin>
      <div class="drawer-footer">
        <a-space>
          <a-button @click="itemDrawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="itemDrawerSaving" @click="saveItems">保存</a-button>
        </a-space>
      </div>
    </a-drawer>
  </PageContainer>
</template>

<style lang="less" scoped>
.text-auxiliary { color: @text-tertiary; }
.drawer-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 24px;
  border-top: 1px solid @border-color-split;
  text-align: right;
}
</style>
