#!/bin/bash
# =============================================================
# V3.7.15+ JWT secret 一致性检查脚本
# =============================================================
# 作用: 扫描 14 module 所有 application*.yml, 检查 secret 配置
# 用法: bash scripts/check-jwt-secret.sh
# 输出: 表格 + 警告 (不一致 / 默认值 / 缺失)
# =============================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

BACKEND_ROOT="$(cd "$(dirname "$0")/.." && pwd)/backend"
REFERENCE_SECRET="a9b33d29b5e2728699fc02fe5f1aad70d2a0eb95c4313b786005573f6b913478"

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}  V3.7.15+ 14 module secret 一致性检查${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""
printf "%-30s | %-15s | %-12s | %s\n" "MODULE" "FILE" "STATUS" "SECRET"
echo "-------------------------------+-----------------+--------------+--------"

# 14 module
MODULES=(
  "minimax-auth"
  "minimax-gateway"
  "minimax-common"
  "minimax-chat"
  "minimax-agent"
  "minimax-rag"
  "minimax-model"
  "minimax-multimodal"
  "minimax-pipeline"
  "minimax-monitor"
  "minimax-admin"
  "minimax-analytics"
  "minimax-audit"
  "minimax-ai"
)

TOTAL_OK=0
TOTAL_WARN=0

for module in "${MODULES[@]}"; do
  MODULE_DIR="$BACKEND_ROOT/$module"
  if [ ! -d "$MODULE_DIR/src/main/resources" ]; then
    continue
  fi
  
  # 找所有 yml
  for yml in "$MODULE_DIR/src/main/resources/application"*.yml "$MODULE_DIR/src/main/resources/application"*.yaml; do
    [ -f "$yml" ] || continue
    YML_NAME=$(basename "$yml")
    REL_PATH="${module}/src/main/resources/$YML_NAME"
    
    # 找 jwt.secret 段
    SECRET=$(grep -E "^\s*secret:\s*(\\\$\{|'|\"|[a-zA-Z0-9])" "$yml" 2>/dev/null | grep -B 1 "secret" | grep "secret:" | head -1)
    
    if [ -z "$SECRET" ]; then
      # 找 import 段
      IMPORT=$(grep -E "import:.*application-common" "$yml" 2>/dev/null | head -1)
      if [ -n "$IMPORT" ]; then
        # 继承自 common
        printf "%-30s | %-15s | ${GREEN}%-12s${NC} | %s\n" "$module" "$YML_NAME" "✓ import" "继承自 application-common"
        TOTAL_OK=$((TOTAL_OK+1))
        continue
      fi
      # 没 import 也没 secret - 警告
      printf "%-30s | %-15s | ${RED}%-12s${NC} | %s\n" "$module" "$YML_NAME" "❌ MISSING" "无 secret 配置"
      TOTAL_WARN=$((TOTAL_WARN+1))
      continue
    fi
    
    # 检查 secret 值
    if echo "$SECRET" | grep -q "test-secret-key"; then
      printf "%-30s | %-15s | ${YELLOW}%-12s${NC} | %s\n" "$module" "$YML_NAME" "⚠️  test" "test profile (test-secret-key)"
      continue
    fi
    
    if echo "$SECRET" | grep -q "a9b33d29b5e2728699fc02fe5f1aad70d2a0eb95c4313b786005573f6b913478"; then
      printf "%-30s | %-15s | ${GREEN}%-12s${NC} | %s\n" "$module" "$YML_NAME" "✅ OK" "默认 a9b33d29..."
      TOTAL_OK=$((TOTAL_OK+1))
    elif echo "$SECRET" | grep -q "mVsZlpnzIoXX8f"; then
      printf "%-30s | %-15s | ${RED}%-12s${NC} | %s\n" "$module" "$YML_NAME" "❌ INCONSIST" "Base64 (非 hex, 跟 common 不一致)"
      TOTAL_WARN=$((TOTAL_WARN+1))
    else
      printf "%-30s | %-15s | ${YELLOW}%-12s${NC} | %s\n" "$module" "$YML_NAME" "❓ UNKNOWN" "其他值"
    fi
  done
done

echo ""
echo -e "${YELLOW}========================================${NC}"
echo -e "  总结: ${GREEN}✅ OK: $TOTAL_OK${NC} | ${RED}❌ 警告: $TOTAL_WARN${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""
echo "参考 secret: $REFERENCE_SECRET"
echo "统一方法: bash scripts/generate-jwt-secret.sh apply"
echo ""
