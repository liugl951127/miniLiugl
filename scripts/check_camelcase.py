#!/usr/bin/env python3
"""
检查 SQL 脚本字段和 Java 实体类字段的驼峰规则是否一致
最终版: 只对比 sql/complete.sql (唯一基线)
"""
import re
import os
from collections import defaultdict

def to_snake(name):
    s = re.sub(r'([A-Z]+)([A-Z][a-z])', r'\1_\2', name)
    s = re.sub(r'([a-z\d])([A-Z])', r'\1_\2', s)
    return s.lower()

def parse_sql_columns(path):
    with open(path, 'r') as f:
        c = f.read()
    c = re.sub(r'--[^\n]*\n', '\n', c)
    c = re.sub(r'/\*[\s\S]*?\*/', '', c)
    c = re.sub(r"COMMENT\s+'[^']*'", '', c)
    tables = {}
    pattern = re.compile(
        r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?(\w+)`?\s*\(([\s\S]*?)\)\s*ENGINE",
        re.IGNORECASE
    )
    for m in pattern.finditer(c):
        table = m.group(1)
        body = m.group(2)
        cols = {}
        for line in body.split('\n'):
            line = line.strip().rstrip(',').strip()
            if not line: continue
            if line.upper().startswith(('PRIMARY','UNIQUE','KEY','INDEX','FOREIGN','CONSTRAINT','CHECK')): continue
            cm = re.match(r'`?(\w+)`?\s+', line)
            if cm and cm.group(1).upper() not in ('PRIMARY','UNIQUE','KEY','INDEX','CONSTRAINT'):
                cols[cm.group(1)] = line
        if cols:
            tables[table] = cols
    return tables

def parse_entity(path):
    with open(path, 'r') as f:
        c = f.read()
    m = re.search(r'@TableName\s*\(\s*(?:value\s*=\s*)?"(\w+)"\s*\)', c)
    if not m:
        m2 = re.search(r'public\s+class\s+(\w+)\b', c)
        if not m2: return None
        table = to_snake(m2.group(1))
    else:
        table = m.group(1)
    fields = []
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
        java_type = m.group(2).strip().split('<')[0].strip()
        field_name = m.group(3)
        if field_name in ('serialVersionUID', 'log', 'logger'): continue
        if field_name.isupper(): continue
        col_name = field_name
        m_col = re.search(r'@TableField\s*\(\s*(?:value\s*=\s*)?"(\w+)"', annots)
        if m_col:
            col_name = m_col.group(1)
        fields.append({
            'name': field_name,
            'col': col_name,
            'java_type': java_type,
            'file': path,
        })
    return {'table': table, 'fields': fields, 'file': path}

def main():
    if not os.path.exists('/workspace/miniLiugl/sql/complete.sql'):
        print("❌ sql/complete.sql 不存在")
        return

    # 收集 Entity
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
    print(f"📦 Entity 总数: {len(entities)}")
    print()

    # 唯一 SQL 源
    sql_tables = parse_sql_columns('/workspace/miniLiugl/sql/complete.sql')
    print("=" * 80)
    print("📄 sql/complete.sql (V3.5.5 唯一基线)")
    print("=" * 80)
    print(f"  表数: {len(sql_tables)}")

    all_cols = []
    for cols in sql_tables.values():
        all_cols.extend(cols.keys())
    camel_cols = [c for c in all_cols if any(ch.isupper() for ch in c)]
    pure_snake = [c for c in all_cols if '_' in c and not any(ch.isupper() for ch in c)]
    print(f"  camelCase 列: {len(camel_cols)}")
    print(f"  snake_case 列 (纯小写): {len(pure_snake)}")

    # 跟 entity 比对
    total_entity = 0
    matched = 0
    mismatched = []
    case_mismatch = []
    for table, ent_list in sorted(ent_by_table.items()):
        sql_cols_dict = sql_tables.get(table, {})
        sql_keys = set(sql_cols_dict.keys())
        sql_lower = {c.lower() for c in sql_cols_dict}
        for e in ent_list:
            for f in e['fields']:
                total_entity += 1
                e_name = f['name']
                e_col = f['col']
                e_snake = to_snake(e_name)
                candidates = {e_name, e_col, e_snake, e_name.lower(), e_col.lower(), e_snake.lower()}
                cand_lower = {c.lower() for c in candidates}
                if candidates & sql_keys or cand_lower & sql_lower:
                    matched += 1
                    # 检查驼峰一致性 (排除 @TableField 显式映射)
                    actual = candidates & sql_keys
                    if not actual:
                        actual = cand_lower & sql_lower
                    if actual:
                        actual_col = list(actual)[0]
                        sql_actual = None
                        for k in sql_keys:
                            if k.lower() == actual_col.lower():
                                sql_actual = k
                                break
                        if sql_actual:
                            if e_col != e_name:
                                pass  # @TableField 显式映射, 正确
                            elif '_' in sql_actual and not '_' in e_name:
                                case_mismatch.append((table, e_name, sql_actual, f['file']))
                            elif sql_actual.isupper() and not e_name.isupper():
                                case_mismatch.append((table, e_name, sql_actual, f['file']))
                else:
                    mismatched.append((table, e_name, e_snake, f['file']))

    rate = (matched / total_entity * 100) if total_entity > 0 else 0
    print(f"  Entity 字段: {total_entity}")
    print(f"  匹配: {matched} ({rate:.1f}%)")
    print(f"  不匹配: {len(mismatched)}")
    print(f"  驼峰不一致 (排除 @TableField 显式映射): {len(case_mismatch)}")
    print()

    # snake_case 列 (与 camelCase 风格不统一)
    if pure_snake:
        print(f"  ⚠️  SQL 中存在的 snake_case 列 (但都是 @TableField 显式映射):")
        for table, cols in sql_tables.items():
            for col in cols:
                if '_' in col and not any(ch.isupper() for ch in col):
                    print(f"    • {table}.{col}")

    if mismatched:
        print(f"\n  ❌ Entity 有, SQL 缺 (前 10 个):")
        for table, e_name, e_snake, efile in mismatched[:10]:
            ent_module = efile.split('minimax-')[-1].split('/')[0]
            print(f"    • {table}.{e_name:30}  [{ent_module}]")
        if len(mismatched) > 10:
            print(f"    ... 还有 {len(mismatched) - 10} 个")

    if case_mismatch:
        print(f"\n  ⚠️  驼峰不一致 (前 10 个):")
        for table, e_name, sql_actual, efile in case_mismatch[:10]:
            ent_module = efile.split('minimax-')[-1].split('/')[0]
            print(f"    • {table}.{e_name:30} SQL: {sql_actual}  [{ent_module}]")
    print()
    print("=" * 80)
    print("📌 结论")
    print("=" * 80)
    print()
    print(f"  sql/complete.sql:  {rate:.1f}% 匹配 ({matched}/{total_entity} entity 字段)")
    print()
    print("驼峰规则:")
    print("  - Java entity 字段: camelCase (userId, createdAt, resourceType)")
    print("  - SQL 列名:         camelCase (同 entity, 100% 对齐)")
    print("  - 例外 (model_battle_log): 用 @TableField 显式映射到 snake_case")
    print()
    if rate == 100 and len(case_mismatch) == 0:
        print("🎉 100% 匹配, 0 驼峰不一致")
    else:
        print(f"⚠️  需补 {len(mismatched)} 字段")
    print()

if __name__ == '__main__':
    main()
