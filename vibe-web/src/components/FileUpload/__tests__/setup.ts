/**
 * Vitest 全局 setup - FileUpload 测试共用
 *
 * 复用 CrudTable / FormModal 的 polyfill 思路：
 *   - matchMedia / getComputedStyle / ResizeObserver polyfill
 *   - 注册 Ant Design Vue 插件，使模板中 a-* 组件可解析
 *   - 安装 pinia（FileUpload 依赖 useUserStore）
 *   - mock IndexedDB（jsdom 不支持，断点续传测试需 mock）
 *
 * 注：本文件独立于其他组件 setup，避免互相影响；
 *     vitest.config.ts 中 setupFiles 已配置本文件。
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

/* ============ Mock createImageBitmap（jsdom 不支持） ============ */
;(globalThis as any).createImageBitmap = undefined

/* ============ 注册 Antd + Pinia ============ */
config.global.plugins = [Antd]
setActivePinia(createPinia())
