#!/bin/bash
# MiniMax 12 个微服务一键启动 (V5.3)
# 用法: bash scripts/dev-start.sh
set +e
LOG=/workspace/minimax-platform/logs/dev-start.log
mkdir -p /workspace/minimax-platform/logs
: > $LOG

start_one() {
  local module=$1
  local port=$2
  local jar=$(ls /workspace/minimax-platform/backend/minimax-${module}/target/minimax-${module}*.jar 2>/dev/null | grep -v original | head -1)
  if [ -z "$jar" ]; then
    echo "  ❌ $module (no jar)" | tee -a $LOG
    return 1
  fi
  if ss -tln 2>/dev/null | grep -q ":${port} "; then
    echo "  ⊙ $module :$port (already)" | tee -a $LOG
    return 0
  fi
  local logfile="/workspace/minimax-platform/logs/${module}.log"
  : > "$logfile"
  cd "$(dirname $jar)"
  nohup setsid java -jar "$(basename $jar)" -Dserver.port=${port} < /dev/null > "$logfile" 2>&1 &
  cd - > /dev/null
  disown 2>/dev/null
  echo "  ✓ $module :$port" | tee -a $LOG
  return 0
}

echo "=== 启动 12 个微服务 (端口 8081-8095) ===" | tee -a $LOG
start_one auth        8081
start_one chat        8082
start_one model       8083
start_one memory      8084
start_one rag         8085
start_one function    8086
start_one admin       8087
start_one multimodal  8088
start_one monitor     8089
start_one agent       8090
start_one prompt      8091
start_one ws          8095
echo "" | tee -a $LOG
echo "=== 等待启动 (30s) ===" | tee -a $LOG
sleep 30
echo "" | tee -a $LOG
echo "=== 端口监听状态 ===" | tee -a $LOG
ss -tlnp 2>&1 | grep -E ":80(8[0-9]|9[0-5])" | sort | tee -a $LOG