-- =============================================================
-- MiniMax Platform V3.5.5+ 修复 SQL - function 模块
-- 共 2 张表, 18 字段
-- 用法: mysql -uroot -proot123456 minimax_platform < fix_function.sql
-- 自动生成: scripts/split_fix_sql_by_module.py
-- =============================================================

-- 表: function_call_log
ALTER TABLE `function_call_log` ADD COLUMN `arguments` VARCHAR(255) DEFAULT NULL COMMENT 'arguments(arguments)';
ALTER TABLE `function_call_log` ADD COLUMN `durationMs` INT DEFAULT 0 COMMENT 'durationMs(durationMs)';
ALTER TABLE `function_call_log` ADD COLUMN `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)';
ALTER TABLE `function_call_log` ADD COLUMN `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip(ip)';
ALTER TABLE `function_call_log` ADD COLUMN `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)';
ALTER TABLE `function_call_log` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `function_call_log` ADD COLUMN `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)';

-- 表: function_tool
ALTER TABLE `function_tool` ADD COLUMN `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)';
ALTER TABLE `function_tool` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `function_tool` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `function_tool` ADD COLUMN `displayName` VARCHAR(255) DEFAULT NULL COMMENT 'displayName(displayName)';
ALTER TABLE `function_tool` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `function_tool` ADD COLUMN `endpoint` VARCHAR(255) DEFAULT NULL COMMENT 'endpoint(endpoint)';
ALTER TABLE `function_tool` ADD COLUMN `httpMethod` VARCHAR(255) DEFAULT NULL COMMENT 'httpMethod(httpMethod)';
ALTER TABLE `function_tool` ADD COLUMN `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)';
ALTER TABLE `function_tool` ADD COLUMN `parameters` VARCHAR(255) DEFAULT NULL COMMENT 'parameters(parameters)';
ALTER TABLE `function_tool` ADD COLUMN `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope(scope)';
ALTER TABLE `function_tool` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';

