#!/bin/bash
# ============================================================
# MiniMax Platform - 压测脚本 (Bash 简单 QPS)
# 用法: bash scripts/benchmark.sh <base_url> <path> [concurrency] [requests]
# ============================================================
set -e

BASE="${1:-http://localhost:8081}"
PATH_URL="${2:-/api/v1/auth/health}"
CONCURRENCY="${3:-50}"
REQUESTS="${4:-1000}"

echo "============================================================"
echo "  MiniMax Benchmark"
echo "  base=$BASE  path=$PATH_URL"
echo "  concurrency=$CONCURRENCY  total=$REQUESTS"
echo "============================================================"

# 用 curl + xargs 并发
T0=$(date +%s%N)
seq 1 $REQUESTS | xargs -n1 -P $CONCURRENCY -I{} \
    curl -s -o /dev/null -w "%{http_code} %{time_total}\n" \
    -H "Accept: application/json" \
    --max-time 30 \
    "$BASE$PATH_URL" > /tmp/bench-out.txt 2>/dev/null

T1=$(date +%s%N)
DUR_MS=$(( (T1 - T0) / 1000000 ))
DUR_S=$(echo "scale=3; $DUR_MS / 1000" | bc)

TOTAL=$(wc -l < /tmp/bench-out.txt)
OK=$(awk '$1==200||$1==401||$1==403' /tmp/bench-out.txt | wc -l)
ERR=$(( TOTAL - OK ))

if [ "$TOTAL" -gt 0 ]; then
    P50=$(awk '{print $2}' /tmp/bench-out.txt | sort -n | sed -n "$((TOTAL*50/100))p")
    P95=$(awk '{print $2}' /tmp/bench-out.txt | sort -n | sed -n "$((TOTAL*95/100))p")
    P99=$(awk '{print $2}' /tmp/bench-out.txt | sort -n | sed -n "$((TOTAL*99/100))p")
    MAX=$(awk '{print $2}' /tmp/bench-out.txt | sort -n | tail -1)
    MIN=$(awk '{print $2}' /tmp/bench-out.txt | sort -n | head -1)
    AVG=$(awk '{sum+=$2} END {printf "%.4f", sum/NR}' /tmp/bench-out.txt)
fi

QPS=$(echo "scale=2; $TOTAL * 1000 / $DUR_MS" | bc)

echo ""
echo "  总耗时:     ${DUR_S}s (${DUR_MS}ms)"
echo "  总请求:     $TOTAL"
echo "  成功:       $OK (HTTP 200/401/403)"
echo "  失败:       $ERR"
echo "  QPS:        $QPS req/s"
echo "  并发:       $CONCURRENCY"
echo "  -----------------------------------"
echo "  延迟 (s):"
echo "    min:      $MIN"
echo "    avg:      $AVG"
echo "    p50:      $P50"
echo "    p95:      $P95"
echo "    p99:      $P99"
echo "    max:      $MAX"
echo "============================================================"
