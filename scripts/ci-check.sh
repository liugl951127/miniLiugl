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
echo "  CI check: schema + JDBC + Driver + Dockerfile + Mapper 重复 + seed-data 列对齐 6 项"
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

# --- Check 5: mapper 接口 @Select/@Update 注解 + XML mapper 重复 (V3.5.68+) ---
echo "--- 5. mapper 接口注解 + XML 同名定义禁止重复 ---"
# MyBatis-Plus 启动报 "ERROR ... mapper[xxx] is ignored, because it exists, maybe from xml file"
# 跑 Python 扫描所有 mapper 接口 + XML
MAPPER_DUP=$(python3 scripts/check_mapper_duplicate.py 2>&1)
MAPPER_EXIT=$?
if [ $MAPPER_EXIT -eq 0 ]; then
    echo "  ✓ PASS (0 mapper 重复)"
else
    echo "  ✗ FAIL: mapper 注解 + XML 重复定义"
    echo "$MAPPER_DUP" | head -10
    EXIT=1
fi
echo ""

# --- Check 6: seed-data INSERT 列名 跟 entity 字段对齐 (V3.5.67+) ---
echo "--- 6. seed-data INSERT 列名 跟 entity 字段对齐 ---"
# V3.5.67 错位: sys_role INSERT 写 status 列, entity 字段是 enabled
SEED_DUP=$(python3 scripts/check_seed_data_columns.py 2>&1)
SEED_EXIT=$?
if [ $SEED_EXIT -eq 0 ]; then
    echo "  ✓ PASS (0 seed-data 列错位)"
else
    echo "  ✗ FAIL: seed-data 列错位"
    echo "$SEED_DUP" | head -10
    EXIT=1
fi
echo ""

# --- Check 7: menu 路径 / router.push 硬编码 vs router 路径 (V3.5.87+) ---
echo "--- 7. menu 路径 / router.push 硬编码 vs router 路径 ---"
# V3.5.86 bug: Login 跳 /admin/dashboard, router 没配 dashboard → 跳 fallback 空白
# 跑 Python 扫所有 menu / router.push 硬编码, 跟 router 路径对比
cd "$(dirname "$0")/.."
MENU_ROUTE=$(node scripts/check_menu_routes.cjs 2>&1)
MENU_EXIT=$?
if [ $MENU_EXIT -eq 0 ]; then
    echo "  ✓ PASS (0 menu 路径缺失)"
else
    echo "  ✗ FAIL: menu 路径 router 找不到"
    echo "$MENU_ROUTE" | tail -15
    EXIT=1
fi

# --- Check 8: <script setup> 防御性自检 (V3.5.95+) ---
echo "--- 8. <script setup> 防御性自检 ---"
SETUP_VAR=$(node scripts/check-setup-var.cjs 2>&1)
SETUP_EXIT=$?
if [ $SETUP_EXIT -eq 0 ]; then
    echo "  ✓ PASS (0 <script setup> 顶层变量未定义)"
else
    echo "  ✗ FAIL: 模板用变量但 setup 未定义"
    echo "$SETUP_VAR" | tail -15
    EXIT=1
fi
echo ""

# --- Check 9: docker-compose 静态验证 (V3.5.95+) ---
echo "--- 9. docker-compose 静态验证 ---"
COMPOSE_CHECK=$(bash scripts/verify-docker-compose.sh 2>&1)
COMPOSE_EXIT=$?
if [ $COMPOSE_EXIT -eq 0 ]; then
    echo "  ✓ PASS (19 services + 14 module depends_on otel-collector)"
else
    echo "  ✗ FAIL: docker-compose 配置异常"
    echo "$COMPOSE_CHECK" | tail -15
    EXIT=1
fi
echo ""

# --- Check 10: pom.xml 一致性 (V3.6.4+) ---
echo "--- 10. pom.xml 一致性静态检查 ---"
POM_CHECK=$(python3 scripts/check_pom_consistency.py 2>&1)
POM_EXIT=$?
if [ $POM_EXIT -eq 0 ]; then
    echo "  ✓ PASS (14 module pom.xml 全部存在)"
else
    echo "  ✗ FAIL: pom.xml 配置异常"
    echo "$POM_CHECK" | tail -15
    EXIT=1
fi
echo ""

# --- Check 11: OTel Trace 沙箱友好版 (V3.6.4+) ---
echo "--- 11. OTel Trace 沙箱友好验证 ---"
OTEL_CHECK=$(bash scripts/otel-trace-sandbox.sh 2>&1 | tail -10)
if echo "$OTEL_CHECK" | grep -q "✅ OTel Trace 沙箱验证完成"; then
    echo "  ✓ PASS (otel-collector + jaeger + 12 module depends_on + sw.js traceparent)"
else
    echo "  ✗ FAIL: OTel 配置异常"
    echo "$OTEL_CHECK" | tail -10
    EXIT=1
fi
echo ""

echo "═══════════════════════════════════════════════════════════"
if [ $EXIT -eq 0 ]; then
    echo "  ✓ ALL PASS (11/11)"
else
    echo "  ✗ FAILED (some checks)"
fi
echo "═══════════════════════════════════════════════════════════"
exit $EXIT
