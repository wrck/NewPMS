/**
 * warehouse 视图单元测试（Task E2.1）
 *
 * 覆盖 CRUD 套路：
 *   - 渲染（标题、表格、搜索表单、按钮）
 *   - onMounted 加载列表（pageWarehouses 调用）
 *   - 搜索 / 重置
 *   - 新增弹窗：openCreate / handleSubmit
 *   - 编辑：openEdit
 *   - 删除：handleDelete -> Modal.confirm
 *   - safetyStock JSON 校验
 *   - formatSafetyStock 工具函数
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import Warehouse from '../warehouse.vue'

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

/* ============ Mock @/api/device ============ */
const apiMocks = vi.hoisted(() => ({
  pageWarehouses: vi.fn(),
  createWarehouse: vi.fn(),
  updateWarehouse: vi.fn(),
  deleteWarehouse: vi.fn()
}))

vi.mock('@/api/device', () => ({
  pageWarehouses: apiMocks.pageWarehouses,
  createWarehouse: apiMocks.createWarehouse,
  updateWarehouse: apiMocks.updateWarehouse,
  deleteWarehouse: apiMocks.deleteWarehouse
}))

describe('warehouse view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.pageWarehouses.mockResolvedValue({
      records: [
        {
          id: 1,
          warehouseCode: 'WH-001',
          warehouseName: '北京中心仓',
          address: '北京市朝阳区',
          region: '华北',
          managerId: 10,
          managerName: '张三',
          safetyStock: '{"1001":5,"1002":3}',
          createTime: '2026-01-01 10:00:00'
        }
      ],
      total: 1
    })
    apiMocks.createWarehouse.mockResolvedValue(2)
    apiMocks.updateWarehouse.mockResolvedValue(undefined)
    apiMocks.deleteWarehouse.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Warehouse, {})
  }

  it('渲染标题与表格容器', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.search-card').exists()).toBe(true)
    expect(wrapper.find('.table-card').exists()).toBe(true)
    expect(wrapper.text()).toContain('刷新')
    expect(wrapper.text()).toContain('新增仓库')
  })

  it('onMounted 调用 pageWarehouses 加载列表', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(apiMocks.pageWarehouses).toHaveBeenCalledTimes(1)
    const args = apiMocks.pageWarehouses.mock.calls[0][0]
    expect(args.page).toBe(1)
    expect(args.size).toBe(10)
  })

  it('加载后渲染表格数据行', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('北京中心仓')
    expect(wrapper.text()).toContain('WH-001')
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
    const last = apiMocks.pageWarehouses.mock.calls.at(-1)[0]
    expect(last.keyword).toBe('kw')
    expect(last.page).toBe(1)
  })

  it('handleReset 清空 query 并重新加载', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.query.keyword = 'dirty'
    vm.query.region = 'dirty'
    await vm.handleReset()
    await flushPromises()
    expect(vm.query.keyword).toBe('')
    expect(vm.query.region).toBe('')
  })

  it('openCreate 重置 formData 并打开弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    await nextTick()
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(false)
    expect(vm.formData.warehouseCode).toBe('')
    expect(vm.formData.warehouseName).toBe('')
  })

  it('openEdit 加载行数据到 formData', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const row = {
      id: 5,
      warehouseCode: 'WH-002',
      warehouseName: '上海仓',
      region: '华东',
      address: '上海',
      managerId: 12,
      safetyStock: '{"1003":2}'
    }
    vm.openEdit(row)
    await nextTick()
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(true)
    expect(vm.formData.id).toBe(5)
    expect(vm.formData.warehouseCode).toBe('WH-002')
    expect(vm.formData.warehouseName).toBe('上海仓')
    expect(vm.formData.safetyStock).toBe('{"1003":2}')
  })

  it('handleSubmit 缺少编码与名称时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formData.warehouseCode = ''
    vm.formData.warehouseName = ''
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写仓库编码和名称')
  })

  it('handleSubmit safetyStock 非法 JSON 时 error', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formData.warehouseCode = 'WH-X'
    vm.formData.warehouseName = 'X'
    vm.formData.safetyStock = 'not-json'
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageError).toHaveBeenCalledWith(
      '安全库存配置需为合法 JSON，例如 {"1001":5,"1002":3}'
    )
    expect(apiMocks.createWarehouse).not.toHaveBeenCalled()
  })

  it('handleSubmit 新增时调用 createWarehouse', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.warehouseCode = 'WH-NEW'
    vm.formData.warehouseName = '新仓库'
    vm.formData.safetyStock = '{"1001":5}'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createWarehouse).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
    expect(vm.formVisible).toBe(false)
  })

  it('handleSubmit 编辑时调用 updateWarehouse', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openEdit({
      id: 7,
      warehouseCode: 'WH-7',
      warehouseName: '旧名',
      safetyStock: ''
    })
    vm.formData.warehouseName = '新名'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.updateWarehouse).toHaveBeenCalledWith(7, expect.objectContaining({
      warehouseCode: 'WH-7',
      warehouseName: '新名'
    }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('更新成功')
  })

  it('handleDelete 触发 Modal.confirm', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const before = mocks.modalConfirmCallbacks.length
    vm.handleDelete({ id: 11, warehouseName: 'X' })
    expect(mocks.modalConfirmCallbacks.length).toBeGreaterThan(before)
  })

  it('handleDelete onOk 调用 deleteWarehouse', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 13, warehouseName: 'X' })
    const last = mocks.modalConfirmCallbacks.at(-1)
    expect(last?.onOk).toBeTypeOf('function')
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteWarehouse).toHaveBeenCalledWith(13)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('handleTableChange 更新分页并重新加载', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const before = apiMocks.pageWarehouses.mock.calls.length
    vm.handleTableChange({ current: 2, pageSize: 20 })
    await flushPromises()
    expect(vm.pagination.current).toBe(2)
    expect(vm.pagination.pageSize).toBe(20)
    expect(apiMocks.pageWarehouses.mock.calls.length).toBeGreaterThan(before)
  })

  it('loadData 异常时不抛错', async () => {
    apiMocks.pageWarehouses.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.table-card').exists()).toBe(true)
  })

  it('formatSafetyStock 解析 JSON 为可读文本', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.formatSafetyStock('{"1001":5,"1002":3}')).toBe('型号1001: 5；型号1002: 3')
    expect(vm.formatSafetyStock('')).toBe('—')
    expect(vm.formatSafetyStock(undefined)).toBe('—')
    expect(vm.formatSafetyStock('{}')).toBe('—')
    // 非法 JSON 退回原值
    expect(vm.formatSafetyStock('raw')).toBe('raw')
  })
})
