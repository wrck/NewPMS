<script setup lang="ts">
/**
 * 客户回访列表示例（Task 19.2）
 *
 * Schema 类型：ListSchema（type='list'）
 * configCode: customer-followup
 *
 * 列：客户名称 / 回访日期 / 回访人 / 满意度 / 备注
 * 搜索：关键字（input）、满意度（select）
 * 操作：新增 / 编辑 / 查看
 *
 * 该 Schema 与 V13__lowcode_business_examples.sql 中 lowcode_list_config（id=1011）一致。
 * 此处使用 staticData 提供 3 条示例数据，避免依赖后端 /api/v1/customer-followups 接口。
 */
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer.vue'
import RuntimeRenderer from '@/components/Lowcode/RuntimeRenderer.vue'
import type { ListSchema } from '@/types/lowcode'

const router = useRouter()

const schema: ListSchema = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  type: 'list',
  title: '客户回访列表',
  description: '客户回访记录低代码列表配置（Task 19.2 业务接入示例）',
  columns: [
    { field: 'customerName', title: '客户名称', width: 180 },
    { field: 'followupDate', title: '回访日期', width: 120 },
    { field: 'followupBy', title: '回访人', width: 120 },
    {
      field: 'satisfaction',
      title: '满意度',
      width: 100,
      align: 'center',
      valueEnum: {
        '5': { text: '★★★★★', status: 'success' },
        '4': { text: '★★★★', status: 'processing' },
        '3': { text: '★★★', status: 'warning' },
        '2': { text: '★★', status: 'warning' },
        '1': { text: '★', status: 'error' }
      }
    },
    { field: 'remark', title: '备注', width: 240, ellipsis: true }
  ],
  searchFields: [
    { field: 'keyword', label: '关键字', type: 'input', placeholder: '客户名称/回访人' },
    {
      field: 'satisfaction',
      label: '满意度',
      type: 'select',
      options: [
        { label: '5星', value: 5 },
        { label: '4星', value: 4 },
        { label: '3星', value: 3 },
        { label: '2星', value: 2 },
        { label: '1星', value: 1 }
      ]
    }
  ],
  actions: [
    { type: 'create', label: '新增' },
    { type: 'edit', label: '编辑' },
    { type: 'view', label: '查看' }
  ],
  apiUrl: '/api/v1/customer-followups',
  rowKey: 'id',
  pageSize: 10,
  scrollX: 900,
  formFields: [
    { field: 'customerName', label: '客户名称', type: 'input', required: true, width: 12 },
    { field: 'followupDate', label: '回访日期', type: 'date', required: true, width: 12 },
    { field: 'followupBy', label: '回访人', type: 'input', required: true, width: 12 },
    {
      field: 'satisfaction',
      label: '满意度',
      type: 'select',
      required: true,
      defaultValue: 5,
      width: 12,
      options: [
        { label: '5星', value: 5 },
        { label: '4星', value: 4 },
        { label: '3星', value: 3 },
        { label: '2星', value: 2 },
        { label: '1星', value: 1 }
      ]
    },
    { field: 'remark', label: '备注', type: 'textarea', width: 24 }
  ]
}

/** 静态示例数据：演示用，避免依赖后端接口 */
const staticData = [
  {
    id: 1,
    customerName: '上海宏图科技有限公司',
    followupDate: '2026-06-15',
    followupBy: '张明',
    satisfaction: 5,
    remark: '客户对项目进度满意，希望加快交付节奏'
  },
  {
    id: 2,
    customerName: '北京华盛集团',
    followupDate: '2026-06-20',
    followupBy: '李娜',
    satisfaction: 4,
    remark: '客户提出增加培训场次需求，已反馈至交付团队'
  },
  {
    id: 3,
    customerName: '深圳创新通信有限公司',
    followupDate: '2026-07-01',
    followupBy: '王强',
    satisfaction: 3,
    remark: '客户对现场工程师响应速度有意见，已记录待改进'
  },
  {
    id: 4,
    customerName: '广州东方实业',
    followupDate: '2026-07-03',
    followupBy: '赵敏',
    satisfaction: 5,
    remark: '项目里程碑按期完成，客户表示愿意长期合作'
  }
]

function handleSubmit(_data: Record<string, unknown>) {
  message.info('表单提交回调（示例，未实际入库）')
}

function handleAction(payload: { type: string; record?: Record<string, unknown> }) {
  console.log('[example:customer-followup] action:', payload)
  message.info(`操作 ${payload.type} 已触发` + (payload.record ? `，行 ID=${payload.record.id}` : ''))
}

function backToList() {
  router.push('/lowcode/examples')
}
</script>

<template>
  <PageContainer title="客户回访列表（示例）" description="Task 19.2：基于 list-config 的业务接入示例，5 列 + 2 搜索 + 3 操作按钮">
    <template #extra>
      <a-button @click="backToList">返回示例列表</a-button>
    </template>

    <a-alert
      type="info"
      show-icon
      message="示例说明"
      description="此列表由 RuntimeRenderer 根据 ListSchema 动态渲染。Schema 与 V13 迁移脚本中 lowcode_list_config（configCode=customer-followup）一致。本示例使用 staticData 提供 4 条演示数据，避免依赖后端 /api/v1/customer-followups 接口。"
      style="margin-bottom: 16px"
    />

    <div class="example-wrapper">
      <RuntimeRenderer
        :schema="schema"
        biz-type="customer-followup"
        :biz-id="0"
        :readonly="false"
        :static-data="staticData"
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
