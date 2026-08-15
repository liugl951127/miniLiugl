#!/usr/bin/env python3
"""
V6.8.1: 模拟后端 API, 验证前端 API 调用的合理性
- 启动 mock 服务器 (端口 9999)
- 用 Python requests 模拟前端调用
"""
import subprocess
import time
import json
import re
from pathlib import Path
from http.server import HTTPServer, BaseHTTPRequestHandler
import threading

# 1. 读后端 controller 提取所有 @GetMapping/@PostMapping 等
be_routes = {}
for f in (Path('backend').rglob('*.java')):
    if 'target' in str(f) or 'test' in str(f):
        continue
    if 'controller' not in str(f).lower():
        continue
    try:
        content = f.read_text(encoding='utf-8')
    except:
        continue
    
    # 找类级别 prefix
    class_prefix = ''
    cm = re.search(r'@RequestMapping\s*\(\s*["\']([^"\']+)["\']', content)
    if cm:
        class_prefix = cm.group(1)
    
    # 找方法级别
    for m in re.finditer(r'@(Get|Post|Put|Delete|Patch|Request)Mapping\s*\(([^)]*)\)', content):
        ann = m.group(1)
        args = m.group(2)
        method = 'ANY' if ann == 'Request' else ann.upper()
        
        path_m = re.search(r'["\']([^"\']+)["\']', args)
        if not path_m:
            continue
        path = path_m.group(1)
        
        full = path
        if class_prefix and not path.startswith('/api/'):
            if class_prefix.endswith('/'):
                full = class_prefix + path.lstrip('/')
            else:
                full = class_prefix + '/' + path.lstrip('/')
        if not full.startswith('/'):
            full = '/' + full
        
        # 加到路由表
        method_clean = 'GET' if method == 'ANY' else method
        be_routes[(method_clean, full)] = str(f).split('/')[-1]

# 补: @RequestMapping 不带 method (在 @RequestMapping 后跟 method)
# 简化为允许 GET/POST/...

print(f'后端路由: {len(be_routes)}')

# 2. mock 服务器
class MockHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        pass  # 静默
    
    def _send(self, code, body):
        self.send_response(code)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        self.wfile.write(json.dumps(body).encode())
    
    def _handle(self, method):
        path = self.path.split('?')[0]
        # 标准化: 数字 id 和 {xxx} 替换为 *
        path_norm = re.sub(r'/\d+', '/*', path)
        path_norm = re.sub(r'/\{[^}]+\}', '/*', path_norm)
        
        # 找路由 (用标准化 path)
        if (method, path_norm) in be_routes:
            body = {'code': 0, 'msg': 'ok', 'data': None, '_mock': True, '_route': path_norm}
            self.send_response(200)
        else:
            # 模糊匹配
            matched = False
            for (m, p), file in be_routes.items():
                if m != method:
                    continue
                if '*' not in p:
                    continue
                p_regex = re.escape(p).replace(r'\*', '[^/]+')
                if re.match(p_regex + '$', path_norm):
                    body = {'code': 0, 'msg': 'ok', 'data': None, '_mock': True, '_route': p}
                    self.send_response(200)
                    matched = True
                    break
            if not matched:
                body = {'code': 404, 'msg': 'Not Found', '_mock': True, '_path': path_norm}
                self.send_response(404)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        self.wfile.write(json.dumps(body).encode())
        return
        
        # 404
        self._send(404, {'code': 404, 'msg': 'Not Found', '_mock': True})
    
    def do_GET(self): self._handle('GET')
    def do_POST(self): self._handle('POST')
    def do_PUT(self): self._handle('PUT')
    def do_DELETE(self): self._handle('DELETE')
    def do_PATCH(self): self._handle('PATCH')

# 启动服务器
server = HTTPServer(('127.0.0.1', 9999), MockHandler)
thread = threading.Thread(target=server.serve_forever, daemon=True)
thread.start()
print('Mock server started on :9999')

# 3. 模拟前端调用
import urllib.request
import urllib.parse

with open('reports/frontend-api-clean.json') as f:
    fe_apis = json.load(f)

results = {'ok': 0, 'not_found': 0, 'mismatch': 0}
not_found_list = []

for api in fe_apis:
    method = api['method'].upper()
    if method == 'FETCH':
        method = 'GET'
    path = api['path']
    
    # 加 /api/v1
    if path.startswith('/'):
        full = '/api/v1' + path
    else:
        full = '/api/v1/' + path
    
    # 替换 * 为 1 (用于路径参数)
    full = full.replace('*', '1')
    
    # 模拟调用
    try:
        if method in ('GET', 'DELETE'):
            req = urllib.request.Request(f'http://127.0.0.1:9999{full}', method=method)
        else:
            req = urllib.request.Request(f'http://127.0.0.1:9999{full}', method=method,
                                          data=json.dumps({}).encode(),
                                          headers={'Content-Type': 'application/json'})
        
        with urllib.request.urlopen(req, timeout=2) as resp:
            body = json.loads(resp.read())
            if body.get('_mock') and 'data' in body:
                if body.get('code') == 0 or '_route' in body:
                    results['ok'] += 1
                else:
                    results['mismatch'] += 1
            else:
                results['ok'] += 1
    except urllib.error.HTTPError as e:
        if e.code == 404:
            results['not_found'] += 1
            not_found_list.append((method, full, api['files']))
        else:
            results['mismatch'] += 1
    except Exception as e:
        results['mismatch'] += 1

# # server.shutdown()

print(f'\n=== 模拟结果 ===')
print(f'成功: {results["ok"]}')
print(f'404: {results["not_found"]}')
print(f'异常: {results["mismatch"]}')

# 保存 404 列表
with open('reports/api-404.json', 'w') as f:
    json.dump([{'method': m, 'path': p, 'files': list(ff)} for m, p, ff in not_found_list], f, indent=2, ensure_ascii=False)

print(f'\n=== 404 端点 ({len(not_found_list)}) ===')
for m, p, f in not_found_list[:30]:
    files_str = ', '.join(f[:2])
    print(f'  {m:6} {p}  ({files_str})')
