#!/usr/bin/env bash
# =============================================================
# MiniMax Platform — 一体化部署脚本 (V1)
#
# 流程:
#   1. 编译后端 (mvn install -DskipTests, 13 个 jar)
#   2. 打包前端 (npm run build, 输出 dist/)
#   3. 部署到 /opt/minimax/
#   4. 安装 nginx 配置
#   5. 启动所有服务
#   6. 健康检查
#
# 用法:
#   chmod +x deploy.sh
#   ./deploy.sh                    # 全流程 (编译 + 部署 + 启动)
#   ./deploy.sh --skip-build       # 跳过编译, 仅部署+启动
#   ./deploy.sh --only-frontend    # 仅前端
#   ./deploy.sh --only-backend     # 仅后端
# =============================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
INSTALL_DIR="/opt/minimax"

# 加载 OS 适配层 (CentOS Stream 9 / RHEL 9 识别)
. "$SCRIPT_DIR/os-detect.sh"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${BLUE}[INFO]${NC} $*"; }
log_ok()    { echo -e "${GREEN}[✓]${NC} $*"; }
log_warn()  { echo -e "${YELLOW}[!]${NC} $*"; }
log_err()   { echo -e "${RED}[✗]${NC} $*"; }

# 解析参数
SKIP_BUILD=0
ONLY_FRONTEND=0
ONLY_BACKEND=0
for arg in "$@"; do
  case "$arg" in
    --skip-build)   SKIP_BUILD=1 ;;
    --only-frontend) ONLY_FRONTEND=1 ;;
    --only-backend)  ONLY_BACKEND=1 ;;
    --help|-h)
      echo "用法: $0 [--skip-build] [--only-frontend] [--only-backend]"
      exit 0
      ;;
  esac
done

# ============================================================
# 0. 前置检查
# ============================================================
preflight_check() {
  log_info "==== 前置检查 ===="

  # JDK 17
  if ! command -v java &>/dev/null; then
    log_err "JDK 未安装。安装方式:
    CentOS Stream 9 / RHEL 9: sudo dnf install -y java-17-openjdk-devel
    Ubuntu / Debian:            sudo apt-get install -y openjdk-17-jdk"
    exit 1
  fi
  JAVA_VERSION=$(java -version 2>&1 | awk -F'"' '/version/ {print $2}' | cut -d. -f1)
  if [ "$JAVA_VERSION" -lt 17 ]; then
    log_err "需要 JDK 17+, 当前: $JAVA_VERSION"
    exit 1
  fi
  log_ok "JDK $JAVA_VERSION"

  # Maven
  if ! command -v mvn &>/dev/null; then
    log_err "Maven 未安装。安装方式:
    CentOS Stream 9 / RHEL 9: sudo dnf install -y maven
    Ubuntu / Debian:            sudo apt-get install -y maven"
    exit 1
  fi
  log_ok "Maven $(mvn -v | head -1 | awk '{print $3}')"

  # nginx
  if ! command -v nginx &>/dev/null; then
    log_warn "nginx 未安装, 跳过 nginx 配置部署 (你需要手动安装)"
  else
    log_ok "nginx $(nginx -v 2>&1 | awk -F'/' '{print $2}')"
  fi

  # Node (仅前端需要)
  if [ "$ONLY_BACKEND" -eq 0 ]; then
    if ! command -v node &>/dev/null; then
      log_err "Node.js 未安装, 请安装 18+: https://nodejs.org/"
      exit 1
    fi
    log_ok "Node $(node -v) / npm $(npm -v)"
  fi

  # 端口检查
  log_info "检查关键端口..."
  for port in 80 8080 8081 8848; do
    if ss -tlnp 2>/dev/null | grep -q ":$port "; then
      log_warn "端口 $port 已被占用, 部署后可能冲突"
    fi
  done

  # 磁盘空间 (至少 5G)
  FREE_GB=$(df -BG "$INSTALL_DIR" 2>/dev/null | awk 'NR==2 {print $4}' | tr -d 'G')
  if [ -n "$FREE_GB" ] && [ "$FREE_GB" -lt 5 ]; then
    log_warn "磁盘剩余仅 ${FREE_GB}G, 建议至少 5G"
  fi

  log_ok "前置检查通过"
  echo ""
}

# ============================================================
# 1. 编译后端
# ============================================================
build_backend() {
  log_info "==== 编译后端 (mvn install) ===="
  cd "$PROJECT_ROOT/backend"

  # 设置镜像 (国内加速)
  if [ ! -f ~/.m2/settings.xml ]; then
    mkdir -p ~/.m2
    cat > ~/.m2/settings.xml <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
EOF
    log_ok "已配置阿里云镜像"
  fi

  # 编译 (跳过测试)
  log_info "开始编译, 约 3-5 分钟..."
  mvn clean install -DskipTests -T 2 -q 2>&1 | tail -20
  log_ok "后端编译完成"

  # 收集 jar
  log_info "收集 jar 到 $INSTALL_DIR/backend/ ..."
  mkdir -p "$INSTALL_DIR/backend/logs" "$INSTALL_DIR/backend/pids"

  local count=0
  for module_dir in "$PROJECT_ROOT/backend"/*/; do
    module_name=$(basename "$module_dir")
    # 跳过父 pom 目录和 common
    if [ "$module_name" = "minimax-common" ] || [ "$module_name" = "pom.xml" ]; then
      continue
    fi
    # 找 spring-boot 打出的 jar (排除 .original)
    jar_file=$(find "$module_dir/target" -maxdepth 1 -name "*.jar" ! -name "*.original*" -size +10M 2>/dev/null | head -1)
    if [ -n "$jar_file" ]; then
      cp "$jar_file" "$INSTALL_DIR/backend/minimax-${module_name#minimax-}.jar"
      log_ok "  minimax-${module_name#minimax-}.jar ($(du -h "$jar_file" | awk '{print $1}'))"
      count=$((count + 1))
    fi
  done

  if [ "$count" -lt 13 ]; then
    log_err "只找到 $count 个 jar, 预期至少 13 个"
    exit 1
  fi
  log_ok "后端 jar 收集完成 ($count 个)"
  echo ""
}

# ============================================================
# 2. 打包前端
# ============================================================
build_frontend() {
  log_info "==== 打包前端 (npm run build) ===="
  cd "$PROJECT_ROOT/frontend"

  # 安装依赖 (首次或 package.json 变化)
  if [ ! -d node_modules ] || [ package.json -nt node_modules ]; then
    log_info "安装 npm 依赖..."
    # 配淘宝镜像
    npm config set registry https://registry.npmmirror.com 2>/dev/null || true
    npm install --silent 2>&1 | tail -5
  fi

  # Vite 3+ terser 变可选依赖, 生产模式需要手动装 (用于 JS 压缩)
  if ! grep -q '"terser"' package.json; then
    log_info "安装 terser (vite 生产压缩依赖)..."
    npm install terser --save-dev --silent 2>&1 | tail -3
  fi

  log_info "vite build..."
  npm run build 2>&1 | tail -10

  if [ ! -d dist ]; then
    log_err "前端 dist/ 未生成"
    exit 1
  fi

  # 部署到 nginx html 目录
  log_info "部署 dist/ → $INSTALL_DIR/frontend/dist/"
  rm -rf "$INSTALL_DIR/frontend"
  mkdir -p "$INSTALL_DIR/frontend"
  cp -r dist "$INSTALL_DIR/frontend/"

  log_ok "前端部署完成 ($(du -sh "$INSTALL_DIR/frontend/dist" | awk '{print $1}'))"
  echo ""
}

# ============================================================
# 3. 生成环境变量 / 启动脚本
# ============================================================
generate_scripts() {
  log_info "==== 生成启动脚本 ===="

  # 环境变量
  cat > "$INSTALL_DIR/backend/env.sh" <<'EOF'
#!/usr/bin/env bash
# MiniMax 公共环境变量
export JAVA_HOME=${JAVA_HOME:-$(readlink -f $(which java) | sed 's|/bin/java||')}
export NACOS_SERVER=${NACOS_SERVER:-127.0.0.1:8848}
export MYSQL_URL=${MYSQL_URL:-jdbc:mysql://127.0.0.1:3306/minimax?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true}
export MYSQL_USER=${MYSQL_USER:-root}
export MYSQL_PASS=${MYSQL_PASS:-root123}
export REDIS_HOST=${REDIS_HOST:-127.0.0.1}
export REDIS_PORT=${REDIS_PORT:-6379}
export REDIS_PASS=${REDIS_PASS:-}
# JVM 参数
export JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Dfile.encoding=UTF-8 -Dnacos.server-addr=$NACOS_SERVER"
EOF
  log_ok "env.sh"

  # start-all.sh
  cat > "$INSTALL_DIR/backend/start-all.sh" <<'BASH_EOF'
#!/usr/bin/env bash
# 启动所有服务 (顺序: nacos 依赖 → gateway → 业务)
set -e
cd "$(dirname "$0")"
source ./env.sh

# 服务启动顺序 (依赖关系) — 15 个微服务
SERVICES=(
  "minimax-gateway.jar:8080"
  "minimax-auth.jar:8081"
  "minimax-chat.jar:8082"
  "minimax-memory.jar:8083"
  "minimax-model.jar:8084"
  "minimax-rag.jar:8085"
  "minimax-function.jar:8086"
  "minimax-multimodal.jar:8087"
  "minimax-agent.jar:8088"
  "minimax-monitor.jar:8089"
  "minimax-admin.jar:8090"
  "minimax-prompt.jar:8091"
  "minimax-analytics.jar:8096"
  "minimax-pipeline.jar:8097"
  "minimax-ws.jar:8095"
)

echo "==== 启动 MiniMax Platform 服务 ===="
for entry in "${SERVICES[@]}"; do
  jar="${entry%%:*}"
  port="${entry##*:}"
  name="${jar%.jar}"

  # 跳过已运行的
  if [ -f "pids/${name}.pid" ] && kill -0 "$(cat pids/${name}.pid)" 2>/dev/null; then
    echo "  ⏭  $name 已在运行 (PID $(cat pids/${name}.pid))"
    continue
  fi

  # 启动
  if [ ! -f "$jar" ]; then
    echo "  ✗ $jar 不存在, 跳过"
    continue
  fi
  SERVER_PORT=$port nohup $JAVA_HOME/bin/java $JAVA_OPTS -jar "$jar" \
    > "logs/${name}.log" 2>&1 &
  echo $! > "pids/${name}.pid"
  echo "  ✓ $name (端口 $port, PID $!)"
  sleep 3  # 错开启动, 避免 Nacos 雪崩
done

echo ""
echo "==== 等待服务就绪 ===="
sleep 15
echo ""
echo "==== 状态 ===="
for entry in "${SERVICES[@]}"; do
  jar="${entry%%:*}"
  port="${entry##*:}"
  name="${jar%.jar}"
  if [ -f "pids/${name}.pid" ] && kill -0 "$(cat pids/${name}.pid)" 2>/dev/null; then
    if curl -sf -o /dev/null "http://127.0.0.1:${port}/actuator/health" 2>/dev/null; then
      echo "  ✓ $name (:$port) UP"
    else
      echo "  ⏳ $name (:$port) 启动中"
    fi
  else
    echo "  ✗ $name (:$port) DOWN"
  fi
done
echo ""
echo "访问: http://localhost"
BASH_EOF
  chmod +x "$INSTALL_DIR/backend/start-all.sh"

  # stop-all.sh
  cat > "$INSTALL_DIR/backend/stop-all.sh" <<'BASH_EOF'
#!/usr/bin/env bash
# 停止所有服务
cd "$(dirname "$0")"

echo "==== 停止 MiniMax Platform 服务 ===="
for pid_file in pids/*.pid; do
  [ -f "$pid_file" ] || continue
  name=$(basename "$pid_file" .pid)
  pid=$(cat "$pid_file")
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid"
    echo "  ✓ $name (PID $pid) 已停止"
  else
    echo "  ⏭  $name 未运行"
  fi
  rm -f "$pid_file"
done
echo ""
echo "全部已停止"
BASH_EOF
  chmod +x "$INSTALL_DIR/backend/stop-all.sh"

  # restart.sh
  cat > "$INSTALL_DIR/backend/restart.sh" <<'BASH_EOF'
#!/usr/bin/env bash
# 重启单个服务: ./restart.sh minimax-chat
cd "$(dirname "$0")"
NAME=$1
if [ -z "$NAME" ]; then
  echo "用法: $0 <service-name>"
  echo "示例: $0 minimax-chat"
  ls pids/ | sed 's/.pid$//'
  exit 1
fi
PID_FILE="pids/${NAME}.pid"
JAR_FILE="${NAME}.jar"

# 找端口
PORT=$(grep -oP "(?<=${NAME}:)\d+" ../start-port-map.txt 2>/dev/null || echo "")
# 简化: 从 jar 名推断
case "$NAME" in
  *gateway*) PORT=8080 ;; *auth*) PORT=8081 ;; *chat*) PORT=8082 ;;
  *memory*) PORT=8083 ;; *model*) PORT=8084 ;; *rag*) PORT=8085 ;;
  *function*) PORT=8086 ;; *multimodal*) PORT=8087 ;; *agent*) PORT=8088 ;;
  *monitor*) PORT=8089 ;; *admin*) PORT=8090 ;; *ws*) PORT=8095 ;;
esac

if [ -f "$PID_FILE" ]; then
  PID=$(cat "$PID_FILE")
  kill "$PID" 2>/dev/null && echo "✓ 已停止 $NAME (PID $PID)"
  rm -f "$PID_FILE"
fi

source ./env.sh
SERVER_PORT=$PORT nohup $JAVA_HOME/bin/java $JAVA_OPTS -jar "$JAR_FILE" \
  > "logs/${NAME}.log" 2>&1 &
echo $! > "$PID_FILE"
echo "✓ 已启动 $NAME (端口 $PORT, PID $!)"
BASH_EOF
  chmod +x "$INSTALL_DIR/backend/restart.sh"

  # status.sh
  cat > "$INSTALL_DIR/backend/status.sh" <<'BASH_EOF'
#!/usr/bin/env bash
# 查看所有服务状态
cd "$(dirname "$0")"
echo "==== MiniMax Platform 服务状态 ===="
echo ""
printf "%-25s %-8s %-8s %s\n" "服务" "端口" "状态" "PID"
printf "%-25s %-8s %-8s %s\n" "----" "----" "----" "---"

for pid_file in pids/*.pid 2>/dev/null; do
  [ -f "$pid_file" ] || continue
  name=$(basename "$pid_file" .pid)
  pid=$(cat "$pid_file")
  if kill -0 "$pid" 2>/dev/null; then
    # 推断端口
    case "$name" in
      *gateway*) PORT=8080 ;; *auth*) PORT=8081 ;; *chat*) PORT=8082 ;;
      *memory*) PORT=8083 ;; *model*) PORT=8084 ;; *rag*) PORT=8085 ;;
      *function*) PORT=8086 ;; *multimodal*) PORT=8087 ;; *agent*) PORT=8088 ;;
      *monitor*) PORT=8089 ;; *admin*) PORT=8090 ;; *ws*) PORT=8095 ;;
    esac
    # 健康检查
    if curl -sf -o /dev/null "http://127.0.0.1:${PORT}/actuator/health" 2>/dev/null; then
      STATUS="✓ UP"
    else
      STATUS="⏳ STARTING"
    fi
    printf "%-25s %-8s %-8s %s\n" "$name" "$PORT" "$STATUS" "$pid"
  else
    printf "%-25s %-8s %-8s %s\n" "$name" "-" "✗ DOWN" "-"
    rm -f "$pid_file"
  fi
done
echo ""
echo "前端: /opt/minimax/frontend/dist ($(du -sh /opt/minimax/frontend/dist 2>/dev/null | awk '{print $1}'))"
echo "nginx: $(systemctl is-active nginx 2>/dev/null || echo 'unknown')"
BASH_EOF
  chmod +x "$INSTALL_DIR/backend/status.sh"

  log_ok "启动脚本已生成 (start-all.sh / stop-all.sh / restart.sh / status.sh)"
}

# ============================================================
# 4. 安装 nginx 配置
# ============================================================
install_nginx() {
  log_info "==== 安装 nginx 配置 ===="

  if ! command -v nginx &>/dev/null; then
    log_warn "nginx 未安装, 跳过"
    return
  fi

  # 备份原配置
  NGINX_CONF_DIR="/etc/nginx/conf.d"
  if [ -f "$NGINX_CONF_DIR/minimax.conf" ]; then
    cp "$NGINX_CONF_DIR/minimax.conf" "$NGINX_CONF_DIR/minimax.conf.bak.$(date +%Y%m%d%H%M%S)"
    log_warn "已备份旧配置"
  fi

  # 复制
  cp "$SCRIPT_DIR/nginx-minimax-80.conf" "$NGINX_CONF_DIR/minimax.conf"

  # 如果 nginx 主配置 include conf.d, 启用
  if ! grep -q "include.*conf.d" /etc/nginx/nginx.conf 2>/dev/null; then
    log_warn "/etc/nginx/nginx.conf 未包含 conf.d, 需要手动添加"
  fi

  # 测试配置
  if nginx -t 2>&1 | grep -q "successful"; then
    log_ok "nginx 配置语法 OK"
    systemctl reload nginx 2>/dev/null && log_ok "nginx 已重载" || log_warn "请手动: sudo systemctl reload nginx"
  else
    log_err "nginx 配置语法错误"
    nginx -t
    exit 1
  fi
  echo ""
}

# ============================================================
# 5. 启动服务
# ============================================================
start_services() {
  log_info "==== 启动所有服务 ===="
  "$INSTALL_DIR/backend/start-all.sh"
  echo ""
}

# ============================================================
# 6. 健康检查
# ============================================================
final_check() {
  log_info "==== 最终检查 ===="
  sleep 5

  # nginx
  if curl -sf -o /dev/null http://127.0.0.1/ 2>/dev/null; then
    log_ok "前端可访问: http://127.0.0.1/"
  else
    log_warn "前端未就绪, 检查 nginx + dist/"
  fi

  # gateway
  if curl -sf -o /dev/null http://127.0.0.1/api/v1/auth/captcha 2>/dev/null; then
    log_ok "网关 + auth 可访问: http://127.0.0.1/api/v1/auth/captcha"
  elif curl -sf -o /dev/null http://127.0.0.1/actuator/health 2>/dev/null; then
    log_ok "网关可访问: http://127.0.0.1/actuator/health"
  else
    log_warn "网关未就绪, 等待服务启动..."
  fi

  echo ""
  echo "=========================================="
  echo "  🎉 MiniMax Platform 部署完成"
  echo "=========================================="
  echo ""
  echo "  访问: http://$(hostname -I | awk '{print $1}')/"
  echo "  默认账号: admin / admin123"
  echo ""
  echo "  常用命令:"
  echo "    启动: /opt/minimax/backend/start-all.sh"
  echo "    停止: /opt/minimax/backend/stop-all.sh"
  echo "    状态: /opt/minimax/backend/status.sh"
  echo "    重启: /opt/minimax/backend/restart.sh <service>"
  echo "    日志: tail -f /opt/minimax/backend/logs/minimax-<service>.log"
  echo ""
}

# ============================================================
# 主流程
# ============================================================
main() {
  echo ""
  echo "=========================================="
  echo "  MiniMax Platform 一体化部署 (V1)"
  echo "  单一端口 80, 前后端一体化"
  echo "=========================================="
  echo ""

  preflight_check

  if [ "$ONLY_FRONTEND" -eq 0 ]; then
    if [ "$SKIP_BUILD" -eq 0 ]; then
      build_backend
    else
      log_info "跳过后端编译 (--skip-build)"
      # 仅复制现有 jar
      mkdir -p "$INSTALL_DIR/backend/logs" "$INSTALL_DIR/backend/pids"
      for module_dir in "$PROJECT_ROOT/backend"/*/; do
        module_name=$(basename "$module_dir")
        jar_file=$(find "$module_dir/target" -maxdepth 1 -name "*.jar" ! -name "*.original*" -size +10M 2>/dev/null | head -1)
        if [ -n "$jar_file" ]; then
          cp "$jar_file" "$INSTALL_DIR/backend/minimax-${module_name#minimax-}.jar"
        fi
      done
    fi
    generate_scripts
  fi

  if [ "$ONLY_BACKEND" -eq 0 ]; then
    if [ "$SKIP_BUILD" -eq 0 ]; then
      build_frontend
    else
      log_info "跳过前端编译 (--skip-build)"
    fi
  fi

  install_nginx

  if [ "$ONLY_FRONTEND" -eq 1 ] || [ "$ONLY_BACKEND" -eq 1 ]; then
    log_ok "仅部署, 不启动服务"
  else
    start_services
    final_check
  fi
}

main "$@"