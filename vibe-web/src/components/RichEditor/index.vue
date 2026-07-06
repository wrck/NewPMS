<script setup lang="ts">
/**
 * RichEditor 富文本编辑器组件（spec 阶段三 - Task 15）
 *
 * 基于 wangEditor 5 + @wangeditor/editor-for-vue 封装，提供：
 *   1. v-model 双向绑定（HTML / 纯文本两种输出格式）
 *   2. mode（default / simple）+ excludeKeys 自定义工具栏
 *   3. readonly 只读模式（隐藏工具栏，仅展示内容）
 *   4. 图片上传：默认走后端 /files/presign 接口预签名 + PUT 直传 MinIO；
 *      也可通过 insertImage 方法由父组件（如 FileUpload）外部触发插入
 *   5. 暴露 getEditor / getHtml / getText / clear / focus / blur / insertImage 方法
 *
 * 使用示例见 ./README.md
 */
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import { onBeforeUnmount, ref, shallowRef, watch, computed } from 'vue'
import { message } from 'ant-design-vue'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'
import { useUserStore } from '@/stores/user'
import type {
  RichEditorProps,
  RichEditorEmits,
  PresignResponse
} from './types'

/* ============ Props ============ */
const props = withDefaults(defineProps<RichEditorProps>(), {
  height: 400,
  mode: 'default',
  readonly: false,
  outputFormat: 'html',
  placeholder: '请输入内容...',
  excludeKeys: () => ['fullScreen', 'group-video'],
  uploadBucket: 'documents',
  maxImageSize: 10
})

/* ============ Emits ============ */
const emit = defineEmits<RichEditorEmits>()

/* ============ 内部状态 ============ */
/**
 * editorRef 必须使用 shallowRef：wangEditor 实例是复杂对象（包含 selection、
 * 编辑器 DOM 等），深度响应式代理会破坏其内部状态。
 */
const editorRef = shallowRef<IDomEditor | null>(null)

/** v-model 内部值：始终为 HTML 字符串（wangEditor 仅支持 HTML 输入输出） */
const valueHtml = ref<string>(props.modelValue || '')

/* ============ userStore（用于图片上传时携带 token） ============ */
const userStore = useUserStore()

/* ============ API 基础地址 ============ */
const apiBaseURL = (import.meta.env.VITE_API_BASE_URL as string) || '/api/v1'

/* ============ 工具栏高度（与 wangEditor 默认 toolbar 高度对齐） ============ */
const TOOLBAR_HEIGHT = 50

/* ============ 工具栏配置 ============ */
const toolbarConfig: Partial<IToolbarConfig> = {
  excludeKeys: props.excludeKeys
}

/* ============ 编辑器配置 ============ */
const editorConfig = computed<Partial<IEditorConfig>>(() => ({
  placeholder: props.placeholder,
  readOnly: props.readonly,
  MENU_CONF: {
    uploadImage: {
      // 自定义图片上传：调用后端 presign 接口预签名，然后 PUT 直传 MinIO
      async customUpload(
        file: File,
        insertFn: (url: string, alt?: string, href?: string) => void
      ) {
        try {
          // 1. 文件类型校验
          if (!file.type.startsWith('image/')) {
            message.error('仅支持上传图片文件')
            emit('upload-error', { file, error: new Error('invalid file type') })
            return
          }
          // 2. 文件大小校验
          const sizeMB = file.size / 1024 / 1024
          if (sizeMB > props.maxImageSize) {
            message.error(`图片大小不能超过 ${props.maxImageSize}MB`)
            emit('upload-error', { file, error: new Error('file too large') })
            return
          }
          // 3. 调用后端 /files/presign 获取预签名 PUT URL
          const presignRes = await fetch(`${apiBaseURL}/files/presign`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              Authorization: `Bearer ${userStore.token}`
            },
            body: JSON.stringify({
              fileName: file.name,
              contentType: file.type,
              fileSize: file.size,
              bucket: props.uploadBucket
            })
          })
          if (!presignRes.ok) {
            throw new Error(`presign failed: ${presignRes.status}`)
          }
          const respJson = (await presignRes.json()) as {
            code: number
            data: PresignResponse
            message?: string
          }
          const { uploadUrl, accessUrl } = respJson.data
          // 4. PUT 直传 MinIO
          const putRes = await fetch(uploadUrl, {
            method: 'PUT',
            body: file,
            headers: { 'Content-Type': file.type }
          })
          if (!putRes.ok) {
            throw new Error(`upload failed: ${putRes.status}`)
          }
          // 5. 插入图片到编辑器
          insertFn(accessUrl, file.name, accessUrl)
          emit('upload-success', { url: accessUrl, alt: file.name, file })
        } catch (err) {
          const error = err instanceof Error ? err : new Error(String(err))
          console.error('[RichEditor] image upload failed:', error)
          message.error(`图片上传失败：${error.message}`)
          emit('upload-error', { file, error })
        }
      }
    }
  }
}))

/* ============ 监听外部 modelValue 变化 ============ */
watch(
  () => props.modelValue,
  (val) => {
    if (val !== valueHtml.value) {
      valueHtml.value = val || ''
    }
  }
)

/* ============ 编辑器生命周期回调 ============ */
function handleCreated(editor: IDomEditor): void {
  editorRef.value = editor
  emit('created', editor)
}

function handleChange(editor: IDomEditor): void {
  let output = ''
  if (props.outputFormat === 'text') {
    output = editor.getText()
  } else {
    output = editor.getHtml()
  }
  emit('update:modelValue', output)
  emit('change', output)
}

/* ============ 组件卸载：销毁编辑器避免内存泄漏 ============ */
onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor) {
    editor.destroy()
    editorRef.value = null
  }
})

/* ============ Expose 方法 ============ */
function getEditor(): IDomEditor | null {
  return editorRef.value
}

function getHtml(): string {
  return editorRef.value?.getHtml() ?? ''
}

function getText(): string {
  return editorRef.value?.getText() ?? ''
}

function clear(): void {
  editorRef.value?.clear()
}

function focus(): void {
  editorRef.value?.focus()
}

function blur(): void {
  editorRef.value?.blur()
}

/**
 * 手动插入图片：供父组件（如 FileUpload 拖入）外部调用
 * @param url 图片访问地址
 * @param alt alt 文本（默认空字符串）
 * @param href 点击跳转链接（默认同 url）
 */
function insertImage(url: string, alt = '', href?: string): void {
  const editor = getEditor()
  if (!editor) {
    message.warning('编辑器尚未初始化完成')
    return
  }
  editor.dangerouslyInsertHtml(
    `<img src="${url}" alt="${alt}" href="${href ?? url}" />`
  )
}

defineExpose({
  getEditor,
  getHtml,
  getText,
  clear,
  focus,
  blur,
  insertImage
})

/* ============ 计算样式：编辑器内容区高度 ============ */
const editorHeight = computed(() =>
  props.readonly ? props.height : Math.max(props.height - TOOLBAR_HEIGHT, 100)
)
</script>

<template>
  <div
    class="rich-editor-container"
    :class="{ 'is-readonly': readonly }"
    :style="{ height: height + 'px' }"
  >
    <Toolbar
      v-if="!readonly"
      :editor="editorRef as unknown as IDomEditor"
      :default-config="toolbarConfig"
      :mode="mode"
      class="rich-editor-toolbar"
    />
    <Editor
      v-model="valueHtml"
      :default-config="editorConfig"
      :mode="mode"
      :style="{ height: editorHeight + 'px', overflowY: 'hidden' }"
      class="rich-editor-content"
      @on-created="handleCreated"
      @on-change="handleChange"
    />
  </div>
</template>

<style lang="less" scoped>
.rich-editor-container {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  overflow: hidden;
  z-index: 100;
  background-color: #fff;

  &.is-readonly {
    border-color: #f0f0f0;
  }
}

.rich-editor-toolbar {
  border-bottom: 1px solid #e8e8e8;
}

.rich-editor-content {
  overflow-y: auto;
}
</style>
