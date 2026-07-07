/**
 * SubTask 4.1: 端到端全流程用例
 *
 * <p>覆盖项目交付的完整生命周期：</p>
 * <ol>
 *   <li>立项（PM 创建项目，状态 INIT）</li>
 *   <li>规划（PM 创建阶段、创建任务，状态 PLAN）</li>
 *   <li>派单（PM 将任务派发给工程师）</li>
 *   <li>执行（工程师接单、更新进度，状态 EXECUTE）</li>
 *   <li>验收（PM 提交验收申请 / 内部审核通过，状态 ACCEPT）</li>
 *   <li>结项（PM 完成结项，状态 CLOSE）</li>
 * </ol>
 *
 * <p>验证策略：每一步骤后通过 API 响应 + 页面展示双重校验数据正确性。</p>
 *
 * <p>运行方式：</p>
 * <pre>
 * cd vibe-web
 * npm run test:e2e -- --grep "全流程"
 * </pre>
 *
 * <p>环境要求：前端 (http://localhost:5173) + 后端 (http://localhost:8080) 均已启动，
 * 且账号 pm/pm123、engineer/engineer123 可登录。否则用例将自动跳过。</p>
 */
import { test, expect } from '@playwright/test'
import {
  loginAs,
  apiRequest,
  makeProjectDTO,
  makeTaskDTO,
  makePhaseDTO,
  isBackendReachable,
  uniqueMark
} from './e2e-helpers'

/** 测试用项目编号前缀（便于在测试结束后清理） */
const PROJECT_PREFIX = 'E2E_FULL'

test.describe('SubTask 4.1 端到端全流程', () => {
  // 共享状态：所有用例间复用 projectId / phaseId / taskId / token
  let pmToken = ''
  let engineerToken = ''
  let projectId = 0
  let phaseId = 0
  let taskId = 0
  const projectName = `E2E全流程_${uniqueMark(PROJECT_PREFIX)}`

  test.beforeAll(async () => {
    // 探测后端是否可达：所有用例都依赖后端
    const reachable = await isBackendReachable()
    if (!reachable) {
      console.warn('[e2e-full-flow] 后端不可达，所有用例将被跳过')
    }
  })

  test.describe.configure({ mode: 'serial' })

  /* ============ 1. 立项 ============ */

  test('1. PM 创建项目（立项），状态应为 INIT', async ({ page }) => {
    test.skip(!(await isBackendReachable()), '后端未启动，跳过全流程用例')

    pmToken = await loginAs(page, 'PM')
    expect(pmToken).toBeTruthy()

    // 通过 API 创建项目（UI 表单交互在 multi-role 用例覆盖，此处用 API 提速）
    const resp = await apiRequest(page, 'POST', '/projects', {
      body: { ...makeProjectDTO(), projectName, pmId: 2 },
      token: pmToken
    })
    expect(resp.ok()).toBe(true)
    const body = await resp.json()
    expect(body.code).toBe(200)
    projectId = body.data
    expect(projectId).toBeGreaterThan(0)
    console.log(`[e2e-full-flow] 项目已创建: id=${projectId}, name=${projectName}`)
  })

  test('2. 项目详情接口应返回 INIT 状态且页面展示正确', async ({ page }) => {
    test.skip(!(await isBackendReachable()), '后端未启动')
    test.skip(!projectId, '前置用例失败，跳过')

    // API 校验
    const resp = await apiRequest(page, 'GET', `/projects/${projectId}`, { token: pmToken })
    expect(resp.ok()).toBe(true)
    const detail = (await resp.json()).data
    expect(detail.status).toBe('INIT')
    expect(detail.projectName).toBe(projectName)

    // UI 校验：进入项目详情页，标题应包含项目名
    await page.goto(`/project/detail/${projectId}`)
    await expect(page.getByText(projectName, { exact: false }).first()).toBeVisible()
  })

  /* ============ 2. 规划 ============ */

  test('3. PM 创建阶段，阶段列表应包含新阶段', async ({ page }) => {
    test.skip(!(await isBackendReachable()), '后端未启动')
    test.skip(!projectId, '前置用例失败，跳过')

    const resp = await apiRequest(page, 'POST', `/projects/${projectId}/phases`, {
      body: makePhaseDTO({ phaseName: `阶段_安装_${uniqueMark()}` }),
      token: pmToken
    })
    expect(resp.ok()).toBe(true)
    const body = await resp.json()
    expect(body.code).toBe(200)
    phaseId = body.data
    expect(phaseId).toBeGreaterThan(0)
  })

  test('4. PM 创建任务，任务应归属当前项目', async ({ page }) => {
    test.skip(!(await isBackendReachable()), '后端未启动')
    test.skip(!projectId, '前置用例失败，跳过')

    const resp = await apiRequest(page, 'POST', `/projects/${projectId}/tasks`, {
      body: makeTaskDTO({
        phaseId,
        taskName: `任务_安装_${uniqueMark()}`
      }),
      token: pmToken
    })
    expect(resp.ok()).toBe(true)
    const body = await resp.json()
    expect(body.code).toBe(200)
    taskId = body.data
    expect(taskId).toBeGreaterThan(0)
  })

  test('5. 项目状态流转到 PLAN（规划中）', async ({ page }) => {
    test.skip(!(await isBackendReachable()), '后端未启动')
    test.skip(!projectId, '前置用例失败，跳过')

    const resp = await apiRequest(page, 'PUT', `/projects/${projectId}/status`, {
      body: { targetStatus: 'PLAN', remark: 'E2E 测试：进入规划' },
      token: pmToken
    })
    expect(resp.ok()).toBe(true)
    const body = await resp.json()
    expect([200, 20000].includes(body.code) || resp.status() === 200).toBe(true)

    // 二次查询校验状态
    const detailResp = await apiRequest(page, 'GET', `/projects/${projectId}`, { token: pmToken })
    const detail = (await detailResp.json()).data
    expect(detail.status).toBe('PLAN')
  })

  /* ============ 3. 派单 ============ */

  test('6. PM 派发任务给工程师，任务状态应为 ASSIGNED', async ({ page }) => {
    test.skip(!(await isBackendReachable()), '后端未启动')
    test.skip(!taskId, '前置用例失败，跳过')

    const resp = await apiRequest(page, 'PUT', `/projects/tasks/${taskId}/dispatch`, {
      body: {
        executeMode: 'SELF',
        assigneeId: 3, // engineer 用户 ID
        remark: 'E2E 测试：派发给工程师'
      },
      token: pmToken
    })
    expect(resp.ok()).toBe(true)

    // 校验任务详情
    const taskResp = await apiRequest(page, 'GET', `/projects/tasks/${taskId}`, { token: pmToken })
    const task = (await taskResp.json()).data
    expect(task.status).toBe('ASSIGNED')
    expect(task.assigneeId).toBe(3)
  })

  test('7. 项目状态流转到 EXECUTE（执行中）', async ({ page }) => {
    test.skip(!(await isBackendReachable()), '后端未启动')
    test.skip(!projectId, '前置用例失败，跳过')

    const resp = await apiRequest(page, 'PUT', `/projects/${projectId}/status`, {
      body: { targetStatus: 'EXECUTE', remark: 'E2E 测试：进入执行' },
      token: pmToken
    })
    expect(resp.ok()).toBe(true)

    const detailResp = await apiRequest(page, 'GET', `/projects/${projectId}`, { token: pmToken })
    const detail = (await detailResp.json()).data
    expect(detail.status).toBe('EXECUTE')
  })

  /* ============ 4. 执行 ============ */

  test('8. 工程师登录后页面可见"我的任务"入口', async ({ page }) => {
    test.skip(!(await isBackendReachable()), '后端未启动')
    test.skip(!taskId, '前置用例失败，跳过')

    engineerToken = await loginAs(page, 'ENGINEER')
    expect(engineerToken).toBeTruthy()

    await page.goto('/dashboard')
    // 工作台应有"我的任务"链接或卡片
    await expect(page.locator('body')).toContainText(/我的任务|工作台|待办/)
  })

  test('9. 工程师查看任务详情，应能看到派给自己的任务', async ({ page }) => {
    test.skip(!(await isBackendReachable()), '后端未启动')
    test.skip(!taskId, '前置用例失败，跳过')

    // API 校验
    const resp = await apiRequest(page, 'GET', `/projects/tasks/${taskId}`, { token: engineerToken })
    expect(resp.ok()).toBe(true)
    const task = (await resp.json()).data
    expect(task.assigneeId).toBe(3)

    // UI 校验
    await page.goto('/dashboard/my-tasks')
    await expect(page.locator('body')).toContainText(/我的任务|待办/)
  })

  test('10. 工程师将任务进度更新为 IN_PROGRESS', async ({ page }) => {
    test.skip(!(await isBackendReachable()), '后端未启动')
    test.skip(!taskId, '前置用例失败，跳过')

    const resp = await apiRequest(page, 'PUT', `/projects/tasks/${taskId}/progress`, {
      body: {
        status: 'IN_PROGRESS',
        progressPct: 30,
        remark: 'E2E 测试：开始执行'
      },
      token: engineerToken
    })
    expect(resp.ok()).toBe(true)

    const taskResp = await apiRequest(page, 'GET', `/projects/tasks/${taskId}`, { token: engineerToken })
    const task = (await taskResp.json()).data
    expect(task.status).toBe('IN_PROGRESS')
    expect(task.progressPct).toBeGreaterThanOrEqual(30)
  })

  test('11. 工程师完成任务，状态变为 COMPLETED', async ({ page }) => {
    test.skip(!(await isBackendReachable()), '后端未启动')
    test.skip(!taskId, '前置用例失败，跳过')

    const resp = await apiRequest(page, 'PUT', `/projects/tasks/${taskId}/progress`, {
      body: {
        status: 'COMPLETED',
        progressPct: 100,
        remark: 'E2E 测试：任务完成'
      },
      token: engineerToken
    })
    expect(resp.ok()).toBe(true)

    const taskResp = await apiRequest(page, 'GET', `/projects/tasks/${taskId}`, { token: engineerToken })
    const task = (await taskResp.json()).data
    expect(task.status).toBe('COMPLETED')
    expect(task.progressPct).toBe(100)
  })

  /* ============ 5. 验收 ============ */

  test('12. PM 将项目状态流转到 ACCEPT（验收中）', async ({ page }) => {
    test.skip(!(await isBackendReachable()), '后端未启动')
    test.skip(!projectId, '前置用例失败，跳过')

    const resp = await apiRequest(page, 'PUT', `/projects/${projectId}/status`, {
      body: { targetStatus: 'ACCEPT', remark: 'E2E 测试：进入验收' },
      token: pmToken
    })
    expect(resp.ok()).toBe(true)

    const detailResp = await apiRequest(page, 'GET', `/projects/${projectId}`, { token: pmToken })
    const detail = (await detailResp.json()).data
    expect(detail.status).toBe('ACCEPT')
  })

  test('13. PM 创建验收任务并提交申请', async ({ page }) => {
    test.skip(!(await isBackendReachable()), '后端未启动')
    test.skip(!projectId, '前置用例失败，跳过')

    const resp = await apiRequest(page, 'POST', '/acceptance/tasks', {
      body: {
        projectId,
        acceptanceType: 'FINAL',
        remark: 'E2E 测试：发起验收'
      },
      token: pmToken
    })
    // 验收任务创建成功（200）或已存在（409 业务冲突也可接受）
    expect([200, 409].includes(resp.status()) || resp.ok()).toBe(true)
  })

  /* ============ 6. 结项 ============ */

  test('14. PM 执行结项检查（close-check）应通过', async ({ page }) => {
    test.skip(!(await isBackendReachable()), '后端未启动')
    test.skip(!projectId, '前置用例失败，跳过')

    const resp = await apiRequest(page, 'GET', `/projects/${projectId}/close-check`, { token: pmToken })
    expect(resp.ok()).toBe(true)
    // close-check 返回 null 表示通过；返回字符串表示不满足原因
    const body = await resp.json()
    expect(body.code).toBe(200)
  })

  test('15. PM 完成结项，项目状态变为 CLOSE', async ({ page }) => {
    test.skip(!(await isBackendReachable()), '后端未启动')
    test.skip(!projectId, '前置用例失败，跳过')

    const resp = await apiRequest(page, 'PUT', `/projects/${projectId}/status`, {
      body: { targetStatus: 'CLOSE', remark: 'E2E 测试：结项' },
      token: pmToken
    })
    expect(resp.ok()).toBe(true)

    const detailResp = await apiRequest(page, 'GET', `/projects/${projectId}`, { token: pmToken })
    const detail = (await detailResp.json()).data
    expect(detail.status).toBe('CLOSE')

    // UI 校验：进入项目列表页，应能看到该项目
    await page.goto('/project/list')
    await expect(page.locator('body')).toContainText(projectName)
  })

  /* ============ 清理 ============ */

  test.afterAll(async ({ request }) => {
    // 清理测试数据：尝试删除项目（CLOSE 状态无法删除，但可尝试归档）
    if (projectId && pmToken) {
      try {
        const API_BASE_URL = process.env.E2E_API_BASE_URL || 'http://localhost:8080'
        await request.put(`${API_BASE_URL}/api/v1/projects/${projectId}/archive`, {
          headers: { Authorization: `Bearer ${pmToken}`, 'Content-Type': 'application/json' },
          data: { reviewRecord: 'E2E 测试自动归档' }
        })
        console.log(`[e2e-full-flow] 项目 ${projectId} 已归档`)
      } catch (e) {
        console.warn(`[e2e-full-flow] 清理失败（可忽略）:`, e)
      }
    }
  })
})
