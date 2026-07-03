<template>
  <div class="photo-upload">
    <div class="photo-upload__grid">
      <!-- 已选图片预览 -->
      <div
        v-for="(item, index) in items"
        :key="item.localId"
        class="photo-upload__item"
        @click="onPreview(index)"
      >
        <img :src="item.thumbnail || item.url" :alt="item.name" />
        <div v-if="item.status === 'UPLOADING'" class="photo-upload__mask">
          <van-loading type="spinner" size="20" color="#fff" />
          <span>{{ item.percent }}%</span>
        </div>
        <div
          v-else-if="item.status === 'FAILED'"
          class="photo-upload__mask photo-upload__mask--failed"
          @click.stop="retryUpload(item)"
        >
          <van-icon name="warning" size="20" />
          <span>点我重试</span>
        </div>
        <span
          v-if="!disabled"
          class="photo-upload__remove"
          @click.stop="onRemove(item)"
        >
          <van-icon name="cross" size="12" color="#fff" />
        </span>
      </div>

      <!-- 添加按钮 -->
      <div
        v-if="!disabled && items.length < maxCount"
        class="photo-upload__add"
        @click="onPick"
      >
        <van-icon name="photograph" size="24" color="#8c8c8c" />
        <span>{{ items.length }}/{{ maxCount }}</span>
      </div>
    </div>

    <div v-if="hint" class="photo-upload__hint">{{ hint }}</div>

    <!-- 拍照/相册选择 ActionSheet -->
    <van-action-sheet
      v-model:show="showAction"
      :actions="actions"
      cancel-text="取消"
      close-on-click-action
      :round="true"
    />

    <!-- 隐藏的 input，用于相册选择 -->
    <input
      ref="albumInput"
      type="file"
      accept="image/*"
      multiple
      hidden
      @change="onAlbumChange"
    />
    <!-- 隐藏的 input，用于拍照 -->
    <input
      ref="cameraInput"
      type="file"
      accept="image/*"
      capture="environment"
      hidden
      @change="onCameraChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { showImagePreview, showConfirmDialog, showToast } from 'vant'
import { compressImage, createObjectURL, genLocalId, type WatermarkOptions } from '@/utils/image'
import { http } from '@/utils/request'
import type { UploadFileMeta } from '@/types/api'

interface Props {
  /** 双向绑定的已上传 URL 列表 */
  modelValue?: string[]
  /** 最大张数 */
  maxCount?: number
  /** 最少张数 */
  minCount?: number
  /** 是否禁用 */
  disabled?: boolean
  /** 上传地址 */
  uploadUrl?: string
  /** 提示文字 */
  hint?: string
  /** 鉴权作用域 */
  auth?: 'customer' | 'agent'
  /** 水印配置，传入则在压缩时叠加水印（默认开启时间戳水印，用于施工取证） */
  watermark?: WatermarkOptions | boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  maxCount: 9,
  minCount: 1,
  disabled: false,
  uploadUrl: '/v1/files/upload',
  hint: '',
  auth: 'agent',
  watermark: true
})

const emit = defineEmits<{
  (e: 'update:modelValue', urls: string[]): void
  (e: 'change', items: UploadFileMeta[]): void
}>()

interface PhotoItem extends UploadFileMeta {
  url: string
  percent: number
}

const items = ref<PhotoItem[]>([])
const showAction = ref(false)
const albumInput = ref<HTMLInputElement>()
const cameraInput = ref<HTMLInputElement>()

const actions = [
  { name: '拍照', callback: () => triggerCamera() },
  { name: '从相册选择', callback: () => triggerAlbum() }
]

/** 当前 URL 列表（成功项） */
const remoteUrls = computed(() =>
  items.value.filter((i) => i.status === 'SUCCESS' && i.remoteUrl).map((i) => i.remoteUrl as string)
)

/** 解析水印配置：boolean true → 默认时间戳水印；对象 → 透传 */
const watermarkConfig = computed<WatermarkOptions | undefined>(() => {
  const w = props.watermark
  if (w === false) return undefined
  if (w === true) return {}
  return w
})

// 同步外部 modelValue 到内部（仅在初始化或外部变更时）
watch(
  () => props.modelValue,
  (urls) => {
    const current = remoteUrls.value
    if (urls.length === current.length && urls.every((u, i) => u === current[i])) return
    items.value = urls.map((url) => ({
      localId: genLocalId(),
      name: url.split('/').pop() || 'photo',
      size: 0,
      type: 'image/jpeg',
      url,
      remoteUrl: url,
      status: 'SUCCESS',
      percent: 100
    }))
  },
  { immediate: true }
)

function emitChange() {
  emit('update:modelValue', remoteUrls.value)
  emit('change', items.value as unknown as UploadFileMeta[])
}

function onPick() {
  showAction.value = true
}

function triggerAlbum() {
  showAction.value = false
  albumInput.value?.click()
}

function triggerCamera() {
  showAction.value = false
  cameraInput.value?.click()
}

async function handleFiles(files: FileList | null) {
  if (!files || !files.length) return
  const remain = props.maxCount - items.value.length
  if (remain <= 0) {
    showToast(`最多上传 ${props.maxCount} 张`)
    return
  }
  const list = Array.from(files).slice(0, remain)

  for (const file of list) {
    if (!file.type.startsWith('image/')) {
      showToast(`${file.name} 不是图片`)
      continue
    }
    const item: PhotoItem = {
      localId: genLocalId(),
      name: file.name,
      size: file.size,
      type: file.type,
      url: createObjectURL(file),
      thumbnail: createObjectURL(file),
      status: 'UPLOADING',
      percent: 0
    }
    items.value.push(item)
    // 异步压缩（含水印）并上传
    uploadOne(item, file)
  }
}

async function uploadOne(item: PhotoItem, file: File) {
  try {
    const compressed = await compressImage(file, {
      maxSize: 1280,
      quality: 0.7,
      watermark: watermarkConfig.value
    })
    const formData = new FormData()
    formData.append('file', compressed, compressed.name)
    const url = await http.upload<string>(props.uploadUrl, formData, {
      authScope: props.auth,
      silent: true,
      onProgress: (p) => {
        item.percent = p
      }
    })
    item.remoteUrl = url
    item.status = 'SUCCESS'
    item.percent = 100
    emitChange()
  } catch (e) {
    item.status = 'FAILED'
    showToast(`${item.name} 上传失败`)
  }
}

function retryUpload(item: PhotoItem) {
  // 简化：提示重新选择（完整重试需保留原始 File，此处仅做占位提示）
  showToast('请删除后重新上传')
}

function onAlbumChange(e: Event) {
  handleFiles((e.target as HTMLInputElement).files)
  ;(e.target as HTMLInputElement).value = ''
}

function onCameraChange(e: Event) {
  handleFiles((e.target as HTMLInputElement).files)
  ;(e.target as HTMLInputElement).value = ''
}

function onRemove(item: PhotoItem) {
  showConfirmDialog({
    title: '提示',
    message: '确定删除该图片吗？'
  })
    .then(() => {
      items.value = items.value.filter((i) => i.localId !== item.localId)
      emitChange()
    })
    .catch(() => {})
}

function onPreview(index: number) {
  const urls = items.value.map((i) => i.url)
  showImagePreview({ images: urls, startPosition: index })
}

defineExpose({
  /** 校验最少张数 */
  validate: (): boolean => {
    if (items.value.length < props.minCount) {
      showToast(`至少上传 ${props.minCount} 张`)
      return false
    }
    if (items.value.some((i) => i.status !== 'SUCCESS')) {
      showToast('存在未上传完成的图片')
      return false
    }
    return true
  },
  /** 获取已上传 URL */
  getUrls: (): string[] => remoteUrls.value
})
</script>

<style lang="scss" scoped>
.photo-upload {
  &__grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 8px;
  }

  &__item,
  &__add {
    position: relative;
    width: 100%;
    aspect-ratio: 1 / 1;
    border-radius: 8px;
    overflow: hidden;
    background: #f5f5f5;
  }

  &__item img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &__add {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    border: 1px dashed #d9d9d9;
    color: #8c8c8c;
    font-size: 12px;
  }

  &__mask {
    position: absolute;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    color: #fff;
    font-size: 12px;

    &--failed {
      background: rgba(255, 77, 79, 0.7);
    }
  }

  &__remove {
    position: absolute;
    top: 0;
    right: 0;
    width: 18px;
    height: 18px;
    background: rgba(0, 0, 0, 0.55);
    border-radius: 0 8px 0 8px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__hint {
    margin-top: 8px;
    font-size: 12px;
    color: var(--color-text-secondary);
  }
}
</style>
