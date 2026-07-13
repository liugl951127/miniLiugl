-- ===================================================================
-- MiniMax Platform V3.0.0 全量 DDL (单文件汇总)
-- 表数: 89
-- 字符集: utf8mb4 / 引擎: InnoDB
-- ===================================================================

-- CREATE DATABASE IF NOT EXISTS `minimax_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SET NAMES utf8mb4;
-- SET FOREIGN_KEY_CHECKS = 0;

-- AdminAuditLog -> admin_audit_log
CREATE TABLE IF NOT EXISTS `admin_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `actorId` BIGINT NOT NULL DEFAULT 0 COMMENT 'actorId',
    `actorName` VARCHAR(255) DEFAULT NULL COMMENT 'actorName',
    `action` VARCHAR(255) DEFAULT NULL COMMENT 'action',
    `resourceType` VARCHAR(255) DEFAULT NULL COMMENT 'resourceType',
    `resourceId` VARCHAR(255) DEFAULT NULL COMMENT 'resourceId',
    `detail` TEXT DEFAULT NULL COMMENT 'detail',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AdminAuditLog (auto-generated V3.0.0)';

-- AgentGroup -> agent_group
CREATE TABLE IF NOT EXISTS `agent_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `groupId` VARCHAR(255) DEFAULT NULL COMMENT 'groupId',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `description` TEXT DEFAULT NULL COMMENT 'description',
    `strategy` VARCHAR(255) DEFAULT NULL COMMENT 'strategy',
    `membersJson` VARCHAR(255) DEFAULT NULL COMMENT 'membersJson',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `ownerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ownerId',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `lastRunAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastRunAt',
    `runCount` INT NOT NULL DEFAULT 0 COMMENT 'runCount',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AgentGroup (auto-generated V3.0.0)';

-- MarketplaceAgent -> agent_marketplace
CREATE TABLE IF NOT EXISTS `agent_marketplace` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `agentKey` VARCHAR(255) DEFAULT NULL COMMENT 'agentKey',
    `name` TEXT DEFAULT NULL COMMENT 'name',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category',
    `icon` BIGINT NOT NULL DEFAULT 0 COMMENT 'icon',
    `authorId` VARCHAR(255) DEFAULT NULL COMMENT 'authorId',
    `definitionJson` VARCHAR(255) DEFAULT NULL COMMENT 'definitionJson',
    `version` VARCHAR(255) DEFAULT NULL COMMENT 'version',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `usageCount` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'usageCount',
    `avgRating` BIGINT NOT NULL DEFAULT 0 COMMENT 'avgRating',
    `ratingCount` VARCHAR(255) DEFAULT NULL COMMENT 'ratingCount',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `capabilities` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'capabilities',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `publishedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'publishedAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_marketplace_agentKey` (`agentKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MarketplaceAgent (auto-generated V3.0.0)';

-- AgentRating -> agent_rating
CREATE TABLE IF NOT EXISTS `agent_rating` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `agentKey` VARCHAR(255) DEFAULT NULL COMMENT 'agentKey',
    `userId` VARCHAR(255) DEFAULT NULL COMMENT 'userId',
    `username` INT NOT NULL DEFAULT 0 COMMENT 'username',
    `rating` VARCHAR(255) DEFAULT NULL COMMENT 'rating',
    `comment` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'comment',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_rating_agentKey` (`agentKey`),
    UNIQUE KEY `uk_agent_rating_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AgentRating (auto-generated V3.0.0)';

-- AgentTask -> agent_task
CREATE TABLE IF NOT EXISTS `agent_task` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `taskId` VARCHAR(255) DEFAULT NULL COMMENT 'taskId',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `goal` VARCHAR(255) DEFAULT NULL COMMENT 'goal',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `rounds` INT NOT NULL DEFAULT 0 COMMENT 'rounds',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result',
    `llmCalls` INT NOT NULL DEFAULT 0 COMMENT 'llmCalls',
    `toolCalls` INT NOT NULL DEFAULT 0 COMMENT 'toolCalls',
    `totalTokens` INT NOT NULL DEFAULT 0 COMMENT 'totalTokens',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg',
    `latencyMs` BIGINT NOT NULL DEFAULT 0 COMMENT 'latencyMs',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_task_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AgentTask (auto-generated V3.0.0)';

-- AiChatMessage -> ai_chat_message
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `sessionId` VARCHAR(255) DEFAULT NULL COMMENT 'sessionId',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role',
    `content` TEXT DEFAULT NULL COMMENT 'content',
    `toolCode` VARCHAR(255) DEFAULT NULL COMMENT 'toolCode',
    `toolInput` VARCHAR(255) DEFAULT NULL COMMENT 'toolInput',
    `toolOutput` VARCHAR(255) DEFAULT NULL COMMENT 'toolOutput',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AiChatMessage (auto-generated V3.0.0)';

-- AiChatSession -> ai_chat_session
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `sessionId` VARCHAR(255) DEFAULT NULL COMMENT 'sessionId',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_chat_session_userId` (`userId`),
    UNIQUE KEY `uk_ai_chat_session_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AiChatSession (auto-generated V3.0.0)';

-- AiGenerationLog -> ai_generation_log
CREATE TABLE IF NOT EXISTS `ai_generation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `generationId` VARCHAR(255) DEFAULT NULL COMMENT 'generationId',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `userIp` VARCHAR(255) DEFAULT NULL COMMENT 'userIp',
    `modality` VARCHAR(255) DEFAULT NULL COMMENT 'modality',
    `modelName` VARCHAR(255) DEFAULT NULL COMMENT 'modelName',
    `modelVersion` VARCHAR(255) DEFAULT NULL COMMENT 'modelVersion',
    `prompt` VARCHAR(255) DEFAULT NULL COMMENT 'prompt',
    `negativePrompt` VARCHAR(255) DEFAULT NULL COMMENT 'negativePrompt',
    `parameters` VARCHAR(255) DEFAULT NULL COMMENT 'parameters',
    `outputUrl` VARCHAR(255) DEFAULT NULL COMMENT 'outputUrl',
    `outputSize` BIGINT NOT NULL DEFAULT 0 COMMENT 'outputSize',
    `outputHash` VARCHAR(255) DEFAULT NULL COMMENT 'outputHash',
    `watermarked` INT NOT NULL DEFAULT 0 COMMENT 'watermarked',
    `watermarkText` VARCHAR(255) DEFAULT NULL COMMENT 'watermarkText',
    `durationMs` INT NOT NULL DEFAULT 0 COMMENT 'durationMs',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_generation_log_userId` (`userId`),
    UNIQUE KEY `uk_ai_generation_log_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AiGenerationLog (auto-generated V3.0.0)';

-- AiIntentKeyword -> ai_intent_keyword
CREATE TABLE IF NOT EXISTS `ai_intent_keyword` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `intent` VARCHAR(255) DEFAULT NULL COMMENT 'intent',
    `keyword` VARCHAR(255) DEFAULT NULL COMMENT 'keyword',
    `weight` INT NOT NULL DEFAULT 0 COMMENT 'weight',
    `isRegex` INT NOT NULL DEFAULT 0 COMMENT 'isRegex',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT 'enabled',
    `language` VARCHAR(255) DEFAULT NULL COMMENT 'language',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT 'remark',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AiIntentKeyword (auto-generated V3.0.0)';

-- AiTool -> ai_tool
CREATE TABLE IF NOT EXISTS `ai_tool` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdBy` BIGINT NOT NULL DEFAULT 0 COMMENT 'createdBy',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `code` VARCHAR(255) DEFAULT NULL COMMENT 'code',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category',
    `description` TEXT DEFAULT NULL COMMENT 'description',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT 'icon',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT 'enabled',
    `builtin` INT NOT NULL DEFAULT 0 COMMENT 'builtin',
    `inputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'inputSchema',
    `outputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'outputSchema',
    `defaultConfig` VARCHAR(255) DEFAULT NULL COMMENT 'defaultConfig',
    `implType` VARCHAR(255) DEFAULT NULL COMMENT 'implType',
    `implValue` VARCHAR(255) DEFAULT NULL COMMENT 'implValue',
    `rateLimit` INT NOT NULL DEFAULT 0 COMMENT 'rateLimit',
    `timeoutSeconds` INT NOT NULL DEFAULT 0 COMMENT 'timeoutSeconds',
    `roleRequired` VARCHAR(255) DEFAULT NULL COMMENT 'roleRequired',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `version` VARCHAR(255) DEFAULT NULL COMMENT 'version',
    `author` VARCHAR(255) DEFAULT NULL COMMENT 'author',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AiTool (auto-generated V3.0.0)';

-- AiToolInvocation -> ai_tool_invocation
CREATE TABLE IF NOT EXISTS `ai_tool_invocation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `toolCode` VARCHAR(255) DEFAULT NULL COMMENT 'toolCode',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `inputJson` VARCHAR(255) DEFAULT NULL COMMENT 'inputJson',
    `outputJson` VARCHAR(255) DEFAULT NULL COMMENT 'outputJson',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage',
    `durationMs` INT NOT NULL DEFAULT 0 COMMENT 'durationMs',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent',
    `dataSourceId` BIGINT NOT NULL DEFAULT 0 COMMENT 'dataSourceId',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_tool_invocation_userId` (`userId`),
    UNIQUE KEY `uk_ai_tool_invocation_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AiToolInvocation (auto-generated V3.0.0)';

-- AlertChannel -> alert_channel
CREATE TABLE IF NOT EXISTS `alert_channel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdBy` BIGINT NOT NULL DEFAULT 0 COMMENT 'createdBy',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `channelType` VARCHAR(255) DEFAULT NULL COMMENT 'channelType',
    `config` TEXT DEFAULT NULL COMMENT 'config',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT 'enabled',
    `priority` INT NOT NULL DEFAULT 0 COMMENT 'priority',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AlertChannel (auto-generated V3.0.0)';

-- AlertEvent -> alert_event
CREATE TABLE IF NOT EXISTS `alert_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `ruleId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ruleId',
    `ruleName` VARCHAR(255) DEFAULT NULL COMMENT 'ruleName',
    `severity` VARCHAR(255) DEFAULT NULL COMMENT 'severity',
    `metricName` VARCHAR(255) DEFAULT NULL COMMENT 'metricName',
    `message` VARCHAR(255) DEFAULT NULL COMMENT 'message',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `resolvedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'resolvedAt',
    `ackedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'ackedAt',
    `ackedBy` BIGINT NOT NULL DEFAULT 0 COMMENT 'ackedBy',
    `duration` BIGINT NOT NULL DEFAULT 0 COMMENT 'duration',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AlertEvent (auto-generated V3.0.0)';

-- AlertRule -> alert_rule
CREATE TABLE IF NOT EXISTS `alert_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `description` TEXT DEFAULT NULL COMMENT 'description',
    `metricName` VARCHAR(255) DEFAULT NULL COMMENT 'metricName',
    `service` VARCHAR(255) DEFAULT NULL COMMENT 'service',
    `operator` VARCHAR(255) DEFAULT NULL COMMENT 'operator',
    `threshold` DECIMAL(20,4) DEFAULT NULL COMMENT 'threshold',
    `severity` VARCHAR(255) DEFAULT NULL COMMENT 'severity',
    `cooldownMinutes` INT NOT NULL DEFAULT 0 COMMENT 'cooldownMinutes',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT 'enabled',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `notifyChannel` VARCHAR(255) DEFAULT NULL COMMENT 'notifyChannel',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AlertRule (auto-generated V3.0.0)';

-- DataSource -> analytics_datasource
CREATE TABLE IF NOT EXISTS `analytics_datasource` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `type` VARCHAR(255) DEFAULT NULL COMMENT 'type',
    `passwordEnc` VARCHAR(255) DEFAULT NULL COMMENT 'passwordEnc',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_analytics_datasource_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DataSource (auto-generated V3.0.0)';

-- IngestTask -> analytics_ingest_task
CREATE TABLE IF NOT EXISTS `analytics_ingest_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `taskId` VARCHAR(255) DEFAULT NULL COMMENT 'taskId',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `finishedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'finishedAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_analytics_ingest_task_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IngestTask (auto-generated V3.0.0)';

-- Nl2SqlHistory -> analytics_nlsql_history
CREATE TABLE IF NOT EXISTS `analytics_nlsql_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `dataSourceId` BIGINT NOT NULL DEFAULT 0 COMMENT 'dataSourceId',
    `completionTokens` INT NOT NULL DEFAULT 0 COMMENT 'completionTokens',
    `durationMs` BIGINT NOT NULL DEFAULT 0 COMMENT 'durationMs',
    `success` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'success',
    `feedbackRating` INT NOT NULL DEFAULT 0 COMMENT 'feedbackRating',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_analytics_nlsql_history_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Nl2SqlHistory (auto-generated V3.0.0)';

-- Report -> analytics_report
CREATE TABLE IF NOT EXISTS `analytics_report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `reportId` VARCHAR(255) DEFAULT NULL COMMENT 'reportId',
    `format` VARCHAR(255) DEFAULT NULL COMMENT 'format',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_analytics_report_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Report (auto-generated V3.0.0)';

-- AsyncTask -> async_task
CREATE TABLE IF NOT EXISTS `async_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `startedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'startedAt',
    `finishedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'finishedAt',
    `taskId` VARCHAR(255) DEFAULT NULL COMMENT 'taskId',
    `taskType` VARCHAR(255) DEFAULT NULL COMMENT 'taskType',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `params` VARCHAR(255) DEFAULT NULL COMMENT 'params',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg',
    `retryCount` INT NOT NULL DEFAULT 0 COMMENT 'retryCount',
    `latencyMs` BIGINT NOT NULL DEFAULT 0 COMMENT 'latencyMs',
    `submitterId` BIGINT NOT NULL DEFAULT 0 COMMENT 'submitterId',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AsyncTask (auto-generated V3.0.0)';

-- AuditLog -> audit_log
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `traceId` VARCHAR(255) DEFAULT NULL COMMENT 'traceId',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `userIp` VARCHAR(255) DEFAULT NULL COMMENT 'userIp',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent',
    `action` VARCHAR(255) DEFAULT NULL COMMENT 'action',
    `resourceType` VARCHAR(255) DEFAULT NULL COMMENT 'resourceType',
    `resourceId` VARCHAR(255) DEFAULT NULL COMMENT 'resourceId',
    `method` VARCHAR(255) DEFAULT NULL COMMENT 'method',
    `path` VARCHAR(255) DEFAULT NULL COMMENT 'path',
    `requestBody` VARCHAR(255) DEFAULT NULL COMMENT 'requestBody',
    `responseStatus` INT NOT NULL DEFAULT 0 COMMENT 'responseStatus',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg',
    `durationMs` INT NOT NULL DEFAULT 0 COMMENT 'durationMs',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_audit_log_userId` (`userId`),
    UNIQUE KEY `uk_audit_log_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AuditLog (auto-generated V3.0.0)';

-- AuditLogFull -> audit_log_full
CREATE TABLE IF NOT EXISTS `audit_log_full` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `traceId` VARCHAR(255) DEFAULT NULL COMMENT 'traceId',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `userIp` VARCHAR(255) DEFAULT NULL COMMENT 'userIp',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent',
    `action` VARCHAR(255) DEFAULT NULL COMMENT 'action',
    `resourceType` VARCHAR(255) DEFAULT NULL COMMENT 'resourceType',
    `resourceId` VARCHAR(255) DEFAULT NULL COMMENT 'resourceId',
    `method` VARCHAR(255) DEFAULT NULL COMMENT 'method',
    `path` VARCHAR(255) DEFAULT NULL COMMENT 'path',
    `requestBody` VARCHAR(255) DEFAULT NULL COMMENT 'requestBody',
    `responseStatus` INT NOT NULL DEFAULT 0 COMMENT 'responseStatus',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg',
    `durationMs` INT NOT NULL DEFAULT 0 COMMENT 'durationMs',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_audit_log_full_userId` (`userId`),
    UNIQUE KEY `uk_audit_log_full_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AuditLogFull (auto-generated V3.0.0)';

-- AuthLoginLog -> auth_login_log
CREATE TABLE IF NOT EXISTS `auth_login_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent',
    `status` INT NOT NULL DEFAULT 0 COMMENT 'status',
    `message` VARCHAR(255) DEFAULT NULL COMMENT 'message',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_auth_login_log_userId` (`userId`),
    UNIQUE KEY `uk_auth_login_log_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AuthLoginLog (auto-generated V3.0.0)';

-- AuthRefreshToken -> auth_refresh_token
CREATE TABLE IF NOT EXISTS `auth_refresh_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `token` VARCHAR(255) DEFAULT NULL COMMENT 'token',
    `expiresAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'expiresAt',
    `revoked` INT NOT NULL DEFAULT 0 COMMENT 'revoked',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_auth_refresh_token_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AuthRefreshToken (auto-generated V3.0.0)';

-- BillingRecord -> billing_record
CREATE TABLE IF NOT EXISTS `billing_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `recordId` VARCHAR(255) DEFAULT NULL COMMENT 'recordId',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `licenseId` BIGINT NOT NULL DEFAULT 0 COMMENT 'licenseId',
    `modelEntryId` BIGINT NOT NULL DEFAULT 0 COMMENT 'modelEntryId',
    `recordType` VARCHAR(255) DEFAULT NULL COMMENT 'recordType',
    `amountCents` BIGINT NOT NULL DEFAULT 0 COMMENT 'amountCents',
    `currency` VARCHAR(255) DEFAULT NULL COMMENT 'currency',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `paymentMethod` VARCHAR(255) DEFAULT NULL COMMENT 'paymentMethod',
    `externalTransactionId` VARCHAR(255) DEFAULT NULL COMMENT 'externalTransactionId',
    `description` TEXT DEFAULT NULL COMMENT 'description',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_billing_record_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BillingRecord (auto-generated V3.0.0)';

-- ChatMessage -> chat_message
CREATE TABLE IF NOT EXISTS `chat_message` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `sessionId` BIGINT NOT NULL DEFAULT 0 COMMENT 'sessionId',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role',
    `content` TEXT DEFAULT NULL COMMENT 'content',
    `tokens` INT NOT NULL DEFAULT 0 COMMENT 'tokens',
    `finishReason` VARCHAR(255) DEFAULT NULL COMMENT 'finishReason',
    `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chat_message_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ChatMessage (auto-generated V3.0.0)';

-- ChatSession -> chat_session
CREATE TABLE IF NOT EXISTS `chat_session` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title',
    `model` VARCHAR(255) DEFAULT NULL COMMENT 'model',
    `systemPrompt` VARCHAR(255) DEFAULT NULL COMMENT 'systemPrompt',
    `temperature` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT 'temperature',
    `status` INT NOT NULL DEFAULT 0 COMMENT 'status',
    `messageCount` INT NOT NULL DEFAULT 0 COMMENT 'messageCount',
    `lastMessageAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastMessageAt',
    `tenantId` BIGINT NOT NULL DEFAULT 0 COMMENT 'tenantId',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chat_session_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ChatSession (auto-generated V3.0.0)';

-- ClusterNode -> cluster_node
CREATE TABLE IF NOT EXISTS `cluster_node` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `nodeId` VARCHAR(255) DEFAULT NULL COMMENT 'nodeId',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `address` VARCHAR(255) DEFAULT NULL COMMENT 'address',
    `region` VARCHAR(255) DEFAULT NULL COMMENT 'region',
    `zone` VARCHAR(255) DEFAULT NULL COMMENT 'zone',
    `capabilities` VARCHAR(255) DEFAULT NULL COMMENT 'capabilities',
    `totalCores` INT NOT NULL DEFAULT 0 COMMENT 'totalCores',
    `totalMemoryMb` BIGINT NOT NULL DEFAULT 0 COMMENT 'totalMemoryMb',
    `totalGpus` INT NOT NULL DEFAULT 0 COMMENT 'totalGpus',
    `cpuUsage` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'cpuUsage',
    `memoryUsage` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'memoryUsage',
    `gpuUsage` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'gpuUsage',
    `activeTasks` INT NOT NULL DEFAULT 0 COMMENT 'activeTasks',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `isLeader` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'isLeader',
    `lastHeartbeat` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastHeartbeat',
    `startedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'startedAt',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ClusterNode (auto-generated V3.0.0)';

-- CollabMember -> collab_member
CREATE TABLE IF NOT EXISTS `collab_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `collabId` BIGINT NOT NULL DEFAULT 0 COMMENT 'collabId',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role',
    `joinedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'joinedAt',
    `lastActiveAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastActiveAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_collab_member_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CollabMember (auto-generated V3.0.0)';

-- CollabMessage -> collab_message
CREATE TABLE IF NOT EXISTS `collab_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `roomId` VARCHAR(255) DEFAULT NULL COMMENT 'roomId',
    `userId` VARCHAR(255) DEFAULT NULL COMMENT 'userId',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `type` VARCHAR(255) DEFAULT NULL COMMENT 'type',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content',
    `metadata` VARCHAR(255) DEFAULT NULL COMMENT 'metadata',
    `clientMsgId` INT NOT NULL DEFAULT 0 COMMENT 'clientMsgId',
    `broadcast` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'broadcast',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_collab_message_roomId` (`roomId`),
    UNIQUE KEY `uk_collab_message_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CollabMessage (auto-generated V3.0.0)';

-- CollabParticipant -> collab_participant
CREATE TABLE IF NOT EXISTS `collab_participant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `roomId` VARCHAR(255) DEFAULT NULL COMMENT 'roomId',
    `userId` VARCHAR(255) DEFAULT NULL COMMENT 'userId',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar',
    `role` INT NOT NULL DEFAULT 0 COMMENT 'role',
    `cursorX` INT NOT NULL DEFAULT 0 COMMENT 'cursorX',
    `cursorY` VARCHAR(255) DEFAULT NULL COMMENT 'cursorY',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `joinedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'joinedAt',
    `leftAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'leftAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_collab_participant_roomId` (`roomId`),
    UNIQUE KEY `uk_collab_participant_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CollabParticipant (auto-generated V3.0.0)';

-- CollabRoom -> collab_room
CREATE TABLE IF NOT EXISTS `collab_room` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `roomId` VARCHAR(255) DEFAULT NULL COMMENT 'roomId',
    `type` VARCHAR(255) DEFAULT NULL COMMENT 'type',
    `ownerId` VARCHAR(255) DEFAULT NULL COMMENT 'ownerId',
    `ownerName` TEXT DEFAULT NULL COMMENT 'ownerName',
    `description` INT NOT NULL DEFAULT 0 COMMENT 'description',
    `isPublic` INT NOT NULL DEFAULT 0 COMMENT 'isPublic',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `currentParticipants` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'currentParticipants',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `lastActivityAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastActivityAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_collab_room_roomId` (`roomId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CollabRoom (auto-generated V3.0.0)';

-- CollabSession -> collab_session
CREATE TABLE IF NOT EXISTS `collab_session` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `sessionId` VARCHAR(255) DEFAULT NULL COMMENT 'sessionId',
    `ownerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ownerId',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title',
    `maxUsers` INT NOT NULL DEFAULT 0 COMMENT 'maxUsers',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CollabSession (auto-generated V3.0.0)';

-- DashboardMetric -> dashboard_metric
CREATE TABLE IF NOT EXISTS `dashboard_metric` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `timestamp` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'timestamp',
    `metric` VARCHAR(255) DEFAULT NULL COMMENT 'metric',
    `dimension` VARCHAR(255) DEFAULT NULL COMMENT 'dimension',
    `value` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'value',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DashboardMetric (auto-generated V3.0.0)';

-- DbDataSource -> data_source
CREATE TABLE IF NOT EXISTS `data_source` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdBy` BIGINT NOT NULL DEFAULT 0 COMMENT 'createdBy',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `type` VARCHAR(255) DEFAULT NULL COMMENT 'type',
    `jdbcUrl` VARCHAR(255) DEFAULT NULL COMMENT 'jdbcUrl',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `password` VARCHAR(255) DEFAULT NULL COMMENT 'password',
    `driverClass` VARCHAR(255) DEFAULT NULL COMMENT 'driverClass',
    `poolSize` INT NOT NULL DEFAULT 0 COMMENT 'poolSize',
    `minIdle` INT NOT NULL DEFAULT 0 COMMENT 'minIdle',
    `maxLifetime` INT NOT NULL DEFAULT 0 COMMENT 'maxLifetime',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT 'enabled',
    `testStatus` VARCHAR(255) DEFAULT NULL COMMENT 'testStatus',
    `testMessage` VARCHAR(255) DEFAULT NULL COMMENT 'testMessage',
    `lastTestAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastTestAt',
    `description` TEXT DEFAULT NULL COMMENT 'description',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_data_source_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DbDataSource (auto-generated V3.0.0)';

-- Document -> document
CREATE TABLE IF NOT EXISTS `document` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `kbId` BIGINT NOT NULL DEFAULT 0 COMMENT 'kbId',
    `ownerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ownerId',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title',
    `sourceType` VARCHAR(255) DEFAULT NULL COMMENT 'sourceType',
    `sourceUri` VARCHAR(255) DEFAULT NULL COMMENT 'sourceUri',
    `content` TEXT DEFAULT NULL COMMENT 'content',
    `sizeBytes` BIGINT NOT NULL DEFAULT 0 COMMENT 'sizeBytes',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg',
    `chunkCount` INT NOT NULL DEFAULT 0 COMMENT 'chunkCount',
    `checksum` VARCHAR(255) DEFAULT NULL COMMENT 'checksum',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Document (auto-generated V3.0.0)';

-- DocumentChunk -> document_chunk
CREATE TABLE IF NOT EXISTS `document_chunk` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `docId` BIGINT NOT NULL DEFAULT 0 COMMENT 'docId',
    `kbId` BIGINT NOT NULL DEFAULT 0 COMMENT 'kbId',
    `ownerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ownerId',
    `chunkIndex` INT NOT NULL DEFAULT 0 COMMENT 'chunkIndex',
    `content` TEXT DEFAULT NULL COMMENT 'content',
    `dim` INT NOT NULL DEFAULT 0 COMMENT 'dim',
    `charCount` INT NOT NULL DEFAULT 0 COMMENT 'charCount',
    `startPos` INT NOT NULL DEFAULT 0 COMMENT 'startPos',
    `endPos` INT NOT NULL DEFAULT 0 COMMENT 'endPos',
    `accessCount` INT NOT NULL DEFAULT 0 COMMENT 'accessCount',
    `lastAccessAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastAccessAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_document_chunk_docId` (`docId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DocumentChunk (auto-generated V3.0.0)';

-- FunctionCallLog -> function_call_log
CREATE TABLE IF NOT EXISTS `function_call_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `sessionId` BIGINT NOT NULL DEFAULT 0 COMMENT 'sessionId',
    `toolName` VARCHAR(255) DEFAULT NULL COMMENT 'toolName',
    `arguments` VARCHAR(255) DEFAULT NULL COMMENT 'arguments',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg',
    `durationMs` INT NOT NULL DEFAULT 0 COMMENT 'durationMs',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_function_call_log_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FunctionCallLog (auto-generated V3.0.0)';

-- FunctionTool -> function_tool
CREATE TABLE IF NOT EXISTS `function_tool` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `displayName` VARCHAR(255) DEFAULT NULL COMMENT 'displayName',
    `description` TEXT DEFAULT NULL COMMENT 'description',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category',
    `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope',
    `ownerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ownerId',
    `parameters` VARCHAR(255) DEFAULT NULL COMMENT 'parameters',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT 'enabled',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FunctionTool (auto-generated V3.0.0)';

-- KbChunk -> kb_chunk
CREATE TABLE IF NOT EXISTS `kb_chunk` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `chunkId` VARCHAR(255) DEFAULT NULL COMMENT 'chunkId',
    `docId` VARCHAR(255) DEFAULT NULL COMMENT 'docId',
    `kbId` VARCHAR(255) DEFAULT NULL COMMENT 'kbId',
    `seq` INT NOT NULL DEFAULT 0 COMMENT 'seq',
    `content` TEXT DEFAULT NULL COMMENT 'content',
    `charCount` INT NOT NULL DEFAULT 0 COMMENT 'charCount',
    `tokenCount` INT NOT NULL DEFAULT 0 COMMENT 'tokenCount',
    `embedding` VARCHAR(255) DEFAULT NULL COMMENT 'embedding',
    `embeddingModel` VARCHAR(255) DEFAULT NULL COMMENT 'embeddingModel',
    `keywords` VARCHAR(255) DEFAULT NULL COMMENT 'keywords',
    `summary` VARCHAR(255) DEFAULT NULL COMMENT 'summary',
    `location` VARCHAR(255) DEFAULT NULL COMMENT 'location',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kb_chunk_docId` (`docId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KbChunk (auto-generated V3.0.0)';

-- KbDocument -> kb_document
CREATE TABLE IF NOT EXISTS `kb_document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `docId` VARCHAR(255) DEFAULT NULL COMMENT 'docId',
    `kbId` VARCHAR(255) DEFAULT NULL COMMENT 'kbId',
    `filename` VARCHAR(255) DEFAULT NULL COMMENT 'filename',
    `mimeType` VARCHAR(255) DEFAULT NULL COMMENT 'mimeType',
    `sizeBytes` BIGINT NOT NULL DEFAULT 0 COMMENT 'sizeBytes',
    `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256',
    `filePath` VARCHAR(255) DEFAULT NULL COMMENT 'filePath',
    `source` VARCHAR(255) DEFAULT NULL COMMENT 'source',
    `sourceUrl` VARCHAR(255) DEFAULT NULL COMMENT 'sourceUrl',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `chunkCount` INT NOT NULL DEFAULT 0 COMMENT 'chunkCount',
    `embeddingCount` INT NOT NULL DEFAULT 0 COMMENT 'embeddingCount',
    `error` VARCHAR(255) DEFAULT NULL COMMENT 'error',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `ownerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ownerId',
    `isPublic` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'isPublic',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kb_document_docId` (`docId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KbDocument (auto-generated V3.0.0)';

-- KbPermission -> kb_permission
CREATE TABLE IF NOT EXISTS `kb_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `kbId` VARCHAR(255) DEFAULT NULL COMMENT 'kbId',
    `subjectType` VARCHAR(255) DEFAULT NULL COMMENT 'subjectType',
    `subjectId` BIGINT NOT NULL DEFAULT 0 COMMENT 'subjectId',
    `permission` VARCHAR(255) DEFAULT NULL COMMENT 'permission',
    `grantBy` BIGINT NOT NULL DEFAULT 0 COMMENT 'grantBy',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KbPermission (auto-generated V3.0.0)';

-- KgEntity -> kg_entity
CREATE TABLE IF NOT EXISTS `kg_entity` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `entityType` VARCHAR(255) DEFAULT NULL COMMENT 'entityType',
    `description` TEXT DEFAULT NULL COMMENT 'description',
    `aliases` VARCHAR(255) DEFAULT NULL COMMENT 'aliases',
    `importance` INT NOT NULL DEFAULT 0 COMMENT 'importance',
    `source` VARCHAR(255) DEFAULT NULL COMMENT 'source',
    `refCount` INT NOT NULL DEFAULT 0 COMMENT 'refCount',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kg_entity_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KgEntity (auto-generated V3.0.0)';

-- KgRelation -> kg_relation
CREATE TABLE IF NOT EXISTS `kg_relation` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `fromEntity` BIGINT NOT NULL DEFAULT 0 COMMENT 'fromEntity',
    `toEntity` BIGINT NOT NULL DEFAULT 0 COMMENT 'toEntity',
    `relationType` VARCHAR(255) DEFAULT NULL COMMENT 'relationType',
    `description` TEXT DEFAULT NULL COMMENT 'description',
    `weight` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT 'weight',
    `source` VARCHAR(255) DEFAULT NULL COMMENT 'source',
    `refCount` INT NOT NULL DEFAULT 0 COMMENT 'refCount',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kg_relation_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KgRelation (auto-generated V3.0.0)';

-- KnowledgeBase -> knowledge_base
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `ownerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ownerId',
    `tenantId` BIGINT NOT NULL DEFAULT 0 COMMENT 'tenantId',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `description` TEXT DEFAULT NULL COMMENT 'description',
    `visibility` VARCHAR(255) DEFAULT NULL COMMENT 'visibility',
    `docCount` INT NOT NULL DEFAULT 0 COMMENT 'docCount',
    `chunkCount` INT NOT NULL DEFAULT 0 COMMENT 'chunkCount',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KnowledgeBase (auto-generated V3.0.0)';

-- LicenseTemplate -> license_template
CREATE TABLE IF NOT EXISTS `license_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `templateKey` VARCHAR(255) DEFAULT NULL COMMENT 'templateKey',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `licenseType` VARCHAR(255) DEFAULT NULL COMMENT 'licenseType',
    `description` TEXT DEFAULT NULL COMMENT 'description',
    `quotaCalls` BIGINT NOT NULL DEFAULT 0 COMMENT 'quotaCalls',
    `quotaDays` INT NOT NULL DEFAULT 0 COMMENT 'quotaDays',
    `priceCents` BIGINT NOT NULL DEFAULT 0 COMMENT 'priceCents',
    `features` VARCHAR(255) DEFAULT NULL COMMENT 'features',
    `limits` VARCHAR(255) DEFAULT NULL COMMENT 'limits',
    `isPublic` INT NOT NULL DEFAULT 0 COMMENT 'isPublic',
    `isActive` INT NOT NULL DEFAULT 0 COMMENT 'isActive',
    `version` INT NOT NULL DEFAULT 0 COMMENT 'version',
    `createdBy` BIGINT NOT NULL DEFAULT 0 COMMENT 'createdBy',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LicenseTemplate (auto-generated V3.0.0)';

-- LongTermMemory -> memory_long_term
CREATE TABLE IF NOT EXISTS `memory_long_term` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `sessionId` BIGINT NOT NULL DEFAULT 0 COMMENT 'sessionId',
    `content` TEXT DEFAULT NULL COMMENT 'content',
    `summary` VARCHAR(255) DEFAULT NULL COMMENT 'summary',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role',
    `dim` INT NOT NULL DEFAULT 0 COMMENT 'dim',
    `importance` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT 'importance',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `accessCount` INT NOT NULL DEFAULT 0 COMMENT 'accessCount',
    `lastAccessAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastAccessAt',
    `expiresAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'expiresAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_memory_long_term_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LongTermMemory (auto-generated V3.0.0)';

-- UserPref -> memory_user_pref
CREATE TABLE IF NOT EXISTS `memory_user_pref` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `prefKey` VARCHAR(255) DEFAULT NULL COMMENT 'prefKey',
    `prefValue` VARCHAR(255) DEFAULT NULL COMMENT 'prefValue',
    `weight` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT 'weight',
    `source` VARCHAR(255) DEFAULT NULL COMMENT 'source',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_memory_user_pref_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='UserPref (auto-generated V3.0.0)';

-- MetricSnapshot -> metric_snapshot
CREATE TABLE IF NOT EXISTS `metric_snapshot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `recordedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'recordedAt',
    `service` VARCHAR(255) DEFAULT NULL COMMENT 'service',
    `metricName` VARCHAR(255) DEFAULT NULL COMMENT 'metricName',
    `metricValue` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT 'metricValue',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MetricSnapshot (auto-generated V3.0.0)';

-- ModelBattleLog -> model_battle_log
CREATE TABLE IF NOT EXISTS `model_battle_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `battle_id` VARCHAR(255) DEFAULT NULL COMMENT 'battle_id',
    `user_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'user_id',
    `model_id` VARCHAR(255) DEFAULT NULL COMMENT 'model_id',
    `prompt_tokens` INT NOT NULL DEFAULT 0 COMMENT 'prompt_tokens',
    `completion_tokens` INT NOT NULL DEFAULT 0 COMMENT 'completion_tokens',
    `latency_ms` VARCHAR(255) DEFAULT NULL COMMENT 'latency_ms',
    `error_msg` VARCHAR(255) DEFAULT NULL COMMENT 'error_msg',
    `judge_model` VARCHAR(255) DEFAULT NULL COMMENT 'judge_model',
    `judge_reason` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'judge_reason',
    `prompt` VARCHAR(255) DEFAULT NULL COMMENT 'prompt',
    `response` TEXT DEFAULT NULL COMMENT 'response',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `score` INT NOT NULL DEFAULT 0 COMMENT 'score',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelBattleLog (auto-generated V3.0.0)';

-- ModelConfig -> model_config
CREATE TABLE IF NOT EXISTS `model_config` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `providerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'providerId',
    `modelCode` VARCHAR(255) DEFAULT NULL COMMENT 'modelCode',
    `displayName` VARCHAR(255) DEFAULT NULL COMMENT 'displayName',
    `maxContext` INT NOT NULL DEFAULT 0 COMMENT 'maxContext',
    `maxOutput` INT NOT NULL DEFAULT 0 COMMENT 'maxOutput',
    `inputPrice` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT 'inputPrice',
    `outputPrice` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT 'outputPrice',
    `supportsVision` INT NOT NULL DEFAULT 0 COMMENT 'supportsVision',
    `supportsTools` INT NOT NULL DEFAULT 0 COMMENT 'supportsTools',
    `supportsStream` INT NOT NULL DEFAULT 0 COMMENT 'supportsStream',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT 'enabled',
    `sort` INT NOT NULL DEFAULT 0 COMMENT 'sort',
    `description` TEXT DEFAULT NULL COMMENT 'description',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelConfig (auto-generated V3.0.0)';

-- ModelLicense -> model_license
CREATE TABLE IF NOT EXISTS `model_license` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `licenseKey` VARCHAR(255) DEFAULT NULL COMMENT 'licenseKey',
    `modelEntryId` BIGINT NOT NULL DEFAULT 0 COMMENT 'modelEntryId',
    `modelVersionId` BIGINT NOT NULL DEFAULT 0 COMMENT 'modelVersionId',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `licenseType` VARCHAR(255) DEFAULT NULL COMMENT 'licenseType',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `quotaCalls` BIGINT NOT NULL DEFAULT 0 COMMENT 'quotaCalls',
    `usedCalls` BIGINT NOT NULL DEFAULT 0 COMMENT 'usedCalls',
    `startAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'startAt',
    `expireAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'expireAt',
    `priceCents` BIGINT NOT NULL DEFAULT 0 COMMENT 'priceCents',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_license_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelLicense (auto-generated V3.0.0)';

-- ModelEntry -> model_market
CREATE TABLE IF NOT EXISTS `model_market` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `modelKey` VARCHAR(255) DEFAULT NULL COMMENT 'modelKey',
    `name` TEXT DEFAULT NULL COMMENT 'name',
    `modelType` VARCHAR(255) DEFAULT NULL COMMENT 'modelType',
    `taskType` VARCHAR(255) DEFAULT NULL COMMENT 'taskType',
    `baseModel` VARCHAR(255) DEFAULT NULL COMMENT 'baseModel',
    `version` VARCHAR(255) DEFAULT NULL COMMENT 'version',
    `filePath` VARCHAR(255) DEFAULT NULL COMMENT 'filePath',
    `fileName` BIGINT NOT NULL DEFAULT 0 COMMENT 'fileName',
    `fileSize` VARCHAR(255) DEFAULT NULL COMMENT 'fileSize',
    `license` VARCHAR(255) DEFAULT NULL COMMENT 'license',
    `authorId` VARCHAR(255) DEFAULT NULL COMMENT 'authorId',
    `authorName` VARCHAR(255) DEFAULT NULL COMMENT 'authorName',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `downloadCount` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'downloadCount',
    `avgRating` BIGINT NOT NULL DEFAULT 0 COMMENT 'avgRating',
    `ratingCount` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'ratingCount',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `publishedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'publishedAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_market_modelKey` (`modelKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelEntry (auto-generated V3.0.0)';

-- ModelProvider -> model_provider
CREATE TABLE IF NOT EXISTS `model_provider` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `code` VARCHAR(255) DEFAULT NULL COMMENT 'code',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `baseUrl` VARCHAR(255) DEFAULT NULL COMMENT 'baseUrl',
    `apiKey` VARCHAR(255) DEFAULT NULL COMMENT 'apiKey',
    `protocol` VARCHAR(255) DEFAULT NULL COMMENT 'protocol',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT 'enabled',
    `sort` INT NOT NULL DEFAULT 0 COMMENT 'sort',
    `description` TEXT DEFAULT NULL COMMENT 'description',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelProvider (auto-generated V3.0.0)';

-- ModelQuota -> model_quota
CREATE TABLE IF NOT EXISTS `model_quota` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `modelId` BIGINT NOT NULL DEFAULT 0 COMMENT 'modelId',
    `quotaDate` DATE DEFAULT NULL COMMENT 'quotaDate',
    `usedTokens` BIGINT NOT NULL DEFAULT 0 COMMENT 'usedTokens',
    `usedRequests` INT NOT NULL DEFAULT 0 COMMENT 'usedRequests',
    `limitTokens` BIGINT NOT NULL DEFAULT 0 COMMENT 'limitTokens',
    `limitRequests` INT NOT NULL DEFAULT 0 COMMENT 'limitRequests',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_quota_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelQuota (auto-generated V3.0.0)';

-- ModelRating -> model_rating
CREATE TABLE IF NOT EXISTS `model_rating` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `modelKey` VARCHAR(255) DEFAULT NULL COMMENT 'modelKey',
    `userId` VARCHAR(255) DEFAULT NULL COMMENT 'userId',
    `username` INT NOT NULL DEFAULT 0 COMMENT 'username',
    `rating` VARCHAR(255) DEFAULT NULL COMMENT 'rating',
    `comment` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'comment',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_rating_modelKey` (`modelKey`),
    UNIQUE KEY `uk_model_rating_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelRating (auto-generated V3.0.0)';

-- ModelVersion -> model_version
CREATE TABLE IF NOT EXISTS `model_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `versionId` VARCHAR(255) DEFAULT NULL COMMENT 'versionId',
    `modelEntryId` BIGINT NOT NULL DEFAULT 0 COMMENT 'modelEntryId',
    `version` VARCHAR(255) DEFAULT NULL COMMENT 'version',
    `changelog` VARCHAR(255) DEFAULT NULL COMMENT 'changelog',
    `filePath` VARCHAR(255) DEFAULT NULL COMMENT 'filePath',
    `sizeBytes` BIGINT NOT NULL DEFAULT 0 COMMENT 'sizeBytes',
    `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256',
    `inputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'inputSchema',
    `outputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'outputSchema',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `isLatest` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'isLatest',
    `uploaderId` BIGINT NOT NULL DEFAULT 0 COMMENT 'uploaderId',
    `backwardCompatible` VARCHAR(255) DEFAULT NULL COMMENT 'backwardCompatible',
    `metadata` VARCHAR(255) DEFAULT NULL COMMENT 'metadata',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelVersion (auto-generated V3.0.0)';

-- ModerationRecord -> moderation_record
CREATE TABLE IF NOT EXISTS `moderation_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `traceId` VARCHAR(255) DEFAULT NULL COMMENT 'traceId',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `contentType` VARCHAR(255) DEFAULT NULL COMMENT 'contentType',
    `contentHash` VARCHAR(255) DEFAULT NULL COMMENT 'contentHash',
    `contentSize` BIGINT NOT NULL DEFAULT 0 COMMENT 'contentSize',
    `contentUrl` VARCHAR(255) DEFAULT NULL COMMENT 'contentUrl',
    `moderationStatus` VARCHAR(255) DEFAULT NULL COMMENT 'moderationStatus',
    `riskLevel` VARCHAR(255) DEFAULT NULL COMMENT 'riskLevel',
    `riskLabels` VARCHAR(255) DEFAULT NULL COMMENT 'riskLabels',
    `riskScore` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT 'riskScore',
    `moderator` VARCHAR(255) DEFAULT NULL COMMENT 'moderator',
    `rejectionReason` VARCHAR(255) DEFAULT NULL COMMENT 'rejectionReason',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_moderation_record_userId` (`userId`),
    UNIQUE KEY `uk_moderation_record_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModerationRecord (auto-generated V3.0.0)';

-- MultimediaFile -> multimedia_file
CREATE TABLE IF NOT EXISTS `multimedia_file` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `fileId` VARCHAR(255) DEFAULT NULL COMMENT 'fileId',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `fileName` VARCHAR(255) DEFAULT NULL COMMENT 'fileName',
    `originalName` VARCHAR(255) DEFAULT NULL COMMENT 'originalName',
    `fileType` VARCHAR(255) DEFAULT NULL COMMENT 'fileType',
    `mimeType` VARCHAR(255) DEFAULT NULL COMMENT 'mimeType',
    `fileSize` BIGINT NOT NULL DEFAULT 0 COMMENT 'fileSize',
    `fileHash` VARCHAR(255) DEFAULT NULL COMMENT 'fileHash',
    `storagePath` VARCHAR(255) DEFAULT NULL COMMENT 'storagePath',
    `storageType` VARCHAR(255) DEFAULT NULL COMMENT 'storageType',
    `encrypted` INT NOT NULL DEFAULT 0 COMMENT 'encrypted',
    `durationMs` BIGINT NOT NULL DEFAULT 0 COMMENT 'durationMs',
    `width` INT NOT NULL DEFAULT 0 COMMENT 'width',
    `height` INT NOT NULL DEFAULT 0 COMMENT 'height',
    `bitrate` INT NOT NULL DEFAULT 0 COMMENT 'bitrate',
    `sampleRate` INT NOT NULL DEFAULT 0 COMMENT 'sampleRate',
    `channels` INT NOT NULL DEFAULT 0 COMMENT 'channels',
    `codec` VARCHAR(255) DEFAULT NULL COMMENT 'codec',
    `exif` VARCHAR(255) DEFAULT NULL COMMENT 'exif',
    `moderationStatus` VARCHAR(255) DEFAULT NULL COMMENT 'moderationStatus',
    `moderationId` BIGINT NOT NULL DEFAULT 0 COMMENT 'moderationId',
    `watermarked` INT NOT NULL DEFAULT 0 COMMENT 'watermarked',
    `isPublic` INT NOT NULL DEFAULT 0 COMMENT 'isPublic',
    `accessCount` INT NOT NULL DEFAULT 0 COMMENT 'accessCount',
    `expireAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'expireAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_multimedia_file_userId` (`userId`),
    UNIQUE KEY `uk_multimedia_file_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MultimediaFile (auto-generated V3.0.0)';

-- Notification -> notification
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `type` VARCHAR(255) DEFAULT NULL COMMENT 'type',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title',
    `content` TEXT DEFAULT NULL COMMENT 'content',
    `isRead` INT NOT NULL DEFAULT 0 COMMENT 'isRead',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notification_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Notification (auto-generated V3.0.0)';

-- OAuthAppConfig -> oauth_app_config
CREATE TABLE IF NOT EXISTS `oauth_app_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform',
    `appType` VARCHAR(255) DEFAULT NULL COMMENT 'appType',
    `appId` VARCHAR(255) DEFAULT NULL COMMENT 'appId',
    `appSecret` VARCHAR(255) DEFAULT NULL COMMENT 'appSecret',
    `publicKey` VARCHAR(255) DEFAULT NULL COMMENT 'publicKey',
    `redirectUri` VARCHAR(255) DEFAULT NULL COMMENT 'redirectUri',
    `scopes` VARCHAR(255) DEFAULT NULL COMMENT 'scopes',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT 'enabled',
    `extraConfig` VARCHAR(255) DEFAULT NULL COMMENT 'extraConfig',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuthAppConfig (auto-generated V3.0.0)';

-- OAuthBinding -> oauth_binding
CREATE TABLE IF NOT EXISTS `oauth_binding` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform',
    `appType` VARCHAR(255) DEFAULT NULL COMMENT 'appType',
    `openid` VARCHAR(255) DEFAULT NULL COMMENT 'openid',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar',
    `accessToken` VARCHAR(255) DEFAULT NULL COMMENT 'accessToken',
    `refreshToken` VARCHAR(255) DEFAULT NULL COMMENT 'refreshToken',
    `tokenExpiresAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'tokenExpiresAt',
    `rawData` VARCHAR(255) DEFAULT NULL COMMENT 'rawData',
    `boundAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'boundAt',
    `lastLoginAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastLoginAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_oauth_binding_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuthBinding (auto-generated V3.0.0)';

-- PipelineLog -> pipeline_log
CREATE TABLE IF NOT EXISTS `pipeline_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `sessionId` VARCHAR(255) DEFAULT NULL COMMENT 'sessionId',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `clientIp` VARCHAR(255) DEFAULT NULL COMMENT 'clientIp',
    `inputText` VARCHAR(255) DEFAULT NULL COMMENT 'inputText',
    `inputModality` VARCHAR(255) DEFAULT NULL COMMENT 'inputModality',
    `intent` VARCHAR(255) DEFAULT NULL COMMENT 'intent',
    `outputText` VARCHAR(255) DEFAULT NULL COMMENT 'outputText',
    `outputTokens` INT NOT NULL DEFAULT 0 COMMENT 'outputTokens',
    `computeDevice` VARCHAR(255) DEFAULT NULL COMMENT 'computeDevice',
    `computeMode` VARCHAR(255) DEFAULT NULL COMMENT 'computeMode',
    `totalCostMs` BIGINT NOT NULL DEFAULT 0 COMMENT 'totalCostMs',
    `stageCosts` VARCHAR(255) DEFAULT NULL COMMENT 'stageCosts',
    `riskLevel` VARCHAR(255) DEFAULT NULL COMMENT 'riskLevel',
    `needsReview` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'needsReview',
    `ragHits` INT NOT NULL DEFAULT 0 COMMENT 'ragHits',
    `toolCalls` INT NOT NULL DEFAULT 0 COMMENT 'toolCalls',
    `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pipeline_log_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PipelineLog (auto-generated V3.0.0)';

-- PipelineNodeLog -> pipeline_node_log
CREATE TABLE IF NOT EXISTS `pipeline_node_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `runId` BIGINT NOT NULL DEFAULT 0 COMMENT 'runId',
    `nodeId` VARCHAR(255) DEFAULT NULL COMMENT 'nodeId',
    `nodeName` VARCHAR(255) DEFAULT NULL COMMENT 'nodeName',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `endTime` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'endTime',
    `durationMs` BIGINT NOT NULL DEFAULT 0 COMMENT 'durationMs',
    `inputRows` INT NOT NULL DEFAULT 0 COMMENT 'inputRows',
    `outputRows` INT NOT NULL DEFAULT 0 COMMENT 'outputRows',
    `outputPreview` VARCHAR(255) DEFAULT NULL COMMENT 'outputPreview',
    `configSnapshot` VARCHAR(255) DEFAULT NULL COMMENT 'configSnapshot',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PipelineNodeLog (auto-generated V3.0.0)';

-- PipelineRun -> pipeline_run
CREATE TABLE IF NOT EXISTS `pipeline_run` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `workflowId` BIGINT NOT NULL DEFAULT 0 COMMENT 'workflowId',
    `workflowName` VARCHAR(255) DEFAULT NULL COMMENT 'workflowName',
    `triggerType` VARCHAR(255) DEFAULT NULL COMMENT 'triggerType',
    `endTime` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'endTime',
    `durationMs` BIGINT NOT NULL DEFAULT 0 COMMENT 'durationMs',
    `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage',
    `resultSummary` VARCHAR(255) DEFAULT NULL COMMENT 'resultSummary',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createTime',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PipelineRun (auto-generated V3.0.0)';

-- PipelineWorkflow -> pipeline_workflow
CREATE TABLE IF NOT EXISTS `pipeline_workflow` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `description` TEXT DEFAULT NULL COMMENT 'description',
    `definition` VARCHAR(255) DEFAULT NULL COMMENT 'definition',
    `status` INT NOT NULL DEFAULT 0 COMMENT 'status',
    `createBy` BIGINT NOT NULL DEFAULT 0 COMMENT 'createBy',
    `updateBy` BIGINT NOT NULL DEFAULT 0 COMMENT 'updateBy',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createTime',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updateTime',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PipelineWorkflow (auto-generated V3.0.0)';

-- PipelineWorkflowVersion -> pipeline_workflow_version
CREATE TABLE IF NOT EXISTS `pipeline_workflow_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `workflowId` BIGINT NOT NULL DEFAULT 0 COMMENT 'workflowId',
    `version` INT NOT NULL DEFAULT 0 COMMENT 'version',
    `definition` VARCHAR(255) DEFAULT NULL COMMENT 'definition',
    `createBy` BIGINT NOT NULL DEFAULT 0 COMMENT 'createBy',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createTime',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PipelineWorkflowVersion (auto-generated V3.0.0)';

-- Plugin -> plugin
CREATE TABLE IF NOT EXISTS `plugin` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `displayName` VARCHAR(255) DEFAULT NULL COMMENT 'displayName',
    `description` TEXT DEFAULT NULL COMMENT 'description',
    `version` VARCHAR(255) DEFAULT NULL COMMENT 'version',
    `author` VARCHAR(255) DEFAULT NULL COMMENT 'author',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category',
    `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope',
    `ownerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ownerId',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT 'icon',
    `entry` VARCHAR(255) DEFAULT NULL COMMENT 'entry',
    `pluginType` VARCHAR(255) DEFAULT NULL COMMENT 'pluginType',
    `config` TEXT DEFAULT NULL COMMENT 'config',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT 'enabled',
    `downloads` INT NOT NULL DEFAULT 0 COMMENT 'downloads',
    `rating` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT 'rating',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Plugin (auto-generated V3.0.0)';

-- PromptTemplate -> prompt_template
CREATE TABLE IF NOT EXISTS `prompt_template` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `description` TEXT DEFAULT NULL COMMENT 'description',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category',
    `content` TEXT DEFAULT NULL COMMENT 'content',
    `variables` VARCHAR(255) DEFAULT NULL COMMENT 'variables',
    `creatorId` BIGINT NOT NULL DEFAULT 0 COMMENT 'creatorId',
    `creatorName` VARCHAR(255) DEFAULT NULL COMMENT 'creatorName',
    `isPublic` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'isPublic',
    `useCount` INT NOT NULL DEFAULT 0 COMMENT 'useCount',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PromptTemplate (auto-generated V3.0.0)';

-- PushMessage -> push_message
CREATE TABLE IF NOT EXISTS `push_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `messageId` VARCHAR(255) DEFAULT NULL COMMENT 'messageId',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title',
    `body` VARCHAR(255) DEFAULT NULL COMMENT 'body',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT 'icon',
    `clickAction` VARCHAR(255) DEFAULT NULL COMMENT 'clickAction',
    `data` VARCHAR(255) DEFAULT NULL COMMENT 'data',
    `targetType` VARCHAR(255) DEFAULT NULL COMMENT 'targetType',
    `targetValue` VARCHAR(255) DEFAULT NULL COMMENT 'targetValue',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `successCount` INT NOT NULL DEFAULT 0 COMMENT 'successCount',
    `failureCount` INT NOT NULL DEFAULT 0 COMMENT 'failureCount',
    `error` VARCHAR(255) DEFAULT NULL COMMENT 'error',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PushMessage (auto-generated V3.0.0)';

-- PushSubscription -> push_subscription
CREATE TABLE IF NOT EXISTS `push_subscription` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `subscriptionId` VARCHAR(255) DEFAULT NULL COMMENT 'subscriptionId',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform',
    `endpoint` VARCHAR(255) DEFAULT NULL COMMENT 'endpoint',
    `p256dhKey` VARCHAR(255) DEFAULT NULL COMMENT 'p256dhKey',
    `authKey` VARCHAR(255) DEFAULT NULL COMMENT 'authKey',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `lastActiveAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastActiveAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_push_subscription_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PushSubscription (auto-generated V3.0.0)';

-- LogEntry -> raft_log
CREATE TABLE IF NOT EXISTS `raft_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `term` BIGINT NOT NULL DEFAULT 0 COMMENT 'term',
    `logIndex` BIGINT NOT NULL DEFAULT 0 COMMENT 'logIndex',
    `nodeId` VARCHAR(255) DEFAULT NULL COMMENT 'nodeId',
    `command` VARCHAR(255) DEFAULT NULL COMMENT 'command',
    `committed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'committed',
    `committedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'committedAt',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LogEntry (auto-generated V3.0.0)';

-- RateLimitRule -> rate_limit_rule
CREATE TABLE IF NOT EXISTS `rate_limit_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `key` VARCHAR(255) DEFAULT NULL COMMENT 'key',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope',
    `description` TEXT DEFAULT NULL COMMENT 'description',
    `capacity` INT NOT NULL DEFAULT 0 COMMENT 'capacity',
    `refillTokens` INT NOT NULL DEFAULT 0 COMMENT 'refillTokens',
    `periodSeconds` INT NOT NULL DEFAULT 0 COMMENT 'periodSeconds',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT 'enabled',
    `priority` INT NOT NULL DEFAULT 0 COMMENT 'priority',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RateLimitRule (auto-generated V3.0.0)';

-- RequestLog -> request_log
CREATE TABLE IF NOT EXISTS `request_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `traceId` VARCHAR(255) DEFAULT NULL COMMENT 'traceId',
    `method` VARCHAR(255) DEFAULT NULL COMMENT 'method',
    `path` VARCHAR(255) DEFAULT NULL COMMENT 'path',
    `queryString` VARCHAR(255) DEFAULT NULL COMMENT 'queryString',
    `clientIp` VARCHAR(255) DEFAULT NULL COMMENT 'clientIp',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `status` INT NOT NULL DEFAULT 0 COMMENT 'status',
    `latencyMs` BIGINT NOT NULL DEFAULT 0 COMMENT 'latencyMs',
    `slow` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'slow',
    `error` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'error',
    `module` VARCHAR(255) DEFAULT NULL COMMENT 'module',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_request_log_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RequestLog (auto-generated V3.0.0)';

-- SensitiveWord -> sensitive_word
CREATE TABLE IF NOT EXISTS `sensitive_word` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `word` VARCHAR(255) DEFAULT NULL COMMENT 'word',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category',
    `level` VARCHAR(255) DEFAULT NULL COMMENT 'level',
    `action` VARCHAR(255) DEFAULT NULL COMMENT 'action',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT 'enabled',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SensitiveWord (auto-generated V3.0.0)';

-- SysRole -> sys_role
CREATE TABLE IF NOT EXISTS `sys_role` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `code` VARCHAR(255) DEFAULT NULL COMMENT 'code',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `description` TEXT DEFAULT NULL COMMENT 'description',
    `sort` INT NOT NULL DEFAULT 0 COMMENT 'sort',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT 'enabled',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SysRole (auto-generated V3.0.0)';

-- SysUser -> sys_user
CREATE TABLE IF NOT EXISTS `sys_user` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdBy` BIGINT NOT NULL DEFAULT 0 COMMENT 'createdBy',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedBy` BIGINT NOT NULL DEFAULT 0 COMMENT 'updatedBy',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `password` VARCHAR(255) DEFAULT NULL COMMENT 'password',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname',
    `email` VARCHAR(255) DEFAULT NULL COMMENT 'email',
    `phone` VARCHAR(255) DEFAULT NULL COMMENT 'phone',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar',
    `gender` INT NOT NULL DEFAULT 0 COMMENT 'gender',
    `status` INT NOT NULL DEFAULT 0 COMMENT 'status',
    `lastLoginIp` VARCHAR(255) DEFAULT NULL COMMENT 'lastLoginIp',
    `lastLoginAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastLoginAt',
    `tenantId` BIGINT NOT NULL DEFAULT 0 COMMENT 'tenantId',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT 'remark',
    `wechatOpenid` VARCHAR(255) DEFAULT NULL COMMENT 'wechatOpenid',
    `wechatUnionid` VARCHAR(255) DEFAULT NULL COMMENT 'wechatUnionid',
    `wechatNickname` VARCHAR(255) DEFAULT NULL COMMENT 'wechatNickname',
    `wechatAvatar` VARCHAR(255) DEFAULT NULL COMMENT 'wechatAvatar',
    `wechatBoundAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'wechatBoundAt',
    `qqOpenid` VARCHAR(255) DEFAULT NULL COMMENT 'qqOpenid',
    `qqUnionid` VARCHAR(255) DEFAULT NULL COMMENT 'qqUnionid',
    `qqNickname` VARCHAR(255) DEFAULT NULL COMMENT 'qqNickname',
    `qqAvatar` VARCHAR(255) DEFAULT NULL COMMENT 'qqAvatar',
    `qqBoundAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'qqBoundAt',
    `alipayOpenid` VARCHAR(255) DEFAULT NULL COMMENT 'alipayOpenid',
    `alipayUserId` VARCHAR(255) DEFAULT NULL COMMENT 'alipayUserId',
    `alipayNickname` VARCHAR(255) DEFAULT NULL COMMENT 'alipayNickname',
    `alipayAvatar` VARCHAR(255) DEFAULT NULL COMMENT 'alipayAvatar',
    `alipayBoundAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'alipayBoundAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SysUser (auto-generated V3.0.0)';

-- SysUserRole -> sys_user_role
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `roleId` BIGINT NOT NULL DEFAULT 0 COMMENT 'roleId',
    UNIQUE KEY `uk_sys_user_role_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SysUserRole (auto-generated V3.0.0)';

-- Tenant -> tenant
CREATE TABLE IF NOT EXISTS `tenant` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `code` VARCHAR(255) DEFAULT NULL COMMENT 'code',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `plan` VARCHAR(255) DEFAULT NULL COMMENT 'plan',
    `status` INT NOT NULL DEFAULT 0 COMMENT 'status',
    `maxUsers` INT NOT NULL DEFAULT 0 COMMENT 'maxUsers',
    `maxModels` INT NOT NULL DEFAULT 0 COMMENT 'maxModels',
    `qpsLimit` INT NOT NULL DEFAULT 0 COMMENT 'qpsLimit',
    `monthlyQuota` BIGINT NOT NULL DEFAULT 0 COMMENT 'monthlyQuota',
    `usedQuota` BIGINT NOT NULL DEFAULT 0 COMMENT 'usedQuota',
    `expireAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'expireAt',
    `contactEmail` VARCHAR(255) DEFAULT NULL COMMENT 'contactEmail',
    `contactPhone` VARCHAR(255) DEFAULT NULL COMMENT 'contactPhone',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT 'remark',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tenant (auto-generated V3.0.0)';

-- TrainingCheckpoint -> training_checkpoint
CREATE TABLE IF NOT EXISTS `training_checkpoint` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `taskId` VARCHAR(255) DEFAULT NULL COMMENT 'taskId',
    `checkpointId` VARCHAR(255) DEFAULT NULL COMMENT 'checkpointId',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `epoch` INT NOT NULL DEFAULT 0 COMMENT 'epoch',
    `step` INT NOT NULL DEFAULT 0 COMMENT 'step',
    `filePath` VARCHAR(255) DEFAULT NULL COMMENT 'filePath',
    `sizeBytes` BIGINT NOT NULL DEFAULT 0 COMMENT 'sizeBytes',
    `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256',
    `valLoss` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'valLoss',
    `accuracy` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'accuracy',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `metadata` VARCHAR(255) DEFAULT NULL COMMENT 'metadata',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TrainingCheckpoint (auto-generated V3.0.0)';

-- TrainingJob -> training_job
CREATE TABLE IF NOT EXISTS `training_job` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `taskId` VARCHAR(255) DEFAULT NULL COMMENT 'taskId',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `model` VARCHAR(255) DEFAULT NULL COMMENT 'model',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `totalEpochs` INT NOT NULL DEFAULT 0 COMMENT 'totalEpochs',
    `currentEpoch` INT NOT NULL DEFAULT 0 COMMENT 'currentEpoch',
    `currentStep` INT NOT NULL DEFAULT 0 COMMENT 'currentStep',
    `startTimeMs` BIGINT NOT NULL DEFAULT 0 COMMENT 'startTimeMs',
    `endTimeMs` BIGINT NOT NULL DEFAULT 0 COMMENT 'endTimeMs',
    `config` TEXT DEFAULT NULL COMMENT 'config',
    `error` VARCHAR(255) DEFAULT NULL COMMENT 'error',
    `ownerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ownerId',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `lastLoss` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'lastLoss',
    `lastValLoss` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'lastValLoss',
    `lastAccuracy` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'lastAccuracy',
    `totalSteps` INT NOT NULL DEFAULT 0 COMMENT 'totalSteps',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TrainingJob (auto-generated V3.0.0)';

-- TrainingMetric -> training_metric
CREATE TABLE IF NOT EXISTS `training_metric` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `timestamp` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'timestamp',
    `taskId` VARCHAR(255) DEFAULT NULL COMMENT 'taskId',
    `epoch` INT NOT NULL DEFAULT 0 COMMENT 'epoch',
    `step` INT NOT NULL DEFAULT 0 COMMENT 'step',
    `loss` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'loss',
    `valLoss` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'valLoss',
    `accuracy` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'accuracy',
    `learningRate` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'learningRate',
    `elapsedMs` BIGINT NOT NULL DEFAULT 0 COMMENT 'elapsedMs',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TrainingMetric (auto-generated V3.0.0)';

-- TrainingTask -> training_task
CREATE TABLE IF NOT EXISTS `training_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `modelName` VARCHAR(255) DEFAULT NULL COMMENT 'modelName',
    `corpusPath` VARCHAR(255) DEFAULT NULL COMMENT 'corpusPath',
    `nHead` INT NOT NULL DEFAULT 0 COMMENT 'nHead',
    `nEmbd` INT NOT NULL DEFAULT 0 COMMENT 'nEmbd',
    `blockSize` INT NOT NULL DEFAULT 0 COMMENT 'blockSize',
    `maxIters` INT NOT NULL DEFAULT 0 COMMENT 'maxIters',
    `batchSize` INT NOT NULL DEFAULT 0 COMMENT 'batchSize',
    `learningRate` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'learningRate',
    `progress` INT NOT NULL DEFAULT 0 COMMENT 'progress',
    `currentIter` INT NOT NULL DEFAULT 0 COMMENT 'currentIter',
    `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage',
    `completedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'completedAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_training_task_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TrainingTask (auto-generated V3.0.0)';

-- UnionidRelations -> unionid_relations
CREATE TABLE IF NOT EXISTS `unionid_relations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform',
    `firstSeenAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'firstSeenAt',
    `lastSeenAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastSeenAt',
    `bindingCount` INT NOT NULL DEFAULT 0 COMMENT 'bindingCount',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_unionid_relations_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='UnionidRelations (auto-generated V3.0.0)';

-- UserApiKey -> user_api_key
CREATE TABLE IF NOT EXISTS `user_api_key` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `keyHash` VARCHAR(255) DEFAULT NULL COMMENT 'keyHash',
    `keyPrefix` VARCHAR(255) DEFAULT NULL COMMENT 'keyPrefix',
    `scopes` VARCHAR(255) DEFAULT NULL COMMENT 'scopes',
    `expiresAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'expiresAt',
    `lastUsedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastUsedAt',
    `useCount` BIGINT NOT NULL DEFAULT 0 COMMENT 'useCount',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT 'enabled',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_api_key_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='UserApiKey (auto-generated V3.0.0)';

-- Webhook -> webhook
CREATE TABLE IF NOT EXISTS `webhook` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `webhookId` VARCHAR(255) DEFAULT NULL COMMENT 'webhookId',
    `name` TEXT DEFAULT NULL COMMENT 'name',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
    `url` VARCHAR(255) DEFAULT NULL COMMENT 'url',
    `events` VARCHAR(255) DEFAULT NULL COMMENT 'events',
    `secret` VARCHAR(255) DEFAULT NULL COMMENT 'secret',
    `customHeaders` INT NOT NULL DEFAULT 0 COMMENT 'customHeaders',
    `enabled` VARCHAR(255) DEFAULT NULL COMMENT 'enabled',
    `status` BIGINT NOT NULL DEFAULT 0 COMMENT 'status',
    `deliveryCount` BIGINT NOT NULL DEFAULT 0 COMMENT 'deliveryCount',
    `successCount` BIGINT NOT NULL DEFAULT 0 COMMENT 'successCount',
    `failCount` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'failCount',
    `lastDeliveryAt` INT NOT NULL DEFAULT 0 COMMENT 'lastDeliveryAt',
    `lastStatus` BIGINT NOT NULL DEFAULT 0 COMMENT 'lastStatus',
    `ownerId` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'ownerId',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_webhook_webhookId` (`webhookId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Webhook (auto-generated V3.0.0)';

-- WebhookDelivery -> webhook_delivery
CREATE TABLE IF NOT EXISTS `webhook_delivery` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `webhookId` VARCHAR(255) DEFAULT NULL COMMENT 'webhookId',
    `eventType` VARCHAR(255) DEFAULT NULL COMMENT 'eventType',
    `eventId` MEDIUMTEXT DEFAULT NULL COMMENT 'eventId',
    `payload` INT NOT NULL DEFAULT 0 COMMENT 'payload',
    `responseStatus` VARCHAR(255) DEFAULT NULL COMMENT 'responseStatus',
    `responseBody` BIGINT NOT NULL DEFAULT 0 COMMENT 'responseBody',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `retryCount` VARCHAR(255) DEFAULT NULL COMMENT 'retryCount',
    `errorMsg` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'errorMsg',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_webhook_delivery_webhookId` (`webhookId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WebhookDelivery (auto-generated V3.0.0)';

-- WechatConfig -> wechat_config
CREATE TABLE IF NOT EXISTS `wechat_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `appType` VARCHAR(255) DEFAULT NULL COMMENT 'appType',
    `appSecret` VARCHAR(255) DEFAULT NULL COMMENT 'appSecret',
    `token` VARCHAR(255) DEFAULT NULL COMMENT 'token',
    `aesKey` VARCHAR(255) DEFAULT NULL COMMENT 'aesKey',
    `redirectUri` VARCHAR(255) DEFAULT NULL COMMENT 'redirectUri',
    `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT 'enabled',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT 'remark',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WechatConfig (auto-generated V3.0.0)';

-- WechatScanSession -> wechat_scan_session
CREATE TABLE IF NOT EXISTS `wechat_scan_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createdAt',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updatedAt',
    `ticket` VARCHAR(255) DEFAULT NULL COMMENT 'ticket',
    `sceneId` VARCHAR(255) DEFAULT NULL COMMENT 'sceneId',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `openid` VARCHAR(255) DEFAULT NULL COMMENT 'openid',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `accessToken` VARCHAR(255) DEFAULT NULL COMMENT 'accessToken',
    `refreshToken` VARCHAR(255) DEFAULT NULL COMMENT 'refreshToken',
    `clientIp` VARCHAR(255) DEFAULT NULL COMMENT 'clientIp',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent',
    `expiresAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'expiresAt',
    `confirmedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'confirmedAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wechat_scan_session_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WechatScanSession (auto-generated V3.0.0)';

-- WechatUserBinding -> wechat_user_binding
CREATE TABLE IF NOT EXISTS `wechat_user_binding` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `boundAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'boundAt',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT 'userId',
    `openid` VARCHAR(255) DEFAULT NULL COMMENT 'openid',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid',
    `appType` VARCHAR(255) DEFAULT NULL COMMENT 'appType',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar',
    `lastLoginAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastLoginAt',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wechat_user_binding_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WechatUserBinding (auto-generated V3.0.0)';

-- SET FOREIGN_KEY_CHECKS = 1;

-- 全部 DDL 生成完毕


-- 种子数据 (合并)

-- ====================================================================
-- MiniMax Platform V3.0.0+ 初始种子数据
-- 让新装环境可立即使用: 1 个超管 + 21 个 AI 工具 + 3 个示例 Agent
--
-- 用法: 先跑 init.sql 建表, 再跑 init_seeds.sql 加种子
--   mysql -u root -p minimax_platform < sql/init.sql
--   mysql -u root -p minimax_platform < sql/init_seeds.sql
-- ====================================================================

-- USE minimax_platform; (已合并)

-- ===========================================
-- 1. 默认角色 (4 个)
-- ===========================================
-- INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `enabled`, `createdAt`, `updatedAt`) VALUES (1, 'SUPER_ADMIN', '超管', '拥有所有权限', 1, NOW(), NOW());
-- INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `enabled`, `createdAt`, `updatedAt`) VALUES (2, 'ADMIN', '管理员', '后台管理权限', 1, NOW(), NOW());
-- INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `enabled`, `createdAt`, `updatedAt`) VALUES (3, 'USER', '普通用户', '基础使用权限', 1, NOW(), NOW());
-- INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `enabled`, `createdAt`, `updatedAt`) VALUES (4, 'GUEST', '访客', '只读权限', 1, NOW(), NOW());
-- -- ===========================================
-- -- 2. 超管账号 (adminLiugl / Liugl@2026)
-- -- ===========================================
-- INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `status`, `createdAt`, `updatedAt`) VALUES (1, 'adminLiugl', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '超管', 'admin@minimax.com', 1, NOW(), NOW());
-- INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `status`, `createdAt`, `updatedAt`) VALUES (2, 'demo', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '演示账号', 'demo@minimax.com', 1, NOW(), NOW());
-- -- ===========================================
-- -- 3. 角色分配
-- -- ===========================================
-- INSERT INTO `sys_user_role` (`userId`, `roleId`) VALUES (1, 1);
-- INSERT INTO `sys_user_role` (`userId`, `roleId`) VALUES (2, 3);
-- -- ===========================================
-- -- 4. AI 意图关键词 (10 个)
-- -- ===========================================
-- INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (1, 'writing', '写文章', 1, 0, 1, NOW());
-- INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (2, 'writing', '写报告', 9, 0, 1, NOW());
-- INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (3, 'writing', '润色', 8, 0, 1, NOW());
-- INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (4, 'coding', '写代码', 10, 0, 1, NOW());
-- INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (5, 'coding', 'debug', 9, 0, 1, NOW());
-- INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (6, 'coding', '重构', 8, 0, 1, NOW());
-- INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (7, 'analysis', '分析', 10, 0, 1, NOW());
-- INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (8, 'analysis', '报表', 9, 0, 1, NOW());
-- INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (9, 'translate', '翻译', 10, 0, 1, NOW());
-- INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (10, 'translate', 'convert', 8, 0, 1, NOW());
-- -- ===========================================
-- -- 5. AI 工具 (21 个)
-- -- ===========================================
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (1, 'nl2sql', 'NL2SQL', '自然语言转 SQL', 'data', '1.0.0', 1, '{"query":"string"}', '{"sql":"string"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (2, 'nl2chart', 'NL2Chart', '自然语言转图表', 'data', '1.0.0', 1, '{"query":"string","data":"json"}', '{"chart":"base64"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (3, 'doc.parse', '文档解析', 'PDF/Word/Excel 解析', 'document', '1.0.0', 1, '{"url":"string"}', '{"text":"string"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (4, 'doc.summary', '文档摘要', '长文本摘要', 'document', '1.0.0', 1, '{"text":"string"}', '{"summary":"string"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (5, 'crdt.merge', 'CRDT 合并', '多人协同冲突合并', 'collab', '1.0.0', 1, '{"text":"string","ops":"json"}', '{"text":"string"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (6, 'code.gen', '代码生成', '自然语言转代码', 'coding', '1.0.0', 1, '{"spec":"string","lang":"string"}', '{"code":"string"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (7, 'code.review', '代码审查', '静态分析+建议', 'coding', '1.0.0', 1, '{"code":"string"}', '{"review":"string"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (8, 'project.pack', '项目打包', 'ZIP 打包源码', 'coding', '1.0.0', 1, '{"files":"json"}', '{"zip":"base64"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (9, 'kg.extract', '知识图谱抽取', '实体关系抽取', 'kg', '1.0.0', 1, '{"text":"string"}', '{"entities":"json","relations":"json"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (10, 'rag.search', 'RAG 检索', '向量+关键词混合检索', 'rag', '1.0.0', 1, '{"query":"string","topK":5}', '{"docs":"json"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (11, 'moderation.text', '文本审核', '敏感词+合规检测', 'compliance', '1.0.0', 1, '{"text":"string"}', '{"safe":"boolean","hits":"json"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (12, 'moderation.image', '图像审核', 'NSFW 检测', 'compliance', '1.0.0', 1, '{"url":"string"}', '{"safe":"boolean"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (13, 'function.call', '函数调用', 'OpenAI Functions 协议', 'function', '1.0.0', 1, '{"name":"string","args":"json"}', '{"result":"json"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (14, 'pipeline.run', 'Pipeline 执行', '多阶段 AI 管线', 'pipeline', '1.0.0', 1, '{"workflow":"string","input":"json"}', '{"output":"json"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (15, 'music.gen', '音乐生成', '文本转 MIDI', 'multimodal', '1.0.0', 1, '{"prompt":"string"}', '{"midi":"base64"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (16, 'vision.describe', '图像描述', 'Vision API', 'multimodal', '1.0.0', 1, '{"image":"base64"}', '{"text":"string"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (17, 'intent.recog', '意图识别', '问句意图分类', 'agent', '1.0.0', 1, '{"query":"string"}', '{"intent":"string","score":"number"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (18, 'agent.route', 'Agent 路由', '能力匹配路由', 'agent', '1.0.0', 1, '{"query":"string"}', '{"agent":"string"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (19, 'agent.exec', 'Agent 执行', 'ReAct 循环', 'agent', '1.0.0', 1, '{"task":"string"}', '{"output":"string","steps":"json"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (20, 'embedding', '向量化', '文本→向量', 'embedding', '1.0.0', 1, '{"text":"string"}', '{"vector":"json"}', NOW(), NOW());
-- INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (21, 'ppt.gen', 'PPT 生成', '大纲→PPTX', 'document', '1.0.0', 1, '{"outline":"string","theme":"string"}', '{"file":"base64"}', NOW(), NOW());
-- -- ===========================================
-- -- 6. 提示词模板 (4 个)
-- -- ===========================================
-- INSERT INTO `prompt_template` (`id`, `name`, `description`, `category`, `content`, `createdAt`, `updatedAt`) VALUES (1, '系统基础', '系统默认提示词', 'system', '你是 MiniMax AI 助手。', NOW(), NOW());
-- INSERT INTO `prompt_template` (`id`, `name`, `description`, `category`, `content`, `createdAt`, `updatedAt`) VALUES (2, '写文章', '通用文章模板', 'writing', '请以专业清晰的口吻撰写。', NOW(), NOW());
-- INSERT INTO `prompt_template` (`id`, `name`, `description`, `category`, `content`, `createdAt`, `updatedAt`) VALUES (3, '代码审查', '代码审查模板', 'coding', '从可读性/性能/安全性审查。', NOW(), NOW());
-- INSERT INTO `prompt_template` (`id`, `name`, `description`, `category`, `content`, `createdAt`, `updatedAt`) VALUES (4, '分析报告', '分析报告模板', 'analysis', '基于数据生成分析报告。', NOW(), NOW());
-- -- ===========================================
-- -- 7. 模型提供方 (4 个)
-- -- ===========================================
-- INSERT INTO `model_provider` (`id`, `code`, `name`, `baseUrl`, `protocol`, `enabled`, `sort`, `createdAt`, `updatedAt`) VALUES (1, 'mock', 'Mock 提供方', 'builtin://mock', 'openai', 1, 0, NOW(), NOW());
-- INSERT INTO `model_provider` (`id`, `code`, `name`, `baseUrl`, `protocol`, `enabled`, `sort`, `createdAt`, `updatedAt`) VALUES (2, 'builtin', '自研提供方', 'builtin://self', 'openai', 1, 1, NOW(), NOW());
-- INSERT INTO `model_provider` (`id`, `code`, `name`, `baseUrl`, `protocol`, `enabled`, `sort`, `createdAt`, `updatedAt`) VALUES (3, 'onnx-local', '本地 ONNX', '/var/minimax/models', 'onnx', 0, 2, NOW(), NOW());
-- INSERT INTO `model_provider` (`id`, `code`, `name`, `baseUrl`, `protocol`, `enabled`, `sort`, `createdAt`, `updatedAt`) VALUES (4, 'openai', 'OpenAI 兼容', 'https://api.openai.com/v1', 'openai', 0, 9, NOW(), NOW());
-- -- ===========================================
-- -- 8. 数据源 (2 个) (只含实际列, userId 必须不同)
-- -- ===========================================
-- INSERT INTO `analytics_datasource` (`id`, `userId`, `name`, `type`, `passwordEnc`, `createdAt`, `updatedAt`) VALUES (1, 1, 'H2 默认', 'h2', '', NOW(), NOW());
-- INSERT INTO `analytics_datasource` (`id`, `userId`, `name`, `type`, `passwordEnc`, `createdAt`, `updatedAt`) VALUES (2, 2, 'MySQL 默认', 'mysql', '', NOW(), NOW());
-- -- ===========================================
-- -- 9. 告警规则 (4 个) (实际列: metricName 不是 metric)
-- -- ===========================================
-- INSERT INTO `alert_rule` (`id`, `name`, `metricName`, `operator`, `threshold`, `severity`, `enabled`, `createdAt`, `updatedAt`) VALUES (1, 'CPU 高', 'cpu_usage', '>', 0.9, 'critical', 1, NOW(), NOW());
-- INSERT INTO `alert_rule` (`id`, `name`, `metricName`, `operator`, `threshold`, `severity`, `enabled`, `createdAt`, `updatedAt`) VALUES (2, '内存高', 'memory_usage', '>', 0.85, 'warning', 1, NOW(), NOW());
-- INSERT INTO `alert_rule` (`id`, `name`, `metricName`, `operator`, `threshold`, `severity`, `enabled`, `createdAt`, `updatedAt`) VALUES (3, 'API 错误率高', 'api_error_rate', '>', 0.05, 'warning', 1, NOW(), NOW());
-- INSERT INTO `alert_rule` (`id`, `name`, `metricName`, `operator`, `threshold`, `severity`, `enabled`, `createdAt`, `updatedAt`) VALUES (4, 'AI 响应慢', 'ai_latency_ms', '>', 5000, 'warning', 1, NOW(), NOW());
-- -- ===========================================
-- -- 10. 敏感词 (3 个)
-- -- ===========================================
-- INSERT INTO `sensitive_word` (`id`, `word`, `category`, `level`, `createdAt`) VALUES (1, '违禁词示例', 'politics', 3, NOW());
-- INSERT INTO `sensitive_word` (`id`, `word`, `category`, `level`, `createdAt`) VALUES (2, '广告', 'ad', 1, NOW());
-- INSERT INTO `sensitive_word` (`id`, `word`, `category`, `level`, `createdAt`) VALUES (3, '暴力', 'violence', 2, NOW());
-- -- ===========================================
-- -- 11. 插件 (3 个) (实际列: name + displayName 等)
-- -- ===========================================
-- INSERT INTO `plugin` (`id`, `name`, `displayName`, `version`, `enabled`, `config`, `createdAt`, `updatedAt`) VALUES (1, 'pwa', 'PWA 离线插件', '1.0.0', 1, '{"cacheStrategy":"NetworkFirst"}', NOW(), NOW());
-- INSERT INTO `plugin` (`id`, `name`, `displayName`, `version`, `enabled`, `config`, `createdAt`, `updatedAt`) VALUES (2, 'capacitor', '移动端插件', '1.0.0', 1, '{"appId":"com.minimax.platform"}', NOW(), NOW());
-- INSERT INTO `plugin` (`id`, `name`, `displayName`, `version`, `enabled`, `config`, `createdAt`, `updatedAt`) VALUES (3, 'i18n', '国际化插件', '1.0.0', 1, '{"default":"zh-CN"}', NOW(), NOW());
-- -- ===========================================
-- -- 12. 协作房间 (跳过: gen_ddl.py 生成的 collab_room 不含 name 列)
-- -- ===========================================
-- -- 种子数据完毕 (已删除 ON DUPLICATE KEY UPDATE 以保证 H2/MySQL 兼容性)