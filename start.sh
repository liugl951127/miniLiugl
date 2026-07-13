#!/usr/bin/env bash
# ============================================================
# MiniMax Platform V3.5.5+ 一键启动脚本
# 自动检测环境, 选择最优启动方式
#
# 优先级 (从轻到重):
#   1. H2 沙箱 (1 java 进程) — 任何环境都能跑, ~512MB
#   2. Docker 精简 6 容器       — 需要 Docker, ~1.5GB
#   3. Docker 完整 21 容器      — 需要 Docker, ~6GB
#
# 用法:
#   ./start.sh           # 智能模式 (默认 H2)
#   ./start.sh h2        # 强制 H2 沙箱
#   ./start.sh mini      # 强制 Docker 精简 6 容器
#   ./start.sh full      # 强制 Docker 完整 21 容器
#   ./start.sh stop      # 停止
#   ./start.sh status    # 状态
#   ./start.sh logs      # 日志
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 颜色
G="\033[32m"; R="\033[31m"; Y="\033[33m"; B="\033[34m"; N="\033[0m"
green() { echo -e "${G}$*${N}"; }
red()   { echo -e "${R}$*${N}"; }
yellow(){ echo -e "${Y}$*${N}"; }
blue()  { echo -e "${B}$*${N}"; }

# ============== 工具函数 ==============
log_step()  { blue; echo "▶ $*"; blue; }
log_ok()    { green "  ✅ $*"; }
log_warn()  { yellow "  ⚠️  $*"; }
log_err()   { red "  ❌ $*"; }

detect_memory_mb() {
  free -m 2>/dev/null | awk '/^Mem:/{print $2}' || echo "0"
}

has_docker() {
  command -v docker &>/dev/null && docker info &>/dev/null 2>&1
}

has_java() {
  command -v java &>/dev/null
}

port_used() {
  ss -ltn 2>/dev/null | grep -q ":$1 " && echo "yes" || echo "no"
}

wait_health() {
  local url="$1" name="$2" max="${3:-60}"
  log_step "等待 $name 健康 ($url, 最长 ${max}s)..."
  for i in $(seq 1 $max); do
    if curl -sf --max-time 2 "$url" &>/dev/null; then
      log_ok "$name 已就绪 (${i}s)"
      return 0
    fi
    sleep 1
  done
  log_warn "$name 未在 ${max}s 内就绪 (继续等待可能更久)"
  return 1
}

# ============== 模式 1: H2 沙箱 ==============
start_h2() {
  log_step "模式: H2 沙箱 (单 java 进程, 内存 ~512MB)"
  
  if ! has_java; then
    log_err "Java 未安装, 请先安装: apt install -y openjdk-17-jdk"
    exit 1
  fi
  if ! port_used 8094 | grep -q no; then
    log_err "端口 8094 已被占用, 请先停止旧服务: ./start.sh stop"
    exit 1
  fi
  
  # 找 jar
  local jar=$(ls backend/minimax-ai/target/minimax-ai-spring-boot.jar 2>/dev/null | head -1)
  if [ -z "$jar" ]; then
    log_step "首次启动, 构建 minimax-ai jar (约 60-120s)..."
    (cd backend && mvn -pl minimax-common,minimax-ai -am package -DskipTests \
       -Dspotless.check.skip=true -Djacoco.skip=true -q 2>&1 | tail -3)
    jar=$(ls backend/minimax-ai/target/minimax-ai-spring-boot.jar 2>/dev/null | head -1)
    [ -z "$jar" ] && { log_err "构建失败"; exit 1; }
  fi
  
  log_step "启动 minimax-ai (H2 内存模式)..."
  mkdir -p /tmp/loa
  nohup java -jar "$jar" \
    --spring.profiles.active=h2local \
    --server.port=8094 \
    > /tmp/loa/ai.log 2>&1 &
  echo $! > /tmp/loa/ai.pid
  log_ok "minimax-ai PID=$(cat /tmp/loa/ai.pid)"
  
  wait_health "http://localhost:8094/actuator/health" "minimax-ai" 90
  echo ""
  green "═════════════════════════════════════════════════════════"
  green "✅ H2 沙箱模式启动成功!"
  green "═════════════════════════════════════════════════════════"
  echo ""
  green "🌐 API:     http://localhost:8094"
  green "❤️  Health: http://localhost:8094/actuator/health"
  green "📖 Swagger: http://localhost:8094/swagger-ui.html"
  green "📋 Logs:    tail -f /tmp/loa/ai.log"
  echo ""
  green "🧪 快速测试:"
  echo "  curl http://localhost:8094/api/v1/ai/cluster/raft/state"
  echo "  curl http://localhost:8094/api/v1/ai/intent/list"
  echo ""
  green "🛑 停止: ./start.sh stop"
}

# ============== 模式 2: Docker 精简 6 容器 ==============
start_docker_mini() {
  log_step "模式: Docker 精简 6 容器 (mariadb + redis + auth + ai + gateway + nginx)"
  
  if ! has_docker; then
    log_err "Docker 未安装或未运行, 请先安装: https://docs.docker.com/engine/install/"
    exit 1
  fi
  
  # 端口检查
  for p in 80 3306 6379 7080 8081 8094; do
    port_used $p | grep -q yes && log_warn "端口 $p 已被占用, 可能冲突"
  done
  
  docker compose -f docker-compose.mini.yml -p minimax-mini up -d --build
  
  echo ""
  log_step "等待服务就绪..."
  wait_health "http://localhost/healthz" "nginx" 30
  wait_health "http://localhost:8094/actuator/health" "ai" 120
  wait_health "http://localhost:8081/actuator/health" "auth" 60
  wait_health "http://localhost:7080/actuator/health" "gateway" 30
  
  echo ""
  green "═════════════════════════════════════════════════════════"
  green "✅ Docker 精简模式启动成功 (6 容器, ~1.5GB)"
  green "═════════════════════════════════════════════════════════"
  echo ""
  green "🌐 前端控制台:  http://localhost/"
  green "🔌 API 网关:    http://localhost:7080"
  green "🔑 鉴权:        http://localhost:8081"
  green "🤖 AI:          http://localhost:8094"
  green "🗄 MariaDB:     localhost:3306 (root/root123456)"
  green "💾 Redis:       localhost:6379 (minimax_redis_2024)"
  echo ""
  green "🔑 默认账号: adminLiugl / Liugl@2026"
  green "📋 状态:   ./start.sh status"
  green "📋 日志:   ./start.sh logs"
  green "🛑 停止:   ./start.sh stop"
}

# ============== 模式 3: Docker 完整 21 容器 ==============
start_docker_full() {
  log_step "模式: Docker 完整 21 容器 (需要 8GB+ 内存)"
  
  if ! has_docker; then
    log_err "Docker 未安装"
    exit 1
  fi
  
  local mem=$(detect_memory_mb)
  if [ "$mem" -lt 6144 ]; then
    log_warn "检测到内存 ${mem}MB < 6GB, 完整模式可能 OOM"
    read -p "继续? [y/N] " yn
    [[ ! "$yn" =~ ^[Yy]$ ]] && exit 1
  fi
  
  if [ ! -f deploy.sh ]; then
    log_err "deploy.sh 不存在 (完整版部署脚本)"
    exit 1
  fi
  
  ./deploy.sh up
}

# ============== 智能选择 ==============
auto_select() {
  local mem=$(detect_memory_mb)
  log_step "环境检测: 内存 ${mem}MB, Docker=$(has_docker && echo 'yes' || echo 'no'), Java=$(has_java && echo 'yes' || echo 'no')"
  
  # 强制条件:
  # 1. < 1.2GB 内存 → 强制 H2
  # 2. 端口 80 占用 / Docker 缺失 → 强制 H2
  if [ "$mem" -lt 1200 ] || ! has_docker; then
    log_step "→ 自动选择 H2 沙箱 (单 java 进程)"
    start_h2
  elif [ "$mem" -lt 4096 ]; then
    log_step "→ 自动选择 Docker 精简 6 容器"
    start_docker_mini
  else
    log_step "→ 自动选择 Docker 完整 21 容器"
    start_docker_full
  fi
}

# ============== 停止 ==============
cmd_stop() {
  log_step "停止所有 MiniMax 服务"
  
  # H2 沙箱
  if [ -f /tmp/loa/ai.pid ]; then
    pid=$(cat /tmp/loa/ai.pid)
    if kill -0 $pid 2>/dev/null; then
      kill $pid 2>/dev/null || true
      log_ok "已停止 minimax-ai (PID=$pid)"
      rm -f /tmp/loa/ai.pid
    fi
  fi
  # 兜底: 杀所有 minimax jar
  pkill -f "minimax-.*spring-boot.jar" 2>/dev/null && log_ok "已停止残余 java 进程" || true
  
  # Docker 精简
  if has_docker && [ -f docker-compose.mini.yml ]; then
    if docker compose -f docker-compose.mini.yml -p minimax-mini ps 2>/dev/null | grep -q "Up"; then
      docker compose -f docker-compose.mini.yml -p minimax-mini down
      log_ok "已停止 Docker 精简 6 容器"
    fi
  fi
  
  # Docker 完整
  if has_docker && [ -f docker-compose.yml ]; then
    if docker compose -f docker-compose.yml -p minimax ps 2>/dev/null | grep -q "Up"; then
      docker compose -f docker-compose.yml -p minimax down
      log_ok "已停止 Docker 完整 21 容器"
    fi
  fi
  
  green "✅ 全部停止"
}

# ============== 状态 ==============
cmd_status() {
  log_step "服务状态"
  echo ""
  echo "=== H2 沙箱 (本进程) ==="
  if pgrep -f "minimax-ai-spring-boot.jar" >/dev/null; then
    pid=$(pgrep -f "minimax-ai-spring-boot.jar")
    green "  ✅ minimax-ai 运行中 (PID=$pid)"
    curl -sf --max-time 3 http://localhost:8094/actuator/health >/dev/null \
      && green "  ✅ http://localhost:8094 health OK" \
      || red   "  ❌ http://localhost:8094 health FAILED"
  else
    yellow "  ⏹  minimax-ai 未运行"
  fi
  
  if has_docker; then
    echo ""
    echo "=== Docker 精简 6 容器 ==="
    docker compose -f docker-compose.mini.yml -p minimax-mini ps 2>/dev/null || echo "  (未启动)"
    echo ""
    echo "=== Docker 完整 21 容器 ==="
    docker compose -f docker-compose.yml -p minimax ps 2>/dev/null | head -25 || echo "  (未启动)"
  fi
}

# ============== 日志 ==============
cmd_logs() {
  echo "=== H2 沙箱日志 ==="
  if [ -f /tmp/loa/ai.log ]; then
    tail -f /tmp/loa/ai.log
  else
    echo "(H2 沙箱未运行)"
  fi
}

# ============== 入口 ==============
case "${1:-auto}" in
  h2|h2local)   start_h2 ;;
  mini)         start_docker_mini ;;
  full)         start_docker_full ;;
  auto|"")      auto_select ;;
  stop)         cmd_stop ;;
  status)       cmd_status ;;
  logs)         cmd_logs ;;
  -h|--help|help)
    cat <<EOF
MiniMax Platform 一键启动脚本 V3.5.5+

用法: $0 [模式]

模式:
  (无)        智能自动选择 (根据内存和 Docker 可用性)
  h2          H2 沙箱 (单 java 进程, ~512MB, 任何环境)
  mini        Docker 精简 6 容器 (~1.5GB, 需要 Docker)
  full        Docker 完整 21 容器 (~6GB, 需要 Docker, 8GB+ 内存)

管理:
  stop        停止所有服务
  status      查看服务状态
  logs        查看日志

示例:
  $0           # 智能选择
  $0 h2        # 强制 H2 沙箱
  $0 mini      # 强制 Docker 精简
  $0 stop      # 停止
  $0 status    # 状态

EOF
    ;;
  *) red "未知模式: $1"; echo "运行 '$0 help' 查看帮助"; exit 1 ;;
esac
