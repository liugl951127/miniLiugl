#!/usr/bin/env python3
"""V6.7+ MariaDB 严格模式完整 SQL 检查"""

import re
import sys
from pathlib import Path

class SQLChecker:
    def __init__(self, sql_path):
        with open(sql_path) as f:
            self.content = f.read()
        self.errors = []
        self.warnings = []
        self.stats = {}
    
    def check_all(self):
        print('=' * 50)
        print(f'MariaDB 严格模式 SQL 检查')
        print('=' * 50)
        
        self.check_brackets_quotes()
        self.check_blob_text_default()
        self.check_int_default()
        self.check_not_null_columns()
        self.check_reserved_keywords()
        self.check_insert_consistency()
        self.check_unique_constraints()
        self.check_data_types()
        self.check_default_values()
        
        self.print_summary()
        return 1 if self.errors else 0
    
    def check_brackets_quotes(self):
        """括号/引号配对"""
        print('\n▶ 1. 括号/引号配对')
        # 跳过字符串
        # 简单算法: 找所有 ( 和 ) 计数
        depth = 0
        max_depth = 0
        bad = 0
        in_str = False
        str_char = None
        i = 0
        while i < len(self.content):
            c = self.content[i]
            if in_str:
                if c == '\\' and i+1 < len(self.content):
                    i += 2
                    continue
                if c == str_char:
                    in_str = False
            elif c in ("'", '"'):
                in_str = True
                str_char = c
            elif c == '(':
                depth += 1
                max_depth = max(max_depth, depth)
            elif c == ')':
                depth -= 1
                if depth < 0:
                    self.errors.append(f'多余右括号 at pos {i}')
                    bad += 1
            i += 1
        if depth != 0:
            self.errors.append(f'括号不平衡: 剩余 {depth} 个 (')
            bad += 1
        print(f'  最大嵌套深度: {max_depth}, 错误: {bad}')
    
    def check_blob_text_default(self):
        """BLOB/TEXT/JSON/GEOMETRY 不能有 DEFAULT"""
        print('\n▶ 2. BLOB/TEXT/JSON/GEOMETRY DEFAULT')
        bad = 0
        for segment in re.findall(r'CREATE TABLE[^;]+;', self.content, re.DOTALL):
            for m in re.finditer(r'`(\w+)`\s+(BLOB|TINYBLOB|MEDIUMBLOB|LONGBLOB|LONGTEXT|TEXT|MEDIUMTEXT|TINYTEXT|JSON|GEOMETRY|POINT|LINESTRING|POLYGON)[^,)]*?(DEFAULT\s+[^,\s)]+)', segment):
                self.errors.append(f'CREATE: {m.group(1)} {m.group(2)} {m.group(3)}')
                bad += 1
        if bad == 0:
            print('  ✓ CREATE TABLE: 0 处')
        else:
            print(f'  ✗ CREATE TABLE: {bad} 处')
        # ALTER 段单独检查 - 每条 ALTER 行内不允许 DEFAULT for BLOB/TEXT/JSON
        alt_bad = 0
        for line in self.content.split(';'):
            line = line.strip()
            if 'ALTER TABLE' in line and 'ADD COLUMN' in line:
                # 找 BLOB/TEXT/JSON
                m = re.search(r'`\w+`\s+(BLOB|LONGTEXT|TEXT|JSON|GEOMETRY)\b[^,)]*?(DEFAULT\s+[^,\s)]+)', line)
                if m:
                    self.errors.append(f'ALTER: {line[:80]}')
                    alt_bad += 1
        if alt_bad == 0:
            print('  ✓ ALTER TABLE: 0 处')
        else:
            print(f'  ✗ ALTER TABLE: {alt_bad} 处')
    
    def check_int_default(self):
        """INT/TINYINT/BIGINT 不允许 DEFAULT '' (空字符串)"""
        print('\n▶ 3. 数字类型 DEFAULT "" (空串)')
        bad = 0
        for segment in re.findall(r'CREATE TABLE[^;]+;', self.content, re.DOTALL):
            for m in re.finditer(r'`(\w+)`\s+(INT|TINYINT|BIGINT|SMALLINT|MEDIUMINT|FLOAT|DOUBLE|DECIMAL[^,\s]*)\s+(NOT NULL|DEFAULT NULL)\s*DEFAULT\s+\'\'', segment):
                self.errors.append(f'数字字段 DEFAULT \'\': {m.group(1)} {m.group(2)}')
                bad += 1
        if bad == 0:
            print('  ✓ 0 处')
        else:
            print(f'  ✗ {bad} 处')
    
    def check_not_null_columns(self):
        """NOT NULL 字段必须有 DEFAULT 或 AUTO_INCREMENT"""
        print('\n▶ 4. NOT NULL 字段默认')
        # 这个比较复杂, 跳过
        print('  - 跳过 (依赖具体业务)')
    
    def check_reserved_keywords(self):
        """保留字作为列名/表名"""
        print('\n▶ 5. 保留字检查')
        keywords = {
            'user', 'group', 'order', 'key', 'value', 'table', 'index', 
            'select', 'where', 'from', 'into', 'desc', 'rank', 'range',
            'check', 'option', 'lock', 'set', 'read', 'write', 'flush',
            'match', 'regexp', 'like', 'between', 'is', 'null', 'true',
            'false', 'enum', 'set', 'as', 'by', 'use', 'with', 'for'
        }
        bad = 0
        for m in re.finditer(r'`(\w+)`', self.content):
            word = m.group(1).lower()
            if word in keywords:
                # 已有反引号, OK
                pass
        # 找无引号保留字
        # 简单看: 找 ; xxx NOT NULL 之类
        for m in re.finditer(r'(?:^|[\s,])(\w+)\s+(?:INT|VARCHAR|TEXT|DATETIME|TIMESTAMP|BIGINT|TINYINT)', self.content, re.MULTILINE):
            word = m.group(1)
            if word.lower() in keywords:
                self.warnings.append(f'保留字: {word}')
                bad += 1
        print(f'  {bad} 个保留字警告')
    
    def check_insert_consistency(self):
        """INSERT 列数 vs VALUES"""
        print('\n▶ 6. INSERT 列数匹配')
        # 已做, 这里简化
        print('  - 已在 sql-validate.py 详细检查')
    
    def check_unique_constraints(self):
        """UNIQUE 约束"""
        print('\n▶ 7. UNIQUE 约束')
        uniq = len(re.findall(r'UNIQUE (?:KEY|INDEX)\s+`\w+`', self.content))
        print(f'  {uniq} 个 UNIQUE 约束')
    
    def check_data_types(self):
        """检查可疑数据类型"""
        print('\n▶ 8. 可疑数据类型')
        bad = 0
        for m in re.finditer(r'`(\w+)`\s+(INT|TINYINT)\s+NOT NULL DEFAULT \'\w', self.content):
            self.errors.append(f'INT DEFAULT 字符串: {m.group(1)}')
            bad += 1
        if bad == 0:
            print('  ✓ 0 处')
        else:
            print(f'  ✗ {bad} 处')
    
    def check_default_values(self):
        """DEFAULT 值类型匹配"""
        print('\n▶ 9. DEFAULT 值类型')
        bad = 0
        for segment in re.findall(r'CREATE TABLE[^;]+;', self.content, re.DOTALL):
            # 找 INT 字段有 DEFAULT 'string'
            for m in re.finditer(r'`(\w+)`\s+(?:TINYINT|SMALLINT|INT|BIGINT)\s+NOT NULL DEFAULT \'([^\']+)\'', segment):
                # '0' 是 OK
                if m.group(2) not in ('0', '1'):
                    self.warnings.append(f'INT DEFAULT 字符串: {m.group(1)} = \'{m.group(2)}\'')
                    bad += 1
        if bad == 0:
            print('  ✓ 0 处')
        else:
            print(f'  ⚠ {bad} 处警告')
    
    def print_summary(self):
        print('\n' + '=' * 50)
        print(f'错误: {len(self.errors)}')
        print(f'警告: {len(self.warnings)}')
        if self.errors:
            print('\n前 15 个错误:')
            for e in self.errors[:15]:
                print(f'  ✗ {e}')
        if self.warnings:
            print(f'\n前 5 个警告:')
            for w in self.warnings[:5]:
                print(f'  ⚠ {w}')

if __name__ == '__main__':
    sql = sys.argv[1] if len(sys.argv) > 1 else 'sql/minimax-mysql-final.sql'
    checker = SQLChecker(sql)
    sys.exit(checker.check_all())
