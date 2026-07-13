-- =============================================================
-- MiniMax Platform V3.5.5+ 修复 SQL - monitor 模块
-- 共 4 张表, 28 字段
-- 用法: mysql -uroot -proot123456 minimax_platform < fix_monitor.sql
-- 自动生成: scripts/split_fix_sql_by_module.py
-- =============================================================

-- 表: alert_channel
ALTER TABLE `alert_channel` ADD COLUMN `channelType` VARCHAR(255) DEFAULT NULL COMMENT 'channelType(channelType)';
ALTER TABLE `alert_channel` ADD COLUMN `config` VARCHAR(255) DEFAULT NULL COMMENT 'config(config)';
ALTER TABLE `alert_channel` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `alert_channel` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `alert_channel` ADD COLUMN `priority` INT DEFAULT 0 COMMENT 'priority(priority)';
ALTER TABLE `alert_channel` ADD COLUMN `target` VARCHAR(255) DEFAULT NULL COMMENT 'target(target)';
ALTER TABLE `alert_channel` ADD COLUMN `type` VARCHAR(255) DEFAULT NULL COMMENT 'type(type)';

-- 表: alert_event
ALTER TABLE `alert_event` ADD COLUMN `ackedAt` DATETIME DEFAULT NULL COMMENT 'ackedAt(ackedAt)';
ALTER TABLE `alert_event` ADD COLUMN `ackedBy` BIGINT DEFAULT 0 COMMENT 'ackedBy(ackedBy)';
ALTER TABLE `alert_event` ADD COLUMN `duration` BIGINT DEFAULT 0 COMMENT 'duration(duration)';
ALTER TABLE `alert_event` ADD COLUMN `firedAt` DATETIME DEFAULT NULL COMMENT 'firedAt(firedAt)';
ALTER TABLE `alert_event` ADD COLUMN `message` VARCHAR(255) DEFAULT NULL COMMENT 'message(message)';
ALTER TABLE `alert_event` ADD COLUMN `metricName` VARCHAR(255) DEFAULT NULL COMMENT 'metricName(metricName)';
ALTER TABLE `alert_event` ADD COLUMN `resolvedAt` DATETIME DEFAULT NULL COMMENT 'resolvedAt(resolvedAt)';
ALTER TABLE `alert_event` ADD COLUMN `severity` VARCHAR(255) DEFAULT NULL COMMENT 'severity(severity)';
ALTER TABLE `alert_event` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';

-- 表: alert_rule
ALTER TABLE `alert_rule` ADD COLUMN `cooldownMinutes` INT DEFAULT 0 COMMENT 'cooldownMinutes(cooldownMinutes)';
ALTER TABLE `alert_rule` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `alert_rule` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `alert_rule` ADD COLUMN `metricName` VARCHAR(255) DEFAULT NULL COMMENT 'metricName(metricName)';
ALTER TABLE `alert_rule` ADD COLUMN `notifyChannel` VARCHAR(255) DEFAULT NULL COMMENT 'notifyChannel(notifyChannel)';
ALTER TABLE `alert_rule` ADD COLUMN `operator` VARCHAR(255) DEFAULT NULL COMMENT 'operator(operator)';
ALTER TABLE `alert_rule` ADD COLUMN `service` VARCHAR(255) DEFAULT NULL COMMENT 'service(service)';
ALTER TABLE `alert_rule` ADD COLUMN `severity` VARCHAR(255) DEFAULT NULL COMMENT 'severity(severity)';
ALTER TABLE `alert_rule` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';

-- 表: metric_snapshot
ALTER TABLE `metric_snapshot` ADD COLUMN `metricName` VARCHAR(255) DEFAULT NULL COMMENT 'metricName(metricName)';
ALTER TABLE `metric_snapshot` ADD COLUMN `metricValue` DECIMAL(20,4) DEFAULT 0 COMMENT 'metricValue(metricValue)';
ALTER TABLE `metric_snapshot` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';

