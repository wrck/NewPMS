<template>
  <div class="file-upload">
    <!-- 已上传文件列表 -->
    <ul v-if="items.length" class="file-upload__list">
      <li v-for="item in items" :key="item.localId" class="file-item">
        <div class="file-item__icon" :class="`file-item__icon--${fileTypeIcon(item.name)}`">
          <van-icon :name="fileIconName(item.name)" size="20" />
          <span>{{ extName(item.name) }}</span>
        </div>
        <div class="file-item__body">
          <div class="file-item__name ellipsis">{{ item.name }}</div>
          <div class="file-item__meta">
            <span v-if="item.size">{{ formatSize(item.size) }}</span>
            <span v-if="item.status === 'UPLOADING'" class="file-item__status file-item__status--loading">
              上传中 {{ item.percent }}%
            </span>
            <span v-else-if="item.status === 'FAILED'" class="file-item__status file-item__status--failed">
              上传失败
            </span>
            <span v-else-if="item.status === 'SUCCESS'" class="file-item__status file-item__status--success">
              <van-icon name="success" size="12" /> 已上传
            </span>
          </div>
        </div>
        <div class="file-item__actions">
          <van-icon
            v-if="!disabled && item.status === 'FAILED'"
            name="replay"
            size="16"
            color="#1677ff"
            @click="retryUpload(item)"
          />
          <van-icon
            v-if="!disabled"
            name="cross"
            size="16"
            color="#8c8c8c"
            @click="onRemove(item)"
          />
        </div>
      </li>
    </ul>

    <!-- 添加按钮 -->
    <div
      v-if="!disabled && (multiple || !items.length) && items.length < maxCount"
      class="file-upload__trigger"
      @click="onPick"
    >
      <van-icon name="description" size="20" color="#1677ff" />
      <span>{{ buttonText }}</span>
    </div>

    <div v-if="hint" class="file-upload__hint">{{ hint }}</div>

    <!-- 隐藏的 input -->
    <input
      ref="inputRef"
      type="file"
      :accept="accept"
      :multiple="multiple"
      hidden
      @change="onChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { showConfirmDialog, showToast, showSuccessToast } from 'vant'
import { genLocalId } from '@/utils/image'
import { http } from '@/utils/request'

interface Props {
  /** 双向绑定的已上传 URL 列表（单文件时取第一项） */
  modelValue?: string | string[]
  /** 是否多选，默认 false */
  multiple?: boolean
  /** 最大文件数（多选时生效） */
  maxCount?: number
  /** 是否禁用 */
  disabled?: boolean
  /** 上传地址 */
  uploadUrl?: string
  /** 接受的文件类型，如 .pdf,.doc,.docx */
  accept?: string
  /** 单文件最大尺寸（MB），默认 20 */
  maxSize?: number
  /** 提示文字 */
  hint?: string
  /** 按钮文案 */
  buttonText?: string
  /** 鉴权作用域 */
  auth?: 'customer' | 'agent'
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  multiple: false,
  maxCount: 5,
  disabled: false,
  uploadUrl: '/v1/files/upload',
  accept: '.pdf,.doc,.docx,.xls,.xlsx,image/*',
  maxSize: 20,
  hint: '',
  buttonText: '上传文件',
  auth: 'agent'
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: string | string[]): void
  (e: 'change', items: FileItem[]): void
}>()

interface FileItem {
  localId: string
  name: string
  size: number
  type: string
  remoteUrl?: string
  status: 'PENDING' | 'UPLOADING' | 'SUCCESS' | 'FAILED'
  percent: number
  /** 保留原始 File 用于重试 */
  rawFile?: File
}

const items = ref<FileItem[]>([])
const inputRef = ref<HTMLInputElement>()

/** 远端 URL 列表（成功项） */
const remoteUrls = computed(() =>
  items.value.filter((i) => i.status === 'SUCCESS' && i.remoteUrl).map((i) => i.remoteUrl as string)
)

// 同步外部 modelValue 到内部
watch(
  () => props.modelValue,
  (val) => {
    const urls = Array.isArray(val) ? val : val ? [val] : []
    const current = remoteUrls.value
    if (urls.length === current.length && urls.every((u, i) => u === current[i])) return
    items.value = urls.map((url) => ({
      localId: genLocalId(),
      name: decodeURIComponent(url.split('/').pop() || 'file'),
      size: 0,
      type: '',
      remoteUrl: url,
      status: 'SUCCESS',
      percent: 100
    }))
  },
  { immediate: true }
)

function emitChange() {
  const urls = remoteUrls.value
  if (props.multiple) {
    emit('update:modelValue', urls)
  } else {
    emit('update:modelValue', urls[0] || '')
  }
  emit('change', items.value)
}

function onPick() {
  inputRef.value?.click()
}

async function onChange(e: Event) {
  const input = e.target as HTMLInputElement
  const files = input.files
  if (!files || !files.length) return

  const remain = props.maxCount - items.value.length
  if (remain <= 0) {
    showToast(`最多上传 ${props.maxCount} 个文件`)
    input.value = ''
    return
  }

  const list = Array.from(files).slice(0, remain)
  for (const file of list) {
    // 校验大小
    if (file.size > props.maxSize * 1024 * 1024) {
      showToast(`${file.name} 超过 ${props.maxSize}MB 限制`)
      continue
    }
    const item: FileItem = {
      localId: genLocalId(),
      name: file.name,
      size: file.size,
      type: file.type,
      status: 'UPLOADING',
      percent: 0,
      rawFile: file
    }
    items.value.push(item)
    uploadOne(item, file)
  }
  input.value = ''
}

async function uploadOne(item: FileItem, file: File) {
  try {
    const formData = new FormData()
    formData.append('file', file, file.name)
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

function retryUpload(item: FileItem) {
  if (!item.rawFile) {
    showToast('请删除后重新上传')
    return
  }
  item.status = 'UPLOADING'
  item.percent = 0
  uploadOne(item, item.rawFile)
}

function onRemove(item: FileItem) {
  showConfirmDialog({
    title: '提示',
    message: `确定删除「${item.name}」吗？`
  })
    .then(() => {
      items.value = items.value.filter((i) => i.localId !== item.localId)
      emitChange()
    })
    .catch(() => {})
}

/** 文件扩展名 */
function extName(name: string): string {
  const ext = name.split('.').pop() || ''
  return ext.toUpperCase().slice(0, 4)
}

/** 文件类型图标颜色分类 */
function fileTypeIcon(name: string): string {
  const ext = (name.split('.').pop() || '').toLowerCase()
  if (ext === 'pdf') return 'pdf'
  if (['doc', 'docx'].includes(ext)) return 'doc'
  if (['xls', 'xlsx'].includes(ext)) return 'xls'
  if (['png', 'jpg', 'jpeg', 'gif', 'bmp'].includes(ext)) return 'img'
  return 'other'
}

/** 文件图标 */
function fileIconName(name: string): string {
  const ext = (name.split('.').pop() || '').toLowerCase()
  if (['png', 'jpg', 'jpeg', 'gif', 'bmp'].includes(ext)) return 'photo-o'
  return 'description'
}

/** 格式化文件大小 */
function formatSize(size: number): string {
  if (size < 1024) return `${size}B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)}KB`
  return `${(size / 1024 / 1024).toFixed(1)}MB`
}

defineExpose({
  /** 校验是否全部上传完成 */
  validate: (): boolean => {
    if (items.value.some((i) => i.status !== 'SUCCESS')) {
      showToast('存在未上传完成的文件')
      return false
    }
    return true
  },
  /** 获取已上传 URL */
  getUrls: (): string[] => remoteUrls.value
})
</script>

<style lang="scss" scoped>
.file-upload {
  &__list {
    .file-item + .file-item {
      margin-top: 8px;
    }
  }

  &__trigger {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 12px;
    border: 1px dashed #d9d9d9;
    border-radius: 8px;
    background: #fafbfc;
    color: var(--brand-primary);
    font-size: 14px;
    cursor: pointer;

    &:active {
      background: #f0f5ff;
    }
  }

  &__hint {
    margin-top: 8px;
    font-size: 12px;
    color: var(--color-text-secondary);
  }
}

.file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #fafbfc;
  border-radius: 8px;
  border: 1px solid var(--border-color-light);

  &__icon {
    width: 36px;
    height: 44px;
    border-radius: 4px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 9px;
    flex-shrink: 0;

    &--pdf {
      background: #ff4d4f;
    }
    &--doc {
      background: #1677ff;
    }
    &--xls {
      background: #52c41a;
    }
    &--img {
      background: #faad14;
    }
    &--other {
      background: #8c8c8c;
    }
  }

  &__body {
    flex: 1;
    min-width: 0;

    .file-item__name {
      font-size: 13px;
      color: var(--color-text-primary);
      line-height: 1.4;
    }

    .file-item__meta {
      margin-top: 4px;
      display: flex;
      gap: 8px;
      font-size: 12px;
      color: var(--color-text-secondary);
    }
  }

  &__status {
    display: inline-flex;
    align-items: center;
    gap: 2px;

    &--loading {
      color: var(--brand-primary);
    }
    &--failed {
      color: var(--color-danger);
    }
    &--success {
      color: var(--color-success);
    }
  }

  &__actions {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
    padding: 4px;
  }
}
</style>
