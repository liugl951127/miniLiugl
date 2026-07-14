#!/usr/bin/env python3
"""
批量修复所有 mapper 的手写 SQL:
- 找 @Select("..."), @Update("...") 等注解里的 SQL 字符串
- 把 SQL 里的 camelCase 列名改成 snake_case (跟 SQL 一致)
"""
import re
import glob
import sys

def to_snake(name):
    s = re.sub(r'([A-Z]+)([A-Z][a-z])', r'\1_\2', name)
    s = re.sub(r'([a-z\d])([A-Z])', r'\1_\2', s)
    return s.lower()

def fix_sql(sql):
    """把 SQL 字面量里的 camelCase 列名转 snake_case"""
    # 找所有 camelCase 单词 (连字符: a-z + A-Z)
    def replace(m):
        word = m.group(0)
        snake = to_snake(word)
        return snake
    # 匹配连续小写后大写 (camelCase 标识符)
    return re.sub(r'\b[a-z]+(?:[A-Z][a-z]+)+\b', replace, sql)

# 找所有 mapper
files = glob.glob('backend/minimax-*/src/main/java/com/minimax/**/mapper/*.java')

total_files = 0
total_fixes = 0

for path in files:
    with open(path) as f:
        c = f.read()
    orig = c

    # 找所有 @Select("..."), @Update("...") 字符串
    # 模式: @(Select|Update|Insert|Delete) ( "sql..." )
    def repl(m):
        annotation = m.group(1)
        sql = m.group(2)
        new_sql = fix_sql(sql)
        return f'@{annotation}("{new_sql}"'

    new_c = re.sub(
        r'@(Select|Update|Insert|Delete)\(\s*"([^"]+)"',
        repl,
        c
    )

    if new_c != orig:
        total_files += 1
        # 算修复数
        diff = sum(1 for a, b in zip(orig.split(), new_c.split()) if a != b)
        total_fixes += diff
        with open(path, 'w') as f:
            f.write(new_c)
        rel = path.replace('backend/', '')
        print(f"  ✓ {rel}: {diff} 处修改")

print()
print(f"修改文件: {total_files}")
print(f"修改处数: ~{total_fixes}")
