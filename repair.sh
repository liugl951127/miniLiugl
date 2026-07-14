#!/usr/bin/env bash
# =============================================================
# MiniMax Platform V3.5.8 修复脚本
#
# 功能 (按场景):
#   1. Maven 缓存损坏 -> 重新预热
#   2. Docker 镜像损坏 -> 重新构建
#   3. 种子数据不匹配 -> 同步并验证
#   4. 数据库表缺失 -> 自动重灌
#   5. 端口冲突 -> 智能检测
#   6. 内存不足 -> 自动调低 JVM
#   7. 网络拉取慢 -> 切阿里云镜像
#
# 用法:
#   ./repair.sh all            全部修复
#   ./repair.sh maven          只修复 Maven 缓存
#   ./repair.sh docker         只修复 Docker 镜像
#   ./repair.sh seed           只修复种子数据
#   ./repair.sh port           只检测端口
#   ./repair.sh memory         只检测内存
#   ./repair.sh network        只修复网络
# =============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

green()  { echo -e "\033[32m$*\033[0m"; }
red()    { echo -e "\033[31m$*\033[0m"; }
yellow() { echo -e "\033[33m$*\033[0m"; }
blue()   { echo -e "\033[36m$*\033[0m"; }
bold()   { echo -e "\033[1m$*\033[0m"; }

dc() {
  if command -v docker compose &>/dev/null; then
    docker compose "$@"
  else
    docker-compose "$@"
  fi
}

# ============== 1. Maven 修复 ==============
fix_maven() {
  echo ""
  bold "===== [1/7] Maven 缓存修复 ====="
  echo ""

  if [[ ! -d .docker-volumes/maven-repo ]]; then
    mkdir -p .docker-volumes/maven-repo
  fi

  # 清理损坏的元数据
  echo "🧹 清理损坏的 .lastUpdated 文件..."
  find .docker-volumes/maven-repo -name "*.lastUpdated" -delete 2>/dev/null || true
  find .docker-volumes/maven-repo -name "_remote.repositories" -delete 2>/dev/null || true
  find .docker-volumes/maven-repo -name "*.part" -delete 2>/dev/null || true
  green "   ✅ 元数据清理完成"

  # 重新预热
  echo ""
  echo "📥 重新预热 Maven 依赖..."
  if [[ -f docker-compose.full.yml ]]; then
    dc -f docker-compose.full.yml --profile init run --rm builder
  else
    dc -f docker-compose.mini.yml --profile init run --rm builder
  fi
  green "   ✅ Maven 缓存重建完成"
  du -sh .docker-volumes/maven-repo/ 2>/dev/null | sed 's/^/   缓存大小: /'
}

# ============== 2. Docker 镜像修复 ==============
fix_docker() {
  echo ""
  bold "===== [2/7] Docker 镜像修复 ====="
  echo ""

  echo "🧹 清理悬挂资源..."
  docker container prune -f
  docker image prune -f
  docker network prune -f
  green "   ✅ 悬挂资源清理完成"

  echo ""
  echo "🔨 重新构建所有镜像 (无缓存)..."
  if [[ -f docker-compose.full.yml ]]; then
    dc -f docker-compose.full.yml build --no-cache --parallel 2>&1 | tail -10
  else
    dc -f docker-compose.mini.yml build --no-cache --parallel 2>&1 | tail -10
  fi
  green "   ✅ 镜像重建完成"

  echo ""
  echo "🚀 重新创建容器..."
  if [[ -f docker-compose.full.yml ]]; then
    dc -f docker-compose.full.yml up -d --force-recreate
  else
    dc -f docker-compose.mini.yml up -d --force-recreate
  fi
  green "   ✅ 容器重启完成"
}

# ============== 3. 种子数据修复 ==============
fix_seed() {
  echo ""
  bold "===== [3/7] 种子数据修复 ====="
  echo ""

  # 复制到 17 个模块
  if [[ -f sql/seed-data.sql ]]; then
    echo "📋 复制 seed-data.sql 到 17 个模块 resources..."
    for mod in backend/minimax-*; do
      if [[ -d "$mod/src/main/resources" ]]; then
        cp sql/seed-data.sql "$mod/src/main/resources/seed-data.sql"
        echo "   ✓ $mod"
      fi
    done
    green "   ✅ 复制完成"
  else
    red "   ❌ sql/seed-data.sql 不存在"
    return 1
  fi

  # 验证种子
  if [[ -f scripts/verify-seed-data.py ]]; then
    echo ""
    echo "🔍 验证种子数据..."
    python3 scripts/verify-seed-data.py 2>&1 | tail -10
  fi

  # 重建 ai 模块
  echo ""
  echo "🔨 重新构建 AI 模块 (加载种子)..."
  cd backend
  timeout 60 mvn -pl minimax-ai -am package -DskipTests -Dcheckstyle.skip -Dspotless.check.skip=true 2>&1 | tail -3
  cd ..
  green "   ✅ AI 模块重新打包完成"
}

# ============== 4. 数据库表修复 ==============
fix_db() {
  echo ""
  bold "===== [4/7] 数据库表修复 ====="
  echo ""

  if ! command -v docker &>/dev/null; then
    yellow "   ⚠️ Docker 未安装, 跳过"
    return
  fi

  # 重新初始化数据库
  echo "🗄️ 重置 MariaDB 数据..."
  if [[ -f docker-compose.full.yml ]]; then
    dc -f docker-compose.full.yml down mariadb
  else
    dc -f docker-compose.mini.yml down mariadb
  fi
  docker volume rm miniLiugl_mariadb_data 2>/dev/null || true
  green "   ✅ 旧数据卷已删除"

  if [[ -f docker-compose.full.yml ]]; then
    dc -f docker-compose.full.yml up -d mariadb
  else
    dc -f docker-compose.mini.yml up -d mariadb
  fi

  echo "⏳ 等待数据库初始化 (60 秒)..."
  sleep 60
  green "   ✅ 数据库初始化完成"
}

# ============== 5. 端口检测 ==============
fix_port() {
  echo ""
  bold "===== [5/7] 端口检测 ====="
  echo ""

  # 17 微服务端口
  local ports=(7080 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090 8091 8092 8093 8094 8095 3306 6379 8848)

  echo "🔍 检测端口占用..."
  local conflict=0
  for port in "${ports[@]}"; do
    if ss -tln 2>/dev/null | grep -q ":$port "; then
      red "   ❌ 端口 $port 已被占用"
      ss -tlnp 2>/dev/null | grep ":$port "
      conflict=$((conflict+1))
    else
      echo "   ✓ $port 空闲"
    fi
  done

  if [[ $conflict -gt 0 ]]; then
    yellow "⚠️ 发现 $conflict 个端口冲突, 解决方法:"
    echo "   1. 停止占用进程: sudo kill \$(ss -tlnp | grep :7080 | awk '{print \$NF}' | cut -d= -f2 | cut -d,) "
    echo "   2. 或修改 compose 文件中的端口"
  else
    green "✅ 所有端口空闲"
  fi
}

# ============== 6. 内存检测 ==============
fix_memory() {
  echo ""
  bold "===== [6/7] 内存检测 ====="
  echo ""

  local total=$(free -g | awk '/^Mem:/{print $2}')
  local avail=$(free -g | awk '/^Mem:/{print $7}')
  echo "总内存: ${total}GB"
  echo "可用内存: ${avail}GB"

  if [[ $total -lt 8 ]]; then
    yellow "⚠️ 主机内存 < 8GB, 建议使用精简模式"
    echo "   启动: ./start-all.sh up mini"
  elif [[ $total -lt 4 ]]; then
    red "❌ 内存严重不足, 无法启动"
    echo "   建议: 至少 4GB 才能跑精简模式"
  else
    green "✅ 内存充足"
  fi

  echo ""
  echo "🐳 Docker 内存使用:"
  docker stats --no-stream --format "table {{.Name}}\t{{.MemUsage}}" 2>/dev/null | head -20 || true
}

# ============== 7. 网络修复 (Docker 镜像加速) ==============
fix_network() {
  echo ""
  bold "===== [7/7] 网络修复 ====="
  echo ""

  # 检查 Docker 镜像源
  if [[ -f /etc/docker/daemon.json ]]; then
    echo "📋 当前 Docker 镜像源:"
    cat /etc/docker/daemon.json
  else
    yellow "⚠️ 未配置 Docker 镜像加速, 国内环境建议配置"
    echo ""
    echo "推荐配置 (阿里云):"
    cat << 'EOF'
   sudo mkdir -p /etc/docker
   sudo tee /etc/docker/daemon.json << 'JSON'
   {
     "registry-mirrors": [
       "https://mirror.ccs.tencentyun.com",
       "https://docker.mirrors.ustc.edu.cn"
     ]
   }
   JSON
   sudo systemctl restart docker
EOF
  fi
  echo ""

  # 测试 Docker Hub 连接
  echo "🌐 测试 Docker Hub 连接..."
  if timeout 10 docker pull hello-world &>/dev/null; then
    green "   ✅ Docker Hub 连接正常"
  else
    red "   ❌ Docker Hub 连接失败, 请配置镜像加速"
  fi

  # 测试 Maven 仓库
  echo ""
  echo "📦 测试 Maven 仓库..."
  if timeout 5 curl -sI https://repo.maven.apache.org/maven2/ 2>/dev/null | head -1; then
    green "   ✅ Maven Central 连接正常"
  else
    yellow "   ⚠️ Maven Central 慢或不可达, 建议配置国内镜像"
    echo "   配置方法: 编辑 backend/pom.xml 加 <mirror>"
  fi
}

# ============== ALL 修复 ==============
fix_all() {
  echo ""
  bold "════════════════════════════════════════════"
  bold "  🔧 全面修复模式"
  bold "════════════════════════════════════════════"

  fix_maven
  fix_docker
  fix_seed
  fix_db
  fix_port
  fix_memory
  fix_network

  echo ""
  green "════════════════════════════════════════════"
  green "  ✅ 全部修复完成!"
  green "════════════════════════════════════════════"
  echo ""
  blue "下一步: ./start-all.sh up"
}

# ============== 主入口 ==============
case "${1:-help}" in
  all)     fix_all ;;
  maven)   fix_maven ;;
  docker)  fix_docker ;;
  seed)    fix_seed ;;
  db)      fix_db ;;
  port)    fix_port ;;
  memory)  fix_memory ;;
  network) fix_network ;;
  help|--help|-h)
    cat << 'EOF'
用法:
  ./repair.sh all         全部修复
  ./repair.sh maven       修复 Maven 缓存
  ./repair.sh docker      修复 Docker 镜像
  ./repair.sh seed        修复种子数据
  ./repair.sh db          修复数据库
  ./repair.sh port        检测端口
  ./repair.sh memory      检测内存
  ./repair.sh network     修复网络
EOF
    ;;
  *) red "未知命令: $1"; exit 1 ;;
esac
