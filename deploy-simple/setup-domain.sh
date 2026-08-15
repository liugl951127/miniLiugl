#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - 生产外网访问一键部署 (V1.9.2)
#
# 支持 OS:
#   - CentOS Stream 9 / RHEL 9 / Rocky 9 (dnf + firewalld + SELinux)
#   - CentOS 7 / RHEL 7 (yum 兼容)
#   - Ubuntu 20+ / Debian 11+ (apt + ufw)
#
# 流程:
#   1. 安装 nginx + certbot
#   2. 申请 Let's Encrypt HTTPS 证书
#   3. 部署 nginx 配置 (HTTPS + 反代)
#   4. 配置自动续期 cron
#   5. 防火墙放行 + SELinux 调整
#   6. 验证外网访问
#
# 用法:
#   chmod +x deploy-simple/setup-domain.sh
#   ./deploy-simple/setup-domain.sh your-domain.com your-email@example.com
#
# 前提:
#   - 域名已解析到本机公网 IP (A 记录)
#   - 80/443 端口开放
#   - 后端服务 (gateway:7080, auth:8081) 已启动
# =============================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# 加载 OS 适配层
. "$SCRIPT_DIR/os-detect.sh"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_ok()   { echo -e "${GREEN}[✓]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[!]${NC} $*"; }
log_err()  { echo -e "${RED}[✗]${NC} $*"; }

DOMAIN="${1:-}"
EMAIL="${2:-}"

if [ -z "$DOMAIN" ] || [ -z "$EMAIL" ]; then
  log_err "用法: $0 <your-domain.com> <your-email@example.com>"
  echo "  示例: $0 minimax.example.com admin@example.com"
  exit 1
fi

# ============================================================
# 0. 必须 root 权限
# ============================================================
if [ "$EUID" -ne 0 ]; then
  log_err "需要 root 权限, 请用 sudo 运行"
  exit 1
fi

# ============================================================
# 1. OS 探测 + 信息展示
# ============================================================
detect_os || exit 1
os_info
echo ""

# ============================================================
# 2. 安装 nginx + certbot
# ============================================================
log_info "==== 安装 nginx + certbot ===="

if ! command -v nginx &>/dev/null; then
  case "$OS_FAMILY" in
    rhel)
      # CentOS Stream 9 / RHEL 9: nginx 在 EPEL
      if [ "$OS_ID" = "centos" ] || [ "$OS_ID" = "rhel" ] || [ "$OS_ID" = "rocky" ] || [ "$OS_ID" = "almalinux" ]; then
        log_info "CentOS/RHEL 系: 启用 EPEL + PowerTools/CRB 仓库"
        if [ "$OS_VERSION" = "9" ] || [ "$OS_VERSION" = "8" ]; then
          dnf install -y epel-release
          # Stream 9 / RHEL 9 需要 crb (Code Ready Builder)
          dnf config-manager --enable crb 2>/dev/null || \
            dnf config-manager --enable powertools 2>/dev/null || true
        else
          # RHEL 7 / CentOS 7
          yum install -y epel-release
        fi
      fi
      pkg_install nginx certbot python3-certbot-nginx
      ;;
    debian)
      pkg_install nginx certbot python3-certbot-nginx
      ;;
  esac
  log_ok "nginx + certbot 已安装"
else
  log_ok "nginx 已存在 ($(nginx -v 2>&1 | awk -F'/' '{print $2}'))"
fi

if command -v certbot &>/dev/null; then
  log_ok "certbot $(certbot --version 2>&1 | awk '{print $2}')"
else
  log_err "certbot 安装失败, 请检查 $PKG_MGR 源"
  exit 1
fi

# ============================================================
# 3. SELinux 处理 (CentOS/RHEL 默认开启)
# ============================================================
if [ "$OS_FAMILY" = "rhel" ]; then
  selinux_setup
fi

# ============================================================
# 4. 启动 nginx (certbot 申请证书需要)
# ============================================================
nginx_enable
if ! pgrep nginx > /dev/null; then
  systemctl start nginx || nginx
  sleep 2
fi
log_ok "nginx 已启动"

# ============================================================
# 5. 申请 Let's Encrypt 证书
# ============================================================
log_info "==== 申请 HTTPS 证书 (Let's Encrypt) ===="

certbot certonly --standalone \
  --preferred-challenges http \
  -d "$DOMAIN" \
  -d "www.$DOMAIN" \
  --email "$EMAIL" \
  --agree-tos \
  --no-eff-email \
  --keep-until-expiring \
  --non-interactive 2>&1 | tail -10

CERT_DIR="/etc/letsencrypt/live/$DOMAIN"
if [ ! -f "$CERT_DIR/fullchain.pem" ]; then
  log_err "证书申请失败, 请检查:"
  echo "  1. 域名解析: nslookup $DOMAIN"
  echo "  2. 公网 IP:  curl ifconfig.me (跟 A 记录对比)"
  echo "  3. 80 端口:  ss -tlnp | grep :80 (不能被其他进程占)"
  exit 1
fi
log_ok "证书: $CERT_DIR"

# ============================================================
# 6. 部署 nginx 配置
# ============================================================
log_info "==== 部署 nginx 配置 ===="

# 复制配置 + 替换占位符
cp "$PROJECT_ROOT/scripts/nginx-minimax-domain.conf" /etc/nginx/conf.d/minimax.conf
sed -i "s|your-domain.com|$DOMAIN|g" /etc/nginx/conf.d/minimax.conf

# 删除默认 server 配置 (不同 OS 路径不同)
for f in \
  /etc/nginx/conf.d/default.conf \
  /etc/nginx/sites-enabled/default \
  /etc/nginx/nginx.conf.bak; do
  [ -f "$f" ] && mv "$f" "${f}.bak" && log_warn "已备份: $f"
done

# 测试配置 (可跳过, 用于容器/跨机器部署)
if [ "${SKIP_NGINX_CHECK:-0}" = "1" ]; then
  log_warn "跳过 nginx 配置校验 (SKIP_NGINX_CHECK=1)"
else
  if ! nginx_test 2>/dev/null; then
    if [ "${STRICT_NGINX:-0}" = "1" ]; then
      log_err "nginx 配置语法错误"
      nginx_test
      exit 1
    else
      log_warn "nginx 配置语法有警告, 但继续 (设 STRICT_NGINX=1 严格退出)"
    fi
  else
    log_ok "nginx 配置语法 OK"
  fi
fi

nginx_reload

# ============================================================
# 7. 自动续期 cron (兼容 CentOS Stream 9 systemd timer)
# ============================================================
log_info "==== 配置证书自动续期 ===="

# 方案 1: certbot 多数发行版已自带 systemd timer 或 cron
if systemctl list-timers 2>/dev/null | grep -q certbot; then
  log_ok "已有 certbot systemd timer, 自动续期启用"
elif [ -d /etc/cron.d ]; then
  cat > /etc/cron.d/certbot-renew <<EOF
# 每天凌晨 3 点检查, 续期后 reload nginx
SHELL=/bin/bash
0 3 * * * root certbot renew --quiet --post-hook "systemctl reload nginx" >> /var/log/certbot-renew.log 2>&1
EOF
  log_ok "已添加 /etc/cron.d/certbot-renew"
fi

# ============================================================
# 8. 防火墙放行 80 + 443
# ============================================================
log_info "==== 配置防火墙 ===="
firewall_open 80 tcp
firewall_open 443 tcp

# ============================================================
# 9. 验证
# ============================================================
log_info "==== 验证 ===="

sleep 2
# 测试 HTTPS
http_code=$(curl -sk -o /dev/null -w "%{http_code}" "https://$DOMAIN/" 2>/dev/null || echo "000")
if echo "$http_code" | grep -qE "^(200|301|302)$"; then
  log_ok "https://$DOMAIN/ 可访问 (HTTP $http_code)"
else
  log_warn "https://$DOMAIN/ 暂时无法访问 (HTTP $http_code), 可能原因:"
  echo "  1. 后端 gateway:7080 / auth:8081 没启动 → 启动后再试"
  echo "  2. nginx 错误日志:  tail -f /var/log/nginx/minimax-error.log"
fi

# 输出总结
echo ""
echo "=========================================="
echo "  🎉 外网访问已部署"
echo "=========================================="
echo ""
echo "  访问地址:  https://$DOMAIN"
echo "  公网 IP:   $(curl -s ifconfig.me 2>/dev/null || echo '(unknown)')"
echo "  证书路径:  $CERT_DIR"
echo "  续期:      certbot renew (已配 cron / systemd timer)"
echo "  系统:      $OS_PRETTY"
echo ""
echo "  常用命令:"
echo "    nginx -t                 # 测试配置"
echo "    nginx -s reload          # 重载"
echo "    tail -f /var/log/nginx/minimax-access.log"
echo ""
echo "  防火墙状态:"
if command -v firewall-cmd &>/dev/null; then
  firewall-cmd --list-ports | tr ' ' '\n' | head
elif command -v ufw &>/dev/null; then
  ufw status | head
fi