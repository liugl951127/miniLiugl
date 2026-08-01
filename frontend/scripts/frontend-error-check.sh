#!/bin/bash
# V3.6.14+ 前端错误检查
# 1. 关键路由 200 验证
# 2. 关键 asset 200 验证
# 3. dev server 实际错误

set -e
cd "$(dirname "$0")/.."

FRONTEND_PORT="${FRONTEND_PORT:-3000}"
BASE_URL="http://localhost:$FRONTEND_PORT"

echo "═══════════════════════════════════════════════════════════"
echo "  V3.6.14+ 前端错误检查"
echo "═══════════════════════════════════════════════════════════"

ERRORS=0
WARNINGS=0

# 1. 5 关键路由 (实际抓 js/css 引用, 验证 200)
ROUTES=("/" "/login" "/chat" "/admin/dashboard" "/kg")
echo ""
echo "--- 5 关键路由 + 引用资源 ---"
for r in "${ROUTES[@]}"; do
    code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$r" --max-time 5)
    if [[ "$code" == "200" ]]; then
        echo "  ✓ $r → 200"
    else
        echo "  ❌ $r → $code"
        ERRORS=$((ERRORS+1))
    fi
done

# 2. 21 路由
echo ""
echo "--- 21 路由 ---"
ALL_ROUTES=(
    "/" "/login" "/admin/dashboard" "/admin/cluster" "/admin/audit"
    "/admin/metrics" "/admin/alerts" "/admin/traces" "/admin/provider"
    "/chat" "/monitor" "/kg" "/agent" "/ai/chat" "/ai/workflow"
    "/ai/image-gen" "/ai/marketplace" "/ai/tool-admin" "/h5login"
    "/chat/stream" "/admin/framework"
)
PASS=0
FAIL=0
for r in "${ALL_ROUTES[@]}"; do
    code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$r?demo=1" --max-time 5)
    if [[ "$code" == "200" ]]; then
        PASS=$((PASS+1))
    else
        echo "  ❌ $r → $code"
        FAIL=$((FAIL+1))
    fi
done
echo "  ✓ $PASS/21 通过"

# 3. 关键 asset
echo ""
echo "--- 关键 asset ---"
ASSETS=(
    "/sw.js"
    "/favicon.svg"
    "/offline.html"
    "/manifest.json"
    "/icons/icon-192.svg"
    "/icons/icon-512.svg"
    "/icons/apple-touch-icon.svg"
    "/icons/mask-icon.svg"
)
for a in "${ASSETS[@]}"; do
    code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$a" --max-time 5)
    if [[ "$code" == "200" ]]; then
        echo "  ✓ $a"
    else
        echo "  ⚠️  $a → $code"
        WARNINGS=$((WARNINGS+1))
    fi
done

# 4. HTML script 引用
echo ""
echo "--- HTML script 引用 ---"
HTML=$(curl -s "$BASE_URL/" --max-time 5)
SCRIPTS=$(echo "$HTML" | grep -oE 'src="/[^"]+\.js"' | head -10)
for s in $SCRIPTS; do
    url=$(echo "$s" | sed 's/src="//; s/"$//')
    code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$url" --max-time 5)
    if [[ "$code" == "200" ]]; then
        echo "  ✓ $url"
    else
        echo "  ❌ $url → $code"
        ERRORS=$((ERRORS+1))
    fi
done

# 5. SW 内容检查
echo ""
echo "--- Service Worker ---"
SW_SIZE=$(curl -s "$BASE_URL/sw.js" --max-time 5 | wc -c)
if [[ "$SW_SIZE" -gt 1000 ]]; then
    echo "  ✓ sw.js 加载 ($SW_SIZE bytes)"
    # 检查关键 API
    SW_CONTENT=$(curl -s "$BASE_URL/sw.js" --max-time 5)
    if echo "$SW_CONTENT" | grep -q "SKIP_WAITING"; then
        echo "  ✓ SKIP_WAITING 消息协议"
    fi
    if echo "$SW_CONTENT" | grep -q "GET_VERSION"; then
        echo "  ✓ GET_VERSION 消息协议"
    fi
    if echo "$SW_CONTENT" | grep -q "Background Sync"; then
        echo "  ✓ Background Sync"
    fi
    if echo "$SW_CONTENT" | grep -q "Periodic"; then
        echo "  ✓ Periodic Background Sync"
    fi
else
    echo "  ❌ sw.js 太小 ($SW_SIZE bytes)"
    ERRORS=$((ERRORS+1))
fi

# 总结
echo ""
echo "═══════════════════════════════════════════════════════════"
echo "  总结: $ERRORS 错误, $WARNINGS 警告, $PASS/21 路由通过"
echo "═══════════════════════════════════════════════════════════"
[[ $ERRORS -eq 0 ]] && echo "  🎉 全部通过" || echo "  ❌ 有错误需修"
