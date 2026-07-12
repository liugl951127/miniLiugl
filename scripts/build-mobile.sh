#!/usr/bin/env bash
#
# Capacitor 移动端一键构建脚本 (V3.1.1)
#
# 用法:
#   ./scripts/build-mobile.sh ios       # 构建 iOS (需 macOS + Xcode)
#   ./scripts/build-mobile.sh android   # 构建 Android (需 JDK 17 + Android SDK)
#   ./scripts/build-mobile.sh both      # 两个都构建
#   ./scripts/build-mobile.sh sync      # 只同步 web 资源到原生项目
#
# 流程:
#   1. 前端 npm run build (Vite 打包)
#   2. cap copy (同步 dist 到 iOS/Android 项目)
#   3. cap sync (更新插件)
#   4. 调用原生工具 (Xcode / Android Studio / Gradle)
#

set -e

# 颜色
C_GREEN='\033[0;32m'
C_RED='\033[0;31m'
C_YEL='\033[0;33m'
C_BLU='\033[0;34m'
C_OFF='\033[0m'

log()   { echo -e "${C_GREEN}[$(date +%H:%M:%S)]${C_OFF} $*"; }
warn()  { echo -e "${C_YEL}[$(date +%H:%M:%S)]${C_OFF} $*"; }
err()   { echo -e "${C_RED}[$(date +%H:%M:%S)]${C_OFF} $*" >&2; }
hr()    { echo -e "${C_BLU}=============================================${C_OFF}"; }

# 路径
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend"
cd "$FRONTEND_DIR"

# 1. 预检
precheck() {
  hr
  log "预检..."
  # 1.1 node
  if ! command -v node >/dev/null 2>&1; then
    err "node 未安装, 请先装 Node.js 18+"
    exit 1
  fi
  # 1.2 npm
  if ! command -v npm >/dev/null 2>&1; then
    err "npm 未安装"
    exit 1
  fi
  # 1.3 node_modules
  if [ ! -d "node_modules" ]; then
    warn "node_modules 缺失, 装依赖..."
    npm ci --prefer-offline --no-audit --no-fund
  fi
  log "预检通过"
}

# 2. Web 打包
build_web() {
  hr
  log "1/4 Web 打包 (Vite)..."
  npm run build
  if [ ! -d "dist" ]; then
    err "dist/ 未生成, 打包失败"
    exit 1
  fi
  log "Web 打包完成: $(du -sh dist | cut -f1)"
}

# 3. cap sync
cap_sync() {
  hr
  log "2/4 cap sync (同步 web 资源 + 插件)..."
  npx cap sync
  log "cap sync 完成"
}

# 4. iOS 构建
build_ios() {
  hr
  log "3/4 iOS 构建..."
  if [[ "$OSTYPE" != "darwin"* ]]; then
    warn "非 macOS, 跳过 iOS 构建 (需在 macOS + Xcode 上执行)"
    warn "可手动 cd ios/App && pod install && xcodebuild"
    return
  fi
  # 1. 检查 Xcode
  if ! command -v xcodebuild >/dev/null 2>&1; then
    warn "xcodebuild 未安装, 跳过"
    return
  fi
  # 2. 检查 ios 目录
  if [ ! -d "ios" ]; then
    warn "ios/ 目录不存在, 执行 cap add ios..."
    npx cap add ios
  fi
  # 3. pod install
  if [ -d "ios/App" ]; then
    cd ios/App
    if command -v pod >/dev/null 2>&1; then
      pod install
    fi
    cd "$FRONTEND_DIR"
  fi
  # 4. xcodebuild (Release)
  log "开始 xcodebuild Release..."
  cd ios
  xcodebuild -workspace App/App.xcworkspace -scheme App -configuration Release -sdk iphonesimulator -derivedDataPath build
  cd "$FRONTEND_DIR"
  log "iOS 构建完成 (Release iphonesimulator)"
}

# 5. Android 构建
build_android() {
  hr
  log "4/4 Android 构建..."
  # 1. 检查 JDK 17
  if ! command -v java >/dev/null 2>&1; then
    warn "java 未安装, 跳过 Android 构建"
    return
  fi
  if ! java -version 2>&1 | grep -q '"17'; then
    warn "需要 JDK 17, 当前: $(java -version 2>&1 | head -1)"
    return
  fi
  # 2. android 目录
  if [ ! -d "android" ]; then
    warn "android/ 目录不存在, 执行 cap add android..."
    npx cap add android
  fi
  # 3. gradlew
  if [ ! -f "android/gradlew" ]; then
    warn "android/gradlew 不存在"
    return
  fi
  # 4. assembleRelease
  log "开始 gradlew assembleRelease..."
  cd android
  ./gradlew assembleRelease --no-daemon --stacktrace
  cd "$FRONTEND_DIR"
  log "Android APK 构建完成: android/app/build/outputs/apk/release/"
  ls -lh android/app/build/outputs/apk/release/*.apk 2>/dev/null || true
}

# 主流程
main() {
  precheck
  case "${1:-sync}" in
    ios)
      build_web
      cap_sync
      build_ios
      ;;
    android)
      build_web
      cap_sync
      build_android
      ;;
    both)
      build_web
      cap_sync
      build_ios
      build_android
      ;;
    sync)
      build_web
      cap_sync
      ;;
    web)
      build_web
      ;;
    *)
      echo "用法: $0 {ios|android|both|sync|web}"
      exit 1
      ;;
  esac
  hr
  log "完成 ✓"
}

main "$@"
