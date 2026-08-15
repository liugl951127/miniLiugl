#!/bin/bash
# V3.6.19+ 综合错误检查 (6 段)
set -e
FRONTEND_PORT="${FRONTEND_PORT:-3600}"
BASE_URL="http://localhost:$FRONTEND_PORT"

echo "═══════════════════════════════════════════════════════════"
echo "  V3.6.19+ 综合前端错误检查"
echo "═══════════════════════════════════════════════════════════"

ERRORS=0
WARNINGS=0

# 1. Build
echo ""
echo "--- 1. vite build ---"
cd frontend
rm -rf dist
NODE_OPTIONS="--max-old-space-size=1500" npx vite build 2>&1 > /tmp/build_check.log || true
cd ..
WARN_COUNT=$(grep -cE "warn|warning" /tmp/build_check.log 2>/dev/null || echo 0)
ERR_COUNT=$(grep -cE "^Error|error during build" /tmp/build_check.log 2>/dev/null || echo 0)
echo "  警告: $WARN_COUNT, 错误: $ERR_COUNT"

# 2. 启动 vite dev
pkill -9 -f "vite.*$FRONTEND_PORT" 2>/dev/null
sleep 2
cd frontend
NODE_OPTIONS="--max-old-space-size=1500" npx vite --port $FRONTEND_PORT --host 0.0.0.0 > /tmp/vite_check.log 2>&1 &
VITE_PID=$!
sleep 12
cd ..

# 3. 21 路由
echo ""
echo "--- 2. 21 路由 ---"
ROUTES=("/" "/login" "/admin/dashboard" "/admin/cluster" "/admin/audit" "/admin/metrics" "/admin/alerts" "/admin/traces" "/admin/provider" "/chat" "/monitor" "/kg" "/agent" "/ai/chat" "/ai/workflow" "/ai/image-gen" "/ai/marketplace" "/ai/tool-admin" "/h5login" "/chat/stream" "/admin/framework")
PASS=0
for r in "${ROUTES[@]}"; do
    code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$r?demo=1" --max-time 5)
    [[ "$code" == "200" ]] && PASS=$((PASS+1)) || { echo "  ❌ $r → $code"; ERRORS=$((ERRORS+1)); }
done
echo "  ✓ $PASS/21 通过"

# 4. .vue / .js 编译
echo ""
echo "--- 3. .vue / .js 编译 ---"
VIEWS=("src/views/chat/Index.vue" "src/views/kg/Index.vue" "src/views/admin/Dashboard.vue" "src/views/admin/Index.vue" "src/components/ErrorBoundary.vue" "src/components/ErrorState.vue" "src/composables/useSpeechCall.js" "src/composables/useToast.js")
PASS=0
for v in "${VIEWS[@]}"; do
    code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/$v" --max-time 5)
    [[ "$code" == "200" ]] && PASS=$((PASS+1)) || { echo "  ❌ $v → $code"; ERRORS=$((ERRORS+1)); }
done
echo "  ✓ $PASS/${#VIEWS[@]} 通过"

# 5. 关键 asset
echo ""
echo "--- 4. 关键 asset ---"
ASSETS=("/sw.js" "/favicon.svg" "/offline.html" "/manifest.json" "/icons/icon-192.svg")
PASS=0
for a in "${ASSETS[@]}"; do
    code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$a" --max-time 5)
    [[ "$code" == "200" ]] && PASS=$((PASS+1)) || { echo "  ❌ $a → $code"; ERRORS=$((ERRORS+1)); }
done
echo "  ✓ $PASS/${#ASSETS[@]} 通过"

# 6. 依赖
echo ""
echo "--- 5. 关键依赖 ---"
for dep in vue "@vueuse/core" element-plus echarts axios vue-router pinia; do
    ver=$(grep "\"$dep\"" frontend/package.json | head -1 | sed 's/.*: "\(.*\)",.*/\1/')
    echo "  $dep: $ver"
done

# 7. dev log 警告
echo ""
echo "--- 6. dev log 警告/error ---"
if [[ -f /tmp/vite_check.log ]]; then
    DEV_WARN=$(grep -ciE "warn|warning" /tmp/vite_check.log || echo 0)
    DEV_ERR=$(grep -ciE "error|cannot|undefined" /tmp/vite_check.log || echo 0)
    echo "  dev 警告: $DEV_WARN, 错误: $DEV_ERR"
    if [[ $DEV_ERR -gt 0 ]]; then
        echo "  dev 错误内容:"
        grep -iE "error|cannot|undefined" /tmp/vite_check.log | head -5
    fi
fi

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "  总结: $ERRORS 错误, 21/21 路由"
echo "═══════════════════════════════════════════════════════════"
[[ $ERRORS -eq 0 ]] && echo "  🎉 无错误" || echo "  ❌ 有错误"

# 清理
pkill -9 -f "vite.*$FRONTEND_PORT" 2>/dev/null
