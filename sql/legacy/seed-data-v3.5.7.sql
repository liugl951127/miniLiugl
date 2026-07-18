-- =====================================================
-- MiniMax Platform V3.5.8 - Seed Data
-- 4 测试账号 (BCrypt 10 rounds) + 业务核心
-- 字段完全对齐 entity, 40 段 INSERT
-- =====================================================
SET NAMES utf8mb4;

-- ===== 1. 租户 (2) =====
INSERT INTO `tenant` (`id`, `code`, `name`, `plan`, `status`, `max_users`, `max_models`, `qps_limit`, `monthly_quota`, `used_quota`, `expire_at`, `contact_email`, `contact_phone`, `remark`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'minimax', 'MiniMax 演示租户', 'enterprise', 1, 100, 50, 1000, 1000000, 0, NULL, 'admin@minimax.io', '13800138000', '系统租户', NOW(), NOW(), 0),
(2, 'demo', 'Demo 演示租户', 'free', 1, 10, 5, 100, 100000, 0, NULL, 'demo@minimax.io', '13800138003', '演示租户', NOW(), NOW(), 0);

-- ===== 2. 角色 (3) =====
INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `sort`, `enabled`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'super_admin', '超级管理员', 0, '系统内置超级管理员', 1, NOW(), NOW(), 0),
(2, 'admin', '管理员',     1, '系统内置管理员',     1, NOW(), NOW(), 0),
(3, 'user', '普通用户',   2, '系统内置普通用户',   1, NOW(), NOW(), 0);

-- ===== 3. 用户 (4 测试账号, BCrypt 10 rounds) =====
-- adminLiugl/Liugl@2026 -> $2a$10$fyhHrqgwteAj5eqesr7yw.OPv3N4SvVj17qkqZN8Kq1/PO5vn3P6W
-- admin_user/admin123   -> $2a$10$VqCU4L5qLu.6R49QzTI/Ne.fbA31HuuDTHkIFN7IeWWjLJkRZFTBi
-- test_user/user123     -> $2a$10$AgpCEcOuWI.ciFYMWDC5Nekyy1TY7ngNGF4myEip3j/MCGtFh5uoq
-- demo_user/demo1234    -> $2a$10$/CTAFwg2cuOINRXzlKV0E.dd02V.nEG1P2cp82nnYrolVvH8ii5JS
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `avatar`, `gender`, `status`, `last_login_ip`, `last_login_at`, `tenant_id`, `remark`, `wechat_openid`, `wechat_unionid`, `wechat_nickname`, `wechat_avatar`, `wechat_bound_at`, `qq_openid`, `qq_unionid`, `qq_nickname`, `qq_avatar`, `qq_bound_at`, `alipay_openid`, `alipay_user_id`, `alipay_nickname`, `alipay_avatar`, `alipay_bound_at`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted`) VALUES
(1, 'adminLiugl', '$2a$10$fyhHrqgwteAj5eqesr7yw.OPv3N4SvVj17qkqZN8Kq1/PO5vn3P6W', '超级管理员', 'admin@minimax.io',  '13800138000', '', 1, 1, 1, NULL, 0, 1, '系统内置超级管理员', NULL, NOW(), NOW(), 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
(2, 'admin_user', '$2a$10$VqCU4L5qLu.6R49QzTI/Ne.fbA31HuuDTHkIFN7IeWWjLJkRZFTBi', '管理员',     'admin2@minimax.io', '13800138001', '', 1, 1, 1, NULL, 0, 1, '演示管理员账号',     1,    NOW(), NOW(), 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
(3, 'test_user',  '$2a$10$AgpCEcOuWI.ciFYMWDC5Nekyy1TY7ngNGF4myEip3j/MCGtFh5uoq', '测试用户',   'test@minimax.io',   '13800138002', '', 1, 1, 1, NULL, 0, 1, '测试用户',            1,    NOW(), NOW(), 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
(4, 'demo_user',  '$2a$10$/CTAFwg2cuOINRXzlKV0E.dd02V.nEG1P2cp82nnYrolVvH8ii5JS', '演示用户',   'demo@minimax.io',   '13800138003', '', 1, 1, 2, NULL, 0, 1, '演示用户',            1,    NOW(), NOW(), 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NOW(), NOW(), 0);

-- ===== 4. 用户角色 (4) =====
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1), (2, 2), (3, 3), (4, 3);

-- ===== 5. AI 工具 (5) =====
INSERT INTO `ai_tool` (`id`, `code`, `name`, `category`, `description`, `icon`, `enabled`, `builtin`, `input_schema`, `output_schema`, `default_config`, `impl_type`, `impl_value`, `rate_limit`, `timeout_seconds`, `role_required`, `tags`, `version`, `author`, `created_by`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'web_search',  'Web 搜索',  'search',     '搜索引擎查询',   'icon-search', 1, 1, '{}', '{}', '{"timeout":30}', 'http',    'web-search-api',  100, 30, 'user', 'search,web',     '1.0.0', 'system', NULL, NOW(), NOW(), 0),
(2, 'code_runner', '代码执行',  'compute',    'Python 沙箱执行', 'icon-code',    1, 1, '{}', '{}', '{"timeout":60}', 'sandbox', 'python-sandbox',  50,  60, 'user', 'compute,code',   '1.0.0', 'system', NULL, NOW(), NOW(), 0),
(3, 'image_gen',   '图像生成',  'multimodal', 'AI 文生图',       'icon-image',   1, 1, '{}', '{}', '{"timeout":120}','http',    'image-gen-api',   30,  120,'user', 'image,aigc',     '1.0.0', 'system', NULL, NOW(), NOW(), 0),
(4, 'sql_query',   'SQL 查询',  'data',       '数据库查询',     'icon-sql',     1, 1, '{}', '{}', '{"timeout":30}', 'jdbc',    'dynamic-jdbc',    200, 30, 'user', 'data,sql',       '1.0.0', 'system', NULL, NOW(), NOW(), 0),
(5, 'file_reader', '文件读取',  'data',       '文档解析',       'icon-file',    1, 1, '{}', '{}', '{"timeout":60}', 'local',   'file-parser',     100, 60, 'user', 'data,file',      '1.0.0', 'system', NULL, NOW(), NOW(), 0);

-- ===== 6. AI 意图关键词 (20) =====
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `is_regex`, `enabled`, `language`, `remark`, `created_at`, `updated_at`) VALUES
(1, 'data_query', '查询', 0.95, 0, 1, 'zh', '查询', NOW(), NOW()),,,,,,,,,,,,,,,,,,,;

-- ===== 7. AI 数据源 (1) =====
INSERT INTO `analytics_datasource` (`id`, `user_id`, `name`, `type`, `jdbc_url`, `username`, `password_enc`, `description`, `deleted`, `created_at`, `updated_at`) VALUES
(1, 1, '内置 H2', 'h2', 'jdbc:h2:mem:minimax_ai', 'sa', '', '内置 H2 测试', NOW(), NOW(), 0);

-- ===== 8. 知识库权限 (4) =====
INSERT INTO `kb_permission` (`id`, `kb_id`, `subject_type`, `subject_id`, `permission`, `grant_by`, `created_at`) VALUES
(1, 'kb_default', 'ROLE', 1, 'ADMIN', 1, NOW()),
(2, 'kb_default', 'ROLE', 2, 'WRITE', 1, NOW()),
(3, 'kb_default', 'ROLE', 3, 'READ',  1, NOW()),
(4, 'kb_default', 'USER', 1, 'ADMIN', 1, NOW());

-- ===== 9. 模型供应商 (3) =====
INSERT INTO `model_provider` (`id`, `code`, `name`, `base_url`, `api_key`, `protocol`, `enabled`, `sort`, `description`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'self',   '自研引擎', '/api/v1/ai',  '',     'openai', 1, 1, '自研 MiniMax AI', NOW(), NOW(), 0),
(2, 'mock',   'Mock 模拟', '/mock',       'mock', 'openai', 1, 2, 'Mock 模拟',       NOW(), NOW(), 0),
(3, 'openai', 'OpenAI',   'https://api.openai.com/v1', '', 'openai', 0, 3, 'OpenAI 官方', NOW(), NOW(), 0);

-- ===== 10. 模型配置 (4) =====
INSERT INTO `model_config` (`id`, `provider_id`, `model_code`, `display_name`, `max_context`, `max_output`, `input_price`, `output_price`, `supports_vision`, `supports_tools`, `supports_stream`, `enabled`, `sort`, `description`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, 'gpt-4o',         'GPT-4o',         8192, 4096, 0.001,  0.002,  1, 1, 1, 1, 1, 'GPT-4o 多模态',     NOW(), NOW(), 0),
(2, 1, 'claude-3.5',     'Claude 3.5',     8192, 4096, 0.0008, 0.0024, 1, 1, 1, 1, 2, 'Claude 3.5 Sonnet', NOW(), NOW(), 0),
(3, 1, 'deepseek-coder', 'DeepSeek Coder', 8192, 4096, 0.0001, 0.0002, 0, 1, 1, 1, 3, 'DeepSeek 代码',     NOW(), NOW(), 0),
(4, 1, 'gemini-pro',     'Gemini Pro',     8192, 4096, 0.0005, 0.0015, 1, 1, 1, 1, 4, 'Gemini Pro',        NOW(), NOW(), 0);

-- ===== 11. 告警规则 (3) =====
INSERT INTO `alert_rule` (`id`, `name`, `description`, `metric_name`, `service`, `operator`, `threshold`, `severity`, `cooldown_minutes`, `enabled`, `tags`, `notify_channel`, `created_at`, `updated_at`) VALUES
(1, 'CPU 高',   'CPU 使用率过高',  'cpu_usage',  'minimax-ai',     '>',  80, 'warning',  5, 1, '', 'email',  NOW(), NOW()),
(2, '内存高',   '内存使用率过高',  'mem_usage',  'minimax-gateway','>',  85, 'warning',  5, 1, '', 'email',  NOW(), NOW()),
(3, '错误率高', '5xx 错误率过高',  'err_rate',   'minimax-ai',     '>',  5,  'critical', 3, 1, '', 'webhook',NOW(), NOW());

-- ===== 12. 告警渠道 (3) =====
INSERT INTO `alert_channel` (`id`, `name`, `channel_type`, `type`, `target`, `config`, `enabled`, `priority`, `description`, `template`, `created_by`, `created_at`) VALUES
(1, '邮件',    'email',    'email',    'admin@minimax.io',  '{"to":"admin@minimax.io"}',  1, 1, '邮件通知',  1, NOW()),
(2, 'Webhook','webhook',  'webhook',  'http://localhost:9090/alert', '{"url":"http://localhost:9090/alert"}', 1, 2, 'Webhook 推送', 1, NOW()),
(3, '钉钉',    'dingtalk', 'dingtalk', 'mock-token',        '{"token":"mock"}', 0, 3, '钉钉机器人 (未启用)', 1, NOW());

-- ===== 13. Prompt 模板 (5) =====
INSERT INTO `prompt_template` (`id`, `name`, `description`, `category`, `content`, `variables`, `creator_id`, `creator_name`, `is_public`, `use_count`, `created_at`, `updated_at`, `deleted`) VALUES
(1, '默认对话',     '通用对话',     'chat',     '你是 MiniMax AI 助手, 友好且专业',  NULL, 1, 'system', 0, 1, 0, NOW(), NOW(), 0),
(2, '数据分析',     '数据分析',     'analysis', '你是数据分析师, 帮用户分析数据',  NULL, 1, 'system', 0, 1, 0, NOW(), NOW(), 0),
(3, '代码生成',     '代码生成',     'code',     '你是工程师, 写可读可运行的代码',  NULL, 1, 'system', 0, 1, 0, NOW(), NOW(), 0),
(4, 'RAG 检索',     'RAG 检索',     'rag',      '根据上下文回答用户问题',          NULL, 1, 'system', 0, 1, 0, NOW(), NOW(), 0),
(5, '意图识别',     '意图识别',     'intent',   '分析用户输入, 返回意图分类',     NULL, 1, 'system', 0, 1, 0, NOW(), NOW(), 0);

-- ===== 14. Function 工具 (3) =====
INSERT INTO `function_tool` (`id`, `name`, `display_name`, `description`, `category`, `scope`, `owner_id`, `parameters`, `endpoint`, `http_method`, `enabled`, `tags`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'get_weather', '天气查询',  '查询某地天气',     'data',   'public', 1, '{}', '/api/weather',  'GET',    1, 'weather,api',   NOW(), NOW(), 0),
(2, 'send_email',  '发邮件',    '发送邮件',         'notify', 'public', 1, '{}', '/api/email',    'POST',   1, 'email,notify',  NOW(), NOW(), 0),
(3, 'query_db',    '数据库查询', '查询业务数据库', 'data',   'public', 1, '{}', '/api/db/query',  'POST',   1, 'database,data', NOW(), NOW(), 0);

-- ===== 15. Plugin 插件 (3) =====
INSERT INTO `plugin` (`id`, `name`, `display_name`, `description`, `version`, `author`, `category`, `scope`, `owner_id`, `icon`, `entry`, `plugin_type`, `config`, `enabled`, `downloads`, `rating`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'web-search', 'Web 搜索',  '集成多种搜索引擎',  '1.0.0', 'system', 'search',     'public', 1, '', 'http://localhost/web-search', 'http',     1, 0, 0, 0, NOW(), NOW(), 0),
(2, 'image-gen',  'AI 生图',   '调用图像生成 API',  '1.0.0', 'system', 'multimodal', 'public', 1, '', 'http://localhost/image-gen',  'http',     1, 0, 0, 0, NOW(), NOW(), 0),
(3, 'data-viz',   '数据可视化','生成各类图表',     '1.0.0', 'system', 'data',       'public', 1, '', 'http://localhost/data-viz',   'chart',    1, 0, 0, 0, NOW(), NOW(), 0);

-- ===== 16. Pipeline 工作流 (3) =====
INSERT INTO `pipeline_workflow` (`id`, `name`, `description`, `definition`, `version`, `status`, `create_by`, `update_by`, `create_time`, `update_time`, `deleted`) VALUES
(1, 'ETL 流程',  '数据抽取转换加载', '{"nodes":[]}',  1, 1, 1, 1, NOW(), NOW(), 0),
(2, 'AI 训练',   '模型训练流程',     '{"nodes":[]}',  1, 1, 1, 1, NOW(), NOW(), 0),
(3, '报告生成',  '自动生成周报',     '{"nodes":[]}',  1, 1, 1, 1, NOW(), NOW(), 0);

-- ===== 17. 知识库 (2) =====
INSERT INTO `knowledge_base` (`id`, `owner_id`, `tenant_id`, `name`, `description`, `visibility`, `doc_count`, `chunk_count`, `tags`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, 1, '产品文档',  'MiniMax 平台文档',     'public', 0, 0, 'doc,product', NOW(), NOW(), 0),
(2, 1, 1, '技术规范',  '开发规范与最佳实践',   'public', 0, 0, 'doc,tech',    NOW(), NOW(), 0);

-- ===== 18. 文档 (5) =====
INSERT INTO `document` (`id`, `kb_id`, `owner_id`, `title`, `source_type`, `source_uri`, `content`, `size_bytes`, `status`, `error_msg`, `chunk_count`, `checksum`, `tags`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, 1, '快速开始',    'manual', '', '快速开始内容', 1024, 1, NULL,  0, '', 'guide',     NOW(), NOW(), 0),
(2, 1, 1, 'API 文档',    'manual', '', 'API 文档',     2048, 1, NULL,  0, '', 'api',       NOW(), NOW(), 0),
(3, 1, 1, '部署指南',    'manual', '', '部署指南',     4096, 1, NULL,  0, '', 'deploy',    NOW(), NOW(), 0),
(4, 2, 1, '编码规范',    'manual', '', '编码规范',     512,  1, NULL,  0, '', 'standard',  NOW(), NOW(), 0),
(5, 2, 1, 'Git 流程',    'manual', '', 'Git 流程',     1024, 1, NULL,  0, '', 'git',       NOW(), NOW(), 0);

-- ===== 19. Dashboard 指标 (3) =====
INSERT INTO `dashboard_metric` (`id`, `metric`, `dimension`, `value`, `tags`, `timestamp`) VALUES
(1, 'total_users',    'global', 4,   '{"scope":"platform"}', NOW()),
(2, 'total_services', 'global', 17,  '{"scope":"platform"}', NOW()),
(3, 'total_apis',     'global', 517, '{"scope":"platform"}', NOW());

-- ===== 20. 知识图谱实体 (5) =====
INSERT INTO `kg_entity` (`id`, `user_id`, `name`, `entity_type`, `description`, `aliases`, `importance`, `source`, `ref_count`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, 'MiniMax Platform', 'product', '本平台',  'MiniMax,AI Platform', 10, 'manual', 0, NOW(), NOW(), 0),
(2, 1, 'RAG',              'tech',    '检索增强',  'retrieval,augmented', 8, 'manual', 0, NOW(), NOW(), 0),
(3, 1, 'AI 引擎',          'tech',    '自研推理',  'engine,inference',     8, 'manual', 0, NOW(), NOW(), 0),
(4, 1, '17 微服务',        'arch',    '系统架构',  'microservices',        7, 'manual', 0, NOW(), NOW(), 0),
(5, 1, 'Vue 3',            'tech',    '前端框架',  'frontend,vue',         6, 'manual', 0, NOW(), NOW(), 0);

-- ===== 21. 知识图谱关系 (4) =====
INSERT INTO `kg_relation` (`id`, `user_id`, `from_entity`, `to_entity`, `relation_type`, `description`, `weight`, `source`, `ref_count`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, 1, 2, 'uses',     'Platform 使用 RAG',         0.95, 'manual', 0, NOW(), NOW(), 0),
(2, 1, 1, 3, 'contains', 'Platform 包含 AI 引擎',     0.95, 'manual', 0, NOW(), NOW(), 0),
(3, 1, 1, 4, 'contains', 'Platform 包含 17 微服务',   0.95, 'manual', 0, NOW(), NOW(), 0),
(4, 1, 1, 5, 'uses',     'Platform 前端用 Vue 3',     0.90, 'manual', 0, NOW(), NOW(), 0);

-- ===== 22. 集群节点 (1) =====
INSERT INTO `cluster_node` (`id`, `node_id`, `name`, `address`, `region`, `zone`, `capabilities`, `total_cores`, `total_memory_mb`, `total_gpus`, `cpu_usage`, `memory_usage`, `gpu_usage`, `active_tasks`, `status`, `is_leader`, `last_heartbeat`, `started_at`, `created_at`, `updated_at`) VALUES
(1, 'node-1', 'node-1', '127.0.0.1', 'local', '{"cpu":true,"gpu":false}', 8, 8192, 0, 0.1, 0.2, 0.0, 0, 1, 1, NOW(), NOW(), NOW());

-- ===== 23. 通知 (2) =====
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `is_read`, `created_at`, `updated_at`) VALUES
(1, 1, 'system', '欢迎使用 MiniMax', '平台已就绪, 4 个测试账号可用', 0, NOW(), NOW()),
(2, 1, 'system', '系统升级',         'V3.5.8 部署完成, 17 服务在线', 0, NOW(), NOW());

-- ===== 24. AI 聊天会话 (3) =====
INSERT INTO `ai_chat_session` (`id`, `session_id`, `user_id`, `username`, `title`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'sess-ai-001', 1, 'adminLiugl', 'AI 平台使用咨询', NOW(), NOW(), 0),
(2, 'sess-ai-002', 2, 'admin_user', '数据分析请求',   NOW(), NOW(), 0),
(3, 'sess-ai-003', 1, 'adminLiugl', '知识库检索测试', NOW(), NOW(), 0);

-- ===== 25. AI 聊天消息 (5) =====
INSERT INTO `ai_chat_message` (`id`, `session_id`, `role`, `content`, `tool_code`, `tool_input`, `tool_output`, `created_at`) VALUES
(1, 1, 'user',      '什么是 RAG?',        NULL, NULL, NULL, NOW()),
(2, 1, 'assistant', 'RAG 是检索增强生成...', NULL, NULL, NULL, NOW()),
(3, 2, 'user',      '查询上月销售数据',   'sql_query', '{"sql":"SELECT * FROM sales"}', '[]', NOW()),
(4, 3, 'user',      'MiniMax 平台架构',   NULL, NULL, NULL, NOW()),
(5, 3, 'assistant', '17 个微服务, 包括...', NULL, NULL, NULL, NOW());

-- ===== 26. Chat 会话 (2) =====
INSERT INTO `chat_session` (`id`, `user_id`, `title`, `model`, `system_prompt`, `temperature`, `status`, `message_count`, `last_message_at`, `tenant_id`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, '关于 MiniMax', 'gpt-4o',  '你是 AI 助手', 0.7, 1, 3, NOW(), 1, NOW(), NOW(), 0),
(2, 2, '数据查询',     'claude-3.5', NULL,        0.5, 1, 2, NOW(), 1, NOW(), NOW(), 0);

-- ===== 27. Chat 消息 (3) =====
INSERT INTO `chat_message` (`id`, `session_id`, `user_id`, `role`, `content`, `tokens`, `finish_reason`, `error_message`, `created_at`, `deleted`) VALUES
(1, 1, 1, 'user',      'MiniMax 有哪些模块?', 8,  'stop', NULL, NOW(), 0),
(2, 1, 1, 'assistant', '17 个微服务...',     128, 'stop', NULL, NOW(), 0),
(3, 2, 2, 'user',      '上月销售',          12, 'stop', NULL, NOW(), 0);

-- ===== 28. 知识库文档 (5) =====
INSERT INTO `kb_document` (`id`, `doc_id`, `kb_id`, `filename`, `mime_type`, `size_bytes`, `sha256`, `file_path`, `source`, `source_url`, `status`, `chunk_count`, `embedding_count`, `error`, `tags`, `owner_id`, `is_public`, `created_at`, `updated_at`) VALUES
(1, 'doc-001', 'kb_product', 'platform-intro.md',  'text/markdown', 4096, 'sha256-aaa', '/data/kb/doc-001.md', 'upload', NULL, 1, 5, 5, NULL, 'guide,intro', 1, 1, NOW(), NOW()),
(2, 'doc-002', 'kb_product', 'api-docs.md',        'text/markdown', 8192, 'sha256-bbb', '/data/kb/doc-002.md', 'upload', NULL, 1, 8, 8, NULL, 'api',        1, 1, NOW(), NOW()),
(3, 'doc-003', 'kb_product', 'deployment.md',      'text/markdown', 6144, 'sha256-ccc', '/data/kb/doc-003.md', 'upload', NULL, 1, 6, 6, NULL, 'deploy',     1, 1, NOW(), NOW()),
(4, 'doc-004', 'kb_tech',    'code-standards.md',  'text/markdown', 3072, 'sha256-ddd', '/data/kb/doc-004.md', 'upload', NULL, 1, 3, 3, NULL, 'standard',   1, 1, NOW(), NOW()),
(5, 'doc-005', 'kb_tech',    'git-workflow.md',    'text/markdown', 2048, 'sha256-eee', '/data/kb/doc-005.md', 'upload', NULL, 1, 2, 2, NULL, 'git',        1, 1, NOW(), NOW());

-- ===== 29. 知识库分块 (8) =====
INSERT INTO `kb_chunk` (`id`, `chunk_id`, `doc_id`, `kb_id`, `seq`, `content`, `char_count`, `token_count`, `embedding`, `embedding_model`, `keywords`, `summary`, `location`, `created_at`) VALUES
(1, 'chunk-001-1', 'doc-001', 'kb_product', 1, 'MiniMax 平台是 AI 大模型平台...',     512, 128, NULL, 'text-embedding-3-small', 'MiniMax,AI,平台',   '平台介绍',   'p1', NOW()),
(2, 'chunk-001-2', 'doc-001', 'kb_product', 2, '包含 17 个微服务: gateway, auth...', 512, 128, NULL, 'text-embedding-3-small', '微服务,架构',     '架构概览',   'p2', NOW()),
(3, 'chunk-002-1', 'doc-002', 'kb_product', 1, 'AI 引擎提供 5 类工具...',            256, 64,  NULL, 'text-embedding-3-small', 'AI,工具,plugin',  '工具介绍',   'p1', NOW()),
(4, 'chunk-002-2', 'doc-002', 'kb_product', 2, 'POST /api/v1/ai/intent/predict...',  256, 64,  NULL, 'text-embedding-3-small', 'API,意图',       'API 示例',   'p2', NOW()),
(5, 'chunk-003-1', 'doc-003', 'kb_product', 1, 'Docker Compose 一键部署...',          256, 64,  NULL, 'text-embedding-3-small', 'docker,部署',    '部署方法',   'p1', NOW()),
(6, 'chunk-004-1', 'doc-004', 'kb_tech',    1, '编码规范: 命名, 注释, 异常处理...', 512, 128, NULL, 'text-embedding-3-small', '规范,代码',      '规范总览',   'p1', NOW()),
(7, 'chunk-005-1', 'doc-005', 'kb_tech',    1, 'Git Flow: main, develop, feature...', 256, 64,  NULL, 'text-embedding-3-small', 'git,工作流',     'Git 流程',   'p1', NOW()),
(8, 'chunk-005-2', 'doc-005', 'kb_tech',    2, 'PR 评审要求: 2 个 reviewer...',       256, 64,  NULL, 'text-embedding-3-small', 'PR,review',      'PR 规范',    'p2', NOW());

-- ===== 30. 模型对战日志 (3) =====
INSERT INTO `model_battle_log` (`id`, `battle_id`, `user_id`, `model_id`, `model_code`, `prompt`, `response`, `prompt_tokens`, `completion_tokens`, `latency_ms`, `status`, `error_msg`, `score`, `judge_model`, `judge_reason`, `created_at`) VALUES
(1, 'battle-001', 1, 1, 'gpt-4o',         '解释 REST 和 GraphQL 区别', 'REST 是资源为中心, GraphQL 是查询为中心...', 45, 320, 4520, 'success', NULL, 5, 'gpt-4o', '对比清晰', NOW()),
(2, 'battle-002', 1, 4, 'deepseek-coder', '写 Python 快速排序',       'def qs(arr): ...',                            28, 180, 2340, 'success', NULL, 5, 'gpt-4o', '代码完整', NOW()),
(3, 'battle-003', 1, 2, 'claude-3.5',     '总结《百年孤独》',         '布恩迪亚家族七代兴衰...',                    56, 480, 5680, 'success', NULL, 4, 'gpt-4o', '文学性强', NOW());

-- ===== 31. 告警事件 (3) =====
INSERT INTO `alert_event` (`id`, `rule_id`, `rule_name`, `severity`, `metric_name`, `metric_value`, `threshold`, `message`, `status`, `fired_at`, `resolved_at`, `acked_at`, `acked_by`, `duration`) VALUES
(1, 1, 'CPU 高',   'warning',  'cpu_usage', 85.5,  80, 'CPU 超过 80%',     'resolved', NOW(), NOW(), NOW(), 1, 300),
(2, 2, '内存高',   'warning',  'mem_usage', 90.2,  85, '内存超过 85%',     'firing',   NOW(), NULL, NULL, NULL, NULL),
(3, 3, '错误率高', 'critical', 'err_rate',  8.7,   5,  '5xx 错误率 8.7%',  'acked',    NOW(), NULL, NOW(), 1, NULL);

-- ===== 32. 文档分块 (5) =====
INSERT INTO `document_chunk` (`id`, `doc_id`, `kb_id`, `owner_id`, `chunk_index`, `content`, `embedding`, `dim`, `char_count`, `start_pos`, `end_pos`, `access_count`, `last_access_at`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, 1, 1, 0, 'MiniMax 平台介绍...',    NULL, 0, 512, 0,    512,  0, NULL, NOW(), NOW(), 0),
(2, 1, 1, 1, 1, '17 个微服务架构...',     NULL, 0, 512, 512,  1024, 0, NULL, NOW(), NOW(), 0),
(3, 2, 1, 1, 0, 'API 端点列表...',        NULL, 0, 256, 0,    256,  0, NULL, NOW(), NOW(), 0),
(4, 3, 1, 1, 0, 'Docker Compose 部署...', NULL, 0, 256, 0,    256,  0, NULL, NOW(), NOW(), 0),
(5, 4, 2, 1, 0, '编码规范...',             NULL, 0, 512, 0,    512,  0, NULL, NOW(), NOW(), 0);

-- ===== 33. 智能体任务 (3) =====
INSERT INTO `agent_task` (`id`, `task_id`, `user_id`, `goal`, `status`, `rounds`, `result`, `llm_calls`, `tool_calls`, `total_tokens`, `error_msg`, `latency_ms`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'task-001', 1, '查询用户增长趋势', 'success', 3, '用户增长 12.5%',  4, 2, 1850, NULL, 12500, NOW(), NOW(), 0),
(2, 'task-002', 2, '分析月度销售',     'success', 5, '销售 100万, +5%', 6, 3, 2400, NULL, 18200, NOW(), NOW(), 0),
(3, 'task-003', 1, '生成 RAG 报告',     'running', 2, NULL,             2, 1, 800,  NULL, NULL,   NOW(), NOW(), 0);

-- ===== 34. 流水线运行 (2) =====
INSERT INTO `pipeline_run` (`id`, `workflow_id`, `workflow_name`, `status`, `trigger_by`, `trigger_type`, `definition_snapshot`, `start_time`, `end_time`, `duration_ms`, `error_message`, `result_summary`, `create_time`) VALUES
(1, 1, 'ETL 流程',  'success', 1, 'manual', '{"nodes":[]}', NOW(), NOW(), 12000, NULL, '处理 10000 行, 错误 0',  NOW()),
(2, 2, 'AI 训练',   'failed',  1, 'cron',   '{"nodes":[]}', NOW(), NOW(), 35000, 'GPU 内存不足', NULL, NOW());

-- ===== 35. 训练任务 (2) =====
INSERT INTO `training_task` (`id`, `user_id`, `model_name`, `corpus_path`, `n_layer`, `n_head`, `n_embd`, `block_size`, `max_iters`, `batch_size`, `learning_rate`, `status`, `progress`, `current_loss`, `current_iter`, `error_message`, `created_at`, `updated_at`, `completed_at`) VALUES
(1, 1, 'minimax-mini',  '/data/corpus/wiki.txt', 6,  6,  384, 256, 5000,  32, 0.0003, 'training',  60.0, 2.18,  3000,  NULL, NOW(), NOW(), NULL),
(2, 2, 'minimax-coder', '/data/corpus/code.txt', 12, 12, 768, 512, 10000, 16, 0.0001, 'completed', 100.0, 1.05, 10000, NULL, NOW(), NOW(), NOW());

-- ===== 36. 审计日志 (3) =====
INSERT INTO `audit_log` (`id`, `trace_id`, `user_id`, `username`, `user_ip`, `user_agent`, `action`, `resource_type`, `resource_id`, `method`, `path`, `request_body`, `response_status`, `result`, `error_msg`, `duration_ms`, `created_at`) VALUES
(1, 'trace-001', 1, 'adminLiugl', '127.0.0.1', 'Mozilla/5.0', 'login',    'auth',     NULL, 'POST', '/api/v1/auth/login',  '{"username":"adminLiugl"}', 200, 'success', NULL, 234, NOW()),
(2, 'trace-002', 1, 'adminLiugl', '127.0.0.1', 'Mozilla/5.0', 'create',   'ai_tool',  '5',  'POST', '/api/v1/admin/ai/tools', '{"code":"web_search"}', 200, 'success', NULL, 89,  NOW()),
(3, 'trace-003', 2, 'admin_user', '127.0.0.1', 'curl/7.81',    'reset_pwd', 'sys_user', '4',  'POST', '/api/v1/admin/users/4/reset-password', '{"password":"demo1234"}', 200, 'success', NULL, 156, NOW());

-- ===== 37. 认证登录日志 (3) =====
INSERT INTO `auth_login_log` (`id`, `user_id`, `username`, `ip`, `user_agent`, `status`, `message`, `created_at`) VALUES
(1, 1, 'adminLiugl', '127.0.0.1', 'Mozilla/5.0', 1, '登录成功', NOW()),
(2, 2, 'admin_user', '127.0.0.1', 'Mozilla/5.0', 1, '登录成功', NOW()),
(3, 3, 'test_user',  '127.0.0.1', 'curl/7.81',   0, '密码错误', NOW());

-- ===== 38. 协作房间 (1) =====
INSERT INTO `collab_room` (`id`, `room_id`, `name`, `type`, `owner_id`, `owner_name`, `description`, `is_public`, `max_participants`, `status`, `current_participants`, `created_at`, `last_activity_at`, `closed_at`) VALUES
(1, 'room-001', '产品评审会议', 'meeting', 1, 'adminLiugl', 'V3.5.8 评审', 0, 10, 1, 2, NOW(), NOW(), NULL);

-- ===== 39. 协作参与者 (2) =====
INSERT INTO `collab_participant` (`id`, `room_id`, `user_id`, `username`, `nickname`, `avatar`, `role`, `cursor_x`, `cursor_y`, `selection_id`, `status`, `joined_at`, `left_at`, `last_heartbeat`) VALUES
(1, 1, 1, 'adminLiugl', 'Liugl', NULL, 'owner',  0,   0,   NULL, 'online', NOW(), NULL, NOW()),
(2, 1, 2, 'admin_user', 'Admin', NULL, 'editor', 100, 200, NULL, 'online', NOW(), NULL, NOW());

-- ===== 40. 协作消息 (2) =====
INSERT INTO `collab_message` (`id`, `room_id`, `user_id`, `username`, `nickname`, `type`, `content`, `metadata`, `client_msg_id`, `broadcast`, `created_at`) VALUES
(1, 1, 1, 'adminLiugl', 'Liugl', 'text', '大家看下 V3.5.8 的设计', NULL, 'msg-001', 1, NOW()),
(2, 1, 2, 'admin_user', 'Admin', 'text', '架构清晰, 建议加监控',  NULL, 'msg-002', 1, NOW());
