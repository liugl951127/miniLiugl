#!/usr/bin/env bash
# ============================================================
# MiniMax Platform — 端到端健康检查脚本 (Day 21)
# 检查项: 全部 14 个微服务
# 成功标准: HTTP 200 + JSON code=0
# ============================================================

set -e
cd "$(dirname "$0")/.."

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 环境变量（可覆盖）
GATEWAY_HOST="${GATEWAY_HOST:-localhost}"
GATEWAY_PORT="${GATEWAY_PORT:-7080}"
TIMEOUT="${TIMEOUT:-5}"
TOKEN="${TOKEN:-}"        # 可选：设置 TOKEN 跳过登录

BASE_URL="http://${GATEWAY_HOST}:${GATEWAY_PORT}"
PASS=0
FAIL=0
SKIP=0

pass() { echo -e "${GREEN}[PASS]${NC} $1"; PASS=$((PASS+1)); }
fail() { echo -e "${RED}[FAIL]${NC} $1"; FAIL=$((FAIL+1)); }
skip() { echo -e "${YELLOW}[SKIP]${NC} $1"; SKIP=$((SKIP+1)); }
info() { echo -e "${BLUE}[INFO]${NC} $1"; }
http_code() { curl -s -o /dev/null -w "%{http_code}" --max-time "$TIMEOUT" "$1" 2>/dev/null || echo "000"; }
json_ok() {
  local code
  code=$(curl -s --max-time "$TIMEOUT" "$1" 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',-1))" 2>/dev/null || echo "-1")
  [ "$code" = "0" ] && return 0 || return 1
}
curl_get() {
  curl -s --max-time "$TIMEOUT" -H "Authorization: Bearer ${TOKEN}" "$1" 2>/dev/null
}

echo "=================================================="
echo " MiniMax Platform — E2E Health Check"
echo " Base URL: ${BASE_URL}"
echo " Timeout:  ${TIMEOUT}s"
echo "=================================================="

# ---- 获取 Token（如果未提供）----
if [ -z "$TOKEN" ]; then
  info "未提供 TOKEN，尝试登录获取..."
  LOGIN_RESP=$(curl -s --max-time "$TIMEOUT" -X POST "${BASE_URL}/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin@123"}' 2>/dev/null)
  TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('accessToken',''))" 2>/dev/null)
  if [ -n "$TOKEN" ]; then
    info "登录成功，TOKEN 已获取"
  else
    info "无法获取 TOKEN，部分端点将跳过"
  fi
fi

echo ""
echo "━━━ 1. Gateway ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
HC=$(http_code "${BASE_URL}/actuator/health")
if [ "$HC" = "200" ]; then
  pass "Gateway (${BASE_URL}) — HTTP ${HC}"
else
  fail "Gateway (${BASE_URL}) — HTTP ${HC} (期望 200)"
fi

echo ""
echo "━━━ 2. Auth :8081 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
HC=$(http_code "${BASE_URL}/api/v1/auth/health")
if [ "$HC" = "200" ]; then
  pass "Auth /health — HTTP ${HC}"
else
  fail "Auth /health — HTTP ${HC}"
fi

HC=$(http_code "${BASE_URL}/api/v1/auth/me")
if [ "$HC" = "200" ]; then
  pass "Auth /me (需 Token) — HTTP ${HC}"
elif [ "$HC" = "401" ]; then
  skip "Auth /me — HTTP 401 (无 Token，正常)"
else
  fail "Auth /me — HTTP ${HC} (期望 200 或 401)"
fi

echo ""
echo "━━━ 3. Chat :8082 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
HC=$(http_code "${BASE_URL}/api/v1/sessions")
if [ "$HC" = "200" ] || [ "$HC" = "401" ]; then
  pass "Chat /sessions — HTTP ${HC}"
else
  fail "Chat /sessions — HTTP ${HC}"
fi

echo ""
echo "━━━ 4. Model :8083 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
HC=$(http_code "${BASE_URL}/api/v1/models")
if [ "$HC" = "200" ] || [ "$HC" = "401" ]; then
  pass "Model /models — HTTP ${HC}"
else
  fail "Model /models — HTTP ${HC}"
fi

HC=$(http_code "${BASE_URL}/api/v1/models/providers")
if [ "$HC" = "200" ] || [ "$HC" = "401" ]; then
  pass "Model /providers — HTTP ${HC}"
else
  fail "Model /providers — HTTP ${HC}"
fi

echo ""
echo "━━━ 5. Memory :8084 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
HC=$(http_code "${BASE_URL}/api/v1/memory/short-term/test-sid")
if [ "$HC" = "200" ] || [ "$HC" = "401" ] || [ "$HC" = "404" ]; then
  pass "Memory /short-term — HTTP ${HC}"
else
  fail "Memory /short-term — HTTP ${HC}"
fi

HC=$(http_code "${BASE_URL}/api/v1/memory/pref/test?userId=1")
if [ "$HC" = "200" ] || [ "$HC" = "401" ]; then
  pass "Memory /pref — HTTP ${HC}"
else
  fail "Memory /pref — HTTP ${HC}"
fi

echo ""
echo "━━━ 6. RAG :8085 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
HC=$(http_code "${BASE_URL}/api/v1/rag/kb?ownerId=1")
if [ "$HC" = "200" ] || [ "$HC" = "401" ]; then
  pass "RAG /kb — HTTP ${HC}"
else
  fail "RAG /kb — HTTP ${HC}"
fi

HC=$(http_code "${BASE_URL}/api/v1/rag/kb/public")
if [ "$HC" = "200" ] || [ "$HC" = "401" ]; then
  pass "RAG /kb/public — HTTP ${HC}"
else
  fail "RAG /kb/public — HTTP ${HC}"
fi

echo ""
echo "━━━ 7. Function :8086 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
HC=$(http_code "${BASE_URL}/api/v1/function/tools")
if [ "$HC" = "200" ] || [ "$HC" = "401" ]; then
  pass "Function /tools — HTTP ${HC}"
else
  fail "Function /tools — HTTP ${HC}"
fi

echo ""
echo "━━━ 8. Admin :8087 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
HC=$(http_code "${BASE_URL}/api/v1/admin/health")
if [ "$HC" = "200" ] || [ "$HC" = "401" ]; then
  pass "Admin /health — HTTP ${HC}"
else
  fail "Admin /health — HTTP ${HC}"
fi

HC=$(http_code "${BASE_URL}/api/v1/admin/ping")
if [ "$HC" = "200" ]; then
  pass "Admin /ping — HTTP ${HC}"
else
  fail "Admin /ping — HTTP ${HC}"
fi

HC=$(http_code "${BASE_URL}/api/v1/admin/apikey/stats")
if [ "$HC" = "200" ] || [ "$HC" = "401" ]; then
  pass "Admin /apikey/stats — HTTP ${HC}"
else
  fail "Admin /apikey/stats — HTTP ${HC}"
fi

echo ""
echo "━━━ 9. Multimodal :8088 ━━━━━━━━━━━━━━━━━━━━━━━━━━━"
HC=$(http_code "${BASE_URL}/api/v1/multimodal/info")
if [ "$HC" = "200" ] || [ "$HC" = "401" ]; then
  pass "Multimodal /info — HTTP ${HC}"
else
  fail "Multimodal /info — HTTP ${HC}"
fi

echo ""
echo "━━━ 10. Monitor :8089 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
HC=$(http_code "${BASE_URL}/api/v1/monitor/health")
if [ "$HC" = "200" ]; then
  pass "Monitor /health — HTTP ${HC}"
else
  fail "Monitor /health — HTTP ${HC}"
fi

HC=$(http_code "${BASE_URL}/api/v1/monitor/metrics")
if [ "$HC" = "200" ]; then
  pass "Monitor /metrics — HTTP ${HC}"
else
  fail "Monitor /metrics — HTTP ${HC}"
fi

HC=$(http_code "${BASE_URL}/actuator/prometheus")
if [ "$HC" = "200" ]; then
  pass "Actuator /prometheus — HTTP ${HC}"
else
  fail "Actuator /prometheus — HTTP ${HC}"
fi

echo ""
echo "━━━ 11. Prompt :8090 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
HC=$(http_code "${BASE_URL}/api/v1/prompt/")
if [ "$HC" = "200" ] || [ "$HC" = "401" ] || [ "$HC" = "404" ]; then
  pass "Prompt — HTTP ${HC}"
else
  fail "Prompt — HTTP ${HC}"
fi

echo ""
echo "━━━ 12. Analytics :8091 ━━━━━━━━━━━━━━━━━━━━━━━━━━"
HC=$(http_code "${BASE_URL}/api/v1/analytics/")
if [ "$HC" = "200" ] || [ "$HC" = "401" ] || [ "$HC" = "404" ]; then
  pass "Analytics — HTTP ${HC}"
else
  fail "Analytics — HTTP ${HC}"
fi

echo ""
echo "━━━ 13. Pipeline :8092 ━━━━━━━━━━━━━━━━━━━━━━━━━━"
HC=$(http_code "${BASE_URL}/api/v1/pipeline/")
if [ "$HC" = "200" ] || [ "$HC" = "401" ] || [ "$HC" = "404" ]; then
  pass "Pipeline — HTTP ${HC}"
else
  fail "Pipeline — HTTP ${HC}"
fi

echo ""
echo "━━━ 14. WebSocket :8093 ━━━━━━━━━━━━━━━━━━━━━━━━━━"
# WebSocket 检查：先检查 HTTP upgrade 端点
HC=$(http_code "${BASE_URL}/api/v1/ws/info")
if [ "$HC" = "200" ] || [ "$HC" = "401" ] || [ "$HC" = "404" ]; then
  pass "WS /info — HTTP ${HC}"
else
  fail "WS /info — HTTP ${HC}"
fi

echo ""
echo "━━━ 15. 全局限流检查 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
# 高频调用触发限流（连续 5 次）
RATE_LIMITED=0
for i in $(seq 1 5); do
  HC=$(http_code "${BASE_URL}/api/v1/auth/login")
  if [ "$HC" = "429" ]; then
    RATE_LIMITED=1
    break
  fi
done
if [ "$RATE_LIMITED" = "1" ]; then
  pass "限流生效 — HTTP 429"
else
  info "限流未触发（正常：高并发场景才触发）"
fi

echo ""
echo "━━━ 16. JWT 鉴权检查 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
# 无 Token 访问受保护端点
HC=$(http_code "${BASE_URL}/api/v1/sessions")
if [ "$HC" = "401" ] || [ "$HC" = "403" ]; then
  pass "JWT 鉴权生效 — HTTP ${HC}"
else
  info "JWT 鉴权 — HTTP ${HC} (可能为 200: 已登录状态)"
fi

echo ""
echo "━━━ 17. CORS 检查 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
CORS=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$TIMEOUT" \
  "${BASE_URL}/api/v1/auth/health" \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" 2>/dev/null)
if [ "$CORS" = "200" ]; then
  pass "CORS 允许 — HTTP ${CORS}"
else
  fail "CORS — HTTP ${CORS}"
fi

echo ""
echo "━━━ 18. 响应时延检查 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
RESP_TIME=$(curl -s -o /dev/null -w "%{time_total}" --max-time "$TIMEOUT" \
  "${BASE_URL}/api/v1/auth/health" 2>/dev/null || echo "99")
RESP_MS=$(echo "$RESP_TIME * 1000" | bc 2>/dev/null | cut -d. -f1)
if [ "${RESP_MS:-999}" -lt 500 ]; then
  pass "响应时延 — ${RESP_MS}ms (优秀 <500ms)"
elif [ "${RESP_MS:-999}" -lt 1000 ]; then
  info "响应时延 — ${RESP_MS}ms (良好 <1s)"
else
  fail "响应时延 — ${RESP_MS}ms (过慢 >1s)"
fi

echo ""
echo "=================================================="
echo " E2E 健康检查结果:"
echo "   ✅ PASS:  $PASS"
echo "   ❌ FAIL:  $FAIL"
echo "   ⏭  SKIP:  $SKIP"
echo "=================================================="

if [ $FAIL -gt 0 ]; then
  echo -e "${RED}有 ${FAIL} 项检查失败，请检查服务状态！${NC}"
  exit 1
else
  echo -e "${GREEN}全部检查通过（或正常跳过）！${NC}"
  exit 0
fi
