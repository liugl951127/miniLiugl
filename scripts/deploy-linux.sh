#!/usr/bin/env bash
# ================================================================
# MiniMax 大模型平台 - Linux 一键打包部署脚本 (非 Docker)
#
# V5.12: 集成 Nacos + Gateway (Spring Cloud Gateway 架构)
#
# 适用系统: Ubuntu 20.04+ / Debian 11+ / CentOS 8+
# 运行用户: root (会自动创建 minimax 用户)
# 默认端口:
#   - 8080   gateway (Spring Cloud Gateway)
#   - 8848   nacos (服务发现 + 配置中心)
#   - 8081-8095  业务微服务 (12 个)
#   - 3000   nginx 入口
#
# 用法:
#   sudo ./deploy-linux.sh install        # 安装 Java/Node/MariaDB/Nacos + 编译 + 启 systemd
#   sudo ./deploy-linux.sh start          # 启动所有服务 (nacos → gateway → 微服务)
#   sudo ./deploy-linux.sh stop           # 停止所有服务
#   sudo ./deploy-linux.sh restart        # 重启所有服务
#   sudo ./deploy-linux.sh status         # 查看所有服务状态
#   sudo ./deploy-linux.sh e2e            # V5.12: 一键健康检查 (13 服务)
#   sudo ./deploy-linux.sh logs [module]  # 查看日志 (auth|chat|gateway|nacos|...)
#   sudo ./deploy-linux.sh backup         # 备份数据库 + jar
#   sudo ./deploy-linux.sh update         # 拉代码 + 重新打包 + 重启
#   sudo ./deploy-linux.sh uninstall      # 完全卸载 (保留数据)
#
# 环境变量 (可选):
#   JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
#   NODE_VERSION=22
#   INSTALL_DIR=/opt/minimax
#   NACOS_VERSION=2.3.2
#   DB_ROOT_PASS=minimax_pass_2024
# ================================================================

set -euo pipefail

# =============== 颜色 ===============
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0;36m'; NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_err()   { echo -e "${RED}[ERROR]${NC} $*"; }
log_step()  { echo -e "\n${BLUE}══════════${NC} ${CYAN}$*${NC} ${BLUE}══════════${NC}\n"; }

# =============== 配置 ===============
INSTALL_DIR="${INSTALL_DIR:-/opt/minimax}"
SRC_DIR="$(cd "$(dirname "$0")/.." && pwd)"   # 假设脚本在 scripts/ 下
LOG_DIR="/var/log/minimax"
DATA_DIR="${INSTALL_DIR}/data"
BACKUP_DIR="${INSTALL_DIR}/backups"
SERVICE_USER="minimax"

JAVA_VERSION="17"
NODE_VERSION="${NODE_VERSION:-22}"
MAVEN_VERSION="3.8.7"
NACOS_VERSION="${NACOS_VERSION:-2.3.2}"  # V5.12: Nacos 服务发现
GATEWAY_PORT=8080                         # V5.12: Spring Cloud Gateway
NACOS_PORT=8848
REDIS_PORT=6379
REDIS_PASS="minimax_redis_2024"

DB_NAME="minimax_platform"
DB_USER="minimax"
DB_PASS="${DB_ROOT_PASS:-minimax_pass_2024}"

JWT_SECRET="${JWT_SECRET:-VwSWPd816F4nwowFzF5B0F8rihlle2836g6QAh5i13o=}"
SERVER_PORT_BASE=8081

# 13 个微服务模块 + 端口
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

FRONTEND_PORT=5173

# =============== 工具函数 ===============
detect_os() {
  if [[ -f /etc/os-release ]]; then
    . /etc/os-release
    OS=$ID
    VER=$VERSION_ID
  else
    log_err "无法识别系统"
    exit 1
  fi
  log_info "检测到系统: $OS $VER"
}

is_root() {
  if [[ $EUID -ne 0 ]]; then
    log_err "请用 root 运行 (sudo ./deploy-linux.sh ...)"
    exit 1
  fi
}

cmd_exists() { command -v "$1" &>/dev/null; }

# =============== install_java ===============
install_java() {
  log_step "安装 Java $JAVA_VERSION"
  if cmd_exists java; then
    JAVA_VER=$(java -version 2>&1 | head -1 | awk -F\" '{print $2}')
    if [[ "$JAVA_VER" == "$JAVA_VERSION"* ]]; then
      log_info "Java $JAVA_VER 已安装 ✓"
      return
    fi
  fi

  case "$OS" in
    ubuntu|debian)
      apt-get update -qq
      apt-get install -y -qq openjdk-${JAVA_VERSION}-jdk wget curl unzip git mariadb-server
      ;;
    centos|rhel|rocky|almalinux)
      dnf install -y java-${JAVA_VERSION}-openjdk-devel wget curl unzip git mariadb-server
      ;;
    *)
      log_err "不支持的系统: $OS"; exit 1
      ;;
  esac
  java -version
}

# =============== install_maven ===============
install_maven() {
  log_step "安装 Maven $MAVEN_VERSION"
  if cmd_exists mvn; then
    log_info "Maven $(mvn -v | head -1 | awk '{print $3}') 已安装 ✓"
    return
  fi

  cd /opt
  wget -q "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
  tar -xzf "apache-maven-${MAVEN_VERSION}-bin.tar.gz"
  rm -f "apache-maven-${MAVEN_VERSION}-bin.tar.gz"
  ln -sf "/opt/apache-maven-${MAVEN_VERSION}/bin/mvn" /usr/local/bin/mvn
  log_info "Maven $(mvn -v | head -1 | awk '{print $3}') 安装完成 ✓"
}

# =============== install_nacos (V5.12) ===============
install_nacos() {
  log_step "安装 Nacos $NACOS_VERSION (V5.12 服务发现)"

  if [[ -d "$INSTALL_DIR/nacos" ]]; then
    log_info "Nacos 已安装 ✓"
    return
  fi

  cd /opt
  NACOS_TGZ="nacos-server-${NACOS_VERSION}.tar.gz"
  if [[ ! -f "$NACOS_TGZ" ]]; then
    log_info "下载 Nacos..."
    wget -q "https://github.com/alibaba/nacos/releases/download/${NACOS_VERSION}/nacos-server-${NACOS_VERSION}.tar.gz" \
      -O "$NACOS_TGZ" || {
        log_err "Nacos 下载失败, 请检查网络"
        return 1
      }
  fi

  tar -xzf "$NACOS_TGZ"
  mv "nacos" "$INSTALL_DIR/nacos"
  rm -f "$NACOS_TGZ"

  # 配 standalone 模式 + MySQL 持久化
  cat > "$INSTALL_DIR/nacos/conf/application.properties" <<EOF
# V5.12 standalone mode
server.servlet.contextPath=/nacos
server.port=${NACOS_PORT}
spring.datasource.platform=mysql
db.num=1
db.url.0=jdbc:mysql://127.0.0.1:3306/${DB_NAME}?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
db.user.0=${DB_USER}
db.password.0=${DB_PASS}
nacos.core.auth.enabled=true
nacos.core.auth.server.identity.key=minimaxKey
nacos.core.auth.server.identity.value=minimaxSecret
nacos.core.auth.plugin.nacos.token.secret.key=${JWT_SECRET}
EOF

  # standalone 启动脚本 (V5.12: 用外部 MySQL 避免内置 Derby)
  cat > "$INSTALL_DIR/nacos/bin/startup-standalone.sh" <<'EOF'
#!/bin/bash
# V5.12: standalone mode (single node)
bash "$(dirname $0)/startup.sh" -m standalone
EOF
  chmod +x "$INSTALL_DIR/nacos/bin/startup-standalone.sh"

  chown -R "$SERVICE_USER:$SERVICE_USER" "$INSTALL_DIR/nacos"
  log_info "Nacos $NACOS_VERSION 安装完成 ✓ (端口 $NACOS_PORT, MySQL 持久化)"
}

# =============== install_redis (V5.12) ===============
install_redis() {
  log_step "安装 Redis (V5.12 限流/缓存)"

  if cmd_exists redis-cli; then
    log_info "Redis 已安装 ✓"
    return
  fi

  case "$OS" in
    ubuntu|debian) apt-get install -y -qq redis-server ;;
    centos|rhel|rocky|almalinux) dnf install -y redis ;;
  esac

  # 配置密码 + bind
  if [[ -f /etc/redis/redis.conf ]]; then
    sed -i "s/^# requirepass.*/requirepass ${REDIS_PASS}/" /etc/redis/redis.conf
    sed -i "s/^bind 127.0.0.1.*/bind 127.0.0.1/" /etc/redis/redis.conf
  fi
  systemctl enable redis-server 2>/dev/null || systemctl enable redis 2>/dev/null
  systemctl restart redis-server 2>/dev/null || systemctl restart redis 2>/dev/null
  log_info "Redis 安装完成 ✓ (端口 $REDIS_PORT, 密码 ${REDIS_PASS})"
}

# =============== install_node ===============
install_node() {
  log_step "安装 Node.js $NODE_VERSION"
  if cmd_exists node; then
    NODE_VER=$(node -v | sed 's/v//')
    if [[ "${NODE_VER%%.*}" -ge "$NODE_VERSION" ]]; then
      log_info "Node v$NODE_VER 已安装 ✓"
      return
    fi
  fi

  curl -fsSL "https://deb.nodesource.com/setup_${NODE_VERSION}.x" -o /tmp/node-setup.sh
  bash /tmp/node-setup.sh
  case "$OS" in
    ubuntu|debian) apt-get install -y -qq nodejs ;;
    centos|rhel|rocky|almalinux) dnf install -y nodejs ;;
  esac
  rm -f /tmp/node-setup.sh
  node -v
  npm -v
}

# =============== setup_mariadb ===============
setup_mariadb() {
  log_step "初始化 MariaDB"
  if ! cmd_exists mysql; then
    log_err "MariaDB 未安装"
    exit 1
  fi

  # 启动 mariadb
  systemctl enable mariadb
  systemctl start mariadb

  # 设置 root 密码 + 创建库
  log_info "创建数据库 $DB_NAME..."
  mysql -uroot <<EOF
ALTER USER 'root'@'localhost' IDENTIFIED BY '${DB_PASS}';
CREATE DATABASE IF NOT EXISTS ${DB_NAME} DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
EOF

  # 导入 SQL
  if [[ -f "$SRC_DIR/sql/init-minimax.sql" ]]; then
    log_info "导入 init-minimax.sql..."
    mysql -uroot -p"${DB_PASS}" < "$SRC_DIR/sql/init-minimax.sql" 2>&1 | tail -3
    log_info "数据库初始化完成 ✓"
  else
    log_warn "未找到 sql/init-minimax.sql, 跳过导入"
  fi
}

# =============== create_user ===============
create_user() {
  log_step "创建服务用户 $SERVICE_USER"
  if ! id -u "$SERVICE_USER" &>/dev/null; then
    useradd -r -s /bin/bash -d "$INSTALL_DIR" "$SERVICE_USER"
    log_info "用户 $SERVICE_USER 创建 ✓"
  fi
}

# =============== build_backend ===============
build_backend() {
  log_step "编译后端 (13 个模块)"
  cd "$SRC_DIR/backend"

  # 配置阿里云镜像
  mkdir -p ~/.m2
  cat > ~/.m2/settings.xml <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central,public</mirrorOf>
      <name>aliyun maven</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
EOF

  # mvn install (跳过测试)
  mvn -B -DskipTests -T 1C clean install 2>&1 | tail -20

  # 拷贝 jar (V5.12: 12 微服务 + gateway)
  mkdir -p "$INSTALL_DIR/apps"
  for module_port in "${MODULES[@]}"; do
    module="${module_port%%:*}"
    jar="$SRC_DIR/backend/minimax-${module}/target/minimax-${module}.jar"
    if [[ -f "$jar" ]]; then
      cp -f "$jar" "$INSTALL_DIR/apps/"
      log_info "  ✓ ${module}.jar"
    else
      log_warn "  ✗ ${module}.jar 未生成"
    fi
  done
  # V5.12: Spring Cloud Gateway (jar 含 spring-boot classifier)
  if [[ -f "$SRC_DIR/backend/minimax-gateway/target/minimax-gateway.jar" ]]; then
    cp -f "$SRC_DIR/backend/minimax-gateway/target/minimax-gateway.jar" "$INSTALL_DIR/apps/"
    log_info "  ✓ gateway.jar (V5.12 Spring Cloud Gateway)"
  else
    log_warn "  ✗ gateway.jar 未生成 (需 minimax-gateway 模块)"
  fi
}

# =============== build_frontend ===============
build_frontend() {
  log_step "编译前端"
  cd "$SRC_DIR/frontend"

  # 配置淘宝镜像
  npm config set registry https://registry.npmmirror.com

  npm install --no-audit --no-fund
  npm run build

  mkdir -p "$INSTALL_DIR/frontend/dist"
  cp -r dist/* "$INSTALL_DIR/frontend/dist/"
  log_info "前端构建完成 ✓"
}

# =============== generate_systemd ===============
generate_systemd() {
  log_step "生成 systemd 服务"
  for module_port in "${MODULES[@]}"; do
    module="${module_port%%:*}"
    port="${module_port##*:}"

    cat > "/etc/systemd/system/minimax-${module}.service" <<EOF
[Unit]
Description=MiniMax ${module} microservice
After=network.target mariadb.service

[Service]
Type=simple
User=${SERVICE_USER}
Group=${SERVICE_USER}
WorkingDirectory=${INSTALL_DIR}/apps
ExecStart=/usr/bin/java \\
  -Xms256m -Xmx512m \\
  -Dspring.profiles.active=prod \\
  -Dserver.port=${port} \\
  -jar ${INSTALL_DIR}/apps/minimax-${module}.jar
Restart=always
RestartSec=5
LimitNOFILE=65536
StandardOutput=append:${LOG_DIR}/${module}.log
StandardError=append:${LOG_DIR}/${module}.err.log

[Install]
WantedBy=multi-user.target
EOF
    log_info "  ✓ minimax-${module}.service (端口 ${port})"
  done

  # 前端用 nginx 部署, 不需要 systemd
  # 但加一个 minimax-frontend.service 用 serve
  cat > /etc/systemd/system/minimax-frontend.service <<EOF
[Unit]
Description=MiniMax Frontend (Vite preview)
After=network.target

[Service]
Type=simple
User=${SERVICE_USER}
Group=${SERVICE_USER}
WorkingDirectory=${INSTALL_DIR}/frontend
ExecStart=/usr/bin/npx vite preview --host 0.0.0.0 --port ${FRONTEND_PORT}
Restart=always
RestartSec=5
StandardOutput=append:${LOG_DIR}/frontend.log
StandardError=append:${LOG_DIR}/frontend.err.log

[Install]
WantedBy=multi-user.target
EOF

  # nginx 反向代理服务
  cat > /etc/systemd/system/minimax-nginx.service <<EOF
[Unit]
Description=MiniMax Nginx Reverse Proxy
After=network.target

[Service]
Type=forking
PIDFile=/run/nginx.pid
ExecStartPre=/usr/sbin/nginx -t
ExecStart=/usr/sbin/nginx
ExecReload=/bin/kill -s HUP \$MAINPID
ExecStop=/bin/kill -s QUIT \$MAINPID
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF

  # V5.12: Nacos systemd 服务
  cat > /etc/systemd/system/minimax-nacos.service <<EOF
[Unit]
Description=MiniMax Nacos ${NACOS_VERSION} (V5.12 服务发现)
After=network.target mariadb.service
Wants=mariadb.service

[Service]
Type=forking
User=${SERVICE_USER}
Group=${SERVICE_USER}
Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
Environment=JVM_XMS=512m
Environment=JVM_XMX=1g
Environment=JVM_XMN=512m
WorkingDirectory=${INSTALL_DIR}/nacos
ExecStart=${INSTALL_DIR}/nacos/bin/startup-standalone.sh
ExecStop=${INSTALL_DIR}/nacos/bin/shutdown.sh
Restart=on-failure
RestartSec=10
LimitNOFILE=65536
StandardOutput=append:${LOG_DIR}/nacos.log
StandardError=append:${LOG_DIR}/nacos.err.log

[Install]
WantedBy=multi-user.target
EOF
  log_info "  ✓ minimax-nacos.service (端口 $NACOS_PORT)"

  # V5.12: Spring Cloud Gateway systemd 服务
  cat > /etc/systemd/system/minimax-gateway.service <<EOF
[Unit]
Description=MiniMax Gateway (Spring Cloud Gateway, V5.12)
After=network.target minimax-nacos.service mariadb.service
Wants=minimax-nacos.service

[Service]
Type=simple
User=${SERVICE_USER}
Group=${SERVICE_USER}
WorkingDirectory=${INSTALL_DIR}/apps
ExecStart=/usr/bin/java \\
  -Xms256m -Xmx512m \\
  -Dspring.profiles.active=prod \\
  -Dserver.port=${GATEWAY_PORT} \\
  -Dnacos.host=127.0.0.1 \\
  -Dnacos.port=${NACOS_PORT} \\
  -jar ${INSTALL_DIR}/apps/minimax-gateway.jar
Restart=always
RestartSec=5
LimitNOFILE=65536
StandardOutput=append:${LOG_DIR}/gateway.log
StandardError=append:${LOG_DIR}/gateway.err.log

[Install]
WantedBy=multi-user.target
EOF
  log_info "  ✓ minimax-gateway.service (端口 $GATEWAY_PORT)"

  systemctl daemon-reload
}

# =============== generate_nginx ===============
generate_nginx() {
  log_step "生成 nginx 配置"

  if ! cmd_exists nginx; then
    case "$OS" in
      ubuntu|debian) apt-get install -y -qq nginx ;;
      centos|rhel|rocky|almalinux) dnf install -y nginx ;;
    esac
  fi

  cat > /etc/nginx/conf.d/minimax.conf <<EOF
upstream minimax_auth      { server 127.0.0.1:8081; }
upstream minimax_chat      { server 127.0.0.1:8082; }
upstream minimax_model     { server 127.0.0.1:8083; }
upstream minimax_memory    { server 127.0.0.1:8084; }
upstream minimax_rag       { server 127.0.0.1:8085; }
upstream minimax_function  { server 127.0.0.1:8086; }
upstream minimax_admin     { server 127.0.0.1:8087; }
upstream minimax_multimodal{ server 127.0.0.1:8088; }
upstream minimax_monitor   { server 127.0.0.1:8089; }
upstream minimax_agent     { server 127.0.0.1:8090; }
upstream minimax_prompt    { server 127.0.0.1:8091; }
upstream minimax_ws        { server 127.0.0.1:8095; }
upstream minimax_frontend  { server 127.0.0.1:${FRONTEND_PORT}; }

server {
    # V5.12: 端口 3000 统一入口 (避免 80 端口冲突, 走 gateway 8080)
    listen 3000 default_server;
    server_name _;

    client_max_body_size 100M;

    # 前端
    location / {
        proxy_pass http://minimax_frontend;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    }

    # 网关: 按模块路由
    location /api/v1/auth/      { proxy_pass http://minimax_auth;      proxy_set_header Host \$host; proxy_set_header X-Real-IP \$remote_addr; proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for; proxy_read_timeout 300s; }
    location /api/v1/chat/      { proxy_pass http://minimax_chat;      proxy_set_header Host \$host; proxy_set_header X-Real-IP \$remote_addr; proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for; }
    location /api/v1/model/     { proxy_pass http://minimax_model;     proxy_set_header Host \$host; proxy_set_header X-Real-IP \$remote_addr; proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for; proxy_read_timeout 300s; }
    location /api/v1/memory/    { proxy_pass http://minimax_memory;    proxy_set_header Host \$host; proxy_set_header X-Real-IP \$remote_addr; proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for; }
    location /api/v1/rag/       { proxy_pass http://minimax_rag;       proxy_set_header Host \$host; proxy_set_header X-Real-IP \$remote_addr; proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for; }
    location /api/v1/function/  { proxy_pass http://minimax_function;  proxy_set_header Host \$host; proxy_set_header X-Real-IP \$remote_addr; proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for; }
    location /api/v1/admin/     { proxy_pass http://minimax_admin;     proxy_set_header Host \$host; proxy_set_header X-Real-IP \$remote_addr; proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for; }
    location /api/v1/multi/     { proxy_pass http://minimax_multimodal;proxy_set_header Host \$host; proxy_set_header X-Real-IP \$remote_addr; proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for; proxy_read_timeout 300s; }
    location /api/v1/monitor/   { proxy_pass http://minimax_monitor;   proxy_set_header Host \$host; proxy_set_header X-Real-IP \$remote_addr; proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for; }
    location /api/v1/agent/     { proxy_pass http://minimax_agent;     proxy_set_header Host \$host; proxy_set_header X-Real-IP \$remote_addr; proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for; proxy_read_timeout 600s; }
    location /api/v1/prompt/    { proxy_pass http://minimax_prompt;    proxy_set_header Host \$host; proxy_set_header X-Real-IP \$remote_addr; proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for; }
    location /ws/              { proxy_pass http://minimax_ws;        proxy_set_header Host \$host; proxy_set_header X-Real-IP \$remote_addr; proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for; proxy_http_version 1.1; proxy_set_header Upgrade \$http_upgrade; proxy_set_header Connection "upgrade"; }

    location /health { return 200 'OK'; add_header Content-Type text/plain; }
}
EOF

  nginx -t && systemctl reload nginx
}

# =============== start_services ===============
start_services() {
  log_step "启动所有服务 (V5.12 顺序: nacos -> gateway -> 12 微服务 -> nginx)"
  systemctl enable mariadb
  systemctl start mariadb

  # V5.12: Nacos (服务发现基础)
  systemctl enable --now minimax-nacos
  log_info "  ✓ minimax-nacos (端口 $NACOS_PORT)"
  log_info "  ⏳ 等待 Nacos 启动 (需 20-30s)..."
  sleep 25

  # V5.12: Spring Cloud Gateway
  systemctl enable --now minimax-gateway
  log_info "  ✓ minimax-gateway (端口 $GATEWAY_PORT)"
  sleep 12   # gateway 需从 Nacos 发现所有微服务

  # 12 个业务微服务
  for module_port in "${MODULES[@]}"; do
    module="${module_port%%:*}"
    systemctl enable "minimax-${module}.service"
    systemctl start  "minimax-${module}.service"
    log_info "  ✓ minimax-${module}"
  done

  # 前端 + nginx
  systemctl enable --now minimax-frontend minimax-nginx
  log_info "  ✓ minimax-frontend (端口 ${FRONTEND_PORT})"
  log_info "  ✓ minimax-nginx (端口 3000)"

  sleep 10
  show_status
}

# =============== stop_services ===============
stop_services() {
  log_step "停止所有服务"
  systemctl stop minimax-nginx 2>/dev/null || true
  systemctl stop minimax-frontend 2>/dev/null || true
  # V5.12: 倒序停 (微服务 -> gateway -> nacos)
  for module_port in "${MODULES[@]}"; do
    module="${module_port%%:*}"
    systemctl stop "minimax-${module}.service" 2>/dev/null || true
  done
  systemctl stop minimax-gateway 2>/dev/null || true
  systemctl stop minimax-nacos 2>/dev/null || true
  log_info "已停止"
}

# =============== restart_services ===============
restart_services() {
  stop_services
  sleep 2
  start_services
}

# =============== show_status ===============
show_status() {
  log_step "服务状态"
  printf "%-25s %-10s %-10s %s\n" "SERVICE" "STATE" "UPTIME" "PORT"
  printf "%-25s %-10s %-10s %s\n" "-------" "-----" "------" "----"
  for module_port in "${MODULES[@]}"; do
    module="${module_port%%:*}"
    port="${module_port##*:}"
    if systemctl is-active --quiet "minimax-${module}.service"; then
      state="${GREEN}active${NC}"
      uptime=$(systemctl show "minimax-${module}.service" --property=ActiveEnterTimestamp --value 2>/dev/null)
    else
      state="${RED}inactive${NC}"
      uptime="-"
    fi
    printf "%-25s %-20s %-10s %s\n" "minimax-${module}" "$state" "${uptime: -8}" "$port"
  done
  echo
  systemctl is-active --quiet minimax-frontend && echo "minimax-frontend            ${GREEN}active${NC} (port $FRONTEND_PORT)" || echo "minimax-frontend            ${RED}inactive${NC}"
  systemctl is-active --quiet minimax-nginx && echo "minimax-nginx               ${GREEN}active${NC} (port 3000)" || echo "minimax-nginx               ${RED}inactive${NC}"
  # V5.12: nacos + gateway
  systemctl is-active --quiet minimax-nacos && echo "minimax-nacos               ${GREEN}active${NC} (port $NACOS_PORT)" || echo "minimax-nacos               ${RED}inactive${NC}"
  systemctl is-active --quiet minimax-gateway && echo "minimax-gateway             ${GREEN}active${NC} (port $GATEWAY_PORT)" || echo "minimax-gateway             ${RED}inactive${NC}"
  systemctl is-active --quiet redis-server && echo "redis (system)              ${GREEN}active${NC} (port $REDIS_PORT)" || systemctl is-active --quiet redis && echo "redis (system)              ${GREEN}active${NC} (port $REDIS_PORT)" || echo "redis (system)              ${RED}inactive${NC}"
}

# =============== e2e_health_check (V5.12 + V5.15) ===============
# V5.15: 调用 e2e-full-test.sh 跑完整端到端 (含 JWT/TraceId/Prometheus)
# 旧 V5.12 健康检查代码保留, 通过 e2e_health_check_quick 调用
e2e_health_check_quick() {

  local pass=0
  local fail=0
  local color

  check() {
    local name=$1
    local url=$2
    local timeout=${3:-3}
    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$timeout" "$url" 2>/dev/null)
    if [[ "$http_code" =~ ^(200|301|302|401)$ ]]; then
      color="${GREEN}"
      pass=$((pass+1))
    else
      color="${RED}"
      fail=$((fail+1))
    fi
    printf "  %-30s %s%s%s (code=%s)
" "$name" "$color" "${http_code:-timeout}" "${NC}" "${http_code:-N/A}"
  }

  echo "[1] 基础设施"
  check "nacos (8848)"        "http://127.0.0.1:8848/nacos/" 5
  check "redis (6379)"        "http://127.0.0.1:6379" 2 || true
  check "mariadb (3306)"      "http://127.0.0.1:3306" 2 || true
  echo
  echo "[2] 入口 (nginx :3000 -> gateway :8080)"
  check "nginx /"             "http://127.0.0.1:3000/" 5
  check "api-docs"            "http://127.0.0.1:3000/api-docs" 5
  check "actuator/health"     "http://127.0.0.1:3000/actuator/health" 5
  echo
  echo "[3] 网关 + 微服务 (走 lb://minimax-* 转发)"
  check "gateway :8080"       "http://127.0.0.1:8080/actuator/health" 5
  for module_port in "${MODULES[@]}"; do
    module="${module_port%%:*}"
    check "$module" "http://127.0.0.1:3000/api/v1/${module}/actuator/health" 5
  done

  echo
  log_step "结果: ${GREEN}${pass} 通过${NC} / ${RED}${fail} 失败${NC}"
  [[ $fail -eq 0 ]] && log_info "✅ 所有服务健康" || log_warn "⚠️  有 ${fail} 个服务异常, 请检查 $0 logs <module>"
  return $fail
}

# =============== e2e_full (V5.15) ===============
# 调用 e2e-full-test.sh 跑完整端到端测试 (含 JWT/跨服务/TraceId/Prometheus)
e2e_full() {
  log_step "V5.15 完整 E2E 测试 (含鉴权/TraceId/Prometheus)"

  local script_path="$SRC_DIR/scripts/e2e-full-test.sh"
  if [[ ! -f "$script_path" ]]; then
    log_err "e2e-full-test.sh 未找到: $script_path"
    return 1
  fi
  chmod +x "$script_path"

  # 跑 quick 模式 (健康检查) + 完整测试
  log_info "[阶段 1/2] 快速健康检查..."
  if "$script_path" --quick; then
    log_info "✅ 健康检查通过"
  else
    log_warn "⚠️  有服务异常, 但继续完整测试..."
  fi

  echo
  log_info "[阶段 2/2] 完整 E2E 测试..."
  "$script_path" --full
  return $?
}

# =============== show_logs ===============
show_logs() {
  local module="${1:-}"
  if [[ -z "$module" ]]; then
    echo "用法: $0 logs <module>"
    echo "可用模块: ${MODULES[*]%%:*} frontend nginx gateway nacos redis"
    return
  fi
  if [[ "$module" == "redis" ]]; then
    tail -f /var/log/redis/redis-server.log 2>/dev/null || tail -f /var/log/redis/redis.log
    return 0
  fi
  if [[ "$module" == "nacos" ]]; then
    tail -f ${LOG_DIR}/nacos.log
    return 0
  fi
  if [[ "$module" == "frontend" || "$module" == "nginx" ]]; then
    journalctl -u "minimax-${module}" -f
  else
    journalctl -u "minimax-${module}" -f
  fi
}

# =============== backup ===============
backup_all() {
  log_step "备份数据库 + jar"
  local ts=$(date +%Y%m%d_%H%M%S)
  mkdir -p "$BACKUP_DIR"

  # 数据库
  log_info "备份数据库..."
  mysqldump -uroot -p"${DB_PASS}" --single-transaction "${DB_NAME}" | gzip > "${BACKUP_DIR}/db_${ts}.sql.gz"
  log_info "  ✓ db_${ts}.sql.gz"

  # jar
  log_info "备份 jar..."
  tar -czf "${BACKUP_DIR}/apps_${ts}.tar.gz" -C "$INSTALL_DIR" apps/
  log_info "  ✓ apps_${ts}.tar.gz"

  # 仅保留最近 5 个备份
  ls -t "${BACKUP_DIR}"/*.gz 2>/dev/null | tail -n +6 | xargs -r rm -f
  log_info "备份目录: $BACKUP_DIR"
}

# =============== update ===============
update_all() {
  log_step "更新代码 + 重打包"
  cd "$SRC_DIR"
  git pull --rebase
  build_backend
  build_frontend
  restart_services
}

# =============== install ===============
install_all() {
  is_root
  detect_os

  log_step "MiniMax 一键安装 (V5.12 Spring Cloud Gateway 架构)"
  mkdir -p "$LOG_DIR" "$BACKUP_DIR" "$INSTALL_DIR"/{apps,frontend,data}

  install_java
  install_maven
  install_node
  create_user

  setup_mariadb
  install_redis        # V5.12: 限流/缓存
  install_nacos        # V5.12: 服务发现

  build_backend
  build_frontend

  generate_systemd
  generate_nginx

  chown -R "$SERVICE_USER:$SERVICE_USER" "$INSTALL_DIR" "$LOG_DIR"
  systemctl enable mariadb

  start_services

  log_step "✅ 安装完成 (V5.12)"
  echo
  echo "  访问入口 (V5.12):"
  echo "    前端:       http://localhost:3000"
  echo "    API:        http://localhost:3000/api/v1/<module>/..."
  echo "    API 文档:   http://localhost:3000/api-docs"
  echo "    Nacos:      http://localhost:8848/nacos  (nacos/nacos)"
  echo "    监控:       http://localhost:3000/admin/metrics"
  echo "    账号:       adminLiugl / Liugl@2026"
  echo
  echo "  端口分配:"
  echo "    3000  nginx 入口"
  echo "    8080  Spring Cloud Gateway"
  echo "    8848  Nacos 服务发现"
  echo "    6379  Redis (限流/缓存)"
  echo "    3306  MariaDB"
  echo "    8081-8095  12 业务微服务"
  echo
  echo "  服务管理:"
  echo "    $0 status         查看所有服务状态"
  echo "    $0 e2e            V5.12: 一键健康检查 (13 服务)"
  echo "    $0 restart        重启所有服务"
  echo "    $0 logs gateway   查看 gateway 日志"
  echo "    $0 logs nacos     查看 nacos 日志"
  echo
}

# =============== uninstall ===============
uninstall_all() {
  log_step "卸载 MiniMax (保留数据)"
  stop_services

  for module_port in "${MODULES[@]}"; do
    module="${module_port%%:*}"
    rm -f "/etc/systemd/system/minimax-${module}.service"
  done
  rm -f /etc/systemd/system/minimax-frontend.service
  rm -f /etc/nginx/conf.d/minimax.conf
  systemctl daemon-reload

  log_info "已删除 systemd 服务 + nginx 配置"
  log_warn "数据保留: $INSTALL_DIR $BACKUP_DIR (如需删除请手动 rm -rf)"
}

# =============== main ===============
case "${1:-}" in
  install)   install_all ;;
  start)     start_services ;;
  stop)      stop_services ;;
  restart)   restart_services ;;
  status)    show_status ;;
  e2e)       e2e_health_check ;;     # V5.12: 一键健康检查
  e2e-full)  e2e_full ;;                  # V5.15: 完整 E2E 测试 (含 JWT/TraceId)
  logs)      show_logs "${2:-}" ;;
  backup)    backup_all ;;
  update)    update_all ;;
  uninstall) uninstall_all ;;
  *)
    echo "用法: $0 {install|start|stop|restart|status|logs|backup|update|uninstall}"
    echo
    echo "  install    一键安装 (Java + Node + MariaDB + 编译 + systemd + nginx)"
    echo "  start      启动所有服务"
    echo "  stop       停止所有服务"
    echo "  restart    重启所有服务"
    echo "  status     查看服务状态"
    echo "  e2e        V5.12 一键 HTTP 健康检查 (13 服务 + nacos + redis + nginx)"
    echo "  logs M     跟踪模块 M 的日志 (auth|chat|gateway|nacos|redis|...)"
    echo "  backup     备份数据库 + jar"
    echo "  update     git pull + 重打包 + 重启"
    echo "  uninstall  卸载服务 (保留数据)"
    exit 1
    ;;
esac