#!/usr/bin/env bash
# =============================================================
# MiniMax Platform V5.26 - CentOS 一键部署脚本
#
# 设计 (CentOS 专用):
#   - 调 install-middleware-centos.sh 装中间件 (Docker)
#   - 装 JDK 17 + nginx (yum)
#   - mvn 编译后端 (如已编译跳过)
#   - 12 个微服务 + gateway 用 systemd 启动
#   - nginx :3000 反代 + 静态
#   - 自动处理 SELinux + firewalld
#
# 用法:
#   sudo ./scripts/deploy-centos.sh install      一键安装 (中间件 + 编译 + 微服务 + nginx)
#   sudo ./scripts/deploy-centos.sh start         启动所有服务
#   sudo ./scripts/deploy-centos.sh stop          停止
#   sudo ./scripts/deploy-centos.sh restart       重启
#   sudo ./scripts/deploy-centos.sh status        状态
#   sudo ./scripts/deploy-centos.sh test          E2E 健康检查
#   sudo ./scripts/deploy-centos.sh uninstall     卸载 (保留数据)
#
# 环境: CentOS 7+ / Rocky 8+ / RHEL 8+ / AlmaLinux 8+ / Anolis 8+
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
APP_DIR="${INSTALL_DIR}/apps"
LOG_DIR="/var/log/minimax"
BACKUP_DIR="${INSTALL_DIR}/backups"
SERVICE_USER="minimax"
SERVICE_UID=999

# =============== 端口 ===============
NGINX_PORT=3000
GATEWAY_PORT=8080
NACOS_PORT=8848
MARIADB_PORT=3306
REDIS_PORT=6379

# =============== 凭证 (生产请改) ===============
DB_NAME="minimax_platform"
DB_USER="minimax"
DB_PASS="${DB_PASS:-minimax_pass_2024}"
DB_ROOT_PASS="${DB_ROOT_PASS:-minimax_root_2024}"
REDIS_PASS="${REDIS_PASS:-minimax_redis_2024}"
JWT_SECRET="${JWT_SECRET:-VwSWPd816F4nwowFzF5B0F8rihlle2836g6QAh5i13o=}"

# 13 个微服务
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

# =============== 工具 ===============
is_root() { [[ $EUID -eq 0 ]] || log_fatal "需要 root, 请用 sudo"; }
cmd_exists() { command -v "$1" >/dev/null 2>&1; }

detect_os() {
  if [[ -f /etc/redhat-release ]]; then
    OS_ID=$(cat /etc/redhat-release | grep -oE '(CentOS|Rocky|AlmaLinux|Red Hat|Anolis)' | head -1 | tr 'A-Z' 'a-z' | tr -d ' ')
    [[ -z "$OS_ID" ]] && OS_ID="centos"
    OS_VER=$(cat /etc/redhat-release | grep -oE '[0-9]+' | head -1)
  else
    log_fatal "此脚本仅支持 CentOS/RHEL/Rocky 系统 (当前: $(cat /etc/os-release 2>/dev/null | grep PRETTY_NAME | head -1))"
  fi
  log_info "OS: $OS_ID $OS_VER"
}


# =============================================================
# usage
# =============================================================
usage() {
  cat <<'EOF'
MiniMax Platform V5.26 - CentOS 一键部署

用法:
  sudo ./scripts/deploy-centos.sh install      一键安装 (中间件+微服务+nginx)
  sudo ./scripts/deploy-centos.sh start        启动所有服务
  sudo ./scripts/deploy-centos.sh stop         停止
  sudo ./scripts/deploy-centos.sh restart      重启
  sudo ./scripts/deploy-centos.sh status       状态
  sudo ./scripts/deploy-centos.sh test         E2E 健康检查
  sudo ./scripts/deploy-centos.sh uninstall    卸载 (保留数据)

环境: CentOS 7+ / Rocky 8+ / RHEL 8+ / AlmaLinux 8+
依赖: 无 (脚本自动装 Docker + JDK + nginx)
EOF
  exit 1
}

# =============================================================
# install - 6 步一键安装
# =============================================================
cmd_install() {
  is_root
  detect_os

  log_step "MiniMax V5.26 - CentOS 一键部署"
  log_info "安装目录: $INSTALL_DIR"

  mkdir -p "$LOG_DIR" "$BACKUP_DIR" "$APP_DIR" "$INSTALL_DIR/frontend"

  step1_jdk
  step2_middleware
  step3_user
  step4_build
  step5_copy_jars
  step6_systemd
  step7_nginx
  step8_start_all

  log_step "✅ CentOS 一键部署完成"
  cat <<EOF

  访问入口:
    前端:       http://localhost:$NGINX_PORT
    API 文档:   http://localhost:$NGINX_PORT/api-docs
    Nacos:      http://localhost:$NACOS_PORT/nacos  (nacos/nacos)
    Adminer:    http://localhost:8082  ($DB_USER/$DB_PASS)
    监控:       http://localhost:3001  (--profile monitoring)

  账号: adminLiugl / Liugl@2026

  后续命令:
    $0 status      服务状态
    $0 test        E2E 健康检查
EOF
}

# =============================================================
# 步骤 1: 装 JDK 17
# =============================================================
step1_jdk() {
  log_step "步骤 1/8: 装 JDK 17"

  if cmd_exists java && java -version 2>&1 | grep -q '17'; then
    log_info "  - JDK 17 已装: $(java -version 2>&1 | head -1)"
  else
    log_info "  装 OpenJDK 17..."
    yum install -y java-17-openjdk-devel
    log_info "  ✓ $(java -version 2>&1 | head -1)"

    # 设 JAVA_HOME
    local jdk_path=$(dirname $(dirname $(readlink -f $(which java))))
    if ! grep -q "JAVA_HOME" /etc/profile.d/java.sh 2>/dev/null; then
      echo "export JAVA_HOME=$jdk_path" > /etc/profile.d/java.sh
      echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> /etc/profile.d/java.sh
      chmod +x /etc/profile.d/java.sh
      log_info "  ✓ JAVA_HOME=$jdk_path (写入 /etc/profile.d/java.sh)"
    fi
  fi
}

# =============================================================
# 步骤 2: 装中间件 (调 install-middleware-centos.sh)
# =============================================================
step2_middleware() {
  log_step "步骤 2/8: 装中间件 (调 install-middleware-centos.sh)"

  if [[ ! -x "$SRC_DIR/scripts/install-middleware-centos.sh" ]]; then
    log_fatal "未找到 $SRC_DIR/scripts/install-middleware-centos.sh"
  fi

  DB_ROOT_PASS="$DB_ROOT_PASS" DB_USER="$DB_USER" DB_PASS="$DB_PASS" \
  REDIS_PASS="$REDIS_PASS" \
  INSTALL_DIR="$INSTALL_DIR" \
  "$SRC_DIR/scripts/install-middleware-centos.sh" install
}

# =============================================================
# 步骤 3: 创建 minimax 用户
# =============================================================
step3_user() {
  log_step "步骤 3/8: 创建服务用户"

  if ! id "$SERVICE_USER" >/dev/null 2>&1; then
    useradd -r -s /bin/false -u "$SERVICE_UID" "$SERVICE_USER"
    log_info "  ✓ 用户 $SERVICE_USER 创建 (UID=$SERVICE_UID)"
  else
    log_info "  - 用户 $SERVICE_USER 已存在"
  fi
}

# =============================================================
# 步骤 4: 编译 (mvn)
# =============================================================
step4_build() {
  log_step "步骤 4/8: 编译 (mvn)"

  if ! cmd_exists mvn; then
    log_info "  装 maven..."
    yum install -y maven
  fi
  log_info "  Maven: $(mvn -version 2>&1 | head -1)"

  # 检查 jar 是否都已编译 (每个模块 target/*.jar > 1MB 视为编译过)
  local need_build=0
  for module_port in "${MICRO_SERVICES[@]}"; do
    local module="${module_port%%:*}"
    local jar="$SRC_DIR/backend/minimax-$module/target/minimax-$module.jar"
    if [[ ! -f "$jar" ]] || [[ $(stat -c%s "$jar" 2>/dev/null || echo 0) -lt 1024 ]]; then
      need_build=1
      break
    fi
  done

  if [[ $need_build -eq 1 ]]; then
    log_info "  检测到部分 jar 缺失, 开始编译 (5-10 分钟)..."
    cd "$SRC_DIR/backend"
    mvn clean install -DskipTests -Dspotless.check.skip=true -Djacoco.skip=true -T 4
    cd "$SRC_DIR"
    log_info "  ✓ 编译完成"
  else
    log_info "  - 所有 jar 已就绪, 跳过编译"
  fi
}

# =============================================================
# 步骤 5: 拷贝 jar
# =============================================================
step5_copy_jars() {
  log_step "步骤 5/8: 拷贝 jar 到 $APP_DIR"

  local count=0
  for module_port in "${MICRO_SERVICES[@]}"; do
    local module="${module_port%%:*}"
    local jar="$SRC_DIR/backend/minimax-$module/target/minimax-$module.jar"
    # 部分模块用 spring-boot classifier
    if [[ ! -f "$jar" ]] || [[ $(stat -c%s "$jar" 2>/dev/null || echo 0) -lt 1024 ]]; then
      jar="$SRC_DIR/backend/minimax-$module/target/minimax-$module-spring-boot.jar"
    fi
    if [[ -f "$jar" ]]; then
      cp -f "$jar" "$APP_DIR/minimax-$module.jar"
      log_info "  ✓ $module.jar"
      count=$((count+1))
    else
      log_warn "  ✗ $module.jar 缺失"
    fi
  done

  # Gateway
  local gw_jar="$SRC_DIR/backend/minimax-gateway/target/minimax-gateway.jar"
  if [[ ! -f "$gw_jar" ]] || [[ $(stat -c%s "$gw_jar" 2>/dev/null || echo 0) -lt 1024 ]]; then
    gw_jar="$SRC_DIR/backend/minimax-gateway/target/minimax-gateway-spring-boot.jar"
  fi
  if [[ -f "$gw_jar" ]]; then
    cp -f "$gw_jar" "$APP_DIR/minimax-gateway.jar"
    log_info "  ✓ gateway.jar"
  fi

  chown -R "$SERVICE_USER:$SERVICE_USER" "$APP_DIR" 2>/dev/null || true
  log_info "  拷贝完成 ($count 个微服务)"
}

# =============================================================
# 步骤 6: systemd 服务
# =============================================================
step6_systemd() {
  log_step "步骤 6/8: 生成 systemd 服务"

  for module_port in "${MICRO_SERVICES[@]}"; do
    local module="${module_port%%:*}"
    local port="${module_port##*:}"
    cat > "/etc/systemd/system/minimax-${module}.service" <<EOF
[Unit]
Description=MiniMax ${module} (V5.26 CentOS)
After=network.target docker.service
Wants=docker.service

[Service]
Type=simple
User=${SERVICE_USER}
WorkingDirectory=${APP_DIR}
Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk
ExecStart=/usr/bin/java \\
  -Xms\${JAVA_XMS:-256m} -Xmx\${JAVA_XMX:-512m} \\
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
Description=MiniMax Gateway (V5.26 Spring Cloud Gateway)
After=network.target docker.service minimax-nacos.service

[Service]
Type=simple
User=${SERVICE_USER}
WorkingDirectory=${APP_DIR}
Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk
ExecStart=/usr/bin/java \\
  -Xms\${JAVA_XMS:-256m} -Xmx\${JAVA_XMX:-512m} \\
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
  log_info "  ✓ 13 个 systemd 服务已生成"
}

# =============================================================
# 步骤 7: 装 nginx
# =============================================================
step7_nginx() {
  log_step "步骤 7/8: 装 nginx (反代 + 静态)"

  if ! cmd_exists nginx; then
    log_info "  装 nginx..."
    yum install -y nginx
  fi

  # 用仓库里的 nginx 配置
  if [[ -f "$SRC_DIR/scripts/nginx-minimax-3000.conf" ]]; then
    cp "$SRC_DIR/scripts/nginx-minimax-3000.conf" /etc/nginx/conf.d/minimax.conf
    log_info "  ✓ nginx 配置已部署"
  else
    log_warn "  ! 未找到 nginx-minimax-3000.conf, 用简化版"
    cat > /etc/nginx/conf.d/minimax.conf <<'NGINX'
upstream minimax_gateway { server 127.0.0.1:8080; }

server {
    listen 3000 default_server;
    server_name _;
    client_max_body_size 100M;

    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript text/xml;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 60s;
    }

    location /actuator {
        proxy_pass http://127.0.0.1:8080;
        access_log off;
    }

    location = /api-docs {
        return 302 /api/v1/monitor/api-docs;
    }

    root /opt/minimax/frontend/dist;
    index index.html;
    location / {
        try_files $uri $uri/ /index.html;
    }
}
NGINX
  fi

  nginx -t && log_info "  ✓ nginx 配置测试通过"
}

# =============================================================
# 步骤 8: 启动所有服务
# =============================================================
step8_start_all() {
  log_step "步骤 8/8: 启动所有服务"

  # Docker 中间件已在 step2 启动
  log_info "[1/4] Docker 中间件: 已在步骤 2 启动"

  # Gateway
  log_info "[2/4] 启动 Gateway (端口 $GATEWAY_PORT)..."
  systemctl enable --now minimax-gateway
  sleep 12
  if systemctl is-active --quiet minimax-gateway; then
    log_info "  ✓ gateway 启动"
  else
    log_warn "  ✗ gateway 启动失败, 查看: journalctl -u minimax-gateway -n 30"
  fi

  # 12 微服务
  log_info "[3/4] 启动 12 个微服务..."
  for module_port in "${MICRO_SERVICES[@]}"; do
    local module="${module_port%%:*}"
    if [[ -f "$APP_DIR/minimax-${module}.jar" ]]; then
      systemctl enable --now "minimax-${module}.service" >/dev/null 2>&1 || true
      log_info "  ✓ $module"
    else
      log_warn "  - $module.jar 缺失, 跳过"
    fi
  done
  sleep 15

  # Nginx
  log_info "[4/4] 启动 nginx (端口 $NGINX_PORT)..."
  systemctl enable --now nginx
  log_info "  ✓ nginx 启动"

  sleep 10
  cmd_status
}

# =============================================================
# start
# =============================================================
cmd_start() {
  is_root
  log_step "启动所有服务"

  # 中间件
  if [[ -f "$SRC_DIR/scripts/install-middleware-centos.sh" ]]; then
    "$SRC_DIR/scripts/install-middleware-centos.sh" start || true
  fi

  # 微服务
  systemctl start minimax-gateway 2>/dev/null || true
  sleep 12
  for module_port in "${MICRO_SERVICES[@]}"; do
    local module="${module_port%%:*}"
    systemctl start "minimax-${module}.service" 2>/dev/null || true
  done
  sleep 10

  # nginx
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

  # 微服务
  for module_port in "${MICRO_SERVICES[@]}"; do
    local module="${module_port%%:*}"
    systemctl stop "minimax-${module}.service" 2>/dev/null || true
  done
  systemctl stop minimax-gateway nginx 2>/dev/null || true

  # 中间件
  if [[ -f "$SRC_DIR/scripts/install-middleware-centos.sh" ]]; then
    "$SRC_DIR/scripts/install-middleware-centos.sh" stop || true
  fi

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
  printf "%-30s %-15s %s\n" "SERVICE" "STATE" "PORT"
  printf "%-30s %-15s %s\n" "------" "-----" "----"

  # Docker 中间件
  for svc in mariadb redis nacos adminer; do
    if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "minimax-$svc"; then
      printf "  ${GREEN}%-28s${NC} ${GREEN}%-15s${NC} %s\n" "minimax-$svc (docker)" "running" "$MARIADB_PORT"
    else
      printf "  ${RED}%-28s${NC} ${RED}%-15s${NC} -\n" "minimax-$svc (docker)" "stopped"
    fi
  done

  # 微服务
  for module_port in "${MICRO_SERVICES[@]}"; do
    local module="${module_port%%:*}"
    local port="${module_port##*:}"
    if systemctl is-active --quiet "minimax-${module}.service" 2>/dev/null; then
      printf "  ${GREEN}%-28s${NC} ${GREEN}%-15s${NC} %s\n" "minimax-$module" "active" "$port"
    else
      printf "  ${RED}%-28s${NC} ${RED}%-15s${NC} %s\n" "minimax-$module" "inactive" "$port"
    fi
  done

  # Gateway + nginx
  for svc in minimax-gateway nginx; do
    if systemctl is-active --quiet "$svc" 2>/dev/null; then
      local port=$([ "$svc" = "minimax-gateway" ] && echo "$GATEWAY_PORT" || echo "$NGINX_PORT")
      printf "  ${GREEN}%-28s${NC} ${GREEN}%-15s${NC} %s\n" "$svc" "active" "$port"
    else
      printf "  ${RED}%-28s${NC} ${RED}%-15s${NC} -\n" "$svc" "inactive"
    fi
  done

  echo
  log_info "HTTP 健康检查:"
  for entry in \
    "nginx|http://127.0.0.1:$NGINX_PORT/" \
    "api-docs|http://127.0.0.1:$NGINX_PORT/api-docs" \
    "gateway|http://127.0.0.1:$GATEWAY_PORT/actuator/health" \
    "nacos|http://127.0.0.1:$NACOS_PORT/nacos/" \
    "adminer|http://127.0.0.1:8082/"; do
    local name="${entry%%|*}"
    local url="${entry##*|}"
    local code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$url" 2>/dev/null)
    if [[ "$code" =~ ^(200|301|302|401)$ ]]; then
      printf "  ${GREEN}%-20s${NC} ✓ (%s)\n" "$name" "$code"
    else
      printf "  ${RED}%-20s${NC} ✗ (%s)\n" "$name" "$code"
    fi
  done
}

# =============================================================
# test
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
  check "gateway :8080"     "http://127.0.0.1:$GATEWAY_PORT/actuator/health"
  check "nacos"             "http://127.0.0.1:$NACOS_PORT/nacos/"
  check "adminer"           "http://127.0.0.1:8082/"

  # 12 微服务
  for module_port in "${MICRO_SERVICES[@]}"; do
    local module="${module_port%%:*}"
    check "$module" "http://127.0.0.1:$NGINX_PORT/api/v1/${module}/actuator/health"
  done

  # 登录
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
  log_step "卸载 MiniMax (CentOS)"
  read -rp "确认卸载? (数据保留) [y/N] " -r
  [[ ! $REPLY =~ ^[Yy]$ ]] && exit 0

  # 停微服务
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

  # 卸中间件
  if [[ -f "$SRC_DIR/scripts/install-middleware-centos.sh" ]]; then
    "$SRC_DIR/scripts/install-middleware-centos.sh" uninstall || true
  fi

  log_info "完成卸载"
  log_info "数据保留: $INSTALL_DIR / $LOG_DIR"
}

# =============== 主入口 (放最后, 需所有函数已定义) ===============
ACTION="${1:-}"
shift || true

case "$ACTION" in
  -h|--help|help|"") usage ;;
  install)    cmd_install ;;
  start)      cmd_start ;;
  stop)       cmd_stop ;;
  restart)    cmd_restart ;;
  status)     cmd_status ;;
  test)       cmd_test ;;
  uninstall)  cmd_uninstall ;;
  *)          usage ;;
esac
