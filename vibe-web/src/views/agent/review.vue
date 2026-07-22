<script setup lang="ts">
/**
 * 交付审核
 * PM 审核代理商提交的转包任务：查看交付物、确认通过或退回
 */
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined,
  CheckOutlined,
  CloseOutlined,
  PaperClipOutlined,
  EyeOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  pageOutsourceTasks,
  confirmOutsourceTask,
  rejectOutsourceTask,
  listDeliverables,
  pageAgentCompanies
} from '@/api/agent'
import { pageProjects } from '@/api/project'
import type {
  OutsourceTask,
  OutsourceTaskQueryParams,
  OutsourceDeliverable
} from '@/types/agent'
import { OutsourceStatus, OutsourceStatusTone, OutsourceStatusLabel } from '@/types/enum'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<OutsourceTask[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
// 默认筛选已提交待审核
const query = reactive<OutsourceTaskQueryParams>({ projectId: undefined, agentCompanyId: undefined, status: OutsourceStatus.SUBMITTED, startBegin: '', startEnd: '' })

async function loadData() {
  loading.value = true
  try {
    const res = (await pageOutsourceTasks({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<OutsourceTask>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[agent.review] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

// ============ 实体引用下拉选项 ============
const projectOptions = ref<Array<{ value: string | number; label: string }>>([])
const agentCompanyOptions = ref<Array<{ value: string | number; label: string }>>([])

async function loadProjectOptions() {
  try {
    const res = await pageProjects({ page: 1, size: 200 } as any)
    const list = (res as any)?.records || []
    projectOptions.value = list.map((p: any) => ({ value: p.id, label: p.projectName }))
  } catch (e) {
    console.warn('[agent.review] load projects failed:', e)
  }
}

async function loadAgentCompanyOptions() {
  try {
    const res = await pageAgentCompanies({ page: 1, size: 200 })
    const list = (res as any)?.records || []
    agentCompanyOptions.value = list.map((c: any) => ({ value: c.id, label: c.companyName }))
  } catch (e) {
    console.warn('[agent.review] load agent companies failed:', e)
  }
}

const columns = [
  { title: '任务编码', dataIndex: 'taskCode', key: 'taskCode', width: 140 },
  { title: '所属项目', dataIndex: 'projectName', key: 'projectName', ellipsis: true },
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName', width: 160, ellipsis: true },
  { title: '代理商', dataIndex: 'agentCompanyName', key: 'agentCompanyName', width: 150, ellipsis: true },
  { title: '执行工程师', dataIndex: 'agentEngineerName', key: 'agentEngineerName', width: 110 },
  { title: '截止日期', dataIndex: 'deadline', key: 'deadline', width: 120 },
  { title: '提交次数', dataIndex: 'submitCount', key: 'submitCount', width: 90 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' }
]

// 审核弹窗
const reviewVisible = ref(false)
const reviewLoading = ref(false)
const reviewRow = ref<OutsourceTask | null>(null)
const reviewForm = reactive({ approved: true, remark: '' })

function openReview(row: OutsourceTask, approved: boolean) {
  reviewRow.value = row
  reviewForm.approved = approved
  reviewForm.remark = row.rejectReason || ''
  reviewVisible.value = true
}

async function handleReview() {
  if (!reviewRow.value) return
  if (!reviewForm.approved && !reviewForm.remark) {
    message.warning('请填写退回原因')
    return
  }
  reviewLoading.value = true
  try {
    if (reviewForm.approved) {
      await confirmOutsourceTask(reviewRow.value.id, reviewForm.remark)
      message.success('已确认通过')
    } else {
      await rejectOutsourceTask(reviewRow.value.id, reviewForm.remark)
      message.success('已退回')
    }
    reviewVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    reviewLoading.value = false
  }
}

// 交付物查看
const deliverableVisible = ref(false)
const deliverableList = ref<OutsourceDeliverable[]>([])
const deliverableLoading = ref(false)
const deliverableRow = ref<OutsourceTask | null>(null)

async function openDeliverables(row: OutsourceTask) {
  deliverableRow.value = row
  deliverableVisible.value = true
  deliverableLoading.value = true
  try {
    deliverableList.value = (await listDeliverables(row.id)) as unknown as OutsourceDeliverable[]
  } catch (e) {
    console.error('[agent.review] load deliverables failed:', e)
  } finally {
    deliverableLoading.value = false
  }
}

const deliverableTypeLabel: Record<string, string> = {
  PHOTO: '现场照片',
  TEST_RECORD: '测试记录',
  SIGN_OFF: '签收单',
  CONFIG: '配置文件',
  OTHER: '其他'
}

const deliverableColumns = [
  { title: '类型', key: 'deliverableType', width: 110 },
  { title: '名称', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '上传人', dataIndex: 'uploadedByName', key: 'uploadedByName', width: 110 },
  { title: '上传时间', dataIndex: 'uploadedAt', key: 'uploadedAt', width: 160 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 90 }
]

onMounted(() => {
  loadData()
  loadProjectOptions()
  loadAgentCompanyOptions()
})
</script>

<template>
  <PageContainer title="交付审核" description="审核代理商提交的转包任务交付物，确认通过或退回">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="项目">
          <a-select
            v-model:value="query.projectId"
            show-search
            allow-clear
            placeholder="选择项目"
            style="width: 180px"
            :options="projectOptions"
            :filter-option="(input: string, option: any) => option.label.includes(input)"
          />
        </a-form-item>
        <a-form-item label="代理商">
          <a-select
            v-model:value="query.agentCompanyId"
            show-search
            allow-clear
            placeholder="选择代理商"
            style="width: 180px"
            :options="agentCompanyOptions"
            :filter-option="(input: string, option: any) => option.label.includes(input)"
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 130px">
            <a-select-option v-for="s in Object.values(OutsourceStatus)" :key="s" :value="s">{{ OutsourceStatusLabel[s] }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="vibe-card table-card">
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" :scroll="{ x: 1400 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <StatusTag :tone="OutsourceStatusTone[record.status as OutsourceStatus]">{{ OutsourceStatusLabel[record.status as OutsourceStatus] || record.status }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'submitCount'">
            <span class="tnum">{{ record.submitCount || 0 }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a @click="openDeliverables(record)"><PaperClipOutlined /> 交付物</a>
              <a v-if="record.status === 'SUBMITTED'" @click="openReview(record, true)"><CheckOutlined /> 通过</a>
              <a v-if="record.status === 'SUBMITTED'" class="danger-link" @click="openReview(record, false)"><CloseOutlined /> 退回</a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无待审核任务" /></template>
      </a-table>
    </div>

    <!-- 审核弹窗 -->
    <a-modal v-model:open="reviewVisible" :title="reviewForm.approved ? '确认通过' : '退回任务'" :confirm-loading="reviewLoading" @ok="handleReview">
      <a-alert v-if="reviewForm.approved" :message="`确认任务「${reviewRow?.taskName || ''}」交付通过？`" type="info" show-icon style="margin-bottom: 12px" />
      <a-alert v-else message="退回后任务将回到代理商继续处理" type="warning" show-icon style="margin-bottom: 12px" />
      <a-form layout="vertical">
        <a-form-item :label="reviewForm.approved ? '审核意见' : '退回原因'" :required="!reviewForm.approved">
          <a-textarea v-model:value="reviewForm.remark" :rows="3" :placeholder="reviewForm.approved ? '可填写审核意见' : '请说明退回原因'" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 交付物 -->
    <a-modal v-model:open="deliverableVisible" :title="`交付物 - ${deliverableRow?.taskName || ''}`" :footer="null" width="780px">
      <a-table :columns="deliverableColumns" :data-source="deliverableList" :loading="deliverableLoading" row-key="id" size="small" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'deliverableType'">
            <a-tag>{{ deliverableTypeLabel[record.deliverableType] || record.deliverableType }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a :href="record.url" target="_blank"><EyeOutlined /> 查看</a>
          </template>
        </template>
      </a-table>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
.danger-link { color: @status-exception; }
</style>
