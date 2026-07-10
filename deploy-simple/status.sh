#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - 状态检查 (V2.1) 开箱即用
#
# 一键看:
#   - 容器状态 + 内存占用
#   - 服务健康检查
#   - 端口监听
#   - 磁盘 + 内存使用
#   - 关键 URL 测试
#
# 用法:
#   sudo ./deploy-simple/status.sh
# =============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/os-detect.sh"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; CYAN='\033[0;36m'; NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║${NC}        ${BLUE}MiniMax Platform - 状态检查${NC}                            ${CYAN}║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# 1. OS 信息
echo -e "${BLUE}[1/7] 系统信息${NC}"
echo "  OS:      $OS_PRETTY"
echo "  Kernel:  $(uname -r)"
echo "  CPU:     $(nproc) cores"
echo "  Memory:  $(free -h | awk '/Mem:/ {print $2 " (used: " $3 ", " $3/$2*100 "%)"}')"
echo "  Disk:    $(df -h /opt/minimax 2>/dev/null | tail -1 | awk '{print $4 " free of " $2}')"
echo ""

# 2. Docker 状态
echo -e "${BLUE}[2/7] Docker${NC}"
if command -v docker &>/dev/null; then
  echo -e "  ${GREEN}✓${NC} Docker $(docker --version | awk '{print $3}' | tr -d ',')"
  echo -e "  ${GREEN}✓${NC} Compose $(docker compose version --short 2>/dev/null || echo 'unknown')"
else
  echo -e "  ${RED}✗${NC} Docker 未安装"
fi
echo ""

# 3. 容器状态
echo -e "${BLUE}[3/7] 容器状态${NC}"
cd "$(find / -name 'docker-compose.yml' -path '*/miniLiugl/*' 2>/dev/null | head -1 | xargs dirname)" 2>/dev/null || cd /opt/miniLiugl

RUNNING=$(docker compose ps --services --filter "status=running" 2>/dev/null | wc -l)
TOTAL=$(docker compose ps --services 2>/dev/null | wc -l)
echo "  总计: $TOTAL / 运行: $RUNNING"
echo ""
docker compose ps --format "  {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null | head -20
echo ""

# 4. 内存占用
echo -e "${BLUE}[4/7] 内存占用 Top 5${NC}"
docker stats --no-stream --format "  {{.Name}}\t{{.MemUsage}}\t{{.MemPerc}}" 2>/dev/null | \
  sort -k3 -h -r | head -5
echo ""

# 5. 端口监听
echo -e "${BLUE}[5/7] 关键端口${NC}"
for port in 80 443 3306 6379 7080 8081 8848 4318; do
  if ss -tln 2>/dev/null | grep -q ":$port "; then
    PROC=$(ss -tlnp 2>/dev/null | grep ":$port " | head -1 | grep -oP 'users:\(\("([^"]+)"' | head -1 | sed 's/users:(("//')
    echo -e "  ${GREEN}●${NC} :$port ($PROC)"
  else
    echo -e "  ${RED}○${NC} :$port (未监听)"
  fi
done
echo ""

# 6. 健康检查
echo -e "${BLUE}[6/7] 健康检查${NC}"
HEALTH_URLS=(
  "http://localhost/actuator/health/liveness"
  "http://localhost:7080/actuator/health"
  "http://localhost:8081/actuator/health"
  "http://localhost:8848/nacos/"
)
for url in "${HEALTH_URLS[@]}"; do
  HTTP=$(curl -s -m 3 -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
  if echo "$HTTP" | grep -qE "^(200|401)$"; then
    echo -e "  ${GREEN}✓${NC} $url (HTTP $HTTP)"
  else
    echo -e "  ${YELLOW}?${NC} $url (HTTP $HTTP)"
  fi
done
echo ""

# 7. 数据卷
echo -e "${BLUE}[7/7] 数据卷${NC}"
DATA_DIR="/opt/minimax/data"
if [ -d "$DATA_DIR" ]; then
  du -sh "$DATA_DIR"/* 2>/dev/null | head -10 | sed 's/^/  /'
else
  echo -e "  ${YELLOW}⚠${NC} 数据目录不存在: $DATA_DIR"
fi
echo ""

# 总结
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
if [ "$RUNNING" -eq "$TOTAL" ] && [ "$RUNNING" -gt 0 ]; then
  echo -e "  ${GREEN}✓ 全部 $RUNNING 个服务运行中${NC}"
elif [ "$RUNNING" -gt 0 ]; then
  echo -e "  ${YELLOW}⚠ 部分服务运行 ($RUNNING/$TOTAL)${NC}"
else
  echo -e "  ${RED}✗ 没有服务在运行${NC}"
  echo ""
  echo "  启动命令: sudo ./deploy-simple/docker-deploy.sh up"
fi
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"