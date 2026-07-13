-- =============================================================
-- MiniMax Platform V3.5.5+ 修复 SQL - rag 模块
-- 共 3 张表, 18 字段
-- 用法: mysql -uroot -proot123456 minimax_platform < fix_rag.sql
-- 自动生成: scripts/split_fix_sql_by_module.py
-- =============================================================

-- 表: document
ALTER TABLE `document` ADD COLUMN `checksum` VARCHAR(255) DEFAULT NULL COMMENT 'checksum(checksum)';
ALTER TABLE `document` ADD COLUMN `chunkCount` INT DEFAULT 0 COMMENT 'chunkCount(chunkCount)';
ALTER TABLE `document` ADD COLUMN `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)';
ALTER TABLE `document` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `document` ADD COLUMN `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)';
ALTER TABLE `document` ADD COLUMN `sizeBytes` BIGINT DEFAULT 0 COMMENT 'sizeBytes(sizeBytes)';
ALTER TABLE `document` ADD COLUMN `sourceType` VARCHAR(255) DEFAULT NULL COMMENT 'sourceType(sourceType)';
ALTER TABLE `document` ADD COLUMN `sourceUri` VARCHAR(255) DEFAULT NULL COMMENT 'sourceUri(sourceUri)';
ALTER TABLE `document` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `document` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';

-- 表: document_chunk
ALTER TABLE `document_chunk` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `document_chunk` ADD COLUMN `embedding` BLOB DEFAULT NULL COMMENT 'embedding(embedding)';

-- 表: knowledge_base
ALTER TABLE `knowledge_base` ADD COLUMN `chunkCount` INT DEFAULT 0 COMMENT 'chunkCount(chunkCount)';
ALTER TABLE `knowledge_base` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `knowledge_base` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `knowledge_base` ADD COLUMN `docCount` INT DEFAULT 0 COMMENT 'docCount(docCount)';
ALTER TABLE `knowledge_base` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';
ALTER TABLE `knowledge_base` ADD COLUMN `visibility` VARCHAR(255) DEFAULT NULL COMMENT 'visibility(visibility)';

