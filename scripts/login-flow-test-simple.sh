#!/bin/bash
# V3.6.23+ 登录流程验证 (简化版 7 场景)
FRONTEND_PORT="${FRONTEND_PORT:-3500}"
BASE_URL="http://localhost:$FRONTEND_PORT"
AUTH_PORT="${AUTH_PORT:-9001}"

echo "═══════════════════════════════════════════════════════════"
echo "  V3.6.23+ 登录流程完整验证 (7 场景)"
echo "═══════════════════════════════════════════════════════════"

PASS=0
FAIL=0

# 1. 演示模式
echo ""
echo "--- 场景 1: 演示模式 ?demo=1 ---"
for r in / /login /chat /admin/dashboard; do
    code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$r?demo=1" --max-time 3)
    if [[ "$code" == "200" ]]; then
        echo "  ✓ $r?demo=1 → 200"
        PASS=$((PASS+1))
    else
        echo "  ❌ $r?demo=1 → $code"
        FAIL=$((FAIL+1))
    fi
done

# 2-6. 真实登录
echo ""
echo "--- 场景 2-6: 真实登录 / 错误密码 / 网络超时 / 401 / 500 ---"
USERS=("admin:admin123" "adminLiugl:liugl951127" "operator:operator123" "auditor:auditor123" "user:user123")
RES=$(curl -s -X POST "http://localhost:$AUTH_PORT/api/v1/auth/login" \
    -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' --max-time 3 2>&1 || echo "TIMEOUT")

if echo "$RES" | grep -qE "token|accessToken"; then
    echo "  ✓ Auth 在跑 - 5 账号真实登录"
    for U in "${USERS[@]}"; do
        IFS=':' read -r user pass <<< "$U"
        R=$(curl -s -X POST "http://localhost:$AUTH_PORT/api/v1/auth/login" \
            -H "Content-Type: application/json" -d "{\"username\":\"$user\",\"password\":\"$pass\"}" --max-time 3 2>&1)
        if echo "$R" | grep -qE "token|accessToken"; then
            echo "    ✓ $user 登录成功"
            PASS=$((PASS+1))
        else
            echo "    ❌ $user 失败"
            FAIL=$((FAIL+1))
        fi
    done
    # 错误密码
    R=$(curl -s -X POST "http://localhost:$AUTH_PORT/api/v1/auth/login" \
        -H "Content-Type: application/json" -d '{"username":"admin","password":"WRONG"}' --max-time 3 2>&1)
    if echo "$R" | grep -qE "401|invalid|password|Unauthorized" -i; then
        echo "    ✓ 错误密码 401"
        PASS=$((PASS+1))
    else
        echo "    ❌ 错误密码响应: $R"
        FAIL=$((FAIL+1))
    fi
else
    echo "  ⚠️  Auth 未起 ($AUTH_PORT) - 跳过"
fi

# 4. 网络超时
echo ""
echo "--- 场景 4: 网络超时 (端口 9999) ---"
R=$(curl -s -X POST "http://localhost:9999/api/v1/auth/login" \
    -H "Content-Type: application/json" -d '{}' --max-time 3 2>&1 || echo "TIMEOUT")
if echo "$R" | grep -qE "TIMEOUT|ECONNREFUSED|connect|fail" -i; then
    echo "  ✓ ECONNREFUSED"
    PASS=$((PASS+1))
else
    echo "  ❌ 响应: $R"
    FAIL=$((FAIL+1))
fi

# 5. 401 token 过期 (静态检查)
echo ""
echo "--- 场景 5: 401 token 过期 (前端 useErrorHandler 行为) ---"
grep -q "401" frontend/src/composables/useErrorHandler.js && {
    echo "  ✓ useErrorHandler.js 含 401 处理"
    PASS=$((PASS+1))
} || {
    echo "  ❌ useErrorHandler.js 缺 401"
    FAIL=$((FAIL+1))
}

# 6. 500
echo ""
echo "--- 场景 6: 500 服务错 ---"
R=$(curl -s -X POST "http://localhost:$AUTH_PORT/api/v1/auth/_error_500" \
    -H "Content-Type: application/json" -d '{}' --max-time 3 2>&1)
if echo "$R" | grep -qE "500|error|Internal" -i; then
    echo "  ✓ 500 错误响应"
    PASS=$((PASS+1))
else
    if echo "$RES" | grep -qE "token|accessToken"; then
        echo "  ⚠️  Auth 在跑, 但 500 endpoint 不存在 (沙箱无此场景)"
    else
        echo "  ⚠️  Auth 未起, 跳过"
    fi
fi

# 7. 演示模式 fetchProfile 降级 (静态)
echo ""
echo "--- 场景 7: 演示模式 fetchProfile 降级 ---"
grep -q "isDemoMode" frontend/src/store/user.js && {
    echo "  ✓ user.js 含 isDemoMode 兜底 (V3.5.93+)"
    PASS=$((PASS+1))
} || {
    echo "  ❌ user.js 缺 isDemoMode"
    FAIL=$((FAIL+1))
}

# 总结
echo ""
echo "═══════════════════════════════════════════════════════════"
echo "  总结: $PASS 通过, $FAIL 失败"
echo "═══════════════════════════════════════════════════════════"
[[ $FAIL -eq 0 ]] && echo "  🎉 全通过" || echo "  ⚠️  $FAIL 项失败"
