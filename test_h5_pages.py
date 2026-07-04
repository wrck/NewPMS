# -*- coding: utf-8 -*-
"""测试 H5 页面是否能通过 Vite 加载"""
import sys
import io
import urllib.request
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

urls = [
    'http://localhost:5173/h5/customer/login',
    'http://localhost:5173/h5/customer/projects',
    'http://localhost:5173/h5/customer/cutover/test-token',
    'http://localhost:5173/h5/customer/acceptance/test-token',
    'http://localhost:5173/h5/customer/todos',
    'http://localhost:5173/h5/customer/messages',
    'http://localhost:5173/h5/agent/login',
    'http://localhost:5173/h5/agent/workbench',
    'http://localhost:5173/h5/agent/messages',
    'http://localhost:5173/h5/agent/deliverable-submit',
]

ok = 0
fail = 0
for u in urls:
    try:
        req = urllib.request.Request(u, headers={'User-Agent': 'Mozilla/5.0'})
        resp = urllib.request.urlopen(req, timeout=10)
        body = resp.read().decode('utf-8', errors='replace')
        if '<div id="app">' in body and ('<title>' in body or '<script' in body):
            print(f'OK    {resp.status}  {u}')
            ok += 1
        else:
            print(f'WARN  {resp.status}  {u}  body_len={len(body)}')
            fail += 1
    except Exception as e:
        print(f'FAIL  {u}  {e}')
        fail += 1

print()
print(f'OK={ok}  FAIL={fail}  Total={len(urls)}')
