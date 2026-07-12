-- ===========================================================
-- V2.9.1 AI 模型市场 + Webhook 集成 DDL
-- ===========================================================

-- AI 模型市场主表
CREATE TABLE IF NOT EXISTS `model_market` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `modelKey` VARCHAR(128) NOT NULL COMMENT 'URL slug, 唯一',
    `name` VARCHAR(128) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `modelType` VARCHAR(32) NOT NULL DEFAULT 'PYTORCH' COMMENT 'PYTORCH/TENSORFLOW/ONNX/SAFETENSORS/GGUF/OTHER',
    `taskType` VARCHAR(64) DEFAULT NULL COMMENT 'TEXT_CLASSIFICATION/NER/SUMMARIZATION/...',
    `baseModel` VARCHAR(128) DEFAULT NULL,
    `version` VARCHAR(16) NOT NULL DEFAULT '1.0.0',
    `filePath` VARCHAR(512) DEFAULT NULL COMMENT '本地文件路径',
    `fileName` VARCHAR(256) DEFAULT NULL,
    `fileSize` BIGINT NOT NULL DEFAULT 0,
    `sha256` VARCHAR(64) DEFAULT NULL,
    `license` VARCHAR(32) NOT NULL DEFAULT 'MIT' COMMENT 'MIT/APACHE_2_0/GPL_3/CC_BY_4/COMMERCIAL/PROPRIETARY',
    `authorId` BIGINT NOT NULL DEFAULT 0,
    `authorName` VARCHAR(64) DEFAULT NULL,
    `tags` VARCHAR(512) DEFAULT NULL,
    `metricsJson` TEXT COMMENT '指标 JSON',
    `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/DEPRECATED',
    `downloadCount` BIGINT NOT NULL DEFAULT 0,
    `avgRating` DOUBLE NOT NULL DEFAULT 0.0,
    `ratingCount` BIGINT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `publishedAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_modelKey` (`modelKey`),
    KEY `idx_modelType` (`modelType`),
    KEY `idx_taskType` (`taskType`),
    KEY `idx_status` (`status`),
    KEY `idx_author` (`authorId`),
    KEY `idx_rating` (`avgRating` DESC),
    KEY `idx_download` (`downloadCount` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 模型市场 V2.9.1';

-- 模型评分
CREATE TABLE IF NOT EXISTS `model_rating` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `modelKey` VARCHAR(128) NOT NULL,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `username` VARCHAR(64) DEFAULT NULL,
    `rating` INT(11) NOT NULL,
    `comment` VARCHAR(1024) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_user` (`modelKey`, `userId`),
    KEY `idx_created` (`createdAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型评分 V2.9.1';

-- Webhook 订阅
CREATE TABLE IF NOT EXISTS `webhook` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `webhookId` VARCHAR(64) NOT NULL,
    `name` VARCHAR(128) NOT NULL,
    `description` VARCHAR(512) DEFAULT NULL,
    `url` VARCHAR(512) NOT NULL,
    `events` VARCHAR(512) DEFAULT '*' COMMENT '逗号分隔,* = 全部',
    `secret` VARCHAR(64) DEFAULT NULL,
    `customHeaders` TEXT COMMENT '自定义 Header JSON',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PAUSED/DISABLED',
    `deliveryCount` BIGINT NOT NULL DEFAULT 0,
    `successCount` BIGINT NOT NULL DEFAULT 0,
    `failCount` BIGINT NOT NULL DEFAULT 0,
    `lastDeliveryAt` DATETIME DEFAULT NULL,
    `lastStatus` INT(11) DEFAULT NULL,
    `ownerId` BIGINT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_webhookId` (`webhookId`),
    KEY `idx_owner` (`ownerId`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Webhook 订阅 V2.9.1';

-- Webhook 投递日志
CREATE TABLE IF NOT EXISTS `webhook_delivery` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `webhookId` VARCHAR(64) NOT NULL,
    `eventType` VARCHAR(64) NOT NULL,
    `eventId` VARCHAR(64) DEFAULT NULL,
    `payload` MEDIUMTEXT,
    `responseStatus` INT(11) DEFAULT NULL,
    `responseBody` TEXT,
    `durationMs` BIGINT DEFAULT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED/RETRY',
    `retryCount` INT(11) NOT NULL DEFAULT 0,
    `errorMsg` VARCHAR(1024) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_webhook_time` (`webhookId`, `createdAt`),
    KEY `idx_event` (`eventType`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Webhook 投递日志 V2.9.1';

-- ===========================================================
-- 种子数据
-- ===========================================================

-- 3 个示例模型
INSERT INTO `model_market` (`modelKey`, `name`, `description`, `modelType`, `taskType`, `baseModel`, `version`, `fileSize`, `license`, `authorId`, `authorName`, `tags`, `status`, `downloadCount`, `avgRating`, `publishedAt`) VALUES
('chinese-sentiment-bert-1234', '中文情感分析 BERT', '基于 bert-base-chinese 微调, 准确率 95%', 'PYTORCH', 'TEXT_CLASSIFICATION', 'bert-base-chinese', '1.0.0', 411000000, 'MIT', 1, 'adminLiugl', 'BERT,情感,中文,NLP', 'PUBLISHED', 152, 4.8, NOW()),
('minimax-7b-gguf-5678', 'MiniMax-7B 量化版', 'GGUF 量化 (Q4_K_M), 4GB 显存即可推理', 'GGUF', 'TEXT_GENERATION', 'minimax-7b', '1.0.0', 4100000000, 'APACHE_2_0', 1, 'adminLiugl', 'LLM,量化,7B,CPU', 'PUBLISHED', 89, 4.6, NOW()),
('product-ner-roberta-9999', '电商商品 NER', 'RoBERTa 微调, 识别品牌/型号/价格', 'SAFETENSORS', 'NER', 'hfl/chinese-roberta-wwm-ext', '1.0.0', 408000000, 'MIT', 1, 'adminLiugl', 'NER,电商,RoBERTa', 'PUBLISHED', 67, 4.5, NOW());

-- 1 个示例 webhook
INSERT INTO `webhook` (`webhookId`, `name`, `description`, `url`, `events`, `secret`, `enabled`, `status`, `ownerId`) VALUES
('wh_demo01xxxxxxxx', 'Demo Slack Notifier', '示例: 事件发到 Slack 通知频道', 'https://example.com/webhook-receiver', 'MODEL_TRAINED,AGENT_PUBLISHED,ALERT_TRIGGERED', 'wh_secret_demo_placeholder', 1, 'ACTIVE', 1);
