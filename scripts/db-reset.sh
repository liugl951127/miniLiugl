#!/bin/bash
# V6.3+ 一键 DB 重置
# 1. DROP 所有 minimax_platform 表
# 2. 跑 sql/minimax-mysql-final.sql (1 个文件包含 DDL + 5 账号 + 96 种子 + 触发器 + 权限)
# 3. 跑 verify-sql.sh 验证

set -e
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

DB_HOST=${DB_HOST:-127.0.0.1}
DB_PORT=${DB_PORT:-3306}
DB_USER=${DB_USER:-root}
DB_PASS=${DB_PASS:-minimax_mariadb_2024}
DB_NAME=${DB_NAME:-minimax_platform}

SQL_FILE="$ROOT/sql/minimax-mysql-final.sql"

echo "========================================="
echo "MiniMax DB 重置 (V6.3+ 单文件部署)"
echo "========================================="
echo "DB: $DB_HOST:$DB_PORT/$DB_NAME"
echo "SQL: $SQL_FILE"
echo ""

if [ ! -f "$SQL_FILE" ]; then
    echo "❌ 找不到 $SQL_FILE"
    exit 1
fi

# 1. DROP 所有表
echo "🗑️  1/3 DROP 所有表..."
TABLES=$(grep -oE "CREATE TABLE \`[a-z_]+\`" "$SQL_FILE" | sed 's/CREATE TABLE `//;s/`$//' | sort -u)
TOTAL=$(echo "$TABLES" | wc -l)
echo "   共 $TOTAL 张表"
for tbl in $TABLES; do
    mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME \
        -e "DROP TABLE IF EXISTS \`$tbl\`" 2>/dev/null || \
        echo "   ⚠️  DROP \`$tbl\` 失败 (mysql 不可用)"
done
echo ""

# 2. 跑 1 个 SQL 文件
echo "📥  2/3 跑 SQL (1 文件 1 步)..."
if mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME < "$SQL_FILE" 2>&1; then
    echo "   ✓ SQL 执行成功"
else
    echo "   ✗ SQL 执行失败 (见上)"
    exit 1
fi
echo ""

# 3. 验证
echo "🔍 3/3 验证..."
if [ -f scripts/verify-sql.sh ]; then
    bash scripts/verify-sql.sh
else
    echo "   ⚠️  scripts/verify-sql.sh 不存在"
fi

echo ""
echo "========================================="
echo "✅ DB 重置完成"
echo "========================================="
echo ""
echo "测试账号 (密码 admin123, 实际需用 BCryptPasswordEncoder 生成):"
echo "  admin   - 系统管理员"
echo "  demo    - 演示账号"
echo "  test    - 测试账号"
echo "  guest   - 访客账号"
echo "  vip     - VIP 账号"
