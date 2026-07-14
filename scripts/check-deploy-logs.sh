#!/usr/bin/env bash
# =============================================================
# 部署后日志检查脚本 (V3.5.8)
# 用途: 部署完成后, 自动检查 5 个后端容器 + nginx 的运行日志
# 验证: 启动成功 / 关键服务注册 / 端口监听 / 无致命错误
# =============================================================

set -e

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.mini.yml}"
PROJECT="${PROJECT:-minimax-mini}"
LOG_DIR="/tmp/minimax-logs"

green()  { echo -e "\033[32m$*\033[0m"; }
red()    { echo -e "\033[31m$*\033[0m"; }
yellow() { echo -e "\033[33m$*\033[0m"; }
blue()   { echo -e "\033[36m$*\033[0m"; }
bold()   { echo -e "\033[1m$*\033[0m"; }

mkdir -p "$LOG_DIR"

# ── 1. 收集所有容器日志 ──
blue "📋 [1/5] 收集所有容器日志"
echo "  COMPOSE: $COMPOSE_FILE"
echo ""
ALL_SERVICES=$(docker compose -f "$COMPOSE_FILE" -p "$PROJECT" ps --services 2>/dev/null)
if [ -z "$ALL_SERVICES" ]; then
  red "❌ 没有运行中的容器 (project: $PROJECT)"
  exit 1
fi

for svc in $ALL_SERVICES; do
  log_file="$LOG_DIR/${svc}.log"
  docker compose -f "$COMPOSE_FILE" -p "$PROJECT" logs --no-color --tail=200 "$svc" > "$log_file" 2>&1
  size=$(du -h "$log_file" | cut -f1)
  echo "  ✓ $svc: $log_file ($size)"
done
echo ""

# ── 2. 检查启动状态 ──
blue "🔍 [2/5] 检查启动状态 (Started Application)"
STARTED_OK=0
STARTED_FAIL=0
for svc in $ALL_SERVICES; do
  log_file="$LOG_DIR/${svc}.log"
  if grep -q "Started.*Application in.*seconds" "$log_file" 2>/dev/null; then
    time=$(grep "Started.*Application" "$log_file" | tail -1 | grep -oE "in [0-9.]+ seconds" | head -1)
    green "  ✓ $svc 启动成功 ($time)"
    STARTED_OK=$((STARTED_OK+1))
  elif [[ "$svc" == "mariadb" || "$svc" == "redis" || "$svc" == "nginx" ]]; then
    # 这些不是 Spring Boot, 不需要 Started
    if docker compose -f "$COMPOSE_FILE" -p "$PROJECT" ps "$svc" 2>/dev/null | grep -q "Up"; then
      green "  ✓ $svc 容器运行中"
      STARTED_OK=$((STARTED_OK+1))
    fi
  else
    red "  ✗ $svc 未启动"
    echo "    最后 5 行日志:"
    tail -5 "$log_file" | sed 's/^/      /'
    STARTED_FAIL=$((STARTED_FAIL+1))
  fi
done
echo "  启动成功: $STARTED_OK, 失败: $STARTED_FAIL"
echo ""

# ── 3. 检查关键服务注册 ──
blue "🔌 [3/5] 检查关键服务注册"
declare -A EXPECTED=(
  ["auth"]="8081"
  ["ai"]="8094"
  ["gateway"]="7080"
)
for svc in "${!EXPECTED[@]}"; do
  port="${EXPECTED[$svc]}"
  log_file="$LOG_DIR/${svc}.log"
  if grep -q "Tomcat started on port ($port)" "$log_file" 2>/dev/null; then
    green "  ✓ $svc 端口 $port 已监听"
  else
    yellow "  ⚠  $svc 端口 $port 未确认 (检查实际暴露)"
    # 直接 curl 测试
    if curl -s --max-time 3 "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
      green "    但 $port 可访问 (通过 nginx 代理?)"
    fi
  fi
done
echo ""

# ── 4. 检查致命错误 ──
blue "🚨 [4/5] 检查致命错误 (ERROR/FATAL)"
ERROR_COUNT=0
for svc in $ALL_SERVICES; do
  log_file="$LOG_DIR/${svc}.log"
  # 排除常见的 WARN 噪音
  errors=$(grep -iE "ERROR|Exception|FATAL|Caused by" "$log_file" 2>/dev/null | \
    grep -viE "DisconnectedClient|Connection refused|getOutputStream()" | \
    head -10 || true)
  if [ -n "$errors" ]; then
    yellow "  ⚠  $svc 有 $(echo "$errors" | wc -l) 个错误:"
    echo "$errors" | sed 's/^/      /' | head -5
    ERROR_COUNT=$((ERROR_COUNT+$(echo "$errors" | wc -l)))
  else
    green "  ✓ $svc 无错误"
  fi
done
echo ""

# ── 5. 端到端健康检查 ──
blue "🩺 [5/5] 端到端健康检查"
# 通过 gateway 网关测
for path in "/actuator/health" "/api/ai/intro" "/api/ai/seed/check"; do
  status=$(curl -s --max-time 5 -o /dev/null -w "%{http_code}" "http://localhost:7080$path" 2>/dev/null)
  if [ "$status" = "200" ]; then
    green "  ✓ GET $path -> $status"
  else
    yellow "  ⚠  GET $path -> $status"
  fi
done
echo ""

# ── 总结 ──
bold "═══════════════════════════════════════════════"
if [ "$STARTED_FAIL" -eq 0 ] && [ "$ERROR_COUNT" -lt 5 ]; then
  green "✅ 部署日志检查通过"
  green "   启动成功: $STARTED_OK, 失败: $STARTED_FAIL"
  green "   错误数: $ERROR_COUNT (阈值 < 5)"
  green "   日志位置: $LOG_DIR"
  exit 0
else
  red "⚠️  部署日志检查有问题"
  red "   启动失败: $STARTED_FAIL"
  red "   错误数: $ERROR_COUNT"
  red "   日志位置: $LOG_DIR"
  red "   查看: cat $LOG_DIR/<service>.log"
  exit 1
fi
