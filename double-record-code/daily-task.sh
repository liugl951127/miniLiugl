#!/bin/bash
# ============================================================
# 双录一体化平台 - 每日定时任务
# 执行时间: 每天 22:00
# 任务: 跑测试 + 检查状态 + 同步 GitHub + 生成日报
# ============================================================

set -e
# 使用绝对路径(可被 cron-daemon.sh 任意目录调用)
WORKSPACE_DIR="${WORKSPACE_DIR:-/workspace}"
SCRIPT_DIR="$WORKSPACE_DIR/double-record-code"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log() { echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
err() { echo -e "${RED}[ERROR]${NC} $1"; }

# ============================================================
# 配置
# ============================================================
REPORT_DIR="$SCRIPT_DIR/.daily-reports"
LOG_DIR="$SCRIPT_DIR/.daily-logs"
GITHUB_TOKEN="${GITHUB_TOKEN:-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}"
GITHUB_REPO="https://github.com/liugl951127/recordInfo.git"
BACKEND_DIR="$WORKSPACE_DIR/double-record-code/backend"
SQL_DIR="$WORKSPACE_DIR/double-record-code/sql"
SDK_DIR="$WORKSPACE_DIR/script-sdk"
CHAINCODE_DIR="$WORKSPACE_DIR/fabric-chaincode-java"

mkdir -p "$REPORT_DIR" "$LOG_DIR"

DAILY_REPORT="$REPORT_DIR/$(date +'%Y-%m-%d').md"
LOG_FILE="$LOG_DIR/$(date +'%Y-%m-%d').log"

cd "$SCRIPT_DIR" 2>/dev/null || true

exec > >(tee -a "$LOG_FILE") 2>&1

# ============================================================
# 0. 初始化报告
# ============================================================
cat > "$DAILY_REPORT" << EOF
# 双录一体化平台 - 每日健康检查报告

> 日期: $(date +'%Y-%m-%d %H:%M:%S')
> 报告路径: $DAILY_REPORT

EOF

log "===== 每日任务开始 ====="
log "工作目录: $SCRIPT_DIR"

# ============================================================
# 1. 环境检查
# ============================================================
log "===== 步骤 1/8: 环境检查 ====="
{
    echo "## 1. 环境检查"
    echo
    echo "| 工具 | 版本 | 状态 |"
    echo "|------|------|------|"
    for tool in java mvn node npm git docker python3 mysql; do
        if command -v $tool >/dev/null 2>&1; then
            ver=$($tool --version 2>&1 | head -1)
            echo "| $tool | $ver | ✓ |"
        else
            echo "| $tool | - | ✗ 缺失 |"
        fi
    done
    echo
} >> "$DAILY_REPORT"

# ============================================================
# 2. Git 状态检查
# ============================================================
log "===== 步骤 2/8: Git 状态 ====="
{
    echo "## 2. Git 状态"
    echo
    echo '```bash'
    git status --short
    echo
    echo "最近 5 次提交:"
    git log --oneline -5
    echo
    echo "远程仓库:"
    git remote -v
    echo '```'
    echo
} >> "$DAILY_REPORT"

# ============================================================
# 3. Java 链码测试
# ============================================================
log "===== 步骤 3/8: Java 链码测试 ====="
{
    echo "## 3. Java 链码测试"
    echo
    echo '```'
    cd "$CHAINCODE_DIR"
    mvn -B test -q 2>&1 | tail -20 || echo "测试失败"
    echo '```'
    echo
} >> "$DAILY_REPORT"

# ============================================================
# 4. 后端测试
# ============================================================
log "===== 步骤 4/8: 后端测试 ====="
{
    echo "## 4. 后端测试"
    echo
    echo '```'
    cd "$BACKEND_DIR"
    mvn -B test -q 2>&1 | tail -20 || echo "测试失败"
    echo '```'
    echo
} >> "$DAILY_REPORT"

# ============================================================
# 5. TypeScript SDK 测试
# ============================================================
log "===== 步骤 5/8: TypeScript SDK 测试 ====="
{
    echo "## 5. TypeScript SDK 测试"
    echo
    echo '```'
    cd "$SDK_DIR"
    npx jest 2>&1 | tail -10 || echo "测试失败"
    echo '```'
    echo
} >> "$DAILY_REPORT"

# ============================================================
# 6. SQL 验证(如果 MySQL 可用)
# ============================================================
log "===== 步骤 6/8: SQL 验证 ====="
{
    echo "## 6. SQL 验证"
    echo
    if command -v mysql >/dev/null 2>&1; then
        if mysql -udual_record -pdual_record_2026 -e "USE dual_record;" 2>/dev/null; then
            echo '```sql'
            echo "-- 表数量"
            mysql -udual_record -pdual_record_2026 dual_record -e "SELECT COUNT(*) AS total_tables FROM information_schema.tables WHERE table_schema='dual_record' AND table_type='BASE TABLE';" 2>/dev/null

            echo "-- 订单今日新增"
            mysql -udual_record -pdual_record_2026 dual_record -e "SELECT COUNT(*) AS today_orders FROM t_order WHERE DATE(created_at) = CURDATE();" 2>/dev/null

            echo "-- 链路事件今日新增"
            mysql -udual_record -pdual_record_2026 dual_record -e "SELECT COUNT(*) AS today_events FROM t_chain_event WHERE DATE(received_at) = CURDATE();" 2>/dev/null

            echo "-- 各状态订单分布"
            mysql -udual_record -pdual_record_2026 dual_record -e "SELECT state, COUNT(*) AS count FROM t_order GROUP BY state ORDER BY state;" 2>/dev/null
            echo '```'
        else
            echo "⚠️ 无法连接 MySQL,跳过"
        fi
    else
        echo "⚠️ MySQL 客户端未安装,跳过"
    fi
    echo
} >> "$DAILY_REPORT"

# ============================================================
# 7. 体积统计
# ============================================================
log "===== 步骤 7/8: 代码体积 ====="
{
    echo "## 7. 代码体积统计"
    echo
    echo "| 模块 | 文件 | 行数 |"
    echo "|------|------|------|"

    for dir in "double-record-code/sql" "double-record-code/frontend/src" "double-record-code/backend/src/main" "script-sdk/src" "fabric-chaincode-java/src/main"; do
        if [ -d "$SCRIPT_DIR/$dir" ]; then
            files=$(find "$SCRIPT_DIR/$dir" -type f \( -name "*.ts" -o -name "*.vue" -o -name "*.java" -o -name "*.sql" \) 2>/dev/null | wc -l)
            lines=$(find "$SCRIPT_DIR/$dir" -type f \( -name "*.ts" -o -name "*.vue" -o -name "*.java" -o -name "*.sql" \) 2>/dev/null -exec cat {} + 2>/dev/null | wc -l)
            echo "| $dir | $files | $lines |"
        fi
    done
    echo
} >> "$DAILY_REPORT"

# ============================================================
# 8. 自动 commit & push
# ============================================================
log "===== 步骤 8/8: GitHub 同步 ====="
{
    echo "## 8. GitHub 同步"
    echo
    cd "$SCRIPT_DIR"

    # 添加所有变更
    if [ -n "$(git status --short)" ]; then
        git add -A
        git commit -m "chore(daily): 每日健康检查 $(date +'%Y-%m-%d')" 2>&1 | tail -3

        # 推送(token 临时嵌入)
        git remote set-url origin "https://${GITHUB_TOKEN}@github.com/liugl951127/recordInfo.git"
        if git push -u origin main 2>&1 | tail -5; then
            log "✓ 推送成功"
            echo "✅ 推送成功" >> "$DAILY_REPORT"
        else
            warn "推送失败,保留本地 commit"
            echo "❌ 推送失败(已保留本地)" >> "$DAILY_REPORT"
        fi
        # 还原
        git remote set-url origin "$GITHUB_REPO"
    else
        log "无变更,无需提交"
        echo "无变更" >> "$DAILY_REPORT"
    fi
    echo
} >> "$DAILY_REPORT"

# ============================================================
# 9. 待办检查
# ============================================================
log "===== 步骤 9/8: 待办检查 ====="
{
    echo "## 9. 待办事项跟踪"
    echo
    cat >> "$DAILY_REPORT" << 'EOF'
| 优先级 | 任务 | 状态 | 负责人 |
|--------|------|------|--------|
| P0 | 分布式限流绕过修复(Redis) | 待修复 | 后端 |
| P1 | JWT Token 撤销机制 | 待修复 | 后端 |
| P1 | Actuator 端点限制 | 待修复 | 后端 |
| P1 | 链码事件 nonce | 待修复 | 链码 |
| P2 | SM4 密钥 KMS 轮换 | 待实施 | 安全 |
| P2 | SFU 服务部署(mediasoup) | 待部署 | 运维 |
| P2 | Kafka 集群生产部署 | 待部署 | 运维 |
| P3 | AI 质检 fine-tune | 长期 | AI |
| P3 | 渗透测试复测 | 8月中 | 安全 |
EOF
    echo
} >> "$DAILY_REPORT"

# ============================================================
# 10. 总结
# ============================================================
{
    echo "## 10. 总结"
    echo
    echo "- 报告生成时间: $(date +'%Y-%m-%d %H:%M:%S')"
    echo "- 报告路径: $DAILY_REPORT"
    echo "- 日志路径: $LOG_FILE"
    echo "- 下次执行: 明天 22:00"
    echo
    echo "---"
    echo
    echo "**维护**: Mavis"
    echo "**版本**: 1.2.0"
} >> "$DAILY_REPORT"

log "===== 每日任务完成 ====="
log "报告: $DAILY_REPORT"
log "日志: $LOG_FILE"

# 清理超过 30 天的旧报告
find "$REPORT_DIR" -name "*.md" -mtime +30 -delete 2>/dev/null
find "$LOG_DIR" -name "*.log" -mtime +30 -delete 2>/dev/null

exit 0
