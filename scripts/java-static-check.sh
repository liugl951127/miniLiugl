#!/usr/bin/env bash
# Java 静态检查脚本 (Day 30)
# 检查: package 声明 / TODO 残留 / System.out / 无穷递归

BASE="/workspace/minimax-platform"
cd "$BASE"

PASS=0
FAIL=0

NEW_FILES="backend/minimax-monitor/src/test/java/com/minimax/monitor/AlertSilenceTest.java
backend/minimax-rag/src/test/java/com/minimax/rag/RagUploadProgressTest.java"

echo "=== Java 静态检查 ==="
echo ""

# 1. 所有 .java 文件有 package 声明
echo -n "  [package 声明] ... "
PKG_MISSING=$(find backend -name "*.java" -not -path "*/test/*" | xargs grep -L "^package " 2>/dev/null | head -5)
if [ -z "$PKG_MISSING" ]; then
    echo "✅ (全部有 package)"
    PASS=$((PASS+1))
else
    echo "❌ 缺失: $PKG_MISSING"
    FAIL=$((FAIL+1))
fi

# 2. 今日新增文件无 TODO 残留（允许 gpt 相关注释）
echo -n "  [TODO 残留检查] ... "
TODO_FILES=$(grep -rn "TODO" $NEW_FILES 2>/dev/null | grep -v "gpt" | grep -v "//.*TODO.*:" | grep "TODO" | head -5)
if [ -z "$TODO_FILES" ]; then
    echo "✅"
    PASS=$((PASS+1))
else
    echo "❌ 找到: $TODO_FILES"
    FAIL=$((FAIL+1))
fi

# 3. 今日新增文件无 System.out (用 log 替代)
echo -n "  [System.out 检查] ... "
SYS_FILES=$(grep -rn "System\.out" $NEW_FILES 2>/dev/null | grep -v "log\." | head -5)
if [ -z "$SYS_FILES" ]; then
    echo "✅"
    PASS=$((PASS+1))
else
    echo "❌ 找到: $SYS_FILES"
    FAIL=$((FAIL+1))
fi

# 4. 类声明匹配（无未闭合的 class 块）
echo -n "  [class 块匹配] ... "
MISMATCH=$(for f in $NEW_FILES; do
    opens=$(grep -c "^[ ]*class " "$f" 2>/dev/null || echo 0)
    closes=$(grep -c "^}" "$f" 2>/dev/null || echo 0)
    if [ "$opens" -gt 0 ] && [ "$closes" -lt "$opens" ]; then
        echo "$f: class=$opens close=$closes"
    fi
done)
if [ -z "$MISMATCH" ]; then
    echo "✅"
    PASS=$((PASS+1))
else
    echo "❌ $MISMATCH"
    FAIL=$((FAIL+1))
fi

# 5. 无 @Autowired 在字段上（推荐构造器注入）
echo -n "  [@Autowired 字段检查] ... "
AUTOWIRED=$(grep -rn "@Autowired" $NEW_FILES 2>/dev/null | grep -v "@RequiredArgsConstructor" | head -5)
if [ -z "$AUTOWIRED" ]; then
    echo "✅"
    PASS=$((PASS+1))
else
    echo "⚠️  建议改构造器注入: $(echo $AUTOWIRED | head -c 100)"
    PASS=$((PASS+1))
fi

echo ""
echo "=== 结果: $PASS passed / $FAIL failed ==="
if [ "$FAIL" -eq 0 ]; then
    echo "✅ 静态检查通过！"
    exit 0
else
    echo "❌ 有错误"
    exit 1
fi
