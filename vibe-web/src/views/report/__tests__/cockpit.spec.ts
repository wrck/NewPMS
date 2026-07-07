/**
 * report/cockpit 视图单元测试（Task 2.8）
 *
 * 覆盖：
 *   - 渲染（标题、KPI 卡片、风险项目列表）
 *   - onMounted 调用 getCockpit + getProjectReport
 *   - kpis computed 计算 4 个 KPI 卡片
 *   - phaseChartData / trendXAxis / trendSeries computed
 *   - deviceGaugeValue computed
 *   - riskList / riskLevelLabel / riskTypeLabel 映射
 *   - 异常兜底（getCockpit 失败 message.error）
 *   - regionData 来自 getProjectReport byRegion
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import CockpitView from '../cockpit.vue'

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

/* ============ Mock @/api/report ============ */
const apiMocks = vi.hoisted(() => ({
  getCockpit: vi.fn(),
  getProjectReport: vi.fn()
}))

vi.mock('@/api/report', () => apiMocks)

/* ============ Stub 图表与基础组件 ============ */
const stubs = {
  PieChart: { template: '<div class="stub-pie-chart" />' },
  StackedChart: { template: '<div class="stub-stacked-chart" />' },
  GaugeChart: { template: '<div class="stub-gauge-chart" />' },
  MapChart: { template: '<div class="stub-map-chart" />' },
  PageContainer: { template: '<div><h1>{{ title }}</h1><slot /><slot name="extra" /></div>', props: ['title', 'description'] },
  EmptyState: { template: '<div class="stub-empty" />' },
  StatusTag: { template: '<span class="stub-status-tag"><slot /></span>' }
}

describe('report cockpit view', () => {
  const mockCockpitData = {
    kpi: {
      ongoingProjects: 10,
      riskProjects: 2,
      overdueProjects: 1,
      monthNewProjects: 3,
      monthClosedProjects: 1,
      engineerUtilization: 0.75,
      deviceArrivalRate: 80,
      acceptanceCompletionRate: 60
    },
    phaseDistribution: [
      { phase: 'PLAN', phaseName: '规划', count: 3 },
      { phase: 'EXECUTE', phaseName: '执行', count: 5 },
      { phase: 'ACCEPT', phaseName: '验收', count: 2 }
    ],
    projectTrend: [
      { month: '2026-06', newCount: 3, closedCount: 1, ongoingCount: 10 },
      { month: '2026-07', newCount: 2, closedCount: 1, ongoingCount: 11 }
    ],
    todoList: [],
    riskWarnings: [
      {
        id: 1,
        projectId: 1,
        projectName: '项目A',
        riskType: 'PROGRESS',
        description: '进度滞后',
        level: 'HIGH',
        detectedAt: '2026-07-01 10:00'
      },
      {
        id: 2,
        projectId: 2,
        projectName: '项目B',
        riskType: 'DEVICE',
        description: '设备未到',
        level: 'MEDIUM',
        detectedAt: '2026-07-02 11:00'
      }
    ],
    recentActivities: []
  }

  const mockProjectReport = {
    summary: { total: 10, completed: 5, ongoing: 3, overdue: 1, avgProgress: 60 },
    byStatus: [],
    byProductLine: [],
    byRegion: [
      { region: '华东', count: 5 },
      { region: '华北', count: 3 }
    ],
    byPm: [],
    detail: []
  }

  beforeEach(() => {
    vi.clearAllMocks()
    apiMocks.getCockpit.mockResolvedValue(mockCockpitData)
    apiMocks.getProjectReport.mockResolvedValue(mockProjectReport)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(CockpitView, { global: { stubs } })
  }

  it('渲染标题与 KPI 卡片', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('管理驾驶舱')
    expect(wrapper.text()).toContain('在建项目')
    expect(wrapper.text()).toContain('风险项目')
    expect(wrapper.text()).toContain('设备到货率')
    expect(wrapper.text()).toContain('验收完成率')
  })

  it('onMounted 调用 getCockpit 与 getProjectReport', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.getCockpit).toHaveBeenCalledTimes(1)
    expect(apiMocks.getProjectReport).toHaveBeenCalledTimes(1)
  })

  it('kpis computed 返回 4 个 KPI 卡片', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.kpis.length).toBe(4)
    expect(vm.kpis[0]).toEqual(expect.objectContaining({ key: 'ongoing', value: 10, unit: '个' }))
    expect(vm.kpis[1]).toEqual(expect.objectContaining({ key: 'risk', value: 2 }))
    expect(vm.kpis[2]).toEqual(expect.objectContaining({ key: 'device', value: 80, unit: '%' }))
    expect(vm.kpis[3]).toEqual(expect.objectContaining({ key: 'acceptance', value: 60 }))
  })

  it('kpis ongoingTrend 计算环比', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    // ongoingTrend = ((3 - 1) / 10) * 100 = 20
    expect(vm.kpis[0].trend).toBe(20)
  })

  it('phaseChartData computed 转换 phaseDistribution', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.phaseChartData.length).toBe(3)
    expect(vm.phaseChartData[0]).toEqual({ name: '规划', value: 3 })
    expect(vm.phaseChartData[1]).toEqual({ name: '执行', value: 5 })
  })

  it('trendXAxis computed 返回月份列表', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.trendXAxis).toEqual(['2026-06', '2026-07'])
  })

  it('trendSeries computed 返回 3 个系列', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.trendSeries.length).toBe(3)
    expect(vm.trendSeries[0].name).toBe('新增')
    expect(vm.trendSeries[0].data).toEqual([3, 2])
    expect(vm.trendSeries[1].name).toBe('结项')
    expect(vm.trendSeries[2].name).toBe('在建')
  })

  it('deviceGaugeValue computed 取 deviceArrivalRate', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.deviceGaugeValue).toBe(80)
  })

  it('riskList computed 返回风险项目', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.riskList.length).toBe(2)
    expect(vm.riskList[0].projectName).toBe('项目A')
  })

  it('riskLevelLabel 包含 LOW/MEDIUM/HIGH', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.riskLevelLabel.LOW).toBe('低')
    expect(vm.riskLevelLabel.MEDIUM).toBe('中')
    expect(vm.riskLevelLabel.HIGH).toBe('高')
  })

  it('riskTypeLabel 包含 5 种风险类型', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(Object.keys(vm.riskTypeLabel).length).toBe(5)
    expect(vm.riskTypeLabel.PROGRESS).toBe('进度')
    expect(vm.riskTypeLabel.DEVICE).toBe('设备')
    expect(vm.riskTypeLabel.RESOURCE).toBe('资源')
    expect(vm.riskTypeLabel.AGENT).toBe('代理商')
    expect(vm.riskTypeLabel.OTHER).toBe('其他')
  })

  it('regionData 来自 getProjectReport byRegion', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.regionData.length).toBe(2)
    expect(vm.regionData[0]).toEqual({ name: '华东', value: 5 })
    expect(vm.regionData[1]).toEqual({ name: '华北', value: 3 })
  })

  it('getCockpit 异常时 message.error', async () => {
    apiMocks.getCockpit.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    expect(mocks.messageError).toHaveBeenCalledWith('加载驾驶舱数据失败')
    const vm = wrapper.vm as any
    expect(vm.loading).toBe(false)
  })

  it('getProjectReport 异常时不影响主流程（catch 返回 null）', async () => {
    apiMocks.getProjectReport.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.data).not.toBeNull()
    expect(vm.regionData).toEqual([])
  })

  it('loadData 手动刷新调用 API', async () => {
    const wrapper = mountView()
    await flushPromises()
    vi.clearAllMocks()
    apiMocks.getCockpit.mockResolvedValue(mockCockpitData)
    apiMocks.getProjectReport.mockResolvedValue(mockProjectReport)
    await vm_call(wrapper, 'loadData')
    await flushPromises()
    expect(apiMocks.getCockpit).toHaveBeenCalledTimes(1)
    expect(apiMocks.getProjectReport).toHaveBeenCalledTimes(1)
  })
})

async function vm_call(wrapper: any, method: string) {
  const vm = wrapper.vm as any
  await vm[method]()
}
