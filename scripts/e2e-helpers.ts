/**
 * E2E 测试公共辅助工具
 *
 * <p>提供：</p>
 * <ul>
 *   <li>API 客户端封装（基于 axios，与 e2e-smoke.spec.ts 保持一致）</li>
 *   <li>多角色登录工具：调用 /auth/login 获取 token 并注入 localStorage</li>
 *   <li>后端可达性探测：用于在缺后端环境下条件跳过用例</li>
 *   <li>测试数据工厂：项目、阶段、任务的 DTO 生成</li>
 * </ul>
 *
 * <p>使用方式：</p>
 * <pre>
 * import { loginAs, apiClient, ensureBackend, makeProjectDTO } from './e2e-helpers'
 *
 * test('xxx', async ({ page }) => {
 *   await ensureBackend()  // 缺后端则自动 skip
 *   const token = await loginAs(page, 'PM')
 *   ...
 * })
 * </pre>
 */
import type { Page, APIResponse } from '@playwright/test'

/* ============ 配置 ============ */

const API_BASE_URL = process.env.E2E_API_BASE_URL || 'http://localhost:8080'
const API_PREFIX = '/api/v1'

/** localStorage 中 token 的 key（与 src/stores/user.ts 保持一致） */
const TOKEN_KEY = 'vibe_token'

/** 测试用角色账号（可由环境变量覆盖；缺省走默认账号） */
export interface RoleCredential {
  username: string
  password: string
  /** 期望角色，用于断言 /auth/me 返回的 roles */
  expectedRole: 'SUPER_ADMIN' | 'DIRECTOR' | 'PM' | 'ENGINEER' | 'AGENT_ADMIN' | 'AGENT_ENGINEER' | 'FINANCE' | 'CUSTOMER'
}

export const ROLE_CREDENTIALS: Record<string, RoleCredential> = {
  SUPER_ADMIN: {
    username: process.env.E2E_ADMIN_USERNAME || 'admin',
    password: process.env.E2E_ADMIN_PASSWORD || 'admin123',
    expectedRole: 'SUPER_ADMIN'
  },
  PM: {
    username: process.env.E2E_PM_USERNAME || 'pm',
    password: process.env.E2E_PM_PASSWORD || 'pm123',
    expectedRole: 'PM'
  },
  ENGINEER: {
    username: process.env.E2E_ENGINEER_USERNAME || 'engineer',
    password: process.env.E2E_ENGINEER_PASSWORD || 'engineer123',
    expectedRole: 'ENGINEER'
  },
  AGENT_ADMIN: {
    username: process.env.E2E_AGENT_ADMIN_USERNAME || 'agent_admin',
    password: process.env.E2E_AGENT_ADMIN_PASSWORD || 'agent123',
    expectedRole: 'AGENT_ADMIN'
  },
  AGENT_ENGINEER: {
    username: process.env.E2E_AGENT_ENGINEER_USERNAME || 'agent_engineer',
    password: process.env.E2E_AGENT_ENGINEER_PASSWORD || 'agent123',
    expectedRole: 'AGENT_ENGINEER'
  },
  FINANCE: {
    username: process.env.E2E_FINANCE_USERNAME || 'finance',
    password: process.env.E2E_FINANCE_PASSWORD || 'finance123',
    expectedRole: 'FINANCE'
  }
}

/* ============ 后端可达性 ============ */

let _backendReachable: boolean | null = null

/** 探测后端是否可达（带 5s 缓存，避免重复请求） */
export async function isBackendReachable(): Promise<boolean> {
  if (_backendReachable !== null) return _backendReachable
  try {
    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), 5000)
    const resp = await fetch(`${API_BASE_URL}/actuator/health`, { signal: controller.signal })
    clearTimeout(timer)
    _backendReachable = resp.ok
  } catch {
    _backendReachable = false
  }
  return _backendReachable
}

/** 在 beforeAll 中调用：若后端不可达，跳过整个 describe */
export function ensureBackendSkipFactory() {
  // 返回一个可在 test.skip 中使用的同步函数
  // 由于 test.skip 是同步 API，需要先用 isBackendReachable 探测后写入全局
  return isBackendReachable
}

/* ============ API 客户端 ============ */

/** 调用登录接口，返回 token 与用户信息 */
export async function loginViaApi(role: keyof typeof ROLE_CREDENTIALS): Promise<{
  token: string
  refreshToken?: string
  userInfo?: unknown
}> {
  const cred = ROLE_CREDENTIALS[role]
  if (!cred) throw new Error(`未知角色: ${role}`)

  const resp = await fetch(`${API_BASE_URL}${API_PREFIX}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: cred.username,
      password: cred.password,
      clientId: 'PC'
    })
  })

  if (!resp.ok) {
    throw new Error(`登录失败: HTTP ${resp.status}`)
  }

  const body = (await resp.json()) as { code: number; data: { token: string; refreshToken?: string; userInfo?: unknown } }
  if (body.code !== 200) {
    throw new Error(`登录失败: ${body.code}`)
  }
  return {
    token: body.data.token,
    refreshToken: body.data.refreshToken,
    userInfo: body.data.userInfo
  }
}

/**
 * 在浏览器上下文中完成登录：
 * 1. 调用 API 获取 token
 * 2. 将 token 注入 localStorage
 * 3. （可选）注入 userInfo
 *
 * <p>对应 src/stores/user.ts 的实现：
 * token 存在 localStorage['vibe_token']，
 * userInfo 存在 Pinia persist 的 localStorage['user'] 中。</p>
 */
export async function loginAs(page: Page, role: keyof typeof ROLE_CREDENTIALS): Promise<string> {
  const { token, userInfo } = await loginViaApi(role)

  // 先打开前端域名（否则 localStorage 写不进去）
  await page.goto('/')
  await page.evaluate(
    ({ token, userInfo, key }) => {
      localStorage.setItem(key, token)
      // Pinia persist 的 user store key 为 'user'，结构为 { token, refreshToken, userInfo }
      try {
        localStorage.setItem('user', JSON.stringify({ token, userInfo }))
      } catch {
        // 忽略：不影响后续 page 内导航
      }
    },
    { token, userInfo, key: TOKEN_KEY }
  )

  return token
}

/** 通过浏览器上下文发送 API 请求（自动携带 token） */
export async function apiRequest(
  page: Page,
  method: 'GET' | 'POST' | 'PUT' | 'DELETE',
  path: string,
  options: { body?: unknown; params?: Record<string, unknown>; token?: string } = {}
): Promise<APIResponse> {
  const url = new URL(`${API_PREFIX}${path}`, API_BASE_URL)
  if (options.params) {
    for (const [k, v] of Object.entries(options.params)) {
      if (v !== undefined && v !== null) url.searchParams.set(k, String(v))
    }
  }
  return page.request.fetch(url.toString(), {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(options.token ? { Authorization: `Bearer ${options.token}` } : {})
    },
    data: options.body ? JSON.stringify(options.body) : undefined
  })
}

/* ============ 测试数据工厂 ============ */

/** 唯一标记（避免并发测试数据冲突） */
export function uniqueMark(prefix = 'e2e'): string {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

/** 项目创建 DTO */
export function makeProjectDTO(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    projectName: `E2E项目_${uniqueMark()}`,
    customerId: 1,
    projectType: 'NEW',
    productLine: 'ROUTER',
    executeMode: 'SELF',
    priority: 'MEDIUM',
    pmId: 1,
    region: '华北',
    contractNo: `HT-${uniqueMark()}`,
    plannedStart: '2026-01-01',
    plannedEnd: '2026-06-30',
    description: 'E2E 自动化测试创建的项目',
    ...overrides
  }
}

/** 任务创建 DTO */
export function makeTaskDTO(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    taskName: `E2E任务_${uniqueMark()}`,
    taskType: 'INSTALL',
    executeMode: 'SELF',
    priority: 'MEDIUM',
    description: 'E2E 自动化测试创建的任务',
    plannedStart: '2026-02-01',
    plannedEnd: '2026-03-01',
    ...overrides
  }
}

/** 阶段 DTO */
export function makePhaseDTO(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    phaseCode: 'INSTALL',
    phaseName: `E2E阶段_${uniqueMark()}`,
    sortOrder: 1,
    plannedStart: '2026-02-01',
    plannedEnd: '2026-03-01',
    ...overrides
  }
}

/* ============ 等待工具 ============ */

/** 等待 Ant Design Vue 的 message 提示出现 */
export async function waitForMessage(page: Page, text: string, timeout = 5000): Promise<void> {
  await page.getByText(text, { exact: false }).first().waitFor({ state: 'visible', timeout })
}

/** 等待 Ant Design Vue 的 Modal 弹窗出现 */
export async function waitForModal(page: Page, title: string, timeout = 5000): Promise<void> {
  await page.getByRole('dialog').filter({ hasText: title }).first().waitFor({ state: 'visible', timeout })
}

/** 关闭可能存在的 message 提示（避免后续用例误识别旧提示） */
export async function dismissMessages(page: Page): Promise<void> {
  // ant-design-vue 的 message 默认 3s 后自动消失，这里不主动关闭
  // 仅做一次空操作，等待 100ms
  await page.waitForTimeout(100)
}
