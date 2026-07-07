/**
 * system/dict 视图单元测试（Task 2.7）
 *
 * 覆盖：
 *   - 渲染（标题、字典类型/字典数据双表）
 *   - onMounted 加载 pageDictTypes
 *   - 字典类型 CRUD：openTypeCreate / openTypeEdit / handleTypeSubmit / handleTypeDelete
 *   - 字典数据 CRUD：openData / openDataCreate / openDataEdit / handleDataSubmit / handleDataDelete
 *   - 校验（dictCode/dictName 必填、dictLabel/dictValue 必填）
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import DictView from '../dict.vue'

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

/* ============ Mock @/api/system ============ */
const apiMocks = vi.hoisted(() => ({
  pageDictTypes: vi.fn(),
  createDictType: vi.fn(),
  updateDictType: vi.fn(),
  deleteDictType: vi.fn(),
  pageDictData: vi.fn(),
  createDictData: vi.fn(),
  updateDictData: vi.fn(),
  deleteDictData: vi.fn()
}))

vi.mock('@/api/system', () => apiMocks)

describe('system dict view', () => {
  const mockTypePage = {
    records: [
      {
        id: 1,
        dictCode: 'project_status',
        dictName: '项目状态',
        description: 'd',
        status: 1,
        createdAt: '2026-07-01'
      }
    ],
    total: 1
  }

  const mockDataPage = {
    records: [
      {
        id: 10,
        dictTypeId: 1,
        dictLabel: '进行中',
        dictValue: 'EXECUTE',
        sort: 0,
        isDefault: 1,
        status: 1,
        remark: 'r'
      }
    ],
    total: 1
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.pageDictTypes.mockResolvedValue(mockTypePage)
    apiMocks.createDictType.mockResolvedValue(2)
    apiMocks.updateDictType.mockResolvedValue(undefined)
    apiMocks.deleteDictType.mockResolvedValue(undefined)
    apiMocks.pageDictData.mockResolvedValue(mockDataPage)
    apiMocks.createDictData.mockResolvedValue(11)
    apiMocks.updateDictData.mockResolvedValue(undefined)
    apiMocks.deleteDictData.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(DictView, {})
  }

  it('渲染标题与双表', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('数据字典')
    expect(wrapper.text()).toContain('字典类型')
    expect(wrapper.text()).toContain('字典数据')
  })

  it('onMounted 调用 pageDictTypes', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(apiMocks.pageDictTypes).toHaveBeenCalledTimes(1)
    const vm = wrapper.vm as any
    expect(vm.typeData.length).toBe(1)
    expect(vm.typePagination.total).toBe(1)
  })

  it('openTypeCreate 重置 typeForm', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openTypeCreate()
    expect(vm.typeVisible).toBe(true)
    expect(vm.typeIsEdit).toBe(false)
    expect(vm.typeForm.dictCode).toBe('')
    expect(vm.typeForm.status).toBe(1)
  })

  it('openTypeEdit 加载记录到 typeForm', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openTypeEdit({ id: 1, dictCode: 'project_status', dictName: '项目状态', status: 1, description: 'd' })
    expect(vm.typeVisible).toBe(true)
    expect(vm.typeIsEdit).toBe(true)
    expect(vm.typeForm.dictCode).toBe('project_status')
  })

  it('handleTypeSubmit 缺 dictCode 时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openTypeCreate()
    vm.typeForm.dictName = '名称'
    vm.typeForm.dictCode = ''
    await vm.handleTypeSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写字典编码和名称')
  })

  it('handleTypeSubmit 新建调用 createDictType', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openTypeCreate()
    vm.typeForm.dictCode = 'new_dict'
    vm.typeForm.dictName = '新字典'
    await vm.handleTypeSubmit()
    await flushPromises()
    expect(apiMocks.createDictType).toHaveBeenCalled()
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
  })

  it('handleTypeSubmit 编辑调用 updateDictType', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openTypeEdit({ id: 1, dictCode: 'project_status', dictName: '旧', status: 1, description: '' })
    vm.typeForm.dictName = '新名称'
    await vm.handleTypeSubmit()
    await flushPromises()
    expect(apiMocks.updateDictType).toHaveBeenCalledWith(1, expect.objectContaining({ dictName: '新名称' }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('更新成功')
  })

  it('handleTypeDelete 触发 Modal.confirm onOk 调用 deleteDictType', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleTypeDelete({ id: 1, dictName: '项目状态' })
    expect(mocks.modalConfirmCallbacks.length).toBe(1)
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteDictType).toHaveBeenCalledWith(1)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('openData 设置 currentType 并加载字典数据', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openData({ id: 1, dictName: '项目状态' })
    await flushPromises()
    expect(vm.currentType.id).toBe(1)
    expect(apiMocks.pageDictData).toHaveBeenCalledWith(expect.objectContaining({ dictTypeId: 1 }))
    expect(vm.dataData.length).toBe(1)
  })

  it('openDataCreate 在 currentType 为空时直接返回', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openDataCreate()
    expect(vm.dataVisible).toBe(false)
  })

  it('openDataCreate 在 currentType 存在时打开弹窗', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openData({ id: 1, dictName: '项目状态' })
    await flushPromises()
    vm.openDataCreate()
    expect(vm.dataVisible).toBe(true)
    expect(vm.dataIsEdit).toBe(false)
  })

  it('openDataEdit 加载记录到 dataForm', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openDataEdit({ id: 10, dictLabel: '进行中', dictValue: 'EXECUTE', sort: 0, isDefault: 1, status: 1, remark: '' })
    expect(vm.dataVisible).toBe(true)
    expect(vm.dataIsEdit).toBe(true)
    expect(vm.dataForm.dictLabel).toBe('进行中')
  })

  it('handleDataSubmit 缺 dictLabel 时 warning', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.currentType = { id: 1 }
    vm.openDataCreate()
    vm.dataForm.dictValue = 'X'
    vm.dataForm.dictLabel = ''
    await vm.handleDataSubmit()
    await flushPromises()
    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写标签和值')
  })

  it('handleDataSubmit 新建调用 createDictData', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openData({ id: 1, dictName: '项目状态' })
    await flushPromises()
    vm.openDataCreate()
    vm.dataForm.dictLabel = '新标签'
    vm.dataForm.dictValue = 'NEW'
    await vm.handleDataSubmit()
    await flushPromises()
    expect(apiMocks.createDictData).toHaveBeenCalledWith(expect.objectContaining({ dictTypeId: 1, dictLabel: '新标签' }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('创建成功')
  })

  it('handleDataSubmit 编辑调用 updateDictData', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openData({ id: 1, dictName: '项目状态' })
    await flushPromises()
    vm.openDataEdit({ id: 10, dictLabel: '进行中', dictValue: 'EXECUTE', sort: 0, isDefault: 1, status: 1, remark: '' })
    vm.dataForm.dictLabel = '进行中改'
    await vm.handleDataSubmit()
    await flushPromises()
    expect(apiMocks.updateDictData).toHaveBeenCalledWith(10, expect.objectContaining({ dictLabel: '进行中改' }))
    expect(mocks.messageSuccess).toHaveBeenCalledWith('更新成功')
  })

  it('handleDataDelete 触发 Modal.confirm onOk 调用 deleteDictData', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDataDelete({ id: 10, dictLabel: '进行中' })
    expect(mocks.modalConfirmCallbacks.length).toBe(1)
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.deleteDictData).toHaveBeenCalledWith(10)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('删除成功')
  })

  it('pageDictTypes 异常时不抛错', async () => {
    apiMocks.pageDictTypes.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.typeData).toEqual([])
    expect(vm.typeLoading).toBe(false)
  })

  it('loadData 在 currentType 为空时直接返回', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.currentType = null
    vi.clearAllMocks()
    await vm.loadData()
    expect(apiMocks.pageDictData).not.toHaveBeenCalled()
  })
})
