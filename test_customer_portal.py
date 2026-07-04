"""
2.6.3 客户协作通道交互接口 - 端到端测试

Tests:
1. Setup: create customer record + customer user + project
2. Login as customer, get token
3. 3.1 GET /customer/projects - 客户项目列表
4. Setup: admin creates cutover plan + steps + submits internal approval + approves + starts customer approval
5. 3.2 GET /customer/cutover/{token} - 按 token 查询割接方案 (no auth)
6. 3.2 POST /customer/cutover/approval - 客户提交割接审批
7. Setup: create acceptance task + test records with customer_sign_link token
8. 3.3 GET /customer/acceptance/{token} - 按 token 查询验收任务 (no auth)
9. 3.3 POST /customer/acceptance/sign - 客户提交验收签核
10. 3.4 GET /customer/todos - 客户待办
11. 3.5 GET /customer/messages - 消息列表
12. 3.5 GET /customer/messages/unread-count - 未读数
13. 3.5 POST /customer/messages/{id}/read - 标记单条已读
14. 3.5 POST /customer/messages/read-all - 全部已读
15. Cleanup
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

# Snowflake-like id pool for test data (use 99xxxxx range to avoid collision)
TEST_ID = 9900001


def next_id():
    global TEST_ID
    TEST_ID += 1
    return TEST_ID


def banner(title):
    print(f'\n========== {title} ==========')


def show(label, resp):
    print(f'  -> {label}: HTTP {resp.status_code}', end='')
    try:
        body = resp.json()
        code = body.get('code')
        msg = body.get('message', '')
        data = body.get('data')
        if isinstance(data, (dict, list)):
            sample = json.dumps(data, ensure_ascii=False)[:300]
            print(f' | code={code} msg="{msg}" data={sample}')
        else:
            print(f' | code={code} msg="{msg}" data={data}')
    except Exception:
        print(f' | body={resp.text[:300]}')
    return resp


# ============ Setup: customer record + customer user + project ============
banner('Setup: customer + user + project')
conn = pymysql.connect(**DB)
cur = conn.cursor()

# Ensure customer record (id=9001)
cur.execute("SELECT id FROM customer WHERE id=9001")
if not cur.fetchone():
    cur.execute("""
        INSERT INTO customer (id, customer_name, customer_code, contact_name, contact_phone, region, industry,
                              create_by, create_time, update_by, update_time, deleted)
        VALUES (9001, '测试客户公司', 'CUST-TEST-001', '测试联系人', '13900000001', '华东', 'IT',
                1, NOW(), 1, NOW(), 0)
    """)
    print('  -> created customer id=9001')
else:
    print('  -> customer 9001 exists')

# Ensure customer user (id=100001)
cur.execute("SELECT id FROM sys_user WHERE id=100001")
if not cur.fetchone():
    bcrypt_hash = '$2a$10$VV2l4ZaqyUWJJD4.HBC16.2LzQgkE2KJ0rS3s/dA44NmPa0KFu/rK'  # admin123
    cur.execute("""
        INSERT INTO sys_user (id, username, password, real_name, phone, email, status, tenant_type, tenant_id, org_id,
                              create_time, update_time)
        VALUES (100001, 'testcustomer', %s, '测试客户', '13900000001', 'customer@test.com', 'ACTIVE',
                'CUSTOMER', 9001, 1, NOW(), NOW())
    """, (bcrypt_hash,))
    print('  -> created user id=100001')
else:
    # Ensure tenant_id is 9001 (in case of legacy test data)
    cur.execute("UPDATE sys_user SET tenant_id=9001, tenant_type='CUSTOMER' WHERE id=100001")
    print('  -> user 100001 exists, ensured tenant_id=9001')

# Assign CUSTOMER role (id=10) to user 100001 - sys_user_role requires id field
cur.execute("SELECT id FROM sys_user_role WHERE user_id=100001 AND role_id=10 AND deleted=0")
if not cur.fetchone():
    ur_id = next_id()
    cur.execute("INSERT INTO sys_user_role (id, user_id, role_id, create_by, create_time, update_by, update_time, deleted) VALUES (%s, 100001, 10, 1, NOW(), 1, NOW(), 0)", (ur_id,))
    print(f'  -> assigned CUSTOMER role (user_role id={ur_id})')
else:
    print('  -> CUSTOMER role already assigned')

# Ensure project (id=9001) linked to customer 9001
cur.execute("SELECT id FROM project WHERE id=9001")
if not cur.fetchone():
    cur.execute("""
        INSERT INTO project (id, project_code, project_name, customer_id, project_type, product_line, execute_mode,
                             priority, status, current_phase, pm_id, region, planned_start, planned_end,
                             progress_pct, version, create_by, create_time, update_by, update_time, deleted)
        VALUES (9001, 'PRJ-TEST-001', '测试项目', 9001, '新建', '路由', 'SELF',
                'P2', 'EXECUTE', 'DELIVER', 1, '华东', '2026-06-01', '2026-08-31',
                30, 1, 1, NOW(), 1, NOW(), 0)
    """)
    print('  -> created project id=9001')
else:
    print('  -> project 9001 exists')

conn.commit()
conn.close()

# ============ Login as admin ============
banner('Login as admin')
r = requests.post(f'{BASE}/auth/login',
                  json={'username': 'admin', 'password': 'admin123', 'clientId': 'web'},
                  timeout=10)
show('admin login', r)
admin_token = r.json().get('data', {}).get('token')
admin_headers = {'Authorization': f'Bearer {admin_token}'} if admin_token else None

# ============ Login as customer ============
banner('Login as customer (testcustomer)')
r = requests.post(f'{BASE}/auth/login',
                  json={'username': 'testcustomer', 'password': 'admin123', 'clientId': 'web'},
                  timeout=10)
show('customer login', r)
customer_token = r.json().get('data', {}).get('token')
if customer_token:
    print(f'  -> customer token: {customer_token[:40]}...')
    customer_headers = {'Authorization': f'Bearer {customer_token}'}
else:
    print('  !! Customer login failed')
    customer_headers = None

# ============ Test 3.1: Customer project list ============
banner('3.1 GET /customer/projects')
r = requests.get(f'{BASE}/customer/projects', headers=customer_headers, timeout=10)
show('customer projects', r)

# ============ Setup: cutover plan + customer approval token ============
banner('Setup: create cutover plan + steps')
plan_id = None
cutover_token = None
if admin_headers:
    create_body = {
        'projectId': 9001,
        'planName': '客户协作通道测试割接方案',
        'cutoverDate': '2026-07-15',
        'startTime': '2026-07-15T22:00:00',
        'endTime': '2026-07-16T02:00:00',
        'impactScope': '测试影响范围',
        'emergencyContact': '应急联系人',
        'remark': '客户协作通道测试',
        'steps': [
            {'sortOrder': 1, 'stepName': '步骤1-准备', 'estimatedDuration': 30,
             'ownerName': '张三', 'rollbackPlan': '回退方案1'},
            {'sortOrder': 2, 'stepName': '步骤2-切换', 'estimatedDuration': 60,
             'ownerName': '李四', 'rollbackPlan': '回退方案2'},
        ]
    }
    r = requests.post(f'{BASE}/cutover/plans', json=create_body, headers=admin_headers, timeout=10)
    show('create plan', r)
    plan_id = r.json().get('data')

    if plan_id:
        r = requests.post(f'{BASE}/cutover/plans/{plan_id}/submit-internal-approval',
                          headers=admin_headers, timeout=10)
        show('submit internal approval', r)

        r = requests.post(f'{BASE}/cutover/plans/internal-approve',
                          json={'planId': plan_id, 'result': 'APPROVED', 'remark': '内部同意'},
                          headers=admin_headers, timeout=10)
        show('internal approve', r)

        r = requests.post(f'{BASE}/cutover/plans/{plan_id}/start-customer-approval',
                          headers=admin_headers, timeout=10)
        show('start customer approval', r)
        cutover_token = r.json().get('data')
        print(f'  -> cutover_token: {cutover_token}')

# ============ Test 3.2: Get cutover plan by token (no auth) ============
banner('3.2 GET /customer/cutover/{token} (no auth)')
if cutover_token:
    r = requests.get(f'{BASE}/customer/cutover/{cutover_token}', timeout=10)
    show('cutover by token', r)
else:
    print('  -> SKIPPED (no token)')

# ============ Test 3.2: Submit cutover approval ============
banner('3.2 POST /customer/cutover/approval')
if customer_headers and cutover_token:
    r = requests.post(f'{BASE}/customer/cutover/approval',
                      json={'token': cutover_token, 'result': 'APPROVED', 'remark': '客户同意割接'},
                      headers=customer_headers, timeout=10)
    show('submit cutover approval', r)
else:
    print('  -> SKIPPED')

# Verify plan status changed
banner('Verify cutover plan status -> CUSTOMER_APPROVED')
if admin_headers and plan_id:
    r = requests.get(f'{BASE}/cutover/plans/{plan_id}', headers=admin_headers, timeout=10)
    show('plan detail', r)

# ============ Setup: acceptance task with customer_sign_link token ============
banner('Setup: create acceptance task + test records (DB)')
acceptance_token = f'test_acc_token_{int(time.time())}'
conn = pymysql.connect(**DB)
cur = conn.cursor()
task_id = next_id()
cur.execute("""
    INSERT INTO acceptance_task (id, project_id, name, apply_user_id, apply_time,
                                  internal_audit_user_id, internal_audit_time, internal_audit_result,
                                  customer_sign_link, status, remark, version, create_time, update_time, deleted)
    VALUES (%s, 9001, '客户协作通道测试验收任务', 1, NOW(),
            1, NOW(), 'PASS',
            %s, 'CUSTOMER_SIGNING', '客户签核测试', 1, NOW(), NOW(), 0)
""", (task_id, acceptance_token))
tr1_id = next_id()
tr2_id = next_id()
cur.execute("""
    INSERT INTO acceptance_test_record (id, task_id, test_type, test_name, test_result, test_value, test_time, remark,
                                         version, create_time, update_time, deleted)
    VALUES (%s, %s, 'FUNCTION', '功能测试', 'PASS', NULL, NOW(), '功能正常', 1, NOW(), NOW(), 0)
""", (tr1_id, task_id))
cur.execute("""
    INSERT INTO acceptance_test_record (id, task_id, test_type, test_name, test_result, test_value, test_time, remark,
                                         version, create_time, update_time, deleted)
    VALUES (%s, %s, 'PERFORMANCE', '性能测试', 'PASS', '99.5ms', NOW(), '响应时间正常', 1, NOW(), NOW(), 0)
""", (tr2_id, task_id))
conn.commit()
conn.close()
print(f'  -> created acceptance_task id={task_id}, 2 test records, token={acceptance_token}')

# ============ Test 3.3: Get acceptance task by token ============
banner('3.3 GET /customer/acceptance/{token} (no auth)')
r = requests.get(f'{BASE}/customer/acceptance/{acceptance_token}', timeout=10)
show('acceptance by token', r)

# ============ Test 3.3: Submit acceptance sign ============
banner('3.3 POST /customer/acceptance/sign')
if customer_headers:
    r = requests.post(f'{BASE}/customer/acceptance/sign',
                      json={'token': acceptance_token, 'result': 'PASS', 'remark': '客户验收通过'},
                      headers=customer_headers, timeout=10)
    show('submit acceptance sign', r)
else:
    print('  -> SKIPPED')

# ============ Test 3.5: Customer messages ============
banner('Setup: insert customer messages')
msg_ids = []
conn = pymysql.connect(**DB)
cur = conn.cursor()
for i, (mtype, title, content) in enumerate([
    ('CUTOVER_NOTICE', '割接方案待审批通知', '您有一个割接方案「客户协作通道测试割接方案」待审批'),
    ('PROJECT_PROGRESS', '项目进度更新', '项目已进入执行阶段'),
]):
    mid = next_id()
    cur.execute("""
        INSERT INTO customer_message (id, customer_id, message_type, business_id, project_id, title, content, is_read, create_time)
        VALUES (%s, 9001, %s, NULL, 9001, %s, %s, 0, NOW())
    """, (mid, mtype, title, content))
    msg_ids.append(mid)
conn.commit()
conn.close()
print(f'  -> inserted 2 messages: {msg_ids}')

banner('3.5 GET /customer/messages')
if customer_headers:
    r = requests.get(f'{BASE}/customer/messages', headers=customer_headers, timeout=10)
    show('messages list', r)

    banner('3.5 GET /customer/messages/unread-count')
    r = requests.get(f'{BASE}/customer/messages/unread-count', headers=customer_headers, timeout=10)
    show('unread count', r)

    banner(f'3.5 POST /customer/messages/{msg_ids[0]}/read')
    r = requests.post(f'{BASE}/customer/messages/{msg_ids[0]}/read', headers=customer_headers, timeout=10)
    show('mark one read', r)

    banner('3.5 GET /customer/messages/unread-count (after mark one)')
    r = requests.get(f'{BASE}/customer/messages/unread-count', headers=customer_headers, timeout=10)
    show('unread count after', r)

    banner('3.5 POST /customer/messages/read-all')
    r = requests.post(f'{BASE}/customer/messages/read-all', headers=customer_headers, timeout=10)
    show('mark all read', r)

    banner('3.5 GET /customer/messages/unread-count (final)')
    r = requests.get(f'{BASE}/customer/messages/unread-count', headers=customer_headers, timeout=10)
    show('unread count final', r)

# ============ Test 3.4: Customer todos ============
banner('3.4 GET /customer/todos')
if customer_headers:
    r = requests.get(f'{BASE}/customer/todos', headers=customer_headers, timeout=10)
    show('todos', r)

# ============ Cleanup ============
banner('Cleanup test data')
conn = pymysql.connect(**DB)
cur = conn.cursor()
cur.execute("DELETE FROM customer_message WHERE id IN %s", (tuple(msg_ids),))
cur.execute("DELETE FROM acceptance_test_record WHERE task_id=%s", (task_id,))
cur.execute("DELETE FROM acceptance_task WHERE id=%s", (task_id,))
if plan_id:
    cur.execute("DELETE FROM cutover_step WHERE plan_id=%s", (plan_id,))
    cur.execute("DELETE FROM cutover_execution_log WHERE plan_id=%s", (plan_id,))
    cur.execute("DELETE FROM cutover_plan WHERE id=%s", (plan_id,))
conn.commit()
conn.close()
print('  -> cleaned up test data')

print('\n========== TEST COMPLETE ==========')
