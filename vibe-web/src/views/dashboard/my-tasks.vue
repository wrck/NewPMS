<script setup lang="ts">
/**
 * 我的任务
 * 当前用户的任务列表，按状态分类筛选
 */
import { ref, onMounted, computed } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getDashboard } from '@/api/report'
import { TaskStatus, TaskStatusTone, TaskStatusLabel } from '@/types/enum'

interface MyTask {
  id: number
  name: string
  dueDate?: string
  status: string
  projectName?: string
}

const loading = ref(false)
const dataSource = ref<MyTask[]>([])
const statusFilter = ref<string | undefined>(undefined)

async function loadData() {
  loading.value = true
  try {
    const res = (await getDashboard()) as unknown as { myTasks: MyTask[] }
    dataSource.value = res.myTasks || []
  } catch (e) {
    console.error('[dashboard.my-tasks] load failed:', e)
  } finally {
    loading.value = false
  }
}

const filtered = computed(() => {
  if (!statusFilter.value) return dataSource.value
  return dataSource.value.filter((t) => t.status === statusFilter.value)
})

const columns = [
  { title: '任务名称', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '所属项目', dataIndex: 'projectName', key: 'projectName', width: 200, ellipsis: true },
  { title: '截止日期', dataIndex: 'dueDate', key: 'dueDate', width: 130 },
  { title: '状态', key: 'status', width: 110 },
  { title: '操作', key: 'action', width: 100 }
]

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="我的任务" description="我负责或参与的任务列表">
    <template #extra>
      <a-button @click="loadData" :loading="loading"><template #icon><ReloadOutlined /></template>刷新</a-button>
    </template>

    <div class="vibe-card filter-card">
      <a-radio-group v-model:value="statusFilter" button-style="solid">
        <a-radio-button :value="undefined">全部</a-radio-button>
        <a-radio-button v-for="s in [TaskStatus.TODO, TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS, TaskStatus.SUBMITTED, TaskStatus.CONFIRMED]" :key="s" :value="s">{{ TaskStatusLabel[s] }}</a-radio-button>
      </a-radio-group>
    </div>

    <div class="vibe-card table-card">
      <a-table :columns="columns" :data-source="filtered" :loading="loading" row-key="id" :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <StatusTag :tone="TaskStatusTone[record.status as TaskStatus] || 'default'">{{ TaskStatusLabel[record.status as TaskStatus] || record.status }}</StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a>查看</a>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无任务" /></template>
      </a-table>
    </div>
  </PageContainer>
</template>

<style lang="less" scoped>
.filter-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
</style>
