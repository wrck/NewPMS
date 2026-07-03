<script setup lang="ts">
/**
 * 弱网上传队列组件（设计文档 1.10 移动端：前端直传 + 进度条 + 断点续传）
 * - 照片先本地缓存（IndexedDB）
 * - 断点续传：失败后自动重试（指数退避）
 * - 网络恢复后自动续传（useOnline 监听）
 * - 上传进度展示、手动重试、清理
 */
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { showToast } from 'vant'
import { useOnline } from '@vueuse/core'
import type { UploadFileMeta } from '@/types/api'
import {
  putCachedFile,
  getCachedFile,
  deleteCachedFile,
  getAllCachedFiles,
  type CachedFile
} from '@/utils/indexeddb'
import { uploadFile } from '@/api'

interface Props {
  /** 业务ID（工单/步骤ID） */
  bizId?: string
  /** 业务类型 */
  bizType?: string
  /** 上传人 */
  uploader?: string
  /** 最大重试次数 */
  maxRetry?: number
  /** 是否自动启动上传 */
  autoStart?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  bizId: '',
  bizType: 'work_order',
  uploader: '',
  maxRetry: 5,
  autoStart: true
})

const emit = defineEmits<{
  (e: 'success', meta: UploadFileMeta): void
  (e: 'fail', meta: UploadFileMeta, error: Error): void
  (e: 'progress', meta: UploadFileMeta, loaded: number, total: number): void
  (e: 'queue-change', items: UploadFileMeta[]): void
}>()

const online = useOnline()
const isUploading = ref(false)
const queue = ref<UploadFileMeta[]>([])
const panelOpen = ref(false)

/** 统计信息 */
const stats = computed(() => {
  const pending = queue.value.filter((i) => i.status === 'PENDING' || i.status === 'FAILED').length
  const uploading = queue.value.filter((i) => i.status === 'UPLOADING').length
  const success = queue.value.filter((i) => i.status === 'SUCCESS').length
  const failed = queue.value.filter((i) => i.status === 'FAILED').length
  return { pending, uploading, success, failed, total: queue.value.length }
})

function genLocalId(): string {
  return `upload_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

/**
 * 添加文件到上传队列，立即写入 IndexedDB 缓存
 */
async function add(
  file: Blob,
  meta?: Partial<UploadFileMeta>,
  thumbnail?: string
): Promise<UploadFileMeta> {
  const localId = meta?.localId || genLocalId()
  const fileMeta: UploadFileMeta = {
    localId,
    bizId: meta?.bizId || props.bizId,
    bizType: meta?.bizType || props.bizType,
    name: meta?.name || `file_${Date.now()}.jpg`,
    size: file.size,
    type: (file as Blob).type || 'image/jpeg',
    thumbnail,
    longitude: meta?.longitude,
    latitude: meta?.latitude,
    capturedAt: meta?.capturedAt || new Date().toISOString(),
    uploader: meta?.uploader || props.uploader,
    status: 'PENDING',
    retryCount: 0,
    createdAt: Date.now()
  }

  const cached: CachedFile = {
    localId,
    bizId: fileMeta.bizId,
    bizType: fileMeta.bizType,
    blob: file,
    thumbnail,
    meta: JSON.stringify(fileMeta),
    createdAt: Date.now()
  }
  await putCachedFile(cached)

  queue.value.push(fileMeta)
  emit('queue-change', queue.value)

  if (props.autoStart && online.value) start()
  return fileMeta
}

/** 启动上传队列 */
async function start(): Promise<void> {
  if (isUploading.value) return
  if (!online.value) {
    showToast('当前网络不可用，文件已缓存，网络恢复后自动上传')
    return
  }
  await processQueue()
}

/** 处理队列（串行，避免弱网拥塞） */
async function processQueue(): Promise<void> {
  if (isUploading.value) return
  isUploading.value = true
  try {
    const pending = queue.value.filter((i) => i.status === 'PENDING' || i.status === 'FAILED')
    for (const meta of pending) {
      if (!online.value) break
      await uploadOne(meta)
    }
  } finally {
    isUploading.value = false
  }
}

/** 上传单个文件（带进度、重试、断点续传） */
async function uploadOne(meta: UploadFileMeta): Promise<void> {
  const cached = await getCachedFile(meta.localId)
  if (!cached) {
    console.warn('[UploadQueue] cached file not found:', meta.localId)
    meta.status = 'FAILED'
    return
  }
  meta.status = 'UPLOADING'
  try {
    const res = await uploadFile(
      cached.blob,
      (loaded, total) => {
        meta.loaded = loaded
        emit('progress', meta, loaded, total)
      },
      {
        bizId: meta.bizId,
        bizType: meta.bizType,
        capturedAt: meta.capturedAt,
        longitude: meta.longitude?.toString() || '',
        latitude: meta.latitude?.toString() || '',
        uploader: meta.uploader
      }
    )
    meta.status = 'SUCCESS'
    meta.remoteUrl = res.url
    emit('success', meta)
    await deleteCachedFile(meta.localId)
  } catch (err: any) {
    meta.retryCount = (meta.retryCount || 0) + 1
    if (meta.retryCount >= props.maxRetry) {
      meta.status = 'FAILED'
      emit('fail', meta, err instanceof Error ? err : new Error(err?.message || '上传失败'))
    } else {
      // 标记为待重试，指数退避
      meta.status = 'PENDING'
      const delay = Math.min(30000, 2000 * Math.pow(2, meta.retryCount))
      setTimeout(() => {
        if (online.value) uploadOne(meta)
      }, delay)
    }
    // 更新缓存中的 meta
    cached.meta = JSON.stringify(meta)
    await putCachedFile(cached)
  }
}

/** 网络恢复时自动续传 */
async function onOnlineChange(isOnline: boolean): Promise<void> {
  if (isOnline) {
    await restoreFromCache()
    if (props.autoStart && stats.value.pending > 0) {
      showToast(`网络恢复，开始续传 ${stats.value.pending} 个文件`)
      start()
    }
  }
}

/** 从 IndexedDB 恢复未完成的上传 */
async function restoreFromCache(): Promise<void> {
  const cachedFiles = await getAllCachedFiles()
  const existingIds = new Set(queue.value.map((i) => i.localId))
  for (const cf of cachedFiles) {
    if (existingIds.has(cf.localId)) continue
    try {
      const meta: UploadFileMeta = JSON.parse(cf.meta)
      if (meta.status === 'UPLOADING') meta.status = 'PENDING'
      queue.value.push(meta)
    } catch (e) {
      console.warn('[UploadQueue] parse meta failed', e)
    }
  }
  emit('queue-change', queue.value)
}

async function retry(localId: string): Promise<void> {
  const meta = queue.value.find((i) => i.localId === localId)
  if (meta) {
    meta.status = 'PENDING'
    meta.retryCount = 0
    start()
  }
}

async function remove(localId: string): Promise<void> {
  const idx = queue.value.findIndex((i) => i.localId === localId)
  if (idx >= 0) {
    queue.value.splice(idx, 1)
    await deleteCachedFile(localId)
    emit('queue-change', queue.value)
  }
}

function clearSuccess(): void {
  queue.value = queue.value.filter((i) => i.status !== 'SUCCESS')
  emit('queue-change', queue.value)
}

function progressPercent(meta: UploadFileMeta): number {
  if (meta.status === 'SUCCESS') return 100
  if (meta.status === 'UPLOADING' && meta.loaded && meta.size) {
    return Math.round((meta.loaded / meta.size) * 100)
  }
  return 0
}

function statusText(status: UploadFileMeta['status']): string {
  return { PENDING: '待上传', UPLOADING: '上传中', SUCCESS: '已完成', FAILED: '失败' }[status]
}

defineExpose({ add, start, retry, remove, clearSuccess, queue })

const stopWatch = watch(online, onOnlineChange)

onMounted(async () => {
  await restoreFromCache()
  if (props.autoStart && online.value && stats.value.pending > 0) start()
})

onBeforeUnmount(() => {
  stopWatch()
})
</script>

<template>
  <div class="upload-queue">
    <!-- 浮动触发按钮 -->
    <div
      v-if="stats.pending + stats.uploading + stats.failed > 0"
      class="upload-queue__fab touchable"
      @click="panelOpen = !panelOpen"
    >
      <van-icon name="upgrade" />
      <span v-if="stats.pending + stats.failed > 0" class="upload-queue__badge">
        {{ stats.pending + stats.failed }}
      </span>
    </div>

    <!-- 队列面板 -->
    <van-popup v-model:show="panelOpen" position="bottom" round :style="{ height: '60%' }">
      <div class="upload-queue__panel">
        <div class="upload-queue__header">
          <span class="title">上传队列</span>
          <div class="stats">
            <span class="stat-item success">完成 {{ stats.success }}</span>
            <span class="stat-item pending">待传 {{ stats.pending }}</span>
            <span class="stat-item failed">失败 {{ stats.failed }}</span>
          </div>
        </div>

        <div v-if="queue.length === 0" class="upload-queue__empty">
          <van-empty description="暂无上传任务" />
        </div>

        <div v-else class="upload-queue__list">
          <div v-for="item in queue" :key="item.localId" class="upload-queue__item">
            <img v-if="item.thumbnail" :src="item.thumbnail" class="thumb" alt="" />
            <div v-else class="thumb thumb--placeholder">
              <van-icon name="photo" />
            </div>
            <div class="info">
              <div class="name ellipsis">{{ item.name }}</div>
              <div class="meta">
                <span class="status" :class="`status--${item.status.toLowerCase()}`">
                  {{ statusText(item.status) }}
                </span>
                <span v-if="item.status === 'UPLOADING'" class="percent">{{ progressPercent(item) }}%</span>
                <span v-if="item.retryCount && item.status === 'FAILED'" class="retry">
                  重试 {{ item.retryCount }}/{{ maxRetry }}
                </span>
              </div>
              <van-progress
                v-if="item.status === 'UPLOADING'"
                :percentage="progressPercent(item)"
                :show-pivot="false"
                stroke-width="2"
              />
            </div>
            <div class="actions">
              <van-icon
                v-if="item.status === 'FAILED'"
                name="replay"
                class="action-icon touchable"
                @click="retry(item.localId)"
              />
              <van-icon
                v-if="item.status !== 'UPLOADING'"
                name="cross"
                class="action-icon touchable"
                @click="remove(item.localId)"
              />
            </div>
          </div>
        </div>

        <div class="upload-queue__footer">
          <van-button v-if="stats.pending > 0" type="primary" block :loading="isUploading" @click="start">
            开始上传（{{ stats.pending }}）
          </van-button>
          <van-button v-if="stats.success > 0" plain block @click="clearSuccess">
            清理已完成
          </van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<style scoped lang="scss">
.upload-queue {
  &__fab {
    position: fixed;
    right: 16px;
    bottom: calc(80px + var(--safe-bottom));
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: var(--brand-primary);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 12px rgba(22, 119, 255, 0.4);
    z-index: 99;
    font-size: 22px;
  }

  &__badge {
    position: absolute;
    top: -4px;
    right: -4px;
    min-width: 18px;
    height: 18px;
    padding: 0 4px;
    border-radius: 9px;
    background: var(--color-danger);
    color: #fff;
    font-size: 11px;
    line-height: 18px;
    text-align: center;
  }

  &__panel {
    display: flex;
    flex-direction: column;
    height: 100%;
  }

  &__header {
    padding: 16px;
    border-bottom: 1px solid var(--border-color-light);
    display: flex;
    align-items: center;
    justify-content: space-between;

    .title {
      font-size: 16px;
      font-weight: 600;
    }

    .stats {
      display: flex;
      gap: 12px;
      font-size: 12px;
    }

    .stat-item {
      &.success { color: var(--color-success); }
      &.pending { color: var(--color-warning); }
      &.failed { color: var(--color-danger); }
    }
  }

  &__list {
    flex: 1;
    overflow-y: auto;
    padding: 8px 16px;
  }

  &__item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 0;
    border-bottom: 1px solid var(--border-color-light);

    .thumb {
      width: 48px;
      height: 48px;
      border-radius: var(--radius-sm);
      object-fit: cover;
      background: var(--bg-disabled);

      &--placeholder {
        display: flex;
        align-items: center;
        justify-content: center;
        color: var(--color-text-placeholder);
        font-size: 20px;
      }
    }

    .info {
      flex: 1;
      min-width: 0;

      .name {
        font-size: 13px;
        color: var(--color-text-primary);
        margin-bottom: 4px;
      }

      .meta {
        display: flex;
        gap: 8px;
        font-size: 11px;
        align-items: center;

        .status {
          &--pending { color: var(--color-warning); }
          &--uploading { color: var(--brand-primary); }
          &--success { color: var(--color-success); }
          &--failed { color: var(--color-danger); }
        }

        .retry {
          color: var(--color-danger);
        }
      }
    }

    .actions {
      display: flex;
      gap: 8px;

      .action-icon {
        font-size: 18px;
        color: var(--color-text-secondary);
        padding: 6px;
      }
    }
  }

  &__footer {
    padding: 12px 16px calc(12px + var(--safe-bottom));
    border-top: 1px solid var(--border-color-light);
    display: flex;
    gap: 8px;
  }
}
</style>
