#!/usr/bin/env python3
"""
V3.5.67+: 扫描 seed-data.sql INSERT 列名跟 entity 字段对齐

V3.5.67 bug: v3.5.58-seed.sql sys_role INSERT 写 `status` 列, 但 entity SysRole 字段
是 `enabled` (V3.5.58 schema 重生时跟 entity 改了列名, seed-data 没同步).

这个脚本扫 sql/v3.5.58-seed.sql, 把所有 INSERT INTO xxx (col1, col2, ...) 跟 entity
@TableName("xxx") 的字段名对比. entity 用 `enabled`, seed-data 必须也用 `enabled`.

退出码: 0 = 0 错位, 1 = 有错位
"""
import os
import re
import sys
from collections import defaultdict

ROOT = '/workspace/miniLiugl'
SEED_FILE = os.path.join(ROOT, 'sql/v3.5.58-seed.sql')

# 扫描所有 entity @TableName 跟字段
ENTITY_DIRS = []
for m in os.listdir(os.path.join(ROOT, 'backend')):
    if m.startswith('minimax-'):
        ent = os.path.join(ROOT, 'backend', m, 'src/main/java')
        if os.path.exists(ent):
            ENTITY_DIRS.append(ent)


def parse_entities():
    """entity_table_name -> {fields: set[str]}"""
    entities = {}
    for ent_dir in ENTITY_DIRS:
        for root, dirs, files in os.walk(ent_dir):
            for f in files:
                if not f.endswith('.java'):
                    continue
                path = os.path.join(root, f)
                content = open(path).read()
                m = re.search(r'@TableName\s*\(\s*"([^"]+)"\s*\)', content)
                if not m:
                    continue
                table = m.group(1)
                fields = set()
                # 抓 private String/Integer/Long/LocalDateTime ... fieldName;
                for fm in re.finditer(
                    r'private\s+[\w<>,\s\[\].?]+\s+(\w+)\s*;',
                    content
                ):
                    fname = fm.group(1)
                    # 排除 serialVersionUID
                    if fname == 'serialVersionUID':
                        continue
                    # 转 snake_case
                    snake = re.sub(r'([A-Z])', r'_\1', fname).lower().lstrip('_')
                    fields.add(snake)
                if fields:
                    entities[table] = fields
    return entities


def parse_seed_inserts():
    """返回 [(line_no, table, [col1, col2, ...])]"""
    inserts = []
    with open(SEED_FILE) as f:
        for i, line in enumerate(f, 1):
            # INSERT INTO table (col1, col2, ...)  (可能跨行)
            line_strip = line.strip()
            if line_strip.startswith('INSERT INTO'):
                # 找 (...)
                m = re.match(
                    r'INSERT\s+INTO\s+(\w+)\s*\(([^)]+)\)',
                    line_strip
                )
                if m:
                    table = m.group(1)
                    cols = [c.strip() for c in m.group(2).split(',')]
                    inserts.append((i, table, cols))
    return inserts


def main():
    entities = parse_entities()
    inserts = parse_seed_inserts()

    mismatches = []
    for line_no, table, cols in inserts:
        if table not in entities:
            continue  # 不是 entity 表 (比如 _sequences 视图) 跳过
        entity_fields = entities[table]
        # 检查每个 column
        for col in cols:
            if col not in entity_fields:
                # 找最近似的字段名
                similar = [f for f in entity_fields if f.startswith(col[:3]) or col.startswith(f[:3])]
                hint = f" (类似: {similar[:3]})" if similar else ""
                mismatches.append((line_no, table, col, hint))

    if mismatches:
        print(f"  发现 {len(mismatches)} 处 seed-data 列错位:")
        for line_no, table, col, hint in mismatches[:20]:
            print(f"    ✗ L{line_no}: INSERT INTO {table} (... {col} ...) {hint}")
        if len(mismatches) > 20:
            print(f"    ... 还有 {len(mismatches) - 20} 处")
        print()
        print("  修法: 改 seed-data 用 entity 真字段名")
        print("    entity @TableName(\"X\") 字段列表见对应 X.java")
        return 1

    # 也对 entity 字段不在 seed 里 提示 (不强求, 因为 seed 可以只填部分)
    print(f"  扫描 {len(inserts)} 个 INSERT (涵盖 {len(set(t for _, t, _ in inserts))} 张表), 0 错位")
    return 0


if __name__ == '__main__':
    sys.exit(main())
