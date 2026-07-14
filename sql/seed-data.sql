-- =========================================
-- MiniMax Platform V3.5.8 种子数据 (统一版)
-- =========================================
-- 作用: 给 complete.sql 77 张表灌入核心业务数据
-- 兼容: MySQL / H2 (MODE=MySQL) / MariaDB
-- 加载顺序: 在 complete.sql 之后执行
-- 包含: 用户/角色/租户/模型/工具/Prompt/插件/知识库/敏感词/意图词典
-- =========================================

-- MySQL 字符集 (H2 MODE=MySQL 自动忽略, MySQL 必须)
-- 外键检查关闭 (H2 忽略, MySQL 用以允许跨表插入)
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================
-- 1. 租户 (2 个)
-- =========================================
INSERT INTO `tenant` (`id`, `code`, `name`, `plan`, `status`, `max_users`, `max_models`, `qps_limit`, `monthly_quota`, `used_quota`, `contact_email`, `contact_phone`, `expire_at`, `remark`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'default', '默认租户', 'enterprise', 1, 100, 50, 1000, 10000000, 0, 'admin@minimax.io', '13800138000', '2099-12-31 23:59:59', '平台默认租户', NOW(), NOW(), 0),
(2, 'demo',    '演示租户', 'pro',        1, 50,  20, 500,  5000000,  0, 'demo@minimax.io',   '13800138001', '2099-12-31 23:59:59', '演示环境',     NOW(), NOW(), 0);

-- =========================================
-- 2. 角色 (3 个)
-- =========================================
INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `sort`, `enabled`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'super_admin', '超级管理员', '拥有所有权限, 平台所有者',      1, 1, NOW(), NOW(), 0),
(2, 'admin',       '管理员',    '管理用户、模型、工具、知识库', 2, 1, NOW(), NOW(), 0),
(3, 'user',        '普通用户',  '使用对话、知识库、Agent 等',    3, 1, NOW(), NOW(), 0);

-- =========================================
-- 3. 用户 (4 个)
-- BCrypt 密码哈希 (10 rounds):
--   Liugl@2026  -> $2a$10$fyhHrqgwteAj5eqesr7yw.OPv3N4SvVj17qkqZN8Kq1/PO5vn3P6W
--   admin123    -> $2a$10$VqCU4L5qLu.6R49QzTI/Ne.fbA31HuuDTHkIFN7IeWWjLJkRZFTBi
--   user123     -> $2a$10$AgpCEcOuWI.ciFYMWDC5Nekyy1TY7ngNGF4myEip3j/MCGtFh5uoq
--   demo1234    -> $2a$10$/CTAFwg2cuOINRXzlKV0E.dd02V.nEG1P2cp82nnYrolVvH8ii5JS
-- =========================================
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `avatar`, `gender`, `status`, `last_login_ip`, `last_login_at`, `tenant_id`, `remark`, `created_by`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'adminLiugl', '$2a$10$fyhHrqgwteAj5eqesr7yw.OPv3N4SvVj17qkqZN8Kq1/PO5vn3P6W', '超级管理员', 'admin@minimax.io',  '13800138000', '', 1, 1, '127.0.0.1', NOW(), 1, '系统内置超级管理员', NULL, NOW(), NOW(), 0),
(2, 'admin_user', '$2a$10$VqCU4L5qLu.6R49QzTI/Ne.fbA31HuuDTHkIFN7IeWWjLJkRZFTBi', '管理员',     'admin2@minimax.io', '13800138001', '', 1, 1, NULL,         NULL,  1, '演示管理员账号',     1,    NOW(), NOW(), 0),
(3, 'test_user',  '$2a$10$AgpCEcOuWI.ciFYMWDC5Nekyy1TY7ngNGF4myEip3j/MCGtFh5uoq', '测试用户',   'user@minimax.io',   '13800138002', '', 1, 1, NULL,         NULL,  1, '演示普通用户',       1,    NOW(), NOW(), 0),
(4, 'demo_user',  '$2a$10$/CTAFwg2cuOINRXzlKV0E.dd02V.nEG1P2cp82nnYrolVvH8ii5JS', '演示账户',   'demo@minimax.io',   '13800138003', '', 1, 1, NULL,         NULL,  2, '演示租户账号',       1,    NOW(), NOW(), 0);

-- =========================================
-- 4. 用户-角色关联
-- =========================================
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1),  -- adminLiugl -> super_admin
(2, 2),  -- admin_user -> admin
(3, 3),  -- test_user  -> user
(4, 3);  -- demo_user  -> user

-- =========================================
-- 5. 模型提供商 (5 个)
-- =========================================
INSERT INTO `model_provider` (`id`, `code`, `name`, `base_url`, `api_key`, `protocol`, `enabled`, `sort`, `description`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'minimax',  'MiniMax 自研',     'http://localhost:8094',  '',                                  'openai', 1, 1, '自研 AI 模型, 离线运行',           NOW(), NOW(), 0),
(2, 'mock',     'Mock 模拟',         'http://localhost:8094',  '',                                  'mock',   1, 0, '开发测试用, 返回固定响应',         NOW(), NOW(), 0),
(3, 'openai',   'OpenAI',            'https://api.openai.com/v1',  '${OPENAI_API_KEY:sk-xxx}',     'openai', 0, 2, 'GPT-4 / GPT-3.5, 需配置 API Key', NOW(), NOW(), 0),
(4, 'deepseek', 'DeepSeek',          'https://api.deepseek.com/v1','${DEEPSEEK_API_KEY:sk-xxx}',   'openai', 0, 3, 'DeepSeek Chat, 高性价比',         NOW(), NOW(), 0),
(5, 'moonshot', '月之暗面 Moonshot', 'https://api.moonshot.cn/v1', '${MOONSHOT_API_KEY:sk-xxx}',    'openai', 0, 4, 'Kimi 长上下文 128K',               NOW(), NOW(), 0);

-- =========================================
-- 6. AI 工具 (5 个)
-- 字段: code/name/category/description/icon/enabled/builtin/
--       input_schema/output_schema/default_config/impl_type/impl_value/
--       rate_limit/timeout_seconds/role_required/tags/author/version
-- =========================================
INSERT INTO `ai_tool` (`id`, `code`, `name`, `category`, `description`, `icon`, `enabled`, `builtin`, `input_schema`, `output_schema`, `default_config`, `impl_type`, `impl_value`, `rate_limit`, `timeout_seconds`, `role_required`, `tags`, `author`, `version`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'web_search',   'Web 搜索',   'search',     '调用搜索引擎获取实时信息',        '🌐', 1, 1, '{"query":"string"}',                            '{"results":[]}',  '{}',         'http',  'http://localhost:8094/api/v1/ai/tools/web_search',  60,  30,  'user',  '["search","web"]',    'MiniMax', '1.0.0', NOW(), NOW(), 0),
(2, 'code_runner',  '代码执行',   'compute',    '在沙箱执行 Python/JavaScript',     '▶️', 1, 1, '{"code":"string","lang":"string"}}',  '{"output":"string"}',                     '{}',         'http',  'http://localhost:8094/api/v1/ai/tools/code_runner', 60,  30,  'user',  '["code","sandbox"]',  'MiniMax', '1.0.0', NOW(), NOW(), 0),
(3, 'image_gen',    '图像生成',   'multimodal', '调用模型生成图片',                '🎨', 1, 1, '{"prompt":"string","size":"string"}}','{"image_url":"string"}', '{}',         'http',  'http://localhost:8094/api/v1/ai/tools/image_gen',   10,  60,  'user',  '["image","ai"]',      'MiniMax', '1.0.0', NOW(), NOW(), 0),
(4, 'sql_query',    'SQL 查询',   'data',       '执行只读 SQL 查询',              '🗃️', 1, 1, '{"sql":"string"}',                            '{"rows":[]}', '{}',         'http',  'http://localhost:8094/api/v1/ai/tools/sql_query',   30,  30,  'admin', '["sql","data"]',      'MiniMax', '1.0.0', NOW(), NOW(), 0),
(5, 'file_reader',  '文件读取',   'data',       '读取本地/远程文件',              '📄', 1, 1, '{"path":"string"}',                                                       '{"content":"string"}',                   '{}',         'http',  'http://localhost:8094/api/v1/ai/tools/file_reader', 30,  30,  'user',  '["file","io"]',       'MiniMax', '1.0.0', NOW(), NOW(), 0);

-- =========================================
-- 7. Function 工具 (4 个)
-- 字段: name/display_name/description/category/scope/owner_id/parameters/
--       enabled/endpoint/http_method/tags
-- =========================================
INSERT INTO `function_tool` (`id`, `name`, `display_name`, `description`, `category`, `scope`, `owner_id`, `parameters`, `enabled`, `endpoint`, `http_method`, `tags`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'get_weather', '获取天气', '查询指定城市的天气信息',  'data',     'public',  0, '{"city":"string"}', 1, 'http://api.weather.example.com/v1/weather', 'GET',  '["weather","api"]', NOW(), NOW(), 0),
(2, 'send_email',  '发送邮件', '给指定邮箱发送文本邮件',  'business', 'private', 0, '{"to":"string","subject":"string","body":"string"}', 1, 'http://localhost:8081/api/v1/email/send', 'POST', '["email","notify"]', NOW(), NOW(), 0),
(3, 'calculator',  '计算器',   '执行数学表达式',          'compute',  'public',  0, '{"expr":"string"}', 1, 'http://localhost:8081/api/v1/tools/calc', 'POST', '["math","compute"]', NOW(), NOW(), 0),
(4, 'translate',   '翻译',     '多语言翻译',              'ai',       'public',  0, '{"text":"string","target_lang":"string"}', 1, 'http://localhost:8094/api/v1/ai/translate', 'POST', '["translate","ai"]', NOW(), NOW(), 0);

-- =========================================
-- 8. Prompt 模板 (6 个)
-- 字段: name/description/category/content/variables/
--       creator_id/creator_name/is_public/use_count
-- =========================================
INSERT INTO `prompt_template` (`id`, `name`, `description`, `category`, `content`, `variables`, `creator_id`, `creator_name`, `is_public`, `use_count`, `created_at`, `updated_at`, `deleted`) VALUES
(1, '通用助手',     '默认对话助手',        'general',  '你是一个有帮助的 AI 助手, 请用简洁、准确的语言回答用户问题。',                                              '[]',                              1, 'adminLiugl', 1, 0, NOW(), NOW(), 0),
(2, '代码审查',     '审查代码质量',        'code',     '请审查以下代码, 指出潜在问题、性能瓶颈、安全漏洞:\n\n```\n{code}\n```',                                '["code"]',                        1, 'adminLiugl', 1, 0, NOW(), NOW(), 0),
(3, 'SQL 生成',     '根据需求生成 SQL',    'data',     '根据以下需求生成 SQL:\n需求: {requirement}\n表结构: {schema}',                                            '["requirement","schema"]',        1, 'adminLiugl', 1, 0, NOW(), NOW(), 0),
(4, '翻译助手',     '多语言翻译',          'translate','请将以下文本翻译成 {target_lang}:\n\n{text}',                                                            '["text","target_lang"]',          1, 'adminLiugl', 1, 0, NOW(), NOW(), 0),
(5, '总结助手',     '总结长文本',          'general',  '请用 {max_words} 字总结以下内容:\n\n{text}',                                                            '["text","max_words"]',            1, 'adminLiugl', 1, 0, NOW(), NOW(), 0),
(6, 'RAG 问答',     '基于知识库回答',      'rag',      '基于以下参考文档回答问题, 如不知道请说"我不知道":\n\n参考:\n{context}\n\n问题: {question}',                  '["context","question"]',          1, 'adminLiugl', 1, 0, NOW(), NOW(), 0);

-- =========================================
-- 9. 插件市场 (5 个)
-- 字段: name/display_name/description/version/author/category/scope/enabled/
--       entry/icon/config/downloads/rating/plugin_type/owner_id
-- =========================================
INSERT INTO `plugin` (`id`, `name`, `display_name`, `description`, `version`, `author`, `category`, `scope`, `enabled`, `entry`, `icon`, `config`, `downloads`, `rating`, `plugin_type`, `owner_id`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'wechat-bot',     '微信机器人',     '集成微信公众号/小程序',           '1.0.0', 'MiniMax 官方',  'integration', 'public',  1, 'com.minimax.plugin.wechat.Bot',          '💬', '{}', 100, 4.5, 'integration', 0, NOW(), NOW(), 0),
(2, 'dingtalk',       '钉钉通知',       '消息推送到钉钉群',                '1.0.0', 'MiniMax 官方',  'notification','public',  1, 'com.minimax.plugin.dingtalk.Notifier',    '🔔', '{}', 80,  4.2, 'notification',0, NOW(), NOW(), 0),
(3, 'feishu',         '飞书',           '飞书文档/消息集成',               '1.0.0', '社区贡献',      'integration', 'public',  1, 'com.minimax.plugin.feishu.Client',        '📋', '{}', 60,  4.0, 'integration', 0, NOW(), NOW(), 0),
(4, 'data-analysis',  '数据分析',       '内置 NL2SQL + 图表生成',          '1.0.0', 'MiniMax 官方',  'analytics',   'public',  1, 'com.minimax.plugin.analysis.Engine',     '📊', '{}', 200, 4.8, 'analytics',   0, NOW(), NOW(), 0),
(5, 'ocr',            'OCR 识别',       '图片文字提取',                    '1.0.0', '社区贡献',      'multimodal',  'public',  1, 'com.minimax.plugin.ocr.Recognizer',      '🔍', '{}', 150, 4.3, 'multimodal',  0, NOW(), NOW(), 0);

-- =========================================
-- 10. 知识库 (2 个)
-- 字段: name/description/owner_id/tenant_id/visibility/chunk_count/doc_count/
--       tags
-- =========================================
INSERT INTO `knowledge_base` (`id`, `name`, `description`, `owner_id`, `tenant_id`, `visibility`, `chunk_count`, `doc_count`, `tags`, `created_at`, `updated_at`, `deleted`) VALUES
(1, '产品文档',     'MiniMax 平台使用手册',         1, 1, 'public',  0, 0, '["manual","guide"]',         NOW(), NOW(), 0),
(2, 'API 文档',     'MiniMax API 参考手册',         1, 1, 'public',  0, 0, '["api","reference"]',        NOW(), NOW(), 0);

-- =========================================
-- 11. 敏感词 (10 个)
-- 字段: word/category/level/action/enabled
-- =========================================
INSERT INTO `sensitive_word` (`id`, `word`, `category`, `level`, `action`, `enabled`, `created_at`) VALUES
(1,  '暴力',    'violence',  '3', 'block',   1, NOW()),
(2,  '色情',    'porn',      '4', 'block',   1, NOW()),
(3,  '毒品',    'drug',      '4', 'block',   1, NOW()),
(4,  '赌博',    'gambling',  '3', 'replace', 1, NOW()),
(5,  '诈骗',    'fraud',     '3', 'block',   1, NOW()),
(6,  '反动',    'politics',  '4', 'block',   1, NOW()),
(7,  '辱骂',    'abuse',     '2', 'replace', 1, NOW()),
(8,  '广告',    'spam',      '1', 'review',  1, NOW()),
(9,  '歧视',    'discrim',   '3', 'replace', 1, NOW()),
(10, '隐私',    'privacy',   '3', 'review',  1, NOW());

-- =========================================
-- 12. AI 意图关键词 (9 意图 × 5 词 = 50+)
-- 字段: intent/keyword/weight/is_regex/enabled/language/remark
-- =========================================
INSERT INTO `ai_intent_keyword` (`intent`, `keyword`, `weight`, `is_regex`, `enabled`, `language`, `remark`, `created_at`, `updated_at`) VALUES
-- query
('query', '查询', 10, 0, 1, 'zh', '查询意图', NOW(), NOW()),
('query', '查', 8, 0, 1, 'zh', '单字', NOW(), NOW()),
('query', '看看', 6, 0, 1, 'zh', '', NOW(), NOW()),
('query', '显示', 5, 0, 1, 'zh', '', NOW(), NOW()),
('query', '多少', 7, 0, 1, 'zh', '', NOW(), NOW()),
('query', '几个', 5, 0, 1, 'zh', '', NOW(), NOW()),
('query', '哪些', 5, 0, 1, 'zh', '', NOW(), NOW()),
('query', '有没有', 6, 0, 1, 'zh', '', NOW(), NOW()),
-- order
('order', '下单', 10, 0, 1, 'zh', '', NOW(), NOW()),
('order', '订购', 9, 0, 1, 'zh', '', NOW(), NOW()),
('order', '买', 8, 0, 1, 'zh', '', NOW(), NOW()),
('order', '购买', 9, 0, 1, 'zh', '', NOW(), NOW()),
('order', '要', 5, 0, 1, 'zh', '', NOW(), NOW()),
-- complaint
('complaint', '投诉', 10, 0, 1, 'zh', '', NOW(), NOW()),
('complaint', '差评', 9, 0, 1, 'zh', '', NOW(), NOW()),
('complaint', '退款', 8, 0, 1, 'zh', '', NOW(), NOW()),
('complaint', '退货', 8, 0, 1, 'zh', '', NOW(), NOW()),
-- consult
('consult', '咨询', 10, 0, 1, 'zh', '', NOW(), NOW()),
('consult', '请问', 8, 0, 1, 'zh', '', NOW(), NOW()),
('consult', '怎么', 5, 0, 1, 'zh', '', NOW(), NOW()),
('consult', '如何', 5, 0, 1, 'zh', '', NOW(), NOW()),
-- cancel
('cancel', '取消', 10, 0, 1, 'zh', '', NOW(), NOW()),
('cancel', '撤销', 9, 0, 1, 'zh', '', NOW(), NOW()),
('cancel', '作废', 8, 0, 1, 'zh', '', NOW(), NOW()),
-- feedback
('feedback', '反馈', 10, 0, 1, 'zh', '', NOW(), NOW()),
('feedback', '建议', 8, 0, 1, 'zh', '', NOW(), NOW()),
('feedback', '意见', 7, 0, 1, 'zh', '', NOW(), NOW()),
('feedback', '希望', 5, 0, 1, 'zh', '', NOW(), NOW()),
-- pay
('pay', '付款', 10, 0, 1, 'zh', '', NOW(), NOW()),
('pay', '支付', 10, 0, 1, 'zh', '', NOW(), NOW()),
('pay', '转账', 8, 0, 1, 'zh', '', NOW(), NOW()),
('pay', '结账', 9, 0, 1, 'zh', '', NOW(), NOW()),
('pay', '充值', 7, 0, 1, 'zh', '', NOW(), NOW()),
-- login
('login', '登录', 10, 0, 1, 'zh', '', NOW(), NOW()),
('login', '登入', 9, 0, 1, 'zh', '', NOW(), NOW()),
-- register
('register', '注册', 10, 0, 1, 'zh', '', NOW(), NOW()),
('register', '开账号', 9, 0, 1, 'zh', '', NOW(), NOW()),
('register', '创建账号', 9, 0, 1, 'zh', '', NOW(), NOW()),
-- 英文
('query', 'select', 5, 0, 1, 'en', '', NOW(), NOW()),
('query', 'find', 5, 0, 1, 'en', '', NOW(), NOW()),
('order', 'order', 8, 0, 1, 'en', '', NOW(), NOW()),
('order', 'buy', 7, 0, 1, 'en', '', NOW(), NOW()),
('complaint', 'complain', 8, 0, 1, 'en', '', NOW(), NOW()),
('complaint', 'refund', 7, 0, 1, 'en', '', NOW(), NOW()),
('pay', 'pay', 8, 0, 1, 'en', '', NOW(), NOW()),
('login', 'login', 8, 0, 1, 'en', '', NOW(), NOW()),
('register', 'register', 8, 0, 1, 'en', '', NOW(), NOW()),
('cancel', 'cancel', 8, 0, 1, 'en', '', NOW(), NOW()),
('feedback', 'feedback', 8, 0, 1, 'en', '', NOW(), NOW()),
('consult', 'help', 5, 0, 1, 'en', '', NOW(), NOW());

-- =========================================
-- 13. 用户 API Key (2 个)
-- 字段: user_id/name/key_hash/key_prefix/scopes/enabled/expires_at/
--       last_used_at/use_count
-- =========================================
INSERT INTO `user_api_key` (`id`, `user_id`, `name`, `key_hash`, `key_prefix`, `scopes`, `enabled`, `expires_at`, `last_used_at`, `use_count`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, '默认 Key', 'mm_demo_key_a1000000000000000000000000000000000000000000000000000000000000001', 'mm_demo_key_a', '["all"]',   1, NULL, NULL, 0, NOW(), NOW(), 0),
(2, 3, '测试 Key', 'mm_demo_key_b3000000000000000000000000000000000000000000000000000000000000003', 'mm_demo_key_b', '["read"]',  1, NULL, NULL, 0, NOW(), NOW(), 0);

-- =========================================
-- 14. 通知 (3 条)
-- 字段: id/user_id/type/title/content/is_read/created_at/updated_at
-- =========================================
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `is_read`, `created_at`, `updated_at`) VALUES
(1, 1, 'system',   '欢迎使用 MiniMax 平台',  '感谢您选择 MiniMax 平台! 平台内置 17 个微服务, 支持多模态 AI、知识库、Agent 编排。', 0, NOW(), NOW()),
(2, 1, 'feature',  'V3.5.7 意图识别升级',   '意图识别升级到 4 模型加权投票 + 文本归一化 + 否定处理, 准确度提升 15pt。',          0, NOW(), NOW()),
(3, 1, 'security', '安全提醒',              '请定期更换 API Key, 避免在公开代码仓库泄露。',                                       0, NOW(), NOW());

-- =========================================
-- 15. 告警规则 (3 条)
-- 字段: id/name/description/metric_name/service/operator/severity/
--       cooldown_minutes/enabled/tags/notify_channel/created_at/updated_at
-- =========================================
INSERT INTO `alert_rule` (`id`, `name`, `description`, `metric_name`, `service`, `operator`, `severity`, `cooldown_minutes`, `enabled`, `tags`, `notify_channel`, `created_at`, `updated_at`) VALUES
(1, 'CPU 高负载',    'CPU 使用率超过 80%',     'cpu_usage',     'all', '>',  'warning',  5, 1, '["cpu","system"]',  '["email"]',          NOW(), NOW()),
(2, '内存不足',      '可用内存低于 1GB',       'mem_free_mb',   'all', '<',  'critical', 1, 1, '["memory","system"]','["email","sms"]',    NOW(), NOW()),
(3, 'API 错误率',    '5xx 错误率超过 5%',     'http_5xx_rate', 'all', '>',  'warning',  3, 1, '["api","error"]',   '["webhook"]',        NOW(), NOW());

-- =========================================
-- 16. 告警渠道 (3 个)
-- 字段: id/name/channel_type/config/priority/target/type/enabled/
--       description/created_by/created_at
-- =========================================
INSERT INTO `alert_channel` (`id`, `name`, `channel_type`, `config`, `priority`, `target`, `type`, `enabled`, `description`, `created_by`, `created_at`) VALUES
(1, '邮件通知',     'email',    '{"smtp_host":"smtp.example.com","from":"alert@minimax.io"}', 1, 'admin@minimax.io',     'email',    1, '默认邮件告警',  1, NOW()),
(2, 'Webhook',     'webhook',  '{"url":"https://hooks.example.com/alert"}',                2, 'https://hooks.example.com/alert', 'webhook', 1, '通用 Webhook',  1, NOW()),
(3, '钉钉机器人',   'dingtalk', '{"webhook":"https://oapi.dingtalk.com/robot/send?access_token=xxx"}', 3, 'https://oapi.dingtalk.com/robot/send', 'dingtalk', 0, '钉钉群机器人', 1, NOW());

-- =========================================
-- 17. Pipeline 工作流模板 (2 个)
-- 字段: name/description/definition/status/version/create_by/create_time/
--       update_by/update_time
-- =========================================
INSERT INTO `pipeline_workflow` (`id`, `name`, `description`, `definition`, `status`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, '智能问答流程', 'RAG + LLM 问答', '{"type":"rag","nodes":4}', 1, 1, 1, NOW(), 1, NOW(), 0),
(2, '数据分析流程', 'NL2SQL + 图表', '{"type":"nl2sql","nodes":5}', 1, 1, 1, NOW(), 1, NOW(), 0);

-- =========================================
-- 18. 文档 (2 篇)
-- 字段: title/kb_id/owner_id/source_type/source_uri/content/size_bytes/
--       status/chunk_count/checksum/tags
-- =========================================
INSERT INTO `document` (`id`, `title`, `kb_id`, `owner_id`, `source_type`, `source_uri`, `content`, `size_bytes`, `status`, `chunk_count`, `checksum`, `tags`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'MiniMax 平台介绍', 1, 1, 'manual', NULL, 'MiniMax Platform 是企业级 AI Agent 平台, 17 个 Spring Cloud 微服务。', 256, 'ready', 1, 'sha256-aa', '["intro"]', NOW(), NOW(), 0),
(2, 'API 快速开始',     2, 1, 'manual', NULL, 'API: 1.登录 /auth/login 2.对话 /sessions/{id}/messages 3.知识库 /rag/kb', 256, 'ready', 1, 'sha256-bb', '["api"]', NOW(), NOW(), 0);

-- =========================================
-- 19. 文档块 (3 个)
-- 字段: doc_id/kb_id/chunk_index/content/start_pos/end_pos/char_count/
--       embedding/owner_id/access_count
-- =========================================
INSERT INTO `document_chunk` (`id`, `doc_id`, `kb_id`, `chunk_index`, `content`, `start_pos`, `end_pos`, `char_count`, `embedding`, `owner_id`, `access_count`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, 1, 0, 'MiniMax Platform 是企业级 AI Agent 平台, 基于 17 个 Spring Cloud 微服务。',        0, 38, 38, '[]', 1, 0, NOW(), NOW(), 0),
(2, 1, 1, 1, '核心功能: 多 LLM 路由、知识库 RAG、Agent 编排、多模态、数据分析 NL2SQL、工作流 Pipeline。', 38, 100, 62, '[]', 1, 0, NOW(), NOW(), 0),
(3, 2, 2, 0, 'API 快速开始: 1. 登录获取 token (POST /api/v1/auth/login) 2. 调对话接口 3. 流式响应 4. 知识库。', 0, 70, 70, '[]', 1, 0, NOW(), NOW(), 0);

-- =========================================
-- 20. AI 生成日志 (5 条)
-- 字段: id/generation_id/user_id/username/user_ip/modality/model_name/
--       model_version/prompt/negative_prompt/parameters/output_url/
--       output_size/output_hash/watermarked/watermark_text/duration_ms/
--       status/error_msg/created_at
-- =========================================
INSERT INTO `ai_generation_log` (`id`, `user_id`, `username`, `user_ip`, `prompt`, `modality`, `model_name`, `model_version`, `parameters`, `negative_prompt`, `status`, `error_msg`, `duration_ms`, `output_size`, `output_url`, `output_hash`, `watermarked`, `watermark_text`, `generation_id`, `created_at`) VALUES
(1, 1, 'adminLiugl', '127.0.0.1', '你好, 请介绍你自己',  'text',  'minimax-chat',  'V3.5.7', '{"temperature":0.7}', NULL, 1, NULL, 350, 45,  NULL, NULL, 0, NULL, 'gen-001', NOW()),
(2, 1, 'adminLiugl', '127.0.0.1', '平台支持哪些模型?',  'text',  'minimax-chat',  'V3.5.7', '{"temperature":0.7}', NULL, 1, NULL, 280, 28,  NULL, NULL, 0, NULL, 'gen-002', NOW()),
(3, 1, 'adminLiugl', '127.0.0.1', '如何创建知识库?',    'text',  'minimax-chat',  'V3.5.7', '{"temperature":0.7}', NULL, 1, NULL, 420, 38,  NULL, NULL, 0, NULL, 'gen-003', NOW()),
(4, 1, 'adminLiugl', '127.0.0.1', '什么是 RAG?',        'text',  'minimax-chat',  'V3.5.7', '{"temperature":0.7}', NULL, 1, NULL, 380, 52,  NULL, NULL, 0, NULL, 'gen-004', NOW()),
(5, 1, 'adminLiugl', '127.0.0.1', 'Agent 能做什么?',   'text',  'minimax-chat',  'V3.5.7', '{"temperature":0.7}', NULL, 1, NULL, 320, 40,  NULL, NULL, 0, NULL, 'gen-005', NOW());

-- =========================================
-- 21. 集群节点 (1 个)
-- 字段: id/node_id/name/address/region/zone/capabilities/total_cores/
--       total_memory_mb/total_gpus/cpu_usage/memory_usage/gpu_usage/
--       active_tasks/is_leader/last_heartbeat/status/started_at/
--       created_at/updated_at
-- =========================================
INSERT INTO `cluster_node` (`id`, `node_id`, `name`, `address`, `region`, `zone`, `capabilities`, `total_cores`, `total_memory_mb`, `total_gpus`, `cpu_usage`, `memory_usage`, `gpu_usage`, `active_tasks`, `is_leader`, `last_heartbeat`, `status`, `started_at`, `created_at`, `updated_at`) VALUES
(1, 'node-local-01', '本地主节点', '127.0.0.1:8094', 'cn-shanghai', 'shanghai-a', '["chat","rag","agent","multimodal"]', 8, 16384, 0, 25.5, 40.2, 0.0, 3, 1, NOW(), 1, NOW(), NOW(), NOW());

-- =========================================
-- 22. 数据源 (1 个示例, 供 NL2SQL 用)
-- 字段: name/type/driver_class/jdbc_url/username/password/pool_size/
--       enabled/test_status/test_message/description/tags
-- =========================================
INSERT INTO `data_source` (`id`, `name`, `type`, `driver_class`, `jdbc_url`, `username`, `password`, `pool_size`, `min_idle`, `max_lifetime`, `enabled`, `test_status`, `test_message`, `description`, `tags`, `created_by`, `created_at`, `updated_at`, `deleted`) VALUES
(1, '示例 MySQL', 'mysql', 'com.mysql.cj.jdbc.Driver', 'jdbc:mysql://localhost:3306/demo?useUnicode=true&characterEncoding=utf8', 'demo', 'demo123', 10, 2, 1800000, 1, 'success', '连接成功', '示例数据源, 用于 NL2SQL 实验', '["demo","mysql"]', 1, NOW(), NOW(), 0);

-- =========================================
-- 23. 知识图谱实体 (3 个)
-- 字段: name/aliases/entity_type/description/user_id/importance/ref_count/source
-- =========================================
INSERT INTO `kg_entity` (`id`, `name`, `aliases`, `entity_type`, `description`, `user_id`, `importance`, `ref_count`, `source`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'MiniMax Platform', '["MiniMax","MM Platform"]', 'product',   'MiniMax 企业级 AI Agent 平台', 1, 5, 10, 'manual', NOW(), NOW(), 0),
(2, 'Spring Cloud',     '["SpringCloud"]',          'framework', '微服务框架',                    1, 4, 5,  'auto',   NOW(), NOW(), 0),
(3, 'RAG',              '["检索增强"]',             'concept',   'Retrieval-Augmented Generation', 1, 5, 8,  'manual', NOW(), NOW(), 0);


-- =========================================
-- 种子数据加载完成
-- =========================================
-- 统计:
--   租户 2
--   角色 3, 用户 4, 关联 4
--   模型提供商 5
--   AI 工具 5, Function 工具 4
--   Prompt 模板 6
--   插件 5
--   知识库 2, 文档 2, 文档块 3
--   敏感词 10
--   意图关键词 53
--   API Key 2
--   通知 3
--   告警规则 3, 告警渠道 3
--   Pipeline 模板 2
--   AI 生成日志 5
--   集群节点 1
--   数据源 1
--   知识图谱实体 3
-- 合计: 124 条种子数据
-- =========================================
