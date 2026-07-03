<template>
  <div class="project-progress">
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <!-- 项目选择 -->
      <section class="card project-picker">
        <div class="project-picker__label">查询项目</div>
        <div class="project-picker__select" @click="showProjectSheet = true">
          <span class="project-picker__name ellipsis">
            {{ currentProject ? currentProject.projectName : '请选择项目' }}
          </span>
          <van-icon name="arrow-down" size="14" color="#8c8c8c" />
        </div>
      </section>

      <!-- 项目概况卡片 -->
      <section class="card progress-summary">
        <div class="progress-summary__title">
          <van-icon name="cluster-o" size="18" color="#1677ff" />
          <span class="ellipsis">{{ progress?.projectName || '项目加载中...' }}</span>
        </div>

        <div class="progress-summary__overall">
          <div class="flex-between">
            <span class="label">整体进度</span>
            <span class="percent">{{ progress?.progressPct ?? 0 }}%</span>
          </div>
          <ProgressBar
            :percent="progress?.progressPct ?? 0"
            variant="customer"
            :show-label="false"
          />
        </div>

        <div class="progress-summary__row">
          <div>
            <span class="label">当前阶段</span>
            <span class="value">{{ progress?.currentPhaseName || '-' }}</span>
          </div>
          <div>
            <span class="label">项目编号</span>
            <span class="value">{{ currentProject?.projectCode || '-' }}</span>
          </div>
        </div>
      </section>

      <!-- 各阶段进展 -->
      <section class="card">
        <div class="section-title">各阶段进展</div>
        <ul class="phase-list">
          <li v-for="phase in progress?.phases || []" :key="phase.id" class="phase-item">
            <div class="phase-item__icon" :class="`phase-item__icon--${phase.status.toLowerCase()}`">
              <van-icon :name="phaseIcon(phase.status)" />
            </div>
            <div class="phase-item__body">
              <div class="flex-between">
                <span class="phase-name">{{ phase.name }}</span>
                <span class="phase-status" :class="`phase-status--${phase.status.toLowerCase()}`">
                  {{ phaseStatusText(phase) }}
                </span>
              </div>
              <ProgressBar
                v-if="phase.status === 'IN_PROGRESS'"
                :percent="phase.percent || 0"
                variant="customer"
                :show-label="false"
              />
              <div v-if="phase.actualEnd" class="phase-date">
                完成：{{ formatDate(phase.actualEnd) }}
              </div>
            </div>
          </li>
          <li v-if="!progress?.phases?.length" class="empty-tip">暂无阶段数据</li>
        </ul>
      </section>

      <!-- 快捷入口 -->
      <section class="card quick-entry">
        <van-cell
          title="项目文档下载"
          icon="description"
          is-link
          @click="goDocuments"
        />
      </section>
    </van-pull-refresh>

    <!-- 项目选择 ActionSheet -->
    <van-action-sheet
      v-model:show="showProjectSheet"
      title="选择项目"
      :actions="projectActions"
      close-on-click-action
      cancel-text="取消"
      :round="true"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import dayjs from 'dayjs'
import ProgressBar from '@/components/ProgressBar.vue'
import { getMyProjects, getProjectProgress } from '@/api/customer'
import type { CustomerProjectVO, ProjectProgressVO, PhaseTimelineVO } from '@/types/api'

const router = useRouter()

const projects = ref<CustomerProjectVO[]>([])
const currentProject = ref<CustomerProjectVO | undefined>()
const progress = ref<ProjectProgressVO | undefined>()
const refreshing = ref(false)
const showProjectSheet = ref(false)

const projectActions = computed(() =>
  projects.value.map((p) => ({
    name: p.projectName,
    subname: p.projectCode,
    // 标记当前选中
    color: currentProject.value?.projectId === p.projectId ? '#1677ff' : '#1a1a1a',
    callback: () => selectProject(p)
  }))
)

/** 拉取项目列表 */
async function fetchProjects() {
  try {
    const data = await getMyProjects()
    projects.value = data || []
    if (projects.value.length && !currentProject.value) {
      selectProject(projects.value[0])
    } else if (!projects.value.length) {
      progress.value = undefined
    }
  } catch (e) {
    // 拦截器已提示
  }
}

/** 选择项目并拉取进度 */
async function selectProject(p: CustomerProjectVO) {
  currentProject.value = p
  showProjectSheet.value = false
  await fetchProgress(p.projectId)
}

/** 拉取项目进度详情 */
async function fetchProgress(projectId: number | string) {
  try {
    progress.value = await getProjectProgress(projectId)
  } catch (e) {
    // 拦截器已提示
  }
}

function onRefresh() {
  const pid = currentProject.value?.projectId
  Promise.all([fetchProjects(), pid ? fetchProgress(pid) : Promise.resolve()]).finally(() => {
    refreshing.value = false
  })
}

function formatDate(d?: string): string {
  return d ? dayjs(d).format('YYYY-MM-DD') : '待定'
}

function phaseIcon(status: string): string {
  switch (status) {
    case 'DONE':
      return 'success'
    case 'IN_PROGRESS':
      return 'replay'
    case 'SKIPPED':
      return 'close'
    default:
      return 'circle'
  }
}

function phaseStatusText(phase: PhaseTimelineVO): string {
  switch (phase.status) {
    case 'DONE':
      return '已完成'
    case 'IN_PROGRESS':
      return `进行中 ${phase.percent ?? 0}%`
    case 'SKIPPED':
      return '已跳过'
    default:
      return '待开始'
  }
}

function goDocuments() {
  if (!currentProject.value) {
    showToast('请先选择项目')
    return
  }
  router.push({
    path: '/customer/documents',
    query: { projectId: String(currentProject.value.projectId) }
  })
}

// 项目列表变化时，若无选中则默认选第一个
watch(projects, (arr) => {
  if (arr.length && !currentProject.value) {
    selectProject(arr[0])
  }
})

onMounted(fetchProjects)
</script>

<style lang="scss" scoped>
.project-progress {
  padding: 12px;
  min-height: 100%;

  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    font-weight: 600;
    color: var(--color-text-primary);
    margin-bottom: 12px;
  }
}

.project-picker {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;

  &__label {
    font-size: 13px;
    color: var(--color-text-secondary);
    flex-shrink: 0;
  }

  &__select {
    display: flex;
    align-items: center;
    gap: 6px;
    flex: 1;
    min-width: 0;
    justify-content: flex-end;
  }

  &__name {
    font-size: 14px;
    color: var(--color-text-primary);
    font-weight: 500;
    max-width: 220px;
  }
}

.progress-summary {
  &__title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 12px;
  }

  &__overall {
    margin-bottom: 12px;

    .label {
      font-size: 13px;
      color: var(--color-text-secondary);
    }
    .percent {
      font-size: 16px;
      font-weight: 600;
      color: var(--customer-primary);
    }
  }

  &__row {
    display: flex;
    gap: 16px;

    > div {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 0;
    }

    .label {
      font-size: 12px;
      color: var(--color-text-secondary);
    }
    .value {
      font-size: 14px;
      color: var(--color-text-primary);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.phase-list {
  .phase-item {
    display: flex;
    gap: 10px;
    padding: 10px 0;

    & + .phase-item {
      border-top: 1px solid var(--border-color-light);
    }

    &__icon {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      font-size: 13px;

      &--done {
        background: rgba(82, 196, 26, 0.12);
        color: var(--color-success);
      }
      &--in_progress {
        background: rgba(22, 119, 255, 0.12);
        color: var(--brand-primary);
      }
      &--pending {
        background: rgba(140, 140, 140, 0.12);
        color: var(--color-text-secondary);
      }
      &--skipped {
        background: rgba(255, 77, 79, 0.12);
        color: var(--color-danger);
      }
    }

    &__body {
      flex: 1;
      min-width: 0;

      .phase-name {
        font-size: 14px;
        font-weight: 500;
      }
      .phase-status {
        font-size: 12px;

        &--done {
          color: var(--color-success);
        }
        &--in_progress {
          color: var(--brand-primary);
        }
        &--pending {
          color: var(--color-text-secondary);
        }
      }
      .phase-date {
        margin-top: 4px;
        font-size: 12px;
        color: var(--color-text-secondary);
      }
    }
  }
}

.empty-tip {
  text-align: center;
  padding: 16px 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.quick-entry {
  padding: 0;
  overflow: hidden;
}
</style>
