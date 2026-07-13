#!/usr/bin/env python3
"""
扫描所有 Java 实体类, 生成 100% 覆盖的完整 SQL DDL
- 解析 @TableName, @TableId, @TableLogic, @TableField
- 按 entity 自动生成 CREATE TABLE
- 按模块分组, 每模块一段
- 直接 mysql < complete.sql 可用
"""
import re
import os
import glob
from collections import defaultdict, OrderedDict

# ============================================================
# Java → MySQL 类型映射
# ============================================================
TYPE_MAP = {
    'String': 'VARCHAR(255)',
    'Long': 'BIGINT',
    'Integer': 'INT',
    'Boolean': 'TINYINT(1)',
    'Double': 'DOUBLE',
    'Float': 'FLOAT',
    'BigDecimal': 'DECIMAL(20,4)',
    'LocalDateTime': 'DATETIME',
    'LocalDate': 'DATE',
    'LocalTime': 'TIME',
    'Date': 'DATETIME',
    'byte[]': 'BLOB',
    'JSONObject': 'TEXT',
    'JSONArray': 'TEXT',
}

# ============================================================
# 工具
# ============================================================
def to_snake(name):
    s = re.sub(r'([A-Z]+)([A-Z][a-z])', r'\1_\2', name)
    s = re.sub(r'([a-z\d])([A-Z])', r'\1_\2', s)
    return s.lower()

def cn_comment(field_name):
    """生成中文 COMMENT 注释 (fallback 到 COMMON 字典)"""
    COMMON = {
        'id': '主键ID', 'createdAt': '创建时间', 'updatedAt': '更新时间',
        'createdBy': '创建人ID', 'updatedBy': '更新人ID', 'deleted': '逻辑删除标记',
        'version': '乐观锁版本号', 'tenantId': '租户ID', 'remark': '备注',
        'description': '描述', 'name': '名称', 'code': '编码', 'type': '类型',
        'status': '状态', 'enabled': '是否启用', 'sort': '排序号',
    }
    cn = COMMON.get(field_name, field_name)
    return f"'{cn}({field_name})'"

# ============================================================
# 解析 Entity
# ============================================================
def parse_entity(path):
    with open(path, 'r') as f:
        c = f.read()

    # 提取模块名
    m_mod = re.search(r'minimax-(\w+)/', path)
    module = m_mod.group(1) if m_mod else 'common'

    # @TableName
    m = re.search(r'@TableName\s*\(\s*(?:value\s*=\s*)?"(\w+)"\s*\)', c)
    if not m:
        m2 = re.search(r'public\s+class\s+(\w+)\b', c)
        if not m2:
            return None
        table = to_snake(m2.group(1))
    else:
        table = m.group(1)

    # 解析类级别注解
    has_logic_delete = '@TableLogic' in c

    # 解析字段: 按顺序 (注解 + 类型 + 名称)
    # 匹配: (注解块)\n类型\n名称
    field_pattern = re.compile(
        r'((?:@\w+(?:\([^)]*\))?\s*\n?\s*)*)'  # 注解块
        r'(?:private|protected|public)\s+'
        r'(?:static\s+)?'
        r'([\w<>,\s\[\]]+?)\s+'
        r'(\w+)\s*[;=]',
        re.MULTILINE
    )
    fields = []
    for m in field_pattern.finditer(c):
        annots = m.group(1)
        java_type = m.group(2).strip().split('<')[0].strip()
        field_name = m.group(3)
        if field_name in ('serialVersionUID', 'log', 'logger'): continue
        if field_name.isupper(): continue  # 常量

        # 字段注解
        is_pk = '@TableId' in annots
        is_logic = '@TableLogic' in annots
        is_insert_fill = 'FieldFill.INSERT' in annots
        is_update_fill = 'FieldFill.INSERT_UPDATE' in annots

        # SQL 列名:
        # - 如果 @TableField("xxx") 显式映射, 用映射名
        # - 否则 entity 字段 (camelCase) 转 snake_case
        # 这样跟 MyBatis-Plus map-underscore-to-camel-case: true 默认行为匹配
        col_name = to_snake(field_name)
        m_col = re.search(r'@TableField\s*\(\s*(?:value\s*=\s*)?"(\w+)"', annots)
        if m_col:
            col_name = m_col.group(1)

        # IdType
        auto_inc = False
        m_id = re.search(r'IdType\.(\w+)', annots)
        if m_id and m_id.group(1) == 'AUTO':
            auto_inc = True

        fields.append({
            'name': field_name,
            'col': col_name,
            'java_type': java_type,
            'is_pk': is_pk,
            'is_logic': is_logic,
            'insert_fill': is_insert_fill,
            'update_fill': is_update_fill,
            'auto_inc': auto_inc,
        })

    return {
        'table': table,
        'module': module,
        'fields': fields,
        'has_logic_delete': has_logic_delete,
        'file': path,
    }

# ============================================================
# 生成 CREATE TABLE
# ============================================================
def gen_create_table(entity):
    table = entity['table']
    fields = entity['fields']
    lines = []

    for f in fields:
        col = f['col']
        java_type = f['java_type']
        sql_type = TYPE_MAP.get(java_type, 'VARCHAR(255)')

        if f['is_pk']:
            if f['auto_inc'] and sql_type == 'BIGINT':
                lines.append(f"    `{col}` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID({col})'")
            else:
                lines.append(f"    `{col}` {sql_type} NOT NULL COMMENT '主键ID({col})'")
        else:
            # 默认值
            if sql_type in ('BIGINT', 'INT', 'TINYINT(1)', 'DOUBLE', 'FLOAT', 'DECIMAL(20,4)'):
                if f['insert_fill'] or f['update_fill']:
                    default = " DEFAULT CURRENT_TIMESTAMP" if sql_type == 'DATETIME' else " DEFAULT 0"
                else:
                    default = " DEFAULT 0"
            else:
                default = " DEFAULT NULL"
            lines.append(f"    `{col}` {sql_type}{default} COMMENT {cn_comment(col)}")

    # 主键
    pks = [f['col'] for f in fields if f['is_pk']]
    if pks:
        lines.append(f"    PRIMARY KEY (`{'`, `'.join(pks)}`)")
    elif len(pks) == 0 and len(fields) >= 2:
        # 如果没有 @TableId 字段, 用前两个字段当联合主键 (中间表)
        candidate = [f['col'] for f in fields[:2]]
        if all(c for c in candidate):
            lines.append(f"    PRIMARY KEY (`{'`, `'.join(candidate)}`)")

    body = ',\n'.join(lines)
    return f"CREATE TABLE IF NOT EXISTS `{table}` (\n{body}\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='{table} (auto-generated V3.5.5)'"

# ============================================================
# 主流程
# ============================================================
def main():
    # 收集所有 entity
    entities = []
    for root, dirs, files in os.walk('/workspace/miniLiugl/backend'):
        if 'src/test' in root: continue
        for f in files:
            if f.endswith('.java') and 'entity' in root.lower():
                e = parse_entity(os.path.join(root, f))
                if e and e['fields']:
                    entities.append(e)

    # 去重 (同一表可能多个 entity)
    seen_tables = set()
    unique = []
    for e in entities:
        if e['table'] not in seen_tables:
            seen_tables.add(e['table'])
            unique.append(e)

    # 按模块分组
    by_module = defaultdict(list)
    for e in unique:
        by_module[e['module']].append(e)

    # 输出
    out = []
    out.append("-- =============================================================")
    out.append(f"-- MiniMax Platform V3.5.5+ 完整 SQL DDL (扫描所有 Entity 自动生成)")
    out.append(f"-- 共 {len(unique)} 张表 / {len(by_module)} 个模块")
    out.append(f"-- 生成时间: {os.popen('date').read().strip()}")
    out.append(f"-- 生成工具: scripts/gen_complete_sql.py")
    out.append(f"--")
    out.append(f"-- 用法: ")
    out.append(f"--   1. 全新部署: docker compose -f docker-compose.mini.yml up -d")
    out.append(f"--   2. 增量修复: mysql -uroot -proot123456 minimax_platform < complete.sql")
    out.append(f"-- =============================================================")
    out.append("")
    out.append("CREATE DATABASE IF NOT EXISTS `minimax_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;")
    out.append("USE `minimax_platform`;")
    out.append("")
    out.append("SET NAMES utf8mb4;")
    out.append("SET FOREIGN_KEY_CHECKS = 0;")
    out.append("")

    # 按模块输出
    module_order = ['common', 'auth', 'ai', 'agent', 'model', 'rag', 'function',
                    'chat', 'memory', 'multimodal', 'monitor', 'admin', 'prompt',
                    'analytics', 'pipeline', 'gateway', 'ws']
    sorted_modules = sorted(by_module.keys(), key=lambda m: module_order.index(m) if m in module_order else 999)

    for mod in sorted_modules:
        ents = by_module[mod]
        out.append(f"\n-- =========================================")
        out.append(f"-- 模块: {mod} ({len(ents)} 张表)")
        out.append(f"-- =========================================\n")
        # 按表名排序
        for e in sorted(ents, key=lambda x: x['table']):
            sql = gen_create_table(e)
            out.append(f"-- {e['file'].split('minimax-')[-1].split('/')[0]}/{e['file'].split('/')[-1]}")
            out.append(f"DROP TABLE IF EXISTS `{e['table']}`;")
            out.append(sql + ";")
            out.append("")

    out.append("SET FOREIGN_KEY_CHECKS = 1;")
    out.append("")
    out.append(f"-- 完成: 共 {len(unique)} 张表")

    # 写入文件
    out_path = '/workspace/miniLiugl/sql/complete.sql'
    with open(out_path, 'w') as f:
        f.write('\n'.join(out))

    # 统计
    total_fields = sum(len(e['fields']) for e in unique)
    print(f"✅ 已生成: {out_path}")
    print(f"   表数: {len(unique)}")
    print(f"   模块: {len(by_module)} ({', '.join(sorted_modules)})")
    print(f"   字段总数: {total_fields}")
    print(f"   文件大小: {os.path.getsize(out_path) / 1024:.1f}KB")

if __name__ == '__main__':
    main()
