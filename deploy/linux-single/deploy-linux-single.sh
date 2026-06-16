#!/bin/bash
# ============================================================
#  MiniMax Platform - Linux 单机部署脚本
#  适用: Ubuntu 20.04+ / CentOS 7+ / Debian 11+
#  作者: Mavis  日期: 2026-06-16
# ============================================================
set -e

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
RUNTIME_DIR="$PROJECT_ROOT/runtime"
LOGS_DIR="$RUNTIME_DIR/logs"

# 模式选择
MODE="${1:-docker}"  # docker | jar | infra

echo -e "${BLUE}============================================================${NC}"
echo -e "${BLUE}  MiniMax Platform Linux 单机部署${NC}"
echo -e "${BLUE}  模式: $MODE${NC}"
echo -e "${BLUE}============================================================${NC}"
echo

# ============================================================
# 0) 前置检查
# ============================================================
echo -e "${YELLOW}[0/8] 检查前置环境...${NC}"

check_command() {
    if ! command -v "$1" &> /dev/null; then
        echo -e "${RED}✗ $1 未安装${NC}"
        return 1
    fi
    echo -e "${GREEN}✓ $1${NC}"
}

check_command docker
check_command java || true
check_command mvn || true

if ! docker info &> /dev/null; then
    echo -e "${RED}✗ Docker 未运行${NC}"
    echo "  启动: sudo systemctl start docker"
    exit 1
fi

echo

# ============================================================
# 1) Docker Compose 全栈
# ============================================================
if [ "$MODE" = "docker" ] || [ "$MODE" = "all" ]; then
    echo -e "${YELLOW}[1/8] Docker Compose 全栈部署${NC}"

    cd "$PROJECT_ROOT"

    # 2) .env
    if [ ! -f ".env" ]; then
        echo -e "${YELLOW}[2/8] 生成 .env 配置文件...${NC}"
        cat > .env <<'EOF'
# MiniMax Platform 环境变量
COMPOSE_PROJECT_NAME=minimax

# MySQL
MYSQL_ROOT_PASSWORD=MinMax2026!
MYSQL_DATABASE=minimax
MYSQL_USER=minimax
MYSQL_PASSWORD=MinMax2026!

# Redis
REDIS_PASSWORD=MinMax2026!

# JWT
JWT_SECRET=please-change-this-to-a-32-byte-random-secret-key-min-max

# 模型 API Key (留空 = Mock 模式)
OPENAI_API_KEY=
MINIMAX_API_KEY=
EMBEDDING_API_KEY=
EMBEDDING_BASE_URL=
EOF
        echo -e "${GREEN}✓ .env 已生成${NC}"
    else
        echo -e "${YELLOW}[2/8] .env 已存在, 跳过${NC}"
    fi
    echo

    # 3) Build 镜像
    echo -e "${YELLOW}[3/8] 构建/拉取 Docker 镜像 (首次 5-10 min)...${NC}"
    docker compose --profile app build --parallel 2>/dev/null || {
        echo -e "${YELLOW}  镜像可能已存在, 跳过 build${NC}"
    }
    echo

    # 4) 启动基础设施
    echo -e "${YELLOW}[4/8] 启动 MySQL + Redis...${NC}"
    docker compose up -d mysql redis

    echo "等待 MySQL 就绪..."
    until docker exec minimax-mysql mysqladmin ping -h localhost -uroot -pMinMax2026! &> /dev/null; do
        sleep 3
    done
    echo -e "${GREEN}✓ MySQL 就绪${NC}"
    echo

    # 5) 初始化数据库
    echo -e "${YELLOW}[5/8] 初始化数据库 (建表)...${NC}"
    for sql_file in "$PROJECT_ROOT/sql/init/"*.sql; do
        if [ -f "$sql_file" ]; then
            echo "  执行 $(basename $sql_file) ..."
            docker exec -i minimax-mysql mysql -uroot -pMinMax2026! minimax < "$sql_file" \
                && echo -e "    ${GREEN}✓${NC}" \
                || echo -e "    ${YELLOW}! 失败, 继续${NC}"
        fi
    done
    echo

    # 6) 启动 ES + MinIO
    echo -e "${YELLOW}[6/8] 启动 ES + MinIO...${NC}"
    read -p "是否启动 Elasticsearch? [y/N]: " START_ES
    if [[ "$START_ES" =~ ^[Yy]$ ]]; then
        docker compose up -d elasticsearch
        echo "等待 ES 就绪 (可能 60s)..."
        until docker exec minimax-elasticsearch curl -sf http://localhost:9200 &> /dev/null; do
            sleep 5
        done
        echo -e "${GREEN}✓ ES 就绪${NC}"
    fi

    read -p "是否启动 MinIO? [y/N]: " START_MINIO
    if [[ "$START_MINIO" =~ ^[Yy]$ ]]; then
        docker compose up -d minio
    fi
    echo

    # 7) 启动应用
    echo -e "${YELLOW}[7/8] 启动应用服务...${NC}"
    docker compose --profile app up -d
    echo "等待应用就绪..."
    sleep 15
    echo

    # 8) 验证
    echo -e "${YELLOW}[8/8] 验证部署...${NC}"
    echo
    echo "服务状态:"
    docker compose ps
    echo
    echo "健康检查:"
    echo "  Gateway:   http://localhost:8080/actuator/health"
    echo "  Auth:      http://localhost:8081/api/v1/auth/health"
    echo "  Chat:      http://localhost:8082/api/v1/sessions"
    echo "  Model:     http://localhost:8083/api/v1/models"
    echo "  Memory:    http://localhost:8084/api/v1/memory/short-term/1/size"
    echo "  RAG:       http://localhost:8085/api/v1/rag/kb/public"
    echo
    echo -e "${GREEN}============================================================${NC}"
    echo -e "${GREEN}  部署完成！${NC}"
    echo -e "${GREEN}============================================================${NC}"
    echo
    echo "默认账号: admin / admin@123"
    echo
    echo "常用命令:"
    echo "  查看日志:    docker compose logs -f [service]"
    echo "  停止服务:    docker compose down"
    echo "  重启服务:    docker compose restart [service]"
    echo
    exit 0
fi

# ============================================================
# 2) 本地 jar 模式
# ============================================================
if [ "$MODE" = "jar" ]; then
    echo -e "${YELLOW}[1/8] 本地 jar 部署模式${NC}"

    if ! command -v java &> /dev/null; then
        echo -e "${RED}✗ 需要 JDK 17${NC}"
        echo "  Ubuntu/Debian: sudo apt install -y openjdk-17-jdk"
        echo "  CentOS/RHEL:   sudo yum install -y java-17-openjdk"
        exit 1
    fi

    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}✗ 需要 Maven 3.8+${NC}"
        echo "  Ubuntu/Debian: sudo apt install -y maven"
        exit 1
    fi

    java -version 2>&1 | head -1

    # 启动基础设施
    echo -e "${YELLOW}[2/8] 启动 MySQL + Redis...${NC}"
    cd "$PROJECT_ROOT"
    docker compose up -d mysql redis

    echo "等待 MySQL..."
    until docker exec minimax-mysql mysqladmin ping -h localhost -uroot -pMinMax2026! &> /dev/null; do
        sleep 3
    done

    echo -e "${YELLOW}[3/8] 初始化数据库...${NC}"
    for sql_file in "$PROJECT_ROOT/sql/init/"*.sql; do
        [ -f "$sql_file" ] && \
            docker exec -i minimax-mysql mysql -uroot -pMinMax2026! minimax < "$sql_file" 2>/dev/null || true
    done
    echo -e "${GREEN}✓ 数据库就绪${NC}"
    echo

    # 构建
    echo -e "${YELLOW}[4/8] Maven 构建...${NC}"
    cd "$PROJECT_ROOT/backend"
    mvn -B -DskipTests clean package
    echo -e "${GREEN}✓ 构建完成${NC}"
    echo

    # 创建运行目录
    mkdir -p "$LOGS_DIR"
    echo -e "${YELLOW}[5/8] 准备运行目录: $RUNTIME_DIR${NC}"
    echo

    # 启动服务 (test profile, H2 内存库 - 无需 MySQL)
    echo -e "${YELLOW}[6/8] 启动服务 (test profile, H2 内存)...${NC}"

    start_service() {
        local name=$1
        local jar=$2
        local port=$3
        local heap=$4

        nohup java -Xms256m -Xmx${heap} \
            -jar "$PROJECT_ROOT/backend/$jar/target/$jar.jar" \
            --spring.profiles.active=test \
            --server.port=$port \
            > "$LOGS_DIR/$name.log" 2>&1 &
        echo "  $name started on :$port (pid=$!)"
    }

    start_service "auth"    "minimax-auth"    8081 512m
    sleep 2
    start_service "model"   "minimax-model"   8083 512m
    sleep 2
    start_service "chat"    "minimax-chat"    8082 768m
    sleep 2
    start_service "memory"  "minimax-memory"  8084 768m
    sleep 2
    start_service "rag"     "minimax-rag"     8085 768m
    sleep 2
    start_service "gateway" "minimax-gateway" 8080 512m

    echo
    echo -e "${YELLOW}[7/8] 等待服务启动 (20s)...${NC}"
    sleep 20

    # 验证
    echo -e "${YELLOW}[8/8] 验证部署...${NC}"
    echo
    echo "Auth 健康检查:"
    curl -s -m 3 http://localhost:8081/api/v1/auth/health || echo "失败"
    echo
    echo
    echo -e "${GREEN}============================================================${NC}"
    echo -e "${GREEN}  部署完成！${NC}"
    echo -e "${GREEN}============================================================${NC}"
    echo
    echo "进程:"
    pgrep -af "minimax-" | head -10
    echo
    echo "日志: $LOGS_DIR/"
    echo "默认账号: admin / admin@123"
    echo
    echo "停止: pkill -f 'minimax-'"
    echo
    exit 0
fi

# ============================================================
# 3) 仅基础设施
# ============================================================
if [ "$MODE" = "infra" ]; then
    echo -e "${YELLOW}[1/8] 启动基础设施 (MySQL + Redis + ES + MinIO)${NC}"
    cd "$PROJECT_ROOT"
    docker compose up -d mysql redis elasticsearch minio
    sleep 10

    echo -e "${YELLOW}[2/8] 初始化数据库...${NC}"
    for sql_file in "$PROJECT_ROOT/sql/init/"*.sql; do
        [ -f "$sql_file" ] && \
            docker exec -i minimax-mysql mysql -uroot -pMinMax2026! minimax < "$sql_file" 2>/dev/null || true
    done

    echo -e "${GREEN}✓ 基础设施已启动${NC}"
    echo
    echo "连接信息:"
    echo "  MySQL:   localhost:3306  user=minimax  pwd=MinMax2026!"
    echo "  Redis:   localhost:6379  pwd=MinMax2026!"
    echo "  ES:      http://localhost:9200"
    echo "  MinIO:   http://localhost:9000  console: http://localhost:9001"
    exit 0
fi

echo -e "${RED}未知模式: $MODE${NC}"
echo "用法: $0 [docker|jar|infra]"
exit 1
