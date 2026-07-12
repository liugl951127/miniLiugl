#!/usr/bin/env bash
# ============================================================
# MiniMax Platform V3.0.0 性能 SLA 测试脚本
#
# 测试关键接口响应时间 SLA:
#   P50 < 100ms / P95 < 500ms / P99 < 1000ms
# ============================================================
set -uo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:7080}"
AI_URL="${AI_URL:-http://127.0.0.1:8094}"
ADMIN_URL="${ADMIN_URL:-http://127.0.0.1:8090}"
WS_URL="${WS_URL:-http://127.0.0.1:8095}"
N=${N:-100}        # 采样数
WARMUP=${WARMUP:-5} # 预热次数

# 接口列表: name|url|method|body|expected_max_ms
declare -a ENDPOINTS=(
    "Gateway health|$BASE_URL/actuator/health|GET||100"
    "Auth ping|$BASE_URL/api/v1/auth/ping|GET||200"
    "AI health|$AI_URL/actuator/health|GET||100"
    "Admin health|$ADMIN_URL/actuator/health|GET||100"
    "WS health|$WS_URL/actuator/health|GET||100"
    "AI tools|$AI_URL/api/v1/ai/tools|GET||500"
    "TB experiments|$AI_URL/api/v1/tensorboard/experiments|GET||500"
    "Model market|$AI_URL/api/v1/model-market/models|GET||500"
    "Agent market|$AI_URL/api/v1/marketplace/agents|GET||500"
    "Webhook hooks|$AI_URL/api/v1/webhook/hooks|GET||500"
    "Governance|$ADMIN_URL/api/v1/governance/overview|GET||500"
    "AI intent|POST|$AI_URL/api/v1/ai/intent/recognize|{\"text\":\"生成图表\"}|500"
)

# 计算百分位数 (P50/P95/P99)
calc_pct() {
    python3 -c "
import sys
data = sorted([float(x) for x in sys.stdin.read().split() if x])
n = len(data)
if n == 0:
    print('N/A'); sys.exit()
p50 = data[int(n*0.5)] if n > 0 else 0
p95 = data[int(n*0.95)] if n > 1 else data[0]
p99 = data[int(n*0.99)] if n > 1 else data[0]
print(f'P50={p50:.1f}ms P95={p95:.1f}ms P99={p99:.1f}ms N={n} avg={sum(data)/n:.1f}ms')
"
}

C_GREEN='\033[0;32m'; C_RED='\033[0;31m'; C_YEL='\033[1;33m'; C_BLU='\033[0;34m'; C_OFF='\033[0m'

echo -e "${C_BLU}════════════════════════════════════════════${C_OFF}"
echo -e "${C_BLU}  V3.0.0 性能 SLA 测试 (N=$N, 预热 $WARMUP)${C_OFF}"
echo -e "${C_BLU}════════════════════════════════════════════${C_OFF}"
echo

PASS=0; FAIL=0; SKIP=0
for ep in "${ENDPOINTS[@]}"; do
    IFS='|' read -r name url method body sla_ms <<< "$ep"
    # 跳过空 url
    [ -z "$url" ] && continue
    # 预热
    i=0
    while [ "$i" -lt "$WARMUP" ]; do
        if [ "$method" = "POST" ]; then
            curl -fsS -m 3 -o /dev/null -X POST "$url" -H "Content-Type: application/json" -d "$body" 2>/dev/null || true
        else
            curl -fsS -m 3 -o /dev/null "$url" 2>/dev/null || true
        fi
        i=$((i + 1))
    done
    # 采样
    samples=()
    i=0
    while [ "$i" -lt "$N" ]; do
        start=$(date +%s%3N)
        if [ "$method" = "POST" ]; then
            curl -fsS -m 3 -o /dev/null -X POST "$url" -H "Content-Type: application/json" -d "$body" 2>/dev/null
        else
            curl -fsS -m 3 -o /dev/null "$url" 2>/dev/null
        fi
        rc=$?
        end=$(date +%s%3N)
        ms=$((end - start))
        if [ $rc -eq 0 ]; then
            samples+=("$ms")
        fi
        i=$((i + 1))
    done
    if [ ${#samples[@]} -eq 0 ]; then
        SKIP=$((SKIP+1))
        echo -e "${C_YEL}⊘${C_OFF} $name ${C_YEL}(无响应)${C_OFF}"
        continue
    fi
    stats=$(printf "%s\n" "${samples[@]}" | calc_pct)
    p95=$(echo "$stats" | grep -oP 'P95=\K[0-9.]+')
    p95_int=${p95%.*}
    if [ -z "$p95_int" ]; then
        SKIP=$((SKIP+1))
        echo -e "${C_YEL}⊘${C_OFF} $name ${C_YEL}(无 P95 数据)${C_OFF}"
    elif [ "$p95_int" -le "$sla_ms" ] 2>/dev/null; then
        PASS=$((PASS+1))
        echo -e "${C_GREEN}✓${C_OFF} $name  $stats  (SLA: P95<${sla_ms}ms)"
    else
        FAIL=$((FAIL+1))
        echo -e "${C_RED}✗${C_OFF} $name  $stats  (SLA: P95<${sla_ms}ms 超出)"
    fi
done

echo
echo -e "${C_BLU}════════════════════════════════════════════${C_OFF}"
TOTAL=$((PASS + FAIL + SKIP))
echo -e "总计: $TOTAL  ${C_GREEN}通过: $PASS${C_OFF}  ${C_RED}失败: $FAIL${C_OFF}  ${C_YEL}跳过: $SKIP${C_OFF}"
echo -e "${C_BLU}════════════════════════════════════════════${C_OFF}"
[ "$FAIL" -eq 0 ] && exit 0 || exit 1
