#!/usr/bin/env bash
# =============================================================
# MiniMax Platform V5.26 - CentOS 中间件安装脚本
#
# 目标系统: CentOS 7+ / Rocky Linux 8+ / RHEL 8+ / Anolis OS 8+
#
# 安装内容:
#   - Docker 20.10+ (含 docker compose plugin)
#   - MariaDB 10.5 (via Docker, 数据持久化)
#   - Redis 7.2 (via Docker, 密码认证)
#   - Nacos 2.3.2 (via Docker, 独立模式)
#   - Adminer 4.8 (via Docker, DB Web GUI)
#
# 默认端口:
#   3306  MariaDB    (绑定 127.0.0.1)
#   6379  Redis      (绑定 127.0.0.1)
#   8848  Nacos      (绑定 0.0.0.0)
#   8082  Adminer    (绑定 0.0.0.0, 用于外部访问)
#
# 用法:
#   sudo ./scripts/install-middleware-centos.sh install     安装全部中间件
#   sudo ./scripts/install-middleware-centos.sh start       启动
#   sudo ./scripts/install-middleware-centos.sh stop        停止
#   sudo ./scripts/install-middleware-centos.sh status      状态
#   sudo ./scripts/install-middleware-centos.sh restart     重启
#   sudo ./scripts/install-middleware-centos.sh uninstall   卸载 (保留数据)
#
# 环境变量 (可选):
#   DB_ROOT_PASS     MariaDB root 密码 (默认: minimax_root_2024)
#   DB_USER          MariaDB 用户   (默认: minimax)
#   DB_PASS          MariaDB 密码   (默认: minimax_pass_2024)
#   REDIS_PASS       Redis 密码     (默认: minimax_redis_2024)
#   INSTALL_DIR      安装目录       (默认: /opt/minimax)
#   SQL_FILE         SQL 文件路径   (默认: ./sql/init-minimax.sql)
# =============================================================

set -euo pipefail

# =============== 颜色 ===============
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0;36m'; NC='\033[0m'
log_info()  { echo -e "${GREEN}[✓]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[!]${NC}  $*"; }
log_err()   { echo -e "${RED}[✗]${NC} $*"; }
log_step()  { echo -e "\n${BLUE}═══${NC} ${CYAN}$*${NC} ${BLUE}═══${NC}"; }
log_fatal() { log_err "$*"; exit 1; }

# =============== 配置 ===============
SRC_DIR="$(cd "$(dirname "$0")/.." && pwd)"
INSTALL_DIR="${INSTALL_DIR:-/opt/minimax}"
DATA_DIR="${INSTALL_DIR}/data"
LOG_DIR="/var/log/minimax"
COMPOSE_FILE="${INSTALL_DIR}/docker-compose.middleware.yml"

DB_NAME="minimax_platform"
DB_USER="${DB_USER:-minimax}"
DB_PASS="${DB_PASS:-minimax_pass_2024}"
DB_ROOT_PASS="${DB_ROOT_PASS:-minimax_root_2024}"
REDIS_PASS="${REDIS_PASS:-minimax_redis_2024}"
SQL_FILE="${SQL_FILE:-${SRC_DIR}/sql/init-minimax.sql}"

# =============== 工具函数 ===============
is_root() { [[ $EUID -eq 0 ]] || log_fatal "需要 root, 请用 sudo $0"; }

cmd_exists() { command -v "$1" >/dev/null 2>&1; }

detect_os() {
  if [[ -f /etc/redhat-release ]]; then
    OS_ID=$(cat /etc/redhat-release | grep -oE '(CentOS|Rocky|AlmaLinux|Red Hat|Anolis|Oracle)' | head -1 | tr 'A-Z' 'a-z' | tr -d ' ')
    [[ -z "$OS_ID" ]] && OS_ID="centos"
    OS_VER=$(cat /etc/redhat-release | grep -oE '[0-9]+' | head -1)
  else
    log_fatal "此脚本仅支持 CentOS/RHEL/Rocky/Alma 系统"
  fi
  log_info "OS: $OS_ID $OS_VER"
}

# =============== 写 docker-compose 文件 ===============
generate_compose() {
  log_info "生成 docker-compose: $COMPOSE_FILE"

  mkdir -p "$INSTALL_DIR" "$DATA_DIR/mariadb" "$DATA_DIR/redis" "$DATA_DIR/nacos" "$DATA_DIR/adminer"
  mkdir -p "$LOG_DIR"

  cat > "$COMPOSE_FILE" <<EOF
# =============================================================
# MiniMax 中间件 docker-compose (CentOS V5.26)
# 由 install-middleware-centos.sh 自动生成
# =============================================================
name: minimax-middleware

services:
  # ========== MariaDB 10.5 ==========
  mariadb:
    image: mariadb:10.5
    container_name: minimax-mariadb
    restart: unless-stopped
    environment:
      MARIADB_ROOT_PASSWORD: ${DB_ROOT_PASS}
      MARIADB_DATABASE: ${DB_NAME}
      MARIADB_USER: ${DB_USER}
      MARIADB_PASSWORD: ${DB_PASS}
      TZ: Asia/Shanghai
    ports:
      - "127.0.0.1:3306:3306"
    volumes:
      - ${DATA_DIR}/mariadb:/var/lib/mysql
      - ${SRC_DIR}/sql/init-minimax.sql:/docker-entrypoint-initdb.d/init-minimax.sql:ro
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --max_connections=512
      - --innodb_buffer_pool_size=256M
    healthcheck:
      test: ["CMD", "healthcheck.sh", "--connect", "--innodb_initialized"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s
    networks:
      - minimax-net
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"

  # ========== Redis 7.2 ==========
  redis:
    image: redis:7.2-alpine
    container_name: minimax-redis
    restart: unless-stopped
    command:
      - redis-server
      - --requirepass
      - "${REDIS_PASS}"
      - --appendonly
      - "yes"
      - --maxmemory
      - "256mb"
      - --maxmemory-policy
      - "allkeys-lru"
    ports:
      - "127.0.0.1:6379:6379"
    volumes:
      - ${DATA_DIR}/redis:/data
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASS}", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5
    networks:
      - minimax-net
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"

  # ========== Nacos 2.3.2 (单机模式) ==========
  nacos:
    image: nacos/nacos-server:v2.3.2
    container_name: minimax-nacos
    restart: unless-stopped
    environment:
      MODE: standalone
      JVM_XMS: 512m
      JVM_XMX: 512m
      JVM_XMN: 256m
      SPRING_DATASOURCE_PLATFORM: mysql
      NACOS_AUTH_ENABLE: "true"
      NACOS_AUTH_TOKEN: SecretKey012345678901234567890123456789012345678901234567890123456789
      NACOS_AUTH_IDENTITY_KEY: minimax
      NACOS_AUTH_IDENTITY_VALUE: minimax
      TZ: Asia/Shanghai
    ports:
      - "${NACOS_PORT:-8848}:8848"
      - "9848:9848"
    volumes:
      - ${DATA_DIR}/nacos:/home/nacos/data
    depends_on:
      mariadb:
        condition: service_healthy
    networks:
      - minimax-net
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"

  # ========== Adminer (DB Web GUI) ==========
  adminer:
    image: adminer:4.8.1
    container_name: minimax-adminer
    restart: unless-stopped
    environment:
      ADMINER_DEFAULT_SERVER: mariadb
      TZ: Asia/Shanghai
    ports:
      - "${ADMINER_PORT:-8082}:8080"
    depends_on:
      - mariadb
    networks:
      - minimax-net
    logging:
      driver: json-file
      options:
        max-size: "5m"
        max-file: "2"

networks:
  minimax-net:
    driver: bridge
EOF

  log_info "  ✓ 生成完成 (5 个服务)"
}

# =============== 步骤 1: 装 Docker ===============
install_docker() {
  log_step "步骤 1/4: 装 Docker"

  if cmd_exists docker && docker --version >/dev/null 2>&1; then
    log_info "  - Docker 已装: $(docker --version)"
  else
    log_info "  卸载旧版 (如有)..."
    yum remove -y docker docker-client docker-client-latest docker-common \
      docker-latest docker-engine docker-ce-cli docker-ce 2>/dev/null || true

    log_info "  装 yum-utils..."
    yum install -y yum-utils device-mapper-persistent-data lvm2

    log_info "  添加 Docker 仓库..."
    yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

    log_info "  装 docker-ce (约 1-2 分钟)..."
    yum install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

    log_info "  启动 docker..."
    systemctl enable --now docker

    # 等 docker ready
    for i in 1 2 3 4 5; do
      if systemctl is-active --quiet docker; then
        log_info "  ✓ Docker 启动成功 (${i}s)"
        break
      fi
      sleep 1
    done
  fi

  # 配置镜像加速 (国内常用)
  if [[ ! -f /etc/docker/daemon.json ]]; then
    log_info "  配置镜像加速..."
    mkdir -p /etc/docker
    cat > /etc/docker/daemon.json <<'EOF'
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com",
    "https://docker.mirrors.ustc.edu.cn"
  ],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m",
    "max-file": "3"
  },
  "storage-driver": "overlay2"
}
EOF
    systemctl restart docker
    sleep 3
    log_info "  ✓ 镜像加速配置完成"
  fi

  # 验证
  docker --version
  docker compose version
  log_info "  ✓ Docker + Compose 就绪"
}

# =============== 步骤 2: SELinux + 防火墙 ===============
configure_selinux_firewall() {
  log_step "步骤 2/4: SELinux + 防火墙配置"

  # SELinux: docker 通常需要 permissive
  if cmd_exists getenforce; then
    local current=$(getenforce)
    if [[ "$current" == "Enforcing" ]]; then
      log_info "  当前 SELinux: $current → permissive (避免容器挂载问题)"
      setenforce 0 || true
      sed -i 's/^SELINUX=enforcing/SELINUX=permissive/' /etc/selinux/config 2>/dev/null || true
    else
      log_info "  - SELinux: $current (无需调整)"
    fi
  fi

  # 防火墙: 开放必要端口
  if systemctl is-active --quiet firewalld 2>/dev/null; then
    log_info "  firewalld 活跃, 开放端口..."
    firewall-cmd --permanent --add-port=3306/tcp 2>/dev/null || true   # MariaDB
    firewall-cmd --permanent --add-port=6379/tcp 2>/dev/null || true   # Redis
    firewall-cmd --permanent --add-port=8848/tcp 2>/dev/null || true   # Nacos
    firewall-cmd --permanent --add-port=8082/tcp 2>/dev/null || true   # Adminer
    firewall-cmd --permanent --add-port=3000/tcp 2>/dev/null || true   # nginx
    firewall-cmd --reload
    log_info "  ✓ 端口 3306/6379/8848/8082/3000 已开放"
  else
    log_info "  - firewalld 未启用, 跳过"
  fi
}

# =============== 步骤 3: 启动中间件 ===============
start_middleware() {
  log_step "步骤 3/4: 启动中间件 (MariaDB + Redis + Nacos + Adminer)"

  cd "$INSTALL_DIR"
  docker compose -f "$COMPOSE_FILE" up -d

  # 等 MariaDB healthy
  log_info "  等 MariaDB 健康 (30-60s)..."
  local ready=0
  for i in $(seq 1 30); do
    sleep 2
    if docker exec minimax-mariadb healthcheck.sh --connect --innodb_initialized >/dev/null 2>&1; then
      log_info "  ✓ MariaDB 就绪 (${i}*2s)"
      ready=1
      break
    fi
  done
  if [[ $ready -eq 0 ]]; then
    log_warn "  MariaDB 健康检查超时, 但仍在启动..."
  fi

  # SQL 导入: 由 docker-entrypoint-initdb.d 自动执行 (容器第一次启动时)
  # 如果 init-minimax.sql 已被挂载, docker 会自动导入
  if [[ -f "$SQL_FILE" ]]; then
    log_info "  ✓ SQL 自动导入: $SQL_FILE (docker-entrypoint-initdb.d)"
    log_info "    MariaDB 第一次启动时自动执行 init-minimax.sql"
  fi

  # 等 Redis
  log_info "  等 Redis (5s)..."
  sleep 5
  if docker exec minimax-redis redis-cli -a "$REDIS_PASS" ping 2>/dev/null | grep -q PONG; then
    log_info "  ✓ Redis 就绪"
  fi

  # 等 Nacos
  log_info "  等 Nacos (30-60s)..."
  for i in $(seq 1 30); do
    sleep 2
    if curl -sf -m 2 "http://127.0.0.1:8848/nacos/" >/dev/null 2>&1; then
      log_info "  ✓ Nacos 就绪 (${i}*2s)"
      break
    fi
  done
}

# =============== 步骤 4: 验证 ===============
verify() {
  log_step "步骤 4/4: 验证"

  # 容器状态
  echo
  printf "%-30s %-15s %s\n" "SERVICE" "STATE" "PORT"
  printf "%-30s %-15s %s\n" "------" "-----" "----"

  for svc in mariadb redis nacos adminer; do
    if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "minimax-$svc"; then
      printf "  ${GREEN}%-28s${NC} ${GREEN}%-15s${NC} 127.0.0.1:%s\n" "minimax-$svc" "running" "${!svc_port:-?}"
    else
      printf "  ${RED}%-28s${NC} ${RED}%-15s${NC} -\n" "minimax-$svc" "stopped"
    fi
  done

  # HTTP 健康检查
  echo
  log_info "HTTP 健康检查:"
  for entry in \
    "MariaDB|mysql -h 127.0.0.1 -uroot -p${DB_ROOT_PASS} -e 'SELECT 1'" \
    "Redis|docker exec minimax-redis redis-cli -a ${REDIS_PASS} ping" \
    "Nacos|curl -sf http://127.0.0.1:8848/nacos/" \
    "Adminer|curl -sf http://127.0.0.1:8082/"; do
    local name="${entry%%|*}"
    local cmd="${entry##*|}"
    if eval "$cmd" >/dev/null 2>&1; then
      printf "  ${GREEN}✓ %-15s${NC} OK\n" "$name"
    else
      printf "  ${RED}✗ %-15s${NC} FAIL\n" "$name"
    fi
  done
}

# =============== 命令分发 ===============
ACTION="${1:-}"
shift || true

# 端口变量 (verify 用)
MARIADB_PORT=3306
REDIS_PORT=6379
NACOS_PORT=8848
ADMINER_PORT=8082

case "$ACTION" in
  install)
    is_root
    detect_os
    log_step "MiniMax V5.26 - CentOS 中间件安装"
    log_info "安装目录: $INSTALL_DIR"
    log_info "数据目录: $DATA_DIR"
    log_info "SQL 文件: $SQL_FILE"
    echo
    install_docker
    configure_selinux_firewall
    generate_compose
    start_middleware
    verify
    log_step "✅ 中间件安装完成"
    cat <<EOF

  访问入口:
    MariaDB:  127.0.0.1:3306  (root/${DB_ROOT_PASS})
    Redis:    127.0.0.1:6379  (无用户名/${REDIS_PASS})
    Nacos:    http://localhost:8848/nacos  (nacos/nacos)
    Adminer:  http://localhost:8082  (${DB_USER}/${DB_PASS})

  后续命令:
    $0 status      服务状态
    $0 start       启动
    $0 stop        停止
    $0 uninstall   卸载 (保留数据)

  下一步 (装 Java + 微服务 + nginx):
    sudo ./scripts/deploy-centos.sh install
EOF
    ;;

  start)
    is_root
    cd "$INSTALL_DIR"
    docker compose -f "$COMPOSE_FILE" start
    log_info "已启动"
    ;;

  stop)
    is_root
    cd "$INSTALL_DIR"
    docker compose -f "$COMPOSE_FILE" stop
    log_info "已停止"
    ;;

  restart)
    is_root
    cd "$INSTALL_DIR"
    docker compose -f "$COMPOSE_FILE" restart
    log_info "已重启"
    ;;

  status)
    cd "$INSTALL_DIR"
    docker compose -f "$COMPOSE_FILE" ps
    echo
    log_info "日志目录: $LOG_DIR"
    log_info "Compose 文件: $COMPOSE_FILE"
    ;;

  uninstall)
    is_root
    read -rp "确认卸载中间件? (数据保留在 $DATA_DIR) [y/N] " -r
    [[ ! $REPLY =~ ^[Yy]$ ]] && exit 0
    cd "$INSTALL_DIR"
    docker compose -f "$COMPOSE_FILE" down
    log_info "中间件已卸载, 数据保留在 $DATA_DIR"
    log_info "如需完全清理: rm -rf $DATA_DIR"
    ;;

  *)
    cat <<'EOF'
MiniMax V5.26 - CentOS 中间件安装脚本

用法:
  sudo ./scripts/install-middleware-centos.sh install     安装所有中间件
  sudo ./scripts/install-middleware-centos.sh start       启动
  sudo ./scripts/install-middleware-centos.sh stop        停止
  sudo ./scripts/install-middleware-centos.sh restart     重启
  sudo ./scripts/install-middleware-centos.sh status      状态
  sudo ./scripts/install-middleware-centos.sh uninstall   卸载 (保留数据)

系统: CentOS 7+ / Rocky 8+ / RHEL 8+ / AlmaLinux 8+
依赖: yum, 无需预先装 Docker (脚本自动装)
EOF
    exit 1
    ;;
esac