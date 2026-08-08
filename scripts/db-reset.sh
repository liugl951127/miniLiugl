#!/bin/bash
# V6.3+ 表结构重置脚本 (完整版)
# 1. DROP 所有表
# 2. 跑 complete-h2.sql / complete.sql 重建 + 灌种子
# 3. 跑 check-entity-schema 验证一致

set -e
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT="$(dirname "$SCRIPT_DIR")"
cd "$ROOT"

DB_HOST=${DB_HOST:-127.0.0.1}
DB_PORT=${DB_PORT:-3306}
DB_USER=${DB_USER:-root}
DB_PASS=${DB_PASS:-minimax_mariadb_2024}
DB_NAME=${DB_NAME:-minimax_platform}
DB_TYPE=${DB_TYPE:-mysql}  # mysql | h2 | mariadb

echo "========================================="
echo "MiniMax DB 重置 (V6.3+)"
echo "========================================="
echo "DB: $DB_TYPE $DB_HOST:$DB_PORT/$DB_NAME"
echo

# 1. 找 schema
SCHEMA_FILE=$(find backend -name "complete-h2.sql" -not -path "*/target/*" 2>/dev/null | head -1)
[ -z "$SCHEMA_FILE" ] && SCHEMA_FILE=$(find backend -name "complete.sql" -not -path "*/target/*" 2>/dev/null | head -1)

if [ -z "$SCHEMA_FILE" ]; then
    echo "❌ 没找到 complete.sql / complete-h2.sql"
    exit 1
fi

echo "📄 Schema: $SCHEMA_FILE"

# 2. DROP 表
if [ "$DB_TYPE" = "h2" ]; then
    echo "🗑️  H2 模式 - 不删, 直接重建"
else
    echo "🗑️  准备 DROP 表..."
    # 提取所有表名
    TABLES=$(grep -oE "CREATE TABLE \`[^`]+\`" "$SCHEMA_FILE" | sed 's/CREATE TABLE `//;s/`$//' | sort -u)
    for tbl in $TABLES; do
        echo "  DROP TABLE IF EXISTS \`$tbl\`"
        mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME -e "DROP TABLE IF EXISTS \`$tbl\`" 2>/dev/null || \
            echo "    (mysql 不可用, 跳过 - 启动后端时会自动 CREATE)"
    done
fi

# 3. 跑 schema
echo
echo "📥 跑 schema..."
if [ "$DB_TYPE" = "h2" ]; then
    # H2 自动从 classpath 加载
    echo "  H2 模式 - 后端启动时自动跑"
else
    mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME < "$SCHEMA_FILE" 2>/dev/null && \
        echo "  ✓ Schema 执行成功" || \
        echo "  ⚠️  mysql 不可用, 跳过"
fi

# 4. 灌种子 (如果有)
SEED_FILE=$(find backend -name "seed*.sql" -not -path "*/target/*" 2>/dev/null | head -1)
if [ -n "$SEED_FILE" ] && [ "$DB_TYPE" != "h2" ]; then
    echo
    echo "🌱 灌种子: $SEED_FILE"
    mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME < "$SEED_FILE" 2>/dev/null && \
        echo "  ✓ 种子数据成功" || echo "  ⚠️  失败"
fi

# 5. 验证
echo
echo "🔍 验证 Entity-Schema 一致性..."
bash scripts/check-entity-schema.sh

echo
echo "========================================="
echo "✅ DB 重置完成"
echo "========================================="
