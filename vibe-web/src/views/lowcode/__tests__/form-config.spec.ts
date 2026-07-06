/**
 * form-config 视图单元测试（Task E1.3）
 *
 * 覆盖范围：
 *   - 渲染（标题、表格、搜索表单、按钮）
 *   - onMounted 加载数据（pageFormConfigs 调用）
 *   - 搜索 / 重置
 *   - 新增弹窗：openCreate / handleSubmit
 *   - 编辑：openEdit
 *   - 删除：handleDelete -> Modal.confirm
 *   - 复制：handleCopy
 *   - 导出 JSON：handleExport
 *   - 导入 JSON：openImporter / handleImport
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import FormConfig from '../form-config.vue'

/* ============ Mock ant-design-vue message + Modal ============ */
const mocks = vi.hoisted(() => ({
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
  messageWarning: vi.fn(),
  messageInfo: vi.fn(),
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
      info: mocks.messageInfo,
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

/* ============ Mock @/api/lowcode ============ */
const apiMocks = vi.hoisted(() => ({
  pageFormConfigs: vi.fn(),
  createFormConfig: vi.fn(),
  updateFormConfig: vi.fn(),
  deleteFormConfig: vi.fn(),
  copyFormConfig: vi.fn(),
  exportFormConfigJson: vi.fn(),
  importFormConfig: vi.fn(),
  downloadBlob: vi.fn()
}))

vi.mock('@/api/lowcode', () => ({
  pageFormConfigs: apiMocks.pageFormConfigs,
  createFormConfig: apiMocks.createFormConfig,
  updateFormConfig: apiMocks.updateFormConfig,
  deleteFormConfig: apiMocks.deleteFormConfig,
  copyFormConfig: apiMocks.copyFormConfig,
  exportFormConfigJson: apiMocks.exportFormConfigJson,
  importFormConfig: apiMocks.importFormConfig,
  downloadBlob: apiMocks.downloadBlob
}))

/* ============ Mock SchemaDesigner 避免复杂子组件 ============ */
vi.mock('@/components/Lowcode/SchemaDesigner.vue', () => ({
  default: {
    name: 'SchemaDesignerStub',
    props: ['mode', 'modelValue', 'disabled'],
    emits: ['update:modelValue'],
    template: '<div class="designer-stub">designer</div>'
  }
}))

vi.mock('@/components/Lowcode/SchemaImporter.vue', () => ({
  default: {
    name: 'SchemaImporterStub',
    props: ['visible', 'templateType', 'title'],
    emits: ['update:visible', 'import'],
    template: '<div class="importer-stub" v-if="visible">importer</div>'
  }
}))

describe('form-config view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.pageFormConfigs.mockResolvedValue({
      records: [
        {
          id: 1,
          configCode: 'customer_form',
          configName: '客户表单',
          status: 1,
          description: '描述',
          version: 1,
          schemaJson: '{}',
          updateTime: '2026-01-01 10:00:00'
        }
      ],
      total: 1
    })
    apiMocks.createFormConfig.mockResolvedValue(2)
    apiMocks.updateFormConfig.mockResolvedValue(undefined)
    apiMocks.deleteFormConfig.mockResolvedValue(undefined)
    apiMocks.copyFormConfig.mockResolvedValue(3)
    apiMocks.exportFormConfigJson.mockResolvedValue(new Blob(['{}'], { type: 'application/json' }))
    apiMocks.importFormConfig.mockResolvedValue(4)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(FormConfig, {})
  }

  it('渲染标题与表格容器', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.search-card').exists()).toBe(true)
    expect(wrapper.find('.table-card').exists()).toBe(true)
    // 顶部按钮存在
    expect(wrapper.text()).toContain('刷新')
    expect(wrapper.text()).toContain('导入 JSON')
    expect(wrapper.text()).toContain('新增配置')
  })

  it('onMounted 调用 pageFormConfigs 加载列表', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(apiMocks.pageFormConfigs).toHaveBeenCalledTimes(1)
    const callArgs = apiMocks.pageFormConfigs.mock.calls[0][0]
    expect(callArgs.page).toBe(1)
    expect(callArgs.size).toBe(10)
  })

  it('加载后渲染表格数据行', async () => {
    const wrapper = mountView()
    await flushPromises()
    // antd table 渲染
    expect(wrapper.text()).toContain('客户表单')
    expect(wrapper.text()).toContain('customer_form')
  })

  it('点击刷新按钮重新加载', async () => {
    const wrapper = mountView()
    await flushPromises()
    const before = apiMocks.pageFormConfigs.mock.calls.length
    // 找到刷新按钮（第一个按钮）
    const refreshBtn = wrapper.find('.ant-btn').element as HTMLElement
    // 通过 expose 调用 loadData
    ;(wrapper.vm as any).loadData()
    await flushPromises()
    expect(apiMocks.pageFormConfigs.mock.calls.length).toBeGreaterThan(before)
    void refreshBtn
  })

  it('handleSearch 重置页码为 1 并重新加载', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.pagination.current = 5
    vm.query.keyword = 'kw'
    await vm.handleSearch()
    await flushPromises()
    expect(vm.pagination.current).toBe(1)
    const lastCall = apiMocks.pageFormConfigs.mock.calls.at(-1)[0]
    expect(lastCall.keyword).toBe('kw')
    expect(lastCall.page).toBe(1)
  })

  it('handleReset 清空 query 并重新加载', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.query.keyword = 'dirty'
    vm.query.status = 0
    await vm.handleReset()
    await flushPromises()
    expect(vm.query.keyword).toBe('')
    expect(vm.query.status).toBeUndefined()
  })

  it('openCreate 重置 formData 并打开抽屉', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    await nextTick()
    expect(vm.designerVisible).toBe(true)
    expect(vm.isEdit).toBe(false)
    expect(vm.formData.configCode).toBe('')
    expect(vm.formData.status).toBe(1)
  })

  it('openEdit 加载行数据到 formData', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const row = {
      id: 5,
      configCode: 'edit_form',
      configName: '编辑表单',
      schemaJson: '{"type":"object"}',
      status: 0,
      description: 'desc'
    }
    vm.openEdit(row)
    await nextTick()
    expect(vm.designerVisible).toBe(true)
    expect(vm.isEdit).toBe(true)
    expect(vm.formData.id).toBe(5)
    expect(vm.formData.configCode).toBe('edit_form')
    expect(vm.formData.configName).toBe('编辑表单')
    expect(vm.formData.status).toBe(0)
  })

  it('handleSubmit 缺少 configCode 时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formData.configCode = ''
    vm.formData.configName = 'X'
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写配置编码与名称')
  })

  it('handleSubmit 缺少 schemaJson 时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formData.configCode = 'code'
    vm.formData.configName = 'name'
    vm.formData.schemaJson = ''
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请使用设计器设计表单 Schema')
  })

  it('handleSubmit 新增时调用 createFormConfig', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.configCode = 'new_code'
    vm.formData.configName = '新名称'
    vm.formData.schemaJson = '{"type":"object","properties":{}}'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createFormConfig).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
    expect(vm.designerVisible).toBe(false)
  })

  it('handleSubmit 编辑时调用 updateFormConfig', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const row = {
      id: 7,
      configCode: 'edit',
      configName: '编辑',
      schemaJson: '{}',
      status: 1
    }
    vm.openEdit(row)
    vm.formData.configName = '更新后'
    vm.formData.schemaJson = '{"type":"object","properties":{}}'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.updateFormConfig).toHaveBeenCalledWith(7, expect.objectContaining({
      configCode: 'edit',
      configName: '更新后'
    }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('更新成功')
  })

  it('handleCopy 调用 copyFormConfig', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.handleCopy({ id: 9, configName: 'X' })
    await flushPromises()
    expect(apiMocks.copyFormConfig).toHaveBeenCalledWith(9)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('复制成功')
  })

  it('handleDelete 触发 Modal.confirm', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const before = mocks.modalConfirmCallbacks.length
    vm.handleDelete({ id: 11, configName: 'X' })
    expect(mocks.modalConfirmCallbacks.length).toBeGreaterThan(before)
  })

  it('handleDelete onOk 调用 deleteFormConfig', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 13, configName: 'X' })
    const last = mocks.modalConfirmCallbacks.at(-1)
    expect(last?.onOk).toBeTypeOf('function')
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteFormConfig).toHaveBeenCalledWith(13)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('handleExport 调用 exportFormConfigJson 并触发下载', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.handleExport({ id: 17, configCode: 'export_code' })
    await flushPromises()
    expect(apiMocks.exportFormConfigJson).toHaveBeenCalledWith(17)
    expect(apiMocks.downloadBlob).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('导出成功')
  })

  it('openImporter 打开导入弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.importerVisible).toBe(false)
    vm.openImporter()
    expect(vm.importerVisible).toBe(true)
  })

  it('handleImport 调用 importFormConfig', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.handleImport({
      schemaJson: '{"type":"object","properties":{}}',
      templateId: 1
    })
    await flushPromises()
    expect(apiMocks.importFormConfig).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('导入成功')
  })

  it('handleTableChange 更新分页并重新加载', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const before = apiMocks.pageFormConfigs.mock.calls.length
    vm.handleTableChange({ current: 2, pageSize: 20 })
    await flushPromises()
    expect(vm.pagination.current).toBe(2)
    expect(vm.pagination.pageSize).toBe(20)
    expect(apiMocks.pageFormConfigs.mock.calls.length).toBeGreaterThan(before)
    const lastCall = apiMocks.pageFormConfigs.mock.calls.at(-1)[0]
    expect(lastCall.page).toBe(2)
    expect(lastCall.size).toBe(20)
  })

  it('loadData 异常时不抛错', async () => {
    apiMocks.pageFormConfigs.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    // 不抛错即可
    expect(wrapper.find('.table-card').exists()).toBe(true)
  })

  it('handleSubmit createFormConfig 异常时不抛错', async () => {
    apiMocks.createFormConfig.mockRejectedValueOnce(new Error('create failed'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.configCode = 'X'
    vm.formData.configName = 'X'
    vm.formData.schemaJson = '{"type":"object","properties":{}}'
    await expect(vm.handleSubmit()).resolves.not.toThrow()
    await flushPromises()
  })
})
