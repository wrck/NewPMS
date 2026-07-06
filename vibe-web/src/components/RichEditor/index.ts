/**
 * RichEditor 富文本编辑器组件入口
 *
 * 对应 spec：阶段三 前端通用组件抽取 - Task 15
 * 基于 wangEditor 5 + @wangeditor/editor-for-vue 封装
 *
 * 用法：
 *   1. 由于 vite.config.ts 已配置 unplugin-vue-components 自动注册
 *      src/components 下的组件，模板中可直接使用 <RichEditor />。
 *   2. 显式 import：import RichEditor from '@/components/RichEditor/index.vue'
 *   3. 命名导出：import { RichEditor } from '@/components/RichEditor'
 */
import RichEditor from './index.vue'

export { default as RichEditor } from './index.vue'
export type {
  RichEditorProps,
  RichEditorEmits,
  RichEditorExpose,
  EditorMode,
  OutputFormat,
  PresignResponse
} from './types'
export default RichEditor
