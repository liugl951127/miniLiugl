#!/usr/bin/env bash
# ============================================================
# MiniMax Platform 一键部署脚本 (V3.5.3+)
# 用法: ./deploy.sh [up|down|build|status|logs|restart]
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

COMPOSE_FILE="docker-compose.yml"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()   { echo -e "${GREEN}✓ $1${NC}"; }
warn() { echo -e "${YELLOW}⚠ $1${NC}"; }
fail() { echo -e "${RED}✗ $1${NC}"; exit 1; }

# 检查 docker
if ! command -v docker >/dev/null 2>&1; then
    fail "docker 未安装, 请先安装 Docker 20+"
fi
if ! command -v docker compose >/dev/null 2>&1; then
    fail "docker compose 未安装, 请先安装 docker compose v2"
fi

# 检查 init.sql
if [ ! -f "sql/init.sql" ]; then
    fail "sql/init.sql 不存在"
fi
TABLES=$(grep -c "CREATE TABLE" sql/init.sql || true)
INSERTS=$(grep -c "INSERT INTO" sql/init.sql || true)
ok "sql/init.sql: ${TABLES} 表 / ${INSERTS} 种子"

CMD="${1:-up}"
case "$CMD" in
    up)
        echo -e "${YELLOW}━━━ 构建镜像 (首次 5-10 分钟) ━━━${NC}"
        docker compose -f "$COMPOSE_FILE" build
        ok "build 完成"

        echo -e "${YELLOW}━━━ 启动服务 ━━━${NC}"
        docker compose -f "$COMPOSE_FILE" up -d
        ok "已启动"

        echo
        echo "等待基础设施就绪 (30s)..."
        sleep 30

        echo -e "${YELLOW}━━━ 服务状态 ━━━${NC}"
        docker compose -f "$COMPOSE_FILE" ps
        ;;

    down)
        echo -e "${YELLOW}━━━ 停止服务 ━━━${NC}"
        docker compose -f "$COMPOSE_FILE" down
        ok "已停止"
        ;;

    build)
        echo -e "${YELLOW}━━━ 构建所有镜像 ━━━${NC}"
        docker compose -f "$COMPOSE_FILE" build --no-cache
        ok "build 完成"
        ;;

    status)
        echo -e "${YELLOW}━━━ 服务状态 ━━━${NC}"
        docker compose -f "$COMPOSE_FILE" ps
        echo
        echo -e "${YELLOW}━━━ 端口检查 ━━━${NC}"
        for port in 80 3306 6379 7080 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090 8091 8092 8093 8094 8095 8848; do
            if curl -s -m 2 -o /dev/null -w "%{http_code}" "http://localhost:$port/" | grep -qE "^[12345]"; then
                ok "port $port"
            else
                echo "  port $port: 未响应"
            fi
        done
        ;;

    logs)
        SERVICE="${2:-}"
        if [ -z "$SERVICE" ]; then
            docker compose -f "$COMPOSE_FILE" logs -f --tail 100
        else
            docker compose -f "$COMPOSE_FILE" logs -f --tail 100 "$SERVICE"
        fi
        ;;

    restart)
        SERVICE="${2:-}"
        if [ -z "$SERVICE" ]; then
            docker compose -f "$COMPOSE_FILE" restart
        else
            docker compose -f "$COMPOSE_FILE" restart "$SERVICE"
        fi
        ;;

    *)
        echo "用法: $0 [up|down|build|status|logs|restart]"
        echo "  up      启动所有服务"
        echo "  down    停止所有服务"
        echo "  build   重新构建镜像"
        echo "  status  查看状态 + 端口检查"
        echo "  logs    查看日志 (可指定 service)"
        echo "  restart 重启 (可指定 service)"
        exit 1
        ;;
esac
