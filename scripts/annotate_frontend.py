#!/usr/bin/env python3
"""
前端代码注释补全脚本 (V3.5.12+)

策略:
1. 扫 backend controller, 提取 (path, method, summary) 三元组
2. 扫 frontend/src/api/*.js, 给每个文件加 JSDoc 头 + 每个 method 加内联注释
3. 扫 frontend/src/utils/, store/, router/, composables/ 加 JSDoc 头
4. 扫 .vue 文件, 给 setup script 加注释
"""
import re
import os
import json
from pathlib import Path
from collections import defaultdict

BASE = Path('/workspace/miniLiugl')
BACKEND = BASE / 'backend'
FRONTEND = BASE / 'frontend' / 'src'

# ── 1. 扫后端 controller, 提取 API 元数据 ─────────────
def extract_backend_apis():
    """提取后端所有 API (path, method, summary)"""
    apis = defaultdict(list)  # service_name -> [(path, method, summary)]
    # 已知 controller 路径映射: (service_module, file_pattern, base_path)
    SERVICE_MAP = {
        'auth':       'minimax-auth',
        'ai':         'minimax-ai',
        'admin':      'minimax-admin',
        'agent':      'minimax-agent',
        'analytics':  'minimax-analytics',
        'chat':       'minimax-chat',
        'function':   'minimax-function',
        'gateway':    'minimax-gateway',
        'memory':     'minimax-memory',
        'model':      'minimax-model',
        'monitor':    'minimax-monitor',
        'multimodal': 'minimax-multimodal',
        'pipeline':   'minimax-pipeline',
        'prompt':     'minimax-prompt',
        'rag':        'minimax-rag',
        'ws':         'minimax-ws',
    }
    for api_name, module in SERVICE_MAP.items():
        ctrl_dir = BACKEND / module / 'src' / 'main' / 'java' / f'com/minimax/{api_name}/controller'
        if not ctrl_dir.exists():
            continue
        for ctrl_file in ctrl_dir.glob('*Controller.java'):
            content = ctrl_file.read_text(errors='ignore')
            # 找 @RequestMapping 顶层
            m = re.search(r'@RequestMapping\s*\(\s*["\']([^"\']+)["\']', content)
            base = m.group(1) if m else '/'
            # 找 @PostMapping 等
            for m in re.finditer(r'@(Get|Post|Put|Delete|Patch)Mapping\s*\(\s*(?:value\s*=\s*)?"([^"]*)"\s*\)', content):
                method = m.group(1).upper()
                sub = m.group(2)
                full_path = (base + sub).replace('//', '/')
                apis[api_name].append((method, full_path))
    return apis

# ── 2. 注释工具: 给 API call 加 JSDoc ─────────────
METHOD_NAMES = {
    'POST': '创建/更新',
    'GET': '查询',
    'PUT': '替换',
    'PATCH': '部分更新',
    'DELETE': '删除',
}

def annotate_api_file(filepath, apis_for_service):
    """给 api/<service>.js 加文件头注释 + method 注释"""
    if not filepath.exists():
        return False
    content = filepath.read_text()
    lines = content.split('\n')

    # 跳过: 已有完整文件头注释的不动
    if lines and lines[0].startswith('/**') and 'V3.5.12' in lines[0]:
        return False

    # 提取 service 名
    service = filepath.stem
    if service == 'http':
        # http.js 是 axios 封装
        header = '''/**
 * @file HTTP 客户端封装 (V3.5.12+)
 *
 * 统一封装 axios, 处理:
 *  - JWT Token 自动注入 (Authorization: Bearer)
 *  - 错误统一处理 (BizException, 401 跳转登录)
 *  - 请求/响应拦截 (TraceId 透传)
 *  - 适配 Vite 代理 (开发模式 /api 直连后端)
 */
'''
        new_lines = [header] + lines
        filepath.write_text('\n'.join(new_lines))
        return True

    # 给 method 加注释
    header_lines = [
        f'/**',
        f' * @file {service} API 调用层 (V3.5.12+)',
        f' *',
    ]
    if apis_for_service:
        header_lines.append(f' * 对应后端模块: minimax-{service}')
        header_lines.append(f' * 接口数: {len(apis_for_service)}')
        header_lines.append(f' *')
        for method, path in apis_for_service[:8]:
            header_lines.append(f' *   {method:6s} {path}')
        if len(apis_for_service) > 8:
            header_lines.append(f' *   ... 共 {len(apis_for_service)} 个')
    header_lines.append(' */')
    header = '\n'.join(header_lines) + '\n'

    # 给每个 export 的 method 加 JSDoc
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        # 找 export const xxx = { 或 export function
        m = re.match(r'^(\s*)([a-zA-Z_][\w]*)\s*:\s*\(', line)
        if m and i > 0 and ('http.post' in lines[i] or 'http.get' in lines[i] or
                            'http.put' in lines[i] or 'http.delete' in lines[i] or
                            'http.patch' in lines[i]):
            indent = m.group(1)
            method_name = m.group(2)
            # 找 http method
            m2 = re.search(r'http\.(post|get|put|delete|patch)', lines[i])
            http_method = m2.group(1).upper() if m2 else 'GET'
            # 找 url
            m3 = re.search(r"['\"]([^'\"]+)['\"]", lines[i])
            url = m3.group(1) if m3 else ''
            verb = METHOD_NAMES.get(http_method, '调用')
            new_lines.append(f'{indent}/**')
            new_lines.append(f'{indent} * {method_name} - {verb} {url}')
            new_lines.append(f'{indent} * @returns {http_method} {url} 的响应 Promise')
            new_lines.append(f'{indent} */')
        new_lines.append(line)
        i += 1

    new_content = header + '\n'.join(new_lines)
    if new_content != content:
        filepath.write_text(new_content)
        return True
    return False

# ── 3. 主流程 ─────────────
def main():
    apis = extract_backend_apis()

    print("═══════════════════════════════════════════════════════════")
    print("  前端代码注释补全 (V3.5.12+)")
    print("═══════════════════════════════════════════════════════════")
    print()

    # api/ 目录
    api_dir = FRONTEND / 'api'
    if api_dir.exists():
        print("【1/3】api/ 目录")
        for f in sorted(api_dir.glob('*.js')):
            service = f.stem
            apis_for_svc = apis.get(service, [])
            ok = annotate_api_file(f, apis_for_svc)
            n = len(apis_for_svc)
            print(f"  {'✓' if ok else ' '} {service:20s} ({n} API)")

    # utils/ 目录
    print()
    print("【2/3】utils/ 目录")
    utils_dir = FRONTEND / 'utils'
    if utils_dir.exists():
        for f in sorted(utils_dir.glob('*.js')):
            content = f.read_text()
            if content.startswith('/**'):
                continue
            # 加文件头
            with open(f, 'w') as fp:
                fp.write(f'/**\n * @file {f.name} - 工具函数 (V3.5.12+)\n */\n\n' + content)
            print(f"  ✓ {f.name}")

    # store/ 目录
    print()
    print("【3/3】store/ 目录")
    store_dir = FRONTEND / 'store'
    if store_dir.exists():
        for f in sorted(store_dir.glob('*.js')):
            content = f.read_text()
            if content.startswith('/**'):
                continue
            with open(f, 'w') as fp:
                fp.write(f'/**\n * @file {f.name} - Pinia 状态管理 (V3.5.12+)\n */\n\n' + content)
            print(f"  ✓ {f.name}")

    print()
    print("✅ 注释补全完成 (api/ + utils/ + store/)")
    print("   剩余: views/ components/ composables/ directives/ 需手写注释")

if __name__ == '__main__':
    main()
