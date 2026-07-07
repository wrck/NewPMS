<script setup lang="ts">
/**
 * 项目阶段交付关联页示例（Task 19.3）
 *
 * Schema 类型：RelationSchema（type='relation'）
 * configCode: project-phase-delivery
 *
 * 主表：项目阶段（bizType=project-phase）
 *   - 列：阶段名称 / 计划开始 / 计划结束 / 状态
 * 从表：阶段交付物（bizType=phase-deliverable）
 *   - 列：交付物名称 / 类型 / 提交时间 / 状态
 *   - 外键：phaseId
 *
 * 该 Schema 与 V13__lowcode_business_examples.sql 中 lowcode_relation_config（id=3001）一致。
 * 由于 RuntimeRenderer 在 relation 模式下默认从 apiUrl 加载数据，本示例通过
 * 自定义 actionHandlers 与 readonly 控制展示形态；后端数据接入由 V13 种子配置承担。
 */
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer.vue'
import RuntimeRenderer from '@/components/Lowcode/RuntimeRenderer.vue'
import type { RelationSchema } from '@/types/lowcode'

const router = useRouter()

const schema: RelationSchema = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  type: 'relation',
  title: '项目阶段交付关联页',
  description: '主表项目阶段 + 从表阶段交付物（Task 19.3 业务接入示例）',
  master: {
    bizType: 'project-phase',
    label: '项目阶段',
    apiUrl: '/api/v1/projects/phases',
    rowKey: 'id',
    displayField: 'phaseName',
    columns: [
      { field: 'phaseName', title: '阶段名称', width: 160 },
      { field: 'planStart', title: '计划开始', width: 120 },
      { field: 'planEnd', title: '计划结束', width: 120 },
      {
        field: 'status',
        title: '状态',
        width: 100,
        valueEnum: {
          PENDING: { text: '未开始', status: 'default' },
          IN_PROGRESS: { text: '进行中', status: 'processing' },
          DONE: { text: '已完成', status: 'success' }
        }
      }
    ],
    searchFields: [
      { field: 'keyword', label: '关键字', type: 'input', placeholder: '阶段名称' }
    ]
  },
  details: [
    {
      bizType: 'phase-deliverable',
      label: '阶段交付物',
      apiUrl: '/api/v1/projects/deliverables',
      rowKey: 'id',
      foreignKey: 'phaseId',
      defaultExpand: true,
      columns: [
        { field: 'deliverableName', title: '交付物名称', width: 200 },
        {
          field: 'deliverableType',
          title: '类型',
          width: 100,
          valueEnum: {
            DOCUMENT: { text: '文档', status: 'processing' },
            CODE: { text: '代码', status: 'success' },
            ARTIFACT: { text: '实物', status: 'warning' }
          }
        },
        { field: 'submitTime', title: '提交时间', width: 140 },
        {
          field: 'status',
          title: '状态',
          width: 100,
          valueEnum: {
            DRAFT: { text: '草稿', status: 'default' },
            SUBMITTED: { text: '已提交', status: 'processing' },
            ACCEPTED: { text: '已验收', status: 'success' },
            REJECTED: { text: '已驳回', status: 'error' }
          }
        }
      ],
      formFields: [
        { field: 'deliverableName', label: '交付物名称', type: 'input', required: true, width: 24 },
        {
          field: 'deliverableType',
          label: '类型',
          type: 'select',
          required: true,
          width: 12,
          options: [
            { label: '文档', value: 'DOCUMENT' },
            { label: '代码', value: 'CODE' },
            { label: '实物', value: 'ARTIFACT' }
          ]
        },
        { field: 'submitTime', label: '提交时间', type: 'date', width: 12 },
        {
          field: 'status',
          label: '状态',
          type: 'select',
          defaultValue: 'DRAFT',
          width: 12,
          options: [
            { label: '草稿', value: 'DRAFT' },
            { label: '已提交', value: 'SUBMITTED' },
            { label: '已验收', value: 'ACCEPTED' },
            { label: '已驳回', value: 'REJECTED' }
          ]
        }
      ]
    }
  ]
}

function handleSubmit(_data: Record<string, unknown>) {
  message.info('表单提交回调（示例，未实际入库）')
}

function handleAction(payload: { type: string; record?: Record<string, unknown> }) {
  console.log('[example:project-phase-delivery] action:', payload)
  message.info(`操作 ${payload.type} 已触发`)
}

function backToList() {
  router.push('/lowcode/examples')
}
</script>

<template>
  <PageContainer title="项目阶段交付关联页（示例）" description="Task 19.3：基于 relation-config 的业务接入示例，主表 4 列 + 从表 4 列 + 外键 phaseId">
    <template #extra>
      <a-button @click="backToList">返回示例列表</a-button>
    </template>

    <a-alert
      type="info"
      show-icon
      message="示例说明"
      description="此关联页由 RuntimeRenderer 根据 RelationSchema 动态渲染。Schema 与 V13 迁移脚本中 lowcode_relation_config（configCode=project-phase-delivery）一致。运行时主表行选中后，从表会以 phaseId 作为外键参数请求 /api/v1/projects/deliverables 接口。"
      style="margin-bottom: 16px"
    />

    <div class="example-wrapper">
      <RuntimeRenderer
        :schema="schema"
        biz-type="project-phase-delivery"
        :biz-id="0"
        :readonly="false"
        @submit="handleSubmit"
        @action="handleAction"
      />
    </div>
  </PageContainer>
</template>

<style lang="less" scoped>
.example-wrapper {
  background: @bg-container;
  border-radius: @radius-card;
  padding: 16px;
}
</style>
