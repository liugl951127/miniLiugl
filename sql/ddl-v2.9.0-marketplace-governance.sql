-- ===========================================================
-- V2.9.0 Agent Marketplace + Admin 治理 DDL
-- ===========================================================

-- Agent Marketplace 主表
CREATE TABLE IF NOT EXISTS `agent_marketplace` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `agentKey` VARCHAR(128) NOT NULL COMMENT 'URL slug, 唯一',
    `name` VARCHAR(128) NOT NULL COMMENT '显示名',
    `description` VARCHAR(1024) DEFAULT NULL COMMENT '描述',
    `category` VARCHAR(32) NOT NULL DEFAULT 'CUSTOM' COMMENT '分类',
    `icon` VARCHAR(32) DEFAULT '🤖' COMMENT 'emoji 图标',
    `authorId` BIGINT NOT NULL DEFAULT 0 COMMENT '作者用户ID',
    `authorName` VARCHAR(64) DEFAULT NULL COMMENT '作者用户名',
    `definitionJson` MEDIUMTEXT NOT NULL COMMENT 'Agent 定义 JSON',
    `version` VARCHAR(16) NOT NULL DEFAULT '1.0.0' COMMENT 'semver',
    `visibility` VARCHAR(16) NOT NULL DEFAULT 'PRIVATE' COMMENT 'PUBLIC/PRIVATE/UNLISTED',
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/PUBLISHED/REJECTED',
    `usageCount` BIGINT NOT NULL DEFAULT 0 COMMENT '使用次数',
    `avgRating` DOUBLE NOT NULL DEFAULT 0.0 COMMENT '平均评分 0-5',
    `ratingCount` BIGINT NOT NULL DEFAULT 0 COMMENT '评分次数',
    `tags` VARCHAR(512) DEFAULT NULL COMMENT '标签,逗号分隔',
    `capabilities` VARCHAR(512) DEFAULT NULL COMMENT '能力列表,逗号分隔',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `publishedAt` DATETIME DEFAULT NULL COMMENT '审核通过时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agentKey` (`agentKey`),
    KEY `idx_category` (`category`),
    KEY `idx_status_vis` (`status`, `visibility`),
    KEY `idx_author` (`authorId`),
    KEY `idx_rating` (`avgRating` DESC),
    KEY `idx_usage` (`usageCount` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent Marketplace V2.9.0';

-- Agent 评分
CREATE TABLE IF NOT EXISTS `agent_rating` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `agentKey` VARCHAR(128) NOT NULL,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `username` VARCHAR(64) DEFAULT NULL,
    `rating` INT(11) NOT NULL COMMENT '1-5',
    `comment` VARCHAR(1024) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_user` (`agentKey`, `userId`),
    KEY `idx_created` (`createdAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 评分 V2.9.0';

-- 数据保留策略 (V2.9.0 治理)
CREATE TABLE IF NOT EXISTS `data_retention_policy` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tableName` VARCHAR(64) NOT NULL,
    `displayName` VARCHAR(128) DEFAULT NULL,
    `retentionDays` INT(11) NOT NULL DEFAULT 90,
    `archiveEnabled` TINYINT(1) NOT NULL DEFAULT 0,
    `lastCleanupAt` DATETIME DEFAULT NULL,
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_table` (`tableName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据保留策略 V2.9.0';

-- 异常检测规则
CREATE TABLE IF NOT EXISTS `anomaly_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(128) NOT NULL,
    `type` VARCHAR(32) NOT NULL COMMENT 'HIGH_FAIL_IP / BURST_USER / UNAUTHORIZED_DELETE / ...',
    `threshold` INT(11) NOT NULL DEFAULT 100,
    `timeWindow` INT(11) NOT NULL DEFAULT 60 COMMENT '秒',
    `severity` VARCHAR(16) NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW/MEDIUM/HIGH/CRITICAL',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `action` VARCHAR(64) DEFAULT 'LOG' COMMENT 'LOG / ALERT / BLOCK',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常检测规则 V2.9.0';

-- ===========================================================
-- 种子数据
-- ===========================================================

-- Agent Marketplace 示例
INSERT INTO `agent_marketplace` (`agentKey`, `name`, `description`, `category`, `icon`, `authorId`, `authorName`, `definitionJson`, `version`, `visibility`, `status`, `usageCount`, `avgRating`, `ratingCount`, `tags`, `capabilities`, `publishedAt`) VALUES
('travel-planner-1234', '智能旅行规划师', '基于 LBS 推荐景点/酒店/餐厅, 行程自动优化', 'TRAVEL', '✈️', 1, 'adminLiugl',
 '{"capabilities":["travel_plan","poi_search","hotel_search"],"tools":["LocationAwareTool","GeoUtils"],"systemPrompt":"你是专业旅行规划师"}',
 '1.0.0', 'PUBLIC', 'PUBLISHED', 0, 4.8, 0, '旅行,LBS,推荐', 'travel_plan,poi_search', NOW()),
('code-reviewer-5678', 'AI 代码审查', '自动审查 PR, 提示潜在 bug / 性能 / 安全', 'PRODUCTIVITY', '🔍', 1, 'adminLiugl',
 '{"capabilities":["code_review","static_analysis"],"tools":["AbstractSimpleTool"],"systemPrompt":"你是高级代码审查员"}',
 '1.0.0', 'PUBLIC', 'PUBLISHED', 0, 4.5, 0, '代码,审查,自动化', 'code_review,static_analysis', NOW()),
('chinese-poet-9999', '古诗词助手', '古典诗词创作/赏析/典故解释', 'EDUCATION', '📜', 1, 'adminLiugl',
 '{"capabilities":["poem_create","poem_analyze","allusion_explain"],"tools":[],"systemPrompt":"你是古诗词大师"}',
 '1.0.0', 'PUBLIC', 'PUBLISHED', 0, 4.9, 0, '诗词,教育,文化', 'poem_create,poem_analyze', NOW());

-- 异常检测规则种子
INSERT INTO `anomaly_rule` (`name`, `type`, `threshold`, `timeWindow`, `severity`, `enabled`, `action`) VALUES
('高频失败用户', 'HIGH_FAIL_USER', 10, 3600, 'HIGH', 1, 'ALERT'),
('异常 IP 高频', 'SUSPICIOUS_IP', 1000, 3600, 'CRITICAL', 1, 'ALERT'),
('短时间突发', 'BURST_USER', 50, 60, 'MEDIUM', 1, 'LOG'),
('越权删除尝试', 'UNAUTHORIZED_DELETE', 3, 600, 'HIGH', 1, 'ALERT'),
('异地登录', 'ABNORMAL_LOCATION', 1, 60, 'MEDIUM', 1, 'ALERT');

-- 数据保留策略种子
INSERT INTO `data_retention_policy` (`tableName`, `displayName`, `retentionDays`, `archiveEnabled`, `enabled`) VALUES
('audit_log_full', '审计日志', 90, 1, 1),
('chat_message', '聊天记录', 365, 1, 1),
('auth_login_log', '登录日志', 180, 0, 1),
('collab_message', '协作消息', 180, 1, 1),
('tensorboard_run', 'TensorBoard runs', 30, 0, 1);
