#!/usr/bin/env bash
# ============================================================
# MiniMax Platform — 压测执行脚本 (Day 21)
# 支持: wrk / JMeter / Apache Bench
# ============================================================

set -e
cd "$(dirname "$0")"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

GATEWAY="${GATEWAY:-http://localhost:7080}"
DURATION="${DURATION:-60}"
CONNECTIONS="${CONNECTIONS:-100}"
THREADS="${THREADS:-4}"

echo "=================================================="
echo " MiniMax Platform — 性能压测"
echo " Gateway:  ${GATEWAY}"
echo " Duration: ${DURATION}s"
echo " Conn:     ${CONNECTIONS}"
echo " Threads:  ${THREADS}"
echo "=================================================="

# ---- 检测可用工具 ----
run_wrk() {
  echo ""
  echo "━━━ wrk 压测 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  if command -v wrk &>/dev/null; then
    echo -e "${GREEN}[wrk 可用]${NC} 执行登录 API 压测..."
    wrk -t${THREADS} -c${CONNECTIONS} -d${DURATION}s \
      -s wrk/login.lua \
      "${GATEWAY}" \
      --latency \
      2>&1 | tee "wrk-login-$(date +%H%M%S).txt"
  else
    echo -e "${YELLOW}[wrk 不可用]${NC} 请安装: brew install wrk / apt install wrk"
    echo "   或访问 https://github.com/wg/wrk 获取源码编译"
  fi
}

run_ab() {
  echo ""
  echo "━━━ Apache Bench 压测 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  if command -v ab &>/dev/null; then
    echo -e "${GREEN}[ab 可用]${NC} 执行登录 API 压测..."
    ab -n 1000 -c ${CONNECTIONS} -p <(cat <<'EOF'
{"username":"admin","password":"admin@123"}
EOF
) -T "application/json" \
      -H "X-Request-Source: ab" \
      "${GATEWAY}/api/v1/auth/login" \
      2>&1 | tee "ab-login-$(date +%H%M%S).txt"
  else
    echo -e "${YELLOW}[ab 不可用]${NC} 请安装: apt install apache2-utils"
  fi
}

run_jmeter() {
  echo ""
  echo "━━━ JMeter 压测 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  if command -v jmeter &>/dev/null; then
    echo -e "${GREEN}[JMeter 可用]${NC} 执行 API 压测..."
    jmeter -n \
      -t jmeter/minimax-api-test.jmx \
      -l "jmeter-result-$(date +%H%M%S).jtl" \
      -e -o "jmeter-html-$(date +%H%M%S)" \
      -JGATEWAY_URL="${GATEWAY}" \
      2>&1
  else
    echo -e "${YELLOW}[JMeter 不可用]${NC} 请安装 Apache JMeter 并配置 PATH"
    echo "   下载: https://jmeter.apache.org/download_jmeter.cgi"
  fi
}

# ---- 健康检查（压测前）----
echo ""
echo "━━━ 压测前健康检查 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
for endpoint in \
  "${GATEWAY}/actuator/health" \
  "${GATEWAY}/api/v1/auth/health" \
  "${GATEWAY}/api/v1/admin/ping"; do
  HC=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$endpoint" 2>/dev/null || echo "000")
  if [ "$HC" = "200" ]; then
    echo -e "  ${GREEN}[OK]${NC} $endpoint"
  else
    echo -e "  ${RED}[FAIL]${NC} $endpoint (HTTP $HC) — 压测前请先启动服务"
    echo ""
    echo -e "${RED}服务未就绪，终止压测！${NC}"
    exit 1
  fi
done

# ---- 压测选择 ----
MODE="${1:-all}"
case "$MODE" in
  wrk)
    run_wrk
    ;;
  ab)
    run_ab
    ;;
  jmeter)
    run_jmeter
    ;;
  all)
    run_wrk
    run_ab
    run_jmeter
    ;;
  *)
    echo "用法: $0 [wrk|ab|jmeter|all]"
    echo "  wrk    — wrk HTTP 压测（推荐）"
    echo "  ab     — Apache Bench"
    echo "  jmeter — JMeter 完整测试"
    echo "  all    — 全部运行"
    ;;
esac

echo ""
echo "=================================================="
echo -e "${GREEN}压测完成！${NC}"
echo "结果文件已保存在 $(pwd)/"
ls -lh *.txt *.jtl 2>/dev/null || true
echo "=================================================="
