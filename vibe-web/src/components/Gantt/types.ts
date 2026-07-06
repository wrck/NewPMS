/**
 * Gantt 甘特图组件类型定义（spec 阶段三 Task 18）
 *
 * 基于 dhtmlx-gantt 8.x（GPL 版本）抽取通用甘特图组件，
 * 屏蔽底层 dhtmlx-gantt API，对业务暴露简洁的 tasks/links/viewMode/readonly 等模型。
 */
import type { Task as DhtmlxTask, Link as DhtmlxLink } from 'dhtmlx-gantt'

/** 视图模式：日 / 周 / 月 / 季 / 年 */
export type ViewMode = 'day' | 'week' | 'month' | 'quarter' | 'year'

/** 任务状态（与后端项目状态语义对齐，用于按状态着色） */
export type GanttTaskStatus = 'pending' | 'in_progress' | 'done' | 'overdue' | 'cancelled'

/** 任务优先级（P0 最高） */
export type GanttTaskPriority = 'P0' | 'P1' | 'P2' | 'P3'

/** 任务类型 */
export type GanttTaskType = 'task' | 'project' | 'milestone'

/**
 * 甘特图任务（业务模型）
 *
 * 与 dhtmlx-gantt 原生 Task 兼容（id/start_date/duration/parent/type/progress 等字段语义一致），
 * 同时附加 priority/status/assignee 等业务字段用于着色与展示。
 * 自定义字段会被 dhtmlx-gantt 8.x 的 Task 索引签名接收并保留。
 */
export interface GanttTask {
  /** 任务 id（唯一） */
  id: string | number
  /** 任务名称 */
  text: string
  /** 开始日期（dhtmlx 接受 Date 或 'YYYY-MM-DD HH:mm' 字符串） */
  start_date: string | Date
  /** 工期（天） */
  duration: number
  /** 进度 0~1 */
  progress?: number
  /** 父任务 id（用于 WBS 层级），根任务为 0 */
  parent?: string | number
  /** 任务类型 */
  type?: GanttTaskType
  /** 优先级（用于按优先级着色） */
  priority?: GanttTaskPriority
  /** 状态（用于按状态着色） */
  status?: GanttTaskStatus
  /** 负责人 */
  assignee?: string
  /** 是否默认展开（仅对有子任务的项目任务生效） */
  open?: boolean
  /** dhtmlx 内部展开状态（同步用） */
  $open?: boolean
  /** 任意扩展字段（与 dhtmlx Task 索引签名对齐） */
  [key: string]: any
}

/** 依赖关系链接 */
export interface GanttLink {
  /** 链接 id（dhtmlx 自动生成，可选） */
  id?: string | number
  /** 起点任务 id */
  source: string | number
  /** 终点任务 id */
  target: string | number
  /**
   * 链接类型：
   *   '0' - Finish-to-Start 完成-开始（默认）
   *   '1' - Start-to-Start 开始-开始
   *   '2' - Finish-to-Finish 完成-完成
   *   '3' - Start-to-Finish 开始-完成
   */
  type: '0' | '1' | '2' | '3'
  /** 任意扩展字段（与 dhtmlx Link 索引签名对齐） */
  [key: string]: any
}

/** Gantt 组件 Props */
export interface GanttProps {
  /** 任务数据（必填） */
  tasks: GanttTask[]
  /** 依赖链接数据 */
  links?: GanttLink[]
  /** 初始视图模式，默认 month */
  viewMode?: ViewMode
  /** 是否只读（不可拖拽/不可创建链接） */
  readonly?: boolean
  /** 是否显示顶部工具栏（视图切换 + 缩放按钮） */
  showToolbar?: boolean
  /** 是否显示依赖链接线 */
  showLinks?: boolean
  /** 是否显示任务进度条 */
  showProgress?: boolean
  /** 是否高亮关键路径（需启用 critical_path 插件） */
  showCriticalPath?: boolean
  /** 是否显示里程碑 */
  showMilestones?: boolean
  /** 行高（px） */
  rowHeight?: number
  /** 任务条高度（px） */
  barHeight?: number
}

/** 暴露给外部的实例方法 */
export interface GanttExpose {
  /** 重新加载任务数据 */
  refresh: () => void
  /** 全部展开 */
  expandAll: () => void
  /** 全部折叠 */
  collapseAll: () => void
  /** 放大 */
  zoomIn: () => void
  /** 缩小 */
  zoomOut: () => void
  /** 切换视图模式 */
  setViewMode: (mode: ViewMode) => void
  /** 获取当前 gantt 实例（高级用法，直接操作原生 API） */
  getInstance: () => typeof import('dhtmlx-gantt')['gantt']
}

/** 业务模型 <-> dhtmlx 原生模型互转工具（仅类型层面，运行时直接透传） */
export type NativeTask = DhtmlxTask
export type NativeLink = DhtmlxLink
