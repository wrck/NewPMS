<script setup lang="ts">
/**
 * 任务派发
 * - 待派发任务列表 + 智能推荐 + 批量派发
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, ThunderboltOutlined, SendOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { pageDispatchTasks, recommendCandidates, batchDispatchTasks, dispatchTask, pageEngineers } from '@/api/resource'
import { pageProjects } from '@/api/project'
import { pageAgentCompanies, listAgentEngineers } from '@/api/agent'
import type { DispatchTask, DispatchCandidate, DispatchQueryParams, DispatchDTO } from '@/types/resource'
import { Priority, PriorityLabel } from '@/types/enum'
import type { PageResult } from '@/types/api'

const loading = ref(false)
const dataSource = ref<DispatchTask[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const query = reactive<DispatchQueryParams>({ projectId: undefined, status: undefined, priority: undefined, unassignedOnly: true })
const selectedRowKeys = ref<number[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = (await pageDispatchTasks({ ...query, page: pagination.current, size: pagination.pageSize })) as unknown as PageResult<DispatchTask>
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[resource.dispatch] load failed:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

const priorityOptions = Object.values(Priority).map((p) => ({ value: p, label: PriorityLabel[p] }))

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[]) => { selectedRowKeys.value = keys }
}))

// ============ 推荐弹窗 ============
const recommendVisible = ref(false)
const recommendLoading = ref(false)
const recommendTask = ref<DispatchTask | null>(null)
const candidates = ref<DispatchCandidate[]>([])

async function openRecommend(row: DispatchTask) {
  recommendTask.value = row
  recommendVisible.value = true
  recommendLoading.value = true
  try {
    candidates.value = (await recommendCandidates(row.id)) || []
  } catch (e) {
    candidates.value = []
  } finally {
    recommendLoading.value = false
  }
}

async function dispatchTo(row: DispatchTask, candidate: DispatchCandidate) {
  try {
    const dto: DispatchDTO = {
      executeMode: 'SELF',
      assigneeId: candidate.engineerId
    }
    await dispatchTask(row.id, dto)
    message.success(`已派发给 ${candidate.engineerName}`)
    recommendVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  }
}

// ============ 派发弹窗 ============
const dispatchVisible = ref(false)
const dispatchLoading = ref(false)
const dispatchForm = reactive<DispatchDTO>({
  executeMode: 'SELF',
  assigneeId: undefined,
  agentCompanyId: undefined,
  agentEngineerId: undefined,
  remark: ''
})

// ============ 下拉选项（实体引用字段） ============
const projectOptions = ref<Array<{ value: string | number; label: string }>>([])
const engineerOptions = ref<Array<{ value: string | number; label: string }>>([])
const agentCompanyOptions = ref<Array<{ value: string | number; label: string }>>([])
const agentEngineerOptions = ref<Array<{ value: string | number; label: string }>>([])

async function loadProjectOptions() {
  try {
    const res = (await pageProjects({ page: 1, size: 200 } as any)) as any
    projectOptions.value = (res?.records || []).map((p: any) => ({ value: p.id, label: p.projectName }))
  } catch (e) {
    console.warn('[resource.dispatch] load projects failed:', e)
  }
}

async function loadEngineerOptions() {
  try {
    const res = (await pageEngineers({ page: 1, size: 200 } as any)) as any
    engineerOptions.value = (res?.records || []).map((e: any) => ({ value: e.id, label: e.name }))
  } catch (e) {
    console.warn('[resource.dispatch] load engineers failed:', e)
  }
}

async function loadAgentCompanyOptions() {
  try {
    const res = (await pageAgentCompanies({ page: 1, size: 200 } as any)) as any
    agentCompanyOptions.value = (res?.records || []).map((c: any) => ({ value: c.id, label: c.companyName }))
  } catch (e) {
    console.warn('[resource.dispatch] load agent companies failed:', e)
  }
}

async function loadAgentEngineerOptions(companyId?: string | number) {
  if (!companyId) {
    agentEngineerOptions.value = []
    return
  }
  try {
    const list = (await listAgentEngineers(companyId as any)) as any
    agentEngineerOptions.value = (list || []).map((e: any) => ({ value: e.id, label: e.name }))
  } catch (e) {
    console.warn('[resource.dispatch] load agent engineers failed:', e)
    agentEngineerOptions.value = []
  }
}

function handleAgentCompanyChange(value?: string | number) {
  dispatchForm.agentEngineerId = undefined
  loadAgentEngineerOptions(value)
}

function openDispatch(row?: DispatchTask) {
  Object.assign(dispatchForm, {
    executeMode: 'SELF',
    assigneeId: undefined,
    agentCompanyId: undefined,
    agentEngineerId: undefined,
    remark: ''
  })
  if (row) {
    selectedRowKeys.value = [row.id]
  }
  dispatchVisible.value = true
  loadEngineerOptions()
  loadAgentCompanyOptions()
  if (dispatchForm.agentCompanyId) {
    loadAgentEngineerOptions(dispatchForm.agentCompanyId)
  } else {
    agentEngineerOptions.value = []
  }
}

async function handleDispatch() {
  if (!selectedRowKeys.value.length) {
    message.warning('请选择任务')
    return
  }
  if (dispatchForm.executeMode === 'SELF' && !dispatchForm.assigneeId) {
    message.warning('请选择工程师')
    return
  }
  dispatchLoading.value = true
  try {
    if (selectedRowKeys.value.length === 1) {
      await dispatchTask(selectedRowKeys.value[0], dispatchForm)
    } else {
      await batchDispatchTasks({
        taskIds: selectedRowKeys.value,
        executeMode: dispatchForm.executeMode,
        assigneeId: dispatchForm.assigneeId,
        agentCompanyId: dispatchForm.agentCompanyId,
        agentEngineerId: dispatchForm.agentEngineerId,
        remark: dispatchForm.remark
      })
    }
    message.success('派发成功')
    dispatchVisible.value = false
    selectedRowKeys.value = []
    loadData()
  } catch (e) {
    // ignore
  } finally {
    dispatchLoading.value = false
  }
}

const columns = [
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName', ellipsis: true },
  { title: '所属项目', dataIndex: 'projectName', key: 'projectName', width: 150, ellipsis: true },
  { title: '阶段', dataIndex: 'phaseName', key: 'phaseName', width: 100 },
  { title: '优先级', key: 'priority', width: 80 },
  { title: '执行模式', key: 'executeMode', width: 90 },
  { title: '计划周期', key: 'plannedRange', width: 200 },
  { title: '区域', dataIndex: 'region', key: 'region', width: 90 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' }
]

onMounted(() => {
  loadData()
  loadProjectOptions()
})
</script>

<template>
  <PageContainer title="任务派发" description="待派发任务列表 + 智能推荐 + 批量派单">
    <template #extra>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" :disabled="!selectedRowKeys.length" @click="openDispatch()">
        <template #icon><SendOutlined /></template>
        批量派发 ({{ selectedRowKeys.length }})
      </a-button>
    </template>

    <div class="vibe-card search-card">
      <a-form layout="inline" :model="query" @submit.prevent="handleSearch">
        <a-form-item label="项目">
          <a-select
            v-model:value="query.projectId"
            placeholder="选择项目"
            allow-clear
            show-search
            style="width: 180px"
            :options="projectOptions"
            :filter-option="(input: string, option: any) => option.label.includes(input)"
          />
        </a-form-item>
        <a-form-item label="优先级">
          <a-select v-model:value="query.priority" placeholder="全部" allow-clear style="width: 110px" :options="priorityOptions" />
        </a-form-item>
        <a-form-item label="仅未派发">
          <a-switch v-model:checked="query.unassignedOnly" />
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
        :row-selection="rowSelection"
        :scroll="{ x: 1200 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'priority'">
            <a-tag :color="record.priority === 'URGENT' ? 'red' : record.priority === 'HIGH' ? 'orange' : 'default'">
              {{ PriorityLabel[record.priority as Priority] }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'executeMode'">
            <a-tag :color="record.executeMode === 'AGENT' ? 'purple' : 'blue'">{{ record.executeMode === 'AGENT' ? '代施' : '自施' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'plannedRange'">
            <span class="text-auxiliary">{{ record.plannedStart || '-' }} ~ {{ record.plannedEnd || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a @click="openRecommend(record)"><ThunderboltOutlined /> 智能推荐</a>
              <a-divider type="vertical" />
              <a @click="openDispatch(record)">派发</a>
            </a-space>
          </template>
        </template>
        <template #emptyText><EmptyState description="暂无待派发任务" size="compact" /></template>
      </a-table>
    </div>

    <!-- 智能推荐弹窗 -->
    <a-modal v-model:open="recommendVisible" title="智能推荐工程师" width="680px" :footer="null">
      <a-spin :spinning="recommendLoading">
        <a-alert v-if="recommendTask" :message="`任务：${recommendTask.taskName}`" type="info" show-icon style="margin-bottom: 16px" />
        <a-list :data-source="candidates" item-layout="horizontal">
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta>
                <template #title>
                  <span style="font-weight: 500">{{ item.engineerName }}</span>
                  <a-tag color="green" style="margin-left: 8px">匹配度 {{ item.matchScore }}%</a-tag>
                </template>
                <template #description>
                  <div>
                    <span class="text-auxiliary">区域：{{ item.region || '-' }} · 利用率：{{ item.utilization }}% · 进行中任务：{{ item.ongoingTaskCount }}</span>
                  </div>
                  <div v-if="item.matchReasons?.length" style="margin-top: 4px">
                    <a-tag v-for="(r, i) in item.matchReasons" :key="i" color="blue">{{ r }}</a-tag>
                  </div>
                </template>
                <template #avatar>
                  <a-avatar style="background-color: #1677ff">{{ item.engineerName?.charAt(0) }}</a-avatar>
                </template>
              </a-list-item-meta>
              <template #actions>
                <a-button type="primary" size="small" @click="recommendTask && dispatchTo(recommendTask, item)">派给他</a-button>
              </template>
            </a-list-item>
          </template>
          <template #emptyText><EmptyState description="暂无推荐候选" size="compact" /></template>
        </a-list>
      </a-spin>
    </a-modal>

    <!-- 派发弹窗 -->
    <a-modal v-model:open="dispatchVisible" title="任务派发" :confirm-loading="dispatchLoading" @ok="handleDispatch">
      <a-alert :message="`已选 ${selectedRowKeys.length} 个任务`" type="info" show-icon style="margin-bottom: 16px" />
      <a-form layout="vertical">
        <a-form-item label="执行模式">
          <a-radio-group v-model:value="dispatchForm.executeMode">
            <a-radio value="SELF">自有工程师</a-radio>
            <a-radio value="AGENT">代理商代施</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="dispatchForm.executeMode === 'SELF'" label="工程师">
          <a-select
            v-model:value="dispatchForm.assigneeId"
            show-search
            placeholder="选择工程师"
            style="width: 100%"
            :options="engineerOptions"
            :filter-option="(input: string, option: any) => option.label.includes(input)"
          />
        </a-form-item>
        <template v-else>
          <a-form-item label="代理商公司">
            <a-select
              v-model:value="dispatchForm.agentCompanyId"
              show-search
              placeholder="选择代理商公司"
              style="width: 100%"
              :options="agentCompanyOptions"
              :filter-option="(input: string, option: any) => option.label.includes(input)"
              @change="handleAgentCompanyChange"
            />
          </a-form-item>
          <a-form-item label="代理商工程师">
            <a-select
              v-model:value="dispatchForm.agentEngineerId"
              show-search
              placeholder="选择代理商工程师"
              style="width: 100%"
              :options="agentEngineerOptions"
              :filter-option="(input: string, option: any) => option.label.includes(input)"
            />
          </a-form-item>
        </template>
        <a-form-item label="备注">
          <a-textarea v-model:value="dispatchForm.remark" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<style lang="less" scoped>
.search-card { padding: 16px 20px; margin-bottom: 16px; }
.table-card { padding: 0; }
</style>
