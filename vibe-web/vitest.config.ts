/**
 * Vitest 配置（spec 阶段三 Task 12 引入）
 * - 环境：jsdom（Vue 3 + ant-design-vue 需要 DOM）
 * - 别名：复用 vite.config.ts 的 @ -> src
 * - 覆盖率：v8 provider，目标 src/components
 */
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'node:path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  css: {
    preprocessorOptions: {
      less: {
        javascriptEnabled: true,
        additionalData: `@import "@/styles/variables.less";`
      }
    }
  },
  test: {
    environment: 'jsdom',
    globals: false,
    setupFiles: ['src/components/CrudTable/__tests__/setup.ts'],
    include: ['src/**/__tests__/**/*.test.ts', 'src/**/*.spec.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      include: ['src/components/CrudTable/index.vue'],
      exclude: [
        'src/**/*.d.ts',
        'src/components/index.ts',
        'src/components/CrudTable/types.ts',
        'src/components/CrudTable/__tests__/**'
      ]
    }
  }
})
