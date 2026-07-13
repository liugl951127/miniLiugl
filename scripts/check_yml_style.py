#!/usr/bin/env python3
"""
扫描所有 application*.yml 配置 key 的命名风格
- kebab-case (短横线): mybatis-plus
- camelCase (驼峰): mapUnderscoreToCamelCase
- snake_case (下划线): id_type
- UPPER_CASE: 全部大写
"""
import os
import glob
import re
from collections import defaultdict, Counter

def get_keys_from_yaml(yaml_text):
    """递归提取所有 leaf key (扁平路径, 如 spring.datasource.url)"""
    keys = []
    lines = yaml_text.split('\n')
    stack = []  # [(indent, key), ...]
    for line in lines:
        if not line.strip() or line.strip().startswith('#'):
            continue
        # 找缩进
        indent = len(line) - len(line.lstrip())
        content = line.strip()
        # 跳过列表项 (- xxx)
        if content.startswith('- '):
            continue
        # 找 key: value
        m = re.match(r'^([\w\-]+)\s*:', content)
        if not m:
            continue
        key = m.group(1)
        # 弹栈直到 indent < 当前
        while stack and stack[-1][0] >= indent:
            stack.pop()
        full_key = '.'.join([k for _, k in stack] + [key])
        keys.append(full_key)
        # 判断是不是 leaf (有 value)
        if ':' in content and not content.endswith(':'):
            # 有 value, 不入栈
            pass
        else:
            stack.append((indent, key))
    return keys

def classify(key):
    """分类 key 的命名风格"""
    # 取最后一段
    parts = key.split('.')
    last = parts[-1]
    # 跳过特殊 (空, _ 占位)
    if not last or last.startswith('$') or last.startswith('{{'):
        return None
    if last.isupper():
        return 'UPPER'
    if '-' in last and not any(c.isupper() for c in last):
        return 'kebab'
    if re.match(r'^[a-z]+(_[a-z]+)+$', last):
        return 'snake'
    if any(c.isupper() for c in last) and not '-' in last and not '_' in last:
        return 'camel'
    if '-' in last and any(c.isupper() for c in last):
        return 'mixed'
    return 'other'

def main():
    # 只看 src/main/resources (源代码), 不看 target
    files = sorted(glob.glob('/workspace/miniLiugl/backend/**/src/main/resources/application*.yml', recursive=True))
    print(f"📄 配置文件: {len(files)} 个\n")

    style_counter = Counter()
    last_parts = Counter()  # 最后一个 segment 的统计
    file_styles = defaultdict(Counter)

    for f in files:
        with open(f) as fp:
            c = fp.read()
        keys = get_keys_from_yaml(c)
        for k in keys:
            style = classify(k)
            if style is None: continue
            style_counter[style] += 1
            last_part = k.split('.')[-1]
            last_parts[last_part] += 1
            file_styles[os.path.basename(f)][style] += 1

    # 整体统计
    total = sum(style_counter.values())
    print("=" * 80)
    print("📊 整体命名风格分布")
    print("=" * 80)
    for style, cnt in style_counter.most_common():
        pct = cnt / total * 100
        print(f"  {style:10} {cnt:5}  ({pct:.1f}%)")
    print(f"  {'─' * 40}")
    print(f"  {'TOTAL':10} {total:5}")
    print()

    # 详细: 列出每个风格的典型 key
    print("=" * 80)
    print("📋 各风格典型 Key 样例")
    print("=" * 80)
    style_examples = defaultdict(list)
    for last, cnt in last_parts.most_common(50):
        style = classify('x.' + last)
        if style:
            style_examples[style].append(last)
    for style, examples in style_examples.items():
        print(f"\n  {style} ({len(examples)} 种):")
        for ex in examples[:15]:
            print(f"    {ex}")
        if len(examples) > 15:
            print(f"    ... +{len(examples) - 15} 更多")

    # 关键配置项
    print()
    print("=" * 80)
    print("🔍 关键配置项命名")
    print("=" * 80)
    important = ['map-underscore-to-camel-case', 'mapUnderscoreToCamelCase',
                 'table-underline', 'tableUnderline',
                 'id-type', 'idType', 'logic-delete-field', 'logicDeleteField',
                 'logic-delete-value', 'logicDeleteValue',
                 'logic-not-delete-value', 'logicNotDeleteValue',
                 'column-underline', 'columnUnderline',
                 'mybatis-plus', 'mybatisPlus',
                 'spring.datasource', 'springDatasource',
                 'spring.profiles.active', 'springProfilesActive',
                 'spring.main.allow-bean-definition-overriding',
                 'springMainAllowBeanDefinitionOverriding']
    for k in important:
        style = classify('x.' + k)
        print(f"  {k:50} → {style}")

if __name__ == '__main__':
    main()
