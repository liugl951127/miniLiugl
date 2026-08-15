#!/usr/bin/env python3
"""V6.8.1: 扫描前端未定义函数 (不 strip import)"""
import re
import json
from pathlib import Path
from collections import defaultdict

ROOT = Path('frontend/src').resolve()

GLOBALS = {
    # Builtin constructors (window.*)
    'TextDecoder', 'TextEncoder', 'DOMException', 'Uint8Array', 'Int8Array',
    'Uint16Array', 'Int16Array', 'Uint32Array', 'Int32Array', 'Float32Array',
    'Float64Array', 'BigInt64Array', 'BigUint64Array', 'ArrayBuffer', 'DataView',
    'Request', 'Response', 'Headers', 'URLSearchParams', 'URL', 'Blob', 'File',
    'FileReader', 'FormData', 'AbortController', 'AbortSignal', 'ReadableStream',
    'WritableStream', 'TransformStream', 'Worker', 'SharedWorker', 'ServiceWorker',
    'EventSource', 'WebSocket', 'Event', 'CustomEvent', 'MessageEvent',
    'MutationObserver', 'IntersectionObserver', 'ResizeObserver', 'PerformanceObserver',
    'Proxy', 'Reflect',
    # Vue 钩子
    'mounted', 'updated', 'unmounted', 'beforeMount', 'beforeUpdate', 'beforeUnmount',
    'errorCaptured', 'renderTracked', 'renderTriggered', 'activated', 'deactivated',
    # 其他
    'ref', 'reactive', 'computed', 'watch', 'watchEffect', 'onMounted', 'onUnmounted',
    'onBeforeMount', 'onBeforeUnmount', 'onActivated', 'onDeactivated', 'onErrorCaptured',
    'nextTick', 'defineComponent', 'defineProps', 'defineEmits', 'defineExpose',
    'inject', 'provide', 'getCurrentInstance', 'createApp', 'h', 'render', 'createVNode',
    'useRoute', 'useRouter', 'useStore', 'defineStore',
    'ElMessage', 'ElMessageBox', 'ElNotification',
    'dayjs', 'axios', 'echarts', 'mitt', 'NProgress', 'Cookies',
    'console', 'window', 'document', 'location', 'history', 'navigator', 'localStorage',
    'sessionStorage', 'setTimeout', 'setInterval', 'clearTimeout', 'clearInterval',
    'Promise', 'Map', 'Set', 'Array', 'Object', 'String', 'Number', 'Boolean',
    'Date', 'Math', 'JSON', 'RegExp', 'Error', 'Symbol', 'parseInt', 'parseFloat',
    'isNaN', 'isFinite', 'encodeURIComponent', 'decodeURIComponent', 'fetch',
    'globalThis', 'process', 'requestAnimationFrame', 'cancelAnimationFrame',
    'alert', 'confirm', 'prompt', 'open', 'close', 'focus', 'blur',
    'URLSearchParams', 'URL', 'Blob', 'File', 'FileReader', 'FormData', 'Headers',
    'Event', 'CustomEvent', 'EventSource', 'WebSocket', 'AbortController', 'AbortSignal',
    'ResizeObserver', 'IntersectionObserver', 'MutationObserver',
    'atob', 'btoa',
}

METHODS = {
    'log', 'error', 'warn', 'info', 'debug', 'trace', 'dir',
    'get', 'set', 'has', 'add', 'delete', 'clear', 'size', 'length',
    'push', 'pop', 'shift', 'unshift', 'slice', 'splice', 'map', 'filter',
    'reduce', 'forEach', 'find', 'findIndex', 'indexOf', 'includes', 'some', 'every',
    'sort', 'reverse', 'concat', 'join', 'split', 'replace', 'trim',
    'toLowerCase', 'toUpperCase', 'startsWith', 'endsWith', 'substring', 'substr',
    'charAt', 'charCodeAt', 'toString', 'valueOf', 'isArray', 'isObject',
    'keys', 'values', 'entries', 'assign', 'freeze', 'fromEntries', 'create',
    'then', 'catch', 'finally', 'resolve', 'reject', 'all', 'race',
    'next', 'send', 'close', 'abort', 'pause', 'resume', 'play',
    'show', 'hide', 'open', 'close', 'toggle', 'focus', 'blur',
    'emit', 'on', 'off', 'once', 'removeListener', 'removeAllListeners',
    'mount', 'unmount', 'forceUpdate',
    'success', 'warning', 'fail', 'done', 'cancel',
    'validate', 'reset', 'clear', 'submit', 'setValue', 'getValue',
    'call', 'apply', 'bind',
    'remove', 'update', 'save', 'load', 'init', 'destroy', 'release',
    'enabled', 'disabled',
}

KEYWORDS = {
    'if', 'for', 'while', 'switch', 'catch', 'function', 'return', 'typeof',
    'new', 'class', 'async', 'await', 'try', 'throw', 'delete', 'in', 'of',
    'yield', 'static', 'get', 'set', 'this', 'super', 'import', 'export',
    'from', 'as', 'default', 'const', 'let', 'var', 'void', 'null', 'undefined',
    'true', 'false', 'instanceof',
}

def strip_comments_and_strings(code):
    result = list(code)
    i = 0
    n = len(code)
    while i < n:
        c = code[i]
        if c == '/' and i+1 < n and code[i+1] == '/':
            j = code.find('\n', i)
            if j == -1: j = n
            for k in range(i, min(j, n)):
                if result[k] != '\n':
                    result[k] = ' '
            i = j
            continue
        if c == '/' and i+1 < n and code[i+1] == '*':
            j = code.find('*/', i+2)
            if j == -1: j = n
            else: j += 2
            for k in range(i, min(j, n)):
                if result[k] != '\n':
                    result[k] = ' '
            i = j
            continue
        if c in ('"', "'", '`'):
            quote = c
            j = i + 1
            while j < n and code[j] != quote:
                if code[j] == '\\':
                    j += 2
                    continue
                if quote == '`' and code[j] == '$' and j+1 < n and code[j+1] == '{':
                    j += 2
                    depth = 1
                    while j < n and depth > 0:
                        if code[j] == '{': depth += 1
                        elif code[j] == '}': depth -= 1
                        j += 1
                    continue
                j += 1
            for k in range(i, min(j+1, n)):
                if result[k] != '\n':
                    result[k] = ' '
            i = j + 1
            continue
        i += 1
    return ''.join(result)

def find_object_method_ranges(code):
    ranges = []
    # const/let/var xxx = { ... }
    for m in re.finditer(r'(?:export\s+)?(?:const|let|var)\s+(\w+)\s*=\s*\{', code):
        start = m.end()
        depth = 1
        j = start
        while j < len(code) and depth > 0:
            c = code[j]
            if c == '{':
                depth += 1
            elif c == '}':
                depth -= 1
            j += 1
        ranges.append((m.start(), j))
    # class X { ... }
    for m in re.finditer(r'class\s+(\w+)\s*(?:extends\s+\w+\s*)?\{', code):
        start = m.end()
        depth = 1
        j = start
        while j < len(code) and depth > 0:
            c = code[j]
            if c == '{':
                depth += 1
            elif c == '}':
                depth -= 1
            j += 1
        ranges.append((m.start(), j))
    return ranges

# 1. 收集 imports (在原始 content 上, 不 strip)
imports_map = defaultdict(set)
def collect_imports_raw(content):
    """直接从原始 content 找 import"""
    for m in re.finditer(r'import\s*\{([^}]+)\}\s*from\s*[\'"]([^\'"]+)[\'"]', content):
        for part in m.group(1).split(','):
            part = part.strip()
            as_m = re.match(r'(\w+)\s+as\s+(\w+)', part)
            if as_m:
                # 按 as 后
                pass  # 暂时先处理直接名
            x = re.match(r'(\w+)', part)
            if x:
                imports_map['__GLOBAL__'].add(x.group(1))
    for m in re.finditer(r'import\s+(\w+)\s+from', content):
        imports_map['__GLOBAL__'].add(m.group(1))
    for m in re.finditer(r'import\s*\*\s*as\s+(\w+)\s*from', content):
        imports_map['__GLOBAL__'].add(m.group(1))
    for m in re.finditer(r"import\s*['\"]([^'\"]+)['\"]", content):
        src = m.group(1)
        if src == 'vue':
            imports_map['__GLOBAL__'].update({'ref', 'reactive', 'computed', 'watch', 'onMounted', 'onUnmounted', 'nextTick', 'defineProps', 'defineEmits'})
        if src == 'vue-router':
            imports_map['__GLOBAL__'].update({'useRoute', 'useRouter'})
        if src == 'pinia':
            imports_map['__GLOBAL__'].add('defineStore')
        if src == 'element-plus':
            imports_map['__GLOBAL__'].update({'ElMessage', 'ElMessageBox', 'ElNotification'})

# 收集所有 import (跨文件)
for f in (ROOT.rglob('*.js')):
    rel = str(f.relative_to(ROOT))
    if 'node_modules' in rel or '__tests__' in rel:
        continue
    content = f.read_text(encoding='utf-8')
    collect_imports_raw(content)

# 2. 收集 import 名字 (按 file)
def collect_imports(rel, content):
    for m in re.finditer(r'import\s*\{([^}]+)\}\s*from\s*[\'"]([^\'"]+)[\'"]', content):
        for part in m.group(1).split(','):
            part = part.strip()
            as_m = re.match(r'(\w+)\s+as\s+(\w+)', part)
            if as_m:
                imports_map[rel].add(as_m.group(2))
            else:
                x = re.match(r'(\w+)', part)
                if x:
                    imports_map[rel].add(x.group(1))
    for m in re.finditer(r'import\s+(\w+)\s+from', content):
        imports_map[rel].add(m.group(1))
    for m in re.finditer(r'import\s*\*\s*as\s+(\w+)\s*from', content):
        imports_map[rel].add(m.group(1))
    for m in re.finditer(r"import\s*['\"]([^'\"]+)['\"]", content):
        src = m.group(1)
        if src == 'vue':
            imports_map[rel].update({'ref', 'reactive', 'computed', 'watch', 'onMounted', 'onUnmounted', 'nextTick', 'defineProps', 'defineEmits'})
        if src == 'vue-router':
            imports_map[rel].update({'useRoute', 'useRouter'})
        if src == 'pinia':
            imports_map[rel].add('defineStore')
        if src == 'element-plus':
            imports_map[rel].update({'ElMessage', 'ElMessageBox', 'ElNotification'})

for f in (ROOT.rglob('*.js')):
    rel = str(f.relative_to(ROOT))
    if 'node_modules' in rel or '__tests__' in rel:
        continue
    content = f.read_text(encoding='utf-8')
    collect_imports(rel, content)

for f in (ROOT.rglob('*.vue')):
    rel = str(f.relative_to(ROOT))
    content = f.read_text(encoding='utf-8')
    collect_imports(rel, content)

# 3. 收集 locals (用 strip)
locals = defaultdict(set)
for f in (ROOT.rglob('*.js')):
    rel = str(f.relative_to(ROOT))
    if 'node_modules' in rel or '__tests__' in rel:
        continue
    content = f.read_text(encoding='utf-8')
    code = strip_comments_and_strings(content)
    
    for m in re.finditer(r'function\s+(\w+)\s*\(', code):
        locals[rel].add(m.group(1))
    for m in re.finditer(r'(?:const|let|var)\s+(\w+)\s*=', code):
        locals[rel].add(m.group(1))
    for m in re.finditer(r'class\s+(\w+)', code):
        locals[rel].add(m.group(1))
    method_ranges = find_object_method_ranges(code)
    for start, end in method_ranges:
        body = code[start:end]
        for m in re.finditer(r'(\w+)\s*\(', body):
            locals[rel].add(m.group(1))

for f in (ROOT.rglob('*.vue')):
    rel = str(f.relative_to(ROOT))
    content = f.read_text(encoding='utf-8')
    scripts = re.findall(r'<script[^>]*>([\s\S]*?)</script>', content)
    for script in scripts:
        code = strip_comments_and_strings(script)
        for m in re.finditer(r'function\s+(\w+)\s*\(', code):
            locals[rel].add(m.group(1))
        for m in re.finditer(r'(?:const|let|var)\s+(\w+)\s*=', code):
            locals[rel].add(m.group(1))
        for m in re.finditer(r'class\s+(\w+)', code):
            locals[rel].add(m.group(1))
        method_ranges = find_object_method_ranges(code)
        for start, end in method_ranges:
            body = code[start:end]
            for m in re.finditer(r'(\w+)\s*\(', body):
                locals[rel].add(m.group(1))

# 4. 检测
errors = defaultdict(list)
def scan_calls(rel, content):
    code = strip_comments_and_strings(content)
    defined = imports_map.get(rel, set()) | locals.get(rel, set()) | imports_map.get('__GLOBAL__', set()) | GLOBALS
    seen = set()
    method_ranges = find_object_method_ranges(code)
    
    for m in re.finditer(r'\b([a-zA-Z_]\w*)\s*\(', code):
        name = m.group(1)
        if name in METHODS or name in defined or name in KEYWORDS or name in seen:
            continue
        before = code[max(0, m.start()-2):m.start()]
        if before.rstrip().endswith('.'):
            continue
        if len(name) < 3:
            continue
        if not name.isascii():
            continue
        in_method_def = False
        for s, e in method_ranges:
            if s <= m.start() < e:
                in_method_def = True
                break
        if in_method_def:
            continue
        seen.add(name)
        line_no = content[:m.start()].count('\n') + 1
        errors[rel].append(f'  L{line_no}: {name}()')

for f in (ROOT.rglob('*.js')):
    rel = str(f.relative_to(ROOT))
    if 'node_modules' in rel or '__tests__' in rel:
        continue
    content = f.read_text(encoding='utf-8')
    scan_calls(rel, content)

for f in (ROOT.rglob('*.vue')):
    rel = str(f.relative_to(ROOT))
    content = f.read_text(encoding='utf-8')
    scripts = re.findall(r'<script[^>]*>([\s\S]*?)</script>', content)
    for script in scripts:
        scan_calls(rel, script)

total = sum(len(v) for v in errors.values())
print(f'严格模式: {total} 可能未定义')

sorted_files = sorted(errors.keys(), key=lambda x: -len(errors[x]))
for f in sorted_files[:15]:
    print(f'\n[{f}] ({len(errors[f])} 个):')
    for e in errors[f][:8]:
        print(f'  {e}')

with open('reports/frontend-undefined.json', 'w') as fp:
    json.dump(dict(errors), fp, indent=2, ensure_ascii=False)
# 追加 class 范围检测
