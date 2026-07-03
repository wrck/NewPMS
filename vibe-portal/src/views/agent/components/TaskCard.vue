<template>
  <div class="task-card" @click="$emit('click')">
    <div class="task-card__head">
      <span class="task-card__project ellipsis">{{ task.projectName }}</span>
      <StatusTag :label="statusInfo.label" :type="statusInfo.type" plain />
    </div>
    <div class="task-card__site">{{ task.siteName }}</div>
    <div class="task-card__code">任务编号：{{ task.taskCode }}</div>

    <div class="task-card__meta">
      <div v-if="task.pmName" class="meta-item">
        <span class="label">PM：</span>
        <span>{{ task.pmName }}</span>
      </div>
      <div v-if="task.executorName" class="meta-item">
        <span class="label">执行人：</span>
        <span>{{ task.executorName }}</span>
      </div>
      <div v-if="task.deadline" class="meta-item">
        <span class="label">截止：</span>
        <span>{{ formatDate(task.deadline) }}</span>
      </div>
    </div>

    <div v-if="task.progressText || task.percent != null" class="task-card__progress">
      <span v-if="task.progressText">{{ task.progressText }}</span>
      <ProgressBar
        v-if="task.percent != null"
        :percent="task.percent"
        variant="agent"
        :show-label="false"
      />
    </div>

    <div v-if="task.requirement" class="task-card__req ellipsis-2">
      要求：{{ task.requirement }}
    </div>

    <div v-if="actions.length" class="task-card__actions" @click.stop>
      <van-button
        v-for="a in actions"
        :key="a.key"
        size="small"
        :type="a.type"
        :plain="a.plain"
        @click="onAction(a)"
      >
        {{ a.label }}
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import dayjs from 'dayjs'
import ProgressBar from '@/components/ProgressBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import { OutsourceTaskStatusEnum, OutsourceTaskStatusMap, type OutsourceTaskVO } from '@/types/api'

interface Props {
  task: OutsourceTaskVO
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'click'): void
  (e: 'accept', task: OutsourceTaskVO): void
  (e: 'reject', task: OutsourceTaskVO): void
  (e: 'submit', task: OutsourceTaskVO): void
  (e: 'assign', task: OutsourceTaskVO): void
}>()

const statusInfo = computed(() => OutsourceTaskStatusMap[props.task.status])

interface ActionDef {
  key: string
  label: string
  event: 'accept' | 'reject' | 'submit' | 'assign'
  type: 'primary' | 'success' | 'danger' | 'default'
  plain: boolean
}

const actions = computed<ActionDef[]>(() => {
  switch (props.task.status) {
    case OutsourceTaskStatusEnum.PENDING:
      return [
        { key: 'reject', label: '拒绝', event: 'reject', type: 'danger', plain: true },
        { key: 'accept', label: '接单', event: 'accept', type: 'primary', plain: false }
      ]
    case OutsourceTaskStatusEnum.ACCEPTED:
      return [
        { key: 'assign', label: '指派工程师', event: 'assign', type: 'primary', plain: false }
      ]
    case OutsourceTaskStatusEnum.IN_PROGRESS:
    case OutsourceTaskStatusEnum.REJECTED:
      return [
        { key: 'submit', label: '提交交付物', event: 'submit', type: 'primary', plain: false }
      ]
    default:
      return []
  }
})

function formatDate(d: string): string {
  return dayjs(d).format('MM-DD')
}

/** 分发按钮事件，规避动态事件名的类型推断问题 */
function onAction(a: ActionDef) {
  switch (a.event) {
    case 'accept':
      emit('accept', props.task)
      break
    case 'reject':
      emit('reject', props.task)
      break
    case 'submit':
      emit('submit', props.task)
      break
    case 'assign':
      emit('assign', props.task)
      break
  }
}
</script>

<style lang="scss" scoped>
.task-card {
  background: #fafbfc;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid var(--border-color-light);

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }

  &__project {
    font-size: 12px;
    color: var(--color-text-secondary);
    flex: 1;
    min-width: 0;
  }

  &__site {
    margin-top: 6px;
    font-size: 15px;
    font-weight: 600;
    color: var(--color-text-primary);
  }

  &__code {
    margin-top: 4px;
    font-size: 12px;
    color: var(--color-text-secondary);
  }

  &__meta {
    margin-top: 8px;
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    font-size: 12px;
    color: var(--color-text-regular);

    .label {
      color: var(--color-text-secondary);
    }
  }

  &__progress {
    margin-top: 8px;
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: var(--color-text-regular);
  }

  &__req {
    margin-top: 8px;
    font-size: 12px;
    color: var(--color-text-regular);
    line-height: 1.5;
  }

  &__actions {
    margin-top: 10px;
    display: flex;
    gap: 8px;
    justify-content: flex-end;
  }
}
</style>
