-- =====================================================
-- MiniMax Platform V3.5.8 - Seed Data
-- 4 测试账号 + 业务核心数据 (BCrypt 10 rounds)
-- 字段完全对齐 entity (snake_case)
-- =====================================================
SET NAMES utf8mb4;

-- ===== 1. 租户 (2) =====
INSERT INTO `tenant` (`id`, `code`, `name`, `plan`, `status`, `max_users`, `max_models`, `qps_limit`, `monthly_quota`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'minimax', 'MiniMax 演示租户', 'enterprise', 1, 100, 50, 1000, 1000000, NOW(), NOW(), 0),
(2, 'demo',     'Demo 演示租户',     'free',       1, 10,  5,  100,  100000,  NOW(), NOW(), 0);

-- ===== 2. 角色 (3) =====
INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'super_admin', '超级管理员', '系统内置超级管理员', NOW(), NOW(), 0),
(2, 'admin',       '管理员',     '系统内置管理员',     NOW(), NOW(), 0),
(3, 'user',        '普通用户',   '系统内置普通用户',   NOW(), NOW(), 0);

-- ===== 3. 用户 (4 测试账号, BCrypt 10 rounds) =====
-- adminLiugl/Liugl@2026 -> $2a$10$fyhHrqgwteAj5eqesr7yw.OPv3N4SvVj17qkqZN8Kq1/PO5vn3P6W
-- admin_user/admin123   -> $2a$10$VqCU4L5qLu.6R49QzTI/Ne.fbA31HuuDTHkIFN7IeWWjLJkRZFTBi
-- test_user/user123     -> $2a$10$AgpCEcOuWI.ciFYMWDC5Nekyy1TY7ngNGF4myEip3j/MCGtFh5uoq
-- demo_user/demo1234    -> $2a$10$/CTAFwg2cuOINRXzlKV0E.dd02V.nEG1P2cp82nnYrolVvH8ii5JS
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `gender`, `status`, `tenant_id`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'adminLiugl', '$2a$10$fyhHrqgwteAj5eqesr7yw.OPv3N4SvVj17qkqZN8Kq1/PO5vn3P6W', '超级管理员', 'admin@minimax.io',  '13800138000', 1, 1, 1, NOW(), NOW(), 0),
(2, 'admin_user', '$2a$10$VqCU4L5qLu.6R49QzTI/Ne.fbA31HuuDTHkIFN7IeWWjLJkRZFTBi', '管理员',     'admin2@minimax.io', '13800138001', 1, 1, 1, NOW(), NOW(), 0),
(3, 'test_user',  '$2a$10$AgpCEcOuWI.ciFYMWDC5Nekyy1TY7ngNGF4myEip3j/MCGtFh5uoq', '测试用户',   'test@minimax.io',   '13800138002', 1, 1, 1, NOW(), NOW(), 0),
(4, 'demo_user',  '$2a$10$/CTAFwg2cuOINRXzlKV0E.dd02V.nEG1P2cp82nnYrolVvH8ii5JS', '演示用户',   'demo@minimax.io',   '13800138003', 1, 1, 2, NOW(), NOW(), 0);

-- ===== 4. AI 工具 (5) =====
INSERT INTO `ai_tool` (`id`, `code`, `name`, `category`, `description`, `enabled`, `builtin`, `impl_type`, `impl_value`, `rate_limit`, `timeout_seconds`, `tags`, `version`, `author`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'web_search',  'Web 搜索',  'search',     '搜索引擎查询',   1, 1, 'http',    'web-search-api',  100, 30, '',   '1.0.0', 'system', NOW(), NOW(), 0),
(2, 'code_runner', '代码执行',  'compute',    'Python 沙箱执行', 1, 1, 'sandbox', 'python-sandbox',  50,  60, 'compute,code', '1.0.0', 'system', NOW(), NOW(), 0),
(3, 'image_gen',   '图像生成',  'multimodal', 'AI 文生图',       1, 1, 'http',    'image-gen-api',   30,  120,'',   '1.0.0', 'system', NOW(), NOW(), 0),
(4, 'sql_query',   'SQL 查询',  'data',       '数据库查询',     1, 1, 'jdbc',    'dynamic-jdbc',    200, 30, 'data,sql',     '1.0.0', 'system', NOW(), NOW(), 0),
(5, 'file_reader', '文件读取',  'data',       '文档解析',       1, 1, 'local',   'file-parser',     100, 60, 'data,file',    '1.0.0', 'system', NOW(), NOW(), 0);

-- ===== 5. AI 意图关键词 (20) =====
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `is_regex`, `enabled`, `language`, `created_at`, `updated_at`) VALUES
(1,  'data_query',     '查询',     0.95, 0, 1, 'zh', NOW(), NOW()),
(2,  'data_query',     '多少',     0.90, 0, 1, 'zh', NOW(), NOW()),
(3,  'data_analyze',   '分析',     0.95, 0, 1, 'zh', NOW(), NOW()),
(4,  'data_analyze',   '趋势',     0.90, 0, 1, 'zh', NOW(), NOW()),
(5,  'data_compare',   '对比',     0.95, 0, 1, 'zh', NOW(), NOW()),
(6,  'data_compare',   '比较',     0.90, 0, 1, 'zh', NOW(), NOW()),
(7,  'data_predict',   '预测',     0.95, 0, 1, 'zh', NOW(), NOW()),
(8,  'data_predict',   '预估',     0.90, 0, 1, 'zh', NOW(), NOW()),
(9,  'data_visualize', '画图',     0.95, 0, 1, 'zh', NOW(), NOW()),
(10, 'data_visualize', '柱状图',   0.90, 0, 1, 'zh', NOW(), NOW()),
(11, 'data_visualize', '折线图',   0.90, 0, 1, 'zh', NOW(), NOW()),
(12, 'data_visualize', '饼图',     0.90, 0, 1, 'zh', NOW(), NOW()),
(13, 'data_report',    '月报',     0.95, 0, 1, 'zh', NOW(), NOW()),
(14, 'data_report',    '周报',     0.95, 0, 1, 'zh', NOW(), NOW()),
(15, 'consult',        '什么是',   0.90, 0, 1, 'zh', NOW(), NOW()),
(16, 'complaint',      '投诉',     0.95, 0, 1, 'zh', NOW(), NOW()),
(17, 'complaint',      '不满',     0.85, 0, 1, 'zh', NOW(), NOW()),
(18, 'chat',           '你好',     0.80, 0, 1, 'zh', NOW(), NOW()),
(19, 'chat',           '转人工',   0.85, 0, 1, 'zh', NOW(), NOW()),
(20, 'image_gen',      '生成图片', 0.95, 0, 1, 'zh', NOW(), NOW());

-- ===== 6. AI 数据源 (1) =====
INSERT INTO `analytics_datasource` (`id`, `user_id`, `name`, `type`, `jdbc_url`, `username`, `password_enc`, `description`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, '内置 H2', 'h2', 'jdbc:h2:mem:minimax_ai', 'sa', '', '内置 H2 测试数据源', NOW(), NOW(), 0);

-- ===== 7. 知识库权限 (4) =====
INSERT INTO `kb_permission` (`id`, `kb_id`, `subject_type`, `subject_id`, `permission`, `grant_by`, `created_at`) VALUES
(1, 'kb_default', 'ROLE', 1, 'ADMIN', 1, NOW()),
(2, 'kb_default', 'ROLE', 2, 'WRITE', 1, NOW()),
(3, 'kb_default', 'ROLE', 3, 'READ',  1, NOW()),
(4, 'kb_default', 'USER', 1, 'ADMIN', 1, NOW());

-- ===== 8. 模型供应商 (3) =====
INSERT INTO `model_provider` (`id`, `code`, `name`, `base_url`, `api_key`, `protocol`, `enabled`, `sort`, `description`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'self',    '自研引擎', '/api/v1/ai',  '',     'openai', 1, 1, '自研 MiniMax AI 引擎',  NOW(), NOW(), 0),
(2, 'mock',    'Mock 模拟', '/mock',       'mock', 'openai', 1, 2, 'Mock 模拟, 用于开发',    NOW(), NOW(), 0),
(3, 'openai',  'OpenAI',   'https://api.openai.com/v1', '', 'openai', 0, 3, 'OpenAI 官方',  NOW(), NOW(), 0);

-- ===== 9. 模型配置 (4) =====
INSERT INTO `model_config` (`id`, `provider_id`, `model_code`, `display_name`, `max_context`, `max_output`, `supports_vision`, `supports_tools`, `supports_stream`, `enabled`, `sort`, `description`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, 'gpt-4o',         'GPT-4o',         8192, 4096, 1, 1, 1, 1, 1, 'GPT-4o 多模态',     NOW(), NOW(), 0),
(2, 1, 'claude-3.5',     'Claude 3.5',     8192, 4096, 1, 1, 1, 1, 2, 'Claude 3.5 Sonnet', NOW(), NOW(), 0),
(3, 1, 'deepseek-coder', 'DeepSeek Coder', 8192, 4096, 0, 1, 1, 1, 3, 'DeepSeek 代码',     NOW(), NOW(), 0),
(4, 1, 'gemini-pro',     'Gemini Pro',     8192, 4096, 1, 1, 1, 1, 4, 'Gemini Pro',        NOW(), NOW(), 0);

-- ===== 10. 告警规则 (3) =====
INSERT INTO `alert_rule` (`id`, `name`, `description`, `metric_name`, `service`, `operator`, `threshold`, `severity`, `cooldown_minutes`, `enabled`, `created_at`, `updated_at`) VALUES
(1, 'CPU 高',   'CPU 使用率过高', 'cpu_usage',  'minimax-ai',     '>',  80, 'warning',  5, 1, NOW(), NOW()),
(2, '内存高',   '内存使用率过高', 'mem_usage',  'minimax-gateway','>',  85, 'warning',  5, 1, NOW(), NOW()),
(3, '错误率高', '5xx 错误率过高', 'err_rate',   'minimax-ai',     '>',  5,  'critical', 3, 1, NOW(), NOW());

-- ===== 11. 告警渠道 (3) =====
INSERT INTO `alert_channel` (`id`, `name`, `channel_type`, `type`, `target`, `config`, `enabled`, `priority`, `description`, `created_by`, `created_at`) VALUES
(1, '邮件',    'email',    'email',    'admin@minimax.io',  '{"to":"admin@minimax.io"}', 1, 1, '邮件通知', 1, NOW()),
(2, 'Webhook','webhook',  'webhook',  'http://localhost:9090/alert', '{"url":"http://localhost:9090/alert"}', 1, 2, 'Webhook 推送', 1, NOW()),
(3, '钉钉',    'dingtalk', 'dingtalk', 'mock-token',        '{"token":"mock"}', 0, 3, '钉钉机器人 (未启用)', 1, NOW());

-- ===== 12. Prompt 模板 (5) =====
INSERT INTO `prompt_template` (`id`, `name`, `description`, `category`, `content`, `is_public`, `use_count`, `created_at`, `updated_at`, `deleted`) VALUES
(1, '默认对话',     '通用对话',     'chat',     '你是 MiniMax AI 助手, 友好且专业', 1, 0, NOW(), NOW(), 0),
(2, '数据分析',     '数据分析',     'analysis', '你是数据分析师, 帮用户分析数据', 1, 0, NOW(), NOW(), 0),
(3, '代码生成',     '代码生成',     'code',     '你是工程师, 写可读可运行的代码', 1, 0, NOW(), NOW(), 0),
(4, 'RAG 检索',     'RAG 检索',     'rag',      '根据上下文回答用户问题',         1, 0, NOW(), NOW(), 0),
(5, '意图识别',     '意图识别',     'intent',   '分析用户输入, 返回意图分类',     1, 0, NOW(), NOW(), 0);

-- ===== 13. Function 工具 (3) =====
INSERT INTO `function_tool` (`id`, `name`, `display_name`, `description`, `category`, `scope`, `owner_id`, `parameters`, `enabled`, `tags`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'get_weather', '天气查询',  '查询某地天气',     'data',   'public', 1, '{}', 1, 'weather,api',   NOW(), NOW(), 0),
(2, 'send_email',  '发邮件',    '发送邮件',         'notify', 'public', 1, '{}', 1, 'email,notify',  NOW(), NOW(), 0),
(3, 'query_db',    '数据库查询', '查询业务数据库', 'data',   'public', 1, '{}', 1, 'database,data', NOW(), NOW(), 0);

-- ===== 14. Plugin 插件 (3) =====
INSERT INTO `plugin` (`id`, `name`, `display_name`, `description`, `version`, `author`, `category`, `scope`, `owner_id`, `enabled`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'web-search', 'Web 搜索',  '集成多种搜索引擎',  '1.0.0', 'system', 'search',     'public', 1, 1,  NOW(), NOW(), 0),
(2, 'image-gen',  'AI 生图',   '调用图像生成 API',  '1.0.0', 'system', 'multimodal', 'public', 1, 1,  NOW(), NOW(), 0),
(3, 'data-viz',   '数据可视化','生成各类图表',      '1.0.0', 'system', 'data',       'public', 1, 1,   NOW(), NOW(), 0);

-- ===== 15. Pipeline 工作流 (3) =====
INSERT INTO `pipeline_workflow` (`id`, `name`, `description`, `definition`, `version`, `status`, `create_by`, `create_time`, `update_time`, `deleted`) VALUES
(1, 'ETL 流程',  '数据抽取转换加载', '{"nodes":[]}',  1, 1, 1, NOW(), NOW(), 0),
(2, 'AI 训练',   '模型训练流程',     '{"nodes":[]}',  1, 1, 1, NOW(), NOW(), 0),
(3, '报告生成', '自动生成周报',     '{"nodes":[]}',  1, 1, 1, NOW(), NOW(), 0);

-- ===== 16. 知识库 (2) =====
INSERT INTO `knowledge_base` (`id`, `owner_id`, `tenant_id`, `name`, `description`, `visibility`, `tags`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, 1, '产品文档',  'MiniMax 平台文档',      'public', 'doc,product', NOW(), NOW(), 0),
(2, 1, 1, '技术规范',  '开发规范与最佳实践',     'public', 'doc,tech',    NOW(), NOW(), 0);

-- ===== 17. 文档 (5) =====
INSERT INTO `document` (`id`, `kb_id`, `owner_id`, `title`, `source_type`, `content`, `size_bytes`, `status`, `tags`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, 1, '快速开始',    'manual',   '快速开始内容', 1024, 1, 'guide',     NOW(), NOW(), 0),
(2, 1, 1, 'API 文档',    'manual',   'API 文档',     2048, 1, 'api',       NOW(), NOW(), 0),
(3, 1, 1, '部署指南',    'manual',   '部署指南',     4096, 1, 'deploy',    NOW(), NOW(), 0),
(4, 2, 1, '编码规范',    'manual',   '编码规范',     512,  1, 'standard',  NOW(), NOW(), 0),
(5, 2, 1, 'Git 流程',    'manual',   'Git 流程',     1024, 1, 'git',       NOW(), NOW(), 0);

-- ===== 18. Dashboard 指标 (3) =====
INSERT INTO `dashboard_metric` (`id`, `metric`, `dimension`, `value`, `tags`, `timestamp`) VALUES
(1, 'total_users',    'global', 4,   '{"scope":"platform"}', NOW()),
(2, 'total_services', 'global', 17,  '{"scope":"platform"}', NOW()),
(3, 'total_apis',     'global', 517, '{"scope":"platform"}', NOW());

-- ===== 19. 知识图谱实体 (5) =====
INSERT INTO `kg_entity` (`id`, `user_id`, `name`, `entity_type`, `description`, `importance`, `source`, `ref_count`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, 'MiniMax Platform', 'product', '本平台',  10, 'manual', 0, NOW(), NOW(), 0),
(2, 1, 'RAG',              'tech',    '检索增强',  8, 'manual', 0, NOW(), NOW(), 0),
(3, 1, 'AI 引擎',          'tech',    '自研推理',  8, 'manual', 0, NOW(), NOW(), 0),
(4, 1, '17 微服务',        'arch',    '系统架构',  7, 'manual', 0, NOW(), NOW(), 0),
(5, 1, 'Vue 3',            'tech',    '前端框架',  6, 'manual', 0, NOW(), NOW(), 0);

-- ===== 20. 知识图谱关系 (4) =====
INSERT INTO `kg_relation` (`id`, `user_id`, `from_entity`, `to_entity`, `relation_type`, `description`, `weight`, `source`, `ref_count`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, 1, 2, 'uses',     'Platform 使用 RAG',         0.95, 'manual', 0, NOW(), NOW(), 0),
(2, 1, 1, 3, 'contains', 'Platform 包含 AI 引擎',     0.95, 'manual', 0, NOW(), NOW(), 0),
(3, 1, 1, 4, 'contains', 'Platform 包含 17 微服务',   0.95, 'manual', 0, NOW(), NOW(), 0),
(4, 1, 1, 5, 'uses',     'Platform 前端用 Vue 3',     0.90, 'manual', 0, NOW(), NOW(), 0);

-- ===== 21. 集群节点 (1) =====
INSERT INTO `cluster_node` (`id`, `node_id`, `name`, `address`, `region`, `capabilities`, `total_cores`, `total_memory_mb`, `status`, `is_leader`, `started_at`, `created_at`, `updated_at`) VALUES
(1, 'node-1', 'node-1', '127.0.0.1', 'local', '{"cpu":true,"gpu":false}', 8, 8192, 1, 1, NOW(), NOW(), NOW());

-- ===== 22. 通知 (2) =====
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `is_read`, `created_at`, `updated_at`) VALUES
(1, 1, 'system', '欢迎使用 MiniMax', '平台已就绪, 4 个测试账号可用', 0, NOW(), NOW()),
(2, 1, 'system', '系统升级',         'V3.5.8 部署完成, 17 服务在线', 0, NOW(), NOW());
