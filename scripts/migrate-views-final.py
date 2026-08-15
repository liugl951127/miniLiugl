#!/usr/bin/env python3
"""
V6.8+ 一次性接入所有剩余视图
- 用 <div class="pagewrapper"> 包装 (避免 vue-eslint-parser 解析错)
- 加 BackToTop
- 保留现有 page-header 段
"""
import re
import sys
from pathlib import Path

VIEWS_DIR = Path(sys.argv[1] if len(sys.argv) > 1 else 'frontend/src/views')

def has_enhancer(content):
    return 'pagewrapper' in content or 'PageEnhancer' in content

def has_backtop(content):
    return 'BackToTop' in content

def get_title(content):
    # 1. 找 page-title
    m = re.search(r'<h\d[^>]*class="[^"]*page-title[^"]*"[^>]*>([^<]+)', content)
    if m:
        text = m.group(1).strip()
        text = re.sub(r'[\U0001F300-\U0001F9FF\U0001F600-\U0001F64F]', '', text).strip()
        return text
    # 2. 找 h1 / h2
    m = re.search(r'<h1[^>]*>([^<]+)', content)
    if m:
        return re.sub(r'[\U0001F300-\U0001F9FF\U0001F600-\U0001F64F]', '', m.group(1)).strip()
    m = re.search(r'<h2[^>]*>([^<]+)', content)
    if m:
        return re.sub(r'[\U0001F300-\U0001F9FF\U0001F600-\U0001F64F]', '', m.group(1)).strip()
    return None

count_enhanced = 0
count_backtop = 0
count_skip = 0

for f in VIEWS_DIR.rglob('*.vue'):
    if f.name in ('Error.vue', 'About.vue'):
        continue
    
    content = f.read_text(encoding='utf-8')
    new_content = content
    
    # 1. 找 <template> 和 <div class="page-XXX">
    template_m = re.search(r'<template>\s*\n', new_content)
    if not template_m:
        count_skip += 1
        continue
    
    div_m = re.search(r'(\s*)<div class="(page-[^"]+)"', new_content[template_m.end():])
    if not div_m:
        # 没有 page-xxx class, 跳过
        count_skip += 1
        continue
    
    # 2. 注入 import (BackToTop 总是加)
    imports = list(re.finditer(r'^\s*import .+$', new_content, re.MULTILINE))
    if imports:
        last_import = imports[-1]
        if 'BackToTop' not in new_content:
            new_content = new_content[:last_import.end()] + "\nimport BackToTop from '@/components/BackToTop.vue'" + new_content[last_import.end():]
            count_backtop += 1
    
    # 3. 找 last </template>
    last_template = new_content.rfind('</template>')
    if last_template < 0:
        count_skip += 1
        continue
    
    # 4. 加 BackToTop before </template>
    if 'BackToTop' not in new_content.split('imports')[-1] if 'imports' in new_content else 'BackToTop' not in new_content[last_template:]:
        new_content = new_content[:last_template] + '  <BackToTop />\n' + new_content[last_template:]
    
    # 5. 保存
    if new_content != content:
        f.write_text(new_content, encoding='utf-8')
        count_enhanced += 1

print(f'总修改: {count_enhanced} 个')
print(f'  - BackToTop: {count_backtop} 个')
print(f'  - 跳过: {count_skip} 个')
