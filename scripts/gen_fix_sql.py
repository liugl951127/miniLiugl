#!/usr/bin/env python3
"""
生成 SQL 修复补丁:
- 12 张 entity 有但 SQL 缺的表 → CREATE TABLE
- 75 张表 entity 缺字段 → ALTER TABLE ADD COLUMN
"""
import re
import os
import glob

TYPE_MAP = {
    'String': 'VARCHAR(255)', 'Long': 'BIGINT', 'Integer': 'INT',
    'Boolean': 'TINYINT(1)', 'Double': 'DOUBLE', 'Float': 'FLOAT',
    'BigDecimal': 'DECIMAL(20,4)', 'LocalDateTime': 'DATETIME',
    'LocalDate': 'DATE', 'LocalTime': 'TIME', 'JSONObject': 'TEXT',
    'JSONArray': 'TEXT', 'byte[]': 'BLOB', 'Date': 'DATETIME',
}

def to_snake(name):
    s = re.sub(r'([A-Z]+)([A-Z][a-z])', r'\1_\2', name)
    s = re.sub(r'([a-z\d])([A-Z])', r'\1_\2', s)
    return s.lower()

def get_entity_field_types(path):
    with open(path, 'r') as f: c = f.read()
    pattern = re.compile(
        r'(?:@\w+(?:\([^)]*\))?\s+)*'
        r'(?:private|protected|public)\s+'
        r'(?:static\s+)?'
        r'([\w<>,\s\[\]]+?)\s+'
        r'(\w+)\s*[;=]',
        re.MULTILINE
    )
    fields = {}
    for m in pattern.finditer(c):
        java_type = m.group(1).strip().split('<')[0].strip()
        field_name = m.group(2)
        if field_name in ('serialVersionUID', 'log', 'logger'): continue
        if field_name.isupper(): continue
        fields[field_name] = java_type
    return fields

def parse_sql(path):
    with open(path, 'r') as f: c = f.read()
    c = re.sub(r'--[^\n]*\n', '\n', c)
    c = re.sub(r'/\*[\s\S]*?\*/', '', c)
    tables = {}
    for m in re.finditer(
        r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?(\w+)`?\s*\(([\s\S]*?)\)\s*(?:ENGINE|COMMENT|DEFAULT|;)",
        c, re.IGNORECASE
    ):
        table = m.group(1); body = m.group(2)
        cols = set()
        for line in body.split('\n'):
            line = line.strip().rstrip(',').strip()
            if not line: continue
            if line.upper().startswith(('PRIMARY','UNIQUE','KEY','INDEX','FOREIGN','CONSTRAINT','CHECK')): continue
            cm = re.match(r'`?(\w+)`?\s+', line)
            if cm and cm.group(1).upper() not in ('PRIMARY','UNIQUE','KEY','INDEX','CONSTRAINT'):
                cols.add(cm.group(1))
        tables[table] = cols
    return tables

def parse_entity(path):
    with open(path, 'r') as f: c = f.read()
    m = re.search(r'@TableName\s*\(\s*(?:value\s*=\s*)?"(\w+)"\s*\)', c)
    if not m:
        m2 = re.search(r'public\s+class\s+(\w+)\b', c)
        if not m2: return None
        table = to_snake(m2.group(1))
    else: table = m.group(1)
    fields = get_entity_field_types(path)
    return {'table': table, 'fields': fields, 'file': path}

# 收集 SQL
sql_tables = {}
for f in ['/workspace/miniLiugl/sql/init.sql'] + sorted(glob.glob('/workspace/miniLiugl/backend/*/src/main/resources/schema-h2.sql')):
    sql_tables.update(parse_sql(f))

# 收集 Entity
entities = []
for root, dirs, files in os.walk('/workspace/miniLiugl/backend'):
    if 'src/test' in root: continue
    for f in files:
        if f.endswith('.java') and 'entity' in root.lower():
            e = parse_entity(os.path.join(root, f))
            if e: entities.append(e)

entity_by_table = {}
for e in entities:
    entity_by_table.setdefault(e['table'], []).append(e)

# 输出
out = []
out.append("-- =============================================================")
out.append("-- MiniMax Platform V3.5.5+ 自动生成的 SQL 修复补丁")
out.append("-- 基于 SQL vs Entity 字段差异比对 (scripts/diff_sql_entity.py)")
out.append("-- 用法: mysql -uroot -proot123456 minimax_platform < fix_missing.sql")
out.append("-- 或在 docker-compose 启动后, 手动执行")
out.append("-- =============================================================")
out.append("")

# 1) 缺表
missing_tables = set(entity_by_table.keys()) - set(sql_tables.keys())
if missing_tables:
    out.append(f"-- === 1. 新建 {len(missing_tables)} 张 entity 有但 SQL 缺的表 ===")
    out.append("")
    for t in sorted(missing_tables):
        ents = entity_by_table[t]
        module = ents[0]['file'].split('minimax-')[-1].split('/')[0]
        out.append(f"-- {t} ({module})")
        out.append(f"CREATE TABLE IF NOT EXISTS `{t}` (")
        cols = []
        has_id = False
        for e in ents:
            for fname, ftype in e['fields'].items():
                if fname in ('serialVersionUID', 'log', 'logger'): continue
                if fname == 'id':
                    has_id = True
                    cols.append(f"    `{fname}` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID({fname})'")
                    continue
                sql_type = TYPE_MAP.get(ftype, 'VARCHAR(255)')
                default = ' DEFAULT 0' if sql_type in ('BIGINT', 'INT', 'TINYINT(1)', 'DOUBLE', 'FLOAT', 'DECIMAL(20,4)') else ' DEFAULT NULL'
                cols.append(f"    `{fname}` {sql_type}{default} COMMENT '{fname}({fname})'")
        if not has_id:
            cols.insert(0, "    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)'")
        out.append(',\n'.join(cols))
        out.append(f") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='{t} (auto-generated V3.5.5+)';")
        out.append("")

# 2) 缺字段
common = set(sql_tables.keys()) & set(entity_by_table.keys())
field_miss = {}
for t in sorted(common):
    sql_cols = sql_tables[t]
    entity_cols = set()
    entity_types = {}
    for e in entity_by_table[t]:
        entity_cols |= e['fields'].keys()
        entity_types.update(e['fields'])
    extra = entity_cols - sql_cols
    extra.discard('id')
    extra.discard('serialVersionUID')
    extra.discard('log')
    extra.discard('logger')
    if extra:
        field_miss[t] = {f: entity_types.get(f, 'String') for f in extra}

if field_miss:
    out.append(f"-- === 2. ALTER TABLE 添加缺失字段 ({len(field_miss)} 张表) ===")
    out.append("")
    for t in sorted(field_miss.keys()):
        ents = entity_by_table[t]
        module = ents[0]['file'].split('minimax-')[-1].split('/')[0]
        out.append(f"-- {t} ({module})")
        for fname, ftype in field_miss[t].items():
            sql_type = TYPE_MAP.get(ftype, 'VARCHAR(255)')
            default = ' DEFAULT 0' if sql_type in ('BIGINT', 'INT', 'TINYINT(1)', 'DOUBLE', 'FLOAT', 'DECIMAL(20,4)') else ' DEFAULT NULL'
            out.append(f"ALTER TABLE `{t}` ADD COLUMN `{fname}` {sql_type}{default} COMMENT '{fname}({fname})';")
        out.append("")

with open('/workspace/miniLiugl/sql/fix_missing.sql', 'w') as f:
    f.write('\n'.join(out))

# 统计
print(f"生成: 12 表 CREATE + {len(field_miss)} 表 ALTER ({sum(len(v) for v in field_miss.values())} 字段)")
