#!/usr/bin/env bash
# ============================================================
# 表结构重建脚本 (V2.8.2)
# 用法: ./scripts/rebuild-schema.sh
# ============================================================
# ⚠️ 危险: 会删除所有表, 仅用于开发/重置

set -euo pipefail

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-3306}
DB_USER=${DB_USER:-root}
DB_PASS=${DB_PASS:-root123456}
DB_NAME=${DB_NAME:-minimax}

ROOT=$(cd "$(dirname "$0")/.." && pwd)

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

step() { echo -e "\n${YELLOW}━━━ $1 ━━━${NC}"; }
ok() { echo -e "${GREEN}✓ $1${NC}"; }
warn() { echo -e "${YELLOW}⚠ $1${NC}"; }
fail() { echo -e "${RED}✗ $1${NC}"; exit 1; }

# 1. 确认操作
step "Pre-check"
read -p "这将删除并重建所有表, 确定吗? (yes/no): " confirm
if [ "$confirm" != "yes" ]; then
    fail "用户取消"
fi

# 2. 测试连接
step "Test connection"
if ! mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS -e "SELECT 1" 2>/dev/null; then
    fail "无法连接 MySQL"
fi
ok "连接成功"

# 3. 重建数据库
step "Drop & create database"
mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS -e "DROP DATABASE IF EXISTS $DB_NAME; CREATE DATABASE $DB_NAME DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;"
ok "数据库 $DB_NAME 已重建"

# 4. 导入 DDL
step "Import schema"
mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME < "$ROOT/sql/schema-v2.8.2.sql"
ok "DDL 导入完成"

# 5. 导入种子数据
step "Import seed data"
mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME < "$ROOT/sql/seed-v2.8.2.sql"
ok "种子数据导入完成"

# 6. 统计
step "Statistics"
TABLE_COUNT=$(mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$DB_NAME'")
USER_COUNT=$(mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME -N -e "SELECT COUNT(*) FROM sys_user")
TOOL_COUNT=$(mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME -N -e "SELECT COUNT(*) FROM ai_tool")

ok "重建完成"
echo "  - 数据库: $DB_NAME"
echo "  - 表数量: $TABLE_COUNT"
echo "  - 用户数: $USER_COUNT"
echo "  - AI 工具: $TOOL_COUNT"
echo ""
warn "请重启应用服务: docker compose restart"
