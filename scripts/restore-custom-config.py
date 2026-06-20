#!/usr/bin/env python3
"""
清理重构后 yml 中的重复 minimax 块.
策略: 只保留 1 个 minimax 顶层 + 第 1 个 minimax 内容.
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
        continue
    content = yml.read_text()
    # 找所有 minimax: 行
    lines = content.split('\n')
    minimax_indices = [i for i, ln in enumerate(lines) if ln.rstrip() == 'minimax:']
    if len(minimax_indices) <= 1:
        continue
    # 只保留第一个
    keep_start = minimax_indices[0]
    # 第一个 minimax 块的结束 (下一个顶层 key 或 # 注释起始)
    keep_end = len(lines)
    for i in range(keep_start + 1, len(lines)):
        ln = lines[i]
        # 顶层 key (不以空格开头, 非空行)
        if ln and not ln.startswith(' ') and not ln.startswith('#') and not ln.startswith('\n') and ':' in ln:
            keep_end = i
            break
        # 或者 "minimax:" 重复出现
        if ln.rstrip() == 'minimax:':
            keep_end = i
            break
    # 删除后续 minimax 块
    cleaned = lines[:keep_end]
    # 删末尾多于注释
    while cleaned and cleaned[-1].strip().startswith('# (无特殊配置'):
        cleaned.pop()
    # 保证末尾有 1 个 placeholder
    cleaned.append('  # (无特殊配置)')
    cleaned.append('')
    new_content = '\n'.join(cleaned)
    if new_content != content:
        yml.write_text(new_content)
        print(f"  ✓ {m}: cleaned (minimax blocks: {len(minimax_indices)} → 1)")

print("\n=== 验证 ===")
for m in modules:
    yml = ROOT / f"backend/minimax-{m}/src/main/resources/application.yml"
    if yml.exists():
        cnt = len(re.findall(r'^minimax:', yml.read_text(), re.MULTILINE))
        print(f"  {m}: {cnt}")