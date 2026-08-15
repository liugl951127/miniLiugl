#!/bin/bash
# V3.6.14+ 前端错误检查
set -e
FRONTEND_PORT="${FRONTEND_PORT:-3000}"
BASE_URL="http://localhost:$FRONTEND_PORT"

echo "═══════════════════════════════════════════════════════════"
echo "  V3.6.14+ 前端错误检查"
echo "═══════════════════════════════════════════════════════════"

ERRORS=0
WARNINGS=0

ROUTES=(
    "/" "/login" "/admin/dashboard" "/admin/cluster" "/admin/audit"
    "/admin/metrics" "/admin/alerts" "/admin/traces" "/admin/provider"
    "/chat" "/monitor" "/kg" "/agent" "/ai/chat" "/ai/workflow"
    "/ai/image-gen" "/ai/marketplace" "/ai/tool-admin" "/h5login"
    "/chat/stream" "/admin/framework"
)
echo ""
echo "--- 21 路由 ---"
PASS=0
FAIL=0
for r in "${ROUTES[@]}"; do
    code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$r?demo=1" --max-time 5)
    if [[ "$code" == "200" ]]; then
        PASS=$((PASS+1))
    else
        echo "  ❌ $r → $code"
        FAIL=$((FAIL+1))
    fi
done
echo "  ✓ $PASS/21 通过"

echo ""
echo "--- 关键 asset ---"
ASSETS=("/sw.js" "/favicon.svg" "/offline.html" "/manifest.json" "/icons/icon-192.svg")
for a in "${ASSETS[@]}"; do
    code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$a" --max-time 5)
    if [[ "$code" == "200" ]]; then
        echo "  ✓ $a"
    else
        echo "  ⚠️  $a → $code"
        WARNINGS=$((WARNINGS+1))
    fi
done

echo ""
echo "--- Service Worker 内容检查 ---"
SW_CONTENT=$(curl -s "$BASE_URL/sw.js" --max-time 5)
echo "$SW_CONTENT" | grep -q "SKIP_WAITING" && echo "  ✓ SKIP_WAITING 消息协议" || { echo "  ❌ SKIP_WAITING 缺失"; ERRORS=$((ERRORS+1)); }
echo "$SW_CONTENT" | grep -q "GET_VERSION" && echo "  ✓ GET_VERSION 消息协议" || { echo "  ❌ GET_VERSION 缺失"; ERRORS=$((ERRORS+1)); }
echo "$SW_CONTENT" | grep -q "sync" && echo "  ✓ Background Sync" || echo "  ⚠️  无 sync 事件"
echo "$SW_CONTENT" | grep -q "periodicsync" && echo "  ✓ Periodic Background Sync" || echo "  ⚠️  无 periodicsync"
echo "$SW_CONTENT" | grep -q "icon-192.svg" && echo "  ✓ 图标引用 .svg (PWA 标准)" || echo "  ⚠️  图标可能用 .png (404)"

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "  总结: $ERRORS 错误, $WARNINGS 警告, $PASS/21 路由"
echo "═══════════════════════════════════════════════════════════"
[[ $ERRORS -eq 0 ]] && echo "  🎉 无致命错误" || echo "  ❌ 有错误需修"
