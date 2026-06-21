# 📦 MiniMax Platform — Windows / Linux 打包顺序

> 一份完整的、可直接复制的打包手册  
> 适用: 12 业务模块 (auth/chat/model/memory/rag/function/admin/multimodal/monitor/agent + common/gateway)  
> 产物: 后端 12 jar + 前端 dist + SQL 初始化脚本 + Docker 镜像 (可选)

---

## 1️⃣ 打包顺序总览

```
                    ┌────────────────────────┐
                    │  0. 准备 (JDK/Maven/Node) │
                    └──────────┬─────────────┘
                               ↓
        ┌──────────────────────┴──────────────────────┐
        │                                             │
   ┌────▼────┐                                  ┌─────▼─────┐
   │ 1. 后端  │  clean install → 12 模块 jar        │  2. 前端  │  npm build → dist/
   │  ~ 2分钟 │                                    │  ~ 30秒   │
   └────┬────┘                                  └─────┬─────┘
        │                                             │
        └──────────────────────┬──────────────────────┘
                               ↓
                  ┌────────────▼────────────┐
                  │  3. 产物收集 (release/)  │
                  │     ~ 5秒                │
                  └────────────┬────────────┘
                               ↓
            ┌──────────────────┴──────────────────┐
            │                                     │
       ┌────▼─────┐                        ┌──────▼──────┐
       │  4. 单平台  │  启动 + 验证            │ 5. Docker  │  build 11 镜像
       │  ~ 1分钟   │                        │  ~ 5分钟    │
       └──────────┘                        └─────────────┘
```

---

## 2️⃣ 各阶段详解

### 阶段 0: 准备环境

#### Windows (PowerShell)

```powershell
# 1. JDK 17 (推荐 Eclipse Temurin / Azul Zulu)
choco install temurin17 -y
# 或手动安装 + 设 JAVA_HOME
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17.0.10.7-hotspot", "User")
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# 2. Maven 3.8+
choco install maven -y

# 3. Node.js 20+ (含 npm)
choco install nodejs-lts -y

# 4. (可选) MySQL / MySQL 8
choco install mysql -y

# 5. (可选) Docker Desktop
choco install docker-desktop -y

# 6. 验证
java -version
mvn -v
node -v
```

#### Linux (Debian/Ubuntu)

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk-headless maven nodejs npm default-mysql-server docker.io
sudo systemctl enable --now docker mysql
java -version
mvn -v
node -v
```

#### macOS

```bash
brew install openjdk@17 maven node@20 mysql docker
java -version
mvn -v
node -v
```

---

### 阶段 1: 后端打包 (12 模块)

**进入项目根目录**:

#### Windows
```powershell
cd D:\projects\minimax-platform
```

#### Linux/macOS
```bash
cd /path/to/minimax-platform
```

#### 通用命令

```bash
cd backend

# 配置 Maven 阿里云镜像 (国内加速)
mkdir -p ~/.m2
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

# clean + install (并行构建, 约 1-2 分钟)
mvn -B clean -DskipTests
mvn -B -DskipTests -T 1C install
```

**产物**:
- `backend/minimax-{module}/target/minimax-{module}.jar` (fat jar, 可独立运行)
- 例: `minimax-auth/target/minimax-auth.jar` (48MB)

---

### 阶段 2: 前端打包

```bash
cd ../frontend

# 安装依赖 (首次 1-3 分钟, 国内用淘宝镜像)
npm config set registry https://registry.npmmirror.com
npm install

# 构建生产包
npm run build
```

**产物**:
- `frontend/dist/` (含 index.html + assets/)
- 大小: ~2.7MB (gzip 1MB)

---

### 阶段 3: 收集产物到 `release/`

```bash
cd ..
mkdir -p release/backend release/frontend release/sql release/scripts

# 复制后端 jar
cp backend/minimax-*/target/minimax-*.jar release/backend/

# 复制前端 dist
cp -r frontend/dist/* release/frontend/

# 复制 SQL
cp sql/init/*.sql release/sql/

# 复制启动脚本
cp scripts/start-platform.sh release/scripts/
cp scripts/stop-platform.sh release/scripts/
cp scripts/start-all.sh release/scripts/ 2>/dev/null || true
```

**最终 release/ 结构**:
```
release/
├── backend/          (12 个 jar, 共 ~500MB)
│   ├── minimax-auth.jar
│   ├── minimax-chat.jar
│   ├── ... (10+)
│
├── frontend/         (2.7MB 静态文件)
│   ├── index.html
│   └── assets/
│
├── sql/              (12 个 SQL 初始化脚本)
│   ├── 01-database.sql
│   └── ... (11+)
│
└── scripts/          (启动脚本)
    ├── start-platform.sh
    └── stop-platform.sh
```

---

### 阶段 4: 单平台启动 (验证)

```bash
# 1. 启 MySQL
mysqld_safe --user=mysql --bind-address=127.0.0.1 &

# 2. 初始化数据库
mysql -uroot -e "CREATE DATABASE minimax DEFAULT CHARSET utf8mb4;"
mysql -uroot -e "CREATE USER 'minimax'@'127.0.0.1' IDENTIFIED BY 'minimax_pass_2024'; GRANT ALL ON minimax.* TO 'minimax'@'127.0.0.1';"
for f in release/sql/*.sql; do mysql -uroot minimax < "$f"; done

# 3. 启后端 (10 微服务, 端口 8081-8090)
cd release/backend
for jar in minimax-*.jar; do
  name=${jar%.jar}
  port=${name##minimax-}
  port=80${port#*-}
  # 简化: 用 name 映射端口
  case $name in
    minimax-auth) port=8081 ;;
    minimax-chat) port=8082 ;;
    minimax-model) port=8083 ;;
    minimax-memory) port=8084 ;;
    minimax-rag) port=8085 ;;
    minimax-function) port=8086 ;;
    minimax-admin) port=8087 ;;
    minimax-multimodal) port=8088 ;;
    minimax-monitor) port=8089 ;;
    minimax-agent) port=8090 ;;
  esac
  java -jar $jar --server.port=$port > /tmp/$name.log 2>&1 &
done
cd ../..

# 4. 启前端
nohup python3 -m http.server 5173 --directory release/frontend > /tmp/frontend.log 2>&1 &
# 或: nginx 反向代理 release/frontend

# 5. 验证
curl http://127.0.0.1:8081/health
curl http://127.0.0.1:5173/
```

---

### 阶段 5: Docker 镜像 (可选, 推荐生产)

```bash
# 5.1 用 docker-compose 一键起整套
cd deploy
docker-compose up -d

# 5.2 或手动 build
cd ..
for module in auth chat model memory rag function admin multimodal monitor agent; do
  docker build \
    -f deploy/docker/Dockerfile.${module} \
    --build-arg MODULE=minimax-${module} \
    -t minimax-${module}:1.0.0 .
done
```

---

## 3️⃣ 🚀 一键命令 (推荐)

### Linux / macOS (bash)

我把整个流程做成一个脚本:

```bash
# 项目根目录
cd /path/to/minimax-platform

bash scripts/build-all.sh
```

### Windows (PowerShell)

```powershell
cd D:\projects\minimax-platform

.\scripts\build-all.ps1
```

---

## 4️⃣ 📜 一键脚本源码

### `scripts/build-all.sh` (Linux/macOS)

```bash
#!/bin/bash
# MiniMax Platform - 一键打包 (Linux/macOS)
# 流程: 准备 → 后端 → 前端 → 收集 → 验证

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
echo "=========================================="
echo "  MiniMax Platform - 一键打包"
echo "  路径: $ROOT"
echo "=========================================="

# ===== 阶段 0: 准备 =====
echo -e "\n${YELLOW}[0/5] 检查环境...${NC}"
for cmd in java mvn node npm; do
  if ! command -v $cmd >/dev/null 2>&1; then
    echo -e "${RED}✗ 缺依赖: $cmd${NC}"
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
mkdir -p release/{backend,frontend,sql,scripts,docs}

# 后端 jar
for module in auth chat model memory rag function admin multimodal monitor agent; do
  jar=backend/minimax-$module/target/minimax-$module.jar
  if [ -f "$jar" ]; then
    cp $jar release/backend/
  else
    # thin jar
    cp backend/minimax-$module/target/minimax-$module-1.0.0-SNAPSHOT.jar release/backend/ 2>/dev/null || true
  fi
done
# gateway (可选)
cp backend/minimax-gateway/target/minimax-gateway-1.0.0-SNAPSHOT.jar release/backend/ 2>/dev/null || true

# 前端
cp -r frontend/dist/* release/frontend/ 2>/dev/null || true

# SQL
cp sql/init/*.sql release/sql/

# 脚本
cp scripts/*.sh scripts/*.bat release/scripts/ 2>/dev/null || true
cp scripts/*.ps1 release/scripts/ 2>/dev/null || true

# 文档
cp README.md release/ 2>/dev/null || true
cp docs/*.md release/docs/ 2>/dev/null || true
echo -e "${GREEN}✓ 产物已收集到 release/${NC}"

# ===== 阶段 4: 验证 =====
echo -e "\n${YELLOW}[4/5] 验证产物...${NC}"
echo "  后端 jar:"
ls -la release/backend/*.jar 2>/dev/null | awk '{print "    "$NF" "$5}' | head -15
echo "  前端:"
ls -la release/frontend/index.html 2>/dev/null | awk '{print "    "$NF" "$5}' | head -3
echo "  SQL:"
ls release/sql/*.sql 2>/dev/null | wc -l | awk '{print "    "$1" 个脚本"}'
echo "  脚本:"
ls release/scripts/ 2>/dev/null | wc -l | awk '{print "    "$1" 个"}'

# 体积统计
size=$(du -sh release/ | awk '{print $1}')
echo "  总大小: $size"

# ===== 阶段 5: 测试可选启动 =====
echo -e "\n${YELLOW}[5/5] 准备部署包 (可选: 启动测试)...${NC}"
if [ "$1" = "--with-test" ]; then
  echo "  启动 MySQL..."
  if ! pgrep -x mysqld >/dev/null 2>&1; then
    mkdir -p /var/run/mysqld /var/log/mysql
    chown -R mysql:mysql /var/run/mysqld /var/log/mysql 2>/dev/null || true
    nohup mysqld_safe --user=mysql --bind-address=127.0.0.1 > /tmp/mysql.log 2>&1 &
    sleep 8
  fi
  echo "  初始化数据库..."
  mysql -uroot -e "CREATE DATABASE IF NOT EXISTS minimax DEFAULT CHARSET utf8mb4;"
  mysql -uroot -e "CREATE USER IF NOT EXISTS 'minimax'@'127.0.0.1' IDENTIFIED BY 'minimax_pass_2024'; GRANT ALL ON minimax.* TO 'minimax'@'127.0.0.1';"
  for f in release/sql/*.sql; do
    mysql -uroot minimax < "$f" 2>/dev/null
  done
  echo "  启动 10 个后端 (后台)..."
  bash scripts/start-platform.sh
fi

echo ""
echo "=========================================="
echo -e "  ${GREEN}✅ 打包完成!${NC}"
echo "  release/ 目录: $ROOT/release"
echo "=========================================="
echo ""
echo "下一步:"
echo "  # 1. 复制到目标机器"
echo "  scp -r release/ user@server:/opt/minimax/"
echo ""
echo "  # 2. 在目标机器启动"
echo "  cd /opt/minimax && bash scripts/start-platform.sh"
echo ""
echo "  # 3. 访问"
echo "  http://<server>:5173"
echo "  登录: adminLiugl / Liugl@2026"
```

### `scripts/build-all.ps1` (Windows PowerShell)

```powershell
# MiniMax Platform - 一键打包 (Windows PowerShell)
# 流程: 准备 → 后端 → 前端 → 收集 → 验证

$ErrorActionPreference = "Stop"

$ROOT = Split-Path -Parent $PSScriptRoot
Set-Location $ROOT

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  MiniMax Platform - 一键打包 (Windows)" -ForegroundColor Cyan
Write-Host "  路径: $ROOT" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# ===== 阶段 0: 准备 =====
Write-Host "`n[0/5] 检查环境..." -ForegroundColor Yellow
foreach ($cmd in @("java", "mvn", "node", "npm")) {
  if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
    Write-Host "✗ 缺依赖: $cmd" -ForegroundColor Red
    exit 1
  }
}
& java -version 2>&1 | Select-Object -First 1
& mvn -v 2>&1 | Select-Object -First 1
& node -v

# Maven 镜像
$m2 = "$env:USERPROFILE\.m2"
New-Item -ItemType Directory -Force -Path $m2 | Out-Null
$settingsPath = "$m2\settings.xml"
if (-not (Test-Path $settingsPath) -or -not (Select-String -Path $settingsPath -Pattern "aliyun" -Quiet)) {
  @'
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
'@ | Out-File -FilePath $settingsPath -Encoding UTF8
  Write-Host "  → 已配置阿里云镜像"
}
Write-Host "✓ 环境就绪" -ForegroundColor Green

# ===== 阶段 1: 后端 =====
Write-Host "`n[1/5] 后端打包 (12 模块)..." -ForegroundColor Yellow
Set-Location backend
& mvn -B clean -DskipTests
if ($LASTEXITCODE -ne 0) { exit 1 }
& mvn -B -DskipTests -T 1C install
if ($LASTEXITCODE -ne 0) { exit 1 }
Set-Location $ROOT
Write-Host "✓ 后端 12 模块 BUILD SUCCESS" -ForegroundColor Green

# ===== 阶段 2: 前端 =====
Write-Host "`n[2/5] 前端打包..." -ForegroundColor Yellow
Set-Location frontend
if (-not (Test-Path "node_modules")) {
  npm config set registry https://registry.npmmirror.com
  npm install
}
npm run build
if ($LASTEXITCODE -ne 0) { exit 1 }
Set-Location $ROOT
Write-Host "✓ 前端 BUILD SUCCESS" -ForegroundColor Green

# ===== 阶段 3: 收集产物 =====
Write-Host "`n[3/5] 收集产物..." -ForegroundColor Yellow
Remove-Item -Recurse -Force release -ErrorAction SilentlyContinue
$dirs = @("backend", "frontend", "sql", "scripts", "docs")
foreach ($d in $dirs) { New-Item -ItemType Directory -Force -Path "release\$d" | Out-Null }

# 复制 jar
$modules = @("auth", "chat", "model", "memory", "rag", "function", "admin", "multimodal", "monitor", "agent")
foreach ($m in $modules) {
  $jar1 = "backend\minimax-$m\target\minimax-$m.jar"
  $jar2 = "backend\minimax-$m\target\minimax-$m-1.0.0-SNAPSHOT.jar"
  if (Test-Path $jar1) {
    Copy-Item $jar1 release\backend\
  } elseif (Test-Path $jar2) {
    Copy-Item $jar2 release\backend\
  }
}

# 复制 dist
if (Test-Path frontend\dist) {
  Copy-Item -Recurse -Force frontend\dist\* release\frontend\
}

# 复制 SQL
Get-ChildItem sql\init\*.sql | ForEach-Object {
  Copy-Item $_.FullName release\sql\
}

# 复制脚本 + 文档
Copy-Item scripts\*.sh scripts\*.bat scripts\*.ps1 -Destination release\scripts\ -ErrorAction SilentlyContinue
Copy-Item README.md release\ -ErrorAction SilentlyContinue
Get-ChildItem docs\*.md -ErrorAction SilentlyContinue | ForEach-Object {
  Copy-Item $_.FullName release\docs\
}

Write-Host "✓ 产物已收集到 release\" -ForegroundColor Green

# ===== 阶段 4: 验证 =====
Write-Host "`n[4/5] 验证产物..." -ForegroundColor Yellow
$jars = Get-ChildItem release\backend\*.jar
Write-Host "  后端 jar ($($jars.Count) 个):"
$jars | ForEach-Object { Write-Host "    $($_.Name) ($([math]::Round($_.Length/1MB,1))MB)" }

$indexHtml = Test-Path release\frontend\index.html
Write-Host "  前端 index.html: $(if($indexHtml){'OK'}else{'缺失'})"
$sqlCount = (Get-ChildItem release\sql\*.sql).Count
Write-Host "  SQL: $sqlCount 个脚本"

$size = (Get-ChildItem release -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB
Write-Host "  总大小: $([math]::Round($size,1))MB"

# ===== 阶段 5: 可选启动 =====
Write-Host "`n[5/5] 准备部署包 (可选: 启动测试)..." -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  打包完成!" -ForegroundColor Green
Write-Host "  release/ 目录: $ROOT\release" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "下一步:"
Write-Host "  # 1. 压缩为 tar/zip (可选)"
Write-Host "  Compress-Archive -Path release\* -DestinationPath release.zip"
Write-Host ""
Write-Host "  # 2. 复制到目标机器"
Write-Host "  scp -r release/ user@server:/opt/minimax/"
Write-Host ""
Write-Host "  # 3. 在目标机器启动"
Write-Host "  cd /opt/minimax && bash scripts/start-platform.sh"
Write-Host ""
Write-Host "  # 4. 访问"
Write-Host "  http://<server>:5173"
Write-Host "  登录: adminLiugl / Liugl@2026"
```

---

## 5️⃣ 快速参考

### 各阶段耗时 (MacBook Pro M2 估算)

| 阶段 | 时间 | 备注 |
|------|------|------|
| 0. 准备 | 5-10 min | 首次装环境 |
| 1. 后端 | 1-2 min | 12 模块并行 |
| 2. 前端 | 30-60 s | npm 依赖首次慢 |
| 3. 收集 | < 5 s | |
| 4. 验证 | 1 min | 可选启动测试 |
| 5. Docker | 5-10 min | 11 个镜像 |
| **总计** | **8-15 min** | 首次 + Docker |

### 产物大小

| 文件 | 大小 |
|------|------|
| minimax-auth.jar | 48MB |
| minimax-chat.jar | 100MB |
| minimax-model.jar | 68MB |
| minimax-memory.jar | 48MB |
| 其他 6 thin | 10-50MB 各 |
| 前端 dist | 2.7MB |
| **release/** | **~500MB** |
| Docker 镜像 | ~2GB (11 镜像) |

### 端口分配

| 端口 | 服务 |
|------|------|
| 5173 | 前端 (vite dev) / 80 (nginx) |
| 8081 | auth |
| 8082 | chat |
| 8083 | model |
| 8084 | memory |
| 8085 | rag |
| 8086 | function |
| 8087 | admin |
| 8088 | multimodal |
| 8089 | monitor |
| 8090 | agent |
| 3306 | MySQL / MySQL |
| 6379 | Redis (可选) |

---

## 6️⃣ 常见问题

### Q1: Windows 编译报 `GBK` 编码错
**A**: pom.xml `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>` 已经在父 pom。

### Q2: Maven 慢 / 超时
**A**: 配置阿里云镜像 (脚本自动配置), 或加 `-T 1C` 并行。

### Q3: 前端 npm install 失败
**A**: 用淘宝镜像 `npm config set registry https://registry.npmmirror.com`

### Q4: 数据库连接失败
**A**: 检查 `application.yml` 的 `127.0.0.1:3306` + `minimax/minimax` 账号。

### Q5: Linux 启动后 java 进程被 kill
**A**: 用 `nohup java -jar ... &` 脱离控制组, 或用 systemd/supervisor 托管。

### Q6: 端口被占用
**A**: `lsof -i :8081` (Linux) / `netstat -ano | findstr :8081` (Windows)

### Q7: 跳过测试
**A**: `mvn -DskipTests` 打包; `mvn test` 跑测试。

---

## 7️⃣ 生产部署检查清单

- [ ] JDK 17 已装 (`java -version`)
- [ ] MySQL / MySQL 8 已启
- [ ] `minimax` 数据库已建 + SQL 已导入
- [ ] `minimax@127.0.0.1` 用户已建 + 授权
- [ ] 12 个 jar 已打包
- [ ] 前端 dist 已构建
- [ ] 防火墙放行 80/443/3306
- [ ] Nginx 反向代理已配置 (推荐)
- [ ] HTTPS 证书 (生产)
- [ ] 日志目录 + logrotate
- [ ] systemd / supervisor 守护
- [ ] Prometheus 抓取 (可选)
- [ ] 备份策略 (每日 mysqldump)
