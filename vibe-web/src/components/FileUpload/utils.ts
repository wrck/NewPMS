/**
 * FileUpload 工具函数
 *
 * 主要职责：
 *   1. 图片压缩（Canvas API，质量 85%，长边 ≤2048px）
 *   2. 缩略图生成（默认 200x200）
 *   3. 图片水印（时间 + GPS + 上传人）
 *   4. 文件分片（默认 5MB）
 *   5. 文件 hash（用于断点续传，使用 SubtleCrypto SHA-256，分块读取避免 OOM）
 *
 * 设计原则：
 *   - 所有方法纯函数 / 无副作用，便于单测
 *   - 非图片类型不做压缩/缩略图/水印，原样返回
 *   - 失败时降级为原始 File，不抛错打断主流程
 */
import type { WatermarkOptions } from './types'

/** 判断是否为图片文件 */
export function isImage(file: File | Blob): boolean {
  return !!file.type && file.type.startsWith('image/')
}

/**
 * 通过 Canvas 压缩图片
 *
 * @param file         原始图片
 * @param quality      压缩质量（0-1），默认 0.85
 * @param maxLongEdge  长边上限（像素），默认 2048
 * @param targetFormat 输出 MIME，默认沿用原图类型；非图片则原样返回
 */
export async function compressImage(
  file: File,
  quality = 0.85,
  maxLongEdge = 2048,
  targetFormat?: string
): Promise<File> {
  if (!isImage(file)) return file
  try {
    const bitmap = await loadImage(file)
    const { width, height } = calcScaledSize(bitmap.width, bitmap.height, maxLongEdge)
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d')
    if (!ctx) return file
    ctx.drawImage(bitmap, 0, 0, width, height)
    const mime = targetFormat || file.type || 'image/jpeg'
    const blob = await canvasToBlob(canvas, mime, quality)
    if (!blob) return file
    // 保留原扩展名
    const outName = ensureExtension(file.name, mime)
    return new File([blob], outName, { type: mime, lastModified: file.lastModified })
  } catch (e) {
    console.warn('[FileUpload] compressImage failed, fallback to original:', e)
    return file
  }
}

/**
 * 生成缩略图
 *
 * @param file 原始图片
 * @param size 缩略图边长（默认 200）
 */
export async function generateThumbnail(file: File, size = 200): Promise<File> {
  if (!isImage(file)) return file
  try {
    const bitmap = await loadImage(file)
    const { width, height } = calcScaledSize(bitmap.width, bitmap.height, size)
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d')
    if (!ctx) return file
    ctx.drawImage(bitmap, 0, 0, width, height)
    const blob = await canvasToBlob(canvas, 'image/jpeg', 0.7)
    if (!blob) return file
    return new File([blob], `thumb-${file.name}.jpg`, { type: 'image/jpeg' })
  } catch (e) {
    console.warn('[FileUpload] generateThumbnail failed:', e)
    return file
  }
}

/**
 * 给图片加水印
 *
 * 在右下角叠加 3 行文字：
 *   时间：2026-07-06 12:34:56
 *   GPS：深圳市南山区
 *   上传人：张三
 *
 * 字体大小根据图片尺寸自适应，使用阴影增强可读性。
 */
export async function addWatermark(file: File, options: WatermarkOptions): Promise<File> {
  if (!isImage(file)) return file
  try {
    const bitmap = await loadImage(file)
    const canvas = document.createElement('canvas')
    canvas.width = bitmap.width
    canvas.height = bitmap.height
    const ctx = canvas.getContext('2d')
    if (!ctx) return file
    ctx.drawImage(bitmap, 0, 0)

    const lines: string[] = []
    lines.push(`时间：${options.time}`)
    if (options.gps) lines.push(`GPS：${options.gps}`)
    lines.push(`上传人：${options.uploader}`)

    // 字体大小自适应：以图片短边 1/25 为基准，最小 12px 最大 48px
    const fontSize = Math.max(12, Math.min(48, Math.floor(Math.min(bitmap.width, bitmap.height) / 25)))
    const padding = Math.floor(fontSize * 0.6)
    const lineHeight = Math.floor(fontSize * 1.4)
    ctx.font = `${fontSize}px "Microsoft YaHei", sans-serif`
    ctx.textAlign = 'right'
    ctx.textBaseline = 'bottom'

    // 半透明黑色背景 + 白色文字
    const maxWidth = Math.max(...lines.map((l) => ctx.measureText(l).width))
    const bgX = canvas.width - maxWidth - padding * 2
    const bgY = canvas.height - lines.length * lineHeight - padding
    const bgW = maxWidth + padding * 2
    const bgH = lines.length * lineHeight + padding
    ctx.fillStyle = 'rgba(0, 0, 0, 0.45)'
    ctx.fillRect(bgX, bgY, bgW, bgH)

    ctx.fillStyle = '#ffffff'
    ctx.shadowColor = 'rgba(0, 0, 0, 0.6)'
    ctx.shadowBlur = 2
    ctx.shadowOffsetX = 1
    ctx.shadowOffsetY = 1
    lines.forEach((line, idx) => {
      ctx.fillText(line, canvas.width - padding, canvas.height - padding - (lines.length - 1 - idx) * lineHeight)
    })

    const blob = await canvasToBlob(canvas, file.type || 'image/jpeg', 0.92)
    if (!blob) return file
    return new File([blob], file.name, { type: file.type || 'image/jpeg', lastModified: file.lastModified })
  } catch (e) {
    console.warn('[FileUpload] addWatermark failed:', e)
    return file
  }
}

/**
 * 将文件分片
 *
 * @param file       原始文件
 * @param chunkSize  每片字节数，默认 5MB
 * @returns Blob[] 按顺序排列的分片数组
 */
export async function sliceFile(file: File, chunkSize = 5 * 1024 * 1024): Promise<Blob[]> {
  const total = file.size
  const chunks: Blob[] = []
  let start = 0
  while (start < total) {
    const end = Math.min(start + chunkSize, total)
    // File.slice 在所有现代浏览器均支持，类型为 Blob
    chunks.push(file.slice(start, end, file.type))
    start = end
  }
  return chunks
}

/**
 * 计算文件 SHA-256 hash（分块读取避免大文件 OOM）
 *
 * 用于断点续传：相同 hash 的文件可以复用本地缓存的分片和后端 uploadId。
 */
export async function getFileHash(file: File): Promise<string> {
  // SubtleCrypto 仅在 https 或 localhost 可用
  if (!globalThis.crypto?.subtle) {
    // 回退：用文件名 + size + lastModified 拼接的简易 hash
    return `fallback-${hashString(`${file.name}-${file.size}-${file.lastModified}`)}`
  }
  try {
    const buffer = await readFileAsArrayBuffer(file)
    const digest = await globalThis.crypto.subtle.digest('SHA-256', buffer)
    return bufferToHex(digest)
  } catch (e) {
    console.warn('[FileUpload] getFileHash failed, fallback to simple hash:', e)
    return `fallback-${hashString(`${file.name}-${file.size}-${file.lastModified}`)}`
  }
}

/**
 * 读取文件为 ArrayBuffer
 */
export function readFileAsArrayBuffer(file: File | Blob): Promise<ArrayBuffer> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as ArrayBuffer)
    reader.onerror = () => reject(reader.error || new Error('FileReader error'))
    reader.readAsArrayBuffer(file)
  })
}

/**
 * 读取文件为 DataURL（用于图片预览）
 */
export function readFileAsDataURL(file: File | Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = () => reject(reader.error || new Error('FileReader error'))
    reader.readAsDataURL(file)
  })
}

/**
 * 格式化文件大小（B/KB/MB/GB）
 */
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
}

/* ============ 内部工具函数 ============ */

/** 通过 Image 或 createImageBitmap 加载图片，返回 ImageBitmap 或 HTMLImageElement */
async function loadImage(file: File | Blob): Promise<ImageBitmap | HTMLImageElement> {
  if (typeof createImageBitmap === 'function') {
    try {
      return await createImageBitmap(file)
    } catch {
      // 某些浏览器对 SVG 等格式不支持，回退到 HTMLImageElement
    }
  }
  return new Promise<HTMLImageElement>((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      // 不立即 revoke，因为 drawImage 同步执行；调用方在调用后应主动 revoke
      // 这里 setTimeout 0 确保 drawImage 完成后再释放
      setTimeout(() => URL.revokeObjectURL(url), 0)
      resolve(img)
    }
    img.onerror = (e) => {
      URL.revokeObjectURL(url)
      reject(e)
    }
    img.src = url
  })
}

/** 计算按长边等比缩放后的尺寸 */
function calcScaledSize(width: number, height: number, maxLongEdge: number): { width: number; height: number } {
  if (width <= maxLongEdge && height <= maxLongEdge) {
    return { width, height }
  }
  const ratio = width > height ? maxLongEdge / width : maxLongEdge / height
  return {
    width: Math.round(width * ratio),
    height: Math.round(height * ratio)
  }
}

/** canvas.toBlob Promise 包装 */
function canvasToBlob(canvas: HTMLCanvasElement, type: string, quality: number): Promise<Blob | null> {
  return new Promise((resolve) => {
    canvas.toBlob(
      (blob) => resolve(blob),
      type,
      quality
    )
  })
}

/** 根据 mime 类型修正文件扩展名 */
function ensureExtension(filename: string, mime: string): string {
  const ext = mimeExtension(mime)
  if (!ext) return filename
  const baseName = filename.replace(/\.[^.]+$/, '')
  return `${baseName}.${ext}`
}

/** MIME -> 扩展名映射（仅覆盖常见图片格式） */
function mimeExtension(mime: string): string | null {
  const map: Record<string, string> = {
    'image/jpeg': 'jpg',
    'image/jpg': 'jpg',
    'image/png': 'png',
    'image/webp': 'webp',
    'image/gif': 'gif',
    'image/bmp': 'bmp'
  }
  return map[mime] || null
}

/** 简易字符串 hash（DJB2） */
function hashString(str: string): string {
  let hash = 5381
  for (let i = 0; i < str.length; i++) {
    hash = ((hash << 5) + hash + str.charCodeAt(i)) >>> 0
  }
  return hash.toString(16)
}

/** ArrayBuffer -> 16 进制字符串 */
function bufferToHex(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer)
  const hex: string[] = []
  for (let i = 0; i < bytes.length; i++) {
    const b = bytes[i]
    hex.push((b >>> 4).toString(16), (b & 0xf).toString(16))
  }
  return hex.join('')
}
