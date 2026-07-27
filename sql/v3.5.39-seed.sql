-- =============================================================
-- MiniMax Platform V3.5.37 Seed Data
-- 重新生成时间: Mon Jul 27 01:51:07 UTC 2026
-- 目标: MySQL 8.0+ / MariaDB 10.4+
-- 用法: mysql -uroot -proot123456 < sql/v3.5.37-seed.sql
-- 
-- 内容:
--   - 5 测试账号 (adminLiugl / admin / admin_user / test_user / demo_user)
--   - 22 AI 关键词 (CHART/CODE_GENERATE/DATA_ANALYSIS/NL2SQL/REPORT)
-- 
-- BCrypt 密码哈希由 AdminDataInitializer 启动时生成 (10 rounds)
-- 沙箱模式 (h2local) 自动跑 5 账号兜底, 生产模式 (mysql) 用本文件
-- =============================================================

USE minimax_platform;

-- ============== 1. 5 测试账号 (明文密码) ==============
-- 注: BCrypt 哈希在 AdminDataInitializer.java 维护, 启动时生成
-- 本表只放 username/明文/角色, 沙箱兜底自动转 BCrypt

INSERT INTO sys_user (username, password, nickname, email, status, tenant_id, remark, created_at, updated_at, deleted) VALUES ('adminLiugl', 'Liugl@2026', 'adminLiugl', 'liugl951127@gmail.com', '1', 0, '平台所有者 (唯一超级管理员)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
INSERT INTO sys_user (username, password, nickname, email, status, tenant_id, remark, created_at, updated_at, deleted) VALUES ('admin', 'admin@123', 'admin', 'admin@minimax.io', '1', 0, '管理员 (旧版, 兼容)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
INSERT INTO sys_user (username, password, nickname, email, status, tenant_id, remark, created_at, updated_at, deleted) VALUES ('admin_user', 'admin123', 'admin_user', 'admin_user@minimax.io', '1', 0, '沙箱测试账号 (管理员)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
INSERT INTO sys_user (username, password, nickname, email, status, tenant_id, remark, created_at, updated_at, deleted) VALUES ('test_user', 'user123', 'test_user', 'test_user@minimax.io', '1', 0, '沙箱测试账号 (普通用户)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
INSERT INTO sys_user (username, password, nickname, email, status, tenant_id, remark, created_at, updated_at, deleted) VALUES ('demo_user', 'demo1234', 'demo_user', 'demo_user@minimax.io', '1', 0, 'Demo 租户 (租户 ID=2)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- 5 账号 + 3 角色 (SUPER_ADMIN/ADMIN/USER) 关联表
-- AdminDataInitializer 启动时自动建关联, 这里不重复

-- ============== 2. AI 关键词 (22 个, V3.5.15 4 模型加权) ==============
-- CHART/CODE_GENERATE/DATA_ANALYSIS/NL2SQL/REPORT 5 大意图
-- 权重 1-10, 越高匹配越优先 (TF/Ngram/Synonym/Context 加权)

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
  ('REPORT', '月报', 5, 0, 1);

-- 3 索引 (高频查询加速)
CREATE INDEX IF NOT EXISTS idx_ai_intent_keyword_intent ON ai_intent_keyword (intent);
CREATE INDEX IF NOT EXISTS idx_ai_intent_keyword_enabled ON ai_intent_keyword (enabled);

-- =============================================================
-- V3.5.37 seed-data.sql 完 (5 账号 + 22 关键词)
-- =============================================================
