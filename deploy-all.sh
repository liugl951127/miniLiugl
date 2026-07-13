#!/usr/bin/env bash
# =============================================================
# MiniMax Platform V3.5.5+ 一键部署 (智能统一入口)
# 自动检测环境, 智能选择部署模式:
#   - Docker mini  (6 容器, 1.5GB)  ← 默认
#   - Docker full  (21 容器, 8GB+)
#   - 宿主机后台  (systemd / nohup)
#   - H2 沙箱     (单 jar, 512MB)
#
# 一次部署包含: 前端 (Vue3 SPA) + 后端 (16 微服务) + nginx 代理
#
# 用法:
#   ./deploy-all.sh up         # 一键启动 (智能选模式)
#   ./deploy-all.sh up mini    # 强制 mini 模式
#   ./deploy-all.sh up full    # 强制 full 模式
#   ./deploy-all.sh up host    # 宿主机后台
#   ./deploy-all.sh up h2      # H2 沙箱 (单服务)
#   ./deploy-all.sh down       # 停止
#   ./deploy-all.sh status     # 查看状态
#   ./deploy-all.sh logs       # 实时日志
#   ./deploy-all.sh restart    # 重启
#   ./deploy-all.sh clean      # 清理 (危险)
#   ./deploy-all.sh help       # 帮助
# =============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

green()  { echo -e "\033[32m$*\033[0m"; }
red()    { echo -e "\033[31m$*\033[0m"; }
yellow() { echo -e "\033[33m$*\033[0m"; }
blue()   { echo -e "\033[36m$*\033[0m"; }
bold()   { echo -e "\033[1m$*\033[0m"; }

# 检测内存
detect_memory_gb() {
  if command -v free &>/dev/null; then
    free -g | awk '/^Mem:/{print $2}'
  else
    echo 2
  fi
}

# 检测模式
auto_detect_mode() {
  local mem_gb=$(detect_memory_gb)
  if [ "$mem_gb" -lt 2 ]; then
    echo "h2"
  elif [ "$mem_gb" -lt 4 ]; then
    echo "mini"
  else
    echo "full"
  fi
}

# ============== 子命令 ==============

cmd_up() {
  local mode="${1:-auto}"
  if [ "$mode" = "auto" ]; then
    mode=$(auto_detect_mode)
    bold ""
    bold "🤖 自动检测模式: $mode (内存 $(detect_memory_gb)GB)"
  fi

  bold ""
  bold "🚀 启动 MiniMax Platform (模式: $mode)"
  bold ""

  case "$mode" in
    mini|full)
      if [ ! -f "deploy-mini.sh" ]; then
        red "❌ deploy-mini.sh 不存在"
        exit 1
      fi
      if [ "$mode" = "full" ]; then
        yellow "📦 完整版 (21 容器) 需要 8GB+ 内存, 切换到完整 docker-compose.yml"
        cp docker-compose.mini.yml /tmp/.bak-mini.yml
        # 完整版用 deploy.sh
        if [ -f deploy.sh ]; then
          bash deploy.sh up
        else
          red "❌ deploy.sh 不存在, 完整版暂未提供独立脚本"
          bash deploy-mini.sh up
        fi
      else
        bash deploy-mini.sh up
      fi
      ;;
    host)
      if [ ! -f "deploy-host.sh" ]; then
        red "❌ deploy-host.sh 不存在"
        exit 1
      fi
      bash deploy-host.sh install
      ;;
    h2)
      yellow "🧪 H2 沙箱模式 (单 jar, 适合开发调试)"
      yellow "   使用: ./start.sh h2"
      bash start.sh h2
      ;;
    *)
      red "❌ 未知模式: $mode"
      echo "可选: auto / mini / full / host / h2"
      exit 1
      ;;
  esac
}

cmd_down() {
  bold ""
  bold "⏹  停止 MiniMax Platform"
  bold ""

  # 同时尝试所有模式
  if [ -f "deploy-mini.sh" ]; then
    docker compose -f docker-compose.mini.yml -p minimax-mini down 2>/dev/null || true
  fi
  if [ -f "deploy.sh" ]; then
    bash deploy.sh down 2>/dev/null || true
  fi
  if [ -f "deploy-host.sh" ]; then
    bash deploy-host.sh stop 2>/dev/null || true
  fi
  if [ -f "start.sh" ]; then
    bash start.sh stop 2>/dev/null || true
  fi
  green "✅ 已停止"
}

cmd_status() {
  bold ""
  bold "📊 MiniMax Platform 状态总览"
  bold ""

  if [ -f "deploy-mini.sh" ]; then
    echo "🐳 [Docker mini]"
    docker compose -f docker-compose.mini.yml -p minimax-mini ps 2>/dev/null || echo "   未运行"
    echo ""
  fi
  if [ -f "deploy.sh" ]; then
    echo "🐳 [Docker full]"
    docker compose -f docker-compose.yml -p minimax-platform ps 2>/dev/null || echo "   未运行"
    echo ""
  fi
  if [ -f "start.sh" ]; then
    echo "🧪 [H2 沙箱 / 宿主机]"
    bash start.sh status 2>/dev/null || true
  fi

  echo ""
  bold "🌐 端口监听:"
  ss -ltn 2>/dev/null | grep -E ":80|:3306|:6379|:7080|:8081|:8094|:8090" | head -10
}

cmd_logs() {
  local mode="${1:-mini}"
  case "$mode" in
    mini|full)
      docker compose -f docker-compose.mini.yml -p minimax-mini logs -f --tail=100
      ;;
    h2)
      tail -f /tmp/minimax-ai.log 2>/dev/null || tail -f /var/log/minimax/ai.log 2>/dev/null || \
        echo "H2 日志在 backend/minimax-ai/console"
      ;;
    *)
      red "用法: $0 logs {mini|full|h2}"
      exit 1
      ;;
  esac
}

cmd_restart() {
  bold ""
  bold "🔄 重启服务"
  bold ""

  if [ -f "deploy-mini.sh" ]; then
    docker compose -f docker-compose.mini.yml -p minimax-mini restart 2>/dev/null || true
  fi
  green "✅ 已重启"
}

cmd_clean() {
  yellow ""
  yellow "⚠️  即将清理所有数据 (不可恢复!)"
  yellow ""
  read -p "确认清理? (输入 yes 继续): " confirm
  if [ "$confirm" = "yes" ]; then
    if [ -f "deploy-mini.sh" ]; then
      docker compose -f docker-compose.mini.yml -p minimax-mini down -v 2>/dev/null || true
    fi
    if [ -f "deploy.sh" ]; then
      docker compose -f docker-compose.yml -p minimax-platform down -v 2>/dev/null || true
    fi
    green "✅ 已清理"
  else
    yellow "❌ 取消"
  fi
}

cmd_help() {
  bold ""
  bold "================================================="
  bold "  MiniMax Platform 一键部署 (V3.5.5+)"
  bold "  前端 Vue3 SPA + 后端 16 微服务 + nginx 代理"
  bold "================================================="
  echo ""
  green "用法: $0 <命令> [选项]"
  echo ""
  echo "核心命令:"
  echo "  up [模式]   一键启动 (auto/mini/full/host/h2)"
  echo "  down        停止所有"
  echo "  status      查看状态"
  echo "  logs [模式] 实时日志 (mini/full/h2)"
  echo "  restart     重启"
  echo "  clean       清理 (危险)"
  echo "  help        帮助"
  echo ""
  echo "部署模式:"
  green "  auto (默认)  根据内存自动选:"
  echo "              < 2GB → h2 (H2 沙箱, 512MB)"
  echo "              2-4GB → mini (Docker 6 容器, 1.5GB)"
  echo "              4GB+  → full (Docker 21 容器, 8GB)"
  green "  mini        Docker 6 容器 (推荐 VPS 1-2GB)"
  green "  full        Docker 21 容器 (推荐生产 8GB+)"
  green "  host        宿主机 systemd/nohup (无 Docker)"
  green "  h2          H2 沙箱单服务 (开发调试)"
  echo ""
  echo "部署后访问 (单端口 80 nginx 代理):"
  green "  http://<server-ip>/            # 前端 SPA (Vue3)"
  green "  http://<server-ip>/healthz     # 健康检查"
  green "  http://<server-ip>/#/login     # 登录页"
  green "  http://<server-ip>/#/dashboard # 控制台"
  echo ""
  echo "快速开始:"
  green "  ./deploy-all.sh up      # 一键部署 (自动选模式)"
  green "  ./deploy-all.sh up mini # 强制精简模式"
  green "  ./deploy-all.sh status  # 查看状态"
  green "  ./deploy-all.sh logs    # 实时日志"
  echo ""
}

case "${1:-help}" in
  up)        cmd_up "${2:-auto}" ;;
  down)      cmd_down ;;
  status)    cmd_status ;;
  logs)      cmd_logs "${2:-mini}" ;;
  restart)   cmd_restart ;;
  clean)     cmd_clean ;;
  help|-h|--help) cmd_help ;;
  *)         red "未知命令: $1"; cmd_help; exit 1 ;;
esac
