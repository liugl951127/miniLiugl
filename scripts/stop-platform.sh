#!/bin/bash
# MiniMax Platform - 停止所有服务

set +e

echo "停止所有 MiniMax 服务..."

# 杀后端
for port in 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090; do
  pid=$(ss -tlnp 2>/dev/null | grep ":$port " | grep -oP 'pid=\K[0-9]+' | head -1)
  if [ -n "$pid" ]; then
    kill $pid 2>/dev/null
    echo "  停止 :$port (PID $pid)"
  fi
done

# 杀前端 (vite dev)
pid=$(ps -ef | grep -E "vite|npm run dev" | grep -v grep | awk '{print $2}' | head -1)
if [ -n "$pid" ]; then
  kill $pid 2>/dev/null
  echo "  停止前端 (PID $pid)"
fi

# 杀 MySQL (可选, 一般保留)
# pkill -f mysqld_safe

sleep 2
echo "✅ 全部停止"
