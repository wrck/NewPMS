/**
 * ImportExport 组件单元测试（spec 阶段三 Task 20 - SubTask 20.5）
 *
 * 覆盖范围：
 *   - 组件渲染（按钮文案、显隐控制）
 *   - 导出按钮：调用 fetch、解析 Content-Disposition、触发 Blob 下载、emit 事件
 *   - 导入按钮：before-upload 校验文件类型/大小、上传 FormData、解析结果、弹窗展示
 *   - 模板下载按钮：fetch + Blob 下载
 *   - 错误处理：HTTP 错误、JSON 错误响应、网络异常
 *   - Expose 方法
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ImportExport from '../index.vue'
import type { ImportResult } from '../types'

/* ============ Mock ant-design-vue 静态方法 ============ */
const mocks = vi.hoisted(() => {
  return {
    messageSuccess: vi.fn(),
    messageError: vi.fn(),
    messageWarning: vi.fn(),
    messageInfo: vi.fn()
  }
})
const { messageSuccess, messageError, messageWarning, messageInfo } = mocks

vi.mock('ant-design-vue', async () => {
  const actual = await vi.importActual<typeof import('ant-design-vue')>('ant-design-vue')
  return {
    ...actual,
    message: {
      success: mocks.messageSuccess,
      error: mocks.messageError,
      warning: mocks.messageWarning,
      info: mocks.messageInfo,
      loading: vi.fn()
    }
  }
})

/* ============ Mock useUserStore ============ */
const userStoreMock = vi.hoisted(() => ({
  token: 'fake-token-xyz'
}))
vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    token: userStoreMock.token
  })
}))

/* ============ fetch mock 工具 ============ */
type FetchOptions = {
  ok?: boolean
  status?: number
  statusText?: string
  contentType?: string
  body?: Blob | string
  json?: unknown
  disposition?: string
}

function makeFetchResponse(opts: FetchOptions = {}): Response {
  const ok = opts.ok ?? true
  const status = opts.status ?? 200
  const statusText = opts.statusText ?? 'OK'
  const contentType = opts.contentType || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  const bodyBlob: Blob =
    opts.body instanceof Blob
      ? opts.body
      : new Blob([opts.body ?? 'fake-excel-bytes'], {
          type: contentType
        })
  const headers = new Map<string, string>()
  headers.set('content-type', contentType)
  if (opts.disposition) {
    headers.set('content-disposition', opts.disposition)
  }
  const jsonValue = opts.json
  const response: Partial<Response> = {
    ok,
    status,
    statusText,
    headers: {
      get: (name: string) => headers.get(name.toLowerCase()) || null
    } as any,
    blob: async () => bodyBlob,
    json: async () => (jsonValue ?? {}) as any
  }
  return response as Response
}

/** 收集 fetch 调用参数 */
const fetchCalls: Array<{ url: string; init?: RequestInit }> = []
let fetchImpl: ((url: string, init?: RequestInit) => Promise<Response>) | null = null

function installFetch(impl: (url: string, init?: RequestInit) => Promise<Response>) {
  fetchImpl = impl
}

beforeEach(() => {
  fetchCalls.length = 0
  fetchImpl = null
  // 默认 fetch：返回成功 Blob
  const defaultImpl = (url: string, init?: RequestInit) => {
    fetchCalls.push({ url, init })
    return Promise.resolve(makeFetchResponse())
  }
  installFetch(defaultImpl)
  ;(globalThis as any).fetch = vi.fn((url: string, init?: RequestInit) => {
    if (fetchImpl) return fetchImpl(url, init)
    return Promise.resolve(makeFetchResponse())
  })
})

afterEach(() => {
  ;(globalThis as any).fetch = undefined
})

/* ============ DOM 下载 mock ============ */
/**
 * 不 mock document.createElement / appendChild / removeChild，避免破坏 Vue 挂载流程。
 * - URL.createObjectURL / revokeObjectURL 用 polyfill（jsdom 未实现），再 spy 用于断言
 * - HTMLAnchorElement.prototype.click 被 spy，捕获 this（即下载用的 <a> 元素）用于断言 download 文件名
 */
const clickSpy = vi.fn()
let lastClickedAnchor: HTMLAnchorElement | null = null
let createObjectURLSpy: any
let revokeObjectURLSpy: any

beforeEach(() => {
  clickSpy.mockClear()
  lastClickedAnchor = null
  // 捕获 <a>.click() 的 this 以便断言 download / href 属性
  vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
    lastClickedAnchor = this
    clickSpy()
  })
  createObjectURLSpy = vi.spyOn(window.URL, 'createObjectURL').mockReturnValue('blob:fake-url')
  revokeObjectURLSpy = vi.spyOn(window.URL, 'revokeObjectURL').mockImplementation(() => undefined)
})

afterEach(() => {
  vi.restoreAllMocks()
})

/* ============ 工具：构造 wrapper ============ */
function makeWrapper(props: Record<string, any> = {}) {
  return mount(ImportExport, {
    props: {
      exportApi: '/api/v1/test/export',
      importApi: '/api/v1/test/import',
      templateUrl: '/api/v1/test/template',
      ...props
    },
    global: {
      stubs: {
        DownloadOutlined: true,
        UploadOutlined: true,
        FileExcelOutlined: true
      }
    }
  })
}

/* ============ 测试用例 ============ */
describe('ImportExport', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('渲染', () => {
    it('默认渲染三个按钮', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const text = wrapper.text()
      expect(text).toContain('导出')
      expect(text).toContain('导入')
      expect(text).toContain('下载模板')
    })

    it('showExport=false 隐藏导出按钮', async () => {
      const wrapper = makeWrapper({ showExport: false })
      await flushPromises()
      const text = wrapper.text()
      expect(text).not.toContain('导出')
      expect(text).toContain('导入')
    })

    it('showImport=false 隐藏导入按钮', async () => {
      const wrapper = makeWrapper({ showImport: false })
      await flushPromises()
      const text = wrapper.text()
      expect(text).toContain('导出')
      expect(text).not.toContain('导入')
    })

    it('showTemplate=false 隐藏模板按钮', async () => {
      const wrapper = makeWrapper({ showTemplate: false })
      await flushPromises()
      expect(wrapper.text()).not.toContain('下载模板')
    })

    it('自定义按钮文案', async () => {
      const wrapper = makeWrapper({
        exportText: '导出用户',
        importText: '批量导入',
        templateText: '下载模板文件'
      })
      await flushPromises()
      const text = wrapper.text()
      expect(text).toContain('导出用户')
      expect(text).toContain('批量导入')
      expect(text).toContain('下载模板文件')
    })

    it('full=true 显示全部按钮（与默认一致）', async () => {
      const wrapper = makeWrapper({ full: true })
      await flushPromises()
      const text = wrapper.text()
      expect(text).toContain('导出')
      expect(text).toContain('导入')
      expect(text).toContain('下载模板')
    })
  })

  describe('导出', () => {
    it('点击导出按钮调用 fetch 并触发下载', async () => {
      const wrapper = makeWrapper({
        exportParams: { keyword: 'foo', status: 1 },
        exportFileName: '用户列表.xlsx'
      })
      await flushPromises()
      // 找到导出按钮（第一个按钮）
      const buttons = wrapper.findAll('button')
      const exportBtn = buttons[0]
      expect(exportBtn.exists()).toBe(true)
      await exportBtn.trigger('click')
      await flushPromises()

      // fetch 被调用
      expect((globalThis as any).fetch).toHaveBeenCalledTimes(1)
      const call = (globalThis as any).fetch.mock.calls[0]
      expect(call[0]).toBe('/api/v1/test/export')
      const init: RequestInit = call[1]
      expect(init.method).toBe('POST')
      expect(init.headers).toMatchObject({
        'Content-Type': 'application/json',
        Authorization: 'Bearer fake-token-xyz'
      })
      expect(init.body).toBe(JSON.stringify({ keyword: 'foo', status: 1 }))

      // 触发 Blob 下载
      expect(createObjectURLSpy).toHaveBeenCalledTimes(1)
      expect(clickSpy).toHaveBeenCalledTimes(1)
      expect(messageSuccess).toHaveBeenCalledWith('导出成功')
    })

    it('从 Content-Disposition 解析文件名', async () => {
      installFetch(async () =>
        makeFetchResponse({
          disposition: "attachment; filename*=UTF-8''%E7%94%A8%E6%88%B7.xlsx"
        })
      )
      const wrapper = makeWrapper()
      await flushPromises()
      const buttons = wrapper.findAll('button')
      await buttons[0].trigger('click')
      await flushPromises()

      // lastClickedAnchor 在 click spy 中捕获，组件在其上设置了 download
      expect(lastClickedAnchor).not.toBeNull()
      expect(lastClickedAnchor!.download).toBe('用户.xlsx')
    })

    it('Content-Disposition 缺失时使用 exportFileName 兜底', async () => {
      installFetch(async () => makeFetchResponse({ disposition: '' }))
      const wrapper = makeWrapper({ exportFileName: 'fallback.xlsx' })
      await flushPromises()
      const buttons = wrapper.findAll('button')
      await buttons[0].trigger('click')
      await flushPromises()

      expect(lastClickedAnchor).not.toBeNull()
      expect(lastClickedAnchor!.download).toBe('fallback.xlsx')
    })

    it('未配置 exportApi 时提示错误', async () => {
      const wrapper = makeWrapper({ exportApi: undefined })
      await flushPromises()
      const buttons = wrapper.findAll('button')
      await buttons[0].trigger('click')
      await flushPromises()

      expect(messageError).toHaveBeenCalledWith('未配置导出接口')
      expect((globalThis as any).fetch).not.toHaveBeenCalled()
    })

    it('HTTP 状态码非 2xx 时报错', async () => {
      installFetch(async () =>
        makeFetchResponse({
          ok: false,
          status: 500,
          statusText: 'Internal Server Error',
          contentType: 'text/plain'
        })
      )
      const wrapper = makeWrapper()
      await flushPromises()
      const buttons = wrapper.findAll('button')
      await buttons[0].trigger('click')
      await flushPromises()

      expect(messageError).toHaveBeenCalled()
      const errMsg = messageError.mock.calls[0][0]
      expect(errMsg).toContain('500')
    })

    it('后端返回 JSON 错误结构时读取 message', async () => {
      installFetch(async () =>
        makeFetchResponse({
          ok: true,
          status: 200,
          contentType: 'application/json',
          json: { code: 50001, message: '导出失败：参数非法' }
        })
      )
      const wrapper = makeWrapper()
      await flushPromises()
      const buttons = wrapper.findAll('button')
      await buttons[0].trigger('click')
      await flushPromises()

      expect(messageError).toHaveBeenCalledWith('导出失败：参数非法')
    })

    it('导出成功 emit export-success', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const buttons = wrapper.findAll('button')
      await buttons[0].trigger('click')
      await flushPromises()

      const emitted = wrapper.emitted()
      expect(emitted['export-success']).toBeTruthy()
      expect(emitted['export-success']).toHaveLength(1)
    })

    it('导出失败 emit export-error', async () => {
      installFetch(async () => {
        throw new Error('network down')
      })
      const wrapper = makeWrapper()
      await flushPromises()
      const buttons = wrapper.findAll('button')
      await buttons[0].trigger('click')
      await flushPromises()

      const emitted = wrapper.emitted()
      expect(emitted['export-error']).toBeTruthy()
      expect(messageError).toHaveBeenCalled()
    })

    it('exportDisabled=true 时不触发导出', async () => {
      const wrapper = makeWrapper({ exportDisabled: true })
      await flushPromises()
      const buttons = wrapper.findAll('button')
      const exportBtn = buttons[0]
      // disabled 状态下点击应不触发
      expect(exportBtn.attributes('disabled')).toBeDefined()
      // 直接调用 handleExport 验证短路
      const vm = wrapper.vm as any
      await vm.handleExport()
      expect((globalThis as any).fetch).not.toHaveBeenCalled()
    })
  })

  describe('导入', () => {
    /** 模拟 a-upload 调用 beforeUpload 钩子 */
    async function triggerBeforeUpload(wrapper: any, file: File): Promise<boolean> {
      const vm = wrapper.vm as any
      return vm.handleBeforeUpload(file)
    }

    function makeFile(name: string, size: number = 1024): File {
      const blob = new Blob([new Uint8Array(size)], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
      const file = new File([blob], name, {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      })
      Object.defineProperty(file, 'size', { value: size })
      return file
    }

    it('beforeUpload 校验文件类型（仅允许 xlsx/xls）', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const ret = await triggerBeforeUpload(wrapper, makeFile('file.txt'))
      expect(ret).toBe(false)
      expect(messageError).toHaveBeenCalledWith('仅支持 Excel 文件 (.xlsx, .xls)')
    })

    it('beforeUpload 校验文件大小（默认 10MB）', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      // 11MB
      const bigFile = makeFile('big.xlsx', 11 * 1024 * 1024)
      const ret = await triggerBeforeUpload(wrapper, bigFile)
      expect(ret).toBe(false)
      expect(messageError).toHaveBeenCalledWith('文件大小不能超过 10MB')
    })

    it('beforeUpload maxSizeMb 自定义上限', async () => {
      const wrapper = makeWrapper({ maxSizeMb: 1 })
      await flushPromises()
      // 2MB
      const file = makeFile('ok.xlsx', 2 * 1024 * 1024)
      const ret = await triggerBeforeUpload(wrapper, file)
      expect(ret).toBe(false)
      expect(messageError).toHaveBeenCalledWith('文件大小不能超过 1MB')
    })

    it('未配置 importApi 时报错', async () => {
      const wrapper = makeWrapper({ importApi: undefined })
      await flushPromises()
      const ret = await triggerBeforeUpload(wrapper, makeFile('ok.xlsx'))
      expect(ret).toBe(false)
      expect(messageError).toHaveBeenCalledWith('未配置导入接口')
    })

    it('上传成功后弹出结果弹窗并 emit import-success', async () => {
      const importPayload = {
        code: 200,
        message: 'ok',
        data: {
          successCount: 95,
          failCount: 5,
          totalCount: 100,
          duration: 1234,
          errors: [
            { row: 3, field: 'phone', message: '手机号格式错误', value: 'abc' }
          ]
        }
      }
      installFetch(async () =>
        makeFetchResponse({
          contentType: 'application/json',
          json: importPayload
        })
      )
      const wrapper = makeWrapper()
      await flushPromises()
      const file = makeFile('users.xlsx', 1024)
      await triggerBeforeUpload(wrapper, file)
      await flushPromises()

      // 验证 fetch 调用
      expect((globalThis as any).fetch).toHaveBeenCalledTimes(1)
      const call = (globalThis as any).fetch.mock.calls[0]
      expect(call[0]).toBe('/api/v1/test/import')
      const init: RequestInit = call[1]
      expect(init.method).toBe('POST')
      expect(init.headers).toMatchObject({
        Authorization: 'Bearer fake-token-xyz'
      })
      // body 应为 FormData
      expect(init.body).toBeInstanceOf(FormData)

      // 弹窗显示
      const vm = wrapper.vm as any
      expect(vm.resultVisible).toBe(true)
      expect(vm.importResult.successCount).toBe(95)
      expect(vm.importResult.failCount).toBe(5)
      expect(vm.importResult.totalCount).toBe(100)
      expect(vm.importResult.duration).toBe(1234)
      expect(vm.importResult.errors).toHaveLength(1)

      // 由于存在 failCount，应弹 warning
      expect(messageWarning).toHaveBeenCalled()

      const emitted = wrapper.emitted()
      expect(emitted['import-success']).toBeTruthy()
      const result = (emitted['import-success'] as any)[0][0] as ImportResult
      expect(result.successCount).toBe(95)
      expect(result.failCount).toBe(5)
    })

    it('全部成功时 message.success', async () => {
      installFetch(async () =>
        makeFetchResponse({
          contentType: 'application/json',
          json: {
            code: 200,
            message: 'ok',
            data: { successCount: 100, failCount: 0, totalCount: 100 }
          }
        })
      )
      const wrapper = makeWrapper()
      await flushPromises()
      await triggerBeforeUpload(wrapper, makeFile('ok.xlsx'))
      await flushPromises()
      expect(messageSuccess).toHaveBeenCalledWith('导入成功')
    })

    it('全部失败时 message.error', async () => {
      installFetch(async () =>
        makeFetchResponse({
          contentType: 'application/json',
          json: {
            code: 200,
            message: 'ok',
            data: { successCount: 0, failCount: 100, totalCount: 100 }
          }
        })
      )
      const wrapper = makeWrapper()
      await flushPromises()
      await triggerBeforeUpload(wrapper, makeFile('ok.xlsx'))
      await flushPromises()
      expect(messageError).toHaveBeenCalled()
      const msg = messageError.mock.calls[0][0]
      expect(msg).toContain('100')
    })

    it('autoRefresh=true 时 emit refresh', async () => {
      installFetch(async () =>
        makeFetchResponse({
          contentType: 'application/json',
          json: {
            code: 200,
            message: 'ok',
            data: { successCount: 10, failCount: 0, totalCount: 10 }
          }
        })
      )
      const wrapper = makeWrapper({ autoRefresh: true })
      await flushPromises()
      await triggerBeforeUpload(wrapper, makeFile('ok.xlsx'))
      await flushPromises()
      expect(wrapper.emitted('refresh')).toBeTruthy()
    })

    it('autoRefresh=false 时不 emit refresh', async () => {
      installFetch(async () =>
        makeFetchResponse({
          contentType: 'application/json',
          json: {
            code: 200,
            message: 'ok',
            data: { successCount: 10, failCount: 0, totalCount: 10 }
          }
        })
      )
      const wrapper = makeWrapper({ autoRefresh: false })
      await flushPromises()
      await triggerBeforeUpload(wrapper, makeFile('ok.xlsx'))
      await flushPromises()
      expect(wrapper.emitted('refresh')).toBeFalsy()
    })

    it('HTTP 错误状态码时报错并 emit import-error', async () => {
      installFetch(async () =>
        makeFetchResponse({
          ok: false,
          status: 400,
          statusText: 'Bad Request',
          contentType: 'application/json',
          json: { message: '文件格式不正确' }
        })
      )
      const wrapper = makeWrapper()
      await flushPromises()
      await triggerBeforeUpload(wrapper, makeFile('ok.xlsx'))
      await flushPromises()
      expect(messageError).toHaveBeenCalledWith('文件格式不正确')
      expect(wrapper.emitted('import-error')).toBeTruthy()
    })

    it('fetch 抛异常时 emit import-error', async () => {
      installFetch(async () => {
        throw new Error('network error')
      })
      const wrapper = makeWrapper()
      await flushPromises()
      await triggerBeforeUpload(wrapper, makeFile('ok.xlsx'))
      await flushPromises()
      expect(messageError).toHaveBeenCalledWith('network error')
      expect(wrapper.emitted('import-error')).toBeTruthy()
      // loading 应回到 false
      const vm = wrapper.vm as any
      expect(vm.importLoading).toBe(false)
    })

    it('importData 附加字段加入 FormData', async () => {
      installFetch(async () =>
        makeFetchResponse({
          contentType: 'application/json',
          json: {
            code: 200,
            message: 'ok',
            data: { successCount: 1, failCount: 0, totalCount: 1 }
          }
        })
      )
      const wrapper = makeWrapper({ importData: { tenantId: 'T001', overwrite: true } })
      await flushPromises()
      await triggerBeforeUpload(wrapper, makeFile('ok.xlsx'))
      await flushPromises()
      const init: RequestInit = (globalThis as any).fetch.mock.calls[0][1]
      const fd = init.body as FormData
      expect(fd.get('tenantId')).toBe('T001')
      expect(fd.get('overwrite')).toBe('true')
      expect(fd.get('file')).toBeInstanceOf(File)
    })
  })

  describe('模板下载', () => {
    function findTemplateButton(wrapper: any) {
      // 第三个按钮（导出/导入/模板）
      const buttons = wrapper.findAll('button')
      return buttons[buttons.length - 1]
    }

    it('点击下载模板调用 fetch + 触发下载', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const btn = findTemplateButton(wrapper)
      await btn.trigger('click')
      await flushPromises()

      expect((globalThis as any).fetch).toHaveBeenCalledTimes(1)
      const call = (globalThis as any).fetch.mock.calls[0]
      expect(call[0]).toBe('/api/v1/test/template')
      const init: RequestInit = call[1]
      expect(init.method).toBe('GET')
      expect(init.headers).toMatchObject({
        Authorization: 'Bearer fake-token-xyz'
      })
      expect(createObjectURLSpy).toHaveBeenCalledTimes(1)
      expect(clickSpy).toHaveBeenCalledTimes(1)
      expect(messageSuccess).toHaveBeenCalledWith('模板下载成功')
      expect(wrapper.emitted('template-downloaded')).toBeTruthy()
    })

    it('未配置 templateUrl 时报错', async () => {
      const wrapper = makeWrapper({ templateUrl: undefined })
      await flushPromises()
      const btn = findTemplateButton(wrapper)
      await btn.trigger('click')
      await flushPromises()
      expect(messageError).toHaveBeenCalledWith('未配置模板下载地址')
      expect((globalThis as any).fetch).not.toHaveBeenCalled()
    })

    it('HTTP 错误时报错并 emit template-error', async () => {
      installFetch(async () =>
        makeFetchResponse({
          ok: false,
          status: 404,
          statusText: 'Not Found',
          contentType: 'text/plain'
        })
      )
      const wrapper = makeWrapper()
      await flushPromises()
      const btn = findTemplateButton(wrapper)
      await btn.trigger('click')
      await flushPromises()
      expect(messageError).toHaveBeenCalled()
      const msg = messageError.mock.calls[0][0]
      expect(msg).toContain('404')
      expect(wrapper.emitted('template-error')).toBeTruthy()
    })

    it('从 Content-Disposition 解析模板文件名', async () => {
      installFetch(async () =>
        makeFetchResponse({
          disposition: 'attachment; filename="user_template.xlsx"'
        })
      )
      const wrapper = makeWrapper()
      await flushPromises()
      const btn = findTemplateButton(wrapper)
      await btn.trigger('click')
      await flushPromises()
      // lastClickedAnchor 在 click spy 中捕获
      expect(lastClickedAnchor).not.toBeNull()
      expect(lastClickedAnchor!.download).toBe('user_template.xlsx')
    })

    it('fetch 异常时 emit template-error', async () => {
      installFetch(async () => {
        throw new Error('net down')
      })
      const wrapper = makeWrapper()
      await flushPromises()
      const btn = findTemplateButton(wrapper)
      await btn.trigger('click')
      await flushPromises()
      expect(messageError).toHaveBeenCalledWith('net down')
      expect(wrapper.emitted('template-error')).toBeTruthy()
    })
  })

  describe('结果弹窗', () => {
    it('closeResult 关闭弹窗', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      vm.resultVisible = true
      vm.closeResult()
      expect(vm.resultVisible).toBe(false)
    })

    it('handleResultClose 关闭弹窗', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      vm.resultVisible = true
      vm.handleResultClose()
      expect(vm.resultVisible).toBe(false)
    })
  })

  describe('Expose', () => {
    it('expose.export 主动触发导出', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      await vm.export()
      await flushPromises()
      expect((globalThis as any).fetch).toHaveBeenCalledTimes(1)
    })

    it('expose.downloadTemplate 主动下载模板', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      await vm.downloadTemplate()
      await flushPromises()
      expect((globalThis as any).fetch).toHaveBeenCalledTimes(1)
    })

    it('expose.closeResult 关闭弹窗', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      vm.resultVisible = true
      vm.closeResult()
      expect(vm.resultVisible).toBe(false)
    })
  })

  describe('工具函数', () => {
    it('parseFileName 支持 filename*=RFC 5987', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      const result = vm.parseFileName(
        "attachment; filename*=UTF-8''%E7%94%A8%E6%88%B7.xlsx",
        'fallback.xlsx'
      )
      expect(result).toBe('用户.xlsx')
    })

    it('parseFileName 支持 filename="xxx" 形式', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      const result = vm.parseFileName('attachment; filename="abc.xlsx"', 'fallback.xlsx')
      expect(result).toBe('abc.xlsx')
    })

    it('parseFileName 缺失时返回 fallback', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.parseFileName('', 'fallback.xlsx')).toBe('fallback.xlsx')
      expect(vm.parseFileName('no-match-here', 'fallback.xlsx')).toBe('fallback.xlsx')
    })

    it('triggerBlobDownload 触发浏览器下载流程', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      const blob = new Blob(['x'], { type: 'text/plain' })
      vm.triggerBlobDownload(blob, 'test.xlsx')
      expect(createObjectURLSpy).toHaveBeenCalledWith(blob)
      expect(clickSpy).toHaveBeenCalledTimes(1)
      expect(lastClickedAnchor).not.toBeNull()
      expect(lastClickedAnchor!.download).toBe('test.xlsx')
      expect(lastClickedAnchor!.href).toBe('blob:fake-url')
      // revokeObjectURL 通过 setTimeout 异步调用，等待一下
      await new Promise((resolve) => setTimeout(resolve, 10))
      expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:fake-url')
    })

    it('resolveImportResult 兼容多种后端结构', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      // 标准 Result 包装
      const r1 = vm.resolveImportResult({
        code: 200,
        data: { successCount: 5, failCount: 0, totalCount: 5, errors: [] }
      })
      expect(r1.successCount).toBe(5)
      expect(r1.success).toBe(true)
      // 直接返回 data
      const r2 = vm.resolveImportResult({
        successCount: 3,
        failCount: 1,
        totalCount: 4
      })
      expect(r2.successCount).toBe(3)
      expect(r2.failCount).toBe(1)
      // 空 data
      const r3 = vm.resolveImportResult({})
      expect(r3.successCount).toBe(0)
      expect(r3.failCount).toBe(0)
      expect(r3.errors).toEqual([])
    })
  })
})
