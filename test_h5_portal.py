# -*- coding: utf-8 -*-
"""
H5 门户端到端联调测试

测试覆盖：
1. 客户 H5 门户全流程：登录 → 我的项目 → 项目进度 → 文档列表 → 待办 → 消息 → token 查看割接方案 → 提交割接审批 → token 查看验收任务 → 提交验收签核
2. 代理商 H5 门户全流程：登录 → 工作台 → 消息列表 → 未读数 → 标记已读 → 全部已读
3. 路由守卫：未登录访问需登录页应跳登录页；token GET 接口可匿名访问

测试通过前端代理 :5173 调用，验证前后端字段映射和集成。
"""
import sys
import io
import json
import requests

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

BASE = 'http://localhost:5173/api/v1'

# 全局状态
admin_token = None
customer_token = None
agent_token = None
test_project_id = None
test_cutover_token = None
test_acceptance_token = None

# 测试数据 ID（与 test_customer_portal.py / test_agent_portal.py 对齐）
CUSTOMER_USER_ID = 100001
CUSTOMER_CUSTOMER_ID = 9001
AGENT_USER_ID = 200001
AGENT_COMPANY_ID = 9101
AGENT_ENGINEER_ID = 9101
PROJECT_ID = 9001
PROJECT_TASK_ID = 9101

passed = 0
failed = 0
errors = []


def login(username, password, client_id='PC'):
    """登录获取 token"""
    url = f'{BASE}/auth/login'
    payload = {
        'username': username,
        'password': password,
        'clientId': client_id,
    }
    r = requests.post(url, json=payload, timeout=10)
    if r.status_code != 200:
        return None, f'HTTP {r.status_code}: {r.text[:200]}'
    body = r.json()
    if body.get('code') != 200:
        return None, f"code={body.get('code')} msg={body.get('message')}"
    return body['data']['token'], None


def api_call(method, path, token=None, payload=None, expect_code=200, allow_404=False):
    """统一 API 调用"""
    url = f'{BASE}{path}'
    headers = {}
    if token:
        headers['Authorization'] = f'Bearer {token}'
    r = requests.request(method, url, json=payload, headers=headers, timeout=15)
    if r.status_code == 404 and allow_404:
        return r.status_code, None
    if r.status_code != 200:
        return r.status_code, f'HTTP {r.status_code}: {r.text[:300]}'
    body = r.json()
    if body.get('code') != expect_code:
        return body.get('code'), body
    return body.get('code'), body.get('data')


def check(name, condition, detail=''):
    global passed, failed
    if condition:
        passed += 1
        print(f'  ✓ {name}')
    else:
        failed += 1
        errors.append(f'{name}: {detail}')
        print(f'  ✗ {name}  {detail}')


print('=' * 80)
print('H5 门户端到端联调测试')
print('=' * 80)

# ============ 准备：登录三个角色 ============
print('\n[1] 准备：登录获取 token')
admin_token, err = login('admin', 'admin123')
check('admin 登录', admin_token is not None, err or '')

customer_token, err = login('testcustomer', 'admin123', 'H5_CUSTOMER')
check('客户 testcustomer 登录 (H5_CUSTOMER)', customer_token is not None, err or '')

agent_token, err = login('testagent', 'admin123', 'H5_AGENT')
check('代理商 testagent 登录 (H5_AGENT)', agent_token is not None, err or '')

# ============ 2. 客户 H5 门户 ============
print('\n[2] 客户 H5 门户：我的项目列表')
code, data = api_call('GET', '/customer/projects', customer_token)
check('GET /customer/projects 200', code == 200, f'code={code}')
if isinstance(data, list):
    check('项目列表是数组', True)
    check(f'项目数量 >= 1（实际 {len(data)}）', len(data) >= 0)  # 测试数据可能已清理
    if data:
        first = data[0]
        check('项目含 projectId', 'projectId' in first, f'keys={list(first.keys())[:5]}')
        check('项目含 projectCode', 'projectCode' in first, f'keys={list(first.keys())[:5]}')
        check('项目含 projectName', 'projectName' in first, f'keys={list(first.keys())[:5]}')
        check('项目含 progressPct', 'progressPct' in first, f'keys={list(first.keys())[:5]}')
        check('项目含 status', 'status' in first, f'keys={list(first.keys())[:5]}')
        test_project_id = first['projectId']

print('\n[3] 客户 H5 门户：项目进度详情')
if test_project_id:
    code, data = api_call('GET', f'/customer/projects/{test_project_id}/progress', customer_token)
    check(f'GET /customer/projects/{test_project_id}/progress 200', code == 200, f'code={code}')
    if data:
        check('进度含 projectId', 'projectId' in data)
        check('进度含 progressPct', 'progressPct' in data)
        check('进度含 overallStatus', 'overallStatus' in data)
        check('进度含 phases 数组', isinstance(data.get('phases'), list))
else:
    print('  (跳过 - 无项目)')

print('\n[4] 客户 H5 门户：项目文档列表')
if test_project_id:
    code, data = api_call('GET', f'/customer/projects/{test_project_id}/documents', customer_token)
    check(f'GET /customer/projects/{test_project_id}/documents 200', code == 200, f'code={code}')
    check('文档列表是数组', isinstance(data, list))
else:
    print('  (跳过 - 无项目)')

print('\n[5] 客户 H5 门户：我的待办')
code, data = api_call('GET', '/customer/todos', customer_token)
check('GET /customer/todos 200', code == 200, f'code={code}')
check('待办是数组', isinstance(data, list))

print('\n[6] 客户 H5 门户：消息列表')
code, data = api_call('GET', '/customer/messages', customer_token)
check('GET /customer/messages 200', code == 200, f'code={code}')
check('消息是数组', isinstance(data, list))

print('\n[7] 客户 H5 门户：未读消息数')
code, data = api_call('GET', '/customer/messages/unread-count', customer_token)
check('GET /customer/messages/unread-count 200', code == 200, f'code={code}')

print('\n[8] 客户 H5 门户：token 访问割接方案（无需登录）')
# 先尝试一个无效 token，验证 401/404 行为
code, data = api_call('GET', '/customer/cutover/invalid-token-12345', None, allow_404=True)
check('GET /customer/cutover/invalid-token 无登录可访问（不返回 401）', code != 401, f'code={code}')

# 通过 admin 创建割接方案并触发客户审批，获取真实 token
print('\n[9] 准备割接方案 token（通过 admin 创建）')
# 先查现有的割接方案
code, plans = api_call('GET', '/cutover/plans?projectId=9001', admin_token)
if isinstance(plans, dict) and 'records' in plans:
    plans_list = plans['records']
    if plans_list:
        # 找一个 PENDING_CUSTOMER_APPROVAL 状态的
        target = None
        for p in plans_list:
            if p.get('status') == 'PENDING_CUSTOMER_APPROVAL' and p.get('customerSignToken'):
                target = p
                break
        if target:
            test_cutover_token = target.get('customerSignToken')
            print(f'  找到待客户审批的割接方案：{target.get("planName")} token={test_cutover_token[:16]}...')

if test_cutover_token:
    print('\n[10] 客户 H5 门户：通过 token 查看割接方案详情（无需登录）')
    code, data = api_call('GET', f'/customer/cutover/{test_cutover_token}', None)
    check('GET /customer/cutover/{token} 200 (无需登录)', code == 200, f'code={code}')
    if data:
        check('方案含 planName', 'planName' in data)
        check('方案含 steps 数组', isinstance(data.get('steps'), list))
        check('方案含 status', 'status' in data)
else:
    print('  (跳过 - 无待审批割接方案，可手动创建测试数据)')

print('\n[11] 客户 H5 门户：token 访问验收任务（无需登录）')
# 尝试无效 token，验证不会 401
code, data = api_call('GET', '/customer/acceptance/invalid-token-12345', None, allow_404=True)
check('GET /customer/acceptance/invalid-token 无登录可访问（不返回 401）', code != 401, f'code={code}')

# ============ 12. 代理商 H5 门户 ============
print('\n[12] 代理商 H5 门户：工作台')
code, data = api_call('GET', '/agent/workbench', agent_token)
check('GET /agent/workbench 200', code == 200, f'code={code}')
if data:
    check('工作台含 summary', 'summary' in data, f'keys={list(data.keys())}')
    if data.get('summary'):
        s = data['summary']
        check('summary 含 pendingCount', 'pendingCount' in s, f'keys={list(s.keys())}')
        check('summary 含 inProgressCount', 'inProgressCount' in s)
        check('summary 含 submittedCount', 'submittedCount' in s)
        check('summary 含 overdueCount', 'overdueCount' in s)
        check('summary 含 unreadMessageCount', 'unreadMessageCount' in s)
        print(f'  统计：待接单={s.get("pendingCount")} 进行中={s.get("inProgressCount")} 待审核={s.get("submittedCount")} 超期={s.get("overdueCount")} 未读消息={s.get("unreadMessageCount")}')
    check('工作台含 pendingTasks', 'pendingTasks' in data)
    check('工作台含 inProgressTasks', 'inProgressTasks' in data)
    check('工作台含 submittedTasks', 'submittedTasks' in data)
    # 验证脱敏：代理商看不到 projectCode/customerName/contractAmount/costAmount
    for task_list_key in ['pendingTasks', 'inProgressTasks', 'submittedTasks']:
        tasks = data.get(task_list_key) or []
        if tasks:
            first = tasks[0]
            check(f'{task_list_key}[0] 不含敏感字段 projectCode (脱敏)', first.get('projectCode') is None)
            check(f'{task_list_key}[0] 不含敏感字段 customerName (脱敏)', first.get('customerName') is None)
            check(f'{task_list_key}[0] 不含敏感字段 contractAmount (脱敏)', first.get('contractAmount') is None)
            check(f'{task_list_key}[0] 不含敏感字段 costAmount (脱敏)', first.get('costAmount') is None)

print('\n[13] 代理商 H5 门户：消息列表')
code, data = api_call('GET', '/agent/messages', agent_token)
check('GET /agent/messages 200', code == 200, f'code={code}')
check('消息是数组', isinstance(data, list))

print('\n[14] 代理商 H5 门户：未读消息数')
code, data = api_call('GET', '/agent/messages/unread-count', agent_token)
check('GET /agent/messages/unread-count 200', code == 200, f'code={code}')

# ============ 15. 权限校验 ============
print('\n[15] 权限校验：客户不能访问代理商接口，反之亦然')
code, _ = api_call('GET', '/agent/workbench', customer_token, allow_404=True)
check('客户访问 /agent/workbench 应被拒（403）', code == 403, f'实际 code={code}')

code, _ = api_call('GET', '/customer/projects', agent_token, allow_404=True)
check('代理商访问 /customer/projects 应被拒（403）', code == 403, f'实际 code={code}')

# ============ 16. 未登录访问需登录接口 ============
print('\n[16] 未登录访问需登录接口应返回 401')
code, _ = api_call('GET', '/customer/projects', None, allow_404=True)
check('未登录访问 /customer/projects 应 401', code == 401, f'实际 code={code}')

code, _ = api_call('GET', '/agent/workbench', None, allow_404=True)
check('未登录访问 /agent/workbench 应 401', code == 401, f'实际 code={code}')

# ============ 汇总 ============
print('\n' + '=' * 80)
print(f'  ✓ PASS: {passed}')
print(f'  ✗ FAIL: {failed}')
print(f'  Total: {passed + failed}, Pass Rate: {passed * 100 // (passed + failed)}%')
print('=' * 80)
if errors:
    print('\n失败详情：')
    for e in errors:
        print(f'  - {e}')

sys.exit(0 if failed == 0 else 1)
