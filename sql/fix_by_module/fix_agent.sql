-- =============================================================
-- MiniMax Platform V3.5.5+ 修复 SQL - agent 模块
-- 共 6 张表, 45 字段
-- 用法: mysql -uroot -proot123456 minimax_platform < fix_agent.sql
-- 自动生成: scripts/split_fix_sql_by_module.py
-- =============================================================

-- 表: agent_task
ALTER TABLE `agent_task` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `agent_task` ADD COLUMN `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)';
ALTER TABLE `agent_task` ADD COLUMN `goal` VARCHAR(255) DEFAULT NULL COMMENT 'goal(goal)';
ALTER TABLE `agent_task` ADD COLUMN `latencyMs` BIGINT DEFAULT 0 COMMENT 'latencyMs(latencyMs)';
ALTER TABLE `agent_task` ADD COLUMN `llmCalls` INT DEFAULT 0 COMMENT 'llmCalls(llmCalls)';
ALTER TABLE `agent_task` ADD COLUMN `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)';
ALTER TABLE `agent_task` ADD COLUMN `rounds` INT DEFAULT 0 COMMENT 'rounds(rounds)';
ALTER TABLE `agent_task` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `agent_task` ADD COLUMN `toolCalls` INT DEFAULT 0 COMMENT 'toolCalls(toolCalls)';
ALTER TABLE `agent_task` ADD COLUMN `totalTokens` INT DEFAULT 0 COMMENT 'totalTokens(totalTokens)';
ALTER TABLE `agent_task` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';

-- 表: collab_member
ALTER TABLE `collab_member` ADD COLUMN `joinedAt` DATETIME DEFAULT NULL COMMENT 'joinedAt(joinedAt)';
ALTER TABLE `collab_member` ADD COLUMN `lastActiveAt` DATETIME DEFAULT NULL COMMENT 'lastActiveAt(lastActiveAt)';

-- 表: collab_session
ALTER TABLE `collab_session` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `collab_session` ADD COLUMN `maxUsers` INT DEFAULT 0 COMMENT 'maxUsers(maxUsers)';
ALTER TABLE `collab_session` ADD COLUMN `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)';
ALTER TABLE `collab_session` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `collab_session` ADD COLUMN `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)';

-- 表: kg_entity
ALTER TABLE `kg_entity` ADD COLUMN `aliases` VARCHAR(255) DEFAULT NULL COMMENT 'aliases(aliases)';
ALTER TABLE `kg_entity` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `kg_entity` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `kg_entity` ADD COLUMN `entityType` VARCHAR(255) DEFAULT NULL COMMENT 'entityType(entityType)';
ALTER TABLE `kg_entity` ADD COLUMN `importance` INT DEFAULT 0 COMMENT 'importance(importance)';
ALTER TABLE `kg_entity` ADD COLUMN `refCount` INT DEFAULT 0 COMMENT 'refCount(refCount)';
ALTER TABLE `kg_entity` ADD COLUMN `source` VARCHAR(255) DEFAULT NULL COMMENT 'source(source)';

-- 表: kg_relation
ALTER TABLE `kg_relation` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `kg_relation` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `kg_relation` ADD COLUMN `refCount` INT DEFAULT 0 COMMENT 'refCount(refCount)';
ALTER TABLE `kg_relation` ADD COLUMN `source` VARCHAR(255) DEFAULT NULL COMMENT 'source(source)';
ALTER TABLE `kg_relation` ADD COLUMN `weight` DECIMAL(20,4) DEFAULT 0 COMMENT 'weight(weight)';

-- 表: plugin
ALTER TABLE `plugin` ADD COLUMN `author` VARCHAR(255) DEFAULT NULL COMMENT 'author(author)';
ALTER TABLE `plugin` ADD COLUMN `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)';
ALTER TABLE `plugin` ADD COLUMN `config` VARCHAR(255) DEFAULT NULL COMMENT 'config(config)';
ALTER TABLE `plugin` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `plugin` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `plugin` ADD COLUMN `displayName` VARCHAR(255) DEFAULT NULL COMMENT 'displayName(displayName)';
ALTER TABLE `plugin` ADD COLUMN `downloads` INT DEFAULT 0 COMMENT 'downloads(downloads)';
ALTER TABLE `plugin` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `plugin` ADD COLUMN `entry` VARCHAR(255) DEFAULT NULL COMMENT 'entry(entry)';
ALTER TABLE `plugin` ADD COLUMN `icon` VARCHAR(255) DEFAULT NULL COMMENT 'icon(icon)';
ALTER TABLE `plugin` ADD COLUMN `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)';
ALTER TABLE `plugin` ADD COLUMN `pluginType` VARCHAR(255) DEFAULT NULL COMMENT 'pluginType(pluginType)';
ALTER TABLE `plugin` ADD COLUMN `rating` DECIMAL(20,4) DEFAULT 0 COMMENT 'rating(rating)';
ALTER TABLE `plugin` ADD COLUMN `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope(scope)';
ALTER TABLE `plugin` ADD COLUMN `version` VARCHAR(255) DEFAULT NULL COMMENT 'version(version)';

