-- ============================================================
-- MiniMax Platform - Day 13: Optimization
-- ============================================================

USE `minimax`;

-- ------------------------------------------------------------
-- request_log 请求日志 (慢请求/错误请求采点)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `request_log`;
CREATE TABLE `request_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `trace_id`      VARCHAR(32)           DEFAULT NULL COMMENT '链路追踪 ID',
  `service`       VARCHAR(32)  NOT NULL,
  `method`        VARCHAR(8)   NOT NULL,
  `path`          VARCHAR(255) NOT NULL,
  `query_string`  VARCHAR(500)          DEFAULT NULL,
  `user_id`       BIGINT                DEFAULT NULL,
  `client_ip`     VARCHAR(64)           DEFAULT NULL,
  `user_agent`    VARCHAR(255)          DEFAULT NULL,
  `status`        INT          NOT NULL,
  `duration_ms`   INT          NOT NULL,
  `is_slow`       TINYINT      NOT NULL DEFAULT 0,
  `is_error`      TINYINT      NOT NULL DEFAULT 0,
  `error_msg`     VARCHAR(500)          DEFAULT NULL,
  `req_size`      INT          NOT NULL DEFAULT 0,
  `resp_size`     INT          NOT NULL DEFAULT 0,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_service_created` (`service`, `created_at`),
  KEY `idx_slow` (`is_slow`, `created_at`),
  KEY `idx_error` (`is_error`, `created_at`),
  KEY `idx_trace` (`trace_id`),
  KEY `idx_path` (`path`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请求日志';

-- ------------------------------------------------------------
-- async_task 异步任务
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `async_task`;
CREATE TABLE `async_task` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `task_id`       VARCHAR(64)  NOT NULL COMMENT '任务 ID (UUID)',
  `task_type`     VARCHAR(64)  NOT NULL COMMENT '任务类型',
  `payload`       MEDIUMTEXT            DEFAULT NULL COMMENT 'JSON 参数',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/running/done/failed',
  `priority`      INT          NOT NULL DEFAULT 5 COMMENT '1-10, 越小越优先',
  `attempts`      INT          NOT NULL DEFAULT 0,
  `max_attempts`  INT          NOT NULL DEFAULT 3,
  `result`        MEDIUMTEXT            DEFAULT NULL,
  `error_msg`     VARCHAR(1000)         DEFAULT NULL,
  `scheduled_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `started_at`    DATETIME              DEFAULT NULL,
  `finished_at`   DATETIME              DEFAULT NULL,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_status_priority` (`status`, `priority`, `scheduled_at`),
  KEY `idx_type` (`task_type`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步任务';

-- ------------------------------------------------------------
-- rate_limit_rule 限流规则 (可配置化)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `rate_limit_rule`;
CREATE TABLE `rate_limit_rule` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `name`          VARCHAR(64)  NOT NULL,
  `description`   VARCHAR(255)          DEFAULT NULL,
  `scope`         VARCHAR(16)  NOT NULL DEFAULT 'ip' COMMENT 'ip/user/global/api_key',
  `key_pattern`   VARCHAR(128)          DEFAULT NULL COMMENT 'url / path / method:path',
  `capacity`      INT          NOT NULL DEFAULT 10 COMMENT '桶容量',
  `refill_tokens` INT          NOT NULL DEFAULT 10 COMMENT '每次补充 token 数',
  `refill_period_seconds` INT   NOT NULL DEFAULT 60 COMMENT '补充周期',
  `enabled`       TINYINT      NOT NULL DEFAULT 1,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限流规则';

-- 初始限流规则
INSERT INTO `rate_limit_rule` (`name`, `description`, `scope`, `key_pattern`, `capacity`, `refill_tokens`, `refill_period_seconds`) VALUES
('default-ip', '默认 IP 限流', 'ip', NULL, 100, 100, 60),
('auth-login', '登录端点 IP 限流', 'ip', 'POST:/api/v1/auth/login', 10, 10, 60),
('chat-send', '聊天 IP 限流', 'ip', 'POST:/api/v1/sessions/*/messages', 60, 60, 60),
('rag-upload', 'RAG 上传 IP 限流', 'ip', 'POST:/api/v1/rag/doc/upload', 20, 20, 60);
