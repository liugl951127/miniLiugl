#!/usr/bin/env python3
"""
V3.5.68+: 扫描 mapper 接口 @Select/@Update/@Insert/@Delete 注解跟 XML mapper 重复定义

V3.5.68 bug: UserApiKeyMapper 接口 3 个 @Select/@Update 方法跟 XML mapper 同名定义
重复, MyBatis-Plus 启动报警告 "ERROR ... mapper[xxx] is ignored, because it exists,
maybe from xml file".

这个脚本扫所有 backend/minimax-*/src/main/java/**/mapper/*.java 接口 + 同名 XML, 找出
同名方法在两边都定义的, 输出错位.

退出码: 0 = 0 重复, 1 = 有重复
"""
import os
import re
import sys

ROOT = '/workspace/miniLiugl/backend'
ANNOTATIONS = ['@Select', '@Update', '@Insert', '@Delete']


def find_mappers():
    """返回 {module: {basename: (java_path, xml_path_or_None)}}"""
    modules = {}
    for m in os.listdir(ROOT):
        if not m.startswith('minimax-'):
            continue
        mdir = os.path.join(ROOT, m, 'src/main')
        if not os.path.exists(mdir):
            continue
        java_dir = os.path.join(mdir, 'java')
        xml_dir = os.path.join(mdir, 'resources/mapper')
        if not os.path.exists(java_dir):
            continue
        # 找 mapper .java
        for root, dirs, files in os.walk(java_dir):
            for f in files:
                if not f.endswith('Mapper.java'):
                    continue
                basename = f[:-5]  # 去掉 .java
                java_path = os.path.join(root, f)
                xml_path = os.path.join(xml_dir, basename + '.xml') if os.path.exists(xml_dir) else None
                if xml_path and not os.path.exists(xml_path):
                    xml_path = None
                modules.setdefault(m, {})[basename] = (java_path, xml_path)
    return modules


def get_annotation_methods(java_path):
    """从 mapper 接口提取 @Select/@Update/@Insert/@Delete 注解的方法名"""
    content = open(java_path).read()
    methods = set()
    # 找 @XXX 紧跟的方法签名 (1-2 行内)
    for ann in ANNOTATIONS:
        # @Select("...") \n ReturnType methodName(
        pattern = rf'{ann}\s*\(.*?\)\s*\n\s*[\w<>\[\]?,\s.]+?\s+(\w+)\s*\('
        for m in re.finditer(pattern, content, re.DOTALL):
            methods.add(m.group(1))
    return methods


def get_xml_methods(xml_path):
    """从 mapper XML 提取 <select|update|insert|delete id="..."> 方法名"""
    content = open(xml_path).read()
    methods = set()
    for tag in ['select', 'update', 'insert', 'delete']:
        pattern = rf'<{tag}\s+id="([^"]+)"'
        for m in re.finditer(pattern, content):
            methods.add(m.group(1))
    return methods


def main():
    modules = find_mappers()
    duplicates = []
    for mod, mappers in modules.items():
        for basename, (java_path, xml_path) in mappers.items():
            if not xml_path:
                continue
            java_methods = get_annotation_methods(java_path)
            xml_methods = get_xml_methods(xml_path)
            overlap = java_methods & xml_methods
            if overlap:
                rel_java = java_path.replace(ROOT + '/', '')
                rel_xml = xml_path.replace(ROOT + '/', '')
                for m in sorted(overlap):
                    duplicates.append((rel_java, rel_xml, m))

    if duplicates:
        print(f"  发现 {len(duplicates)} 处 mapper 重复定义:")
        for j, x, m in duplicates:
            print(f"    ✗ {m} (java: {j} ↔ xml: {x})")
        print()
        print("  修法: 二选一")
        print("    A. 删接口 @XXX 注解 (XML 已有 resultMap 完整版)")
        print("    B. 删 XML <select|update|...> (接口注解版)")
        print("  推荐 A: 保留 XML resultMap apiKeyMap 全字段映射")
        return 1

    print(f"  扫描 {sum(len(m) for m in modules.values())} 个 mapper 接口 (14 module), 0 重复")
    return 0


if __name__ == '__main__':
    sys.exit(main())
