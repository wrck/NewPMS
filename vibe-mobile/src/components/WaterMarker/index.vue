<script setup lang="ts">
/**
 * 图片水印可视化组件
 * 选择图片 → 预览原图 → 一键加水印 → 预览结果 + 下载
 */
import { ref } from 'vue'
import { showImagePreview, showSuccessToast, showFailToast } from 'vant'
import { useWaterMarker } from './useWaterMarker'
import type { WaterMarkOutput } from './useWaterMarker'

const props = defineProps<{
  /** 上传人姓名 */
  uploader?: string
}>()

const emit = defineEmits<{
  (e: 'processed', payload: WaterMarkOutput & { originalFile: File }): void
}>()

const { process } = useWaterMarker()

const originalUrl = ref('')
const resultUrl = ref('')
const processing = ref(false)
const originalFile = ref<File | null>(null)
const resultBlob = ref<Blob | null>(null)

async function onPick(e: Event): Promise<void> {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  originalFile.value = file
  if (originalUrl.value) URL.revokeObjectURL(originalUrl.value)
  originalUrl.value = URL.createObjectURL(file)
  resultUrl.value = ''
  resultBlob.value = null
  await runProcess()
}

async function runProcess(): Promise<void> {
  if (!originalFile.value) return
  processing.value = true
  try {
    const out = await process(originalFile.value, { uploader: props.uploader })
    if (resultUrl.value) URL.revokeObjectURL(resultUrl.value)
    resultBlob.value = out.blob
    resultUrl.value = URL.createObjectURL(out.blob)
    emit('processed', { ...out, originalFile: originalFile.value })
    showSuccessToast('水印添加成功')
  } catch (e: any) {
    showFailToast(e?.message || '处理失败')
  } finally {
    processing.value = false
  }
}

function previewOriginal(): void {
  if (originalUrl.value) showImagePreview([originalUrl.value])
}

function previewResult(): void {
  if (resultUrl.value) showImagePreview([resultUrl.value])
}

function download(): void {
  if (!resultBlob.value) return
  const url = URL.createObjectURL(resultBlob.value)
  const a = document.createElement('a')
  a.href = url
  a.download = `watermarked_${Date.now()}.jpg`
  a.click()
  URL.revokeObjectURL(url)
}
</script>

<template>
  <div class="water-marker">
    <label class="pick-btn touchable">
      <input type="file" accept="image/*" hidden @change="onPick" />
      <van-icon name="plus" size="22" />
      <span>选择图片加水印</span>
    </label>

    <div v-if="originalUrl" class="compare">
      <div class="cell" @click="previewOriginal">
        <img :src="originalUrl" alt="原图" />
        <span class="tag">原图</span>
      </div>
      <div class="cell" @click="previewResult">
        <van-loading v-if="processing" size="24" />
        <img v-else-if="resultUrl" :src="resultUrl" alt="加水印" />
        <span v-else class="placeholder">处理中…</span>
        <span class="tag ok">含水印</span>
      </div>
    </div>

    <div v-if="resultUrl" class="actions">
      <van-button size="small" plain type="primary" icon="replay" @click="runProcess">
        重新加水印
      </van-button>
      <van-button size="small" type="primary" icon="down" @click="download">
        下载
      </van-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.water-marker {
  width: 100%;
}

.pick-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  min-height: 80px;
  border: 1px dashed var(--border-color);
  border-radius: 8px;
  background: var(--bg-disabled);
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
}

.compare {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.cell {
  position: relative;
  flex: 1;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  background: var(--bg-disabled);
  display: flex;
  align-items: center;
  justify-content: center;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .placeholder {
    font-size: 12px;
    color: var(--color-text-secondary);
  }

  .tag {
    position: absolute;
    bottom: 4px;
    left: 4px;
    background: rgba(0, 0, 0, 0.55);
    color: #fff;
    font-size: 11px;
    padding: 1px 6px;
    border-radius: 4px;

    &.ok {
      background: rgba(22, 119, 255, 0.85);
    }
  }
}

.actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  :deep(.van-button) {
    flex: 1;
  }
}
</style>
