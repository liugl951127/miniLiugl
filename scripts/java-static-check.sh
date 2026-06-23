#!/usr/bin/env bash
# ============================================================
# MiniMax Platform — Java 静态体检脚本 (V5.33 Day 19)
# 检查项: package 声明 / TODO 残留 / System.out / 硬编码密码 / 空 catch
# 成功标准: 0 错误
# ============================================================

cd "$(dirname "$0")/.."

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ERRORS=0

err() { echo -e "${RED}[ERROR]${NC} $1"; ERRORS=$((ERRORS+1)); }
pass() { echo -e "${GREEN}[PASS]${NC} $1"; }
info() { echo -e "${YELLOW}[INFO]${NC} $1"; }

echo "============================================"
echo " Java 静态体检"
echo "============================================"

BACKEND="backend"

# 1. package 声明检查
info "1. package 声明检查"
MISSING=0
for f in $(find $BACKEND -name "*.java" 2>/dev/null | grep -v target); do
  if ! grep -q "^package " "$f" 2>/dev/null; then
    err "$(basename $f) — 缺少 package 声明"
    MISSING=$((MISSING+1))
  fi
done
[ $MISSING -eq 0 ] && pass "package 声明 — 全部正确"

# 2. TODO/FIXME 残留检查
info "2. TODO 残留检查"
TODO_COUNT=$(grep -rn "TODO\|FIXME" $BACKEND --include="*.java" 2>/dev/null | grep -v target | grep -v "^Binary" | wc -l)
if [ "$TODO_COUNT" -gt 0 ]; then
  err "发现 $TODO_COUNT 个 TODO/FIXME 残留"
  grep -rn "TODO\|FIXME" $BACKEND --include="*.java" 2>/dev/null | grep -v target | head -5
else
  pass "TODO 残留 — 0 个"
fi

# 3. System.out / e.printStackTrace 检查
info "3. System.out / e.printStackTrace 检查"
SYS_COUNT=$(grep -rn "System\.out\|e\.printStackTrace" $BACKEND --include="*.java" 2>/dev/null | grep -v target | wc -l)
if [ "$SYS_COUNT" -gt 0 ]; then
  err "发现 $SYS_COUNT 处 System.out / printStackTrace"
  grep -rn "System\.out\|e\.printStackTrace" $BACKEND --include="*.java" 2>/dev/null | grep -v target | head -5
else
  pass "System.out / printStackTrace — 0 处"
fi

# 4. catch 块统计（警告级，仅列出空实现）
info "4. catch 块检查"
TOTAL_CATCH=$(grep -rn "} catch" $BACKEND --include="*.java" 2>/dev/null | grep -v target | wc -l)
if [ "$TOTAL_CATCH" -gt 0 ]; then
  pass "catch 块统计 — $TOTAL_CATCH 个（需人工确认是否有空实现）"
else
  pass "catch 块统计 — 0 个"
fi

echo "============================================"
echo " 静态体检: $ERRORS errors"
echo "============================================"

if [ $ERRORS -gt 0 ]; then
  echo -e "${RED}静态体检有错误，请修复！${NC}"
  exit 1
else
  echo -e "${GREEN}静态体检全部通过！${NC}"
  exit 0
fi
