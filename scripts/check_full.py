#!/usr/bin/env python3
"""V6.4 完整代码检查 - API + 语法 + import + 未定义函数"""
import re
import os
import sys
import json
from pathlib import Path
from collections import defaultdict

errors = []
warnings = []
report = {'summary': {}}

# ============ 1. API 路径匹配 ============
print('=' * 60)
print('1. API 路径匹配')
print('=' * 60)

backend_paths = set()
for f in Path('backend').rglob('*Controller.java'):
    if 'target' in str(f):
        continue
    try:
        content = f.read_text(encoding='utf-8', errors='ignore')
    except:
        continue
    class_mappings = re.findall(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']*)["\']', content)
    if not class_mappings and '@RestController' in content:
        class_mappings = ['']
    method_mappings = re.findall(
        r'@(?:Request|Get|Post|Put|Delete|Patch)Mapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']*)["\']',
        content
    )
    for cm in class_mappings:
        base = '/' + cm.strip('/') if cm else ''
        for mm in method_mappings:
            full = base + '/' + mm.strip('/') if mm else base
            full = '/' + full.strip('/')
            backend_paths.add(full)

frontend_paths = {}
for f in list(Path('frontend/src').rglob('*.js')) + list(Path('frontend/src').rglob('*.vue')):
    if 'node_modules' in str(f) or 'dist' in str(f) or '__tests__' in str(f):
        continue
    try:
        content = f.read_text(encoding='utf-8', errors='ignore')
    except:
        continue
    urls = re.findall(r"url\s*:\s*[`'\"]([^`'\"]+)[`'\"]", content)
    urls += re.findall(r"url\s*\(\s*[`'\"]([^`'\"]+)[`'\"]", content)
    urls += re.findall(r"['\"](/api/v\d+/[^'\"]*)['\"]", content)
    for url in urls:
        url_clean = re.sub(r'\$\{[^}]+\}', '{id}', url)
        if not url_clean.startswith('/') or len(url_clean) < 3:
            continue
        url_clean = re.sub(r'\{[^}]+\}', '{id}', url_clean)
        if url_clean.count('{id}') > 2:
            continue
        if any(url_clean.endswith(ext) for ext in ['.png', '.jpg', '.svg', '.ico', '.css', '.js', '.html']):
            continue
        # WebSocket 路径排除
        if url_clean.startswith('/ws/'):
            continue
        frontend_paths.setdefault(url_clean, []).append(str(f.relative_to('frontend/src')))

def smart_match(fp, backend_paths):
    candidates = [fp]
    if fp.startswith('/api/'):
        candidates.append('/' + fp[5:])
    if fp.startswith('/api/v1/'):
        candidates.append('/' + fp[8:])
    for c in candidates:
        if c in backend_paths:
            return True
    fp_base = fp.rstrip('/').split('?')[0]
    for bp in backend_paths:
        bp_clean = bp.rstrip('/')
        if fp_base == bp_clean or fp_base.startswith(bp_clean + '/') or bp_clean.startswith(fp_base + '/'):
            return True
    return False

matched = sum(1 for fp in frontend_paths if smart_match(fp, backend_paths))
unmatched = [(fp, files) for fp, files in frontend_paths.items() if not smart_match(fp, backend_paths)]

print(f'  后端: {len(backend_paths)} paths')
print(f'  前端: {len(frontend_paths)} paths')
print(f'  匹配: {matched}')
print(f'  未匹配: {len(unmatched)}')
if unmatched:
    for p, files in unmatched:
        print(f'    ⚠️  {p} ← {",".join(files)}')
        warnings.append(f'前端调用 {p} 在后端无对应接口 (来自 {",".join(files)})')

report['summary']['api_paths'] = {
    'backend': len(backend_paths),
    'frontend': len(frontend_paths),
    'matched': matched,
    'unmatched': len(unmatched)
}
report['unmatched_paths'] = [{'path': p, 'files': files} for p, files in unmatched]

# ============ 2. 语法 + import ============
print()
print('=' * 60)
print('2. import 检查')
print('=' * 60)

imports_total = 0
imports_broken = []
for f in Path('frontend/src').rglob('*.js'):
    if 'node_modules' in str(f) or 'dist' in str(f) or '__tests__' in str(f):
        continue
    try:
        content = f.read_text(encoding='utf-8', errors='ignore')
    except:
        continue
    imports = re.findall(r"import\s+(?:\{([^}]+)\}|(\w+))\s+from\s+['\"]([^'\"]+)['\"]", content)
    for named, default, path in imports:
        imports_total += 1
        if not path.startswith('.'):
            continue
        # 解析相对路径
        base = f.parent
        target = (base / path).resolve()
        exists = (target.exists() or
                  Path(str(target) + '.vue').exists() or
                  Path(str(target) + '.js').exists() or
                  Path(str(target) + '/index.vue').exists() or
                  Path(str(target) + '/index.js').exists())
        if not exists:
            imports_broken.append((str(f.relative_to('frontend/src')), path))

for f in Path('frontend/src').rglob('*.vue'):
    if 'node_modules' in str(f) or 'dist' in str(f):
        continue
    try:
        content = f.read_text(encoding='utf-8', errors='ignore')
    except:
        continue
    # 提取 <script> 块
    script_match = re.search(r'<script[^>]*>(.*?)</script>', content, re.DOTALL)
    if not script_match:
        continue
    script = script_match.group(1)
    imports = re.findall(r"import\s+(?:\{([^}]+)\}|(\w+))\s+from\s+['\"]([^'\"]+)['\"]", script)
    for named, default, path in imports:
        imports_total += 1
        if not path.startswith('.'):
            continue
        base = f.parent
        target = (base / path).resolve()
        exists = (target.exists() or
                  Path(str(target) + '.vue').exists() or
                  Path(str(target) + '.js').exists() or
                  Path(str(target) + '/index.vue').exists() or
                  Path(str(target) + '/index.js').exists())
        if not exists:
            imports_broken.append((str(f.relative_to('frontend/src')), path))

print(f'  Total imports: {imports_total}')
print(f'  Broken: {len(imports_broken)}')
if imports_broken:
    for f, p in imports_broken[:20]:
        print(f'    ⚠️  {f}: import {p}')
        errors.append(f'{f} 导入 {p} 失败')

report['summary']['imports'] = {
    'total': imports_total,
    'broken': len(imports_broken)
}

# ============ 3. Canvas.vue 未定义函数检查 ============
print()
print('=' * 60)
print('3. Canvas.vue 函数定义检查')
print('=' * 60)

canvas_file = Path('frontend/src/views/agent/Canvas.vue')
content = canvas_file.read_text(encoding='utf-8', errors='ignore')

script_match = re.search(r'<script setup>(.*?)</script>', content, re.DOTALL)
script = script_match.group(1) if script_match else ''

# 提取定义
defined = set()
defined |= set(re.findall(r'function\s+(\w+)\s*\(', script))
defined |= set(re.findall(r'const\s+(\w+)\s*=\s*\(', script))
defined |= set(re.findall(r'const\s+(\w+)\s*=\s*async', script))
defined |= set(re.findall(r'const\s+(\w+)\s*=\s*ref', script))
defined |= set(re.findall(r'const\s+(\w+)\s*=\s*reactive', script))
defined |= set(re.findall(r'const\s+(\w+)\s*=\s*computed', script))

# 提取 template 调用
template = re.sub(r'<script[^>]*>.*?</script>', '', content, flags=re.DOTALL)
template_calls = set()
# @click="funcName"
template_calls |= set(re.findall(r'@[\w\-]+\s*=\s*"(\w+)\s*\(', template))
template_calls |= set(re.findall(r'@[\w\-]+\s*=\s*\'(\w+)\s*\(', template))
# {{ funcName( }}
template_calls |= set(re.findall(r'\{\{[^}]*?(\w+)\s*\(', template))
# :funcName
template_calls |= set(re.findall(r':([\w\-]+)\s*=', template))

# 排除 Vue 指令和 element 属性
exclude = {
    # HTML/SVG 属性
    'class', 'style', 'key', 'ref', 'slot', 'is', 'data', 'cx', 'cy', 'd', 'fill',
    'stroke', 'stroke-width', 'stroke-dasharray', 'width', 'height', 'viewBox', 'rx', 'ry',
    'x', 'y', 'r', 'opacity', 'gradient', 'transform', 'type', 'value', 'active',
    # Vue 指令
    'v-if', 'v-else', 'v-for', 'v-show', 'v-model', 'v-html', 'v-text', 'v-once', 'v-pre',
    # 事件
    'click', 'change', 'input', 'submit', 'mousedown', 'mousemove', 'mouseup', 'drop',
    'dragover', 'dragstart', 'focus', 'blur', 'keydown', 'keyup', 'update:modelValue',
    # Element Plus / 组件 props
    'icon', 'description', 'disabled', 'navigate', 'update', 'delete', 'cancel', 'close',
    'open', 'size', 'type', 'effect', 'color', 'plain', 'round', 'circle', 'loading',
    'min', 'max', 'step', 'rows', 'autosize', 'clearable', 'placeholder', 'multiple',
    'filterable', 'remote', 'page-size', 'current-page', 'total', 'background',
    'node-types', 'selected-edge', 'selected-node', 'viewport', 'active-name',
    'close-on-click-modal', 'close-on-press-escape', 'append-to-body', 'modal',
    # 全局 / 内置
    'stringify', 'parse', 'log', 'error', 'warn', 'info', 'debug', 'format'
}
template_calls = {c for c in template_calls if not c.startswith('el-') and not c.startswith('v-') and c not in exclude}

# Vue 内置
vue_builtins = {'ref', 'reactive', 'computed', 'onMounted', 'watch', 'nextTick', 'defineProps',
                'defineEmits', 'defineExpose', 'useRoute', 'useRouter', 'useStore'}
template_calls -= vue_builtins

undefined = template_calls - defined
if undefined:
    print('  ⚠️  Template 调用但未定义:')
    for u in sorted(undefined)[:20]:
        print(f'    {u}')
        errors.append(f'Canvas.vue 模板调用 {u} 但未定义')
else:
    print('  ✓ Canvas.vue 模板函数全部已定义')

# ============ 4. ESLint ============
print()
print('=' * 60)
print('4. ESLint 检查')
print('=' * 60)

import subprocess
# 用 stylish 输出, 更简单解析
r = subprocess.run(['node_modules/.bin/eslint', 'src/', '--no-color', '--format', 'compact'],
                   capture_output=True, text=True, cwd='frontend')
errors_count = 0
warnings_count = 0
if r.stdout:
    for line in r.stdout.split('\n'):
        if 'error' in line.lower():
            errors_count += 1
        elif 'warning' in line.lower():
            warnings_count += 1
print(f'  Errors: {errors_count}, Warnings: {warnings_count}')

# 也跑 stylish 看具体错误
r2 = subprocess.run(['node_modules/.bin/eslint', 'src/', '--no-color'],
                    capture_output=True, text=True, cwd='frontend')
if r2.stdout:
    for line in r2.stdout.split('\n'):
        if 'error' in line and 'no-unused-vars' not in line:
            print(f'    {line[:120]}')

# ============ 总结 ============
print()
print('=' * 60)
print('总结')
print('=' * 60)
print(f'  API 路径未匹配: {len(unmatched)}')
print(f'  坏 import: {len(imports_broken)}')
print(f'  Canvas 未定义: {len(undefined) if "undefined" in dir() else 0}')


Path('reports').mkdir(exist_ok=True)
with open('reports/code-check.json', 'w') as f:
    json.dump({
        'summary': report['summary'],
        'errors': errors,
        'warnings': warnings,
        'unmatched_paths': report['unmatched_paths']
    }, f, indent=2, ensure_ascii=False)

print()
print(f'报告: reports/code-check.json')
if errors:
    print(f'❌ {len(errors)} 个错误')
    sys.exit(1)
else:
    print('✓ 0 错误')
