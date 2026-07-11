#!/usr/bin/env bash
# ============================================================
# AI 独立运行模式 (V2.8.3)
# 用法: ./scripts/ai-standalone.sh
# ============================================================
# 不依赖 Nacos / 其他微服务
# 直连 MariaDB + Redis, 端口 8094

set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)

DB_HOST=${DB_HOST:-127.0.0.1}
DB_PORT=${DB_PORT:-3306}
DB_NAME=${DB_NAME:-minimax}
DB_USER=${DB_USER:-root}
DB_PASS=${DB_PASS:-root123456}
REDIS_HOST=${REDIS_HOST:-127.0.0.1}
REDIS_PORT=${REDIS_PORT:-6379}
REDIS_PASS=${REDIS_PASS:-minimax_redis_2024}
SERVER_PORT=${SERVER_PORT:-8094}

export DB_HOST DB_PORT DB_NAME DB_USER DB_PASS
export REDIS_HOST REDIS_PORT REDIS_PASS
export SERVER_PORT

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  MiniMax AI Standalone (V2.8.3)"
echo "  DB: $DB_HOST:$DB_PORT/$DB_NAME"
echo "  Redis: $REDIS_HOST:$REDIS_PORT"
echo "  Port: $SERVER_PORT"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

JAR="$ROOT/backend/minimax-ai/target/minimax-ai.jar"
if [ ! -f "$JAR" ]; then
    echo "❌ $JAR 不存在, 先编译: cd $ROOT/backend && mvn package -pl minimax-ai -am -DskipTests"
    exit 1
fi

exec java \
    -Xms256m -Xmx1024m \
    -XX:+UseG1GC -XX:MaxRAMPercentage=70.0 \
    -Dspring.profiles.active=standalone \
    -Dfile.encoding=UTF-8 \
    -jar "$JAR" \
    com.minimax.ai.StandaloneApplication
