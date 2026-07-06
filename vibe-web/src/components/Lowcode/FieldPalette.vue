<script setup lang="ts">
/**
 * FieldPalette 低代码字段组件库（spec 阶段三 - Task A2.2）
 *
 * 10 类可拖拽字段：input / textarea / number / select / date / switch / cascader /
 *                richText / file / relSelect
 *
 * 使用原生 HTML5 drag-drop API（不引入 vuedraggable）。
 */
import { computed } from 'vue'
import {
  EditOutlined,
  FileTextOutlined,
  NumberOutlined,
  DownSquareOutlined,
  CalendarOutlined,
  SwitcherOutlined,
  ApartmentOutlined,
  FontSizeOutlined,
  PaperClipOutlined,
  ApartmentOutlined as RelOutlined
} from '@ant-design/icons-vue'
import type { FieldType } from '@/types/lowcode'

/** 字段元数据（用于展示与初始化） */
interface FieldMeta {
  type: FieldType
  label: string
  icon: typeof EditOutlined
  /** 默认数据类型 */
  dataType: 'string' | 'number' | 'boolean' | 'object' | 'array'
  /** 字段名前缀（用于生成 field） */
  prefix: string
  /** 描述 */
  description?: string
}

/** 字段库分组 */
const groups = computed<Array<{ title: string; items: FieldMeta[] }>>(() => [
  {
    title: '基础字段',
    items: [
      { type: 'input', label: '单行输入', icon: EditOutlined, dataType: 'string', prefix: 'input', description: '文本输入框' },
      { type: 'textarea', label: '多行输入', icon: FileTextOutlined, dataType: 'string', prefix: 'textarea', description: '长文本输入' },
      { type: 'number', label: '数字', icon: NumberOutlined, dataType: 'number', prefix: 'num', description: '数字输入' },
      { type: 'switch', label: '开关', icon: SwitcherOutlined, dataType: 'boolean', prefix: 'switch', description: '布尔切换' }
    ]
  },
  {
    title: '选择类',
    items: [
      { type: 'select', label: '下拉选择', icon: DownSquareOutlined, dataType: 'string', prefix: 'select', description: '单选下拉' },
      { type: 'cascader', label: '级联选择', icon: ApartmentOutlined, dataType: 'array', prefix: 'cascade', description: '多级联动' },
      { type: 'date', label: '日期', icon: CalendarOutlined, dataType: 'string', prefix: 'date', description: '日期选择' },
      { type: 'relSelect', label: '关联选择', icon: RelOutlined, dataType: 'number', prefix: 'rel', description: '关联实体选择' }
    ]
  },
  {
    title: '富内容',
    items: [
      { type: 'richText', label: '富文本', icon: FontSizeOutlined, dataType: 'string', prefix: 'richText', description: '富文本编辑器' },
      { type: 'file', label: '文件上传', icon: PaperClipOutlined, dataType: 'array', prefix: 'file', description: '附件上传' }
    ]
  }
])

/** 拖拽开始：将字段元数据写入 dataTransfer */
function onDragStart(event: DragEvent, item: FieldMeta) {
  if (!event.dataTransfer) return
  const payload = {
    type: item.type,
    label: item.label,
    dataType: item.dataType,
    prefix: item.prefix,
    /** 标记来自字段库 */
    source: 'palette' as const
  }
  event.dataTransfer.effectAllowed = 'copy'
  // IE 11 兼容（虽不在支持范围，但保留降级行为）
  try {
    event.dataTransfer.setData('application/json', JSON.stringify(payload))
    event.dataTransfer.setData('text/plain', item.type)
  } catch {
    event.dataTransfer.setData('text', item.type)
  }
}

defineExpose({
  /** 暴露分组数据，供外部使用 */
  groups
})
</script>

<template>
  <div class="field-palette">
    <div class="palette-header">
      <span class="title">字段库</span>
      <span class="hint">拖拽到画布</span>
    </div>
    <div class="palette-body">
      <div v-for="(group, gIdx) in groups" :key="gIdx" class="palette-group">
        <div class="group-title">{{ group.title }}</div>
        <div class="group-items">
          <div
            v-for="item in group.items"
            :key="item.type"
            class="palette-item"
            draggable="true"
            :title="item.description || item.label"
            @dragstart="onDragStart($event, item)"
          >
            <component :is="item.icon" class="item-icon" />
            <span class="item-label">{{ item.label }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
.field-palette {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: @bg-container;
  border-right: 1px solid @border-split;
}
.palette-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid @border-split;
}
.palette-header .title {
  font-weight: 600;
  color: @text-primary;
}
.palette-header .hint {
  font-size: 12px;
  color: @text-tertiary;
}
.palette-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.palette-group {
  margin-bottom: 16px;
}
.group-title {
  font-size: 12px;
  color: @text-tertiary;
  padding: 4px 8px;
  margin-bottom: 4px;
}
.group-items {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 0 4px;
}
.palette-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 12px 8px;
  background: @bg-elevated;
  border: 1px solid @border-color;
  border-radius: @radius-card;
  cursor: grab;
  user-select: none;
  transition: all 0.2s;
  &:hover {
    border-color: @brand-primary;
    color: @brand-primary;
    background: @brand-bg;
  }
  &:active {
    cursor: grabbing;
  }
}
.item-icon {
  font-size: 18px;
}
.item-label {
  font-size: 12px;
  color: @text-secondary;
}
.palette-item:hover .item-label {
  color: @brand-primary;
}
</style>
