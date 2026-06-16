-- ============================================================
-- MiniMax Platform - Day 7: Long-Term Memory (Vector)
-- 长期记忆：用 MySQL BLOB 存向量，Java 端做余弦相似度检索
-- ============================================================

USE `minimax`;

-- ------------------------------------------------------------
-- memory_long_term 长期记忆（向量 + 原文 + 元数据）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `memory_long_term`;
CREATE TABLE `memory_long_term` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT       NOT NULL COMMENT '所属用户',
  `session_id`   BIGINT                DEFAULT NULL COMMENT '来源会话 (可空 = 跨会话)',
  `content`      MEDIUMTEXT   NOT NULL COMMENT '原文',
  `summary`      VARCHAR(500)         DEFAULT NULL COMMENT 'LLM 摘要',
  `role`         VARCHAR(16)           DEFAULT 'user' COMMENT 'user / assistant / system',
  `embedding`    LONGBLOB     NOT NULL COMMENT '向量 (float[] 序列化, 通常 384-1536 维)',
  `dim`          INT          NOT NULL COMMENT '向量维度',
  `importance`   DECIMAL(3,2) NOT NULL DEFAULT 0.50 COMMENT '重要性 0-1',
  `tags`         VARCHAR(255)          DEFAULT NULL COMMENT '逗号分隔标签',
  `access_count` INT          NOT NULL DEFAULT 0 COMMENT '被召回次数',
  `last_access_at` DATETIME            DEFAULT NULL,
  `expires_at`   DATETIME             DEFAULT NULL COMMENT 'null=永不过期',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`, `created_at`),
  KEY `idx_user_session` (`user_id`, `session_id`),
  KEY `idx_importance` (`importance`),
  KEY `idx_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='长期记忆（向量）';

-- ------------------------------------------------------------
-- memory_user_pref 用户偏好（轻量 KV）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `memory_user_pref`;
CREATE TABLE `memory_user_pref` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       NOT NULL,
  `pref_key`   VARCHAR(64)  NOT NULL COMMENT 'language / tone / style / likes / ...',
  `pref_value` TEXT         NOT NULL,
  `weight`     DECIMAL(3,2) NOT NULL DEFAULT 0.50,
  `source`     VARCHAR(32)           DEFAULT 'manual' COMMENT 'manual / inferred',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_key` (`user_id`, `pref_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好记忆';
