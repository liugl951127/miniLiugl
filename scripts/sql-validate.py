#!/usr/bin/env python3
"""V6.7+ SQL 详细验证 - 模拟 MariaDB 严格模式"""

import re
import sys
from pathlib import Path

def count_values(group):
    """算 group (..., ...) 内顶层值数"""
    depth = -1
    n = 0
    in_str = False
    str_char = None
    for c in group:
        if in_str:
            if c == '\\':
                continue
            if c == str_char:
                in_str = False
        elif c in ("'", '"'):
            in_str = True
            str_char = c
        elif c == '(':
            depth += 1
        elif c == ')':
            depth -= 1
        elif c == ',' and depth == 0:
            n += 1
    return n + 1

def validate(sql_path):
    with open(sql_path) as f:
        content = f.read()
    
    print('========================================')
    print('SQL 详细验证 (V6.7+ MariaDB 严格模式)')
    print('========================================')
    
    # 1. 重复列
    print('\n▶ 1. 重复列检查')
    total_dup = 0
    for m in re.finditer(r'CREATE TABLE IF NOT EXISTS `(\w+)`\s*\((.*?)\)\s*ENGINE', content, re.DOTALL):
        body = m.group(2)
        cols = re.findall(r'^\s*`(\w+)`', body, re.MULTILINE)
        seen = {}
        for c in cols:
            seen[c] = seen.get(c, 0) + 1
        dups = {k: v for k, v in seen.items() if v > 1}
        if dups:
            print(f'  ✗ {m.group(1)}: {dups}')
            total_dup += len(dups)
    print(f'  ✓ 重复列: {total_dup}')
    
    # 2. camelCase
    print('\n▶ 2. camelCase 字段检查')
    bad = 0
    for m in re.finditer(r'CREATE TABLE IF NOT EXISTS `(\w+)`\s*\((.*?)\)\s*ENGINE', content, re.DOTALL):
        body = m.group(2)
        for cm in re.finditer(r'^\s*`([a-z]+[A-Z]\w*)`', body, re.MULTILINE):
            print(f'  ✗ {m.group(1)}: {cm.group(1)}')
            bad += 1
    print(f'  ✓ camelCase: {bad}')
    
    # 3. BLOB/TEXT/JSON/GEOMETRY DEFAULT
    print('\n▶ 3. BLOB/TEXT/JSON/GEOMETRY DEFAULT')
    bad = 0
    for segment in re.findall(r'CREATE TABLE[^;]+;', content, re.DOTALL):
        for m in re.finditer(r'`(\w+)`\s+(BLOB|LONGTEXT|TEXT|JSON|GEOMETRY)[^,)]*?(DEFAULT\s+[^,\s)]+)', segment):
            print(f'  ✗ {m.group(1)} {m.group(2)} {m.group(3)}')
            bad += 1
    print(f'  ✓ 错误: {bad}')
    
    # 4. NOT NULL 无 DEFAULT
    print('\n▶ 4. NOT NULL 字段无 DEFAULT')
    bad_list = []
    for m in re.finditer(r'CREATE TABLE IF NOT EXISTS `(\w+)`\s*\((.*?)\)\s*ENGINE', content, re.DOTALL):
        body = m.group(2)
        for fm in re.finditer(r'^\s*`(\w+)`\s+(\w+(?:\([^)]*\))?)\s+NOT NULL(?!\s+DEFAULT|\s+COMMENT|\s+AUTO_INCREMENT)(?:\s*,|\s*$)', body, re.MULTILINE):
            col = fm.group(1)
            typ = fm.group(2)
            if typ.upper() in ('TEXT', 'LONGTEXT', 'JSON', 'BLOB', 'GEOMETRY', 'MEDIUMTEXT', 'TINYTEXT'):
                continue
            bad_list.append(f'{m.group(1)}.{col} ({typ})')
    print(f'  ✓ 错误: {len(bad_list)}')
    for b in bad_list[:5]:
        print(f'    {b}')
    
    # 5. INSERT 列数 vs VALUES
    print('\n▶ 5. INSERT IGNORE 列数匹配')
    mismatches = 0
    total = 0
    for m in re.finditer(r'INSERT\s+IGNORE\s+INTO\s+`(\w+)`\s*\(([^)]+)\)', content):
        total += 1
        table = m.group(1)
        cols = re.findall(r'`\w+`', m.group(2))
        n_cols = len(cols)
        after = content[m.end():]
        vm = re.match(r'\s*VALUES\s*', after)
        if not vm:
            continue
        after_vals = after[vm.end():]
        pos = 0
        while pos < len(after_vals):
            while pos < len(after_vals) and after_vals[pos].isspace():
                pos += 1
            if pos >= len(after_vals) or after_vals[pos] != '(':
                break
            depth = 0
            start = pos
            in_str = False
            str_char = None
            while pos < len(after_vals):
                c = after_vals[pos]
                if in_str:
                    if c == '\\' and pos+1 < len(after_vals):
                        pos += 2
                        continue
                    if c == str_char:
                        in_str = False
                elif c in ("'", '"'):
                    in_str = True
                    str_char = c
                elif c == '(':
                    depth += 1
                elif c == ')':
                    depth -= 1
                    if depth == 0:
                        pos += 1
                        break
                pos += 1
            group = after_vals[start:pos]
            n_values = count_values(group)
            if n_values != n_cols:
                mismatches += 1
                if mismatches <= 5:
                    print(f'  ✗ {table}: cols={n_cols} values={n_values}')
            while pos < len(after_vals) and after_vals[pos].isspace():
                pos += 1
            if pos < len(after_vals) and after_vals[pos] == ',':
                pos += 1
            else:
                break
    print(f'  ✓ {mismatches}/{total} 不匹配')
    
    # 6. INSERT 字段引用
    print('\n▶ 6. INSERT 字段引用 DDL/ALTER')
    table_cols = {}
    for m in re.finditer(r'CREATE TABLE IF NOT EXISTS `(\w+)`\s*\((.*?)\)\s*ENGINE', content, re.DOTALL):
        body = m.group(2)
        cols = re.findall(r'^\s*`(\w+)`', body, re.MULTILINE)
        table_cols[m.group(1)] = set(cols)
    for m in re.finditer(r'ALTER TABLE `(\w+)` ADD COLUMN (?:IF NOT EXISTS )?`(\w+)`', content):
        if m.group(1) in table_cols:
            table_cols[m.group(1)].add(m.group(2))
    bad = 0
    for m in re.finditer(r'INSERT\s+IGNORE\s+INTO\s+`(\w+)`\s*\(([^)]+)\)', content):
        if m.group(1) in table_cols:
            cols = re.findall(r'`(\w+)`', m.group(2))
            missing = [c for c in cols if c not in table_cols[m.group(1)]]
            if missing:
                print(f'  ✗ {m.group(1)}: {missing}')
                bad += 1
    print(f'  ✓ 字段缺失: {bad}')
    
    # 7. ALTER 加已存在字段
    print('\n▶ 7. ALTER 加已存在字段')
    alter = 0
    redundant = 0
    for m in re.finditer(r'ALTER TABLE `(\w+)` ADD COLUMN (?:IF NOT EXISTS )?`(\w+)`', content):
        alter += 1
        if m.group(1) in table_cols and m.group(2) in table_cols[m.group(1)]:
            redundant += 1
    print(f'  ✓ {redundant}/{alter} (IF NOT EXISTS 保护)')
    
    # 8. 主键
    print('\n▶ 8. 主键缺失')
    no_pk = 0
    for tbl in re.findall(r'CREATE TABLE IF NOT EXISTS `(\w+)`', content):
        ddl = re.search(rf'CREATE TABLE IF NOT EXISTS `{tbl}`\s*\((.*?)\)\s*ENGINE', content, re.DOTALL)
        if ddl and 'PRIMARY KEY' not in ddl.group(1):
            no_pk += 1
            if no_pk <= 3:
                print(f'  ⚠  {tbl}')
    print(f'  ✓ 无主键: {no_pk}')
    
    # 9. 字符集
    print('\n▶ 9. 字符集')
    utf8 = len(re.findall(r'CHARSET=utf8mb4', content))
    print(f'  ✓ utf8mb4: {utf8}')
    
    # 10. 类型级 DEFAULT 错 (MariaDB 严格模式)
    print('\n▶ 10. 类型级 DEFAULT 错 (DATETIME/DATE/TIME/TIMESTAMP/YEAR DEFAULT 0)')
    bad10 = 0
    for m in re.finditer(r'CREATE TABLE IF NOT EXISTS `(\w+)`\s*\((.*?)\)\s*ENGINE', content, re.DOTALL):
        body = m.group(2)
        for cm in re.finditer(r'^\s*`(\w+)`\s+(DATETIME|TIMESTAMP|DATE|TIME|YEAR)\b[^,)]*?DEFAULT\s+(0|\'0\'|\'0000-00-00[^\']*\')\b', body, re.MULTILINE):
            print(f'  ✗ {m.group(1)}.{cm.group(1)} {cm.group(2)} DEFAULT 0')
            bad10 += 1
    if bad10 == 0:
        print('  ✓ 0 处')
    else:
        print(f'  ✗ {bad10} 处')
    
    # 11. 兼容性检查: MySQL 不支持语法
    print('\n▶ 11. MySQL 8.0 不支持语法 (CREATE INDEX IF NOT EXISTS, DROP INDEX IF EXISTS ON tbl)')
    bad11 = 0
    # CREATE INDEX IF NOT EXISTS
    for m in re.finditer(r'CREATE\s+(?:UNIQUE\s+)?INDEX\s+IF\s+NOT\s+EXISTS', content, re.IGNORECASE):
        print(f'  ✗ pos {m.start()}: {content[m.start():m.start()+80]}')
        bad11 += 1
    # DROP INDEX IF EXISTS (注意: ALTER TABLE DROP INDEX IF EXISTS MySQL 8.0.16+ 支持)
    # 只检测 DROP INDEX IF EXISTS 不带 ALTER TABLE 前缀
    for m in re.finditer(r'(?<!ALTER TABLE )DROP\s+INDEX\s+IF\s+EXISTS', content, re.IGNORECASE):
        # 排除 ALTER TABLE ... DROP INDEX IF EXISTS
        # 向前找 ALTER TABLE (跨多行)
        ctx_start = max(0, m.start() - 200)
        ctx = content[ctx_start:m.start()]
        # 找最后一个 ALTER TABLE 到当前位置之间没 ; (说明是同一语句)
        last_alter = ctx.upper().rfind('ALTER TABLE')
        last_semi = ctx.rfind(';')
        if last_alter > last_semi:
            continue  # ALTER TABLE ... DROP INDEX IF EXISTS (同一语句)
        print(f'  ✗ pos {m.start()}: {content[m.start():m.start()+80]}')
        bad11 += 1
    if bad11 == 0:
        print('  ✓ 0 处 (用 ALTER TABLE DROP INDEX IF EXISTS 模式)')
    else:
        print(f'  ✗ {bad11} 处')
    
    # 总结
    total_errors = total_dup + bad + bad10 + bad11 + len(bad_list) + mismatches
    print('\n' + '=' * 40)
    print(f'总错误: {total_errors}')
    return 0 if total_errors == 0 else 1

if __name__ == '__main__':
    sql = sys.argv[1] if len(sys.argv) > 1 else 'sql/minimax-mysql-final.sql'
    sys.exit(validate(sql))
