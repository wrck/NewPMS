/**
 * @wangeditor/editor-for-vue 类型声明 shim
 *
 * 原因：@wangeditor/editor-for-vue@5.1.12 的 package.json exports 字段
 * 仅声明了 import/require 子键，未声明 types 子键，导致 TypeScript 在
 * moduleResolution: bundler 模式下回退到 JS 文件，丢失类型。
 *
 * 实际类型定义见 node_modules/@wangeditor/editor-for-vue/dist/src/index.d.ts，
 * 这里重新声明，保证 RichEditor 组件获得完整类型支持。
 */
declare module '@wangeditor/editor-for-vue' {
  import type { DefineComponent, PropType } from 'vue'
  import type {
    IEditorConfig,
    IToolbarConfig,
    IDomEditor,
    SlateDescendant
  } from '@wangeditor/editor'

  export const Editor: DefineComponent<{
    /** 编辑器模式 */
    mode?: string
    /** 编辑器默认内容（Slate JSON） */
    defaultContent?: PropType<SlateDescendant[]>
    /** 编辑器默认 HTML */
    defaultHtml?: string
    /** 编辑器默认配置 */
    defaultConfig?: Partial<IEditorConfig>
    /** v-model 绑定的 HTML 字符串 */
    modelValue?: string
  }>

  export const Toolbar: DefineComponent<{
    /** 已创建的编辑器实例 */
    editor?: IDomEditor
    /** 编辑器模式 */
    mode?: string
    /** 工具栏配置 */
    defaultConfig?: Partial<IToolbarConfig>
  }>
}
