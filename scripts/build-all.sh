#!/bin/bash
# MiniMax Platform - 一键打包 (Linux/macOS)
# 流程: 准备 → 后端 → 前端 → 收集 → 验证
# 用法: bash scripts/build-all.sh [可选 --with-test]

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
echo "=========================================="
echo -e "  ${BLUE}MiniMax Platform - 一键打包${NC}"
echo "  路径: $ROOT"
echo "=========================================="

# ===== 阶段 0: 准备 =====
echo -e "\n${YELLOW}[0/5] 检查环境...${NC}"
for cmd in java mvn node npm; do
  if ! command -v $cmd >/dev/null 2>&1; then
    echo -e "${RED}✗ 缺依赖: $cmd${NC}"
    echo "  安装: sudo apt install -y openjdk-17-jdk-headless maven nodejs npm"
    exit 1
  fi
done
java -version 2>&1 | head -1
mvn -v 2>&1 | head -1
node -v
echo -e "${GREEN}✓ 环境就绪${NC}"

# Maven 阿里云镜像
mkdir -p ~/.m2
if ! grep -q aliyun ~/.m2/settings.xml 2>/dev/null; then
  cat > ~/.m2/settings.xml <<'EOF'
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
  echo "  → 已配置阿里云镜像"
fi

# ===== 阶段 1: 后端打包 =====
echo -e "\n${YELLOW}[1/5] 后端打包 (12 模块)...${NC}"
cd backend
mvn -B clean -DskipTests -q
mvn -B -DskipTests -T 1C install -q
cd ..
echo -e "${GREEN}✓ 后端 12 模块 BUILD SUCCESS${NC}"

# ===== 阶段 2: 前端打包 =====
echo -e "\n${YELLOW}[2/5] 前端打包...${NC}"
cd frontend
if [ ! -d node_modules ]; then
  npm config set registry https://registry.npmmirror.com 2>/dev/null || true
  npm install --silent
fi
npm run build
cd ..
echo -e "${GREEN}✓ 前端 BUILD SUCCESS${NC}"

# ===== 阶段 3: 收集产物 =====
echo -e "\n${YELLOW}[3/5] 收集产物...${NC}"
rm -rf release
mkdir -p release/backend release/frontend release/sql release/scripts release/docs

# 后端 jar (fat jar 优先)
for module in auth chat model memory rag function admin multimodal monitor agent; do
  if [ -f "backend/minimax-$module/target/minimax-$module.jar" ]; then
    cp "backend/minimax-$module/target/minimax-$module.jar" release/backend/
  elif [ -f "backend/minimax-$module/target/minimax-$module-1.0.0-SNAPSHOT.jar" ]; then
    cp "backend/minimax-$module/target/minimax-$module-1.0.0-SNAPSHOT.jar" release/backend/
  fi
done
# gateway (可选)
if [ -f "backend/minimax-gateway/target/minimax-gateway-1.0.0-SNAPSHOT.jar" ]; then
  cp "backend/minimax-gateway/target/minimax-gateway-1.0.0-SNAPSHOT.jar" release/backend/ 2>/dev/null || true
fi

# 前端
if [ -d frontend/dist ]; then
  cp -r frontend/dist/* release/frontend/
fi

# SQL
for f in sql/init/*.sql; do
  cp "$f" release/sql/
done

# 脚本
for f in scripts/*.sh scripts/*.bat scripts/*.ps1; do
  [ -f "$f" ] && cp "$f" release/scripts/
done

# 文档
[ -f README.md ] && cp README.md release/
[ -f ARCHITECTURE.md ] && cp ARCHITECTURE.md release/docs/ 2>/dev/null || true
[ -f CHANGELOG.md ] && cp CHANGELOG.md release/docs/ 2>/dev/null || true
[ -f API.md ] && cp API.md release/docs/ 2>/dev/null || true
for f in docs/*.md; do
  [ -f "$f" ] && cp "$f" release/docs/
done

echo -e "${GREEN}✓ 产物已收集到 release/${NC}"

# ===== 阶段 4: 验证 =====
echo -e "\n${YELLOW}[4/5] 验证产物...${NC}"
echo "  后端 jar:"
for jar in release/backend/*.jar; do
  size=$(du -h "$jar" | awk '{print $1}')
  echo "    $(basename $jar) ($size)"
done
echo "  前端:"
if [ -f release/frontend/index.html ]; then
  size=$(du -h release/frontend/index.html | awk '{print $1}')
  echo "    index.html ($size) + assets/"
else
  echo "    ⚠ index.html 缺失"
fi
sql_count=$(ls release/sql/*.sql 2>/dev/null | wc -l)
echo "  SQL: $sql_count 个脚本"
script_count=$(ls release/scripts/ 2>/dev/null | wc -l)
echo "  脚本: $script_count 个"

# 体积统计
size=$(du -sh release/ | awk '{print $1}')
echo "  总大小: $size"

# ===== 阶段 5: 可选启动测试 =====
echo -e "\n${YELLOW}[5/5] 准备部署包 (可选: 启动测试)...${NC}"
if [ "$1" = "--with-test" ]; then
  echo "  启动 MariaDB..."
  if ! pgrep -x mysqld >/dev/null 2>&1; then
    mkdir -p /var/run/mysqld /var/log/mysql
    chown -R mysql:mysql /var/run/mysqld /var/log/mysql 2>/dev/null || true
    nohup mysqld_safe --user=mysql --bind-address=127.0.0.1 > /tmp/mariadb.log 2>&1 &
    sleep 8
  fi
  echo "  初始化数据库..."
  mysql -uroot -e "CREATE DATABASE IF NOT EXISTS minimax DEFAULT CHARSET utf8mb4;" 2>/dev/null
  mysql -uroot -e "CREATE USER IF NOT EXISTS 'minimax'@'127.0.0.1' IDENTIFIED BY 'minimax_pass_2024'; GRANT ALL ON minimax.* TO 'minimax'@'127.0.0.1';" 2>/dev/null
  for f in release/sql/*.sql; do
    mysql -uroot minimax < "$f" 2>/dev/null
  done
  echo "  启动 10 个后端 (后台)..."
  if [ -f scripts/start-platform.sh ]; then
    bash scripts/start-platform.sh
  fi
fi

echo ""
echo "=========================================="
echo -e "  ${GREEN}✅ 打包完成!${NC}"
echo "  release/ 目录: $ROOT/release"
echo "=========================================="
echo ""
echo "下一步:"
echo "  # 1. 压缩打包 (可选)"
echo "  tar czf minimax-release.tar.gz release/"
echo ""
echo "  # 2. 复制到目标机器"
echo "  scp -r release/ user@server:/opt/minimax/"
echo ""
echo "  # 3. 在目标机器启动"
echo "  cd /opt/minimax && bash scripts/start-platform.sh"
echo ""
echo "  # 4. 访问"
echo "  http://<server>:5173"
echo "  登录: adminLiugl / Liugl@2026"
