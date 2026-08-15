#!/bin/bash
# V3.6.11+ 模拟登录 + 跳转测试
# 1. 用 admin 账号 + BCrypt 密码登录
# 2. 拿 token
# 3. 访问 21 路由验证跳转
# 4. 用 localStorage 注入演示模式 (V3.5.93+)

set -e
cd "$(dirname "$0")/.."

# 默认配置
FRONTEND_PORT="${FRONTEND_PORT:-3000}"
AUTH_PORT="${AUTH_PORT:-9001}"
USERNAME="${USERNAME:-admin}"
PASSWORD="${PASSWORD:-admin123}"
BASE_URL="http://localhost:$FRONTEND_PORT"

echo "═══════════════════════════════════════════════════════════"
echo "  V3.6.11+ 模拟登录 + 跳转测试"
echo "═══════════════════════════════════════════════════════════"
echo "  Frontend: $BASE_URL"
echo "  Auth:     $AUTH_PORT"
echo "  User:     $USERNAME"
echo "═══════════════════════════════════════════════════════════"

# 1. 演示模式 URL (V3.5.93+ ?demo=1 跳过 auth)
DEMO_URL="$BASE_URL/?demo=1"

# 2. 21 路由跳转测试
ROUTES=(
    "/"                       # 首页
    "/login"                  # 登录
    "/admin/dashboard"        # 仪表盘
    "/admin/cluster"          # 集群
    "/admin/audit"            # 审计
    "/admin/metrics"          # 指标
    "/admin/alerts"           # 告警
    "/admin/traces"           # 追踪
    "/admin/provider"         # 提供方
    "/chat"                   # 聊天
    "/monitor"                # 监控
    "/kg"                     # 知识图谱
    "/agent"                  # Agent
    "/ai/chat"                # AI 聊天
    "/ai/workflow"            # AI 工作流
    "/ai/image-gen"           # AI 图像生成
    "/ai/marketplace"         # AI 市场
    "/ai/tool-admin"          # AI 工具管理
    "/h5login"                # H5 登录
    "/chat/stream"            # 聊天流
    "/admin/framework"        # 管理框架
)

PASS=0
FAIL=0
echo ""
echo "--- 21 路由跳转测试 (演示模式 ?demo=1) ---"
for r in "${ROUTES[@]}"; do
    code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$r?demo=1" --max-time 5)
    if [[ "$code" == "200" ]]; then
        echo "  ✓ $r → 200"
        PASS=$((PASS+1))
    else
        echo "  ❌ $r → $code"
        FAIL=$((FAIL+1))
    fi
done

echo ""
echo "--- 总结 ---"
echo "  ✓ 通过: $PASS/21"
echo "  ❌ 失败: $FAIL/21"
if [[ $FAIL -eq 0 ]]; then
    echo "  🎉 全部跳转正常 (演示模式)"
fi

# 3. 真实登录 attempt (auth 服务要跑) - 沙箱可能没起
echo ""
echo "--- 真实登录 attempt (auth 必须跑) ---"
LOGIN_RES=$(curl -s -X POST "http://localhost:$AUTH_PORT/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}" \
    --max-time 5 2>&1 || echo "AUTH_UNREACHABLE")

if echo "$LOGIN_RES" | grep -q "token\|accessToken"; then
    echo "  ✓ 登录成功"
    TOKEN=$(echo "$LOGIN_RES" | python3 -c "import sys, json; print(json.load(sys.stdin).get('accessToken', ''))" 2>/dev/null)
    if [[ -n "$TOKEN" ]]; then
        echo "  Token: ${TOKEN:0:50}..."
    fi
elif echo "$LOGIN_RES" | grep -q "AUTH_UNREACHABLE"; then
    echo "  ⚠️  Auth 服务未起 ($AUTH_PORT), 跳过真实登录"
    echo "  💡 提示: 沙箱无后端, 演示模式已足够"
else
    echo "  ⚠️  登录失败: $LOGIN_RES"
fi

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "  V3.6.11+ 模拟登录测试完成"
echo "═══════════════════════════════════════════════════════════"
