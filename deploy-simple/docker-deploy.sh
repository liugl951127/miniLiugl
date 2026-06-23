#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - Docker 一键部署 (V1.9.4)
#
# 特性:
#   - 一条命令拉起全部: nacos + redis + mysql + 15 微服务 + nginx
#   - 端口 80 浏览器直接访问
#   - 容器网络互通 (gateway 用 nacos:8848, mysql 用 mysql:3306 等)
#   - MySQL 容器首次启动自动执行 sql/init-minimax.sql
#   - 兼容 CentOS Stream 9 / RHEL 9 / Ubuntu 20+ / Debian 11+
#   - V1.9.4: 不校验 nginx 配置 (nginx 在容器内, 无需宿主机 nginx)
#
# 用法:
#   chmod +x deploy-simple/docker-deploy.sh
#   ./deploy-simple/docker-deploy.sh up                   # 启动全部
#   ./deploy-simple/docker-deploy.sh down                 # 停止
#   ./deploy-simple/docker-deploy.sh logs [svc]           # 日志
#   ./deploy-simple/docker-deploy.sh ps                   # 状态
#   ./deploy-simple/docker-deploy.sh rebuild              # 强制重新构建镜像
#   sudo ./deploy-simple/docker-deploy.sh domain DOMAIN EMAIL   # 配置公网域名+HTTPS
#   sudo ./deploy-simple/docker-deploy.sh verify [DOMAIN]      # 验证完整链路
#   sudo ./deploy-simple/docker-deploy.sh frontend DOMAIN EMAIL # 宿主机nginx反代前端+gateway (V1.9.6 推荐)
#   sudo ./deploy-simple/docker-deploy.sh fix-80                # 修 80 端口冲突
#
# 前置:
#   - Docker 20+ (含 docker compose v2)
#   - 至少 8 GB 内存, 30 GB 磁盘
# =============================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 加载 OS 适配层 (CentOS Stream 9 / RHEL 9 识别)
. "$SCRIPT_DIR/os-detect.sh"
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

  # OS 探测 (CentOS Stream 9 / RHEL 9 / Ubuntu / Debian 自动适配)
  detect_os 2>/dev/null && os_info || log_warn "OS 探测失败, 继续"

  # Docker 检查
  if ! command -v docker &>/dev/null; then
    log_err "docker 未安装"
    echo ""
    case "$OS_ID" in
      centos|rhel|rocky|almalinux)
        echo "  CentOS Stream 9 / RHEL 9 安装:"
        echo "    sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo"
        echo "    sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin"
        echo "    sudo systemctl enable --now docker"
        echo "    sudo usermod -aG docker $USER  # 重新登录后生效"
        ;;
      ubuntu|debian)
        echo "  Ubuntu/Debian 安装:"
        echo "    sudo apt-get update"
        echo "    sudo apt-get install -y docker.io docker-compose-plugin"
        echo "    sudo systemctl enable --now docker"
        echo "    sudo usermod -aG docker $USER  # 重新登录后生效"
        ;;
    esac
    exit 1
  fi
  DOCKER_VER=$(docker --version | awk '{print $3}' | tr -d ',')
  log_ok "Docker $DOCKER_VER"

  if ! docker compose version &>/dev/null; then
    log_err "docker compose v2 未安装"
    exit 1
  fi
  log_ok "Docker Compose $(docker compose version | awk '{print $4}')"

  # V1.9.5: 智能端口冲突处理 - 宿主机 nginx 占用 80 时自动停掉
  if [ "${SKIP_PORT_CHECK:-0}" = "1" ]; then
    log_info "跳过端口冲突检查 (SKIP_PORT_CHECK=1)"
  else
    for port in 3306 6379 8848 7080 8081 8093 9090; do
      if ss -tlnp 2>/dev/null | grep -q ":$port "; then
        log_warn "端口 $port 已被占用, 可能冲突"
      fi
    done

    # 80 端口特殊: docker nginx 要用, 宿主机 nginx 必须让位
    if ss -tlnp 2>/dev/null | grep -q ":80 "; then
      log_warn "端口 80 已被占用"
      # 智能识别: 是不是宿主机 nginx?
      if systemctl is-active nginx 2>/dev/null | grep -q "^active$"; then
        log_info "检测到宿主机 nginx 正在运行"
        if [ "${AUTO_STOP_NGINX:-1}" = "1" ]; then
          log_info "自动停掉宿主机 nginx (AUTO_STOP_NGINX=1)"
          systemctl stop nginx 2>/dev/null || true
          systemctl disable nginx 2>/dev/null || true
          sleep 2
          if ss -tlnp 2>/dev/null | grep -q ":80 "; then
            log_err "停掉 nginx 后 80 仍被占用, 请手动处理"
            exit 1
          else
            log_ok "80 端口已释放"
          fi
        else
          log_err "请手动停止: sudo systemctl stop nginx"
          exit 1
        fi
      else
        # 不是 nginx, 是其他进程
        PORT80_PID=$(ss -tlnp 2>/dev/null | grep ":80 " | head -1 | grep -oP 'pid=\K[0-9]+' | head -1)
        if [ -n "$PORT80_PID" ]; then
          PROC=$(cat /proc/$PORT80_PID/comm 2>/dev/null)
          log_err "80 端口被进程 $PROC (PID $PORT80_PID) 占用, 请手动:"
          log_err "    sudo kill $PORT80_PID"
          log_err "  或: sudo lsof -i :80  # 找占用进程"
          exit 1
        else
          log_err "80 端口被未知进程占用, 请手动检查"
          exit 1
        fi
      fi
    fi
  fi

  # 检查镜像缓存
  IMAGES=$(docker images --format '{{.Repository}}' 2>/dev/null | grep -c minimax- || true)
  log_info "已构建 minimax-* 镜像数: $IMAGES"

  # V1.9.3: 检查数据目录
  DATA_ROOT="${DATA_ROOT:-/opt/minimax/data}"
  if [ ! -d "$DATA_ROOT" ]; then
    log_warn "数据目录 $DATA_ROOT 不存在"
    if [ -x "$SCRIPT_DIR/setup-data-dir.sh" ]; then
      log_info "自动运行 setup-data-dir.sh 创建..."
      bash "$SCRIPT_DIR/setup-data-dir.sh"
    else
      log_err "请手动创建: mkdir -p $DATA_ROOT/{mysql,redis,nacos,otel}"
      exit 1
    fi
  else
    log_ok "数据目录: $DATA_ROOT"
  fi
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
  up)         do_up ;;
  down)       do_down ;;
  logs)       do_logs ;;
  ps)         do_ps ;;
  rebuild)    do_rebuild ;;
  restart)    do_restart ;;
  domain)     exec "$SCRIPT_DIR/setup-public-domain.sh" "${@:2}" ;;
  verify)     exec "$SCRIPT_DIR/verify-public-domain.sh" "${@:2}" ;;
  frontend)   exec "$SCRIPT_DIR/setup-frontend-via-host-nginx.sh" "${@:2}" ;;
  fix-80)     exec "$SCRIPT_DIR/fix-port-80.sh" ;;
  *)
    echo "用法: $0 {up|down|logs [svc]|ps|rebuild [svc]|restart <svc>|domain DOMAIN EMAIL|verify [DOMAIN]|frontend DOMAIN EMAIL|fix-80}"
    exit 1
    ;;
esac