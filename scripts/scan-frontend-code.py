#!/usr/bin/env python3
"""V6.8.1: 扫描前端代码 - 处理 as rename"""
import re
import json
from pathlib import Path
from collections import defaultdict

ROOT = Path('frontend/src').resolve()
errors = defaultdict(list)
warnings = defaultdict(list)

js_files = list(ROOT.rglob('*.js'))
print(f'扫描 JS 文件: {len(js_files)}')

exports_map = defaultdict(set)
imports_map = defaultdict(list)

for f in js_files:
    rel = str(f.relative_to(ROOT))
    if 'node_modules' in rel:
        continue
    content = f.read_text(encoding='utf-8')
    
    for m in re.finditer(r'export\s+(?:async\s+)?(?:function|const|let|var|class)\s+(\w+)', content):
        exports_map[rel].add(m.group(1))
    for m in re.finditer(r'export\s*\{([^}]+)\}', content):
        names = re.findall(r'\b(\w+)\b', m.group(1))
        exports_map[rel].update(names)
    for m in re.finditer(r'export\s+default\s+(?:class\s+)?(\w+)?', content):
        exports_map[rel].add('default')
    
    # import { xxx as yyy, zzz } - 关键: 抓原始名 (as 前)
    for m in re.finditer(r'import\s*\{([^}]+)\}\s*from\s*[\'"]([^\'"]+)[\'"]', content):
        import_body = m.group(1)
        source = m.group(2)
        line_no = content[:m.start()].count('\n') + 1
        # 每个 xxx as yyy
        names = []
        for part in import_body.split(','):
            part = part.strip()
            if not part:
                continue
            # 跳过 type-only (TypeScript)
            if part.startswith('type '):
                part = part[5:].strip()
            # xxx as yyy -> xxx
            as_m = re.match(r'(\w+)\s+as\s+(\w+)', part)
            if as_m:
                names.append(as_m.group(1))  # 用原名
            else:
                # 纯 xxx
                x = re.match(r'(\w+)', part)
                if x:
                    names.append(x.group(1))
        imports_map[rel].append({'source': source, 'names': names, 'line': line_no, 'f_abs': str(f)})
    
    for m in re.finditer(r'import\s+(\w+)\s+from\s*[\'"]([^\'"]+)[\'"]', content):
        name = m.group(1)
        source = m.group(2)
        line_no = content[:m.start()].count('\n') + 1
        imports_map[rel].append({'source': source, 'names': [name], 'line': line_no, 'default': True, 'f_abs': str(f)})

def resolve_import(f_abs, source):
    if source.startswith('@/'):
        candidate = ROOT / source[2:]
    elif source.startswith('~'):
        candidate = ROOT.parent / source[2:]
    elif source.startswith('.'):
        f_path = Path(f_abs)
        candidate = (f_path.parent / source).resolve()
    elif source.startswith('/'):
        candidate = ROOT / source[1:]
    else:
        return None, None
    
    for s in ['', '.js', '.vue', '.json', '/index.js', '/index.vue']:
        p = Path(str(candidate) + s)
        if p.exists():
            try:
                rel = str(p.relative_to(ROOT))
                return str(p), rel
            except:
                return str(p), None
    return None, None

EXTERNAL = {
    'vue', 'vue-router', 'pinia', 'axios', 'echarts', 'dayjs', 'element-plus',
    'lodash', 'vue-i18n', '@vueuse/core', 'pinia-plugin-persistedstate',
    'nprogress', 'js-cookie', 'mitt', 'echarts/core', 'echarts/charts',
    'echarts/components', 'echarts/renderers', 'zrender', 'uuid', 'sortablejs',
    'qrcode', 'html2canvas', 'jspdf', 'xlsx', 'file-saver',
    'vue-echarts', 'echarts-gl', '@vue/runtime-core', '@vue/shared',
    'unplugin-vue-components/resolvers', 'unplugin-auto-import/vite',
    '@vue/test-utils', 'vitest', 'happy-dom', 'jsdom', '@vue/compiler-sfc',
}

deps = set()
try:
    with open('frontend/package.json') as f:
        pkg = json.load(f)
    deps.update(pkg.get('dependencies', {}).keys())
    deps.update(pkg.get('devDependencies', {}).keys())
except:
    pass

for f, imps in imports_map.items():
    for imp in imps:
        src = imp['source']
        if src in EXTERNAL or src in deps:
            continue
        if src.split('/')[0] in deps:
            continue
        if '@' + src.split('/')[0].lstrip('@') + '/' in deps:
            continue
        
        actual, rel = resolve_import(imp['f_abs'], src)
        if not actual:
            errors[f].append(f'  L{imp["line"]}: import 路径不存在: {src}')
            continue
        
        if imp.get('default') or not rel:
            continue
        if rel in exports_map:
            src_exports = exports_map[rel]
            for name in imp['names']:
                if name not in src_exports and 'default' not in src_exports:
                    warnings[f].append(f'  L{imp["line"]}: import {{ {name} }} 未在 {rel} 导出')

total_err = sum(len(v) for v in errors.values())
total_warn = sum(len(v) for v in warnings.values())
print(f'\n=== 结果 ===')
print(f'错误: {total_err}, 警告: {total_warn}')

for f in sorted(errors.keys()):
    for e in errors[f]:
        print(f'  [ERR] [{f}] {e}')
for f in sorted(warnings.keys()):
    for w in warnings[f]:
        print(f'  [WARN] [{f}] {w}')

with open('reports/frontend-scan.json', 'w') as fp:
    json.dump({
        'errors': dict(errors),
        'warnings': dict(warnings),
        'total_err': total_err,
        'total_warn': total_warn
    }, fp, indent=2, ensure_ascii=False)
