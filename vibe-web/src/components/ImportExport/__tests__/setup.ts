/**
 * Vitest 全局 setup - ImportExport 测试共用
 * - 复用 polyfill（matchMedia / getComputedStyle / ResizeObserver）
 * - 注册 Ant Design Vue 插件
 * - 安装 pinia（ImportExport 依赖 useUserStore）
 *
 * 注：本文件独立于其他 setup.ts，避免互相影响；
 * vitest.config.ts 中 setupFiles 同时配置多份 setup。
 */
import { config } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'

/* ============ Polyfill jsdom 缺失 API ============ */
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

const origGetComputedStyle = window.getComputedStyle
window.getComputedStyle = ((elt: Element, pseudoElt?: string | null) => {
  if (pseudoElt) {
    return { getPropertyValue: () => '' } as unknown as CSSStyleDeclaration
  }
  return origGetComputedStyle(elt)
}) as any

class MockResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}
;(window as any).ResizeObserver = (window as any).ResizeObserver || MockResizeObserver

/* ============ Polyfill Blob 下载 API（jsdom 未实现 URL.createObjectURL） ============ */
if (!(window.URL as any).createObjectURL) {
  ;(window.URL as any).createObjectURL = () => 'blob:fake-url'
}
if (!(window.URL as any).revokeObjectURL) {
  ;(window.URL as any).revokeObjectURL = () => undefined
}

/* ============ 注册 Antd + Pinia ============ */
config.global.plugins = [Antd]
setActivePinia(createPinia())
