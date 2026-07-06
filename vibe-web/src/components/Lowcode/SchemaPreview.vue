<script setup lang="ts">
/**
 * SchemaPreview 低代码 Schema 实时预览（spec 阶段三 - Task A2.4）
 *
 * 调用 RuntimeRenderer 渲染只读视图。
 * 父组件传入 schemaJson 字符串，本组件解析后传递给 RuntimeRenderer。
 */
import { computed } from 'vue'
import { Empty } from 'ant-design-vue'
import RuntimeRenderer from './RuntimeRenderer.vue'
import type {
  FormSchema,
  ListSchema,
  TabSchema,
  RelationSchema
} from '@/types/lowcode'
import {
  parseFormSchema,
  parseListSchema,
  parseTabSchema,
  parseRelationSchema
} from '@/types/lowcode'

interface Props {
  /** Schema JSON 字符串 */
  schemaJson: string
  /** 是否只读 */
  readonly?: boolean
  /** 业务类型（运行时数据源） */
  bizType?: string
}

const props = withDefaults(defineProps<Props>(), {
  readonly: true
})

/** 解析后的 Schema 对象 */
const parsedSchema = computed<FormSchema | ListSchema | TabSchema | RelationSchema | null>(() => {
  if (!props.schemaJson) return null
  // 尝试按类型解析
  const trimmed = props.schemaJson.trim()
  if (!trimmed) return null

  // 先解析出 type
  try {
    const obj = JSON.parse(trimmed) as { type?: string }
    if (obj.type === 'object') {
      return parseFormSchema(props.schemaJson)
    } else if (obj.type === 'list') {
      return parseListSchema(props.schemaJson)
    } else if (obj.type === 'tab') {
      return parseTabSchema(props.schemaJson)
    } else if (obj.type === 'relation') {
      return parseRelationSchema(props.schemaJson)
    }
    // 未知类型，尝试通用解析
    return null
  } catch {
    return null
  }
})

/** 是否解析失败 */
const parseFailed = computed(() => !parsedSchema.value && !!props.schemaJson)

/** Schema 类型字符串（用于展示） */
const parsedSchemaType = computed<string>(() => {
  const s = parsedSchema.value as { type?: string } | null
  return s?.type || 'unknown'
})
</script>

<template>
  <div class="schema-preview">
    <div class="preview-header">
      <span class="title">实时预览</span>
      <span v-if="parsedSchema" class="type-tag">
        {{ parsedSchemaType }}
      </span>
    </div>
    <div class="preview-body">
      <Empty v-if="parseFailed" description="Schema 解析失败，请检查 JSON 格式" />
      <RuntimeRenderer
        v-else-if="parsedSchema"
        :schema="parsedSchema"
        :readonly="readonly"
        :biz-type="bizType"
      />
      <Empty v-else description="请先设计 Schema 后预览" />
    </div>
  </div>
</template>

<style lang="less" scoped>
.schema-preview {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: @bg-container;
  border: 1px solid @border-split;
  border-radius: @radius-card;
}
.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid @border-split;
}
.preview-header .title {
  font-weight: 600;
  color: @text-primary;
}
.type-tag {
  display: inline-block;
  padding: 2px 8px;
  font-size: 12px;
  color: @brand-primary;
  background: @brand-bg;
  border-radius: 4px;
}
.preview-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}
</style>
