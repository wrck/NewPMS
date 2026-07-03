/**
 * 图片处理工具：压缩、缩略图、Canvas 水印
 */

/** 图片压缩配置 */
export interface CompressOptions {
  /** 最大宽度 */
  maxWidth?: number
  /** 最大高度 */
  maxHeight?: number
  /** 质量 0-1 */
  quality?: number
  /** 输出 MIME 类型 */
  mimeType?: string
}

/**
 * 压缩图片文件
 * @param file 原始 File/Blob
 * @param options 压缩选项
 */
export function compressImage(
  file: File | Blob,
  options: CompressOptions = {}
): Promise<Blob> {
  const { maxWidth = 1280, maxHeight = 1280, quality = 0.75, mimeType = 'image/jpeg' } = options
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        let { width, height } = img
        if (width > maxWidth) {
          height = (height * maxWidth) / width
          width = maxWidth
        }
        if (height > maxHeight) {
          width = (width * maxHeight) / height
          height = maxHeight
        }
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        if (!ctx) {
          reject(new Error('Canvas 2D context unavailable'))
          return
        }
        ctx.drawImage(img, 0, 0, width, height)
        canvas.toBlob(
          (blob) => {
            if (blob) resolve(blob)
            else reject(new Error('Canvas toBlob failed'))
          },
          mimeType,
          quality
        )
      }
      img.onerror = () => reject(new Error('Image load failed'))
      img.src = e.target?.result as string
    }
    reader.onerror = () => reject(new Error('FileReader failed'))
    reader.readAsDataURL(file)
  })
}

/**
 * 生成缩略图 base64
 * @param file 文件
 * @param size 最大边长
 */
export function makeThumbnail(file: File | Blob, size = 200): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        let { width, height } = img
        if (width > height) {
          height = (height * size) / width
          width = size
        } else {
          width = (width * size) / height
          height = size
        }
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        if (!ctx) {
          reject(new Error('Canvas context unavailable'))
          return
        }
        ctx.drawImage(img, 0, 0, width, height)
        resolve(canvas.toDataURL('image/jpeg', 0.6))
      }
      img.onerror = () => reject(new Error('Image load failed'))
      img.src = e.target?.result as string
    }
    reader.onerror = () => reject(new Error('FileReader failed'))
    reader.readAsDataURL(file)
  })
}

/**
 * 在图片上叠加水印（时间 + GPS + 上传人）并返回新 Blob
 */
export interface WatermarkOptions {
  /** 拍照时间字符串 */
  time: string
  /** GPS 经纬度文本 */
  location?: string
  /** 上传人姓名 */
  uploader?: string
  /** 水印字体 */
  fontSize?: number
  /** 水印颜色 */
  color?: string
  /** 水印背景透明度 */
  backgroundAlpha?: number
}

export function drawWatermark(file: File | Blob, options: WatermarkOptions): Promise<Blob> {
  const {
    time,
    location,
    uploader,
    fontSize = 24,
    color = '#ffffff',
    backgroundAlpha = 0.45
  } = options
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        const canvas = document.createElement('canvas')
        canvas.width = img.width
        canvas.height = img.height
        const ctx = canvas.getContext('2d')
        if (!ctx) {
          reject(new Error('Canvas context unavailable'))
          return
        }
        ctx.drawImage(img, 0, 0)

        // 水印区域：底部条带
        const padding = 16
        const lineHeight = fontSize * 1.5
        const lines: string[] = [`时间：${time}`]
        if (location) lines.push(`GPS：${location}`)
        if (uploader) lines.push(`上传人：${uploader}`)

        const blockHeight = lines.length * lineHeight + padding * 2
        const gradient = ctx.createLinearGradient(0, img.height - blockHeight, 0, img.height)
        gradient.addColorStop(0, `rgba(0,0,0,0)`)
        gradient.addColorStop(1, `rgba(0,0,0,${backgroundAlpha})`)
        ctx.fillStyle = gradient
        ctx.fillRect(0, img.height - blockHeight, img.width, blockHeight)

        ctx.font = `${fontSize}px -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif`
        ctx.fillStyle = color
        ctx.textBaseline = 'middle'
        lines.forEach((line, idx) => {
          ctx.fillText(
            line,
            padding,
            img.height - blockHeight + padding + lineHeight * idx + lineHeight / 2
          )
        })

        canvas.toBlob(
          (blob) => {
            if (blob) resolve(blob)
            else reject(new Error('Canvas toBlob failed'))
          },
          'image/jpeg',
          0.85
        )
      }
      img.onerror = () => reject(new Error('Image load failed'))
      img.src = e.target?.result as string
    }
    reader.onerror = () => reject(new Error('FileReader failed'))
    reader.readAsDataURL(file)
  })
}

/**
 * 将 Blob 转为 base64
 */
export function blobToDataURL(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(blob)
  })
}
