#!/bin/bash
# MiniMax Platform - 一键启动所有服务 (后端 10 微服务 + 前端)
# 用法: bash scripts/start-platform.sh [frontend-port]

set +e

ROOT=/workspace/minimax-platform
BACKEND=$ROOT/backend
FRONTEND=$ROOT/frontend
LOGS=$ROOT/logs
SERVICES=$LOGS/services
FRONTEND_LOG=$LOGS/frontend.log
mkdir -p $SERVICES

# 颜色
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}  MiniMax Platform - 一键启动${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""

# 1. 检查依赖
echo -e "${YELLOW}[1/6] 检查环境...${NC}"
which java mvn node npm mysql 2>&1 >/dev/null
if [ $? -ne 0 ]; then
  echo -e "${RED}✗ 缺依赖, 需要: java17 mvn node npm mysql${NC}"
  exit 1
fi
java -version 2>&1 | head -1
echo -e "${GREEN}✓ 环境就绪${NC}"

# 2. 启动 MariaDB
echo ""
echo -e "${YELLOW}[2/6] 启动 MariaDB...${NC}"
if ! mysql -uroot -e "SELECT 1" >/dev/null 2>&1; then
  mkdir -p /var/run/mysqld /var/log/mysql
  chown -R mysql:mysql /var/run/mysqld /var/log/mysql 2>/dev/null
  nohup mysqld_safe --user=mysql --bind-address=127.0.0.1 > $LOGS/mariadb.log 2>&1 &
  disown
  sleep 8
fi
if mysql -uroot -e "SELECT 1" >/dev/null 2>&1; then
  echo -e "${GREEN}✓ MariaDB UP${NC}"
else
  echo -e "${RED}✗ MariaDB 启动失败, 查看 $LOGS/mariadb.log${NC}"
  exit 1
fi

# 3. 建库 + 导入 SQL
echo ""
echo -e "${YELLOW}[3/6] 初始化数据库...${NC}"
mysql -uroot -e "CREATE DATABASE IF NOT EXISTS minimax DEFAULT CHARSET utf8mb4;"
mysql -uroot -e "CREATE USER IF NOT EXISTS 'minimax'@'127.0.0.1' IDENTIFIED BY 'minimax'; GRANT ALL ON minimax.* TO 'minimax'@'127.0.0.1'; FLUSH PRIVILEGES;"
if [ -z "$(mysql -uminimax -pminimax -h 127.0.0.1 minimax -e 'SHOW TABLES' 2>/dev/null)" ]; then
  for f in $ROOT/sql/init/*.sql; do
    echo "  → $(basename $f)"
    mysql -uroot minimax < $f 2>&1 | head -2
  done
  echo -e "${GREEN}✓ 数据库初始化完成 (31 张表)${NC}"
else
  echo -e "${GREEN}✓ 数据库已存在${NC}"
fi

# 4. 配置 Maven + aliyun mirror
echo ""
echo -e "${YELLOW}[4/6] 配置 Maven 镜像...${NC}"
mkdir -p ~/.m2
if ! grep -q "aliyun" ~/.m2/settings.xml 2>/dev/null; then
  cat > ~/.m2/settings.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
  <mirrors>
    <mirror>
      <id>aliyun-public</id>
      <name>Aliyun Public</name>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
EOF
fi
echo -e "${GREEN}✓ Maven 镜像配置完成${NC}"

# 5. 编译 + 启动后端
echo ""
echo -e "${YELLOW}[5/6] 编译后端...${NC}"
cd $BACKEND
mvn -B -DskipTests install 2>&1 | tail -3

# 生成 classpath
for m in function rag admin multimodal monitor agent; do
  mvn -B -pl minimax-$m dependency:build-classpath -Dmdep.includeScope=runtime -Dmdep.outputFile=/tmp/cp-minimax-minimax-$m.txt 2>&1 | tail -1
done

# 杀掉旧服务
for port in 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090; do
  pid=$(ss -tlnp 2>/dev/null | grep ":$port " | grep -oP 'pid=\K[0-9]+' | head -1)
  [ -n "$pid" ] && kill $pid 2>/dev/null
done
sleep 2

# 启动 fat jar
cd $BACKEND/minimax-auth/target && java -jar minimax-auth.jar --server.port=8081 > $SERVICES/auth.log 2>&1 & disown
cd $BACKEND/minimax-chat/target && java -jar minimax-chat.jar --server.port=8082 > $SERVICES/chat.log 2>&1 & disown
cd $BACKEND/minimax-model/target && java -jar minimax-model.jar --server.port=8083 > $SERVICES/model.log 2>&1 & disown
cd $BACKEND/minimax-memory/target && java -jar minimax-memory.jar --server.port=8084 > $SERVICES/memory.log 2>&1 & disown

# 启动 thin
declare -A MAIN_CLASS=(
  [rag]=com.minimax.rag.RagApplication
  [function]=com.minimax.function.FunctionApplication
  [admin]=com.minimax.admin.AdminApplication
  [multimodal]=com.minimax.multimodal.MultimodalApplication
  [monitor]=com.minimax.monitor.MonitorApplication
  [agent]=com.minimax.agent.config.AgentApp
)
declare -A PORT_OF=(
  [rag]=8085 [function]=8086 [admin]=8087
  [multimodal]=8088 [monitor]=8089 [agent]=8090
)
for m in function rag admin multimodal monitor agent; do
  port=${PORT_OF[$m]}
  cd $BACKEND/minimax-$m/target
  cp=$(cat /tmp/cp-minimax-minimax-$m.txt)
  java -cp "minimax-$m-1.0.0-SNAPSHOT.jar:$cp" ${MAIN_CLASS[$m]} --server.port=$port > $SERVICES/$m.log 2>&1 & disown
done

echo -e "${GREEN}✓ 后端 10 进程已启动${NC}"
echo ""
echo "等待后端就绪 (60s)..."
sleep 60

# 6. 启动前端
echo ""
echo -e "${YELLOW}[6/6] 启动前端...${NC}"
cd $FRONTEND
if [ ! -d node_modules ]; then
  echo "  → npm install (首次需要几分钟)"
  npm install 2>&1 | tail -2
fi
FE_PORT=${1:-5173}
nohup npm run dev -- --port $FE_PORT > $FRONTEND_LOG 2>&1 &
disown
echo -e "${GREEN}✓ 前端启动中, 端口 $FE_PORT${NC}"
sleep 10

# 健康检查
echo ""
echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}  服务状态${NC}"
echo -e "${BLUE}===========================================${NC}"
up=0
for m in "auth:8081:/health" "chat:8082:/api/v1/sessions" "model:8083:/api/v1/models" "memory:8084:/api/v1/memory/list?userId=2" "rag:8085:/api/v1/rag/knowledge" "function:8086:/api/v1/function/tools" "admin:8087:/api/v1/admin/users" "multimodal:8088:/api/v1/multimodal/info" "monitor:8089:/api/v1/monitor/health" "agent:8090:/api/v1/agent/kg/entities/search?userId=2&keyword=a"; do
  IFS=':' read -r name port path <<< "$m"
  code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "http://127.0.0.1:$port$path" 2>/dev/null)
  if [ "$code" = "200" ]; then
    echo -e "  ${GREEN}✅${NC} $name :$port  UP"
    up=$((up+1))
  else
    echo -e "  ${RED}❌${NC} $name :$port  $code"
  fi
done

echo ""
echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}  访问地址${NC}"
echo -e "${BLUE}===========================================${NC}"
echo -e "  ${GREEN}前端:${NC}  http://localhost:$FE_PORT"
echo -e "  ${GREEN}账号:${NC}  adminLiugl / Liugl@2026 (超级管理员)"
echo -e "  ${GREEN}备选:${NC}  admin / admin@123 (普通管理员)"
echo ""
echo -e "  ${YELLOW}日志位置:${NC}"
echo "    后端: $SERVICES/*.log"
echo "    前端: $FRONTEND_LOG"
echo ""
echo -e "  ${YELLOW}停止服务:${NC}"
echo "    bash $ROOT/scripts/stop-platform.sh"
echo ""
