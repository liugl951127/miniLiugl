#!/bin/bash
# V3.6.22+ 清理无用文件 (一键)
# 1. .view-*-backup 目录 (V3.5.74+ 历史备份)
# 2. vite.config.js.timestamp-* (Vite 临时)
# 3. *.v3.5.*.bak / *.vue.*.bak (历史 bak)
# 4. .env.development (跟 .env.production 重复)
# 5. .jwt-backup* (历史 JWT 备份)

set -e
cd "$(dirname "$0")/.."

echo "═══════════════════════════════════════════════════════════"
echo "  V3.6.22+ 清理无用文件"
echo "═══════════════════════════════════════════════════════════"

# 1. 删 .view-*-backup
echo ""
echo "--- 1. .view-*-backup 目录 ---"
COUNT=0
for d in frontend/.view-*-backup; do
    if [[ -d "$d" ]]; then
        SIZE=$(du -sh "$d" 2>/dev/null | awk '{print $1}')
        rm -rf "$d"
        echo "  ✓ 删 $d ($SIZE)"
        COUNT=$((COUNT+1))
    fi
done
echo "  共删 $COUNT 个目录"

# 2. 删 vite.config.js.timestamp-*
echo ""
echo "--- 2. vite.config.js.timestamp-* ---"
COUNT=0
for f in frontend/vite.config.js.timestamp-*; do
    if [[ -f "$f" ]]; then
        SIZE=$(du -sh "$f" 2>/dev/null | awk '{print $1}')
        rm -f "$f"
        echo "  ✓ 删 $f ($SIZE)"
        COUNT=$((COUNT+1))
    fi
done
echo "  共删 $COUNT 个文件"

# 3. 删 *.bak (除 .gitignore 已有规则)
echo ""
echo "--- 3. *.bak 文件 ---"
COUNT=0
while IFS= read -r f; do
    if [[ -f "$f" ]]; then
        SIZE=$(du -sh "$f" 2>/dev/null | awk '{print $1}')
        rm -f "$f"
        echo "  ✓ 删 $f ($SIZE)"
        COUNT=$((COUNT+1))
    fi
done < <(find frontend -name "*.bak" -not -path "*/node_modules/*" 2>/dev/null)
echo "  共删 $COUNT 个文件"

# 4. 删 .env.development (跟 .env.production 重复)
echo ""
echo "--- 4. .env.development ---"
if [[ -f frontend/.env.development && -f frontend/.env.production ]]; then
    rm -f frontend/.env.development
    echo "  ✓ 删 .env.development (Vite 默认加载 .env.development 已生效)"
fi

# 5. 删 .jwt-backup
echo ""
echo "--- 5. .jwt-backup* ---"
COUNT=0
for d in .jwt-backup*; do
    if [[ -d "$d" ]]; then
        SIZE=$(du -sh "$d" 2>/dev/null | awk '{print $1}')
        rm -rf "$d"
        echo "  ✓ 删 $d ($SIZE)"
        COUNT=$((COUNT+1))
    fi
done
echo "  共删 $COUNT 个目录"

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "  ✓ 清理完成"
echo "═══════════════════════════════════════════════════════════"

# 6. 加强 .gitignore
echo ""
echo "--- 6. 加强 .gitignore ---"
GITIGNORE=".gitignore"
if ! grep -q "vite.config.js.timestamp" "$GITIGNORE"; then
    cat >> "$GITIGNORE" << 'EOF'

# V3.6.22+ 临时文件
vite.config.js.timestamp-*
.env.development
.eslintcache
EOF
    echo "  ✓ .gitignore 加 vite.config.js.timestamp-* / .env.development / .eslintcache"
fi
