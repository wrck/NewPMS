/**
 * spare 视图单元测试（Task E2.1）
 *
 * 覆盖 CRUD + 领用/归还/返修/入库操作 + 流水查询抽屉：
 *   - 渲染（标题、表格、搜索表单、按钮）
 *   - onMounted 加载列表
 *   - 搜索 / 重置
 *   - 新增弹窗：openCreate / handleSubmit
 *   - 编辑：openEdit
 *   - 删除：handleDelete -> Modal.confirm
 *   - 领用/归还/返修/入库：openAction / handleAction
 *   - 流水查询：openLogs
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import Spare from '../spare.vue'

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
  pageSpareParts: vi.fn(),
  createSparePart: vi.fn(),
  updateSparePart: vi.fn(),
  deleteSparePart: vi.fn(),
  sparePartAction: vi.fn(),
  listSparePartLogs: vi.fn()
}))

vi.mock('@/api/device', () => ({
  pageSpareParts: apiMocks.pageSpareParts,
  createSparePart: apiMocks.createSparePart,
  updateSparePart: apiMocks.updateSparePart,
  deleteSparePart: apiMocks.deleteSparePart,
  sparePartAction: apiMocks.sparePartAction,
  listSparePartLogs: apiMocks.listSparePartLogs
}))

describe('spare view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.pageSpareParts.mockResolvedValue({
      records: [
        {
          id: 1,
          partCode: 'P-001',
          partName: '路由器备件',
          modelId: 100,
          modelName: '型号X',
          category: 'ROUTER',
          unit: '个',
          stockQty: 10,
          safetyStockQty: 5,
          warehouseId: 1,
          warehouseName: '北京仓',
          status: 'IN_STOCK',
          remark: ''
        }
      ],
      total: 1
    })
    apiMocks.createSparePart.mockResolvedValue(2)
    apiMocks.updateSparePart.mockResolvedValue(undefined)
    apiMocks.deleteSparePart.mockResolvedValue(undefined)
    apiMocks.sparePartAction.mockResolvedValue(undefined)
    apiMocks.listSparePartLogs.mockResolvedValue([
      {
        id: 1,
        sparePartId: 1,
        actionType: 'IN',
        quantity: 10,
        projectId: undefined,
        projectName: undefined,
        remark: '初始入库',
        createTime: '2026-01-01 10:00:00'
      }
    ])
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Spare, {})
  }

  it('渲染标题与表格容器', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.search-card').exists()).toBe(true)
    expect(wrapper.find('.table-card').exists()).toBe(true)
    expect(wrapper.text()).toContain('刷新')
    expect(wrapper.text()).toContain('新备件')
  })

  it('onMounted 调用 pageSpareParts 加载列表', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(apiMocks.pageSpareParts).toHaveBeenCalledTimes(1)
    const args = apiMocks.pageSpareParts.mock.calls[0][0]
    expect(args.page).toBe(1)
    expect(args.size).toBe(10)
  })

  it('加载后渲染表格数据行', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('路由器备件')
    expect(wrapper.text()).toContain('P-001')
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
    const last = apiMocks.pageSpareParts.mock.calls.at(-1)[0]
    expect(last.keyword).toBe('kw')
    expect(last.page).toBe(1)
  })

  it('handleReset 清空 query 并重新加载', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.query.keyword = 'dirty'
    vm.query.category = 'ROUTER'
    vm.query.status = 'IN_STOCK'
    await vm.handleReset()
    await flushPromises()
    expect(vm.query.keyword).toBe('')
    expect(vm.query.category).toBeUndefined()
    expect(vm.query.status).toBeUndefined()
  })

  it('openCreate 重置 formData 并打开弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    await nextTick()
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(false)
    expect(vm.formData.partCode).toBe('')
    expect(vm.formData.partName).toBe('')
    expect(vm.formData.category).toBe('ROUTER')
  })

  it('openEdit 加载行数据到 formData', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const row = {
      id: 5,
      partCode: 'P-5',
      partName: '交换机',
      category: 'SWITCH',
      stockQty: 8,
      safetyStockQty: 3
    }
    vm.openEdit(row)
    await nextTick()
    expect(vm.formVisible).toBe(true)
    expect(vm.isEdit).toBe(true)
    expect(vm.formData.id).toBe(5)
    expect(vm.formData.partCode).toBe('P-5')
    expect(vm.formData.partName).toBe('交换机')
  })

  it('handleSubmit 缺少编码与名称时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.formData.partCode = ''
    vm.formData.partName = ''
    await vm.handleSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写备件编码和名称')
  })

  it('handleSubmit 新增时调用 createSparePart', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openCreate()
    vm.formData.partCode = 'P-NEW'
    vm.formData.partName = '新备件'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.createSparePart).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
    expect(vm.formVisible).toBe(false)
  })

  it('handleSubmit 编辑时调用 updateSparePart', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openEdit({ id: 7, partCode: 'P-7', partName: '旧名' })
    vm.formData.partName = '新名'
    await vm.handleSubmit()
    await flushPromises()
    expect(apiMocks.updateSparePart).toHaveBeenCalledWith(7, expect.objectContaining({
      partCode: 'P-7',
      partName: '新名'
    }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('更新成功')
  })

  it('handleDelete 触发 Modal.confirm', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const before = mocks.modalConfirmCallbacks.length
    vm.handleDelete({ id: 11, partName: 'X' })
    expect(mocks.modalConfirmCallbacks.length).toBeGreaterThan(before)
  })

  it('handleDelete onOk 调用 deleteSparePart', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDelete({ id: 13, partName: 'X' })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteSparePart).toHaveBeenCalledWith(13)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('openAction 打开操作弹窗并初始化 form', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const row = { id: 1, partCode: 'P-1', partName: 'X', stockQty: 10 }
    vm.openAction(row, 'OUT')
    expect(vm.actionVisible).toBe(true)
    expect(vm.actionType).toBe('OUT')
    expect(vm.actionRow).toEqual(row)
    expect(vm.actionForm.quantity).toBe(1)
  })

  it('handleAction 数量非法时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openAction({ id: 1, partName: 'X', stockQty: 10 }, 'OUT')
    vm.actionForm.quantity = 0
    await vm.handleAction()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写有效数量')
    expect(apiMocks.sparePartAction).not.toHaveBeenCalled()
  })

  it('handleAction 领用时库存不足 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openAction({ id: 1, partName: 'X', stockQty: 5 }, 'OUT')
    vm.actionForm.quantity = 10
    await vm.handleAction()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('库存不足，当前库存 5')
    expect(apiMocks.sparePartAction).not.toHaveBeenCalled()
  })

  it('handleAction 调用 sparePartAction', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openAction({ id: 1, partName: 'X', stockQty: 10 }, 'OUT')
    vm.actionForm.quantity = 2
    vm.actionForm.projectId = 100
    await vm.handleAction()
    await flushPromises()
    expect(apiMocks.sparePartAction).toHaveBeenCalledWith(expect.objectContaining({
      sparePartId: 1,
      actionType: 'OUT',
      quantity: 2,
      projectId: 100
    }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('领用成功')
    expect(vm.actionVisible).toBe(false)
  })

  it('openLogs 调用 listSparePartLogs 并打开抽屉', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openLogs({ id: 1, partName: 'X' })
    await flushPromises()
    expect(apiMocks.listSparePartLogs).toHaveBeenCalledWith({ sparePartId: 1 })
    expect(vm.logVisible).toBe(true)
    expect(vm.logDataSource.length).toBeGreaterThan(0)
  })

  it('openLogs 异常时不抛错且清空列表', async () => {
    apiMocks.listSparePartLogs.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await expect(vm.openLogs({ id: 1, partName: 'X' })).resolves.not.toThrow()
    await flushPromises()
    expect(vm.logDataSource).toEqual([])
  })

  it('handleTableChange 更新分页并重新加载', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const before = apiMocks.pageSpareParts.mock.calls.length
    vm.handleTableChange({ current: 2, pageSize: 20 })
    await flushPromises()
    expect(vm.pagination.current).toBe(2)
    expect(vm.pagination.pageSize).toBe(20)
    expect(apiMocks.pageSpareParts.mock.calls.length).toBeGreaterThan(before)
  })
})
