-- ============================================================
-- MiniMax Platform - Day 8: RAG (Knowledge Base + Document + Chunk)
-- ============================================================

USE `minimax_platform`;

-- ------------------------------------------------------------
-- knowledge_base 知识库 (用户的"文档容器")
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `owner_id`    BIGINT       NOT NULL COMMENT '所属用户 (创建者)',
  `tenant_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '租户 (0=个人)',
  `name`        VARCHAR(128) NOT NULL,
  `description` VARCHAR(500)          DEFAULT NULL,
  `visibility`  VARCHAR(16)  NOT NULL DEFAULT 'private' COMMENT 'private/public/team',
  `doc_count`   INT          NOT NULL DEFAULT 0,
  `chunk_count` INT          NOT NULL DEFAULT 0,
  `tags`        VARCHAR(255)         DEFAULT NULL,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_owner_name` (`owner_id`, `name`, `deleted`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_visibility` (`visibility`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库';

-- ------------------------------------------------------------
-- document 文档
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `kb_id`         BIGINT       NOT NULL,
  `owner_id`      BIGINT       NOT NULL,
  `title`         VARCHAR(255) NOT NULL,
  `source_type`   VARCHAR(16)  NOT NULL DEFAULT 'txt' COMMENT 'txt/md/docx/pdf',
  `source_uri`    VARCHAR(500)          DEFAULT NULL COMMENT '原始文件位置/MinIO key/url',
  `content`       MEDIUMTEXT            DEFAULT NULL COMMENT '解析后纯文本',
  `size_bytes`    BIGINT       NOT NULL DEFAULT 0,
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/parsing/chunked/failed',
  `error_msg`     VARCHAR(500)          DEFAULT NULL,
  `chunk_count`   INT          NOT NULL DEFAULT 0,
  `checksum`      VARCHAR(64)           DEFAULT NULL COMMENT 'SHA-256 去重',
  `tags`          VARCHAR(255)          DEFAULT NULL,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_kb` (`kb_id`),
  KEY `idx_owner_created` (`owner_id`, `created_at`),
  KEY `idx_status` (`status`),
  KEY `idx_checksum` (`owner_id`, `checksum`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档';

-- ------------------------------------------------------------
-- document_chunk 文档切片 (含向量, 复用 Day 7 思路)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `document_chunk`;
CREATE TABLE `document_chunk` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `doc_id`      BIGINT       NOT NULL,
  `kb_id`       BIGINT       NOT NULL,
  `owner_id`    BIGINT       NOT NULL,
  `chunk_index` INT          NOT NULL COMMENT '在文档内的顺序',
  `content`     MEDIUMTEXT   NOT NULL,
  `embedding`   LONGBLOB     NOT NULL,
  `dim`         INT          NOT NULL,
  `char_count`  INT          NOT NULL DEFAULT 0,
  `start_pos`   INT          NOT NULL DEFAULT 0,
  `end_pos`     INT          NOT NULL DEFAULT 0,
  `access_count` INT         NOT NULL DEFAULT 0,
  `last_access_at` DATETIME           DEFAULT NULL,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_doc` (`doc_id`, `chunk_index`),
  KEY `idx_kb` (`kb_id`),
  KEY `idx_owner` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档切片(向量)';
