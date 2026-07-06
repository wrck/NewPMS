<script setup lang="ts">
/**
 * Gantt 甘特图组件（spec 阶段三 Task 18）
 *
 * 基于 dhtmlx-gantt 8.x（GPL 版本）封装，职责：
 *   1. 屏蔽 dhtmlx-gantt 复杂配置，对业务暴露 tasks/links/viewMode/readonly 等简洁模型
 *   2. 顶部工具栏：视图切换（日/周/月/季/年）+ 展开/折叠 + 放大/缩小
 *   3. 任务拖拽调整时间（onAfterTaskDrag），拖拽创建依赖链接（onAfterLinkAdd）
 *   4. 自定义任务条颜色（按 status/priority）+ 里程碑标记
 *   5. 关键路径高亮（需 showCriticalPath=true）
 *   6. 容器尺寸变化自动 setSizes（监听 window resize）
 *   7. 暴露事件：task-drag / task-select / task-dblclick / link-create / link-delete / view-mode-change
 *
 * 使用示例见 ./README.md
 */
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { gantt } from 'dhtmlx-gantt'
import type { Scales } from 'dhtmlx-gantt'
import 'dhtmlx-gantt/codebase/dhtmlxgantt.css'
import type { GanttProps, GanttTask, GanttLink, ViewMode, GanttExpose } from './types'

/* ============ Props ============ */
const props = withDefaults(defineProps<GanttProps>(), {
  links: () => [],
  viewMode: 'month',
  readonly: false,
  showToolbar: true,
  showLinks: true,
  showProgress: true,
  showCriticalPath: false,
  showMilestones: true,
  rowHeight: 36,
  barHeight: 24
})

/* ============ Emits ============ */
const emit = defineEmits<{
  /** 任务数据更新（拖拽调整时间后触发，回传最新任务列表） */
  'update:tasks': [tasks: GanttTask[]]
  /** 任务拖拽结束（mode: 'move'/'resize'/'progress'） */
  'task-drag': [task: GanttTask, mode: string, original: GanttTask]
  /** 任务选中 */
  'task-select': [taskId: string | number]
  /** 任务双击 */
  'task-dblclick': [taskId: string | number]
  /** 创建依赖链接 */
  'link-create': [link: GanttLink]
  /** 删除依赖链接 */
  'link-delete': [linkId: string | number]
  /** 视图模式切换 */
  'view-mode-change': [mode: ViewMode]
}>()

/* ============ 内部状态 ============ */
const ganttRef = ref<HTMLElement>()
const currentViewMode = ref<ViewMode>(props.viewMode)
/** 拖拽开始前的原始任务快照（id -> 任务），用于 task-drag 事件回传 original */
const originalTaskMap = new Map<string | number, GanttTask>()
/** 已注册事件 id 集合，卸载时统一 detach */
const eventIds: string[] = []
/** 标记是否已初始化，避免 props 在 onMounted 前变化触发 watch 报错 */
let initialized = false

/* ============ 业务模型 <-> dhtmlx Task 转换 ============ */
/** 提取业务关心的字段（剔除 $开头的 dhtmlx 内部属性） */
function toPlainTask(t: any): GanttTask {
  return {
    id: t.id,
    text: t.text,
    start_date: t.start_date,
    duration: t.duration,
    progress: t.progress,
    parent: t.parent,
    type: t.type,
    priority: t.priority,
    status: t.status,
    assignee: t.assignee,
    open: t.open,
    $open: t.$open
  }
}

function toPlainLink(l: any): GanttLink {
  return {
    id: l.id,
    source: l.source,
    target: l.target,
    type: l.type
  }
}

/** 获取当前全部任务（深度遍历） */
function getAllTasks(): GanttTask[] {
  const list: GanttTask[] = []
  gantt.eachTask((t: any) => list.push(toPlainTask(t)))
  return list
}

/* ============ 视图模式 -> scales 配置（dhtmlx-gantt 8.x 已废弃 scale_unit/subscales） ============ */
function buildScales(mode: ViewMode): Scales {
  switch (mode) {
    case 'day':
      return [
        { unit: 'day', step: 1, format: '%m-%d %D' }
      ] as Scales
    case 'week':
      return [
        { unit: 'week', step: 1, format: '%Y 年第 %W 周' },
        { unit: 'day', step: 1, format: '%m-%d' }
      ] as Scales
    case 'month':
      return [
        { unit: 'month', step: 1, format: '%Y-%m' },
        { unit: 'week', step: 1, format: '%W 周' }
      ] as Scales
    case 'quarter':
      return [
        {
          unit: 'quarter',
          step: 1,
          format: (date: Date) => `${date.getFullYear()} 年 Q${Math.floor(date.getMonth() / 3) + 1}`
        },
        { unit: 'month', step: 1, format: '%m 月' }
      ] as Scales
    case 'year':
      return [
        { unit: 'year', step: 1, format: '%Y 年' },
        {
          unit: 'quarter',
          step: 1,
          format: (date: Date) => `Q${Math.floor(date.getMonth() / 3) + 1}`
        }
      ] as Scales
    default:
      return [{ unit: 'day', step: 1, format: '%m-%d' }] as Scales
  }
}

/* ============ 初始化 zoom 插件（提供 5 个对应视图模式层级，并支持 zoomIn/zoomOut） ============ */
function initZoomPlugin(): void {
  gantt.ext.zoom.init({
    levels: [
      { name: 'day', scales: buildScales('day') },
      { name: 'week', scales: buildScales('week') },
      { name: 'month', scales: buildScales('month'), min_column_width: 80 },
      { name: 'quarter', scales: buildScales('quarter'), min_column_width: 60 },
      { name: 'year', scales: buildScales('year'), min_column_width: 40 }
    ]
  })
}

/* ============ 应用视图模式 ============ */
function applyViewMode(mode: ViewMode): void {
  gantt.config.scales = buildScales(mode)
  // 同步 zoom 插件层级，使 zoomIn/zoomOut 边界与当前视图模式对齐
  try {
    gantt.ext.zoom.setLevel(mode)
  } catch (e) {
    // zoom 未初始化时忽略，buildScales 已直接生效
    console.warn('[Gantt] set zoom level failed:', e)
  }
  gantt.render()
}

/* ============ 加载数据 ============ */
function loadData(tasks: GanttTask[], links: GanttLink[]): void {
  if (!initialized) return
  gantt.clearAll()
  // dhtmlx-gantt parse 接受 { data, links } 结构，自定义字段（priority/status/assignee）会被保留
  gantt.parse({ data: tasks, links } as any)
}

/* ============ 初始化 Gantt ============ */
function initGantt(): void {
  if (!ganttRef.value || initialized) return

  /* ----- 基础配置 ----- */
  gantt.config.row_height = props.rowHeight
  gantt.config.bar_height = props.barHeight
  gantt.config.readonly = props.readonly
  gantt.config.show_links = props.showLinks
  gantt.config.show_progress = props.showProgress
  gantt.config.fit_tasks = true
  gantt.config.autosize = 'y'
  // 中文本地化（dhtmlx-gantt 默认 locale 为英文）
  gantt.i18n.setLocale({
    date: {
      month_full: ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月'],
      month_short: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'],
      day_full: ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'],
      day_short: ['日', '一', '二', '三', '四', '五', '六']
    },
    labels: {
      new_task: '新任务',
      icon_save: '保存',
      icon_cancel: '取消',
      icon_details: '详情',
      icon_edit: '编辑',
      icon_delete: '删除',
      confirm_closing: '',
      confirm_deleting: '任务将被删除，确定吗？',
      section_description: '描述',
      section_time: '时间区间',
      section_type: '类型',
      section_priority: '优先级',
      section_status: '状态',
      section_assignee: '负责人',
      section_progress: '进度',
      column_text: '任务名称',
      column_start_date: '开始时间',
      column_duration: '工期',
      column_add: '',
      link: '链接',
      confirm_link_deleting: '将被删除',
      link_start: '（开始）',
      link_end: '（结束）',
      type_task: '任务',
      type_project: '项目',
      type_milestone: '里程碑',
      minutes: '分钟',
      hours: '小时',
      days: '天',
      weeks: '周',
      months: '月',
      years: '年'
    }
  } as any)

  // 默认 scales（与 viewMode 对齐）
  gantt.config.scales = buildScales(currentViewMode.value)

  /* ----- 关键路径插件（可选） ----- */
  if (props.showCriticalPath) {
    gantt.plugins({ critical_path: true })
    gantt.config.highlight_critical_path = true
  }

  /* ----- 自定义任务条样式（按 status / priority / milestone 分类） ----- */
  gantt.templates.task_class = (start: Date, end: Date, task: any): string => {
    if (!props.showMilestones && task.type === 'milestone') return 'gantt-task-hidden'
    if (task.type === 'milestone') return 'gantt-milestone'
    const classes: string[] = []
    if (task.priority) classes.push(`gantt-task-${String(task.priority).toLowerCase()}`)
    if (task.status) classes.push(`gantt-task-${String(task.status).toLowerCase()}`)
    return classes.join(' ')
  }

  /* ----- tooltip 显示业务字段 ----- */
  gantt.templates.tooltip_text = (start: Date, end: Date, task: any): string => {
    const lines: string[] = [`<b>${task.text}</b>`]
    if (task.assignee) lines.push(`负责人: ${task.assignee}`)
    if (task.priority) lines.push(`优先级: ${task.priority}`)
    if (task.status) lines.push(`状态: ${task.status}`)
    if (task.progress != null) lines.push(`进度: ${Math.round(task.progress * 100)}%`)
    return lines.join('<br/>')
  }

  /* ----- 初始化 zoom 插件 ----- */
  initZoomPlugin()

  /* ----- 注册事件 ----- */
  // 拖拽前保存原始任务，便于 onAfterTaskDrag 时回传 original
  eventIds.push(
    gantt.attachEvent('onBeforeTaskDrag', (id: string | number, mode: string, e: Event): boolean => {
      const t = gantt.getTask(id)
      originalTaskMap.set(id, toPlainTask(t))
      return true
    })
  )
  // 拖拽结束 -> emit task-drag + update:tasks
  eventIds.push(
    gantt.attachEvent('onAfterTaskDrag', (id: string | number, mode: string, e: Event): void => {
      const task = gantt.getTask(id)
      const original = originalTaskMap.get(id) || toPlainTask(task)
      emit('task-drag', toPlainTask(task), mode, original)
      originalTaskMap.delete(id)
      emit('update:tasks', getAllTasks())
    })
  )
  // 选中
  eventIds.push(
    gantt.attachEvent('onTaskSelected', (id: string | number): void => {
      emit('task-select', id)
    })
  )
  // 双击
  eventIds.push(
    gantt.attachEvent('onTaskDblClick', (id: string | number, e?: Event): boolean => {
      emit('task-dblclick', id)
      return true
    })
  )
  // 创建链接
  eventIds.push(
    gantt.attachEvent('onAfterLinkAdd', (id: string | number, link: any): void => {
      emit('link-create', toPlainLink(link))
    })
  )
  // 删除链接
  eventIds.push(
    gantt.attachEvent('onAfterLinkDelete', (id: string | number, link: any): void => {
      emit('link-delete', link?.id != null ? link.id : id)
    })
  )

  /* ----- 初始化 ----- */
  gantt.init(ganttRef.value)
  initialized = true

  // 加载初始数据
  loadData(props.tasks, props.links)

  // 视图模式同步到 zoom 插件层级
  try {
    gantt.ext.zoom.setLevel(currentViewMode.value)
  } catch (e) {
    console.warn('[Gantt] initial setLevel failed:', e)
  }
}

/* ============ watch props.tasks / links ============ */
watch(
  () => props.tasks,
  (newTasks) => {
    loadData(newTasks, props.links)
  },
  { deep: true }
)

watch(
  () => props.links,
  (newLinks) => {
    loadData(props.tasks, newLinks)
  },
  { deep: true }
)

/* ============ viewMode 双向同步 ============ */
// 父组件修改 viewMode -> 同步 currentViewMode -> watch(currentViewMode) 触发 applyViewMode
watch(
  () => props.viewMode,
  (mode) => {
    if (mode !== currentViewMode.value) {
      currentViewMode.value = mode
    }
  }
)

// toolbar 切换或父组件驱动 currentViewMode 变化 -> applyViewMode + emit
watch(currentViewMode, (mode) => {
  if (!initialized) return
  applyViewMode(mode)
  emit('view-mode-change', mode)
})

/* ============ 工具栏事件处理 ============ */
function handleExpandAll(): void {
  if (!initialized) return
  gantt.eachTask((t: any) => {
    t.$open = true
  })
  gantt.render()
}

function handleCollapseAll(): void {
  if (!initialized) return
  gantt.eachTask((t: any) => {
    t.$open = false
  })
  gantt.render()
}

function handleZoomIn(): void {
  if (!initialized) return
  // zoomIn 会切换到更精细的层级；同步 currentViewMode 以保持 toolbar 状态一致
  gantt.ext.zoom.zoomIn()
  syncViewModeFromZoom()
}

function handleZoomOut(): void {
  if (!initialized) return
  gantt.ext.zoom.zoomOut()
  syncViewModeFromZoom()
}

/** zoom 切换后，反查当前 level 名称同步 currentViewMode（避免与 toolbar 不一致） */
function syncViewModeFromZoom(): void {
  try {
    // dhtmlx-gantt 8.x 类型声明 getLevels() 返回 void，实际返回 ZoomLevel[]，需类型断言
    const levels = gantt.ext.zoom.getLevels() as unknown as any[]
    const idx = gantt.ext.zoom.getCurrentLevel()
    const name = levels?.[idx]?.name as ViewMode | undefined
    if (name && name !== currentViewMode.value) {
      currentViewMode.value = name
    }
  } catch (e) {
    // 部分版本 getLevels 返回 void，忽略
  }
}

/* ============ 自适应窗口尺寸 ============ */
function handleResize(): void {
  if (ganttRef.value && initialized) {
    gantt.setSizes()
  }
}

/* ============ 生命周期 ============ */
onMounted(() => {
  initGantt()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  eventIds.forEach((id) => gantt.detachEvent(id))
  eventIds.length = 0
  originalTaskMap.clear()
  if (initialized) {
    gantt.clearAll()
    initialized = false
  }
})

/* ============ Expose ============ */
defineExpose<GanttExpose>({
  refresh: () => loadData(props.tasks, props.links),
  expandAll: handleExpandAll,
  collapseAll: handleCollapseAll,
  zoomIn: handleZoomIn,
  zoomOut: handleZoomOut,
  setViewMode: (mode: ViewMode) => {
    currentViewMode.value = mode
  },
  getInstance: () => gantt
})
</script>

<template>
  <div class="gantt-container" :class="{ 'gantt-readonly': readonly }">
    <div v-if="showToolbar" class="gantt-toolbar">
      <a-radio-group v-model:value="currentViewMode" size="small">
        <a-radio-button value="day">日</a-radio-button>
        <a-radio-button value="week">周</a-radio-button>
        <a-radio-button value="month">月</a-radio-button>
        <a-radio-button value="quarter">季</a-radio-button>
        <a-radio-button value="year">年</a-radio-button>
      </a-radio-group>
      <a-space>
        <a-button size="small" @click="handleExpandAll">全部展开</a-button>
        <a-button size="small" @click="handleCollapseAll">全部折叠</a-button>
        <a-button size="small" @click="handleZoomIn">放大</a-button>
        <a-button size="small" @click="handleZoomOut">缩小</a-button>
      </a-space>
    </div>
    <div ref="ganttRef" class="gantt-chart"></div>
  </div>
</template>

<style lang="less" scoped>
.gantt-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}
.gantt-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}
.gantt-chart {
  width: 100%;
  flex: 1;
  min-height: 400px;
  overflow: auto;
}
.gantt-readonly {
  // 只读模式下鼠标指针提示
  :deep(.gantt_task_line) {
    cursor: not-allowed;
  }
}
/* 隐藏 dhtmlx 默认边框阴影，避免与卡片样式冲突 */
:deep(.gantt_container) {
  border: none;
}
</style>

<style>
/* ============ 自定义任务条颜色（全局，dhtmlx-gantt 内部 DOM 不受 scoped 影响） ============ */
/* 按优先级 */
.gantt_task_line.gantt-task-p0 {
  background-color: #ff4d4f !important;
  border-color: #ff4d4f !important;
}
.gantt_task_line.gantt-task-p1 {
  background-color: #fa8c16 !important;
  border-color: #fa8c16 !important;
}
.gantt_task_line.gantt-task-p2 {
  background-color: #faad14 !important;
  border-color: #faad14 !important;
}
.gantt_task_line.gantt-task-p3 {
  background-color: #1677ff !important;
  border-color: #1677ff !important;
}
/* 按状态 */
.gantt_task_line.gantt-task-pending {
  background-color: #d9d9d9 !important;
  border-color: #d9d9d9 !important;
}
.gantt_task_line.gantt-task-in_progress {
  background-color: #1677ff !important;
  border-color: #1677ff !important;
}
.gantt_task_line.gantt-task-done {
  background-color: #52c41a !important;
  border-color: #52c41a !important;
}
.gantt_task_line.gantt-task-overdue {
  background-color: #ff4d4f !important;
  border-color: #ff4d4f !important;
}
.gantt_task_line.gantt-task-cancelled {
  background-color: #8c8c8c !important;
  border-color: #8c8c8c !important;
}
/* 里程碑：紫色圆形 */
.gantt_task_line.gantt-milestone {
  background-color: #722ed1 !important;
  border-color: #722ed1 !important;
  border-radius: 50%;
}
.gantt_task_line.gantt-task-hidden {
  display: none !important;
}
/* 文字色对比（深色任务条文字白色） */
.gantt_task_line .gantt_task_text {
  color: #fff;
  font-size: 12px;
  padding: 0 8px;
}
.gantt_task_line.gantt-task-pending .gantt_task_text {
  color: #595959;
}
</style>
