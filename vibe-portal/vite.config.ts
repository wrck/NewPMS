import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { VantResolver } from '@vant/auto-import-resolver'
import path from 'node:path'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_TARGET || 'http://localhost:8080'
  // 端口优先级：环境变量 VITE_PORT > .env.development 中 VITE_PORT > 默认 5175
  // 集中配置见项目根目录 .env.ports（start.ps1 启动时会把 VIBE_PORTAL_PORT 注入为 VITE_PORT）
  const port = Number(env.VITE_PORT) || 5175

  return {
    plugins: [
      vue(),
      Components({
        resolvers: [VantResolver()],
        dts: 'src/types/components.d.ts'
      })
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src')
      }
    },
    server: {
      host: '0.0.0.0',
      port,
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/api/, '/api')
        }
      }
    },
    build: {
      target: 'es2015',
      cssCodeSplit: true,
      sourcemap: false,
      rollupOptions: {
        output: {
          chunkFileNames: 'assets/js/[name]-[hash].js',
          entryFileNames: 'assets/js/[name]-[hash].js',
          assetFileNames: 'assets/[ext]/[name]-[hash].[ext]',
          manualChunks: {
            vant: ['vant'],
            vue: ['vue', 'vue-router', 'pinia']
          }
        }
      }
    }
  }
})
