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
-- =========================================
-- MiniMax Platform V3.5.8 增量种子数据 (18 张新表 + 50+ 条)
-- =========================================
-- 时间: 2026-07-14
-- 加载: 在 sql/seed-data.sql 之后执行
-- 兼容: MySQL 8 / MariaDB / H2 (MODE=MySQL)
-- 字符集: UTF-8 (含中文)

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- =========================================
-- 1. auth_login_log (登录日志, 5 条)
-- =========================================
INSERT INTO `auth_login_log` (`id`, `user_id`, `username`, `ip`, `user_agent`, `status`, `message`, `created_at`) VALUES
(1, 1, 'adminLiugl', '192.168.1.10', 'Mozilla/5.0 Chrome/120', 1, '登录成功', '2026-07-14 10:00:00'),
(2, 1, 'adminLiugl', '192.168.1.10', 'Mozilla/5.0 Chrome/120', 1, '登录成功', '2026-07-14 04:00:00'),
(3, 3, 'test_user', '10.0.0.5', 'Mozilla/5.0 Safari/17', 1, '登录成功', '2026-07-14 07:00:00'),
(4, 4, 'demo_user', '172.16.0.20', 'Mozilla/5.0 iOS/17', 1, '登录成功', '2026-07-13 09:00:00'),
(5, 99, 'unknown', '203.0.113.50', 'curl/8.4', 0, '密码错误', '2026-07-14 11:00:00');


-- =========================================
-- 2. auth_refresh_token (刷新令牌, 3 条)
-- =========================================
INSERT INTO `auth_refresh_token` (`id`, `user_id`, `token`, `expires_at`, `revoked`, `created_at`) VALUES
(1, 1, 'rt_adminLiugl_eyJhbGciOiJIUzI1NiJ9_refresh_a1b2c3d4e5f6', '2026-07-21 12:00:00', 0, '2026-07-14 10:00:00'),
(2, 3, 'rt_test_user_eyJhbGciOiJIUzI1NiJ9_refresh_b2c3d4e5f6g7', '2026-07-21 12:00:00', 0, '2026-07-14 07:00:00'),
(3, 4, 'rt_demo_user_eyJhbGciOiJIUzI1NiJ9_refresh_c3d4e5f6g7h8', '2026-07-21 12:00:00', 1, '2026-07-13 09:00:00');


-- =========================================
-- 3. chat_session (聊天会话, 4 条)
-- =========================================
INSERT INTO `chat_session` (`id`, `user_id`, `title`, `model`, `system_prompt`, `temperature`, `status`, `message_count`, `last_message_at`, `tenant_id`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, '产品需求讨论', 'gpt-4o', '你是专业的产品经理', 0.7000, 1, 6, '2026-07-14 11:00:00', 1, '2026-07-14 09:00:00', '2026-07-14 11:00:00', 0),
(2, 1, '代码 Review', 'claude-3.5-sonnet', '你是资深工程师, 严格审查代码', 0.3000, 1, 4, '2026-07-14 08:00:00', 1, '2026-07-14 06:00:00', '2026-07-14 08:00:00', 0),
(3, 3, 'SQL 优化咨询', 'deepseek-coder', '你是 SQL 优化专家', 0.5000, 1, 2, '2026-07-13 10:00:00', 1, '2026-07-13 07:00:00', '2026-07-13 10:00:00', 0),
(4, 4, '翻译助手', 'gpt-4o-mini', '你是中英翻译', 0.3000, 1, 8, '2026-07-14 10:00:00', 2, '2026-07-14 04:00:00', '2026-07-14 10:00:00', 0);


-- =========================================
-- 4. chat_message (聊天消息, 10 条)
-- =========================================
INSERT INTO `chat_message` (`id`, `session_id`, `user_id`, `role`, `content`, `tokens`, `finish_reason`, `error_message`, `created_at`, `deleted`) VALUES
(1, 1, 1, 'user', '我想做一个 AI 客服系统, 需要哪些模块?', 24, NULL, NULL, '2026-07-14 09:00:00', 0),
(2, 1, 1, 'assistant', 'AI 客服系统核心模块: 1) 对话引擎 2) 知识库 RAG 3) 工单系统 4) 多轮管理 5) 人工接管...', 256, 'stop', NULL, '2026-07-14 09:00:00', 0),
(3, 1, 1, 'user', '需要多少人力?', 12, NULL, NULL, '2026-07-14 09:00:00', 0),
(4, 1, 1, 'assistant', '建议 3-5 人: 1 PM + 1 后端 + 1 算法 + 1 前端 + 1 测试', 86, 'stop', NULL, '2026-07-14 09:00:00', 0),
(5, 2, 1, 'user', 'Review this Python code: def add(a, b): return a + b', 32, NULL, NULL, '2026-07-14 08:00:00', 0),
(6, 2, 1, 'assistant', '建议加类型注解: def add(a: int, b: int) -> int: return a + b', 64, 'stop', NULL, '2026-07-14 08:00:00', 0),
(7, 3, 3, 'user', '如何优化这条 SQL: SELECT * FROM orders WHERE date > 2025-01-01', 36, NULL, NULL, '2026-07-13 10:00:00', 0),
(8, 3, 3, 'assistant', '建议: 1) 加索引 2) LIMIT 限制 3) 指定字段代替 *', 92, 'stop', NULL, '2026-07-13 10:00:00', 0),
(9, 4, 4, 'user', 'Translate: Hello World', 8, NULL, NULL, '2026-07-14 10:00:00', 0),
(10, 4, 4, 'assistant', '你好, 世界', 6, 'stop', NULL, '2026-07-14 10:00:00', 0);


-- =========================================
-- 5. agent_task (Agent 任务, 3 条)
-- =========================================
INSERT INTO `agent_task` (`id`, `task_id`, `user_id`, `goal`, `status`, `rounds`, `result`, `llm_calls`, `tool_calls`, `total_tokens`, `error_msg`, `latency_ms`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'task_a1b2c3d4', 1, '分析上周销售数据并生成周报', 'success', 5, '已生成 8 页 PDF 周报, 包含 6 个核心指标', 12, 4, 8420, NULL, 23450, '2026-07-14 06:00:00', '2026-07-14 06:00:00', 0),
(2, 'task_e5f6g7h8', 3, '帮我优化 5 个慢查询', 'running', 3, NULL, 6, 2, 4150, NULL, 12800, '2026-07-14 10:00:00', '2026-07-14 11:00:00', 0),
(3, 'task_i9j0k1l2', 1, '翻译文档到 3 种语言', 'failed', 2, NULL, 4, 0, 1230, 'rate limit exceeded', 8500, '2026-07-14 04:00:00', '2026-07-14 04:00:00', 0);


-- =========================================
-- 6. alert_event (告警事件, 3 条)
-- =========================================
INSERT INTO `alert_event` (`id`, `rule_id`, `rule_name`, `severity`, `metric_name`, `message`, `status`, `fired_at`, `resolved_at`, `acked_at`, `acked_by`, `duration`) VALUES
(1, 1, 'CPU > 80%', 'warning', 'system.cpu.usage', 'CPU 使用率达 92%', 'recovered', '2026-07-14 08:00:00', '2026-07-14 10:00:00', '2026-07-14 10:00:00', 1, 7200000),
(2, 2, 'Memory > 90%', 'critical', 'jvm.memory.used', 'JVM 堆内存使用 95%', 'firing', '2026-07-14 11:30:00', NULL, NULL, 0, 1800000),
(3, 3, '5xx 错误率', 'warning', 'http.error.5xx.rate', '5xx 错误率达 5.2%', 'acked', '2026-07-14 09:00:00', NULL, '2026-07-14 10:00:00', 1, 3600000);


-- =========================================
-- 7. model_battle_log (模型对决日志, 3 条)
-- =========================================
INSERT INTO `model_battle_log` (`id`, `user_id`, `prompt`, `models`, `winner`, `latency`, `tokens`, `score`, `created_at`) VALUES
(1, 1, '解释 REST API 和 GraphQL 区别', 'gpt-4o,claude-3.5,deepseek-coder,gemini-pro', 'claude-3.5', 4520, 1820, 4.5, '2026-07-14 07:00:00'),
(2, 3, '写一个 Python 快速排序', 'gpt-4o,deepseek-coder,claude-3.5', 'deepseek-coder', 2340, 980, 4.8, '2026-07-14 09:00:00'),
(3, 1, '总结《百年孤独》', 'gpt-4o,claude-3.5,gemini-pro', 'gpt-4o', 5680, 2150, 4.6, '2026-07-14 11:00:00');


SET FOREIGN_KEY_CHECKS = 1;

-- =========================================
-- 8. function_call_log (函数调用日志, 5 条)
-- =========================================
INSERT INTO `function_call_log` (`id`, `user_id`, `session_id`, `tool_name`, `arguments`, `result`, `status`, `error_msg`, `duration_ms`, `ip`, `user_agent`, `created_at`) VALUES
(1, 1, 1, 'sql_query', '{"sql":"SELECT COUNT(*) FROM users"}', '{"count":1234}', 'success', NULL, 120, '192.168.1.10', 'Mozilla/5.0', '2026-07-14 11:00:00'),
(2, 1, 1, 'web_search', '{"query":"Spring Boot 3 新特性"}', '{"results":12}', 'success', NULL, 850, '192.168.1.10', 'Mozilla/5.0', '2026-07-14 11:00:00'),
(3, 3, 2, 'code_runner', '{"code":"print(1+1)","lang":"python"}', '{"output":"2"}', 'success', NULL, 240, '10.0.0.5', 'Mozilla/5.0', '2026-07-14 10:00:00'),
(4, 1, 3, 'sql_query', '{"sql":"DROP TABLE users"}', NULL, 'failed', '权限不足', 50, '192.168.1.10', 'Mozilla/5.0', '2026-07-14 09:00:00'),
(5, 4, 4, 'image_gen', '{"prompt":"a cat","size":"1024x1024"}', '{"url":"https://cdn.example.com/cat.png"}', 'success', NULL, 4200, '172.16.0.20', 'Mobile/15', '2026-07-14 08:00:00');


-- =========================================
-- 9. analytics_datasource (数据源, 2 条)
-- =========================================
INSERT INTO `analytics_datasource` (`id`, `user_id`, `name`, `type`, `jdbc_url`, `username`, `password_enc`, `description`, `deleted`, `created_at`, `updated_at`) VALUES
(1, 1, '生产订单库', 'mysql', 'jdbc:mysql://mysql-prod:3306/orders?useSSL=true', 'readonly_user', 'enc_aes256_***', '生产环境订单数据 (只读)', 0, '2026-07-13 21:00:00', '2026-07-13 21:00:00'),
(2, 1, '日志分析库', 'clickhouse', 'jdbc:clickhouse://clickhouse:8123/logs', 'log_reader', 'enc_aes256_***', '用户行为日志 (聚合)', 0, '2026-07-14 02:00:00', '2026-07-14 02:00:00');


-- =========================================
-- 10. pipeline_run (工作流运行, 2 条)
-- =========================================
INSERT INTO `pipeline_run` (`id`, `workflow_id`, `workflow_name`, `status`, `trigger_by`, `trigger_type`, `definition_snapshot`, `start_time`, `end_time`, `duration_ms`, `error_message`, `result_summary`) VALUES
(1, 1, '智能问答工作流', 'success', 1, 'manual', '{"nodes":5,"edges":6}', '2026-07-14 09:00:00', '2026-07-14 09:00:00', 12450, NULL, '处理 12 个问题, 成功率 92%'),
(2, 2, '数据分析流水线', 'running', 3, 'schedule', '{"nodes":8,"edges":10}', '2026-07-14 11:45:00', NULL, 0, NULL, '执行中...');


-- =========================================
-- 11. ai_tool_invocation (AI 工具调用, 4 条)
-- =========================================
INSERT INTO `ai_tool_invocation` (`id`, `tool_code`, `user_id`, `username`, `input_json`, `output_json`, `status`, `error_message`, `duration_ms`, `ip`, `created_at`) VALUES
(1, 'web_search', 1, 'adminLiugl', '{"q":"Vue 3 教程"}', '{"items":10}', 'success', NULL, 1200, '192.168.1.10', '2026-07-14 10:00:00'),
(2, 'sql_query', 1, 'adminLiugl', '{"sql":"SELECT * FROM users LIMIT 5"}', '{"rows":5}', 'success', NULL, 85, '192.168.1.10', '2026-07-14 09:00:00'),
(3, 'code_runner', 3, 'test_user', '{"code":"[x*2 for x in range(5)]","lang":"python"}', '{"output":"[0,2,4,6,8]"}', 'success', NULL, 180, '10.0.0.5', '2026-07-14 11:00:00'),
(4, 'image_gen', 4, 'demo_user', '{"prompt":"mountain sunset"}', '{"url":"https://cdn/img/001.png"}', 'success', NULL, 3500, '172.16.0.20', '2026-07-14 08:00:00');


-- =========================================
-- 12. dashboard_metric (仪表盘指标, 5 条)
-- =========================================
INSERT INTO `dashboard_metric` (`id`, `metric`, `dimension`, `value`, `tags`, `timestamp`) VALUES
(1, 'api.requests.count', 'endpoint=/chat', 12450, '{''tenant'':''default''}', '2026-07-14 11:55:00'),
(2, 'api.latency.avg', 'endpoint=/chat', 235.6, '{''unit'':''ms''}', '2026-07-14 11:55:00'),
(3, 'llm.tokens.used', 'model=gpt-4o', 845000, '{''unit'':''tokens''}', '2026-07-14 11:55:00'),
(4, 'active.users', 'tenant=default', 234, NULL, '2026-07-14 11:55:00'),
(5, 'error.rate', 'service=minimax-ai', 0.02, '{''unit'':''%''}', '2026-07-14 11:55:00');


-- =========================================
-- 13. ai_chat_session (AI 对话会话, 3 条)
-- =========================================
INSERT INTO `ai_chat_session` (`id`, `session_id`, `user_id`, `username`, `title`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'sess_ai_a1b2c3', 1, 'adminLiugl', '帮我设计 API', '2026-07-14 08:00:00', '2026-07-14 09:00:00', 0),
(2, 'sess_ai_d4e5f6', 3, 'test_user', '学习 Rust', '2026-07-14 10:00:00', '2026-07-14 11:00:00', 0),
(3, 'sess_ai_g7h8i9', 4, 'demo_user', '健身计划', '2026-07-13 07:00:00', '2026-07-13 10:00:00', 0);


-- =========================================
-- 14. ai_chat_message (AI 对话消息, 8 条)
-- =========================================
INSERT INTO `ai_chat_message` (`id`, `session_id`, `role`, `content`, `tool_code`, `tool_input`, `tool_output`, `created_at`) VALUES
(1, 'sess_ai_a1b2c3', 'user', '设计 RESTful API 有什么原则?', NULL, NULL, NULL, '2026-07-14 08:00:00'),
(2, 'sess_ai_a1b2c3', 'assistant', 'RESTful 6 大原则: 1) 客户端-服务器 2) 无状态 3) 缓存 4) 分层 5) 统一接口 6) 按需代码', NULL, NULL, NULL, '2026-07-14 08:00:00'),
(3, 'sess_ai_d4e5f6', 'user', 'Rust 所有权是什么?', NULL, NULL, NULL, '2026-07-14 10:00:00'),
(4, 'sess_ai_d4e5f6', 'assistant', '所有权是 Rust 核心: 每个值有唯一所有者, 离开作用域自动释放, 避免内存泄漏', NULL, NULL, NULL, '2026-07-14 10:00:00'),
(5, 'sess_ai_d4e5f6', 'user', '给我个例子', NULL, NULL, NULL, '2026-07-14 11:00:00'),
(6, 'sess_ai_d4e5f6', 'assistant', 'let s = String::from("hello"); // s 是所有者', 'code_runner', '{"code":"fn main() { let s = String::from(\"hi\"); println!(\"{}\", s); }"}', 'compiled', '2026-07-14 11:00:00'),
(7, 'sess_ai_g7h8i9', 'user', '一周健身计划', NULL, NULL, NULL, '2026-07-13 07:00:00'),
(8, 'sess_ai_g7h8i9', 'assistant', '周一胸/三头, 周二背/二头, 周三腿, 周四休息, 周五肩, 周六有氧, 周日拉伸', NULL, NULL, NULL, '2026-07-13 07:00:00');


-- =========================================
-- 15. model_quota (模型配额, 4 条)
-- =========================================
INSERT INTO `model_quota` (`id`, `user_id`, `model_id`, `quota_date`, `used_tokens`, `used_requests`, `limit_tokens`, `limit_requests`, `created_at`, `updated_at`) VALUES
(1, 1, 1, '2026-07-14', 124500, 245, 1000000, 5000, '2026-07-14 12:00:00', '2026-07-14 12:00:00'),
(2, 1, 2, '2026-07-14', 45200, 89, 500000, 2000, '2026-07-14 12:00:00', '2026-07-14 12:00:00'),
(3, 3, 1, '2026-07-14', 18200, 56, 100000, 500, '2026-07-14 12:00:00', '2026-07-14 12:00:00'),
(4, 4, 3, '2026-07-14', 5400, 23, 50000, 200, '2026-07-14 12:00:00', '2026-07-14 12:00:00');


-- =========================================
-- 16. push_message (推送消息, 3 条)
-- =========================================
INSERT INTO `push_message` (`id`, `message_id`, `title`, `body`, `icon`, `click_action`, `data`, `target_type`, `target_value`, `status`) VALUES
(1, 'msg_2026_07_a1', '新功能上线', 'V3.5.8 数据分析能力已上线, 试试 SQL 查询', '/icons/rocket.png', '/admin/ai-market', '{"feature":"data_analysis"}', 'all', '*', 'sent'),
(2, 'msg_2026_07_b2', '系统维护', '今晚 23:00-01:00 系统维护, 请提前保存', '/icons/wrench.png', '/about', '{"maintenance":true}', 'all', '*', 'sent'),
(3, 'msg_2026_07_c3', '优惠活动', '新用户注册即送 10000 tokens!', '/icons/gift.png', '/register', '{"promo":"new_user"}', 'role', 'guest', 'draft');


-- =========================================
-- 17. moderation_record (审核记录, 3 条)
-- =========================================
INSERT INTO `moderation_record` (`id`, `trace_id`, `user_id`, `username`, `content_type`, `content_hash`, `content_size`, `content_url`, `moderation_status`, `risk_level`) VALUES
(1, 'trace_mod_a1b2', 1, 'adminLiugl', 'text', 'sha256_aaa111', 256, NULL, 'pass', 'low'),
(2, 'trace_mod_c3d4', 3, 'test_user', 'image', 'sha256_bbb222', 524288, 'https://cdn/img/test.jpg', 'pass', 'low'),
(3, 'trace_mod_e5f6', 99, 'spammer', 'text', 'sha256_ccc333', 1024, NULL, 'block', 'high');


-- =========================================
-- 18. multimedia_file (多媒体文件, 2 条)
-- =========================================
INSERT INTO `multimedia_file` (`id`, `file_id`, `user_id`, `username`, `file_name`, `original_name`, `file_type`, `mime_type`, `file_size`, `file_hash`) VALUES
(1, 'file_a1b2c3d4', 1, 'adminLiugl', '2026_product_intro.png', '产品介绍图.png', 'image', 'image/png', 524288, 'sha256_img001'),
(2, 'file_e5f6g7h8', 3, 'test_user', 'audio_001.mp3', 'meeting.mp3', 'audio', 'audio/mpeg', 3145728, 'sha256_aud001');


-- =========================================
-- 19. audit_log (审计日志, 3 条)
-- =========================================
INSERT INTO `audit_log` (`id`, `trace_id`, `user_id`, `username`, `user_ip`, `user_agent`, `action`, `resource_type`, `resource_id`, `method`) VALUES
(1, 'trace_aud_a1', 1, 'adminLiugl', '192.168.1.10', 'Mozilla/5.0', 'create', 'model', '1', 'POST'),
(2, 'trace_aud_b2', 1, 'adminLiugl', '192.168.1.10', 'Mozilla/5.0', 'update', 'alert_rule', '2', 'PUT'),
(3, 'trace_aud_c3', 3, 'test_user', '10.0.0.5', 'Mozilla/5.0', 'delete', 'chat_session', '5', 'DELETE');


SET FOREIGN_KEY_CHECKS = 1;

-- =========================================
-- 20. kg_relation (知识图谱关系, 4 条)
-- =========================================
INSERT INTO `kg_relation` (`id`, `user_id`, `from_entity`, `to_entity`, `relation_type`, `description`, `weight`, `source`, `ref_count`, `created_at`) VALUES
(1, 1, 1, 2, 'developed_by', 'Platform 由 MiniMax 开发', 1.0000, 'system', 15, '2026-07-13 06:00:1783922400'),
(2, 1, 1, 3, 'uses', 'Platform 使用 RAG 技术', 0.9500, 'system', 12, '2026-07-13 06:00:1783922400'),
(3, 1, 1, 4, 'supports', 'Platform 支持 17 微服务', 0.9000, 'system', 8, '2026-07-13 06:00:1783922400'),
(4, 1, 1, 5, 'contains', 'Platform 包含 AI 模块', 0.8500, 'system', 6, '2026-07-13 06:00:1783922400');


-- =========================================
-- 21. collab_room (协作房间, 3 条)
-- =========================================
INSERT INTO `collab_room` (`id`, `roomId`, `name`, `type`, `ownerId`, `ownerName`, `description`, `isPublic`, `maxParticipants`, `status`) VALUES
(1, 'room_a1b2c3', '产品需求讨论', 'chat', 1, 'adminLiugl', '团队产品需求实时讨论', 0, 10, 'active'),
(2, 'room_d4e5f6', '代码审查', 'code', 1, 'adminLiugl', 'PR 实时审查', 1, 5, 'active'),
(3, 'room_g7h8i9', '数据看板', 'dashboard', 3, 'test_user', '实时数据监控', 0, 20, 'active');


-- =========================================
-- 22. pipeline_node_log (工作流节点日志, 4 条)
-- =========================================
INSERT INTO `pipeline_node_log` (`id`, `run_id`, `node_id`, `node_type`, `node_name`, `status`, `start_time`, `end_time`, `duration_ms`, `input_rows`) VALUES
(1, 1, 'node_input_1', 'input', '用户输入', 'success', '2026-07-14 09:00:1784019600', '2026-07-14 09:00:1784019600', 50, 1),
(2, 1, 'node_llm_1', 'llm', 'GPT-4 推理', 'success', '2026-07-14 09:00:1784019600', '2026-07-14 09:00:1784019600', 4200, 1),
(3, 1, 'node_output_1', 'output', '返回结果', 'success', '2026-07-14 09:00:1784019600', '2026-07-14 09:00:1784019600', 80, 1),
(4, 2, 'node_sql_1', 'sql', '查询订单', 'running', '2026-07-14 11:45:1784029500', NULL, 0, 0);


-- =========================================
-- 23. model_config (模型配置, 5 条)
-- =========================================
INSERT INTO `model_config` (`id`, `provider_id`, `model_code`, `display_name`, `max_context`, `max_output`, `input_price`, `output_price`, `supports_vision`, `supports_tools`) VALUES
(1, 1, 'gpt-4o', 'GPT-4o', 128000, 4096, 0.0050, 0.0150, 1, 1),
(2, 1, 'gpt-4o-mini', 'GPT-4o Mini', 128000, 16384, 0.0002, 0.0006, 1, 1),
(3, 2, 'claude-3.5-sonnet', 'Claude 3.5 Sonnet', 200000, 8192, 0.0030, 0.0150, 1, 1),
(4, 3, 'deepseek-coder', 'DeepSeek Coder', 32000, 4096, 0.0001, 0.0002, 0, 1),
(5, 4, 'gemini-pro-1.5', 'Gemini Pro 1.5', 1000000, 8192, 0.0035, 0.0105, 1, 1);


-- =========================================
-- 24. kb_document (知识库文档, 3 条)
-- =========================================
INSERT INTO `kb_document` (`id`, `doc_id`, `kb_id`, `filename`, `mime_type`, `size_bytes`, `sha256`, `file_path`, `source`, `source_url`) VALUES
(1, 'doc_a1b2c3', '1', 'platform_intro.md', 'text/markdown', 15360, 'sha256_doc001', '/data/kb/1/doc001.md', 'upload', NULL),
(2, 'doc_d4e5f6', '1', 'api_quickstart.md', 'text/markdown', 24576, 'sha256_doc002', '/data/kb/1/doc002.md', 'upload', NULL),
(3, 'doc_g7h8i9', '2', 'faq.pdf', 'application/pdf', 524288, 'sha256_doc003', '/data/kb/2/doc003.pdf', 'web', 'https://docs.example.com/faq.pdf');


-- =========================================
-- 25. kb_chunk (知识库块, 4 条)
-- =========================================
INSERT INTO `kb_chunk` (`id`, `chunk_id`, `doc_id`, `kb_id`, `seq`, `content`, `char_count`, `token_count`, `embedding`, `embedding_model`) VALUES
(1, 'chunk_a1', 'doc_a1b2c3', '1', 1, 'MiniMax Platform 是企业级 LLM 应用平台', 32, 18, 'emb_a1b2c3d4', 'text-embedding-3-small'),
(2, 'chunk_a2', 'doc_a1b2c3', '1', 2, '支持 17 个微服务, 涵盖 AI 全场景', 28, 15, 'emb_e5f6g7h8', 'text-embedding-3-small'),
(3, 'chunk_b1', 'doc_d4e5f6', '1', 1, 'API 调用请参考 /api/v1/*', 24, 13, 'emb_i9j0k1l2', 'text-embedding-3-small'),
(4, 'chunk_b2', 'doc_d4e5f6', '1', 2, '鉴权用 Bearer Token', 18, 10, 'emb_m3n4o5p6', 'text-embedding-3-small');


-- =========================================
-- 26. agent_group (Agent 群组, 3 条)
-- =========================================
INSERT INTO `agent_group` (`id`, `name`, `description`, `owner_id`, `member_count`, `created_at`) VALUES
(1, '开发组', '后端 + 前端 + 测试', 1, 8, '2026-07-13 06:00:1783922400'),
(2, '数据组', '数据分析 + 算法', 1, 5, '2026-07-13 11:00:1783940400'),
(3, '运维组', 'DevOps + SRE', 1, 3, '2026-07-13 16:00:1783958400');


-- =========================================
-- 27. training_job (训练任务, 2 条)
-- =========================================
INSERT INTO `training_job` (`id`, `job_name`, `model_name`, `dataset_name`, `status`, `epochs`, `batch_size`, `learning_rate`, `loss`, `accuracy`, `started_at`, `finished_at`) VALUES
(1, 'sentiment-finetune-v1', 'bert-base-chinese', 'weibo-sentiment-100k', 'success', 3, 32, 0.00002, 0.1234, 0.9234, '2026-07-13 07:00:1783926000', '2026-07-14 10:00:1784023200'),
(2, 'qa-finetune-v2', 'qwen-1.5b', 'custom-qa-50k', 'running', 1, 16, 0.00001, 0.4521, 0.7856, '2026-07-14 09:00:1784019600', NULL);


-- =========================================
-- 28. analytics_nlsql_history (NL2SQL 历史, 4 条)
-- =========================================
INSERT INTO `analytics_nlsql_history` (`id`, `user_id`, `question`, `generated_sql`, `data_source_id`, `status`, `row_count`, `duration_ms`, `created_at`) VALUES
(1, 1, '上月订单总数', 'SELECT COUNT(*) FROM orders WHERE created_at > DATE_SUB(NOW(), INTERVAL 1 MONTH)', 1, 'success', 1234, 120, '2026-07-14 10:00:1784023200'),
(2, 1, '本周活跃用户', 'SELECT COUNT(DISTINCT user_id) FROM events WHERE ts > DATE_SUB(NOW(), INTERVAL 7 DAY)', 1, 'success', 567, 85, '2026-07-14 08:00:1784016000'),
(3, 3, '畅销产品 TOP 10', 'SELECT product_id, SUM(qty) as total FROM order_items GROUP BY product_id ORDER BY total DESC LIMIT 10', 1, 'success', 10, 220, '2026-07-14 09:00:1784019600'),
(4, 1, '上月营收', 'SELECT SUM(amount) FROM payments WHERE created_at > DATE_SUB(NOW(), INTERVAL 1 MONTH)', 1, 'failed', 0, 50, '2026-07-14 11:00:1784026800');


-- =========================================
-- 29. analytics_report (分析报告, 3 条)
-- =========================================
INSERT INTO `analytics_report` (`id`, `report_name`, `report_type`, `data_source_id`, `format`, `file_path`, `file_size`, `status`, `creator_id`, `created_at`) VALUES
(1, '2026 Q2 销售报告', 'quarterly', 1, 'pdf', '/reports/2026_q2_sales.pdf', 2048576, 'ready', 1, '2026-07-14 07:00:1784012400'),
(2, '用户增长周报', 'weekly', 1, 'html', '/reports/weekly_growth.html', 524288, 'ready', 1, '2026-07-14 09:00:1784019600'),
(3, '产品库存月报', 'monthly', 1, 'xlsx', '/reports/monthly_inventory.xlsx', 1048576, 'generating', 3, '2026-07-14 11:00:1784026800');


-- =========================================
-- 30. billing_record (计费记录, 4 条)
-- =========================================
INSERT INTO `billing_record` (`id`, `user_id`, `username`, `model_code`, `input_tokens`, `output_tokens`, `total_tokens`, `cost_cny`, `billing_date`, `created_at`) VALUES
(1, 1, 'adminLiugl', 'gpt-4o', 12500, 8200, 20700, 0.4500, '2026-07-14', '2026-07-14 10:00:1784023200'),
(2, 1, 'adminLiugl', 'claude-3.5', 8200, 5100, 13300, 0.3200, '2026-07-14', '2026-07-14 09:00:1784019600'),
(3, 3, 'test_user', 'deepseek-coder', 15200, 9800, 25000, 0.0250, '2026-07-14', '2026-07-14 08:00:1784016000'),
(4, 4, 'demo_user', 'gpt-4o-mini', 4500, 2100, 6600, 0.0030, '2026-07-14', '2026-07-14 07:00:1784012400');
