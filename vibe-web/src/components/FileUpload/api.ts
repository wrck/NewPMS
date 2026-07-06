/**
 * FileUpload 后端 API 封装
 *
 * 端点约定：
 *   POST /api/v1/files/presign          - 单文件预签名
 *   POST /api/v1/files/presign/batch   - 批量预签名
 *   POST /api/v1/files/multipart/init  - 分片上传初始化
 *   POST /api/v1/files/multipart/complete - 完成分片上传
 *   DELETE /api/v1/files               - 删除对象（按 objectName）
 *
 * 后端 controller 暂未实现，组件以乐观方式调用：
 *   若接口返回 404 / 500，组件回退为单文件直传，并保留 accessUrl 等字段供父组件兜底处理。
 */
import { http } from '@/utils/request'
import type { PresignResponse, MultipartInitResponse, CompleteMultipartPayload } from './types'

/** 单文件预签名请求参数 */
export interface PresignRequest {
  /** 文件名 */
  filename: string
  /** MIME 类型 */
  contentType: string
  /** 文件大小（字节） */
  size: number
  /** Bucket 内目录 */
  dir?: string
  /** 文件 hash（用于断点续传场景的秒传判断） */
  hash?: string
}

/**
 * 获取单文件预签名上传 URL
 *
 * 调用后端 /api/v1/files/presign，得到 MinIO 预签名 PUT URL。
 * 前端拿到 uploadUrl 后直接 PUT 文件二进制到 MinIO，无需经服务端转发。
 */
export function getPresign(file: File, opts?: { dir?: string; hash?: string }): Promise<PresignResponse> {
  const body: PresignRequest = {
    filename: file.name,
    contentType: file.type || 'application/octet-stream',
    size: file.size,
    dir: opts?.dir,
    hash: opts?.hash
  }
  return http.post<PresignResponse>('/files/presign', body) as unknown as Promise<PresignResponse>
}

/**
 * 批量预签名（一次请求拿多个文件的 uploadUrl）
 */
export function getPresignBatch(
  files: File[],
  opts?: { dir?: string }
): Promise<PresignResponse[]> {
  const body: PresignRequest[] = files.map((f) => ({
    filename: f.name,
    contentType: f.type || 'application/octet-stream',
    size: f.size,
    dir: opts?.dir
  }))
  return http.post<PresignResponse[]>('/files/presign/batch', body) as unknown as Promise<PresignResponse[]>
}

/**
 * 分片上传初始化
 *
 * @param file       原始文件
 * @param partCount  分片数量
 * @param opts       附加选项
 */
export function getMultipartPresign(
  file: File,
  partCount: number,
  opts?: { dir?: string; hash?: string }
): Promise<MultipartInitResponse> {
  const body = {
    filename: file.name,
    contentType: file.type || 'application/octet-stream',
    size: file.size,
    partCount,
    dir: opts?.dir,
    hash: opts?.hash
  }
  return http.post<MultipartInitResponse>(
    '/files/multipart/init',
    body
  ) as unknown as Promise<MultipartInitResponse>
}

/**
 * 完成分片上传（合并所有 part）
 *
 * 调用后端 /api/v1/files/multipart/complete，后端向 MinIO 发起 CompleteMultipartUpload。
 */
export function completeMultipartUpload(
  accessUrl: string,
  uploadId: string,
  etags: string[],
  objectName?: string
): Promise<void> {
  const body: CompleteMultipartPayload = {
    uploadId,
    accessUrl,
    objectName,
    etags
  }
  return http.post<void>('/files/multipart/complete', body) as unknown as Promise<void>
}

/**
 * 删除已上传的文件
 *
 * @param objectName MinIO 对象路径
 */
export function deleteFile(objectName: string): Promise<void> {
  return http.delete<void>('/files', { objectName }) as unknown as Promise<void>
}

/**
 * 直接 PUT 文件二进制到 MinIO 预签名 URL
 *
 * 注意：axios 默认 Content-Type 为 application/json，PUT 二进制时必须显式设置
 *      Content-Type 与 presign 阶段一致，否则 MinIO 会签名不匹配报 403。
 *
 * @param uploadUrl 预签名 PUT URL
 * @param blob     文件二进制（File / Blob）
 * @param headers  预签名响应里要求附带的 headers
 * @param onProgress 进度回调（参数为 0-1）
 */
export function putToMinio(
  uploadUrl: string,
  blob: Blob,
  headers?: Record<string, string>,
  onProgress?: (ratio: number) => void
): Promise<string> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('PUT', uploadUrl, true)
    const finalHeaders: Record<string, string> = {
      'Content-Type': blob.type || 'application/octet-stream',
      ...(headers || {})
    }
    for (const [k, v] of Object.entries(finalHeaders)) {
      xhr.setRequestHeader(k, v)
    }
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable && onProgress) {
        onProgress(e.loaded / e.total)
      }
    }
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        // ETag 在响应头，用于分片上传 complete
        const etag = xhr.getResponseHeader('ETag') || ''
        resolve(etag)
      } else {
        reject(new Error(`MinIO PUT failed: ${xhr.status} ${xhr.statusText}`))
      }
    }
    xhr.onerror = () => reject(new Error('MinIO PUT network error'))
    xhr.ontimeout = () => reject(new Error('MinIO PUT timeout'))
    xhr.timeout = 5 * 60 * 1000 // 5 分钟，避免大文件分片超时
    xhr.send(blob)
  })
}

export default {
  getPresign,
  getPresignBatch,
  getMultipartPresign,
  completeMultipartUpload,
  deleteFile,
  putToMinio
}
