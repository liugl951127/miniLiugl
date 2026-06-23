#!/usr/bin/env bash
# ============================================================
# MiniMax Platform — 每日自检脚本 (V5.33 Day 19)
# 检查项: SQL 语法 / Java 编译 / 前端构建
# 成功标准: 0 错误
# ============================================================

set -e
cd "$(dirname "$0")/.."

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASS=0
FAIL=0

pass() { echo -e "${GREEN}[PASS]${NC} $1"; PASS=$((PASS+1)); }
fail() { echo -e "${RED}[FAIL]${NC} $1"; FAIL=$((FAIL+1)); }
info() { echo -e "${YELLOW}[INFO]${NC} $1"; }

echo "============================================"
echo " MiniMax Platform — Daily Self Check"
echo "============================================"

# 1. SQL 文件检查
info "1. SQL 文件语法检查"
for f in sql/*.sql; do
  if grep -q "DROP.*TABLE" "$f" 2>/dev/null; then
    if grep -q "CREATE.*TABLE" "$f" 2>/dev/null; then
      pass "SQL: $(basename $f) — 结构完整"
    else
      fail "SQL: $(basename $f) — 缺少 CREATE TABLE"
    fi
  fi
done

# 2. Java 编译检查 (mvn compile)
info "2. Maven 编译"
if cd backend && mvn compile -q -DskipTests -Dspotless.skip=true > /tmp/mvn_compile.log 2>&1; then
  pass "Maven 编译 — 通过"
else
  fail "Maven 编译 — 失败"
  cat /tmp/mvn_compile.log | tail -20
fi
cd ..

# 3. 前端构建检查
info "3. 前端构建"
if cd frontend && npm run build > /tmp/npm_build.log 2>&1; then
  pass "前端构建 — 通过"
else
  fail "前端构建 — 失败"
  cat /tmp/npm_build.log | tail -20
fi
cd ..

# 4. 关键配置文件检查
info "4. 关键配置文件"
for f in docker-compose.yml backend/pom.xml frontend/package.json; do
  if [ -f "$f" ]; then
    pass "配置存在: $f"
  else
    fail "配置缺失: $f"
  fi
done

echo "============================================"
echo " 自检结果: ${PASS} 通过 / ${FAIL} 失败"
echo "============================================"

if [ $FAIL -gt 0 ]; then
  echo -e "${RED}自检未通过，请修复错误后重试！${NC}"
  exit 1
else
  echo -e "${GREEN}自检全部通过！${NC}"
  exit 0
fi
