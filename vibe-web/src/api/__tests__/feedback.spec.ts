/**
 * 反馈与工单模块 API 单元测试（Task E1.1）
 *
 * 覆盖范围：
 *   - submitFeedback / pageFeedback / pageMyFeedback / handleFeedback 调用
 *   - URL 拼接与请求方法
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

const mocks = vi.hoisted(() => {
  return {
    httpGet: vi.fn(),
    httpPost: vi.fn(),
    httpPut: vi.fn(),
    httpDelete: vi.fn()
  }
})

vi.mock('@/utils/request', async () => {
  const actual = await vi.importActual<typeof import('@/utils/request')>('@/utils/request')
  return {
    ...actual,
    http: {
      get: mocks.httpGet,
      post: mocks.httpPost,
      put: mocks.httpPut,
      patch: vi.fn(),
      delete: mocks.httpDelete
    }
  }
})

import {
  submitFeedback,
  pageFeedback,
  pageMyFeedback,
  handleFeedback
} from '../feedback'

describe('feedback api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('submitFeedback 调用 POST /feedback', async () => {
    mocks.httpPost.mockResolvedValueOnce(1)
    await submitFeedback({
      type: 'BUG',
      title: '标题',
      content: '内容'
    })
    expect(mocks.httpPost).toHaveBeenCalledWith('/feedback', {
      type: 'BUG',
      title: '标题',
      content: '内容'
    })
  })

  it('pageFeedback 调用 GET /feedback', async () => {
    mocks.httpGet.mockResolvedValueOnce({ records: [], total: 0 })
    await pageFeedback({ page: 1, size: 10, status: 'PENDING' })
    expect(mocks.httpGet).toHaveBeenCalledWith('/feedback', { page: 1, size: 10, status: 'PENDING' })
  })

  it('pageMyFeedback 调用 GET /feedback/mine', async () => {
    mocks.httpGet.mockResolvedValueOnce({ records: [], total: 0 })
    await pageMyFeedback({ page: 1, size: 10 })
    expect(mocks.httpGet).toHaveBeenCalledWith('/feedback/mine', { page: 1, size: 10 })
  })

  it('handleFeedback 调用 PUT /feedback/{id}/handle', async () => {
    mocks.httpPut.mockResolvedValueOnce(undefined)
    await handleFeedback(7, { status: 'RESOLVED', handleNote: '已修复' })
    expect(mocks.httpPut).toHaveBeenCalledWith('/feedback/7/handle', {
      status: 'RESOLVED',
      handleNote: '已修复'
    })
  })
})
