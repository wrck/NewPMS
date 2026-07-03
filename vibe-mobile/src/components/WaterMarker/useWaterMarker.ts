/**
 * 图片水印工具（设计文档 1.10 图片处理）
 * - Canvas 添加「时间 + GPS 坐标 + 上传人」水印
 * - 自动压缩质量 85%，长边 ≤ 2048px
 * - 生成缩略图
 *
 * 用法：
 *   const { process } = useWaterMarker()
 *   const { blob, thumbnail } = await process(file, { uploader: '张三' })
 */
import { compressImage, drawWatermark, makeThumbnail } from '@/utils/image'
import dayjs from 'dayjs'
import { getCurrentLocation } from '@/utils/gps'

export interface WaterMarkInput {
  /** 拍照时间（不传则取当前） */
  time?: string
  /** GPS 文本（不传则自动定位） */
  location?: string
  /** 上传人 */
  uploader?: string
}

export interface WaterMarkOutput {
  /** 加水印+压缩后的 Blob */
  blob: Blob
  /** 缩略图 base64 */
  thumbnail: string
  /** 实际使用的时间 */
  time: string
  /** 实际使用的 GPS 文本 */
  location?: string
}

/** 默认压缩参数：长边 2048，质量 0.85 */
const COMPRESS_OPT = { maxWidth: 2048, maxHeight: 2048, quality: 0.85, mimeType: 'image/jpeg' }

export function useWaterMarker() {
  /** 对一张图片执行：压缩 → 水印 → 缩略图 */
  async function process(
    file: File | Blob,
    input: WaterMarkInput = {}
  ): Promise<WaterMarkOutput> {
    const time = input.time || dayjs().format('YYYY-MM-DD HH:mm:ss')

    let location = input.location
    if (location === undefined) {
      try {
        const loc = await getCurrentLocation()
        location = `${loc.longitude.toFixed(6)},${loc.latitude.toFixed(6)}`
      } catch {
        location = ''
      }
    }

    const compressed = await compressImage(file, COMPRESS_OPT)
    const marked = await drawWatermark(compressed, {
      time,
      location: location || undefined,
      uploader: input.uploader || undefined
    })
    const thumbnail = await makeThumbnail(marked, 200)

    return { blob: marked, thumbnail, time, location }
  }

  /** 仅压缩（不打水印） */
  async function compressOnly(file: File | Blob): Promise<Blob> {
    return compressImage(file, COMPRESS_OPT)
  }

  return { process, compressOnly }
}
