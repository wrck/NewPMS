/**
 * FileUpload 组件单元测试（spec 阶段三 Task 14 - SubTask 14.10）
 *
 * 覆盖范围：
 *   - 工具函数：图片压缩、缩略图、水印、文件分片、文件 hash
 *   - 组件渲染：picture-card / picture / text 三种模式
 *   - props 传递与默认值
 *   - 校验逻辑：文件大小 / 类型
 *   - 上传流程：小文件直传（mock presign）、大文件分片（mock multipart）
 *   - 错误处理
 *   - expose 方法
 *
 * 注：
 *   - jsdom 不支持 createImageBitmap，通过 setup.ts 中置为 undefined，
 *     工具函数会回退到 HTMLImageElement 加载路径
 *   - jsdom 也不支持真实的 Canvas drawImage（Canvas API 存在但绘制为空），
 *     因此压缩/缩略图/水印测试主要验证返回 File 的类型与不抛错
 *   - Mock axios via vi.mock('@/utils/request')
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'

/* ============ Mock ant-design-vue message ============ */
const messageMocks = vi.hoisted(() => ({
  success: vi.fn(),
  error: vi.fn(),
  warning: vi.fn(),
  info: vi.fn()
}))

vi.mock('ant-design-vue', async () => {
  const actual = await vi.importActual<typeof import('ant-design-vue')>('ant-design-vue')
  return {
    ...actual,
    message: {
      success: messageMocks.success,
      error: messageMocks.error,
      warning: messageMocks.warning,
      info: messageMocks.info,
      loading: vi.fn()
    }
  }
})

/* ============ Mock @/utils/request ============ */
const httpMocks = vi.hoisted(() => ({
  post: vi.fn(),
  delete: vi.fn(),
  get: vi.fn(),
  put: vi.fn()
}))

vi.mock('@/utils/request', () => ({
  http: {
    post: httpMocks.post,
    delete: httpMocks.delete,
    get: httpMocks.get,
    put: httpMocks.put
  },
  default: {
    request: vi.fn()
  },
  request: vi.fn()
}))

/* ============ Mock @/stores/user ============ */
const userStoreMocks = vi.hoisted(() => ({
  realName: '张三',
  username: 'zhangsan',
  hasPermission: vi.fn(() => true)
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    realName: { value: userStoreMocks.realName },
    username: { value: userStoreMocks.username },
    hasPermission: userStoreMocks.hasPermission
  })
}))

/* ============ Mock FileUpload/api 中的 putToMinio（用 XHR，jsdom 不支持真实 PUT） ============ */
const putToMinioMock = vi.hoisted(() => vi.fn())
vi.mock('../api', async () => {
  const actual = await vi.importActual<typeof import('../api')>('../api')
  return {
    ...actual,
    putToMinio: putToMinioMock,
    // 保留真实函数签名，但用 mock
    getPresign: actual.getPresign,
    getMultipartPresign: actual.getMultipartPresign,
    completeMultipartUpload: actual.completeMultipartUpload
  }
})

/* ============ Mock offline-cache ============ */
const offlineCacheMocks = vi.hoisted(() => ({
  saveChunk: vi.fn(),
  getChunks: vi.fn(() => Promise.resolve({})),
  clearResume: vi.fn(),
  saveUploadId: vi.fn(),
  getUploadId: vi.fn(() => Promise.resolve(null)),
  isIndexedDBAvailable: vi.fn(() => Promise.resolve(true))
}))

vi.mock('../offline-cache', () => offlineCacheMocks)

/* ============ 引入被测组件（在 vi.mock 之后） ============ */
import FileUpload from '../index.vue'
import {
  compressImage,
  generateThumbnail,
  addWatermark,
  sliceFile,
  getFileHash,
  isImage,
  formatFileSize,
  readFileAsDataURL
} from '../utils'
import { getPresign, getMultipartPresign, completeMultipartUpload } from '../api'

/* ============ 测试辅助 ============ */
function makeWrapper(options: any = {}) {
  return mount(FileUpload, {
    props: {
      modelValue: options.modelValue ?? (options.multiple ? [] : ''),
      ...options.props
    },
    global: {
      stubs: options.stubs || {}
    }
  })
}

/** 创建一个真实可用的图片 File（1x1 PNG） */
function makeImageFile(name = 'test.png'): File {
  // 1x1 透明 PNG
  const base64 =
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M8AAAMBAQDJ/pLvAAAAAElFTkSuQmCC'
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
  return new File([bytes], name, { type: 'image/png' })
}

function makeTextFile(name = 'test.txt', size = 1024): File {
  const content = 'x'.repeat(size)
  return new File([content], name, { type: 'text/plain' })
}

/* ============ 开始测试 ============ */
describe('FileUpload - 工具函数', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('isImage', () => {
    it('图片类型返回 true', () => {
      const file = makeImageFile('a.png')
      expect(isImage(file)).toBe(true)
    })

    it('非图片类型返回 false', () => {
      const file = makeTextFile('a.txt')
      expect(isImage(file)).toBe(false)
    })

    it('无 type 的 Blob 返回 false', () => {
      const blob = new Blob(['x'])
      expect(isImage(blob)).toBe(false)
    })
  })

  describe('formatFileSize', () => {
    it('B 级别直接返回 B', () => {
      expect(formatFileSize(500)).toBe('500 B')
    })
    it('KB 级别', () => {
      expect(formatFileSize(1024 * 2)).toBe('2.0 KB')
    })
    it('MB 级别', () => {
      expect(formatFileSize(1024 * 1024 * 5)).toBe('5.0 MB')
    })
    it('GB 级别', () => {
      expect(formatFileSize(1024 * 1024 * 1024 * 2)).toBe('2.00 GB')
    })
  })

  describe('sliceFile', () => {
    it('按指定大小分片', async () => {
      const file = makeTextFile('big.txt', 15 * 1024 * 1024) // 15MB
      const chunks = await sliceFile(file, 5 * 1024 * 1024) // 5MB
      expect(chunks.length).toBe(3)
      expect(chunks[0].size).toBe(5 * 1024 * 1024)
      expect(chunks[2].size).toBe(5 * 1024 * 1024)
    })

    it('文件大小恰好等于分片大小，返回 1 片', async () => {
      const file = makeTextFile('exact.txt', 5 * 1024 * 1024)
      const chunks = await sliceFile(file, 5 * 1024 * 1024)
      expect(chunks.length).toBe(1)
    })

    it('空文件返回空数组', async () => {
      const file = new File([], 'empty.txt', { type: 'text/plain' })
      const chunks = await sliceFile(file, 5 * 1024 * 1024)
      expect(chunks.length).toBe(0)
    })

    it('文件小于分片大小，返回 1 片', async () => {
      const file = makeTextFile('small.txt', 100)
      const chunks = await sliceFile(file, 5 * 1024 * 1024)
      expect(chunks.length).toBe(1)
      expect(chunks[0].size).toBe(100)
    })
  })

  describe('getFileHash', () => {
    it('返回字符串（含 crypto.subtle 时为 hex）', async () => {
      const file = makeTextFile('a.txt', 100)
      const hash = await getFileHash(file)
      expect(typeof hash).toBe('string')
      expect(hash.length).toBeGreaterThan(0)
    })

    it('相同内容返回相同 hash', async () => {
      const file1 = makeTextFile('a.txt', 100)
      const file2 = makeTextFile('a.txt', 100)
      const hash1 = await getFileHash(file1)
      const hash2 = await getFileHash(file2)
      expect(hash1).toBe(hash2)
    })

    it('不同内容返回不同 hash', async () => {
      const file1 = makeTextFile('a.txt', 100)
      const file2 = makeTextFile('a.txt', 200)
      const hash1 = await getFileHash(file1)
      const hash2 = await getFileHash(file2)
      expect(hash1).not.toBe(hash2)
    })
  })

  describe('compressImage', () => {
    it('非图片文件原样返回', async () => {
      const file = makeTextFile('a.txt', 100)
      const result = await compressImage(file, 0.85, 2048)
      expect(result).toBe(file)
    })

    it('图片文件返回 File 类型（不抛错）', async () => {
      const file = makeImageFile('test.png')
      const result = await compressImage(file, 0.85, 2048)
      expect(result).toBeInstanceOf(File)
    })

    it('图片文件压缩后保留文件名', async () => {
      const file = makeImageFile('my-photo.png')
      const result = await compressImage(file, 0.85, 2048)
      expect(result.name).toBeTruthy()
    })
  })

  describe('generateThumbnail', () => {
    it('非图片文件原样返回', async () => {
      const file = makeTextFile('a.txt', 100)
      const result = await generateThumbnail(file, 200)
      expect(result).toBe(file)
    })

    it('图片文件返回 File 类型', async () => {
      const file = makeImageFile('test.png')
      const result = await generateThumbnail(file, 200)
      expect(result).toBeInstanceOf(File)
    })
  })

  describe('addWatermark', () => {
    it('非图片文件原样返回', async () => {
      const file = makeTextFile('a.txt', 100)
      const result = await addWatermark(file, {
        time: '2026-07-06 12:00:00',
        uploader: '张三'
      })
      expect(result).toBe(file)
    })

    it('图片文件返回 File 类型', async () => {
      const file = makeImageFile('test.png')
      const result = await addWatermark(file, {
        time: '2026-07-06 12:00:00',
        gps: '深圳市',
        uploader: '张三'
      })
      expect(result).toBeInstanceOf(File)
    })

    it('水印函数不抛错（即使 canvas 失败也降级为原文件）', async () => {
      const file = makeImageFile('test.png')
      await expect(
        addWatermark(file, {
          time: '2026-07-06',
          uploader: '张三'
        })
      ).resolves.toBeInstanceOf(File)
    })
  })

  describe('readFileAsDataURL', () => {
    it('读取文件为 data URL', async () => {
      const file = makeTextFile('a.txt', 5)
      const dataUrl = await readFileAsDataURL(file)
      expect(dataUrl).toMatch(/^data:text\/plain;base64,/)
    })
  })
})

describe('FileUpload - API 封装', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getPresign', () => {
    it('调用 http.post 并返回 PresignResponse', async () => {
      const mockResp = {
        uploadUrl: 'https://minio.local/upload',
        accessUrl: 'https://minio.local/access',
        expires: 3600
      }
      httpMocks.post.mockResolvedValueOnce(mockResp)
      const file = makeTextFile('a.txt', 100)
      const result = await getPresign(file, { dir: 'common' })
      expect(httpMocks.post).toHaveBeenCalledWith('/files/presign', expect.objectContaining({
        filename: 'a.txt',
        contentType: 'text/plain',
        size: 100,
        dir: 'common'
      }))
      expect(result).toEqual(mockResp)
    })
  })

  describe('getMultipartPresign', () => {
    it('调用 http.post 并返回 MultipartInitResponse', async () => {
      const mockResp = {
        uploadId: 'uid-123',
        uploadUrls: ['https://minio.local/p1', 'https://minio.local/p2']
      }
      httpMocks.post.mockResolvedValueOnce(mockResp)
      const file = makeTextFile('big.txt', 10 * 1024 * 1024)
      const result = await getMultipartPresign(file, 2, { dir: 'common' })
      expect(httpMocks.post).toHaveBeenCalledWith('/files/multipart/init', expect.objectContaining({
        filename: 'big.txt',
        partCount: 2,
        dir: 'common'
      }))
      expect(result).toEqual(mockResp)
    })
  })

  describe('completeMultipartUpload', () => {
    it('调用 http.post 完成分片上传', async () => {
      httpMocks.post.mockResolvedValueOnce(undefined)
      await completeMultipartUpload('https://access.url', 'uid-123', ['etag1', 'etag2'])
      expect(httpMocks.post).toHaveBeenCalledWith('/files/multipart/complete', expect.objectContaining({
        uploadId: 'uid-123',
        accessUrl: 'https://access.url',
        etags: ['etag1', 'etag2']
      }))
    })
  })
})

describe('FileUpload - 组件渲染与 Props', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('单文件模式默认渲染 picture-card 上传组件', async () => {
    const wrapper = makeWrapper()
    await flushPromises()
    expect(wrapper.find('.ant-upload').exists()).toBe(true)
    expect(wrapper.text()).toContain('点击上传')
  })

  it('listType=text 时渲染上传按钮', async () => {
    const wrapper = makeWrapper({
      props: { listType: 'text' }
    })
    await flushPromises()
    expect(wrapper.find('button').exists()).toBe(true)
  })

  it('disabled=true 时上传区域不可点击', async () => {
    const wrapper = makeWrapper({
      props: { disabled: true }
    })
    await flushPromises()
    // antd disabled 上传区域含 ant-upload-disabled 类
    expect(wrapper.find('.ant-upload-disabled').exists() || wrapper.html().includes('disabled')).toBe(true)
  })

  it('modelValue 为字符串时初始化 fileList（done 状态）', async () => {
    const wrapper = makeWrapper({
      modelValue: 'https://example.com/avatar.png'
    })
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.fileList.length).toBe(1)
    expect(vm.fileList[0].status).toBe('done')
    expect(vm.fileList[0].url).toBe('https://example.com/avatar.png')
    expect(vm.fileList[0].name).toBe('avatar.png')
  })

  it('modelValue 为数组时初始化 fileList（多文件）', async () => {
    const wrapper = makeWrapper({
      modelValue: ['https://example.com/a.png', 'https://example.com/b.png'],
      props: { multiple: true }
    })
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.fileList.length).toBe(2)
    expect(vm.fileList[0].url).toBe('https://example.com/a.png')
    expect(vm.fileList[1].url).toBe('https://example.com/b.png')
  })

  it('达到 maxCount 时不显示上传按钮', async () => {
    const wrapper = makeWrapper({
      modelValue: ['https://example.com/1.png', 'https://example.com/2.png'],
      props: { multiple: true, maxCount: 2 }
    })
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.fileList.length).toBe(2)
    expect(vm.showUploadButton).toBe(false)
  })
})

describe('FileUpload - 上传流程', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    putToMinioMock.mockReset()
    httpMocks.post.mockReset()
  })

  it('小文件上传：调用 presign -> putToMinio -> 同步 modelValue', async () => {
    // mock presign 返回
    httpMocks.post.mockResolvedValueOnce({
      uploadUrl: 'https://minio.local/upload',
      accessUrl: 'https://minio.local/access/photo.png',
      expires: 3600
    })
    // mock putToMinio 返回 etag
    putToMinioMock.mockImplementation((_url, _blob, _headers, onProgress) => {
      if (onProgress) onProgress(1)
      return Promise.resolve('etag-123')
    })

    const wrapper = makeWrapper({
      props: { multiple: false, listType: 'text', maxSize: 10, resume: false }
    })
    await flushPromises()
    const vm = wrapper.vm as any

    // 模拟 a-upload customRequest
    const file = makeTextFile('test.txt', 1024) // 1KB
    await vm.handleCustomRequest({ file })

    // 验证 presign 已调用
    expect(httpMocks.post).toHaveBeenCalledWith('/files/presign', expect.any(Object))
    // 验证 putToMinio 已调用
    expect(putToMinioMock).toHaveBeenCalled()
    // 验证 fileList 状态
    expect(vm.fileList.length).toBe(1)
    expect(vm.fileList[0].status).toBe('done')
    expect(vm.fileList[0].url).toBe('https://minio.local/access/photo.png')
    // 验证 success 事件
    const successEvents = wrapper.emitted('success')
    expect(successEvents).toBeTruthy()
    // 验证 update:modelValue
    const updateEvents = wrapper.emitted('update:modelValue')
    expect(updateEvents).toBeTruthy()
    const lastUpdate = updateEvents!.at(-1)![0]
    expect(lastUpdate).toBe('https://minio.local/access/photo.png')
  })

  it('大文件上传：触发分片流程', async () => {
    // mock init -> init -> complete（init 调用两次：第一次拿 uploadId，第二次重新拿 uploadUrls）
    httpMocks.post.mockResolvedValue({
      uploadId: 'uid-123',
      uploadUrls: ['https://minio.local/p1', 'https://minio.local/p2', 'https://minio.local/p3'],
      accessUrl: 'https://minio.local/access/big.bin',
      objectName: 'common/big.bin'
    })
    putToMinioMock.mockImplementation((_url, _blob, _headers, onProgress) => {
      if (onProgress) onProgress(1)
      return Promise.resolve('etag-x')
    })

    const wrapper = makeWrapper({
      props: {
        multiple: false,
        listType: 'text',
        maxSize: 100,
        multipartThreshold: 1, // 1MB 即触发分片
        chunkSize: 1, // 1MB 一片
        concurrency: 2,
        resume: false
      }
    })
    await flushPromises()
    const vm = wrapper.vm as any

    // 3MB 文件，1MB 一片 = 3 片
    const file = makeTextFile('big.bin', 3 * 1024 * 1024)
    await vm.handleCustomRequest({ file })

    // 验证 multipart/init 调用
    expect(httpMocks.post).toHaveBeenCalledWith('/files/multipart/init', expect.any(Object))
    // 验证 multipart/complete 调用
    const completeCall = httpMocks.post.mock.calls.find(
      (call: any[]) => call[0] === '/files/multipart/complete'
    )
    expect(completeCall).toBeTruthy()
    // 验证 putToMinio 调用 3 次
    expect(putToMinioMock).toHaveBeenCalledTimes(3)
    // 验证最终状态
    expect(vm.fileList[0].status).toBe('done')
    expect(vm.fileList[0].url).toBe('https://minio.local/access/big.bin')
  })

  it('上传失败时 emit error 事件', async () => {
    httpMocks.post.mockRejectedValueOnce(new Error('presign failed'))
    putToMinioMock.mockReset()

    const wrapper = makeWrapper({
      props: { multiple: false, listType: 'text', resume: false }
    })
    await flushPromises()
    const vm = wrapper.vm as any

    const file = makeTextFile('err.txt', 1024)
    await vm.handleCustomRequest({ file })

    expect(vm.fileList[0].status).toBe('error')
    const errorEvents = wrapper.emitted('error')
    expect(errorEvents).toBeTruthy()
  })
})

describe('FileUpload - 校验逻辑', () => {
  it('文件超过 maxSize 时拒绝并提示', async () => {
    const wrapper = makeWrapper({
      props: { maxSize: 1, listType: 'text' }
    })
    await flushPromises()
    const vm = wrapper.vm as any
    // 5MB 文件，maxSize=1
    const bigFile = makeTextFile('big.txt', 5 * 1024 * 1024)
    const result = vm.handleBeforeUpload(bigFile)
    // handleBeforeUpload 是 async，返回 Promise
    const final = await result
    expect(final).toBe(false)
    expect(messageMocks.error).toHaveBeenCalled()
  })

  it('文件类型不匹配时拒绝', async () => {
    const wrapper = makeWrapper({
      props: { accept: '.jpg,.png', listType: 'text' }
    })
    await flushPromises()
    const vm = wrapper.vm as any
    const wrongFile = makeTextFile('doc.txt', 100)
    const result = await vm.handleBeforeUpload(wrongFile)
    expect(result).toBe(false)
    expect(messageMocks.error).toHaveBeenCalled()
  })

  it('accept 为 image/* 时接受图片类型', async () => {
    const wrapper = makeWrapper({
      props: { accept: 'image/*', listType: 'text' }
    })
    await flushPromises()
    const vm = wrapper.vm as any
    const imgFile = makeImageFile('photo.png')
    // validateFile 是内部函数，通过 handleBeforeUpload 间接测试
    // handleBeforeUpload 对图片会调用 compressImage，需 await
    const result = await vm.handleBeforeUpload(imgFile)
    expect(result).not.toBe(false)
  })

  it('accept 为 .pdf 时通过扩展名匹配', async () => {
    const wrapper = makeWrapper({
      props: { accept: '.pdf', listType: 'text' }
    })
    await flushPromises()
    const vm = wrapper.vm as any
    const pdfFile = new File(['x'], 'doc.pdf', { type: 'application/pdf' })
    const result = await vm.handleBeforeUpload(pdfFile)
    expect(result).not.toBe(false)
  })
})

describe('FileUpload - Expose 方法', () => {
  it('getFileList 返回当前 fileList', async () => {
    const wrapper = makeWrapper({
      modelValue: 'https://example.com/a.png'
    })
    await flushPromises()
    const vm = wrapper.vm as any
    const list = vm.getFileList()
    expect(list.length).toBe(1)
    expect(list[0].url).toBe('https://example.com/a.png')
  })

  it('clear 清空文件列表', async () => {
    const wrapper = makeWrapper({
      modelValue: ['https://example.com/a.png', 'https://example.com/b.png'],
      props: { multiple: true }
    })
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.fileList.length).toBe(2)
    vm.clear()
    expect(vm.fileList.length).toBe(0)
    // 应 emit update:modelValue
    const updates = wrapper.emitted('update:modelValue')
    expect(updates).toBeTruthy()
    const last = updates!.at(-1)![0]
    expect(last).toEqual([])
  })

  it('retryAll 对 error 状态文件重试', async () => {
    const wrapper = makeWrapper({
      props: { multiple: true, listType: 'text', resume: false }
    })
    await flushPromises()
    const vm = wrapper.vm as any

    // 手动塞入一个 error 状态的 fileItem
    vm.fileList.push({
      uid: 'err-1',
      name: 'failed.txt',
      status: 'error',
      rawFile: makeTextFile('failed.txt', 100),
      percent: 0
    })

    // mock presign 成功
    httpMocks.post.mockResolvedValueOnce({
      uploadUrl: 'https://minio.local/u',
      accessUrl: 'https://minio.local/a',
      expires: 3600
    })
    putToMinioMock.mockImplementation((_u, _b, _h, onProgress) => {
      if (onProgress) onProgress(1)
      return Promise.resolve('etag')
    })

    await vm.retryAll()
    expect(vm.fileList[0].status).toBe('done')
  })
})

describe('FileUpload - 双向绑定', () => {
  it('modelValue 变化时同步新 URL 到 fileList', async () => {
    const wrapper = makeWrapper({
      modelValue: ''
    })
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.fileList.length).toBe(0)
    await wrapper.setProps({ modelValue: 'https://example.com/new.png' })
    await nextTick()
    expect(vm.fileList.length).toBe(1)
    expect(vm.fileList[0].url).toBe('https://example.com/new.png')
    expect(vm.fileList[0].status).toBe('done')
  })

  it('modelValue 移除某 URL 时从 fileList 中移除', async () => {
    const wrapper = makeWrapper({
      modelValue: ['https://example.com/a.png', 'https://example.com/b.png'],
      props: { multiple: true }
    })
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.fileList.length).toBe(2)
    await wrapper.setProps({ modelValue: ['https://example.com/a.png'] })
    await nextTick()
    expect(vm.fileList.length).toBe(1)
    expect(vm.fileList[0].url).toBe('https://example.com/a.png')
  })
})

describe('FileUpload - 预览 / 删除', () => {
  it('handlePreview 图片时打开预览弹窗', async () => {
    const wrapper = makeWrapper({
      modelValue: 'https://example.com/photo.png'
    })
    await flushPromises()
    const vm = wrapper.vm as any
    const file = vm.fileList[0]
    vm.handlePreview(file)
    await nextTick()
    expect(vm.previewVisible).toBe(true)
    expect(vm.previewImage).toBe('https://example.com/photo.png')
  })

  it('handleRemove 移除文件并 emit remove 事件', async () => {
    const wrapper = makeWrapper({
      modelValue: 'https://example.com/a.png'
    })
    await flushPromises()
    const vm = wrapper.vm as any
    const file = vm.fileList[0]
    await vm.handleRemove(file)
    expect(vm.fileList.length).toBe(0)
    const removes = wrapper.emitted('remove')
    expect(removes).toBeTruthy()
  })
})

describe('FileUpload - 断点续传', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    offlineCacheMocks.isIndexedDBAvailable.mockResolvedValue(true)
    offlineCacheMocks.getUploadId.mockResolvedValue(null)
    offlineCacheMocks.getChunks.mockResolvedValue({})
  })

  it('启用 resume 时计算 fileHash 并保存 uploadId（大文件）', async () => {
    httpMocks.post.mockResolvedValue({
      uploadId: 'uid-resume',
      uploadUrls: ['https://minio.local/p1'],
      accessUrl: 'https://minio.local/a.bin',
      objectName: 'common/a.bin'
    })
    putToMinioMock.mockImplementation((_u, _b, _h, onProgress) => {
      if (onProgress) onProgress(1)
      return Promise.resolve('etag-r')
    })

    const wrapper = makeWrapper({
      props: {
        multiple: false,
        listType: 'text',
        multipartThreshold: 1,
        chunkSize: 5,
        resume: true
      }
    })
    await flushPromises()
    const vm = wrapper.vm as any

    const file = makeTextFile('resume.bin', 1.5 * 1024 * 1024)
    await vm.handleCustomRequest({ file })

    expect(offlineCacheMocks.saveUploadId).toHaveBeenCalled()
    expect(offlineCacheMocks.clearResume).toHaveBeenCalled()
  })
})
