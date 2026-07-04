"""
3.5.2 代理商 H5 门户后端 - 端到端测试

Tests:
1. Setup: 创建代理商公司、代理商用户、项目、项目任务、转包任务
2. Login as agent
3. GET /agent/workbench - 工作台 (统计+top N 任务)
4. Setup: 插入 agent_message 测试消息
5. GET /agent/messages - 消息列表
6. GET /agent/messages/unread-count - 未读数
7. POST /agent/messages/{id}/read - 标记单条已读
8. POST /agent/messages/read-all - 全部已读
9. GET /agent/workbench (验证未读数变化)
10. Cleanup
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

TEST_ID = 9100001


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
            sample = json.dumps(data, ensure_ascii=False)[:400]
            print(f' | code={code} msg="{msg}" data={sample}')
        else:
            print(f' | code={code} msg="{msg}" data={data}')
    except Exception:
        print(f' | body={resp.text[:300]}')
    return resp


# ============ Setup: agent_company + agent user + project + project_task + outsource_task ============
banner('Setup: agent + project + tasks')
conn = pymysql.connect(**DB)
cur = conn.cursor()

# Ensure agent company (id=9101)
cur.execute("SELECT id FROM agent_company WHERE id=9101")
if not cur.fetchone():
    cur.execute("""
        INSERT INTO agent_company (id, company_name, company_code, qualification, contact_name, contact_phone,
                                    status, create_by, create_time, update_by, update_time, deleted)
        VALUES (9101, '测试代理商公司', 'AGT-TEST-001', 'A级', '代理商联系人', '13900009001',
                'ACTIVE', 1, NOW(), 1, NOW(), 0)
    """)
    print('  -> created agent_company id=9101')
else:
    print('  -> agent_company 9101 exists')

# Ensure agent user (id=200001, AGENT_ADMIN role)
cur.execute("SELECT id FROM sys_user WHERE id=200001")
if not cur.fetchone():
    bcrypt_hash = '$2a$10$VV2l4ZaqyUWJJD4.HBC16.2LzQgkE2KJ0rS3s/dA44NmPa0KFu/rK'  # admin123
    cur.execute("""
        INSERT INTO sys_user (id, username, password, real_name, phone, email, status, tenant_type, tenant_id, org_id,
                              create_time, update_time)
        VALUES (200001, 'testagent', %s, '测试代理商管理员', '13900009001', 'agent@test.com', 'ACTIVE',
                'AGENT', 9101, 1, NOW(), NOW())
    """, (bcrypt_hash,))
    print('  -> created agent user id=200001')
else:
    cur.execute("UPDATE sys_user SET tenant_id=9101, tenant_type='AGENT' WHERE id=200001")
    print('  -> agent user 200001 exists, ensured tenant_id=9101')

# Assign AGENT_ADMIN role (id=8)
cur.execute("SELECT id FROM sys_role WHERE role_code='AGENT_ADMIN'")
role_row = cur.fetchone()
agent_admin_role_id = role_row[0] if role_row else 8
cur.execute("SELECT id FROM sys_user_role WHERE user_id=200001 AND role_id=%s AND deleted=0", (agent_admin_role_id,))
if not cur.fetchone():
    ur_id = next_id()
    cur.execute("INSERT INTO sys_user_role (id, user_id, role_id, create_by, create_time, update_by, update_time, deleted) VALUES (%s, 200001, %s, 1, NOW(), 1, NOW(), 0)", (ur_id, agent_admin_role_id))
    print(f'  -> assigned AGENT_ADMIN role (id={agent_admin_role_id})')
else:
    print('  -> AGENT_ADMIN role already assigned')

# Ensure project + project_task (id=9101, 9102)
cur.execute("SELECT id FROM project WHERE id=9101")
if not cur.fetchone():
    cur.execute("""
        INSERT INTO project (id, project_code, project_name, customer_id, project_type, product_line, execute_mode,
                             priority, status, current_phase, pm_id, region, planned_start, planned_end,
                             progress_pct, version, create_by, create_time, update_by, update_time, deleted)
        VALUES (9101, 'PRJ-AGT-001', '代理商测试项目', 9001, '新建', '路由', 'MIXED',
                'P2', 'EXECUTE', 'DELIVER', 1, '华东', '2026-06-01', '2026-08-31',
                30, 1, 1, NOW(), 1, NOW(), 0)
    """)
    print('  -> created project id=9101')
else:
    print('  -> project 9101 exists')

cur.execute("SELECT id FROM project_task WHERE id=9101")
if not cur.fetchone():
    cur.execute("""
        INSERT INTO project_task (id, project_id, task_name, task_type, status, execute_mode,
                                   planned_start, planned_end, priority, version,
                                   create_by, create_time, update_by, update_time, deleted)
        VALUES (9101, 9101, '代理商测试任务1', 'INSTALL', 'IN_PROGRESS', 'AGENT',
                '2026-07-01', '2026-07-20', 'HIGH', 1,
                1, NOW(), 1, NOW(), 0)
    """)
    print('  -> created project_task id=9101')
else:
    print('  -> project_task 9101 exists')

# Insert outsource_task records (3 statuses: PENDING / IN_PROGRESS / SUBMITTED / OVERDUE)
outsource_task_ids = []
for idx, (status, deadline) in enumerate([
    ('PENDING', '2026-07-25'),
    ('PENDING', '2026-07-30'),
    ('IN_PROGRESS', '2026-07-20'),
    ('SUBMITTED', '2026-07-15'),
    ('OVERDUE', '2026-06-30'),
]):
    ot_id = next_id()
    cur.execute("""
        INSERT INTO outsource_task (id, project_id, task_id, agent_company_id, task_scope, deadline,
                                     status, submit_count, version, create_by, create_time, update_by, update_time, deleted)
        VALUES (%s, 9101, 9101, 9101, %s, %s, %s, %s, 1, 1, NOW(), 1, NOW(), 0)
    """, (ot_id, f'测试任务范围-{idx+1}', deadline, status, 1 if status == 'SUBMITTED' else 0))
    outsource_task_ids.append((ot_id, status))
    print(f'  -> created outsource_task id={ot_id} status={status}')

conn.commit()
conn.close()

# ============ Login as agent ============
banner('Login as agent (testagent)')
r = requests.post(f'{BASE}/auth/login',
                  json={'username': 'testagent', 'password': 'admin123', 'clientId': 'web'},
                  timeout=10)
show('agent login', r)
agent_token = r.json().get('data', {}).get('token')
if agent_token:
    print(f'  -> agent token: {agent_token[:40]}...')
    agent_headers = {'Authorization': f'Bearer {agent_token}'}
else:
    print('  !! Agent login failed')
    agent_headers = None

# ============ Test: GET /agent/workbench ============
banner('GET /agent/workbench')
if agent_headers:
    r = requests.get(f'{BASE}/agent/workbench', headers=agent_headers, timeout=10)
    show('workbench', r)
    wb = r.json().get('data', {})
    summary = wb.get('summary', {}) if wb else None
    if summary:
        print(f'  >> 待接单={summary.get("pendingCount")} 进行中={summary.get("inProgressCount")} '
              f'待审核={summary.get("submittedCount")} 已超期={summary.get("overdueCount")} '
              f'未读消息={summary.get("unreadMessageCount")}')
        print(f'  >> pendingTasks={len(wb.get("pendingTasks") or [])} '
              f'inProgressTasks={len(wb.get("inProgressTasks") or [])} '
              f'submittedTasks={len(wb.get("submittedTasks") or [])}')

# ============ Setup: insert agent_message ============
banner('Setup: insert agent messages')
msg_ids = []
conn = pymysql.connect(**DB)
cur = conn.cursor()
for i, (mtype, title, content) in enumerate([
    ('TASK_ASSIGNED', '新任务派发通知', '您有一个新任务「测试任务范围-1」待接单'),
    ('TASK_EXPIRING', '任务即将到期', '任务「测试任务范围-3」将在5天内到期'),
]):
    mid = next_id()
    cur.execute("""
        INSERT INTO agent_message (id, agent_company_id, message_type, business_id, project_id, title, content, is_read, create_time)
        VALUES (%s, 9101, %s, NULL, 9101, %s, %s, 0, NOW())
    """, (mid, mtype, title, content))
    msg_ids.append(mid)
conn.commit()
conn.close()
print(f'  -> inserted 2 messages: {msg_ids}')

# ============ Test: GET /agent/messages ============
banner('GET /agent/messages')
if agent_headers:
    r = requests.get(f'{BASE}/agent/messages', headers=agent_headers, timeout=10)
    show('messages list', r)

    banner('GET /agent/messages/unread-count')
    r = requests.get(f'{BASE}/agent/messages/unread-count', headers=agent_headers, timeout=10)
    show('unread count', r)

    banner(f'POST /agent/messages/{msg_ids[0]}/read')
    r = requests.post(f'{BASE}/agent/messages/{msg_ids[0]}/read', headers=agent_headers, timeout=10)
    show('mark one read', r)

    banner('GET /agent/messages/unread-count (after mark one)')
    r = requests.get(f'{BASE}/agent/messages/unread-count', headers=agent_headers, timeout=10)
    show('unread count after', r)

    banner('POST /agent/messages/read-all')
    r = requests.post(f'{BASE}/agent/messages/read-all', headers=agent_headers, timeout=10)
    show('mark all read', r)

    banner('GET /agent/messages/unread-count (final)')
    r = requests.get(f'{BASE}/agent/messages/unread-count', headers=agent_headers, timeout=10)
    show('unread count final', r)

    banner('GET /agent/workbench (verify unread=0 in summary)')
    r = requests.get(f'{BASE}/agent/workbench', headers=agent_headers, timeout=10)
    show('workbench final', r)

# ============ Cleanup ============
banner('Cleanup test data')
conn = pymysql.connect(**DB)
cur = conn.cursor()
cur.execute("DELETE FROM agent_message WHERE id IN %s", (tuple(msg_ids),))
for ot_id, _ in outsource_task_ids:
    cur.execute("DELETE FROM outsource_task WHERE id=%s", (ot_id,))
cur.execute("DELETE FROM project_task WHERE id=9101")
cur.execute("DELETE FROM project WHERE id=9101")
# Keep agent_company + agent_user for future test runs
conn.commit()
conn.close()
print('  -> cleaned up test data')

print('\n========== TEST COMPLETE ==========')
