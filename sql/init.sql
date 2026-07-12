-- ===================================================================
-- MiniMax Platform V3.0.0 全量 DDL (单文件汇总)
-- 表数: 62
-- 字符集: utf8mb4 / 引擎: InnoDB
-- ===================================================================

CREATE DATABASE IF NOT EXISTS `minimax_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `minimax_platform`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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

-- AgentTask -> agent_task
CREATE TABLE IF NOT EXISTS `agent_task` (
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
    `severity` VARCHAR(255) DEFAULT NULL COMMENT 'severity',
    `cooldownMinutes` INT NOT NULL DEFAULT 0 COMMENT 'cooldownMinutes',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT 'enabled',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `notifyChannel` VARCHAR(255) DEFAULT NULL COMMENT 'notifyChannel',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AlertRule (auto-generated V3.0.0)';

-- DataSource -> analytics_datasource
CREATE TABLE IF NOT EXISTS `analytics_datasource` (
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

-- ChatMessage -> chat_message
CREATE TABLE IF NOT EXISTS `chat_message` (
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

-- DbDataSource -> data_source
CREATE TABLE IF NOT EXISTS `data_source` (
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

-- KgEntity -> kg_entity
CREATE TABLE IF NOT EXISTS `kg_entity` (
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

-- ModelProvider -> model_provider
CREATE TABLE IF NOT EXISTS `model_provider` (
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

SET FOREIGN_KEY_CHECKS = 1;

-- ========================================================
-- V3.2.1: 看板指标 (dashboard_metric)
-- ========================================================
CREATE TABLE IF NOT EXISTS `dashboard_metric` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `metric` VARCHAR(64) NOT NULL COMMENT '指标名 (e.g. user.total, ai.call.count)',
    `dimension` VARCHAR(64) NOT NULL DEFAULT 'global' COMMENT '维度 (global/tool:ppt.gen)',
    `value` DOUBLE NOT NULL DEFAULT 0 COMMENT '指标值',
    `tags` VARCHAR(512) DEFAULT NULL COMMENT '额外标签JSON',
    `timestamp` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '快照时间',
    PRIMARY KEY (`id`),
    KEY `idx_dashboard_metric_metric_ts` (`metric`, `timestamp`),
    KEY `idx_dashboard_metric_dimension` (`dimension`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DashboardMetric (V3.2.1 看板指标历史)';

-- ========================================================
-- V3.2.0: 训练可视化 (training_job / training_metric / training_checkpoint)
-- ========================================================
CREATE TABLE IF NOT EXISTS `training_job` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `taskId` VARCHAR(64) NOT NULL COMMENT '业务taskId (UUID)',
    `name` VARCHAR(255) NOT NULL COMMENT '任务名',
    `model` VARCHAR(255) DEFAULT NULL COMMENT '模型名',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/COMPLETED/FAILED/CANCELLED',
    `totalEpochs` INT NOT NULL DEFAULT 0 COMMENT '总epoch数',
    `currentEpoch` INT NOT NULL DEFAULT 0 COMMENT '当前epoch',
    `currentStep` INT NOT NULL DEFAULT 0 COMMENT '当前step',
    `startTimeMs` BIGINT NOT NULL DEFAULT 0 COMMENT '起始时间戳ms',
    `endTimeMs` BIGINT NOT NULL DEFAULT 0 COMMENT '结束时间戳ms (0=未结束)',
    `config` TEXT DEFAULT NULL COMMENT '配置JSON',
    `error` TEXT DEFAULT NULL COMMENT '错误信息',
    `ownerId` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签',
    `lastLoss` DOUBLE DEFAULT NULL COMMENT '最新loss',
    `lastValLoss` DOUBLE DEFAULT NULL COMMENT '最新val_loss',
    `lastAccuracy` DOUBLE DEFAULT NULL COMMENT '最新accuracy',
    `totalSteps` INT NOT NULL DEFAULT 0 COMMENT '总步数',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_training_job_taskId` (`taskId`),
    KEY `idx_training_job_status` (`status`),
    KEY `idx_training_job_ownerId` (`ownerId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TrainingJob (V3.2.0 训练任务)';

CREATE TABLE IF NOT EXISTS `training_metric` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `taskId` VARCHAR(64) NOT NULL COMMENT '业务taskId',
    `epoch` INT NOT NULL DEFAULT 0 COMMENT '当前epoch',
    `step` INT NOT NULL DEFAULT 0 COMMENT '当前step',
    `loss` DOUBLE NOT NULL DEFAULT 0 COMMENT '训练loss',
    `valLoss` DOUBLE NOT NULL DEFAULT 0 COMMENT '验证loss',
    `accuracy` DOUBLE NOT NULL DEFAULT 0 COMMENT '准确率',
    `learningRate` DOUBLE NOT NULL DEFAULT 0 COMMENT '学习率',
    `elapsedMs` BIGINT NOT NULL DEFAULT 0 COMMENT '累计耗时ms',
    `timestamp` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上报时间',
    PRIMARY KEY (`id`),
    KEY `idx_training_metric_taskId_step` (`taskId`, `step`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TrainingMetric (V3.2.0 训练指标历史)';

CREATE TABLE IF NOT EXISTS `training_checkpoint` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `taskId` VARCHAR(64) NOT NULL COMMENT '业务taskId',
    `checkpointId` VARCHAR(64) NOT NULL COMMENT 'checkpoint业务ID',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'checkpoint名称 (e.g. best-val-loss)',
    `epoch` INT NOT NULL DEFAULT 0 COMMENT 'epoch',
    `step` INT NOT NULL DEFAULT 0 COMMENT 'step',
    `filePath` VARCHAR(512) NOT NULL COMMENT '文件路径',
    `sizeBytes` BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小 (字节)',
    `sha256` VARCHAR(64) DEFAULT NULL COMMENT 'SHA256校验',
    `valLoss` DOUBLE DEFAULT NULL COMMENT 'val_loss (排序用)',
    `accuracy` DOUBLE DEFAULT NULL COMMENT 'accuracy (排序用)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签 (best/latest/milestone)',
    `metadata` TEXT DEFAULT NULL COMMENT '元数据JSON',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_training_checkpoint_checkpointId` (`checkpointId`),
    KEY `idx_training_checkpoint_taskId` (`taskId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TrainingCheckpoint (V3.2.0 检查点)';

-- ========================================================
-- V3.0.3: agent_group 智能体群组表
-- ========================================================
CREATE TABLE IF NOT EXISTS `agent_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    `groupId` VARCHAR(64) NOT NULL COMMENT '群组业务ID (UUID)',
    `name` VARCHAR(255) NOT NULL COMMENT '群组名',
    `description` TEXT DEFAULT NULL COMMENT '群组描述',
    `strategy` VARCHAR(32) NOT NULL DEFAULT 'PIPELINE' COMMENT '协作策略: PIPELINE/DEBATE/VOTE/SWARM',
    `membersJson` TEXT DEFAULT NULL COMMENT '群成员JSON: [{agentName, role, weight, capability, order}]',
    `status` VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT '状态: CREATED/RUNNING/COMPLETED/FAILED',
    `ownerId` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签 (逗号分隔)',
    `lastRunAt` DATETIME DEFAULT NULL COMMENT '最后运行时间',
    `runCount` INT NOT NULL DEFAULT 0 COMMENT '总运行次数',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_group_groupId` (`groupId`),
    KEY `idx_agent_group_ownerId` (`ownerId`),
    KEY `idx_agent_group_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AgentGroup (V3.0.3 智能体群组)';

-- 全部 DDL 生成完毕
