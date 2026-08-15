#!/bin/bash
# V3.6.11+ 模拟登录完整流程
# 1. 用 5 测试账号生成 mock JWT
# 2. 模拟 localStorage 注入 (返回 set-cookies.js 模板)
# 3. 验证带 token 访问 21 路由

set -e
cd "$(dirname "$0")/.."

# 5 测试账号 (V3.5.5+ BCrypt + JWT)
USERS=(
  "admin:admin123:SUPER_ADMIN"
  "adminLiugl:liugl951127:SUPER_ADMIN"
  "operator:operator123:OPERATOR"
  "auditor:auditor123:AUDITOR"
  "user:user123:USER"
)

# 选一个
USER="${1:-admin}"
PASS="${2:-admin123}"
ROLE="${3:-SUPER_ADMIN}"

echo "═══════════════════════════════════════════════════════════"
echo "  V3.6.11+ 模拟 JWT 登录 (5 账号可选)"
echo "═══════════════════════════════════════════════════════════"
echo "  User: $USER / $ROLE"
echo ""

# 1. 找 5 账号
for U in "${USERS[@]}"; do
  IFS=':' read -r name pass role <<< "$U"
  if [[ "$name" == "$USER" ]]; then
    PASS="$pass"
    ROLE="$role"
    break
  fi
done

# 2. 生成 mock JWT (沙箱友好, 不验签)
# JWT = base64(header) . base64(payload) . signature
HEADER='{"alg":"HS256","typ":"JWT"}'
PAYLOAD="{\"sub\":\"$USER\",\"username\":\"$USER\",\"roles\":[\"$ROLE\"],\"iat\":$(date +%s),\"exp\":$(($(date +%s) + 3600))}"

b64() {
  echo -n "$1" | base64 -w 0 | tr '+/' '-_' | tr -d '='
}

H=$(b64 "$HEADER")
P=$(b64 "$PAYLOAD")
# Mock signature (沙箱用)
S="sim-v3.6.11-$USER-$ROLE"

JWT="$H.$P.$S"

echo "--- Mock JWT ---"
echo "$JWT"
echo ""

# 3. 生成 localStorage 注入片段 (供浏览器 console 用)
cat << EOF
--- localStorage 注入 (浏览器 console) ---

// V3.6.11+ 模拟登录 (复制到 DevTools Console)
const mockUser = {
  id: ${RANDOM},
  username: '$USER',
  nickname: '$USER',
  roles: ['$ROLE'],
  avatar: '',
  email: '$USER@example.com',
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
}
localStorage.setItem('minimax_user', JSON.stringify(mockUser))
localStorage.setItem('minimax_token', '$JWT')
localStorage.setItem('minimax_demo_mode', 'false')  // V3.5.93+ 关演示
localStorage.setItem('minimax_login_time', new Date().toISOString())
console.log('✅ 模拟登录成功: $USER / $ROLE')
console.log('🔄 刷新页面生效')

EOF

# 4. 模拟 cookie + token 跳 21 路由
BASE_URL="${BASE_URL:-http://localhost:3000}"
echo "--- 21 路由跳转测试 (带 mock Authorization) ---"

PASS_ROUTE=0
FAIL_ROUTE=0
ROUTES=(
    "/" "/login" "/admin/dashboard" "/admin/cluster" "/admin/audit"
    "/admin/metrics" "/admin/alerts" "/admin/traces" "/admin/provider"
    "/chat" "/monitor" "/kg" "/agent" "/ai/chat" "/ai/workflow"
    "/ai/image-gen" "/ai/marketplace" "/ai/tool-admin" "/h5login"
    "/chat/stream" "/admin/framework"
)

for r in "${ROUTES[@]}"; do
    code=$(curl -s -o /dev/null -w "%{http_code}" \
        "$BASE_URL$r" \
        -H "Authorization: Bearer $JWT" \
        --max-time 5)
    if [[ "$code" == "200" ]]; then
        echo "  ✓ $r → 200"
        PASS_ROUTE=$((PASS_ROUTE+1))
    else
        echo "  ❌ $r → $code"
        FAIL_ROUTE=$((FAIL_ROUTE+1))
    fi
done

echo ""
echo "--- 总结 ---"
echo "  ✓ 通过: $PASS_ROUTE/21"
echo "  ❌ 失败: $FAIL_ROUTE/21"
echo ""
echo "═══════════════════════════════════════════════════════════"
echo "  5 测试账号 (5 选 1):"
for U in "${USERS[@]}"; do
  IFS=':' read -r n p r <<< "$U"
  echo "    $n / $p ($r)"
done
echo "═══════════════════════════════════════════════════════════"
