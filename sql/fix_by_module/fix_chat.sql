-- =============================================================
-- MiniMax Platform V3.5.5+ 修复 SQL - chat 模块
-- 共 2 张表, 13 字段
-- 用法: mysql -uroot -proot123456 minimax_platform < fix_chat.sql
-- 自动生成: scripts/split_fix_sql_by_module.py
-- =============================================================

-- 表: chat_message
ALTER TABLE `chat_message` ADD COLUMN `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)';
ALTER TABLE `chat_message` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `chat_message` ADD COLUMN `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)';
ALTER TABLE `chat_message` ADD COLUMN `finishReason` VARCHAR(255) DEFAULT NULL COMMENT 'finishReason(finishReason)';
ALTER TABLE `chat_message` ADD COLUMN `tokens` INT DEFAULT 0 COMMENT 'tokens(tokens)';

-- 表: chat_session
ALTER TABLE `chat_session` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `chat_session` ADD COLUMN `lastMessageAt` DATETIME DEFAULT NULL COMMENT 'lastMessageAt(lastMessageAt)';
ALTER TABLE `chat_session` ADD COLUMN `messageCount` INT DEFAULT 0 COMMENT 'messageCount(messageCount)';
ALTER TABLE `chat_session` ADD COLUMN `model` VARCHAR(255) DEFAULT NULL COMMENT 'model(model)';
ALTER TABLE `chat_session` ADD COLUMN `status` INT DEFAULT 0 COMMENT 'status(status)';
ALTER TABLE `chat_session` ADD COLUMN `systemPrompt` VARCHAR(255) DEFAULT NULL COMMENT 'systemPrompt(systemPrompt)';
ALTER TABLE `chat_session` ADD COLUMN `temperature` DECIMAL(20,4) DEFAULT 0 COMMENT 'temperature(temperature)';
ALTER TABLE `chat_session` ADD COLUMN `tenantId` BIGINT DEFAULT 0 COMMENT 'tenantId(tenantId)';

