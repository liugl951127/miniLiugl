#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - 生产外网访问一键部署 (V1.9.1)
#
# 流程:
#   1. 安装 nginx + certbot
#   2. 申请 Let's Encrypt HTTPS 证书
#   3. 部署 nginx 配置 (HTTPS + 反代)
#   4. 验证外网访问
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
# 1. 检查 + 安装 nginx + certbot
# ============================================================
log_info "==== 1. 安装 nginx + certbot ===="

if ! command -v nginx &>/dev/null; then
  if [ -f /etc/debian_version ]; then
    apt-get update -qq
    apt-get install -y nginx certbot python3-certbot-nginx
  elif [ -f /etc/redhat-release ]; then
    yum install -y epel-release
    yum install -y nginx certbot python3-certbot-nginx
  fi
fi
log_ok "nginx $(nginx -v 2>&1 | awk -F'/' '{print $2}')"
log_ok "certbot $(certbot --version 2>&1 | awk '{print $2}')"

# ============================================================
# 2. 申请 Let's Encrypt 证书
# ============================================================
log_info "==== 2. 申请 HTTPS 证书 (Let's Encrypt) ===="

# 临时启动 nginx (如果没启动)
if ! pgrep nginx > /dev/null; then
  nginx
  sleep 2
fi

# 申请证书 (standalone 模式, 自动验证域名)
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
  log_err "证书申请失败, 请检查域名解析 + 80 端口"
  exit 1
fi
log_ok "证书: $CERT_DIR"

# ============================================================
# 3. 部署 nginx 配置
# ============================================================
log_info "==== 3. 部署 nginx 配置 ===="

# 复制配置 + 替换占位符
cp "$PROJECT_ROOT/scripts/nginx-minimax-domain.conf" /etc/nginx/conf.d/minimax.conf
sed -i "s|your-domain.com|$DOMAIN|g" /etc/nginx/conf.d/minimax.conf

# 备份原 default
if [ -f /etc/nginx/sites-enabled/default ]; then
  mv /etc/nginx/sites-enabled/default /etc/nginx/sites-enabled/default.bak
fi

# 测试配置
if ! nginx -t 2>&1; then
  log_err "nginx 配置错误"
  nginx -t
  exit 1
fi
log_ok "nginx 配置 OK"

# 重新加载
systemctl reload nginx
log_ok "nginx 已重载"

# ============================================================
# 4. 自动续期证书 (cron)
# ============================================================
log_info "==== 4. 配置证书自动续期 ===="

cat > /etc/cron.d/certbot-renew <<EOF
# 每天凌晨 3 点检查, 续期后 reload nginx
0 3 * * * root certbot renew --quiet --post-hook "systemctl reload nginx"
EOF

log_ok "证书自动续期 cron 已配置"

# ============================================================
# 5. 防火墙 + 验证
# ============================================================
log_info "==== 5. 验证 ===="

# 开放 80 + 443
if command -v ufw &>/dev/null; then
  ufw allow 80/tcp
  ufw allow 443/tcp
fi
if command -v firewall-cmd &>/dev/null; then
  firewall-cmd --permanent --add-port=80/tcp
  firewall-cmd --permanent --add-port=443/tcp
  firewall-cmd --reload
fi

# 测试 HTTPS
sleep 2
if curl -sk -o /dev/null -w "%{http_code}" "https://$DOMAIN/" | grep -q "200\|301\|302"; then
  log_ok "https://$DOMAIN/ 可访问 ✓"
else
  log_warn "https://$DOMAIN/ 暂时无法访问, 请检查:"
  echo "  1. 域名 A 记录是否指向 $(curl -s ifconfig.me)"
  echo "  2. 后端 gateway:7080 / auth:8081 是否启动"
  echo "  3. nginx 错误日志: tail -f /var/log/nginx/minimax-error.log"
fi

# 测试 HTTP→HTTPS 重定向
echo ""
echo "  HTTP → HTTPS 测试:"
echo "    curl -I http://$DOMAIN/"
echo ""
echo "  HTTPS 健康检查:"
echo "    curl -I https://$DOMAIN/actuator/health"

echo ""
echo "=========================================="
echo "  🎉 外网访问已部署"
echo "=========================================="
echo ""
echo "  访问地址: https://$DOMAIN"
echo "  Nacos:    https://$DOMAIN:8848/nacos (走 gateway)"
echo ""
echo "  证书续期: certbot renew (已配 cron 每天 3 点自动续)"
echo ""
echo "  日志:"
echo "    访问: tail -f /var/log/nginx/minimax-access.log"
echo "    错误: tail -f /var/log/nginx/minimax-error.log"