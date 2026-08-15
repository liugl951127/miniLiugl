#!/usr/bin/env bash
# MiniMax Platform 自检脚本 (Day 34)
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

check_build() {
    # npm run build with memory limit; output first/last 5 lines on failure
    # Day 42: 先尝试 workspace node_modules，如果 vite 不存在则用 /tmp 本地构建
    local name="$1"
    local dir="$2"
    echo -n "  [$name] ... "
    if [ -d "$dir/node_modules/.bin/vite" ]; then
        # workspace node_modules 完整，直接构建
        if NODE_OPTIONS="--max-old-space-size=1536" npm run build --prefix "$dir" > /tmp/npm-build.log 2>&1; then
            echo "✅"
            PASS=$((PASS+1))
            return 0
        else
            echo "❌ (构建失败，详见 /tmp/npm-build.log)"
            tail -5 /tmp/npm-build.log
            FAIL=$((FAIL+1))
            return 1
        fi
    elif [ -x "/opt/npm-packages/node_modules/.bin/vite" ]; then
        # Day 42: workspace node_modules 不完整，用 /opt 本地 vite 构建
        echo -n "(使用本地 vite) ... "
        # 同步最新 src 文件
        cp -r "$dir/src"/* /tmp/minimax-frontend/src/ 2>/dev/null
        if [ -f "$dir/vite.config.js" ]; then cp "$dir/vite.config.js" /tmp/minimax-frontend/; fi
        if [ -d "$dir/vite-plugins" ]; then cp -r "$dir/vite-plugins" /tmp/minimax-frontend/; fi
        if [ -d "$dir/public" ]; then cp -r "$dir/public" /tmp/minimax-frontend/; fi
        if [ -f "$dir/index.html" ]; then cp "$dir/index.html" /tmp/minimax-frontend/; fi
        # cd 到 /tmp 目录构建（vite 默认以配置文件所在目录为 root）
        if (cd /tmp/minimax-frontend && /opt/npm-packages/node_modules/.bin/vite build) > /tmp/npm-build.log 2>&1; then
            cp -r /tmp/minimax-frontend/dist "$dir/dist"
            echo "✅"
            PASS=$((PASS+1))
            return 0
        else
            echo "❌ (构建失败，详见 /tmp/npm-build.log)"
            tail -5 /tmp/npm-build.log
            FAIL=$((FAIL+1))
            return 1
        fi
    else
        echo "⚠️ 跳过（vite 不可用，请先 npm install）"
        return 0
    fi
}

echo "=== MiniMax Platform 自检 ==="
echo ""

# 1. 关键目录
check "backend 目录存在" "[ -d backend ]"
check "frontend 目录存在" "[ -d frontend ]"
check "sql 目录存在" "[ -d sql ]"
check "scripts 目录存在" "[ -d scripts ]"

# 2. SQL 文件 (已合并为 minimax-mysql-final.sql)
SQL_COUNT=$(find sql -name "*.sql" 2>/dev/null | wc -l)
echo -n "  [SQL 文件数: $SQL_COUNT] ... "
if [ "$SQL_COUNT" -ge 1 ]; then
    echo "✅ (>= 1)"
    PASS=$((PASS+1))
else
    echo "❌ (< 1)"
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

# 5. 前端 node_modules + npm run build (Day 34: 主动构建)
check "node_modules 存在" "[ -d frontend/node_modules ]"
check_build "npm run build（前端构建）" "frontend"

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
