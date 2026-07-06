<script setup lang="ts">
/**
 * CrudTable 通用 CRUD 表格组件（spec 阶段三 - Task 12）
 *
 * 一站式封装：
 *   1. 搜索表单（基于 searchFields 自动渲染，支持展开/折叠）
 *   2. 表格（a-table + 分页 + 排序 + 行选择 + 批量操作）
 *   3. 新增/编辑弹窗（基于 columns 自动生成 Form + 表单校验）
 *   4. 删除确认（a-popconfirm）
 *   5. 状态反馈（message.success / message.error）+ 异常处理（统一 try/catch）
 *   6. 自定义 actions（行级操作按钮）
 *   7. 权限控制（permissionPrefix 自动拼接 create/update/delete）
 *
 * 使用示例见 ./README.md
 */
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
  DownOutlined,
  UpOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import type { PageResult, PageParams } from '@/types/api'
import type {
  CrudColumn,
  SearchField,
  CrudAction,
  CrudApiFunc,
  ModelBinding,
  CrudTableExpose
} from './types'

/* ============ Props ============ */
interface CrudProps {
  /** 列定义 */
  columns: CrudColumn[]
  /** 搜索字段 */
  searchFields?: SearchField[]
  /** API 函数集合 */
  apiFunc: CrudApiFunc
  /** 主键绑定（默认 idField='id'） */
  modelBinding?: ModelBinding
  /** 是否支持行选择 */
  rowSelection?: boolean
  /** 自定义操作按钮 */
  actions?: CrudAction[]
  /** 权限前缀，如 'system:user'，自动拼接 :create / :update / :delete */
  permissionPrefix?: string
  /** 表格标题 */
  title?: string
  /** 表格描述 */
  description?: string
  /** 每页条数（默认 10） */
  pageSize?: number
  /** 表格滚动宽度 */
  scrollX?: number | string
  /** 弹窗宽度（默认 640） */
  formWidth?: number | string
  /** 新增按钮文案 */
  createText?: string
  /** 是否默认展开搜索表单 */
  searchCollapsed?: boolean
  /** 行 key 字段（默认与 modelBinding.idField 一致） */
  rowKey?: string
}

const props = withDefaults(defineProps<CrudProps>(), {
  modelBinding: () => ({ idField: 'id' }),
  rowSelection: false,
  pageSize: 10,
  formWidth: 640,
  createText: '新增',
  searchCollapsed: true,
  title: '',
  description: ''
})

/* ============ Emits ============ */
interface CrudEmits {
  /** 行选择变化 */
  (e: 'selectionChange', rows: any[]): void
  /** 新增/编辑成功后触发 */
  (e: 'saved', type: 'create' | 'update', record: any): void
  /** 删除成功后触发 */
  (e: 'deleted', id: any): void
  /** 搜索触发 */
  (e: 'search', query: Record<string, any>): void
}

const emit = defineEmits<CrudEmits>()

/* ============ Store ============ */
const userStore = useUserStore()

/* ============ 主键相关 ============ */
const idField = computed(() => props.modelBinding?.idField || props.rowKey || 'id')
const idLabel = computed(() => props.modelBinding?.idLabel || 'ID')

function getRecordId(record: any): any {
  if (!record) return undefined
  return record[idField.value]
}

function getNestedValue(obj: any, path: string): any {
  if (!path) return undefined
  if (!path.includes('.')) return obj?.[path]
  return path.split('.').reduce((acc, key) => (acc == null ? undefined : acc[key]), obj)
}

function setNestedValue(obj: any, path: string, value: any): void {
  if (!path.includes('.')) {
    obj[path] = value
    return
  }
  const keys = path.split('.')
  const last = keys.pop() as string
  const target = keys.reduce((acc, key) => {
    if (acc[key] == null) acc[key] = {}
    return acc[key]
  }, obj)
  target[last] = value
}

/* ============ 权限 ============ */
function hasPermission(perm?: string): boolean {
  if (!perm) return true
  if (!props.permissionPrefix) return true
  return userStore.hasPermission(`${props.permissionPrefix}:${perm}`)
}

function getActionPermission(perm?: string): string | undefined {
  if (!perm) return undefined
  if (props.permissionPrefix) {
    return `${props.permissionPrefix}:${perm}`
  }
  return perm
}

/* ============ 搜索表单 ============ */
const collapsed = ref(props.searchCollapsed)
const searchQuery = reactive<Record<string, any>>({})

function initSearchQuery() {
  // 重置
  Object.keys(searchQuery).forEach((k) => delete searchQuery[k])
  const fields = props.searchFields || []
  for (const f of fields) {
    searchQuery[f.field] = f.defaultValue !== undefined ? f.defaultValue : undefined
  }
}

const visibleSearchFields = computed(() => {
  const fields = props.searchFields || []
  if (collapsed.value) {
    // 折叠时只展示前 3 个（如果有 4 个及以上）
    return fields.length > 3 ? fields.slice(0, 3) : fields
  }
  return fields
})

const hasMoreSearch = computed(() => (props.searchFields?.length || 0) > 3)

function toggleCollapse() {
  collapsed.value = !collapsed.value
}

function handleSearch() {
  pagination.current = 1
  emit('search', { ...searchQuery })
  loadData()
}

function handleReset() {
  initSearchQuery()
  pagination.current = 1
  emit('search', { ...searchQuery })
  return loadData()
}

/* ============ 表格 / 分页 ============ */
const loading = ref(false)
const dataSource = ref<any[]>([])
const pagination = reactive({
  current: 1,
  pageSize: props.pageSize,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50', '100']
})

const sorter = ref<{ field?: string; order?: 'ascend' | 'descend' }>({})

async function loadData() {
  loading.value = true
  try {
    const params: PageParams & Record<string, any> = {
      page: pagination.current,
      size: pagination.pageSize,
      ...searchQuery
    }
    if (sorter.value.field) {
      params.sortField = sorter.value.field
      params.sortOrder = sorter.value.order === 'descend' ? 'desc' : 'asc'
    }
    const res = (await props.apiFunc.page(params as any)) as unknown as PageResult<any>
    dataSource.value = res?.records || []
    pagination.total = res?.total || 0
  } catch (e) {
    console.error('[CrudTable] load failed:', e)
    // 错误已在 request 拦截器统一弹提示，这里不重复 message
  } finally {
    loading.value = false
  }
}

function handleTableChange(pag: any, _filters: any, sort: any) {
  if (pag) {
    pagination.current = pag.current || pag.page || 1
    pagination.pageSize = pag.pageSize || pag.size || props.pageSize
  }
  if (sort) {
    sorter.value = sort.field
      ? { field: sort.field, order: sort.order as 'ascend' | 'descend' }
      : {}
  }
  loadData()
}

/* ============ 行选择 ============ */
const selectedRowKeys = ref<any[]>([])
const selectedRows = ref<any[]>([])

const rowSelectionConfig = computed(() => {
  if (!props.rowSelection) return undefined
  return {
    selectedRowKeys: selectedRowKeys.value,
    onChange: (keys: any[], rows: any[]) => {
      selectedRowKeys.value = keys
      selectedRows.value = rows
      emit('selectionChange', rows)
    },
    getCheckboxProps: (record: any) => ({ disabled: false })
  }
})

function clearSelected() {
  selectedRowKeys.value = []
  selectedRows.value = []
}

async function handleBatchDelete() {
  if (!selectedRowKeys.value.length) {
    message.warning('请先勾选要删除的行')
    return
  }
  Modal.confirm({
    title: '批量删除',
    content: `确定删除选中的 ${selectedRowKeys.value.length} 条数据吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await Promise.all(selectedRowKeys.value.map((id) => props.apiFunc.delete(id)))
        message.success('批量删除成功')
        clearSelected()
        await loadData()
      } catch (e) {
        console.error('[CrudTable] batch delete failed:', e)
      }
    }
  })
}

/* ============ 表格列构造 ============ */
const tableColumns = computed(() => {
  const cols = props.columns
    .filter((c) => !c.hideInTable)
    .map((c) => {
      const col: any = {
        title: c.title,
        dataIndex: c.field,
        key: c.field,
        width: c.width,
        align: c.align,
        ellipsis: c.ellipsis !== false,
        fixed: c.fixed,
        sorter: c.sortable
      }
      return col
    })
  // 操作列
  cols.push({
    title: '操作',
    key: '__action__',
    width: computeActionWidth(),
    fixed: 'right',
    align: 'center'
  })
  return cols
})

function computeActionWidth(): number {
  let width = 0
  if (hasPermission('update')) width += 60
  if (hasPermission('delete')) width += 60
  const customs = props.actions?.filter((a) => !a.divider) || []
  width += customs.length * 70
  if (customs.length > 0) width += 16
  return Math.max(width, 120)
}

/** 当前行的可用 actions（含内置编辑/删除） */
function resolveRowActions(record: any): CrudAction[] {
  const list: CrudAction[] = []
  if (hasPermission('update')) {
    list.push({
      label: '编辑',
      icon: EditOutlined,
      onClick: () => openEdit(record),
      permission: 'update'
    })
  }
  const customs = props.actions || []
  for (const a of customs) {
    if (a.divider) {
      list.push(a)
      continue
    }
    if (a.visible && !a.visible(record)) continue
    list.push(a)
  }
  if (hasPermission('delete')) {
    list.push({
      label: '删除',
      icon: DeleteOutlined,
      onClick: () => handleDelete(record),
      permission: 'delete',
      danger: true
    })
  }
  return list
}

/* ============ 单元格渲染（valueEnum / format） ============ */
function findColumn(field: string): CrudColumn | undefined {
  return props.columns.find((c) => c.field === field)
}

function renderCellText(col: CrudColumn, record: any): string {
  const value = getNestedValue(record, col.field)
  if (col.format) {
    try {
      return col.format(value, record)
    } catch {
      return value == null ? '' : String(value)
    }
  }
  if (col.valueEnum && value != null) {
    const item = col.valueEnum[String(value)]
    return item?.text ?? String(value)
  }
  return value == null ? '' : String(value)
}

/** 按字段名渲染单元格文本（用于模板，避免每次重新 find） */
function renderCellTextByField(field: string, record: any): string {
  const col = findColumn(field)
  if (!col) {
    const v = getNestedValue(record, field)
    return v == null ? '' : String(v)
  }
  return renderCellText(col, record)
}

/** 取列的 valueEnum（用于模板判断） */
function getColumnValueEnum(field: string): Record<string, any> | undefined {
  return findColumn(field)?.valueEnum
}

/** 取列的 valueEnum 项（用于模板渲染 Tag） */
function getValueEnumTag(field: string, record: any) {
  const col = findColumn(field)
  if (!col || !col.valueEnum) return undefined
  const value = getNestedValue(record, col.field)
  if (value == null) return undefined
  return col.valueEnum[String(value)]
}

/* ============ 弹窗表单 ============ */
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formModel = reactive<Record<string, any>>({})
const formRef = ref()
/** 编辑时记录当前 record 主键，避免依赖 formModel 中是否包含 id 字段 */
const editingRecord = ref<any>(null)

const formColumns = computed(() =>
  props.columns.filter((c) => !c.hideInForm && c.formType)
)

function buildEmptyForm(): Record<string, any> {
  const model: Record<string, any> = {}
  for (const c of formColumns.value) {
    const dv = c.formDefaultValue !== undefined ? c.formDefaultValue : undefined
    setNestedValue(model, c.field, dv)
  }
  return model
}

function fillFormFromRecord(record: any) {
  const model: Record<string, any> = {}
  for (const c of formColumns.value) {
    setNestedValue(model, c.field, getNestedValue(record, c.field))
  }
  return model
}

function openCreate() {
  if (!hasPermission('create')) {
    message.warning('无新增权限')
    return
  }
  isEdit.value = false
  editingRecord.value = null
  // 重置 formModel
  Object.keys(formModel).forEach((k) => delete formModel[k])
  Object.assign(formModel, buildEmptyForm())
  formVisible.value = true
}

function openEdit(record: any) {
  if (!hasPermission('update')) {
    message.warning('无编辑权限')
    return
  }
  isEdit.value = true
  editingRecord.value = record
  Object.keys(formModel).forEach((k) => delete formModel[k])
  Object.assign(formModel, fillFormFromRecord(record))
  formVisible.value = true
}

async function handleSubmit() {
  // 表单校验
  try {
    if (formRef.value) {
      await formRef.value.validate()
    }
  } catch (e) {
    message.warning('请完善表单信息')
    return
  }
  formLoading.value = true
  try {
    const payload = JSON.parse(JSON.stringify(formModel))
    if (isEdit.value) {
      // 优先取 editingRecord 的 id；兜底从 formModel 取
      const id = (editingRecord.value && getRecordId(editingRecord.value)) ?? getRecordId(formModel) ?? formModel[idField.value]
      await props.apiFunc.update(id, payload)
      message.success('更新成功')
      emit('saved', 'update', payload)
    } else {
      await props.apiFunc.create(payload)
      message.success('新增成功')
      emit('saved', 'create', payload)
    }
    formVisible.value = false
    await loadData()
  } catch (e) {
    console.error('[CrudTable] submit failed:', e)
    // 错误已由 request 拦截器统一提示
  } finally {
    formLoading.value = false
  }
}

/* ============ 删除 ============ */
function handleDelete(record: any) {
  if (!hasPermission('delete')) {
    message.warning('无删除权限')
    return
  }
  const id = getRecordId(record)
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除该条数据吗？（${idLabel}：${id}）`,
    okType: 'danger',
    async onOk() {
      try {
        await props.apiFunc.delete(id)
        message.success('删除成功')
        emit('deleted', id)
        await loadData()
      } catch (e) {
        console.error('[CrudTable] delete failed:', e)
      }
    }
  })
}

/* ============ 暴露方法 ============ */
defineExpose<CrudTableExpose>({
  refresh: loadData,
  reset: handleReset,
  openCreate,
  openEdit,
  getSelectedRows: () => selectedRows.value,
  clearSelected
})

/* ============ 工具函数：用于模板渲染 actions 按钮 ============ */
function isDividerAction(a: CrudAction): boolean {
  return !!a.divider
}

async function onActionClick(a: CrudAction, record: any) {
  try {
    await a.onClick(record)
  } catch (e) {
    console.error('[CrudTable] action onClick error:', e)
    message.error('操作失败')
  }
}

/* ============ 选项查找（用于 select/radio/checkbox 在表单中渲染选项） ============ */
function getFormOptions(col: CrudColumn): any[] {
  if (col.formOptions) return col.formOptions
  // 尝试 valueEnum 转换为 options
  if (col.valueEnum) {
    return Object.entries(col.valueEnum).map(([k, v]) => ({
      label: v.text,
      value: isNaN(Number(k)) ? k : Number(k),
      disabled: v.disabled
    }))
  }
  return []
}

/* ============ 初始化 ============ */
onMounted(() => {
  initSearchQuery()
  loadData()
})

// 监听 searchFields 变化时重新初始化（防止父组件异步加载选项后默认值丢失）
watch(
  () => props.searchFields,
  () => {
    // 仅初始化 query 中不存在的字段，避免覆盖用户输入
    for (const f of props.searchFields || []) {
      if (!(f.field in searchQuery)) {
        searchQuery[f.field] = f.defaultValue !== undefined ? f.defaultValue : undefined
      }
    }
  },
  { deep: true }
)
</script>

<template>
  <div class="crud-table">
    <!-- 搜索表单 -->
    <div v-if="searchFields && searchFields.length" class="vibe-card crud-search">
      <a-form layout="inline" :model="searchQuery" @submit.prevent="handleSearch">
        <template v-for="f in visibleSearchFields" :key="f.field">
          <a-form-item :label="f.label">
            <!-- input -->
            <a-input
              v-if="f.type === 'input'"
              v-model:value="searchQuery[f.field]"
              :placeholder="f.placeholder || `请输入${f.label}`"
              :allow-clear="f.allowClear !== false"
              :style="{ width: f.width ? `${f.width}px` : '180px' }"
              @press-enter="handleSearch"
            />
            <!-- select -->
            <a-select
              v-else-if="f.type === 'select'"
              v-model:value="searchQuery[f.field]"
              :placeholder="f.placeholder || `请选择${f.label}`"
              :allow-clear="f.allowClear !== false"
              :style="{ width: f.width ? `${f.width}px` : '160px' }"
              :options="f.options"
            />
            <!-- date -->
            <a-date-picker
              v-else-if="f.type === 'date'"
              v-model:value="searchQuery[f.field]"
              :placeholder="f.placeholder || `请选择${f.label}`"
              :style="{ width: f.width ? `${f.width}px` : '180px' }"
            />
            <!-- dateRange -->
            <a-range-picker
              v-else-if="f.type === 'dateRange'"
              v-model:value="searchQuery[f.field]"
              :style="{ width: f.width ? `${f.width}px` : '240px' }"
            />
            <!-- cascader -->
            <a-cascader
              v-else-if="f.type === 'cascader'"
              v-model:value="searchQuery[f.field]"
              :options="f.options"
              :placeholder="f.placeholder || `请选择${f.label}`"
              :style="{ width: f.width ? `${f.width}px` : '200px' }"
            />
            <!-- treeSelect -->
            <a-tree-select
              v-else-if="f.type === 'treeSelect'"
              v-model:value="searchQuery[f.field]"
              :tree-data="f.options"
              :placeholder="f.placeholder || `请选择${f.label}`"
              :style="{ width: f.width ? `${f.width}px` : '200px' }"
              allow-clear
            />
            <!-- switch -->
            <a-switch
              v-else-if="f.type === 'switch'"
              v-model:checked="searchQuery[f.field]"
            />
          </a-form-item>
        </template>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">
              <template #icon><SearchOutlined /></template>
              查询
            </a-button>
            <a-button @click="handleReset">重置</a-button>
            <a-button v-if="hasMoreSearch" type="link" @click="toggleCollapse">
              {{ collapsed ? '展开' : '折叠' }}
              <DownOutlined v-if="collapsed" />
              <UpOutlined v-else />
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </div>

    <!-- 表格卡片 -->
    <div class="vibe-card crud-table-card">
      <!-- 工具栏 -->
      <div class="crud-toolbar">
        <div class="crud-title">
          <span v-if="title" class="title-text">{{ title }}</span>
          <span v-if="description" class="title-desc">{{ description }}</span>
        </div>
        <a-space>
          <a-button v-if="rowSelection && selectedRowKeys.length" danger @click="handleBatchDelete">
            <template #icon><DeleteOutlined /></template>
            批量删除（{{ selectedRowKeys.length }}）
          </a-button>
          <a-button @click="loadData">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <a-button v-if="hasPermission('create')" type="primary" @click="openCreate">
            <template #icon><PlusOutlined /></template>
            {{ createText }}
          </a-button>
          <slot name="toolbar" />
        </a-space>
      </div>

      <!-- 表格 -->
      <a-table
        :columns="tableColumns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="idField"
        :row-selection="rowSelectionConfig"
        :scroll="scrollX ? { x: scrollX } : undefined"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- 操作列 -->
          <template v-if="column.key === '__action__'">
            <a-space :size="4" wrap>
              <template v-for="(a, idx) in resolveRowActions(record)" :key="idx">
                <a-divider v-if="isDividerAction(a)" type="vertical" />
                <a
                  v-else
                  class="crud-action-link"
                  :class="{ danger: a.danger }"
                  @click="onActionClick(a, record)"
                >
                  <component :is="a.icon" v-if="a.icon" />
                  {{ a.label }}
                </a>
              </template>
            </a-space>
          </template>
          <!-- 单元格：valueEnum 渲染 Tag -->
          <template v-else-if="column.key && getColumnValueEnum(String(column.key))">
            <a-tag
              v-if="getValueEnumTag(String(column.key), record)"
              :color="getValueEnumTag(String(column.key), record)?.status"
            >
              {{ getValueEnumTag(String(column.key), record)?.text }}
            </a-tag>
            <span v-else>{{ renderCellTextByField(String(column.key), record) }}</span>
          </template>
          <!-- 单元格：默认文本 -->
          <template v-else>
            {{ renderCellTextByField(String(column.key), record) }}
          </template>
        </template>

        <!-- 自定义 empty -->
        <template #emptyText>
          <slot name="empty">
            <span class="crud-empty">暂无数据</span>
          </slot>
        </template>
      </a-table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="formVisible"
      :title="isEdit ? '编辑' : '新增'"
      :width="formWidth"
      :confirm-loading="formLoading"
      :mask-closable="false"
      @ok="handleSubmit"
    >
      <a-form ref="formRef" :model="formModel" layout="vertical">
        <a-row :gutter="16">
          <a-col
            v-for="col in formColumns"
            :key="col.field"
            :span="col.formSpan || 12"
          >
            <a-form-item
              :label="col.title"
              :name="col.field"
              :rules="col.formRules"
            >
              <!-- input -->
              <a-input
                v-if="!col.formType || col.formType === 'input'"
                v-model:value="formModel[col.field]"
                :placeholder="`请输入${col.title}`"
                :disabled="isEdit && col.readonly"
              />
              <!-- inputNumber -->
              <a-input-number
                v-else-if="col.formType === 'inputNumber'"
                v-model:value="formModel[col.field]"
                :placeholder="`请输入${col.title}`"
                :disabled="isEdit && col.readonly"
                style="width: 100%"
              />
              <!-- inputPassword -->
              <a-input-password
                v-else-if="col.formType === 'inputPassword'"
                v-model:value="formModel[col.field]"
                :placeholder="`请输入${col.title}`"
                :disabled="isEdit && col.readonly"
              />
              <!-- textarea -->
              <a-textarea
                v-else-if="col.formType === 'textarea'"
                v-model:value="formModel[col.field]"
                :placeholder="`请输入${col.title}`"
                :rows="3"
                :disabled="isEdit && col.readonly"
              />
              <!-- select -->
              <a-select
                v-else-if="col.formType === 'select'"
                v-model:value="formModel[col.field]"
                :placeholder="`请选择${col.title}`"
                :options="getFormOptions(col)"
                :disabled="isEdit && col.readonly"
                allow-clear
              />
              <!-- date -->
              <a-date-picker
                v-else-if="col.formType === 'date'"
                v-model:value="formModel[col.field]"
                :placeholder="`请选择${col.title}`"
                :disabled="isEdit && col.readonly"
                style="width: 100%"
              />
              <!-- dateRange -->
              <a-range-picker
                v-else-if="col.formType === 'dateRange'"
                v-model:value="formModel[col.field]"
                :disabled="isEdit && col.readonly"
                style="width: 100%"
              />
              <!-- switch -->
              <a-switch
                v-else-if="col.formType === 'switch'"
                v-model:checked="formModel[col.field]"
                :disabled="isEdit && col.readonly"
              />
              <!-- radio -->
              <a-radio-group
                v-else-if="col.formType === 'radio'"
                v-model:value="formModel[col.field]"
                :disabled="isEdit && col.readonly"
              >
                <a-radio v-for="opt in getFormOptions(col)" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </a-radio>
              </a-radio-group>
              <!-- checkbox -->
              <a-checkbox-group
                v-else-if="col.formType === 'checkbox'"
                v-model:value="formModel[col.field]"
                :disabled="isEdit && col.readonly"
              >
                <a-checkbox v-for="opt in getFormOptions(col)" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </a-checkbox>
              </a-checkbox-group>
              <!-- cascader -->
              <a-cascader
                v-else-if="col.formType === 'cascader'"
                v-model:value="formModel[col.field]"
                :options="getFormOptions(col) || col.formOptions"
                :placeholder="`请选择${col.title}`"
                :disabled="isEdit && col.readonly"
              />
              <!-- treeSelect -->
              <a-tree-select
                v-else-if="col.formType === 'treeSelect'"
                v-model:value="formModel[col.field]"
                :tree-data="getFormOptions(col) || col.formOptions"
                :placeholder="`请选择${col.title}`"
                :disabled="isEdit && col.readonly"
                allow-clear
              />
              <!-- upload -->
              <a-upload-dragger
                v-else-if="col.formType === 'upload'"
                v-model:file-list="formModel[col.field]"
              >
                <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
              </a-upload-dragger>
            </a-form-item>
          </a-col>
        </a-row>
        <!-- 自定义表单插槽 -->
        <slot name="form-extra" :model="formModel" :is-edit="isEdit" />
      </a-form>
    </a-modal>
  </div>
</template>

<style lang="less" scoped>
.crud-table {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.crud-search {
  padding: 16px 20px;
}
.crud-table-card {
  padding: 16px 20px;
}
.crud-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.crud-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.title-text {
  font-size: 16px;
  font-weight: 600;
  color: @text-primary;
}
.title-desc {
  font-size: 12px;
  color: @text-tertiary;
}
.crud-action-link {
  cursor: pointer;
  color: @brand-primary;
  font-size: 13px;
  white-space: nowrap;
  &:hover {
    opacity: 0.85;
  }
  &.danger {
    color: @status-exception;
  }
}
.crud-empty {
  color: @text-tertiary;
}
</style>
