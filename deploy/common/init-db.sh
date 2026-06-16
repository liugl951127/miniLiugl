#!/bin/bash
# ============================================================
#  MiniMax Platform - 数据库初始化
#  适用: 任意已运行的 MySQL 8.0+ 实例
#  作者: Mavis  日期: 2026-06-16
# ============================================================
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 解析参数
HOST="${MYSQL_HOST:-localhost}"
PORT="${MYSQL_PORT:-3306}"
USER="${MYSQL_USER:-root}"
PASS="${MYSQL_PASSWORD:-MinMax2026!}"
DB="${MYSQL_DATABASE:-minimax}"
CONTAINER="${MYSQL_CONTAINER:-minimax-mysql}"  # Docker 容器名 (二选一)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SQL_DIR="$PROJECT_ROOT/sql/init"

echo -e "${YELLOW}============================================================${NC}"
echo -e "${YELLOW}  MiniMax Platform 数据库初始化${NC}"
echo -e "${YELLOW}  Host: $HOST:$PORT  DB: $DB${NC}"
echo -e "${YELLOW}============================================================${NC}"
echo

# 决定连接方式: 优先直连, 失败用 docker exec
USE_DOCKER=false
if ! command -v mysql &> /dev/null; then
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}✗ 需要 mysql client 或 docker${NC}"
        exit 1
    fi
    if ! docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
        echo -e "${RED}✗ MySQL 容器 $CONTAINER 不存在${NC}"
        exit 1
    fi
    USE_DOCKER=true
fi

if [ "$USE_DOCKER" = true ]; then
    echo -e "${YELLOW}使用 Docker 模式: $CONTAINER${NC}"
    mysql_cmd() {
        docker exec -i "$CONTAINER" mysql -uroot -p"$PASS" "$@"
    }
else
    echo -e "${YELLOW}使用直连模式: $HOST:$PORT${NC}"
    mysql_cmd() {
        mysql -h"$HOST" -P"$PORT" -u"$USER" -p"$PASS" "$@"
    }
fi

# ---------- 1) 创建数据库 ----------
echo -e "${YELLOW}[1/3] 创建数据库 $DB ...${NC}"
mysql_cmd -e "CREATE DATABASE IF NOT EXISTS \`$DB\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null \
    && echo -e "${GREEN}✓${NC}" \
    || echo -e "${YELLOW}! 已存在或权限不足, 继续${NC}"
echo

# ---------- 2) 执行 SQL 文件 ----------
echo -e "${YELLOW}[2/3] 执行 SQL 脚本...${NC}"
COUNT=0
for sql_file in "$SQL_DIR"/*.sql; do
    if [ -f "$sql_file" ]; then
        echo "  $(basename $sql_file) ..."
        if mysql_cmd "$DB" < "$sql_file" 2>&1 | grep -v "Using a password" | head -3; then
            echo -e "    ${GREEN}✓${NC}"
            COUNT=$((COUNT + 1))
        else
            echo -e "    ${YELLOW}! 部分失败, 继续${NC}"
        fi
    fi
done
echo
echo -e "${GREEN}✓ 已执行 $COUNT 个 SQL 脚本${NC}"
echo

# ---------- 3) 验证 ----------
echo -e "${YELLOW}[3/3] 验证表结构...${NC}"
mysql_cmd -e "USE \`$DB\`; SHOW TABLES;" 2>/dev/null
echo

echo -e "${GREEN}============================================================${NC}"
echo -e "${GREEN}  数据库初始化完成！${NC}"
echo -e "${GREEN}============================================================${NC}"
echo
echo "默认管理员:"
echo "  用户名: admin"
echo "  密码:   admin@123"
echo "  (启动时由 AdminDataInitializer 用 BCrypt 编码)"
echo
