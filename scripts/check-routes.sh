#!/bin/bash
# V6.3+ 路由一致性检查脚本
# 检查: 后端 Controller / Gateway 路由 / 前端 API 调用
# 输出: 不匹配报告

set -e
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT="$(dirname "$SCRIPT_DIR")"
cd "$ROOT"

echo "========================================="
echo "MiniMax 路由一致性检查 (V6.3+)"
echo "========================================="
echo

# 1. 后端 Controller 路由
echo "📦 收集后端 Controller 路由..."
BACKEND_ROUTES=$(mktemp)
find backend -name "*Controller.java" -path "*/main/*" | while read f; do
    # 提取类级别 @RequestMapping
    cls=$(grep -oE '@RequestMapping\(["\x27]([^"\x27]+)["\x27]' "$f" | head -1 | sed -E 's/@RequestMapping\(["\x27]([^"\x27]+)["\x27]/\1/')
    # 提取方法级 @*Mapping
    grep -oE '@(Get|Post|Put|Delete|Patch)Mapping' "$f" | while read m; do
        if [ -n "$cls" ]; then
            echo "$cls" >> "$BACKEND_ROUTES"
        fi
    done
done
echo "  ✓ 后端 Controller 数: $(find backend -name '*Controller.java' -path '*/main/*' | wc -l)"
echo "  ✓ 后端路由数 (粗略): $(wc -l < "$BACKEND_ROUTES")"

# 2. Gateway 路由
echo "🌐 收集 Gateway 路由..."
GATEWAY_ROUTES=$(mktemp)
if [ -f backend/minimax-gateway/src/main/resources/application.yml ]; then
    # 提取 Path=...
    grep -oE 'Path=[^,\n]+' backend/minimax-gateway/src/main/resources/application.yml | \
        sed -E 's/Path=//; s/^"//; s/"$//' | tr ',' '\n' | sed 's/^[[:space:]]*//' | sort -u > "$GATEWAY_ROUTES"
    echo "  ✓ Gateway 路由数: $(wc -l < "$GATEWAY_ROUTES")"
fi

# 3. 前端 API 调用
echo "🎨 收集前端 API 调用..."
FRONTEND_URLS=$(mktemp)
grep -rEh "http\.(get|post|put|delete|patch)\([\`'\"]/[^\\\'\"\\)]+" frontend/src/api/ 2>/dev/null | \
    grep -oE "[\`'\"](/[^\`'\"\\)]+)" | sed -E "s/[\`'\"](.*)/\\1/" | sort -u > "$FRONTEND_URLS"
echo "  ✓ 前端 API 调用: $(wc -l < "$FRONTEND_URLS")"

# 4. 不匹配分析
echo
echo "========================================="
echo "🔍 不匹配分析"
echo "========================================="

# 4.1 前端调但 Gateway 没声明
NOT_IN_GATEWAY=$(comm -23 "$FRONTEND_URLS" "$GATEWAY_ROUTES" 2>/dev/null | wc -l)
echo "❌ 前端调但 Gateway 没声明: $NOT_IN_GATEWAY"
if [ "$NOT_IN_GATEWAY" -gt 0 ] && [ "$NOT_IN_GATEWAY" -lt 50 ]; then
    comm -23 "$FRONTEND_URLS" "$GATEWAY_ROUTES" 2>/dev/null | head -20
fi

# 4.2 Gateway 有但前端没调
NOT_USED=$(comm -13 "$FRONTEND_URLS" "$GATEWAY_ROUTES" 2>/dev/null | wc -l)
echo "ℹ️  Gateway 有但前端没调: $NOT_USED"

# 5. 清理
rm -f "$BACKEND_ROUTES" "$GATEWAY_ROUTES" "$FRONTEND_URLS"

echo
echo "========================================="
echo "✅ 检查完成"
echo "========================================="
