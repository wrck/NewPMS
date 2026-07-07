/**
 * report/project 视图单元测试（Task 2.8）
 *
 * 覆盖：
 *   - 渲染（标题、筛选表单、明细表格）
 *   - onMounted 调用 getProjectReport
 *   - handleSearch / handleReset
 *   - statusPieData / monthlyTrend / pmBarData computed
 *   - detailData computed
 *   - 异常兜底
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ProjectView from '../project.vue'

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
  getProjectReport: vi.fn()
}))

vi.mock('@/api/report', () => apiMocks)

/* ============ Stub 图表与基础组件 ============ */
const stubs = {
  PieChart: { template: '<div class="stub-pie-chart" />' },
  LineChart: { template: '<div class="stub-line-chart" />' },
  BarChart: { template: '<div class="stub-bar-chart" />' },
  ImportExport: { template: '<div class="stub-import-export" />' },
  PageContainer: { template: '<div><h1>{{ title }}</h1><slot /><slot name="extra" /></div>', props: ['title', 'description'] },
  EmptyState: { template: '<div class="stub-empty" />' },
  StatusTag: { template: '<span class="stub-status-tag"><slot /></span>' },
  ProgressBar: { template: '<div class="stub-progress-bar" />' }
}

describe('report project view', () => {
  const mockReportData = {
    summary: { total: 10, completed: 5, ongoing: 3, overdue: 2, avgProgress: 65 },
    byStatus: [
      { status: 'EXECUTE', statusName: '执行中', count: 3 },
      { status: 'CLOSE', statusName: '已结项', count: 5 },
      { status: 'PLAN', statusName: '规划中', count: 2 }
    ],
    byProductLine: [{ productLine: 'P1', count: 5 }],
    byRegion: [{ region: '华东', count: 4 }],
    byPm: [
      { pmId: 1, pmName: '张三', total: 4, completed: 2, overdue: 1 },
      { pmId: 2, pmName: '李四', total: 3, completed: 1, overdue: 0 }
    ],
    detail: [
      {
        id: 1,
        projectCode: 'P001',
        projectName: '项目A',
        status: 'EXECUTE',
        progressPct: 50,
        pmName: '张三',
        plannedStart: '2026-05-01',
        plannedEnd: '2026-08-01',
        actualEnd: undefined,
        overdue: false
      },
      {
        id: 2,
        projectCode: 'P002',
        projectName: '项目B',
        status: 'CLOSE',
        progressPct: 100,
        pmName: '李四',
        plannedStart: '2026-04-01',
        plannedEnd: '2026-06-01',
        actualEnd: '2026-06-05',
        overdue: false
      }
    ]
  }

  beforeEach(() => {
    vi.clearAllMocks()
    apiMocks.getProjectReport.mockResolvedValue(mockReportData)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountView() {
    return mount(ProjectView, { global: { stubs } })
  }

  it('渲染标题与筛选区', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('项目报表')
    // ant-design-vue Button 在两个中文字符之间插入空格：查询 -> 查 询
    expect(wrapper.text()).toMatch(/查\s*询/)
    expect(wrapper.text()).toMatch(/重\s*置/)
  })

  it('onMounted 调用 getProjectReport', async () => {
    mountView()
    await flushPromises()
    expect(apiMocks.getProjectReport).toHaveBeenCalledTimes(1)
  })

  it('statusPieData computed 转换 byStatus', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.statusPieData.length).toBe(3)
    expect(vm.statusPieData[0]).toEqual({ name: '执行中', value: 3 })
    expect(vm.statusPieData[1]).toEqual({ name: '已结项', value: 5 })
  })

  it('monthlyTrend computed 从 detail 聚合月度数据', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    // 2 个明细：plannedStart 2026-05 / 2026-04，actualEnd 仅 2026-06
    expect(vm.monthlyTrend.xAxis).toEqual(expect.arrayContaining(['2026-04', '2026-05', '2026-06']))
    const newSeries = vm.monthlyTrend.series.find((s: any) => s.name === '新增')
    const completedSeries = vm.monthlyTrend.series.find((s: any) => s.name === '完成')
    expect(newSeries).toBeDefined()
    expect(completedSeries).toBeDefined()
    // 2026-04 新增 1，2026-05 新增 1，2026-06 新增 0
    const aprIdx = vm.monthlyTrend.xAxis.indexOf('2026-04')
    const mayIdx = vm.monthlyTrend.xAxis.indexOf('2026-05')
    expect(newSeries.data[aprIdx]).toBe(1)
    expect(newSeries.data[mayIdx]).toBe(1)
    // 2026-06 完成 1
    const junIdx = vm.monthlyTrend.xAxis.indexOf('2026-06')
    expect(completedSeries.data[junIdx]).toBe(1)
  })

  it('pmBarData computed 返回 PM 业绩数据', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.pmBarData.xAxis).toEqual(['张三', '李四'])
    expect(vm.pmBarData.series.length).toBe(3)
    expect(vm.pmBarData.series[0].name).toBe('负责项目')
    expect(vm.pmBarData.series[0].data).toEqual([4, 3])
    expect(vm.pmBarData.series[1].name).toBe('已完成')
    expect(vm.pmBarData.series[1].data).toEqual([2, 1])
    expect(vm.pmBarData.series[2].name).toBe('超期')
    expect(vm.pmBarData.series[2].data).toEqual([1, 0])
  })

  it('detailData computed 返回明细列表', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.detailData.length).toBe(2)
    expect(vm.detailData[0].projectCode).toBe('P001')
  })

  it('handleSearch 调用 getProjectReport', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.query.status = 'EXECUTE'
    await vm.handleSearch()
    await flushPromises()
    expect(apiMocks.getProjectReport).toHaveBeenCalledWith(expect.objectContaining({ status: 'EXECUTE' }))
  })

  it('handleReset 清空 query 并重新加载', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    vm.query.status = 'EXECUTE'
    vm.query.pmId = 1
    vm.query.region = '华东'
    await vm.handleReset()
    await flushPromises()
    expect(vm.query.status).toBeUndefined()
    expect(vm.query.pmId).toBeUndefined()
    expect(vm.query.region).toBe('')
    expect(apiMocks.getProjectReport).toHaveBeenCalled()
  })

  it('getProjectReport 异常时 message.error', async () => {
    apiMocks.getProjectReport.mockRejectedValueOnce(new Error('network'))
    const wrapper = mountView()
    await flushPromises()
    expect(mocks.messageError).toHaveBeenCalledWith('加载项目报表数据失败')
    const vm = wrapper.vm as any
    expect(vm.data).toBeNull()
    expect(vm.loading).toBe(false)
  })

  it('loadData 手动刷新调用 getProjectReport', async () => {
    const wrapper = mountView()
    await flushPromises()
    vi.clearAllMocks()
    apiMocks.getProjectReport.mockResolvedValue(mockReportData)
    await (wrapper.vm as any).loadData()
    await flushPromises()
    expect(apiMocks.getProjectReport).toHaveBeenCalledTimes(1)
  })

  it('monthlyTrend 在 detail 为空时返回空结构', async () => {
    apiMocks.getProjectReport.mockResolvedValueOnce({
      ...mockReportData,
      detail: []
    })
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.monthlyTrend.xAxis).toEqual([])
    expect(vm.monthlyTrend.series).toEqual([])
  })

  it('pmBarData 在 byPm 为空时返回空结构', async () => {
    apiMocks.getProjectReport.mockResolvedValueOnce({
      ...mockReportData,
      byPm: []
    })
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as any
    expect(vm.pmBarData.xAxis).toEqual([])
    expect(vm.pmBarData.series).toEqual([])
  })
})
