-- ============================================================
-- V2.7.3 告警 + 审计表
-- ============================================================
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 告警规则 (与现有 AlertRule 实体保持一致)
DROP TABLE IF EXISTS `alert_rule`;
CREATE TABLE `alert_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(128) NOT NULL,
    `description` VARCHAR(512) DEFAULT NULL,
    `metric_name` VARCHAR(64) NOT NULL COMMENT 'cpu_usage/memory_usage/...',
    `service` VARCHAR(64) DEFAULT NULL COMMENT '服务名 (可选)',
    `operator` VARCHAR(8) NOT NULL,
    `threshold` DECIMAL(20,4) NOT NULL,
    `severity` VARCHAR(16) NOT NULL DEFAULT 'warning',
    `cooldown_minutes` INT DEFAULT 5,
    `enabled` TINYINT DEFAULT 1,
    `tags` VARCHAR(512) DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_enabled` (`enabled`),
    KEY `idx_metric` (`metric_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则';

DROP TABLE IF EXISTS `alert_channel`;
CREATE TABLE `alert_channel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(128) NOT NULL,
    `type` VARCHAR(32) NOT NULL,
    `target` VARCHAR(512) NOT NULL,
    `config` TEXT,
    `enabled` TINYINT DEFAULT 1,
    `description` VARCHAR(512) DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警渠道';

DROP TABLE IF EXISTS `alert_event`;
CREATE TABLE `alert_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `rule_id` BIGINT NOT NULL,
    `rule_name` VARCHAR(128) DEFAULT NULL,
    `severity` VARCHAR(16) DEFAULT NULL,
    `metric_name` VARCHAR(64) DEFAULT NULL,
    `metric_value` DECIMAL(20,4) DEFAULT NULL,
    `threshold` DECIMAL(20,4) DEFAULT NULL,
    `message` VARCHAR(1024) DEFAULT NULL,
    `status` VARCHAR(32) DEFAULT 'firing',
    `fired_at` DATETIME DEFAULT NULL,
    `resolved_at` DATETIME DEFAULT NULL,
    `acked_at` DATETIME DEFAULT NULL,
    `acked_by` BIGINT DEFAULT NULL,
    `duration` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_rule_id` (`rule_id`),
    KEY `idx_status` (`status`),
    KEY `idx_fired_at` (`fired_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警事件';

INSERT INTO `alert_channel` (`name`, `type`, `target`, `description`) VALUES
('运维钉钉群', 'dingtalk', 'https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN', '运维告警'),
('运维邮箱', 'email', 'ops@example.com', '运维邮箱');

INSERT INTO `alert_rule` (`name`, `description`, `metric_name`, `operator`, `threshold`, `severity`, `cooldown_minutes`) VALUES
('CPU 过高', 'CPU 使用率超过 80%', 'cpu_usage', '>', 0.8, 'warning', 5),
('内存满', '内存使用率超过 90%', 'memory_usage', '>', 0.9, 'critical', 5),
('JVM 堆满', 'JVM 堆使用率超过 85%', 'jvm_heap_usage', '>', 0.85, 'warning', 5),
('API 错误率高', 'API 错误率超过 5%', 'api_error_rate', '>', 0.05, 'critical', 1),
('响应慢', '平均响应时间超过 3 秒', 'response_time', '>', 3000, 'warning', 5);

-- 审计表
DROP TABLE IF EXISTS `audit_log_full`;
CREATE TABLE `audit_log_full` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `trace_id` VARCHAR(64) DEFAULT NULL,
    `user_id` BIGINT DEFAULT NULL,
    `username` VARCHAR(64) DEFAULT NULL,
    `user_ip` VARCHAR(64) DEFAULT NULL,
    `user_agent` VARCHAR(512) DEFAULT NULL,
    `action` VARCHAR(64) NOT NULL,
    `resource_type` VARCHAR(64) DEFAULT NULL,
    `resource_id` VARCHAR(128) DEFAULT NULL,
    `method` VARCHAR(8) DEFAULT NULL,
    `path` VARCHAR(512) DEFAULT NULL,
    `request_body` TEXT,
    `response_status` INT DEFAULT NULL,
    `result` VARCHAR(32) DEFAULT 'SUCCESS',
    `error_msg` VARCHAR(1024) DEFAULT NULL,
    `duration_ms` INT DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_action` (`action`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志';

INSERT INTO `audit_log_full` (trace_id, user_id, username, user_ip, action, resource_type, method, path, response_status, result, duration_ms) VALUES
('trace-001', 1, 'admin', '127.0.0.1', 'LOGIN', 'user', 'POST', '/api/v1/auth/login', 200, 'SUCCESS', 120),
('trace-002', 1, 'admin', '127.0.0.1', 'AI_GENERATE', 'ai', 'POST', '/api/ai/generate', 200, 'SUCCESS', 850),
('trace-003', 2, 'user1', '192.168.1.10', 'EXPORT_DATA', 'order', 'GET', '/api/v1/orders/export', 200, 'SUCCESS', 2300),
('trace-004', 2, 'user1', '192.168.1.10', 'FILE_UPLOAD', 'file', 'POST', '/api/multimodal/upload', 200, 'SUCCESS', 450),
('trace-005', 3, 'agent1', '192.168.1.20', 'CONFIG_CHANGE', 'config', 'PUT', '/api/v1/config', 403, 'DENIED', 50);

SELECT '告警 + 审计表初始化完成' AS status;
