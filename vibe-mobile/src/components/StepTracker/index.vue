<script setup lang="ts">
/**
 * 施工步骤跟踪组件（设计文档 3.4.2）
 * - 步骤列表、逐步标记完成 + 拍照 + 耗时
 * - 进度显示
 */
import { computed, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { showFailToast } from 'vant'
import type { WorkStep } from '@/types/api'
import PhotoCapture, { type CapturedPhoto } from '@/components/PhotoCapture/index.vue'

interface Props {
  /** 步骤列表 */
  steps: WorkStep[]
  /** 是否只读（查看模式） */
  readonly?: boolean
  /** 上传人（拍照水印用） */
  uploader?: string
}

const props = withDefaults(defineProps<Props>(), {
  readonly: false,
  uploader: ''
})

const emit = defineEmits<{
  (e: 'update:steps', steps: WorkStep[]): void
  (e: 'complete', step: WorkStep, photos: CapturedPhoto[]): void
  (e: 'allComplete', steps: WorkStep[]): void
}>()

/** 步骤是否已完成（兼容后端 COMPLETED 与旧版 DONE） */
function isStepDone(step: WorkStep): boolean {
  return step.status === 'COMPLETED' || step.status === ('DONE' as any)
}

/** 内部可变步骤副本 */
const localSteps = ref<WorkStep[]>([])
watch(
  () => props.steps,
  (val) => {
    localSteps.value = (val || []).map((s) => ({ ...s }))
  },
  { immediate: true, deep: true }
)

/** 每个步骤的照片集（key: step.id） */
const stepPhotos = ref<Record<string, CapturedPhoto[]>>({})
/** 每个步骤的开始时间（用于计算耗时） */
const stepStartMap = ref<Record<string, number>>({})
/** 每个步骤的备注 */
const stepRemarkMap = ref<Record<string, string>>({})

/** 当前展开的步骤ID */
const activeId = ref<string | number>('')

const completedCount = computed(() => localSteps.value.filter((s) => isStepDone(s)).length)
const totalCount = computed(() => localSteps.value.length)
const progress = computed(() => {
  if (!totalCount.value) return 0
  return Math.round((completedCount.value / totalCount.value) * 100)
})
const allDone = computed(() => totalCount.value > 0 && completedCount.value === totalCount.value)

function formatDuration(sec: number): string {
  if (sec < 60) return `${sec}秒`
  const m = Math.floor(sec / 60)
  const s = sec % 60
  if (m < 60) return `${m}分${s}秒`
  const h = Math.floor(m / 60)
  return `${h}小时${m % 60}分`
}

function toggle(step: WorkStep): void {
  if (props.readonly) return
  activeId.value = activeId.value === step.id ? '' : step.id
  if (!stepStartMap.value[String(step.id)]) {
    stepStartMap.value[String(step.id)] = Date.now()
  }
}

function onPhotosChange(step: WorkStep, photos: CapturedPhoto[]): void {
  stepPhotos.value[String(step.id)] = photos
}

/** 标记步骤完成 */
function markDone(step: WorkStep): void {
  const photos = stepPhotos.value[String(step.id)] || []
  if (step.needPhoto && step.minPhotoCount && photos.length < step.minPhotoCount) {
    showFailToast(`该步骤至少需要 ${step.minPhotoCount} 张照片`)
    return
  }
  const start = stepStartMap.value[String(step.id)]
  const durationSec = start ? Math.round((Date.now() - start) / 1000) : 0
  const updated: WorkStep = {
    ...step,
    status: 'COMPLETED',
    completedTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    // 兼容字段（旧组件逻辑用 completedAt）
    completedAt: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    photoCount: photos.length,
    remark:
      stepRemarkMap.value[String(step.id)] ||
      (durationSec ? `耗时 ${formatDuration(durationSec)}` : undefined)
  }
  const idx = localSteps.value.findIndex((s) => s.id === step.id)
  if (idx >= 0) localSteps.value[idx] = updated
  emit('update:steps', localSteps.value)
  emit('complete', updated, photos)
  if (allDone.value) emit('allComplete', localSteps.value)
}

/** 撤销完成 */
function undoDone(step: WorkStep): void {
  const idx = localSteps.value.findIndex((s) => s.id === step.id)
  if (idx < 0) return
  localSteps.value[idx] = {
    ...step,
    status: 'WAITING',
    completedTime: undefined,
    completedAt: undefined,
    photoCount: 0,
    remark: undefined
  }
  emit('update:steps', localSteps.value)
}

defineExpose({ progress, allDone, completedCount, totalCount })
</script>

<template>
  <div class="step-tracker">
    <!-- 进度总览 -->
    <div class="progress-card">
      <div class="progress-head">
        <span>施工进度</span>
        <span class="count">{{ completedCount }}/{{ totalCount }}</span>
      </div>
      <van-progress
        :percentage="progress"
        :color="progress === 100 ? '#52c41a' : '#1677ff'"
        stroke-width="8"
        :show-pivot="true"
      />
    </div>

    <!-- 步骤列表 -->
    <div
      v-for="(step, idx) in localSteps"
      :key="step.id"
      class="step-item"
      :class="{ done: isStepDone(step), active: activeId === step.id }"
    >
      <div class="step-head" @click="toggle(step)">
        <span class="step-no">
          <van-icon v-if="isStepDone(step)" name="success" color="#52c41a" />
          <span v-else>{{ idx + 1 }}</span>
        </span>
        <div class="step-info">
          <div class="step-name">{{ step.stepName }}</div>
          <div v-if="isStepDone(step) && (step.completedTime || step.completedAt)" class="step-meta">
            完成于 {{ step.completedTime || step.completedAt }}
            <span v-if="step.photoCount">· {{ step.photoCount }} 张照片</span>
          </div>
          <div v-else-if="step.needPhoto" class="step-meta warn">
            需拍照<template v-if="step.minPhotoCount">（至少 {{ step.minPhotoCount }} 张）</template>
          </div>
        </div>
        <van-icon v-if="!readonly" :name="activeId === step.id ? 'arrow-up' : 'arrow-down'" />
      </div>

      <!-- 展开内容 -->
      <div v-if="activeId === step.id" class="step-body">
        <p v-if="step.description" class="desc">{{ step.description }}</p>

        <!-- 已完成态 -->
        <template v-if="isStepDone(step)">
          <div v-if="stepPhotos[String(step.id)]?.length" class="done-photos">
            <img
              v-for="(p, i) in stepPhotos[String(step.id)]"
              :key="p.localId"
              :src="p.thumbnail"
              :alt="`照片${i + 1}`"
            />
          </div>
          <div v-if="step.remark" class="done-remark">备注：{{ step.remark }}</div>
          <van-button v-if="!readonly" size="small" plain type="warning" @click="undoDone(step)">
            撤销完成
          </van-button>
        </template>

        <!-- 待完成态 -->
        <template v-else>
          <PhotoCapture
            :max-count="9"
            :watermark="true"
            :uploader="uploader"
            @change="(ps) => onPhotosChange(step, ps)"
          />
          <van-field
            v-model="stepRemarkMap[String(step.id)]"
            label="备注"
            type="textarea"
            placeholder="可填写施工备注（选填）"
            rows="2"
            autosize
          />
          <van-button
            class="done-btn"
            type="primary"
            size="small"
            block
            @click="markDone(step)"
          >
            完成该步骤
          </van-button>
        </template>
      </div>
    </div>

    <div v-if="allDone" class="all-done-tip">✅ 所有步骤已完成</div>
  </div>
</template>

<style scoped lang="scss">
.step-tracker {
  width: 100%;
}

.progress-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;

  .progress-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    font-size: 14px;
    color: var(--color-text-regular);

    .count {
      font-size: 16px;
      font-weight: 600;
      color: var(--brand-primary);
    }
  }
}

.step-item {
  background: #fff;
  border-radius: 12px;
  margin-bottom: 8px;
  overflow: hidden;

  &.done .step-name {
    color: var(--color-text-secondary);
    text-decoration: line-through;
  }
  &.active {
    box-shadow: 0 0 0 1px var(--brand-primary) inset;
  }
}

.step-head {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  gap: 12px;
}

.step-no {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--bg-disabled);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: var(--color-text-regular);
  flex-shrink: 0;
}

.step-info {
  flex: 1;
  min-width: 0;

  .step-name {
    font-size: 15px;
    color: var(--color-text-primary);
  }
  .step-meta {
    margin-top: 2px;
    font-size: 12px;
    color: var(--color-text-secondary);

    &.warn {
      color: var(--color-warning);
    }
  }
}

.step-body {
  padding: 0 16px 16px;
  border-top: 1px solid var(--border-color-light);

  .desc {
    margin: 10px 0;
    font-size: 13px;
    color: var(--color-text-regular);
  }
}

.done-photos {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 10px 0;

  img {
    width: 56px;
    height: 56px;
    border-radius: 6px;
    object-fit: cover;
  }
}

.done-remark {
  font-size: 13px;
  color: var(--color-text-regular);
  margin: 8px 0;
  padding: 8px;
  background: var(--bg-disabled);
  border-radius: 6px;
}

.done-btn {
  margin-top: 12px;
  min-height: 40px;
}

.all-done-tip {
  text-align: center;
  padding: 16px;
  color: var(--color-success);
  font-size: 15px;
  font-weight: 500;
}
</style>
