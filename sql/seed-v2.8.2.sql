-- ============================================================
-- 种子数据 (V2.8.2)
-- 必须在 schema-v2.8.2.sql 之后执行
-- ============================================================

-- 1. 默认租户
INSERT INTO `tenant` (`id`, `tenantCode`, `name`, `status`, `createdAt`, `updatedAt`)
VALUES (1, 'default', '默认租户', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE name='默认租户';

-- 2. 默认角色
INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `createdAt`)
VALUES
(1, 'SUPER_ADMIN', '超级管理员', '拥有所有权限', NOW()),
(2, 'ADMIN',      '管理员',     '除 super 外的所有权限', NOW()),
(3, 'USER',       '普通用户',   '基础功能权限', NOW()),
(4, 'GUEST',      '访客',       '只读权限', NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 3. 默认用户 (密码 BCrypt 加密, admin@123 / Liugl@2026)
-- BCrypt: admin@123 -> $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- BCrypt: Liugl@2026 -> $2a$10$dummy1234567890abcdefghijklmnopqrstuvwxyz1234567890abcd
-- (实际部署时通过 AuthService.register 重新加密)
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `status`, `tenantId`, `isSuperAdmin`, `createdAt`, `updatedAt`)
VALUES
(1, 'adminLiugl', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '超级管理员', 'adminLiugl@minimax.com', 'ACTIVE', 1, 1, NOW(), NOW()),
(2, 'admin',      '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '管理员',     'admin@minimax.com',      'ACTIVE', 1, 0, NOW(), NOW()),
(3, 'user',       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '普通用户',   'user@minimax.com',       'ACTIVE', 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE nickname=VALUES(nickname);

-- 4. 用户-角色关系
INSERT INTO `sys_user_role` (`userId`, `roleId`)
VALUES
(1, 1),  -- adminLiugl -> SUPER_ADMIN
(2, 2),  -- admin -> ADMIN
(3, 3)   -- user -> USER
ON DUPLICATE KEY UPDATE roleId=VALUES(roleId);

-- 5. AI 工具 (9 个内置)
INSERT INTO `ai_tool` (`code`, `name`, `description`, `category`, `enabled`, `version`, `inputSchema`, `createdAt`, `updatedAt`)
VALUES
('data.clean.missing',      '缺失值处理',     '均值/中位数/众数填充或删除',     'data.clean',   1, '1.0.0', '{"type":"object","properties":{"dataSourceId":{"type":"number"},"table":{"type":"string"},"column":{"type":"string"},"strategy":{"type":"string"}}}', NOW(), NOW()),
('data.clean.deduplicate',  '去重',           '按主键去重, HashMap 内存索引',   'data.clean',   1, '1.0.0', '{"type":"object","properties":{"dataSourceId":{"type":"number"},"table":{"type":"string"},"keyColumns":{"type":"array"}}}', NOW(), NOW()),
('data.analyze.stats',      '基础统计',       '均值/方差/分位数/极值',         'data.analyze', 1, '1.0.0', '{"type":"object","properties":{"dataSourceId":{"type":"number"},"table":{"type":"string"},"columns":{"type":"array"}}}', NOW(), NOW()),
('data.analyze.trend',      '趋势分析',       '线性回归 + R² 拟合优度',         'data.analyze', 1, '1.0.0', '{"type":"object","properties":{"dataSourceId":{"type":"number"},"table":{"type":"string"},"column":{"type":"string"}}}', NOW(), NOW()),
('data.analyze.anomaly',    '异常检测',       'Z-Score / IQR / 移动平均',       'data.analyze', 1, '1.0.0', '{"type":"object","properties":{"dataSourceId":{"type":"number"},"table":{"type":"string"},"column":{"type":"string"},"method":{"type":"string"}}}', NOW(), NOW()),
('data.analyze.distribution','分布分析',      '直方图 + 偏度 + 峰度',           'data.analyze', 1, '1.0.0', '{"type":"object","properties":{"dataSourceId":{"type":"number"},"table":{"type":"string"},"column":{"type":"string"},"buckets":{"type":"number"}}}', NOW(), NOW()),
('sql.query',               'NL2SQL',         '自然语言转 SQL 查询',           'data.analyze', 1, '1.0.0', '{"type":"object","properties":{"dataSourceId":{"type":"number"},"question":{"type":"string"}}}', NOW(), NOW()),
('code.gen.from-schema',    '代码生成',       '数据库表 → Spring Boot 项目 ZIP', 'code.gen',     1, '1.0.0', '{"type":"object","properties":{"dataSourceId":{"type":"number"},"table":{"type":"string"},"projectName":{"type":"string"},"basePackage":{"type":"string"}}}', NOW(), NOW()),
('chat.assistant',          '对话助手',       '通用 AI 对话, 多轮记忆',         'chat',         1, '1.0.0', '{"type":"object","properties":{"message":{"type":"string"},"sessionId":{"type":"string"}}}', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name), description=VALUES(description);

-- 6. 告警规则
INSERT INTO `alert_rule` (`name`, `description`, `metricName`, `operator`, `threshold`, `severity`, `cooldownMinutes`, `enabled`, `createdAt`, `updatedAt`)
VALUES
('CPU 过高',         'CPU 使用率超过 80%',     'cpu_usage',     '>', 0.8,  'warning',  5, 1, NOW(), NOW()),
('内存满',           '内存使用率超过 90%',     'memory_usage',  '>', 0.9,  'critical', 5, 1, NOW(), NOW()),
('JVM 堆满',         'JVM 堆使用率超过 85%',   'jvm_heap_usage','>', 0.85, 'warning',  5, 1, NOW(), NOW()),
('API 错误率高',     'API 错误率超过 5%',      'api_error_rate','>', 0.05, 'critical', 1, 1, NOW(), NOW()),
('响应慢',           '平均响应时间超过 3 秒',   'response_time', '>', 3000, 'warning',  5, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE description=VALUES(description);

-- 7. 告警渠道
INSERT INTO `alert_channel` (`name`, `channelType`, `target`, `description`, `enabled`, `createdAt`)
VALUES
('运维钉钉群', 'dingtalk', 'https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN', '运维告警', 1, NOW()),
('运维邮箱',   'email',    'ops@example.com',                                              '运维邮箱', 1, NOW())
ON DUPLICATE KEY UPDATE target=VALUES(target);

-- 8. 敏感词 (示例)
INSERT INTO `sensitive_word` (`word`, `category`, `level`, `action`, `createdAt`)
VALUES
('暴力',     'violance',  'high',   'block',  NOW()),
('色情',     'porn',      'high',   'block',  NOW()),
('赌博',     'gambling',  'medium', 'review', NOW()),
('诈骗',     'fraud',     'high',   'block',  NOW()),
('反动',     'political', 'high',   'block',  NOW()),
('广告',     'spam',      'low',    'review', NOW())
ON DUPLICATE KEY UPDATE category=VALUES(category);

-- 9. Prompt 模板
INSERT INTO `prompt_template` (`name`, `category`, `content`, `variables`, `description`, `createdAt`, `updatedAt`)
VALUES
('通用助手',   'chat',     '你是 MiniMax AI 助手, 友好专业, 简洁回答用户问题。',           '[]',  '通用对话模板',     NOW(), NOW()),
('代码审查',   'code',     '请审查以下代码, 指出潜在 bug, 性能问题, 安全漏洞:\n{code}',  '["code"]', '代码审查',     NOW(), NOW()),
('数据总结',   'data',     '请总结以下数据的关键发现:\n{data}',                          '["data"]', '数据总结',     NOW(), NOW()),
('翻译',       'translate','请将以下内容翻译成 {target_lang}:\n{text}',                 '["text","target_lang"]', '翻译', NOW(), NOW())
ON DUPLICATE KEY UPDATE content=VALUES(content);

-- 10. AI 会话示例
INSERT INTO `ai_chat_session` (`id`, `sessionId`, `userId`, `title`, `createdAt`, `updatedAt`, `deleted`)
VALUES
(1, 'demo-session-1', 1, '欢迎使用 MiniMax AI', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE title=VALUES(title);

INSERT INTO `ai_chat_message` (`sessionId`, `role`, `content`, `createdAt`)
VALUES
(1, 'system', '你是 MiniMax AI 助手, 友好专业, 简洁回答用户问题。', NOW()),
(1, 'assistant', '你好! 我是 MiniMax AI, 有什么可以帮你?', NOW())
ON DUPLICATE KEY UPDATE content=VALUES(content);

-- 11. 系统通知
INSERT INTO `notification` (`userId`, `type`, `title`, `content`, `status`, `createdAt`)
VALUES
(1, 'system', '欢迎使用 MiniMax Platform V2.8.2', '已完成 5 个版本升级, 新增训练可视化/AIGC 视频/音乐/文档解析/RBAC/CI/CD 等企业级能力。', 'unread', NOW()),
(2, 'system', '管理员您好', '请在 ⚙️ 系统 中配置告警渠道, 确保异常及时通知。', 'unread', NOW()),
(3, 'system', '用户您好', '可使用 9 个内置 AI 工具 (清洗/分析/代码生成/对话), 试试看!', 'unread', NOW())
ON DUPLICATE KEY UPDATE title=VALUES(title);

-- 12. 审计日志示例
INSERT INTO `audit_log_full` (`traceId`, `userId`, `username`, `userIp`, `action`, `resourceType`, `method`, `path`, `responseStatus`, `result`, `durationMs`, `createdAt`)
VALUES
('trace-001', 1, 'adminLiugl', '127.0.0.1', 'LOGIN',         'user',    'POST', '/api/v1/auth/login',  200, 'SUCCESS', 120,  NOW()),
('trace-002', 1, 'adminLiugl', '127.0.0.1', 'AI_GENERATE',   'ai',      'POST', '/api/ai/generate',    200, 'SUCCESS', 850,  NOW()),
('trace-003', 2, 'admin',      '192.168.1.10', 'EXPORT_DATA', 'order',   'GET',  '/api/v1/orders/export', 200, 'SUCCESS', 2300, NOW()),
('trace-004', 2, 'admin',      '192.168.1.10', 'FILE_UPLOAD', 'file',   'POST', '/api/multimodal/upload', 200, 'SUCCESS', 450, NOW()),
('trace-005', 3, 'user',       '192.168.1.20', 'TOOL_INVOKE', 'ai',     'POST', '/api/ai/tools/sql.query/invoke', 200, 'SUCCESS', 320, NOW())
ON DUPLICATE KEY UPDATE content=VALUES(content);

-- 完成
SELECT 'V2.8.2 种子数据导入完成' AS status;
SELECT COUNT(*) AS total_tables FROM information_schema.tables WHERE table_schema = DATABASE();
