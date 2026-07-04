<template>
  <H5Layout title="我的待办" :show-back="true">
    <div v-if="loading" class="loading-area">
      <a-spin />
    </div>

    <div v-else-if="todos.length === 0" class="empty-area">
      <a-empty description="暂无待办" />
    </div>

    <div v-else class="todo-list">
      <div
        v-for="(todo, idx) in todos"
        :key="idx"
        class="todo-card"
        @click="onClickTodo(todo)"
      >
        <div class="todo-icon" :class="`type-${todo.type.toLowerCase()}`">
          {{ getTypeIcon(todo.type) }}
        </div>
        <div class="todo-info">
          <div class="todo-title">{{ todo.title }}</div>
          <div class="todo-meta">
            <span class="project-name">{{ todo.projectName }}</span>
            <span class="todo-type" :class="`type-${todo.type.toLowerCase()}`">
              {{ getTypeName(todo.type) }}
            </span>
          </div>
          <div class="todo-time">{{ formatDateTime(todo.createTime) }}</div>
        </div>
        <div class="todo-arrow">›</div>
      </div>
    </div>
  </H5Layout>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import H5Layout from '@/layouts/H5Layout.vue'
import { getMyTodos } from '@/api/customerPortal'
import type { CustomerTodo } from '@/types/customerPortal'

const router = useRouter()

const loading = ref(true)
const todos = ref<CustomerTodo[]>([])

onMounted(async () => {
  loading.value = true
  try {
    todos.value = (await getMyTodos()) || []
  } catch (e: any) {
    console.error('[H5 customer todos]', e)
    if (e?.code === 401 || e?.code === 40301) {
      router.push({
        path: '/h5/customer/login',
        query: { redirect: '/h5/customer/todos' }
      })
    } else {
      message.error(e?.message || '加载失败')
    }
  } finally {
    loading.value = false
  }
})

function onClickTodo(todo: CustomerTodo) {
  if (todo.type === 'CUTOVER_APPROVAL') {
    router.push(`/h5/customer/cutover/${todo.signToken}`)
  } else if (todo.type === 'ACCEPTANCE_SIGN') {
    router.push(`/h5/customer/acceptance/${todo.signToken}`)
  }
}

function getTypeIcon(type: string): string {
  if (type === 'CUTOVER_APPROVAL') return '📋'
  if (type === 'ACCEPTANCE_SIGN') return '✍️'
  return '📌'
}

function getTypeName(type: string): string {
  if (type === 'CUTOVER_APPROVAL') return '割接审批'
  if (type === 'ACCEPTANCE_SIGN') return '验收签核'
  return type
}

function formatDateTime(dt: string): string {
  if (!dt) return ''
  return dt.replace('T', ' ').substring(0, 16)
}
</script>

<style scoped>
.loading-area,
.empty-area {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px 0;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.todo-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.15s;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.todo-card:active {
  transform: scale(0.98);
}

.todo-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: #f0f5ff;
}

.todo-icon.type-cutover_approval {
  background: #fff7e6;
}

.todo-icon.type-acceptance_sign {
  background: #f6ffed;
}

.todo-info {
  flex: 1;
  min-width: 0;
}

.todo-title {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.todo-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 4px;
}

.project-name {
  font-size: 12px;
  color: #888;
}

.todo-type {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 8px;
  background: #f0f0f0;
  color: #666;
}

.todo-type.type-cutover_approval {
  background: #fff7e6;
  color: #fa8c16;
}

.todo-type.type-acceptance_sign {
  background: #f6ffed;
  color: #52c41a;
}

.todo-time {
  font-size: 11px;
  color: #bbb;
}

.todo-arrow {
  color: #ccc;
  font-size: 18px;
  font-weight: 300;
}
</style>
