#!/usr/bin/env bash
# =============================================================
# 验证 Spring Boot layered jar 拆分是否成功
# 用于: deploy 脚本中检查 jar 是否能拆出 dependencies/snapshot/application 等层
# =============================================================

set -e

green() { echo -e "\033[32m$*\033[0m"; }
red()   { echo -e "\033[31m$*\033[0m"; }
yellow(){ echo -e "\033[33m$*\033[0m"; }

MODULE="${1:-ai}"
WORKSPACE="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$WORKSPACE/backend/minimax-$MODULE/target/minimax-$MODULE-spring-boot.jar"

if [ ! -f "$JAR" ]; then
  red "❌ jar 不存在: $JAR"
  exit 1
fi

yellow "🔍 验证 layered jar: minimax-$MODULE"
echo ""

# 1. 检查 jar 内部 layers.idx (Spring Boot 拆层标记)
green "  1. 检查 layers.idx (Spring Boot 拆层索引)"
unzip -p "$JAR" BOOT-INF/layers.idx 2>/dev/null | head -10 || {
  red "  ❌ 找不到 BOOT-INF/layers.idx, 说明 maven 没启用 layered"
  exit 1
}
echo ""

# 2. 拆出 layers
green "  2. 拆 layers 到临时目录"
TMP=$(mktemp -d)
java -Djarmode=layertools -jar "$JAR" extract --destination "$TMP" 2>&1 | tail -3
echo ""

# 3. 验证 4 个标准层
green "  3. 检查 4 个标准层"
LAYERS_OK=true
for layer in dependencies spring-boot-loader snapshot-dependencies application; do
  if [ -d "$TMP/$layer" ]; then
    size=$(du -sh "$TMP/$layer" 2>/dev/null | cut -f1)
    files=$(find "$TMP/$layer" -type f | wc -l)
    echo "    ✓ $layer  ($files files, $size)"
  else
    echo "    ✗ $layer  (缺失)"
    LAYERS_OK=false
  fi
done
echo ""

# 4. 验证 dependencies 层主要是 jar (不变)
green "  4. dependencies 层内容"
JAR_COUNT=$(find "$TMP/dependencies" -name "*.jar" 2>/dev/null | wc -l)
echo "    jar 文件数: $JAR_COUNT (spring + 业务依赖)"
echo ""

# 5. 验证 application 层主要是 .class (变)
green "  5. application 层内容"
CLASS_COUNT=$(find "$TMP/application" -name "*.class" 2>/dev/null | wc -l)
echo "    class 文件数: $CLASS_COUNT (业务代码)"
echo ""

# 6. 总结磁盘使用
green "  6. 拆出后总体积 (对比 fat jar)"
TOTAL=$(du -sh "$TMP" 2>/dev/null | cut -f1)
ORIG=$(du -sh "$JAR" 2>/dev/null | cut -f1)
echo "    fat jar:        $ORIG"
echo "    拆层后总:       $TOTAL"
echo "    节省:           $(du -sh "$JAR" "$TMP" 2>/dev/null | tail -1) (重复 jar 减少镜像层)"
echo ""

# 7. 模拟 docker 启动方式
green "  7. 测试 JarLauncher 启动 (5 秒超时, 仅验证 classloader)"
cd "$TMP"
timeout 5 java org.springframework.boot.loader.JarLauncher 2>&1 | head -5 || true
cd "$WORKSPACE"
echo ""

# 清理
rm -rf "$TMP"

if $LAYERS_OK; then
  green "✅ layered jar 验证通过"
  green "   镜像构建可启用分层缓存, 增量构建秒级完成"
  exit 0
else
  red "❌ layered jar 验证失败"
  exit 1
fi
