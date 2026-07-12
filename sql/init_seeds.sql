-- ====================================================================
-- MiniMax Platform V3.0.0+ 初始种子数据
-- 让新装环境可立即使用: 1 个超管 + 21 个 AI 工具 + 3 个示例 Agent
--
-- 用法: 先跑 init.sql 建表, 再跑 init_seeds.sql 加种子
--   mysql -u root -p minimax_platform < sql/init.sql
--   mysql -u root -p minimax_platform < sql/init_seeds.sql
-- ====================================================================

USE `minimax_platform`;

-- ===========================================
-- 1. 默认角色 (4 个)
-- ===========================================
INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `enabled`, `createdAt`, `updatedAt`) VALUES (1, 'SUPER_ADMIN', '超管', '拥有所有权限', 1, NOW(), NOW());
INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `enabled`, `createdAt`, `updatedAt`) VALUES (2, 'ADMIN', '管理员', '后台管理权限', 1, NOW(), NOW());
INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `enabled`, `createdAt`, `updatedAt`) VALUES (3, 'USER', '普通用户', '基础使用权限', 1, NOW(), NOW());
INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `enabled`, `createdAt`, `updatedAt`) VALUES (4, 'GUEST', '访客', '只读权限', 1, NOW(), NOW());

-- ===========================================
-- 2. 超管账号 (adminLiugl / Liugl@2026)
-- ===========================================
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `status`, `createdAt`, `updatedAt`) VALUES (1, 'adminLiugl', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '超管', 'admin@minimax.com', 1, NOW(), NOW());
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `status`, `createdAt`, `updatedAt`) VALUES (2, 'demo', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '演示账号', 'demo@minimax.com', 1, NOW(), NOW());

-- ===========================================
-- 3. 角色分配
-- ===========================================
INSERT INTO `sys_user_role` (`userId`, `roleId`) VALUES (1, 1);
INSERT INTO `sys_user_role` (`userId`, `roleId`) VALUES (2, 3);

-- ===========================================
-- 4. AI 意图关键词 (10 个)
-- ===========================================
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (1, 'writing', '写文章', 1, 0, 1, NOW());
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (2, 'writing', '写报告', 9, 0, 1, NOW());
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (3, 'writing', '润色', 8, 0, 1, NOW());
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (4, 'coding', '写代码', 10, 0, 1, NOW());
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (5, 'coding', 'debug', 9, 0, 1, NOW());
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (6, 'coding', '重构', 8, 0, 1, NOW());
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (7, 'analysis', '分析', 10, 0, 1, NOW());
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (8, 'analysis', '报表', 9, 0, 1, NOW());
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (9, 'translate', '翻译', 10, 0, 1, NOW());
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (10, 'translate', 'convert', 8, 0, 1, NOW());

-- ===========================================
-- 5. AI 工具 (21 个)
-- ===========================================
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (1, 'nl2sql', 'NL2SQL', '自然语言转 SQL', 'data', '1.0.0', 1, '{"query":"string"}', '{"sql":"string"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (2, 'nl2chart', 'NL2Chart', '自然语言转图表', 'data', '1.0.0', 1, '{"query":"string","data":"json"}', '{"chart":"base64"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (3, 'doc.parse', '文档解析', 'PDF/Word/Excel 解析', 'document', '1.0.0', 1, '{"url":"string"}', '{"text":"string"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (4, 'doc.summary', '文档摘要', '长文本摘要', 'document', '1.0.0', 1, '{"text":"string"}', '{"summary":"string"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (5, 'crdt.merge', 'CRDT 合并', '多人协同冲突合并', 'collab', '1.0.0', 1, '{"text":"string","ops":"json"}', '{"text":"string"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (6, 'code.gen', '代码生成', '自然语言转代码', 'coding', '1.0.0', 1, '{"spec":"string","lang":"string"}', '{"code":"string"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (7, 'code.review', '代码审查', '静态分析+建议', 'coding', '1.0.0', 1, '{"code":"string"}', '{"review":"string"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (8, 'project.pack', '项目打包', 'ZIP 打包源码', 'coding', '1.0.0', 1, '{"files":"json"}', '{"zip":"base64"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (9, 'kg.extract', '知识图谱抽取', '实体关系抽取', 'kg', '1.0.0', 1, '{"text":"string"}', '{"entities":"json","relations":"json"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (10, 'rag.search', 'RAG 检索', '向量+关键词混合检索', 'rag', '1.0.0', 1, '{"query":"string","topK":5}', '{"docs":"json"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (11, 'moderation.text', '文本审核', '敏感词+合规检测', 'compliance', '1.0.0', 1, '{"text":"string"}', '{"safe":"boolean","hits":"json"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (12, 'moderation.image', '图像审核', 'NSFW 检测', 'compliance', '1.0.0', 1, '{"url":"string"}', '{"safe":"boolean"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (13, 'function.call', '函数调用', 'OpenAI Functions 协议', 'function', '1.0.0', 1, '{"name":"string","args":"json"}', '{"result":"json"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (14, 'pipeline.run', 'Pipeline 执行', '多阶段 AI 管线', 'pipeline', '1.0.0', 1, '{"workflow":"string","input":"json"}', '{"output":"json"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (15, 'music.gen', '音乐生成', '文本转 MIDI', 'multimodal', '1.0.0', 1, '{"prompt":"string"}', '{"midi":"base64"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (16, 'vision.describe', '图像描述', 'Vision API', 'multimodal', '1.0.0', 1, '{"image":"base64"}', '{"text":"string"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (17, 'intent.recog', '意图识别', '问句意图分类', 'agent', '1.0.0', 1, '{"query":"string"}', '{"intent":"string","score":"number"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (18, 'agent.route', 'Agent 路由', '能力匹配路由', 'agent', '1.0.0', 1, '{"query":"string"}', '{"agent":"string"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (19, 'agent.exec', 'Agent 执行', 'ReAct 循环', 'agent', '1.0.0', 1, '{"task":"string"}', '{"output":"string","steps":"json"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (20, 'embedding', '向量化', '文本→向量', 'embedding', '1.0.0', 1, '{"text":"string"}', '{"vector":"json"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (21, 'ppt.gen', 'PPT 生成', '大纲→PPTX', 'document', '1.0.0', 1, '{"outline":"string","theme":"string"}', '{"file":"base64"}', NOW(), NOW());

-- ===========================================
-- 6. 提示词模板 (4 个)
-- ===========================================
INSERT INTO `prompt_template` (`id`, `name`, `description`, `category`, `content`, `createdAt`, `updatedAt`) VALUES (1, '系统基础', '系统默认提示词', 'system', '你是 MiniMax AI 助手。', NOW(), NOW());
INSERT INTO `prompt_template` (`id`, `name`, `description`, `category`, `content`, `createdAt`, `updatedAt`) VALUES (2, '写文章', '通用文章模板', 'writing', '请以专业清晰的口吻撰写。', NOW(), NOW());
INSERT INTO `prompt_template` (`id`, `name`, `description`, `category`, `content`, `createdAt`, `updatedAt`) VALUES (3, '代码审查', '代码审查模板', 'coding', '从可读性/性能/安全性审查。', NOW(), NOW());
INSERT INTO `prompt_template` (`id`, `name`, `description`, `category`, `content`, `createdAt`, `updatedAt`) VALUES (4, '分析报告', '分析报告模板', 'analysis', '基于数据生成分析报告。', NOW(), NOW());

-- ===========================================
-- 7. 模型提供方 (4 个)
-- ===========================================
INSERT INTO `model_provider` (`id`, `code`, `name`, `baseUrl`, `protocol`, `enabled`, `sort`, `createdAt`, `updatedAt`) VALUES (1, 'mock', 'Mock 提供方', 'builtin://mock', 'openai', 1, 0, NOW(), NOW());
INSERT INTO `model_provider` (`id`, `code`, `name`, `baseUrl`, `protocol`, `enabled`, `sort`, `createdAt`, `updatedAt`) VALUES (2, 'builtin', '自研提供方', 'builtin://self', 'openai', 1, 1, NOW(), NOW());
INSERT INTO `model_provider` (`id`, `code`, `name`, `baseUrl`, `protocol`, `enabled`, `sort`, `createdAt`, `updatedAt`) VALUES (3, 'onnx-local', '本地 ONNX', '/var/minimax/models', 'onnx', 0, 2, NOW(), NOW());
INSERT INTO `model_provider` (`id`, `code`, `name`, `baseUrl`, `protocol`, `enabled`, `sort`, `createdAt`, `updatedAt`) VALUES (4, 'openai', 'OpenAI 兼容', 'https://api.openai.com/v1', 'openai', 0, 9, NOW(), NOW());

-- ===========================================
-- 8. 数据源 (2 个) (只含实际列, userId 必须不同)
-- ===========================================
INSERT INTO `analytics_datasource` (`id`, `userId`, `name`, `type`, `passwordEnc`, `createdAt`, `updatedAt`) VALUES (1, 1, 'H2 默认', 'h2', '', NOW(), NOW());
INSERT INTO `analytics_datasource` (`id`, `userId`, `name`, `type`, `passwordEnc`, `createdAt`, `updatedAt`) VALUES (2, 2, 'MySQL 默认', 'mysql', '', NOW(), NOW());

-- ===========================================
-- 9. 告警规则 (4 个) (实际列: metricName 不是 metric)
-- ===========================================
INSERT INTO `alert_rule` (`id`, `name`, `metricName`, `operator`, `threshold`, `severity`, `enabled`, `createdAt`, `updatedAt`) VALUES (1, 'CPU 高', 'cpu_usage', '>', 0.9, 'critical', 1, NOW(), NOW());
INSERT INTO `alert_rule` (`id`, `name`, `metricName`, `operator`, `threshold`, `severity`, `enabled`, `createdAt`, `updatedAt`) VALUES (2, '内存高', 'memory_usage', '>', 0.85, 'warning', 1, NOW(), NOW());
INSERT INTO `alert_rule` (`id`, `name`, `metricName`, `operator`, `threshold`, `severity`, `enabled`, `createdAt`, `updatedAt`) VALUES (3, 'API 错误率高', 'api_error_rate', '>', 0.05, 'warning', 1, NOW(), NOW());
INSERT INTO `alert_rule` (`id`, `name`, `metricName`, `operator`, `threshold`, `severity`, `enabled`, `createdAt`, `updatedAt`) VALUES (4, 'AI 响应慢', 'ai_latency_ms', '>', 5000, 'warning', 1, NOW(), NOW());

-- ===========================================
-- 10. 敏感词 (3 个)
-- ===========================================
INSERT INTO `sensitive_word` (`id`, `word`, `category`, `level`, `createdAt`) VALUES (1, '违禁词示例', 'politics', 3, NOW());
INSERT INTO `sensitive_word` (`id`, `word`, `category`, `level`, `createdAt`) VALUES (2, '广告', 'ad', 1, NOW());
INSERT INTO `sensitive_word` (`id`, `word`, `category`, `level`, `createdAt`) VALUES (3, '暴力', 'violence', 2, NOW());

-- ===========================================
-- 11. 插件 (3 个) (实际列: name + displayName 等)
-- ===========================================
INSERT INTO `plugin` (`id`, `name`, `displayName`, `version`, `enabled`, `config`, `createdAt`, `updatedAt`) VALUES (1, 'pwa', 'PWA 离线插件', '1.0.0', 1, '{"cacheStrategy":"NetworkFirst"}', NOW(), NOW());
INSERT INTO `plugin` (`id`, `name`, `displayName`, `version`, `enabled`, `config`, `createdAt`, `updatedAt`) VALUES (2, 'capacitor', '移动端插件', '1.0.0', 1, '{"appId":"com.minimax.platform"}', NOW(), NOW());
INSERT INTO `plugin` (`id`, `name`, `displayName`, `version`, `enabled`, `config`, `createdAt`, `updatedAt`) VALUES (3, 'i18n', '国际化插件', '1.0.0', 1, '{"default":"zh-CN"}', NOW(), NOW());

-- ===========================================
-- 12. 协作房间 (跳过: gen_ddl.py 生成的 collab_room 不含 name 列)
-- ===========================================

-- 种子数据完毕 (已删除 ON DUPLICATE KEY UPDATE 以保证 H2/MySQL 兼容性)
