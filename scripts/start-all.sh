#!/bin/bash
# =============================================================
# MiniMax Platform V3.5.19 一键启动脚本
# 13 微服务 (宿主机模式, h2local profile)
# 用法: bash scripts/start-all.sh [start|stop|status|restart]
# =============================================================
set -e

export JAVA_HOME=/opt/jdk-tmp/jdk-17
export PATH=$JAVA_HOME/bin:/opt/maven/apache-maven-3.9.6/bin:$PATH
BACKEND=/workspace/miniLiugl/backend
LOG_DIR=/tmp/minimax-logs
mkdir -p "$LOG_DIR"

# 13 服务端口 (gateway 8080, 业务 8081-8095)
declare -A SERVICES=(
  [gateway]=7080
  [auth]=8081
  [chat]=8082
  [model]=8084
  [rag]=8085
  [multimodal]=8087
  [agent]=8088
  [monitor]=8089
  [admin]=8090
  [analytics]=8092
  [pipeline]=8093
  [ai]=8094
  [ws]=8095
)

# 启动参数 (统一)
JVM_OPTS="-Xmx192m"
SPRING_OPTS="--spring.profiles.active=h2local --spring.cloud.nacos.discovery.enabled=false --spring.cloud.nacos.config.enabled=false --otel.metrics.exporter=none --otel.traces.exporter=none --otel.logs.exporter=none --otel.java.global-autoconfigure.enabled=false --management.tracing.enabled=false"

start_all() {
  echo "═══════════════════════════════════════════════"
  echo "  启动 13 微服务 (V3.5.19)"
  echo "═══════════════════════════════════════════════"
  for svc in gateway auth chat model rag multimodal agent monitor admin analytics pipeline ai ws; do
    port=${SERVICES[$svc]}
    if [ ! -f "$BACKEND/minimax-$svc/target/minimax-$svc-spring-boot.jar" ]; then
      echo "  ⏭️  $svc jar 不存在, 跳过 (需先 mvn package)"
      continue
    fi
    if pgrep -f "minimax-$svc-spring-boot.jar" > /dev/null; then
      echo "  ✓ $svc 已在运行 (port $port)"
      continue
    fi
    echo "  ▶ 启动 $svc (port $port)..."
    cd "$BACKEND"
    nohup $JAVA_HOME/bin/java $JVM_OPTS -jar "minimax-$svc/target/minimax-$svc-spring-boot.jar" \
      $SPRING_OPTS --server.port=$port \
      < /dev/null > "$LOG_DIR/$svc.log" 2>&1 &
    disown
  done
  echo ""
  echo "启动命令已发出, 等 60s 让 Spring Boot 完成初始化..."
  sleep 60
  echo ""
  echo "═══════════════════════════════════════════════"
  echo "  服务状态"
  echo "═══════════════════════════════════════════════"
  status_all
}

stop_all() {
  echo "═══════════════════════════════════════════════"
  echo "  停止 13 微服务"
  echo "═══════════════════════════════════════════════"
  for svc in gateway auth chat model rag multimodal agent monitor admin analytics pipeline ai ws; do
    if pgrep -f "minimax-$svc-spring-boot.jar" > /dev/null; then
      pkill -f "minimax-$svc-spring-boot.jar" 2>/dev/null
      echo "  ✓ $svc 已停止"
    else
      echo "  - $svc 未运行"
    fi
  done
}

status_all() {
  for svc in gateway auth chat model rag multimodal agent monitor admin analytics pipeline ai ws; do
    port=${SERVICES[$svc]}
    if pgrep -f "minimax-$svc-spring-boot.jar" > /dev/null; then
      code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/api/v1/$svc" 2>/dev/null || echo "000")
      printf "  ✓ %-12s port=%s  http=%s\n" "$svc" "$port" "$code"
    else
      printf "  ✗ %-12s port=%s  (未运行)\n" "$svc" "$port"
    fi
  done
}

case "${1:-start}" in
  start)   start_all ;;
  stop)    stop_all ;;
  status)  status_all ;;
  restart) stop_all; sleep 3; start_all ;;
  *)       echo "用法: $0 {start|stop|status|restart}"; exit 1 ;;
esac
