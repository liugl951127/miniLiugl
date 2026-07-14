#!/usr/bin/env bash
# 同步 seed-data.sql 到所有模块 + 转换为 H2 格式
# 用法: bash scripts/sync-seed-data.sh

set -e

ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"

SRC="sql/seed-data.sql"
H2_SRC="sql/seed-data-h2.sql"

# 转 H2 格式 (从 MySQL -> H2)
python3 << 'PYEOF'
import re

with open('sql/seed-data.sql', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. 移除 SET NAMES / SET FOREIGN_KEY_CHECKS (H2 不支持 SET NAMES utf8mb4)
content = re.sub(r'^SET NAMES.*$', '', content, flags=re.MULTILINE)
content = re.sub(r'^SET FOREIGN_KEY_CHECKS.*$', '', content, flags=re.MULTILINE)

# 2. 移除 ENGINE=InnoDB (H2 不支持, 但其实 seed-data 没有这个, 跳过)
# 3. H2 不支持 ON DUPLICATE KEY UPDATE 等 (seed 没有)

with open('sql/seed-data-h2.sql', 'w', encoding='utf-8') as f:
    f.write(content)
print("  ✓ 生成 sql/seed-data-h2.sql")
PYEOF

# 复制到所有模块
for m in admin agent ai analytics auth chat common function gateway memory model monitor multimodal pipeline prompt rag ws; do
    DEST="backend/minimax-$m/src/main/resources/seed-data.sql"
    DEST_H2="backend/minimax-$m/src/main/resources/seed-data-h2.sql"
    cp "$SRC" "$DEST"
    cp "$H2_SRC" "$DEST_H2"
    echo "  ✓ 复制到 $DEST"
done

echo ""
echo "  ✅ seed-data.sql 已同步到 17 模块"
echo "  ✅ seed-data-h2.sql 已生成 (H2 沙箱模式用)"
