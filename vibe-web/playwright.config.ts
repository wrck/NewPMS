/**
 * Playwright 配置
 *
 * <p>端到端测试配置：</p>
 * <ul>
 *   <li>测试目录：项目根目录的 <code>scripts/</code>（e2e-*.spec.ts）</li>
 *   <li>测试并发：按文件并行，文件内串行（避免用例间状态污染）</li>
 *   <li>报告：HTML + list</li>
 *   <li>WebServer：自动拉起 <code>npm run dev</code>，若端口已占用则复用现有实例</li>
 * </ul>
 *
 * <p>运行方式：</p>
 * <pre>
 * # 在 vibe-web 目录下
 * npm run test:e2e                   # 全部 e2e 用例
 * npm run test:e2e -- --grep "全流程"  # 仅运行匹配关键字的用例
 * npm run test:e2e:headed            # 有头模式（可视化调试）
 * npm run test:e2e:ui               # Playwright UI 模式
 * </pre>
 *
 * <p>环境变量：</p>
 * <ul>
 *   <li><code>E2E_BASE_URL</code>：被测前端地址，默认 http://localhost:5173</li>
 *   <li><code>E2E_API_BASE_URL</code>：后端 API 地址，默认 http://localhost:8080</li>
 *   <li><code>E2E_PM_USERNAME</code> / <code>E2E_PM_PASSWORD</code>：PM 角色账号</li>
 *   <li><code>E2E_ENGINEER_USERNAME</code> / <code>E2E_ENGINEER_PASSWORD</code>：工程师角色账号</li>
 *   <li><code>E2E_AGENT_ADMIN_USERNAME</code> / <code>E2E_AGENT_ADMIN_PASSWORD</code>：代理商管理员账号</li>
 * </ul>
 */
import { defineConfig, devices } from '@playwright/test'

const FRONT_BASE_URL = process.env.E2E_BASE_URL || 'http://localhost:5173'
const API_BASE_URL = process.env.E2E_API_BASE_URL || 'http://localhost:8080'

/** 是否在 CI 环境运行（CI 下更宽容的重试与超时） */
const isCI = !!process.env.CI

/** 是否需要自动启动前端 dev server（若已经有 dev 在跑，Playwright 会复用） */
const reuseExistingServer = !isCI || process.env.E2E_REUSE_SERVER === 'true'

export default defineConfig({
  // 测试目录：scripts/ 下的所有 e2e-*.spec.ts
  testDir: '../scripts',
  testMatch: /e2e-.*\.spec\.ts$/,
  // 排除已有 vitest 编写的 e2e-smoke.spec.ts（它是 vitest + axios 的 API 冒烟，不是 Playwright UI 用例）
  testIgnore: /e2e-smoke\.spec\.ts$/,

  // 测试输出
  outputDir: '../playwright-report/output',
  snapshotDir: '../playwright-report/snapshots',

  // 并发策略：文件级并行，文件内串行
  fullyParallel: false,
  workers: isCI ? 1 : undefined,

  // 超时与重试
  timeout: 60_000,
  expect: { timeout: 10_000 },
  retries: isCI ? 2 : 0,

  // 报告
  reporter: [
    ['list'],
    ['html', { outputFolder: '../playwright-report/html', open: 'never' }]
  ],

  // 全局配置
  use: {
    // 前端地址
    baseURL: FRONT_BASE_URL,
    // 浏览器上下文
    browserName: 'chromium',
    viewport: { width: 1440, height: 900 },
    locale: 'zh-CN',
    timezoneId: 'Asia/Shanghai',
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
    // 失败时截图 + 录屏 + 追踪
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'retain-on-failure',
    // 注入到所有 page 的全局变量（便于测试读取 API 基址）
    extraHTTPHeaders: {
      'X-E2E-Test': 'true'
    }
  },

  // 项目配置：桌面浏览器 + 移动 H5 视图
  projects: [
    {
      name: 'chromium-desktop',
      use: { ...devices['Desktop Chrome'] }
    },
    {
      name: 'mobile-h5',
      testMatch: /e2e-.*-h5\.spec\.ts/,
      use: {
        ...devices['iPhone 13'],
        viewport: { width: 375, height: 812 },
        isMobile: true,
        hasTouch: true
      }
    }
  ],

  // 自动启动 dev server（仅当端口未占用时）
  webServer: reuseExistingServer
    ? {
        command: 'npm run dev',
        url: FRONT_BASE_URL,
        timeout: 120_000,
        reuseExistingServer: true,
        stdout: 'pipe',
        env: {
          VITE_API_BASE_URL: API_BASE_URL
        }
      }
    : undefined
})
