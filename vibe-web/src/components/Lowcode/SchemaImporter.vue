<script setup lang="ts">
/**
 * SchemaImporter 低代码 JSON 导入校验组件（spec 阶段三 - Task A2.5）
 *
 * 功能：
 *   1. 提供模板选择下拉（按 templateType 过滤），选择后自动填充 schemaJson
 *   2. 文本域粘贴 JSON
 *   3. 上传 JSON 文件
 *   4. 校验 JSON 是否合法（手写校验，不依赖 ajv）
 *   5. 校验通过后向父组件 emit('import', { schemaJson, templateId })
 *
 * 不引入 ajv：项目 package.json 未安装 ajv，且后端会再次校验 Draft 7。
 */
import { ref, watch, onMounted, computed } from 'vue'
import { Modal, Input, Select, Upload, message, Alert } from 'ant-design-vue'
import { UploadOutlined, FileTextOutlined } from '@ant-design/icons-vue'
import type { UploadFile } from 'ant-design-vue'
import { pageTemplates } from '@/api/lowcode'
import type { LowcodeTemplateVO, LowcodeTemplateType } from '@/types/lowcode'
import type { PageResult } from '@/types/api'

interface Props {
  visible: boolean
  /** 模板类型过滤（FORM/LIST/TAB/RELATION） */
  templateType?: LowcodeTemplateType
  /** 弹窗标题 */
  title?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '导入 Schema'
})

const emit = defineEmits<{
  (e: 'update:visible', visible: boolean): void
  (e: 'import', payload: { schemaJson: string; templateId?: number }): void
}>()

/** 内部状态 */
const schemaJson = ref('')
const selectedTemplateId = ref<number | undefined>(undefined)
const templates = ref<LowcodeTemplateVO[]>([])
const templatesLoading = ref(false)
const validating = ref(false)
const validationErrors = ref<string[]>([])

/** 加载模板列表（按 templateType 过滤） */
async function loadTemplates() {
  templatesLoading.value = true
  try {
    const res = (await pageTemplates({
      page: 1,
      size: 100,
      templateType: props.templateType,
      status: 1
    })) as unknown as PageResult<LowcodeTemplateVO>
    templates.value = res?.records || []
  } catch (e) {
    console.error('[SchemaImporter] load templates failed:', e)
  } finally {
    templatesLoading.value = false
  }
}

/** 模板选择变更：填充 schemaJson */
function handleTemplateChange(value: unknown): void {
  if (value == null) {
    return
  }
  // 选择模板时只接受 number 类型的 id
  if (typeof value !== 'number') {
    return
  }
  const templateId = value
  selectedTemplateId.value = templateId
  const tpl = templates.value.find((t) => t.id === templateId)
  if (tpl) {
    schemaJson.value = tpl.schemaJson
    validate()
  }
}

/** 上传文件处理：读取文件内容到 schemaJson */
function handleUpload(file: File): boolean {
  const reader = new FileReader()
  reader.onload = (e) => {
    schemaJson.value = String(e.target?.result || '')
    validate()
  }
  reader.onerror = () => {
    message.error('文件读取失败')
  }
  reader.readAsText(file)
  return false
}

/** 上传组件 before-upload 钩子 */
function beforeUpload(file: UploadFile | File): boolean {
  handleUpload(file as File)
  return false
}

/** select 过滤函数 */
function filterOption(input: string, option: { label?: string } | undefined): boolean {
  return (option?.label || '').toLowerCase().includes(input.toLowerCase())
}

/** 校验 JSON 合法性（手写） */
function validate(): boolean {
  validating.value = true
  validationErrors.value = []
  const text = schemaJson.value.trim()
  if (!text) {
    validationErrors.value = ['JSON 内容不能为空']
    validating.value = false
    return false
  }
  try {
    const parsed = JSON.parse(text)
    if (typeof parsed !== 'object' || parsed === null) {
      validationErrors.value = ['JSON 必须是对象']
      validating.value = false
      return false
    }
    // 检查 type 字段（根据 templateType 期望不同 type）
    if (props.templateType === 'LIST' && parsed.type !== 'list') {
      validationErrors.value = [`期望 type="list"，实际 type="${parsed.type || ''}"`]
      validating.value = false
      return false
    }
    if (props.templateType === 'TAB' && parsed.type !== 'tab') {
      validationErrors.value = [`期望 type="tab"，实际 type="${parsed.type || ''}"`]
      validating.value = false
      return false
    }
    if (props.templateType === 'RELATION' && parsed.type !== 'relation') {
      validationErrors.value = [`期望 type="relation"，实际 type="${parsed.type || ''}"`]
      validating.value = false
      return false
    }
    // FORM 期望 type="object"
    if (props.templateType === 'FORM' && parsed.type !== 'object') {
      validationErrors.value = [`期望 type="object"，实际 type="${parsed.type || ''}"`]
      validating.value = false
      return false
    }
    validating.value = false
    return true
  } catch (e) {
    validationErrors.value = [`JSON 解析失败：${(e as Error).message}`]
    validating.value = false
    return false
  }
}

/** 确认导入 */
function handleOk() {
  if (!validate()) {
    message.warning('请检查 JSON 格式')
    return
  }
  emit('import', {
    schemaJson: schemaJson.value,
    templateId: selectedTemplateId.value
  })
  emit('update:visible', false)
  // 重置
  schemaJson.value = ''
  selectedTemplateId.value = undefined
  validationErrors.value = []
}

/** 取消 */
function handleCancel() {
  emit('update:visible', false)
  schemaJson.value = ''
  selectedTemplateId.value = undefined
  validationErrors.value = []
}

/** 监听 visible 打开时加载模板列表 */
watch(
  () => props.visible,
  (val) => {
    if (val) {
      loadTemplates()
    }
  }
)

onMounted(() => {
  if (props.visible) {
    loadTemplates()
  }
})

const selectOptions = computed(() =>
  templates.value.map((t) => ({
    value: t.id,
    label: `${t.templateName}（${t.templateCode}）`
  }))
)

/** 期望的 Schema type 提示文案 */
const expectedTypeMessage = computed(() => {
  if (!props.templateType) return ''
  const expected = props.templateType === 'FORM' ? 'object' : props.templateType.toLowerCase()
  return `期望 schema type="${expected}"`
})
</script>

<template>
  <Modal
    :open="visible"
    :title="title"
    width="720px"
    :ok-text="validating ? '校验中...' : '导入'"
    :ok-button-props="{ disabled: validating }"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <Alert
      v-if="!templateType"
      type="info"
      message="JSON 须为合法对象，符合后端 JSON Schema Draft 7 规范；后端会再次校验。"
      show-icon
      style="margin-bottom: 12px"
    />

    <Alert
      v-if="templateType"
      type="info"
      :message="expectedTypeMessage"
      show-icon
      style="margin-bottom: 12px"
    />

    <!-- 模板选择 -->
    <div class="import-section">
      <div class="section-label">
        <FileTextOutlined />
        <span>从模板选择</span>
      </div>
      <Select
        v-model:value="selectedTemplateId"
        :options="selectOptions"
        :loading="templatesLoading"
        placeholder="选择模板后自动填充 Schema"
        allow-clear
        show-search
        :filter-option="filterOption"
        style="width: 100%"
        @change="handleTemplateChange"
      />
    </div>

    <!-- 文件上传 -->
    <div class="import-section">
      <div class="section-label">
        <UploadOutlined />
        <span>从文件导入</span>
      </div>
      <Upload
        :before-upload="beforeUpload"
        :max-count="1"
        accept=".json"
      >
        <a-button>
          <template #icon><UploadOutlined /></template>
          选择 JSON 文件
        </a-button>
      </Upload>
    </div>

    <!-- 文本域 -->
    <div class="import-section">
      <div class="section-label">
        <span>Schema 内容</span>
        <a-button type="link" size="small" @click="validate" :disabled="!schemaJson">校验</a-button>
      </div>
      <Input.TextArea
        v-model:value="schemaJson"
        :rows="10"
        placeholder='粘贴 JSON Schema，例如：&#10;{&#10;  "type": "object",&#10;  "title": "客户表单",&#10;  "properties": {}&#10;}'
        spellcheck="false"
        :style="{ fontFamily: 'monospace', fontSize: '12px' }"
      />
    </div>

    <!-- 校验错误 -->
    <Alert
      v-if="validationErrors.length"
      type="error"
      show-icon
      style="margin-top: 8px"
    >
      <template #message>
        <ul class="error-list">
          <li v-for="(err, idx) in validationErrors" :key="idx">{{ err }}</li>
        </ul>
      </template>
    </Alert>
  </Modal>
</template>

<style lang="less" scoped>
.import-section {
  margin-bottom: 16px;
}
.section-label {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  font-size: 13px;
  color: @text-secondary;
  justify-content: space-between;
}
.section-label > span:first-child {
  display: flex;
  align-items: center;
  gap: 6px;
}
.error-list {
  margin: 0;
  padding-left: 16px;
}
</style>
