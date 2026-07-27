#!/usr/bin/env bash
# MiniMax Platform 自检脚本 (Day 30)
# 检查: SQL 文件数 / Maven 模块 / 前端构建 / 关键文件存在

BASE="/workspace/minimax-platform"
cd "$BASE"

PASS=0
FAIL=0

check() {
    local name="$1"
    local cmd="$2"
    echo -n "  [$name] ... "
    if eval "$cmd" > /dev/null 2>&1; then
        echo "✅"
        PASS=$((PASS+1))
    else
        echo "❌"
        FAIL=$((FAIL+1))
    fi
}

echo "=== MiniMax Platform 自检 ==="
echo ""

# 1. 关键目录
check "backend 目录存在" "[ -d backend ]"
check "frontend 目录存在" "[ -d frontend ]"
check "sql 目录存在" "[ -d sql ]"
check "scripts 目录存在" "[ -d scripts ]"

# 2. SQL 文件
SQL_COUNT=$(find sql -name "*.sql" 2>/dev/null | wc -l)
echo -n "  [SQL 文件数: $SQL_COUNT] ... "
if [ "$SQL_COUNT" -ge 2 ]; then
    echo "✅ (>= 15)"
    PASS=$((PASS+1))
else
    echo "❌ (< 15)"
    FAIL=$((FAIL+1))
fi

# 3. Maven 模块
MOD_COUNT=$(find backend -name "pom.xml" 2>/dev/null | wc -l)
echo -n "  [Maven 模块数: $MOD_COUNT] ... "
if [ "$MOD_COUNT" -ge 14 ]; then
    echo "✅ (>= 14)"
    PASS=$((PASS+1))
else
    echo "❌ (< 14)"
    FAIL=$((FAIL+1))
fi

# 4. 关键后端文件 (Day 30 新增)
check "MultiModelVotingService 存在" "[ -f backend/minimax-ai/src/main/java/com/minimax/ai/intent/MultiModelVotingService.java ]"
check "QueryExpander 存在" "[ -f backend/minimax-rag/src/main/java/com/minimax/rag/service/QueryExpander.java ]"
check "AlertRcaService 存在" "[ -f backend/minimax-monitor/src/main/java/com/minimax/monitor/service/AlertRcaService.java ]"
check "LogAnomalyDetector 存在" "[ -f backend/minimax-monitor/src/main/java/com/minimax/monitor/service/LogAnomalyDetector.java ]"

# 5. 前端 dist 存在
check "前端构建产物存在" "[ -d frontend/dist ]"

# 6. 配置文件
check "pom.xml 存在" "[ -f backend/pom.xml ]"
check "docker-compose.yml 存在" "[ -f docker-compose.yml ]"

echo ""
echo "=== 结果: $PASS passed / $FAIL failed ==="
if [ "$FAIL" -eq 0 ]; then
    echo "✅ 全部通过！"
    exit 0
else
    echo "❌ 有失败项"
    exit 1
fi
