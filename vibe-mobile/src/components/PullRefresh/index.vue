<script setup lang="ts">
/**
 * 下拉刷新 + 上拉加载列表封装（设计文档 3.6 移动端优化）
 * 基于 van-pull-refresh + van-list 二次封装，统一分页加载逻辑
 */
import { ref, reactive, computed } from 'vue'

interface Props {
  /** 请求函数：接收分页参数，返回 { list, total } */
  loader: (params: { pageNum: number; pageSize: number }) => Promise<{ list: any[]; total: number }>
  /** 每页条数 */
  pageSize?: number
  /** 空数据描述 */
  emptyText?: string
  /** 是否自动首次加载 */
  autoLoad?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  pageSize: 10,
  emptyText: '暂无数据',
  autoLoad: true
})

const emit = defineEmits<{
  (e: 'loaded', list: any[], total: number): void
}>()

const list = ref<any[]>([])
const loading = ref(false)
const refreshing = ref(false)
const finished = ref(false)
const error = ref(false)
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: props.pageSize
})

const isEmpty = computed(() => !loading.value && !refreshing.value && list.value.length === 0)

/** 上拉加载 */
async function onLoad(): Promise<void> {
  if (refreshing.value) return
  loading.value = true
  try {
    const res = await props.loader({ pageNum: query.pageNum, pageSize: query.pageSize })
    const items = res.list || []
    if (query.pageNum === 1) list.value = items
    else list.value.push(...items)
    total.value = res.total || 0
    finished.value = list.value.length >= total.value
    if (!finished.value) query.pageNum += 1
    error.value = false
    emit('loaded', list.value, total.value)
  } catch (e) {
    error.value = true
  } finally {
    loading.value = false
  }
}

/** 下拉刷新 */
async function onRefresh(): Promise<void> {
  refreshing.value = true
  query.pageNum = 1
  finished.value = false
  try {
    const res = await props.loader({ pageNum: 1, pageSize: query.pageSize })
    list.value = res.list || []
    total.value = res.total || 0
    finished.value = list.value.length >= total.value
    if (!finished.value) query.pageNum = 2
  } catch (e) {
    list.value = []
  } finally {
    refreshing.value = false
    loading.value = false
  }
}

/** 外部手动刷新 */
function refresh(): Promise<void> {
  return onRefresh()
}

/** 重置 */
function reset(): void {
  list.value = []
  query.pageNum = 1
  finished.value = false
  total.value = 0
}

if (props.autoLoad) {
  // van-list 挂载后会自动触发 onLoad
}

defineExpose({ list, total, refresh, reset, onLoad })
</script>

<template>
  <div class="pull-refresh">
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        :error="error"
        finished-text="没有更多了"
        error-text="加载失败，点击重试"
        @load="onLoad"
      >
        <slot :list="list" />
      </van-list>

      <van-empty v-if="isEmpty" :description="emptyText" />
    </van-pull-refresh>
  </div>
</template>

<style scoped lang="scss">
.pull-refresh {
  min-height: 100%;
}
</style>
