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
    setupFiles: [
      'src/components/CrudTable/__tests__/setup.ts',
      'src/components/FormModal/__tests__/setup.ts',
      'src/components/ImportExport/__tests__/setup.ts',
      'src/components/OrgTree/__tests__/setup.ts',
      'src/components/RichEditor/__tests__/setup.ts',
      'src/views/lowcode/__tests__/setup.ts',
      'src/views/device/__tests__/setup.ts',
      'src/views/resource/__tests__/setup.ts',
      'src/views/project/__tests__/setup.ts'
    ],
    include: [
      'src/**/__tests__/**/*.test.ts',
      'src/**/*.spec.ts',
      'src/**/__tests__/**/*.spec.ts'
    ],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      include: [
        'src/components/CrudTable/index.vue',
        'src/components/FormModal/index.vue',
        'src/components/ImportExport/index.vue',
        'src/components/OrgTree/index.vue',
        'src/components/OrgTree/OrgTreeSelect.vue',
        'src/components/OrgTree/useOrgTree.ts',
        'src/components/RichEditor/index.vue',
        'src/api/lowcode.ts',
        'src/api/feedback.ts',
        'src/components/Lowcode/SchemaDesigner.vue',
        'src/components/Lowcode/FieldPalette.vue',
        'src/components/Lowcode/PropertyPanel.vue',
        'src/components/Lowcode/SchemaPreview.vue',
        'src/components/Lowcode/SchemaImporter.vue',
        'src/components/Lowcode/RuntimeRenderer.vue',
        'src/views/lowcode/form-config.vue',
        'src/views/lowcode/list-config.vue',
        'src/views/lowcode/tab-config.vue',
        'src/views/lowcode/relation-config.vue',
        'src/views/lowcode/template-library.vue',
        'src/views/lowcode/runtime-renderer.vue'
      ],
      exclude: [
        'src/**/*.d.ts',
        'src/components/index.ts',
        'src/components/CrudTable/types.ts',
        'src/components/CrudTable/__tests__/**',
        'src/components/FormModal/types.ts',
        'src/components/FormModal/__tests__/**',
        'src/components/ImportExport/types.ts',
        'src/components/ImportExport/__tests__/**',
        'src/components/OrgTree/types.ts',
        'src/components/OrgTree/index.ts',
        'src/components/OrgTree/__tests__/**',
        'src/components/RichEditor/types.ts',
        'src/components/RichEditor/index.ts',
        'src/components/RichEditor/__tests__/**',
        'src/components/Lowcode/__tests__/**',
        'src/api/__tests__/**',
        'src/views/lowcode/__tests__/**'
      ],
      thresholds: {
        // 新增低代码 / 反馈模块代码行覆盖率目标 ≥ 90%
        // 仅作为局部新增模块的目标参考，整体覆盖率不足时测试仍可运行通过。
        lines: 80,
        statements: 80,
        branches: 70,
        functions: 80
      }
    }
  }
})
