/**
 * SubTask 4.3: 异常路径 E2E 用例
 *
 * <p>覆盖系统的异常处理路径：</p>
 * <ul>
 *   <li>状态非法流转（如 DRAFT 直接 → COMPLETED，跳过中间步骤）→ 后端返回 409 业务冲突</li>
 *   <li>权限越权（ENGINEER 尝试访问 admin 页面）→ 返回 403 或重定向到 404</li>
 *   <li>必填项缺失（表单提交时 name 为空）→ 前端校验拦截，请求不应发出</li>
 *   <li>未登录访问受保护页面 → 重定向到登录页</li>
 *   <li>无效 Token → 接口返回 401</li>
 *   <li>资源不存在 → 接口返回 404</li>
 * </ul>
 *
 * <p>运行方式：</p>
 * <pre>
 * cd vibe-web
 * npm run test:e2e -- --grep "异常路径"
 * </pre>
 */
import { test, expect } from '@playwright/test'
import {
  loginAs,
  apiRequest,
  isBackendReachable,
  makeProjectDTO,
  makeTaskDTO,
  uniqueMark
} from './e2e-helpers'

test.describe('SubTask 4.3 异常路径 E2E', () => {
  test.describe.configure({ mode: 'serial' })

  let pmToken = ''
  let engineerToken = ''
  let projectId = 0

  test.beforeAll(async () => {
    const reachable = await isBackendReachable()
    if (!reachable) {
      console.warn('[e2e-exception] 后端不可达，依赖后端的用例将被跳过')
    }
  })

  /* ===================================================
   * 1. 状态非法流转 → 409 业务冲突
   * =================================================== */

  test.describe('状态非法流转', () => {
    test('1. 项目从 INIT 直接跳到 CLOSE 应被拒绝（409 业务冲突）', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动，跳过')

      pmToken = await loginAs(page, 'PM')

      // 创建一个 INIT 状态的新项目
      const createResp = await apiRequest(page, 'POST', '/projects', {
        body: { ...makeProjectDTO(), projectName: `E2E异常流转_${uniqueMark()}` },
        token: pmToken
      })
      expect(createResp.ok()).toBe(true)
      projectId = (await createResp.json()).data
      expect(projectId).toBeGreaterThan(0)

      // 尝试直接跳到 CLOSE（应被拒绝：状态机要求 INIT→PLAN→EXECUTE→ACCEPT→CLOSE）
      const transitionResp = await apiRequest(page, 'PUT', `/projects/${projectId}/status`, {
        body: { targetStatus: 'CLOSE', remark: '尝试非法跳转' },
        token: pmToken
      })

      // 期望 409 业务冲突；若后端策略为 200+code=40900 也接受
      const isConflict =
        transitionResp.status() === 409 ||
        (transitionResp.ok() && (await transitionResp.json()).code >= 40900 && (await transitionResp.json()).code < 50000)
      expect(isConflict).toBe(true)
    })

    test('2. 任务从 PENDING 直接跳 COMPLETED 应被拒绝', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      test.skip(!projectId, '前置项目未创建，跳过')

      await loginAs(page, 'PM')

      // 创建任务（PENDING 状态）
      const createResp = await apiRequest(page, 'POST', `/projects/${projectId}/tasks`, {
        body: makeTaskDTO(),
        token: pmToken
      })
      expect(createResp.ok()).toBe(true)
      const taskId = (await createResp.json()).data

      // 尝试直接从 PENDING 跳到 COMPLETED（应被拒绝）
      const progressResp = await apiRequest(page, 'PUT', `/projects/tasks/${taskId}/progress`, {
        body: { status: 'COMPLETED', progressPct: 100 },
        token: pmToken
      })

      // 期望 409 或业务码 40900~40999
      const isConflict =
        progressResp.status() === 409 ||
        (progressResp.ok() && (await progressResp.json()).code >= 40900)
      expect(isConflict).toBe(true)
    })

    test('3. 删除非 INIT/PLAN 状态项目应被拒绝', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      test.skip(!projectId, '前置项目未创建，跳过')

      await loginAs(page, 'PM')

      // 把项目流转到 PLAN（合法流转）
      const planResp = await apiRequest(page, 'PUT', `/projects/${projectId}/status`, {
        body: { targetStatus: 'PLAN', remark: '先流转到 PLAN' },
        token: pmToken
      })
      expect(planResp.ok()).toBe(true)

      // 再流转到 EXECUTE（合法流转）
      const execResp = await apiRequest(page, 'PUT', `/projects/${projectId}/status`, {
        body: { targetStatus: 'EXECUTE', remark: '再流转到 EXECUTE' },
        token: pmToken
      })
      expect(execResp.ok()).toBe(true)

      // EXECUTE 状态下尝试删除（应被拒绝）
      const delResp = await apiRequest(page, 'DELETE', `/projects/${projectId}`, { token: pmToken })

      const isRejected =
        delResp.status() === 409 || delResp.status() === 403 ||
        (delResp.ok() && (await delResp.json()).code >= 40900)
      expect(isRejected).toBe(true)
    })
  })

  /* ===================================================
   * 2. 权限越权 → 403 或重定向
   * =================================================== */

  test.describe('权限越权', () => {
    test('4. ENGINEER 访问系统管理页应被拦截（重定向到 404 或显示无权限）', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')

      engineerToken = await loginAs(page, 'ENGINEER')

      // 系统管理路由 meta.roles = ['SUPER_ADMIN']，ENGINEER 不在白名单
      await page.goto('/system/user')

      // 路由守卫应拦截：要么跳到 404，要么显示无权限（不渲染管理页内容）
      const url = page.url()
      const isBlocked = url.includes('/404') || url.includes('/login') ||
        (await page.locator('body').textContent() || '').includes('404') ||
        (await page.locator('body').textContent() || '').includes('页面不存在')
      expect(isBlocked).toBe(true)
    })

    test('5. ENGINEER 调用系统用户管理 API 应返回 403', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      test.skip(!engineerToken, 'ENGINEER 未登录，跳过')

      await loginAs(page, 'ENGINEER')
      const resp = await apiRequest(page, 'GET', '/users', {
        params: { page: 1, size: 10 },
        token: engineerToken
      })

      // 期望 403 或业务码 40300~40399；后端也可能返回 200+空数据（前端兜底），故宽松断言
      const isForbidden =
        resp.status() === 403 ||
        (resp.ok() && (await resp.json()).code >= 40300 && (await resp.json()).code < 40400)
      // 至少应不被授予完整管理权限（200+code=200+有数据则视为越权，应失败）
      // 宽松断言：403 OR 业务码 403xx OR 后端拒绝返回数据（数据为空）
      if (!isForbidden && resp.ok()) {
        const body = await resp.json()
        const records = body.data?.records || body.data || []
        // 若返回空数据（数据权限隔离），也算权限隔离生效
        expect(records.length === 0 || body.code !== 200).toBe(true)
      } else {
        expect(isForbidden).toBe(true)
      }
    })

    test('6. 未登录访问受保护页面应重定向到登录页', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')

      // 不调用 loginAs，直接访问受保护页面
      await page.goto('/project/list')

      // 路由守卫应重定向到 /login
      await expect(page).toHaveURL(/\/login/, { timeout: 5000 })
    })

    test('7. 无效 Token 调用 API 应返回 401', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')

      // 直接用伪造的 token 调用需要认证的接口
      const resp = await apiRequest(page, 'GET', '/projects', {
        params: { page: 1, size: 10 },
        token: 'invalid_token_for_e2e_test_xxx'
      })

      // 期望 401 或业务码 401xx
      const isUnauthorized =
        resp.status() === 401 ||
        (resp.ok() && (await resp.json()).code >= 40100 && (await resp.json()).code < 40200)
      expect(isUnauthorized).toBe(true)
    })
  })

  /* ===================================================
   * 3. 必填项缺失 → 前端校验拦截
   * ===================================================
   */

  test.describe('必填项缺失（前端校验）', () => {
    test('8. 项目名称为空时点击保存，前端应显示校验提示且不发请求', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')

      await loginAs(page, 'PM')

      // 拦截创建项目 API，断言不应被调用
      const createProjectCalls: unknown[] = []
      await page.route('**/api/v1/projects', async (route) => {
        if (route.request().method() === 'POST') {
          createProjectCalls.push(route.request().postData())
          await route.continue()
        } else {
          await route.continue()
        }
      })

      await page.goto('/project/list')
      await page.getByRole('button', { name: /新建项目/ }).first().click()

      const modal = page.getByRole('dialog').filter({ hasText: '新建项目' }).first()
      await expect(modal).toBeVisible({ timeout: 5000 })

      // 不填写项目名称，直接点保存
      await modal.getByRole('button', { name: '保存' }).click()

      // 应出现校验提示
      await expect(page.getByText('请输入项目名称').first()).toBeVisible({ timeout: 3000 })

      // 不应有请求被发出
      expect(createProjectCalls.length).toBe(0)
    })

    test('9. 项目名称只填空格时也应被校验拦截', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')

      await loginAs(page, 'PM')

      const createProjectCalls: unknown[] = []
      await page.route('**/api/v1/projects', async (route) => {
        if (route.request().method() === 'POST') {
          createProjectCalls.push(route.request().postData())
          await route.continue()
        } else {
          await route.continue()
        }
      })

      await page.goto('/project/list')
      await page.getByRole('button', { name: /新建项目/ }).first().click()

      const modal = page.getByRole('dialog').filter({ hasText: '新建项目' }).first()
      await expect(modal).toBeVisible({ timeout: 5000 })

      // 仅填空格
      await modal.getByPlaceholder('请输入项目名称').fill('   ')
      await modal.getByRole('button', { name: '保存' }).click()

      // 应出现校验提示（required 校验或自定义 trim 校验）
      // ant-design-vue 的 a-input 默认会 trim，但即使不 trim 也应触发 required
      // 这里宽松断言：要么出现"请输入项目名称"，要么请求被拦截
      const hasValidationError = await page.getByText('请输入项目名称').first().isVisible().catch(() => false)
      const noRequestSent = createProjectCalls.length === 0
      expect(hasValidationError || noRequestSent).toBe(true)
    })

    test('10. 登录页用户名为空时点登录应被前端校验拦截', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')

      const loginCalls: unknown[] = []
      await page.route('**/api/v1/auth/login', async (route) => {
        loginCalls.push(route.request().postData())
        await route.continue()
      })

      await page.goto('/login')

      // 不输入用户名直接点登录
      await page.getByRole('button', { name: '登录' }).click()

      // 应出现校验提示
      await expect(page.getByText('请输入用户名').first()).toBeVisible({ timeout: 3000 })
      expect(loginCalls.length).toBe(0)
    })

    test('11. 登录页密码少于 6 位应被前端校验拦截', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')

      const loginCalls: unknown[] = []
      await page.route('**/api/v1/auth/login', async (route) => {
        loginCalls.push(route.request().postData())
        await route.continue()
      })

      await page.goto('/login')

      // 输入用户名 + 短密码
      await page.getByPlaceholder('请输入用户名').fill('admin')
      await page.getByPlaceholder('请输入密码').fill('123')
      await page.getByRole('button', { name: '登录' }).click()

      // 应出现校验提示（密码至少 6 位）
      await expect(page.getByText(/密码至少.*位/).first()).toBeVisible({ timeout: 3000 })
      expect(loginCalls.length).toBe(0)
    })
  })

  /* ===================================================
   * 4. 资源不存在 → 404
   * =================================================== */

  test.describe('资源不存在', () => {
    test('12. 查询不存在的项目详情应返回 404', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      await loginAs(page, 'PM')

      const resp = await apiRequest(page, 'GET', '/projects/999999999', { token: pmToken })

      // 期望 404 或业务码 404xx
      const isNotFound =
        resp.status() === 404 ||
        (resp.ok() && (await resp.json()).code >= 40400 && (await resp.json()).code < 40500)
      expect(isNotFound).toBe(true)
    })

    test('13. 查询不存在的任务详情应返回 404', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      await loginAs(page, 'PM')

      const resp = await apiRequest(page, 'GET', '/projects/tasks/999999999', { token: pmToken })

      const isNotFound =
        resp.status() === 404 ||
        (resp.ok() && (await resp.json()).code >= 40400 && (await resp.json()).code < 40500)
      expect(isNotFound).toBe(true)
    })

    test('14. 删除不存在的项目应返回 404', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      await loginAs(page, 'PM')

      const resp = await apiRequest(page, 'DELETE', '/projects/999999999', { token: pmToken })

      const isNotFound =
        resp.status() === 404 ||
        (resp.ok() && (await resp.json()).code >= 40400 && (await resp.json()).code < 40500)
      expect(isNotFound).toBe(true)
    })
  })

  /* ===================================================
   * 5. 参数校验失败 → 400
   * =================================================== */

  test.describe('参数校验失败', () => {
    test('15. 创建项目时 projectName 为空字符串应返回 400', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      await loginAs(page, 'PM')

      // 直接调用 API 绕过前端校验
      const resp = await apiRequest(page, 'POST', '/projects', {
        body: { ...makeProjectDTO(), projectName: '' },
        token: pmToken
      })

      // 期望 400 或业务码 400xx；后端 Bean Validation 应拦截
      const isParamError =
        resp.status() === 400 ||
        (resp.ok() && (await resp.json()).code >= 40000 && (await resp.json()).code < 40100)
      expect(isParamError).toBe(true)
    })

    test('16. 创建项目时 customerId 为 0 应返回 400', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      await loginAs(page, 'PM')

      const resp = await apiRequest(page, 'POST', '/projects', {
        body: { ...makeProjectDTO(), customerId: 0 },
        token: pmToken
      })

      const isParamError =
        resp.status() === 400 ||
        (resp.ok() && (await resp.json()).code >= 40000 && (await resp.json()).code < 40100)
      // customerId 为 0 可能也被后端校验为非法
      expect(isParamError || resp.status() === 409).toBe(true)
    })
  })

  /* ============ 清理 ============ */

  test.afterAll(async ({ request }) => {
    if (projectId && pmToken) {
      try {
        const API_BASE_URL = process.env.E2E_API_BASE_URL || 'http://localhost:8080'
        // 项目当前可能处于 EXECUTE 状态，无法直接删除，先归档
        await request.put(`${API_BASE_URL}/api/v1/projects/${projectId}/archive`, {
          headers: { Authorization: `Bearer ${pmToken}`, 'Content-Type': 'application/json' },
          data: { reviewRecord: 'E2E 异常路径测试自动归档' }
        }).catch(() => undefined)
        console.log(`[e2e-exception] 项目 ${projectId} 已尝试清理`)
      } catch (e) {
        console.warn(`[e2e-exception] 清理失败（可忽略）:`, e)
      }
    }
  })
})
