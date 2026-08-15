#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - 公网链路验证脚本 (V1.9.5)
#
# 一键验证 4 段链路:
#   1. docker nginx (宿主机 :80) → 前端静态
#   2. gateway (:7080) → 后端 API
#   3. 宿主机 nginx (:443) → 反代到 docker nginx
#   4. 公网域名 (https://DOMAIN) → 整条链路
#
# 用法:
#   sudo ./deploy-simple/verify-public-domain.sh [your-domain.com]
# =============================================================

set -e
DOMAIN="${1:-}"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log_ok()   { echo -e "${GREEN}[✓]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[!]${NC} $*"; }
log_err()  { echo -e "${RED}[✗]${NC} $*"; }

echo "=========================================="
echo "  🔍 MiniMax 链路验证 (V1.9.5)"
echo "=========================================="
echo ""

# 链路 1: docker nginx
echo "[1/4] docker nginx (宿主机 :80)"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -m 5 "http://localhost/" 2>/dev/null || echo "000")
if echo "$HTTP" | grep -qE "^(200|301|302)$"; then
  log_ok "HTTP $HTTP"
else
  log_err "HTTP $HTTP"
  echo "       docker compose logs nginx"
fi

# 链路 2: gateway
echo "[2/4] gateway (宿主机 :7080)"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -m 5 "http://localhost:7080/actuator/health" 2>/dev/null || echo "000")
if echo "$HTTP" | grep -qE "^(200|301|302|401|403)$"; then
  log_ok "HTTP $HTTP"
else
  log_err "HTTP $HTTP"
  echo "       docker compose logs gateway"
fi

# 链路 3: 宿主机 nginx HTTPS
echo "[3/4] 宿主机 nginx HTTPS (:443)"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -m 5 -k "https://localhost/" 2>/dev/null || echo "000")
if echo "$HTTP" | grep -qE "^(200|301|302)$"; then
  log_ok "HTTP $HTTP"
else
  log_err "HTTP $HTTP"
  echo "       nginx -t && systemctl status nginx"
fi

# 链路 4: 公网域名
if [ -n "$DOMAIN" ]; then
  echo "[4/4] 公网域名 https://$DOMAIN"
  HTTP=$(curl -s -o /dev/null -w "%{http_code}" -m 15 "https://$DOMAIN/" 2>/dev/null || echo "000")
  if echo "$HTTP" | grep -qE "^(200|301|302)$"; then
    log_ok "HTTP $HTTP 🎉"
  else
    log_err "HTTP $HTTP"
    echo "       排查 DNS / 云安全组 / firewalld"
  fi
  
  # 登录测试
  echo ""
  echo "[*] 登录 API 测试"
  RESP=$(curl -s -m 10 -X POST "https://$DOMAIN/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"adminLiugl","password":"Liugl@2026"}' 2>&1)
  if echo "$RESP" | grep -q '"code":0\|accessToken'; then
    log_ok "登录成功"
    echo "    $(echo "$RESP" | head -c 150)..."
  else
    log_err "登录失败"
    echo "    $(echo "$RESP" | head -c 200)"
  fi
else
  echo "[4/4] 公网域名 (跳过, 没指定 DOMAIN)"
  echo "       用法: $0 your-domain.com"
fi

echo ""
echo "=========================================="
echo "  📊 容器状态"
echo "=========================================="
docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>&1 | head -25