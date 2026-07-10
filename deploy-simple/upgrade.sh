#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - 一键升级 (V2.2)
#
# 自动做:
#   1. 备份当前数据
#   2. git pull 最新代码
#   3. 检测代码变更 (frontend / backend / configs)
#   4. 重新构建受影响的镜像
#   5. 重启服务 (零停机滚动)
#   6. 验证健康状态
#
# 用法:
#   sudo ./deploy-simple/upgrade.sh                # 升级到最新
#   sudo ./deploy-simple/upgrade.sh --no-backup    # 不备份
#   sudo ./deploy-simple/upgrade.sh --rebuild-all  # 全部重建
# =============================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/os-detect.sh"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; CYAN='\033[0;36m'; NC='\033[0m'
log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_ok()   { echo -e "${GREEN}[✓]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[!]${NC} $*"; }
log_err()  { echo -e "${RED}[✗]${NC} $*"; }

# 找项目根目录
PROJECT_ROOT=""
for p in /opt/miniLiugl /root/miniLiugl /home/*/miniLiugl "$(pwd)/miniLiugl" "$(pwd)"; do
  if [ -f "$p/docker-compose.yml" ] && grep -q "minimax-gateway" "$p/docker-compose.yml" 2>/dev/null; then
    PROJECT_ROOT="$p"
    break
  fi
done

if [ -z "$PROJECT_ROOT" ]; then
  log_err "找不到 miniLiugl 项目目录"
  exit 1
fi

cd "$PROJECT_ROOT"

# 解析参数
NO_BACKUP=0
REBUILD_ALL=0
for arg in "$@"; do
  case "$arg" in
    --no-backup)    NO_BACKUP=1 ;;
    --rebuild-all)  REBUILD_ALL=1 ;;
    -h|--help)
      cat << 'USAGE'
用法: sudo ./upgrade.sh [options]

Options:
  --no-backup     跳过备份 (生产环境慎用)
  --rebuild-all   强制重建所有镜像 (不只是变更的)
  -h, --help      显示帮助

示例:
  sudo ./upgrade.sh               # 标准升级 (备份 + 增量构建)
  sudo ./upgrade.sh --no-backup   # 不备份, 快速升级
  sudo ./upgrade.sh --rebuild-all # 全部强制重建
USAGE
      exit 0
      ;;
  esac
done

log_info "=========================================="
log_info " MiniMax Platform - 一键升级 (V2.2)"
log_info "=========================================="
log_info "项目: $PROJECT_ROOT"
log_info "当前: $(git rev-parse --short HEAD) ($(git log -1 --format='%h %s' HEAD))"
echo ""

# 1. 备份
if [ "$NO_BACKUP" = "0" ]; then
  log_info "==== 1. 自动备份 ===="
  bash "$SCRIPT_DIR/backup.sh" 7 2>&1 | tail -10
  echo ""
else
  log_warn "跳过备份 (--no-backup)"
fi

# 2. 拉最新代码
log_info "==== 2. 拉最新代码 ===="
# 用环境变量里的 token 拉代码 (不在仓库里保存 token)
# 1) 优先从 ~/.git-credentials (git 自动用)
# 2) 否则从 $MINIMAX_GITHUB_TOKEN 环境变量
# 3) 否则跳过 remote set-url (假设已配)
if [ -n "$MINIMAX_GITHUB_TOKEN" ]; then
  git remote set-url origin "https://${MINIMAX_GITHUB_TOKEN}@github.com/liugl951127/miniLiugl.git" 2>/dev/null
fi
OLD_HEAD=$(git rev-parse --short HEAD)
git pull origin main 2>&1 | tail -5
git remote set-url origin "https://github.com/liugl951127/miniLiugl.git"
NEW_HEAD=$(git rev-parse --short HEAD)
log_ok "代码: $OLD_HEAD -> $NEW_HEAD"

# 3. 检测代码变更
log_info "==== 3. 检测代码变更 ===="
if [ "$OLD_HEAD" = "$NEW_HEAD" ]; then
  log_warn "代码无变化, 跳过重建"
  exit 0
fi

CHANGED_FILES=$(git diff --name-only "$OLD_HEAD" "$NEW_HEAD" 2>/dev/null)
echo "$CHANGED_FILES" | head -10 | sed 's/^/  /'
echo ""

NEED_REBUILD_BACKEND=$(echo "$CHANGED_FILES" | grep -E "^backend/" | wc -l)
NEED_REBUILD_FRONTEND=$(echo "$CHANGED_FILES" | grep -E "^frontend/" | wc -l)
NEED_REBUILD_NGINX=$(echo "$CHANGED_FILES" | grep -E "^(scripts/nginx|deploy-simple/docker-deploy|docker-compose)" | wc -l)

log_info "变更统计:"
echo "  backend 变更文件: $NEED_REBUILD_BACKEND"
echo "  frontend 变更文件: $NEED_REBUILD_FRONTEND"
echo "  nginx/compose 变更: $NEED_REBUILD_NGINX"
echo ""

# 4. 重新构建
if [ "$REBUILD_ALL" = "1" ] || [ "$NEED_REBUILD_BACKEND" -gt 0 ] || [ "$NEED_REBUILD_FRONTEND" -gt 0 ]; then
  log_info "==== 4. 重新构建镜像 ===="
  
  if [ "$REBUILD_ALL" = "1" ] || [ "$NEED_REBUILD_BACKEND" -gt 0 ]; then
    log_info "重建后端模块..."
    mvn clean install -s .mvn/settings.xml -DskipTests -T 4 2>&1 | tail -10
  fi
  
  if [ "$REBUILD_ALL" = "1" ] || [ "$NEED_REBUILD_FRONTEND" -gt 0 ]; then
    log_info "重建前端..."
    cd frontend
    if [ ! -d node_modules ]; then
      npm config set registry https://registry.npmmirror.com
      npm install --silent
    fi
    npm run build 2>&1 | tail -5
    cd ..
  fi
  
  # Docker compose rebuild
  if [ "$REBUILD_ALL" = "1" ]; then
    log_info "全部重建 docker 镜像..."
    docker compose build --no-cache 2>&1 | tail -10
  else
    log_info "增量重建..."
    docker compose build 2>&1 | tail -10
  fi
  log_ok "镜像构建完成"
else
  log_info "无需重建镜像 (仅配置变更)"
fi
echo ""

# 5. 滚动重启
log_info "==== 5. 滚动重启服务 ===="

# 先停 gateway (入口)
log_info "重启 gateway..."
docker compose up -d --no-deps gateway 2>&1 | tail -3
sleep 5

# 再重启其他服务
log_info "重启其他服务..."
docker compose up -d 2>&1 | tail -10

# 健康检查
sleep 10
log_info "==== 6. 健康检查 ===="
HEALTHY=$(docker compose ps --services --filter "status=running" 2>/dev/null | wc -l)
TOTAL=$(docker compose ps --services 2>/dev/null | wc -l)

if [ "$HEALTHY" -eq "$TOTAL" ]; then
  log_ok "全部 $HEALTHY 个服务运行中"
else
  log_warn "部分服务运行: $HEALTHY/$TOTAL"
fi

# 关键 URL 测试
for url in \
  "http://localhost/actuator/health/liveness" \
  "http://localhost:7080/actuator/health" \
  "http://localhost:8081/actuator/health"; do
  HTTP=$(curl -s -m 5 -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
  if echo "$HTTP" | grep -qE "^(200|401)$"; then
    log_ok "$url HTTP $HTTP"
  else
    log_warn "$url HTTP $HTTP"
  fi
done

echo ""
echo "=========================================="
echo "  🎉 升级完成"
echo "=========================================="
echo "  旧: $OLD_HEAD"
echo "  新: $NEW_HEAD"
echo "  服务: $HEALTHY/$TOTAL 运行中"
echo ""
echo "  下一步:"
echo "    ./deploy-simple/docker-deploy.sh status   # 看状态"
echo "    ./deploy-simple/docker-deploy.sh logs gateway   # 看日志"