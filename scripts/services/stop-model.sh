#!/usr/bin/env bash
SVC="model"
PID_FILE=".docker-data/logs/${SVC}.pid"
if [[ -f "$PID_FILE" ]]; then
  PID=$(cat "$PID_FILE")
  if kill -0 $PID 2>/dev/null; then
    echo "🛑 停止 $SVC (PID=$PID)"
    kill $PID
    sleep 2
    kill -0 $PID 2>/dev/null && kill -9 $PID
  fi
  rm -f "$PID_FILE"
fi
echo "✅ $SVC 已停止"
