/**
 * finance/profit 视图单元测试（Task 2.3）
 *
 * 覆盖：
 *   - 渲染（标题、表格、维度切换）
 *   - onMounted 加载 listProjectProfit
 *   - 维度切换：project/customer/region/productLine
 *   - summary 计算汇总
 *   - formatMoney / formatPercent / profitColor 工具函数
 *   - handleExport 空数据时 warning
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Profit from '../profit.vue'

/* ============ Mock ant-design-vue message ============ */
const mocks = vi.hoisted(() => ({
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
  messageWarning: vi.fn()
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
    }
  }
})

/* ============ Mock @/api/finance ============ */
const apiMocks = vi.hoisted(() => ({
  listProjectProfit: vi.fn(),
  listProfitByCustomer: vi.fn(),
  listProfitByRegion: vi.fn(),
  listProfitByProductLine: vi.fn()
}))

vi.mock('@/api/finance', () => apiMocks)

describe('finance profit view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    apiMocks.listProjectProfit.mockResolvedValue([
      { projectName: '项目A', revenue: 100000, selfCost: 50000, agentCost: 20000, totalCost: 70000, profit: 30000, profitMargin: 30, selfCostRatio: 71.4, agentCostRatio: 28.6 }
    ])
    apiMocks.listProfitByCustomer.mockResolvedValue([
      { projectName: '客户A', revenue: 200000, selfCost: 80000, agentCost: 40000, totalCost: 120000, profit: 80000, profitMargin: 40, selfCostRatio: 66.7, agentCostRatio: 33.3 }
    ])
    apiMocks.listProfitByRegion.mockResolvedValue([
      { projectName: '华北', revenue: 500000, selfCost: 200000, agentCost: 100000, totalCost: 300000, profit: 200000, profitMargin: 40, selfCostRatio: 66.7, agentCostRatio: 33.3 }
    ])
    apiMocks.listProfitByProductLine.mockResolvedValue([
      { projectName: '网络设备', revenue: 800000, selfCost: 400000, agentCost: 200000, totalCost: 600000, profit: 200000, profitMargin: 25, selfCostRatio: 66.7, agentCostRatio: 33.3 }
    ])
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(Profit, {})
  }

  it('渲染标题与表格', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('利润分析')
  })

  it('onMounted 调用 listProjectProfit', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.listProjectProfit).toHaveBeenCalledTimes(1)
  })

  it('渲染项目数据', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('项目A')
  })

  it('维度切换到 customer 调用 listProfitByCustomer', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDimensionChange('customer')
    await flushPromises()
    expect(apiMocks.listProfitByCustomer).toHaveBeenCalled()
    expect(vm.dimension).toBe('customer')
  })

  it('维度切换到 region 调用 listProfitByRegion', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDimensionChange('region')
    await flushPromises()
    expect(apiMocks.listProfitByRegion).toHaveBeenCalled()
  })

  it('维度切换到 productLine 调用 listProfitByProductLine', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleDimensionChange('productLine')
    await flushPromises()
    expect(apiMocks.listProfitByProductLine).toHaveBeenCalled()
  })

  it('summary 计算汇总正确', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.summary.totalRevenue).toBe(100000)
    expect(vm.summary.totalCost).toBe(70000)
    expect(vm.summary.totalProfit).toBe(30000)
    expect(vm.summary.avgMargin).toBe(30)
    expect(vm.summary.count).toBe(1)
  })

  it('formatMoney 格式化金额', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.formatMoney(1234.5)).toBe('1,234.50')
    expect(vm.formatMoney(undefined)).toBe('-')
    expect(vm.formatMoney(null)).toBe('-')
  })

  it('formatPercent 格式化百分比', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.formatPercent(30)).toBe('30%')
    expect(vm.formatPercent(undefined)).toBe('-')
    expect(vm.formatPercent(null)).toBe('-')
  })

  it('profitColor 根据正负返回颜色', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.profitColor(100)).toBe('success')
    expect(vm.profitColor(-50)).toBe('error')
    expect(vm.profitColor(undefined)).toBe('default')
    expect(vm.profitColor(null)).toBe('default')
  })

  it('handleExport 空数据时 warning', async () => {
    apiMocks.listProjectProfit.mockResolvedValueOnce([])
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.handleExport()
    expect(mocks.messageWarning).toHaveBeenCalledWith('暂无数据可导出')
  })

  it('handleExport 有数据时 success', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    // Mock document.createElement + URL.createObjectURL + appendChild/removeChild
    const originalCreateElement = document.createElement
    const originalURL = global.URL.createObjectURL
    const originalAppendChild = document.body.appendChild
    const originalRemoveChild = document.body.removeChild
    document.createElement = vi.fn().mockReturnValue({ click: vi.fn(), href: '', download: '' }) as any
    global.URL.createObjectURL = vi.fn().mockReturnValue('blob:url') as any
    global.URL.revokeObjectURL = vi.fn()
    document.body.appendChild = vi.fn() as any
    document.body.removeChild = vi.fn() as any
    try {
      vm.handleExport()
      expect(mocks.messageSuccess).toHaveBeenCalledWith('已导出 CSV')
    } finally {
      document.createElement = originalCreateElement
      global.URL.createObjectURL = originalURL
      document.body.appendChild = originalAppendChild
      document.body.removeChild = originalRemoveChild
    }
  })

  it('listProjectProfit 异常时显示错误消息', async () => {
    apiMocks.listProjectProfit.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    expect(mocks.messageError).toHaveBeenCalled()
    const vm = wrapper.vm as any
    expect(vm.dataSource).toEqual([])
  })

  it('dimensionLabel 包含 4 种维度', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(Object.keys(vm.dimensionLabel).length).toBe(4)
    expect(vm.dimensionLabel.project).toBe('项目维度')
    expect(vm.dimensionLabel.customer).toBe('客户维度')
    expect(vm.dimensionLabel.region).toBe('区域维度')
    expect(vm.dimensionLabel.productLine).toBe('产品线维度')
  })
})
