#!/bin/bash
# =============================================================
# MiniMax Platform V6.8.2 — 本地启动脚本 (h2local 沙箱模式)
# 
# 用法:
#   ./start-all.sh           启动全部服务
#   ./start-all.sh --stop   停止全部服务
#   ./start-all.sh auth      只启动 auth
#
# 要求: JDK 17+, Maven 3.9+
# =============================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# 颜色
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; NC='\033[0m'

log()  { echo -e "${CYAN}[$(date '+%H:%M:%S')]${NC} $1"; }
ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
err()  { echo -e "${RED}[ERR]${NC} $1"; }

# 检验 JDK
if ! java -version 2>&1 | grep -q "version \"17"; then
  warn "JDK 17+ recommended, current: $(java -version 2>&1 | head -1)"
fi

# JWT 密钥 (每个服务独立生成)
JWT_SECRET="${MINIMAX_JWT_SECRET:-$(openssl rand -hex 32)}"

# ============================================================
# 停止
# ============================================================
stop_all() {
  log "Stopping all services..."
  for svc in auth chat model rag agent admin ai analytics monitor pipeline ws gateway multimodal; do
    pidf="/tmp/minimax-${svc}.pid"
    if [ -f "$pidf" ]; then
      pid=$(cat "$pidf")
      if kill -0 "$pid" 2>/dev/null; then
        kill "$pid" 2>/dev/null && ok "$svc stopped" || warn "$svc kill failed"
      fi
      rm -f "$pidf"
    fi
  done
  ok "All stopped"
}

# ============================================================
# 启动单个服务
# 用法: start_service <name> <port> <db_name> [wait]
# ============================================================
start_service() {
  local name=$1 port=$2 db=$3 wait=${4:-true}
  
  local jar="$SCRIPT_DIR/$name/target/$name-spring-boot.jar"
  local logf="/tmp/minimax-${name}.log"
  local pidf="/tmp/minimax-${name}.pid"
  
  if [ ! -f "$jar" ]; then
    err "$name JAR not found: $jar"
    return 1
  fi
  
  # 端口检查
  if command -v lsof >/dev/null 2>&1; then
    if lsof -i ":$port" >/dev/null 2>&1; then
      ok "$name already running on port $port"
      return 0
    fi
  fi
  
  log "Starting $name (port $port, db=$db)..."
  
  nohup java \
    -Xms128m -Xmx256m \
    -Dminimax.jwt.secret="$JWT_SECRET" \
    -jar "$jar" \
    --spring.profiles.active=h2local \
    --spring.datasource.url="jdbc:h2:mem:${db};MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1" \
    --spring.datasource.driver-class-name=org.h2.Driver \
    --spring.datasource.username=sa \
    --spring.datasource.password="" \
    > "$logf" 2>&1 &
  
  echo $! > "$pidf"
  log "$name PID: $(cat $pidf)"
  
  if [ "$wait" = "true" ]; then
    log "Waiting for $name to start (up to 60s)..."
    for i in $(seq 1 20); do
      if curl -sf --max-time 2 "http://localhost:$port/ping" >/dev/null 2>&1 || \
         curl -sf --max-time 2 "http://localhost:$port/" -o /dev/null 2>&1; then
        ok "$name ready on port $port"
        return 0
      fi
      if ! kill -0 $(cat $pidf) 2>/dev/null; then
        err "$name crashed. Log:"
        tail -5 "$logf"
        return 1
      fi
      sleep 3
    done
    warn "$name may not be fully started yet"
  fi
}

# ============================================================
# 主逻辑
# ============================================================
case "${1:-start}" in
  --stop|stop)
    stop_all
    exit 0
    ;;
  auth|model|chat|rag|agent|admin|ai|analytics|monitor|pipeline|ws|gateway|multimodal)
    # 启动单个服务
    log "Starting single service: $1"
    ;;
  *)
    log "Starting all MiniMax Platform V6.8.2 services (h2local mode)..."
    ;;
esac

# ============================================================
# 服务定义: name | port | db_name | depends
# ============================================================
declare -A SERVICE_PORT=( [auth]=8081 [chat]=8082 [model]=8084 [rag]=8085 [agent]=8088 [admin]=8090 [ai]=8094 [analytics]=8092 [monitor]=8089 [pipeline]=8093 [ws]=8095 [gateway]=7080 [multimodal]=8087 )
declare -A SERVICE_DB=(  [auth]=auth [chat]=chat [model]=model [rag]=rag [agent]=agent [admin]=admin [ai]=ai [analytics]=analytics [monitor]=monitor [pipeline]=pipeline [ws]=ws [gateway]=gateway [multimodal]=multimodal )

# 编译 (如果需要)
if [ "$1" = "" ] || [ "$1" = "start" ]; then
  log "Building all modules..."
  for svc in auth chat model rag agent admin ai analytics monitor pipeline ws gateway multimodal; do
    if [ ! -f "$SCRIPT_DIR/$svc/target/$svc-spring-boot.jar" ]; then
      log "Building $svc..."
      mvn package -pl "$svc" -DskipTests -q 2>&1 | tail -1
    fi
  done
fi

# 启动服务
if [ "$1" = "start" ] || [ "$1" = "" ]; then
  # 启动顺序: auth → model → chat → rag → others
  start_service auth 8081 auth
  sleep 5
  start_service model 8084 model
  sleep 3
  start_service chat 8082 chat
  sleep 3
  start_service rag 8085 rag
  sleep 3
  start_service agent 8088 agent
  sleep 3
  start_service admin 8090 admin
  
  # 后台服务
  start_service ai 8094 ai false
  start_service analytics 8092 analytics false
  start_service monitor 8089 monitor false
  start_service pipeline 8093 pipeline false
  start_service ws 8095 ws false
  start_service gateway 7080 gateway false
  start_service multimodal 8087 multimodal false
  
  echo ""
  echo "============================================================"
  echo " MiniMax Platform V6.8.2 — 全部服务已启动"
  echo "============================================================"
  echo " 默认账号: adminLiugl / Liugl@2026"
  echo "============================================================"
  echo ""
fi

if [ "$1" = "auth" ]; then
  start_service auth 8081 auth
fi

if [ "$1" = "model" ]; then
  start_service model 8084 model
fi

echo ""
log "日志文件: /tmp/minimax-<service>.log"
log "停止: ./start-all.sh --stop"
