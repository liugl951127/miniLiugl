#!/usr/bin/env python3
"""
从 git HEAD 提取每个模块的特殊 minimax.* 配置
合并到重构后的 application.yml
"""
import subprocess
import re
import sys
from pathlib import Path

ROOT = Path("/workspace/minimax-platform")
modules = ['auth', 'chat', 'model', 'memory', 'rag', 'function', 'admin',
           'multimodal', 'monitor', 'agent', 'prompt', 'ws']

for m in modules:
    yml = ROOT / f"backend/minimax-{m}/src/main/resources/application.yml"
    if not yml.exists():
        print(f"  ❌ {m} (no yml)")
        continue

    # 拿 git HEAD 原始 yml
    result = subprocess.run(
        ['git', 'show', f'HEAD:backend/minimax-{m}/src/main/resources/application.yml'],
        cwd=ROOT, capture_output=True, text=True
    )
    if result.returncode != 0:
        print(f"  ❌ {m} (git show failed)")
        continue

    orig = result.stdout

    # 提取 minimax: 块
    match = re.search(r'^minimax:.*?(?=^\S|\Z)', orig, re.MULTILINE | re.DOTALL)
    if not match:
        print(f"  = {m} (no custom)")
        continue

    custom_block = match.group(0)
    # 去 "minimax:" 顶层行 + 去每行 2 空格缩进
    lines = custom_block.split('\n')
    new_lines = []
    for i, line in enumerate(lines):
        if i == 0 and line == "minimax:":
            continue
        if line.startswith('  '):
            new_lines.append(line[2:])
        else:
            new_lines.append(line)
    custom = '\n'.join(new_lines).rstrip()

    if not custom:
        print(f"  = {m} (empty custom)")
        continue

    # 读当前 yml
    current = yml.read_text()

    # 替换占位符 "# (无特殊配置)" 或追加
    placeholder_pattern = r'minimax:\n  # \(无特殊配置\)\n'
    new_minimax_block = f"minimax:\n{custom}\n"

    if re.search(placeholder_pattern, current):
        new_content = re.sub(placeholder_pattern, new_minimax_block, current, count=1)
        yml.write_text(new_content)
        print(f"  ✓ {m} (replaced placeholder, {len(custom.splitlines())} custom lines)")
    elif f"\nminimax:" in current:
        # 已有 minimax 块, 不动
        print(f"  = {m} (already has minimax block)")
    else:
        # 追加
        if not current.endswith('\n'):
            current += '\n'
        new_content = current + f"\n# 模块特殊配置 (从 git HEAD 恢复)\n{new_minimax_block}"
        yml.write_text(new_content)
        print(f"  ✓ {m} (appended, {len(custom.splitlines())} custom lines)")

print("\n=== 验证 minimax 块 ===")
for m in modules:
    yml = ROOT / f"backend/minimax-{m}/src/main/resources/application.yml"
    if yml.exists():
        lines = yml.read_text().splitlines()
        has_minimax = any(l.startswith('minimax:') for l in lines)
        print(f"  {m}: minimax block = {has_minimax}")