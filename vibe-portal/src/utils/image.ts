/**
 * 图片处理工具 - 外部入口 H5
 * 主要用于 PhotoUpload 组件的客户端压缩与水印叠加
 * 水印用于施工拍照取证：自动叠加拍摄时间与定位信息，防止图片被替换
 */

/** 压缩选项 */
export interface CompressOptions {
  /** 最大宽/高，按比例缩放 */
  maxSize?: number
  /** 质量 0-1 */
  quality?: number
  /** 水印配置，传入则绘制水印 */
  watermark?: WatermarkOptions
}

/** 水印配置 */
export interface WatermarkOptions {
  /** 水印文字内容（多行用 \n 分隔），未传则使用默认时间戳 */
  text?: string
  /** 附加定位信息 */
  location?: string
  /** 字体大小（px），默认按图片宽度自适应 */
  fontSize?: number
  /** 字体颜色，默认 rgba(255,255,255,0.9) */
  color?: string
  /** 是否绘制半透明底框，默认 true */
  drawBackground?: boolean
}

/**
 * 压缩图片文件（可选叠加水印）
 * @param file 原始 File
 * @param options 压缩选项
 */
export function compressImage(file: File, options: CompressOptions = {}): Promise<File> {
  const { maxSize = 1280, quality = 0.7, watermark } = options

  // 非图片或 GIF 直接返回
  if (!file.type.startsWith('image/') || file.type === 'image/gif') {
    return Promise.resolve(file)
  }

  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        let { width, height } = img
        if (width > maxSize || height > maxSize) {
          if (width >= height) {
            height = Math.round((height * maxSize) / width)
            width = maxSize
          } else {
            width = Math.round((width * maxSize) / height)
            height = maxSize
          }
        }
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        if (!ctx) {
          resolve(file)
          return
        }
        ctx.drawImage(img, 0, 0, width, height)

        // 叠加水印
        if (watermark) {
          drawWatermark(ctx, width, height, watermark)
        }

        canvas.toBlob(
          (blob) => {
            if (!blob) {
              resolve(file)
              return
            }
            const compressed = new File([blob], file.name.replace(/\.(jpe?g|png)$/i, '.jpg'), {
              type: 'image/jpeg',
              lastModified: Date.now()
            })
            resolve(compressed)
          },
          'image/jpeg',
          quality
        )
      }
      img.onerror = () => resolve(file)
      img.src = e.target?.result as string
    }
    reader.onerror = () => reject(new Error('读取图片失败'))
    reader.readAsDataURL(file)
  })
}

/**
 * 在 canvas 上绘制水印（右下角，含时间戳与定位）
 */
function drawWatermark(
  ctx: CanvasRenderingContext2D,
  width: number,
  height: number,
  options: WatermarkOptions
): void {
  const fontSize = options.fontSize ?? Math.max(12, Math.round(width / 32))
  const color = options.color ?? 'rgba(255,255,255,0.9)'
  const pad = Math.max(8, fontSize * 0.6)

  // 拼装水印文本：默认为拍摄时间，可选叠加定位
  const lines: string[] = []
  if (options.text) {
    lines.push(...options.text.split('\n'))
  } else {
    const now = new Date()
    const ts = `${now.getFullYear()}-${pad2(now.getMonth() + 1)}-${pad2(now.getDate())} ${pad2(
      now.getHours()
    )}:${pad2(now.getMinutes())}`
    lines.push(ts)
  }
  if (options.location) {
    lines.push(options.location)
  }

  ctx.save()
  ctx.font = `${fontSize}px -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif`
  ctx.textBaseline = 'top'

  // 计算文本尺寸以绘制半透明底框
  let maxWidth = 0
  for (const line of lines) {
    const w = ctx.measureText(line).width
    if (w > maxWidth) maxWidth = w
  }
  const lineHeight = Math.round(fontSize * 1.3)
  const boxWidth = maxWidth + pad * 2
  const boxHeight = lines.length * lineHeight + pad * 2
  const boxX = width - boxWidth - pad
  const boxY = height - boxHeight - pad

  if (options.drawBackground !== false) {
    ctx.fillStyle = 'rgba(0,0,0,0.4)'
    roundRect(ctx, boxX, boxY, boxWidth, boxHeight, Math.max(2, fontSize / 4))
    ctx.fill()
  }

  ctx.fillStyle = color
  lines.forEach((line, i) => {
    ctx.fillText(line, boxX + pad, boxY + pad + i * lineHeight)
  })
  ctx.restore()
}

/** 两位补零 */
function pad2(n: number): string {
  return n < 10 ? `0${n}` : String(n)
}

/** 圆角矩形路径 */
function roundRect(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  w: number,
  h: number,
  r: number
): void {
  const radius = Math.min(r, w / 2, h / 2)
  ctx.beginPath()
  ctx.moveTo(x + radius, y)
  ctx.arcTo(x + w, y, x + w, y + h, radius)
  ctx.arcTo(x + w, y + h, x, y + h, radius)
  ctx.arcTo(x, y + h, x, y, radius)
  ctx.arcTo(x, y, x + w, y, radius)
  ctx.closePath()
}

/** 生成文件本地预览 URL */
export function createObjectURL(file: File): string {
  return URL.createObjectURL(file)
}

/** 生成简易唯一ID */
export function genLocalId(): string {
  return `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}
