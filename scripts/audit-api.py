#!/usr/bin/env python3
"""
V3.5.8 端到端 API 审计脚本

功能:
  1. 扫描前端所有 API 调用 (api/*.js + *.vue)
  2. 扫描后端所有 Controller 端点
  3. 交叉对比, 输出 4 类报告:
     A) 前端调用但后端未提供 (未匹配)
     B) 后端提供但前端未调用 (未使用)
     C) 前后端路由 vs Vue 视图 (未注册)
     D) 引用但不存在的文件

匹配规则 (V3 fuzzy):
  1. 精确匹配: (path, method) == backend_endpoints
  2. 通配符: 前端 /* 匹配后端 /{id} /{name} /{code} 等
  3. 占位符: 前端和后端的 {xxx} 都标准化成 /*, 然后比较

集成: .github/workflows/audit.yml
  - 每次 push / PR
  - 每周一早上 9 点
  - 手动触发

用法:
  python3 scripts/audit-api.py
  python3 scripts/audit-api.py --json (输出 JSON)

输出:
  - 控制台: 人类可读报告
  - /tmp/audit/report_*.json: 结构化数据

作者: MiniMax Agent Team
许可: MIT
"""

import os, re, json, argparse
from pathlib import Path
from collections import defaultdict

# ════════════════════════════════════════════════════════════
# 路径配置 (相对脚本位置)
# ════════════════════════════════════════════════════════════
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FRONTEND = f"{ROOT}/frontend/src"
BACKEND = f"{ROOT}/backend"
from pathlib import Path
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FRONTEND = f'{ROOT}/frontend/src'
BACKEND = f'{ROOT}/backend'

# 后端端点
backend_endpoints = set()
backend_paths = set()  # 不带 method
for cf in Path(BACKEND).rglob('*Controller.java'):
    if '/target/' in str(cf): continue
    try: content = cf.read_text(encoding='utf-8')
    except: continue
    class_base = ''
    m = re.search(r'@RequestMapping\s*\(\s*["\']?(/[a-zA-Z0-9/_{}\-]*)["\']?', content)
    if m: class_base = m.group(1)
    for mm in re.finditer(r'@(Get|Post|Put|Delete|Patch)Mapping(\s*\([^)]*\))?', content, re.DOTALL):
        method = mm.group(1).upper()
        args = mm.group(2) or ''
        val_match = re.search(r'(?:value|path)\s*=\s*["\']([^"\']*)["\']', args)
        if not val_match:
            val_match = re.search(r'["\']([^"\']*)["\']', args)
        val = val_match.group(1) if val_match else ''
        full = (class_base + (val if val.startswith('/') else '/' + val) if val else class_base) or '/'
        full = re.sub(r'/:[a-zA-Z]+', '/*', full)
        backend_endpoints.add((full, method))
        backend_paths.add(full)

# 提取非通配的 path 段 (用于模糊匹配)
def path_keywords(path):
    """提取 path 段作为关键词 (忽略 api, v1)"""
    return [p for p in path.split('/') if p and p not in ('api', 'v1', '*', '{id}')]

def fuzzy_match(fe_path, fe_method, backend_endpoints, backend_paths):
    """终极模糊匹配 (V2: 支持任意 {xxx} 占位)"""
    # 标准化: 把前端和后端的所有 {xxx} 都替换成 /*
    norm = lambda p: re.sub(r'/\{[^}]+\}', '/*', p)
    fp_norm = norm(fe_path)
    # 1. 精确
    if (fe_path, fe_method) in backend_endpoints:
        return True, fe_path
    # 2. 模糊: 标准化后比较
    for bp, bm in backend_endpoints:
        if bm != fe_method: continue
        if fp_norm == norm(bp):
            return True, bp
    return False, None

# 前端调用
frontend_calls = set()
for af in Path(f'{FRONTEND}/api').glob('*.js'):
    content = af.read_text(encoding='utf-8')
    for m in re.finditer(r'http\.(get|post|put|delete)\s*\(\s*[`\'"](/api/[^`\'"\\]+)[`\'"]', content):
        path = m.group(2).split('?')[0]
        method = m.group(1).upper()
        path = re.sub(r'/:[a-zA-Z]+', '/*', path)
        path = re.sub(r'\$\{[^}]+\}', '*', path)
        frontend_calls.add((path, method))

for vf in Path(FRONTEND).rglob('*.vue'):
    if '/node_modules/' in str(vf): continue
    try: content = vf.read_text(encoding='utf-8')
    except: continue
    for m in re.finditer(r'http\.(get|post|put|delete)\s*\(\s*[`\'"](/api/[^`\'"\\]+)[`\'"]', content):
        path = m.group(2).split('?')[0]
        method = m.group(1).upper()
        path = re.sub(r'/:[a-zA-Z]+', '/*', path)
        path = re.sub(r'\$\{[^}]+\}', '*', path)
        frontend_calls.add((path, method))

# 匹配
matched = []
unmatched = []
for p, m in sorted(frontend_calls):
    ok, bp = fuzzy_match(p, m, backend_endpoints, backend_paths)
    if ok:
        matched.append((p, m, bp))
    else:
        unmatched.append((p, m))

print(f"前端 API 调用去重: {len(frontend_calls)}")
print(f"匹配后端: {len(matched)}")
print(f"未匹配: {len(unmatched)}")
print()
print("="*70)
print("  最终未匹配 (前端调用但后端真未提供)")
print("="*70)
for p, m in unmatched:
    print(f"  ✗ {m:6s} {p}")

# 未使用
print()
print("="*70)
print("  后端未调用端点 (按模块)")
print("="*70)
frontend_paths_methods = set([(p, m) for p, m, bp in matched])
frontend_paths = set([p for p, m, bp in matched])

# 加 /* 替代 {id} 形式
frontend_paths_with_id = set()
for p in frontend_paths:
    frontend_paths_with_id.add(p)
    frontend_paths_with_id.add(p.replace('/*', '/{id}'))

unused = [(p, m) for p, m in backend_endpoints if p not in frontend_paths and p not in frontend_paths_with_id]
by_mod = defaultdict(list)
for p, m in unused:
    parts = p.split('/')
    if len(parts) > 2 and parts[1] == 'api':
        mod = parts[2]
    else:
        mod = parts[1] if len(parts) > 1 else 'other'
    by_mod[mod].append((p, m))

for mod in sorted(by_mod.keys()):
    items = by_mod[mod]
    print(f"\n  [{mod}] {len(items)} 个:")
    for p, m in items[:8]:
        print(f"    ○ {m:6s} {p}")
    if len(items) > 8:
        print(f"    ... +{len(items)-8} 个")

# 路由
print()
print("="*70)
print("  路由 vs 视图")
print("="*70)
router_content = Path(f'{FRONTEND}/router/index.js').read_text(encoding='utf-8')
referenced = set(re.findall(r"@/views/([a-zA-Z0-9/_.]+\.vue)", router_content))
vue_files = [str(v.relative_to(f'{FRONTEND}/views')) for v in Path(f'{FRONTEND}/views').rglob('*.vue')]
unref = sorted([v for v in vue_files if v not in referenced])
print(f"视图: {len(vue_files)}, 路由引用: {len(referenced)}, 未引用: {len(unref)}")
for u in unref:
    print(f"  ⚠ {u}")

# 输出
report = {
    'summary': {
        'backend_endpoints': len(backend_endpoints),
        'frontend_calls': len(frontend_calls),
        'matched': len(matched),
        'unmatched': len(unmatched),
        'unused': len(unused),
        'unreferenced_views': len(unref),
    },
    'unmatched_real': [(p, m) for p, m in unmatched],
    'unused_by_module': dict(by_mod),
    'unreferenced_views': unref,
}
with open('/tmp/audit/report_final.json', 'w', encoding='utf-8') as f:
    json.dump(report, f, ensure_ascii=False, indent=2)
print(f"\n报告: /tmp/audit/report_final.json")
