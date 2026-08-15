#!/bin/bash
# 找 Login.vue / H5Login.vue / chat/Index.vue 干净版本
for f in "frontend/src/views/auth/Login.vue" \
         "frontend/src/views/auth/H5Login.vue" \
         "frontend/src/views/chat/Index.vue" \
         "frontend/src/views/agent/Index.vue" \
         "frontend/src/views/knowledge/Index.vue" \
         "frontend/src/App.vue"; do
  echo "=== $f ==="
  for commit in $(git log --all --oneline | awk '{print $1}' | head -200); do
    content=$(git show $commit:$f 2>/dev/null)
    if [ -n "$content" ]; then
      # 检查是否有破坏
      if echo "$content" | grep -q "class=\"pageenhancer\">" || echo "$content" | grep -q "class=\"page-login>"; then
        continue
      fi
      if echo "$content" | grep -q "<template>"; then
        echo "  ✓ $commit"
        break
      fi
    fi
  done
done
