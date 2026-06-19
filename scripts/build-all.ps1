# MiniMax Platform - 一键打包 (Windows PowerShell)
# 流程: 准备 → 后端 → 前端 → 收集 → 验证
# 用法: .\scripts\build-all.ps1 [可选 -WithTest]

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
    Write-Host "  安装: choco install $cmd -y"
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
Get-ChildItem scripts\*.sh -ErrorAction SilentlyContinue | ForEach-Object { Copy-Item $_.FullName release\scripts\ }
Get-ChildItem scripts\*.bat -ErrorAction SilentlyContinue | ForEach-Object { Copy-Item $_.FullName release\scripts\ }
Get-ChildItem scripts\*.ps1 -ErrorAction SilentlyContinue | ForEach-Object { Copy-Item $_.FullName release\scripts\ }
Copy-Item README.md release\ -ErrorAction SilentlyContinue
Get-ChildItem docs\*.md -ErrorAction SilentlyContinue | ForEach-Object {
  Copy-Item $_.FullName release\docs\
}

Write-Host "✓ 产物已收集到 release\" -ForegroundColor Green

# ===== 阶段 4: 验证 =====
Write-Host "`n[4/5] 验证产物..." -ForegroundColor Yellow
$jars = Get-ChildItem release\backend\*.jar
Write-Host "  后端 jar ($($jars.Count) 个):"
$jars | ForEach-Object {
  $size = [math]::Round($_.Length / 1MB, 1)
  Write-Host "    $($_.Name) (${size}MB)"
}

$indexHtml = Test-Path release\frontend\index.html
Write-Host "  前端 index.html: $(if($indexHtml){'OK'}else{'缺失'})"
$sqlCount = (Get-ChildItem release\sql\*.sql).Count
Write-Host "  SQL: $sqlCount 个脚本"

$size = (Get-ChildItem release -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB
Write-Host "  总大小: $([math]::Round($size, 1))MB"

# ===== 阶段 5: 可选启动 =====
Write-Host "`n[5/5] 准备部署包 (可选: 启动测试)..." -ForegroundColor Yellow
if ($args -contains "-WithTest") {
  Write-Host "  启动 MariaDB..."
  if (-not (Get-Process -Name "mysqld" -ErrorAction SilentlyContinue)) {
    New-Item -ItemType Directory -Force -Path "C:\tools\mariadb\data" -ErrorAction SilentlyContinue | Out-Null
    & "C:\Program Files\MariaDB 10.5\bin\mysqld.exe" --user=mysql --bind-address=127.0.0.1 --datadir="C:\tools\mariadb\data" 2>&1 | Out-Null
    Start-Sleep -Seconds 8
  }
  Write-Host "  初始化数据库..."
  & mysql -uroot -e "CREATE DATABASE IF NOT EXISTS minimax DEFAULT CHARSET utf8mb4;" 2>$null
  & mysql -uroot -e "CREATE USER IF NOT EXISTS 'minimax'@'127.0.0.1' IDENTIFIED BY 'minimax_pass_2024'; GRANT ALL ON minimax.* TO 'minimax'@'127.0.0.1';" 2>$null
  foreach ($f in (Get-ChildItem release\sql\*.sql)) {
    & mysql -uroot minimax < $f.FullName 2>$null
  }
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  打包完成!" -ForegroundColor Green
Write-Host "  release/ 目录: $ROOT\release" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "下一步:"
Write-Host "  # 1. 压缩为 zip"
Write-Host "  Compress-Archive -Path release\* -DestinationPath release.zip"
Write-Host ""
Write-Host "  # 2. 复制到目标机器 (PowerShell 自带 scp 或用 WinSCP)"
Write-Host "  scp -r release/ user@server:/opt/minimax/"
Write-Host ""
Write-Host "  # 3. 在目标 Linux 机器启动"
Write-Host "  cd /opt/minimax && bash scripts/start-platform.sh"
Write-Host ""
Write-Host "  # 4. 访问"
Write-Host "  http://<server>:5173"
Write-Host "  登录: adminLiugl / Liugl@2026"
