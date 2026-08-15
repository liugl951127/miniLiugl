-- ============================================================
-- MiniMax Platform V6.8.2 种子数据
-- 生成时间: 2026-08-12
-- 说明: BCrypt 密码占位符 (部署后建议通过管理后台修改)
--       Admin@123 → $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy (password)
-- ============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 模块: minimax-auth  认证与用户
-- ============================================================

-- 角色 (先插角色，因为用户依赖 role)
INSERT INTO `mm_sys_role` (id, code, name, description, sort, enabled, created_at) VALUES
(1, 'ADMIN', '系统管理员', '拥有所有系统管理权限', 1, 1, '2026-01-01 00:00:00'),
(2, 'USER',  '普通用户',   '基础功能使用权限',       2, 1, '2026-01-01 00:00:00'),
(3, 'GUEST', '访客',      '只读权限',                3, 1, '2026-01-01 00:00:00');

-- 租户
INSERT INTO `mm_tenant` (id, code, name, plan, status, max_users, max_models, qps_limit, monthly_quota, used_quota, expire_at, contact_email, created_at) VALUES
(1, 'default',   '默认租户',     'free',  1, 10,  3, 10, 100000, 0, '2027-12-31 23:59:59', 'admin@minimax.io',  '2026-01-01 00:00:00'),
(2, 'enterprise','企业版租户',    'pro',   1, 100, 20, 100, 10000000, 0, '2027-12-31 23:59:59', 'corp@minimax.io',  '2026-01-15 00:00:00');

-- 用户 (BCrypt hash = password)
INSERT INTO `mm_sys_user` (id, username, password, nickname, email, phone, avatar, gender, status, last_login_ip, tenant_id, created_at) VALUES
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 'admin@minimax.io', '13800138000', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin', 1, 1, '127.0.0.1', 1, '2026-01-01 00:00:00'),
(2, 'liugl',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '刘GL',        'liugl@minimax.io',  '13900139000', 'https://api.dicebear.com/7.x/avataaars/svg?seed=liugl',  1, 1, '127.0.0.1', 1, '2026-01-02 00:00:00'),
(3, 'guest',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '访客用户',    'guest@minimax.io',  NULL,           'https://api.dicebear.com/7.x/avataaars/svg?seed=guest',  0, 1, NULL, 1, '2026-01-03 00:00:00');

-- 用户-角色关联
INSERT INTO `mm_sys_user_role` (user_id, role_id) VALUES
(1, 1),  -- admin → ADMIN
(2, 2),  -- liugl → USER
(3, 3);  -- guest → GUEST

-- API Key
INSERT INTO `mm_user_api_key` (id, user_id, name, key_hash, key_prefix, scopes, expires_at, last_used_at, use_count, enabled, created_at) VALUES
(1, 2, '默认 Key', 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6', 'sk-mm_a1b2****', 'chat,rag,agent', '2027-12-31 00:00:00', '2026-08-01 00:00:00', 0, 1, '2026-01-10 00:00:00'),
(2, 1, 'Admin Key', 'b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1', 'sk-mm_b2c3****', '*', '2027-12-31 00:00:00', '2026-08-10 00:00:00', 150, 1, '2026-01-10 00:00:00');

-- 通知
INSERT INTO `mm_notification` (id, user_id, type, title, content, is_read, created_at) VALUES
(1, 1, 'SYSTEM', '欢迎使用 MiniMax', '系统初始化完成，V6.8.2 版本已就绪', 0, '2026-01-01 00:00:00'),
(2, 2, 'SYSTEM', '账号激活',         '您的账号已激活，可以开始使用了', 1, '2026-01-02 00:00:00');

-- 登录日志
INSERT INTO `mm_auth_login_log` (id, user_id, username, ip, user_agent, status, fail_reason, created_at) VALUES
(1, 1, 'admin', '127.0.0.1', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', 1, NULL, '2026-01-01 08:00:00'),
(2, 2, 'liugl', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 1, NULL, '2026-01-02 09:00:00');

-- ============================================================
-- 模块: minimax-model  模型服务
-- ============================================================

-- 模型提供商
INSERT INTO `mm_model_provider` (id, code, name, base_url, api_key, protocol, enabled, sort, description, created_at) VALUES
(1, 'openai',      'OpenAI',          'https://api.openai.com',        '',                  'openai',     1, 1, 'OpenAI GPT 系列模型',                          '2026-01-01 00:00:00'),
(2, 'siliconflow', 'SiliconFlow',     'https://api.siliconflow.cn',     '',                  'openai',     1, 2, 'SiliconFlow 聚合 API (DeepSeek/Qwen/GLM 等)',   '2026-01-01 00:00:00'),
(3, 'ollama',      'Ollama (本地)',    'http://localhost:11434',         '',                  'ollama',     1, 3, 'Ollama 本地推理 (MiniTransformer/Qwen 等)',     '2026-01-01 00:00:00'),
(4, 'deepseek',    'DeepSeek',         'https://api.deepseek.com',       '',                  'openai',     1, 4, 'DeepSeek 大模型',                               '2026-01-01 00:00:00'),
(5, 'minimax',     'MiniMax 自研',     'http://localhost:8080/model',    '',                  'onnx',       1, 5, 'MiniMax 自研 MiniTransformer ONNX 推理',       '2026-01-01 00:00:00');

-- 模型配置
INSERT INTO `mm_model_config` (id, provider_id, model_code, display_name, max_context, max_output, input_price, output_price, supports_vision, supports_tools, supports_stream, enabled, sort, description, created_at) VALUES
-- OpenAI
(1, 1, 'gpt-4o',             'GPT-4o',            128000, 16384, 0.005,    0.015,   1, 1, 1, 1, 10, 'OpenAI 最强多模态模型',                   '2026-01-01 00:00:00'),
(2, 1, 'gpt-4o-mini',         'GPT-4o mini',        128000, 16384, 0.00015, 0.0006,  1, 1, 1, 1, 20, 'GPT-4o 轻量版，性价比高',                '2026-01-01 00:00:00'),
(3, 1, 'gpt-4-turbo',         'GPT-4 Turbo',        128000, 4096,  0.01,    0.03,    1, 1, 1, 0, 15, 'GPT-4 高速版',                           '2026-01-01 00:00:00'),
-- SiliconFlow
(4, 2, 'deepseek-chat',       'DeepSeek Chat',      64000,  8192,  0.0001,  0.0003,  1, 1, 1, 1, 30, 'DeepSeek 深度思考模型',                  '2026-01-01 00:00:00'),
(5, 2, 'Qwen/Qwen2.5-72B-Instruct', 'Qwen 72B',     32000,  4096,  0.0006,  0.0018,  1, 1, 1, 1, 40, '通义千问 72B 大模型',                     '2026-01-01 00:00:00'),
-- Ollama 本地
(6, 3, 'llama3.2',            'Llama 3.2',          8192,   4096,  0,       0,        0, 1, 1, 1, 50, 'Meta Llama 3.2 (本地)',                  '2026-01-01 00:00:00'),
(7, 3, 'qwen2.5',             'Qwen 2.5 (本地)',    8192,   4096,  0,       0,        1, 1, 1, 1, 51, '通义千问 2.5 (本地)',                    '2026-01-01 00:00:00'),
(8, 3, 'min-transformer',     'MiniTransformer',    4096,   1024,  0,       0,        0, 1, 1, 1, 99, 'MiniMax 自研 Transformer (ONNX)',         '2026-01-01 00:00:00'),
-- DeepSeek
(9, 4, 'deepseek-reasoner',   'DeepSeek R1',         64000,  8192,  0.0001,  0.0003,  0, 1, 1, 1, 35, 'DeepSeek 推理模型 R1',                   '2026-01-01 00:00:00'),
-- Embedding
(10, 2, 'BAAI/bge-m3',        'BGE-M3 Embedding',   4096,   0,     0.00001, 0,       0, 0, 0, 1, 60, '智谱 BGE-M3 多语言 Embedding',           '2026-01-01 00:00:00');

-- 模型配额 (每个用户每天免费额度)
INSERT INTO `mm_model_quota` (id, user_id, model_id, quota_date, used_tokens, used_requests, limit_tokens, limit_requests, created_at, updated_at) VALUES
(1, 2, 4,  CURDATE(), 0, 0, 100000, 100, NOW(), NOW()),
(2, 2, 5,  CURDATE(), 0, 0, 100000, 100, NOW(), NOW()),
(3, 2, 8,  CURDATE(), 0, 0, 999999999, 999999, NOW(), NOW()),
(4, 3, 4,  CURDATE(), 0, 0, 10000, 10, NOW(), NOW());

-- ============================================================
-- 模块: minimax-chat  聊天会话
-- ============================================================

INSERT INTO `mm_chat_session` (id, user_id, title, model, system_prompt, temperature, status, message_count, last_message_at, tenant_id, created_at, updated_at) VALUES
(1, 2, 'AI 助手对话',   'gpt-4o-mini',     '你是一个有帮助的 AI 助手。', 0.7, 1, 4, '2026-08-10 10:00:00', 1, '2026-08-10 09:00:00', '2026-08-10 10:00:00'),
(2, 2, '本地模型测试',  'min-transformer', '本地 MiniTransformer 推理测试', 0.8, 1, 2, '2026-08-11 15:00:00', 1, '2026-08-11 14:00:00', '2026-08-11 15:00:00'),
(3, 2, 'RAG 知识问答',  'deepseek-chat',   '基于知识库回答问题。', 0.5, 1, 3, '2026-08-12 08:00:00', 1, '2026-08-12 08:00:00', '2026-08-12 08:00:00');

INSERT INTO `mm_chat_message` (id, session_id, user_id, role, content, tokens, finish_reason, created_at) VALUES
(1, 1, 2, 'user',   '你好，请介绍一下 MiniMax 平台', NULL, NULL, '2026-08-10 09:05:00'),
(2, 1, 2, 'assistant','MiniMax 是一个企业级 AI Agent 平台，支持 RAG、Agent 编排、多模态模型接入。', 128, 'stop', '2026-08-10 09:05:30'),
(3, 1, 2, 'user',   'V6.8.2 有哪些新特性？', NULL, NULL, '2026-08-10 09:10:00'),
(4, 1, 2, 'assistant','V6.8.2: RAG SSE 流式、Agent Canvas 可视化、多模态真实 API、安全加固。', 96, 'stop', '2026-08-10 09:10:15'),
(5, 2, 2, 'user',   '用本地模型生成一段 Python 代码', NULL, NULL, '2026-08-11 14:05:00'),
(6, 2, 2, 'assistant','以下是 Python 示例代码：\n```python\ndef hello():\n    print("Hello from MiniTransformer!")\n```', 64, 'stop', '2026-08-11 14:05:30'),
(7, 3, 2, 'user',   '什么是 RAG？', NULL, NULL, '2026-08-12 08:05:00'),
(8, 3, 2, 'assistant','RAG (检索增强生成) 结合向量检索与 LLM 推理，实时从知识库获取相关片段，提升回答准确性。', 112, 'stop', '2026-08-12 08:05:30');

-- ============================================================
-- 模块: minimax-rag  知识库 RAG
-- ============================================================

INSERT INTO `mm_knowledge_base` (id, owner_id, tenant_id, name, description, visibility, doc_count, chunk_count, tags, created_at, updated_at) VALUES
(1, 1, 1, 'MiniMax 产品文档', 'MiniMax V6.8.2 产品功能文档', 'public', 3, 12, '产品,文档,RAG', '2026-06-01 00:00:00', '2026-08-12 00:00:00'),
(2, 2, 1, '我的私人知识库', '个人笔记与收藏', 'private', 1, 5, '私人,笔记', '2026-07-01 00:00:00', '2026-08-10 00:00:00'),
(3, 1, 1, '企业知识库', '公司内部知识共享', 'public', 5, 20, '企业,内部,知识', '2026-05-01 00:00:00', '2026-08-11 00:00:00');

INSERT INTO `mm_document` (id, kb_id, owner_id, title, source_type, source_uri, content, size_bytes, status, chunk_count, tags, created_at, updated_at) VALUES
(1, 1, 1, 'MiniMax 平台介绍',    'url',     'https://minimax.io/doc/intro',      'MiniMax 是新一代企业级 AI Agent 平台...', 2048, 'completed', 4, '产品介绍', '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
(2, 1, 1, 'RAG 使用手册',        'file',    'doc://rag-guide.pdf',               '本文档介绍如何在 MiniMax 中使用 RAG 功能...', 8192, 'completed', 6, 'RAG,手册', '2026-06-15 00:00:00', '2026-06-15 00:00:00'),
(3, 2, 2, '个人笔记：AI 趋势',   'text',    NULL,                                '我认为 AI 的发展方向是：多模态、Agent、RAG...', 512, 'completed', 2, 'AI趋势', '2026-07-01 00:00:00', '2026-07-01 00:00:00'),
(4, 3, 1, '公司规章制度',         'file',    'doc://company-rules.pdf',           '第一章 总则...', 4096, 'completed', 5, '制度,HR', '2026-05-01 00:00:00', '2026-05-01 00:00:00');

INSERT INTO `mm_document_chunk` (id, doc_id, kb_id, owner_id, chunk_index, content, dim, char_count, start_pos, end_pos, access_count, created_at) VALUES
(1, 1, 1, 1, 0, 'MiniMax 是新一代企业级 AI Agent 平台，支持 RAG、Agent 编排、多模态模型接入。', 384, 52, 0, 52, 10, '2026-06-01 00:00:00'),
(2, 1, 1, 1, 1, 'V6.8.2 版本带来：真实 LLM 路由、Agent Canvas、RAG SSE 流式推送、安全加固。', 384, 56, 52, 108, 8, '2026-06-01 00:00:00'),
(3, 2, 1, 1, 0, 'RAG (Retrieval-Augmented Generation) 是一种结合检索与生成的 AI 技术。', 384, 48, 0, 48, 15, '2026-06-15 00:00:00'),
(4, 2, 1, 1, 1, '在 MiniMax 中创建知识库：上传文档 → 自动分块 → 向量化 → 检索问答。', 384, 44, 48, 92, 12, '2026-06-15 00:00:00'),
(5, 3, 2, 2, 0, '我认为 AI 的发展方向是：多模态、Agent、RAG、端侧推理。', 384, 38, 0, 38, 3, '2026-07-01 00:00:00');

-- ============================================================
-- 模块: minimax-agent  Agent 智能体
-- ============================================================

INSERT INTO `mm_plugin` (id, name, display_name, description, version, author, category, scope, owner_id, icon, entry, plugin_type, enabled, created_at) VALUES
(1, 'web-search',     '网络搜索',    '实时搜索互联网，获取最新信息',          '1.0.0', 'MiniMax', 'search',    'global', 1, '🔍', 'SearchPlugin',     'class',  1, '2026-01-01 00:00:00'),
(2, 'calculator',     '计算器',      '执行数学计算，支持函数运算',             '1.0.0', 'MiniMax', 'tool',      'global', 1, '🧮', 'CalcPlugin',       'class',  1, '2026-01-01 00:00:00'),
(3, 'weather',       '天气查询',    '查询指定城市当前天气',                   '1.0.0', 'MiniMax', 'tool',      'global', 1, '🌤',  'WeatherPlugin',    'class',  1, '2026-01-01 00:00:00'),
(4, 'code-runner',   '代码执行',    '沙箱环境执行 Python/JS 代码',           '1.0.0', 'MiniMax', 'analysis',  'global', 1, '💻', 'CodeRunnerPlugin', 'class',  1, '2026-01-01 00:00:00'),
(5, 'image-gen',     '图片生成',    '调用 DALL-E/MJ 生成图片',               '1.0.0', 'MiniMax', 'creative',  'global', 1, '🎨', 'ImageGenPlugin',   'class',  0, '2026-01-01 00:00:00'),
(6, 'rag-retriever', 'RAG 检索',    '从知识库检索相关文档片段',              '1.0.0', 'MiniMax', 'tool',      'global', 1, '📚', 'RagPlugin',        'class',  1, '2026-01-01 00:00:00'),
(7, 'sql-query',     'SQL 查询',    '执行只读 SQL 查询（防注入）',           '1.0.0', 'MiniMax', 'analysis',  'global', 1, '🗄', 'SqlPlugin',         'class',  1, '2026-01-01 00:00:00');

INSERT INTO `mm_kg_entity` (id, user_id, name, entity_type, description, aliases, importance, source, ref_count, created_at, updated_at) VALUES
(1, 2, 'MiniMax',        'Organization', 'MiniMax AI 科技公司',       'MiniMax,稀宇科技', 10, 'manual', 3, '2026-03-01 00:00:00', '2026-03-01 00:00:00'),
(2, 2, 'RAG',            'Technology',   '检索增强生成技术',           'Retrieval-Augmented Generation', 8, 'manual', 2, '2026-03-01 00:00:00', '2026-03-01 00:00:00'),
(3, 2, 'GPT-4o',         'Model',        'OpenAI GPT-4o 多模态模型',  'gpt4o,gpt-4o', 7, 'manual', 1, '2026-03-01 00:00:00', '2026-03-01 00:00:00'),
(4, 2, 'Agent',          'Technology',   'AI 智能体 / Agent 框架',     'ai-agent,智能体', 9, 'manual', 2, '2026-03-01 00:00:00', '2026-03-01 00:00:00');

INSERT INTO `mm_kg_relation` (id, user_id, from_entity, to_entity, relation_type, description, weight, source, created_at) VALUES
(1, 2, 1, 2, 'DEVELOPS',    'MiniMax 开发了 RAG 平台', 1.0, 'manual', '2026-03-01 00:00:00'),
(2, 2, 2, 3, 'USED_BY',     'RAG 常用 GPT-4o 作为基座', 0.9, 'manual', '2026-03-01 00:00:00'),
(3, 2, 4, 2, 'USES',        'Agent 使用 RAG 做知识增强', 1.0, 'manual', '2026-03-01 00:00:00'),
(4, 2, 4, 3, 'USES',        'Agent 调用 GPT-4o 执行推理', 0.8, 'manual', '2026-03-01 00:00:00');

INSERT INTO `mm_collab_session` (id, session_id, owner_id, title, status, max_users, created_at) VALUES
(1, 1001, 2, 'AI 产品评审', 'ACTIVE', 5, '2026-08-10 10:00:00');

INSERT INTO `mm_collab_member` (id, collab_id, user_id, role, joined_at) VALUES
(1, 1, 2, 0,  '2026-08-10 10:00:00'),
(2, 1, 1, 1, '2026-08-10 10:05:00');

-- ============================================================
-- 模块: minimax-ai  AI 服务
-- ============================================================

INSERT INTO `mm_ai_chat_session` (id, session_id, user_id, username, title, model, status, intent, confidence, created_at) VALUES
(1, 'sess-ai-001', 2, 'liugl', '通用对话',  'gpt-4o-mini', 1, NULL, NULL, '2026-08-10 09:00:00'),
(2, 'sess-ai-002', 2, 'liugl', '代码助手',  'deepseek-chat', 1, 'code', 0.95, '2026-08-11 14:00:00');

INSERT INTO `mm_ai_chat_message` (id, session_id, role, content, created_at) VALUES
(1, 'sess-ai-001', 'user',     '什么是 Transformer 架构？', '2026-08-10 09:01:00'),
(2, 'sess-ai-001', 'assistant', 'Transformer 是一种基于自注意力机制的深度学习架构...', '2026-08-10 09:01:30'),
(3, 'sess-ai-002', 'user',     '写一个快速排序', '2026-08-11 14:01:00'),
(4, 'sess-ai-002', 'assistant', 'def quicksort(arr):\n    if len(arr) <= 1: return arr\n    pivot = arr[len(arr)//2]\n    return quicksort([x for x in arr if x < pivot])...', '2026-08-11 14:01:30');

INSERT INTO `mm_ai_tool` (id, code, name, category, description, enabled, builtin, impl_type, impl_value, input_schema, created_at) VALUES
(1, 'web_search', '网络搜索',  'search',     '实时搜索互联网',        1, 1, 'http',     'https://api.search.minimax.io/search', '{"query":"string"}', '2026-01-01 00:00:00'),
(2, 'calculator', '科学计算',  'tool',       '数学计算',              1, 1, 'class',    'com.minimax.ai.tool.Calculator',       '{"expr":"string"}',   '2026-01-01 00:00:00'),
(3, 'rag',        '知识检索',  'retrieval',  'RAG 知识库检索',        1, 1, 'class',    'com.minimax.ai.tool.RagTool',         '{"query":"string"}',  '2026-01-01 00:00:00');

INSERT INTO `mm_billing_record` (id, record_id, user_id, record_type, amount_cents, currency, status, description, created_at) VALUES
(1, 'bill-001', 2, 'CHAT', -50, 'CNY', 'COMPLETED', 'GPT-4o mini 聊天消费', '2026-08-01 00:00:00'),
(2, 'bill-002', 2, 'API_CALL', -30, 'CNY', 'COMPLETED', 'API 调用消费', '2026-08-02 00:00:00'),
(3, 'bill-003', 1, 'SUBSCRIPTION', 9900, 'CNY', 'COMPLETED', 'Pro 月费订阅', '2026-08-01 00:00:00');

INSERT INTO `mm_audit_log` (id, trace_id, user_id, username, action, resource_type, resource_id, method, path, response_status, created_at) VALUES
(1, 'trace-001', 2, 'liugl', 'LOGIN',     'Auth',    NULL,       'POST', '/api/v1/auth/login',     200, '2026-08-10 09:00:00'),
(2, 'trace-002', 2, 'liugl', 'CHAT_SEND', 'Chat',    NULL,       'POST', '/api/v1/chat/stream',    200, '2026-08-10 09:01:00'),
(3, 'trace-003', 1, 'admin', 'MODEL_CREATE', 'Model', '1',       'POST', '/api/v1/admin/models',  201, '2026-08-10 10:00:00');

-- ============================================================
-- 模块: minimax-monitor  监控告警
-- ============================================================

INSERT INTO `mm_alert_channel` (id, name, channel_type, target, config, enabled, priority, description, template, created_by, created_at) VALUES
(1, '钉钉告警群', 'dingtalk', 'https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN', '{"secret":"SEC...","atMobiles":[]}', 1, 1, '生产环境钉钉告警', '【告警】${severity} ${name}: ${message}', 1, '2026-01-01 00:00:00'),
(2, '管理员邮箱', 'email',    'admin@minimax.io', '{"smtp":"smtp.example.com","from":"alert@minimax.io"}', 1, 2, '邮件告警通知', '告警通知: ${name}', 1, '2026-01-01 00:00:00'),
(3, '企业微信',   'wechat',   'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY', '{}', 0, 3, '企业微信备用告警', NULL, 1, '2026-01-01 00:00:00');

INSERT INTO `mm_alert_rule` (id, name, description, metric_name, service, operator, threshold, severity, cooldown_minutes, enabled, tags, notify_channel, created_at) VALUES
(1, 'CPU 过高告警',   'CPU 使用率超过 80%',          'cpu_usage',    'cs-auth',    '>', 80.0,  'WARNING', 10, 1, 'prod',      '1',     '2026-01-01 00:00:00'),
(2, '内存告警',       '内存使用超过 90%',            'memory_usage', 'cs-auth',    '>', 90.0,  'CRITICAL',5, 1, 'prod',      '1,2',   '2026-01-01 00:00:00'),
(3, 'API 延迟过高',   'P99 响应时间超过 3 秒',       'api_latency_p99', 'gateway', '>', 3000.0, 'WARNING', 15, 1, 'prod,api', '2',     '2026-01-01 00:00:00'),
(4, '错误率告警',     '5xx 错误率超过 5%',            'error_rate_5xx', 'gateway', '>', 5.0,   'CRITICAL',5, 1, 'prod',      '1,2',   '2026-01-01 00:00:00'),
(5, 'Token 配额不足', 'Token 配额使用超过 80%',       'quota_usage',  'cs-model',   '>', 80.0,  'WARNING', 60, 1, 'quota',    '1',     '2026-01-01 00:00:00');

INSERT INTO `mm_alert_event` (id, rule_id, channel_id, severity, message, metric_value, triggered_at, acknowledged, acknowledged_by, acknowledged_at, resolved_at) VALUES
(1, 1, 1, 'WARNING', 'CPU 使用率 85%，触发告警', 85.0, '2026-08-10 14:00:00', 1, 'admin', '2026-08-10 14:05:00', NULL),
(2, 3, 2, 'WARNING', 'API P99 延迟 3.5s，偏高', 3500.0, '2026-08-11 10:00:00', 0, NULL, NULL, NULL);

-- ============================================================
-- 模块: minimax-pipeline  流程编排
-- ============================================================

INSERT INTO `mm_pipeline_workflow` (id, name, description, definition, version, status, create_by, create_time, update_time) VALUES
(1, 'RAG 问答流程', '{"nodes":[{"id":"1","type":"input","label":"用户问题"},{"id":"2","type":"rag","label":"知识检索"},{"id":"3","type":"llm","label":"生成回答"},{"id":"4","type":"output","label":"返回结果"}],"edges":[{"from":"1","to":"2"},{"from":"2","to":"3"},{"from":"3","to":"4"}]}', 1, 1, 1, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
(2, 'Agent 任务流', '{"nodes":[{"id":"1","type":"input"},{"id":"2","type":"planner"},{"id":"3","type":"executor"},{"id":"4","type":"critic"},{"id":"5","type":"output"}],"edges":[{"from":"1","to":"2"},{"from":"2","to":"3"},{"from":"3","to":"4"},{"from":"4","to":"5"}]}', 1, 1, 1, '2026-07-01 00:00:00', '2026-07-01 00:00:00');

INSERT INTO `mm_skill_approval` (id, task_id, user_id, username, tool_name, risk_level, goal, tool_params, status, approver_id, approver_name, reason, created_at, updated_at) VALUES
(1, 'skill-001', 2, 'liugl', 'rag.retrieve', 'HIGH', '需要跨知识库检索', NULL, 'APPROVED', 1, 'admin', '已通过', '2026-08-01 00:00:00', '2026-08-01 00:00:00'),
(2, 'skill-002', 2, 'liugl', 'code.execute', 'CRITICAL', '需要执行用户代码', NULL, 'PENDING', NULL, NULL, NULL, '2026-08-10 00:00:00', '2026-08-10 00:00:00');

-- ============================================================
-- 模块: minimax-prompt  提示词模板
-- ============================================================

INSERT INTO `mm_prompt_template` (id, name, description, category, content, variables, creator_id, creator_name, is_public, use_count, created_at, updated_at) VALUES
(1, '通用助手',      '默认 AI 助手提示词',        'system',   '你是一个专业、友好的 AI 助手。请根据用户问题给出准确、简洁的回答。\n当前时间: ${current_time}\n用户语言: ${language}', 'current_time,language', 1, 'admin', 1, 50, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(2, '代码助手',      '编程任务专用提示词',         'system',   '你是一个资深程序员。请帮助用户解决编程问题。\n语言偏好: ${language}\n代码风格: ${style}', 'language,style', 1, 'admin', 1, 30, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(3, 'RAG 检索增强',  'RAG 场景下的 LLM 提示词',   'system',   '基于以下上下文信息回答用户问题。\n\n上下文:\n${context}\n\n问题: ${question}\n\n要求: 答案必须仅基于提供的上下文，不要编造信息。', 'context,question', 1, 'admin', 1, 20, '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- ============================================================
-- 模块: minimax-admin  审计日志
-- ============================================================

INSERT INTO `mm_admin_audit_log` (id, actor_id, actor_name, action, resource_type, resource_id, detail, result, ip, created_at) VALUES
(1, 1, 'admin', 'USER_CREATE',   'User',    '2',   '{"username":"liugl","email":"liugl@minimax.io"}',       'SUCCESS', '127.0.0.1', '2026-01-02 00:00:00'),
(2, 1, 'admin', 'MODEL_CREATE',  'Model',   '1',   '{"modelCode":"gpt-4o","providerId":1}',                'SUCCESS', '127.0.0.1', '2026-01-01 00:00:00'),
(3, 1, 'admin', 'RATE_LIMIT',    'Model',   '1',   '{"code":"gpt-4o","capacity":100,"refillPerMin":60}',   'SUCCESS', '127.0.0.1', '2026-01-10 00:00:00'),
(4, 1, 'admin', 'KB_CREATE',     'KnowledgeBase', '1', '{"name":"MiniMax 产品文档"}',                      'SUCCESS', '127.0.0.1', '2026-06-01 00:00:00'),
(5, 1, 'admin', 'PLUGIN_ENABLE', 'Plugin',  '4',   '{"plugin":"code-runner","enabled":true}',             'SUCCESS', '127.0.0.1', '2026-08-01 00:00:00');

-- ============================================================
-- 模块: minimax-ws  WebSocket 协作
-- ============================================================

INSERT INTO `mm_collab_room` (id, name, owner_id, status, max_participants, created_at) VALUES
(1, 'AI 评审室', 2, 1, 5, '2026-08-10 10:00:00');

INSERT INTO `mm_collab_participant` (id, room_id, user_id, role, joined_at) VALUES
(1, 1, 2, 'owner',  '2026-08-10 10:00:00'),
(2, 1, 1, 'member', '2026-08-10 10:05:00');

INSERT INTO `mm_collab_message` (id, room_id, user_id, content, message_type, created_at) VALUES
(1, 1, 2, '开始评审 MiniMax V6.8.2', 'text', '2026-08-10 10:01:00'),
(2, 1, 1, 'RAG SSE 流式效果很好', 'text', '2026-08-10 10:02:00');

-- ============================================================
-- 模块: minimax-analytics  数据分析
-- ============================================================

INSERT INTO `mm_analytics_datasource` (id, name, type, connection_config, status, created_at) VALUES
(1, 'MySQL 主库', 'mysql', '{"host":"localhost","port":3306,"database":"minimax"}', 1, '2026-01-01 00:00:00'),
(2, 'Elasticsearch', 'elasticsearch', '{"host":"localhost","port":9200}', 1, '2026-01-01 00:00:00');

INSERT INTO `mm_analytics_report` (id, name, description, report_type, config, created_by, created_at) VALUES
(1, '日活用户报表', '统计每日活跃用户数', 'daily_active_users', '{"metrics":["active_users","new_users"],"granularity":"day"}', 1, '2026-01-01 00:00:00'),
(2, 'Token 消耗报表', 'Token 使用趋势', 'token_consumption', '{"metrics":["input_tokens","output_tokens"]}', 1, '2026-01-01 00:00:00');

-- ============================================================
-- 模块: minimax-pipeline-fn  函数扩展
-- ============================================================

INSERT INTO `mm_function_tool` (id, name, description, category, input_schema, output_schema, enabled, builtin, created_at) VALUES
(1, 'send_email',   '发送邮件',         'notification', '{"to":"string","subject":"string","body":"string"}', '{"sent":"boolean"}', 1, 1, '2026-01-01 00:00:00'),
(2, 'create_issue', '创建工单',         'workflow',    '{"title":"string","desc":"string","priority":"string"}', '{"id":"string"}', 1, 1, '2026-01-01 00:00:00'),
(3, 'webhook_call', 'Webhook 调用',     'http',        '{"url":"string","method":"string","body":"object"}', '{"status":200}', 1, 1, '2026-01-01 00:00:00');

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 补充：训练任务 + 监控告警事件
-- ============================================================

INSERT INTO `mm_training_task` (id, user_id, model_name, corpus_path, n_layer, n_head, n_embd, block_size, max_iters, batch_size, learning_rate, status, progress, current_iter, created_at) VALUES
(1, 2, 'MiniTransformer-v1', '/data/corpus/ai-text-100k.txt', 4, 4, 128, 128, 1000, 16, 0.05, 'COMPLETED', 100, 1000, '2026-07-01 00:00:00'),
(2, 2, 'MiniTransformer-v2', '/data/corpus/ai-text-500k.txt', 6, 6, 256, 256, 2000, 32, 0.03, 'TRAINING', 45, 900, '2026-08-01 00:00:00');

INSERT INTO `mm_training_metric` (id, task_id, iter, loss, accuracy, progress, lr, gpu_util, vram_gb, created_at) VALUES
(1, 1, 100,  2.453, 0.123, 10, '0.05', 80, 6.5, '2026-07-01 01:00:00'),
(2, 1, 500,  1.234, 0.456, 50, '0.05', 85, 7.0, '2026-07-01 05:00:00'),
(3, 1, 1000, 0.567, 0.789, 100, '0.005', 85, 7.0, '2026-07-02 00:00:00'),
(4, 2, 100,  1.890, 0.345, 5, '0.03', 78, 6.2, '2026-08-01 01:00:00'),
(5, 2, 900,  0.890, 0.678, 45, '0.03', 82, 6.8, '2026-08-10 00:00:00');

INSERT INTO `mm_metric_snapshot` (id, service, metric_name, metric_value, recorded_at) VALUES
(1, 'cs-auth',    'cpu_usage',    45.5, '2026-08-12 00:00:00'),
(2, 'cs-auth',    'memory_usage', 62.3, '2026-08-12 00:00:00'),
(3, 'cs-chat',    'cpu_usage',    72.1, '2026-08-12 00:00:00'),
(4, 'cs-chat',    'memory_usage', 55.0, '2026-08-12 00:00:00'),
(5, 'gateway',    'qps',         125.0, '2026-08-12 00:00:00'),
(6, 'gateway',    'error_rate',   0.5,  '2026-08-12 00:00:00'),
(7, 'cs-model',   'active_tokens', 850000, '2026-08-12 00:00:00'),
(8, 'cs-model',   'quota_usage',  38.5, '2026-08-12 00:00:00');

INSERT INTO `mm_sensitive_word` (id, word, category, level, action, enabled, created_at) VALUES
(1, '作弊',     '违规', 'HIGH', 'BLOCK',  1, '2026-01-01 00:00:00'),
(2, '作弊软件', '违规', 'HIGH', 'BLOCK',  1, '2026-01-01 00:00:00'),
(3, '政治敏感', '合规', 'MEDIUM', 'REVIEW', 1, '2026-01-01 00:00:00'),
(4, '暴力内容', '合规', 'MEDIUM', 'REVIEW', 1, '2026-01-01 00:00:00'),
(5, '色情',     '合规', 'HIGH', 'BLOCK',  1, '2026-01-01 00:00:00');
