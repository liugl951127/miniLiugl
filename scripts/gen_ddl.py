#!/usr/bin/env python3
"""
从 Java 实体生成 MySQL DDL (V2.8.2)
- 扫描 @TableName 实体
- 解析字段类型
- 生成 CREATE TABLE 语句
"""
import re
import os
import sys
from pathlib import Path

ROOT = Path('/workspace/miniLiugl/backend')
OUT_FILE = Path('/workspace/miniLiugl/sql/schema-v2.8.2.sql')

# Java -> MySQL 类型映射
TYPE_MAP = {
    'String': ('VARCHAR(255)', ''),
    'Long': ('BIGINT', 'DEFAULT 0'),
    'Integer': ('INT', 'DEFAULT 0'),
    'int': ('INT', 'DEFAULT 0'),
    'long': ('BIGINT', 'DEFAULT 0'),
    'Double': ('DOUBLE', 'DEFAULT 0'),
    'double': ('DOUBLE', 'DEFAULT 0'),
    'Float': ('FLOAT', 'DEFAULT 0'),
    'float': ('FLOAT', 'DEFAULT 0'),
    'Boolean': ('TINYINT(1)', 'DEFAULT 0'),
    'boolean': ('TINYINT(1)', 'DEFAULT 0'),
    'BigDecimal': ('DECIMAL(20,4)', 'DEFAULT 0'),
    'LocalDateTime': ('DATETIME', 'DEFAULT NULL'),
    'LocalDate': ('DATE', 'DEFAULT NULL'),
    'LocalTime': ('TIME', 'DEFAULT NULL'),
    'Date': ('DATETIME', 'DEFAULT NULL'),
}

def extract_table_name(content):
    m = re.search(r'@TableName\("([^"]+)"\)', content)
    return m.group(1) if m else None

def extract_class_name(content):
    m = re.search(r'public\s+class\s+(\w+)', content)
    return m.group(1) if m else None

def extract_fields(content):
    pattern = r'(@TableId[^)]*\)|@TableField[^)]*\)|)\s*(private|public)\s+([\w<>,\s\[\]]+?)\s+(\w+)\s*[;=]'
    fields = []
    for m in re.finditer(pattern, content):
        ann = m.group(1)
        ftype = m.group(3).strip()
        fname = m.group(4).strip()
        if fname == 'serialVersionUID':
            continue
        if 'static' in m.group(0):
            continue
        fields.append({'name': fname, 'type': ftype, 'ann': ann})
    return fields

def get_sql_type(java_type):
    if '<' in java_type:
        java_type = java_type.split('<')[0]
    java_type = java_type.strip()
    return TYPE_MAP.get(java_type, ('VARCHAR(255)', ''))[0]

def has_annotation(ann, name):
    return f'@{name}' in ann

def gen_create_ddl(entity_file):
    content = entity_file.read_text(encoding='utf-8', errors='ignore')
    table = extract_table_name(content)
    cls = extract_class_name(content)
    if not table or not cls:
        return None
    fields = extract_fields(content)
    if not fields:
        return None

    lines = [f'-- {cls} -> {table}', f'DROP TABLE IF EXISTS `{table}`;', f'CREATE TABLE `{table}` (']
    field_lines = []
    pk_field = None

    for f in fields:
        sql_type = get_sql_type(f['type'])
        if has_annotation(f['ann'], 'TableId'):
            field_lines.append(f'    `{f["name"]}` BIGINT NOT NULL AUTO_INCREMENT')
            pk_field = f['name']
        else:
            if sql_type in ('VARCHAR(255)', 'TEXT', 'BLOB', 'DATETIME', 'DATE', 'TIME'):
                field_lines.append(f'    `{f["name"]}` {sql_type} DEFAULT NULL')
            else:
                field_lines.append(f'    `{f["name"]}` {sql_type} NOT NULL DEFAULT 0')

    if pk_field:
        field_lines.append(f'    PRIMARY KEY (`{pk_field}`)')

    lines.append(',\n'.join(field_lines))
    lines.append(f") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='{cls}';")
    return '\n'.join(lines)

def main():
    java_files = list(ROOT.rglob('*.java'))
    entity_files = []
    for f in java_files:
        try:
            content = f.read_text(encoding='utf-8', errors='ignore')
            if '@TableName' in content and 'public class' in content:
                entity_files.append(f)
        except:
            pass

    entity_files = sorted(set(entity_files), key=lambda x: str(x))
    print(f'Found {len(entity_files)} entity files')

    output = []
    output.append('-- ============================================================')
    output.append('-- MiniMax Platform V2.8.2 全量 DDL (自动生成)')
    output.append('-- 生成时间: 2026-07-12')
    output.append('-- 字符集: utf8mb4 / 引擎: InnoDB')
    output.append('-- ============================================================')
    output.append('SET NAMES utf8mb4;')
    output.append('SET CHARACTER SET utf8mb4;')
    output.append('')

    seen = set()
    for f in entity_files:
        ddl = gen_create_ddl(f)
        if ddl:
            m = re.search(r'CREATE TABLE `(\w+)`', ddl)
            if m and m.group(1) not in seen:
                seen.add(m.group(1))
                output.append(ddl)
                output.append('')

    OUT_FILE.parent.mkdir(exist_ok=True)
    OUT_FILE.write_text('\n'.join(output), encoding='utf-8')
    print(f'Written {len(seen)} tables to {OUT_FILE}')
    print(f'Size: {OUT_FILE.stat().st_size} bytes')

if __name__ == '__main__':
    main()
