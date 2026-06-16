-- ============================================================
-- MiniMax Platform - Day 3: Chat Session & Message
-- MySQL 8.x  |  utf8mb4  |  InnoDB
-- ============================================================

USE `minimax`;

-- ------------------------------------------------------------
-- chat_session 会话表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT       NOT NULL COMMENT '所属用户',
  `title`        VARCHAR(255) NOT NULL DEFAULT '新会话' COMMENT '会话标题',
  `model`        VARCHAR(64)           DEFAULT NULL COMMENT '使用的模型: gpt-4 / qwen-max / ollama:llama3',
  `system_prompt` TEXT                  COMMENT '系统提示词',
  `temperature`  DECIMAL(3,2) NOT NULL DEFAULT 0.70 COMMENT '温度 0-2',
  `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '0归档 1正常',
  `message_count` INT          NOT NULL DEFAULT 0 COMMENT '消息数(冗余)',
  `last_message_at` DATETIME            DEFAULT NULL COMMENT '最后消息时间',
  `tenant_id`    BIGINT       NOT NULL DEFAULT 0,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_updated` (`user_id`, `updated_at`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_last_message` (`last_message_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话';

-- ------------------------------------------------------------
-- chat_message 消息表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id`          BIGINT     NOT NULL AUTO_INCREMENT,
  `session_id`  BIGINT     NOT NULL,
  `user_id`     BIGINT     NOT NULL COMMENT '冗余,加速查询',
  `role`        VARCHAR(16) NOT NULL COMMENT 'user / assistant / system / tool',
  `content`     MEDIUMTEXT  NOT NULL COMMENT '消息内容(支持长文)',
  `tokens`      INT          DEFAULT NULL COMMENT 'token 消耗',
  `finish_reason` VARCHAR(32) DEFAULT NULL COMMENT 'stop / length / tool_calls',
  `error_message` VARCHAR(512) DEFAULT NULL,
  `created_at`  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`     TINYINT    NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_session_created` (`session_id`, `created_at`),
  KEY `idx_user_created` (`user_id`, `created_at`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息';
