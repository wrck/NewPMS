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
 * spec 阶段三 Task 17：新增 Charts 图表包装组件（ECharts 5 + vue-echarts 6）
 */
export { default as CrudTable } from './CrudTable/index.vue'
export { default as FormModal } from './FormModal/index.vue'
export { default as StatisticCard } from './StatisticCard.vue'
export { default as StatusTag } from './StatusTag.vue'
export { default as PageContainer } from './PageContainer.vue'
export { default as EmptyState } from './EmptyState.vue'
export { default as ProgressBar } from './ProgressBar.vue'

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
