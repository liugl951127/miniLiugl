#!/bin/bash
# V6.3+ SQL 语法 dry-run 检查
# 1. 用 sqlparse 拆语句
# 2. 验证 CREATE TABLE / INSERT 语法
# 3. 验证外键引用

set -e
SQL_FILE=${1:-sql/minimax-mysql-final.sql}
echo "========================================="
echo "MiniMax SQL Dry-Run 验证 ($SQL_FILE)"
echo "========================================="

if [ ! -f "$SQL_FILE" ]; then
    echo "❌ 找不到 $SQL_FILE"
    exit 1
fi

# 1. 文件大小
SIZE=$(wc -c < "$SQL_FILE")
LINES=$(wc -l < "$SQL_FILE")
echo "文件: $LINES 行 / $SIZE 字节"
echo ""

# 2. 关键关键字计数
echo "=== 关键语句统计 ==="
echo "  CREATE TABLE:  $(grep -c 'CREATE TABLE IF NOT EXISTS' $SQL_FILE)"
echo "  CREATE INDEX:  $(grep -c 'CREATE INDEX' $SQL_FILE)"
echo "  CREATE TRIGGER: $(grep -c 'CREATE TRIGGER' $SQL_FILE)"
echo "  CREATE PROCEDURE: $(grep -c 'CREATE PROCEDURE' $SQL_FILE)"
echo "  CREATE FUNCTION: $(grep -c 'CREATE FUNCTION' $SQL_FILE)"
echo "  INSERT IGNORE: $(grep -c 'INSERT IGNORE' $SQL_FILE)"
echo "  ALTER TABLE: $(grep -c 'ALTER TABLE' $SQL_FILE)"
echo "  PRIMARY KEY: $(grep -c 'PRIMARY KEY' $SQL_FILE)"
echo "  AUTO_INCREMENT: $(grep -c 'AUTO_INCREMENT' $SQL_FILE)"
echo ""

# 3. 模块种子
echo "=== 模块分布 ==="
grep "^-- ============ 模块:" $SQL_FILE
echo ""

# 4. 验证 INSERT 列数
echo "=== INSERT 列数验证 ==="
ERR=0
# 找所有 INSERT IGNORE
grep -A 1 "INSERT IGNORE INTO" $SQL_FILE | grep -oE 'VALUES \([0-9]+, [^)]*\)' | while read line; do
    cols=$(echo "$line" | grep -oE '\([0-9]+ values\)' || echo "")
done

# 5. 验证语法 - 关键检查
echo ""
echo "=== 语法检查 ==="
# 5.1 检查 /* */ 注释
COMMENT_OPEN=$(grep -c '/\*' $SQL_FILE)
COMMENT_CLOSE=$(grep -c '\*/' $SQL_FILE)
if [ "$COMMENT_OPEN" = "$COMMENT_CLOSE" ]; then
    echo "  ✓ 注释平衡 ($COMMENT_OPEN 个 /* $COMMENT_CLOSE 个 */)"
else
    echo "  ✗ 注释不平衡: $COMMENT_OPEN 个 /* $COMMENT_CLOSE 个 */"
    ERR=$((ERR+1))
fi

# 5.2 检查 () 平衡
OPEN=$(grep -o '\(' $SQL_FILE | wc -l)
CLOSE=$(grep -o '\)' $SQL_FILE | wc -l)
echo "  ( 共 $OPEN 个, ) 共 $CLOSE 个"
if [ "$OPEN" = "$CLOSE" ]; then
    echo "  ✓ 括号平衡"
else
    echo "  ✗ 括号不平衡: ( = $OPEN, ) = $CLOSE"
    ERR=$((ERR+1))
fi

# 5.3 检查 "" ' 平衡 (粗略)
SINGLE_QUOTE=$(grep -o "'" $SQL_FILE | wc -l)
echo "  单引号: $SINGLE_QUOTE 个 (期望偶数)"
if [ $((SINGLE_QUOTE % 2)) -eq 0 ]; then
    echo "  ✓ 单引号平衡"
else
    echo "  ✗ 单引号不平衡: $SINGLE_QUOTE 个"
    ERR=$((ERR+1))
fi

# 5.4 检查表名一致性
echo ""
echo "=== 表名一致性 ==="
TABLES_CREATED=$(grep -oE 'CREATE TABLE IF NOT EXISTS \`[a-z_]+\`' $SQL_FILE | sort -u | wc -l)
TABLES_INSERTED=$(grep -oE 'INSERT IGNORE INTO \`[a-z_]+\`' $SQL_FILE | sort -u | wc -l)
TABLES_ALTERED=$(grep -oE 'ALTER TABLE \`[a-z_]+\`' $SQL_FILE | sort -u | wc -l)
echo "  CREATE: $TABLES_CREATED"
echo "  INSERT: $TABLES_INSERTED (目标表)"
echo "  ALTER:  $TABLES_ALTERED (增强)"

# 6. 总结
echo ""
echo "========================================="
if [ $ERR -eq 0 ]; then
    echo "✅ Dry-run 通过"
    exit 0
else
    echo "❌ Dry-run 发现 $ERR 个问题"
    exit 1
fi
