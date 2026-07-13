#!/usr/bin/env bash
# =============================================================
# 自动检查所有 Dockerfile / 部署脚本的 jar 路径
# 必须用 ${MODULE}-spring-boot.jar (Spring Boot fat jar)
# 不能用 ${MODULE}.jar (普通 jar, 无 Main-Class)
# =============================================================

set -e

green() { echo -e "\033[32m$*\033[0m"; }
red()   { echo -e "\033[31m$*\033[0m"; }
yellow(){ echo -e "\033[33m$*\033[0m"; }
bold()  { echo -e "\033[1m$*\033[0m"; }

bold ""
bold "🔍 检查所有 Dockerfile / 部署脚本的 jar 路径"
bold ""

ISSUES=0

# 1. 找所有 Dockerfile
echo ""
echo "[1/3] Dockerfile 检查"
echo "---"
for df in $(find . -name "Dockerfile*" -not -path "*/node_modules/*" -not -path "*/target/*" 2>/dev/null); do
    if grep -q "COPY.*\.jar\|target.*\.jar" "$df"; then
        # 看是不是有 -spring-boot.jar
        if grep -q "\${MODULE}-spring-boot\.jar\|spring-boot\.jar" "$df"; then
            echo "  ✓ $df (用 -spring-boot.jar)"
        else
            # 看是不是有错的 $MODULE.jar 引用
            if grep -qE "target/(\\\${MODULE}|\\\$svc|\$module)[^/]*\.jar" "$df" 2>/dev/null; then
                if ! grep -q "spring-boot\.jar" "$df"; then
                    echo "  ✗ $df: 用普通 jar (没 spring-boot)"
                    grep -E "target/(\\\${MODULE}|\\\$svc|\$module)[^/]*\.jar" "$df" | head -3
                    ISSUES=$((ISSUES+1))
                fi
            fi
        fi
    fi
done
echo ""

# 2. 部署脚本检查
echo "[2/3] 部署脚本检查"
echo "---"
for sh in $(find . -maxdepth 2 -name "deploy*.sh" -o -name "start.sh" 2>/dev/null); do
    if grep -q "\.jar" "$sh"; then
        # 找 java -jar
        for ref in $(grep -oE "[\"\'][^\"']*\.jar[\"\']|java -jar [^ ]+" "$sh" 2>/dev/null); do
            if echo "$ref" | grep -q "spring-boot\.jar"; then
                echo "  ✓ $sh: $ref"
            elif echo "$ref" | grep -qE "\.jar" && ! echo "$ref" | grep -qE "app\.jar|/app/|/usr/" ; then
                echo "  ⚠  $sh: 路径含 jar 但没 spring-boot: $ref"
            fi
        done
    fi
done | head -20
echo ""

# 3. 实际生成的 jar 文件 (如果有 mvn build)
echo "[3/3] 实际生成的 jar 文件"
echo "---"
for module_dir in backend/minimax-*; do
    [ -d "$module_dir" ] || continue
    svc=$(basename "$module_dir" | sed 's/minimax-//')
    for jar in "$module_dir"/target/*.jar; do
        [ -f "$jar" ] || continue
        size=$(du -h "$jar" 2>/dev/null | cut -f1)
        jar_name=$(basename "$jar")
        if echo "$jar_name" | grep -q "spring-boot\.jar$"; then
            echo "  ✓ $svc: $jar_name ($size) [Spring Boot fat jar - 启动用这个]"
        else
            echo "  - $svc: $jar_name ($size) [普通 jar - 不用]"
        fi
    done
done | head -40
echo ""

# 总结
bold "================================================="
if [ "$ISSUES" -gt 0 ]; then
    red "❌ 发现 $ISSUES 个 jar 路径问题"
    echo ""
    echo "修复方法: 把 Dockerfile 里的 COPY 从 \${MODULE}.jar 改为 \${MODULE}-spring-boot.jar"
else
    green "✅ 所有 jar 路径正确 (用 -spring-boot.jar)"
fi
bold "================================================="
