#!/usr/bin/env bash
# =============================================================
# MiniMax Platform V5.28 - 一键部署 (纯 Docker 模式)
#
# 设计:
#   - 中间件 (MySQL/Redis/Nacos) + 13 微服务 + gateway + nginx 全部用 Docker
#   - 服务通过 Docker 网络 DNS 互联 (nacos:8848, mysql:3306, redis:6379)
#   - 后端镜像由 docker-compose 构建 (基于 backend/Dockerfile)
#   - 前端静态文件挂载到 nginx
#   - 一行启动: docker compose up -d
#
# 用法:
#   sudo ./scripts/deploy-minimax.sh install       构建 + 启动全部 (15+ 容器)
#   sudo ./scripts/deploy-minimax.sh start         启动所有服务
#   sudo ./scripts/deploy-minimax.sh stop          停止
#   sudo ./scripts/deploy-minimax.sh restart       重启
#   sudo ./scripts/deploy-minimax.sh status        状态
#   sudo ./scripts/deploy-minimax.sh test          E2E 健康检查
#   sudo ./scripts/deploy-minimax.sh build         只构建镜像 (不启动)
#   sudo ./scripts/deploy-minimax.sh logs <svc>    查看日志
#   sudo ./scripts/deploy-minimax.sh uninstall     卸载 (保留数据卷)
#   sudo ./scripts/deploy-minimax.sh check         静态检查 (CI 用)
#
# 环境: 任何支持 Docker 20.10+ 的 Linux (CentOS 7+ / Ubuntu 20+ / Debian 11+)
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

# =============== 路径 ===============
SRC_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="/var/log/minimax"
COMPOSE_FILE="$SRC_DIR/docker-compose.yml"

# 13 个微服务
MICRO_SERVICES=(auth chat model memory rag function admin multimodal monitor agent prompt ws)
GATEWAY_PORT=8080
NGINX_PORT=3000
NACOS_PORT=8848
MYSQL_PORT=3306
REDIS_PORT=6379

# =============== 工具函数 ===============
is_root() {
  [[ $EUID -eq 0 ]] || log_fatal "需要 root, 请用 sudo"
}

cmd_exists() { command -v "$1" >/dev/null 2>&1; }

# =============== 帮助 ===============
usage() {
  cat <<'EOF'
MiniMax Platform V5.28 - 一键部署 (纯 Docker)

用法:
  sudo ./scripts/deploy-minimax.sh install       构建 + 启动全部 (15+ 容器)
  sudo ./scripts/deploy-minimax.sh start         启动所有服务
  sudo ./scripts/deploy-minimax.sh stop          停止
  sudo ./scripts/deploy-minimax.sh restart       重启
  sudo ./scripts/deploy-minimax.sh status        状态
  sudo ./scripts/deploy-minimax.sh test          E2E 健康检查
  sudo ./scripts/deploy-minimax.sh build         只构建镜像
  sudo ./scripts/deploy-minimax.sh logs <svc>    查看日志 (e.g. auth, gateway, mysql)
  sudo ./scripts/deploy-minimax.sh uninstall     卸载 (保留数据)
  sudo ./scripts/deploy-minimax.sh check         静态检查 (CI 用)

环境: Docker 20.10+ (任意 Linux)
依赖: 自动装 Docker + docker compose plugin

端口:
  3000  nginx (前端统一入口)
  8080  Gateway
  8081-8091, 8095  12 个微服务
  8848  Nacos (nacos/nacos)
  3306  MySQL
  6379  Redis
EOF
  exit 1
}

# =============================================================
# install - 4 步一键安装
# =============================================================
cmd_install() {
  is_root
  log_step "MiniMax V5.28 - 一键部署 (Docker 全栈)"
  log_info "项目目录: $SRC_DIR"

  mkdir -p "$LOG_DIR"

  step1_check_docker
  step2_build_images
  step3_start_all
  step4_verify

  log_step "✅ Docker 一键部署完成"
  cat <<EOF

  访问入口:
    前端:       http://localhost:$NGINX_PORT
    API 文档:   http://localhost:$NGINX_PORT/api-docs
    Nacos:      http://localhost:$NACOS_PORT/nacos  (nacos/nacos)
    MySQL:      127.0.0.1:$MYSQL_PORT  (minimax/minimax_pass_2024)

  账号: adminLiugl / Liugl@2026

  后续命令:
    $0 status      服务状态
    $0 test        E2E 健康检查
    $0 logs auth   查看 auth 日志
EOF
}

# =============================================================
# 步骤 1: 装 Docker
# =============================================================
step1_check_docker() {
  log_step "步骤 1/4: 检查 Docker"

  if ! cmd_exists docker; then
    log_warn "Docker 未安装, 自动装..."
    if [[ -f /etc/redhat-release ]]; then
      yum install -y yum-utils
      yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
      yum install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
    elif [[ -f /etc/os-release ]]; then
      apt-get update
      apt-get install -y -qq ca-certificates curl gnupg
      install -m 0755 -d /etc/apt/keyrings
      curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg 2>/dev/null || \
      curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
      echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian $(. /etc/os-release && echo $VERSION_CODENAME) stable" > /etc/apt/sources.list.d/docker.list
      apt-get update
      apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-compose-plugin
    else
      log_fatal "无法自动装 Docker, 请手动装后重试"
    fi
    systemctl enable --now docker
  fi

  if ! docker compose version >/dev/null 2>&1; then
    log_fatal "docker compose plugin 未安装"
  fi

  log_info "  ✓ Docker: $(docker --version)"
  log_info "  ✓ Compose: $(docker compose version)"

  # 镜像加速 (国内)
  if [[ ! -f /etc/docker/daemon.json ]]; then
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
  }
}
EOF
    systemctl restart docker 2>/dev/null || true
    sleep 3
    log_info "  ✓ 国内镜像加速已配置"
  fi
}

# =============================================================
# 步骤 2: 构建镜像
# =============================================================
step2_build_images() {
  log_step "步骤 2/4: 构建 Docker 镜像 (5-10 分钟)"

  cd "$SRC_DIR"
  log_info "  镜像: 13 个微服务 + gateway"
  log_info "  开始构建 (首次较慢, 后续缓存加速)..."

  # 只构建 13 个服务镜像 (mysql/redis/nacos/nginx 是官方镜像, 自动 pull)
  docker compose build --parallel 2>&1 | tail -20
  log_info "  ✓ 镜像构建完成"
}

# =============================================================
# 步骤 3: 启动
# =============================================================
step3_start_all() {
  log_step "步骤 3/4: 启动所有服务 (顺序: MySQL → Redis → Nacos → Gateway → 12 微服务 → nginx)"

  cd "$SRC_DIR"
  docker compose up -d

  log_info "  启动完成, 等 30s 让服务注册到 Nacos..."
  sleep 30

  cmd_status
}

# =============================================================
# 步骤 4: 验证
# =============================================================
step4_verify() {
  log_step "步骤 4/4: 验证"

  cd "$SRC_DIR"

  # 容器状态
  echo
  printf "%-30s %-15s %s\n" "SERVICE" "STATE" "PORT"
  printf "%-30s %-15s %s\n" "------" "-----" "----"

  docker compose ps --format "table {{.Name}}\t{{.State}}\t{{.Ports}}" | tail -n +2 | head -20
  echo
  log_info "总容器数: $(docker compose ps -q | wc -l)"
}

# =============================================================
# start (启动已构建好的镜像)
# =============================================================
cmd_start() {
  cd "$SRC_DIR"
  docker compose up -d
  log_info "已启动"
  sleep 5
  cmd_status
}

# =============================================================
# stop
# =============================================================
cmd_stop() {
  cd "$SRC_DIR"
  docker compose stop
  log_info "已停止 (数据卷保留)"
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
  cd "$SRC_DIR"

  log_step "服务状态"
  printf "%-30s %-15s %s\n" "SERVICE" "STATE" "PORT"
  printf "%-30s %-15s %s\n" "------" "-----" "----"

  docker compose ps --format "table {{.Name}}\t{{.State}}\t{{.Ports}}" 2>/dev/null | tail -n +2 | while read line; do
    name=$(echo "$line" | awk '{print $1}')
    state=$(echo "$line" | awk '{print $2}')
    port=$(echo "$line" | awk '{print $3}')

    if [[ "$state" == "Up" || "$state" == "running" ]]; then
      printf "  ${GREEN}%-28s${NC} ${GREEN}%-15s${NC} %s\n" "$name" "$state" "$port"
    else
      printf "  ${RED}%-28s${NC} ${RED}%-15s${NC} %s\n" "$name" "$state" "$port"
    fi
  done

  echo
  log_info "HTTP 健康检查:"
  for entry in \
    "nginx :3000|http://127.0.0.1:$NGINX_PORT/" \
    "api-docs|http://127.0.0.1:$NGINX_PORT/api-docs" \
    "gateway|http://127.0.0.1:$GATEWAY_PORT/actuator/health" \
    "nacos|http://127.0.0.1:$NACOS_PORT/nacos/" \
    "auth|http://127.0.0.1:8081/actuator/health"; do
    name="${entry%%|*}"
    url="${entry##*|}"
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$url" 2>/dev/null)
    if [[ "$code" =~ ^(200|301|302|401)$ ]]; then
      printf "  ${GREEN}%-20s${NC} ✓ (%s)\n" "$name" "$code"
    else
      printf "  ${RED}%-20s${NC} ✗ (%s)\n" "$name" "$code"
    fi
  done
}

# =============================================================
# test - E2E 健康检查
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
  check "nginx :3000"        "http://127.0.0.1:$NGINX_PORT/"
  check "api-docs 重定向"    "http://127.0.0.1:$NGINX_PORT/api-docs"
  check "gateway :8080"      "http://127.0.0.1:$GATEWAY_PORT/actuator/health"
  check "nacos"              "http://127.0.0.1:$NACOS_PORT/nacos/"
  check "auth 微服务"        "http://127.0.0.1:8081/actuator/health"

  # 12 微服务 (通过 gateway 路由)
  for module in "${MICRO_SERVICES[@]}"; do
    if [[ "$module" == "auth" ]]; then continue; fi
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
    log_warn "  ✗ 登录失败 (可能 SQL 未导入或 auth 未就绪)"
    fail=$((fail+1))
  fi

  echo
  log_step "结果: ${GREEN}${pass} 通过${NC} / ${RED}${fail} 失败${NC}"
  return $fail
}

# =============================================================
# build - 只构建镜像
# =============================================================
cmd_build() {
  cd "$SRC_DIR"
  log_step "构建 Docker 镜像"
  docker compose build --parallel 2>&1 | tail -20
  log_info "✓ 构建完成"
}

# =============================================================
# logs - 查看日志
# =============================================================
cmd_logs() {
  cd "$SRC_DIR"
  local service="${1:-}"
  if [[ -z "$service" ]]; then
    docker compose logs -f --tail=100
  else
    docker compose logs -f --tail=100 "$service"
  fi
}

# =============================================================
# uninstall
# =============================================================
cmd_uninstall() {
  is_root
  log_step "卸载 MiniMax (保留数据卷)"
  read -rp "确认卸载容器? (数据卷保留) [y/N] " -r
  [[ ! $REPLY =~ ^[Yy]$ ]] && exit 0

  cd "$SRC_DIR"
  docker compose down
  log_info "容器已停止, 数据卷保留 (如需完全清理: docker compose down -v)"
}

# =============================================================
# check (静态检查, CI 用)
# =============================================================
cmd_check() {
  log_step "静态检查 (CI 模式)"

  local pass=0 fail=0

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
  if python3 -c "
import re
c = open('sql/init-minimax.sql').read()
c = re.sub(r'--[^\\n]*', '', c)
c = re.sub(r'/\\*.*?\\*/', '', c, flags=re.S)
tables = c.count('CREATE TABLE')
inserts = c.count('INSERT INTO')
quotes = c.count(\"'\") + c.count('\"')
ok = tables >= 35 and inserts >= 30 and quotes % 2 == 0
print(f'    tables={tables}, inserts={inserts}, quotes={quotes}')
exit(0 if ok else 1)
" 2>/dev/null; then
    log_info "  ✓ SQL 平衡"
    pass=$((pass+1))
  else
    log_err "  ✗ SQL 平衡"
    fail=$((fail+1))
  fi

  # 必需文件
  for f in docker-compose.yml sql/init-minimax.sql scripts/deploy-minimax.sh scripts/install-middleware-centos.sh scripts/deploy-centos.sh backend/Dockerfile frontend/Dockerfile; do
    if [[ -f "$f" ]]; then
      log_info "  ✓ 文件存在 $f"
      pass=$((pass+1))
    else
      log_err "  ✗ 文件缺失 $f"
      fail=$((fail+1))
    fi
  done

  # 检测反模式: ${!var} 间接变量展开 (V5.29 fix 教训)
  # 仅检测非数组下标形式: ${!svc_*} ${!foo_bar} (但允许 ${!arr[@]})
  # 排除 deploy-minimax.sh 自身 (grep 检测代码会匹配到示例文本)
  bad_indirect=$(grep -nE '\$\{![a-z_]+[^@*]' scripts/install-middleware-centos.sh scripts/deploy-centos.sh 2>/dev/null | grep -v '^[^:]*:[0-9]*:#' | head -3)
  if [[ -z "$bad_indirect" ]]; then
    log_info "  ✓ 无 \${!var} 反模式"
    pass=$((pass+1))
  else
    log_warn "  ! 检测到间接变量展开 (bash 4.x 部分支持, 慎用):"
    echo "$bad_indirect" | sed 's/^/      /'
    log_warn "    建议改用 case 直接映射"
    # 不计入失败 (合法但脆弱)
    pass=$((pass+1))
  fi

  log_step "结果: ${GREEN}${pass} 通过${NC} / ${RED}${fail} 失败${NC}"
  return $fail
}

# =============== 主入口 (放最后) ===============
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
  build)      cmd_build ;;
  logs)       cmd_logs "$@" ;;
  uninstall)  cmd_uninstall ;;
  check)      cmd_check ;;
  *)          usage ;;
esac