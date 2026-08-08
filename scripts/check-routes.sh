#!/bin/bash
# V6.3+ 路由一致性检查 (V3 完整版)
# 检查: 后端 Controller / Gateway 路由 / Nginx location / 前端 API 调用

set -e
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT="$(dirname "$SCRIPT_DIR")"
cd "$ROOT"

echo "========================================="
echo "MiniMax 路由一致性检查 (V6.3+ V3)"
echo "========================================="
echo

# 1. 后端 Controller 路由
echo "📦 [1/4] 后端 Controller 路由..."
BACKEND_ROUTES=$(mktemp)
find backend -name "*Controller.java" -path "*/main/*" 2>/dev/null | while read f; do
    cls=$(grep -oE '@RequestMapping\(["\x27]([^"\x27]+)["\x27]' "$f" | head -1 | sed -E 's/@RequestMapping\(["\x27]([^"\x27]+)["\x27]/\1/')
    grep -oE '@(Get|Post|Put|Delete|Patch)Mapping' "$f" | while read m; do
        if [ -n "$cls" ]; then
            echo "$cls" >> "$BACKEND_ROUTES"
        fi
    done
done
echo "  ✓ 后端 Controller 数: $(find backend -name '*Controller.java' -path '*/main/*' 2>/dev/null | wc -l)"
echo "  ✓ 后端路由数 (粗略): $(wc -l < "$BACKEND_ROUTES")"

# 2. Gateway 路由
echo "🌐 [2/4] Gateway 路由..."
GATEWAY_ROUTES=$(mktemp)
if [ -f backend/minimax-gateway/src/main/resources/application.yml ]; then
    grep -oE 'Path=[^,\n]+' backend/minimax-gateway/src/main/resources/application.yml | \
        sed -E 's/Path=//; s/^"//; s/"$//' | tr ',' '\n' | sed 's/^[[:space:]]*//' | sort -u > "$GATEWAY_ROUTES"
fi
echo "  ✓ Gateway 路由数: $(wc -l < "$GATEWAY_ROUTES")"

# 3. Nginx location
echo "📡 [3/4] Nginx location 路由..."
NGINX_ROUTES=$(mktemp)
NGINX_CONF=$(find . -name "nginx*.conf" -not -path "*/node_modules/*" -not -path "*/target/*" 2>/dev/null | head -1)
if [ -n "$NGINX_CONF" ]; then
    # 提取 location 路径
    grep -oE 'location\s+[~^]*\s*[\^~]?/?[^ ]+\s*\{' "$NGINX_CONF" | \
        sed -E 's/location[[:space:]]+[~^]*[[:space:]]*[\^~]?//;s/[[:space:]]*\{$//' | \
        grep -v "^/healthz\|^/$\|^~$" | sort -u > "$NGINX_ROUTES"
fi
echo "  ✓ Nginx 路径数: $(wc -l < "$NGINX_ROUTES")"
echo "  ✓ Nginx 配置: $NGINX_CONF"

# 4. 前端 API 调用
echo "🎨 [4/4] 前端 API 调用..."
FRONTEND_URLS=$(mktemp)
grep -rEh "http\.(get|post|put|delete|patch)\([\`'\"]/[^\\\'\"\\)]+" frontend/src/api/ 2>/dev/null | \
    grep -oE "[\`'\"](/[^\`'\"\\)]+)" | sed -E "s/[\`'\"](.*)/\\1/" | sort -u > "$FRONTEND_URLS"
echo "  ✓ 前端 API 调用: $(wc -l < "$FRONTEND_URLS")"

# 5. 不匹配分析
echo
echo "========================================="
echo "🔍 不匹配分析"
echo "========================================="

# 5.1 前端调但 Nginx 没声明
NOT_IN_NGINX=$(comm -23 "$FRONTEND_URLS" "$NGINX_ROUTES" 2>/dev/null | wc -l)
echo "❌ [前端 → Nginx] 前端调但 Nginx 没声明: $NOT_IN_NGINX"
if [ "$NOT_IN_NGINX" -gt 0 ] && [ "$NOT_IN_NGINX" -lt 50 ]; then
    echo "  示例 (前 10):"
    comm -23 "$FRONTEND_URLS" "$NGINX_ROUTES" 2>/dev/null | head -10 | sed 's/^/    - /'
fi

# 5.2 前端调但 Gateway 没声明
NOT_IN_GATEWAY=$(comm -23 "$FRONTEND_URLS" "$GATEWAY_ROUTES" 2>/dev/null | wc -l)
echo "❌ [前端 → Gateway] 前端调但 Gateway 没声明: $NOT_IN_GATEWAY"

# 5.3 Nginx 有但 Gateway 没声明 (Nginx → 后端 绕过 Gateway)
NGINX_NOT_GATEWAY=$(comm -23 "$NGINX_ROUTES" "$GATEWAY_ROUTES" 2>/dev/null | wc -l)
echo "ℹ️  [Nginx → Gateway] Nginx 有但 Gateway 没声明: $NGINX_NOT_GATEWAY"
if [ "$NGINX_NOT_GATEWAY" -gt 0 ] && [ "$NGINX_NOT_GATEWAY" -lt 20 ]; then
    echo "  示例:"
    comm -23 "$NGINX_ROUTES" "$GATEWAY_ROUTES" 2>/dev/null | head -5 | sed 's/^/    - /'
fi

# 5.4 Gateway 有但 Nginx 没声明 (Nginx 没暴露)
GATEWAY_NOT_NGINX=$(comm -13 "$NGINX_ROUTES" "$GATEWAY_ROUTES" 2>/dev/null | wc -l)
echo "ℹ️  [Gateway → Nginx] Gateway 有但 Nginx 没暴露: $GATEWAY_NOT_NGINX"

# 5.5 Nginx 有但前端没调
NGINX_NOT_USED=$(comm -13 "$FRONTEND_URLS" "$NGINX_ROUTES" 2>/dev/null | wc -l)
echo "ℹ️  [Nginx → 前端] Nginx 有但前端没调: $NGINX_NOT_USED"

# 6. 清理
rm -f "$BACKEND_ROUTES" "$GATEWAY_ROUTES" "$NGINX_ROUTES" "$FRONTEND_URLS"

echo
echo "========================================="
echo "✅ 检查完成"
echo "========================================="
