-- =============================================================
-- MiniMax Platform V3.5.5+ 修复 SQL - analytics 模块
-- 共 4 张表, 36 字段
-- 用法: mysql -uroot -proot123456 minimax_platform < fix_analytics.sql
-- 自动生成: scripts/split_fix_sql_by_module.py
-- =============================================================

-- 表: analytics_datasource
ALTER TABLE `analytics_datasource` ADD COLUMN `createdAt` DATETIME DEFAULT NULL COMMENT 'createdAt(createdAt)';
ALTER TABLE `analytics_datasource` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `analytics_datasource` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `analytics_datasource` ADD COLUMN `jdbcUrl` VARCHAR(255) DEFAULT NULL COMMENT 'jdbcUrl(jdbcUrl)';
ALTER TABLE `analytics_datasource` ADD COLUMN `passwordEnc` VARCHAR(255) DEFAULT NULL COMMENT 'passwordEnc(passwordEnc)';
ALTER TABLE `analytics_datasource` ADD COLUMN `type` VARCHAR(255) DEFAULT NULL COMMENT 'type(type)';
ALTER TABLE `analytics_datasource` ADD COLUMN `updatedAt` DATETIME DEFAULT NULL COMMENT 'updatedAt(updatedAt)';
ALTER TABLE `analytics_datasource` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';

-- 表: analytics_ingest_task
ALTER TABLE `analytics_ingest_task` ADD COLUMN `columnsJson` VARCHAR(255) DEFAULT NULL COMMENT 'columnsJson(columnsJson)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `createdAt` DATETIME DEFAULT NULL COMMENT 'createdAt(createdAt)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `encoding` VARCHAR(255) DEFAULT NULL COMMENT 'encoding(encoding)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `fileSize` BIGINT DEFAULT 0 COMMENT 'fileSize(fileSize)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `fileType` VARCHAR(255) DEFAULT NULL COMMENT 'fileType(fileType)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `filename` VARCHAR(255) DEFAULT NULL COMMENT 'filename(filename)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `finishedAt` DATETIME DEFAULT NULL COMMENT 'finishedAt(finishedAt)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `qualityJson` VARCHAR(255) DEFAULT NULL COMMENT 'qualityJson(qualityJson)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `separator` VARCHAR(255) DEFAULT NULL COMMENT 'separator(separator)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `totalColumns` BIGINT DEFAULT 0 COMMENT 'totalColumns(totalColumns)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `totalRows` BIGINT DEFAULT 0 COMMENT 'totalRows(totalRows)';

-- 表: analytics_nlsql_history
ALTER TABLE `analytics_nlsql_history` ADD COLUMN `correctedSql` VARCHAR(255) DEFAULT NULL COMMENT 'correctedSql(correctedSql)';
ALTER TABLE `analytics_nlsql_history` ADD COLUMN `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)';
ALTER TABLE `analytics_nlsql_history` ADD COLUMN `generatedSql` VARCHAR(255) DEFAULT NULL COMMENT 'generatedSql(generatedSql)';
ALTER TABLE `analytics_nlsql_history` ADD COLUMN `model` VARCHAR(255) DEFAULT NULL COMMENT 'model(model)';
ALTER TABLE `analytics_nlsql_history` ADD COLUMN `promptTokens` INT DEFAULT 0 COMMENT 'promptTokens(promptTokens)';
ALTER TABLE `analytics_nlsql_history` ADD COLUMN `question` VARCHAR(255) DEFAULT NULL COMMENT 'question(question)';

-- 表: analytics_report
ALTER TABLE `analytics_report` ADD COLUMN `chartOptionsJson` VARCHAR(255) DEFAULT NULL COMMENT 'chartOptionsJson(chartOptionsJson)';
ALTER TABLE `analytics_report` ADD COLUMN `createdAt` DATETIME DEFAULT NULL COMMENT 'createdAt(createdAt)';
ALTER TABLE `analytics_report` ADD COLUMN `durationMs` BIGINT DEFAULT 0 COMMENT 'durationMs(durationMs)';
ALTER TABLE `analytics_report` ADD COLUMN `format` VARCHAR(255) DEFAULT NULL COMMENT 'format(format)';
ALTER TABLE `analytics_report` ADD COLUMN `markdown` VARCHAR(255) DEFAULT NULL COMMENT 'markdown(markdown)';
ALTER TABLE `analytics_report` ADD COLUMN `question` VARCHAR(255) DEFAULT NULL COMMENT 'question(question)';
ALTER TABLE `analytics_report` ADD COLUMN `rowCount` BIGINT DEFAULT 0 COMMENT 'rowCount(rowCount)';
ALTER TABLE `analytics_report` ADD COLUMN `sqlText` VARCHAR(255) DEFAULT NULL COMMENT 'sqlText(sqlText)';
ALTER TABLE `analytics_report` ADD COLUMN `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)';

