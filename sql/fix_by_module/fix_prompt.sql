-- =============================================================
-- MiniMax Platform V3.5.5+ 修复 SQL - prompt 模块
-- 共 1 张表, 9 字段
-- 用法: mysql -uroot -proot123456 minimax_platform < fix_prompt.sql
-- 自动生成: scripts/split_fix_sql_by_module.py
-- =============================================================

-- 表: prompt_template
ALTER TABLE `prompt_template` ADD COLUMN `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)';
ALTER TABLE `prompt_template` ADD COLUMN `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)';
ALTER TABLE `prompt_template` ADD COLUMN `creatorId` BIGINT DEFAULT 0 COMMENT 'creatorId(creatorId)';
ALTER TABLE `prompt_template` ADD COLUMN `creatorName` VARCHAR(255) DEFAULT NULL COMMENT 'creatorName(creatorName)';
ALTER TABLE `prompt_template` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `prompt_template` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `prompt_template` ADD COLUMN `isPublic` TINYINT(1) DEFAULT 0 COMMENT 'isPublic(isPublic)';
ALTER TABLE `prompt_template` ADD COLUMN `useCount` INT DEFAULT 0 COMMENT 'useCount(useCount)';
ALTER TABLE `prompt_template` ADD COLUMN `variables` VARCHAR(255) DEFAULT NULL COMMENT 'variables(variables)';

