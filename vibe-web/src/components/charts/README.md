# Charts 图表包装组件

> spec 阶段三 Task 17：基于 ECharts 5 + vue-echarts 6 抽取通用图表组件，覆盖饼图/折线/柱状/堆叠/漏斗/地图/雷达/仪表盘 8 类，统一 vibe 主题（品牌色 #1677FF + 状态色映射），支持响应式 resize、loading 状态、空数据 EmptyState。

## 目录

- [设计概览](#设计概览)
- [依赖](#依赖)
- [快速开始](#快速开始)
- [BaseChart 基础组件](#basechart-基础组件)
- [PieChart 饼图](#piechart-饼图)
- [LineChart 折线图](#linechart-折线图)
- [BarChart 柱状图](#barchart-柱状图)
- [StackedChart 堆叠图](#stackedchart-堆叠图)
- [FunnelChart 漏斗图](#funnelchart-漏斗图)
- [MapChart 地图](#mapchart-地图)
- [RadarChart 雷达图](#radarchart-雷达图)
- [GaugeChart 仪表盘](#gaugechart-仪表盘)
- [主题与调色盘](#主题与调色盘)
- [注意事项](#注意事项)

## 设计概览

- **BaseChart** 为基础包装组件，封装 vue-echarts 6，统一处理：模块按需注册、vibe 主题注册、option 响应、autoresize、dispose、loading（a-spin）、空数据（EmptyState）。
- 8 个图表子组件基于 BaseChart 封装，各自负责将业务 props 转换为 ECharts option，业务页面无需关心 option 细节。
- 主题在 `theme.ts` 统一维护，调色盘与 `src/styles/variables.less` 的状态色语义对齐。

## 依赖

```json
{
  "echarts": "^5.5.0",
  "vue-echarts": "6.7.3"
}
```

> ECharts 5 已移除内置地图数据，MapChart 在挂载时按 mapType 从公共 CDN（阿里 DataV）懒加载 GeoJSON 并通过 `echarts.registerMap` 注册（全局缓存，仅加载一次）。

## 快速开始

```vue
<script setup lang="ts">
import { PieChart, LineChart, BarChart, GaugeChart } from '@/components/charts'

const pieData = [
  { name: '立项中', value: 12 },
  { name: '执行中', value: 28 },
  { name: '已验收', value: 19 },
  { name: '已归档', value: 7 }
]

const months = ['1月', '2月', '3月', '4月', '5月', '6月']
const lineData = [
  { name: '新增项目', data: [3, 5, 4, 6, 8, 7] },
  { name: '完成项目', data: [1, 2, 3, 4, 5, 6] }
]

const barData = [{ name: '工时', data: [120, 200, 150, 80, 70, 110] }]

const gaugeValue = 76
</script>

<template>
  <PieChart :data="pieData" title="项目阶段分布" />
  <LineChart :data="lineData" :x-axis="months" smooth area />
  <BarChart :data="barData" :x-axis="months" />
  <GaugeChart :value="gaugeValue" title="设备在线率" :max="100" :target="90" />
</template>
```

## BaseChart 基础组件

最基础的封装，接收完整 ECharts option。一般业务直接使用具体子组件，仅在需要完全自定义 option 时使用 BaseChart。

### Props

| 名称      | 类型                | 默认值  | 说明                          |
| --------- | ------------------- | ------- | ----------------------------- |
| option    | `EChartsOption`     | -       | ECharts 配置，为空时显示 EmptyState |
| loading   | `boolean`           | `false` | 是否 loading（显示 a-spin）   |
| height    | `number \| string`  | `320`   | 图表高度                      |
| theme     | `string`            | `'vibe'`| 主题名，默认 vibe 自定义主题  |

### Expose

| 方法           | 返回值                | 说明                          |
| -------------- | --------------------- | ----------------------------- |
| `resize()`     | `void`                | 手动触发 resize              |
| `getInstance()`| `vue-echarts 实例`    | 获取 vue-echarts 实例（含 `.chart` 原生 ECharts 实例） |

### 示例

```vue
<script setup lang="ts">
import { ref } from 'vue'
import type { EChartsOption } from 'echarts'
import { BaseChart } from '@/components/charts'

const option = ref<EChartsOption>({
  xAxis: { type: 'category', data: ['A', 'B', 'C'] },
  yAxis: { type: 'value' },
  series: [{ type: 'bar', data: [10, 20, 30] }]
})
</script>

<template>
  <BaseChart ref="chartRef" :option="option" :height="360" />
</template>
```

## PieChart 饼图

### Props

| 名称           | 类型                      | 默认值  | 说明                       |
| -------------- | ------------------------- | ------- | -------------------------- |
| data           | `{ name: string; value: number }[]` | -    | 数据项                     |
| title          | `string`                  | -       | 标题                       |
| legendVisible  | `boolean`                 | `true`  | 是否显示图例               |
| ring           | `boolean`                 | `true`  | 是否环形图（false 为实心饼图） |

```vue
<PieChart :data="pieData" title="项目阶段分布" :ring="false" />
```

## LineChart 折线图

### Props

| 名称   | 类型                                    | 默认值   | 说明                       |
| ------ | --------------------------------------- | -------- | -------------------------- |
| data   | `{ name: string; data: number[] }[]`    | -        | 系列                       |
| xAxis  | `string[]`                              | -        | X 轴类目                   |
| smooth | `boolean`                               | `false`  | 是否平滑曲线               |
| area   | `boolean`                               | `false`  | 是否面积图                 |
| stack  | `boolean`                               | `false`  | 是否堆叠                   |

```vue
<LineChart :data="lineData" :x-axis="months" smooth area stack />
```

## BarChart 柱状图

### Props

| 名称        | 类型                                    | 默认值   | 说明                |
| ----------- | --------------------------------------- | -------- | ------------------- |
| data        | `{ name: string; data: number[] }[]`   | -        | 系列                |
| xAxis       | `string[]`                              | -        | X 轴类目            |
| horizontal  | `boolean`                               | `false`  | 是否横向条形图      |
| stack       | `boolean`                               | `false`  | 是否堆叠            |

```vue
<BarChart :data="barData" :x-axis="months" horizontal />
```

## StackedChart 堆叠图

### Props

| 名称   | 类型                                    | 默认值  | 说明                       |
| ------ | --------------------------------------- | ------- | -------------------------- |
| data   | `{ name: string; data: number[] }[]`    | -       | 系列（自动堆叠）           |
| xAxis  | `string[]`                              | -       | X 轴类目                   |
| type   | `'bar' \| 'line'`                       | `'bar'` | 堆叠类型                   |

```vue
<StackedChart :data="stackData" :x-axis="months" type="line" />
```

## FunnelChart 漏斗图

### Props

| 名称   | 类型                                          | 默认值         | 说明       |
| ------ | --------------------------------------------- | -------------- | ---------- |
| data   | `{ name: string; value: number }[]`           | -              | 数据项     |
| sort   | `'descending' \| 'ascending' \| 'none'`      | `'descending'` | 排序方式   |

```vue
<FunnelChart :data="funnelData" sort="descending" />
```

## MapChart 地图

### Props

| 名称     | 类型                                  | 默认值     | 说明                          |
| -------- | ------------------------------------- | ---------- | ----------------------------- |
| data     | `{ name: string; value: number }[]`   | -          | 区域数据（name 须与 GeoJSON 对齐） |
| mapType  | `'china' \| 'world'`                  | `'china'`  | 地图类型                      |
| roam     | `boolean`                             | `false`    | 是否开启缩放/平移             |

```vue
<MapChart :data="regionData" map-type="china" roam />
```

> name 取值示例：`北京市`、`上海市`、`广东省`、`浙江省`（与阿里 DataV GeoJSON 一致）。

## RadarChart 雷达图

### Props

| 名称       | 类型                                    | 默认值 | 说明              |
| ---------- | --------------------------------------- | ------ | ----------------- |
| data       | `{ name: string; value: number[] }[]`  | -      | 系列              |
| indicators | `{ name: string; max: number }[]`      | -      | 雷达维度指示器    |

```vue
<RadarChart :data="radarData" :indicators="indicators" />
```

## GaugeChart 仪表盘

### Props

| 名称    | 类型     | 默认值  | 说明                       |
| ------- | -------- | ------- | -------------------------- |
| value   | `number` | -       | 当前值                     |
| title   | `string` | -       | 标题                       |
| min     | `number` | `0`     | 最小值                     |
| max     | `number` | `100`   | 最大值                     |
| target  | `number` | -       | 目标值（绘制目标刻度标记） |

```vue
<GaugeChart :value="76" title="设备在线率" :max="100" :target="90" />
```

> 进度条颜色按比值自动切换：≥90% 绿、≥60% 蓝、≥30% 黄、<30% 红（与状态色语义对齐）。

## 主题与调色盘

```typescript
import { vibeColors, vibeStatusColors, vibeTheme } from '@/components/charts'

// 10 色调色盘
vibeColors // ['#1677FF', '#52C41A', '#FAAD14', '#FF4D4F', '#722ED1', ...]

// 状态色映射（按业务状态取色）
vibeStatusColors.success    // '#52C41A'
vibeStatusColors.processing // '#1677FF'
vibeStatusColors.exception  // '#FF4D4F'

// 主题对象（已通过 echarts.registerTheme('vibe', vibeTheme) 注册）
vibeTheme
```

业务页面在自定义 ECharts option 时，可直接复用调色盘与状态色，保持视觉统一。

## 注意事项

1. **按需引入**：BaseChart 在模块加载时通过 `echarts/core` 的 `use()` 注册所需图表/组件，未引入的图表类型不可用（当前已覆盖 8 类图表所需的全部模块）。
2. **响应式 resize**：vue-echarts 的 `autoresize` 基于 `resize-detector` 监听容器尺寸变化，等价于监听窗口 resize，无需业务自行绑定 `window.resize`。
3. **资源释放**：vue-echarts 在组件 `onUnmounted` 自动调用 `dispose`，无需业务手动释放。
4. **空数据**：所有子组件在 data 为空时返回 `null` option，BaseChart 自动渲染 EmptyState，避免空白图表。
5. **地图数据**：MapChart 首次渲染需联网拉取 GeoJSON（约 200KB），离线环境会降级为 EmptyState 提示。
6. **主题切换**：默认 `theme="vibe"`，如需暗色主题可额外 `registerTheme` 后通过 `theme` prop 指定。
