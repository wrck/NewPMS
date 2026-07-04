"""
全量回归测试 - 覆盖所有主要模块

Tests:
1. 认证模块 (auth)
2. 系统管理 (sys: user/role/menu/dict/config/log)
3. 项目管理 (project)
4. 设备管理 (device)
5. 资源管理 (resource)
6. 交付管理 (delivery: cutover/schedule)
7. 验收管理 (acceptance)
8. 财务管理 (finance)
9. 集成管理 (integration)
10. 报表分析 (report)
11. 客户门户 (customer)
12. 代理商门户 (agent)
13. 转包任务 (outsource-tasks)
"""
import json
import time
import requests
import pymysql
from urllib3.exceptions import InsecureRequestWarning

requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

BASE = 'http://localhost:8080/api/v1'
DB = dict(host='localhost', port=3306, user='root', password='!Q@W3e4r',
          database='vibe_db', charset='utf8mb4')

# Test results tracker
PASS = 0
FAIL = 0
ERRORS = []


def banner(title):
    print(f'\n{"="*70}\n  {title}\n{"="*70}')


def check(label, resp, expected_code=200, expected_status=None):
    """Check API response and track results"""
    global PASS, FAIL
    try:
        body = resp.json()
        code = body.get('code')
        msg = body.get('message', '')
        data = body.get('data')
        ok = (resp.status_code == 200 and code == expected_code)
        if expected_status and isinstance(data, dict):
            actual_status = data.get('status')
            if actual_status != expected_status:
                ok = False
        status_mark = '✓' if ok else '✗'
        if ok:
            PASS += 1
        else:
            FAIL += 1
            ERRORS.append(f'{label}: HTTP={resp.status_code} code={code} msg={msg}')
        if isinstance(data, (dict, list)):
            sample = json.dumps(data, ensure_ascii=False)[:120]
        else:
            sample = str(data)[:120] if data is not None else 'null'
        print(f'  {status_mark} {label}: HTTP {resp.status_code} code={code} data={sample}')
        return body
    except Exception as e:
        FAIL += 1
        ERRORS.append(f'{label}: exception={e}')
        print(f'  ✗ {label}: exception={e}, body={resp.text[:100]}')
        return None


# ============ Setup: ensure test data exists ============
banner('Setup: test data')
conn = pymysql.connect(**DB)
cur = conn.cursor()
# Ensure customer 9001 + project 9001 + agent 9101 + agent user 200001 + customer user 100001
cur.execute("SELECT COUNT(*) FROM customer WHERE id=9001")
if cur.fetchone()[0] == 0:
    cur.execute("""INSERT INTO customer (id, customer_name, customer_code, contact_name, contact_phone, region, industry,
                  create_by, create_time, update_by, update_time, deleted)
                  VALUES (9001, '测试客户公司', 'CUST-TEST-001', '测试联系人', '13900000001', '华东', 'IT', 1, NOW(), 1, NOW(), 0)""")
    print('  -> created customer 9001')

cur.execute("SELECT COUNT(*) FROM sys_user WHERE id=100001")
if cur.fetchone()[0] == 0:
    bcrypt_hash = '$2a$10$VV2l4ZaqyUWJJD4.HBC16.2LzQgkE2KJ0rS3s/dA44NmPa0KFu/rK'
    cur.execute("""INSERT INTO sys_user (id, username, password, real_name, status, tenant_type, tenant_id, org_id,
                  create_time, update_time)
                  VALUES (100001, 'testcustomer', %s, '测试客户', 'ACTIVE', 'CUSTOMER', 9001, 1, NOW(), NOW())""", (bcrypt_hash,))
    print('  -> created customer user 100001')
else:
    cur.execute("UPDATE sys_user SET tenant_id=9001, tenant_type='CUSTOMER' WHERE id=100001")

cur.execute("SELECT COUNT(*) FROM sys_user_role WHERE user_id=100001 AND role_id=10 AND deleted=0")
if cur.fetchone()[0] == 0:
    cur.execute("INSERT INTO sys_user_role (id, user_id, role_id, create_by, create_time, update_by, update_time, deleted) VALUES (9901001, 100001, 10, 1, NOW(), 1, NOW(), 0)")
    print('  -> assigned CUSTOMER role to user 100001')

cur.execute("SELECT COUNT(*) FROM agent_company WHERE id=9101")
if cur.fetchone()[0] == 0:
    cur.execute("""INSERT INTO agent_company (id, company_name, company_code, contact_name, contact_phone, status,
                  create_by, create_time, update_by, update_time, deleted)
                  VALUES (9101, '测试代理商公司', 'AGT-TEST-001', '代理商联系人', '13900009001', 'ACTIVE', 1, NOW(), 1, NOW(), 0)""")
    print('  -> created agent_company 9101')

cur.execute("SELECT COUNT(*) FROM sys_user WHERE id=200001")
if cur.fetchone()[0] == 0:
    bcrypt_hash = '$2a$10$VV2l4ZaqyUWJJD4.HBC16.2LzQgkE2KJ0rS3s/dA44NmPa0KFu/rK'
    cur.execute("""INSERT INTO sys_user (id, username, password, real_name, status, tenant_type, tenant_id, org_id,
                  create_time, update_time)
                  VALUES (200001, 'testagent', %s, '测试代理商管理员', 'ACTIVE', 'AGENT', 9101, 1, NOW(), NOW())""", (bcrypt_hash,))
    print('  -> created agent user 200001')
else:
    cur.execute("UPDATE sys_user SET tenant_id=9101, tenant_type='AGENT' WHERE id=200001")

cur.execute("SELECT COUNT(*) FROM sys_user_role WHERE user_id=200001 AND role_id=8 AND deleted=0")
if cur.fetchone()[0] == 0:
    cur.execute("INSERT INTO sys_user_role (id, user_id, role_id, create_by, create_time, update_by, update_time, deleted) VALUES (9901002, 200001, 8, 1, NOW(), 1, NOW(), 0)")
    print('  -> assigned AGENT_ADMIN role to user 200001')

conn.commit()
conn.close()

# ============ Login all users ============
banner('Login: admin / customer / agent')
r = requests.post(f'{BASE}/auth/login',
                  json={'username': 'admin', 'password': 'admin123', 'clientId': 'web'}, timeout=10)
admin_body = check('admin login', r)
admin_token = admin_body.get('data', {}).get('token') if admin_body else None
admin_h = {'Authorization': f'Bearer {admin_token}'} if admin_token else None

r = requests.post(f'{BASE}/auth/login',
                  json={'username': 'testcustomer', 'password': 'admin123', 'clientId': 'web'}, timeout=10)
cust_body = check('customer login', r)
cust_token = cust_body.get('data', {}).get('token') if cust_body else None
cust_h = {'Authorization': f'Bearer {cust_token}'} if cust_token else None

r = requests.post(f'{BASE}/auth/login',
                  json={'username': 'testagent', 'password': 'admin123', 'clientId': 'web'}, timeout=10)
agent_body = check('agent login', r)
agent_token = agent_body.get('data', {}).get('token') if agent_body else None
agent_h = {'Authorization': f'Bearer {agent_token}'} if agent_token else None

# ============ 1. 系统管理 ============
banner('1. 系统管理 (sys)')
if admin_h:
    r = requests.get(f'{BASE}/users?page=1&size=5', headers=admin_h, timeout=10)
    check('GET /users', r)
    r = requests.get(f'{BASE}/roles', headers=admin_h, timeout=10)
    check('GET /roles', r)
    r = requests.get(f'{BASE}/menus', headers=admin_h, timeout=10)
    check('GET /menus', r)
    r = requests.get(f'{BASE}/dicts/types', headers=admin_h, timeout=10)
    check('GET /dicts/types', r)
    r = requests.get(f'{BASE}/configs?page=1&size=5', headers=admin_h, timeout=10)
    check('GET /configs', r)
    r = requests.get(f'{BASE}/logs/login?page=1&size=5', headers=admin_h, timeout=10)
    check('GET /logs/login', r)
    r = requests.get(f'{BASE}/orgs', headers=admin_h, timeout=10)
    check('GET /orgs', r)
    r = requests.get(f'{BASE}/positions', headers=admin_h, timeout=10)
    check('GET /positions', r)

# ============ 2. 项目管理 ============
banner('2. 项目管理 (project)')
if admin_h:
    r = requests.get(f'{BASE}/projects?page=1&size=5', headers=admin_h, timeout=10)
    check('GET /projects', r)

# ============ 3. 设备管理 ============
banner('3. 设备管理 (device)')
if admin_h:
    r = requests.get(f'{BASE}/devices/instances?page=1&size=5', headers=admin_h, timeout=10)
    check('GET /devices/instances', r)
    r = requests.get(f'{BASE}/devices/boms?page=1&size=5', headers=admin_h, timeout=10)
    check('GET /devices/boms', r)
    r = requests.get(f'{BASE}/devices/warehouses?page=1&size=5', headers=admin_h, timeout=10)
    check('GET /devices/warehouses', r)
    r = requests.get(f'{BASE}/devices/models?page=1&size=5', headers=admin_h, timeout=10)
    check('GET /devices/models', r)
    r = requests.get(f'{BASE}/devices/dashboard', headers=admin_h, timeout=10)
    check('GET /devices/dashboard', r)

# ============ 4. 资源管理 ============
banner('4. 资源管理 (resource)')
if admin_h:
    r = requests.get(f'{BASE}/engineers?page=1&size=5', headers=admin_h, timeout=10)
    check('GET /engineers', r)
    r = requests.get(f'{BASE}/schedules?page=1&size=5', headers=admin_h, timeout=10)
    check('GET /schedules', r)

# ============ 5. 交付管理 (割接 + 排期) ============
banner('5. 交付管理 (delivery)')
if admin_h:
    r = requests.get(f'{BASE}/cutover/plans?page=1&size=5', headers=admin_h, timeout=10)
    check('GET /cutover/plans', r)

# ============ 6. 验收管理 ============
banner('6. 验收管理 (acceptance)')
if admin_h:
    r = requests.get(f'{BASE}/acceptance/tasks?page=1&size=5', headers=admin_h, timeout=10)
    check('GET /acceptance/tasks', r)

# ============ 7. 财务管理 ============
banner('7. 财务管理 (finance)')
if admin_h:
    r = requests.get(f'{BASE}/finance/costs?page=1&size=5', headers=admin_h, timeout=10)
    check('GET /finance/costs', r)

# ============ 8. 集成管理 ============
banner('8. 集成管理 (integration)')
if admin_h:
    r = requests.get(f'{BASE}/integration/configs?page=1&size=5', headers=admin_h, timeout=10)
    check('GET /integration/configs', r)
    r = requests.get(f'{BASE}/integration/call-logs?page=1&size=5', headers=admin_h, timeout=10)
    check('GET /integration/call-logs', r)

# ============ 9. 报表分析 ============
banner('9. 报表分析 (report)')
if admin_h:
    r = requests.get(f'{BASE}/report/project', headers=admin_h, timeout=10)
    check('GET /report/project', r)
    r = requests.get(f'{BASE}/report/device', headers=admin_h, timeout=10)
    check('GET /report/device', r)
    r = requests.get(f'{BASE}/report/resource', headers=admin_h, timeout=10)
    check('GET /report/resource', r)
    r = requests.get(f'{BASE}/report/finance', headers=admin_h, timeout=10)
    check('GET /report/finance', r)

# ============ 10. 客户门户 ============
banner('10. 客户门户 (customer)')
if cust_h:
    r = requests.get(f'{BASE}/customer/projects', headers=cust_h, timeout=10)
    check('GET /customer/projects', r)
    r = requests.get(f'{BASE}/customer/todos', headers=cust_h, timeout=10)
    check('GET /customer/todos', r)
    r = requests.get(f'{BASE}/customer/messages', headers=cust_h, timeout=10)
    check('GET /customer/messages', r)
    r = requests.get(f'{BASE}/customer/messages/unread-count', headers=cust_h, timeout=10)
    check('GET /customer/messages/unread-count', r)

# ============ 11. 代理商门户 ============
banner('11. 代理商门户 (agent)')
if agent_h:
    r = requests.get(f'{BASE}/agent/workbench', headers=agent_h, timeout=10)
    check('GET /agent/workbench', r)
    r = requests.get(f'{BASE}/agent/messages', headers=agent_h, timeout=10)
    check('GET /agent/messages', r)
    r = requests.get(f'{BASE}/agent/messages/unread-count', headers=agent_h, timeout=10)
    check('GET /agent/messages/unread-count', r)

# ============ 12. 转包任务 ============
banner('12. 转包任务 (outsource-tasks)')
if agent_h:
    r = requests.get(f'{BASE}/outsource-tasks?page=1&size=5', headers=agent_h, timeout=10)
    check('GET /outsource-tasks', r)

# ============ 13. 驾驶舱 (cockpit) ============
banner('13. 驾驶舱 (cockpit)')
if admin_h:
    r = requests.get(f'{BASE}/cockpit', headers=admin_h, timeout=10)
    check('GET /cockpit', r)
    r = requests.get(f'{BASE}/cockpit/stats', headers=admin_h, timeout=10)
    check('GET /cockpit/stats', r)
    r = requests.get(f'{BASE}/cockpit/project-phases', headers=admin_h, timeout=10)
    check('GET /cockpit/project-phases', r)

# ============ Summary ============
banner('TEST SUMMARY')
print(f'\n  ✓ PASS: {PASS}')
print(f'  ✗ FAIL: {FAIL}')
if ERRORS:
    print(f'\n  Errors:')
    for e in ERRORS:
        print(f'    - {e}')
print(f'\n  Total: {PASS + FAIL}, Pass Rate: {PASS * 100 // (PASS + FAIL) if (PASS + FAIL) > 0 else 0}%')
print('\n========== REGRESSION TEST COMPLETE ==========')
