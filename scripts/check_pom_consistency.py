#!/usr/bin/env python3
"""
V3.6.4+ pom.xml 一致性静态检查 (沙箱友好)
"""
import os
import xml.etree.ElementTree as ET
from pathlib import Path

BACKEND = Path("backend")
EXPECTED_MODULES = [
    "minimax-gateway", "minimax-auth", "minimax-chat", "minimax-model",
    "minimax-rag", "minimax-multimodal", "minimax-agent", "minimax-monitor",
    "minimax-admin", "minimax-analytics", "minimax-pipeline",
    "minimax-ai", "minimax-ws", "minimax-common",
]

errors = 0
warnings = 0
print("═══════════════════════════════════════════════════════════")
print("  V3.6.4+ pom.xml 一致性静态检查 (沙箱友好)")
print("═══════════════════════════════════════════════════════════")

for mod in EXPECTED_MODULES:
    pom = BACKEND / mod / "pom.xml"
    if not pom.exists():
        print(f"❌ {mod}/pom.xml 缺失")
        errors += 1
        continue
    try:
        tree = ET.parse(pom)
        root = tree.getroot()
        ns = "{http://maven.apache.org/POM/4.0.0}"
        # Java 17
        java_ver = root.find(f".//{ns}java.version")
        if java_ver is not None and java_ver.text != "17":
            print(f"⚠️ {mod}: Java {java_ver.text} (期望 17)")
            warnings += 1
        # groupId
        gid = root.find(f".//{ns}groupId")
        if gid is not None and not gid.text.startswith("com.minimax"):
            print(f"⚠️ {mod}: groupId={gid.text} (期望 com.minimax.*)")
            warnings += 1
        # artifactId
        aid = root.find(f".//{ns}artifactId")
        if aid is not None and aid.text != mod:
            print(f"⚠️ {mod}: artifactId={aid.text}")
            warnings += 1
    except ET.ParseError as e:
        print(f"❌ {mod}/pom.xml 解析错误: {e}")
        errors += 1

print("═══════════════════════════════════════════════════════════")
print(f"  错误: {errors}, 警告: {warnings}")
print(f"  状态: {'✅ ALL PASS' if errors == 0 else f'❌ {errors} errors'}")
print("═══════════════════════════════════════════════════════════")
exit(1 if errors else 0)
