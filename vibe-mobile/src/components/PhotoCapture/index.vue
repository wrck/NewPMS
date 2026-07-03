<script setup lang="ts">
/**
 * 拍照/相册选图组件（设计文档 3.4.2 施工拍照）
 * - 调用相机（capture）或相册
 * - 支持多选、预览、删除
 * - 选中后自动获取 GPS + 时间，可选拍照水印（时间+GPS+上传人）
 * - 自动压缩（长边 ≤ 2048px，质量 0.85）+ 生成缩略图
 */
import { ref, computed } from 'vue'
import { showImagePreview, showToast } from 'vant'
import dayjs from 'dayjs'
import { getCurrentLocation, type LocationResult } from '@/utils/gps'
import { compressImage, drawWatermark, makeThumbnail } from '@/utils/image'

export interface CapturedPhoto {
  /** 本地唯一ID */
  localId: string
  /** 原始文件名 */
  name: string
  /** 处理后的 Blob（压缩/水印后） */
  blob: Blob
  /** 预览 URL */
  url: string
  /** 缩略图 base64 */
  thumbnail: string
  /** 经度 */
  longitude?: number
  /** 纬度 */
  latitude?: number
  /** 拍照时间 */
  capturedAt: string
  /** 拍照地址描述 */
  address?: string
  /** 文件大小 */
  size: number
}

interface Props {
  /** 最多选择数量 */
  maxCount?: number
  /** 是否仅拍照（false 时可选相册） */
  cameraOnly?: boolean
  /** 是否添加水印 */
  watermark?: boolean
  /** 上传人姓名（水印用） */
  uploader?: string
}

const props = withDefaults(defineProps<Props>(), {
  maxCount: 9,
  cameraOnly: false,
  watermark: true,
  uploader: ''
})

const emit = defineEmits<{
  (e: 'change', photos: CapturedPhoto[]): void
  (e: 'add', photo: CapturedPhoto): void
  (e: 'remove', localId: string): void
}>()

const photos = ref<CapturedPhoto[]>([])
const processing = ref(false)

const count = computed(() => photos.value.length)
const canAdd = computed(() => count.value < props.maxCount)

const cameraInput = ref<HTMLInputElement | null>(null)
const albumInput = ref<HTMLInputElement | null>(null)

function genId(): string {
  return `photo_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

function openCamera(): void {
  if (!canAdd.value) {
    showToast(`最多选择 ${props.maxCount} 张`)
    return
  }
  cameraInput.value?.click()
}

function openAlbum(): void {
  if (!canAdd.value) {
    showToast(`最多选择 ${props.maxCount} 张`)
    return
  }
  albumInput.value?.click()
}

/** 处理文件选择：压缩 + 水印 + 缩略图 + GPS */
async function onFileChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const files = input.files
  if (!files || files.length === 0) return

  processing.value = true
  const remaining = props.maxCount - count.value
  const list = Array.from(files).slice(0, remaining)

  // 多张照片共享一次定位
  let loc: LocationResult | undefined
  try {
    loc = await getCurrentLocation()
  } catch (e) {
    console.warn('[PhotoCapture] location failed', e)
  }
  const capturedAt = dayjs().format('YYYY-MM-DD HH:mm:ss')
  const locationText = loc
    ? `${loc.longitude.toFixed(6)},${loc.latitude.toFixed(6)}`
    : undefined

  try {
    for (const file of list) {
      if (!file.type.startsWith('image/')) {
        showToast(`${file.name} 不是图片文件`)
        continue
      }
      try {
        // 1. 压缩（长边 ≤ 2048px，质量 0.85）
        let blob: Blob = await compressImage(file, {
          maxWidth: 2048,
          maxHeight: 2048,
          quality: 0.85,
          mimeType: 'image/jpeg'
        })
        // 2. 水印
        if (props.watermark) {
          blob = await drawWatermark(blob, {
            time: capturedAt,
            location: locationText,
            uploader: props.uploader || undefined
          })
        }
        // 3. 缩略图
        const thumbnail = await makeThumbnail(blob, 200)
        const photo: CapturedPhoto = {
          localId: genId(),
          name: file.name || `photo_${dayjs().format('YYYYMMDD_HHmmss')}.jpg`,
          blob,
          url: URL.createObjectURL(blob),
          thumbnail,
          longitude: loc?.longitude,
          latitude: loc?.latitude,
          capturedAt,
          address: loc?.address,
          size: blob.size
        }
        photos.value.push(photo)
        emit('add', photo)
      } catch (e: any) {
        console.error('[PhotoCapture] process failed', file.name, e)
      }
    }
    emit('change', photos.value)
  } finally {
    processing.value = false
    input.value = ''
  }
}

function preview(index: number): void {
  showImagePreview({ images: photos.value.map((p) => p.url), startPosition: index })
}

function remove(localId: string): void {
  const idx = photos.value.findIndex((p) => p.localId === localId)
  if (idx >= 0) {
    const removed = photos.value[idx]
    if (removed?.url) URL.revokeObjectURL(removed.url)
    photos.value.splice(idx, 1)
    emit('remove', localId)
    emit('change', photos.value)
  }
}

function clear(): void {
  photos.value.forEach((p) => p.url && URL.revokeObjectURL(p.url))
  photos.value = []
  emit('change', photos.value)
}

function getPhotos(): CapturedPhoto[] {
  return photos.value
}

defineExpose({ openCamera, openAlbum, remove, clear, getPhotos, photos })
</script>

<template>
  <div class="photo-capture">
    <div class="photo-capture__actions">
      <van-button
        icon="photograph"
        type="primary"
        size="small"
        class="touchable"
        :loading="processing"
        :disabled="!canAdd"
        @click="openCamera"
      >
        拍照
      </van-button>
      <van-button
        icon="photo-o"
        type="default"
        size="small"
        class="touchable"
        :loading="processing"
        :disabled="!canAdd"
        @click="openAlbum"
      >
        相册
      </van-button>
      <span class="photo-capture__count">{{ count }}/{{ maxCount }}</span>
    </div>

    <!-- 隐藏的 input：相机 capture + 相册多选 -->
    <input
      ref="cameraInput"
      type="file"
      accept="image/*"
      capture="environment"
      class="photo-capture__input"
      @change="onFileChange"
    />
    <input
      ref="albumInput"
      type="file"
      accept="image/*"
      multiple
      class="photo-capture__input"
      @change="onFileChange"
    />

    <div v-if="photos.length > 0" class="photo-capture__list">
      <div v-for="(photo, idx) in photos" :key="photo.localId" class="photo-capture__item">
        <img :src="photo.thumbnail" alt="预览" @click="preview(idx)" />
        <span class="photo-capture__del touchable" @click="remove(photo.localId)">
          <van-icon name="cross" size="12" />
        </span>
        <span v-if="photo.longitude" class="photo-capture__gps">📍</span>
      </div>
    </div>

    <van-loading v-if="processing" type="spinner" class="photo-capture__loading" />
  </div>
</template>

<style scoped lang="scss">
.photo-capture {
  &__actions {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
  }

  &__count {
    margin-left: auto;
    font-size: 13px;
    color: var(--color-text-secondary);
  }

  &__input {
    display: none;
  }

  &__list {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 8px;
  }

  &__item {
    position: relative;
    aspect-ratio: 1;
    border-radius: var(--radius-sm);
    overflow: hidden;
    background: var(--bg-disabled);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  &__del {
    position: absolute;
    top: 2px;
    right: 2px;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.55);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__gps {
    position: absolute;
    bottom: 2px;
    left: 2px;
    font-size: 12px;
    background: rgba(0, 0, 0, 0.45);
    border-radius: 4px;
    padding: 0 2px;
  }

  &__loading {
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    z-index: 2000;
  }
}
</style>
