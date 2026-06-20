#!/bin/bash
# scripts/extract-custom-config.sh
# 从 git HEAD 提取每个模块的特殊 minimax.* 配置
# 合并到重构后的 application.yml

set -e
cd "$(cd "$(dirname "$0")/.." && pwd)"

modules=(auth chat model memory rag function admin multimodal monitor agent prompt ws)

for m in "${modules[@]}"; do
  yml="backend/minimax-${m}/src/main/resources/application.yml"

  # 从 git HEAD 拿原 yml
  orig=$(git show "HEAD:backend/minimax-${m}/src/main/resources/application.yml" 2>/dev/null)
  if [ -z "$orig" ]; then
    echo "  ❌ ${m} (no git history)"
    continue
  fi

  # 提取 minimax.* 块
  custom=$(echo "$orig" | awk '
    /^minimax:/ { in_block=1; print "minimax:"; next }
    in_block && /^[a-z]/ { in_block=0 }
    in_block { print }
  ')
  # 去 2 空格缩进 (因为顶层是 minimax:)
  custom=$(echo "$custom" | tail -n +2 | sed 's/^  //')

  if [ -z "$custom" ]; then
    echo "  = ${m} (no custom)"
    continue
  fi

  # 替换 "minimax:\n  # (无特殊配置)" 为 custom
  python3 -c "
import re
with open('$yml') as f: content = f.read()
# 删除占位
content = re.sub(r'minimax:\n  # \(无特殊配置\)\n', '', content)
# 在 '# 模块特殊配置' 后追加
if 'minimax:' in content:
    print('  ⚠ ${m} 已存在 minimax: 块, 跳过')
else:
    content = content.rstrip() + '\n\n# 模块特殊配置 (保留原 minimax.* 块)\n' + '''$custom''' + '\n'
    with open('$yml', 'w') as f: f.write(content)
    print('  ✓ ${m} (added ${custom_count} lines)')
" 2>/dev/null

  # 用更直接的方式:
  if grep -q "# 模块特殊配置" "$yml" && ! grep -q "^minimax:" "$yml" | grep -v "minimax:" | head -1; then
    # 用 sed 直接替换占位
    sed -i "s|^minimax:$|minimax:|" "$yml"
    # 在文件末尾追加 custom
    echo "" >> "$yml"
    echo "# 模块特殊配置 (从 git HEAD 恢复)" >> "$yml"
    echo "$custom" >> "$yml"
  fi
done