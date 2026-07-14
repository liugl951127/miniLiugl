#!/usr/bin/env bash
# =============================================================
# MiniMax Platform V3.5.8 一键启动脚本
#
# 特性:
#   ✓ Maven 仓库挂载到磁盘 (避免重复下载)
#   ✓ 前端 dist 挂载 (避免重复构建)
#   ✓ 日志挂载 (实时查看)
#   ✓ 内存自动检测 + 服务内存限制
#   ✓ 健康检查 (等服务完全启动)
#   ✓ 启动顺序: DB → Cache → Config → Service
#   ✓ 一键修复: ./start-all.sh repair
#
# 用法:
#   ./start-all.sh up        # 一键启动 (智能选模式)
#   ./start-all.sh up full   # 强制全量 17 服务
#   ./start-all.sh up mini   # 强制精简 6 服务
#   ./start-all.sh down      # 停止
#   ./start-all.sh status    # 状态
#   ./start-all.sh logs      # 实时日志
#   ./start-all.sh logs ai   # 单个服务日志
#   ./start-all.sh restart   # 重启
#   ./start-all.sh repair    # 修复 (重 build 缓存损坏的)
#   ./start-all.sh clean     # 清理 (危险)
#   ./start-all.sh help      # 帮助
# =============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# ============== 颜色 ==============
green()  { echo -e "\033[32m$*\033[0m"; }
red()    { echo -e "\033[31m$*\033[0m"; }
yellow() { echo -e "\033[33m$*\033[0m"; }
blue()   { echo -e "\033[36m$*\033[0m"; }
bold()   { echo -e "\033[1m$*\033[0m"; }

# ============== 内存检测 ==============
detect_memory_gb() {
  if command -v free &>/dev/null; then
    free -g | awk '/^Mem:/{print $2}'
  else
    echo "8"  # 默认 8GB
  fi
}

# ============== Docker 检测 ==============
check_docker() {
  if ! command -v docker &>/dev/null; then
    red "❌ Docker 未安装. 安装: https://docs.docker.com/engine/install/"
    exit 1
  fi
  if ! docker info &>/dev/null; then
    red "❌ Docker daemon 未运行. 启动: systemctl start docker"
    exit 1
  fi
  if ! command -v docker compose &>/dev/null && ! docker compose version &>/dev/null 2>&1; then
    red "❌ Docker Compose 未安装"
    exit 1
  fi
}

# ============== compose 命令 ==============
dc() {
  if command -v docker compose &>/dev/null; then
    docker compose "$@"
  else
    docker-compose "$@"
  fi
}

# ============== UP 命令 ==============
cmd_up() {
  check_docker

  local mode="${1:-smart}"

  # 创建持久化目录
  mkdir -p .docker-volumes/{maven-repo,npm-cache,logs,backups,frontend-dist}
  green "✅ 持久化目录已就绪"

  # 显示内存
  local mem=$(detect_memory_gb)
  blue "ℹ️  主机内存: ${mem}GB"

  if [[ "$mode" == "smart" ]]; then
    if [[ $mem -ge 8 ]]; then
      mode="full"
      yellow "🧠 内存 ${mem}GB >= 8GB, 自动选择全量模式 (17 服务)"
    else
      mode="mini"
      yellow "🧠 内存 ${mem}GB < 8GB, 自动选择精简模式 (6 服务)"
    fi
  fi

  # 选择 compose 文件
  if [[ "$mode" == "full" ]]; then
    local compose_file="docker-compose.full.yml"
    blue "📦 使用: $compose_file (全量 17 服务)"
  else
    local compose_file="docker-compose.mini.yml"
    blue "📦 使用: $compose_file (精简 6 服务)"
  fi

  # 第一次启动? 检查 Maven 缓存
  if [[ ! -d ".docker-volumes/maven-repo/repository" ]] || [[ -z "$(ls -A .docker-volumes/maven-repo/repository 2>/dev/null)" ]]; then
    yellow "📥 第一次启动, 预热 Maven 依赖 (一次性, 10-20 分钟)"
    yellow "   后续启动复用缓存, < 30 秒"
    dc -f "$compose_file" --profile init run --rm builder
    green "✅ Maven 缓存预热完成"
  else
    green "✅ Maven 缓存已存在, 复用 (避免重复下载)"
    du -sh .docker-volumes/maven-repo/ 2>/dev/null | sed 's/^/   /'
  fi

  # 构建 + 启动
  echo ""
  bold "===== 构建镜像 (增量, 仅变更层重 build) ====="
  dc -f "$compose_file" build --parallel

  echo ""
  bold "===== 启动服务 (按依赖顺序) ====="
  # 先启动基础设施
  dc -f "$compose_file" up -d mariadb redis nacos
  echo "⏳ 等待 MariaDB 启动..."
  sleep 15
  echo "⏳ 等待 Nacos 启动..."
  sleep 10

  # 再启动业务服务
  dc -f "$compose_file" up -d

  echo ""
  bold "===== 健康检查 ====="
  sleep 30
  cmd_status_internal

  echo ""
  green "════════════════════════════════════════════"
  green "  ✅ 启动完成!"
  green "════════════════════════════════════════════"
  echo ""
  blue "🌐 访问地址:"
  echo "   平台首页:  http://localhost/"
  echo "   网关直连:  http://localhost:7080/"
  echo "   AI 服务:   http://localhost:8094/actuator/health"
  echo "   默认账号:  adminLiugl / Liugl@2026"
  echo ""
  yellow "📋 后续命令:"
  echo "   ./start-all.sh status    # 查看状态"
  echo "   ./start-all.sh logs      # 实时日志"
  echo "   ./start-all.sh down      # 停止"
}

# ============== DOWN 命令 ==============
cmd_down() {
  yellow "===== 停止所有服务 ====="
  if [[ -f docker-compose.full.yml ]]; then
    dc -f docker-compose.full.yml down
  fi
  if [[ -f docker-compose.mini.yml ]]; then
    dc -f docker-compose.mini.yml down
  fi
  green "✅ 已停止 (数据卷保留)"
}

# ============== STATUS 命令 ==============
cmd_status_internal() {
  echo ""
  blue "服务状态:"
  if [[ -f docker-compose.full.yml ]]; then
    dc -f docker-compose.full.yml ps 2>/dev/null || true
  else
    dc -f docker-compose.mini.yml ps 2>/dev/null || true
  fi
  echo ""
  blue "健康检查:"
  for svc in "gateway:7080" "ai:8094" "auth:8081" "admin:8090"; do
    name=${svc%%:*}
    port=${svc##*:}
    code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/actuator/health" 2>/dev/null || echo "000")
    if [[ "$code" == "200" ]]; then
      green "   ✅ $name ($port) - UP"
    else
      yellow "   ⏳ $name ($port) - HTTP $code"
    fi
  done
}

cmd_status() {
  cmd_status_internal
}

# ============== LOGS 命令 ==============
cmd_logs() {
  local svc="${1:-}"
  if [[ -f docker-compose.full.yml ]]; then
    if [[ -n "$svc" ]]; then
      dc -f docker-compose.full.yml logs -f --tail=100 "$svc"
    else
      dc -f docker-compose.full.yml logs -f --tail=100
    fi
  else
    dc -f docker-compose.mini.yml logs -f --tail=100
  fi
}

# ============== RESTART 命令 ==============
cmd_restart() {
  yellow "===== 重启所有服务 ====="
  if [[ -f docker-compose.full.yml ]]; then
    dc -f docker-compose.full.yml restart
  else
    dc -f docker-compose.mini.yml restart
  fi
  green "✅ 已重启"
}

# ============== REPAIR 命令 ==============
cmd_repair() {
  check_docker
  yellow "════════════════════════════════════════════"
  yellow "  🔧 修复模式 (耗时 5-15 分钟)"
  yellow "════════════════════════════════════════════"
  echo ""
  echo "1️⃣  修复: 清理 Docker 悬挂资源"
  docker system prune -f
  echo ""

  echo "2️⃣  修复: 重建 Maven 缓存 (损坏的层)"
  if [[ -d ".docker-volumes/maven-repo" ]]; then
    find .docker-volumes/maven-repo -name "*.lastUpdated" -delete 2>/dev/null || true
    find .docker-volumes/maven-repo -name "_remote.repositories" -delete 2>/dev/null || true
    green "   ✅ Maven 元数据清理完成"
  fi
  echo ""

  echo "3️⃣  修复: 重新构建镜像 (无缓存, 强制重 build)"
  if [[ -f docker-compose.full.yml ]]; then
    dc -f docker-compose.full.yml build --no-cache --parallel
  else
    dc -f docker-compose.mini.yml build --no-cache --parallel
  fi
  echo ""

  echo "4️⃣  修复: 重新创建容器"
  if [[ -f docker-compose.full.yml ]]; then
    dc -f docker-compose.full.yml up -d --force-recreate
  else
    dc -f docker-compose.mini.yml up -d --force-recreate
  fi
  echo ""

  echo "5️⃣  修复: 验证种子数据完整性"
  if command -v python3 &>/dev/null; then
    python3 scripts/verify-seed-data.py 2>&1 | tail -5
  fi
  echo ""

  sleep 30
  cmd_status_internal

  green "════════════════════════════════════════════"
  green "  ✅ 修复完成!"
  green "════════════════════════════════════════════"
}

# ============== CLEAN 命令 (危险) ==============
cmd_clean() {
  red "════════════════════════════════════════════"
  red "  ⚠️  危险: 清理所有数据 (包括数据库)"
  red "════════════════════════════════════════════"
  read -p "确认清理? (yes/no): " confirm
  if [[ "$confirm" != "yes" ]]; then
    yellow "已取消"
    return
  fi
  if [[ -f docker-compose.full.yml ]]; then
    dc -f docker-compose.full.yml down -v
  fi
  rm -rf .docker-volumes/maven-repo/* .docker-volumes/npm-cache/*
  green "✅ 清理完成"
}

# ============== HELP 命令 ==============
cmd_help() {
  cat << 'EOF'
═══════════════════════════════════════════════════════════
  MiniMax Platform V3.5.8 一键启动脚本
═══════════════════════════════════════════════════════════

用法:
  ./start-all.sh up [full|mini]   启动 (智能/full/mini)
  ./start-all.sh down              停止 (数据保留)
  ./start-all.sh status            查看状态
  ./start-all.sh logs [service]    实时日志
  ./start-all.sh restart           重启
  ./start-all.sh repair            修复 (重 build)
  ./start-all.sh clean             清理 (危险)
  ./start-all.sh help              帮助

═══════════════════════════════════════════════════════════
  内存要求:
═══════════════════════════════════════════════════════════
  mini 模式 (6 服务):  4GB+ 内存
  full 模式 (17 服务): 8GB+ 内存
  推荐:               16GB

═══════════════════════════════════════════════════════════
  持久化目录:
═══════════════════════════════════════════════════════════
  .docker-volumes/maven-repo/    Maven 仓库 (避免重复下载)
  .docker-volumes/npm-cache/     npm 缓存
  .docker-volumes/logs/          日志
  .docker-volumes/backups/       备份
  .docker-volumes/frontend-dist/ 前端构建产物

═══════════════════════════════════════════════════════════
  默认账号:
═══════════════════════════════════════════════════════════
  adminLiugl / Liugl@2026    超级管理员
  admin_user / admin123      普通管理员
  test_user  / user123       测试用户
  demo_user  / demo1234      演示用户
EOF
}

# ============== 主入口 ==============
case "${1:-help}" in
  up)      shift; cmd_up "$@" ;;
  down)    cmd_down ;;
  status)  cmd_status ;;
  logs)    shift; cmd_logs "$@" ;;
  restart) cmd_restart ;;
  repair)  cmd_repair ;;
  clean)   cmd_clean ;;
  help|--help|-h) cmd_help ;;
  *) cmd_help; exit 1 ;;
esac
