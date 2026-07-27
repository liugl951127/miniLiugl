-- =============================================================
-- MiniMax Platform V3.5.50 Seed Data
-- 重新生成时间: Mon Jul 27 23:22:55 UTC 2026
-- 目标: MySQL 8.0+ / MariaDB 10.4+
-- 用法: mysql -uroot -proot123456 < sql/V3.5.58-seed.sql
-- 
-- 内容:
--   - 5 测试账号 (adminLiugl / admin / admin_user / test_user / demo_user)
--   - 3 角色 (SUPER_ADMIN / ADMIN / USER) + 关联
--   - 3 租户 (default / demo / minimax)
--   - 30 AI 关键词 (9 意图: CHART/CODE_GEN/DATA_ANALYSIS/NL2SQL/REPORT/PROJECT/IMAGE/PPT)
--   - 3 alert_channel (钉钉/邮件/企业微信)
--   - 2 alert_rule (CPU 80% / 内存 85%)
--   - 3 model_config 默认 (gpt-3.5/4/claude-3)
-- 
-- BCrypt 密码哈希 (10 rounds) 跟 AdminDataInitializer 兼容
-- 沙箱模式 (h2local): AdminDataInitializer 自动建账号, 不用跑本文件
-- 生产模式 (mariadb/mysql): 手动跑 mysql -uroot -p < V3.5.58-seed.sql
-- =============================================================

USE minimax_platform;

-- ============== 1. 5 测试账号 (BCrypt 10 rounds) ==============

INSERT INTO sys_user (username, password, nickname, email, status, tenant_id, remark, created_at, updated_at, deleted) VALUES ('adminLiugl', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Adminliugl', 'liugl951127@gmail.com', 1, 0, '平台所有者 (唯一超级管理员)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
INSERT INTO sys_user (username, password, nickname, email, status, tenant_id, remark, created_at, updated_at, deleted) VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Admin', 'admin@minimax.io', 1, 0, '管理员 (旧版, 兼容)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
INSERT INTO sys_user (username, password, nickname, email, status, tenant_id, remark, created_at, updated_at, deleted) VALUES ('admin_user', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Admin User', 'admin_user@minimax.io', 1, 0, '沙箱测试账号 (管理员)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
INSERT INTO sys_user (username, password, nickname, email, status, tenant_id, remark, created_at, updated_at, deleted) VALUES ('test_user', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Test User', 'test_user@minimax.io', 1, 0, '沙箱测试账号 (普通用户)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
INSERT INTO sys_user (username, password, nickname, email, status, tenant_id, remark, created_at, updated_at, deleted) VALUES ('demo_user', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Demo User', 'demo_user@minimax.io', 1, 2, 'Demo 租户 (租户 ID=2)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ============== 2. 3 角色 ==============
INSERT INTO sys_role (code, name, description, sort, status, created_at, updated_at, deleted) VALUES ('SUPER_ADMIN', '超级管理员 (adminLiugl)', '拥有平台所有权限, 包括管理其他管理员', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
INSERT INTO sys_role (code, name, description, sort, status, created_at, updated_at, deleted) VALUES ('ADMIN', '管理员', '管理员, 拥有大部分业务权限', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
INSERT INTO sys_role (code, name, description, sort, status, created_at, updated_at, deleted) VALUES ('USER', '普通用户', '普通用户, 受限权限', 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ============== 3. 3 租户 ==============
INSERT INTO tenant (id, code, name, plan, status, max_users, max_models, qps_limit, monthly_quota, used_quota, contact_email, contact_phone, remark, created_at, updated_at) VALUES (0, 'default', '默认租户', 'enterprise', 1, 999, 999, 1000, 1000000, 0, 'admin@minimax.io', '13900000000', '平台默认租户', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO tenant (id, code, name, plan, status, max_users, max_models, qps_limit, monthly_quota, used_quota, contact_email, contact_phone, remark, created_at, updated_at) VALUES (1, 'demo', 'Demo 租户', 'demo', 1, 100, 50, 100, 100000, 0, 'demo@minimax.io', '13900000001', 'Demo 测试租户', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO tenant (id, code, name, plan, status, max_users, max_models, qps_limit, monthly_quota, used_quota, contact_email, contact_phone, remark, created_at, updated_at) VALUES (2, 'minimax', 'minimax 测试租户', 'enterprise', 1, 999, 999, 1000, 1000000, 0, 'minimax@minimax.io', '13900000002', 'minimax 测试租户', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============== 4. 5 账号 + 3 角色 关联 ==============
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO sys_user_role (user_id, role_id) VALUES (2, 2);
INSERT INTO sys_user_role (user_id, role_id) VALUES (3, 2);
INSERT INTO sys_user_role (user_id, role_id) VALUES (4, 3);
INSERT INTO sys_user_role (user_id, role_id) VALUES (5, 3);

-- ============== 5. 30 AI 关键词 ==============
INSERT INTO ai_intent_keyword (intent, keyword, weight, is_regex, enabled) VALUES
  ('CHART', 'chart', 3, 0, 1),
  ('CHART', '图表', 3, 0, 1),
  ('CHART', '可视化', 3, 0, 1),
  ('CHART', '柱状图', 5, 0, 1),
  ('CHART', '折线图', 5, 0, 1),
  ('CHART', '饼图', 5, 0, 1),
  ('CODE_GENERATE', '代码生成', 5, 0, 1),
  ('CODE_GENERATE', 'code', 3, 0, 1),
  ('CODE_GENERATE', '生成代码', 5, 0, 1),
  ('CODE_GENERATE', '生成项目', 8, 0, 1),
  ('DATA_ANALYSIS', '数据分析', 5, 0, 1),
  ('DATA_ANALYSIS', 'analysis', 3, 0, 1),
  ('DATA_ANALYSIS', '分析', 3, 0, 1),
  ('DATA_ANALYSIS', '统计', 3, 0, 1),
  ('NL2SQL', '查询', 3, 0, 1),
  ('NL2SQL', 'sql', 3, 0, 1),
  ('NL2SQL', 'sql 生成', 5, 0, 1),
  ('NL2SQL', '数据查询', 5, 0, 1),
  ('REPORT', '报告', 5, 0, 1),
  ('REPORT', 'report', 3, 0, 1),
  ('REPORT', '周报', 5, 0, 1),
  ('REPORT', '月报', 5, 0, 1),
  ('PROJECT', '项目下载', 8, 0, 1),
  ('PROJECT', '项目代码', 8, 0, 1),
  ('PROJECT', 'spring boot', 5, 0, 1),
  ('PROJECT', '脚手架', 5, 0, 1),
  ('IMAGE', '画图', 5, 0, 1),
  ('IMAGE', '图像生成', 5, 0, 1),
  ('PPT', 'ppt', 5, 0, 1),
  ('PPT', '演示文稿', 5, 0, 1);

-- ============== 6. 3 alert_channel ==============
INSERT INTO alert_channel (name, channel_type, type, target, config, enabled, priority, description, template, created_by, created_at) VALUES ('钉钉群', 'DINGTALK', 'dingtalk', 'https://oapi.dingtalk.com/robot/send?access_token=PLACEHOLDER', 'PLACEHOLDER', 1, 1, '钉钉群机器人告警', '{"atMobiles": [], "isAtAll": false}', 1, CURRENT_TIMESTAMP);
INSERT INTO alert_channel (name, channel_type, type, target, config, enabled, priority, description, template, created_by, created_at) VALUES ('运维邮箱', 'EMAIL', 'smtp', 'smtp.minimax.io', 'ops@minimax.io', 1, 2, '运维邮箱告警', '{"subject": "告警通知"}', 1, CURRENT_TIMESTAMP);
INSERT INTO alert_channel (name, channel_type, type, target, config, enabled, priority, description, template, created_by, created_at) VALUES ('企业微信群', 'WECOM', 'wecom', 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=PLACEHOLDER', 'PLACEHOLDER', 1, 3, '企业微信群机器人', '{}', 1, CURRENT_TIMESTAMP);

-- ============== 7. 2 alert_rule ==============
INSERT INTO alert_rule (name, description, metric_name, service, operator, threshold, severity, cooldown_minutes, enabled, tags, notify_channel, created_at, updated_at) VALUES ('CPU_HIGH', 'CPU 使用率过高', 'cpu_usage', 'minimax-ai', '>', 80.0, 'WARNING', 5, 1, 'cpu,memory', 'DINGTALK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO alert_rule (name, description, metric_name, service, operator, threshold, severity, cooldown_minutes, enabled, tags, notify_channel, created_at, updated_at) VALUES ('MEMORY_HIGH', '内存使用率过高', 'memory_usage', 'minimax-ai', '>', 85.0, 'CRITICAL', 5, 1, 'memory,disk', 'EMAIL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============== 8. 3 model_config ==============
INSERT INTO model_config (model_code, display_name, max_context, max_output, input_price, output_price, supports_vision, supports_tools, supports_stream, enabled, sort, description, created_at, updated_at) VALUES ('gpt-3.5-turbo', 'OpenAI GPT-3.5', 4096, 2048, 0.0015, 0.002, 0, 1, 1, 1, 0, 'OpenAI GPT-3.5 Turbo 模型', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO model_config (model_code, display_name, max_context, max_output, input_price, output_price, supports_vision, supports_tools, supports_stream, enabled, sort, description, created_at, updated_at) VALUES ('gpt-4', 'OpenAI GPT-4', 8192, 4096, 0.03, 0.06, 1, 1, 1, 1, 0, 'OpenAI GPT-4 模型, 支持视觉 + 工具', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO model_config (model_code, display_name, max_context, max_output, input_price, output_price, supports_vision, supports_tools, supports_stream, enabled, sort, description, created_at, updated_at) VALUES ('claude-3-haiku', 'Claude 3 Haiku', 200000, 4096, 0.00025, 0.00125, 1, 0, 1, 1, 0, 'Anthropic Claude 3 Haiku 模型, 长上下文 + 视觉', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============== 9. 索引 ==============
CREATE INDEX IF NOT EXISTS idx_ai_intent_keyword_intent ON ai_intent_keyword (intent);
CREATE INDEX IF NOT EXISTS idx_ai_intent_keyword_enabled ON ai_intent_keyword (enabled);
CREATE INDEX IF NOT EXISTS idx_sys_user_username ON sys_user (username);
CREATE INDEX IF NOT EXISTS idx_alert_rule_enabled ON alert_rule (enabled);

-- =============================================================
-- V3.5.50 seed-data.sql 完
-- =============================================================
