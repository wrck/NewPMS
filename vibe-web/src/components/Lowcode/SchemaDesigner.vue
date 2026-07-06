<script setup lang="ts">
/**
 * SchemaDesigner 低代码可视化设计器（spec 阶段三 - Task A2.1）
 *
 * 三栏布局：
 *   左：FieldPalette 字段库（10 类可拖拽字段）
 *   中：画布（拖拽生成字段、调整顺序、选中编辑）
 *   右：PropertyPanel 属性面板
 *
 * 支持设计器模式：form / list / tab / relation
 *   - form: 拖拽字段 → FieldConfig[]
 *   - list: 拖拽字段 → 列定义 + 搜索字段 + 表单字段
 *   - tab: 配置 Tab 列表
 *   - relation: 主从配置
 *
 * 通过 v-model:schemaJson 双向绑定，外部提交时直接保存到后端。
 */
import { ref, reactive, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  EyeOutlined,
  CodeOutlined,
  ImportOutlined,
  ExportOutlined,
  PlusOutlined,
  DeleteOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  CopyOutlined
} from '@ant-design/icons-vue'
import FieldPalette from './FieldPalette.vue'
import PropertyPanel from './PropertyPanel.vue'
import SchemaPreview from './SchemaPreview.vue'
import SchemaImporter from './SchemaImporter.vue'
import type {
  FieldConfig,
  FormSchema,
  ListSchema,
  TabSchema,
  RelationSchema,
  FieldType,
  DataType
} from '@/types/lowcode'
import {
  parseFormSchema,
  parseListSchema,
  parseTabSchema,
  parseRelationSchema,
  stringifyFormSchema,
  stringifyListSchema,
  stringifyTabSchema,
  stringifyRelationSchema
} from '@/types/lowcode'

type DesignerMode = 'form' | 'list' | 'tab' | 'relation'

interface Props {
  /** 当前模式 */
  mode: DesignerMode
  /** Schema JSON 字符串（v-model） */
  modelValue: string
  /** 是否禁用编辑 */
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

/* ============ 内部状态 ============ */
const fields = ref<FieldConfig[]>([])
const tabs = ref<TabSchema['tabs']>([])
const relation = ref<RelationSchema | null>(null)
const selectedFieldKey = ref<string>('')
const showPreview = ref(false)
const showJson = ref(false)
const showImporter = ref(false)
const jsonText = ref('')

/* ============ 元信息：基础配置 ============ */
const meta = reactive({
  title: '',
  description: '',
  apiUrl: '',
  pageSize: 10,
  rowKey: 'id',
  scrollX: 1200,
  layout: 'vertical' as 'horizontal' | 'vertical' | 'inline',
  // Tab
  tabPosition: 'top' as 'top' | 'right' | 'bottom' | 'left',
  type2: 'line' as 'line' | 'card'
})

/* ============ 当前选中字段 ============ */
const selectedField = computed<FieldConfig | null>(() => {
  if (!selectedFieldKey.value) return null
  return fields.value.find((f) => f.field === selectedFieldKey.value) || null
})

/* ============ 监听 modelValue：解析 Schema 到内部状态 ============ */
watch(
  () => props.modelValue,
  (val) => {
    if (!val) {
      fields.value = []
      tabs.value = []
      relation.value = null
      return
    }
    parseFromJson(val)
  },
  { immediate: true }
)

function parseFromJson(jsonStr: string) {
  const formSchema = parseFormSchema(jsonStr)
  if (formSchema) {
    fields.value = Object.entries(formSchema.properties || {}).map(([key, f]) => ({
      ...f,
      field: f.field || key
    }))
    meta.title = formSchema.title || ''
    meta.description = formSchema.description || ''
    meta.apiUrl = formSchema.apiUrl || ''
    meta.layout = formSchema.layout || 'vertical'
    return
  }
  const listSchema = parseListSchema(jsonStr)
  if (listSchema) {
    fields.value = listSchema.formFields || []
    meta.title = listSchema.title || ''
    meta.description = listSchema.description || ''
    meta.apiUrl = listSchema.apiUrl || ''
    meta.pageSize = listSchema.pageSize || 10
    meta.rowKey = listSchema.rowKey || 'id'
    meta.scrollX = listSchema.scrollX || 1200
    return
  }
  const tabSchema = parseTabSchema(jsonStr)
  if (tabSchema) {
    tabs.value = tabSchema.tabs || []
    meta.title = tabSchema.title || ''
    meta.description = tabSchema.description || ''
    meta.tabPosition = tabSchema.tabPosition || 'top'
    meta.type2 = tabSchema.type2 || 'line'
    return
  }
  const relSchema = parseRelationSchema(jsonStr)
  if (relSchema) {
    relation.value = relSchema
    meta.title = relSchema.title || ''
    meta.description = relSchema.description || ''
    return
  }
}

/* ============ 同步：内部状态 -> schemaJson ============ */
function syncToJson() {
  let jsonStr = ''
  if (props.mode === 'form') {
    const schema: FormSchema = {
      $schema: 'http://json-schema.org/draft-07/schema#',
      type: 'object',
      title: meta.title,
      description: meta.description,
      layout: meta.layout,
      apiUrl: meta.apiUrl,
      properties: {}
    }
    fields.value.forEach((f, idx) => {
      const key = f.field || `field_${idx}`
      schema.properties[key] = { ...f, order: idx }
    })
    jsonStr = stringifyFormSchema(schema)
  } else if (props.mode === 'list') {
    const schema: ListSchema = {
      $schema: 'http://json-schema.org/draft-07/schema#',
      type: 'list',
      title: meta.title,
      description: meta.description,
      apiUrl: meta.apiUrl,
      rowKey: meta.rowKey,
      pageSize: meta.pageSize,
      scrollX: meta.scrollX,
      columns: fields.value
        .filter((f) => !f.hideInTable)
        .map((f) => ({
          field: f.field,
          title: f.label,
          width: 120,
          ellipsis: true
        })),
      formFields: fields.value
    }
    jsonStr = stringifyListSchema(schema)
  } else if (props.mode === 'tab') {
    const schema: TabSchema = {
      $schema: 'http://json-schema.org/draft-07/schema#',
      type: 'tab',
      title: meta.title,
      description: meta.description,
      tabPosition: meta.tabPosition,
      type2: meta.type2,
      tabs: tabs.value
    }
    jsonStr = stringifyTabSchema(schema)
  } else if (props.mode === 'relation') {
    if (relation.value) {
      relation.value.title = meta.title
      relation.value.description = meta.description
      jsonStr = stringifyRelationSchema(relation.value)
    }
  }
  emit('update:modelValue', jsonStr)
}

/* ============ 拖拽：从字段库拖入画布 ============ */
const dragOverIdx = ref<number>(-1)

function onCanvasDragOver(event: DragEvent) {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'copy'
  }
}

function onCanvasDrop(event: DragEvent) {
  event.preventDefault()
  if (props.disabled) return
  if (!event.dataTransfer) return
  let payload: { type?: string; label?: string; dataType?: string; prefix?: string; source?: string } = {}
  try {
    const raw = event.dataTransfer.getData('application/json') || event.dataTransfer.getData('text/plain')
    payload = JSON.parse(raw)
  } catch {
    return
  }
  if (payload.source !== 'palette' || !payload.type) return

  // 新增字段
  const fieldType = payload.type as FieldType
  const dataType = (payload.dataType || 'string') as DataType
  const newField: FieldConfig = {
    field: `${payload.prefix || fieldType}_${Date.now().toString(36)}`,
    label: payload.label || fieldType,
    type: fieldType,
    dataType,
    required: false,
    placeholder: '',
    width: 12,
    order: fields.value.length
  }
  fields.value.push(newField)
  selectedFieldKey.value = newField.field
  syncToJson()
  message.success(`已添加字段：${newField.label}`)
}

/** 画布字段拖拽排序（同列表内） */
function onItemDragStart(event: DragEvent, idx: number) {
  if (!event.dataTransfer) return
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', String(idx))
}

function onItemDrop(event: DragEvent, targetIdx: number) {
  event.preventDefault()
  if (props.disabled) return
  if (!event.dataTransfer) return
  const sourceIdxStr = event.dataTransfer.getData('text/plain')
  const sourceIdx = parseInt(sourceIdxStr, 10)
  if (isNaN(sourceIdx) || sourceIdx === targetIdx) return
  if (sourceIdx < 0 || sourceIdx >= fields.value.length) return
  if (targetIdx < 0 || targetIdx >= fields.value.length) return
  const item = fields.value.splice(sourceIdx, 1)[0]
  fields.value.splice(targetIdx, 0, item)
  // 重新排序
  fields.value.forEach((f, idx) => (f.order = idx))
  syncToJson()
}

/* ============ 字段操作 ============ */
function selectField(field: FieldConfig) {
  selectedFieldKey.value = field.field
}

function updateField(updated: FieldConfig) {
  const idx = fields.value.findIndex((f) => f.field === selectedFieldKey.value)
  if (idx >= 0) {
    // 保留原 field 用于引用一致性（除非用户改名）
    fields.value[idx] = { ...updated, order: fields.value[idx].order }
    syncToJson()
  }
}

function deleteField() {
  if (!selectedFieldKey.value) return
  const idx = fields.value.findIndex((f) => f.field === selectedFieldKey.value)
  if (idx < 0) return
  fields.value.splice(idx, 1)
  selectedFieldKey.value = ''
  // 重新排序
  fields.value.forEach((f, i) => (f.order = i))
  syncToJson()
}

function moveField(idx: number, direction: -1 | 1) {
  const newIdx = idx + direction
  if (newIdx < 0 || newIdx >= fields.value.length) return
  const tmp = fields.value[idx]
  fields.value[idx] = fields.value[newIdx]
  fields.value[newIdx] = tmp
  fields.value.forEach((f, i) => (f.order = i))
  syncToJson()
}

function duplicateField(idx: number) {
  const src = fields.value[idx]
  const copy: FieldConfig = {
    ...src,
    field: `${src.field}_copy_${Date.now().toString(36)}`,
    label: `${src.label}_copy`,
    order: idx + 1
  }
  fields.value.splice(idx + 1, 0, copy)
  fields.value.forEach((f, i) => (f.order = i))
  selectedFieldKey.value = copy.field
  syncToJson()
}

/* ============ Tab 模式：增删 Tab ============ */
function addTab() {
  const idx = tabs.value.length + 1
  tabs.value.push({
    key: `tab_${Date.now().toString(36)}`,
    label: `标签 ${idx}`,
    contentType: 'list',
    bizType: '',
    order: idx - 1
  })
  syncToJson()
}

function removeTab(idx: number) {
  tabs.value.splice(idx, 1)
  tabs.value.forEach((t, i) => (t.order = i))
  syncToJson()
}

function moveTab(idx: number, direction: -1 | 1) {
  const newIdx = idx + direction
  if (newIdx < 0 || newIdx >= tabs.value.length) return
  const tmp = tabs.value[idx]
  tabs.value[idx] = tabs.value[newIdx]
  tabs.value[newIdx] = tmp
  tabs.value.forEach((t, i) => (t.order = i))
  syncToJson()
}

/* ============ Relation 模式：初始化 ============ */
function initRelation() {
  if (!relation.value) {
    relation.value = {
      $schema: 'http://json-schema.org/draft-07/schema#',
      type: 'relation',
      title: meta.title,
      master: {
        bizType: '',
        label: '主表',
        apiUrl: '',
        rowKey: 'id',
        columns: []
      },
      details: []
    }
    syncToJson()
  }
}

function addDetail() {
  if (!relation.value) {
    initRelation()
  }
  relation.value?.details.push({
    bizType: '',
    label: '从表',
    apiUrl: '',
    rowKey: 'id',
    foreignKey: 'masterId',
    columns: []
  })
  syncToJson()
}

function removeDetail(idx: number) {
  if (relation.value) {
    relation.value.details.splice(idx, 1)
    syncToJson()
  }
}

/* ============ 工具：JSON 预览 / 导入 / 导出 ============ */
function togglePreview() {
  syncToJson()
  showPreview.value = !showPreview.value
  if (showPreview.value) showJson.value = false
}

function toggleJson() {
  syncToJson()
  showJson.value = !showJson.value
  if (showJson.value) {
    jsonText.value = props.modelValue || ''
    showPreview.value = false
  }
}

function toggleImporter() {
  showImporter.value = true
}

function handleImport(payload: { schemaJson: string; templateId?: number }) {
  emit('update:modelValue', payload.schemaJson)
  parseFromJson(payload.schemaJson)
  message.success('导入成功')
}

/** 复制 JSON 到剪贴板 */
async function copyJson() {
  try {
    await navigator.clipboard.writeText(props.modelValue || '')
    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败，请手动选择')
  }
}

/** 下载 JSON 文件 */
function downloadJson() {
  const blob = new Blob([props.modelValue || ''], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${props.mode}_schema_${Date.now()}.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

/** 模式标识 */
const modeLabel = computed(() => {
  switch (props.mode) {
    case 'form':
      return '表单设计器'
    case 'list':
      return '列表设计器'
    case 'tab':
      return '标签页设计器'
    case 'relation':
      return '关联页设计器'
  }
  return ''
})
</script>

<template>
  <div class="schema-designer">
    <!-- 工具栏 -->
    <div class="designer-toolbar">
      <div class="toolbar-left">
        <span class="mode-label">{{ modeLabel }}</span>
      </div>
      <div class="toolbar-right">
        <a-button size="small" @click="toggleImporter" :disabled="disabled">
          <template #icon><ImportOutlined /></template>
          导入
        </a-button>
        <a-button size="small" @click="downloadJson" :disabled="!modelValue">
          <template #icon><ExportOutlined /></template>
          导出
        </a-button>
        <a-button size="small" :type="showJson ? 'primary' : 'default'" @click="toggleJson">
          <template #icon><CodeOutlined /></template>
          JSON
        </a-button>
        <a-button size="small" :type="showPreview ? 'primary' : 'default'" @click="togglePreview">
          <template #icon><EyeOutlined /></template>
          预览
        </a-button>
      </div>
    </div>

    <!-- 基础信息 -->
    <div class="designer-meta vibe-card">
      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item label="标题">
            <a-input v-model:value="meta.title" placeholder="Schema 标题" @change="syncToJson" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="描述">
            <a-input v-model:value="meta.description" placeholder="Schema 描述" @change="syncToJson" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="数据源 apiUrl">
            <a-input v-model:value="meta.apiUrl" placeholder="/customers 等" @change="syncToJson" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row v-if="mode === 'list'" :gutter="16">
        <a-col :span="6">
          <a-form-item label="主键字段">
            <a-input v-model:value="meta.rowKey" @change="syncToJson" />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="每页条数">
            <a-input-number v-model:value="meta.pageSize" :min="1" :max="200" style="width: 100%" @change="syncToJson" />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="表格滚动宽度">
            <a-input-number v-model:value="meta.scrollX" :min="0" :step="100" style="width: 100%" @change="syncToJson" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row v-if="mode === 'form'" :gutter="16">
        <a-col :span="6">
          <a-form-item label="表单布局">
            <a-select
              v-model:value="meta.layout"
              :options="[
                { value: 'horizontal', label: 'horizontal' },
                { value: 'vertical', label: 'vertical' },
                { value: 'inline', label: 'inline' }
              ]"
              @change="syncToJson"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row v-if="mode === 'tab'" :gutter="16">
        <a-col :span="6">
          <a-form-item label="Tab 位置">
            <a-select
              v-model:value="meta.tabPosition"
              :options="[
                { value: 'top', label: 'top' },
                { value: 'right', label: 'right' },
                { value: 'bottom', label: 'bottom' },
                { value: 'left', label: 'left' }
              ]"
              @change="syncToJson"
            />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="Tab 类型">
            <a-select
              v-model:value="meta.type2"
              :options="[
                { value: 'line', label: 'line' },
                { value: 'card', label: 'card' }
              ]"
              @change="syncToJson"
            />
          </a-form-item>
        </a-col>
      </a-row>
    </div>

    <!-- 主体三栏 -->
    <div class="designer-body" v-if="!showJson && !showPreview">
      <!-- 左：字段库（仅 form/list 模式） -->
      <FieldPalette v-if="mode === 'form' || mode === 'list'" class="designer-left" />

      <!-- 中：画布 -->
      <div class="designer-canvas">
        <!-- form / list 模式：字段画布 -->
        <template v-if="mode === 'form' || mode === 'list'">
          <div
            class="canvas-area"
            @dragover="onCanvasDragOver"
            @drop="onCanvasDrop"
          >
            <div v-if="fields.length === 0" class="canvas-empty">
              拖拽左侧字段到此处
            </div>
            <div
              v-for="(field, idx) in fields"
              :key="field.field"
              class="canvas-item"
              :class="{ selected: field.field === selectedFieldKey, hidden: field.hideInForm }"
              draggable="true"
              @click="selectField(field)"
              @dragstart="onItemDragStart($event, idx)"
              @drop="onItemDrop($event, idx)"
              @dragover.prevent="onCanvasDragOver"
            >
              <div class="item-info">
                <a-tag :color="field.required ? 'red' : 'blue'">{{ field.type }}</a-tag>
                <span class="item-label">{{ field.label }}</span>
                <span class="item-field">{{ field.field }}</span>
                <a-tag v-if="field.required" color="red" size="small">必填</a-tag>
              </div>
              <div class="item-actions">
                <a-button type="text" size="small" :disabled="idx === 0" @click.stop="moveField(idx, -1)">
                  <template #icon><ArrowUpOutlined /></template>
                </a-button>
                <a-button type="text" size="small" :disabled="idx === fields.length - 1" @click.stop="moveField(idx, 1)">
                  <template #icon><ArrowDownOutlined /></template>
                </a-button>
                <a-button type="text" size="small" @click.stop="duplicateField(idx)">
                  <template #icon><CopyOutlined /></template>
                </a-button>
                <a-button type="text" size="small" danger @click.stop="deleteField">
                  <template #icon><DeleteOutlined /></template>
                </a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- tab 模式：Tab 项管理 -->
        <template v-else-if="mode === 'tab'">
          <div class="canvas-area tab-canvas">
            <div class="canvas-toolbar">
              <a-button type="primary" size="small" @click="addTab">
                <template #icon><PlusOutlined /></template>
                新增标签页
              </a-button>
            </div>
            <div v-if="tabs.length === 0" class="canvas-empty">点击「新增标签页」开始</div>
            <div
              v-for="(tab, idx) in tabs"
              :key="tab.key"
              class="tab-item"
            >
              <div class="tab-info">
                <a-input v-model:value="tab.label" placeholder="标签名" style="width: 200px" @change="syncToJson" />
                <a-input v-model:value="tab.key" placeholder="key" style="width: 180px; margin-left: 8px" @change="syncToJson" />
                <a-input v-model:value="tab.bizType" placeholder="引用 bizType" style="width: 200px; margin-left: 8px" @change="syncToJson" />
                <a-select
                  v-model:value="tab.contentType"
                  :options="[
                    { value: 'list', label: 'list' },
                    { value: 'form', label: 'form' },
                    { value: 'relation', label: 'relation' },
                    { value: 'custom', label: 'custom' }
                  ]"
                  style="width: 120px; margin-left: 8px"
                  @change="syncToJson"
                />
              </div>
              <div class="item-actions">
                <a-button type="text" size="small" :disabled="idx === 0" @click="moveTab(idx, -1)">
                  <template #icon><ArrowUpOutlined /></template>
                </a-button>
                <a-button type="text" size="small" :disabled="idx === tabs.length - 1" @click="moveTab(idx, 1)">
                  <template #icon><ArrowDownOutlined /></template>
                </a-button>
                <a-button type="text" size="small" danger @click="removeTab(idx)">
                  <template #icon><DeleteOutlined /></template>
                </a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- relation 模式：主从配置 -->
        <template v-else-if="mode === 'relation'">
          <div class="canvas-area relation-canvas">
            <div v-if="!relation" class="canvas-empty">
              <a-button type="primary" @click="initRelation">初始化关联页</a-button>
            </div>
            <template v-else>
              <div class="relation-section">
                <div class="section-title">主表</div>
                <a-row :gutter="16">
                  <a-col :span="6">
                    <a-form-item label="实体编码">
                      <a-input v-model:value="relation.master.bizType" @change="syncToJson" />
                    </a-form-item>
                  </a-col>
                  <a-col :span="6">
                    <a-form-item label="显示名称">
                      <a-input v-model:value="relation.master.label" @change="syncToJson" />
                    </a-form-item>
                  </a-col>
                  <a-col :span="6">
                    <a-form-item label="数据源">
                      <a-input v-model:value="relation.master.apiUrl" @change="syncToJson" />
                    </a-form-item>
                  </a-col>
                  <a-col :span="6">
                    <a-form-item label="主键">
                      <a-input v-model:value="relation.master.rowKey" @change="syncToJson" />
                    </a-form-item>
                  </a-col>
                </a-row>
              </div>
              <div class="relation-section">
                <div class="section-title">
                  <span>从表</span>
                  <a-button type="primary" size="small" @click="addDetail">
                    <template #icon><PlusOutlined /></template>
                    新增从表
                  </a-button>
                </div>
                <div v-for="(detail, idx) in relation.details" :key="idx" class="detail-item">
                  <a-row :gutter="16">
                    <a-col :span="5">
                      <a-form-item label="实体编码">
                        <a-input v-model:value="detail.bizType" @change="syncToJson" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="5">
                      <a-form-item label="显示名称">
                        <a-input v-model:value="detail.label" @change="syncToJson" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="5">
                      <a-form-item label="数据源">
                        <a-input v-model:value="detail.apiUrl" @change="syncToJson" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="4">
                      <a-form-item label="外键">
                        <a-input v-model:value="detail.foreignKey" @change="syncToJson" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="3">
                      <a-form-item label="主键">
                        <a-input v-model:value="detail.rowKey" @change="syncToJson" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="2">
                      <a-form-item label=" ">
                        <a-button type="text" danger @click="removeDetail(idx)">
                          <template #icon><DeleteOutlined /></template>
                        </a-button>
                      </a-form-item>
                    </a-col>
                  </a-row>
                </div>
              </div>
            </template>
          </div>
        </template>
      </div>

      <!-- 右：属性面板（仅 form/list 模式有选中字段时显示） -->
      <PropertyPanel
        v-if="mode === 'form' || mode === 'list'"
        class="designer-right"
        :field="selectedField"
        :disabled="disabled"
        @update:field="updateField"
        @delete="deleteField"
      />
      <div v-else class="designer-right designer-right-empty">
        <a-empty description="该模式无属性面板" />
      </div>
    </div>

    <!-- JSON 预览 -->
    <div v-if="showJson" class="designer-json">
      <div class="json-toolbar">
        <span>JSON Schema</span>
        <a-space>
          <a-button size="small" @click="copyJson">
            <template #icon><CopyOutlined /></template>
            复制
          </a-button>
        </a-space>
      </div>
      <a-textarea
        v-model:value="jsonText"
        :rows="20"
        spellcheck="false"
        :style="{ fontFamily: 'monospace', fontSize: '12px' }"
        @change="emit('update:modelValue', jsonText)"
      />
    </div>

    <!-- 预览 -->
    <div v-if="showPreview" class="designer-preview">
      <SchemaPreview :schema-json="modelValue" :readonly="false" />
    </div>

    <!-- 导入弹窗 -->
    <SchemaImporter
      v-model:visible="showImporter"
      :template-type="mode === 'form' ? 'FORM' : mode === 'list' ? 'LIST' : mode === 'tab' ? 'TAB' : 'RELATION'"
      :title="`导入 ${modeLabel} Schema`"
      @import="handleImport"
    />
  </div>
</template>

<style lang="less" scoped>
.schema-designer {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
}
.designer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: @bg-container;
  border-radius: @radius-card;
}
.mode-label {
  font-weight: 600;
  color: @text-primary;
}
.designer-meta {
  padding: 12px 16px;
}
.designer-body {
  display: flex;
  flex: 1;
  min-height: 0;
  border: 1px solid @border-split;
  border-radius: @radius-card;
  overflow: hidden;
}
.designer-left {
  width: 240px;
  flex-shrink: 0;
}
.designer-canvas {
  flex: 1;
  min-width: 0;
  overflow: auto;
  background: @bg-layout;
}
.designer-right {
  width: 320px;
  flex-shrink: 0;
}
.designer-right-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: @bg-container;
  border-left: 1px solid @border-split;
}
.canvas-area {
  min-height: 100%;
  padding: 16px;
}
.canvas-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 240px;
  border: 2px dashed @border-color;
  border-radius: @radius-card;
  color: @text-tertiary;
  font-size: 14px;
  background: @bg-container;
}
.canvas-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  margin-bottom: 8px;
  background: @bg-container;
  border: 1px solid @border-color;
  border-radius: @radius-card;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    border-color: @brand-primary;
  }
  &.selected {
    border-color: @brand-primary;
    background: @brand-bg;
  }
  &.hidden {
    opacity: 0.5;
  }
}
.item-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}
.item-label {
  font-weight: 500;
  color: @text-primary;
}
.item-field {
  font-size: 12px;
  color: @text-tertiary;
  font-family: monospace;
}
.item-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}
.tab-canvas,
.relation-canvas {
  display: flex;
  flex-direction: column;
}
.canvas-toolbar {
  margin-bottom: 12px;
}
.tab-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  margin-bottom: 8px;
  background: @bg-container;
  border: 1px solid @border-color;
  border-radius: @radius-card;
}
.tab-info {
  display: flex;
  align-items: center;
}
.relation-section {
  margin-bottom: 24px;
}
.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: @brand-bg;
  border-radius: @radius-card;
  font-weight: 600;
  color: @brand-primary;
}
.detail-item {
  padding: 12px;
  margin-bottom: 12px;
  background: @bg-container;
  border: 1px solid @border-color;
  border-radius: @radius-card;
}
.designer-json,
.designer-preview {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.json-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: @bg-container;
  border-radius: @radius-card;
  margin-bottom: 8px;
}
</style>
