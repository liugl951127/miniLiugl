#!/bin/bash
# V3.6.7+ 沙箱友好 mvn (通过 Docker 跑 mvn 命令)
# 沙箱无 Java/Maven 但有 Docker 时可用
# 用法: bash scripts/mvn-docker.sh [mvn 命令] 默认 "mvn -version"

set -e
cd "$(dirname "$0")/.."

MVN_IMAGE="${MVN_IMAGE:-maven:3.9-eclipse-temurin-17}"
BACKEND_DIR="$(pwd)/backend"

if ! command -v docker &> /dev/null; then
    echo "❌ 沙箱无 docker"
    echo "替代: 直接用 CI 跑 mvn (V3.5.65+ backend job)"
    echo "或本地: apt-get install -y openjdk-17-jdk maven"
    exit 1
fi

if ! docker info &> /dev/null 2>&1; then
    echo "❌ docker daemon 不可用"
    exit 1
fi

echo "═══════════════════════════════════════════════════════════"
echo "  V3.6.7+ mvn 沙箱友好 (docker 跑 $MVN_IMAGE)"
echo "═══════════════════════════════════════════════════════════"

docker run --rm \
    -v "$BACKEND_DIR:/app" \
    -v "$HOME/.m2:/root/.m2" \
    -w /app \
    -e MAVEN_OPTS="-Xmx2g" \
    "$MVN_IMAGE" \
    mvn "${@:---version}"
