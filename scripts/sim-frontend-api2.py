#!/usr/bin/env python3
"""
V6.8.1: 模拟后端 API (纯字典查找, 不发请求)
"""
import re
import json
from pathlib import Path

# 1. 后端路由
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
    
    class_prefix = ''
    cm = re.search(r'@RequestMapping\s*\(\s*["\']([^"\']+)["\']', content)
    if cm:
        class_prefix = cm.group(1)
    
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
        
        # 标准化
        full_norm = re.sub(r'\{[^}]+\}', '*', full)
        full_norm = re.sub(r'/\d+', '/*', full_norm)
        
        method_clean = 'GET' if method == 'ANY' else method
        be_routes[(method_clean, full_norm)] = str(f).split('/')[-1]

# 也加 ALL method (任何 method 都接受)
be_routes_any = {}
for (m, p), f in be_routes.items():
    for method in ['GET', 'POST', 'PUT', 'DELETE', 'PATCH']:
        be_routes_any[(method, p)] = f
be_routes = be_routes_any

print(f'后端路由: {len(be_routes)} (含 method 通配)')

# 2. 前端 API
with open('reports/frontend-api-clean.json') as f:
    fe_apis = json.load(f)

results = {'ok': 0, 'not_found': 0}
not_found = []

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
    
    # 替换 * 为 1
    full = full.replace('*', '1')
    
    # 标准化
    full_norm = re.sub(r'\{[^}]+\}', '*', full)
    full_norm = re.sub(r'/\d+', '/*', full_norm)
    
    # 匹配
    if (method, full_norm) in be_routes:
        results['ok'] += 1
    else:
        # 模糊匹配
        matched = False
        for (m, p), f in be_routes.items():
            if m != method or '*' not in p:
                continue
            p_regex = re.escape(p).replace(r'\*', '[^/]+')
            if re.match(p_regex + '$', full_norm):
                results['ok'] += 1
                matched = True
                break
        if not matched:
            results['not_found'] += 1
            not_found.append((method, full_norm, api['files']))

print(f'\n=== 结果 ===')
print(f'成功: {results["ok"]}')
print(f'404: {results["not_found"]}')

with open('reports/api-404-v3.json', 'w') as f:
    json.dump([{'method': m, 'path': p, 'files': list(ff)} for m, p, ff in not_found], f, indent=2, ensure_ascii=False)

print(f'\n=== 404 端点 ({len(not_found)}) ===')
for m, p, f in not_found:
    files_str = ', '.join(f[:2])
    print(f'  {m:6} {p}  ({files_str})')
