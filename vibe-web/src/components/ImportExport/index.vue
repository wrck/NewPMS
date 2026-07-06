<script setup lang="ts">
/**
 * ImportExport 导入导出按钮组件（spec 阶段三 - Task 20）
 *
 * 一站式封装：
 *   1. 导出按钮：调用后端 export 接口（POST + JSON body），返回 Blob 后浏览器触发下载
 *   2. 导入按钮：触发文件选择 → 上传到 import 接口（multipart/form-data）→ 弹窗展示导入结果
 *   3. 模板下载按钮：GET templateUrl，下载二进制流
 *   4. 文件类型 / 大小校验（默认仅 .xlsx/.xls，10MB 以内）
 *   5. 自动携带 Bearer Token（从 useUserStore 读取）
 *   6. 自动解析 Content-Disposition 中的 filename
 *   7. 错误统一 try/catch + message.error + emit 事件
 *
 * 使用示例见 ./README.md
 */
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  DownloadOutlined,
  UploadOutlined,
  FileExcelOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import type { ImportExportProps, ImportResult } from './types'

/* ============ Props ============ */
const props = withDefaults(defineProps<ImportExportProps>(), {
  exportText: '导出',
  importText: '导入',
  templateText: '下载模板',
  showExport: true,
  showImport: true,
  showTemplate: true,
  size: 'middle',
  exportDisabled: false,
  full: false,
  maxSizeMb: 10,
  autoRefresh: false,
  exportParams: () => ({}),
  importData: () => ({})
})

/* ============ Emits ============ */
const emit = defineEmits<{
  /** 导出成功（文件已开始下载） */
  (e: 'export-success'): void
  /** 导出失败 */
  (e: 'export-error', error: unknown): void
  /** 导入完成（无论成败，返回结果） */
  (e: 'import-success', result: ImportResult): void
  /** 导入失败（网络/后端异常） */
  (e: 'import-error', error: unknown): void
  /** 模板下载成功 */
  (e: 'template-downloaded'): void
  /** 模板下载失败 */
  (e: 'template-error', error: unknown): void
  /** 导入成功后请求父组件刷新数据（autoRefresh=true 时触发） */
  (e: 'refresh'): void
}>()

/* ============ Store ============ */
const userStore = useUserStore()

/* ============ State ============ */
const exportLoading = ref(false)
const importLoading = ref(false)
const templateLoading = ref(false)
const resultVisible = ref(false)
const importResult = ref<ImportResult>({
  success: false,
  successCount: 0,
  failCount: 0,
  totalCount: 0,
  errors: []
})

/* ============ 计算属性 ============ */
/** a-upload 的 headers（携带 Authorization） */
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))

/** 是否显示导出按钮（full 模式优先） */
const isShowExport = computed(() => props.full || props.showExport)
/** 是否显示导入按钮 */
const isShowImport = computed(() => props.full || props.showImport)
/** 是否显示模板下载按钮 */
const isShowTemplate = computed(() => props.full || props.showTemplate)

/* ============ 工具函数 ============ */
/**
 * 从 Content-Disposition 头解析文件名
 * 支持：filename="xxx.xlsx" / filename*=UTF-8''xxx.xlsx
 */
function parseFileName(disposition: string, fallback: string): string {
  if (!disposition) return fallback
  // 优先匹配 filename*（RFC 5987）
  const starMatch = /filename\*=(?:UTF-8'')?([^;]+)/i.exec(disposition)
  if (starMatch && starMatch[1]) {
    try {
      return decodeURIComponent(starMatch[1].replace(/['"]/g, '').trim())
    } catch {
      return starMatch[1].replace(/['"]/g, '').trim()
    }
  }
  // 回退匹配 filename="xxx"
  const match = /filename="?([^";]+)"?/i.exec(disposition)
  if (match && match[1]) {
    return match[1].trim()
  }
  return fallback
}

/**
 * 触发浏览器下载 Blob 文件
 */
function triggerBlobDownload(blob: Blob, fileName: string): void {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.style.display = 'none'
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  // 异步释放，避免在 click 之前被回收
  setTimeout(() => window.URL.revokeObjectURL(url), 0)
}

/** 根据后端响应解析为 ImportResult */
function resolveImportResult(data: any): ImportResult {
  const payload = data?.data ?? data
  return {
    success: data?.code === 200 || payload?.success === true || (payload?.failCount ?? 0) === 0,
    successCount: Number(payload?.successCount ?? 0),
    failCount: Number(payload?.failCount ?? 0),
    totalCount: Number(payload?.totalCount ?? payload?.successCount ?? 0),
    errors: Array.isArray(payload?.errors) ? payload.errors : [],
    duration: typeof payload?.duration === 'number' ? payload.duration : undefined
  }
}

/* ============ 导出 ============ */
async function handleExport(): Promise<void> {
  if (!props.exportApi) {
    message.error('未配置导出接口')
    return
  }
  if (props.exportDisabled) return
  exportLoading.value = true
  try {
    const response = await fetch(props.exportApi, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${userStore.token}`
      },
      body: JSON.stringify(props.exportParams || {})
    })
    if (!response.ok) {
      throw new Error(`导出失败: ${response.status} ${response.statusText}`)
    }
    const contentType = response.headers.get('content-type') || ''
    // 后端返回 JSON（一般是错误信息）
    if (contentType.includes('application/json')) {
      const data = await response.json()
      if (data && typeof data === 'object' && 'code' in data && data.code !== 200) {
        throw new Error(data.message || '导出失败')
      }
      throw new Error(data?.message || '返回格式异常，期望二进制流')
    }
    const disposition = response.headers.get('content-disposition') || ''
    const fileName = parseFileName(
      disposition,
      props.exportFileName || `导出_${Date.now()}.xlsx`
    )
    const blob = await response.blob()
    triggerBlobDownload(blob, fileName)
    message.success('导出成功')
    emit('export-success')
  } catch (e: any) {
    console.error('[ImportExport] 导出失败:', e)
    message.error(e?.message || '导出失败')
    emit('export-error', e)
  } finally {
    exportLoading.value = false
  }
}

/* ============ 导入 ============ */
/** a-upload before-upload 钩子：返回 false 阻止默认上传，由我们自定义上传 */
function handleBeforeUpload(file: File): boolean {
  if (!props.importApi) {
    message.error('未配置导入接口')
    return false
  }
  const isExcel = /\.xlsx?$/i.test(file.name)
  if (!isExcel) {
    message.error('仅支持 Excel 文件 (.xlsx, .xls)')
    return false
  }
  const limitMb = props.maxSizeMb || 10
  const isLtLimit = file.size / 1024 / 1024 < limitMb
  if (!isLtLimit) {
    message.error(`文件大小不能超过 ${limitMb}MB`)
    return false
  }
  importLoading.value = true
  void handleImport(file)
  return false
}

async function handleImport(file: File): Promise<void> {
  const formData = new FormData()
  formData.append('file', file)
  // 追加额外参数
  Object.keys(props.importData).forEach((key) => {
    formData.append(key, String((props.importData as Record<string, unknown>)[key]))
  })
  try {
    const response = await fetch(props.importApi as string, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${userStore.token}`
      },
      body: formData
    })
    const contentType = response.headers.get('content-type') || ''
    if (!response.ok) {
      // 尝试从响应体提取错误信息
      let errMsg = `导入失败: ${response.status} ${response.statusText}`
      if (contentType.includes('application/json')) {
        const errData = await response.json().catch(() => null)
        if (errData?.message) errMsg = errData.message
      }
      throw new Error(errMsg)
    }
    const data = await response.json()
    const result = resolveImportResult(data)
    importResult.value = result
    resultVisible.value = true
    if (result.success && result.failCount === 0) {
      message.success('导入成功')
    } else if (result.failCount > 0 && result.successCount > 0) {
      message.warning(`部分导入失败（成功 ${result.successCount} 行，失败 ${result.failCount} 行）`)
    } else if (result.failCount > 0 && result.successCount === 0) {
      message.error(`导入失败（共 ${result.failCount} 行错误）`)
    } else {
      message.success('导入完成')
    }
    emit('import-success', result)
    if (props.autoRefresh) {
      emit('refresh')
    }
  } catch (e: any) {
    console.error('[ImportExport] 导入失败:', e)
    message.error(e?.message || '导入失败')
    emit('import-error', e)
  } finally {
    importLoading.value = false
  }
}

/* ============ 模板下载 ============ */
async function handleDownloadTemplate(): Promise<void> {
  if (!props.templateUrl) {
    message.error('未配置模板下载地址')
    return
  }
  templateLoading.value = true
  try {
    const response = await fetch(props.templateUrl, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${userStore.token}`
      }
    })
    if (!response.ok) {
      throw new Error(`下载模板失败: ${response.status} ${response.statusText}`)
    }
    const disposition = response.headers.get('content-disposition') || ''
    const fileName = parseFileName(disposition, 'template.xlsx')
    const blob = await response.blob()
    triggerBlobDownload(blob, fileName)
    message.success('模板下载成功')
    emit('template-downloaded')
  } catch (e: any) {
    console.error('[ImportExport] 下载模板失败:', e)
    message.error(e?.message || '下载模板失败')
    emit('template-error', e)
  } finally {
    templateLoading.value = false
  }
}

/* ============ 结果弹窗关闭 ============ */
function handleResultClose(): void {
  resultVisible.value = false
}

/* ============ Expose ============ */
defineExpose({
  export: handleExport,
  downloadTemplate: handleDownloadTemplate,
  closeResult: handleResultClose
})
</script>

<template>
  <a-space :size="size">
    <a-button
      v-if="isShowExport"
      type="primary"
      :loading="exportLoading"
      :disabled="exportDisabled"
      :size="size"
      @click="handleExport"
    >
      <template #icon><DownloadOutlined /></template>
      {{ exportText }}
    </a-button>

    <a-upload
      v-if="isShowImport"
      name="file"
      :headers="uploadHeaders"
      :data="importData"
      :before-upload="handleBeforeUpload"
      :show-upload-list="false"
      accept=".xlsx,.xls"
      :disabled="importLoading"
    >
      <a-button :loading="importLoading" :size="size">
        <template #icon><UploadOutlined /></template>
        {{ importText }}
      </a-button>
    </a-upload>

    <a-button
      v-if="isShowTemplate"
      :loading="templateLoading"
      :size="size"
      @click="handleDownloadTemplate"
    >
      <template #icon><FileExcelOutlined /></template>
      {{ templateText }}
    </a-button>
  </a-space>

  <!-- 导入结果弹窗 -->
  <a-modal
    v-model:open="resultVisible"
    title="导入结果"
    :footer="null"
    :mask-closable="true"
    :width="600"
    @cancel="handleResultClose"
  >
    <a-result
      :status="importResult.success && importResult.failCount === 0 ? 'success' : 'warning'"
      :title="importResult.success && importResult.failCount === 0 ? '导入成功' : '部分导入失败'"
    >
      <template #subTitle>
        <div>
          成功：{{ importResult.successCount }} 行；失败：{{ importResult.failCount }} 行；总计：{{
            importResult.totalCount
          }}
          行
        </div>
        <div v-if="importResult.duration" style="margin-top: 8px; color: #999">
          耗时：{{ importResult.duration }}ms
        </div>
      </template>
      <template #extra>
        <a-button type="primary" @click="handleResultClose">关闭</a-button>
      </template>
      <div
        v-if="importResult.errors && importResult.errors.length > 0"
        style="margin-top: 16px"
      >
        <a-divider>错误详情（最多展示 100 条）</a-divider>
        <a-list
          size="small"
          bordered
          :data-source="importResult.errors.slice(0, 100)"
          :pagination="{ pageSize: 10, size: 'small', hideOnSinglePage: true }"
        >
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta>
                <template #title>第 {{ item.row }} 行<span v-if="item.field"> / {{ item.field }}</span></template>
                <template #description>
                  <span>{{ item.message }}</span>
                  <span v-if="item.value !== undefined" style="color: #999; margin-left: 8px">
                    （值：{{ item.value }}）
                  </span>
                </template>
              </a-list-item-meta>
            </a-list-item>
          </template>
        </a-list>
      </div>
    </a-result>
  </a-modal>
</template>

<style lang="less" scoped>
/* 暂无额外样式 */
</style>
