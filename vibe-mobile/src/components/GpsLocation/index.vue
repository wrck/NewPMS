<script setup lang="ts">
/**
 * GPS 定位组件（设计文档 3.4.2 签到流程）
 * - 调用 navigator.geolocation / 高德 SDK 获取当前位置
 * - 渲染地图，显示与客户现场的距离
 * - 校验是否在允许签到范围内
 */
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { showToast, showLoadingToast, closeToast } from 'vant'
import {
  getCurrentLocation,
  getDistance,
  renderMap,
  type LocationResult
} from '@/utils/gps'

interface Props {
  /** 客户现场经度 */
  targetLongitude?: number
  /** 客户现场纬度 */
  targetLatitude?: number
  /** 允许签到半径（米），默认 500m */
  allowRadius?: number
  /** 是否显示地图 */
  showMap?: boolean
  /** 是否自动定位（挂载即定位） */
  autoLocate?: boolean
  /** 地图缩放级别 */
  zoom?: number
}

const props = withDefaults(defineProps<Props>(), {
  targetLongitude: undefined,
  targetLatitude: undefined,
  allowRadius: 500,
  showMap: true,
  autoLocate: true,
  zoom: 16
})

const emit = defineEmits<{
  (e: 'located', payload: { location: LocationResult; distance: number; inRange: boolean }): void
  (e: 'in-range', inRange: boolean): void
  (e: 'error', message: string): void
}>()

const state = reactive({
  loading: false,
  located: false,
  location: undefined as LocationResult | undefined,
  distance: undefined as number | undefined,
  error: '' as string
})

const mapContainer = ref<HTMLDivElement | null>(null)
let mapInstance: any = null

/** 与客户现场的距离（米） */
const distance = computed(() => state.distance)

/** 是否在允许范围内 */
const inRange = computed(() => {
  if (state.distance === undefined) return false
  return state.distance <= props.allowRadius
})

const distanceText = computed(() => {
  if (state.distance === undefined) return '未知'
  if (state.distance < 1000) return `${state.distance}m`
  return `${(state.distance / 1000).toFixed(2)}km`
})

const rangeText = computed(() => {
  if (state.distance === undefined) return ''
  if (inRange.value) return `在允许范围内（${state.distance}m ≤ ${props.allowRadius}m）`
  return `超出允许范围（${state.distance}m > ${props.allowRadius}m）`
})

/** 定位 */
async function locate(): Promise<LocationResult | undefined> {
  state.loading = true
  state.error = ''
  const loading = showLoadingToast({ message: '定位中...', forbidClick: true, duration: 0 })
  try {
    const loc = await getCurrentLocation()
    state.location = loc
    state.located = true
    if (props.targetLongitude != null && props.targetLatitude != null) {
      state.distance = getDistance(
        loc.longitude,
        loc.latitude,
        props.targetLongitude,
        props.targetLatitude
      )
    }
    emit('located', {
      location: loc,
      distance: state.distance ?? -1,
      inRange: inRange.value
    })
    emit('in-range', inRange.value)
    if (props.showMap && mapContainer.value) await renderMapToContainer(loc)
    return loc
  } catch (err: any) {
    state.error = err?.message || '定位失败'
    showToast(state.error)
    emit('error', state.error)
    return undefined
  } finally {
    closeToast()
    state.loading = false
  }
}

async function renderMapToContainer(loc: LocationResult): Promise<void> {
  if (!mapContainer.value) return
  if (mapInstance) {
    mapInstance.destroy?.()
    mapInstance = null
  }
  try {
    mapInstance = await renderMap(mapContainer.value, loc.longitude, loc.latitude, {
      zoom: props.zoom,
      markerLabel: '我的位置'
    })
  } catch (e) {
    console.warn('[GpsLocation] map render failed', e)
  }
}

function reset(): void {
  state.located = false
  state.location = undefined
  state.distance = undefined
  state.error = ''
  if (mapInstance) {
    mapInstance.destroy?.()
    mapInstance = null
  }
}

defineExpose({ locate, reset, location: computed(() => state.location), distance, inRange })

onMounted(() => {
  if (props.autoLocate) locate()
})

onBeforeUnmount(() => {
  if (mapInstance) {
    mapInstance.destroy?.()
    mapInstance = null
  }
})

// 目标点变化时重新计算距离
watch(
  () => [props.targetLongitude, props.targetLatitude],
  () => {
    if (state.location && props.targetLongitude != null && props.targetLatitude != null) {
      state.distance = getDistance(
        state.location.longitude,
        state.location.latitude,
        props.targetLongitude,
        props.targetLatitude
      )
      emit('in-range', inRange.value)
    }
  }
)
</script>

<template>
  <div class="gps-location">
    <div class="gps-location__header">
      <van-icon name="location-o" class="gps-location__icon" />
      <span class="gps-location__title">GPS 定位</span>
      <van-button
        size="small"
        type="primary"
        :loading="state.loading"
        class="touchable"
        @click="locate"
      >
        重新定位
      </van-button>
    </div>

    <div v-if="showMap" ref="mapContainer" class="gps-location__map">
      <div v-if="!state.located && !state.loading" class="gps-location__placeholder">
        <van-icon name="location" size="40" />
        <p>{{ state.error || '点击上方按钮开始定位' }}</p>
      </div>
    </div>

    <div v-if="state.located && state.location" class="gps-location__info">
      <div class="gps-location__row">
        <span class="label">经纬度：</span>
        <span class="value">
          {{ state.location.longitude.toFixed(6) }}, {{ state.location.latitude.toFixed(6) }}
        </span>
      </div>
      <div v-if="state.location.address" class="gps-location__row">
        <span class="label">地址：</span>
        <span class="value">{{ state.location.address }}</span>
      </div>
      <div v-if="distance !== undefined" class="gps-location__row">
        <span class="label">距客户现场：</span>
        <span class="value" :class="{ 'in-range': inRange, 'out-range': !inRange }">
          {{ distanceText }}
        </span>
      </div>
      <div v-if="rangeText" class="gps-location__range" :class="{ 'in-range': inRange, 'out-range': !inRange }">
        <van-icon :name="inRange ? 'success' : 'warning-o'" />
        <span>{{ rangeText }}</span>
      </div>
    </div>

    <div v-if="state.error && !state.located" class="gps-location__error">
      <van-icon name="warning-o" />
      <span>{{ state.error }}</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.gps-location {
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 16px;
  margin-bottom: 12px;

  &__header {
    display: flex;
    align-items: center;
    margin-bottom: 12px;
  }

  &__icon {
    font-size: 20px;
    color: var(--brand-primary);
    margin-right: 6px;
  }

  &__title {
    flex: 1;
    font-size: 15px;
    font-weight: 600;
  }

  &__map {
    width: 100%;
    height: 180px;
    border-radius: var(--radius-md);
    background: #f0f2f5;
    overflow: hidden;
    margin-bottom: 12px;
    position: relative;
  }

  &__placeholder {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: var(--color-text-placeholder);

    p {
      margin-top: 8px;
      font-size: 13px;
    }
  }

  &__info {
    font-size: 13px;
    color: var(--color-text-regular);
  }

  &__row {
    display: flex;
    padding: 4px 0;

    .label {
      color: var(--color-text-secondary);
      flex-shrink: 0;
    }

    .value {
      flex: 1;
      word-break: break-all;

      &.in-range {
        color: var(--color-success);
        font-weight: 600;
      }
      &.out-range {
        color: var(--color-danger);
        font-weight: 600;
      }
    }
  }

  &__range {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-top: 6px;
    padding: 6px 10px;
    border-radius: var(--radius-sm);
    font-size: 13px;

    &.in-range {
      background: rgba(82, 196, 26, 0.08);
      color: var(--color-success);
    }
    &.out-range {
      background: rgba(255, 77, 79, 0.08);
      color: var(--color-danger);
    }
  }

  &__error {
    display: flex;
    align-items: center;
    gap: 6px;
    color: var(--color-danger);
    font-size: 13px;
    padding: 8px 0;
  }
}
</style>
