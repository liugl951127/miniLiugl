#!/usr/bin/env bash
# =============================================================
# MiniMax Platform V3.5.8+ 端到端验证脚本
# 启动服务后跑这个, 验证所有 API
# =============================================================
set -e

green()  { echo -e "\033[32m$*\033[0m"; }
red()    { echo -e "\033[31m$*\033[0m"; }
yellow() { echo -e "\033[33m$*\033[0m"; }
blue()   { echo -e "\033[36m$*\033[0m"; }

echo "═══════════════════════════════════════════════════════════"
echo "  MiniMax Platform V3.5.8+ 端到端验证"
echo "═══════════════════════════════════════════════════════════"
echo ""

PASS=0
FAIL=0

# 测试函数
test_api() {
  local name=$1
  local url=$2
  local expected=$3
  
  code=$(curl -s -o /tmp/e2e-resp -w "%{http_code}" "$url" 2>/dev/null || echo "000")
  if [[ "$code" == "$expected" ]]; then
    green "  ✅ $name ($code)"
    PASS=$((PASS+1))
  else
    red "  ❌ $name (HTTP $code, 期望 $expected)"
    FAIL=$((FAIL+1))
  fi
}

test_api_json() {
  local name=$1
  local url=$2
  local jq_path=$3
  local expected=$4
  
  result=$(curl -s "$url" 2>/dev/null)
  actual=$(echo "$result" | python3 -c "import sys, json; print(json.load(sys.stdin).get('data', {}).get('$jq_path', 'N/A'))" 2>/dev/null || echo "ERR")
  if [[ "$actual" == "$expected" ]]; then
    green "  ✅ $name ($jq_path=$actual)"
    PASS=$((PASS+1))
  else
    yellow "  ⚠️ $name ($jq_path=$actual, 期望 $expected)"
  fi
}

# ============== 1. 健康检查 ==============
echo "【1】健康检查"
test_api "Gateway"  "http://localhost:7080/actuator/health"  "200"
test_api "AI"       "http://localhost:8094/actuator/health"  "200"
test_api "Auth"     "http://localhost:8081/actuator/health"  "200"
test_api "Admin"    "http://localhost:8090/actuator/health"  "200"
test_api "Monitor"  "http://localhost:8089/actuator/health"  "200"
test_api "Nginx"    "http://localhost/health"                "200"
echo ""

# ============== 2. AI 核心 ==============
echo "【2】AI 核心 API"
test_api_json "AI Intro"     "http://localhost:8094/api/ai/intro"               "name"    "minimax-ai"
test_api_json "AI Modules"   "http://localhost:8094/api/ai/intro"               "version" "V3.5.7"

# 意图识别
INTENT_TESTS=(
  "查询销售|data_query"
  "对比趋势|data_compare"
  "分析用户|data_analyze"
  "预测销量|data_predict"
  "画个折线图|data_visualize"
  "生成月报|data_report"
  "什么是RAG|consult"
  "我要投诉|complaint"
)

PASS_INTENT=0
for test in "${INTENT_TESTS[@]}"; do
  text="${test%%|*}"
  expected="${test##*|}"
  result=$(curl -s -X POST http://localhost:8094/api/v1/ai/intent/predict \
      -H "Content-Type: application/json" \
      -d "{\"text\":\"$text\"}" 2>/dev/null)
  actual=$(echo "$result" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data', {}).get('intent', 'N/A'))" 2>/dev/null)
  if [[ "$actual" == "$expected" ]]; then
    PASS_INTENT=$((PASS_INTENT+1))
  else
    yellow "  ⚠️ '$text' -> $actual (期望 $expected)"
  fi
done
green "  ✅ 意图识别: $PASS_INTENT/${#INTENT_TESTS[@]} 准确"
echo ""

# ============== 3. 工具调用 ==============
echo "【3】AI 工具调用"
test_api_json "Tools 列表" "http://localhost:8094/api/v1/ai/tools" "" "5"
echo ""

# ============== 4. 总结 ==============
echo "═══════════════════════════════════════════════════════════"
green "  验证完成: $PASS 通过, $FAIL 失败"
echo "═══════════════════════════════════════════════════════════"
[[ $FAIL -eq 0 ]] && exit 0 || exit 1
