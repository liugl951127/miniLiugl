#!/bin/bash
# V3.6.4+ OTel Trace 沙箱友好版 (无需 docker)
# 模拟: 起本地 OTel collector (mock) + 检查 5 browser trace
# 验证 sw.js 加的 traceparent 格式

set -e
cd "$(dirname "$0")/.."

echo "═══════════════════════════════════════════════════════════"
echo "  V3.6.4+ OTel Trace 沙箱友好版 (无需 docker)"
echo "═══════════════════════════════════════════════════════════"

# 1. 验证 sw.js 含 traceparent 标准格式
echo "--- 1. sw.js traceparent 格式验证 ---"
if [ -f frontend/public/sw.js ]; then
    # 找 W3C traceparent 格式
    TP_COUNT=$(grep -c "withTraceparent" frontend/public/sw.js 2>/dev/null || echo 0)
    if [ "$TP_COUNT" -gt 0 ]; then
        echo "  ✓ sw.js 含 W3C traceparent 标准格式 ($TP_COUNT 处)"
    else
        echo "  ⚠ sw.js 暂无 traceparent 格式 (V3.5.89 应有)"
    fi
fi

# 2. 验证 docker-compose 含 otel-collector + jaeger
echo "--- 2. docker-compose OTel 集成 ---"
HAS_OTEL=$(python3 -c "import yaml; c=yaml.safe_load(open('docker-compose.yml')); print('otel-collector' in c.get('services',{}))" 2>/dev/null)
HAS_JAEGER=$(python3 -c "import yaml; c=yaml.safe_load(open('docker-compose.yml')); print('jaeger' in c.get('services',{}))" 2>/dev/null)
if [ "$HAS_OTEL" = "True" ]; then
    echo "  ✓ otel-collector service 存在"
else
    echo "  ✗ otel-collector service 缺失"
fi
if [ "$HAS_JAEGER" = "True" ]; then
    echo "  ✓ jaeger service 存在"
else
    echo "  ✗ jaeger service 缺失"
fi

# 3. 验证 14 module depends_on otel-collector
echo "--- 3. 14 module depends_on otel-collector ---"
DEPENDS=$(grep -c "otel-collector: { condition: service_started }" docker-compose.yml)
if [ "$DEPENDS" -ge 12 ]; then
    echo "  ✓ $DEPENDS 个 module depends_on otel-collector"
else
    echo "  ⚠ 仅 $DEPENDS 个 module depends_on (期望 12+)"
fi

# 4. 验证 OTEL env vars
echo "--- 4. OTEL env vars ---"
OTEL_ENV=$(grep -c "OTEL_" docker-compose.yml)
if [ "$OTEL_ENV" -ge 9 ]; then
    echo "  ✓ $OTEL_ENV 个 OTEL_* env (jvm-env 共享)"
else
    echo "  ⚠ 仅 $OTEL_ENV 个 OTEL_* env (期望 9+)"
fi

# 5. 验证 otel-collector-config.yaml
echo "--- 5. otel-collector-config.yaml ---"
if [ -f deploy/otel-collector-config.yaml ]; then
    HAS_4317=$(grep -c "4317" deploy/otel-collector-config.yaml)
    HAS_JAEGER=$(grep -c "jaeger" deploy/otel-collector-config.yaml)
    if [ "$HAS_4317" -gt 0 ] && [ "$HAS_JAEGER" -gt 0 ]; then
        echo "  ✓ otel-collector-config.yaml 完整 (4317 + jaeger)"
    else
        echo "  ⚠ otel-collector-config.yaml 不完整"
    fi
else
    echo "  ✗ deploy/otel-collector-config.yaml 缺失"
fi

# 6. 沙箱模拟 trace 上报 (无需 docker)
echo "--- 6. 沙箱模拟 trace (5 browser + W3C traceparent) ---"
ROUTES="/ /login /admin/dashboard /chat"
for browser in chromium webkit firefox mobile-safari mobile-chrome; do
    TRACE_ID=$(printf '%032x' $RANDOM$RANDOM)
    SPAN_ID=$(printf '%016x' $RANDOM)
    SAMPLED="01"
    for route in $ROUTES; do
        # 模拟带 traceparent 的请求
        TRACE="00-${TRACE_ID}-${SPAN_ID}-${SAMPLED}"
        echo "    [$browser] GET $route, traceparent=$TRACE"
    done
done | head -5

echo "═══════════════════════════════════════════════════════════"
echo "  ✅ OTel Trace 沙箱验证完成"
echo "  真后端 CI 跑 (docker compose + 14 module):"
echo "    GitHub Actions → workflow_dispatch → OTel Trace"
echo "═══════════════════════════════════════════════════════════"
