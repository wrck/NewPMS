/**
 * 低代码模块 API 单元测试（Task E1.1）
 *
 * 覆盖范围：
 *   - Form/List/Tab/Relation/Template 五类 CRUD 接口调用
 *   - 复制 / 导出 / 导入 / 模板实例化 接口
 *   - 请求参数与 URL 拼接正确性
 *   - downloadBlob 工具函数
 *
 * Mock 策略：使用 vi.mock 替换 @/utils/request，断言调用参数。
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

/* ============ Mock @/utils/request ============ */
const mocks = vi.hoisted(() => {
  return {
    httpGet: vi.fn(),
    httpPost: vi.fn(),
    httpPut: vi.fn(),
    httpDelete: vi.fn(),
    serviceGet: vi.fn()
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
    },
    // 默认导出的 axios 实例（用于 exportXxxJson）
    default: {
      get: mocks.serviceGet,
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
      request: vi.fn()
    }
  }
})

import {
  pageFormConfigs,
  getFormConfigDetail,
  createFormConfig,
  updateFormConfig,
  deleteFormConfig,
  copyFormConfig,
  exportFormConfigJson,
  importFormConfig,
  instantiateFormFromTemplate,
  pageListConfigs,
  createListConfig,
  copyListConfig,
  pageTabConfigs,
  createTabConfig,
  pageRelationConfigs,
  createRelationConfig,
  pageTemplates,
  createTemplate,
  instantiateTemplate,
  downloadBlob
} from '../lowcode'
import type { LowcodeFormConfigDTO, LowcodeTemplateDTO } from '@/types/lowcode'

describe('lowcode api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('FormConfig 接口', () => {
    it('pageFormConfigs 调用 GET /lowcode/forms', async () => {
      mocks.httpGet.mockResolvedValueOnce({ records: [], total: 0 })
      await pageFormConfigs({ page: 1, size: 10, keyword: 'kw' })
      expect(mocks.httpGet).toHaveBeenCalledWith('/lowcode/forms', {
        page: 1,
        size: 10,
        keyword: 'kw'
      })
    })

    it('getFormConfigDetail 调用 GET /lowcode/forms/{id}', async () => {
      mocks.httpGet.mockResolvedValueOnce({ id: 5 })
      await getFormConfigDetail(5)
      expect(mocks.httpGet).toHaveBeenCalledWith('/lowcode/forms/5')
    })

    it('createFormConfig 调用 POST /lowcode/forms', async () => {
      mocks.httpPost.mockResolvedValueOnce(1)
      const dto: LowcodeFormConfigDTO = {
        configCode: 'customer_form',
        configName: '客户表单',
        schemaJson: '{}'
      }
      await createFormConfig(dto)
      expect(mocks.httpPost).toHaveBeenCalledWith('/lowcode/forms', dto)
    })

    it('updateFormConfig 调用 PUT /lowcode/forms/{id}', async () => {
      mocks.httpPut.mockResolvedValueOnce(undefined)
      const dto: LowcodeFormConfigDTO = {
        configCode: 'customer_form',
        configName: '客户表单',
        schemaJson: '{}'
      }
      await updateFormConfig(7, dto)
      expect(mocks.httpPut).toHaveBeenCalledWith('/lowcode/forms/7', dto)
    })

    it('deleteFormConfig 调用 DELETE /lowcode/forms/{id}', async () => {
      mocks.httpDelete.mockResolvedValueOnce(undefined)
      await deleteFormConfig(9)
      expect(mocks.httpDelete).toHaveBeenCalledWith('/lowcode/forms/9')
    })

    it('copyFormConfig 调用 POST /lowcode/forms/{id}/copy', async () => {
      mocks.httpPost.mockResolvedValueOnce(11)
      await copyFormConfig(11)
      expect(mocks.httpPost).toHaveBeenCalledWith('/lowcode/forms/11/copy')
    })

    it('exportFormConfigJson 调用 service.get 并返回 Blob', async () => {
      const fakeBlob = new Blob(['{}'], { type: 'application/json' })
      mocks.serviceGet.mockResolvedValueOnce(fakeBlob)
      const result = await exportFormConfigJson(13)
      expect(mocks.serviceGet).toHaveBeenCalledWith('/lowcode/forms/13/export', {
        responseType: 'blob'
      })
      expect(result).toBeInstanceOf(Blob)
    })

    it('importFormConfig 调用 POST /lowcode/forms/import', async () => {
      mocks.httpPost.mockResolvedValueOnce(21)
      const dto: LowcodeFormConfigDTO = {
        configCode: 'imported',
        configName: '导入的',
        schemaJson: '{}'
      }
      await importFormConfig(dto)
      expect(mocks.httpPost).toHaveBeenCalledWith('/lowcode/forms/import', dto)
    })

    it('instantiateFormFromTemplate 调用 POST /lowcode/forms/templates/{tid}/instantiate', async () => {
      mocks.httpPost.mockResolvedValueOnce(31)
      await instantiateFormFromTemplate(99, { configName: '实例化' })
      expect(mocks.httpPost).toHaveBeenCalledWith(
        '/lowcode/forms/templates/99/instantiate',
        { configName: '实例化' }
      )
    })
  })

  describe('ListConfig 接口', () => {
    it('pageListConfigs 调用 GET /lowcode/lists', async () => {
      mocks.httpGet.mockResolvedValueOnce({ records: [], total: 0 })
      await pageListConfigs({ page: 1, size: 20 })
      expect(mocks.httpGet).toHaveBeenCalledWith('/lowcode/lists', { page: 1, size: 20 })
    })

    it('createListConfig 调用 POST /lowcode/lists', async () => {
      mocks.httpPost.mockResolvedValueOnce(1)
      const dto = {
        configCode: 'customer_list',
        configName: '客户列表',
        schemaJson: '{}'
      }
      await createListConfig(dto)
      expect(mocks.httpPost).toHaveBeenCalledWith('/lowcode/lists', dto)
    })

    it('copyListConfig 调用 POST /lowcode/lists/{id}/copy', async () => {
      mocks.httpPost.mockResolvedValueOnce(2)
      await copyListConfig(2)
      expect(mocks.httpPost).toHaveBeenCalledWith('/lowcode/lists/2/copy')
    })
  })

  describe('TabConfig 接口', () => {
    it('pageTabConfigs 调用 GET /lowcode/tabs', async () => {
      mocks.httpGet.mockResolvedValueOnce({ records: [], total: 0 })
      await pageTabConfigs({ page: 1, size: 10 })
      expect(mocks.httpGet).toHaveBeenCalledWith('/lowcode/tabs', { page: 1, size: 10 })
    })

    it('createTabConfig 调用 POST /lowcode/tabs', async () => {
      mocks.httpPost.mockResolvedValueOnce(3)
      const dto = {
        configCode: 'customer_tab',
        configName: '客户 Tab',
        schemaJson: '{}'
      }
      await createTabConfig(dto)
      expect(mocks.httpPost).toHaveBeenCalledWith('/lowcode/tabs', dto)
    })
  })

  describe('RelationConfig 接口', () => {
    it('pageRelationConfigs 调用 GET /lowcode/relations', async () => {
      mocks.httpGet.mockResolvedValueOnce({ records: [], total: 0 })
      await pageRelationConfigs({ page: 1, size: 10 })
      expect(mocks.httpGet).toHaveBeenCalledWith('/lowcode/relations', { page: 1, size: 10 })
    })

    it('createRelationConfig 调用 POST /lowcode/relations', async () => {
      mocks.httpPost.mockResolvedValueOnce(4)
      const dto = {
        configCode: 'customer_relation',
        configName: '客户关联',
        schemaJson: '{}'
      }
      await createRelationConfig(dto)
      expect(mocks.httpPost).toHaveBeenCalledWith('/lowcode/relations', dto)
    })
  })

  describe('Template 接口', () => {
    it('pageTemplates 调用 GET /lowcode/templates', async () => {
      mocks.httpGet.mockResolvedValueOnce({ records: [], total: 0 })
      await pageTemplates({ page: 1, size: 10, templateType: 'FORM' })
      expect(mocks.httpGet).toHaveBeenCalledWith('/lowcode/templates', {
        page: 1,
        size: 10,
        templateType: 'FORM'
      })
    })

    it('createTemplate 调用 POST /lowcode/templates', async () => {
      mocks.httpPost.mockResolvedValueOnce(101)
      const dto: LowcodeTemplateDTO = {
        templateCode: 'tpl_customer',
        templateName: '客户模板',
        templateType: 'FORM',
        schemaJson: '{}'
      }
      await createTemplate(dto)
      expect(mocks.httpPost).toHaveBeenCalledWith('/lowcode/templates', dto)
    })

    it('instantiateTemplate 调用 POST /lowcode/templates/{tid}/instantiate', async () => {
      mocks.httpPost.mockResolvedValueOnce(102)
      await instantiateTemplate(50, { configName: '实例' })
      expect(mocks.httpPost).toHaveBeenCalledWith('/lowcode/templates/50/instantiate', {
        configName: '实例'
      })
    })
  })

  describe('downloadBlob', () => {
    it('触发浏览器下载并释放 URL', () => {
      const blob = new Blob(['{"a":1}'], { type: 'application/json' })
      const createObjectURL = vi.fn(() => 'blob:fake-url')
      const revokeObjectURL = vi.fn()
      const origURL = window.URL
      Object.defineProperty(window, 'URL', {
        value: { ...origURL, createObjectURL, revokeObjectURL },
        configurable: true
      })
      const fakeAnchor = {
        click: vi.fn(),
        href: '',
        download: ''
      }
      const origCreate = document.createElement
      const origAppend = document.body.appendChild
      const origRemove = document.body.removeChild
      vi.spyOn(document, 'createElement').mockImplementation((tag: string) => {
        if (tag === 'a') return fakeAnchor as any
        return origCreate.call(document, tag)
      })
      vi.spyOn(document.body, 'appendChild').mockImplementation((node: Node) => node)
      vi.spyOn(document.body, 'removeChild').mockImplementation((node: Node) => node)

      downloadBlob(blob, 'test.json')

      expect(createObjectURL).toHaveBeenCalledWith(blob)
      expect(fakeAnchor.href).toBe('blob:fake-url')
      expect(fakeAnchor.download).toBe('test.json')
      expect(fakeAnchor.click).toHaveBeenCalled()
      expect(revokeObjectURL).toHaveBeenCalledWith('blob:fake-url')

      // 恢复
      Object.defineProperty(window, 'URL', { value: origURL, configurable: true })
      vi.restoreAllMocks()
      void origAppend
      void origRemove
    })
  })
})
