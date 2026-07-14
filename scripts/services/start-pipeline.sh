#!/usr/bin/env bash
# =============================================================
# pipeline 启动脚本 (V3.5.8)
# 端口: 8093, 内存: 384MB
# 依赖: common, auth, ai
# =============================================================
set -e

SVC="pipeline"
PORT=8093
HEAP=384
JAR="backend/minimax-${SVC}/target/minimax-${SVC}-spring-boot.jar"
LOG_DIR=".docker-data/logs"
PID_FILE="$LOG_DIR/${SVC}.pid"

# Java
if [[ -z "${JAVA_HOME:-}" ]]; then
  export JAVA_HOME=/opt/jdk-17
  export PATH=$JAVA_HOME/bin:$PATH
fi

# 日志
mkdir -p "$LOG_DIR"

# 检查 jar
if [[ ! -f "$JAR" ]]; then
  echo "❌ 未找到 $JAR"
  echo "   先编译: cd backend && mvn -pl minimax-${SVC} -am package -DskipTests"
  exit 1
fi

# 已有进程
if [[ -f "$PID_FILE" ]] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
  echo "⚠️ $SVC 已在运行 (PID=$(cat $PID_FILE))"
  exit 0
fi

# 启动
echo "🚀 启动 $SVC (port=$PORT, heap=${HEAP}MB)"
nohup java -Xms256m -Xmx${HEAP}m -XX:+UseG1GC -XX:MaxRAMPercentage=70 \
    -Dfile.encoding=UTF-8 \
    -jar "$JAR" \
    --spring.profiles.active=h2local --server.port=${PORT} \
    > "$LOG_DIR/${SVC}.log" 2>&1 &
PID=$!
echo $PID > "$PID_FILE"

# 健康检查
echo "⏳ 等待服务启动..."
for i in 1 2 3 4 5 6 7 8 9 10; do
  sleep 3
  if curl -s -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/actuator/health" 2>/dev/null | grep -q 200; then
    echo "✅ $SVC 已启动 (PID=$PID, http://localhost:${PORT})"
    echo "   日志: $LOG_DIR/${SVC}.log"
    exit 0
  fi
done
echo "❌ $SVC 启动超时, 查看日志: $LOG_DIR/${SVC}.log"
exit 1
