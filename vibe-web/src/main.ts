/**
 * 应用入口
 * - 创建 Vue 实例
 * - 挂载 Pinia / Router
 * - 引入全局样式（Ant Design Vue 4.x 使用 CSS-in-JS，无需整体引入样式）
 * - dayjs 中文 locale
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPersistedstate from 'pinia-plugin-persistedstate'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import relativeTime from 'dayjs/plugin/relativeTime'

import App from './App.vue'
import router from './router'
import './styles/global.less'

// dayjs 中文 + 相对时间
dayjs.locale('zh-cn')
dayjs.extend(relativeTime)

const app = createApp(App)

// Pinia + 持久化插件
const pinia = createPinia()
pinia.use(piniaPersistedstate)
app.use(pinia)

// 路由
app.use(router)

// Ant Design Vue（按需 + ConfigProvider 主题在 App.vue 中包裹）
// 此处整体注册以保证 message/notification/Modal 等静态方法可用；
// 组件仍可由 unplugin-vue-components 自动按需导入覆盖
app.use(Antd)

app.mount('#app')
