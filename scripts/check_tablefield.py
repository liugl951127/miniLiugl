#!/usr/bin/env python3
"""
检查所有 Entity 的 @TableField 显式映射, 确保列名符合驼峰转 snake 规则
- @TableField("xxx_yyy") 应该是 snake_case
- @TableField("xxxyyy") 或 @TableField("xxxYyy") 不规范
"""
import re
import os
import glob
from collections import defaultdict

def to_snake(name):
    s = re.sub(r'([A-Z]+)([A-Z][a-z])', r'\1_\2', name)
    s = re.sub(r'([a-z\d])([A-Z])', r'\1_\2', s)
    return s.lower()

def classify(name):
    """分类命名风格"""
    if not name: return 'empty'
    if '_' in name and not any(c.isupper() for c in name):
        return 'snake'
    if any(c.isupper() for c in name):
        if '_' in name:
            return 'mixed (snake+UPPER)'
        return 'camel'
    return 'lower'

def main():
    # 扫描所有 entity
    issues = defaultdict(list)  # kind -> [(file, field, tableField_value, expected)]
    tablefield_examples = defaultdict(list)

    entity_count = 0
    tablefield_count = 0
    for root, dirs, files in os.walk('/workspace/miniLiugl/backend'):
        if 'src/test' in root: continue
        for f in files:
            if f.endswith('.java') and 'entity' in root.lower():
                path = os.path.join(root, f)
                entity_count += 1
                with open(path) as fp:
                    c = fp.read()
                # 找 @TableName
                m = re.search(r'@TableName\s*\(\s*(?:value\s*=\s*)?"(\w+)"\s*\)', c)
                table = m.group(1) if m else 'unknown'
                # 找所有 @TableField 显式映射
                # 模式: 注解 + private/protected 类型 fieldName
                pattern = re.compile(
                    r'((?:@\w+(?:\([^)]*\))?\s*\n?\s*)*)'  # 注解块
                    r'(?:private|protected|public)\s+'
                    r'(?:static\s+)?'
                    r'([\w<>,\s\[\]]+?)\s+'
                    r'(\w+)\s*[;=]',
                    re.MULTILINE
                )
                for m in pattern.finditer(c):
                    annots = m.group(1)
                    field_name = m.group(3)
                    if field_name in ('serialVersionUID', 'log', 'logger'): continue
                    if field_name.isupper(): continue
                    # @TableField
                    m_tf = re.search(r'@TableField\s*\(\s*(?:value\s*=\s*)?"(\w+)"\s*\)', annots)
                    if not m_tf:
                        continue
                    tablefield_count += 1
                    tf_value = m_tf.group(1)
                    style = classify(tf_value)
                    expected = to_snake(field_name)
                    if tf_value != field_name:
                        # 显式映射: 跟 field name 不一样
                        if tf_value != expected:
                            # 也不等于 snake 转换, 不规范
                            issues['wrong_mapping'].append((path, table, field_name, tf_value, expected))
                    tablefield_examples[style].append((table, field_name, tf_value, expected))

    print("=" * 80)
    print(f"📦 扫描 {entity_count} 个 Entity")
    print(f"📋 @TableField 显式映射: {tablefield_count} 个")
    print("=" * 80)

    print("\n命名风格分布:")
    for style, items in sorted(tablefield_examples.items(), key=lambda x: -len(x[1])):
        print(f"  {style:25} {len(items):3} 个")

    if issues['wrong_mapping']:
        print(f"\n⚠️  @TableField 不规范 ({len(issues['wrong_mapping'])} 个):")
        for path, table, field_name, tf_value, expected in issues['wrong_mapping'][:30]:
            ent_module = path.split('minimax-')[-1].split('/')[0]
            print(f"  • {table}.{field_name}")
            print(f"      实际: @TableField(\"{tf_value}\")")
            print(f"      应为: @TableField(\"{expected}\")")
            print(f"      [{ent_module}]")
        if len(issues['wrong_mapping']) > 30:
            print(f"  ... 还有 {len(issues['wrong_mapping']) - 30} 个")
    else:
        print("\n🎉 全部 @TableField 映射都符合 snake_case 规则")

if __name__ == '__main__':
    main()
