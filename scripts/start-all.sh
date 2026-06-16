#!/bin/bash
# MiniMax Platform - 启动所有微服务
# 端口 8081-8090
# 用 disown 防 SIGHUP 杀掉后台 java 进程

set +e

ROOT=/workspace/minimax-platform/backend
LOGS=/workspace/minimax-platform/logs/services
mkdir -p $LOGS

# 模块名 + 主类 + 端口 + 启动方式
MODULES=(
  "auth:com.minimax.auth.AuthApplication:8081:fat"
  "chat:com.minimax.chat.ChatApplication:8082:fat"
  "model:com.minimax.model.ModelApplication:8083:fat"
  "memory:com.minimax.memory.MemoryApplication:8084:fat"
  "rag:com.minimax.rag.RagApplication:8085:thin"
  "function:com.minimax.function.FunctionApplication:8086:thin"
  "admin:com.minimax.admin.AdminApplication:8087:thin"
  "multimodal:com.minimax.multimodal.MultimodalApplication:8088:thin"
  "monitor:com.minimax.monitor.MonitorApplication:8089:thin"
  "agent:com.minimax.agent.config.AgentApp:8090:thin"
)

for m in "${MODULES[@]}"; do
  IFS=':' read -r name main port type <<< "$m"

  if ss -lnt 2>/dev/null | grep -q ":$port "; then
    echo "⏭  $name (port $port 已被占用, 跳过)"
    continue
  fi

  echo "▶ 启动 $name (port $port, $type)"
  cd $ROOT/minimax-$name/target
  if [ "$type" = "fat" ]; then
    java -jar minimax-$name.jar --server.port=$port \
      > $LOGS/$name.log 2>&1 &
  else
    cp=$(cat /tmp/cp-minimax-minimax-$name.txt)
    java -cp "minimax-$name-1.0.0-SNAPSHOT.jar:$cp" $main --server.port=$port \
      > $LOGS/$name.log 2>&1 &
  fi
  disown
  echo "  PID=$!"
  sleep 1
done

cd $ROOT
echo ""
echo "等待服务就绪 (75s)..."
sleep 75

echo ""
echo "=== 健康检查 ==="
up=0
down=0
for m in "${MODULES[@]}"; do
  IFS=':' read -r name main port type <<< "$m"
  ok=0
  for path in /health /api/v1/health /api/v1/system/health; do
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 2 "http://127.0.0.1:$port$path" 2>/dev/null)
    if [ "$code" = "200" ]; then
      echo "✅ $name :$port  UP ($path)"
      ok=1
      up=$((up+1))
      break
    fi
  done
  if [ $ok -eq 0 ]; then
    echo "❌ $name :$port  DOWN (查看 $LOGS/$name.log)"
    down=$((down+1))
  fi
done

echo ""
echo "总计: $up UP / $down DOWN"
echo "日志位置: $LOGS/"
