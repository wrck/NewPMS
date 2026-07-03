<template>
  <div class="task-list-view">
    <van-tabs v-model:active="activeStatus" sticky @change="onTabChange">
      <van-tab v-for="t in tabs" :key="t.value" :title="t.label" :name="t.value" />
    </van-tabs>

    <van-search v-model="keyword" placeholder="搜索任务/站点名称" shape="round" @search="onSearch" />

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh" class="list-wrap">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <ul class="list">
          <li v-for="t in list" :key="t.id">
            <TaskCard
              :task="t"
              @click="goDetail(t.id)"
              @accept="onAccept"
              @reject="onReject"
              @submit="goSubmit(t.id)"
              @assign="goDetail(t.id)"
            />
          </li>
        </ul>
        <van-empty v-if="finished && !list.length" description="暂无任务" />
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import TaskCard from './components/TaskCard.vue'
import { getOutsourceTasks, acceptTask, rejectTask } from '@/api/agent'
import { OutsourceTaskStatusEnum, type OutsourceTaskVO } from '@/types/api'

const route = useRoute()
const router = useRouter()

const tabs = [
  { label: '全部', value: '' },
  { label: '待接单', value: OutsourceTaskStatusEnum.PENDING },
  { label: '已接单', value: OutsourceTaskStatusEnum.ACCEPTED },
  { label: '进行中', value: OutsourceTaskStatusEnum.IN_PROGRESS },
  { label: '待审核', value: OutsourceTaskStatusEnum.SUBMITTED },
  { label: '已通过', value: OutsourceTaskStatusEnum.CONFIRMED },
  { label: '已驳回', value: OutsourceTaskStatusEnum.REJECTED }
]

const activeStatus = ref<string>((route.query.status as string) || '')
const keyword = ref('')
const list = ref<OutsourceTaskVO[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)
const pageSize = 10

async function onLoad() {
  if (refreshing.value) return
  try {
    const data = await getOutsourceTasks({
      page: pageNum.value,
      size: pageSize,
      status: activeStatus.value || undefined,
      keyword: keyword.value || undefined
    })
    const arr = data?.list || []
    if (pageNum.value === 1) {
      list.value = arr
    } else {
      list.value.push(...arr)
    }
    if (arr.length < pageSize || list.value.length >= (data?.total || 0)) {
      finished.value = true
    } else {
      pageNum.value++
    }
  } catch (e) {
    // 拦截器已提示
    finished.value = true
  } finally {
    loading.value = false
  }
}

function onRefresh() {
  pageNum.value = 1
  finished.value = false
  list.value = []
  refreshing.value = false
  loading.value = true
  onLoad()
}

function onTabChange() {
  pageNum.value = 1
  finished.value = false
  list.value = []
  loading.value = true
  onLoad()
}

function onSearch() {
  onTabChange()
}

function goDetail(id: number | string) {
  router.push(`/agent/tasks/${id}`)
}

function goSubmit(id: number | string) {
  router.push(`/agent/tasks/${id}/submit`)
}

async function onAccept(task: OutsourceTaskVO) {
  showConfirmDialog({ title: '确认接单', message: `确定接受任务「${task.siteName}」吗？` })
    .then(async () => {
      try {
        await acceptTask(task.id)
        showToast('接单成功')
        onRefresh()
      } catch (e) {
        // 拦截器已提示
      }
    })
    .catch(() => {})
}

async function onReject(task: OutsourceTaskVO) {
  showConfirmDialog({ title: '确认拒绝', message: `确定拒绝任务「${task.siteName}」吗？` })
    .then(async () => {
      try {
        await rejectTask(task.id)
        showToast('已拒绝')
        onRefresh()
      } catch (e) {
        // 拦截器已提示
      }
    })
    .catch(() => {})
}
</script>

<style lang="scss" scoped>
.task-list-view {
  min-height: 100%;
  background: var(--bg-page);
}

.list-wrap {
  padding: 0 12px;
}

.list {
  li + li {
    margin-top: 8px;
  }
  padding-bottom: 8px;
}
</style>
