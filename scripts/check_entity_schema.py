#!/usr/bin/env python3
"""
V3.5.49: 验证 entity ↔ schema 100% 对齐
- 扫描后端 entity (@TableName)
- 跟 sql/v3.5.49-schema.sql 对比
- 输出 entity 缺 schema / schema 缺 entity / 字段差异
"""
import re
import os
import sys
from collections import defaultdict

ROOT = '/workspace/miniLiugl'
ENTITY_DIRS = []
for m in os.listdir(os.path.join(ROOT, 'backend')):
    if m.startswith('minimax-'):
        ent = os.path.join(ROOT, 'backend', m, 'src/main/java')
        if os.path.exists(ent):
            ENTITY_DIRS.append(ent)

SCHEMA_FILE = os.path.join(ROOT, 'sql/v3.5.49-schema.sql')

def parse_entities():
    """提取所有 entity: table_name -> {fields: {name: type}}"""
    entities = {}
    for ent_dir in ENTITY_DIRS:
        for root, dirs, files in os.walk(ent_dir):
            for f in files:
                if not f.endswith('.java'):
                    continue
                # V3.5.49: 严格匹配 /dto|/vo|/req|/resp/ 目录段 (避免 'request' 误判)
                if re.search(r'/(dto|vo|req|resp)(/|$)', root.lower()):
                    continue
                fp = os.path.join(root, f)
                try:
                    content = open(fp).read()
                except:
                    continue
                m = re.search(r'@TableName\("(\w+)"\)', content)
                if not m:
                    continue
                table = m.group(1)
                fields = {}
                for fm in re.finditer(r'private\s+([\w<>,\s\[\]\.]+?)\s+(\w+)\s*[;|=]', content):
                    ftype = fm.group(1).strip()
                    fname = fm.group(2).strip()
                    if 'static' in ftype or 'final' in ftype:
                        continue
                    if fname.startswith('this'):
                        continue
                    fields[fname] = ftype
                entities[table] = fields
    return entities

def parse_schema():
    """提取 schema: table -> [fields]"""
    with open(SCHEMA_FILE) as f:
        content = f.read()
    schema = {}
    for tm in re.finditer(r'CREATE TABLE\s+(?:IF NOT EXISTS\s+)?`?(\w+)`?\s*\((.*?)\)\s*(?:ENGINE=|;)', content, re.DOTALL):
        table = tm.group(1)
        body = tm.group(2)
        fields = []
        for line in body.split('\n'):
            line = line.strip().rstrip(',').strip()
            if not line or line.startswith('PRIMARY') or line.startswith('UNIQUE') or line.startswith('KEY') or line.startswith('INDEX') or line.startswith('CONSTRAINT') or line.startswith('--'):
                continue
            fm = re.match(r'`?(\w+)`?\s+', line)
            if fm:
                fields.append(fm.group(1))
        schema[table] = fields
    return schema

def snake_to_camel(name):
    """snake_case → camelCase (跟 MyBatis-Plus 默认行为一致)"""
    parts = name.split('_')
    return parts[0] + ''.join(p.title() for p in parts[1:])

def main():
    print("═══════════════════════════════════════════════════════════")
    print("  V3.5.49 entity ↔ schema 对齐检查")
    print("═══════════════════════════════════════════════════════════")
    print()
    entities = parse_entities()
    schema = parse_schema()
    print(f"  entity: {len(entities)} 张表")
    print(f"  schema: {len(schema)} 张表")
    print()
    e_set = set(entities.keys())
    s_set = set(schema.keys())
    print(f"  交集: {len(e_set & s_set)}")
    print(f"  entity 缺 schema: {len(e_set - s_set)}")
    for t in sorted(e_set - s_set):
        print(f"    ❌ {t}")
    print(f"  schema 缺 entity: {len(s_set - e_set)}")
    for t in sorted(s_set - e_set):
        print(f"    ❌ {t}")
    print()

    # 字段对比 (entity camelCase vs schema snake_case, 转 camelCase 比)
    field_diff = 0
    for t in e_set & s_set:
        ef = set(entities[t].keys())
        sf = set(snake_to_camel(f) for f in schema[t])
        if ef != sf:
            field_diff += 1
            print(f"  ⚠ {t}:")
            missing = ef - sf
            extra = sf - ef
            if missing:
                print(f"    entity 有 schema 缺: {sorted(missing)[:5]}")
            if extra:
                print(f"    schema 有 entity 缺: {sorted(extra)[:5]}")
    print()
    print("═══════════════════════════════════════════════════════════")
    if e_set == s_set and field_diff == 0:
        print("  ✓ 100% 对齐 (entity 表数 = schema 表数, 字段 100% 一致)")
    else:
        print(f"  ⚠ 字段差异: {field_diff} 个表")
    print("═══════════════════════════════════════════════════════════")

if __name__ == '__main__':
    main()
