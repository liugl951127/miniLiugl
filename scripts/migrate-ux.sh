#!/bin/bash
# V6.7+ UX 批量迁移脚本
# - 给所有视图加 BackToTop (统一体验)
# - 给视图加 PageEnhancer (如果有 page-header 段)

set -e
VIEWS_DIR="${1:-frontend/src/views}"

# 1. 找所有 .vue
count=0
for f in $(find "$VIEWS_DIR" -name "*.vue" -type f); do
    # 已包含 BackToTop 跳过
    if grep -q "BackToTop" "$f"; then
        continue
    fi
    
    # 找 import 区
    last_import=$(grep -n "^import" "$f" | tail -1 | cut -d: -f1)
    if [ -z "$last_import" ]; then continue; fi
    
    # 插入 BackToTop import
    sed -i "${last_import}a import BackToTop from '@/components/BackToTop.vue'" "$f"
    
    # 找 </template> 前插入 <BackToTop />
    # 找最后一个 </template>
    last_template=$(grep -n "^</template>" "$f" | tail -1 | cut -d: -f1)
    if [ -z "$last_template" ]; then continue; fi
    
    # 插入 BackToTop
    sed -i "${last_template}i\    <BackToTop />" "$f"
    
    count=$((count + 1))
done

echo "✓ 已给 $count 个视图加 BackToTop"
