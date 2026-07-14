#!/usr/bin/env bash
# =============================================================
# 验证所有 16 个微服务 H2 沙箱模式能启动 (SQL 不报错)
# =============================================================
# 启动每个模块, 等就绪, 检查启动日志是否报 SQL 错误
# 然后关闭, 跑下一个
# =============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$PROJECT_DIR/backend"
LOG_DIR="/tmp/minimax-verify"
mkdir -p $LOG_DIR

# 颜色
green()  { echo -e "\033[32m$*\033[0m"; }
red()    { echo -e "\033[31m$*\033[0m"; }
yellow(){ echo -e "\033[33m$*\033[0m"; }
blue()  { echo -e "\033[36m$*\033[0m"; }
bold()  { echo -e "\033[1m$*\033[0m"; }

# 16 个模块 + 端口
declare -A MODULES=(
    ["admin"]=8090
    ["agent"]=8088
    ["ai"]=8094
    ["analytics"]=8092
    ["auth"]=8081
    ["chat"]=8082
    ["function"]=8086
    ["gateway"]=7080
    ["memory"]=8083
    ["model"]=8084
    ["monitor"]=8089
    ["multimodal"]=8087
    ["pipeline"]=8093
    ["prompt"]=8091
    ["rag"]=8085
    ["ws"]=8095
)

# 必装: java 17
if ! command -v java &>/dev/null; then
    red "❌ java 未安装"
    exit 1
fi

# 检查 jar
JAR_DIR="$BACKEND_DIR"
if [ ! -d "$JAR_DIR/minimax-ai/target" ]; then
    red "❌ jar 未编译, 先跑: cd $BACKEND_DIR && mvn -B clean install -DskipTests"
    exit 1
fi

# 清理
pkill -9 -f "minimax-.*spring-boot.jar" 2>/dev/null || true
sleep 2

bold ""
bold "═══════════════════════════════════════════════════════"
bold "  验证 16 个微服务 H2 沙箱模式 (SQL 是否正确)"
bold "═══════════════════════════════════════════════════════"
echo ""

PASS=0
FAIL=0
FAIL_LIST=()
START_TIME=$(date +%s)

for module in "${!MODULES[@]}"; do
    port=${MODULES[$module]}
    jar="$JAR_DIR/minimax-$module/target/minimax-$module-spring-boot.jar"
    log_file="$LOG_DIR/$module.log"

    blue "▶ [$module] 启动中 (port $port)..."

    if [ ! -f "$jar" ]; then
        yellow "  ⚠  jar 不存在: $jar (跳过)"
        continue
    fi

    # 启动
    nohup /usr/bin/java -Xms128m -Xmx384m -XX:+UseG1GC \
        -jar "$jar" \
        --spring.profiles.active=h2local --server.port=$port \
        > $log_file 2>&1 &
    PID=$!
    disown 2>/dev/null || true

    # 等启动 (最多 60s)
    STARTED=0
    for i in 1 2 3 4 5 6 7 8 9 10 11 12; do
        sleep 5
        if [ -f $log_file ] && grep -q "Started.*Application" $log_file 2>/dev/null; then
            STARTED=1
            break
        fi
        if ! kill -0 $PID 2>/dev/null; then
            break
        fi
    done

    if [ "$STARTED" -eq 1 ]; then
        # 检查 SQL 错误
        SQL_ERR=$(grep -iE "Unknown column|Unknown table|Table .* doesn't exist|SQLSyntaxErrorException|Column .* not found" $log_file 2>/dev/null | head -3)
        if [ -z "$SQL_ERR" ]; then
            green "  ✓ 启动成功 (无 SQL 错误)"
            PASS=$((PASS+1))
        else
            red "  ✗ 启动成功但有 SQL 错误:"
            echo "$SQL_ERR" | head -3 | sed 's/^/      /'
            FAIL=$((FAIL+1))
            FAIL_LIST+=("$module")
        fi
    else
        red "  ✗ 启动失败 (60s 内未就绪)"
        # 打印最后 5 行错误
        tail -5 $log_file 2>/dev/null | sed 's/^/      /'
        FAIL=$((FAIL+1))
        FAIL_LIST+=("$module")
    fi

    # 关闭
    kill -9 $PID 2>/dev/null || true
    sleep 2
done

# 总结
ELAPSED=$(( $(date +%s) - START_TIME ))
bold ""
bold "═══════════════════════════════════════════════════════"
bold "  验证结果"
bold "═══════════════════════════════════════════════════════"
echo ""
green "  ✓ 通过: $PASS / 16"
if [ $FAIL -gt 0 ]; then
    red "  ✗ 失败: $FAIL / 16"
    for m in "${FAIL_LIST[@]}"; do
        echo "      - $m"
    done
else
    green "  🎉 全部 16 个微服务 SQL 验证通过!"
fi
echo ""
yellow "  耗时: ${ELAPSED}s"
echo ""
yellow "  日志: $LOG_DIR/*.log"
echo ""

exit $FAIL
