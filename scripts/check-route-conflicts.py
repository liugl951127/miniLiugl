import os
import re
import subprocess
from collections import defaultdict

ROOT = '/workspace/miniLiugl/backend'
controllers = []
for root, dirs, files in os.walk(ROOT):
    for f in files:
        if f.endswith('Controller.java'):
            controllers.append(os.path.join(root, f))

# 用 javap 提取 @RequestMapping 信息
routes = defaultdict(list)
for c in controllers:
    # 用 grep 提取 (避免 javap 复杂)
    with open(c) as fp:
        content = fp.read()
    # 类级 @RequestMapping
    class_m = re.search(r'@RequestMapping\(\s*["\']([^"\']+)["\']', content)
    class_path = class_m.group(1) if class_m else ''
    # 方法级
    for m in re.finditer(r'@(Get|Post|Put|Delete|Patch)Mapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']+)["\']', content):
        method = m.group(1)
        path = m.group(2)
        full = class_path + path
        # 处理 value
        if '{' in full:
            full_norm = re.sub(r'\{[^}]+\}', '*', full)
        else:
            full_norm = full
        routes[(method, full_norm)].append((c, full))

# 找冲突
conflicts = []
for (method, path), locs in routes.items():
    if len(locs) > 1:
        conflicts.append((method, path, locs))

if conflicts:
    print(f'Found {len(conflicts)} route conflicts:')
    for m, p, locs in conflicts:
        print(f'  {m} {p}:')
        for c, full in locs:
            print(f'    {c}: {full}')
else:
    print('No route conflicts!')
