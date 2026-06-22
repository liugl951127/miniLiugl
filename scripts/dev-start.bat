@echo off
REM =============================================================
REM MiniMax Platform - Windows IDEA 本地一键启动 (V5.30)
REM
REM 流程:
REM   1. 检查 Java 17 / Maven / Node
REM   2. 启动中间件 (MySQL + Redis + Nacos) via Docker
REM   3. 编译后端 (mvn install -DskipTests)
REM   4. 启动后端 (12 个微服务 + gateway, 后台窗口)
REM   5. 启动前端 (npm run dev)
REM
REM 用法:
REM   .\scripts\dev-start.bat                启动所有
REM   .\scripts\dev-start.bat backend        只启动后端
REM   .\scripts\dev-start.bat frontend       只启动前端
REM   .\scripts\dev-start.bat middleware     只启动中间件
REM   .\scripts\dev-start.bat stop           停止所有
REM =============================================================

setlocal enabledelayedexpansion

REM ============================================================
REM 配置
REM ============================================================
set "PROJECT_ROOT=%~dp0.."
set "APP_DIR=%PROJECT_ROOT%\backend\minimax-*-target"
set "LOG_DIR=%PROJECT_ROOT%\logs"
set "MYSQL_PORT=3306"
set "REDIS_PORT=6379"
set "NACOS_PORT=8848"

REM ============================================================
REM 工具函数
REM ============================================================
:log_info
echo [INFO] %~1
goto :eof

:log_warn
echo [WARN] %~1
goto :eof

:log_error
echo [ERROR] %~1
goto :eof

REM ============================================================
REM 检查环境
REM ============================================================
:check_env
call :log_info "检查环境..."

where java >nul 2>&1
if %errorlevel% neq 0 (
    call :log_error "Java 未安装, 请装 JDK 17+"
    exit /b 1
)

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    call :log_error "Maven 未安装"
    exit /b 1
)

where docker >nul 2>&1
if %errorlevel% neq 0 (
    call :log_warn "Docker 未安装, 中间件需手动启动"
)

where node >nul 2>&1
if %errorlevel% neq 0 (
    call :log_error "Node 未安装, 请装 Node 18+"
    exit /b 1
)

call :log_info "Java: %java_version%"
call :log_info "Maven: OK"
call :log_info "Node: OK"
goto :eof

REM ============================================================
REM 启动中间件 (Docker)
REM ============================================================
:start_middleware
call :log_info "启动中间件 (MySQL + Redis + Nacos)..."

if not exist "%PROJECT_ROOT%\docker-compose.yml" (
    call :log_error "未找到 docker-compose.yml"
    exit /b 1
)

docker compose up -d mysql redis nacos
if %errorlevel% neq 0 (
    call :log_error "中间件启动失败"
    exit /b 1
)

call :log_info "等 MySQL 就绪 (30s)..."
timeout /t 30 /nobreak >nul

call :log_info "中间件启动完成"
call :log_info "  MySQL:  127.0.0.1:%MYSQL_PORT%  (root/minimax_root_2024)"
call :log_info "  Redis:  127.0.0.1:%REDIS_PORT%"
call :log_info "  Nacos:  http://localhost:%NACOS_PORT%/nacos"
goto :eof

REM ============================================================
REM 编译后端
REM ============================================================
:build_backend
call :log_info "编译后端 (mvn install -DskipTests)..."
cd /d "%PROJECT_ROOT%\backend"

call mvn clean install -DskipTests -Dspotless.check.skip=true -Djacoco.skip=true
if %errorlevel% neq 0 (
    call :log_error "编译失败"
    exit /b 1
)

call :log_info "编译完成"
goto :eof

REM ============================================================
REM 启动后端 (12 微服务 + gateway)
REM ============================================================
:start_backend
call :log_info "启动后端服务..."

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

REM 微服务列表 (模块名:端口)
set "SERVICES=auth:8081 chat:8082 model:8083 memory:8084 rag:8085 function:8086 admin:8087 multimodal:8088 monitor:8089 agent:8090 prompt:8091 ws:8095 gateway:8080"

REM 启动顺序: nacos -> gateway -> 12 微服务 (避免 nacos 还没起来就注册)
call :log_info "等 15s 让 Nacos 完全就绪..."
timeout /t 15 /nobreak >nul

for %%S in (%SERVICES%) do (
    for /f "tokens=1,2 delims=:" %%a in ("%%S") do (
        set "MODULE=%%a"
        set "PORT=%%b"
        call :start_service !MODULE! !PORT!
    )
)

call :log_info "后端启动完成"
goto :eof

:start_service
set "MODULE=%~1"
set "PORT=%~2"
set "JAR=%PROJECT_ROOT%\backend\minimax-%MODULE%\target\minimax-%MODULE%.jar"

if not exist "%JAR%" (
    call :log_warn "  %MODULE% jar 不存在, 跳过"
    goto :eof
)

call :log_info "  启动 %MODULE% (端口 %PORT%)..."

REM 用 start /b 后台启动, 重定向日志到文件
set "LOG_FILE=%LOG_DIR%\%MODULE%.log"

REM 设置 Spring 配置环境变量
set "SERVER_PORT=%PORT%"
set "NACOS_HOST=127.0.0.1"
set "NACOS_PORT=%NACOS_PORT%"
set "MYSQL_HOST=127.0.0.1"
set "MYSQL_PORT=%MYSQL_PORT%"
set "MYSQL_USER=minimax"
set "MYSQL_PASSWORD=minimax_pass_2024"
set "REDIS_HOST=127.0.0.1"
set "REDIS_PORT=%REDIS_PORT%"
set "REDIS_PASSWORD=minimax_redis_2024"
set "MINIMAX_JWT_SECRET=0f6beadebfcee3e97845856757a3babf97b2af8c80f0b95690783ccc7a595352"

start /b "minimax-%MODULE%" cmd /c "java -Xms256m -Xmx512m ^
    -Dspring.profiles.active=dev ^
    -jar \"%JAR%\" > \"%LOG_FILE%\" 2>&1"

goto :eof

REM ============================================================
REM 启动前端
REM ============================================================
:start_frontend
call :log_info "启动前端 (npm run dev)..."
cd /d "%PROJECT_ROOT%\frontend"

if not exist "node_modules" (
    call :log_info "  安装 npm 依赖 (首次较慢)..."
    call npm install --registry=https://registry.npmmirror.com
)

call npm run dev
goto :eof

REM ============================================================
REM 停止所有
REM ============================================================
:stop_all
call :log_info "停止所有服务..."

REM 杀 Java 进程 (minimax-* 模块)
taskkill /F /FI "WINDOWTITLE eq minimax-*" >nul 2>&1
taskkill /F /IM java.exe /FI "MEMUSAGE gt 50000" >nul 2>&1

REM 停 docker 中间件
cd /d "%PROJECT_ROOT%"
docker compose stop

call :log_info "已停止"
goto :eof

REM ============================================================
REM 主入口
REM ============================================================
set "ACTION=%~1"

if "%ACTION%"=="" goto :full_start
if "%ACTION%"=="all" goto :full_start
if "%ACTION%"=="backend" goto :backend_only
if "%ACTION%"=="frontend" goto :frontend_only
if "%ACTION%"=="middleware" goto :middleware_only
if "%ACTION%"=="stop" goto :stop_all
if "%ACTION%"=="help" goto :show_help
if "%ACTION%"=="-h" goto :show_help

call :log_error "未知命令: %ACTION%"
goto :show_help

:full_start
call :check_env
if %errorlevel% neq 0 exit /b 1
call :start_middleware
if %errorlevel% neq 0 exit /b 1
call :build_backend
if %errorlevel% neq 0 exit /b 1
call :start_backend
if %errorlevel% neq 0 exit /b 1
call :start_frontend
goto :end

:backend_only
call :check_env
if %errorlevel% neq 0 exit /b 1
call :start_middleware
if %errorlevel% neq 0 exit /b 1
call :build_backend
if %errorlevel% neq 0 exit /b 1
call :start_backend
goto :end

:frontend_only
call :check_env
if %errorlevel% neq 0 exit /b 1
call :start_frontend
goto :end

:middleware_only
call :start_middleware
goto :end

:show_help
echo MiniMax Platform V5.30 - Windows 本地一键启动
echo.
echo 用法:
echo   dev-start.bat                  启动全部 (中间件 + 后端 + 前端)
echo   dev-start.bat backend          只启动中间件 + 后端
echo   dev-start.bat frontend         只启动前端
echo   dev-start.bat middleware       只启动中间件
echo   dev-start.bat stop             停止所有
echo.
echo 访问:
echo   前端:   http://localhost:5173
echo   后端:   http://localhost:8080  (gateway)
echo   Nacos:  http://localhost:8848/nacos
echo.
echo 日志: %PROJECT_ROOT%\logs\
goto :end

:end
endlocal