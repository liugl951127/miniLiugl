-- ============================================================
-- MiniMax Platform 全量种子数据 SQL (V7.2)
-- 生成时间: 2026-08-22
-- 包含: 用户/角色/租户/API Key/模型/工具/敏感词/告警/RAG/限流/演示电商
-- 配套文件: minimax-schema.sql (表结构)
-- 字符集: utf8mb4 / 模式: MySQL
-- 顺序: sys_user → sys_role → sys_user_role → tenant → user_api_key →
--       model_provider → model_config → ai_tool → ai_intent_keyword →
--       sensitive_word → alert_channel → alert_rule → knowledge_base →
--       rate_limit_rule → prompt_template → user_preferences →
--       memory_user_pref → memory_long_term → kg_entity → kg_relation →
--       db_data_source (与 data_source alias) → demo_* (电商)
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- [minimax-auth] sys_user
INSERT INTO sys_user (id, username, password, nickname, email, phone, avatar, gender, status, lastLoginIp, lastLoginAt, tenantId, remark, wechatOpenid, wechatUnionid, wechatNickname, wechatAvatar, wechatBoundAt, qqOpenid, qqUnionid, qqNickname, qqAvatar, qqBoundAt, alipayOpenid, alipayUserId, alipayNickname, alipayAvatar, alipayBoundAt, createdBy, createdAt, updatedBy, updatedAt, deleted) VALUES
    (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Administrator', 'admin@minimax.com', '13800000000', '', 1, 1, '127.0.0.1', DEFAULT, 1, 'System admin', DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, 1),
    (2, 'alice88', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '林小燕', 'linxy@minimax.com', '13800001001', '', 0, 1, DEFAULT, DEFAULT, 1, 'Demo user', DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, 1),
    (3, 'bob_dev', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张建国', 'zhangjg@minimax.com', '13800001002', '', 1, 1, DEFAULT, DEFAULT, 1, 'Developer', DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT);

-- [minimax-auth] sys_role
INSERT INTO sys_role (id, code, name, description, sort, enabled, createdAt, updatedAt, deleted) VALUES
    (1, 'SUPER_ADMIN', '超级管理员', '拥有全部权限', 1, 1, DEFAULT, DEFAULT, 0),
    (2, 'ADMIN', '管理员', '系统管理权限', 2, 1, DEFAULT, DEFAULT, 0),
    (3, 'USER', '普通用户', '基础用户权限', 3, 1, DEFAULT, DEFAULT, 0),
    (4, 'VIEWER', '访客', '只读权限', 4, 1, DEFAULT, DEFAULT, 0);

-- [minimax-auth] sys_user_role
INSERT INTO sys_user_role (userId, roleId) VALUES
    (1, 1),
    (1, 2),
    (2, 3),
    (3, 3);

-- [minimax-auth] tenant
INSERT INTO tenant (id, code, name, plan, status, maxUsers, maxModels, qpsLimit, monthlyQuota, usedQuota, expireAt, contactEmail, contactPhone, remark, createdAt, updatedAt, deleted) VALUES
    (1, DEFAULT, '默认租户', 'ENTERPRISE', 1, 100, 10, 100, 1000000000, 0, DEFAULT, 'admin@minimax.com', '13800000000', 'Platform default tenant', DEFAULT, DEFAULT, 0),
    (2, 'DEMO', '演示租户', 'BASIC', 1, 10, 3, 10, 100000, 0, DEFAULT, 'demo@minimax.com', '13800000001', 'Demo tenant', DEFAULT, DEFAULT, 0);

-- [minimax-auth] user_api_key
INSERT INTO user_api_key (id, userId, name, keyHash, keyPrefix, scopes, expiresAt, lastUsedAt, useCount, enabled, createdAt, updatedAt, deleted) VALUES (1, 1, 'Admin Key', '$2a$10$dummyhash_admin_key_1234567890123456789012345678901234567890', 'mmx_', 'all', DEFAULT, DEFAULT, 0, 1, DEFAULT, DEFAULT, 0);

-- [minimax-model] model_provider
INSERT INTO model_provider (id, code, name, baseUrl, apiKey, protocol, enabled, sort, description, createdAt, updatedAt, deleted) VALUES
    (1, 'OPENAI', 'OpenAI', 'https://api.openai.com', '', 'OPENAI', 1, 1, 'OpenAI GPT series', DEFAULT, DEFAULT, 0),
    (2, 'DEEPSEEK', 'DeepSeek', 'https://api.deepseek.com', '', 'OPENAI', 1, 2, 'DeepSeek series', DEFAULT, DEFAULT, 0),
    (3, 'DASHSCOPE', '阿里通义', 'https://dashscope.aliyuncs.com', '', 'OPENAI', 1, 3, 'Alibaba Tongyi', DEFAULT, DEFAULT, 0),
    (4, 'ZHIPU', '智谱AI', 'https://open.bigmodel.cn', '', 'OPENAI', 1, 4, 'Zhipu GLM series', DEFAULT, DEFAULT, 0);

-- [minimax-model] model_config
INSERT INTO model_config (id, providerId, modelCode, displayName, maxContext, maxOutput, inputPrice, outputPrice, supportsVision, supportsTools, supportsStream, enabled, sort, description, createdAt, updatedAt, deleted) VALUES
    (1, 1, 'gpt-4o', 'GPT-4o', 128000, 16384, 2.5, 10.0, 1, 1, 1, 1, 1, 'OpenAI GPT-4o', DEFAULT, DEFAULT, 0),
    (2, 1, 'gpt-4o-mini', 'GPT-4o Mini', 128000, 16384, 0.15, 0.6, 1, 1, 1, 1, 2, 'OpenAI GPT-4o Mini', DEFAULT, DEFAULT, 0),
    (3, 2, 'deepseek-chat', 'DeepSeek V3', 64000, 8192, 0.0, 2.0, 1, 1, 1, 1, 3, 'DeepSeek V3', DEFAULT, DEFAULT, 0),
    (4, 2, 'deepseek-reasoner', 'DeepSeek R1', 64000, 8192, 2.0, 8.0, 0, 1, 1, 1, 4, 'DeepSeek R1 reasoning model', DEFAULT, DEFAULT, 0),
    (5, 3, 'qwen-plus', '通义千问Plus', 128000, 8192, 0.6, 2.0, 1, 1, 1, 1, 5, 'Alibaba Qwen Plus', DEFAULT, DEFAULT, 0),
    (6, 4, 'glm-4', 'GLM-4', 128000, 8192, 1.0, 2.0, 1, 1, 1, 1, 6, 'Zhipu GLM-4', DEFAULT, DEFAULT, 0);

-- [minimax-ai] ai_tool
INSERT INTO ai_tool (id, code, name, category, description, icon, enabled, builtin, inputSchema, outputSchema, defaultConfig, implType, implValue, rateLimit, timeoutSeconds, roleRequired, tags, version, author, createdBy, createdAt, updatedAt, status, deleted) VALUES
    (1, 'web_search', '网页搜索', 'search', '联网搜索互联网内容', '', 1, 1, '{"type":"object","properties":{"query":{"type":"string","description":"搜索关键词"}},"required":["query"]}', '', '{"maxResults":5}', 'HTTP', 'https://api.example.com/search', 10, 30, DEFAULT, 'search,web', '1.0', 'MiniMax', 1, DEFAULT, DEFAULT, 1, 0),
    (2, 'web_fetch', '网页抓取', 'search', '抓取指定URL的网页内容', '', 1, 1, '{"type":"object","properties":{"url":{"type":"string"}},"required":["url"]}', '', '{}', 'HTTP', 'https://api.example.com/fetch', 5, 30, DEFAULT, 'fetch,web', '1.0', 'MiniMax', 1, DEFAULT, DEFAULT, 1, 0),
    (3, 'calculator', '计算器', 'util', '数学表达式计算', '', 1, 1, '{"type":"object","properties":{"expression":{"type":"string"}},"required":["expression"]}', '', '{}', 'SCRIPT', '', 10, 5, DEFAULT, 'math,calc', '1.0', 'MiniMax', 1, DEFAULT, DEFAULT, 1, 0),
    (4, 'code_interpreter', '代码执行', 'code', '安全执行Python代码片段', '', 1, 1, '{"type":"object","properties":{"code":{"type":"string"},"language":{"type":"string"}},"required":["code"]}', '', '{"timeout":30}', 'DOCKER', '', 20, 60, DEFAULT, 'python,code', '1.0', 'MiniMax', 1, DEFAULT, DEFAULT, 1, 0),
    (5, 'file_read', '文件读取', 'file', '读取服务器文件内容', '', 1, 1, '{"type":"object","properties":{"path":{"type":"string"}},"required":["path"]}', '', '{}', 'LOCAL', '', 10, 10, DEFAULT, 'file,read', '1.0', 'MiniMax', 1, DEFAULT, DEFAULT, 1, 0);

-- [minimax-ai] ai_intent_keyword
INSERT INTO ai_intent_keyword (id, intent, keyword, weight, isRegex, enabled, language, remark, createdAt, updatedAt) VALUES
    (1, 'chat', '你好', 10, 0, 1, 'zh', '问候语', DEFAULT, DEFAULT),
    (2, 'chat', '在吗', 8, 0, 1, 'zh', '问候语', DEFAULT, DEFAULT),
    (3, 'chat', 'help', 8, 0, 1, 'en', 'help keyword', DEFAULT, DEFAULT),
    (4, 'search', '搜索', 10, 0, 1, 'zh', '搜索意图', DEFAULT, DEFAULT),
    (5, 'search', 'find', 8, 0, 1, 'en', 'search keyword', DEFAULT, DEFAULT),
    (6, 'code', '写代码', 10, 0, 1, 'zh', '代码生成', DEFAULT, DEFAULT),
    (7, 'code', '代码', 8, 0, 1, 'zh', '代码相关', DEFAULT, DEFAULT),
    (8, 'math', '计算', 10, 0, 1, 'zh', '数学计算', DEFAULT, DEFAULT),
    (9, 'image', '画', 10, 0, 1, 'zh', '图片生成', DEFAULT, DEFAULT),
    (10, 'image', '生成图片', 10, 0, 1, 'zh', '图片生成', DEFAULT, DEFAULT);

-- [minimax-ai] sensitive_word
INSERT INTO sensitive_word (id, word, category, level, action, enabled, createdAt) VALUES
    (1, '政治敏感词', 'political', 'HIGH', 'REJECT', 1, DEFAULT),
    (2, '色情词汇', 'porn', 'HIGH', 'REJECT', 1, DEFAULT),
    (3, '暴力词汇', 'violence', 'MEDIUM', 'REVIEW', 1, DEFAULT),
    (4, '广告词汇', 'ad', 'LOW', 'REPLACE', 1, DEFAULT);

-- [minimax-monitor] alert_channel
INSERT INTO alert_channel (id, name, channelType, type, target, config, enabled, priority, description, template, createdBy, createdAt) VALUES
    (1, '钉钉告警', 'DINGTALK', 'DINGTALK', 'https://oapi.dingtalk.com/robot/send', '', 1, 1, '钉钉群通知', DEFAULT, 1, DEFAULT),
    (2, '邮件告警', 'EMAIL', 'EMAIL', 'admin@minimax.com', '', 1, 2, '管理员邮件', DEFAULT, 1, DEFAULT),
    (3, 'Slack告警', 'SLACK', 'SLACK', 'https://hooks.slack.com/...', '', 1, 3, 'Slack频道', DEFAULT, 1, DEFAULT);

-- [minimax-monitor] alert_rule
INSERT INTO alert_rule (id, name, description, metricName, service, operator, threshold, severity, cooldownMinutes, enabled, tags, notifyChannel, silencedUntil, sessionId, createdAt, updatedAt) VALUES
    (1, 'CPU使用率告警', 'CPU使用率超过80%', 'cpu.usage', 'minimax-ai', '>', 80.0, 'WARNING', 5, 1, 'system', '1,2', DEFAULT, DEFAULT, DEFAULT, DEFAULT),
    (2, '内存使用率告警', '内存使用率超过85%', 'memory.usage', 'minimax-ai', '>', 85.0, 'WARNING', 5, 1, 'system', '1,2', DEFAULT, DEFAULT, DEFAULT, DEFAULT),
    (3, '错误率告警', '5分钟内错误率超过5%', 'error.rate', 'minimax-ai', '>', 5.0, 'CRITICAL', 2, 1, 'error', '1,2', DEFAULT, DEFAULT, DEFAULT, DEFAULT);

-- [minimax-rag] knowledge_base
INSERT INTO knowledge_base (id, ownerId, tenantId, name, description, visibility, docCount, chunkCount, tags, createdAt, updatedAt, deleted) VALUES (1, 1, 1, '产品知识库', 'MiniMax产品文档知识库', 'PRIVATE', 0, 0, 'product,docs', DEFAULT, DEFAULT, 0);

-- [minimax-common] rate_limit_rule
INSERT INTO rate_limit_rule (id, scope, rule_key, description, capacity, refill_tokens, period_seconds, enabled, priority, created_at, updated_at, deleted) VALUES
    (1, 'USER', '', '用户级限流', 100, 10, 60, 1, 1, DEFAULT, DEFAULT, 0),
    (2, 'API_KEY', '', 'API Key限流', 1000, 100, 60, 1, 2, DEFAULT, DEFAULT, 0),
    (3, 'IP', '', 'IP级限流', 200, 20, 60, 1, 3, DEFAULT, DEFAULT, 0);

-- ============================================================
-- 演示数据
-- ============================================================
-- [demo] demo_user
INSERT INTO demo_user (userId, userName, realName, email, phone, gender, age, city, level, balance, status, createdAt, updatedAt) VALUES
    (1, 'alice88', '林小燕', 'linxy@email.com', '13800001001', 'F', 28, '深圳', 4, 2580.5, 'ACTIVE', DEFAULT, DEFAULT),
    (2, 'bob_dev', '张建国', 'zhangjg@email.com', '13800001002', 'M', 35, '北京', 5, 12000.0, 'ACTIVE', DEFAULT, DEFAULT),
    (3, 'charlie_z', '赵海涛', 'zhaoht@email.com', '13800001003', 'M', 42, '上海', 3, 340.0, 'ACTIVE', DEFAULT, DEFAULT),
    (4, 'diana_pm', '陈美玲', 'chenml@email.com', '13800001004', 'F', 31, '广州', 4, 5600.0, 'ACTIVE', DEFAULT, DEFAULT),
    (5, 'evan_art', '王海波', 'wanghb@email.com', '13800001005', 'M', 26, '深圳', 3, 2100.0, 'ACTIVE', DEFAULT, DEFAULT),
    (6, 'fiona_fin', '刘晓芳', 'liuxf@email.com', '13800001006', 'F', 38, '上海', 4, 8800.0, 'ACTIVE', DEFAULT, DEFAULT),
    (7, 'george_pm', '陈志强', 'chenzq@email.com', '13800001007', 'M', 33, '北京', 3, 4200.0, 'ACTIVE', DEFAULT, DEFAULT),
    (8, 'helen_mkt', '周雅琴', 'zhouyq@email.com', '13800001008', 'F', 29, '杭州', 2, 1500.0, 'ACTIVE', DEFAULT, DEFAULT),
    (9, 'ivan_ops', '吴海明', 'wuhm@email.com', '13800001009', 'M', 36, '广州', 4, 6800.0, 'ACTIVE', DEFAULT, DEFAULT),
    (10, 'julia_hr', '孙丽娟', 'sunlj@email.com', '13800001010', 'F', 32, '成都', 3, 3900.0, 'ACTIVE', DEFAULT, DEFAULT),
    (11, 'kevin_dev', '郑伟', 'zhengw@email.com', '13800001011', 'M', 27, '深圳', 2, 2300.0, 'ACTIVE', DEFAULT, DEFAULT),
    (12, 'lisa_sal', '李娜', 'lina@email.com', '13800001012', 'F', 34, '北京', 4, 9200.0, 'ACTIVE', DEFAULT, DEFAULT),
    (13, 'mike_ops', '刘东明', 'liudm@email.com', '13800001013', 'M', 36, '武汉', 4, 9200.0, 'ACTIVE', DEFAULT, DEFAULT),
    (14, 'nancy_des', '陈雪琴', 'chenxq@email.com', '13800001014', 'F', 25, '杭州', 2, 450.0, 'ACTIVE', DEFAULT, DEFAULT),
    (15, 'oscar_fin', '马俊峰', 'majf@email.com', '13800001015', 'M', 40, '北京', 5, 156000.0, 'ACTIVE', DEFAULT, DEFAULT),
    (16, 'peggy_sal', '李秀英', 'lixy@email.com', '13800001016', 'F', 32, '上海', 3, 2100.0, 'ACTIVE', DEFAULT, DEFAULT),
    (17, 'quinn_dev', '罗志远', 'luozhy@email.com', '13800001017', 'M', 29, '深圳', 3, 3200.0, 'ACTIVE', DEFAULT, DEFAULT),
    (18, 'rachel_cs', '张小敏', 'zhangxm@email.com', '13800001018', 'F', 27, '成都', 2, 980.0, 'ACTIVE', DEFAULT, DEFAULT),
    (19, 'steve_pm', '杨建强', 'yangjq@email.com', '13800001019', 'M', 44, '广州', 4, 7500.0, 'ACTIVE', DEFAULT, DEFAULT),
    (20, 'tina_data', '周若兰', 'zhou_rl@email.com', '13800001020', 'F', 31, '杭州', 3, 4100.0, 'FROZEN', DEFAULT, DEFAULT);

-- [demo] demo_category
INSERT INTO demo_category (categoryId, categoryName, parentId, sortOrder, iconUrl, description, createdAt) VALUES
    (1, '手机数码', DEFAULT, 1, '/icons/phone.png', '手机、耳机、充电器等', DEFAULT),
    (2, '电脑办公', DEFAULT, 2, '/icons/laptop.png', '笔记本、台式机、键鼠等', DEFAULT),
    (3, '箱包皮具', DEFAULT, 3, '/icons/bag.png', '旅行箱、双肩包、手提包等', DEFAULT),
    (4, '食品饮料', DEFAULT, 4, '/icons/food.png', '零食、生鲜、饮料等', DEFAULT),
    (5, '美妆护肤', DEFAULT, 5, '/icons/beauty.png', '护肤品、化妆品、香水等', DEFAULT),
    (6, '家用电器', DEFAULT, 6, '/icons/appliance.png', '电饭煲、空气炸锅、吸尘器等', DEFAULT),
    (7, '服饰内衣', DEFAULT, 7, '/icons/clothing.png', '男装、女装、童装等', DEFAULT),
    (8, '母婴用品', DEFAULT, 8, '/icons/baby.png', '婴儿纸尿裤、湿巾、奶粉等', DEFAULT);

-- [demo] demo_product
INSERT INTO demo_product (productId, productName, categoryId, brand, price, cost, stock, soldCount, rating, reviewCount, tags, status, createdAt, updatedAt) VALUES
    (1, 'iPhone 15 Pro 256G 深空黑', 1, 'Apple', 8999.0, 7500.0, 120, 890, 4.9, 4520, '手机,旗舰,5G', 'ONLINE', DEFAULT, DEFAULT),
    (2, 'AirPods Pro 2 无线降噪耳机', 1, 'Apple', 1899.0, 1400.0, 350, 2100, 4.85, 8760, '耳机,降噪,无线', 'ONLINE', DEFAULT, DEFAULT),
    (3, '小米14 Ultra 影像旗舰 512G', 1, '小米', 6499.0, 5200.0, 200, 560, 4.78, 1230, '手机,旗舰,徕卡', 'ONLINE', DEFAULT, DEFAULT),
    (4, 'MacBook Pro 14寸 M3 Pro 18+512', 2, 'Apple', 16999.0, 14500.0, 80, 320, 4.93, 1890, '笔记本,苹果,专业', 'ONLINE', DEFAULT, DEFAULT),
    (5, 'ThinkPad X1 Carbon 14寸超薄本', 2, '联想', 8999.0, 7200.0, 150, 480, 4.72, 980, '笔记本,商务,轻薄', 'ONLINE', DEFAULT, DEFAULT),
    (6, '新秀丽 商务拉杆箱 20寸', 3, '新秀丽', 899.0, 450.0, 300, 1800, 4.8, 5600, '箱包,商务,拉杆箱', 'ONLINE', DEFAULT, DEFAULT),
    (7, '戴睿 商务双肩包 笔记本电脑包', 3, '戴睿', 299.0, 120.0, 600, 2300, 4.55, 7800, '箱包,商务,电脑包', 'ONLINE', DEFAULT, DEFAULT),
    (8, '阳澄湖大闸蟹 公4两母3两 8只装', 4, '阳澄湖', 568.0, 280.0, 100, 3500, 4.88, 12000, '大闸蟹,生鲜,海鲜', 'ONLINE', DEFAULT, DEFAULT),
    (9, '雅诗兰黛 第七代小棕瓶精华 50ml', 5, '雅诗兰黛', 899.0, 560.0, 200, 1200, 4.87, 8900, '精华,护肤,抗老', 'ONLINE', DEFAULT, DEFAULT),
    (10, '兰蔻 小黑瓶精华肌底液 50ml', 5, '兰蔻', 1080.0, 680.0, 140, 780, 4.87, 1900, '精华,护肤,法国', 'ONLINE', DEFAULT, DEFAULT),
    (11, '飞利浦 空气炸锅 5L大容量', 6, '飞利浦', 699.0, 380.0, 250, 1100, 4.72, 3100, '空气炸锅,厨房,健康', 'ONLINE', DEFAULT, DEFAULT),
    (12, 'iPad Pro 12.9寸 M2 256G', 1, 'Apple', 8999.0, 7200.0, 80, 650, 4.91, 3200, '平板,苹果,办公', 'ONLINE', DEFAULT, DEFAULT),
    (13, '索尼 WH-1000XM5 头戴降噪耳机', 1, '索尼', 2699.0, 1900.0, 180, 920, 4.83, 2300, '耳机,降噪,头戴', 'ONLINE', DEFAULT, DEFAULT),
    (14, '海天 酱油 味极鲜 1.9L*2瓶装', 4, '海天', 35.8, 15.0, 2000, 12000, 4.6, 35000, '酱油,调味,厨房', 'ONLINE', DEFAULT, DEFAULT),
    (15, 'Babycare 婴儿湿巾 80抽*12包整箱', 8, 'Babycare', 89.9, 38.0, 1500, 7800, 4.75, 21000, '湿巾,婴儿,整箱', 'ONLINE', DEFAULT, DEFAULT),
    (16, 'SK-II 神仙水 230ml 精华液', 5, 'SK-II', 1199.0, 750.0, 100, 890, 4.88, 12000, '精华,护肤,日本', 'ONLINE', DEFAULT, DEFAULT),
    (17, '戴森 V12 无线吸尘器', 6, '戴森', 4990.0, 3500.0, 50, 320, 4.9, 1500, '吸尘器,无线,家电', 'ONLINE', DEFAULT, DEFAULT),
    (18, 'Nintendo Switch OLED 国行版', 1, '任天堂', 2299.0, 1700.0, 200, 2500, 4.85, 8900, '游戏机,掌机,娱乐', 'ONLINE', DEFAULT, DEFAULT),
    (19, '阿迪达斯 男子运动T恤 黑色 XL', 7, '阿迪达斯', 199.0, 60.0, 500, 3200, 4.6, 15000, 'T恤,运动,男装', 'ONLINE', DEFAULT, DEFAULT),
    (20, '费列罗榛果威化巧克力 32粒装', 4, '费列罗', 89.0, 38.0, 1000, 8000, 4.7, 25000, '巧克力,零食,礼盒', 'ONLINE', DEFAULT, DEFAULT);

-- [demo] demo_order
INSERT INTO demo_order (orderId, userId, orderStatus, totalAmount, discountAmount, payAmount, payMethod, payTime, shippingFee, receiverName, receiverPhone, receiverAddress, remark, orderDate, createdAt, updatedAt) VALUES
    ('ORD20250801001', 1, 'COMPLETED', 1899.0, 100.0, 1799.0, 'WECHAT', '2025-08-01 10:23:00', 0.0, '林小燕', '13800001001', '广东省深圳市南山区科技园', '', '2025-08-01', DEFAULT, DEFAULT),
    ('ORD20250801002', 2, 'COMPLETED', 8999.0, 500.0, 8499.0, 'ALIPAY', '2025-08-01 14:35:00', 0.0, '张建国', '13800001002', '北京市海淀区中关村大街1号', '', '2025-08-01', DEFAULT, DEFAULT),
    ('ORD20250801003', 3, 'COMPLETED', 299.0, 0.0, 299.0, 'WECHAT', '2025-08-01 16:00:00', 0.0, '赵海涛', '13800001003', '上海市浦东新区陆家嘴', '', '2025-08-01', DEFAULT, DEFAULT),
    ('ORD20250802004', 4, 'PAID', 5600.0, 300.0, 5300.0, 'ALIPAY', '2025-08-02 09:10:00', 0.0, '陈美玲', '13800001004', '广东省广州市天河区珠江新城', '', '2025-08-02', DEFAULT, DEFAULT),
    ('ORD20250802005', 5, 'PENDING', 2100.0, 100.0, 2000.0, 'CARD', DEFAULT, 0.0, '王海波', '13800001005', '广东省深圳市南山区科技园', '', '2025-08-02', DEFAULT, DEFAULT),
    ('ORD20250803006', 6, 'COMPLETED', 8800.0, 500.0, 8300.0, 'ALIPAY', '2025-08-03 11:20:00', 0.0, '刘晓芳', '13800001006', '上海市静安区南京西路', '', '2025-08-03', DEFAULT, DEFAULT),
    ('ORD20250803007', 7, 'SHIPPED', 4200.0, 200.0, 4000.0, 'WECHAT', '2025-08-03 14:45:00', 0.0, '陈志强', '13800001007', '北京市朝阳区望京SOHO', '', '2025-08-03', DEFAULT, DEFAULT),
    ('ORD20250804008', 8, 'COMPLETED', 1500.0, 0.0, 1500.0, 'POINTS', '2025-08-04 08:30:00', 0.0, '周雅琴', '13800001008', '浙江省杭州市滨江区阿里中心', '', '2025-08-04', DEFAULT, DEFAULT),
    ('ORD20250805009', 9, 'COMPLETED', 6888.0, 300.0, 6588.0, 'ALIPAY', '2025-08-05 10:00:00', 0.0, '吴海明', '13800001009', '广东省广州市越秀区环市东路', '', '2025-08-05', DEFAULT, DEFAULT),
    ('ORD20250806010', 10, 'DELIVERED', 3900.0, 200.0, 3700.0, 'WECHAT', '2025-08-06 15:30:00', 0.0, '孙丽娟', '13800001010', '四川省成都市高新区天府大道', '', '2025-08-06', DEFAULT, DEFAULT),
    ('ORD20250807011', 11, 'COMPLETED', 2300.0, 0.0, 2300.0, 'CARD', '2025-08-07 09:00:00', 0.0, '郑伟', '13800001011', '广东省深圳市福田区华强北', '', '2025-08-07', DEFAULT, DEFAULT),
    ('ORD20250807012', 12, 'COMPLETED', 9200.0, 500.0, 8700.0, 'ALIPAY', '2025-08-07 13:20:00', 0.0, '李娜', '13800001012', '北京市海淀区上地信息路', '', '2025-08-07', DEFAULT, DEFAULT),
    ('ORD20250808013', 1, 'COMPLETED', 35.8, 0.0, 35.8, 'WECHAT', '2025-08-08 18:00:00', 0.0, '林小燕', '13800001001', '广东省深圳市南山区科技园', '', '2025-08-08', DEFAULT, DEFAULT),
    ('ORD20250808014', 13, 'PAID', 9200.0, 0.0, 9200.0, 'ALIPAY', '2025-08-08 20:00:00', 0.0, '刘东明', '13800001013', '湖北省武汉市洪山区光谷广场', '', '2025-08-08', DEFAULT, DEFAULT),
    ('ORD20250808015', 1, 'PENDING', 8999.0, 500.0, 8499.0, 'WECHAT', DEFAULT, 0.0, '林小燕', '13800001001', '广东省深圳市南山区科技园', '', '2025-08-08', DEFAULT, DEFAULT),
    ('ORD20250808016', 9, 'COMPLETED', 1299.0, 0.0, 1299.0, 'ALIPAY', '2025-08-08 18:30:00', 0.0, '吴海明', '13800001009', '广东省广州市越秀区环市东路', '', '2025-08-08', DEFAULT, DEFAULT),
    ('ORD20250809017', 12, 'PAID', 899.0, 50.0, 849.0, 'WECHAT', '2025-08-09 11:20:00', 0.0, '李娜', '13800001012', '北京市海淀区上地信息路', '', '2025-08-09', DEFAULT, DEFAULT),
    ('ORD20250809018', 10, 'SHIPPED', 568.0, 0.0, 568.0, 'CARD', '2025-08-09 09:45:00', 0.0, '孙丽娟', '13800001010', '四川省成都市高新区天府大道', '', '2025-08-09', DEFAULT, DEFAULT),
    ('ORD20250810019', 22, 'COMPLETED', 35.8, 0.0, 35.8, 'WECHAT', '2025-08-10 12:00:00', 0.0, '周若兰', '13800001020', '浙江省杭州市西湖区文三路', '', '2025-08-10', DEFAULT, DEFAULT),
    ('ORD20250811020', 15, 'COMPLETED', 156000.0, 5000.0, 151000.0, 'CARD', '2025-08-11 14:30:00', 0.0, '马俊峰', '13800001015', '北京市朝阳区三里屯SOHO', '', '2025-08-11', DEFAULT, DEFAULT),
    ('ORD20250812021', 2, 'COMPLETED', 16999.0, 0.0, 16999.0, 'ALIPAY', '2025-08-12 10:00:00', 0.0, '张建国', '13800001002', '北京市海淀区中关村大街1号', '', '2025-08-12', DEFAULT, DEFAULT),
    ('ORD20250812022', 4, 'COMPLETED', 899.0, 0.0, 899.0, 'WECHAT', '2025-08-12 16:00:00', 0.0, '陈美玲', '13800001004', '广东省广州市天河区珠江新城', '', '2025-08-12', DEFAULT, DEFAULT),
    ('ORD20250813023', 6, 'PAID', 1080.0, 0.0, 1080.0, 'ALIPAY', '2025-08-13 11:00:00', 0.0, '刘晓芳', '13800001006', '上海市静安区南京西路', '', '2025-08-13', DEFAULT, DEFAULT),
    ('ORD20250814024', 8, 'PENDING', 4990.0, 200.0, 4790.0, 'WECHAT', DEFAULT, 0.0, '周雅琴', '13800001008', '浙江省杭州市滨江区阿里中心', '', '2025-08-14', DEFAULT, DEFAULT),
    ('ORD20250815025', 1, 'COMPLETED', 89.0, 0.0, 89.0, 'WECHAT', '2025-08-15 08:00:00', 0.0, '林小燕', '13800001001', '广东省深圳市南山区科技园', '', '2025-08-15', DEFAULT, DEFAULT),
    ('ORD20250815060', 17, 'DELIVERED', 1299.0, 0.0, 1299.0, 'CARD', '2025-08-12 09:00:00', 0.0, '罗志远', '13800001017', '广东省深圳市南山区科技园', '', '2025-08-12', DEFAULT, DEFAULT);

-- [demo] demo_order_item
INSERT INTO demo_order_item (itemId, orderId, productId, productName, categoryId, skuCode, unitPrice, quantity, totalAmount, discountAmount, createdAt) VALUES
    (1, 'ORD20250801001', 2, 'AirPods Pro 2 无线降噪耳机', 1, 'APPL-Airpods2', 1899.0, 1, 1899.0, 100.0, DEFAULT),
    (2, 'ORD20250801002', 1, 'iPhone 15 Pro 256G 深空黑', 1, 'APPL-IP15P-256', 8999.0, 1, 8999.0, 500.0, DEFAULT),
    (3, 'ORD20250801003', 7, '戴睿 商务双肩包', 3, 'DAIR-SLB-01', 299.0, 1, 299.0, 0.0, DEFAULT),
    (4, 'ORD20250802004', 16, 'SK-II 神仙水 230ml', 5, 'SKII-GODW-230', 1199.0, 4, 4796.0, 300.0, DEFAULT),
    (5, 'ORD20250802004', 9, '雅诗兰黛 小棕瓶精华 50ml', 5, 'ESTEE-SB50-01', 899.0, 1, 899.0, 0.0, DEFAULT),
    (6, 'ORD20250803006', 1, 'iPhone 15 Pro 256G', 1, 'APPL-IP15P-256', 8999.0, 1, 8999.0, 500.0, DEFAULT),
    (7, 'ORD20250803007', 4, 'MacBook Pro 14寸', 2, 'APPL-MBP14-M3', 16999.0, 1, 16999.0, 200.0, DEFAULT),
    (8, 'ORD20250804008', 15, 'Babycare 婴儿湿巾 80抽*12包', 8, 'BABY-WET-80-12', 89.9, 10, 899.0, 0.0, DEFAULT),
    (9, 'ORD20250804008', 14, '海天 酱油 1.9L*2瓶装', 4, 'HAIT-SOY-19L2', 35.8, 10, 358.0, 0.0, DEFAULT),
    (10, 'ORD20250805009', 17, '戴森 V12 无线吸尘器', 6, 'DYSON-V12-01', 4990.0, 1, 4990.0, 200.0, DEFAULT),
    (11, 'ORD20250805009', 11, '飞利浦 空气炸锅', 6, 'PHILIPS-AF-01', 699.0, 3, 2097.0, 0.0, DEFAULT),
    (12, 'ORD20250806010', 5, 'ThinkPad X1 Carbon', 2, 'LENOVO-X1C-14', 8999.0, 1, 8999.0, 200.0, DEFAULT),
    (13, 'ORD20250807011', 6, '新秀丽 商务拉杆箱 20寸', 3, 'SAMSO-SU-20', 899.0, 2, 1798.0, 0.0, DEFAULT),
    (14, 'ORD20250807012', 10, '兰蔻 小黑瓶精华 50ml', 5, 'LANCOME-SB50-01', 1080.0, 6, 6480.0, 500.0, DEFAULT),
    (15, 'ORD20250807012', 12, 'iPad Pro 12.9寸', 1, 'APPL-iPadPro129', 8999.0, 1, 8999.0, 0.0, DEFAULT),
    (16, 'ORD20250808013', 14, '海天 酱油 1.9L*2瓶', 4, 'HAIT-SOY-19L2', 35.8, 1, 35.8, 0.0, DEFAULT),
    (17, 'ORD20250808015', 1, 'iPhone 15 Pro 256G', 1, 'APPL-IP15P-256', 8999.0, 1, 8999.0, 500.0, DEFAULT),
    (18, 'ORD20250808016', 6, '新秀丽 商务拉杆箱 20寸', 3, 'SAMSO-SU-20', 1299.0, 1, 1299.0, 0.0, DEFAULT),
    (19, 'ORD20250809017', 12, '雅诗兰黛 小棕瓶精华 50ml', 5, 'ESTEE-SB50-01', 899.0, 1, 899.0, 50.0, DEFAULT),
    (20, 'ORD20250809018', 10, '阳澄湖大闸蟹 8只装', 4, 'FOOD-DZ-8BOX', 568.0, 1, 568.0, 0.0, DEFAULT),
    (21, 'ORD20250810019', 22, '海天 酱油 1.9L*2瓶', 4, 'HAIT-SOY-19L2', 35.8, 1, 35.8, 0.0, DEFAULT),
    (22, 'ORD20250811020', 15, 'MacBook Pro 14寸 M3 Pro', 2, 'APPL-MBP14-M3', 16999.0, 9, 152991.0, 5000.0, DEFAULT),
    (23, 'ORD20250812021', 2, 'MacBook Pro 14寸 M3 Pro', 2, 'APPL-MBP14-M3', 16999.0, 1, 16999.0, 0.0, DEFAULT),
    (24, 'ORD20250812022', 6, '新秀丽 商务拉杆箱 20寸', 3, 'SAMSO-SU-20', 899.0, 1, 899.0, 0.0, DEFAULT),
    (25, 'ORD20250813023', 10, '兰蔻 小黑瓶精华 50ml', 5, 'LANCOME-SB50-01', 1080.0, 1, 1080.0, 0.0, DEFAULT),
    (26, 'ORD20250815025', 20, '费列罗榛果巧克力 32粒', 4, 'FERR-CHO-32', 89.0, 1, 89.0, 0.0, DEFAULT),
    (27, 'ORD20250815060', 6, '新秀丽 商务拉杆箱 20寸', 3, 'SAMSO-SU-20', 1299.0, 1, 1299.0, 0.0, DEFAULT);

-- [demo] demo_payment
INSERT INTO demo_payment (paymentId, orderId, userId, amount, payMethod, transactionId, payStatus, payTime, createdAt) VALUES
    ('PAY20250801001', 'ORD20250801001', 1, 1799.0, 'WECHAT', 'WX20250801001', 'SUCCESS', '2025-08-01 10:23:00', DEFAULT),
    ('PAY20250801002', 'ORD20250801002', 2, 8499.0, 'ALIPAY', 'ZFB20250801002', 'SUCCESS', '2025-08-01 14:35:00', DEFAULT),
    ('PAY20250801003', 'ORD20250801003', 3, 299.0, 'WECHAT', 'WX20250801003', 'SUCCESS', '2025-08-01 16:00:00', DEFAULT),
    ('PAY20250802004', 'ORD20250802004', 4, 5300.0, 'ALIPAY', 'ZFB20250802004', 'SUCCESS', '2025-08-02 09:10:00', DEFAULT),
    ('PAY20250802005', 'ORD20250802005', 5, 2000.0, 'CARD', 'CARD20250802005', 'PENDING', '2025-08-02 09:11:00', DEFAULT),
    ('PAY20250803006', 'ORD20250803006', 6, 8300.0, 'ALIPAY', 'ZFB20250803006', 'SUCCESS', '2025-08-03 11:20:00', DEFAULT),
    ('PAY20250803007', 'ORD20250803007', 7, 4000.0, 'WECHAT', 'WX20250803007', 'SUCCESS', '2025-08-03 14:45:00', DEFAULT),
    ('PAY20250804008', 'ORD20250804008', 8, 1500.0, 'POINTS', 'PTS20250804008', 'SUCCESS', '2025-08-04 08:30:00', DEFAULT),
    ('PAY20250805009', 'ORD20250805009', 9, 6588.0, 'ALIPAY', 'ZFB20250805009', 'SUCCESS', '2025-08-05 10:00:00', DEFAULT),
    ('PAY20250806010', 'ORD20250806010', 10, 3700.0, 'WECHAT', 'WX20250806010', 'SUCCESS', '2025-08-06 15:30:00', DEFAULT),
    ('PAY20250807011', 'ORD20250807011', 11, 2300.0, 'CARD', 'CARD20250807011', 'SUCCESS', '2025-08-07 09:00:00', DEFAULT),
    ('PAY20250807012', 'ORD20250807012', 12, 8700.0, 'ALIPAY', 'ZFB20250807012', 'SUCCESS', '2025-08-07 13:20:00', DEFAULT),
    ('PAY20250808013', 'ORD20250808013', 1, 35.8, 'WECHAT', 'WX20250808013', 'SUCCESS', '2025-08-08 18:00:00', DEFAULT),
    ('PAY20250808014', 'ORD20250808014', 13, 9200.0, 'ALIPAY', 'ZFB20250808014', 'SUCCESS', '2025-08-08 20:00:00', DEFAULT),
    ('PAY20250808015', 'ORD20250808015', 1, 8499.0, 'WECHAT', 'WX20250808015', 'PENDING', '2025-08-08 20:01:00', DEFAULT),
    ('PAY20250808016', 'ORD20250808016', 16, 1299.0, 'ALIPAY', 'ZFB20250808016', 'SUCCESS', '2025-08-08 18:30:00', DEFAULT),
    ('PAY20250809017', 'ORD20250809017', 12, 849.0, 'WECHAT', 'WX20250809017', 'SUCCESS', '2025-08-09 11:20:00', DEFAULT),
    ('PAY20250809018', 'ORD20250809018', 10, 568.0, 'CARD', 'CARD20250809018', 'SUCCESS', '2025-08-09 09:45:00', DEFAULT),
    ('PAY20250810019', 'ORD20250810019', 22, 35.8, 'WECHAT', 'WX20250810019', 'SUCCESS', '2025-08-10 12:00:00', DEFAULT),
    ('PAY20250811020', 'ORD20250811020', 15, 151000.0, 'CARD', 'CARD20250811020', 'SUCCESS', '2025-08-11 14:30:00', DEFAULT),
    ('PAY20250812021', 'ORD20250812021', 2, 16999.0, 'ALIPAY', 'ZFB20250812021', 'SUCCESS', '2025-08-12 10:00:00', DEFAULT),
    ('PAY20250812022', 'ORD20250812022', 4, 899.0, 'WECHAT', 'WX20250812022', 'SUCCESS', '2025-08-12 16:00:00', DEFAULT),
    ('PAY20250813023', 'ORD20250813023', 6, 1080.0, 'ALIPAY', 'ZFB20250813023', 'SUCCESS', '2025-08-13 11:00:00', DEFAULT),
    ('PAY20250815025', 'ORD20250815025', 1, 89.0, 'WECHAT', 'WX20250815025', 'SUCCESS', '2025-08-15 08:00:00', DEFAULT),
    ('PAY20250815060', 'ORD20250815060', 17, 1299.0, 'CARD', 'CARD20250815060', 'SUCCESS', '2025-08-12 09:00:00', DEFAULT);

SET FOREIGN_KEY_CHECKS = 1;
-- ==========================================================
-- [V7.2] 新增 7 张表的种子数据
-- ==========================================================

-- [minimax-model/prompt] 提示词模板 (V7.2 补)
INSERT INTO prompt_template (id, name, description, category, content, variables, creatorId, creatorName, isPublic, useCount, createdAt, updatedAt, deleted) VALUES
    (1, '通用对话', '通用对话模板', 'chat', '你好，我是 {{assistant_name}}，请问有什么可以帮您？', '["assistant_name"]', 1, 'admin', 1, 0, DEFAULT, DEFAULT, 0),
    (2, '代码生成', '代码生成模板', 'code', '请用 {{language}} 写一个 {{description}} 的函数', '["language","description"]', 1, 'admin', 1, 0, DEFAULT, DEFAULT, 0),
    (3, '数据分析师', '数据分析模板', 'analysis', '请分析以下数据的趋势：{{data}}', '["data"]', 1, 'admin', 1, 0, DEFAULT, DEFAULT, 0),
    (4, '翻译助手', '中英翻译模板', 'translation', '请将以下 {{from_lang}} 翻译成 {{to_lang}}: {{text}}', '["from_lang","to_lang","text"]', 1, 'admin', 1, 0, DEFAULT, DEFAULT, 0);

-- [minimax-auth] user preferences (V7.2 补, 用户主题/语言)
INSERT INTO user_preferences (id, userId, theme, language, createdAt, updatedAt) VALUES
    (1, 1, 'light', 'zh-CN', DEFAULT, DEFAULT),
    (2, 2, 'light', 'zh-CN', DEFAULT, DEFAULT),
    (3, 3, 'dark', 'en-US', DEFAULT, DEFAULT);

-- [minimax-chat/memory_ext] user preferences memory (V7.2 补)
INSERT INTO memory_user_pref (id, userId, prefKey, prefValue, weight, source, createdAt, updatedAt, deleted) VALUES
    (1, 1, 'response_length', 'concise', 1.0, 'learned', DEFAULT, DEFAULT, 0),
    (2, 1, 'language', 'zh-CN', 1.0, 'explicit', DEFAULT, DEFAULT, 0),
    (3, 2, 'topic_interest', 'AI,code,music', 0.8, 'learned', DEFAULT, DEFAULT, 0);

-- [minimax-chat/memory_ext] long-term memory (V7.2 补, 示例)
INSERT INTO memory_long_term (id, userId, sessionId, content, summary, role, importance, tags, accessCount, createdAt, updatedAt, deleted) VALUES
    (1, 1, NULL, 'User 1 is a senior backend developer working on Java/Spring Cloud microservices', 'Java 资深后端开发', 'user', 9.5, 'profile,java,backend', 5, DEFAULT, DEFAULT, 0),
    (2, 1, NULL, 'User 1 prefers concise responses with code examples', '偏好简洁+代码示例', 'user', 8.0, 'preference,style', 12, DEFAULT, DEFAULT, 0);

-- [minimax-ai/cluster/raft] raft log 示例 (V7.2 补)
INSERT INTO raft_log (idx, term, commandType, commandPayload, timestampMs) VALUES
    (1, 1, 'NOOP', '{}', UNIX_TIMESTAMP() * 1000),
    (2, 1, 'STATE_UPDATE', '{"key":"minimax-system","value":"healthy"}', UNIX_TIMESTAMP() * 1000);

-- [minimax-agent] knowledge graph entity 种子 (V7.2 补, 4 个示例)
INSERT INTO kg_entity (id, userId, name, entityType, description, importance, source, refCount, createdAt, updatedAt, deleted) VALUES
    (1, 1, '爱因斯坦', 'PERSON', '物理学家, 相对论提出者', 10, 'seed', 0, DEFAULT, DEFAULT, 0),
    (2, 1, '居里夫人', 'PERSON', '物理学家/化学家, 放射性研究', 9, 'seed', 0, DEFAULT, DEFAULT, 0),
    (3, 1, '相对论', 'CONCEPT', '爱因斯坦提出的物理学理论', 10, 'seed', 0, DEFAULT, DEFAULT, 0),
    (4, 1, '诺贝尔奖', 'AWARD', '国际顶级学术奖项', 8, 'seed', 0, DEFAULT, DEFAULT, 0);

-- [minimax-agent] knowledge graph relation 种子 (V7.2 补, 4 个示例)
INSERT INTO kg_relation (id, userId, fromEntity, toEntity, relationType, description, weight, source, refCount, createdAt, updatedAt, deleted) VALUES
    (1, 1, 1, 3, 'PROPOSED', '爱因斯坦提出相对论', 1.0, 'seed', 0, DEFAULT, DEFAULT, 0),
    (2, 1, 2, 4, 'WON', '居里夫人获诺贝尔奖', 1.0, 'seed', 0, DEFAULT, DEFAULT, 0),
    (3, 1, 1, 4, 'WON', '爱因斯坦获诺贝尔奖', 1.0, 'seed', 0, DEFAULT, DEFAULT, 0),
    (4, 1, 1, 2, 'CONTEMPORARY', '同时代物理学家', 0.9, 'seed', 0, DEFAULT, DEFAULT, 0);

-- [V7.2] data_source 同步示例 (兼容实体, 与 db_data_source 相同结构)
INSERT INTO data_source (id, name, type, jdbcUrl, username, password, driverClass, poolSize, minIdle, maxLifetime, enabled, testStatus, testMessage, lastTestAt, description, tags, createdBy, createdAt, updatedAt, deleted) VALUES
    (1, '生产 MySQL', 'mysql', 'jdbc:mysql://10.0.0.1:3306/prod?useSSL=false&serverTimezone=UTC', 'readonly', '******', 'com.mysql.cj.jdbc.Driver', 10, 2, 1800, 1, 'OK', 'Connection successful', DEFAULT, '生产环境主库 (只读)', 'prod,mysql,readonly', 1, DEFAULT, DEFAULT, 0);


-- ==========================================================
-- [T1-backend-apis / P0] 5 张新表的种子数据
-- 配套端点:
--   rule_definition        (minimax-pipeline,  /api/v1/rule)
--   trained_model          (minimax-ai,        /api/v1/training/models)
--   notification_settings  (minimax-auth,      /api/v1/notification/settings)
--   collab_invite          (minimax-ai,        /api/v1/collab/rooms/{id}/invite)
--   system_settings        (minimax-system,    /api/v1/system/settings)
-- ==========================================================

-- [minimax-pipeline] rule_definition
INSERT INTO rule_definition (id, name, jsonContent, scope, enabled, createdBy, createdAt, updatedAt, deleted) VALUES
    (1, '默认安全规则', '{"action":"block","pattern":"forbidden_word","level":"high"}', 'GLOBAL', 1, 1, DEFAULT, DEFAULT, 0),
    (2, '内容审核规则', '{"action":"review","pattern":"sensitive","level":"medium"}', 'GLOBAL', 1, 1, DEFAULT, DEFAULT, 0),
    (3, '速率限制规则', '{"action":"throttle","limit":100,"window":"1m"}', 'GLOBAL', 1, 1, DEFAULT, DEFAULT, 0);

-- [minimax-ai] trained_model (5 个 seed 数据, 覆盖 ENABLED/DISABLED/DRAFT 三种状态)
INSERT INTO trained_model (id, code, name, accuracy, status, publishedAt, createdBy, createdAt, updatedAt, deleted) VALUES
    (1, 'mmx-mini-v1',  'MiniMax 轻量对话 v1',  0.872, 'ENABLED',  '2026-07-15 10:00:00', 1, DEFAULT, DEFAULT, 0),
    (2, 'mmx-coder-v1', 'MiniMax 代码生成 v1',  0.798, 'ENABLED',  '2026-08-01 14:30:00', 1, DEFAULT, DEFAULT, 0),
    (3, 'mmx-rag-v1',   'MiniMax 检索增强 v1',  0.835, 'ENABLED',  '2026-08-10 09:15:00', 1, DEFAULT, DEFAULT, 0),
    (4, 'mmx-mini-v2',  'MiniMax 轻量对话 v2',  0.910, 'DRAFT',    NULL,                    1, DEFAULT, DEFAULT, 0),
    (5, 'mmx-coder-v2', 'MiniMax 代码生成 v2',  0.752, 'DISABLED', '2026-06-20 11:00:00', 1, DEFAULT, DEFAULT, 0);

-- [minimax-auth] notification_settings (3 个用户各一条)
INSERT INTO notification_settings (id, userId, channels, events, quietStart, quietEnd, createdAt, updatedAt) VALUES
    (1, 1, 'email,dingtalk,webhook', 'login,error,alert,system', '22:00', '08:00', DEFAULT, DEFAULT),
    (2, 2, 'email,webhook',         'error,alert',               '23:00', '07:00', DEFAULT, DEFAULT),
    (3, 3, 'push,email',            'login,system',              '00:00', '06:00', DEFAULT, DEFAULT);

-- [minimax-ai] collab_invite (2 个示例邀请)
INSERT INTO collab_invite (id, roomId, inviterId, inviteeEmail, inviteeUserId, token, status, expiresAt, acceptedAt, createdAt, updatedAt, deleted) VALUES
    (1, 1, 1, 'alice88@minimax.com',   2, 'inv-aaaa-1111-bbbb-2222-cccc-3333dddd4444', 'ACCEPTED', '2026-09-01 00:00:00', '2026-08-15 10:30:00', DEFAULT, DEFAULT, 0),
    (2, 1, 1, 'bob_dev@minimax.com',   3, 'inv-eeee-5555-ffff-6666-aaaa-7777bbbb8888', 'PENDING',  '2026-09-15 00:00:00', NULL,                    DEFAULT, DEFAULT, 0);

-- [minimax-system] system_settings (单行, id=1)
INSERT INTO system_settings (id, siteName, siteLogo, maintenanceMode, allowRegister, defaultModelCode, description, contactEmail, updatedBy, createdAt, updatedAt) VALUES
    (1, 'MiniMax 平台', '/logo.svg', 0, 1, 'gpt-4o', 'MiniMax 大模型平台 - 集成对话/智能体/多模态/训练全流程', 'admin@minimax.com', 1, DEFAULT, DEFAULT);


-- ==========================================================
-- [T1-backend-orchestrator / P0] agent_group_member 种子
-- 配套端点: /api/v1/agent-group/{groupId}/members
-- 针对已有 AgentGroup.id=1 添加 3 个成员
-- ==========================================================
INSERT INTO agent_group_member (id, group_id, agent_code, role, position, config_json, enabled, created_at, updated_at) VALUES
    (1, 1, 'echo-manager',     'MANAGER', 0, '{"capability":"coordinator","weight":2.0}', 1, DEFAULT, DEFAULT),
    (2, 1, 'echo-writer',      'WORKER',  1, '{"capability":"writer","weight":1.0}',      1, DEFAULT, DEFAULT),
    (3, 1, 'echo-reviewer',    'CRITIC',  2, '{"capability":"reviewer","weight":1.5}',    1, DEFAULT, DEFAULT);


-- ==========================================================
-- 演示电商数据 (原 demo-init.sql, 整合到本文件)
-- ==========================================================


SET MODE MySQL;
SET NAMES utf8mb4;

INSERT INTO demo_user (userId, userName, realName, email, phone, gender, age, city, level, balance, status, createdAt, updatedAt) VALUES
    (1, 'alice88', '林小燕', 'linxy@email.com', '13800001001', 'F', 28, '深圳', 4, 2580.5, 'ACTIVE', DEFAULT, DEFAULT),
    (2, 'bob_dev', '张建国', 'zhangjg@email.com', '13800001002', 'M', 35, '北京', 5, 12000.0, 'ACTIVE', DEFAULT, DEFAULT),
    (3, 'charlie_z', '赵海涛', 'zhaoht@email.com', '13800001003', 'M', 42, '上海', 3, 340.0, 'ACTIVE', DEFAULT, DEFAULT),
    (4, 'diana_pm', '陈美玲', 'chenml@email.com', '13800001004', 'F', 31, '广州', 4, 5600.0, 'ACTIVE', DEFAULT, DEFAULT),
    (5, 'evan_art', '王海波', 'wanghb@email.com', '13800001005', 'M', 26, '深圳', 3, 2100.0, 'ACTIVE', DEFAULT, DEFAULT),
    (6, 'fiona_fin', '刘晓芳', 'liuxf@email.com', '13800001006', 'F', 38, '上海', 4, 8800.0, 'ACTIVE', DEFAULT, DEFAULT),
    (7, 'george_pm', '陈志强', 'chenzq@email.com', '13800001007', 'M', 33, '北京', 3, 4200.0, 'ACTIVE', DEFAULT, DEFAULT),
    (8, 'helen_mkt', '周雅琴', 'zhouyq@email.com', '13800001008', 'F', 29, '杭州', 2, 1500.0, 'ACTIVE', DEFAULT, DEFAULT),
    (9, 'ivan_ops', '吴海明', 'wuhm@email.com', '13800001009', 'M', 36, '广州', 4, 6800.0, 'ACTIVE', DEFAULT, DEFAULT),
    (10, 'julia_hr', '孙丽娟', 'sunlj@email.com', '13800001010', 'F', 32, '成都', 3, 3900.0, 'ACTIVE', DEFAULT, DEFAULT),
    (11, 'kevin_dev', '郑伟', 'zhengw@email.com', '13800001011', 'M', 27, '深圳', 2, 2300.0, 'ACTIVE', DEFAULT, DEFAULT),
    (12, 'lisa_sal', '李娜', 'lina@email.com', '13800001012', 'F', 34, '北京', 4, 9200.0, 'ACTIVE', DEFAULT, DEFAULT),
    (13, 'mike_ops', '刘东明', 'liudm@email.com', '13800001013', 'M', 36, '武汉', 4, 9200.0, 'ACTIVE', DEFAULT, DEFAULT),
    (14, 'nancy_des', '陈雪琴', 'chenxq@email.com', '13800001014', 'F', 25, '杭州', 2, 450.0, 'ACTIVE', DEFAULT, DEFAULT),
    (15, 'oscar_fin', '马俊峰', 'majf@email.com', '13800001015', 'M', 40, '北京', 5, 156000.0, 'ACTIVE', DEFAULT, DEFAULT),
    (16, 'peggy_sal', '李秀英', 'lixy@email.com', '13800001016', 'F', 32, '上海', 3, 2100.0, 'ACTIVE', DEFAULT, DEFAULT),
    (17, 'quinn_dev', '罗志远', 'luozhy@email.com', '13800001017', 'M', 29, '深圳', 3, 3200.0, 'ACTIVE', DEFAULT, DEFAULT),
    (18, 'rachel_cs', '张小敏', 'zhangxm@email.com', '13800001018', 'F', 27, '成都', 2, 980.0, 'ACTIVE', DEFAULT, DEFAULT),
    (19, 'steve_pm', '杨建强', 'yangjq@email.com', '13800001019', 'M', 44, '广州', 4, 7500.0, 'ACTIVE', DEFAULT, DEFAULT),
    (20, 'tina_data', '周若兰', 'zhou_rl@email.com', '13800001020', 'F', 31, '杭州', 3, 4100.0, 'FROZEN', DEFAULT, DEFAULT);

-- [demo] demo_category
INSERT INTO demo_category (categoryId, categoryName, parentId, sortOrder, iconUrl, description, createdAt) VALUES
    (1, '手机数码', DEFAULT, 1, '/icons/phone.png', '手机、耳机、充电器等', DEFAULT),
    (2, '电脑办公', DEFAULT, 2, '/icons/laptop.png', '笔记本、台式机、键鼠等', DEFAULT),
    (3, '箱包皮具', DEFAULT, 3, '/icons/bag.png', '旅行箱、双肩包、手提包等', DEFAULT),
    (4, '食品饮料', DEFAULT, 4, '/icons/food.png', '零食、生鲜、饮料等', DEFAULT),
    (5, '美妆护肤', DEFAULT, 5, '/icons/beauty.png', '护肤品、化妆品、香水等', DEFAULT),
    (6, '家用电器', DEFAULT, 6, '/icons/appliance.png', '电饭煲、空气炸锅、吸尘器等', DEFAULT),
    (7, '服饰内衣', DEFAULT, 7, '/icons/clothing.png', '男装、女装、童装等', DEFAULT),
    (8, '母婴用品', DEFAULT, 8, '/icons/baby.png', '婴儿纸尿裤、湿巾、奶粉等', DEFAULT);

-- [demo] demo_product
INSERT INTO demo_product (productId, productName, categoryId, brand, price, cost, stock, soldCount, rating, reviewCount, tags, status, createdAt, updatedAt) VALUES
    (1, 'iPhone 15 Pro 256G 深空黑', 1, 'Apple', 8999.0, 7500.0, 120, 890, 4.9, 4520, '手机,旗舰,5G', 'ONLINE', DEFAULT, DEFAULT),
    (2, 'AirPods Pro 2 无线降噪耳机', 1, 'Apple', 1899.0, 1400.0, 350, 2100, 4.85, 8760, '耳机,降噪,无线', 'ONLINE', DEFAULT, DEFAULT),
    (3, '小米14 Ultra 影像旗舰 512G', 1, '小米', 6499.0, 5200.0, 200, 560, 4.78, 1230, '手机,旗舰,徕卡', 'ONLINE', DEFAULT, DEFAULT),
    (4, 'MacBook Pro 14寸 M3 Pro 18+512', 2, 'Apple', 16999.0, 14500.0, 80, 320, 4.93, 1890, '笔记本,苹果,专业', 'ONLINE', DEFAULT, DEFAULT),
    (5, 'ThinkPad X1 Carbon 14寸超薄本', 2, '联想', 8999.0, 7200.0, 150, 480, 4.72, 980, '笔记本,商务,轻薄', 'ONLINE', DEFAULT, DEFAULT),
    (6, '新秀丽 商务拉杆箱 20寸', 3, '新秀丽', 899.0, 450.0, 300, 1800, 4.8, 5600, '箱包,商务,拉杆箱', 'ONLINE', DEFAULT, DEFAULT),
    (7, '戴睿 商务双肩包 笔记本电脑包', 3, '戴睿', 299.0, 120.0, 600, 2300, 4.55, 7800, '箱包,商务,电脑包', 'ONLINE', DEFAULT, DEFAULT),
    (8, '阳澄湖大闸蟹 公4两母3两 8只装', 4, '阳澄湖', 568.0, 280.0, 100, 3500, 4.88, 12000, '大闸蟹,生鲜,海鲜', 'ONLINE', DEFAULT, DEFAULT),
    (9, '雅诗兰黛 第七代小棕瓶精华 50ml', 5, '雅诗兰黛', 899.0, 560.0, 200, 1200, 4.87, 8900, '精华,护肤,抗老', 'ONLINE', DEFAULT, DEFAULT),
    (10, '兰蔻 小黑瓶精华肌底液 50ml', 5, '兰蔻', 1080.0, 680.0, 140, 780, 4.87, 1900, '精华,护肤,法国', 'ONLINE', DEFAULT, DEFAULT),
    (11, '飞利浦 空气炸锅 5L大容量', 6, '飞利浦', 699.0, 380.0, 250, 1100, 4.72, 3100, '空气炸锅,厨房,健康', 'ONLINE', DEFAULT, DEFAULT),
    (12, 'iPad Pro 12.9寸 M2 256G', 1, 'Apple', 8999.0, 7200.0, 80, 650, 4.91, 3200, '平板,苹果,办公', 'ONLINE', DEFAULT, DEFAULT),
    (13, '索尼 WH-1000XM5 头戴降噪耳机', 1, '索尼', 2699.0, 1900.0, 180, 920, 4.83, 2300, '耳机,降噪,头戴', 'ONLINE', DEFAULT, DEFAULT),
    (14, '海天 酱油 味极鲜 1.9L*2瓶装', 4, '海天', 35.8, 15.0, 2000, 12000, 4.6, 35000, '酱油,调味,厨房', 'ONLINE', DEFAULT, DEFAULT),
    (15, 'Babycare 婴儿湿巾 80抽*12包整箱', 8, 'Babycare', 89.9, 38.0, 1500, 7800, 4.75, 21000, '湿巾,婴儿,整箱', 'ONLINE', DEFAULT, DEFAULT),
    (16, 'SK-II 神仙水 230ml 精华液', 5, 'SK-II', 1199.0, 750.0, 100, 890, 4.88, 12000, '精华,护肤,日本', 'ONLINE', DEFAULT, DEFAULT),
    (17, '戴森 V12 无线吸尘器', 6, '戴森', 4990.0, 3500.0, 50, 320, 4.9, 1500, '吸尘器,无线,家电', 'ONLINE', DEFAULT, DEFAULT),
    (18, 'Nintendo Switch OLED 国行版', 1, '任天堂', 2299.0, 1700.0, 200, 2500, 4.85, 8900, '游戏机,掌机,娱乐', 'ONLINE', DEFAULT, DEFAULT),
    (19, '阿迪达斯 男子运动T恤 黑色 XL', 7, '阿迪达斯', 199.0, 60.0, 500, 3200, 4.6, 15000, 'T恤,运动,男装', 'ONLINE', DEFAULT, DEFAULT),
    (20, '费列罗榛果威化巧克力 32粒装', 4, '费列罗', 89.0, 38.0, 1000, 8000, 4.7, 25000, '巧克力,零食,礼盒', 'ONLINE', DEFAULT, DEFAULT);

-- [demo] demo_order
INSERT INTO demo_order (orderId, userId, orderStatus, totalAmount, discountAmount, payAmount, payMethod, payTime, shippingFee, receiverName, receiverPhone, receiverAddress, remark, orderDate, createdAt, updatedAt) VALUES
    ('ORD20250801001', 1, 'COMPLETED', 1899.0, 100.0, 1799.0, 'WECHAT', '2025-08-01 10:23:00', 0.0, '林小燕', '13800001001', '广东省深圳市南山区科技园', '', '2025-08-01', DEFAULT, DEFAULT),
    ('ORD20250801002', 2, 'COMPLETED', 8999.0, 500.0, 8499.0, 'ALIPAY', '2025-08-01 14:35:00', 0.0, '张建国', '13800001002', '北京市海淀区中关村大街1号', '', '2025-08-01', DEFAULT, DEFAULT),
    ('ORD20250801003', 3, 'COMPLETED', 299.0, 0.0, 299.0, 'WECHAT', '2025-08-01 16:00:00', 0.0, '赵海涛', '13800001003', '上海市浦东新区陆家嘴', '', '2025-08-01', DEFAULT, DEFAULT),
    ('ORD20250802004', 4, 'PAID', 5600.0, 300.0, 5300.0, 'ALIPAY', '2025-08-02 09:10:00', 0.0, '陈美玲', '13800001004', '广东省广州市天河区珠江新城', '', '2025-08-02', DEFAULT, DEFAULT),
    ('ORD20250802005', 5, 'PENDING', 2100.0, 100.0, 2000.0, 'CARD', DEFAULT, 0.0, '王海波', '13800001005', '广东省深圳市南山区科技园', '', '2025-08-02', DEFAULT, DEFAULT),
    ('ORD20250803006', 6, 'COMPLETED', 8800.0, 500.0, 8300.0, 'ALIPAY', '2025-08-03 11:20:00', 0.0, '刘晓芳', '13800001006', '上海市静安区南京西路', '', '2025-08-03', DEFAULT, DEFAULT),
    ('ORD20250803007', 7, 'SHIPPED', 4200.0, 200.0, 4000.0, 'WECHAT', '2025-08-03 14:45:00', 0.0, '陈志强', '13800001007', '北京市朝阳区望京SOHO', '', '2025-08-03', DEFAULT, DEFAULT),
    ('ORD20250804008', 8, 'COMPLETED', 1500.0, 0.0, 1500.0, 'POINTS', '2025-08-04 08:30:00', 0.0, '周雅琴', '13800001008', '浙江省杭州市滨江区阿里中心', '', '2025-08-04', DEFAULT, DEFAULT),
    ('ORD20250805009', 9, 'COMPLETED', 6888.0, 300.0, 6588.0, 'ALIPAY', '2025-08-05 10:00:00', 0.0, '吴海明', '13800001009', '广东省广州市越秀区环市东路', '', '2025-08-05', DEFAULT, DEFAULT),
    ('ORD20250806010', 10, 'DELIVERED', 3900.0, 200.0, 3700.0, 'WECHAT', '2025-08-06 15:30:00', 0.0, '孙丽娟', '13800001010', '四川省成都市高新区天府大道', '', '2025-08-06', DEFAULT, DEFAULT),
    ('ORD20250807011', 11, 'COMPLETED', 2300.0, 0.0, 2300.0, 'CARD', '2025-08-07 09:00:00', 0.0, '郑伟', '13800001011', '广东省深圳市福田区华强北', '', '2025-08-07', DEFAULT, DEFAULT),
    ('ORD20250807012', 12, 'COMPLETED', 9200.0, 500.0, 8700.0, 'ALIPAY', '2025-08-07 13:20:00', 0.0, '李娜', '13800001012', '北京市海淀区上地信息路', '', '2025-08-07', DEFAULT, DEFAULT),
    ('ORD20250808013', 1, 'COMPLETED', 35.8, 0.0, 35.8, 'WECHAT', '2025-08-08 18:00:00', 0.0, '林小燕', '13800001001', '广东省深圳市南山区科技园', '', '2025-08-08', DEFAULT, DEFAULT),
    ('ORD20250808014', 13, 'PAID', 9200.0, 0.0, 9200.0, 'ALIPAY', '2025-08-08 20:00:00', 0.0, '刘东明', '13800001013', '湖北省武汉市洪山区光谷广场', '', '2025-08-08', DEFAULT, DEFAULT),
    ('ORD20250808015', 1, 'PENDING', 8999.0, 500.0, 8499.0, 'WECHAT', DEFAULT, 0.0, '林小燕', '13800001001', '广东省深圳市南山区科技园', '', '2025-08-08', DEFAULT, DEFAULT),
    ('ORD20250808016', 9, 'COMPLETED', 1299.0, 0.0, 1299.0, 'ALIPAY', '2025-08-08 18:30:00', 0.0, '吴海明', '13800001009', '广东省广州市越秀区环市东路', '', '2025-08-08', DEFAULT, DEFAULT),
    ('ORD20250809017', 12, 'PAID', 899.0, 50.0, 849.0, 'WECHAT', '2025-08-09 11:20:00', 0.0, '李娜', '13800001012', '北京市海淀区上地信息路', '', '2025-08-09', DEFAULT, DEFAULT),
    ('ORD20250809018', 10, 'SHIPPED', 568.0, 0.0, 568.0, 'CARD', '2025-08-09 09:45:00', 0.0, '孙丽娟', '13800001010', '四川省成都市高新区天府大道', '', '2025-08-09', DEFAULT, DEFAULT),
    ('ORD20250810019', 22, 'COMPLETED', 35.8, 0.0, 35.8, 'WECHAT', '2025-08-10 12:00:00', 0.0, '周若兰', '13800001020', '浙江省杭州市西湖区文三路', '', '2025-08-10', DEFAULT, DEFAULT),
    ('ORD20250811020', 15, 'COMPLETED', 156000.0, 5000.0, 151000.0, 'CARD', '2025-08-11 14:30:00', 0.0, '马俊峰', '13800001015', '北京市朝阳区三里屯SOHO', '', '2025-08-11', DEFAULT, DEFAULT),
    ('ORD20250812021', 2, 'COMPLETED', 16999.0, 0.0, 16999.0, 'ALIPAY', '2025-08-12 10:00:00', 0.0, '张建国', '13800001002', '北京市海淀区中关村大街1号', '', '2025-08-12', DEFAULT, DEFAULT),
    ('ORD20250812022', 4, 'COMPLETED', 899.0, 0.0, 899.0, 'WECHAT', '2025-08-12 16:00:00', 0.0, '陈美玲', '13800001004', '广东省广州市天河区珠江新城', '', '2025-08-12', DEFAULT, DEFAULT),
    ('ORD20250813023', 6, 'PAID', 1080.0, 0.0, 1080.0, 'ALIPAY', '2025-08-13 11:00:00', 0.0, '刘晓芳', '13800001006', '上海市静安区南京西路', '', '2025-08-13', DEFAULT, DEFAULT),
    ('ORD20250814024', 8, 'PENDING', 4990.0, 200.0, 4790.0, 'WECHAT', DEFAULT, 0.0, '周雅琴', '13800001008', '浙江省杭州市滨江区阿里中心', '', '2025-08-14', DEFAULT, DEFAULT),
    ('ORD20250815025', 1, 'COMPLETED', 89.0, 0.0, 89.0, 'WECHAT', '2025-08-15 08:00:00', 0.0, '林小燕', '13800001001', '广东省深圳市南山区科技园', '', '2025-08-15', DEFAULT, DEFAULT),
    ('ORD20250815060', 17, 'DELIVERED', 1299.0, 0.0, 1299.0, 'CARD', '2025-08-12 09:00:00', 0.0, '罗志远', '13800001017', '广东省深圳市南山区科技园', '', '2025-08-12', DEFAULT, DEFAULT);

-- [demo] demo_order_item
INSERT INTO demo_order_item (itemId, orderId, productId, productName, categoryId, skuCode, unitPrice, quantity, totalAmount, discountAmount, createdAt) VALUES
    (1, 'ORD20250801001', 2, 'AirPods Pro 2 无线降噪耳机', 1, 'APPL-Airpods2', 1899.0, 1, 1899.0, 100.0, DEFAULT),
    (2, 'ORD20250801002', 1, 'iPhone 15 Pro 256G 深空黑', 1, 'APPL-IP15P-256', 8999.0, 1, 8999.0, 500.0, DEFAULT),
    (3, 'ORD20250801003', 7, '戴睿 商务双肩包', 3, 'DAIR-SLB-01', 299.0, 1, 299.0, 0.0, DEFAULT),
    (4, 'ORD20250802004', 16, 'SK-II 神仙水 230ml', 5, 'SKII-GODW-230', 1199.0, 4, 4796.0, 300.0, DEFAULT),
    (5, 'ORD20250802004', 9, '雅诗兰黛 小棕瓶精华 50ml', 5, 'ESTEE-SB50-01', 899.0, 1, 899.0, 0.0, DEFAULT),
    (6, 'ORD20250803006', 1, 'iPhone 15 Pro 256G', 1, 'APPL-IP15P-256', 8999.0, 1, 8999.0, 500.0, DEFAULT),
    (7, 'ORD20250803007', 4, 'MacBook Pro 14寸', 2, 'APPL-MBP14-M3', 16999.0, 1, 16999.0, 200.0, DEFAULT),
    (8, 'ORD20250804008', 15, 'Babycare 婴儿湿巾 80抽*12包', 8, 'BABY-WET-80-12', 89.9, 10, 899.0, 0.0, DEFAULT),
    (9, 'ORD20250804008', 14, '海天 酱油 1.9L*2瓶装', 4, 'HAIT-SOY-19L2', 35.8, 10, 358.0, 0.0, DEFAULT),
    (10, 'ORD20250805009', 17, '戴森 V12 无线吸尘器', 6, 'DYSON-V12-01', 4990.0, 1, 4990.0, 200.0, DEFAULT),
    (11, 'ORD20250805009', 11, '飞利浦 空气炸锅', 6, 'PHILIPS-AF-01', 699.0, 3, 2097.0, 0.0, DEFAULT),
    (12, 'ORD20250806010', 5, 'ThinkPad X1 Carbon', 2, 'LENOVO-X1C-14', 8999.0, 1, 8999.0, 200.0, DEFAULT),
    (13, 'ORD20250807011', 6, '新秀丽 商务拉杆箱 20寸', 3, 'SAMSO-SU-20', 899.0, 2, 1798.0, 0.0, DEFAULT),
    (14, 'ORD20250807012', 10, '兰蔻 小黑瓶精华 50ml', 5, 'LANCOME-SB50-01', 1080.0, 6, 6480.0, 500.0, DEFAULT),
    (15, 'ORD20250807012', 12, 'iPad Pro 12.9寸', 1, 'APPL-iPadPro129', 8999.0, 1, 8999.0, 0.0, DEFAULT),
    (16, 'ORD20250808013', 14, '海天 酱油 1.9L*2瓶', 4, 'HAIT-SOY-19L2', 35.8, 1, 35.8, 0.0, DEFAULT),
    (17, 'ORD20250808015', 1, 'iPhone 15 Pro 256G', 1, 'APPL-IP15P-256', 8999.0, 1, 8999.0, 500.0, DEFAULT),
    (18, 'ORD20250808016', 6, '新秀丽 商务拉杆箱 20寸', 3, 'SAMSO-SU-20', 1299.0, 1, 1299.0, 0.0, DEFAULT),
    (19, 'ORD20250809017', 12, '雅诗兰黛 小棕瓶精华 50ml', 5, 'ESTEE-SB50-01', 899.0, 1, 899.0, 50.0, DEFAULT),
    (20, 'ORD20250809018', 10, '阳澄湖大闸蟹 8只装', 4, 'FOOD-DZ-8BOX', 568.0, 1, 568.0, 0.0, DEFAULT),
    (21, 'ORD20250810019', 22, '海天 酱油 1.9L*2瓶', 4, 'HAIT-SOY-19L2', 35.8, 1, 35.8, 0.0, DEFAULT),
    (22, 'ORD20250811020', 15, 'MacBook Pro 14寸 M3 Pro', 2, 'APPL-MBP14-M3', 16999.0, 9, 152991.0, 5000.0, DEFAULT),
    (23, 'ORD20250812021', 2, 'MacBook Pro 14寸 M3 Pro', 2, 'APPL-MBP14-M3', 16999.0, 1, 16999.0, 0.0, DEFAULT),
    (24, 'ORD20250812022', 6, '新秀丽 商务拉杆箱 20寸', 3, 'SAMSO-SU-20', 899.0, 1, 899.0, 0.0, DEFAULT),
    (25, 'ORD20250813023', 10, '兰蔻 小黑瓶精华 50ml', 5, 'LANCOME-SB50-01', 1080.0, 1, 1080.0, 0.0, DEFAULT),
    (26, 'ORD20250815025', 20, '费列罗榛果巧克力 32粒', 4, 'FERR-CHO-32', 89.0, 1, 89.0, 0.0, DEFAULT),
    (27, 'ORD20250815060', 6, '新秀丽 商务拉杆箱 20寸', 3, 'SAMSO-SU-20', 1299.0, 1, 1299.0, 0.0, DEFAULT);

-- [demo] demo_payment
INSERT INTO demo_payment (paymentId, orderId, userId, amount, payMethod, transactionId, payStatus, payTime, createdAt) VALUES
    ('PAY20250801001', 'ORD20250801001', 1, 1799.0, 'WECHAT', 'WX20250801001', 'SUCCESS', '2025-08-01 10:23:00', DEFAULT),
    ('PAY20250801002', 'ORD20250801002', 2, 8499.0, 'ALIPAY', 'ZFB20250801002', 'SUCCESS', '2025-08-01 14:35:00', DEFAULT),
    ('PAY20250801003', 'ORD20250801003', 3, 299.0, 'WECHAT', 'WX20250801003', 'SUCCESS', '2025-08-01 16:00:00', DEFAULT),
    ('PAY20250802004', 'ORD20250802004', 4, 5300.0, 'ALIPAY', 'ZFB20250802004', 'SUCCESS', '2025-08-02 09:10:00', DEFAULT),
    ('PAY20250802005', 'ORD20250802005', 5, 2000.0, 'CARD', 'CARD20250802005', 'PENDING', '2025-08-02 09:11:00', DEFAULT),
    ('PAY20250803006', 'ORD20250803006', 6, 8300.0, 'ALIPAY', 'ZFB20250803006', 'SUCCESS', '2025-08-03 11:20:00', DEFAULT),
    ('PAY20250803007', 'ORD20250803007', 7, 4000.0, 'WECHAT', 'WX20250803007', 'SUCCESS', '2025-08-03 14:45:00', DEFAULT),
    ('PAY20250804008', 'ORD20250804008', 8, 1500.0, 'POINTS', 'PTS20250804008', 'SUCCESS', '2025-08-04 08:30:00', DEFAULT),
    ('PAY20250805009', 'ORD20250805009', 9, 6588.0, 'ALIPAY', 'ZFB20250805009', 'SUCCESS', '2025-08-05 10:00:00', DEFAULT),
    ('PAY20250806010', 'ORD20250806010', 10, 3700.0, 'WECHAT', 'WX20250806010', 'SUCCESS', '2025-08-06 15:30:00', DEFAULT),
    ('PAY20250807011', 'ORD20250807011', 11, 2300.0, 'CARD', 'CARD20250807011', 'SUCCESS', '2025-08-07 09:00:00', DEFAULT),
    ('PAY20250807012', 'ORD20250807012', 12, 8700.0, 'ALIPAY', 'ZFB20250807012', 'SUCCESS', '2025-08-07 13:20:00', DEFAULT),
    ('PAY20250808013', 'ORD20250808013', 1, 35.8, 'WECHAT', 'WX20250808013', 'SUCCESS', '2025-08-08 18:00:00', DEFAULT),
    ('PAY20250808014', 'ORD20250808014', 13, 9200.0, 'ALIPAY', 'ZFB20250808014', 'SUCCESS', '2025-08-08 20:00:00', DEFAULT),
    ('PAY20250808015', 'ORD20250808015', 1, 8499.0, 'WECHAT', 'WX20250808015', 'PENDING', '2025-08-08 20:01:00', DEFAULT),
    ('PAY20250808016', 'ORD20250808016', 16, 1299.0, 'ALIPAY', 'ZFB20250808016', 'SUCCESS', '2025-08-08 18:30:00', DEFAULT),
    ('PAY20250809017', 'ORD20250809017', 12, 849.0, 'WECHAT', 'WX20250809017', 'SUCCESS', '2025-08-09 11:20:00', DEFAULT),
    ('PAY20250809018', 'ORD20250809018', 10, 568.0, 'CARD', 'CARD20250809018', 'SUCCESS', '2025-08-09 09:45:00', DEFAULT),
    ('PAY20250810019', 'ORD20250810019', 22, 35.8, 'WECHAT', 'WX20250810019', 'SUCCESS', '2025-08-10 12:00:00', DEFAULT),
    ('PAY20250811020', 'ORD20250811020', 15, 151000.0, 'CARD', 'CARD20250811020', 'SUCCESS', '2025-08-11 14:30:00', DEFAULT),
    ('PAY20250812021', 'ORD20250812021', 2, 16999.0, 'ALIPAY', 'ZFB20250812021', 'SUCCESS', '2025-08-12 10:00:00', DEFAULT),
    ('PAY20250812022', 'ORD20250812022', 4, 899.0, 'WECHAT', 'WX20250812022', 'SUCCESS', '2025-08-12 16:00:00', DEFAULT),
    ('PAY20250813023', 'ORD20250813023', 6, 1080.0, 'ALIPAY', 'ZFB20250813023', 'SUCCESS', '2025-08-13 11:00:00', DEFAULT),
    ('PAY20250815025', 'ORD20250815025', 1, 89.0, 'WECHAT', 'WX20250815025', 'SUCCESS', '2025-08-15 08:00:00', DEFAULT),
    ('PAY20250815060', 'ORD20250815060', 17, 1299.0, 'CARD', 'CARD20250815060', 'SUCCESS', '2025-08-12 09:00:00', DEFAULT);

SET FOREIGN_KEY_CHECKS = 1;