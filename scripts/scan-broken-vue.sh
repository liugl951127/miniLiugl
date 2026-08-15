#!/bin/bash
cd /workspace/miniLiugl
count=0
for f in $(find frontend/src -name "*.vue" -not -path "*/node_modules/*" -not -path "*/dist/*"); do
  # 破坏 1: class="xxx> 缺 "
  if grep -qE 'class="[a-zA-Z][^"]*[a-zA-Z]>$' "$f" 2>/dev/null; then
    echo "BROKEN1: $f"
    count=$((count+1))
  fi
  # 破坏 2: 错误用 class="pageenhancer"> 当 PageEnhancer
  if grep -qE 'class="pageenhancer">' "$f" 2>/dev/null; then
    echo "BROKEN2: $f (pageenhancer div 替代 PageEnhancer 组件)"
    count=$((count+1))
  fi
done
echo "Total: $count"
