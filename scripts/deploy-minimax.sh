#!/usr/bin/env bash
# =============================================================
# MiniMax Platform V5.21 - 一键部署脚本 (单文件, 完整闭环)
#
# 功能:
#   install    一键安装 (Docker 模式 / Linux 模式)
#   start      启动所有服务
#   stop       停止
#   restart    重启
#   status     查看状态
#   test       E2E 健康检查
#   backup     备份数据库
#   update     拉代码 + 重打包
#   uninstall  卸载
#
# 用法:
#   sudo ./scripts/deploy-minimax.sh install              # 默认 Docker 模式
#   sudo ./scripts/deploy-minimax.sh install --native     # Linux 原生模式 (apt 装中间件)
#   sudo ./scripts/deploy-minimax.sh start
#   sudo ./scripts/deploy-minimax.sh status
#   sudo ./scripts/deploy-minimax.sh test
#   sudo ./scripts/deploy-minimax.sh uninstall
#
# 环境要求:
#   - Linux (Ubuntu 20.04+ / Debian 11+ / CentOS 8+)
#   - 4 核+ CPU, 8 GB+ RAM
#   - sudo / root 权限
#   - Docker (Docker 模式) / Java 17 + Maven (Native 模式)
# =============================================================

set -euo pipefail

# =============== 颜色 ===============
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0:36m'; NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_err()   { echo -e "${RED}[ERROR]${NC} $*"; }
log_step()  { echo -e "\n${BLUE}══════════${NC} ${CYAN}$*${NC} ${BLUE}══════════${NC}\n"; }

# =============== 配置 ===============
INSTALL_DIR="${INSTALL_DIR:-/opt/minimax}"
LOG_DIR="/var/log/minimax"
BACKUP_DIR="${INSTALL_DIR}/backups"
SERVICE_USER="minimax"
SRC_DIR="$(cd "$(dirname "$0")/.." && pwd)"

# 数据库
DB_NAME="minimax_platform"
DB_USER="minimax"
DB_PASS="${DB_ROOT_PASS:-minimax_pass_2024}"
DB_ROOT_PASS="${DB_ROOT_PASS:-minimax_root_2024}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"

# Redis
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASS="${REDIS_PASS:-minimax_redis_2024}"

# Nacos
NACOS_VERSION="${NACOS_VERSION:-2.3.2}"
NACOS_PORT="${NACOS_PORT:-8848}"

# Gateway
GATEWAY_PORT="${GATEWAY_PORT:-8080}"

# JWT
JWT_SECRET="${JWT_SECRET:-VwSWPd816F4nwowFzF5B0F8rihlle2836g6QAh5i13o=}"

# 模式
MODE="docker"   # docker | native
for arg in "$@"; do
  case "$arg" in
    --docker)  MODE="docker" ;;
    --native)  MODE="native" ;;
  esac
done

# 13 个微服务 + 端口 (V5.5+ V5.7)
MODULES=(
  "auth:8081"
  "chat:8082"
  "model:8083"
  "memory:8084"
  "rag:8085"
  "function:8086"
  "admin:8087"
  "multimodal:8088"
  "monitor:8089"
  "agent:8090"
  "prompt:8091"
  "ws:8095"
)

# 工具函数
is_root() {
  [[ $EUID -eq 0 ]] || { log_err "需要 root 权限, 请用 sudo"; exit 1; }
}

cmd_exists() {
  command -v "$1" >/dev/null 2>&1
}

detect_os() {
  if [[ -f /etc/os-release ]]; then
    . /etc/os-release
    OS="${ID:-unknown}"
  else
    OS="unknown"
  fi
  log_info "OS: $OS"
}

# =============== 模式分发 ===============
case "${1:-}" in
  install)    install_all ;;
  start)      start_services ;;
  stop)       stop_services ;;
  restart)    restart_services ;;
  status)     show_status ;;
  test)       e2e_test ;;
  backup)     backup_all ;;
  update)     update_all ;;
  uninstall)  uninstall_all ;;
  *)
    echo "用法: $0 {install|start|stop|restart|status|test|backup|update|uninstall} [--docker|--native]"
    echo
    echo "  install [--docker|--native]  一键安装 (默认 docker, native=Linux 原生)"
    echo "  start      启动所有服务"
    echo "  stop       停止"
    echo "  restart    重启"
    echo "  status     查看状态"
    echo "  test       E2E 健康检查 (13 服务)"
    echo "  backup     备份数据库"
    echo "  update     git pull + 重打包"
    echo "  uninstall  卸载 (保留数据)"
    exit 1
    ;;
esac

# =============================================================
# install_all
# =============================================================
install_all() {
  is_root
  detect_os

  log_step "MiniMax 一键安装 (V5.21, 模式=$MODE)"
  mkdir -p "$LOG_DIR" "$BACKUP_DIR" "$INSTALL_DIR"/{apps,frontend,data}

  case "$MODE" in
    docker)
      install_docker
      ;;
    native)
      install_native
      ;;
  esac

  case "$MODE" in
    docker)
      init_db_docker
      start_services
      ;;
    native)
      init_db_native
      start_services
      ;;
  esac

  log_step "✅ 安装完成"
  echo "  访问入口:"
  echo "    前端:       http://localhost:3000"
  echo "    API 文档:   http://localhost:3000/api-docs"
  echo "    Nacos:      http://localhost:8848/nacos  (nacos/nacos)"
  echo "    Adminer:    http://localhost:8082  (minimax/minimax_pass_2024)"
  echo "    监控:       http://localhost:3001  (admin/minimax_grafana_2024, 需 monitoring profile)"
  echo "    追踪:       http://localhost:16686  (需 tracing profile)"
  echo
  echo "  账号: adminLiugl / Liugl@2026"
  echo
  echo "  后续: $0 status / $0 test / $0 logs gateway"
}

# =============================================================
# Docker 模式: 一行启动所有中间件
# =============================================================
install_docker() {
  log_step "Docker 模式: 启动中间件"
  if ! cmd_exists docker; then
    log_err "docker 未安装, 请先装 Docker 20.10+"
    exit 1
  fi
  if ! cmd_exists docker compose; then
    log_err "docker compose 未安装 (需 Docker Desktop 或 docker-compose-plugin)"
    exit 1
  fi

  cd "$SRC_DIR"
  log_info "启动必选: MariaDB + Redis + Nacos + Adminer"
  docker compose up -d
  log_info "等 30s 让 MariaDB 初始化..."
  sleep 30

  log_info "可选: 启动监控 (Prometheus + Grafana)"
  docker compose --profile monitoring up -d 2>/dev/null || true

  log_info "可选: 启动追踪 (Jaeger)"
  docker compose --profile tracing up -d 2>/dev/null || true

  # 等待 health
  log_info "等待 Nacos 启动 (20-30s)..."
  for i in $(seq 1 30); do
    sleep 2
    if curl -sf -m 2 "http://127.0.0.1:$NACOS_PORT/nacos/" >/dev/null 2>&1; then
      log_info "Nacos 已就绪 (${i}*2s)"
      break
    fi
  done
}

# =============================================================
# Native 模式: apt 装中间件
# =============================================================
install_native() {
  log_step "Native 模式: 装 Java/Maven/Node/MariaDB/Redis"

  # Java 17
  if ! cmd_exists java; then
    log_info "装 OpenJDK 17..."
    case "$OS" in
      ubuntu|debian) apt-get install -y -qq openjdk-17-jdk-headless ;;
      centos|rhel|rocky|almalinux) dnf install -y java-17-openjdk-devel ;;
    esac
  fi

  # Maven
  if ! cmd_exists mvn; then
    log_info "装 Maven 3.8.7..."
    cd /opt
    wget -q https://archive.apache.org/dist/maven/maven-3/3.8.7/binaries/apache-maven-3.8.7-bin.tar.gz
    tar -xzf apache-maven-3.8.7-bin.tar.gz
    ln -sf /opt/apache-maven-3.8.7/bin/mvn /usr/local/bin/mvn
  fi

  # Node 22
  if ! cmd_exists node; then
    log_info "装 Node.js 22..."
    curl -fsSL https://deb.nodesource.com/setup_22.x | bash -
    case "$OS" in
      ubuntu|debian) apt-get install -y -qq nodejs ;;
    esac
  fi

  # MariaDB
  if ! cmd_exists mariadb; then
    log_info "装 MariaDB..."
    case "$OS" in
      ubuntu|debian) apt-get install -y -qq mariadb-server ;;
      centos|rhel|rocky|almalinux) dnf install -y mariadb-server ;;
    esac
    case "$OS" in
      ubuntu|debian) service mariadb start ;;
      centos|rhel|rocky|almalinux) systemctl start mariadb ;;
    esac
  fi

  # Redis
  if ! cmd_exists redis-cli; then
    log_info "装 Redis..."
    case "$OS" in
      ubuntu|debian) apt-get install -y -qq redis-server ;;
      centos|rhel|rocky|almalinux) dnf install -y redis ;;
    esac
  fi

  # 创建 minimax 用户
  if ! id minimax &>/dev/null; then
    useradd -r -s /bin/false minimax
  fi
  setup_mariadb_native
  setup_redis_native
  build_backend
  build_frontend
  generate_systemd
  generate_nginx
}

setup_mariadb_native() {
  log_info "初始化 MariaDB..."
  mysql -uroot <<EOF
CREATE DATABASE IF NOT EXISTS ${DB_NAME} DEFAULT CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';
GRANT ALL ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
EOF
  log_info "导入 SQL: sql/init-minimax.sql"
  mysql -uroot < "$SRC_DIR/sql/init-minimax.sql"
  log_info "  ✓ 数据库初始化完成"
}

setup_redis_native() {
  if [[ -f /etc/redis/redis.conf ]]; then
    sed -i "s/^# requirepass.*/requirepass ${REDIS_PASS}/" /etc/redis/redis.conf
    systemctl restart redis-server 2>/dev/null || systemctl restart redis 2>/dev/null
  fi
}

init_db_docker() {
  log_step "导入 SQL 到 Docker MariaDB"
  sleep 10  # 等 init scripts
  docker exec -i minimax-mariadb mysql -uroot -p"${DB_ROOT_PASS}" < "$SRC_DIR/sql/init-minimax.sql" && \
    log_info "  ✓ SQL 导入成功" || log_err "  ✗ SQL 导入失败"
}

# =============================================================
# build_backend / build_frontend
# =============================================================
build_backend() {
  log_step "编译后端 (13 模块 + gateway)"
  cd "$SRC_DIR/backend"
  mkdir -p ~/.m2
  cat > ~/.m2/settings.xml <<'EOF'
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id><mirrorOf>central,public</mirrorOf>
      <name>aliyun</name><url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
EOF
  mvn -B -DskipTests -T 1C clean install 2>&1 | tail -10

  mkdir -p "$INSTALL_DIR/apps"
  for module_port in "${MODULES[@]}"; do
    module="${module_port%%:*}"
    cp -f "minimax-${module}/target/minimax-${module}.jar" "$INSTALL_DIR/apps/" 2>/dev/null && \
      log_info "  ✓ ${module}.jar"
  done
  cp -f "minimax-gateway/target/minimax-gateway.jar" "$INSTALL_DIR/apps/" 2>/dev/null && \
    log_info "  ✓ gateway.jar"
}

build_frontend() {
  log_step "编译前端"
  cd "$SRC_DIR/frontend"
  npm config set registry https://registry.npmmirror.com
  npm install --no-audit --no-fund >/dev/null 2>&1
  npm run build
  mkdir -p "$INSTALL_DIR/frontend/dist"
  cp -r dist/* "$INSTALL_DIR/frontend/dist/"
  log_info "前端构建完成 ✓"
}

# =============================================================
# systemd 生成
# =============================================================
generate_systemd() {
  log_step "生成 systemd 服务"
  for module_port in "${MODULES[@]}"; do
    module="${module_port%%:*}"
    port="${module_port##*:}"
    cat > "/etc/systemd/system/minimax-${module}.service" <<EOF
[Unit]
Description=MiniMax ${module}
After=network.target mariadb.service redis-server.service

[Service]
Type=simple
User=${SERVICE_USER}
ExecStart=/usr/bin/java -Xms256m -Xmx512m \
  -Dspring.profiles.active=prod \
  -Dserver.port=${port} \
  -jar ${INSTALL_DIR}/apps/minimax-${module}.jar
Restart=always
StandardOutput=append:${LOG_DIR}/${module}.log

[Install]
WantedBy=multi-user.target
EOF
  done

  # gateway
  cat > "/etc/systemd/system/minimax-gateway.service" <<EOF
[Unit]
Description=MiniMax Gateway (V5.5+)
After=network.target mariadb.service redis-server.service

[Service]
Type=simple
User=${SERVICE_USER}
ExecStart=/usr/bin/java -Xms256m -Xmx512m \
  -Dspring.profiles.active=prod \
  -Dserver.port=${GATEWAY_PORT} \
  -jar ${INSTALL_DIR}/apps/minimax-gateway.jar
Restart=always
StandardOutput=append:${LOG_DIR}/gateway.log

[Install]
WantedBy=multi-user.target
EOF

  # nginx
  cat > "/etc/systemd/system/minimax-nginx.service" <<EOF
[Unit]
Description=MiniMax Nginx (port 3000)
After=network.target

[Service]
Type=forking
PIDFile=/run/nginx.pid
ExecStartPre=/usr/sbin/nginx -t
ExecStart=/usr/sbin/nginx
ExecReload=/bin/kill -s HUP \$MAINPID

[Install]
WantedBy=multi-user.target
EOF

  systemctl daemon-reload
  log_info "  ✓ 生成 14 个 systemd 服务"
}

generate_nginx() {
  log_step "配置 nginx :3000"
  if ! cmd_exists nginx; then
    case "$OS" in
      ubuntu|debian) apt-get install -y -qq nginx ;;
      centos|rhel|rocky|almalinux) dnf install -y nginx ;;
    esac
  fi
  cp "$SRC_DIR/scripts/nginx-minimax-3000.conf" /etc/nginx/conf.d/minimax.conf
  systemctl enable nginx
  log_info "  ✓ nginx 配置完成"
}

# =============================================================
# 启动/停止/重启
# =============================================================
start_services() {
  log_step "启动服务"
  if [[ "$MODE" == "docker" ]]; then
    cd "$SRC_DIR"
    docker compose up -d
  else
    # Native 模式
    systemctl start mariadb redis-server 2>/dev/null || true
    systemctl start minimax-gateway
    sleep 12
    for module_port in "${MODULES[@]}"; do
      module="${module_port%%:*}"
      systemctl start "minimax-${module}.service"
    done
    systemctl start minimax-nginx
  fi
  log_info "启动完成 ✓"
  sleep 5
  show_status
}

stop_services() {
  log_step "停止服务"
  if [[ "$MODE" == "docker" ]]; then
    cd "$SRC_DIR"
    docker compose stop
  else
    for module_port in "${MODULES[@]}"; do
      module="${module_port%%:*}"
      systemctl stop "minimax-${module}.service" 2>/dev/null || true
    done
    systemctl stop minimax-gateway 2>/dev/null || true
    systemctl stop minimax-nginx 2>/dev/null || true
  fi
  log_info "已停止"
}

restart_services() {
  stop_services
  start_services
}

# =============================================================
# 状态
# =============================================================
show_status() {
  log_step "服务状态"
  if [[ "$MODE" == "docker" ]]; then
    cd "$SRC_DIR"
    docker compose ps 2>/dev/null || true
  else
    for module_port in "${MODULES[@]}"; do
      module="${module_port%%:*}"
      systemctl is-active "minimax-${module}.service" 2>/dev/null && \
        echo "  ✓ minimax-${module}" || echo "  ✗ minimax-${module}"
    done
    systemctl is-active minimax-gateway 2>/dev/null && echo "  ✓ minimax-gateway" || echo "  ✗ minimax-gateway"
  fi

  # 健康检查
  echo
  log_info "HTTP 健康:"
  for url in "http://127.0.0.1:3000/" "http://127.0.0.1:8080/actuator/health" "http://127.0.0.1:8848/nacos/" "http://127.0.0.1:8082" "http://127.0.0.1:3306"; do
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$url" 2>/dev/null)
    [[ "$code" =~ ^(200|301|302|401)$ ]] && echo "  ✓ $url ($code)" || echo "  ✗ $url ($code)"
  done
}

# =============================================================
# E2E 测试
# =============================================================
e2e_test() {
  log_step "E2E 健康检查"
  local pass=0 fail=0
  local checks=(
    "http://127.0.0.1:3000/"
    "http://127.0.0.1:3000/api-docs"
    "http://127.0.0.1:3000/actuator/health"
    "http://127.0.0.1:8080/actuator/health"
    "http://127.0.0.1:8848/nacos/"
  )
  for url in "${checks[@]}"; do
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$url" 2>/dev/null)
    if [[ "$code" =~ ^(200|301|302|401)$ ]]; then
      log_info "  ✓ $url ($code)"
      pass=$((pass+1))
    else
      log_err "  ✗ $url ($code)"
      fail=$((fail+1))
    fi
  done

  for module_port in "${MODULES[@]}"; do
    module="${module_port%%:*}"
    url="http://127.0.0.1:3000/api/v1/${module}/actuator/health"
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$url" 2>/dev/null)
    if [[ "$code" =~ ^(200|401)$ ]]; then
      log_info "  ✓ ${module}"
      pass=$((pass+1))
    else
      log_err "  ✗ ${module} ($code)"
      fail=$((fail+1))
    fi
  done

  log_step "结果: ${GREEN}${pass} 通过${NC} / ${RED}${fail} 失败${NC}"
  return $fail
}

# =============================================================
# 备份
# =============================================================
backup_all() {
  log_step "备份数据库"
  local ts=$(date +%Y%m%d_%H%M%S)
  mkdir -p "$BACKUP_DIR"
  if [[ "$MODE" == "docker" ]]; then
    docker exec minimax-mariadb mysqldump -uroot -p"${DB_ROOT_PASS}" "${DB_NAME}" | gzip > "${BACKUP_DIR}/db_${ts}.sql.gz"
  else
    mysqldump -uroot -p"${DB_ROOT_PASS}" "${DB_NAME}" | gzip > "${BACKUP_DIR}/db_${ts}.sql.gz"
  fi
  log_info "  ✓ ${BACKUP_DIR}/db_${ts}.sql.gz"
}

# =============================================================
# 更新
# =============================================================
update_all() {
  log_step "更新代码"
  cd "$SRC_DIR"
  git pull --rebase origin main
  case "$MODE" in
    docker)
      docker compose build
      docker compose up -d
      ;;
    native)
      build_backend
      build_frontend
      systemctl restart minimax-gateway
      for module_port in "${MODULES[@]}"; do
        module="${module_port%%:*}"
        systemctl restart "minimax-${module}.service"
      done
      ;;
  esac
  log_info "更新完成 ✓"
}

# =============================================================
# 卸载
# =============================================================
uninstall_all() {
  log_step "卸载 MiniMax"
  read -p "确认卸载? (数据将保留) [y/N] " -n 1 -r
  echo
  [[ ! $REPLY =~ ^[Yy]$ ]] && exit 0

  if [[ "$MODE" == "docker" ]]; then
    cd "$SRC_DIR"
    docker compose down
  else
    for module_port in "${MODULES[@]}"; do
      module="${module_port%%:*}"
      systemctl stop "minimax-${module}.service" 2>/dev/null || true
      systemctl disable "minimax-${module}.service" 2>/dev/null || true
      rm -f "/etc/systemd/system/minimax-${module}.service"
    done
    systemctl stop minimax-gateway minimax-nginx 2>/dev/null || true
    rm -f /etc/systemd/system/minimax-{gateway,nginx}.service
    systemctl daemon-reload
  fi
  log_info "已卸载 (数据保留在 $BACKUP_DIR)"
}