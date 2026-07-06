/**
 * 统一组件导出
 *
 * 说明：
 * - 业务页面使用 CrudTable 时，可 `import CrudTable from '@/components/CrudTable/index.vue'`
 * - 也支持 `import { CrudTable } from '@/components'` 走聚合导出
 * - unplugin-vue-components 已配置自动注册（src/components 目录），模板中可无需显式 import
 *
 * spec 阶段三 Task 12：新增 CrudTable 通用 CRUD 表格组件
 * spec 阶段三 Task 13：新增 FormModal 通用表单弹窗组件
 * spec 阶段三 Task 14：新增 FileUpload 通用文件上传组件
 * spec 阶段三 Task 16：新增 OrgTree 组织树组件（含 OrgTreeSelect 表单选择器变体）
 * spec 阶段三 Task 17：新增 Charts 图表包装组件（ECharts 5 + vue-echarts 6）
 * spec 阶段三 Task 18：新增 Gantt 甘特图组件（dhtmlx-gantt 8）
 */
export { default as CrudTable } from './CrudTable/index.vue'
export { default as FormModal } from './FormModal/index.vue'
export { default as FileUpload } from './FileUpload/index.vue'
export { default as Gantt } from './Gantt/index.vue'
export { default as StatisticCard } from './StatisticCard.vue'
export { default as StatusTag } from './StatusTag.vue'
export { default as PageContainer } from './PageContainer.vue'
export { default as EmptyState } from './EmptyState.vue'
export { default as ProgressBar } from './ProgressBar.vue'
export { default as ImportExport } from './ImportExport/index.vue'

/* ============ OrgTree 组织树组件（Task 16） ============ */
export { OrgTree, OrgTreeSelect, useOrgTree } from './OrgTree'
export type {
  OrgTreeNode,
  OrgType,
  OrgTypeFilter,
  OrgTreeProps,
  OrgTreeSelectProps,
  OrgTreeExpose,
  HighlightSegment
} from './OrgTree'

/* ============ Charts 图表组件（Task 17） ============ */
export {
  BaseChart,
  PieChart,
  LineChart,
  BarChart,
  StackedChart,
  FunnelChart,
  MapChart,
  RadarChart,
  GaugeChart
} from './charts'

/* ============ AMap 高德地图组件（Task 19） ============ */
export { AMap, loadAMap, destroyAMap } from './AMap'
export type { AMapProps, AMapMarker, AMapInstance, MarkerInstance } from './AMap'

/* ============ Gantt 甘特图组件（Task 18） ============ */
export type {
  GanttProps,
  GanttTask,
  GanttLink,
  ViewMode,
  GanttTaskStatus,
  GanttTaskPriority,
  GanttTaskType,
  GanttExpose
} from './Gantt/types'

export type {
  CrudColumn,
  SearchField,
  CrudAction,
  CrudApiFunc,
  ModelBinding,
  CrudTableExpose,
  FormFieldType,
  SearchFieldType,
  ColumnAlign,
  ValueEnumItem
} from './CrudTable/types'

export type {
  FormField,
  FormFieldOption,
  FormFieldCondition,
  FormFieldOptionsWhen,
  FormModalExpose,
  FormFieldType as FormModalFieldType
} from './FormModal/types'

/* ============ FileUpload 类型（Task 14） ============ */
export type {
  FileItem,
  FileItemStatus,
  UploadListType,
  PresignResponse,
  MultipartInitResponse,
  WatermarkOptions,
  FileUploadProps,
  FileUploadEmits,
  FileUploadExpose
} from './FileUpload/types'

/* ============ ImportExport 类型（Task 20） ============ */
export type {
  ImportExportProps,
  ImportResult,
  ImportError,
  ImportExportExpose
} from './ImportExport/types'
