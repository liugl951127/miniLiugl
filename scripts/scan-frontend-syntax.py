#!/usr/bin/env python3
"""
V6.8.1: 扫描前端代码 - 语法检查
- 用 node 自带的 syntax check
"""
import subprocess
import re
import json
from pathlib import Path
from collections import defaultdict

ROOT = Path('frontend/src').resolve()
errors = defaultdict(list)

js_files = list(ROOT.rglob('*.js'))
vue_files = list(ROOT.rglob('*.vue'))
all_files = js_files + vue_files

print(f'检查文件: {len(all_files)}')

# 找 node
node = None
for p in ['node', '/usr/bin/node', '/usr/local/bin/node']:
    try:
        r = subprocess.run([p, '--version'], capture_output=True, text=True)
        if r.returncode == 0:
            node = p
            print(f'用 node: {node} ({r.stdout.strip()})')
            break
    except:
        pass

if not node:
    print('⚠ 无 node, 跳过语法检查')
    exit(0)

# 检查 JS
print('\n=== JS 语法检查 ===')
js_checked = 0
for f in js_files:
    rel = str(f.relative_to(ROOT))
    if 'node_modules' in rel:
        continue
    
    # node --check
    r = subprocess.run([node, '--check', str(f)], capture_output=True, text=True, timeout=10)
    if r.returncode != 0:
        errors[rel].append(f'  语法错: {r.stderr.strip()[:200]}')
    js_checked += 1

print(f'  检查 {js_checked} JS')

# 检查 Vue 脚本块
print('\n=== Vue <script> 语法检查 ===')
vue_checked = 0
for f in vue_files:
    rel = str(f.relative_to(ROOT))
    content = f.read_text(encoding='utf-8')
    
    # 提取 <script> 块
    scripts = re.findall(r'<script[^>]*>([\s\S]*?)</script>', content)
    for idx, script in enumerate(scripts):
        if not script.strip():
            continue
        # 写到临时文件检查
        tmp = Path('/tmp/_vue_check.js')
        tmp.write_text(script)
        r = subprocess.run([node, '--check', str(tmp)], capture_output=True, text=True, timeout=10)
        if r.returncode != 0:
            errors[rel].append(f'  <script>[{idx}] 语法错: {r.stderr.strip()[:200]}')
        vue_checked += 1

print(f'  检查 {vue_checked} Vue script')

# 输出
total_err = sum(len(v) for v in errors.values())
print(f'\n=== 结果 ===')
print(f'语法错: {total_err}')

if total_err > 0:
    for f in sorted(errors.keys()):
        for e in errors[f]:
            print(f'  [{f}] {e}')

with open('reports/frontend-syntax.json', 'w') as fp:
    json.dump({
        'errors': dict(errors),
        'total_err': total_err
    }, fp, indent=2, ensure_ascii=False)
