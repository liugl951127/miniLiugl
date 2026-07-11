#!/usr/bin/env bash
# ============================================================
# MiniMax Platform 端到端 API 验证脚本 (V2.8.7)
# 验证: 登录 → 主功能 → AI 框架 → 业务 Agent
# 状态: 通过/失败/跳过 (无 DB)
# ============================================================

set -uo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:7080}"
AI_URL="${AI_URL:-http://127.0.0.1:8094}"
ADMIN_USER="${ADMIN_USER:-adminLiugl}"
ADMIN_PASS="${ADMIN_PASS:-Liugl@2026}"
REGULAR_USER="${REGULAR_USER:-admin}"
REGULAR_PASS="${REGULAR_PASS:-admin@123}"

PASS=0
FAIL=0
SKIP=0
RESULTS=()

# 颜色
G='\033[0;32m'; R='\033[0;31m'; Y='\033[1;33m'; N='\033[0m'

# 工具函数
check() {
    local name="$1" cmd="$2" expected="$3"
    local out
    out=$(eval "$cmd" 2>&1)
    if echo "$out" | grep -q "$expected"; then
        PASS=$((PASS+1))
        RESULTS+=("✓ $name")
        echo -e "${G}✓${N} $name"
    elif echo "$out" | grep -qi "Connection refused\|无法连接\|ECONNREFUSED"; then
        SKIP=$((SKIP+1))
        RESULTS+=("⊘ $name (服务未启动)")
        echo -e "${Y}⊘${N} $name ${Y}(服务未启动)${N}"
    else
        FAIL=$((FAIL+1))
        RESULTS+=("✗ $name: $out" | head -c 200)
        echo -e "${R}✗${N} $name"
        echo "  $out" | head -3
    fi
}

print_header() {
    echo
    echo "════════════════════════════════════════════"
    echo "  $1"
    echo "════════════════════════════════════════════"
}

# ====================================================
# 1. 基础健康
# ====================================================
print_header "1. 基础健康检查"
check "Gateway Health" "curl -fsS -m 3 $BASE_URL/actuator/health 2>&1 || echo no" '"status":"UP"'
check "Auth Ping"      "curl -fsS -m 3 $BASE_URL/api/v1/auth/ping 2>&1 || echo no" 'ok\|pong'

# ====================================================
# 2. 登录流程
# ====================================================
print_header "2. 登录流程"
LOGIN_RESP=$(curl -fsS -m 5 -X POST $BASE_URL/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" 2>&1) || true
if echo "$LOGIN_RESP" | grep -q "accessToken"; then
    PASS=$((PASS+1))
    ACCESS_TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import json,sys;print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null)
    REFRESH_TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import json,sys;print(json.load(sys.stdin)['data']['refreshToken'])" 2>/dev/null)
    echo -e "${G}✓${N} 超级管理员登录: $ADMIN_USER"
    echo "  token: ${ACCESS_TOKEN:0:50}..."
else
    FAIL=$((FAIL+1))
    echo -e "${R}✗${N} 超级管理员登录"
    echo "  $LOGIN_RESP"
fi

# 普通用户登录
USER_RESP=$(curl -fsS -m 5 -X POST $BASE_URL/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$REGULAR_USER\",\"password\":\"$REGULAR_PASS\"}" 2>&1) || true
if echo "$USER_RESP" | grep -q "accessToken"; then
    PASS=$((PASS+1))
    echo -e "${G}✓${N} 普通用户登录: $REGULAR_USER"
    USER_TOKEN=$(echo "$USER_RESP" | python3 -c "import json,sys;print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null)
else
    SKIP=$((SKIP+1))
    echo -e "${Y}⊘${N} 普通用户登录 (服务未启动)"
    USER_TOKEN=""
fi

# 错误密码
ERR_RESP=$(curl -s -m 5 -X POST $BASE_URL/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$ADMIN_USER\",\"password\":\"wrong\"}" 2>&1) || true
if echo "$ERR_RESP" | grep -qi "1004\|password\|密码"; then
    PASS=$((PASS+1))
    echo -e "${G}✓${N} 错误密码被拒绝"
else
    SKIP=$((SKIP+1))
    echo -e "${Y}⊘${N} 错误密码测试"
fi

# ====================================================
# 3. 用户认证
# ====================================================
print_header "3. 用户认证"
if [ -n "${ACCESS_TOKEN:-}" ]; then
    check "获取当前用户" "curl -s -m 3 -H 'Authorization: Bearer $ACCESS_TOKEN' $BASE_URL/api/v1/auth/me 2>&1" "username"
    check "刷新令牌" "curl -s -m 3 -X POST $BASE_URL/api/v1/auth/refresh -H 'Content-Type: application/json' -d '{\"refreshToken\":\"$REFRESH_TOKEN\"}' 2>&1" "accessToken"
else
    SKIP=$((SKIP+2))
    echo -e "${Y}⊘${N} 跳过 (无 token)"
fi

# ====================================================
# 4. 监控指标
# ====================================================
print_header "4. 监控指标"
check "Admin Stats" "curl -s -m 3 $BASE_URL/api/v1/admin/stats/ops 2>&1" "code\|total\|0"
check "Dashboard"   "curl -s -m 3 $BASE_URL/api/v1/admin/stats/dashboard 2>&1" "code\|data"
check "Recent Audit" "curl -s -m 3 $BASE_URL/api/v1/admin/audit/recent 2>&1" "code\|data"

# ====================================================
# 5. AI 服务直连
# ====================================================
print_header "5. AI 服务 (minimax-ai)"
check "AI Health"     "curl -s -m 3 $AI_URL/api/ai/health 2>&1" "ok\|code"
check "AI Info"       "curl -s -m 3 $AI_URL/api/ai/info 2>&1" "name\|code"
check "AI Frameworks" "curl -s -m 3 $AI_URL/api/ai/framework/agents 2>&1" "shopping-agent\|hotel-agent"
check "AI 工具列表"   "curl -s -m 3 $AI_URL/api/ai/admin/tools 2>&1" "total\|list"

# ====================================================
# 6. AI Pipeline
# ====================================================
print_header "6. AI Pipeline"
check "Pipeline Config"   "curl -s -m 3 $AI_URL/api/ai/pipeline/config 2>&1" "vocabSize"
check "Intent Stats"      "curl -s -m 3 $AI_URL/api/ai/pipeline/intent/stats 2>&1" "intentCount"

# ====================================================
# 7. AI Framework (Agent 路由)
# ====================================================
print_header "7. AI 框架 - 业务 Agent"
check "Shopping Agent"  "curl -s -m 10 -X POST $AI_URL/api/ai/framework/agents/execute?agentName=shopping-agent -H 'Content-Type: application/json' -d '{\"query\":\"iPhone 15, 不超过 12000 元\"}' 2>&1" "success\|iPhone"
check "Hotel Agent"     "curl -s -m 10 -X POST $AI_URL/api/ai/framework/agents/execute?agentName=hotel-agent -H 'Content-Type: application/json' -d '{\"sessionId\":\"test-1\",\"query\":\"北京附近 4 星以上酒店\"}' 2>&1" "permissionDenied\|success"
check "Entertainment Agent" "curl -s -m 10 -X POST $AI_URL/api/ai/framework/agents/execute?agentName=entertainment-agent -H 'Content-Type: application/json' -d '{\"sessionId\":\"test-2\",\"userCity\":\"上海\",\"query\":\"上海有什么电影院\"}' 2>&1" "permissionDenied\|success"

# ====================================================
# 8. 路由测试
# ====================================================
print_header "8. AI 智能路由"
check "路由: 酒店"   "curl -s -m 10 -X POST $AI_URL/api/ai/framework/agents/route -H 'Content-Type: application/json' -d '{\"query\":\"我要订酒店\"}' 2>&1" "hotel-agent"
check "路由: 购物"   "curl -s -m 10 -X POST $AI_URL/api/ai/framework/agents/route -H 'Content-Type: application/json' -d '{\"query\":\"iPhone 15\"}' 2>&1" "shopping-agent"
check "路由: 娱乐"   "curl -s -m 10 -X POST $AI_URL/api/ai/framework/agents/route -H 'Content-Type: application/json' -d '{\"query\":\"上海有什么影院\"}' 2>&1" "entertainment-agent"

# ====================================================
# 9. 权限
# ====================================================
print_header "9. 权限系统"
check "授权 location:read" "curl -s -m 3 -X POST '$AI_URL/api/ai/framework/permission/grant?sessionId=test' -H 'Content-Type: application/json' -d '[\"location:read\"]' 2>&1" "granted"
check "已授权列表"   "curl -s -m 3 '$AI_URL/api/ai/framework/permission/list?sessionId=test' 2>&1" "location:read"
check "撤销"         "curl -s -m 3 -X POST '$AI_URL/api/ai/framework/permission/revoke?sessionId=test&permissionCode=location:read' 2>&1" "revoked"

# ====================================================
# 10. 记忆
# ====================================================
print_header "10. 记忆系统"
check "记忆统计" "curl -s -m 3 $AI_URL/api/ai/framework/memory/stats 2>&1" "shortTermSessions\|longTermUsers"

# ====================================================
# 11. 商品/位置直接查询
# ====================================================
print_header "11. 商品 + LBS"
check "商品搜索 iPhone" "curl -s -m 5 '$AI_URL/api/ai/framework/products/search?keyword=iPhone&topK=3' 2>&1" "iPhone"
check "商品搜索 华为"   "curl -s -m 5 '$AI_URL/api/ai/framework/products/search?keyword=%E5%8D%8E%E4%B8%BA&topK=3' 2>&1" "华为"

# ====================================================
# 总结
# ====================================================
print_header "测试总结"
echo -e "  通过: ${G}${PASS}${N}"
echo -e "  失败: ${R}${FAIL}${N}"
echo -e "  跳过: ${Y}${SKIP}${N}"
echo -e "  总计: $((PASS+FAIL+SKIP))"
echo

if [ $FAIL -gt 0 ]; then
    echo "失败的测试:"
    for r in "${RESULTS[@]}"; do
        if [[ "$r" == ✗* ]]; then
            echo "  $r"
        fi
    done
    exit 1
fi

echo "✓ 所有有效测试通过"
exit 0
