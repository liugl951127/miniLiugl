-- =============================================================
-- MiniMax Platform V3.5.5+ 修复 SQL - admin 模块
-- 共 2 张表, 22 字段
-- 用法: mysql -uroot -proot123456 minimax_platform < fix_admin.sql
-- 自动生成: scripts/split_fix_sql_by_module.py
-- =============================================================

-- 表: admin_audit_log
ALTER TABLE `admin_audit_log` ADD COLUMN `action` VARCHAR(255) DEFAULT NULL COMMENT 'action(action)';
ALTER TABLE `admin_audit_log` ADD COLUMN `detail` VARCHAR(255) DEFAULT NULL COMMENT 'detail(detail)';
ALTER TABLE `admin_audit_log` ADD COLUMN `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)';
ALTER TABLE `admin_audit_log` ADD COLUMN `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip(ip)';
ALTER TABLE `admin_audit_log` ADD COLUMN `resourceId` VARCHAR(255) DEFAULT NULL COMMENT 'resourceId(resourceId)';
ALTER TABLE `admin_audit_log` ADD COLUMN `resourceType` VARCHAR(255) DEFAULT NULL COMMENT 'resourceType(resourceType)';
ALTER TABLE `admin_audit_log` ADD COLUMN `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)';
ALTER TABLE `admin_audit_log` ADD COLUMN `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)';

-- 表: audit_log_full
ALTER TABLE `audit_log_full` ADD COLUMN `action` VARCHAR(255) DEFAULT NULL COMMENT 'action(action)';
ALTER TABLE `audit_log_full` ADD COLUMN `durationMs` INT DEFAULT 0 COMMENT 'durationMs(durationMs)';
ALTER TABLE `audit_log_full` ADD COLUMN `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)';
ALTER TABLE `audit_log_full` ADD COLUMN `method` VARCHAR(255) DEFAULT NULL COMMENT 'method(method)';
ALTER TABLE `audit_log_full` ADD COLUMN `path` VARCHAR(255) DEFAULT NULL COMMENT 'path(path)';
ALTER TABLE `audit_log_full` ADD COLUMN `requestBody` VARCHAR(255) DEFAULT NULL COMMENT 'requestBody(requestBody)';
ALTER TABLE `audit_log_full` ADD COLUMN `resourceId` VARCHAR(255) DEFAULT NULL COMMENT 'resourceId(resourceId)';
ALTER TABLE `audit_log_full` ADD COLUMN `resourceType` VARCHAR(255) DEFAULT NULL COMMENT 'resourceType(resourceType)';
ALTER TABLE `audit_log_full` ADD COLUMN `responseStatus` INT DEFAULT 0 COMMENT 'responseStatus(responseStatus)';
ALTER TABLE `audit_log_full` ADD COLUMN `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)';
ALTER TABLE `audit_log_full` ADD COLUMN `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)';
ALTER TABLE `audit_log_full` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `audit_log_full` ADD COLUMN `userIp` VARCHAR(255) DEFAULT NULL COMMENT 'userIp(userIp)';
ALTER TABLE `audit_log_full` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';

