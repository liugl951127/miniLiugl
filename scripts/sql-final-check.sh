#!/bin/bash
# V6.7+ SQL 完整检查
set -e
SQL=${1:-sql/minimax-mysql-final.sql}

ERRORS=0
echo "========================================="
echo "SQL 完整检查 (V6.7+ MariaDB 严格模式)"
echo "========================================="

# 1. 跑 Python 详细检查
python3 scripts/sql-validate.py "$SQL" 2>&1 | tail -20
ERRORS=${PIPESTATUS[0]}

# 2. 跑 MariaDB 严格模式检查
echo ""
python3 scripts/sql-mariadb-check.py "$SQL" 2>&1 | tail -10
ERRORS=$((ERRORS + ${PIPESTATUS[0]}))

# 3. 总结
echo ""
echo "========================================="
if [ $ERRORS -eq 0 ]; then
    echo "✓ SQL 检查全部通过"
    echo ""
    echo "数据库对象统计:"
    echo "  表:     $(grep -c 'CREATE TABLE' $SQL)"
    echo "  索引:   $(grep -c 'CREATE INDEX' $SQL)"
    echo "  触发器: $(grep -c 'CREATE TRIGGER' $SQL)"
    echo "  过程:   $(grep -c 'CREATE PROCEDURE' $SQL)"
    echo "  函数:   $(grep -c 'CREATE FUNCTION' $SQL)"
    echo "  INSERT: $(grep -c 'INSERT IGNORE' $SQL)"
    exit 0
else
    echo "✗ 发现 $ERRORS 个问题"
    exit 1
fi
