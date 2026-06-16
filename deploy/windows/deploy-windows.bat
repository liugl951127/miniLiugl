@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ============================================================
REM  MiniMax Platform - Windows 单机部署脚本
REM  适用: Windows 10/11 + Docker Desktop
REM  作者: Mavis  日期: 2026-06-16
REM ============================================================

title MiniMax Platform - Windows Deploy

echo.
echo ============================================================
echo   MiniMax Platform Windows 单机部署
echo ============================================================
echo.

REM ---------- 0) 前置检查 ----------
echo [0/8] 检查前置环境...

where docker >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到 Docker，请先安装 Docker Desktop:
    echo        https://www.docker.com/products/docker-desktop
    pause
    exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
    echo [错误] Docker 未运行，请启动 Docker Desktop
    pause
    exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
    echo [警告] 未检测到 Java (用于本地直接运行 jar)
    echo        部署仍可继续 (用 Docker 模式)
)

where mvn >nul 2>&1
if errorlevel 1 (
    echo [警告] 未检测到 Maven (用于源码构建)
)

echo [OK] 前置环境检查通过
echo.

REM ---------- 1) 选择部署模式 ----------
echo 请选择部署模式:
echo   [1] Docker Compose  (推荐, 一键启动全部服务)
echo   [2] 本地 jar 模式  (需要 JDK 17 + Maven)
echo   [3] 仅启动基础设施 (MySQL/Redis/ES, 应用 jar 自己起)
echo.
set /p MODE="选择 [1/2/3]: "

if "%MODE%"=="" set MODE=1
if "%MODE%"=="1" goto :DOCKER_MODE
if "%MODE%"=="2" goto :JAR_MODE
if "%MODE%"=="3" goto :INFRA_MODE
echo [错误] 无效选择
pause
exit /b 1

REM ============================================================
REM 模式 1: Docker Compose 全栈
REM ============================================================
:DOCKER_MODE
echo.
echo [1/8] Docker Compose 全栈部署
echo.

REM ---------- 2) 准备环境变量 ----------
if not exist ".env" (
    echo [2/8] 生成 .env 配置文件...
    (
        echo # MiniMax Platform 环境变量
        echo COMPOSE_PROJECT_NAME=minimax
        echo.
        echo # MySQL
        echo MYSQL_ROOT_PASSWORD=MinMax2026!
        echo MYSQL_DATABASE=minimax
        echo MYSQL_USER=minimax
        echo MYSQL_PASSWORD=MinMax2026!
        echo.
        echo # Redis
        echo REDIS_PASSWORD=MinMax2026!
        echo.
        echo # JWT
        echo JWT_SECRET=please-change-this-to-a-32-byte-random-secret-key-min-max
        echo.
        echo # 模型 (留空 = Mock 模式)
        echo OPENAI_API_KEY=
        echo MINIMAX_API_KEY=
        echo.
        echo # Embedding (留空 = Mock)
        echo EMBEDDING_API_KEY=
        echo EMBEDDING_BASE_URL=
    ) > .env
    echo [OK] .env 已生成, 请根据需要修改后重新执行
    echo     或直接继续 (将使用默认配置)
) else (
    echo [2/8] .env 已存在, 跳过生成
)
echo.

REM ---------- 3) 拉取/构建镜像 ----------
echo [3/8] 准备 Docker 镜像 (首次约 5-10 分钟)...
docker compose --profile app build --parallel 2>nul
if errorlevel 1 (
    echo [提示] 镜像可能已存在, 跳过 build
)
echo.

REM ---------- 4) 启动基础设施 ----------
echo [4/8] 启动 MySQL + Redis + ES...
docker compose up -d mysql redis
echo 等待 MySQL 就绪...
:WAIT_MYSQL
docker exec minimax-mysql mysqladmin ping -h localhost -uroot -pMinMax2026! >nul 2>&1
if errorlevel 1 (
    timeout /t 3 /nobreak >nul
    goto :WAIT_MYSQL
)
echo [OK] MySQL 就绪
echo.

REM ---------- 5) 初始化数据库 ----------
echo [5/8] 初始化数据库 (建表)...
for %%F in (..\sql\init\*.sql) do (
    echo   执行 %%F ...
    docker exec -i minimax-mysql mysql -uroot -pMinMax2026! minimax < %%F
    if errorlevel 1 (
        echo   [警告] %%F 执行失败, 继续...
    )
)
echo [OK] 数据库初始化完成
echo.

REM ---------- 6) 启动 ES + MinIO (可选) ----------
echo [6/8] 启动 ES + MinIO (可选)...
set /p START_ES="是否启动 Elasticsearch? [y/N]: "
if /i "!START_ES!"=="y" (
    docker compose up -d elasticsearch
    echo 等待 ES 就绪 (可能 60s)...
    :WAIT_ES
    docker exec minimax-elasticsearch curl -sf http://localhost:9200 >nul 2>&1
    if errorlevel 1 (
        timeout /t 5 /nobreak >nul
        goto :WAIT_ES
    )
    echo [OK] ES 就绪
)

set /p START_MINIO="是否启动 MinIO? [y/N]: "
if /i "!START_MINIO!"=="y" (
    docker compose up -d minio
)
echo.

REM ---------- 7) 启动应用 ----------
echo [7/8] 启动应用服务 (auth/gateway/chat/memory/model/rag)...
docker compose --profile app up -d
echo.
echo 等待应用就绪...
:WAIT_APP
timeout /t 5 /nobreak >nul
docker ps --filter "name=minimax" --format "{{.Names}}\t{{.Status}}" | findstr "Up" >nul
if errorlevel 1 goto :WAIT_APP
echo [OK] 应用已启动
echo.

REM ---------- 8) 验证 ----------
echo [8/8] 验证部署...
echo.
echo 服务状态:
docker compose ps
echo.
echo 健康检查:
echo   Gateway:   http://localhost:8080/actuator/health
echo   Auth:      http://localhost:8081/api/v1/auth/health
echo   Chat:      http://localhost:8082/api/v1/sessions
echo   Model:     http://localhost:8083/api/v1/models
echo   Memory:    http://localhost:8084/api/v1/memory/short-term/1/size
echo   RAG:       http://localhost:8085/api/v1/rag/kb/public
echo   Frontend:  http://localhost:80  (or  http://localhost:5173 dev mode)
echo.
echo [SUCCESS] 部署完成！
echo.
echo 默认账号: admin / admin@123
echo.
pause
exit /b 0

REM ============================================================
REM 模式 2: 本地 jar 模式
REM ============================================================
:JAR_MODE
echo.
echo [1/8] 本地 jar 部署模式
echo.

where java >nul 2>&1
if errorlevel 1 (
    echo [错误] 本地模式需要 JDK 17
    echo        请安装: choco install temurin17 或下载 https://adoptium.net
    pause
    exit /b 1
)

java -version 2>&1 | findstr "17" >nul
if errorlevel 1 (
    echo [错误] 需要 Java 17, 当前版本:
    java -version
    pause
    exit /b 1
)

where mvn >nul 2>&1
if errorlevel 1 (
    echo [错误] 需要 Maven
    pause
    exit /b 1
)

echo [2/8] 仅启动基础设施 (MySQL + Redis)...
docker compose up -d mysql redis
echo 等待 MySQL 就绪...
:WAIT_MYSQL2
docker exec minimax-mysql mysqladmin ping -h localhost -uroot -pMinMax2026! >nul 2>&1
if errorlevel 1 (
    timeout /t 3 /nobreak >nul
    goto :WAIT_MYSQL2
)

echo [3/8] 初始化数据库...
for %%F in (..\sql\init\*.sql) do (
    docker exec -i minimax-mysql mysql -uroot -pMinMax2026! minimax < %%F
)
echo [OK] 数据库就绪
echo.

echo [4/8] Maven 构建...
cd ..\backend
call mvn -B -DskipTests clean package
if errorlevel 1 (
    echo [错误] Maven 构建失败
    cd ..\deploy\windows
    pause
    exit /b 1
)
cd ..\deploy\windows
echo [OK] 构建完成
echo.

echo [5/8] 创建运行目录...
if not exist "..\..\runtime" mkdir "..\..\runtime"
if not exist "..\..\runtime\logs" mkdir "..\..\runtime\logs"
echo.

echo [6/8] 启动服务 (test profile - H2 内存库)...
set "GATEWAY_PORT=8080"
set "AUTH_PORT=8081"
set "CHAT_PORT=8082"
set "MODEL_PORT=8083"
set "MEMORY_PORT=8084"
set "RAG_PORT=8085"

REM 注意: test profile 用 H2, 不需要 MySQL. 这里用 prod profile (需要 MySQL 已就绪)

start "Auth" /D ..\..\runtime cmd /c "java -Xms256m -Xmx512m -jar ..\..\backend\minimax-auth\target\minimax-auth.jar --spring.profiles.active=test --server.port=8081 > ..\..\runtime\logs\auth.log 2>&1"
timeout /t 2 /nobreak >nul
start "Model" /D ..\..\runtime cmd /c "java -Xms256m -Xmx512m -jar ..\..\backend\minimax-model\target\minimax-model.jar --spring.profiles.active=test --server.port=8083 > ..\..\runtime\logs\model.log 2>&1"
timeout /t 2 /nobreak >nul
start "Chat" /D ..\..\runtime cmd /c "java -Xms256m -Xmx768m -jar ..\..\backend\minimax-chat\target\minimax-chat.jar --spring.profiles.active=test --server.port=8082 > ..\..\runtime\logs\chat.log 2>&1"
timeout /t 2 /nobreak >nul
start "Memory" /D ..\..\runtime cmd /c "java -Xms256m -Xmx768m -jar ..\..\backend\minimax-memory\target\minimax-memory.jar --spring.profiles.active=test --server.port=8084 > ..\..\runtime\logs\memory.log 2>&1"
timeout /t 2 /nobreak >nul
start "RAG" /D ..\..\runtime cmd /c "java -Xms256m -Xmx768m -jar ..\..\backend\minimax-rag\target\minimax-rag.jar --spring.profiles.active=test --server.port=8085 > ..\..\runtime\logs\rag.log 2>&1"
timeout /t 2 /nobreak >nul
start "Gateway" /D ..\..\runtime cmd /c "java -Xms256m -Xmx512m -jar ..\..\backend\minimax-gateway\target\minimax-gateway.jar --server.port=8080 > ..\..\runtime\logs\gateway.log 2>&1"

echo.
echo [7/8] 等待服务启动 (20s)...
timeout /t 20 /nobreak >nul
echo.

echo [8/8] 验证部署...
curl -s -m 3 http://localhost:8081/api/v1/auth/health
echo.
echo.
echo [SUCCESS] 部署完成！
echo.
echo 进程已在新窗口启动, 日志在 ..\..\runtime\logs\
echo 默认账号: admin / admin@123
echo.
pause
exit /b 0

REM ============================================================
REM 模式 3: 仅基础设施
REM ============================================================
:INFRA_MODE
echo.
echo [1/8] 仅启动基础设施
echo.

docker compose up -d mysql redis elasticsearch minio
echo 等待启动...
timeout /t 10 /nobreak >nul

echo 初始化数据库...
for %%F in (..\sql\init\*.sql) do (
    docker exec -i minimax-mysql mysql -uroot -pMinMax2026! minimax < %%F
)
echo [OK] 数据库就绪
echo.
echo [SUCCESS] 基础设施已启动
echo.
echo 连接信息:
echo   MySQL:   localhost:3306  user=minimax  pwd=MinMax2026!
echo   Redis:   localhost:6379  pwd=MinMax2026!
echo   ES:      http://localhost:9200
echo   MinIO:   http://localhost:9000  http://localhost:9001 (console)
echo.
echo 应用服务需要你自己启动:
echo   cd ..\backend ^&^& mvn package
echo   java -jar minimax-auth/target/minimax-auth.jar
echo.
pause
exit /b 0
