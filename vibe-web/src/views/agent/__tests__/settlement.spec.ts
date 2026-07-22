/**
 * agent/settlement 视图单元测试（Task 2.2）
 *
 * 覆盖：
 *   - 渲染（标题、表格、搜索）
 *   - onMounted 加载 listWorkloads
 *   - 搜索 / 重置
 *   - openConfirm 加载 getTaskWorkload（数组）后回填
 *   - handleConfirm 调用 confirmWorkload(taskId, workloadId)
 *   - openReject -> Modal.confirm -> rejectWorkload(taskId, workloadId, remark)
 *   - formatMoney 工具函数
 *   - statusMap 包含 3 状态（SUBMITTED/CONFIRMED/REJECTED）
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Settlement from '../settlement.vue'

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

/* ============ Mock @/api/agent ============ */
const apiMocks = vi.hoisted(() => ({
  listWorkloads: vi.fn(),
  getTaskWorkload: vi.fn(),
  confirmWorkload: vi.fn(),
  rejectWorkload: vi.fn()
}))

vi.mock('@/api/agent', () => apiMocks)

describe('agent settlement view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.modalConfirmCallbacks.length = 0
    apiMocks.listWorkloads.mockResolvedValue({
      records: [
        {
          id: 1,
          taskId: 1,
          projectName: '项目A',
          agentCompanyName: '代理商A',
          manDays: 5,
          siteCount: 2,
          deviceCount: 10,
          status: 'SUBMITTED',
          confirmByName: '张三'
        }
      ],
      total: 1
    })
    apiMocks.getTaskWorkload.mockResolvedValue([
      {
        id: 1,
        taskId: 1,
        manDays: 5,
        siteCount: 2,
        deviceCount: 10,
        remark: '已有备注'
      }
    ])
    apiMocks.confirmWorkload.mockResolvedValue(undefined)
    apiMocks.rejectWorkload.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Settlement, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('结算管理')
    expect(wrapper.text()).toContain('项目A')
    expect(wrapper.text()).toContain('代理商A')
  })

  it('onMounted 调用 listWorkloads', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.listWorkloads).toHaveBeenCalledTimes(1)
  })

  it('handleSearch 重置页码为 1', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.pagination.current = 5
    vm.query.agentCompanyId = 100
    await vm.handleSearch()
    await flushPromises()
    expect(vm.pagination.current).toBe(1)
  })

  it('openConfirm 调用 getTaskWorkload 并回填 confirmForm', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openConfirm({ id: 1, taskId: 1, projectName: 'X' })
    await flushPromises()
    expect(apiMocks.getTaskWorkload).toHaveBeenCalledWith(1)
    expect(vm.confirmVisible).toBe(true)
    expect(vm.confirmForm.manDays).toBe(5)
    expect(vm.confirmForm.remark).toBe('已有备注')
  })

  it('handleConfirm 调用 confirmWorkload(taskId, workloadId)', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    await vm.openConfirm({ id: 1, taskId: 1 })
    await flushPromises()
    vm.confirmForm.manDays = 5
    await vm.handleConfirm()
    await flushPromises()
    expect(apiMocks.confirmWorkload).toHaveBeenCalledWith(1, 1)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('工作量已确认')
    expect(vm.confirmVisible).toBe(false)
  })

  it('openReject 触发 Modal.confirm', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    const before = mocks.modalConfirmCallbacks.length
    vm.openReject({ id: 2, taskId: 1 })
    expect(mocks.modalConfirmCallbacks.length).toBeGreaterThan(before)
  })

  it('openReject onOk 调用 rejectWorkload(taskId, workloadId, remark)', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.openReject({ id: 2, taskId: 1 })
    const last = mocks.modalConfirmCallbacks.at(-1)
    await last!.onOk!()
    await flushPromises()
    expect(apiMocks.rejectWorkload).toHaveBeenCalledWith(1, 2, '')
    expect(mocks.messageSuccess).toHaveBeenCalledWith('已驳回')
  })

  it('formatMoney 格式化金额', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.formatMoney(1234.5)).toBe('¥1,234.50')
    expect(vm.formatMoney(undefined)).toBe('-')
    expect(vm.formatMoney(null)).toBe('-')
  })

  it('statusMap 包含 3 种状态', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(Object.keys(vm.statusMap).length).toBe(3)
    expect(vm.statusMap.SUBMITTED.label).toBe('待确认')
    expect(vm.statusMap.CONFIRMED.label).toBe('已确认')
    expect(vm.statusMap.REJECTED.label).toBe('已驳回')
  })

  it('listWorkloads 异常时不抛错', async () => {
    apiMocks.listWorkloads.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.dataSource).toEqual([])
  })
})
