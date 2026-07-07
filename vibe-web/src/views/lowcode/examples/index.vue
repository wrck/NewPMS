<script setup lang="ts">
/**
 * 低代码业务接入示例总览页（spec 阶段三 - Task 19）
 *
 * 三个示例：
 *   1. 设备巡检表单（form-config 类型）
 *   2. 客户回访列表（list-config 类型）
 *   3. 项目阶段交付关联页（relation-config 类型）
 *
 * 这些示例在数据库 V13 迁移中也以种子数据形式存在，可在低代码配置页中查看。
 * 此页面提供独立的运行时演示入口，Schema 内联，无需依赖后端配置。
 */
import { useRouter } from 'vue-router'
import PageContainer from '@/components/PageContainer.vue'
import {
  FormOutlined,
  TableOutlined,
  ApartmentOutlined,
  ArrowRightOutlined
} from '@ant-design/icons-vue'

const router = useRouter()

interface ExampleCard {
  key: string
  title: string
  type: 'form' | 'list' | 'relation'
  typeLabel: string
  description: string
  icon: typeof FormOutlined
  color: string
  route: string
  fields: string[]
}

const examples: ExampleCard[] = [
  {
    key: 'device-inspection',
    title: '设备巡检表单',
    type: 'form',
    typeLabel: 'Form 配置',
    description: '现场设备巡检记录表单，包含 7 个字段，覆盖 input/date/select/textarea/file 五类组件。提交接口为 POST /api/v1/device-inspections。',
    icon: FormOutlined,
    color: '#1677ff',
    route: '/lowcode/examples/device-inspection',
    fields: ['设备SN', '巡检日期', '巡检人', '外观检查', '功能测试', '备注', '现场照片']
  },
  {
    key: 'customer-followup',
    title: '客户回访列表',
    type: 'list',
    typeLabel: 'List 配置',
    description: '客户回访记录列表，含 5 列字段、2 个搜索条件（关键字 + 满意度）、3 个操作按钮（新增/编辑/查看）。数据源 /api/v1/customer-followups。',
    icon: TableOutlined,
    color: '#52c41a',
    route: '/lowcode/examples/customer-followup',
    fields: ['客户名称', '回访日期', '回访人', '满意度', '备注']
  },
  {
    key: 'project-phase-delivery',
    title: '项目阶段交付关联页',
    type: 'relation',
    typeLabel: 'Relation 配置',
    description: '主从关联页：主表为项目阶段，从表为阶段交付物，外键 phaseId。主表 4 列、从表 4 列，主表行选中后联动加载从表数据。',
    icon: ApartmentOutlined,
    color: '#fa8c16',
    route: '/lowcode/examples/project-phase-delivery',
    fields: ['项目阶段（主）', '阶段交付物（从）', '外键 phaseId']
  }
]

function go(route: string) {
  router.push(route)
}
</script>

<template>
  <PageContainer title="低代码业务接入示例" description="Task 19：3 个开箱即用的业务接入示例（Schema 内联，可直接演示）">
    <div class="example-grid">
      <div
        v-for="ex in examples"
        :key="ex.key"
        class="example-card"
        @click="go(ex.route)"
      >
        <div class="card-header" :style="{ background: ex.color }">
          <component :is="ex.icon" class="card-icon" />
          <span class="card-type">{{ ex.typeLabel }}</span>
        </div>
        <div class="card-body">
          <div class="card-title">{{ ex.title }}</div>
          <div class="card-desc">{{ ex.description }}</div>
          <div class="card-fields">
            <a-tag v-for="f in ex.fields" :key="f" color="blue">{{ f }}</a-tag>
          </div>
        </div>
        <div class="card-footer">
          <a-button type="link">
            查看示例
            <ArrowRightOutlined />
          </a-button>
        </div>
      </div>
    </div>

    <a-alert
      type="info"
      show-icon
      message="数据库种子数据"
      description="这些示例的 Schema 同时以种子数据形式存在于 V13__lowcode_business_examples.sql 中，可在『低代码配置 → 表单/列表/关联页配置』中以 configCode（device-inspection / customer-followup / project-phase-delivery）查看与编辑。"
      style="margin-top: 24px"
    />
  </PageContainer>
</template>

<style lang="less" scoped>
.example-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
}
.example-card {
  background: @bg-container;
  border: 1px solid @border-color;
  border-radius: @radius-card;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  &:hover {
    border-color: @brand-primary;
    box-shadow: 0 4px 12px rgba(22, 119, 255, 0.15);
    transform: translateY(-2px);
  }
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  color: #fff;
}
.card-icon {
  font-size: 28px;
}
.card-type {
  font-size: 12px;
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 4px;
}
.card-body {
  flex: 1;
  padding: 16px 20px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: @text-primary;
  margin-bottom: 8px;
}
.card-desc {
  font-size: 13px;
  color: @text-secondary;
  line-height: 1.6;
  margin-bottom: 12px;
  min-height: 62px;
}
.card-fields {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.card-footer {
  padding: 0 20px 12px;
  text-align: right;
}
</style>
