#!/usr/bin/env python3
"""
V3.5.87+: 校验 <el-menu-item index="/X"> 的路径在 router 里存在

V3.5.86 bug: src/views/auth/Login.vue 跳 /admin/dashboard, 但 router admin 容器没
配 'dashboard' 子路由, 跳 fallback { path: '/:pathMatch(.*)*', redirect: '/' } → /
→ /chat, 用户看到错的页面.

这个脚本扫所有 .vue 里的 <el-menu-item index="...">, 跟 router 里的 path 对比:
  1. Menu 路径在 router 必须存在 (避免跳到 fallback)
  2. Router 里的 admin/ai 子路由 (admin 容器) 必须跟 menu 对得上 (相对路径)
  3. router.push('/X') 跟 router.push(`/${X}`) 静态字符串也扫

退出码: 0 = 0 不一致, 1 = 有不一致
"""
import os
import re
import sys
from collections import defaultdict

ROOT = '/workspace/miniLiugl'
FRONTEND_SRC = os.path.join(ROOT, 'frontend/src')
ROUTER_FILE = os.path.join(FRONTEND_SRC, 'router/index.js')


def scan_menu_paths():
    """
    扫所有 <el-menu-item index="/X"> 跟 router.push('/X') 的硬编码路径
    返回: {path: [source_file:line, ...]}
    """
    paths = defaultdict(list)

    for root, _, files in os.walk(FRONTEND_SRC):
        for f in files:
            if not f.endswith(('.vue', '.js', '.ts')):
                continue
            fpath = os.path.join(root, f)
            content = open(fpath).read()
            rel = os.path.relpath(fpath, ROOT)

            # 1. <el-menu-item index="/X">
            for m in re.finditer(r'el-menu-item\s+index="([^"]+)"', content):
                path = m.group(1)
                line = content[:m.start()].count('\n') + 1
                paths[path].append(f'{rel}:{line}  (el-menu-item)')

            # 2. router.push('/X') 硬编码 (排除 ${} 模板字符串跟 // 注释)
            for m in re.finditer(r"router\.push\(['\"`]([^'\"`]+)['\"`]\)", content):
                arg = m.group(1)
                if '${' in arg or '//' in arg or not arg.startswith('/'):
                    continue
                path = arg.split('?')[0] if '?' in arg else arg
                line = content[:m.start()].count('\n') + 1
                paths[path].append(f'{rel}:{line}  (router.push)')

            # 3. window.location.href = '/X' (排除 ${} 跟 //)
            for m in re.finditer(r"location(?:\.href)?\s*=\s*['\"`]([^'\"`]+)['\"`]", content):
                arg = m.group(1)
                if '${' in arg or '//' in arg or not arg.startswith('/'):
                    continue
                path = arg.split('?')[0] if '?' in arg else arg
                line = content[:m.start()].count('\n') + 1
                paths[path].append(f'{rel}:{line}  (location.href)')

    return paths


def scan_router_paths():
    """
    扫 router/index.js 里的 path, 包含:
      - 顶层 path: '/X'
      - 容器 children: path: 'X' (相对, 拼到容器 path 上)
      - 重定向: redirect: '/X'
    返回: {full_path}
    """
    content = open(ROUTER_FILE).read()
    paths = set()

    # 提取所有 path: 'x' 跟 path: "/x" (顶层)
    for m in re.finditer(r"path:\s*['\"]([^'\"]+)['\"]", content):
        path = m.group(1)
        if path.startswith('/'):
            paths.add(path)
        else:
            # 相对路径, 默认挂到根 (/) - 实际还可能是父容器的 path, 这里简化处理
            paths.add('/' + path)

    # 提取 redirect: '/X'
    for m in re.finditer(r"redirect:\s*['\"]([^'\"]+)['\"]", content):
        path = m.group(1)
        if path.startswith('/'):
            paths.add(path)

    return paths


def extract_admin_children():
    """
    提取 admin 容器下的 children 路径 (path: 'X' 相对)
    返回: {full_path}  完整 /admin/X
    """
    content = open(ROUTER_FILE).read()
    # 找 admin 容器开始
    m = re.search(r"path:\s*['\"]admin['\"].*?children:\s*\[", content, re.DOTALL)
    if not m:
        return set()
    start = m.end()
    # 找匹配 ] (考虑嵌套)
    depth = 1
    i = start
    while i < len(content) and depth > 0:
        if content[i] == '[':
            depth += 1
        elif content[i] == ']':
            depth -= 1
        i += 1
    admin_section = content[start:i - 1]

    children = set()
    for m2 in re.finditer(r"path:\s*['\"]([^'\"]+)['\"]", admin_section):
        children.add('/admin/' + m2.group(1))
    children.add('/admin')  # 容器本身
    return children


def main():
    print("=" * 60)
    print("  V3.5.87+ Menu 路径 vs Router 路径一致性检查")
    print("=" * 60)
    print()

    menu_paths = scan_menu_paths()
    router_paths = scan_router_paths()
    admin_children = extract_admin_children()

    print(f"  扫到 menu / router.push 路径: {len(menu_paths)} 个")
    print(f"  Router 顶层 + redirect 路径: {len(router_paths)} 个")
    print(f"  Admin 容器 children 路径:    {len(admin_children)} 个")
    print()

    # admin children 路径优先 (menu 通常跳 /admin/...)
    # router 顶层 + admin children = 全部可能的路由
    all_routes = router_paths | admin_children

    # 检查
    missing = []
    matched = []
    for path in sorted(menu_paths.keys()):
        if path in all_routes:
            matched.append(path)
        else:
            missing.append(path)

    print(f"  ✅ 匹配: {len(matched)} 个")
    for p in sorted(matched):
        print(f"    {p}")
    print()
    if missing:
        print(f"  ❌ 缺失 (router 找不到): {len(missing)} 个")
        for p in missing:
            print(f"    {p}")
            for src in menu_paths[p]:
                print(f"      ↳ {src}")
        print()
        print(f"  💡 修复: 在 src/router/index.js admin 容器 children 加 path: '{p.replace('/admin/', '')}'")
        print(f"           或 path: '{p[1:] if p.startswith('/') else p}' 顶层")
        return 1
    else:
        print("  🎉 全部 menu 路径在 router 找到!")
        return 0


if __name__ == '__main__':
    sys.exit(main())
