#!/bin/bash
# V6.3+ Entity ↔ Schema 字段一致性检查 (V2 完整版)
# 支持单文件 complete.sql + per-module schema-*.sql

set -e
MODULE=${1:-all}
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT="$(dirname "$SCRIPT_DIR")"
cd "$ROOT"

echo "========================================="
echo "Entity ↔ Schema 一致性检查 (V6.3+)"
echo "模块: $MODULE"
echo "========================================="
echo

# 找 schemas (单文件)
SCHEMA_FILES=$(find backend -name "complete*.sql" -not -path "*/target/*" 2>/dev/null)
# 也找 per-module
[ -z "$SCHEMA_FILES" ] && SCHEMA_FILES=$(find backend -name "schema-*.sql" -not -path "*/target/*" 2>/dev/null)

if [ -z "$SCHEMA_FILES" ]; then
    echo "❌ 没找到 schema 文件"
    exit 1
fi

TOTAL_ERRORS=0
TOTAL_TABLES=0
TOTAL_ENTITIES=0

for schema in $SCHEMA_FILES; do
    echo "📄 Schema: $schema"
    echo "========================================="
    
    # 找所有 Entity
    ENTITIES=$(find backend -name "*Entity.java" -path "*/main/*" -not -name "BaseEntity.java" 2>/dev/null)
    
    for entity in $ENTITIES; do
        entity_name=$(basename "$entity" .java)
        # 表名 (默认: 去掉 Entity + snake_case)
        table_name=$(echo "$entity_name" | sed 's/Entity$//' | sed -E 's/([A-Z])/_\L\1/g' | sed 's/^_//')
        # 也支持 @TableName 注解
        ann_table=$(grep -oE '@TableName\(["\x27]([^"\x27]+)["\x27]' "$entity" 2>/dev/null | head -1 | sed -E 's/@TableName\(["\x27]([^"\x27]+)["\x27]/\1/')
        [ -n "$ann_table" ] && table_name="$ann_table"
        
        # 检查表是否在 schema 中
        if ! grep -q "CREATE TABLE.*\`$table_name\`" "$schema" 2>/dev/null; then
            continue
        fi
        
        TOTAL_ENTITIES=$((TOTAL_ENTITIES + 1))
        
        # 提取 entity 字段
        ENTITY_FIELDS=$(grep -oE "private \w+ \w+;" "$entity" | awk '{print $3}' | tr -d ';' | grep -v "serialVersionUID" | sort -u)
        
        # 从 schema 提取该表的列
        COLUMNS=$(awk "/CREATE TABLE \`$table_name\`/,/^\) ENGINE/" "$schema" | \
            grep -oE '^\s*`[a-zA-Z_]+`' | tr -d ' `' | sort -u)
        
        # 跳过 id (实体里叫 id, 列也叫 id, 走默认)
        # 实体字段 → snake_case
        ENTITY_SNAKE=$(echo "$ENTITY_FIELDS" | sed -E 's/([A-Z])/_\L\1/g' | sed 's/^_//')
        
        # 不在 schema 的实体字段
        MISSING=$(comm -23 <(echo "$ENTITY_SNAKE") <(echo "$COLUMNS") | grep -v '^$' | grep -v '^id$')
        # 不在实体的 schema 字段 (通常是 create_time/update_time)
        EXTRA=$(comm -13 <(echo "$ENTITY_SNAKE") <(echo "$COLUMNS") | grep -v '^$' | grep -v -E "^(create_time|update_time|create_by|update_by|deleted|version|tenant_id)$")
        
        if [ -n "$MISSING" ]; then
            echo "  ❌ $table_name (Entity: $entity_name): 缺字段"
            echo "$MISSING" | sed 's/^/    - /'
            TOTAL_ERRORS=$((TOTAL_ERRORS + 1))
        else
            ENTITY_COUNT=$(echo "$ENTITY_FIELDS" | wc -l)
            SCHEMA_COUNT=$(echo "$COLUMNS" | wc -l)
            echo "  ✓ $table_name: $ENTITY_COUNT 字段 ↔ $SCHEMA_COUNT 列"
            TOTAL_TABLES=$((TOTAL_TABLES + 1))
        fi
    done
done

echo
echo "========================================="
echo "总结"
echo "========================================="
echo "  Entity 类: $TOTAL_ENTITIES"
echo "  一致: $TOTAL_TABLES"
echo "  不一致: $TOTAL_ERRORS"
echo
[ $TOTAL_ERRORS -eq 0 ]
