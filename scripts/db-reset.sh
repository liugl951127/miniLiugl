#!/bin/bash
# V6.3+ 表结构变更脚本
# 1. 删原表 (DROP)
# 2. 重建表 (CREATE)
# 3. 灌种子数据 (INSERT)
# 4. 验证字段一致性 (Entity ↔ Schema)

set -e
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT="$(dirname "$SCRIPT_DIR")"
cd "$ROOT"

MODULE=${1:-all}
DB_HOST=${DB_HOST:-127.0.0.1}
DB_PORT=${DB_PORT:-3306}
DB_USER=${DB_USER:-root}
DB_PASS=${DB_PASS:-minimax_mariadb_2024}
DB_NAME=${DB_NAME:-minimax_platform}

echo "========================================="
echo "MiniMax DB 重置 (模块: $MODULE)"
echo "========================================="
echo "DB: $DB_HOST:$DB_PORT/$DB_NAME"
echo

# 1. 找 schema 文件
SCHEMA_FILES=$(find backend -name "schema-*.sql" 2>/dev/null)
if [ -z "$SCHEMA_FILES" ]; then
    echo "❌ 没找到 schema-*.sql 文件"
    exit 1
fi

# 2. 按模块处理
for schema in $SCHEMA_FILES; do
    mod_name=$(basename "$schema" | sed 's/schema-//;s/\.sql//')
    if [ "$MODULE" != "all" ] && [ "$MODULE" != "$mod_name" ]; then
        continue
    fi
    echo "📦 处理模块: $mod_name"
    
    # 提取表名
    TABLES=$(grep -oE "CREATE TABLE (\`[^`]+\`)" "$schema" | sed 's/CREATE TABLE `//;s/`$//')
    for tbl in $TABLES; do
        echo "  🗑️  DROP TABLE IF EXISTS \`$tbl\`"
        mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME -e "DROP TABLE IF EXISTS \`$tbl\`" 2>/dev/null || true
    done
    
    # 3. 灌 schema
    echo "  📥 灌 schema..."
    mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME < "$schema" 2>/dev/null
    
    # 4. 灌种子数据
    seed="backend/minimax-common/src/main/resources/data/seed-${mod_name}.sql"
    if [ -f "$seed" ]; then
        echo "  🌱 灌种子..."
        mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME < "$seed" 2>/dev/null
    fi
    
    # 5. 验证一致性
    echo "  ✅ 验证字段一致性..."
    ./scripts/check-entity-schema.sh "$mod_name"
done

echo
echo "✅ DB 重置完成"
