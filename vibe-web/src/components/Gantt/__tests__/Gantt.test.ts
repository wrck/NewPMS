/**
 * Gantt 组件单元测试（spec 阶段三 Task 18 - SubTask 18.7）
 *
 * 覆盖范围：
 *   - 组件挂载（gantt.init / gantt.parse 调用）
 *   - props.tasks / links 传递
 *   - viewMode 切换（emit view-mode-change）
 *   - readonly 模式（gantt.config.readonly 同步）
 *   - 工具栏按钮触发（zoomIn / zoomOut / expandAll / collapseAll）
 *   - 事件回调（onAfterTaskDrag -> task-drag / onTaskSelected -> task-select）
 *
 * 测试策略：
 *   - mock dhtmlx-gantt 模块（jsdom 无法完整渲染甘特图）
 *   - 复用全局 setup（Antd + Pinia + jsdom polyfill）
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import Gantt from '../index.vue'
import type { GanttTask, GanttLink } from '../types'

/* ============ Mock dhtmlx-gantt 模块 ============ */
// 收集事件处理器，便于测试触发回调
const handlers: Record<string, Array<(...args: any[]) => any>> = {}
// 模拟内部任务/链接存储
const store = {
  tasks: [] as any[],
  links: [] as any[]
}
// 记录 zoom 调用
const zoomCalls: string[] = []

vi.mock('dhtmlx-gantt', () => {
  const mockGantt = {
    config: {} as Record<string, any>,
    templates: {} as Record<string, any>,
    i18n: {
      setLocale: vi.fn()
    },
    plugins: vi.fn(),
    ext: {
      zoom: {
        init: vi.fn(),
        zoomIn: vi.fn(() => zoomCalls.push('zoomIn')),
        zoomOut: vi.fn(() => zoomCalls.push('zoomOut')),
        setLevel: vi.fn((level: string) => zoomCalls.push(`setLevel:${level}`)),
        getCurrentLevel: vi.fn(() => 2),
        getLevels: vi.fn(() => [
          { name: 'day' },
          { name: 'week' },
          { name: 'month' },
          { name: 'quarter' },
          { name: 'year' }
        ])
      }
    },
    init: vi.fn((container: HTMLElement) => {
      // 模拟 dhtmlx 在容器内创建结构
      container.innerHTML = '<div class="gantt_container"></div>'
    }),
    parse: vi.fn((data: any) => {
      store.tasks = Array.isArray(data) ? data : data?.data || []
      store.links = Array.isArray(data) ? [] : data?.links || []
    }),
    clearAll: vi.fn(() => {
      store.tasks = []
      store.links = []
    }),
    render: vi.fn(),
    setSizes: vi.fn(),
    eachTask: vi.fn((cb: (t: any) => void) => {
      store.tasks.forEach((t) => cb(t))
    }),
    getTask: vi.fn((id: string | number) => store.tasks.find((t) => t.id === id)),
    attachEvent: vi.fn((name: string, handler: any) => {
      handlers[name] = handlers[name] || []
      handlers[name].push(handler)
      return `evt-${name}-${handlers[name].length}`
    }),
    detachEvent: vi.fn(),
    getTaskCount: () => store.tasks.length
  }
  // 静态导出 gantt
  return { gantt: mockGantt }
})

// 引入 mock 后的 gantt，便于断言
import { gantt } from 'dhtmlx-gantt'
const mockGantt = gantt as any

/* ============ 测试数据 ============ */
const sampleTasks: GanttTask[] = [
  {
    id: 1,
    text: '需求分析',
    start_date: '2026-07-01',
    duration: 5,
    progress: 0.6,
    status: 'in_progress',
    priority: 'P0',
    assignee: '张三'
  },
  {
    id: 2,
    text: '架构设计',
    start_date: '2026-07-06',
    duration: 4,
    progress: 0.2,
    status: 'pending',
    priority: 'P1',
    parent: 1,
    assignee: '李四'
  },
  {
    id: 3,
    text: '里程碑：设计评审',
    start_date: '2026-07-10',
    duration: 0,
    type: 'milestone',
    priority: 'P0'
  }
]

const sampleLinks: GanttLink[] = [
  { id: 1, source: 1, target: 2, type: '0' },
  { id: 2, source: 2, target: 3, type: '0' }
]

/* ============ 工具：构建 wrapper ============ */
function makeWrapper(props: Record<string, any> = {}) {
  return mount(Gantt, {
    props: {
      tasks: sampleTasks,
      links: sampleLinks,
      viewMode: 'month',
      ...props
    }
  })
}

/* ============ 触发 gantt 事件回调 ============ */
function fireEvent(name: string, ...args: any[]): any {
  const list = handlers[name]
  if (!list || list.length === 0) {
    throw new Error(`No handler registered for event: ${name}`)
  }
  let result: any
  for (const h of list) {
    result = h(...args)
  }
  return result
}

/* ============ 测试用例 ============ */
describe('Gantt 组件', () => {
  beforeEach(() => {
    // 重置 mock 调用记录
    vi.clearAllMocks()
    Object.keys(handlers).forEach((k) => delete handlers[k])
    store.tasks = []
    store.links = []
    zoomCalls.length = 0
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('应正确挂载并初始化 dhtmlx-gantt', async () => {
    const wrapper = makeWrapper()
    await flushPromises()
    await nextTick()

    // gantt.init 应以容器元素为参数被调用
    expect(mockGantt.init).toHaveBeenCalledTimes(1)
    const initArg = mockGantt.init.mock.calls[0][0]
    expect(initArg).toBeInstanceOf(HTMLElement)
    expect(initArg.classList.contains('gantt-chart')).toBe(true)

    // gantt.parse 应被调用以加载初始数据
    expect(mockGantt.parse).toHaveBeenCalledTimes(1)
    const parsed = mockGantt.parse.mock.calls[0][0]
    expect(parsed.data).toEqual(sampleTasks)
    expect(parsed.links).toEqual(sampleLinks)

    // 默认应加载中文 locale
    expect(mockGantt.i18n.setLocale).toHaveBeenCalledTimes(1)

    wrapper.unmount()
  })

  it('应将 readonly/rowHeight/barHeight 同步到 gantt.config', async () => {
    const wrapper = makeWrapper({ readonly: true, rowHeight: 48, barHeight: 32 })
    await flushPromises()

    expect(mockGantt.config.readonly).toBe(true)
    expect(mockGantt.config.row_height).toBe(48)
    expect(mockGantt.config.bar_height).toBe(32)
    expect(mockGantt.config.show_links).toBe(true)
    expect(mockGantt.config.show_progress).toBe(true)

    // readonly 模式应在容器添加 gantt-readonly 类
    expect(wrapper.find('.gantt-readonly').exists()).toBe(true)

    wrapper.unmount()
  })

  it('showCriticalPath=true 应启用 critical_path 插件并设置 highlight_critical_path', async () => {
    const wrapper = makeWrapper({ showCriticalPath: true })
    await flushPromises()

    expect(mockGantt.plugins).toHaveBeenCalledWith({ critical_path: true })
    expect(mockGantt.config.highlight_critical_path).toBe(true)

    wrapper.unmount()
  })

  it('showCriticalPath=false（默认）不应启用 critical_path 插件', async () => {
    const wrapper = makeWrapper()
    await flushPromises()

    expect(mockGantt.plugins).not.toHaveBeenCalled()
    expect(mockGantt.config.highlight_critical_path).toBeUndefined()

    wrapper.unmount()
  })

  it('应注册 task_class 模板用于按状态/优先级着色', async () => {
    const wrapper = makeWrapper()
    await flushPromises()

    expect(typeof mockGantt.templates.task_class).toBe('function')

    // 测试不同任务的 class
    const taskClass = mockGantt.templates.task_class
    expect(taskClass(new Date(), new Date(), { type: 'milestone' })).toBe('gantt-milestone')
    expect(taskClass(new Date(), new Date(), { type: 'task', priority: 'P0' })).toBe('gantt-task-p0')
    expect(taskClass(new Date(), new Date(), { type: 'task', priority: 'P1', status: 'in_progress' }))
      .toBe('gantt-task-p1 gantt-task-in_progress')
    expect(taskClass(new Date(), new Date(), { type: 'task', status: 'done' })).toBe('gantt-task-done')

    wrapper.unmount()
  })

  it('showMilestones=false 时里程碑任务应返回 hidden class', async () => {
    const wrapper = makeWrapper({ showMilestones: false })
    await flushPromises()

    const taskClass = mockGantt.templates.task_class
    expect(taskClass(new Date(), new Date(), { type: 'milestone' })).toBe('gantt-task-hidden')

    wrapper.unmount()
  })

  it('应注册 6 类事件回调（onBeforeTaskDrag/onAfterTaskDrag/onTaskSelected/onTaskDblClick/onAfterLinkAdd/onAfterLinkDelete）', async () => {
    const wrapper = makeWrapper()
    await flushPromises()

    const expectedEvents = [
      'onBeforeTaskDrag',
      'onAfterTaskDrag',
      'onTaskSelected',
      'onTaskDblClick',
      'onAfterLinkAdd',
      'onAfterLinkDelete'
    ]
    for (const evt of expectedEvents) {
      expect(handlers[evt]).toBeDefined()
      expect(handlers[evt].length).toBeGreaterThanOrEqual(1)
    }

    wrapper.unmount()
  })

  it('任务拖拽结束应 emit task-drag 与 update:tasks', async () => {
    const wrapper = makeWrapper()
    await flushPromises()

    // 模拟 dhtmlx 在 onBeforeTaskDrag 时已有原始任务
    fireEvent('onBeforeTaskDrag', 1, 'move', new Event('drag'))
    // 模拟 dhtmlx 内部已更新任务
    store.tasks[0] = { ...store.tasks[0], start_date: '2026-07-02', duration: 6 }
    fireEvent('onAfterTaskDrag', 1, 'move', new Event('drag'))

    const taskDragEvent = wrapper.emitted('task-drag')
    expect(taskDragEvent).toBeDefined()
    expect(taskDragEvent).toHaveLength(1)
    const [task, mode, original] = taskDragEvent![0] as any
    expect(mode).toBe('move')
    expect(task.id).toBe(1)
    expect(original.id).toBe(1)
    expect(original.start_date).toBe('2026-07-01') // 拖拽前的原始值

    const updateEvent = wrapper.emitted('update:tasks')
    expect(updateEvent).toBeDefined()

    wrapper.unmount()
  })

  it('任务选中应 emit task-select', async () => {
    const wrapper = makeWrapper()
    await flushPromises()

    fireEvent('onTaskSelected', 2)

    expect(wrapper.emitted('task-select')).toBeDefined()
    expect(wrapper.emitted('task-select')![0]).toEqual([2])

    wrapper.unmount()
  })

  it('任务双击应 emit task-dblclick', async () => {
    const wrapper = makeWrapper()
    await flushPromises()

    fireEvent('onTaskDblClick', 3, new Event('dblclick'))

    expect(wrapper.emitted('task-dblclick')).toBeDefined()
    expect(wrapper.emitted('task-dblclick')![0]).toEqual([3])

    wrapper.unmount()
  })

  it('创建链接应 emit link-create', async () => {
    const wrapper = makeWrapper()
    await flushPromises()

    fireEvent('onAfterLinkAdd', 99, { id: 99, source: 1, target: 3, type: '0' })

    expect(wrapper.emitted('link-create')).toBeDefined()
    expect(wrapper.emitted('link-create')![0]).toEqual([
      { id: 99, source: 1, target: 3, type: '0' }
    ])

    wrapper.unmount()
  })

  it('删除链接应 emit link-delete', async () => {
    const wrapper = makeWrapper()
    await flushPromises()

    fireEvent('onAfterLinkDelete', 5, { id: 5, source: 1, target: 2, type: '0' })

    expect(wrapper.emitted('link-delete')).toBeDefined()
    expect(wrapper.emitted('link-delete')![0]).toEqual([5])

    wrapper.unmount()
  })

  it('切换 viewMode radio 应 emit view-mode-change 并调用 setLevel', async () => {
    const wrapper = makeWrapper({ viewMode: 'month' })
    await flushPromises()

    // 找到 radio-group，触发 change
    const radioGroup = wrapper.findComponent({ name: 'ARadioGroup' })
    expect(radioGroup.exists()).toBe(true)

    // 直接修改 v-model 绑定的值（更可靠的方式）
    await radioGroup.vm.$emit('update:value', 'week')
    await nextTick()
    await flushPromises()

    const evt = wrapper.emitted('view-mode-change')
    expect(evt).toBeDefined()
    expect(evt![0]).toEqual(['week'])

    wrapper.unmount()
  })

  it('父组件修改 viewMode 应同步 currentViewMode 并 emit', async () => {
    const wrapper = makeWrapper({ viewMode: 'month' })
    await flushPromises()

    await wrapper.setProps({ viewMode: 'quarter' })
    await nextTick()
    await flushPromises()

    expect(wrapper.emitted('view-mode-change')).toBeDefined()
    const events = wrapper.emitted('view-mode-change')!
    expect(events[events.length - 1]).toEqual(['quarter'])

    wrapper.unmount()
  })

  it('放大/缩小按钮应调用 gantt.ext.zoom.zoomIn / zoomOut', async () => {
    const wrapper = makeWrapper()
    await flushPromises()

    zoomCalls.length = 0
    const buttons = wrapper.findAll('button')
    const zoomInBtn = buttons.find((b) => b.text().includes('放大'))
    const zoomOutBtn = buttons.find((b) => b.text().includes('缩小'))
    expect(zoomInBtn).toBeDefined()
    expect(zoomOutBtn).toBeDefined()

    await zoomInBtn!.trigger('click')
    await nextTick()
    expect(zoomCalls).toContain('zoomIn')

    await zoomOutBtn!.trigger('click')
    await nextTick()
    expect(zoomCalls).toContain('zoomOut')

    wrapper.unmount()
  })

  it('全部展开/折叠按钮应遍历任务设置 $open', async () => {
    const wrapper = makeWrapper()
    await flushPromises()

    const buttons = wrapper.findAll('button')
    const expandBtn = buttons.find((b) => b.text().includes('全部展开'))
    const collapseBtn = buttons.find((b) => b.text().includes('全部折叠'))

    await expandBtn!.trigger('click')
    await nextTick()
    expect(mockGantt.render).toHaveBeenCalled()
    // 每个任务应被设置 $open=true
    expect(store.tasks.every((t) => t.$open === true)).toBe(true)

    mockGantt.render.mockClear()
    await collapseBtn!.trigger('click')
    await nextTick()
    expect(mockGantt.render).toHaveBeenCalled()
    expect(store.tasks.every((t) => t.$open === false)).toBe(true)

    wrapper.unmount()
  })

  it('props.tasks 变化应触发 gantt.parse 重新加载', async () => {
    const wrapper = makeWrapper()
    await flushPromises()
    expect(mockGantt.parse).toHaveBeenCalledTimes(1)

    const newTasks: GanttTask[] = [
      { id: 10, text: '新任务', start_date: '2026-08-01', duration: 3 }
    ]
    await wrapper.setProps({ tasks: newTasks })
    await nextTick()
    await flushPromises()

    // parse 应被再次调用
    expect(mockGantt.parse).toHaveBeenCalledTimes(2)
    const lastCall = mockGantt.parse.mock.calls[mockGantt.parse.mock.calls.length - 1][0]
    expect(lastCall.data).toEqual(newTasks)

    wrapper.unmount()
  })

  it('showToolbar=false 应隐藏工具栏', async () => {
    const wrapper = makeWrapper({ showToolbar: false })
    await flushPromises()

    expect(wrapper.find('.gantt-toolbar').exists()).toBe(false)

    wrapper.unmount()
  })

  it('卸载时应 detach 所有事件并 clearAll', async () => {
    const wrapper = makeWrapper()
    await flushPromises()

    const attachedCount = mockGantt.attachEvent.mock.calls.length

    wrapper.unmount()
    await nextTick()

    expect(mockGantt.detachEvent).toHaveBeenCalledTimes(attachedCount)
    expect(mockGantt.clearAll).toHaveBeenCalled()
  })

  it('Expose 应暴露 refresh/expandAll/collapseAll/zoomIn/zoomOut/setViewMode/getInstance', async () => {
    const wrapper = makeWrapper()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(typeof vm.refresh).toBe('function')
    expect(typeof vm.expandAll).toBe('function')
    expect(typeof vm.collapseAll).toBe('function')
    expect(typeof vm.zoomIn).toBe('function')
    expect(typeof vm.zoomOut).toBe('function')
    expect(typeof vm.setViewMode).toBe('function')
    expect(typeof vm.getInstance).toBe('function')
    expect(vm.getInstance()).toBe(mockGantt)

    wrapper.unmount()
  })
})
