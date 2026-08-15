#!/bin/bash
# =============================================================
# V3.7.14+ JWT secret 一键生成 + 替换脚本
# =============================================================
# 作用: 生成 64 字符 hex (256 bit) HMAC 密钥, 替换所有 14 module 默认值
# 用法: bash scripts/generate-jwt-secret.sh [apply|show|rollback]
#   - show (默认): 仅显示新生成的 secret, 不修改任何文件
#   - apply: 生成 + 替换 14 module 配置
#   - rollback: 恢复备份文件
# =============================================================

set -e

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

BACKEND_ROOT="$(cd "$(dirname "$0")/.." && pwd)/backend"
ENV_FILE="${BACKEND_ROOT}/.env"

# 1. 生成新 secret (64 字符 hex, 256 bit)
generate_secret() {
  if command -v openssl &> /dev/null; then
    openssl rand -hex 32
  elif [ -r /dev/urandom ]; then
    head -c 32 /dev/urandom | xxd -p -c 64
  else
    echo -e "${RED}❌ 需要 openssl 或 /dev/urandom${NC}" >&2
    exit 1
  fi
}

# 2. 备份
backup_files() {
  echo -e "${YELLOW}📦 备份现有配置...${NC}"
  local backup_dir="${BACKEND_ROOT}/.backup-$(date +%Y%m%d-%H%M%S)"
  mkdir -p "$backup_dir"
  cp -r "$BACKEND_ROOT/minimax-common/src/main/resources/application-common.yml" "$backup_dir/" 2>/dev/null || true
  cp -r "$BACKEND_ROOT/minimax-auth/src/main/java/com/minimax/auth/jwt/JwtProperties.java" "$backup_dir/" 2>/dev/null || true
  echo "  备份目录: $backup_dir"
}

# 3. 替换 application-common.yml
apply_common() {
  local secret="$1"
  local yml="${BACKEND_ROOT}/minimax-common/src/main/resources/application-common.yml"
  if [ ! -f "$yml" ]; then
    echo -e "${RED}❌ 找不到 $yml${NC}" >&2
    return 1
  fi
  # 替换 secret 默认值 (a9b33d... 或 7add49...)
  sed -i "s|secret: \\\${MINIMAX_JWT_SECRET:[a-f0-9]\{64\}}|secret: \${MINIMAX_JWT_SECRET:${secret}}|" "$yml"
  echo -e "${GREEN}  ✓ $yml${NC}"
}

# 4. 替换 JwtProperties.java
apply_properties() {
  local secret="$1"
  local props="${BACKEND_ROOT}/minimax-auth/src/main/java/com/minimax/auth/jwt/JwtProperties.java"
  if [ ! -f "$props" ]; then
    return 0  # V3.7.14+ 已删 default
  fi
  # 不再使用 - V3.7.14+ 删了 default
  echo -e "${GREEN}  ✓ $props (无 default, 跳过)${NC}"
}

# 5. 写 .env
write_env() {
  local secret="$1"
  cat > "$ENV_FILE" << EOF
# =============================================================
# JWT Secret (V3.7.14+ generate-jwt-secret.sh 生成)
# 64 字符 hex / 256 bit / HS256
# =============================================================
MINIMAX_JWT_SECRET=${secret}
MINIMAX_COMMON_SECRET=${secret}
EOF
  echo -e "${GREEN}  ✓ $ENV_FILE${NC}"
}

# 6. 主流程
case "${1:-show}" in
  show)
    NEW=$(generate_secret)
    echo ""
    echo -e "${YELLOW}新生成的 JWT Secret (64 字符 hex / 256 bit):${NC}"
    echo ""
    echo "  $NEW"
    echo ""
    echo -e "用 MINIMAX_JWT_SECRET 环境变量覆盖 (生产推荐)"
    echo ""
    echo "应用到所有 14 module:"
    echo "  bash scripts/generate-jwt-secret.sh apply"
    echo ""
    ;;
  apply)
    echo -e "${YELLOW}========================================${NC}"
    echo -e "${YELLOW}  V3.7.14+ JWT Secret 一键替换${NC}"
    echo -e "${YELLOW}========================================${NC}"
    NEW=$(generate_secret)
    echo ""
    echo "新 secret: $NEW"
    echo ""
    backup_files
    echo ""
    echo -e "${YELLOW}🔄 替换配置...${NC}"
    apply_common "$NEW"
    apply_properties "$NEW"
    write_env "$NEW"
    echo ""
    echo -e "${GREEN}✅ 完成! 14 module 现在用新 secret${NC}"
    echo ""
    echo -e "${YELLOW}⚠️ 重要: 重新生成后, 所有已签发 token 失效, 用户需重新登录${NC}"
    echo -e "${YELLOW}⚠️ 备份在: ${BACKEND_ROOT}/.backup-*/${NC}"
    echo ""
    ;;
  rollback)
    echo -e "${YELLOW}回滚到最近备份:${NC}"
    LATEST=$(ls -td "${BACKEND_ROOT}"/.backup-*/ 2>/dev/null | head -1)
    if [ -z "$LATEST" ]; then
      echo -e "${RED}❌ 没找到备份${NC}"
      exit 1
    fi
    echo "  $LATEST"
    cp "$LATEST/application-common.yml" "${BACKEND_ROOT}/minimax-common/src/main/resources/application-common.yml"
    [ -f "$LATEST/JwtProperties.java" ] && cp "$LATEST/JwtProperties.java" "${BACKEND_ROOT}/minimax-auth/src/main/java/com/minimax/auth/jwt/JwtProperties.java"
    echo -e "${GREEN}✅ 已回滚${NC}"
    ;;
  *)
    echo "用法: $0 [show|apply|rollback]"
    exit 1
    ;;
esac
