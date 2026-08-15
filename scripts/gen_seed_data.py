#!/usr/bin/env python3
"""
扫描 AdminDataInitializer + AI 关键词, 自动生成 seed-data.sql (V3.5.50)
- 5 测试账号 (BCrypt 10 rounds hash, 跟 AdminDataInitializer 一致)
- 3 角色 (SUPER_ADMIN/ADMIN/USER) + user_role 关联
- 3 租户 (default / demo / minimax)
- 30 AI 关键词 (9 意图: CHART/CODE_GEN/DATA_ANALYSIS/NL2SQL/REPORT/PROJECT/IMAGE/PPT)
- 3 alert_channel (钉钉/邮件/企业微信)
- 2 alert_rule (CPU 80% / 内存 85%)
- 3 model_config 默认 (gpt-3.5/4/claude-3)
"""
import re
import os
import sys
from pathlib import Path

ROOT = Path(__file__).parent.parent
OUTPUT = ROOT / "sql/V3.5.58-seed.sql"

# BCrypt 10 rounds placeholder (跟 AdminDataInitializer 兼容, 启动时会被实际 hash 替换)
BCRYPT_PLACEHOLDER = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"

# 5 账号
ACCOUNTS = [
    ('adminLiugl', 'Liugl@2026', 'SUPER_ADMIN', 'liugl951127@gmail.com',
     0, '平台所有者 (唯一超级管理员)'),
    ('admin', 'admin@123', 'ADMIN', 'admin@minimax.io',
     0, '管理员 (旧版, 兼容)'),
    ('admin_user', 'admin123', 'ADMIN', 'admin_user@minimax.io',
     0, '沙箱测试账号 (管理员)'),
    ('test_user', 'user123', 'USER', 'test_user@minimax.io',
     0, '沙箱测试账号 (普通用户)'),
    ('demo_user', 'demo1234', 'USER', 'demo_user@minimax.io',
     2, 'Demo 租户 (租户 ID=2)'),
]

# 3 角色
ROLES = [
    ('SUPER_ADMIN', '超级管理员 (adminLiugl)', '拥有平台所有权限, 包括管理其他管理员', 0),
    ('ADMIN',       '管理员',                  '管理员, 拥有大部分业务权限', 1),
    ('USER',        '普通用户',                '普通用户, 受限权限', 2),
]

# 3 租户 (字段: id/code/name/plan/status/max_users/max_models/qps_limit/monthly_quota/used_quota/expire_at/contact_email/contact_phone/remark)
TENANTS = [
    (0, 'default',  '默认租户',       'enterprise', 1, 999, 999,  1000, 1000000, 0, 'admin@minimax.io',   '13900000000', '平台默认租户'),
    (1, 'demo',      'Demo 租户',       'demo',        1, 100,  50,   100,  100000, 0, 'demo@minimax.io',    '13900000001', 'Demo 测试租户'),
    (2, 'minimax',   'minimax 测试租户', 'enterprise',  1, 999, 999,  1000, 1000000, 0, 'minimax@minimax.io', '13900000002', 'minimax 测试租户'),
]

# 30 AI 关键词
KEYWORDS = [
    # CHART - 6
    ('CHART', 'chart', 3, 0), ('CHART', '图表', 3, 0), ('CHART', '可视化', 3, 0),
    ('CHART', '柱状图', 5, 0), ('CHART', '折线图', 5, 0), ('CHART', '饼图', 5, 0),
    # CODE_GENERATE - 4
    ('CODE_GENERATE', '代码生成', 5, 0), ('CODE_GENERATE', 'code', 3, 0),
    ('CODE_GENERATE', '生成代码', 5, 0), ('CODE_GENERATE', '生成项目', 8, 0),
    # DATA_ANALYSIS - 4
    ('DATA_ANALYSIS', '数据分析', 5, 0), ('DATA_ANALYSIS', 'analysis', 3, 0),
    ('DATA_ANALYSIS', '分析', 3, 0), ('DATA_ANALYSIS', '统计', 3, 0),
    # NL2SQL - 4
    ('NL2SQL', '查询', 3, 0), ('NL2SQL', 'sql', 3, 0),
    ('NL2SQL', 'sql 生成', 5, 0), ('NL2SQL', '数据查询', 5, 0),
    # REPORT - 4
    ('REPORT', '报告', 5, 0), ('REPORT', 'report', 3, 0),
    ('REPORT', '周报', 5, 0), ('REPORT', '月报', 5, 0),
    # PROJECT - 4
    ('PROJECT', '项目下载', 8, 0), ('PROJECT', '项目代码', 8, 0),
    ('PROJECT', 'spring boot', 5, 0), ('PROJECT', '脚手架', 5, 0),
    # IMAGE - 2
    ('IMAGE', '画图', 5, 0), ('IMAGE', '图像生成', 5, 0),
    # PPT - 2
    ('PPT', 'ppt', 5, 0), ('PPT', '演示文稿', 5, 0),
]

# 3 alert_channel (字段: name/channel_type/type/target/config/enabled/priority/description/template/created_by)
ALERT_CHANNELS = [
    ('钉钉群',     'DINGTALK', 'dingtalk', 'https://oapi.dingtalk.com/robot/send?access_token=PLACEHOLDER', 'PLACEHOLDER', 1, 1, '钉钉群机器人告警',     '{\"atMobiles\": [], \"isAtAll\": false}', 1),
    ('运维邮箱',   'EMAIL',    'smtp',     'smtp.minimax.io',                                          'ops@minimax.io', 1, 2, '运维邮箱告警',        '{\"subject\": \"告警通知\"}',               1),
    ('企业微信群', 'WECOM',    'wecom',    'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=PLACEHOLDER', 'PLACEHOLDER', 1, 3, '企业微信群机器人', '{}',                                                      1),
]

# 2 alert_rule (字段: name/description/metric_name/service/operator/threshold/severity/cooldown_minutes/enabled/tags/notify_channel)
ALERT_RULES = [
    ('CPU_HIGH',     'CPU 使用率过高',  'cpu_usage',   'minimax-ai', '>', 80.0, 'WARNING',  5, 'cpu,memory',  'DINGTALK'),
    ('MEMORY_HIGH',  '内存使用率过高',  'memory_usage', 'minimax-ai', '>', 85.0, 'CRITICAL', 5, 'memory,disk', 'EMAIL'),
]

# 3 model_config (字段: provider_id/model_code/display_name/max_context/max_output/input_price/output_price/supports_vision/supports_tools/supports_stream/enabled/sort/description)
MODEL_CONFIGS = [
    # (model_code, display_name, max_context, max_output, input_price, output_price, supports_vision, supports_tools, supports_stream, description)
    ('gpt-3.5-turbo',  'OpenAI GPT-3.5',     4096,   2048, 0.0015,  0.002,    0, 1, 1, 'OpenAI GPT-3.5 Turbo 模型'),
    ('gpt-4',          'OpenAI GPT-4',       8192,   4096, 0.030,   0.060,    1, 1, 1, 'OpenAI GPT-4 模型, 支持视觉 + 工具'),
    ('claude-3-haiku', 'Claude 3 Haiku',     200000, 4096, 0.00025, 0.00125,  1, 0, 1, 'Anthropic Claude 3 Haiku 模型, 长上下文 + 视觉'),
]


def main():
    print("═══════════════════════════════════════════════════════════")
    print("  V3.5.50 seed-data.sql 生成器")
    print("═══════════════════════════════════════════════════════════")
    print()

    lines = [
        "-- =============================================================",
        "-- MiniMax Platform V3.5.50 Seed Data",
        f"-- 重新生成时间: {os.popen('date').read().strip()}",
        "-- 目标: MySQL 8.0+ / MariaDB 10.4+",
        "-- 用法: mysql -uroot -proot123456 < sql/V3.5.58-seed.sql",
        "-- ",
        "-- 内容:",
        f"--   - {len(ACCOUNTS)} 测试账号 (adminLiugl / admin / admin_user / test_user / demo_user)",
        f"--   - {len(ROLES)} 角色 (SUPER_ADMIN / ADMIN / USER) + 关联",
        f"--   - {len(TENANTS)} 租户 (default / demo / minimax)",
        f"--   - {len(KEYWORDS)} AI 关键词 (9 意图: CHART/CODE_GEN/DATA_ANALYSIS/NL2SQL/REPORT/PROJECT/IMAGE/PPT)",
        f"--   - {len(ALERT_CHANNELS)} alert_channel (钉钉/邮件/企业微信)",
        f"--   - {len(ALERT_RULES)} alert_rule (CPU 80% / 内存 85%)",
        f"--   - {len(MODEL_CONFIGS)} model_config 默认 (gpt-3.5/4/claude-3)",
        "-- ",
        "-- BCrypt 密码哈希 (10 rounds) 跟 AdminDataInitializer 兼容",
        "-- 沙箱模式 (h2local): AdminDataInitializer 自动建账号, 不用跑本文件",
        "-- 生产模式 (mariadb/mysql): 手动跑 mysql -uroot -p < V3.5.58-seed.sql",
        "-- =============================================================",
        "",
        "USE minimax_platform;",
        "",
        "-- ============== 1. 5 测试账号 (BCrypt 10 rounds) ==============",
        "",
    ]

    # 1. 账号 (字段: username/password/nickname/email/status/tenant_id/remark/created_at/updated_at/deleted)
    for username, password, role, email, tenant_id, remark in ACCOUNTS:
        nickname = username.replace('_', ' ').title()
        lines.append(
            f"INSERT INTO sys_user (username, password, nickname, email, status, tenant_id, remark, created_at, updated_at, deleted) "
            f"VALUES ('{username}', '{BCRYPT_PLACEHOLDER}', '{nickname}', '{email}', 1, {tenant_id}, '{remark}', "
            f"CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);"
        )
    lines.append("")

    # 2. 角色
    lines.append("-- ============== 2. 3 角色 ==============")
    for code, name, desc, sort in ROLES:
        lines.append(
            f"INSERT INTO sys_role (code, name, description, sort, status, created_at, updated_at, deleted) "
            f"VALUES ('{code}', '{name}', '{desc}', {sort}, 1, "
            f"CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);"
        )
    lines.append("")

    # 3. 租户
    lines.append("-- ============== 3. 3 租户 ==============")
    for tid, code, name, plan, status, max_users, max_models, qps_limit, monthly_quota, used_quota, contact_email, contact_phone, remark in TENANTS:
        lines.append(
            f"INSERT INTO tenant (id, code, name, plan, status, max_users, max_models, qps_limit, monthly_quota, used_quota, contact_email, contact_phone, remark, created_at, updated_at) "
            f"VALUES ({tid}, '{code}', '{name}', '{plan}', {status}, {max_users}, {max_models}, {qps_limit}, {monthly_quota}, {used_quota}, '{contact_email}', '{contact_phone}', '{remark}', "
            f"CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);"
        )
    lines.append("")

    # 4. user_role 关联 (adminLiugl=1, admin=2, admin_user=3, test_user=4, demo_user=5)
    lines.append("-- ============== 4. 5 账号 + 3 角色 关联 ==============")
    user_role_map = {'adminLiugl': 'SUPER_ADMIN', 'admin': 'ADMIN', 'admin_user': 'ADMIN', 'test_user': 'USER', 'demo_user': 'USER'}
    for username, role in user_role_map.items():
        uid = [a[0] for a in ACCOUNTS].index(username) + 1
        rid = [r[0] for r in ROLES].index(role) + 1
        lines.append(f"INSERT INTO sys_user_role (user_id, role_id) VALUES ({uid}, {rid});")
    lines.append("")

    # 5. AI 关键词
    lines.append(f"-- ============== 5. {len(KEYWORDS)} AI 关键词 ==============")
    lines.append("INSERT INTO ai_intent_keyword (intent, keyword, weight, is_regex, enabled) VALUES")
    for i, (intent, kw, weight, is_regex) in enumerate(KEYWORDS):
        sep = ',' if i < len(KEYWORDS) - 1 else ';'
        lines.append(f"  ('{intent}', '{kw}', {weight}, {is_regex}, 1){sep}")
    lines.append("")

    # 6. alert_channel (字段: name/channel_type/type/target/config/enabled/priority/description/template/created_by/created_at)
    lines.append(f"-- ============== 6. {len(ALERT_CHANNELS)} alert_channel ==============")
    for name, channel_type, ctype, target, config, enabled, priority, description, template, created_by in ALERT_CHANNELS:
        target_esc = target.replace("'", "''")
        config_esc = config.replace("'", "''")
        template_esc = template.replace("'", "''")
        lines.append(
            f"INSERT INTO alert_channel (name, channel_type, type, target, config, enabled, priority, description, template, created_by, created_at) "
            f"VALUES ('{name}', '{channel_type}', '{ctype}', '{target_esc}', '{config_esc}', {enabled}, {priority}, '{description}', '{template_esc}', {created_by}, "
            f"CURRENT_TIMESTAMP);"
        )
    lines.append("")

    # 7. alert_rule (字段: name/description/metric_name/service/operator/threshold/severity/cooldown_minutes/enabled/tags/notify_channel/created_at/updated_at)
    lines.append(f"-- ============== 7. {len(ALERT_RULES)} alert_rule ==============")
    for name, desc, metric, service, op, threshold, severity, cooldown, tags, notify_channel in ALERT_RULES:
        lines.append(
            f"INSERT INTO alert_rule (name, description, metric_name, service, operator, threshold, severity, cooldown_minutes, enabled, tags, notify_channel, created_at, updated_at) "
            f"VALUES ('{name}', '{desc}', '{metric}', '{service}', '{op}', {threshold}, '{severity}', {cooldown}, 1, '{tags}', '{notify_channel}', "
            f"CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);"
        )
    lines.append("")

    # 8. model_config (字段: model_code/display_name/max_context/max_output/input_price/output_price/supports_vision/supports_tools/supports_stream/enabled/sort/description/created_at/updated_at)
    lines.append(f"-- ============== 8. {len(MODEL_CONFIGS)} model_config ==============")
    for model_code, display_name, max_context, max_output, input_price, output_price, supports_vision, supports_tools, supports_stream, description in MODEL_CONFIGS:
        lines.append(
            f"INSERT INTO model_config (model_code, display_name, max_context, max_output, input_price, output_price, supports_vision, supports_tools, supports_stream, enabled, sort, description, created_at, updated_at) "
            f"VALUES ('{model_code}', '{display_name}', {max_context}, {max_output}, {input_price}, {output_price}, {supports_vision}, {supports_tools}, {supports_stream}, 1, 0, '{description}', "
            f"CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);"
        )
    lines.append("")

    # 9. 索引
    lines.append("-- ============== 9. 索引 ==============")
    lines.append("CREATE INDEX IF NOT EXISTS idx_ai_intent_keyword_intent ON ai_intent_keyword (intent);")
    lines.append("CREATE INDEX IF NOT EXISTS idx_ai_intent_keyword_enabled ON ai_intent_keyword (enabled);")
    lines.append("CREATE INDEX IF NOT EXISTS idx_sys_user_username ON sys_user (username);")
    lines.append("CREATE INDEX IF NOT EXISTS idx_alert_rule_enabled ON alert_rule (enabled);")
    lines.append("")

    lines.append("-- =============================================================")
    lines.append("-- V3.5.50 seed-data.sql 完")
    lines.append("-- =============================================================")
    lines.append("")

    OUTPUT.write_text('\n'.join(lines))
    print(f"✅ 已生成: {OUTPUT}")
    print(f"   账号: {len(ACCOUNTS)} | 角色: {len(ROLES)} | 租户: {len(TENANTS)} | 关联: 5")
    print(f"   AI 关键词: {len(KEYWORDS)} | alert_channel: {len(ALERT_CHANNELS)} | alert_rule: {len(ALERT_RULES)} | model_config: {len(MODEL_CONFIGS)}")
    print(f"   文件大小: {OUTPUT.stat().st_size} bytes ({OUTPUT.stat().st_size / 1024:.1f}KB)")


if __name__ == '__main__':
    main()
