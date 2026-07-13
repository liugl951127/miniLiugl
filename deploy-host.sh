#!/usr/bin/env bash
# ============================================================
# MiniMax Platform V3.5.5+ 宿主机精简部署
# 适用: CentOS Stream 9 / Ubuntu 20+ / Debian 11+ (无 Docker 环境)
# 组件: MariaDB + Redis + auth(8081) + ai(8094) + gateway(7080) + nginx(80)
# 内存: ~1.5GB
# 用法:
#   sudo ./deploy-host.sh install   # 一键安装 (含 MariaDB/Redis/Java/Nginx)
#   sudo ./deploy-host.sh start     # 启动所有服务
#   sudo ./deploy-host.sh stop      # 停止
#   sudo ./deploy-host.sh status    # 状态
#   sudo ./deploy-host.sh logs      # 日志
# ============================================================

set -e

# ============== 配置 ==============
PROJECT_DIR="/opt/minimax"
LOG_DIR="/var/log/minimax"
RUN_DIR="/var/run/minimax"
DATA_DIR="/var/lib/minimax"

MYSQL_ROOT_PASS="root123456"
DB_NAME="minimax_platform"
DB_USER="minimax"
DB_PASS="minimax_2024"
REDIS_PASS="minimax_redis_2024"

JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
M2_HOME=${M2_HOME:-/usr/share/maven}

# 3 个核心服务 + 端口
declare -A SERVICES=(
    [auth]=8081
    [ai]=8094
    [gateway]=7080
)

# ============== 颜色 ==============
G="\033[32m"; R="\033[31m"; Y="\033[33m"; B="\033[34m"; N="\033[0m"
green()  { echo -e "${G}$*${N}"; }
red()    { echo -e "${R}$*${N}"; }
yellow() { echo -e "${Y}$*${N}"; }
blue()   { echo -e "${B}$*${N}"; }
log_step() { blue; echo; echo "▶ $*"; blue; }
log_ok()   { green "  ✅ $*"; }
log_warn() { yellow "  ⚠️  $*"; }
log_err()  { red "  ❌ $*"; }

# ============== 检测 ==============
detect_os() {
    if [ -f /etc/redhat-release ]; then
        echo "centos"
    elif [ -f /etc/debian_version ]; then
        echo "debian"
    else
        echo "unknown"
    fi
}

is_root() {
    [ "$EUID" -eq 0 ] && return 0 || return 1
}

# ============== 安装依赖 ==============
cmd_install() {
    log_step "检测系统环境"
    local os=$(detect_os)
    green "  系统: $os"
    
    if ! is_root; then
        log_err "需要 root 权限, 请用: sudo $0 install"
        exit 1
    fi
    
    # 1. Java 17
    log_step "安装 Java 17"
    if ! command -v java &>/dev/null || ! java -version 2>&1 | grep -q "17\."; then
        case $os in
            centos) yum install -y java-17-openjdk java-17-openjdk-devel ;;
            debian) apt-get update -qq && apt-get install -y -qq openjdk-17-jdk ;;
        esac
        log_ok "Java 17 已安装"
    else
        log_ok "Java 17 已存在"
    fi
    
    # 2. Maven
    log_step "安装 Maven"
    if ! command -v mvn &>/dev/null; then
        case $os in
            centos) yum install -y maven ;;
            debian) apt-get install -y -qq maven ;;
        esac
        log_ok "Maven 已安装"
    else
        log_ok "Maven 已存在"
    fi
    
    # 3. MariaDB
    log_step "安装 MariaDB"
    if ! command -v mariadb &>/dev/null && ! command -v mysql &>/dev/null; then
        case $os in
            centos) 
                yum install -y mariadb-server mariadb
                systemctl enable mariadb
                systemctl start mariadb
                ;;
            debian)
                apt-get install -y -qq mariadb-server
                systemctl enable mariadb
                systemctl start mariadb
                ;;
        esac
        log_ok "MariaDB 已安装并启动"
    else
        log_ok "MariaDB 已存在"
        systemctl is-active --quiet mariadb || systemctl is-active --quiet mysql || \
            (systemctl start mariadb 2>/dev/null || systemctl start mysql)
    fi
    
    # 4. Redis
    log_step "安装 Redis"
    if ! command -v redis-server &>/dev/null; then
        case $os in
            centos) yum install -y redis ;;
            debian) apt-get install -y -qq redis-server ;;
        esac
        systemctl enable redis || systemctl enable redis-server
        systemctl start redis || systemctl start redis-server
        log_ok "Redis 已安装并启动"
    else
        log_ok "Redis 已存在"
    fi
    
    # 5. Nginx
    log_step "安装 Nginx"
    if ! command -v nginx &>/dev/null; then
        case $os in
            centos) yum install -y nginx && systemctl enable nginx ;;
            debian) apt-get install -y -qq nginx && systemctl enable nginx ;;
        esac
        log_ok "Nginx 已安装"
    else
        log_ok "Nginx 已存在"
    fi
    
    # 6. 配置 Redis 密码
    log_step "配置 Redis 密码"
    redis-cli -a "" ping 2>/dev/null | grep -q PONG && \
        log_ok "Redis 无密码, 继续" || \
        (redis-cli ping 2>/dev/null | grep -q PONG && \
            log_ok "Redis 无密码" || \
            log_warn "Redis 已有密码, 继续")
    
    # 7. 初始化目录
    mkdir -p $PROJECT_DIR $LOG_DIR $RUN_DIR $DATA_DIR
    cp -r /workspace/miniLiugl/* $PROJECT_DIR/ 2>/dev/null || \
        log_err "复制项目失败, 请手动: cp -r /path/to/miniLiugl $PROJECT_DIR/"
    log_ok "项目复制到 $PROJECT_DIR"
    
    # 8. 配置数据库
    log_step "初始化数据库"
    cmd_init_db
    
    # 9. 编译 Java 服务
    log_step "编译 Java 服务"
    cd $PROJECT_DIR/backend
    # V3.5.5+: minimax-common 是 packaging=pom library, mvn install 装到本地 repo 即可 (不打 jar)
    $M2_HOME/bin/mvn -B -pl minimax-common -am install -DskipTests -Dspotless.skip -Djacoco.skip -q
    for svc in auth ai gateway; do
        log_step "编译 $svc"
        $M2_HOME/bin/mvn -B -pl minimax-$svc -am package -DskipTests -Dspotless.skip -Djacoco.skip -q
        log_ok "$svc 编译完成"
    done
    
    # 10. 配置 Nginx (复用独立 install-nginx.sh)
    log_step "配置 Nginx (使用 ./nginx/install-nginx.sh)"
    if [ -f $PROJECT_DIR/nginx/install-nginx.sh ]; then
        bash $PROJECT_DIR/nginx/install-nginx.sh install
    else
        # fallback: 手动拷贝 (老脚本兼容)
        cp $PROJECT_DIR/nginx/nginx.conf /etc/nginx/conf.d/minimax.conf
        sed -i 's/server auth:8081/server 127.0.0.1:8081/' /etc/nginx/conf.d/minimax.conf
        sed -i 's/server ai:8094/server 127.0.0.1:8094/' /etc/nginx/conf.d/minimax.conf
        sed -i 's/server gateway:7080/server 127.0.0.1:7080/' /etc/nginx/conf.d/minimax.conf
        nginx -t && systemctl reload nginx
        log_ok "Nginx 配置已加载"
        # 复制前端 dist
        if [ -d "$PROJECT_DIR/frontend/dist" ]; then
            cp -r $PROJECT_DIR/frontend/dist/* /usr/share/nginx/html/ 2>/dev/null || true
            cp $PROJECT_DIR/frontend/dist/index-mini.html /usr/share/nginx/html/ 2>/dev/null || true
            log_ok "前端 dist 复制到 /usr/share/nginx/html/"
        fi
    fi
    
    # 12. 注册 systemd 服务
    log_step "注册 systemd 服务"
    for svc in "${!SERVICES[@]}"; do
        port=${SERVICES[$svc]}
        # 不同服务的 heap size
        case $svc in
            ai) heap="768m" ;;
            *)  heap="384m" ;;
        esac
        cat > /etc/systemd/system/minimax-$svc.service <<EOF
[Unit]
Description=MiniMax $svc Service
After=network.target mariadb.service redis.service

[Service]
Type=simple
User=root
WorkingDirectory=$PROJECT_DIR/backend
Environment="JAVA_HOME=$JAVA_HOME"
Environment="SERVER_PORT=$port"
Environment="SPRING_PROFILES_ACTIVE=mysql"
Environment="MYSQL_HOST=127.0.0.1"
Environment="MYSQL_PORT=3306"
Environment="MYSQL_DB=$DB_NAME"
Environment="MYSQL_USER=root"
Environment="MYSQL_PASSWORD=$MYSQL_ROOT_PASS"
Environment="REDIS_HOST=127.0.0.1"
Environment="REDIS_PORT=6379"
Environment="REDIS_PASS=$REDIS_PASS"
ExecStart=$JAVA_HOME/bin/java -Xms256m -Xmx$heap -XX:+UseG1GC -jar $PROJECT_DIR/backend/minimax-$svc/target/minimax-$svc-spring-boot.jar
Restart=on-failure
RestartSec=10
StandardOutput=append:$LOG_DIR/$svc.log
StandardError=append:$LOG_DIR/$svc.err

[Install]
WantedBy=multi-user.target
EOF
        systemctl daemon-reload
        systemctl enable minimax-$svc.service
    done
    log_ok "systemd 服务已注册"
    
    # 总结
    echo ""
    green "═════════════════════════════════════════════════════════"
    green "✅ 宿主机精简部署安装完成!"
    green "═════════════════════════════════════════════════════════"
    echo ""
    green "📦 组件:"
    green "   Java 17 + Maven + MariaDB + Redis + Nginx"
    green ""
    green "🚀 启动:"
    green "   $0 start"
    echo ""
    green "🌐 访问:"
    green "   http://<server-ip>/"
    green "   http://<server-ip>:8094/actuator/health  (AI)"
    green ""
    green "🔑 默认账号: adminLiugl / Liugl@2026"
}

# ============== 初始化数据库 ==============
cmd_init_db() {
    log_step "配置 MariaDB + 导入 SQL"
    
    # 设置 root 密码 (首次)
    if mysql -uroot -e "SELECT 1" 2>/dev/null; then
        log_ok "MariaDB root 无密码, 设置密码"
        mysql -uroot <<EOF
ALTER USER 'root'@'localhost' IDENTIFIED BY '$MYSQL_ROOT_PASS';
CREATE DATABASE IF NOT EXISTS $DB_NAME DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASS';
GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'localhost';
FLUSH PRIVILEGES;
EOF
    else
        log_ok "MariaDB root 已有密码"
    fi
    
    # 导入 SQL (优先 complete.sql 100% 覆盖)
    if [ -f $PROJECT_DIR/sql/complete.sql ]; then
        log_step "导入 sql/complete.sql (完整, 77 表)"
        mysql -uroot -p"$MYSQL_ROOT_PASS" $DB_NAME < $PROJECT_DIR/sql/complete.sql 2>&1 | head -3
        log_ok "complete.sql 导入完成"
    elif [ -f $PROJECT_DIR/sql/complete.sql ]; then
        log_step "导入 sql/complete.sql (基线, 89 表)"
        mysql -uroot -p"$MYSQL_ROOT_PASS" $DB_NAME < $PROJECT_DIR/sql/complete.sql 2>&1 | head -3
        log_ok "complete.sql 导入完成"
    fi
    
    # 增量修复
    if [ -d $PROJECT_DIR/sql/fix_by_module ]; then
        log_step "应用增量修复 (fix_by_module)"
        for f in $PROJECT_DIR/sql/fix_by_module/fix_*.sql; do
            log_step "  $(basename $f)"
            mysql -uroot -p"$MYSQL_ROOT_PASS" $DB_NAME < $f 2>&1 | head -1 || true
        done
        log_ok "增量修复完成"
    fi
}

# ============== 启动 ==============
cmd_start() {
    log_step "启动所有 MiniMax 服务"
    
    # 启动基础设施
    systemctl is-active --quiet mariadb || systemctl start mariadb
    systemctl is-active --quiet redis || systemctl start redis
    log_ok "MariaDB + Redis 已启动"
    
    # 启动 3 个 Java 服务
    for svc in "${!SERVICES[@]}"; do
        log_step "启动 $svc (port ${SERVICES[$svc]})"
        systemctl start minimax-$svc.service
        sleep 3
        if systemctl is-active --quiet minimax-$svc.service; then
            log_ok "$svc 已启动"
        else
            log_err "$svc 启动失败, 查看: journalctl -u minimax-$svc -n 30"
        fi
    done
    
    # 启动 Nginx
    log_step "启动 Nginx"
    systemctl start nginx
    log_ok "Nginx 已启动"
    
    # 健康检查
    sleep 10
    log_step "健康检查"
    check_health
}

# ============== 停止 ==============
cmd_stop() {
    log_step "停止所有 MiniMax 服务"
    for svc in "${!SERVICES[@]}"; do
        systemctl stop minimax-$svc.service 2>/dev/null && log_ok "停止 $svc" || true
    done
    log_ok "所有服务已停止"
}

# ============== 重启 ==============
cmd_restart() {
    cmd_stop
    sleep 2
    cmd_start
}

# ============== 状态 ==============
cmd_status() {
    log_step "服务状态"
    echo ""
    for svc in "${!SERVICES[@]}"; do
        port=${SERVICES[$svc]}
        if systemctl is-active --quiet minimax-$svc.service; then
            green "  ✅ $svc (port $port): 运行中"
            curl -sf --max-time 2 http://localhost:$port/actuator/health >/dev/null \
                && green "     http://localhost:$port 健康" \
                || yellow "     http://localhost:$port 启动中..."
        else
            red "  ❌ $svc (port $port): 未运行"
        fi
    done
    echo ""
    echo "  Nginx:    $(systemctl is-active nginx)"
    echo "  MariaDB:  $(systemctl is-active mariadb)"
    echo "  Redis:    $(systemctl is-active redis 2>/dev/null || systemctl is-active redis-server)"
}

# ============== 日志 ==============
cmd_logs() {
    local svc="${2:-all}"
    if [ "$svc" = "all" ]; then
        journalctl -u minimax-auth -u minimax-ai -u minimax-gateway -u nginx -f
    else
        journalctl -u minimax-$svc -f
    fi
}

# ============== 健康检查 ==============
check_health() {
    local all_ok=1
    for svc in "${!SERVICES[@]}"; do
        port=${SERVICES[$svc]}
        for i in $(seq 1 30); do
            if curl -sf --max-time 2 http://localhost:$port/actuator/health >/dev/null; then
                log_ok "$svc:$port 健康"
                break
            fi
            sleep 2
        done
    done
    curl -sf --max-time 2 http://localhost/healthz && log_ok "Nginx /healthz OK"
    echo ""
    green "🌐 访问入口: http://<server-ip>/"
    green "🔑 默认账号: adminLiugl / Liugl@2026"
}

# ============== 宿主机 nohup 启动 (不依赖 systemd) ==============
cmd_bg_start() {
    log_step "宿主机后台启动 (nohup 模式, 不需 systemd)"
    mkdir -p $LOG_DIR $RUN_DIR
    
    # 1. 启动 MariaDB (尝试)
    if command -v mariadbd-safe &>/dev/null; then
        pgrep -x mariadbd >/dev/null || (mariadbd-safe --user=mysql &>/dev/null &)
        sleep 2
        log_ok "MariaDB 已尝试启动"
    elif systemctl list-unit-files mariadb.service &>/dev/null; then
        systemctl start mariadb 2>/dev/null && log_ok "MariaDB (systemctl)" || log_warn "MariaDB 启动跳过"
    fi
    
    # 2. 启动 Redis
    if command -v redis-server &>/dev/null; then
        if ! pgrep -x redis-server >/dev/null; then
            nohup redis-server --requirepass $REDIS_PASS --daemonize yes --port 6379 \
                --logfile $LOG_DIR/redis.log --dir $DATA_DIR &>/dev/null
        fi
        log_ok "Redis 已尝试启动"
    fi
    
    # 3. 编译 (如果 jar 不存在)
    cd $PROJECT_DIR/backend
    for svc in "${!SERVICES[@]}"; do
        if [ ! -f $PROJECT_DIR/backend/minimax-$svc/target/minimax-$svc-spring-boot.jar ]; then
            log_step "编译 $svc"
            # V3.5.5+: -pl minimax-common,minimax-$svc -am install (common 是 pom library, install 才能传递)
            $M2_HOME/bin/mvn -B -pl minimax-common,minimax-$svc -am install -DskipTests -Dspotless.skip -Djacoco.skip -q 2>&1 | tail -2
        fi
    done
    
    # 4. 启动 3 个 Java 服务
    for svc in "${!SERVICES[@]}"; do
        port=${SERVICES[$svc]}
        # 已运行则跳过
        if [ -f $RUN_DIR/$svc.pid ] && kill -0 $(cat $RUN_DIR/$svc.pid) 2>/dev/null; then
            log_ok "$svc 已在运行 (PID=$(cat $RUN_DIR/$svc.pid))"
            continue
        fi
        case $svc in
            ai) heap="768m" ;;
            *)  heap="384m" ;;
        esac
        nohup $JAVA_HOME/bin/java -Xms256m -Xmx$heap -XX:+UseG1GC \
            -DSERVER_PORT=$port \
            -DSPRING_PROFILES_ACTIVE=mysql \
            -DMYSQL_HOST=127.0.0.1 -DMYSQL_PORT=3306 -DMYSQL_DB=$DB_NAME \
            -DMYSQL_USER=root -DMYSQL_PASSWORD=$MYSQL_ROOT_PASS \
            -DREDIS_HOST=127.0.0.1 -DREDIS_PORT=6379 -DREDIS_PASS=$REDIS_PASS \
            -jar $PROJECT_DIR/backend/minimax-$svc/target/minimax-$svc-spring-boot.jar \
            > $LOG_DIR/$svc.log 2>&1 &
        echo $! > $RUN_DIR/$svc.pid
        log_ok "$svc 已后台启动 (PID=$!, port=$port, log=$LOG_DIR/$svc.log)"
    done
    
    # 5. 启动 nginx (如果有)
    if command -v nginx &>/dev/null; then
        if [ -f /etc/nginx/conf.d/minimax.conf ]; then
            nginx -t 2>/dev/null && nginx || log_warn "Nginx 启动失败, 检查配置"
        fi
    fi
    
    # 6. 健康检查
    sleep 15
    log_step "健康检查"
    for svc in "${!SERVICES[@]}"; do
        port=${SERVICES[$svc]}
        for i in $(seq 1 30); do
            if curl -sf --max-time 2 http://localhost:$port/actuator/health >/dev/null; then
                log_ok "$svc:$port 健康"
                break
            fi
            sleep 2
        done
    done
    echo ""
    green "═════════════════════════════════════════════════════════"
    green "✅ 宿主机后台启动完成!"
    green "═════════════════════════════════════════════════════════"
    echo ""
    green "🌐 API:"
    green "   AI:      http://localhost:8094"
    green "   Auth:    http://localhost:8081"
    green "   Gateway: http://localhost:7080"
    green ""
    green "📋 管理:"
    green "   $0 bg-stop     停止"
    green "   $0 bg-status   状态"
    green "   $0 bg-logs     日志"
    green "   tail -f $LOG_DIR/ai.log"
}

cmd_bg_stop() {
    log_step "停止所有后台 Java 服务"
    for svc in "${!SERVICES[@]}"; do
        if [ -f $RUN_DIR/$svc.pid ]; then
            pid=$(cat $RUN_DIR/$svc.pid)
            if kill -0 $pid 2>/dev/null; then
                kill $pid 2>/dev/null
                log_ok "停止 $svc (PID=$pid)"
            fi
            rm -f $RUN_DIR/$svc.pid
        fi
    done
    # 兜底
    pkill -f "minimax-.*spring-boot.jar" 2>/dev/null && log_ok "残余进程清理" || true
    log_ok "全部停止"
}

cmd_bg_status() {
    log_step "后台服务状态"
    for svc in "${!SERVICES[@]}"; do
        port=${SERVICES[$svc]}
        if [ -f $RUN_DIR/$svc.pid ] && kill -0 $(cat $RUN_DIR/$svc.pid) 2>/dev/null; then
            pid=$(cat $RUN_DIR/$svc.pid)
            green "  ✅ $svc (port $port): PID=$pid"
            curl -sf --max-time 2 http://localhost:$port/actuator/health >/dev/null \
                && green "     health: OK" \
                || yellow "     health: 启动中"
        else
            red "  ❌ $svc (port $port): 未运行"
        fi
    done
}

cmd_bg_logs() {
    local svc="${2:-all}"
    if [ "$svc" = "all" ]; then
        tail -f $LOG_DIR/auth.log $LOG_DIR/ai.log $LOG_DIR/gateway.log
    elif [ -f "$LOG_DIR/$svc.log" ]; then
        tail -f $LOG_DIR/$svc.log
    else
        log_err "日志不存在: $LOG_DIR/$svc.log"
    fi
}

# ============== 入口 ==============
case "${1:-help}" in
    install)    cmd_install ;;
    start)      cmd_start ;;
    stop)       cmd_stop ;;
    restart)    cmd_restart ;;
    status)     cmd_status ;;
    logs)       cmd_logs "$@" ;;
    init-db)    cmd_init_db ;;
    bg-start|start-bg)  cmd_bg_start ;;
    bg-stop|stop-bg)    cmd_bg_stop ;;
    bg-status|status-bg) cmd_bg_status ;;
    bg-logs|logs-bg)  cmd_bg_logs "$@" ;;
    -h|--help|help)
        cat <<EOF
MiniMax Platform 宿主机精简部署 V3.5.5+

用法: sudo $0 <命令>

命令:
  install    一键安装 (Java 17 + Maven + MariaDB + Redis + Nginx + 编译 + 初始化)
  start      启动 systemd 服务 (需 root)
  stop       停止 systemd 服务
  restart    重启
  status     查看状态
  logs [svc] 查看 systemd 日志
  init-db    重新初始化数据库

宿主机后台模式 (不需 systemd, 轻量):
  bg-start   后台 nohup 启动 3 个服务 + MariaDB/Redis
  bg-stop    停止后台服务
  bg-status  查看后台状态
  bg-logs [svc]  查看后台日志

适用环境: CentOS Stream 9 / Ubuntu 20+ / Debian 11+
内存需求: ~1.5GB
部署组件: MariaDB + Redis + auth(8081) + ai(8094) + gateway(7080) + nginx(80)

示例:
  sudo $0 install    # 一键安装 (systemd 模式)
  sudo $0 start      # systemd 启动
  sudo $0 bg-start   # 宿主机后台启动 (不需 systemd)
  sudo $0 status     # 状态
EOF
        ;;
    *) red "未知命令: $1"; echo "运行 '$0 help'"; exit 1 ;;
esac
