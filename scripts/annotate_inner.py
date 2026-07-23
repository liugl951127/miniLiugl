#!/usr/bin/env python3
"""
给核心 .vue 文件内部 method 加 JSDoc 注释 (V3.5.12+)

策略:
- 找 <script setup> 内的关键 method 定义
- 在 function / arrow 之前加 JSDoc
"""
import re
from pathlib import Path

BASE = Path('/workspace/miniLiugl/frontend/src')

# 关键 method 注释 (按 文件名: {method_name: comment})
METHOD_DOCS = {
    'Login.vue': {
        'submitLogin': '提交登录表单 (POST /api/v1/auth/login, 成功后保存 token + 跳转)',
        'onWxScan': '微信扫码登录回调 (轮询 /api/v1/auth/wechat/qrcode-status)',
        'validateForm': '前端表单校验 (账号非空, 密码长度)',
        'handleLoginSuccess': '登录成功后的统一处理 (持久化 + 跳转)',
    },
    'Chat.vue': {
        'sendMessage': '发送用户消息 (POST /api/v1/ai/generate 或 SSE 流式)',
        'stopGeneration': '停止当前生成 (关闭 EventSource)',
        'regenerate': '重新生成最后一条 (用同 prompt)',
        'selectModel': '切换 AI 模型 (GPT-4o / Claude / 自研)',
        'uploadFile': '上传附件 (图片/PDF, 调 /api/v1/multimodal/upload)',
        'createSession': '新建对话会话 (POST /api/v1/sessions)',
        'deleteSession': '删除会话 (DELETE /api/v1/sessions/:id)',
    },
    'Models.vue': {
        'fetchProviders': '查询模型 Provider 列表 (GET /api/v1/models/providers)',
        'fetchConfigs': '查询模型配置 (GET /api/v1/models/configs)',
        'testBattle': '发起模型对战 (POST /api/v1/models/battle)',
        'saveProvider': '保存/更新 Provider (POST /api/v1/models/providers)',
    },
    'Index.vue': {
        'toggleSidebar': '折叠/展开侧边栏 (localStorage 持久化)',
        'handleLogout': '退出登录 (清 token + 跳 /login)',
        'navigateTo': '路由跳转 (校验权限)',
    },
    'Users.vue': {
        'fetchUsers': '查询用户列表 (GET /api/v1/auth/users?page=1)',
        'createUser': '创建用户 (POST /api/v1/auth/users)',
        'updateUser': '更新用户 (PUT /api/v1/auth/users/:id)',
        'deleteUser': '删除用户 (DELETE /api/v1/auth/users/:id)',
        'assignRole': '分配角色 (POST /api/v1/auth/users/:id/roles)',
    },
}

def annotate_vue_inner(filepath: Path, method_docs: dict) -> bool:
    """给 .vue 的 <script setup> 内的 method 加 JSDoc"""
    if not filepath.exists() or not method_docs:
        return False
    content = filepath.read_text()
    new_content = content
    n_added = 0
    for method_name, doc in method_docs.items():
        # 找: const methodName = ( 或 function methodName( 或 methodName(
        patterns = [
            rf'(\n\s*)(const\s+{method_name}\s*=\s*async\s*\()',
            rf'(\n\s*)(const\s+{method_name}\s*=\s*\()',
            rf'(\n\s*)(async\s+function\s+{method_name}\s*\()',
            rf'(\n\s*)(function\s+{method_name}\s*\()',
        ]
        for pat in patterns:
            m = re.search(pat, new_content)
            if m:
                # 检查是否已经有 JSDoc
                line_start = m.start()
                # 找前 5 行看有没有 /** 注释
                prefix = new_content[max(0, line_start-300):line_start]
                if '*/' in prefix[-100:]:  # 已经有 JSDoc
                    continue
                indent = m.group(1)
                # 替换
                old_line = m.group(0)
                new_lines = (
                    f'{indent}/**\n'
                    f'{indent} * {doc}\n'
                    f'{indent} */\n'
                    f'{old_line.lstrip()}'
                )
                new_content = new_content[:line_start] + new_lines + new_content[m.end():]
                n_added += 1
                break
    if n_added > 0:
        filepath.write_text(new_content)
        return True
    return False

def main():
    print("═══════════════════════════════════════════════════════════")
    print("  核心 .vue 内部 method 注释 (V3.5.12+)")
    print("═══════════════════════════════════════════════════════════")
    print()
    total = 0
    for filename, method_docs in METHOD_DOCS.items():
        # 找文件
        candidates = list(BASE.rglob(filename))
        if not candidates:
            # 尝试子目录
            for c in (BASE / 'views').rglob(filename):
                candidates.append(c)
        if not candidates:
            print(f"  ✗ {filename}: 未找到")
            continue
        for f in candidates:
            n_added = sum(1 for m in method_docs if f" * {m}" in f.read_text())
            ok = annotate_vue_inner(f, method_docs)
            print(f"  {'✓' if ok else ' '} {f.relative_to(BASE)}")
            if ok:
                total += 1
    print()
    print(f"✅ {total} 个核心 .vue 内部 method 注释补全")

if __name__ == '__main__':
    main()
