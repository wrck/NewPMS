<script setup lang="ts">
/**
 * 交付看板
 * 按工单状态分组展示，支持按项目/执行方式过滤
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { pageWorkOrders } from '@/api/delivery'
import type { WorkOrder, WorkOrderQueryParams } from '@/types/delivery'
import { TaskStatus, TaskStatusTone, TaskStatusLabel, Priority, PriorityLabel } from '@/types/enum'

const loading = ref(false)
const allData = ref<WorkOrder[]>([])
const query = reactive<Pick<WorkOrderQueryParams, 'projectId' | 'executeMode'>>({
  projectId: undefined,
  executeMode: undefined
})

// 看板列定义（按状态机顺序）
const columns = [
  { status: TaskStatus.TODO, label: TaskStatusLabel[TaskStatus.TODO], tone: TaskStatusTone[TaskStatus.TODO] },
  { status: TaskStatus.ASSIGNED, label: TaskStatusLabel[TaskStatus.ASSIGNED], tone: TaskStatusTone[TaskStatus.ASSIGNED] },
  { status: TaskStatus.IN_PROGRESS, label: TaskStatusLabel[TaskStatus.IN_PROGRESS], tone: TaskStatusTone[TaskStatus.IN_PROGRESS] },
  { status: TaskStatus.SUBMITTED, label: TaskStatusLabel[TaskStatus.SUBMITTED], tone: TaskStatusTone[TaskStatus.SUBMITTED] },
  { status: TaskStatus.CONFIRMED, label: TaskStatusLabel[TaskStatus.CONFIRMED], tone: TaskStatusTone[TaskStatus.CONFIRMED] }
]

const executeModeLabel: Record<string, string> = { SELF: '自营', AGENT: '代理' }
const priorityTone: Record<Priority, any> = {
  LOW: 'default', MEDIUM: 'processing', HIGH: 'warning', URGENT: 'error'
}

async function loadData() {
  loading.value = true
  try {
    // 一次拉取较大数据量用于看板分组
    const res = await pageWorkOrders({ ...query, page: 1, size: 200 } as WorkOrderQueryParams) as unknown as { records: WorkOrder[] }
    allData.value = res.records || []
  } catch (e) {
    console.error('[delivery.board] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

// 按状态分组
const grouped = computed(() => {
  const map: Record<string, WorkOrder[]> = {}
  columns.forEach((c) => (map[c.status] = []))
  allData.value.forEach((item) => {
    if (map[item.status as TaskStatus]) {
      map[item.status as TaskStatus].push(item)
    }
  })
  return map
})

// 统计卡片
const stats = computed(() => {
  const total = allData.value.length
  const done = allData.value.filter((d) => d.status === TaskStatus.CONFIRMED).length
  const ongoing = allData.value.filter((d) => [TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS].includes(d.status as TaskStatus)).length
  const pending = allData.value.filter((d) => d.status === TaskStatus.TODO).length
  return { total, done, ongoing, pending }
})

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="交付看板" description="按工单状态分组展示，全局跟踪交付进度">
    <template #extra>
      <a-button @click="loadData" :loading="loading"><template #icon><ReloadOutlined /></template>刷新</a-button>
    </template>

    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="vibe-card stat-card">
        <div class="stat-label">工单总数</div>
        <div class="stat-value tnum">{{ stats.total }}</div>
      </div>
      <div class="vibe-card stat-card">
        <div class="stat-label">待派发</div>
        <div class="stat-value tnum text-auxiliary">{{ stats.pending }}</div>
      </div>
      <div class="vibe-card stat-card">
        <div class="stat-label">进行中</div>
        <div class="stat-value tnum text-processing">{{ stats.ongoing }}</div>
      </div>
      <div class="vibe-card stat-card">
        <div class="stat-label">已完成</div>
        <div class="stat-value tnum text-success">{{ stats.done }}</div>
      </div>
    </div>

    <!-- 过滤 -->
    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="项目ID">
          <a-input-number v-model:value="query.projectId" placeholder="项目ID" style="width: 140px" />
        </a-form-item>
        <a-form-item label="执行方式">
          <a-select v-model:value="query.executeMode" placeholder="全部" allow-clear style="width: 130px">
            <a-select-option v-for="(v, k) in executeModeLabel" :key="k" :value="k">{{ v }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">筛选</a-button>
        </a-form-item>
      </a-form>
    </div>

    <!-- 看板 -->
    <div class="kanban" v-if="stats.total">
      <div v-for="col in columns" :key="col.status" class="kanban-col">
        <div class="kanban-col-header">
          <StatusTag :tone="col.tone">{{ col.label }}</StatusTag>
          <span class="count tnum">{{ grouped[col.status]?.length || 0 }}</span>
        </div>
        <div class="kanban-body">
          <EmptyState v-if="!grouped[col.status]?.length" description="无" size="compact" />
          <div v-for="item in grouped[col.status]" :key="item.id" class="kanban-card">
            <div class="card-title">
              <span class="card-no">{{ item.workOrderNo }}</span>
              <StatusTag :tone="priorityTone[item.priority as Priority]">{{ PriorityLabel[item.priority as Priority] }}</StatusTag>
            </div>
            <div class="card-name">{{ item.workOrderName }}</div>
            <div class="card-meta">
              <span class="text-auxiliary">{{ item.projectName || '-' }}</span>
            </div>
            <div class="card-meta">
              <span class="text-auxiliary">{{ item.engineerName || item.agentEngineerName || '未指派' }}</span>
              <a-tag :color="item.executeMode === 'AGENT' ? 'purple' : 'blue'" style="margin: 0">{{ executeModeLabel[item.executeMode] }}</a-tag>
            </div>
            <div class="card-footer">
              <span class="text-auxiliary">{{ item.plannedStart || '-' }}</span>
              <span v-if="item.stepProgress" class="text-auxiliary">步骤 {{ item.stepProgress.completed }}/{{ item.stepProgress.total }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="vibe-card empty-wrap">
      <EmptyState description="暂无工单数据" />
    </div>
  </PageContainer>
</template>

<style lang="less" scoped>
.stat-row {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 16px;
}
.stat-card {
  padding: 16px 20px;
  .stat-label { color: @text-auxiliary; font-size: 13px; }
  .stat-value { font-size: 28px; font-weight: 600; margin-top: 4px; }
}
.text-auxiliary { color: @text-auxiliary; }
.text-processing { color: @brand-primary; }
.text-success { color: @status-success; }

.search-card { padding: 16px 20px; margin-bottom: 16px; }

.kanban {
  display: flex; gap: 12px; overflow-x: auto; padding-bottom: 8px;
}
.kanban-col {
  flex: 0 0 280px; background: @bg-page; border-radius: 8px;
  display: flex; flex-direction: column; max-height: calc(100vh - 320px);
}
.kanban-col-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-bottom: 1px solid @border-color-split;
  .count { color: @text-auxiliary; font-size: 14px; }
}
.kanban-body {
  flex: 1; overflow-y: auto; padding: 12px;
}
.kanban-card {
  background: #fff; border-radius: 6px; padding: 12px; margin-bottom: 8px;
  border: 1px solid @border-color-split; transition: box-shadow 0.2s;
  &:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
  .card-title {
    display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;
    .card-no { font-size: 12px; color: @text-auxiliary; }
  }
  .card-name { font-weight: 500; margin-bottom: 6px; }
  .card-meta {
    display: flex; justify-content: space-between; align-items: center;
    font-size: 12px; margin-bottom: 4px;
  }
  .card-footer {
    display: flex; justify-content: space-between;
    font-size: 12px; margin-top: 6px; padding-top: 6px;
    border-top: 1px dashed @border-color-split;
  }
}
.empty-wrap { padding: 40px 0; }
</style>
