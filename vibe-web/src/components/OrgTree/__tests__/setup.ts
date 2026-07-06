/**
 * Vitest 全局 setup - OrgTree 测试共用
 * - 复用 polyfill（matchMedia / getComputedStyle / ResizeObserver）
 * - 注册 Ant Design Vue 插件，使模板中 a-* 组件可解析
 * - 安装 pinia（OrgTree 暂未直接依赖 userStore，但保持一致性）
 *
 * 注：本文件独立于 CrudTable/FormModal 的 setup.ts，避免互相影响；
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

/* ============ 注册 Antd + Pinia ============ */
config.global.plugins = [Antd]
setActivePinia(createPinia())
