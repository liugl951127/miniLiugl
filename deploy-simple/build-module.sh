#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - 单模块打 jar 包 (V1.9)
#
# 用法:
#   chmod +x deploy-simple/build-module.sh
#   ./deploy-simple/build-module.sh auth              # 编译 auth 模块
#   ./deploy-simple/build-module.sh gateway           # 编译 gateway
#   ./deploy-simple/build-module.sh all               # 全部 15 个模块
#   ./deploy-simple/build-module.sh auth --docker     # 编译 + Docker build
#   ./deploy-simple/build-module.sh auth --run        # 编译 + Docker build + 启动
#
# 模块列表:
#   auth / chat / memory / model / rag / function
#   multimodal / agent / monitor / admin / prompt
#   analytics / pipeline / ws / gateway
# =============================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND="$PROJECT_ROOT/backend"
INSTALL_DIR="/opt/minimax/backend"

# 加载 OS 适配层 (CentOS Stream 9 / RHEL 9 识别)
. "$SCRIPT_DIR/os-detect.sh"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_ok()   { echo -e "${GREEN}[✓]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[!]${NC} $*"; }
log_err()  { echo -e "${RED}[✗]${NC} $*"; }

MODULE="${1:-}"
MODE="${2:-}"

if [ -z "$MODULE" ]; then
  echo "用法: $0 <module-name> [--docker|--run|--all]"
  echo ""
  echo "模块: auth, chat, memory, model, rag, function, multimodal,"
  echo "      agent, monitor, admin, prompt, analytics, pipeline, ws, gateway"
  echo "      all  (全部 15 个)"
  exit 1
fi

# ============================================================
# 全部模块列表
# ============================================================
ALL_MODULES="auth chat memory model rag function multimodal agent monitor admin prompt analytics pipeline ws gateway"
MODULE_PORT_MAP="auth:8081 chat:8082 memory:8083 model:8084 rag:8085 function:8086 multimodal:8087 agent:8088 monitor:8089 admin:8090 prompt:8091 analytics:8092 pipeline:8093 ws:8095 gateway:7080"

# ============================================================
# 前置检查
# ============================================================
if [ "$MODULE" != "all" ] && ! echo "$ALL_MODULES" | grep -wq "$MODULE"; then
  log_err "未知模块: $MODULE"
  echo "可选: $ALL_MODULES 或 all"
  exit 1
fi

if ! command -v java &>/dev/null; then
  log_err "JDK 未安装"
  detect_os 2>/dev/null
  case "$OS_ID" in
    centos|rhel|rocky|almalinux)
      echo "    CentOS Stream 9 / RHEL 9: sudo dnf install -y java-17-openjdk-devel"
      ;;
    ubuntu|debian)
      echo "    Ubuntu / Debian:           sudo apt-get install -y openjdk-17-jdk"
      ;;
    *)
      echo "    通用: 装 OpenJDK 17"
      ;;
  esac
  exit 1
fi
log_ok "JDK $(java -version 2>&1 | head -1 | awk -F'"' '{print $2}')"

if ! command -v mvn &>/dev/null; then
  log_err "Maven 未安装"
  detect_os 2>/dev/null
  case "$OS_ID" in
    centos|rhel|rocky|almalinux)
      echo "    CentOS Stream 9 / RHEL 9: sudo dnf install -y maven"
      ;;
    ubuntu|debian)
      echo "    Ubuntu / Debian:           sudo apt-get install -y maven"
      ;;
  esac
  exit 1
fi
log_ok "Maven $(mvn -v | head -1 | awk '{print $3}')"

# ============================================================
# 配阿里云镜像
# ============================================================
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
  log_ok "已配置阿里云 Maven 镜像"
fi

cd "$BACKEND"

# ============================================================
# 构建模块
# ============================================================
build_module() {
  local m="$1"
  log_info "==== 编译 $m ===="
  mvn -B -pl "minimax-common,minimax-$m" -am clean package -DskipTests \
    -Dspotless.check.skip=true -Djacoco.skip=true -q 2>&1 | tail -5
  local jar=$(find "minimax-$m/target" -maxdepth 1 -name "*spring-boot.jar" | head -1)
  if [ -z "$jar" ]; then
    log_err "$m 编译失败, jar 未生成"
    return 1
  fi
  log_ok "$m 编译成功: $jar ($(du -h "$jar" | awk '{print $1}'))"
  echo "$jar"
}

# ============================================================
# Docker build
# ============================================================
docker_build() {
  local m="$1"
  log_info "==== Docker build $m ===="
  cd "$PROJECT_ROOT"
  docker build -f backend/Dockerfile --build-arg "MODULE=$m" -t "minimax-$m:latest" backend/
  log_ok "镜像 minimax-$m:latest 已构建"
}

# ============================================================
# Docker run
# ============================================================
docker_run() {
  local m="$1"
  local port=$(echo "$MODULE_PORT_MAP" | tr ' ' '\n' | grep "^$m:" | cut -d: -f2)
  [ -z "$port" ] && port=8080

  # 找 nacos / mysql / redis 容器是否在运行
  local nacos_host="nacos"
  local mysql_host="mysql"
  local redis_host="redis"

  if ! docker ps --format '{{.Names}}' | grep -q "^minimax-nacos$"; then
    log_warn "nacos 容器未运行, 假设宿主网络 (使用 host.docker.internal)"
    nacos_host="host.docker.internal"
    mysql_host="host.docker.internal"
    redis_host="host.docker.internal"
  fi

  log_info "==== 启动 $m (端口 $port) ===="
  docker run -d \
    --name "minimax-$m" \
    --restart unless-stopped \
    -p "$port:$port" \
    -e "SERVER_PORT=$port" \
    -e "SPRING_PROFILES_ACTIVE=dev" \
    -e "NACOS_HOST=$nacos_host" \
    -e "NACOS_PORT=8848" \
    -e "NACOS_NAMESPACE=public" \
    -e "NACOS_USER=nacos" \
    -e "NACOS_PASS=nacos" \
    -e "MYSQL_HOST=$mysql_host" \
    -e "MYSQL_PORT=3306" \
    -e "MYSQL_DATABASE=minimax_platform" \
    -e "MYSQL_USER=root" \
    -e "MYSQL_PASSWORD=root123456" \
    -e "REDIS_HOST=$redis_host" \
    -e "REDIS_PORT=6379" \
    -e "REDIS_PASSWORD=minimax_redis_2024" \
    "minimax-$m:latest"

  log_ok "$m 已启动 (容器 minimax-$m, 端口 $port)"
  log_info "日志: docker logs -f minimax-$m"
  log_info "注册到 nacos: http://localhost:8848/nacos (nacos/nacos)"
}

# ============================================================
# 单模块模式
# ============================================================
if [ "$MODULE" != "all" ]; then
  build_module "$MODULE"
  case "$MODE" in
    --docker) docker_build "$MODULE" ;;
    --run)    docker_build "$MODULE"; docker_run "$MODULE" ;;
  esac
  exit 0
fi

# ============================================================
# 全部模块模式
# ============================================================
log_info "==== 编译全部 15 个模块 ===="
for m in $ALL_MODULES; do
  build_module "$m" || exit 1
done

case "$MODE" in
  --docker)
    for m in $ALL_MODULES; do
      docker_build "$m" || exit 1
    done
    ;;
  --run)
    for m in $ALL_MODULES; do
      docker_build "$m" || exit 1
      docker_run "$m"
    done
    ;;
esac

log_ok "全部完成"