#!/bin/bash
cd /workspace/miniLiugl/frontend
# 把 import { useI18n } from 'vue-i18n' 改为 import { useI18n } from '@/i18n'
# 注意保留: 其他从 vue-i18n 引的可能 (createI18n, i18n 之类)
for f in $(grep -rln "from 'vue-i18n'" src/ 2>/dev/null); do
  # 单 useI18n / t 的 import 改
  if grep -qE "import\s*\{[^}]*useI18n[^}]*\}\s*from\s*'vue-i18n'" "$f"; then
    # 检查是否还有其他从 vue-i18n 引
    if grep -qE "createI18n|useI18n.*[\{,].*[a-zA-Z]" "$f"; then
      # 复杂 import, 不动
      continue
    fi
    # 替换 useI18n
    sed -i "s|import\s*{\s*useI18n\s*}\s*from\s*'vue-i18n';|import { useI18n } from '@/i18n';|g" "$f"
    echo "✓ $f"
  fi
done
