#!/usr/bin/env bash
# =============================================================
# 修复所有脚本的 CRLF (Windows 换行符) → LF (Unix 换行符)
# =============================================================
# 报错信息: /usr/bin/env: 'bash\r': No such file or directory
# 原因: git 在 Windows clone 时 core.autocrlf=true 会把 LF 转 CRLF
#       或者文件被 Windows 编辑器编辑过
# =============================================================

set -e

green() { echo -e "\033[32m$*\033[0m"; }
red()   { echo -e "\033[31m$*\033[0m"; }
yellow(){ echo -e "\033[33m$*\033[0m"; }
bold()  { echo -e "\033[1m$*\033[0m"; }

bold ""
bold "🔧 修复脚本 CRLF → LF"
bold ""

# 1. 找所有含 \r 的脚本
CRLF_COUNT=0
FIXED=0

for f in $(find . -type f \( -name "*.sh" -o -name "*.bash" \) \
    -not -path "*/node_modules/*" \
    -not -path "*/target/*" \
    -not -path "*/.git/*" \
    2>/dev/null); do
    if grep -l $'\r' "$f" > /dev/null 2>&1; then
        CRLF_COUNT=$((CRLF_COUNT + 1))
        # 显示状态
        if [ "${1:-}" = "--yes" ] || [ "${FIX_ALL:-}" = "1" ]; then
            # 自动修复
            sed -i 's/\r$//' "$f"
            FIXED=$((FIXED + 1))
            green "  ✓ 修复: $f"
        else
            yellow "  ⚠  发现: $f"
        fi
    fi
done

# 2. 也检查 .conf / .yml (nginx 配置)
for f in $(find . -type f \( -name "*.conf" -o -name "*.yml" -o -name "*.yaml" \) \
    -not -path "*/node_modules/*" \
    -not -path "*/target/*" \
    -not -path "*/.git/*" \
    -not -path "*/frontend/*" \
    2>/dev/null); do
    if grep -l $'\r' "$f" > /dev/null 2>&1; then
        CRLF_COUNT=$((CRLF_COUNT + 1))
        if [ "${1:-}" = "--yes" ] || [ "${FIX_ALL:-}" = "1" ]; then
            sed -i 's/\r$//' "$f"
            FIXED=$((FIXED + 1))
            green "  ✓ 修复: $f"
        else
            yellow "  ⚠  发现: $f"
        fi
    fi
done

echo ""
if [ "$CRLF_COUNT" -eq 0 ]; then
    green "✅ 所有脚本都已经是 LF 换行符 (无需修复)"
    exit 0
fi

# 3. 如果有发现但没自动修, 询问
if [ "$FIXED" -eq 0 ]; then
    yellow "发现 $CRLF_COUNT 个文件有 CRLF 换行符"
    echo ""
    echo "  选项:"
    echo "    1) 自动修复: bash scripts/fix-crlf.sh --yes"
    echo "    2) 手动修复: sed -i 's/\\r\$//' <file>"
    exit 0
fi

# 4. 自动修复完, 设置 git config
bold ""
green "✅ 修复完成: $FIXED 个文件"
echo ""
yellow "💡 预防: 设置 git core.autocrlf 避免再次出问题"
echo ""
echo "  Linux/Mac (推荐): git config --global core.autocrlf input"
echo "  Windows (推荐):    git config --global core.autocrlf false"
echo ""
echo "  输入: 在 Windows 上保持 LF, 不要 auto 转 CRLF"
echo ""
