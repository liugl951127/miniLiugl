#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - 备份脚本 (V2.1)
#
# 自动备份:
#   1. MySQL 全库 (mysqldump)
#   2. 数据目录 (mysql/redis/nacos/otel data)
#   3. 应用配置 (application*.yml)
#
# 用法:
#   sudo ./deploy-simple/backup.sh
#   sudo ./deploy-simple/backup.sh --keep=7   # 保留 7 天 (默认)
# =============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/os-detect.sh"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_ok()   { echo -e "${GREEN}[✓]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[!]${NC} $*"; }
log_err()  { echo -e "${RED}[✗]${NC} $*"; }

KEEP_DAYS="${1:-7}"
BACKUP_DIR="/opt/minimax/backup"
DATA_DIR="/opt/minimax/data"
DATE=$(date +%Y%m%d-%H%M%S)

# 解析 --keep=N
for arg in "$@"; do
  case "$arg" in
    --keep=*) KEEP_DAYS="${arg#*=}" ;;
  esac
done

log_info "==== MiniMax 备份 ===="
log_info "保留天数: $KEEP_DAYS"
log_info "备份目录: $BACKUP_DIR"
echo ""

mkdir -p "$BACKUP_DIR"

# 1. MySQL 备份
log_info "==== 1. MySQL 备份 ===="
if docker ps --format "{{.Names}}" | grep -q minimax-mysql; then
  docker exec minimax-mysql mysqldump \
    -uroot -proot123456 \
    --all-databases \
    --single-transaction \
    --routines \
    --triggers \
    --events \
    --quick \
    --lock-tables=false \
    > "$BACKUP_DIR/mysql-$DATE.sql" 2>/dev/null
  
  MYSQL_SIZE=$(du -sh "$BACKUP_DIR/mysql-$DATE.sql" | awk '{print $1}')
  log_ok "MySQL 备份完成: $MYSQL_SIZE"
else
  log_warn "MySQL 容器没在跑, 跳过"
fi
echo ""

# 2. 数据目录备份
log_info "==== 2. 数据目录备份 ===="
if [ -d "$DATA_DIR" ]; then
  tar czf "$BACKUP_DIR/data-$DATE.tar.gz" \
    -C "$(dirname $DATA_DIR)" \
    "$(basename $DATA_DIR)" \
    2>/dev/null
  
  DATA_SIZE=$(du -sh "$BACKUP_DIR/data-$DATE.tar.gz" | awk '{print $1}')
  log_ok "数据目录备份完成: $DATA_SIZE"
else
  log_warn "数据目录不存在: $DATA_DIR"
fi
echo ""

# 3. 应用配置备份
log_info "==== 3. 应用配置备份 ===="
CONFIG_BACKUP="$BACKUP_DIR/config-$DATE.tar.gz"
tar czf "$CONFIG_BACKUP" \
  -C "$SCRIPT_DIR/.." \
  --exclude='backend/target' \
  --exclude='frontend/node_modules' \
  --exclude='frontend/dist' \
  --exclude='.git' \
  backend/*/src/main/resources/application*.yml \
  docker-compose.yml \
  deploy-simple/ \
  scripts/ \
  2>/dev/null || log_warn "配置备份部分失败"

if [ -f "$CONFIG_BACKUP" ]; then
  CONFIG_SIZE=$(du -sh "$CONFIG_BACKUP" | awk '{print $1}')
  log_ok "配置备份完成: $CONFIG_SIZE"
fi
echo ""

# 4. 清理过期
log_info "==== 4. 清理过期备份 ===="
DELETED=$(find "$BACKUP_DIR" -type f -mtime "+$KEEP_DAYS" -delete -print | wc -l)
log_ok "已清理 $DELETED 个过期备份"
echo ""

# 5. 总结
echo "=========================================="
echo "  📦 备份完成"
echo "=========================================="
ls -lah "$BACKUP_DIR/" | tail -10
echo ""
echo "  总大小: $(du -sh $BACKUP_DIR | awk '{print $1}')"
echo "  保留:   $KEEP_DAYS 天"
echo ""
echo "  恢复 MySQL:"
echo "    docker exec -i minimax-mysql mysql -uroot -proot123456 < $BACKUP_DIR/mysql-$DATE.sql"
echo ""
echo "  恢复数据:"
echo "    tar xzf $BACKUP_DIR/data-$DATE.tar.gz -C /opt/minimax/"