/**
 * AMap 组件单元测试（spec 阶段三 Task 19）
 *
 * 覆盖范围：
 *   - 组件渲染（容器、loading、error）
 *   - props 传递（center / zoom / height）
 *   - 挂载后调用 loadAMap 并创建 Map 实例
 *   - map-ready 事件
 *   - markers 渲染（Marker / MarkerCluster / map.add）
 *   - map-click 事件触发
 *   - expose 方法（setCenter / setZoom / getMapInstance / renderMarkers）
 *   - loader 失败显示 error
 *   - markers 变化时重新渲染
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import AMap from '../index.vue'
import { destroyAMap } from '../loader'

/* ============ Mock @amap/amap-jsapi-loader ============ */
const mocks = vi.hoisted(() => {
  /** 共享状态：跨 mock 实例访问，便于断言 */
  const state: {
    mapInstance: any
    markerInstances: any[]
    clusterInstance: any
  } = {
    mapInstance: null,
    markerInstances: [],
    clusterInstance: null
  }

  /** 创建一个带 on 回调缓存的 mock 对象 */
  const createEventTarget = () => {
    const target: any = {
      on: vi.fn((event: string, cb: Function) => {
        target[`__cb_${event}`] = cb
      })
    }
    return target
  }

  /** 创建地图实例 mock */
  const createMapInstance = () => {
    const inst = {
      setCenter: vi.fn(),
      setZoom: vi.fn(),
      addControl: vi.fn(),
      add: vi.fn(),
      remove: vi.fn(),
      on: vi.fn((event: string, cb: Function) => {
        ;(inst as any)[`__cb_${event}`] = cb
      }),
      destroy: vi.fn()
    }
    state.mapInstance = inst
    return inst
  }

  const AMapMock = {
    Map: vi.fn<[any, any]>(() => createMapInstance()),
    Marker: vi.fn<[any]>(() => {
      const m = createEventTarget()
      state.markerInstances.push(m)
      return m
    }),
    Pixel: vi.fn<[number, number]>((x, y) => ({ x, y })),
    InfoWindow: vi.fn<[any]>(() => ({ open: vi.fn() })),
    MarkerCluster: vi.fn<[any, any[], any]>((map, markers, opts) => {
      const c = { setMarkers: vi.fn(), __markers: markers, __opts: opts }
      state.clusterInstance = c
      return c
    }),
    ToolBar: vi.fn<[]>(() => undefined),
    Scale: vi.fn<[]>(() => undefined)
  }

  const loadMock = vi.fn(() => Promise.resolve(AMapMock))

  return {
    loadMock,
    AMapMock,
    state,
    resetState: () => {
      state.mapInstance = null
      state.markerInstances = []
      state.clusterInstance = null
    }
  }
})

vi.mock('@amap/amap-jsapi-loader', () => ({
  default: { load: mocks.loadMock },
  load: mocks.loadMock
}))

/* ============ 测试用例 ============ */
describe('AMap', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.resetState()
    // 清空 loader 单例缓存，确保每次测试都重新调用 loadMock
    destroyAMap()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('渲染', () => {
    it('渲染地图容器与画布', async () => {
      const wrapper = mount(AMap)
      await flushPromises()
      expect(wrapper.find('.amap-container').exists()).toBe(true)
      expect(wrapper.find('.amap-canvas').exists()).toBe(true)
    })

    it('height 应用到画布样式', async () => {
      const wrapper = mount(AMap, { props: { height: '320px' } })
      await flushPromises()
      const style = wrapper.find('.amap-canvas').attributes('style') || ''
      expect(style).toContain('320px')
    })

    it('默认 loading 状态在地图就绪后关闭', async () => {
      const wrapper = mount(AMap)
      // 挂载瞬间 loading 应为 true
      expect(wrapper.find('.amap-loading').exists()).toBe(true)
      await flushPromises()
      // 加载完成后 loading 关闭
      expect(wrapper.find('.amap-loading').exists()).toBe(false)
    })
  })

  describe('加载与初始化', () => {
    it('挂载后调用 loadAMap 并创建 Map 实例', async () => {
      mount(AMap)
      await flushPromises()
      expect(mocks.loadMock).toHaveBeenCalledTimes(1)
      expect(mocks.AMapMock.Map).toHaveBeenCalledTimes(1)
    })

    it('默认 center/zoom 应用到 Map 构造参数', async () => {
      mount(AMap)
      await flushPromises()
      const opts = mocks.AMapMock.Map.mock.calls[0][1]
      expect(opts.zoom).toBe(11)
      expect(opts.center).toEqual([116.397428, 39.90923])
      expect(opts.mapStyle).toBe('amap://styles/normal')
    })

    it('自定义 center/zoom/mapStyle 透传', async () => {
      mount(AMap, {
        props: {
          center: [121.473701, 31.230416],
          zoom: 13,
          mapStyle: 'amap://styles/dark'
        }
      })
      await flushPromises()
      const opts = mocks.AMapMock.Map.mock.calls[0][1]
      expect(opts.zoom).toBe(13)
      expect(opts.center).toEqual([121.473701, 31.230416])
      expect(opts.mapStyle).toBe('amap://styles/dark')
    })

    it('showToolBar/showScale 默认调用 addControl', async () => {
      mount(AMap)
      await flushPromises()
      expect(mocks.AMapMock.ToolBar).toHaveBeenCalledTimes(1)
      expect(mocks.AMapMock.Scale).toHaveBeenCalledTimes(1)
      expect(mocks.state.mapInstance.addControl).toHaveBeenCalledTimes(2)
    })

    it('showToolBar=false 时不创建 ToolBar', async () => {
      mount(AMap, { props: { showToolBar: false, showScale: false } })
      await flushPromises()
      expect(mocks.AMapMock.ToolBar).not.toHaveBeenCalled()
      expect(mocks.AMapMock.Scale).not.toHaveBeenCalled()
    })

    it('emit map-ready 事件并回传地图实例', async () => {
      const wrapper = mount(AMap)
      await flushPromises()
      const ready = wrapper.emitted('map-ready')
      expect(ready).toBeTruthy()
      expect(ready![0][0]).toBe(mocks.state.mapInstance)
    })
  })

  describe('标记渲染', () => {
    it('markers 为空时不创建 Marker', async () => {
      mount(AMap, { props: { markers: [] } })
      await flushPromises()
      expect(mocks.AMapMock.Marker).not.toHaveBeenCalled()
    })

    it('markers 渲染创建对应数量的 Marker', async () => {
      const markers = [
        { id: 1, lng: 116.4, lat: 39.9, title: 'A' },
        { id: 2, lng: 116.5, lat: 39.95, title: 'B' }
      ]
      mount(AMap, { props: { markers } })
      await flushPromises()
      expect(mocks.AMapMock.Marker).toHaveBeenCalledTimes(2)
      // 校验 Marker 构造参数 position
      const call1 = mocks.AMapMock.Marker.mock.calls[0][0]
      expect(call1.position).toEqual([116.4, 39.9])
    })

    it('cluster=true 且 markers>1 时使用 MarkerCluster', async () => {
      const markers = [
        { lng: 116.4, lat: 39.9, title: 'A' },
        { lng: 116.5, lat: 39.95, title: 'B' }
      ]
      mount(AMap, { props: { markers, cluster: true } })
      await flushPromises()
      expect(mocks.AMapMock.MarkerCluster).toHaveBeenCalledTimes(1)
      expect(mocks.state.clusterInstance).toBeTruthy()
    })

    it('cluster=false 时调用 map.add 添加标记', async () => {
      const markers = [{ lng: 116.4, lat: 39.9, title: 'A' }]
      mount(AMap, { props: { markers, cluster: false } })
      await flushPromises()
      expect(mocks.AMapMock.MarkerCluster).not.toHaveBeenCalled()
      expect(mocks.state.mapInstance.add).toHaveBeenCalledTimes(1)
      const added = mocks.state.mapInstance.add.mock.calls[0][0]
      expect(Array.isArray(added)).toBe(true)
      expect(added.length).toBe(1)
    })

    it('单个 marker 默认 cluster 不生效（length<=1）走 map.add', async () => {
      const markers = [{ lng: 116.4, lat: 39.9, title: 'A' }]
      mount(AMap, { props: { markers, cluster: true } })
      await flushPromises()
      // 单个 marker 不触发 MarkerCluster
      expect(mocks.AMapMock.MarkerCluster).not.toHaveBeenCalled()
      expect(mocks.state.mapInstance.add).toHaveBeenCalled()
    })

    it('infoWindow 配置时创建 InfoWindow', async () => {
      const markers = [
        {
          lng: 116.4,
          lat: 39.9,
          title: 'A',
          infoWindow: { content: '<div>hello</div>' }
        }
      ]
      mount(AMap, { props: { markers, cluster: false } })
      await flushPromises()
      expect(mocks.AMapMock.InfoWindow).toHaveBeenCalledTimes(1)
      const opts = mocks.AMapMock.InfoWindow.mock.calls[0][0]
      expect(opts.content).toBe('<div>hello</div>')
    })

    it('markers 变化时重新渲染', async () => {
      const wrapper = mount(AMap, { props: { markers: [] } })
      await flushPromises()
      expect(mocks.AMapMock.Marker).not.toHaveBeenCalled()
      await wrapper.setProps({
        markers: [{ lng: 116.4, lat: 39.9, title: 'A' }]
      })
      await flushPromises()
      expect(mocks.AMapMock.Marker).toHaveBeenCalled()
    })

    it('draggable marker 注册 dragend 事件', async () => {
      const markers = [{ lng: 116.4, lat: 39.9, title: 'A', draggable: true }]
      mount(AMap, { props: { markers, cluster: false } })
      await flushPromises()
      const m = mocks.state.markerInstances[0]
      expect(m.__cb_dragend).toBeTypeOf('function')
    })
  })

  describe('事件', () => {
    it('map-click 事件触发并传递 lng/lat', async () => {
      const wrapper = mount(AMap)
      await flushPromises()
      const cb = mocks.state.mapInstance.__cb_click
      expect(cb).toBeTypeOf('function')
      cb({ lnglat: { getLng: () => 116.4, getLat: () => 39.9 } })
      const events = wrapper.emitted('map-click')
      expect(events).toBeTruthy()
      expect(events![0]).toEqual([116.4, 39.9])
    })

    it('marker-click 事件触发', async () => {
      const markers = [{ id: 7, lng: 116.4, lat: 39.9, title: 'A' }]
      const wrapper = mount(AMap, { props: { markers, cluster: false } })
      await flushPromises()
      const m = mocks.state.markerInstances[0]
      const cb = m.__cb_click
      expect(cb).toBeTypeOf('function')
      cb()
      const events = wrapper.emitted('marker-click')
      expect(events).toBeTruthy()
      expect(events![0][0]).toMatchObject({ id: 7, lng: 116.4 })
    })
  })

  describe('Expose 方法', () => {
    it('setCenter / setZoom 调用地图实例', async () => {
      const wrapper = mount(AMap)
      await flushPromises()
      const vm = wrapper.vm as any
      vm.setCenter(121.4, 31.2, 12)
      expect(mocks.state.mapInstance.setCenter).toHaveBeenCalledWith([121.4, 31.2])
      expect(mocks.state.mapInstance.setZoom).toHaveBeenCalledWith(12)
      vm.setZoom(15)
      expect(mocks.state.mapInstance.setZoom).toHaveBeenLastCalledWith(15)
    })

    it('setCenter 不传 zoom 时不调用 setZoom', async () => {
      const wrapper = mount(AMap)
      await flushPromises()
      const vm = wrapper.vm as any
      vm.setCenter(121.4, 31.2)
      expect(mocks.state.mapInstance.setCenter).toHaveBeenCalledWith([121.4, 31.2])
      expect(mocks.state.mapInstance.setZoom).not.toHaveBeenCalled()
    })

    it('getMapInstance 返回当前地图实例', async () => {
      const wrapper = mount(AMap)
      await flushPromises()
      const vm = wrapper.vm as any
      expect(vm.getMapInstance()).toBe(mocks.state.mapInstance)
    })

    it('renderMarkers 可重复调用重新渲染', async () => {
      const markers = [{ lng: 116.4, lat: 39.9, title: 'A' }]
      const wrapper = mount(AMap, { props: { markers, cluster: false } })
      await flushPromises()
      const vm = wrapper.vm as any
      const before = mocks.AMapMock.Marker.mock.calls.length
      await vm.renderMarkers()
      await flushPromises()
      expect(mocks.AMapMock.Marker.mock.calls.length).toBeGreaterThan(before)
    })
  })

  describe('异常处理', () => {
    it('loader 失败时显示 error 提示', async () => {
      mocks.loadMock.mockRejectedValueOnce(new Error('load failed'))
      const wrapper = mount(AMap)
      await flushPromises()
      expect(wrapper.find('.amap-error').exists()).toBe(true)
      expect(wrapper.text()).toContain('load failed')
      expect(wrapper.find('.amap-loading').exists()).toBe(false)
    })

    it('loader 失败且无 message 时回退默认文案', async () => {
      mocks.loadMock.mockRejectedValueOnce(new Error())
      const wrapper = mount(AMap)
      await flushPromises()
      expect(wrapper.find('.amap-error').exists()).toBe(true)
      expect(wrapper.text()).toContain('地图加载失败')
    })

    it('卸载时调用 mapInstance.destroy', async () => {
      const wrapper = mount(AMap)
      await flushPromises()
      const inst = mocks.state.mapInstance
      wrapper.unmount()
      expect(inst.destroy).toHaveBeenCalledTimes(1)
    })
  })
})
