#!/usr/bin/env bash
# =============================================================
# MiniMax Platform V5.22 - 一键部署脚本 (生产可用)
#
# 设计:
#   - 中间件 (Nacos/MySQL/Redis) 全部用 Docker 启动 (避免污染宿主机)
#   - 12 个微服务 + gateway 用 systemd 启动 (host 上的 jar)
#   - nginx 反代统一入口 :3000
#   - 所有 host 端口都映射到 docker 内部网络
#
# 用法:
#   sudo ./scripts/deploy-minimax.sh install    # 一键安装 (中间件+微服务+nginx)
#   sudo ./scripts/deploy-minimax.sh start      # 启动所有服务
#   sudo ./scripts/deploy-minimax.sh stop       # 停止所有服务
#   sudo ./scripts/deploy-minimax.sh restart    # 重启
#   sudo ./scripts/deploy-minimax.sh status     # 状态
#   sudo ./scripts/deploy-minimax.sh test       # E2E 健康检查
#   sudo ./scripts/deploy-minimax.sh uninstall  # 卸载
#
# 环境:
#   - CentOS 7+ / Ubuntu 20.04+ / Debian 11+
#   - Docker 20.10+ (含 docker compose plugin)
#   - JDK 17 (运行 jar)
#   - 8GB+ RAM
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

# =============== 路径 / 用户 ===============
SRC_DIR="$(cd "$(dirname "$0")/.." && pwd)"
INSTALL_DIR="${INSTALL_DIR:-/opt/minimax}"
LOG_DIR="/var/log/minimax"
BACKUP_DIR="${INSTALL_DIR}/backups"
APP_DIR="${INSTALL_DIR}/apps"
SERVICE_USER="minimax"
SERVICE_UID=999

# =============== 端口分配 ===============
NGINX_PORT=3000
GATEWAY_PORT=8080
NACOS_PORT=8848
MARIADB_PORT=3306
REDIS_PORT=6379

# =============== 凭证 (生产请改) ===============
DB_NAME="minimax_platform"
DB_USER="minimax"
DB_PASS="${DB_ROOT_PASS:-minimax_pass_2024}"
DB_ROOT_PASS="${DB_ROOT_PASS:-minimax_root_2024}"
REDIS_PASS="${REDIS_PASS:-minimax_redis_2024}"
JWT_SECRET="${JWT_SECRET:-VwSWPd816F4nwowFzF5B0F8rihlle2836g6QAh5i13o=}"

# 13 个微服务 (V5.5+ 顺序: auth 在前, ws 在后)
MICRO_SERVICES=(
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

# =============== 工具函数 ===============
is_root() {
  [[ $EUID -eq 0 ]] || log_fatal "需要 root, 请用 sudo"
}

cmd_exists() { command -v "$1" >/dev/null 2>&1; }

detect_os() {
  if [[ -f /etc/os-release ]]; then
    . /etc/os-release
    OS_ID="${ID:-unknown}"
    OS_VER="${VERSION_ID:-}"
  elif [[ -f /etc/redhat-release ]]; then
    OS_ID="centos"
    OS_VER=$(cat /etc/redhat-release | grep -oE '[0-9]+' | head -1)
  else
    OS_ID="unknown"
  fi
  log_info "OS: $OS_ID $OS_VER"
}


# =============================================================
# usage
# =============================================================
usage() {
  cat <<'EOF'
MiniMax Platform V5.22 一键部署

用法:
  sudo ./scripts/deploy-minimax.sh install      一键安装
  sudo ./scripts/deploy-minimax.sh start        启动所有服务
  sudo ./scripts/deploy-minimax.sh stop         停止
  sudo ./scripts/deploy-minimax.sh restart      重启
  sudo ./scripts/deploy-minimax.sh status       状态
  sudo ./scripts/deploy-minimax.sh test         E2E 健康检查
  sudo ./scripts/deploy-minimax.sh uninstall    卸载 (保留数据)
  sudo ./scripts/deploy-minimax.sh check        静态检查 (CI 用)

环境: CentOS 7+ / Ubuntu 20.04+ / Debian 11+
依赖: docker, docker compose, JDK 17
EOF
  exit 1
}

# =============================================================
# install
# =============================================================
cmd_install() {
  is_root
  detect_os

  log_step "MiniMax V5.22 一键安装"
  log_info "安装目录: $INSTALL_DIR"
  log_info "日志目录: $LOG_DIR"

  mkdir -p "$LOG_DIR" "$BACKUP_DIR" "$APP_DIR" "$INSTALL_DIR/frontend"

  step_check_dependencies
  step_create_user
  step_docker_middleware
  step_copy_jars
  step_generate_systemd
  step_install_nginx
  step_start_all

  log_step "✅ 安装完成"
  cat <<EOF

  访问入口:
    前端:       http://localhost:$NGINX_PORT
    API 文档:   http://localhost:$NGINX_PORT/api-docs
    Nacos:      http://localhost:$NACOS_PORT/nacos  (nacos/nacos)
    Adminer:    http://localhost:8082  (minimax/$DB_PASS)
    监控:       http://localhost:3001  (--profile monitoring)

  账号: adminLiugl / Liugl@2026

  后续命令:
    $0 status      服务状态
    $0 test        E2E 健康检查
    $0 logs gateway  查看日志
EOF
}

# =============================================================
# Step: 依赖检查
# =============================================================
step_check_dependencies() {
  log_step "检查依赖"

  # Docker
  if ! cmd_exists docker; then
    log_warn "Docker 未安装, 自动安装..."
    case "$OS_ID" in
      centos|rhel|rocky|almalinux)
        yum install -y yum-utils
        yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
        yum install -y docker-ce docker-ce-cli containerd.io
        ;;
      ubuntu|debian)
        apt-get update
        apt-get install -y -qq ca-certificates curl gnupg
        install -m 0755 -d /etc/apt/keyrings
        curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
        echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" > /etc/apt/sources.list.d/docker.list
        apt-get update
        apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-compose-plugin
        ;;
    esac
    systemctl enable --now docker
  fi
  log_info "Docker: $(docker --version)"

  # Docker Compose (v2 plugin)
  if ! docker compose version >/dev/null 2>&1; then
    log_warn "docker compose 未安装, 装 plugin..."
    case "$OS_ID" in
      centos|rhel|rocky|almalinux) yum install -y docker-compose-plugin ;;
      ubuntu|debian) apt-get install -y -qq docker-compose-plugin ;;
    esac
  fi
  log_info "Compose: $(docker compose version)"

  # Java
  if ! cmd_exists java; then
    log_warn "Java 未安装, 装 OpenJDK 17..."
    case "$OS_ID" in
      centos|rhel|rocky|almalinux)
        yum install -y java-17-openjdk-devel
        ;;
      ubuntu|debian)
        apt-get install -y -qq openjdk-17-jdk-headless
        ;;
    esac
  fi
  java -version 2>&1 | head -1
  log_info "Java: $(java -version 2>&1 | head -1)"

  # Nginx
  if ! cmd_exists nginx; then
    log_warn "Nginx 未安装..."
    case "$OS_ID" in
      centos|rhel|rocky|almalinux) yum install -y nginx ;;
      ubuntu|debian) apt-get install -y -qq nginx ;;
    esac
  fi
  log_info "Nginx: $(nginx -v 2>&1 | head -1)"
}

# =============================================================
# Step: 创建 minimax 用户
# =============================================================
step_create_user() {
  log_step "创建服务用户"
  if ! id "$SERVICE_USER" >/dev/null 2>&1; then
    useradd -r -s /bin/false -u "$SERVICE_UID" "$SERVICE_USER"
    log_info "  ✓ 用户 $SERVICE_USER 创建 (UID=$SERVICE_UID)"
  else
    log_info "  - 用户 $SERVICE_USER 已存在"
  fi
}

# =============================================================
# Step: Docker 中间件 (MariaDB + Redis + Nacos + Adminer)
# =============================================================
step_docker_middleware() {
  log_step "启动中间件 (Docker: MariaDB + Redis + Nacos + Adminer)"

  cd "$SRC_DIR"

  # 1) MariaDB
  log_info "[1/4] 启动 MariaDB ..."
  docker compose up -d mariadb
  log_info "  等待 MariaDB 健康检查 (30s)..."
  for i in $(seq 1 30); do
    sleep 2
    if docker exec minimax-mariadb healthcheck.sh --connect --innodb_initialized >/dev/null 2>&1; then
      log_info "  ✓ MariaDB 就绪 (${i}*2s)"
      break
    fi
  done

  # 2) SQL: docker 自动执行 init/ 里的 .sql 文件 (无操作)
  log_info "[2/4] SQL: docker 自动执行 sql/init/init-minimax.sql ..."

  # 3) Redis
  log_info "[3/4] 启动 Redis ..."
  docker compose up -d redis
  sleep 2
  if docker exec minimax-redis redis-cli -a "$REDIS_PASS" ping >/dev/null 2>&1; then
    log_info "  ✓ Redis 就绪"
  fi

  # 4) Nacos (等 MariaDB 健康)
  log_info "[4/4] 启动 Nacos (等 MariaDB + 20-30s)..."
  docker compose up -d nacos
  for i in $(seq 1 30); do
    sleep 2
    if curl -sf -m 2 "http://127.0.0.1:$NACOS_PORT/nacos/" >/dev/null 2>&1; then
      log_info "  ✓ Nacos 就绪 (${i}*2s)"
      break
    fi
  done

  # 5) Adminer (DB Web GUI, 端口 8082)
  docker compose up -d adminer
  log_info "  ✓ Adminer 就绪: http://localhost:8082"
}

# =============================================================
# Step: 拷贝 jar
# =============================================================
step_copy_jars() {
  log_step "拷贝 jar 到 $APP_DIR"

  if [[ ! -d "$SRC_DIR/backend" ]]; then
    log_warn "  ! $SRC_DIR/backend 不存在, 跳过 (需 mvn install 后再运行)"
    return 0
  fi

  local count=0
  for module_port in "${MICRO_SERVICES[@]}"; do
    local module="${module_port%%:*}"
    # 注意: minimax-gateway 用 -spring-boot.jar classifier (V5.7), 其它用普通 jar
    local src_jar=""
    if [[ -f "$SRC_DIR/backend/minimax-$module/target/minimax-$module.jar" ]]; then
      # 排除空 jar (< 1KB)
      local size=$(stat -c%s "$SRC_DIR/backend/minimax-$module/target/minimax-$module.jar" 2>/dev/null || echo 0)
      if [[ $size -gt 1024 ]]; then
        src_jar="$SRC_DIR/backend/minimax-$module/target/minimax-$module.jar"
      else
        # 试 spring-boot classifier
        if [[ -f "$SRC_DIR/backend/minimax-$module/target/minimax-$module-spring-boot.jar" ]]; then
          src_jar="$SRC_DIR/backend/minimax-$module/target/minimax-$module-spring-boot.jar"
        fi
      fi
    fi
    if [[ -n "$src_jar" ]]; then
      cp -f "$src_jar" "$APP_DIR/minimax-$module.jar"
      log_info "  ✓ $module.jar"
      count=$((count+1))
    else
      log_warn "  ✗ $module.jar 缺失或为空 (需 mvn install)"
    fi
  done

  # Gateway (V5.7 用 spring-boot classifier)
  local gw_jar="$SRC_DIR/backend/minimax-gateway/target/minimax-gateway.jar"
  [[ -f "$gw_jar" ]] || gw_jar="$SRC_DIR/backend/minimax-gateway/target/minimax-gateway-spring-boot.jar"
  if [[ -f "$gw_jar" ]]; then
    cp -f "$gw_jar" "$APP_DIR/minimax-gateway.jar"
    log_info "  ✓ gateway.jar"
  else
    log_warn "  ! gateway.jar 缺失 (需 mvn install)"
  fi

  chown -R "$SERVICE_USER:$SERVICE_USER" "$APP_DIR" 2>/dev/null || true

  if [[ $count -lt 6 ]]; then
    log_warn "只有 $count 个微服务 jar 就绪, 需先 mvn install -DskipTests"
  fi
}

# =============================================================
# Step: 生成 systemd 服务
# =============================================================
step_generate_systemd() {
  log_step "生成 systemd 服务"

  for module_port in "${MICRO_SERVICES[@]}"; do
    local module="${module_port%%:*}"
    local port="${module_port##*:}"
    cat > "/etc/systemd/system/minimax-${module}.service" <<EOF
[Unit]
Description=MiniMax ${module} (V5.22)
After=network.target docker.service
Wants=docker.service

[Service]
Type=simple
User=${SERVICE_USER}
WorkingDirectory=${APP_DIR}
Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk
ExecStart=/usr/bin/java \\
  -Xms${JAVA_XMS:-256m} -Xmx${JAVA_XMX:-512m} \\
  -Dspring.profiles.active=prod \\
  -Dserver.port=${port} \\
  -Dnacos.host=127.0.0.1 \\
  -Dnacos.port=${NACOS_PORT} \\
  -Dmysql.host=127.0.0.1 \\
  -Dmysql.port=${MARIADB_PORT} \\
  -Dmysql.database=${DB_NAME} \\
  -Dmysql.username=${DB_USER} \\
  -Dmysql.password=${DB_PASS} \\
  -Dredis.host=127.0.0.1 \\
  -Dredis.port=${REDIS_PORT} \\
  -Dredis.password=${REDIS_PASS} \\
  -Dminimax.jwt.secret=${JWT_SECRET} \\
  -jar ${APP_DIR}/minimax-${module}.jar
Restart=always
RestartSec=10
StandardOutput=append:${LOG_DIR}/${module}.log
StandardError=append:${LOG_DIR}/${module}.err.log

[Install]
WantedBy=multi-user.target
EOF
  done

  # Gateway
  cat > "/etc/systemd/system/minimax-gateway.service" <<EOF
[Unit]
Description=MiniMax Gateway (V5.22 Spring Cloud Gateway)
After=network.target docker.service minimax-nacos.service

[Service]
Type=simple
User=${SERVICE_USER}
WorkingDirectory=${APP_DIR}
Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk
ExecStart=/usr/bin/java \\
  -Xms${JAVA_XMS:-256m} -Xmx${JAVA_XMX:-512m} \\
  -Dspring.profiles.active=prod \\
  -Dserver.port=${GATEWAY_PORT} \\
  -Dnacos.host=127.0.0.1 \\
  -Dnacos.port=${NACOS_PORT} \\
  -jar ${APP_DIR}/minimax-gateway.jar
Restart=always
RestartSec=10
StandardOutput=append:${LOG_DIR}/gateway.log
StandardError=append:${LOG_DIR}/gateway.err.log

[Install]
WantedBy=multi-user.target
EOF

  systemctl daemon-reload
  log_info "  ✓ 生成 13 个 systemd 服务 (12 微服务 + 1 gateway)"
}

# =============================================================
# Step: 装 nginx (反代 :3000 → gateway :8080 + 静态文件)
# =============================================================
step_install_nginx() {
  log_step "配置 nginx :$NGINX_PORT (反代 + 静态文件)"

  # 用已有的 nginx-minimax-3000.conf (V5.8+ V5.12 优化)
  if [[ -f "$SRC_DIR/scripts/nginx-minimax-3000.conf" ]]; then
    cp "$SRC_DIR/scripts/nginx-minimax-3000.conf" /etc/nginx/conf.d/minimax.conf
    log_info "  ✓ nginx 配置已部署 (含 gzip / API 文档 / WS 精确分流)"
  else
    # 兜底: 生成简化版
    cat > /etc/nginx/conf.d/minimax.conf <<'NGINX'
upstream minimax_gateway { server 127.0.0.1:8080; }
upstream minimax_frontend { server 127.0.0.1:5173; }

server {
    listen 3000 default_server;
    server_name _;
    client_max_body_size 100M;

    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript text/xml;

    # /api → gateway
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 60s;
    }

    # /actuator → gateway
    location /actuator {
        proxy_pass http://127.0.0.1:8080;
        access_log off;
    }

    # /api-docs → monitor 聚合
    location = /api-docs {
        return 302 /api/v1/monitor/api-docs;
    }

    # 静态文件 (前端 dist, 如有)
    root /opt/minimax/frontend/dist;
    index index.html;
    location / {
        try_files $uri $uri/ /index.html;
    }
}
NGINX
    log_info "  ✓ 生成简化 nginx 配置"
  fi

  # 测试配置
  nginx -t && log_info "  ✓ nginx 配置测试通过"
}

# =============================================================
# Step: 启动所有服务
# =============================================================
step_start_all() {
  log_step "启动所有服务 (顺序: mariadb → redis → nacos → gateway → 12 微服务 → nginx)"

  # 1) Docker 中间件 (已在 step_docker_middleware 启动)
  log_info "[1/5] Docker 中间件: 已在 Step 3 启动"

  # 2) Gateway
  log_info "[2/5] 启动 Gateway (端口 $GATEWAY_PORT)..."
  systemctl enable --now minimax-gateway
  sleep 12  # gateway 需从 nacos 拉注册列表
  if systemctl is-active --quiet minimax-gateway; then
    log_info "  ✓ gateway 启动"
  else
    log_warn "  ✗ gateway 启动失败, 查看: $0 logs gateway"
  fi

  # 3) 12 微服务
  log_info "[3/5] 启动 12 个微服务..."
  for module_port in "${MICRO_SERVICES[@]}"; do
    local module="${module_port%%:*}"
    if [[ -f "$APP_DIR/minimax-${module}.jar" ]]; then
      systemctl enable --now "minimax-${module}.service" >/dev/null 2>&1 || true
      log_info "  ✓ $module"
    else
      log_warn "  - $module.jar 缺失, 跳过 (需 mvn install)"
    fi
  done
  sleep 15  # 等微服务注册到 nacos

  # 4) Nginx
  log_info "[4/5] 启动 nginx (端口 $NGINX_PORT)..."
  systemctl enable --now nginx
  log_info "  ✓ nginx 启动"

  # 5) 等服务稳定
  log_info "[5/5] 等 15s 让服务注册完成..."
  sleep 15

  # 验证
  cmd_status
}

# =============================================================
# start
# =============================================================
cmd_start() {
  is_root
  log_step "启动所有服务"

  cd "$SRC_DIR"
  docker compose up -d 2>&1 | tail -5

  systemctl start minimax-gateway 2>/dev/null || true
  sleep 12
  for module_port in "${MICRO_SERVICES[@]}"; do
    local module="${module_port%%:*}"
    systemctl start "minimax-${module}.service" 2>/dev/null || true
  done
  sleep 10
  systemctl start nginx 2>/dev/null || true
  log_info "已启动"
  sleep 5
  cmd_status
}

# =============================================================
# stop
# =============================================================
cmd_stop() {
  is_root
  log_step "停止所有服务"

  # 倒序停
  for module_port in "${MICRO_SERVICES[@]}"; do
    local module="${module_port%%:*}"
    systemctl stop "minimax-${module}.service" 2>/dev/null || true
  done
  systemctl stop minimax-gateway 2>/dev/null || true
  systemctl stop nginx 2>/dev/null || true

  cd "$SRC_DIR"
  docker compose stop 2>&1 | tail -3 || true
  log_info "已停止"
}

# =============================================================
# restart
# =============================================================
cmd_restart() {
  cmd_stop
  sleep 3
  cmd_start
}

# =============================================================
# status
# =============================================================
cmd_status() {
  log_step "服务状态"
  printf "%-30s %-15s %-10s\n" "SERVICE" "STATE" "PORT"
  printf "%-30s %-15s %-10s\n" "------" "-----" "----"

  # Docker
  for svc in mariadb redis nacos adminer; do
    if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "minimax-$svc"; then
      port_var="${svc^^}_PORT"
      port="${!port_var:-$MARIADB_PORT}"
      printf "  ${GREEN}%-28s${NC} ${GREEN}%-15s${NC} %-10s\n" "minimax-$svc (docker)" "running" "$port"
    else
      printf "  ${RED}%-28s${NC} ${RED}%-15s${NC} %-10s\n" "minimax-$svc (docker)" "stopped" "-"
    fi
  done

  # 微服务
  for module_port in "${MICRO_SERVICES[@]}"; do
    local module="${module_port%%:*}"
    local port="${module_port##*:}"
    if systemctl is-active --quiet "minimax-${module}.service" 2>/dev/null; then
      printf "  ${GREEN}%-28s${NC} ${GREEN}%-15s${NC} %-10s\n" "minimax-$module" "active" "$port"
    else
      printf "  ${RED}%-28s${NC} ${RED}%-15s${NC} %-10s\n" "minimax-$module" "inactive" "$port"
    fi
  done

  # Gateway + Nginx
  for svc in "minimax-gateway" "nginx"; do
    if systemctl is-active --quiet "$svc" 2>/dev/null; then
      port="$([ "$svc" = "minimax-gateway" ] && echo "$GATEWAY_PORT" || echo "$NGINX_PORT")"
      printf "  ${GREEN}%-28s${NC} ${GREEN}%-15s${NC} %-10s\n" "$svc" "active" "$port"
    else
      printf "  ${RED}%-28s${NC} ${RED}%-15s${NC} %-10s\n" "$svc" "inactive" "-"
    fi
  done

  echo
  log_info "HTTP 健康检查:"
  for entry in \
    "前端入口|http://127.0.0.1:$NGINX_PORT/" \
    "API 文档|http://127.0.0.1:$NGINX_PORT/api-docs" \
    "Gateway|http://127.0.0.1:$GATEWAY_PORT/actuator/health" \
    "Nacos|http://127.0.0.1:$NACOS_PORT/nacos/" \
    "Adminer|http://127.0.0.1:8082/"; do
    local name="${entry%%|*}"
    local url="${entry##*|}"
    local code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$url" 2>/dev/null)
    if [[ "$code" =~ ^(200|301|302|401)$ ]]; then
      printf "  ${GREEN}%-20s${NC} %s (%s)\n" "$name" "✓" "$code"
    else
      printf "  ${RED}%-20s${NC} %s (%s)\n" "$name" "✗" "$code"
    fi
  done
}


# =============================================================
# check (静态检查, CI 用)
# =============================================================
cmd_check() {
  log_step "静态检查 (CI 模式)"

  local pass=0 fail=0

  check() {
    local name=$1
    local cmd=$2
    if eval "$cmd" >/dev/null 2>&1; then
      log_info "  ✓ $name"
      pass=$((pass+1))
    else
      log_err "  ✗ $name"
      fail=$((fail+1))
    fi
  }

  # bash 语法
  for f in scripts/*.sh; do
    [[ -f "$f" ]] || continue
    if bash -n "$f" 2>/dev/null; then
      log_info "  ✓ bash -n $f"
      pass=$((pass+1))
    else
      log_err "  ✗ bash -n $f"
      fail=$((fail+1))
    fi
  done

  # docker-compose 语法
  if docker compose -f docker-compose.yml config >/dev/null 2>&1; then
    log_info "  ✓ docker compose config"
    pass=$((pass+1))
  else
    log_warn "  - docker compose config (docker 未装, 跳过)"
  fi

  # SQL 平衡
  python3 - <<PYEOF
import re
c = open("sql/init-minimax.sql").read()
c = re.sub(r"--[^\n]*", "", c)
c = re.sub(r"/\*.*?\*/", "", c, flags=re.S)
tables = c.count("CREATE TABLE")
inserts = c.count("INSERT INTO")
quotes = c.count("'") + c.count('"')
ok = tables >= 35 and inserts >= 30 and quotes % 2 == 0
print(f"    tables={tables}, inserts={inserts}, quotes={quotes} (even={quotes%2==0})")
exit(0 if ok else 1)
PYEOF
  if [[ $? -eq 0 ]]; then
    log_info "  ✓ SQL 平衡"
    pass=$((pass+1))
  else
    log_err "  ✗ SQL 平衡"
    fail=$((fail+1))
  fi

  # 必需文件
  for f in docker-compose.yml sql/init-minimax.sql scripts/deploy-minimax.sh; do
    if [[ -f "$f" ]]; then
      log_info "  ✓ 文件存在 $f"
      pass=$((pass+1))
    else
      log_err "  ✗ 文件缺失 $f"
      fail=$((fail+1))
    fi
  done

  log_step "结果: ${GREEN}${pass} 通过${NC} / ${RED}${fail} 失败${NC}"
  return $fail
}

# =============================================================
# test (E2E 健康检查)
# =============================================================
cmd_test() {
  log_step "E2E 健康检查"

  local pass=0 fail=0

  check() {
    local name=$1
    local url=$2
    local code
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$url" 2>/dev/null)
    if [[ "$code" =~ ^(200|301|302|401)$ ]]; then
      log_info "  ✓ $name ($code)"
      pass=$((pass+1))
    else
      log_err "  ✗ $name ($code)"
      fail=$((fail+1))
    fi
  }

  # 基础入口
  check "nginx :3000"       "http://127.0.0.1:$NGINX_PORT/"
  check "api-docs 重定向"   "http://127.0.0.1:$NGINX_PORT/api-docs"
  check "actuator/health"   "http://127.0.0.1:$NGINX_PORT/actuator/health"
  check "gateway :8080"      "http://127.0.0.1:$GATEWAY_PORT/actuator/health"
  check "nacos"             "http://127.0.0.1:$NACOS_PORT/nacos/"
  check "adminer"           "http://127.0.0.1:8082/"

  # 12 微服务 (通过 nginx → gateway → 微服务)
  for module_port in "${MICRO_SERVICES[@]}"; do
    local module="${module_port%%:*}"
    check "$module" "http://127.0.0.1:$NGINX_PORT/api/v1/${module}/actuator/health"
  done

  # 登录测试
  echo
  log_info "登录测试 (adminLiugl):"
  local token=$(curl -s --max-time 5 -X POST "http://127.0.0.1:$NGINX_PORT/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"adminLiugl\",\"password\":\"Liugl@2026\"}" 2>/dev/null | \
    grep -oP '"accessToken"\s*:\s*"\K[^"]+' | head -1)
  if [[ -n "$token" ]]; then
    log_info "  ✓ 登录成功, token=${token:0:30}..."
    pass=$((pass+1))
  else
    log_warn "  ✗ 登录失败 (可能 auth 服务未启或 SQL 未导入)"
    fail=$((fail+1))
  fi

  echo
  log_step "结果: ${GREEN}${pass} 通过${NC} / ${RED}${fail} 失败${NC}"
  return $fail
}

# =============================================================
# uninstall
# =============================================================
cmd_uninstall() {
  is_root
  log_step "卸载 MiniMax"
  read -rp "确认卸载? (数据保留) [y/N] " -r
  [[ ! $REPLY =~ ^[Yy]$ ]] && exit 0

  # 停服务
  for module_port in "${MICRO_SERVICES[@]}"; do
    local module="${module_port%%:*}"
    systemctl stop "minimax-${module}.service" 2>/dev/null || true
    systemctl disable "minimax-${module}.service" 2>/dev/null || true
    rm -f "/etc/systemd/system/minimax-${module}.service"
  done
  systemctl stop minimax-gateway nginx 2>/dev/null || true
  rm -f /etc/systemd/system/minimax-gateway.service
  rm -f /etc/nginx/conf.d/minimax.conf
  systemctl daemon-reload

  # 停 docker
  cd "$SRC_DIR"
  docker compose down 2>&1 | tail -3 || true

  # 保留数据
  log_info "数据保留: $BACKUP_DIR / $LOG_DIR"
  log_info "完成卸载"
}

# =============================================================
# help 别名
# =============================================================
if [[ "${1:-}" =~ ^(-h|--help|help)$ ]]; then
  usage
fi

# =============== 主入口 (放最后, 需所有函数已定义) ===============
ACTION="${1:-}"
shift || true

case "$ACTION" in
  -h|--help|help) usage ;;
  install)    cmd_install ;;
  start)      cmd_start ;;
  stop)       cmd_stop ;;
  restart)    cmd_restart ;;
  status)     cmd_status ;;
  test)       cmd_test ;;
  uninstall)  cmd_uninstall ;;
  check)      cmd_check ;;
  *)          usage ;;
esac
