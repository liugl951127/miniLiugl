#!/usr/bin/env bash
# ============================================================
# MiniMax Platform — E2E 健康检查脚本 (Day 20)
# 检查所有微服务的 /actuator/health 和关键 API 端点
# 成功标准: 所有服务 UP
# ============================================================

set -e
cd "$(dirname "$0")/.."

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PASS=0
FAIL=0
TOTAL=0

# 默认端口配置 (可通过环境变量覆盖)
GATEWAY_PORT="${GATEWAY_PORT:-3000}"
AUTH_PORT="${AUTH_PORT:-8081}"
CHAT_PORT="${CHAT_PORT:-8082}"
MODEL_PORT="${MODEL_PORT:-8083}"
MEMORY_PORT="${MEMORY_PORT:-8084}"
RAG_PORT="${RAG_PORT:-8085}"
FUNCTION_PORT="${FUNCTION_PORT:-8086}"
AGENT_PORT="${AGENT_PORT:-8090}"
MONITOR_PORT="${MONITOR_PORT:-8092}"
ADMIN_PORT="${ADMIN_PORT:-8093}"
ANALYTICS_PORT="${ANALYTICS_PORT:-8094}"
PROMPT_PORT="${PROMPT_PORT:-8091}"
WS_PORT="${WS_PORT:-8095}"

BASE_URL="${BASE_URL:-http://localhost}"

pass() { echo -e "${GREEN}[PASS]${NC} $1"; PASS=$((PASS+1)); TOTAL=$((TOTAL+1)); }
fail() { echo -e "${RED}[FAIL]${NC} $1"; FAIL=$((FAIL+1)); TOTAL=$((TOTAL+1)); }
info() { echo -e "${BLUE}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

# HTTP GET 检查
check_get() {
  local name="$1"; local url="$2"; local expect="$3"
  TOTAL=$((TOTAL+1))
  local resp
  resp=$(curl -sf --max-time 5 "$url" 2>/dev/null) || true
  if [ -z "$resp" ]; then
    fail "$name → $url [连接失败]"
    return 1
  fi
  if [ -n "$expect" ] && ! echo "$resp" | grep -q "$expect"; then
    fail "$name → $url [期望: $expect]"
    return 1
  fi
  pass "$name → $url"
  return 0
}

# 带 Bearer Token 检查
check_auth() {
  local name="$1"; local url="$2"; local token="$3"
  TOTAL=$((TOTAL+1))
  local resp
  resp=$(curl -sf --max-time 5 -H "Authorization: Bearer $token" "$url" 2>/dev/null) || true
  if [ -z "$resp" ]; then
    fail "$name [需要认证]"
    return 1
  fi
  pass "$name → $url"
  return 0
}

echo "============================================"
echo " MiniMax Platform — E2E Health Check (Day 20)"
echo " 时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "============================================"
echo ""

# 1. Gateway
info "1. Gateway ($GATEWAY_PORT)"
check_get "Gateway /health"     "$BASE_URL:$GATEWAY_PORT/health"       "UP"
check_get "Gateway /api/v1/health" "$BASE_URL:$GATEWAY_PORT/api/v1/health"
check_get "Gateway /actuator/prometheus" "$BASE_URL:$GATEWAY_PORT/actuator/prometheus" "process_"

# 2. Auth
info "2. Auth ($AUTH_PORT)"
check_get "Auth /actuator/health" "$BASE_URL:$AUTH_PORT/actuator/health" "UP"
check_get "Auth /api/v1/health"   "$BASE_URL:$AUTH_PORT/api/v1/health"

# 3. Chat
info "3. Chat ($CHAT_PORT)"
check_get "Chat /actuator/health" "$BASE_URL:$CHAT_PORT/actuator/health" "UP"
check_get "Chat /api/v1/health"   "$BASE_URL:$CHAT_PORT/api/v1/health"

# 4. Model
info "4. Model ($MODEL_PORT)"
check_get "Model /actuator/health" "$BASE_URL:$MODEL_PORT/actuator/health" "UP"
check_get "Model /api/v1/models/health" "$BASE_URL:$MODEL_PORT/api/v1/models/health"

# 5. Memory
info "5. Memory ($MEMORY_PORT)"
check_get "Memory /actuator/health" "$BASE_URL:$MEMORY_PORT/actuator/health" "UP"
check_get "Memory /api/v1/health"   "$BASE_URL:$MEMORY_PORT/api/v1/health"

# 6. RAG
info "6. RAG ($RAG_PORT)"
check_get "RAG /actuator/health" "$BASE_URL:$RAG_PORT/actuator/health" "UP"
check_get "RAG /api/v1/health"   "$BASE_URL:$RAG_PORT/api/v1/health"

# 7. Function
info "7. Function ($FUNCTION_PORT)"
check_get "Function /actuator/health" "$BASE_URL:$FUNCTION_PORT/actuator/health" "UP"
check_get "Function /api/v1/health"   "$BASE_URL:$FUNCTION_PORT/api/v1/health"

# 8. Agent (optional)
info "8. Agent ($AGENT_PORT)"
if curl -sf --max-time 3 "$BASE_URL:$AGENT_PORT/actuator/health" > /dev/null 2>&1; then
  check_get "Agent /actuator/health" "$BASE_URL:$AGENT_PORT/actuator/health" "UP"
else
  warn "Agent ($AGENT_PORT) 未启动，跳过"
fi

# 9. Monitor
info "9. Monitor ($MONITOR_PORT)"
check_get "Monitor /actuator/health" "$BASE_URL:$MONITOR_PORT/actuator/health" "UP"
check_get "Monitor /api/v1/health"   "$BASE_URL:$MONITOR_PORT/api/v1/health"

# 10. Admin
info "10. Admin ($ADMIN_PORT)"
check_get "Admin /actuator/health" "$BASE_URL:$ADMIN_PORT/actuator/health" "UP"
check_get "Admin /admin/health"    "$BASE_URL:$ADMIN_PORT/admin/health"

# 11. Analytics
info "11. Analytics ($ANALYTICS_PORT)"
check_get "Analytics /actuator/health" "$BASE_URL:$ANALYTICS_PORT/actuator/health" "UP"
check_get "Analytics /api/v1/health"   "$BASE_URL:$ANALYTICS_PORT/api/v1/health"

# 12. Prompt
info "12. Prompt ($PROMPT_PORT)"
check_get "Prompt /actuator/health" "$BASE_URL:$PROMPT_PORT/actuator/health" "UP"
check_get "Prompt /api/v1/health"   "$BASE_URL:$PROMPT_PORT/api/v1/health"

# 13. WebSocket (TCP 检查)
info "13. WebSocket ($WS_PORT)"
TOTAL=$((TOTAL+1))
if nc -zw3 localhost $WS_PORT 2>/dev/null; then
  pass "WebSocket 端口 $WS_PORT 开放"
else
  warn "WebSocket 端口 $WS_PORT 未开放 (可能未启动)"
fi

echo ""
echo "============================================"
echo " 结果: $PASS 通过 / $FAIL 失败 / 共 $TOTAL 项"
echo "============================================"

if [ "$FAIL" -gt 0 ]; then
  echo -e "${RED}E2E 检查未全部通过！${NC}"
  exit 1
else
  echo -e "${GREEN}E2E 全部健康！${NC}"
  exit 0
fi
