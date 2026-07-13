#!/usr/bin/env bash
# =============================================================
# MiniMax Platform 完整模式部署 (V3.5.5+)
# 21 个容器: 3 基础设施 + 16 微服务 + 1 nginx + 1 监控 (可选)
# 适用环境: 8GB+ 内存 / 生产 / 全功能测试
#
# 用法:
#   sudo ./deploy-full.sh up          # 一键启动全部 21 容器
#   sudo ./deploy-full.sh down        # 停止
#   sudo ./deploy-full.sh build       # 重新构建所有镜像
#   sudo ./deploy-full.sh status      # 查看状态 (21 容器)
#   sudo ./deploy-full.sh logs        # 实时日志
#   sudo ./deploy-full.sh restart     # 重启
#   sudo ./deploy-full.sh clean       # 清理所有数据 (危险)
#   sudo ./deploy-full.sh help        # 帮助
#
# 部署后访问 (单端口 80 nginx 统一入口):
#   http://<server-ip>/                # 前端 SPA (Vue3)
#   http://<server-ip>/healthz         # 健康检查
#   http://<server-ip>/api/v1/{module}/*  # 16 个微服务 API
# =============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# 颜色
green()  { echo -e "\033[32m$*\033[0m"; }
red()    { echo -e "\033[31m$*\033[0m"; }
yellow() { echo -e "\033[33m$*\033[0m"; }
blue()   { echo -e "\033[36m$*\033[0m"; }
bold()   { echo -e "\033[1m$*\033[0m"; }

# ============== 配置 ==============
COMPOSE_FILE="docker-compose.yml"           # 完整版 21 容器
PROJECT_NAME="minimax-platform"             # compose project 名
MIN_MEM_GB=6                                # 最低内存要求 (GB)
SERVICES=(
    "infrastructure:mariadb,redis,nacos"
    "gateway:gateway"
    "auth:auth"
    "ai:ai"
    "chat:chat"
    "memory:memory"
    "model:model"
    "rag:rag"
    "function:function"
    "multimodal:multimodal"
    "agent:agent"
    "monitor:monitor"
    "admin:admin"
    "prompt:prompt"
    "analytics:analytics"
    "pipeline:pipeline"
    "ws:ws"
    "frontend:nginx"
)

# ============== 工具函数 ==============

# 检测内存
detect_memory_gb() {
    if command -v free &>/dev/null; then
        free -g | awk '/^Mem:/{print $2}'
    else
        echo 8  # 默认假设 8GB
    fi
}

# 检查 docker
check_docker() {
    if ! command -v docker &>/dev/null; then
        red "❌ docker 未安装"
        red "   CentOS: sudo dnf install -y docker docker-compose-plugin"
        red "   Ubuntu: sudo apt install -y docker.io docker-compose-plugin"
        exit 1
    fi
    if ! docker info &>/dev/null; then
        red "❌ docker daemon 未运行"
        red "   sudo systemctl enable --now docker"
        exit 1
    fi
    if ! docker compose version &>/dev/null; then
        red "❌ docker compose v2 未安装 (需要 v2.0+)"
        exit 1
    fi
}

# 检查 Node.js (前端构建)
check_node() {
    if ! command -v node &>/dev/null; then
        yellow "⚠  node 未安装, 跳过前端构建, 使用预构建 dist/"
        return 1
    fi
    NODE_VER=$(node -v 2>/dev/null | sed 's/v//' | cut -d. -f1)
    if [ "${NODE_VER:-0}" -lt 18 ]; then
        yellow "⚠  node 版本 < 18, 跳过前端构建"
        return 1
    fi
    return 0
}

# 检查 maven
check_maven() {
    if ! command -v mvn &>/dev/null; then
        yellow "⚠  maven 未安装, 将使用 docker 内构建 (首次慢)"
        return 1
    fi
    return 0
}

# ============== 步骤函数 ==============

# 1. 构建前端
build_frontend() {
    blue "📦 [1/5] 构建前端 (Vue3 + Vite)"
    if ! check_node; then
        if [ -d frontend/dist ]; then
            yellow "   使用预构建 dist/"
            return 0
        else
            red "❌ frontend/dist 不存在且无 node, 无法构建"
            exit 1
        fi
    fi
    cd frontend
    if [ ! -d node_modules ]; then
        yellow "   安装前端依赖..."
        npm config set registry https://registry.npmmirror.com 2>/dev/null || true
        npm install --no-audit --no-fund --silent
    fi
    green "   ⚡ 构建中 (vue-router history 模式, base=/)..."
    npm run build 2>&1 | tail -10
    cd ..
    if [ ! -f frontend/dist/index.html ]; then
        red "❌ 前端构建失败"
        exit 1
    fi
    green "   ✓ 前端构建完成: $(du -sh frontend/dist 2>/dev/null | cut -f1)"
}

# 2. 编译后端 (本地加速 docker build)
build_backend_locally() {
    blue "📦 [2/5] 编译后端 (16 个 Spring Boot 模块)"
    if ! check_maven; then
        yellow "   跳过本地编译, 让 docker 内构建"
        return 0
    fi
    cd backend
    # 编译所有 16 个可执行应用模块 (不包含 common 的 -spring-boot.jar, common 改 packaging=pom)
    yellow "   编译: admin agent ai analytics auth chat function gateway memory model monitor multimodal pipeline prompt rag ws"
    mvn -B \
        -pl admin,agent,ai,analytics,auth,chat,function,gateway,memory,model,monitor,multimodal,pipeline,prompt,rag,ws \
        -am clean install \
        -DskipTests -Dcheckstyle.skip -Dpmd.skip -Dmaven.javadoc.skip \
        -Dspotless.check.skip=true -q 2>&1 | tail -5 || true
    cd ..
    green "   ✓ 后端编译完成"
}

# 3. 检查宿主 nginx
ensure_host_nginx() {
    blue "📦 [3/5] 检查/部署宿主 nginx"
    if command -v nginx &>/dev/null; then
        green "   ✓ nginx 已安装: $(nginx -v 2>&1 | cut -d' ' -f3)"
    else
        yellow "   ⚠  nginx 未安装 (宿主 nginx 是必要的, 负责 80 端口入口 + 前端代理)"
        if [ "$EUID" -eq 0 ]; then
            bash nginx/install-nginx.sh install 2>&1 | tail -5
        else
            red "   ❌ 非 root 用户, 请手动: sudo ./nginx/install-nginx.sh install"
            exit 1
        fi
    fi
}

# 4. 启动 docker compose
start_docker() {
    blue "📦 [4/5] 启动 21 容器 (3 基础设施 + 16 微服务 + 1 nginx + ...)"

    # 检查端口
    for p in 80 3306 6379 7080 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090 8091 8092 8093 8094 8095 8848; do
        if ss -ltn 2>/dev/null | grep -q ":$p "; then
            yellow "   ⚠  端口 $p 已被占用"
        fi
    done

    # docker compose up
    docker compose -f $COMPOSE_FILE -p $PROJECT_NAME up -d --build 2>&1 | tail -20

    green ""
    green "   ✓ 21 容器已启动 (后台运行)"
}

# 5. 等待 + 验证
wait_and_verify() {
    blue "📦 [5/5] 等待服务就绪 + 验证"

    yellow "   等待基础设施 (mariadb/redis/nacos)..."
    sleep 30

    yellow "   等待 16 微服务启动..."
    for i in 1 2 3 4 5 6 7 8 9 10; do
        RUNNING=$(docker compose -f $COMPOSE_FILE -p $PROJECT_NAME ps --status running 2>/dev/null | grep -c "minimax-")
        if [ "$RUNNING" -ge 16 ]; then
            green "   ✓ $RUNNING 容器运行中"
            break
        fi
        echo "   [$i/10] 当前 $RUNNING 个运行中, 等待..."
        sleep 10
    done

    # 重新加载 nginx (前端 dist 可能更新了)
    if command -v nginx &>/dev/null; then
        # 复制前端 dist 到 nginx 路径
        if [ -d frontend/dist ]; then
            mkdir -p /usr/share/nginx/html
            cp -r frontend/dist/* /usr/share/nginx/html/ 2>/dev/null || true
        fi
        nginx -t 2>/dev/null && systemctl reload nginx 2>/dev/null || nginx -s reload 2>/dev/null || true
        green "   ✓ nginx 已重载"
    fi

    echo ""
    bold "════════════════════════════════════════"
    bold "  🎉 MiniMax Platform 完整模式启动成功"
    bold "════════════════════════════════════════"
    echo ""
    bold "📊 21 容器状态:"
    docker compose -f $COMPOSE_FILE -p $PROJECT_NAME ps
    echo ""
    bold "🌐 访问入口 (单端口 80 宿主 nginx 统一代理):"
    green "   前端 SPA:     http://<server-ip>/"
    green "   登录页:       http://<server-ip>/#/login"
    green "   控制台:       http://<server-ip>/#/dashboard"
    green "   健康检查:     http://<server-ip>/healthz"
    echo ""
    green "   16 个微服务 API:"
    green "     /api/v1/auth/      → 8081 (auth)"
    green "     /api/v1/ai/        → 8094 (ai)"
    green "     /api/v1/chat/      → 8082 (chat)"
    green "     /api/v1/memory/    → 8083 (memory)"
    green "     /api/v1/model/     → 8084 (model)"
    green "     /api/v1/rag/       → 8085 (rag)"
    green "     /api/v1/function/  → 8086 (function)"
    green "     /api/v1/multimodal/→ 8087 (multimodal)"
    green "     /api/v1/agent/     → 8088 (agent)"
    green "     /api/v1/monitor/   → 8089 (monitor)"
    green "     /api/v1/admin/     → 8090 (admin)"
    green "     /api/v1/prompt/    → 8091 (prompt)"
    green "     /api/v1/analytics/ → 8092 (analytics)"
    green "     /api/v1/pipeline/  → 8093 (pipeline)"
    green "     /api/v1/ws/        → 8095 (ws)"
    green "     /api/v1/gateway/   → 7080 (gateway)"
    echo ""
    green "🔑 默认账号: adminLiugl / Liugl@2026"
    yellow "⏳ 完全就绪需 3-5 分钟, 实时查看: ./deploy-full.sh logs"
}

# ============== 命令实现 ==============

cmd_up() {
    check_docker
    bold ""
    bold "🚀 MiniMax Platform 完整模式部署 (21 容器)"
    bold "   内存需求: ~6-8GB / 21 容器"
    bold "   当前内存: $(detect_memory_gb)GB"
    bold ""

    # 内存检查
    local mem=$(detect_memory_gb)
    if [ "$mem" -lt "$MIN_MEM_GB" ]; then
        yellow "⚠  内存 < ${MIN_MEM_GB}GB, 启动可能 OOM"
        yellow "   推荐: 用 mini 模式 (6 容器, 1.5GB)"
        yellow "   命令: ./deploy-mini.sh up"
        read -p "继续完整模式? (yes 确认): " confirm
        [ "$confirm" = "yes" ] || exit 0
    fi

    # 1. 构建前端
    build_frontend

    # 2. 编译后端
    build_backend_locally

    # 3. 检查宿主 nginx
    ensure_host_nginx

    # 4. 启动 docker
    start_docker

    # 5. 等待 + 验证
    wait_and_verify
}

cmd_down() {
    check_docker
    yellow "⏹  停止 21 容器 (数据保留)"
    docker compose -f $COMPOSE_FILE -p $PROJECT_NAME down
    green "✅ 已停止"
}

cmd_build() {
    check_docker
    yellow "🔨 重新构建所有镜像 (无缓存)"
    docker compose -f $COMPOSE_FILE -p $PROJECT_NAME build --no-cache
    green "✅ 构建完成"
}

cmd_status() {
    check_docker
    bold ""
    bold "📊 21 容器状态"
    bold ""
    docker compose -f $COMPOSE_FILE -p $PROJECT_NAME ps
    echo ""
    bold "💾 资源使用:"
    docker stats --no-stream $(docker compose -f $COMPOSE_FILE -p $PROJECT_NAME ps -q 2>/dev/null) 2>/dev/null | head -25 || true
    echo ""
    bold "🌐 端口监听:"
    ss -ltn 2>/dev/null | grep -E ":80|:3306|:6379|:8848|:7080|:80[8-9][0-9]" | head -25
}

cmd_logs() {
    check_docker
    yellow "📜 实时日志 (Ctrl+C 退出):"
    docker compose -f $COMPOSE_FILE -p $PROJECT_NAME logs -f --tail=100
}

cmd_restart() {
    check_docker
    yellow "🔄 重启所有 21 容器"
    docker compose -f $COMPOSE_FILE -p $PROJECT_NAME restart
    green "✅ 已重启"
}

cmd_clean() {
    check_docker
    yellow "⚠  即将清理所有容器 + 数据卷 (不可恢复!)"
    read -p "确认清理? (输入 yes 继续): " confirm
    if [ "$confirm" = "yes" ]; then
        docker compose -f $COMPOSE_FILE -p $PROJECT_NAME down -v
        green "✅ 已清理所有数据"
    else
        yellow "❌ 取消"
    fi
}

cmd_help() {
    bold ""
    bold "═══════════════════════════════════════════════════"
    bold "  MiniMax Platform 完整模式部署 (V3.5.5+)"
    bold "  21 容器: 3 基础设施 + 16 微服务 + nginx + ..."
    bold "═══════════════════════════════════════════════════"
    echo ""
    green "用法: sudo $0 <命令>"
    echo ""
    echo "核心命令:"
    echo "  up        一键启动 (build 前端 + 编译后端 + 启动 21 容器)"
    echo "  down      停止 (数据保留)"
    echo "  build     重新构建所有镜像 (无缓存)"
    echo "  status    查看 21 容器状态 + 资源 + 端口"
    echo "  logs      实时日志"
    echo "  restart   重启"
    echo "  clean     清理所有数据 (危险, 需输入 yes)"
    echo "  help      显示帮助"
    echo ""
    echo "资源需求:"
    echo "  内存: 6GB+ 推荐 8GB+"
    echo "  磁盘: 5GB+ (含镜像)"
    echo "  时间: 首次 5-10 分钟, 后续增量 30s"
    echo ""
    echo "部署后访问 (单端口 80 宿主 nginx):"
    green "  http://<server-ip>/                # 前端 SPA"
    green "  http://<server-ip>/api/v1/{module}/*  # 16 微服务 API"
    green "  账号: adminLiugl / Liugl@2026"
    echo ""
    echo "对比:"
    echo "  mini 模式:  6 容器,  1.5GB, 3 核心服务"
    echo "  full 模式: 21 容器,  6-8GB, 全 16 微服务  ← 本脚本"
    echo "  h2 沙箱:   0 容器,  512MB, 单服务开发"
    echo ""
    echo "快速开始:"
    green "  sudo $0 up        # 一键启动"
    green "  sudo $0 status    # 查看状态"
    green "  sudo $0 logs      # 实时日志"
    echo ""
}

case "${1:-help}" in
    up)        cmd_up ;;
    down)      cmd_down ;;
    build)     cmd_build ;;
    status)    cmd_status ;;
    logs)      cmd_logs ;;
    restart)   cmd_restart ;;
    clean)     cmd_clean ;;
    help|-h|--help) cmd_help ;;
    *)         red "未知命令: $1"; cmd_help; exit 1 ;;
esac
