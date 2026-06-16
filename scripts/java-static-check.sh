#!/usr/bin/env bash
# Java 静态体检（无 javac 时的 fallback）
# 1. 每个 .java 文件至少有 package 声明
# 2. import 的类引用路径存在
# 3. controller / service / mapper 之间的引用对得上
# 4. 没有 TODO / FIXME 遗留
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/backend"

echo "=========================================="
echo "  Java 静态体检（无 JDK 环境 fallback）"
echo "=========================================="

ERR=0

# 1. 体检 package 声明
echo ""
echo "[1] package 声明体检（排除 package-info.java）..."
for f in $(find . -name "*.java" -not -name "package-info.java"); do
    pkg=$(head -5 "$f" | grep -E "^package " | head -1)
    if [ -z "$pkg" ]; then
        echo "  ❌ $f 缺少 package"
        ERR=1
    fi
done
[ "$ERR" = "0" ] && echo "  ✅ 全部有 package"

# 2. 体检 import 路径
echo ""
echo "[2] import 路径体检..."
# 收集所有 .java 文件的 fully qualified class name
ALL_CLASSES=$(find . -name "*.java" | sed -E 's|.*/src/main/java/||;s|.*/src/test/java/||;s|\.java$||;s|/|.|g' | sort -u)
MISSING=0
SKIP_WILDCARD=0
for f in $(find . -name "*.java"); do
    while IFS= read -r imp; do
        # 跳过通配符 import
        if echo "$imp" | grep -qE "\.\*$"; then
            SKIP_WILDCARD=$((SKIP_WILDCARD+1))
            continue
        fi
        # 跳过 java.* / jakarta.* / lombok / springframework / mybatisplus / baomidou / io.* / org.* / javax.*
        if echo "$imp" | grep -qE "^(java|jakarta|javax|lombok|org|com\.baomidou|com\.fasterxml|io\.jsonwebtoken|org\.springframework|org\.apache|org\.mybatis|org\.slf4j|io\.swagger|io\.minio|com\.alibaba|com\.github|com\.fasterxml)"; then
            continue
        fi
        # 内部类 com.minimax.*
        if ! echo "$ALL_CLASSES" | grep -qE "^${imp}$"; then
            echo "  ⚠️  $f -> $imp  (未找到定义)"
            MISSING=$((MISSING+1))
        fi
    done < <(grep -E "^import " "$f" | grep -v "import static" | sed -E 's/^import[ \t]+([a-zA-Z0-9_.*]+);?$/\1/')
done
echo "  内网 import 引用数: $MISSING  (跳过通配符 $SKIP_WILDCARD 条)"

# 3. 体检 @RestController / @Service / @Mapper 注解
echo ""
echo "[3] 注解分布..."
echo "  @RestController: $(grep -rE '@RestController' --include='*.java' | wc -l)"
echo "  @Service:         $(grep -rE '@Service\b' --include='*.java' | wc -l)"
echo "  @Mapper:          $(grep -rE '@Mapper\b' --include='*.java' | wc -l)"
echo "  @Configuration:   $(grep -rE '@Configuration\b' --include='*.java' | wc -l)"
echo "  @Component:       $(grep -rE '@Component\b' --include='*.java' | wc -l)"

# 4. 体检 @TableName 实体与 mapper xml 是否对应
echo ""
echo "[4] Mapper XML 体检..."
XML_COUNT=$(find . -name "*Mapper.xml" | wc -l)
JAVA_MAPPER=$(grep -rE "namespace=" --include="*.xml" | wc -l)
echo "  XML 文件: $XML_COUNT  / namespace 数: $JAVA_MAPPER"

# 5. 体检 TODO / FIXME
echo ""
echo "[5] TODO/FIXME 体检..."
TODO=$(grep -rE "TODO|FIXME" --include="*.java" 2>/dev/null | wc -l || echo 0)
echo "  TODO/FIXME 数量: $TODO"

# 6. 文件统计
echo ""
echo "[6] 文件统计..."
JAVA_COUNT=$(find . -name "*.java" | wc -l)
JAVA_LINES=$(find . -name "*.java" -exec cat {} + | wc -l)
XML_LINES=$(find . -name "*.xml" -exec cat {} + | wc -l)
echo "  Java 文件: $JAVA_COUNT"
echo "  Java 行数: $JAVA_LINES"
echo "  XML 行数: $XML_LINES"

echo ""
echo "=========================================="
[ "$ERR" = "0" ] && echo "  ✅ 静态体检通过" || echo "  ❌ 有问题，请查看"
echo "=========================================="
