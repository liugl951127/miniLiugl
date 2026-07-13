#!/usr/bin/env bash
# ============================================================
# MiniMax Platform V3.5.5+ 精简部署脚本
# 6 个核心容器: mariadb + redis + auth + ai + gateway + nginx
# 适用: 2GB 内存 / 沙箱 / 开发测试 / 树莓派
# 用法:
#   ./deploy-mini.sh up      # 一键启动
#   ./deploy-mini.sh down    # 停止
#   ./deploy-mini.sh build   # 重新构建镜像
#   ./deploy-mini.sh status  # 查看状态
#   ./deploy-mini.sh logs    # 查看日志
# ============================================================

set -e

COMPOSE_FILE="docker-compose.mini.yml"
PROJECT="minimax-mini"

green() { echo -e "\033[32m$*\033[0m"; }
red()   { echo -e "\033[31m$*\033[0m"; }
yellow(){ echo -e "\033[33m$*\033[0m"; }

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

cmd_up() {
  check_docker
  green "🚀 MiniMax 精简部署 (6 容器)"
  green "   内存需求: ~1.5GB"
  echo ""

  # 检查端口
  for p in 80 3306 6379 7080 8081 8094; do
    if ss -ltn 2>/dev/null | grep -q ":$p "; then
      yellow "⚠  端口 $p 已被占用, 可能冲突"
    fi
  done

  # 启动
  docker compose -f $COMPOSE_FILE -p $PROJECT up -d --build

  echo ""
  green "✅ 部署完成!"
  echo ""
  green "🌐 访问入口:"
  green "   前端:    http://<server-ip>/"
  green "   API 网关: http://<server-ip>:7080"
  green "   鉴权:    http://<server-ip>:8081/actuator/health"
  green "   AI:      http://<server-ip>:8094/actuator/health"
  echo ""
  green "🔑 默认账号: adminLiugl / Liugl@2026"
  green "⏳ 启动约需 60-90 秒, 查看日志: ./deploy-mini.sh logs"
}

cmd_down() {
  check_docker
  yellow "⏹  停止 MiniMax 精简部署"
  docker compose -f $COMPOSE_FILE -p $PROJECT down
  green "✅ 已停止"
}

cmd_build() {
  check_docker
  yellow "🔨 重新构建镜像"
  docker compose -f $COMPOSE_FILE -p $PROJECT build --no-cache
  green "✅ 构建完成"
}

cmd_status() {
  check_docker
  docker compose -f $COMPOSE_FILE -p $PROJECT ps
  echo ""
  yellow "📊 资源使用:"
  docker stats --no-stream $(docker compose -f $COMPOSE_FILE -p $PROJECT ps -q) 2>/dev/null || true
}

cmd_logs() {
  check_docker
  docker compose -f $COMPOSE_FILE -p $PROJECT logs -f --tail=100
}

cmd_restart() {
  check_docker
  yellow "🔄 重启服务"
  docker compose -f $COMPOSE_FILE -p $PROJECT restart
  green "✅ 已重启"
}

case "${1:-up}" in
  up)         cmd_up ;;
  down)       cmd_down ;;
  build)      cmd_build ;;
  status)     cmd_status ;;
  logs)       cmd_logs ;;
  restart)    cmd_restart ;;
  *)          red "未知命令: $1"; echo "用法: $0 {up|down|build|status|logs|restart}"; exit 1 ;;
esac
