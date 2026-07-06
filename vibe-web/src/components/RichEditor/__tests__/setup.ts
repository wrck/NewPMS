/**
 * Vitest 全局 setup - RichEditor 测试共用
 * - 复用项目 polyfill（matchMedia / getComputedStyle / ResizeObserver）
 * - 注册 Ant Design Vue 插件（RichEditor 内部使用 message.error 提示）
 * - 安装 pinia（RichEditor 依赖 useUserStore().token）
 *
 * 注：wangEditor 5 在 jsdom 下无法完整初始化（依赖 Selection / Range API），
 * 测试中通过 stub Editor / Toolbar 组件绕过该限制，专注于验证业务逻辑。
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
