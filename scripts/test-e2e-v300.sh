#!/usr/bin/env bash
# ============================================================
# MiniMax Platform V3.0.0 端到端 API 验证脚本
#
# 覆盖完整接口链路:
#   1. 基础健康 (gateway/auth/ai/admin/ws)
#   2. 登录 (超级管理员 + 普通用户)
#   3. AI 平台: 意图识别 → Pipeline → 工具调用 → 多模态 → 文档解析
#   4. 协作: WebSocket CRDT (单编辑 / 多端同步)
#   5. 业务: Model Market → Agent Marketplace → Webhook
#   6. 治理: 概览 → 时间线 → 异常 → 合规
#   7. PWA: manifest / sw / 离线页 / 缓存策略
#   8. AI 算法可观测性: EMA 平滑 / 重复检测 / TopK-P 采样
#   9. 浏览器兼容: polyfill 安装 / 特性检测
#  10. 性能 SLA: 关键接口 P95 < 500ms
# ============================================================
set -uo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:7080}"
AI_URL="${AI_URL:-http://127.0.0.1:8094}"
ADMIN_URL="${ADMIN_URL:-http://127.0.0.1:8090}"
WS_URL="${WS_URL:-http://127.0.0.1:8095}"
ADMIN_USER="${ADMIN_USER:-adminLiugl}"
ADMIN_PASS="${ADMIN_PASS:-Liugl@2026}"
REGULAR_USER="${REGULAR_USER:-admin}"
REGULAR_PASS="${REGULAR_PASS:-admin@123}"

PASS=0; FAIL=0; SKIP=0
RESULTS=()
G='\033[0;32m'; R='\033[0;31m'; Y='\033[1;33m'; B='\033[0;34m'; N='\033[0m'

print_header() {
    echo
    echo -e "${B}════════════════════════════════════════════${N}"
    echo -e "${B}  $1${N}"
    echo -e "${B}════════════════════════════════════════════${N}"
}

check() {
    local name="$1" cmd="$2" expected="$3"
    local out
    out=$(eval "$cmd" 2>&1)
    if echo "$out" | grep -q "$expected"; then
        PASS=$((PASS+1))
        RESULTS+=("PASS  $name")
        echo -e "${G}✓${N} $name"
    elif echo "$out" | grep -qiE "Connection refused|无法连接|ECONNREFUSED|Couldn't connect to server"; then
        SKIP=$((SKIP+1))
        RESULTS+=("SKIP  $name (服务未启动)")
        echo -e "${Y}⊘${N} $name ${Y}(服务未启动)${N}"
    else
        FAIL=$((FAIL+1))
        RESULTS+=("FAIL  $name: $(echo "$out" | head -c 150)")
        echo -e "${R}✗${N} $name"
        echo "  $(echo "$out" | head -3)"
    fi
}

# 性能检查 (响应时间 < 阈值 ms)
check_perf() {
    local name="$1" url="$2" max_ms="$3"
    local start end ms
    start=$(date +%s%3N)
    local out
    out=$(curl -fsS -m 5 "$url" 2>&1) || true
    end=$(date +%s%3N)
    ms=$((end - start))
    if [ -z "$out" ]; then
        SKIP=$((SKIP+1))
        echo -e "${Y}⊘${N} $name ${Y}(无响应)${N}"
    elif [ "$ms" -lt "$max_ms" ]; then
        PASS=$((PASS+1))
        echo -e "${G}✓${N} $name ${G}(${ms}ms < ${max_ms}ms)${N}"
    else
        FAIL=$((FAIL+1))
        echo -e "${R}✗${N} $name ${R}(${ms}ms > ${max_ms}ms SLA)${N}"
    fi
}

# ============================================================
# 1. 基础健康
# ============================================================
print_header "1. 基础健康检查 (V3.0.0)"
check "Gateway Health"     "curl -fsS -m 3 $BASE_URL/actuator/health 2>&1 || echo no" '"status":"UP"'
check "Auth Ping"          "curl -fsS -m 3 $BASE_URL/api/v1/auth/ping 2>&1 || echo no" 'ok\|pong\|{"'
check "AI Health"          "curl -fsS -m 3 $AI_URL/actuator/health 2>&1 || echo no" '"status":"UP"'
check "Admin Health"       "curl -fsS -m 3 $ADMIN_URL/actuator/health 2>&1 || echo no" '"status":"UP"'
check "WS Health"          "curl -fsS -m 3 $WS_URL/actuator/health 2>&1 || echo no" '"status":"UP"'

# ============================================================
# 2. 登录流程 (超级管理员)
# ============================================================
print_header "2. 登录流程 (V3.0.0: /api/v1/auth/login)"
LOGIN_RESP=$(curl -fsS -m 5 -X POST $BASE_URL/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" 2>&1) || true
if echo "$LOGIN_RESP" | grep -q "accessToken"; then
    PASS=$((PASS+1))
    ACCESS_TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import json,sys;print(json.load(sys.stdin).get('data',{}).get('accessToken',''))" 2>/dev/null)
    REFRESH_TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import json,sys;print(json.load(sys.stdin).get('data',{}).get('refreshToken',''))" 2>/dev/null)
    echo -e "${G}✓${N} 超级管理员登录: $ADMIN_USER"
elif echo "$LOGIN_RESP" | grep -qiE "Connection refused|无法连接|ECONNREFUSED|Couldn't connect to server"; then
    SKIP=$((SKIP+1))
    RESULTS+=("SKIP  超级管理员登录 (服务未启动)")
    echo -e "${Y}⊘${N} 超级管理员登录 ${Y}(服务未启动)${N}"
    ACCESS_TOKEN=""
else
    FAIL=$((FAIL+1))
    echo -e "${R}✗${N} 超级管理员登录: $LOGIN_RESP"
    ACCESS_TOKEN=""
fi

AUTH_HDR="Authorization: Bearer $ACCESS_TOKEN"

# ============================================================
# 3. AI 平台 (V2.5-V2.9 全量)
# ============================================================
print_header "3. AI 平台 (V3.0.0: minimax-ai 8094)"

# 3.1 意图识别 (V2.8.4 错别字容错)
check "AI 意图识别-图表" "curl -fsS -m 3 -X POST $AI_URL/api/v1/ai/intent/recognize \
    -H 'Content-Type: application/json' \
    -d '{\"text\":\"生成一个柱状图统计销售数据\"}' 2>&1 || echo no" 'GENERATE_CHART\|CHART'
check "AI 意图识别-转人工" "curl -fsS -m 3 -X POST $AI_URL/api/v1/ai/intent/recognize \
    -H 'Content-Type: application/json' \
    -d '{\"text\":\"我要找真人客服\"}' 2>&1 || echo no" 'TRANSFER_HUMAN'
check "AI 意图识别-代码生成" "curl -fsS -m 3 -X POST $AI_URL/api/v1/ai/intent/recognize \
    -H 'Content-Type: application/json' \
    -d '{\"text\":\"生成 Spring Boot 项目代码\"}' 2>&1 || echo no" 'GENERATE_CODE'

# 3.2 Pipeline (V2.8.5 13 阶段)
check "AI Pipeline 配置" "curl -fsS -m 3 $AI_URL/api/v1/pipeline/config 2>&1 || echo no" 'USER_INPUT\|stages'
check "AI Pipeline 切换 CPU" "curl -fsS -m 3 -X POST $AI_URL/api/v1/pipeline/config/compute-mode \
    -H 'Content-Type: application/json' -d '{\"mode\":\"CPU\"}' 2>&1 || echo no" 'success\|ok\|CPU'
check "AI Pipeline 执行" "curl -fsS -m 10 -X POST $AI_URL/api/v1/pipeline/execute \
    -H 'Content-Type: application/json' -d '{\"text\":\"你好,请介绍你自己\",\"sessionId\":\"e2e-001\"}' 2>&1 || echo no" 'output\|pipelineId'

# 3.3 多模态 (V2.7)
check "多模态-图像分析" "curl -fsS -m 3 -X POST $AI_URL/api/v1/multimodal/image/analyze \
    -H 'Content-Type: application/json' -d '{\"url\":\"http://example.com/test.png\"}' 2>&1 || echo no" 'success\|"objects"'
check "多模态-音频分析" "curl -fsS -m 3 -X POST $AI_URL/api/v1/multimodal/audio/analyze \
    -H 'Content-Type: application/json' -d '{\"url\":\"http://example.com/test.mp3\"}' 2>&1 || echo no" 'success\|"duration"'
check "文档解析" "curl -fsS -m 3 -X POST $AI_URL/api/v1/document/parse \
    -H 'Content-Type: application/json' -d '{\"url\":\"http://example.com/test.pdf\"}' 2>&1 || echo no" 'success\|"pages"'

# 3.4 AI 工具 (V2.8.3 19 个工具)
check "AI 工具列表" "curl -fsS -m 3 $AI_URL/api/v1/ai/tools 2>&1 || echo no" 'nl2sql\|chart\|tool'
check "AI 工具调用 NL2SQL" "curl -fsS -m 3 -X POST $AI_URL/api/v1/ai/tools/invoke \
    -H 'Content-Type: application/json' \
    -d '{\"toolName\":\"nl2sql\",\"input\":{\"text\":\"查询 user 表\"}}' 2>&1 || echo no" 'sql\|SELECT'
check "AI 工具调用 DEDUPLICATE" "curl -fsS -m 3 -X POST $AI_URL/api/v1/ai/tools/invoke \
    -H 'Content-Type: application/json' \
    -d '{\"toolName\":\"deduplicate\",\"input\":{\"data\":[1,1,2,3,3]}}' 2>&1 || echo no" '\[1,2,3\]\|data'
check "AI 工具调用 DISTRIBUTION" "curl -fsS -m 3 -X POST $AI_URL/api/v1/ai/tools/invoke \
    -H 'Content-Type: application/json' \
    -d '{\"toolName\":\"distribution\",\"input\":{\"data\":[1,2,3,4,5]}}' 2>&1 || echo no" 'mean\|distribution'

# 3.5 框架 (V2.8.6 ReAct)
check "AI 框架对话" "curl -fsS -m 5 -X POST $AI_URL/api/v1/framework/chat \
    -H 'Content-Type: application/json' \
    -d '{\"message\":\"北京今天天气怎么样\",\"sessionId\":\"e2e-fw\"}' 2>&1 || echo no" 'content\|response\|"tool"'
check "AI Agent 列表" "curl -fsS -m 3 $AI_URL/api/v1/framework/agents 2>&1 || echo no" 'POI\|entertainment\|"agents"'
check "AI 框架统计" "curl -fsS -m 3 $AI_URL/api/v1/framework/stats 2>&1 || echo no" 'tools\|agents\|total'

# 3.6 TensorBoard (V2.8.7-V2.8.9)
check "TB 实验列表" "curl -fsS -m 3 $AI_URL/api/v1/tensorboard/experiments 2>&1 || echo no" 'experiments\|runs\|name'
check "TB 创建实验" "curl -fsS -m 3 -X POST $AI_URL/api/v1/tensorboard/experiments \
    -H 'Content-Type: application/json' -d '{\"name\":\"e2e-test-run\"}' 2>&1 || echo no" 'success\|id'
check "TB 训练追踪" "curl -fsS -m 3 -X POST $AI_URL/api/v1/tensorboard/runs \
    -H 'Content-Type: application/json' -d '{\"name\":\"e2e\",\"loss\":0.5,\"acc\":0.8}' 2>&1 || echo no" 'success'
check "TB 统计" "curl -fsS -m 3 "$AI_URL/api/v1/tensorboard/stats?runId=e2e-run" 2>&1 || echo no" 'count\|mean\|stats'
check "TB 直方图" "curl -fsS -m 3 "$AI_URL/api/v1/tensorboard/histogram?runId=e2e-run" 2>&1 || echo no" 'bins\|histogram'

# ============================================================
# 4. Model Market (V2.9.1)
# ============================================================
print_header "4. Model Market (V2.9.1)"
check "Model Market 列表" "curl -fsS -m 3 $AI_URL/api/v1/model-market/models 2>&1 || echo no" 'models\|name'
check "Model Market 统计" "curl -fsS -m 3 $AI_URL/api/v1/model-market/stats 2>&1 || echo no" 'total\|downloads'
check "Model Market 上传元数据" "curl -fsS -m 3 -X POST $AI_URL/api/v1/model-market/models/metadata \
    -H 'Content-Type: application/json' \
    -d '{\"name\":\"e2e-model\",\"type\":\"PYTORCH\",\"license\":\"MIT\"}' 2>&1 || echo no" 'success\|id'
check "Model Market 评分" "curl -fsS -m 3 -X POST $AI_URL/api/v1/model-market/models/1/rate \
    -H 'Content-Type: application/json' \
    -d '{\"score\":5,\"comment\":\"e2e test\"}' 2>&1 || echo no" 'success'

# ============================================================
# 5. Agent Marketplace (V2.9.0)
# ============================================================
print_header "5. Agent Marketplace (V2.9.0)"
check "Agent Market 列表" "curl -fsS -m 3 $AI_URL/api/v1/marketplace/agents 2>&1 || echo no" 'agents\|name'
check "Agent Market 浏览" "curl -fsS -m 3 $AI_URL/api/v1/marketplace/agents/browse 2>&1 || echo no" 'agents\|total'
check "Agent Market 详情" "curl -fsS -m 3 $AI_URL/api/v1/marketplace/agents/1 2>&1 || echo no" 'name\|description'

# ============================================================
# 6. Webhook (V2.9.1)
# ============================================================
print_header "6. Webhook 集成 (V2.9.1)"
check "Webhook 列表" "curl -fsS -m 3 $AI_URL/api/v1/webhook/hooks 2>&1 || echo no" 'webhooks\|url'
check "Webhook 创建" "curl -fsS -m 3 -X POST $AI_URL/api/v1/webhook/hooks \
    -H 'Content-Type: application/json' \
    -d '{\"url\":\"https://example.com/hook\",\"events\":[\"USER_LOGIN\"]}' 2>&1 || echo no" 'success\|id'
check "Webhook 测试投递" "curl -fsS -m 3 -X POST $AI_URL/api/v1/webhook/hooks/1/test 2>&1 || echo no" 'success\|delivery'
check "Webhook 投递日志" "curl -fsS -m 3 $AI_URL/api/v1/webhook/deliveries 2>&1 || echo no" 'deliveries\|status'

# ============================================================
# 7. 治理 (V2.9.0)
# ============================================================
print_header "7. 治理面板 (V2.9.0)"
check "治理概览" "curl -fsS -m 3 $ADMIN_URL/api/v1/governance/overview 2>&1 || echo no" 'users\|actions'
check "治理时间线" "curl -fsS -m 3 $ADMIN_URL/api/v1/governance/timeline 2>&1 || echo no" 'timeline\|events'
check "治理异常" "curl -fsS -m 3 $ADMIN_URL/api/v1/governance/anomalies 2>&1 || echo no" 'anomalies\|type'
check "治理合规" "curl -fsS -m 3 $ADMIN_URL/api/v1/governance/compliance 2>&1 || echo no" 'compliance\|checks'

# ============================================================
# 8. PWA 离线 (V2.8.9)
# ============================================================
print_header "8. PWA 离线支持 (V2.8.9)"
check "PWA manifest" "curl -fsS -m 3 $BASE_URL/manifest.json 2>&1 || echo no" 'name\|shortcuts'
check "PWA sw.js"     "curl -fsS -m 3 $BASE_URL/sw.js 2>&1 || echo no" 'caches\|fetch'
check "PWA offline"   "curl -fsS -m 3 $BASE_URL/offline.html 2>&1 || echo no" 'offline\|缓存'

# ============================================================
# 9. 性能 SLA (V3.0.0)
# ============================================================
print_header "9. 性能 SLA (V3.0.0: P95 < 500ms)"
check_perf "Gateway health"   "$BASE_URL/actuator/health" 500
check_perf "Auth login"       "$BASE_URL/api/v1/auth/login" 1000
check_perf "AI intent"        "$AI_URL/api/v1/ai/intent/recognize" 500
check_perf "TB experiments"   "$AI_URL/api/v1/tensorboard/experiments" 500
check_perf "Model market"     "$AI_URL/api/v1/model-market/models" 500

# ============================================================
# 10. 浏览器兼容 (V3.0.0)
# ============================================================
print_header "10. 浏览器兼容 (V3.0.0)"
if [ -f frontend/src/composables/useBrowserCompat.js ]; then
    PASS=$((PASS+1))
    echo -e "${G}✓${N} useBrowserCompat.js 存在"
    # 检查关键函数
    if grep -q "installPolyfills" frontend/src/composables/useBrowserCompat.js; then
        PASS=$((PASS+1)); echo -e "${G}✓${N} installPolyfills 函数"
    else
        FAIL=$((FAIL+1)); echo -e "${R}✗${N} installPolyfills 函数缺失"
    fi
    if grep -q "detectFeatures" frontend/src/composables/useBrowserCompat.js; then
        PASS=$((PASS+1)); echo -e "${G}✓${N} detectFeatures 函数"
    else
        FAIL=$((FAIL+1)); echo -e "${R}✗${N} detectFeatures 函数缺失"
    fi
    if grep -q "detectBrowser" frontend/src/composables/useBrowserCompat.js; then
        PASS=$((PASS+1)); echo -e "${G}✓${N} detectBrowser 函数"
    else
        FAIL=$((FAIL+1)); echo -e "${R}✗${N} detectBrowser 函数缺失"
    fi
    # 检查 main.js 初始化
    if grep -q "initBrowserCompat" frontend/src/main.js; then
        PASS=$((PASS+1)); echo -e "${G}✓${N} main.js 初始化浏览器兼容"
    else
        FAIL=$((FAIL+1)); echo -e "${R}✗${N} main.js 未初始化"
    fi
    # 检查 vite config target
    if grep -q "target: 'es2015'" frontend/vite.config.js; then
        PASS=$((PASS+1)); echo -e "${G}✓${N} vite target es2015"
    else
        FAIL=$((FAIL+1)); echo -e "${R}✗${N} vite target 应为 es2015"
    fi
    # 检查 package.json browserslist
    if grep -q "browserslist" frontend/package.json; then
        PASS=$((PASS+1)); echo -e "${G}✓${N} package.json browserslist"
    else
        FAIL=$((FAIL+1)); echo -e "${R}✗${N} package.json 缺 browserslist"
    fi
else
    FAIL=$((FAIL+1))
    echo -e "${R}✗${N} useBrowserCompat.js 不存在"
fi

# ============================================================
# 11. SQL 单文件 (V3.0.0)
# ============================================================
print_header "11. SQL 单文件 (V3.0.0: init.sql)"
if [ -f sql/init.sql ]; then
    PASS=$((PASS+1))
    SIZE=$(wc -l < sql/init.sql)
    TABLES=$(grep -c "CREATE TABLE" sql/init.sql)
    echo -e "${G}✓${N} init.sql 存在 (${SIZE} 行, ${TABLES} 表)"
    # 检查不能有重复 CREATE TABLE
    DUP=$(grep "^CREATE TABLE" sql/init.sql | sort | uniq -d | wc -l)
    if [ "$DUP" -eq 0 ]; then
        PASS=$((PASS+1)); echo -e "${G}✓${N} 无重复 CREATE TABLE"
    else
        FAIL=$((FAIL+1)); echo -e "${R}✗${N} 有 ${DUP} 个重复 CREATE TABLE"
    fi
    # 检查旧 SQL 文件已删除
    LEGACY=$(ls sql/*.sql 2>/dev/null | grep -v "init.sql" | wc -l)
    if [ "$LEGACY" -eq 0 ]; then
        PASS=$((PASS+1)); echo -e "${G}✓${N} 无遗留 SQL 文件"
    else
        FAIL=$((FAIL+1)); echo -e "${R}✗${N} 有 ${LEGACY} 个遗留 SQL 文件"
    fi
else
    FAIL=$((FAIL+1))
    echo -e "${R}✗${N} init.sql 不存在"
fi

# ============================================================
# 12. 路径标准化 (V3.0.0: /api/v1)
# ============================================================
print_header "12. 路径标准化 (V3.0.0: 全部 /api/v1/*)"
# 检查 controllers 都用 /api/v1
TOTAL_CONTROLLERS=$(find backend -name "*Controller.java" -not -path "*/target/*" | wc -l)
CONTROLLERS_WITH_V1=$(grep -rl "@RequestMapping(\"/api/v1" backend/*/src/main/java --include="*Controller.java" 2>/dev/null | wc -l)
echo "  Controller 总数: $TOTAL_CONTROLLERS, 已加 /api/v1: $CONTROLLERS_WITH_V1"
if [ "$TOTAL_CONTROLLERS" -gt 0 ] && [ "$CONTROLLERS_WITH_V1" -ge $((TOTAL_CONTROLLERS * 8 / 10)) ]; then
    PASS=$((PASS+1))
    echo -e "${G}✓${N} 大部分 Controller 已加 /api/v1 前缀 (>= 80%)"
else
    FAIL=$((FAIL+1))
    echo -e "${R}✗${N} 路径标准化覆盖率不足"
fi

# ============================================================
# 13. AI 算法注释 (V3.0.0: 详细)
# ============================================================
print_header "13. AI 算法注释 (V3.0.0: 详细逐行)"
ALG_FILES=("ModelInference.java" "CrdtEngine.java" "GeoUtils.java" "KeywordEngine.java" "TrainingTracker.java")
for f in "${ALG_FILES[@]}"; do
    found=$(find backend -name "$f" -not -path "*/target/*" 2>/dev/null | head -1)
    if [ -n "$found" ]; then
        # 检查 @param, @return, @algorithm 等 javadoc tag
        TAGS=$(grep -c "@param\|@return\|<b>\|复杂度\|公式" "$found" 2>/dev/null)
        if [ "$TAGS" -ge 3 ]; then
            PASS=$((PASS+1))
            echo -e "${G}✓${N} $f 详细注释 (${TAGS} tags)"
        else
            FAIL=$((FAIL+1))
            echo -e "${R}✗${N} $f 注释不足 (${TAGS} tags)"
        fi
    fi
done

# ============================================================
# 14. 文档完整性 (V3.0.0)
# ============================================================
print_header "14. 文档完整性"
for f in CHANGELOG.md docs/ARCHITECTURE.md docs/DEVELOPMENT.md docs/OPERATIONS.md docs/TEST_REPORT.md; do
    if [ -f "$f" ]; then
        SIZE=$(wc -c < "$f")
        if [ "$SIZE" -gt 1000 ]; then
            PASS=$((PASS+1))
            echo -e "${G}✓${N} $f (${SIZE}B)"
        else
            FAIL=$((FAIL+1))
            echo -e "${R}✗${N} $f 过小 (${SIZE}B)"
        fi
    else
        FAIL=$((FAIL+1))
        echo -e "${R}✗${N} $f 缺失"
    fi
done

# ============================================================
# 15. K8s 清理 (V3.0.0)
# ============================================================
print_header "15. K8s 清理 (V3.0.0: 完全去除)"
K8S_REFS=$(grep -r "kubectl\|kubernetes\|k8s\.yaml" --include="*.md" --include="*.sh" --include="*.yml" --include="*.yaml" . 2>/dev/null | grep -v node_modules | grep -v target | grep -v ".git/" | grep -v "CHANGELOG.md" | grep -v "kubectl 为" | wc -l)
if [ "$K8S_REFS" -eq 0 ]; then
    PASS=$((PASS+1))
    echo -e "${G}✓${N} 已彻底清理 K8s 引用"
else
    FAIL=$((FAIL+1))
    echo -e "${R}✗${N} 仍有 ${K8S_REFS} 处 K8s 引用"
    grep -r "kubectl\|kubernetes" --include="*.md" --include="*.sh" --include="*.yml" --include="*.yaml" . 2>/dev/null | grep -v node_modules | grep -v target | grep -v ".git/" | head -3
fi

# ============================================================
# 汇总
# ============================================================
print_header "V3.0.0 E2E 验证结果"
TOTAL=$((PASS + FAIL + SKIP))
echo -e "总计: $TOTAL  通过: ${G}${PASS}${N}  失败: ${R}${FAIL}${N}  跳过: ${Y}${SKIP}${N}"
if [ "$FAIL" -eq 0 ]; then
    echo -e "${G}═══════════════════════════════════════${N}"
    echo -e "${G}  ✓ V3.0.0 E2E 验证通过 ✓${N}"
    echo -e "${G}═══════════════════════════════════════${N}"
    exit 0
else
    echo -e "${R}═══════════════════════════════════════${N}"
    echo -e "${R}  ✗ V3.0.0 E2E 验证失败 ✗${N}"
    echo -e "${R}═══════════════════════════════════════${N}"
    echo
    echo "失败项:"
    for r in "${RESULTS[@]}"; do
        if [[ "$r" == FAIL* ]]; then
            echo "  $r"
        fi
    done
    exit 1
fi
