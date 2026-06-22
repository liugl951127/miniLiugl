#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - Docker 一键部署 (V1 简化版)
#
# 特性:
#   - 一条命令拉起全部: nacos + redis + mysql + 15 微服务 + nginx
#   - 端口 80 浏览器直接访问
#   - 容器网络互通 (gateway 用 nacos:8848, mysql 用 mysql:3306 等)
#   - MySQL 容器首次启动自动执行 sql/init-minimax.sql
#
# 用法:
#   chmod +x deploy-simple/docker-deploy.sh
#   ./deploy-simple/docker-deploy.sh up           # 启动全部
#   ./deploy-simple/docker-deploy.sh down         # 停止
#   ./deploy-simple/docker-deploy.sh logs [svc]   # 日志
#   ./deploy-simple/docker-deploy.sh ps           # 状态
#   ./deploy-simple/docker-deploy.sh rebuild      # 强制重新构建镜像
#
# 前置:
#   - Docker 20+ (含 docker compose v2)
#   - 至少 8 GB 内存, 30 GB 磁盘
# =============================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_ok()   { echo -e "${GREEN}[✓]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[!]${NC} $*"; }
log_err()  { echo -e "${RED}[✗]${NC} $*"; }

ACTION="${1:-up}"
SERVICE="${2:-}"

# ============================================================
# 前置检查
# ============================================================
preflight() {
  log_info "==== 前置检查 ===="
  if ! command -v docker &>/dev/null; then
    log_err "docker 未安装"
    exit 1
  fi
  DOCKER_VER=$(docker --version | awk '{print $3}' | tr -d ',')
  log_ok "Docker $DOCKER_VER"

  if ! docker compose version &>/dev/null; then
    log_err "docker compose v2 未安装"
    exit 1
  fi
  log_ok "Docker Compose $(docker compose version | awk '{print $4}')"

  # 检查端口冲突
  for port in 80 3306 6379 8848 8080 8081 8097 9090; do
    if ss -tlnp 2>/dev/null | grep -q ":$port "; then
      log_warn "端口 $port 已被占用, 可能冲突"
    fi
  done

  # 检查镜像缓存
  IMAGES=$(docker images --format '{{.Repository}}' 2>/dev/null | grep -c minimax- || true)
  log_info "已构建 minimax-* 镜像数: $IMAGES"
  echo ""
}

# ============================================================
# 构建前端 (dist 会被 nginx 容器挂载)
# ============================================================
build_frontend() {
  log_info "==== 打包前端 ===="
  cd "$PROJECT_ROOT/frontend"

  if [ ! -d node_modules ] || [ package.json -nt node_modules ]; then
    log_info "安装 npm 依赖..."
    npm config set registry https://registry.npmmirror.com 2>/dev/null || true
    npm install --silent 2>&1 | tail -3
  fi

  # Vite 3+ terser 变可选
  if ! grep -q '"terser"' package.json; then
    log_info "安装 terser..."
    npm install terser --save-dev --silent 2>&1 | tail -2
  fi

  log_info "vite build..."
  npm run build 2>&1 | tail -10
  log_ok "前端 dist/ 已生成 ($(du -sh dist 2>/dev/null | awk '{print $1}'))"
  cd "$PROJECT_ROOT"
  echo ""
}

# ============================================================
# up
# ============================================================
do_up() {
  preflight
  build_frontend

  log_info "==== docker compose up -d (并行构建镜像) ===="
  log_warn "首次启动需要 5-15 分钟 (编译 15 个微服务 + 拉镜像)"
  docker compose up -d --build

  echo ""
  log_info "==== 等待服务就绪 (健康检查 60s) ===="
  sleep 30
  log_info "检查状态..."
  docker compose ps

  echo ""
  echo "=========================================="
  echo "  🎉 MiniMax Platform 部署完成"
  echo "=========================================="
  echo ""
  echo "  访问:  http://localhost  (端口 80)"
  echo "  Nacos: http://localhost:8848/nacos  (nacos/nacos)"
  echo "  默认账号: admin / admin@123"
  echo ""
  echo "  常用命令:"
  echo "    $0 ps                                # 状态"
  echo "    $0 logs auth                         # auth 日志"
  echo "    $0 logs gateway                      # gateway 日志"
  echo "    $0 down                              # 停止"
  echo ""
}

# ============================================================
# down
# ============================================================
do_down() {
  log_info "==== docker compose down ===="
  docker compose down
  log_ok "已停止"
}

# ============================================================
# logs
# ============================================================
do_logs() {
  if [ -z "$SERVICE" ]; then
    docker compose logs -f --tail=100
  else
    docker compose logs -f --tail=200 "$SERVICE"
  fi
}

# ============================================================
# ps
# ============================================================
do_ps() {
  docker compose ps
}

# ============================================================
# rebuild (重新构建镜像)
# ============================================================
do_rebuild() {
  preflight
  log_info "==== 强制重新构建镜像 ===="
  SERVICE_ARG="${SERVICE:-}"
  if [ -n "$SERVICE_ARG" ]; then
    docker compose build --no-cache "$SERVICE_ARG"
    docker compose up -d "$SERVICE_ARG"
  else
    docker compose build --no-cache
    docker compose up -d
  fi
  log_ok "重建完成"
}

# ============================================================
# restart
# ============================================================
do_restart() {
  if [ -z "$SERVICE" ]; then
    log_err "用法: $0 restart <service-name>"
    exit 1
  fi
  docker compose restart "$SERVICE"
}

# ============================================================
# 入口
# ============================================================
case "$ACTION" in
  up)      do_up ;;
  down)    do_down ;;
  logs)    do_logs ;;
  ps)      do_ps ;;
  rebuild) do_rebuild ;;
  restart) do_restart ;;
  *)
    echo "用法: $0 {up|down|logs [svc]|ps|rebuild [svc]|restart <svc>}"
    exit 1
    ;;
esac