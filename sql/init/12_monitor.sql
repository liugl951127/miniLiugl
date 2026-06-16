-- ============================================================
-- MiniMax Platform - Day 12: Monitor
-- ============================================================

USE `minimax`;

-- ------------------------------------------------------------
-- metric_snapshot 指标快照 (定期落库, 供历史趋势)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `metric_snapshot`;
CREATE TABLE `metric_snapshot` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `service`       VARCHAR(32)  NOT NULL COMMENT '指标来源服务',
  `metric_name`   VARCHAR(64)  NOT NULL COMMENT '指标名 (e.g. cpu_usage, jvm_heap)',
  `metric_value`  DECIMAL(20,4) NOT NULL,
  `tags`          VARCHAR(500)         DEFAULT NULL COMMENT 'JSON 标签 {host: x, region: y}',
  `recorded_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_service_metric` (`service`, `metric_name`),
  KEY `idx_recorded` (`recorded_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标快照';

-- ------------------------------------------------------------
-- alert_rule 告警规则
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `alert_rule`;
CREATE TABLE `alert_rule` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `name`          VARCHAR(64)  NOT NULL,
  `description`   VARCHAR(255)          DEFAULT NULL,
  `metric_name`   VARCHAR(64)  NOT NULL,
  `service`       VARCHAR(32)           DEFAULT NULL,
  `operator`      VARCHAR(8)   NOT NULL DEFAULT '>' COMMENT '>/</>=/<=/=/!=',
  `threshold`     DECIMAL(20,4) NOT NULL,
  `severity`      VARCHAR(16)  NOT NULL DEFAULT 'warning' COMMENT 'info/warning/critical',
  `enabled`       TINYINT      NOT NULL DEFAULT 1,
  `cooldown_minutes` INT       NOT NULL DEFAULT 5 COMMENT '触发后冷却时间',
  `notify_channel` VARCHAR(255)         DEFAULT NULL COMMENT 'JSON: {email, webhook, ...}',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则';

-- ------------------------------------------------------------
-- alert_event 告警事件
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `alert_event`;
CREATE TABLE `alert_event` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `rule_id`       BIGINT       NOT NULL,
  `rule_name`     VARCHAR(64)  NOT NULL,
  `severity`      VARCHAR(16)  NOT NULL,
  `metric_name`   VARCHAR(64)  NOT NULL,
  `metric_value`  DECIMAL(20,4) NOT NULL,
  `threshold`     DECIMAL(20,4) NOT NULL,
  `message`       VARCHAR(500)          DEFAULT NULL,
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'firing' COMMENT 'firing/resolved',
  `fired_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `resolved_at`   DATETIME              DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_rule` (`rule_id`),
  KEY `idx_status` (`status`),
  KEY `idx_fired` (`fired_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警事件';

-- 初始 5 个默认告警规则
INSERT INTO `alert_rule` (`name`, `description`, `metric_name`, `service`, `operator`, `threshold`, `severity`, `cooldown_minutes`) VALUES
('CPU 高', 'CPU 使用率 > 80%', 'cpu_usage', NULL, '>', 80, 'warning', 5),
('JVM Heap 高', 'JVM 堆使用率 > 85%', 'jvm_heap_usage', NULL, '>', 85, 'critical', 5),
('磁盘高', '磁盘使用率 > 90%', 'disk_usage', NULL, '>', 90, 'critical', 10),
('LLM 延迟高', 'LLM 平均响应时间 > 3000ms', 'llm_avg_latency', 'model', '>', 3000, 'warning', 3),
('错误率高', 'HTTP 5xx 错误率 > 5%', 'http_5xx_rate', NULL, '>', 5, 'critical', 2);
