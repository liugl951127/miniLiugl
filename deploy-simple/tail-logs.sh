#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - 一键日志查看 (V2.2)
#
# 智能日志:
#   - tail-logs.sh           - 所有服务 (混合 + 高亮)
#   - tail-logs.sh gateway   - 单服务跟踪
#   - tail-logs.sh error     - 只看 ERROR
#   - tail-logs.sh slow      - 只看慢请求 (>1s)
#
# 用法:
#   sudo ./deploy-simple/tail-logs.sh [服务名|error|slow]
# =============================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; CYAN='\033[0;36m'; NC='\033[0m'

# 找项目根目录
PROJECT_ROOT=""
for p in /opt/miniLiugl /root/miniLiugl "$(pwd)"; do
  if [ -f "$p/docker-compose.yml" ] && grep -q "minimax-gateway" "$p/docker-compose.yml" 2>/dev/null; then
    PROJECT_ROOT="$p"
    break
  fi
done

if [ -z "$PROJECT_ROOT" ]; then
  echo "❌ 找不到 miniLiugl 项目目录"
  exit 1
fi

cd "$PROJECT_ROOT"

FILTER="${1:-all}"

# 颜色映射函数
colorize() {
  # 给 docker compose logs 输出染色
  # 输入: docker compose logs 输出
  # 染色: 容器名 / ERROR / WARN / 慢请求 / 高亮
  sed -E \
    -e "s|^(minimax-[a-z]+)|\033[1;36m\1\033[0m|" \
    -e "s|(\bERROR\b|\bException\b|\bFailed\b)|\033[1;31m\1\033[0m|g" \
    -e "s|(\bWARN\b|\bWARNING\b)|\033[1;33m\1\033[0m|g" \
    -e "s|(\bINFO\b)|\033[0;32m\1\033[0m|g" \
    -e "s|(\[([0-9]+)ms\])|$(echo -e '\033[1;31m[\2ms]\033[0m')|g"
}

case "$FILTER" in
  all)
    echo -e "${CYAN}📜 跟踪所有服务日志 (Ctrl+C 退出)${NC}"
    docker compose logs -f --tail=50 2>&1 | colorize
    ;;

  error|err)
    echo -e "${RED}🚨 只看 ERROR 日志 (Ctrl+C 退出)${NC}"
    docker compose logs -f --tail=500 2>&1 | grep -E "ERROR|Exception|Failed|Exception|Caused by" | colorize
    ;;

  slow)
    echo -e "${YELLOW}🐌 只看慢请求 (>1s, Ctrl+C 退出)${NC}"
    docker compose logs -f --tail=500 2>&1 | grep -E "\[[0-9]{4,}ms\]" | colorize
    ;;

  gateway|auth|chat|memory|model|rag|function|multimodal|monitor|agent|prompt|analytics|pipeline|ws|admin|nginx|mysql|redis|nacos|otel)
    echo -e "${CYAN}📜 跟踪 $FILTER (Ctrl+C 退出)${NC}"
    docker compose logs -f --tail=100 "$FILTER" 2>&1 | colorize
    ;;

  *)
    # 模糊匹配
    if docker compose ps --services 2>/dev/null | grep -q "^$FILTER$"; then
      docker compose logs -f --tail=100 "$FILTER" 2>&1 | colorize
    else
      echo "用法: $0 [服务名|all|error|slow]"
      echo ""
      echo "可用服务:"
      docker compose ps --services 2>/dev/null | sed 's/^/  /'
      echo ""
      echo "特殊模式:"
      echo "  all    - 全部服务 (默认)"
      echo "  error  - 只看 ERROR"
      echo "  slow   - 只看慢请求"
    fi
    ;;
esac