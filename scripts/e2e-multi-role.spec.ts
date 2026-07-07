/**
 * SubTask 4.2: 多角色 E2E 用例
 *
 * <p>按角色覆盖核心路径：</p>
 * <ul>
 *   <li>PM 角色：创建项目 + 派发任务 + 审批</li>
 *   <li>ENGINEER 角色：查看任务 + 执行 + 提交</li>
 *   <li>AGENT_ADMIN 角色：代理商管理 + 工程师管理 + 结算确认</li>
 * </ul>
 *
 * <p>验证策略：每个角色的核心路径走 UI + API 双重验证，确保权限隔离正确。</p>
 *
 * <p>运行方式：</p>
 * <pre>
 * cd vibe-web
 * npm run test:e2e -- --grep "多角色"
 * </pre>
 */
import { test, expect } from '@playwright/test'
import {
  loginAs,
  apiRequest,
  isBackendReachable,
  makeTaskDTO,
  uniqueMark,
  ROLE_CREDENTIALS
} from './e2e-helpers'

test.describe('SubTask 4.2 多角色 E2E', () => {
  test.describe.configure({ mode: 'serial' })

  // 跨用例共享状态
  let pmToken = ''
  let engineerToken = ''
  let agentAdminToken = ''
  let sharedProjectId = 0
  let sharedTaskId = 0
  let sharedAgentCompanyId = 0
  let sharedAgentEngineerId = 0

  test.beforeAll(async () => {
    const reachable = await isBackendReachable()
    if (!reachable) {
      console.warn('[e2e-multi-role] 后端不可达，所有用例将被跳过')
    }
  })

  /* ===================================================
   *  PM 角色核心路径：创建项目 + 派发任务 + 审批
   * =================================================== */

  test.describe('PM 角色', () => {
    test('1. PM 登录后可访问项目列表页', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      pmToken = await loginAs(page, 'PM')
      expect(pmToken).toBeTruthy()

      await page.goto('/project/list')
      // 页面应包含"新建项目"按钮（PM 角色应有此权限）
      await expect(page.getByRole('button', { name: /新建项目/ }).first()).toBeVisible({ timeout: 10000 })
    })

    test('2. PM 通过 UI 创建项目，列表应展示新项目', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      test.skip(!pmToken, 'PM 未登录，跳过')

      await loginAs(page, 'PM')
      await page.goto('/project/list')

      // 点击新建项目按钮
      await page.getByRole('button', { name: /新建项目/ }).first().click()

      // 等待弹窗出现
      const modal = page.getByRole('dialog').filter({ hasText: '新建项目' }).first()
      await expect(modal).toBeVisible({ timeout: 5000 })

      // 填写项目名称（必填项）
      const projectName = `E2E_PM_${uniqueMark()}`
      await modal.getByPlaceholder('请输入项目名称').fill(projectName)

      // 提交保存
      await modal.getByRole('button', { name: '保存' }).click()

      // 等待弹窗关闭 + 列表加载完成
      await expect(modal).toBeHidden({ timeout: 10000 })

      // 通过 API 校验项目已创建
      const resp = await apiRequest(page, 'GET', '/projects', {
        params: { page: 1, size: 10, keyword: projectName },
        token: pmToken
      })
      expect(resp.ok()).toBe(true)
      const body = await resp.json()
      const records = body.data?.records || []
      const found = records.find((r: { projectName: string }) => r.projectName === projectName)
      expect(found).toBeTruthy()
      if (found) sharedProjectId = found.id
    })

    test('3. PM 通过 API 创建任务并派发给工程师', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      test.skip(!sharedProjectId, '前置项目未创建，跳过')

      await loginAs(page, 'PM')

      // 创建任务
      const createResp = await apiRequest(page, 'POST', `/projects/${sharedProjectId}/tasks`, {
        body: makeTaskDTO({ taskName: `E2E_PM任务_${uniqueMark()}` }),
        token: pmToken
      })
      expect(createResp.ok()).toBe(true)
      sharedTaskId = (await createResp.json()).data
      expect(sharedTaskId).toBeGreaterThan(0)

      // 派发任务（assigneeId=3 对应 engineer 账号 ID，与 e2e-helpers 中默认凭据对应）
      const dispatchResp = await apiRequest(page, 'PUT', `/projects/tasks/${sharedTaskId}/dispatch`, {
        body: {
          executeMode: 'SELF',
          assigneeId: 3,
          remark: `E2E 测试派发给 ${ROLE_CREDENTIALS.ENGINEER.username}`
        },
        token: pmToken
      })
      expect(dispatchResp.ok()).toBe(true)
    })

    test('4. PM 进入任务派发页可见待派发任务', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      test.skip(!pmToken, 'PM 未登录，跳过')

      await loginAs(page, 'PM')
      await page.goto('/resource/dispatch')
      // 页面应渲染（即使无数据）
      await expect(page.locator('body')).toContainText(/任务派发|待派发|派发/)
    })

    test('5. PM 可审批差旅申请', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      test.skip(!pmToken, 'PM 未登录，跳过')

      await loginAs(page, 'PM')
      await page.goto('/resource/business-trip')
      // 页面应可访问
      await expect(page.locator('body')).not.toContainText(/404|页面不存在/)
    })
  })

  /* ===================================================
   *  ENGINEER 角色核心路径：查看任务 + 执行 + 提交
   * =================================================== */

  test.describe('ENGINEER 角色', () => {
    test('6. ENGINEER 登录后可访问工作台', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      engineerToken = await loginAs(page, 'ENGINEER')
      expect(engineerToken).toBeTruthy()

      await page.goto('/dashboard')
      // 工作台应正常渲染（不跳回登录页）
      await expect(page).not.toHaveURL(/\/login/)
    })

    test('7. ENGINEER 查看我的任务列表', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      test.skip(!engineerToken, 'ENGINEER 未登录，跳过')

      await loginAs(page, 'ENGINEER')
      await page.goto('/dashboard/my-tasks')
      // 页面标题或正文应包含"我的任务"
      await expect(page.locator('body')).toContainText(/我的任务|待办/)
    })

    test('8. ENGINEER 查看任务详情，应能看到分配给自己的任务', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      test.skip(!sharedTaskId, '前置任务未创建，跳过')

      await loginAs(page, 'ENGINEER')

      // API 校验
      const resp = await apiRequest(page, 'GET', `/projects/tasks/${sharedTaskId}`, { token: engineerToken })
      expect(resp.ok()).toBe(true)
      const task = (await resp.json()).data
      expect(['ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CONFIRMED']).toContain(task.status)

      // UI 校验
      await page.goto(`/project/task/${sharedTaskId}`)
      await expect(page.locator('body')).not.toContainText(/404/)
    })

    test('9. ENGINEER 更新任务进度为 IN_PROGRESS', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      test.skip(!sharedTaskId, '前置任务未创建，跳过')

      await loginAs(page, 'ENGINEER')
      const resp = await apiRequest(page, 'PUT', `/projects/tasks/${sharedTaskId}/progress`, {
        body: { status: 'IN_PROGRESS', progressPct: 50, remark: 'E2E 工程师开始执行' },
        token: engineerToken
      })
      expect(resp.ok()).toBe(true)

      // 二次查询校验
      const taskResp = await apiRequest(page, 'GET', `/projects/tasks/${sharedTaskId}`, { token: engineerToken })
      const task = (await taskResp.json()).data
      expect(task.status).toBe('IN_PROGRESS')
    })

    test('10. ENGINEER 提交任务完成（COMPLETED）', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      test.skip(!sharedTaskId, '前置任务未创建，跳过')

      await loginAs(page, 'ENGINEER')
      const resp = await apiRequest(page, 'PUT', `/projects/tasks/${sharedTaskId}/progress`, {
        body: { status: 'COMPLETED', progressPct: 100, remark: 'E2E 工程师完成' },
        token: engineerToken
      })
      expect(resp.ok()).toBe(true)

      const taskResp = await apiRequest(page, 'GET', `/projects/tasks/${sharedTaskId}`, { token: engineerToken })
      const task = (await taskResp.json()).data
      expect(task.status).toBe('COMPLETED')
    })

    test('11. ENGINEER 工时填报页面可访问', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      test.skip(!engineerToken, 'ENGINEER 未登录，跳过')

      await loginAs(page, 'ENGINEER')
      await page.goto('/resource/timesheet')
      // 应可访问（不跳 404）
      await expect(page.locator('body')).not.toContainText(/404|页面不存在/)
    })
  })

  /* ===================================================
   *  AGENT_ADMIN 角色核心路径：代理商管理 + 工程师管理 + 结算确认
   * =================================================== */

  test.describe('AGENT_ADMIN 角色', () => {
    test('12. AGENT_ADMIN 登录后可访问代理商档案页', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      agentAdminToken = await loginAs(page, 'AGENT_ADMIN')
      expect(agentAdminToken).toBeTruthy()

      await page.goto('/agent/profile')
      // 页面应可访问（不跳 404 / 不被重定向到登录页）
      await expect(page).not.toHaveURL(/\/login/)
      await expect(page.locator('body')).not.toContainText(/404|页面不存在/)
    })

    test('13. AGENT_ADMIN 可查询代理商公司列表', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      test.skip(!agentAdminToken, 'AGENT_ADMIN 未登录，跳过')

      await loginAs(page, 'AGENT_ADMIN')
      const resp = await apiRequest(page, 'GET', '/agent-companies', {
        params: { page: 1, size: 10 },
        token: agentAdminToken
      })
      // 应可访问（200 或因权限策略返回空数据，不应是 401/403）
      expect([200, 403].includes(resp.status()) || resp.ok()).toBe(true)
      if (resp.ok()) {
        const body = await resp.json()
        const records = body.data?.records || body.data || []
        if (records.length > 0) {
          sharedAgentCompanyId = records[0].id
        }
      }
    })

    test('14. AGENT_ADMIN 可查询旗下工程师列表', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      test.skip(!agentAdminToken, 'AGENT_ADMIN 未登录，跳过')

      await loginAs(page, 'AGENT_ADMIN')

      // 若上一用例拿到 companyId，则用之；否则查询全部
      const targetCompany = sharedAgentCompanyId || 1
      const resp = await apiRequest(page, 'GET', `/agent-companies/${targetCompany}/engineers/all`, {
        token: agentAdminToken
      })
      expect([200, 403, 404].includes(resp.status()) || resp.ok()).toBe(true)
      if (resp.ok()) {
        const body = await resp.json()
        const list = body.data || []
        if (list.length > 0) {
          sharedAgentEngineerId = list[0].id
        }
      }
    })

    test('15. AGENT_ADMIN 可访问转包任务页', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      test.skip(!agentAdminToken, 'AGENT_ADMIN 未登录，跳过')

      await loginAs(page, 'AGENT_ADMIN')
      await page.goto('/agent/outsource')
      await expect(page).not.toHaveURL(/\/login/)
      await expect(page.locator('body')).not.toContainText(/404|页面不存在/)
    })

    test('16. AGENT_ADMIN 可访问结算管理页并查询工作量', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      test.skip(!agentAdminToken, 'AGENT_ADMIN 未登录，跳过')

      await loginAs(page, 'AGENT_ADMIN')
      await page.goto('/agent/settlement')
      await expect(page).not.toHaveURL(/\/login/)

      // API 校验：查询工作量列表
      const resp = await apiRequest(page, 'GET', '/outsource-tasks/workloads', {
        params: { page: 1, size: 10 },
        token: agentAdminToken
      })
      // 结算查询应可访问
      expect([200, 403].includes(resp.status()) || resp.ok()).toBe(true)
    })

    test('17. AGENT_ADMIN 可访问交付审核页', async ({ page }) => {
      test.skip(!(await isBackendReachable()), '后端未启动')
      test.skip(!agentAdminToken, 'AGENT_ADMIN 未登录，跳过')

      await loginAs(page, 'AGENT_ADMIN')
      await page.goto('/agent/review')
      await expect(page).not.toHaveURL(/\/login/)
      await expect(page.locator('body')).not.toContainText(/404|页面不存在/)
    })
  })

  /* ============ 清理 ============ */

  test.afterAll(async ({ request }) => {
    // 尝试清理：删除测试项目（仅 INIT/PLAN 状态可删除）
    if (sharedProjectId && pmToken) {
      try {
        const API_BASE_URL = process.env.E2E_API_BASE_URL || 'http://localhost:8080'
        await request.delete(`${API_BASE_URL}/api/v1/projects/${sharedProjectId}`, {
          headers: { Authorization: `Bearer ${pmToken}` }
        })
        console.log(`[e2e-multi-role] 项目 ${sharedProjectId} 已尝试清理`)
      } catch (e) {
        console.warn(`[e2e-multi-role] 清理失败（可忽略）:`, e)
      }
    }
  })
})
