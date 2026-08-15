#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - 公网域名 + HTTPS + 完整链路一键部署 (V1.9.5)
#
# 完整链路:
#   [用户 https://your-domain.com]
#     ↓ DNS A 记录
#   [宿主机公网 IP :443]
#     ↓ certbot + nginx (宿主机)
#     ↓ /etc/nginx/conf.d/minimax.conf
#     ↓ location / proxy_pass http://127.0.0.1:80
#   [宿主机 :80 (docker nginx 容器 minimax-nginx)]
#     ↓ /etc/nginx/conf.d/minimax.conf
#     ↓ /api/** → http://127.0.0.1:7080
#   [宿主机 :7080 (docker gateway 容器)]
#     ↓ lb://minimax-* lb://minimax-*-service
#   [15 个微服务 docker 容器]
#     ↓
#   [mysql/redis/nacos/otel docker 容器]
#
# 使用:
#   sudo ./deploy-simple/setup-public-domain.sh your-domain.com admin@your-domain.com
#
# 前提:
#   1. 域名已解析到本机公网 IP (A 记录)
#   2. docker compose 已 up (gateway:7080 / auth:8081 在跑)
#   3. nginx 容器已启动 (否则先跑 fix-port-80.sh)
#
# 自动做的事:
#   1. 检测公网 IP 并验证 DNS
#   2. 修 80 端口冲突 (宿主机 nginx vs docker nginx)
#   3. 申请 Let's Encrypt 证书
#   4. 配置宿主机 nginx (HTTPS 反代 → docker nginx)
#   5. 配置 docker nginx (反代 → gateway)
#   6. 防火墙放行 80 + 443
#   7. 自动续期 cron
#   8. 完整链路验证
# =============================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

. "$SCRIPT_DIR/os-detect.sh"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_ok()   { echo -e "${GREEN}[✓]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[!]${NC} $*"; }
log_err()  { echo -e "${RED}[✗]${NC} $*"; }

DOMAIN="${1:-}"
EMAIL="${2:-}"

# ============================================================
# 0. 参数校验
# ============================================================
if [ "$EUID" -ne 0 ]; then
  log_err "请用 sudo 跑: sudo $0 your-domain.com admin@your-domain.com"
  exit 1
fi

if [ -z "$DOMAIN" ] || [ -z "$EMAIL" ]; then
  log_err "用法: $0 <your-domain.com> <your-email@example.com>"
  echo ""
  echo "示例:"
  echo "  sudo $0 minimax.example.com admin@example.com"
  echo ""
  echo "前提:"
  echo "  1. 域名已解析到本机公网 IP (A 记录)"
  echo "  2. docker compose 已起来"
  exit 1
fi

log_info "=========================================="
log_info " MiniMax Platform - 公网域名 + HTTPS 一键部署"
log_info "=========================================="
log_info "域名:  $DOMAIN"
log_info "邮箱:  $EMAIL"
log_info "系统:  $OS_PRETTY"
log_info "项目:  $PROJECT_ROOT"
echo ""

# ============================================================
# 1. 检测公网 IP + DNS 解析
# ============================================================
log_info "==== 1. 检测公网 IP + DNS ===="

PUBLIC_IP=$(curl -s --max-time 5 ifconfig.me 2>/dev/null || curl -s --max-time 5 ipinfo.io/ip 2>/dev/null || echo "")
if [ -z "$PUBLIC_IP" ]; then
  log_err "无法获取本机公网 IP"
  exit 1
fi
log_ok "本机公网 IP: $PUBLIC_IP"

# DNS 解析
RESOLVED_IP=$(dig +short "$DOMAIN" 2>/dev/null | head -1 || nslookup "$DOMAIN" 2>/dev/null | grep -oP 'Address: \K.*' | head -1 || echo "")
if [ -z "$RESOLVED_IP" ]; then
  log_err "域名解析失败: $DOMAIN"
  echo "  请到域名服务商添加 A 记录:"
  echo "    $DOMAIN    A    $PUBLIC_IP"
  echo "    www.$DOMAIN A    $PUBLIC_IP"
  exit 1
fi
log_ok "域名 $DOMAIN 解析到: $RESOLVED_IP"

if [ "$RESOLVED_IP" != "$PUBLIC_IP" ]; then
  log_warn "域名解析 IP ($RESOLVED_IP) 跟本机公网 IP ($PUBLIC_IP) 不一致"
  log_warn "Let's Encrypt 申请可能失败, 继续..."
fi

# ============================================================
# 2. 检测 docker / 启动后端
# ============================================================
log_info "==== 2. 检测 docker / 后端 ===="

if ! command -v docker &>/dev/null; then
  log_err "docker 未安装"
  exit 1
fi
log_ok "docker: $(docker --version)"

cd "$PROJECT_ROOT"
if [ ! -f docker-compose.yml ]; then
  log_err "找不到 docker-compose.yml"
  exit 1
fi

# 检查后端容器是否在跑
RUNNING=$(docker compose ps --services --filter "status=running" 2>/dev/null | wc -l)
if [ "$RUNNING" -lt 5 ]; then
  log_warn "只有 $RUNNING 个容器在跑, 建议先拉起完整 stack"
  log_info "自动 docker compose up -d ..."
  docker compose up -d
  sleep 10
fi

# 检查 gateway 端口
if ! ss -tln 2>/dev/null | grep -q ":7080 "; then
  log_warn "宿主机 7080 (gateway) 未监听, 尝试拉起 gateway..."
  docker compose up -d gateway
  sleep 10
fi

if ss -tln 2>/dev/null | grep -q ":7080 "; then
  log_ok "gateway:7080 在跑"
else
  log_err "gateway:7080 未启动, 无法继续"
  echo "  排查: docker compose logs gateway"
  exit 1
fi

# ============================================================
# 3. 修 80 端口冲突 (docker nginx vs 宿主机 nginx)
# ============================================================
log_info "==== 3. 修 80 端口冲突 ===="

bash "$SCRIPT_DIR/fix-port-80.sh" 2>&1 | tail -20 || true
echo ""

# 确认 docker nginx 在跑
if ! docker ps --filter "name=minimax-nginx" --format "{{.Status}}" | grep -q "Up"; then
  log_err "minimax-nginx 容器还是没起, 请排查:"
  echo "  docker logs minimax-nginx --tail 30"
  exit 1
fi
log_ok "docker nginx (minimax-nginx) 在跑"

# ============================================================
# 4. 安装宿主机 nginx + certbot (HTTPS 终结点)
# ============================================================
log_info "==== 4. 安装宿主机 nginx + certbot ===="

if ! command -v nginx &>/dev/null; then
  case "$OS_FAMILY" in
    rhel)
      if [ "$OS_VERSION" = "9" ] || [ "$OS_VERSION" = "8" ]; then
        dnf install -y epel-release
        dnf config-manager --enable crb 2>/dev/null || \
          dnf config-manager --enable powertools 2>/dev/null || true
      else
        yum install -y epel-release
      fi
      pkg_install nginx certbot python3-certbot-nginx
      ;;
    debian)
      pkg_install nginx certbot python3-certbot-nginx
      ;;
  esac
fi

if ! command -v certbot &>/dev/null; then
  log_err "certbot 安装失败"
  exit 1
fi
log_ok "nginx + certbot 就绪"

# SELinux 处理
if [ "$OS_FAMILY" = "rhel" ]; then
  selinux_setup || true
fi

# ============================================================
# 5. 申请 Let's Encrypt 证书
# ============================================================
log_info "==== 5. 申请 HTTPS 证书 ===="

# 先临时停 docker nginx 让出 80 (certbot standalone 需要独占 80)
log_info "临时停 docker nginx 让 80 给 certbot"
docker compose stop nginx || true
sleep 2

certbot certonly --standalone \
  --preferred-challenges http \
  -d "$DOMAIN" \
  -d "www.$DOMAIN" \
  --email "$EMAIL" \
  --agree-tos \
  --no-eff-email \
  --keep-until-expiring \
  --non-interactive 2>&1 | tail -8

CERT_DIR="/etc/letsencrypt/live/$DOMAIN"
if [ ! -f "$CERT_DIR/fullchain.pem" ]; then
  log_err "证书申请失败"
  echo "  排查:"
  echo "  1. 域名解析: dig +short $DOMAIN"
  echo "  2. 公网 IP:  curl ifconfig.me"
  echo "  3. 80 端口:  ss -tlnp | grep :80 (不能被占)"
  # 拉回 docker nginx
  cd "$PROJECT_ROOT" && docker compose start nginx
  exit 1
fi
log_ok "证书: $CERT_DIR"

# 拉回 docker nginx
cd "$PROJECT_ROOT" && docker compose start nginx
sleep 3

# ============================================================
# 6. 部署宿主机 nginx (HTTPS 终结点 → 反代到 docker nginx 80)
# ============================================================
log_info "==== 6. 配置宿主机 nginx (HTTPS 反代) ===="

# 生成宿主机 nginx 配置 (HTTPS 终结点, 反代到 localhost:80)
cat > /etc/nginx/conf.d/minimax-public.conf <<NGINX_CONF
# =============================================================
# MiniMax 公网 HTTPS 终结点 (V1.9.5)
# 由 setup-public-domain.sh 自动生成
# 链路: 用户 :443 → 本机 :80 (docker nginx) → gateway:7080
# =============================================================

# HTTP :80 - 自动跳 HTTPS
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name $DOMAIN www.$DOMAIN;

    # Let's Encrypt 验证用
    location /.well-known/acme-challenge/ {
        root /var/www/html;
    }

    # 其他 HTTP 跳 HTTPS
    location / {
        return 301 https://\$host\$request_uri;
    }
}

# HTTPS :443 - 反代到 docker nginx (localhost:80)
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name $DOMAIN www.$DOMAIN;

    # SSL 证书
    ssl_certificate     $CERT_DIR/fullchain.pem;
    ssl_certificate_key $CERT_DIR/privkey.pem;
    ssl_trusted_certificate $CERT_DIR/chain.pem;

    # SSL 配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;
    ssl_session_tickets off;

    # 安全头
    add_header Strict-Transport-Security "max-age=31536000" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # 反代到 docker nginx (宿主机 localhost:80)
    # docker nginx minimax-nginx 反代到 gateway:7080
    location / {
        proxy_pass http://127.0.0.1:80;
        proxy_http_version 1.1;
        proxy_set_header Host              \$host;
        proxy_set_header X-Real-IP         \$remote_addr;
        proxy_set_header X-Forwarded-For   \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Forwarded-Host  \$host;
        proxy_set_header X-Forwarded-Port  \$server_port;

        # WebSocket
        proxy_set_header Upgrade           \$http_upgrade;
        proxy_set_header Connection        "upgrade";

        # 超时
        proxy_connect_timeout 60s;
        proxy_send_timeout    60s;
        proxy_read_timeout    60s;

        # 缓冲
        proxy_buffering off;
        proxy_request_buffering off;
    }
}
NGINX_CONF

log_ok "宿主机 nginx 配置: /etc/nginx/conf.d/minimax-public.conf"

# 备份默认配置
for f in /etc/nginx/conf.d/default.conf /etc/nginx/sites-enabled/default; do
  [ -f "$f" ] && mv "$f" "${f}.bak-$(date +%s)" && log_warn "已备份: $f"
done

# 测试配置
if ! nginx -t 2>&1 | tail -5; then
  log_err "宿主机 nginx 配置错误"
  exit 1
fi
log_ok "宿主机 nginx 配置语法 OK"

# 启动宿主机 nginx
systemctl enable nginx 2>/dev/null || true
systemctl restart nginx
sleep 2
log_ok "宿主机 nginx 已启动"

# ============================================================
# 7. docker nginx 也要配通链路
# ============================================================
log_info "==== 7. 确认 docker nginx 配置 ===="

# 确保 docker nginx 配置文件是最新版 (V1.9.5: 反代 127.0.0.1)
NGINX_FILE="$PROJECT_ROOT/scripts/nginx-minimax-80.conf"
if [ -f "$NGINX_FILE" ]; then
  # 强制使用 127.0.0.1 (不依赖容器名 DNS)
  if grep -q "proxy_pass http://gateway:" "$NGINX_FILE"; then
    log_warn "检测到 docker nginx 配置用容器名, 自动改为 127.0.0.1"
    sed -i 's|proxy_pass http://gateway:|proxy_pass http://127.0.0.1:|g' "$NGINX_FILE"
    sed -i 's|proxy_pass http://auth:|proxy_pass http://127.0.0.1:|g' "$NGINX_FILE"
  fi
  log_ok "docker nginx 配置: $NGINX_FILE"
  cd "$PROJECT_ROOT" && docker compose restart nginx
  sleep 3
fi

# ============================================================
# 8. 防火墙
# ============================================================
log_info "==== 8. 防火墙放行 80 + 443 ===="
firewall_open 80 tcp
firewall_open 443 tcp
log_ok "防火墙已配置"

# ============================================================
# 9. 自动续期 cron
# ============================================================
log_info "==== 9. 配置证书自动续期 ===="

if systemctl list-timers 2>/dev/null | grep -q certbot; then
  log_ok "已有 certbot systemd timer"
elif [ -d /etc/cron.d ]; then
  cat > /etc/cron.d/certbot-renew <<EOF
# 每天 3 点检查证书, 续期后 reload 宿主机 nginx + 重启 docker nginx
SHELL=/bin/bash
0 3 * * * root certbot renew --quiet --post-hook "systemctl reload nginx && cd $PROJECT_ROOT && docker compose restart nginx" >> /var/log/certbot-renew.log 2>&1
EOF
  log_ok "已添加 /etc/cron.d/certbot-renew"
fi

# ============================================================
# 10. 完整链路验证
# ============================================================
log_info "==== 10. 完整链路验证 ===="
echo ""

# 链路 1: 宿主机 nginx :80 (docker)
log_info "链路 1/4: docker nginx (宿主机 :80)"
sleep 3
HTTP_80=$(curl -s -o /dev/null -w "%{http_code}" -m 5 "http://localhost/" 2>/dev/null || echo "000")
if echo "$HTTP_80" | grep -qE "^(200|301|302)$"; then
  log_ok "  → HTTP $HTTP_80"
else
  log_warn "  → HTTP $HTTP_80 (docker nginx 可能还没好)"
fi

# 链路 2: gateway
log_info "链路 2/4: gateway (宿主机 :7080)"
HTTP_7080=$(curl -s -o /dev/null -w "%{http_code}" -m 5 "http://localhost:7080/actuator/health" 2>/dev/null || echo "000")
if echo "$HTTP_7080" | grep -qE "^(200|301|302)$"; then
  log_ok "  → HTTP $HTTP_7080"
else
  log_warn "  → HTTP $HTTP_7080"
fi

# 链路 3: 宿主机 nginx :443 (HTTPS)
log_info "链路 3/4: 宿主机 nginx HTTPS (:443)"
sleep 2
HTTP_443=$(curl -s -o /dev/null -w "%{http_code}" -m 8 "https://localhost/" -k 2>/dev/null || echo "000")
if echo "$HTTP_443" | grep -qE "^(200|301|302)$"; then
  log_ok "  → HTTP $HTTP_443"
else
  log_warn "  → HTTP $HTTP_443 (证书/配置可能未生效)"
fi

# 链路 4: 公网域名 (https://$DOMAIN)
log_info "链路 4/4: 公网域名 https://$DOMAIN"
sleep 2
HTTP_DOMAIN=$(curl -s -o /dev/null -w "%{http_code}" -m 15 "https://$DOMAIN/" 2>/dev/null || echo "000")
if echo "$HTTP_DOMAIN" | grep -qE "^(200|301|302)$"; then
  log_ok "  → HTTP $HTTP_DOMAIN 🎉"
else
  log_warn "  → HTTP $HTTP_DOMAIN"
  echo "  可能原因:"
  echo "  1. DNS 解析刚改, 全球生效要 5-30 分钟"
  echo "  2. 云服务商安全组没放行 80/443"
  echo "  3. firewall-cmd --list-all 检查本机防火墙"
fi

# ============================================================
# 11. 登录 API 测试
# ============================================================
log_info "==== 11. 登录 API 测试 ===="
echo ""

LOGIN_RESP=$(curl -s -m 10 -X POST "https://$DOMAIN/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"adminLiugl","password":"Liugl@2026"}' 2>&1)

if echo "$LOGIN_RESP" | grep -q '"code":0'; then
  log_ok "登录 API 成功 🎉"
  echo "  响应: $(echo "$LOGIN_RESP" | head -c 200)..."
elif echo "$LOGIN_RESP" | grep -q "accessToken"; then
  log_ok "登录 API 成功 (返回 token)"
  echo "  响应: $(echo "$LOGIN_RESP" | head -c 200)..."
else
  log_warn "登录 API 失败:"
  echo "  响应: $(echo "$LOGIN_RESP" | head -c 300)"
  echo "  排查: docker logs minimax-auth --tail 30"
fi

# ============================================================
# 输出最终总结
# ============================================================
echo ""
echo "=========================================="
echo "  🎉 完整链路部署完成"
echo "=========================================="
echo ""
echo "  🌐 公网访问:  https://$DOMAIN"
echo "  🏠 内网 IP:   $PUBLIC_IP"
echo "  📜 证书:      $CERT_DIR"
echo "  🔧 系统:      $OS_PRETTY"
echo ""
echo "  链路图:"
echo "    [用户 https://$DOMAIN]"
echo "         ↓"
echo "    [宿主机 nginx :443 (HTTPS 终结点)]"
echo "         ↓ proxy_pass http://127.0.0.1:80"
echo "    [宿主机 docker nginx :80 (minimax-nginx)]"
echo "         ↓ proxy_pass http://127.0.0.1:7080"
echo "    [宿主机 docker gateway :7080 (minimax-gateway)]"
echo "         ↓ lb://minimax-*"
echo "    [15 个微服务容器 (docker network)]"
echo ""
echo "  登录账号:"
echo "    超级管理员: adminLiugl / Liugl@2026"
echo "    普通管理员: admin / admin@123"
echo ""
echo "  常用命令:"
echo "    systemctl status nginx         # 宿主机 nginx"
echo "    docker compose ps nginx        # docker nginx"
echo "    docker compose logs gateway    # gateway 日志"
echo "    certbot certificates           # 证书状态"
echo "    nginx -t && nginx -s reload    # 重载宿主机 nginx"
echo "    tail -f /var/log/nginx/minimax-access.log"
echo ""
echo "  链路验证脚本:"
echo "    $SCRIPT_DIR/verify-public-domain.sh $DOMAIN"
echo ""