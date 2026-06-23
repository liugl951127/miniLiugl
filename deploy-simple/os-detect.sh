#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - OS 探测 + 包管理器适配层
# 自动识别: CentOS Stream 9 / RHEL 9 / Ubuntu 20+ / Debian 11+
# =============================================================

# 日志函数 (定义在这里避免依赖外部脚本)
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_ok()   { echo -e "${GREEN}[✓]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[!]${NC} $*"; }
log_err()  { echo -e "${RED}[✗]${NC} $*"; }

# 探测 OS 家族 + 包管理器
detect_os() {
  if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS_ID="${ID:-unknown}"
    OS_VERSION="${VERSION_ID:-unknown}"
    OS_LIKE="${ID_LIKE:-}"
    OS_PRETTY="${PRETTY_NAME:-$OS_ID}"
  elif [ -f /etc/redhat-release ]; then
    OS_ID="rhel"
    OS_VERSION=$(cat /etc/redhat-release | grep -oE '[0-9]+' | head -1)
    OS_PRETTY=$(cat /etc/redhat-release)
  elif [ -f /etc/debian_version ]; then
    OS_ID="debian"
    OS_VERSION=$(cat /etc/debian_version)
    OS_PRETTY="Debian $OS_VERSION"
  else
    OS_ID="unknown"
    OS_PRETTY="Unknown Linux"
  fi

  # 家族判断
  case "$OS_ID" in
    centos|rhel|rocky|almalinux|fedora|ol)
      OS_FAMILY="rhel"
      PKG_MGR="dnf"
      if ! command -v dnf &>/dev/null; then
        PKG_MGR="yum"     # RHEL 7 兼容
      fi
      ;;
    ubuntu|debian|linuxmint|pop)
      OS_FAMILY="debian"
      PKG_MGR="apt-get"
      ;;
    *)
      # 备用 ID_LIKE 推断
      if echo "$OS_LIKE" | grep -q "rhel\|centos\|fedora"; then
        OS_FAMILY="rhel"
        PKG_MGR="dnf"
      elif echo "$OS_LIKE" | grep -q "debian\|ubuntu"; then
        OS_FAMILY="debian"
        PKG_MGR="apt-get"
      else
        log_err "无法识别操作系统: $OS_PRETTY"
        return 1
      fi
      ;;
  esac
  return 0
}

# 安装包(跨包管理器)
pkg_install() {
  case "$PKG_MGR" in
    dnf|yum)
      $PKG_MGR install -y "$@"
      ;;
    apt-get)
      apt-get update -qq 2>/dev/null || true
      DEBIAN_FRONTEND=noninteractive apt-get install -y "$@"
      ;;
  esac
}

# 启用 + 启动 systemd 服务
systemd_enable_start() {
  local svc="$1"
  systemctl enable "$svc" 2>/dev/null || true
  systemctl start "$svc" 2>/dev/null || true
}

# 防火墙开端口(自动识别 ufw vs firewalld)
firewall_open() {
  local port="$1"
  local proto="${2:-tcp}"

  if command -v firewall-cmd &>/dev/null; then
    # RHEL/CentOS 9 默认 firewalld
    firewall-cmd --permanent --add-port="${port}/${proto}" 2>/dev/null || true
    firewall-cmd --reload 2>/dev/null || true
    log_ok "firewalld: 已开放 ${port}/${proto}"
  elif command -v ufw &>/dev/null; then
    # Ubuntu/Debian
    ufw allow "${port}/${proto}" 2>/dev/null || true
    log_ok "ufw: 已开放 ${port}/${proto}"
  elif command -v iptables &>/dev/null; then
    # 兜底: 直接 iptables
    iptables -I INPUT -p "${proto}" --dport "${port}" -j ACCEPT 2>/dev/null || true
    log_warn "iptables: 已开放 ${port}/${proto} (重启可能失效, 建议装 firewalld)"
  else
    log_warn "未找到防火墙工具, 请手动开放端口 ${port}/${proto}"
  fi
}

# 重启 nginx
nginx_reload() {
  if systemctl reload nginx 2>/dev/null; then
    log_ok "nginx 已重载"
  elif systemctl restart nginx 2>/dev/null; then
    log_ok "nginx 已重启"
  else
    log_err "nginx 重载失败, 请手动检查"
    return 1
  fi
}

# nginx -t 测试
nginx_test() {
  nginx -t 2>&1
}

# 启用 nginx 自启动
nginx_enable() {
  if ! systemctl is-enabled nginx &>/dev/null; then
    systemctl enable nginx 2>/dev/null || true
  fi
}

# 检查 Docker
docker_install_hint() {
  if command -v docker &>/dev/null; then
    return 0
  fi
  log_warn "docker 未安装, V1.9.1 推荐用 docker compose 部署"
  echo "    CentOS Stream 9 安装:"
  echo "      sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo"
  echo "      sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin"
  echo "      sudo systemctl enable --now docker"
  echo "      sudo usermod -aG docker \$USER"
  return 1
}

# SELinux 处理 (CentOS Stream 9 默认开)
selinux_setup() {
  if command -v getenforce &>/dev/null && [ "$(getenforce)" = "Enforcing" ]; then
    log_warn "SELinux 是 Enforcing, 需要调整:"
    # nginx 读证书 + 前端 dist 目录
    setsebool -P httpd_read_user_content 1 2>/dev/null || true
    setsebool -P httpd_can_network_connect 1 2>/dev/null || true
    # 允许 nginx 反代到非标准端口 (7080 / 8081)
    setsebool -P nis_enabled 1 2>/dev/null || true
    # 自定义 dist 目录
    [ -d /opt/minimax ] && chcon -Rt httpd_sys_content_t /opt/minimax 2>/dev/null || true
    log_ok "SELinux 策略已调整 (httpd_read_user_content, httpd_can_network_connect)"
  fi
}

# 输出系统信息
os_info() {
  log_info "==== 系统信息 ===="
  log_ok "OS:      $OS_PRETTY ($OS_ID $OS_VERSION)"
  log_ok "Family:  $OS_FAMILY"
  log_ok "PkgMgr:  $PKG_MGR"
  log_ok "Kernel:  $(uname -r)"
  log_ok "Arch:    $(uname -m)"
  if command -v docker &>/dev/null; then
    log_ok "Docker:  $(docker --version | awk '{print $3}' | tr -d ',')"
  else
    log_warn "Docker:  未安装"
  fi
}