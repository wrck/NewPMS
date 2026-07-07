/**
 * acceptance/doc 视图单元测试（Task 2.4）
 *
 * 覆盖：
 *   - 渲染（标题、表格、搜索）
 *   - onMounted 加载 pageAcceptanceDocs
 *   - 搜索 / 重置
 *   - openCreate / openEdit
 *   - handleSubmit 新建/编辑
 *   - handleDelete -> Modal.confirm
 *   - docTypeMap 包含 6 类型
 *   - formatFileSize 工具函数
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Doc from '../doc.vue'

/* ============ Mock ant-design-vue message + Modal ============ */
const mocks = vi.hoisted(() => ({
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
  messageWarning: vi.fn(),
  modalConfirmCallbacks: [] as Array<{ onOk?: () => any; onCancel?: () => any }>
}))

vi.mock('ant-design-vue', async () => {
  const actual = await vi.importActual<typeof import('ant-design-vue')>('ant-design-vue')
  return {
    ...actual,
    message: {
      success: mocks.messageSuccess,
      error: mocks.messageError,
      warning: mocks.messageWarning,
      info: vi.fn(),
      loading: vi.fn()
    },
    Modal: {
      confirm: (opts: any) => {
        mocks.modalConfirmCallbacks.push({ onOk: opts?.onOk, onCancel: opts?.onCancel })
        return { destroy: vi.fn(), update: vi.fn() }
      }
    }
  }
})

/* ============ Mock @/api/acceptance ============ */
const apiMocks = vi.hoisted(() => ({
  pageAcceptanceDocs: vi.fn(),
  createAcceptanceDoc: vi.fn(),
  updateAcceptanceDoc: vi.fn(),
  deleteAcceptanceDoc: vi.fn()
}))

vi.mock('@/api/acceptance', () => apiMocks)

describe('acceptance doc view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.pageAcceptanceDocs.mockResolvedValue({
      records: [
        {
          id: 1,
          name: '网络拓扑图.pdf',
          docType: 'TOPOLOGY',
          projectId: 100,
          docVersion: '1.0',
          fileSize: 1024,
          createTime: '2026-07-01 10:00:00'
        }
      ],
      total: 1
    })
    apiMocks.createAcceptanceDoc.mockResolvedValue(2)
    apiMocks.updateAcceptanceDoc.mockResolvedValue(undefined)
    apiMocks.deleteAcceptanceDoc.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Doc, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('竣工文档')
  })

  it('onMounted 调用 pageAcceptanceDocs', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.pageAcceptanceDocs).toHaveBeenCalledTimes(1)
  })

  it('handleSearch 重置页码为 1', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.pagination.current = 5
    vm.query.projectId = 100
    await vm.handleSearch()
    await flushPromises()
    expect(vm.pagination.current).toBe(1)
  })

  it('handleReset 清空 query', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.query.projectId = 999
    vm.query.docType = 'TOPOLOGY'
    await vm.handleReset()
    await flushPromises()
    expect(vm.query.projectId).toBeUndefined()
    expect(vm.query.docType).toBeUndefined()
  })

  it('openCreate 重置 form 并打开弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    expect(vm.modalVisible).toBe(true)
    expect(vm.form.name).toBe('')
    expect(vm.form.docType).toBe('TOPOLOGY')
  })

  it('openEdit 加载记录到 form', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openEdit({ id: 5, taskId: 1, projectId: 100, docType: 'TEST_REPORT', name: 'r.pdf', fileUrl: '/x.pdf', fileSize: 100, docVersion: '2.0', remark: '' })
    expect(vm.modalVisible).toBe(true)
    expect(vm.form.id).toBe(5)
    expect(vm.form.docType).toBe('TEST_REPORT')
  })

  it('handleSubmit 新建调用 createAcceptanceDoc', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formRef = { validate: () => Promise.resolve() }
    vm.openCreate()
    vm.form.taskId = 1
    vm.form.projectId = 100
    vm.form.docType = 'TOPOLOGY'
    vm.form.name = '拓扑'
    vm.form.fileUrl = '/topo.pdf'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createAcceptanceDoc).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
  })

  it('handleSubmit 编辑调用 updateAcceptanceDoc', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formRef = { validate: () => Promise.resolve() }
    vm.openEdit({ id: 7, taskId: 1, projectId: 100, docType: 'TOPOLOGY', name: 'old.pdf', fileUrl: '/x.pdf', fileSize: 0, docVersion: '1.0', remark: '' })
    vm.form.name = 'new.pdf'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.updateAcceptanceDoc).toHaveBeenCalledWith(7, expect.objectContaining({ name: 'new.pdf' }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('更新成功')
  })

  it('handleDelete onOk 调用 deleteAcceptanceDoc', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 13, name: 'X' })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteAcceptanceDoc).toHaveBeenCalledWith(13)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('docTypeMap 包含 6 种文档类型', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(Object.keys(vm.docTypeMap).length).toBe(6)
    expect(vm.docTypeMap.TOPOLOGY.label).toBe('网络拓扑图')
    expect(vm.docTypeMap.DEVICE_LIST.label).toBe('设备清单')
    expect(vm.docTypeMap.CONFIG_BACKUP.label).toBe('配置备份')
    expect(vm.docTypeMap.TEST_REPORT.label).toBe('测试报告')
    expect(vm.docTypeMap.MAINTENANCE_MANUAL.label).toBe('维护手册')
  })

  it('formatFileSize 格式化文件大小', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.formatFileSize(undefined)).toBe('-')
    expect(vm.formatFileSize(500)).toBe('500 B')
    expect(vm.formatFileSize(1024)).toBe('1.0 KB')
    expect(vm.formatFileSize(1024 * 1024)).toBe('1.0 MB')
    expect(vm.formatFileSize(2 * 1024 * 1024)).toBe('2.0 MB')
  })

  it('pageAcceptanceDocs 异常时不抛错', async () => {
    apiMocks.pageAcceptanceDocs.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.dataSource).toEqual([])
  })
})
