#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - JWT Secret 生成工具 (V2.4)
#
# 用途:
#   生成符合 HS256 规范的强随机 secret
#   替换项目中硬编码的 JWT secret
#   支持环境变量 + 自动备份
#
# 用法:
#   ./deploy-simple/generate-jwt-secret.sh              # 生成新 secret
#   ./deploy-simple/generate-jwt-secret.sh --apply      # 生成并替换所有配置
#   ./deploy-simple/generate-jwt-secret.sh --apply --secret=<64位hex>   # 用指定 secret
#
# 输出:
#   - 32 字节 (256 bit) 随机字节
#   - 64 字符十六进制编码 (符合 HS256 规范)
#   - 包含大小写字母 + 数字
#
# 规范:
#   - HS256 算法要求 secret 至少 256 bit (32 字节)
#   - 推荐 64+ 字节, 增加安全性
#   - 必须用加密随机源 (openssl rand / /dev/urandom)
#   - 不能用容易猜的 (生日, 姓名, 单词)
# =============================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; CYAN='\033[0;36m'; NC='\033[0m'
log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_ok()   { echo -e "${GREEN}[✓]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[!]${NC} $*"; }
log_err()  { echo -e "${RED}[✗]${NC} $*"; }

# 找项目根目录
PROJECT_ROOT=""
for p in /opt/miniLiugl /root/miniLiugl /home/*/miniLiugl "$(pwd)/miniLiugl" "$(pwd)"; do
  if [ -f "$p/docker-compose.yml" ] && grep -q "minimax-gateway" "$p/docker-compose.yml" 2>/dev/null; then
    PROJECT_ROOT="$p"
    break
  fi
done

if [ -z "$PROJECT_ROOT" ]; then
  log_err "找不到 miniLiugl 项目目录"
  exit 1
fi

cd "$PROJECT_ROOT"

# 解析参数
APPLY=0
SECRET=""
for arg in "$@"; do
  case "$arg" in
    --apply)         APPLY=1 ;;
    --secret=*)      SECRET="${arg#*=}" ;;
    -h|--help)
      cat << 'USAGE'
用法: ./generate-jwt-secret.sh [options]

Options:
  (无参数)              生成新 secret 并显示
  --apply              生成并替换所有 application*.yml 中的 jwt.secret
  --secret=<hex>       用指定的 secret (必须是 64 字符 hex)
  -h, --help           显示帮助

示例:
  # 只生成
  ./generate-jwt-secret.sh

  # 生成 + 自动替换所有 yml
  ./generate-jwt-secret.sh --apply

  # 用指定 secret
  ./generate-jwt-secret.sh --apply --secret=a9b33d29b5e2728699fc02fe5f1aad70d2a0eb95c4313b786005573f6b913478

注意:
  - 替换 secret 后, 所有已签发的 token 会失效
  - 用户需要重新登录
  - 多个服务必须用同一个 secret (HS256 跨服务验证)
USAGE
      exit 0
      ;;
  esac
done

# 验证提供的 secret
if [ -n "$SECRET" ]; then
  # 验证格式: 64 个 hex 字符
  if [[ ! "$SECRET" =~ ^[0-9a-fA-F]{64}$ ]]; then
    log_err "secret 格式错误: 必须是 64 字符十六进制 (256 bit)"
    log_err "  示例: a9b33d29b5e2728699fc02fe5f1aad70d2a0eb95c4313b786005573f6b913478"
    exit 1
  fi
else
  # 生成新 secret
  SECRET=$(openssl rand -hex 32)
  if [ -z "$SECRET" ]; then
    log_err "openssl rand 失败, 请检查 openssl 是否安装"
    exit 1
  fi
fi

# 输出信息
log_info "=========================================="
log_info " MiniMax Platform - JWT Secret 生成器"
log_info "=========================================="
echo ""
log_info "生成的 secret (HS256 / 256 bit / 64 hex chars):"
echo ""
echo -e "  ${CYAN}$SECRET${NC}"
echo ""

# 验证强度
log_info "强度检查:"
HEX_LEN=${#SECRET}
log_ok "长度: $HEX_LEN 字符 ($((HEX_LEN * 4)) bit)"
log_ok "格式: 十六进制编码"

# 字符分布
DIGITS=$(echo -n "$SECRET" | grep -oE "[0-9]" | wc -l)
LOWER=$(echo -n "$SECRET" | grep -oE "[a-f]" | wc -l)
UPPER=$(echo -n "$SECRET" | grep -oE "[A-F]" | wc -l)
log_ok "数字: $DIGITS / 小写: $LOWER / 大写: $UPPER"
echo ""

# 替换配置
if [ "$APPLY" = "1" ]; then
  log_info "==== 替换所有 yml 配置 ===="
  
  # 备份原文件
  BACKUP_DIR="$PROJECT_ROOT/.jwt-backup-$(date +%Y%m%d-%H%M%S)"
  mkdir -p "$BACKUP_DIR"
  find "$PROJECT_ROOT/backend" -name "application*.yml" -exec cp {} "$BACKUP_DIR/" \;
  log_ok "已备份到: $BACKUP_DIR"
  echo ""
  
  # 替换所有 application*.yml 中的 jwt.secret
  # 模式: secret: 32+ 个十六进制字符 (旧 secret)
  COUNT=0
  for f in $(find "$PROJECT_ROOT/backend" -name "application*.yml" 2>/dev/null); do
    if grep -q "jwt:" "$f" && grep -q "secret:" "$f"; then
      # 用 sed 替换
      # 匹配 "secret: HEX64" 行
      sed -i.bak -E "s|secret:[[:space:]]*[0-9a-fA-F]{32,}|secret: $SECRET|g" "$f"
      rm -f "$f.bak"
      COUNT=$((COUNT + 1))
      log_ok "  $(realpath --relative-to=$PROJECT_ROOT $f)"
    fi
  done
  
  echo ""
  log_ok "已更新 $COUNT 个配置文件"
  echo ""
  
  # 同时更新 .env.example
  ENV_EXAMPLE="$PROJECT_ROOT/.env.example"
  if [ -f "$ENV_EXAMPLE" ]; then
    sed -i -E "s|JWT_SECRET=.*|JWT_SECRET=$SECRET|" "$ENV_EXAMPLE"
    log_ok "已更新 .env.example"
  fi
  
  # 生成 .env (如果不存在)
  ENV_FILE="$PROJECT_ROOT/.env"
  if [ ! -f "$ENV_FILE" ]; then
    cp "$ENV_EXAMPLE" "$ENV_FILE"
    log_ok "已生成 .env"
  else
    sed -i -E "s|JWT_SECRET=.*|JWT_SECRET=$SECRET|" "$ENV_FILE"
    log_ok "已更新 .env"
  fi
  
  echo ""
  log_warn "⚠️ 重要: 替换 secret 后需要重启服务"
  log_info "  1. 重新构建镜像: ./deploy-simple/docker-deploy.sh rebuild"
  log_info "  2. 或手动重启: docker compose restart gateway auth chat"
  log_info "  3. 用户需要重新登录 (旧 token 失效)"
else
  echo ""
  log_info "用法:"
  log_info "  ./deploy-simple/generate-jwt-secret.sh --apply"
  echo ""
  log_info "或手动复制到配置:"
  log_info "  $SECRET"
fi