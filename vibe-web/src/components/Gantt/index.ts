/**
 * Gantt 甘特图组件聚合导出（spec 阶段三 Task 18）
 *
 * 用法：
 *   import Gantt from '@/components/Gantt/index.vue'
 *   // 或聚合导出
 *   import { Gantt } from '@/components'
 *
 * 说明：
 *   - 基于 dhtmlx-gantt 8.x（GPL 版本）封装
 *   - 支持 tasks/links/viewMode/readonly 等简洁 props
 *   - 支持任务拖拽调整时间、拖拽创建依赖链接
 *   - 视图切换：日 / 周 / 月 / 季 / 年
 *   - 自定义任务条颜色（按 status / priority）+ 里程碑标记
 *   - 暴露事件：task-drag / task-select / link-create / link-delete / view-mode-change
 */
import Gantt from './index.vue'

export { default as Gantt } from './index.vue'
export type {
  GanttProps,
  GanttTask,
  GanttLink,
  ViewMode,
  GanttTaskStatus,
  GanttTaskPriority,
  GanttTaskType,
  GanttExpose,
  NativeTask,
  NativeLink
} from './types'

export default Gantt
