#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - 宿主机 Nginx 一键部署 (V3.5.5+ 完整版)
# 适配: CentOS 9 / RHEL / Ubuntu 20+ / Debian 11+
#
# 包含:
#   - 16 个微服务 upstream (自动扫描生成)
#   - 前端 SPA 反向代理 (vue-router history fallback)
#   - WebSocket / SSE 长连接支持
#   - HTTPS (certbot 一键配置)
#   - 安全 headers (CSP, X-Frame-Options, etc.)
#   - 限流 / 缓存 / Gzip
#
# 用法:
#   sudo ./nginx/install-nginx.sh install     # 一键装 + 配置 + 启动
#   sudo ./nginx/install-nginx.sh config      # 重新加载配置
#   sudo ./nginx/install-nginx.sh status      # 状态
#   sudo ./nginx/install-nginx.sh logs        # 实时日志
#   sudo ./nginx/install-nginx.sh restart     # 重启
#   sudo ./nginx/install-nginx.sh https       # 配置 HTTPS
#   sudo ./nginx/install-nginx.sh uninstall   # 卸载
# =============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
NGINX_DIR="$SCRIPT_DIR"

# 颜色
green()  { echo -e "\033[32m$*\033[0m"; }
red()    { echo -e "\033[31m$*\033[0m"; }
yellow(){ echo -e "\033[33m$*\033[0m"; }
blue()  { echo -e "\033[36m$*\033[0m"; }
bold()  { echo -e "\033[1m$*\033[0m"; }

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

# ============== 安装 ==============

do_install() {
    check_root
    bold ""
    bold "📦 MiniMax Nginx 一键部署"
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
    bold "  [2/5] 准备目录"
    mkdir -p /etc/nginx/conf.d
    mkdir -p /etc/nginx/ssl
    mkdir -p /usr/share/nginx/html
    mkdir -p /var/log/nginx
    green "  ✓ 目录就绪"

    # 3. 自动生成 upstream + 主配置
    bold "  [3/5] 生成配置 (16 个 upstream + 主配置)"
    if [ -f "$PROJECT_DIR/scripts/gen-nginx-config.sh" ]; then
        yellow "   扫描后端服务..."
        bash $PROJECT_DIR/scripts/gen-nginx-config.sh 2>&1 | tail -3 || true
    fi

    # 4. 部署配置
    bold "  [4/5] 部署配置"
    cp "$NGINX_DIR/upstream.conf" /etc/nginx/conf.d/minimax-upstream.conf
    cp "$NGINX_DIR/nginx.conf" /etc/nginx/conf.d/minimax.conf
    cp "$NGINX_DIR/security-headers.conf" /etc/nginx/conf.d/minimax-security.conf 2>/dev/null || true
    green "  ✓ upstream.conf → /etc/nginx/conf.d/minimax-upstream.conf"
    green "  ✓ nginx.conf → /etc/nginx/conf.d/minimax.conf"
    green "  ✓ security-headers.conf → /etc/nginx/conf.d/minimax-security.conf"

    # 5. 复制前端
    bold "  [5/5] 复制前端 dist"
    if [ -d "$PROJECT_DIR/frontend/dist" ]; then
        rm -rf /usr/share/nginx/html/*
        cp -r $PROJECT_DIR/frontend/dist/* /usr/share/nginx/html/ 2>/dev/null || true
        green "  ✓ 前端 dist → /usr/share/nginx/html/ ($(du -sh /usr/share/nginx/html 2>/dev/null | cut -f1))"
    else
        yellow "  ⚠  frontend/dist 不存在 (跑: cd frontend && npm run build)"
    fi

    # 6. 验证 + 启动
    bold ""
    bold "🚀 验证配置 + 启动 nginx"
    nginx -t
    systemctl enable nginx
    systemctl restart nginx
    sleep 1

    if systemctl is-active --quiet nginx; then
        bold ""
        green "✅ Nginx 部署成功!"
        echo ""
        bold "🌐 访问入口:"
        green "   http://<server-ip>/                 # 前端 SPA"
        green "   http://<server-ip>/healthz          # 健康检查"
        green "   http://<server-ip>/api/v1/{module}/* # 16 个微服务"
        echo ""
        green "🔑 默认账号: adminLiugl / Liugl@2026"
    else
        red "❌ nginx 启动失败, 查看: journalctl -u nginx -n 30"
        exit 1
    fi
}

# ============== HTTPS ==============

do_https() {
    check_root
    bold ""
    bold "🔒 配置 HTTPS (certbot)"
    bold ""

    local domain="${1:-}"
    if [ -z "$domain" ]; then
        read -p "请输入域名 (e.g. example.com): " domain
    fi
    if [ -z "$domain" ]; then
        red "❌ 域名不能为空"
        exit 1
    fi

    # 1. 装 certbot
    if ! command -v certbot &>/dev/null; then
        case $(detect_os) in
            centos) dnf install -y certbot python3-certbot-nginx ;;
            debian) apt-get install -y -qq certbot python3-certbot-nginx ;;
        esac
    fi

    # 2. 申请证书
    yellow "申请证书: $domain"
    certbot --nginx -d "$domain" --non-interactive --agree-tos -m "admin@$domain" 2>&1 | tail -10 || {
        red "❌ 证书申请失败, 检查 DNS 是否解析到本机"
        exit 1
    }

    # 3. 续期 cron
    echo "0 3 * * * certbot renew --quiet" > /etc/cron.d/certbot-renew
    green "✅ HTTPS 已配置 (证书自动续期)"
    echo "  https://$domain/"
}

# ============== 其他命令 ==============

do_config() {
    check_root
    nginx -t && systemctl reload nginx
    green "✅ 配置已重载"
}

do_status() {
    if ! command -v nginx &>/dev/null; then
        red "❌ nginx 未安装"
        return 1
    fi
    bold ""
    bold "📊 Nginx 状态"
    bold ""
    echo "  版本:    $(nginx -v 2>&1 | cut -d' ' -f3)"
    if systemctl is-active --quiet nginx 2>/dev/null; then
        green "  状态:    运行中"
    else
        red "  状态:    未运行"
    fi
    echo "  配置:    /etc/nginx/conf.d/minimax.conf"
    echo "  upstream: /etc/nginx/conf.d/minimax-upstream.conf"
    echo "  前端:    /usr/share/nginx/html"
    echo ""
    bold "  upstream 数: $(grep -c '^upstream' /etc/nginx/conf.d/minimax-upstream.conf 2>/dev/null || echo 0)"
    echo ""
    bold "  端口 80 监听:"
    ss -ltn 2>/dev/null | grep ":80 " | head -3
    echo ""
    bold "  健康检查:"
    curl -sf --max-time 3 http://localhost/healthz && echo " (OK)" || red "  ❌ /healthz 失败"
    echo ""
}

do_logs() {
    journalctl -u nginx -f
}

do_restart() {
    check_root
    systemctl restart nginx
    green "✅ nginx 已重启"
}

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
    install)    do_install "$@" ;;
    config|reload) do_config ;;
    status)     do_status ;;
    logs)       do_logs ;;
    restart)    do_restart ;;
    https)      do_https "$2" ;;
    uninstall)  do_uninstall ;;
    *)          red "用法: $0 {install|config|status|logs|restart|https|uninstall}"; exit 1 ;;
esac
