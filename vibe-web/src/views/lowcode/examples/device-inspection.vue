<script setup lang="ts">
/**
 * 设备巡检表单示例（Task 19.1）
 *
 * Schema 类型：FormSchema（type='object'）
 * configCode: device-inspection
 *
 * 字段：
 *   1. deviceSn        input    必填  设备SN
 *   2. inspectionDate  date     必填  巡检日期
 *   3. inspector       input    必填  巡检人
 *   4. appearanceCheck select   必填  外观检查（正常/异常）
 *   5. functionTest    select   必填  功能测试（通过/不通过）
 *   6. remark          textarea 可选  备注
 *   7. photos          file     可选  照片上传
 *
 * 该 Schema 与 V13__lowcode_business_examples.sql 中插入的种子数据保持一致。
 * 在此处内联的目的：示例页面可独立运行演示，不依赖后端配置查询。
 */
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/PageContainer.vue'
import RuntimeRenderer from '@/components/Lowcode/RuntimeRenderer.vue'
import type { FormSchema } from '@/types/lowcode'

const router = useRouter()

const schema: FormSchema = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  type: 'object',
  title: '设备巡检表单',
  description: '现场设备巡检记录表单（Task 19.1 业务接入示例）',
  layout: 'vertical',
  apiUrl: '/api/v1/device-inspections',
  apiMethod: 'POST',
  properties: {
    deviceSn: {
      field: 'deviceSn',
      label: '设备SN',
      type: 'input',
      dataType: 'string',
      required: true,
      placeholder: '请输入设备SN',
      width: 12,
      order: 0,
      rules: [{ required: true, message: '请输入设备SN', trigger: 'blur' }]
    },
    inspectionDate: {
      field: 'inspectionDate',
      label: '巡检日期',
      type: 'date',
      dataType: 'string',
      required: true,
      placeholder: '请选择巡检日期',
      width: 12,
      order: 1
    },
    inspector: {
      field: 'inspector',
      label: '巡检人',
      type: 'input',
      dataType: 'string',
      required: true,
      placeholder: '请输入巡检人姓名',
      width: 12,
      order: 2
    },
    appearanceCheck: {
      field: 'appearanceCheck',
      label: '外观检查',
      type: 'select',
      dataType: 'string',
      required: true,
      placeholder: '请选择外观检查结果',
      width: 12,
      order: 3,
      options: [
        { label: '正常', value: 'PASS' },
        { label: '异常', value: 'FAIL' }
      ]
    },
    functionTest: {
      field: 'functionTest',
      label: '功能测试',
      type: 'select',
      dataType: 'string',
      required: true,
      placeholder: '请选择功能测试结果',
      width: 12,
      order: 4,
      options: [
        { label: '通过', value: 'PASS' },
        { label: '不通过', value: 'FAIL' }
      ]
    },
    remark: {
      field: 'remark',
      label: '备注',
      type: 'textarea',
      dataType: 'string',
      required: false,
      placeholder: '补充说明（可选）',
      width: 24,
      order: 5
    },
    photos: {
      field: 'photos',
      label: '现场照片',
      type: 'file',
      dataType: 'array',
      required: false,
      placeholder: '上传现场照片',
      width: 24,
      order: 6
    }
  }
}

const submitting = ref(false)

function handleSubmit(data: Record<string, unknown>) {
  submitting.value = true
  console.log('[example:device-inspection] 表单提交数据：', data)
  message.success('设备巡检表单已提交（示例，未实际入库）')
  setTimeout(() => {
    submitting.value = false
  }, 300)
}

function handleAction(payload: { type: string; record?: Record<string, unknown> }) {
  console.log('[example:device-inspection] action:', payload)
  message.info(`操作 ${payload.type} 已触发`)
}

function backToList() {
  router.push('/lowcode/examples')
}
</script>

<template>
  <PageContainer title="设备巡检表单（示例）" description="Task 19.1：基于 form-config 的业务接入示例，7 字段覆盖 5 类组件">
    <template #extra>
      <a-button @click="backToList">返回示例列表</a-button>
    </template>

    <a-alert
      type="info"
      show-icon
      message="示例说明"
      description="此表单由 RuntimeRenderer 根据 FormSchema 动态渲染。Schema 与 V13 迁移脚本中 lowcode_form_config（configCode=device-inspection）保持一致。点击底部『提交』可触发 handleSubmit 回调。"
      style="margin-bottom: 16px"
    />

    <div class="example-wrapper">
      <RuntimeRenderer
        :schema="schema"
        biz-type="device-inspection"
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
  padding: 24px;
}
</style>
