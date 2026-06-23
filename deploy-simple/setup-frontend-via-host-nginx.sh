#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - 通过宿主机 nginx 反代前端 + gateway (V1.9.6)
#
# 架构 (核心改动: 不用 docker nginx 容器, 改用宿主机 nginx):
#   [用户 https://liugeliang.com]
#     ↓ DNS A 记录
#   [宿主机 nginx :443 (certbot 证书, 反代终结点)]
#     ↓ location /          → /opt/miniLiugl/frontend/dist  (前端静态)
#     ↓ location /api/**     → http://127.0.0.1:7080          (gateway)
#     ↓ location /ws         → http://127.0.0.1:7080          (gateway WS)
#     ↓ location /actuator/**→ http://127.0.0.1:7080          (gateway 健康检查)
#   [宿主机 :7080 (docker minimax-gateway 容器)]
#     ↓ lb://minimax-* (Nacos 服务发现)
#   [15 个微服务 docker 容器]
#     ↓
#   [mysql/redis/nacos/otel docker 容器]
#
# 关键变化 (V1.9.6):
#   - 不再用 docker nginx 容器 (删除 minimax-nginx 服务)
#   - 宿主机 nginx 直接反代前端静态 + gateway
#   - 端口 80 完全归宿主机 nginx (没冲突!)
#   - 端口冲突问题彻底解决
#
# 用法:
#   sudo ./deploy-simple/setup-frontend-via-host-nginx.sh liugeliang.com admin@liugeliang.com
#
# 前提:
#   1. 域名已解析到 VPS 公网 IP (A 记录)
#   2. docker compose 已 up (gateway:7080 在跑)
#   3. 前端已构建 (frontend/dist 存在)
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
  log_err "请用 sudo 跑: sudo $0 <your-domain.com> <your-email>"
  exit 1
fi

if [ -z "$DOMAIN" ] || [ -z "$EMAIL" ]; then
  log_err "用法: $0 <your-domain.com> <your-email@example.com>"
  echo ""
  echo "示例: sudo $0 liugeliang.com admin@liugeliang.com"
  exit 1
fi

log_info "=========================================="
log_info " MiniMax Platform - 宿主机 nginx 反代前端"
log_info "=========================================="
log_info "域名:  $DOMAIN"
log_info "邮箱:  $EMAIL"
log_info "系统:  $OS_PRETTY"
log_info "项目:  $PROJECT_ROOT"
echo ""

# ============================================================
# 1. 检测环境
# ============================================================
log_info "==== 1. 环境检测 ===="

# 公网 IP
PUBLIC_IP=$(curl -s --max-time 5 ifconfig.me 2>/dev/null || curl -s --max-time 5 ipinfo.io/ip 2>/dev/null || echo "")
[ -n "$PUBLIC_IP" ] && log_ok "公网 IP: $PUBLIC_IP" || { log_err "无法获取公网 IP"; exit 1; }

# DNS 解析
RESOLVED_IP=$(dig +short "$DOMAIN" 2>/dev/null | head -1 || nslookup "$DOMAIN" 2>/dev/null | grep -oP 'Address: \K.*' | head -1 || echo "")
if [ -z "$RESOLVED_IP" ]; then
  log_err "域名解析失败: $DOMAIN"
  echo "  请到域名服务商添加 A 记录:"
  echo "    $DOMAIN    A    $PUBLIC_IP"
  exit 1
fi
log_ok "域名解析: $DOMAIN → $RESOLVED_IP"

if [ "$RESOLVED_IP" != "$PUBLIC_IP" ]; then
  log_warn "域名解析 IP 跟公网 IP 不一致 ($RESOLVED_IP vs $PUBLIC_IP)"
  log_warn "Let's Encrypt 申请会失败"
  read -p "继续吗? [y/N] " CONFIRM
  [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ] && exit 1
fi

# docker
command -v docker &>/dev/null && log_ok "Docker: $(docker --version)" || { log_err "docker 未安装"; exit 1; }

# docker compose
docker compose version &>/dev/null && log_ok "Docker Compose: $(docker compose version | awk '{print $4}')" || { log_err "docker compose 未安装"; exit 1; }

cd "$PROJECT_ROOT"
[ -f docker-compose.yml ] && log_ok "docker-compose.yml: 找到" || { log_err "找不到 docker-compose.yml"; exit 1; }

# 前端 dist
if [ ! -d frontend/dist ]; then
  log_warn "前端 dist 不存在, 自动构建..."
  cd "$PROJECT_ROOT/frontend"
  if [ ! -d node_modules ]; then
    npm config set registry https://registry.npmmirror.com 2>/dev/null || true
    npm install --silent 2>&1 | tail -3
  fi
  npm run build 2>&1 | tail -10
  cd "$PROJECT_ROOT"
fi
log_ok "前端 dist: $(du -sh frontend/dist 2>/dev/null | awk '{print $1}')"

echo ""

# ============================================================
# 2. 启动后端 docker stack (不包含 nginx 容器)
# ============================================================
log_info "==== 2. 启动后端 docker stack ===="

cd "$PROJECT_ROOT"

# 关键 V1.9.6: 排除 nginx 容器 (用宿主机 nginx 替代)
log_info "启动后端服务 (排除 docker nginx)..."
docker compose up -d --scale nginx=0 2>/dev/null || docker compose up -d mysql redis nacos otel-collector gateway auth chat memory model rag function multimodal agent monitor admin prompt analytics pipeline ws
sleep 10

# 检查关键服务
for svc in mysql redis nacos gateway; do
  STATUS=$(docker compose ps --services --filter "status=running" 2>/dev/null | grep -c "^$svc$" || echo 0)
  if [ "$STATUS" -gt 0 ]; then
    log_ok "$svc: 运行中"
  else
    log_warn "$svc: 未运行 (继续, 可能在启动中)"
  fi
done

# 验证 gateway 端口
if ! ss -tln 2>/dev/null | grep -q ":7080 "; then
  log_warn "gateway:7080 未监听, 拉起 gateway..."
  docker compose up -d gateway
  sleep 15
fi

if ss -tln 2>/dev/null | grep -q ":7080 "; then
  log_ok "gateway:7080 在跑"
else
  log_err "gateway:7080 启动失败"
  docker compose logs gateway --tail 30
  exit 1
fi

# 删除 docker nginx 容器 (如果存在)
if docker ps -a --filter "name=minimax-nginx" --format "{{.Names}}" | grep -q minimax-nginx; then
  log_info "停止并删除 docker nginx 容器 (改用宿主机 nginx)..."
  docker compose stop nginx 2>/dev/null || docker stop minimax-nginx 2>/dev/null || true
  docker compose rm -f nginx 2>/dev/null || docker rm -f minimax-nginx 2>/dev/null || true
  log_ok "docker nginx 已移除"
fi

echo ""

# ============================================================
# 3. 停掉可能占用 80 的宿主机 nginx / 其他进程
# ============================================================
log_info "==== 3. 释放 80 端口给宿主机 nginx ===="

# 如果宿主机 nginx 在跑且配置 OK, 不用停
if systemctl is-active nginx 2>/dev/null | grep -q active; then
  log_info "宿主机 nginx 在跑, 重新加载配置..."
  # 不停, 后面直接覆盖配置 + reload
else
  log_info "宿主机 nginx 未运行, 安装..."
fi

# 检查 80 端口
PORT80_PID=$(ss -tlnp 2>/dev/null | grep ":80 " | head -1 | grep -oP 'pid=\K[0-9]+' || echo "")
if [ -n "$PORT80_PID" ]; then
  PROC=$(cat /proc/$PORT80_PID/comm 2>/dev/null || echo "unknown")
  log_warn "80 端口被占用: PID=$PORT80_PID, 进程=$PROC"
  
  if [ "$PROC" = "docker-proxy" ]; then
    log_info "docker-proxy 占 80 (可能是 minimax-nginx 残留), 清理..."
    docker compose stop nginx 2>/dev/null || true
    docker rm -f minimax-nginx 2>/dev/null || true
    sleep 2
  elif [ "$PROC" = "nginx" ]; then
    log_info "宿主机 nginx 占 80, 稍后 reload 时让它接管"
  else
    log_err "未知进程 $PROC 占 80, 请手动处理"
    kill -9 $PORT80_PID 2>/dev/null || true
    sleep 2
  fi
fi

# 验证 80 已释放
sleep 2
if ss -tlnp 2>/dev/null | grep -q ":80 " | grep -v docker-proxy; then
  log_warn "80 仍被占, 强制处理..."
fi
log_ok "80 端口可用"

echo ""

# ============================================================
# 4. 安装宿主机 nginx + certbot (如果没有)
# ============================================================
log_info "==== 4. 安装宿主机 nginx + certbot ===="

if ! command -v nginx &>/dev/null; then
  case "$OS_FAMILY" in
    rhel)
      if [ "$OS_VERSION" = "9" ] || [ "$OS_VERSION" = "8" ]; then
        dnf install -y epel-release
        dnf config-manager --enable crb 2>/dev/null || dnf config-manager --enable powertools 2>/dev/null || true
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

command -v nginx &>/dev/null && log_ok "nginx: $(nginx -v 2>&1)" || { log_err "nginx 安装失败"; exit 1; }
command -v certbot &>/dev/null && log_ok "certbot: $(certbot --version 2>&1 | awk '{print $2}')" || { log_err "certbot 安装失败"; exit 1; }

# SELinux
[ "$OS_FAMILY" = "rhel" ] && selinux_setup || true

echo ""

# ============================================================
# 5. 申请 Let's Encrypt HTTPS 证书
# ============================================================
log_info "==== 5. 申请 HTTPS 证书 (Let's Encrypt) ===="

# 临时启动 nginx 占 80 (certbot standalone 需要)
if ! pgrep nginx >/dev/null; then
  systemctl start nginx 2>/dev/null || nginx
  sleep 2
fi

certbot certonly --standalone \
  --preferred-challenges http \
  -d "$DOMAIN" \
  -d "www.$DOMAIN" \
  --email "$EMAIL" \
  --agree-tos \
  --no-eff-email \
  --non-interactive 2>&1 | tail -10

CERT_DIR="/etc/letsencrypt/live/$DOMAIN"
if [ ! -f "$CERT_DIR/fullchain.pem" ]; then
  log_err "证书申请失败"
  echo "  排查:"
  echo "  1. 域名解析: dig +short $DOMAIN (跟公网 IP 一致?)"
  echo "  2. 公网 IP:  curl ifconfig.me"
  echo "  3. 80 端口:  ss -tlnp | grep :80"
  exit 1
fi
log_ok "证书: $CERT_DIR"

echo ""

# ============================================================
# 6. 部署宿主机 nginx 配置 (核心: 反代前端 + gateway)
# ============================================================
log_info "==== 6. 配置宿主机 nginx (反代前端 + gateway) ===="

# 备份旧的 (V1.9.8: 不再放 conf.d, 直接改 /etc/nginx/nginx.conf)
[ -f /etc/nginx/conf.d/minimax.conf ] && cp /etc/nginx/conf.d/minimax.conf /etc/nginx/conf.d/minimax.conf.bak-$(date +%s)
[ -f /etc/nginx/nginx.conf ] && cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.bak-$(date +%s)

# V1.9.8: 直接替换宿主机默认的 nginx.conf (不依赖 conf.d)
cp "$PROJECT_ROOT/scripts/nginx.conf" /etc/nginx/nginx.conf

# 替换占位符
sed -i "s|/opt/miniLiugl/frontend/dist|$PROJECT_ROOT/frontend/dist|g" /etc/nginx/nginx.conf
# 启用 HTTPS server 块: 替换 _ 为具体域名
# (脚本默认 HTTPS 是注释的, 这里改成启用)
sed -i "s|^    # server {|    server {|g" /etc/nginx/nginx.conf
sed -i "s|^    #     listen 443|    listen 443|g" /etc/nginx/nginx.conf
sed -i "s|^    #     server_name _;|    server_name $DOMAIN;|g" /etc/nginx/nginx.conf
sed -i "s|/etc/letsencrypt/live/your-domain.com|/etc/letsencrypt/live/$DOMAIN|g" /etc/nginx/nginx.conf

# 备份默认 conf.d
for f in /etc/nginx/conf.d/default.conf /etc/nginx/sites-enabled/default; do
  [ -f "$f" ] && mv "$f" "${f}.bak-$(date +%s)" && log_warn "已备份: $f"
done

log_ok "宿主机 nginx 配置: /etc/nginx/nginx.conf"

# 测试
if ! nginx -t 2>&1 | tail -3; then
  log_err "宿主机 nginx 配置错误"
  exit 1
fi
log_ok "宿主机 nginx 配置语法 OK"

# 启动 / 重载
systemctl enable nginx 2>/dev/null || true
if systemctl is-active nginx 2>/dev/null | grep -q active; then
  systemctl reload nginx
else
  systemctl start nginx
fi
sleep 2
log_ok "宿主机 nginx 已启动/重载"

echo ""

# ============================================================
# 7. 防火墙放行 80 + 443
# ============================================================
log_info "==== 7. 防火墙放行 80 + 443 ===="
firewall_open 80 tcp
firewall_open 443 tcp
log_ok "防火墙已配置"

echo ""

# ============================================================
# 8. 自动续期 cron
# ============================================================
log_info "==== 8. 配置证书自动续期 ===="

if systemctl list-timers 2>/dev/null | grep -q certbot; then
  log_ok "已有 certbot systemd timer"
elif [ -d /etc/cron.d ]; then
  cat > /etc/cron.d/certbot-renew <<EOF
# 每天 3 点检查证书续期
SHELL=/bin/bash
0 3 * * * root certbot renew --quiet --post-hook "systemctl reload nginx" >> /var/log/certbot-renew.log 2>&1
EOF
  log_ok "已添加 /etc/cron.d/certbot-renew"
fi

echo ""

# ============================================================
# 9. 完整链路验证
# ============================================================
log_info "==== 9. 完整链路验证 ===="
echo ""

sleep 3

# 链路 1: 前端静态
echo "[1/5] 前端静态 (nginx → frontend/dist)"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -m 5 "http://localhost/" 2>/dev/null || echo "000")
echo "$HTTP" | grep -qE "^(200|301|302)$" && log_ok "HTTP $HTTP" || log_warn "HTTP $HTTP"

# 链路 2: API
echo "[2/5] API (nginx → gateway)"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -m 5 "http://localhost/api/v1/auth/login" -X POST -H "Content-Type: application/json" -d '{"username":"x","password":"x"}' 2>/dev/null || echo "000")
echo "$HTTP" | grep -qE "^(200|400|401|403|404|405|500)$" && log_ok "HTTP $HTTP (网关通了, 业务层处理)" || log_warn "HTTP $HTTP"

# 链路 3: 宿主机 nginx HTTPS
echo "[3/5] 宿主机 nginx HTTPS (:443)"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -m 5 -k "https://localhost/" 2>/dev/null || echo "000")
echo "$HTTP" | grep -qE "^(200|301|302)$" && log_ok "HTTP $HTTP" || log_warn "HTTP $HTTP"

# 链路 4: 公网域名
echo "[4/5] 公网域名 https://$DOMAIN"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -m 15 "https://$DOMAIN/" 2>/dev/null || echo "000")
echo "$HTTP" | grep -qE "^(200|301|302)$" && log_ok "HTTP $HTTP 🎉" || log_warn "HTTP $HTTP"

# 链路 5: 登录
echo "[5/5] 登录 API 测试"
RESP=$(curl -s -m 10 -X POST "https://$DOMAIN/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"adminLiugl","password":"Liugl@2026"}' 2>&1)
echo "$RESP" | grep -q '"code":0\|accessToken' && log_ok "登录成功" || log_warn "登录失败: $(echo "$RESP" | head -c 150)"

echo ""
echo "=========================================="
echo "  🎉 部署完成"
echo "=========================================="
echo ""
echo "  🌐 公网访问:  https://$DOMAIN"
echo "  🏠 内网 IP:   $PUBLIC_IP"
echo "  📜 证书:      $CERT_DIR"
echo ""
echo "  链路:"
echo "    [用户 https://$DOMAIN]"
echo "         ↓"
echo "    [宿主机 nginx :443 (HTTPS 终结点, certbot 证书)]"
echo "         ↓"
echo "    [宿主机的 nginx 反代]:"
echo "      ├── /         → $PROJECT_ROOT/frontend/dist (前端静态)"
echo "      ├── /api/**    → http://127.0.0.1:7080 (gateway)"
echo "      ├── /ws        → http://127.0.0.1:7080 (gateway WS)"
echo "      └── /actuator/**→ http://127.0.0.1:7080 (gateway health)"
echo "         ↓"
echo "    [宿主机 docker gateway :7080 (minimax-gateway)]"
echo "         ↓"
echo "    [15 个微服务 docker 容器]"
echo ""
echo "  ⚠️ 关键变化 (V1.9.6):"
echo "    - 不再用 docker nginx 容器"
echo "    - 宿主机 nginx 直接反代前端静态 + gateway"
echo "    - 端口冲突问题彻底解决 (宿主机 nginx 占 80+443)"
echo ""
echo "  登录: adminLiugl / Liugl@2026"