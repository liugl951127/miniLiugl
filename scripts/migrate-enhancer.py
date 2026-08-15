#!/usr/bin/env python3
"""
V6.7+ 批量给视图加 PageEnhancer 包装
"""
import os
import re
import sys
from pathlib import Path

VIEWS_DIR = Path(sys.argv[1] if len(sys.argv) > 1 else 'frontend/src/views')

def has_page_header(content):
    return 'class="page-' in content or 'class="page-header"' in content

def has_enhancer(content):
    return 'PageEnhancer' in content

def get_title(content, file_path):
    # 1. 找 page-title 内容
    m = re.search(r'class="page-title"[^>]*>([^<]+)', content)
    if m:
        text = m.group(1).strip()
        # 移除 emoji
        text = re.sub(r'[^\w\u4e00-\u9fff\s]', '', text).strip()
        return text
    # 2. 找 <h1> 或 <h2>
    m = re.search(r'<h[12][^>]*>([^<]+)', content)
    if m:
        text = m.group(1).strip()
        text = re.sub(r'[^\w\u4e00-\u9fff\s]', '', text).strip()
        return text
    return Path(file_path).stem

count_enhanced = 0
count_already = 0
for vue_file in VIEWS_DIR.rglob('*.vue'):
    content = vue_file.read_text(encoding='utf-8')
    
    if has_enhancer(content):
        count_already += 1
        continue
    
    if not has_page_header(content):
        continue
    
    title = get_title(content, vue_file)
    
    # 找第一个 <div class="page-XXX"> 位置
    m = re.search(r'(\s*<div class="page-[^"]+")', content)
    if not m:
        continue
    
    # 找 import 区
    imports = list(re.finditer(r'^import .+$', content, re.MULTILINE))
    if not imports:
        continue
    last_import = imports[-1]
    
    # 加 import
    new_content = content[:last_import.end()] + "\nimport PageEnhancer from '@/components/PageEnhancer.vue'" + content[last_import.end():]
    
    # 找第一个 <div class="page-XXX" 位置 (在改后的 content 中)
    m = re.search(r'(\s*)<div class="page-[^"]+"', new_content)
    if not m:
        continue
    
    insert_pos = m.end()  # 在 <div ...> 之后
    
    # 找匹配的 </div> for the page div (最后一个 </div> before </template>)
    # 简化: 找 page-XXX 字符串, 找下一个 </template>
    # 实际: 数 <div> 和 </div> 平衡
    # 简化: 在 </template> 前插入 </PageEnhancer>
    
    last_template = new_content.rfind('</template>')
    if last_template < 0:
        continue
    
    # 找最后一个 BackToTop - 删掉 (因为 PageEnhancer 内可能也加)
    # 简化: 保留 BackToTop (PageEnhancer 没自带)
    
    # 在第一个 <div class="page-..."> 之后插入 <PageEnhancer :title="...">
    enhancer_open = f'\n    <PageEnhancer :title="{title}" icon="📊" gradient="primary">'
    new_content = new_content[:insert_pos] + enhancer_open + new_content[insert_pos:]
    
    # 在 </template> 前插入 </PageEnhancer>
    enhancer_close = '    </PageEnhancer>\n  '
    new_content = new_content[:last_template] + enhancer_close + new_content[last_template:]
    
    vue_file.write_text(new_content, encoding='utf-8')
    count_enhanced += 1
    if count_enhanced <= 5:
        print(f'  ✓ {vue_file.relative_to(VIEWS_DIR.parent.parent)}: {title}')

print(f'\n总计: {count_enhanced} 个新增强, {count_already} 个已存在')
