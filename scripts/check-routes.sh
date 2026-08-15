#!/bin/bash
# V6.3+ 路由一致性检查 (V4 - 用 python 跑)
# 后端 Controller / Gateway / Nginx / 前端 API

set -e
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT="$(dirname "$SCRIPT_DIR")"
cd "$ROOT"

# 1. 用 python 跑全部提取
python3 << 'PYEOF'
import re, os, sys
from pathlib import Path

ROOT = Path('.').resolve()

# 后端 Controller 路由 (提取类级 + 方法级)
backend_routes = set()
for f in (ROOT / 'backend').rglob('*Controller.java'):
    if 'target' in str(f): continue
    text = f.read_text(encoding='utf-8', errors='ignore')
    cls = re.search(r'@RequestMapping\(["\']([^"\']+)["\']', text)
    cls_path = cls.group(1) if cls else ''
    # 找方法
    for m in re.finditer(r'@(Get|Post|Put|Delete|Patch)Mapping(?:\(([^)]*)\))?', text):
        sub = ''
        if m.group(1):
            vm = re.search(r'(?:value|path)\s*=\s*["\']([^"\']*)["\']', m.group(1))
            if vm: sub = vm.group(1)
        full = (cls_path + sub).replace('//', '/').rstrip('/')
        if full: backend_routes.add(full)

# Gateway 路由
gw_routes = set()
gwf = ROOT / 'backend/minimax-gateway/src/main/resources/application.yml'
if gwf.exists():
    text = gwf.read_text(encoding='utf-8', errors='ignore')
    for m in re.finditer(r'Path=([\w/,\*]+)', text):
        for p in m.group(1).split(','):
            gw_routes.add(p.strip().rstrip('/'))

# Nginx location
ng_routes = set()
for f in (ROOT).rglob('nginx*.conf'):
    if 'node_modules' in str(f): continue
    text = f.read_text(encoding='utf-8', errors='ignore')
    for m in re.finditer(r'location\s+[~^]*\s*[\^~]?/?([^ \{]+)', text):
        ng_routes.add(m.group(1).rstrip('/'))

# 前端 API 调用
frontend_urls = set()
for f in (ROOT / 'frontend/src/api').rglob('*.js'):
    text = f.read_text(encoding='utf-8', errors='ignore')
    for m in re.finditer(r'http\.\w+\(\s*[`\'"](/[^`\'"\\)]*)', text):
        url = m.group(1).strip().split('?')[0].split('${')[0]
        if url.startswith('/'): frontend_urls.add(url)

# 输出
print(f'📦 后端 Controller: {len(backend_routes)} 个路由')
print(f'🌐 Gateway: {len(gw_routes)} 个路由')
print(f'📡 Nginx: {len(ng_routes)} 个 location')
print(f'🎨 前端 API: {len(frontend_urls)} 个调用')
print()

# 不匹配分析
# 前端加 /api/v1 前缀
fe_with_prefix = {('/api/v1' + u).replace('//', '/').rstrip('/') for u in frontend_urls if not u.startswith('/api/v1')}

def matches(url, patterns):
    """检查 url 是否匹配任一 pattern (支持 /** 通配)"""
    for p in patterns:
        if p.endswith('/**') or p.endswith('/*'):
            base = p.rstrip('/*').rstrip('/')
            if url.startswith(base + '/') or url == base:
                return True
        elif url == p or url.startswith(p + '/'):
            return True
    return False

# 前端调但 gateway + nginx 都没的
all_paths = ng_routes | gw_routes
not_matched = []
for url in sorted(fe_with_prefix):
    if not matches(url, all_paths):
        not_matched.append(url)

print(f'❌ [前端 → 后端] 前端调但 nginx+gateway 都没: {len(not_matched)}')
print()
print('分类 (按 /api/v1/xxx/ 第一段):')
cats = {}
for url in not_matched:
    parts = url.split('/')
    cat = f'/{parts[3]}/' if len(parts) >= 5 else url
    cats.setdefault(cat, []).append(url)
for cat, urls in sorted(cats.items(), key=lambda x: -len(x[1])):
    print(f'  {cat}: {len(urls)} 个')
    for u in urls[:3]:
        print(f'    - {u}')
    if len(urls) > 3:
        print(f'    ... ({len(urls) - 3} more)')

print()
# 输出到文件给后续分析
with open('/tmp/route-audit.txt', 'w') as f:
    f.write(f'# 前端调但 nginx+gateway 都没 ({len(not_matched)} 个)\n')
    for url in not_matched:
        f.write(f'{url}\n')
print('📝 完整列表写到 /tmp/route-audit.txt')
PYEOF
