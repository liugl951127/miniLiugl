-- ============================================================
-- MiniMax Platform - Day 10: Admin 后台 + 统一审计
-- ============================================================

USE `minimax_platform`;

-- ------------------------------------------------------------
-- admin_audit_log 统一操作审计
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `admin_audit_log`;
CREATE TABLE `admin_audit_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `actor_id`      BIGINT       NOT NULL COMMENT '操作人 (admin user)',
  `actor_name`    VARCHAR(64)           DEFAULT NULL,
  `action`        VARCHAR(64)  NOT NULL COMMENT 'create_user/disable_user/reset_password/...',
  `resource_type` VARCHAR(32)  NOT NULL COMMENT 'user/model/kb/document/tool/...',
  `resource_id`   VARCHAR(64)           DEFAULT NULL,
  `detail`        MEDIUMTEXT            DEFAULT NULL COMMENT 'JSON 详情',
  `result`        VARCHAR(16)  NOT NULL DEFAULT 'ok' COMMENT 'ok/error',
  `error_msg`     VARCHAR(500)          DEFAULT NULL,
  `ip`            VARCHAR(64)           DEFAULT NULL,
  `user_agent`    VARCHAR(255)          DEFAULT NULL,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_actor_created` (`actor_id`, `created_at`),
  KEY `idx_action` (`action`),
  KEY `idx_resource` (`resource_type`, `resource_id`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一操作审计';
