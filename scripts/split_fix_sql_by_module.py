#!/usr/bin/env python3
"""
按服务模块拆分 fix_missing.sql
- 每个服务一个文件 (auth.sql, ai.sql, chat.sql, ...)
- 外加 all.sql 汇总
- 直接可 mysql < xxx.sql 应用
"""
import re
import os
import glob
from collections import defaultdict

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

# 按模块分组
module_fix = defaultdict(list)  # module -> list of ALTER statements
for e in entities:
    # 从 file 路径提取模块名 minimax-X
    m = re.search(r'minimax-(\w+)/', e['file'])
    module = m.group(1) if m else 'common'
    # 找该 entity 对应表
    sql_cols = sql_tables.get(e['table'], set())
    entity_cols = set(e['fields'].keys())
    extra = entity_cols - sql_cols
    extra.discard('id')
    extra.discard('serialVersionUID')
    extra.discard('log')
    extra.discard('logger')
    if extra:
        module_fix[module].append((e['table'], e['fields'], extra))

# 创建输出目录
out_dir = '/workspace/miniLiugl/sql/fix_by_module'
os.makedirs(out_dir, exist_ok=True)

# 为每个模块生成独立文件
for module in sorted(module_fix.keys()):
    items = module_fix[module]
    out_path = os.path.join(out_dir, f'fix_{module}.sql')
    with open(out_path, 'w') as f:
        f.write(f"-- =============================================================\n")
        f.write(f"-- MiniMax Platform V3.5.5+ 修复 SQL - {module} 模块\n")
        f.write(f"-- 共 {len(items)} 张表, {sum(len(x[2]) for x in items)} 字段\n")
        f.write(f"-- 用法: mysql -uroot -proot123456 minimax_platform < {os.path.basename(out_path)}\n")
        f.write(f"-- 自动生成: scripts/split_fix_sql_by_module.py\n")
        f.write(f"-- =============================================================\n\n")
        for table, fields, missing in items:
            f.write(f"-- 表: {table}\n")
            for fname in sorted(missing):
                ftype = fields.get(fname, 'String')
                sql_type = TYPE_MAP.get(ftype, 'VARCHAR(255)')
                default = ' DEFAULT 0' if sql_type in ('BIGINT','INT','TINYINT(1)','DOUBLE','FLOAT','DECIMAL(20,4)') else ' DEFAULT NULL'
                f.write(f"ALTER TABLE `{table}` ADD COLUMN `{fname}` {sql_type}{default} COMMENT '{fname}({fname})';\n")
            f.write("\n")
    print(f"✓ {module}: {len(items)} 张表, {sum(len(x[2]) for x in items)} 字段 → {out_path}")

# 生成 all.sql (汇总, 按模块顺序)
all_path = os.path.join(out_dir, 'fix_all.sql')
with open(all_path, 'w') as f:
    f.write(f"-- =============================================================\n")
    f.write(f"-- MiniMax Platform V3.5.5+ 修复 SQL - 全部模块汇总\n")
    f.write(f"-- 共 {len(module_fix)} 模块, {sum(len(v) for v in module_fix.values())} 张表, "
            f"{sum(sum(len(x[2]) for x in v) for v in module_fix.values())} 字段\n")
    f.write(f"-- 用法: mysql -uroot -proot123456 minimax_platform < fix_all.sql\n")
    f.write(f"-- 自动生成: scripts/split_fix_sql_by_module.py\n")
    f.write(f"-- =============================================================\n\n")
    for module in sorted(module_fix.keys()):
        items = module_fix[module]
        f.write(f"\n-- ========== {module} 模块 ({len(items)} 张表) ==========\n\n")
        for table, fields, missing in items:
            f.write(f"-- 表: {table}\n")
            for fname in sorted(missing):
                ftype = fields.get(fname, 'String')
                sql_type = TYPE_MAP.get(ftype, 'VARCHAR(255)')
                default = ' DEFAULT 0' if sql_type in ('BIGINT','INT','TINYINT(1)','DOUBLE','FLOAT','DECIMAL(20,4)') else ' DEFAULT NULL'
                f.write(f"ALTER TABLE `{table}` ADD COLUMN `{fname}` {sql_type}{default} COMMENT '{fname}({fname})';\n")
            f.write("\n")

print(f"\n✓ 汇总: {all_path}")

# 生成 README
readme = os.path.join(out_dir, 'README.md')
with open(readme, 'w') as f:
    f.write(f"# 修复 SQL 文件夹 (按模块拆分)\n\n")
    f.write(f"## 文件清单\n\n")
    f.write(f"| 文件 | 模块 | 表数 | 字段数 |\n")
    f.write(f"|------|------|------|--------|\n")
    f.write(f"| fix_all.sql | 全部 | {sum(len(v) for v in module_fix.values())} | {sum(sum(len(x[2]) for x in v) for v in module_fix.values())} |\n")
    for module in sorted(module_fix.keys()):
        items = module_fix[module]
        n_tables = len(items)
        n_cols = sum(len(x[2]) for x in items)
        f.write(f"| fix_{module}.sql | {module} | {n_tables} | {n_cols} |\n")
    f.write(f"\n## 用法\n\n")
    f.write(f"### 1. 全部模块 (推荐首次部署)\n\n")
    f.write(f"```bash\n")
    f.write(f"docker exec -i minimax-mariadb mysql -uroot -proot123456 minimax_platform < fix_all.sql\n")
    f.write(f"```\n\n")
    f.write(f"### 2. 单个模块 (按需)\n\n")
    f.write(f"```bash\n")
    f.write(f"docker exec -i minimax-mariadb mysql -uroot -proot123456 minimax_platform < fix_ai.sql\n")
    f.write(f"```\n\n")
    f.write(f"### 3. 精简版 (只 apply 精简部署用到的模块)\n\n")
    f.write(f"```bash\n")
    f.write(f"docker exec -i minimax-mariadb mysql -uroot -proot123456 minimax_platform < fix_auth.sql\n")
    f.write(f"docker exec -i minimax-mariadb mysql -uroot -proot123456 minimax_platform < fix_ai.sql\n")
    f.write(f"```\n\n")
    f.write(f"### 4. 完整 21 容器版本 (全部模块)\n\n")
    f.write(f"```bash\n")
    f.write(f"for f in fix_*.sql; do\n")
    f.write(f"  docker exec -i minimax-mariadb mysql -uroot -proot123456 minimax_platform < $f\n")
    f.write(f"done\n")
    f.write(f"```\n\n")
    f.write(f"## 模块对应表\n\n")
    f.write(f"| 模块 | Java 包 | 端口 | 说明 |\n")
    f.write(f"|------|---------|------|------|\n")
    f.write(f"| auth | minimax-auth | 8081 | 鉴权 |\n")
    f.write(f"| ai | minimax-ai | 8094 | AI 核心 |\n")
    f.write(f"| chat | minimax-chat | 8082 | 聊天 |\n")
    f.write(f"| memory | minimax-memory | 8083 | 记忆 |\n")
    f.write(f"| model | minimax-model | 8084 | 模型管理 |\n")
    f.write(f"| rag | minimax-rag | 8085 | 知识库 |\n")
    f.write(f"| function | minimax-function | 8086 | 函数工具 |\n")
    f.write(f"| multimodal | minimax-multimodal | 8087 | 多模态 |\n")
    f.write(f"| agent | minimax-agent | 8088 | 智能体 |\n")
    f.write(f"| monitor | minimax-monitor | 8089 | 监控 |\n")
    f.write(f"| admin | minimax-admin | 8090 | 管理后台 |\n")
    f.write(f"| prompt | minimax-prompt | 8091 | 提示词 |\n")
    f.write(f"| analytics | minimax-analytics | 8092 | 数据分析 |\n")
    f.write(f"| pipeline | minimax-pipeline | 8093 | 工作流 |\n")
    f.write(f"| gateway | minimax-gateway | 7080 | API 网关 |\n")
    f.write(f"| common | minimax-common | - | 公共 |\n")
    f.write(f"\n## 重新生成\n\n")
    f.write(f"```bash\n")
    f.write(f"python3 scripts/split_fix_sql_by_module.py\n")
    f.write(f"```\n")
print(f"✓ README: {readme}")
