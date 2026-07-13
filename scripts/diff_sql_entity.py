#!/usr/bin/env python3
"""
SQL 脚本 vs Java 实体类字段比对 (最终版)
- 唯一 SQL 源: sql/complete.sql
- 比对 Entity (含 @TableField 显式映射) 跟 SQL 列
"""
import re
import os
from collections import defaultdict

def to_snake(name):
    s = re.sub(r'([A-Z]+)([A-Z][a-z])', r'\1_\2', name)
    s = re.sub(r'([a-z\d])([A-Z])', r'\1_\2', s)
    return s.lower()

def parse_sql(path):
    """返回 {table: {col1, col2, ...}}"""
    with open(path, 'r') as f:
        content = f.read()
    content = re.sub(r'--[^\n]*\n', '\n', content)
    content = re.sub(r'/\*[\s\S]*?\*/', '', content)
    content = re.sub(r"COMMENT\s+'[^']*'", '', content)
    tables = {}
    pattern = re.compile(
        r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?(\w+)`?\s*\(([\s\S]*?)\)\s*ENGINE",
        re.IGNORECASE
    )
    for m in pattern.finditer(content):
        table = m.group(1)
        body = m.group(2)
        cols = set()
        for line in body.split('\n'):
            line = line.strip().rstrip(',').strip()
            if not line: continue
            if line.upper().startswith(('PRIMARY','UNIQUE','KEY','INDEX','FOREIGN','CONSTRAINT','CHECK')): continue
            cm = re.match(r'`?(\w+)`?\s+', line)
            if cm and cm.group(1).upper() not in ('PRIMARY','UNIQUE','KEY','INDEX','CONSTRAINT'):
                cols.add(cm.group(1))
        if cols:
            tables[table] = cols
    return tables

def parse_entity(path):
    """返回 {table, fields: set, file}"""
    with open(path, 'r') as f:
        c = f.read()
    m = re.search(r'@TableName\s*\(\s*(?:value\s*=\s*)?"(\w+)"\s*\)', c)
    if not m:
        m2 = re.search(r'public\s+class\s+(\w+)\b', c)
        if not m2: return None
        table = to_snake(m2.group(1))
    else:
        table = m.group(1)
    fields = set()
    # 匹配字段
    pattern = re.compile(
        r'((?:@\w+(?:\([^)]*\))?\s*\n?\s*)*)'
        r'(?:private|protected|public)\s+'
        r'(?:static\s+)?'
        r'([\w<>,\s\[\]]+?)\s+'
        r'(\w+)\s*[;=]',
        re.MULTILINE
    )
    for m in pattern.finditer(c):
        annots = m.group(1)
        field_name = m.group(3)
        if field_name in ('serialVersionUID', 'log', 'logger'): continue
        if field_name.isupper(): continue
        fields.add(field_name)
    return {'table': table, 'fields': fields, 'file': path}

def main():
    sql_path = '/workspace/miniLiugl/sql/complete.sql'
    if not os.path.exists(sql_path):
        print("❌ sql/complete.sql 不存在")
        return
    sql_tables = parse_sql(sql_path)

    entities = []
    for root, dirs, files in os.walk('/workspace/miniLiugl/backend'):
        if 'src/test' in root: continue
        for f in files:
            if f.endswith('.java') and 'entity' in root.lower():
                e = parse_entity(os.path.join(root, f))
                if e and e['fields']:
                    entities.append(e)
    ent_by_table = defaultdict(list)
    for e in entities:
        ent_by_table[e['table']].append(e)

    print("=" * 80)
    print("🔍 SQL 脚本 vs Java 实体类 字段比对 (sql/complete.sql 唯一基线)")
    print("=" * 80)
    print(f"\n📄 SQL: {len(sql_tables)} 张表")
    print(f"📦 Java Entity: {len(entities)} 个文件, {sum(len(e['fields']) for e in entities)} 字段")

    # 统计
    sql_only_tables = set(sql_tables.keys()) - set(ent_by_table.keys())
    ent_only_tables = set(ent_by_table.keys()) - set(sql_tables.keys())
    common_tables = set(sql_tables.keys()) & set(ent_by_table.keys())

    print(f"\n✅ 两边共有: {len(common_tables)} 张表")
    print(f"⚠️  SQL 有但 entity 没: {len(sql_only_tables)} 张表")
    print(f"⚠️  Entity 有但 SQL 没: {len(ent_only_tables)} 张表")

    if sql_only_tables:
        print(f"\n{'─' * 80}\n⚠️  SQL 孤儿表 (有表无 entity):")
        for t in sorted(sql_only_tables):
            print(f"   • {t}  ({len(sql_tables[t])} 字段)")

    # 字段比对
    missing_in_entity = defaultdict(set)
    extra_in_entity = defaultdict(set)
    for t in sorted(common_tables):
        sql_cols = sql_tables[t]
        entity_cols = set()
        for e in ent_by_table[t]:
            entity_cols |= e['fields']
        # SQL 有 entity 没
        missing = sql_cols - entity_cols
        for e in ent_by_table[t]:
            if e['fields'] & {'deleted', 'id'}:
                # 检查 @TableLogic / @TableId 排除
                with open(e['file']) as f:
                    fc = f.read()
                if '@TableLogic' in fc: missing.discard('deleted')
                if '@TableId' in fc: missing.discard('id')
        for t2 in ('createdAt', 'updatedAt', 'createdBy', 'updatedBy'):
            missing.discard(t2)
        if missing:
            missing_in_entity[t] = missing
        # Entity 有 SQL 没
        extra = entity_cols - sql_cols
        extra.discard('serialVersionUID')
        extra.discard('log')
        extra.discard('logger')
        if extra:
            extra_in_entity[t] = extra

    if missing_in_entity:
        print(f"\n{'─' * 80}\n⚠️  SQL 有字段, entity 缺 ({len(missing_in_entity)} 张表):")
        for t in sorted(missing_in_entity.keys()):
            cols = missing_in_entity[t]
            print(f"   • {t}  缺 {len(cols)} 字段: {sorted(cols)}")

    if extra_in_entity:
        print(f"\n{'─' * 80}\n⚠️  Entity 有字段, SQL 缺 ({len(extra_in_entity)} 张表):")
        for t in sorted(extra_in_entity.keys())[:30]:
            cols = extra_in_entity[t]
            print(f"   • {t}  多 {len(cols)} 字段: {sorted(cols)[:10]}{'...' if len(cols)>10 else ''}")
        if len(extra_in_entity) > 30:
            print(f"   ... 还有 {len(extra_in_entity) - 30} 张表")

    # 总结
    total_issues = len(missing_in_entity) + len(extra_in_entity)
    print(f"\n{'=' * 80}")
    if total_issues == 0 and not sql_only_tables and not ent_only_tables:
        print("🎉 全部一致, 0 差异! SQL 100% 覆盖 Entity")
    else:
        print(f"⚠️  共 {total_issues} 处差异")
    print("=" * 80)

if __name__ == '__main__':
    main()
