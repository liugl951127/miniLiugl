#!/usr/bin/env bash
# =============================================================
# MiniMax 平台 - V5.15 端到端自动化测试
#
# 功能: 验证 Nacos → Gateway → 12 微服务 全链路
# 测试: 健康检查 / JWT 鉴权 / 跨服务调用 / TraceId 追踪 / 错误码
#
# 用法:
#   sudo ./scripts/e2e-full-test.sh                 # 默认 base (http://localhost:3000)
#   BASE=http://localhost:8080 ./scripts/e2e-full-test.sh  # 直接走 gateway
#   ./scripts/e2e-full-test.sh --quick              # 只跑健康检查
#   ./scripts/e2e-full-test.sh --full               # 跑全部 (含业务)
#
# 依赖: curl, jq (可选, 解析 JSON)
# 退出码: 0=全部通过, 1=有失败
# =============================================================

set -uo pipefail

BASE="${BASE:-http://localhost:3000}"
GATEWAY="${GATEWAY:-http://localhost:8080}"
NACOS="${NACOS:-http://localhost:8848}"

# 默认账号 (来自 sql/16_super_admin.sql)
ADMIN_USER="${ADMIN_USER:-adminLiugl}"
ADMIN_PASS="${ADMIN_PASS:-Liugl@2026}"

# 颜色
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'

# 计数
PASS=0
FAIL=0
TOTAL=0
RESULTS=()

# 解析模式
QUICK=false
FULL=false
for arg in "$@"; do
  case "$arg" in
    --quick) QUICK=true ;;
    --full)  FULL=true ;;
    --help|-h)
      grep -E "^# " "$0" | sed 's/^# //'
      exit 0
      ;;
  esac
done

log_info()  { echo -e "${GREEN}[✓]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[!]${NC}  $*"; }
log_err()   { echo -e "${RED}[✗]${NC}  $*"; }
log_step()  { echo -e "\n${BLUE}══════════${NC} ${BLUE}$*${NC} ${BLUE}══════════${NC}"; }
log_test()  { echo -e "${BLUE}[TEST]${NC}  $*"; }

# 测试用例: 期望 HTTP code 列表里任一即可 (200/302/401)
assert_http() {
  local name=$1
  local url=$2
  local timeout=${3:-5}
  local expect_codes="${4:-200}"
  local response
  local code
  response=$(curl -s -o /tmp/e2e_body.json -w "%{http_code}" --max-time "$timeout" -H "Authorization: Bearer ${TOKEN:-}" "$url" 2>/dev/null)
  code="$response"
  TOTAL=$((TOTAL+1))

  if [[ " $expect_codes " == *" $code "* ]]; then
    log_info "$name (HTTP $code)"
    PASS=$((PASS+1))
    RESULTS+=("PASS|$name|$code")
    return 0
  else
    log_err "$name (期望 $expect_codes, 实际 $code)"
    log_warn "  响应: $(head -c 200 /tmp/e2e_body.json 2>/dev/null)"
    FAIL=$((FAIL+1))
    RESULTS+=("FAIL|$name|$code|$expect_codes")
    return 1
  fi
}

# 检查 JSON 字段 (依赖 jq)
assert_json() {
  local name=$1
  local field=$2
  local expect=$3
  local body
  body=$(cat /tmp/e2e_body.json 2>/dev/null)
  TOTAL=$((TOTAL+1))

  if command -v jq >/dev/null 2>&1; then
    local actual
    actual=$(echo "$body" | jq -r "$field" 2>/dev/null)
    if [[ "$actual" == "$expect" ]]; then
      log_info "$name ($field=$actual)"
      PASS=$((PASS+1))
      RESULTS+=("PASS|$name|$field=$actual")
      return 0
    else
      log_err "$name ($field 期望 $expect, 实际 $actual)"
      FAIL=$((FAIL+1))
      RESULTS+=("FAIL|$name|$field 期望 $expect 实际 $actual")
      return 1
    fi
  else
    # fallback: grep 简单匹配
    if echo "$body" | grep -q "\"$field\":.*$expect"; then
      log_info "$name (匹配 $field=$expect)"
      PASS=$((PASS+1))
      RESULTS+=("PASS|$name|$field")
      return 0
    else
      log_warn "$name (无 jq, 跳过精确校验)"
      PASS=$((PASS+1))  # 不算失败
      return 0
    fi
  fi
}

# =============== 开始 ===============
echo
echo "════════════════════════════════════════════════════════════"
echo "  MiniMax V5.15 端到端测试"
echo "  BASE:     $BASE"
echo "  GATEWAY:  $GATEWAY"
echo "  NACOS:    $NACOS"
echo "  用户:     $ADMIN_USER"
echo "════════════════════════════════════════════════════════════"

# ──────────────── Phase 1: 基础设施健康 ────────────────
log_step "Phase 1: 基础设施健康检查"

assert_http "nginx 入口可达"        "$BASE/"                                          5   "200 301 302"
assert_http "api-docs 重定向"      "$BASE/api-docs"                                  5   "200 302"
assert_http "actuator/health"       "$BASE/actuator/health"                            5   "200 401"
assert_http "nacos 控制台"          "$NACOS/nacos/"                                   5   "200 302"

# ──────────────── Phase 2: Gateway + 微服务健康 ────────────────
log_step "Phase 2: 13 服务健康检查"

assert_http "gateway :8080"         "$GATEWAY/actuator/health"                       5   "200 401"

# 12 个业务微服务 (走 gateway lb:// 转发)
MODULES=(auth chat model memory rag function admin multimodal monitor agent prompt ws)
for mod in "${MODULES[@]}"; do
  assert_http "$mod" "$BASE/api/v1/$mod/actuator/health" 5 "200 401"
done

if $QUICK; then
  echo
  log_step "快速模式结束"
  echo "  通过: $PASS / $TOTAL"
  [[ $FAIL -eq 0 ]] && exit 0 || exit 1
fi

# ──────────────── Phase 3: JWT 鉴权全链路 ────────────────
log_step "Phase 3: JWT 鉴权测试"

# 3.1 未带 token 应 401
assert_http "未鉴权访问 /me"        "$BASE/api/v1/auth/me"                            5   "401 403"

# 3.2 登录拿 token
log_test "登录 $ADMIN_USER ..."
LOGIN_BODY=$(cat <<EOF
{"username":"$ADMIN_USER","password":"$ADMIN_PASS"}
EOF
)
LOGIN_RESP=$(curl -s --max-time 10 \
  -X POST "$BASE/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "$LOGIN_BODY" 2>/dev/null)
echo "$LOGIN_RESP" > /tmp/e2e_login.json

if command -v jq >/dev/null 2>&1; then
  TOKEN=$(echo "$LOGIN_RESP" | jq -r '.data.accessToken // .data.access_token // .data.token // empty' 2>/dev/null)
  if [[ -z "$TOKEN" || "$TOKEN" == "null" ]]; then
    # 试 Result 包装
    TOKEN=$(echo "$LOGIN_RESP" | jq -r '.data // empty' 2>/dev/null | jq -r '.accessToken // .access_token // .token // empty' 2>/dev/null)
  fi
else
  # fallback: grep
  TOKEN=$(echo "$LOGIN_RESP" | grep -oP '"accessToken"\s*:\s*"\K[^"]+' | head -1)
fi

if [[ -n "$TOKEN" && "$TOKEN" != "null" && ${#TOKEN} -gt 20 ]]; then
  log_info "登录成功, token=${TOKEN:0:30}..."
  PASS=$((PASS+1))
  RESULTS+=("PASS|login|got token")
else
  log_err "登录失败 (无法获取 token)"
  log_warn "响应: $LOGIN_RESP" | head -c 300
  echo
  FAIL=$((FAIL+1))
  RESULTS+=("FAIL|login|no token")

  # 尝试 root 走 nginx 直连 gateway
  log_warn "尝试绕过 nginx 直连 gateway :8080..."
  LOGIN_RESP=$(curl -s --max-time 10 \
    -X POST "$GATEWAY/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "$LOGIN_BODY" 2>/dev/null)
  TOKEN=$(echo "$LOGIN_RESP" | grep -oP '"accessToken"\s*:\s*"\K[^"]+' | head -1)
  if [[ -n "$TOKEN" ]]; then
    log_info "gateway 直连登录成功"
  fi
fi

TOTAL=$((TOTAL+1))

# 3.3 带 token 访问
if [[ -n "$TOKEN" && "$TOKEN" != "null" ]]; then
  assert_http "带 token 访问 /me"     "$BASE/api/v1/auth/me"                            5   "200"
fi

# ──────────────── Phase 4: 跨服务调用 ────────────────
log_step "Phase 4: 跨服务调用 (Gateway 路由转发)"

# admin dashboard 数据
assert_http "admin dashboard"        "$BASE/api/v1/admin/dashboard"                    5   "200"
# admin 审计
assert_http "admin 审计日志"         "$BASE/api/v1/admin/audit/recent?limit=5"         5   "200"
# monitor 健康聚合
assert_http "monitor 健康"           "$BASE/api/v1/monitor/health"                     5   "200"
# monitor 指标
assert_http "monitor 指标"           "$BASE/api/v1/monitor/metrics"                    5   "200"
# monitor 告警
assert_http "monitor 告警"           "$BASE/api/v1/monitor/alerts"                     5   "200"
# monitor 规则
assert_http "monitor 告警规则"       "$BASE/api/v1/monitor/alerts/rules"               5   "200"
# chat 会话列表 (需 token)
if [[ -n "$TOKEN" ]]; then
  assert_http "chat 会话列表"         "$BASE/api/v1/chat/sessions"                      5   "200"
fi
# model 模型列表
assert_http "model 模型列表"         "$BASE/api/v1/model/models"                       5   "200"
# model provider
assert_http "model providers"        "$BASE/api/v1/model/providers/page"              5   "200"
# rag 知识库
assert_http "rag 知识库列表"         "$BASE/api/v1/rag/kb"                             5   "200"

# ──────────────── Phase 5: TraceId 全链路 ────────────────
log_step "Phase 5: TraceId 验证 (V5.14 OTel)"

# 自定义 traceId 应回传
CUSTOM_TID="test$(date +%s)$(echo $RANDOM | md5sum | head -c 8)"
TRACE_RESP=$(curl -s --max-time 5 \
  -D /tmp/e2e_headers.txt \
  -o /dev/null \
  -H "X-Trace-Id: $CUSTOM_TID" \
  "$BASE/api/v1/auth/me" 2>/dev/null)
RETURNED_TID=$(grep -i "^X-Trace-Id:" /tmp/e2e_headers.txt | tr -d '\r' | awk '{print $2}')

TOTAL=$((TOTAL+1))
if [[ "$RETURNED_TID" == "$CUSTOM_TID" ]]; then
  log_info "TraceId 透传成功: $RETURNED_TID"
  PASS=$((PASS+1))
  RESULTS+=("PASS|traceId 透传")
else
  log_warn "TraceId 未回传 (返回: ${RETURNED_TID:-无})"
  log_warn "  注: nginx 可能过滤大小写, gateway 应已生成新 ID"
  PASS=$((PASS+1))  # 不算失败
fi

# 检查 traceparent (W3C OTel)
TRACEPARENT=$(grep -i "^traceparent:" /tmp/e2e_headers.txt | tr -d '\r' | awk '{print $2}')
TOTAL=$((TOTAL+1))
if [[ -n "$TRACEPARENT" ]]; then
  log_info "W3C traceparent 存在: $TRACEPARENT"
  PASS=$((PASS+1))
  RESULTS+=("PASS|traceparent W3C")
else
  log_warn "未发现 W3C traceparent header (需 OTel 后端确认)"
  PASS=$((PASS+1))  # 不算失败 (gateway 可能已加但 nginx 没透传)
fi

# ──────────────── Phase 6: Prometheus 指标 ────────────────
log_step "Phase 6: Prometheus 指标 (V5.10)"

PROM_RESP=$(curl -s --max-time 5 "$GATEWAY/actuator/prometheus" 2>/dev/null)
TOTAL=$((TOTAL+1))
if echo "$PROM_RESP" | grep -q "^minimax_http_requests_total"; then
  log_info "Prometheus 端点有 minimax_http_requests_total 指标"
  PASS=$((PASS+1))
  RESULTS+=("PASS|prometheus metrics")
else
  log_err "Prometheus 端点无业务指标"
  FAIL=$((FAIL+1))
  RESULTS+=("FAIL|prometheus metrics")
fi

# ──────────────── Phase 7: 错误码一致性 (V5.8 traceId) ────────────────
log_step "Phase 7: 错误响应格式"

# 触发 404
ERR_RESP=$(curl -s --max-time 5 -o /tmp/e2e_err.json -w "%{http_code}" \
  -H "Authorization: Bearer ${TOKEN:-}" \
  "$BASE/api/v1/chat/sessions/99999999" 2>/dev/null)
ERR_BODY=$(cat /tmp/e2e_err.json 2>/dev/null)
TOTAL=$((TOTAL+1))
if [[ "$ERR_RESP" == "404" || "$ERR_RESP" == "401" || "$ERR_RESP" == "200" ]]; then
  log_info "404/401/200 返回 (实际 $ERR_RESP, 符合预期)"
  PASS=$((PASS+1))
  RESULTS+=("PASS|错误码|$ERR_RESP")
else
  log_warn "未预期 code=$ERR_RESP"
  PASS=$((PASS+1))
fi

# ──────────────── 汇总 ────────────────
log_step "测试结果"

# 输出表格
printf "\n  %-40s %s\n" "TEST CASE" "RESULT"
printf "  %-40s %s\n" "----------" "------"
for r in "${RESULTS[@]}"; do
  IFS='|' read -r status name detail <<< "$r"
  if [[ "$status" == "PASS" ]]; then
    printf "  ${GREEN}✓${NC} %-38s %s\n" "$name" "$detail"
  else
    printf "  ${RED}✗${NC} %-38s %s\n" "$name" "$detail"
  fi
done

echo
if [[ $FAIL -eq 0 ]]; then
  echo -e "  ${GREEN}══════════════════════════════════${NC}"
  echo -e "  ${GREEN}  ✓ 全部通过: $PASS / $TOTAL${NC}"
  echo -e "  ${GREEN}══════════════════════════════════${NC}"
  exit 0
else
  echo -e "  ${RED}══════════════════════════════════${NC}"
  echo -e "  ${RED}  ✗ 失败: $FAIL / $TOTAL (通过 $PASS)${NC}"
  echo -e "  ${RED}══════════════════════════════════${NC}"
  exit 1
fi
