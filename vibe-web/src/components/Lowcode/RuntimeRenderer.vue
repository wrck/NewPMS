<script setup lang="ts">
/**
 * RuntimeRenderer 低代码运行时渲染器（spec 阶段三 - Task A3）
 *
 * 根据 schema.type 动态渲染：
 *   - form     -> 基于 a-form 渲染字段
 *   - list     -> 基于 a-table + 搜索表单 + 分页 + 操作按钮
 *   - tab      -> 基于 a-tabs 渲染嵌套
 *   - relation -> 主从关联（master 列表 + detail 子表）
 *
 * 数据源绑定：apiUrl + 字段映射 + 分页 + 操作按钮回调。
 * 视觉风格与 system/user.vue、device/ledger.vue 一致。
 */
import { ref, reactive, computed, onMounted, watch, defineAsyncComponent } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { UploadFile } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined
} from '@ant-design/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  FormSchema,
  ListSchema,
  TabSchema,
  RelationSchema,
  FieldConfig,
  ListColumn,
  ListSearchField,
  FieldOption
} from '@/types/lowcode'

/** 自身引用（用于 tab 嵌套）：使用 defineAsyncComponent 避免循环依赖 */
const RuntimeRendererSelf = defineAsyncComponent(() => import('./RuntimeRenderer.vue'))

interface Props {
  /** 已解析的 Schema 对象（form/list/tab/relation 之一） */
  schema: FormSchema | ListSchema | TabSchema | RelationSchema | null
  /** 业务实体类型（用于关联接口） */
  bizType?: string
  /** 业务实体 ID（详情态用） */
  bizId?: number | string
  /** 是否只读 */
  readonly?: boolean
  /** 自定义数据覆盖（不调用 apiUrl 时使用） */
  staticData?: unknown[]
  /** 表单模式初始数据 */
  initialData?: Record<string, unknown>
  /** 自定义按钮回调（key 为 action.callback，value 为函数） */
  actionHandlers?: Record<string, (record: Record<string, unknown>) => void | Promise<void>>
}

const props = withDefaults(defineProps<Props>(), {
  readonly: false
})

const emit = defineEmits<{
  (e: 'submit', data: Record<string, unknown>): void
  (e: 'action', payload: { type: string; record?: Record<string, unknown> }): void
}>()

/** Schema 类型 */
const schemaType = computed(() => props.schema?.type || null)

/* ============ Form 渲染 ============ */
const formModel = reactive<Record<string, unknown>>({})
const formRef = ref()

function initFormModel() {
  Object.keys(formModel).forEach((k) => delete formModel[k])
  const schema = props.schema as FormSchema | null
  if (!schema || schema.type !== 'object') return
  for (const [key, field] of Object.entries(schema.properties || {})) {
    if (props.initialData && props.initialData[key] !== undefined) {
      formModel[key] = props.initialData[key]
    } else if (field.defaultValue !== undefined) {
      formModel[key] = field.defaultValue
    } else {
      formModel[key] = undefined
    }
  }
}

/** 表单字段列表（按 order 排序） */
const formFields = computed<FieldConfig[]>(() => {
  const schema = props.schema as FormSchema | null
  if (!schema || schema.type !== 'object') return []
  return Object.entries(schema.properties || {})
    .map(([key, f]) => ({ ...f, field: f.field || key }))
    .sort((a, b) => (a.order ?? 0) - (b.order ?? 0))
})

/** 表单校验规则 */
function getFormRules(field: FieldConfig) {
  const rules: Array<Record<string, unknown>> = []
  if (field.required) {
    rules.push({
      required: true,
      message: field.placeholder || `请输入${field.label}`,
      trigger: ['blur', 'change']
    })
  }
  if (field.rules) {
    rules.push(...(field.rules as unknown as Array<Record<string, unknown>>))
  }
  return rules
}

/** 表单提交 */
async function handleFormSubmit() {
  try {
    if (formRef.value) {
      await formRef.value.validate()
    }
  } catch {
    message.warning('请完善表单信息')
    return
  }
  emit('submit', { ...formModel })
  // 若有 apiUrl，则自动提交
  const schema = props.schema as FormSchema | null
  if (schema?.apiUrl && schema.apiMethod) {
    try {
      if (schema.apiMethod === 'POST') {
        await http.post(schema.apiUrl, formModel)
      } else if (schema.apiMethod === 'PUT') {
        await http.put(schema.apiUrl, formModel)
      }
      message.success('提交成功')
    } catch (e) {
      // 错误已由 request 拦截器统一提示
    }
  }
}

/* ============ List 渲染 ============ */
const listLoading = ref(false)
const listData = ref<Record<string, unknown>[]>([])
const listPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`
})
const searchQuery = reactive<Record<string, string | number | boolean | undefined>>({})

const listSchema = computed<ListSchema | null>(() => {
  const s = props.schema
  if (s && s.type === 'list') return s as ListSchema
  return null
})

const listColumns = computed(() => {
  if (!listSchema.value) return []
  return listSchema.value.columns || []
})

const listSearchFields = computed<ListSearchField[]>(() => {
  if (!listSchema.value) return []
  return listSchema.value.searchFields || []
})

const listActions = computed(() => {
  if (!listSchema.value) return []
  return listSchema.value.actions || []
})

const tableColumns = computed(() => {
  const cols = listColumns.value.map((c: ListColumn) => ({
    title: c.title,
    dataIndex: c.field,
    key: c.field,
    width: c.width,
    align: c.align,
    ellipsis: c.ellipsis !== false,
    fixed: c.fixed,
    sorter: c.sortable
  }))
  // 操作列
  if (!props.readonly) {
    cols.push({
      title: '操作',
      dataIndex: '__action__',
      key: '__action__',
      width: 160,
      align: 'center' as const,
      ellipsis: false,
      fixed: 'right' as const,
      sorter: false
    })
  }
  return cols
})

const rowKeyField = computed(() => listSchema.value?.rowKey || 'id')

/** 加载列表数据 */
async function loadListData() {
  const schema = listSchema.value
  if (!schema) return
  if (props.staticData && props.staticData.length >= 0) {
    listData.value = [...(props.staticData as Record<string, unknown>[])]
    listPagination.total = listData.value.length
    return
  }
  if (!schema.apiUrl) {
    listData.value = []
    return
  }
  listLoading.value = true
  try {
    const params: Record<string, unknown> = {
      page: listPagination.current,
      size: listPagination.pageSize,
      ...searchQuery
    }
    const res = (await http.get(schema.apiUrl, params)) as unknown as PageResult<
      Record<string, unknown>
    >
    listData.value = res?.records || []
    listPagination.total = res?.total || 0
  } catch (e) {
    console.error('[RuntimeRenderer] load list failed:', e)
  } finally {
    listLoading.value = false
  }
}

function handleListSearch() {
  listPagination.current = 1
  loadListData()
}

function handleListReset() {
  Object.keys(searchQuery).forEach((k) => delete searchQuery[k])
  listPagination.current = 1
  loadListData()
}

function handleTableChange(pag: { current?: number; pageSize?: number }) {
  if (pag) {
    listPagination.current = pag.current || 1
    listPagination.pageSize = pag.pageSize || listPagination.pageSize
  }
  loadListData()
}

/** 行操作按钮 */
function resolveRowActions(record: Record<string, unknown>) {
  const actions = listActions.value.filter((a) => a.type !== 'create')
  return actions
}

/** 触发操作 */
function triggerAction(type: string, record?: Record<string, unknown>) {
  emit('action', { type, record })
  // 查找自定义回调
  if (props.actionHandlers) {
    const cbKey = type
    if (props.actionHandlers[cbKey]) {
      props.actionHandlers[cbKey](record || {})
    }
  }
}

/** 渲染单元格文本（接受 a-table 注入的 ColumnType<any>） */
function renderCellText(col: unknown, record: Record<string, unknown>): string {
  const colDef = col as ListColumn
  const colObj = col as { dataIndex?: string; key?: string }
  const field = colDef.field || colObj.dataIndex || ''
  if (!field) return ''
  const value = record[field]
  if (value == null) return ''
  if (colDef.valueEnum) {
    const item = colDef.valueEnum[String(value)]
    return item?.text ?? String(value)
  }
  return String(value)
}

/** 渲染 Tag（接受 a-table 注入的 ColumnType<any>） */
function renderTag(col: unknown, record: Record<string, unknown>) {
  const colDef = col as ListColumn
  if (!colDef.valueEnum) return undefined
  const colObj = col as { dataIndex?: string; key?: string }
  const field = colDef.field || colObj.dataIndex || ''
  const value = record[field]
  if (value == null) return undefined
  return colDef.valueEnum[String(value)]
}

/* ============ Tab 渲染 ============ */
const tabSchema = computed<TabSchema | null>(() => {
  const s = props.schema
  if (s && s.type === 'tab') return s as TabSchema
  return null
})

const tabItems = computed(() => {
  if (!tabSchema.value) return []
  return tabSchema.value.tabs || []
})

/** 根据 contentType 异步加载子组件 */
// 使用上方声明的 RuntimeRendererSelf 引用（用于 tab 嵌套）

/* ============ Relation 渲染 ============ */
const relationSchema = computed<RelationSchema | null>(() => {
  const s = props.schema
  if (s && s.type === 'relation') return s as RelationSchema
  return null
})

const relationMasterData = ref<Record<string, unknown>[]>([])
const relationDetailDataMap = ref<Record<string, Record<string, unknown>[]>>({})
const relationMasterLoading = ref(false)
const relationActiveKey = ref<string>('')
const relationSelectedMaster = ref<Record<string, unknown> | null>(null)

async function loadRelationMaster() {
  const schema = relationSchema.value
  if (!schema || !schema.master.apiUrl) return
  relationMasterLoading.value = true
  try {
    const res = (await http.get(schema.master.apiUrl, {
      page: 1,
      size: 50
    })) as unknown as PageResult<Record<string, unknown>>
    relationMasterData.value = res?.records || []
    if (relationMasterData.value.length > 0) {
      relationSelectedMaster.value = relationMasterData.value[0]
      loadRelationDetails(relationMasterData.value[0])
    }
  } catch (e) {
    console.error('[RuntimeRenderer] load relation master failed:', e)
  } finally {
    relationMasterLoading.value = false
  }
}

async function loadRelationDetails(masterRow: Record<string, unknown>) {
  const schema = relationSchema.value
  if (!schema || !masterRow) return
  const masterId = masterRow[schema.master.rowKey || 'id']
  for (const detail of schema.details) {
    if (!detail.apiUrl) continue
    try {
      const params: Record<string, unknown> = { page: 1, size: 50 }
      if (detail.foreignKey) {
        params[detail.foreignKey] = masterId as unknown
      }
      const res = (await http.get(detail.apiUrl, params)) as unknown as PageResult<
        Record<string, unknown>
      >
      relationDetailDataMap.value[detail.bizType] = res?.records || []
    } catch (e) {
      console.error('[RuntimeRenderer] load relation detail failed:', e)
    }
  }
}

function handleRelationMasterSelect(record: Record<string, unknown>) {
  relationSelectedMaster.value = record
  loadRelationDetails(record)
}

/* ============ 工具：选项查找 ============ */
function getFieldOptions(field: FieldConfig): FieldOption[] {
  return field.options || []
}

/* ============ 工具：表单字段值访问器（避免 unknown 类型不匹配） ============ */
function getStrValue(field: FieldConfig): string | undefined {
  const v = formModel[field.field]
  return typeof v === 'string' ? v : v == null ? undefined : String(v)
}
function getNumValue(field: FieldConfig): number | undefined {
  const v = formModel[field.field]
  return typeof v === 'number' ? v : v == null ? undefined : Number(v)
}
function getBoolValue(field: FieldConfig): boolean | undefined {
  const v = formModel[field.field]
  return typeof v === 'boolean' ? v : v == null ? undefined : Boolean(v)
}
function getArrValue(field: FieldConfig): unknown[] | undefined {
  const v = formModel[field.field]
  return Array.isArray(v) ? v : v == null ? undefined : []
}
function setFieldValue(field: FieldConfig, value: unknown): void {
  formModel[field.field] = value
}

/** 搜索表单字段值访问器（string 类型，满足 a-input/a-select/a-date-picker） */
function getSearchStr(field: string): string | undefined {
  const v = searchQuery[field]
  if (v == null) return undefined
  return typeof v === 'string' ? v : String(v)
}

/** 搜索表单字段值访问器（number 类型，满足 a-input-number） */
function getSearchNum(field: string): number | undefined {
  const v = searchQuery[field]
  if (v == null) return undefined
  return typeof v === 'number' ? v : typeof v === 'string' && v !== '' ? Number(v) : undefined
}

/** 设置搜索表单字段值（统一存储为 unknown） */
function setSearchValue(field: string, value: unknown): void {
  if (value == null || value === '') {
    delete searchQuery[field]
  } else {
    searchQuery[field] = value as string | number | boolean
  }
}

/** 文件字段访问器：返回符合 antd UploadFile 形状（uid/name 必填，status 为受限联合类型） */
function getFileListValue(field: FieldConfig): UploadFile[] | undefined {
  const v = formModel[field.field]
  if (Array.isArray(v)) {
    // 规范化数组项：补全 uid/name 必填字段，规范 status 取值
    return (v as Array<Record<string, unknown>>).map((item) => {
      const rawStatus = item?.status != null ? String(item.status) : 'done'
      const validStatus: 'error' | 'success' | 'done' | 'uploading' | 'removed' = ['error', 'success', 'done', 'uploading', 'removed'].includes(rawStatus)
        ? (rawStatus as 'error' | 'success' | 'done' | 'uploading' | 'removed')
        : 'done'
      return {
        uid: String(item?.uid ?? item?.id ?? Math.random().toString(36).slice(2)),
        name: String(item?.name ?? item?.fileName ?? 'file'),
        url: item?.url != null ? String(item.url) : undefined,
        status: validStatus,
        percent: typeof item?.percent === 'number' ? item.percent : undefined,
        thumbUrl: item?.thumbUrl != null ? String(item.thumbUrl) : undefined
      } as UploadFile
    })
  }
  return undefined
}

/** Cascader 字段访问器：返回 (string | number)[] */
function getCascaderValue(field: FieldConfig): Array<string | number> | undefined {
  const v = formModel[field.field]
  if (Array.isArray(v)) {
    return v as Array<string | number>
  }
  return undefined
}

/** 将 ListSchema 的列定义转换为 a-table 接受的列对象 */
function toTableColumns(cols: ListColumn[] | undefined): Array<{
  title: string
  dataIndex: string
  key: string
  width?: string | number
  align?: 'left' | 'center' | 'right'
  ellipsis?: boolean
  fixed?: 'left' | 'right'
}> {
  if (!cols) return []
  return cols.map((c) => ({
    title: c.title,
    dataIndex: c.field,
    key: c.field,
    width: c.width,
    align: c.align,
    ellipsis: c.ellipsis,
    fixed: c.fixed
  }))
}

/** 关联页主表行点击事件 */
function onRelationRowClick(record: Record<string, unknown>): { onClick: () => void } {
  return {
    onClick: () => handleRelationMasterSelect(record)
  }
}

/** 关联页主表行 class */
function relationRowClass(record: Record<string, unknown>): string {
  if (!relationSelectedMaster.value || !relationSchema.value) return ''
  const key = relationSchema.value.master.rowKey || 'id'
  if (record[key] === relationSelectedMaster.value[key]) {
    return 'row-selected'
  }
  return ''
}

/* ============ 监听 ============ */
watch(
  () => props.schema,
  () => {
    if (schemaType.value === 'object') {
      initFormModel()
    } else if (schemaType.value === 'list') {
      listPagination.current = 1
      loadListData()
    } else if (schemaType.value === 'relation') {
      loadRelationMaster()
    }
  },
  { immediate: false }
)

watch(
  () => props.initialData,
  () => {
    if (schemaType.value === 'object') {
      initFormModel()
    }
  },
  { deep: true }
)

onMounted(() => {
  if (schemaType.value === 'object') {
    initFormModel()
  } else if (schemaType.value === 'list') {
    loadListData()
  } else if (schemaType.value === 'relation') {
    loadRelationMaster()
  }
})

defineExpose({
  refreshList: loadListData,
  refreshRelation: loadRelationMaster,
  submitForm: handleFormSubmit
})
</script>

<template>
  <div class="runtime-renderer">
    <!-- ============ 表单 ============ -->
    <a-form
      v-if="schemaType === 'object'"
      ref="formRef"
      :model="formModel"
      :layout="(schema as FormSchema).layout || 'vertical'"
      @submit.prevent="handleFormSubmit"
    >
      <a-row :gutter="16">
        <a-col
          v-for="field in formFields"
          :key="field.field"
          :span="field.width || 12"
          v-show="!field.hideInForm"
        >
          <a-form-item
            :label="field.label"
            :name="field.field"
            :rules="getFormRules(field)"
          >
            <!-- input -->
            <a-input
              v-if="field.type === 'input'"
              :value="getStrValue(field)"
              :placeholder="field.placeholder || `请输入${field.label}`"
              :readonly="field.readonly || readonly"
              allow-clear
              @update:value="(v) => setFieldValue(field, v)"
            />
            <!-- textarea -->
            <a-textarea
              v-else-if="field.type === 'textarea'"
              :value="getStrValue(field)"
              :placeholder="field.placeholder || `请输入${field.label}`"
              :rows="3"
              :disabled="field.readonly || readonly"
              @update:value="(v) => setFieldValue(field, v)"
            />
            <!-- number -->
            <a-input-number
              v-else-if="field.type === 'number'"
              :value="getNumValue(field)"
              :placeholder="field.placeholder || `请输入${field.label}`"
              :disabled="field.readonly || readonly"
              style="width: 100%"
              @update:value="(v) => setFieldValue(field, v)"
            />
            <!-- select -->
            <a-select
              v-else-if="field.type === 'select'"
              :value="getStrValue(field)"
              :options="getFieldOptions(field)"
              :placeholder="field.placeholder || `请选择${field.label}`"
              :disabled="field.readonly || readonly"
              allow-clear
              @update:value="(v) => setFieldValue(field, v)"
            />
            <!-- date -->
            <a-date-picker
              v-else-if="field.type === 'date'"
              :value="getStrValue(field)"
              :placeholder="field.placeholder || `请选择${field.label}`"
              :disabled="field.readonly || readonly"
              style="width: 100%"
              @update:value="(v) => setFieldValue(field, v)"
            />
            <!-- switch -->
            <a-switch
              v-else-if="field.type === 'switch'"
              :checked="!!getBoolValue(field)"
              :disabled="field.readonly || readonly"
              @update:checked="(v) => setFieldValue(field, v)"
            />
            <!-- cascader -->
            <a-cascader
              v-else-if="field.type === 'cascader'"
              :value="getCascaderValue(field)"
              :options="getFieldOptions(field)"
              :placeholder="field.placeholder || `请选择${field.label}`"
              :disabled="field.readonly || readonly"
              change-on-select
              @update:value="(v) => setFieldValue(field, v)"
            />
            <!-- relSelect（先用 select 兜底） -->
            <a-select
              v-else-if="field.type === 'relSelect'"
              :value="getStrValue(field)"
              :placeholder="field.placeholder || `请选择${field.label}`"
              :disabled="field.readonly || readonly"
              allow-clear
              show-search
              @update:value="(v) => setFieldValue(field, v)"
            />
            <!-- richText（先用 textarea 兜底，不引入 wangeditor） -->
            <a-textarea
              v-else-if="field.type === 'richText'"
              :value="getStrValue(field)"
              :rows="6"
              :placeholder="field.placeholder || `请输入${field.label}`"
              :disabled="field.readonly || readonly"
              @update:value="(v) => setFieldValue(field, v)"
            />
            <!-- file -->
            <a-upload-dragger
              v-else-if="field.type === 'file'"
              :file-list="getFileListValue(field)"
              :disabled="field.readonly || readonly"
              @update:file-list="(v) => setFieldValue(field, v)"
            >
              <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
            </a-upload-dragger>
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item v-if="!readonly">
        <a-space>
          <a-button type="primary" html-type="submit">提交</a-button>
          <a-button @click="initFormModel">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <!-- ============ 列表 ============ -->
    <div v-else-if="schemaType === 'list'" class="list-mode">
      <!-- 搜索 -->
      <div v-if="listSearchFields.length" class="vibe-card search-card">
        <a-form layout="inline" :model="searchQuery" @submit.prevent="handleListSearch">
          <a-form-item
            v-for="f in listSearchFields"
            :key="f.field"
            :label="f.label"
          >
            <a-input
              v-if="f.type === 'input'"
              :value="getSearchStr(f.field)"
              :placeholder="f.placeholder || f.label"
              allow-clear
              style="width: 180px"
              @update:value="(v) => setSearchValue(f.field, v)"
              @press-enter="handleListSearch"
            />
            <a-select
              v-else-if="f.type === 'select'"
              :value="getSearchStr(f.field)"
              :options="f.options"
              :placeholder="f.placeholder || f.label"
              allow-clear
              style="width: 160px"
              @update:value="(v) => setSearchValue(f.field, v)"
            />
            <a-date-picker
              v-else-if="f.type === 'date'"
              :value="getSearchStr(f.field)"
              :placeholder="f.placeholder || f.label"
              style="width: 180px"
              @update:value="(v) => setSearchValue(f.field, v)"
            />
            <a-input-number
              v-else-if="f.type === 'number'"
              :value="getSearchNum(f.field)"
              :placeholder="f.placeholder || f.label"
              style="width: 160px"
              @update:value="(v) => setSearchValue(f.field, v)"
            />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit">
                <template #icon><SearchOutlined /></template>
                查询
              </a-button>
              <a-button @click="handleListReset">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- 表格 -->
      <div class="vibe-card table-card">
        <div class="table-toolbar">
          <div class="table-title">
            <span v-if="listSchema?.title" class="title-text">{{ listSchema.title }}</span>
            <span v-if="listSchema?.description" class="title-desc">{{ listSchema.description }}</span>
          </div>
          <a-space>
            <a-button @click="loadListData">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
            <a-button
              v-if="listActions.some(a => a.type === 'create') && !readonly"
              type="primary"
              @click="triggerAction('create')"
            >
              <template #icon><PlusOutlined /></template>
              新增
            </a-button>
          </a-space>
        </div>
        <a-table
          :columns="tableColumns"
          :data-source="listData"
          :loading="listLoading"
          :pagination="listPagination"
          :row-key="rowKeyField"
          :scroll="listSchema?.scrollX ? { x: listSchema.scrollX } : undefined"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === '__action__'">
              <a-space size="small" wrap>
                <a
                  v-for="a in resolveRowActions(record)"
                  :key="a.type"
                  :class="{ 'danger-link': a.danger }"
                  @click="triggerAction(a.type, record)"
                >
                  <EditOutlined v-if="a.icon === 'EditOutlined'" />
                  <DeleteOutlined v-else-if="a.icon === 'DeleteOutlined'" />
                  {{ a.label }}
                </a>
              </a-space>
            </template>
            <template v-else>
              <StatusTag
                v-if="renderTag(column, record)"
                :color="renderTag(column, record)?.status"
              >
                {{ renderTag(column, record)?.text }}
              </StatusTag>
              <span v-else>{{ renderCellText(column, record) }}</span>
            </template>
          </template>
          <template #emptyText>
            <EmptyState description="暂无数据" />
          </template>
        </a-table>
      </div>
    </div>

    <!-- ============ 标签页 ============ -->
    <a-tabs
      v-else-if="schemaType === 'tab'"
      :tab-position="tabSchema?.tabPosition || 'top'"
      :type="tabSchema?.type2 || 'line'"
      v-model:active-key="relationActiveKey"
    >
      <a-tab-pane
        v-for="tab in tabItems"
        :key="tab.key"
        :tab="tab.label"
        :disabled="tab.disabled"
        :force-render="tab.forceRender"
      >
        <RuntimeRendererSelf :biz-type="tab.bizType" :schema="null" />
        <div class="tab-placeholder">
          引用配置：<strong>{{ tab.bizType || '未指定' }}</strong>
          <span v-if="tab.contentType">（{{ tab.contentType }}）</span>
        </div>
      </a-tab-pane>
    </a-tabs>

    <!-- ============ 关联页 ============ -->
    <div v-else-if="schemaType === 'relation'" class="relation-mode">
      <a-row :gutter="16">
        <a-col :span="12">
          <div class="vibe-card relation-master">
            <div class="card-header">
              <span class="title">{{ relationSchema?.master.label || '主表' }}</span>
            </div>
            <a-table
              :columns="toTableColumns(relationSchema?.master.columns)"
              :data-source="relationMasterData"
              :loading="relationMasterLoading"
              :row-key="relationSchema?.master.rowKey || 'id'"
              size="small"
              :pagination="false"
              :custom-row="onRelationRowClick"
              :row-class-name="relationRowClass"
            />
          </div>
        </a-col>
        <a-col :span="12">
          <a-tabs v-if="relationSchema && relationSchema.details.length" v-model:active-key="relationActiveKey">
            <a-tab-pane
              v-for="detail in relationSchema.details"
              :key="detail.bizType"
              :tab="detail.label || detail.bizType"
            >
              <a-table
                :columns="toTableColumns(detail.columns)"
                :data-source="relationDetailDataMap[detail.bizType] || []"
                :row-key="detail.rowKey || 'id'"
                size="small"
                :pagination="false"
              >
                <template #emptyText>
                  <EmptyState description="暂无关联数据" size="compact" />
                </template>
              </a-table>
            </a-tab-pane>
          </a-tabs>
          <EmptyState v-else description="未配置从表" />
        </a-col>
      </a-row>
    </div>

    <!-- ============ 无 Schema ============ -->
    <EmptyState v-else description="未加载到有效 Schema" />
  </div>
</template>

<style lang="less" scoped>
.runtime-renderer {
  width: 100%;
}
.list-mode {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.search-card {
  padding: 16px 20px;
}
.table-card {
  padding: 16px 20px;
}
.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.table-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
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
.danger-link {
  color: @status-exception;
  cursor: pointer;
}
.tab-placeholder {
  padding: 16px;
  color: @text-tertiary;
  font-size: 13px;
}
.relation-mode {
  .relation-master {
    padding: 12px 16px;
  }
  .card-header {
    margin-bottom: 8px;
    .title {
      font-weight: 600;
      color: @text-primary;
    }
  }
  :deep(.row-selected) {
    background: @brand-bg;
  }
}
</style>
