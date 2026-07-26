#!/bin/bash
# =============================================================
# CI check (V3.5.33+)
# 4 项硬性检查:
#   1. schema.sql 禁止 INT/BIGINT/TIMESTAMP DEFAULT ''
#   2. application*.yml 禁止 jdbc:mysql:// (要 jdbc:mariadb://)
#   3. application*.yml 禁止 com.mysql.cj.jdbc.Driver (要 org.mariadb.jdbc.Driver)
# =============================================================
set -e
cd "$(dirname "$0")/.."
ROOT=$(pwd)
echo "═══════════════════════════════════════════════════════════"
echo "  CI check: schema + JDBC URL + Driver 3 项"
echo "═══════════════════════════════════════════════════════════"
echo ""

EXIT=0

# --- Check 1: schema.sql 数字/时间字段 DEFAULT '' ---
echo "--- 1. schema.sql 数字/时间字段禁止 DEFAULT '' ---"
BAD1=$(grep -rn "NOT NULL DEFAULT ''" sql/*.sql 2>/dev/null | \
       grep -E "(INT|BIGINT|TIMESTAMP) NOT NULL DEFAULT ''" | head -5 || true)
if [ -n "$BAD1" ]; then
    echo "  ✗ FAIL: 还有 NOT NULL DEFAULT '' 错位"
    echo "$BAD1"
    EXIT=1
else
    echo "  ✓ PASS"
fi
echo ""

# --- Check 2: JDBC URL 禁止 jdbc:mysql:// ---
echo "--- 2. application*.yml 禁止 jdbc:mysql:// ---"
BAD2=$(grep -rn "jdbc:mysql://" backend/minimax-*/src/main/resources/application*.yml 2>/dev/null | head -5 || true)
if [ -n "$BAD2" ]; then
    echo "  ✗ FAIL: 还有 jdbc:mysql://"
    echo "$BAD2"
    EXIT=1
else
    echo "  ✓ PASS (14 module 全部 jdbc:mariadb://)"
fi
echo ""

# --- Check 3: Driver 禁止 com.mysql.cj.jdbc.Driver / com.mysql.jdbc.Driver ---
echo "--- 3. application*.yml 禁止 com.mysql.*.Driver ---"
BAD3=$(grep -rn -E "com\.mysql\.(cj\.)?jdbc\.Driver" backend/minimax-*/src/main/resources/application*.yml 2>/dev/null | head -5 || true)
if [ -n "$BAD3" ]; then
    echo "  ✗ FAIL: 还有 MySQL driver"
    echo "$BAD3"
    EXIT=1
else
    echo "  ✓ PASS (14 module 全部 org.mariadb.jdbc.Driver)"
fi
echo ""

# --- Check 4: Dockerfile 禁止 V3.5.18 合并前模块残留 ---
echo "--- 4. Dockerfile 禁止 minimax-{memory,prompt,function} 残留 ---"
# V3.5.18: prompt→model, memory→chat, function→pipeline
# 这些目录已删除, Dockerfile 还引用会 build fail
RESIDUAL=0
for old_mod in memory prompt function; do
    HITS=$(grep -rln "minimax-$old_mod" backend/minimax-*/Dockerfile backend/Dockerfile 2>/dev/null || true)
    if [ -n "$HITS" ]; then
        echo "  ✗ FAIL: minimax-$old_mod 残留 in: $HITS"
        RESIDUAL=1
    fi
done
if [ $RESIDUAL -eq 0 ]; then
    echo "  ✓ PASS (15 Dockerfile 全部对齐 (14 module + 1 通用) V3.5.18 module 列表)"
else
    EXIT=1
fi
echo ""

echo "═══════════════════════════════════════════════════════════"
if [ $EXIT -eq 0 ]; then
    echo "  ✓ ALL PASS (4/4)"
else
    echo "  ✗ FAILED (some checks)"
fi
echo "═══════════════════════════════════════════════════════════"
exit $EXIT
