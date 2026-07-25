-- =============================================================
-- MiniMax Platform V3.5.19 Seed (5 测试账号 + 关键词)
-- AdminDataInitializer 也会兜底初始化 (沙箱模式)
-- =============================================================

USE minimax_platform;

-- 5 测试账号 (BCrypt 10 rounds 加密, 见 AdminDataInitializer)
-- adminLiugl / Liugl@2026 / SUPER_ADMIN
-- admin / admin@123 / ADMIN
-- admin_user / admin123 / ADMIN
-- test_user / user123 / USER
-- demo_user / demo1234 / USER

-- 注: BCrypt 加密后的 hash 在 AdminDataInitializer.java 维护
-- 沙箱模式 (h2local) 会自动跑 5 账号兜底
-- 生产模式 (mysql) 用此文件 + AdminDataInitializer 双保险

-- AI 关键词 (V3.5.15 4 模型加权 + 149 同义词组)
INSERT INTO ai_intent_keyword (intent, keyword, weight, is_regex, enabled) VALUES
  ('CHART',         'chart',         3, 0, 1),
  ('CHART',         '图表',          3, 0, 1),
  ('CHART',         '可视化',        3, 0, 1),
  ('CHART',         '柱状图',        5, 0, 1),
  ('CHART',         '折线图',        5, 0, 1),
  ('CHART',         '饼图',          5, 0, 1),
  ('CODE_GENERATE', '代码生成',      5, 0, 1),
  ('CODE_GENERATE', 'code',          3, 0, 1),
  ('CODE_GENERATE', '生成代码',      5, 0, 1),
  ('CODE_GENERATE', '生成项目',      8, 0, 1),
  ('DATA_ANALYSIS', '数据分析',      5, 0, 1),
  ('DATA_ANALYSIS', '分析',          2, 0, 1),
  ('DATA_ANALYSIS', '统计',          3, 0, 1),
  ('KNOWLEDGE',     '知识库',        5, 0, 1),
  ('KNOWLEDGE',     'rag',           3, 0, 1),
  ('KNOWLEDGE',     '文档',          2, 0, 1),
  ('TOOL',          '工具',          3, 0, 1),
  ('TOOL',          'tool',          3, 0, 1),
  ('CHAT',          '聊天',          2, 0, 1),
  ('CHAT',          'chat',          2, 0, 1),
  ('CHAT',          '你好',          1, 0, 1),
  ('CHAT',          'hi',            1, 0, 1)
;
