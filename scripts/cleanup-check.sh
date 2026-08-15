#!/bin/bash
# =============================================================
# V3.7.16+ 一键清理检查脚本
# =============================================================
# 作用: 扫 frontend/backend 无用目录 (dist/coverage/...)
# 用法: bash scripts/cleanup-check.sh [clean|check]
#   - check (默认): 仅扫描, 不删除
#   - clean: 扫 + 删除
# =============================================================

set -e

YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

cd "$(dirname "$0")/.."

# 无用目录列表
DIRS=(
  "frontend/dist"
  "frontend/coverage"
  "frontend/e2e-report"
  "frontend/test-results"
  "frontend/playwright-report"
  "frontend/.vite"
  "frontend/.cache"
  "frontend/.parcel-cache"
  "frontend/.next"
  "frontend/.nuxt"
  "frontend/.output"
  "frontend/.turbo"
  "frontend/.rollup.cache"
)

# 无用文件列表
FILES=(
  "frontend/auto-imports.d.ts"
  "frontend/components.d.ts"
)

TOTAL_SIZE=0
FOUND=()

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}  V3.7.16+ 前端无用文件扫描${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

for d in "${DIRS[@]}"; do
  if [ -e "$d" ]; then
    SIZE=$(du -sh "$d" 2>/dev/null | awk '{print $1}')
    SIZE_KB=$(du -sk "$d" 2>/dev/null | awk '{print $1}')
    printf "  ${RED}%-40s %8s${NC}\n" "$d" "$SIZE"
    FOUND+=("$d")
    TOTAL_SIZE=$((TOTAL_SIZE + SIZE_KB))
  fi
done

for f in "${FILES[@]}"; do
  if [ -e "$f" ]; then
    SIZE=$(ls -la "$f" 2>/dev/null | awk '{print $5}')
    printf "  ${RED}%-40s %8s B${NC}\n" "$f" "$SIZE"
    FOUND+=("$f")
  fi
done

echo ""
TOTAL_MB=$(echo "scale=2; $TOTAL_SIZE/1024" | bc 2>/dev/null || echo "$((TOTAL_SIZE/1024))")
echo -e "发现 ${#FOUND[@]} 个无用目录/文件, 共约 ${RED}${TOTAL_MB}MB${NC}"
echo ""

if [ "${1:-check}" = "clean" ]; then
  echo -e "${YELLOW}🗑️  清理...${NC}"
  for d in "${FOUND[@]}"; do
    rm -rf "$d"
    echo "  ✓ 删除 $d"
  done
  echo ""
  echo -e "${GREEN}✅ 清理完成${NC}"
else
  echo "应用清理: bash scripts/cleanup-check.sh clean"
fi
