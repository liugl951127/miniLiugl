-- =============================================================
-- MiniMax Platform V3.5.5+ 修复 SQL - model 模块
-- 共 4 张表, 49 字段
-- 用法: mysql -uroot -proot123456 minimax_platform < fix_model.sql
-- 自动生成: scripts/split_fix_sql_by_module.py
-- =============================================================

-- 表: model_battle_log
ALTER TABLE `model_battle_log` ADD COLUMN `battleId` VARCHAR(255) DEFAULT NULL COMMENT 'battleId(battleId)';
ALTER TABLE `model_battle_log` ADD COLUMN `completionTokens` INT DEFAULT 0 COMMENT 'completionTokens(completionTokens)';
ALTER TABLE `model_battle_log` ADD COLUMN `createdAt` DATETIME DEFAULT NULL COMMENT 'createdAt(createdAt)';
ALTER TABLE `model_battle_log` ADD COLUMN `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)';
ALTER TABLE `model_battle_log` ADD COLUMN `judgeModel` VARCHAR(255) DEFAULT NULL COMMENT 'judgeModel(judgeModel)';
ALTER TABLE `model_battle_log` ADD COLUMN `judgeReason` VARCHAR(255) DEFAULT NULL COMMENT 'judgeReason(judgeReason)';
ALTER TABLE `model_battle_log` ADD COLUMN `latencyMs` INT DEFAULT 0 COMMENT 'latencyMs(latencyMs)';
ALTER TABLE `model_battle_log` ADD COLUMN `modelCode` VARCHAR(255) DEFAULT NULL COMMENT 'modelCode(modelCode)';
ALTER TABLE `model_battle_log` ADD COLUMN `modelId` BIGINT DEFAULT 0 COMMENT 'modelId(modelId)';
ALTER TABLE `model_battle_log` ADD COLUMN `prompt` VARCHAR(255) DEFAULT NULL COMMENT 'prompt(prompt)';
ALTER TABLE `model_battle_log` ADD COLUMN `promptTokens` INT DEFAULT 0 COMMENT 'promptTokens(promptTokens)';
ALTER TABLE `model_battle_log` ADD COLUMN `response` VARCHAR(255) DEFAULT NULL COMMENT 'response(response)';
ALTER TABLE `model_battle_log` ADD COLUMN `score` INT DEFAULT 0 COMMENT 'score(score)';
ALTER TABLE `model_battle_log` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `model_battle_log` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';

-- 表: model_config
ALTER TABLE `model_config` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `model_config` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `model_config` ADD COLUMN `displayName` VARCHAR(255) DEFAULT NULL COMMENT 'displayName(displayName)';
ALTER TABLE `model_config` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `model_config` ADD COLUMN `inputPrice` DECIMAL(20,4) DEFAULT 0 COMMENT 'inputPrice(inputPrice)';
ALTER TABLE `model_config` ADD COLUMN `maxContext` INT DEFAULT 0 COMMENT 'maxContext(maxContext)';
ALTER TABLE `model_config` ADD COLUMN `maxOutput` INT DEFAULT 0 COMMENT 'maxOutput(maxOutput)';
ALTER TABLE `model_config` ADD COLUMN `outputPrice` DECIMAL(20,4) DEFAULT 0 COMMENT 'outputPrice(outputPrice)';
ALTER TABLE `model_config` ADD COLUMN `sort` INT DEFAULT 0 COMMENT 'sort(sort)';
ALTER TABLE `model_config` ADD COLUMN `supportsStream` INT DEFAULT 0 COMMENT 'supportsStream(supportsStream)';
ALTER TABLE `model_config` ADD COLUMN `supportsTools` INT DEFAULT 0 COMMENT 'supportsTools(supportsTools)';
ALTER TABLE `model_config` ADD COLUMN `supportsVision` INT DEFAULT 0 COMMENT 'supportsVision(supportsVision)';

-- 表: model_provider
ALTER TABLE `model_provider` ADD COLUMN `apiKey` VARCHAR(255) DEFAULT NULL COMMENT 'apiKey(apiKey)';
ALTER TABLE `model_provider` ADD COLUMN `baseUrl` VARCHAR(255) DEFAULT NULL COMMENT 'baseUrl(baseUrl)';
ALTER TABLE `model_provider` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `model_provider` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `model_provider` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `model_provider` ADD COLUMN `name` VARCHAR(255) DEFAULT NULL COMMENT 'name(name)';
ALTER TABLE `model_provider` ADD COLUMN `protocol` VARCHAR(255) DEFAULT NULL COMMENT 'protocol(protocol)';
ALTER TABLE `model_provider` ADD COLUMN `sort` INT DEFAULT 0 COMMENT 'sort(sort)';

-- 表: training_task
ALTER TABLE `training_task` ADD COLUMN `batchSize` INT DEFAULT 0 COMMENT 'batchSize(batchSize)';
ALTER TABLE `training_task` ADD COLUMN `blockSize` INT DEFAULT 0 COMMENT 'blockSize(blockSize)';
ALTER TABLE `training_task` ADD COLUMN `completedAt` DATETIME DEFAULT NULL COMMENT 'completedAt(completedAt)';
ALTER TABLE `training_task` ADD COLUMN `corpusPath` VARCHAR(255) DEFAULT NULL COMMENT 'corpusPath(corpusPath)';
ALTER TABLE `training_task` ADD COLUMN `currentIter` INT DEFAULT 0 COMMENT 'currentIter(currentIter)';
ALTER TABLE `training_task` ADD COLUMN `currentLoss` DOUBLE DEFAULT 0 COMMENT 'currentLoss(currentLoss)';
ALTER TABLE `training_task` ADD COLUMN `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)';
ALTER TABLE `training_task` ADD COLUMN `learningRate` DOUBLE DEFAULT 0 COMMENT 'learningRate(learningRate)';
ALTER TABLE `training_task` ADD COLUMN `maxIters` INT DEFAULT 0 COMMENT 'maxIters(maxIters)';
ALTER TABLE `training_task` ADD COLUMN `nEmbd` INT DEFAULT 0 COMMENT 'nEmbd(nEmbd)';
ALTER TABLE `training_task` ADD COLUMN `nHead` INT DEFAULT 0 COMMENT 'nHead(nHead)';
ALTER TABLE `training_task` ADD COLUMN `nLayer` INT DEFAULT 0 COMMENT 'nLayer(nLayer)';
ALTER TABLE `training_task` ADD COLUMN `progress` INT DEFAULT 0 COMMENT 'progress(progress)';
ALTER TABLE `training_task` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';

