-- =============================================================
-- MiniMax Platform V3.5.5+ 修复 SQL - pipeline 模块
-- 共 4 张表, 33 字段
-- 用法: mysql -uroot -proot123456 minimax_platform < fix_pipeline.sql
-- 自动生成: scripts/split_fix_sql_by_module.py
-- =============================================================

-- 表: pipeline_node_log
ALTER TABLE `pipeline_node_log` ADD COLUMN `configSnapshot` VARCHAR(255) DEFAULT NULL COMMENT 'configSnapshot(configSnapshot)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `durationMs` BIGINT DEFAULT 0 COMMENT 'durationMs(durationMs)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `endTime` DATETIME DEFAULT NULL COMMENT 'endTime(endTime)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `inputRows` INT DEFAULT 0 COMMENT 'inputRows(inputRows)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `nodeName` VARCHAR(255) DEFAULT NULL COMMENT 'nodeName(nodeName)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `nodeType` VARCHAR(255) DEFAULT NULL COMMENT 'nodeType(nodeType)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `outputPreview` VARCHAR(255) DEFAULT NULL COMMENT 'outputPreview(outputPreview)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `outputRows` INT DEFAULT 0 COMMENT 'outputRows(outputRows)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `startTime` DATETIME DEFAULT NULL COMMENT 'startTime(startTime)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';

-- 表: pipeline_run
ALTER TABLE `pipeline_run` ADD COLUMN `createTime` DATETIME DEFAULT NULL COMMENT 'createTime(createTime)';
ALTER TABLE `pipeline_run` ADD COLUMN `definitionSnapshot` VARCHAR(255) DEFAULT NULL COMMENT 'definitionSnapshot(definitionSnapshot)';
ALTER TABLE `pipeline_run` ADD COLUMN `durationMs` BIGINT DEFAULT 0 COMMENT 'durationMs(durationMs)';
ALTER TABLE `pipeline_run` ADD COLUMN `endTime` DATETIME DEFAULT NULL COMMENT 'endTime(endTime)';
ALTER TABLE `pipeline_run` ADD COLUMN `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)';
ALTER TABLE `pipeline_run` ADD COLUMN `resultSummary` VARCHAR(255) DEFAULT NULL COMMENT 'resultSummary(resultSummary)';
ALTER TABLE `pipeline_run` ADD COLUMN `startTime` DATETIME DEFAULT NULL COMMENT 'startTime(startTime)';
ALTER TABLE `pipeline_run` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `pipeline_run` ADD COLUMN `triggerBy` BIGINT DEFAULT 0 COMMENT 'triggerBy(triggerBy)';
ALTER TABLE `pipeline_run` ADD COLUMN `triggerType` VARCHAR(255) DEFAULT NULL COMMENT 'triggerType(triggerType)';

-- 表: pipeline_workflow
ALTER TABLE `pipeline_workflow` ADD COLUMN `createBy` BIGINT DEFAULT 0 COMMENT 'createBy(createBy)';
ALTER TABLE `pipeline_workflow` ADD COLUMN `createTime` DATETIME DEFAULT NULL COMMENT 'createTime(createTime)';
ALTER TABLE `pipeline_workflow` ADD COLUMN `definition` VARCHAR(255) DEFAULT NULL COMMENT 'definition(definition)';
ALTER TABLE `pipeline_workflow` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `pipeline_workflow` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `pipeline_workflow` ADD COLUMN `status` INT DEFAULT 0 COMMENT 'status(status)';
ALTER TABLE `pipeline_workflow` ADD COLUMN `updateBy` BIGINT DEFAULT 0 COMMENT 'updateBy(updateBy)';
ALTER TABLE `pipeline_workflow` ADD COLUMN `updateTime` DATETIME DEFAULT NULL COMMENT 'updateTime(updateTime)';
ALTER TABLE `pipeline_workflow` ADD COLUMN `version` INT DEFAULT 0 COMMENT 'version(version)';

-- 表: pipeline_workflow_version
ALTER TABLE `pipeline_workflow_version` ADD COLUMN `changeLog` VARCHAR(255) DEFAULT NULL COMMENT 'changeLog(changeLog)';
ALTER TABLE `pipeline_workflow_version` ADD COLUMN `createBy` BIGINT DEFAULT 0 COMMENT 'createBy(createBy)';
ALTER TABLE `pipeline_workflow_version` ADD COLUMN `createTime` DATETIME DEFAULT NULL COMMENT 'createTime(createTime)';

