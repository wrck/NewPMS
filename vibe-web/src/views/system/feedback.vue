<script setup lang="ts">
/**
 * 反馈管理（Task D5.3）
 *
 * 仅管理员可见（路由已配置 roles: ['SUPER_ADMIN']）。
 *
 * 功能：
 *   - 顶部筛选区：类型 / 状态 / 标题关键字
 *   - 主列表：分页展示全部反馈
 *   - 处理抽屉：管理员变更状态 + 填写处理备注，提交后通过站内信通知提交人
 *   - 「我的反馈」可切换查看（非管理员也可读，但本页为管理员视图）
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { message, Modal as AModal } from 'ant-design-vue'
import {
  ReloadOutlined,
  EyeOutlined,
  CheckOutlined,
  BugOutlined,
  BulbOutlined,
  QuestionOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import type { StatusTone } from '@/styles/theme'
import { pageFeedback, handleFeedback } from '@/api/feedback'
import type {
  SysFeedback,
  SysFeedbackQueryParams,
  FeedbackType,
  FeedbackStatus
} from '@/api/feedback'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<SysFeedback[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50']
})

const query = reactive<SysFeedbackQueryParams>({
  type: undefined,
  status: undefined,
  keyword: ''
})

async function loadData() {
  loading.value = true
  try {
    const res = (await pageFeedback({
      ...query,
      page: pagination.current,
      size: pagination.pageSize
    })) as unknown as PageResult<SysFeedback>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[system.feedback] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  Object.assign(query, { type: undefined, status: undefined, keyword: '' })
  handleSearch()
}

function handleTableChange(p: { current?: number; pageSize?: number }) {
  if (p.current) pagination.current = p.current
  if (p.pageSize) pagination.pageSize = p.pageSize
  loadData()
}

/* ============ 类型 / 状态展示 ============ */
const typeLabel: Record<FeedbackType, string> = {
  BUG: 'Bug',
  SUGGESTION: '建议',
  QUESTION: '疑问'
}

const typeTone: Record<FeedbackType, StatusTone> = {
  BUG: 'error',
  SUGGESTION: 'processing',
  QUESTION: 'warning'
}

const typeIcon: Record<FeedbackType, unknown> = {
  BUG: BugOutlined,
  SUGGESTION: BulbOutlined,
  QUESTION: QuestionOutlined
}

const statusLabel: Record<FeedbackStatus, string> = {
  PENDING: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已解决',
  CLOSED: '已关闭'
}

const statusTone: Record<FeedbackStatus, StatusTone> = {
  PENDING: 'warning',
  PROCESSING: 'processing',
  RESOLVED: 'success',
  CLOSED: 'default'
}

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '类型', key: 'type', width: 100 },
  { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '提交人', dataIndex: 'submitterName', key: 'submitterName', width: 120 },
  { title: '状态', key: 'status', width: 110 },
  { title: '提交时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '处理人', dataIndex: 'handlerName', key: 'handlerName', width: 110 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const }
]

/* ============ 详情查看 ============ */
const detailVisible = ref(false)
const currentRow = ref<SysFeedback | null>(null)

function viewDetail(row: SysFeedback) {
  currentRow.value = row
  detailVisible.value = true
}

/* ============ 处理反馈 ============ */
const handleVisible = ref(false)
const handleLoading = ref(false)
const handleForm = reactive<{
  status: 'PROCESSING' | 'RESOLVED' | 'CLOSED'
  handleNote: string
}>({
  status: 'PROCESSING',
  handleNote: ''
})

const statusOptions: Array<{ label: string; value: 'PROCESSING' | 'RESOLVED' | 'CLOSED'; tone: string }> = [
  { label: '处理中', value: 'PROCESSING', tone: 'processing' },
  { label: '已解决', value: 'RESOLVED', tone: 'success' },
  { label: '已关闭', value: 'CLOSED', tone: 'default' }
]

function openHandle(row: SysFeedback) {
  currentRow.value = row
  // 默认设置：当前为 PENDING 时设为 PROCESSING；其他保持原状态可调整
  handleForm.status = row.status === 'PENDING' ? 'PROCESSING' : (row.status as 'PROCESSING' | 'RESOLVED' | 'CLOSED')
  handleForm.handleNote = row.handleNote || ''
  handleVisible.value = true
}

async function submitHandle() {
  if (!currentRow.value?.id) return
  if (handleForm.handleNote.length > 1000) {
    message.warning('处理备注长度不能超过 1000')
    return
  }
  handleLoading.value = true
  try {
    await handleFeedback(currentRow.value.id, {
      status: handleForm.status,
      handleNote: handleForm.handleNote.trim() || undefined
    })
    message.success('处理成功，已通过站内信通知提交人')
    handleVisible.value = false
    loadData()
  } catch (e) {
    console.error('[system.feedback] handle failed:', e)
  } finally {
    handleLoading.value = false
  }
}

/* ============ 截图预览 ============ */
const previewImages = computed(() => {
  if (!currentRow.value?.screenshotUrl) return []
  return currentRow.value.screenshotUrl.split(',').filter(Boolean)
})

/* ============ 工具方法 ============ */
function formatTime(t?: string): string {
  if (!t) return '-'
  return dayjs(t).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="反馈管理" description="用户反馈与工单处理：查看详情、变更状态、记录处理结果并通知提交人">
    <template #extra>
      <a-button @click="handleReset"><template #icon><ReloadOutlined /></template>重置</a-button>
      <a-button type="primary" @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="标题">
          <a-input
            v-model:value="query.keyword"
            placeholder="标题关键字"
            allow-clear
            style="width: 220px"
            @pressEnter="handleSearch"
          />
        </a-form-item>
        <a-form-item label="类型">
          <a-select v-model:value="query.type" placeholder="全部" allow-clear style="width: 120px">
            <a-select-option value="BUG">Bug 反馈</a-select-option>
            <a-select-option value="SUGGESTION">功能建议</a-select-option>
            <a-select-option value="QUESTION">使用疑问</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width: 120px">
            <a-select-option value="PENDING">待处理</a-select-option>
            <a-select-option value="PROCESSING">处理中</a-select-option>
            <a-select-option value="RESOLVED">已解决</a-select-option>
            <a-select-option value="CLOSED">已关闭</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="vibe-card table-card">
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1200 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <StatusTag :tone="typeTone[record.type as FeedbackType]">
              <component :is="typeIcon[record.type as FeedbackType]" />
              <span style="margin-left: 4px">{{ typeLabel[record.type as FeedbackType] || record.type }}</span>
            </StatusTag>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusTag :tone="statusTone[record.status as FeedbackStatus]">
              {{ statusLabel[record.status as FeedbackStatus] || record.status }}
            </StatusTag>
          </template>
          <template v-else-if="column.key === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'submitterName'">
            {{ record.submitterName || '-' }}
          </template>
          <template v-else-if="column.key === 'handlerName'">
            {{ record.handlerName || '-' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a @click="viewDetail(record as SysFeedback)"><EyeOutlined /> 详情</a>
              <a v-if="record.status !== 'CLOSED'" type="primary" @click="openHandle(record as SysFeedback)">
                <CheckOutlined /> 处理
              </a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无反馈记录" /></template>
      </a-table>
    </div>

    <!-- ============ 详情抽屉 ============ -->
    <a-drawer
      v-model:open="detailVisible"
      title="反馈详情"
      placement="right"
      width="520"
    >
      <div v-if="currentRow" class="feedback-detail">
        <div class="detail-row">
          <span class="detail-label">类型</span>
          <StatusTag :tone="typeTone[currentRow.type]">
            {{ typeLabel[currentRow.type] || currentRow.type }}
          </StatusTag>
        </div>
        <div class="detail-row">
          <span class="detail-label">状态</span>
          <StatusTag :tone="statusTone[currentRow.status]">
            {{ statusLabel[currentRow.status] || currentRow.status }}
          </StatusTag>
        </div>
        <div class="detail-row">
          <span class="detail-label">标题</span>
          <span class="detail-value">{{ currentRow.title }}</span>
        </div>
        <div class="detail-row vertical">
          <span class="detail-label">详细描述</span>
          <pre class="detail-pre">{{ currentRow.content || '（无）' }}</pre>
        </div>
        <div class="detail-row vertical">
          <span class="detail-label">截图</span>
          <div v-if="previewImages.length > 0" class="detail-images">
            <a
              v-for="(img, idx) in previewImages"
              :key="idx"
              :href="img"
              target="_blank"
              class="detail-image-link"
            >
              <img :src="img" :alt="`截图${idx + 1}`" class="detail-image" />
            </a>
          </div>
          <span v-else class="detail-value">（无）</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">联系方式</span>
          <span class="detail-value">{{ currentRow.contact || '（无）' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">提交人</span>
          <span class="detail-value">{{ currentRow.submitterName || `用户#${currentRow.submitterId}` }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">提交时间</span>
          <span class="detail-value">{{ formatTime(currentRow.createTime) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">处理人</span>
          <span class="detail-value">{{ currentRow.handlerName || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">处理时间</span>
          <span class="detail-value">{{ formatTime(currentRow.handleTime) }}</span>
        </div>
        <div class="detail-row vertical">
          <span class="detail-label">处理备注</span>
          <pre class="detail-pre">{{ currentRow.handleNote || '（无）' }}</pre>
        </div>

        <div class="detail-actions">
          <a-button v-if="currentRow.status !== 'CLOSED'" type="primary" @click="openHandle(currentRow)">
            <template #icon><CheckOutlined /></template>
            处理反馈
          </a-button>
        </div>
      </div>
    </a-drawer>

    <!-- ============ 处理反馈 Modal ============ -->
    <a-modal
      v-model:open="handleVisible"
      title="处理反馈"
      width="540px"
      :confirm-loading="handleLoading"
      ok-text="提交处理"
      cancel-text="取消"
      @ok="submitHandle"
    >
      <div v-if="currentRow" class="handle-modal-body">
        <div class="handle-tip">
          <StatusTag :tone="typeTone[currentRow.type]">
            {{ typeLabel[currentRow.type] || currentRow.type }}
          </StatusTag>
          <span class="handle-tip-title">{{ currentRow.title }}</span>
        </div>

        <a-form layout="vertical">
          <a-form-item label="目标状态" required>
            <a-radio-group v-model:value="handleForm.status" button-style="solid">
              <a-radio-button
                v-for="opt in statusOptions"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </a-radio-button>
            </a-radio-group>
          </a-form-item>

          <a-form-item label="处理备注" :max-length="1000">
            <a-textarea
              v-model:value="handleForm.handleNote"
              :rows="5"
              placeholder="说明处理结果、原因或后续计划，提交后将通过站内信通知提交人"
              :max-length="1000"
              show-count
            />
          </a-form-item>
        </a-form>

        <div class="handle-notice">
          <span class="handle-notice-icon">!</span>
          <span>提交后系统将向提交人发送站内信通知，告知状态变更与处理备注。</span>
        </div>
      </div>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card {
  padding: 16px 20px;
  margin-bottom: 16px;
}
.table-card {
  padding: 0;
}

.feedback-detail {
  padding: 4px 0;

  .detail-row {
    display: flex;
    align-items: center;
    padding: 8px 0;
    border-bottom: 1px dashed #f0f0f0;

    &.vertical {
      flex-direction: column;
      align-items: flex-start;

      .detail-label {
        margin-bottom: 8px;
      }
    }

    .detail-label {
      flex-shrink: 0;
      width: 90px;
      color: @text-tertiary;
      font-size: 13px;
    }

    .detail-value {
      flex: 1;
      color: @text-primary;
      word-break: break-all;
    }

    .detail-pre {
      width: 100%;
      margin: 0;
      padding: 8px 12px;
      background: @bg-sub;
      border-radius: 4px;
      color: @text-primary;
      font-family: inherit;
      font-size: 13px;
      line-height: 1.6;
      white-space: pre-wrap;
      word-break: break-all;
    }
  }

  .detail-images {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 4px;
  }

  .detail-image-link {
    display: inline-block;
    width: 96px;
    height: 96px;
    border: 1px solid #f0f0f0;
    border-radius: 4px;
    overflow: hidden;
  }

  .detail-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    cursor: pointer;
    transition: transform 0.2s;

    &:hover {
      transform: scale(1.05);
    }
  }

  .detail-actions {
    margin-top: 16px;
    text-align: right;
  }
}

.handle-modal-body {
  padding: 4px 0;
}

.handle-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: @bg-sub;
  border-radius: 4px;
  margin-bottom: 16px;

  &-title {
    flex: 1;
    color: @text-primary;
    font-weight: 500;
    word-break: break-all;
  }
}

.handle-notice {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 8px;
  padding: 10px 12px;
  background: rgba(@status-warning, 0.08);
  border-left: 3px solid @status-warning;
  border-radius: 4px;
  font-size: 12px;
  color: @text-secondary;

  &-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: @status-warning;
    color: #fff;
    font-weight: 600;
    font-size: 12px;
    flex-shrink: 0;
  }
}
</style>
