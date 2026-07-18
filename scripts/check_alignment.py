#!/usr/bin/env python3
"""
三方对齐检查: Java entity vs SQL schema vs SQL seed
- 扫所有 @TableName entity, 提取字段 (camelCase → snake_case)
- 解析 sql/00-schema.sql 的 CREATE TABLE
- 解析 sql/01-seed-data.sql 的 INSERT INTO
- 输出: 实体有的字段但 schema 缺、schema 有的字段但 entity 缺、
        seed 用了 schema 没的列、seed 值数 vs 列数不匹配
"""
import re
import os
import glob
from collections import defaultdict

# ── Java → SQL 字段名转换 ─────────────────────────────────
def java_to_sql(name: str) -> str:
    # camelCase → snake_case
    s1 = re.sub(r'([A-Z]+)([A-Z][a-z])', r'\1_\2', name)
    s2 = re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', s1)
    return s2.lower()

# ── 扫 entity ─────────────────────────────────────────────
def parse_entity(path):
    """解析 Java entity 文件, 返回 (table_name, [fields])"""
    with open(path, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
    m = re.search(r'@TableName\("(\w+)"\)', content)
    if not m:
        return None, []
    table = m.group(1)
    # 找所有 private 字段 (排除 final static / lombok 生成的)
    fields = []
    # 找 @TableField 注解 (映射列名)
    for m in re.finditer(r'private\s+(\w+(?:<[^>]+>)?)\s+(\w+)\s*[;=]', content):
        java_type, name = m.group(1), m.group(2)
        # 找 @TableField 映射
        col = java_to_sql(name)
        # 找前一个 @TableField 注解
        idx = m.start()
        prefix = content[max(0, idx-200):idx]
        m2 = re.search(rf'@TableField\s*\(\s*(?:value\s*=\s*)?"(\w+)"\s*\)', prefix[::-1])
        # 简单实现: 在 prefix 找 @TableField
        m2 = re.search(r'@TableField\s*\(\s*(?:value\s*=\s*)?"(\w+)"\s*\)', prefix)
        if m2:
            col = m2.group(1)
        fields.append((name, col, java_type))
    return table, fields

# ── 扫 schema ─────────────────────────────────────────────
def parse_schema(path):
    """解析 sql/00-schema.sql, 返回 {table: [cols]}"""
    with open(path, 'r', encoding='utf-8') as f:
        sql = f.read()
    result = {}
    # 用更宽松的正则
    blocks = re.findall(
        r'CREATE TABLE IF NOT EXISTS `(\w+)`\s*\((.*?)\)\s*ENGINE=InnoDB',
        sql, re.DOTALL
    )
    for table, body in blocks:
        cols = []
        for line in body.split('\n'):
            line = line.strip().rstrip(',').strip()
            if not line or line.upper().startswith('PRIMARY KEY'):
                continue
            m = re.match(r'`(\w+)`\s+', line)
            if m:
                cols.append(m.group(1))
        result[table] = cols
    return result

# ── 扫 seed ──────────────────────────────────────────────
def parse_seed(path):
    """解析 sql/01-seed-data.sql, 返回 [(table, [cols], [values_list])]"""
    with open(path, 'r', encoding='utf-8') as f:
        sql = f.read()
    result = []
    # 找所有 INSERT INTO
    blocks = re.findall(
        r'INSERT INTO `(\w+)`\s*\(([^)]+)\)\s*VALUES\s*(.+?);',
        sql, re.DOTALL
    )
    for table, cols_str, values_str in blocks:
        cols = [c.strip().strip('`') for c in cols_str.split(',')]
        # 解析 values
        values = []
        # 简化: 按行 split, 每行一个 value tuple
        for vline in values_str.strip().split('\n'):
            vline = vline.strip().rstrip(',')
            if not vline:
                continue
            m = re.match(r'\((.+)\)$', vline)
            if m:
                # 简单 split (不考虑括号嵌套)
                parts = m.group(1).split(',')
                # 修正: 字符串里有逗号, 但 seed 都是简单值, 先这样
                # 简单点: 计数
                values.append(parts)
        result.append((table, cols, values))
    return result

# ── 主流程 ──────────────────────────────────────────────
def main():
    base = '/workspace/miniLiugl'

    # 1. 收集所有 entity
    entities = {}
    for root, dirs, files in os.walk(f'{base}/backend'):
        if 'src/test' in root or 'target' in root:
            continue
        for f in files:
            if f.endswith('.java') and 'entity' in root.lower():
                table, fields = parse_entity(os.path.join(root, f))
                if table and fields:
                    entities[table] = fields

    # 2. 解析 schema
    schema = parse_schema(f'{base}/sql/00-schema.sql')

    # 3. 解析 seed
    seed = parse_seed(f'{base}/sql/01-seed-data.sql')

    print("══════════════════════════════════════════════════════════════")
    print(f"  📊 三方对齐报告")
    print("══════════════════════════════════════════════════════════════")
    print(f"  实体 entity:        {len(entities)} 张表")
    print(f"  SQL schema:         {len(schema)} 张表")
    print(f"  SQL seed INSERT:    {len(seed)} 段")
    print()

    # 4. 对比 entity vs schema
    print("──────────────────────────────────────────────────────────────")
    print("  A. 实体字段 vs Schema 字段 (驼峰规则)")
    print("──────────────────────────────────────────────────────────────")

    entity_schema_issues = 0
    entity_only = []  # entity 有 schema 没
    schema_only = []  # schema 有 entity 没
    for table, fields in sorted(entities.items()):
        entity_cols = {col for _, col, _ in fields}
        if table not in schema:
            print(f"  ⚠ {table:30s} entity 有, schema 缺")
            entity_schema_issues += 1
            continue
        schema_cols = set(schema[table])
        only_in_entity = entity_cols - schema_cols
        only_in_schema = schema_cols - entity_cols
        if only_in_entity:
            print(f"  ⚠ {table:30s} entity 有 schema 缺: {sorted(only_in_entity)}")
            entity_schema_issues += 1
            entity_only.append((table, only_in_entity))
        if only_in_schema:
            # schema 多了字段也常见 (id, created_at, updated_at, deleted)
            extra = only_in_schema - {'id', 'create_time', 'update_time', 'created_at', 'updated_at', 'deleted', 'create_by', 'update_by', 'created_by', 'updated_by', 'tenant_id'}
            if extra:
                print(f"  ℹ {table:30s} schema 有 entity 缺: {sorted(extra)}")
                schema_only.append((table, extra))

    print(f"  → {entity_schema_issues} 处 entity↔schema 字段不匹配")
    print()

    # 5. 对比 seed vs schema
    print("──────────────────────────────────────────────────────────────")
    print("  B. Seed 段 vs Schema 字段")
    print("──────────────────────────────────────────────────────────────")

    seed_issues = 0
    for table, cols, values in seed:
        if table not in schema:
            print(f"  ⚠ {table:30s} seed 引用了不存在的表!")
            seed_issues += 1
            continue
        schema_cols = set(schema[table])
        seed_cols = set(cols)
        only_in_seed = seed_cols - schema_cols
        if only_in_seed:
            print(f"  ✗ {table:30s} seed 用了 schema 缺的列: {sorted(only_in_seed)}")
            seed_issues += 1
        # 缺列
        only_in_schema = schema_cols - seed_cols
        # 必需字段
        required = {'id', 'deleted'}
        missing_required = (only_in_schema & required) - seed_cols
        if missing_required:
            print(f"  ℹ {table:30s} seed 缺必需字段: {sorted(missing_required)}")
        # 值数 vs 列数
        if values:
            for i, v in enumerate(values):
                # 简单: 字符串 split, 不处理嵌套
                cnt = len([p for p in v if p.strip()])
                if cnt != len(cols):
                    print(f"  ✗ {table:30s} 第 {i+1} 行: 值数 {cnt} ≠ 列数 {len(cols)}")
                    seed_issues += 1
                    if i == 0:
                        print(f"     列: {cols}")
                        print(f"     值: {v[:10]}{'...' if len(v) > 10 else ''}")
                    break  # 一行就够说明

    print(f"  → {seed_issues} 处 seed 字段/值错位")
    print()

    # 6. 总结
    print("══════════════════════════════════════════════════════════════")
    print("  总结")
    print("══════════════════════════════════════════════════════════════")
    if entity_schema_issues == 0 and seed_issues == 0:
        print("  ✅ 全部对齐, 0 错")
    else:
        print(f"  ⚠ entity↔schema: {entity_schema_issues} 处")
        print(f"  ⚠ seed 错位:     {seed_issues} 处")
        print()
        print("  修复建议:")
        print("  1. 删 seed 中被 AdminDataInitializer 兜底的表")
        print("     (sys_role / sys_user / sys_user_role)")
        print("  2. h2local 16 份加 data-locations: 空 (跳过 seed)")

if __name__ == '__main__':
    main()
