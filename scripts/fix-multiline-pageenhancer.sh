#!/bin/bash
cd /workspace/miniLiugl
for f in $(find frontend/src -name "*.vue" -not -path "*/node_modules/*" -not -path "*/dist/*"); do
  # 检查多行 PageEnhancer 自闭合
  if grep -q '<PageEnhancer\s*$' "$f" 2>/dev/null; then
    # 看是否 5 行内 self-close
    if awk '/<PageEnhancer\s*$/{found=1; n=0; next} found{n++; if(n<=6 && /\/>\s*$/){print FILENAME; exit} if(n>6){exit}}' "$f" | grep -q .; then
      echo "FIX: $f"
    fi
  fi
done
