#!/usr/bin/env python3
"""
SQL 脚本 vs Java 实体类字段比对
- 找出 SQL 有但 entity 没的字段
- 找出 entity 有但 SQL 没的字段
- 找出 @TableName 跟 SQL 表名不匹配的 entity
"""
import re
import os
import sys
import glob
from collections import defaultdict

PROJECT_ROOT = "/workspace/miniLiugl"

# ============================================================
# 1. 解析所有 SQL 脚本, 提取 (table -> set of columns)
# ============================================================
def parse_sql(path):
    """解析 SQL 文件, 返回 {table_name: {col1, col2, ...}}"""
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    # 去注释
    content = re.sub(r'--[^\n]*\n', '\n', content)
    content = re.sub(r'/\*[\s\S]*?\*/', '', content)
    
    tables = {}
    # 匹配 CREATE TABLE ... (
    pattern = re.compile(
        r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?(\w+)`?\s*\(([\s\S]*?)\)\s*(?:ENGINE|COMMENT|DEFAULT|;)",
        re.IGNORECASE
    )
    for m in pattern.finditer(content):
        table = m.group(1)
        body = m.group(2)
        cols = set()
        for line in body.split('\n'):
            line = line.strip().rstrip(',').strip()
            if not line: continue
            if line.upper().startswith(('PRIMARY', 'UNIQUE', 'KEY', 'INDEX', 'FOREIGN', 'CONSTRAINT', 'CHECK')):
                continue
            cm = re.match(r'`?(\w+)`?\s+', line)
            if cm and cm.group(1).upper() not in ('PRIMARY', 'UNIQUE', 'KEY', 'INDEX', 'CONSTRAINT'):
                cols.add(cm.group(1))
        tables[table] = cols
    return tables

# ============================================================
# 2. 解析所有 Java Entity, 提取 (@TableName -> {fields})
# ============================================================
def to_snake(name):
    """CamelCase -> snake_case"""
    s = re.sub(r'([A-Z]+)([A-Z][a-z])', r'\1_\2', name)
    s = re.sub(r'([a-z\d])([A-Z])', r'\1_\2', s)
    return s.lower()

def parse_entity(path):
    """解析 Java entity, 返回 ({table_name, {fields}, @TableLogic, class_name})"""
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    # @TableName
    m = re.search(r'@TableName\s*\(\s*(?:value\s*=\s*)?"(\w+)"\s*\)', content)
    if not m:
        # 没 @TableName, 用类名转 snake
        m2 = re.search(r'public\s+class\s+(\w+)\b', content)
        if not m2:
            return None
        table = to_snake(m2.group(1))
    else:
        table = m.group(1)
    
    fields = set()
    # 匹配 private/protected/public 字段 (忽略 static final)
    field_pattern = re.compile(
        r'(?:@\w+(?:\([^)]*\))?\s+)*'  # 可选注解
        r'(?:private|protected|public)\s+'
        r'(?:static\s+)?'
        r'([\w<>,\s\[\]]+?)\s+'
        r'(\w+)\s*[;=]',
        re.MULTILINE
    )
    for m in field_pattern.finditer(content):
        field_name = m.group(2)
        if field_name in ('serialVersionUID', 'log', 'logger'):
            continue
        # 排除大写常量 (UPPER_SNAKE 通常是常量)
        if field_name.isupper():
            continue
        fields.add(field_name)
    
    has_logic_delete = '@TableLogic' in content
    return {'table': table, 'fields': fields, 'logic_delete': has_logic_delete, 'file': path}

# ============================================================
# 3. 主流程
# ============================================================
def main():
    print("=" * 80)
    print("🔍 SQL 脚本 vs Java 实体类 字段比对")
    print("=" * 80)
    
    # --- 收集 SQL ---
    sql_files = ['/workspace/miniLiugl/sql/init.sql'] + sorted(glob.glob(
        '/workspace/miniLiugl/backend/*/src/main/resources/schema-h2.sql'))
    sql_tables = {}
    for f in sql_files:
        t = parse_sql(f)
        sql_tables.update(t)
    print(f"\n📄 SQL 脚本: {len(sql_files)} 个文件, {len(sql_tables)} 张表")
    
    # --- 收集 Entity ---
    entity_files = []
    for root, dirs, files in os.walk('/workspace/miniLiugl/backend'):
        # 跳过 test 目录
        if 'src/test' in root: continue
        for f in files:
            if f.endswith('.java') and 'entity' in root.lower():
                entity_files.append(os.path.join(root, f))
    print(f"📦 Java Entity: {len(entity_files)} 个文件")
    
    entities = []
    for ef in entity_files:
        e = parse_entity(ef)
        if e:
            entities.append(e)
    print(f"   解析成功: {len(entities)} 个实体")
    
    # 实体按表名索引
    entity_by_table = defaultdict(list)
    for e in entities:
        entity_by_table[e['table']].append(e)
    
    # --- 比对 ---
    print("\n" + "=" * 80)
    print("📊 比对结果")
    print("=" * 80)
    
    # 1) SQL 有, 但 entity 没的表
    sql_only_tables = set(sql_tables.keys()) - set(entity_by_table.keys())
    # 2) Entity 有, 但 SQL 没的表
    entity_only_tables = set(entity_by_table.keys()) - set(sql_tables.keys())
    # 3) 都有
    common_tables = set(sql_tables.keys()) & set(entity_by_table.keys())
    
    print(f"\n✅ 两边都有: {len(common_tables)} 张表")
    print(f"⚠️  SQL 有但 entity 没: {len(sql_only_tables)} 张表")
    print(f"⚠️  Entity 有但 SQL 没: {len(entity_only_tables)} 张表")
    
    # 详情
    if sql_only_tables:
        print(f"\n{'─' * 80}")
        print(f"⚠️  SQL 有但 entity 没的表 ({len(sql_only_tables)}):")
        for t in sorted(sql_only_tables):
            print(f"   • {t}  ({len(sql_tables[t])} 字段)")
    
    if entity_only_tables:
        print(f"\n{'─' * 80}")
        print(f"⚠️  Entity 有但 SQL 没的表 ({len(entity_only_tables)}):")
        for t in sorted(entity_only_tables):
            ents = entity_by_table[t]
            for e in ents:
                print(f"   • {t}  →  {e['file'].replace(PROJECT_ROOT + '/', '')}")
                print(f"     字段: {sorted(e['fields'])}")
    
    # 4) 字段比对 (仅 common tables)
    field_mismatches = []
    missing_in_entity = defaultdict(set)  # table -> sql columns not in entity
    extra_in_entity = defaultdict(set)   # table -> entity fields not in sql
    
    for t in sorted(common_tables):
        sql_cols = sql_tables[t]
        # 收集该表所有 entity 字段的并集
        entity_cols = set()
        for e in entity_by_table[t]:
            entity_cols |= e['fields']
        # 直接比较 (SQL 与 entity 都用 camelCase)
        sql_cols_normalized = {c for c in sql_cols}
        entity_cols_normalized = {c for c in entity_cols}
        
        # SQL 有 entity 没
        missing = sql_cols_normalized - entity_cols_normalized
        # 排除 deleted (如果有 @TableLogic)
        for e in entity_by_table[t]:
            if e['logic_delete'] and 'deleted' in missing:
                missing.discard('deleted')
        # 排除 id (主键 @TableId)
        if 'id' in missing:
            for e in entity_by_table[t]:
                if '@TableId' in open(e['file']).read():
                    missing.discard('id')
        # 排除 createdAt/updatedAt (FieldFill 自动填充)
        for t2 in ('createdAt', 'updatedAt', 'createdBy', 'updatedBy'):
            missing.discard(t2)
        
        if missing:
            missing_in_entity[t] = missing
        
        # Entity 有 SQL 没
        extra = entity_cols_normalized - sql_cols_normalized
        # 排除常见自动填充
        for t2 in ('serialVersionUID', 'log', 'logger'):
            extra.discard(t2)
        if extra:
            extra_in_entity[t] = extra
    
    if missing_in_entity:
        print(f"\n{'─' * 80}")
        print(f"⚠️  SQL 有字段, 但 entity 缺 ({len(missing_in_entity)} 张表):")
        for t in sorted(missing_in_entity.keys()):
            cols = missing_in_entity[t]
            print(f"   • {t}  缺 {len(cols)} 字段: {sorted(cols)}")
    
    if extra_in_entity:
        print(f"\n{'─' * 80}")
        print(f"⚠️  Entity 有字段, 但 SQL 缺 ({len(extra_in_entity)} 张表):")
        for t in sorted(extra_in_entity.keys()):
            cols = extra_in_entity[t]
            print(f"   • {t}  多 {len(cols)} 字段: {sorted(cols)}")
    
    # --- 总结 ---
    print(f"\n{'=' * 80}")
    total_issues = (len(sql_only_tables) + len(entity_only_tables) 
                    + len(missing_in_entity) + len(extra_in_entity))
    if total_issues == 0:
        print("🎉 全部一致, 0 差异!")
    else:
        print(f"⚠️  共发现 {total_issues} 处差异")
        print(f"   • SQL only:    {len(sql_only_tables)}")
        print(f"   • Entity only: {len(entity_only_tables)}")
        print(f"   • 缺字段:      {len(missing_in_entity)}")
        print(f"   • 多字段:      {len(extra_in_entity)}")
    print("=" * 80)

if __name__ == '__main__':
    main()
