#!/usr/bin/env python3
"""
V6.7+ 一次性给所有视图加 PageEnhancer 包装
- 智能提取 title (从 page-title/h1/h2)
- 智能图标 (按路由名映射)
- 智能 gradient (按目录分类)
"""
import re
import sys
from pathlib import Path

VIEWS_DIR = Path(sys.argv[1] if len(sys.argv) > 1 else 'frontend/src/views')

# 路由图标映射
ICON_MAP = {
    'admin': '🛡️', 'monitor': '📊', 'agent': '🤖', 'ai': '🧠',
    'analytics': '📈', 'apikey': '🔑', 'chat': '💬', 'collab': '🤝',
    'compliance': '⚖️', 'function': '🔧', 'kg': '🕸️', 'knowledge': '📚',
    'memory': '🧠', 'model': '🧬', 'multimodal': '🎨', 'notification': '🔔',
    'pipeline': '🔄', 'plugins': '🧩', 'prompts': '💭', 'showcase': '🎪',
    'super': '👑', 'tenant': '🏢', 'training': '📈', 'user': '👤'
}

# 路由渐变色映射
GRADIENT_MAP = {
    'admin': ['#667eea', '#764ba2'],     # 紫蓝
    'monitor': ['#30cfd0', '#330867'],   # 青紫
    'agent': ['#14b8a6', '#0ea5e9'],     # 青蓝
    'ai': ['#f093fb', '#f5576c'],       # 粉红
    'analytics': ['#fa709a', '#fee140'], # 桃黄
    'apikey': ['#4facfe', '#00f2fe'],    # 蓝青
    'chat': ['#667eea', '#764ba2'],
    'collab': ['#43e97b', '#38f9d7'],    # 绿青
    'kg': ['#f093fb', '#f5576c'],
    'knowledge': ['#a8edea', '#fed6e3'],  # 浅
    'memory': ['#a18cd1', '#fbc2eb'],
    'model': ['#ff9a9e', '#fecfef'],
    'multimodal': ['#ffecd2', '#fcb69f'],
    'notification': ['#ff9a9e', '#fad0c4'],
    'pipeline': ['#84fab0', '#8fd3f4'],
    'plugins': ['#d299c2', '#fef9d7'],
    'training': ['#14b8a6', '#0ea5e9'],
    'super': ['#f5576c', '#f093fb'],
    'tenant': ['#5ee7df', '#b490ca'],
}

def get_meta(file_path):
    """根据文件路径获取图标和渐变"""
    parts = file_path.parts
    # 找目录名 (views 下的子目录)
    for p in parts:
        if p in ICON_MAP:
            return ICON_MAP.get(p, '📊'), GRADIENT_MAP.get(p, ['#667eea', '#764ba2'])
    return '📊', ['#667eea', '#764ba2']

def extract_title(content):
    """从内容提取 title"""
    # 1. 找 page-title 元素
    m = re.search(r'<h\d[^>]*class="[^"]*page-title[^"]*"[^>]*>([^<]+)', content)
    if m:
        text = m.group(1).strip()
        # 移除 emoji 和 t() 包裹
        text = re.sub(r'[\U0001F300-\U0001F9FF\U0001F600-\U0001F64F]', '', text).strip()
        return text
    # 2. 找 h1
    m = re.search(r'<h1[^>]*>([^<]+)', content)
    if m:
        return re.sub(r'[\U0001F300-\U0001F9FF\U0001F600-\U0001F64F]', '', m.group(1)).strip()
    # 3. 找 h2
    m = re.search(r'<h2[^>]*>([^<]+)', content)
    if m:
        return re.sub(r'[\U0001F300-\U0001F9FF\U0001F600-\U0001F64F]', '', m.group(1)).strip()
    return None

def process(file_path):
    content = file_path.read_text(encoding='utf-8')
    
    if 'PageEnhancer' in content:
        return False, '已有 PageEnhancer'
    
    if 'class="page-' not in content and 'class="page-header"' not in content:
        return False, '无 page class'
    
    title = extract_title(content)
    if not title:
        return False, '无 title'
    
    icon, gradient = get_meta(file_path)
    gradient_str = f"['{gradient[0]}', '{gradient[1]}']"
    
    # 找 <template> 行
    template_m = re.search(r'<template>\s*\n', content)
    if not template_m:
        return False, '无 <template>'
    template_end = template_m.end()
    
    # 找 <div class="page-XXX"> 行
    div_m = re.search(r'(\s*)<div class="(page-[^"]+)"', content[template_end:])
    if not div_m:
        return False, '无 page div'
    div_start = template_end + div_m.start()
    div_end = template_end + div_m.end()
    
    # 找 imports
    imports = list(re.finditer(r'^\s*import .+$', content, re.MULTILINE))
    if not imports:
        return False, '无 import'
    last_import = imports[-1]
    
    # 加 import
    new_content = content[:last_import.end()] + "\nimport PageEnhancer from '@/components/PageEnhancer.vue'" + content[last_import.end():]
    
    # 重新找位置 (因为插入了 import, 偏移变了)
    template_m = re.search(r'<template>\s*\n', new_content)
    template_end = template_m.end()
    div_m = re.search(r'(\s*)<div class="(page-[^"]+)"', new_content[template_end:])
    div_start = template_end + div_m.start()
    div_end = template_end + div_m.end()
    
    # 在 <div class="..."> 之后插入 <PageEnhancer>
    # 提取的 title 要加 t() - 用 i18n key
    # 简化: title 直接是字符串 - 但 Vue 模板要 i18n
    # 方案: 用 t('xxx') 形式, key 是从 title 提取
    title_key = re.sub(r'[^\w]', '', title).lower()[:30]
    
    enhancer_open = f'\n  <PageEnhancer :title="{title}" icon="{icon}" :gradient="{gradient_str}">'
    new_content = new_content[:div_end] + enhancer_open + new_content[div_end:]
    
    # 找最后 </template> 前插入 </PageEnhancer>
    last_template = new_content.rfind('</template>')
    if last_template < 0:
        return False, '无 </template>'
    
    # 在 last_template 前 1 行 (template 闭合是 file 末尾) - 用 perl
    # 找最后 </div> 之前 (在 template 闭合前)
    # 简化: 在 </template> 前插入 </PageEnhancer>
    new_content = new_content[:last_template] + '  </PageEnhancer>\n' + new_content[last_template:]
    
    file_path.write_text(new_content, encoding='utf-8')
    return True, title

count_done = 0
count_skip = 0
for f in VIEWS_DIR.rglob('*.vue'):
    if f.name in ('Error.vue',):
        continue
    ok, info = process(f)
    if ok:
        count_done += 1
        if count_done <= 5:
            print(f'  ✓ {f.relative_to(VIEWS_DIR.parent.parent)}: {info}')
    else:
        count_skip += 1
        if count_skip <= 5:
            print(f'  - {f.relative_to(VIEWS_DIR.parent.parent)}: {info}')

print(f'\n总计: {count_done} 个新增强, {count_skip} 个跳过')
