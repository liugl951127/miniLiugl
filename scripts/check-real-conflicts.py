import os
import re
from collections import defaultdict

ROOT = '/workspace/miniLiugl/backend'
controllers = []
for root, dirs, files in os.walk(ROOT):
    for f in files:
        if f.endswith('Controller.java'):
            controllers.append(os.path.join(root, f))

routes = defaultdict(list)
for c in controllers:
    if 'GlobalMissingController' in c or 'MissingAiController' in c:
        continue  # 跳过兜底
    with open(c) as fp:
        content = fp.read()
    class_m = re.search(r'@RequestMapping\(\s*["\']([^"\']+)["\']', content)
    class_path = class_m.group(1) if class_m else ''
    for m in re.finditer(r'@(Get|Post|Put|Delete|Patch)Mapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']+)["\']', content):
        method = m.group(1)
        path = m.group(2)
        full = class_path + path
        full_norm = re.sub(r'\{[^}]+\}', '*', full)
        routes[(method, full_norm)].append((c.replace(ROOT + '/', ''), full))

# 找真实冲突
conflicts = []
for (method, path), locs in routes.items():
    if len(locs) > 1:
        conflicts.append((method, path, locs))

if conflicts:
    print(f'真实 Controller 跟 Controller 冲突: {len(conflicts)}')
    for m, p, locs in conflicts:
        print(f'  {m} {p}:')
        for c, full in locs:
            print(f'    {c}')
else:
    print('无真实 Controller 冲突!')
