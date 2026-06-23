#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - 80 端口自动修复 (V1.9.5)
#
# 当 nginx 容器启动报 "address already in use" 时自动修复
# 自动识别占用进程并处理:
#   - 宿主机 nginx (systemctl stop)
#   - Apache httpd (systemctl stop)
#   - 其他进程 (kill)
# =============================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/os-detect.sh"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_ok()   { echo -e "${GREEN}[✓]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[!]${NC} $*"; }
log_err()  { echo -e "${RED}[✗]${NC} $*"; }

cd /opt/miniLiugl 2>/dev/null || cd "$(find / -name 'docker-compose.yml' -path '*/miniLiugl/*' 2>/dev/null | head -1 | xargs dirname)" || {
  log_err "找不到 miniLiugl 项目目录"
  exit 1
}

log_info "==== 1. 诊断: 80 端口被谁占用 ===="

PORT80_PID=""
PORT80_PROC=""

# 用多种方式找
if command -v ss &>/dev/null; then
  LINE=$(ss -tlnp 2>/dev/null | grep ":80 " | head -1)
  if [ -n "$LINE" ]; then
    PORT80_PID=$(echo "$LINE" | grep -oP 'pid=\K[0-9]+' | head -1)
  fi
elif command -v lsof &>/dev/null; then
  PORT80_PID=$(lsof -t -i:80 2>/dev/null | head -1)
fi

if [ -z "$PORT80_PID" ]; then
  log_ok "80 端口未被占用, 无需修复"
  log_info "尝试启动 nginx 容器..."
  cd /opt/miniLiugl && docker compose up -d nginx
  exit 0
fi

PORT80_PROC=$(cat /proc/$PORT80_PID/comm 2>/dev/null || echo "unknown")
log_warn "80 端口被占用: PID=$PORT80_PID, 进程=$PORT80_PROC"

log_info "==== 2. 修复 ===="

case "$PORT80_PROC" in
  nginx)
    log_info "检测到宿主机 nginx, 自动停掉..."
    systemctl stop nginx 2>/dev/null || true
    systemctl disable nginx 2>/dev/null || true
    log_ok "宿主机 nginx 已停止 + 禁用"
    ;;
  httpd|apache2)
    log_info "检测到 Apache httpd, 自动停掉..."
    systemctl stop httpd 2>/dev/null || systemctl stop apache2 2>/dev/null || true
    systemctl disable httpd 2>/dev/null || systemctl disable apache2 2>/dev/null || true
    log_ok "Apache httpd 已停止"
    ;;
  docker-proxy)
    log_info "是 docker-proxy (其他容器占用了 80)"
    CONTAINER=$(docker ps --filter "publish=80" --format "{{.Names}}" | head -1)
    log_warn "占用 80 的容器: ${CONTAINER:-未知}"
    if [ "$CONTAINER" = "minimax-nginx" ]; then
      log_info "重启 minimax-nginx 容器..."
      docker compose restart nginx
    else
      log_err "其他容器占用了 80, 请手动: docker stop $CONTAINER"
      exit 1
    fi
    ;;
  *)
    log_warn "未知进程, 尝试 kill..."
    kill -9 $PORT80_PID 2>/dev/null || true
    sleep 1
    ;;
esac

sleep 2

log_info "==== 3. 验证 80 端口已释放 ===="
if ss -tln 2>/dev/null | grep -q ":80 "; then
  log_err "80 端口仍被占用:"
  ss -tlnp 2>/dev/null | grep ":80 "
  log_err "请手动排查: ss -tlnp | grep ':80 '"
  exit 1
fi
log_ok "80 端口已释放"

log_info "==== 4. 启动 nginx 容器 ===="
cd /opt/miniLiugl
docker compose up -d nginx
sleep 5

log_info "==== 5. 验证 nginx 容器 ===="
docker ps --filter "name=nginx" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

log_info "==== 6. 测试访问 ===="
curl -I -m 5 http://localhost/ 2>&1 | head -3

echo ""
log_ok "修复完成"
echo ""
echo "如果还有问题:"
echo "  1. docker logs minimax-nginx --tail 30"
echo "  2. ./deploy-simple/fix-port-80.sh (再次运行)"
echo "  3. ./deploy-simple/docker-deploy.sh down && up (全量重启)"