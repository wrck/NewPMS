import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'node:path'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'
// vite-plugin-style-import 已安装；Ant Design Vue 4.x 使用 CSS-in-JS，
// 样式由 AntDesignVueResolver 自动处理，无需再激活 style-import 插件以避免路径冲突。

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const port = Number(env.VITE_PORT) || 5173

  return {
    base: '/',
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src')
      }
    },
    css: {
      preprocessorOptions: {
        less: {
          javascriptEnabled: true,
          // 仅作为 less 全局变量备用，主色由 ConfigProvider token 控制
          additionalData: `@import "@/styles/variables.less";`
        }
      }
    },
    plugins: [
      vue(),
      Components({
        resolvers: [
          // Ant Design Vue 4.x 组件按需自动导入（样式由 CSS-in-JS 自动注入）
          AntDesignVueResolver({
            importStyle: false,
            resolveIcons: true
          })
        ],
        dts: 'src/components.d.ts',
        dirs: ['src/components']
      })
    ],
    server: {
      host: '0.0.0.0',
      port,
      open: true,
      proxy: {
        '/api': {
          target: env.VITE_API_TARGET || 'http://localhost:8080',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '/api')
        }
      }
    },
    build: {
      target: 'es2015',
      outDir: 'dist',
      sourcemap: false,
      chunkSizeWarningLimit: 1500,
      rollupOptions: {
        output: {
          manualChunks: {
            vue: ['vue', 'vue-router', 'pinia'],
            antd: ['ant-design-vue', '@ant-design/icons-vue'],
            echarts: ['echarts'],
            gantt: ['dhtmlx-gantt']
          }
        }
      }
    }
  }
})
