#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - 每日 20:00 推进入口
# cron 触发这脚本，自动完成：
#   1. 读取 PROGRESS.md 找到 "## Day N - 待开始"
#   2. 推进 N 号那天的模块（生成代码、SQL、测试）
#   3. 跑自检（前端构建 + Java 静态体检）
#   4. 打包成加密 zip
#   5. 发邮件到 liugeliang951127@gmail.com
#
# 注意：实际的"代码生成"步骤是把任务写到一个 runbook
# 文件里，让 mavis session 在 cron 触发时主动 follow。
# =============================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT"

DAY_FILE="$ROOT/reports/next-day.txt"
LOG_DIR="$ROOT/logs"
mkdir -p "$LOG_DIR"

DAY=$(cat "$DAY_FILE" 2>/dev/null || echo "3")
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

echo "[$TIMESTAMP] ====== 每日推进 Day $DAY 启动 ======" | tee -a "$LOG_DIR/cron.log"

# 1. 写任务 runbook（让后续 session 接管）
cat > "$ROOT/reports/runbook-day-${DAY}.md" <<EOF
# Day $DAY 自动推进 Runbook

**触发时间**: $TIMESTAMP
**目标**: 按 PROGRESS.md 的 Day $DAY 计划推进，落地代码 + 自检 + 打包 + 邮件

## 步骤
1. 读 PROGRESS.md 找 Day $DAY 计划
2. 落地：实体 / Mapper / Service / Controller / 前端页面 / SQL
3. 跑 \`scripts/daily-build.sh $DAY\` 自检
4. 跑 \`scripts/java-static-check.sh\` 体检
5. 打包：\`tar --exclude=... -czf day${DAY}.tar.gz .\`
6. 加密 zip：\`zip -P 'MinMax2026!' day${DAY}.zip day${DAY}.tar.gz\`
7. 发邮件：\`python3 scripts/send-daily-report.py\`
8. 更新 PROGRESS.md + 报告 + 推进 day 文件

EOF

echo "[$TIMESTAMP] Runbook 已生成: reports/runbook-day-${DAY}.md" | tee -a "$LOG_DIR/cron.log"
echo "[$TIMESTAMP] Day $DAY 推进任务入队（待 session 接管）" | tee -a "$LOG_DIR/cron.log"
exit 0
