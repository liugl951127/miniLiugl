#!/usr/bin/env python3
"""
MiniMax Platform 全量 DDL 生成器 (V3.0.0)

功能:
  1. 扫描 backend/minimax-*/src/main/java/**/entity/*.java
  2. 解析 @TableName / @TableField / 字段类型
  3. 生成 CREATE TABLE (IF NOT EXISTS) + 索引
  4. 输出 sql/init.sql (单文件汇总)

设计原则:
  - 全量 DDL: 一次生成所有表的定义, 不依赖外部文件
  - 去重: 同一表只输出一次 (基于 @TableName)
  - 注释: 自动从 Java 类注释 / 字段注释提取
  - 引擎: InnoDB / utf8mb4
  - 排序: 表名按字母顺序输出, 便于阅读

用法:
  python3 scripts/gen_ddl.py
  python3 scripts/gen_ddl.py --module minimax-ai  # 仅某模块
"""
import os
import re
import sys
import argparse
from pathlib import Path
from collections import OrderedDict

# 项目根 (允许命令行参数)
DEFAULT_ROOT = Path('/workspace/miniLiugl-v300/backend')
ROOT = DEFAULT_ROOT
OUT_FILE = Path('/workspace/miniLiugl-v300/sql/init.sql')

# Java -> MySQL 类型映射
# 第一个元素: MySQL 类型
# 第二个元素: 默认值 (空字符串表示无)
TYPE_MAP = {
    'String':     ('VARCHAR(255)',  'DEFAULT NULL'),
    'Long':       ('BIGINT',        'DEFAULT 0'),
    'Integer':    ('INT',           'DEFAULT 0'),
    'int':        ('INT',           'DEFAULT 0'),
    'long':       ('BIGINT',        'DEFAULT 0'),
    'Double':     ('DOUBLE',        'DEFAULT 0.0'),
    'double':     ('DOUBLE',        'DEFAULT 0.0'),
    'Float':      ('FLOAT',         'DEFAULT 0.0'),
    'float':      ('FLOAT',         'DEFAULT 0.0'),
    'Boolean':    ('TINYINT(1)',    'DEFAULT 0'),
    'boolean':    ('TINYINT(1)',    'DEFAULT 0'),
    'BigDecimal': ('DECIMAL(20,4)', 'DEFAULT 0'),
    'LocalDateTime': ('DATETIME',   'DEFAULT CURRENT_TIMESTAMP'),
    'LocalDate':  ('DATE',          'DEFAULT NULL'),
    'LocalTime':  ('TIME',          'DEFAULT NULL'),
    'Date':       ('DATETIME',      'DEFAULT CURRENT_TIMESTAMP'),
}

# TEXT / MEDIUMTEXT 类型 (按需调整)
TEXT_FIELDS = {
    'description', 'content', 'detail', 'definitionJson', 'payload',
    'snapshot', 'text', 'itemsJson', 'tombstonesJson', 'responseBody',
    'metricsJson', 'customHeaders', 'configuration', 'config',
    'systemPrompt', 'requestBody', 'errorStack', 'response', 'log'
}


def parse_args():
    """解析命令行参数"""
    p = argparse.ArgumentParser(description='生成 MiniMax 全量 DDL')
    p.add_argument('--root', default=str(DEFAULT_ROOT), help='backend 根目录')
    p.add_argument('--out', default=str(OUT_FILE), help='输出文件')
    p.add_argument('--module', help='仅生成某模块 (e.g. minimax-ai)')
    return p.parse_args()


def find_entity_files(root: Path, module: str = None) -> list:
    """查找所有实体类文件"""
    pattern = '**/entity/*.java'
    files = []
    modules = [module] if module else [d for d in os.listdir(root) if d.startswith('minimax-')]
    for mod in modules:
        mod_path = root / mod / 'src' / 'main' / 'java'
        if not mod_path.exists():
            continue
        for f in mod_path.glob(pattern):
            files.append(f)
    return sorted(files)


def extract_class_javadoc(content: str) -> str:
    """提取类注释"""
    m = re.search(r'/\*\*(.*?)\*/', content, re.DOTALL)
    if m:
        text = m.group(1)
        # 清理 *
        text = re.sub(r'\n\s*\*\s?', ' ', text)
        return text.strip()[:200]
    return ''


def extract_table_info(content: str) -> dict:
    """提取 @TableName 和字段"""
    # 类名 + TableName
    cls_match = re.search(r'public\s+class\s+(\w+)', content)
    if not cls_match:
        return None
    class_name = cls_match.group(1)
    table_match = re.search(r'@TableName\s*\(\s*["\'](\w+)["\']\s*\)', content)
    if not table_match:
        return None
    table_name = table_match.group(1)
    # 类注释
    table_comment = extract_class_javadoc(content)

    fields = []
    seen = set()

    # 1. @TableId 主键
    for m in re.finditer(r'@TableId\s*\([^)]*\)\s*\n\s*private\s+([\w<>,\s]+?)\s+(\w+)\s*;', content):
        ftype, fname = m.group(1).strip(), m.group(2).strip()
        ftype = re.sub(r'\s+', ' ', ftype)
        fields.append({
            'name': fname,
            'type_raw': ftype,
            'db_type': 'BIGINT NOT NULL AUTO_INCREMENT',
            'default': None,
            'nullable': False,
            'primary': True,
            'comment': ''
        })
        seen.add(fname)

    # 2. @TableField(...) private Type name;
    for m in re.finditer(
        r'@TableField(?:\s*\([^)]*\))?\s*\n\s*private\s+([\w<>,\s]+?)\s+(\w+)\s*;',
        content
    ):
        ftype, fname = m.group(1).strip(), m.group(2).strip()
        ftype = re.sub(r'\s+', ' ', ftype)
        if fname in seen:
            continue
        # 提取 @TableField 里的 column name
        ann_match = re.search(r'@TableField\s*\(\s*["\'](\w+)["\']', content[max(0, m.start()-100):m.end()])
        col_name = ann_match.group(1) if ann_match else fname
        seen.add(fname)

        # 推断类型
        if ftype in TYPE_MAP:
            db_type, default = TYPE_MAP[ftype]
        elif ftype.startswith('List<') or ftype.startswith('Set<'):
            db_type = 'TEXT'
            default = 'DEFAULT NULL'
        elif ftype.startswith('Map<'):
            db_type = 'TEXT'
            default = 'DEFAULT NULL'
        else:
            db_type = 'VARCHAR(255)'
            default = 'DEFAULT NULL'

        # TEXT 类 (长文本)
        if fname.lower() in TEXT_FIELDS:
            if 'definition' in fname.lower() or 'payload' in fname.lower() or 'snapshot' in fname.lower():
                db_type = 'MEDIUMTEXT'
            else:
                db_type = 'TEXT'
            default = 'DEFAULT NULL'

        # 是否 NOT NULL
        is_basic = ftype in {'Long', 'Integer', 'int', 'long', 'Double', 'double', 'Float', 'float', 'Boolean', 'boolean', 'BigDecimal'}
        nullable = not is_basic

        fields.append({
            'name': col_name,  # 用 col_name
            'type_raw': ftype,
            'db_type': db_type,
            'default': default,
            'nullable': nullable,
            'primary': False,
            'comment': ''
        })

    # 3. 无注解的 private (非 transient/static) - 用 field 名作 col 名
    for m in re.finditer(
        r'(?<![)\w])\n\s*private\s+([\w<>,\s]+?)\s+(\w+)\s*;',
        content
    ):
        ftype, fname = m.group(1).strip(), m.group(2).strip()
        ftype = re.sub(r'\s+', ' ', ftype)
        if fname in seen or fname in {'id'}:
            continue
        # 排除 static / transient
        if 'static' in ftype or 'transient' in ftype:
            continue
        # 排除 lombok (@Data 生成的 getter/setter, 没字段, 所以这段已经只匹配字段)
        if ftype in TYPE_MAP:
            db_type, default = TYPE_MAP[ftype]
        elif ftype.startswith('List<') or ftype.startswith('Set<'):
            db_type = 'TEXT'
            default = 'DEFAULT NULL'
        elif ftype.startswith('Map<'):
            db_type = 'TEXT'
            default = 'DEFAULT NULL'
        else:
            db_type = 'VARCHAR(255)'
            default = 'DEFAULT NULL'
        if fname.lower() in TEXT_FIELDS:
            db_type = 'TEXT' if 'definition' not in fname.lower() else 'MEDIUMTEXT'
            default = 'DEFAULT NULL'
        is_basic = ftype in {'Long', 'Integer', 'int', 'long', 'Double', 'double', 'Float', 'float', 'Boolean', 'boolean', 'BigDecimal'}
        fields.append({
            'name': fname,
            'type_raw': ftype,
            'db_type': db_type,
            'default': default,
            'nullable': not is_basic,
            'primary': False,
            'comment': ''
        })
        seen.add(fname)

    return {
        'class_name': class_name,
        'table_name': table_name,
        'comment': table_comment,
        'fields': fields
    }


def generate_create_table(table: dict) -> str:
    """生成 CREATE TABLE 语句"""
    lines = []
    lines.append(f'-- {table["class_name"]} -> {table["table_name"]}')
    lines.append('CREATE TABLE IF NOT EXISTS `' + table['table_name'] + '` (')
    pk_field = None
    field_defs = []
    for f in table['fields']:
        if f['primary']:
            pk_field = f
            field_defs.append(f'    `{f["name"]}` {f["db_type"]} COMMENT \'{f["name"]}\'')
        else:
            not_null = '' if f['nullable'] else ' NOT NULL'
            default = f['default'] or ''
            field_defs.append(f'    `{f["name"]}` {f["db_type"]}{not_null} {default} COMMENT \'{f["name"]}\'')
    # 主键
    if pk_field:
        field_defs.append(f'    PRIMARY KEY (`{pk_field["name"]}`)')
    # 唯一索引 (常见字段)
    unique_fields = []
    for f in table['fields']:
        if f['name'] in ('username', 'agentKey', 'modelKey', 'webhookId', 'roomId', 'userId', 'docId'):
            if not f['primary'] and f['name'] not in unique_fields:
                unique_fields.append(f['name'])
    for u in unique_fields[:2]:  # 最多 2 个 unique
        field_defs.append(f'    UNIQUE KEY `uk_{u}` (`{u}`)')
    lines.append(',\n'.join(field_defs))
    lines.append(f") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='{table['class_name']} (auto-generated V3.0.0)';")
    return '\n'.join(lines)


def main():
    args = parse_args()
    global ROOT, OUT_FILE
    ROOT = Path(args.root)
    OUT_FILE = Path(args.out)

    if not ROOT.exists():
        print(f'[ERROR] backend 根目录不存在: {ROOT}', file=sys.stderr)
        sys.exit(1)

    entity_files = find_entity_files(ROOT, args.module)
    print(f'[INFO] 扫描到 {len(entity_files)} 个实体类')

    tables = OrderedDict()
    for f in entity_files:
        try:
            content = f.read_text(encoding='utf-8')
            info = extract_table_info(content)
            if not info:
                continue
            # 跳过自增 ID 主键冲突 (多个 @TableId)
            if info['table_name'] in tables:
                continue
            tables[info['table_name']] = info
        except Exception as e:
            print(f'[WARN] 解析失败 {f}: {e}')

    print(f'[INFO] 生成 {len(tables)} 张表')

    # 输出
    OUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    with OUT_FILE.open('w', encoding='utf-8') as f:
        f.write('-- ' + '=' * 67 + '\n')
        f.write('-- MiniMax Platform V3.0.0 全量 DDL (单文件汇总)\n')
        f.write(f'-- 表数: {len(tables)}\n')
        f.write('-- 字符集: utf8mb4 / 引擎: InnoDB\n')
        f.write('-- ' + '=' * 67 + '\n\n')
        f.write('CREATE DATABASE IF NOT EXISTS `minimax_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n')
        f.write('USE `minimax_platform`;\n\n')
        f.write('SET NAMES utf8mb4;\n')
        f.write('SET FOREIGN_KEY_CHECKS = 0;\n\n')
        for table_name, info in sorted(tables.items()):
            f.write(generate_create_table(info) + '\n\n')
        f.write('SET FOREIGN_KEY_CHECKS = 1;\n')
        f.write('\n-- 全部 DDL 生成完毕\n')
    print(f'[OK] 输出: {OUT_FILE} ({OUT_FILE.stat().st_size} bytes)')


if __name__ == '__main__':
    main()
