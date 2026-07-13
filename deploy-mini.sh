#!/usr/bin/env bash
# =============================================================
# MiniMax Platform V3.5.5+ 一键部署脚本 (前端 + 后端 + nginx)
# 6 个核心容器: mariadb + redis + auth + ai + gateway + nginx
# 适用: 2GB 内存 / 沙箱 / 开发测试 / 树莓派 / 单 VPS
#
# 用法:
#   ./deploy-mini.sh up        # 一键启动 (自动 build 前端 + 后端)
#   ./deploy-mini.sh down      # 停止
#   ./deploy-mini.sh rebuild   # 重新构建 (前端 + 后端 + 镜像)
#   ./deploy-mini.sh build     # 只重新构建 Docker 镜像
#   ./deploy-mini.sh status    # 查看状态
#   ./deploy-mini.sh logs      # 实时日志
#   ./deploy-mini.sh restart   # 重启
#   ./deploy-mini.sh clean     # 清理所有数据 (危险)
#
# 部署后访问:
#   前端 SPA:  http://<server-ip>/        (nginx 代理)
#   API 网关:  http://<server-ip>:7080
#   健康检查:  http://<server-ip>/healthz
# =============================================================

set -e

COMPOSE_FILE="docker-compose.mini.yml"
PROJECT="minimax-mini"

# 颜色输出
green()  { echo -e "\033[32m$*\033[0m"; }
red()    { echo -e "\033[31m$*\033[0m"; }
yellow() { echo -e "\033[33m$*\033[0m"; }
blue()   { echo -e "\033[36m$*\033[0m"; }
bold()   { echo -e "\033[1m$*\033[0m"; }

# 检查 Docker
check_docker() {
  if ! command -v docker &>/dev/null; then
    red "❌ docker 未安装"
    exit 1
  fi
  if ! docker info &>/dev/null; then
    red "❌ docker daemon 未运行, 请执行: systemctl start docker"
    exit 1
  fi
}

# 检查 Node.js (用于构建前端)
check_node() {
  if ! command -v node &>/dev/null; then
    yellow "⚠  node 未安装, 跳过前端构建, 使用预构建 dist/"
    return 1
  fi
  if ! command -v npm &>/dev/null; then
    yellow "⚠  npm 未安装, 跳过前端构建"
    return 1
  fi
  NODE_VER=$(node -v 2>/dev/null | sed 's/v//' | cut -d. -f1)
  if [ "${NODE_VER:-0}" -lt 18 ]; then
    yellow "⚠  node 版本 < 18, 跳过前端构建 (需要 18+)"
    return 1
  fi
  return 0
}

# 构建前端 (Vue3 SPA → dist/)
build_frontend() {
  blue "📦 [1/3] 构建前端 (Vue3 + Vite)"
  if ! check_node; then
    yellow "   使用预构建 dist/ (frontend/dist/index.html)"
    return 0
  fi
  cd frontend
  if [ ! -d node_modules ]; then
    yellow "   安装前端依赖..."
    npm config set registry https://registry.npmmirror.com 2>/dev/null || true
    npm install --no-audit --no-fund --silent
  fi
  # 构建生产版
  green "   ⚡ 正在构建 (vue-router history 模式, base=/)..."
  npm run build 2>&1 | tail -15
  cd ..
  if [ ! -f frontend/dist/index.html ]; then
    red "❌ 前端构建失败, 找不到 frontend/dist/index.html"
    exit 1
  fi
  green "   ✓ 前端构建完成: $(du -sh frontend/dist 2>/dev/null | cut -f1)"
}

# 构建后端镜像 (Maven 多模块)
build_backend() {
  blue "📦 [2/3] 构建后端镜像 (Maven)"
  if ! command -v mvn &>/dev/null; then
    yellow "   ⚠  mvn 未安装, 将使用 docker 内构建 (首次慢)"
    return 0
  fi
  if [ ! -d backend/target ]; then
    yellow "   ⚙  本地编译 minimax-auth / minimax-ai / minimax-gateway 三个核心服务..."
    cd backend
    mvn -pl minimax-auth,minimax-ai,minimax-gateway -am clean package \
        -DskipTests -Dcheckstyle.skip -Dpmd.skip -Dmaven.javadoc.skip \
        -Dspotless.check.skip=true -q 2>&1 | tail -5 || true
    cd ..
  else
    green "   ✓ 本地已有编译产物, 复用"
  fi
}

cmd_up() {
  check_docker
  bold ""
  bold "🚀 MiniMax 精简部署 (前端 + 后端 + nginx 代理)"
  bold "   内存需求: ~1.5GB / 6 容器"
  bold ""

  # 检查端口
  for p in 80 3306 6379 7080 8081 8094; do
    if ss -ltn 2>/dev/null | grep -q ":$p "; then
      yellow "⚠  端口 $p 已被占用, 可能冲突"
    fi
  done

  # 1. 构建前端
  build_frontend

  # 2. 构建后端 (可选)
  build_backend

  # 3. 启动 docker compose
  blue "📦 [3/3] 启动 Docker Compose"
  docker compose -f $COMPOSE_FILE -p $PROJECT up -d --build

  echo ""
  green "✅ 部署完成!"
  echo ""
  bold "🌐 访问入口 (单端口 80 统一入口, nginx 代理):"
  green "   前端 SPA:     http://<server-ip>/"
  green "   登录页:       http://<server-ip>/#/login"
  green "   控制台:       http://<server-ip>/#/dashboard"
  green "   健康检查:     http://<server-ip>/healthz"
  green "   API 网关:     http://<server-ip>:7080"
  green "   鉴权服务:     http://<server-ip>:8081/actuator/health"
  green "   AI 服务:      http://<server-ip>:8094/actuator/health"
  echo ""
  green "🔑 默认账号: adminLiugl / Liugl@2026"
  yellow "⏳ 首次启动约需 60-90 秒, 实时查看: ./deploy-mini.sh logs"
  echo ""
  yellow "💡 前端路由 /dashboard 等会通过 nginx fallback 到 index.html (SPA history 模式)"
}

cmd_down() {
  check_docker
  yellow "⏹  停止 MiniMax 精简部署"
  docker compose -f $COMPOSE_FILE -p $PROJECT down
  green "✅ 已停止 (数据保留)"
}

cmd_rebuild() {
  check_docker
  bold ""
  bold "🔨 完整重建 (前端 + 后端 + Docker 镜像)"
  build_frontend
  build_backend
  yellow "重建 Docker 镜像 (无缓存)..."
  docker compose -f $COMPOSE_FILE -p $PROJECT build --no-cache
  green "✅ 重建完成, 启动: ./deploy-mini.sh up"
}

cmd_build() {
  check_docker
  yellow "🔨 重新构建 Docker 镜像 (无缓存)"
  docker compose -f $COMPOSE_FILE -p $PROJECT build --no-cache
  green "✅ 构建完成"
}

cmd_status() {
  check_docker
  bold ""
  bold "📊 容器状态:"
  docker compose -f $COMPOSE_FILE -p $PROJECT ps
  echo ""
  bold "💾 资源使用:"
  docker stats --no-stream $(docker compose -f $COMPOSE_FILE -p $PROJECT ps -q 2>/dev/null) 2>/dev/null || true
  echo ""
  bold "🌐 端口监听:"
  ss -ltn 2>/dev/null | grep -E ":80|:3306|:6379|:7080|:8081|:8094" | head -10
}

cmd_logs() {
  check_docker
  yellow "📜 实时日志 (Ctrl+C 退出):"
  docker compose -f $COMPOSE_FILE -p $PROJECT logs -f --tail=100
}

cmd_restart() {
  check_docker
  yellow "🔄 重启服务"
  docker compose -f $COMPOSE_FILE -p $PROJECT restart
  green "✅ 已重启"
}

cmd_clean() {
  check_docker
  yellow "⚠  即将清理所有容器 + 数据卷 (不可恢复!)"
  read -p "确认清理? (输入 yes 继续): " confirm
  if [ "$confirm" = "yes" ]; then
    docker compose -f $COMPOSE_FILE -p $PROJECT down -v
    green "✅ 已清理所有数据"
  else
    yellow "❌ 取消"
  fi
}

cmd_help() {
  bold ""
  bold "MiniMax Platform 一键部署脚本 (V3.5.5+)"
  bold ""
  green "用法: $0 <命令> [选项]"
  echo ""
  echo "  up        一键启动 (自动 build 前端 + 后端 + 启动 docker)"
  echo "  rebuild   完整重建 (前端 + 后端 + Docker 镜像, 慢)"
  echo "  build     只重新构建 Docker 镜像"
  echo "  down      停止 (数据保留)"
  echo "  status    查看状态 (容器 + 资源 + 端口)"
  echo "  logs      实时日志"
  echo "  restart   重启所有容器"
  echo "  clean     清理所有数据 (危险, 需输入 yes 确认)"
  echo "  help      显示帮助"
  echo ""
  echo "示例:"
  green "  ./deploy-mini.sh up        # 一键部署并启动"
  green "  ./deploy-mini.sh logs      # 查看实时日志"
  green "  ./deploy-mini.sh status    # 查看运行状态"
  echo ""
  echo "部署后:"
  green "  http://<server-ip>/        # 前端 SPA (nginx 代理)"
  green "  http://<server-ip>/healthz # 健康检查"
  echo ""
}

case "${1:-help}" in
  up)        cmd_up ;;
  down)      cmd_down ;;
  rebuild)   cmd_rebuild ;;
  build)     cmd_build ;;
  status)    cmd_status ;;
  logs)      cmd_logs ;;
  restart)   cmd_restart ;;
  clean)     cmd_clean ;;
  help|-h|--help) cmd_help ;;
  *)         red "未知命令: $1"; cmd_help; exit 1 ;;
esac
