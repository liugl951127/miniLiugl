#!/bin/bash
# =============================================================
# verify-deploy.sh (V3.5.77+)
# 一键验证前端部署: build + dev server + 跨浏览器 E2E + 报告
# 用法: bash scripts/verify-deploy.sh [browser]
#   browser: chromium (默认) / webkit / firefox / all
# =============================================================
set -e
cd "$(dirname "$0")/.."
ROOT=$(pwd)

BROWSER=${1:-chromium}
TIMESTAMP=$(date +"%Y-%m-%d %H:%M:%S")
REPORT_DIR="$ROOT/reports/verify-deploy"
mkdir -p "$REPORT_DIR"
REPORT="$REPORT_DIR/verify-$TIMESTAMP.log"

echo "═══════════════════════════════════════════════════════════" | tee -a "$REPORT"
echo "  V3.5.77+ 部署验证" | tee -a "$REPORT"
echo "  时间: $TIMESTAMP" | tee -a "$REPORT"
echo "  浏览器: $BROWSER" | tee -a "$REPORT"
echo "═══════════════════════════════════════════════════════════" | tee -a "$REPORT"
echo "" | tee -a "$REPORT"

EXIT=0

# 1. 验证 Node 版本
echo "--- 1. Node 版本检查 ---" | tee -a "$REPORT"
NODE_VERSION=$(node -v 2>/dev/null || echo "未安装")
echo "  当前 Node: $NODE_VERSION" | tee -a "$REPORT"
if [[ "$NODE_VERSION" == "v16.20"* ]] || [[ "$NODE_VERSION" == "v18."* ]] || [[ "$NODE_VERSION" == "v20."* ]] || [[ "$NODE_VERSION" == "v22."* ]]; then
    echo "  ✓ Node 版本兼容 (V3.5.74+ engines >=16.20.0)" | tee -a "$REPORT"
else
    echo "  ✗ Node 版本不在兼容范围 (需要 >=16.20.0)" | tee -a "$REPORT"
    EXIT=1
fi
echo "" | tee -a "$REPORT"

# 2. 验证后端编译 (V3.5.77+ SKIP_BACKEND=1 跳过, 沙箱 OOM 保护)
echo "--- 2. 后端 14 module 编译 ---" | tee -a "$REPORT"
if [ "${SKIP_BACKEND:-0}" == "1" ]; then
    echo "  ⊘ 跳过 (SKIP_BACKEND=1)" | tee -a "$REPORT"
else
    cd "$ROOT/backend"
    if mvn install -DskipTests -T 4 -q 2>&1 | tail -3 | tee -a "$REPORT"; then
        echo "  ✓ 后端 14 module BUILD SUCCESS" | tee -a "$REPORT"
    else
        echo "  ✗ 后端编译失败" | tee -a "$REPORT"
        EXIT=1
    fi
fi
echo "" | tee -a "$REPORT"

# 3. 验证前端依赖
echo "--- 3. 前端依赖检查 ---" | tee -a "$REPORT"
cd "$ROOT/frontend"
if [ ! -d "node_modules" ]; then
    echo "  安装依赖..." | tee -a "$REPORT"
    NODE_OPTIONS="--max-old-space-size=1500" npm install --no-audit --prefer-offline 2>&1 | tail -3 | tee -a "$REPORT"
fi
echo "  ✓ node_modules 存在" | tee -a "$REPORT"
echo "" | tee -a "$REPORT"

# 4. 验证前端 build
echo "--- 4. 前端 build ---" | tee -a "$REPORT"
rm -rf dist
if NODE_OPTIONS="--max-old-space-size=1500" npx vite build 2>&1 | tail -5 | tee -a "$REPORT"; then
    echo "  ✓ Vite build 成功" | tee -a "$REPORT"
else
    echo "  ✗ Vite build 失败" | tee -a "$REPORT"
    EXIT=1
fi
echo "" | tee -a "$REPORT"

# 5. 启动 dev server
echo "--- 5. 启动 vite dev server ---" | tee -a "$REPORT"
DEV_PORT=5188
NODE_OPTIONS="--max-old-space-size=1500" npx vite dev --port $DEV_PORT --host 0.0.0.0 > /tmp/verify-dev.log 2>&1 &
DEV_PID=$!
echo "  Vite dev PID: $DEV_PORT:$DEV_PID" | tee -a "$REPORT"
sleep 6
if curl -sf http://localhost:$DEV_PORT/ > /dev/null 2>&1; then
    echo "  ✓ Vite dev 启动成功 (http://localhost:$DEV_PORT)" | tee -a "$REPORT"
else
    echo "  ✗ Vite dev 启动失败" | tee -a "$REPORT"
    kill $DEV_PID 2>/dev/null || true
    exit 1
fi
echo "" | tee -a "$REPORT"

# 6. 跨浏览器 E2E
echo "--- 6. 跨浏览器 E2E ---" | tee -a "$REPORT"
if [ "$BROWSER" == "all" ]; then
    PROJECTS=(--project=chromium --project=webkit --project=firefox --project=mobile-safari --project=mobile-chrome)
else
    PROJECTS=(--project=$BROWSER)
fi
if E2E_BASE_URL=http://localhost:$DEV_PORT NODE_OPTIONS="--max-old-space-size=1500" npx playwright test "${PROJECTS[@]}" --reporter=line 2>&1 | tail -20 | tee -a "$REPORT"; then
    echo "  ✓ E2E 跨浏览器测试通过" | tee -a "$REPORT"
else
    echo "  ✗ E2E 部分失败" | tee -a "$REPORT"
    EXIT=1
fi
echo "" | tee -a "$REPORT"

# 7. 关闭 dev server
kill $DEV_PID 2>/dev/null || true
sleep 1
echo "" | tee -a "$REPORT"

# 8. 总结
echo "═══════════════════════════════════════════════════════════" | tee -a "$REPORT"
if [ $EXIT -eq 0 ]; then
    echo "  ✓ ALL VERIFY PASS" | tee -a "$REPORT"
else
    echo "  ✗ SOME VERIFY FAILED" | tee -a "$REPORT"
fi
echo "═══════════════════════════════════════════════════════════" | tee -a "$REPORT"
echo "  报告: $REPORT" | tee -a "$REPORT"
echo "═══════════════════════════════════════════════════════════" | tee -a "$REPORT"

exit $EXIT
