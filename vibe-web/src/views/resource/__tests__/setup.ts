/**
 * Vitest 全局 setup - 资源管理视图测试共用
 */
import { config } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'

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

config.global.plugins = [Antd]
setActivePinia(createPinia())
