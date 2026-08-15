#!/bin/bash
# 给视图加 PageEnhancer 包装
set -e
VIEWS_DIR="${1:-frontend/src/views}"

count=0
for f in $(find "$VIEWS_DIR" -name "*.vue" -type f); do
    # 已包含 PageEnhancer 跳过
    if grep -q "PageEnhancer" "$f"; then
        continue
    fi
    
    # 找有 page-header 的
    if ! grep -q "page-header" "$f"; then
        continue
    fi
    
    # 找第一个 <div class="page-XXX"
    # 看是否有 <template>
    template_line=$(grep -n "^<template>" "$f" | head -1 | cut -d: -f1)
    if [ -z "$template_line" ]; then continue; fi
    
    # 在 <template> 后插入 PageEnhancer 开放
    # 实际: 用 <PageEnhancer 包裹整个 div
    # 简化: 加 <PageEnhancer> 在第一个 <div class=page-...> 之前
    # 找 page-xxx class
    page_line=$(grep -n '<div class="page-' "$f" | head -1 | cut -d: -f1)
    if [ -z "$page_line" ]; then continue; fi
    
    # 找 <PageEnhancer :title=...  </PageEnhancer> 之后插入
    # 看是否能从 page-title 提取 title
    page_title=$(grep -oE 'class="page-title">[^<]*' "$f" | head -1 | sed 's/.*>//')
    if [ -z "$page_title" ]; then
        page_title=$(basename "$f" .vue)
    fi
    
    # 简化: 加 <PageEnhancer :title="..." 包裹
    # 实际只加最简单: 在 <div class="page-xxx"> 后第一行插入 PageEnhancer
    # 这里只插入 <PageEnhancer :title="$page_title">, 然后在 </div></template> 前插入 </PageEnhancer>
    
    # 用 awk 插入
    # 找 import 区
    last_import=$(grep -n "^import" "$f" | tail -1 | cut -d: -f1)
    if [ -z "$last_import" ]; then continue; fi
    
    # 加 import
    if ! grep -q "import PageEnhancer" "$f"; then
        sed -i "${last_import}a import PageEnhancer from '@/components/PageEnhancer.vue'" "$f"
    fi
    
    # 找 <div class="page-XXX">  - 替换为 <PageEnhancer :title="...">  + <div>
    # 简化: 在 page-line 后插入 <PageEnhancer :title="...">
    # 不替换原 div, 简单插入
    sed -i "${page_line}a\    <PageEnhancer :title=\"'${page_title}'\" :subtitle=\"t('${page_title}.subtitle')\" icon=\"📊\">" "$f"
    
    # 找最后一个 </div> 在 </template> 前 - 加 </PageEnhancer>
    # 找最后 </template>
    last_template=$(grep -n "^</template>" "$f" | tail -1 | cut -d: -f1)
    if [ -z "$last_template" ]; then continue; fi
    
    # 在 last_template 前 1 行 (其实是倒序) - 实际用 sed -i 'N;...' 难
    # 用 perl
    perl -i -pe "if(\$. == $((last_template - 1))) { print qq(    </PageEnhancer>\n); }" "$f"
    
    # 删 BackToTop 因为已经在 PageEnhancer 内
    # 跳过
    
    count=$((count + 1))
done

echo "✓ 给 $count 个视图加 PageEnhancer"
