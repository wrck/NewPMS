# Gantt 甘特图组件

> spec 阶段三 Task 18：基于 dhtmlx-gantt 8.x（GPL 版本）抽取通用甘特图组件，支持任务拖拽调整时间、拖拽创建依赖链接、视图切换（日/周/月/季/年）、按状态/优先级自定义任务条颜色、里程碑标记、关键路径高亮。

## 目录

- [快速开始](#快速开始)
- [依赖](#依赖)
- [Props](#props)
- [Emits](#emits)
- [Expose](#expose)
- [任务数据结构](#任务数据结构)
- [依赖链接结构](#依赖链接结构)
- [视图模式](#视图模式)
- [自定义任务条颜色](#自定义任务条颜色)
- [里程碑](#里程碑)
- [关键路径](#关键路径)
- [只读模式](#只读模式)
- [注意事项](#注意事项)

## 依赖

```json
{
  "dhtmlx-gantt": "^8.0.6"
}
```

> 使用 GPL 版本（免费），适合内部项目。商业产品请购买 PRO 授权。

## 快速开始

```vue
<template>
  <Gantt
    :tasks="tasks"
    :links="links"
    view-mode="month"
    @task-drag="onTaskDrag"
    @link-create="onLinkCreate"
    @view-mode-change="onViewModeChange"
  />
</template>

<script setup lang="ts">
import Gantt from '@/components/Gantt/index.vue'
import type { GanttTask, GanttLink, ViewMode } from '@/components/Gantt/types'

const tasks: GanttTask[] = [
  { id: 1, text: '需求分析', start_date: '2026-07-01', duration: 5, progress: 0.6, status: 'in_progress', priority: 'P0', assignee: '张三' },
  { id: 2, text: '架构设计', start_date: '2026-07-06', duration: 4, progress: 0.2, status: 'pending', priority: 'P1', assignee: '李四', parent: 1 },
  { id: 3, text: '里程碑：设计评审', start_date: '2026-07-10', duration: 0, type: 'milestone', priority: 'P0' },
  { id: 4, text: '开发实现', start_date: '2026-07-11', duration: 10, progress: 0, status: 'pending', priority: 'P1' }
]

const links: GanttLink[] = [
  { id: 1, source: 1, target: 2, type: '0' }, // 完成-开始
  { id: 2, source: 2, target: 3, type: '0' },
  { id: 3, source: 3, target: 4, type: '0' }
]

function onTaskDrag(task: GanttTask, mode: string, original: GanttTask) {
  console.log('任务拖拽结束:', task, '模式:', mode, '原始:', original)
}

function onLinkCreate(link: GanttLink) {
  console.log('创建依赖:', link)
}

function onViewModeChange(mode: ViewMode) {
  console.log('视图切换到:', mode)
}
</script>
```

## Props

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `tasks` | `GanttTask[]` | 必填 | 任务数据，详见 [任务数据结构](#任务数据结构) |
| `links` | `GanttLink[]` | `[]` | 依赖链接数据，详见 [依赖链接结构](#依赖链接结构) |
| `viewMode` | `ViewMode` | `'month'` | 初始视图模式，详见 [视图模式](#视图模式) |
| `readonly` | `boolean` | `false` | 是否只读（不可拖拽/不可创建链接） |
| `showToolbar` | `boolean` | `true` | 是否显示顶部工具栏（视图切换 + 缩放按钮） |
| `showLinks` | `boolean` | `true` | 是否显示依赖链接线 |
| `showProgress` | `boolean` | `true` | 是否显示任务进度条 |
| `showCriticalPath` | `boolean` | `false` | 是否高亮关键路径 |
| `showMilestones` | `boolean` | `true` | 是否显示里程碑 |
| `rowHeight` | `number` | `36` | 行高（px） |
| `barHeight` | `number` | `24` | 任务条高度（px） |

## Emits

| 事件 | 参数 | 说明 |
| --- | --- | --- |
| `update:tasks` | `(tasks: GanttTask[])` | 任务数据更新（拖拽调整时间后触发，回传最新任务列表，可用于 `v-model:tasks`） |
| `task-drag` | `(task: GanttTask, mode: string, original: GanttTask)` | 任务拖拽结束。`mode` 取值：`'move'`（拖动整体）、`'resize'`（拖动边缘调整工期）、`'progress'`（拖动调整进度） |
| `task-select` | `(taskId: string \| number)` | 任务选中 |
| `task-dblclick` | `(taskId: string \| number)` | 任务双击 |
| `link-create` | `(link: GanttLink)` | 创建依赖链接 |
| `link-delete` | `(linkId: string \| number)` | 删除依赖链接 |
| `view-mode-change` | `(mode: ViewMode)` | 视图模式切换 |

## Expose

通过 `ref` 获取组件实例可调用以下方法：

| 方法 | 说明 |
| --- | --- |
| `refresh()` | 重新加载当前 props.tasks 与 props.links |
| `expandAll()` | 全部展开 |
| `collapseAll()` | 全部折叠 |
| `zoomIn()` | 放大视图（切换到更精细的层级） |
| `zoomOut()` | 缩小视图（切换到更粗略的层级） |
| `setViewMode(mode)` | 切换视图模式 |
| `getInstance()` | 获取原生 dhtmlx-gantt 实例（高级用法，直接操作原生 API） |

```vue
<template>
  <Gantt ref="ganttRef" :tasks="tasks" />
  <a-button @click="ganttRef?.expandAll()">展开全部</a-button>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Gantt from '@/components/Gantt/index.vue'
import type { GanttExpose } from '@/components/Gantt/types'

const ganttRef = ref<GanttExpose>()
</script>
```

## 任务数据结构

```ts
interface GanttTask {
  id: string | number       // 必填，任务 id
  text: string              // 必填，任务名称
  start_date: string | Date // 必填，开始日期（'YYYY-MM-DD' 或 Date）
  duration: number          // 必填，工期（天），里程碑为 0
  progress?: number         // 进度 0~1
  parent?: string | number  // 父任务 id（用于 WBS 层级），根任务为 0
  type?: 'task' | 'project' | 'milestone'  // 任务类型
  priority?: 'P0' | 'P1' | 'P2' | 'P3'      // 优先级（用于着色）
  status?: 'pending' | 'in_progress' | 'done' | 'overdue' | 'cancelled' // 状态
  assignee?: string         // 负责人
  open?: boolean            // 是否默认展开（仅对有子任务的项目任务生效）
  [key: string]: any         // 任意扩展字段会被 dhtmlx 保留
}
```

## 依赖链接结构

```ts
interface GanttLink {
  id?: string | number       // 链接 id（dhtmlx 自动生成）
  source: string | number   // 起点任务 id
  target: string | number   // 终点任务 id
  type: '0' | '1' | '2' | '3' // 链接类型
}
```

链接类型说明：

| 值 | 含义 | 说明 |
| --- | --- | --- |
| `'0'` | Finish-to-Start | 完成-开始（默认，A 完成后 B 才能开始） |
| `'1'` | Start-to-Start | 开始-开始（A 开始后 B 才能开始） |
| `'2'` | Finish-to-Finish | 完成-完成（A 完成后 B 才能完成） |
| `'3'` | Start-to-Finish | 开始-完成（A 开始后 B 才能完成） |

## 视图模式

| 模式 | 顶层刻度 | 次层刻度 |
| --- | --- | --- |
| `day` | 日 | - |
| `week` | 周（`YYYY 年第 WW 周`） | 日 |
| `month` | 月（`YYYY-MM`） | 周 |
| `quarter` | 季度（`YYYY 年 QN`） | 月 |
| `year` | 年（`YYYY 年`） | 季度 |

通过工具栏 radio 切换，或通过 `view-mode` prop 与 `view-mode-change` 事件双向同步。

## 自定义任务条颜色

任务条颜色按以下优先级自动应用（CSS 类名拼接在 dhtmlx 任务条 DOM 上）：

1. **里程碑**：紫色圆形（`gantt-milestone`）
2. **优先级**：P0 红 / P1 橙 / P2 黄 / P3 蓝
3. **状态**：pending 灰 / in_progress 蓝 / done 绿 / overdue 红 / cancelled 深灰

CSS 定义在组件 `<style>` 块（非 scoped，因为 dhtmlx 内部 DOM 不受 scoped 影响）。可通过覆盖以下类名自定义：

```css
.gantt_task_line.gantt-task-p0 { background-color: #ff4d4f !important; }
.gantt_task_line.gantt-task-done { background-color: #52c41a !important; }
.gantt_task_line.gantt-milestone { background-color: #722ed1 !important; border-radius: 50%; }
```

## 里程碑

设置 `type: 'milestone'` 与 `duration: 0` 即可渲染里程碑（紫色圆形）。`showMilestones=false` 可隐藏所有里程碑。

```ts
{ id: 3, text: '里程碑：设计评审', start_date: '2026-07-10', duration: 0, type: 'milestone' }
```

## 关键路径

`showCriticalPath=true` 启用关键路径高亮（自动加载 `critical_path` 插件）。关键路径上的任务会用红色高亮（dhtmlx 默认样式）。

## 只读模式

`readonly=true` 时：

- 任务不可拖拽（dhtmlx `config.readonly = true`）
- 不可创建/删除链接
- 鼠标指针变为 `not-allowed`
- 工具栏仍可切换视图与缩放

## 注意事项

1. **GPL 授权**：dhtmlx-gantt 8.x GPL 版本仅适用于开源项目或内部使用，商业产品请购买 PRO 授权。
2. **CSS 隔离**：dhtmlx 内部 DOM 不受 Vue scoped 影响，自定义任务条颜色需放在 `<style>` 非 scoped 块，类名以 `.gantt_task_line` 前缀。
3. **i18n**：组件已内置中文本地化（月份、星期、按钮文案、字段标签）。
4. **响应式**：监听 `window.resize` 自动调用 `gantt.setSizes()` 适配容器尺寸。
5. **数据双向同步**：`watch(props.tasks, { deep: true })` 重新加载，注意替换数组引用而非直接修改元素（避免触发深度 watch 的高频更新）。
6. **性能**：dhtmlx-gantt 8.x 推荐使用 `scales` 数组替代旧版 `scale_unit + subscales`，本组件已采用新 API。
7. **卸载清理**：`onBeforeUnmount` 会 detach 所有事件并 `clearAll()`，避免内存泄漏。
