#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - 数据目录初始化 (V1.9.3)
#
# 自动创建 /opt/minimax/data/{mysql,redis,nacos,otel} 目录
# 并设置正确的 SELinux 上下文 (CentOS Stream 9 / RHEL 9)
# =============================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/os-detect.sh"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_ok()   { echo -e "${GREEN}[✓]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[!]${NC} $*"; }
log_err()  { echo -e "${RED}[✗]${NC} $*"; }

DATA_ROOT="${DATA_ROOT:-/opt/minimax/data}"
SERVICE_USER="${SERVICE_USER:-}"
detect_os 2>/dev/null

log_info "==== 创建数据目录 $DATA_ROOT ===="
mkdir -p "$DATA_ROOT"/{mysql,redis,nacos,otel,logs}
log_ok "目录创建完成: $DATA_ROOT/{mysql,redis,nacos,otel,logs}"

# 设置权限
# Docker 容器内进程通常以 UID 999 (mysql) 或 1000 (redis) 运行
# 简单做法: 777 权限, 让任何容器能读写
chmod -R 777 "$DATA_ROOT"
log_ok "权限: chmod -R 777"

# CentOS Stream 9 / RHEL 9 SELinux 处理
if [ "$OS_FAMILY" = "rhel" ] && command -v getenforce &>/dev/null; then
  if [ "$(getenforce)" = "Enforcing" ]; then
    log_info "SELinux Enforcing, 设置容器可读写上下文..."
    # docker SELinux 标签: container_file_t 或 svirt_sandbox_file_t
    chcon -Rt svirt_sandbox_file_t "$DATA_ROOT" 2>/dev/null || \
    chcon -Rt container_file_t "$DATA_ROOT" 2>/dev/null || true
    # 或者最稳: 让容器用 :z 自动重新打标签
    log_ok "SELinux 标签已应用 (svirt_sandbox_file_t)"
    log_warn "如仍有问题, 编辑 docker-compose.yml 加 :z 后缀:"
    echo "    - ${DATA_ROOT}/mysql:/var/lib/mysql:z"
  fi
fi

# 让 docker compose 能 bind mount, 可能需要 firewalld 放行 docker0
if command -v firewall-cmd &>/dev/null; then
  firewall-cmd --zone=trusted --add-interface=docker0 2>/dev/null || true
  firewall-cmd --reload 2>/dev/null || true
fi

# 显示磁盘使用
echo ""
log_info "==== 磁盘使用 ===="
du -sh "$DATA_ROOT"/* 2>/dev/null
df -h "$DATA_ROOT" | tail -1 | awk '{print "  剩余: "$4" / 总共: "$2" ("$5" used)"}'

echo ""
log_ok "数据目录初始化完成"
echo ""
echo "  使用:"
echo "    DATA_ROOT=$DATA_ROOT docker compose up -d"
echo ""
echo "  自定义路径:"
echo "    DATA_ROOT=/data/minimax $0"
echo "    DATA_ROOT=/data/minimax docker compose up -d"
echo ""
echo "  备份:"
echo "    tar czf minimax-data-\$(date +%Y%m%d).tgz $DATA_ROOT"
echo ""
echo "  恢复:"
echo "    tar xzf minimax-data-20260101.tgz -C /"