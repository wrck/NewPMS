<script setup lang="ts">
/**
 * 项目详情页（设计文档 3.3.3）
 * 标签页：基本信息 / 阶段任务 / 里程碑 / 风险 / 问题 / 变更 / 成员 / 评论
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  ExportOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  getProjectDetail,
  listPhases,
  listTasksByProject,
  listMilestones,
  listRisks,
  listIssues,
  listChanges,
  listMembers,
  listComments,
  addComment,
  transitionProjectStatus
} from '@/api/project'
import type { ProjectDetail, ProjectPhase, ProjectTask, Milestone, ProjectRisk, ProjectIssue, ProjectChange, ProjectMember, ProjectComment } from '@/types/project'
import {
  ProjectStatus,
  ProjectStatusTone,
  ProjectStatusLabel,
  TaskStatus,
  TaskStatusTone,
  TaskStatusLabel,
  Priority,
  PriorityLabel
} from '@/types/enum'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))

const loading = ref(false)
const detail = ref<ProjectDetail | null>(null)
const activeTab = ref<'info' | 'phases' | 'tasks' | 'milestones' | 'risks' | 'issues' | 'changes' | 'members' | 'comments'>('info')

// 子资源
const phases = ref<ProjectPhase[]>([])
const tasks = ref<ProjectTask[]>([])
const milestones = ref<Milestone[]>([])
const risks = ref<ProjectRisk[]>([])
const issues = ref<ProjectIssue[]>([])
const changes = ref<ProjectChange[]>([])
const members = ref<ProjectMember[]>([])
const comments = ref<ProjectComment[]>([])

const commentText = ref('')
const commentSubmitting = ref(false)

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getProjectDetail(projectId.value)
  } catch (e) {
    console.error('[project.detail] load failed:', e)
  } finally {
    loading.value = false
  }
}

async function loadTabData(tab: typeof activeTab.value) {
  try {
    switch (tab) {
      case 'phases':
        phases.value = (await listPhases(projectId.value)) || []
        break
      case 'tasks':
        tasks.value = (await listTasksByProject(projectId.value)) || []
        break
      case 'milestones':
        milestones.value = (await listMilestones(projectId.value)) || []
        break
      case 'risks':
        risks.value = (await listRisks(projectId.value)) || []
        break
      case 'issues':
        issues.value = (await listIssues(projectId.value)) || []
        break
      case 'changes':
        changes.value = (await listChanges(projectId.value)) || []
        break
      case 'members':
        members.value = (await listMembers(projectId.value)) || []
        break
      case 'comments':
        comments.value = (await listComments(projectId.value)) || []
        break
    }
  } catch (e) {
    console.error('[project.detail] load tab data failed:', e)
  }
}

function handleTabChange(tab: string) {
  activeTab.value = tab as typeof activeTab.value
  loadTabData(activeTab.value)
}

// ============ 状态流转 ============
function nextStatus(cur: ProjectStatus): ProjectStatus | null {
  const order: ProjectStatus[] = [ProjectStatus.INIT, ProjectStatus.PLAN, ProjectStatus.EXECUTE, ProjectStatus.ACCEPT, ProjectStatus.CLOSE]
  const idx = order.indexOf(cur)
  if (idx >= 0 && idx < order.length - 1) return order[idx + 1]
  return null
}

function handleTransition() {
  if (!detail.value) return
  const next = nextStatus(detail.value.status)
  if (!next) {
    message.info('当前状态无下一步流转')
    return
  }
  Modal.confirm({
    title: '状态流转确认',
    content: `确认将项目状态从「${ProjectStatusLabel[detail.value.status]}」流转为「${ProjectStatusLabel[next]}」？`,
    okText: '确认流转',
    cancelText: '取消',
    async onOk() {
      try {
        await transitionProjectStatus(projectId.value, {
          targetStatus: next,
          version: detail.value?.version
        })
        message.success('状态流转成功')
        loadDetail()
      } catch (e) {
        // ignore
      }
    }
  })
}

// ============ 评论 ============
async function submitComment() {
  if (!commentText.value.trim()) {
    message.warning('请输入评论内容')
    return
  }
  commentSubmitting.value = true
  try {
    await addComment(projectId.value, commentText.value)
    message.success('评论已发布')
    commentText.value = ''
    comments.value = await listComments(projectId.value)
  } catch (e) {
    // ignore
  } finally {
    commentSubmitting.value = false
  }
}

function goTask(taskId: number) {
  router.push(`/project/task/${taskId}`)
}

// ============ 表格列 ============
const taskColumns = [
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName', ellipsis: true },
  { title: '所属阶段', dataIndex: 'phaseName', key: 'phaseName', width: 120 },
  { title: '执行人', dataIndex: 'assigneeName', key: 'assigneeName', width: 110 },
  { title: '执行模式', key: 'executeMode', width: 90 },
  { title: '状态', key: 'status', width: 100 },
  { title: '进度', key: 'progressPct', width: 140 },
  { title: '优先级', key: 'priority', width: 80 },
  { title: '计划周期', key: 'plannedRange', width: 200 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]

const phaseColumns = [
  { title: '阶段', dataIndex: 'phaseName', key: 'phaseName', width: 140 },
  { title: '状态', key: 'status', width: 120 },
  { title: '进度', key: 'progressPct', width: 160 },
  { title: '计划开始', dataIndex: 'plannedStart', key: 'plannedStart', width: 130 },
  { title: '计划结束', dataIndex: 'plannedEnd', key: 'plannedEnd', width: 130 },
  { title: '实际开始', dataIndex: 'actualStart', key: 'actualStart', width: 130 },
  { title: '实际结束', dataIndex: 'actualEnd', key: 'actualEnd', width: 130 }
]

const phaseStatusMap: Record<string, { tone: any; label: string }> = {
  NOT_STARTED: { tone: 'default', label: '未开始' },
  IN_PROGRESS: { tone: 'processing', label: '进行中' },
  COMPLETED: { tone: 'success', label: '已完成' }
}

onMounted(() => {
  loadDetail()
  loadTabData('phases')
})
</script>

<template>
  <PageContainer :show-back="true" @back="router.back()">
    <template #header>
      <div class="detail-header">
        <h2 class="vibe-page-title">
          {{ detail?.projectName || '项目详情' }}
          <span class="project-code">{{ detail?.projectCode }}</span>
        </h2>
        <p class="detail-meta" v-if="detail">
          PM：{{ detail.pmName || '-' }} · 客户：{{ detail.customerName || detail.customerId }} ·
          区域：{{ detail.region || '-' }} · 优先级：{{ PriorityLabel[detail.priority as Priority] }}
        </p>
      </div>
    </template>
    <template #extra>
      <a-button @click="loadDetail">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
      <a-button>
        <template #icon><ExportOutlined /></template>
        导出
      </a-button>
      <a-button v-if="detail && nextStatus(detail.status)" type="primary" @click="handleTransition">
        流转至「{{ ProjectStatusLabel[nextStatus(detail.status) as ProjectStatus] }}」
      </a-button>
    </template>

    <a-spin :spinning="loading">
      <!-- 概览卡片 -->
      <a-row v-if="detail" :gutter="16" class="overview-row">
        <a-col :xs="12" :md="6">
          <div class="vibe-card overview-card">
            <div class="overview-label">当前状态</div>
            <div class="overview-value">
              <StatusTag :tone="ProjectStatusTone[detail.status as ProjectStatus]">
                {{ ProjectStatusLabel[detail.status as ProjectStatus] }}
              </StatusTag>
            </div>
          </div>
        </a-col>
        <a-col :xs="12" :md="6">
          <div class="vibe-card overview-card">
            <div class="overview-label">总进度</div>
            <div class="overview-value">
              <ProgressBar :percent="detail.progressPct || 0" size="large" />
            </div>
          </div>
        </a-col>
        <a-col :xs="12" :md="6">
          <div class="vibe-card overview-card">
            <div class="overview-label">任务统计</div>
            <div class="overview-value text-statistic">
              {{ detail.taskStats?.completed || 0 }} / {{ detail.taskStats?.total || 0 }}
              <span class="overview-sub">已完成</span>
            </div>
          </div>
        </a-col>
        <a-col :xs="12" :md="6">
          <div class="vibe-card overview-card">
            <div class="overview-label">风险 / 问题</div>
            <div class="overview-value text-statistic">
              <span class="text-exception">{{ detail.riskCount || 0 }}</span> /
              <span class="text-exception">{{ detail.issueCount || 0 }}</span>
            </div>
          </div>
        </a-col>
      </a-row>

      <!-- 标签页 -->
      <div class="vibe-card tab-card">
        <a-tabs :active-key="activeTab" @change="handleTabChange">
          <!-- 基本信息 -->
          <a-tab-pane key="info" tab="基本信息">
            <a-descriptions v-if="detail" :column="3" bordered size="small">
              <a-descriptions-item label="项目编号">{{ detail.projectCode }}</a-descriptions-item>
              <a-descriptions-item label="项目名称">{{ detail.projectName }}</a-descriptions-item>
              <a-descriptions-item label="客户">{{ detail.customerName || detail.customerId }}</a-descriptions-item>
              <a-descriptions-item label="项目类型">{{ detail.projectType }}</a-descriptions-item>
              <a-descriptions-item label="产品线">{{ detail.productLine }}</a-descriptions-item>
              <a-descriptions-item label="执行模式">{{ detail.executeMode }}</a-descriptions-item>
              <a-descriptions-item label="优先级">{{ PriorityLabel[detail.priority as Priority] }}</a-descriptions-item>
              <a-descriptions-item label="项目经理">{{ detail.pmName || detail.pmId }}</a-descriptions-item>
              <a-descriptions-item label="区域">{{ detail.region || '-' }}</a-descriptions-item>
              <a-descriptions-item label="合同编号">{{ detail.contractNo || '-' }}</a-descriptions-item>
              <a-descriptions-item label="计划开始">{{ detail.plannedStart || '-' }}</a-descriptions-item>
              <a-descriptions-item label="计划结束">{{ detail.plannedEnd || '-' }}</a-descriptions-item>
              <a-descriptions-item label="实际开始">{{ detail.actualStart || '-' }}</a-descriptions-item>
              <a-descriptions-item label="实际结束">{{ detail.actualEnd || '-' }}</a-descriptions-item>
              <a-descriptions-item label="当前阶段">{{ detail.currentPhase || '-' }}</a-descriptions-item>
              <a-descriptions-item label="项目描述" :span="3">{{ detail.description || '-' }}</a-descriptions-item>
              <a-descriptions-item label="备注" :span="3">{{ detail.remark || '-' }}</a-descriptions-item>
            </a-descriptions>
          </a-tab-pane>

          <!-- 阶段 -->
          <a-tab-pane key="phases" tab="阶段">
            <a-table
              :columns="phaseColumns"
              :data-source="phases"
              row-key="id"
              :pagination="false"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <StatusTag :tone="phaseStatusMap[record.status]?.tone">
                    {{ phaseStatusMap[record.status]?.label || record.status }}
                  </StatusTag>
                </template>
                <template v-else-if="column.key === 'progressPct'">
                  <ProgressBar :percent="record.progressPct || 0" />
                </template>
              </template>
              <template #emptyText><EmptyState description="暂无阶段数据" size="compact" /></template>
            </a-table>
          </a-tab-pane>

          <!-- 任务 -->
          <a-tab-pane key="tasks" tab="任务">
            <div class="tab-toolbar">
              <a-button type="primary" size="small">
                <template #icon><PlusOutlined /></template>
                新增任务
              </a-button>
            </div>
            <a-table
              :columns="taskColumns"
              :data-source="tasks"
              row-key="id"
              :pagination="{ pageSize: 10 }"
              size="small"
              :scroll="{ x: 1100 }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'executeMode'">
                  <a-tag :color="record.executeMode === 'AGENT' ? 'purple' : 'blue'">
                    {{ record.executeMode === 'AGENT' ? '代施' : '自施' }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'status'">
                  <StatusTag :tone="TaskStatusTone[record.status as TaskStatus]">
                    {{ TaskStatusLabel[record.status as TaskStatus] }}
                  </StatusTag>
                </template>
                <template v-else-if="column.key === 'progressPct'">
                  <ProgressBar :percent="record.progressPct || 0" />
                </template>
                <template v-else-if="column.key === 'priority'">
                  {{ PriorityLabel[record.priority as Priority] }}
                </template>
                <template v-else-if="column.key === 'plannedRange'">
                  <span class="text-auxiliary">{{ record.plannedStart || '-' }} ~ {{ record.plannedEnd || '-' }}</span>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a @click="goTask(record.id)">详情</a>
                </template>
              </template>
              <template #emptyText><EmptyState description="暂无任务" size="compact" /></template>
            </a-table>
          </a-tab-pane>

          <!-- 里程碑 -->
          <a-tab-pane key="milestones" tab="里程碑">
            <a-timeline>
              <a-timeline-item
                v-for="m in milestones"
                :key="m.id"
                :color="m.status === 'ACHIEVED' ? 'green' : m.status === 'OVERDUE' ? 'red' : 'blue'"
              >
                <div class="milestone-item">
                  <span class="milestone-name">{{ m.milestoneName }}</span>
                  <span class="milestone-date">计划：{{ m.plannedDate }}</span>
                  <span v-if="m.actualDate" class="milestone-date">实际：{{ m.actualDate }}</span>
                  <StatusTag
                    :tone="m.status === 'ACHIEVED' ? 'success' : m.status === 'OVERDUE' ? 'error' : 'default'"
                    :text="m.status === 'ACHIEVED' ? '已达成' : m.status === 'OVERDUE' ? '已超期' : '待达成'"
                  />
                </div>
                <div v-if="m.deliverables" class="milestone-deliverable">交付物：{{ m.deliverables }}</div>
              </a-timeline-item>
            </a-timeline>
            <EmptyState v-if="!milestones.length" description="暂无里程碑" size="compact" />
          </a-tab-pane>

          <!-- 风险 -->
          <a-tab-pane key="risks" tab="风险">
            <a-table
              :data-source="risks"
              row-key="id"
              :pagination="false"
              size="small"
              :columns="[
                { title: '风险描述', dataIndex: 'riskDesc', key: 'riskDesc', ellipsis: true },
                { title: '影响', dataIndex: 'impact', key: 'impact', width: 80 },
                { title: '概率', dataIndex: 'probability', key: 'probability', width: 80 },
                { title: '应对措施', dataIndex: 'response', key: 'response', ellipsis: true },
                { title: '责任人', dataIndex: 'ownerName', key: 'ownerName', width: 100 },
                { title: '状态', key: 'status', width: 100 },
                { title: '截止', dataIndex: 'dueDate', key: 'dueDate', width: 120 }
              ]"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <StatusTag
                    :tone="record.status === 'CLOSED' ? 'success' : record.status === 'PROCESSING' ? 'processing' : 'warning'"
                    :text="record.status === 'CLOSED' ? '已关闭' : record.status === 'PROCESSING' ? '处理中' : '待处理'"
                  />
                </template>
              </template>
              <template #emptyText><EmptyState description="暂无风险登记" size="compact" /></template>
            </a-table>
          </a-tab-pane>

          <!-- 问题 -->
          <a-tab-pane key="issues" tab="问题">
            <a-table
              :data-source="issues"
              row-key="id"
              :pagination="false"
              size="small"
              :columns="[
                { title: '问题描述', dataIndex: 'issueDesc', key: 'issueDesc', ellipsis: true },
                { title: '影响', dataIndex: 'impact', key: 'impact', ellipsis: true },
                { title: '责任人', dataIndex: 'ownerName', key: 'ownerName', width: 100 },
                { title: '状态', key: 'status', width: 100 },
                { title: '截止', dataIndex: 'dueDate', key: 'dueDate', width: 120 },
                { title: '解决时间', dataIndex: 'resolvedAt', key: 'resolvedAt', width: 150 }
              ]"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <StatusTag
                    :tone="record.status === 'CLOSED' ? 'success' : record.status === 'RESOLVED' ? 'success' : record.status === 'PROCESSING' ? 'processing' : 'warning'"
                    :text="({ OPEN: '待处理', PROCESSING: '处理中', RESOLVED: '已解决', CLOSED: '已关闭' } as Record<string, string>)[record.status]"
                  />
                </template>
              </template>
              <template #emptyText><EmptyState description="暂无问题记录" size="compact" /></template>
            </a-table>
          </a-tab-pane>

          <!-- 变更 -->
          <a-tab-pane key="changes" tab="变更">
            <a-table
              :data-source="changes"
              row-key="id"
              :pagination="false"
              size="small"
              :columns="[
                { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true },
                { title: '类型', dataIndex: 'changeType', key: 'changeType', width: 100 },
                { title: '原因', dataIndex: 'reason', key: 'reason', ellipsis: true },
                { title: '申请人', dataIndex: 'applicantName', key: 'applicantName', width: 100 },
                { title: '审批人', dataIndex: 'approverName', key: 'approverName', width: 100 },
                { title: '状态', key: 'status', width: 100 },
                { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 }
              ]"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <StatusTag
                    :tone="record.status === 'APPROVED' ? 'success' : record.status === 'REJECTED' ? 'error' : record.status === 'EXECUTED' ? 'archived' : 'warning'"
                    :text="({ PENDING: '待审批', APPROVED: '已通过', REJECTED: '已拒绝', EXECUTED: '已执行' } as Record<string, string>)[record.status]"
                  />
                </template>
              </template>
              <template #emptyText><EmptyState description="暂无变更记录" size="compact" /></template>
            </a-table>
          </a-tab-pane>

          <!-- 成员 -->
          <a-tab-pane key="members" tab="成员">
            <a-table
              :data-source="members"
              row-key="id"
              :pagination="false"
              size="small"
              :columns="[
                { title: '用户名', dataIndex: 'userName', key: 'userName' },
                { title: '姓名', dataIndex: 'realName', key: 'realName' },
                { title: '角色', dataIndex: 'role', key: 'role' },
                { title: '加入时间', dataIndex: 'joinedAt', key: 'joinedAt' }
              ]"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'role'">
                  <a-tag>{{ record.role }}</a-tag>
                </template>
              </template>
              <template #emptyText><EmptyState description="暂无成员" size="compact" /></template>
            </a-table>
          </a-tab-pane>

          <!-- 评论 -->
          <a-tab-pane key="comments" tab="评论">
            <div class="comment-input">
              <a-textarea
                v-model:value="commentText"
                :rows="3"
                placeholder="发表评论..."
                :maxlength="500"
                show-count
              />
              <div class="comment-actions">
                <a-button type="primary" :loading="commentSubmitting" @click="submitComment">发布</a-button>
              </div>
            </div>
            <a-list :data-source="comments" item-layout="horizontal">
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta :description="item.content">
                    <template #title>
                      {{ item.userName }}
                      <span class="comment-time">{{ item.createdAt }}</span>
                    </template>
                    <template #avatar>
                      <a-avatar>{{ item.userName?.charAt(0)?.toUpperCase() }}</a-avatar>
                    </template>
                  </a-list-item-meta>
                </a-list-item>
              </template>
              <template #emptyText><EmptyState description="暂无评论，快来发表第一条吧" size="compact" /></template>
            </a-list>
          </a-tab-pane>
        </a-tabs>
      </div>
    </a-spin>
  </PageContainer>
</template>

<style lang="less" scoped>
.detail-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.project-code {
  margin-left: 12px;
  font-size: 13px;
  color: @text-tertiary;
  font-weight: 400;
}
.detail-meta {
  margin: 0;
  font-size: 13px;
  color: @text-tertiary;
}
.overview-row {
  margin-bottom: 16px;
}
.overview-card {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
}
.overview-label {
  font-size: 13px;
  color: @text-tertiary;
}
.overview-value {
  font-size: 16px;
}
.overview-sub {
  margin-left: 4px;
  font-size: 12px;
  color: @text-tertiary;
}
.tab-card {
  padding: 0 20px 16px;
}
.tab-toolbar {
  padding: 12px 0;
  display: flex;
  justify-content: flex-end;
}
.milestone-item {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.milestone-name {
  font-weight: 500;
}
.milestone-date {
  font-size: 12px;
  color: @text-tertiary;
}
.milestone-deliverable {
  margin-top: 4px;
  font-size: 12px;
  color: @text-secondary;
}
.comment-input {
  margin: 12px 0;
}
.comment-actions {
  margin-top: 8px;
  text-align: right;
}
.comment-time {
  margin-left: 12px;
  font-size: 12px;
  color: @text-tertiary;
  font-weight: 400;
}
.text-exception {
  color: @status-exception;
  font-weight: 600;
}
</style>
