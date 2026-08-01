#!/bin/bash
# V3.6.25+ mvn 沙箱友好入口 (docker 跑 mvn)
set -e
cd "$(dirname "$0")/.."

MVN_IMAGE="${MVN_IMAGE:-maven:3.9-eclipse-temurin-17}"
BACKEND_DIR="$(pwd)/backend"

echo "═══════════════════════════════════════════════════════════"
echo "  V3.6.25+ mvn 沙箱友好 (docker 跑 $MVN_IMAGE)"
echo "═══════════════════════════════════════════════════════════"

if ! command -v docker &> /dev/null; then
    echo "❌ 沙箱无 docker"
    echo "替代:"
    echo "  1. CI 跑 mvn (V3.5.65+ backend job)"
    echo "  2. 本地: apt-get install -y openjdk-17-jdk maven"
    echo "  3. 静态检查: bash scripts/check_pom_consistency.py"
    exit 1
fi

if ! docker info &> /dev/null 2>&1; then
    echo "❌ docker daemon 不可用"
    exit 1
fi

echo "✓ docker 可用 - 跑 mvn..."
docker run --rm \
    -v "$BACKEND_DIR:/app" \
    -v "$HOME/.m2:/root/.m2" \
    -w /app \
    -e MAVEN_OPTS="-Xmx2g" \
    "$MVN_IMAGE" \
    mvn "${@:---version}"
