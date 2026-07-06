<script setup lang="ts">
/**
 * 功能说明文档中心（Task D4.3）
 *
 * - 按模块组织（工作台 / 项目 / 设备 / 资源 / 交付 / 代理商 / 验收 / 财务 / 报表 / 系统 / 低代码）
 * - 顶部搜索框，前端实时过滤
 * - 内容为内嵌的精简文档（如需 markdown 渲染，请安装 marked 库；当前以纯文本展示）
 */
import { ref, computed } from 'vue'
import { Input, Card, Empty, Tag } from 'ant-design-vue'
import { SearchOutlined, BookOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'

interface HelpDoc {
  id: string
  module: string
  title: string
  summary: string
  content: string
}

const MODULE_LABELS: Record<string, { label: string; color: string }> = {
  dashboard: { label: '工作台', color: 'blue' },
  project: { label: '项目管理', color: 'cyan' },
  device: { label: '设备资产', color: 'geekblue' },
  resource: { label: '资源调度', color: 'green' },
  delivery: { label: '交付管理', color: 'orange' },
  agent: { label: '代理商', color: 'purple' },
  acceptance: { label: '验收管理', color: 'gold' },
  finance: { label: '财务核算', color: 'magenta' },
  report: { label: '报表中心', color: 'red' },
  system: { label: '系统管理', color: 'volcano' },
  lowcode: { label: '低代码', color: 'lime' }
}

const docs: HelpDoc[] = [
  {
    id: 'dashboard-overview',
    module: 'dashboard',
    title: '工作台概览',
    summary: '登录后默认进入工作台，集中查看项目进度、待办任务、未读消息。',
    content: `工作台分为三个区域：
1. 顶部统计卡：项目总数、设备总数、待办任务数、未读消息数
2. 中间图表：项目状态分布饼图、近 7 天任务完成趋势
3. 底部列表：今日待办、最新通知

操作：
- 点击「我的任务」查看分配给您的工作项
- 点击「我的消息」进入消息中心
- 点击侧边栏菜单进入各业务模块`
  },
  {
    id: 'project-list',
    module: 'project',
    title: '项目列表与详情',
    summary: '创建项目、查看项目详情、跟踪项目进度与阶段任务。',
    content: `项目列表：
- 支持按项目名/编号/状态/PM 筛选
- 点击项目编号进入详情页

项目详情：
- 基本信息：客户、PM、起止时间、预算
- 阶段任务树：WBS 结构展开，可指派工程师
- 交付物清单：上传文档、设备清单
- 验收信息：验收任务、遗留问题

操作权限：
- PM 可创建/编辑项目
- 工程师可查看自己参与的项目
- 客户可在 H5 门户查看项目进度`
  },
  {
    id: 'project-template',
    module: 'project',
    title: '项目模板',
    summary: '维护标准项目模板（阶段/任务/工时配额），便于快速创建同类项目。',
    content: `模板包含：
- 阶段定义（如：准备 → 安装 → 调测 → 割接 → 验收）
- 每阶段的标准任务与预计工时
- 必备交付物清单

创建项目时选择模板，系统自动生成 WBS。`
  },
  {
    id: 'device-model',
    module: 'device',
    title: '设备型号库',
    summary: '维护设备型号主数据，含型号编码、规格参数、保修期等。',
    content: `设备型号是设备的"模板"，一台具体设备引用型号 + 序列号。

字段：型号编码、型号名称、品牌、类别、规格参数（JSON）、保修期（月）。

设备台账引用型号，避免重复录入。`
  },
  {
    id: 'device-ledger',
    module: 'device',
    title: '设备台账',
    summary: '设备实例的台账，记录序列号、所属项目、安装位置、状态。',
    content: `每台设备实例：
- 引用型号
- 序列号（唯一）
- 当前状态：在库 / 已出库 / 安装中 / 已安装 / 故障 / 返修
- 所属项目 / 安装位置 / 安装时间
- 维修记录

支持批量导入（Excel）、按项目筛选、状态变更日志。`
  },
  {
    id: 'device-inout',
    module: 'device',
    title: '出入库管理',
    summary: '记录设备的出入库流水，关联项目与领用人。',
    content: `出库：从仓库领出设备到项目现场，需指定项目、领用人、计划归还时间。
入库：将设备退回仓库，可选择归还 / 返修 / 报废。

每条记录都会更新设备台账的当前状态。`
  },
  {
    id: 'device-spare',
    module: 'device',
    title: '备件管理',
    summary: '备件库存台账与领用归还记录。',
    content: `备件主数据：备件编码、名称、规格、库存数量、预警阈值。

操作：
- 领用：选择备件 + 项目 + 领用人 + 数量，扣减库存
- 归还：逆向恢复库存
- 返修：记录返修原因与状态

库存低于阈值时在备件列表标红预警。`
  },
  {
    id: 'resource-engineer',
    module: 'resource',
    title: '工程师资源池',
    summary: '维护工程师档案，含技能矩阵、可用性、当前任务负载。',
    content: `工程师信息：
- 基本信息：姓名、工号、组织、岗位、联系方式
- 技能矩阵：技能项 + 等级（初级/熟练/专家）
- 可用性：可用 / 已排满 / 请假
- 当前任务负载：进行中任务数、剩余工时

PM 派单时优先匹配技能与可用性。`
  },
  {
    id: 'resource-schedule',
    module: 'resource',
    title: '排期日历',
    summary: '工程师排期日历视图，避免冲突派工。',
    content: `日历视图：
- 月视图：每天显示工程师的任务条
- 周视图：含时段详情
- 颜色：项目色块表示任务归属

排期冲突时高亮提示，请假/差旅自动标记不可分配。`
  },
  {
    id: 'resource-dispatch',
    module: 'resource',
    title: '任务派发',
    summary: 'PM 给工程师派发任务，含工时预估与截止时间。',
    content: `派单流程：
1. 选择任务（来自项目 WBS）
2. 选择工程师（按技能筛选）
3. 设置计划起止时间与工时
4. 工程师收到站内信通知

工程师可在 H5 端接单/拒绝/反馈进度。`
  },
  {
    id: 'delivery-field',
    module: 'delivery',
    title: '现场作业',
    summary: '现场工程师作业记录，含照片打卡与签到。',
    content: `作业单包含：
- 关联项目任务
- 作业地点（高德地图定位）
- 签到/签退时间
- 现场照片（带时间+GPS水印）
- 作业内容描述

代理商工程师可在 H5 端提交作业单，PM 在 PC 端审核。`
  },
  {
    id: 'delivery-cutover',
    module: 'delivery',
    title: '割接管理',
    summary: '割接方案制定、审批、执行与回退预案。',
    content: `割接方案：
- 割接时间窗（凌晨低峰期）
- 操作步骤清单
- 风险评估
- 回退预案

审批流：PM 提交 → 客户审批 → 执行 → 结果确认

客户可在 H5 门户通过链接审批割接方案。`
  },
  {
    id: 'agent-profile',
    module: 'agent',
    title: '代理商档案',
    summary: '代理商基本信息、合同、工程师名册。',
    content: `代理商信息：
- 公司基本信息：名称、统一社会信用代码、联系人
- 资质：营业范围、行业资质证书
- 服务区域：省/市
- 工程师名册：可派单的工程师列表`
  },
  {
    id: 'agent-outsource',
    module: 'agent',
    title: '转包任务',
    summary: '将任务转包给代理商执行，跟踪进度与质量。',
    content: `转包流程：
1. 主包方 PM 创建转包任务
2. 选择代理商与工程师
3. 设置 SLA（响应时间 / 完成时间）
4. 代理商工程师执行并提交交付物
5. 主包方审核与结算

转包任务有独立状态机：待接单 → 进行中 → 已提交 → 已审核 → 已结算`
  },
  {
    id: 'acceptance-standard',
    module: 'acceptance',
    title: '验收标准',
    summary: '维护验收检查项模板，应用于不同类型项目的验收。',
    content: `标准模板：
- 适用项目类型（设备安装 / 系统集成 / 网络优化）
- 检查项分类（外观 / 功能 / 性能 / 文档）
- 每项的检查方法与判定标准

新建验收任务时引用模板，自动生成检查清单。`
  },
  {
    id: 'acceptance-task',
    module: 'acceptance',
    title: '验收任务',
    summary: '执行验收检查，记录问题，提交客户签核。',
    content: `验收任务流程：
1. 工程师按检查清单逐项检查
2. 不合格项记录为遗留问题
3. 整改完成后复核
4. 全部通过后提交客户签核
5. 客户在 H5 门户确认签字

签核后项目状态变更为"已验收"。`
  },
  {
    id: 'finance-budget',
    module: 'finance',
    title: '项目预算',
    summary: '维护项目预算与执行情况。',
    content: `预算结构：
- 人工成本：按工程师日费率 × 工时
- 设备成本：设备采购/租赁
- 差旅成本：按差旅申请单汇总
- 代理商结算：转包任务结算金额

实际成本自动归集，预算执行率实时展示。`
  },
  {
    id: 'report-cockpit',
    module: 'report',
    title: '管理驾驶舱',
    summary: '高管视图，多维度统计与趋势分析。',
    content: `驾驶舱包含：
- 项目健康度分布
- 收入与利润趋势
- 工程师利用率
- 客户满意度

支持时间范围筛选与导出 Excel。`
  },
  {
    id: 'system-user',
    module: 'system',
    title: '用户管理',
    summary: '系统用户 CRUD、角色分配、状态管理。',
    content: `用户字段：
- 用户名（登录名，唯一）
- 真实姓名
- 邮箱 / 手机号
- 组织 / 岗位
- 角色（多选）
- 状态（启用 / 禁用）

操作：新增 / 编辑 / 删除 / 重置密码 / 分配角色 / 状态变更。

权限：仅 SUPER_ADMIN 可访问。`
  },
  {
    id: 'system-role',
    module: 'system',
    title: '角色权限',
    summary: '维护 RBAC 角色与权限分配。',
    content: `预置角色：
- SUPER_ADMIN 超级管理员
- DIRECTOR 总监
- PM 项目经理
- ENGINEER 工程师
- FINANCE 财务
- AGENT_ADMIN 代理商管理员
- AGENT_ENGINEER 代理商工程师
- CUSTOMER 客户

可自定义角色并分配菜单权限。`
  },
  {
    id: 'system-feedback',
    module: 'system',
    title: '反馈管理',
    summary: '查看与处理用户提交的 Bug 报告、功能建议、咨询。',
    content: `反馈类型：
- BUG 缺陷
- SUGGESTION 建议
- QUESTION 咨询

状态流转：PENDING 待处理 → PROCESSING 处理中 → RESOLVED 已解决 → CLOSED 已关闭

处理反馈时填写处理备注，状态变更后自动通过站内信通知提交人。

任意登录用户可通过页面右下角悬浮按钮提交反馈。`
  },
  {
    id: 'lowcode-config',
    module: 'lowcode',
    title: '低代码配置',
    summary: '通过可视化设计器配置表单/列表/标签页 Schema，运行时动态渲染业务页面。',
    content: `四类 Schema：
- 表单 Schema：字段定义 + 校验规则
- 列表 Schema：列定义 + 筛选器 + 操作按钮
- 标签页 Schema：多 Tab 布局
- 关联页 Schema：master-detail 主从结构

设计器：左侧字段库 → 中间画布 → 右侧属性面板，支持 JSON 预览与导入导出。

模板库：可将配置保存为模板，后续通过实例化快速创建。

低代码渲染率 ≥ 60%：客户档案、设备型号、备件等通用 CRUD 已通过低代码渲染。`
  }
]

const searchText = ref('')

const filteredDocs = computed(() => {
  if (!searchText.value.trim()) return docs
  const kw = searchText.value.trim().toLowerCase()
  return docs.filter(d =>
    d.title.toLowerCase().includes(kw) ||
    d.summary.toLowerCase().includes(kw) ||
    d.content.toLowerCase().includes(kw) ||
    (MODULE_LABELS[d.module]?.label || '').toLowerCase().includes(kw)
  )
})

const groupedDocs = computed(() => {
  const groups: Record<string, HelpDoc[]> = {}
  for (const d of filteredDocs.value) {
    if (!groups[d.module]) groups[d.module] = []
    groups[d.module].push(d)
  }
  return groups
})

const moduleOrder = ['dashboard', 'project', 'device', 'resource', 'delivery', 'agent', 'acceptance', 'finance', 'report', 'system', 'lowcode']

const selectedDoc = ref<HelpDoc | null>(docs[0])

function selectDoc(d: HelpDoc) {
  selectedDoc.value = d
}
</script>

<template>
  <PageContainer title="功能说明" description="按模块组织的功能使用文档，支持搜索">
    <template #extra>
      <a-button @click="$router.back()"><template #icon /><BookOutlined /> 返回</a-button>
    </template>

    <div class="vibe-card search-card">
      <Input
        v-model:value="searchText"
        placeholder="搜索模块 / 标题 / 内容关键字"
        allow-clear
        size="large"
      >
        <template #prefix><SearchOutlined /></template>
      </Input>
    </div>

    <div class="help-layout">
      <!-- 左侧目录 -->
      <div class="help-sider">
        <div v-for="mod in moduleOrder" :key="mod" v-show="groupedDocs[mod]?.length" class="help-module">
          <div class="help-module-title">
            <Tag :color="MODULE_LABELS[mod]?.color">{{ MODULE_LABELS[mod]?.label || mod }}</Tag>
          </div>
          <ul class="help-doc-list">
            <li
              v-for="d in groupedDocs[mod] || []"
              :key="d.id"
              :class="{ active: selectedDoc?.id === d.id }"
              @click="selectDoc(d)"
            >
              {{ d.title }}
            </li>
          </ul>
        </div>
        <Empty v-if="!filteredDocs.length" description="未找到匹配的文档" />
      </div>

      <!-- 右侧内容 -->
      <div class="help-content">
        <Card v-if="selectedDoc" :bordered="false">
          <template #title>
            <div class="help-content-header">
              <Tag :color="MODULE_LABELS[selectedDoc.module]?.color">
                {{ MODULE_LABELS[selectedDoc.module]?.label || selectedDoc.module }}
              </Tag>
              <span class="help-content-title">{{ selectedDoc.title }}</span>
            </div>
          </template>
          <template #extra>
            <span class="help-summary">{{ selectedDoc.summary }}</span>
          </template>
          <div class="help-doc-body">
            <pre>{{ selectedDoc.content }}</pre>
          </div>
        </Card>
        <Empty v-else description="请从左侧选择一个文档" />
      </div>
    </div>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card {
  padding: 16px 20px;
  margin-bottom: 16px;
}

.help-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.help-sider {
  width: 280px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  padding: 12px 16px;
  max-height: calc(100vh - 240px);
  overflow-y: auto;
}

.help-module {
  margin-bottom: 12px;
  &-title {
    margin-bottom: 6px;
  }
}

.help-doc-list {
  list-style: none;
  padding: 0;
  margin: 0 0 0 8px;
  li {
    padding: 6px 10px;
    border-radius: 4px;
    cursor: pointer;
    color: rgba(0, 0, 0, 0.7);
    font-size: 13px;
    transition: all 0.15s;
    &:hover {
      background: rgba(24, 144, 255, 0.06);
      color: @brand-primary;
    }
    &.active {
      background: @brand-primary;
      color: #fff;
    }
  }
}

.help-content {
  flex: 1;
  min-width: 0;
}

.help-content-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.help-content-title {
  font-size: 16px;
  font-weight: 600;
}

.help-summary {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.help-doc-body {
  pre {
    white-space: pre-wrap;
    word-wrap: break-word;
    margin: 0;
    font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
    font-size: 14px;
    line-height: 1.7;
    color: rgba(0, 0, 0, 0.75);
  }
}

@media (max-width: 768px) {
  .help-layout {
    flex-direction: column;
  }
  .help-sider {
    width: 100%;
    max-height: 240px;
  }
}
</style>
