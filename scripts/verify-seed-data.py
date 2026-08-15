#!/usr/bin/env python3
"""
验证 seed-data.sql 字段一致性
- 对比每个 INSERT 的字段 vs 表的字段
- 报告缺失/多余字段
"""
import re, sys, os

def get_table_columns(sql_content, table_name):
    """从 complete.sql 找指定表的字段"""
    m = re.search(
        rf'CREATE TABLE\s+IF NOT EXISTS\s+`{table_name}`\s*\(([^;]+)\)',
        sql_content, re.DOTALL
    )
    if not m:
        return None
    body = m.group(1)
    cols = re.findall(r'`(\w+)`\s+', body)
    # 排除 SQL 关键字
    return set(c for c in cols if c not in (
        'IF', 'NOT', 'EXISTS', 'TABLE', 'CREATE', 'NULL', 'DEFAULT',
        'COMMENT', 'PRIMARY', 'KEY', 'UNIQUE', 'INDEX', 'AUTO_INCREMENT'
    ))

def check_seed(complete_sql, seed_sql):
    """检查 seed 中所有 INSERT 的字段是否在表中存在"""
    issues = []
    # 找所有 INSERT INTO `xxx` (col1, col2, ...) VALUES
    for m in re.finditer(
        r'INSERT INTO `(\w+)`\s*\(([^)]+)\)\s*VALUES',
        seed_sql, re.MULTILINE
    ):
        table = m.group(1)
        cols_str = m.group(2)
        cols = set(re.findall(r'`(\w+)`', cols_str))
        
        table_cols = get_table_columns(complete_sql, table)
        if not table_cols:
            issues.append(f"❌ {table}: 表不存在")
            continue
        extra = cols - table_cols
        missing = table_cols - cols
        if extra:
            issues.append(f"❌ {table}: 多余字段 {sorted(extra)}")
        if missing:
            issues.append(f"⚠️  {table}: 缺失字段 {sorted(missing)} (可能 OK, 不必全填)")
    return issues

if __name__ == '__main__':
    with open('sql/complete.sql') as f:
        complete = f.read()
    with open('sql/seed-data.sql') as f:
        seed = f.read()
    issues = check_seed(complete, seed)
    if not issues:
        print("  ✅ 全部 INSERT 字段一致")
    else:
        print(f"  发现 {len(issues)} 个问题:")
        for i in issues:
            print(f"    {i}")
