#!/usr/bin/env bash
# =============================================================
# 部署"每日 20:00 自动构建"任务
# 用法: 在你**本机或服务器**上执行本脚本（不需要 root，会装到用户 crontab）
# =============================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_SCRIPT="$SCRIPT_DIR/daily-build.sh"
LOG_DIR="$SCRIPT_DIR/../logs"
CRON_LINE="0 20 * * * /bin/bash $BUILD_SCRIPT >> $LOG_DIR/cron.log 2>&1"

echo "将注册 cron 任务: $CRON_LINE"
echo "按 Enter 继续，Ctrl+C 取消..."
read -r

# 加到 crontab
( crontab -l 2>/dev/null | grep -v 'daily-build.sh'; echo "$CRON_LINE" ) | crontab -

echo "✅ 已注册。当前 crontab:"
crontab -l | grep -E '(daily-build|^# )' || crontab -l
