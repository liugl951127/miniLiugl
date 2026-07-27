#!/usr/bin/env python3
"""
扫描 AdminDataInitializer + AI 关键词, 自动生成 seed-data.sql
- 5 测试账号 (BCrypt hash 从 AdminDataInitializer 提取)
- AI 关键词 (从 v3.5.15 4 模型加权 + 149 同义词组)

用法:
  python3 scripts/gen_seed_data.py
"""
import re
import os
import sys
from pathlib import Path

ROOT = Path(__file__).parent.parent
INITIALIZER = ROOT / "backend/minimax-auth/src/main/java/com/minimax/auth/config/AdminDataInitializer.java"
KEYWORDS_FILE = ROOT / "scripts/.ai_keywords.txt"
OUTPUT = ROOT / "sql/v3.5.49-seed.sql"


def extract_bcrypt_hashes():
    """从 AdminDataInitializer 读 BCrypt 哈希, 跟 username 一一对应.

    实际: V3.5.8+ BCrypt 哈希在 AdminDataInitializer 生成 (10 rounds),
    seed-data.sql 只放 username + 明文密码 + 角色, 哈希运行时跑 AdminDataInitializer 兜底.
    """
    if not INITIALIZER.exists():
        return None
    content = INITIALIZER.read_text()
    # 简单提取: superAdminUsername, superAdminPassword 等
    pairs = []
    # 找 @Value 默认值
    for m in re.finditer(r'@Value\("\$\{[^:]+:([^}]+)\}"\)', content):
        pairs.append(m.group(1))
    return pairs


def read_keywords():
    """从 .ai_keywords.txt 读 AI 关键词 (intent, keyword, weight, is_regex)"""
    if not KEYWORDS_FILE.exists():
        return []
    keywords = []
    for line in KEYWORDS_FILE.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        parts = line.split('|')
        if len(parts) >= 3:
            keywords.append({
                'intent': parts[0].strip(),
                'keyword': parts[1].strip(),
                'weight': int(parts[2].strip()),
                'is_regex': int(parts[3].strip()) if len(parts) > 3 else 0,
            })
    return keywords


def main():
    print("═══════════════════════════════════════════════════════════")
    print("  V3.5.37 seed-data.sql 生成器")
    print("═══════════════════════════════════════════════════════════")
    print()

    # 5 测试账号 (从 AdminDataInitializer 文档注释读)
    # AdminDataInitializer 里 @Value 模式提取 password 默认值
    hashes = extract_bcrypt_hashes() or []
    # 兜底: 5 账号 (V3.5.12 沙箱兜底逻辑)
    accounts = [
        ('adminLiugl', 'Liugl@2026', 'SUPER_ADMIN', 'liugl951127@gmail.com',
         '平台所有者 (唯一超级管理员)'),
        ('admin', 'admin@123', 'ADMIN', 'admin@minimax.io',
         '管理员 (旧版, 兼容)'),
        ('admin_user', 'admin123', 'ADMIN', 'admin_user@minimax.io',
         '沙箱测试账号 (管理员)'),
        ('test_user', 'user123', 'USER', 'test_user@minimax.io',
         '沙箱测试账号 (普通用户)'),
        ('demo_user', 'demo1234', 'USER', 'demo_user@minimax.io',
         'Demo 租户 (租户 ID=2)'),
    ]

    # 读关键词
    keywords = read_keywords()
    if not keywords:
        # 兜底: 22 关键词 (V3.5.15 4 模型)
        keywords = [
            ('CHART',         'chart',         3, 0),
            ('CHART',         '图表',          3, 0),
            ('CHART',         '可视化',        3, 0),
            ('CHART',         '柱状图',        5, 0),
            ('CHART',         '折线图',        5, 0),
            ('CHART',         '饼图',          5, 0),
            ('CODE_GENERATE', '代码生成',      5, 0),
            ('CODE_GENERATE', 'code',          3, 0),
            ('CODE_GENERATE', '生成代码',      5, 0),
            ('CODE_GENERATE', '生成项目',      8, 0),
            ('DATA_ANALYSIS', '数据分析',      5, 0),
            ('DATA_ANALYSIS', 'analysis',      3, 0),
            ('DATA_ANALYSIS', '分析',          3, 0),
            ('DATA_ANALYSIS', '统计',          3, 0),
            ('NL2SQL',        '查询',          3, 0),
            ('NL2SQL',        'sql',           3, 0),
            ('NL2SQL',        'sql 生成',      5, 0),
            ('NL2SQL',        '数据查询',      5, 0),
            ('REPORT',        '报告',          5, 0),
            ('REPORT',        'report',        3, 0),
            ('REPORT',        '周报',          5, 0),
            ('REPORT',        '月报',          5, 0),
        ]

    # 写 SQL
    lines = [
        "-- =============================================================",
        "-- MiniMax Platform V3.5.37 Seed Data",
        f"-- 重新生成时间: {os.popen('date').read().strip()}",
        "-- 目标: MySQL 8.0+ / MariaDB 10.4+",
        "-- 用法: mysql -uroot -proot123456 < sql/v3.5.49-seed.sql",
        "-- ",
        "-- 内容:",
        f"--   - {len(accounts)} 测试账号 (adminLiugl / admin / admin_user / test_user / demo_user)",
        f"--   - {len(keywords)} AI 关键词 (CHART/CODE_GENERATE/DATA_ANALYSIS/NL2SQL/REPORT)",
        "-- ",
        "-- BCrypt 密码哈希由 AdminDataInitializer 启动时生成 (10 rounds)",
        "-- 沙箱模式 (h2local) 自动跑 5 账号兜底, 生产模式 (mysql) 用本文件",
        "-- =============================================================",
        "",
        "USE minimax_platform;",
        "",
        "-- ============== 1. 5 测试账号 (明文密码) ==============",
        "-- 注: BCrypt 哈希在 AdminDataInitializer.java 维护, 启动时生成",
        "-- 本表只放 username/明文/角色, 沙箱兜底自动转 BCrypt",
        "",
    ]
    for u, p, role, email, remark in accounts:
        lines.append(
            f"INSERT INTO sys_user (username, password, nickname, email, status, tenant_id, remark, created_at, updated_at, deleted) "
            f"VALUES ('{u}', '{p}', '{u}', '{email}', '1', 0, '{remark}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);"
        )
    lines.append("")
    lines.append("-- 5 账号 + 3 角色 (SUPER_ADMIN/ADMIN/USER) 关联表")
    lines.append("-- AdminDataInitializer 启动时自动建关联, 这里不重复")
    lines.append("")

    lines.append("-- ============== 2. AI 关键词 (22 个, V3.5.15 4 模型加权) ==============")
    lines.append("-- CHART/CODE_GENERATE/DATA_ANALYSIS/NL2SQL/REPORT 5 大意图")
    lines.append("-- 权重 1-10, 越高匹配越优先 (TF/Ngram/Synonym/Context 加权)")
    lines.append("")
    lines.append("INSERT INTO ai_intent_keyword (intent, keyword, weight, is_regex, enabled) VALUES")
    for i, (intent, kw, weight, is_regex) in enumerate(keywords):
        sep = ',' if i < len(keywords) - 1 else ';'
        lines.append(f"  ('{intent}', '{kw}', {weight}, {is_regex}, 1){sep}")
    lines.append("")
    lines.append("-- 3 索引 (高频查询加速)")
    lines.append("CREATE INDEX IF NOT EXISTS idx_ai_intent_keyword_intent ON ai_intent_keyword (intent);")
    lines.append("CREATE INDEX IF NOT EXISTS idx_ai_intent_keyword_enabled ON ai_intent_keyword (enabled);")
    lines.append("")
    lines.append("-- =============================================================")
    lines.append(f"-- V3.5.37 seed-data.sql 完 ({len(accounts)} 账号 + {len(keywords)} 关键词)")
    lines.append("-- =============================================================")
    lines.append("")

    OUTPUT.write_text('\n'.join(lines))
    print(f"✅ 已生成: {OUTPUT}")
    print(f"   账号数: {len(accounts)}")
    print(f"   关键词数: {len(keywords)}")
    print(f"   文件大小: {OUTPUT.stat().st_size} bytes")


if __name__ == '__main__':
    main()
