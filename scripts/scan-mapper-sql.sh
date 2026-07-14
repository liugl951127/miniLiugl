#!/usr/bin/env bash
# 扫所有 mapper, 找 SQL 字面量里的 camelCase 列名
# 输出可能跟 snake_case SQL 不一致的位置

green() { echo -e "\033[32m$*\033[0m"; }
red()   { echo -e "\033[31m$*\033[0m"; }
yellow(){ echo -e "\033[33m$*\033[0m"; }
bold()  { echo -e "\033[1m$*\033[0m"; }

bold ""
bold "🔍 扫所有 mapper 手写 SQL 找 camelCase 列名"
bold ""

python3 << 'PYEOF'
import re, os, glob

# 1. 读所有 entity 字段 (camelCase)
entity_fields = {}  # module -> set(fields)
for path in glob.glob('backend/minimax-*/src/main/java/com/minimax/**/entity/*.java'):
    module = path.split('/minimax-')[1].split('/')[0]
    fields = set()
    for m in re.finditer(r'private\s+\w+[\w<>,\s\[\]]*?\s+(\w+)\s*[;=]', open(path).read()):
        f = m.group(1)
        if f not in ('serialVersionUID', 'log', 'logger'):
            fields.add(f)
    entity_fields[module] = fields

# 2. 读所有 mapper, 找 SQL 字面量
def to_snake(name):
    s = re.sub(r'([A-Z]+)([A-Z][a-z])', r'\1_\2', name)
    s = re.sub(r'([a-z\d])([A-Z])', r'\1_\2', s)
    return s.lower()

issues = 0
for path in glob.glob('backend/minimax-*/src/main/java/com/minimax/**/mapper/*.java'):
    c = open(path).read()
    module = path.split('/minimax-')[1].split('/')[0]
    fields = entity_fields.get(module, set())

    # 找 @Select("..."), @Update("...") 等
    for m in re.finditer(r'@(Select|Update|Insert|Delete)\(\s*"([^"]+)"', c):
        sql = m.group(2)
        # 跳 SELECT * 
        if 'SELECT *' in sql.upper() and 'WHERE' not in sql.upper():
            continue
        # 找 camelCase 单词 (字段)
        for col in re.findall(r'\b([a-z]+(?:[A-Z][a-z]+)+[A-Z]?)\b', sql):
            # 单个单词字段 (lastHeartbeat, isRegex) 跳过
            if col in ('Active', 'DESC', 'ASC', 'NULL', 'NOW', 'TRUE', 'FALSE'):
                continue
            # 如果 SQL 同时有 snake 版本, 说明不一致
            snake = to_snake(col)
            if snake != col.lower() and snake in sql.lower():
                # SQL 也有 snake 版, 警告
                continue
            # 报告
            rel_path = path.replace('backend/', '')
            line = c[:m.start()].count('\n') + 1
            print(f"  ⚠  {rel_path}:{line} 字段 '{col}' 在 SQL 中 (entity: camelCase, SQL 列: snake_case)")
            print(f"     SQL: {sql[:100]}...")
            issues += 1
            if issues > 30:
                print("  ... 太多, 停止")
                break
    if issues > 30:
        break

print()
print(f"总问题: {issues}")
PYEOF
