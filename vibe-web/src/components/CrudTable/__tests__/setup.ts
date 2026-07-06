/**
 * Vitest 全局 setup
 * - polyfill jsdom 缺失 API（matchMedia / getComputedStyle pseudoElt / ResizeObserver）
 * - 注册 Ant Design Vue 插件，使模板中 a-* 组件可解析
 * - 安装 pinia（CrudTable 依赖 useUserStore）
 *
 * 注意：测试中通过 vi.mock('@/stores/user') 已替换 useUserStore 实现，
 * 这里仍安装 pinia 以满足 createPinia() 初始化。
 */
import { config } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'

/* ============ Polyfill jsdom 缺失 API ============ */
// matchMedia: antd ResponsiveObserve 依赖
if (!window.matchMedia) {
  window.matchMedia = ((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false
  })) as any
}

// getComputedStyle: jsdom 不支持 pseudoElt 参数，antd vc-table 调用滚动条尺寸时使用
const origGetComputedStyle = window.getComputedStyle
window.getComputedStyle = ((elt: Element, pseudoElt?: string | null) => {
  if (pseudoElt) {
    // 返回空 CSSStyleDeclaration 兼容 antd 调用
    return { getPropertyValue: () => '' } as unknown as CSSStyleDeclaration
  }
  return origGetComputedStyle(elt)
}) as any

// ResizeObserver: antd 部分组件使用
class MockResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}
;(window as any).ResizeObserver = (window as any).ResizeObserver || MockResizeObserver

/* ============ 注册 Antd + Pinia ============ */
config.global.plugins = [Antd]
setActivePinia(createPinia())

