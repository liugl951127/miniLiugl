#!/usr/bin/env bash
# =============================================================
# MiniMax Platform 宿主机 Nginx 部署 (V3.5.5+)
# 独立脚本, 适配:
#   - CentOS Stream 9 / RHEL 9 (dnf + nginx 1.20+)
#   - Ubuntu 20+ / Debian 11+ (apt + nginx 1.18+)
#
# 用法:
#   sudo ./nginx/install-nginx.sh install    # 一键装 + 配置 + 启动
#   sudo ./nginx/install-nginx.sh config     # 只重新加载配置
#   sudo ./nginx/install-nginx.sh start      # 启动
#   sudo ./nginx/install-nginx.sh stop       # 停止
#   sudo ./nginx/install-nginx.sh restart    # 重启
#   sudo ./nginx/install-nginx.sh status     # 状态
#   sudo ./nginx/install-nginx.sh logs       # 实时日志
#   sudo ./nginx/install-nginx.sh uninstall  # 卸载
#
# 部署后访问 (单端口 80, 反代 docker 后端):
#   http://<server-ip>/            # 前端 SPA
#   http://<server-ip>/healthz     # 健康检查
# =============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
NGINX_CONF_SRC="$SCRIPT_DIR/nginx.conf"
UPSTREAM_CONF_SRC="$SCRIPT_DIR/upstream.conf"
DEFAULT_NGINX_DIR="/etc/nginx"
MINIMAX_CONF="$DEFAULT_NGINX_DIR/conf.d/minimax.conf"
UPSTREAM_CONF="$DEFAULT_NGINX_DIR/conf.d/minimax-upstream.conf"
FRONTEND_SRC="$PROJECT_DIR/frontend/dist"
FRONTEND_DST="/usr/share/nginx/html"
LOG_DIR="/var/log/minimax"

# 颜色
green()  { echo -e "\033[32m$*\033[0m"; }
red()    { echo -e "\033[31m$*\033[0m"; }
yellow() { echo -e "\033[33m$*\033[0m"; }
bold()   { echo -e "\033[1m$*\033[0m"; }

# 检测系统
detect_os() {
  if [ -f /etc/centos-release ] || [ -f /etc/redhat-release ]; then
    echo "centos"
  elif [ -f /etc/debian_version ]; then
    echo "debian"
  else
    echo "unknown"
  fi
}

# 检查 root
check_root() {
  if [ "$EUID" -ne 0 ]; then
    red "❌ 需要 root 权限, 请用: sudo $0 $*"
    exit 1
  fi
}

# 安装 nginx
do_install() {
  check_root
  bold ""
  bold "📦 安装并配置宿主机 Nginx"
  bold ""

  local os=$(detect_os)
  yellow "检测系统: $os"

  # 1. 装 nginx
  if ! command -v nginx &>/dev/null; then
    bold "  [1/5] 安装 nginx"
    case $os in
      centos)
        dnf install -y nginx
        systemctl enable nginx
        ;;
      debian)
        apt-get update -qq
        apt-get install -y -qq nginx
        systemctl enable nginx
        ;;
      *)
        red "  ❌ 未知系统, 请手动安装 nginx"
        exit 1
        ;;
    esac
    green "  ✓ nginx 已安装"
  else
    green "  [1/5] nginx 已存在: $(nginx -v 2>&1 | cut -d' ' -f3)"
  fi

  # 2. 准备目录
  bold "  [2/5] 准备配置目录"
  mkdir -p $DEFAULT_NGINX_DIR/conf.d
  mkdir -p $FRONTEND_DST
  mkdir -p $LOG_DIR/nginx
  green "  ✓ 目录就绪"

  # 3. 自动生成 upstream + 反向代理 (扫描所有后端服务, 16 个 upstream)
  bold "  [3/5] 自动生成 upstream + 反向代理 (扫描所有后端服务)"
  if [ -f "$PROJECT_DIR/scripts/gen-nginx-config.sh" ]; then
    yellow "   扫描后端服务 (16 个 minimax-* 模块), 生成 nginx 配置..."
    bash $PROJECT_DIR/scripts/gen-nginx-config.sh 2>&1 | tail -5 || true
  fi
  if [ -f "$UPSTREAM_CONF_SRC" ]; then
    cp "$UPSTREAM_CONF_SRC" "$UPSTREAM_CONF"
    SVC_COUNT=$(grep -c "^upstream" "$UPSTREAM_CONF" 2>/dev/null || echo 0)
    green "  ✓ upstream: $UPSTREAM_CONF ($SVC_COUNT 个 upstream)"
  else
    yellow "  ⚠  upstream.conf 不存在"
  fi

  # 4. 部署主配置
  bold "  [4/5] 部署主配置 (反向代理 + SPA + 静态资源)"
  if [ ! -f "$NGINX_CONF_SRC" ]; then
    red "  ❌ $NGINX_CONF_SRC 不存在"
    exit 1
  fi
  cp "$NGINX_CONF_SRC" "$MINIMAX_CONF"

  # 5. 复制前端 dist (如果存在)
  bold "  [5/5] 复制前端 dist"
  if [ -d "$FRONTEND_SRC" ]; then
    rm -rf $FRONTEND_DST/*
    cp -r $FRONTEND_SRC/* $FRONTEND_DST/
    green "  ✓ 前端 dist → $FRONTEND_DST/ ($(du -sh $FRONTEND_DST 2>/dev/null | cut -f1))"
  else
    yellow "  ⚠  frontend/dist 不存在, 前端未构建 (跑: cd frontend && npm run build)"
  fi

  # 6. 验证 + 启动
  bold ""
  bold "🚀 验证配置 + 启动 nginx"
  nginx -t
  systemctl enable nginx
  systemctl restart nginx
  sleep 1

  if systemctl is-active --quiet nginx; then
    green ""
    green "✅ Nginx 部署成功!"
    green ""
    green "🌐 访问入口:"
    green "   前端 SPA:     http://<server-ip>/"
    green "   健康检查:     http://<server-ip>/healthz"
    green "   Auth API:     http://<server-ip>/api/v1/auth/*  → 127.0.0.1:8081"
    green "   AI API:       http://<server-ip>/api/v1/ai/*    → 127.0.0.1:8094"
    green "   Gateway API:  http://<server-ip>/api/v1/*       → 127.0.0.1:7080"
    echo ""
    green "🔑 默认账号: adminLiugl / Liugl@2026"
  else
    red "❌ nginx 启动失败, 查看: journalctl -u nginx -n 30"
    exit 1
  fi
}

# 重新加载配置
do_config() {
  check_root
  bold ""
  bold "🔄 重新加载 nginx 配置"
  nginx -t && systemctl reload nginx
  green "✅ 配置已重新加载"
}

# 启动
do_start() {
  check_root
  if ! command -v nginx &>/dev/null; then
    red "❌ nginx 未安装, 先跑: $0 install"
    exit 1
  fi
  systemctl start nginx
  green "✅ nginx 已启动"
}

# 停止
do_stop() {
  check_root
  systemctl stop nginx
  green "✅ nginx 已停止"
}

# 重启
do_restart() {
  check_root
  systemctl restart nginx
  green "✅ nginx 已重启"
}

# 状态
do_status() {
  if ! command -v nginx &>/dev/null; then
    red "❌ nginx 未安装"
    return 1
  fi
  bold ""
  bold "📊 Nginx 状态"
  bold ""
  if systemctl is-active --quiet nginx 2>/dev/null; then
    green "  运行中: $(systemctl is-active nginx)"
  else
    red "  未运行"
  fi
  echo "  PID:    $(pgrep -f 'nginx: master' | head -1)"
  echo "  配置:   $MINIMAX_CONF"
  echo "  upstream: $UPSTREAM_CONF"
  echo "  前端:   $FRONTEND_DST"
  echo ""
  bold "  端口 80 监听:"
  ss -ltn 2>/dev/null | grep ":80 " | head -3
  echo ""
  bold "  健康检查:"
  curl -sf --max-time 3 http://localhost/healthz && echo " (OK)" || red "  ❌ /healthz 失败"
  echo ""
}

# 实时日志
do_logs() {
  journalctl -u nginx -f
}

# 卸载
do_uninstall() {
  check_root
  yellow "⚠  即将卸载 nginx (配置保留, 仅卸载包)"
  read -p "确认卸载? (输入 yes 继续): " confirm
  if [ "$confirm" = "yes" ]; then
    systemctl stop nginx 2>/dev/null || true
    case $(detect_os) in
      centos) dnf remove -y nginx ;;
      debian) apt-get remove -y -qq nginx ;;
    esac
    green "✅ nginx 已卸载"
  else
    yellow "❌ 取消"
  fi
}

case "${1:-install}" in
  install)    do_install ;;
  config|reload) do_config ;;
  start)      do_start ;;
  stop)       do_stop ;;
  restart)    do_restart ;;
  status)     do_status ;;
  logs)       do_logs ;;
  uninstall)  do_uninstall ;;
  *)          red "用法: $0 {install|config|start|stop|restart|status|logs|uninstall}"; exit 1 ;;
esac
