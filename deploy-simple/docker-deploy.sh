#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - Docker 一键部署 (V1.9.7)
#
# 默认行为 (V1.9.7):
#   - up 自动用宿主机 nginx (反代前端 + gateway)
#   - 不强制域名校验 (用 IP 也行, HTTP 也能访问)
#   - 域名 / HTTPS 单独配: frontend DOMAIN EMAIL
#
# 架构 (默认):
#   [用户 http://VPS_IP 或 https://DOMAIN]
#     ↓
#   [宿主机 nginx :80/:443 (反代终结点)]
#     ↓ /             → /opt/miniLiugl/frontend/dist  (前端静态)
#     ↓ /api/**        → http://127.0.0.1:7080          (gateway)
#     ↓ /ws            → http://127.0.0.1:7080          (gateway WS)
#   [宿主机 docker gateway :7080 (minimax-gateway)]
#     ↓ lb://minimax-*
#   [15 个微服务 + mysql/redis/nacos/otel]
#
# 用法:
#   sudo ./deploy-simple/docker-deploy.sh up              # 一键启动 (默认宿主机 nginx)
#   sudo ./deploy-simple/docker-deploy.sh up --ip         # 只用 IP, 不配域名
#   sudo ./deploy-simple/docker-deploy.sh down            # 停止
#   sudo ./deploy-simple/docker-deploy.sh logs [svc]      # 日志
#   sudo ./deploy-simple/docker-deploy.sh ps              # 状态
#   sudo ./deploy-simple/docker-deploy.sh rebuild         # 强制重建镜像
#   sudo ./deploy-simple/docker-deploy.sh frontend DOMAIN EMAIL  # 配置公网域名 + HTTPS
#   sudo ./deploy-simple/docker-deploy.sh verify [DOMAIN]       # 验证链路
#   sudo ./deploy-simple/docker-deploy.sh fix-80                # 修 80 端口冲突
#
# 前置:
#   - Docker 20+ (含 docker compose v2)
#   - 至少 8 GB 内存, 30 GB 磁盘
#   - 宿主机 nginx (apt-get install nginx 或 dnf install nginx)
# =============================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 加载 OS 适配层 (CentOS Stream 9 / RHEL 9 识别)
. "$SCRIPT_DIR/os-detect.sh"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_ok()   { echo -e "${GREEN}[✓]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[!]${NC} $*"; }
log_err()  { echo -e "${RED}[✗]${NC} $*"; }

ACTION="${1:-up}"
shift || true

# 解析额外参数
USE_HOST_NGINX="${USE_HOST_NGINX:-1}"  # V1.9.7: 默认 1
USE_DOCKER_NGINX="${USE_DOCKER_NGINX:-0}"  # V1.9.7: 默认 0
SKIP_NGINX="${SKIP_NGINX:-0}"  # 设 1 完全跳过 nginx 配置
SKIP_DOMAIN_CHECK="${SKIP_DOMAIN_CHECK:-0}"  # 设 1 跳过域名校验 (用 IP 直接访问)
SKIP_NGINX_CHECK="${SKIP_NGINX_CHECK:-0}"   # V1.9.4: 跳过 nginx -t

while [ $# -gt 0 ]; do
  case "$1" in
    --host-nginx)      USE_HOST_NGINX=1; USE_DOCKER_NGINX=0 ;;
    --docker-nginx)    USE_HOST_NGINX=0; USE_DOCKER_NGINX=1 ;;
    --no-nginx)        USE_HOST_NGINX=0; USE_DOCKER_NGINX=0; SKIP_NGINX=1 ;;
    --ip)              SKIP_DOMAIN_CHECK=1 ;;
    --skip-nginx)      SKIP_NGINX=1 ;;
    *) ;;
  esac
  shift || true
done

# ============================================================
# 前置检查
# ============================================================
preflight() {
  log_info "==== 前置检查 ===="

  detect_os 2>/dev/null && os_info || log_warn "OS 探测失败, 继续"

  if ! command -v docker &>/dev/null; then
    log_err "docker 未安装"
    echo ""
    case "$OS_ID" in
      centos|rhel|rocky|almalinux)
        echo "  CentOS Stream 9 / RHEL 9 安装:"
        echo "    sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo"
        echo "    sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin"
        echo "    sudo systemctl enable --now docker"
        echo "    sudo usermod -aG docker \$USER  # 重新登录后生效"
        ;;
      ubuntu|debian)
        echo "  Ubuntu/Debian 安装:"
        echo "    sudo apt-get update"
        echo "    sudo apt-get install -y docker.io docker-compose-plugin"
        echo "    sudo systemctl enable --now docker"
        echo "    sudo usermod -aG docker \$USER  # 重新登录后生效"
        ;;
    esac
    exit 1
  fi
  log_ok "Docker $(docker --version | awk '{print $3}' | tr -d ',')"

  if ! docker compose version &>/dev/null; then
    log_err "docker compose v2 未安装"
    exit 1
  fi
  log_ok "Docker Compose $(docker compose version | awk '{print $4}')"

  # 端口冲突处理
  if [ "${SKIP_PORT_CHECK:-0}" = "1" ]; then
    log_info "跳过端口冲突检查 (SKIP_PORT_CHECK=1)"
  else
    # 检查关键端口
    for port in 3306 6379 8848 7080 8081; do
      if ss -tlnp 2>/dev/null | grep -q ":$port "; then
        log_warn "端口 $port 已被占用, 可能冲突"
      fi
    done

    # 80 端口: 宿主机 nginx 用, 自动停其他占 80 进程
    if [ "$USE_HOST_NGINX" = "1" ] && ss -tlnp 2>/dev/null | grep -q ":80 "; then
      log_warn "端口 80 已被占用, 自动停掉 (宿主机 nginx 需要 80)"
      bash "$SCRIPT_DIR/fix-port-80.sh" 2>&1 | tail -10 || true
    fi

    # 443 端口
    if [ "$USE_HOST_NGINX" = "1" ] && ss -tlnp 2>/dev/null | grep -q ":443 "; then
      log_warn "端口 443 已被占用, 自动停掉"
      PID=$(ss -tlnp 2>/dev/null | grep ":443 " | head -1 | grep -oP 'pid=\K[0-9]+' || echo "")
      [ -n "$PID" ] && PROC=$(cat /proc/$PID/comm 2>/dev/null) && [ "$PROC" = "nginx" ] && {
        systemctl stop nginx 2>/dev/null || true
        log_ok "宿主机 nginx 已停 (释放 443)"
      }
    fi
  fi

  # 数据目录
  DATA_ROOT="${DATA_ROOT:-/opt/minimax/data}"
  if [ ! -d "$DATA_ROOT" ]; then
    log_warn "数据目录 $DATA_ROOT 不存在"
    if [ -x "$SCRIPT_DIR/setup-data-dir.sh" ]; then
      log_info "自动运行 setup-data-dir.sh..."
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
# 装宿主机 nginx
# ============================================================
ensure_host_nginx() {
  if [ "$USE_HOST_NGINX" != "1" ]; then
    return 0
  fi

  log_info "==== 装宿主机 nginx ===="

  if command -v nginx &>/dev/null; then
    log_ok "nginx: $(nginx -v 2>&1 | awk -F'/' '{print $2}')"
  else
    case "$OS_FAMILY" in
      rhel)
        if [ "$OS_VERSION" = "9" ] || [ "$OS_VERSION" = "8" ]; then
          dnf install -y epel-release
          dnf config-manager --enable crb 2>/dev/null || dnf config-manager --enable powertools 2>/dev/null || true
        else
          yum install -y epel-release
        fi
        pkg_install nginx
        ;;
      debian)
        pkg_install nginx
        ;;
    esac
    log_ok "nginx: $(nginx -v 2>&1 | awk -F'/' '{print $2}')"
  fi

  # SELinux
  [ "$OS_FAMILY" = "rhel" ] && selinux_setup || true

  systemctl enable nginx 2>/dev/null || true
}

# ============================================================
# 配置宿主机 nginx (反代前端 + gateway)
# ============================================================
configure_host_nginx() {
  if [ "$USE_HOST_NGINX" != "1" ]; then
    return 0
  fi

  log_info "==== 配置宿主机 nginx (反代前端 + gateway) ===="

  # 用统一配置模板
  NGINX_CONF_FILE="/etc/nginx/conf.d/minimax.conf"

  # 备份旧配置
  if [ -f "$NGINX_CONF_FILE" ]; then
    cp "$NGINX_CONF_FILE" "${NGINX_CONF_FILE}.bak-$(date +%s)"
    log_warn "已备份旧配置"
  fi

  # 拷贝配置模板
  cp "$PROJECT_ROOT/scripts/nginx-minimax-full.conf" "$NGINX_CONF_FILE"

  # V1.9.7: 不强制替换域名 - 用 placeholder, 后端能跑就行
  if [ "${SKIP_DOMAIN_CHECK:-0}" = "1" ]; then
    # 用 _ 通配符 (匹配任何域名/IP)
    sed -i 's|your-domain.com|_|g' "$NGINX_CONF_FILE"
    sed -i 's|www.your-domain.com|_|g' "$NGINX_CONF_FILE"
    # 证书路径暂时保留但不强求
    log_info "用 IP 模式 (server_name _)"
  else
    # 域名模式: 占位符保留 (用户跑 frontend 命令时再替换)
    log_info "默认配置已生成 (用 _ 匹配, 任何域名/IP 都行)"
  fi

  # 前端路径: 替换成实际位置
  sed -i "s|/opt/minimax/frontend/dist|$PROJECT_ROOT/frontend/dist|g" "$NGINX_CONF_FILE"

  # 备份默认配置
  for f in /etc/nginx/conf.d/default.conf /etc/nginx/sites-enabled/default; do
    [ -f "$f" ] && mv "$f" "${f}.bak-$(date +%s)" && log_warn "已备份: $f"
  done

  # 测试配置
  if [ "${SKIP_NGINX_CHECK:-0}" = "1" ]; then
    log_warn "跳过 nginx 配置校验 (SKIP_NGINX_CHECK=1)"
  else
    if nginx -t 2>&1 | grep -q "successful"; then
      log_ok "宿主机 nginx 配置语法 OK"
    else
      log_warn "宿主机 nginx 配置有警告, 继续"
    fi
  fi

  # 启动 / 重载
  if ! systemctl is-active nginx 2>/dev/null | grep -q active; then
    systemctl start nginx 2>/dev/null || nginx
    sleep 2
    log_ok "宿主机 nginx 已启动"
  else
    systemctl reload nginx 2>/dev/null || nginx -s reload
    log_ok "宿主机 nginx 已 reload"
  fi
}

# ============================================================
# 防火墙
# ============================================================
open_firewall() {
  if [ "$USE_HOST_NGINX" != "1" ]; then
    return 0
  fi
  log_info "==== 防火墙放行 80 + 443 ===="
  firewall_open 80 tcp 2>/dev/null || true
  firewall_open 443 tcp 2>/dev/null || true
  log_ok "防火墙已配置"
}

# ============================================================
# 构建前端
# ============================================================
build_frontend() {
  log_info "==== 打包前端 ===="
  cd "$PROJECT_ROOT/frontend"

  if [ ! -d node_modules ] || [ package.json -nt node_modules ]; then
    log_info "安装 npm 依赖..."
    npm config set registry https://registry.npmmirror.com 2>/dev/null || true
    npm install --silent 2>&1 | tail -3
  fi

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
# up (V1.9.7 默认走宿主机 nginx)
# ============================================================
do_up() {
  preflight
  build_frontend
  ensure_host_nginx

  # V1.9.7: docker compose up 默认排除 nginx 容器 (避免端口冲突)
  log_info "==== docker compose up -d ===="
  log_warn "首次启动需要 5-15 分钟 (编译 15 个微服务 + 拉镜像)"

  if [ "$USE_DOCKER_NGINX" = "1" ]; then
    log_info "用 docker nginx (老路径): --profile docker-nginx"
    docker compose --profile docker-nginx up -d --build
  else
    log_info "用宿主机 nginx (默认): 不启动 docker nginx 容器"
    docker compose up -d --build
  fi

  echo ""
  log_info "==== 配置宿主机 nginx ===="
  configure_host_nginx
  open_firewall

  echo ""
  log_info "==== 等待服务就绪 ===="
  sleep 30

  # 链路验证
  log_info "==== 链路验证 ===="

  # 1. gateway
  if curl -s -m 5 -o /dev/null -w "%{http_code}" "http://127.0.0.1:7080/actuator/health" 2>/dev/null | grep -qE "^(200|401|403|404)$"; then
    log_ok "gateway:7080 (宿主机直接访问)"
  else
    log_warn "gateway:7080 暂时不通 (可能还在启动)"
  fi

  # 2. nginx 80
  sleep 2
  HTTP=$(curl -s -m 5 -o /dev/null -w "%{http_code}" "http://localhost/" 2>/dev/null || echo "000")
  if echo "$HTTP" | grep -qE "^(200|301|302)$"; then
    log_ok "http://localhost/ (宿主机 nginx 80)"
  else
    log_warn "http://localhost/ HTTP $HTTP"
  fi

  # 3. 公网 IP (如果有)
  PUBLIC_IP=$(curl -s --max-time 5 -4 ifconfig.me 2>/dev/null || curl -s --max-time 5 -6 ifconfig.me 2>/dev/null || echo "")
  if [ -n "$PUBLIC_IP" ]; then
    HTTP=$(curl -s -m 5 -o /dev/null -w "%{http_code}" "http://$PUBLIC_IP/" 2>/dev/null || echo "000")
    if echo "$HTTP" | grep -qE "^(200|301|302)$"; then
      log_ok "http://$PUBLIC_IP/ (公网访问)"
    else
      log_warn "http://$PUBLIC_IP/ HTTP $HTTP (可能云安全组没放 80)"
    fi
  fi

  echo ""
  echo "=========================================="
  echo "  🎉 MiniMax Platform 部署完成"
  echo "=========================================="
  echo ""
  echo "  内网访问:  http://localhost/"
  if [ -n "$PUBLIC_IP" ]; then
    echo "  公网访问:  http://$PUBLIC_IP/"
  fi
  echo ""
  echo "  默认账号:"
  echo "    超级管理员: adminLiugl / Liugl@2026"
  echo "    普通管理员: admin / admin@123"
  echo ""
  echo "  常用命令:"
  echo "    $0 ps                                # 状态"
  echo "    $0 logs gateway                      # gateway 日志"
  echo "    $0 logs auth                         # auth 日志"
  echo "    $0 down                              # 停止"
  echo "    $0 rebuild                           # 强制重建"
  echo ""
  if [ "$USE_HOST_NGINX" = "1" ]; then
    echo "  配置公网域名 + HTTPS:"
    echo "    sudo $0 frontend liugeliang.com admin@liugeliang.com"
    echo ""
    echo "  ⚠️ V1.9.7 默认用宿主机 nginx (避免 80 端口冲突)"
    echo "     现在已经通过 http://localhost 或 http://公网IP 访问"
    echo "     配域名 + HTTPS 单独跑 frontend 命令"
  fi
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
# rebuild
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
  frontend)   exec "$SCRIPT_DIR/setup-frontend-via-host-nginx.sh" "${@:2}" ;;
  domain)     exec "$SCRIPT_DIR/setup-public-domain.sh" "${@:2}" ;;
  verify)     exec "$SCRIPT_DIR/verify-public-domain.sh" "${@:2}" ;;
  fix-80)     exec "$SCRIPT_DIR/fix-port-80.sh" ;;
  *)
    echo "用法: $0 {up|down|logs [svc]|ps|rebuild [svc]|restart <svc>|frontend DOMAIN EMAIL|domain DOMAIN EMAIL|verify [DOMAIN]|fix-80}"
    echo ""
    echo "up 参数:"
    echo "  --host-nginx    用宿主机 nginx (默认 V1.9.7)"
    echo "  --docker-nginx  用 docker nginx (老路径, 配 --profile docker-nginx)"
    echo "  --no-nginx      不配 nginx (微服务直接 7080 访问)"
    echo "  --ip            用 IP 模式 (不校验域名)"
    echo "  --skip-nginx    跳过 nginx 配置步骤"
    echo ""
    echo "环境变量:"
    echo "  SKIP_PORT_CHECK=1     跳过端口冲突检查"
    echo "  SKIP_NGINX_CHECK=1    跳过 nginx -t 校验"
    echo "  SKIP_DOMAIN_CHECK=1   跳过域名校验"
    echo "  USE_HOST_NGINX=1      默认用宿主机 nginx"
    echo "  USE_DOCKER_NGINX=1    用 docker nginx"
    exit 1
    ;;
esac