-- ============================================================
-- MiniMax Platform V2.8.2 全量 DDL (自动生成)
-- 生成时间: 2026-07-12
-- 字符集: utf8mb4 / 引擎: InnoDB
-- ============================================================
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- AdminAuditLog -> admin_audit_log
DROP TABLE IF EXISTS `admin_audit_log`;
CREATE TABLE `admin_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `actorId` BIGINT NOT NULL DEFAULT 0,
    `actorName` VARCHAR(255) DEFAULT NULL,
    `action` VARCHAR(255) DEFAULT NULL,
    `resourceType` VARCHAR(255) DEFAULT NULL,
    `resourceId` VARCHAR(255) DEFAULT NULL,
    `detail` VARCHAR(255) DEFAULT NULL,
    `result` VARCHAR(255) DEFAULT NULL,
    `errorMsg` VARCHAR(255) DEFAULT NULL,
    `ip` VARCHAR(255) DEFAULT NULL,
    `userAgent` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AdminAuditLog';

-- AuditLogFull -> audit_log_full
DROP TABLE IF EXISTS `audit_log_full`;
CREATE TABLE `audit_log_full` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `traceId` VARCHAR(255) DEFAULT NULL,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `username` VARCHAR(255) DEFAULT NULL,
    `userIp` VARCHAR(255) DEFAULT NULL,
    `userAgent` VARCHAR(255) DEFAULT NULL,
    `action` VARCHAR(255) DEFAULT NULL,
    `resourceType` VARCHAR(255) DEFAULT NULL,
    `resourceId` VARCHAR(255) DEFAULT NULL,
    `method` VARCHAR(255) DEFAULT NULL,
    `path` VARCHAR(255) DEFAULT NULL,
    `requestBody` VARCHAR(255) DEFAULT NULL,
    `responseStatus` INT NOT NULL DEFAULT 0,
    `result` VARCHAR(255) DEFAULT NULL,
    `errorMsg` VARCHAR(255) DEFAULT NULL,
    `durationMs` INT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AuditLogFull';

-- AgentTask -> agent_task
DROP TABLE IF EXISTS `agent_task`;
CREATE TABLE `agent_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `taskId` VARCHAR(255) DEFAULT NULL,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `goal` VARCHAR(255) DEFAULT NULL,
    `status` VARCHAR(255) DEFAULT NULL,
    `rounds` INT NOT NULL DEFAULT 0,
    `result` VARCHAR(255) DEFAULT NULL,
    `llmCalls` INT NOT NULL DEFAULT 0,
    `toolCalls` INT NOT NULL DEFAULT 0,
    `totalTokens` INT NOT NULL DEFAULT 0,
    `errorMsg` VARCHAR(255) DEFAULT NULL,
    `latencyMs` BIGINT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AgentTask';

-- CollabMember -> collab_member
DROP TABLE IF EXISTS `collab_member`;
CREATE TABLE `collab_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `collabId` BIGINT NOT NULL DEFAULT 0,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `role` VARCHAR(255) DEFAULT NULL,
    `joinedAt` DATETIME DEFAULT NULL,
    `lastActiveAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CollabMember';

-- CollabSession -> collab_session
DROP TABLE IF EXISTS `collab_session`;
CREATE TABLE `collab_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `sessionId` VARCHAR(255) DEFAULT NULL,
    `ownerId` BIGINT NOT NULL DEFAULT 0,
    `title` VARCHAR(255) DEFAULT NULL,
    `maxUsers` INT NOT NULL DEFAULT 0,
    `status` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CollabSession';

-- KgEntity -> kg_entity
DROP TABLE IF EXISTS `kg_entity`;
CREATE TABLE `kg_entity` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `name` VARCHAR(255) DEFAULT NULL,
    `entityType` VARCHAR(255) DEFAULT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `aliases` VARCHAR(255) DEFAULT NULL,
    `importance` INT NOT NULL DEFAULT 0,
    `source` VARCHAR(255) DEFAULT NULL,
    `refCount` INT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KgEntity';

-- KgRelation -> kg_relation
DROP TABLE IF EXISTS `kg_relation`;
CREATE TABLE `kg_relation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `fromEntity` BIGINT NOT NULL DEFAULT 0,
    `toEntity` BIGINT NOT NULL DEFAULT 0,
    `relationType` VARCHAR(255) DEFAULT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `weight` DECIMAL(20,4) NOT NULL DEFAULT 0,
    `source` VARCHAR(255) DEFAULT NULL,
    `refCount` INT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KgRelation';

-- Plugin -> plugin
DROP TABLE IF EXISTS `plugin`;
CREATE TABLE `plugin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) DEFAULT NULL,
    `displayName` VARCHAR(255) DEFAULT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `version` VARCHAR(255) DEFAULT NULL,
    `author` VARCHAR(255) DEFAULT NULL,
    `category` VARCHAR(255) DEFAULT NULL,
    `scope` VARCHAR(255) DEFAULT NULL,
    `ownerId` BIGINT NOT NULL DEFAULT 0,
    `icon` VARCHAR(255) DEFAULT NULL,
    `entry` VARCHAR(255) DEFAULT NULL,
    `pluginType` VARCHAR(255) DEFAULT NULL,
    `config` VARCHAR(255) DEFAULT NULL,
    `enabled` INT NOT NULL DEFAULT 0,
    `downloads` INT NOT NULL DEFAULT 0,
    `rating` DECIMAL(20,4) NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Plugin';

-- AiChatMessage -> ai_chat_message
DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE `ai_chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `sessionId` VARCHAR(255) DEFAULT NULL,
    `role` VARCHAR(255) DEFAULT NULL,
    `content` VARCHAR(255) DEFAULT NULL,
    `toolCode` VARCHAR(255) DEFAULT NULL,
    `toolInput` VARCHAR(255) DEFAULT NULL,
    `toolOutput` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AiChatMessage';

-- AiChatSession -> ai_chat_session
DROP TABLE IF EXISTS `ai_chat_session`;
CREATE TABLE `ai_chat_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `sessionId` VARCHAR(255) DEFAULT NULL,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `username` VARCHAR(255) DEFAULT NULL,
    `title` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AiChatSession';

-- AiGenerationLog -> ai_generation_log
DROP TABLE IF EXISTS `ai_generation_log`;
CREATE TABLE `ai_generation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `generationId` VARCHAR(255) DEFAULT NULL,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `username` VARCHAR(255) DEFAULT NULL,
    `userIp` VARCHAR(255) DEFAULT NULL,
    `modality` VARCHAR(255) DEFAULT NULL,
    `modelName` VARCHAR(255) DEFAULT NULL,
    `modelVersion` VARCHAR(255) DEFAULT NULL,
    `prompt` VARCHAR(255) DEFAULT NULL,
    `negativePrompt` VARCHAR(255) DEFAULT NULL,
    `parameters` VARCHAR(255) DEFAULT NULL,
    `outputUrl` VARCHAR(255) DEFAULT NULL,
    `outputSize` BIGINT NOT NULL DEFAULT 0,
    `outputHash` VARCHAR(255) DEFAULT NULL,
    `watermarked` INT NOT NULL DEFAULT 0,
    `watermarkText` VARCHAR(255) DEFAULT NULL,
    `durationMs` INT NOT NULL DEFAULT 0,
    `status` VARCHAR(255) DEFAULT NULL,
    `errorMsg` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AiGenerationLog';

-- AiTool -> ai_tool
DROP TABLE IF EXISTS `ai_tool`;
CREATE TABLE `ai_tool` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(255) DEFAULT NULL,
    `name` VARCHAR(255) DEFAULT NULL,
    `category` VARCHAR(255) DEFAULT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `icon` VARCHAR(255) DEFAULT NULL,
    `enabled` INT NOT NULL DEFAULT 0,
    `builtin` INT NOT NULL DEFAULT 0,
    `inputSchema` VARCHAR(255) DEFAULT NULL,
    `outputSchema` VARCHAR(255) DEFAULT NULL,
    `defaultConfig` VARCHAR(255) DEFAULT NULL,
    `implType` VARCHAR(255) DEFAULT NULL,
    `implValue` VARCHAR(255) DEFAULT NULL,
    `rateLimit` INT NOT NULL DEFAULT 0,
    `timeoutSeconds` INT NOT NULL DEFAULT 0,
    `roleRequired` VARCHAR(255) DEFAULT NULL,
    `tags` VARCHAR(255) DEFAULT NULL,
    `version` VARCHAR(255) DEFAULT NULL,
    `author` VARCHAR(255) DEFAULT NULL,
    `createdBy` BIGINT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AiTool';

-- AiToolInvocation -> ai_tool_invocation
DROP TABLE IF EXISTS `ai_tool_invocation`;
CREATE TABLE `ai_tool_invocation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `toolCode` VARCHAR(255) DEFAULT NULL,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `username` VARCHAR(255) DEFAULT NULL,
    `inputJson` VARCHAR(255) DEFAULT NULL,
    `outputJson` VARCHAR(255) DEFAULT NULL,
    `status` VARCHAR(255) DEFAULT NULL,
    `errorMessage` VARCHAR(255) DEFAULT NULL,
    `durationMs` INT NOT NULL DEFAULT 0,
    `ip` VARCHAR(255) DEFAULT NULL,
    `userAgent` VARCHAR(255) DEFAULT NULL,
    `dataSourceId` BIGINT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AiToolInvocation';

-- AuditLog -> audit_log
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `traceId` VARCHAR(255) DEFAULT NULL,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `username` VARCHAR(255) DEFAULT NULL,
    `userIp` VARCHAR(255) DEFAULT NULL,
    `userAgent` VARCHAR(255) DEFAULT NULL,
    `action` VARCHAR(255) DEFAULT NULL,
    `resourceType` VARCHAR(255) DEFAULT NULL,
    `resourceId` VARCHAR(255) DEFAULT NULL,
    `method` VARCHAR(255) DEFAULT NULL,
    `path` VARCHAR(255) DEFAULT NULL,
    `requestBody` VARCHAR(255) DEFAULT NULL,
    `responseStatus` INT NOT NULL DEFAULT 0,
    `result` VARCHAR(255) DEFAULT NULL,
    `errorMsg` VARCHAR(255) DEFAULT NULL,
    `durationMs` INT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AuditLog';

-- DbDataSource -> data_source
DROP TABLE IF EXISTS `data_source`;
CREATE TABLE `data_source` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) DEFAULT NULL,
    `type` VARCHAR(255) DEFAULT NULL,
    `jdbcUrl` VARCHAR(255) DEFAULT NULL,
    `username` VARCHAR(255) DEFAULT NULL,
    `password` VARCHAR(255) DEFAULT NULL,
    `driverClass` VARCHAR(255) DEFAULT NULL,
    `poolSize` INT NOT NULL DEFAULT 0,
    `minIdle` INT NOT NULL DEFAULT 0,
    `maxLifetime` INT NOT NULL DEFAULT 0,
    `enabled` INT NOT NULL DEFAULT 0,
    `testStatus` VARCHAR(255) DEFAULT NULL,
    `testMessage` VARCHAR(255) DEFAULT NULL,
    `lastTestAt` DATETIME DEFAULT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `tags` VARCHAR(255) DEFAULT NULL,
    `createdBy` BIGINT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DbDataSource';

-- ModerationRecord -> moderation_record
DROP TABLE IF EXISTS `moderation_record`;
CREATE TABLE `moderation_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `traceId` VARCHAR(255) DEFAULT NULL,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `username` VARCHAR(255) DEFAULT NULL,
    `contentType` VARCHAR(255) DEFAULT NULL,
    `contentHash` VARCHAR(255) DEFAULT NULL,
    `contentSize` BIGINT NOT NULL DEFAULT 0,
    `contentUrl` VARCHAR(255) DEFAULT NULL,
    `moderationStatus` VARCHAR(255) DEFAULT NULL,
    `riskLevel` VARCHAR(255) DEFAULT NULL,
    `riskLabels` VARCHAR(255) DEFAULT NULL,
    `riskScore` DECIMAL(20,4) NOT NULL DEFAULT 0,
    `moderator` VARCHAR(255) DEFAULT NULL,
    `rejectionReason` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModerationRecord';

-- MultimediaFile -> multimedia_file
DROP TABLE IF EXISTS `multimedia_file`;
CREATE TABLE `multimedia_file` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `fileId` VARCHAR(255) DEFAULT NULL,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `username` VARCHAR(255) DEFAULT NULL,
    `fileName` VARCHAR(255) DEFAULT NULL,
    `originalName` VARCHAR(255) DEFAULT NULL,
    `fileType` VARCHAR(255) DEFAULT NULL,
    `mimeType` VARCHAR(255) DEFAULT NULL,
    `fileSize` BIGINT NOT NULL DEFAULT 0,
    `fileHash` VARCHAR(255) DEFAULT NULL,
    `storagePath` VARCHAR(255) DEFAULT NULL,
    `storageType` VARCHAR(255) DEFAULT NULL,
    `encrypted` INT NOT NULL DEFAULT 0,
    `durationMs` BIGINT NOT NULL DEFAULT 0,
    `width` INT NOT NULL DEFAULT 0,
    `height` INT NOT NULL DEFAULT 0,
    `bitrate` INT NOT NULL DEFAULT 0,
    `sampleRate` INT NOT NULL DEFAULT 0,
    `channels` INT NOT NULL DEFAULT 0,
    `codec` VARCHAR(255) DEFAULT NULL,
    `exif` VARCHAR(255) DEFAULT NULL,
    `moderationStatus` VARCHAR(255) DEFAULT NULL,
    `moderationId` BIGINT NOT NULL DEFAULT 0,
    `watermarked` INT NOT NULL DEFAULT 0,
    `isPublic` INT NOT NULL DEFAULT 0,
    `accessCount` INT NOT NULL DEFAULT 0,
    `expireAt` DATETIME DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MultimediaFile';

-- SensitiveWord -> sensitive_word
DROP TABLE IF EXISTS `sensitive_word`;
CREATE TABLE `sensitive_word` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `word` VARCHAR(255) DEFAULT NULL,
    `category` VARCHAR(255) DEFAULT NULL,
    `level` VARCHAR(255) DEFAULT NULL,
    `action` VARCHAR(255) DEFAULT NULL,
    `enabled` INT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SensitiveWord';

-- DataSource -> analytics_datasource
DROP TABLE IF EXISTS `analytics_datasource`;
CREATE TABLE `analytics_datasource` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `name` VARCHAR(255) DEFAULT NULL,
    `type` VARCHAR(255) DEFAULT NULL,
    `jdbcUrl` VARCHAR(255) DEFAULT NULL,
    `username` VARCHAR(255) DEFAULT NULL,
    `passwordEnc` VARCHAR(255) DEFAULT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DataSource';

-- IngestTask -> analytics_ingest_task
DROP TABLE IF EXISTS `analytics_ingest_task`;
CREATE TABLE `analytics_ingest_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `taskId` VARCHAR(255) DEFAULT NULL,
    `filename` VARCHAR(255) DEFAULT NULL,
    `fileType` VARCHAR(255) DEFAULT NULL,
    `encoding` VARCHAR(255) DEFAULT NULL,
    `separator` VARCHAR(255) DEFAULT NULL,
    `fileSize` BIGINT NOT NULL DEFAULT 0,
    `status` VARCHAR(255) DEFAULT NULL,
    `errorMessage` VARCHAR(255) DEFAULT NULL,
    `qualityJson` VARCHAR(255) DEFAULT NULL,
    `totalRows` BIGINT NOT NULL DEFAULT 0,
    `totalColumns` BIGINT NOT NULL DEFAULT 0,
    `columnsJson` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `finishedAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IngestTask';

-- Nl2SqlHistory -> analytics_nlsql_history
DROP TABLE IF EXISTS `analytics_nlsql_history`;
CREATE TABLE `analytics_nlsql_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `dataSourceId` BIGINT NOT NULL DEFAULT 0,
    `question` VARCHAR(255) DEFAULT NULL,
    `generatedSql` VARCHAR(255) DEFAULT NULL,
    `correctedSql` VARCHAR(255) DEFAULT NULL,
    `model` VARCHAR(255) DEFAULT NULL,
    `promptTokens` INT NOT NULL DEFAULT 0,
    `completionTokens` INT NOT NULL DEFAULT 0,
    `durationMs` BIGINT NOT NULL DEFAULT 0,
    `success` TINYINT(1) NOT NULL DEFAULT 0,
    `errorMessage` VARCHAR(255) DEFAULT NULL,
    `feedbackRating` INT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Nl2SqlHistory';

-- Report -> analytics_report
DROP TABLE IF EXISTS `analytics_report`;
CREATE TABLE `analytics_report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `reportId` VARCHAR(255) DEFAULT NULL,
    `title` VARCHAR(255) DEFAULT NULL,
    `question` VARCHAR(255) DEFAULT NULL,
    `sqlText` VARCHAR(255) DEFAULT NULL,
    `markdown` VARCHAR(255) DEFAULT NULL,
    `chartOptionsJson` VARCHAR(255) DEFAULT NULL,
    `rowCount` BIGINT NOT NULL DEFAULT 0,
    `durationMs` BIGINT NOT NULL DEFAULT 0,
    `format` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Report';

-- AuthLoginLog -> auth_login_log
DROP TABLE IF EXISTS `auth_login_log`;
CREATE TABLE `auth_login_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `username` VARCHAR(255) DEFAULT NULL,
    `ip` VARCHAR(255) DEFAULT NULL,
    `userAgent` VARCHAR(255) DEFAULT NULL,
    `status` INT NOT NULL DEFAULT 0,
    `message` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AuthLoginLog';

-- AuthRefreshToken -> auth_refresh_token
DROP TABLE IF EXISTS `auth_refresh_token`;
CREATE TABLE `auth_refresh_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `token` VARCHAR(255) DEFAULT NULL,
    `expiresAt` DATETIME DEFAULT NULL,
    `revoked` INT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AuthRefreshToken';

-- Notification -> notification
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `type` VARCHAR(255) DEFAULT NULL,
    `title` VARCHAR(255) DEFAULT NULL,
    `content` VARCHAR(255) DEFAULT NULL,
    `isRead` INT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Notification';

-- OAuthAppConfig -> oauth_app_config
DROP TABLE IF EXISTS `oauth_app_config`;
CREATE TABLE `oauth_app_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `platform` VARCHAR(255) DEFAULT NULL,
    `appType` VARCHAR(255) DEFAULT NULL,
    `appId` VARCHAR(255) DEFAULT NULL,
    `appSecret` VARCHAR(255) DEFAULT NULL,
    `publicKey` VARCHAR(255) DEFAULT NULL,
    `redirectUri` VARCHAR(255) DEFAULT NULL,
    `scopes` VARCHAR(255) DEFAULT NULL,
    `enabled` INT NOT NULL DEFAULT 0,
    `extraConfig` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuthAppConfig';

-- OAuthBinding -> oauth_binding
DROP TABLE IF EXISTS `oauth_binding`;
CREATE TABLE `oauth_binding` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `platform` VARCHAR(255) DEFAULT NULL,
    `appType` VARCHAR(255) DEFAULT NULL,
    `openid` VARCHAR(255) DEFAULT NULL,
    `unionid` VARCHAR(255) DEFAULT NULL,
    `nickname` VARCHAR(255) DEFAULT NULL,
    `avatar` VARCHAR(255) DEFAULT NULL,
    `accessToken` VARCHAR(255) DEFAULT NULL,
    `refreshToken` VARCHAR(255) DEFAULT NULL,
    `tokenExpiresAt` DATETIME DEFAULT NULL,
    `rawData` VARCHAR(255) DEFAULT NULL,
    `boundAt` DATETIME DEFAULT NULL,
    `lastLoginAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuthBinding';

-- SysRole -> sys_role
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(255) DEFAULT NULL,
    `name` VARCHAR(255) DEFAULT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `sort` INT NOT NULL DEFAULT 0,
    `enabled` INT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SysRole';

-- SysUser -> sys_user
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(255) DEFAULT NULL,
    `password` VARCHAR(255) DEFAULT NULL,
    `nickname` VARCHAR(255) DEFAULT NULL,
    `email` VARCHAR(255) DEFAULT NULL,
    `phone` VARCHAR(255) DEFAULT NULL,
    `avatar` VARCHAR(255) DEFAULT NULL,
    `gender` INT NOT NULL DEFAULT 0,
    `status` INT NOT NULL DEFAULT 0,
    `lastLoginIp` VARCHAR(255) DEFAULT NULL,
    `lastLoginAt` DATETIME DEFAULT NULL,
    `tenantId` BIGINT NOT NULL DEFAULT 0,
    `remark` VARCHAR(255) DEFAULT NULL,
    `wechatOpenid` VARCHAR(255) DEFAULT NULL,
    `wechatUnionid` VARCHAR(255) DEFAULT NULL,
    `wechatNickname` VARCHAR(255) DEFAULT NULL,
    `wechatAvatar` VARCHAR(255) DEFAULT NULL,
    `wechatBoundAt` DATETIME DEFAULT NULL,
    `qqOpenid` VARCHAR(255) DEFAULT NULL,
    `qqUnionid` VARCHAR(255) DEFAULT NULL,
    `qqNickname` VARCHAR(255) DEFAULT NULL,
    `qqAvatar` VARCHAR(255) DEFAULT NULL,
    `qqBoundAt` DATETIME DEFAULT NULL,
    `alipayOpenid` VARCHAR(255) DEFAULT NULL,
    `alipayUserId` VARCHAR(255) DEFAULT NULL,
    `alipayNickname` VARCHAR(255) DEFAULT NULL,
    `alipayAvatar` VARCHAR(255) DEFAULT NULL,
    `alipayBoundAt` DATETIME DEFAULT NULL,
    `createdBy` BIGINT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedBy` BIGINT NOT NULL DEFAULT 0,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SysUser';

-- SysUserRole -> sys_user_role
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `userId` BIGINT NOT NULL DEFAULT 0,
    `roleId` BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SysUserRole';

-- Tenant -> tenant
DROP TABLE IF EXISTS `tenant`;
CREATE TABLE `tenant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(255) DEFAULT NULL,
    `name` VARCHAR(255) DEFAULT NULL,
    `plan` VARCHAR(255) DEFAULT NULL,
    `status` INT NOT NULL DEFAULT 0,
    `maxUsers` INT NOT NULL DEFAULT 0,
    `maxModels` INT NOT NULL DEFAULT 0,
    `qpsLimit` INT NOT NULL DEFAULT 0,
    `monthlyQuota` BIGINT NOT NULL DEFAULT 0,
    `usedQuota` BIGINT NOT NULL DEFAULT 0,
    `expireAt` DATETIME DEFAULT NULL,
    `contactEmail` VARCHAR(255) DEFAULT NULL,
    `contactPhone` VARCHAR(255) DEFAULT NULL,
    `remark` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tenant';

-- UnionidRelations -> unionid_relations
DROP TABLE IF EXISTS `unionid_relations`;
CREATE TABLE `unionid_relations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `unionid` VARCHAR(255) DEFAULT NULL,
    `platform` VARCHAR(255) DEFAULT NULL,
    `firstSeenAt` DATETIME DEFAULT NULL,
    `lastSeenAt` DATETIME DEFAULT NULL,
    `bindingCount` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='UnionidRelations';

-- UserApiKey -> user_api_key
DROP TABLE IF EXISTS `user_api_key`;
CREATE TABLE `user_api_key` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `name` VARCHAR(255) DEFAULT NULL,
    `keyHash` VARCHAR(255) DEFAULT NULL,
    `keyPrefix` VARCHAR(255) DEFAULT NULL,
    `scopes` VARCHAR(255) DEFAULT NULL,
    `expiresAt` DATETIME DEFAULT NULL,
    `lastUsedAt` DATETIME DEFAULT NULL,
    `useCount` BIGINT NOT NULL DEFAULT 0,
    `enabled` INT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='UserApiKey';

-- WechatConfig -> wechat_config
DROP TABLE IF EXISTS `wechat_config`;
CREATE TABLE `wechat_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `appType` VARCHAR(255) DEFAULT NULL,
    `appId` VARCHAR(255) DEFAULT NULL,
    `appSecret` VARCHAR(255) DEFAULT NULL,
    `token` VARCHAR(255) DEFAULT NULL,
    `aesKey` VARCHAR(255) DEFAULT NULL,
    `redirectUri` VARCHAR(255) DEFAULT NULL,
    `scope` VARCHAR(255) DEFAULT NULL,
    `enabled` INT NOT NULL DEFAULT 0,
    `remark` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WechatConfig';

-- WechatScanSession -> wechat_scan_session
DROP TABLE IF EXISTS `wechat_scan_session`;
CREATE TABLE `wechat_scan_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `ticket` VARCHAR(255) DEFAULT NULL,
    `sceneId` VARCHAR(255) DEFAULT NULL,
    `status` VARCHAR(255) DEFAULT NULL,
    `openid` VARCHAR(255) DEFAULT NULL,
    `unionid` VARCHAR(255) DEFAULT NULL,
    `nickname` VARCHAR(255) DEFAULT NULL,
    `avatar` VARCHAR(255) DEFAULT NULL,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `accessToken` VARCHAR(255) DEFAULT NULL,
    `refreshToken` VARCHAR(255) DEFAULT NULL,
    `clientIp` VARCHAR(255) DEFAULT NULL,
    `userAgent` VARCHAR(255) DEFAULT NULL,
    `expiresAt` DATETIME DEFAULT NULL,
    `confirmedAt` DATETIME DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WechatScanSession';

-- WechatUserBinding -> wechat_user_binding
DROP TABLE IF EXISTS `wechat_user_binding`;
CREATE TABLE `wechat_user_binding` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `openid` VARCHAR(255) DEFAULT NULL,
    `unionid` VARCHAR(255) DEFAULT NULL,
    `appType` VARCHAR(255) DEFAULT NULL,
    `nickname` VARCHAR(255) DEFAULT NULL,
    `avatar` VARCHAR(255) DEFAULT NULL,
    `boundAt` DATETIME DEFAULT NULL,
    `lastLoginAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WechatUserBinding';

-- ChatMessage -> chat_message
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `sessionId` BIGINT NOT NULL DEFAULT 0,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `role` VARCHAR(255) DEFAULT NULL,
    `content` VARCHAR(255) DEFAULT NULL,
    `tokens` INT NOT NULL DEFAULT 0,
    `finishReason` VARCHAR(255) DEFAULT NULL,
    `errorMessage` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ChatMessage';

-- ChatSession -> chat_session
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `title` VARCHAR(255) DEFAULT NULL,
    `model` VARCHAR(255) DEFAULT NULL,
    `systemPrompt` VARCHAR(255) DEFAULT NULL,
    `temperature` DECIMAL(20,4) NOT NULL DEFAULT 0,
    `status` INT NOT NULL DEFAULT 0,
    `messageCount` INT NOT NULL DEFAULT 0,
    `lastMessageAt` DATETIME DEFAULT NULL,
    `tenantId` BIGINT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ChatSession';

-- AsyncTask -> async_task
DROP TABLE IF EXISTS `async_task`;
CREATE TABLE `async_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `taskId` VARCHAR(255) DEFAULT NULL,
    `taskType` VARCHAR(255) DEFAULT NULL,
    `status` VARCHAR(255) DEFAULT NULL,
    `params` VARCHAR(255) DEFAULT NULL,
    `result` VARCHAR(255) DEFAULT NULL,
    `errorMsg` VARCHAR(255) DEFAULT NULL,
    `retryCount` INT NOT NULL DEFAULT 0,
    `latencyMs` BIGINT NOT NULL DEFAULT 0,
    `submitterId` BIGINT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `startedAt` DATETIME DEFAULT NULL,
    `finishedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AsyncTask';

-- RateLimitRule -> rate_limit_rule
DROP TABLE IF EXISTS `rate_limit_rule`;
CREATE TABLE `rate_limit_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `scope` VARCHAR(255) DEFAULT NULL,
    `key` VARCHAR(255) DEFAULT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `capacity` INT NOT NULL DEFAULT 0,
    `refillTokens` INT NOT NULL DEFAULT 0,
    `periodSeconds` INT NOT NULL DEFAULT 0,
    `enabled` INT NOT NULL DEFAULT 0,
    `priority` INT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RateLimitRule';

-- RequestLog -> request_log
DROP TABLE IF EXISTS `request_log`;
CREATE TABLE `request_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `traceId` VARCHAR(255) DEFAULT NULL,
    `method` VARCHAR(255) DEFAULT NULL,
    `path` VARCHAR(255) DEFAULT NULL,
    `queryString` VARCHAR(255) DEFAULT NULL,
    `clientIp` VARCHAR(255) DEFAULT NULL,
    `userAgent` VARCHAR(255) DEFAULT NULL,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `status` INT NOT NULL DEFAULT 0,
    `latencyMs` BIGINT NOT NULL DEFAULT 0,
    `slow` TINYINT(1) NOT NULL DEFAULT 0,
    `error` TINYINT(1) NOT NULL DEFAULT 0,
    `module` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RequestLog';

-- FunctionCallLog -> function_call_log
DROP TABLE IF EXISTS `function_call_log`;
CREATE TABLE `function_call_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `sessionId` BIGINT NOT NULL DEFAULT 0,
    `toolName` VARCHAR(255) DEFAULT NULL,
    `arguments` VARCHAR(255) DEFAULT NULL,
    `result` VARCHAR(255) DEFAULT NULL,
    `status` VARCHAR(255) DEFAULT NULL,
    `errorMsg` VARCHAR(255) DEFAULT NULL,
    `durationMs` INT NOT NULL DEFAULT 0,
    `ip` VARCHAR(255) DEFAULT NULL,
    `userAgent` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FunctionCallLog';

-- FunctionTool -> function_tool
DROP TABLE IF EXISTS `function_tool`;
CREATE TABLE `function_tool` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) DEFAULT NULL,
    `displayName` VARCHAR(255) DEFAULT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `category` VARCHAR(255) DEFAULT NULL,
    `scope` VARCHAR(255) DEFAULT NULL,
    `ownerId` BIGINT NOT NULL DEFAULT 0,
    `parameters` VARCHAR(255) DEFAULT NULL,
    `endpoint` VARCHAR(255) DEFAULT NULL,
    `httpMethod` VARCHAR(255) DEFAULT NULL,
    `enabled` INT NOT NULL DEFAULT 0,
    `tags` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FunctionTool';

-- LongTermMemory -> memory_long_term
DROP TABLE IF EXISTS `memory_long_term`;
CREATE TABLE `memory_long_term` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `sessionId` BIGINT NOT NULL DEFAULT 0,
    `content` VARCHAR(255) DEFAULT NULL,
    `summary` VARCHAR(255) DEFAULT NULL,
    `role` VARCHAR(255) DEFAULT NULL,
    `embedding` VARCHAR(255) DEFAULT NULL,
    `dim` INT NOT NULL DEFAULT 0,
    `importance` DECIMAL(20,4) NOT NULL DEFAULT 0,
    `tags` VARCHAR(255) DEFAULT NULL,
    `accessCount` INT NOT NULL DEFAULT 0,
    `lastAccessAt` DATETIME DEFAULT NULL,
    `expiresAt` DATETIME DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LongTermMemory';

-- UserPref -> memory_user_pref
DROP TABLE IF EXISTS `memory_user_pref`;
CREATE TABLE `memory_user_pref` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `prefKey` VARCHAR(255) DEFAULT NULL,
    `prefValue` VARCHAR(255) DEFAULT NULL,
    `weight` DECIMAL(20,4) NOT NULL DEFAULT 0,
    `source` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='UserPref';

-- ModelBattleLog -> model_battle_log
DROP TABLE IF EXISTS `model_battle_log`;
CREATE TABLE `model_battle_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `battleId` VARCHAR(255) DEFAULT NULL,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `modelId` BIGINT NOT NULL DEFAULT 0,
    `modelCode` VARCHAR(255) DEFAULT NULL,
    `prompt` VARCHAR(255) DEFAULT NULL,
    `response` VARCHAR(255) DEFAULT NULL,
    `promptTokens` INT NOT NULL DEFAULT 0,
    `completionTokens` INT NOT NULL DEFAULT 0,
    `latencyMs` INT NOT NULL DEFAULT 0,
    `status` VARCHAR(255) DEFAULT NULL,
    `errorMsg` VARCHAR(255) DEFAULT NULL,
    `score` INT NOT NULL DEFAULT 0,
    `judgeModel` VARCHAR(255) DEFAULT NULL,
    `judgeReason` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelBattleLog';

-- ModelConfig -> model_config
DROP TABLE IF EXISTS `model_config`;
CREATE TABLE `model_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `providerId` BIGINT NOT NULL DEFAULT 0,
    `modelCode` VARCHAR(255) DEFAULT NULL,
    `displayName` VARCHAR(255) DEFAULT NULL,
    `maxContext` INT NOT NULL DEFAULT 0,
    `maxOutput` INT NOT NULL DEFAULT 0,
    `inputPrice` DECIMAL(20,4) NOT NULL DEFAULT 0,
    `outputPrice` DECIMAL(20,4) NOT NULL DEFAULT 0,
    `supportsVision` INT NOT NULL DEFAULT 0,
    `supportsTools` INT NOT NULL DEFAULT 0,
    `supportsStream` INT NOT NULL DEFAULT 0,
    `enabled` INT NOT NULL DEFAULT 0,
    `sort` INT NOT NULL DEFAULT 0,
    `description` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelConfig';

-- ModelProvider -> model_provider
DROP TABLE IF EXISTS `model_provider`;
CREATE TABLE `model_provider` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(255) DEFAULT NULL,
    `name` VARCHAR(255) DEFAULT NULL,
    `baseUrl` VARCHAR(255) DEFAULT NULL,
    `apiKey` VARCHAR(255) DEFAULT NULL,
    `protocol` VARCHAR(255) DEFAULT NULL,
    `enabled` INT NOT NULL DEFAULT 0,
    `sort` INT NOT NULL DEFAULT 0,
    `description` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelProvider';

-- ModelQuota -> model_quota
DROP TABLE IF EXISTS `model_quota`;
CREATE TABLE `model_quota` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `modelId` BIGINT NOT NULL DEFAULT 0,
    `quotaDate` DATE DEFAULT NULL,
    `usedTokens` BIGINT NOT NULL DEFAULT 0,
    `usedRequests` INT NOT NULL DEFAULT 0,
    `limitTokens` BIGINT NOT NULL DEFAULT 0,
    `limitRequests` INT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelQuota';

-- TrainingTask -> training_task
DROP TABLE IF EXISTS `training_task`;
CREATE TABLE `training_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `userId` BIGINT NOT NULL DEFAULT 0,
    `modelName` VARCHAR(255) DEFAULT NULL,
    `corpusPath` VARCHAR(255) DEFAULT NULL,
    `nLayer` INT NOT NULL DEFAULT 0,
    `nHead` INT NOT NULL DEFAULT 0,
    `nEmbd` INT NOT NULL DEFAULT 0,
    `blockSize` INT NOT NULL DEFAULT 0,
    `maxIters` INT NOT NULL DEFAULT 0,
    `batchSize` INT NOT NULL DEFAULT 0,
    `learningRate` DOUBLE NOT NULL DEFAULT 0,
    `status` VARCHAR(255) DEFAULT NULL,
    `progress` INT NOT NULL DEFAULT 0,
    `currentLoss` DOUBLE NOT NULL DEFAULT 0,
    `currentIter` INT NOT NULL DEFAULT 0,
    `errorMessage` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `completedAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TrainingTask';

-- AlertChannel -> alert_channel
DROP TABLE IF EXISTS `alert_channel`;
CREATE TABLE `alert_channel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) DEFAULT NULL,
    `channelType` VARCHAR(255) DEFAULT NULL,
    `type` VARCHAR(255) DEFAULT NULL,
    `target` VARCHAR(255) DEFAULT NULL,
    `config` VARCHAR(255) DEFAULT NULL,
    `enabled` INT NOT NULL DEFAULT 0,
    `priority` INT NOT NULL DEFAULT 0,
    `description` VARCHAR(255) DEFAULT NULL,
    `createdBy` BIGINT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AlertChannel';

-- AlertEvent -> alert_event
DROP TABLE IF EXISTS `alert_event`;
CREATE TABLE `alert_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `ruleId` BIGINT NOT NULL DEFAULT 0,
    `ruleName` VARCHAR(255) DEFAULT NULL,
    `severity` VARCHAR(255) DEFAULT NULL,
    `metricName` VARCHAR(255) DEFAULT NULL,
    `message` VARCHAR(255) DEFAULT NULL,
    `status` VARCHAR(255) DEFAULT NULL,
    `firedAt` DATETIME DEFAULT NULL,
    `resolvedAt` DATETIME DEFAULT NULL,
    `ackedAt` DATETIME DEFAULT NULL,
    `ackedBy` BIGINT NOT NULL DEFAULT 0,
    `duration` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AlertEvent';

-- AlertRule -> alert_rule
DROP TABLE IF EXISTS `alert_rule`;
CREATE TABLE `alert_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) DEFAULT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `metricName` VARCHAR(255) DEFAULT NULL,
    `service` VARCHAR(255) DEFAULT NULL,
    `operator` VARCHAR(255) DEFAULT NULL,
    `severity` VARCHAR(255) DEFAULT NULL,
    `cooldownMinutes` INT NOT NULL DEFAULT 0,
    `enabled` INT NOT NULL DEFAULT 0,
    `tags` VARCHAR(255) DEFAULT NULL,
    `notifyChannel` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AlertRule';

-- MetricSnapshot -> metric_snapshot
DROP TABLE IF EXISTS `metric_snapshot`;
CREATE TABLE `metric_snapshot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `service` VARCHAR(255) DEFAULT NULL,
    `metricName` VARCHAR(255) DEFAULT NULL,
    `metricValue` DECIMAL(20,4) NOT NULL DEFAULT 0,
    `tags` VARCHAR(255) DEFAULT NULL,
    `recordedAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MetricSnapshot';

-- PipelineNodeLog -> pipeline_node_log
DROP TABLE IF EXISTS `pipeline_node_log`;
CREATE TABLE `pipeline_node_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `runId` BIGINT NOT NULL DEFAULT 0,
    `nodeId` VARCHAR(255) DEFAULT NULL,
    `nodeType` VARCHAR(255) DEFAULT NULL,
    `nodeName` VARCHAR(255) DEFAULT NULL,
    `status` VARCHAR(255) DEFAULT NULL,
    `startTime` DATETIME DEFAULT NULL,
    `endTime` DATETIME DEFAULT NULL,
    `durationMs` BIGINT NOT NULL DEFAULT 0,
    `inputRows` INT NOT NULL DEFAULT 0,
    `outputRows` INT NOT NULL DEFAULT 0,
    `outputPreview` VARCHAR(255) DEFAULT NULL,
    `errorMessage` VARCHAR(255) DEFAULT NULL,
    `configSnapshot` VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PipelineNodeLog';

-- PipelineRun -> pipeline_run
DROP TABLE IF EXISTS `pipeline_run`;
CREATE TABLE `pipeline_run` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `workflowId` BIGINT NOT NULL DEFAULT 0,
    `workflowName` VARCHAR(255) DEFAULT NULL,
    `status` VARCHAR(255) DEFAULT NULL,
    `triggerBy` BIGINT NOT NULL DEFAULT 0,
    `triggerType` VARCHAR(255) DEFAULT NULL,
    `definitionSnapshot` VARCHAR(255) DEFAULT NULL,
    `startTime` DATETIME DEFAULT NULL,
    `endTime` DATETIME DEFAULT NULL,
    `durationMs` BIGINT NOT NULL DEFAULT 0,
    `errorMessage` VARCHAR(255) DEFAULT NULL,
    `resultSummary` VARCHAR(255) DEFAULT NULL,
    `createTime` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PipelineRun';

-- PipelineWorkflow -> pipeline_workflow
DROP TABLE IF EXISTS `pipeline_workflow`;
CREATE TABLE `pipeline_workflow` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) DEFAULT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `definition` VARCHAR(255) DEFAULT NULL,
    `version` INT NOT NULL DEFAULT 0,
    `status` INT NOT NULL DEFAULT 0,
    `createBy` BIGINT NOT NULL DEFAULT 0,
    `updateBy` BIGINT NOT NULL DEFAULT 0,
    `createTime` DATETIME DEFAULT NULL,
    `updateTime` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PipelineWorkflow';

-- PipelineWorkflowVersion -> pipeline_workflow_version
DROP TABLE IF EXISTS `pipeline_workflow_version`;
CREATE TABLE `pipeline_workflow_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `workflowId` BIGINT NOT NULL DEFAULT 0,
    `version` INT NOT NULL DEFAULT 0,
    `definition` VARCHAR(255) DEFAULT NULL,
    `changeLog` VARCHAR(255) DEFAULT NULL,
    `createBy` BIGINT NOT NULL DEFAULT 0,
    `createTime` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PipelineWorkflowVersion';

-- PromptTemplate -> prompt_template
DROP TABLE IF EXISTS `prompt_template`;
CREATE TABLE `prompt_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) DEFAULT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `category` VARCHAR(255) DEFAULT NULL,
    `content` VARCHAR(255) DEFAULT NULL,
    `variables` VARCHAR(255) DEFAULT NULL,
    `creatorId` BIGINT NOT NULL DEFAULT 0,
    `creatorName` VARCHAR(255) DEFAULT NULL,
    `isPublic` TINYINT(1) NOT NULL DEFAULT 0,
    `useCount` INT NOT NULL DEFAULT 0,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PromptTemplate';

-- Document -> document
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `kbId` BIGINT NOT NULL DEFAULT 0,
    `ownerId` BIGINT NOT NULL DEFAULT 0,
    `title` VARCHAR(255) DEFAULT NULL,
    `sourceType` VARCHAR(255) DEFAULT NULL,
    `sourceUri` VARCHAR(255) DEFAULT NULL,
    `content` VARCHAR(255) DEFAULT NULL,
    `sizeBytes` BIGINT NOT NULL DEFAULT 0,
    `status` VARCHAR(255) DEFAULT NULL,
    `errorMsg` VARCHAR(255) DEFAULT NULL,
    `chunkCount` INT NOT NULL DEFAULT 0,
    `checksum` VARCHAR(255) DEFAULT NULL,
    `tags` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Document';

-- DocumentChunk -> document_chunk
DROP TABLE IF EXISTS `document_chunk`;
CREATE TABLE `document_chunk` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `docId` BIGINT NOT NULL DEFAULT 0,
    `kbId` BIGINT NOT NULL DEFAULT 0,
    `ownerId` BIGINT NOT NULL DEFAULT 0,
    `chunkIndex` INT NOT NULL DEFAULT 0,
    `content` VARCHAR(255) DEFAULT NULL,
    `embedding` VARCHAR(255) DEFAULT NULL,
    `dim` INT NOT NULL DEFAULT 0,
    `charCount` INT NOT NULL DEFAULT 0,
    `startPos` INT NOT NULL DEFAULT 0,
    `endPos` INT NOT NULL DEFAULT 0,
    `accessCount` INT NOT NULL DEFAULT 0,
    `lastAccessAt` DATETIME DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DocumentChunk';

-- KnowledgeBase -> knowledge_base
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `ownerId` BIGINT NOT NULL DEFAULT 0,
    `tenantId` BIGINT NOT NULL DEFAULT 0,
    `name` VARCHAR(255) DEFAULT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `visibility` VARCHAR(255) DEFAULT NULL,
    `docCount` INT NOT NULL DEFAULT 0,
    `chunkCount` INT NOT NULL DEFAULT 0,
    `tags` VARCHAR(255) DEFAULT NULL,
    `createdAt` DATETIME DEFAULT NULL,
    `updatedAt` DATETIME DEFAULT NULL,
    `deleted` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KnowledgeBase';
