#!/bin/bash
# ============================================================
# 双录一体化平台 - 常驻定时任务守护进程
# 适用:无 crontab/systemd 环境的沙箱/容器
# 行为: 每 24 小时跑一次 daily-task.sh
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DAILY_SCRIPT="$SCRIPT_DIR/daily-task.sh"
PID_FILE="$SCRIPT_DIR/.cron-daemon.pid"
LOG_FILE="$SCRIPT_DIR/.cron-daemon.log"
INTERVAL="${DAILY_INTERVAL:-86400}"  # 默认 24 小时,可通过环境变量覆盖

# ============================================================
# 启动/停止
# ============================================================
start() {
    if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
        echo "已在运行: PID $(cat "$PID_FILE")"
        exit 1
    fi

    echo "启动守护进程..."
    nohup bash -c "
        while true; do
            echo \"[\$(date +'%Y-%m-%d %H:%M:%S')] 触发每日任务\"
            $DAILY_SCRIPT
            echo \"[\$(date +'%Y-%m-%d %H:%M:%S')] 任务完成,等待 $INTERVAL 秒\"
            sleep $INTERVAL
        done
    " >> "$LOG_FILE" 2>&1 &

    echo $! > "$PID_FILE"
    echo "✓ 守护进程已启动: PID $(cat "$PID_FILE")"
    echo "  日志: $LOG_FILE"
    echo "  间隔: $INTERVAL 秒"
}

stop() {
    if [ ! -f "$PID_FILE" ]; then
        echo "未运行"
        exit 1
    fi

    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
        kill "$PID"
        echo "✓ 停止 PID $PID"
    fi
    rm -f "$PID_FILE"
}

status() {
    if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
        echo "运行中: PID $(cat "$PID_FILE")"
        ps -p $(cat "$PID_FILE") -o pid,etime,cmd 2>/dev/null
    else
        echo "未运行"
    fi
    echo
    echo "最近日志:"
    tail -20 "$LOG_FILE" 2>/dev/null || echo "(无日志)"
}

run_once() {
    echo "立即执行一次(测试模式)..."
    bash "$DAILY_SCRIPT"
}

# ============================================================
# CLI
# ============================================================
case "${1:-start}" in
    start) start ;;
    stop) stop ;;
    status) status ;;
    restart) stop; sleep 2; start ;;
    run-once) run_once ;;
    *)
        echo "用法: $0 {start|stop|status|restart|run-once}"
        echo
        echo "环境变量:"
        echo "  DAILY_INTERVAL  间隔秒数(默认 86400 = 24h)"
        exit 1
        ;;
esac
