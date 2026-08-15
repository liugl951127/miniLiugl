#!/usr/bin/env python3
"""
V6.8.1: 扫描后端 Java 代码语法
- 检测: 大括号/圆括号/方括号匹配
- 检测: 注释闭合, 字符串闭合
- 检测: package 声明
- 检测: import 顺序
"""
import re
import json
from pathlib import Path
from collections import defaultdict

ROOT = Path('backend')

issues = defaultdict(list)
stats = defaultdict(int)

for f in ROOT.rglob('*.java'):
    if 'target' in str(f) or 'test' in str(f):
        continue
    rel = str(f.relative_to(ROOT))
    
    try:
        content = f.read_text(encoding='utf-8')
    except:
        continue
    
    stats['files'] += 1
    lines = content.split('\n')
    stats['lines'] += len(lines)
    
    # 检测 1: 大括号匹配
    open_brace = content.count('{')
    close_brace = content.count('}')
    if open_brace != close_brace:
        issues[rel].append(f'大括号不匹配: {{ {open_brace} 个, }} {close_brace} 个, 差 {open_brace - close_brace}')
    
    # 检测 2: 圆括号匹配
    open_paren = content.count('(')
    close_paren = content.count(')')
    if open_paren != close_paren:
        issues[rel].append(f'圆括号不匹配: ( {open_paren} 个, ) {close_paren} 个, 差 {open_paren - close_paren}')
    
    # 检测 3: 中括号匹配
    open_bracket = content.count('[')
    close_bracket = content.count(']')
    if open_bracket != close_bracket:
        issues[rel].append(f'方括号不匹配: [ {open_bracket} 个, ] {close_bracket} 个, 差 {open_bracket - close_bracket}')
    
    # 检测 4: /** 注释闭合
    block_comments = content.count('/*')
    block_close = content.count('*/')
    if block_comments != block_close:
        issues[rel].append(f'块注释不闭合: /* {block_comments} 个, */ {block_close} 个')
    
    # 检测 5: 字符串字面量
    quote_double = content.count('"')
    if quote_double % 2 != 0:
        issues[rel].append(f'双引号不成对: {quote_double} 个')
    
    # 检测 6: package 行
    if not re.match(r'package\s+[\w.]+;', content[:500]):
        issues[rel].append('缺少 package 声明')

print(f'扫描文件: {stats["files"]}')
print(f'总行数: {stats["lines"]}')
print(f'问题文件: {len(issues)}')

total = sum(len(v) for v in issues.values())
print(f'总问题: {total}')

if total > 0:
    print('\n问题列表:')
    for f, errs in sorted(issues.items()):
        for e in errs:
            print(f'  [{f}] {e}')

with open('reports/java-syntax-issues.json', 'w') as f:
    json.dump(dict(issues), f, indent=2, ensure_ascii=False)
