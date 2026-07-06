# AMap 高德地图组件

基于 `@amap/amap-jsapi-loader` 封装的 Vue 3 + TypeScript 地图组件（spec 阶段三 Task 19）。

## 文件结构

```
AMap/
├── index.vue      # 主组件
├── loader.ts      # JSAPI 单例加载器
├── types.ts       # 类型定义
├── index.ts       # 聚合导出
└── README.md      # 本文档
```

## 安装与配置

### 1. 环境变量

在 `vibe-web/.env.development` 或 `.env.production` 配置：

```bash
VITE_AMAP_KEY=your_amap_key_here          # Web 端 Key
VITE_AMAP_SECURITY_CODE=your_security_code_here  # 安全密钥
VITE_AMAP_VERSION=2.0                     # JSAPI 版本
```

Key 申请：https://console.amap.com/dev/key/app

### 2. 自动注册

`vite.config.ts` 已配置 `unplugin-vue-components`，`src/components` 目录下组件自动注册，
模板中可直接使用 `<AMap />` 无需手动 import。

## 用法

### 基础用法

```vue
<template>
  <AMap
    :center="[116.397428, 39.90923]"
    :zoom="11"
    :markers="markers"
    cluster
    height="500px"
    @map-ready="onReady"
    @marker-click="onMarkerClick"
  />
</template>

<script setup lang="ts">
import type { AMapMarker } from '@/components/AMap'

const markers: AMapMarker[] = [
  { id: 1, lng: 116.397428, lat: 39.90923, title: '天安门' },
  { id: 2, lng: 116.41, lat: 39.91, title: '点 B', infoWindow: { content: '<div>B</div>' } }
]

const onReady = (map: any) => console.log('地图就绪', map)
const onMarkerClick = (m: AMapMarker) => console.log('点击标记', m)
</script>
```

### 通过 ref 调用 expose 方法

```vue
<template>
  <AMap ref="mapRef" :markers="markers" />
  <a-button @click="moveTo">移动到上海</a-button>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const mapRef = ref()
const moveTo = () => {
  // setCenter / setZoom / getMapInstance / renderMarkers
  mapRef.value?.setCenter(121.473701, 31.230416, 12)
}
</script>
```

### 直接调用 loader

```ts
import { loadAMap } from '@/components/AMap'

const AMap = await loadAMap()
const map = new AMap.Map(container, { zoom: 11, center: [116.39, 39.9] })
```

## Props

| 名称        | 类型             | 默认值                  | 说明                |
| ----------- | ---------------- | ----------------------- | ------------------- |
| center      | [number, number] | [116.397428, 39.90923]  | 地图中心点 [lng,lat] |
| zoom        | number           | 11                      | 缩放级别            |
| markers     | AMapMarker[]     | []                      | 标记点列表          |
| cluster     | boolean          | true                    | 是否聚合            |
| height      | string           | '500px'                 | 容器高度            |
| showToolBar | boolean          | true                    | 显示工具条          |
| showScale   | boolean          | true                    | 显示比例尺          |
| mapStyle    | string           | 'amap://styles/normal'  | 地图样式 URL        |

## AMapMarker

| 字段       | 类型                              | 说明                          |
| ---------- | --------------------------------- | ----------------------------- |
| id         | string \| number                  | 业务标识                      |
| lng        | number                            | 经度                          |
| lat        | number                            | 纬度                          |
| title      | string                            | 悬浮提示                      |
| content   | string                            | HTML 标记内容                 |
| icon       | string \| object                  | 图标                          |
| draggable  | boolean                           | 可拖拽                        |
| offset     | [number, number]                  | 偏移                          |
| infoWindow | { content: string }              | 点击弹出的信息窗口            |
| data       | any                               | 自定义数据（事件回调原样透传） |

## Events

| 事件           | 参数                                          | 说明           |
| -------------- | --------------------------------------------- | -------------- |
| map-ready      | (map)                                        | 地图实例就绪   |
| marker-click   | (marker, markerInstance)                     | 标记点击       |
| map-click      | (lng, lat)                                   | 地图空白点击   |
| marker-dragend | (marker, lng, lat)                           | 标记拖拽结束   |

## Expose 方法

| 方法             | 签名                                   | 说明           |
| ---------------- | -------------------------------------- | -------------- |
| setCenter        | (lng, lat, zoom?) => void             | 设置中心点     |
| setZoom          | (zoom) => void                         | 设置缩放       |
| getMapInstance   | () => AMapInstance \| null            | 获取地图实例   |
| renderMarkers    | () => Promise<void>                    | 重新渲染标记   |

## 注意事项

- 未配置 `VITE_AMAP_KEY` 时 loader 会回退到 `demo_key`，仅用于本地联调，生产环境务必配置真实 Key。
- 高德官方未提供完整 TS 类型，`AMapInstance` / `MarkerInstance` 采用 `[key: string]: any` 最小定义。
- 组件卸载时会调用 `mapInstance.destroy()` 释放资源。
- 轻量替代方案见 `@/components/charts/MapChart.vue`（ECharts 地图，无需 Key）。
